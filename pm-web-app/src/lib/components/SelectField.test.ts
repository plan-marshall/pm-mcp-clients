import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import SelectField from './SelectField.svelte';
import { styleOf } from '../../test/templateStyle';

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
    expect(select.options.length).toBe(3);
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

  it('uses --control-height and does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './SelectField.svelte'));
    expect(style).toContain('var(--control-height)');
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
