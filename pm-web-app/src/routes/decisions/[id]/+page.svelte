<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/decisions/{id}` addresses one decision beside the list; the path names the one the store
  // shows.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
