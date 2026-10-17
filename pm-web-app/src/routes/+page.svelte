<script lang="ts">
  import { runtimeStore } from '../lib/stores/runtime';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import WorkspaceView from '../lib/views/WorkspaceView.svelte';
  import SettingsView from '../lib/views/SettingsView.svelte';
  import MachineSettingsView from '../lib/views/MachineSettingsView.svelte';
  import ProcessDesigner from '../lib/views/ProcessDesigner.svelte';
  import RevokedView from '../lib/views/RevokedView.svelte';
  import OverviewView from '../lib/views/OverviewView.svelte';
  import ReportsView from '../lib/views/ReportsView.svelte';
  import DecisionView from '../lib/views/DecisionView.svelte';
  import SystemView from '../lib/views/SystemView.svelte';
  import CleanupView from '../lib/views/CleanupView.svelte';
  import ProjectExplorerView from '../lib/views/ProjectExplorerView.svelte';
  import PlanView from '../lib/views/PlanView.svelte';
  import EpicView from '../lib/views/EpicView.svelte';
  import StartWorkModal from '../lib/components/StartWorkModal.svelte';
  import GitSourceIntakeModal from '../lib/components/GitSourceIntakeModal.svelte';
  import { startModalStore, closeStartWork } from '../lib/stores/start';
  import { gitSourceModalStore, closeGitSource } from '../lib/stores/gitSource';
</script>

{#if $runtimeStore.currentRoute === 'revoked'}
  <RevokedView />
{:else if !$runtimeStore.isPaired || $runtimeStore.currentRoute === 'pair'}
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
{:else}
  <OverviewView />
{/if}

<StartWorkModal
  open={$startModalStore.open}
  projectId={$startModalStore.projectId}
  kind={$startModalStore.kind}
  onClose={closeStartWork}
  onStarted={() => {}}
/>

<GitSourceIntakeModal
  open={$gitSourceModalStore.open}
  projectId={$gitSourceModalStore.projectId}
  onClose={closeGitSource}
  onCompleted={() => {}}
/>
