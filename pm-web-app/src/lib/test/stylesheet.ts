import fs from 'node:fs';

/**
 * Readers of a component's stylesheet, for the tests of a rule jsdom does not apply: a container
 * query, a media query, or the display a rule sets at the top level.
 */

/**
 * @param componentPath the absolute path of a `.svelte` file
 * @returns the content of its `<style>` element, comments dropped, so a rule is found by its
 *   selector alone
 */
export function stylesheetOf(componentPath: string): string {
  const source = fs.readFileSync(componentPath, 'utf-8');
  return (/<style>([\s\S]*?)<\/style>/.exec(source)?.[1] ?? '').replace(/\/\*[\s\S]*?\*\//g, '');
}

/**
 * @param css a stylesheet
 * @param opener the at-rule whose block is wanted, as written
 * @returns the block's content, braces matched
 */
export function blockOf(css: string, opener: string): string {
  const at = css.indexOf(opener);
  if (at < 0) throw new Error(`no "${opener}" block`);
  const open = css.indexOf('{', at + opener.length);
  let depth = 0;
  for (let i = open; i < css.length; i += 1) {
    if (css[i] === '{') depth += 1;
    if (css[i] === '}') {
      depth -= 1;
      if (depth === 0) return css.slice(open + 1, i);
    }
  }
  throw new Error(`unbalanced "${opener}" block`);
}

/**
 * @param css a stylesheet
 * @param selector the selector of the rule, as written
 * @returns the declarations of the first rule with exactly that selector
 */
export function ruleOf(css: string, selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
  // The selector must open the rule, so it never matches the tail of a longer selector.
  const match = new RegExp(String.raw`(^|[{}])\s*${escaped}\s*\{([^}]*)\}`).exec(css);
  if (!match) throw new Error(`no rule for "${selector}"`);
  return match[2];
}

/**
 * @param css a stylesheet
 * @param openers the at-rules to drop
 * @returns the stylesheet without those at-rules' blocks, so a rule is read at the top level
 */
export function withoutBlocks(css: string, ...openers: string[]): string {
  return openers.reduce((rest, opener) => {
    const at = rest.indexOf(opener);
    if (at < 0) return rest;
    const inner = blockOf(rest, opener);
    const end = rest.indexOf(inner, at) + inner.length + 1;
    return rest.slice(0, at) + rest.slice(end);
  }, css);
}
