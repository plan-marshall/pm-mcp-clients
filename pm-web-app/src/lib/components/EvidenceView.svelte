<script lang="ts" module>
  /** The keys an evidence object may name a file and its lines by. */
  const FILE_KEYS = ['file', 'path'] as const;

  /** The keys an evidence object may name a link by. */
  const LINK_KEYS = ['link', 'url', 'href'] as const;

  /**
   * A value an evidence field shows as text: a string, a number or a boolean.
   *
   * @param value an evidence value
   * @returns whether the value is one sentence, not a structure
   */
  export function isScalar(value: unknown): value is string | number | boolean {
    return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean';
  }

  /**
   * Turns a key the runtime wrote into a plain label: `waiver_gate` and `waiverGate` both read
   * `Waiver gate`.
   *
   * The keys of a decision's evidence are the runtime's own vocabulary, so a label is derived from
   * the key and never guessed from a catalogue it does not have — a key the catalogue does not know
   * would otherwise render as its raw identifier (`decisions.adoc#order`).
   *
   * @param key the evidence key
   * @returns the label the field shows
   */
  export function plainLabel(key: string): string {
    const words = key
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/[_-]+/g, ' ')
      .trim()
      .toLowerCase();
    return words.length === 0 ? key : words.charAt(0).toUpperCase() + words.slice(1);
  }
</script>

<script lang="ts">
  import Link from './Link.svelte';
  import ExternalLink from './ExternalLink.svelte';
  import { isWebAddress } from './ExternalLink.svelte';
  import { t } from '../i18n';
  import { stripControlSequences } from '../utils/outputSafety';

  /**
   * The evidence a decision names, as fields with plain labels (`decisions.adoc#order`,
   * `EvidenceView.svelte`).
   *
   * Every key becomes one field: a sentence, a number or a flag shows as its value; an object that
   * names a file shows the file and its lines, and a link the project offers as a link; any
   * structure the view does not know is folded into a JSON block, shown expanded, so an unexpected
   * shape is read instead of guessed at. Nothing here is ever rendered as HTML: every value is text.
   */
  interface Props {
    /** The evidence of one decision, as the runtime sent it. */
    evidence: Record<string, unknown>;
    /** The heading level of the block's title, so the view keeps its outline in order. */
    titleLevel?: 2 | 3 | 4;
  }

  let { evidence, titleLevel = 3 }: Props = $props();

  /** One field of the evidence: its key, its label and what it found. */
  interface Field {
    key: string;
    label: string;
    /** The value of a sentence-like field. */
    text?: string;
    /** The file of a file field. */
    file?: string;
    /** The lines of a file field. */
    lines?: string;
    /** A link the field names, in or out of the app. */
    href?: string;
    /** A structure the view does not know, shown folded. */
    structure?: unknown;
  }

  /**
   * @param value an evidence value
   * @returns the text a sentence-like field shows
   */
  function textOf(value: string | number | boolean): string {
    return stripControlSequences(typeof value === 'string' ? value : String(value));
  }

  /**
   * @param value an evidence value
   * @returns the first link the value names, where it names one
   */
  function linkOf(value: unknown): string | undefined {
    if (typeof value !== 'object' || value === null) return undefined;
    const record = value as Record<string, unknown>;
    for (const key of LINK_KEYS) {
      const candidate = record[key];
      if (typeof candidate === 'string' && candidate.trim().length > 0) return candidate.trim();
    }
    return undefined;
  }

  /**
   * Builds the field of one evidence entry: a file with its lines, a sentence, or a folded structure.
   *
   * @param key the evidence key
   * @param value its value
   * @returns the field
   */
  function fieldOf(key: string, value: unknown): Field {
    const cleanKey = stripControlSequences(key);
    const label = stripControlSequences(plainLabel(cleanKey));
    if (isScalar(value)) {
      return { key: cleanKey, label, text: textOf(value) };
    }
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      const record = value as Record<string, unknown>;
      const fileKey = FILE_KEYS.find((name) => typeof record[name] === 'string');
      if (fileKey !== undefined) {
        const lines = record['lines'];
        return {
          key: cleanKey,
          label,
          file: stripControlSequences(String(record[fileKey])),
          lines:
            typeof lines === 'string' || typeof lines === 'number'
              ? stripControlSequences(String(lines))
              : undefined,
          href: linkOf(record),
        };
      }
    }
    return { key: cleanKey, label, structure: value };
  }

  const fields = $derived.by<Field[]>(() =>
    Object.entries(evidence).map(([key, value]) => fieldOf(key, value)),
  );

  /**
   * @param field a field that names a link
   * @returns whether the link goes outside the app
   */
  function isExternal(field: Field): boolean {
    return field.href !== undefined && isWebAddress(field.href);
  }

  /**
   * @param structure an unknown evidence structure
   * @returns its JSON, without the control sequences a runtime value may carry
   */
  function jsonText(structure: unknown): string {
    return stripControlSequences(JSON.stringify(structure, null, 2));
  }
</script>

{#if fields.length > 0}
  <section class="evidence" data-testid="evidence-view">
    {#if titleLevel === 2}
      <h2 class="evidence__title">{$t('decision.evidence.title')}</h2>
    {:else if titleLevel === 3}
      <h3 class="evidence__title">{$t('decision.evidence.title')}</h3>
    {:else}
      <h4 class="evidence__title">{$t('decision.evidence.title')}</h4>
    {/if}
    <dl class="evidence__fields">
      {#each fields as field (field.key)}
        {#if field.structure !== undefined}
          <div class="evidence__structure">
            <details class="evidence__fold" open data-testid="evidence-structure-{field.key}">
              <summary class="evidence__summary">{field.label}</summary>
              <pre class="evidence__json" data-model-text
                >{jsonText(field.structure)}</pre>
            </details>
          </div>
        {:else}
          <div class="evidence__field" data-testid="evidence-field-{field.key}">
            <dt class="evidence__label">{field.label}</dt>
            <dd class="evidence__value">
              {#if field.file !== undefined}
                <span class="evidence__file" data-model-text
                  >{field.file}{field.lines ? `:${field.lines}` : ''}</span
                >
                {#if field.href !== undefined}
                  {#if isExternal(field)}
                    <ExternalLink href={field.href} label={$t('decision.evidence.open')} />
                  {:else}
                    <Link href={field.href} label={$t('decision.evidence.open')} />
                  {/if}
                {/if}
              {:else}
                <span data-model-text>{field.text}</span>
              {/if}
            </dd>
          </div>
        {/if}
      {/each}
    </dl>
  </section>
{/if}

<style>
  .evidence {
    margin-block-end: var(--space-4);
  }

  .evidence__title {
    margin: 0 0 var(--space-2);
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
  }

  .evidence__fields {
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  /* Two columns, label and value, as `KeyValueList` draws them; the value takes the room. */
  .evidence__field {
    display: grid;
    grid-template-columns: minmax(6rem, max-content) 1fr;
    gap: var(--space-3);
    align-items: baseline;
  }

  .evidence__label {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .evidence__value {
    margin: 0;
    min-inline-size: 0;
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-2);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    overflow-wrap: anywhere;
  }

  .evidence__file {
    font-family: var(--font-mono);
    font-size: var(--text-xs);
  }

  .evidence__structure {
    padding-block: var(--space-1);
  }

  .evidence__summary {
    cursor: pointer;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .evidence__json {
    margin: var(--space-2) 0 0;
    padding: var(--space-2) var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    overflow-x: auto;
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }
</style>
