<script lang="ts" module>
  /** The closed set of kinds an object of the interface has (components-tags.adoc, KindMark). */
  export type Kind = 'project' | 'epic' | 'plan' | 'side_job' | 'decision' | 'report' | 'machine';

  /** The UI icon of each kind. */
  export const KIND_ICONS: Readonly<Record<Kind, string>> = Object.freeze({
    project: 'kind.project',
    epic: 'kind.epic',
    plan: 'kind.plan',
    side_job: 'kind.side-job',
    decision: 'kind.decision',
    report: 'kind.report',
    machine: 'kind.machine',
  });

  /**
   * The catalogue key of each kind's word. Every key is written as a literal, so the catalogue guard
   * finds it and checks that both languages carry it.
   */
  export const KIND_WORD_KEYS: Readonly<Record<Kind, string>> = Object.freeze({
    project: 'common.kind.project',
    epic: 'common.kind.epic',
    plan: 'common.kind.plan',
    side_job: 'common.kind.side_job',
    decision: 'common.kind.decision',
    report: 'common.kind.report',
    machine: 'common.kind.machine',
  });

  /** The three kinds whose icon takes a tint of its own; the other four stay neutral. */
  export const TINTED_KINDS: readonly Kind[] = Object.freeze(['project', 'epic', 'plan']);
</script>

<script lang="ts">
  import Icon from './Icon.svelte';
  import { t } from '../i18n';

  /**
   * The kind of an object: its icon and its word, no box.
   *
   * Only the icon carries colour, and only for the three tinted kinds; the word stays in the colour
   * of the text around it. Where the space is short the word is visually hidden and stays the
   * accessible name, so the icon never stands alone in the accessibility tree.
   */
  interface Props {
    /** The kind shown. */
    kind: Kind;
    /** Hides the word visually; it stays in the accessibility tree. */
    wordHidden?: boolean;
  }

  let { kind, wordHidden = false }: Props = $props();

  const tinted = $derived(TINTED_KINDS.includes(kind));
</script>

<span class="kind-mark" data-kind={kind}>
  <span
    class="kind-mark__icon"
    class:kind-mark__icon--project={kind === 'project'}
    class:kind-mark__icon--epic={kind === 'epic'}
    class:kind-mark__icon--plan={kind === 'plan'}
    class:kind-mark__icon--neutral={!tinted}
  >
    <Icon name={KIND_ICONS[kind]} size={16} />
  </span>
  <span class="kind-mark__word" class:sr-only={wordHidden}>{$t(KIND_WORD_KEYS[kind])}</span>
</span>

<style>
  .kind-mark {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    vertical-align: middle;
  }

  .kind-mark__icon {
    display: inline-flex;
  }

  .kind-mark__icon--project {
    color: var(--color-kind-project);
  }

  .kind-mark__icon--epic {
    color: var(--color-kind-epic);
  }

  .kind-mark__icon--plan {
    color: var(--color-kind-plan);
  }

  .kind-mark__icon--neutral {
    color: var(--color-text-secondary);
  }
</style>
