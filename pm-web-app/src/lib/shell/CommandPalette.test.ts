import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CommandPalette from './CommandPalette.svelte';
import { apiClient } from '../api/client';
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
 * Asserts that every child of the listbox is an option or a group of options, and that neither the
 * children nor their descendants carry focusable or editable content.
 *
 * @param box the listbox
 */
function expectOptionsAndGroupsOnly(box: HTMLElement): void {
  const focusable =
    'button, a[href], input, select, textarea, [contenteditable]:not([contenteditable="false"]), [tabindex]:not([tabindex="-1"])';
  for (const child of childrenOf(box)) {
    const role = child.getAttribute('role');
    expect(['option', 'group']).toContain(role);
    if (role === 'group') {
      expect(child.querySelectorAll('[role="option"]').length).toBeGreaterThan(0);
      for (const heading of Array.from(
        child.querySelectorAll('h1, h2, h3, h4, h5, h6'),
      )) {
        expect(heading).toHaveAttribute('role', 'presentation');
      }
    }
    expect(child.matches(focusable)).toBe(false);
    for (const element of Array.from(child.querySelectorAll('*'))) {
      const insideOption = element.parentElement?.closest('[role="option"]') != null;
      const elementRole = element.getAttribute('role');
      if (!insideOption) {
        if (elementRole === null) {
          expect(['DIV', 'SPAN']).toContain(element.tagName);
        } else {
          expect(['presentation', 'option']).toContain(elementRole);
        }
      }
      expect(element.matches(focusable)).toBe(false);
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

  it('keeps Retry outside the listbox after a failed read and sends the query again', async () => {
    const findWork = vi
      .spyOn(apiClient, 'findWork')
      .mockRejectedValue(new Error('unavailable'));
    openPalette({ text: 'relay', matches: [], status: 'failed' });

    render(CommandPalette);

    const box = screen.getByRole('listbox');
    const field = screen.getByTestId('command-palette-field') as HTMLInputElement;
    expect(box.contains(screen.getByTestId('command-palette-retry'))).toBe(false);

    // The field holds the failed text; the read fails again, and Retry sends the same query.
    await fireEvent.input(field, { target: { value: 'relay' } });
    await waitFor(() => expect(findWork).toHaveBeenCalledTimes(1));

    const retry = await screen.findByTestId('command-palette-retry');
    expect(box.contains(retry)).toBe(false);
    expect(retry.tagName).toBe('BUTTON');
    expect(retry).not.toHaveAttribute('tabindex');
    expect(field).toHaveAttribute('aria-expanded', 'false');

    await fireEvent.click(retry);
    await waitFor(() => expect(findWork).toHaveBeenCalledTimes(2));
    expect(findWork).toHaveBeenLastCalledWith('relay', expect.anything());
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
    const ids = Array.from(
      screen.getByRole('listbox').querySelectorAll('[role="option"]'),
      (option) => option.id,
    );
    expect(ids.length).toBeGreaterThan(1);
    expect(new Set(ids).size).toBe(ids.length);

    searchIndexStore.set({ text: 'fix', matches: [PLAN_MATCH], status: 'done' });

    await waitFor(() => {
      const changed = field.getAttribute('aria-activedescendant');
      expect(changed).toBeTruthy();
      const option = document.getElementById(changed ?? '');
      expect(option?.getAttribute('role')).toBe('option');
      expect(option?.textContent).toContain('Fix Relay Transport');
    });
  });

  it('brings the selected option into view when ArrowDown moves the selection', async () => {
    const scrolled: Element[] = [];
    const scroll = vi.fn(function (this: Element) {
      scrolled.push(this);
    });
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
      scrolled.length = 0;
      await fireEvent.keyDown(field, { key: 'ArrowDown' });

      await waitFor(() => {
        const active = field.getAttribute('aria-activedescendant');
        expect(active).toBeTruthy();
        expect(scrolled.at(-1)).toBe(document.getElementById(active ?? ''));
      });
    } finally {
      Element.prototype.scrollIntoView = original;
    }
  });
});
