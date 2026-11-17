<script lang="ts">
  import Button from '../../components/Button.svelte';
  import RadioGroup, { type RadioOption } from '../../components/RadioGroup.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * How plans run on this machine (`defaults.review`, `defaults.reconfigure`).
   *
   * Per axis the screen shows the axis' name, one sentence of what it means, and its variants with
   * plain names and a few words each; a variant the machine cannot run is greyed with its reason.
   * The runtime's value is the proposal, so a line the operator does not touch keeps it, and the
   * button saves only the lines that changed. The posture is the later run's fourth line and never
   * stands in the first run (onboarding-steps-4-7.adoc, Plans).
   */
  let { view }: ScreenProps = $props();

  /** One variant of an axis: its wire value, its plain name and a few words about what it does. */
  interface Variant {
    value: string;
    label: string;
    detail: string;
  }

  /** One axis of the plan's defaults. */
  interface Axis {
    /** The state field the axis is reported in. */
    key: string;
    /** The catalogue key of the axis' name. */
    title: string;
    /** The catalogue key of the one sentence that says what the axis means. */
    meaning: string;
    /** The variants, in the order the design lists them. */
    variants: readonly Variant[];
  }

  /** A variant the machine reports it cannot run, with the reason it cannot. */
  interface Unavailable {
    axis: string;
    value: string;
    reason: string;
  }

  const LANDING: readonly Variant[] = [
    {
      value: 'landing.pull-request',
      label: 'onboard.defaults.landing.pull_request',
      detail: 'onboard.defaults.landing.pull_request_detail',
    },
    {
      value: 'landing.main',
      label: 'onboard.defaults.landing.main',
      detail: 'onboard.defaults.landing.main_detail',
    },
    {
      value: 'landing.files',
      label: 'onboard.defaults.landing.files',
      detail: 'onboard.defaults.landing.files_detail',
    },
  ];

  const METHOD: readonly Variant[] = [
    {
      value: 'method.test-first',
      label: 'onboard.defaults.method.test_first',
      detail: 'onboard.defaults.method.test_first_detail',
    },
    {
      value: 'method.tests-after',
      label: 'onboard.defaults.method.test_after',
      detail: 'onboard.defaults.method.test_after_detail',
    },
  ];

  const ASKING: readonly Variant[] = [
    {
      value: 'ask.every-phase',
      label: 'onboard.defaults.asking.every_phase',
      detail: 'onboard.defaults.asking.every_phase_detail',
    },
    {
      value: 'ask.key-points',
      label: 'onboard.defaults.asking.key_points',
      detail: 'onboard.defaults.asking.key_points_detail',
    },
    {
      value: 'ask.questions-only',
      label: 'onboard.defaults.asking.questions',
      detail: 'onboard.defaults.asking.questions_detail',
    },
  ];

  /** The posture's wire values are bare (`minimal`, `standard`, `full`); unset is the empty value. */
  const POSTURE: readonly Variant[] = [
    {
      value: '',
      label: 'onboard.defaults.posture.unset',
      detail: 'onboard.defaults.posture.unset_detail',
    },
    {
      value: 'minimal',
      label: 'onboard.defaults.posture.minimal',
      detail: 'onboard.defaults.posture.minimal_detail',
    },
    {
      value: 'standard',
      label: 'onboard.defaults.posture.standard',
      detail: 'onboard.defaults.posture.standard_detail',
    },
    {
      value: 'full',
      label: 'onboard.defaults.posture.full',
      detail: 'onboard.defaults.posture.full_detail',
    },
  ];

  const FIRST_RUN_AXES: readonly Axis[] = [
    {
      key: 'landing',
      title: 'onboard.defaults.review.landing',
      meaning: 'onboard.defaults.review.landing_meaning',
      variants: LANDING,
    },
    {
      key: 'method',
      title: 'onboard.defaults.review.method',
      meaning: 'onboard.defaults.review.method_meaning',
      variants: METHOD,
    },
    {
      key: 'asking',
      title: 'onboard.defaults.review.asking',
      meaning: 'onboard.defaults.review.asking_meaning',
      variants: ASKING,
    },
  ];

  const POSTURE_AXIS: Axis = {
    key: 'posture',
    title: 'onboard.defaults.review.posture',
    meaning: 'onboard.defaults.review.posture_meaning',
    variants: POSTURE,
  };

  /** The states of the later run from the machine settings, which alone carry the posture. */
  const LATER_RUN_STATES = new Set(['defaults.reconfigure']);

  const stateId = $derived(view.state?.state ?? '');
  const laterRun = $derived(LATER_RUN_STATES.has(stateId));
  const axes = $derived(laterRun ? [...FIRST_RUN_AXES, POSTURE_AXIS] : FIRST_RUN_AXES);
  const unavailable = $derived(view.arrayField<Unavailable>('unavailable'));

  /** The lines the operator changed; a line set to the runtime's own value is not a change. */
  let changes = $state<Record<string, string>>({});

  /** @param axis the axis @returns the value it shows: the operator's change, else the runtime's */
  function valueOf(axis: Axis): string {
    return changes[axis.key] ?? view.field(axis.key) ?? '';
  }

  /** @param axis the axis @returns whether the operator changed it away from the runtime's value */
  function isChanged(axis: Axis): boolean {
    const change = changes[axis.key];
    return change !== undefined && change !== (view.field(axis.key) ?? '');
  }

  const changedAxes = $derived(axes.filter(isChanged));

  /**
   * The axis whose chosen value the state now names as unavailable, if any.
   *
   * A refresh can grey the very variant the screen holds: recording it would send a value the
   * machine says it cannot run, so the record waits until an available variant is chosen.
   */
  const blockedAxis = $derived(
    axes.find((axis) => {
      const value = valueOf(axis);
      return unavailable.some((entry) => entry.axis === axis.key && entry.value === value);
    }),
  );

  /** @param axis the axis @returns its variants as the radio group's options, the impossible ones greyed */
  function optionsOf(axis: Axis): RadioOption[] {
    return axis.variants.map((variant) => {
      const blocked = unavailable.find((entry) => entry.axis === axis.key && entry.value === variant.value);
      return {
        value: variant.value,
        label: $t(variant.label),
        description: $t(variant.detail),
        disabled: blocked !== undefined,
        disabledReason: blocked?.reason,
      };
    });
  }

  /** Records the choices: the changed axes only, or nothing when every line keeps its default. */
  function record(): void {
    const target = recordLink();
    if (!target) return;
    const body: Record<string, unknown> = {};
    for (const axis of changedAxes) {
      const value = changes[axis.key];
      // An unset posture is the absence of the key, never an empty string: a plan asks it then.
      body[axis.key] = axis.key === 'posture' && value === '' ? null : value;
    }
    view.onLink(target.id, body);
  }

  /** @returns the link that records the choices: the state's accept, its set, or its last link */
  function recordLink() {
    return view.linkBy('accept', 'set') ?? view.links[view.links.length - 1];
  }

  /** The state's other links, which stand beside the record act as the design draws them. */
  const otherLinks = $derived(view.links.filter((link) => link.id !== recordLink()?.id));
</script>

<div class="state-card" data-testid="defaults-screen">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.defaults.review.title')}
    </h1>
    <p class="state-card__summary">{$t('onboard.defaults.review.override_note')}</p>
  </div>

  <div class="defaults-axes">
    {#each axes as axis (axis.key)}
      <div class="defaults-axis" data-axis={axis.key}>
        <RadioGroup
          legend={$t(axis.title)}
          name={`defaults-${axis.key}`}
          value={valueOf(axis)}
          options={optionsOf(axis)}
          onchange={(value) => (changes[axis.key] = value)}
          testid={`defaults-axis-${axis.key}`}
        />
        <p class="defaults-axis__meaning">{$t(axis.meaning)}</p>
      </div>
    {/each}
  </div>

  <div class="state-card__actions">
    {#if blockedAxis}
      <p class="defaults-blocked" role="alert" data-testid="defaults-blocked">
        {$t('onboard.defaults.review.choose_available', { axis: $t(blockedAxis.title) })}
      </p>
    {/if}
    {#each otherLinks as link (link.id)}
      <Button
        variant="secondary"
        loading={view.busy}
        onclick={() => view.onLink(link.id)}
        data-testid={`defaults-link-${link.id}`}
      >
        {view.linkLabel(link.id)}
      </Button>
    {/each}
    <Button
      variant="primary"
      loading={view.busy}
      disabled={blockedAxis !== undefined}
      onclick={record}
      data-testid="defaults-record"
    >
      {changedAxes.length > 0
        ? $t('onboard.defaults.review.action')
        : $t('onboard.defaults.review.action_defaults')}
    </Button>
  </div>
</div>

<style>
  .defaults-axes {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
  }

  .defaults-axis {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .defaults-axis__meaning {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .defaults-blocked {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-danger);
  }
</style>
