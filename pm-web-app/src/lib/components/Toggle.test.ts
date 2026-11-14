import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import path from 'node:path';
import fs from 'node:fs';
import Toggle from './Toggle.svelte';

function styleOf(filePath: string): string {
  const content = fs.readFileSync(filePath, 'utf-8');
  const styleMatch = content.match(/<style[^>]*>([\s\S]*?)<\/style>/);
  return styleMatch ? styleMatch[1] : '';
}

function rulesOf(css: string, selector: string): string[] {
  const rules: string[] = [];
  const pattern = new RegExp(
    `(?:^|[;{}\\s])${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([^}]+)\\}`,
    'g',
  );
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(css)) !== null) {
    rules.push(match[1].trim());
  }
  return rules;
}

describe('Toggle component', () => {
  it('renders a button with aria-pressed, label naming the thing, and fires onToggle', async () => {
    const onToggle = vi.fn();
    render(Toggle, {
      props: {
        label: 'Favourite',
        pressed: false,
        icon: 'star',
        onToggle,
      },
    });

    const button = screen.getByRole('button', { name: 'Favourite' });
    expect(button).toBeInTheDocument();
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(button).toHaveAttribute('title', 'Favourite');

    await fireEvent.click(button);
    expect(onToggle).toHaveBeenCalledWith(true);
  });

  it('renders pressed state with aria-pressed true and fires onToggle false', async () => {
    const onToggle = vi.fn();
    render(Toggle, {
      props: {
        label: 'Follow mode',
        pressed: true,
        icon: 'follow',
        onToggle,
      },
    });

    const button = screen.getByRole('button', { name: 'Follow mode' });
    expect(button).toHaveAttribute('aria-pressed', 'true');

    await fireEvent.click(button);
    expect(onToggle).toHaveBeenCalledWith(false);
  });

  it('changes icon when pressed if pressedIcon is provided or indicates pressed glyph', () => {
    const { rerender } = render(Toggle, {
      props: {
        label: 'Favourite',
        pressed: false,
        icon: 'star',
        pressedIcon: 'star.filled',
      },
    });

    let iconEl = screen.getByTestId('toggle-icon');
    expect(iconEl).toHaveAttribute('data-icon', 'star');

    rerender({
      label: 'Favourite',
      pressed: true,
      icon: 'star',
      pressedIcon: 'star.filled',
    });

    iconEl = screen.getByTestId('toggle-icon');
    expect(iconEl).toHaveAttribute('data-icon', 'star.filled');
  });

  it('disabled toggle does not fire onToggle', async () => {
    const onToggle = vi.fn();
    render(Toggle, {
      props: {
        label: 'Favourite',
        pressed: false,
        icon: 'star',
        disabled: true,
        onToggle,
      },
    });

    const button = screen.getByRole('button', { name: 'Favourite' });
    expect(button).toBeDisabled();

    await fireEvent.click(button);
    expect(onToggle).not.toHaveBeenCalled();
  });

  it('conforms to design tokens: pressed ground brand-subtle, focus ring, 24px and phone 44px', () => {
    const style = styleOf(path.resolve(__dirname, './Toggle.svelte'));
    const pressedRules = rulesOf(style, '.toggle[aria-pressed="true"]').join(';');
    expect(pressedRules).toContain('var(--color-brand-subtle)');

    const toggleRules = rulesOf(style, '.toggle').join(';');
    expect(toggleRules).toContain('min-block-size: 24px');
    expect(toggleRules).toContain('min-inline-size: 24px');

    const focusRules = rulesOf(style, '.toggle:focus').join(';');
    expect(focusRules).toContain('box-shadow: var(--focus-ring)');

    const mediaMatch = style.match(/@media\s*\(max-width:\s*767px\)\s*\{([\s\S]*?\})\s*\}/);
    expect(mediaMatch).not.toBeNull();
    expect(mediaMatch![1]).toContain('min-block-size: 44px');
  });
});
