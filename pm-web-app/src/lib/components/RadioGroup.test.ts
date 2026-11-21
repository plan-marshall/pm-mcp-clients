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
    expect(cards).toHaveLength(3);

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

  describe('one pointer click selects once', () => {
    const described = [
      { value: 'first', label: 'First', description: 'The first choice' },
      { value: 'second', label: 'Second', description: 'The second choice' },
    ];

    it.each([
      ['the label text', '.radio-option__label'],
      ['the radio', 'input[type="radio"]'],
      ['the description', '.radio-option__description'],
    ])('calls onchange exactly once for a click on %s', async (_target, selector) => {
      const onchange = vi.fn();
      const { container } = render(RadioGroup, {
        props: { legend: 'Pick', options: described, value: 'first', name: 'once', onchange },
      });
      const second = container.querySelectorAll('.radio-option')[1];

      await fireEvent.click(second.querySelector(selector) as HTMLElement);

      expect(onchange).toHaveBeenCalledTimes(1);
      expect(onchange).toHaveBeenCalledWith('second');
      expect(second.querySelector('input')).toBeChecked();
    });
  });

  it('gives every option of two groups with the same name and values its own id', () => {
    const { container } = render(RadioGroup, {
      props: { legend: 'Twin', options, value: 'opt1', name: 'twin' },
    });
    render(RadioGroup, { props: { legend: 'Twin', options, value: 'opt1', name: 'twin' } });

    const radios = [...document.body.querySelectorAll<HTMLInputElement>('input[name="twin"]')];
    const ids = radios.map((radio) => radio.id);

    expect(radios).toHaveLength(6);
    expect(ids.every((id) => id.length > 0)).toBe(true);
    expect(new Set(ids).size).toBe(ids.length);
    expect(container.querySelectorAll('input[type="radio"]')).toHaveLength(3);
  });

  it('points each radio at its own description and disabled reason', () => {
    const { container } = render(RadioGroup, {
      props: { legend: 'Choose tier', options, value: 'opt1', name: 'described' },
    });
    const rows = container.querySelectorAll('.radio-option');
    const describedBy = (row: Element) =>
      row.querySelector('input')?.getAttribute('aria-describedby') ?? null;

    const description = rows[0].querySelector('.radio-option__description') as HTMLElement;
    expect(description.id).not.toBe('');
    expect(describedBy(rows[0])).toBe(description.id);

    expect(describedBy(rows[1])).toBeNull();

    const reason = rows[2].querySelector('.radio-option__disabled-reason') as HTMLElement;
    expect(reason.id).not.toBe('');
    expect(describedBy(rows[2])).toBe(reason.id);
  });

  it('does not point a disabled radio without a reason at a reason, nor a description-less radio', () => {
    const { container } = render(RadioGroup, {
      props: {
        legend: 'Locked',
        options: [
          { value: 'x', label: 'X', description: 'Has a description' },
          { value: 'y', label: 'Y' },
        ],
        value: 'x',
        name: 'locked',
        disabled: true,
      },
    });
    const radios = container.querySelectorAll('input[type="radio"]');
    const description = container.querySelector('.radio-option__description') as HTMLElement;

    expect(radios[0].getAttribute('aria-describedby')).toBe(description.id);
    expect(radios[1].getAttribute('aria-describedby')).toBeNull();
  });

  it('reaches and focuses an option whose value carries a double quote and a closing bracket', async () => {
    const onchange = vi.fn();
    const tricky = [
      { value: 'plain', label: 'Plain' },
      { value: 'a"]b', label: 'Tricky' },
    ];
    const { container } = render(RadioGroup, {
      props: { legend: 'Pick', options: tricky, value: 'plain', name: 'tricky', onchange },
    });
    const radios = container.querySelectorAll<HTMLInputElement>('input[type="radio"]');
    radios[0].focus();

    await fireEvent.keyDown(radios[0], { key: 'ArrowRight' });

    expect(onchange).toHaveBeenCalledExactlyOnceWith('a"]b');
    expect(document.activeElement).toBe(radios[1]);
    expect(radios[1]).toBeChecked();
  });

  it('moves to the last option with ArrowLeft when no option is selected, and ignores other keys', async () => {
    const onchange = vi.fn();
    const activeOptions = [
      { value: 'a', label: 'A' },
      { value: 'b', label: 'B' },
    ];
    const { container } = render(RadioGroup, {
      props: { legend: 'Pick', options: activeOptions, name: 'none-selected', onchange },
    });
    const radios = container.querySelectorAll<HTMLInputElement>('input[type="radio"]');

    await fireEvent.keyDown(radios[0], { key: 'Home' });
    expect(onchange).not.toHaveBeenCalled();

    await fireEvent.keyDown(radios[0], { key: 'ArrowLeft' });
    expect(onchange).toHaveBeenCalledExactlyOnceWith('b');
    expect(document.activeElement).toBe(radios[1]);
  });

  it('moves nowhere when every option is disabled', async () => {
    const onchange = vi.fn();
    const { container } = render(RadioGroup, {
      props: {
        legend: 'Pick',
        options: [{ value: 'a', label: 'A' }],
        name: 'all-disabled',
        disabled: true,
        onchange,
      },
    });

    await fireEvent.keyDown(container.querySelector('input') as HTMLInputElement, {
      key: 'ArrowDown',
    });
    expect(onchange).not.toHaveBeenCalled();
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
