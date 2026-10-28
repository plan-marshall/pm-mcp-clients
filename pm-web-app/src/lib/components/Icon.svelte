<script lang="ts">
  import { uiIconUrl } from '../icons/uiIcons';

  /**
   * One UI icon of the delivered set, painted through its alpha mask.
   *
   * The icon is decorative wherever a visible label carries the meaning, which is every place the
   * interface uses it; `label` names it where it would stand alone. A name the set does not deliver
   * draws nothing: there is no neutral member of the set to fall back to.
   */
  interface Props {
    /** The icon name from `uiIcons`. */
    name: string | null | undefined;
    /** The delivered size; the icon contract allows 16, 20, and 24. */
    size?: 16 | 20 | 24;
    /** An accessible name, for an icon that stands without a visible label. */
    label?: string;
  }

  let { name, size = 20, label }: Props = $props();

  let url = $derived(uiIconUrl(name));
</script>

{#if url}
  <span
    class="ui-icon"
    style={`--ui-icon-url: url("${url}"); --ui-icon-size: ${size}px;`}
    role={label ? 'img' : undefined}
    aria-label={label || undefined}
    aria-hidden={label ? undefined : 'true'}
    data-icon={name ?? ''}
  ></span>
{/if}

<style>
  .ui-icon {
    display: inline-block;
    inline-size: var(--ui-icon-size);
    block-size: var(--ui-icon-size);
    flex: 0 0 auto;
    background-color: currentColor;
    -webkit-mask-image: var(--ui-icon-url);
    mask-image: var(--ui-icon-url);
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-size: contain;
    mask-size: contain;
    vertical-align: -0.125em;
  }
</style>
