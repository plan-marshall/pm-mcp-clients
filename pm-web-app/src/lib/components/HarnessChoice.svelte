<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';

  /**
   * ② Harnesses: choose the ones to enrol, one of them the default.
   *
   * Every found harness is listed with its version, its path and whether it can run workers;
   * what it reports of its tokens stands below it as information, never as a condition.
   */
  interface HarnessRow {
    harness_id: string;
    name: string;
    path?: string;
    version?: string;
    found: boolean;
    supported: boolean;
    worker_eligible: boolean;
    token_usage?: string;
    token_detail?: string;
    default?: boolean;
    reason?: string;
    /** The icon the runtime fetched from the maker, served by the runtime itself. */
    icon_url?: string;
  }

  export let harnesses: HarnessRow[] = [];
  export let busy = false;

  export let onCheck: (chosen: string[], defaultId: string) => void = () => {};

  const selectable = harnesses.filter((h) => h.found && h.supported && h.worker_eligible);
  const initialChosen = selectable.map((h) => h.harness_id);
  const recordedDefault = harnesses.find((h) => h.default && selectable.includes(h));

  let chosen: string[] = [...initialChosen];
  let defaultId = recordedDefault?.harness_id ?? selectable[0]?.harness_id ?? '';
  // A path the runtime serves but the browser cannot load falls back to the monogram, so a broken
  // icon never replaces the row's only mark.
  let failedIconPaths: Record<string, string> = {};

  function rememberIconFailure(harnessId: string, path: string): void {
    failedIconPaths = { ...failedIconPaths, [harnessId]: path };
  }

  function toggle(id: string): void {
    if (chosen.includes(id)) {
      chosen = chosen.filter((value) => value !== id);
      if (defaultId === id) {
        defaultId = chosen[0] ?? '';
      }
    } else {
      chosen = [...chosen, id];
      if (!defaultId) {
        defaultId = id;
      }
    }
  }

  function makeDefault(id: string): void {
    if (chosen.includes(id)) {
      defaultId = id;
    }
  }

  /**
   * The harness's fetched icon, where the runtime serves one from a same-origin path.
   *
   * An absolute URL is refused: the browser must never load an icon from a maker's server, so a
   * harness that carries one keeps its monogram instead. A backslash, a double slash and any
   * control character are refused for the same reason: the browser strips a tab, a newline or a
   * carriage return and normalises a backslash, so each of them can turn the value into another
   * origin's URL.
   *
   * @param row the harness row
   * @returns the path to draw, or an empty string when the row carries none
   */
  function harnessIconPath(row: HarnessRow): string {
    const path = row.icon_url?.trim() ?? '';
    // eslint-disable-next-line no-control-regex -- the control characters are what this refuses
    if (/[\u0000-\u001f\u007f\\]/.test(path) || path.startsWith('//') || !path.startsWith('/')) {
      return '';
    }
    return path;
  }

  /**
   * The monogram of a harness without a fetched icon: the initial letters of up to two words,
   * in the rounded square the harness icon specification draws.
   *
   * @param row the harness row
   * @returns one or two upper-case letters
   */
  function monogram(row: HarnessRow): string {
    const words = (row.name?.trim() || row.harness_id).trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) {
      return '?';
    }
    return words
      .slice(0, 2)
      .map((word) => [...word][0]?.toUpperCase() ?? '')
      .join('');
  }

  function tokenState(row: HarnessRow): string {
    if (row.token_usage === 'complete')
      return $t('onboard.harness.tokens.complete', { figures: '' });
    if (row.token_usage === 'partial') {
      return $t('onboard.harness.tokens.partial', { figures: row.token_detail ?? '' });
    }
    return $t('onboard.harness.tokens.not_reported');
  }

  $: defaultName = harnesses.find((h) => h.harness_id === defaultId)?.name ?? '';
  $: primaryLabel =
    chosen.length === 0
      ? $t('onboard.harness.choose.none')
      : $t('onboard.harness.choose.action', { count: chosen.length, name: defaultName });
</script>

<section class="choice-screen" data-testid="harness-choose-screen">
  <p class="choice-screen__label">{$t('onboard.harness.choose.found')}</p>

  <ul class="choice-screen__list">
    {#each harnesses as harness}
      {@const canChoose = harness.found && harness.supported && harness.worker_eligible}
      {@const iconPath = harnessIconPath(harness)}
      {@const iconFailed = failedIconPaths[harness.harness_id] === iconPath}
      <li
        class="choice-screen__row"
        class:choice-screen__row--disabled={!canChoose}
        data-harness={harness.harness_id}
      >
        <input
          type="checkbox"
          checked={chosen.includes(harness.harness_id)}
          disabled={!canChoose}
          aria-label={harness.name}
          onchange={() => toggle(harness.harness_id)}
          data-testid={`harness-toggle-${harness.harness_id}`}
        />

        {#if iconPath && !iconFailed}
          <img
            class="choice-screen__icon"
            src={iconPath}
            alt=""
            aria-hidden="true"
            data-testid={`harness-icon-${harness.harness_id}`}
            onerror={() => rememberIconFailure(harness.harness_id, iconPath)}
          />
          />
        {:else}
          <span
            class="choice-screen__monogram"
            aria-hidden="true"
            data-testid={`harness-monogram-${harness.harness_id}`}>{monogram(harness)}</span
          >
        {/if}

        <div class="choice-screen__row-main">
          <span class="choice-screen__row-title">
            {harness.name}
            {#if harness.version}<span class="choice-screen__row-meta">
                {harness.version}</span
              >{/if}
          </span>
          <span class="choice-screen__row-meta mono">{harness.path ?? ''}</span>
          <span class="choice-screen__row-meta">
            {#if !harness.found}
              {$t('onboard.harness.choose.not_found')}
            {:else if !harness.worker_eligible}
              {$t('onboard.harness.choose.no_workers')}
              {#if harness.reason}· {harness.reason}{/if}
            {:else}
              {$t('onboard.harness.choose.supported')}
            {/if}
          </span>
          {#if harness.found}
            <span class="choice-screen__row-meta">{tokenState(harness)}</span>
          {/if}
        </div>

        <div class="choice-screen__row-side">
          {#if chosen.includes(harness.harness_id) && defaultId === harness.harness_id}
            <span class="choice-screen__badge choice-screen__badge--default">
              {$t('onboard.harness.choose.default_label')}
            </span>
          {:else if canChoose}
            <button
              type="button"
              class="choice-screen__badge"
              onclick={() => makeDefault(harness.harness_id)}
              data-testid={`harness-default-${harness.harness_id}`}
            >
              {$t('onboard.harness.choose.default_label')}
            </button>
          {/if}
        </div>
      </li>
    {/each}
  </ul>

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      disabled={chosen.length === 0}
      loading={busy}
      on:click={() => onCheck(chosen, defaultId)}
      data-testid="harness-check"
    >
      {primaryLabel}
    </Button>
  </div>
</section>
