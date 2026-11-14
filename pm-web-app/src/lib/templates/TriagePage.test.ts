import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import { tick } from 'svelte';
import fs from 'node:fs';
import path from 'node:path';
import TriagePageFixture from '../../test/fixtures/TriagePageFixture.svelte';
import { reportBox } from '../test/resizeObserver';
import { ruleOf, styleOf } from '../../test/templateStyle';
import { contentWidth, windowFor } from './columnWindow';
import type { Level } from './TriagePage.svelte';

/**
 * `TriagePage` as the column engine (`components.adoc#triage-page`): it measures its own container,
 * renders only the columns of the window, keeps the object column at least `480 px`, shows a pinned
 * row for a selection the list does not hold, and never widens the page.
 */

const SOURCE = path.resolve(__dirname, './TriagePage.svelte');

/** The five levels of the deepest chain, the object column last. */
const LEVELS: Level[] = [
  { key: 'lens', kind: 'report', title: 'Lens list', href: '/reports', state: 'two new' },
  { key: 'project', kind: 'project', title: 'Project column', href: '/projects/relay', state: 'running' },
  { key: 'epic', kind: 'epic', title: 'Epic column', href: '/epics/relay', state: 'running' },
  { key: 'plan', kind: 'plan', title: 'Plan column', href: '/plans/fix-relay', state: 'three left' },
  { key: 'task', kind: 'plan', title: 'Object column', href: '/plans/fix-relay/tasks/TASK-1', state: 'done' },
];

const BODIES = ['Lens body', 'Project body', 'Epic body', 'Plan body', 'Task body'];

/**
 * Renders the engine and reports the container's width, as a browser's observer would.
 *
 * @param width the container's content width in px
 * @returns the rendered engine, its frame and how many observers were told
 */
async function renderWindow(width: number, levels: Level[] = LEVELS) {
  const result = render(TriagePageFixture, {
    props: { levels, label: 'Levels', bodies: BODIES, objectBody: 'Object body' },
  });
  const frame = screen.getByTestId('triage-page');
  const told = reportBox(frame, width, 800);
  await tick();
  return { ...result, frame, told };
}

/**
 * @returns the rendered columns, left to right
 */
function columns(): HTMLElement[] {
  return Array.from(document.querySelectorAll<HTMLElement>('[data-column-index]'));
}

/**
 * @param element an element carrying an inline width
 * @returns its inline width in px
 */
function widthOf(element: HTMLElement): number {
  return Number.parseFloat(element.style.inlineSize);
}

describe('TriagePage as the column engine', () => {
  it('measures its own container and renders only the columns of the window', async () => {
    const { told } = await renderWindow(1168);

    expect(told).toBeGreaterThan(0);
    expect(columns()).toHaveLength(3);
    expect(document.querySelectorAll('[data-testid="triage-list"]')).toHaveLength(2);
    expect(document.querySelectorAll('[data-testid="triage-detail"]')).toHaveLength(1);
  });

  it('computes the window again when the container changes its width', async () => {
    const { frame } = await renderWindow(744);

    expect(columns()).toHaveLength(2);
    expect(screen.getByTestId('triage-page')).toHaveAttribute('data-columns', '2');

    reportBox(frame, 1168, 800);
    await tick();

    expect(columns()).toHaveLength(3);
    expect(screen.getByTestId('triage-page')).toHaveAttribute('data-columns', '3');
  });

  it('follows the container, not the viewport', async () => {
    window.innerWidth = 1920;

    const { frame } = await renderWindow(744);

    // 1920 px of viewport would fill four columns; the 744 px container fills two.
    expect(columns()).toHaveLength(2);

    reportBox(frame, 744, 800);
    await tick();
    expect(columns()).toHaveLength(2);
  });

  it('leaves a column out of the window out of the DOM', async () => {
    await renderWindow(744);

    expect(screen.queryByText('Lens body')).toBeNull();
    expect(screen.queryByText('Project body')).toBeNull();
    expect(screen.queryByText('Epic body')).toBeNull();
    expect(screen.getByText('Plan body')).toBeInTheDocument();
    expect(screen.getByText('Object body')).toBeInTheDocument();
  });

  it.each([744, 912, 1000, 1168, 1328, 1640])(
    'keeps the object column at least 480 px in a %i px container',
    async (width) => {
      await renderWindow(width);

      const object = screen.getByTestId('triage-detail');

      expect(widthOf(object)).toBeGreaterThanOrEqual(480);
    },
  );

  it('shows one column for a container below the marks threshold', async () => {
    await renderWindow(743);

    expect(columns()).toHaveLength(1);
    expect(screen.getByTestId('triage-detail')).toHaveTextContent('Object body');
    expect(screen.queryByTestId('triage-list')).toBeNull();
  });

  it('shows a pinned row with its reason for a selection the list does not hold', async () => {
    render(TriagePageFixture, {
      props: {
        levels: LEVELS,
        label: 'Levels',
        bodies: BODIES,
        objectBody: 'Object body',
        pinned: { level: LEVELS[4], reason: 'ended' },
      },
    });
    reportBox(screen.getByTestId('triage-page'), 1168, 800);
    await tick();

    const pinned = screen.getByTestId('triage-pinned');

    expect(pinned).toHaveTextContent('Object column');
    expect(pinned).toHaveTextContent('ended');
  });

  it('keeps the widest chain inside the content width', async () => {
    await renderWindow(1168);

    const layout = windowFor({ container: 1168, depth: LEVELS.length });
    const total = columns().reduce((sum, column) => sum + widthOf(column), 0);

    expect(total).toBe(layout.listWidth * (layout.columns - 1) + layout.objectWidth);
    expect(total + layout.stripWidth).toBe(contentWidth(1168));
  });

  it('never scrolls the page sideways', () => {
    const style = styleOf(SOURCE);

    expect(ruleOf(style, '.triage-page')).toContain('overflow-x: hidden');
    expect(ruleOf(style, '.triage-page__window')).toContain('flex-wrap: nowrap');
    expect(ruleOf(style, '.triage-page__column')).toContain('min-inline-size: 0');
  });

  it('scrolls each column on its own', () => {
    expect(ruleOf(styleOf(SOURCE), '.triage-page__column')).toContain('overflow-y: auto');
  });

  it('applies its widths with a style directive, never a static style attribute', () => {
    const source = fs.readFileSync(SOURCE, 'utf-8');
    const markup = source.replace(/<script[\s\S]*?<\/script>/g, '').replace(/<style[\s\S]*?<\/style>/g, '');

    expect(source).toContain('inline-size={`');
    expect(/\sstyle\s*=\s*"[^"{]*"/.test(markup)).toBe(false);
  });

  it('has given up the fixed 40 / 60 grid, the 1099 px query and the filter slot', () => {
    const source = fs.readFileSync(SOURCE, 'utf-8');

    expect(ruleOf(styleOf(SOURCE), '.triage-page')).not.toContain('grid-template-columns');
    expect(source).not.toContain('1099');
    expect(source).not.toContain('hasFilters');
    expect(source).not.toContain('slot="filters"');
    expect(source).not.toContain('2fr');
  });
});
