<script lang="ts" module>
  /** The three properties a marker names (components-tags.adoc, markers). */
  export type MarkerKind = 'recommended' | 'default' | 'this_one';

  /** The markers, in the order the design names them. */
  export const MARKERS: readonly MarkerKind[] = Object.freeze(['recommended', 'default', 'this_one']);

  /** The neutral UI icon of each marker; the word tells the three apart. */
  export const MARKER_ICONS: Readonly<Record<MarkerKind, string>> = Object.freeze({
    recommended: 'state.check',
    default: 'state.dot',
    this_one: 'state.dot',
  });

  /**
   * The catalogue key of each marker's word. Every key is written as a literal, so the catalogue
   * guard finds it and checks that both languages carry it.
   */
  export const MARKER_WORD_KEYS: Readonly<Record<MarkerKind, string>> = Object.freeze({
    recommended: 'common.marker.recommended',
    default: 'common.marker.default',
    this_one: 'common.marker.this_one',
  });
</script>

<script lang="ts">
  import Icon from './Icon.svelte';
  import { t } from '../i18n';

  /**
   * A property of one option among several — _recommended_, _default_, _this one_ — after the
   * option's label: a small neutral icon and its word in `--color-text-secondary`.
   *
   * It draws no fill, no border and no brand colour; the option itself is what the operator chooses,
   * and the marker only says something about it.
   */
  interface Props {
    /** Which property the marker names. */
    kind: MarkerKind;
    /** The option the marker belongs to, as its accessible name. */
    label?: string;
  }

  let { kind, label }: Props = $props();
</script>

<span class="marker" data-marker={kind} aria-label={label || undefined}>
  <Icon name={MARKER_ICONS[kind]} size={16} />
  <span class="marker__word">{$t(MARKER_WORD_KEYS[kind])}</span>
</span>

<style>
  .marker {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
  }
</style>
