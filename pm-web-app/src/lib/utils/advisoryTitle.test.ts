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

  it('uses an explicit advisory first', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    advisory(button, 'Save the plan');
    expect(button.getAttribute('title')).toBe('Save the plan');
  });

  it('falls back to the aria-label, then the visible label', () => {
    const iconOnly = document.createElement('button');
    iconOnly.textContent = '🌙';
    iconOnly.setAttribute('aria-label', 'Toggle light/dark theme');
    advisory(iconOnly);
    expect(iconOnly.getAttribute('title')).toBe('Toggle light/dark theme');

    const labelled = document.createElement('button');
    labelled.textContent = '  Open   the report ';
    advisory(labelled);
    expect(labelled.getAttribute('title')).toBe('Open the report');
  });

  it('keeps an explicit title it did not write', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('title', 'Keep my words');
    advisory(button, 'Save the plan');
    expect(button.getAttribute('title')).toBe('Keep my words');
  });

  it('removes a title it wrote when the label disappears', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save');

    button.textContent = '';
    advisory(button);
    expect(button.hasAttribute('title')).toBe(false);
  });

  it('follows a changed advisory and aria-label', () => {
    const button = document.createElement('button');
    button.textContent = 'Save';
    button.setAttribute('data-advisory', 'Save the plan');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save the plan');

    button.setAttribute('data-advisory', 'Save the draft');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save the draft');

    button.removeAttribute('data-advisory');
    button.setAttribute('aria-label', 'Save now');
    advisory(button);
    expect(button.getAttribute('title')).toBe('Save now');
  });

  it('follows a changed visible label', async () => {
    const root = document.createElement('main');
    document.body.appendChild(root);
    stop = installAdvisoryTitles(root);

    const button = document.createElement('button');
    button.textContent = 'Delete';
    root.appendChild(button);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(button.getAttribute('title')).toBe('Delete');

    // A text node that changes in place is followed through characterData.
    (button.firstChild as CharacterData).data = 'Archive';
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(button.getAttribute('title')).toBe('Archive');
  });

  it('applies the rule to buttons rendered after it was installed', async () => {
    const root = document.createElement('main');
    document.body.appendChild(root);
    stop = installAdvisoryTitles(root);

    const button = document.createElement('button');
    button.textContent = 'Delete';
    root.appendChild(button);

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(button.getAttribute('title')).toBe('Delete');
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
