import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import EpicDecompositionView from './EpicDecompositionView.svelte';
import { apiClient, type EpicDecomposition } from '../api/client';
import { ClientError } from '../api/errors';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

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
    runtimeStore.set(
      makeRuntimeState({
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
        isListenerClosed: false,
        completedSteps: 0,
        isDraining: false,
        isStopped: false,
        activeStep: 1,
        unreadReportsCount: 0,
        openDecisionsCount: 0,
        selectedPlanId: null,
        selectedEpicId: 'relay-hardening',
        selectedProjectId: 'pm-mcp-clients',
        selectedTaskId: null,
      }),
    );
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('draws the error pattern with its act when the cut cannot be read', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockRejectedValue(new ClientError('network_offline'));

    render(EpicDecompositionView);

    const error = await screen.findByTestId('decomposition-error');
    expect(error).toHaveTextContent('The machine is not reachable');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('draws skeletons while the cut is being read', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockReturnValue(new Promise(() => {}));

    render(EpicDecompositionView);

    const loading = await screen.findByTestId('skeleton');
    expect(loading).toHaveAttribute('data-shape', 'card');
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
    const check = vi
      .spyOn(apiClient, 'checkDecomposition')
      .mockResolvedValue({ ...rewritten, status: 'review', findings: [] });

    render(EpicDecompositionView);
    await screen.findByTestId('plan-edit-relay-auth-v2');

    await fireEvent.click(screen.getByTestId('plan-edit-relay-auth-v2'));
    const modal = screen.getByTestId('instruct-modal');
    expect(modal).toBeInTheDocument();

    const input = within(modal).getByRole('textbox');
    await fireEvent.input(input, {
      target: { value: 'the exchange belongs into pm-relay' },
    });
    await fireEvent.keyDown(input, { key: 'Enter' });

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
    // The accepted edit re-runs the checks, so a resolved finding opens the commit gate.
    expect(check).toHaveBeenCalledWith('pm-mcp-clients', 'relay-hardening');
    expect(await screen.findByTestId('agree-commit')).not.toBeDisabled();
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

  it('disables agree while a check fails with disabledReason', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());

    render(EpicDecompositionView);

    const agree = await screen.findByTestId('agree-commit');
    expect(agree).toHaveAttribute('aria-disabled', 'true');
    const describedBy = agree.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy!)).toHaveTextContent('1 check fails');
  });

  it('shows the check states by word with StatusLabel and icons', async () => {
    const cutWithStates = cut({
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
            {
              plan_id: 'relay-reconnect-docs',
              title: 'relay-reconnect-docs',
              expected_surface: ['doc/relay'],
              dependencies: [],
              status: 'changed',
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
              expected_surface: ['pm-api/src/cursor'],
              dependencies: [],
              status: 'staged',
            },
            {
              plan_id: 'relay-token-rotation',
              title: 'relay-token-rotation',
              expected_surface: ['pm-api/src/tokens'],
              dependencies: ['relay-auth-v2'],
              status: 'staged',
            },
          ],
        },
      ],
      findings: [
        {
          severity: 'error',
          rule: 'surface_overlap',
          affected_plans: ['relay-auth-v2'],
          message: 'surface overlaps relay-cursor-order (pm-api)',
        },
      ],
    });
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cutWithStates);

    render(EpicDecompositionView);
    await screen.findByTestId('plan-relay-cursor-order');

    // Checked plan: success tone, "checked" word, check icon
    const checkedPlan = screen.getByTestId('plan-relay-cursor-order');
    expect(checkedPlan.querySelector('.status-label--success')).toBeInTheDocument();
    expect(checkedPlan).toHaveTextContent('checked');
    expect(checkedPlan.querySelector('[data-icon="state.check"]')).toBeInTheDocument();

    // Check failed plan: danger tone, "check failed" word with reason, error icon
    const failedPlan = screen.getByTestId('plan-relay-auth-v2');
    expect(failedPlan.querySelector('.status-label--danger')).toBeInTheDocument();
    expect(failedPlan).toHaveTextContent('check failed: surface overlaps relay-cursor-order (pm-api)');
    expect(failedPlan.querySelector('[data-icon="state.error"]')).toBeInTheDocument();

    // Waiting plan: neutral tone, "waiting for another plan" word, clock icon
    const waitingPlan = screen.getByTestId('plan-relay-token-rotation');
    expect(waitingPlan.querySelector('.status-label--neutral')).toBeInTheDocument();
    expect(waitingPlan).toHaveTextContent('waiting for another plan');
    expect(waitingPlan.querySelector('[data-icon="state.clock"]')).toBeInTheDocument();

    // Edited plan: attention tone, "edited" word, edit icon
    const editedPlan = screen.getByTestId('plan-relay-reconnect-docs');
    expect(editedPlan.querySelector('.status-label--attention')).toBeInTheDocument();
    expect(editedPlan).toHaveTextContent('edited');
    expect(editedPlan.querySelector('[data-icon="act.edit"]')).toBeInTheDocument();
  });

  it('uses Dialog for the instruct modal which closes on Escape', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());

    render(EpicDecompositionView);
    await screen.findByTestId('plan-edit-relay-auth-v2');

    await fireEvent.click(screen.getByTestId('plan-edit-relay-auth-v2'));
    const modal = screen.getByTestId('instruct-modal');
    expect(modal).toHaveAttribute('role', 'dialog');
    expect(modal).toHaveAttribute('aria-modal', 'true');

    // Escape closes Dialog
    await fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByTestId('instruct-modal')).not.toBeInTheDocument();
  });

  it('marks a plan dropped through the drop action', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cut());

    render(EpicDecompositionView);
    await screen.findByTestId('plan-drop-relay-auth-v2');

    await fireEvent.click(screen.getByTestId('plan-drop-relay-auth-v2'));

    expect(screen.getByTestId('plan-relay-auth-v2')).toHaveTextContent('dropped');
  });

  it('renders check failure for dependency_cycle errors affecting a plan', async () => {
    vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(
      cut({
        findings: [
          {
            rule: 'dependency_cycle',
            severity: 'error',
            message: 'plans relay-cursor-order and relay-auth-v2 form a cycle',
            affected_plans: ['relay-cursor-order', 'relay-auth-v2'],
          },
        ],
      }),
    );

    render(EpicDecompositionView);
    await screen.findByTestId('plan-relay-cursor-order');

    const card = screen.getByTestId('plan-relay-cursor-order');
    expect(card.querySelector('.status-label--danger')).toBeInTheDocument();
    expect(card).toHaveTextContent(
      'check failed: plans relay-cursor-order and relay-auth-v2 form a cycle',
    );
    expect(card.querySelector('[data-icon="state.error"]')).toBeInTheDocument();
  });
});
