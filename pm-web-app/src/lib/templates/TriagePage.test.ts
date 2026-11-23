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
  {
    key: 'project',
    kind: 'project',
    title: 'Project column',
    href: '/projects/relay',
    state: 'running',
  },
  { key: 'epic', kind: 'epic', title: 'Epic column', href: '/epics/relay', state: 'running' },
  {
    key: 'plan',
    kind: 'plan',
    title: 'Plan column',
    href: '/plans/fix-relay',
    state: 'three left',
  },
  {
    key: 'task',
    kind: 'plan',
    title: 'Object column',
    href: '/plans/fix-relay/tasks/TASK-1',
    state: 'done',
  },
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

  it('binds the page inset it measures on the window', async () => {
    const { frame } = await renderWindow(1048);
    expect(frame.style.getPropertyValue('--triage-inset')).toBe('24px');

    reportBox(frame, 390, 800);
    await tick();
    expect(frame.style.getPropertyValue('--triage-inset')).toBe('12px');
  });

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
    const markup = source
      .replace(/<script[\s\S]*?<\/script>/g, '')
      .replace(/<style[\s\S]*?<\/style>/g, '');

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
    expect(screen.getByRole('button', { name: 'project, Project column' })).toHaveAttribute(
      'title',
      'Show this level; the selection stays',
    );
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

  it('keeps the window honest when a key move after a peek reaches the object', async () => {
    await renderWindow(1168);
    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    // The peek at the lens shows the lens, the project and the epic; `]` shifts it to the plan,
    // which still leaves the object to its right.
    const epic = screen.getByRole('heading', { name: 'Epic column' });
    epic.focus();
    await fireEvent.keyDown(epic, { key: ']' });
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '1');

    // `]` again reaches the object; the window is then the object's own, not a peek.
    await fireEvent.keyDown(document.activeElement as HTMLElement, { key: ']' });

    const normal = windowFor({ container: 1216, depth: LEVELS.length });
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    expect(screen.getByRole('button', { name: 'report, Lens list' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'project, Project column' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Back to Object column' })).toBeNull();
    expect(widthOf(screen.getByTestId('triage-detail'))).toBe(normal.objectWidth);
  });

  it('clears a peek when a resize leaves one column, so widening shows the object', async () => {
    const { frame } = await renderWindow(1168);
    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '0');

    reportBox(frame, 496, 800);
    await tick();
    expect(columns()).toHaveLength(1);

    reportBox(frame, 1216, 800);
    await tick();

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    expect(screen.getByText('Object body')).toBeInTheDocument();
  });

  it('clears a peek that ended at the object, so narrowing cannot resurrect it', async () => {
    const { frame } = await renderWindow(1168);
    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    // Two presses of `]` walk the window to the object, which drops the peek from the state.
    const epic = screen.getByRole('heading', { name: 'Epic column' });
    epic.focus();
    await fireEvent.keyDown(epic, { key: ']' });
    await fireEvent.keyDown(document.activeElement as HTMLElement, { key: ']' });
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');

    // A window of two columns would hold a stale peek at the plan and hide the object behind it.
    reportBox(frame, 832, 800);
    await tick();

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '3');
    expect(screen.getByText('Object body')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Back to Object column' })).toBeNull();
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
    const window_ = document.querySelector('.triage-page__window') as HTMLElement;
    const before = get(runtimeStore).quietNavigationNotice;

    // Cmd+[ is the browser's Back; AltGr+8 types `[` on a German keyboard. The handler sits on the
    // window, so the events are dispatched there, and neither is cancelled.
    const first = await fireEvent.keyDown(window_, { key: '[', metaKey: true });
    const second = await fireEvent.keyDown(window_, { key: '[', altKey: true, ctrlKey: true });

    expect(first).toBe(true);
    expect(second).toBe(true);
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    expect(get(runtimeStore).quietNavigationNotice).toBe(before);
  });

  it('leaves the keys to a focused field', async () => {
    render(TriagePageFixture, {
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

  it('slides a shift on the columns and not at all under reduced motion', () => {
    const style = styleOf(SOURCE);
    const source = fs.readFileSync(SOURCE, 'utf-8');

    // The slide is replayed on the columns themselves; the window is never keyed on the start, which
    // remounted every column, the strip and the chat on any shift.
    expect(ruleOf(style, '.triage-page__column--shift')).toContain(
      'animation: triage-page-shift var(--duration-base)',
    );
    expect(source).not.toContain('{#key layout.start}');
    const reduced = style.slice(style.indexOf('@media (prefers-reduced-motion: reduce)'));
    expect(reduced).toContain('animation: none');
  });

  it('keeps a list column scroll position when the window shifts', async () => {
    const { frame } = await renderWindow(1168);
    const plan = document.querySelector('[data-column-index="3"]') as HTMLElement;
    plan.scrollTop = 120;

    reportBox(frame, 1376, 800);
    await tick();

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '1');
    expect(document.querySelector('[data-column-index="3"]')).toBe(plan);
    expect(plan.scrollTop).toBe(120);
  });

  it('keeps a chat draft when the window shifts', async () => {
    render(TriagePageFixture, {
      props: {
        levels: LEVELS,
        label: 'Levels',
        bodies: BODIES,
        objectBody: 'Object body',
        sideBody: 'The chat',
        sideOpen: true,
        sideFieldLabel: 'Chat draft',
      },
    });
    const frame = screen.getByTestId('triage-page');
    reportBox(frame, 1216, 800);
    await tick();
    const field = screen.getByRole('textbox', { name: 'Chat draft' });
    await fireEvent.input(field, { target: { value: 'half a thought' } });

    reportBox(frame, 1640, 800);
    await tick();

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '2');
    expect(screen.getByRole('textbox', { name: 'Chat draft' })).toBe(field);
    expect(field).toHaveValue('half a thought');
  });

  it('puts no transformed ancestor above the phone overlay', async () => {
    const props = {
      levels: LEVELS,
      label: 'Levels',
      bodies: BODIES,
      objectBody: 'Object body',
      sideBody: 'The chat',
      sideOpen: false,
    };
    const rendered = render(TriagePageFixture, { props });
    reportBox(screen.getByTestId('triage-page'), 390, 800);
    await tick();
    expect(screen.queryByTestId('side-column')).toBeNull();

    // The chat is opened at the phone width: it takes the screen as a fixed overlay.
    await rendered.rerender({ ...props, sideOpen: true });
    await tick();

    const overlay = screen.getByTestId('side-column');
    expect(overlay).toHaveClass('side-column--taken');

    // jsdom lays nothing out, so the stylesheet is the load-bearing witness: the slide animates the
    // columns, never the group that holds the overlay, and a computed read adds the intent.
    const style = styleOf(SOURCE);
    const windowRule = ruleOf(style, '.triage-page__window');
    expect(windowRule).not.toBe('');
    expect(windowRule).not.toContain('animation');
    expect(windowRule).not.toContain('transform');

    const ancestors: HTMLElement[] = [];
    for (
      let ancestor = overlay.parentElement;
      ancestor !== null;
      ancestor = ancestor.parentElement
    ) {
      ancestors.push(ancestor);
    }
    expect(ancestors.length).toBeGreaterThan(0);
    for (const ancestor of ancestors) {
      const transform = getComputedStyle(ancestor).transform;
      expect(transform === 'none' || transform === '').toBe(true);
    }
  });

  it('replays no slide where the person asked for reduced motion', async () => {
    const original = window.matchMedia;
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: true,
      media: query,
    })) as unknown as typeof window.matchMedia;

    try {
      const { frame } = await renderWindow(1168);
      reportBox(frame, 1376, 800);
      await tick();

      expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '1');
      expect(document.querySelector('.triage-page__column--shift')).toBeNull();
      const reduced = styleOf(SOURCE).slice(
        styleOf(SOURCE).indexOf('@media (prefers-reduced-motion: reduce)'),
      );
      expect(reduced).toContain('animation: none');
      expect(reduced).toContain('transition: none');
    } finally {
      window.matchMedia = original;
    }
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

  it('moves the focus into the chat and back to the column it came from', async () => {
    const props = {
      levels: LEVELS,
      label: 'Levels',
      bodies: BODIES,
      objectBody: 'Object body',
      sideBody: 'The chat',
      sideOpen: false,
    };
    const rendered = render(TriagePageFixture, { props });
    const frame = screen.getByTestId('triage-page');
    reportBox(frame, 1216, 800);
    await tick();
    const opener = screen.getByRole('heading', { name: 'Plan column' });
    opener.focus();

    await rendered.rerender({ ...props, sideOpen: true });
    reportBox(frame, 792, 800);
    await tick();

    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');
    expect(document.activeElement).toHaveTextContent('Hide the chat');

    await rendered.rerender({ ...props, sideOpen: false });
    await tick();

    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '3');
    // The columns unmount under the chat, so the control comes back as the heading of the column
    // that held the focus — never the page body.
    expect(document.activeElement?.closest('[data-column-index]')).toHaveAttribute(
      'data-column-index',
      '3',
    );
    expect(document.activeElement).toHaveTextContent('Plan column');
  });

  it('leaves an Escape the page already handled to the page', async () => {
    const onHideSide = vi.fn();
    render(TriagePageFixture, {
      props: {
        levels: LEVELS,
        label: 'Levels',
        bodies: BODIES,
        objectBody: 'Object body',
        sideBody: 'The chat',
        sideOpen: true,
        onHideSide,
      },
    });
    reportBox(screen.getByTestId('triage-page'), 1048, 800);
    await tick();

    // An open ConfirmDialog also keeps the key, but by stopping it at the window's capture phase
    // (`Dialog.svelte`), so it never reaches this template's group at all: no assertion here can
    // tell that stop from this guard. The witness is a widget that marks the key handled and lets
    // it bubble, which is what separates a handled Escape from a free one.
    const menu = document.createElement('button');
    menu.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
      }
    });
    document.querySelector('.triage-page__window')?.append(menu);
    menu.focus();
    await fireEvent.keyDown(menu, { key: 'Escape' });

    expect(onHideSide).not.toHaveBeenCalled();
    menu.remove();
  });

  it('keeps the focus in a chat that only becomes docked', async () => {
    const props = {
      levels: LEVELS,
      label: 'Levels',
      bodies: BODIES,
      objectBody: 'Object body',
      sideBody: 'The chat',
      sideFieldLabel: 'Chat draft',
      sideOpen: false,
    };
    const rendered = render(TriagePageFixture, { props });
    const frame = screen.getByTestId('triage-page');
    reportBox(frame, 1216, 800);
    await tick();
    screen.getByRole('heading', { name: 'Plan column' }).focus();

    // The chat takes the space, then the container widens until it docks beside the columns: it is
    // still open, so its own control keeps the focus.
    await rendered.rerender({ ...props, sideOpen: true });
    reportBox(frame, 792, 800);
    await tick();
    const editor = screen.getByRole('textbox', { name: 'Chat draft' });
    editor.focus();

    reportBox(frame, 1216, 800);
    await tick();

    expect(screen.getByTestId('side-column')).not.toHaveClass('side-column--taken');
    expect(document.activeElement).toBe(editor);
  });

  it('returns the focus to a column the window shows, not to one it dropped', async () => {
    const props = {
      levels: LEVELS,
      label: 'Levels',
      bodies: BODIES,
      objectBody: 'Object body',
      sideBody: 'The chat',
      sideOpen: false,
    };
    const rendered = render(TriagePageFixture, { props });
    const frame = screen.getByTestId('triage-page');
    reportBox(frame, 1216, 800);
    await tick();
    // The peek starts the window at the lens, and the chat is opened from there.
    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));
    expect(document.activeElement).toHaveTextContent('Lens list');

    await rendered.rerender({ ...props, sideOpen: true });
    reportBox(frame, 792, 800);
    await tick();
    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');

    await rendered.rerender({ ...props, sideOpen: false });
    await tick();

    // The window is the object's own now, so the lens is gone; the focus goes to a column that is
    // there rather than falling to the page.
    expect(document.querySelector('.triage-page__window')).toHaveAttribute('data-start', '3');
    expect(document.activeElement).not.toBe(document.body);
    expect(document.activeElement?.closest('[data-column-index]')).toHaveAttribute(
      'data-column-index',
      '3',
    );
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

  it('renders UpLink in running work with its accessible name and pushing parent (PLAN-144 D2)', async () => {
    runtimeStore.update((s) => ({
      ...s,
      currentRoute: 'plan',
      selectedPlanId: 'fix-relay',
      currentProcessTree: {
        plan_id: 'fix-relay',
        title: 'Fix Relay Transport',
        project: { project_id: 'pm-mcp-clients', display_name: 'Relay Transport' },
        epic: { epic_id: 'relay-hardening', title: 'Relay Hardening' },
        current_phase: 'execute',
        phase_state: 'running',
        needs_operator: false,
        instance_version: 1,
        tokens_so_far: { total_tokens: 100, cost_usd: 0.01, duration_ms: 1000 },
        nodes: [],
        current_position_path: [],
      },
    }));

    await renderSide(1216, false, () => {}, false);

    const upLink = screen.getByTestId('up-link');
    expect(upLink).toBeInTheDocument();
    expect(upLink).toHaveAttribute('aria-label', 'Up to epic Relay Hardening');
    expect(upLink).toHaveTextContent('Relay Hardening');
  });

  it('renders no Up outside running work (e.g. on reports or cleanup)', async () => {
    runtimeStore.update((s) => ({
      ...s,
      currentRoute: 'reports',
      currentProcessTree: null,
      currentEpicResource: null,
    }));

    await renderSide(1216, false, () => {}, false);

    expect(screen.queryByTestId('up-link')).toBeNull();
  });
});
