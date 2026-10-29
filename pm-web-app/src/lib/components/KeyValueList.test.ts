import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import KeyValueList from './KeyValueList.svelte';

/**
 * `KeyValueList` (VD-P-05): two columns of runtime figures — the label muted, the value primary —
 * as a definition list, so a screen reader pairs every label with its value.
 */

describe('KeyValueList', () => {
  it('draws every pair as a label and its value', () => {
    render(KeyValueList, {
      props: {
        items: [
          { label: 'Runtime', value: 'plan-marshall 0.1.0' },
          { label: 'Started', value: '2 minutes ago' },
        ],
      },
    });
    const list = screen.getByTestId('key-value-list');
    const labels = Array.from(list.querySelectorAll('dt'));
    const values = Array.from(list.querySelectorAll('dd'));

    expect(labels.map((label) => label.textContent)).toEqual(['Runtime', 'Started']);
    expect(values.map((value) => value.textContent)).toEqual([
      'plan-marshall 0.1.0',
      '2 minutes ago',
    ]);
    expect(labels[0]?.className).toContain('key-value-list__label');
    expect(values[0]?.className).toContain('key-value-list__value');
  });

  it('draws nothing but the empty list when there is no pair', () => {
    render(KeyValueList, { props: { items: [] } });
    const list = screen.getByTestId('key-value-list');

    expect(list.querySelectorAll('dt')).toHaveLength(0);
    expect(list.querySelectorAll('dd')).toHaveLength(0);
  });
});
