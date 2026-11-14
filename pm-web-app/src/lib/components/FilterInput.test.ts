import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import FilterInput from './FilterInput.svelte';

describe('FilterInput', () => {
  it('reports what the operator types, under its label, with the count of matches', async () => {
    const onchange = vi.fn();
    render(FilterInput, { label: 'Filter plans', resultCount: 1, onchange });

    await fireEvent.input(screen.getByLabelText('Filter plans'), { target: { value: 'relay' } });

    expect(onchange).toHaveBeenCalledWith('relay');
    expect(screen.getByTestId('filter-results')).toHaveTextContent('1 match');
  });

  it('names the filter and shows no count where the caller gives none', () => {
    render(FilterInput, { onchange: vi.fn() });

    expect(screen.getByLabelText('Filter')).toBeInTheDocument();
    expect(screen.queryByTestId('filter-results')).toBeNull();
    expect(screen.queryByTestId('filter-clear')).toBeNull();
  });

  it('clears a set filter with its button and with Esc, and keeps the focus in the field', async () => {
    const onchange = vi.fn();
    render(FilterInput, { value: 'relay', onchange });
    const input = screen.getByTestId('filter-input');

    await fireEvent.click(screen.getByRole('button', { name: 'Clear filter' }));
    expect(onchange).toHaveBeenLastCalledWith('');
    expect(input).toHaveFocus();

    onchange.mockClear();
    await fireEvent.keyDown(input, { key: 'a' });
    expect(onchange).not.toHaveBeenCalled();
    await fireEvent.keyDown(input, { key: 'Escape' });
    expect(onchange).toHaveBeenCalledWith('');
  });

  it('takes the focus on / pressed outside a field', async () => {
    render(FilterInput, { onchange: vi.fn() });

    const pressed = await fireEvent.keyDown(document.body, { key: '/' });

    expect(screen.getByTestId('filter-input')).toHaveFocus();
    expect(pressed).toBe(false);
  });

  it.each([
    ['with a modifier', { key: '/', ctrlKey: true }],
    ['another key', { key: 'f' }],
  ])('leaves the focus where it is for / %s', async (_case, init) => {
    render(FilterInput, { onchange: vi.fn() });

    await fireEvent.keyDown(document.body, init);

    expect(screen.getByTestId('filter-input')).not.toHaveFocus();
  });

  it.each(['input', 'textarea', 'select'])(
    'lets / reach a %s the operator types in',
    async (tag) => {
      render(FilterInput, { onchange: vi.fn() });
      const other = document.createElement(tag);
      document.body.appendChild(other);
      other.focus();

      const pressed = await fireEvent.keyDown(other, { key: '/' });

      expect(pressed).toBe(true);
      expect(screen.getByTestId('filter-input')).not.toHaveFocus();
      other.remove();
    },
  );

  it('leaves a / that another handler already took', async () => {
    render(FilterInput, { onchange: vi.fn() });
    const taken = new KeyboardEvent('keydown', { key: '/', bubbles: true, cancelable: true });
    taken.preventDefault();

    window.dispatchEvent(taken);

    expect(screen.getByTestId('filter-input')).not.toHaveFocus();
  });
});
