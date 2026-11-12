<script lang="ts">
  import ExternalLink from './ExternalLink.svelte';
  import { t } from '../i18n';

  /**
   * A pull request: an `ExternalLink` in the chip look whose text is `common.pull_request.chip`
   * (`PR #214`). It opens the pull request at the provider and copies nothing. A pull request whose
   * address is not known is the same text in the chip look without a link, never `href="#"`.
   */
  interface Props {
    /** The pull request's number. */
    number: number;
    /** The pull request's address at the provider, when known. */
    href?: string;
  }

  let { number, href }: Props = $props();

  const text = $derived($t('common.pull_request.chip', { number }));
</script>

{#if href}
  <ExternalLink {href} label={text} chip />
{:else}
  <span class="pr-chip">{text}</span>
{/if}

<style>
  .pr-chip {
    display: inline-flex;
    align-items: center;
    padding: var(--space-1) var(--space-2);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
  }
</style>
