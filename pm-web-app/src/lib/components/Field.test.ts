import path from 'node:path';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import Field from './Field.svelte';
import { styleOf } from '../../test/templateStyle';

describe('Field', () => {
  it('renders label above control', () => {
    const { container } = render(Field, {
      props: { label: 'Path of checkout', id: 'field-1' },
    });
    const label = container.querySelector('label');
    expect(label).toBeInTheDocument();
    expect(label?.textContent).toContain('Path of checkout');
    expect(label).toHaveAttribute('for', 'field-1');
  });

  it('renders (optional) mark after optional label', () => {
    const { container } = render(Field, {
      props: { label: 'Note', optional: true, id: 'field-2' },
    });
    const label = container.querySelector('label');
    expect(label?.textContent).toContain('Note');
    expect(label?.textContent).toContain('(optional)');
  });

  it('renders hint under control', () => {
    const { container } = render(Field, {
      props: { label: 'Note', hint: 'One sentence hint.', id: 'field-3' },
    });
    const hint = container.querySelector('.field__hint');
    expect(hint).toBeInTheDocument();
    expect(hint?.textContent).toContain('One sentence hint.');
  });

  it('renders error in place of hint while showing', () => {
    const { container } = render(Field, {
      props: { label: 'Note', hint: 'One sentence hint.', error: 'Give a reason.', id: 'field-4' },
    });
    const hint = container.querySelector('.field__hint');
    const err = container.querySelector('.field-error');
    expect(hint).toBeNull();
    expect(err).toBeInTheDocument();
    expect(err?.textContent).toContain('Give a reason.');
  });

  it('sets disabled state with reason', () => {
    const { container } = render(Field, {
      props: { label: 'Note', disabled: true, disabledReason: 'Not available', id: 'field-5' },
    });
    const reason = container.querySelector('.field__disabled-reason');
    expect(reason).toBeInTheDocument();
    expect(reason?.textContent).toContain('Not available');
    expect(container.querySelector('.field')).toHaveClass('field--disabled');
  });

  it('never replaces focus ring with outline: none without box-shadow focus-ring', () => {
    const style = styleOf(path.resolve(__dirname, './Field.svelte'));
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
