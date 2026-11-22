import { describe, it, expect, vi } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
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

    const dialog = screen.getByRole('dialog', { name: 'relay-hardening · findings review' });

    expect(dialog).toHaveTextContent('2 of 3 decided');
    expect(screen.getByTestId('review-progress')).toHaveTextContent('2 of 3 decided');
    expect(screen.getByTestId('review-open-count')).toHaveTextContent('1 open');
    expect(screen.getByTestId('review-item-F-0004')).toHaveTextContent(
      'Reconnect loses the last event',
    );
    expect(screen.getByTestId('review-item-F-0005')).toHaveTextContent('kept');
  });

  it('holds the focus inside the dialog and walks its acts with Tab', async () => {
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', register: 'findings', review, open: true },
    });
    const dialog = screen.getByRole('dialog', { name: 'relay-hardening · findings review' });

    expect(dialog.contains(document.activeElement)).toBe(true);

    await fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });

    expect(dialog.contains(document.activeElement)).toBe(true);
  });

  it('draws every item and every act through the design vocabulary', () => {
    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', register: 'findings', review, open: true },
    });

    // The rows are Buttons, and the disposition beside the item is the state label of the design.
    for (const id of ['F-0004', 'F-0005']) {
      const row = screen.getByTestId(`review-item-${id}`);

      expect(row.className).toContain('btn');
      expect(row.querySelector('.status-label')).not.toBeNull();
      expect(within(row).getByText(/open|kept/)).toBeInTheDocument();
    }
    // What still waits for the operator carries the attention tone.
    expect(
      screen.getByTestId('review-item-F-0004').querySelector('.status-label--attention'),
    ).not.toBeNull();
    // In a list the state takes the fixed slot, so the dots stand one under the other.
    expect(screen.getByTestId('review-item-F-0004').querySelector('.status-label--fixed')).not.toBeNull();
    expect(document.querySelectorAll('.btn--secondary').length).toBeGreaterThanOrEqual(5);
    // One primary act: Finish.
    const primaries = document.querySelectorAll('.btn--primary');
    expect(primaries).toHaveLength(1);
    expect(primaries[0]).toBe(screen.getByTestId('review-commit'));
  });

  it('opens on its first open item again after a parent closed it', async () => {
    // The parent closes the modal (as the epic's commit does) and the item it had selected is
    // decided by the time it reopens: the next open must be the one selected, not the old one.
    const both: ReviewResource = {
      ...review,
      items: [
        {
          item_id: 'F-0004',
          register: 'findings',
          statement: 'Reconnect loses the last event',
          rules: [],
          origin: 'landing of relay-reconnect (#211)',
          trust: 'trusted',
          state: 'open',
        },
        {
          item_id: 'F-0005',
          register: 'findings',
          statement: 'The retry count is not bounded',
          rules: [],
          origin: 'review comment of #212',
          trust: 'trusted',
          state: 'open',
        },
      ],
    };
    const decided: ReviewResource = {
      ...both,
      items: [{ ...both.items[0], state: 'kept' }, both.items[1]],
    };

    const { rerender } = render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review: both, open: true },
    });
    expect(screen.getByTestId('review-item-F-0004')).toHaveAttribute('aria-current', 'true');

    await rerender({ epicId: 'relay-hardening', review: decided, open: false });
    await rerender({ epicId: 'relay-hardening', review: decided, open: true });

    expect(screen.getByTestId('review-item-F-0005')).toHaveAttribute('aria-current', 'true');
    expect(screen.getByTestId('review-item-F-0004')).not.toHaveAttribute('aria-current');
  });

  it('opens on its first open item, never on a decided one', () => {
    // The first item is decided: the panel opens on F-0005, the first one still open.
    const decidedFirst: ReviewResource = {
      ...review,
      items: [
        {
          item_id: 'F-0003',
          register: 'findings',
          statement: 'The retry count is not bounded',
          rules: [],
          origin: 'review comment of #212',
          trust: 'trusted',
          state: 'kept',
        },
        {
          item_id: 'F-0004',
          register: 'findings',
          statement: 'Reconnect loses the last event',
          rules: [],
          origin: 'landing of relay-reconnect (#211)',
          trust: 'trusted',
          state: 'open',
        },
      ],
    };

    render(FindingsReviewModal, {
      props: { epicId: 'relay-hardening', review: decidedFirst, open: true },
    });

    expect(screen.getByTestId('review-statement')).toHaveTextContent(
      'Reconnect loses the last event',
    );
    expect(screen.getByTestId('review-item-F-0004')).toHaveAttribute('aria-current', 'true');
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

  it('draws the selected row of the register in the brand colours', () => {
    const content = fs.readFileSync(path.resolve(__dirname, './FindingsReviewModal.svelte'), 'utf-8');

    expect(content).toMatch(
      /\.review-modal__row :global\(\.btn\[aria-current='true'\]\)\s*\{[^}]*border-color:\s*var\(--color-brand\)/,
    );
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
