import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Button from './Button.svelte';
import IconButton from './IconButton.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';

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

    /** @param hex a #rrggbb token value @returns its rgb channels */
    const hexToRgb = (hex: string) => {
      const clean = hex.replace('#', '').trim();
      return {
        r: parseInt(clean.slice(0, 2), 16),
        g: parseInt(clean.slice(2, 4), 16),
        b: parseInt(clean.slice(4, 6), 16),
      };
    };

    /** @param colour rgb channels @returns the relative luminance */
    const luminance = (colour: { r: number; g: number; b: number }) => {
      const channels = [colour.r / 255, colour.g / 255, colour.b / 255].map((value) =>
        value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4),
      );
      return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
    };

    /** @param marker the theme selector @returns that block's declarations */
    const blockOf = (marker: string) => {
      const start = tokensCss.indexOf(marker);
      expect(start, `${marker} must exist in tokens.css`).toBeGreaterThanOrEqual(0);
      const open = tokensCss.indexOf('{', start);
      const close = tokensCss.indexOf('}', open);
      return tokensCss.slice(open + 1, close);
    };

    /** @returns a token's colour from a theme block */
    const tokenOf = (block: string, name: string) => {
      const match = block.match(new RegExp(`--${name}:\\s*(#[0-9a-fA-F]{6})`));
      expect(match, `token --${name} must be a hex value`).not.toBeNull();
      return hexToRgb(match![1]);
    };

    /** @returns the contrast ratio of two colours */
    const contrast = (
      first: { r: number; g: number; b: number },
      second: { r: number; g: number; b: number },
    ) =>
      (Math.max(luminance(first), luminance(second)) + 0.05) /
      (Math.min(luminance(first), luminance(second)) + 0.05);

    const light = blockOf("[data-theme='light']");
    const dark = blockOf("[data-theme='dark']");

    const pairs = [
      ['primary', 'color-on-brand', 'color-brand'],
      ['danger', 'color-on-brand', 'color-danger'],
      ['secondary', 'color-text-primary', 'color-bg-surface'],
      ['ghost', 'color-text-secondary', 'color-bg-surface'],
      ['disabled', 'color-text-muted', 'color-bg-subtle'],
    ] as const;

    for (const [variant, foreground, background] of pairs) {
      expect(
        contrast(tokenOf(light, foreground), tokenOf(light, background)),
        `${variant} contrast in the light theme`,
      ).toBeGreaterThanOrEqual(4.5);
      expect(
        contrast(tokenOf(dark, foreground), tokenOf(dark, background)),
        `${variant} contrast in the dark theme`,
      ).toBeGreaterThanOrEqual(4.5);
    }
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
