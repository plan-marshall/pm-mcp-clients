import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import HoldResolutionModal from './HoldResolutionModal.svelte';
import type { HoldRecord } from '../api/client';

describe('HoldResolutionModal component', () => {
  const conditionHold: HoldRecord = {
    hold_id: 'H-0001',
    subject_id: 'fix-relay',
    subject_kind: 'plan',
    kind: 'condition',
    cause: 'the plan reads cui-http, which this project may not read',
    attempts: 3,
    last_tried_at: '14:10',
    acts: ['retry', 'grant', 'end_plan'],
  };

  const integrityHold: HoldRecord = {
    hold_id: 'H-0002',
    subject_id: 'relay-cursor-order',
    subject_kind: 'plan',
    kind: 'integrity_pause',
    cause: "the plan's store file references.json changed outside the runtime",
    attempts: 0,
    file_path: 'references.json',
    recorded_size: 4812,
    recorded_digest: '3f9a6b2cc21e',
    current_size: 5034,
    current_digest: 'b70d41aa9a44',
    acts: ['retry', 'accept', 'abort'],
  };

  it('renders the hold, its cause, and its acts', () => {
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true } });

    expect(screen.getByTestId('hold-title')).toHaveTextContent('fix-relay holds: a condition');
    expect(screen.getByTestId('hold-cause')).toHaveTextContent(
      'the plan reads cui-http, which this project may not read',
    );
    expect(screen.getByTestId('hold-tried')).toHaveTextContent('Tried 3 times, last 14:10');
    expect(screen.getByTestId('hold-act-retry')).toHaveTextContent('Retry now');
    expect(screen.getByTestId('hold-act-grant')).toHaveTextContent('Grant read access to');
  });

  it('shows the tampered file of an integrity pause with recorded and current facts', () => {
    render(HoldResolutionModal, { props: { hold: integrityHold, open: true } });

    expect(screen.getByTestId('hold-title')).toHaveTextContent('holds: an integrity pause');
    expect(screen.getByTestId('hold-file')).toHaveTextContent('references.json');
    expect(screen.getByTestId('hold-recorded')).toHaveTextContent('4812');
    expect(screen.getByTestId('hold-current')).toHaveTextContent('5034');
  });

  it('calls onResolve for a reversible act at once', async () => {
    const onResolve = vi.fn();
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true, onResolve } });

    await fireEvent.click(screen.getByTestId('hold-act-retry'));
    expect(onResolve).toHaveBeenCalledTimes(1);
    expect(onResolve).toHaveBeenCalledWith({ act: 'retry' });
  });

  it('asks for a confirmation before an act that cannot be undone', async () => {
    const onResolve = vi.fn();
    render(HoldResolutionModal, { props: { hold: integrityHold, open: true, onResolve } });

    await fireEvent.click(screen.getByTestId('hold-act-accept'));
    expect(onResolve).not.toHaveBeenCalled();
    expect(screen.getByTestId('hold-confirm')).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('hold-confirm-button'));
    expect(onResolve).toHaveBeenCalledTimes(1);
    expect(onResolve.mock.calls[0][0].act).toBe('accept');
  });

  it('calls onClose', async () => {
    const onClose = vi.fn();
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true, onClose } });

    await fireEvent.click(screen.getByTestId('hold-close-button'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('renders nothing while it is closed', () => {
    render(HoldResolutionModal, { props: { hold: conditionHold, open: false } });
    expect(screen.queryByTestId('hold-modal')).not.toBeInTheDocument();
  });
});
