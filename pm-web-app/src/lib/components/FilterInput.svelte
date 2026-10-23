<script module lang="ts">
  let instanceCount = 0;
</script>

<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';

  let {
    value = '',
    label,
    placeholder,
    resultCount,
    onchange,
    testid = 'filter-input',
  }: {
    value?: string;
    label?: string;
    placeholder?: string;
    resultCount?: number;
    onchange: (value: string) => void;
    testid?: string;
  } = $props();

  const inputId = `filter-input-${++instanceCount}`;
  let inputElement = $state<HTMLInputElement | undefined>(undefined);

  /**
   * @param target the event target
   * @returns whether the target already takes typed text, so `/` must reach it unchanged
   */
  function isEditable(target: EventTarget | null): boolean {
    if (!(target instanceof HTMLElement)) return false;
    const tag = target.tagName.toLowerCase();
    return tag === 'input' || tag === 'textarea' || tag === 'select' || target.isContentEditable;
  }

  /**
   * Focuses the filter on `/`, unless the operator is typing somewhere else.
   *
   * @param event the window key event
   */
  function focusOnSlash(event: KeyboardEvent): void {
    if (event.key !== '/' || event.defaultPrevented) return;
    if (event.metaKey || event.ctrlKey || event.altKey) return;
    if (isEditable(event.target)) return;
    event.preventDefault();
    inputElement?.focus();
  }

  /**
   * Clears the filter on `Esc`.
   *
   * @param event the input key event
   */
  function clearOnEscape(event: KeyboardEvent): void {
    if (event.key !== 'Escape') return;
    event.preventDefault();
    onchange('');
    inputElement?.focus();
  }

  onMount(() => {
    window.addEventListener('keydown', focusOnSlash);
    return () => window.removeEventListener('keydown', focusOnSlash);
  });
</script>

<div class="filter" role="search">
  <label class="filter__label" for={inputId}>
    {label ?? $t('common.filter.label')}
  </label>
  <div class="filter__field">
    <input
      id={inputId}
      class="filter__input"
      type="search"
      autocomplete="off"
      {placeholder}
      {value}
      data-testid={testid}
      oninput={(event) => onchange((event.currentTarget as HTMLInputElement).value)}
      onkeydown={clearOnEscape}
      bind:this={inputElement}
    />
    {#if value}
      <button
        type="button"
        class="filter__clear"
        data-testid="filter-clear"
        aria-label={$t('common.filter.clear')}
        data-advisory={$t('common.filter.clear.title')}
        onclick={() => {
          onchange('');
          inputElement?.focus();
        }}
      >
        ×
      </button>
    {/if}
  </div>
  {#if typeof resultCount === 'number'}
    <span class="filter__results" data-testid="filter-results" aria-live="polite">
      {$t('common.filter.results', { count: resultCount })}
    </span>
  {/if}
</div>

<style>
  .filter {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .filter__label {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .filter__field {
    position: relative;
    display: flex;
    align-items: center;
  }

  .filter__input {
    width: 100%;
    padding: var(--space-2) var(--space-8) var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .filter__input:focus {
    outline: none;
    border-color: var(--color-primary);
  }

  .filter__clear {
    position: absolute;
    right: var(--space-1);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1.75rem;
    height: 1.75rem;
    border-radius: var(--radius-full);
    color: var(--color-text-muted);
    font-size: var(--text-lg);
    line-height: 1;
  }

  .filter__clear:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .filter__results {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
</style>
