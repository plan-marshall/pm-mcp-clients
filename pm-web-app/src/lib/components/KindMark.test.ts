import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { render } from '@testing-library/svelte';
import KindMark, { KIND_ICONS, type Kind } from './KindMark.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `KindMark` (components-tags.adoc, KindMark): the kind's icon and its word, no box; the icon of
 * project, epic and plan in its tint, the other four neutral, the word in the surrounding colour.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/** What the design expects of one kind: its icon, its words and the class that colours its icon. */
interface KindExpectation {
  icon: string;
  en: string;
  de: string;
  tone: string;
}

/**
 * The expectation of the design for every kind. It is a `Record<Kind, …>`, so a kind that is added
 * to the `Kind` domain without an expectation here does not compile.
 */
const EXPECTED: Readonly<Record<Kind, KindExpectation>> = {
  project: { icon: 'kind.project', en: 'project', de: 'Projekt', tone: 'kind-mark__icon--project' },
  epic: { icon: 'kind.epic', en: 'epic', de: 'Epic', tone: 'kind-mark__icon--epic' },
  plan: { icon: 'kind.plan', en: 'plan', de: 'Plan', tone: 'kind-mark__icon--plan' },
  side_job: { icon: 'kind.side-job', en: 'side job', de: 'Nebenarbeit', tone: 'kind-mark__icon--neutral' },
  decision: { icon: 'kind.decision', en: 'decision', de: 'Entscheidung', tone: 'kind-mark__icon--neutral' },
  report: { icon: 'kind.report', en: 'report', de: 'Bericht', tone: 'kind-mark__icon--neutral' },
  machine: { icon: 'kind.machine', en: 'machine', de: 'Maschine', tone: 'kind-mark__icon--neutral' },
};

/** Every kind of `KIND_ICONS`, the authoritative mapping, with its expectation. */
const KINDS: Array<KindExpectation & { kind: Kind }> = (Object.keys(KIND_ICONS) as Kind[]).map((kind) => ({
  kind,
  ...EXPECTED[kind],
}));

/** The classes that colour a kind's icon; exactly one of them sits on every icon. */
const TONES = [
  'kind-mark__icon--project',
  'kind-mark__icon--epic',
  'kind-mark__icon--plan',
  'kind-mark__icon--neutral',
];

describe('KindMark', () => {
  it('has an expectation for every kind of the authoritative mapping and for none beyond it', () => {
    expect(Object.keys(EXPECTED).sort()).toEqual(Object.keys(KIND_ICONS).sort());
  });

  it.each(KINDS)('draws the $kind icon $icon in its tone', ({ kind, icon, tone }) => {
    useCatalogue('en');
    const { container } = render(KindMark, { props: { kind } });
    const iconBox = container.querySelector('.kind-mark__icon') as HTMLElement;
    const drawn = iconBox.querySelector('.ui-icon') as HTMLElement;

    expect(drawn.getAttribute('data-icon')).toBe(icon);
    expect(drawn.getAttribute('aria-hidden')).toBe('true');
    expect(TONES.filter((name) => iconBox.classList.contains(name))).toEqual([tone]);
  });

  it.each(KINDS)('names the $kind kind "$en" in English and "$de" in German', ({ kind, en, de }) => {
    useCatalogue('en');
    const english = render(KindMark, { props: { kind } });
    expect(english.container.querySelector('.kind-mark__word')?.textContent).toBe(en);
    english.unmount();

    useCatalogue('de');
    const german = render(KindMark, { props: { kind } });
    expect(german.container.querySelector('.kind-mark__word')?.textContent).toBe(de);
  });

  it('keeps a hidden word as the accessible name and the icon out of the tree', () => {
    useCatalogue('de');
    const { container } = render(KindMark, { props: { kind: 'side_job', wordHidden: true } });
    const word = container.querySelector('.kind-mark__word') as HTMLElement;

    expect(word).toHaveClass('sr-only');
    expect(word.textContent).toBe('Nebenarbeit');
    expect(container.querySelector('.ui-icon')?.getAttribute('aria-hidden')).toBe('true');
    expect(container.querySelector('.kind-mark')).toHaveTextContent('Nebenarbeit');
  });

  it.each([
    ['.kind-mark__icon--project', '--color-kind-project'],
    ['.kind-mark__icon--epic', '--color-kind-epic'],
    ['.kind-mark__icon--plan', '--color-kind-plan'],
    ['.kind-mark__icon--neutral', '--color-text-secondary'],
  ])('colours %s with %s', (selector, token) => {
    const style = styleOf(path.resolve(__dirname, 'KindMark.svelte'));

    expect(ruleOf(style, selector).trim()).toBe(`color: var(${token});`);
  });

  it('shows the word by default', () => {
    useCatalogue('en');
    const { container } = render(KindMark, { props: { kind: 'plan' } });

    expect(container.querySelector('.kind-mark__word')).not.toHaveClass('sr-only');
  });

  it('gives the word no colour of its own and draws no box', () => {
    useCatalogue('en');
    const { container } = render(KindMark, { props: { kind: 'epic' } });
    const word = container.querySelector('.kind-mark__word') as HTMLElement;
    const mark = container.querySelector('.kind-mark') as HTMLElement;

    expect(word.className.split(/\s+/).filter((name) => name.startsWith('kind-mark__icon'))).toEqual([]);
    expect(word.getAttribute('style')).toBeNull();
    expect(mark.getAttribute('style')).toBeNull();
    expect(mark.querySelector('.chip, .badge')).toBeNull();
  });
});
