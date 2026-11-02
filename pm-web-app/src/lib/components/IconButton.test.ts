import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import IconButton from './IconButton.svelte';

/**
 * `IconButton`: the square ghost control of the design system. Its glyph alone does not name it, so
 * it always carries `aria-label`, and from the tablet width up it shows the word beside the glyph
 * as well (CC-P-07).
 */
describe('IconButton component', () => {
  it('always names itself for assistive technology', () => {
    render(IconButton, { props: { icon: 'act.more', label: 'More' } });

    expect(screen.getByRole('button')).toHaveAccessibleName('More');
  });

  it('carries the visible label beside the glyph, so the control is not icon-only', () => {
    const { container } = render(IconButton, { props: { icon: 'act.more', label: 'More' } });

    expect(container.querySelector('.icon-btn__label')?.textContent).toBe('More');
  });

  it('says why it is disabled, as the advisory and as its description', () => {
    render(IconButton, {
      props: { icon: 'act.more', label: 'More', disabled: true, disabledReason: 'Nothing to copy' },
    });

    const button = screen.getByRole('button');
    const describedBy = button.getAttribute('aria-describedby');

    expect(button).toBeDisabled();
    expect(button.getAttribute('title')).toBe('Nothing to copy');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toBe('Nothing to copy');
  });
});
