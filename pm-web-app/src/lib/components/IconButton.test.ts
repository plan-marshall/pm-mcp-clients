import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import IconButton from './IconButton.svelte';
import IconButtonOnClickFixture from '../../test/fixtures/IconButtonOnClickFixture.svelte';

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

  // A regression guard, not a proof: Svelte 5's handler wrapper already skips a forwarded `on:click`
  // once the act's own handler stopped propagation, so this test passes on the code before PLAN-172
  // too. The raw-`addEventListener` test below is the one that pins `stopImmediatePropagation`.
  it('regression guard: a click on a disabled-with-reason IconButton through on:click calls nothing', async () => {
    const onClick = vi.fn();
    render(IconButtonOnClickFixture, {
      props: { disabled: true, disabledReason: 'Nothing to copy', label: 'Copy', onClick },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Copy' }));

    expect(onClick).not.toHaveBeenCalled();
  });

  it('forwards the click of an enabled control through on:click', async () => {
    const onClick = vi.fn();
    render(IconButtonOnClickFixture, {
      props: { disabled: false, label: 'Copy', onClick },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Copy' }));

    expect(onClick).toHaveBeenCalledOnce();
  });

  it('a click on a disabled-with-reason IconButton stops immediate propagation on the button element', async () => {
    const directListener = vi.fn();
    render(IconButton, {
      props: { icon: 'act.copy', label: 'Copy', disabled: true, disabledReason: 'Nothing to copy' },
    });
    const button = screen.getByRole('button', { name: 'Copy' });
    button.addEventListener('click', directListener);

    await fireEvent.click(button);
    expect(directListener).not.toHaveBeenCalled();
  });
});
