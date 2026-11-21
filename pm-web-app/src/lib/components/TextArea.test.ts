import path from 'node:path';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import TextArea from './TextArea.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';
import {
  REMOVED_OUTLINE,
  TRANSPARENT_OUTLINE,
  sourceOf,
  uniqueIdShape,
} from '../../test/fieldContract';

describe('TextArea', () => {
  it('renders textarea with label and links aria-describedby to hint', () => {
    const { container } = render(TextArea, {
      props: { label: 'Reason', hint: 'Give a reason.', id: 'reason-area' },
    });
    const textarea = container.querySelector('textarea');
    const label = container.querySelector('label');
    expect(textarea).toBeInTheDocument();
    expect(label?.textContent).toContain('Reason');
    expect(textarea?.getAttribute('id')).toBe('reason-area');

    const describedBy = textarea?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Give a reason.');
  });

  it('renders optional mark when optional', () => {
    const { container } = render(TextArea, {
      props: { label: 'Note', optional: true },
    });
    expect(container.querySelector('label')?.textContent).toContain('(optional)');
  });

  it('sets aria-invalid and aria-describedby to error when invalid', () => {
    const { container } = render(TextArea, {
      props: { label: 'Reason', error: 'Give a reason.' },
    });
    const textarea = container.querySelector('textarea');
    expect(textarea).toHaveAttribute('aria-invalid', 'true');

    const describedBy = textarea?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Give a reason.');
  });

  it('sets disabled and aria-describedby for disabled reason', () => {
    const { container } = render(TextArea, {
      props: { label: 'Reason', disabled: true, disabledReason: 'Closed' },
    });
    const textarea = container.querySelector('textarea');
    expect(textarea).toBeDisabled();
    const describedBy = textarea?.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toContain('Closed');
  });

  it('supports monospace variant', () => {
    const { container } = render(TextArea, {
      props: { label: 'Code', mono: true },
    });
    const textarea = container.querySelector('textarea');
    expect(textarea).toHaveClass('text-area__input--mono');
  });

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './TextArea.svelte');

    it('keeps a transparent focus outline beside the focus ring', () => {
      const style = styleOf(file);
      expect(style).not.toMatch(REMOVED_OUTLINE);
      const focus = ruleOf(style, '.text-area__input:focus');
      expect(focus).toMatch(TRANSPARENT_OUTLINE);
      expect(focus).toContain('box-shadow: var(--focus-ring)');
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(TextArea, { props: { label } }).container.querySelector('textarea')?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('ta'));
      expect(second).toMatch(uniqueIdShape('ta'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});
