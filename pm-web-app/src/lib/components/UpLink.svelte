<script lang="ts">
  import { openAddress } from '../stores/runtime';
  import { formatAddress } from '../routes';
  import type { Address } from '../routes';
  import { t } from '../i18n';
  import KindMark, { KIND_WORD_KEYS, type Kind } from './KindMark.svelte';

  interface Props {
    kind: Kind;
    title: string;
    address: Address;
  }

  let { kind, title, address }: Props = $props();

  const href = $derived(formatAddress(address) ?? '#');
  const accessibleName = $derived(
    $t('web.nav.up', {
      kind: $t(KIND_WORD_KEYS[kind]),
      name: title,
    }),
  );

  function handleClick(event: MouseEvent): void {
    event.preventDefault();
    openAddress(address);
  }
</script>

<a
  class="up-link"
  data-testid="up-link"
  {href}
  aria-label={accessibleName}
  title={accessibleName}
  onclick={handleClick}
>
  <KindMark {kind} wordHidden />
  <span class="up-link__title">{title}</span>
</a>

<style>
  .up-link {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--color-text-secondary);
    text-decoration: none;
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
  }

  .up-link:hover {
    color: var(--color-text-primary);
    text-decoration: underline;
  }

  .up-link__title {
    color: var(--color-text-primary);
  }
</style>
