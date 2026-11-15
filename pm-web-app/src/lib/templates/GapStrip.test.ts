import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import path from 'node:path';
import GapStripFixture from '../../test/fixtures/GapStripFixture.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';
import type { Level } from './TriagePage.svelte';

/**
 * The gap strip (`navigation.adoc#gap-strip`): the levels the window hides, from the lens down,
 * stacked as marks with their names visible — or, where the names would leave a single column, as
 * marks only behind a _Names_ popover. A mark peeks at its level; the strip at the right edge
 * returns to the object.
 */

const SOURCE = path.resolve(__dirname, './GapStrip.svelte');

/** Two hidden levels, as a window of three columns leaves them. */
const HIDDEN: Array<{ level: Level; index: number }> = [
  {
    level: { key: 'lens', kind: 'report', title: 'Lens list', href: '/reports', state: 'new' },
    index: 0,
  },
  {
    level: {
      key: 'project',
      kind: 'project',
      title: 'Project column',
      href: '/projects/relay',
      state: 'running',
    },
    index: 1,
  },
];

describe('the gap strip', () => {
  it('names the hidden levels, each a button of the group', () => {
    render(GapStripFixture, {
      props: { hidden: HIDDEN, names: true, label: 'Hidden levels' },
    });

    const group = screen.getByRole('group', { name: 'Hidden levels' });

    expect(group).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'report, Lens list' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'project, Project column' })).toBeInTheDocument();
    // The names stand beside the marks, so no popover is offered.
    expect(screen.queryByRole('button', { name: 'Names' })).toBeNull();
    expect(screen.getByText('Lens list')).toBeInTheDocument();
  });
  it('shows marks only, with the advisory, where the names would leave one column', () => {
    render(GapStripFixture, {
      props: { hidden: HIDDEN, names: false, label: 'Hidden levels' },
    });

    const mark = screen.getByRole('button', { name: 'project, Project column' });

    expect(mark).toHaveAttribute('title', 'Show this level; the selection stays');
    expect(mark.querySelector('.kind-mark__word')).toHaveClass('sr-only');
  });

  it('opens the popover of names behind the last button, one step each', async () => {
    const peeks: number[] = [];
    render(GapStripFixture, {
      props: {
        hidden: HIDDEN,
        names: false,
        label: 'Hidden levels',
        onPeek: (index: number) => peeks.push(index),
      },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Names' }));

    const names = screen.getByRole('group', { name: 'Names' });
    expect(names).toHaveTextContent('Lens list');
    expect(names).toHaveTextContent('Project column');

    await fireEvent.click(screen.getByRole('button', { name: 'Project column' }));

    expect(peeks).toEqual([1]);
  });

  it('stands the popover over the columns, so the narrow strip never clips the names', async () => {
    render(GapStripFixture, {
      props: { hidden: HIDDEN, names: false, label: 'Hidden levels' },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Names' }));

    const popover = ruleOf(styleOf(SOURCE), '.gap-strip__popover');

    expect(popover).toContain('position: absolute');
    expect(popover).toContain('inline-size: 14rem');
    expect(ruleOf(styleOf(SOURCE), '.gap-strip--start')).toContain('position: relative');
    // The strip itself may not clip what its popover shows.
    expect(ruleOf(styleOf(SOURCE), '.gap-strip')).not.toContain('overflow-x: hidden');
  });

  it('closes the popover on Escape and returns the focus to its control', async () => {
    render(GapStripFixture, {
      props: { hidden: HIDDEN, names: false, label: 'Hidden levels' },
    });

    const namesButton = screen.getByRole('button', { name: 'Names' });
    await fireEvent.click(namesButton);
    expect(screen.getByRole('group', { name: 'Names' })).toBeInTheDocument();

    await fireEvent.keyDown(screen.getByRole('group', { name: 'Names' }), { key: 'Escape' });

    expect(screen.queryByRole('group', { name: 'Names' })).toBeNull();
    expect(document.activeElement).toBe(namesButton);
  });

  it('peeks at the level of the mark that was selected', async () => {
    const peeks: number[] = [];
    render(GapStripFixture, {
      props: {
        hidden: HIDDEN,
        names: true,
        label: 'Hidden levels',
        onPeek: (index: number) => peeks.push(index),
      },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'report, Lens list' }));

    expect(peeks).toEqual([0]);
  });

  it('returns to the object from the strip at the right edge', async () => {
    const onReturn = vi.fn();
    render(GapStripFixture, {
      props: {
        placement: 'end',
        objectTitle: 'Object column',
        label: 'Hidden levels',
        onReturn,
      },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Back to Object column' }));

    expect(onReturn).toHaveBeenCalledOnce();
  });
});
