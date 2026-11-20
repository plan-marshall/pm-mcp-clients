import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import path from 'node:path';
import fs from 'node:fs';
import Toggle from './Toggle.svelte';
import ToggleOnClickFixture from '../../test/fixtures/ToggleOnClickFixture.svelte';

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
        icon: 'act.favourite',
        onToggle,
      },
    });

    const button = screen.getByRole('button', { name: 'Favourite' });
    expect(button).toBeInTheDocument();
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(button).toHaveAttribute('title', 'Favourite');

    const icon = button.querySelector('.ui-icon');
    expect(icon).toHaveAttribute('data-icon', 'act.favourite');

    await fireEvent.click(button);
    expect(onToggle).toHaveBeenCalledWith(true);
  });

  it('renders pressed state with aria-pressed true and fires onToggle false', async () => {
    const onToggle = vi.fn();
    render(Toggle, {
      props: {
        label: 'Follow mode',
        pressed: true,
        icon: 'act.favourite-on',
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
        icon: 'act.favourite',
        pressedIcon: 'act.favourite-on',
      },
    });

    let iconEl = screen.getByTestId('toggle-icon');
    expect(iconEl).toHaveAttribute('data-icon', 'act.favourite');
    let uiIcon = iconEl.querySelector('.ui-icon');
    expect(uiIcon).toHaveAttribute('data-icon', 'act.favourite');

    rerender({
      label: 'Favourite',
      pressed: true,
      icon: 'act.favourite',
      pressedIcon: 'act.favourite-on',
    });

    iconEl = screen.getByTestId('toggle-icon');
    expect(iconEl).toHaveAttribute('data-icon', 'act.favourite-on');
    uiIcon = iconEl.querySelector('.ui-icon');
    expect(uiIcon).toHaveAttribute('data-icon', 'act.favourite-on');
  });

  it('renders a persistent non-colour indicator when pressed without pressedIcon', () => {
    const { container, rerender } = render(Toggle, {
      props: {
        label: 'Favourite',
        pressed: false,
        icon: 'act.favourite',
      },
    });

    expect(container.querySelector('.toggle__indicator')).toBeNull();

    rerender({
      label: 'Favourite',
      pressed: true,
      icon: 'act.favourite',
    });

    expect(container.querySelector('.toggle__indicator')).toBeInTheDocument();
  });

  it('disabled toggle does not fire onToggle', async () => {
    const onToggle = vi.fn();
    render(Toggle, {
      props: {
        label: 'Favourite',
        pressed: false,
        icon: 'act.favourite',
        disabled: true,
        onToggle,
      },
    });

    const button = screen.getByRole('button', { name: 'Favourite' });
    expect(button).toBeDisabled();

    await fireEvent.click(button);
    expect(onToggle).not.toHaveBeenCalled();
  });

  it('a Toggle with disabled and disabledReason shows the reason as its advisory and does not call onToggle through on:click or onclick', async () => {
    const onToggle = vi.fn();
    const onClick = vi.fn();
    render(ToggleOnClickFixture, {
      props: {
        label: 'Reveal secret',
        icon: 'act.view',
        disabled: true,
        disabledReason: 'You do not have permission',
        onToggle,
        onClick,
      },
    });

    const button = screen.getByRole('button', { name: 'Reveal secret' });
    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('aria-disabled', 'true');
    expect(button.getAttribute('title')).toBe('You do not have permission');
    const describedBy = button.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toBe('You do not have permission');

    await fireEvent.click(button);
    expect(onToggle).not.toHaveBeenCalled();
    expect(onClick).not.toHaveBeenCalled();
  });

  it('a Toggle with disabled and disabledReason does not call onToggle or onclick on direct Toggle', async () => {
    const onToggle = vi.fn();
    const onClick = vi.fn();
    render(Toggle, {
      props: {
        label: 'Reveal secret',
        icon: 'act.view',
        disabled: true,
        disabledReason: 'You do not have permission',
        onToggle,
        onclick: onClick,
      },
    });

    const button = screen.getByRole('button', { name: 'Reveal secret' });
    await fireEvent.click(button);
    expect(onToggle).not.toHaveBeenCalled();
    expect(onClick).not.toHaveBeenCalled();
  });

  it('preserves caller-supplied aria-describedby alongside disabledReason', () => {
    render(Toggle, {
      props: {
        label: 'Reveal secret',
        icon: 'act.view',
        disabled: true,
        disabledReason: 'You do not have permission',
        'aria-describedby': 'custom-help',
      },
    });

    const button = screen.getByRole('button', { name: 'Reveal secret' });
    const describedBy = button.getAttribute('aria-describedby') ?? '';
    expect(describedBy).toContain('custom-help');
    const ids = describedBy.split(' ');
    expect(ids).toHaveLength(2);
    const reasonId = ids.find((id) => id !== 'custom-help')!;
    expect(document.getElementById(reasonId)?.textContent).toBe('You do not have permission');
  });

  it('conforms to design tokens: pressed ground brand-subtle, focus ring, 24px and phone 44px', () => {
    const style = styleOf(path.resolve(__dirname, './Toggle.svelte'));
    const pressedRules = rulesOf(style, ".toggle[aria-pressed='true']").join(';');
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
