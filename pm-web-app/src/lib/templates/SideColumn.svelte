<script lang="ts">
  import type { Snippet } from 'svelte';
  import Button from '../components/Button.svelte';
  import { t } from '../i18n';

  /**
   * The side column of `navigation.adoc#side-columns`: the chat, docked to the right of the object
   * column of the column engine or of a wizard's content. The template owns the column — its width,
   * its own scroll, the act that hides it and the phone that makes it take the whole screen — and
   * the view gives only its content, so no view builds a side column of its own.
   *
   * It is never a level: the gap strip does not show it, and it takes no part of the chain.
   */
  interface Props {
    /** The chat's content. */
    side: Snippet;
    /** The width the template computed for the column, in px. */
    width: number;
    /** Whether the column takes the template's whole space, the object hidden. */
    takesSpace?: boolean;
    /** The act _Hide the chat_ performs (`Escape` is the template's to route). */
    onHide?: () => void;
  }

  let { side, width, takesSpace = false, onHide = () => {} }: Props = $props();
</script>

<aside
  class="side-column"
  class:side-column--taken={takesSpace}
  data-testid="side-column"
  style:--side-width={`${width}px`}
>
  <header class="side-column__head">
    <Button variant="ghost" size="small" onclick={() => onHide()}>{$t('control.chat.hide')}</Button>
  </header>
  <div class="side-column__body">
    {@render side()}
  </div>
</aside>

<style>
  .side-column {
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
    inline-size: var(--side-width, 0);
    gap: var(--space-3);
    min-inline-size: 0;
    max-block-size: 100%;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .side-column__head {
    display: flex;
    justify-content: flex-end;
    padding-block: var(--space-1);
  }

  .side-column__body {
    min-inline-size: 0;
  }

  /*
   * On a phone the chat takes the whole screen, the bottom bar included: it stands above the shell
   * at a layer of its own, and _Hide the chat_ (or Escape) is the only way back.
   */
  @media (max-width: 767px) {
    .side-column--taken {
      position: fixed;
      inset: 0;
      inline-size: 100vw;
      block-size: 100vh;
      z-index: 60;
      padding: var(--space-3);
      background-color: var(--color-bg-app);
    }
  }
</style>
