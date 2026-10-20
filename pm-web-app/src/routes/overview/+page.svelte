<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../lib/stores/runtime';

  // `/overview` is the running and ended work; the layout has already adopted the path.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
