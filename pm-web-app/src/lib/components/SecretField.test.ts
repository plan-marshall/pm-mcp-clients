import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import SecretField from './SecretField.svelte';
import { styleOf } from '../../test/templateStyle';

describe('SecretField', () => {
  it('masks secret by default and sets autocomplete/spellcheck/autocapitalize off', () => {
    const { container } = render(SecretField, {
      props: { label: 'API Token', id: 'token-input' },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    expect(input).toBeInTheDocument();
    expect(input.type).toBe('password');
    expect(input).toHaveAttribute('autocomplete', 'off');
    expect(input).toHaveAttribute('spellcheck', 'false');
    expect(input).toHaveAttribute('autocapitalize', 'off');
  });

  it('reveals secret only while reveal toggle is pressed', async () => {
    const { container } = render(SecretField, {
      props: { label: 'Secret key' },
    });
    const input = container.querySelector('input') as HTMLInputElement;
    const toggle = container.querySelector('.toggle') as HTMLButtonElement;

    expect(toggle).toBeInTheDocument();
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(input.type).toBe('password');

    // Press toggle
    await fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(input.type).toBe('text');

    // Press toggle again
    await fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(input.type).toBe('password');
  });

  it('trims whitespace on paste', async () => {
    const { container } = render(SecretField, {
      props: { label: 'Secret' },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    const pasteData = {
      getData: (format: string) => (format === 'text' || format === 'text/plain' ? '   my-secret-key   \n' : ''),
    };
    await fireEvent.paste(input, { clipboardData: pasteData });
    expect(input.value).toBe('my-secret-key');
  });

  it('clears secret and is absent from DOM after onstore', async () => {
    const onstore = vi.fn();
    const { container } = render(SecretField, {
      props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
    });
    const input = container.querySelector('input') as HTMLInputElement;
    const storeBtn = screen.getByRole('button', { name: 'Save Secret' });

    // Initially store button is disabled when empty
    expect(storeBtn).toBeDisabled();

    // Type secret
    await fireEvent.input(input, { target: { value: 'super-secret-token' } });
    expect(input.value).toBe('super-secret-token');
    expect(storeBtn).not.toBeDisabled();

    // Store secret
    await fireEvent.click(storeBtn);
    expect(onstore).toHaveBeenCalledWith('super-secret-token');

    // Secret is forgotten once stored: absent from DOM
    expect(input.value).toBe('');
    expect(container.textContent).not.toContain('super-secret-token');
  });

  it('uses --control-height and does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './SecretField.svelte'));
    expect(style).toContain('var(--control-height)');
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
