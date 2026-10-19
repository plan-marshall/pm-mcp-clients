<script lang="ts">
  import { t } from '../i18n';
  import { apiClient, type ConfinedFileList, type ConfinedFile } from '../api/client';
  import { failureMessage } from '../start/sourceLabels';

  export let open = false;
  export let projectId = '';
  export let onClose: () => void = () => {};
  export let onAdd: (paths: string[]) => void = () => {};

  let root: 'checkout' | 'home' = 'checkout';
  let path = '';
  let listing: ConfinedFileList | null = null;
  let selected: string[] = [];
  let loading = false;
  let error = '';
  let loadedKey = '';

  function localHost(): boolean {
    if (typeof window === 'undefined') return true;
    return ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(window.location.hostname);
  }

  $: loopback = localHost();

  async function load(): Promise<void> {
    if (!open || !projectId) return;
    const key = `${projectId}|${root}|${path}`;
    if (key === loadedKey) return;
    loadedKey = key;
    loading = true;
    error = '';
    try {
      listing = await apiClient.listProjectFiles(projectId, root, path);
      selected = [];
    } catch (e) {
      error = e instanceof Error ? e.message : '';
      listing = null;
    } finally {
      loading = false;
    }
  }

  function chooseRoot(next: 'checkout' | 'home'): void {
    if (next === 'home' && !loopback) return;
    root = next;
    path = '';
    loadedKey = '';
    void load();
  }

  function enter(entry: ConfinedFile): void {
    if (!entry.directory) return;
    path = entry.path;
    loadedKey = '';
    void load();
  }

  function up(): void {
    if (!listing?.parent) return;
    path = listing.parent === '.' ? '' : listing.parent;
    loadedKey = '';
    void load();
  }

  function toggle(entry: ConfinedFile): void {
    if (!entry.readable || entry.refusal) return;
    selected = selected.includes(entry.path)
      ? selected.filter((candidate) => candidate !== entry.path)
      : [...selected, entry.path];
  }

  function reasonOf(entry: ConfinedFile): string {
    return entry.refusal ? failureMessage(entry.refusal).key : '';
  }

  function add(): void {
    if (selected.length === 0) return;
    onAdd([...selected]);
    selected = [];
  }

  function typesOf(): string {
    const counts = new Map<string, number>();
    for (const entry of listing?.entries ?? []) {
      if (entry.directory) continue;
      const type = entry.name.includes('.') ? (entry.name.split('.').pop() ?? '') : '';
      counts.set(type, (counts.get(type) ?? 0) + 1);
    }
    return [...counts.entries()]
      .filter(([type]) => type)
      .map(([type, count]) => `${count} ${type}`)
      .join(', ');
  }

  $: if (open && projectId) {
    const key = `${projectId}|${root}|${path}`;
    if (key !== loadedKey && !loading) {
      void load();
    }
  }
</script>

{#if open}
  <div class="file-picker" data-testid="confined-file-picker" role="dialog" aria-modal="true">
    <div class="file-picker__panel">
      <h2 class="file-picker__title">{$t('start.source.documents')}</h2>

      <div class="file-picker__roots" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={root === 'checkout'}
          class:active={root === 'checkout'}
          data-testid="picker-root-checkout"
          on:click={() => chooseRoot('checkout')}
        >
          {$t('start.documents.root_checkout')}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={root === 'home'}
          class:active={root === 'home'}
          data-testid="picker-root-home"
          disabled={!loopback}
          title={!loopback ? $t('start.documents.loopback_only') : undefined}
          on:click={() => chooseRoot('home')}
        >
          {$t('start.documents.root_home')}
        </button>
      </div>

      {#if listing?.parent}
        <button type="button" class="file-picker__up" data-testid="picker-up" on:click={up}>
          {$t('web.common.back')}
        </button>
      {/if}

      {#if loading}
        <p class="file-picker__status" data-testid="picker-loading">
          {$t('start.state.checking')}
        </p>
      {:else if error}
        <p class="file-picker__error" data-testid="picker-error">{error}</p>
        <button
          type="button"
          class="file-picker__up"
          data-testid="picker-retry"
          on:click={() => {
            loadedKey = '';
            void load();
          }}
        >
          {$t('start.source.retry')}
        </button>
      {:else}
        <ul class="file-picker__list">
          {#each listing?.entries ?? [] as entry (entry.path)}
            <li class="file-picker__row" data-testid={`picker-row-${entry.path}`}>
              <input
                type="checkbox"
                data-testid={`picker-check-${entry.path}`}
                checked={selected.includes(entry.path)}
                disabled={!entry.readable || Boolean(entry.refusal)}
                aria-label={entry.name}
                on:change={() => toggle(entry)}
              />
              {#if entry.directory}
                <button
                  type="button"
                  class="file-picker__name file-picker__name--dir"
                  data-testid={`picker-enter-${entry.path}`}
                  on:click={() => enter(entry)}
                >
                  {entry.name}
                </button>
              {:else}
                <span
                  class="file-picker__name"
                  title={reasonOf(entry) ? $t(reasonOf(entry)) : entry.name}
                >
                  {entry.name}
                </span>
              {/if}
            </li>
          {/each}
        </ul>
        <p class="file-picker__summary" data-testid="picker-summary">
          {$t('start.documents.directory', {
            count: listing?.entries.filter((entry) => !entry.directory).length ?? 0,
            types: typesOf(),
          })}
        </p>
      {/if}

      <p class="file-picker__hint">{$t('start.documents.loopback_only')}</p>

      <div class="file-picker__actions">
        <button
          type="button"
          data-testid="picker-add"
          disabled={selected.length === 0}
          on:click={add}
        >
          {$t('start.documents.add')}
        </button>
        <button type="button" data-testid="picker-close" on:click={onClose}>
          {$t('web.common.close')}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .file-picker {
    position: fixed;
    inset: 0;
    z-index: 60;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-3, 0.75rem);
    background-color: rgb(0 0 0 / 0.4);
  }

  .file-picker__panel {
    width: 100%;
    max-width: 40rem;
    max-height: 90vh;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
    padding: var(--space-4, 1rem);
    background-color: var(--color-bg-surface, #fff);
    border-radius: var(--radius-lg, 0.75rem);
  }

  .file-picker__title {
    margin: 0;
    font-size: var(--text-lg, 1.125rem);
  }

  .file-picker__roots,
  .file-picker__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .file-picker__roots button.active {
    font-weight: 700;
    text-decoration: underline;
  }

  .file-picker__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
    max-height: 24rem;
    overflow-y: auto;
  }

  .file-picker__row {
    display: flex;
    align-items: center;
    gap: var(--space-2, 0.5rem);
    min-height: 44px;
  }

  .file-picker__name {
    overflow-wrap: anywhere;
  }

  .file-picker__name--dir {
    font-weight: 600;
    text-decoration: underline;
    cursor: pointer;
  }

  .file-picker__hint,
  .file-picker__summary,
  .file-picker__status {
    margin: 0;
    color: var(--color-text-muted, #6b7280);
    font-size: var(--text-sm, 0.875rem);
  }

  .file-picker__error {
    margin: 0;
    color: var(--color-danger, #b3261e);
  }

  .file-picker__actions button,
  .file-picker__roots button,
  .file-picker__up {
    min-height: 44px;
    padding: var(--space-1, 0.25rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  @media (max-width: 480px) {
    .file-picker__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
