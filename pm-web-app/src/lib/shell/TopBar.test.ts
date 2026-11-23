import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import TopBar from './TopBar.svelte';
import * as runtime from '../stores/runtime';
import { runtimeStore } from '../stores/runtime';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import type { ProcessTree } from '../api/client';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('TopBar component (PLAN-144 D2)', () => {
  beforeEach(() => {
    useCatalogue('en');
    runtimeStore.set({
      isInitialized: true,
      isPaired: true,
      isOnboarded: true,
      deviceLabel: 'test-device',
      machineName: 'test-machine',
      currentRoute: 'overview',
      currentState: null,
      isRevoked: false,
      revokedReason: '',
      isOffline: false,
      streamState: 'connected',
      isListenerClosed: false,
      isDraining: false,
      isStopped: false,
      activeStep: 1,
      completedSteps: 0,
      unreadReportsCount: 0,
      openDecisionsCount: 0,
      currentProcessTree: null,
      currentEpicResource: null,
    });
  });

  const mockPlan: ProcessTree = {
    plan_id: 'fix-relay',
    title: 'Fix Relay Transport',
    project: {
      project_id: 'pm-mcp-clients',
      display_name: 'Relay Transport',
    },
    epic: {
      epic_id: 'relay-hardening',
      title: 'Relay Hardening',
    },
    current_phase: 'execute',
    phase_state: 'running',
    needs_operator: false,
    instance_version: 1,
    tokens_so_far: { total_tokens: 100, cost_usd: 0.01, duration_ms: 1000 },
    nodes: [],
    current_position_path: [],
  };

  it('draws one breadcrumb: lens › project › epic › plan with KindMarks and titles', () => {
    runtimeStore.update((s) => ({
      ...s,
      currentRoute: 'plan',
      selectedPlanId: 'fix-relay',
      currentProcessTree: mockPlan,
    }));

    render(TopBar);

    expect(screen.getByText('Running work')).toBeInTheDocument();
    expect(screen.getByText('Relay Transport')).toBeInTheDocument();
    expect(screen.getByText('Relay Hardening')).toBeInTheDocument();
    expect(screen.getByText('Fix Relay Transport')).toBeInTheDocument();

    const marks = Array.from(document.querySelectorAll('.app-header__path .kind-mark'));
    expect(marks).toHaveLength(4);
    expect(marks[0]).toHaveAttribute('data-kind', 'plan');
    expect(marks[1]).toHaveAttribute('data-kind', 'project');
    expect(marks[2]).toHaveAttribute('data-kind', 'epic');
    expect(marks[3]).toHaveAttribute('data-kind', 'plan');
  });

  it('pushes the unfilled lens address when the lens segment is clicked', async () => {
    const openLensListSpy = vi.spyOn(runtime, 'openLensList').mockImplementation(() => {});

    runtimeStore.update((s) => ({
      ...s,
      currentRoute: 'plan',
      selectedPlanId: 'fix-relay',
      currentProcessTree: mockPlan,
    }));

    render(TopBar);

    const lensLink = screen.getByRole('link', { name: /Running work/i });
    await fireEvent.click(lensLink);

    expect(openLensListSpy).toHaveBeenCalled();
  });

  it('pushes the parent address when an ancestor segment is clicked', async () => {
    const openAddressSpy = vi.spyOn(runtime, 'openAddress').mockImplementation(() => {});

    runtimeStore.update((s) => ({
      ...s,
      currentRoute: 'plan',
      selectedPlanId: 'fix-relay',
      currentProcessTree: mockPlan,
    }));

    render(TopBar);

    const projectLink = screen.getByRole('link', { name: /Relay Transport/i });
    await fireEvent.click(projectLink);

    expect(openAddressSpy).toHaveBeenCalledWith({
      route: 'overview',
      project: 'pm-mcp-clients',
    });
  });

  it('draws group and title outside the lenses (e.g. Machine › System › Cleanup)', () => {
    runtimeStore.update((s) => ({
      ...s,
      currentRoute: 'cleanup',
    }));

    render(TopBar);

    expect(screen.getByText('Machine')).toBeInTheDocument();
    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.getByText('Cleanup')).toBeInTheDocument();
  });
});
