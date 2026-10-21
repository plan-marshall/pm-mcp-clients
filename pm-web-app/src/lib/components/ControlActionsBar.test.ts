import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import ControlActionsBar from './ControlActionsBar.svelte';
import type { ArchiveCheck, WorkActions } from '../api/client';

describe('ControlActionsBar component', () => {
  const running: WorkActions = {
    subject_id: 'fix-relay',
    subject_kind: 'plan',
    state: 'running',
    epic_id: 'relay-hardening',
    actions: [
      { action: 'pause', available: true },
      { action: 'cancel', available: true },
      { action: 'chat', available: true },
      { action: 'settings', available: true },
    ],
  };

  const archiveCheck: ArchiveCheck = {
    epic_id: 'relay-hardening',
    lines: [
      { label: 'control.archive.no_plan_active', passed: true },
      {
        label: 'control.archive.open_findings',
        passed: false,
        note: '2',
        action: 'review-findings',
      },
    ],
    carried_findings: 3,
    carried_watches: 5,
    completion_review_first: false,
    passed: false,
  };

  it('renders one button per action of the state', () => {
    render(ControlActionsBar, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: running },
    });

    expect(screen.getByTestId('control-actions')).toBeInTheDocument();
    expect(screen.getByTestId('action-pause')).toHaveTextContent('Pause');
    expect(screen.getByTestId('action-cancel')).toHaveTextContent('Cancel');
    expect(screen.getByTestId('action-settings')).toHaveTextContent('Settings');
    expect(screen.queryByTestId('action-start')).not.toBeInTheDocument();
    expect(screen.queryByTestId('action-abandon')).not.toBeInTheDocument();
  });

  it('offers the actions of a paused plan', () => {
    const paused: WorkActions = {
      ...running,
      state: 'paused',
      actions: [
        { action: 'start', available: true },
        { action: 'abandon', available: true },
        { action: 'chat', available: true },
        { action: 'settings', available: true },
      ],
    };
    render(ControlActionsBar, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: paused },
    });

    expect(screen.getByTestId('action-start')).toBeInTheDocument();
    expect(screen.getByTestId('action-abandon')).toBeInTheDocument();
    expect(screen.queryByTestId('action-pause')).not.toBeInTheDocument();
  });

  it('shows a disabled action with its reason', () => {
    const withDisabled: WorkActions = {
      ...running,
      actions: [
        { action: 'pause', available: true },
        { action: 'chat', available: false, reason: 'no configuration of replan qualifies yet' },
      ],
    };
    render(ControlActionsBar, {
      props: { subjectId: 'relay-metrics', subjectKind: 'plan', actions: withDisabled },
    });

    expect(screen.getByTestId('action-chat')).toBeDisabled();
    expect(screen.getByTestId('reason-chat')).toHaveTextContent(
      'Chat is not available: no configuration of replan qualifies yet',
    );
  });

  it('asks where a plan holds before it sends the pause', async () => {
    const onAction = vi.fn();
    render(ControlActionsBar, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: running, onAction },
    });

    await fireEvent.click(screen.getByTestId('action-pause'));
    expect(onAction).not.toHaveBeenCalled();
    expect(screen.getByTestId('pause-title')).toHaveTextContent('Pause fix-relay?');

    await fireEvent.click(screen.getByLabelText('After the current phase'));
    await fireEvent.click(screen.getByTestId('pause-confirm'));

    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onAction).toHaveBeenCalledWith('pause', { point: 'phase' });
  });

  it('confirms an end with a required reason and the pull request choice', async () => {
    const onAction = vi.fn();
    render(ControlActionsBar, {
      props: {
        subjectId: 'fix-relay',
        subjectKind: 'plan',
        actions: running,
        pullRequestNumber: 214,
        onAction,
      },
    });

    await fireEvent.click(screen.getByTestId('action-cancel'));
    expect(screen.getByTestId('end-title')).toHaveTextContent('Cancel fix-relay?');
    expect(screen.getByTestId('end-confirm')).toBeDisabled();

    await fireEvent.input(screen.getByTestId('end-reason'), {
      target: { value: 'the approach is replaced by plan relay-v2' },
    });
    await fireEvent.click(screen.getByLabelText('Close the pull request with the reason'));
    await fireEvent.click(screen.getByTestId('end-confirm'));

    expect(onAction).toHaveBeenCalledWith('cancel', {
      reason: 'the approach is replaced by plan relay-v2',
      close_pr: true,
    });
  });

  it('confirms an abandon of a paused plan', async () => {
    const onAction = vi.fn();
    const paused: WorkActions = {
      ...running,
      state: 'paused',
      actions: [{ action: 'abandon', available: true }],
    };
    render(ControlActionsBar, {
      props: { subjectId: 'relay-metrics', subjectKind: 'plan', actions: paused, onAction },
    });

    await fireEvent.click(screen.getByTestId('action-abandon'));
    expect(screen.getByTestId('end-title')).toHaveTextContent('Abandon relay-metrics?');
    await fireEvent.input(screen.getByTestId('end-reason'), { target: { value: 'obsolete' } });
    await fireEvent.click(screen.getByTestId('end-confirm'));

    expect(onAction).toHaveBeenCalledWith('abandon', { reason: 'obsolete', close_pr: false });
  });

  it('names the abort of an integrity pause as the end act', async () => {
    const onAction = vi.fn();
    const held: WorkActions = {
      ...running,
      state: 'held',
      hold_kind: 'integrity_pause',
      actions: [{ action: 'cancel', available: true }],
    };
    render(ControlActionsBar, {
      props: { subjectId: 'relay-cursor-order', subjectKind: 'plan', actions: held, onAction },
    });

    expect(screen.getByTestId('action-cancel')).toHaveTextContent('Abort the plan');
    await fireEvent.click(screen.getByTestId('action-cancel'));
    expect(screen.getByTestId('end-title')).toHaveTextContent('Abort relay-cursor-order?');
    await fireEvent.input(screen.getByTestId('end-reason'), { target: { value: 'replaced' } });
    await fireEvent.click(screen.getByTestId('end-confirm'));

    expect(onAction).toHaveBeenCalledWith('abort', { reason: 'replaced', close_pr: false });
  });

  it('toggles a named value of an epic rather than flipping it', async () => {
    const onAction = vi.fn();
    const epic: WorkActions = {
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      auto_enabled: true,
      overlap_max_files: 0,
      actions: [
        { action: 'next', available: true },
        { action: 'auto', available: true },
      ],
    };
    render(ControlActionsBar, {
      props: { subjectId: 'relay-hardening', subjectKind: 'epic', actions: epic, onAction },
    });

    expect(screen.getByTestId('action-auto')).toHaveTextContent('Automatic: on');
    await fireEvent.click(screen.getByTestId('action-auto'));

    expect(onAction).toHaveBeenCalledWith('auto', { enabled: false });
  });

  it('shows the archive check and keeps archiving disabled until every line passes', async () => {
    const onAction = vi.fn();
    const epic: WorkActions = {
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'archive', available: true }],
    };
    render(ControlActionsBar, {
      props: {
        subjectId: 'relay-hardening',
        subjectKind: 'epic',
        actions: epic,
        archiveCheck,
        onAction,
      },
    });

    await fireEvent.click(screen.getByTestId('action-archive'));
    expect(screen.getByTestId('archive-title')).toHaveTextContent('Archive relay-hardening?');
    expect(screen.getByTestId('archive-line-control.archive.open_findings')).toBeInTheDocument();
    expect(screen.getByTestId('archive-carried')).toHaveTextContent('3 kept findings');
    expect(screen.getByTestId('archive-confirm')).toBeDisabled();

    await fireEvent.click(screen.getByTestId('archive-review-findings'));
    expect(onAction).toHaveBeenCalledWith('findings');
  });

  it('offers to raise the scope when the epic runs its full scope', async () => {
    const onAction = vi.fn();
    const epic: WorkActions = {
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    };
    render(ControlActionsBar, {
      props: {
        subjectId: 'relay-hardening',
        subjectKind: 'epic',
        actions: epic,
        scope: 3,
        running: 3,
        nextPlan: 'relay-token-rotation',
        onAction,
      },
    });

    await fireEvent.click(screen.getByTestId('action-next'));
    expect(screen.getByTestId('raise-confirm')).toHaveTextContent(
      'Raise to 4 at once and start it',
    );
    await fireEvent.click(screen.getByTestId('raise-confirm'));

    expect(onAction).toHaveBeenCalledWith('next', { raise_scope: true });
  });

  it('renders no buttons while the actions are still loading', () => {
    render(ControlActionsBar, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: null },
    });
    expect(screen.getByTestId('control-actions-empty')).toBeInTheDocument();
  });
});
