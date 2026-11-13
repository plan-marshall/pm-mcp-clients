<script lang="ts" module>
  /** The two renderings of the brand icon: the mascot palette, or one colour in `currentColor`. */
  export type BrandMarkRendering = 'colour' | 'monochrome';

  /** Every size a rendering is delivered at (brand-mark.adoc, Where the Icon Is Used). */
  export const DELIVERED_SIZES = {
    colour: [16, 28, 64],
    monochrome: [16, 20, 24, 28, 64],
  } as const satisfies Record<BrandMarkRendering, readonly number[]>;

  /** A size the given rendering is delivered at. */
  export type BrandMarkSize<R extends BrandMarkRendering> = (typeof DELIVERED_SIZES)[R][number];

  /** The name of the mark, for a mark that stands without the visible wordmark. */
  interface Naming {
    /** An accessible name; without it the mark is decorative. */
    label?: string;
  }

  /**
   * The props, one shape per rendering: a size the rendering is not delivered at is a type error
   * at the call site, so no call site can ask for a mark that does not exist.
   */
  export type BrandMarkProps =
    | (Naming & {
        /** The colour rendering in the mascot palette, the default. */
        rendering?: 'colour';
        /** 16, 28 or 64 px; 28 px by default. */
        size?: BrandMarkSize<'colour'>;
      })
    | (Naming & {
        /** The one-colour rendering in `currentColor`. */
        rendering: 'monochrome';
        /** 16, 20, 24, 28 or 64 px; 28 px by default. */
        size?: BrandMarkSize<'monochrome'>;
      });
</script>

<script lang="ts">
  let { rendering = 'colour', size = 28, label }: BrandMarkProps = $props();
</script>

<svg
  xmlns="http://www.w3.org/2000/svg"
  viewBox="0 0 24 24"
  width={size}
  height={size}
  class="brand-mark"
  data-size={size}
  data-rendering={rendering}
  role={label ? 'img' : undefined}
  aria-label={label || undefined}
  aria-hidden={label ? undefined : 'true'}
  data-testid="brand-mark"
>
  {#if rendering === 'colour'}
    <!-- The colour rendering: the gold star with a ball on each tip and the fur disc, outlined. -->
    <g class="brand-mark__colour">
      <g class="brand-mark__star">
        <path
          d="M12 4.5 14.59 9.34 19.99 10.3 16.18 14.26 16.94 19.7 12 17.3 7.06 19.7 7.82 14.26 4.01 10.3 9.41 9.34Z"
        />
        <circle cx="12" cy="3.9" r="1.5" />
        <circle cx="20.56" cy="10.12" r="1.5" />
        <circle cx="17.29" cy="20.18" r="1.5" />
        <circle cx="6.71" cy="20.18" r="1.5" />
        <circle cx="3.44" cy="10.12" r="1.5" />
      </g>
      <circle class="brand-mark__disc" cx="12" cy="12.9" r="2.9" />
    </g>
  {/if}
  <!-- The monochrome rendering: the star and its balls as one silhouette, a ring cut out around a
       solid centre. In the colour rendering it is drawn only where forced colours are active. -->
  <g class="brand-mark__mono" class:brand-mark__mono--fallback={rendering === 'colour'} fill="currentColor">
    <path
      fill-rule="evenodd"
      d="M12 4.5 14.59 9.34 19.99 10.3 16.18 14.26 16.94 19.7 12 17.3 7.06 19.7 7.82 14.26 4.01 10.3 9.41 9.34ZM9.4 12.9a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0-5.2 0Z"
    />
    <circle cx="12" cy="12.9" r="1.4" />
    <circle cx="12" cy="3.9" r="1.8" />
    <circle cx="20.56" cy="10.12" r="1.8" />
    <circle cx="17.29" cy="20.18" r="1.8" />
    <circle cx="6.71" cy="20.18" r="1.8" />
    <circle cx="3.44" cy="10.12" r="1.8" />
  </g>
</svg>

<style>
  .brand-mark {
    display: inline-block;
    flex: 0 0 auto;
    vertical-align: -0.125em;
  }

  .brand-mark__colour {
    stroke: var(--color-mascot-outline);
    stroke-linejoin: round;
  }

  .brand-mark__star {
    fill: var(--color-mascot-gold);
    stroke-width: 0.9;
  }

  .brand-mark__disc {
    fill: var(--color-mascot-fur);
    stroke-width: 0.8;
  }

  .brand-mark__mono--fallback {
    display: none;
  }

  /* Forced colours draw the monochrome rendering, whatever was asked. */
  @media (forced-colors: active) {
    .brand-mark__colour {
      display: none;
    }

    .brand-mark__mono--fallback {
      display: inline;
    }
  }
</style>
