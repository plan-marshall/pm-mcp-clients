<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/entries/{id}` continues an entry another client opened, whichever screen it reached.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
