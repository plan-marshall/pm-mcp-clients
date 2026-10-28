import { describe, it, expect } from 'vitest';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { cleanup, render } from '@testing-library/svelte';
import { UI_ICON_IDS, isUiIconId, uiIconUrl } from './uiIcons';
import Icon from '../components/Icon.svelte';

// The suite runs from `pm-web-app` under npm and the repository root under a direct call.
const projectRoot = existsSync(join(process.cwd(), 'src/lib/icons/ui'))
  ? process.cwd()
  : join(process.cwd(), 'pm-web-app');
const iconDirectory = join(projectRoot, 'src/lib/icons/ui');

/** @param id an icon id @returns its delivered file name */
function assetName(id: string): string {
  return `${id.replaceAll('.', '-')}.svg`;
}

/**
 * @param directory the directory to walk
 * @returns every file below it, recursively
 */
function walk(directory: string): string[] {
  const found: string[] = [];
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const full = join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...walk(full));
    } else {
      found.push(full);
    }
  }
  return found;
}

describe('UI icon registry', () => {
  it('delivers one unique asset for every id and none beside the registry', () => {
    expect(UI_ICON_IDS.length).toBeGreaterThanOrEqual(40);
    expect(new Set(UI_ICON_IDS).size).toBe(UI_ICON_IDS.length);

    const files = readdirSync(iconDirectory)
      .filter((name) => name.endsWith('.svg'))
      .sort();
    const expected = UI_ICON_IDS.map(assetName).sort();
    expect(files).toEqual(expected);

    const urls = UI_ICON_IDS.map((id) => uiIconUrl(id));
    expect(new Set(urls).size).toBe(UI_ICON_IDS.length);

    for (const id of UI_ICON_IDS) {
      expect(existsSync(join(iconDirectory, assetName(id))), `${id} is delivered`).toBe(true);
      expect(isUiIconId(id)).toBe(true);
    }
  });

  it('keeps the set on the workflow icon contract: 24 grid, 1.5 stroke, currentColor', () => {
    for (const id of UI_ICON_IDS) {
      const file = join(iconDirectory, assetName(id));
      const svg = readFileSync(file, 'utf8');

      expect(svg, file).toMatch(/<svg\s[^>]*viewBox="0 0 24 24"/);
      expect(svg, file).toContain('xmlns="http://www.w3.org/2000/svg"');
      expect(svg, file).toContain('fill="none"');
      expect(svg, file).toContain('stroke="currentColor"');
      expect(svg, file).toContain('stroke-width="1.5"');
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
      expect(colours.length, `${file} draws something`).toBeGreaterThan(0);
      for (const colour of colours) {
        expect(['none', 'currentColor'], `${file} colour ${colour}`).toContain(colour);
      }
    }
  });

  it('resolves each name through the registry and nothing else', () => {
    for (const id of UI_ICON_IDS) {
      expect(uiIconUrl(id), `${id} resolves to a delivered asset`).toBeTruthy();
    }
    expect(uiIconUrl('nav.unknown')).toBeNull();
    expect(uiIconUrl('')).toBeNull();
    expect(isUiIconId('nav.unknown')).toBe(false);
  });
});

describe('Icon component', () => {
  it('renders every delivered name as a decorative masked icon', () => {
    for (const id of UI_ICON_IDS) {
      const { container } = render(Icon, { props: { name: id, size: 16 } });
      const icon = container.querySelector('.ui-icon') as HTMLElement;

      expect(icon, `${id} renders`).not.toBeNull();
      expect(icon.getAttribute('data-icon')).toBe(id);
      expect(icon.getAttribute('aria-hidden')).toBe('true');
      expect(icon.getAttribute('role')).toBeNull();
      expect(icon.getAttribute('style')).toContain(`url("${uiIconUrl(id)}")`);
      expect(icon.getAttribute('style')).toContain('--ui-icon-size: 16px');
      cleanup();
    }
  });

  it('carries an accessible name when it stands without a label', () => {
    const { container } = render(Icon, { props: { name: 'nav.inbox', label: 'Inbox' } });
    const icon = container.querySelector('.ui-icon') as HTMLElement;

    expect(icon.getAttribute('role')).toBe('img');
    expect(icon.getAttribute('aria-label')).toBe('Inbox');
    expect(icon.hasAttribute('aria-hidden')).toBe(false);
  });

  it('renders nothing for a name the set does not deliver', () => {
    const { container } = render(Icon, { props: { name: 'nav.unknown' } });

    expect(container.querySelector('.ui-icon')).toBeNull();
  });
});

describe('the emoji sweep', () => {
  it('leaves no pictographic character anywhere in the web app source', () => {
    const pictographic = /\p{Extended_Pictographic}/u;
    const sourceDirectory = join(projectRoot, 'src');
    const offenders = walk(sourceDirectory)
      .filter((file) => pictographic.test(readFileSync(file, 'utf-8')))
      .map((file) => file.slice(projectRoot.length + 1));

    expect(offenders).toEqual([]);
  });
});
