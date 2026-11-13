<script lang="ts" module>
  /**
   * A C0 control character, in front of the address or inside it. A URL parser strips some of them
   * before it reads an address, so an address that carries one is no address a person typed.
   */
  const CONTROL_CHARACTER = /[\u0000-\u001F\u007F]/;

  /**
   * A web address: `http` or `https`, its two slashes, and then a character that is no slash, no
   * backslash and no white space.
   *
   * A special scheme skips every slash and backslash after it (WHATWG URL spec, "special authority
   * ignore slashes state"), so `https:host`, `https:/host`, `https:///host` and `https:\\host` all
   * name the host `host`. Only the canonical spelling is admitted: in the others what a reader sees
   * is not the address the browser opens.
   */
  const WEB_ADDRESS = /^https?:\/\/[^/\\\s]/i;

  /**
   * @param href the address a caller hands in
   * @returns whether, without the white space around it, it is an absolute http or https URL
   *   written with exactly the two slashes right after the scheme. `https:host`, `https:/host`,
   *   `https:///host` and `https:\\host` are refused although a URL parser reads each of them as
   *   the host `host`: their visible text and their destination would disagree. A C0 control
   *   character is refused wherever it stands, a leading tab or line feed included; only trailing
   *   white space is tolerated, because it is invisible and changes no address.
   */
  export function isWebAddress(href: string): boolean {
    if (CONTROL_CHARACTER.test(href.trimEnd())) {
      return false;
    }
    const address = href.trim();
    if (!WEB_ADDRESS.test(address)) {
      return false;
    }
    try {
      const parsed = new URL(address);
      return parsed.protocol === 'http:' || parsed.protocol === 'https:';
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

  /** The address that was checked and is rendered: the one without the white space around it. */
  const address = $derived(href.trim());
  const safe = $derived(isWebAddress(href));
</script>

{#if safe}
  <a href={address} target="_blank" rel="noopener noreferrer" class="link external-link" class:link--text={!chip} class:link--chip={chip}>
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
