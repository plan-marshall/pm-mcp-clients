<script lang="ts">
  import { workflowIconUrl } from '../icons/workflowIcons';

  interface Props {
    /** The icon id from a unit's frontmatter or the catalogue. */
    id: string | null | undefined;
    /** The delivered size; the visual specification allows 16, 20, and 24. */
    size?: 16 | 20 | 24;
    /** An accessible name, for an icon that stands without a visible label. */
    label?: string;
  }

  let { id, size = 20, label }: Props = $props();

  /** An unknown id still renders: the registry resolves it to the neutral fallback. */
  let url = $derived(workflowIconUrl(id));
</script>

<span
  class="workflow-icon"
  style={`--workflow-icon-url: url("${url}"); --workflow-icon-size: ${size}px;`}
  role={label ? 'img' : undefined}
  aria-label={label || undefined}
  aria-hidden={label ? undefined : 'true'}
  data-workflow-icon={id ?? ''}
></span>

<style>
  .workflow-icon {
    display: inline-block;
    inline-size: var(--workflow-icon-size);
    block-size: var(--workflow-icon-size);
    flex: 0 0 auto;
    background-color: currentColor;
    -webkit-mask-image: var(--workflow-icon-url);
    mask-image: var(--workflow-icon-url);
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: center;
    mask-position: center;
    -webkit-mask-size: contain;
    mask-size: contain;
    vertical-align: -0.125em;
  }
</style>
