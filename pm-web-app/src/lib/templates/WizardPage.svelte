<script lang="ts">
  import { onMount, type Snippet } from 'svelte';
  import SideColumn from './SideColumn.svelte';
  import { pageInset, windowFor } from './columnWindow';

  /**
   * The wizard page (UX-P-03, template C): the compact step bar, the one card and the footer with
   * Back, the secondary act and the primary act.
   *
   * The content is 720 px wide and centred, the measure a form wants. Beside it the chat docks as a
   * side column (`navigation.adoc#side-columns`): `480` or `400 px` by the width of the page's own
   * container — never the viewport — and where the content and the chat do not fit side by side the
   * chat takes the space. The template owns the bar, the card surface, the footer and the side
   * column; the view fills the steps, the card's content, the acts and the chat.
   */

  /** The chat's content; absent when the page opens none. */
  export let side: Snippet | undefined = undefined;
  /** Whether the chat's side column is shown. */
  export let sideOpen = false;
  /** The act that hides the chat; the view replaces the address. */
  export let onHideSide: () => void = () => {};

  /**
   * The width a container is assumed to have before the observer's first report: the page's own
   * width where it reports one, and the desktop measure where no layout engine reports anything.
   */
  const PROVISIONAL_WIDTH =
    typeof window !== 'undefined' && window.innerWidth > 0 ? window.innerWidth : 1024;
  /** The widest the content is, the measure a form wants. */
  const CONTENT_MAX = 720;

  /** The measuring frame: the template's own container. */
  let frame: HTMLElement | null = null;
  /** The container's measured width; `0` until the observer's first report. */
  let measured = 0;

  $: container = measured > 0 ? measured : PROVISIONAL_WIDTH;
  $: layout = windowFor({ container, depth: 1, sideOpen: sideOpen && side !== undefined });
  $: contentWidth = layout.sideTakesSpace
    ? 0
    : Math.min(CONTENT_MAX, sideOpen && side !== undefined ? layout.objectWidth : layout.width);

  onMount(() => {
    if (frame === null || typeof ResizeObserver === 'undefined') {
      return;
    }
    const observer = new ResizeObserver((entries) => {
      const entry = entries[entries.length - 1];
      if (entry) {
        measured = entry.contentRect.width;
      }
    });
    observer.observe(frame);
    return () => observer.disconnect();
  });

  /**
   * @param event the key press on the page
   */
  function onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && sideOpen && side !== undefined) {
      event.preventDefault();
      onHideSide();
    }
  }
</script>

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div
  class="wizard-page"
  data-testid="wizard-page"
  style:--wizard-inset={`${pageInset(container)}px`}
  bind:this={frame}
  role="group"
  on:keydown={onKeydown}
>
  <div class="wizard-page__layout">
    {#if !layout.sideTakesSpace}
      <div
        class="wizard-page__content"
        data-testid="wizard-content"
        style:inline-size={`${contentWidth}px`}
      >
        {#if $$slots.steps}
          <div class="wizard-page__steps" data-testid="wizard-page-steps">
            <slot name="steps" />
          </div>
        {/if}

        <div class="wizard-page__card" data-testid="wizard-page-card">
          <slot />
        </div>

        {#if $$slots.back || $$slots.secondary || $$slots.primary}
          <footer class="wizard-page__footer" data-testid="wizard-page-footer">
            <div class="wizard-page__back">
              <slot name="back" />
            </div>
            <div class="wizard-page__acts">
              <slot name="secondary" />
              <slot name="primary" />
            </div>
          </footer>
        {/if}
      </div>
    {/if}

    {#if sideOpen && side}
      <SideColumn
        {side}
        width={layout.sideWidth}
        takesSpace={layout.sideTakesSpace}
        onHide={onHideSide}
      />
    {/if}
  </div>
</div>

<style>
  .wizard-page {
    inline-size: 100%;
    min-inline-size: 0;
    padding-block: var(--space-6);
    overflow-x: hidden;
  }

  .wizard-page__layout {
    display: flex;
    flex-wrap: nowrap;
    align-items: stretch;
    justify-content: center;
    box-sizing: border-box;
    inline-size: 100%;
    min-inline-size: 0;
    padding-inline: var(--wizard-inset, 0);
    gap: 0;
  }

  .wizard-page__content {
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
    gap: var(--space-5);
    max-inline-size: 720px;
    margin-inline: auto;
    min-inline-size: 0;
    overflow-y: auto;
  }

  .wizard-page__steps {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .wizard-page__card {
    padding: var(--space-5);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    box-shadow: var(--shadow-1);
  }

  .wizard-page__footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .wizard-page__acts {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  /* The phone takes the app's narrow inset, as every view did before the template owned it. */
  @media (max-width: 480px) {
    .wizard-page {
      padding-block: var(--space-3);
    }
  }
</style>
