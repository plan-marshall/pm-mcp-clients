import { describe, it, expect } from 'vitest';
import { formatString, translate, setLocale, currentLocale } from './index';
import { get } from 'svelte/store';

describe('i18n message formatting', () => {
  it('replaces parameter placeholders in template strings', () => {
    // Arrange
    const template = 'Hello {name}, your code is {code}';
    const params = { name: 'Oliver', code: 'KXQB-MRTN' };

    // Act
    const result = formatString(template, params);

    // Assert
    expect(result).toBe('Hello Oliver, your code is KXQB-MRTN');
  });

  it('translates known keys in English and German', () => {
    // Act & Assert
    expect(translate('en', 'web.pair.page.title')).toBe('Pair this browser');
    expect(translate('de', 'web.pair.page.title')).toBe('Kopple diesen Browser');
  });

  it('falls back to English when German translation is missing', () => {
    // Arrange
    const key = 'onboard.step.language';

    // Act
    const result = translate('de', key);

    // Assert
    expect(result).toBe('Sprache');
  });

  it('updates reactive currentLocale store on setLocale', () => {
    // Act
    setLocale('de');

    // Assert
    expect(get(currentLocale)).toBe('de');

    // Teardown
    setLocale('en');
  });
});
