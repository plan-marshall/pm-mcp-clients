<script lang="ts">
  import Dialog from '../../lib/components/Dialog.svelte';
  import Button from '../../lib/components/Button.svelte';

  /**
   * A dialog opened from another dialog, as a view opens a confirmation on top of the dialog it is
   * drawn in. Both stay in the document; the inner one is on top. Every word is a prop, as in a view.
   */
  export let outerTitle = '';
  export let innerTitle = '';
  export let openInnerLabel = '';
  export let outerActLabel = '';
  export let innerFirstLabel = '';
  export let innerLastLabel = '';

  let innerOpen = false;
</script>

<Dialog open={true} title={outerTitle} onClose={() => {}}>
  <Button on:click={() => (innerOpen = true)}>{openInnerLabel}</Button>
  <svelte:fragment slot="footer">
    <Button variant="ghost">{outerActLabel}</Button>
  </svelte:fragment>

  <Dialog open={innerOpen} title={innerTitle} onClose={() => (innerOpen = false)}>
    <Button>{innerFirstLabel}</Button>
    <svelte:fragment slot="footer">
      <Button>{innerLastLabel}</Button>
    </svelte:fragment>
  </Dialog>
</Dialog>
