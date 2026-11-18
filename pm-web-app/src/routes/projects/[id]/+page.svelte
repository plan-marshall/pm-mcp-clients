<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/projects/{id}` addresses the one project page; the path names the project the store shows.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
