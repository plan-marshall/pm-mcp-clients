<script lang="ts">
  import { t } from '../i18n';
  import type { SourceInput } from '../api/client';

  export let description = '';
  export let documents = '';
  export let issue = '';
  export let onChange: (sources: SourceInput[]) => void = () => {};

  const KIND_DESCRIPTION = 'description';
  const KIND_DOCUMENTS = 'documents';
  const KIND_ISSUE = 'issue';

  function buildSources(desc: string, docs: string, iss: string): SourceInput[] {
    const sources: SourceInput[] = [];
    if (desc.trim().length > 0) {
      sources.push({ kind: KIND_DESCRIPTION, location: desc.trim() });
    }
    for (const line of docs.split('\n')) {
      const path = line.trim();
      if (path.length > 0) {
        sources.push({ kind: KIND_DOCUMENTS, location: path });
      }
    }
    if (iss.trim().length > 0) {
      sources.push({ kind: KIND_ISSUE, location: iss.trim() });
    }
    return sources;
  }

  function emit(): void {
    onChange(buildSources(description, documents, issue));
  }

  $: sourceCount = buildSources(description, documents, issue).length;
</script>

<div class="source-intake" data-testid="source-intake">
  <label class="source-intake__field">
    <span>{$t('start.source.description')}</span>
    <textarea data-testid="source-description" rows="3" bind:value={description} on:input={emit}
    ></textarea>
  </label>

  <label class="source-intake__field">
    <span>{$t('start.source.documents')}</span>
    <textarea
      data-testid="source-documents"
      rows="2"
      placeholder={$t('start.field.documents')}
      bind:value={documents}
      on:input={emit}
    ></textarea>
  </label>

  <label class="source-intake__field">
    <span>{$t('start.source.issue')}</span>
    <input
      type="text"
      data-testid="source-issue"
      placeholder={$t('start.field.issue')}
      bind:value={issue}
      on:input={emit}
    />
  </label>

  <p class="source-intake__count" data-testid="source-count">
    {$t('start.sources.waits', { count: sourceCount })}
  </p>
</div>

<style>
  .source-intake {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .source-intake__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .source-intake__field textarea,
  .source-intake__field input {
    min-height: 44px;
    padding: var(--space-1, 0.25rem);
    font-family: inherit;
  }

  .source-intake__count {
    margin: 0;
    color: var(--color-text-muted);
  }

  @media (max-width: 480px) {
    .source-intake {
      width: 100%;
    }
  }
</style>
