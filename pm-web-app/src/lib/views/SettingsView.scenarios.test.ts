import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SettingsView from './SettingsView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type MachineSettingsRecord } from '../api/client';

/**
 * The machine settings' panels of the one settings view as a view test against the states
 * `pm-mock-runtime` reports (`onboarding-settings-conflict` and the settings record of
 * `onboarding`).
 */

function settingsRecord(partial: Partial<MachineSettingsRecord> = {}): MachineSettingsRecord {
  return {
    revision: 4,
    aspects: [
      { aspect: 'language', step: 1, summary: 'Deutsch, models English', current: 'de' },
      { aspect: 'harness', step: 2, summary: 'Claude Code 2.4.1 (default)', current: 'claude' },
      { aspect: 'catalogue', step: 3, summary: '3 models on 2 harnesses', current: '3' },
      { aspect: 'roles', step: 4, summary: '36 roles', current: '36' },
      { aspect: 'store', step: 5, summary: 'macOS Keychain', current: 'os' },
      { aspect: 'providers', step: 6, summary: 'GitHub (gh)', current: 'github' },
      { aspect: 'defaults', step: 7, summary: 'Pull Request · Test first', current: 'landing' },
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
      {
        kind: 'probe_harness_login',
        status: 'remembered',
        remember: true,
        granted_at: '2026-10-12T09:10:00Z',
        channel: 'Firefox on mbp-oliver',
      },
    ],
    credentials: [
      {
        key: 'github',
        provider: 'GitHub',
        kind: 'fine-grained',
        expires_in_days: 9,
        expired: false,
      },
      {
        key: 'sonar',
        provider: 'SonarCloud',
        kind: 'user token',
        expired: true,
        held_plan: 'fix-relay',
      },
    ],
    ...partial,
  };
}

/**
 * @param name the visible label of the tab
 */
async function openTab(name: string): Promise<void> {
  await fireEvent.click(screen.getByRole('tab', { name }));
}

describe('the settings panels against the mock runtime states', () => {
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
        currentRoute: 'settings',
        objectTab: 'setup',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 7,
        activeStep: 8,
        unreadReportsCount: 0,
        openDecisionsCount: 0,
      }),
    );

    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue(settingsRecord());
    vi.spyOn(apiClient, 'getDevices').mockResolvedValue([]);
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      status: 'ready',
      web: { enabled: true, lan: false, port: 7420 },
    });
  });

  it('shows every aspect once, split over the tabs that carry them', async () => {
    render(SettingsView);

    await waitFor(() => expect(screen.getByTestId('aspect-list')).toBeInTheDocument());
    // The setup carries the records of the onboarding's own steps.
    expect(screen.getByText('3 models on 2 harnesses')).toBeInTheDocument();
    expect(screen.getByText('36 roles')).toBeInTheDocument();
    expect(screen.queryByText('macOS Keychain')).not.toBeInTheDocument();

    // The credentials carry the store and the providers.
    await openTab('Credentials');
    expect(screen.getByText('macOS Keychain')).toBeInTheDocument();
    expect(screen.getByText('GitHub (gh)')).toBeInTheDocument();
  });

  it('withdraws a remembered consent', async () => {
    const updateSpy = vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 5,
    });
    vi.spyOn(apiClient, 'getMachineSettings')
      .mockResolvedValueOnce(settingsRecord())
      .mockResolvedValueOnce(settingsRecord({ revision: 5, consents: [] }));

    render(SettingsView);
    await openTab('Consents');

    await waitFor(() => expect(screen.getByTestId('withdraw-scan_harnesses')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('withdraw-scan_harnesses'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('consent:scan_harnesses', null, 4);
    });
    await waitFor(() => {
      expect(screen.getByText('Withdrawn: you will be asked again next time.')).toBeInTheDocument();
    });
  });

  it('opens the 409 conflict dialogue and applies the value again on the current revision', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({ applied: false, conflict: true, revision: 9, current_value: 'copy' })
      .mockResolvedValueOnce({ applied: true, conflict: false, revision: 10 });

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());
    await fireEvent.click(screen.getByLabelText('Move'));

    await fireEvent.click(screen.getByTestId('save-documents'));

    await waitFor(() => expect(screen.getByTestId('conflict-dialog')).toBeInTheDocument());
    expect(screen.getByTestId('conflict-dialog')).toHaveTextContent('Current value: Copy');
    expect(screen.getByTestId('conflict-dialog')).toHaveTextContent('Your value: Move');

    // Apply again: the value this page wanted, on the revision the other client left behind.
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenLastCalledWith('documents_mode', 'move', 4);
    });
    await waitFor(() => expect(screen.queryByTestId('conflict-dialog')).not.toBeInTheDocument());
  });

  it('keeps the current value instead of applying the refused one', async () => {
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: false,
      conflict: true,
      revision: 9,
      current_value: 'copy',
    });
    const loadSpy = vi
      .spyOn(apiClient, 'getMachineSettings')
      .mockResolvedValue(settingsRecord({ documents_mode: 'copy' }));

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());
    await fireEvent.click(screen.getByLabelText('Move'));
    await fireEvent.click(screen.getByTestId('save-documents'));
    await waitFor(() => expect(screen.getByTestId('conflict-keep')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('conflict-keep'));

    await waitFor(() => expect(screen.queryByTestId('conflict-dialog')).not.toBeInTheDocument());
    expect(loadSpy).toHaveBeenCalled();
  });

  it('marks an expired token with the plans it holds and offers rotate and replace', async () => {
    render(SettingsView);
    await openTab('Credentials');

    await waitFor(() => expect(screen.getByTestId('rotate-sonar')).toBeInTheDocument());
    expect(screen.getByText(/expired: holds 1 plan/)).toBeInTheDocument();
    expect(screen.getByText(/token expires in 9 days/)).toBeInTheDocument();
    expect(screen.getByTestId('replace-github')).toBeInTheDocument();
  });

  it('stores a rotated token and never shows it again', async () => {
    const setCredentialSpy = vi.spyOn(apiClient, 'setCredential').mockResolvedValue();
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 5,
    });

    render(SettingsView);
    await openTab('Credentials');
    await waitFor(() => expect(screen.getByTestId('rotate-github')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('rotate-github'));
    expect(screen.getByTestId('rotate-modal')).toBeInTheDocument();

    const input = screen.getByTestId('rotate-secret');
    expect(input).toHaveAttribute('type', 'password');
    await fireEvent.input(input, { target: { value: 'ghp_new_secret' } });
    await fireEvent.click(screen.getByTestId('rotate-save'));

    await waitFor(() => expect(setCredentialSpy).toHaveBeenCalledWith('github', 'ghp_new_secret'));
    await waitFor(() => expect(screen.getByTestId('token-notice')).toBeInTheDocument());
    expect(screen.queryByTestId('rotate-modal')).not.toBeInTheDocument();
  });

  it('shows the other client’s value in the conflict notice and applies yours again', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({ applied: false, conflict: true, revision: 9, current_value: 'copy' })
      .mockResolvedValueOnce({ applied: true, conflict: false, revision: 10 });

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());
    await fireEvent.click(screen.getByLabelText('Move'));
    await fireEvent.click(screen.getByTestId('save-documents'));

    const dialog = await screen.findByTestId('conflict-dialog');
    // The refusal names both values, so the operator sees what the other client left behind.
    expect(dialog).toHaveTextContent('Current value: Copy');
    expect(dialog).toHaveTextContent('Your value: Move');
    expect(dialog).toHaveTextContent('Apply yours again');
    expect(dialog).toHaveTextContent('Keep the current value');

    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenLastCalledWith('documents_mode', 'move', 4);
    });
  });

  it('keeps the other client’s reminder when only the plans changed', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: JSON.stringify({
          ...settingsRecord().cleanup_durations,
          plans_after_days: 35,
          remind_after_days: 5,
        }),
      })
      .mockResolvedValue({ applied: true, conflict: false, revision: 10 });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    // Only the plans value is the operator's; the reminder is the record's.
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => expect(screen.getByTestId('conflict-apply')).toBeInTheDocument());

    // The runtime the retry leaves behind is the one the page reloads: the operator's plans value
    // over the other client's reminder.
    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue(
      settingsRecord({
        revision: 10,
        cleanup_durations: {
          ...settingsRecord().cleanup_durations,
          plans_after_days: 45,
          remind_after_days: 5,
        },
      }),
    );
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(2));
    const sent = JSON.parse(updateSpy.mock.calls[1]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    // The reminder the operator did not touch keeps the other client's value — and the input
    // follows it, so the next save does not overwrite it with the old one.
    expect(sent.remind_after_days).toBe(5);
    await waitFor(() => expect(screen.getByTestId('cleanup-remind-days')).toHaveValue(5));
  });

  it('shows a consent conflict in the catalogue’s words and withdraws on apply', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: 'withdrawn',
      })
      .mockResolvedValue({ applied: true, conflict: false, revision: 10 });

    render(SettingsView);
    await openTab('Consents');

    // The other client withdrew the consent meanwhile: the page says so in words, not in the wire
    // value, and its own resend is the withdrawal.
    await waitFor(() => expect(screen.getByTestId('withdraw-scan_harnesses')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('withdraw-scan_harnesses'));

    const current = await screen.findByTestId('conflict-current');
    const yours = await screen.findByTestId('conflict-yours');
    expect(current).toHaveTextContent('Withdrawn in the other client');
    expect(yours).toHaveTextContent('Withdraw the consent');

    await fireEvent.click(screen.getByTestId('conflict-apply'));
    await waitFor(() =>
      expect(updateSpy).toHaveBeenLastCalledWith('consent:scan_harnesses', null, 4),
    );
  });

  it('clears the changed mark after the documents mode is saved', async () => {
    vi.spyOn(apiClient, 'getMachineSettings')
      .mockResolvedValueOnce(settingsRecord())
      .mockResolvedValue(settingsRecord({ documents_mode: 'copy', revision: 5 }));
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 5,
    });

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());
    await fireEvent.click(screen.getByLabelText('Copy'));
    await fireEvent.click(screen.getByTestId('save-documents'));

    // The page's read is now the value it saved: no mark, and the Save is held back again.
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeDisabled());
    expect(screen.queryByTestId('documents-changed')).not.toBeInTheDocument();
  });

  it('keeps the other categories when one of them has its own retention', async () => {
    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue(
      settingsRecord({
        cleanup_durations: { ...settingsRecord().cleanup_durations, epics_after_days: 90 },
      }),
    );
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true, conflict: false, revision: 5 });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalled());
    const sent = JSON.parse(updateSpy.mock.calls[0]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    expect(sent.epics_after_days).toBe(90);
  });

  it('saves the reminder while plans are never offered', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true, conflict: false, revision: 5 });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    // The field's own never choice; `-1` never reaches the view.
    await fireEvent.click(screen.getByRole('checkbox', { name: 'Never' }));
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalled());
    const sent = JSON.parse(updateSpy.mock.calls[0]?.[1] ?? '{}') as Record<string, number>;
    // The contract's "never" is a retention duration of its own, not a value below the minimum.
    expect(sent.plans_after_days).toBe(-1);
    expect(sent.remind_after_days).toBe(7);
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

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => expect(screen.getByTestId('conflict-apply')).toBeInTheDocument());

    // The other client changes again between the two answers: the second resend still applies the
    // operator's categories over the newer record, not the one the first resend was built for.
    await fireEvent.click(screen.getByTestId('conflict-apply'));
    await waitFor(() => expect(screen.getByTestId('conflict-apply')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(3));
    const sent = JSON.parse(updateSpy.mock.calls[2]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    expect(sent.remind_after_days).toBe(7);
    expect(sent.epics_after_days).toBe(95);
    expect(sent.logs_after_days).toBe(95);
    expect(sent.reports_after_days).toBe(31);
    expect(sent.unenrolled_after_days).toBe(31);
  });

  it('applies the operator’s categories over a readable record the other client left', async () => {
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

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.input(screen.getByTestId('cleanup-remind-days'), { target: { value: '7' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => expect(screen.getByTestId('conflict-apply')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(2));
    const sent = JSON.parse(updateSpy.mock.calls[1]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    expect(sent.remind_after_days).toBe(7);
    expect(sent.epics_after_days).toBe(90);
    expect(sent.logs_after_days).toBe(90);
    expect(sent.reports_after_days).toBe(30);
    expect(sent.unenrolled_after_days).toBe(30);
  });

  it('shows the documents mode conflict in the catalogue’s words, never the wire word', async () => {
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValue({
      applied: false,
      conflict: true,
      revision: 9,
      current_value: 'copy',
    });

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());
    await fireEvent.click(screen.getByLabelText('Move'));
    await fireEvent.click(screen.getByTestId('save-documents'));

    const current = await screen.findByTestId('conflict-current');
    const yours = await screen.findByTestId('conflict-yours');
    expect(current).toHaveTextContent('Current value: Copy');
    expect(yours).toHaveTextContent('Your value: Move');
  });

  it('keeps the stored categories when a conflict’s current value is not a record', async () => {
    // The other client left a value the page cannot read as a record: the resend must not fall back
    // to the defaults, or the categories the operator did not touch would be reset to 30/3.
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: '30',
      })
      .mockResolvedValueOnce({ applied: true, conflict: false, revision: 10 });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => expect(screen.getByTestId('conflict-apply')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(2));
    const sent = JSON.parse(updateSpy.mock.calls[1]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    // The stored record's categories stand in for the unreadable value.
    const stored = settingsRecord();
    expect(sent.epics_after_days).toBe(stored.cleanup_durations.epics_after_days);
    expect(sent.remind_after_days).toBe(stored.cleanup_durations.remind_after_days);
  });

  it('shows in the Yours line what the resend will send, not the page’s old record', async () => {
    vi.spyOn(apiClient, 'updateMachineSetting').mockResolvedValueOnce({
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
    });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    // Only the plans value is the operator's.
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    const yours = await screen.findByTestId('conflict-yours');
    // The resend sends the operator's plans over the other client's record, so the line names 90,
    // not the 30 the page read before the other client changed it.
    expect(yours).toHaveTextContent('Archived epics: 90 days');
    expect(yours).toHaveTextContent('Ask me again after 4 days');
    expect(yours).not.toHaveTextContent('{');
  });

  it('names the minimum when the server refuses a duration as invalid', async () => {
    vi.spyOn(apiClient, 'updateMachineSetting').mockRejectedValue(new Error('invalid_value'));

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    // The runtime's refusal names what the client pre-check names: the contract's minimum.
    await waitFor(() => {
      expect(screen.getByText('At least 7 days, or never.')).toBeInTheDocument();
    });
  });

  it('keeps a category added to the record when the page saves', async () => {
    const stored = settingsRecord();
    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue(
      settingsRecord({
        cleanup_durations: { ...stored.cleanup_durations, custom_after_days: 12 },
      }),
    );
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true, conflict: false, revision: 5 });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });
    await fireEvent.click(screen.getByTestId('save-cleanup'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalled());
    const sent = JSON.parse(updateSpy.mock.calls[0]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.custom_after_days).toBe(12);
  });

  it('does not turn a store refresh into the operator’s change of the reminder', async () => {
    const updateSpy = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: JSON.stringify({
          plans_after_days: 35,
          epics_after_days: 30,
          logs_after_days: 30,
          reports_after_days: 30,
          unenrolled_after_days: 30,
          remind_after_days: 7,
        }),
      })
      .mockResolvedValue({ applied: true, conflict: false, revision: 10 });

    render(SettingsView);
    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('save-cleanup')).toBeInTheDocument());
    // The operator changes the plans only; the page read a reminder of 3.
    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });

    // A settings event refreshes the store meanwhile; the page's read is still the one that says
    // what the operator changed.
    const refreshed = settingsRecord();
    runtimeStore.update((state) => ({
      ...state,
      machineSettings: {
        ...refreshed,
        cleanup_durations: { ...refreshed.cleanup_durations, remind_after_days: 5 },
      },
    }));

    await fireEvent.click(screen.getByTestId('save-cleanup'));
    await waitFor(() => expect(screen.getByTestId('conflict-apply')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => expect(updateSpy).toHaveBeenCalledTimes(2));
    const sent = JSON.parse(updateSpy.mock.calls[1]?.[1] ?? '{}') as Record<string, number>;
    expect(sent.plans_after_days).toBe(45);
    // The reminder the operator did not touch keeps the other client's value.
    expect(sent.remind_after_days).toBe(7);
  });

  it('shows the documents modes and the cleanup durations on their tabs', async () => {
    render(SettingsView);

    await waitFor(() => expect(screen.getByTestId('documents-section')).toBeInTheDocument());
    expect(screen.getByLabelText('Copy')).toBeInTheDocument();
    expect(screen.getByLabelText('Move')).toBeInTheDocument();
    expect(screen.getByLabelText('Ask each time')).toBeInTheDocument();

    await openTab('Retention');
    await waitFor(() => expect(screen.getByTestId('cleanup-days')).toHaveValue(30));
  });

  it('keeps the documents Save disabled until the mode changes, and resets to the inherited value', async () => {
    render(SettingsView);

    const save = await screen.findByTestId('save-documents');
    // One Save per form, disabled until something changed, with the reason in the form's line.
    expect(save).toBeDisabled();
    expect(screen.getByTestId('documents-form-status')).toHaveTextContent('Nothing has changed yet.');

    await fireEvent.click(screen.getByLabelText('Copy'));

    expect(save).toBeEnabled();
    // The changed field carries its mark beside it.
    expect(screen.getByTestId('documents-changed')).toBeInTheDocument();

    // Use the inherited value drops the change and returns to the recorded one.
    await fireEvent.click(screen.getByTestId('documents-reset'));

    expect(save).toBeDisabled();
    expect(screen.getByLabelText('Ask each time')).toBeChecked();
    expect(screen.queryByTestId('documents-changed')).not.toBeInTheDocument();
  });

  it('keeps the cleanup Save disabled until a duration changes, marks it, and resets it', async () => {
    render(SettingsView);
    await openTab('Retention');

    const save = await screen.findByTestId('save-cleanup');
    expect(save).toBeDisabled();

    await fireEvent.input(screen.getByTestId('cleanup-days'), { target: { value: '45' } });

    expect(save).toBeEnabled();
    expect(screen.getByTestId('cleanup-changed-plans')).toBeInTheDocument();
    // The field the operator did not touch carries no mark.
    expect(screen.queryByTestId('cleanup-changed-remind')).not.toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('cleanup-reset'));

    expect(save).toBeDisabled();
    expect(screen.getByTestId('cleanup-days')).toHaveValue(30);
    expect(screen.queryByTestId('cleanup-changed-plans')).not.toBeInTheDocument();
  });
});
