<script lang="ts" module>
  /** The operator's unfinished edits of the roles screen. */
  export interface RoleEditSession {
    drafts: Record<string, { harness_id: string; model_id: string; effort: string }>;
    decisions: Record<string, 'accept' | 'reject'>;
    decidedOn: Record<string, string>;
    editing: Record<string, boolean>;
    openAdvisory: Record<string, boolean>;
  }

  /**
   * The edits the operator has made and not yet recorded, kept for this page visit.
   *
   * A refused `roles.validate` returns to this screen as a fresh mount, and so does a back from the
   * validating screen: the edits live outside the component instance, so the hand-typed model ids,
   * the opened rows and the departure decisions survive it (PLAN-66 residue, D3). They are not
   * cleared when the record act is sent — a refused record keeps them — only when a refreshed state
   * no longer carries the role or changes its proposal.
   */
  export const roleEditSession: RoleEditSession = $state({
    drafts: {},
    decisions: {},
    decidedOn: {},
    editing: {},
    openAdvisory: {},
  });

  /** Forgets every edit of this page visit; a screen that ends its run calls it, the tests call it. */
  export function resetRoleEditSession(): void {
    roleEditSession.drafts = {};
    roleEditSession.decisions = {};
    roleEditSession.decidedOn = {};
    roleEditSession.editing = {};
    roleEditSession.openAdvisory = {};
  }
</script>

<script lang="ts">
  import { tick } from 'svelte';
  import Button from '../../components/Button.svelte';
  import Icon from '../../components/Icon.svelte';
  import SelectField, { type SelectOption } from '../../components/SelectField.svelte';
  import StatusLabel from '../../components/StatusLabel.svelte';
  import TextField from '../../components/TextField.svelte';
  import { hasMessage, t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * The role assignment (`roles.review`, `roles.reconfigure`).
   *
   * The roles are grouped by the phase they serve. Each row names the role, the harness and model
   * it runs on, its effort and where the value comes from: the default, a departure the steward
   * proposed (to accept or reject per role), the operator's own change, or open because no model of
   * the needed class was chosen. A role's advisory opens in place. A role that stands open is
   * assigned in place, and the run waits for it (onboarding-steps-4-7.adoc, Roles).
   */
  let { view }: ScreenProps = $props();

  /** The advisory of one role: what it does, what it needs, and why this tier. */
  interface Advisory {
    does?: string;
    needs?: string;
    why?: string;
    lower_tier?: string;
    steward?: string;
  }

  /** The assignment the steward proposes in place of the default. */
  interface Departure {
    harness_id?: string;
    model_id?: string;
    effort?: string;
    reason?: string;
  }

  /** One role as the runtime reports it. */
  interface RoleRow {
    role: string;
    class?: string;
    phase?: string;
    harness_id?: string;
    model_id?: string;
    effort?: string;
    origin?: string;
    /** The assignment the role holds by default; the row's own values when the state names none. */
    default?: Departure;
    departure?: Departure;
    /** Why `roles.validate` refused this role, when it did. */
    validation?: string;
    advisory?: Advisory;
  }

  /** One choice the operator made for the row's harness, model and effort. */
  interface Draft {
    harness_id: string;
    model_id: string;
    effort: string;
  }

  /** The phases in the order the design draws them. */
  const PHASES = ['planning', 'execution', 'review', 'finalize', 'decisions', 'speech'] as const;

  /** @param phase the phase id @returns the catalogue key of its heading; `other` for an unknown one */
  function phaseKey(phase: string): string {
    return `onboard.roles.phase.${phase}`;
  }

  /** The effort levels a role can carry, and the words the catalogue has for them. */
  const EFFORTS: readonly string[] = ['low', 'medium', 'high'];

  /** The catalogue key of each class of role; a class the catalogue does not name reads as Other. */
  const CLASS_KEYS: Record<string, string> = {
    read_only: 'onboard.roles.class.read_only',
    write: 'onboard.roles.class.write',
    zero_tool: 'onboard.roles.class.zero_tool',
    other: 'onboard.roles.class.other',
  };

  const roles = $derived(view.arrayField<RoleRow>('roles'));

  /** The rows the operator opened for editing, kept outside the instance (see the module above). */
  const editing = roleEditSession.editing;
  /** The assignment the operator set per role. */
  const drafts = roleEditSession.drafts;
  /** The advisory the operator opened. */
  const openAdvisory = roleEditSession.openAdvisory;
  /** The operator's decision on a departure, per role. */
  const decisions = roleEditSession.decisions;
  /** The departure each decision was taken against, so a proposal that changes clears the decision. */
  const decidedOn = roleEditSession.decidedOn;

  // A refreshed proposal for the same role makes the decision and its draft stale: the row starts
  // again from the proposal the state reports, and no stale assignment travels with the decision.
  // A role the refresh no longer lists takes its edits with it, so a vanished role's decision is
  // never recorded.
  $effect(() => {
    const present = new Set(roles.map((role) => role.role));
    for (const key of Object.keys(decisions)) {
      if (!present.has(key)) {
        delete decisions[key];
        delete decidedOn[key];
      }
    }
    for (const key of Object.keys(drafts)) {
      if (!present.has(key)) delete drafts[key];
    }
    for (const key of Object.keys(editing)) {
      if (!present.has(key)) delete editing[key];
    }
    for (const key of Object.keys(openAdvisory)) {
      if (!present.has(key)) delete openAdvisory[key];
    }
    for (const role of roles) {
      const seen = decidedOn[role.role];
      if (seen === undefined || seen === departureSnapshot(role)) continue;
      delete decisions[role.role];
      delete drafts[role.role];
      delete decidedOn[role.role];
    }
    // An edit the refreshed state already carries is recorded: its draft is dropped, so a later
    // visit to this screen in the same page does not resend it. A decision stays until its own
    // proposal is gone or changed (above), so a successful record with a decision keeps the word it
    // recorded until the row's proposal settles.
    for (const role of roles) {
      const draft = drafts[role.role];
      if (!draft || decisions[role.role]) continue;
      if (
        draft.harness_id === (role.harness_id ?? '') &&
        draft.model_id === (role.model_id ?? '') &&
        draft.effort === (role.effort ?? '')
      ) {
        delete drafts[role.role];
      }
    }
  });

  /** @param role the role's id @returns the proposal the state reports, as the decision records it */
  function departureSnapshot(role: RoleRow): string {
    return JSON.stringify(role.departure ?? null);
  }

  /** @param role the role's id @returns the assignment the row shows: the operator's, else the runtime's */
  function draftOf(role: RoleRow): Draft {
    return (
      drafts[role.role] ?? {
        harness_id: role.harness_id ?? '',
        model_id: role.model_id ?? '',
        effort: role.effort ?? '',
      }
    );
  }

  /** @param role the role's id @returns whether the operator changed the row's assignment */
  function isChanged(role: RoleRow): boolean {
    const draft = drafts[role.role];
    if (!draft) return false;
    return (
      draft.harness_id !== (role.harness_id ?? '') ||
      draft.model_id !== (role.model_id ?? '') ||
      draft.effort !== (role.effort ?? '')
    );
  }

  /** @param role the role's id @returns whether no harness and model are assigned yet */
  function isOpen(role: RoleRow): boolean {
    const draft = draftOf(role);
    return draft.harness_id === '' || draft.model_id === '';
  }

  /**
   * Kinds of the origin word, one status label per row.
   *
   * - `unmet` is an open role: no chosen model of the class it needs;
   * - `accepted` and `rejected` are the operator's decision on a steward's departure;
   * - `changed` is the operator's own assignment;
   * - `steward` is a departure still to decide;
   * - `default` is the shipped assignment.
   *
   * @param role the role's id
   * @returns the kind the row's status label names
   */
  function originOf(role: RoleRow): string {
    const decision = decisions[role.role];
    if (decision === 'accept') return 'accepted';
    if (decision === 'reject') return 'rejected';
    if (isChanged(role)) return 'changed';
    if (isOpen(role)) return 'unmet';
    return role.origin === 'steward' ? 'steward' : role.origin === 'changed' ? 'changed' : 'default';
  }

  /** @param kind the origin the row names @returns the tone its status label carries */
  function toneOf(kind: string): 'neutral' | 'attention' | 'danger' | 'success' {
    if (kind === 'unmet') return 'danger';
    if (kind === 'steward') return 'attention';
    if (kind === 'accepted') return 'success';
    return 'neutral';
  }

  /** @param kind the origin the row names @returns the catalogue key of its word */
  function originKey(kind: string): string {
    return `onboard.roles.origin.${kind}`;
  }

  /** @param roleClass the class the state reports @returns the catalogue key of its word */
  function classKey(roleClass: string | undefined): string {
    return CLASS_KEYS[roleClass ?? 'other'] ?? CLASS_KEYS.other;
  }

  /**
   * @param role the role's id
   * @param field the part of the assignment to set
   * @param value the value to set
   */
  function setDraft(role: RoleRow, field: keyof Draft, value: string): void {
    drafts[role.role] = { ...draftOf(role), [field]: value };
  }

  /** @param role the role's id @param decision the decision to take on its departure */
  function decide(role: RoleRow, decision: 'accept' | 'reject'): void {    decisions[role.role] = decision;
    decidedOn[role.role] = departureSnapshot(role);
    // Accepting takes the steward's proposal; rejecting returns the row to the role's default.
    const target = decision === 'accept' ? role.departure : role.default;
    drafts[role.role] = {
      harness_id: target?.harness_id ?? role.harness_id ?? '',
      model_id: target?.model_id ?? role.model_id ?? '',
      effort: target?.effort ?? role.effort ?? '',
    };
  }

  /**
   * Opens or closes a row's edit, and moves the focus into its first field when it opens, so the
   * operator lands where they act instead of at the top of the page.
   *
   * @param role the role whose row is opened
   */
  async function toggleEdit(role: RoleRow): Promise<void> {
    editing[role.role] = !editing[role.role];
    if (!editing[role.role]) return;
    await tick();
    document.querySelector<HTMLElement>(`[data-testid="role-harness-${role.role}"]`)?.focus();
  }

  /** The harnesses a role can be assigned to, by their names. */
  const harnessOptions = $derived.by((): SelectOption[] => {
    const names = view.harnessNames;
    const options: SelectOption[] = [];
    for (const [id, name] of Object.entries(names)) {
      options.push({ value: id, label: name });
    }
    for (const role of roles) {
      const id = role.harness_id ?? '';
      if (id !== '' && !options.some((option) => option.value === id)) {
        options.push({ value: id, label: id });
      }
    }
    return options;
  });

  /** @param id the harness's id @returns the name the runtime reports for it, or the id */
  function harnessLabel(id: string): string {
    return view.harnessNames[id] ?? id;
  }

  const effortOptions = $derived<SelectOption[]>([
    { value: '', label: $t('onboard.roles.effort.none') },
    ...EFFORTS.map((effort) => ({
      value: effort,
      label: $t(`onboard.roles.effort.${effort}`),
    })),
  ]);

  /** The roles of one phase, in the runtime's order. */
  interface PhaseGroup {
    phase: string;
    title: string;
    roles: RoleRow[];
  }

  const groups = $derived.by((): PhaseGroup[] => {
    const found: PhaseGroup[] = [];
    for (const phase of [...PHASES, 'other']) {
      const members = roles.filter(
        (role) =>
          (PHASES.includes(role.phase as (typeof PHASES)[number]) ? role.phase : 'other') === phase,
      );
      if (members.length > 0) {
        found.push({ phase, title: phaseKey(phase), roles: members });
      }
    }
    return found;
  });

  /** The roles nothing assigns yet: the run waits for each of them. */
  const openRoles = $derived(roles.filter(isOpen));
  /**
   * The rows the operator changed by hand.
   *
   * A row with a departure decision travels as that decision, and a hand change after it travels
   * as an assignment beside it: the decision says what was taken, the assignment what the operator
   * set last.
   */
  const changedRows = $derived(roles.filter(isHandChanged));

  /** @param role the role's id @returns the assignment the operator's decision settles on, if any */
  function decidedTarget(role: RoleRow): Draft | undefined {
    const decision = decisions[role.role];
    const target = decision === 'accept' ? role.departure : decision === 'reject' ? role.default : undefined;
    if (!target) return undefined;
    return {
      harness_id: target.harness_id ?? role.harness_id ?? '',
      model_id: target.model_id ?? role.model_id ?? '',
      effort: target.effort ?? role.effort ?? '',
    };
  }

  /** @param role the role's id @returns whether the draft differs from the assignment it starts at */
  function isHandChanged(role: RoleRow): boolean {
    const start = decidedTarget(role) ?? {
      harness_id: role.harness_id ?? '',
      model_id: role.model_id ?? '',
      effort: role.effort ?? '',
    };
    const draft = draftOf(role);
    return (
      draft.harness_id !== start.harness_id ||
      draft.model_id !== start.model_id ||
      draft.effort !== start.effort
    );
  }

  /** @param role the role's id @returns the word beside its effort, from the catalogue */
  function effortLabel(role: RoleRow): string {
    return effortWord(draftOf(role).effort);
  }

  /**
   * The catalogue's word for an effort it names, and the value itself for one it does not: the
   * engine defines the effort as what the harness accepts for the model
   * (`model-work/01-roles-and-configuration.adoc:84`), so a value outside low, medium and high is a
   * legal fact, never a missing translation.
   *
   * @param effort the effort the assignment carries
   * @returns the word to show
   */
  function effortWord(effort: string): string {
    if (effort === '') return $t('onboard.roles.effort.none');
    const key = `onboard.roles.effort.${effort}`;
    return hasMessage(key) ? $t(key) : effort;
  }

  /** @param role the role's id @returns the assignment the steward proposes, in the row's words */
  function departureLabel(role: RoleRow): string {
    const departure = role.departure;
    if (!departure) return '';
    const harness = harnessLabel(departure.harness_id ?? '');
    const model = departure.model_id ?? '';
    const effort = departure.effort ? ` · ${effortWord(departure.effort)}` : '';
    return `${harness} · ${model}${effort}`;
  }

  /** @returns the link that records the assignment: the state's accept, its set, or its last link */
  function recordLink() {
    return view.linkBy('accept', 'set') ?? view.links[view.links.length - 1];
  }

  /** The state's other links, beside the steward act and the record act, as the design draws them. */
  const otherLinks = $derived(
    view.links.filter((link) => link.id !== recordLink()?.id && link.id !== 'ask_steward'),
  );

  /** Records the assignment: the changed rows, the decided departures, or nothing when untouched. */
  function record(): void {
    const target = recordLink();
    if (!target) return;
    const body: Record<string, unknown> = {};
    const departures = Object.entries(decisions).map(([role, decision]) => ({ role, decision }));
    if (departures.length > 0) body.departures = departures;
    if (changedRows.length > 0) {
      body.roles = changedRows.map((role) => {
        const draft = draftOf(role);
        return {
          role: role.role,
          harness_id: draft.harness_id,
          model_id: draft.model_id,
          effort: draft.effort,
        };
      });
    }
    view.onLink(target.id, body);
  }
</script>

<div class="state-card" data-testid="roles-screen">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.roles.review.title')}
    </h1>
    {#if view.state?.state === 'roles.review'}
      <p class="state-card__summary">
        {$t('onboard.roles.review.defaults', { count: roles.length })}
      </p>
    {/if}
  </div>

  <div class="roles-groups">
    {#each groups as group (group.phase)}
      <section class="roles-group" data-testid={`roles-phase-${group.phase}`}>
        <h2 class="roles-group__title">{$t(group.title)}</h2>
        <ul class="roles-group__list">
          {#each group.roles as role (role.role)}
            {@const kind = originOf(role)}
            <li class="roles-row" data-testid={`role-row-${role.role}`}>
              <div class="roles-row__main">
                <span class="roles-row__role">{role.role}</span>
                <span class="roles-row__class mono">{$t(classKey(role.class))}</span>
                <span class="roles-row__assignment mono">
                  {#if draftOf(role).harness_id}
                    {harnessLabel(draftOf(role).harness_id)} · {draftOf(role).model_id}
                  {:else}
                    {$t('onboard.roles.review.no_harness')}
                  {/if}
                </span>
                <span class="roles-row__effort">{effortLabel(role)}</span>
                <StatusLabel tone={toneOf(kind)} label={$t(originKey(kind))} />
              </div>

              {#if role.departure}
                <p class="roles-row__proposed" data-testid={`role-departure-proposed-${role.role}`}>
                  {$t('onboard.roles.review.proposed', { assignment: departureLabel(role) })}
                  {#if role.departure.reason}— {role.departure.reason}{/if}
                </p>
              {/if}

              {#if role.validation}
                <p class="roles-row__validation" data-testid={`role-validation-${role.role}`}>
                  <Icon name="state.alert" size={16} />
                  <span>{$t('onboard.roles.review.refused')} {role.validation}</span>
                </p>
              {/if}

              <div class="roles-row__acts">
                {#if role.departure}
                  <Button
                    variant="secondary"
                    size="sm"
                    loading={view.busy}
                    aria-pressed={decisions[role.role] === 'accept'}
                    onclick={() => decide(role, 'accept')}
                    data-testid={`role-departure-accept-${role.role}`}
                  >
                    {$t('onboard.roles.review.accept_departure')}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    loading={view.busy}
                    aria-pressed={decisions[role.role] === 'reject'}
                    onclick={() => decide(role, 'reject')}
                    data-testid={`role-departure-reject-${role.role}`}
                  >
                    {$t('onboard.roles.review.reject_departure')}
                  </Button>
                {/if}
                <Button
                  variant="ghost"
                  size="sm"
                  icon="act.edit"
                  aria-label={$t('onboard.roles.review.change_for', { role: role.role })}
                  title={$t('onboard.roles.review.change_for', { role: role.role })}
                  onclick={() => void toggleEdit(role)}
                  data-testid={`role-change-${role.role}`}
                >
                  {$t('onboard.roles.review.change')}
                </Button>
                {#if role.advisory}
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-pressed={openAdvisory[role.role] ?? false}
                    aria-label={$t('onboard.roles.review.advisory_for', { role: role.role })}
                    title={$t('onboard.roles.review.advisory_for', { role: role.role })}
                    onclick={() => (openAdvisory[role.role] = !openAdvisory[role.role])}
                    data-testid={`role-advisory-${role.role}`}
                  >
                    {$t('onboard.roles.review.advisory_label')}
                  </Button>
                {/if}
              </div>

              {#if editing[role.role]}
                <div class="roles-row__edit">
                  <SelectField
                    label={$t('onboard.roles.review.harness')}
                    value={draftOf(role).harness_id}
                    options={harnessOptions}
                    placeholder={$t('onboard.roles.review.no_harness')}
                    testid={`role-harness-${role.role}`}
                    onchange={(value) => setDraft(role, 'harness_id', value)}
                  />
                  <TextField
                    label={$t('onboard.roles.review.model')}
                    value={draftOf(role).model_id}
                    mono
                    testid={`role-model-${role.role}`}
                    oninput={(event) => setDraft(role, 'model_id', event.currentTarget.value)}
                  />
                  <SelectField
                    label={$t('onboard.roles.review.effort_field')}
                    value={draftOf(role).effort}
                    options={effortOptions}
                    testid={`role-effort-${role.role}`}
                    onchange={(value) => setDraft(role, 'effort', value)}
                  />
                </div>
              {/if}

              {#if openAdvisory[role.role] && role.advisory}
                <dl class="roles-advisory" data-testid={`role-advisory-body-${role.role}`}>
                  {#if role.advisory.does}
                    <div class="roles-advisory__row">
                      <dt>{$t('onboard.roles.review.does')}</dt>
                      <dd>{role.advisory.does}</dd>
                    </div>
                  {/if}
                  {#if role.advisory.needs}
                    <div class="roles-advisory__row">
                      <dt>{$t('onboard.roles.review.needs')}</dt>
                      <dd>{role.advisory.needs}</dd>
                    </div>
                  {/if}
                  {#if role.advisory.why}
                    <div class="roles-advisory__row">
                      <dt>{$t('onboard.roles.review.why')}</dt>
                      <dd>{role.advisory.why}</dd>
                    </div>
                  {/if}
                  {#if role.advisory.lower_tier}
                    <div class="roles-advisory__row">
                      <dt>{$t('onboard.roles.review.lower_tier')}</dt>
                      <dd>{role.advisory.lower_tier}</dd>
                    </div>
                  {/if}
                  <div class="roles-advisory__row">
                    <dt>{$t('onboard.roles.review.steward')}</dt>
                    <dd>
                      {role.advisory.steward ??
                        role.departure?.reason ??
                        $t('onboard.roles.review.no_departure')}
                    </dd>
                  </div>
                </dl>
              {/if}
            </li>
          {/each}
        </ul>
      </section>
    {/each}
  </div>

  <div class="state-card__actions">
    {#each otherLinks as link (link.id)}
      <Button
        variant="secondary"
        loading={view.busy}
        onclick={() => view.onLink(link.id)}
        data-testid={`roles-link-${link.id}`}
      >
        {view.linkLabel(link.id)}
      </Button>
    {/each}
    {#if view.linkBy('ask_steward')}
      <Button
        variant="secondary"
        loading={view.busy}
        onclick={() => view.onLink('ask_steward')}
        data-testid="roles-ask-steward"
      >
        {$t('onboard.roles.review.ask_steward')}
      </Button>
    {/if}
    <Button
      variant="primary"
      loading={view.busy}
      disabled={openRoles.length > 0}
      onclick={record}
      data-testid="roles-record"
    >
      {#if openRoles.length > 0}
        {$t('onboard.roles.review.open_roles', { count: openRoles.length })}
      {:else if changedRows.length > 0 || Object.keys(decisions).length > 0}
        {$t('onboard.roles.review.save', { count: roles.length })}
      {:else}
        {$t('onboard.roles.review.action')}
      {/if}
    </Button>
  </div>
</div>

<style>
  .roles-groups {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
  }

  .roles-group__title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-2);
  }

  .roles-group__list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .roles-row {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .roles-row__main {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-3);
    font-size: var(--text-sm);
  }

  .roles-row__role {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .roles-row__class,
  .roles-row__assignment,
  .roles-row__effort {
    color: var(--color-text-secondary);
    overflow-wrap: anywhere;
  }

  .roles-row__acts {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .roles-row__proposed {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .roles-row__validation {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-danger);
  }

  .roles-row__edit {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .roles-advisory {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
  }

  .roles-advisory__row {
    display: grid;
    grid-template-columns: 10rem 1fr;
    gap: var(--space-3);
  }

  .roles-advisory__row dt {
    color: var(--color-text-muted);
  }

  .roles-advisory__row dd {
    margin: 0;
    color: var(--color-text-primary);
  }

  @media (max-width: 767px) {
    .roles-advisory__row {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }
  }
</style>
