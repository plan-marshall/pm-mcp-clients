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
  'report.list.title': 'Reports',
  'report.list.empty': 'Nothing new. Reports you have read are not shown any more.',
  'report.list.show_older': 'Show {count} older',
  'report.action.details': 'Show the details',
  'report.action.mark_read': 'Mark read',
  'report.action.mark_all_read': 'Mark all shown read',
  'report.arrival.show': 'Show them',
  'report.arrival.show_them': 'Show them',
  'web.frame.nav.reports': 'Reports',
  'web.frame.nav.decisions': 'Decisions',
  'common.actions.back': 'Back',
  'common.actions.read': 'Mark read',
  'decision.blocks.plan': 'blocks the plan',
  'decision.blocks.epic': 'blocks the epic',
  'decision.blocks.setup': "blocks the project's setup",
  'decision.blocks.entry': 'blocks the start',
  'decision.blocks.task': 'blocks one task; the plan goes on',
  'decision.blocks.none': 'blocks nothing',
  'decision.reversibility.reversible': 'can be undone',
  'decision.reversibility.costly': 'costly to undo',
  'decision.reversibility.irreversible': 'cannot be undone',
  'decision.waiting':
    '{subject, select, epic {the epic} setup {the setup of {project}} entry {the start in {project}} other {the plan}} waits for your answer, since {time}',
  'decision.part.position': 'part {n} of {total}',
  'decision.part.depends_on': 'depends on part {n}: {answer}',
  'decision.part.reopened': 'Part {n} was opened again, because part {m} changed.',
  'decision.support.votes':
    '{votes} of {total} votes · {models, plural, one {# model} other {# models}}',
  'decision.support.band.unanimous': 'unanimous',
  'decision.support.band.clear_majority': 'clear majority',
  'decision.support.band.split': 'split',
  'decision.support.band.no_consensus': 'no consensus',
  'decision.support.not_voted': 'not voted on',
  'decision.support.self_reported':
    "the model's own estimate: {value} of 100; models tend to overrate themselves",
  'decision.recommendation.by': 'Recommended by {role}: {option}',
  'decision.recommendation.on_request':
    'The recommendation is one step away, so that you weigh the options first.',
  'decision.dissent': 'Against it ({role}): {reason}',
  'decision.answer.own_words': 'in my own words',
  'decision.answer.note': 'add a note',
  'decision.answer.none_fits':
    'none fits: let {subject, select, epic {the epic} setup {the setup} entry {the start} other {the plan}} think again',
  'decision.answer.later': 'later',
  'decision.interpret.working': '{role} is reading your answer',
  'decision.interpret.unclear_bound':
    'Your words could not be read clearly enough ({reason}). Choose an option, or answer again.',
  'decision.own_words.unqualified':
    'Answering in your own words is not available: no model qualifies for {role}.',
  'decision.refused.invalid_answer': 'This answer does not fit the part: {reason}',
  'decision.refused.dependency_unanswered': 'Answer part {n} first.',
  'decision.readback.understood': 'I understood: {reading}',
  'decision.readback.condition': '{option}, provided {condition}',
  'decision.readback.own_option': 'your own answer: {words}',
  'decision.readback.delegation': 'you leave it to the plan',
  'decision.readback.confirm': 'Correct',
  'decision.readback.edit': 'edit my words',
  'decision.readback.choose': 'choose an option instead',
  'decision.clarify.asks': '{role} asks: {question}',
  'decision.confirm.title': 'Your decision',
  'decision.confirm.action': 'Decide',
  'decision.outcome.answered_elsewhere': 'answered in {channel} ({device}): {answer}',
  'decision.outcome.revised': 'The decision changed: {change}. Please answer again.',
  'decision.outcome.rethink':
    'you asked {subject, select, epic {the epic} setup {the setup of {project}} entry {the start in {project}} other {the plan}} to think again',
  'decision.outcome.asked_anew': 'asked anew after {id}',
  'decision.channel.web': 'the web app',
  'decision.channel.cli': 'the console',
  'decision.notify.ask':
    "Should the web app notify you when a plan, an epic, a project's setup, or a start asks a decision? It names the plan, the epic, the setup, or the start and the kind of decision, never its content.",
  'decision.notify.allow': 'Allow notifications',
  'decision.notify.deny': 'Not now',
  'decision.notify.title':
    '{subject, select, epic {Epic {name}} setup {The setup of {project}} entry {The start in {project}} other {{name}}} asks {kind, select, waiver {for a waiver} other {a decision}}',
  'decision.notify.tab_title': '({count}) {title}',
  'decision.waiver.title': '{plan} asks for a waiver',
  'decision.waiver.gate': 'Gate',
  'decision.waiver.result': 'Result',
  'decision.waiver.asked_by': 'Asked by',
  'decision.waiver.its_reason': 'Its reason',
  'decision.waiver.reason': 'Your reason',
  'decision.waiver.reason_required': 'Give your reason first.',
  'decision.waiver.waive': 'Waive the gate',
  'decision.waiver.decline': 'Decline',
  'decision.waiver.later': 'Later',
  'decision.answer.chat': 'chat about it first',
  'decision.chat.no_own_words':
    'this decision takes no answer in your own words: the chat explains, you choose',
  'decision.chat.proposed_answer': 'Proposed answer{part, select, none {} other {, part {part}}}',
  'decision.chat.use_words': 'Answer in these words',
  'decision.chat.stays_open': '{id} stays open; answer it once the chat has ended.',
  'decision.chat.withdrawn': '{id} is closed: the revision removed the need for it ({reason}).',
  'decision.chat.closed_elsewhere':
    '{id} was answered in {channel} ({device}); the chat has ended.',
  'decision.chat.after_chat': 'answered after a chat',
  'decision.chat.no_role':
    'This question comes from the setup itself: no planning role can chat about it.',
  'decision.chat.none_for_waiver':
    'A waiver request offers no chat: the waiver is your override, not an answer to its content.',
  'decisions.list.title': 'Decisions',
  'decisions.list.empty': 'No open decisions. Running plans will ask when a decision is needed.',
  'decisions.arrival.show_them': 'Show decision',
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
