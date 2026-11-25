import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Button, { BUTTON_VARIANTS, BUTTON_SIZES } from './Button.svelte';
import IconButton from './IconButton.svelte';
import ButtonOnClickFixture from '../../test/fixtures/ButtonOnClickFixture.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';
import { contrastRatio } from '../../test/colourMetrics';
import { parseThemes } from '../test/stylesheet';

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

  const combinations = BUTTON_VARIANTS.flatMap((variant) =>
    BUTTON_SIZES.map((size) => [variant, size] as const),
  );

  it.each(combinations)('draws variant %s in size %s', (variant, size) => {
    const { container } = render(Button, { props: { variant, size } });
    const btn = buttonIn(container);
    expect(btn.className).toContain(`btn--${variant}`);
    expect(btn.className).toContain(`btn--${size}`);
  });

  it('an act with disabledReason stays in tab order with aria-disabled, carries reason, and ignores clicks', async () => {
    const clicked = vi.fn();
    render(Button, {
      props: { disabled: true, disabledReason: 'The plan has ended', onclick: clicked },
    });
    const button = screen.getByRole('button');
    const describedBy = button.getAttribute('aria-describedby');

    // Stays in tab order: no native disabled attribute, has aria-disabled="true"
    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('aria-disabled', 'true');
    expect(button.getAttribute('title')).toBe('The plan has ended');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toBe('The plan has ended');

    await fireEvent.click(button);
    expect(clicked).not.toHaveBeenCalled();
  });

  // A regression guard, not a proof: Svelte 5's handler wrapper already skips a forwarded `on:click`
  // once the act's own handler stopped propagation, so this test passes on the code before PLAN-172
  // too. The raw-`addEventListener` test below is the one that pins `stopImmediatePropagation`.
  it('regression guard: an act with disabledReason ignores clicks forwarded through on:click', async () => {
    const onClick = vi.fn();
    render(ButtonOnClickFixture, {
      props: { disabled: true, disabledReason: 'The plan has ended', label: 'Action', onClick },
    });
    const button = screen.getByRole('button');

    await fireEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it('a click on a disabled-with-reason button stops immediate propagation on the button element', async () => {
    const directListener = vi.fn();
    render(Button, {
      props: { disabled: true, disabledReason: 'The plan has ended' },
    });
    const button = screen.getByRole('button');
    button.addEventListener('click', directListener);

    await fireEvent.click(button);
    expect(directListener).not.toHaveBeenCalled();
  });

  it('an act with no disabledReason keeps native disabled', () => {
    render(Button, { props: { disabled: true } });
    const button = screen.getByRole('button');

    expect(button).toBeDisabled();
    expect(button).not.toHaveAttribute('aria-disabled');
    expect(button.hasAttribute('aria-describedby')).toBe(false);
    expect(button.hasAttribute('title')).toBe(false);
  });

  it('a loading button keeps label text alongside spinner and ignores clicks', async () => {
    const clicked = vi.fn();
    const { container } = render(Button, {
      props: { loading: true, onclick: clicked },
    });
    const button = buttonIn(container);

    expect(button.className).toContain('btn--loading');
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(button.querySelector('.btn__spinner')).not.toBeNull();
    expect(button.querySelector('.btn__content')).not.toBeNull();

    await fireEvent.click(button);
    expect(clicked).not.toHaveBeenCalled();
  });

  it('an icon button with loading shows the spinner and no icon', () => {
    const { container } = render(Button, {
      props: { loading: true, icon: 'act.copy' },
    });
    const button = buttonIn(container);

    expect(button.querySelector('.btn__spinner')).not.toBeNull();
    expect(button.querySelector('.ui-icon')).toBeNull();
  });

  it('submit button inside form does not submit when disabled with reason or loading', async () => {
    const form = document.createElement('form');
    const onSubmit = vi.fn((e) => e.preventDefault());
    form.addEventListener('submit', onSubmit);
    document.body.appendChild(form);

    // 1. Disabled with reason
    const { container: c1, unmount: u1 } = render(Button, {
      target: form,
      props: { type: 'submit', disabled: true, disabledReason: 'Not ready' },
    });
    const b1 = buttonIn(c1);
    await fireEvent.click(b1);
    expect(onSubmit).not.toHaveBeenCalled();
    u1();

    // 2. Loading
    const { container: c2, unmount: u2 } = render(Button, {
      target: form,
      props: { type: 'submit', loading: true },
    });
    const b2 = buttonIn(c2);
    await fireEvent.click(b2);
    expect(onSubmit).not.toHaveBeenCalled();
    u2();

    document.body.removeChild(form);
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

    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('aria-disabled', 'true');
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

  it('derives the variant contrast from the design tokens and meets 4.5:1 in both themes', () => {
    const style = styleOf(path.resolve(__dirname, './Button.svelte'));

    // Every variant colour is a token, never a literal colour.
    expect(style).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(ruleOf(style, '.btn--primary')).toContain('var(--color-brand)');
    expect(ruleOf(style, '.btn--primary')).toContain('var(--color-on-brand)');
    expect(ruleOf(style, '.btn--danger')).toContain('var(--color-danger)');
    expect(ruleOf(style, '.btn--danger')).toContain('var(--color-on-brand)');
    expect(ruleOf(style, '.btn--secondary')).toContain('var(--color-bg-surface)');
    expect(ruleOf(style, '.btn--secondary')).toContain('var(--color-text-primary)');
    // The ghost variant paints no background of its own (`transparent`), so the contrast pair
    // below assumes the default surface background behind it. The rule must still name its
    // foreground token, or the table would check a colour the variant does not use.
    expect(ruleOf(style, '.btn--ghost')).toContain('var(--color-text-secondary)');
    // The disabled state pairs a muted foreground with the subtle fill; both tokens are named so
    // the pair below is the one the state actually paints (D2 d-style).
    expect(ruleOf(style, '.btn:disabled')).toContain('var(--color-bg-subtle)');
    expect(ruleOf(style, '.btn:disabled')).toContain('var(--color-text-muted)');

    const tokensCss = fs.readFileSync(path.resolve(__dirname, '../../styles/tokens.css'), 'utf-8');
    const themes = parseThemes(tokensCss);

    const pairs = [
      ['primary', '--color-on-brand', '--color-brand'],
      ['danger', '--color-on-brand', '--color-danger'],
      ['secondary', '--color-text-primary', '--color-bg-surface'],
      ['ghost', '--color-text-secondary', '--color-bg-surface'],
      ['disabled', '--color-text-muted', '--color-bg-subtle'],
    ] as const;

    for (const [variant, foreground, background] of pairs) {
      expect(
        contrastRatio(themes.light[foreground], themes.light[background]),
        `${variant} contrast in the light theme`,
      ).toBeGreaterThanOrEqual(4.5);
      expect(
        contrastRatio(themes.dark[foreground], themes.dark[background]),
        `${variant} contrast in the dark theme`,
      ).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('asserts the md button and a TextField have the same height token', () => {
    const btnStyle = styleOf(path.resolve(__dirname, './Button.svelte'));
    const textFieldStyle = styleOf(path.resolve(__dirname, './TextField.svelte'));

    expect(ruleOf(btnStyle, '.btn--md')).toContain('var(--control-height)');
    expect(ruleOf(textFieldStyle, '.text-field__input')).toContain('var(--control-height)');
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

    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('aria-disabled', 'true');
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
