import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import WorkControlPanel from './WorkControlPanel.svelte';
import { apiClient, type ArchiveCheck, type PlanSettingsRecord, type WorkActions } from '../api/client';
import { PLAN_ACTS, EPIC_ACTS, handledActIds, tableActIds } from '../actions/byState';

/**
 * `WorkControlPanel` (UC-CTL-1..5, UX-P-04): it draws the acts the state offers through
 * `ActionBar`, keeps the destructive ones behind the overflow and their confirmation, and carries
 * the panel's own dialogues.
 */

/**
 * Opens the bar's overflow so an act that lives there can be chosen.
 *
 * @returns the open menu
 */
async function openOverflow(): Promise<HTMLElement> {
  await fireEvent.click(await screen.findByTestId('action-bar-overflow'));
  return screen.findByRole('menu');
}

describe('WorkControlPanel component', () => {
  const running: WorkActions = {
    subject_id: 'fix-relay',
    subject_kind: 'plan',
    state: 'running',
    epic_id: 'relay-hardening',
    actions: [
      { action: 'pause', available: true },
      { action: 'cancel', available: true },
      { action: 'chat', available: true },
      { action: 'settings', available: true },
    ],
  };

  const settings: PlanSettingsRecord = {
    plan_id: 'fix-relay',
    revision: 7,
    values: [
      {
        aspect: 'landing',
        value: 'landing.pull-request',
        origin: 'project',
        choices: ['landing.pull-request', 'landing.main', 'landing.files'],
      },
    ],
  };

  const archiveCheck: ArchiveCheck = {
    epic_id: 'relay-hardening',
    lines: [
      { label: 'control.archive.no_plan_active', passed: true },
      {
        label: 'control.archive.open_findings',
        passed: false,
        note: '2',
        action: 'review-findings',
      },
    ],
    carried_findings: 3,
    carried_watches: 5,
    completion_review_first: false,
    passed: false,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue(running);
    vi.spyOn(apiClient, 'getPlanHolds').mockResolvedValue([]);
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue(running);
    vi.spyOn(apiClient, 'getEpicArchiveCheck').mockRejectedValue(new Error('none'));
    vi.spyOn(apiClient, 'getPlanSettings').mockResolvedValue(settings);
    vi.spyOn(apiClient, 'updatePlanSettings').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 8,
    });
    vi.spyOn(apiClient, 'applyPlanAction').mockResolvedValue(running);
    vi.spyOn(apiClient, 'applyEpicAction').mockResolvedValue(running);
    vi.spyOn(apiClient, 'getPlanHolds').mockResolvedValue([]);
  });

  it('renders no acts while the actions are still loading', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockReturnValue(new Promise(() => {}));
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    expect(await screen.findByTestId('control-actions-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('control-actions')).not.toBeInTheDocument();
  });

  it('loads the plan actions and offers them', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    expect(await screen.findByTestId('action-pause')).toBeInTheDocument();
    expect(apiClient.getPlanActions).toHaveBeenCalledWith('fix-relay');
    expect(screen.getByTestId('action-chat')).toBeInTheDocument();
    expect(screen.queryByTestId('action-cancel')).not.toBeInTheDocument();

    const menu = await openOverflow();
    expect(within(menu).getByTestId('action-settings')).toBeInTheDocument();
    expect(within(menu).getByTestId('action-settings')).toHaveTextContent('Plan settings');
    expect(within(menu).getByTestId('action-cancel')).toBeInTheDocument();
  });

  it('applies the pause the dialogue asked for and tells the view', async () => {
    const onChanged = vi.fn();
    render(WorkControlPanel, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan', onChanged },
    });

    await fireEvent.click(await screen.findByTestId('action-pause'));
    await fireEvent.click(screen.getByTestId('pause-confirm'));

    await waitFor(() => {
      expect(apiClient.applyPlanAction).toHaveBeenCalledWith('fix-relay', 'pause', {
        point: 'task',
      });
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('asks where a plan holds before it sends the pause', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await fireEvent.click(await screen.findByTestId('action-pause'));
    expect(screen.getByTestId('pause-title')).toHaveTextContent('Pause fix-relay?');
    await fireEvent.click(screen.getByLabelText('After the current phase'));
    await fireEvent.click(screen.getByTestId('pause-confirm'));

    await waitFor(() => {
      expect(apiClient.applyPlanAction).toHaveBeenCalledWith('fix-relay', 'pause', {
        point: 'phase',
      });
    });
  });

  it('confirms an end with a required reason and the pull request choice', async () => {
    render(WorkControlPanel, {
      props: {
        subjectId: 'fix-relay',
        subjectKind: 'plan',
        pullRequestNumber: 214,
      },
    });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));
    expect(await screen.findByTestId('end-title')).toHaveTextContent('Cancel fix-relay?');
    expect(screen.getByTestId('end-confirm')).toBeDisabled();

    await fireEvent.input(screen.getByTestId('end-reason'), {
      target: { value: 'the approach is replaced by plan relay-v2' },
    });
    await fireEvent.click(screen.getByLabelText('Close the pull request with the reason'));
    await fireEvent.click(screen.getByTestId('end-confirm'));

    await waitFor(() => {
      expect(apiClient.applyPlanAction).toHaveBeenCalledWith('fix-relay', 'cancel', {
        reason: 'the approach is replaced by plan relay-v2',
        close_pr: true,
      });
    });
  });

  it('confirms an abandon of a paused plan', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      subject_id: 'relay-metrics',
      state: 'paused',
      actions: [
        { action: 'start', available: true },
        { action: 'abandon', available: true },
      ],
    });
    render(WorkControlPanel, { props: { subjectId: 'relay-metrics', subjectKind: 'plan' } });

    expect(await screen.findByTestId('action-start')).toBeInTheDocument();
    expect(screen.queryByTestId('action-pause')).not.toBeInTheDocument();
    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-abandon'));
    expect(await screen.findByTestId('end-title')).toHaveTextContent('Abandon relay-metrics?');
    await fireEvent.input(screen.getByTestId('end-reason'), { target: { value: 'obsolete' } });
    await fireEvent.click(screen.getByTestId('end-confirm'));

    await waitFor(() => {
      expect(apiClient.applyPlanAction).toHaveBeenCalledWith('relay-metrics', 'abandon', {
        reason: 'obsolete',
        close_pr: false,
      });
    });
  });

  it('names the abort of an integrity pause as the end act', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      subject_id: 'relay-cursor-order',
      state: 'held',
      hold_kind: 'integrity_pause',
      actions: [{ action: 'cancel', available: true }],
    });
    render(WorkControlPanel, { props: { subjectId: 'relay-cursor-order', subjectKind: 'plan' } });

    await openOverflow();
    const cancel = screen.getByTestId('action-cancel');
    expect(cancel).toHaveTextContent('Abort the plan');
    await fireEvent.click(cancel);
    expect(await screen.findByTestId('end-title')).toHaveTextContent('Abort relay-cursor-order?');
    await fireEvent.input(screen.getByTestId('end-reason'), { target: { value: 'replaced' } });
    await fireEvent.click(screen.getByTestId('end-confirm'));

    await waitFor(() => {
      expect(apiClient.applyPlanAction).toHaveBeenCalledWith('relay-cursor-order', 'abort', {
        reason: 'replaced',
        close_pr: false,
      });
    });
  });

  it('asks before an epic cancel and sends nothing until it is confirmed', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      overlap_max_files: 0,
      actions: [
        { action: 'next', available: true },
        { action: 'cancel', available: true },
      ],
    });
    const apply = vi.spyOn(apiClient, 'applyEpicAction').mockResolvedValue(running);
    render(WorkControlPanel, {
      props: { subjectId: 'relay-hardening', subjectKind: 'epic', running: 2 },
    });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));
    expect(await screen.findByTestId('confirm-dialog')).toHaveTextContent(
      'Cancel relay-hardening?',
    );
    expect(screen.getByTestId('epic-cancel-plans')).toHaveTextContent('2 plans run');
    expect(apply).not.toHaveBeenCalled();

    await fireEvent.input(screen.getByTestId('epic-cancel-reason'), {
      target: { value: 'superseded by the next epic' },
    });
    await fireEvent.click(screen.getByLabelText('Let them run on without the epic'));
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    await waitFor(() => {
      expect(apply).toHaveBeenCalledWith('relay-hardening', 'cancel', {
        with_plans: false,
        reason: 'superseded by the next epic',
      });
    });
  });

  it('keeps the epic cancel dialogue open when the runtime refuses it, and clears it with the subject', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      overlap_max_files: 0,
      actions: [{ action: 'cancel', available: true }],
    });
    vi.spyOn(apiClient, 'applyEpicAction').mockRejectedValue(new Error('the epic is landing'));
    const { rerender } = render(WorkControlPanel, {
      props: { subjectId: 'relay-hardening', subjectKind: 'epic' },
    });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));
    await screen.findByTestId('confirm-dialog');
    await fireEvent.input(screen.getByTestId('epic-cancel-reason'), {
      target: { value: 'superseded' },
    });
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    expect(await screen.findByTestId('epic-cancel-failure')).toHaveTextContent('the epic is landing');
    expect(screen.getByTestId('confirm-dialog')).toBeInTheDocument();

    await rerender({ subjectId: 'relay-docs', subjectKind: 'epic' });
    await waitFor(() => {
      expect(screen.queryByTestId('confirm-dialog')).not.toBeInTheDocument();
    });
  });

  it('names no plan count when the view does not know how many run', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      overlap_max_files: 0,
      actions: [{ action: 'cancel', available: true }],
    });
    render(WorkControlPanel, { props: { subjectId: 'relay-hardening', subjectKind: 'epic' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));

    expect(await screen.findByTestId('confirm-dialog')).toBeInTheDocument();
    expect(screen.queryByTestId('epic-cancel-plans')).not.toBeInTheDocument();
  });

  it('clears the refusal of the last epic cancel when the dialogue opens again', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      overlap_max_files: 0,
      actions: [{ action: 'cancel', available: true }],
    });
    vi.spyOn(apiClient, 'applyEpicAction').mockRejectedValue(new Error('the epic is landing'));
    render(WorkControlPanel, { props: { subjectId: 'relay-hardening', subjectKind: 'epic' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));
    await screen.findByTestId('confirm-dialog');
    await fireEvent.input(screen.getByTestId('epic-cancel-reason'), {
      target: { value: 'superseded' },
    });
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
    expect(await screen.findByTestId('epic-cancel-failure')).toHaveTextContent('the epic is landing');

    // The operator keeps the epic and opens the dialogue again: the last refusal belonged to the
    // attempt before, not to this one.
    await fireEvent.click(screen.getByTestId('confirm-dialog-cancel'));
    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));

    expect(await screen.findByTestId('confirm-dialog')).toBeInTheDocument();
    expect(screen.queryByTestId('epic-cancel-failure')).not.toBeInTheDocument();
  });

  it('says the running count is unknown in the raise dialogue instead of naming zero', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    const apply = vi.spyOn(apiClient, 'applyEpicAction').mockResolvedValue(running);
    render(WorkControlPanel, {
      props: {
        subjectId: 'relay-hardening',
        subjectKind: 'epic',
        scope: 3,
        running: null,
        nextPlan: 'relay-token-rotation',
      },
    });

    await fireEvent.click(await screen.findByTestId('action-next'));

    const line = await screen.findByTestId('raise-scope');
    expect(line).toHaveTextContent('How many plans run is not known');
    expect(line).not.toHaveTextContent('runs 0 of 3');
    expect(apply).not.toHaveBeenCalled();
  });

  it('starts the next plan at once where the view knows no scope', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    const apply = vi.spyOn(apiClient, 'applyEpicAction').mockResolvedValue(running);
    render(WorkControlPanel, { props: { subjectId: 'relay-hardening', subjectKind: 'epic' } });

    await fireEvent.click(await screen.findByTestId('action-next'));

    // A row of Running Work knows neither the scope nor the count: nothing is weighed, so the act
    // starts the plan instead of asking about a scope it cannot name.
    await waitFor(() => {
      expect(apply).toHaveBeenCalledWith('relay-hardening', 'next', { raise_scope: false });
    });
    expect(screen.queryByTestId('raise-confirm')).not.toBeInTheDocument();
  });

  it('waits with the next plan while the epic itself is not read', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    const apply = vi.spyOn(apiClient, 'applyEpicAction').mockResolvedValue(running);
    render(WorkControlPanel, {
      props: { subjectId: 'relay-hardening', subjectKind: 'epic', scopeKnown: false },
    });

    const next = await screen.findByTestId('action-next');

    // Nothing weighs a scope it has not read: the act is refused with its reason, and sends nothing.
    expect(next).toBeDisabled();
    expect(next).toHaveAttribute('title', 'The epic is still being read; the next plan cannot start yet.');
    await fireEvent.click(next);
    expect(apply).not.toHaveBeenCalled();
  });

  it('keeps the hold dialogue shut when the read behind it fails', async () => {
    const hold = {
      hold_id: 'H-0001',
      subject_id: 'fix-relay',
      subject_kind: 'plan',
      kind: 'condition',
      cause: 'the plan reads cui-http, which this project may not read',
      attempts: 3,
      acts: ['retry', 'end_plan'],
    };
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      state: 'held',
      actions: [{ action: 'resolve_hold', available: true }],
    });
    vi.spyOn(apiClient, 'getPlanHolds')
      .mockResolvedValueOnce([hold])
      .mockRejectedValue(new Error('the ledger is landing'));

    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    // The load opens the hold once; the operator closes it.
    await screen.findByTestId('hold-modal');
    await fireEvent.click(screen.getByTestId('hold-close-button'));
    await waitFor(() => expect(screen.queryByTestId('hold-modal')).not.toBeInTheDocument());

    // Act: the fresh read fails, so the dialogue would show a hold nobody confirmed any more.
    await fireEvent.click(await screen.findByTestId('action-resolve_hold'));

    expect(await screen.findByTestId('control-failure')).toHaveTextContent('the ledger is landing');
    expect(screen.queryByTestId('hold-modal')).not.toBeInTheDocument();
  });

  it('draws no act the panel cannot run, and posts none', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      actions: [...running.actions, { action: 'teleport', available: true }],
    });
    const apply = vi.spyOn(apiClient, 'applyPlanAction').mockResolvedValue(running);

    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    expect(await screen.findByTestId('action-pause')).toBeInTheDocument();
    const menu = await openOverflow();

    // The runtime's own acts still stand, in the bar and in its overflow.
    expect(within(menu).getByTestId('action-cancel')).toBeInTheDocument();
    expect(screen.queryByTestId('action-teleport')).not.toBeInTheDocument();
    expect(apply).not.toHaveBeenCalled();
  });

  it('gives the focus back to the opener once the confirmed act settles', async () => {
    let settle: (actions: WorkActions) => void = () => {};
    const pending = new Promise<WorkActions>((resolve) => {
      settle = resolve;
    });
    vi.spyOn(apiClient, 'applyPlanAction').mockReturnValue(pending);
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    const pause = await screen.findByTestId('action-pause');
    await fireEvent.click(pause);
    await fireEvent.click(screen.getByTestId('pause-confirm'));

    // While the request runs the opener is disabled: it cannot take the focus yet.
    await waitFor(() => {
      expect(screen.queryByTestId('control-dialog-pause')).not.toBeInTheDocument();
    });
    expect(document.activeElement).not.toBe(pause);

    settle(running);

    await waitFor(() => {
      expect(document.activeElement).toBe(pause);
    });
  });

  it('moves the focus to the first control that can run when the act removes the opener', async () => {
    let settle: (actions: WorkActions) => void = () => {};
    const pending = new Promise<WorkActions>((resolve) => {
      settle = resolve;
    });
    vi.spyOn(apiClient, 'getPlanActions')
      .mockResolvedValueOnce(running)
      .mockResolvedValue({
        ...running,
        state: 'paused',
        actions: [{ action: 'start', available: true }],
      });
    vi.spyOn(apiClient, 'applyPlanAction').mockReturnValue(pending);
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await fireEvent.click(await screen.findByTestId('action-pause'));
    await fireEvent.click(screen.getByTestId('pause-confirm'));
    settle(running);

    await waitFor(() => {
      expect(screen.queryByTestId('action-pause')).not.toBeInTheDocument();
      expect(document.activeElement).toBe(screen.getByTestId('action-start'));
    });
  });

  it('keeps the focus in the end dialogue and names its consequence', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));
    const dialog = await screen.findByTestId('control-dialog-end');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByTestId('end-consequences')).toHaveTextContent(
      'the plan is archived as cancelled',
    );

    const reason = screen.getByTestId('end-reason');
    reason.focus();
    const buttons = Array.from(
      dialog.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled])'),
    );
    const last = buttons[buttons.length - 1];
    last.focus();
    const tab = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    last.dispatchEvent(tab);
    expect(tab.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(buttons[0]);
  });

  it('moves the focus from the dialog box to its last control on Shift+Tab', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-cancel'));
    const dialog = await screen.findByTestId('control-dialog-end');

    // The box itself takes the focus when the dialogue opens: it is the trap's own starting point.
    await waitFor(() => expect(document.activeElement).toBe(dialog));

    const controls = Array.from(
      dialog.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled])'),
    );
    const last = controls[controls.length - 1];
    const shiftTab = new KeyboardEvent('keydown', {
      key: 'Tab',
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    });

    dialog.dispatchEvent(shiftTab);

    expect(shiftTab.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(last);
  });

  it('closes a dialogue on Escape and gives the focus back', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    const pause = await screen.findByTestId('action-pause');
    await fireEvent.click(pause);
    expect(screen.getByTestId('control-dialog-pause')).toBeInTheDocument();

    await fireEvent.keyDown(window, { key: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByTestId('control-dialog-pause')).not.toBeInTheDocument();
    });
    expect(document.activeElement).toBe(pause);
  });

  it('drops the decision act another view owns instead of posting it', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      state: 'waiting',
      actions: [{ action: 'decision', available: true }],
    });
    const apply = vi.spyOn(apiClient, 'applyPlanAction').mockResolvedValue(running);
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await screen.findByTestId('control-actions');
    expect(screen.queryByTestId('action-decision')).not.toBeInTheDocument();
    expect(apply).not.toHaveBeenCalled();
  });

  it('routes the report act to the completion report instead of posting it', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      state: 'waiting',
      actions: [
        { action: 'pause', available: true },
        { action: 'report', available: true },
      ],
    });
    const apply = vi.spyOn(apiClient, 'applyPlanAction').mockResolvedValue(running);
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-report'));

    await waitFor(() => expect(window.location.pathname).toBe('/reports'));
    expect(window.location.search).toContain('plan=fix-relay');
    expect(apply).not.toHaveBeenCalled();
  });

  it('opens the plan’s hold for the resolve_hold act instead of posting it', async () => {
    const hold = {
      hold_id: 'H-0001',
      subject_id: 'fix-relay',
      subject_kind: 'plan',
      kind: 'condition',
      cause: 'the plan reads cui-http, which this project may not read',
      attempts: 3,
      acts: ['retry', 'end_plan'],
    };
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      state: 'held',
      actions: [{ action: 'resolve_hold', available: true }],
    });
    vi.spyOn(apiClient, 'getPlanHolds').mockResolvedValue([hold]);
    const apply = vi.spyOn(apiClient, 'applyPlanAction').mockResolvedValue(running);
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    // The modal opens with the load; close it so only the act can open it again.
    await fireEvent.click(await screen.findByTestId('hold-close-button'));
    await waitFor(() => expect(screen.queryByTestId('hold-modal')).not.toBeInTheDocument());

    await fireEvent.click(await screen.findByTestId('action-resolve_hold'));

    expect(await screen.findByTestId('hold-modal')).toBeInTheDocument();
    expect(apply).not.toHaveBeenCalledWith('fix-relay', 'resolve_hold', expect.anything());
  });

  it('opens the plan settings and reports where a change applies', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-settings'));
    expect(await screen.findByTestId('settings-modal')).toBeInTheDocument();

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.main' },
    });
    await fireEvent.click(screen.getByTestId('settings-apply-landing'));

    await waitFor(() => {
      expect(apiClient.updatePlanSettings).toHaveBeenCalledWith(
        'fix-relay',
        'landing',
        'landing.main',
        7,
      );
    });
    expect(await screen.findByTestId('settings-notice')).toHaveTextContent(
      "Applies where fix-relay next reads it; the plan's version becomes 8.",
    );
  });

  it('shows a conflict and applies the same change against the current revision', async () => {
    vi.spyOn(apiClient, 'updatePlanSettings').mockResolvedValueOnce({
      applied: false,
      conflict: true,
      revision: 8,
      current_value: 'landing.main',
    });
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-settings'));
    await fireEvent.change(await screen.findByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });
    await fireEvent.click(screen.getByTestId('settings-apply-landing'));

    expect(await screen.findByTestId('settings-conflict')).toBeInTheDocument();
    await fireEvent.click(screen.getByTestId('settings-conflict-apply-again'));

    await waitFor(() => {
      expect(apiClient.updatePlanSettings).toHaveBeenLastCalledWith(
        'fix-relay',
        'landing',
        'landing.files',
        8,
      );
    });
  });

  it('toggles a named value of an epic rather than flipping it', async () => {
    const epic: WorkActions = {
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      auto_enabled: true,
      overlap_max_files: 0,
      actions: [
        { action: 'next', available: true },
        { action: 'auto', available: true },
      ],
    };
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue(epic);
    render(WorkControlPanel, { props: { subjectId: 'relay-hardening', subjectKind: 'epic' } });

    expect(await screen.findByTestId('action-next')).toBeInTheDocument();
    await openOverflow();
    expect(screen.getByTestId('action-auto')).toHaveTextContent('Automatic: on');
    await fireEvent.click(screen.getByTestId('action-auto'));

    await waitFor(() => {
      expect(apiClient.applyEpicAction).toHaveBeenCalledWith('relay-hardening', 'auto', {
        enabled: false,
      });
    });
  });

  it('shows the archive check and keeps archiving disabled until every line passes', async () => {
    const onReview = vi.fn();
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'archive', available: true }],
    });
    vi.spyOn(apiClient, 'getEpicArchiveCheck').mockResolvedValue(archiveCheck);
    render(WorkControlPanel, {
      props: { subjectId: 'relay-hardening', subjectKind: 'epic', onReview },
    });

    await openOverflow();
    await fireEvent.click(screen.getByTestId('action-archive'));
    expect(await screen.findByTestId('archive-title')).toHaveTextContent(
      'Archive relay-hardening?',
    );
    expect(screen.getByTestId('archive-line-control.archive.open_findings')).toBeInTheDocument();
    expect(
      screen
        .getByTestId('archive-line-control.archive.open_findings')
        .querySelector('[role="img"]')
        ?.getAttribute('aria-label'),
    ).toBe('not met');
    expect(
      screen
        .getByTestId('archive-line-control.archive.no_plan_active')
        .querySelector('[role="img"]')
        ?.getAttribute('aria-label'),
    ).toBe('passed');
    expect(screen.getByTestId('archive-carried')).toHaveTextContent('3 kept findings');
    expect(screen.getByTestId('archive-confirm')).toBeDisabled();

    await fireEvent.click(screen.getByTestId('archive-review-findings'));
    expect(onReview).toHaveBeenCalledWith('findings');
  });

  it('offers to raise the scope when the epic runs its full scope', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    render(WorkControlPanel, {
      props: {
        subjectId: 'relay-hardening',
        subjectKind: 'epic',
        scope: 3,
        running: 3,
        nextPlan: 'relay-token-rotation',
      },
    });

    await fireEvent.click(await screen.findByTestId('action-next'));
    expect(screen.getByTestId('raise-confirm')).toHaveTextContent(
      'Raise to 4 at once and start it',
    );
    await fireEvent.click(screen.getByTestId('raise-confirm'));

    await waitFor(() => {
      expect(apiClient.applyEpicAction).toHaveBeenCalledWith('relay-hardening', 'next', {
        raise_scope: true,
      });
    });
  });

  it('opens the hold that holds the plan and reports a release', async () => {
    const hold = {
      hold_id: 'H-0001',
      subject_id: 'fix-relay',
      subject_kind: 'plan',
      kind: 'condition',
      cause: 'the plan reads cui-http, which this project may not read',
      attempts: 3,
      acts: ['retry', 'end_plan'],
    };
    vi.spyOn(apiClient, 'getPlanHolds').mockResolvedValueOnce([hold]).mockResolvedValue([]);
    vi.spyOn(apiClient, 'resolveHold').mockResolvedValue({
      hold_id: 'H-0001',
      released: true,
      subject: 'fix-relay',
    });

    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    expect(await screen.findByTestId('hold-modal')).toBeInTheDocument();
    await fireEvent.click(screen.getByTestId('hold-act-retry'));

    await waitFor(() => {
      expect(apiClient.resolveHold).toHaveBeenCalledWith('fix-relay', 'H-0001', 'retry', undefined);
    });
  });

  it('opens the chat of a plan and sends a message', async () => {
    vi.spyOn(apiClient, 'getChat').mockResolvedValue({
      chat_id: 'C-0001',
      subject_id: 'fix-relay',
      subject_kind: 'plan',
      role: 'replan',
      scope: 'plan',
      state: 'open',
      turns: [],
    });
    const turn = vi.spyOn(apiClient, 'chatWithSubject').mockResolvedValue({
      chat_id: 'C-0001',
      subject_id: 'fix-relay',
      subject_kind: 'plan',
      role: 'replan',
      scope: 'plan',
      state: 'turn',
      turns: [{ role: 'operator', text: 'Can the test cover SSE?', at: 'now', untrusted: false }],
    });

    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await fireEvent.click(await screen.findByTestId('action-chat'));
    await fireEvent.input(await screen.findByTestId('chat-input'), {
      target: { value: 'Can the test cover SSE?' },
    });
    await fireEvent.click(screen.getByTestId('chat-send'));

    await waitFor(() => {
      expect(turn).toHaveBeenCalledWith('fix-relay', 'Can the test cover SSE?');
    });
  });

  it('offers a chat about ended work and opens the start of its follow-up', async () => {
    vi.spyOn(apiClient, 'getEndedChat').mockResolvedValue({
      subject_id: 'codeberg-issues',
      subject_kind: 'plan',
      role: 'replan',
      scope: 'archive',
      available: true,
      proposal: {
        summary: 'A follow-up plan',
        after: 'Add the Codeberg issue provider on top of relay-v2.',
        applies: 'a new plan',
      },
    });
    const accept = vi.spyOn(apiClient, 'acceptFollowUp').mockResolvedValue({
      start: {
        entry: {
          entry_id: 'ent-3f9a2c41',
          kind: 'plan',
          project_id: 'pm-mcp-clients',
          state: 'start.sources',
          sources: [],
          kind_offers: [],
          can_continue: true,
          chat_available: true,
        },
      },
    });

    render(WorkControlPanel, {
      props: {
        subjectId: 'codeberg-issues',
        subjectKind: 'plan',
        projectId: 'pm-mcp-clients',
        ended: true,
      },
    });

    await fireEvent.click(await screen.findByTestId('action-chat'));
    expect(await screen.findByTestId('chat-proposal-after')).toHaveTextContent(
      'Add the Codeberg issue provider',
    );

    await fireEvent.click(screen.getByTestId('chat-accept'));

    await waitFor(() => {
      expect(accept).toHaveBeenCalledWith('pm-mcp-clients', 'codeberg-issues');
    });
    expect(window.location.pathname).toBe('/entries/ent-3f9a2c41');
  });

  it('reloads the actions when the subject changes', async () => {
    const { rerender } = render(WorkControlPanel, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan' },
    });

    expect(await screen.findByTestId('action-pause')).toBeInTheDocument();
    expect(apiClient.getPlanActions).toHaveBeenCalledWith('fix-relay');

    await rerender({ subjectId: 'relay-metrics', subjectKind: 'plan' });

    await waitFor(() => {
      expect(apiClient.getPlanActions).toHaveBeenLastCalledWith('relay-metrics');
    });
  });

  it('says why an action cannot run, as the advisory and as its description', async () => {
    vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
      ...running,
      actions: [
        { action: 'pause', available: false, reason: 'the ledger is landing' },
        { action: 'chat', available: true },
      ],
    });

    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    const pause = await screen.findByTestId('action-pause');
    const describedBy = pause.getAttribute('aria-describedby');

    expect(pause).toBeDisabled();
    expect(pause.getAttribute('title')).toBe('the ledger is landing');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain(
      'the ledger is landing',
    );
  });

  it('names the panel and every control it offers', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    expect(await screen.findByTestId('action-pause')).toBeInTheDocument();
    expect(screen.getByTestId('work-control')).toHaveAccessibleName();
    for (const button of screen.getAllByRole('button')) {
      expect(button).toHaveAccessibleName();
    }
  });

  it('derives handled act ids from the action tables and posts a dynamically added table act', async () => {
    expect(handledActIds('plan')).toEqual(tableActIds(PLAN_ACTS, ['decision']));
    expect(handledActIds('epic')).toEqual(tableActIds(EPIC_ACTS, ['decision']));

    const customAct = {
      id: 'custom_inspect',
      labelKey: 'control.plan.custom_inspect',
      role: 'overflow' as const,
    };
    PLAN_ACTS.running.push(customAct);
    try {
      vi.spyOn(apiClient, 'getPlanActions').mockResolvedValue({
        ...running,
        actions: [
          ...running.actions!,
          { action: 'custom_inspect', available: true },
        ],
      });

      render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

      const menu = await openOverflow();
      const customButton = within(menu).getByTestId('action-custom_inspect');
      expect(customButton).toBeInTheDocument();

      await fireEvent.click(customButton);
      expect(apiClient.applyPlanAction).toHaveBeenCalledWith('fix-relay', 'custom_inspect', {});
    } finally {
      const idx = PLAN_ACTS.running.indexOf(customAct);
      if (idx !== -1) {
        PLAN_ACTS.running.splice(idx, 1);
      }
    }
  });
});
