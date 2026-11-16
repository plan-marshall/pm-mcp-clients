import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import NumberField from './NumberField.svelte';
import { styleOf } from '../../test/templateStyle';

describe('NumberField', () => {
  it('renders numeric input with label and unit beside control', () => {
    const { container } = render(NumberField, {
      props: { label: 'Retention', unit: 'days', id: 'retention-input', value: 30 },
    });
    const input = container.querySelector('input[type="number"]');
    const label = container.querySelector('label');
    const unit = container.querySelector('.number-field__unit');

    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('inputmode', 'numeric');
    expect(label?.textContent).toContain('Retention');
    expect(unit?.textContent).toContain('days');
    expect(input).toHaveValue(30);
  });

  it('enforces min, max and step bounds', async () => {
    const onchange = vi.fn();
    const { container } = render(NumberField, {
      props: { label: 'Count', min: 1, max: 10, step: 1, value: 5, onchange },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    expect(input).toHaveAttribute('min', '1');
    expect(input).toHaveAttribute('max', '10');
    expect(input).toHaveAttribute('step', '1');

    await fireEvent.input(input, { target: { value: '15' } });
    await fireEvent.change(input);
    // Value clamped or flagged invalid
    expect(input.value).toBe('10');
  });

  it('offers explicit never choice when allowNever is true, with null value', async () => {
    const onchange = vi.fn();
    const { container } = render(NumberField, {
      props: { label: 'Timeout', unit: 'seconds', allowNever: true, value: 60, onchange },
    });

    const neverCheck = container.querySelector('.number-field__never input[type="checkbox"]') as HTMLInputElement;
    const numInput = container.querySelector('input[type="number"]') as HTMLInputElement;

    expect(neverCheck).toBeInTheDocument();
    expect(container.querySelector('.number-field__never')?.textContent).toContain('Never');
    expect(neverCheck.checked).toBe(false);
    expect(numInput).not.toBeDisabled();

    // Check "Never"
    await fireEvent.click(neverCheck);
    expect(neverCheck.checked).toBe(true);
    expect(numInput).toBeDisabled();
    expect(onchange).toHaveBeenCalledWith(null);

    // Uncheck "Never"
    await fireEvent.click(neverCheck);
    expect(neverCheck.checked).toBe(false);
    expect(numInput).not.toBeDisabled();
    expect(onchange).toHaveBeenCalledWith(60);
  });

  it('starts in never state if initial value is null', () => {
    const { container } = render(NumberField, {
      props: { label: 'Expiry', allowNever: true, value: null },
    });
    const neverCheck = container.querySelector('.number-field__never input[type="checkbox"]') as HTMLInputElement;
    const numInput = container.querySelector('input[type="number"]') as HTMLInputElement;

    expect(neverCheck.checked).toBe(true);
    expect(numInput).toBeDisabled();
  });

  it('uses --control-height and does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './NumberField.svelte'));
    expect(style).toContain('var(--control-height)');
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
