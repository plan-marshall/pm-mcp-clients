import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import CodeField from './CodeField.svelte';
import { styleOf } from '../../test/templateStyle';

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

  it('uses --control-height and does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './CodeField.svelte'));
    expect(style).toContain('var(--control-height)');
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
