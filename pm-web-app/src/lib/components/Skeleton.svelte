<script lang="ts">
  import { onMount } from 'svelte';

  /** The four shapes of VD-P-05: a row of text, a card, a table and a figure. */
  type SkeletonShape = 'row' | 'card' | 'table' | 'figure';

  /**
   * The blocks a view draws while what it shows is still loading (VD-P-05, VD-S-04): a row, a card,
   * a table or a figure, in `--color-bg-subtle` with a slow shimmer.
   *
   * The shimmer stops where the person asked for reduced motion, and the blocks then stand still:
   * the shape still says what is coming, without a movement the design system may not use there.
   * A skeleton with no `label` is decoration and stays out of the accessibility tree; the view that
   * announces its load names it.
   */
  export let shape: SkeletonShape = 'row';
  /** How many pieces the shape repeats: the lines of a row, the rows of a table. */
  export let rows = 1;
  /** The accessible name of the skeleton; without one it is decorative. */
  export let label: string | undefined = undefined;

  /** Whether the person asked the system for reduced motion. */
  let reducedMotion = false;

  onMount(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    reducedMotion = query.matches;
    const onChange = (event: MediaQueryListEvent): void => {
      reducedMotion = event.matches;
    };
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  });

  /** The parts the shape is built from, in the order it draws them. */
  $: parts =
    shape === 'card'
      ? ['title', 'line', 'line']
      : shape === 'figure'
        ? ['figure']
        : shape === 'table'
          ? ['head', ...Array.from({ length: Math.max(rows, 0) }, () => 'row')]
          : Array.from({ length: Math.max(rows, 0) }, () => 'line');
</script>

<div
  class="skeleton"
  data-testid="skeleton"
  data-shape={shape}
  data-motion={reducedMotion ? 'none' : 'shimmer'}
  role={label ? 'status' : undefined}
  aria-label={label || undefined}
  aria-hidden={label ? undefined : 'true'}
>
  {#each parts as part, index (index)}
    <span class="skeleton__block" data-part={part}></span>
  {/each}
</div>

<style>
  .skeleton {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    inline-size: 100%;
  }

  .skeleton__block {
    display: block;
    inline-size: 100%;
    block-size: var(--space-4);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    background-image: linear-gradient(
      90deg,
      transparent 0%,
      var(--color-bg-raised) 50%,
      transparent 100%
    );
    background-repeat: no-repeat;
    background-size: 200% 100%;
    background-position: 200% 0;
  }

  /* A slow shimmer, and none at all where the person asked for less motion. */
  .skeleton[data-motion='shimmer'] .skeleton__block {
    animation: skeleton-shimmer 1.6s var(--ease-standard) infinite;
  }

  .skeleton[data-motion='none'] .skeleton__block {
    animation: none;
    background-image: none;
  }

  .skeleton__block[data-part='title'] {
    inline-size: 40%;
    block-size: var(--space-5);
  }

  .skeleton__block[data-part='line']:last-child {
    inline-size: 80%;
  }

  .skeleton__block[data-part='head'] {
    block-size: var(--space-5);
  }

  .skeleton__block[data-part='row'] {
    block-size: var(--space-5);
  }

  .skeleton__block[data-part='figure'] {
    block-size: 120px;
    border-radius: var(--radius-md);
  }

  @keyframes skeleton-shimmer {
    from {
      background-position: 200% 0;
    }

    to {
      background-position: -200% 0;
    }
  }
</style>
