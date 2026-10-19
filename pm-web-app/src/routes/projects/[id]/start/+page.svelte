<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../../lib/stores/runtime';

  // `/projects/{id}/start` opens a new entry under that project.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
