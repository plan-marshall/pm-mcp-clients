<script lang="ts">
  import { onMount, onDestroy, tick } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { errorCodeOf } from '../api/errors';
  import StatusLabel from '../components/StatusLabel.svelte';
  import { apiClient, type ChangeSizeRecord, type PlanCompletionReport } from '../api/client';
  import { formatSplit, formatTokens, formatDuration, formatTime } from '../display/format';
  import { displayState } from '../display';

  interface Props {
    planId: string;
    report?: PlanCompletionReport | null;
    titleLevel?: 1 | 2;
    onClose?: () => void;
  }

  let { planId, report = null, titleLevel = 1, onClose }: Props = $props();

  let fetchedData = $state<PlanCompletionReport | null>(null);
  let data = $derived(report ?? fetchedData);
  let isLoading = $state(false);
  let errorMessage = $state<string | null>(null);
  let showRawComponents = $state(false);

  /** The rendered subtree, so the title can be found and focused. */
  let rootEl = $state<HTMLElement | null>(null);
  /** The control that opened the report; focus returns to it when the report closes. */
  let restoreFocusTo: HTMLElement | null = null;

  /**
   * Moves focus to the report's current heading, so an opened report is entered at its head and
   * the loading and error states keep focus on the visible heading instead of the body.
   */
  function focusReportHeading(): void {
    const heading = rootEl?.querySelector<HTMLElement>('.report-title, .error-state__title');
    heading?.focus();
  }

  /**
   * Returns focus to the invoking control before the report closes.
   */
  function closeReport(): void {
    restoreFocusTo?.focus();
    onClose?.();
  }

  onMount(() => {
    restoreFocusTo =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    focusReportHeading();
    if (!report && planId) {
      void fetchReport();
    }
  });

  onDestroy(() => {
    // Restore only when the report still owns focus, or focus fell to the body: a Back click
    // already moved it to the invoking control, and focusing again here would move it twice —
    // or steal it from wherever the operator went next.
    const active = document.activeElement;
    const insideReport = active instanceof HTMLElement && (rootEl?.contains(active) ?? false);
    if (insideReport || active === null || active === document.body) {
      restoreFocusTo?.focus();
    }
  });

  async function fetchReport() {
    isLoading = true;
    errorMessage = null;
    // The loading heading replaces the previous state's heading; take focus to it, so the fetch
    // does not leave focus on the body while it runs.
    await tick();
    focusReportHeading();
    try {
      fetchedData = await apiClient.getPlanCompletion(planId);
    } catch (err: unknown) {
      errorMessage = errorCodeOf(err);
    } finally {
      isLoading = false;
    }
    // A fetched report (or the error state) replaces the loading placeholder; take focus to the
    // visible heading now, so focus does not fall to the body when the placeholder is removed.
    await tick();
    if (data || errorMessage !== null) {
      focusReportHeading();
    }
  }

  function getDeliverableIcon(state: string): 'state.check' | 'state.dot' {
    return state?.toLowerCase() === 'done' ? 'state.check' : 'state.dot';
  }

  /**
   * @param outcome the plan's outcome as the runtime names it
   * @returns the state colour the design system gives that outcome
   */
  function outcomeTone(outcome: string): 'success' | 'neutral' | 'attention' | 'danger' {
    const value = outcome?.toLowerCase();
    if (value === 'merged' || value === 'completed') return 'success';
    if (value === 'cancelled' || value === 'failed') return 'danger';
    return 'neutral';
  }

  function formatDecisionBy(d: { by: string; channel?: string; device?: string }): string {
    if (d.by?.toLowerCase() === 'operator') {
      const parts = [$t('report.completion.by_you')];
      if (d.channel) parts.push(d.channel);
      if (d.device) parts.push(d.device);
      return parts.join(', ');
    }
    return d.by || $t('report.completion.delegated');
  }

  function judgmentLabel(call: { judgment: string; conflict_target?: string }): string {
    if (call.judgment?.toLowerCase() === 'conflicts') {
      return $t('report.completion.own_call_conflicts', { item: call.conflict_target || '—' });
    }
    if (call.judgment?.toLowerCase() === 'consistent') {
      return $t('report.completion.own_call_consistent');
    }
    return call.judgment;
  }

  /**
   * The size of the plan's change at its last capture, as the plan view states it. The client adds
   * nothing up and shows no line where the plan has no measured size.
   *
   * @param change the size the runtime served
   * @returns the catalogue's one-line form
   */
  function changeText(change: ChangeSizeRecord): string {
    return $t('plan.change.size', {
      files: change.files,
      insertions: change.insertions,
      deletions: change.deletions,
      unplanned: change.unplanned ?? 0,
    });
  }
</script>

<div class="completion-report" bind:this={rootEl}>
  <div class="completion-report__nav">
    {#if onClose}
      <Button variant="ghost" size="small" icon="act.back" onclick={closeReport}>
        {$t('web.common.back')}
      </Button>
    {/if}
  </div>

  {#if isLoading}
    <div class="completion-report__loading">
      <svelte:element
        this={`h${titleLevel}`}
        class="report-title report-title--loading"
        tabindex="-1"
      >
        {$t('report.completion.loading')}
      </svelte:element>
      <span aria-hidden="true"><Skeleton shape="card" rows={2} /></span>
    </div>
  {:else if errorMessage !== null}
    <div class="completion-report__error" data-testid="completion-report-error">
      <ErrorState
        code={errorMessage}
        actionLabel={$t('common.action.retry')}
        onAct={fetchReport}
        titleLevel={titleLevel}
      />
    </div>
  {:else if data}
    <!-- Path breadcrumb -->
    {#if data.from}
      <div class="report-breadcrumb mono">
        {#if data.from.project}
          <span class="breadcrumb-item">{data.from.project}</span>
        {/if}
        {#if data.from.epic}
          <span class="breadcrumb-sep">›</span>
        <span class="breadcrumb-item">{data.from.epic}</span>
        {/if}
        <span class="breadcrumb-sep">›</span>
        <span class="breadcrumb-item font-semibold">{data.plan_id}</span>
      </div>
    {/if}

    <!-- Header bar -->
    <header class="report-header">
      <div class="report-header__headline">
        <svelte:element this={`h${titleLevel}`} class="report-title" tabindex="-1"
          >{data.plan_id}</svelte:element
        >
        <StatusLabel tone={outcomeTone(data.outcome)} label={$t('overview.ended.outcome', { outcome: data.outcome })} />
        {#if data.pull_request_number}
          <a
            class="pr-link mono"
            href={data.pull_request_url || `#`}
            target="_blank"
            rel="noopener noreferrer"
          >
            #{data.pull_request_number}
          </a>
        {/if}
        <span class="ended-time text-muted">
          {data.ended || formatTime(data.ended_at, 'short', $currentLocale)}
        </span>
      </div>
    </header>

    <div class="report-content">
      <!-- Summary section -->
      <section class="report-section summary-grid">
        <div class="summary-row">
          <span class="summary-label">{$t('report.completion.outcome')}</span>
          <div class="summary-value">
            <span
              >{data.terminal_reason ||
                $t('overview.ended.outcome', { outcome: data.outcome })}</span
            >
            {#if data.pull_request_url}
              <a
                class="pr-url-link"
                href={data.pull_request_url}
                target="_blank"
                rel="noopener noreferrer"
              >
                <code>{data.pull_request_url}</code>
              </a>
            {/if}
          </div>
        </div>

        <div class="summary-row">
          <span class="summary-label">{$t('report.completion.reason')}</span>
          <span class="summary-value">{data.reason || '—'}</span>
        </div>

        {#if data.ran || data.duration_seconds > 0}
          <div class="summary-row">
            <span class="summary-label">{$t('report.completion.ran')}</span>
            <span class="summary-value"
              >{data.ran ||
                formatDuration(data.duration_seconds * 1000, $currentLocale)}</span
            >
          </div>
        {/if}

        {#if data.change}
          <div class="summary-row" data-testid="completion-change">
            <span class="summary-label">{$t('report.completion.change')}</span>
            <span class="summary-value">{changeText(data.change)}</span>
          </div>
        {/if}
      </section>

      <!-- Deliverables -->
      <section class="report-section">
        <svelte:element this={`h${titleLevel + 1}`} class="section-title">
          {$t('report.completion.deliverables')}
          {#if data.deliverables && data.deliverables.length > 0}
            <span class="section-badge">
              {$t('report.completion.deliverables_done', {
                done: data.deliverables.filter((d) => d.state?.toLowerCase() === 'done').length,
                total: data.deliverables.length,
              })}
            </span>
          {/if}
        </svelte:element>
        {#if !data.deliverables || data.deliverables.length === 0}
          <p class="empty-text">{$t('report.completion.deliverables_none', { phase: 'refine' })}</p>
        {:else}
          <ul class="deliverables-list">
            {#each data.deliverables as d}
              <li class="deliverable-item deliverable-item--{d.state?.toLowerCase()}">
                <span class="deliverable-mark" aria-hidden="true"
                  ><Icon name={getDeliverableIcon(d.state)} size={16} /></span
                >
                <span class="deliverable-state mono">{displayState('task', d.state, $t)}</span>
                <span class="deliverable-title">{d.name || d.title}</span>
              </li>
            {/each}
          </ul>
        {/if}
      </section>

      <!-- Re-planning -->
      {#if data.replanning_records && data.replanning_records.length > 0}
        <section class="report-section">
          <svelte:element this={`h${titleLevel + 1}`} class="section-title">
            {$t('report.completion.replanning')}
            <span class="section-badge">{data.replanning_records.length}</span>
          </svelte:element>
          <ul class="replanning-list">
            {#each data.replanning_records as r}
              <li class="replanning-item">
                <span class="replanning-trigger font-medium">{r.trigger}</span>
                {#if r.time}
                  <span class="replanning-time text-muted">({r.time})</span>
                {/if}
                <span class="replanning-change">: {r.change}</span>
              </li>
            {/each}
          </ul>
        </section>
      {/if}

      <!-- Decisions -->
      {#if data.decision_records && data.decision_records.length > 0}
        <section class="report-section">
          <svelte:element this={`h${titleLevel + 1}`} class="section-title">
            {$t('report.completion.decisions')}
            <span class="section-badge">{data.decision_records.length}</span>
          </svelte:element>
          <ul class="decisions-list">
            {#each data.decision_records as dec}
              <li class="decision-item">
                <span class="decision-id mono font-semibold">{dec.id}</span>
                {#if dec.question || dec.text}
                  <span class="decision-q">{dec.question || dec.text}</span>
                {/if}
                <span class="decision-by text-muted">({formatDecisionBy(dec)})</span>
              </li>
            {/each}
          </ul>
        </section>
      {/if}

      <!-- Own calls -->
      {#if data.own_calls && data.own_calls.length > 0}
        <section class="report-section">
          <svelte:element this={`h${titleLevel + 1}`} class="section-title">
            {$t('report.completion.own_calls')}
            <span class="section-badge">{data.own_calls.length}</span>
          </svelte:element>
          <ul class="own-calls-list">
            {#each data.own_calls as call}
              <li class="own-call-item">
                <div class="own-call-head">
                  <span class="own-call-kind">
                    {$t('report.completion.own_call_kind', { kind: call.kind })}
                  </span>
                  <span class="own-call-chip own-call-chip--{call.judgment?.toLowerCase()}">
                    {judgmentLabel(call)}
                  </span>
                </div>
                <p class="own-call-description">{call.description}</p>
                {#if call.limit || call.ends_when}
                  <p class="own-call-detail text-muted">
                    {#if call.limit}
                      <span>{$t('report.completion.own_call_limit', { limit: call.limit })}</span>
                    {/if}
                    {#if call.limit && call.ends_when}
                      <span class="own-call-sep">·</span>
                    {/if}
                    {#if call.ends_when}
                      <span
                        >{$t('report.completion.own_call_ends', {
                          condition: call.ends_when,
                        })}</span
                      >
                    {/if}
                  </p>
                {/if}
              </li>
            {/each}
          </ul>
        </section>
      {/if}

      <!-- Change map -->
      {#if data.change_map}
        <section class="report-section">
          <svelte:element this={`h${titleLevel + 1}`} class="section-title">
            {$t('report.completion.change_map')}
            <span class="section-badge">
              {displayState('change_map', data.change_map.state, $t)}
            </span>
          </svelte:element>
          {#if data.change_map.cause}
            <p class="change-map-cause text-muted">
              {$t('report.completion.change_map_cause', { cause: data.change_map.cause })}
            </p>
          {/if}
          <pre class="change-map-diagram mono">{data.change_map.diagram}</pre>
        </section>
      {/if}

      <!-- Tokens section -->
      {#if data.token_usage}        {@const usage = data.token_usage}
        <section class="report-section tokens-section">
          <div class="tokens-header">
            <svelte:element this={`h${titleLevel + 1}`} class="section-title">
              {$t('report.completion.tokens')}
            </svelte:element>
            <Button
              variant="secondary"
              size="small"
              onclick={() => (showRawComponents = !showRawComponents)}
              aria-expanded={showRawComponents}
            >
              {$t(showRawComponents ? 'report.completion.raw_hide' : 'report.completion.raw_show')}
            </Button>
          </div>

          <!-- Total & Coverage -->
          <div class="tokens-total-card">
            <div class="total-row">
              <span class="total-label">{$t('report.completion.total')}</span>
              <span class="total-value mono font-semibold">
                {formatSplit(
                  usage.total,
                  usage.input,
                  usage.output,
                  usage.is_floor,
                  $currentLocale,
                )}
                {#if data.ran}
                  <span class="total-ran text-muted">· {data.ran}</span>
                {/if}
              </span>
            </div>
            <div class="coverage-row text-muted">
              <span class="coverage-label">{$t('report.completion.coverage')}</span>
              <span>
                {#if usage.tasks_total > usage.tasks_measured}
                  {$t('common.tokens.coverage', {
                    unmeasured: usage.tasks_total - usage.tasks_measured,
                    total: usage.tasks_total,
                    reason: usage.unmeasured_reason || 'none',
                  })}
                {:else}
                  {$t('report.completion.tasks_measured', {
                    count: usage.tasks_total,
                    total: usage.tasks_total,
                  })}
                {/if}
              </span>
            </div>
          </div>

          <!-- Breakdown by Phase -->
          {#if usage.by_phase && usage.by_phase.length > 0}
            <div class="breakdown-group">
              <svelte:element this={`h${titleLevel + 2}`} class="breakdown-title">
                {$t('report.completion.by_phase')}
              </svelte:element>
              <table class="breakdown-table">
                <tbody>
                  {#each usage.by_phase as item}
                    <tr>
                      <td class="col-name mono">{item.name}</td>
                      <td class="col-value mono">
                        {formatSplit(
                          item.total,
                          item.input,
                          item.output,
                          item.floor,
                          $currentLocale,
                        )}
                      </td>
                      <td class="col-extra text-muted">
                        {#if item.unmeasured > 0}
                          {item.unmeasured} not measured
                        {/if}
                      </td>
                    </tr>
                    {#if showRawComponents && item.components}
                      <tr class="raw-components-row">
                        <td colspan="3" class="mono text-muted">
                          input {formatTokens(item.components.input, false, $currentLocale)} · cache read
                          {formatTokens(item.components.cache_read, false, $currentLocale)} · cache write
                          {formatTokens(item.components.cache_write, false, $currentLocale)} · output
                          {formatTokens(item.components.output, false, $currentLocale)} · reasoning {formatTokens(
                            item.components.reasoning,
                            false,
                            $currentLocale,
                          )}
                        </td>
                      </tr>
                    {/if}
                  {/each}
                </tbody>
              </table>
            </div>
          {/if}

          <!-- Breakdown by Role -->
          {#if usage.by_role && usage.by_role.length > 0}
            <div class="breakdown-group">
              <svelte:element this={`h${titleLevel + 2}`} class="breakdown-title">
                {$t('report.completion.by_role')}
              </svelte:element>
              <table class="breakdown-table">
                <tbody>
                  {#each usage.by_role as item}
                    <tr>
                      <td class="col-name mono">{item.name}</td>
                      <td class="col-pin text-muted mono">
                        {#if item.harness_model}
                          {item.harness_model}{item.effort ? ` · ${item.effort}` : ''}
                        {/if}
                      </td>
                      <td class="col-value mono">
                        {formatSplit(
                          item.total,
                          item.input,
                          item.output,
                          item.floor,
                          $currentLocale,
                        )}
                      </td>
                      <td class="col-extra text-muted">
                        {#if item.unmeasured > 0}
                          {item.unmeasured} not measured
                        {/if}
                      </td>
                    </tr>
                    {#if showRawComponents && item.components}
                      <tr class="raw-components-row">
                        <td colspan="4" class="mono text-muted">
                          input {formatTokens(item.components.input, false, $currentLocale)} · cache read
                          {formatTokens(item.components.cache_read, false, $currentLocale)} · cache write
                          {formatTokens(item.components.cache_write, false, $currentLocale)} · output
                          {formatTokens(item.components.output, false, $currentLocale)} · reasoning {formatTokens(
                            item.components.reasoning,
                            false,
                            $currentLocale,
                          )}
                        </td>
                      </tr>
                    {/if}
                  {/each}
                </tbody>
              </table>
            </div>
          {/if}

          <!-- Breakdown by Model -->
          {#if usage.by_model && usage.by_model.length > 0}
            <div class="breakdown-group">
              <svelte:element this={`h${titleLevel + 2}`} class="breakdown-title">
                {$t('report.completion.by_model')}
              </svelte:element>
              <table class="breakdown-table">
                <tbody>
                  {#each usage.by_model as item}
                    <tr>
                      <td class="col-name mono">{item.name}</td>
                      <td class="col-value mono">
                        {formatSplit(
                          item.total,
                          item.input,
                          item.output,
                          item.floor,
                          $currentLocale,
                        )}
                      </td>
                      <td class="col-extra text-muted">
                        {#if item.unmeasured > 0}
                          {item.unmeasured} not measured
                        {/if}
                      </td>
                    </tr>
                    {#if showRawComponents && item.components}
                      <tr class="raw-components-row">
                        <td colspan="3" class="mono text-muted">
                          input {formatTokens(item.components.input, false, $currentLocale)} · cache read
                          {formatTokens(item.components.cache_read, false, $currentLocale)} · cache write
                          {formatTokens(item.components.cache_write, false, $currentLocale)} · output
                          {formatTokens(item.components.output, false, $currentLocale)} · reasoning {formatTokens(
                            item.components.reasoning,
                            false,
                            $currentLocale,
                          )}
                        </td>
                      </tr>
                    {/if}
                  {/each}
                </tbody>
              </table>
            </div>
          {/if}

          <!-- Waste breakdown -->
          {#if (usage.total_waste_tokens && usage.total_waste_tokens > 0) || (usage.waste_records && usage.waste_records.length > 0)}
            <div class="breakdown-group waste-group">
              <div class="waste-header">
                <span class="breakdown-title">{$t('report.completion.waste')}</span>
                <span class="waste-total mono font-semibold">
                  {$t('report.completion.tokens_count', {
                    tokens: formatTokens(usage.total_waste_tokens, false, $currentLocale),
                  })}
                  {#if usage.waste_share_percent > 0}
                    <span class="text-muted">
                      ({$t('report.completion.waste_share', {
                        percent: usage.waste_share_percent,
                      })})
                    </span>
                  {/if}
                </span>
              </div>
              <ul class="waste-list">
                {#each usage.waste_records as w}
                  <li class="waste-item">
                    <span class="waste-class">
                      {#if w.waste_class === 'no_product'}
                        {$t('report.completion.produced_nothing')}
                      {:else}
                        {w.waste_class}
                      {/if}
                    </span>
                    <span class="waste-tokens mono"
                      >{formatTokens(w.tokens, false, $currentLocale)}</span
                    >
                    <span class="waste-cause text-muted"
                      >{w.cause_summary ||
                        $t('report.completion.tasks_count', { count: w.tasks_count })}</span
                    >
                  </li>
                {/each}
              </ul>
            </div>
          {/if}
        </section>
      {/if}
    </div>
  {/if}
</div>

<style>
  .completion-report {
    inline-size: 100%;
    min-inline-size: 0;
  }

  .completion-report__loading {
    display: flex;
    justify-content: center;
    padding: var(--space-10) 0;
  }

  .report-breadcrumb {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
    margin-bottom: var(--space-2);
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .breadcrumb-sep {
    color: var(--color-text-muted);
  }

  .report-header {
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-4);
    margin-bottom: var(--space-6);
  }

  .report-header__headline {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .report-title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .pr-link {
    color: var(--color-primary);
    font-weight: 600;
    text-decoration: none;
  }

  .pr-link:hover {
    text-decoration: underline;
  }

  .ended-time {
    font-size: var(--text-sm);
  }

  .report-section {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-5);
    margin-bottom: var(--space-5);
  }

  .section-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-4) 0;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .section-badge {
    font-size: var(--text-xs);
    font-weight: 500;
    color: var(--color-text-muted);
  }

  .summary-grid {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .summary-row {
    display: grid;
    grid-template-columns: 140px 1fr;
    gap: var(--space-4);
    align-items: baseline;
  }

  .summary-label {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
    font-weight: 500;
  }

  .summary-value {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .pr-url-link {
    color: var(--color-primary);
    font-size: var(--text-xs);
    word-break: break-all;
  }

  .deliverables-list,
  .replanning-list,
  .decisions-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .deliverable-item {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    font-size: var(--text-sm);
  }

  .deliverable-mark {
    font-weight: bold;
    width: 1.25rem;
    text-align: center;
  }

  .deliverable-item--done .deliverable-mark {
    color: var(--color-success);
  }

  .deliverable-state {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    width: 60px;
  }

  .replanning-item,
  .decision-item {
    font-size: var(--text-sm);
    line-height: 1.5;
  }

  .own-calls-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .own-call-item {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .own-call-head {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .own-call-kind {
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .own-call-chip {
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-full);
    background: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
  }

  .own-call-chip--consistent {
    background: var(--color-success-subtle);
    color: var(--color-success-text);
    border-color: var(--color-success-border);
  }

  .own-call-chip--conflicts {
    background: var(--color-danger-subtle);
    color: var(--color-danger-text);
    border-color: var(--color-danger-border);
  }

  .own-call-description {
    margin: 0;
    font-size: var(--text-sm);
  }

  .own-call-detail {
    margin: 0;
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
    font-size: var(--text-xs);
  }

  .own-call-sep {
    color: var(--color-text-muted);
  }

  .change-map-cause {
    margin: 0 0 var(--space-2) 0;
    font-size: var(--text-sm);
  }

  .change-map-diagram {
    margin: 0;
    padding: var(--space-3);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    background: var(--color-bg-subtle);
    font-size: var(--text-xs);
    line-height: 1.5;
    white-space: pre;
    overflow-x: auto;
  }

  .tokens-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-4);
  }

  .tokens-total-card {
    background: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
    margin-bottom: var(--space-5);
  }

  .total-row {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: var(--space-2);
  }

  .total-label {
    font-size: var(--text-base);
    font-weight: 600;
  }

  .total-value {
    font-size: var(--text-base);
  }

  .breakdown-group {
    margin-top: var(--space-4);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-3);
  }

  .breakdown-title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-2) 0;
  }

  .breakdown-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
  }

  .breakdown-table td {
    padding: var(--space-1) 0;
  }

  .col-name {
    width: 25%;
  }

  .col-pin {
    width: 30%;
    font-size: var(--text-xs);
  }

  .col-value {
    width: 30%;
  }

  .col-extra {
    width: 15%;
    text-align: right;
    font-size: var(--text-xs);
  }

  .raw-components-row td {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
    background: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
  }

  .waste-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: var(--space-2);
  }

  .waste-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .waste-item {
    display: grid;
    grid-template-columns: 180px 100px 1fr;
    font-size: var(--text-sm);
    align-items: baseline;
  }

  @media (max-width: 640px) {
    .summary-row {
      grid-template-columns: 100px 1fr;
    }

    .breakdown-table,
    .breakdown-table tbody,
    .breakdown-table tr,
    .breakdown-table td {
      display: block;
      width: 100%;
    }

    .waste-item {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }
  }
</style>
