import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import StartWorkModal from './StartWorkModal.svelte';
import { apiClient, type PlanStartResult, type RecipeRef } from '../api/client';

describe('StartWorkModal component', () => {
  const planResult: PlanStartResult = {
    id: 'codeberg-provider-issues',
    kind: 'plan',
    title: 'Codeberg provider for issues',
    state: 'started',
    follow_href: '/api/v1/plans/codeberg-provider-issues',
  };

  const recipes: RecipeRef[] = [
    {
      key: 'audit',
      name: 'audit',
      description: 'audit against a request',
      origin: 'project',
      takes_request: true,
      parameters: [],
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'getRecipes').mockResolvedValue(recipes);
    vi.spyOn(apiClient, 'startPlan').mockResolvedValue(planResult);
    vi.spyOn(apiClient, 'startEpic').mockResolvedValue({ ...planResult, kind: 'epic' });
  });

  it('renders nothing while it is closed', () => {
    render(StartWorkModal, { props: { open: false, projectId: 'pm-mcp-clients' } });
    expect(screen.queryByTestId('start-modal')).not.toBeInTheDocument();
  });

  it('titles the run for the kind and the project', () => {
    render(StartWorkModal, { props: { open: true, projectId: 'pm-mcp-clients', kind: 'plan' } });
    expect(screen.getByTestId('start-title')).toHaveTextContent('Start a plan in pm-mcp-clients');
  });

  it('starts a plan from the sources and reports the created plan', async () => {
    const onStarted = vi.fn();
    render(StartWorkModal, {
      props: { open: true, projectId: 'pm-mcp-clients', kind: 'plan', onStarted },
    });

    await fireEvent.input(screen.getByTestId('source-description'), {
      target: { value: 'Add a Codeberg provider for issues' },
    });
    await fireEvent.input(screen.getByTestId('source-documents'), {
      target: { value: 'doc/concepts/forgejo.adoc' },
    });
    await fireEvent.input(screen.getByTestId('source-issue'), { target: { value: '4711' } });
    await fireEvent.click(screen.getByTestId('start-submit'));

    await waitFor(() => expect(onStarted).toHaveBeenCalledWith(planResult));
    expect(apiClient.startPlan).toHaveBeenCalledWith(
      expect.objectContaining({
        project_id: 'pm-mcp-clients',
        objective: 'Add a Codeberg provider for issues',
        documents: ['doc/concepts/forgejo.adoc'],
        issue: '4711',
      }),
    );
    expect(screen.getByTestId('start-done')).toHaveTextContent(
      'codeberg-provider-issues has started',
    );
  });

  it('vets a recipe before the start is offered', async () => {
    render(StartWorkModal, { props: { open: true, projectId: 'pm-mcp-clients', kind: 'plan' } });

    const submit = screen.getByTestId('start-submit') as HTMLButtonElement;
    expect(submit.disabled).toBe(false);

    await fireEvent.click(screen.getByTestId('start-mode-recipe'));
    await waitFor(() => expect(screen.getByTestId('recipe-select')).toBeInTheDocument());
    expect(submit.disabled).toBe(true);

    await fireEvent.change(screen.getByTestId('recipe-select'), { target: { value: 'audit' } });
    expect(submit.disabled).toBe(false);

    await fireEvent.click(submit);
    await waitFor(() =>
      expect(apiClient.startPlan).toHaveBeenCalledWith(
        expect.objectContaining({ project_id: 'pm-mcp-clients', recipe_id: 'audit' }),
      ),
    );
  });

  it('starts an epic and reports it', async () => {
    const onStarted = vi.fn();
    render(StartWorkModal, {
      props: { open: true, projectId: 'pm-mcp-clients', kind: 'epic', onStarted },
    });

    expect(screen.getByTestId('start-title')).toHaveTextContent('Start an epic in pm-mcp-clients');
    await fireEvent.input(screen.getByTestId('source-description'), {
      target: { value: 'Relay hardening' },
    });
    await fireEvent.click(screen.getByTestId('start-submit'));

    await waitFor(() => expect(apiClient.startEpic).toHaveBeenCalled());
    expect(onStarted.mock.calls[0][0].kind).toBe('epic');
  });

  it('reports a failure and stays open', async () => {
    vi.spyOn(apiClient, 'startPlan').mockRejectedValue(new Error('start_refused'));
    render(StartWorkModal, { props: { open: true, projectId: 'plan-marshall', kind: 'plan' } });

    await fireEvent.click(screen.getByTestId('start-submit'));
    await waitFor(() =>
      expect(screen.getByTestId('start-error')).toHaveTextContent('start_refused'),
    );
  });
});
