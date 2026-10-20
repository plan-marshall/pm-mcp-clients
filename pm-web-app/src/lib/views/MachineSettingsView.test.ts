import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import MachineSettingsView from './MachineSettingsView.svelte';
import { runtimeStore } from '../stores/runtime';
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
      completedSteps: 0,
      activeStep: 1,
    });

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
      completedSteps: 0,
      activeStep: 1,
    });

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
