import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CommandPalette from './CommandPalette.svelte';
import {
  commandPaletteOpen,
  searchIndexStore,
  type FindMatch,
  type SearchIndexState,
} from '../stores/searchIndex';

/**
 * The command palette's listbox (UX-P-02, UC-PLN-8).
 *
 * The panel is one combobox over one listbox: the listbox holds options and groups of options and
 * nothing else, the empty text and the failed-read `Retry` sit beside it, a group is named once by
 * its heading, `aria-expanded` follows the rows, `aria-activedescendant` never names an id that is
 * not in the document, and the selected option is brought into view when the selection moves.
 */

const PROJECT_MATCH: FindMatch = {
  kind: 'project',
  id: 'pm-mcp-core',
  title: 'pm-mcp-core',
  project: { id: 'pm-mcp-core', title: 'pm-mcp-core' },
  epic: null,
  plan: null,
  ended: false,
  outcome: null,
};

const EPIC_MATCH: FindMatch = {
  kind: 'epic',
  id: 'relay-hardening',
  title: 'Relay Hardening',
  project: { id: 'pm-mcp-clients', title: 'pm-mcp-clients' },
  epic: null,
  plan: null,
  ended: false,
  outcome: null,
};

const PLAN_MATCH: FindMatch = {
  kind: 'plan',
  id: 'fix-relay',
  title: 'Fix Relay Transport',
  project: { id: 'pm-mcp-clients', title: 'pm-mcp-clients' },
  epic: { id: 'relay-hardening', title: 'Relay Hardening' },
  plan: null,
  ended: true,
  outcome: 'done',
};

/**
 * Opens the panel in one state.
 *
 * @param state the search index state the palette draws
 */
function openPalette(state: SearchIndexState): void {
  searchIndexStore.set(state);
  commandPaletteOpen.set(true);
}

/** @param box the listbox @returns its element children, in document order */
function childrenOf(box: HTMLElement): HTMLElement[] {
  return Array.from(box.children) as HTMLElement[];
}

/**
 * Asserts that every child of the listbox is an option or a group of options.
 *
 * @param box the listbox
 */
function expectOptionsAndGroupsOnly(box: HTMLElement): void {
  for (const child of childrenOf(box)) {
    const role = child.getAttribute('role');
    expect(['option', 'group']).toContain(role);
    if (role === 'group') {
      expect(child.querySelectorAll('[role="option"]').length).toBeGreaterThan(0);
    }
  }
}

describe('the command palette listbox', () => {
  beforeEach(() => {
    commandPaletteOpen.set(false);
    searchIndexStore.set({ text: '', matches: [], status: 'idle' });
  });

  it('holds nothing but options and groups of options while the field is empty', () => {
    openPalette({ text: '', matches: [], status: 'idle' });

    render(CommandPalette);

    const box = screen.getByRole('listbox');
    expect(childrenOf(box)).toEqual([]);
    expect(screen.getByTestId('command-palette-field')).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('command-palette-empty')).not.toBeInTheDocument();
    expect(screen.queryByTestId('command-palette-retry')).not.toBeInTheDocument();
  });

  it('holds nothing but options and groups of options when matches arrive', () => {
    openPalette({
      text: 'rel',
      matches: [PROJECT_MATCH, EPIC_MATCH, PLAN_MATCH],
      status: 'done',
    });

    render(CommandPalette);

    const box = screen.getByRole('listbox');
    expect(childrenOf(box).length).toBeGreaterThan(0);
    expectOptionsAndGroupsOnly(box);
    expect(screen.getByTestId('command-palette-field')).toHaveAttribute('aria-expanded', 'true');
  });

  it('names a group once, through the heading its aria-labelledby points at', () => {
    openPalette({
      text: 'rel',
      matches: [PROJECT_MATCH, EPIC_MATCH, PLAN_MATCH],
      status: 'done',
    });

    render(CommandPalette);

    const groups = Array.from(
      screen.getByRole('listbox').querySelectorAll('[role="group"]'),
    ) as HTMLElement[];
    expect(groups.length).toBeGreaterThan(0);
    for (const group of groups) {
      const labelledBy = group.getAttribute('aria-labelledby');
      expect(labelledBy).toBeTruthy();
      expect(group).not.toHaveAttribute('aria-label');
      const heading = labelledBy ? document.getElementById(labelledBy) : null;
      expect(heading).not.toBeNull();
      expect(heading?.textContent?.trim().length).toBeGreaterThan(0);
    }
  });

  it('keeps the empty text outside the listbox when nothing matches', () => {
    openPalette({ text: 'relayy', matches: [], status: 'done' });

    render(CommandPalette);

    const box = screen.getByRole('listbox');
    const empty = screen.getByTestId('command-palette-empty');
    expect(box.contains(empty)).toBe(false);
    expect(empty).toHaveTextContent('Nothing matches');
    expect(childrenOf(box)).toEqual([]);
    expect(screen.getByTestId('command-palette-field')).toHaveAttribute('aria-expanded', 'false');
  });

  it('keeps Retry outside the listbox after a failed read', () => {
    openPalette({ text: 'relay', matches: [], status: 'failed' });

    render(CommandPalette);

    const box = screen.getByRole('listbox');
    const retry = screen.getByTestId('command-palette-retry');
    expect(box.contains(retry)).toBe(false);
    expect(retry.tagName).toBe('BUTTON');
    expect(retry).not.toHaveAttribute('tabindex');
    expect(screen.getByTestId('command-palette-field')).toHaveAttribute('aria-expanded', 'false');
  });

  it('names an option that is in the document after the answer changes', async () => {
    openPalette({
      text: 'rel',
      matches: [PROJECT_MATCH, EPIC_MATCH, PLAN_MATCH],
      status: 'done',
    });

    render(CommandPalette);

    const field = screen.getByTestId('command-palette-field');
    const first = field.getAttribute('aria-activedescendant');
    expect(first).toBeTruthy();
    expect(document.getElementById(first ?? '')?.getAttribute('role')).toBe('option');

    searchIndexStore.set({ text: 'fix', matches: [PLAN_MATCH], status: 'done' });

    await waitFor(() => {
      const changed = field.getAttribute('aria-activedescendant');
      if (changed) {
        expect(document.getElementById(changed)?.getAttribute('role')).toBe('option');
      }
    });
  });

  it('brings the selected option into view when ArrowDown moves the selection', async () => {
    const scroll = vi.fn();
    const original = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = scroll as unknown as typeof Element.prototype.scrollIntoView;

    try {
      openPalette({
        text: 'rel',
        matches: [PROJECT_MATCH, EPIC_MATCH, PLAN_MATCH],
        status: 'done',
      });

      render(CommandPalette);

      const field = screen.getByTestId('command-palette-field');
      scroll.mockClear();
      await fireEvent.keyDown(field, { key: 'ArrowDown' });

      await waitFor(() => expect(scroll).toHaveBeenCalled());
    } finally {
      Element.prototype.scrollIntoView = original;
    }
  });
});
