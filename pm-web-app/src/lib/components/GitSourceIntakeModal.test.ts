import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import GitSourceIntakeModal from './GitSourceIntakeModal.svelte';
import {
  apiClient,
  type LocalChangesPreview,
  type PullRequestObservation,
  type PrAdoptResult,
} from '../api/client';

describe('GitSourceIntakeModal component', () => {
  const observation: PullRequestObservation = {
    url: 'https://codeberg.org/plan-marshall/pm-mcp-clients/pulls/4711',
    number: 4711,
    title: 'Add a Codeberg provider for issues',
    author: 'octocat',
    branch: 'codeberg-provider',
    state: 'open',
    writable: true,
    changed_files: [
      { path: 'doc/concepts/forgejo.adoc', additions: 12, deletions: 3, untrusted: true, operator_authored: false },
    ],
    screening: 'clean',
    rate_limit: { limit: 0, remaining: 0, limited: false },
  };

  const adoptResult: PrAdoptResult = {
    plan_id: 'adopt-pr-4711',
    lane: 'adopt',
    branch: 'codeberg-provider',
    status: 'adopted',
  };

  const preview: LocalChangesPreview = {
    status: 'previewed',
    preview_id: 'preview-1',
    branch: 'main',
    changed_files: [
      {
        path: 'pm-operator/src/main/java/de/planmarshall/operator/GitSourceCommand.java',
        additions: 88,
        deletions: 0,
        untrusted: false,
        operator_authored: true,
      },
    ],
    landing: 'pr',
    landing_commits: true,
    eligible: true,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'observePullRequest').mockResolvedValue(observation);
    vi.spyOn(apiClient, 'adoptPullRequest').mockResolvedValue(adoptResult);
    vi.spyOn(apiClient, 'previewLocalChanges').mockResolvedValue(preview);
    vi.spyOn(apiClient, 'captureLocalChanges').mockResolvedValue({
      plan_id: 'capture-local-changes',
      lane: 'land',
      snapshot_commit: 'a1b2c3d4e5f6',
      status: 'captured',
    });
  });

  it('renders nothing while it is closed', () => {
    render(GitSourceIntakeModal, { props: { open: false, projectId: 'pm-mcp-clients' } });
    expect(screen.queryByTestId('git-source-modal')).not.toBeInTheDocument();
  });

  it('titles the modal for the project', () => {
    render(GitSourceIntakeModal, { props: { open: true, projectId: 'pm-mcp-clients' } });
    expect(screen.getByTestId('git-source-title')).toHaveTextContent(
      'Git sources in pm-mcp-clients',
    );
  });

  it('observes and adopts a clean pull request', async () => {
    const onCompleted = vi.fn();
    render(GitSourceIntakeModal, {
      props: { open: true, projectId: 'pm-mcp-clients', onCompleted },
    });

    await fireEvent.input(screen.getByTestId('pr-reference-input'), { target: { value: '4711' } });
    await fireEvent.click(screen.getByTestId('pr-observe'));

    await waitFor(() => expect(screen.getByTestId('pr-observation')).toBeInTheDocument());
    expect(screen.getByTestId('pr-files')).toHaveTextContent('1 changed file');

    await fireEvent.click(screen.getByTestId('pr-adopt-submit'));
    await waitFor(() => expect(onCompleted).toHaveBeenCalledWith('adopt-pr-4711'));
    expect(screen.getByTestId('git-source-done')).toHaveTextContent(
      'adopt-pr-4711 adopted the pull request',
    );
  });

  it('requires an explicit acceptance before a flagged pull request can be adopted', async () => {
    vi.spyOn(apiClient, 'observePullRequest').mockResolvedValue({
      ...observation,
      screening: 'flagged',
    });
    render(GitSourceIntakeModal, { props: { open: true, projectId: 'pm-mcp-clients' } });

    await fireEvent.input(screen.getByTestId('pr-reference-input'), { target: { value: '6677' } });
    await fireEvent.click(screen.getByTestId('pr-observe'));

    await waitFor(() => expect(screen.getByTestId('pr-flagged-title')).toBeInTheDocument());
    const adopt = screen.getByTestId('pr-adopt-submit') as HTMLButtonElement;
    expect(adopt.disabled).toBe(true);

    await fireEvent.click(screen.getByTestId('pr-accept-screening'));
    expect(adopt.disabled).toBe(false);
  });

  it('previews and captures the local changes', async () => {
    const onCompleted = vi.fn();
    render(GitSourceIntakeModal, {
      props: { open: true, projectId: 'pm-mcp-clients', onCompleted },
    });

    await fireEvent.click(screen.getByTestId('git-source-tab-local'));
    await waitFor(() => expect(screen.getByTestId('local-preview')).toBeInTheDocument());
    expect(screen.getByTestId('local-files')).toHaveTextContent('GitSourceCommand.java');

    const capture = screen.getByTestId('local-capture') as HTMLButtonElement;
    expect(capture.disabled).toBe(true);

    await fireEvent.click(screen.getByTestId('local-confirm'));
    expect(capture.disabled).toBe(false);

    await fireEvent.click(capture);
    await waitFor(() => expect(onCompleted).toHaveBeenCalledWith('capture-local-changes'));
    expect(apiClient.captureLocalChanges).toHaveBeenCalledWith('pm-mcp-clients', 'preview-1');
    expect(screen.getByTestId('git-source-snapshot')).toHaveTextContent('a1b2c3d4e5f6');
  });

  it('refuses local changes where the landing does not commit', async () => {
    vi.spyOn(apiClient, 'previewLocalChanges').mockResolvedValue({
      ...preview,
      landing: 'landing.files',
      landing_commits: false,
      eligible: false,
      reason: 'landing.files',
    });
    render(GitSourceIntakeModal, { props: { open: true, projectId: 'pm-core' } });

    await fireEvent.click(screen.getByTestId('git-source-tab-local'));
    await waitFor(() => expect(screen.getByTestId('local-ineligible')).toBeInTheDocument());
    expect(screen.getByTestId('local-pm-wf-25')).toHaveTextContent('PM-WF-25');
    expect((screen.getByTestId('local-capture') as HTMLButtonElement).disabled).toBe(true);
  });
});
