<script lang="ts" module>
  /** How a link is drawn: text, the secondary Button look, or the chip look. */
  export type LinkLook = 'text' | 'button' | 'chip';

  /** A stand-in origin the path of the app is resolved against; it is never rendered. */
  const APP_ORIGIN = 'http://app.invalid';

  /**
   * A second slash or a backslash right after the first one, with only the tabs, carriage returns and
   * line feeds between them that a URL parser strips before it reads an address. Such an address
   * supplies its own host (WHATWG URL spec, authority); it is no path of the app.
   */
  const AUTHORITY_FORM = /^\/[\t\n\r]*[/\\]/;

  /**
   * @param href the address a caller hands in
   * @returns whether it is a path of the app: it starts with `/`, names no host of its own and,
   *   resolved the way a browser resolves it, stays on the origin of the app. A `javascript:` or
   *   `data:` address, a host written `//host` or `/\host` (the placeholder host included, which the
   *   origin comparison alone would let through), and one hidden behind a tab or a line break are all
   *   refused.
   */
  export function isAppPath(href: string): boolean {
    const address = href.trim();
    if (!address.startsWith('/') || AUTHORITY_FORM.test(address)) {
      return false;
    }
    try {
      return new URL(address, APP_ORIGIN).origin === APP_ORIGIN;
    } catch {
      return false;
    }
  }
</script>

<script lang="ts">
  import type { Snippet } from 'svelte';

  /**
   * An `<a href>` to a place of the app.
   *
   * The text look takes the colour of the text around it: underlined in a sentence, and standing
   * alone underlined only on hover. The button look is the secondary `Button`'s, the chip look that
   * of a reference chip; navigation stays a link in every look. Only a path of the app (`/…`) becomes
   * an anchor: a link whose URL is not known, and any address that is not a path of the app (another
   * scheme such as `javascript:`, or another host), is drawn as its label without an anchor, never
   * as `href="#"`; a place outside the app is an `ExternalLink`.
   */
  interface Props {
    /** The place the link goes to. */
    href: string;
    /** The destination, named as a noun. */
    label: string;
    /** The look; `text` by default. */
    look?: LinkLook;
    /** An icon before the label. */
    icon?: Snippet;
  }

  let { href, label, look = 'text', icon }: Props = $props();

  /** The address that was checked and is rendered: the one without the white space around it. */
  const address = $derived(href.trim());
  const known = $derived(isAppPath(address));
</script>

{#if known}
  <a href={address} class="link link--{look}">
    {#if icon}<span class="link__icon">{@render icon()}</span>{/if}
    <span class="link__label">{label}</span>
  </a>
{:else}
  <span class="link-text">{label}</span>
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

  .link__icon {
    display: inline-flex;
  }

  .link--text {
    color: inherit;
    text-decoration: none;
  }

  .link--text:hover {
    text-decoration: underline;
  }

  /*
   * In running text a link is underlined at rest, so it is found without a hover. The text look in
   * running text (directly inside a p, li, dd, td, figcaption or blockquote) takes the size of its
   * line and is exempt from the target size (components.adoc, States).
   */
  :global(:is(p, li, dd, td, figcaption, blockquote)) > .link--text {
    text-decoration: underline;
    min-width: 0;
    min-height: 0;
  }

  .link--button {
    gap: var(--space-2);
    min-height: 34px;
    padding: var(--space-2) var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-md);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    font-weight: 500;
    line-height: 1.25;
    text-decoration: none;
    white-space: nowrap;
  }

  .link--button:hover {
    background-color: var(--color-bg-subtle);
  }

  .link--chip {
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
