import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import EscapingKeyForm from './EscapingKeyForm.svelte';

/**
 * The dev gallery's form whose field key is no CSS identifier (`2fa`): a refused submit must show
 * the reason and move the focus to the field's own control. Before PLAN-178 D3 the selector built
 * from the key threw inside `submit()`, so neither happened.
 */
describe('EscapingKeyForm', () => {
  it('focuses its own control and shows the reason when the key needs no selector', async () => {
    render(EscapingKeyForm);
    const control = screen.getByTestId('2fa');

    await fireEvent.click(screen.getByTestId('escaping-key-submit'));

    expect(document.activeElement).toBe(control);
    expect(screen.getByText('Fill in this field.')).toBeInTheDocument();
  });

  it('accepts a value typed into it', async () => {
    render(EscapingKeyForm);
    const control = screen.getByTestId('2fa') as HTMLInputElement;

    await fireEvent.input(control, { target: { value: 'abc123' } });

    expect(control.value).toBe('abc123');
  });
});
