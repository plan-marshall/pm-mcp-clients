import { describe, it, expect, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { currentLocale, messagesStore, t } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { harnessUsageText } from './harnessUsage';

describe('harnessUsageText', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  it('prints usage complete without detail on HarnessChoice even when token_detail is present', () => {
    const translate = get(t);
    expect(
      harnessUsageText(translate, { token_usage: 'complete', token_detail: 'input, output' }),
    ).toBe('usage complete');
  });

  it('prints complete: {figures} on TokensScreen when preferDetailOnComplete is enabled', () => {
    const translate = get(t);
    expect(
      harnessUsageText(
        translate,
        { token_usage: 'complete', token_detail: 'input, cache read, output' },
        { preferDetailOnComplete: true },
      ),
    ).toBe('complete: input, cache read, output');
    expect(
      harnessUsageText(translate, { token_usage: 'complete', token_detail: '   ' }, { preferDetailOnComplete: true }),
    ).toBe('usage complete');
  });

  it('translates missing_figures via report.tokens.component in English and German', () => {
    expect(
      harnessUsageText(get(t), {
        token_usage: 'partial',
        missing_figures: ['cache_read', 'cache_write'],
      }),
    ).toBe('usage partial: no cache read, cache write');

    try {
      currentLocale.set('de');
      messagesStore.set(loadMessages('de'));
      expect(
        harnessUsageText(get(t), {
          token_usage: 'partial',
          missing_figures: ['cache_read', 'cache_write'],
        }),
      ).toBe('Tokenverbrauch teilweise: ohne aus dem Cache gelesen, in den Cache geschrieben');
    } finally {
      currentLocale.set('en');
      messagesStore.set(loadMessages('en'));
    }
  });

  it('derives missing components from usage.figures when missing_figures is absent', () => {
    expect(
      harnessUsageText(get(t), {
        usage: {
          coverage: 'partial',
          figures: {
            input: 'not_reported',
            output: 'reported',
            reasoning: 'not_reported',
          },
        },
      }),
    ).toBe('usage partial: no input, reasoning');
  });

  it('preserves pre-formatted token_detail and avoids dangling colons when figures are absent', () => {
    const translate = get(t);
    expect(
      harnessUsageText(translate, {
        token_usage: 'partial',
        token_detail: 'cache read',
      }),
    ).toBe('usage partial: no cache read');
    expect(harnessUsageText(translate, { token_usage: 'partial' })).toBe('usage partial');
    expect(harnessUsageText(translate, { token_usage: 'not_reported' })).toBe('usage not reported');
    expect(harnessUsageText(translate, {})).toBe('usage not reported');
  });
});
