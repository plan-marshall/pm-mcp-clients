import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  formatString,
  translate,
  setLocale,
  currentLocale,
  loadCachedCatalogue,
  loadMessages,
  messagesStore,
  persistCatalogue,
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

  it('renders a readable label when the catalogue carries no message', () => {
    // Production keeps no copy of the catalogue: an uncatalogued key becomes readable copy,
    // never a raw dot-separated technical key on screen.
    expect(translate('en', 'web.pair.page.title')).toBe('Title');
    const tFn = get(t);
    expect(tFn('web.offline.unreachable')).toBe('Unreachable');
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

  it('formats ICU plurals with an exact-count branch', () => {
    const template = '{count, plural, =0 {nothing} one {# item} other {# items}}';

    expect(formatString(template, { count: 0 })).toBe('nothing');
    expect(formatString(template, { count: 1 })).toBe('1 item');
    expect(formatString(template, { count: 4 })).toBe('4 items');
  });

  it('formats ICU select strings correctly for matched branch and other fallback', () => {
    const template =
      '{reason, select, device_expired {It expired after not being used since {time}.} other {It was revoked from {by} {time}.}}';

    expect(
      formatString(template, {
        reason: 'device_expired',
        time: 'yesterday',
      }),
    ).toBe('It expired after not being used since yesterday.');

    expect(
      formatString(template, {
        reason: 'device_revoked',
        by: 'terminal',
        time: '14:10',
      }),
    ).toBe('It was revoked from terminal 14:10.');

    expect(
      formatString(template, {
        reason: 'unknown_reason',
        by: 'admin',
        time: 'now',
      }),
    ).toBe('It was revoked from admin now.');
  });
});

describe('i18n catalogue cache and the never-display-keys shield', () => {
  const CACHED_AT = '2026-10-01T08:00:00.000Z';

  /** @param locale the locale @param messages its catalogue @returns a stored entry */
  function catalogueEntry(locale: string, messages: Record<string, string>): string {
    return JSON.stringify({ locale, cachedAt: CACHED_AT, messages });
  }

  beforeEach(() => {
    localStorage.clear();
    messagesStore.set({});
    currentLocale.set('en');
    vi.restoreAllMocks();
  });

  it('hydrates the message store synchronously when the module loads', async () => {
    // Arrange
    localStorage.setItem('pm_locale', 'de');
    localStorage.setItem(
      'pm_i18n_messages_de',
      catalogueEntry('de', { 'boot.cached.label': 'Aus dem Speicher' }),
    );

    // Act
    vi.resetModules();
    const fresh = await import('./index');

    // Assert
    expect(get(fresh.currentLocale)).toBe('de');
    expect(get(fresh.messagesStore)['boot.cached.label']).toBe('Aus dem Speicher');
  });

  it('renders the cached catalogue while the runtime request is still in flight', async () => {
    // Arrange
    localStorage.setItem('pm_locale', 'de');
    localStorage.setItem(
      'pm_i18n_messages_de',
      catalogueEntry('de', { 'swr.cached.label': 'Aus dem Speicher' }),
    );
    let resolveFetch: (messages: Record<string, string>) => void = () => {};
    vi.spyOn(apiClient, 'getMessages').mockReturnValue(
      new Promise<Record<string, string>>((resolve) => {
        resolveFetch = resolve;
      }),
    );

    // Act
    const pending = initI18n();

    // Assert
    expect(get(t)('swr.cached.label')).toBe('Aus dem Speicher');

    resolveFetch({ 'swr.cached.label': 'Vom Server' });
    await pending;
    expect(get(t)('swr.cached.label')).toBe('Vom Server');
  });

  it('persists the catalogue the runtime serves', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getMessages').mockResolvedValue({
      'cache.write.label': 'Gespeichert',
    });

    // Act
    await loadMessages('de', 'all');

    // Assert
    const stored = JSON.parse(localStorage.getItem('pm_i18n_messages_de') as string);
    expect(stored.locale).toBe('de');
    expect(stored.cachedAt).toEqual(expect.any(String));
    expect(stored.messages['cache.write.label']).toBe('Gespeichert');
  });

  it('keeps the cached messages when the runtime is unreachable', async () => {
    // Arrange
    localStorage.setItem(
      'pm_i18n_messages_en',
      catalogueEntry('en', { 'offline.cached.label': 'Cached label' }),
    );
    vi.spyOn(apiClient, 'getMessages').mockRejectedValue(new Error('network down'));

    // Act
    await loadMessages('en', 'all');

    // Assert
    expect(get(t)('offline.cached.label')).toBe('Cached label');
  });

  it('switches to a cached locale while offline', async () => {
    // Arrange
    localStorage.setItem(
      'pm_i18n_messages_de',
      catalogueEntry('de', { 'locale.offline.label': 'Offline Deutsch' }),
    );
    vi.spyOn(apiClient, 'getMessages').mockRejectedValue(new Error('network down'));

    // Act
    await setLocale('de');

    // Assert
    expect(get(currentLocale)).toBe('de');
    expect(get(t)('locale.offline.label')).toBe('Offline Deutsch');
  });

  it('falls back to the cached English catalogue for a gap in the active locale', async () => {
    // Arrange
    localStorage.setItem(
      'pm_i18n_messages_en',
      catalogueEntry('en', { 'fallback.only.in.en': 'English only' }),
    );
    vi.resetModules();
    const fresh = await import('./index');
    fresh.messagesStore.set({ 'de.only.label': 'Nur Deutsch' });
    fresh.currentLocale.set('de');

    // Act
    const label = get(fresh.t)('fallback.only.in.en');

    // Assert
    expect(label).toBe('English only');
  });

  it('never renders a raw dot-separated key', () => {
    // Arrange
    const missingKey = 'web.offline.never_catalogued_key';

    // Act
    const label = get(t)(missingKey);

    // Assert
    expect(label).toBe('Never catalogued key');
    expect(label).not.toContain('.');
    expect(translate('en', 'web.offline.unreachable')).toBe('Unreachable');
  });

  it('warns once in development about a missing key', () => {
    // Arrange
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const missingKey = 'web.offline.warn_only_once';

    // Act
    get(t)(missingKey);
    get(t)(missingKey);

    // Assert
    expect(warn).toHaveBeenCalledTimes(1);
    expect(warn).toHaveBeenCalledWith('[i18n] Missing translation for key:', missingKey);
  });

  it('ignores and drops a corrupt cached catalogue', () => {
    // Arrange
    localStorage.setItem('pm_i18n_messages_en', '{not json');

    // Act
    const cached = loadCachedCatalogue('en');

    // Assert
    expect(cached).toBeNull();
    expect(localStorage.getItem('pm_i18n_messages_en')).toBeNull();
  });

  it('keeps the newly chosen locale when an older full response arrives late', async () => {
    // Arrange
    localStorage.setItem('pm_locale', 'de');
    let resolveGerman: (messages: Record<string, string>) => void = () => {};
    vi.spyOn(apiClient, 'getMessages').mockImplementation((locale) =>
      locale === 'de'
        ? new Promise<Record<string, string>>((resolve) => {
            resolveGerman = resolve;
          })
        : Promise.resolve({ 'stale.race.label': 'English wins' }),
    );

    // Act
    const initializing = initI18n();
    await setLocale('en');
    resolveGerman({ 'stale.race.label': 'Deutsch verliert' });
    await initializing;

    // Assert
    expect(get(currentLocale)).toBe('en');
    expect(get(t)('stale.race.label')).toBe('English wins');
  });

  it('merges a partial catalogue into its own locale without touching the active store', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getMessages').mockImplementation((_locale, area) =>
      area === 'all'
        ? Promise.resolve({ 'owned.en.label': 'English' })
        : Promise.resolve({ 'partial.de.label': 'Deutsch' }),
    );
    await loadMessages('en', 'all');

    // Act
    await loadMessages('de', 'common');

    // Assert
    const store = get(messagesStore);
    expect(store['owned.en.label']).toBe('English');
    expect(store['partial.de.label']).toBeUndefined();
    const cachedGerman = JSON.parse(localStorage.getItem('pm_i18n_messages_de') as string);
    expect(cachedGerman.messages['partial.de.label']).toBe('Deutsch');
    expect(cachedGerman.messages['owned.en.label']).toBeUndefined();
  });

  it('keeps working when web storage refuses writes', () => {
    // Arrange
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota exceeded');
    });

    // Act & Assert
    expect(() => persistCatalogue('en', { 'quota.label': 'Fits in memory' })).not.toThrow();
  });
});
