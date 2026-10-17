<script lang="ts">
  import { onMount } from 'svelte';
  import { navigateTo } from '../../lib/stores/runtime';
  import ProjectExplorerView from '../../lib/views/ProjectExplorerView.svelte';

  onMount(() => {
    navigateTo('projects');
  });
</script>

<ProjectExplorerView />
