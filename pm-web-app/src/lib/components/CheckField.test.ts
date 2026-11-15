import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import CheckField from './CheckField.svelte';
import { styleOf } from '../../test/templateStyle';

describe('CheckField', () => {
  it('renders check control with label row as target', async () => {
    const onchange = vi.fn();
    const { container } = render(CheckField, {
      props: { label: 'Enable notifications', checked: false, onchange },
    });
    const labelRow = container.querySelector('.check-field') as HTMLElement;
    const checkbox = container.querySelector('input[type="checkbox"]') as HTMLInputElement;

    expect(checkbox).toBeInTheDocument();
    expect(checkbox.checked).toBe(false);

    // Clicking anywhere on label row toggles checkbox
    await fireEvent.click(labelRow);
    expect(checkbox.checked).toBe(true);
    expect(onchange).toHaveBeenCalledWith(true);
  });

  it('supports Space key on checkbox to toggle', async () => {
    const onchange = vi.fn();
    const { container } = render(CheckField, {
      props: { label: 'Agree to terms', checked: false, onchange },
    });
    const checkbox = container.querySelector('input[type="checkbox"]') as HTMLInputElement;

    await fireEvent.keyDown(checkbox, { key: ' ' });
    expect(onchange).toHaveBeenCalledWith(true);
  });

  it('renders disabled check with reason', async () => {
    const onchange = vi.fn();
    const { container } = render(CheckField, {
      props: {
        label: 'Auto-pilot',
        checked: false,
        disabled: true,
        disabledReason: 'Feature requires pro license',
        onchange,
      },
    });
    const checkbox = container.querySelector('input[type="checkbox"]') as HTMLInputElement;
    const reason = container.querySelector('.check-field__disabled-reason');

    expect(checkbox).toBeDisabled();
    expect(reason?.textContent).toContain('Feature requires pro license');

    await fireEvent.click(container.querySelector('.check-field') as HTMLElement);
    expect(onchange).not.toHaveBeenCalled();
  });
});
