import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { tick } from 'svelte';
import { get } from 'svelte/store';
import fs from 'node:fs';
import path from 'node:path';
import TriagePageFixture from '../../test/fixtures/TriagePageFixture.svelte';
import { reportBox } from '../test/resizeObserver';
import { ruleOf, styleOf } from '../../test/templateStyle';
import { contentWidth, windowFor } from './columnWindow';
import { runtimeStore } from '../stores/runtime';
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
 * @param width the container's content width `W` in px, as the design's tables state it
 * @returns the rendered engine, its frame and how many observers were told
 */
async function renderWindow(width: number, levels: Level[] = LEVELS) {
  const result = render(TriagePageFixture, {
    props: { levels, label: 'Levels', bodies: BODIES, objectBody: 'Object body' },
  });
  const frame = screen.getByTestId('triage-page');
  // The design states the inset-less content width; the template measures the container.
  const told = reportBox(frame, width + 48, 800);
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
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-columns', '2');

    reportBox(frame, 1216, 800);
    await tick();

    expect(columns()).toHaveLength(3);
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-columns', '3');
  });

  it('takes the reported container over the measured fallback', async () => {
    const viewport = window.innerWidth;
    window.innerWidth = 1920;

    try {
      const { frame } = await renderWindow(496);

      // The fallback (1024 px of container) would fill two columns; the reported 544 px container
      // fills one.
      expect(columns()).toHaveLength(1);

      reportBox(frame, 792, 800);
      await tick();
      expect(columns()).toHaveLength(2);
    } finally {
      window.innerWidth = viewport;
    }
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
    reportBox(screen.getByTestId('triage-page'), 1216, 800);
    await tick();

    const pinned = screen.getByTestId('triage-pinned');

    expect(pinned).toHaveTextContent('Object column');
    expect(pinned).toHaveTextContent('ended');
  });

  it('hides the pinned row while the peek puts its list column out of the window', async () => {
    render(TriagePageFixture, {
      props: {
        levels: LEVELS,
        label: 'Levels',
        bodies: BODIES,
        objectBody: 'Object body',
        pinned: { level: LEVELS[4], reason: 'ended' },
      },
    });
    reportBox(screen.getByTestId('triage-page'), 1216, 800);
    await tick();
    expect(screen.getByTestId('triage-pinned')).toBeInTheDocument();

    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    expect(screen.queryByTestId('triage-pinned')).toBeNull();
  });

  it('keeps the widest chain inside the content width', async () => {
    await renderWindow(1168);

    const layout = windowFor({ container: 1216, depth: LEVELS.length });
    const total = columns().reduce((sum, column) => sum + widthOf(column), 0);

    expect(total).toBe(layout.listWidth * (layout.columns - 1) + layout.objectWidth);
    expect(total + layout.stripWidth).toBe(contentWidth(1216));
  });

  it('never scrolls the page sideways', () => {
    const style = styleOf(SOURCE);

    expect(ruleOf(style, '.triage-page')).toContain('overflow-x: hidden');
    expect(ruleOf(style, '.triage-page__window')).toContain('flex-wrap: nowrap');
    expect(ruleOf(style, '.triage-page__column')).toContain('min-inline-size: 0');
  });

  it('declares each column its own scroll (the shell height follows in PLAN-144)', () => {
    const column = ruleOf(styleOf(SOURCE), '.triage-page__column');

    // The declaration is pinned; the height it caps against is the app shell's, which keeps only a
    // minimum today (PLAN-144), so the cap resolves to none and the page scrolls as one.
    expect(column).toContain('overflow-y: auto');
    expect(column).toContain('max-block-size: 100%');
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

describe('the gap strip, the peek, the focus and the keys', () => {
  it('names the levels the window hides to the left', async () => {
    await renderWindow(1168);

    const strip = screen.getByRole('group', { name: 'Hidden levels' });

    expect(strip).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'report, Lens list' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'project, Project column' })).toBeInTheDocument();
    // The names stand beside the marks, so the names popover is not offered.
    expect(screen.queryByRole('button', { name: 'Names' })).toBeNull();
  });

  it('stands for the hidden levels with marks only where the names would leave one column', async () => {
    await renderWindow(744);

    expect(screen.getByRole('button', { name: 'Names' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'project, Project column' }),
    ).toHaveAttribute('title', 'Show this level; the selection stays');
  });

  it('peeks at a hidden level, then returns to the object', async () => {
    await renderWindow(1168);

    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '0');
    expect(screen.queryByText('Object body')).toBeNull();

    await fireEvent.click(screen.getByRole('button', { name: 'Back to Object column' }));

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    expect(screen.getByText('Object body')).toBeInTheDocument();
    // The return lands on the object, and the announcement names the same column.
    expect(document.activeElement).toHaveAttribute('data-testid', 'triage-detail');
    expect(get(runtimeStore).quietNavigationNotice).toBe('Object column, plan');
  });

  it('moves the focus to the heading of the column now in view and announces it', async () => {
    await renderWindow(1168);

    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    expect(document.activeElement).toHaveTextContent('Lens list');
    // The frame's polite live region carries the announcement (`navigation.adoc#keys`).
    expect(get(runtimeStore).quietNavigationNotice).toBe('Lens list, report');
  });

  it('ends a peek at the next selection', async () => {
    const rendered = await renderWindow(1168);
    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '0');

    await rendered.rerender({
      levels: [...LEVELS.slice(0, 4), { ...LEVELS[4], key: 'task-2', title: 'Another task' }],
      label: 'Levels',
      bodies: BODIES,
      objectBody: 'Object body',
    });
    await tick();

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    // The focus never falls to the page: it lands on the object the new chain opens.
    expect(document.activeElement).toHaveAttribute('data-testid', 'triage-detail');
    expect(get(runtimeStore).quietNavigationNotice).toBe('Another task, plan');
  });

  it('moves the focus to the neighbour column with the keys, shifting the window where it is hidden', async () => {
    await renderWindow(1168);
    const heading = screen.getByRole('heading', { name: 'Epic column' });
    heading.focus();

    await fireEvent.keyDown(heading, { key: 'ArrowLeft', ctrlKey: true });

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '1');
    expect(document.activeElement).toHaveTextContent('Project column');

    await fireEvent.keyDown(document.activeElement as HTMLElement, { key: ']' });

    expect(document.activeElement).toHaveTextContent('Epic column');
    expect(get(runtimeStore).quietNavigationNotice).toBe('Epic column, epic');
  });

  it('keeps the focus on the column it asks for at the right edge of a peek', async () => {
    await renderWindow(1168);
    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    // The peek starts at the lens and shows three columns: lens, project, epic.
    const epic = screen.getByRole('heading', { name: 'Epic column' });
    epic.focus();

    await fireEvent.keyDown(epic, { key: ']' });

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '1');
    expect(document.activeElement).toHaveTextContent('Plan column');
    expect(get(runtimeStore).quietNavigationNotice).toBe('Plan column, plan');
  });

  it('leaves the keys alone in a one-column window', async () => {
    await renderWindow(496);
    const window_ = document.querySelector('.triage-page__window');
    const before = get(runtimeStore).quietNavigationNotice;

    await fireEvent.keyDown(window_ as HTMLElement, { key: '[' });

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '4');
    expect(get(runtimeStore).quietNavigationNotice).toBe(before);
  });

  it('leaves the keys alone when a modifier is held', async () => {
    await renderWindow(1168);
    const before = get(runtimeStore).quietNavigationNotice;

    // Cmd+[ is the browser's Back; AltGr+8 types `[` on a German keyboard.
    await fireEvent.keyDown(screen.getByTestId('triage-page'), { key: '[', metaKey: true });
    await fireEvent.keyDown(screen.getByTestId('triage-page'), { key: '[', altKey: true, ctrlKey: true });

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    expect(get(runtimeStore).quietNavigationNotice).toBe(before);
  });

  it('leaves the keys to a focused field', async () => {    render(TriagePageFixture, {
      props: {
        levels: LEVELS,
        label: 'Levels',
        bodies: BODIES,
        objectBody: 'Object body',
        fieldLabel: 'Filter',
      },
    });
    reportBox(screen.getByTestId('triage-page'), 1216, 800);
    await tick();

    const field = screen.getAllByRole('textbox', { name: 'Filter' })[0];
    field.focus();
    const start = document.querySelector('.triage-page__window')?.getAttribute('data-start');

    await fireEvent.keyDown(field, { key: 'ArrowLeft', ctrlKey: true });
    await fireEvent.keyDown(field, { key: '[' });

    expect(document.activeElement).toBe(field);
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', start);
  });

  it('slides a shift for the base duration and not at all under reduced motion', () => {
    const style = styleOf(SOURCE);
    const source = fs.readFileSync(SOURCE, 'utf-8');

    expect(style).toContain('animation: triage-page-shift var(--duration-base)');
    // The animation runs on a shift: the window is keyed on the level it starts at.
    expect(source).toContain('{#key layout.start}');
    const reduced = style.slice(style.indexOf('@media (prefers-reduced-motion: reduce)'));
    expect(reduced).toContain('animation: none');
  });
});

describe('the side column of the chat', () => {
  /**
   * @returns the width the side column takes from its bound custom property
   */
  function sideWidth(): number {
    return Number.parseFloat(
      screen.getByTestId('side-column').style.getPropertyValue('--side-width'),
    );
  }

  /**
   * Renders the engine with the chat beside it.
   *
   * @param container the container's width in px
   * @param sideOpen whether the chat is open
   * @param onHideSide the act the side column reports
   * @param withUp whether a page leaves its Up act for the first visible column
   */
  async function renderSide(
    container: number,
    sideOpen = true,
    onHideSide = () => {},
    withUp = false,
  ) {
    render(TriagePageFixture, {
      props: {
        levels: LEVELS,
        label: 'Levels',
        bodies: BODIES,
        objectBody: 'Object body',
        sideBody: 'The chat',
        sideOpen,
        onHideSide,
        withUp,
      },
    });
    reportBox(screen.getByTestId('triage-page'), container, 800);
    await tick();
  }

  it('draws no side column while the chat is closed', async () => {
    await renderSide(1048, false);

    expect(screen.queryByTestId('side-column')).toBeNull();
    expect(screen.getByText('Object body')).toBeInTheDocument();
  });

  it('docks the chat at 480 px where the window keeps its column', async () => {
    await renderSide(1048);

    expect(sideWidth()).toBe(480);
    expect(screen.getByTestId('triage-detail').style.inlineSize).toBe('520px');
  });

  it('narrows the chat to 400 px where 480 would cost the window a column', async () => {
    await renderSide(960);

    expect(sideWidth()).toBe(400);
    expect(Number.parseFloat(screen.getByTestId('triage-detail').style.inlineSize)).toBe(512);
  });

  it('lets the chat take the space where the object and the chat do not fit', async () => {
    await renderSide(792);

    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');
    expect(columns()).toHaveLength(0);
  });

  it('hides the chat on its own act and on Escape', async () => {
    const onHideSide = vi.fn();
    await renderSide(1048, true, onHideSide);

    await fireEvent.click(screen.getByRole('button', { name: 'Hide the chat' }));
    expect(onHideSide).toHaveBeenCalledOnce();

    await fireEvent.keyDown(screen.getByTestId('side-column'), { key: 'Escape' });
    expect(onHideSide).toHaveBeenCalledTimes(2);
  });

  it('takes the whole screen on a phone, the bottom bar included', async () => {
    await renderSide(390);

    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');
  });

  it('leaves the head of the first visible column for the page that fills it', async () => {
    await renderSide(1216, false, () => {}, true);

    const first = columns()[0];

    expect(first.querySelector('.triage-page__up')).not.toBeNull();
    expect(first).toHaveTextContent('Up');
  });
});
