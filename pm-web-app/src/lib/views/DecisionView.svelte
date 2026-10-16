<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import {
    apiClient,
    type DecisionEnvelope,
    type DecisionPart,
    type DecisionInterpretation,
  } from '../api/client';
  import { runtimeStore, refreshDecisionsCount } from '../stores/runtime';

  let decisions = $state<DecisionEnvelope[]>([]);
  let isLoading = $state(true);
  let errorMessage = $state('');

  let showEvidence = $state<Record<string, boolean>>({});
  let selectedOptions = $state<Record<string, string>>({});
  let waiverReasons = $state<Record<string, string>>({});
  let ownWordsTexts = $state<Record<string, string>>({});
  let notes = $state<Record<string, string>>({});
  let isInterpreting = $state<Record<string, boolean>>({});
  let interpretations = $state<Record<string, DecisionInterpretation | null>>({});
  let isOwnWordsMode = $state<Record<string, boolean>>({});
  let isSubmitting = $state<Record<string, boolean>>({});

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  onMount(async () => {
    isMounted = true;
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    await loadDecisions();
  });

  onDestroy(() => {
    isMounted = false;
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  function handleEvent(event: string, data: unknown) {
    if (!isMounted) return;
    if (event === 'decision.opened') {
      const dec = data as DecisionEnvelope;
      if (dec && dec.id) {
        if (!decisions.some((d) => d.id === dec.id)) {
          decisions = [dec, ...decisions];
        }
      } else {
        void loadDecisions();
      }
    } else if (event === 'decision.answered' || event === 'decision.closed') {
      const closeData = data as { id?: string; decision_id?: string };
      const id = closeData?.id || closeData?.decision_id;
      if (id) {
        decisions = decisions.filter((d) => d.id !== id);
      } else {
        void loadDecisions();
      }
    }
  }

  async function loadDecisions() {
    isLoading = true;
    errorMessage = '';
    try {
      decisions = await apiClient.getDecisions();
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to load decisions';
    } finally {
      isLoading = false;
    }
  }

  function toggleEvidence(id: string) {
    showEvidence[id] = !showEvidence[id];
  }

  function selectOption(id: string, key: string) {
    selectedOptions[id] = key;
    isOwnWordsMode[id] = false;
    interpretations[id] = null;
  }

  async function handleWaiver(id: string, action: 'waive' | 'decline') {
    const reason = waiverReasons[id]?.trim() || '';
    if (!reason) return;
    isSubmitting[id] = true;
    try {
      await apiClient.answerDecision(id, { action, reason });
      decisions = decisions.filter((d) => d.id !== id);
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to submit waiver';
    } finally {
      isSubmitting[id] = false;
    }
  }

  async function interpretOwnWords(id: string, part: DecisionPart) {
    const words = ownWordsTexts[id]?.trim() || '';
    if (!words) return;
    isInterpreting[id] = true;
    try {
      const res = await apiClient.interpretDecision(id, words);
      interpretations[id] = res;
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to interpret answer';
    } finally {
      isInterpreting[id] = false;
    }
  }

  async function submitOwnWordsAnswer(id: string, part: DecisionPart) {
    const interp = interpretations[id];
    isSubmitting[id] = true;
    try {
      await apiClient.answerDecision(id, {
        words: ownWordsTexts[id],
        option: interp?.option || 'own_words',
        note: notes[id],
      });
      decisions = decisions.filter((d) => d.id !== id);
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to submit answer';
    } finally {
      isSubmitting[id] = false;
    }
  }

  async function submitOptionAnswer(id: string) {
    const option = selectedOptions[id];
    if (!option) return;
    isSubmitting[id] = true;
    try {
      await apiClient.answerDecision(id, {
        option,
        note: notes[id],
      });
      decisions = decisions.filter((d) => d.id !== id);
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to submit answer';
    } finally {
      isSubmitting[id] = false;
    }
  }

  async function submitWithdraw(id: string) {
    isSubmitting[id] = true;
    try {
      await apiClient.withdrawDecision(id, { reason: 'none fits' });
      decisions = decisions.filter((d) => d.id !== id);
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to withdraw decision';
    } finally {
      isSubmitting[id] = false;
    }
  }

  function handleLater(id: string) {
    // Decision stays open; operator moves away
    decisions = decisions.filter((d) => d.id !== id);
  }
</script>

<div class="decisions-view">
  <div class="decisions-container">
    <header class="decisions-header">
      <h1 class="decisions-heading">
        {$t('decisions.list.title') || 'Decisions'}
        {#if decisions.length > 0}
          <span class="unread-pill">{decisions.length} open</span>
        {/if}
      </h1>
    </header>

    {#if errorMessage}
      <div class="error-banner" role="alert">
        <p>{errorMessage}</p>
        <Button variant="secondary" size="small" onclick={loadDecisions}>Retry</Button>
      </div>
    {/if}

    {#if isLoading}
      <div class="loading-state">
        <Spinner size="2.5rem" label="Loading decisions…" />
      </div>
    {:else if decisions.length === 0}
      <div class="empty-state">
        <p class="empty-title">
          {$t('decisions.list.empty') ||
            'No open decisions. Running plans will ask when a decision is needed.'}
        </p>
      </div>
    {:else}
      <div class="decisions-list">
        {#each decisions as dec (dec.id)}
          <article class="decision-card" data-decision-id={dec.id}>
            {#if dec.is_waiver || dec.id.startsWith('WR-')}
              <!-- Waiver Request View -->
              <div class="waiver-card">
                <header class="card-header">
                  <h2 class="decision-title">
                    {$t('decision.waiver.title', { plan: dec.subject || 'Plan' })}
                  </h2>
                  <span class="waiting-time">
                    {$t('decision.waiting', {
                      subject: dec.scope || 'plan',
                      time: dec.delivered_at || 'now',
                    })}
                  </span>
                </header>

                <div class="waiver-details">
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.gate')}:</span>
                    <span class="detail-value mono"
                      >{dec.waiver_gate || (dec.evidence?.gate as string) || 'Quality gate'}</span
                    >
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.result')}:</span>
                    <span class="detail-value"
                      >{dec.waiver_result ||
                        (dec.evidence?.result as string) ||
                        'Gate failed'}</span
                    >
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.asked_by')}:</span>
                    <span class="detail-value"
                      >{dec.asking_phase || 'execute'} · {dec.asking_role || 'implement'}</span
                    >
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.its_reason')}:</span>
                    <span class="detail-value">{dec.waiver_reason || dec.description || ''}</span>
                  </div>
                </div>

                <div class="waiver-form">
                  <label for="waiver-reason-{dec.id}" class="form-label">
                    {$t('decision.waiver.reason')}
                  </label>
                  <textarea
                    id="waiver-reason-{dec.id}"
                    class="form-textarea"
                    rows="2"
                    placeholder={$t('decision.waiver.reason_required')}
                    bind:value={waiverReasons[dec.id]}
                  ></textarea>
                  {#if !waiverReasons[dec.id]?.trim()}
                    <span class="form-hint">{$t('decision.waiver.reason_required')}</span>
                  {/if}
                </div>

                <footer class="card-actions">
                  <Button variant="secondary" onclick={() => handleLater(dec.id)}>
                    {$t('decision.waiver.later')}
                  </Button>
                  <Button
                    variant="danger"
                    disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
                    onclick={() => handleWaiver(dec.id, 'decline')}
                  >
                    {$t('decision.waiver.decline')}
                  </Button>
                  <Button
                    variant="primary"
                    disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
                    onclick={() => handleWaiver(dec.id, 'waive')}
                  >
                    {$t('decision.waiver.waive')}
                  </Button>
                </footer>
              </div>
            {:else}
              <!-- Standard Decision View -->
              <div class="standard-decision-card">
                <header class="card-header">
                  <h2 class="decision-title">{dec.title}</h2>
                  <div class="badges-row">
                    <span class="badge badge-subject">{dec.subject}</span>
                    <span class="badge badge-phase">{dec.asking_phase} · {dec.asking_role}</span>
                    <span class="badge badge-blocks">{$t(`decision.blocks.${dec.blocks}`)}</span>
                    <span class="badge badge-reversibility badge-{dec.reversibility}">
                      {$t(`decision.reversibility.${dec.reversibility}`)}
                    </span>
                  </div>
                </header>

                {#if dec.description}
                  <p class="decision-description">{dec.description}</p>
                {/if}

                {#if dec.evidence}
                  <div class="evidence-section">
                    <button
                      type="button"
                      class="evidence-toggle"
                      onclick={() => toggleEvidence(dec.id)}
                    >
                      {showEvidence[dec.id] ? '▾ Hide evidence' : '▸ Show evidence'}
                    </button>
                    {#if showEvidence[dec.id]}
                      <div class="evidence-box">
                        <pre class="evidence-json">{JSON.stringify(dec.evidence, null, 2)}</pre>
                      </div>
                    {/if}
                  </div>
                {/if}

                <!-- Parts -->
                {#each dec.parts as part, pIdx (part.part_id || pIdx)}
                  <div class="decision-part">
                    {#if dec.parts.length > 1}
                      <div class="part-position">
                        {$t('decision.part.position', { n: pIdx + 1, total: dec.parts.length })}
                      </div>
                    {/if}

                    <p class="part-question">{part.question}</p>

                    {#if part.recommendation}
                      <div class="recommendation-banner">
                        {$t('decision.recommendation.by', {
                          role: part.recommending_role || dec.asking_role || 'reviewer',
                          option: part.recommendation,
                        })}
                      </div>
                    {/if}

                    <!-- Options list -->
                    <div class="options-list" role="radiogroup" aria-label={part.question}>
                      {#each part.options as opt}
                        <label
                          class="option-card"
                          class:selected={selectedOptions[dec.id] === opt.key &&
                            !isOwnWordsMode[dec.id]}
                          class:recommended={part.recommendation === opt.key}
                        >
                          <input
                            type="radio"
                            name="opt-{dec.id}-{pIdx}"
                            value={opt.key}
                            checked={selectedOptions[dec.id] === opt.key && !isOwnWordsMode[dec.id]}
                            onchange={() => selectOption(dec.id, opt.key)}
                          />
                          <div class="option-content">
                            <div class="option-label-row">
                              <span class="option-label">{opt.label}</span>
                              {#if part.recommendation === opt.key}
                                <span class="recommended-tag">← recommended</span>
                              {/if}
                              {#if opt.quorum_support}
                                <span class="support-badge">
                                  {$t('decision.support.votes', {
                                    votes: opt.quorum_support.votes_count,
                                    total: opt.quorum_support.total_votes,
                                    models: opt.quorum_support.models_count,
                                  })}
                                  · {$t(`decision.support.band.${opt.quorum_support.band}`)}
                                </span>
                              {/if}
                            </div>
                            {#if opt.tradeoff_description}
                              <p class="option-tradeoff">{opt.tradeoff_description}</p>
                            {/if}
                            {#if opt.consequences}
                              <p class="option-consequences">{opt.consequences}</p>
                            {/if}
                            {#if opt.quorum_support?.dissent_summary}
                              <blockquote class="option-dissent">
                                {$t('decision.dissent', {
                                  role: 'reviewer',
                                  reason: opt.quorum_support.dissent_summary,
                                })}
                              </blockquote>
                            {/if}
                          </div>
                        </label>
                      {/each}
                    </div>

                    <!-- Own words section -->
                    {#if part.allow_own_words !== false}
                      <div class="own-words-section">
                        <button
                          type="button"
                          class="btn-toggle-own-words"
                          onclick={() => {
                            isOwnWordsMode[dec.id] = !isOwnWordsMode[dec.id];
                            if (isOwnWordsMode[dec.id]) {
                              selectedOptions[dec.id] = '';
                            }
                          }}
                        >
                          ✎ {$t('decision.answer.own_words')}
                        </button>

                        {#if isOwnWordsMode[dec.id]}
                          <div class="own-words-box">
                            {#if !interpretations[dec.id]}
                              <textarea
                                class="form-textarea"
                                rows="3"
                                placeholder="State your decision in your own words…"
                                bind:value={ownWordsTexts[dec.id]}
                              ></textarea>
                              <div class="own-words-actions">
                                <Button
                                  size="small"
                                  variant="secondary"
                                  disabled={!ownWordsTexts[dec.id]?.trim() ||
                                    isInterpreting[dec.id]}
                                  onclick={() => interpretOwnWords(dec.id, part)}
                                >
                                  {isInterpreting[dec.id]
                                    ? $t('decision.interpret.working', {
                                        role: part.recommending_role || 'interpret',
                                      })
                                    : 'Read back'}
                                </Button>
                              </div>
                            {:else}
                              <div class="readback-box">
                                <p class="readback-text">
                                  {$t('decision.readback.understood', {
                                    reading:
                                      interpretations[dec.id]?.reading ||
                                      interpretations[dec.id]?.words ||
                                      '',
                                  })}
                                </p>
                                <div class="readback-actions">
                                  <Button
                                    size="small"
                                    variant="primary"
                                    onclick={() => submitOwnWordsAnswer(dec.id, part)}
                                  >
                                    {$t('decision.readback.confirm')}
                                  </Button>
                                  <Button
                                    size="small"
                                    variant="secondary"
                                    onclick={() => {
                                      interpretations[dec.id] = null;
                                    }}
                                  >
                                    {$t('decision.readback.edit')}
                                  </Button>
                                  <Button
                                    size="small"
                                    variant="secondary"
                                    onclick={() => {
                                      isOwnWordsMode[dec.id] = false;
                                      interpretations[dec.id] = null;
                                    }}
                                  >
                                    {$t('decision.readback.choose')}
                                  </Button>
                                </div>
                              </div>
                            {/if}
                          </div>
                        {/if}
                      </div>
                    {/if}

                    <!-- Optional note -->
                    {#if part.allow_note}
                      <div class="note-section">
                        <label for="note-{dec.id}" class="form-label">
                          {$t('decision.answer.note')}
                        </label>
                        <input
                          id="note-{dec.id}"
                          type="text"
                          class="form-input"
                          placeholder="Optional note"
                          bind:value={notes[dec.id]}
                        />
                      </div>
                    {/if}
                  </div>
                {/each}

                <footer class="card-actions">
                  <Button
                    variant="primary"
                    disabled={!selectedOptions[dec.id] || isSubmitting[dec.id]}
                    onclick={() => submitOptionAnswer(dec.id)}
                  >
                    {$t('decision.confirm.action')}
                  </Button>
                  <Button
                    variant="secondary"
                    disabled={isSubmitting[dec.id]}
                    onclick={() => submitWithdraw(dec.id)}
                  >
                    {$t('decision.answer.none_fits', { subject: dec.scope || 'plan' })}
                  </Button>
                  <Button variant="secondary" onclick={() => handleLater(dec.id)}>
                    {$t('decision.answer.later')}
                  </Button>
                </footer>
              </div>
            {/if}
          </article>
        {/each}
      </div>
    {/if}
  </div>
</div>

<style>
  .decisions-view {
    padding: var(--space-6) var(--space-4);
    display: flex;
    justify-content: center;
  }

  .decisions-container {
    width: 100%;
    max-width: 800px;
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .decisions-header {
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-4);
  }

  .decisions-heading {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .unread-pill {
    font-size: var(--text-xs);
    font-weight: 600;
    padding: 0.15rem 0.5rem;
    border-radius: var(--radius-full);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .error-banner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-danger-subtle);
    border: 1px solid var(--color-danger-border);
    border-radius: var(--radius-md);
    color: var(--color-danger);
  }

  .loading-state,
  .empty-state {
    padding: var(--space-10) var(--space-4);
    text-align: center;
    color: var(--color-text-muted);
  }

  .decisions-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
  }

  .decision-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    padding: var(--space-5);
  }

  .card-header {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
  }

  .decision-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0;
  }

  .waiting-time {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .badges-row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    align-items: center;
  }

  .badge {
    font-size: var(--text-xs);
    font-weight: 500;
    padding: 0.15rem 0.45rem;
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .badge-subject {
    font-family: var(--font-mono);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .badge-reversibility.badge-reversible {
    background-color: var(--color-success-subtle);
    color: var(--color-success);
  }

  .badge-reversibility.badge-costly {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .badge-reversibility.badge-irreversible {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  .decision-description {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    margin: 0 0 var(--space-4) 0;
  }

  .evidence-section {
    margin-bottom: var(--space-4);
  }

  .evidence-toggle {
    background: none;
    border: none;
    color: var(--color-primary);
    font-size: var(--text-xs);
    font-weight: 600;
    cursor: pointer;
    padding: 0;
  }

  .evidence-box {
    margin-top: var(--space-2);
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    overflow-x: auto;
  }

  .evidence-json {
    margin: 0;
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .decision-part {
    margin-top: var(--space-4);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  .part-position {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin-bottom: var(--space-1);
  }

  .part-question {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-3) 0;
  }

  .recommendation-banner {
    padding: var(--space-2) var(--space-3);
    background-color: var(--color-primary-subtle);
    border-left: 3px solid var(--color-primary);
    color: var(--color-primary);
    font-size: var(--text-xs);
    font-weight: 600;
    margin-bottom: var(--space-3);
    border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  }

  .options-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    margin-bottom: var(--space-4);
  }

  .option-card {
    display: flex;
    align-items: flex-start;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    cursor: pointer;
    transition:
      border-color 0.15s ease,
      background-color 0.15s ease;
  }

  .option-card:hover {
    background-color: var(--color-bg-subtle);
  }

  .option-card.selected {
    border-color: var(--color-primary);
    background-color: var(--color-primary-subtle);
  }

  .option-card.recommended {
    border-left: 3px solid var(--color-primary);
  }

  .option-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .option-label-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .option-label {
    font-weight: 600;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .recommended-tag {
    font-size: var(--text-xs);
    color: var(--color-primary);
    font-weight: 600;
  }

  .support-badge {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .option-tradeoff,
  .option-consequences {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    margin: 0;
  }

  .option-dissent {
    margin: var(--space-2) 0 0 0;
    padding: var(--space-2);
    background-color: var(--color-warning-subtle);
    border-left: 2px solid var(--color-warning);
    font-size: var(--text-xs);
    color: var(--color-warning);
    font-style: italic;
  }

  .own-words-section {
    margin-top: var(--space-3);
  }

  .btn-toggle-own-words {
    background: none;
    border: none;
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    font-weight: 600;
    cursor: pointer;
    padding: 0;
  }

  .btn-toggle-own-words:hover {
    color: var(--color-primary);
  }

  .own-words-box {
    margin-top: var(--space-2);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .readback-box {
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    border-left: 3px solid var(--color-primary);
  }

  .readback-text {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-2) 0;
  }

  .readback-actions,
  .own-words-actions {
    display: flex;
    gap: var(--space-2);
  }

  .note-section {
    margin-top: var(--space-3);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .form-label {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .form-textarea,
  .form-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    font-family: inherit;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    background-color: var(--color-bg-surface);
    box-sizing: border-box;
  }

  .form-textarea:focus,
  .form-input:focus {
    outline: none;
    border-color: var(--color-primary);
  }

  .form-hint {
    font-size: var(--text-xs);
    color: var(--color-warning);
  }

  .card-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: flex-end;
    margin-top: var(--space-5);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  /* Waiver view styles */
  .waiver-details {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
    background-color: var(--color-bg-subtle);
    padding: var(--space-3);
    border-radius: var(--radius-md);
  }

  .detail-row {
    display: flex;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .detail-label {
    font-weight: 600;
    color: var(--color-text-secondary);
    min-width: 5.5rem;
  }

  .detail-value {
    color: var(--color-text-primary);
  }

  .waiver-form {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  @media (max-width: 640px) {
    .decisions-view {
      padding: var(--space-3) var(--space-2);
    }

    .card-actions {
      flex-direction: column;
    }
  }
</style>
