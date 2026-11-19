import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

/**
 * Vocabulary completeness guard (PLAN-151, components.adoc § #vocabulary).
 *
 * Asserts that every component of the design vocabulary (not on the pending list)
 * has its component source, its unit test, and a fixture on /dev/components.
 */

const DOCS_PATH = path.resolve(__dirname, '../../../doc/design/web-app/components.adoc');
const COMPONENTS_DIR = path.resolve(__dirname, 'components');
const TEMPLATES_DIR = path.resolve(__dirname, 'templates');
const DEV_PAGE_PATH = path.resolve(__dirname, '../routes/dev/components/+page.svelte');

/**
 * Components built by other packages, pending their landing.
 */
export const PENDING_LIST = new Set([
  'MascotFigure',
  'Chat',
  'ChatTurn',
  'ProposalCard',
  'ChatStatusCard',
  'ChatMarkdown',
  'VoiceInput',
  'TriagePage',
]);

/**
 * Extracts component names from the AsciiDoc vocabulary table.
 *
 * @param {string} adocContent
 * @returns {Array<{ group: string, name: string }>}
 */
export function extractVocabularyComponents(adocContent: string): Array<{ group: string; name: string }> {
  const vocabIndex = adocContent.indexOf('[#vocabulary]');
  if (vocabIndex === -1) {
    throw new Error('Could not find [#vocabulary] in components.adoc');
  }

  const section = adocContent.slice(vocabIndex);
  const tableStart = section.indexOf('|===');
  const tableEnd = section.indexOf('|===', tableStart + 4);
  const tableText = section.slice(tableStart, tableEnd);

  const results: Array<{ group: string; name: string }> = [];
  const rows = tableText.split(/\n\|(?=[A-Z])/);

  for (const row of rows) {
    const lines = row.trim().split('\n');
    if (lines.length < 2) continue;
    const group = lines[0].replace(/^\|/, '').trim();
    if (group === 'Group' || !group) continue;

    const componentsCell = lines.slice(1).join(' ');
    const matches = componentsCell.matchAll(/`([A-Z][A-Za-z0-9]+)`/g);
    for (const match of matches) {
      const name = match[1];
      results.push({ group, name });
    }
  }

  return results;
}

describe('vocabulary completeness', () => {
  it('every component in the vocabulary table has source, test, and dev fixture', () => {
    expect(fs.existsSync(DOCS_PATH)).toBe(true);
    expect(fs.existsSync(DEV_PAGE_PATH)).toBe(true);

    const docContent = fs.readFileSync(DOCS_PATH, 'utf-8');
    const devPageSource = fs.readFileSync(DEV_PAGE_PATH, 'utf-8');
    const templateOnly = devPageSource
      .replace(/<script[\s\S]*?<\/script>/g, '')
      .replace(/<!--[\s\S]*?-->/g, '');

    const vocabulary = extractVocabularyComponents(docContent);
    expect(vocabulary.length).toBeGreaterThan(30);

    const activeComponents = vocabulary.filter((c) => !PENDING_LIST.has(c.name));
    expect(activeComponents.length).toBeGreaterThan(25);

    for (const { group, name } of activeComponents) {
      const componentInComponents = path.join(COMPONENTS_DIR, `${name}.svelte`);
      const componentInTemplates = path.join(TEMPLATES_DIR, `${name}.svelte`);
      const componentExists =
        fs.existsSync(componentInComponents) ||
        (group === 'Templates' && fs.existsSync(componentInTemplates));

      expect(
        componentExists,
        `Expected component source for ${name} (group: ${group}) to exist in components/ or templates/`,
      ).toBe(true);

      const testInComponents = path.join(COMPONENTS_DIR, `${name}.test.ts`);
      const testInTemplates = path.join(TEMPLATES_DIR, `${name}.test.ts`);
      const testExists =
        fs.existsSync(testInComponents) ||
        (group === 'Templates' && fs.existsSync(testInTemplates));

      expect(
        testExists,
        `Expected test for ${name} (${name}.test.ts) to exist beside component`,
      ).toBe(true);

      // eslint-disable-next-line security/detect-non-literal-regexp
      const componentTagRegex = new RegExp(`<${name}\\b`);
      expect(
        componentTagRegex.test(templateOnly),
        `Expected component ${name} to have a rendered fixture (<${name} ...) in template of routes/dev/components/+page.svelte`,
      ).toBe(true);
    }
  });
});
