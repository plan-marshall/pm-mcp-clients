import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import FindingsReviewModal from './FindingsReviewModal.svelte';
import type { ReviewResource } from '../api/client';

describe('FindingsReviewModal component', () => {
  const review: ReviewResource = {
    review_id: 'R-0001',
    epic_id: 'relay-hardening',
    register: 'findings',
    decided: 2,
    total: 3,
    state: 'item',
    items: [
      {
        item_id: 'F-0004',
        register: 'findings',
        statement: 'Reconnect loses the last event',
        rules: ['a fix must hold for every transport'],
        origin: 'landing of relay-reconnect (#211)',
        trust: 'trusted',
        state: 'open',
        proposal: 'Promote to a plan: it needs a change across two modules',
      },
      {
        item_id: 'F-0005',
        register: 'findings',
        statement: 'The retry count is not bounded',
        rules: [],
        origin: 'review comment of #212',
        trust: 'untrusted',
        state: 'kept',
      },
    ],
  };

  it('shows the register, its progress, and every item', () => {
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', register: 'findings', review, open: true },
    });

    expect(screen.getByTestId('review-title')).toHaveTextContent(
      'relay-hardening · findings review',
    );
    expect(screen.getByTestId('review-progress')).toHaveTextContent('2 of 3 decided');
    expect(screen.getByTestId('review-item-F-0004')).toHaveTextContent(
      'Reconnect loses the last event',
    );
    expect(screen.getByTestId('review-item-F-0005')).toHaveTextContent('kept');
  });

  it('shows the item, its rules, origin, and proposal', async () => {
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true },
    });

    await fireEvent.click(screen.getByTestId('review-item-F-0004'));

    expect(screen.getByTestId('review-statement')).toHaveTextContent(
      'Reconnect loses the last event',
    );
    expect(screen.getByTestId('review-rules')).toHaveTextContent(
      'a fix must hold for every transport',
    );
    expect(screen.getByTestId('review-origin')).toHaveTextContent(
      'landing of relay-reconnect (#211)',
    );
    expect(screen.getByTestId('review-proposal')).toHaveTextContent('Promote to a plan');
  });

  it('marks an item from outside the machine and shows its text only', async () => {
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true },
    });

    await fireEvent.click(screen.getByTestId('review-item-F-0005'));

    expect(screen.getByTestId('review-untrusted')).toHaveTextContent('untrusted');
    expect(screen.getByTestId('review-rules')).toHaveTextContent('no rules named');
  });

  it('keeps an item as a finding or moves it to the watches', async () => {
    const onKeep = vi.fn();
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true, onKeep },
    });

    await fireEvent.click(screen.getByTestId('review-item-F-0004'));
    await fireEvent.click(screen.getByTestId('review-keep'));
    await fireEvent.click(screen.getByTestId('review-move'));

    expect(onKeep).toHaveBeenNthCalledWith(1, 'F-0004', 'finding');
    expect(onKeep).toHaveBeenNthCalledWith(2, 'F-0004', 'watch');
  });

  it('drops an item only once the reason is given', async () => {
    const onDrop = vi.fn();
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true, onDrop },
    });

    await fireEvent.click(screen.getByTestId('review-item-F-0004'));
    expect(screen.getByTestId('review-drop')).toBeDisabled();
    await fireEvent.input(screen.getByTestId('review-drop-reason'), {
      target: { value: 'covered by another finding' },
    });
    await fireEvent.click(screen.getByTestId('review-drop'));

    expect(onDrop).toHaveBeenCalledWith('F-0004', 'covered by another finding');
  });

  it('asks the planning role to promote or edit an item', async () => {
    const onPromote = vi.fn();
    const onEdit = vi.fn();
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true, onPromote, onEdit },
    });

    await fireEvent.click(screen.getByTestId('review-item-F-0004'));
    await fireEvent.click(screen.getByTestId('review-promote'));
    expect(onPromote).toHaveBeenCalledWith('F-0004', undefined);

    await fireEvent.click(screen.getByTestId('review-edit'));
    await fireEvent.input(screen.getByTestId('review-instruction'), {
      target: { value: 'name the transport' },
    });
    await fireEvent.click(screen.getByTestId('review-instruct'));
    expect(onEdit).toHaveBeenCalledWith('F-0004', 'name the transport');
  });

  it('reads back an edit old against new', () => {
    render(FindingsReviewModal, {
      props: {
        epicId: 'relay-hardening',
        review: {
          ...review,
          read_back: {
            kind: 'statement',
            before: 'Reconnect loses the last event',
            after: 'Reconnect loses the last event on SSE',
          },
        },
        open: true,
      },
    });

    fireEvent.click(screen.getByTestId('review-item-F-0004'));

    expect(screen.getByTestId('review-read-back')).toHaveTextContent(
      'Reconnect loses the last event on SSE',
    );
    expect(screen.getByTestId('review-read-back-accept')).toBeInTheDocument();
    expect(screen.getByTestId('review-read-back-discard')).toBeInTheDocument();
  });

  it('stages a drafted plan after a promotion', async () => {
    const onStagePlan = vi.fn();
    render(FindingsReviewModal, {
      props: {
        epicId: 'relay-hardening',
        review: {
          ...review,
          drafted_plan: {
            plan_id: 'relay-cursor-fix',
            objective: 'fix the cursor order',
            deliverables: ['the cursor order'],
            surface: ['pm-relay/src'],
            dependencies: [],
          },
        },
        open: true,
        onStagePlan,
      },
    });

    await fireEvent.click(screen.getByTestId('review-item-F-0004'));
    await fireEvent.click(screen.getByTestId('review-promote'));
    expect(await screen.findByTestId('review-drafted-plan')).toHaveTextContent('relay-cursor-fix');
    await fireEvent.click(screen.getByTestId('review-plan-stage'));

    expect(onStagePlan).toHaveBeenCalled();
  });

  it('confirms which remaining proposals are accepted and commits the review', async () => {
    const onAcceptRemaining = vi.fn();
    const onCommit = vi.fn();
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true, onAcceptRemaining, onCommit },
    });

    await fireEvent.click(screen.getByTestId('review-accept-remaining'));
    expect(screen.getByTestId('review-remaining-confirm')).toHaveTextContent(
      'Take the proposal for this item?',
    );
    await fireEvent.click(screen.getByTestId('review-remaining-yes'));
    expect(onAcceptRemaining).toHaveBeenCalled();

    await fireEvent.click(screen.getByTestId('review-commit'));
    expect(onCommit).toHaveBeenCalled();
  });

  it('renders nothing while it is closed', () => {
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: false },
    });
    expect(screen.queryByTestId('review-modal')).not.toBeInTheDocument();
  });

  it('closes on Escape', async () => {
    const onClose = vi.fn();
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review, open: true, onClose },
    });

    await fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
