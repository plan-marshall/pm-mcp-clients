import { describe, it, expect, afterEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { advisory, installAdvisoryTitles } from './advisoryTitle';

const SOURCE_DIR = path.resolve(__dirname, '../..');

/**
 * Every `.svelte` source of the package, with its path relative to the source root.
 *
 * @returns the source files, tests excluded
 */
function svelteSources(): Array<{ relative: string; full: string }> {
  const found: Array<{ relative: string; full: string }> = [];
  const walk = (directory: string): void => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const full = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        walk(full);
      } else if (entry.name.endsWith('.svelte')) {
        found.push({ relative: path.relative(SOURCE_DIR, full), full });
      }
    }
  };
  walk(SOURCE_DIR);
  return found;
}

/**
 * @param inner the markup between a button's tags
 * @returns whether a person would read text on the button
 */
function hasReadableText(inner: string): boolean {
  // An expression that names a value (`{actLabel(...)}`, `{project.title}`) carries text a person
  // reads; only a literal glyph with no expression is an icon-only button.
  const withoutTags = inner.replace(/<[^>]*>/g, ' ');
  return /[A-Za-z]{2,}/.test(withoutTags);
}

describe('the advisory title layer', () => {
  let stop: (() => void) | null = null;

  afterEach(() => {
    stop?.();
    stop = null;
    document.body.innerHTML = '';
  });

  it('gives a button that only carries a visible label no title', () => {
    const button = document.createElement('button');
    button.textContent = 'Open the report';
    advisory(button);
    expect(button.hasAttribute('title')).toBe(false);
  });

  it('gives a button that only carries an aria-label no title', () => {
    const button = document.createElement('button');
    // An icon-only control: a presentational child carries the drawing, no text does.
    button.appendChild(document.createElement('span'));
    button.setAttribute('aria-label', 'Toggle light/dark theme');
    advisory(button);
    expect(button.hasAttribute('title')).toBe(false);
  });

  it('uses the explicit advisory, which adds to the label', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    advisory(button, 'Save the plan and keep its tasks');
    expect(button.getAttribute('title')).toBe('Save the plan and keep its tasks');
  });

  it('uses a data-advisory attribute when the view passes none explicitly', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('data-advisory', 'Save the plan and keep its tasks');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save the plan and keep its tasks');
  });

  it('keeps an explicit title it did not write, even when the label changes', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('title', 'Keep my words');
    advisory(button, 'Save the plan');
    expect(button.getAttribute('title')).toBe('Keep my words');

    button.textContent = 'Save now';
    advisory(button, 'Save the plan');
    expect(button.getAttribute('title')).toBe('Keep my words');
  });

  it('follows a changed advisory', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('data-advisory', 'Save the plan');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save the plan');

    button.setAttribute('data-advisory', 'Save the draft');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save the draft');
  });

  it('removes the title it wrote when the advisory disappears', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('data-advisory', 'Save the plan');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save the plan');

    button.removeAttribute('data-advisory');
    advisory(button);
    expect(button.hasAttribute('title')).toBe(false);
  });

  it('follows a changed advisory of a button rendered before it was installed', async () => {
    const root = document.createElement('main');
    document.body.appendChild(root);
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('data-advisory', 'Save the plan');
    root.appendChild(button);
    stop = installAdvisoryTitles(root);
    expect(button.getAttribute('title')).toBe('Save the plan');

    button.setAttribute('data-advisory', 'Save the draft');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(button.getAttribute('title')).toBe('Save the draft');

    button.removeAttribute('data-advisory');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(button.hasAttribute('title')).toBe(false);
  });

  it('applies the rule to buttons rendered after it was installed', async () => {
    const root = document.createElement('main');
    document.body.appendChild(root);
    stop = installAdvisoryTitles(root);

    const labelled = document.createElement('button');
    labelled.textContent = 'Delete';
    root.appendChild(labelled);

    const advised = document.createElement('button');
    advised.textContent = 'Delete';
    advised.setAttribute('data-advisory', 'Delete the plan and its worktree');
    root.appendChild(advised);

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(labelled.hasAttribute('title')).toBe(false);
    expect(advised.getAttribute('title')).toBe('Delete the plan and its worktree');
  });
});

describe('the source guard for glyph-only actions', () => {
  it('lets no glyph-only button stand without a title, aria-label, or advisory', () => {
    const offenders: string[] = [];
    for (const source of svelteSources()) {
      const text = fs.readFileSync(source.full, 'utf-8');
      for (const match of text.matchAll(/<button\b([^>]*)>([\s\S]*?)<\/button>/g)) {
        const attributes = match[1];
        const inner = match[2];
        const explained =
          /\baria-label\s*=/.test(attributes) ||
          /\btitle\s*=/.test(attributes) ||
          /\bdata-advisory\s*=/.test(attributes) ||
          // A shared wrapper forwards the caller's aria-label, title, or data-advisory.
          /\{\.\.\.\$\$restProps\}/.test(attributes);
        if (explained || hasReadableText(inner)) {
          continue;
        }
        offenders.push(`${source.relative}: ${match[0].replace(/\s+/g, ' ').slice(0, 120)}`);
      }
    }
    expect(offenders).toEqual([]);
  });
});
