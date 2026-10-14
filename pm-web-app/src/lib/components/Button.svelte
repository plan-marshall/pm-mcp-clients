<script lang="ts">
  export let variant: 'primary' | 'secondary' | 'danger' | 'ghost' = 'secondary';
  export let type: 'button' | 'submit' | 'reset' = 'button';
  export let disabled = false;
  export let loading = false;
</script>

<button
  {type}
  class="btn btn--{variant}"
  class:btn--loading={loading}
  disabled={disabled || loading}
  on:click
>
  {#if loading}
    <span class="btn__spinner" aria-hidden="true"></span>
  {/if}
  <span class="btn__content"><slot /></span>
</button>

<style>
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-4);
    font-size: var(--text-sm);
    font-weight: 500;
    line-height: 1.25;
    border-radius: var(--radius-md);
    transition: all 0.15s ease-in-out;
    white-space: nowrap;
    border: 1px solid transparent;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn--primary {
    background-color: var(--color-primary);
    color: var(--color-primary-text);
  }

  .btn--primary:hover:not(:disabled) {
    background-color: var(--color-primary-hover);
  }

  .btn--secondary {
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    border-color: var(--color-border);
  }

  .btn--secondary:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
  }

  .btn--danger {
    background-color: var(--color-danger);
    color: var(--color-text-inverse);
  }

  .btn--danger:hover:not(:disabled) {
    filter: brightness(0.9);
  }

  .btn--ghost {
    background-color: transparent;
    color: var(--color-text-secondary);
  }

  .btn--ghost:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .btn__spinner {
    width: 1em;
    height: 1em;
    border: 2px solid currentColor;
    border-right-color: transparent;
    border-radius: 50%;
    animation: spin 0.75s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
</style>
