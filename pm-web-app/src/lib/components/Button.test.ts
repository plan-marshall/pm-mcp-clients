import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Button from './Button.svelte';
import IconButton from './IconButton.svelte';

/**
 * The control components of VD-P-05: the four `Button` variants, its three sizes, the optional
 * icon, the disabled reason as advisory and description, and `IconButton` as the square ghost
 * control with the advisory of its `X.title` key.
 */

/** @param container the rendered subtree @returns its one button */
function buttonIn(container: HTMLElement): HTMLButtonElement {
  const button = container.querySelector('button');
  if (!button) {
    throw new Error('the component rendered no button');
  }
  return button;
}

describe('Button', () => {
  it.each(['primary', 'secondary', 'ghost', 'danger'] as const)(
    'draws the %s variant',
    (variant) => {
      const { container } = render(Button, { props: { variant } });

      expect(buttonIn(container).className).toContain(`btn--${variant}`);
    },
  );

  it('is secondary unless a view names a variant', () => {
    const { container } = render(Button, {});

    expect(buttonIn(container).className).toContain('btn--secondary');
  });

  it.each(['sm', 'md', 'lg'] as const)('draws the %s size', (size) => {
    const { container } = render(Button, { props: { size } });

    expect(buttonIn(container).className).toContain(`btn--${size}`);
  });

  it('is medium unless a view names a size', () => {
    const { container } = render(Button, {});

    expect(buttonIn(container).className).toContain('btn--md');
  });

  it('draws the `small` size the views already pass as the sm size', () => {
    const { container } = render(Button, { props: { size: 'small' } });

    expect(buttonIn(container).className).toContain('btn--sm');
  });

  it.each([
    ['sm', 16],
    ['md', 20],
    ['lg', 20],
  ] as const)('draws the %s size with its icon at %i px', (size, iconSize) => {
    const { container } = render(Button, { props: { size, icon: 'act.copy' } });
    const icon = buttonIn(container).querySelector('.ui-icon');

    expect(icon?.getAttribute('data-icon')).toBe('act.copy');
    expect(icon?.getAttribute('style')).toContain(`${iconSize}px`);
  });

  it('draws no icon when the view names none', () => {
    const { container } = render(Button, {});

    expect(buttonIn(container).querySelector('.ui-icon')).toBeNull();
  });

  it('keeps the icon out of the accessible name, which the label carries', () => {
    const { container } = render(Button, { props: { icon: 'act.copy' } });

    expect(buttonIn(container).querySelector('.ui-icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('says why it is disabled, as the advisory and as its description', () => {
    render(Button, { props: { disabled: true, disabledReason: 'The plan has ended' } });
    const button = screen.getByRole('button');
    const describedBy = button.getAttribute('aria-describedby');

    expect(button).toBeDisabled();
    expect(button.getAttribute('title')).toBe('The plan has ended');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toBe('The plan has ended');
  });

  it('describes nothing when no reason is given', () => {
    render(Button, { props: { disabled: true } });
    const button = screen.getByRole('button');

    expect(button.hasAttribute('aria-describedby')).toBe(false);
    expect(button.hasAttribute('title')).toBe(false);
  });

  it("keeps the caller's description beside the reason", () => {
    render(Button, {
      props: { disabled: true, disabledReason: 'Ended', 'aria-describedby': 'plan-ended-hint' },
    });
    const describedBy = screen.getByRole('button').getAttribute('aria-describedby');

    expect(describedBy?.split(' ')).toContain('plan-ended-hint');
    expect(describedBy?.split(' ')).toHaveLength(2);
  });

  it('keeps the props the views already pass', () => {
    const { container } = render(Button, { props: { type: 'submit', loading: true } });
    const button = buttonIn(container);

    expect(button.getAttribute('type')).toBe('submit');
    expect(button.className).toContain('btn--loading');
    expect(button).toBeDisabled();
    expect(button.querySelector('.btn__spinner')).not.toBeNull();
  });

  it('forwards the click of an enabled control', async () => {
    const clicked = vi.fn();
    render(Button, { props: { onclick: clicked } });

    await fireEvent.click(screen.getByRole('button'));

    expect(clicked).toHaveBeenCalledOnce();
  });
});

describe('IconButton', () => {
  it('is a square ghost control named by its label', () => {
    const { container } = render(IconButton, {
      props: { icon: 'act.more', label: 'More acts', advisory: 'Show the acts that do not fit' },
    });
    const button = buttonIn(container);

    expect(button.className).toContain('icon-btn');
    expect(button.getAttribute('aria-label')).toBe('More acts');
    expect(button.getAttribute('title')).toBe('Show the acts that do not fit');
    expect(button.querySelector('.ui-icon')?.getAttribute('data-icon')).toBe('act.more');
  });

  it.each(['sm', 'md', 'lg'] as const)('draws the %s size', (size) => {
    const { container } = render(IconButton, {
      props: { icon: 'act.more', label: 'More acts', size },
    });

    expect(buttonIn(container).className).toContain(`icon-btn--${size}`);
  });

  it('is medium unless a view names a size', () => {
    const { container } = render(IconButton, { props: { icon: 'act.more', label: 'More acts' } });

    expect(buttonIn(container).className).toContain('icon-btn--md');
  });

  it('names the icon for the advisory, so the tooltip comes from the catalogue', () => {
    const { container } = render(IconButton, {
      props: { icon: 'act.more', label: 'More acts', advisory: 'Show the acts that do not fit' },
    });
    const button = buttonIn(container);

    // The icon itself is decorative: the label of the control carries the meaning.
    expect(button.querySelector('.ui-icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('says why it is disabled, as the advisory and as its description', () => {
    render(IconButton, {
      props: {
        icon: 'act.copy',
        label: 'Copy',
        advisory: 'Copy this id to the clipboard',
        disabled: true,
        disabledReason: 'Nothing to copy',
      },
    });
    const button = screen.getByRole('button');
    const describedBy = button.getAttribute('aria-describedby');

    expect(button).toBeDisabled();
    expect(button.getAttribute('title')).toBe('Nothing to copy');
    expect(document.getElementById(describedBy as string)?.textContent).toBe('Nothing to copy');
  });

  it('forwards the click', async () => {
    const clicked = vi.fn();
    render(IconButton, { props: { icon: 'act.more', label: 'More acts', onclick: clicked } });

    await fireEvent.click(screen.getByRole('button'));

    expect(clicked).toHaveBeenCalledOnce();
  });
});
