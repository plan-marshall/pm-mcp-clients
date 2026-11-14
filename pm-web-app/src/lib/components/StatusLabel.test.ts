import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import StatusLabel from './StatusLabel.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `StatusLabel` (components-tags.adoc, status-label): an 8 px dot in the state colour and the state
 * word in `--color-text-secondary`. One per row; it replaces the state pill. The word is the
 * caller's catalogue word, never a wire value, and a list keeps the dot in a slot of its own so the
 * dots stand one under the other.
 */

const COMPONENT = path.resolve(__dirname, 'StatusLabel.svelte');
const STYLE = styleOf(COMPONENT);
const SOURCE = fs.readFileSync(COMPONENT, 'utf-8');

describe('StatusLabel', () => {
  it('draws the state word beside a dot that only carries the colour', () => {
    const { container } = render(StatusLabel, {
      props: { tone: 'running', label: 'running' },
    });
    const label = screen.getByText('running');
    const dot = container.querySelector('.status-label__dot');

    expect(label).toHaveClass('status-label__word');
    expect(dot).not.toBeNull();
    expect(dot?.getAttribute('aria-hidden')).toBe('true');
  });

  it.each(['neutral', 'running', 'attention', 'danger', 'success'] as const)(
    'draws the %s state colour',
    (tone) => {
      const { container } = render(StatusLabel, { props: { tone, label: 'state' } });

      expect(container.querySelector('.status-label')?.className).toContain(
        `status-label--${tone}`,
      );
    },
  );

  it('is neutral unless a view names the state colour', () => {
    const { container } = render(StatusLabel, { props: { label: 'staged' } });

    expect(container.querySelector('.status-label')?.className).toContain('status-label--neutral');
  });

  it('draws a fixed state slot for a list', () => {
    const { container } = render(StatusLabel, {
      props: { tone: 'running', label: 'running', slot: true },
    });

    expect(container.querySelector('.status-label')).toHaveClass('status-label--slot');
    expect(ruleOf(STYLE, '.status-label--slot')).toContain('display: inline-grid');
    expect(ruleOf(STYLE, '.status-label--slot')).toContain(
      'grid-template-columns: auto minmax(0, 1fr)',
    );
  });

  it('wraps a long word without moving the dot', () => {
    const slot = ruleOf(STYLE, '.status-label--slot');
    const word = ruleOf(STYLE, '.status-label--slot .status-label__word');

    expect(slot).toContain('align-items: start');
    expect(word).toContain('overflow-wrap: anywhere');
    // The dot keeps its column, so a wrapped word in the second column moves no dot.
    expect(word).toContain('min-inline-size: 0');
  });

  it('shows the word it is given, never a wire value of its own', () => {
    render(StatusLabel, { props: { tone: 'attention', label: 'wartet auf dich' } });

    expect(screen.getByText('wartet auf dich')).toBeInTheDocument();
    // The state's word comes from the view's catalogue key; the component translates nothing.
    expect(SOURCE).not.toContain('$t(');
    expect(SOURCE).not.toContain('humanizeKey');
  });
});
