<script lang="ts">
  import type { Component } from 'svelte';
  import MissingScreen from './MissingScreen.svelte';
  import type { OnboardingScreenContext, ScreenProps } from './screens';

  /**
   * Draws the one screen the registry named for the current state, and the fallback when it named
   * none.
   *
   * The host exists because the screen changes while the frame stays: a component held in a
   * constant inside the view's markup would keep the first screen it saw.
   */
  let {
    screen,
    view,
  }: { screen: Component<ScreenProps> | null; view: OnboardingScreenContext } = $props();

  const Screen = $derived(screen);
</script>

{#if Screen}
  <Screen {view} />
{:else}
  <MissingScreen {view} />
{/if}
