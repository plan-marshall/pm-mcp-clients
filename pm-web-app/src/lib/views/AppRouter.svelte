<script lang="ts">
  import { runtimeStore } from '../stores/runtime';
  import PairingView from './PairingView.svelte';
  import OnboardingView from './OnboardingView.svelte';
  import WorkspaceView from './WorkspaceView.svelte';
  import SettingsView from './SettingsView.svelte';
  import MachineSettingsView from './MachineSettingsView.svelte';
  import ProcessDesigner from './ProcessDesigner.svelte';
  import RevokedView from './RevokedView.svelte';
  import ListenerOffView from './ListenerOffView.svelte';
  import OverviewView from './OverviewView.svelte';
  import ReportsView from './ReportsView.svelte';
  import DecisionView from './DecisionView.svelte';
  import SystemView from './SystemView.svelte';
  import CleanupView from './CleanupView.svelte';
  import ProjectExplorerView from './ProjectExplorerView.svelte';
  import PlanView from './PlanView.svelte';
  import EpicView from './EpicView.svelte';
  import EpicDecompositionView from './EpicDecompositionView.svelte';

  /**
   * The one dispatcher of the app.
   *
   * Every route page renders it, so the store is the only thing that decides what is shown: a
   * reload, a bookmark, or a change of route renders the same view, and the address bar follows
   * the store rather than the other way round.
   */
</script>

{#if $runtimeStore.currentRoute === 'revoked'}
  <RevokedView />
{:else if $runtimeStore.isListenerClosed && $runtimeStore.currentRoute === 'listener-off'}
  <ListenerOffView />
{:else if !$runtimeStore.isPaired}
  <PairingView />
{:else if !$runtimeStore.isOnboarded && $runtimeStore.currentRoute !== 'onboarding'}
  <OnboardingView />
{:else if $runtimeStore.currentRoute === 'pair' || $runtimeStore.currentRoute === 'entry'}
  <PairingView />
{:else if $runtimeStore.currentRoute === 'onboarding'}
  <OnboardingView />
{:else if $runtimeStore.currentRoute === 'projects'}
  <ProjectExplorerView />
{:else if $runtimeStore.currentRoute === 'reports'}
  <ReportsView />
{:else if $runtimeStore.currentRoute === 'decisions'}
  <DecisionView />
{:else if $runtimeStore.currentRoute === 'settings'}
  <SettingsView />
{:else if $runtimeStore.currentRoute === 'machine-settings'}
  <MachineSettingsView />
{:else if $runtimeStore.currentRoute === 'system'}
  <SystemView />
{:else if $runtimeStore.currentRoute === 'cleanup'}
  <CleanupView />
{:else if $runtimeStore.currentRoute === 'designer'}
  <ProcessDesigner />
{:else if $runtimeStore.currentRoute === 'workspace'}
  <WorkspaceView />
{:else if $runtimeStore.currentRoute === 'plan'}
  <PlanView />
{:else if $runtimeStore.currentRoute === 'epic'}
  <EpicView />
{:else if $runtimeStore.currentRoute === 'decomposition'}
  <EpicDecompositionView />
{:else}
  <OverviewView />
{/if}
