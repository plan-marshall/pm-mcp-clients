import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { pageWidthOffenders } from '../../test/templateStyle';

/**
 * A view that sits on a page template sets no page width of its own (UX-P-03): the template owns
 * the width and the split, so a second cap on the view would narrow the page twice.
 *
 * The list grows as the plans that move a view add it — PLAN-51, PLAN-53, PLAN-55 and PLAN-58 move
 * the decision, start, plan and epic views onto their templates, and each one appends its view and
 * the selector of its page root here. The control fixture proves the guard still reports a view
 * that carries a width, and a renamed selector is reported instead of passing vacuously.
 */

const VIEWS_DIR = path.resolve(__dirname, '../views');
const CONTROL_DIR = path.resolve(__dirname, '../../test/fixtures/templateViews');

/** The views that sit on a page template, with the selector of their page root. */
const VIEWS_ON_TEMPLATES: ReadonlyArray<{ file: string; root: string }> = [
  { file: 'SystemView.svelte', root: '.system-container' },
];

/** The templates a view can sit on: every `*Page.svelte` component in this directory. */
const TEMPLATES = fs
  .readdirSync(__dirname)
  .filter((name) => name.endsWith('Page.svelte'))
  .map((name) => name.slice(0, -'.svelte'.length));

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

  it('lists every view that imports a page template', () => {
    const listed = new Set(VIEWS_ON_TEMPLATES.map((view) => view.file));
    const unlisted = fs
      .readdirSync(VIEWS_DIR)
      .filter((file) => file.endsWith('.svelte'))
      .filter((file) =>
        TEMPLATES.some((template) =>
          sourceOf(path.join(VIEWS_DIR, file)).includes(`templates/${template}.svelte`),
        ),
      )
      .filter((file) => !listed.has(file));

    expect(unlisted).toEqual([]);
  });

  it('sets no max-width on the page root of a view that sits on a template', () => {
    expect(pageWidthOffenders(VIEWS_DIR, VIEWS_ON_TEMPLATES)).toEqual([]);
  });

  it('reports a view that carries its own page width (control)', () => {
    const offenders = pageWidthOffenders(CONTROL_DIR, [
      { file: 'WidthView.svelte', root: '.control-page' },
    ]);

    expect(offenders).toHaveLength(1);
    expect(offenders[0]).toContain('max-width: 1200px');
  });

  it('reports a view that caps its root with max-inline-size (control)', () => {
    const offenders = pageWidthOffenders(CONTROL_DIR, [
      { file: 'InlineWidthView.svelte', root: '.control-inline-page' },
    ]);

    expect(offenders).toHaveLength(1);
    expect(offenders[0]).toContain('max-inline-size: 1200px');
  });

  it('reports a view that caps its root with an inline style (control)', () => {
    const offenders = pageWidthOffenders(CONTROL_DIR, [
      { file: 'InlineStyleView.svelte', root: '.control-inline-style' },
    ]);

    expect(offenders).toHaveLength(1);
    expect(offenders[0]).toContain('max-width: 1200px');
  });

  it('reports a page root whose selector matches no rule instead of passing vacuously', () => {
    const offenders = pageWidthOffenders(VIEWS_DIR, [
      { file: 'SystemView.svelte', root: '.a-selector-no-view-carries' },
    ]);

    expect(offenders).toEqual([
      'SystemView.svelte (.a-selector-no-view-carries): no rule found for the page root',
    ]);
  });
});
