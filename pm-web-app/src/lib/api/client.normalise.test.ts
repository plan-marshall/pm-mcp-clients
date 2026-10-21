import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ApiClient } from './client';

/**
 * The runtime writes these records under names of its own (`project_id`, `tokens`,
 * `newer_version`, `size_bytes`); the views read the short names. These tests pin the wiring.
 */
describe('ApiClient wire normalisation', () => {
  let client: ApiClient;

  beforeEach(() => {
    localStorage.clear();
    client = new ApiClient('http://localhost:7420');
  });

  /** @param body the response body @returns a fetch stub answering it */
  function respond(body: unknown): void {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => body,
    });
  }

  it('reads a report origin and a deleted subject from the runtime spelling', async () => {
    respond({
      items: [
        {
          id: 'rep-1',
          from: { project_id: 'pm-mcp-clients', epic_id: 'relay', plan_id: 'fix' },
          kind: 'plan.ended',
          subject: 'fix ended',
          created_at: '2026-10-16T11:20:00Z',
          subject_deleted: true,
        },
      ],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });

    const page = await client.getReports();

    expect(page.items[0].from).toMatchObject({
      project: 'pm-mcp-clients',
      epic: 'relay',
      plan: 'fix',
    });
    expect(page.items[0].subject_deleted).toBe(true);
  });

  it('reads the completion report tokens under the names the view reads', async () => {
    respond({
      plan_id: 'fix',
      title: 'fix',
      from: { project_id: 'pm-mcp-clients', plan_id: 'fix' },
      outcome: 'merged',
      started_at: '2026-10-16T08:40:00Z',
      ended_at: '2026-10-16T11:20:00Z',
      duration_seconds: 9600,
      paused_seconds: 0,
      deliverables: [],
      replanning_records: [],
      decision_records: [],
      token_usage: {
        total_tokens: 100,
        input_tokens: 70,
        output_tokens: 30,
        duration_seconds: 9600,
        workers_count: 1,
        models_count: 1,
        tasks_measured: 3,
        tasks_total: 4,
        is_floor: true,
        unmeasured_reason: 'their worker was terminated',
        by_phase: [
          {
            label: 'refine',
            tokens: 100,
            input_tokens: 70,
            output_tokens: 30,
            unmeasured_tasks: 1,
            is_floor: true,
            details: { input: 70, cache_read: 0, cache_write: 0, output: 30, reasoning: 0 },
          },
        ],
        by_role: [],
        by_model: [],
        waste_records: [],
        total_waste_tokens: 0,
        waste_share_percent: 0,
      },
    });

    const report = await client.getPlanCompletion('fix');

    expect(report.from.plan).toBe('fix');
    expect(report.token_usage?.total).toBe(100);
    expect(report.token_usage?.input).toBe(70);
    expect(report.token_usage?.is_floor).toBe(true);
    expect(report.token_usage?.by_phase[0]).toMatchObject({
      name: 'refine',
      total: 100,
      input: 70,
      output: 30,
      unmeasured: 1,
      floor: true,
    });
    expect(report.token_usage?.by_phase[0].components?.input).toBe(70);
  });

  it('reads the runtime metrics and models under the view names', async () => {
    respond({
      runtime: {
        version: '0.9.1',
        build: '2026-10-02 (a1b2c3d)',
        heap_used_bytes: 100,
        heap_max_bytes: 200,
        resident_memory_bytes: 150,
        cpu_percent: 4,
        paired_devices_count: 2,
        loopback_listener: '127.0.0.1:7420',
        network_listener: 'off',
        newer_version: '0.10.0',
        newer_version_date: '2026-10-16',
        newer_version_checked_at: 'today 08:14',
        release_notes_link: '/api/v1/system/release-notes?version=0.10.0',
        upgrade_supported: true,
      },
      work: {},
      workers: [],
      jobs: {},
      models: [
        {
          harness_and_model: 'claude:opus',
          role: 'planner',
          calls_count: 2,
          tokens_total: 10,
          tokens_in: 8,
          tokens_out: 2,
          errors_count: 0,
          retries_count: 0,
          is_floor: true,
          unmeasured_calls: 1,
        },
      ],
      cleanup: {
        plans_count: 1,
        epics_count: 2,
        logs_count: 3,
        reports_count: 4,
        projects_count: 5,
      },
      window: 'today',
    });

    const snapshot = await client.getSystemSnapshot('today');

    expect(snapshot.runtime).toMatchObject({
      build_date: '2026-10-02 (a1b2c3d)',
      memory_used_bytes: 100,
      memory_max_bytes: 200,
      memory_resident_bytes: 150,
      cpu_measured: true,
      lan_devices_count: 2,
      loopback_address: '127.0.0.1:7420',
      lan_address: null,
      upgrade_available_version: '0.10.0',
      upgrade_possible: true,
      upgrade_release_notes_url: '/api/v1/system/release-notes?version=0.10.0',
    });
    expect(snapshot.models[0]).toMatchObject({
      id: 'claude:opus',
      tokens_unmeasured: true,
      unmeasured_calls: 1,
    });
    expect(snapshot.cleanup).toMatchObject({
      read_reports_count: 4,
      unenrolled_projects_count: 5,
    });
  });

  it('does not fall back to the system withdraw route on a refusal', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({ error: 'cannot_withdraw' }),
    });

    await expect(client.withdrawUpgrade()).rejects.toThrow('cannot_withdraw');
    expect(globalThis.fetch).toHaveBeenCalledTimes(1);
  });

  it('reads a cleanup job and its kept items under the view names', async () => {
    respond({
      job_id: 'cln-1',
      state: 'open',
      due_since: 'today 03:00',
      summary: { plans_count: 1, epics_count: 0, logs_count: 0, freed_bytes: 10 },
      project_items: [
        {
          project_id: 'pm-mcp-clients',
          items: [{ id: 'fix', kind: 'plan', title: 'fix', outcome: 'merged', size_bytes: 10 }],
        },
      ],
      links: {},
    });

    const job = await client.getCleanupJob();
    expect(job.projects[0].display_name).toBe('pm-mcp-clients');
    expect(job.projects[0].items[0]).toMatchObject({
      id: 'fix',
      title: 'fix',
      freed_bytes: 10,
      detail: 'merged',
    });

    respond([
      { id: 'kept-1', kind: 'plan', title: 'kept', how: 'ignored', kept_at: '2 Oct', who: 'web' },
    ]);
    const kept = await client.getKeptCleanup();
    expect(kept.items[0]).toMatchObject({
      item_id: 'kept-1',
      why: 'ignored',
      title: 'kept',
    });
  });
});
