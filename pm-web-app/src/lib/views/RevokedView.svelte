<script lang="ts">
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import MascotFigure from '../components/MascotFigure.svelte';
  import { runtimeStore } from '../stores/runtime';

  /**
   * The page a revoked device lands on. The `access-ended` figure stands beside the text, quieter
   * than the pairing page's hero and never above the text; this is the only surface of that pose.
   */

  /** The figure's height beside the text, within the design's 160–200 px. */
  const FIGURE_HEIGHT = 180;

  $: info = $runtimeStore.revokedInfo;
  $: reasonKey = info?.reason || $runtimeStore.revokedReason || 'device_revoked';
  $: by = info?.revoked_by || 'the terminal client';
  // The runtime ships this value display-ready (`today at 14:10`); it is shown as it arrives,
  // like the other fields the wire already formats.
  $: time = info?.revoked_at ?? '';

  function handlePair() {
    runtimeStore.update((s) => ({
      ...s,
      isRevoked: false,
      revokedInfo: null,
      currentRoute: 'pair',
    }));
  }
</script>

<div class="revoked-view" data-testid="revoked-view">
  <MascotFigure pose="access-ended" height={FIGURE_HEIGHT} />
  <div class="revoked-card">
    <h1 class="revoked-title">{$t('devices.revoked.title')}</h1>
    <hr class="revoked-divider" />

    <p class="revoked-reason" data-testid="revoked-reason">
      {$t('devices.revoked.reason', { reason: reasonKey, by, time })}
    </p>

    <p class="revoked-how-back">
      {$t('devices.revoked.how_back')}
    </p>

    <div class="revoked-actions">
      <Button variant="primary" on:click={handlePair} data-testid="pair-browser-button">
        {$t('devices.revoked.pair')}
      </Button>
    </div>
  </div>
</div>

<style>
  .revoked-view {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-6);
    padding: var(--space-4);
    background-color: var(--color-bg-base);
  }

  .revoked-card {
    width: 100%;
    max-width: 600px;
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-8);
    box-shadow: var(--shadow-md);
  }

  .revoked-title {
    font-size: var(--text-xl);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-4) 0;
  }

  .revoked-divider {
    border: none;
    border-top: 1px solid var(--color-border);
    margin: 0 0 var(--space-4) 0;
  }

  .revoked-reason {
    font-size: var(--text-base);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-4) 0;
    line-height: 1.5;
  }

  .revoked-how-back {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-6) 0;
    line-height: 1.5;
  }

  .revoked-actions {
    display: flex;
    justify-content: flex-end;
  }
</style>
