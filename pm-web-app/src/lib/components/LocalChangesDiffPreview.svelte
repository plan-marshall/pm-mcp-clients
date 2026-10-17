<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import { apiClient, type LocalChangesPreview, type LocalChangesCaptureResult } from '../api/client';

  export let projectId = '';
  export let onCaptured: (result: LocalChangesCaptureResult) => void = () => {};

  let preview: LocalChangesPreview | null = null;
  let confirmed = false;
  let busy = false;
  let error = '';

  onMount(load);

  async function load(): Promise<void> {
    busy = true;
    error = '';
    try {
      preview = await apiClient.previewLocalChanges(projectId);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }

  async function capture(): Promise<void> {
    busy = true;
    error = '';
    try {
      const result = await apiClient.captureLocalChanges(projectId);
      onCaptured(result);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }
</script>

<div class="local-changes" data-testid="local-changes">
  {#if preview}
    <p class="local-changes__summary" data-testid="local-preview">
      {$t('git_sources.local.preview', {
        count: preview.changed_files.length,
        branch: preview.branch || '?',
      })}
    </p>

    <ul class="local-changes__files" data-testid="local-files">
      {#each preview.changed_files as file (file.path)}
        <li class="local-changes__file" data-testid="local-file">
          <span class="local-changes__path">{file.path}</span>
          <span class="local-changes__add">{file.additions}</span>
          <span class="local-changes__del">{file.deletions}</span>
        </li>
      {/each}
    </ul>

    {#if preview.eligible}
      <p class="local-changes__eligible" data-testid="local-eligible">
        {$t('git_sources.local.eligible', { landing: preview.landing || '?' })}
      </p>
      <label class="local-changes__confirm">
        <input type="checkbox" data-testid="local-confirm" bind:checked={confirmed} />
        <span>{$t('git_sources.local.confirm')}</span>
      </label>
    {:else}
      <p class="local-changes__ineligible" data-testid="local-ineligible">
        {$t('git_sources.local.ineligible', { landing: preview.landing || '?' })}
      </p>
      <p class="local-changes__ineligible" data-testid="local-pm-wf-25">
        {$t('git_sources.local.pm_wf_25')}
      </p>
    {/if}
  {/if}

  {#if error}
    <p class="local-changes__error" data-testid="local-error">
      {$t('git_sources.error', { cause: error })}
    </p>
  {/if}

  <button
    type="button"
    data-testid="local-capture"
    on:click={capture}
    disabled={busy || !preview || !preview.eligible || !confirmed}
  >
    {$t('git_sources.local.capture')}
  </button>
</div>

<style>
  .local-changes {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .local-changes__files {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .local-changes__file {
    display: flex;
    justify-content: space-between;
    gap: var(--space-2, 0.5rem);
    font-family: monospace;
    font-size: var(--text-sm, 0.875rem);
  }

  .local-changes__path {
    overflow-wrap: anywhere;
  }

  .local-changes__add {
    color: var(--color-success, #1f7a3d);
  }

  .local-changes__del {
    color: var(--color-danger, #b3261e);
  }

  .local-changes__ineligible,
  .local-changes__error {
    color: var(--color-danger, #b3261e);
  }

  .local-changes__confirm {
    display: flex;
    align-items: center;
    gap: var(--space-2, 0.5rem);
    min-height: 44px;
  }

  .local-changes button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
    align-self: flex-start;
  }

  @media (max-width: 480px) {
    .local-changes button {
      align-self: stretch;
    }
  }
</style>
