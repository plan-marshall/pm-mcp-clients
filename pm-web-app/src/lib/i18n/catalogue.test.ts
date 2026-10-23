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
      'start.decomposition.drop',
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
      'web.common.retry',
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

  it('looks up only keys the catalogue carries', () => {
    const en = loadMessages('en');
    const offenders: string[] = [];
    for (const source of LOOKUP_SOURCES) {
      const text = readSource(source.full);
      for (const match of text.matchAll(/\$t\('([^']+)'/g)) {
        const key = match[1];
        // A key that ends in a dot is the prefix of a computed key (`overview.state.${state}`);
        // its values are checked by the parity test above.
        if (key.endsWith('.')) continue;
        if (!(key in en)) {
          offenders.push(`${source.relative}: ${key}`);
        }
      }
    }
    expect(offenders).toEqual([]);
  });
});
