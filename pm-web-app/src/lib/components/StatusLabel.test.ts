import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import StatusLabel from './StatusLabel.svelte';
import StatusLabelTemplateFixture from '../../test/fixtures/StatusLabelTemplateFixture.svelte';
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

  it('compiles and renders inside snippets and component children via StatusLabelTemplateFixture', () => {
    const { container } = render(StatusLabelTemplateFixture);

    const labels = container.querySelectorAll('.status-label');
    expect(labels).toHaveLength(6);
    for (const label of labels) {
      expect(label).toHaveClass('status-label--fixed');
    }
  });

  it('draws a fixed state slot of 8.5rem for a list, keeping dot and word in separate columns', () => {
    const { container } = render(StatusLabel, {
      props: { tone: 'running', label: 'running', fixed: true },
    });

    const el = container.querySelector('.status-label');
    expect(el).toHaveClass('status-label--fixed');

    const fixedRule = ruleOf(STYLE, '.status-label--fixed');
    expect(fixedRule).toContain('display: inline-grid');
    expect(fixedRule).toContain('inline-size: 8.5rem');
    expect(fixedRule).toContain('grid-template-columns: auto minmax(0, 1fr)');
    expect(fixedRule).toContain('align-items: start');

    const wordRule = ruleOf(STYLE, '.status-label--fixed .status-label__word');
    expect(wordRule).toContain('overflow-wrap: anywhere');
    expect(wordRule).toContain('min-inline-size: 0');
  });

  it('shows the word it is given, never a wire value of its own', () => {
    render(StatusLabel, { props: { tone: 'attention', label: 'wartet auf dich' } });

    expect(screen.getByText('wartet auf dich')).toBeInTheDocument();
    // The state's word comes from the view's catalogue key; the component translates nothing.
    expect(SOURCE).not.toContain('$t(');
    expect(SOURCE).not.toContain('humanizeKey');
  });
});
