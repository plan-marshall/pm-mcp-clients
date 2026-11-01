import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/svelte';
import OnboardingView from './OnboardingView.svelte';
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
});
