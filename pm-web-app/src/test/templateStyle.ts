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
  const match = source.match(/<style>([\s\S]*?)<\/style>/);
  if (!match) {
    throw new Error(`${componentPath} carries no <style> block`);
  }
  return match[1];
}

/**
 * Reads one top-level rule of a stylesheet by its selector.
 *
 * @param style the stylesheet text
 * @param selector the rule's selector, exactly as the component writes it
 * @returns the declarations of the first matching rule, or an empty string
 */
export function ruleOf(style: string, selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = style.match(new RegExp(`(?:^|\\n)\\s*${escaped}\\s*\\{([^}]*)\\}`));
  return match ? match[1] : '';
}

/**
 * Reports every listed view whose page root carries a `max-width`.
 *
 * A listed view whose page root matches no rule is reported too: an empty rule would otherwise
 * pass the check whenever the selector is renamed or the rule is deleted, which is the vacuous
 * pass this guard exists to prevent.
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
    const root = ruleOf(styleOf(path.join(viewsDir, view.file)), view.root);
    if (!root.trim()) {
      return [`${view.file} (${view.root}): no rule found for the page root`];
    }
    return /max-width\s*:/.test(root) ? [`${view.file} (${view.root}): ${root.trim()}`] : [];
  });
}
