import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import NumberField from './NumberField.svelte';
import { ruleOf, rulesOf, styleOf } from '../../test/templateStyle';
import {
  REMOVED_OUTLINE,
  TRANSPARENT_OUTLINE,
  sourceOf,
  uniqueIdShape,
} from '../../test/fieldContract';

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

    it('does not commit on the Enter that confirms an IME composition and forwards it to the caller', async () => {
      const onchange = vi.fn();
      const onkeydown = vi.fn();
      const { container } = render(NumberField, {
        props: { label: 'Count', min: 5, max: 10, value: 7, onchange, onkeydown },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: '3' } });
      await fireEvent.keyDown(input, { key: 'Enter', isComposing: true });

      expect(input.value).toBe('3');
      expect(onchange).not.toHaveBeenCalled();
      expect(onkeydown).toHaveBeenCalledOnce();

      // The composition's Enter set no commit mark, so the change that follows commits
      await fireEvent.change(input);
      expect(input.value).toBe('5');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(5);
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

  describe('a value set from outside', () => {
    const partsOf = (container: HTMLElement) => ({
      input: container.querySelector('input[type="number"]') as HTMLInputElement,
      neverCheck: container.querySelector(
        '.number-field__never input[type="checkbox"]',
      ) as HTMLInputElement,
    });

    it('turns never off and shows a number the parent assigns to a field that started never', async () => {
      const { container, rerender } = render(NumberField, {
        props: { label: 'Expiry', allowNever: true, value: null },
      });
      const { input, neverCheck } = partsOf(container);
      expect(neverCheck.checked).toBe(true);

      await rerender({ value: 42 });

      expect(input.value).toBe('42');
      expect(input).not.toBeDisabled();
      expect(neverCheck.checked).toBe(false);
    });

    it('turns never on when the parent assigns null to a field holding a number', async () => {
      const { container, rerender } = render(NumberField, {
        props: { label: 'Expiry', allowNever: true, value: 30 },
      });
      const { input, neverCheck } = partsOf(container);
      expect(neverCheck.checked).toBe(false);

      await rerender({ value: null });

      expect(input.value).toBe('');
      expect(input).toBeDisabled();
      expect(neverCheck.checked).toBe(true);
    });

    it('restores the number the parent assigned last when never is toggled on and off', async () => {
      const onchange = vi.fn();
      const { container, rerender } = render(NumberField, {
        props: { label: 'Expiry', allowNever: true, value: null, onchange },
      });
      const { input, neverCheck } = partsOf(container);

      await rerender({ value: 12 });
      await fireEvent.click(neverCheck);
      await fireEvent.click(neverCheck);

      expect(onchange).toHaveBeenLastCalledWith(12);
      expect(input.value).toBe('12');
    });

    it('does not turn never on for an emptied field the parent echoes back as null', async () => {
      const { container, rerender } = render(NumberField, {
        props: { label: 'Timeout', allowNever: true, value: 60 },
      });
      const { input, neverCheck } = partsOf(container);

      await fireEvent.input(input, { target: { value: '' } });
      await rerender({ value: null });

      expect(input).not.toBeDisabled();
      expect(neverCheck.checked).toBe(false);
    });

    it('leaves never and enables the input when the parent turns allowNever off without changing the value', async () => {
      const onchange = vi.fn();
      const { container, rerender } = render(NumberField, {
        props: { label: 'Expiry', allowNever: true, value: null, onchange },
      });
      const input = container.querySelector('input[type="number"]') as HTMLInputElement;
      expect(input).toBeDisabled();

      await rerender({ allowNever: false });

      expect(container.querySelector('.number-field__never')).toBeNull();
      expect(input).not.toBeDisabled();

      await fireEvent.input(input, { target: { value: '8' } });
      await fireEvent.change(input);
      expect(input.value).toBe('8');
      expect(onchange).toHaveBeenCalledExactlyOnceWith(8);
    });

    it('shows a number the parent assigns to a field without a never choice and keeps it editable', async () => {
      const { container, rerender } = render(NumberField, {
        props: { label: 'Count', value: 5 },
      });
      const input = container.querySelector('input[type="number"]') as HTMLInputElement;

      await rerender({ value: null });
      expect(input.value).toBe('');
      expect(input).not.toBeDisabled();

      await rerender({ value: 9 });
      expect(input.value).toBe('9');
      expect(input).not.toBeDisabled();
    });
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

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './NumberField.svelte');

    it('uses --control-height and keeps a transparent focus outline beside the focus ring', () => {
      const style = styleOf(file);
      expect(style).toContain('var(--control-height)');
      expect(style).not.toMatch(REMOVED_OUTLINE);
      const focus = ruleOf(style, '.number-field__input:focus');
      expect(focus).toMatch(TRANSPARENT_OUTLINE);
      expect(focus).toContain('box-shadow: var(--focus-ring)');
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(NumberField, { props: { label } }).container.querySelector('input[type="number"]')
          ?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('nf'));
      expect(second).toMatch(uniqueIdShape('nf'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});
