<script lang="ts">
  import { onMount } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import KindMark, { KIND_WORD_KEYS, type Kind } from '../components/KindMark.svelte';
  import ReasonBadge from '../components/ReasonBadge.svelte';
  import RowLink from '../components/RowLink.svelte';
  import Button from '../components/Button.svelte';
  import MascotFigure from '../components/MascotFigure.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import TriagePage, { type Level } from '../templates/TriagePage.svelte';
  import { apiClient, type ProjectSummary } from '../api/client';
  import { parseAddress } from '../routes';
  import { openAddress } from '../stores/runtime';
  import {
    attentionActLabelOf,
    attentionFallbackKeyOf,
    attentionKindMarkOf,
    attentionStore,
    attentionTargetOf,
    attentionTitleOf,
    loadAttention,
    loadMoreAttention,
    type AttentionItem,
  } from '../stores/attention';

  /**
   * The start page: the list of what waits (UC-PLN-7; needs-and-find.adoc, What Waits).
   *
   * It is the list column of the lens What waits on the column engine, with no object column of its
   * own (`#route`): each item opens at the address of the object it belongs to, and the act is a
   * button that opens the place the act belongs to — the list itself performs no act, so an item
   * resolved elsewhere leaves rather than being answered twice.
   *
   * The rows, the order and the empty state are the design's (`#list-layout`): the `KindMark` of the
   * object, its name, the reason the item waits (`ReasonBadge`, none for a decision), its path, the
   * since, what it blocks, and its act. An empty list names what runs per project.
   */

  /** One item waiting; the levels of the list are one, so the engine draws one column. */
  const LEVELS_KEY = 'what-waits';

  /** The kinds a `blocks` value can name: the three work kinds of the shared vocabulary. */
  const BLOCK_KINDS: ReadonlySet<string> = new Set(['plan', 'epic', 'project']);

  /** The projects with running work, for the empty state. */
  let projects = $state<ProjectSummary[]>([]);

  /** The one level of the lens: the list itself, which the rail keeps selected. */
  const levels = $derived<Level[]>([
    {
      key: LEVELS_KEY,
      kind: 'machine' as Kind,
      title: $t('overview.needs.title'),
      href: '/?lens=waits',
      state: '',
    },
  ]);

  /** The projects the empty state names: those whose work still runs. */
  const runningProjects = $derived(
    projects.filter((project) => {
      const plans = Number(project.active_plans_count ?? 0);
      const epics = Number(project.active_epics_count ?? 0);
      return plans + epics > 0;
    }),
  );

  onMount(() => {
    void loadAttention();
    void apiClient
      .getProjects()
      .then((summary) => {
        projects = summary.projects ?? [];
      })
      .catch(() => {
        // The empty state's running work is a courtesy: a failed read leaves it out, never stalls
        // the list the page is.
      });
  });

  /**
   * @param item one item of the list
   * @returns the name the row shows: the object's title, or the item's own word
   */
  function nameOf(item: AttentionItem): string {
    return attentionTitleOf(item) || $t(attentionFallbackKeyOf(item));
  }

  /**
   * @param item one item of the list
   * @returns the words of its act: those of the place the act opens
   */
  function actLabelOf(item: AttentionItem): string {
    const label = attentionActLabelOf(item);
    return $t(label.key, label.args);
  }

  /**
   * @param item one item of the list
   * @returns the trail of the object it belongs to, without the name the row's title carries
   */
  function pathOf(item: AttentionItem): string {
    const trail = [item.project?.title, item.epic?.title, item.plan?.title]
      .map((part) => (part ?? '').trim())
      .filter((part) => part.length > 0);
    const title = attentionTitleOf(item);
    return trail[trail.length - 1] === title ? trail.slice(0, -1).join(' › ') : trail.join(' › ');
  }

  /**
   * @param item one item of the list
   * @returns what it blocks, in the shared kind vocabulary, or the words for blocking no work
   */
  function blocksOf(item: AttentionItem): string {
    const blocks = item.blocks ?? '';
    if (!BLOCK_KINDS.has(blocks)) {
      return $t('overview.needs.blocks_nothing');
    }
    return $t('overview.needs.blocks', { what: $t(KIND_WORD_KEYS[blocks as Kind]) });
  }

  /**
   * @param item one item of the list
   * @returns when it started to wait, or an empty string while the runtime measures none
   */
  function sinceOf(item: AttentionItem): string {
    return formatTime(item.since, 'short', $currentLocale);
  }

  /** Opens the place an item's act belongs to, through the one address writer. */
  function openItem(item: AttentionItem): void {
    const address = parseAddress(attentionTargetOf(item));
    if (address) {
      openAddress(address);
    }
  }

  /**
   * @param item one item of the list
   * @param index its position in the list
   * @returns the key of its row, so an item resolved elsewhere does not move its neighbours
   */
  function rowKey(item: AttentionItem, index: number): string {
    return `${item.kind}\u0000${item.link ?? ''}\u0000${index}`;
  }
</script>

<div class="inbox-view" data-testid="inbox-view">
  <TriagePage levels={levels} label={$t('overview.needs.title')} column={listColumn} />
</div>

{#snippet listColumn()}
  {@render listPane()}
{/snippet}

{#snippet listPane()}
  <div class="inbox">
    <header class="inbox__header">
      <h1 class="inbox__title" tabindex="-1">{$t('overview.needs.title')}</h1>
      {#if $attentionStore.total > 0}
        <span class="inbox__count" data-testid="inbox-count">
          {$t('overview.needs.count', { count: $attentionStore.total })}
        </span>
      {/if}
    </header>

    {#if $attentionStore.loadError !== null && $attentionStore.items.length > 0}
      <div class="inbox__error" data-testid="inbox-error">
        <ErrorState
          code={$attentionStore.loadError}
          actionLabel={$t('common.action.retry')}
          onAct={() => void loadAttention()}
          titleLevel={2}
        />
      </div>
    {/if}

    {#if $attentionStore.isLoading && !$attentionStore.loaded}
      <Skeleton shape="card" rows={3} label={$t('overview.needs.title')} />
    {:else if $attentionStore.loadError !== null && !$attentionStore.loaded}
      <ErrorState
        code={$attentionStore.loadError}
        actionLabel={$t('common.action.retry')}
        onAct={() => void loadAttention()}
        titleLevel={2}
      />
    {:else if $attentionStore.items.length === 0}
      <div class="inbox__empty" data-testid="inbox-empty">
        <MascotFigure pose="all-clear" crop="bust" height={140} />
        <p class="inbox__empty-title">{$t('overview.needs.empty')}</p>
        {#if runningProjects.length > 0}
          <h2 class="inbox__empty-running">{$t('overview.needs.empty.running')}</h2>
          <ul class="inbox__projects" data-testid="inbox-running-projects">
            {#each runningProjects as project (project.project_id)}
              <li>
                <RowLink href={`/projects/${project.project_id}`} title={project.display_name}>
                  {#snippet kind()}<KindMark kind="project" wordHidden />{/snippet}
                  {#snippet tags()}
                    <span>{project.active_plans_count}</span>
                    <span>{project.active_epics_count}</span>
                  {/snippet}
                </RowLink>
              </li>
            {/each}
          </ul>
        {/if}
      </div>
    {:else}
      <ul class="inbox__rows" data-testid="inbox-rows">
        {#each $attentionStore.items as item, index (rowKey(item, index))}
          <li class="inbox__row" data-testid="inbox-row" data-kind={item.kind}>
            <RowLink href={attentionTargetOf(item)} title={nameOf(item)}>
              {#snippet kind()}
                <KindMark kind={attentionKindMarkOf(item)} wordHidden />
              {/snippet}
              {#snippet tags()}
                {#if item.kind !== 'decision'}
                  <ReasonBadge reason={item.kind} />
                {/if}
              {/snippet}
              {#snippet acts()}
                <Button variant="ghost" size="sm" onclick={() => openItem(item)}>
                  {actLabelOf(item)}
                </Button>
              {/snippet}
              {#snippet detail()}
                <span class="inbox__detail" data-testid="inbox-detail">
                  {#if pathOf(item)}
                    <span class="inbox__path">{pathOf(item)}</span>
                  {/if}
                  {#if sinceOf(item)}
                    <span>{$t('overview.needs.since', { time: sinceOf(item) })}</span>
                  {/if}
                  <span>{blocksOf(item)}</span>
                </span>
              {/snippet}
            </RowLink>
          </li>
        {/each}
      </ul>

      {#if $attentionStore.hasOlder}
        <div class="inbox__more">
          <Button
            variant="secondary"
            size="sm"
            loading={$attentionStore.isLoading}
            onclick={() => void loadMoreAttention()}
            data-testid="inbox-more"
          >
            {$t('overview.needs.more')}
          </Button>
        </div>
      {/if}
    {/if}
  </div>
{/snippet}

<style>
  .inbox-view {
    inline-size: 100%;
    min-inline-size: 0;
  }

  .inbox {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding-inline: var(--space-3);
    padding-block-end: var(--space-4);
  }

  .inbox__header {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: var(--space-2);
    padding-inline: var(--space-2);
  }

  .inbox__title {
    margin: 0;
    font-size: var(--text-lg);
    font-weight: var(--weight-semibold);
  }

  /* The count is text, not a pill: the design system names a count in its own words. */
  .inbox__count {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
  }

  .inbox__rows,
  .inbox__projects {
    display: flex;
    flex-direction: column;
    gap: 1px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .inbox__row {
    min-inline-size: 0;
  }

  .inbox__detail {
    display: inline-flex;
    flex-wrap: wrap;
    gap: var(--space-1) var(--space-3);
  }

  .inbox__path {
    color: var(--color-text-muted);
  }

  .inbox__empty {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
    padding: var(--space-4) var(--space-2);
  }

  .inbox__empty-title {
    margin: 0;
    font-size: var(--text-base);
    color: var(--color-text-primary);
  }

  .inbox__empty-running {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    color: var(--color-text-secondary);
  }

  .inbox__projects {
    align-self: stretch;
  }

  .inbox__error {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .inbox__more {
    padding-inline: var(--space-2);
  }
</style>
