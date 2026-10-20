import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ProjectOnboardingView from './ProjectOnboardingView.svelte';
import { apiClient, type ProjectExplorer, type ProjectOnboardingState } from '../api/client';

const RUN: ProjectOnboardingState = {
  project_id: 'pm-mcp-clients',
  onboarding_id: 'onb-pm-mcp-clients',
  checkout_path: '/Users/oliver/git/pm-mcp-clients',
  step: 3,
  total_steps: 10,
  state: 'project.enrolment',
  summary: 'Enrol pm-mcp-clients with these settings?',
  chat_available: true,
  build_environment: {
    captured_at: '2026-10-18T08:00:00Z',
    source: 'terminal',
    variables: { PATH: '/usr/bin', JAVA_HOME: '/opt/java' },
  },
  links: { enrol: { href: '/api/v1/projects/pm-mcp-clients/onboarding/step' } },
};

const EMPTY_EXPLORER: ProjectExplorer = {
  favourites: [],
  recent: [],
  all_projects: [],
  being_added: [],
};

const BEING_ADDED: ProjectExplorer = {
  ...EMPTY_EXPLORER,
  being_added: [
    {
      checkout_path: '~/git/cui-parent',
      current_step: 'Identity',
      step_number: 2,
      total_steps: 10,
      continue_link: '/api/v1/entries/ent-cui/continue',
      abandon_link: '/api/v1/entries/ent-cui/abandon',
    },
  ],
};

describe('ProjectOnboardingView component', () => {
  beforeEach(() => {
    vi.spyOn(apiClient, 'startProjectOnboarding').mockResolvedValue({ ok: true });
    vi.spyOn(apiClient, 'abandonOnboarding').mockResolvedValue();
  });

  it('renders the ten steps and the captured build environment of the current run', async () => {
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue(RUN);

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(screen.getByTestId('project-onboarding-step')).toBeInTheDocument());

    for (let step = 1; step <= 10; step++) {
      expect(screen.getByTestId(`project-step-${step}`)).toBeInTheDocument();
    }
    expect(screen.getByTestId('project-step-1')).toHaveTextContent('done');
    expect(screen.getByTestId('project-step-3')).toHaveAttribute('aria-current', 'step');
    expect(screen.getByTestId('project-step-4')).toHaveTextContent('waiting');

    expect(screen.getByTestId('project-build-environment')).toHaveTextContent(
      'captured by the terminal client',
    );
    expect(screen.getByTestId('project-build-environment')).toHaveTextContent('JAVA_HOME');
    expect(screen.getByTestId('project-chat-open')).not.toBeDisabled();
  });

  it('names the command that captures a build environment that was never captured', async () => {
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      ...RUN,
      build_environment: null,
    });

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() =>
      expect(screen.getByTestId('project-build-environment')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('project-build-environment')).toHaveTextContent(
      'No build environment captured yet: run pm-operator project add',
    );
  });

  it('opens the conflict card instead of overwriting a value changed in another client', async () => {
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      ...RUN,
      step: 4,
      state: 'project.operator',
      links: { apply: { href: '/api/v1/projects/pm-mcp-clients/onboarding/step' } },
    });
    const stepSpy = vi
      .spyOn(apiClient, 'stepProjectOnboarding')
      .mockResolvedValueOnce({
        ...RUN,
        conflict: true,
        current_value: 'landing.main',
        wanted_value: 'landing.files',
      })
      .mockResolvedValueOnce({ ...RUN, step: 5, state: 'setup.project-settings' });

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(screen.getByTestId('project-step-link-apply')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('project-step-link-apply'));

    await waitFor(() => expect(screen.getByTestId('project-conflict')).toBeInTheDocument());
    expect(screen.getByTestId('project-conflict')).toHaveTextContent('landing.main');
    expect(screen.getByTestId('project-conflict')).toHaveTextContent('landing.files');

    await fireEvent.click(screen.getByTestId('project-conflict-apply'));

    await waitFor(() =>
      expect(stepSpy).toHaveBeenLastCalledWith('pm-mcp-clients', 'apply', {
        value: 'landing.files',
      }),
    );
    await waitFor(() => expect(screen.queryByTestId('project-conflict')).not.toBeInTheDocument());
  });

  it('keeps the current value and closes the conflict card without writing', async () => {
    const loadSpy = vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      ...RUN,
      step: 4,
      state: 'project.operator',
      links: { apply: { href: '/api/v1/projects/pm-mcp-clients/onboarding/step' } },
    });
    const stepSpy = vi.spyOn(apiClient, 'stepProjectOnboarding').mockResolvedValue({
      ...RUN,
      conflict: true,
      current_value: 'landing.main',
      wanted_value: 'landing.files',
    });

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(screen.getByTestId('project-step-link-apply')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('project-step-link-apply'));
    await waitFor(() => expect(screen.getByTestId('project-conflict')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('project-conflict-keep'));

    await waitFor(() => expect(screen.queryByTestId('project-conflict')).not.toBeInTheDocument());
    expect(stepSpy).toHaveBeenCalledTimes(1);
    expect(loadSpy).toHaveBeenCalledTimes(2);
  });

  it('disables the setup chat with its catalogue reason when no role is configured', async () => {
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      ...RUN,
      chat_available: false,
      chat_disabled_reason: 'project.chat.disabled.no_role',
    });

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(screen.getByTestId('project-chat-open')).toBeDisabled());
    expect(screen.getByTestId('project-chat-disabled')).toHaveTextContent(
      'No planning role is configured for these questions.',
    );
  });

  it('shows the setup chat turns and marks an untrusted one', async () => {
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      ...RUN,
      chat: [
        {
          role: 'steward',
          text: 'The build wrapper reads its version from the pinned properties.',
          at: '2026-10-18T08:01:00Z',
          untrusted: true,
        },
      ],
    });

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(screen.getByTestId('project-chat-open')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('project-chat-open'));

    expect(screen.getByTestId('project-chat-drawer')).toBeInTheDocument();
    expect(screen.getByTestId('project-chat-drawer')).toHaveTextContent(
      'The build wrapper reads its version from the pinned properties.',
    );
    expect(screen.getByTestId('project-chat-drawer')).toHaveTextContent('untrusted');
  });

  it('shows the step a pre-enrolment run waits at, from the explorer report', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue(BEING_ADDED);

    render(ProjectOnboardingView, { onboardingId: '~/git/cui-parent' });

    await waitFor(() => expect(screen.getByTestId('project-onboarding-step')).toBeInTheDocument());
    expect(screen.getByTestId('project-step-2')).toHaveAttribute('aria-current', 'step');
    expect(screen.getByText('~/git/cui-parent')).toBeInTheDocument();
    expect(screen.getByTestId('project-continue')).toBeInTheDocument();
  });

  it('abandons a pre-enrolment run after the operator confirms', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue(BEING_ADDED);
    const abandonSpy = vi.spyOn(apiClient, 'abandonOnboarding').mockResolvedValue();

    render(ProjectOnboardingView, { onboardingId: '~/git/cui-parent' });

    await waitFor(() => expect(screen.getByTestId('project-abandon')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('project-abandon'));

    expect(abandonSpy).toHaveBeenCalledWith('/api/v1/entries/ent-cui/abandon');
  });

  it('starts a new run at the checkout path and shows the step it reaches', async () => {
    const explorerSpy = vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      ...EMPTY_EXPLORER,
      being_added: [
        {
          ...BEING_ADDED.being_added[0]!,
          checkout_path: '~/git/new-service',
        },
      ],
    });
    const startSpy = vi.spyOn(apiClient, 'startProjectOnboarding').mockResolvedValue({ ok: true });

    render(ProjectOnboardingView, {});

    await waitFor(() => expect(screen.getByTestId('project-checkout-step')).toBeInTheDocument());
    await fireEvent.input(screen.getByTestId('project-checkout-path'), {
      target: { value: '~/git/new-service' },
    });
    await fireEvent.click(screen.getByTestId('project-checkout-start'));

    expect(startSpy).toHaveBeenCalledWith('~/git/new-service');
    await waitFor(() => expect(screen.getByTestId('project-onboarding-step')).toBeInTheDocument());
    expect(explorerSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByText('~/git/new-service')).toBeInTheDocument();
  });

  it('reports a run it could not read and offers to load it again', async () => {
    vi.spyOn(apiClient, 'getProjectOnboarding').mockRejectedValue(new Error('HTTP 404'));

    render(ProjectOnboardingView, { projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(screen.getByTestId('project-onboarding-retry')).toBeInTheDocument());
    expect(screen.getByText('The project setup could not be loaded')).toBeInTheDocument();
  });
});
