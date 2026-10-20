<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/plans/{id}` addresses one plan; the path names the plan the store shows.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
