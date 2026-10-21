import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import WorkControlPanel from './WorkControlPanel.svelte';
import { apiClient, type PlanSettingsRecord, type WorkActions } from '../api/client';

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
    vi.spyOn(apiClient, 'getPlanHolds').mockResolvedValue([]);
  });

  it('loads the plan actions and offers them', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    expect(await screen.findByTestId('action-pause')).toBeInTheDocument();
    expect(screen.getByTestId('action-settings')).toBeInTheDocument();
    expect(apiClient.getPlanActions).toHaveBeenCalledWith('fix-relay');
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

  it('opens the plan settings and reports where a change applies', async () => {
    render(WorkControlPanel, { props: { subjectId: 'fix-relay', subjectKind: 'plan' } });

    await fireEvent.click(await screen.findByTestId('action-settings'));
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

    await fireEvent.click(await screen.findByTestId('action-settings'));
    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
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
});
