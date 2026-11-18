import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import Spinner from './Spinner.svelte';

/**
 * Spinner (components.adoc § #supporting).
 *
 * Tests the single-line wait spinner.
 */

describe('Spinner', () => {
  it('renders with role="status" and default label', () => {
    render(Spinner);
    const spinner = screen.getByRole('status');
    expect(spinner).toBeInTheDocument();
    expect(spinner).toHaveAttribute('aria-label', 'Loading…');
  });

  it('supports custom size and label', () => {
    render(Spinner, { props: { size: '1rem', label: 'Please wait…' } });
    const spinner = screen.getByRole('status');
    expect(spinner).toHaveAttribute('data-size', '1rem');
    expect(spinner).toHaveAttribute('aria-label', 'Please wait…');
  });
});
