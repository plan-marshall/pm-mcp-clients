import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import OverSizeChart from './OverSizeChart.svelte';
import type { OverSizeDistributionRecord } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `OverSizeChart` (design `intent-and-outline.adoc#oversize`, `#buckets`, `#web`): one role's
 * `over_size` distribution as a small bar chart — the buckets in the runtime's order, the last one
 * open-ended, the buckets a proposal would flag marked, and the counts, the flagged share and the
 * direction in text, so it reads without colour and at phone width. It never recomputes a bucket, and
 * it offers no act.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de' = 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/**
 * @param overrides the members this case changes
 * @returns the `implement` distribution the mock runtime serves in `running-work`
 */
function distribution(
  overrides: Partial<OverSizeDistributionRecord> = {},
): OverSizeDistributionRecord {
  return {
    project_id: 'pm-mcp-clients',
    role: 'implement',
    current_multiple: 3,
    proposed_multiple: 2,
    buckets: [
      { label: '0-1x', from_multiple: 0, to_multiple: 1, task_count: 8, flagged_tasks: 0 },
      { label: '1-2x', from_multiple: 1, to_multiple: 2, task_count: 5, flagged_tasks: 0 },
      { label: '2-3x', from_multiple: 2, to_multiple: 3, task_count: 3, flagged_tasks: 3 },
      { label: '3x and more', from_multiple: 3, to_multiple: 6, task_count: 4, flagged_tasks: 4 },
    ],
    ...overrides,
  };
}

/** @returns the chart, rendered for the record */
function renderChart(record: OverSizeDistributionRecord) {
  const { container } = render(OverSizeChart, { props: { distribution: record } });
  return container;
}

describe('OverSizeChart', () => {
  beforeEach(() => {
    localStorage.clear();
    useCatalogue();
  });

  it('names the role and the multiple in force beside the proposed one', () => {
    renderChart(distribution());

    expect(screen.getByTestId('oversize-title')).toHaveTextContent('implement · over_size 3x → 2x');
  });

  it('draws the buckets of the mock record in their order, each with its count', () => {
    const container = renderChart(distribution());

    const buckets = within(container).getAllByTestId(/^oversize-bucket-\d$/);
    expect(buckets.map((bucket) => bucket.getAttribute('data-bucket-index'))).toEqual([
      '0',
      '1',
      '2',
      '3',
    ]);
    expect(within(container).getByTestId('oversize-bucket-0')).toHaveTextContent('0–1x');
    expect(within(container).getByTestId('oversize-bucket-0')).toHaveTextContent('8 tasks');
    expect(within(container).getByTestId('oversize-bucket-1')).toHaveTextContent('5 tasks');
  });

  it('leaves an empty bucket out instead of inventing a zero bar', () => {
    const container = renderChart(
      distribution({
        buckets: [
          { label: '0-1x', from_multiple: 0, to_multiple: 1, task_count: 8, flagged_tasks: 0 },
          { label: '1-2x', from_multiple: 1, to_multiple: 2, task_count: 0, flagged_tasks: 0 },
          { label: '3x and more', from_multiple: 3, to_multiple: 6, task_count: 0, flagged_tasks: 0 },
        ],
      }),
    );

    expect(within(container).getAllByTestId(/^oversize-bucket-\d$/)).toHaveLength(1);
    expect(within(container).queryByTestId('oversize-bucket-1')).toBeNull();
    expect(container.textContent).not.toContain('1–2x');
  });

  it('draws the last bucket as the open-ended one, whatever its label', () => {
    const container = renderChart(
      distribution({
        buckets: [
          { label: '0-1x', from_multiple: 0, to_multiple: 1, task_count: 8, flagged_tasks: 0 },
          { label: 'the tail', from_multiple: 3, to_multiple: 6, task_count: 4, flagged_tasks: 4 },
        ],
      }),
    );

    expect(within(container).getByTestId('oversize-bucket-1')).toHaveTextContent('3x and more');
    expect(within(container).getByTestId('oversize-bucket-1')).not.toHaveTextContent('the tail');
  });

  it('marks the buckets the proposal would flag, in text as well as in colour', () => {
    const container = renderChart(distribution());

    const flagged = within(container).getByTestId('oversize-bucket-2');
    expect(flagged).toHaveClass('oversize-bucket--flagged');
    expect(within(flagged).getByTestId('oversize-flagged-2')).toHaveTextContent('3 flagged');

    const plain = within(container).getByTestId('oversize-bucket-0');
    expect(plain).not.toHaveClass('oversize-bucket--flagged');
    expect(within(plain).queryByTestId('oversize-flagged-0')).toBeNull();
  });

  it('states the share of flagged tasks as a count and a percentage', () => {
    renderChart(distribution());

    expect(screen.getByTestId('oversize-share')).toHaveTextContent('20 tasks · 7 flagged · 35 %');
  });

  it('rounds the share to a whole percentage', () => {
    renderChart(
      distribution({
        buckets: [
          { label: '0-1x', from_multiple: 0, to_multiple: 1, task_count: 2, flagged_tasks: 0 },
          { label: '3x and more', from_multiple: 3, to_multiple: 6, task_count: 1, flagged_tasks: 1 },
        ],
      }),
    );

    expect(screen.getByTestId('oversize-share')).toHaveTextContent('3 tasks · 1 flagged · 33 %');
  });

  it('says what lowering the multiple would do', () => {
    renderChart(distribution());

    expect(screen.getByTestId('oversize-direction')).toHaveTextContent(
      '3x → 2x lowers the multiple and can flag more',
    );
  });

  it('says what raising the multiple would do', () => {
    renderChart(distribution({ proposed_multiple: 4 }));

    expect(screen.getByTestId('oversize-direction')).toHaveTextContent(
      '3x → 4x raises the multiple and can flag fewer',
    );
  });

  it('shows the distribution under the multiple in force without a proposal: no mark, no share, no direction', () => {
    const container = renderChart(
      distribution({
        proposed_multiple: undefined,
        buckets: [
          { label: '0-1x', from_multiple: 0, to_multiple: 1, task_count: 8, flagged_tasks: 0 },
          { label: '1-2x', from_multiple: 1, to_multiple: 2, task_count: 5, flagged_tasks: 0 },
          { label: '3x and more', from_multiple: 3, to_multiple: 6, task_count: 4, flagged_tasks: 0 },
        ],
      }),
    );

    expect(within(container).getByTestId('oversize-title')).toHaveTextContent(
      'implement · over_size 3x',
    );
    expect(within(container).getByTestId('oversize-title')).not.toHaveTextContent('→');
    expect(container.querySelectorAll('.oversize-bucket--flagged')).toHaveLength(0);
    expect(within(container).queryByTestId('oversize-share')).toBeNull();
    expect(within(container).queryByTestId('oversize-direction')).toBeNull();
    expect(within(container).getAllByTestId(/^oversize-bucket-\d$/)).toHaveLength(3);
  });

  it('shows the empty text for a role without measured tasks', () => {
    const container = renderChart(distribution({ buckets: [], proposed_multiple: undefined }));

    expect(within(container).getByTestId('oversize-empty')).toHaveTextContent(
      'No measured tasks of this role yet.',
    );
    expect(within(container).getByTestId('oversize-title')).toHaveTextContent(
      'implement · over_size 3x',
    );
    expect(within(container).queryAllByTestId(/^oversize-bucket-\d$/)).toEqual([]);
  });

  it('offers no act: no button, no link, no field', () => {
    const container = renderChart(distribution());

    expect(container.querySelector('button')).toBeNull();
    expect(container.querySelector('a')).toBeNull();
    expect(container.querySelector('input')).toBeNull();
    expect(container.querySelector('[role="button"]')).toBeNull();
  });
});
