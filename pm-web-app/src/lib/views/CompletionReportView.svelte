<script lang="ts">
  import { onMount } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import Spinner from '../components/Spinner.svelte';
  import { apiClient, type PlanCompletionReport } from '../api/client';
  import { formatTokens, formatSplit } from '../utils/tokenFormat';
  import { displayState } from '../display';

  interface Props {
    planId: string;
    report?: PlanCompletionReport | null;
    onClose?: () => void;
  }

  let { planId, report = null, onClose }: Props = $props();

  let fetchedData = $state<PlanCompletionReport | null>(null);
  let data = $derived(report ?? fetchedData);
  let isLoading = $state(false);
  let errorMessage = $state('');
  let showRawComponents = $state(false);

  onMount(async () => {
    if (!report && planId) {
      await fetchReport();
    }
  });

  async function fetchReport() {
    isLoading = true;
    errorMessage = '';
    try {
      fetchedData = await apiClient.getPlanCompletion(planId);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('report.completion.load_failed');
    } finally {
      isLoading = false;
    }
  }

  function getDeliverableIcon(state: string): 'state.check' | 'state.dot' {
    return state?.toLowerCase() === 'done' ? 'state.check' : 'state.dot';
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
</script>

<div class="completion-report">
  <div class="completion-report__nav">
    {#if onClose}
      <button
        class="nav-back-btn"
        onclick={onClose}
        aria-label={$t('report.completion.back_to_reports')}
      >
        ← {$t('web.common.back')}
      </button>
    {/if}
  </div>

  {#if isLoading}
    <div class="completion-report__loading">
      <Spinner size="2.5rem" label={$t('report.completion.loading')} />
    </div>
  {:else if errorMessage}
    <div class="completion-report__error" role="alert">
      <p>{errorMessage}</p>
      <Button variant="secondary" onclick={fetchReport}>{$t('web.common.retry')}</Button>
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
        <h1 class="report-title">{data.plan_id}</h1>
        <span class="report-badge report-badge--{data.outcome.toLowerCase()}">
          {$t('overview.ended.outcome', { outcome: data.outcome })}
        </span>
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
          {data.ended || data.ended_at}
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
              >{data.ran || `${Math.round(data.duration_seconds / 60)} min`}</span
            >
          </div>
        {/if}
      </section>

      <!-- Deliverables -->
      <section class="report-section">
        <h2 class="section-title">
          {$t('report.completion.deliverables')}
          {#if data.deliverables && data.deliverables.length > 0}
            <span class="section-badge">
              {$t('report.completion.deliverables_done', {
                done: data.deliverables.filter((d) => d.state?.toLowerCase() === 'done').length,
                total: data.deliverables.length,
              })}
            </span>
          {/if}
        </h2>
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
          <h2 class="section-title">
            {$t('report.completion.replanning')}
            <span class="section-badge">{data.replanning_records.length}</span>
          </h2>
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
          <h2 class="section-title">
            {$t('report.completion.decisions')}
            <span class="section-badge">{data.decision_records.length}</span>
          </h2>
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
          <h2 class="section-title">
            {$t('report.completion.own_calls')}
            <span class="section-badge">{data.own_calls.length}</span>
          </h2>
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
          <h2 class="section-title">
            {$t('report.completion.change_map')}
            <span class="section-badge">
              {displayState('change_map', data.change_map.state, $t)}
            </span>
          </h2>
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
            <h2 class="section-title">{$t('report.completion.tokens')}</h2>
            <button
              class="components-toggle-btn"
              onclick={() => (showRawComponents = !showRawComponents)}
              aria-expanded={showRawComponents}
            >
              {$t(showRawComponents ? 'report.completion.raw_hide' : 'report.completion.raw_show')}
            </button>
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
              <h3 class="breakdown-title">{$t('report.completion.by_phase')}</h3>
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
              <h3 class="breakdown-title">{$t('report.completion.by_role')}</h3>
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
              <h3 class="breakdown-title">{$t('report.completion.by_model')}</h3>
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
    max-width: 900px;
    margin: 0 auto;
    padding: var(--space-6) var(--space-4);
  }

  .nav-back-btn {
    background: none;
    border: none;
    color: var(--color-primary);
    font-size: var(--text-sm);
    font-weight: 500;
    cursor: pointer;
    padding: var(--space-2) 0;
    margin-bottom: var(--space-4);
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
  }

  .nav-back-btn:hover {
    text-decoration: underline;
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

  .report-badge {
    display: inline-flex;
    align-items: center;
    padding: var(--space-1) var(--space-3);
    border-radius: var(--radius-full);
    font-size: var(--text-xs);
    font-weight: 600;
    text-transform: uppercase;
  }

  .report-badge--merged {
    background: var(--color-success-subtle);
    color: var(--color-success-text);
    border: 1px solid var(--color-success-border);
  }

  .report-badge--no_change,
  .report-badge--completed {
    background: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    border: 1px solid var(--color-border);
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

  .components-toggle-btn {
    background: none;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    cursor: pointer;
  }

  .components-toggle-btn:hover {
    background: var(--color-bg-subtle);
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
