<script lang="ts">
  import { t } from '../i18n';
  import { apiClient, type PullRequestObservation, type PrAdoptResult } from '../api/client';
  import { actFailureText } from '../api/errors';
  import Button from './Button.svelte';
  import TextField from './TextField.svelte';
  import CheckField from './CheckField.svelte';
  import PrChip from './PrChip.svelte';

  /**
   * Adopting a pull request that already exists (`shared/starting-work.adoc` § #pull-request): the
   * reference is a `TextField` that takes a number or a URL, the observed pull request is a `PrChip`,
   * and the acts are the design's `Button`s. A flagged pull request is adopted only once the
   * operator accepted its screening.
   */
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
      error = actFailureText(e, $t('error.unknown.fix'));
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
      error = actFailureText(e, $t('error.unknown.fix'));
    } finally {
      busy = false;
    }
  }
</script>

<div class="pr-adopt" data-testid="pr-adopt">
  <TextField
    label={$t('git_sources.pr.field')}
    testid="pr-reference-input"
    bind:value={reference}
    autocomplete="off"
  />

  <div>
    <Button
      variant="secondary"
      data-testid="pr-observe"
      disabled={busy || reference.trim().length === 0}
      on:click={observe}
    >
      {$t('git_sources.pr.observe')}
    </Button>
  </div>

  {#if observation}
    <div class="pr-adopt__card" data-testid="pr-observation">
      <PrChip number={observation.number} href={observation.url} />
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
      <p
        class="pr-adopt__screening badge badge--{observation.screening}"
        data-testid="pr-screening"
      >
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
        <CheckField
          label={observation.screening === 'flagged'
            ? $t('git_sources.pr.flagged.accept')
            : $t('git_sources.pr.unscreened.accept')}
          testid="pr-accept-screening"
          bind:checked={accepted}
        />
      {/if}
    </div>
  {/if}

  {#if error}
    <p class="pr-adopt__error" data-testid="pr-error">
      {$t('git_sources.error', { cause: error })}
    </p>
  {/if}

  <div>
    <Button
      variant="secondary"
      data-testid="pr-adopt-submit"
      disabled={busy || !canAdopt}
      on:click={adopt}
    >
      {$t('git_sources.pr.adopt.action')}
    </Button>
  </div>
</div>

<style>
  .pr-adopt {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .pr-adopt__card {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    align-items: flex-start;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: var(--space-3);
  }

  .pr-adopt__warning {
    color: var(--color-warning-text);
  }

  .pr-adopt__flagged {
    color: var(--color-warning-text);
  }

  .pr-adopt__error {
    color: var(--color-danger-text);
  }
</style>
