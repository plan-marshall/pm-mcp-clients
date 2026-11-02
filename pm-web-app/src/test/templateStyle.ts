import fs from 'node:fs';
import path from 'node:path';

/** A CSS comment, dropped before any rule is read. */
const CSS_COMMENT = /\/\*[\s\S]*?\*\//g;

/** An HTML comment, dropped before the markup is read. */
const HTML_COMMENT = /<!--[\s\S]*?-->/g;

/**
 * Reads the CSS a Svelte component carries in its own `<style>` block.
 *
 * A page template's width is a stylesheet rule, not a runtime measurement: jsdom applies no media
 * query, so a template test reads the rule the component ships instead of a computed width it
 * cannot produce.
 *
 * @param componentPath the component's file
 * @returns the text between its `<style>` tags
 */
export function styleOf(componentPath: string): string {
  const source = fs.readFileSync(componentPath, 'utf-8');
  const match = /<style>([\s\S]*?)<\/style>/.exec(source);
  if (!match) {
    throw new Error(`${componentPath} carries no <style> block`);
  }
  return match[1];
}

/**
 * Reads every rule of a stylesheet whose selector is exactly the given one.
 *
 * Comments are dropped first, and a match must start a rule — the character before it is the start
 * of the sheet or one of `{`, `}`, `;` — so a commented-out rule and a descendant selector such as
 * `.wrap .system-container` are both ignored. A selector inside a media query is a rule of its own
 * and is returned too, so a cap that applies only below a breakpoint is never missed.
 *
 * @param style the stylesheet text
 * @param selector the rule's selector, exactly as the component writes it
 * @returns the declarations of every matching rule, in source order
 */
export function rulesOf(style: string, selector: string): string[] {
  const cleaned = style.replace(CSS_COMMENT, '');
  const rules: string[] = [];
  let from = 0;
  while (from <= cleaned.length) {
    const at = cleaned.indexOf(selector, from);
    if (at < 0) {
      break;
    }
    const rest = at + selector.length;
    const before = cleaned.slice(0, at).trimEnd();
    const startsRule =
      before === '' || ['{', '}', ';'].some((boundary) => before.endsWith(boundary));
    if (startsRule && cleaned.slice(rest).trimStart().startsWith('{')) {
      const open = cleaned.indexOf('{', rest);
      const close = cleaned.indexOf('}', open + 1);
      if (open >= 0 && close >= 0) {
        rules.push(cleaned.slice(open + 1, close));
      }
      from = close >= 0 ? close + 1 : rest;
    } else {
      from = rest;
    }
  }
  return rules;
}

/**
 * Reads one top-level rule of a stylesheet by its selector.
 *
 * @param style the stylesheet text
 * @param selector the rule's selector, exactly as the component writes it
 * @returns the declarations of the first matching rule, or an empty string
 */
export function ruleOf(style: string, selector: string): string {
  return rulesOf(style, selector)[0] ?? '';
}

/**
 * @param markup a component's markup
 * @param open the index of a tag's opening `<`
 * @returns the index of the tag's closing `>`, or `-1` when the tag is never closed
 */
function tagEnd(markup: string, open: number): number {
  let quote = '';
  let braces = 0;
  for (let index = open + 1; index < markup.length; index++) {
    const char = markup[index];
    if (quote) {
      if (char === quote) {
        quote = '';
      }
    } else if (char === '"' || char === "'") {
      quote = char;
    } else if (char === '{') {
      braces++;
    } else if (char === '}') {
      braces = Math.max(0, braces - 1);
    } else if (char === '>' && braces === 0) {
      return index;
    }
  }
  return -1;
}

/**
 * @param markup a component's markup
 * @returns every tag, with the `>` inside a quoted value or a `{…}` block kept
 */
function tagsOf(markup: string): string[] {
  const tags: string[] = [];
  let from = 0;
  let open = markup.indexOf('<', from);
  while (open >= 0) {
    const close = tagEnd(markup, open);
    if (close < 0) {
      break;
    }
    tags.push(markup.slice(open, close + 1));
    from = close + 1;
    open = markup.indexOf('<', from);
  }
  return tags;
}

/**
 * Reads an attribute's value from a tag, distinguishing it from a longer name such as `data-class`.
 *
 * @param tag an element's tag text
 * @param name an attribute name
 * @returns the attribute's value, or an empty string when the tag carries none
 */
function attributeOf(tag: string, name: string): string {
  let from = 0;
  while (from < tag.length) {
    const at = tag.indexOf(name, from);
    if (at < 0) {
      return '';
    }
    const before = tag[at - 1];
    const afterName = tag.slice(at + name.length).trimStart();
    if ((at === 0 || /\s/.test(before)) && afterName.startsWith('=')) {
      const value = afterName.slice(1).trimStart();
      const quote = value[0];
      if (quote === '"' || quote === "'") {
        const end = value.indexOf(quote, 1);
        return end < 0 ? '' : value.slice(1, end);
      }
    }
    from = at + name.length;
  }
  return '';
}

/**
 * @param markup a component's markup
 * @param selector the page-root selector, e.g. `.system-container`
 * @returns the tag carrying that class, or an empty string when no element carries it
 */
function pageRootTag(markup: string, selector: string): string {
  const token = selector.startsWith('.') ? selector.slice(1) : selector;
  return (
    tagsOf(markup).find((tag) => attributeOf(tag, 'class').split(/\s+/).includes(token)) ?? ''
  );
}

/**
 * @param componentPath the component's file
 * @returns the component's markup, without its `<style>` block and without its HTML comments
 */
function markupOf(componentPath: string): string {
  return fs
    .readFileSync(componentPath, 'utf-8')
    .replace(/<style>[\s\S]*?<\/style>/g, '')
    .replace(HTML_COMMENT, '');
}

/**
 * Reads the inline width declarations of the element that carries a view's page-root class: its
 * `style` attribute and any `style:max-width` or `style:max-inline-size` directive.
 *
 * @param componentPath the component's file
 * @param selector the page-root selector, e.g. `.system-container`
 * @returns the declarations, or an empty string when the root carries none
 */
export function rootInlineStyle(componentPath: string, selector: string): string {
  const tag = pageRootTag(markupOf(componentPath), selector);
  const declarations = [attributeOf(tag, 'style')];
  if (tag.includes('style:max-width')) {
    declarations.push('max-width: directive');
  }
  if (tag.includes('style:max-inline-size')) {
    declarations.push('max-inline-size: directive');
  }
  return declarations.filter(Boolean).join(' ');
}

/**
 * Whether a view's markup carries its page-root class on an element.
 *
 * @param componentPath the component's file
 * @param selector the page-root selector, e.g. `.system-container`
 * @returns `true` when an element carries the class
 */
export function pageRootCarriedByMarkup(componentPath: string, selector: string): boolean {
  return pageRootTag(markupOf(componentPath), selector) !== '';
}

/**
 * Reports every listed view whose page root carries a page-width cap.
 *
 * Both spellings are read, in every rule for the root and in its inline declarations: `max-width`
 * and `max-inline-size`, except the fluid `max-inline-size: 100%`. CSS comments are dropped and a
 * custom property such as `--max-width` is not a cap. A listed view whose page root matches no
 * rule, or whose class no element carries, is reported too: an empty rule would otherwise pass
 * the check whenever the selector is renamed or the rule is deleted, which is the vacuous pass
 * this guard exists to prevent.
 *
 * @param viewsDir the directory the listed view files live in
 * @param views the views that sit on a template, with their page-root selector
 * @returns one entry per offending view, naming the file, the selector and the declarations
 */
export function pageWidthOffenders(
  viewsDir: string,
  views: ReadonlyArray<{ file: string; root: string }>,
): string[] {
  return views.flatMap((view) => {
    const file = path.join(viewsDir, view.file);
    const rules = rulesOf(styleOf(file), view.root);
    if (rules.length === 0) {
      return [`${view.file} (${view.root}): no rule found for the page root`];
    }
    if (!pageRootCarriedByMarkup(file, view.root)) {
      return [`${view.file} (${view.root}): no element carries the page-root class`];
    }
    const declarations = `${rules.join('\n')}\n${rootInlineStyle(file, view.root)}`;
    return /(?<![\w-])max-width\s*:|(?<![\w-])max-inline-size\s*:(?!\s*100%)/.test(declarations)
      ? [`${view.file} (${view.root}): ${declarations.trim()}`]
      : [];
  });
}
