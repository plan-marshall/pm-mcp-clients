import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import TextField from './TextField.svelte';
import { styleOf } from '../../test/templateStyle';

describe('TextField', () => {
  it('renders input with label and links aria-describedby to hint', () => {
    const { container } = render(TextField, {
      props: { label: 'Path of checkout', hint: 'Absolute directory path.', id: 'path-input' },
    });
    const input = container.querySelector('input');
    const label = container.querySelector('label');
    expect(input).toBeInTheDocument();
    expect(label?.textContent).toContain('Path of checkout');
    expect(input?.getAttribute('id')).toBe('path-input');

    const describedBy = input?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Absolute directory path.');
    expect(input).not.toHaveAttribute('aria-invalid');
  });

  it('renders optional mark', () => {
    const { container } = render(TextField, {
      props: { label: 'Description', optional: true },
    });
    expect(container.querySelector('label')?.textContent).toContain('(optional)');
  });

  it('sets aria-invalid and aria-describedby to error when invalid', () => {
    const { container } = render(TextField, {
      props: { label: 'Token', error: 'Give a reason.', hint: 'A hint' },
    });
    const input = container.querySelector('input');
    expect(input).toHaveAttribute('aria-invalid', 'true');

    const describedBy = input?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Give a reason.');
  });

  it('sets disabled and aria-describedby for disabled reason', () => {
    const { container } = render(TextField, {
      props: { label: 'Path', disabled: true, disabledReason: 'Plan is ended' },
    });
    const input = container.querySelector('input');
    expect(input).toBeDisabled();
    const describedBy = input?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Plan is ended');
  });

  it('supports monospace variant for paths, ids and references', () => {
    const { container } = render(TextField, {
      props: { label: 'Path', mono: true },
    });
    const input = container.querySelector('input');
    expect(input).toHaveClass('text-field__input--mono');
  });

  it('uses --control-height and does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './TextField.svelte'));
    expect(style).toContain('var(--control-height)');
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
