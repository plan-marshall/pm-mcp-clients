<script lang="ts">
  import Icon from './Icon.svelte';

  /**
   * One error message under one field (components-fields.adoc, One Field System).
   * Linked to the control by `aria-describedby` and announced through `role="alert"`.
   */
  interface Props {
    /** The error message; nothing is rendered when empty or null. */
    message?: string | null;
    /** The element id for `aria-describedby` linkage. */
    id?: string;
  }

  let { message = '', id }: Props = $props();

  const text = $derived(message ?? '');
</script>

{#if text}
  <p {id} class="field-error" role="alert">
    <Icon name="state.error" size={16} />
    <span class="field-error__text">{text}</span>
  </p>
{/if}

<style>
  .field-error {
    display: flex;
    align-items: center;
    gap: var(--space-1);
    margin: 0;
    color: var(--color-danger);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
  }

  .field-error__text {
    word-break: break-word;
  }
</style>
