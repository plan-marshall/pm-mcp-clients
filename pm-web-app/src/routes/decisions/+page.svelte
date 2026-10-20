<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../lib/stores/runtime';

  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
