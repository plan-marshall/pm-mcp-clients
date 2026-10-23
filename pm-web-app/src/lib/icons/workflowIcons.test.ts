import { describe, it, expect } from 'vitest';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import {
  WORKFLOW_ICON_IDS,
  isWorkflowIconId,
  phaseIconId,
  unitIconId,
  workflowIconUrl,
} from './workflowIcons';

// The suite runs from `pm-web-app` under npm and the repository root under a direct call.
const projectRoot = existsSync(join(process.cwd(), 'src/lib/icons/workflows'))
  ? process.cwd()
  : join(process.cwd(), 'pm-web-app');
const iconDirectory = join(projectRoot, 'src/lib/icons/workflows');
const cataloguePath = join(
  dirname(projectRoot),
  'doc/design/web-app/workflow-visual-language.adoc',
);

/**
 * @param text the visual specification
 * @param start the opening marker
 * @param end the closing marker
 * @returns the marked section
 */
function markedSection(text: string, start: string, end: string): string {
  const from = text.indexOf(start);
  const to = text.indexOf(end, from + start.length);
  if (from < 0 || to < 0) throw new Error(`missing section ${start}..${end}`);
  return text.slice(from, to);
}

/** @param id an icon id @returns its delivered file name */
function assetName(id: string): string {
  return `${id.replaceAll('.', '-')}.svg`;
}

const specification = readFileSync(cataloguePath, 'utf8');
const catalogue = markedSection(specification, '// icon-catalogue:start', '// icon-catalogue:end');
const catalogueIds = [...catalogue.matchAll(/^\|`([a-z0-9._-]+)`$/gm)].map((match) => match[1]);

const mapping = markedSection(specification, '// unit-icon-map:start', '// unit-icon-map:end');
const mappingRows = [
  ...mapping.matchAll(/^\|`([^`]+)`\n\|`([^`]+)`\n\|`(phaseIconId|unitIconId)`/gm),
].map((match) => ({ subject: match[1], icon: match[2], resolver: match[3] }));

describe('workflow icon registry', () => {
  it('delivers one unique asset for every id and none beside the registry', () => {
    expect(new Set(WORKFLOW_ICON_IDS).size).toBe(WORKFLOW_ICON_IDS.length);

    const files = readdirSync(iconDirectory).sort();
    const expected = WORKFLOW_ICON_IDS.map(assetName).sort();
    expect(files).toEqual(expected);

    const urls = WORKFLOW_ICON_IDS.map((id) => workflowIconUrl(id));
    expect(new Set(urls).size).toBe(WORKFLOW_ICON_IDS.length);

    for (const id of WORKFLOW_ICON_IDS) {
      expect(existsSync(join(iconDirectory, assetName(id)))).toBe(true);
    }
  });

  it('agrees with the catalogue table in the visual specification', () => {
    expect(catalogueIds.length).toBeGreaterThan(0);
    expect([...new Set(catalogueIds)].sort()).toEqual([...WORKFLOW_ICON_IDS].sort());
  });

  it('resolves every unit and phase mapping the catalogue records', () => {
    expect(mappingRows.length).toBeGreaterThan(0);

    for (const row of mappingRows) {
      const resolved =
        row.resolver === 'phaseIconId' ? phaseIconId(row.subject) : unitIconId(row.subject);
      expect(resolved, `${row.subject} resolves through ${row.resolver}`).toBe(row.icon);
      expect(isWorkflowIconId(row.icon)).toBe(true);
    }
  });

  it('resolves a phase-unit id and a bare token to the same phase icon', () => {
    expect(phaseIconId('phase-5-execute')).toBe('phase.execute');
    expect(phaseIconId('execute')).toBe('phase.execute');
    expect(phaseIconId('phase-6-finalize')).toBe('phase.finalize');
  });

  it('falls back for an unknown, empty, or missing id', () => {
    const fallback = workflowIconUrl('workflow.default');
    expect(workflowIconUrl(null)).toBe(fallback);
    expect(workflowIconUrl(undefined)).toBe(fallback);
    expect(workflowIconUrl('')).toBe(fallback);
    expect(workflowIconUrl('future.unknown')).toBe(fallback);
    expect(phaseIconId('phase-99-unknown')).toBe('workflow.default');
    expect(unitIconId('nothing-known')).toBe('workflow.default');
  });
});

describe('workflow icon assets', () => {
  it('lints every SVG against the visual contract', () => {
    for (const id of WORKFLOW_ICON_IDS) {
      const file = join(iconDirectory, assetName(id));
      const svg = readFileSync(file, 'utf8');

      expect(svg, file).toMatch(/<svg\s[^>]*viewBox="0 0 24 24"/);
      expect(svg, file).toContain('fill="none"');
      expect(svg, file).toContain('stroke="currentColor"');
      expect(svg, file).toContain('stroke-width="2"');
      expect(svg, file).toContain('stroke-linecap="round"');
      expect(svg, file).toContain('stroke-linejoin="round"');
      expect((svg.match(/<svg/g) ?? []).length, `${file} has one root`).toBe(1);

      for (const forbidden of [
        '<text',
        '<script',
        '<image',
        '<use',
        '<foreignObject',
        '<style',
        'href=',
        'style=',
        'data:',
      ]) {
        expect(svg.includes(forbidden), `${file} contains ${forbidden}`).toBe(false);
      }

      const colours = [...svg.matchAll(/(?:fill|stroke)="([^"]+)"/g)].map((match) => match[1]);
      for (const colour of colours) {
        expect(['none', 'currentColor'], `${file} colour ${colour}`).toContain(colour);
      }
    }
  });
});
