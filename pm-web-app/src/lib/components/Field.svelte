<script lang="ts">
  import type { Snippet } from 'svelte';
  import { t } from '../i18n';
  import FieldError from './FieldError.svelte';

  /**
   * The wrapper of every control (components-fields.adoc, One Field System).
   *
   * Owns the label above the control, the optional mark, the hint under it, the error in the
   * hint's place while it shows, and the disabled state with its reason.
   */
  interface Props {
    /** The element id of the control the label points to. */
    id?: string;
    /** The visible label text in sentence case as a noun phrase. */
    label?: string;
    /** Whether the field is optional; draws `(optional)` after the label. */
    optional?: boolean;
    /** One sentence under the field explaining format or consequence. */
    hint?: string;
    /** One error message from the catalogue under the field. */
    error?: string | null;
    /** Whether the field is disabled. */
    disabled?: boolean;
    /** Why the field is disabled, linked to the control and visible under it. */
    disabledReason?: string;
    /** Optional id for the hint element. Defaults to `{id}-hint`. */
    hintId?: string;
    /** Optional id for the error element. Defaults to `{id}-error`. */
    errorId?: string;
    /** Optional id for the disabled-reason element. Defaults to `{id}-reason`. */
    reasonId?: string;
    /** The control itself. */
    children?: Snippet;
  }

  let {
    id,
    label,
    optional = false,
    hint,
    error,
    disabled = false,
    disabledReason,
    hintId,
    errorId,
    reasonId,
    children,
  }: Props = $props();

  const effectiveHintId = $derived(hintId ?? (id ? `${id}-hint` : undefined));
  const effectiveErrorId = $derived(errorId ?? (id ? `${id}-error` : undefined));
  const effectiveReasonId = $derived(reasonId ?? (id ? `${id}-reason` : undefined));
</script>

<div class="field" class:field--disabled={disabled} class:field--invalid={Boolean(error)}>
  {#if label}
    <label for={id} class="field__label">
      <span>{label}</span>
      {#if optional}
        <span class="field__optional">{$t('common.field.optional')}</span>
      {/if}
    </label>
  {/if}

  <div class="field__control">
    {@render children?.()}
  </div>

  {#if error}
    <FieldError id={effectiveErrorId} message={error} />
  {:else if hint}
    <p id={effectiveHintId} class="field__hint">{hint}</p>
  {/if}

  {#if disabled && disabledReason}
    <p id={effectiveReasonId} class="field__disabled-reason">{disabledReason}</p>
  {/if}
</div>

<style>
  .field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    width: 100%;
  }

  .field__label {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
    line-height: var(--line-height-sm);
  }

  .field__optional {
    font-size: var(--text-xs);
    font-weight: var(--weight-regular);
    color: var(--color-text-muted);
  }

  .field__control {
    display: flex;
    flex-direction: column;
    width: 100%;
  }

  .field__hint,
  .field__disabled-reason {
    margin: 0;
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
    color: var(--color-text-muted);
  }

  .field--disabled .field__label {
    color: var(--color-text-muted);
  }
</style>
