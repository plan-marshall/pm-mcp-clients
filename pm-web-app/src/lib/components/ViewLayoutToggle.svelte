<script lang="ts">
  import { t } from '../i18n';
  import type { ViewLayout } from '../stores/viewLayout';

  let {
    layout,
    modes,
    onchange,
  }: {
    layout: ViewLayout;
    modes: readonly ViewLayout[];
    onchange: (layout: ViewLayout) => void;
  } = $props();
</script>

<div class="layout-toggle" role="group" aria-label={$t('common.layout.label')}>
  {#each modes as mode (mode)}
    <button
      type="button"
      class="layout-toggle__option"
      class:layout-toggle__option--active={layout === mode}
      aria-pressed={layout === mode}
      data-advisory={mode === 'comfortable'
        ? $t('common.layout.comfortable.title')
        : mode === 'compact'
          ? $t('common.layout.compact.title')
          : $t('common.layout.split.title')}
      data-testid="layout-{mode}"
      onclick={() => onchange(mode)}
    >
      {$t(`common.layout.${mode}`)}
    </button>
  {/each}
</div>

<style>
  .layout-toggle {
    display: inline-flex;
    padding: 2px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .layout-toggle__option {
    padding: var(--space-1) var(--space-3);
    border-radius: var(--radius-sm);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    font-weight: 500;
    white-space: nowrap;
  }

  .layout-toggle__option:hover {
    color: var(--color-text-primary);
  }

  .layout-toggle__option--active {
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    box-shadow: var(--shadow-sm);
  }
</style>
