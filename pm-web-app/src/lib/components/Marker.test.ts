import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { render } from '@testing-library/svelte';
import Marker, { MARKERS } from './Marker.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `Marker` (components-tags.adoc, markers): a small neutral icon and its word in
 * `--color-text-secondary`, after the label of the option it marks. No fill, no border, no brand
 * colour.
 */

const STYLE = styleOf(path.resolve(__dirname, 'Marker.svelte'));

/** The catalogue's word of each marker, per locale. */
const WORDS: Record<'en' | 'de', Record<string, string>> = {
  en: { recommended: 'recommended', default: 'default', this_one: 'this one' },
  de: { recommended: 'empfohlen', default: 'Standard', this_one: 'dieses Gerät' },
};

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('Marker', () => {
  it('carries the three kinds of a marker', () => {
    expect([...MARKERS]).toEqual(['recommended', 'default', 'this_one']);
  });

  it.each(['en', 'de'] as const)('draws each kind with its word in %s', (locale) => {
    useCatalogue(locale);

    for (const kind of MARKERS) {
      const word = WORDS[locale][kind];
      const { container, unmount } = render(Marker, { props: { kind } });

      expect(container.querySelector('.marker')).toHaveTextContent(word);
      expect(container.querySelector('[data-icon]')).not.toBeNull();
      unmount();
    }
  });

  it.each([
    ['en', 'Primary desktop'],
    ['de', 'Primary desktop'],
  ] as const)('names the option it marks in %s', (locale, label) => {
    useCatalogue(locale);
    const { container } = render(Marker, { props: { kind: 'this_one', label } });

    expect(container.querySelector('.marker')).toHaveAttribute('aria-label', label);
  });

  it('draws no fill, no border and no brand colour', () => {
    const rule = ruleOf(STYLE, '.marker');

    expect(rule).toContain('color: var(--color-text-secondary)');
    expect(STYLE).not.toContain('background-color');
    expect(STYLE).not.toContain('border');
    expect(STYLE).not.toContain('--color-brand');
  });
});
