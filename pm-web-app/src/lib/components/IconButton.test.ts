import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import IconButton from './IconButton.svelte';

/**
 * `IconButton`: the square ghost control of the design system (components.adoc#icon-button).
 * One icon and no visible label, at every width. The accessible name is always `label`.
 */
describe('IconButton component', () => {
  it('always names itself for assistive technology', () => {
    render(IconButton, { props: { icon: 'act.more', label: 'More' } });

    expect(screen.getByRole('button')).toHaveAccessibleName('More');
  });

  it('never shows a visible label: square and ghost at all widths', () => {
    const { container } = render(IconButton, { props: { icon: 'act.more', label: 'More' } });

    expect(container.querySelector('.icon-btn__label')).toBeNull();
  });

  it('says why it is disabled, as the advisory and as its description, staying in tab order', async () => {
    const onclick = vi.fn();
    render(IconButton, {
      props: {
        icon: 'act.copy',
        label: 'Copy',
        disabled: true,
        disabledReason: 'Nothing to copy',
        onclick,
      },
    });

    const button = screen.getByRole('button', { name: 'Copy' });
    const describedBy = button.getAttribute('aria-describedby');

    // Stays in tab order (no native disabled attribute, aria-disabled true)
    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('aria-disabled', 'true');
    expect(button.getAttribute('title')).toBe('Nothing to copy');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toBe('Nothing to copy');

    await fireEvent.click(button);
    expect(onclick).not.toHaveBeenCalled();
  });

  it('keeps native disabled when no reason is given', () => {
    render(IconButton, {
      props: { icon: 'act.copy', label: 'Copy', disabled: true },
    });

    const button = screen.getByRole('button', { name: 'Copy' });
    expect(button).toBeDisabled();
    expect(button).not.toHaveAttribute('aria-disabled');
  });

  it('forwards the click of an enabled control', async () => {
    const clicked = vi.fn();
    render(IconButton, { props: { icon: 'act.more', label: 'More acts', onclick: clicked } });

    await fireEvent.click(screen.getByRole('button'));

    expect(clicked).toHaveBeenCalledOnce();
  });
});
