<script lang="ts">
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import MascotFigure, { FIGURE_HEIGHT } from '../components/MascotFigure.svelte';
  import { runtimeStore } from '../stores/runtime';

  /**
   * The page a revoked device lands on. The `access-ended` figure stands beside the text, quieter
   * than the pairing page's hero and never above the text; this is the only surface of that pose.
   * Where the page is too narrow to hold both, the figure is hidden and the card takes the width.
   */

  $: info = $runtimeStore.revokedInfo;
  $: reasonKey = info?.reason ?? $runtimeStore.revokedReason ?? '';
  $: by = info?.revoked_by || '';
  // The runtime ships this value display-ready (`today at 14:10`); it is shown as it arrives,
  // like the other fields the wire already formats.
  $: time = info?.revoked_at ?? '';

  /**
   * Resolves the reason sentence following entry.adoc (§ #revoked) and web.revoked.reason.*.
   */
  function resolveReason(
    revokedInfo: typeof info,
    fallbackKey: string,
    revokedBy: string,
    revokedTime: string,
    translate: typeof $t,
  ): string {
    const rawReason = revokedInfo?.reason ?? (fallbackKey || '');
    if (!rawReason || rawReason === 'unknown' || rawReason === 'web.revoked.reason.unknown') {
      return translate('web.revoked.reason.unknown');
    }

    const device =
      revokedInfo?.revoked_by ||
      (revokedInfo as unknown as Record<string, string>)?.device ||
      revokedBy ||
      '';
    const tVal =
      revokedInfo?.revoked_at ??
      (revokedInfo as unknown as Record<string, string>)?.time ??
      revokedTime;

    if (rawReason === 'device' || rawReason === 'web.revoked.reason.device') {
      if (!device) {
        return translate('web.revoked.reason.unknown');
      }
      return translate('web.revoked.reason.device', { device, time: tVal });
    }
    if (rawReason === 'terminal' || rawReason === 'web.revoked.reason.terminal') {
      return translate('web.revoked.reason.terminal', { time: tVal });
    }
    if (rawReason === 'others' || rawReason === 'all_others' || rawReason === 'web.revoked.reason.others') {
      if (!device) {
        return translate('web.revoked.reason.unknown');
      }
      return translate('web.revoked.reason.others', { device, time: tVal });
    }
    if (rawReason === 'self' || rawReason === 'this_device' || rawReason === 'web.revoked.reason.self') {
      return translate('web.revoked.reason.self', { time: tVal });
    }
    if (rawReason === 'idle' || rawReason === 'web.revoked.reason.idle') {
      return translate('web.revoked.reason.idle', { days: 30 });
    }
    if (rawReason === 'device_revoked') {
      if (!device) {
        return translate('web.revoked.reason.unknown');
      }
      return translate('devices.revoked.reason', { reason: rawReason, by: device, time: tVal });
    }
    if (rawReason === 'device_expired') {
      return translate('devices.revoked.reason', { reason: rawReason, by: device, time: tVal });
    }
    return translate('web.revoked.reason.unknown');
  }

  $: reasonText = resolveReason(info, reasonKey, by, time, $t);

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
      {reasonText}
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
  /*
   * The page is the query container: it stretches across the view area, so its width is the page's
   * and never the content's, and the narrow rule below follows the page, not the viewport the rail
   * also takes from.
   */
  .revoked-view {
    container: revoked-view / inline-size;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-6);
    padding: var(--space-4);
    background-color: var(--color-bg-base);
  }

  /*
   * Where the page cannot hold the figure beside a card that keeps its text and its button, the
   * figure is hidden, never stacked above the text.
   */
  @container revoked-view (max-width: 36rem) {
    .revoked-view :global(.mascot-figure) {
      display: none;
    }
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
