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
 * Reads the inline `style` of the element that carries a view's page-root class.
 *
 * @param componentPath the component's file
 * @param selector the page-root selector, e.g. `.system-container`
 * @returns the `style` attribute's value, or an empty string when the root carries none
 */
export function rootInlineStyle(componentPath: string, selector: string): string {
  const token = selector.startsWith('.') ? selector.slice(1) : selector;
  if (!/^[A-Za-z][\w-]*$/.test(token)) {
    return '';
  }
  const markup = fs
    .readFileSync(componentPath, 'utf-8')
    .replace(/<style>[\s\S]*?<\/style>/g, '');
  const tag = new RegExp(`<[^>]*class="[^"]*\\b${token}\\b[^"]*"[^>]*>`).exec(markup);
  const style = tag ? /style="([^"]*)"/.exec(tag[0]) : null;
  return style ? style[1] : '';
}

/**
 * Reports every listed view whose page root carries a page-width cap.
 *
 * Both spellings are read, in the stylesheet and in the root's inline `style`: `max-width` and
 * `max-inline-size`, except the fluid `max-inline-size: 100%`. A listed view whose page root
 * matches no rule is reported too: an empty rule would otherwise pass the check whenever the
 * selector is renamed or the rule is deleted, which is the vacuous pass this guard exists to
 * prevent.
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
    const declarations = `${root}\n${rootInlineStyle(file, view.root)}`;
    return /max-width\s*:|max-inline-size\s*:(?!\s*100%)/.test(declarations)
      ? [`${view.file} (${view.root}): ${declarations.trim()}`]
      : [];
  });
}
