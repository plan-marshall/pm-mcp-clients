<script lang="ts">
  import Dialog from '../../lib/components/Dialog.svelte';
  import Button from '../../lib/components/Button.svelte';

  /**
   * A test fixture for the slotted `Dialog`: one control opens it, its two footer acts close it
   * again. It is the position a view draws the dialog in — not a component of the page.
   */
  export let title: string;
  export let body: string;
  export let openLabel: string;
  export let cancelLabel: string;
  export let confirmLabel: string;

  let open = false;
</script>

<Button on:click={() => (open = true)}>{openLabel}</Button>

<Dialog {title} {open} onClose={() => (open = false)}>
  <p>{body}</p>
  <svelte:fragment slot="footer">
    <Button variant="ghost" on:click={() => (open = false)}>{cancelLabel}</Button>
    <Button variant="danger" on:click={() => (open = false)}>{confirmLabel}</Button>
  </svelte:fragment>
</Dialog>
