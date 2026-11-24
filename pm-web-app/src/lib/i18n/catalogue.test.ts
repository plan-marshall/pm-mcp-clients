import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * The catalogue guard: every user-facing text of the web app is loaded from the message
 * catalogue, in English and in German.
 *
 * The check reads every source of the package and asserts that
 *
 * 1. no `.svelte` file writes a user-facing literal (a text node, a `title`/`placeholder`/
 *    `aria-label` string, or a button label) instead of a `$t(...)` look-up,
 * 2. no `.svelte` or `.ts` file hides a catalogue gap behind a `$t(...) || '...'` fallback,
 * 3. `src/lib/i18n/index.ts` carries no embedded `fallbackMessages` dictionary, and
 * 4. every key a source looks up exists in the catalogue.
 */

const SOURCE_DIR = path.resolve(__dirname, '../..');
const I18N_SOURCE = path.resolve(__dirname, 'index.ts');

/**
 * The act keys the catalogue retired when the shared acts got one key each
 * (`texts-and-languages.adoc`, Keys: `common.action.back|close|cancel|save|retry|later`).
 *
 * They are named here on purpose: this is the absence assertion a put-back key fails.
 */
const RETIRED_ACT_KEYS = [
  'web.common.back',
  'web.common.close',
  'web.common.save',
  'web.common.cancel',
  'web.common.retry',
  'common.actions.back',
  'common.actions.back.title',
  'common.back',
  'common.close',
  'common.save',
  'decision.waiver.later',
  'decomposition.drop',
  'start.decomposition.drop',
];

/**
 * The kind, wait and filter keys retired when each concept got one key
 * (`texts-and-languages.adoc`, Keys: `common.kind.*`, `common.filter.label`).
 */
const RETIRED_KIND_AND_FILTER_KEYS = [
  'overview.kind.project',
  'overview.find.kind.project',
  'overview.find.kind.epic',
  'overview.find.kind.plan',
  'overview.find.kind.decision',
  'overview.find.kind.report',
  'overview.needs.operator_wait',
  'decisions.filter.label',
  'report.filter.label',
];

/**
 * Texts that are not UI copy: they are wire values, ids, or the punctuation of a layout.
 */
const ALLOWED_LITERALS = new Set([
  'en',
  'de',
  'same',
  'copy',
  'move',
  'ask',
  'roles',
  'store',
  'providers',
  'defaults',
  'light',
  'dark',
  'on',
  'off',
  'none',
  'complete',
  'partial',
  'not_reported',
  'button',
  'submit',
  'reset',
  'primary',
  'secondary',
  'danger',
  'ghost',
  'dialog',
  'alert',
  'status',
  'img',
  'text',
  'password',
  'number',
  'radio',
  'checkbox',
  'numeric',
  'short',
  '2-digit',
  'multipart',
]);

/**
 * @param directory the directory to walk
 * @returns every file below it, recursively
 */
function walk(directory: string): string[] {
  const found: string[] = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...walk(full));
    } else {
      found.push(full);
    }
  }
  return found;
}

/** Every source file of the package, with its path relative to the source root. */
const ALL_SOURCES = walk(SOURCE_DIR).map((file) => ({
  relative: path.relative(SOURCE_DIR, file),
  full: file,
}));

const SVELTE_SOURCES = ALL_SOURCES.filter((file) => file.relative.endsWith('.svelte'));

/** Sources that look up catalogue keys; the tests' own fixtures are left out. */
const LOOKUP_SOURCES = ALL_SOURCES.filter(
  (file) =>
    (file.relative.endsWith('.svelte') || file.relative.endsWith('.ts')) &&
    !file.relative.endsWith('.test.ts') &&
    !file.relative.startsWith('test/'),
);

function readSource(full: string): string {
  return fs.readFileSync(full, 'utf-8');
}

/** Strips the script block, so only the markup's own text is inspected. */
function markupOf(source: string): string {
  return source.replace(/<script[\s\S]*?<\/script>/g, '').replace(/<style[\s\S]*?<\/style>/g, '');
}

/**
 * @param source the markup
 * @returns the text nodes and attribute values that are literal copy
 */
function literalTexts(source: string): string[] {
  const found: string[] = [];

  // A text node between tags that carries more than punctuation or a single symbol.
  const textNodes = source.match(/>([^<>{}]+)</g) ?? [];
  for (const node of textNodes) {
    const text = node.slice(1, -1).trim();
    if (!text) continue;
    if (!/[A-Za-z]{3,}/.test(text)) continue;
    if (text.startsWith('{') || text.endsWith('}')) continue;
    found.push(text);
  }

  // User-facing attributes.
  const attributes = source.matchAll(
    /\b(title|placeholder|aria-label|alt|label|summary|legend)\s*=\s*"([^"]*)"/g,
  );
  for (const match of attributes) {
    const value = match[2].trim();
    if (!value || value.includes('{') || value.startsWith('$t(')) continue;
    if (!/[A-Za-z]{2,}/.test(value)) continue;
    if (ALLOWED_LITERALS.has(value)) continue;
    found.push(`${match[1]}="${value}"`);
  }

  return found;
}

/**
 * Finds every `$t(...)` whose result is defaulted with a string.
 *
 * The catalogue is the only source of copy: a fallback hides a missing key, so the guard fails
 * on the expression instead of letting the view show English where the catalogue has a gap.
 *
 * @param source a source file's text
 * @returns the offsets of the offending `$t(` calls
 */
function fallbackOffsets(source: string): number[] {
  const offsets: number[] = [];
  let index = source.indexOf('$t(');
  while (index !== -1) {
    let depth = 0;
    let cursor = index + 2;
    for (; cursor < source.length; cursor++) {
      if (source[cursor] === '(') depth++;
      if (source[cursor] === ')') {
        depth--;
        if (depth === 0) {
          cursor++;
          break;
        }
      }
    }
    const suffix = source.slice(cursor).match(/^\s*(\|\||\?\?)\s*(['"`])/);
    if (suffix) {
      offsets.push(index);
    }
    index = source.indexOf('$t(', cursor);
  }
  return offsets;
}

/**
 * @param source a source file's text
 * @param offset an offset into it
 * @returns the 1-based line of that offset
 */
function lineOf(source: string, offset: number): number {
  return source.slice(0, offset).split('\n').length;
}

describe('the catalogue carries every text of the package', () => {
  it('has the same keys in English and in German', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(Object.keys(en).length).toBeGreaterThan(0);
    const missingInDe = Object.keys(en).filter((key) => !(key in de));
    const missingInEn = Object.keys(de).filter((key) => !(key in en));

    expect(missingInDe).toEqual([]);
    expect(missingInEn).toEqual([]);
  });

  it('carries the keys this package renders', () => {
    const en = loadMessages('en');
    const required = [
      'onboard.entry.title',
      'onboard.entry.intro',
      'onboard.entry.step.language',
      'onboard.entry.step.plans',
      'onboard.mode.auto',
      'onboard.mode.guided',
      'onboard.language.interface_label',
      'onboard.language.model_label',
      'onboard.harness.choose.found',
      'onboard.harness.choose.action',
      'onboard.harness.choose.default_label',
      'onboard.harness.probe.cause',
      'onboard.catalogue.select.action',
      'onboard.roles.review.action',
      'onboard.store.choose.action',
      'onboard.providers.select.action',
      'onboard.defaults.review.action',
      'onboard.auto.result.aspect',
      'onboard.completed.title',
      'onboard.completed.readonly',
      'onboard.completed.show_step',
      'onboard.completed.to_projects',
      'onboard.link.set',
      'onboard.link.accept',
      'web.pair.page.title',
      'web.pair.page.invalid',
      'web.pair.page.offline',
      'web.pair.qr.label',
      'web.settings.page.title',
      'web.settings.conflict.title',
      'web.settings.conflict.apply_again',
      'web.settings.conflict.keep',
      'web.settings.credentials.rotate_title',
      'web.settings.credentials.secret',
      'web.settings.devices.loading',
      'web.settings.devices.others_title',
      'web.entry.lan_off.title',
      'web.entry.lan_off.reason',
      'start.plan.title',
      'start.epic.title',
      'start.step.sources',
      'start.step.ready',
      'start.step.started',
      'start.step.decomposition',
      'start.choose.title',
      'start.choose.sources',
      'start.choose.recipe',
      'start.choose.no_recipe',
      'start.gate.machine',
      'start.gate.project',
      'start.gate.continue_onboarding',
      'start.gate.continue_project',
      'start.error',
      'start.field.prompt',
      'start.epic.adopt.offer',
      'start.epic.adopt.action',
      'start.epic.adopt.new',
      'start.epic.adopt.failed',
      'start.sources.title',
      'start.source.description',
      'start.source.documents',
      'start.source.issue',
      'start.source.lesson',
      'start.source.local_changes',
      'start.source.pull_request',
      'start.source.add',
      'start.source.remove_it',
      'start.source.retry',
      'start.sources.waits',
      'start.sources.continue',
      'start.sources.chat',
      'start.sources.no_lessons',
      'start.chat.subject',
      'start.chat.waits',
      'start.chat.proposed',
      'start.ready.starts_from',
      'start.ready.title',
      'start.ready.id',
      'start.ready.id_from_title',
      'start.ready.start_plan',
      'start.ready.start_epic',
      'start.ready.epic_auto',
      'start.sources.local_changes_landing',
      'start.create.failed',
      'start.started.title',
      'start.started.follow',
      'start.started.back',
      'start.recipe.choose.title',
      'start.recipe.parameters.title',
      'start.recipe.proposed',
      'start.recipe.no_option',
      'start.recipe.no_module',
      'start.recipe.run',
      'start.entry.continue',
      'start.entry.abandon',
      'start.decomposition.title',
      'start.decomposition.drafting',
      'start.decomposition.plans',
      'start.decomposition.keep',
      'start.decomposition.edit',
      'start.decomposition.add',
      'start.decomposition.instruct.title',
      'start.decomposition.read_back.title',
      'start.decomposition.chat',
      'start.decomposition.accept',
      'start.decomposition.instruct_again',
      'start.decomposition.discard',
      'start.decomposition.draft_again',
      'start.decomposition.agree',
      'start.decomposition.checks_fail',
      'start.decomposition.checks_pass',
      'start.decomposition.committed',
      'start.decomposition.then',
      'start.decomposition.overlap',
      'start.state.checking',
      'start.state.fetching',
      'start.state.failed',
      'start.state.screening',
      'start.state.stored',
      'start.state.copied',
      'start.state.moved',
      'start.state.flagged',
      'start.state.unscreened',
      'start.state.untrusted',
      'start.cause.not_readable',
      'start.cause.unknown_forge',
      'start.cause.too_large',
      'start.cause.no_screener',
      'start.cause.link_outside',
      'start.cause.not_found',
      'start.cause.not_granted',
      'start.cause.no_credential',
      'start.cause.rate_limited',
      'start.cause.screening_failed',
      'start.cause.lesson_claimed',
      'start.cause.lesson_retired',
      'start.documents.directory',
      'start.documents.mode.title',
      'start.documents.mode.copy',
      'start.documents.mode.move',
      'start.documents.mode.preselected',
      'start.documents.mode.always',
      'start.documents.loopback_only',
      'start.documents.root_checkout',
      'start.documents.root_home',
      'start.documents.add',
      'start.issue.field',
      'start.issue.grant.title',
      'start.issue.grant.once',
      'start.issue.grant.project',
      'start.issue.grant.no',
      'start.issue.flagged.title',
      'start.issue.flagged.accept',
      'start.unscreened.accept',
      'start.pr.field',
      'start.pr.files_untrusted',
      'start.lesson.accept',
      'start.lesson.retire',
      'start.lesson.choose_another',
      'start.local_changes.add',
      'start.local_changes.not_now',
      'start.local_changes.changed',
      'report.completion.outcome',
      'report.completion.reason',
      'report.completion.ran',
      'report.completion.tokens',
      'report.list.title',
      'report.list.empty',
      'web.frame.nav.reports',
      'web.frame.nav.decisions',
      'web.frame.home',
      'web.frame.home.title',
      'designer.plan.open',
      'designer.plan.open.title',
      'decisions.list.title',
      'decisions.list.empty',
      'system.view.title',
      'system.cleanup.title',
      'system.cleanup.summary.title',
      'project.explorer.title',
      'overview.running.title',
      'plan.view.title',
      'epic.view.title',
      'common.breadcrumb',
      'common.action.retry',
      'common.link.new_tab',
      'common.pull_request.chip',
      'common.kind.project',
      'common.kind.epic',
      'common.kind.plan',
      'common.kind.side_job',
      'common.kind.decision',
      'common.kind.report',
      'common.kind.machine',
    ];
    for (const key of required) {
      expect(en[key], `Key '${key}' must exist in the catalogue`).toBeTruthy();
    }
  });

  it('writes no user-facing literal in any screen', () => {
    const offenders: string[] = [];
    for (const source of SVELTE_SOURCES) {
      for (const literal of literalTexts(markupOf(readSource(source.full)))) {
        offenders.push(`${source.relative}: ${literal}`);
      }
    }
    expect(offenders).toEqual([]);
  });

  it('hides no catalogue gap behind a fallback in any svelte or ts source', () => {
    const offenders: string[] = [];
    for (const source of LOOKUP_SOURCES) {
      const text = readSource(source.full);
      for (const offset of fallbackOffsets(text)) {
        offenders.push(`${source.relative}:${lineOf(text, offset)}`);
      }
    }
    expect(offenders).toEqual([]);
  });

  it('keeps no embedded fallback dictionary in the i18n module', () => {
    const source = readSource(I18N_SOURCE);
    expect(source).not.toContain('fallbackMessages');
  });

  it('retires every duplicate act key the shared family replaced', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    const present = RETIRED_ACT_KEYS.filter((key) => key in en || key in de);

    expect(present).toEqual([]);
  });

  it('retires the kind, wait and filter keys the shared ones replaced', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    const present = RETIRED_KIND_AND_FILTER_KEYS.filter((key) => key in en || key in de);

    expect(present).toEqual([]);
  });

  it('looks up only keys the catalogue carries', () => {
    const en = loadMessages('en');
    const offenders: string[] = [];
    for (const source of LOOKUP_SOURCES) {
      const text = readSource(source.full);
      for (const match of text.matchAll(/\$t\('([^']+)'/g)) {
        const key = match[1];
        // A key that ends in a dot is the prefix of a computed key (`overview.state.${state}`);
        // the parity test checks that both locales carry the same keys, not that every value a
        // computed key can select has an entry, which the enum guard and the vocabulary own.
        if (key.endsWith('.')) continue;
        if (!(key in en)) {
          offenders.push(`${source.relative}: ${key}`);
        }
      }
    }
    expect(offenders).toEqual([]);
  });
});

describe('the catalogue carries the corrected strings of CC-F-05', () => {
  /** A terminal key hint (`r) entfernen`): a key press that only a terminal client sends. */
  const TERMINAL_KEY_HINT = /^\s*[a-zA-Z]\)\s/;

  it('keeps every terminal key hint out of a text the web app renders', () => {
    const offenders: string[] = [];
    for (const locale of ['en', 'de']) {
      const messages = loadMessages(locale);
      for (const source of LOOKUP_SOURCES) {
        const text = readSource(source.full);
        for (const match of text.matchAll(/\$t\('([^']+)'/g)) {
          const value = messages[match[1]];
          if (value !== undefined && TERMINAL_KEY_HINT.test(value)) {
            offenders.push(`${locale}: ${match[1]} = ${value}`);
          }
        }
      }
    }
    expect(offenders).toEqual([]);
  });

  it('starts a started plan with the quoted title and one sentence end', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(en['start.started.title']).toBe('\u201c{name}\u201d has started.');
    expect(de['start.started.title']).toBe('\u201e{name}\u201c ist gestartet.');
  });

  it('names both cleanup durations instead of putting the question after the number', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(en['web.settings.cleanup.keep']).toBe('Keep finished work for');
    expect(en['web.settings.cleanup.days']).toBe('days');
    expect(en['web.settings.cleanup.remind']).toBe('Reminder after');
    expect(en['web.settings.cleanup.days_after']).toBe('days');

    expect(de['web.settings.cleanup.keep']).toBe('Beendete Arbeit');
    expect(de['web.settings.cleanup.days']).toBe('Tage aufbewahren');
    expect(de['web.settings.cleanup.remind']).toBe('Erinnerung nach');
    expect(de['web.settings.cleanup.days_after']).toBe('Tagen');
  });

  it('says the stopping sentence once on the onboarding entry', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');
    const STOPPING_SENTENCE_EN = 'You can stop at any point;';
    const STOPPING_SENTENCE_DE = 'Du kannst jederzeit aufh\u00f6ren;';

    expect(en['onboard.entry.intro'].split(STOPPING_SENTENCE_EN)).toHaveLength(2);
    expect(de['onboard.entry.intro'].split(STOPPING_SENTENCE_DE)).toHaveLength(2);
  });

  it('names the provider field by its own word in the failure detail', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(en['onboard.providers.field']).toBe('Provider');
    expect(de['onboard.providers.field']).toBe('Provider');
  });

  it('names a completed machine without repeating the title as the body', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(en['onboard.completed.summary']).not.toBe(en['onboard.completed.title']);
    expect(de['onboard.completed.summary']).not.toBe(de['onboard.completed.title']);
  });

  it('shows a system value without repeating the label beside it', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(en['system.work.tasks']).toBe('{running} running \u00b7 {queued} queued');
    expect(de['system.work.tasks']).toBe('{running} laufen \u00b7 {queued} in der Warteschlange');
    expect(en['system.jobs.slots']).toBe(
      '{used} of {total, plural, one {# slot} other {# slots}}{queued, plural, =0 {} other { \u00b7 # queued}}',
    );
    expect(de['system.jobs.slots']).toBe(
      '{used} von {total, plural, one {# Slot} other {# Slots}}{queued, plural, =0 {} other { \u00b7 # wartend}}',
    );
    expect(en['system.runtime.listeners']).toMatch(/^socket/);
    expect(de['system.runtime.listeners']).toMatch(/^Socket/);
  });

  it('answers a decision with the chosen option instead of a channel and an index', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(en['decision.outcome.answered']).toBe('Decided: {answer}');
    expect(de['decision.outcome.answered']).toBe('Entschieden: {answer}');
    // An answer from another client names where and how it came (`decisions.adoc#outcomes`).
    expect(en['decision.outcome.answered_elsewhere']).toBe(
      'answered in {channel}, {device}: {answer}',
    );
    expect(de['decision.outcome.answered_elsewhere']).toBe(
      'beantwortet in {channel}, {device}: {answer}',
    );
  });

  it('agrees on the German term for the epic waiting text', () => {
    const de = loadMessages('de');
    expect(de['control.epic.next_waiting']).toBe(
      'Das Epic wird noch gelesen; der nächste Plan kann noch nicht starten.',
    );
  });

  it('pins the German wording of the token figure literally', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    // The token figure is the shared label plus its unit; the German one is a literal, not a copy
    // of the English spelling with the number inserted.
    expect(de['common.tokens.total']).toBe('Tokens {figure}');
    expect(de['common.tokens.millions']).toBe('{value} Mio.');
    expect(de['common.tokens.thousands']).toBe('{value} Tsd.');
    expect(de['common.tokens.split']).toBe('{total} ({in} rein \u00b7 {out} raus)');
    expect(en['common.tokens.total']).toBe('tokens {figure}');
    expect(en['common.tokens.millions']).toBe('{value} M');
  });

  it('labels the remember answer every consent state offers', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    // The engine offers `yes`, `yes_remember` where the kind can be remembered, and `no` on every
    // consent state (`14-machine-onboarding.adoc:178`, `19-…:105,201,251,283`,
    // `15-project-onboarding.adoc:33,49`). `linkLabel` builds `onboard.link.{id}` from the id at
    // run time, so only a named key keeps the raw id off the screen; the project onboarding draws
    // every consent answer through that label (PLAN-59 D5c).
    expect(en['onboard.link.yes_remember']).toBe("Yes, and don't ask again");
    expect(de['onboard.link.yes_remember']).toBe('Ja, und nicht mehr fragen');
  });
});
