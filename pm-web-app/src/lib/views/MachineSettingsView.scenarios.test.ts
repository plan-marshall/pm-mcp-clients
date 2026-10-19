import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import MachineSettingsView from './MachineSettingsView.svelte';
import { runtimeStore } from '../stores/runtime';
import { apiClient, type MachineSettingsRecord } from '../api/client';

/**
 * The machine settings as a view test against the states `pm-mock-runtime` reports
 * (`onboarding-settings-conflict` and the settings record of `onboarding`).
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

describe('MachineSettingsView against the mock runtime states', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');

    runtimeStore.set({
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
      completedSteps: 7,
      activeStep: 8,
      unreadReportsCount: 0,
      openDecisionsCount: 0,
    });

    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue(settingsRecord());
  });

  it('shows all seven aspects with their recorded summary', async () => {
    render(MachineSettingsView);

    await waitFor(() => expect(screen.getByTestId('aspect-list')).toBeInTheDocument());

    for (const aspect of [
      'language',
      'harness',
      'catalogue',
      'roles',
      'store',
      'providers',
      'defaults',
    ]) {
      expect(
        screen.getByTestId('aspect-list').querySelector(`[data-aspect="${aspect}"]`),
      ).not.toBeNull();
    }
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

    render(MachineSettingsView);

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

    render(MachineSettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('save-documents'));

    await waitFor(() => expect(screen.getByTestId('conflict-dialog')).toBeInTheDocument());
    expect(screen.getByTestId('conflict-dialog')).toHaveTextContent('Current value: copy');
    expect(screen.getByTestId('conflict-dialog')).toHaveTextContent('Your value: ask');

    // Apply again: the value this page wanted, on the revision the other client left behind.
    await fireEvent.click(screen.getByTestId('conflict-apply'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenLastCalledWith('documents_mode', 'ask', 4);
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

    render(MachineSettingsView);
    await waitFor(() => expect(screen.getByTestId('save-documents')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('save-documents'));
    await waitFor(() => expect(screen.getByTestId('conflict-keep')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('conflict-keep'));

    await waitFor(() => expect(screen.queryByTestId('conflict-dialog')).not.toBeInTheDocument());
    expect(loadSpy).toHaveBeenCalled();
  });

  it('marks an expired token with the plans it holds and offers rotate and replace', async () => {
    render(MachineSettingsView);

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

    render(MachineSettingsView);
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

  it('shows the documents modes and the cleanup durations', async () => {
    render(MachineSettingsView);

    await waitFor(() => expect(screen.getByTestId('documents-section')).toBeInTheDocument());
    expect(screen.getByLabelText('Copy')).toBeInTheDocument();
    expect(screen.getByLabelText('Move')).toBeInTheDocument();
    expect(screen.getByLabelText('Ask each time')).toBeInTheDocument();
    expect(screen.getByTestId('cleanup-days')).toHaveValue(30);
  });
});
