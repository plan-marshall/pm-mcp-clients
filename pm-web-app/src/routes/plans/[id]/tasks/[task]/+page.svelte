<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../../../lib/stores/runtime';

  // `/plans/{id}/tasks/{task}` addresses a task of a plan: a level of its own, with no tab of the
  // plan.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
