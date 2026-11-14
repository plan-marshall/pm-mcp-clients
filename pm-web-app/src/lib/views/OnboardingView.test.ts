import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/svelte';
import OnboardingView from './OnboardingView.svelte';
import { apiClient, type OnboardingState } from '../api/client';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

describe('OnboardingView component', () => {
  beforeEach(() => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: false,
        deviceLabel: 'Test Browser',
        machineName: 'mbp-oliver',
        currentRoute: 'onboarding',
        currentState: {
          state: 'onboard.mode',
          summary: 'Choose onboarding mode',
        },
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 1,
      }),
    );
  });

  it('renders onboarding mode with 7 step overview', () => {
    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'Set up this machine for plan-marshall',
    );
    expect(screen.getAllByText(/Language/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Harnesses/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: /decide everything for me/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /begin with step 1/i })).toBeInTheDocument();
  });

  it('renders consent box when in consent state', () => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: false,
        deviceLabel: 'Test Browser',
        machineName: 'mbp-oliver',
        currentRoute: 'onboarding',
        currentState: {
          state: 'harness.consent',
          summary: 'Look for coding agents',
          looks_for: 'Claude Code, OpenCode, Aider',
          runs: 'which claude, which opencode',
        },
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 2,
      }),
    );

    render(OnboardingView);

    expect(screen.getByText('Look for coding agents')).toBeInTheDocument();
    expect(screen.getByText('Claude Code, OpenCode, Aider')).toBeInTheDocument();
    expect(screen.getByText('which claude, which opencode')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^yes$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /not now/i })).toBeInTheDocument();
  });

  it('renders model working indicator when probing', () => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: false,
        deviceLabel: 'Test Browser',
        machineName: 'mbp-oliver',
        currentRoute: 'onboarding',
        currentState: {
          state: 'harness.probe',
          summary: 'Checking harnesses',
        },
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 2,
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Checking harnesses');
    expect(screen.getByText(/steward is working/i)).toBeInTheDocument();
  });

  it('renders completed card when onboard.completed is reached', () => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Test Browser',
        machineName: 'mbp-oliver',
        currentRoute: 'onboarding',
        currentState: {
          state: 'onboard.completed',
          summary: 'Machine setup is complete',
        },
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 8,
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('This machine is ready');
    expect(screen.getByRole('button', { name: /add a project/i })).toBeInTheDocument();
  });

  it('does not repeat the completion title as the body', () => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Test Browser',
        machineName: 'mbp-oliver',
        currentRoute: 'onboarding',
        currentState: {
          state: 'onboard.completed',
          summary: 'This machine is ready',
        },
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 8,
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('This machine is ready');
    expect(screen.getByText('Machine setup is complete.')).toBeInTheDocument();
    expect(screen.queryAllByText('This machine is ready')).toHaveLength(1);
  });

  it('opens at the step another screen asked for', async () => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Test Browser',
        machineName: 'mbp-oliver',
        currentRoute: 'onboarding',
        currentState: {
          state: 'onboard.completed',
          summary: 'Machine setup is complete',
        },
        onboarding: {
          state: 'onboard.completed',
          completed_steps: 7,
          step_summaries: { '2': 'Claude Code 2.4.1 (default)' },
        },
        onboardingStep: 2,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 7,
        activeStep: 8,
      }),
    );

    render(OnboardingView);

    await waitFor(() => {
      expect(screen.getByTestId('step-summary')).toHaveTextContent('Claude Code 2.4.1 (default)');
    });
  });

  describe('against the runtime', () => {
    /**
     * @param state the onboarding state the runtime reports
     * @returns the spy of the step the view takes
     */
    function arrange(state: OnboardingState) {
      vi.spyOn(apiClient, 'isPaired').mockReturnValue(true);
      vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(state);
      return vi.spyOn(apiClient, 'stepOnboarding').mockResolvedValue(state);
    }

    const CONSENT: OnboardingState = {
      state: 'harness.consent',
      kind: 'scan_harnesses',
      summary: 'Look for coding agents',
      links: {
        no: { href: '/api/v1/onboarding/harness/consent/no' },
        yes: { href: '/api/v1/onboarding/harness/consent/yes' },
        remember: { href: '/api/v1/onboarding/harness/consent/remember' },
      },
    };

    it('names the consent by its kind and takes the yes link', async () => {
      const step = arrange(CONSENT);
      render(OnboardingView);

      await fireEvent.click(await screen.findByRole('button', { name: 'Look for harnesses' }));

      expect(step).toHaveBeenCalledWith('yes', { remember: false });
    });

    it('takes the remember link when the operator chose not to be asked again', async () => {
      const step = arrange(CONSENT);
      render(OnboardingView);

      await fireEvent.click(await screen.findByTestId('consent-remember'));
      await fireEvent.click(screen.getByRole('button', { name: 'Look for harnesses' }));

      expect(step).toHaveBeenCalledWith('remember', { remember: true });
    });

    it('takes the no link when the operator declines', async () => {
      const step = arrange(CONSENT);
      render(OnboardingView);

      await fireEvent.click(await screen.findByRole('button', { name: 'Not now' }));

      expect(step).toHaveBeenCalledWith('no', { remember: false });
    });

    it('falls back to the last link to accept and the first to decline, under their labels', async () => {
      const step = arrange({
        state: 'store.check-consent',
        summary: 'Check the store?',
        links: { later: { href: '/later' }, default: { href: '/default' } },
      });
      render(OnboardingView);

      await fireEvent.click(await screen.findByRole('button', { name: 'Continue' }));
      expect(step).toHaveBeenLastCalledWith('default', { remember: false });

      await fireEvent.click(screen.getByRole('button', { name: 'Not now' }));
      expect(step).toHaveBeenLastCalledWith('later', { remember: false });
    });

    it('never offers to remember the consent of the automatic run', async () => {
      arrange({ ...CONSENT, state: 'auto.consent', kind: 'auto_run' });
      render(OnboardingView);

      await screen.findByRole('button', { name: 'Decide everything for me' });
      expect(screen.queryByTestId('consent-remember')).toBeNull();
    });

    it('offers to go on without the advice while the steward works', async () => {
      const step = arrange({
        state: 'catalogue.advise',
        summary: 'The steward advises',
        tokens: 'about 4,000 tokens',
        links: { no_advice: { href: '/no-advice' } },
      });
      render(OnboardingView);

      expect(await screen.findByText('about 4,000 tokens')).toBeInTheDocument();
      await fireEvent.click(screen.getByRole('button', { name: 'Continue without the advice' }));

      expect(step).toHaveBeenCalledWith('no_advice', {});
    });

    it('lists every done step with the summary the runtime reported, or says it reported none', async () => {
      arrange({
        state: 'onboard.completed',
        completed_steps: 3,
        aspects: [
          { step: 1, summary: 'English' },
          { number: 2, value: 'Claude Code 2.4.1' },
          { step: 3 },
        ],
      });
      runtimeStore.update((s) => ({ ...s, completedSteps: 3 }));
      render(OnboardingView);

      const done = await screen.findByTestId('completed-steps');
      const rows = within(done).getAllByRole('listitem');
      expect(rows.map((row) => row.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
        'Language English',
        'Harnesses Claude Code 2.4.1',
        'Models This step is recorded; no summary was reported for it.',
      ]);
    });

    it('says the state could not be loaded and loads it again on request', async () => {
      vi.spyOn(apiClient, 'isPaired').mockReturnValue(true);
      const read = vi
        .spyOn(apiClient, 'getOnboardingState')
        .mockRejectedValueOnce(new Error('offline'))
        .mockResolvedValue({ state: 'onboard.mode' });
      runtimeStore.update((s) => ({ ...s, isPaired: true }));
      render(OnboardingView);

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'The onboarding state could not be loaded.',
      );
      await fireEvent.click(screen.getByRole('button', { name: 'Load it again' }));

      await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
      expect(read).toHaveBeenCalledTimes(2);
    });
  });
});
