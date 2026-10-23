<script lang="ts">
  import markUrl from '../icons/brand/plan-marshall-mark.svg?url';

  interface Props {
    /** The delivered size; the header uses 24 and the compact surfaces use 20. */
    size?: 20 | 24;
    /** An accessible name, for a mark that stands without the visible brand text. */
    label?: string;
  }

  let { size = 24, label }: Props = $props();
</script>

<span
  class="brand-mark"
  style={`--brand-mark-url: url("${markUrl}"); --brand-mark-size: ${size}px;`}
  role={label ? 'img' : undefined}
  aria-label={label || undefined}
  aria-hidden={label ? undefined : 'true'}
  data-testid="brand-mark"
></span>

<style>
  .brand-mark {
    display: inline-block;
    inline-size: var(--brand-mark-size);
    block-size: var(--brand-mark-size);
    flex: 0 0 auto;
    background-color: currentColor;
    -webkit-mask-image: var(--brand-mark-url);
    mask-image: var(--brand-mark-url);
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-size: contain;
    mask-size: contain;
    vertical-align: -0.125em;
  }
</style>
