<script lang="ts">
  import { onMount, onDestroy, tick } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Card from '../components/Card.svelte';
  import Chip from '../components/Chip.svelte';
  import DataTable, { type DataTableColumn, type DataTableRow } from '../components/DataTable.svelte';
  import ExternalLink from '../components/ExternalLink.svelte';
  import Icon from '../components/Icon.svelte';
  import KeyValueList, { type KeyValueItem } from '../components/KeyValueList.svelte';
  import KindMark from '../components/KindMark.svelte';
  import Link from '../components/Link.svelte';
  import PrChip from '../components/PrChip.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { errorCodeOf } from '../api/errors';
  import StatusLabel from '../components/StatusLabel.svelte';
  import { apiClient, type ChangeSizeRecord, type PlanCompletionReport } from '../api/client';
  import { formatSplit, formatTokens, formatDuration, formatTime } from '../display/format';
  import { displayState, formatChangeSize } from '../display';
  import { formatAddress, pathForRoute } from '../routes';
  import { openAddress } from '../stores/runtime';

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
    restoreFocusTo = document.activeElement instanceof HTMLElement ? document.activeElement : null;
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
    // Where the act left the focus, so a report that resolves later knows whether the operator has
    // moved on meanwhile.
    const actFocus = document.activeElement;
    try {
      fetchedData = await apiClient.getPlanCompletion(planId);
    } catch (err: unknown) {
      errorMessage = errorCodeOf(err);
    } finally {
      isLoading = false;
    }
    // A fetched report (or the error state) replaces the loading placeholder. Take the visible
    // heading only while the focus is still where the act left it, or where the removed placeholder
    // dropped it; a focus the operator moved meanwhile stays where they put it.
    await tick();
    const active = document.activeElement;
    const stillWhereTheActLeftIt =
      active === actFocus || active === null || active === document.body;
    if ((data || errorMessage !== null) && stillWhereTheActLeftIt) {
      focusReportHeading();
    }
  }

  const sectionLevel = $derived((titleLevel + 1) as 2 | 3);

  function getDeliverableIcon(state: string): 'state.check' | 'state.dot' | 'act.cancel' {
    const normalized = state?.toLowerCase().trim();
    if (normalized === 'done') return 'state.check';
    if (normalized === 'dropped') return 'act.cancel';
    return 'state.dot';
  }

  function judgmentTone(judgment: string): 'success' | 'danger' | 'neutral' {
    const normalized = judgment?.toLowerCase();
    if (normalized === 'consistent') return 'success';
    if (normalized === 'conflicts') return 'danger';
    return 'neutral';
  }

  function changeMapTone(state: string): 'success' | 'attention' | 'neutral' {
    const normalized = state?.toLowerCase();
    if (normalized === 'ok') return 'success';
    if (normalized === 'partial') return 'attention';
    return 'neutral';
  }

  const summaryItems = $derived.by<KeyValueItem[]>(() => {
    if (!data) return [];
    const rows: KeyValueItem[] = [
      {
        label: $t('report.completion.outcome'),
        value: data.terminal_reason || $t('overview.ended.outcome', { outcome: data.outcome }),
      },
      {
        label: $t('report.completion.reason'),
        value: data.reason || $t('report.completion.none'),
      },
    ];
    if (data.ran || data.duration_seconds > 0) {
      rows.push({
        label: $t('report.completion.ran'),
        value: data.ran || formatDuration(data.duration_seconds * 1000, $currentLocale),
      });
    }
    if (data.change) {
      rows.push({
        label: $t('report.completion.change'),
        value: changeText(data.change),
      });
    }
    return rows;
  });

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
    return formatChangeSize(change, $t);
  }

  function phaseHref(id: string, phase: string): string {
    return (
      formatAddress({ route: 'plan', planId: id, mode: 'ended', node: phase }) ??
      pathForRoute('plan', { planId: id }) ??
      ''
    );
  }

  function formatBreakdownSplit(
    total: number,
    input: number,
    output: number,
    floor: boolean,
    unmeasured: number,
  ): string {
    if (total === 0 && unmeasured > 0) {
      return $t('state.model.not_measured');
    }
    return formatSplit(total, input, output, floor, $currentLocale);
  }

  function formatRawComponentsRow(components: {
    input: number;
    cache_read: number;
    cache_write: number;
    output: number;
    reasoning: number;
  }): string {
    return $t('report.completion.component_row', {
      input: formatTokens(components.input, false, $currentLocale),
      cache_read: formatTokens(components.cache_read, false, $currentLocale),
      cache_write: formatTokens(components.cache_write, false, $currentLocale),
      output: formatTokens(components.output, false, $currentLocale),
      reasoning: formatTokens(components.reasoning, false, $currentLocale),
    });
  }

  const tokenSummaryItems = $derived.by<KeyValueItem[]>(() => {
    const usage = data?.token_usage;
    if (!usage) return [];
    const totalValue =
      usage.tasks_total > 0 && usage.tasks_measured === 0
        ? $t('state.model.not_measured')
        : `${formatSplit(usage.total, usage.input, usage.output, usage.is_floor, $currentLocale)}${
            data?.ran ? ` · ${data.ran}` : ''
          }`;
    const coverageValue =
      usage.tasks_total > usage.tasks_measured
        ? $t('common.tokens.coverage', {
            unmeasured: usage.tasks_total - usage.tasks_measured,
            total: usage.tasks_total,
            reason: usage.unmeasured_reason || 'none',
          })
        : $t('report.completion.tasks_measured', {
            count: usage.tasks_total,
            total: usage.tasks_total,
          });
    return [
      { label: $t('report.completion.total'), value: totalValue },
      { label: $t('report.completion.coverage'), value: coverageValue },
    ];
  });

  const roleColumns = $derived<DataTableColumn[]>([
    { id: 'name', header: $t('report.completion.by_role'), width: '10rem' },
    { id: 'pin', header: $t('report.completion.by_model'), width: '14rem' },
    { id: 'tokens', header: $t('report.completion.tokens'), width: '16rem', numeric: true },
    { id: 'coverage', header: $t('report.completion.coverage'), width: '10rem', numeric: true },
  ]);

  const roleRows = $derived<DataTableRow[]>(
    (data?.token_usage?.by_role ?? []).map((item) => ({
      id: item.name,
      cells: {
        name: item.name,
        pin: item.harness_model ? `${item.harness_model}${item.effort ? ` · ${item.effort}` : ''}` : '',
        tokens: formatBreakdownSplit(
          item.total,
          item.input,
          item.output,
          item.floor,
          item.unmeasured,
        ),
        coverage: item.unmeasured > 0 ? $t('report.tokens.unmeasured', { count: item.unmeasured }) : '',
      },
    })),
  );

  const modelColumns = $derived<DataTableColumn[]>([
    { id: 'name', header: $t('report.completion.by_model'), width: '16rem' },
    { id: 'tokens', header: $t('report.completion.tokens'), width: '16rem', numeric: true },
    { id: 'coverage', header: $t('report.completion.coverage'), width: '10rem', numeric: true },
  ]);

  const modelRows = $derived<DataTableRow[]>(
    (data?.token_usage?.by_model ?? []).map((item) => ({
      id: item.name,
      cells: {
        name: item.name,
        tokens: formatBreakdownSplit(
          item.total,
          item.input,
          item.output,
          item.floor,
          item.unmeasured,
        ),
        coverage: item.unmeasured > 0 ? $t('report.tokens.unmeasured', { count: item.unmeasured }) : '',
      },
    })),
  );
</script>

<div class="completion-report" bind:this={rootEl}>
  <div class="completion-report__nav">
    {#if onClose}
      <Button variant="ghost" size="sm" icon="act.back" onclick={closeReport}>
        {$t('common.action.back')}
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
        {titleLevel}
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
        <KindMark kind="plan" />
        <svelte:element this={`h${titleLevel}`} class="report-title" tabindex="-1"
          >{data.plan_id}</svelte:element
        >
        <div class="report-header__actions">
          <Button
            variant="secondary"
            size="sm"
            icon="act.chat"
            title={$t('control.plan.chat_ended.title')}
            onclick={() => openAddress({ route: 'plan', planId: data.plan_id, chat: true })}
          >
            {$t('control.plan.chat')}
          </Button>
        </div>
      </div>
      <div class="report-header__meta">
        <Chip value={data.plan_id} />
        {#if data.pull_request_number}
          <PrChip number={data.pull_request_number} href={data.pull_request_url} />
        {:else if data.pull_request_url}
          <ExternalLink href={data.pull_request_url} label={data.pull_request_url} />
        {/if}
        <Link
          href={pathForRoute('plan', { planId: data.plan_id }) ?? ''}
          label={$t('report.completion.plan_view')}
        />
        <StatusLabel
          tone={outcomeTone(data.outcome)}
          label={$t('overview.ended.outcome', { outcome: data.outcome })}
        />
        <span class="ended-time text-muted">
          {data.ended || formatTime(data.ended_at, 'short', $currentLocale)}
        </span>
      </div>
    </header>

    <div class="report-content">
      <!-- Summary section -->
      <Card>
        <KeyValueList items={summaryItems} />
      </Card>

      <!-- Deliverables -->
      <Card title={$t('report.completion.deliverables')} titleLevel={sectionLevel}>
        <span slot="actions" class="section-meta">
          {#if data.deliverables && data.deliverables.length > 0}
            {$t('report.completion.deliverables_done', {
              done: data.deliverables.filter((d) => d.state?.toLowerCase() === 'done').length,
              total: data.deliverables.length,
            })}
          {/if}
        </span>
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
      </Card>

      <!-- Re-planning -->
      {#if data.replanning_records && data.replanning_records.length > 0}
        <Card title={$t('report.completion.replanning')} titleLevel={sectionLevel}>
          <span slot="actions" class="section-meta">{data.replanning_records.length}</span>
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
        </Card>
      {/if}

      <!-- Decisions -->
      {#if data.decision_records && data.decision_records.length > 0}
        <Card title={$t('report.completion.decisions')} titleLevel={sectionLevel}>
          <span slot="actions" class="section-meta">{data.decision_records.length}</span>
          <ul class="decisions-list">
            {#each data.decision_records as dec}
              <li class="decision-item">
                <Chip value={dec.id} />
                {#if dec.question || dec.text}
                  <span class="decision-q">{dec.question || dec.text}</span>
                {/if}
                <span class="decision-by text-muted">({formatDecisionBy(dec)})</span>
              </li>
            {/each}
          </ul>
        </Card>
      {/if}

      <!-- Own calls -->
      {#if data.own_calls && data.own_calls.length > 0}
        <Card title={$t('report.completion.own_calls')} titleLevel={sectionLevel}>
          <span slot="actions" class="section-meta">{data.own_calls.length}</span>
          <ul class="own-calls-list">
            {#each data.own_calls as call}
              <li class="own-call-item">
                <div class="own-call-head">
                  <span class="own-call-kind">
                    {$t('report.completion.own_call_kind', { kind: call.kind })}
                  </span>
                  <StatusLabel tone={judgmentTone(call.judgment)} label={judgmentLabel(call)} />
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
        </Card>
      {/if}

      <!-- Change map -->
      {#if data.change_map}
        <Card title={$t('report.completion.change_map')} titleLevel={sectionLevel}>
          <div slot="actions">
            <StatusLabel
              tone={changeMapTone(data.change_map.state)}
              label={displayState('change_map', data.change_map.state, $t)}
            />
          </div>
          {#if data.change_map.cause}
            <p class="change-map-cause text-muted">
              {$t('report.completion.change_map_cause', { cause: data.change_map.cause })}
            </p>
          {/if}
          <pre class="change-map-diagram mono">{data.change_map.diagram}</pre>
        </Card>
      {/if}

      <!-- Tokens section -->
      {#if data.token_usage}
        {@const usage = data.token_usage}
        <Card title={$t('report.completion.tokens')} titleLevel={sectionLevel}>
          <div slot="actions">
            <Button
              variant="secondary"
              size="sm"
              onclick={() => (showRawComponents = !showRawComponents)}
              aria-expanded={showRawComponents}
            >
              {$t(showRawComponents ? 'report.completion.raw_hide' : 'report.completion.raw_show')}
            </Button>
          </div>
          <div class="tokens-section">
            <!-- Total & Coverage -->
            <div class="tokens-total-block">
              <KeyValueList items={tokenSummaryItems} />
            </div>

            <!-- Breakdown by Phase -->
            {#if usage.by_phase && usage.by_phase.length > 0}
              <div class="breakdown-group">
                <svelte:element this={`h${titleLevel + 2}`} class="breakdown-title">
                  {$t('report.completion.by_phase')}
                </svelte:element>
                <table class="breakdown-table" aria-label={$t('report.completion.by_phase')}>
                  <tbody>
                    {#each usage.by_phase as item}
                      <tr>
                        <td class="col-name mono">
                          <Link href={phaseHref(data.plan_id, item.name)} label={item.name} />
                        </td>
                        <td class="col-value col-numeric mono">
                          {formatBreakdownSplit(
                            item.total,
                            item.input,
                            item.output,
                            item.floor,
                            item.unmeasured,
                          )}
                        </td>
                        <td class="col-extra col-numeric text-muted">
                          {#if item.unmeasured > 0}
                            {$t('report.tokens.unmeasured', { count: item.unmeasured })}
                          {/if}
                        </td>
                      </tr>
                      {#if showRawComponents && item.components}
                        <tr class="raw-components-row">
                          <td colspan="3" class="mono text-muted">
                            {formatRawComponentsRow(item.components)}
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
                <DataTable
                  columns={roleColumns}
                  rows={roleRows}
                  label={$t('report.completion.by_role')}
                  compact
                />
                {#if showRawComponents}
                  {#each usage.by_role as item}
                    {#if item.components}
                      <p class="raw-components-line mono text-muted">
                        {item.name}: {formatRawComponentsRow(item.components)}
                      </p>
                    {/if}
                  {/each}
                {/if}
              </div>
            {/if}

            <!-- Breakdown by Model -->
            {#if usage.by_model && usage.by_model.length > 0}
              <div class="breakdown-group">
                <svelte:element this={`h${titleLevel + 2}`} class="breakdown-title">
                  {$t('report.completion.by_model')}
                </svelte:element>
                <DataTable
                  columns={modelColumns}
                  rows={modelRows}
                  label={$t('report.completion.by_model')}
                  compact
                />
                {#if showRawComponents}
                  {#each usage.by_model as item}
                    {#if item.components}
                      <p class="raw-components-line mono text-muted">
                        {item.name}: {formatRawComponentsRow(item.components)}
                      </p>
                    {/if}
                  {/each}
                {/if}
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
          </div>
        </Card>
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
    margin-bottom: var(--space-5);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .report-header__headline {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .report-header__actions {
    margin-inline-start: auto;
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
  }

  .report-header__meta {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
    font-size: var(--text-sm);
  }

  .report-title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .ended-time {
    font-size: var(--text-sm);
  }

  .report-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
  }

  .section-meta {
    font-size: var(--text-xs);
    font-weight: 500;
    color: var(--color-text-muted);
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

  .tokens-total-block {
    background: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
    margin-bottom: var(--space-5);
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

  .col-value {
    width: 45%;
  }

  .col-extra {
    width: 30%;
    font-size: var(--text-xs);
  }

  .col-numeric {
    text-align: end;
    font-variant-numeric: tabular-nums;
  }

  .raw-components-row td,
  .raw-components-line {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
    background: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    margin: var(--space-2) 0 0 0;
  }

  .waste-tokens {
    text-align: end;
    font-variant-numeric: tabular-nums;
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
    gap: var(--space-3);
    font-size: var(--text-sm);
    align-items: baseline;
  }

  @media (max-width: 640px) {
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
