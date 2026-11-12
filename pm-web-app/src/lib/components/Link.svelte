<script lang="ts" module>
  /** How a link is drawn: text, the secondary Button look, or the chip look. */
  export type LinkLook = 'text' | 'button' | 'chip';
</script>

<script lang="ts">
  import type { Snippet } from 'svelte';

  /**
   * An `<a href>` to a place of the app.
   *
   * The text look takes the colour of the text around it: underlined in a sentence, and standing
   * alone underlined only on hover. The button look is the secondary `Button`'s, the chip look that
   * of a reference chip; navigation stays a link in every look. A link whose URL is not known is
   * drawn as its label without an anchor, never as `href="#"`.
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

  const known = $derived(href.trim() !== '' && href.trim() !== '#');
</script>

{#if known}
  <a {href} class="link link--{look}">
    {#if icon}<span class="link__icon">{@render icon()}</span>{/if}
    <span class="link__label">{label}</span>
  </a>
{:else}
  <span class="link-text">{label}</span>
{/if}

<style>
  .link {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
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

  /* In running text a link is underlined at rest, so it is found without a hover. */
  :global(:is(p, li, dd, td, figcaption, blockquote)) > .link--text {
    text-decoration: underline;
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

  /* A phone gets the 44 px target. */
  @media (max-width: 767px) {
    .link--button {
      min-height: 44px;
    }
  }
</style>
