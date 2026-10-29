<script lang="ts">
  import { t } from '../../../lib/i18n';
  import ActionBar from '../../../lib/components/ActionBar.svelte';
  import Button from '../../../lib/components/Button.svelte';
  import Card from '../../../lib/components/Card.svelte';
  import Chip from '../../../lib/components/Chip.svelte';
  import CountBadge from '../../../lib/components/CountBadge.svelte';
  import IconButton from '../../../lib/components/IconButton.svelte';
  import KeyValueList from '../../../lib/components/KeyValueList.svelte';
  import Breadcrumb from '../../../lib/components/Breadcrumb.svelte';
  import AttentionStrip from '../../../lib/components/AttentionStrip.svelte';
  import Dialog from '../../../lib/components/Dialog.svelte';
  import StatusLabel from '../../../lib/components/StatusLabel.svelte';
  import Tabs from '../../../lib/components/Tabs.svelte';
  import Toast from '../../../lib/components/Toast.svelte';

  /**
   * The development page (VD-P-05, VD-S-03): every control of design system v2, in both themes.
   *
   * It is a fixture, not a view: the route exists only in development (see
   * `lib/dev/devRouteExclusion.ts`), and it draws the components with the same catalogue copy a
   * view uses, so a label that reads badly shows up here first. Each theme is a `data-theme` block,
   * which carries its own tokens, so both are visible at once.
   */

  /** The two themes the page draws, in the order it draws them. */
  const THEMES = ['light', 'dark'] as const;

  /** The four `Button` variants of VD-P-05. */
  const VARIANTS = ['primary', 'secondary', 'ghost', 'danger'] as const;

  /** The three `Button` sizes. */
  const SIZES = ['sm', 'md', 'lg'] as const;

  /** An id, as a chip carries one; a fixture value, not a runtime one. */
  const CHIP_VALUE = 'plan-marshall/relay@36e3fc5c';

  /** The six acts the bar gets: one primary, three secondary, two in the overflow. */
  $: acts = [
    { id: 'save', label: $t('web.common.save') },
    { id: 'cancel', label: $t('web.common.cancel') },
    { id: 'copy', label: $t('web.common.copy'), advisory: $t('web.common.copy.title') },
    { id: 'back', label: $t('common.actions.back'), advisory: $t('common.actions.back.title') },
    {
      id: 'archive',
      label: $t('control.epic.archive'),
      disabled: true,
      disabledReason: $t('dev.disabled_reason'),
    },
    { id: 'retry', label: $t('web.common.retry') },
  ];

  /** The runtime figures of the card fixture, as a view would read them. */
  $: runtimeFacts = [
    { label: $t('dev.key_value.status'), value: $t('state.plan.running') },
    { label: $t('dev.key_value.duration'), value: $t('dev.key_value.duration_value') },
    { label: $t('dev.key_value.tokens'), value: $t('dev.key_value.tokens_value') },
  ];

  /** The tab the fixture shows as selected, one per theme, so each list answers its own keyboard. */
  let activeTabs: Record<string, string> = {};

  /** The tabs of the fixture: one count that waits for the operator, one that does not. */
  $: tabs = [
    { id: 'open', label: $t('dev.tabs.open'), count: 2, attention: true },
    { id: 'done', label: $t('dev.tabs.done') },
    { id: 'failed', label: $t('dev.tabs.failed'), count: 1 },
  ];

  /** The trail of the fixture: two links and the page a person stands on. */
  $: trail = [
    { label: $t('dev.breadcrumb.projects'), href: '/projects' },
    { label: CHIP_VALUE, href: '/projects/relay' },
    { label: $t('dev.breadcrumb.plan') },
  ];

  /** Which theme's danger dialog is open, one flag per theme so each shows in its own colour. */
  let openDialogs: Record<string, boolean> = {};

  /** Which theme's toast is on screen. */
  let openToasts: Record<string, boolean> = {};

  /** What the danger act of the fixture costs. */
  $: dialogConsequences = [
    $t('dev.dialog.consequence.drafts'),
    $t('dev.dialog.consequence.lessons'),
  ];
</script>

<div class="dev">
  <h1 class="dev__title">{$t('dev.components.title')}</h1>
  <p class="dev__intro">{$t('dev.components.intro')}</p>

  {#each THEMES as theme (theme)}
    <section class="dev__theme" data-theme={theme} aria-label={$t(`dev.theme.${theme}`)}>
      <h2 class="dev__theme-title">{$t(`dev.theme.${theme}`)}</h2>

      <h3 class="dev__section-title">{$t('dev.section.buttons')}</h3>
      <div class="dev__row">
        {#each VARIANTS as variant (variant)}
          <Button {variant}>{$t(`dev.variant.${variant}`)}</Button>
        {/each}
      </div>
      <div class="dev__row">
        {#each SIZES as size (size)}
          <Button variant="primary" {size}>{$t(`dev.size.${size}`)}</Button>
        {/each}
      </div>
      <div class="dev__row">
        <Button variant="secondary" icon="act.copy">{$t('web.common.copy')}</Button>
        <Button variant="primary" loading>{$t('web.common.save')}</Button>
        <Button variant="primary" disabled disabledReason={$t('dev.disabled_reason')}>
          {$t('web.common.save')}
        </Button>
      </div>

      <h3 class="dev__section-title">{$t('dev.section.icons')}</h3>
      <div class="dev__row">
        <IconButton
          icon="act.copy"
          label={$t('web.common.copy')}
          advisory={$t('web.common.copy.title')}
        />
        <IconButton
          icon="act.more"
          label={$t('web.common.more')}
          advisory={$t('web.common.more.title')}
        />
        <IconButton
          icon="act.back"
          label={$t('common.actions.back')}
          advisory={$t('common.actions.back.title')}
        />
        <IconButton
          icon="act.copy"
          size="sm"
          label={$t('web.common.copy')}
          advisory={$t('web.common.copy.title')}
        />
        <IconButton
          icon="act.copy"
          size="lg"
          label={$t('web.common.copy')}
          advisory={$t('web.common.copy.title')}
        />
      </div>

      <h3 class="dev__section-title">{$t('dev.section.status')}</h3>
      <div class="dev__row">
        <StatusLabel tone="running" label={$t('state.plan.running')} />
        <StatusLabel tone="attention" label={$t('state.plan.waiting_for_you')} />
        <StatusLabel tone="neutral" label={$t('state.plan.held')} />
        <StatusLabel tone="danger" label={$t('state.task.failed')} />
        <StatusLabel tone="success" label={$t('state.task.done')} />
      </div>
      <div class="dev__row">
        <CountBadge count={3} />
        <CountBadge count={2} attention label={$t('dev.badge.needs_you', { count: 2 })} />
        <Chip value={CHIP_VALUE} />
      </div>

      <h3 class="dev__section-title">{$t('dev.section.action_bar')}</h3>
      <ActionBar {acts} onAct={() => {}} />

      <h3 class="dev__section-title">{$t('dev.section.cards')}</h3>
      <div class="dev__grid">
        <Card title={$t('dev.card.runtime')}>
          <svelte:fragment slot="actions">
            <Button variant="ghost" size="sm">{$t('web.common.copy')}</Button>
          </svelte:fragment>
          <KeyValueList items={runtimeFacts} />
        </Card>
        <Card title={$t('dev.card.plain')}>
          <p class="dev__note">{$t('dev.card.body')}</p>
        </Card>
        <Card title={$t('dev.card.nested')}>
          <KeyValueList items={runtimeFacts} />
          <Card title={$t('dev.card.nested_part')}>
            <p class="dev__note">{$t('dev.card.body')}</p>
          </Card>
        </Card>
      </div>

      <h3 class="dev__section-title">{$t('dev.section.key_values')}</h3>
      <Card title={$t('dev.key_value.title')}>
        <KeyValueList items={runtimeFacts} />
      </Card>

      <h3 class="dev__section-title">{$t('dev.section.tabs')}</h3>
      <Tabs
        {tabs}
        active={activeTabs[theme] ?? 'open'}
        onSelect={(id) => (activeTabs = { ...activeTabs, [theme]: id })}
      />

      <h3 class="dev__section-title">{$t('dev.section.breadcrumb')}</h3>
      <Breadcrumb crumbs={trail} label={$t('dev.breadcrumb.label')} />

      <h3 class="dev__section-title">{$t('dev.section.dialog')}</h3>
      <div class="dev__row">
        <Button
          variant="danger"
          on:click={() => (openDialogs = { ...openDialogs, [theme]: true })}
        >
          {$t('dev.dialog.open')}
        </Button>
      </div>
      <Dialog
        open={openDialogs[theme] ?? false}
        danger
        title={$t('dev.dialog.title')}
        consequences={dialogConsequences}
        onClose={() => (openDialogs = { ...openDialogs, [theme]: false })}
      >
        <p class="dev__note">{$t('dev.dialog.body')}</p>
        <svelte:fragment slot="footer">
          <Button variant="ghost" on:click={() => (openDialogs = { ...openDialogs, [theme]: false })}>
            {$t('web.common.cancel')}
          </Button>
          <Button
            variant="danger"
            on:click={() => (openDialogs = { ...openDialogs, [theme]: false })}
          >
            {$t('dev.dialog.confirm')}
          </Button>
        </svelte:fragment>
      </Dialog>

      <h3 class="dev__section-title">{$t('dev.section.toast')}</h3>
      <div class="dev__row">
        <Button variant="secondary" on:click={() => (openToasts = { ...openToasts, [theme]: true })}>
          {$t('dev.toast.show')}
        </Button>
      </div>
      <Toast
        open={openToasts[theme] ?? false}
        message={$t('dev.toast.message')}
        actionLabel={$t('dev.toast.act')}
        onAction={() => (openToasts = { ...openToasts, [theme]: false })}
        onDismiss={() => (openToasts = { ...openToasts, [theme]: false })}
      />

      <h3 class="dev__section-title">{$t('dev.section.attention')}</h3>
      <AttentionStrip
        message={$t('dev.attention.message')}
        actionLabel={$t('dev.attention.act')}
        onAct={() => {}}
      />
    </section>
  {/each}
</div>

<style>
  .dev {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
    padding: var(--space-6);
  }

  .dev__title {
    margin: 0;
    font-size: var(--text-2xl);
    line-height: var(--line-height-2xl);
  }

  .dev__intro {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-base);
  }

  .dev__theme {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding: var(--space-6);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .dev__theme-title {
    margin: 0;
    font-size: var(--text-lg);
    line-height: var(--line-height-lg);
  }

  .dev__section-title {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    text-transform: none;
  }

  .dev__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
  }

  .dev__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: var(--space-4);
  }

  .dev__note {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }
</style>
