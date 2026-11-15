import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/svelte';
import EpicIntentPanel from './EpicIntentPanel.svelte';
import { apiClient, type EpicIntentRecord } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `EpicIntentPanel` (design `intent-and-outline.adoc#epic-destination`, `#epic-rest`, `#web`): the
 * epic's destination, the done-when checks with their verdict as a `StatusLabel` state and their
 * evidence as text, the non-goals, the questions not yet specified, and each plan's own criterion
 * with the plan's title. No verdict is invented, and nothing becomes a progress bar.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de' = 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/**
 * @param overrides the members this case changes
 * @returns an epic intent record with every member of the contract
 */
function epicIntent(overrides: Partial<EpicIntentRecord> = {}): EpicIntentRecord {
  return {
    epic_id: 'relay-hardening',
    destination: 'The relay survives a dropped connection without losing queued events.',
    done_when: [
      {
        text: 'The relay reconnects within five seconds.',
        verdict: 'met',
        evidence: ['pm://plans/fix-relay/report', 'doc/design/shared/intent-and-outline.adoc'],
      },
      {
        text: 'The reconnect count is observable.',
        verdict: 'not_met',
        evidence: ['pm://plans/fix-relay/report'],
      },
      {
        text: 'The old endpoint still answers.',
        verdict: 'pending',
        evidence: [],
      },
    ],
    non_goals: ['Do not change the queue format.', 'Do not add a second relay per machine.'],
    not_yet_specified: [
      'Does the relay need IPv6 support?',
      'Which queue size triggers the backpressure warning?',
    ],
    plans: [
      {
        plan_id: 'fix-relay',
        title: 'Fix Relay Transport',
        done_when: [
          'The relay reconnects within five seconds.',
          'The queue keeps every accepted event.',
        ],
      },
      {
        plan_id: 'queue-format',
        title: 'Keep the Queue',
        done_when: [],
      },
    ],
    ...overrides,
  };
}

/**
 * @param record the record the runtime serves
 * @returns the panel, rendered against that record
 */
async function renderPanel(record: EpicIntentRecord | null) {
  vi.spyOn(apiClient, 'getEpicIntent').mockResolvedValue(record);
  vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  const { container } = render(EpicIntentPanel, { props: { epicId: 'relay-hardening' } });
  await screen.findByTestId('epic-intent-panel');
  return container;
}

describe('EpicIntentPanel', () => {
  beforeEach(() => {
    localStorage.clear();
    useCatalogue();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('draws the destination as one sentence', async () => {
    await renderPanel(epicIntent());

    expect(screen.getByTestId('epic-destination')).toHaveTextContent(
      'The relay survives a dropped connection without losing queued events.',
    );
  });

  it('draws a pending check neutral and a decided one success or danger, never a progress bar', async () => {
    const container = await renderPanel(epicIntent());

    const met = within(container).getByTestId('epic-check-verdict-0');
    expect(met).toHaveTextContent('met');
    expect(met.querySelector('.status-label')).toHaveClass('status-label--success');

    const notMet = within(container).getByTestId('epic-check-verdict-1');
    expect(notMet).toHaveTextContent('not met');
    expect(notMet.querySelector('.status-label')).toHaveClass('status-label--danger');

    const pending = within(container).getByTestId('epic-check-verdict-2');
    expect(pending).toHaveTextContent('pending');
    expect(pending.querySelector('.status-label')).toHaveClass('status-label--neutral');
    expect(pending.querySelector('.status-label')).not.toHaveClass('status-label--attention');

    expect(container.querySelector('progress')).toBeNull();
    expect(container.querySelector('[role="progressbar"]')).toBeNull();
  });

  it('shows a check evidence as text, one entry per line, and nothing for a check without one', async () => {
    const container = await renderPanel(epicIntent());

    const evidence = within(container).getAllByTestId(/^epic-check-evidence-0-/);
    expect(evidence).toHaveLength(2);
    expect(evidence[0]).toHaveTextContent('evidence: pm://plans/fix-relay/report');
    expect(evidence[1]).toHaveTextContent('evidence: doc/design/shared/intent-and-outline.adoc');
    expect(within(container).getByTestId('epic-check-0').querySelector('a')).toBeNull();
    expect(within(container).queryAllByTestId(/^epic-check-evidence-2-/)).toEqual([]);
  });

  it('shows the non-goals and the unspecified questions, each as its own line', async () => {
    await renderPanel(epicIntent());

    expect(screen.getByTestId('epic-non-goals')).toHaveTextContent('Non-goals');
    expect(screen.getByTestId('epic-non-goal-1')).toHaveTextContent(
      'Do not add a second relay per machine.',
    );
    expect(screen.getByTestId('epic-not-specified')).toHaveTextContent('Not yet specified');
    expect(screen.getByTestId('epic-question-0')).toHaveTextContent(
      'Does the relay need IPv6 support?',
    );
  });

  it('draws no not-yet-specified section for an empty list', async () => {
    const container = await renderPanel(epicIntent({ not_yet_specified: [] }));

    expect(within(container).queryByTestId('epic-not-specified')).toBeNull();
    expect(within(container).queryAllByTestId(/^epic-question-/)).toEqual([]);
  });

  it("shows each plan's done-when with the plan's title, and a plan without one by its title alone", async () => {
    const container = await renderPanel(epicIntent());

    const plan = within(container).getByTestId('epic-plan-fix-relay');
    expect(plan).toHaveTextContent('Fix Relay Transport');
    expect(within(plan).getAllByTestId(/^epic-plan-fix-relay-done-when-/)).toHaveLength(2);
    expect(within(plan).getByTestId('epic-plan-fix-relay-done-when-1')).toHaveTextContent(
      'The queue keeps every accepted event.',
    );

    const bare = within(container).getByTestId('epic-plan-queue-format');
    expect(bare).toHaveTextContent('Keep the Queue');
    expect(within(bare).queryAllByTestId(/^epic-plan-queue-format-done-when-/)).toEqual([]);
  });

  it('renders control sequences and markup of a record text as text', async () => {
    const container = await renderPanel(
      epicIntent({
        destination: 'The relay <b>survives</b>\u001b[31m a drop.\u001b[0m',
        not_yet_specified: ['Does the relay <i>need</i> IPv6?'],
      }),
    );

    expect(within(container).getByTestId('epic-destination').textContent).toBe(
      'The relay <b>survives</b> a drop.',
    );
    expect(within(container).getByTestId('epic-destination').querySelector('b')).toBeNull();
    expect(within(container).getByTestId('epic-question-0').querySelector('i')).toBeNull();
    expect(container.innerHTML).not.toContain('\u001b');
  });

  it('says the epic has no intent record when the runtime serves none', async () => {
    await renderPanel(null);

    expect(screen.getByTestId('epic-intent-panel')).toHaveTextContent(
      'This epic has no intent record.',
    );
  });

  it("reads the record again on the epic's own events and says the record changed once", async () => {
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });
    vi.spyOn(apiClient, 'getEpicIntent')
      .mockResolvedValueOnce(epicIntent())
      .mockResolvedValueOnce(epicIntent({ destination: 'The relay survives a restart.' }));

    render(EpicIntentPanel, { props: { epicId: 'relay-hardening' } });
    await screen.findByTestId('epic-destination');
    expect(screen.queryByTestId('epic-changed')).toBeNull();

    onEvent?.('epic.decomposition_changed');

    await waitFor(() =>
      expect(screen.getByTestId('epic-destination')).toHaveTextContent(
        'The relay survives a restart.',
      ),
    );
    expect(screen.getByTestId('epic-changed')).toHaveTextContent('the record changed');
  });

  it('keeps the record on screen when an event of another record arrives', async () => {
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });
    const read = vi.spyOn(apiClient, 'getEpicIntent').mockResolvedValue(epicIntent());

    render(EpicIntentPanel, { props: { epicId: 'relay-hardening' } });
    await screen.findByTestId('epic-destination');
    const reads = read.mock.calls.length;

    onEvent?.('tokens.changed');

    expect(read.mock.calls).toHaveLength(reads);
    expect(screen.queryByTestId('epic-changed')).toBeNull();
  });
});
