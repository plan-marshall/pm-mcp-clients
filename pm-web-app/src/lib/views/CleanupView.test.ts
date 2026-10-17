import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CleanupView from './CleanupView.svelte';
import { runtimeStore } from '../stores/runtime';
import { apiClient, type CleanupJob, type CleanupKeptItem } from '../api/client';

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
    runtimeStore.set({
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
      isDraining: false,
      drainInfo: null,
      isStopped: false,
      stoppedInfo: null,
      activeStep: 1,
      unreadReportsCount: 0,
    });
    vi.restoreAllMocks();
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
  });

  it('unchecking an item excludes it and updates freed bytes in delete button', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('fix-relay')).toBeInTheDocument();
    });

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes.length).toBe(3);
    expect(checkboxes[0]).toBeChecked();

    // Uncheck first item
    await fireEvent.click(checkboxes[0]);
    expect(checkboxes[0]).not.toBeChecked();

    // Delete button should now show 2 items
    expect(screen.getByRole('button', { name: /Delete 2 items/i })).toBeInTheDocument();
  });

  it('uncheck all (None) unchecks all items in project', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('fix-relay')).toBeInTheDocument();
    });

    const noneBtn = screen.getAllByText('None')[0];
    await fireEvent.click(noneBtn);

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes[0]).not.toBeChecked();
    expect(checkboxes[1]).not.toBeChecked();
  });

  it('opens delete confirmation modal and executes cleanup with excluded ids', async () => {
    vi.spyOn(apiClient, 'getCleanupJob').mockResolvedValue(mockJob);
    vi.spyOn(apiClient, 'getKeptCleanup').mockResolvedValue({ items: mockKeptItems });
    const deleteSpy = vi
      .spyOn(apiClient, 'executeCleanup')
      .mockResolvedValue({ status: 'deleted', freed_bytes: 1825361100 });

    render(CleanupView);

    await waitFor(() => {
      expect(screen.getByText('fix-relay')).toBeInTheDocument();
    });

    // Uncheck item-1
    const checkboxes = screen.getAllByRole('checkbox');
    await fireEvent.click(checkboxes[0]);

    // Click Delete button
    const deleteBtn = screen.getByRole('button', { name: /Delete 2 items/i });
    await fireEvent.click(deleteBtn);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/cannot be undone/i)).toBeInTheDocument();

    // Confirm deletion
    const confirmBtn = screen.getByRole('button', { name: /^Delete$/i });
    await fireEvent.click(confirmBtn);

    expect(deleteSpy).toHaveBeenCalledWith('cln-0007', ['plan-1']);
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
});
