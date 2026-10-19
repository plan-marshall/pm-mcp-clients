import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
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
  it('shows the workstreams, the rows and the failing check', () => {
    render(DecompositionReviewView, { props: { decomposition: cut() } });

    expect(screen.getByTestId('decomposition-title')).toHaveTextContent(
      'relay-hardening · decomposition',
    );
    expect(screen.getByTestId('workstream-WS-1')).toHaveTextContent('Reconnect');
    expect(screen.getByTestId('plan-surface-relay-auth-v2')).toHaveTextContent('pm-api/src/cursor');
    expect(screen.getByTestId('decomposition-checks')).toHaveTextContent('1 check fails');
    expect(screen.getByTestId('plan-conflict-relay-auth-v2')).toHaveTextContent(
      'surface overlaps relay-auth-v2 (pm-api/src/cursor)',
    );
    expect(screen.getByTestId('agree-commit')).toBeDisabled();
  });

  it('passes an instruction on and reads the change back', async () => {
    const instruct = vi.fn();
    const { rerender } = render(DecompositionReviewView, {
      props: { decomposition: cut(), onInstruct: instruct },
    });

    await fireEvent.click(screen.getByTestId('plan-edit-relay-auth-v2'));
    await fireEvent.input(screen.getByTestId('instruct-input'), {
      target: { value: 'the exchange belongs into pm-relay' },
    });
    await fireEvent.click(screen.getByTestId('instruct-submit'));

    expect(instruct).toHaveBeenCalledWith('relay-auth-v2', 'the exchange belongs into pm-relay');

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
    expect(screen.getByTestId('read-back-after')).toHaveTextContent('pm-relay/src/relay-auth-v2');
  });

  it('agrees once every check passes', async () => {
    const agree = vi.fn();
    render(DecompositionReviewView, {
      props: { decomposition: cut({ findings: [] }), onAgree: agree },
    });

    expect(screen.getByTestId('decomposition-checks')).toHaveTextContent('every check passes');
    await fireEvent.click(screen.getByTestId('agree-commit'));
    expect(agree).toHaveBeenCalled();
  });

  it('drops a plan through its drop action', async () => {
    const drop = vi.fn();
    render(DecompositionReviewView, { props: { decomposition: cut(), onDrop: drop } });

    await fireEvent.click(screen.getByTestId('plan-drop-relay-auth-v2'));
    expect(drop).toHaveBeenCalledWith('relay-auth-v2');
  });
});
