<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { t } from '../i18n';
  import {
    closeCommandPalette,
    commandPaletteOpen,
    groupFindMatches,
    openCommandPalette,
    resetSearchIndex,
    searchIndexStore,
    setSearchText,
  } from '../stores/searchIndex';
  import type { FindKind, FindMatch } from '../stores/searchIndex';
  import {
    openCompletionReport,
    openEndedPlanView,
    openEpicView,
    openPlanView,
    openProjectInExplorer,
    pushRoute,
  } from '../stores/runtime';
  import { stripControlSequences } from '../utils/outputSafety';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * The command palette (UX-P-02, UC-PLN-8): the search button of the top bar and `Ctrl+K`/`⌘K` or
   * `/` open one field over the view, never a route.
   *
   * The field sends its text to the runtime's find after the pause in the typing, and the panel
   * shows what comes back grouped by kind in the order served — running work first, ended work
   * marked. Every title is a text node after its control sequences are removed; an act is offered
   * for the project a match names, so a search can end in starting work there. `↑`/`↓` move, `Enter`
   * opens, `Esc` closes and returns to the view the panel was opened from.
   */

  /** An act the panel offers beside the matches. */
  interface ActRow {
    key: string;
    kind: 'act';
    label: string;
    projectId: string;
  }

  /** One match, as the panel draws and opens it. */
  interface MatchRow {
    key: string;
    kind: FindKind;
    match: FindMatch;
  }

  type Row = ActRow | MatchRow;

  /** The id of the panel's title, for `aria-labelledby`. */
  const titleId = uniqueId('command-palette-title');

  /** The text of the field; the store holds the text the matches belong to. */
  let text = $state('');

  /** The one field, so Tab can return the focus to it. */
  let field = $state<HTMLInputElement | undefined>(undefined);

  /** The row the keyboard acts on, anchored to its kind and id, never to its position. */
  let selectedKey = $state<string | null>(null);

  /** The one-line answer for an Enter on an empty field. */
  let notice = $state('');

  /** The control the focus returns to when the panel leaves the document. */
  let returnFocus: HTMLElement | null = null;

  /** @param value a title the runtime serves @returns it as plain text, without control sequences */
  function plain(value: string | null | undefined): string {
    return stripControlSequences(value);
  }

  /** @param match a match @returns the stable key the selection is anchored to */
  function matchKey(match: FindMatch): string {
    return `${match.ended ? 'ended:' : ''}${match.kind}:${match.id}`;
  }

  /**
   * @param match a match
   * @returns where it belongs, as the names the runtime serves, without the match's own title
   */
  function locationOf(match: FindMatch): string {
    const parts =
      match.kind === 'plan'
        ? [match.project?.title, match.epic?.title]
        : match.kind === 'epic'
          ? [match.project?.title]
          : match.kind === 'project'
            ? []
            : [match.project?.title, match.epic?.title, match.plan?.title];
    return parts
      .map((part) => plain(part))
      .filter((part) => part.length > 0)
      .join(' › ');
  }

  /** The matches the runtime served, in its order. */
  const matches = $derived($searchIndexStore.matches);

  /** The acts the answer offers: one per project it names. */
  const acts = $derived.by<ActRow[]>(() =>
    matches
      .filter((match) => match.kind === 'project')
      .map((match) => {
        const projectId = match.project?.id ?? match.id;
        const project = plain(match.project?.title ?? match.title);
        return {
          key: `act:${projectId}`,
          kind: 'act' as const,
          label: $t('start.plan.title', { project }),
          projectId,
        };
      }),
  );

  /** Every row the keyboard walks: the acts first, then the matches in the runtime's order. */
  const rows = $derived<Row[]>([
    ...acts,
    ...matches.map(
      (match): MatchRow => ({ key: matchKey(match), kind: match.kind, match }),
    ),
  ]);

  /** The matches grouped by kind in the order served; a run of one kind may be ended. */
  const groups = $derived(groupFindMatches(matches));

  // A selection whose row the new answer no longer carries falls back to the first row, so Enter
  // always has somewhere to go.
  $effect(() => {
    const keys = rows.map((row) => row.key);
    if (selectedKey === null || !keys.includes(selectedKey)) {
      selectedKey = keys[0] ?? null;
    }
  });

  /** @param key a row's key @returns whether the keyboard acts on it */
  function isSelected(key: string): boolean {
    return key === selectedKey;
  }

  /**
   * @param direction how far the selection moves, wrapping at both ends
   */
  function move(direction: 1 | -1): void {
    if (rows.length === 0) {
      return;
    }
    const current = rows.findIndex((row) => row.key === selectedKey);
    const from = current === -1 ? (direction === 1 ? -1 : 0) : current;
    const next = (from + direction + rows.length) % rows.length;
    selectedKey = rows[next]?.key ?? null;
  }

  /**
   * Opens a row: an act starts a plan in its project, a match opens where it belongs.
   *
   * @param row the row the operator chose
   */
  function openRow(row: Row | undefined): void {
    if (!row) {
      return;
    }
    close();
    if (row.kind === 'act') {
      pushRoute('start', { projectId: row.projectId });
      return;
    }
    const match = row.match;
    switch (match.kind) {
      case 'project':
        openProjectInExplorer(match.project?.id ?? match.id);
        return;
      case 'epic':
        openEpicView(match.id);
        return;
      case 'plan':
        if (match.ended) {
          openEndedPlanView({ plan_id: match.id, outcome: match.outcome ?? null });
        } else {
          openPlanView(match.id);
        }
        return;
      case 'decision':
        pushRoute('decisions', { decisionId: match.id });
        return;
      case 'report':
        if (match.plan?.id) {
          openCompletionReport(match.plan.id);
        } else {
          pushRoute('reports');
        }
    }
  }

  /** Closes the panel, forgets the search, and leaves the view where it was. */
  function close(): void {
    resetSearchIndex();
    text = '';
    notice = '';
    closeCommandPalette();
  }

  /** Sends the text again after a failed read. */
  function retry(): void {
    setSearchText(text);
  }

  /**
   * @param event the key press inside the panel
   */
  function onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      close();
      return;
    }
    // The panel is the only stop of the page behind it: Tab keeps the focus in its field, as every
    // dialogue of the app keeps it in its own box.
    if (event.key === 'Tab') {
      event.preventDefault();
      field?.focus();
      return;
    }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      move(event.key === 'ArrowDown' ? 1 : -1);
      return;
    }
    // A button in the panel (a row, Retry) keeps its own Enter: only the field's Enter opens the
    // selection.
    if (event.key === 'Enter' && (event.target as HTMLElement | null)?.tagName !== 'BUTTON') {
      event.preventDefault();
      if ($searchIndexStore.text.length === 0) {
        // An empty field is a hint, not a search: the name stays where it is.
        notice = $t('overview.find.no_text');
        return;
      }
      notice = '';
      openRow(rows.find((row) => row.key === selectedKey));
    }
  }

  /**
   * Takes the focus into the field when the panel opens, locks the page behind it, and gives the
   * focus back to the control that opened it when the panel leaves the document.
   *
   * @param node the panel's own element
   * @returns the action's teardown, which restores the focus
   */
  function takeOver(node: HTMLElement): { destroy: () => void } {
    returnFocus = document.activeElement as HTMLElement | null;
    const root = document.documentElement;
    const previousOverflow = root.style.overflow;
    root.style.overflow = 'hidden';
    node.querySelector<HTMLInputElement>('input')?.focus();
    return {
      destroy(): void {
        root.style.overflow = previousOverflow;
        returnFocus?.focus();
        returnFocus = null;
      },
    };
  }

  /** @param target the element that received the key press @returns whether it takes typed text */
  function isTextTarget(target: EventTarget | null): boolean {
    const element = target as HTMLElement | null;
    if (!element) {
      return false;
    }
    return element.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(element.tagName);
  }

  onMount(() => {
    const onWindowKeydown = (event: KeyboardEvent): void => {
      if (event.defaultPrevented) {
        return;
      }
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        openCommandPalette();
        return;
      }
      // `/` is the second way in, and only where nothing in a field has the focus. It is taken
      // before the views' own `/` shortcuts (a filter's focus key), so one key has one meaning.
      if (event.key === '/' && !get(commandPaletteOpen) && !isTextTarget(event.target)) {
        event.preventDefault();
        event.stopPropagation();
        openCommandPalette();
      }
    };
    // The capture phase is what lets the panel claim `/` ahead of a view's bubble-phase shortcut.
    window.addEventListener('keydown', onWindowKeydown, true);
    return () => window.removeEventListener('keydown', onWindowKeydown, true);
  });
</script>

{#if $commandPaletteOpen}
  <div class="command-palette-backdrop" data-testid="command-palette">
    <div
      class="command-palette"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      tabindex="-1"
      use:takeOver
      onkeydown={onKeydown}
    >
      <h2 class="command-palette__title" id={titleId}>{$t('overview.find.title')}</h2>

      <input
        class="command-palette__field"
        type="search"
        autocomplete="off"
        spellcheck="false"
        aria-autocomplete="list"
        aria-controls="command-palette-rows"
        placeholder={$t('overview.find.hint')}
        aria-label={$t('overview.find.hint')}
        data-testid="command-palette-field"
        bind:this={field}
        bind:value={text}
        oninput={() => {
          notice = '';
          setSearchText(text);
        }}
      />

      <p class="sr-only" role="status" data-testid="command-palette-notice">{notice}</p>

      <div class="command-palette__results" id="command-palette-rows">
        {#if acts.length > 0}
          <ul class="command-palette__list" aria-label={$t('overview.find.title')}>
            {#each acts as act (act.key)}
              <li>
                <button
                  type="button"
                  class="command-palette__row"
                  class:command-palette__row--selected={isSelected(act.key)}
                  aria-current={isSelected(act.key) ? 'true' : undefined}
                  data-testid="command-palette-act"
                  onclick={() => openRow(act)}
                  onmouseenter={() => (selectedKey = act.key)}
                >
                  <span class="command-palette__act-label">{act.label}</span>
                </button>
              </li>
            {/each}
          </ul>
        {/if}

        {#each groups as group, index (index)}
          <div class="command-palette__group">
            {#if index === 0 || groups[index - 1]?.kind !== group.kind}
              <h3 class="command-palette__group-title">
                {$t(`overview.find.kind.${group.kind}`)}
              </h3>
            {/if}
            <ul class="command-palette__list" aria-label={$t(`overview.find.kind.${group.kind}`)}>
              {#each group.matches as match (matchKey(match))}
                <li>
                  <button
                    type="button"
                    class="command-palette__row"
                    class:command-palette__row--selected={isSelected(matchKey(match))}
                    aria-current={isSelected(matchKey(match)) ? 'true' : undefined}
                    data-testid="command-palette-match"
                    data-kind={match.kind}
                    onclick={() => openRow({ key: matchKey(match), kind: match.kind, match })}
                    onmouseenter={() => (selectedKey = matchKey(match))}
                  >
                    <span class="command-palette__match-title">{plain(match.title)}</span>
                    {#if locationOf(match).length > 0}
                      <span class="command-palette__match-location">{locationOf(match)}</span>
                    {/if}
                    {#if match.ended}
                      <span class="command-palette__match-ended">
                        {$t('overview.find.ended')}
                        {#if match.outcome}
                          · {$t('overview.ended.outcome', { outcome: match.outcome })}
                        {/if}
                      </span>
                    {/if}
                  </button>
                </li>
              {/each}
            </ul>
          </div>
        {/each}

        {#if $searchIndexStore.text.length > 0 && $searchIndexStore.status === 'done' && matches.length === 0}
          <p class="command-palette__empty" data-testid="command-palette-empty">
            {$t('overview.find.empty', { text: $searchIndexStore.text })}
          </p>
        {/if}

        {#if $searchIndexStore.status === 'failed'}
          <p class="command-palette__failed">
            <button
              type="button"
              class="command-palette__retry"
              data-testid="command-palette-retry"
              onclick={retry}
            >
              {$t('web.common.retry')}
            </button>
          </p>
        {/if}
      </div>
    </div>
  </div>
{/if}

<style>
  .command-palette-backdrop {
    position: fixed;
    inset: 0;
    z-index: 100;
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding: 10vh var(--space-4) var(--space-4);
    background-color: var(--color-overlay);
  }

  .command-palette {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    inline-size: min(640px, 100%);
    max-block-size: 70vh;
    padding: var(--space-4);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    box-shadow: var(--shadow-overlay);
    color: var(--color-text-primary);
  }

  .command-palette:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring), var(--shadow-overlay);
  }

  .command-palette__title {
    margin: 0;
    font-size: var(--text-lg);
    line-height: var(--line-height-lg);
  }

  .command-palette__field {
    inline-size: 100%;
    min-block-size: 40px;
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-base);
    color: var(--color-text-primary);
    background-color: var(--color-bg-subtle);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-md);
  }

  .command-palette__results {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    overflow: auto;
    min-block-size: 0;
  }

  .command-palette__group {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .command-palette__group-title {
    margin: 0;
    font-size: var(--text-xs);
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--color-text-muted);
  }

  .command-palette__list {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .command-palette__row {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-2);
    inline-size: 100%;
    padding: var(--space-2) var(--space-3);
    text-align: start;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    background: transparent;
    border: 1px solid transparent;
    border-radius: var(--radius-sm);
    cursor: pointer;
  }

  .command-palette__row:hover,
  .command-palette__row--selected {
    background-color: var(--color-bg-subtle);
    border-color: var(--color-border);
  }

  .command-palette__match-title {
    font-weight: 500;
  }

  .command-palette__match-location,
  .command-palette__match-ended {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .command-palette__act-label {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .command-palette__empty,
  .command-palette__failed {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .command-palette__retry {
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-sm);
    cursor: pointer;
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  @media (max-width: 767px) {
    /* The panel covers the view it was opened from. */
    .command-palette-backdrop {
      padding: 0;
    }

    .command-palette {
      inline-size: 100%;
      block-size: 100%;
      max-block-size: none;
      border-radius: 0;
    }
  }
</style>
