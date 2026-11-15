import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import RadioGroup from './RadioGroup.svelte';

describe('RadioGroup', () => {
  const options = [
    { value: 'opt1', label: 'Option 1', description: 'First option' },
    { value: 'opt2', label: 'Option 2', marker: 'recommended' as const },
    { value: 'opt3', label: 'Option 3', disabled: true, disabledReason: 'Unavailable' },
  ];

  it('renders fieldset with legend as the question', () => {
    const { container } = render(RadioGroup, {
      props: { legend: 'Choose mode', options, value: 'opt1', name: 'mode' },
    });
    const fieldset = container.querySelector('fieldset.radio-group');
    const legend = container.querySelector('legend');

    expect(fieldset).toBeInTheDocument();
    expect(legend?.textContent).toContain('Choose mode');
  });

  it('renders card form where the whole card is the target', async () => {
    const onchange = vi.fn();
    const { container } = render(RadioGroup, {
      props: { legend: 'Choose tier', options, value: 'opt1', variant: 'card', name: 'tier', onchange },
    });

    const group = container.querySelector('.radio-group--card');
    expect(group).toBeInTheDocument();

    const cards = container.querySelectorAll('.radio-card');
    expect(cards.length).toBe(3);

    // Clicking the second card element directly
    await fireEvent.click(cards[1]);
    expect(onchange).toHaveBeenCalledWith('opt2');
  });

  it('renders a Marker after an option label', () => {
    const { container } = render(RadioGroup, {
      props: { legend: 'Choose tier', options, value: 'opt1', name: 'tier' },
    });
    const marker = container.querySelector('.marker');
    expect(marker).toBeInTheDocument();
    expect(marker?.textContent).toContain('recommended');
  });

  it('supports arrow-key movement and wrapping', async () => {
    const onchange = vi.fn();
    const activeOptions = [
      { value: 'a', label: 'A' },
      { value: 'b', label: 'B' },
      { value: 'c', label: 'C' },
    ];
    const { container } = render(RadioGroup, {
      props: { legend: 'Pick', options: activeOptions, value: 'a', name: 'pick', onchange },
    });

    const radios = container.querySelectorAll('input[type="radio"]');

    // ArrowDown from A -> B
    await fireEvent.keyDown(radios[0], { key: 'ArrowDown' });
    expect(onchange).toHaveBeenCalledWith('b');

    // ArrowDown from B -> C
    await fireEvent.keyDown(radios[1], { key: 'ArrowDown' });
    expect(onchange).toHaveBeenCalledWith('c');

    // ArrowDown from C wraps to A
    await fireEvent.keyDown(radios[2], { key: 'ArrowDown' });
    expect(onchange).toHaveBeenCalledWith('a');

    // ArrowUp from A wraps to C
    await fireEvent.keyDown(radios[0], { key: 'ArrowUp' });
    expect(onchange).toHaveBeenCalledWith('c');
  });

  it('renders disabled option with reason and ignores clicks', async () => {
    const onchange = vi.fn();
    const { container } = render(RadioGroup, {
      props: { legend: 'Choose tier', options, value: 'opt1', name: 'tier', onchange },
    });

    const disabledOption = container.querySelectorAll('.radio-option')[2];
    const disabledRadio = disabledOption.querySelector('input[type="radio"]') as HTMLInputElement;
    const reason = disabledOption.querySelector('.radio-option__disabled-reason');

    expect(disabledRadio).toBeDisabled();
    expect(reason?.textContent).toContain('Unavailable');

    await fireEvent.click(disabledOption);
    expect(onchange).not.toHaveBeenCalled();
  });
});
