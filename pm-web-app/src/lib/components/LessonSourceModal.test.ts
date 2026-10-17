import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import LessonSourceModal from './LessonSourceModal.svelte';
import {
  apiClient,
  type LessonClaimResult,
  type LessonPremiseCheckResult,
  type PlanStartResult,
  type UnclaimedLessonSummary,
} from '../api/client';

describe('LessonSourceModal component', () => {
  const lessons: UnclaimedLessonSummary[] = [
    {
      lesson_id: 'les-0007',
      title: 'Retire lessons when a plan lands',
      summary: 'A landed plan retires the lessons it claimed.',
      recorded_at: '2026-10-14T15:36:00Z',
      citing_plan_id: 'fix-relay',
      symbols_cited: ['src/A.java', 'de.Old#m()'],
    },
  ];

  const holds: LessonPremiseCheckResult = {
    lesson_id: 'les-0007',
    status: 'holds',
    evidence: [],
  };

  const broken: LessonPremiseCheckResult = {
    lesson_id: 'les-0009',
    status: 'broken',
    evidence: [
      {
        kind: 'file',
        name: 'src/Old.java',
        expected_path: 'src/Old.java',
        last_verified_commit: 'a1b2c3d',
      },
    ],
  };

  const claimed: LessonClaimResult = {
    lesson_id: 'les-0007',
    status: 'claimed',
    plan_id: 'lesson-les-0007',
  };

  const started: PlanStartResult = {
    id: 'lesson-plan',
    kind: 'plan',
    title: 'Retire lessons when a plan lands',
    state: 'started',
    follow_href: '/api/v1/plans/lesson-plan',
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'getUnclaimedLessons').mockResolvedValue(lessons);
    vi.spyOn(apiClient, 'checkLessonPremise').mockResolvedValue(holds);
    vi.spyOn(apiClient, 'claimLesson').mockResolvedValue(claimed);
    vi.spyOn(apiClient, 'startPlan').mockResolvedValue(started);
    vi.spyOn(apiClient, 'retireLesson').mockResolvedValue({
      lesson_id: 'les-0007',
      retired: true,
    });
  });

  it('renders nothing while it is closed', () => {
    render(LessonSourceModal, { props: { open: false, projectId: 'pm-mcp-clients' } });
    expect(screen.queryByTestId('lesson-modal')).not.toBeInTheDocument();
  });

  it('lists the unclaimed lessons for the project', async () => {
    render(LessonSourceModal, { props: { open: true, projectId: 'pm-mcp-clients' } });

    expect(screen.getByTestId('lesson-title')).toHaveTextContent(
      'Start from a lesson in pm-mcp-clients',
    );
    await waitFor(() =>
      expect(apiClient.getUnclaimedLessons).toHaveBeenCalledWith('pm-mcp-clients'),
    );
    expect(await screen.findByTestId('lesson-item-les-0007')).toHaveTextContent(
      'Retire lessons when a plan lands',
    );
  });

  it('claims a holding lesson and starts the plan', async () => {
    const onStarted = vi.fn();
    render(LessonSourceModal, {
      props: { open: true, projectId: 'pm-mcp-clients', onStarted },
    });

    await screen.findByTestId('lesson-item-les-0007');
    await fireEvent.click(screen.getByTestId('lesson-select-les-0007'));

    await waitFor(() =>
      expect(apiClient.claimLesson).toHaveBeenCalledWith('pm-mcp-clients', 'les-0007', false),
    );
    await waitFor(() => expect(onStarted).toHaveBeenCalledWith(started));
    expect(screen.getByTestId('lesson-done')).toHaveTextContent(
      'les-0007 is claimed by lesson-plan',
    );
  });

  it('shows the missing evidence of a broken premise', async () => {
    vi.spyOn(apiClient, 'checkLessonPremise').mockResolvedValue(broken);
    render(LessonSourceModal, { props: { open: true, projectId: 'pm-mcp-clients' } });

    await screen.findByTestId('lesson-item-les-0007');
    await fireEvent.click(screen.getByTestId('lesson-select-les-0007'));

    expect(await screen.findByTestId('premise-evidence')).toBeInTheDocument();
    expect(screen.getByTestId('premise-evidence-0')).toHaveTextContent('src/Old.java');
    expect(apiClient.claimLesson).not.toHaveBeenCalled();
  });

  it('claims with use-anyway when the operator overrides a broken premise', async () => {
    vi.spyOn(apiClient, 'checkLessonPremise').mockResolvedValue(broken);
    render(LessonSourceModal, { props: { open: true, projectId: 'pm-mcp-clients' } });

    await screen.findByTestId('lesson-item-les-0007');
    await fireEvent.click(screen.getByTestId('lesson-select-les-0007'));
    await screen.findByTestId('premise-use-anyway');
    await fireEvent.click(screen.getByTestId('premise-use-anyway'));

    await waitFor(() =>
      expect(apiClient.claimLesson).toHaveBeenCalledWith('pm-mcp-clients', 'les-0007', true),
    );
    await waitFor(() => expect(apiClient.startPlan).toHaveBeenCalled());
  });

  it('retires the lesson when the operator chooses retirement', async () => {
    vi.spyOn(apiClient, 'checkLessonPremise').mockResolvedValue(broken);
    render(LessonSourceModal, { props: { open: true, projectId: 'pm-mcp-clients' } });

    await screen.findByTestId('lesson-item-les-0007');
    await fireEvent.click(screen.getByTestId('lesson-select-les-0007'));
    await screen.findByTestId('premise-retire');
    await fireEvent.click(screen.getByTestId('premise-retire'));

    await waitFor(() =>
      expect(apiClient.retireLesson).toHaveBeenCalledWith('pm-mcp-clients', 'les-0007'),
    );
    expect(await screen.findByTestId('lesson-unchanged')).toHaveTextContent('stays unclaimed');
    expect(apiClient.startPlan).not.toHaveBeenCalled();
  });

  it('reports a failure and stays open', async () => {
    vi.spyOn(apiClient, 'getUnclaimedLessons').mockRejectedValue(new Error('lesson_not_found'));
    render(LessonSourceModal, { props: { open: true, projectId: 'pm-mcp-clients' } });

    expect(await screen.findByTestId('lesson-error')).toHaveTextContent('lesson_not_found');
  });
});
