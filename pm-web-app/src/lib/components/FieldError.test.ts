import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import FieldError from './FieldError.svelte';

describe('FieldError', () => {
  it('renders one message under one field', () => {
    render(FieldError, { props: { message: 'Give a reason.', id: 'err-1' } });
    const err = screen.getByRole('alert');
    expect(err).toBeInTheDocument();
    expect(err.textContent).toContain('Give a reason.');
    expect(err).toHaveAttribute('id', 'err-1');
  });

  it('renders nothing when message is empty or null', () => {
    const { container } = render(FieldError, { props: { message: '' } });
    expect(container.querySelector('.field-error')).toBeNull();
  });
});
