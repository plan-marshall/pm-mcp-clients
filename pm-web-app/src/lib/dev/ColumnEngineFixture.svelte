<script lang="ts">
  import TriagePage, { type Level } from '../templates/TriagePage.svelte';
  import { t } from '../i18n';

  /**
   * The development page's column-engine fixture: a synthetic five-level chain, so the window, the
   * gap strip, the peek and the side column of the chat stand side by side on the showroom page in
   * both themes and both languages. It is a fixture, not a view: it holds no selection and no
   * address, and it is drawn only in development (`lib/dev/devRouteExclusion.ts`).
   */

  /** The five levels of the chain, the object column last. */
  const LEVELS: Level[] = [
    { key: 'lens', kind: 'report', title: 'reports', href: '/reports', state: 'two new' },
    { key: 'project', kind: 'project', title: 'relay', href: '/projects/relay', state: 'running' },
    {
      key: 'epic',
      kind: 'epic',
      title: 'relay-hardening',
      href: '/epics/relay-hardening',
      state: 'running',
    },
    {
      key: 'plan',
      kind: 'plan',
      title: 'fix-relay',
      href: '/plans/fix-relay',
      state: 'three left',
    },
    {
      key: 'task',
      kind: 'plan',
      title: 'TASK-4',
      href: '/plans/fix-relay/tasks/TASK-4',
      state: 'waiting',
    },
  ];
</script>

{#snippet columnPane(level: Level)}
  <h3 class="dev__note">{level.title}</h3>
  <p class="dev__note">{$t('dev.column_engine.column', { name: level.title })}</p>
{/snippet}

{#snippet objectPane()}
  <p class="dev__note">{$t('dev.column_engine.object')}</p>
{/snippet}

{#snippet sidePane()}
  <p class="dev__note">{$t('dev.column_engine.side')}</p>
{/snippet}

<div class="dev__engine" data-testid="column-engine-fixture">
  <TriagePage
    levels={LEVELS}
    label={$t('dev.column_engine.label')}
    column={columnPane}
    object={objectPane}
    side={sidePane}
    sideOpen
  />
</div>

<style>
  .dev__engine {
    /*
     * The showroom's own width: five levels leave some of them to the gap strip once the window is
     * wide enough for two columns beside the chat, which the page shows on a wide screen.
     */
    inline-size: 100%;
    min-inline-size: 0;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
  }
</style>
