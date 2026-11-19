import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import Icon from './Icon.svelte';

/**
 * Icon (components.adoc § #supporting).
 *
 * Tests that Icon renders delivered icons through alpha masks.
 */

describe('Icon', () => {
  it('renders icon with mask-image for delivered icon', () => {
    const { container } = render(Icon, { props: { name: 'act.copy', size: 20 } });
    const span = container.querySelector('.ui-icon');
    expect(span).toBeInTheDocument();
    expect(span).toHaveAttribute('data-icon', 'act.copy');
  });

  it('renders nothing when icon name is unknown', () => {
    const { container } = render(Icon, { props: { name: 'unknown.icon.name' } });
    expect(container.querySelector('.ui-icon')).toBeNull();
  });

  it('renders accessible label when provided', () => {
    const { container } = render(Icon, { props: { name: 'act.copy', label: 'Copy action' } });
    const span = container.querySelector('.ui-icon');
    expect(span).toHaveAttribute('aria-label', 'Copy action');
    expect(span).toHaveAttribute('role', 'img');
  });
});
