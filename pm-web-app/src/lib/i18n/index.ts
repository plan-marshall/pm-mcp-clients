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
  'overview.running.title': 'Running work',
  'overview.projects.empty': 'Nothing runs. Start a plan in a project, or enrol one.',
  'overview.project.counts':
    '{plans, plural, one {# plan} other {# plans}} · {epics, plural, one {# epic} other {# epics}}',
  'overview.project.plans_only': '{plans, plural, one {# plan} other {# plans}}',
  'overview.project.epics_only': '{epics, plural, one {# epic} other {# epics}}',
  'overview.project.setup': 'setup needs you',
  'overview.project.left': '{project} has nothing running any more.',
  'overview.view.by_plan': 'by plan',
  'overview.view.by_epic': 'by epic',
  'overview.kind.plan': 'plan',
  'overview.kind.epic': 'epic',
  'overview.group.without_epic': 'without epic',
  'overview.needs.count': '! {count}',
  'overview.needs.decision': 'decision',
  'overview.needs.waiver_request': 'waiver',
  'overview.needs.operator_wait': 'waits for you',
  'overview.needs.integrity_pause': 'integrity pause',
  'overview.needs.stalled': 'stalled',
  'overview.needs.condition': 'needs a fix',
  'overview.needs.held': 'held',
  'overview.needs.landing_failed': 'landing failed',
  'overview.needs.findings': 'new findings',
  'overview.needs.setup': 'setup',
  'overview.state.running': 'running',
  'overview.state.pausing':
    'pausing {point, select, task {after the task} other {after the phase}}',
  'overview.state.paused': 'paused',
  'overview.state.waiting': 'waiting for you',
  'overview.state.held': 'held',
  'overview.time.running': '{duration}',
  'overview.time.in_phase': 'in {phase} {duration}',
  'overview.progress.tasks': '{done} of {total} tasks',
  'overview.progress.plans': '{done} of {total} plans',
  'overview.epic.plans': '{running} running · {staged} staged',
  'overview.plan.belongs_to': 'in {epic}',
  'overview.plan.pull_request': '#{number}',
  'overview.plan.off_track': 'off track: {signal}',
  'overview.group.alongside': 'alongside',
  'overview.landing.removal':
    'removing {count, plural, one {# moved document} other {# moved documents}} from git · {state}',
  'overview.landing.unit': 'landing {unit} · {state}',
  'overview.connection.lost': 'Not connected to the runtime; reconnecting …',
  'overview.fallback.ended': '{item} has ended; showing its project.',
  'overview.ended.title': 'Ended work',
  'overview.ended.open': 'Ended work',
  'overview.ended.empty': 'Nothing in {project} has ended yet.',
  'overview.ended.outcome':
    '{outcome, select, merged {merged} no_change {no change} handed_back {handed back} returned {returned} cancelled {cancelled} abandoned {abandoned} released {released} aborted {aborted} other {ended}}',
  'overview.ended.epic_end':
    '{end, select, completed {completed} cancelled {cancelled} released {released} other {ended}}',
  'overview.ended.reason': 'Reason: {reason}',
  'overview.ended.time': 'ended {time}',
  'overview.ended.page': 'page {page} of {pages}',
  'overview.ended.newer': 'Newer',
  'overview.ended.older': 'Older',
  'overview.ended.as_it_ended': '{item} has ended ({outcome}, {time}); this is how it ended.',
};

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
