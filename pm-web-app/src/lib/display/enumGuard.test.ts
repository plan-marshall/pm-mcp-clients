import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { ENUM_FIELDS } from './enumFields';

/**
 * The enum guard of CC-P-01: no screen prints a field the API enumerates without the display
 * layer.
 *
 * The guard reads every `.svelte` file of the package and inspects the text nodes of its markup —
 * the place where a value reaches the operator. A field that `enumFields.ts` lists may appear
 * there only inside `displayState`, so a view that interpolates `{worker.state}` fails instead of
 * showing `waiting_for_you` on the screen.
 *
 * Attribute values are not read: a class token (`badge--state-{state}`) is not copy, and a value
 * handed to a child component is printed by that component, where the guard reads it.
 */

const SOURCE_DIR = path.resolve(__dirname, '../..');

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

/**
 * @param source a `.svelte` file's text
 * @returns its markup, with the script and style blocks removed
 */
function markupOf(source: string): string {
  return source.replace(/<script[\s\S]*?<\/script>/g, '').replace(/<style[\s\S]*?<\/style>/g, '');
}

/**
 * @param markup the markup of a `.svelte` file
 * @returns its text nodes only: every tag is removed, so attribute expressions are not read
 */
function textOf(markup: string): string {
  return markup.replace(/<[^>]*>/g, '');
}

/**
 * @param text the text nodes of a `.svelte` file
 * @returns every `{…}` expression they carry, without its braces
 */
function interpolations(text: string): string[] {
  const found: string[] = [];
  let index = 0;
  while (index < text.length) {
    const start = text.indexOf('{', index);
    if (start === -1) break;
    let depth = 0;
    let end = -1;
    for (let cursor = start; cursor < text.length; cursor += 1) {
      if (text[cursor] === '{') depth += 1;
      else if (text[cursor] === '}') {
        depth -= 1;
        if (depth === 0) {
          end = cursor;
          break;
        }
      }
    }
    if (end === -1) break;
    found.push(text.slice(start + 1, end).trim());
    index = end + 1;
  }
  return found;
}

/**
 * @param expression one interpolation's expression
 * @returns whether the markup block rather than a value (`#if`, `:else`, `/each`, `@render`)
 */
function isBlockDirective(expression: string): boolean {
  return /^[#:/@]/.test(expression);
}

/**
 * @param expression one interpolation's expression
 * @returns the expression without its string literals, so a catalogue key that carries a field's
 *   name is not mistaken for a read of it
 */
function withoutLiterals(expression: string): string {
  return expression.replace(/'[^']*'|"[^"]*"|`[^`]*`/g, "''");
}

/**
 * @param expression one interpolation's expression
 * @returns whether the expression decides something rather than printing a value: a comparison,
 *   a filter, a ternary or another block's condition. Optional chaining (`?.`), nullish
 *   coalescing (`??`) and `||` (`{node.state || 'running'}` prints the left operand) are no
 *   decision: they still print the value the expression reads.
 */
function decidesRatherThanPrints(expression: string): boolean {
  if (/===|!==|==|!=|&&|<|>|!/.test(expression)) {
    return true;
  }
  return /(?<!\?)\?(?![.?])/.test(expression);
}

/**
 * The helpers that name a value themselves, so the guard reads their result rather than their
 * argument. A generic wrapper (`String(node.state)`) is not one of them.
 */
const NAMING_HELPERS = Object.freeze(['stateLabel', 'getNodeStateText', 'consentChannel']);

/**
 * @param expression one interpolation's expression
 * @returns whether the expression hands the value to a helper (`stateLabel(node.state)`) that names
 *   it in its own module. A catalogue look-up is not such a helper: `$t('x', { state: node.state })`
 *   prints the value through the message and is exactly what the guard must catch.
 */
function handsToHelper(expression: string): boolean {
  const call = expression.match(/^([A-Za-z_$][\w$]*)\(/);
  return call !== null && NAMING_HELPERS.includes(call[1]);
}

/**
 * @param expression one interpolation's expression, without its string literals
 * @returns the expression with each balanced `displayState(...)` call removed, so a read beside the
 *   call (`{displayState('plan', a.state) + b.state}`) is still inspected
 */
function withoutDisplayStateCalls(expression: string): string {
  let rest = expression;
  let start = rest.indexOf('displayState(');
  while (start !== -1) {
    let depth = 0;
    let end = -1;
    for (let cursor = start + 'displayState'.length; cursor < rest.length && end === -1; cursor += 1) {
      if (rest[cursor] === '(') depth += 1;
      else if (rest[cursor] === ')') {
        depth -= 1;
        if (depth === 0) end = cursor;
      }
    }
    if (end === -1) break;
    rest = rest.slice(0, start) + rest.slice(end + 1);
    start = rest.indexOf('displayState(');
  }
  return rest;
}

/**
 * @param expression one interpolation's expression
 * @param field a field of `enumFields.ts`
 * @returns whether the expression reads that field as a property of a value
 */
function readsField(expression: string, field: string): boolean {
  return new RegExp(`\\.${field}\\b`).test(expression);
}

/**
 * @param markup the markup of a `.svelte` file, or a fragment written for this test
 * @returns the text interpolations that print an enumerated field without the display layer
 */
function offendersIn(markup: string): string[] {
  const offenders: string[] = [];
  const withoutTags = textOf(markupOf(markup));
  for (const expression of interpolations(withoutTags)) {
    if (isBlockDirective(expression)) continue;
    const rest = withoutDisplayStateCalls(withoutLiterals(expression));
    if (handsToHelper(rest)) continue;
    if (decidesRatherThanPrints(rest)) continue;
    const field = ENUM_FIELDS.find((candidate) => readsField(rest, candidate));
    if (field !== undefined) {
      offenders.push(expression);
    }
  }
  for (const attribute of markupOf(markup).matchAll(
    /\b(aria-label|title|placeholder|alt)=(?:"([^"]*)"|\{([^}]*)\})/g,
  )) {
    const value = attribute[2] ?? attribute[3] ?? '';
    const rest = withoutDisplayStateCalls(withoutLiterals(value));
    if (decidesRatherThanPrints(rest)) continue;
    const field = ENUM_FIELDS.find((candidate) => readsField(rest, candidate));
    if (field !== undefined) {
      offenders.push(`${attribute[1]}=${value}`);
    }
  }
  return offenders;
}

describe('the enum guard', () => {
  it('lists the enumerated fields the display layer names', () => {
    expect(ENUM_FIELDS.length).toBeGreaterThan(0);
  });

  it('flags a bare read, an optional chain, a fallback and a catalogue pass-through', () => {
    const offenders = offendersIn(`
      <p>{node.state}</p>
      <p>{worker?.state}</p>
      <p>{$t('system.workers.count', { state: worker.state })}</p>
      <p>{plan.phase_state ?? 'staged'}</p>
      <p>{node.state || 'running'}</p>
      <p>{displayState('plan', a.state) + b.state}</p>
      <p>{String(node.state)}</p>
      <button aria-label={worker.state}>x</button>
      <p>{task.off_track.signal}</p>
    `);

    expect(offenders).toEqual([
      'node.state',
      'worker?.state',
      "$t('system.workers.count', { state: worker.state })",
      "plan.phase_state ?? 'staged'",
      "node.state || 'running'",
      "displayState('plan', a.state) + b.state",
      'String(node.state)',
      'task.off_track.signal',
      'aria-label=worker.state',
    ]);
  });

  it('passes the display layer, a naming helper and a value a view only compares', () => {
    const offenders = offendersIn(`
      <p>{displayState('worker', worker.state)}</p>
      <p>{stateLabel(item.state)}</p>
      <p>{workers.filter((w) => w.state === 'busy').length}</p>
      <p>{node.state === 'done' ? 'a' : 'b'}</p>
      <p aria-label={$t('system.work.link.plans', { state: displayState('plan', st) })}>x</p>
    `);

    expect(offenders).toEqual([]);
  });

  it('finds no enumerated field printed without the display layer', () => {
    const screens = walk(SOURCE_DIR).filter((file) => file.endsWith('.svelte'));
    expect(screens.length).toBeGreaterThan(0);

    const offenders: string[] = [];
    for (const file of screens) {
      const relative = path.relative(SOURCE_DIR, file);
      const markup = fs.readFileSync(file, 'utf-8');
      for (const expression of offendersIn(markup)) {
        offenders.push(`${relative}: ${expression}`);
      }
    }

    expect(offenders).toEqual([]);
  });
});
