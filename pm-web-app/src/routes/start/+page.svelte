<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../lib/stores/runtime';

  // The address bar names the start; the router shows the screen of the state it reports.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
