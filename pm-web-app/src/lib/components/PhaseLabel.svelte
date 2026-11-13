<script lang="ts" module>
  /** The six phases of a plan (shared/texts-and-languages.adoc, Phases and Roles). */
  export type Phase = 'init' | 'refine' | 'outline' | 'plan' | 'execute' | 'finalize';

  /** The phases in the workflow's order. */
  export const PHASES: readonly Phase[] = Object.freeze([
    'init',
    'refine',
    'outline',
    'plan',
    'execute',
    'finalize',
  ]);

  /**
   * The catalogue key of each phase's word. Every key is a literal here, and `MessageCatalogueTest`
   * of `pm-api` asserts it in both languages; the web catalogue guard cannot see a key passed as a
   * variable.
   */
  export const PHASE_WORD_KEYS: Readonly<Record<Phase, string>> = Object.freeze({
    init: 'phase.name.init',
    refine: 'phase.name.refine',
    outline: 'phase.name.outline',
    plan: 'phase.name.plan',
    execute: 'phase.name.execute',
    finalize: 'phase.name.finalize',
  });

  /** The roles the runtime sends as the asking role of a decision, as the shipped scenarios do. */
  export type Role = 'implement' | 'architecture' | 'outline' | 'enrich' | 'decompose' | 'settings';

  /** The roles whose words the catalogue carries. */
  export const ROLES: readonly Role[] = Object.freeze([
    'implement',
    'architecture',
    'outline',
    'enrich',
    'decompose',
    'settings',
  ]);

  /** The catalogue key of each role's word; it equals the English word in both languages. */
  export const ROLE_WORD_KEYS: Readonly<Record<Role, string>> = Object.freeze({
    implement: 'role.name.implement',
    architecture: 'role.name.architecture',
    outline: 'role.name.outline',
    enrich: 'role.name.enrich',
    decompose: 'role.name.decompose',
    settings: 'role.name.settings',
  });

  /**
   * @param value a phase a caller hands in
   * @returns whether the workflow visual language knows it
   */
  export function isPhase(value: string): value is Phase {
    return (PHASES as readonly string[]).includes(value);
  }

  /**
   * @param value a role a caller hands in
   * @returns whether the catalogue carries its word
   */
  export function isRole(value: string | undefined): value is Role {
    return value !== undefined && (ROLES as readonly string[]).includes(value);
  }
</script>

<script lang="ts">
  import WorkflowIcon from './WorkflowIcon.svelte';
  import { t } from '../i18n';

  /**
   * The phase an object is in: the phase's icon and its word, no box, in
   * `--color-text-secondary`.
   *
   * Where a decision names the role that asks, the role's word follows after `·`
   * (`execute · implement`). Both words are the catalogue's, so no wire token reaches the screen; an
   * unknown phase draws nothing rather than its token.
   */
  interface Props {
    /** The phase the object is in. */
    phase: string;
    /** The role that asks, where a decision names one. */
    role?: string;
  }

  let { phase, role }: Props = $props();
</script>

{#if isPhase(phase)}
  <span class="phase-label" data-phase={phase}>
    <WorkflowIcon id={`phase.${phase}`} size={16} />
    <span class="phase-label__word">{$t(PHASE_WORD_KEYS[phase])}</span>
    {#if isRole(role)}
      <span class="phase-label__role">· {$t(ROLE_WORD_KEYS[role])}</span>
    {/if}
  </span>
{/if}

<style>
  .phase-label {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .phase-label__role {
    color: var(--color-text-secondary);
  }
</style>
