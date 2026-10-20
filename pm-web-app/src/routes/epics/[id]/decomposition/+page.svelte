<script lang="ts">
  import { onMount } from 'svelte';
  import { openEpicDecomposition } from '../../../../lib/stores/runtime';
  import EpicDecompositionView from '../../../../lib/views/EpicDecompositionView.svelte';

  onMount(() => {
    const parts = window.location.pathname.split('/').filter(Boolean);
    const id = parts[1] ? decodeURIComponent(parts[1]) : '';
    const project = new URLSearchParams(window.location.search).get('project') ?? 'pm-mcp-clients';
    openEpicDecomposition(id, project);
  });
</script>

<EpicDecompositionView />
