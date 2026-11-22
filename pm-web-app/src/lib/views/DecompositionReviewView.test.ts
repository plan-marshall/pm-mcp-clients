import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import DecompositionReviewView from './DecompositionReviewView.svelte';
import type { EpicDecomposition } from '../api/client';

function cut(overrides: Partial<EpicDecomposition> = {}): EpicDecomposition {
  return {
    epic_id: 'relay-hardening',
    status: 'check',
    revision: 3,
    workstreams: [
      {
        id: 'WS-1',
        name: 'Reconnect',
        order: 1,
        staged_plans: [
          {
            plan_id: 'relay-cursor-order',
            title: 'relay-cursor-order',
            expected_surface: ['pm-relay/src/cursor'],
            dependencies: [],
            status: 'staged',
          },
        ],
      },
      {
        id: 'WS-2',
        name: 'Authentication',
        order: 2,
        staged_plans: [
          {
            plan_id: 'relay-auth-v2',
            title: 'relay-auth-v2',
            objective: 'a token exchange',
            expected_surface: ['pm-api/src/cursor'],
            dependencies: ['relay-cursor-order'],
            status: 'staged',
          },
        ],
      },
    ],
    findings: [
      {
        severity: 'error',
        rule: 'surface_overlap',
        affected_plans: ['relay-cursor-order', 'relay-auth-v2'],
        message: 'pm-api/src/cursor',
      },
    ],
    ...overrides,
  };
}

describe('the decomposition review', () => {
  it('shows the workstreams, rows, failing check, and plan details in KeyValueList (a plan with no deliverables)', () => {
    render(DecompositionReviewView, { props: { decomposition: cut() } });

    expect(screen.getByTestId('decomposition-title')).toHaveTextContent(
      'relay-hardening · decomposition',
    );
    expect(screen.getByTestId('workstream-WS-1')).toHaveTextContent('Reconnect');
    expect(screen.getByTestId('plan-surface-relay-auth-v2')).toHaveTextContent('pm-api/src/cursor');
    expect(screen.getByTestId('decomposition-checks')).toHaveTextContent('1 check fails');
    expect(screen.getByTestId('plan-conflict-relay-auth-v2')).toHaveTextContent(
      'surface overlaps relay-cursor-order (pm-api/src/cursor)',
    );
    expect(screen.getByTestId('agree-commit')).toHaveAttribute('aria-disabled', 'true');

    // KeyValueList for relay-auth-v2:
    const planAuth = screen.getByTestId('plan-relay-auth-v2');
    const kv = within(planAuth).getByTestId('key-value-list');
    expect(kv).toHaveTextContent('Objective');
    expect(kv).toHaveTextContent('a token exchange');
    expect(kv).toHaveTextContent('Deliverables');
    expect(kv).toHaveTextContent('—'); // No deliverables shows —
    expect(kv).toHaveTextContent('Surface');
    expect(kv).toHaveTextContent('pm-api/src/cursor');
    expect(kv).toHaveTextContent('Depends on');
    expect(kv).toHaveTextContent('relay-cursor-order');
    expect(kv).toHaveTextContent('Order');
    expect(kv).toHaveTextContent('2');
  });

  it('shows deliverables in KeyValueList when present', () => {
    const cutWithDeliverables = cut({
      workstreams: [
        {
          id: 'WS-2',
          name: 'Authentication',
          order: 2,
          staged_plans: [
            {
              plan_id: 'relay-auth-v2',
              title: 'relay-auth-v2',
              objective: 'a token exchange',
              deliverables: ['the token exchange', 'token rotation'],
              expected_surface: ['pm-api/src/cursor'],
              dependencies: ['relay-cursor-order'],
              order: 3,
              status: 'staged',
            },
          ],
        },
      ],
      findings: [],
    });

    render(DecompositionReviewView, { props: { decomposition: cutWithDeliverables } });

    const planAuth = screen.getByTestId('plan-relay-auth-v2');
    const kv = within(planAuth).getByTestId('key-value-list');
    expect(kv).toHaveTextContent('the token exchange, token rotation');
    expect(kv).toHaveTextContent('3');
  });

  it('renders the three actions of a plan as secondary buttons', async () => {
    const onKeep = vi.fn();
    const onDrop = vi.fn();
    render(DecompositionReviewView, { props: { decomposition: cut(), onKeep, onDrop } });

    const keep = screen.getByTestId('plan-keep-relay-auth-v2');
    expect(keep).toHaveClass('btn--secondary');
    expect(keep).not.toHaveClass('btn--ghost');
    await fireEvent.click(keep);
    expect(onKeep).toHaveBeenCalledWith('relay-auth-v2');

    const edit = screen.getByTestId('plan-edit-relay-auth-v2');
    expect(edit).toHaveClass('btn--secondary');
    expect(edit).not.toHaveClass('btn--ghost');

    const drop = screen.getByTestId('plan-drop-relay-auth-v2');
    expect(drop).toHaveClass('btn--secondary');
    expect(drop).not.toHaveClass('btn--ghost');
    await fireEvent.click(drop);
    expect(onDrop).toHaveBeenCalledWith('relay-auth-v2');
  });

  it('passes an instruction on through Compose and reads the change back with signs − and + and hierarchy', async () => {
    const instruct = vi.fn();
    const onAccept = vi.fn();
    const onInstructAgain = vi.fn();
    const onDiscard = vi.fn();

    const { rerender } = render(DecompositionReviewView, {
      props: {
        decomposition: cut(),
        onInstruct: instruct,
        onAccept,
        onInstructAgain,
        onDiscard,
      },
    });

    await fireEvent.click(screen.getByTestId('plan-edit-relay-auth-v2'));

    // Instruct field through Compose
    const instructPanel = screen.getByTestId('decomposition-instruction');
    const textbox = within(instructPanel).getByRole('textbox', { name: 'What should change?' });
    await fireEvent.input(textbox, {
      target: { value: 'the exchange belongs into pm-relay' },
    });
    await fireEvent.keyDown(textbox, { key: 'Enter' });

    expect(instruct).toHaveBeenCalledWith('relay-auth-v2', 'the exchange belongs into pm-relay');

    // Rerender with readBack
    await rerender({
      decomposition: cut({ findings: [] }),
      readBack: {
        planId: 'relay-auth-v2',
        before: cut().workstreams[1].staged_plans[0],
        after: {
          ...cut().workstreams[1].staged_plans[0],
          objective: 'the exchange belongs into pm-relay',
          expected_surface: ['pm-relay/src/relay-auth-v2'],
        },
      },
    });

    expect(screen.getByTestId('decomposition-read-back')).toHaveTextContent(
      'relay-auth-v2: old against new',
    );

    // Old and new shown with signs − and +
    const before = screen.getByTestId('read-back-before');
    const after = screen.getByTestId('read-back-after');
    expect(before.textContent?.trim()).toMatch(/^−/);
    expect(after.textContent?.trim()).toMatch(/^\+/);
    expect(after).toHaveTextContent('pm-relay/src/relay-auth-v2');

    // Three acts of read-back: Accept primary, Instruct again secondary, Discard ghost
    const acceptBtn = screen.getByTestId('read-back-accept');
    expect(acceptBtn).toHaveClass('btn--primary');
    await fireEvent.click(acceptBtn);
    expect(onAccept).toHaveBeenCalled();

    const instructAgainBtn = screen.getByTestId('read-back-instruct');
    expect(instructAgainBtn).toHaveClass('btn--secondary');
    await fireEvent.click(instructAgainBtn);
    expect(onInstructAgain).toHaveBeenCalled();

    const discardBtn = screen.getByTestId('read-back-discard');
    expect(discardBtn).toHaveClass('btn--ghost');
    await fireEvent.click(discardBtn);
    expect(onDiscard).toHaveBeenCalled();
  });

  it('combines edited objective and instruction when both are present', async () => {
    const instruct = vi.fn();
    render(DecompositionReviewView, {
      props: {
        decomposition: cut(),
        onInstruct: instruct,
      },
    });

    await fireEvent.click(screen.getByTestId('plan-edit-relay-auth-v2'));

    const objectiveInput = screen.getByLabelText('Objective');
    await fireEvent.input(objectiveInput, {
      target: { value: 'token rotation objective' },
    });

    const instructPanel = screen.getByTestId('decomposition-instruction');
    const textbox = within(instructPanel).getByRole('textbox', { name: 'What should change?' });
    await fireEvent.input(textbox, {
      target: { value: 'keep the API compatible' },
    });

    await fireEvent.click(screen.getByTestId('instruct-submit'));

    expect(instruct).toHaveBeenCalledWith(
      'relay-auth-v2',
      'token rotation objective\nkeep the API compatible',
    );
  });

  it('disables agree while checks fail with disabledReason and agrees once passing', async () => {
    const agree = vi.fn();
    const { rerender } = render(DecompositionReviewView, {
      props: { decomposition: cut(), onAgree: agree },
    });

    const agreeBtn = screen.getByTestId('agree-commit');
    expect(agreeBtn).toHaveAttribute('aria-disabled', 'true');
    const describedBy = agreeBtn.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy!)).toHaveTextContent('1 check fails');

    // Passing cut
    await rerender({
      decomposition: cut({ findings: [] }),
      onAgree: agree,
    });

    expect(screen.getByTestId('decomposition-checks')).toHaveTextContent('every check passes');
    expect(agreeBtn).toHaveAttribute('class', expect.stringContaining('btn--primary'));
    expect(agreeBtn).not.toHaveAttribute('aria-disabled', 'true');

    await fireEvent.click(agreeBtn);
    expect(agree).toHaveBeenCalled();
  });
});
