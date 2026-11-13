import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render } from '@testing-library/svelte';
import ReasonBadge, { REASONS, toneOf } from './ReasonBadge.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `ReasonBadge` (components-tags.adoc, reason-badge): a pill that starts with `!` and names why
 * the object needs the operator. Its text is the one key `overview.needs.badge`, so the `!` is the
 * catalogue's; the reason's word is `overview.needs.<reason>`.
 */

/** The catalogue's word of every reason of the closed list, per locale. */
const WORDS: Record<'en' | 'de', Record<string, string>> = {
  en: {
    decision: 'decision',
    waiver: 'waiver',
    waits_for_you: 'waits for you',
    stalled: 'stalled',
    condition: 'needs a fix',
    integrity_pause: 'integrity pause',
    held: 'held',
    landing_failed: 'landing failed',
    findings: 'new findings',
    setup: 'setup',
    credential: 'credential',
    cleanup_due: 'cleanup due',
    completion_review: 'completion review',
  },
  de: {
    decision: 'Entscheidung',
    waiver: 'Waiver',
    waits_for_you: 'wartet auf dich',
    stalled: 'steht',
    condition: 'braucht eine Korrektur',
    integrity_pause: 'Integritätspause',
    held: 'zurückgehalten',
    landing_failed: 'Landen fehlgeschlagen',
    findings: 'neue Findings',
    setup: 'Einrichtung',
    credential: 'Zugangsdaten',
    cleanup_due: 'Aufräumen fällig',
    completion_review: 'Abschlussprüfung',
  },
};

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('ReasonBadge', () => {
  it('carries the thirteen reasons of the closed list', () => {
    expect([...REASONS]).toEqual([
      'decision',
      'waiver',
      'waits_for_you',
      'stalled',
      'condition',
      'integrity_pause',
      'held',
      'landing_failed',
      'findings',
      'setup',
      'credential',
      'cleanup_due',
      'completion_review',
    ]);
  });

  it.each(['en', 'de'] as const)('draws every reason with its word in %s', (locale) => {
    useCatalogue(locale);
    for (const reason of REASONS) {
      const word = WORDS[locale][reason];
      expect(word, `a ${locale} expectation for ${reason}`).toBeTypeOf('string');

      const { container, unmount } = render(ReasonBadge, { props: { reason } });

      expect(container.querySelector('.reason-badge')).toHaveTextContent(`! ${word}`);
      unmount();
    }
  });

  it.each([
    ['en', '! 2 · decision'],
    ['de', '! 2 · Entscheidung'],
  ] as const)('counts the reasons in %s', (locale, text) => {
    useCatalogue(locale);
    const { container } = render(ReasonBadge, { props: { reason: 'decision', count: 2 } });

    expect(container.querySelector('.reason-badge')).toHaveTextContent(text);
  });

  it('draws one reason without a number', () => {
    useCatalogue('en');
    const { container } = render(ReasonBadge, { props: { reason: 'decision' } });

    expect(container.querySelector('.reason-badge')).toHaveTextContent('! decision');
    expect(container.querySelector('.reason-badge')).not.toHaveTextContent('1');
  });

  it('takes the attention tone for every reason but the two that stop the work', () => {
    expect(toneOf('decision')).toBe('attention');
    expect(toneOf('findings')).toBe('attention');
    expect(toneOf('landing_failed')).toBe('danger');
    expect(toneOf('integrity_pause')).toBe('danger');
  });

  it('derives the tone from the reason when none is given', () => {
    useCatalogue('en');
    const attention = render(ReasonBadge, { props: { reason: 'decision' } });
    expect(attention.container.querySelector('.reason-badge')).toHaveClass(
      'reason-badge--attention',
    );
    expect(attention.container.querySelector('.reason-badge')).not.toHaveClass(
      'reason-badge--danger',
    );
    attention.unmount();

    const danger = render(ReasonBadge, { props: { reason: 'landing_failed' } });
    expect(danger.container.querySelector('.reason-badge')).toHaveClass('reason-badge--danger');
    expect(danger.container.querySelector('.reason-badge')).not.toHaveClass(
      'reason-badge--attention',
    );
  });

  it('lets a caller name the tone', () => {
    useCatalogue('en');
    const { container } = render(ReasonBadge, {
      props: { reason: 'decision', tone: 'danger' },
    });

    expect(container.querySelector('.reason-badge')).toHaveClass('reason-badge--danger');
  });

  it.each([
    ['en', '! another reason'],
    ['de', '! anderer Grund'],
  ] as const)('draws a reason the catalogue does not know in %s in the attention tone', (locale, text) => {
    useCatalogue(locale);
    const { container } = render(ReasonBadge, { props: { reason: 'teleported' } });
    const badge = container.querySelector('.reason-badge') as HTMLElement;

    expect(badge).toHaveTextContent(text);
    expect(badge).toHaveClass('reason-badge--attention');
    expect(badge.textContent).not.toContain('teleported');
  });

  it('draws the superseded spelling of the integrity hold as an unknown reason, never in red', () => {
    useCatalogue('en');
    const { container } = render(ReasonBadge, { props: { reason: 'integrity_hold' } });
    const badge = container.querySelector('.reason-badge') as HTMLElement;

    expect(badge).toHaveTextContent('! another reason');
    expect(badge).toHaveClass('reason-badge--attention');
  });

  it('carries the runtime reasons of NeedsOperatorKind, plus the completion review', () => {
    const source = fs.readFileSync(
      path.resolve(
        __dirname,
        '../../../../pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorKind.java',
      ),
      'utf8',
    );
    const wire = Array.from(source.matchAll(/^\s{4}[A-Z_]+\(['"]([a-z_]+)['"]\)/gm))
      .map((match) => match[1])
      .filter((value) => value !== 'other');

    expect([...REASONS]).toEqual([...wire, 'completion_review']);
  });

  it('keeps an unknown reason in the attention tone even when a caller asks for danger', () => {
    useCatalogue('en');
    const { container } = render(ReasonBadge, {
      props: { reason: 'teleported', tone: 'danger' },
    });

    expect(container.querySelector('.reason-badge')).toHaveClass('reason-badge--attention');
  });
});
