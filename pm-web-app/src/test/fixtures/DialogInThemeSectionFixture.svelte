<script lang="ts">
  import Dialog from '../../lib/components/Dialog.svelte';

  /**
   * A dialog drawn inside a section that carries a theme of its own, as the development page draws
   * its dialogs: the portal keeps the dialog in the section, so it takes the section's palette.
   */
  export let theme = 'dark';
  export let title = 'Themed dialog';
  export let open = true;
  export let onClose: () => void = () => {};
</script>

<section data-theme={theme} data-testid="theme-section">
  <Dialog {open} {title} {onClose} />
</section>
