import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import PrAdoptPanel from './PrAdoptPanel.svelte';
import { apiClient, type PullRequestObservation } from '../api/client';

const OBSERVATION: PullRequestObservation = {
  url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
  number: 211,
  title: 'Reconnect keeps the cursor',
  author: 'oliver',
  branch: 'fix/cursor',
  state: 'open',
  writable: true,
  changed_files: [
    {
      path: 'pm-web-app/src/lib/api/client.ts',
      additions: 3,
      deletions: 1,
      untrusted: false,
      operator_authored: true,
    },
  ],
  screening: 'clean',
  rate_limit: { limit: 5000, remaining: 42, limited: false },
};

describe('PrAdoptPanel', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('takes the reference on a TextField and observes a pull request number', async () => {
    const observe = vi.spyOn(apiClient, 'observePullRequest').mockResolvedValue(OBSERVATION);
    render(PrAdoptPanel, { props: { projectId: 'p-1' } });

    const field = screen.getByRole('textbox', { name: /pull request/i });
    await fireEvent.input(field, { target: { value: '211' } });
    await fireEvent.click(screen.getByTestId('pr-observe'));

    expect(observe).toHaveBeenCalledWith('p-1', '211');
  });

  it('observes a pull request URL as well, and shows the pull request as a PrChip', async () => {
    const observe = vi.spyOn(apiClient, 'observePullRequest').mockResolvedValue(OBSERVATION);
    render(PrAdoptPanel, { props: { projectId: 'p-1' } });

    await fireEvent.input(screen.getByRole('textbox', { name: /pull request/i }), {
      target: { value: OBSERVATION.url },
    });
    await fireEvent.click(screen.getByTestId('pr-observe'));

    expect(observe).toHaveBeenCalledWith('p-1', OBSERVATION.url);
    const chip = await screen.findByText('PR #211');
    expect(chip.closest('a')).toHaveAttribute('href', OBSERVATION.url);
  });

  it('keeps the acts disabled until a pull request is observed', async () => {
    render(PrAdoptPanel, { props: { projectId: 'p-1' } });

    expect(screen.getByTestId('pr-observe')).toBeDisabled();
    expect(screen.getByTestId('pr-adopt-submit')).toBeDisabled();
  });

  it('adopts the observed pull request and reports the result', async () => {
    vi.spyOn(apiClient, 'observePullRequest').mockResolvedValue(OBSERVATION);
    const adopt = vi.spyOn(apiClient, 'adoptPullRequest').mockResolvedValue({
      plan_id: 'relay-cursor-fix',
      lane: 'runbook',
      branch: 'fix/cursor',
      status: 'staged',
    });
    const onAdopted = vi.fn();
    render(PrAdoptPanel, { props: { projectId: 'p-1', onAdopted } });

    await fireEvent.input(screen.getByRole('textbox', { name: /pull request/i }), {
      target: { value: OBSERVATION.url },
    });
    await fireEvent.click(screen.getByTestId('pr-observe'));
    const adoptButton = await screen.findByTestId('pr-adopt-submit');
    await fireEvent.click(adoptButton);

    expect(adopt).toHaveBeenCalledWith({ project_id: 'p-1', pr_url: OBSERVATION.url });
    expect(onAdopted).toHaveBeenCalledWith(
      expect.objectContaining({ plan_id: 'relay-cursor-fix' }),
    );
  });

  it('asks for acceptance of a flagged pull request before adopting it', async () => {
    const flagged = { ...OBSERVATION, screening: 'flagged' as const };
    vi.spyOn(apiClient, 'observePullRequest').mockResolvedValue(flagged);
    render(PrAdoptPanel, { props: { projectId: 'p-1' } });

    await fireEvent.input(screen.getByRole('textbox', { name: /pull request/i }), {
      target: { value: OBSERVATION.url },
    });
    await fireEvent.click(screen.getByTestId('pr-observe'));

    const adoptButton = await screen.findByTestId('pr-adopt-submit');
    expect(adoptButton).toBeDisabled();

    await fireEvent.click(screen.getByRole('checkbox'));

    expect(adoptButton).not.toBeDisabled();
  });
});
