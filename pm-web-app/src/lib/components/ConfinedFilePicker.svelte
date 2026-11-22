<script lang="ts">
  import { t } from '../i18n';
  import { apiClient, type ConfinedFileList, type ConfinedFile } from '../api/client';
  import { failureMessage } from '../start/sourceLabels';
  import { actFailureText } from '../api/errors';
  import { isLoopbackSession } from '../utils/loopback';
  import Dialog from './Dialog.svelte';
  import Button from './Button.svelte';
  import TextField from './TextField.svelte';
  import CheckGroup, { type CheckOption } from './CheckGroup.svelte';

  /**
   * The picker of documents inside the project's checkout or the operator's home directory
   * (starting-work.adoc § #documents): the design `Dialog` with the two roots, the path the listing
   * shows, the directories to enter and the files as a `CheckGroup`. A file the runtime refuses is a
   * disabled option that says why; the home directory is a disabled act with its reason off the
   * loopback listener.
   */
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

  $: loopback = isLoopbackSession();

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
      error = actFailureText(e, $t('error.unknown.fix'));
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

  function retry(): void {
    loadedKey = '';
    void load();
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

  $: directories = (listing?.entries ?? []).filter((entry) => entry.directory);
  $: summary = $t('start.documents.directory', {
    count: (listing?.entries ?? []).filter((entry) => !entry.directory).length,
    types: typesOf(),
  });
  $: entryOptions = (listing?.entries ?? []).map((entry): CheckOption => {
    const message = entry.refusal ? failureMessage(entry.refusal) : null;
    return {
      value: entry.path,
      label: entry.name,
      disabled: !entry.readable || Boolean(entry.refusal),
      disabledReason: message ? $t(message.key, message.args) : undefined,
    };
  });

  $: if (open && projectId) {
    const key = `${projectId}|${root}|${path}`;
    if (key !== loadedKey && !loading) {
      void load();
    }
  }
</script>

<Dialog open={open} title={$t('start.source.documents')} {onClose} testid="confined-file-picker">
  <div class="file-picker__roots">
    <Button
      variant="secondary"
      data-testid="picker-root-checkout"
      aria-pressed={root === 'checkout'}
      on:click={() => chooseRoot('checkout')}
    >
      {$t('start.documents.root_checkout')}
    </Button>
    <Button
      variant="secondary"
      data-testid="picker-root-home"
      aria-pressed={root === 'home'}
      disabled={!loopback}
      disabledReason={!loopback ? $t('start.documents.loopback_only') : undefined}
      on:click={() => chooseRoot('home')}
    >
      {$t('start.documents.root_home')}
    </Button>
  </div>

  <TextField
    label={root === 'checkout' ? $t('start.documents.root_checkout') : $t('start.documents.root_home')}
    value={path || '/'}
    readonly
    mono
    testid="picker-path"
  />

  {#if listing?.parent}
    <div class="file-picker__up">
      <Button variant="ghost" data-testid="picker-up" on:click={up}>
        {$t('common.action.back')}
      </Button>
    </div>
  {/if}

  {#if loading}
    <p class="file-picker__status" data-testid="picker-loading">{$t('start.state.checking')}</p>
  {:else if error}
    <p class="file-picker__error" data-testid="picker-error">{error}</p>
    <div class="file-picker__up">
      <Button variant="secondary" data-testid="picker-retry" on:click={retry}>
        {$t('start.source.retry')}
      </Button>
    </div>
  {:else}
    {#if directories.length > 0}
      <ul class="file-picker__dirs">
        {#each directories as entry (entry.path)}
          <li>
            <Button
              variant="secondary"
              size="sm"
              data-testid={`picker-enter-${entry.path}`}
              on:click={() => enter(entry)}
            >
              {entry.name}
            </Button>
          </li>
        {/each}
      </ul>
    {/if}

    {#if entryOptions.length > 0}
      <CheckGroup
        legend={summary}
        options={entryOptions}
        values={selected}
        onchange={(values) => (selected = values)}
        testid="picker-files"
      />
    {:else}
      <p class="file-picker__summary" data-testid="picker-summary">{summary}</p>
    {/if}
  {/if}

  {#if loopback}
    <p class="file-picker__hint">{$t('start.documents.loopback_only')}</p>
  {/if}

  <svelte:fragment slot="footer">
    <Button variant="ghost" data-testid="picker-close" on:click={onClose}>
      {$t('common.action.close')}
    </Button>
    <Button
      variant="primary"
      data-testid="picker-add"
      disabled={selected.length === 0}
      on:click={add}
    >
      {$t('start.documents.add')}
    </Button>
  </svelte:fragment>
</Dialog>

<style>
  .file-picker__roots,
  .file-picker__up {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .file-picker__dirs {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .file-picker__hint,
  .file-picker__summary,
  .file-picker__status {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .file-picker__error {
    margin: 0;
    color: var(--color-danger-text);
  }
</style>
