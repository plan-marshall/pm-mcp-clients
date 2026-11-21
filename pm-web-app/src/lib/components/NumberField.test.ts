import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import NumberField from './NumberField.svelte';
import { rulesOf, styleOf } from '../../test/templateStyle';

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

  it('enforces min, max and step bounds on commit, not while typing', async () => {
    const onchange = vi.fn();
    const { container } = render(NumberField, {
      props: { label: 'Count', min: 1, max: 10, step: 1, value: 5, onchange },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    expect(input).toHaveAttribute('min', '1');
    expect(input).toHaveAttribute('max', '10');
    expect(input).toHaveAttribute('step', '1');

    await fireEvent.input(input, { target: { value: '15' } });
    expect(input.value).toBe('15');
    expect(onchange).not.toHaveBeenCalled();

    await fireEvent.change(input);
    expect(input.value).toBe('10');
    expect(onchange).toHaveBeenCalledOnce();
    expect(onchange).toHaveBeenCalledWith(10);
  });

  describe('typing and committing', () => {
    it('keeps each keystroke of 15 in a field with min 10, and value follows what is typed', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 10, allowNever: true, value: 20, onchange },
      });
      const input = container.querySelector('input[type="number"]') as HTMLInputElement;
      const neverCheck = container.querySelector(
        '.number-field__never input[type="checkbox"]',
      ) as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '1' } });
      expect(input.value).toBe('1');

      await fireEvent.input(input, { target: { value: '15' } });
      expect(input.value).toBe('15');
      expect(onchange).not.toHaveBeenCalled();

      // The never toggle restores the last number value held, which is the typed 15
      await fireEvent.click(neverCheck);
      await fireEvent.click(neverCheck);
      expect(onchange).toHaveBeenLastCalledWith(15);
    });

    it('clamps an out-of-range value on blur and reports the clamped number once', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 1, max: 10, value: 5, onchange },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '42' } });
      await fireEvent.change(input);

      expect(input.value).toBe('10');
      expect(onchange).toHaveBeenCalledOnce();
      expect(onchange).toHaveBeenCalledWith(10);
    });

    it('clamps on Enter, forwards the keydown and does not report the same commit again on the change that follows', async () => {
      const onchange = vi.fn();
      const onkeydown = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 5, max: 10, value: 7, onchange, onkeydown },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '3' } });
      await fireEvent.keyDown(input, { key: 'Enter' });

      expect(input.value).toBe('5');
      expect(onchange).toHaveBeenCalledOnce();
      expect(onchange).toHaveBeenCalledWith(5);
      expect(onkeydown).toHaveBeenCalledOnce();

      await fireEvent.change(input);
      expect(onchange).toHaveBeenCalledOnce();
    });

    it('commits on Enter without a keydown handler of the caller', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 5, max: 10, value: 7, onchange },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '12' } });
      await fireEvent.keyDown(input, { key: 'Enter' });

      expect(input.value).toBe('10');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(10);
    });

    it('does not commit on a key other than Enter and forwards it to the caller', async () => {
      const onchange = vi.fn();
      const onkeydown = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 5, value: 7, onchange, onkeydown },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '2' } });
      await fireEvent.keyDown(input, { key: 'Tab' });

      expect(input.value).toBe('2');
      expect(onchange).not.toHaveBeenCalled();
      expect(onkeydown).toHaveBeenCalledOnce();
    });

    it('clamps a step change below the minimum', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 1, max: 10, step: 1, value: 1, onchange },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '0' } });
      await fireEvent.change(input);

      expect(input.value).toBe('1');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(1);
    });

    it('commits an in-range value unchanged', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 1, max: 10, value: 5, onchange },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '7' } });
      await fireEvent.change(input);

      expect(input.value).toBe('7');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(7);
    });

    it('commits any number when no bounds are set', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Offset', value: 0, onchange },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '-300' } });
      await fireEvent.change(input);

      expect(input.value).toBe('-300');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(-300);
    });

    it('commits an emptied field as null', async () => {
      const onchange = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 1, value: 5, onchange },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '' } });
      await fireEvent.change(input);

      expect(input.value).toBe('');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(null);
    });
  });

  it('keeps an emptied allowNever field editable, with never unchecked and value null', async () => {
    const onchange = vi.fn();
    const { container } = render(NumberField, {
      props: { label: 'Timeout', allowNever: true, value: 60, onchange },
    });
    const neverCheck = container.querySelector(
      '.number-field__never input[type="checkbox"]',
    ) as HTMLInputElement;
    const numInput = container.querySelector('input[type="number"]') as HTMLInputElement;

    await fireEvent.input(numInput, { target: { value: '' } });

    expect(numInput).not.toBeDisabled();
    expect(neverCheck.checked).toBe(false);

    await fireEvent.change(numInput);
    expect(onchange).toHaveBeenCalledExactlyOnceWith(null);
    expect(numInput).not.toBeDisabled();
    expect(neverCheck.checked).toBe(false);

    // Never on and off still restores the last number
    await fireEvent.click(neverCheck);
    expect(onchange).toHaveBeenLastCalledWith(null);
    await fireEvent.click(neverCheck);
    expect(onchange).toHaveBeenLastCalledWith(60);
    expect(numInput.value).toBe('60');
  });

  it('restores the last committed number when never is toggled on and off', async () => {
    const onchange = vi.fn();
    const { container } = render(NumberField, {
      props: { label: 'Timeout', allowNever: true, value: 60, onchange },
    });
    const neverCheck = container.querySelector(
      '.number-field__never input[type="checkbox"]',
    ) as HTMLInputElement;
    const numInput = container.querySelector('input[type="number"]') as HTMLInputElement;

    await fireEvent.input(numInput, { target: { value: '45' } });
    await fireEvent.change(numInput);
    await fireEvent.click(neverCheck);
    await fireEvent.click(neverCheck);

    expect(onchange).toHaveBeenLastCalledWith(45);
    expect(numInput.value).toBe('45');
    expect(numInput).not.toBeDisabled();
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

  it('gives the never label the --control-height minimum on a phone', () => {
    const style = styleOf(path.resolve(__dirname, './NumberField.svelte'));
    const phoneAt = style.indexOf('@media (max-width: 767px)');
    expect(phoneAt).toBeGreaterThan(-1);

    const phoneRules = rulesOf(style.slice(phoneAt), '.number-field__never');
    expect(phoneRules.some((rule) => /min-height:\s*var\(--control-height\)/.test(rule))).toBe(
      true,
    );
  });

  it('uses --control-height and does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './NumberField.svelte'));
    expect(style).toContain('var(--control-height)');
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
