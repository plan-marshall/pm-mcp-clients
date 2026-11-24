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

  it('prints one complete wording, with no figures the state does not publish', () => {
    const translate = get(t);
    expect(harnessUsageText(translate, { token_usage: 'complete' })).toBe('usage complete');
  });

  it('prints the partial line with the missing figures named in English and German', () => {
    expect(
      harnessUsageText(get(t), {
        token_usage: 'partial',
        missing_figures: ['cache_read', 'cache_write'],
      }),
    ).toBe('usage partly reported (not reported: cache read, cache write)');

    try {
      currentLocale.set('de');
      messagesStore.set(loadMessages('de'));
      expect(
        harnessUsageText(get(t), {
          token_usage: 'partial',
          missing_figures: ['cache_read', 'cache_write'],
        }),
      ).toBe(
        'Tokenverbrauch teilweise gemeldet (nicht gemeldet: aus dem Cache gelesen, in den Cache geschrieben)',
      );
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
    ).toBe('usage partly reported (not reported: input, reasoning)');
  });

  it('avoids dangling parentheses when figures are absent', () => {
    const translate = get(t);
    expect(harnessUsageText(translate, { token_usage: 'partial' })).toBe('usage partly reported');
    expect(harnessUsageText(translate, { token_usage: 'not_reported' })).toBe('usage not reported');
    expect(harnessUsageText(translate, {})).toBe('usage not reported');
  });
});
