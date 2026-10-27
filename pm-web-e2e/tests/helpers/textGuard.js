/**
 * Text guard of CC-P-09.
 *
 * It collects the visible text of a page and reports the six classes of defect the interface
 * review found, without failing: raw enums, state ids and placeholders that reach the operator
 * as catalogue keys instead of words, ISO timestamps beside the localised times, emoji as
 * interface iconography, and interface words of the other language inside a localised page.
 *
 * The guard only reads the DOM. Content the model writes carries `data-model-text` and is
 * skipped. Raw enums and state ids are also skipped in code and in chips whose class token
 * starts with `badge`, `chip` or `tag` (`badge--danger`, `badge-subject`); a prefixed tag such
 * as `entry-kind-tag` or `status-tag` is read, because there the raw identifier is the defect.
 * The other four rules report everywhere outside model-written content.
 *
 * Evidence for the review `doc/claude_review/cross-cutting/proposals.adoc#CC-P-09`.
 */

/** Rule names, the vocabulary a report and the baseline use. */
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
const FOREIGN_INTERFACE_WORDS = Object.freeze({
  de: Object.freeze(['running', 'done', 'failed', 'paused', 'busy', 'idle', 'ago']),
  en: Object.freeze(['läuft', 'fertig', 'fehlgeschlagen', 'pausiert', 'beschäftigt', 'untätig', 'vor']),
});

/**
 * The word the guard records for an ISO timestamp.
 *
 * The timestamps a runtime reports are generated per run, so the baseline compares the rule
 * that fired, not the instant that happened to be rendered.
 */
const ISO_TIME_PLACEHOLDER = '<iso-time>';

/** How often the guard re-reads a page that is still filling, and how long it waits between reads. */
const SETTLE_ATTEMPTS = 20;
const SETTLE_INTERVAL_MS = 150;

/** How many quiet windows in a row a page has to show before its text is taken. */
const SETTLE_QUIET_WINDOWS = 3;

/**
 * Reads every accepted text node of the page in the browser and collects the violations.
 *
 * Runs inside the page, so it carries its own rules and selects nothing from this module.
 *
 * @param {{ foreignWords: Array<string>, isoPlaceholder: string }} options
 * @returns {Array<{ rule: string, word: string, context: string }>} the violations, sorted by
 *   rule and then word, so a baseline entry has one stable position
 */
function collectInPage({ foreignWords, isoPlaceholder }) {
  const SKIPPED_SUBTREES = '[data-model-text], script, style, noscript, template, svg, head';
  const CODE_TAGS = new Set(['code', 'pre', 'kbd', 'samp']);
  const CHIP_CLASS = /^(badge|chip|tag)(-|--|$)/;
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
      return element.checkVisibility({
        visibilityProperty: true,
        contentVisibilityAuto: true,
        opacityProperty: true,
      });
    }
    return element.offsetParent !== null;
  }

  /** @param {Element} element @returns {boolean} whether a chip or a code block wraps it */
  function isChipOrCode(element) {
    for (let node = element; node !== null; node = node.parentElement) {
      if (node === document.body) {
        return false;
      }
      if (CODE_TAGS.has(node.tagName.toLowerCase())) {
        return true;
      }
      for (const name of node.classList ?? []) {
        if (CHIP_CLASS.test(name)) {
          return true;
        }
      }
    }
    return false;
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
        'giu',
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
    scan(text, { context: describe(element), inChip: isChipOrCode(element) });
  }

  // A rendered control shows its placeholder and, for the controls that display it, its value as
  // text. A radio's value is an internal key the operator never sees, so a select contributes the
  // selected option's label, never the option value.
  const VALUE_DISPLAYING_TYPES = new Set([
    'date',
    'datetime-local',
    'email',
    'month',
    'number',
    'password',
    'search',
    'tel',
    'text',
    'time',
    'url',
    'week',
  ]);
  function displayedValue(control) {
    const tag = control.tagName.toLowerCase();
    if (tag === 'select') {
      return control.selectedOptions?.[0]?.textContent ?? null;
    }
    if (tag === 'textarea') {
      return control.value;
    }
    const type = (control.getAttribute('type') ?? 'text').toLowerCase();
    return VALUE_DISPLAYING_TYPES.has(type) ? control.value : null;
  }
  for (const control of document.querySelectorAll('input, textarea, select')) {
    if (!isRendered(control) || control.closest(SKIPPED_SUBTREES)) {
      continue;
    }
    const context = describe(control);
    const inChip = isChipOrCode(control);
    const texts = [control.getAttribute('placeholder'), displayedValue(control)];
    for (const text of texts) {
      if (text) {
        scan(String(text), { context, inChip });
      }
    }
  }

  return violations.sort((left, right) => {
    if (left.rule !== right.rule) {
      return left.rule < right.rule ? -1 : 1;
    }
    if (left.word === right.word) {
      return 0;
    }
    return left.word < right.word ? -1 : 1;
  });
}

/**
 * Orders two entries by rule and then by word, with a plain code-unit comparison so a written
 * baseline stays byte-identical across machines.
 *
 * @param {{ rule: string, word: string }} left the first entry
 * @param {{ rule: string, word: string }} right the second entry
 * @returns {number} the comparison result
 */
function compareEntries(left, right) {
  if (left.rule !== right.rule) {
    return left.rule < right.rule ? -1 : 1;
  }
  if (left.word === right.word) {
    return 0;
  }
  return left.word < right.word ? -1 : 1;
}

/**
 * Collects the text-guard violations of the page that is open.
 *
 * A view fills asynchronously, so the page is read until the document has stopped changing: a
 * mutation observer counts DOM changes, and the violations are taken once three consecutive
 * windows are quiet and the lists agree. A page that never quiets down is an error, not a
 * silent partial read.
 *
 * @param {import('@playwright/test').Page} page the page to read
 * @param {{ locale: 'de' | 'en' }} options the interface locale the page is shown in; words of
 *   the other language are reported in it
 * @returns {Promise<Array<{ rule: string, word: string, context: string }>>} the violations
 * @throws {TypeError} when the locale is neither `de` nor `en`
 * @throws {Error} when the page did not settle within the read budget
 */
export async function collectTextGuardViolations(page, options) {
  const { locale } = options;
  const foreignWords = FOREIGN_INTERFACE_WORDS[locale];
  if (!foreignWords) {
    throw new TypeError(`The text guard covers the locales de and en, not ${String(locale)}.`);
  }
  const read = () =>
    page.evaluate(collectInPage, { foreignWords, isoPlaceholder: ISO_TIME_PLACEHOLDER });
  const countChanges = () =>
    page.evaluate(() => {
      const observer = window.__textGuardObserver;
      const seen = window.__textGuardChanges;
      window.__textGuardChanges = 0;
      return { seen, observing: Boolean(observer) };
    });
  const startObserving = () =>
    page.evaluate(() => {
      window.__textGuardObserver?.disconnect();
      window.__textGuardChanges = 0;
      window.__textGuardObserver = new MutationObserver((records) => {
        window.__textGuardChanges += records.length;
      });
      window.__textGuardObserver.observe(document.body, {
        subtree: true,
        childList: true,
        characterData: true,
      });
    });

  await startObserving();
  let previous = await read();
  let quietWindows = 0;
  for (let attempt = 0; attempt < SETTLE_ATTEMPTS; attempt += 1) {
    await page.waitForTimeout(SETTLE_INTERVAL_MS);
    const { seen } = await countChanges();
    const current = await read();
    const unchanged = JSON.stringify(current) === JSON.stringify(previous);
    previous = current;
    if (seen > 0) {
      quietWindows = 0;
      continue;
    }
    quietWindows += 1;
    if (unchanged && quietWindows >= SETTLE_QUIET_WINDOWS) {
      await page.evaluate(() => window.__textGuardObserver?.disconnect());
      return current;
    }
  }
  await page.evaluate(() => window.__textGuardObserver?.disconnect());
  throw new Error(
    `The page did not settle in ${SETTLE_ATTEMPTS} reads of ${SETTLE_INTERVAL_MS} ms; its text is still changing.`,
  );
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
  return [...seen.values()].sort((left, right) => compareEntries(left, right));
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
  return `${violation.rule}: ${violation.word}${location}`;
}
