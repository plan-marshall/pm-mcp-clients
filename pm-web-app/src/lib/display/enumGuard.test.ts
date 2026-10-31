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
 *   a filter, a conditional or another block's condition
 */
function decidesRatherThanPrints(expression: string): boolean {
  return /===|!==|==|!=|&&|\|\||\?|<|>|!/.test(expression);
}

/**
 * @param expression one interpolation's expression
 * @returns whether the expression hands the value to a helper (`stateLabel(node.state)`), which
 *   names it in its own module
 */
function handsToHelper(expression: string): boolean {
  return /^[A-Za-z_$][\w$]*\(/.test(expression);
}

/**
 * @param expression one interpolation's expression
 * @param field a field of `enumFields.ts`
 * @returns whether the expression reads that field as a property of a value
 */
function readsField(expression: string, field: string): boolean {
  return new RegExp(`\\.${field}\\b`).test(expression);
}

describe('the enum guard', () => {
  it('lists the enumerated fields the display layer names', () => {
    expect(ENUM_FIELDS.length).toBeGreaterThan(0);
  });

  it('finds no enumerated field printed without the display layer', () => {
    const screens = walk(SOURCE_DIR).filter((file) => file.endsWith('.svelte'));
    expect(screens.length).toBeGreaterThan(0);

    const offenders: string[] = [];
    for (const file of screens) {
      const relative = path.relative(SOURCE_DIR, file);
      const expressions = interpolations(textOf(markupOf(fs.readFileSync(file, 'utf-8'))));
      for (const expression of expressions) {
        if (
          isBlockDirective(expression) ||
          expression.includes('displayState(') ||
          handsToHelper(expression)
        ) {
          continue;
        }
        const printed = withoutLiterals(expression);
        if (decidesRatherThanPrints(printed)) continue;
        const field = ENUM_FIELDS.find((candidate) => readsField(printed, candidate));
        if (field !== undefined) {
          offenders.push(`${relative}: ${expression}`);
        }
      }
    }

    expect(offenders).toEqual([]);
  });
});
