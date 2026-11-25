import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/svelte';
import OutlinePanel from './OutlinePanel.svelte';
import { apiClient, type OutlineRecord } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `OutlinePanel` (design `intent-and-outline.adoc#outline`): the plan's answer to how it will satisfy
 * the intent — the deliverables with their gloss, the files they affect and the intent of each, the
 * deliverables they depend on, the criteria they satisfy and how they are verified. A path is never
 * turned into a link, and a dependency on a deliverable the record does not carry is shown, never
 * hidden.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de' = 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/**
 * @param overrides the members this case changes
 * @returns an outline with one deliverable per file intent
 */
function outline(overrides: Partial<OutlineRecord> = {}): OutlineRecord {
  return {
    plan_id: 'fix-relay',
    revision: 2,
    summary: 'Reconnect the relay with backoff and keep the queue durable.',
    deliverables: [
      {
        number: 1,
        title: 'Reconnect the relay',
        intent_gloss: 'Reconnect with exponential backoff.',
        affected_files: [
          { path: 'pm-relay/src/main/java/RelayConnection.java', intent: 'write-replace' },
          { path: 'pm-relay/src/test/java/RelayConnectionTest.java', intent: 'write-new' },
        ],
        depends_on: [],
        satisfies: ['AC-001'],
        verification: 'RelayConnectionTest covers reconnect.',
      },
      {
        number: 2,
        title: 'Keep the queue durable',
        intent_gloss: 'Persist every accepted event before acknowledging it.',
        affected_files: [
          { path: 'pm-relay/src/main/java/EventQueue.java', intent: 'write-conditional' },
          { path: 'docs/relay/queue.adoc', intent: 'read' },
          { path: 'pm-relay/src/main/java/Legacy.java', intent: 'delete' },
        ],
        depends_on: [1],
        satisfies: ['AC-002'],
      },
    ],
    added_dependencies: [
      {
        name: 'de.cuioss:cui-http',
        version: '2.1.0',
        justification: 'The retry adapter carries the backoff policy.',
        deliverable: 1,
      },
    ],
    ...overrides,
  };
}

/**
 * @param record the record the runtime serves
 * @returns the panel, rendered against that record
 */
async function renderPanel(record: OutlineRecord | null) {
  vi.spyOn(apiClient, 'getPlanOutline').mockResolvedValue(record);
  vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  const { container } = render(OutlinePanel, { props: { planId: 'fix-relay' } });
  await screen.findByTestId('outline-panel');
  return container;
}

describe('OutlinePanel', () => {
  beforeEach(() => {
    localStorage.clear();
    useCatalogue();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('draws each deliverable with its number, title and gloss', async () => {
    await renderPanel(outline());

    expect(screen.getByTestId('outline-deliverable-1')).toHaveTextContent(
      '1 · Reconnect the relay',
    );
    expect(screen.getByTestId('outline-gloss-1')).toHaveTextContent(
      'Reconnect with exponential backoff.',
    );
    expect(screen.getByTestId('outline-deliverable-2')).toHaveTextContent(
      '2 · Keep the queue durable',
    );
  });

  it('draws each affected file one per line, with the intent of each', async () => {
    const container = await renderPanel(outline());

    const files = Array.from(container.querySelectorAll('[data-testid^="outline-file-"]')).map(
      (file) => file.textContent?.replace(/\s+/g, ' ').trim(),
    );

    expect(files).toEqual([
      'pm-relay/src/main/java/RelayConnection.java replaced',
      'pm-relay/src/test/java/RelayConnectionTest.java new',
      'pm-relay/src/main/java/EventQueue.java changed on a condition',
      'docs/relay/queue.adoc read',
      'pm-relay/src/main/java/Legacy.java deleted',
    ]);
  });

  it('never turns a path into a link', async () => {
    const container = await renderPanel(outline());

    // The only anchors are the criteria a deliverable references; a file path is text.
    const links = Array.from(container.querySelectorAll('a'));
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/plans/fix-relay?tab=intent#criterion-AC-001',
      '/plans/fix-relay?tab=intent#criterion-AC-002',
    ]);
    expect(links.some((link) => (link.textContent ?? '').includes('pm-relay/'))).toBe(false);
  });

  it('shows the dependency by number and title, and a missing one as missing deliverable', async () => {
    await renderPanel(
      outline({
        deliverables: [
          {
            number: 1,
            title: 'Reconnect the relay',
            intent_gloss: 'Reconnect with backoff.',
            affected_files: [],
            depends_on: [],
            satisfies: ['AC-001'],
          },
          {
            number: 2,
            title: 'Keep the queue durable',
            intent_gloss: 'Persist every event.',
            affected_files: [],
            depends_on: [1, 7],
            satisfies: ['AC-002'],
          },
        ],
      }),
    );

    expect(screen.getByTestId('outline-depends-2')).toHaveTextContent(
      'depends on 1 · Reconnect the relay, missing deliverable 7',
    );
  });

  it('shows the criteria a deliverable satisfies, and satisfies no criterion where it names none', async () => {
    await renderPanel(
      outline({
        deliverables: [
          {
            number: 1,
            title: 'Reconnect the relay',
            intent_gloss: 'Reconnect with backoff.',
            affected_files: [],
            depends_on: [],
            satisfies: ['AC-001', 'AC-004'],
          },
          {
            number: 2,
            title: 'Keep the queue',
            intent_gloss: 'Persist every event.',
            affected_files: [],
            depends_on: [1],
            satisfies: [],
          },
        ],
      }),
    );

    expect(screen.getByTestId('outline-satisfies-1')).toHaveTextContent('satisfies AC-001, AC-004');
    expect(screen.getByTestId('outline-satisfies-1').querySelector('a')).toHaveAttribute(
      'href',
      '/plans/fix-relay?tab=intent#criterion-AC-001',
    );
    expect(screen.getByTestId('outline-satisfies-2')).toHaveTextContent('satisfies no criterion');
  });

  it('shows the verification where the deliverable declares one', async () => {
    await renderPanel(outline());

    expect(screen.getByTestId('outline-verification-1')).toHaveTextContent(
      'Verification: RelayConnectionTest covers reconnect.',
    );
    expect(screen.queryByTestId('outline-verification-2')).toBeNull();
  });

  it('renders a single deliverable', async () => {
    await renderPanel(
      outline({
        deliverables: [
          {
            number: 1,
            title: 'Reconnect the relay',
            intent_gloss: 'Reconnect with backoff.',
            affected_files: [],
            depends_on: [],
            satisfies: ['AC-001'],
          },
        ],
      }),
    );

    expect(screen.getByTestId('outline-deliverable-1')).toBeInTheDocument();
  });

  it('renders an outline without deliverables without failing', async () => {
    await renderPanel(outline({ deliverables: [] }));

    expect(screen.getByTestId('outline-panel')).toBeInTheDocument();
    expect(screen.queryAllByTestId(/^outline-deliverable-/)).toEqual([]);
  });

  it('renders an outline without a record as its title alone', async () => {
    await renderPanel(null);

    expect(screen.getByTestId('outline-panel')).toHaveTextContent('Outline');
    expect(screen.queryAllByTestId(/^outline-deliverable-/)).toEqual([]);
  });

  it("reads the outline again when the plan's events say the plan changed, and says so once", async () => {
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });
    const load = vi
      .spyOn(apiClient, 'getPlanOutline')
      .mockResolvedValueOnce(outline())
      .mockResolvedValueOnce(outline({ revision: 3 }));

    render(OutlinePanel, { props: { planId: 'fix-relay' } });
    await screen.findByTestId('outline-panel');
    expect(load).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId('outline-changed')).toBeNull();

    onEvent?.('hold.opened');
    expect(load).toHaveBeenCalledTimes(1);

    onEvent?.('phase.changed');
    await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(screen.getByTestId('outline-changed')).toHaveTextContent('the record changed'),
    );
  });

  it('keeps the newer outline when two reads answer out of order', async () => {
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    let resolveFirst: ((value: OutlineRecord | null) => void) | undefined;
    let resolveSecond: ((value: OutlineRecord | null) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });
    vi.spyOn(apiClient, 'getPlanOutline')
      .mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));

    render(OutlinePanel, { props: { planId: 'fix-relay' } });
    onEvent?.('phase.changed');

    resolveSecond?.(outline({ revision: 5, deliverables: [] }));
    await screen.findByTestId('outline-panel');
    resolveFirst?.(outline({ revision: 4 }));
    await waitFor(() => expect(screen.queryByTestId('outline-changed')).toBeNull());
    expect(screen.getByTestId('outline-panel')).not.toHaveTextContent('the record changed');
  });
});
