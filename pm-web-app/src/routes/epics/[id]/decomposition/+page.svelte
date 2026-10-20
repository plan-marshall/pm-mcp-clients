<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../../lib/views/AppRouter.svelte';
  import { openEpicDecomposition } from '../../../../lib/stores/runtime';

  // The path names the epic; the dispatcher renders its decomposition, and a raw history entry
  // to another view still replaces this page's content through the store.
  onMount(() => {
    const parts = window.location.pathname.split('/').filter(Boolean);
    const id = parts[1] ? decodeURIComponent(parts[1]) : '';
    const project = new URLSearchParams(window.location.search).get('project') ?? 'pm-mcp-clients';
    openEpicDecomposition(id, project);
  });
</script>

<AppRouter />
