import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import CheckField from './CheckField.svelte';
import { styleOf } from '../../test/templateStyle';
import { REMOVED_OUTLINE, sourceOf, uniqueIdShape } from '../../test/fieldContract';

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

  it('toggles once and reports once on Space', async () => {
    const onchange = vi.fn();
    const { container } = render(CheckField, {
      props: { label: 'Agree to terms', checked: false, onchange },
    });
    const checkbox = container.querySelector('input[type="checkbox"]') as HTMLInputElement;

    await fireEvent.keyDown(checkbox, { key: ' ' });

    expect(checkbox.checked).toBe(true);
    expect(onchange).toHaveBeenCalledExactlyOnceWith(true);
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

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './CheckField.svelte');

    it('does not remove the outline the browser draws around the focused box', () => {
      expect(styleOf(file)).not.toMatch(REMOVED_OUTLINE);
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(CheckField, { props: { label } }).container.querySelector('input[type="checkbox"]')
          ?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('chk'));
      expect(second).toMatch(uniqueIdShape('chk'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});
