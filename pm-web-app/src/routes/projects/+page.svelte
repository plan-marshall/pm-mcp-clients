<script lang="ts">
  import { onMount } from 'svelte';
  import ProjectExplorerView from '../../lib/views/ProjectExplorerView.svelte';
  import { adoptRouteFromLocation } from '../../lib/stores/runtime';

  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<ProjectExplorerView />
