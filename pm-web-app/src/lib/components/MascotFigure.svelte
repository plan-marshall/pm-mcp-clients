<script lang="ts" module>
  import { writable, type Readable } from 'svelte/store';

  const shownFigures = writable(0);

  /**
   * The number of mascot figures shown right now: drawn, and with a box on the page. A figure a
   * container query or a hidden ancestor sets to `display: none` keeps no box and does not count.
   * The rail-foot bear hides while the number is above zero, so a screen never shows two
   * (brand-mark-mascot.adoc, The Rail-Foot Bear).
   */
  export const mascotFiguresOnScreen: Readable<number> = { subscribe: shownFigures.subscribe };
</script>

<script lang="ts">
  import { mascotFiles, type MascotCrop, type MascotPose } from '../assets/mascot/mascotFiles';

  /**
   * One figure of the mascot on a calm surface (components-brand.adoc, MascotFigure).
   *
   * The surface names a pose and a crop; the files come from `mascotFiles`, so a pose switches to its
   * accepted artwork without a change of the surface. The image is decorative and sized by its
   * `width` and `height` attributes: a style attribute is blocked by the daemon's content security
   * policy, so the halo and the entrance live in this stylesheet alone.
   */

  interface Props {
    /** The pose the surface shows. */
    pose: MascotPose;
    /** The whole figure (the default) or the bust. */
    crop?: MascotCrop;
    /** The drawn height in px; the width follows from the crop's proportions. */
    height: number;
    /** Loads the image at once, for the pairing page's hero; otherwise it loads lazily. */
    eager?: boolean;
    /** Fades and rises the figure in once, unless reduced motion is asked for. */
    entrance?: boolean;
    /**
     * An object URL the app holds, for the states shown after the origin is lost: drawn instead of
     * the pose's files. Passed but empty, the piece draws nothing and keeps no space.
     */
    src?: string;
  }

  let { pose, crop = 'full', height, eager = false, entrance = false, src }: Props = $props();

  const files = $derived(mascotFiles(pose, crop));
  const width = $derived(Math.round((height * files.width) / files.height));
  const drawn = $derived(src !== '');

  /** The drawn image, while there is one. */
  let figure: HTMLImageElement | null = $state(null);

  /**
   * Counts the figure while it has a box on the page. The page's container and media queries hide a
   * figure with `display: none` as the page narrows or loses height, and a view kept open behind
   * another sits in a hidden wrapper; the image then has no box. The observer reports each change of
   * the box, so the count follows the page as it narrows, widens, or shows the view again.
   */
  $effect(() => {
    const element = figure;
    if (element === null) return;
    let shown = false;
    const observer = new ResizeObserver((entries) => {
      const box = entries[entries.length - 1].contentRect;
      const hasBox = box.width > 0 && box.height > 0;
      if (hasBox === shown) return;
      shown = hasBox;
      shownFigures.update((count) => count + (shown ? 1 : -1));
    });
    observer.observe(element);
    return () => {
      observer.disconnect();
      if (shown) shownFigures.update((count) => count - 1);
    };
  });
</script>

{#if drawn}
  <img
    bind:this={figure}
    class="mascot-figure"
    class:mascot-figure--entrance={entrance}
    src={src ?? files.src1x}
    srcset={src === undefined ? `${files.src1x} 1x, ${files.src2x} 2x` : undefined}
    {width}
    {height}
    alt=""
    aria-hidden="true"
    loading={eager ? 'eager' : 'lazy'}
    decoding={eager ? undefined : 'async'}
    data-testid="mascot-figure"
    data-pose={pose}
    data-crop={crop}
  />
{/if}

<style>
  .mascot-figure {
    display: block;
    flex: 0 0 auto;
    max-inline-size: 100%;
    block-size: auto;
  }

  /* The dark theme: a soft halo, so the dark outline does not sink into the background. */
  :global([data-theme='dark']) .mascot-figure {
    background-image: radial-gradient(closest-side, var(--color-bg-subtle), transparent);
  }

  /* One fade and rise of 300 ms. */
  .mascot-figure--entrance {
    animation: mascot-figure-entrance 300ms var(--ease-standard) both;
  }

  @keyframes mascot-figure-entrance {
    from {
      opacity: 0;
      transform: translateY(var(--space-2));
    }

    to {
      opacity: 1;
      transform: none;
    }
  }

  /* Reduced motion: the figure stands where it ends, without an entrance. */
  @media (prefers-reduced-motion: reduce) {
    .mascot-figure--entrance {
      animation: none;
    }
  }
</style>
