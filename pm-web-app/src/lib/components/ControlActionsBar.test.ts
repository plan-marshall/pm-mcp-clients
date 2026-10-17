import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import ControlActionsBar from './ControlActionsBar.svelte';
import type { WorkActions } from '../api/client';

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

  it('renders one button per action of the state', () => {
    render(ControlActionsBar, { props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: running } });

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
    render(ControlActionsBar, { props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: paused } });

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
    render(ControlActionsBar, { props: { subjectId: 'relay-metrics', subjectKind: 'plan', actions: withDisabled } });

    expect(screen.getByTestId('action-chat')).toBeDisabled();
    expect(screen.getByTestId('reason-chat')).toHaveTextContent(
      'Chat is not available: no configuration of replan qualifies yet',
    );
  });

  it('calls onAction when an action is chosen', async () => {
    const onAction = vi.fn();
    render(ControlActionsBar, {
      props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: running, onAction },
    });

    await fireEvent.click(screen.getByTestId('action-pause'));
    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onAction).toHaveBeenCalledWith('pause');
  });

  it('renders no buttons while the actions are still loading', () => {
    render(ControlActionsBar, { props: { subjectId: 'fix-relay', subjectKind: 'plan', actions: null } });
    expect(screen.getByTestId('control-actions-empty')).toBeInTheDocument();
  });
});
