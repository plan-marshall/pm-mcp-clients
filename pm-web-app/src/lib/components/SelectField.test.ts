import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import SelectField from './SelectField.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';
import {
  REMOVED_OUTLINE,
  TRANSPARENT_OUTLINE,
  sourceOf,
  uniqueIdShape,
} from '../../test/fieldContract';

describe('SelectField', () => {
  const options = [
    { value: 'opt1', label: 'Option 1' },
    { value: 'opt2', label: 'Option 2' },
    { value: 'opt3', label: 'Option 3', disabled: true, disabledReason: 'Not available' },
  ];

  it('renders select with options and label', () => {
    const { container } = render(SelectField, {
      props: { label: 'Model', options, value: 'opt1', id: 'model-select' },
    });
    const select = container.querySelector('select') as HTMLSelectElement;
    const label = container.querySelector('label');

    expect(select).toBeInTheDocument();
    expect(label?.textContent).toContain('Model');
    expect(select.value).toBe('opt1');
    expect(select.options).toHaveLength(3);
  });

  it('applies aria-label when label is omitted', () => {
    const { container } = render(SelectField, {
      props: { ariaLabel: 'Choose model', options, value: 'opt1' },
    });
    const select = container.querySelector('select') as HTMLSelectElement;
    expect(select.getAttribute('aria-label')).toBe('Choose model');
  });

  it('links aria-describedby to hint or error', () => {
    const { container } = render(SelectField, {
      props: { label: 'Model', options, hint: 'Select a model.', id: 'model-select' },
    });
    const select = container.querySelector('select') as HTMLSelectElement;
    const describedBy = select.getAttribute('aria-describedby');

    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Select a model.');
  });

  it('fires onchange when option changes', async () => {
    const onchange = vi.fn();
    const { container } = render(SelectField, {
      props: { label: 'Model', options, value: 'opt1', onchange },
    });
    const select = container.querySelector('select') as HTMLSelectElement;

    await fireEvent.change(select, { target: { value: 'opt2' } });
    expect(onchange).toHaveBeenCalledWith('opt2');
  });

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './SelectField.svelte');

    it('uses --control-height and keeps a transparent focus outline beside the focus ring', () => {
      const style = styleOf(file);
      expect(style).toContain('var(--control-height)');
      expect(style).not.toMatch(REMOVED_OUTLINE);
      const focus = ruleOf(style, '.select-field__select:focus');
      expect(focus).toMatch(TRANSPARENT_OUTLINE);
      expect(focus).toContain('box-shadow: var(--focus-ring)');
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(SelectField, { props: { label, options } }).container.querySelector('select')?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('sel'));
      expect(second).toMatch(uniqueIdShape('sel'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});
