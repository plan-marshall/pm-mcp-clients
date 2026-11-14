import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import path from 'node:path';
import fs from 'node:fs';
import SegmentedControl from './SegmentedControl.svelte';

function styleOf(filePath: string): string {
  const content = fs.readFileSync(filePath, 'utf-8');
  const styleMatch = content.match(/<style[^>]*>([\s\S]*?)<\/style>/);
  return styleMatch ? styleMatch[1] : '';
}

function rulesOf(css: string, selector: string): string[] {
  const rules: string[] = [];
  const pattern = new RegExp(
    `(?:^|[;{}\\s])${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([^}]+)\\}`,
    'g',
  );
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(css)) !== null) {
    rules.push(match[1].trim());
  }
  return rules;
}

describe('SegmentedControl component', () => {
  const defaultOptions = [
    { id: 'plans', label: 'By Plan' },
    { id: 'epics', label: 'By Epic' },
  ];

  it('rejects option counts outside 2 to 5', () => {
    // Rejects 1 option
    expect(() => {
      render(SegmentedControl, {
        props: {
          label: 'Grouping',
          value: 'opt1',
          options: [{ id: 'opt1', label: 'Option 1' }],
        },
      });
    }).toThrow(/between 2 and 5/i);

    // Rejects 6 options
    expect(() => {
      render(SegmentedControl, {
        props: {
          label: 'Grouping',
          value: 'opt1',
          options: [
            { id: '1', label: '1' },
            { id: '2', label: '2' },
            { id: '3', label: '3' },
            { id: '4', label: '4' },
            { id: '5', label: '5' },
            { id: '6', label: '6' },
          ],
        },
      });
    }).toThrow(/between 2 and 5/i);
  });

  it('renders a radiogroup with aria-checked on each option and single tab stop', () => {
    render(SegmentedControl, {
      props: {
        label: 'View Mode',
        value: 'plans',
        options: defaultOptions,
      },
    });

    const group = screen.getByRole('radiogroup', { name: 'View Mode' });
    expect(group).toBeInTheDocument();

    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(2);

    expect(radios[0]).toHaveAttribute('aria-checked', 'true');
    expect(radios[0]).toHaveAttribute('tabindex', '0');
    expect(radios[0]).toHaveTextContent('By Plan');

    expect(radios[1]).toHaveAttribute('aria-checked', 'false');
    expect(radios[1]).toHaveAttribute('tabindex', '-1');
    expect(radios[1]).toHaveTextContent('By Epic');
  });

  it('calls onChange when clicking an option', async () => {
    const onChange = vi.fn();
    render(SegmentedControl, {
      props: {
        label: 'View Mode',
        value: 'plans',
        options: defaultOptions,
        onChange,
      },
    });

    const epicsRadio = screen.getByRole('radio', { name: 'By Epic' });
    await fireEvent.click(epicsRadio);

    expect(onChange).toHaveBeenCalledWith('epics');
  });

  it('navigates and wraps with ArrowDown/ArrowRight and ArrowUp/ArrowLeft', async () => {
    const onChange = vi.fn();
    const threeOptions = [
      { id: 'a', label: 'Option A' },
      { id: 'b', label: 'Option B' },
      { id: 'c', label: 'Option C' },
    ];

    render(SegmentedControl, {
      props: {
        label: 'Letters',
        value: 'a',
        options: threeOptions,
        onChange,
      },
    });

    const group = screen.getByRole('radiogroup');
    const radios = screen.getAllByRole('radio');

    radios[0].focus();
    expect(document.activeElement).toBe(radios[0]);

    // ArrowRight moves to next
    await fireEvent.keyDown(radios[0], { key: 'ArrowRight' });
    expect(onChange).toHaveBeenCalledWith('b');
    expect(document.activeElement).toBe(radios[1]);

    // ArrowDown moves to next
    await fireEvent.keyDown(radios[1], { key: 'ArrowDown' });
    expect(onChange).toHaveBeenCalledWith('c');
    expect(document.activeElement).toBe(radios[2]);

    // ArrowRight wraps to first
    await fireEvent.keyDown(radios[2], { key: 'ArrowRight' });
    expect(onChange).toHaveBeenCalledWith('a');
    expect(document.activeElement).toBe(radios[0]);

    // ArrowLeft wraps to last
    await fireEvent.keyDown(radios[0], { key: 'ArrowLeft' });
    expect(onChange).toHaveBeenCalledWith('c');
    expect(document.activeElement).toBe(radios[2]);

    // ArrowUp moves to previous
    await fireEvent.keyDown(radios[2], { key: 'ArrowUp' });
    expect(onChange).toHaveBeenCalledWith('b');
    expect(document.activeElement).toBe(radios[1]);
  });

  it('jumps to first and last option with Home and End', async () => {
    const onChange = vi.fn();
    const fourOptions = [
      { id: '1', label: 'One' },
      { id: '2', label: 'Two' },
      { id: '3', label: 'Three' },
      { id: '4', label: 'Four' },
    ];

    render(SegmentedControl, {
      props: {
        label: 'Numbers',
        value: '2',
        options: fourOptions,
        onChange,
      },
    });

    const radios = screen.getAllByRole('radio');
    radios[1].focus();

    await fireEvent.keyDown(radios[1], { key: 'End' });
    expect(onChange).toHaveBeenCalledWith('4');
    expect(document.activeElement).toBe(radios[3]);

    await fireEvent.keyDown(radios[3], { key: 'Home' });
    expect(onChange).toHaveBeenCalledWith('1');
    expect(document.activeElement).toBe(radios[0]);
  });

  it('conforms to design tokens: brand-subtle background, text-primary, 24px and phone 44px', () => {
    const style = styleOf(path.resolve(__dirname, './SegmentedControl.svelte'));
    const selectedRules = rulesOf(style, '.segmented-control__option[aria-checked="true"]').join(';');
    expect(selectedRules).toContain('var(--color-brand-subtle)');
    expect(selectedRules).toContain('var(--color-text-primary)');
    expect(selectedRules).not.toContain('var(--color-brand);'); // never solid brand fill

    const optionRules = rulesOf(style, '.segmented-control__option').join(';');
    expect(optionRules).toContain('min-block-size: 24px');

    const focusRules = rulesOf(style, '.segmented-control__option:focus').join(';');
    expect(focusRules).toContain('box-shadow: var(--focus-ring)');

    const mediaMatch = style.match(/@media\s*\(max-width:\s*767px\)\s*\{([\s\S]*?\})\s*\}/);
    expect(mediaMatch).not.toBeNull();
    expect(mediaMatch![1]).toContain('min-block-size: 44px');
  });
});
