import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import EpicDecompositionView from './EpicDecompositionView.svelte';
import { apiClient, type EpicDecomposition } from '../api/client';
import { runtimeStore } from '../stores/runtime';

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
            expected_surface: ['pm-relay/src/cursor', 'pm-api/src/cursor'],
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
            dependencies: [],
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
        message: 'surface overlaps relay-auth-v2 (pm-api/src/cursor)',
      },
    ],
    ...overrides,
  };
}

describe('EpicDecompositionView', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    runtimeStore.set({
      isInitialized: true,
      isPaired: true,
      isOnboarded: true,
      deviceLabel: 'test',
      machineName: 'test-mac',
      currentRoute: 'decomposition',
      currentState: null,
      isRevoked: false,
      revokedReason: '',
      isOffline: false,
      isDraining: false,
      isStopped: false,
      activeStep: 1,
      unreadReportsCount: 0,
      openDecisionsCount: 0,
      selectedPlanId: null,
      selectedEpicId: 'relay-hardening',
      selectedProjectId: 'pm-mcp-clients',
      selectedTaskId: null,
    });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('renders the workstreams, plans, and the conflict banner', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());

    render(EpicDecompositionView);

    expect(await screen.findByTestId('decomposition-title')).toHaveTextContent(
      'relay-hardening · decomposition',
    );
    expect(screen.getByTestId('workstream-WS-1')).toHaveTextContent('Reconnect');
    expect(screen.getByTestId('plan-relay-auth-v2')).toHaveTextContent('relay-auth-v2');
    expect(screen.getByTestId('plan-surface-relay-auth-v2')).toHaveTextContent('pm-api/src/cursor');
    expect(screen.getByTestId('conflict-banner')).toHaveTextContent('1 check fails');
    expect(screen.getByTestId('plan-conflict-relay-auth-v2')).toBeInTheDocument();
  });

  it('shows the read-back when an instruction is submitted and accepts it', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());
    const rewritten = cut({
      status: 'read_back',
      findings: [],
      workstreams: [
        {
          id: 'WS-1',
          name: 'Reconnect',
          order: 1,
          staged_plans: [
            {
              plan_id: 'relay-cursor-order',
              title: 'relay-cursor-order',
              expected_surface: ['pm-relay/src/cursor', 'pm-api/src/cursor'],
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
              objective: 'the exchange belongs into pm-relay',
              expected_surface: ['pm-relay/src/relay-auth-v2'],
              dependencies: [],
              status: 'changed',
            },
          ],
        },
      ],
    });
    const instruct = vi.spyOn(apiClient, 'instructDecomposition').mockResolvedValue(rewritten);

    render(EpicDecompositionView);
    await screen.findByTestId('plan-edit-relay-auth-v2');

    await fireEvent.click(screen.getByTestId('plan-edit-relay-auth-v2'));
    expect(screen.getByTestId('instruct-modal')).toBeInTheDocument();

    await fireEvent.input(screen.getByTestId('instruct-input'), {
      target: { value: 'the exchange belongs into pm-relay' },
    });
    await fireEvent.click(screen.getByTestId('instruct-submit'));

    expect(await screen.findByTestId('read-back')).toBeInTheDocument();
    const rows = screen.getAllByTestId('read-back-row');
    expect(rows.length).toBeGreaterThan(0);
    expect(screen.getByTestId('read-back')).toHaveTextContent('Surface');
    expect(screen.getByTestId('read-back')).toHaveTextContent('pm-relay/src/relay-auth-v2');
    expect(instruct).toHaveBeenCalledWith(
      'pm-mcp-clients',
      'relay-hardening',
      expect.objectContaining({ plan_id: 'relay-auth-v2', action: 'edit' }),
    );

    await fireEvent.click(screen.getByTestId('read-back-accept'));
    expect(screen.getByTestId('plan-surface-relay-auth-v2')).toHaveTextContent(
      'pm-relay/src/relay-auth-v2',
    );
  });

  it('agrees and commits a passing cut', async () => {
    const passing = cut({ status: 'review', findings: [] });
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(passing);
    vi.spyOn(apiClient, 'checkDecomposition').mockResolvedValue(passing);
    const commit = vi
      .spyOn(apiClient, 'commitDecomposition')
      .mockResolvedValue({ status: 'committed', ledger_revision: 4, launchable_plan_count: 2 });

    render(EpicDecompositionView);
    const agree = await screen.findByTestId('agree-commit');
    expect(agree).not.toBeDisabled();

    await fireEvent.click(agree);

    expect(await screen.findByTestId('decomposition-committed')).toHaveTextContent(
      'relay-hardening: 2 workstreams, 2 plans committed',
    );
    expect(commit).toHaveBeenCalledWith('pm-mcp-clients', 'relay-hardening');
  });

  it('disables agree while a check fails', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());

    render(EpicDecompositionView);

    const agree = await screen.findByTestId('agree-commit');
    expect(agree).toBeDisabled();
  });

  it('marks a plan dropped through the drop action', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());

    render(EpicDecompositionView);
    await screen.findByTestId('plan-drop-relay-auth-v2');

    await fireEvent.click(screen.getByTestId('plan-drop-relay-auth-v2'));

    expect(screen.getByTestId('plan-relay-auth-v2')).toHaveTextContent('dropped');
  });
});
