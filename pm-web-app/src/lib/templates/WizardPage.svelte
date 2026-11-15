<script lang="ts">
  import { onMount, afterUpdate, type Snippet } from 'svelte';
  import SideColumn from './SideColumn.svelte';
  import { MEASURE_FALLBACK, pageInset, windowFor } from './columnWindow';

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

  /** The widest the content is, the measure a form wants. */
  const CONTENT_MAX = 720;

  /** The measuring frame: the template's own container. */
  let frame: HTMLElement | null = null;
  /** The container's measured width; `0` until it can be measured. */
  let measured = 0;

  // The viewport is never the source of the width: the container is. Where no layout engine reports
  // a box at all, the desktop measure stands in (MEASURE_FALLBACK).
  $: container = measured > 0 ? measured : MEASURE_FALLBACK;
  $: layout = windowFor({ container, depth: 1, sideOpen: sideOpen && side !== undefined });
  $: contentWidth = layout.sideTakesSpace
    ? 0
    : Math.min(CONTENT_MAX, sideOpen && side !== undefined ? layout.objectWidth : layout.width);

  onMount(() => {
    if (frame === null) {
      return;
    }
    // A browser lays the element out before the observer's first delivery; read that box now.
    const initial = frame.getBoundingClientRect().width;
    if (initial > 0) {
      measured = initial;
    }
    if (typeof ResizeObserver === 'undefined') {
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

  /** Whether the chat held the space at the last update, so a takeover is seen once. */
  let sideTaken = false;
  /** What held the focus before the chat took the space, to restore it when the content returns. */
  let rememberedFocus: HTMLElement | null = null;

  // The focus never falls to the page when the chat takes the space: it moves into the chat, and it
  // returns to the control it left when the content comes back (`navigation.adoc#side-columns`).
  afterUpdate(() => {
    const taken = layout.sideTakesSpace;
    if (taken === sideTaken) {
      return;
    }
    sideTaken = taken;
    if (taken) {
      const active = document.activeElement as HTMLElement | null;
      rememberedFocus = active !== null && frame?.contains(active) ? active : null;
      frame?.querySelector<HTMLElement>('[data-testid="side-column"] button')?.focus();
    } else if (rememberedFocus !== null && rememberedFocus.isConnected) {
      // The focus returns only while it is still the wizard's: a person who moved to the shell
      // keeps it there.
      const active = document.activeElement as HTMLElement | null;
      const stillOurs =
        active === null || active === document.body || frame?.contains(active) === true;
      if (stillOurs) {
        rememberedFocus.focus();
      }
      rememberedFocus = null;
    }
  });
</script>

<div
  class="wizard-page"
  data-testid="wizard-page"
  style:--wizard-inset={`${pageInset(container)}px`}
  bind:this={frame}
>
  <div class="wizard-page__layout">
    <div
      class="wizard-page__content"
      data-testid="wizard-content"
      hidden={layout.sideTakesSpace}
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

<svelte:window on:keydown={onKeydown} />

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
    max-block-size: 100%;
    margin-inline: auto;
    min-inline-size: 0;
    overflow-y: auto;
  }

  /* The content stays mounted while the chat takes the space: a form keeps its state. */
  .wizard-page__content[hidden] {
    display: none;
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
