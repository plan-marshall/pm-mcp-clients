<script lang="ts">
  import { t } from '../i18n';
  import type { DraftedPlan, ReviewItem, ReviewReadBack, ReviewResource } from '../api/client';
  import Dialog from './Dialog.svelte';
  import Button from './Button.svelte';
  import Compose from './Compose.svelte';
  import TextField from './TextField.svelte';
  import StatusLabel from './StatusLabel.svelte';

  /**
   * The review of a register (work-actions.adoc § #review): the items beside the one shown, the item
   * with its rules, origin and proposal, and the acts that decide it.
   *
   * It is the design `Dialog`: the title names the register, the item rows are `Button`s, each row's
   * disposition is a `StatusLabel`, the acts follow the hierarchy (_Finish_ primary, the named acts
   * secondary, the ways out ghost), the operator's words are a `Compose` and the drop reason a
   * `TextField`. The order of the items, the first-open selection and the register's logic are the
   * review's own.
   */
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

  let selectedId = $state<string | null>(null);
  let dropReason = $state('');
  let editing = $state(false);
  let promoting = $state(false);
  let confirmingRemaining = $state(false);

  let selected = $derived<ReviewItem | null>(
    review?.items.find((item) => item.item_id === selectedId) ?? null,
  );
  let openItems = $derived(review?.items.filter((item) => item.state === 'open') ?? []);
  let drafted: DraftedPlan | null = $derived(review?.drafted_plan ?? null);
  let readBack: ReviewReadBack | null = $derived(review?.read_back ?? null);

  // The review opens on its first open item: a review is read by working through what is still
  // open, so the panel never opens on a decided item, or on nothing at all. A selection the person
  // made stays theirs — only an empty one, or one the review no longer carries, is replaced.
  $effect(() => {
    if (!open) {
      // A close — whether the modal's own or a parent's — forgets the selection, so the next open
      // selects its own first open item instead of a decided one it still remembers.
      selectedId = null;
      return;
    }
    const items = review?.items ?? [];
    if (selectedId !== null && items.some((item) => item.item_id === selectedId)) {
      return;
    }
    const firstOpen = items.find((item) => item.state === 'open');
    if (firstOpen !== undefined) {
      selectedId = firstOpen.item_id;
    }
  });

  /** The readable label of an item's disposition. */
  function stateLabel(state: string): string {
    return $t(`control.review.state.${state}`);
  }

  /** The tone of a disposition: what still waits for the operator is attention, a promotion succeeds. */
  function toneOf(state: string): 'attention' | 'success' | 'neutral' {
    if (state === 'open') {
      return 'attention';
    }
    return state === 'promoted' ? 'success' : 'neutral';
  }

  function select(item: ReviewItem): void {
    selectedId = item.item_id;
    dropReason = '';
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
    promoting = false;
    editing = true;
  }

  /** Sends the operator's words for the item: the wrapper of `Edit` and `Instruct again`. */
  function instruct(text: string): void {
    if (!selected || !text.trim()) return;
    onEdit(selected.item_id, text.trim());
    editing = true;
  }

  /** Starts the promotion: the button asks for a draft, with no words of the operator's. */
  function promote(): void {
    if (!selected) return;
    editing = false;
    onPromote(selected.item_id, undefined);
    promoting = true;
  }

  /** Asks the planning role to draft the plan again: only the words the operator typed count. */
  function redraft(text: string): void {
    if (!selected || !text.trim()) return;
    onPromote(selected.item_id, text.trim());
    promoting = true;
  }

  function acceptReadBack(): void {
    onAcceptReadBack();
    editing = false;
  }

  function discardReadBack(): void {
    onDiscardReadBack();
    editing = false;
  }

  function stagePlan(): void {
    onStagePlan();
    promoting = false;
  }
</script>

<Dialog
  {open}
  title={$t('control.review.title', { epic: epicId, register })}
  onClose={close}
  testid="review-modal"
>
  <div class="review-modal__head">
    <span class="review-modal__progress" data-testid="review-progress">
      {$t('control.review.progress', { done: review?.decided ?? 0, total: review?.total ?? 0 })}
    </span>
    <span class="review-modal__open" data-testid="review-open-count">
      {$t('control.review.open_count', { count: openItems.length })}
    </span>
  </div>

  {#if notice}
    <p class="review-modal__notice" data-testid="review-notice" role="alert">{notice}</p>
  {/if}

  <div class="review-modal__body">
    <nav class="review-modal__items" aria-label={$t('control.review.items')}>
      <h3 class="review-modal__subtitle">{$t('control.review.items')}</h3>
      {#if review && review.items.length > 0}
        <ul class="review-modal__list">
          {#each review.items as item (item.item_id)}
            <li class="review-modal__row">
              <Button
                variant="secondary"
                size="sm"
                aria-current={selectedId === item.item_id ? 'true' : undefined}
                data-testid={`review-item-${item.item_id}`}
                on:click={() => select(item)}
              >
                <span class="review-modal__item-id mono">{item.item_id}</span>
                <span class="review-modal__item-text">{item.statement}</span>
                <StatusLabel fixed tone={toneOf(item.state)} label={stateLabel(item.state)} />
              </Button>
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
          <Button
            variant="secondary"
            data-testid="review-keep"
            disabled={busy}
            on:click={() => onKeep(selected!.item_id, keepAs())}
          >
            {$t('control.review.keep_finding', { register })}
          </Button>
          <Button
            variant="secondary"
            data-testid="review-move"
            disabled={busy}
            on:click={() => onKeep(selected!.item_id, moveTo())}
          >
            {$t('control.review.keep_watch', { register })}
          </Button>
          <Button
            variant="secondary"
            data-testid="review-promote"
            disabled={busy}
            on:click={promote}
          >
            {$t('control.review.promote')}
          </Button>
          <Button variant="secondary" data-testid="review-edit" disabled={busy} on:click={edit}>
            {$t('control.review.edit')}
          </Button>
          <Button
            variant="secondary"
            data-testid="review-chat"
            disabled={busy}
            on:click={() => onChat(selected!.item_id)}
          >
            {$t('control.review.chat')}
          </Button>
        </div>

        <div class="review-modal__drop">
          <TextField
            label={$t('control.review.drop_reason')}
            testid="review-drop-reason"
            bind:value={dropReason}
            autocomplete="off"
          />
          <Button
            variant="secondary"
            data-testid="review-drop"
            disabled={busy || !dropReason.trim()}
            on:click={drop}
          >
            {$t('control.review.drop')}
          </Button>
        </div>

        {#if editing}
          <div class="review-modal__edit" data-testid="review-edit-panel">
            <Compose
              label={$t('control.review.instruct_plan')}
              sendLabel={$t('control.review.instruct_again')}
              inputTestid="review-instruction"
              submitTestid="review-instruct"
              disabled={busy}
              onsend={instruct}
            />
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
              <Button
                variant="secondary"
                data-testid="review-read-back-accept"
                on:click={acceptReadBack}
              >
                {$t('control.review.accept_change')}
              </Button>
              <Button
                variant="secondary"
                data-testid="review-read-back-instruct"
                on:click={() => {
                  onInstructAgain();
                  editing = true;
                }}
              >
                {$t('control.review.instruct_again')}
              </Button>
              <Button
                variant="secondary"
                data-testid="review-read-back-discard"
                on:click={discardReadBack}
              >
                {$t('control.review.discard')}
              </Button>
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
            <Compose
              label={$t('control.review.instruct_plan')}
              sendLabel={$t('control.review.instruct_again')}
              inputTestid="review-plan-instruction"
              submitTestid="review-plan-redraft"
              disabled={busy}
              onsend={redraft}
            />
            <div class="review-modal__actions">
              <Button
                variant="secondary"
                data-testid="review-plan-stage"
                disabled={busy}
                on:click={stagePlan}
              >
                {$t('control.review.stage_plan')}
              </Button>
              <Button variant="ghost" data-testid="review-plan-back" on:click={() => (promoting = false)}>
                {$t('control.review.back')}
              </Button>
            </div>
          </div>
        {/if}
      {:else}
        <p class="review-modal__empty">{$t('control.review.select_item')}</p>
      {/if}

      {#if confirmingRemaining}
        <p class="review-modal__confirm" data-testid="review-remaining-confirm">
          {$t('control.review.accept_remaining_confirm', { count: openItems.length })}
        </p>
      {/if}
    </section>
  </div>

  <svelte:fragment slot="footer">
    <Button variant="ghost" data-testid="review-close" on:click={close}>
      {$t('common.action.close')}
    </Button>
    {#if confirmingRemaining}
      <Button
        variant="secondary"
        data-testid="review-remaining-yes"
        on:click={() => {
          confirmingRemaining = false;
          onAcceptRemaining();
        }}
      >
        {$t('control.review.accept')}
      </Button>
      <Button
        variant="ghost"
        data-testid="review-remaining-no"
        on:click={() => (confirmingRemaining = false)}
      >
        {$t('control.review.back')}
      </Button>
    {:else}
      <Button
        variant="secondary"
        data-testid="review-accept-remaining"
        disabled={busy || openItems.length === 0}
        on:click={() => (confirmingRemaining = true)}
      >
        {$t('control.review.accept_remaining', { count: openItems.length })}
      </Button>
    {/if}
    <Button variant="primary" data-testid="review-commit" disabled={busy} on:click={onCommit}>
      {$t('control.review.finish')}
    </Button>
  </svelte:fragment>
</Dialog>

<style>
  .review-modal__head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-2);
  }

  .review-modal__progress,
  .review-modal__open {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .review-modal__body {
    display: grid;
    grid-template-columns: minmax(9rem, 12rem) 1fr;
    gap: var(--space-4);
    min-height: 0;
  }

  .review-modal__items,
  .review-modal__detail {
    min-height: 0;
    overflow-y: auto;
    max-block-size: 60vh;
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

  /* A row is a Button; its three parts stand one under the other, and the whole row is the target. */
  .review-modal__row :global(.btn) {
    display: flex;
    inline-size: 100%;
    min-block-size: 44px;
    block-size: auto;
    padding: var(--space-2);
    white-space: normal;
    text-align: start;
    justify-content: flex-start;
  }

  .review-modal__row :global(.btn__content) {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-1);
  }

  .review-modal__row :global(.btn[aria-current='true']) {
    border-color: var(--color-brand);
    background-color: var(--color-brand-subtle);
  }

  .review-modal__item-id {
    font-size: var(--text-xs);
  }

  .review-modal__item-text {
    font-size: var(--text-sm);
    white-space: normal;
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

  .review-modal__before {
    margin: 0;
    text-decoration: line-through;
    color: var(--color-text-muted);
  }

  .review-modal__after {
    margin: 0;
  }

  .review-modal__confirm {
    flex: 1 1 100%;
    margin: var(--space-3) 0 0 0;
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

  @media (max-width: 768px) {
    .review-modal__body {
      grid-template-columns: 1fr;
    }

    .review-modal__items,
    .review-modal__detail {
      max-block-size: none;
    }
  }
</style>
