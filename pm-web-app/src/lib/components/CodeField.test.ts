import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import CodeField from './CodeField.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';
import {
  REMOVED_OUTLINE,
  TRANSPARENT_OUTLINE,
  sourceOf,
  uniqueIdShape,
} from '../../test/fieldContract';

describe('CodeField', () => {
  it('renders monospace and large input with autocomplete="one-time-code"', () => {
    const { container } = render(CodeField, {
      props: { label: 'Pairing code', id: 'code-input' },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('autocomplete', 'one-time-code');
    expect(input).toHaveAttribute('spellcheck', 'false');
    expect(input).toHaveAttribute('autocapitalize', 'characters');
    expect(input).toHaveClass('code-field__input');
  });

  it.each([
    ['lowercase with dash', 'kxqb-mrtn'],
    ['lowercase without dash', 'kxqbmrtn'],
    ['uppercase with dash', 'KXQB-MRTN'],
    ['uppercase without dash', 'KXQBMRTN'],
  ])('accepts code in %s and reports complete at 8 characters', async (_label, typed) => {
    const oncomplete = vi.fn();
    const { container } = render(CodeField, {
      props: { label: 'Code', oncomplete },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    // Partial input (4 chars) -> not complete
    await fireEvent.input(input, { target: { value: typed.slice(0, 4) } });
    expect(oncomplete).not.toHaveBeenCalled();

    // Full 8-character code
    await fireEvent.input(input, { target: { value: typed } });
    expect(oncomplete).toHaveBeenCalledWith('KXQB-MRTN');
  });

  it('reports the same complete code once, whether typed with or without the dash', async () => {
    const oncomplete = vi.fn();
    const { container } = render(CodeField, { props: { label: 'Code', oncomplete } });
    const input = container.querySelector('input') as HTMLInputElement;

    await fireEvent.input(input, { target: { value: 'KXQBMRTN' } });
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });

    expect(oncomplete).toHaveBeenCalledExactlyOnceWith('KXQB-MRTN');
  });

  it('reports again after the code was shortened and completed again', async () => {
    const oncomplete = vi.fn();
    const { container } = render(CodeField, { props: { label: 'Code', oncomplete } });
    const input = container.querySelector('input') as HTMLInputElement;

    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    await fireEvent.input(input, { target: { value: 'KXQB-MRT' } });
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });

    expect(oncomplete).toHaveBeenCalledTimes(2);
    expect(oncomplete).toHaveBeenNthCalledWith(2, 'KXQB-MRTN');
  });

  it('reports a different complete code that replaces a reported one', async () => {
    const oncomplete = vi.fn();
    const { container } = render(CodeField, { props: { label: 'Code', oncomplete } });
    const input = container.querySelector('input') as HTMLInputElement;

    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    await fireEvent.input(input, { target: { value: 'KXQB-MRTA' } });

    expect(oncomplete).toHaveBeenCalledTimes(2);
    expect(oncomplete).toHaveBeenLastCalledWith('KXQB-MRTA');
  });

  it('reports the same code again when it is pasted after the parent cleared the field', async () => {
    const oncomplete = vi.fn();
    const { container, rerender } = render(CodeField, { props: { label: 'Code', oncomplete } });
    const input = container.querySelector('input') as HTMLInputElement;

    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    await rerender({ value: '' });
    expect(input.value).toBe('');
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });

    expect(oncomplete).toHaveBeenCalledTimes(2);
    expect(oncomplete).toHaveBeenNthCalledWith(2, 'KXQB-MRTN');
  });

  it('reports a complete code once when the parent echoes it back', async () => {
    const oncomplete = vi.fn();
    const { container, rerender } = render(CodeField, { props: { label: 'Code', oncomplete } });
    const input = container.querySelector('input') as HTMLInputElement;

    await fireEvent.input(input, { target: { value: 'KXQBMRTN' } });
    await rerender({ value: 'KXQB-MRTN' });
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });

    expect(oncomplete).toHaveBeenCalledExactlyOnceWith('KXQB-MRTN');
  });

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './CodeField.svelte');

    it('uses --control-height and keeps a transparent focus outline beside the focus ring', () => {
      const style = styleOf(file);
      expect(style).toContain('var(--control-height)');
      expect(style).not.toMatch(REMOVED_OUTLINE);
      const focus = ruleOf(style, '.code-field__input:focus');
      expect(focus).toMatch(TRANSPARENT_OUTLINE);
      expect(focus).toContain('box-shadow: var(--focus-ring)');
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(CodeField, { props: { label } }).container.querySelector('input')?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('cf'));
      expect(second).toMatch(uniqueIdShape('cf'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});
