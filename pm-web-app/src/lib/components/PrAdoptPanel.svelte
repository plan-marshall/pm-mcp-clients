<script lang="ts">
  import { t } from '../i18n';
  import { apiClient, type PullRequestObservation, type PrAdoptResult } from '../api/client';

  export let projectId = '';
  export let onAdopted: (result: PrAdoptResult) => void = () => {};

  let reference = '';
  let observation: PullRequestObservation | null = null;
  let accepted = false;
  let busy = false;
  let error = '';

  $: flagged = observation !== null && observation.screening !== 'clean';
  $: blocked =
    observation !== null &&
    (!observation.writable || observation.state !== 'open' || observation.rate_limit.limited);
  $: canAdopt = observation !== null && !blocked && (!flagged || accepted);

  async function observe(): Promise<void> {
    busy = true;
    error = '';
    observation = null;
    accepted = false;
    try {
      observation = await apiClient.observePullRequest(projectId, reference.trim());
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }

  async function adopt(): Promise<void> {
    busy = true;
    error = '';
    try {
      const result = await apiClient.adoptPullRequest({
        project_id: projectId,
        pr_url: reference.trim(),
        screening_bypass_reason: flagged && accepted ? 'accepted in the web app' : undefined,
      });
      onAdopted(result);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }
</script>

<div class="pr-adopt" data-testid="pr-adopt">
  <label class="pr-adopt__field">
    <span>{$t('git_sources.pr.field')}</span>
    <input type="text" data-testid="pr-reference-input" bind:value={reference} />
  </label>

  <button
    type="button"
    data-testid="pr-observe"
    on:click={observe}
    disabled={busy || reference.trim().length === 0}
  >
    {$t('git_sources.pr.observe')}
  </button>

  {#if observation}
    <div class="pr-adopt__card" data-testid="pr-observation">
      <p class="pr-adopt__headline">
        {$t('git_sources.pr.observation', {
          title: observation.title,
          author: observation.author,
          branch: observation.branch,
        })}
      </p>
      <p class="pr-adopt__files" data-testid="pr-files">
        {$t('git_sources.pr.files_untrusted', { count: observation.changed_files.length })}
      </p>
      <p class="pr-adopt__screening badge badge--{observation.screening}" data-testid="pr-screening">
        {$t('git_sources.screening.' + observation.screening)}
      </p>

      {#if observation.state !== 'open'}
        <p class="pr-adopt__warning" data-testid="pr-not-open">
          {$t('git_sources.pr.not_open')}
        </p>
      {/if}
      {#if !observation.writable}
        <p class="pr-adopt__warning" data-testid="pr-not-writable">
          {$t('git_sources.pr.head_not_writable')}
        </p>
      {/if}
      {#if observation.rate_limit.limited}
        <p class="pr-adopt__warning" data-testid="pr-rate-limited">
          {$t('git_sources.pr.rate_limited', { time: observation.rate_limit.reset_at || '' })}
        </p>
      {/if}

      {#if flagged}
        <p class="pr-adopt__flagged" data-testid="pr-flagged-title">
          {$t('git_sources.pr.flagged.title')}
        </p>
        <label class="pr-adopt__accept">
          <input type="checkbox" data-testid="pr-accept-screening" bind:checked={accepted} />
          <span>
            {observation.screening === 'flagged'
              ? $t('git_sources.pr.flagged.accept')
              : $t('git_sources.pr.unscreened.accept')}
          </span>
        </label>
      {/if}
    </div>
  {/if}

  {#if error}
    <p class="pr-adopt__error" data-testid="pr-error">{$t('git_sources.error', { cause: error })}</p>
  {/if}

  <button
    type="button"
    data-testid="pr-adopt-submit"
    on:click={adopt}
    disabled={busy || !canAdopt}
  >
    {$t('git_sources.pr.adopt.action')}
  </button>
</div>

<style>
  .pr-adopt {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .pr-adopt__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .pr-adopt__field input {
    min-height: 44px;
    padding: var(--space-1, 0.25rem);
  }

  .pr-adopt button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
    align-self: flex-start;
  }

  .pr-adopt__card {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
    border: 1px solid var(--color-border, #d1d5db);
    border-radius: var(--radius-md, 0.5rem);
    padding: var(--space-3, 0.75rem);
  }

  .pr-adopt__warning {
    color: var(--color-warning, #92640b);
  }

  .pr-adopt__flagged {
    color: var(--color-warning, #92640b);
  }

  .pr-adopt__error {
    color: var(--color-danger, #b3261e);
  }

  .pr-adopt__accept {
    display: flex;
    align-items: center;
    gap: var(--space-2, 0.5rem);
    min-height: 44px;
  }

  @media (max-width: 480px) {
    .pr-adopt button {
      align-self: stretch;
    }
  }
</style>
