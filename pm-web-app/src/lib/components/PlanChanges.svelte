<script lang="ts">
  import { onDestroy } from 'svelte';
  import { t } from '../i18n';
  import {
    apiClient,
    type ChangeSizeRecord,
    type PlanChangedFile,
    type PlanChangeFile,
    type PlanChangeHunk,
    type PlanChangeLine,
    type PlanChangesAnswer,
  } from '../api/client';
  import { stripControlSequences } from '../utils/outputSafety';
  import { errorCodeOf } from '../api/errors';
  import { formatChangeSize } from '../display';
  import ErrorState from './ErrorState.svelte';
  import Spinner from './Spinner.svelte';

  /**
   * The plan's changes: the list of the last capture, and one file's hunks (design
   * `plan-view.adoc#changes`, `plan-view-interaction.adoc#changes`).
   *
   * Everything comes from the runtime's answers at the capture's head; the view computes no diff
   * and adds no figure up. Every path and every line is untrusted text: it is shown after
   * `stripControlSequences`, never as markup and never as a link the client follows. The view is
   * read only: it offers no act on the code.
   */
  interface Props {
    /** The plan whose changes are shown. */
    planId: string;
    /** The size of the change, the same figures the head's line shows. */
    change: ChangeSizeRecord;
    /** The file the address names, or `null` when the list stands alone. */
    selectedFile: string | null;
    /** Reports the file a row opens, or `null` when the list no longer names the selected file. */
    onSelectFile: (path: string | null) => void;
    /** A count the view raises on `plan.change_captured`: the list is read again. */
    reloadToken?: number;
    /** Whether the object column is at least `720 px` wide: the list and the hunks stand side by side. */
    wide?: boolean;
  }

  let {
    planId,
    change,
    selectedFile,
    onSelectFile,
    reloadToken = 0,
    wide = false,
  }: Props = $props();

  let answer = $state<PlanChangesAnswer | null>(null);
  let isLoading = $state(true);
  let listError = $state<string | null>(null);
  let currentFile = $state<PlanChangeFile | null>(null);
  let isFileLoading = $state(false);
  /** The error of each row whose read failed, by row identity: a failed row keeps its own account. */
  let fileErrors = $state<Record<string, string>>({});
  /** The row the shown file answer belongs to; a different row is a different set of hunks. */
  let loadedIdentity = $state<string | null>(null);
  /** The row whose read is open, so a re-render does not open a second read of the same hunks. */
  let inFlightIdentity = $state<string | null>(null);
  /** The read started last, so an answer of a superseded read never settles the current one. */
  let fileRequest = 0;
  /** A count a person's retry raises, so a failed row is read again rather than staying failed. */
  let fileRetryToken = $state(0);
  /** Whether the view is gone, so an answer still on its way is not written onto dead state. */
  let disposed = false;
  /**
   * The read that owns the list.
   *
   * A list request can resolve after a capture has replaced it; the generation is bumped on every
   * read, so an old answer never overwrites the one the newer read brought.
   */
  let listGeneration = 0;

  /**
   * @param file one row of the list
   * @returns its status mark: `A`, `M`, `D`, `R`, or `B` for a binary file, as the design draws it
   */
  function statusMark(file: PlanChangedFile): string {
    if (file.binary) {
      return 'B';
    }
    switch (file.status) {
      case 'added':
        return 'A';
      case 'deleted':
        return 'D';
      case 'renamed':
        return 'R';
      default:
        return 'M';
    }
  }

  /**
   * @param size the size the process resource or an event served
   * @returns the catalogue's one-line form of it
   */
  function changeText(size: ChangeSizeRecord): string {
    return formatChangeSize(size, $t);
  }

  /**
   * @param read the answer, or none
   * @returns its files, and none for a refusal
   */
  function filesOf(read: PlanChangesAnswer | null): PlanChangedFile[] {
    return read !== null && read.state === 'captured' ? read.files : [];
  }

  /**
   * @param read the answer, or none
   * @param path a file's path
   * @returns the row the list holds for the path, or `null`
   */
  function rowOf(read: PlanChangesAnswer | null, path: string): PlanChangedFile | null {
    return filesOf(read).find((file) => file.path === path) ?? null;
  }

  /**
   * The identity of a row: the plan, the capture's base and head, the path and the figures its hunks
   * belong to. A file answer carries no head, so this is what tells a current answer from one the list
   * moved under, one plan's row from another's, and two files of the same status and counts apart.
   *
   * @param read the answer the row belongs to, or none
   * @param path the file's path
   * @returns the row's identity, or `null` when the list names no such row
   */
  function rowIdentity(read: PlanChangesAnswer | null, path: string): string | null {
    const row = rowOf(read, path);
    if (row === null || read === null || read.state !== 'captured') {
      return null;
    }
    // A tuple, never a joined string: a path may itself hold the separator a join would use.
    return JSON.stringify([
      planId,
      read.base_ref,
      read.head,
      path,
      row.status,
      row.added,
      row.removed,
      row.binary,
      row.renamed_from ?? '',
    ]);
  }

  /**
   * @param hunk one hunk of a file
   * @returns its lines with the old and the new line number each one holds
   */
  function numberLines(hunk: PlanChangeHunk): {
    index: number;
    kind: PlanChangeLine['kind'];
    text: string;
    old: number | null;
    next: number | null;
  }[] {
    let old = hunk.old_start;
    let next = hunk.new_start;
    return hunk.lines.map((line, index) => {
      const numbered = {
        index,
        kind: line.kind,
        text: line.text,
        old: null as number | null,
        next: null as number | null,
      };
      if (line.kind !== 'added') {
        numbered.old = old;
        old += 1;
      }
      if (line.kind !== 'removed') {
        numbered.next = next;
        next += 1;
      }
      return numbered;
    });
  }

  /**
   * @param kind what a line is
   * @returns the diff's mark for it, as the design spells one
   */
  function markOf(kind: PlanChangeLine['kind']): string {
    if (kind === 'added') return '+';
    if (kind === 'removed') return '−';
    return ' ';
  }

  /** The plan whose list the current answer belongs to; a plain field, so an effect never tracks it. */
  let loadedPlanId: string | null = null;

  /** Reads the list of the capture, keeping the selected file when the list still names it. */
  async function readList(): Promise<void> {
    const generation = ++listGeneration;
    const plan = planId;
    if (loadedPlanId !== null && loadedPlanId !== plan) {
      // Another plan is open: its predecessor's list must not stand under the new plan's head.
      answer = null;
      currentFile = null;
    }
    loadedPlanId = plan;
    isLoading = true;
    listError = null;
    try {
      const read = await apiClient.getPlanChanges(plan);
      if (disposed || generation !== listGeneration) {
        return;
      }
      answer = read;
      if (selectedFile !== null && rowOf(read, selectedFile) === null) {
        // The capture the list now serves no longer holds the file: the address must not keep it.
        onSelectFile(null);
      }
    } catch (error) {
      if (!disposed && generation === listGeneration) {
        answer = null;
        listError = errorCodeOf(error);
      }
    } finally {
      if (!disposed && generation === listGeneration) {
        isLoading = false;
      }
    }
  }

  /**
   * The selected row's identity, or `null` when the list names none.
   */
  const selectedIdentity = $derived(
    selectedFile === null || answer === null ? null : rowIdentity(answer, selectedFile),
  );

  /** The selected row's error, where its read failed; another row never inherits it. */
  const fileError = $derived(
    selectedIdentity === null ? null : (fileErrors[selectedIdentity] ?? null),
  );

  /**
   * @param errors the errors by row identity
   * @param identity the row whose error is dropped
   * @returns the errors without that row
   */
  function withoutRow(errors: Record<string, string>, identity: string): Record<string, string> {
    const next = { ...errors };
    delete next[identity];
    return next;
  }

  $effect(() => {
    // The plan, and the token a capture raises: each reads the list again.
    void planId;
    void reloadToken;
    void readList();
  });

  $effect(() => {
    const path = selectedFile;
    // A person's retry clears the failed row and reads it again.
    void fileRetryToken;
    const identity = selectedIdentity;
    if (path === null || identity === null) {
      currentFile = null;
      loadedIdentity = null;
      isFileLoading = false;
      return;
    }
    if (currentFile !== null && loadedIdentity === identity) {
      // The list was read again and the row did not change: the hunks it owns stay as they are.
      return;
    }
    if (fileError !== null) {
      // This row failed: the panel shows its own error and retry, never another row's hunks.
      currentFile = null;
      loadedIdentity = null;
      isFileLoading = false;
      return;
    }
    if (inFlightIdentity === identity) {
      // Returning to the row whose read is still open: its spinner stands until the answer lands.
      isFileLoading = true;
      return;
    }
    if (disposed) {
      return;
    }
    // The hunks of another row are no longer the ones the tab stands on: the panel shows the read in
    // flight, never the previous row's lines under the new capture's title.
    currentFile = null;
    loadedIdentity = null;
    isFileLoading = true;
    inFlightIdentity = identity;
    const request = ++fileRequest;
    void (async () => {
      try {
        const read = await apiClient.getPlanChangeFile(planId, path);
        // The answer carries no head. It belongs to the row the list held when the read started; a
        // list that moved meanwhile — or a later read of the row — makes it stale, so it is dropped and
        // the effect reads the row of the new list instead.
        if (request !== fileRequest || disposed || selectedFile !== path
            || rowIdentity(answer, path) !== identity) {
          return;
        }
        currentFile = read;
        loadedIdentity = identity;
        fileErrors = withoutRow(fileErrors, identity);
      } catch (error) {
        if (request === fileRequest && !disposed && selectedFile === path
            && rowIdentity(answer, path) === identity) {
          currentFile = null;
          loadedIdentity = null;
          fileErrors = { ...fileErrors, [identity]: errorCodeOf(error) };
        }
      } finally {
        if (request === fileRequest) {
          inFlightIdentity = null;
          isFileLoading = false;
        }
      }
    })();
  });

  onDestroy(() => {
    // A read a view still owes leaves its answer on the floor: the view it would fill is gone.
    disposed = true;
  });

  /** Reads the selected file again after a failed read, instead of leaving the row failed for good. */
  function retryFile(): void {
    if (selectedIdentity !== null) {
      fileErrors = withoutRow(fileErrors, selectedIdentity);
    }
    fileRetryToken += 1;
  }
</script>

<div class="plan-changes" data-testid="plan-changes">
  {#if isLoading && answer === null}
    <div class="plan-changes__loading">
      <Spinner size="1.5rem" label={$t('plan.view.loading')} />
    </div>
  {:else if listError !== null}
    <div class="plan-changes__error">
      <ErrorState code={listError} actionLabel={$t('common.action.retry')} onAct={readList} />
    </div>
  {:else if answer?.state === 'not_captured'}
    <p class="plan-changes__empty text-muted" data-testid="changes-not-captured">
      {$t('plan.changes.not_captured')}
    </p>
  {:else if answer?.state === 'unavailable'}
    <p class="plan-changes__unavailable" data-testid="changes-unavailable">
      {$t('plan.changes.unavailable')}
      <span class="plan-changes__reason text-muted"
        >· {stripControlSequences(answer.reason)}</span
      >
    </p>
  {:else if answer?.state === 'captured'}
    <h3 class="plan-changes__title mono" data-testid="changes-title">
      {$t('plan.changes.title', { head: answer.head.slice(0, 7), size: changeText(change) })}
    </h3>

    <div class="plan-changes__body" class:plan-changes__body--wide={wide}>
      <ol class="change-list">
        {#each answer.files as file (file.path)}
          <li class="change-row" class:change-row--current={selectedFile === file.path}>
            <button
              type="button"
              class="change-row__open"
              data-testid="change-row"
              data-status={file.binary ? 'binary' : file.status}
              aria-current={selectedFile === file.path ? 'true' : undefined}
              onclick={() => onSelectFile(file.path)}
            >
              <span class="change-status mono">{statusMark(file)}</span>
              <span class="change-path mono" title={stripControlSequences(file.path)}
                >{stripControlSequences(file.path)}</span
              >
              {#if file.renamed_from}
                <span class="change-from mono"
                  >← {stripControlSequences(file.renamed_from)}</span
                >
              {/if}
              {#if file.binary}
                <span class="change-count">{$t('plan.changes.binary')}</span>
              {:else}
                <span class="change-count change-count--added mono">+{file.added}</span>
                {#if file.removed > 0}
                  <span class="change-count change-count--removed mono">−{file.removed}</span>
                {/if}
              {/if}
              {#if file.unplanned}
                <span class="change-unplanned">{$t('plan.changes.unplanned')}</span>
              {/if}
            </button>
          </li>
        {/each}
      </ol>

      <div class="change-file">
        {#if selectedFile !== null}
          {#if isFileLoading && currentFile === null}
            <div class="change-file__loading">
              <Spinner size="1.25rem" label={$t('plan.view.loading')} />
            </div>
          {:else if fileError !== null}
            <ErrorState
              code={fileError}
              actionLabel={$t('common.action.retry')}
              onAct={retryFile}
              titleLevel={4}
            />
          {:else if currentFile?.binary}
            <p class="change-file__binary text-muted" data-testid="change-file-binary">
              {$t('plan.changes.binary')}
            </p>
          {:else if currentFile}
            <div class="change-file__path mono">{stripControlSequences(currentFile.path)}</div>
            <div class="change-file__hunks" data-testid="change-hunks">
              {#each currentFile.hunks as hunk, hunkIndex (hunkIndex)}
                <div class="hunk">
                  <div class="hunk-header mono" data-testid="hunk-header">
                    @@ -{hunk.old_start},{hunk.old_len} +{hunk.new_start},{hunk.new_len} @@
                  </div>
                  {#each numberLines(hunk) as line (line.index)}
                    <div class="diff-line diff-line--{line.kind}" data-testid="diff-line">
                      <span class="diff-num mono" data-testid="line-old">{line.old ?? ''}</span>
                      <span class="diff-num mono" data-testid="line-new">{line.next ?? ''}</span>
                      <span class="diff-mark mono">{markOf(line.kind)}</span>
                      <span class="diff-text mono">{stripControlSequences(line.text)}</span>
                    </div>
                  {/each}
                </div>
              {/each}
              {#if currentFile.truncated}
                <p class="change-file__truncated text-muted" data-testid="changes-truncated">
                  {$t('plan.changes.truncated', { lines: currentFile.lines_omitted ?? 0 })}
                </p>
              {/if}
            </div>
          {/if}
        {/if}
      </div>
    </div>
  {/if}
</div>

<style>
  .plan-changes {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    min-block-size: 0;
  }

  .plan-changes__loading,
  .plan-changes__error {
    display: flex;
    justify-content: center;
    padding: var(--space-6);
  }

  .plan-changes__title {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .plan-changes__unavailable {
    color: var(--color-danger);
  }

  .plan-changes__body {
    display: grid;
    gap: var(--space-4);
  }

  /*
   * The list and the hunks stand side by side while the object column is at least `720 px` wide,
   * the width the plan view measures on its own panel host; a narrower column stacks them, the
   * hunks below the list (`plan-view-interaction.adoc#layout`).
   */
  .plan-changes__body--wide {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr);
    align-items: start;
  }

  .change-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
  }

  .change-row {
    min-inline-size: 0;
  }

  .change-row__open {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    inline-size: 100%;
    min-block-size: 32px;
    padding: var(--space-1) var(--space-2);
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    color: var(--color-text-primary);
    text-align: start;
    cursor: pointer;
    font-size: var(--text-sm);
  }

  .change-row__open:hover {
    background-color: var(--color-bg-subtle);
  }

  .change-row__open:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .change-row--current .change-row__open {
    background-color: var(--color-brand-subtle);
  }

  .change-status {
    flex: 0 0 auto;
    inline-size: 1ch;
    color: var(--color-text-secondary);
  }

  .change-path {
    min-inline-size: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .change-from {
    min-inline-size: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--color-text-secondary);
  }

  .change-count {
    flex: 0 0 auto;
    margin-inline-start: auto;
    color: var(--color-text-secondary);
  }

  .change-count--added {
    color: var(--color-success);
  }

  .change-count--removed {
    color: var(--color-danger);
  }

  .change-unplanned {
    flex: 0 0 auto;
    color: var(--color-attention);
  }

  .change-file {
    min-inline-size: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .change-file__path {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    overflow-wrap: anywhere;
  }

  .change-file__hunks {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    min-inline-size: 0;
  }

  .hunk {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    overflow: hidden;
  }

  .hunk-header {
    padding: var(--space-1) var(--space-2);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
  }

  .diff-line {
    display: grid;
    grid-template-columns: 4ch 4ch 1.5ch minmax(0, 1fr);
    gap: var(--space-1);
    padding-inline: var(--space-2);
    font-size: var(--text-xs);
    white-space: pre;
  }

  .diff-line--added {
    background-color: var(--color-diff-added);
  }

  .diff-line--removed {
    background-color: var(--color-diff-removed);
  }

  .diff-num {
    color: var(--color-text-muted);
    text-align: end;
    user-select: none;
  }

  .diff-text {
    min-inline-size: 0;
    overflow-x: auto;
  }

  .change-file__truncated {
    margin: 0;
    font-size: var(--text-xs);
  }

  .change-file__binary {
    margin: 0;
  }
</style>
