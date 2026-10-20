import { writable, derived, get } from 'svelte/store';
import { apiClient } from '../api/client';

export type Locale = 'en' | 'de';

/** The storage key of the locale the person last chose. */
const LOCALE_STORAGE_KEY = 'pm_locale';
/** The storage key prefix of a cached catalogue, one entry per locale. */
const CATALOGUE_STORAGE_PREFIX = 'pm_i18n_messages_';

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

/** @param locale the locale @returns the storage key of its cached catalogue */
function storageKeyFor(locale: Locale): string {
  return `${CATALOGUE_STORAGE_PREFIX}${locale}`;
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
 * A missing, unreadable, or corrupt entry is treated as no cache: the runtime's own catalogue
 * is then the only source, and an unreadable entry is dropped so the next save starts clean.
 *
 * @param locale the locale to read
 * @returns its cached messages, or null when the browser holds none
 */
export function loadCachedCatalogue(locale: Locale): Record<string, string> | null {
  const storage = catalogueStorage();
  if (!storage) return null;
  const key = storageKeyFor(locale);
  try {
    const raw = storage.getItem(key);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (isCachedCatalogue(parsed, locale)) {
      return parsed.messages;
    }
    dropCachedCatalogue(storage, key);
    return null;
  } catch {
    dropCachedCatalogue(storage, key);
    return null;
  }
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
  try {
    storage.setItem(storageKeyFor(locale), JSON.stringify(entry));
  } catch {
    // Full, disabled, or private-mode storage: the catalogue still lives in memory.
  }
}

/**
 * Every message this session has resolved, across locales.
 *
 * A locale switch replaces the store but not this history, so a label seen before the switch
 * stays readable even where the new catalogue has a gap.
 */
const sessionMessages: Record<string, string> = Object.create(null);

/** The English catalogue, the fallback of every other locale. */
let englishMessages: Record<string, string> | null = null;
let englishLookupAttempted = false;
/** Keys already reported as missing, so a render loop cannot flood the console. */
const warnedKeys = new Set<string>();

/** @param messages the messages to remember for the rest of the session */
function rememberMessages(messages: Record<string, string>): void {
  for (const [key, text] of Object.entries(messages)) {
    sessionMessages[key] = text;
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
  messagesStore.set(cached);
  rememberMessages(cached);
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
    const merged = area === 'all' ? msgs : { ...get(messagesStore), ...msgs };
    messagesStore.set(merged);
    rememberMessages(msgs);
    persistCatalogue(locale, merged);
    if (locale === 'en') {
      englishMessages = merged;
      englishLookupAttempted = true;
    }
  } catch {
    // Offline or the runtime is down: the cached catalogue stays on screen.
  }
}

/**
 * Switches the interface language and loads its catalogue.
 *
 * A cached catalogue of the new locale is rendered before the request, so an offline switch
 * still changes the labels instead of waiting for a runtime that cannot answer.
 *
 * @param locale the locale to switch to
 */
export async function setLocale(locale: Locale): Promise<void> {
  currentLocale.set(locale);
  const storage = catalogueStorage();
  try {
    storage?.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // The choice applies to this visit even when the browser cannot remember it.
  }
  await loadMessages(locale, 'all');
}

/**
 * Settles the locale and shows its catalogue, from the browser's cache first.
 */
export async function initI18n(): Promise<void> {
  const initialLocale = detectInitialLocale();
  currentLocale.set(initialLocale);
  hydrateFromCache(initialLocale);
  if (initialLocale !== 'en') {
    englishCatalogue();
  }
  await loadMessages(initialLocale, 'all');
}

/** @param messages a catalogue @param key a message key @returns its text, if the catalogue has it */
function ownMessage(messages: Record<string, string> | null, key: string): string | undefined {
  if (!messages || !Object.prototype.hasOwnProperty.call(messages, key)) return undefined;
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
  const leaf =
    key
      .split('.')
      .filter((segment) => segment.length > 0)
      .pop() ?? '';
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

  const seen = ownMessage(sessionMessages, key);
  if (seen !== undefined) return seen;

  warnMissingKey(key);
  return humanizeKey(key);
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
