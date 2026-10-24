<script lang="ts">
  import { t } from '../i18n';
  import type { DraftedPlan, ReviewItem, ReviewReadBack, ReviewResource } from '../api/client';

  interface Props {
    epicId: string;
    register?: 'findings' | 'watches';
    review?: ReviewResource | null;
    open?: boolean;
    busy?: boolean;
    notice?: string;
    onKeep?: (itemId: string, as: 'finding' | 'watch') => void;
    onDrop?: (itemId: string, reason: string) => void;
    onPromote?: (itemId: string, instruction?: string) => void;
    onEdit?: (itemId: string, instruction: string) => void;
    onAcceptReadBack?: () => void;
    onInstructAgain?: () => void;
    onDiscardReadBack?: () => void;
    onStagePlan?: () => void;
    onAcceptRemaining?: () => void;
    onCommit?: () => void;
    onChat?: (itemId: string | null) => void;
    onClose?: () => void;
  }

  let {
    epicId,
    register = 'findings',
    review = null,
    open = false,
    busy = false,
    notice = '',
    onKeep = () => {},
    onDrop = () => {},
    onPromote = () => {},
    onEdit = () => {},
    onAcceptReadBack = () => {},
    onInstructAgain = () => {},
    onDiscardReadBack = () => {},
    onStagePlan = () => {},
    onAcceptRemaining = () => {},
    onCommit = () => {},
    onChat = () => {},
    onClose = () => {},
  }: Props = $props();

  let panel = $state<HTMLElement | null>(null);
  let selectedId = $state<string | null>(null);
  let dropReason = $state('');
  let instruction = $state('');
  let planInstruction = $state('');
  let confirmingRemaining = $state(false);
  let editing = $state(false);
  let promoting = $state(false);

  let selected = $derived<ReviewItem | null>(
    review?.items.find((item) => item.item_id === selectedId) ?? null,
  );
  let openItems = $derived(review?.items.filter((item) => item.state === 'open') ?? []);
  let drafted: DraftedPlan | null = $derived(review?.drafted_plan ?? null);
  let readBack: ReviewReadBack | null = $derived(review?.read_back ?? null);

  $effect(() => {
    if (open) {
      panel?.focus();
    }
  });

  function onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      close();
    }
  }

  /** The readable label of an item's disposition. */
  function stateLabel(state: string): string {
    return $t(`control.review.state.${state}`);
  }

  function select(item: ReviewItem): void {
    selectedId = item.item_id;
    dropReason = '';
    instruction = '';
    planInstruction = '';
    editing = false;
    promoting = false;
    confirmingRemaining = false;
  }

  function keepAs(): 'finding' | 'watch' {
    if (register === 'findings') {
      return 'finding';
    }
    return 'watch';
  }

  function moveTo(): 'finding' | 'watch' {
    return keepAs() === 'finding' ? 'watch' : 'finding';
  }

  function close(): void {
    selectedId = null;
    dropReason = '';
    instruction = '';
    editing = false;
    promoting = false;
    confirmingRemaining = false;
    onClose();
  }

  function drop(): void {
    if (!selected || !dropReason.trim()) return;
    onDrop(selected.item_id, dropReason.trim());
    dropReason = '';
  }

  function edit(): void {
    if (!selected || !instruction.trim()) return;
    onEdit(selected.item_id, instruction.trim());
    editing = true;
  }

  function promote(): void {
    if (!selected) return;
    onPromote(selected.item_id, planInstruction.trim() || undefined);
    promoting = true;
  }

  function acceptReadBack(): void {
    onAcceptReadBack();
    editing = false;
  }

  function discardReadBack(): void {
    onDiscardReadBack();
    editing = false;
    instruction = '';
    planInstruction = '';
  }

  function stagePlan(): void {
    onStagePlan();
    promoting = false;
    planInstruction = '';
  }
</script>

{#if open}
  <div class="review-modal" data-testid="review-modal">
    <div
      class="review-modal__panel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="review-title"
      tabindex="-1"
      bind:this={panel}
      onkeydown={onKeydown}
    >
      <header class="review-modal__head">
        <h2 id="review-title" class="review-modal__title" data-testid="review-title">
          {$t('control.review.title', { epic: epicId, register })}
        </h2>
        <span class="review-modal__progress" data-testid="review-progress">
          {$t('control.review.progress', {
            done: review?.decided ?? 0,
            total: review?.total ?? 0,
          })}
        </span>
        <button type="button" data-testid="review-close" onclick={close}>
          {$t('common.close')}
        </button>
      </header>

      {#if notice}
        <p class="review-modal__notice" data-testid="review-notice" role="alert">{notice}</p>
      {/if}

      <div class="review-modal__body">
        <nav class="review-modal__items" aria-label={$t('control.review.items')}>
          <h3 class="review-modal__subtitle">{$t('control.review.items')}</h3>
          {#if review && review.items.length > 0}
            <ul class="review-modal__list">
              {#each review.items as item (item.item_id)}
                <li>
                  <button
                    type="button"
                    class="review-modal__item"
                    class:selected={selectedId === item.item_id}
                    data-testid={`review-item-${item.item_id}`}
                    onclick={() => select(item)}
                  >
                    <span class="review-modal__item-id mono">{item.item_id}</span>
                    <span class="review-modal__item-text">{item.statement}</span>
                    <span class="review-modal__item-state">{stateLabel(item.state)}</span>
                  </button>
                </li>
              {/each}
            </ul>
          {:else}
            <p class="review-modal__empty" data-testid="review-empty">
              {$t('control.review.no_items')}
            </p>
          {/if}
        </nav>

        <section class="review-modal__detail">
          {#if selected}
            <h3 class="review-modal__statement" data-testid="review-statement">
              {selected.statement}
            </h3>
            {#if selected.trust === 'untrusted'}
              <p class="review-modal__untrusted" data-testid="review-untrusted">
                {$t('control.review.untrusted')} · {$t('control.review.untrusted_text')}
              </p>
            {/if}

            <dl class="review-modal__facts">
              <dt>{$t('control.review.rules')}</dt>
              <dd data-testid="review-rules">
                {#if selected.rules.length > 0}
                  <ul class="review-modal__rules">
                    {#each selected.rules as rule}
                      <li>{rule}</li>
                    {/each}
                  </ul>
                {:else}
                  {$t('control.review.rules_empty')}
                {/if}
              </dd>
              <dt>{$t('control.review.origin')}</dt>
              <dd data-testid="review-origin">{selected.origin}</dd>
              {#if selected.proposal}
                <dt>{$t('control.review.proposal')}</dt>
                <dd data-testid="review-proposal">{selected.proposal}</dd>
              {/if}
            </dl>

            <div class="review-modal__actions">
              <button
                type="button"
                data-testid="review-keep"
                disabled={busy}
                onclick={() => onKeep(selected!.item_id, keepAs())}
              >
                {$t('control.review.keep_finding', { register })}
              </button>
              <button
                type="button"
                data-testid="review-move"
                disabled={busy}
                onclick={() => onKeep(selected!.item_id, moveTo())}
              >
                {$t('control.review.keep_watch', { register })}
              </button>
              <button type="button" data-testid="review-promote" disabled={busy} onclick={promote}>
                {$t('control.review.promote')}
              </button>
              <button
                type="button"
                data-testid="review-edit"
                disabled={busy}
                onclick={() => (editing = true)}
              >
                {$t('control.review.edit')}
              </button>
              <button
                type="button"
                data-testid="review-chat"
                disabled={busy}
                onclick={() => onChat(selected!.item_id)}
              >
                {$t('control.review.chat')}
              </button>
            </div>

            <div class="review-modal__drop">
              <label class="review-modal__field">
                {$t('control.review.drop_reason')}
                <input type="text" data-testid="review-drop-reason" bind:value={dropReason} />
              </label>
              <button
                type="button"
                data-testid="review-drop"
                disabled={busy || !dropReason.trim()}
                onclick={drop}
              >
                {$t('control.review.drop')}
              </button>
            </div>

            {#if editing}
              <div class="review-modal__edit" data-testid="review-edit-panel">
                <label class="review-modal__field">
                  {$t('control.review.instruct_plan')}
                  <textarea rows="2" data-testid="review-instruction" bind:value={instruction}
                  ></textarea>
                </label>
                <button
                  type="button"
                  data-testid="review-instruct"
                  disabled={busy || !instruction.trim()}
                  onclick={edit}
                >
                  {$t('control.review.instruct_again')}
                </button>
              </div>
            {/if}

            {#if readBack}
              <div class="review-modal__read-back" data-testid="review-read-back">
                <h4 class="review-modal__subtitle">{$t('control.review.read_back')}</h4>
                {#if readBack.before}
                  <p class="review-modal__before">{readBack.before}</p>
                {/if}
                {#if readBack.after}
                  <p class="review-modal__after">{readBack.after}</p>
                {/if}
                <div class="review-modal__actions">
                  <button
                    type="button"
                    data-testid="review-read-back-accept"
                    onclick={acceptReadBack}
                  >
                    {$t('control.review.accept_change')}
                  </button>
                  <button
                    type="button"
                    data-testid="review-read-back-instruct"
                    onclick={() => {
                      onInstructAgain();
                      editing = true;
                    }}
                  >
                    {$t('control.review.instruct_again')}
                  </button>
                  <button
                    type="button"
                    data-testid="review-read-back-discard"
                    onclick={discardReadBack}
                  >
                    {$t('control.review.discard')}
                  </button>
                </div>
              </div>
            {/if}

            {#if promoting && drafted}
              <div class="review-modal__draft" data-testid="review-drafted-plan">
                <h4 class="review-modal__subtitle">{$t('control.review.draft_plan')}</h4>
                <p class="review-modal__draft-id mono">{drafted.plan_id}</p>
                {#if drafted.objective}
                  <p>{drafted.objective}</p>
                {/if}
                <ul class="review-modal__rules">
                  {#each drafted.deliverables as deliverable}
                    <li>{deliverable}</li>
                  {/each}
                </ul>
                <label class="review-modal__field">
                  {$t('control.review.instruct_plan')}
                  <textarea
                    rows="2"
                    data-testid="review-plan-instruction"
                    bind:value={planInstruction}
                  ></textarea>
                </label>
                <div class="review-modal__actions">
                  <button
                    type="button"
                    data-testid="review-plan-stage"
                    disabled={busy}
                    onclick={stagePlan}
                  >
                    {$t('control.review.stage_plan')}
                  </button>
                  <button
                    type="button"
                    data-testid="review-plan-redraft"
                    disabled={busy || !planInstruction.trim()}
                    onclick={promote}
                  >
                    {$t('control.review.instruct_again')}
                  </button>
                  <button
                    type="button"
                    data-testid="review-plan-back"
                    onclick={() => (promoting = false)}
                  >
                    {$t('control.review.back')}
                  </button>
                </div>
              </div>
            {/if}
          {:else}
            <p class="review-modal__empty">{$t('control.review.select_item')}</p>
          {/if}

          <footer class="review-modal__footer">
            {#if confirmingRemaining}
              <p class="review-modal__confirm" data-testid="review-remaining-confirm">
                {$t('control.review.accept_remaining_confirm', { count: openItems.length })}
              </p>
              <button
                type="button"
                data-testid="review-remaining-yes"
                onclick={() => {
                  confirmingRemaining = false;
                  onAcceptRemaining();
                }}
              >
                {$t('control.review.accept')}
              </button>
              <button
                type="button"
                data-testid="review-remaining-no"
                onclick={() => (confirmingRemaining = false)}
              >
                {$t('control.review.back')}
              </button>
            {:else}
              <button
                type="button"
                data-testid="review-accept-remaining"
                disabled={busy || openItems.length === 0}
                onclick={() => (confirmingRemaining = true)}
              >
                {$t('control.review.accept_remaining', { count: openItems.length })}
              </button>
            {/if}
            <button type="button" data-testid="review-commit" disabled={busy} onclick={onCommit}>
              {$t('control.review.finish')}
            </button>
          </footer>
        </section>
      </div>
    </div>
  </div>
{/if}

<style>
  .review-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--color-overlay);
    z-index: 50;
  }

  .review-modal__panel {
    width: min(72rem, 100%);
    max-height: 92vh;
    margin: var(--space-3);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-4);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    overflow: hidden;
  }

  .review-modal__head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-2);
  }

  .review-modal__title {
    margin: 0;
    font-size: var(--text-lg);
    flex: 1 1 12rem;
  }

  .review-modal__progress {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .review-modal__body {
    display: grid;
    grid-template-columns: minmax(14rem, 20rem) 1fr;
    gap: var(--space-4);
    min-height: 0;
  }

  .review-modal__items,
  .review-modal__detail {
    min-height: 0;
    overflow-y: auto;
  }

  .review-modal__list,
  .review-modal__rules {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .review-modal__list li {
    margin: 0;
  }

  .review-modal__item {
    width: 100%;
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: var(--space-2);
    align-items: start;
    text-align: left;
    padding: var(--space-2);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    cursor: pointer;
  }

  .review-modal__item.selected {
    border-color: var(--color-primary);
    background-color: var(--color-primary-subtle);
  }

  .review-modal__item-id {
    font-size: var(--text-xs);
  }

  .review-modal__item-text {
    font-size: var(--text-sm);
  }

  .review-modal__item-state {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .review-modal__subtitle {
    margin: 0 0 var(--space-2) 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .review-modal__statement {
    margin: 0 0 var(--space-2) 0;
    font-size: var(--text-base);
  }

  .review-modal__facts {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0 0 var(--space-3) 0;
    font-size: var(--text-sm);
  }

  .review-modal__untrusted {
    margin: 0 0 var(--space-2) 0;
    padding: var(--space-2);
    border-left: 3px solid var(--color-warning);
    background-color: var(--color-warning-subtle);
    font-size: var(--text-xs);
  }

  .review-modal__actions,
  .review-modal__footer,
  .review-modal__read-back,
  .review-modal__draft,
  .review-modal__edit,
  .review-modal__drop {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    align-items: flex-end;
  }

  .review-modal__read-back,
  .review-modal__draft,
  .review-modal__edit,
  .review-modal__drop {
    flex-direction: column;
    align-items: stretch;
    margin-top: var(--space-3);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .review-modal__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    flex: 1 1 12rem;
  }

  .review-modal__field input,
  .review-modal__field textarea {
    padding: var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    font-family: inherit;
  }

  .review-modal__before {
    margin: 0;
    text-decoration: line-through;
    color: var(--color-text-muted);
  }

  .review-modal__after {
    margin: 0;
  }

  .review-modal__footer {
    margin-top: var(--space-4);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-3);
    justify-content: flex-end;
  }

  .review-modal__confirm {
    flex: 1 1 100%;
    margin: 0;
    font-size: var(--text-sm);
  }

  .review-modal__notice {
    margin: 0;
    padding: var(--space-2);
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
    font-size: var(--text-sm);
  }

  .review-modal__empty {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .review-modal button {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  @media (max-width: 768px) {
    .review-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
      max-height: none;
    }

    .review-modal__body {
      grid-template-columns: 1fr;
    }
  }
</style>
