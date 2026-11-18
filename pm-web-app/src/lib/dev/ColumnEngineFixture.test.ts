import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { tick } from 'svelte';
import ColumnEngineFixture from './ColumnEngineFixture.svelte';
import { reportBox } from '../test/resizeObserver';

/**
 * The showroom's column-engine fixture: a synthetic five-level chain in both themes. It is drawn
 * only in development, and it must be usable on a phone, where the chat takes the whole screen and
 * _Hide the chat_ is the only way back (`navigation.adoc#side-columns`).
 */

describe('ColumnEngineFixture', () => {
  it('hides the chat on a phone, so the showroom is usable there', async () => {
    render(ColumnEngineFixture);
    reportBox(screen.getByTestId('triage-page'), 390, 800);
    await tick();
    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');

    await fireEvent.click(screen.getByRole('button', { name: 'Hide the chat' }));

    expect(screen.queryByTestId('side-column')).toBeNull();
  });

  it('draws the chat on a wide screen', async () => {
    render(ColumnEngineFixture);
    reportBox(screen.getByTestId('triage-page'), 1216, 800);
    await tick();

    expect(screen.getByTestId('side-column')).not.toHaveClass('side-column--taken');
  });
});
