<script lang="ts">
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import { pairBrowser, runtimeStore } from '../stores/runtime';

  let codeInput = '';
  let isPairing = false;
  let errorMessage = '';
  let copiedCmd: string | null = null;

  $: isCodeValid = codeInput.replace(/[^A-Za-z0-9]/g, '').length >= 8;

  const cmdLocal = 'pm-operator web open --pair';
  const cmdRemote = 'pm-operator web pair';

  async function submitPairing(codeToPair: string) {
    if (!codeToPair || isPairing) return;
    isPairing = true;
    errorMessage = '';

    try {
      await pairBrowser(codeToPair);
    } catch {
      errorMessage = $t('web.pair.page.invalid');
    } finally {
      isPairing = false;
    }
  }

  function handleManualSubmit(e: Event) {
    e.preventDefault();
    submitPairing(codeInput);
  }

  async function copyToClipboard(text: string, id: string) {
    try {
      await navigator.clipboard.writeText(text);
      copiedCmd = id;
      setTimeout(() => {
        copiedCmd = null;
      }, 2000);
    } catch {
      // Fallback
    }
  }
</script>

<div class="pair-card">
  {#if $runtimeStore.isRevoked}
    <div class="revoked-banner" role="alert">
      <h2 class="revoked-banner__title">{$t('web.revoked.page.title')}</h2>
      <p class="revoked-banner__reason">
        {$runtimeStore.revokedReason || $t('web.revoked.reason.unknown')}
      </p>
    </div>
  {/if}

  {#if isPairing}
    <div class="pair-card__loading" role="status">
      <Spinner size="2rem" />
      <p class="pair-card__loading-text">{$t('web.pair.page.pairing')}</p>
    </div>
  {:else}
    <div class="pair-card__header">
      <h1 class="pair-card__title">{$t('web.pair.page.title')}</h1>
      <p class="pair-card__intro">{$t('web.pair.page.intro')}</p>
    </div>

    {#if errorMessage}
      <div class="alert alert--danger" role="alert">
        {errorMessage}
      </div>
    {/if}

    <form class="pair-card__form" on:submit={handleManualSubmit}>
      <div class="form-group">
        <label for="pairing-code" class="form-label">
          {$t('web.pair.page.code')}
        </label>
        <input
          id="pairing-code"
          type="text"
          class="form-input mono"
          placeholder="KXQB-MRTN"
          maxlength="12"
          autocomplete="off"
          autocorrect="off"
          autocapitalize="characters"
          spellcheck="false"
          bind:value={codeInput}
        />
      </div>

      <div class="pair-card__help">
        <p class="pair-card__help-title">{$t('web.pair.page.get_code')}</p>
        <div class="cmd-row">
          <span class="cmd-row__desc">{$t('web.pair.page.this_machine')}</span>
          <code class="cmd-row__code">{cmdLocal}</code>
          <Button type="button" variant="ghost" on:click={() => copyToClipboard(cmdLocal, 'local')}>
            {copiedCmd === 'local' ? $t('web.common.copied') : $t('web.common.copy')}
          </Button>
        </div>
        <div class="cmd-row">
          <span class="cmd-row__desc">{$t('web.pair.page.other_computer')}</span>
          <code class="cmd-row__code">{cmdRemote}</code>
          <Button
            type="button"
            variant="ghost"
            on:click={() => copyToClipboard(cmdRemote, 'remote')}
          >
            {copiedCmd === 'remote' ? $t('web.common.copied') : $t('web.common.copy')}
          </Button>
        </div>
      </div>

      <div class="pair-card__footer">
        <Button type="submit" variant="primary" disabled={!isCodeValid} loading={isPairing}>
          {$t('web.pair.page.action')}
        </Button>
      </div>
    </form>
  {/if}
</div>

<style>
  .pair-card {
    max-width: 600px;
    width: 100%;
    margin: var(--space-10) auto;
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-md);
    overflow: hidden;
  }

  .revoked-banner {
    background-color: var(--color-danger-subtle);
    border-bottom: 1px solid var(--color-danger-border);
    padding: var(--space-4) var(--space-6);
  }

  .revoked-banner__title {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-danger);
    margin-bottom: var(--space-1);
  }

  .revoked-banner__reason {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .pair-card__header {
    padding: var(--space-6) var(--space-6) var(--space-4);
  }

  .pair-card__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: var(--space-2);
  }

  .pair-card__intro {
    font-size: var(--text-base);
    color: var(--color-text-secondary);
  }

  .alert {
    margin: 0 var(--space-6) var(--space-4);
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
  }

  .alert--danger {
    background-color: var(--color-danger-subtle);
    border: 1px solid var(--color-danger-border);
    color: var(--color-danger);
  }

  .pair-card__form {
    padding: 0 var(--space-6) var(--space-6);
  }

  .form-group {
    margin-bottom: var(--space-6);
  }

  .form-label {
    display: block;
    font-size: var(--text-sm);
    font-weight: 600;
    margin-bottom: var(--space-2);
    color: var(--color-text-primary);
  }

  .form-input {
    width: 100%;
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-lg);
    letter-spacing: 0.1em;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .pair-card__help {
    margin-bottom: var(--space-6);
    padding: var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .pair-card__help-title {
    font-size: var(--text-sm);
    font-weight: 600;
    margin-bottom: var(--space-3);
    color: var(--color-text-secondary);
  }

  .cmd-row {
    display: grid;
    grid-template-columns: 140px 1fr auto;
    align-items: center;
    gap: var(--space-3);
    margin-bottom: var(--space-2);
    font-size: var(--text-xs);
  }

  .cmd-row:last-child {
    margin-bottom: 0;
  }

  .cmd-row__desc {
    color: var(--color-text-muted);
  }

  .cmd-row__code {
    background-color: var(--color-bg-surface);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    border: 1px solid var(--color-border-subtle);
    overflow-x: auto;
    white-space: nowrap;
  }

  .pair-card__footer {
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  .pair-card__loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--space-4);
    padding: var(--space-10) var(--space-6);
  }

  .pair-card__loading-text {
    font-size: var(--text-base);
    color: var(--color-text-secondary);
  }

  @media (max-width: 640px) {
    .cmd-row {
      grid-template-columns: 1fr auto;
    }
    .cmd-row__desc {
      grid-column: 1 / -1;
    }
  }
</style>
