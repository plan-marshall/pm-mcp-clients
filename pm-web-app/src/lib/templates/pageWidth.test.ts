import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * A view that sits on a page template sets no page width of its own (UX-P-03): the template owns
 * the width and the split, so a second cap on the view would narrow the page twice.
 *
 * The list grows as the plans that move a view add it — PLAN-51, PLAN-53, PLAN-55 and PLAN-58 move
 * the decision, start, plan and epic views onto their templates, and each one appends its view and
 * the selector of its page root here.
 */

const VIEWS_DIR = path.resolve(__dirname, '../views');

/** The views that sit on a page template, with the selector of their page root. */
const VIEWS_ON_TEMPLATES: ReadonlyArray<{ file: string; root: string }> = [
  { file: 'SystemView.svelte', root: '.system-container' },
];

/** The templates a view can sit on. */
const TEMPLATES = ['TriagePage', 'ObjectPage', 'WizardPage', 'DashboardPage'];

/**
 * @param file a view file's path
 * @returns the view's source text
 */
function sourceOf(file: string): string {
  return fs.readFileSync(file, 'utf-8');
}

describe('the page width belongs to the template', () => {
  it('names at least one view that sits on a template', () => {
    expect(VIEWS_ON_TEMPLATES.length).toBeGreaterThan(0);
  });

  it('renders every listed view inside a page template', () => {
    const offenders = VIEWS_ON_TEMPLATES.filter((view) => {
      const source = sourceOf(path.join(VIEWS_DIR, view.file));
      return !TEMPLATES.some((template) => source.includes(`<${template}`));
    }).map((view) => view.file);

    expect(offenders).toEqual([]);
  });

  it('sets no max-width on the page root of a view that sits on a template', () => {
    const offenders = VIEWS_ON_TEMPLATES.flatMap((view) => {
      const file = path.join(VIEWS_DIR, view.file);
      const root = ruleOf(styleOf(file), view.root);
      return /max-width\s*:/.test(root) ? [`${view.file} (${view.root}): ${root.trim()}`] : [];
    });

    expect(offenders).toEqual([]);
  });
});
