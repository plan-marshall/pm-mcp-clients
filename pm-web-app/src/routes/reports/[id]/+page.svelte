<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/reports/{id}` addresses one report's detail beside the list; the view resolves the report's
  // plan from the report itself.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
