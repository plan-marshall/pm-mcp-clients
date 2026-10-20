<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/system/cleanup` is a route below the system view.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
