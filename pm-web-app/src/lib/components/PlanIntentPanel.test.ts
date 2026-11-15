import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import PlanIntentPanel from './PlanIntentPanel.svelte';
import { apiClient, type PlanIntentRecord } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `PlanIntentPanel` (design `intent-and-outline.adoc#intent`): the plan's intent in the record's own
 * order — the head with the counts by verdict, the criteria with their verdict as a `StatusLabel`,
 * the non-goals by their own `NG-n` ids, the choices grouped by who made them, and the open
 * questions and deferred items. Every record text is shown as text, never as HTML.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de' = 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/**
 * @param overrides the members this case changes
 * @returns a plan intent record with every member of the contract
 */
function intent(overrides: Partial<PlanIntentRecord> = {}): PlanIntentRecord {
  return {
    plan_id: 'fix-relay',
    revision: 3,
    goal: 'Make the relay reconnect without losing queued events.',
    criteria: [
      {
        id: 'AC-001',
        text: 'When a relay connection drops, the relay shall reconnect within five seconds.',
        satisfies: [1],
        status: 'met',
      },
      {
        id: 'AC-002',
        text: 'While the relay reconnects, the queue shall keep every accepted event.',
        satisfies: [2],
        status: 'not_met',
        status_reason: 'The compatibility test still fails for the legacy event shape.',
      },
      {
        id: 'AC-003',
        text: 'The relay shall report the number of reconnects it performed.',
        satisfies: [1],
        status: 'not_verified',
        status_reason: 'The harness cannot observe the reconnect counter.',
      },
      {
        id: 'AC-004',
        text: 'The old relay endpoint shall keep serving existing clients.',
        satisfies: [],
      },
    ],
    non_goals: [
      { id: 'NG-3', text: 'Do not change the queue format.' },
      { id: 'NG-1', text: 'Do not add a second relay per machine.' },
      { id: 'NG-12', text: 'Do not raise the backoff ceiling.' },
    ],
    decisions: [
      {
        id: 'DC-001',
        text: 'Retry with exponential backoff.',
        decider: 'operator',
        options_weighed: ['fixed interval', 'exponential backoff'],
        question_id: 'Q-007',
        adr: { adr_id: 'ADR-0001', relation: 'follows' },
      },
      {
        id: 'DC-002',
        text: 'The quorum set the backoff ceiling at five minutes.',
        decider: 'quorum',
        options_weighed: ['30 seconds', '5 minutes'],
        question_id: 'Q-008',
      },
      {
        id: 'DC-003',
        text: 'The transport stays HTTP/2.',
        decider: 'role',
        options_weighed: ['HTTP/1.1', 'HTTP/2'],
      },
      {
        id: 'DC-004',
        text: 'The existing queue already persists each accepted event.',
        decider: 'fact',
        options_weighed: [],
        evidence: 'pm-relay/src/main/java/de/planmarshall/relay/EventQueue.java',
      },
      {
        id: 'DC-005',
        text: 'The reconnect count leaves the status resource for now.',
        decider: 'operator',
        options_weighed: ['keep the count', 'leave it out'],
        question_id: 'Q-009',
        adr: { adr_id: 'ADR-0002', relation: 'contradicts' },
      },
    ],
    open_questions: ['Does the relay need IPv6 support?'],
    deferred: [
      {
        id: 'DF-001',
        text: 'Add reconnect metrics to the status resource.',
        reason: "No metrics endpoint is in this plan's scope.",
      },
    ],
    ...overrides,
  };
}

/**
 * @param record the record the runtime serves
 * @returns the panel, rendered against that record
 */
async function renderPanel(record: PlanIntentRecord | null) {
  vi.spyOn(apiClient, 'getPlanIntent').mockResolvedValue(record);
  vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  const { container } = render(PlanIntentPanel, {
    props: { planId: 'fix-relay', deliverables: [] },
  });
  await screen.findByTestId('plan-intent-panel');
  return container;
}

describe('PlanIntentPanel', () => {
  beforeEach(() => {
    localStorage.clear();
    useCatalogue();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('draws the head with the plan, the revision, the goal and the counts by verdict', async () => {
    await renderPanel(intent());

    expect(screen.getByTestId('intent-head')).toHaveTextContent(
      'Intent · fix-relay · revision 3',
    );
    expect(screen.getByTestId('intent-goal')).toHaveTextContent(
      'Make the relay reconnect without losing queued events.',
    );
    expect(screen.getByTestId('intent-counts')).toHaveTextContent(
      '4 criteria · 1 met · 1 not met · 1 not verified',
    );
  });

  it('shows not reviewed yet for a criterion the self-review has not judged', async () => {
    await renderPanel(
      intent({
        criteria: [
          {
            id: 'AC-001',
            text: 'When a relay connection drops, the relay shall reconnect within five seconds.',
            satisfies: [1],
          },
        ],
      }),
    );

    expect(screen.getByTestId('intent-counts')).toHaveTextContent('not reviewed yet');
    expect(screen.getByTestId('criterion-verdict-AC-001')).toHaveTextContent('not reviewed yet');
    expect(
      screen.getByTestId('criterion-verdict-AC-001').querySelector('.status-label'),
    ).toHaveClass('status-label--neutral');
  });

  it('draws each verdict as its StatusLabel state and tone, not_verified neutral', async () => {
    const container = await renderPanel(intent());

    const met = within(container).getByTestId('criterion-verdict-AC-001');
    expect(met).toHaveTextContent('met');
    expect(met.querySelector('.status-label')).toHaveClass('status-label--success');

    const notMet = within(container).getByTestId('criterion-verdict-AC-002');
    expect(notMet).toHaveTextContent('not met');
    expect(notMet.querySelector('.status-label')).toHaveClass('status-label--danger');

    const notVerified = within(container).getByTestId('criterion-verdict-AC-003');
    expect(notVerified).toHaveTextContent('not verified');
    expect(notVerified.querySelector('.status-label')).toHaveClass('status-label--neutral');
    expect(notVerified.querySelector('.status-label')).not.toHaveClass('status-label--attention');

    const notReviewed = within(container).getByTestId('criterion-verdict-AC-004');
    expect(notReviewed).toHaveTextContent('not reviewed yet');
    expect(notReviewed.querySelector('.status-label')).toHaveClass('status-label--neutral');
  });

  it('keeps a not_met and a not_verified reason one expansion away', async () => {
    await renderPanel(intent());

    expect(screen.queryByText(/The compatibility test still fails/)).toBeNull();
    expect(screen.queryByText(/The harness cannot observe/)).toBeNull();

    await fireEvent.click(screen.getByTestId('criterion-reason-AC-002'));
    await fireEvent.click(screen.getByTestId('criterion-reason-AC-003'));

    expect(screen.getByTestId('criterion-reason-text-AC-002')).toHaveTextContent(
      'Reason: The compatibility test still fails for the legacy event shape.',
    );
    expect(screen.getByTestId('criterion-reason-text-AC-003')).toHaveTextContent(
      'Reason: The harness cannot observe the reconnect counter.',
    );
  });

  it('says no deliverable yet for a criterion without one, and names the ones that satisfy it', async () => {
    await renderPanel(intent());

    expect(screen.getByTestId('criterion-deliverables-AC-001')).toHaveTextContent(
      'satisfied by 1',
    );
    expect(screen.getByTestId('criterion-deliverables-AC-004')).toHaveTextContent(
      'no deliverable yet',
    );
  });

  it('shows the non-goals by the record ids, out of sequence as given', async () => {
    await renderPanel(intent());

    const ids = screen
      .getAllByTestId(/^non-goal-/)
      .map((item) => item.getAttribute('data-non-goal-id'));

    expect(ids).toEqual(['NG-3', 'NG-1', 'NG-12']);
    expect(screen.getByTestId('non-goal-NG-1')).toHaveTextContent(
      'Do not add a second relay per machine.',
    );
  });

  it('groups the choices the operator and a quorum made first, the role calls apart', async () => {
    const container = await renderPanel(intent());

    const groups = within(container)
      .getAllByTestId(/^choice-group-/)
      .map((group) => group.getAttribute('data-decider'));

    expect(groups).toEqual(['operator', 'quorum', 'role', 'fact']);
    expect(within(container).getByTestId('choice-group-operator')).toHaveTextContent(
      'you decided',
    );
    expect(within(container).getByTestId('choice-group-role')).toHaveTextContent(
      "the model's call",
    );
    expect(within(container).getByTestId('choice-group-role')).toHaveClass(
      'intent-choice-group--own-call',
    );
    expect(within(container).getByTestId('choice-group-operator')).not.toHaveClass(
      'intent-choice-group--own-call',
    );
  });

  it('keeps the weighed options in the record order, the question a step away, and the ADR relation', async () => {
    await renderPanel(intent());

    expect(screen.getByTestId('choice-options-DC-001')).toHaveTextContent(
      'options weighed: fixed interval, exponential backoff',
    );
    expect(screen.getByTestId('choice-question-DC-001')).toHaveTextContent('answers Q-007');
    expect(screen.getByTestId('choice-question-DC-001').querySelector('a')).toHaveAttribute(
      'href',
      '/decisions?decision=Q-007',
    );
    expect(screen.getByTestId('choice-adr-DC-001')).toHaveTextContent('follows ADR-0001');
    expect(screen.getByTestId('choice-adr-DC-005')).toHaveTextContent('contradicts ADR-0002');
    expect(screen.queryByTestId('choice-question-DC-003')).toBeNull();
  });

  it('shows a fact decision with its evidence as text, never as a link', async () => {
    await renderPanel(intent());

    const fact = screen.getByTestId('choice-DC-004');
    expect(fact).toHaveTextContent(
      'evidence: pm-relay/src/main/java/de/planmarshall/relay/EventQueue.java',
    );
    expect(fact.querySelector('a')).toBeNull();
  });

  it('shows no vote support, because the record carries none', async () => {
    const container = await renderPanel(intent());

    expect(container.textContent ?? '').not.toMatch(/vote/i);
    expect(container.querySelector('[class*="vote"]')).toBeNull();
  });

  it('renders control sequences and markup of a record text as text', async () => {
    const container = await renderPanel(
      intent({
        criteria: [
          {
            id: 'AC-001',
            text: 'When a relay drops\u001b[31m the queue <b>shall stay</b>\u001b[0m intact.',
            satisfies: [],
          },
        ],
      }),
    );

    const text = within(container).getByTestId('criterion-text-AC-001');
    expect(text.textContent).toBe('When a relay drops the queue <b>shall stay</b> intact.');
    expect(text.querySelector('b')).toBeNull();
    expect(container.innerHTML).not.toContain('\u001b');
  });

  it('renders the title of a satisfying deliverable as text after the control sequences are gone', async () => {
    vi.spyOn(apiClient, 'getPlanIntent').mockResolvedValue(
      intent({
        criteria: [
          {
            id: 'AC-001',
            text: 'When a relay connection drops, the relay shall reconnect within five seconds.',
            satisfies: [1],
            status: 'met',
          },
        ],
      }),
    );
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    render(PlanIntentPanel, {
      props: {
        planId: 'fix-relay',
        deliverables: [
          {
            number: 1,
            title: 'Reconnect\u001b[31m the relay <b>now</b>\u001b[0m',
            intent_gloss: 'gloss',
            affected_files: [],
            depends_on: [],
            satisfies: ['AC-001'],
          },
        ],
      },
    });

    const satisfied = await screen.findByTestId('criterion-deliverables-AC-001');
    expect(satisfied.textContent?.replace(/\s+/g, ' ')).toBe(
      'satisfied by 1 · Reconnect the relay <b>now</b>',
    );
    expect(satisfied.querySelector('b')).toBeNull();
  });

  it('renders a record without criteria without failing', async () => {
    await renderPanel(intent({ criteria: [] }));

    expect(screen.getByTestId('intent-counts')).toHaveTextContent('not reviewed yet');
    expect(screen.queryAllByTestId(/^criterion-AC-/)).toEqual([]);
  });

  it('says refine has not written the record yet when the runtime has none', async () => {
    await renderPanel(null);

    expect(screen.getByTestId('plan-intent-panel')).toHaveTextContent(
      'Refine has not written the intent yet.',
    );
  });

  it("reads the record again when the plan's events say the plan changed, and says so once", async () => {
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });
    const load = vi
      .spyOn(apiClient, 'getPlanIntent')
      .mockResolvedValueOnce(intent())
      .mockResolvedValueOnce(intent({ revision: 4 }));

    render(PlanIntentPanel, { props: { planId: 'fix-relay', deliverables: [] } });
    await screen.findByTestId('intent-head');
    expect(load).toHaveBeenCalledTimes(1);

    onEvent?.('hold.opened');
    expect(load).toHaveBeenCalledTimes(1);

    onEvent?.('phase.changed');
    await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(screen.getByTestId('intent-head')).toHaveTextContent('revision 4'),
    );
    expect(screen.getByTestId('intent-changed')).toHaveTextContent('the record changed');
  });

  it('keeps the newer record when two reads answer out of order', async () => {
    let resolveFirst: ((value: PlanIntentRecord | null) => void) | undefined;
    let resolveSecond: ((value: PlanIntentRecord | null) => void) | undefined;
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });
    vi.spyOn(apiClient, 'getPlanIntent')
      .mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));

    render(PlanIntentPanel, { props: { planId: 'fix-relay', deliverables: [] } });
    onEvent?.('phase.changed');

    // The newer read answers first, the older one last: the older record is dropped.
    resolveSecond?.(intent({ revision: 5 }));
    await waitFor(() =>
      expect(screen.getByTestId('intent-head')).toHaveTextContent('revision 5'),
    );
    resolveFirst?.(intent({ revision: 4 }));
    await waitFor(() =>
      expect(screen.getByTestId('intent-head')).toHaveTextContent('revision 5'),
    );
  });
});
