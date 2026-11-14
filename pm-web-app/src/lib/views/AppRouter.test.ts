import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import { tick } from 'svelte';
import AppRouter from './AppRouter.svelte';
import { runtimeStore, type RuntimeState } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

/**
 * Every view the router dispatches to is replaced by a stand-in that names it and shows the props
 * it received, so the tests read the dispatcher's decision and not the views' own loads.
 */
const { stubFor } = vi.hoisted(() => ({
  stubFor: async (view: string) => {
    const { default: RouteStub } = await import('../../test/fixtures/RouteStub.svelte');
    const mount = RouteStub as unknown as (anchor: unknown, props: Record<string, unknown>) => void;
    return {
      default: (anchor: unknown, props: Record<string, unknown>) => mount(anchor, { ...props, view }),
    };
  },
}));

vi.mock('./PairingView.svelte', () => stubFor('PairingView'));
vi.mock('./OnboardingView.svelte', () => stubFor('OnboardingView'));
vi.mock('./SettingsView.svelte', () => stubFor('SettingsView'));
vi.mock('./MachineSettingsView.svelte', () => stubFor('MachineSettingsView'));
vi.mock('./ProcessDesigner.svelte', () => stubFor('ProcessDesigner'));
vi.mock('./RevokedView.svelte', () => stubFor('RevokedView'));
vi.mock('./ListenerOffView.svelte', () => stubFor('ListenerOffView'));
vi.mock('./OverviewView.svelte', () => stubFor('OverviewView'));
vi.mock('./ReportsView.svelte', () => stubFor('ReportsView'));
vi.mock('./DecisionView.svelte', () => stubFor('DecisionView'));
vi.mock('./SystemView.svelte', () => stubFor('SystemView'));
vi.mock('./CleanupView.svelte', () => stubFor('CleanupView'));
vi.mock('./ProjectExplorerView.svelte', () => stubFor('ProjectExplorerView'));
vi.mock('./PlanView.svelte', () => stubFor('PlanView'));
vi.mock('./EpicView.svelte', () => stubFor('EpicView'));
vi.mock('./EpicDecompositionView.svelte', () => stubFor('EpicDecompositionView'));
vi.mock('./StartView.svelte', () => stubFor('StartView'));

/** @returns the view the router shows */
function shownView(): string | null {
  return screen.getByTestId('route-view').getAttribute('data-view');
}

/** @returns the props the shown view received */
function shownProps(): Record<string, unknown> {
  return JSON.parse(screen.getByTestId('route-view').textContent ?? '{}') as Record<
    string,
    unknown
  >;
}

describe('AppRouter', () => {
  beforeEach(() => {
    runtimeStore.set(makeRuntimeState());
  });

  it.each<[RuntimeState['currentRoute'], string]>([
    ['projects', 'ProjectExplorerView'],
    ['reports', 'ReportsView'],
    ['decisions', 'DecisionView'],
    ['settings', 'SettingsView'],
    ['machine-settings', 'MachineSettingsView'],
    ['system', 'SystemView'],
    ['cleanup', 'CleanupView'],
    ['plan', 'PlanView'],
    ['epic', 'EpicView'],
    ['decomposition', 'EpicDecompositionView'],
    ['overview', 'OverviewView'],
    ['onboarding', 'OnboardingView'],
    ['project-onboarding', 'OnboardingView'],
  ])('shows the route %s of a paired, onboarded machine as %s', (route, view) => {
    runtimeStore.set(makeRuntimeState({ currentRoute: route }));

    render(AppRouter);

    expect(shownView()).toBe(view);
  });

  it.each<[string, Partial<RuntimeState>, string]>([
    ['a revoked browser, whatever else holds', { currentRoute: 'revoked', isPaired: false }, 'RevokedView'],
    ['a closed network listener', { currentRoute: 'listener-off', isListenerClosed: true }, 'ListenerOffView'],
    ['the listener page once the listener is open again', { currentRoute: 'listener-off' }, 'OverviewView'],
    ['an unpaired browser on any route', { currentRoute: 'plan', isPaired: false }, 'PairingView'],
    ['a machine not onboarded, on any route', { currentRoute: 'plan', isOnboarded: false }, 'OnboardingView'],
    [
      'a machine not onboarded that does not answer, on the route it named',
      { currentRoute: 'plan', isOnboarded: false, isOffline: true },
      'PlanView',
    ],
  ])('shows %s', (_case, state, view) => {
    runtimeStore.set(makeRuntimeState(state));

    render(AppRouter);

    expect(shownView()).toBe(view);
  });

  it('opens the start of an unpaired browser too, with its project, entry and kind', () => {
    runtimeStore.set(
      makeRuntimeState({
        currentRoute: 'start',
        isPaired: false,
        startProjectId: 'pm-mcp-clients',
        startEntryId: 'entry-1',
        startKind: 'epic',
      }),
    );

    render(AppRouter);

    expect(shownView()).toBe('StartView');
    expect(shownProps()).toEqual({ projectId: 'pm-mcp-clients', entryId: 'entry-1', kind: 'epic' });
  });

  it('opens a new start when the project of the start changes', async () => {
    runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startProjectId: 'a' }));
    render(AppRouter);
    expect(shownProps()).toEqual({ projectId: 'a', entryId: null, kind: 'plan' });

    runtimeStore.update((s) => ({ ...s, startProjectId: 'b' }));
    await tick();

    expect(shownProps()).toEqual({ projectId: 'b', entryId: null, kind: 'plan' });
  });

  it('opens the designer for the project the address named, or for the machine', () => {
    runtimeStore.set(makeRuntimeState({ currentRoute: 'designer', designerProjectId: 'core' }));
    const { unmount } = render(AppRouter);
    expect(shownView()).toBe('ProcessDesigner');
    expect(shownProps()).toEqual({ projectId: 'core' });
    unmount();

    runtimeStore.set(makeRuntimeState({ currentRoute: 'designer' }));
    render(AppRouter);
    expect(shownProps()).toEqual({ projectId: null });
  });
});
