import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import OnboardingView from './OnboardingView.svelte';
import { runtimeStore } from '../stores/runtime';

describe('OnboardingView component', () => {
  beforeEach(() => {
    runtimeStore.set({
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
    });
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
    runtimeStore.set({
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
    });

    render(OnboardingView);

    expect(screen.getByText('Look for coding agents')).toBeInTheDocument();
    expect(screen.getByText('Claude Code, OpenCode, Aider')).toBeInTheDocument();
    expect(screen.getByText('which claude, which opencode')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^yes$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /not now/i })).toBeInTheDocument();
  });

  it('renders model working indicator when probing', () => {
    runtimeStore.set({
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
    });

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Checking harnesses');
    expect(screen.getByText(/steward is working/i)).toBeInTheDocument();
  });

  it('renders completed card when onboard.completed is reached', () => {
    runtimeStore.set({
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
    });

    render(OnboardingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('This machine is ready');
    expect(screen.getByRole('button', { name: /add a project/i })).toBeInTheDocument();
  });
});
