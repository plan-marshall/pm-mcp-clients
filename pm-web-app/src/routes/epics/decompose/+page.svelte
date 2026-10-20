<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/epics/decompose` opens a decomposition without an epic yet; an epic already cut keeps its
  // own `/epics/{id}/decomposition` path.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
