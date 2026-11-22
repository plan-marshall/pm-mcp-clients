<script lang="ts">
  import HoldResolutionModal from '../../lib/components/HoldResolutionModal.svelte';
  import Button from '../../lib/components/Button.svelte';
  import type { HoldRecord } from '../../lib/api/client';

  /**
   * The position a view opens the hold modal in: one act of the view opens it, the dialog closes
   * back onto that act. It stands in for `WorkControlPanel`'s _Resolve the hold_.
   */
  export let hold: HoldRecord;
  export let openLabel = '';
  export let onResolve: (payload: { act: string; reason?: string }) => void = () => {};

  let open = false;
</script>

<Button on:click={() => (open = true)}>{openLabel}</Button>

<HoldResolutionModal {hold} {open} {onResolve} onClose={() => (open = false)} />
