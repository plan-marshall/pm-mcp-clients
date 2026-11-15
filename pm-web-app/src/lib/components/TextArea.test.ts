import path from 'node:path';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import TextArea from './TextArea.svelte';
import { styleOf } from '../../test/templateStyle';

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

  it('does not use outline: none without focus ring', () => {
    const style = styleOf(path.resolve(__dirname, './TextArea.svelte'));
    expect(style).not.toMatch(/outline:\s*none(?!\s*;\s*box-shadow:\s*var\(--focus-ring\))/);
  });
});
