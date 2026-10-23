import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * The advisory convention: a control's visible label is identified by a catalogue key `X`, and its
 * hover advisory is a separate key `X.title` whose words add information rather than repeat `X`.
 *
 * The check reads every source of the package and asserts that
 *
 * 1. every `data-advisory` expression reads a catalogue key through `$t`, and only `.title` keys,
 * 2. every referenced `.title` key exists in English and in German, and
 * 3. for an advisory key whose base key `X` exists, the English advisory differs from `X`.
 *
 * The divergence rule covers the advisory surface, not every catalogue key ending in `.title`: a
 * heading such as `start.recipe.parameters.title` is not a hover advisory and may name the same
 * thing as its body text.
 */

const SOURCE_DIR = path.resolve(__dirname, '../..');

/** @param directory the directory to walk @returns every file below it */
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

const SOURCES = walk(SOURCE_DIR)
  .filter((file) => file.endsWith('.svelte') || file.endsWith('.ts'))
  .filter((file) => !file.endsWith('.test.ts'))
  .map((file) => ({
    relative: path.relative(SOURCE_DIR, file),
    text: fs.readFileSync(file, 'utf-8'),
  }));

/**
 * Extracts the expression of every `data-advisory={...}` binding.
 *
 * The braces are balanced by hand, because an advisory may pass a placeholder map.
 *
 * @param source a source file's text
 * @returns each binding's expression
 */
function advisoryExpressions(source: string): string[] {
  const found: string[] = [];
  const marker = 'data-advisory={';
  let index = source.indexOf(marker);
  while (index !== -1) {
    let depth = 1;
    let cursor = index + marker.length;
    for (; cursor < source.length && depth > 0; cursor++) {
      if (source[cursor] === '{') depth++;
      else if (source[cursor] === '}') depth--;
    }
    found.push(source.slice(index + marker.length, cursor - 1));
    index = source.indexOf(marker, cursor);
  }
  return found;
}

/**
 * @param expression an advisory expression
 * @returns the catalogue key of every `$t(...)` call, including both arms of a ternary
 */
function keysIn(expression: string): string[] {
  const found: string[] = [];
  let index = expression.indexOf('$t(');
  while (index !== -1) {
    let depth = 1;
    let cursor = index + '$t('.length;
    for (; cursor < expression.length && depth > 0; cursor++) {
      if (expression[cursor] === '(') depth++;
      else if (expression[cursor] === ')') depth--;
    }
    // The key is the first argument; a placeholder map starts at the first top-level comma.
    const call = expression.slice(index + '$t('.length, cursor - 1);
    let comma = -1;
    let nesting = 0;
    for (let position = 0; position < call.length; position++) {
      if (call[position] === '{' || call[position] === '(') nesting++;
      else if (call[position] === '}' || call[position] === ')') nesting--;
      else if (call[position] === ',' && nesting === 0) {
        comma = position;
        break;
      }
    }
    const keyArgument = comma === -1 ? call : call.slice(0, comma);
    for (const match of keyArgument.matchAll(/'([^']+)'/g)) {
      found.push(match[1]);
    }
    index = expression.indexOf('$t(', cursor);
  }
  return found;
}

interface Advisory {
  file: string;
  key: string;
}

const ADVISORY_EXPRESSIONS: Array<{ file: string; expression: string }> = [];
const ADVISORIES: Advisory[] = [];
for (const source of SOURCES) {
  for (const expression of advisoryExpressions(source.text)) {
    ADVISORY_EXPRESSIONS.push({ file: source.relative, expression });
    for (const key of keysIn(expression)) {
      ADVISORIES.push({ file: source.relative, key });
    }
  }
}

const TITLE_REFERENCES: Advisory[] = [];
for (const source of SOURCES) {
  for (const match of source.text.matchAll(/'([^']+\.title)'/g)) {
    TITLE_REFERENCES.push({ file: source.relative, key: match[1] });
  }
}

describe('the advisory key convention', () => {
  it('passes a catalogue .title key to every data-advisory', () => {
    expect(ADVISORIES.length).toBeGreaterThan(0);

    const literal = ADVISORY_EXPRESSIONS.filter((entry) => !entry.expression.includes('$t(')).map(
      (entry) => `${entry.file}: ${entry.expression.replace(/\s+/g, ' ').slice(0, 80)}`,
    );
    expect(literal).toEqual([]);

    // A dynamic key (`$t(key)`, or a call whose first argument names no literal) cannot be checked
    // against the catalogue, so it is refused here rather than passing silently.
    const withoutKey = ADVISORY_EXPRESSIONS.filter(
      (entry) => keysIn(entry.expression).length === 0,
    ).map((entry) => `${entry.file}: ${entry.expression.replace(/\s+/g, ' ').slice(0, 80)}`);
    expect(withoutKey).toEqual([]);

    const offenders = ADVISORIES.filter((entry) => !entry.key.endsWith('.title')).map(
      (entry) => `${entry.file}: ${entry.key}`,
    );
    expect(offenders).toEqual([]);
  });

  it('carries every referenced .title key in English and in German', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');
    const offenders = TITLE_REFERENCES.filter(
      (entry) => !(entry.key in en) || !(entry.key in de),
    ).map((entry) => `${entry.file}: ${entry.key}`);
    expect(offenders).toEqual([]);
  });

  it('adds information instead of repeating the base label', () => {
    const en = loadMessages('en');
    const offenders = ADVISORIES.filter((entry) => {
      const base = entry.key.slice(0, -'.title'.length);
      return base in en && en[base] === en[entry.key];
    }).map((entry) => `${entry.file}: ${entry.key} repeats ${entry.key.slice(0, -6)}`);
    expect(offenders).toEqual([]);
  });
});
