import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  formatString,
  translate,
  setLocale,
  setCatalogueVersion,
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
import { setReloadDeferred } from '../stores/reloadDeferred';

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

  /** @param locale the locale @returns the cache key a browser with no known runtime version uses */
  function cacheKey(locale: string): string {
    return `pm_i18n_messages_${locale}@unversioned`;
  }

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
      cacheKey('de'),
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
      cacheKey('de'),
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
    const stored = JSON.parse(localStorage.getItem(cacheKey('de')) as string);
    expect(stored.locale).toBe('de');
    expect(stored.cachedAt).toEqual(expect.any(String));
    expect(stored.messages['cache.write.label']).toBe('Gespeichert');
  });

  it('keeps the cached messages when the runtime is unreachable', async () => {
    // Arrange
    localStorage.setItem(
      cacheKey('en'),
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
      cacheKey('de'),
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
      cacheKey('en'),
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
    localStorage.setItem(cacheKey('en'), '{not json');

    // Act
    const cached = loadCachedCatalogue('en');

    // Assert
    expect(cached).toBeNull();
    expect(localStorage.getItem(cacheKey('en'))).toBeNull();
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
    const cachedGerman = JSON.parse(localStorage.getItem(cacheKey('de')) as string);
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

describe('the locale switch never mixes the two catalogues', () => {
  const SUMMARY_KEY = 'overview.project.counts';
  const SUMMARIES: Record<string, Record<string, string>> = {
    de: {
      [SUMMARY_KEY]:
        '{plans, plural, one {# Plan} other {# Pläne}} · {epics, plural, one {# Epic} other {# Epics}}',
    },
    en: {
      [SUMMARY_KEY]:
        '{plans, plural, one {# plan} other {# plans}} · {epics, plural, one {# epic} other {# epics}}',
    },
  };

  /** @returns the summary a view composes from the active catalogue */
  function summary(): string {
    return get(t)(SUMMARY_KEY, { plans: 3, epics: 1 });
  }

  beforeEach(async () => {
    localStorage.clear();
    messagesStore.set({});
    currentLocale.set('en');
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'getMessages').mockImplementation((locale) =>
      Promise.resolve(SUMMARIES[locale] ?? {}),
    );
    currentLocale.set('de');
    await loadMessages('de', 'all');
  });

  afterEach(() => {
    setCatalogueVersion(null);
  });

  it('switches de → en → de without a word of the other locale in a summary', async () => {
    expect(summary()).toBe('3 Pläne · 1 Epic');

    await setLocale('en');
    expect(summary()).toBe('3 plans · 1 epic');

    await setLocale('de');
    expect(summary()).toBe('3 Pläne · 1 Epic');
  });

  it('keeps the old locale and catalogue while the new one is still loading', async () => {
    let resolveEnglish: (messages: Record<string, string>) => void = () => {};
    vi.spyOn(apiClient, 'getMessages').mockImplementation((locale) =>
      locale === 'en'
        ? new Promise<Record<string, string>>((resolve) => {
            resolveEnglish = resolve;
          })
        : Promise.resolve(SUMMARIES[locale] ?? {}),
    );

    const switching = setLocale('en');

    expect(get(currentLocale)).toBe('de');
    expect(summary()).toBe('3 Pläne · 1 Epic');

    resolveEnglish(SUMMARIES.en);
    await switching;
    expect(summary()).toBe('3 plans · 1 epic');
  });

  it('keeps the old locale when the new catalogue cannot be loaded at all', async () => {
    vi.spyOn(apiClient, 'getMessages').mockRejectedValue(new Error('runtime unreachable'));

    await setLocale('en');

    expect(get(currentLocale)).toBe('de');
    expect(summary()).toBe('3 Pläne · 1 Epic');
  });

  it('does not serve a catalogue cached for an older runtime version', async () => {
    // Arrange: a cache written by the runtime this browser saw last time.
    setCatalogueVersion('0.9.1');
    persistCatalogue('de', { [SUMMARY_KEY]: 'Aus der alten Fassung' });

    // Act: the runtime now reports a new version.
    setCatalogueVersion('0.9.2');

    // Assert: the stale entry is not read, and the newer one replaces it.
    expect(loadCachedCatalogue('de')).toBeNull();
    persistCatalogue('de', SUMMARIES.de);
    expect(loadCachedCatalogue('de')?.[SUMMARY_KEY]).toBe(SUMMARIES.de[SUMMARY_KEY]);
  });

  it('renders the newest catalogue the browser holds when the version is unknown', () => {
    // Arrange: two entries of the same locale, written by different runtimes.
    localStorage.setItem(
      'pm_i18n_messages_de@1.0.0',
      JSON.stringify({
        locale: 'de',
        cachedAt: '2026-10-01T08:00:00.000Z',
        messages: { [SUMMARY_KEY]: 'Ältere Fassung' },
      }),
    );
    localStorage.setItem(
      'pm_i18n_messages_de@1.0.1',
      JSON.stringify({
        locale: 'de',
        cachedAt: '2026-10-20T08:00:00.000Z',
        messages: SUMMARIES.de,
      }),
    );
    setCatalogueVersion(null);

    // Act
    const cached = loadCachedCatalogue('de');

    // Assert: the later entry wins, whatever order the storage lists the keys in.
    expect(cached?.[SUMMARY_KEY]).toBe(SUMMARIES.de[SUMMARY_KEY]);
  });
});

describe('a deferred reload never fetches a pack the old code does not hold', () => {
  /** @param locale the locale @param messages its catalogue @returns a stored entry */
  function catalogueEntry(locale: string, messages: Record<string, string>): string {
    return JSON.stringify({ locale, cachedAt: '2026-10-01T08:00:00.000Z', messages });
  }

  beforeEach(() => {
    localStorage.clear();
    messagesStore.set({});
    currentLocale.set('en');
    vi.restoreAllMocks();
    setReloadDeferred(false);
  });

  afterEach(() => {
    setReloadDeferred(false);
    setCatalogueVersion(null);
  });

  it('keeps the current locale, fetches nothing, and applies the choice when the reload is released', async () => {
    // Arrange: the reload waits for unsent input, and German is not in the cache.
    const getMessages = vi
      .spyOn(apiClient, 'getMessages')
      .mockResolvedValue({ 'deferred.only.label': 'Deutsch' });
    setReloadDeferred(true);

    // Act
    await setLocale('de');

    // Assert: no request left the page, and the screen keeps the language it shows.
    expect(getMessages).not.toHaveBeenCalled();
    expect(get(currentLocale)).toBe('en');

    // Act: the operator sends the text and the reload proceeds.
    setReloadDeferred(false);

    // Assert: the kept choice is applied.
    await vi.waitFor(() => expect(get(currentLocale)).toBe('de'));
    expect(getMessages).toHaveBeenCalledWith('de', 'all');
    expect(get(t)('deferred.only.label')).toBe('Deutsch');
  });

  it('switches to a cached locale at once while the reload waits', async () => {
    // Arrange: German is in the hand of this browser already.
    setCatalogueVersion('0.9.1');
    localStorage.setItem(
      'pm_i18n_messages_de@0.9.1',
      catalogueEntry('de', { 'deferred.cached.label': 'Aus dem Speicher' }),
    );
    const getMessages = vi.spyOn(apiClient, 'getMessages');
    setReloadDeferred(true);

    // Act
    await setLocale('de');

    // Assert
    expect(getMessages).not.toHaveBeenCalled();
    expect(get(currentLocale)).toBe('de');
    expect(get(t)('deferred.cached.label')).toBe('Aus dem Speicher');
  });

  it('fetches no pack while the reload waits, not even the one it holds', async () => {
    // Arrange: the German pack is in hand, and the reload waits for unsent input.
    setCatalogueVersion('0.9.1');
    localStorage.setItem(
      'pm_i18n_messages_de@0.9.1',
      catalogueEntry('de', { 'deferred.held.label': 'Aus dem Speicher' }),
    );
    const getMessages = vi.spyOn(apiClient, 'getMessages');
    setReloadDeferred(true);

    // Act
    await loadMessages('de', 'all');
    await loadMessages('de', 'system');

    // Assert: the old page renders what it holds, whatever the area holds, and asks for nothing.
    expect(getMessages).not.toHaveBeenCalled();
  });

  it('lets a later choice of the language shown supersede the pending one', async () => {
    // Arrange: the catalogue store settles on English, then a deferred request keeps German pending.
    const getMessages = vi
      .spyOn(apiClient, 'getMessages')
      .mockResolvedValue({ 'deferred.superseded.label': 'English' });
    await setLocale('en');
    getMessages.mockClear();
    setReloadDeferred(true);

    // Act
    await setLocale('de');
    expect(get(currentLocale)).toBe('en');
    await setLocale('en');

    // Assert: no request leaves the page while it waits…
    expect(getMessages).not.toHaveBeenCalled();

    // …and the release applies no superseded choice.
    setReloadDeferred(false);
    await Promise.resolve();
    expect(get(currentLocale)).toBe('en');
    expect(getMessages).not.toHaveBeenCalled();
  });
});

describe('the page declares the language it shows', () => {
  beforeEach(() => {
    localStorage.clear();
    messagesStore.set({});
    currentLocale.set('en');
    vi.restoreAllMocks();
    setReloadDeferred(false);
  });

  afterEach(() => {
    document.documentElement.lang = 'en';
    setCatalogueVersion(null);
    vi.unstubAllGlobals();
  });

  it('follows the locale to German and back to English', async () => {
    // Arrange: both catalogues are in the hand of this browser.
    setCatalogueVersion('0.9.1');
    for (const locale of ['en', 'de'] as const) {
      localStorage.setItem(
        `pm_i18n_messages_${locale}@0.9.1`,
        JSON.stringify({
          locale,
          cachedAt: '2026-10-01T08:00:00.000Z',
          messages: { 'lang.label': locale === 'de' ? 'Deutsch' : 'English' },
        }),
      );
    }

    // Act
    await setLocale('de');

    // Assert
    expect(document.documentElement.lang).toBe('de');

    await setLocale('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('declares German on the first load of the pairing page in German', async () => {
    // Arrange: the language this browser chose, as the pairing page finds it on its first load.
    localStorage.setItem('pm_locale', 'de');
    setCatalogueVersion('0.9.1');
    localStorage.setItem(
      'pm_i18n_messages_de@0.9.1',
      JSON.stringify({
        locale: 'de',
        cachedAt: '2026-10-01T08:00:00.000Z',
        messages: { 'pair.first.label': 'Koppeln' },
      }),
    );
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')));
    vi.resetModules();
    const fresh = await import('./index');

    // Act: what the root layout does for every route, the pairing page included.
    await fresh.initI18n();

    // Assert
    expect(document.documentElement.lang).toBe('de');
  });
});
