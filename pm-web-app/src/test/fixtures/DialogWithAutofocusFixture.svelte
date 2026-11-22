<script lang="ts">
  import { onMount } from 'svelte';
  import Dialog from '../../lib/components/Dialog.svelte';

  /** A dialog whose body holds a field that takes the focus itself as it mounts. */
  export let title = 'Rename';
  export let label = 'Name';

  let field: HTMLInputElement | undefined;

  onMount(() => field?.focus());
</script>

<Dialog open={true} {title} onClose={() => {}}>
  <input bind:this={field} aria-label={label} />
</Dialog>
