<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../lib/stores/runtime';

  // `/decomposition` is the workflow's own path, so no epic id can collide with it.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
