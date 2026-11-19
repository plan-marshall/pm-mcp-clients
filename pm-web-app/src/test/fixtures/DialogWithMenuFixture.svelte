<script lang="ts">
  import Dialog from '../../lib/components/Dialog.svelte';
  import RowMenu from '../../lib/components/RowMenu.svelte';
  import type { ActMenuItem } from '../../lib/components/ActMenu.svelte';

  export let title = '';
  export let menuLabel = '';
  export let items: ActMenuItem[] = [];
  export let open = true;
  export let onClose: () => void = () => {};
</script>

<Dialog {open} {title} {onClose}>
  <RowMenu label={menuLabel} {items} />
</Dialog>
