import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  formatString,
  translate,
  setLocale,
  currentLocale,
  loadMessages,
  messagesStore,
  t,
  initI18n,
} from './index';
import { get } from 'svelte/store';
import { apiClient } from '../api/client';

describe('i18n dynamic message formatting', () => {
  beforeEach(() => {
    localStorage.clear();
    messagesStore.set({});
    currentLocale.set('en');
    vi.restoreAllMocks();
  });

  it('replaces parameter placeholders in template strings', () => {
    // Arrange
    const template = 'Hello {name}, your code is {code}';
    const params = { name: 'Oliver', code: 'KXQB-MRTN' };

    // Act
    const result = formatString(template, params);

    // Assert
    expect(result).toBe('Hello Oliver, your code is KXQB-MRTN');
  });

  it('translates fallback messages when no remote bundle is loaded', () => {
    // Act & Assert
    expect(translate('en', 'web.pair.page.title')).toBe('Pair this browser');
    const tFn = get(t);
    expect(tFn('web.pair.page.title')).toBe('Pair this browser');
  });

  it('dynamically loads remote messages and updates t store', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getMessages').mockResolvedValue({
      'custom.key': 'Remote Value {name}',
    });

    // Act
    await loadMessages('en', 'common');

    // Assert
    const tFn = get(t);
    expect(tFn('custom.key', { name: 'Test' })).toBe('Remote Value Test');
  });

  it('updates reactive currentLocale store and loads messages on setLocale', async () => {
    // Arrange
    const loadSpy = vi.spyOn(apiClient, 'getMessages').mockResolvedValue({
      'web.pair.page.title': 'Kopple diesen Browser',
    });

    // Act
    await setLocale('de');

    // Assert
    expect(get(currentLocale)).toBe('de');
    expect(localStorage.getItem('pm_locale')).toBe('de');
    expect(loadSpy).toHaveBeenCalledWith('de', 'all');

    const tFn = get(t);
    expect(tFn('web.pair.page.title')).toBe('Kopple diesen Browser');
  });

  it('initializes locale from saved localStorage preference', async () => {
    // Arrange
    localStorage.setItem('pm_locale', 'de');
    const loadSpy = vi.spyOn(apiClient, 'getMessages').mockResolvedValue({});

    // Act
    await initI18n();

    // Assert
    expect(get(currentLocale)).toBe('de');
    expect(loadSpy).toHaveBeenCalledWith('de', 'all');
  });
});
