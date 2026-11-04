import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import WorkControlPanel from './WorkControlPanel.svelte';
import { apiClient, type ArchiveCheck, type PlanSettingsRecord, type WorkActions } from '../api/client';

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
});
