<script lang="ts">
  interface Props {
    /** The delivered size; the header uses 24 and the compact surfaces use 20. */
    size?: 20 | 24;
    /** An accessible name, for a mark that stands without the visible brand text. */
    label?: string;
  }

  let { size = 24, label }: Props = $props();
</script>

<svg
  xmlns="http://www.w3.org/2000/svg"
  viewBox="0 0 24 24"
  width={size}
  height={size}
  class="brand-mark"
  data-size={size}
  role={label ? 'img' : undefined}
  aria-label={label || undefined}
  aria-hidden={label ? undefined : 'true'}
  data-testid="brand-mark"
>
  <g fill="currentColor">
    <path d="M2.5 21.5V14.2L7.2 11.6V21.5Z" />
    <path d="M9.4 21.5V9.2L14.1 6.6V21.5Z" />
    <path d="M16.3 21.5V4.8L21 2.2V21.5Z" />
  </g>
  <path
    d="M2.7 11.3 13.9 4.4"
    fill="none"
    stroke="currentColor"
    stroke-width="1.9"
    stroke-linecap="round"
  />
  <circle cx="16.2" cy="3.7" r="1.4" fill="currentColor" />
</svg>

<style>
  .brand-mark {
    display: inline-block;
    flex: 0 0 auto;
    vertical-align: -0.125em;
  }
</style>
