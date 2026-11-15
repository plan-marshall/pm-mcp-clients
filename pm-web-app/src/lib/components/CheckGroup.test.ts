import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import CheckGroup from './CheckGroup.svelte';

describe('CheckGroup', () => {
  const options = [
    { value: 'file1', label: 'File 1' },
    { value: 'file2', label: 'File 2', marker: 'recommended' as const },
    { value: 'file3', label: 'File 3', disabled: true, disabledReason: 'Locked by another process' },
  ];

  it('renders fieldset with legend', () => {
    const { container } = render(CheckGroup, {
      props: { legend: 'Select items', options, values: [] },
    });
    const fieldset = container.querySelector('fieldset.check-group');
    const legend = container.querySelector('legend');

    expect(fieldset).toBeInTheDocument();
    expect(legend?.textContent).toContain('Select items');
  });

  it('supports none, one and all checked', async () => {
    const onchange = vi.fn();
    const activeOptions = [
      { value: 'a', label: 'A' },
      { value: 'b', label: 'B' },
    ];
    const { container } = render(CheckGroup, {
      props: { legend: 'Choices', options: activeOptions, values: [], onchange },
    });

    const rows = container.querySelectorAll('.check-option');
    const checks = container.querySelectorAll('input[type="checkbox"]');

    // None checked initially
    expect((checks[0] as HTMLInputElement).checked).toBe(false);
    expect((checks[1] as HTMLInputElement).checked).toBe(false);

    // One checked (click A)
    await fireEvent.click(rows[0]);
    expect(onchange).toHaveBeenCalledWith(['a']);

    // All checked (click B)
    await fireEvent.click(rows[1]);
    expect(onchange).toHaveBeenCalledWith(['a', 'b']);
  });

  it('renders card form where the whole card is the target', async () => {
    const onchange = vi.fn();
    const { container } = render(CheckGroup, {
      props: { legend: 'Cards', options, values: ['file1'], variant: 'card', onchange },
    });

    const group = container.querySelector('.check-group--card');
    expect(group).toBeInTheDocument();

    const cards = container.querySelectorAll('.check-card');
    expect(cards).toHaveLength(3);

    // Click card 2 to toggle it
    await fireEvent.click(cards[1]);
    expect(onchange).toHaveBeenCalledWith(['file1', 'file2']);
  });

  it('renders disabled option with reason and ignores clicks', async () => {
    const onchange = vi.fn();
    const { container } = render(CheckGroup, {
      props: { legend: 'Items', options, values: [], onchange },
    });

    const disabledOption = container.querySelectorAll('.check-option')[2];
    const disabledCheck = disabledOption.querySelector('input[type="checkbox"]') as HTMLInputElement;
    const reason = disabledOption.querySelector('.check-option__disabled-reason');

    expect(disabledCheck).toBeDisabled();
    expect(reason?.textContent).toContain('Locked by another process');

    await fireEvent.click(disabledOption);
    expect(onchange).not.toHaveBeenCalled();
  });
});
