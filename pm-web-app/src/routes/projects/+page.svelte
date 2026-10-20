<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../lib/stores/runtime';

  // Every route mounts the dispatcher: a raw history entry leaves the page mounted, so the store
  // is the only thing that may decide what is shown.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
