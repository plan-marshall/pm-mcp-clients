import { writable, derived, get } from 'svelte/store';
import { apiClient } from '../api/client';

export type Locale = 'en' | 'de';

export const fallbackMessages: Record<string, string> = {
  'web.offline.unreachable':
    'plan-marshall is not reachable on this machine. Start it in a terminal: pm-operator',
  'web.offline.reconnecting': 'Reconnecting …',
  'web.pair.page.title': 'Pair this browser',
  'web.pair.page.intro': 'A paired browser can act on this machine as you do.',
  'web.pair.page.code': 'Code',
  'web.pair.page.action': 'Pair this browser',
  'web.pair.page.pairing': 'Pairing this browser …',
  'web.common.close': 'Close',
  'web.common.back': 'Back',
  'web.settings.title': 'Machine Settings',
  'web.settings.aspects': 'Configuration Aspects',
  'web.settings.devices': 'Paired Devices',
  'devices.list.title': 'Paired devices',
  'devices.list.subtitle': 'The browsers that can act on this machine',
  'devices.list.empty': 'No browser is paired.',
  'devices.list.this_one': 'this one',
  'devices.act.rename': 'Rename',
  'devices.act.label': 'Label',
  'devices.act.renamed': 'Renamed: {label}',
  'devices.act.revoke': 'Revoke…',
  'devices.act.revoke_others': 'Revoke all others',
  'devices.act.listener_off': 'Turn the network listener off',
  'devices.revoke.title': 'Revoke {label}?',
  'devices.revoke.explain': 'It is signed out at once; its open pages close with the reason.',
  'devices.revoke.this_one':
    'It is the browser you are using: its pages close and show the pairing page; a new code pairs it again.',
  'devices.revoke.others_title': 'Revoke {count} devices other than this one ({label})?',
  'devices.revoke.keep': 'Keep it',
  'devices.revoke.action': 'Revoke it',
  'devices.listener_off.done':
    'The network listener is off; the devices on the network stay paired and work again once it is on.',
  'devices.revoked.title': 'This browser is no longer paired',
  'devices.revoked.reason':
    '{reason, select, device_expired {It expired after not being used since {time}.} other {It was revoked from {by} {time}.}}',
  'devices.revoked.how_back':
    'To use the web app here again, pair it with a new code: run pm-operator in a terminal of this machine.',
  'devices.revoked.pair': 'Pair this browser',
};

export const currentLocale = writable<Locale>('en');
export const messagesStore = writable<Record<string, string>>({});

function parseSelectBranches(body: string): Record<string, string> {
  const branches: Record<string, string> = {};
  let i = 0;
  while (i < body.length) {
    while (i < body.length && /\s/.test(body[i])) i++;
    if (i >= body.length) break;

    const keyStart = i;
    while (i < body.length && /[a-zA-Z0-9_-]/.test(body[i])) i++;
    const key = body.slice(keyStart, i);

    while (i < body.length && /\s/.test(body[i])) i++;
    if (i >= body.length || body[i] !== '{') break;
    i++;

    let depth = 1;
    const valStart = i;
    while (i < body.length && depth > 0) {
      if (body[i] === '{') depth++;
      else if (body[i] === '}') depth--;
      i++;
    }
    branches[key] = body.slice(valStart, i - 1);
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

export function formatString(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  const processed = resolveIcuSelect(template, params);
  return processed.replace(/\{(\w+)\}/g, (match, key) => {
    if (key in params) {
      return String(params[key]);
    }
    return match;
  });
}

export async function loadMessages(locale: Locale, area = 'all'): Promise<void> {
  try {
    const msgs = await apiClient.getMessages(locale, area);
    if (area === 'all') {
      messagesStore.set(msgs);
    } else {
      messagesStore.update((curr) => ({ ...curr, ...msgs }));
    }
  } catch {
    // Retain existing messages or fallbacks on failure
  }
}

export async function setLocale(locale: Locale): Promise<void> {
  currentLocale.set(locale);
  if (typeof window !== 'undefined') {
    localStorage.setItem('pm_locale', locale);
  }
  await loadMessages(locale, 'all');
}

export async function initI18n(): Promise<void> {
  let initialLocale: Locale = 'en';
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem('pm_locale') as Locale | null;
    if (saved === 'en' || saved === 'de') {
      initialLocale = saved;
    } else if (navigator?.language?.startsWith('de')) {
      initialLocale = 'de';
    }
  }
  currentLocale.set(initialLocale);
  await loadMessages(initialLocale, 'all');
}

export function translate(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>,
): string {
  const allMessages = get(messagesStore);
  const template = allMessages[key] || fallbackMessages[key] || key;
  return formatString(template, params);
}

export const t = derived(
  [currentLocale, messagesStore],
  ([, $messages]) =>
    (key: string, params?: Record<string, string | number>): string => {
      const template = $messages[key] || fallbackMessages[key] || key;
      return formatString(template, params);
    },
);
