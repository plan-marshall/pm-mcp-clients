import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import MachineSettingsView from './MachineSettingsView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type MachineSettingsRecord } from '../api/client';

const settings: MachineSettingsRecord = {
  revision: 4,
  aspects: [
    { aspect: 'language', step: 1, summary: 'Deutsch, models English', current: 'de' },
    { aspect: 'harness', step: 2, summary: 'Claude Code 2.4.1 (default)', current: 'claude' },
    { aspect: 'providers', step: 6, summary: 'GitHub (gh)', current: 'github' },
  ],
  language_interface: 'de',
  language_model: 'en',
  documents_mode: 'ask',
  cleanup_durations: {
    plans_after_days: 30,
    epics_after_days: 30,
    logs_after_days: 30,
    reports_after_days: 30,
    unenrolled_after_days: 30,
    remind_after_days: 3,
  },
  plan_defaults: {
    landing: 'landing.pull-request',
    method: 'method.test-first',
    asking: 'ask.key-points',
  },
  consents: [
    {
      kind: 'scan_harnesses',
      status: 'remembered',
      remember: true,
      granted_at: '2026-10-12T09:00:00Z',
      channel: 'terminal',
    },
  ],
  credentials: [
    { key: 'github', provider: 'GitHub', kind: 'fine-grained', expires_in_days: 9, expired: false },
  ],
};

describe('MachineSettingsView component', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');

    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Firefox on mbp-oliver',
        machineName: 'mbp-oliver',
        currentRoute: 'machine-settings',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 1,
      }),
    );

    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue(settings);
  });

  it('renders the recorded aspects, consents, documents mode and tokens', async () => {
    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('aspect-list')).toBeInTheDocument();
    });

    expect(screen.getByText('Claude Code 2.4.1 (default)')).toBeInTheDocument();
    expect(screen.getByText('look for harnesses')).toBeInTheDocument();
    expect(screen.getByTestId('documents-section')).toBeInTheDocument();
    expect(screen.getByText(/token expires in 9 days/)).toBeInTheDocument();
  });

  it('withdraws a remembered consent', async () => {
    const withdrawSpy = vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 5,
    });
    vi.spyOn(apiClient, 'getMachineSettings')
      .mockResolvedValueOnce(settings)
      .mockResolvedValueOnce({ ...settings, revision: 5, consents: [] });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('withdraw-scan_harnesses')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('withdraw-scan_harnesses'));

    await waitFor(() => {
      expect(withdrawSpy).toHaveBeenCalledWith('consent:scan_harnesses', null, 4);
    });
  });

  it('sends the reminder interval beside the days and shows it again after a reload', async () => {
    let recorded: MachineSettingsRecord = settings;
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockImplementation(async (key: string, value: string | null) => {
        // The runtime keeps what it was sent, so the reload reads it back from the update itself.
        if (key === 'cleanup_durations' && value) {
          recorded = {
            ...recorded,
            revision: recorded.revision + 1,
            cleanup_durations: {
              ...recorded.cleanup_durations,
              ...(JSON.parse(value) as Record<string, number>),
            },
          };
        }
        return { applied: true, conflict: false, revision: recorded.revision };
      });
    vi.spyOn(apiClient, 'getMachineSettings').mockImplementation(async () => recorded);

    const first = render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('cleanup_durations', expect.any(String), 4);
    });
    const sent = JSON.parse(updateSpy.mock.calls[0]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    expect(sent.remind_after_days).toBe(7);
    // A category the page does not show keeps the value the runtime holds.
    expect(sent.epics_after_days).toBe(settings.cleanup_durations.epics_after_days);

    // A reload reads the runtime's record back: the saved value is shown again.
    first.unmount();
    render(MachineSettingsView);
    await waitFor(() => {
      expect(screen.getByTestId('cleanup-remind-days')).toHaveValue(7);
    });
  });

  it('keeps the other categories when one of them has its own retention', async () => {
    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue({
      ...settings,
      cleanup_durations: { ...settings.cleanup_durations, epics_after_days: 90 },
    });
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true, conflict: false, revision: 5 });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalled();
    });
    const sent = JSON.parse(updateSpy.mock.calls[0]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    expect(sent.epics_after_days).toBe(90);
  });

  it.each([
    ['cleanup-days', '', 'At least 7 days, or never.'],
    ['cleanup-days', '2.5', 'At least 7 days, or never.'],
    ['cleanup-days', '3', 'At least 7 days, or never.'],
    ['cleanup-remind-days', '', 'At least 1 day.'],
    ['cleanup-remind-days', '2.5', 'At least 1 day.'],
  ])('writes nothing when %s holds "%s" and names the minimum', async (testId, value, minimum) => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true, conflict: false, revision: 5 });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId(testId), { target: { value } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    // Each of these would reach the runtime as a value the contract refuses, or as a missing field
    // the runtime fills with a default; the refusal names the minimum the value missed.
    await waitFor(() => {
      expect(screen.getByText(minimum)).toBeInTheDocument();
    });
    expect(updateSpy).not.toHaveBeenCalled();
  });

  it('keeps the operator’s categories over a second conflict', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: JSON.stringify({
          plans_after_days: 35,
          epics_after_days: 90,
          logs_after_days: 90,
          reports_after_days: 30,
          unenrolled_after_days: 30,
          remind_after_days: 4,
        }),
      })
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 10,
        current_value: JSON.stringify({
          plans_after_days: 40,
          epics_after_days: 95,
          logs_after_days: 95,
          reports_after_days: 31,
          unenrolled_after_days: 31,
          remind_after_days: 5,
        }),
      })
      .mockResolvedValue({ applied: true, conflict: false, revision: 11 });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => {
      expect(screen.getByTestId('conflict-apply')).toBeInTheDocument();
    });

    // The other client changes again between the two answers: the second resend must still apply
    // the operator's categories over the newer record, not the one the first resend was built for.
    await fireEvent.click(screen.getByTestId('conflict-apply'));
    await waitFor(() => {
      expect(screen.getByTestId('conflict-apply')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledTimes(3);
    });
    const sent = JSON.parse(updateSpy.mock.calls[2]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    expect(sent.remind_after_days).toBe(7);
    expect(sent.epics_after_days).toBe(95);
    expect(sent.logs_after_days).toBe(95);
    expect(sent.reports_after_days).toBe(31);
    expect(sent.unenrolled_after_days).toBe(31);
  });

  it('saves the reminder while plans are never offered', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true, conflict: false, revision: 5 });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '-1' } });
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('cleanup_durations', expect.any(String), 4);
    });
    const sent = JSON.parse(updateSpy.mock.calls[0]?.[1] ?? '{}') as Record<string, number>;
    // The contract's "never" is a retention duration of its own, not a value below the minimum.
    expect(sent.plans_after_days).toBe(-1);
    expect(sent.remind_after_days).toBe(7);
  });

  it('keeps the other client’s reminder when only the plans changed', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: JSON.stringify({
          ...settings.cleanup_durations,
          plans_after_days: 35,
          remind_after_days: 5,
        }),
      })
      .mockResolvedValue({ applied: true, conflict: false, revision: 10 });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    // Only the plans value is the operator's; the reminder is the record's.
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => {
      expect(screen.getByTestId('conflict-apply')).toBeInTheDocument();
    });
    // The runtime the retry leaves behind is the one the page reloads: the operator's plans value
    // over the other client's reminder.
    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue({
      ...settings,
      revision: 10,
      cleanup_durations: {
        ...settings.cleanup_durations,
        plans_after_days: 45,
        remind_after_days: 5,
      },
    });
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledTimes(2);
    });
    const sent = JSON.parse(updateSpy.mock.calls[1]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    // The reminder the operator did not touch keeps the other client's value — and the input
    // follows it, so the next save does not overwrite it with the old one.
    expect(sent.remind_after_days).toBe(5);
    await waitFor(() => {
      expect(screen.getByTestId('cleanup-remind-days')).toHaveValue(5);
    });
  });

  it('shows a cleanup conflict in the catalogue’s words instead of JSON', async () => {
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: false,
      conflict: true,
      revision: 9,
      current_value: JSON.stringify({
        plans_after_days: 90,
        epics_after_days: 90,
        logs_after_days: 90,
        reports_after_days: 30,
        unenrolled_after_days: 30,
        remind_after_days: 5,
      }),
    });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => {
      expect(screen.getByTestId('conflict-dialog')).toBeInTheDocument();
    });
    const dialog = screen.getByTestId('conflict-dialog');
    expect(dialog).toHaveTextContent('Archived plans: 90 days');
    expect(dialog).toHaveTextContent('Archived epics: 90 days');
    expect(dialog).toHaveTextContent('Logs of ended work: 90 days');
    expect(dialog).toHaveTextContent('Ask me again after 5 days');
    expect(dialog).not.toHaveTextContent('{');
  });

  it('applies the operator’s cleanup categories over the record the other client left', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: JSON.stringify({
          plans_after_days: 35,
          epics_after_days: 90,
          logs_after_days: 90,
          reports_after_days: 30,
          unenrolled_after_days: 30,
          remind_after_days: 4,
        }),
      })
      .mockResolvedValue({ applied: true, conflict: false, revision: 10 });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-cleanup')).toBeInTheDocument();
    });
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => {
      expect(screen.getByTestId('conflict-apply')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledTimes(2);
    });
    const sent = JSON.parse(updateSpy.mock.calls[1]?.[1] ?? '{}') as Record<string, number>;
    // The two categories the operator changed are applied over the record the other client left.
    expect(sent.plans_after_days).toBe(45);
    expect(sent.remind_after_days).toBe(7);
    // The categories the page only carried along keep the other client's value.
    expect(sent.epics_after_days).toBe(90);
    expect(sent.logs_after_days).toBe(90);
    expect(sent.reports_after_days).toBe(30);
    expect(sent.unenrolled_after_days).toBe(30);
  });

  it('reports a conflict instead of overwriting the current value', async () => {
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: false,
      conflict: true,
      revision: 9,
      current_value: 'copy',
    });

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('save-documents')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('save-documents'));

    await waitFor(() => {
      expect(screen.getByTestId('conflict-dialog')).toBeInTheDocument();
    });
    expect(screen.getByTestId('conflict-dialog')).toHaveTextContent('Current value: copy');
    expect(screen.getByTestId('conflict-apply')).toBeInTheDocument();
    expect(screen.getByTestId('conflict-keep')).toBeInTheDocument();
  });

  it('opens the onboarding at the aspect whose Change was clicked', async () => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Firefox on mbp-oliver',
        machineName: 'mbp-oliver',
        currentRoute: 'machine-settings',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 1,
      }),
    );

    render(MachineSettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('change-aspect-providers')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('change-aspect-providers'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('onboarding');
    expect(state.onboardingStep).toBe(6);
  });
});
