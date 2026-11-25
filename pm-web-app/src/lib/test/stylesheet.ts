import fs from 'node:fs';

/**
 * Readers of a component's stylesheet, for the tests of a rule jsdom does not apply: a container
 * query, a media query, or the display a rule sets at the top level — and the one reader of the
 * theme blocks of `tokens.css`, so every test that measures the palette reads the same rules.
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
 * @param opener the at-rule or selector whose block is wanted, as written
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
 * @returns the stylesheet without its comments, so a commented-out declaration never counts
 */
export function withoutComments(css: string): string {
  return css.replace(/\/\*[\s\S]*?\*\//g, '');
}

/**
 * @param block a declaration block
 * @returns every custom property the block declares, keyed by its name
 */
export function declarations(block: string): Record<string, string> {
  const found: Record<string, string> = {};
  const pattern = /(--[a-zA-Z0-9_-]+)\s*:\s*([^;]+);/g;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(block)) !== null) {
    found[match[1]] = match[2].trim();
  }
  return found;
}

/** The theme blocks of `tokens.css`, each read by its selector and keyed by declaration. */
export interface ThemeBlocks {
  /** The top-level `:root` block: the shared declarations and the light palette as the default. */
  root: Record<string, string>;
  /** The `[data-theme='light']` block. */
  light: Record<string, string>;
  /** The `[data-theme='dark']` block. */
  dark: Record<string, string>;
  /** The `:root` block inside `@media (prefers-color-scheme: dark)`. */
  mediaDark: Record<string, string>;
}

/**
 * @param block a declaration block
 * @param name the custom property's name, with its leading `--`
 * @returns its value, asserted present so a missing token fails by name rather than downstream
 */
export function tokenOf(block: Record<string, string>, name: string): string {
  const value = block[name];
  if (value === undefined) throw new Error(`no "${name}" declaration in the block`);
  return value;
}

/**
 * Reads the four theme blocks of `tokens.css` by selector — never through the header comment, which
 * only describes them, so a restructured file still yields the blocks its rules declare.
 *
 * @param source the stylesheet
 * @returns the parsed themes, with the top-level `:root` declaration as the light default
 */
export function parseThemes(source: string): ThemeBlocks {
  const css = withoutComments(source);
  // `:root` is declared more than once: the palette default at the top level and one inside each
  // media block. The palette default is the TOP-LEVEL one, so the media blocks are dropped first
  // rather than trusting the declaration order a restructure of the file could move.
  const topLevel = withoutBlocks(
    css,
    '@media (prefers-color-scheme: dark)',
    '@media (prefers-reduced-motion: reduce)',
    '@media (max-width: 767px)',
  );
  return {
    root: declarations(blockOf(topLevel, ':root')),
    light: declarations(blockOf(css, "[data-theme='light']")),
    dark: declarations(blockOf(css, "[data-theme='dark']")),
    mediaDark: declarations(blockOf(blockOf(css, '@media (prefers-color-scheme: dark)'), ':root')),
  };
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
