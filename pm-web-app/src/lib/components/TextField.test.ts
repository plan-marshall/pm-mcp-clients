import path from 'node:path';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import TextField from './TextField.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';
import {
  REMOVED_OUTLINE,
  TRANSPARENT_OUTLINE,
  sourceOf,
  uniqueIdShape,
} from '../../test/fieldContract';

describe('TextField', () => {
  it('renders input with label and links aria-describedby to hint', () => {
    const { container } = render(TextField, {
      props: { label: 'Path of checkout', hint: 'Absolute directory path.', id: 'path-input' },
    });
    const input = container.querySelector('input');
    const label = container.querySelector('label');
    expect(input).toBeInTheDocument();
    expect(label?.textContent).toContain('Path of checkout');
    expect(input?.getAttribute('id')).toBe('path-input');

    const describedBy = input?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Absolute directory path.');
    expect(input).not.toHaveAttribute('aria-invalid');
  });

  it('renders optional mark', () => {
    const { container } = render(TextField, {
      props: { label: 'Description', optional: true },
    });
    expect(container.querySelector('label')?.textContent).toContain('(optional)');
  });

  it('sets aria-invalid and aria-describedby to error when invalid', () => {
    const { container } = render(TextField, {
      props: { label: 'Token', error: 'Give a reason.', hint: 'A hint' },
    });
    const input = container.querySelector('input');
    expect(input).toHaveAttribute('aria-invalid', 'true');

    const describedBy = input?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Give a reason.');
  });

  it('sets disabled and aria-describedby for disabled reason', () => {
    const { container } = render(TextField, {
      props: { label: 'Path', disabled: true, disabledReason: 'Plan is ended' },
    });
    const input = container.querySelector('input');
    expect(input).toBeDisabled();
    const describedBy = input?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Plan is ended');
  });

  it('supports monospace variant for paths, ids and references', () => {
    const { container } = render(TextField, {
      props: { label: 'Path', mono: true },
    });
    const input = container.querySelector('input');
    expect(input).toHaveClass('text-field__input--mono');
  });

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './TextField.svelte');

    it('uses --control-height and keeps a transparent focus outline beside the focus ring', () => {
      const style = styleOf(file);
      expect(style).toContain('var(--control-height)');
      expect(style).not.toMatch(REMOVED_OUTLINE);
      const focus = ruleOf(style, '.text-field__input:focus');
      expect(focus).toMatch(TRANSPARENT_OUTLINE);
      expect(focus).toContain('box-shadow: var(--focus-ring)');
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(TextField, { props: { label } }).container.querySelector('input')?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('tf'));
      expect(second).toMatch(uniqueIdShape('tf'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});
