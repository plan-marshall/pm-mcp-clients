<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../../lib/stores/runtime';

  // `/projects/onboarding/{id}` is the address of one onboarding run: a reload or a second tab
  // restores the run from it (`adoptRouteFromLocation` reads the id out of the path).
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
