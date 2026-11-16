import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import InboxView from './InboxView.svelte';
import {
  attentionStore,
  resetAttention,
  type AttentionItem,
  type AttentionState,
} from '../stores/attention';
import { apiClient, type ProjectSummary } from '../api/client';

/**
 * The start page's list (needs-and-find.adoc, The List in the Frame): the rows in the served order,
 * the reason of each item, the decision without one, the second line and the act; and the empty
 * state with the running work.
 */

/**
 * @param overrides the item's own fields
 * @returns one item of the list, as the store keeps it
 */
function itemOf(overrides: Partial<AttentionItem> = {}): AttentionItem {
  return {
    kind: 'cleanup_due',
    link: '/api/v1/system/cleanup',
    project: null,
    epic: null,
    plan: null,
    since: '2026-10-15T08:00:00Z',
    blocks: null,
    ...overrides,
  };
}

/**
 * @param items the items the list holds
 * @param extra the rest of the state
 */
function seedList(items: AttentionItem[], extra: Partial<AttentionState> = {}): void {
  attentionStore.set({
    items,
    total: items.length,
    page: 1,
    hasOlder: false,
    loaded: true,
    isLoading: false,
    loadError: null,
    leftNotice: null,
    ...extra,
  });
}

/**
 * @param project a project of the summary
 * @returns its sum-able counts for the fixture
 */
function projectOf(project: Partial<ProjectSummary> & { project_id: string }): ProjectSummary {
  return {
    display_name: project.project_id,
    active_plans_count: 0,
    active_epics_count: 0,
    needs_operator_count: 0,
    setup_needs_operator: false,
    ...project,
  };
}

describe('InboxView component', () => {
  beforeEach(() => {
    localStorage.clear();
    resetAttention();
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('draws the rows in the served order, blocking first, with their reason', () => {
    seedList([
      itemOf({
        kind: 'decision',
        link: '/api/v1/plans/drop-legacy-api',
        project: { id: 'pm-mcp-clients', title: 'pm-mcp-clients' },
        plan: { id: 'drop-legacy-api', title: 'Drop the legacy endpoints' },
        blocks: 'plan',
      }),
      itemOf(),
    ]);

    render(InboxView);

    const rows = screen.getAllByTestId('inbox-row');
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent('Drop the legacy endpoints');
    expect(rows[1]).toHaveTextContent('cleanup');

    // A decision carries no reason badge; the machine's own cleanup item carries its reason.
    expect(within(rows[0]).queryByText(/cleanup due/)).toBeNull();
    expect(rows[0].querySelector('.reason-badge')).toBeNull();
    expect(rows[1].querySelector('.reason-badge')).toHaveTextContent('cleanup due');
  });

  it('carries the second line of every row: the path, the since and what it blocks', () => {
    seedList([
      itemOf({
        kind: 'findings',
        link: '/api/v1/epics/relay-hardening',
        project: { id: 'pm-mcp-clients', title: 'pm-mcp-clients' },
        epic: { id: 'relay-hardening', title: 'Relay Hardening' },
        blocks: 'epic',
      }),
    ]);

    render(InboxView);

    const detail = screen.getByTestId('inbox-detail');
    // The row's title already names the epic, so the path is what lies above it.
    expect(detail).toHaveTextContent('pm-mcp-clients');
    expect(detail).not.toHaveTextContent('pm-mcp-clients ›');
    expect(detail).toHaveTextContent('waits since');
    expect(detail).toHaveTextContent('blocks: epic');
  });

  it('offers the act of each place as a button, and the due cleanup opens the cleanup job', () => {
    seedList([itemOf()]);

    render(InboxView);

    const row = screen.getByTestId('inbox-row');
    expect(row.querySelector('a')).toHaveAttribute('href', '/system/cleanup');
    expect(within(row).getByRole('button', { name: 'Cleanup' })).toBeInTheDocument();
  });

  it('names the empty list and the running work per project', async () => {
    seedList([]);
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({
      projects: [
        projectOf({ project_id: 'pm-mcp-clients', active_plans_count: 2 }),
        projectOf({ project_id: 'quiet' }),
      ],
    });

    render(InboxView);

    expect(await screen.findByText('Nothing waits for you.')).toBeInTheDocument();
    const running = await screen.findByTestId('inbox-running-projects');
    expect(running).toHaveTextContent('pm-mcp-clients');
    expect(running).not.toHaveTextContent('quiet');
  });

  it('reads the next page only where the runtime reports one', async () => {
    seedList([itemOf()], { hasOlder: true, total: 2 });

    render(InboxView);

    expect(screen.getByTestId('inbox-more')).toHaveTextContent('Show more');
    expect(screen.getByTestId('inbox-count')).toHaveTextContent('! 2');
  });
});
