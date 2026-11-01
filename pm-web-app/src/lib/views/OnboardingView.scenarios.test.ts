import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import OnboardingView from './OnboardingView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type OnboardingState } from '../api/client';

/**
 * The onboarding as a view test against the states `pm-mock-runtime` reports.
 *
 * Each case arranges the state the mock scenario is in, renders the one route, and asserts what
 * the operator sees and what the runtime is asked to do.
 */

function onboardingState(partial: Partial<OnboardingState>): OnboardingState {
  return {
    scope: 'machine',
    state: 'onboard.mode',
    summary: 'Set up this machine for plan-marshall',
    completed_steps: 0,
    links: {},
    ...partial,
  };
}

function arrange(state: OnboardingState, completedSteps = 0): void {
  runtimeStore.set(
    makeRuntimeState({
      isInitialized: true,
      isPaired: true,
      isOnboarded: state.state === 'onboard.completed',
      deviceLabel: 'Firefox on mbp-oliver',
      machineName: 'mbp-oliver',
      currentRoute: 'onboarding',
      currentState: state,
      onboarding: state,
      isRevoked: false,
      revokedReason: '',
      isOffline: false,
      isListenerClosed: false,
      completedSteps,
      activeStep: 1,
      unreadReportsCount: 0,
      openDecisionsCount: 0,
    }),
  );
  // The view pulls the state on mount; the mock reports the state this case arranges.
  vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(state);
}

describe('OnboardingView against the mock runtime states', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(onboardingState({}));
  });

  it('renders the seven steps of the start screen in the message catalogue', () => {
    arrange(
      onboardingState({
        state: 'onboard.mode',
        links: {
          auto: { href: '/api/v1/onboarding/auto', next: 'auto.consent' },
          begin: { href: '/api/v1/onboarding/language', next: 'language.choose' },
        },
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'Set up this machine for plan-marshall',
    );
    const steps = screen.getByTestId('steps-overview');
    expect(steps).toHaveTextContent('Language');
    expect(steps).toHaveTextContent('Harnesses');
    expect(steps).toHaveTextContent('Models');
    expect(steps).toHaveTextContent('Roles');
    expect(steps).toHaveTextContent('Credentials');
    expect(steps).toHaveTextContent('Providers');
    expect(steps).toHaveTextContent('Plans');
    expect(screen.getByRole('button', { name: /decide everything for me/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /begin with step 1/i })).toBeInTheDocument();
  });

  it('asks a consent with the fields the runtime states, and remembers only on request', async () => {
    arrange(
      onboardingState({
        state: 'harness.consent',
        summary: 'Look for harnesses on this machine?',
        kind: 'scan_harnesses',
        looks_for: 'claude, opencode, codex, agy',
        in: 'your PATH, ~/.local/bin',
        runs: '<program> --version on each one found',
        never: 'reads other files, starts a session, uses the network',
        tokens: 'none',
        links: {
          no: { href: '/api/v1/onboarding/harness/decline', next: 'harness.consent' },
          remember: { href: '/api/v1/onboarding/harness/consent', next: 'harness.choose' },
          yes: { href: '/api/v1/onboarding/harness/scan', next: 'harness.choose' },
        },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'harness.choose' }));

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      'Look for harnesses on this machine?',
    );
    expect(screen.getByText('claude, opencode, codex, agy')).toBeInTheDocument();
    expect(screen.getByText('<program> --version on each one found')).toBeInTheDocument();
    expect(screen.getByText('none')).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('consent-remember'));
    await fireEvent.click(screen.getByRole('button', { name: /^look for harnesses$/i }));

    await waitFor(() =>
      expect(stepSpy).toHaveBeenCalledWith('remember', { remember: true }),
    );
  });

  it('says a consent is for this run only when it is never remembered', () => {
    arrange(
      onboardingState({
        state: 'auto.consent',
        summary: 'Decide everything for me?',
        kind: 'auto_run',
        links: {
          no: { href: '/api/v1/onboarding/auto-decline', next: 'onboard.mode' },
          yes: { href: '/api/v1/onboarding/auto/run', next: 'auto.run' },
        },
      }),
    );

    render(OnboardingView);

    expect(screen.getByText('This consent is for this run only.')).toBeInTheDocument();
    expect(screen.queryByTestId('consent-remember')).not.toBeInTheDocument();
  });

  it('shows the model at work with the role, harness, model and the tokens expected', () => {
    arrange(
      onboardingState({
        state: 'catalogue.advise',
        summary: 'The steward is advising',
        role: 'steward',
        harness: 'Claude Code',
        model: 'sonnet-5.5',
        tokens: 'about 50 k tokens',
        links: {},
      }),
    );

    render(OnboardingView);

    expect(screen.getByText(/steward is working on Claude Code/)).toBeInTheDocument();
    expect(screen.getByText('about 50 k tokens')).toBeInTheDocument();
    expect(screen.getByText(/you can leave this page/i)).toBeInTheDocument();
  });

  it('renders the language choice with both languages and records the choice', async () => {
    arrange(
      onboardingState({
        state: 'language.choose',
        summary: 'Choose your languages',
        interface: 'en',
        model: 'same',
        links: { set: { href: '/api/v1/onboarding/language/set', next: 'harness.consent' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'harness.consent' }));

    render(OnboardingView);

    expect(screen.getByTestId('language-screen')).toBeInTheDocument();
    expect(screen.getAllByLabelText('English').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByLabelText('Deutsch').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId('language-record')).toHaveTextContent('Use English for both');

    await fireEvent.click(screen.getByTestId('language-record'));
    await waitFor(() =>
      expect(stepSpy).toHaveBeenCalledWith('set', {
        language_interface: 'en',
        language_model: 'en',
      }),
    );
  });

  it('lists every found harness with its tokens and cannot choose one that cannot run workers', () => {
    arrange(
      onboardingState({
        state: 'harness.choose',
        summary: 'Choose the harnesses to enrol',
        harnesses: [
          {
            harness_id: 'claude',
            name: 'Claude Code',
            path: '~/.local/bin/claude',
            version: '2.4.1',
            found: true,
            supported: true,
            worker_eligible: true,
            token_usage: 'complete',
            default: true,
          },
          {
            harness_id: 'codex',
            name: 'Codex',
            path: '/opt/homebrew/bin/codex',
            version: '0.31.2',
            found: true,
            supported: true,
            worker_eligible: false,
            token_usage: 'not_reported',
          },
          {
            harness_id: 'agy',
            name: 'Antigravity',
            found: false,
            supported: false,
            worker_eligible: false,
            token_usage: 'not_reported',
          },
        ],
        links: { choose: { href: '/api/v1/onboarding/harness/choose', next: 'harness.probe' } },
      }),
    );

    render(OnboardingView);

    expect(screen.getByTestId('harness-toggle-claude')).toBeChecked();
    expect(screen.getByTestId('harness-toggle-codex')).toBeDisabled();
    expect(screen.getByText('cannot run workers')).toBeInTheDocument();
    expect(screen.getByText('not found')).toBeInTheDocument();
    expect(screen.getByTestId('harness-check')).toHaveTextContent(
      'Check 1 harness, Claude Code the default',
    );
  });

  it('shows the failing harness with its cause and fix', () => {
    arrange(
      onboardingState({
        state: 'harness.blocked',
        summary: 'OpenCode is not ready',
        harness: 'opencode',
        cause: 'not logged in',
        fix: 'opencode auth login',
        links: {
          choose_again: { href: '/api/v1/onboarding/harness/choose', next: 'harness.choose' },
          leave_out: { href: '/api/v1/onboarding/harness/leave-out', next: 'harness.confirm' },
          recheck: { href: '/api/v1/onboarding/harness/recheck', next: 'harness.probe' },
        },
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('OpenCode is not ready');
    expect(screen.getByText('not logged in')).toBeInTheDocument();
    expect(screen.getByText('opencode auth login')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /leave it out/i })).toBeInTheDocument();
  });

  it('allows the models a plan may use and names the count in tokens, never money', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        models: [
          { harness_id: 'claude', model_id: 'opus-5.5', source: 'probe', recommended: true },
          { harness_id: 'claude', model_id: 'sonnet-5.5', source: 'probe' },
          { harness_id: 'opencode', model_id: 'qwen-coder-32b', source: 'operator' },
        ],
        links: { set: { href: '/api/v1/onboarding/catalogue/set', next: 'catalogue.record' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'catalogue.record' }));

    render(OnboardingView);

    expect(screen.getByTestId('model-toggle-claude-opus-5.5')).toBeChecked();
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 3 models');

    await fireEvent.click(screen.getByTestId('models-record'));
    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('set', {}));

    // Tokens, never money: no currency sign and no price appears anywhere on the screen.
    expect(screen.queryByText(/[$€£]/)).not.toBeInTheDocument();
  });

  it('shows the automatic run result per aspect and continues', async () => {
    arrange(
      onboardingState({
        state: 'auto.result',
        summary: 'Everything is decided.',
        results: [
          { aspect: 'language', value: 'Deutsch, models too', reason: 'detected' },
          { aspect: 'harness', value: 'Claude Code 2.4.1 (default)', reason: 'all_supported' },
        ],
        links: { done: { href: '/api/v1/onboarding/auto/result/done', next: 'onboard.completed' } },
      }),
    );

    render(OnboardingView);

    const result = screen.getByTestId('auto-result');
    expect(result).toHaveTextContent('Deutsch, models too');
    expect(result).toHaveTextContent('every supported one, the first the default');
    expect(screen.getByRole('button', { name: /continue/i })).toBeInTheDocument();
  });

  it('renders the completed onboarding read-only, with the recorded summary of each done step', () => {
    arrange(
      onboardingState({
        state: 'onboard.completed',
        summary: 'This machine is ready',
        completed_steps: 7,
        step_summaries: {
          '1': 'Deutsch, models English',
          '2': 'Claude Code 2.4.1 (default), OpenCode 1.9.0',
          '3': '3 models on 2 harnesses',
        },
        links: { settings: { href: '/api/v1/onboarding/settings', next: 'settings.choose' } },
      }),
      7,
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('This machine is ready');
    const completed = screen.getByTestId('completed-steps');
    expect(completed).toHaveTextContent('Deutsch, models English');
    expect(completed).toHaveTextContent('Claude Code 2.4.1 (default), OpenCode 1.9.0');

    // A done step opened from the bar is read-only: it shows its summary, never a link to re-run.
    expect(screen.queryByRole('button', { name: /record this choice/i })).not.toBeInTheDocument();
  });

  it('opens a done step from the step bar as a read-only summary', async () => {
    arrange(
      onboardingState({
        state: 'harness.consent',
        summary: 'Look for harnesses on this machine?',
        step_summaries: { '1': 'Deutsch, models English' },
        links: { no: { href: '/x', next: 'harness.consent' } },
      }),
      1,
    );

    render(OnboardingView);

    await fireEvent.click(screen.getByTestId('step-button-1'));
    await waitFor(() => expect(screen.getByTestId('step-summary')).toBeInTheDocument());
    expect(screen.getByTestId('step-summary')).toHaveTextContent('Deutsch, models English');
    expect(screen.getByTestId('step-summary')).toHaveTextContent('Recorded.');

    // The bar never navigates ahead: step 2 is not yet done and offers no button.
    expect(screen.queryByTestId('step-button-2')).not.toBeInTheDocument();
  });

  it('renders every string through the message catalogue, in English and in German', async () => {
    const { setLocale, messagesStore } = await import('../i18n');
    const { loadMessages } = await import('../../test/catalogue-fixture');
    arrange(onboardingState({ state: 'onboard.mode' }));

    render(OnboardingView);
    expect(screen.getByRole('button', { name: /begin with step 1/i })).toBeInTheDocument();

    // The German pack is the one the catalogue ships; no string is written in the component.
    messagesStore.set(loadMessages('de'));
    await setLocale('de');

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
        'Maschine für plan-marshall einrichten',
      );
    });
    expect(screen.getByRole('button', { name: /mit schritt 1 beginnen/i })).toBeInTheDocument();
  });
});
