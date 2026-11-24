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
  export let onOuterClose: () => void = () => {};
  export let onInnerClose: () => void = () => {};
  /** Whether the outer dialog draws its footer; a stage without an act turns it off. */
  export let outerFooter = true;
  /** A counter whose change re-renders the outer dialog, as a view's title update does. */
  export let outerTick = 0;

  let innerOpen = false;
</script>

<Dialog open={true} title={`${outerTitle}${outerTick > 0 ? ` ${outerTick}` : ''}`} footer={outerFooter} onClose={onOuterClose}>
  <Button on:click={() => (innerOpen = true)}>{openInnerLabel}</Button>
  <svelte:fragment slot="footer">
    <Button variant="ghost">{outerActLabel}</Button>
  </svelte:fragment>

  <Dialog
    open={innerOpen}
    title={innerTitle}
    onClose={() => {
      innerOpen = false;
      onInnerClose();
    }}
  >
    <Button>{innerFirstLabel}</Button>
    <svelte:fragment slot="footer">
      <Button>{innerLastLabel}</Button>
    </svelte:fragment>
  </Dialog>
</Dialog>
