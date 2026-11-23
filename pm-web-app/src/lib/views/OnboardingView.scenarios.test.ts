import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { tick } from 'svelte';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import { get } from 'svelte/store';
import OnboardingView from './OnboardingView.svelte';
import { loadOnboardingState, runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type OnboardingState } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

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
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(onboardingState({}));
  });

  afterEach(() => {
    vi.restoreAllMocks();
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

    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('remember', { remember: true }));
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
    expect(screen.getByTestId('model-toggle-claude-sonnet-5.5')).not.toBeChecked();
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 2 models');

    await fireEvent.click(screen.getByTestId('models-record'));
    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('set', {}));

    // Tokens, never money: no currency sign and no price appears anywhere on the screen.
    expect(screen.queryByText(/[$€£]/)).not.toBeInTheDocument();
  });

  it.each([
    [
      'the roles by their accept link',
      {
        state: 'roles.review',
        roles: [{ role: 'implement', harness_id: 'claude', model_id: 'sonnet-5.5' }],
        links: {
          back: { href: '/api/v1/onboarding/roles/back', next: 'catalogue.select' },
          accept: { href: '/api/v1/onboarding/roles/accept', next: 'store.choose' },
        },
      },
      'roles-record',
      'accept',
    ],
    [
      'the chosen store by its own link',
      {
        state: 'store.choose',
        stores: [
          { kind: 'os', name: 'Keychain', available: true },
          { kind: 'file', name: 'File', available: true, recommended: true },
        ],
        links: {
          os: { href: '/api/v1/onboarding/store/os', next: 'providers.select' },
          file: { href: '/api/v1/onboarding/store/file', next: 'providers.select' },
        },
      },
      'store-record',
      'file',
    ],
    [
      'the plan defaults by their accept link',
      {
        state: 'defaults.review',
        landing: 'landing.pull-request',
        method: 'method.test-first',
        asking: 'ask.key-points',
        links: {
          back: { href: '/api/v1/onboarding/defaults/back', next: 'providers.select' },
          accept: { href: '/api/v1/onboarding/defaults/accept', next: 'onboard.completed' },
        },
      },
      'defaults-record',
      'accept',
    ],
  ])('records %s', async (_case, partial, button, link) => {
    arrange(onboardingState(partial as Partial<OnboardingState>));
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'onboard.completed' }));
    render(OnboardingView);

    await fireEvent.click(await screen.findByTestId(button));

    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith(link, {}));
  });

  it('shows each plan-default axis with plain names, a sentence of meaning and no raw unit id', () => {
    arrange(
      onboardingState({
        state: 'defaults.review',
        summary: 'How should plans run on this machine?',
        landing: 'landing.pull-request',
        method: 'method.test-first',
        asking: 'ask.key-points',
        links: { accept: { href: '/api/v1/onboarding/defaults/accept', next: 'onboard.completed' } },
      }),
    );

    render(OnboardingView);

    const card = screen.getByTestId('defaults-screen');
    // Per axis: the name, one sentence of meaning, and the variants with their plain names.
    expect(card).toHaveTextContent('Landing');
    expect(card).toHaveTextContent("How a plan's work reaches the base branch.");
    expect(card).toHaveTextContent('Branch and Pull Request');
    expect(card).toHaveTextContent('review and merge on the provider');
    expect(card).toHaveTextContent('Commit on the base branch');
    expect(card).toHaveTextContent('Files only');
    expect(card).toHaveTextContent('Method');
    expect(card).toHaveTextContent('Test first');
    expect(card).toHaveTextContent('Tests after');
    expect(card).toHaveTextContent('Asking');
    expect(card).toHaveTextContent('At the key points');

    // The variant the runtime reports is the one chosen.
    expect(screen.getByRole('radio', { name: /Branch and Pull Request/ })).toBeChecked();
    expect(screen.getByRole('radio', { name: /Test first/ })).toBeChecked();
    expect(screen.getByRole('radio', { name: /At the key points/ })).toBeChecked();

    // Posture is a later run's line: the first run never offers it.
    expect(card).not.toHaveTextContent('Posture');

    // No raw unit id of the runtime reaches the page.
    for (const raw of ['landing.pull-request', 'method.test-first', 'ask.key-points']) {
      expect(document.body.textContent).not.toContain(raw);
    }
  });

  it('greys a variant the machine cannot run with its reason and records a changed axis', async () => {
    arrange(
      onboardingState({
        state: 'defaults.review',
        summary: 'How should plans run on this machine?',
        landing: 'landing.pull-request',
        method: 'method.test-first',
        asking: 'ask.key-points',
        unavailable: [
          {
            axis: 'landing',
            value: 'landing.main',
            reason: 'needs a protected base branch: none is known at machine level',
          },
        ],
        links: { accept: { href: '/api/v1/onboarding/defaults/accept', next: 'onboard.completed' } },
      } as Partial<OnboardingState>),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'onboard.completed' }));

    render(OnboardingView);

    // A variant that cannot run on this machine is disabled, and its reason stands beside it.
    expect(screen.getByRole('radio', { name: /Commit on the base branch/ })).toBeDisabled();
    expect(
      screen.getByText('needs a protected base branch: none is known at machine level'),
    ).toBeInTheDocument();
    // Nothing changed yet, so the button offers the shipped defaults.
    expect(screen.getByTestId('defaults-record')).toHaveTextContent('Use these defaults');

    // A changed line makes the button save the choices, and only the changed axis travels.
    await fireEvent.click(screen.getByRole('radio', { name: /Files only/ }));
    expect(screen.getByTestId('defaults-record')).toHaveTextContent('Save these choices');
    await fireEvent.click(screen.getByTestId('defaults-record'));

    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('accept', { landing: 'landing.files' }));
  });

  it('offers the posture on a later run only, preselected at Not set while it is unset', async () => {
    arrange(
      onboardingState({
        state: 'defaults.reconfigure',
        summary: 'How should plans run on this machine?',
        landing: 'landing.pull-request',
        method: 'method.test-first',
        asking: 'ask.key-points',
        links: { accept: { href: '/api/v1/onboarding/defaults/accept', next: 'settings.record' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'settings.record' }));

    render(OnboardingView);

    const card = screen.getByTestId('defaults-screen');
    expect(card).toHaveTextContent('Posture');
    expect(card).toHaveTextContent('How full the plan');
    expect(screen.getByRole('radio', { name: /Not set: each plan asks/ })).toBeChecked();
    expect(card).not.toHaveTextContent('posture.unset');

    // The posture travels as its own bare value, and only because this line changed.
    await fireEvent.click(screen.getByRole('radio', { name: /^Full/ }));
    await fireEvent.click(screen.getByTestId('defaults-record'));
    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('accept', { posture: 'full' }));
  });

  it('waits for an available variant when a refresh greys the chosen one', () => {
    arrange(
      onboardingState({
        state: 'defaults.review',
        summary: 'How should plans run on this machine?',
        landing: 'landing.main',
        method: 'method.test-first',
        asking: 'ask.key-points',
        unavailable: [
          { axis: 'landing', value: 'landing.main', reason: 'needs a protected base branch' },
        ],
        links: { accept: { href: '/api/v1/onboarding/defaults/accept', next: 'onboard.completed' } },
      } as Partial<OnboardingState>),
    );

    render(OnboardingView);

    // The chosen line is greyed now: the screen says why it cannot record it.
    expect(screen.getByTestId('defaults-record')).toBeDisabled();
    expect(screen.getByTestId('defaults-blocked')).toHaveTextContent(
      'Choose an available variant for Landing first.',
    );
  });

  it('preselects the posture the state records on a later run', () => {
    arrange(
      onboardingState({
        state: 'defaults.reconfigure',
        summary: 'How should plans run on this machine?',
        landing: 'landing.pull-request',
        method: 'method.test-first',
        asking: 'ask.key-points',
        posture: 'standard',
        links: { accept: { href: '/api/v1/onboarding/defaults/accept', next: 'settings.record' } },
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('radio', { name: /^Standard/ })).toBeChecked();
    expect(screen.getByRole('radio', { name: /Not set: each plan asks/ })).not.toBeChecked();
  });

  it('groups the roles by phase, marks a departure and shows the advisory of a role', async () => {
    arrange(
      onboardingState({
        state: 'roles.review',
        summary: 'Give every role a harness and a model',
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code' },
          { harness_id: 'opencode', name: 'OpenCode' },
        ],
        roles: [
          {
            role: 'outline',
            class: 'read_only',
            phase: 'planning',
            harness_id: 'claude',
            model_id: 'opus-5.5',
            effort: 'high',
            origin: 'default',
            advisory: {
              does: 'Turns the refined request into deliverables and the plan lane.',
              needs: 'Deep reasoning over a large context; few tool calls.',
              why: 'A weak outline makes more work later than a frontier model takes here.',
              lower_tier: 'More loop-backs from the Q-Gate.',
              steward: 'no departure',
            },
          },
          {
            role: 'qgate',
            class: 'read_only',
            phase: 'review',
            harness_id: 'opencode',
            model_id: 'qwen-coder-32b',
            effort: '',
            origin: 'steward',
            default: { harness_id: 'claude', model_id: 'sonnet-5.5', effort: 'medium' },
            departure: {
              harness_id: 'opencode',
              model_id: 'qwen-coder-32b',
              effort: '',
              reason: 'reads only; a fast model suffices',
            },
            advisory: {
              does: 'Checks the artifacts against the plan.',
              needs: 'A steady reader of long documents.',
              why: 'The default sits above what the checks need.',
              lower_tier: 'Not qualified for the frontier tier.',
              steward: 'reads only; a fast model suffices',
            },
          },
          {
            role: 'screener',
            class: 'zero_tool',
            phase: 'review',
            harness_id: '',
            model_id: '',
            effort: '',
            origin: 'unmet',
            advisory: {
              does: 'Sorts the entry the operator hands over.',
              needs: 'A fast model of the zero-tool class.',
              why: 'The tier is not met by any chosen model.',
              lower_tier: 'none',
              steward: 'no departure',
            },
          },
        ],
        links: {
          ask_steward: { href: '/api/v1/onboarding/roles/consent', next: 'roles.consent' },
          accept: { href: '/api/v1/onboarding/roles/validate', next: 'store.choose' },
        },
      }),
    );

    render(OnboardingView);

    const card = screen.getByTestId('roles-screen');
    // The roles are grouped by the phase they serve.
    expect(card).toHaveTextContent('Planning');
    expect(card).toHaveTextContent('Review');
    expect(screen.getByTestId('roles-phase-planning')).toContainElement(
      screen.getByTestId('role-row-outline'),
    );
    expect(screen.getByTestId('roles-phase-review')).toContainElement(
      screen.getByTestId('role-row-qgate'),
    );

    // Each row names its harness (by the name the state reports), model and effort.
    const outline = screen.getByTestId('role-row-outline');
    expect(outline).toHaveTextContent('outline');
    expect(outline).toHaveTextContent('Claude Code');
    expect(outline).toHaveTextContent('opus-5.5');
    expect(outline).toHaveTextContent('high');
    expect(outline).toHaveTextContent('Default');

    // A departure is marked, names what the steward proposes, and carries its own accept and reject.
    const qgate = screen.getByTestId('role-row-qgate');
    expect(qgate).toHaveTextContent('Departure from the steward');
    expect(screen.getByTestId('role-departure-proposed-qgate')).toHaveTextContent(
      'Proposed: OpenCode · qwen-coder-32b',
    );
    expect(screen.getByTestId('role-departure-proposed-qgate')).toHaveTextContent(
      'reads only; a fast model suffices',
    );
    expect(screen.getByTestId('role-departure-accept-qgate')).toBeInTheDocument();
    expect(screen.getByTestId('role-departure-reject-qgate')).toBeInTheDocument();
    // The overview counts the roles the default assignment covers.
    expect(card).toHaveTextContent('The default assignment is applied to all 3 roles.');
    // The class of each role is a word, never the state's own enum.
    expect(screen.getByTestId('role-row-qgate')).toHaveTextContent('reads only');
    expect(card).not.toHaveTextContent('read_only');

    // The advisory of a role opens in place, with what it does and what it needs.
    await fireEvent.click(screen.getByTestId('role-advisory-outline'));
    const advisory = screen.getByTestId('role-advisory-body-outline');
    expect(advisory).toHaveTextContent('Deep reasoning over a large context; few tool calls.');
    expect(advisory).toHaveTextContent('More loop-backs from the Q-Gate.');

    // An unmet role stands empty and assignable, and the record button waits for it.
    const screener = screen.getByTestId('role-row-screener');
    expect(screener).toHaveTextContent('Open');
    expect(screener).toHaveTextContent('no harness');
    expect(screen.getByTestId('role-change-screener')).toBeInTheDocument();
    expect(screen.getByTestId('roles-record')).toBeDisabled();
    expect(screen.getByTestId('roles-record')).toHaveTextContent('Assign the 1 open role first');
  });

  it('decides a departure per role and records a role changed by hand', async () => {
    arrange(
      onboardingState({
        state: 'roles.review',
        summary: 'Give every role a harness and a model',
        roles: [
          {
            role: 'qgate',
            class: 'read_only',
            phase: 'review',
            harness_id: 'opencode',
            model_id: 'qwen-coder-32b',
            effort: '',
            origin: 'steward',
            departure: {
              harness_id: 'opencode',
              model_id: 'qwen-coder-32b',
              effort: '',
              reason: 'reads only; a fast model suffices',
            },
          },
          {
            role: 'implement',
            class: 'write',
            phase: 'execution',
            harness_id: 'claude',
            model_id: 'sonnet-5.5',
            effort: 'medium',
            origin: 'default',
          },
        ],
        links: {
          ask_steward: { href: '/api/v1/onboarding/roles/consent', next: 'roles.consent' },
          accept: { href: '/api/v1/onboarding/roles/validate', next: 'store.choose' },
        },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'store.choose' }));

    render(OnboardingView);

    // The steward is one act; a departure is taken or left per role.
    expect(screen.getByTestId('roles-ask-steward')).toHaveTextContent('Ask the steward');
    await fireEvent.click(screen.getByTestId('role-departure-accept-qgate'));
    expect(screen.getByTestId('role-row-qgate')).toHaveTextContent('Departure accepted');

    // The operator changes a role's harness, model and effort in place.
    await fireEvent.click(screen.getByTestId('role-change-implement'));
    await fireEvent.change(screen.getByTestId('role-harness-implement'), {
      target: { value: 'opencode' },
    });
    await fireEvent.input(screen.getByTestId('role-model-implement'), {
      target: { value: 'qwen-coder-32b' },
    });
    await fireEvent.change(screen.getByTestId('role-effort-implement'), {
      target: { value: 'low' },
    });
    expect(screen.getByTestId('role-row-implement')).toHaveTextContent('Changed');

    await fireEvent.click(screen.getByTestId('roles-record'));

    await waitFor(() =>
      expect(stepSpy).toHaveBeenCalledWith('accept', {
        departures: [{ role: 'qgate', decision: 'accept' }],
        roles: [
          {
            role: 'implement',
            harness_id: 'opencode',
            model_id: 'qwen-coder-32b',
            effort: 'low',
          },
        ],
      }),
    );
  });

  it('shows the failed provider under Provider, with its token entry and the three acts', async () => {
    arrange(
      onboardingState({
        state: 'providers.failed',
        summary: 'The provider could not be verified',
        provider: 'SonarCloud',
        provider_id: 'sonar',
        cause: '401 "Invalid token"',
        fix: 'enter the token again',
        links: {
          check_again: { href: '/api/v1/onboarding/providers/check-again', next: 'providers.verify' },
          continue_without: {
            href: '/api/v1/onboarding/providers/continue-without',
            next: 'providers.record',
          },
        },
      } as Partial<OnboardingState>),
    );

    render(OnboardingView);

    const card = screen.getByTestId('providers-failed');
    // What the state is about: one provider, under the label Provider.
    expect(card).toHaveTextContent('Provider');
    expect(card).toHaveTextContent('SonarCloud');
    expect(card).toHaveTextContent('401 "Invalid token"');

    // "Enter the token again" opens the token entry for that provider.
    expect(screen.queryByTestId('credential-entry')).not.toBeInTheDocument();
    await fireEvent.click(screen.getByTestId('providers-failed-enter-token'));
    expect(screen.getByTestId('credential-entry')).toBeInTheDocument();
    expect(screen.getByTestId('credential-secret-input')).toBeInTheDocument();

    // The other two acts are the state's own links, in its words.
    expect(screen.getByRole('button', { name: 'Check again' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue without' })).toBeInTheDocument();
  });

  it('offers no token entry for a provider whose state names no id', () => {
    arrange(
      onboardingState({
        state: 'providers.failed',
        summary: 'The provider could not be verified',
        provider: 'SonarCloud',
        cause: '401 "Invalid token"',
        links: {
          check_again: { href: '/api/v1/onboarding/providers/check-again', next: 'providers.verify' },
        },
      } as Partial<OnboardingState>),
    );

    render(OnboardingView);

    // Storing a token under a guessed key would put it where the check never looks.
    expect(screen.getByTestId('providers-failed')).toHaveTextContent('SonarCloud');
    expect(screen.queryByTestId('providers-failed-enter-token')).not.toBeInTheDocument();
  });

  it('rejects a departure and returns the row to the default of the role', async () => {
    arrange(
      onboardingState({
        state: 'roles.review',
        summary: 'Give every role a harness and a model',
        roles: [
          {
            role: 'qgate',
            class: 'read_only',
            phase: 'review',
            harness_id: 'opencode',
            model_id: 'qwen-coder-32b',
            effort: '',
            origin: 'steward',
            default: { harness_id: 'claude', model_id: 'sonnet-5.5', effort: 'medium' },
            departure: {
              harness_id: 'opencode',
              model_id: 'qwen-coder-32b',
              effort: '',
              reason: 'reads only; a fast model suffices',
            },
          },
        ],
        links: { accept: { href: '/api/v1/onboarding/roles/validate', next: 'store.choose' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'store.choose' }));

    render(OnboardingView);

    await fireEvent.click(screen.getByTestId('role-departure-reject-qgate'));

    const row = screen.getByTestId('role-row-qgate');
    expect(row).toHaveTextContent('Departure rejected');
    expect(row).toHaveTextContent('claude · sonnet-5.5');

    await fireEvent.click(screen.getByTestId('roles-record'));

    // The decision is the record; the row is no hand change of its own.
    await waitFor(() =>
      expect(stepSpy).toHaveBeenCalledWith('accept', {
        departures: [{ role: 'qgate', decision: 'reject' }],
      }),
    );
  });

  it('marks a role the validation refused with its reason', () => {
    arrange(
      onboardingState({
        state: 'roles.review',
        summary: 'Give every role a harness and a model',
        roles: [
          {
            role: 'outline',
            class: 'read_only',
            phase: 'planning',
            harness_id: 'claude',
            model_id: 'opus-5.5',
            effort: 'high',
            validation: 'no model of that tier on this harness',
          },
        ],
        links: { accept: { href: '/api/v1/onboarding/roles/validate', next: 'store.choose' } },
      }),
    );

    render(OnboardingView);

    expect(screen.getByTestId('role-validation-outline')).toHaveTextContent(
      'Not accepted: no model of that tier on this harness',
    );
  });

  it('drops a decision whose proposal the state reports differently', async () => {
    const rolesWith = (reason: string) => [
      {
        role: 'qgate',
        class: 'read_only',
        phase: 'review',
        harness_id: 'opencode',
        model_id: 'qwen-coder-32b',
        effort: '',
        origin: 'steward',
        default: { harness_id: 'claude', model_id: 'sonnet-5.5', effort: 'medium' },
        departure: {
          harness_id: 'opencode',
          model_id: 'qwen-coder-32b',
          effort: '',
          reason,
        },
      },
    ];
    const first = onboardingState({
      state: 'roles.review',
      summary: 'Give every role a harness and a model',
      roles: rolesWith('reads only; a fast model suffices'),
      links: { accept: { href: '/api/v1/onboarding/roles/validate', next: 'store.choose' } },
    });
    arrange(first);
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(first);

    render(OnboardingView);

    await fireEvent.click(screen.getByTestId('role-departure-accept-qgate'));
    expect(screen.getByTestId('role-row-qgate')).toHaveTextContent('Departure accepted');

    // A refresh reports another proposal for the same role: the old decision no longer holds.
    const refreshed = onboardingState({
      state: 'roles.review',
      summary: 'Give every role a harness and a model',
      roles: rolesWith('the checks grew; a stronger model is needed'),
      links: { accept: { href: '/api/v1/onboarding/roles/validate', next: 'store.choose' } },
    });
    runtimeStore.update((store) => ({
      ...store,
      onboarding: refreshed,
      currentState: refreshed as never,
    }));

    await waitFor(() =>
      expect(screen.getByTestId('role-row-qgate')).not.toHaveTextContent('Departure accepted'),
    );
    expect(screen.getByTestId('role-row-qgate')).toHaveTextContent('Departure from the steward');
  });

  it('shows the provider that failed with its cause and fix, and tries again by its link', async () => {
    arrange(
      onboardingState({
        state: 'providers.failed',
        summary: 'The provider could not be reached',
        provider: 'github',
        cause: 'the token was refused',
        fix: 'gh auth login',
        links: { retry: { href: '/api/v1/onboarding/providers/retry', next: 'providers.select' } },
      } as Partial<OnboardingState>),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'providers.select' }));
    render(OnboardingView);

    expect(await screen.findByText('github')).toBeInTheDocument();
    expect(screen.getByText('the token was refused')).toBeInTheDocument();
    expect(screen.getByText('gh auth login')).toBeInTheDocument();
    const [retry] = screen
      .getAllByRole('button')
      .filter((candidate) => candidate.closest('.state-card__actions'));
    await fireEvent.click(retry);
    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('retry', {}));
  });

  it('names each harness that failed its check at harness.none, never the raw state id', () => {
    arrange(
      onboardingState({
        state: 'harness.none',
        summary: 'No harness can be used',
        checks: [
          {
            harness: 'claude',
            name: 'Claude Code',
            result: 'fail',
            cause: 'not logged in',
            fix: 'claude login',
          },
          {
            harness: 'opencode',
            name: 'OpenCode',
            result: 'fail',
            cause: 'version 0.9.2 is not supported (1.5 - 1.x)',
            fix: 'install a version in 1.5 - 1.x, then check again',
          },
        ],
        links: {
          recheck: { href: '/api/v1/onboarding/harness/recheck', next: 'harness.probe-failed' },
          later: { href: '/api/v1/onboarding/harness/none/later', next: 'harness.none' },
        },
      }),
    );

    render(OnboardingView);

    // The published shape of a failed check: the harness's name, the answer it gave and the fix.
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('No harness can be used');
    const failures = screen.getByTestId('harness-none-failures');
    expect(failures).toHaveTextContent('Claude Code');
    expect(failures).toHaveTextContent('not logged in');
    expect(failures).toHaveTextContent('claude login');
    expect(failures).toHaveTextContent('OpenCode');
    expect(failures).toHaveTextContent('version 0.9.2 is not supported (1.5 - 1.x)');

    // A raw state id never reaches the screen, and neither does the scope-and-id spelling of it.
    expect(document.body).not.toHaveTextContent('harness.none');
    expect(document.body).not.toHaveTextContent('Machine · harness.none');
  });

  it('shows a harness’s path and failed check when the runtime reports them', () => {
    arrange(
      onboardingState({
        state: 'harness.none',
        summary: 'No harness can be used',
        checks: [
          {
            harness: 'claude',
            name: 'Claude Code',
            path: '~/.local/bin/claude',
            check: 'login',
            result: 'fail',
            cause: 'not logged in',
            fix: 'claude login',
          },
          {
            harness: 'opencode',
            name: 'OpenCode',
            check: 'wordsmith',
            result: 'fail',
            cause: 'refused',
            fix: 'reinstall',
          },
          {
            harness: 'codex',
            name: 'Codex',
            path: '/opt/homebrew/bin/codex',
            result: 'pass',
            cause: '',
            fix: '',
          },
        ],
      }),
    );

    render(OnboardingView);

    const failures = screen.getByTestId('harness-none-failures');
    expect(failures).toHaveTextContent('~/.local/bin/claude');
    expect(failures).toHaveTextContent('the login check');
    // An id the catalogue does not know reads as a word, never as a raw key or a wire token.
    expect(failures).toHaveTextContent('Wordsmith');
    expect(failures).not.toHaveTextContent('onboard.harness.check.');
    expect(failures).not.toHaveTextContent('wordsmith');

    // A check that passed is not listed under "none passed".
    expect(failures).not.toHaveTextContent('Codex');
  });

  it('says what was looked for when nothing was found', () => {
    arrange(
      onboardingState({
        state: 'harness.none',
        summary: 'No harness found',
        looks_for: 'claude, opencode',
        locations: 'your PATH, ~/.local/bin',
      }),
    );

    render(OnboardingView);

    expect(
      screen.getByText('Looked for claude, opencode in your PATH, ~/.local/bin.'),
    ).toBeInTheDocument();
    expect(screen.queryByTestId('harness-none-failures')).not.toBeInTheDocument();
  });

  it('names the harnesses of a list and the place the runtime calls “in”', () => {
    arrange(
      onboardingState({
        state: 'harness.none',
        summary: 'No harness found',
        harnesses: [{ name: 'Claude Code' }, { harness_id: 'opencode' }],
        in: 'your PATH',
      }),
    );

    render(OnboardingView);

    expect(screen.getByText('Looked for Claude Code, opencode in your PATH.')).toBeInTheDocument();
  });

  it('names the aspect of a change link whose line the result does not report', () => {
    arrange(
      onboardingState({
        state: 'auto.result',
        summary: 'Everything is decided.',
        results: [{ aspect: 'language', value: 'Deutsch, models too', reason: 'detected' }],
        links: {
          'change:language': {
            href: '/api/v1/onboarding/settings/reconfigure/language',
            next: 'language.reconfigure',
          },
          'change:roles': {
            href: '/api/v1/onboarding/settings/reconfigure/roles',
            next: 'roles.reconfigure',
          },
          done: { href: '/api/v1/onboarding/auto/result/done', next: 'onboard.completed' },
        },
      }),
    );

    render(OnboardingView);

    // The row's act and the orphan link's act are told apart by the aspect each opens.
    expect(screen.getByRole('button', { name: 'Change Language' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Change Roles' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^change /i })).toHaveLength(2);
  });

  it('keeps a state the runtime reported while a step was still in flight', async () => {
    arrange(
      onboardingState({
        state: 'onboard.mode',
        summary: 'Set up this machine for plan-marshall',
        links: { begin: { href: '/api/v1/onboarding/language', next: 'language.choose' } },
      }),
    );
    let answer: ((state: OnboardingState) => void) | undefined;
    vi.spyOn(apiClient, 'stepOnboarding').mockReturnValue(
      new Promise<OnboardingState>((resolve) => {
        answer = resolve;
      }) as never,
    );

    render(OnboardingView);
    await fireEvent.click(await screen.findByRole('button', { name: /begin with step 1/i }));

    // A step-completed event refreshes the state while the step's own answer is still on its way.
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(
      onboardingState({ state: 'harness.probe', summary: 'Checking 2 harnesses' }),
    );
    await loadOnboardingState();

    // The older answer must not overwrite the newer state.
    answer?.(onboardingState({ state: 'language.choose', summary: 'Choose your languages' }));
    await waitFor(() =>
      expect(get(runtimeStore).onboarding?.summary).toBe('Checking 2 harnesses'),
    );
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

  it('offers a Change per row of auto.result that opens that aspect again', async () => {
    arrange(
      onboardingState({
        state: 'auto.result',
        summary: 'Everything is decided.',
        results: [
          { aspect: 'language', value: 'Deutsch, models too', reason: 'detected' },
          { aspect: 'harness', value: 'Claude Code 2.4.1 (default)', reason: 'all_supported' },
          { aspect: 'store', value: 'macOS Keychain', reason: 'detected' },
        ],
        links: {
          'change:language': {
            href: '/api/v1/onboarding/settings/reconfigure/language',
            next: 'language.reconfigure',
          },
          'change:harness': {
            href: '/api/v1/onboarding/settings/reconfigure/harnesses',
            next: 'harness.reconfigure-consent',
          },
          done: { href: '/api/v1/onboarding/auto/result/done', next: 'onboard.completed' },
        },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'auto.result' }));

    render(OnboardingView);

    // One Change per line the state can run again, named after that line; the credentials line
    // offers none because the state carries no link for it.
    const changes = screen.getAllByRole('button', { name: /^change /i });
    expect(changes).toHaveLength(2);
    expect(changes[0]).toHaveAccessibleName('Change Language');
    expect(changes[1]).toHaveAccessibleName('Change Harnesses');
    expect(screen.getByTestId('auto-result')).toHaveTextContent('Credentials');

    await fireEvent.click(changes[1]);
    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('change:harness', {}));
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

    // A runtime that sends the card's own title as the summary does not print it twice.
    expect(screen.getByTestId('completed-summary')).toHaveTextContent('Machine setup is complete.');
    expect(screen.getByTestId('completed-summary')).not.toHaveTextContent('This machine is ready');

    // A done step opened from the bar is read-only: it shows its summary, never a link to re-run.
    expect(screen.queryByRole('button', { name: /record this choice/i })).not.toBeInTheDocument();
  });

  it('shows the runtime’s own sentence under the completed title when it differs from it', () => {
    arrange(
      onboardingState({
        state: 'onboard.completed',
        summary: 'Seven steps taken on this machine',
        completed_steps: 7,
        step_summaries: { '1': 'Deutsch, models English' },
      }),
      7,
    );

    render(OnboardingView);

    expect(screen.getByTestId('completed-summary')).toHaveTextContent(
      'Seven steps taken on this machine',
    );
  });

  it('compares the completed summary with the title of the active language', () => {
    messagesStore.set(loadMessages('de'));
    currentLocale.set('de');
    arrange(
      onboardingState({
        state: 'onboard.completed',
        summary: 'Diese Maschine ist bereit',
        completed_steps: 7,
        step_summaries: { '1': 'Deutsch, Modelle Deutsch' },
      }),
      7,
    );

    try {
      render(OnboardingView);

      // The German title sent back as the summary is not printed twice either.
      expect(screen.getByTestId('completed-summary')).toHaveTextContent(
        'Die Einrichtung der Maschine ist abgeschlossen.',
      );
      expect(screen.getByTestId('completed-summary')).not.toHaveTextContent(
        'Diese Maschine ist bereit',
      );
    } finally {
      messagesStore.set(loadMessages('en'));
      currentLocale.set('en');
    }
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

  it('draws the missing-screen pattern for a state no screen claims, never its id', () => {
    arrange(
      onboardingState({
        state: 'unknown.mystery',
        summary: 'Something the app does not know',
        links: {},
      }),
    );

    render(OnboardingView);

    expect(screen.getByTestId('missing-screen')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'This screen is not available in the web app',
    );
    // The terminal command that shows the state is named, and a way back is offered.
    expect(screen.getByTestId('missing-screen')).toHaveTextContent('pm-operator');
    expect(screen.getByRole('button', { name: 'Back' })).toBeInTheDocument();
    // The state's id is never printed as content.
    expect(document.body.textContent).not.toContain('unknown.mystery');
  });

  it('sends every entry state to the fallback with the terminal command', () => {
    arrange(
      onboardingState({
        state: 'entry.lan-open',
        summary: 'Network listener open with its fingerprint',
        links: {},
      }),
    );

    render(OnboardingView);

    expect(screen.getByTestId('missing-screen')).toBeInTheDocument();
    expect(screen.getByTestId('missing-screen')).toHaveTextContent('pm-operator');
    expect(document.body.textContent).not.toContain('entry.lan-open');
  });

  it('takes the probe-only answer of a consent that offers one', async () => {
    const state = onboardingState({
      state: 'catalogue.consent',
      summary: 'Ask the harnesses for their models, and a model for advice?',
      kind: 'probe_harness_models',
      links: {
        no: { href: '/api/v1/onboarding/catalogue/decline', next: 'catalogue.consent' },
        no_advice: { href: '/api/v1/onboarding/catalogue/no-advice', next: 'catalogue.select' },
        yes: { href: '/api/v1/onboarding/catalogue/advice', next: 'catalogue.models' },
      },
    });
    arrange(state);
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'catalogue.select' }));

    render(OnboardingView);

    await fireEvent.click(await screen.findByTestId('consent-no-advice'));

    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('no_advice', {}));
  });

  it('offers no third answer on a consent that states only yes and no', async () => {
    arrange(
      onboardingState({
        state: 'harness.consent',
        summary: 'Look for harnesses on this machine?',
        links: {
          no: { href: '/api/v1/onboarding/harness/decline', next: 'harness.consent' },
          yes: { href: '/api/v1/onboarding/harness/scan', next: 'harness.choose' },
        },
      }),
    );

    render(OnboardingView);

    expect(await screen.findByTestId('consent-box')).toBeInTheDocument();
    expect(screen.queryByTestId('consent-no-advice')).not.toBeInTheDocument();
  });

  it('reviews the recorded setup of every step on the summary screen', () => {
    arrange(
      onboardingState({
        state: 'onboard.summary',
        summary: 'Review the setup of this machine',
        aspects: [
          { aspect: 'language', step: 1, summary: 'Deutsch, models English' },
          { aspect: 'roles', step: 4, summary: '31 roles assigned' },
        ],
        links: { done: { href: '/api/v1/onboarding/summary/done', next: 'onboard.completed' } },
      }),
    );

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'Review the setup of this machine',
    );
    const summary = screen.getByTestId('setup-summary');
    expect(summary).toHaveTextContent('Deutsch, models English');
    expect(summary).toHaveTextContent('31 roles assigned');
    expect(screen.getByRole('button', { name: /continue/i })).toBeInTheDocument();
  });
});

describe('catalogue.select recommendation and manual model entry (D8)', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(onboardingState({}));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('preselects the recommendation with a recommended mark, and drops the mark when unchecked', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        recommendation: 'Opus 4.6 for planning and review; Sonnet 4.6 for implementation.',
        models: [
          {
            harness_id: 'claude',
            model_id: 'opus-4.6',
            source: 'probe',
            recommended: true,
          },
          { harness_id: 'claude', model_id: 'sonnet-4.6', source: 'probe', recommended: true },
          { harness_id: 'claude', model_id: 'haiku-4.5', source: 'probe', recommended: false },
        ],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );

    const { container } = render(OnboardingView);

    const opusToggle = screen.getByTestId('model-toggle-claude-opus-4.6');
    const sonnetToggle = screen.getByTestId('model-toggle-claude-sonnet-4.6');
    const haikuToggle = screen.getByTestId('model-toggle-claude-haiku-4.5');
    expect(opusToggle).toBeChecked();
    expect(sonnetToggle).toBeChecked();
    expect(haikuToggle).not.toBeChecked();

    expect(screen.getByTestId('model-recommendation')).toHaveTextContent(
      'Opus 4.6 for planning and review; Sonnet 4.6 for implementation.',
    );
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 2 models');

    const opusRow = opusToggle.closest('li') as HTMLElement;
    const sonnetRow = sonnetToggle.closest('li') as HTMLElement;
    expect(opusRow).not.toBeNull();
    expect(sonnetRow).not.toBeNull();
    expect(within(opusRow).getByText('recommended')).toBeVisible();
    expect(within(sonnetRow).getByText('recommended')).toBeVisible();

    await fireEvent.click(opusToggle);

    expect(opusToggle).not.toBeChecked();
    expect(within(opusRow).queryByText('recommended')).toBeNull();
    expect(within(sonnetRow).getByText('recommended')).toBeVisible();
    expect(container.querySelectorAll('[data-marker="recommended"]')).toHaveLength(1);
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 1 model');
  });

  it('offers a text field to enter model ids for a harness that cannot list its models', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        models: [
          { harness_id: 'claude', model_id: 'opus-5.5', source: 'probe' },
          { harness_id: 'claude', model_id: 'sonnet-5.5', source: 'probe' },
          { harness_id: 'opencode', model_id: 'qwen-coder-32b', source: 'operator' },
        ],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'catalogue.record' }));

    const { container } = render(OnboardingView);

    const claudeGroup = container.querySelector('[data-harness="claude"]');
    const opencodeGroup = container.querySelector('[data-harness="opencode"]');
    expect(claudeGroup).not.toBeNull();
    expect(opencodeGroup).not.toBeNull();

    expect(within(claudeGroup as HTMLElement).queryByTestId('models-input-claude')).toBeNull();
    expect(opencodeGroup).toHaveTextContent('cannot list its models');

    const input = within(opencodeGroup as HTMLElement).getByTestId('models-input-opencode');
    await fireEvent.input(input, { target: { value: 'anthropic/claude-opus-4-6' } });
    await fireEvent.click(within(opencodeGroup as HTMLElement).getByTestId('models-add-opencode'));

    const addedToggle = screen.getByTestId('model-toggle-opencode-anthropic/claude-opus-4-6');
    expect(addedToggle).toBeChecked();
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 4 models');

    await fireEvent.click(screen.getByTestId('models-record'));
    await waitFor(() =>
      expect(stepSpy).toHaveBeenCalledWith('set', {
        models: expect.arrayContaining([
          expect.objectContaining({
            harness_id: 'opencode',
            model_id: 'anthropic/claude-opus-4-6',
            source: 'operator',
          }),
        ]),
      }),
    );
  });

  it('keeps the manual TextField when removing the sole operator model and avoids duplicates on re-add', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        models: [{ harness_id: 'opencode', model_id: 'qwen-coder-32b', source: 'operator' }],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );

    const { container } = render(OnboardingView);

    const opencodeGroup = container.querySelector('[data-harness="opencode"]') as HTMLElement;
    expect(opencodeGroup).not.toBeNull();
    expect(within(opencodeGroup).getByTestId('model-toggle-opencode-qwen-coder-32b')).toBeChecked();

    await fireEvent.click(within(opencodeGroup).getByRole('button', { name: /Remove/i }));

    expect(container.querySelector('[data-harness="opencode"]')).not.toBeNull();
    expect(opencodeGroup).toHaveTextContent('no model allowed');
    expect(within(opencodeGroup).getByTestId('models-input-opencode')).toBeVisible();

    await fireEvent.input(within(opencodeGroup).getByTestId('models-input-opencode'), {
      target: { value: 'qwen-coder-32b' },
    });
    await fireEvent.click(within(opencodeGroup).getByTestId('models-add-opencode'));

    expect(
      within(opencodeGroup).getAllByTestId('model-toggle-opencode-qwen-coder-32b'),
    ).toHaveLength(1);
    expect(within(opencodeGroup).getByTestId('model-toggle-opencode-qwen-coder-32b')).toBeChecked();
  });

  it('offers a text field for a harness the scan listed when the select state omits harnesses[]', async () => {
    arrange(
      onboardingState({
        state: 'harness.choose',
        summary: 'Choose the harnesses to enrol',
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code', found: true, supported: true },
          { harness_id: 'opencode', name: 'OpenCode', found: true, supported: true },
        ],
        links: {
          choose: { href: '/api/v1/onboarding/harness/confirm', next: 'harness.confirm' },
        },
      }),
    );

    render(OnboardingView);
    expect(await screen.findByTestId('harness-choose-screen')).toBeInTheDocument();
    expect(screen.getByText('OpenCode')).toBeInTheDocument();

    // The step lands on `catalogue.select`: the state names no `harnesses[]` and carries no row at
    // all for OpenCode, so only the scan the machine just listed can name that harness's group.
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        models: [{ harness_id: 'claude', model_id: 'opus-5.5', source: 'probe' }],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );
    await tick();

    const group = screen.getByRole('group', { name: 'OpenCode' });
    const input = within(group).getByTestId('models-input-opencode') as HTMLInputElement;
    expect(input).toBeVisible();
  });

  it('sends exactly the recorded rows for a changed selection', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        models: [
          {
            harness_id: 'claude',
            model_id: 'opus-5.5',
            source: 'probe',
            recommended: true,
            tokens: 1200,
          },
          { harness_id: 'claude', model_id: 'sonnet-5.5', recommended: true },
        ],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'catalogue.record' }));

    render(OnboardingView);

    await fireEvent.click(screen.getByTestId('model-toggle-claude-opus-5.5'));
    await fireEvent.click(screen.getByTestId('models-record'));

    await waitFor(() =>
      expect(stepSpy).toHaveBeenCalledWith('set', {
        models: [{ harness_id: 'claude', model_id: 'sonnet-5.5', source: 'probe' }],
      }),
    );
  });

  it('sends an empty body when the selection is unchanged', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        models: [
          { harness_id: 'claude', model_id: 'opus-5.5', source: 'probe' },
          { harness_id: 'claude', model_id: 'sonnet-5.5', source: 'probe' },
        ],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'catalogue.record' }));

    render(OnboardingView);

    await fireEvent.click(screen.getByTestId('models-record'));

    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('set', {}));
  });

  it('renders no advice reason and reads no cannot-list flag: only the published shape speaks', () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        no_advice_reason: 'steward timed out',
        harnesses: [{ harness_id: 'opencode', name: 'OpenCode', cannot_list: false, models_listed: true }],
        models: [{ harness_id: 'opencode', model_id: 'qwen-coder-32b', source: 'operator' }],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );

    const { container } = render(OnboardingView);

    expect(container.textContent).not.toContain('steward timed out');
    const opencodeGroup = container.querySelector('[data-harness="opencode"]') as HTMLElement;
    expect(within(opencodeGroup).getByTestId('models-input-opencode')).toBeVisible();
    expect(within(opencodeGroup).getByTestId('model-toggle-opencode-qwen-coder-32b')).toBeChecked();
  });

  it('keeps probe models preselected when prose recommendation carries no per-row recommended flags', () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        recommendation: 'All listed models fit standard coding tasks.',
        models: [
          { harness_id: 'claude', model_id: 'opus-5.5', source: 'probe', reason: 'strongest planner' },
          { harness_id: 'claude', model_id: 'sonnet-5.5', source: 'probe' },
        ],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );

    render(OnboardingView);

    expect(screen.getByTestId('model-recommendation')).toHaveTextContent(
      'All listed models fit standard coding tasks.',
    );
    expect(screen.getByTestId('model-toggle-claude-opus-5.5')).toBeChecked();
    expect(screen.getByTestId('model-toggle-claude-sonnet-5.5')).toBeChecked();
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 2 models');
    expect(screen.getByTestId('models-record')).toBeEnabled();
  });

  it('keeps added operator models when models is absent during store updates and a failed submit', async () => {
    arrange(
      onboardingState({
        state: 'catalogue.select',
        summary: 'Choose the models plans may use',
        harnesses: [{ harness_id: 'opencode', name: 'OpenCode', cannot_list: true }],
        links: { set: { href: '/api/v1/onboarding/catalogue/record', next: 'catalogue.record' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockRejectedValueOnce(new Error('temporary failure'));

    render(OnboardingView);

    await fireEvent.input(screen.getByTestId('models-input-opencode'), {
      target: { value: 'anthropic/claude-opus-4-6' },
    });
    await fireEvent.click(screen.getByTestId('models-add-opencode'));
    expect(screen.getByTestId('model-toggle-opencode-anthropic/claude-opus-4-6')).toBeChecked();

    runtimeStore.update((store) => ({
      ...store,
      unreadReportsCount: store.unreadReportsCount + 1,
    }));
    await waitFor(() =>
      expect(screen.getByTestId('model-toggle-opencode-anthropic/claude-opus-4-6')).toBeChecked(),
    );

    await fireEvent.click(screen.getByTestId('models-record'));
    await waitFor(() => expect(stepSpy).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByTestId('models-record')).toBeEnabled());

    expect(screen.getByTestId('model-toggle-opencode-anthropic/claude-opus-4-6')).toBeChecked();
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 1 model');
  });
});

describe('store.choose protection lines per option (D9)', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(onboardingState({}));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('shows the generic protection line and no platform claim on the published store rows', () => {
    arrange(
      onboardingState({
        state: 'store.choose',
        summary: 'Where should this machine keep its tokens?',
        stores: [
          {
            kind: 'os',
            name: 'macOS Keychain',
            available: true,
            recommended: true,
          },
          {
            kind: 'file',
            name: 'Files in ~/.plan-marshall-mcp/credentials/',
            available: true,
            recommended: false,
          },
        ],
        links: {
          os: { href: '/api/v1/onboarding/store/os', next: 'store.consent' },
          file: { href: '/api/v1/onboarding/store/file', next: 'store.check' },
        },
      }),
    );

    const { container } = render(OnboardingView);

    expect(screen.getAllByText(/^Protects a secret against:/)).toHaveLength(2);
    expect(screen.getAllByText(/^Does not protect against:/)).toHaveLength(2);

    const osOption = screen.getByTestId('store-os').closest('li') as HTMLElement;
    expect(osOption).toHaveTextContent(
      'Protects a secret against: anything the store itself guarantees',
    );
    expect(osOption).toHaveTextContent(
      'Does not protect against: anything the store itself does not guarantee',
    );
    expect(osOption).not.toHaveTextContent('macOS asking first');
    expect(within(osOption).getByText('recommended')).toBeVisible();

    const fileOption = screen.getByTestId('store-file').closest('li') as HTMLElement;
    expect(fileOption).not.toHaveTextContent('mode 0600');
    expect(container.querySelectorAll('[data-marker="recommended"]')).toHaveLength(1);
  });

  it('reads no platform fact: a Linux Secret Service option carries no macOS or Secret Service copy', () => {
    arrange(
      onboardingState({
        state: 'store.choose',
        summary: 'Where should this machine keep its tokens?',
        platform: 'linux',
        stores: [
          {
            kind: 'os',
            name: 'GNOME Keyring',
            backend: 'secret_service',
            available: true,
            recommended: true,
          },
        ],
        links: { os: { href: '/api/v1/onboarding/store/os', next: 'store.consent' } },
      }),
    );

    const { container } = render(OnboardingView);

    expect(container).not.toHaveTextContent('without macOS asking first');
    expect(container).not.toHaveTextContent('Secret Service');
    expect(container).toHaveTextContent('anything the store itself guarantees');
  });

  it('records os_unavailable for an unavailable OS store', async () => {
    arrange(
      onboardingState({
        state: 'store.choose',
        summary: 'Where should this machine keep its tokens?',
        stores: [
          { kind: 'file', name: 'the file store', available: true },
          { kind: 'os', name: 'GNOME Keyring', available: false },
        ],
        links: {
          file: { href: '/api/v1/onboarding/store/file', next: 'store.check' },
          os_unavailable: {
            href: '/api/v1/onboarding/store/unavailable',
            next: 'store.unavailable',
          },
        },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'store.unavailable' }));

    render(OnboardingView);

    await fireEvent.change(screen.getByTestId('store-os'));
    await fireEvent.click(screen.getByTestId('store-record'));

    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('os_unavailable', {}));
  });

  it('records no link for a store of a kind the state offers no link for', async () => {
    arrange(
      onboardingState({
        state: 'store.choose',
        summary: 'Where should this machine keep its tokens?',
        stores: [
          { kind: 'file', name: 'the file store', available: true },
          { kind: 'vault', name: 'Vault', available: true },
        ],
        links: { file: { href: '/api/v1/onboarding/store/file', next: 'store.check' } },
      }),
    );
    const stepSpy = vi.spyOn(apiClient, 'stepOnboarding');

    render(OnboardingView);

    await fireEvent.change(screen.getByTestId('store-vault'));
    await fireEvent.click(screen.getByTestId('store-record'));

    expect(stepSpy).not.toHaveBeenCalled();
  });

  it('keeps the store reconfiguration action enabled when store.reconfigure-result carries no stores array', async () => {
    arrange(
      onboardingState({
        state: 'store.reconfigure-result',
        summary: 'Where should this machine keep its tokens?',
        current: 'keychain',
        links: { os: { href: '/api/v1/onboarding/store/record', next: 'auto.result' } },
      }),
    );
    const stepSpy = vi
      .spyOn(apiClient, 'stepOnboarding')
      .mockResolvedValue(onboardingState({ state: 'auto.result' }));

    render(OnboardingView);

    const recordBtn = screen.getByTestId('store-record');
    expect(recordBtn).toBeEnabled();
    await fireEvent.click(recordBtn);
    await waitFor(() => expect(stepSpy).toHaveBeenCalledWith('os', {}));
  });
});

describe('harness.choose monogram and token usage wording (D10)', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');
    vi.spyOn(apiClient, 'getOnboardingState').mockResolvedValue(onboardingState({}));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('draws a monogram when no icon is fetched and says usage complete, usage partial or usage not reported', () => {
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
            harness_id: 'opencode',
            name: 'OpenCode',
            path: '/opt/homebrew/bin/opencode',
            version: '1.9.0',
            found: true,
            supported: true,
            worker_eligible: true,
            token_usage: 'partial',
            missing_figures: ['cache_read', 'cache_write'],
          },
          {
            harness_id: 'aider',
            name: 'Aider',
            path: '/opt/homebrew/bin/aider',
            version: '0.72.0',
            found: true,
            supported: true,
            worker_eligible: false,
            token_usage: 'partial',
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
        ],
        links: { choose: { href: '/api/v1/onboarding/harness/choose', next: 'harness.probe-consent' } },
      }),
    );

    const { container } = render(OnboardingView);

    expect(screen.getByTestId('harness-monogram-claude')).toHaveTextContent('CC');
    expect(screen.getByTestId('harness-monogram-opencode')).toHaveTextContent('O');
    expect(screen.getByTestId('harness-monogram-codex')).toHaveTextContent('C');

    expect(container.querySelector('[data-harness="claude"]')).toHaveTextContent('usage complete');
    expect(container.querySelector('[data-harness="claude"]')?.textContent).not.toContain('complete:');
    expect(container.querySelector('[data-harness="opencode"]')).toHaveTextContent(
      'usage partial (not reported: cache read, cache write)',
    );
    expect(container.querySelector('[data-harness="aider"]')).toHaveTextContent('usage partial');
    expect(container.querySelector('[data-harness="codex"]')).toHaveTextContent('usage not reported');
  });

  it('localises partial missing figures in German on harness.choose', () => {
    try {
      messagesStore.set(loadMessages('de'));
      currentLocale.set('de');
      arrange(
        onboardingState({
          state: 'harness.choose',
          summary: 'Wähle die Harnesses zum Eintragen',
          harnesses: [
            {
              harness_id: 'claude',
              name: 'Claude Code',
              found: true,
              supported: true,
              worker_eligible: true,
              token_usage: 'complete',
              default: true,
            },
            {
              harness_id: 'opencode',
              name: 'OpenCode',
              found: true,
              supported: true,
              worker_eligible: true,
              token_usage: 'partial',
              missing_figures: ['cache_read', 'cache_write'],
            },
          ],
          links: { choose: { href: '/api/v1/onboarding/harness/choose', next: 'harness.probe-consent' } },
        }),
      );

      const { container } = render(OnboardingView);

      expect(container.querySelector('[data-harness="claude"]')).toHaveTextContent(
        'Tokenverbrauch vollständig',
      );
      expect(container.querySelector('[data-harness="opencode"]')).toHaveTextContent(
        'Tokenverbrauch teilweise (nicht gemeldet: aus dem Cache gelesen, in den Cache geschrieben)',
      );
    } finally {
      messagesStore.set(loadMessages('en'));
      currentLocale.set('en');
    }
  });

  it('renders TokensScreen for harness.tokens-* with the one usage wording of harness.choose', () => {
    arrange(
      onboardingState({
        state: 'harness.tokens-partial',
        summary: 'How each harness reports tokens',
        harnesses: [
          {
            harness_id: 'claude',
            name: 'Claude Code',
            token_usage: 'complete',
            token_detail: 'input, cache read, output',
          },
          {
            harness_id: 'opencode',
            name: 'OpenCode',
            token_usage: 'partial',
            missing_figures: ['cache_read', 'cache_write'],
          },
          {
            harness_id: 'codex',
            name: 'Codex',
            token_usage: 'not_reported',
          },
        ],
        links: { ok: { href: '/api/v1/onboarding/harness/tokens', next: 'catalogue.consent' } },
      }),
    );

    render(OnboardingView);

    const list = screen.getByTestId('token-reporting');
    expect(list).toHaveTextContent('Claude Code');
    expect(list).toHaveTextContent('usage complete');
    expect(list).not.toHaveTextContent('complete:');
    expect(list).not.toHaveTextContent('input, cache read');
    expect(list).toHaveTextContent('OpenCode');
    expect(list).toHaveTextContent('usage partial (not reported: cache read, cache write)');
    expect(list).toHaveTextContent('Codex');
    expect(list).toHaveTextContent('usage not reported');
  });

  it('draws a fetched icon without any stray markup fragment', () => {
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
            icon_url: '/api/v1/harness-icons/claude.svg',
            default: true,
          },
        ],
        links: { choose: { href: '/api/v1/onboarding/harness/choose', next: 'harness.probe-consent' } },
      }),
    );

    render(OnboardingView);

    expect(screen.getByTestId('harness-icon-claude')).toBeInTheDocument();
    expect(screen.getByTestId('harness-choose-screen').textContent).not.toContain('/>');
  });
});
