import fs from 'node:fs';
import path from 'node:path';

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
 * Reads one top-level rule of a stylesheet by its selector.
 *
 * The selector is matched literally and must be followed by `{`, so `.triage-page` never picks up
 * the `.triage-page__filters` rule that only shares a prefix; no regular expression is built, so
 * the selector needs no escaping.
 *
 * @param style the stylesheet text
 * @param selector the rule's selector, exactly as the component writes it
 * @returns the declarations of the first matching rule, or an empty string
 */
export function ruleOf(style: string, selector: string): string {
  let from = 0;
  while (from <= style.length) {
    const at = style.indexOf(selector, from);
    if (at < 0) {
      return '';
    }
    const rest = at + selector.length;
    if (style.slice(rest).trimStart().startsWith('{')) {
      const open = style.indexOf('{', rest);
      const close = style.indexOf('}', open + 1);
      return open < 0 || close < 0 ? '' : style.slice(open + 1, close);
    }
    from = rest;
  }
  return '';
}

/**
 * @param markup a component's markup
 * @returns every opening or closing tag, with the `>` inside a quoted value or a `{…}` block kept
 */
function tagsOf(markup: string): string[] {
  const tags: string[] = [];
  let from = 0;
  while (from < markup.length) {
    const open = markup.indexOf('<', from);
    if (open < 0) {
      break;
    }
    let index = open + 1;
    let quote = '';
    let braces = 0;
    for (; index < markup.length; index++) {
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
        break;
      }
    }
    tags.push(markup.slice(open, index + 1));
    from = index + 1;
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
 * @returns the component's markup, without its `<style>` block
 */
function markupOf(componentPath: string): string {
  return fs
    .readFileSync(componentPath, 'utf-8')
    .replace(/<style>[\s\S]*?<\/style>/g, '');
}

/**
 * Reads the inline `style` of the element that carries a view's page-root class.
 *
 * @param componentPath the component's file
 * @param selector the page-root selector, e.g. `.system-container`
 * @returns the `style` attribute's value, or an empty string when the root carries none
 */
export function rootInlineStyle(componentPath: string, selector: string): string {
  return attributeOf(pageRootTag(markupOf(componentPath), selector), 'style');
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
 * Both spellings are read, in the stylesheet and in the root's inline `style`: `max-width` and
 * `max-inline-size`, except the fluid `max-inline-size: 100%`. CSS comments are dropped and a
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
    const root = ruleOf(styleOf(file), view.root);
    if (!root.trim()) {
      return [`${view.file} (${view.root}): no rule found for the page root`];
    }
    if (!pageRootCarriedByMarkup(file, view.root)) {
      return [`${view.file} (${view.root}): no element carries the page-root class`];
    }
    const declarations = `${root}\n${rootInlineStyle(file, view.root)}`;
    const effective = declarations.replace(/\/\*[\s\S]*?\*\//g, '');
    return /(?<![\w-])max-width\s*:|(?<![\w-])max-inline-size\s*:(?!\s*100%)/.test(effective)
      ? [`${view.file} (${view.root}): ${declarations.trim()}`]
      : [];
  });
}
