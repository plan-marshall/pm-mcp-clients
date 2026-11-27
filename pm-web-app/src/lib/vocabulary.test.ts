import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { parse } from 'svelte/compiler';

/**
 * Vocabulary completeness guard (PLAN-151, PLAN-170, ADR 002).
 *
 * Asserts that every component of the literal vocabulary (not on the pending list)
 * has its component source, its unit test, and a fixture on /dev/components.
 */

/**
 * The component vocabulary, carried here as the expectation (ADR 002): 8 groups, 60 names, as the
 * design's `components.adoc#vocabulary` describes them. The test reads no document; a component the
 * design adds is added here with the view that needs it.
 */
export const VOCABULARY = Object.freeze({
  Tags: [
    'StatusLabel',
    'ReasonBadge',
    'CountBadge',
    'Chip',
    'PrChip',
    'KindMark',
    'PhaseLabel',
    'Marker',
  ],
  Interaction: [
    'Button',
    'IconButton',
    'ActionBar',
    'RowMenu',
    'Link',
    'ExternalLink',
    'RowLink',
    'SegmentedControl',
    'Toggle',
    'Tabs',
    'Breadcrumb',
    'Dialog',
    'ConfirmDialog',
  ],
  Fields: [
    'Field',
    'TextField',
    'TextArea',
    'NumberField',
    'SecretField',
    'CodeField',
    'SelectField',
    'RadioGroup',
    'CheckField',
    'CheckGroup',
    'FilterInput',
    'Compose',
    'FieldError',
    'FormStatus',
  ],
  Containers: ['Card', 'KeyValueList', 'Toast', 'AttentionStrip'],
  Data: ['DataTable', 'EmptyState', 'ErrorState', 'EvidenceView', 'Skeleton'],
  Supporting: ['Icon', 'Spinner', 'StepBar', 'BrandMark', 'MascotFigure', 'WaitingMark'],
  Chat: ['Chat', 'ChatTurn', 'ProposalCard', 'ChatStatusCard', 'ChatMarkdown', 'VoiceInput'],
  Templates: ['TriagePage', 'ObjectPage', 'WizardPage', 'DashboardPage'],
} as const);

const COMPONENTS_DIR = path.resolve(__dirname, 'components');
const TEMPLATES_DIR = path.resolve(__dirname, 'templates');
const DEV_PAGE_PATH = path.resolve(__dirname, '../routes/dev/components/+page.svelte');
const DEV_DIR = path.resolve(__dirname, 'dev');

/**
 * Components built by other packages, pending their landing.
 * MascotFigure and TriagePage have landed and are active components.
 */
export const PENDING_LIST = new Set([
  'Chat',
  'ChatTurn',
  'ProposalCard',
  'ChatStatusCard',
  'ChatMarkdown',
  'VoiceInput',
]);

/**
 * Validates that PENDING_LIST names only components that do NOT exist yet.
 * Returns an array of error messages for any pending component found on disk.
 */
export function validatePendingList(
  pendingList: Set<string>,
  componentsDir: string = COMPONENTS_DIR,
  templatesDir: string = TEMPLATES_DIR,
): string[] {
  const errors: string[] = [];
  for (const name of pendingList) {
    const inComponents = path.join(componentsDir, `${name}.svelte`);
    const inTemplates = path.join(templatesDir, `${name}.svelte`);
    if (fs.existsSync(inComponents) || fs.existsSync(inTemplates)) {
      errors.push(
        `Pending component ${name} already exists on disk — remove it from PENDING_LIST`,
      );
    }
  }
  return errors;
}

/**
 * Checks whether a component is rendered as an actual tag in a markup position.
 * Uses the Svelte compiler AST to verify the component is used as a tag,
 * avoiding false positives from attribute strings or comments.
 *
 * A fixture that does not parse is an error, never a guess: a regex over broken markup cannot tell a
 * tag from a comment, and a broken fixture must fail the test with the parser's own message.
 */
export function hasComponentFixture(name: string, templateSource: string): boolean {
  let ast;
  try {
    ast = parse(templateSource, { modern: true });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    throw new Error(`The fixture source does not parse: ${message}`);
  }
  let found = false;

  function walk(node: unknown): void {
    if (found || !node || typeof node !== 'object') return;
    const n = node as Record<string, unknown>;
    if ((n.type === 'Component' || n.type === 'RegularElement') && n.name === name) {
      found = true;
      return;
    }
    for (const key of Object.keys(n)) {
      if (key === 'parent' || key === 'css') continue;
      const child = n[key];
      if (Array.isArray(child)) {
        for (const item of child) walk(item);
      } else if (child && typeof child === 'object') {
        walk(child);
      }
    }
  }

  if (ast.fragment) {
    walk(ast.fragment);
  }
  return found;
}

describe('vocabulary completeness', () => {
  it('fails completeness when a pending component already exists on disk', () => {
    const fixturePending = new Set(['Button']);
    const errors = validatePendingList(fixturePending);
    expect(errors.length).toBeGreaterThan(0);
    expect(errors.some((e) => e.includes('Button'))).toBe(true);
  });

  it('a fixture that mentions <Name only inside an attribute value does not satisfy the fixture check', () => {
    const fakeMarkup = '<div data-info="<Button"></div>';
    const hasTag = hasComponentFixture('Button', fakeMarkup);
    expect(hasTag).toBe(false);
  });

  it('a fixture that does not parse fails the fixture check with the parser message', () => {
    const brokenMarkup = '<!-- <Button < --> <div>';
    let parserMessage = '';
    try {
      parse(brokenMarkup, { modern: true });
    } catch (err) {
      parserMessage = err instanceof Error ? err.message : String(err);
    }
    expect(parserMessage).not.toBe('');

    expect(() => hasComponentFixture('Button', brokenMarkup)).toThrow(parserMessage);
  });

  it('a comment that mentions <Name does not satisfy the fixture check', () => {
    const commentOnly = '<!-- <Button /> --><div>Nothing renders it</div>';
    expect(hasComponentFixture('Button', commentOnly)).toBe(false);
  });

  it('PENDING_LIST names only components that do not exist yet on disk', () => {
    const errors = validatePendingList(PENDING_LIST);
    expect(errors).toEqual([]);
  });

  it('every component of the literal vocabulary has source, test, and dev fixture', () => {
    expect(fs.existsSync(DEV_PAGE_PATH)).toBe(true);

    const devSources: string[] = [fs.readFileSync(DEV_PAGE_PATH, 'utf-8')];
    if (fs.existsSync(DEV_DIR)) {
      for (const file of fs.readdirSync(DEV_DIR)) {
        if (file.endsWith('.svelte')) {
          devSources.push(fs.readFileSync(path.join(DEV_DIR, file), 'utf-8'));
        }
      }
    }

    const vocabulary = Object.entries(VOCABULARY).flatMap(([group, names]) =>
      names.map((name) => ({ group, name })),
    );
    // The vocabulary carries exactly 60 components across 8 groups.
    expect(Object.keys(VOCABULARY)).toHaveLength(8);
    expect(vocabulary).toHaveLength(60);

    // 6 components pending landing (Chat, ChatTurn, ProposalCard, ChatStatusCard, ChatMarkdown, VoiceInput).
    const activeComponents = vocabulary.filter((c) => !PENDING_LIST.has(c.name));
    expect(activeComponents).toHaveLength(60 - PENDING_LIST.size);

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

      const hasFixture = devSources.some((source) => hasComponentFixture(name, source));
      expect(
        hasFixture,
        `Expected component ${name} to have a rendered fixture (<${name} ...) in routes/dev/components or dev fixtures`,
      ).toBe(true);
    }
  });
});
