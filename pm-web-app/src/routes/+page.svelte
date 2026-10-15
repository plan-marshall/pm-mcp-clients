<script lang="ts">
  import { runtimeStore } from '../lib/stores/runtime';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import WorkspaceView from '../lib/views/WorkspaceView.svelte';
  import SettingsView from '../lib/views/SettingsView.svelte';
  import ProcessDesigner from '../lib/views/ProcessDesigner.svelte';
</script>

{#if !$runtimeStore.isPaired || $runtimeStore.currentRoute === 'pair'}
  <PairingView />
{:else if $runtimeStore.currentRoute === 'onboarding'}
  <OnboardingView />
{:else if $runtimeStore.currentRoute === 'settings'}
  <SettingsView />
{:else if $runtimeStore.currentRoute === 'designer'}
  <ProcessDesigner />
{:else}
  <WorkspaceView />
{/if}
