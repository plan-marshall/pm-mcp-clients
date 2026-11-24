import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CleanupView from './CleanupView.svelte';
import { runtimeStore } from '../stores/runtime';
import { get } from 'svelte/store';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type CleanupJob, type CleanupKeptItem } from '../api/client';
import { ClientError } from '../api/errors';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * @param locale the language the catalogue is loaded in
 */
function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('CleanupView component', () => {
  const mockJob: CleanupJob = {
    job_id: 'cln-0007',
    due_since: 'today 03:00',
    summary: {
      plans_count: 12,
      epics_count: 2,
      logs_count: 40,
      read_reports_count: 5,
      unenrolled_projects_count: 1,
      freed_bytes: 1932735283,
      kept_for_good_count: 4,
      last_sweep_at: 'today 03:00',
      next_check_at: 'tomorrow 03:00',
    },
    projects: [
      {
        project_id: 'pm-mcp-clients',
        display_name: 'pm-mcp-clients',
        items: [
          {
            id: 'plan-1',
            kind: 'plan',
            title: 'fix-relay',
            ended_at: 'yesterday 14:00',
            freed_bytes: 100000000,
            detail: 'plan kept until 2026-10-20',
          },
          {
            id: 'logs-1',
            kind: 'logs',
            title: 'logs of relay-docs',
            ended_at: 'yesterday 15:00',
            freed_bytes: 73741824,
            detail: 'logs of ended plan',
          },
        ],
      },
      {
        project_id: 'legacy-service',
        display_name: 'legacy-service',
        items: [
          {
            id: 'unenrolled-1',
            kind: 'unenrolled',
            title: 'Unenrolled project legacy-service',
            ended_at: '2026-09-01',
            freed_bytes: 1758993459,
          },
        ],
      },
    ],
    kept: [],
    residue: [],
    durations: {
      plans_days: 14,
      epics_days: 30,
      logs_days: 7,
      reports_days: 14,
      unenrolled_days: 60,
      remind_days: 3,
    },
  };

  const mockKeptItems: CleanupKeptItem[] = [
    {
      item_id: 'kept-1',
      kind: 'plan',
      title: 'token-usage',
      why: 'exclude',
      kept_at: 'yesterday 18:00',
      who: 'operator',
    },
    {
      item_id: 'kept-2',
      kind: 'epic',
      title: 'v1-migration',
      why: 'ignore',
      kept_at: '2026-10-10',
      who: 'web app (Firefox on mbp)',
    },
  ];

  beforeEach(() => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Firefox on mbp',
        machineName: 'mbp-oliver',
        currentRoute: 'cleanup',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        isDraining: false,
        drainInfo: null,
        isStopped: false,
        stoppedInfo: null,
        activeStep: 1,
        unreadReportsCount: 0,
      }),
    );
    vi.restoreAllMocks();
  });

  afterEach(() => {
    useCatalogue('en');
  });

  it('draws the error pattern when the cleanup job cannot be read', async () => {
    const job = vi.spyOn(apiClient, 'getCleanupJob').mockRejectedValue(new ClientError('timeout'));
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: [] });

    render(CleanupView);

    const error = await screen.findByTestId('cleanup-error');
    expect(error).toHaveTextContent('The request took too long');
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(job).toHaveBeenCalledTimes(2));
  });

  it('renders cleanup job summary and project item checklists', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('Summary')).toBeInTheDocument();
    });
    expect(screen.getByText('fix-relay')).toBeInTheDocument();
    expect(screen.getByText('Unenrolled project legacy-service')).toBeInTheDocument();
    expect(screen.getByText(/plans/i)).toBeInTheDocument();
    expect(screen.getByText(/frees 1\.8 GB/i)).toBeInTheDocument();
    // Every item is checked when the job opens.
    for (const checkbox of screen.getAllByRole('checkbox')) {
      expect(checkbox).toBeChecked();
    }
  });

  it('unchecking an item excludes it and updates freed bytes in delete button', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('fix-relay')).toBeInTheDocument();
    });

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes).toHaveLength(3);
    expect(checkboxes[0]).toBeChecked();

    // Uncheck first item
    await fireEvent.click(checkboxes[0]);
    expect(checkboxes[0]).not.toBeChecked();

    // Delete button should now name the two kinds it still removes
    expect(screen.getByRole('button', { name: /Delete 1 log/i })).toBeInTheDocument();
  });

  it('uncheck all (None) unchecks every item of the job, and Check all checks them again', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('fix-relay')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'None' }));

    const checkboxes = screen.getAllByRole('checkbox');
    for (const checkbox of checkboxes) {
      expect(checkbox).not.toBeChecked();
    }

    await fireEvent.click(screen.getByRole('button', { name: 'Check all' }));

    for (const checkbox of screen.getAllByRole('checkbox')) {
      expect(checkbox).toBeChecked();
    }
  });

  it('disables Delete while nothing is checked and says why', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => expect(screen.getByText('fix-relay')).toBeInTheDocument());
    await fireEvent.click(screen.getByRole('button', { name: 'None' }));

    const deleteButton = screen.getByRole('button', { name: /^Delete/ });
    expect(deleteButton).toHaveAttribute('aria-disabled', 'true');
    expect(deleteButton).toHaveAttribute('aria-describedby', expect.stringContaining('btn-reason'));
    expect(
      screen.getByText('Nothing is checked; Ignore keeps everything listed.'),
    ).toBeInTheDocument();
  });

  it('opens the confirmation naming the counts and executes cleanup with excluded ids once', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    let resolveDelete: (value: { status: string; freed_bytes: number }) => void = () => {};
    const deleteSpy = vi.spyOn(apiClient, 'executeCleanup').mockReturnValue(
      new Promise<{ status: string; freed_bytes: number }>((resolve) => {
        resolveDelete = resolve;
      }),
    );

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('fix-relay')).toBeInTheDocument();
    });

    // Uncheck item-1
    const checkboxes = screen.getAllByRole('checkbox');
    await fireEvent.click(checkboxes[0]);

    // Click Delete button
    const deleteBtn = screen.getByRole('button', { name: /Delete 1 log/i });
    await fireEvent.click(deleteBtn);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
    // The confirmation names the counts, the space and what stays.
    expect(dialog).toHaveTextContent('Delete 1 log · 1 unenrolled project?');
    expect(dialog).toHaveTextContent('This frees 1.7 GB. It cannot be undone.');
    expect(dialog).toHaveTextContent('Branches, pull requests, the remote');
    expect(dialog).toHaveTextContent('1 unchecked item is kept for good.');

    // Confirm deletion: a second confirm while the act runs is ignored.
    const confirmBtn = screen.getByRole('button', { name: /^Delete$/i });
    await fireEvent.click(confirmBtn);
    await fireEvent.click(confirmBtn);

    expect(deleteSpy).toHaveBeenCalledTimes(1);
    expect(deleteSpy).toHaveBeenCalledWith('cln-0007', ['plan-1']);

    resolveDelete({ status: 'deleted', freed_bytes: 1825361100 });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it("shows the sentence of the act failure's code instead of the code", async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    vi.spyOn(apiClient, 'executeCleanup').mockRejectedValue(new ClientError('timeout'));

    render(CleanupView);

    await waitFor(() => expect(screen.getByText('fix-relay')).toBeInTheDocument());
    await fireEvent.click(screen.getByRole('button', { name: /Delete 1 plan/i }));
    await fireEvent.click(screen.getByRole('button', { name: /^Delete$/i }));

    expect(
      await screen.findByText(
        'Try again; if it keeps taking this long, check that the runtime is running.',
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText('timeout')).not.toBeInTheDocument();
  });

  it('handles remind and ignore action triggers', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    const remindSpy = vi.spyOn(apiClient, 'remindCleanup').mockResolvedValue();
    const ignoreSpy = vi.spyOn(apiClient, 'ignoreCleanup').mockResolvedValue();

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Ask me again/i })).toBeInTheDocument();
    });

    const remindBtn = screen.getByRole('button', { name: /Ask me again/i });
    await fireEvent.click(remindBtn);
    expect(remindSpy).toHaveBeenCalledWith('cln-0007');

    const ignoreBtn = screen.getByRole('button', { name: 'Ignore' });
    await fireEvent.click(ignoreBtn);
    expect(ignoreSpy).toHaveBeenCalledWith('cln-0007');
  });

  it('opens an item read-only and returns with the checks kept', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => expect(screen.getByText('fix-relay')).toBeInTheDocument());

    const before = screen.getAllByRole('checkbox');
    await fireEvent.click(before[0]);
    expect(before[0]).not.toBeChecked();

    await fireEvent.click(screen.getByRole('button', { name: /Open fix-relay read-only/i }));

    expect(screen.getByTestId('cleanup-preview')).toBeInTheDocument();
    expect(screen.getByText(/Read-only: opening an item decides nothing/i)).toBeInTheDocument();
    expect(get(runtimeStore).cleanupPreviewItemId).toBe('plan-1');

    await fireEvent.click(screen.getByRole('button', { name: /Back to the job/i }));

    const after = screen.getAllByRole('checkbox');
    expect(after[0]).not.toBeChecked();
    expect(get(runtimeStore).cleanupUncheckedIds).toEqual({ 'plan-1': true });
  });

  it('highlights the item a report opened the job at', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    runtimeStore.update((s) => ({ ...s, cleanupHighlightItemId: 'plan-1' }));

    render(CleanupView);

    await waitFor(() => expect(screen.getByText('fix-relay')).toBeInTheDocument());
    expect(document.getElementById('cleanup-item-plan-1')?.className).toContain(
      'item-row--highlight',
    );
  });

  it('switches to kept tab and releases kept items', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    const releaseSpy = vi
      .spyOn(apiClient, 'releaseKeptCleanup')
      .mockResolvedValue({ released_count: 2 });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /Kept for good/i })).toBeInTheDocument();
    });

    const keptTab = screen.getByRole('tab', { name: /Kept for good/i });
    await fireEvent.click(keptTab);

    expect(screen.getByText('token-usage')).toBeInTheDocument();
    expect(screen.getByText('v1-migration')).toBeInTheDocument();

    const releaseAllBtn = screen.getByRole('button', { name: 'Release all' });
    await fireEvent.click(releaseAllBtn);

    expect(releaseSpy).toHaveBeenCalledWith([], true);
  });

  it('names what a German delete removed from the catalogue, not an English items literal', async () => {
    useCatalogue('de');
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    vi.spyOn(apiClient, 'executeCleanup').mockResolvedValue({
      status: 'deleted',
      freed_bytes: 1825361100,
    });

    render(CleanupView);

    await waitFor(() => expect(screen.getByText('fix-relay')).toBeInTheDocument());
    await fireEvent.click(screen.getByRole('button', { name: /^Lösche 1 Plan/i }));
    await fireEvent.click(screen.getByRole('button', { name: 'Lösche' }));

    const notice = await screen.findByRole('status');
    expect(notice).toHaveTextContent('1 Plan · 1 Log · 1 ausgetragenes Projekt gelöscht');
    expect(notice).not.toHaveTextContent('items');
  });

  it('offers the way back to the system view, which the delete flow came from', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);
    await waitFor(() => expect(screen.getByText('fix-relay')).toBeInTheDocument());

    const back = screen.getByRole('link', { name: 'System' });
    expect(back).toHaveAttribute('href', '/system');
  });

  it('does not render an in-page back button in the empty state (D3)', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(null as unknown as CleanupJob);
    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Nothing is due/i })).toBeInTheDocument();
    });

    expect(screen.queryByRole('button', { name: /Back to System/i })).not.toBeInTheDocument();
  });
});
