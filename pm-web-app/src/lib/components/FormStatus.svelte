<script lang="ts" module>
  /** The tone of a form's result line. */
  export type FormStatusTone = 'neutral' | 'success' | 'danger';
</script>

<script lang="ts">
  import Icon from './Icon.svelte';

  /**
   * The result line of a form, above its buttons (components-fields.adoc, One Field System):
   * _Saved_, the reason the submit is held back, or the error of the whole form.
   *
   * The polite live region is always in the document and empty without a message: a region that
   * arrives with its text is a node several screen readers never announce, so a message is an update
   * to a region the reader already knows. The danger tone draws the error icon beside its sentence,
   * so colour never carries the tone alone.
   */
  interface Props {
    /** The sentence, from the catalogue; nothing is shown without one. */
    message?: string | null;
    /** The tone of the sentence. */
    tone?: FormStatusTone;
    /** The id a control points at with `aria-describedby`. */
    id?: string;
  }

  let { message = '', tone = 'neutral', id }: Props = $props();

  const text = $derived(message ?? '');
</script>

<p {id} class="form-status form-status--{tone}" role="status" aria-live="polite">
  {#if text}
    {#if tone === 'danger'}
      <Icon name="state.error" size={16} />
    {/if}
    <span class="form-status__text">{text}</span>
  {/if}
</p>

<style>
  .form-status {
    display: flex;
    align-items: center;
    gap: var(--space-1);
    min-block-size: var(--line-height-sm);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .form-status--neutral {
    color: var(--color-text-secondary);
  }

  .form-status--success {
    color: var(--color-success);
  }

  .form-status--danger {
    color: var(--color-danger);
  }
</style>
