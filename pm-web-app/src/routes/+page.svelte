<script lang="ts">
  import { runtimeStore } from '../lib/stores/runtime';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import WorkspaceView from '../lib/views/WorkspaceView.svelte';
  import SettingsView from '../lib/views/SettingsView.svelte';
  import ProcessDesigner from '../lib/views/ProcessDesigner.svelte';
  import RevokedView from '../lib/views/RevokedView.svelte';
  import OverviewView from '../lib/views/OverviewView.svelte';
  import ReportsView from '../lib/views/ReportsView.svelte';
</script>

{#if $runtimeStore.currentRoute === 'revoked'}
  <RevokedView />
{:else if !$runtimeStore.isPaired || $runtimeStore.currentRoute === 'pair'}
  <PairingView />
{:else if $runtimeStore.currentRoute === 'onboarding'}
  <OnboardingView />
{:else if $runtimeStore.currentRoute === 'reports'}
  <ReportsView />
{:else if $runtimeStore.currentRoute === 'settings'}
  <SettingsView />
{:else if $runtimeStore.currentRoute === 'designer'}
  <ProcessDesigner />
{:else if $runtimeStore.currentRoute === 'workspace'}
  <WorkspaceView />
{:else}
  <OverviewView />
{/if}
