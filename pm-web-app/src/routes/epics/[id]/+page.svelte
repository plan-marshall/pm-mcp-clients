<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/epics/{id}` addresses one epic; `/epics/decompose` is its own page below.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
