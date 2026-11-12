<script lang="ts" module>
  /** The schemes an external link may open; every other address is drawn as text. */
  const WEB_SCHEMES = new Set(['http:', 'https:']);

  /**
   * @param href the address a caller hands in
   * @returns whether it parses as an absolute http or https URL
   */
  export function isWebAddress(href: string): boolean {
    try {
      return WEB_SCHEMES.has(new URL(href).protocol);
    } catch {
      return false;
    }
  }
</script>

<script lang="ts">
  import Icon from './Icon.svelte';
  import { t } from '../i18n';

  /**
   * A link to a place outside the app — a pull request, an issue, the documentation — that opens
   * in a new tab, with the external-link glyph after its label and "opens in a new tab" in its
   * accessible name.
   *
   * The address comes from the runtime, so only a parsed http or https URL becomes an anchor; any
   * other scheme (`javascript:`, `data:`, `file:`) or an unparsable address draws the label as text.
   */
  interface Props {
    /** The address outside the app. */
    href: string;
    /** The destination, named as a noun. */
    label: string;
    /** Draws the chip look of a pull request instead of the text look. */
    chip?: boolean;
  }

  let { href, label, chip = false }: Props = $props();

  const safe = $derived(isWebAddress(href));
</script>

{#if safe}
  <a {href} target="_blank" rel="noopener noreferrer" class="link external-link" class:link--text={!chip} class:link--chip={chip}>
    <span class="external-link__label">{label}</span>
    <Icon name="act.external" size={16} />
    <span class="sr-only">({$t('common.link.new_tab')})</span>
  </a>
{:else}
  <span class="external-link__text" class:link--chip={chip}>{label}</span>
{/if}

<style>
  /* Every look is a target of at least 24 px, in both directions (components.adoc, States). */
  .link {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    min-width: 24px;
    min-height: 24px;
    border-radius: var(--radius-sm);
    cursor: pointer;
  }

  .link:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .link--text {
    color: inherit;
    text-decoration: none;
  }

  .link--text:hover {
    text-decoration: underline;
  }

  /*
   * The text look in running text (directly inside a p, li, dd, td, figcaption or blockquote) takes
   * the size of its line and is exempt from the target size (components.adoc, States).
   */
  :global(:is(p, li, dd, td, figcaption, blockquote)) > .link--text {
    text-decoration: underline;
    min-width: 0;
    min-height: 0;
  }

  .link--chip {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    padding: var(--space-1) var(--space-2);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
    text-decoration: none;
  }

  .link--chip:hover {
    color: var(--color-text-primary);
  }

  /* A phone gets the 44 px target, except for the text look in running text. */
  @media (max-width: 767px) {
    .link {
      min-width: 44px;
      min-height: 44px;
    }

    :global(:is(p, li, dd, td, figcaption, blockquote)) > .link--text {
      min-width: 0;
      min-height: 0;
    }
  }
</style>
