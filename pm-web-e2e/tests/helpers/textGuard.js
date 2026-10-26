/**
 * Text guard of CC-P-09.
 *
 * It collects the visible text of a page and reports the six classes of defect the interface
 * review found, without failing: raw enums, state ids and placeholders that reach the operator
 * as catalogue keys instead of words, ISO timestamps beside the localised times, emoji as
 * interface iconography, and interface words of the other language inside a localised page.
 *
 * The guard only reads the DOM. Content the model writes carries `data-model-text` and is
 * skipped, and so are chips (`badge`, `chip`, `tag`) and code, where a raw identifier is the
 * intended presentation rather than a leak.
 *
 * Evidence for the review `doc/claude_review/cross-cutting/proposals.adoc#CC-P-09`.
 */

/** Rule names, in the order a report lists them. */
export const TEXT_GUARD_RULES = Object.freeze([
  'raw-enum',
  'state-id',
  'placeholder',
  'iso-time',
  'emoji',
  'foreign-word',
]);

/**
 * The interface words of the other language, per locale.
 *
 * `de` lists what must not appear in a German interface, `en` the German counterparts. They are
 * state labels and relative time words: the terms the review saw untranslated on one screen.
 */
export const FOREIGN_INTERFACE_WORDS = Object.freeze({
  de: Object.freeze(['running', 'done', 'failed', 'paused', 'busy', 'idle', 'ago']),
  en: Object.freeze(['läuft', 'fertig', 'fehlgeschlagen', 'pausiert', 'beschäftigt', 'untätig', 'vor']),
});

/**
 * The word the guard records for an ISO timestamp.
 *
 * The timestamps a runtime reports are generated per run, so the baseline compares the rule
 * that fired, not the instant that happened to be rendered.
 */
export const ISO_TIME_PLACEHOLDER = '<iso-time>';

/**
 * Reads every accepted text node of the page in the browser and collects the violations.
 *
 * Runs inside the page, so it carries its own rules and selects nothing from this module.
 *
 * @param {{ locale: string, foreignWords: Array<string>, isoPlaceholder: string }} options
 * @returns {Array<{ rule: string, word: string, context: string }>} the violations, sorted
 */
function collectInPage({ locale, foreignWords, isoPlaceholder }) {
  const SKIPPED_SUBTREES = '[data-model-text], script, style, noscript, template, svg, head';
  const CHIPS_AND_CODE = 'code, pre, kbd, samp, [class*="badge"], [class*="chip"], [class*="tag"]';
  const FIXED_RULES = [
    { rule: 'raw-enum', pattern: /\b[a-z]+_[a-z_]+\b/g, skipInChip: true },
    { rule: 'state-id', pattern: /\b[a-z]+\.[a-z][a-z-]+\b/g, skipInChip: true },
    { rule: 'placeholder', pattern: /\{[a-z_]+\}/g, skipInChip: false },
    { rule: 'iso-time', pattern: /\d{4}-\d{2}-\d{2}T\d{2}:/g, skipInChip: false },
    { rule: 'emoji', pattern: /\p{Extended_Pictographic}/gu, skipInChip: false },
  ];
  const violations = [];
  const seen = new Set();

  function report(rule, word, context) {
    const key = `${rule}\u0000${word}`;
    if (seen.has(key)) {
      return;
    }
    seen.add(key);
    violations.push({ rule, word, context });
  }

  function describe(element) {
    const tag = element.tagName.toLowerCase();
    const firstClass = (element.getAttribute('class') ?? '').split(/\s+/).find(Boolean);
    return firstClass ? `${tag}.${firstClass}` : tag;
  }

  function isRendered(element) {
    if (typeof element.checkVisibility === 'function') {
      return element.checkVisibility();
    }
    return element.offsetParent !== null;
  }

  function scan(text, { context, inChip }) {
    for (const rule of FIXED_RULES) {
      if (inChip && rule.skipInChip) {
        continue;
      }
      for (const match of text.matchAll(new RegExp(rule.pattern.source, rule.pattern.flags))) {
        const word = rule.rule === 'iso-time' ? isoPlaceholder : match[0];
        report(rule.rule, word, context);
      }
    }
    for (const word of foreignWords) {
      const pattern = new RegExp(
        `(?<!\\p{L})${word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?!\\p{L})`,
        'gu',
      );
      if (pattern.test(text)) {
        report('foreign-word', word, context);
      }
    }
  }

  scan(document.title ?? '', { context: 'title', inChip: false });

  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
    const text = node.nodeValue ?? '';
    if (!text.trim()) {
      continue;
    }
    const element = node.parentElement;
    if (!element || element.closest(SKIPPED_SUBTREES) || !isRendered(element)) {
      continue;
    }
    scan(text, { context: describe(element), inChip: element.closest(CHIPS_AND_CODE) !== null });
  }

  return violations.sort((left, right) =>
    left.rule === right.rule
      ? left.word.localeCompare(right.word)
      : left.rule.localeCompare(right.rule),
  );
}

/**
 * Collects the text-guard violations of the page that is open.
 *
 * @param {import('@playwright/test').Page} page the page to read
 * @param {{ locale: 'de' | 'en' }} options the interface locale the page is shown in; words of
 *   the other language are reported in it
 * @returns {Promise<Array<{ rule: string, word: string, context: string }>>} the violations
 * @throws {TypeError} when the locale is neither `de` nor `en`
 */
export async function collectTextGuardViolations(page, options) {
  const { locale } = options;
  const foreignWords = FOREIGN_INTERFACE_WORDS[locale];
  if (!foreignWords) {
    throw new TypeError(`The text guard covers the locales de and en, not ${String(locale)}.`);
  }
  return page.evaluate(collectInPage, { locale, foreignWords, isoPlaceholder: ISO_TIME_PLACEHOLDER });
}

/**
 * Reduces violations to the comparable form a baseline holds: rule and word, sorted, without
 * the location.
 *
 * @param {Array<{ rule: string, word: string }>} violations the collected violations
 * @returns {Array<{ rule: string, word: string }>} one entry per rule and word
 */
export function normalizeViolations(violations) {
  const seen = new Map();
  for (const { rule, word } of violations) {
    seen.set(`${rule}\u0000${word}`, { rule, word });
  }
  return [...seen.values()].sort((left, right) =>
    left.rule === right.rule
      ? left.word.localeCompare(right.word)
      : left.rule.localeCompare(right.rule),
  );
}

/**
 * Compares a view's baseline entries with the violations a run collected.
 *
 * @param {Array<{ rule: string, word: string }>} baseline the committed entries of the view
 * @param {Array<{ rule: string, word: string }>} actual the entries the run collected
 * @returns {{ added: Array<{ rule: string, word: string }>, removed: Array<{ rule: string, word: string }> }}
 *   `added` are violations the view gained (the guard's failure), `removed` are entries the
 *   baseline still holds for a defect that is gone (the baseline must shrink)
 */
export function diffViolations(baseline, actual) {
  const expected = normalizeViolations(baseline);
  const observed = normalizeViolations(actual);
  const keyOf = (entry) => `${entry.rule}\u0000${entry.word}`;
  const expectedKeys = new Set(expected.map(keyOf));
  const observedKeys = new Set(observed.map(keyOf));
  return {
    added: observed.filter((entry) => !expectedKeys.has(keyOf(entry))),
    removed: expected.filter((entry) => !observedKeys.has(keyOf(entry))),
  };
}

/**
 * Renders a violation for a report line.
 *
 * @param {{ rule: string, word?: string, context?: string }} violation the violation to render
 * @returns {string} `rule: word` with the location when the violation carries one
 */
export function formatViolation(violation) {
  const location = violation.context ? ` (${violation.context})` : '';
  return `${violation.rule}: ${violation.word}${location}`.replace(': undefined', ': -');
}
