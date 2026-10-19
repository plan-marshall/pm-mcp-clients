<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';

  /**
   * The QR code of a pairing URL, drawn as an SVG.
   *
   * The code is rendered here, in the browser, and never sent anywhere: the URL carries the
   * pairing code in its fragment, which the browser does not transmit to a server.
   */
  export let value: string;
  export let size = 220;
  export let label = '';

  let modules: boolean[][] = [];

  onMount(() => {
    void draw();
  });

  $: if (value) {
    void draw();
  }

  async function draw(): Promise<void> {
    if (!value) {
      modules = [];
      return;
    }
    try {
      const { default: qrcode } = await import('qrcode-generator');
      const code = qrcode(0, 'M');
      code.addData(value);
      code.make();
      const count = code.getModuleCount();
      const next: boolean[][] = [];
      for (let row = 0; row < count; row++) {
        const line: boolean[] = [];
        for (let col = 0; col < count; col++) {
          line.push(code.isDark(row, col));
        }
        next.push(line);
      }
      modules = next;
    } catch {
      modules = [];
    }
  }
</script>

{#if modules.length > 0}
  <figure class="qr" data-testid="pairing-qr">
    <svg
      class="qr__svg"
      width={size}
      height={size}
      viewBox="0 0 {modules.length} {modules.length}"
      role="img"
      aria-label={label || $t('web.pair.qr.label')}
      shape-rendering="crispEdges"
    >
      <rect width={modules.length} height={modules.length} fill="#ffffff" />
      {#each modules as row, y}
        {#each row as dark, x}
          {#if dark}
            <rect {x} {y} width="1" height="1" fill="#000000" />
          {/if}
        {/each}
      {/each}
    </svg>
    {#if label}
      <figcaption class="qr__caption">{label}</figcaption>
    {/if}
  </figure>
{/if}

<style>
  .qr {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    margin: 0;
    padding: var(--space-3);
    background-color: #ffffff;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    align-self: center;
  }

  .qr__svg {
    display: block;
    max-width: 100%;
    height: auto;
  }

  .qr__caption {
    font-size: var(--text-xs);
    color: #0f172a;
    text-align: center;
    overflow-wrap: anywhere;
  }
</style>
