import { writable, derived, get } from 'svelte/store';
import { apiClient } from '../api/client';

export type Locale = 'en' | 'de';

/** The storage key of the locale the person last chose. */
const LOCALE_STORAGE_KEY = 'pm_locale';
/** The storage key prefix of a cached catalogue, one entry per locale and runtime version. */
const CATALOGUE_STORAGE_PREFIX = 'pm_i18n_messages_';

/**
 * The runtime version the catalogue belongs to, read from `GET /api/v1/status`.
 *
 * The pack a runtime serves changes only when the runtime changes, so the version is what makes a
 * cached catalogue current or stale: the cache key carries it, and a new runtime never serves the
 * catalogue of the old one (CC-P-05).
 */
let catalogueVersion: string | null = null;

/**
 * Settles the runtime version a cached catalogue belongs to.
 *
 * @param version the runtime's version, or `null` when the status could not be read
 */
export function setCatalogueVersion(version: string | null | undefined): void {
  catalogueVersion = version && version.trim().length > 0 ? version.trim() : null;
}

/** A catalogue as the browser keeps it between visits. */
interface CachedCatalogue {
  locale: Locale;
  /** When the runtime served this catalogue, for diagnostics. */
  cachedAt: string;
  messages: Record<string, string>;
}

export const currentLocale = writable<Locale>('en');
export const messagesStore = writable<Record<string, string>>({});

function skipWhitespace(text: string, startIndex: number): number {
  let i = startIndex;
  while (i < text.length && /\s/.test(text[i])) {
    i++;
  }
  return i;
}

function readBraceBlock(body: string, startIndex: number): { text: string; nextIndex: number } {
  let depth = 1;
  let i = startIndex;
  while (i < body.length && depth > 0) {
    if (body[i] === '{') {
      depth++;
    } else if (body[i] === '}') {
      depth--;
    }
    i++;
  }
  return { text: body.slice(startIndex, i - 1), nextIndex: i };
}

function parseSelectBranches(body: string): Record<string, string> {
  const branches: Record<string, string> = {};
  let i = 0;
  while (i < body.length) {
    i = skipWhitespace(body, i);
    if (i >= body.length) break;

    const keyStart = i;
    if (body[i] === '=') {
      // An exact-count branch (`=0`, `=1`, …), as ICU plural messages allow.
      i++;
    }
    while (i < body.length && /[a-zA-Z0-9_-]/.test(body[i])) {
      i++;
    }
    const key = body.slice(keyStart, i);

    i = skipWhitespace(body, i);
    if (i >= body.length || body[i] !== '{') break;

    const block = readBraceBlock(body, i + 1);
    branches[key] = block.text;
    i = block.nextIndex;
  }
  return branches;
}

function resolveIcuSelect(result: string, params: Record<string, string | number>): string {
  const selectIdx = result.indexOf(', select,');
  if (selectIdx === -1) return result;

  const openBrace = result.lastIndexOf('{', selectIdx);
  if (openBrace === -1) return result;

  const varName = result.slice(openBrace + 1, selectIdx).trim();
  const val = String(params[varName] ?? '');

  let depth = 0;
  let closeBrace = -1;
  for (let i = openBrace; i < result.length; i++) {
    if (result[i] === '{') depth++;
    else if (result[i] === '}') {
      depth--;
      if (depth === 0) {
        closeBrace = i;
        break;
      }
    }
  }

  if (closeBrace === -1) return result;

  const body = result.slice(selectIdx + ', select,'.length, closeBrace);
  const branches = parseSelectBranches(body);
  const replacement = branches[val] ?? branches['other'] ?? '';
  return result.slice(0, openBrace) + replacement + result.slice(closeBrace + 1);
}

function resolveIcuPlural(result: string, params: Record<string, string | number>): string {
  const pluralIdx = result.indexOf(', plural,');
  if (pluralIdx === -1) return result;

  const openBrace = result.lastIndexOf('{', pluralIdx);
  if (openBrace === -1) return result;

  const varName = result.slice(openBrace + 1, pluralIdx).trim();
  const count = Number(params[varName] ?? 0);

  let depth = 0;
  let closeBrace = -1;
  for (let i = openBrace; i < result.length; i++) {
    if (result[i] === '{') depth++;
    else if (result[i] === '}') {
      depth--;
      if (depth === 0) {
        closeBrace = i;
        break;
      }
    }
  }

  if (closeBrace === -1) return result;

  const body = result.slice(pluralIdx + ', plural,'.length, closeBrace);
  const branches = parseSelectBranches(body);
  const exactKey = `=${count}`;
  let replacement = branches[exactKey];
  if (replacement === undefined) {
    if (count === 1 && branches['one'] !== undefined) {
      replacement = branches['one'];
    } else {
      replacement = branches['other'] ?? '';
    }
  }
  replacement = replacement.replace(/#/g, String(count));
  return result.slice(0, openBrace) + replacement + result.slice(closeBrace + 1);
}

export function formatString(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  let processed = template;
  while (processed.includes(', select,')) {
    const next = resolveIcuSelect(processed, params);
    if (next === processed) break;
    processed = next;
  }
  while (processed.includes(', plural,')) {
    const next = resolveIcuPlural(processed, params);
    if (next === processed) break;
    processed = next;
  }
  return processed.replace(/\{(\w+)\}/g, (match, key) => {
    if (key in params) {
      return String(params[key]);
    }
    return match;
  });
}

/**
 * @returns the browser's local storage, or null when it is unavailable (server rendering,
 * disabled storage, private mode)
 */
function catalogueStorage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage;
  } catch {
    return null;
  }
}

/** @param locale the locale @returns the storage key of its catalogue for the current runtime */
function storageKeyFor(locale: Locale): string {
  return `${CATALOGUE_STORAGE_PREFIX}${locale}@${catalogueVersion ?? 'unversioned'}`;
}

/**
 * @param storage the browser storage
 * @param locale the locale
 * @returns every catalogue key this browser holds for the locale, whatever runtime wrote it
 */
function cachedKeysFor(storage: Storage, locale: Locale): string[] {
  const prefix = `${CATALOGUE_STORAGE_PREFIX}${locale}@`;
  const keys: string[] = [];
  for (let index = 0; index < storage.length; index += 1) {
    const key = storage.key(index);
    if (key?.startsWith(prefix)) {
      keys.push(key);
    }
  }
  return keys;
}

/** @param value a parsed storage entry @returns whether it maps keys to message text */
function isMessageMap(value: unknown): value is Record<string, string> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  return Object.values(value).every((text) => typeof text === 'string');
}

/** @param value a parsed storage entry @param locale the locale it must hold */
function isCachedCatalogue(value: unknown, locale: Locale): value is CachedCatalogue {
  if (!value || typeof value !== 'object') return false;
  const entry = value as Partial<CachedCatalogue>;
  return (
    entry.locale === locale && typeof entry.cachedAt === 'string' && isMessageMap(entry.messages)
  );
}

/**
 * Reads the catalogue the browser stored for a locale.
 *
 * With a known runtime version only that version's entry is read, so a catalogue of an older
 * runtime is never served. Before the version is known — the first render, before the status
 * answered — the newest entry the browser holds for the locale is rendered, so an offline start
 * still shows labels.
 *
 * A missing, unreadable, or corrupt entry is treated as no cache: the runtime's own catalogue
 * is then the only source, and an unreadable entry is dropped so the next save starts clean.
 *
 * @param locale the locale to read
 * @returns its cached messages, or null when the browser holds none
 */
export function loadCachedCatalogue(locale: Locale): Record<string, string> | null {
  const storage = catalogueStorage();
  if (!storage) return null;
  const keys = catalogueVersion === null ? cachedKeysFor(storage, locale) : [storageKeyFor(locale)];
  for (const key of keys) {
    try {
      const raw = storage.getItem(key);
      if (!raw) continue;
      const parsed: unknown = JSON.parse(raw);
      if (isCachedCatalogue(parsed, locale)) {
        return parsed.messages;
      }
      dropCachedCatalogue(storage, key);
    } catch {
      dropCachedCatalogue(storage, key);
    }
  }
  return null;
}

/**
 * Removes a catalogue the browser can no longer read, so the next save starts from a clean key.
 *
 * @param storage the browser storage
 * @param key the storage key to remove
 */
function dropCachedCatalogue(storage: Storage, key: string): void {
  try {
    storage.removeItem(key);
  } catch {
    // Storage turned read-only between the read and the drop; nothing else to do.
  }
}

/**
 * Stores a catalogue for the next visit, with the moment the runtime served it.
 *
 * The entry's key carries the runtime version, and the entries of earlier versions are dropped, so
 * a browser keeps one catalogue per locale and never grows a library of stale packs.
 *
 * Web storage can be full, disabled, or refuse writes in private mode; none of that may break
 * the screen, so a failed write keeps the catalogue in memory only.
 *
 * @param locale the locale the catalogue belongs to
 * @param messages its messages
 */
export function persistCatalogue(locale: Locale, messages: Record<string, string>): void {
  const storage = catalogueStorage();
  if (!storage) return;
  const entry: CachedCatalogue = {
    locale,
    cachedAt: new Date().toISOString(),
    messages,
  };
  const key = storageKeyFor(locale);
  try {
    storage.setItem(key, JSON.stringify(entry));
  } catch {
    // Full, disabled, or private-mode storage: the catalogue still lives in memory.
    return;
  }
  for (const stale of cachedKeysFor(storage, locale)) {
    if (stale !== key) {
      dropCachedCatalogue(storage, stale);
    }
  }
}

/**
 * Every message this session has resolved, per locale.
 *
 * A locale switch replaces the store but not this history, so a label seen before the switch
 * stays readable where the new catalogue has a gap — and the history of one locale is never read
 * while another is active, so no word of the other locale can reach the screen.
 */
const sessionMessages: Record<Locale, Record<string, string>> = {
  en: Object.create(null),
  de: Object.create(null),
};

/** The English catalogue, the fallback of every other locale. */
let englishMessages: Record<string, string> | null = null;
let englishLookupAttempted = false;
/** The locale whose catalogue `messagesStore` currently holds. */
let messagesLocale: Locale = 'en';
/** Keys already reported as missing, so a render loop cannot flood the console. */
const warnedKeys = new Set<string>();

/** @param locale the locale @param messages the messages to remember for the rest of the session */
function rememberMessages(locale: Locale, messages: Record<string, string>): void {
  for (const [key, text] of Object.entries(messages)) {
    sessionMessages[locale][key] = text;
  }
}

/**
 * @returns the English catalogue the browser stored, or null when it holds none
 */
function englishCatalogue(): Record<string, string> | null {
  if (!englishLookupAttempted) {
    englishLookupAttempted = true;
    englishMessages = loadCachedCatalogue('en');
  }
  return englishMessages;
}

/**
 * Renders the catalogue the browser stored, synchronously, before any request is awaited.
 *
 * @param locale the locale to hydrate
 * @returns whether a cached catalogue was found
 */
function hydrateFromCache(locale: Locale): boolean {
  const cached = loadCachedCatalogue(locale);
  if (!cached) return false;
  if (locale === get(currentLocale)) {
    messagesStore.set(cached);
    messagesLocale = locale;
  }
  rememberMessages(locale, cached);
  if (locale === 'en') {
    englishMessages = cached;
    englishLookupAttempted = true;
  }
  return true;
}

/** @returns the locale the person last chose, or null when the browser holds none */
function storedLocale(): Locale | null {
  const storage = catalogueStorage();
  if (!storage) return null;
  try {
    const saved = storage.getItem(LOCALE_STORAGE_KEY);
    return saved === 'en' || saved === 'de' ? saved : null;
  } catch {
    return null;
  }
}

/** @returns the saved locale, the browser's language, or English */
function detectInitialLocale(): Locale {
  const saved = storedLocale();
  if (saved) return saved;
  return typeof navigator !== 'undefined' && navigator.language?.startsWith('de') ? 'de' : 'en';
}

/**
 * Reads the interface language the machine recorded at onboarding (UC-ONB-3).
 *
 * The header switch overrides it for this browser, so this is read only where the browser holds no
 * choice of its own. A machine that cannot be asked — an unpaired browser, a runtime that is down —
 * answers nothing, and the browser's own language decides.
 *
 * @returns the recorded locale, or null when none can be read
 */
async function recordedLocale(): Promise<Locale | null> {
  try {
    const settings = await apiClient.getMachineSettings();
    const recorded = settings.language_interface?.trim().toLowerCase();
    return recorded === 'en' || recorded === 'de' ? recorded : null;
  } catch {
    return null;
  }
}

/**
 * Reads the runtime version the catalogue belongs to, so the cache key carries it.
 *
 * A status that cannot be read leaves the version unknown: the cache is then read by whatever
 * version wrote it, and the catalogue is fetched before it is trusted.
 */
async function readCatalogueVersion(): Promise<void> {
  try {
    const status = await apiClient.getStatus();
    setCatalogueVersion(status.version ?? null);
  } catch {
    setCatalogueVersion(null);
  }
}

/**
 * Loads a locale's catalogue from the runtime and keeps it for the next visit.
 *
 * The cached catalogue is rendered first, so the screen never waits on the network; the fetch
 * then replaces or extends it in the background. When the runtime is unreachable the error is
 * swallowed and the cached labels stay on screen.
 *
 * @param locale the locale to load
 * @param area the catalogue area, `all` for the complete catalogue
 */
export async function loadMessages(locale: Locale, area = 'all'): Promise<void> {
  if (area === 'all') {
    hydrateFromCache(locale);
  }
  try {
    const msgs = await apiClient.getMessages(locale, area);
    const merged = area === 'all' ? msgs : { ...baseFor(locale), ...msgs };
    // A slow response must not replace the catalogue of a locale the person has switched to
    // since the request began: the store follows the active locale only.
    const ownsStore = area === 'all' ? locale === get(currentLocale) : messagesLocale === locale;
    if (ownsStore) {
      messagesStore.set(merged);
      messagesLocale = locale;
    }
    rememberMessages(locale, msgs);
    persistCatalogue(locale, area === 'all' ? msgs : merged);
    if (locale === 'en') {
      englishMessages =
        area === 'all' ? msgs : { ...(englishMessages ?? loadCachedCatalogue('en')), ...msgs };
      englishLookupAttempted = true;
    }
  } catch {
    // Offline or the runtime is down: the cached catalogue stays on screen.
  }
}

/**
 * @param locale the locale a partial area belongs to
 * @returns the messages to merge it into: the store when it holds that locale, else its cache
 */
function baseFor(locale: Locale): Record<string, string> {
  return messagesLocale === locale ? get(messagesStore) : (loadCachedCatalogue(locale) ?? {});
}

/**
 * Switches the interface language and loads its catalogue.
 *
 * The new catalogue is loaded FIRST and the switch happens only once it is in hand: the screen
 * never shows the words of the old language under the new one, and a catalogue that cannot be
 * loaded at all leaves the language as it was instead of mixing the two (CC-F-10, CC-P-05).
 *
 * @param locale the locale to switch to
 */
export async function setLocale(locale: Locale): Promise<void> {
  if (locale === get(currentLocale) && messagesLocale === locale) {
    rememberLocaleChoice(locale);
    return;
  }

  const loaded = await loadCatalogueFor(locale);
  if (!loaded) {
    // Nothing was loaded, so nothing is replaced: the old language and its catalogue stay whole.
    return;
  }

  rememberLocaleChoice(locale);
  currentLocale.set(locale);
  messagesStore.set(loaded);
  messagesLocale = locale;
}

/**
 * Keeps the person's choice for this browser; the header switch overrides the machine's recorded
 * language, and says so in its advisory.
 *
 * @param locale the chosen locale
 */
function rememberLocaleChoice(locale: Locale): void {
  const storage = catalogueStorage();
  try {
    storage?.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // The choice applies to this visit even when the browser cannot remember it.
  }
}

/**
 * Loads one locale's complete catalogue, from the cache of the current runtime or from the runtime.
 *
 * A cached catalogue of the runtime version this browser already knows is current by construction
 * (the pack changes only with the runtime), so it is used at once and the switch is instantaneous.
 * Before the version is known the runtime is asked first, and the cache is the offline fallback.
 *
 * @param locale the locale to load
 * @returns its catalogue, or null when neither the cache nor the runtime answered
 */
async function loadCatalogueFor(locale: Locale): Promise<Record<string, string> | null> {
  const cached = loadCachedCatalogue(locale);
  if (cached && catalogueVersion !== null) {
    rememberMessages(locale, cached);
    rememberEnglish(locale, cached);
    return cached;
  }
  try {
    const messages = await apiClient.getMessages(locale, 'all');
    rememberMessages(locale, messages);
    rememberEnglish(locale, messages);
    persistCatalogue(locale, messages);
    return messages;
  } catch {
    if (cached) {
      rememberMessages(locale, cached);
      rememberEnglish(locale, cached);
      return cached;
    }
    return null;
  }
}

/**
 * @param locale the locale a catalogue belongs to @param messages its messages
 */
function rememberEnglish(locale: Locale, messages: Record<string, string>): void {
  if (locale === 'en') {
    englishMessages = messages;
    englishLookupAttempted = true;
  }
}

/**
 * Applies the interface language the machine recorded, where this browser chose none itself.
 *
 * Called after pairing, when the machine can be asked: a browser that paired just now must show
 * the recorded language without a reload (UC-ONB-3).
 */
export async function adoptRecordedLocale(): Promise<void> {
  if (storedLocale()) return;
  const recorded = await recordedLocale();
  if (!recorded || recorded === get(currentLocale)) return;
  const loaded = await loadCatalogueFor(recorded);
  if (!loaded) return;
  currentLocale.set(recorded);
  messagesStore.set(loaded);
  messagesLocale = recorded;
}

/**
 * Settles the locale and shows its catalogue, from the browser's cache first.
 *
 * The precedence is the design's: the choice this browser made, then the interface language the
 * machine recorded at onboarding, then the browser's own language.
 */
export async function initI18n(): Promise<void> {
  // First paint: the locale this browser can settle without asking the runtime, from its cache.
  const browserLocale = detectInitialLocale();
  currentLocale.set(browserLocale);
  const hydratedBefore = hydrateFromCache(browserLocale);
  if (browserLocale !== 'en') {
    englishCatalogue();
  }

  await readCatalogueVersion();
  // With the version known, only its own cache may be shown: a pack of an older runtime is dropped
  // rather than rendered, so a new runtime never serves a stale catalogue (CC-P-05).
  if (hydratedBefore && !hydrateFromCache(browserLocale)) {
    messagesStore.set({});
  }

  const initialLocale = storedLocale() ?? (await recordedLocale()) ?? browserLocale;
  if (initialLocale === browserLocale) {
    await loadMessages(initialLocale, 'all');
    return;
  }

  const loaded = await loadCatalogueFor(initialLocale);
  if (!loaded) {
    return;
  }
  currentLocale.set(initialLocale);
  messagesStore.set(loaded);
  messagesLocale = initialLocale;
}

/** @param messages a catalogue @param key a message key @returns its text, if the catalogue has it */
function ownMessage(messages: Record<string, string> | null, key: string): string | undefined {
  if (!messages || !Object.hasOwn(messages, key)) return undefined;
  return messages[key];
}

/**
 * Turns a message key into a label a person can read.
 *
 * `web.offline.unreachable` becomes `Unreachable`: the leaf identifier names the action or the
 * subject, while the package path is an implementation detail that never belongs on screen.
 *
 * @param key the message key
 * @returns a human-readable label
 */
export function humanizeKey(key: string): string {
  const leaf = key.split('.').findLast((segment) => segment.length > 0) ?? '';
  const words = leaf
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim();
  if (!words) return 'Unknown';
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** @param key the key no catalogue carries */
function warnMissingKey(key: string): void {
  if (warnedKeys.has(key)) return;
  warnedKeys.add(key);
  const env = (import.meta as ImportMeta & { env?: { DEV?: boolean } }).env;
  if (env?.DEV === true) {
    console.warn('[i18n] Missing translation for key:', key);
  }
}

/**
 * Resolves a key through the fallback tiers: the active catalogue, then English, then every
 * message this session has seen, and finally a readable form of the key itself.
 *
 * @param locale the active locale
 * @param key the message key
 * @returns the template to format
 */
function resolveTemplate(locale: Locale, key: string): string {
  const active = ownMessage(get(messagesStore), key);
  if (active !== undefined) return active;

  if (locale !== 'en') {
    const fallback = ownMessage(englishCatalogue(), key);
    if (fallback !== undefined) return fallback;
  }

  const seen =
    ownMessage(sessionMessages[locale], key) ??
    (locale === 'en' ? undefined : ownMessage(sessionMessages.en, key));
  if (seen !== undefined) return seen;

  warnMissingKey(key);
  return humanizeKey(key);
}

/**
 * Tells whether a catalogue carries a message, through the same fallback tiers `translate` reads.
 *
 * The display layer needs the answer, not the text: a value whose key the catalogue does not
 * carry reads as the unknown word instead of a humanized key, and a humanized key is
 * indistinguishable from a message once it is rendered.
 *
 * @param key the message key
 * @returns `true` when a catalogue this session can read carries the key
 */
export function hasMessage(key: string): boolean {
  const locale = get(currentLocale);
  return (
    ownMessage(get(messagesStore), key) !== undefined ||
    ownMessage(englishCatalogue(), key) !== undefined ||
    ownMessage(sessionMessages[locale], key) !== undefined ||
    (locale !== 'en' && ownMessage(sessionMessages.en, key) !== undefined)
  );
}

/**
 * @param locale the active locale
 * @param key the message key
 * @param params the ICU parameters
 * @returns the formatted message, never a raw dot-separated key
 */
export function translate(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>,
): string {
  return formatString(resolveTemplate(locale, key), params);
}

export const t = derived(
  [currentLocale, messagesStore],
  ([$locale]) =>
    (key: string, params?: Record<string, string | number>): string =>
      formatString(resolveTemplate($locale, key), params),
);

// Hydrate at import time: the first render reads the last catalogue the browser stored, so a
// reload while the runtime is unreachable shows labels instead of keys.
if (typeof window !== 'undefined') {
  const bootLocale = detectInitialLocale();
  currentLocale.set(bootLocale);
  hydrateFromCache(bootLocale);
  if (bootLocale !== 'en') {
    englishCatalogue();
  }
}
