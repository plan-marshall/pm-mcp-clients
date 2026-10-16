export interface PairingResponse {
  device_secret: string;
  device_id: string;
  label: string;
}

export interface PairedDevice {
  device_id: string;
  label: string;
  paired_from?: string;
  created_at: string;
  last_used_at: string;
}

export interface RevokedEventData {
  reason?: string;
  revoked_by?: string;
  revoked_at?: string;
  details?: string;
  [key: string]: unknown;
}

export interface StatusResponse {
  status: string;
  version?: string;
  listener?: string;
  onboarded?: boolean;
  machine?: {
    name?: string;
    onboarded?: boolean;
  };
  projects?: unknown[];
  web?: {
    enabled?: boolean;
    lan?: boolean;
    port?: number;
  };
}

export interface HypermediaLink {
  href: string;
  method?: string;
  next?: string;
}

export interface ProjectSummary {
  project_id: string;
  display_name: string;
  active_plans_count: number;
  active_epics_count: number;
  needs_operator_count: number;
  needs_operator_summary?: string | null;
  setup_needs_operator: boolean;
  last_activity_at?: string | null;
}

export interface ProjectOverviewResponse {
  projects: ProjectSummary[];
}

export interface ActivePlanSummary {
  plan_id: string;
  title: string;
  current_phase: string;
  phase_state: string;
  pausing_point?: string | null;
  phase_entered_at?: string | null;
  running_since?: string | null;
  tasks_done?: number;
  tasks_total?: number;
  epic_id?: string | null;
  epic_title?: string | null;
  pull_request_number?: number | null;
  pull_request_url?: string | null;
  off_track_signal?: string | null;
  needs_operator_kind?: string | null;
  needs_operator_count?: number | null;
}

export interface ActiveEpicSummary {
  epic_id: string;
  title: string;
  running_since?: string | null;
  running_plans_count: number;
  staged_plans_count: number;
  plans_done?: number;
  plans_total?: number;
  needs_operator_kind?: string | null;
  needs_operator_count?: number | null;
  plans: ActivePlanSummary[];
}

export interface AlongsideLanding {
  unit?: string | null;
  count?: number | null;
  state: string;
  kind: string;
  hold_kind?: string | null;
}

export interface ProjectDetailResponse {
  project_id: string;
  display_name: string;
  plans: ActivePlanSummary[];
  epics: ActiveEpicSummary[];
  alongside_landings: AlongsideLanding[];
}

export interface EndedWorkItem {
  id?: string;
  item_id?: string;
  kind: string;
  title: string;
  outcome?: string | null;
  terminal_reason?: string | null;
  pull_request_number?: number | null;
  pull_request_url?: string | null;
  reason?: string | null;
  ended_at: string;
}

export interface EndedWorkPage {
  project_id: string;
  page: number;
  total_pages: number;
  has_newer: boolean;
  has_older: boolean;
  items: EndedWorkItem[];
}

export interface StateRepresentation {
  scope?: string;
  state?: string;
  summary?: string;
  links?: Record<string, HypermediaLink | string>;
  _links?: Record<string, HypermediaLink | string>;
  [key: string]: unknown;
}

export interface ReportFrom {
  project?: string;
  epic?: string;
  plan?: string;
}

export interface ReportItem {
  id: string;
  from: ReportFrom;
  kind: string;
  subject: string;
  subject_params?: Record<string, unknown>;
  body?: string;
  created_at: string;
  sent_at?: string;
  read_at?: string;
  read_channel?: string;
  read_device?: string;
  detail_link?: string;
  created?: string;
  sent?: string;
  summary?: string;
  detail?: string;
}

export interface ReportsPage {
  items: ReportItem[];
  unread_count: number;
  oldest_unread_time?: string;
  has_older: boolean;
  older_count: number;
}

export interface DeliverableSummary {
  name: string;
  state: string;
  title?: string;
}

export interface ReplanningRecord {
  trigger: string;
  time: string;
  change: string;
}

export interface DecisionRecord {
  id: string;
  question?: string;
  text?: string;
  answer?: string;
  by: string;
  channel?: string;
  device?: string;
}

export interface TokenComponentRecord {
  input: number;
  cache_read: number;
  cache_write: number;
  output: number;
  reasoning: number;
}

export interface TokenBreakdownItem {
  name: string;
  total: number;
  input: number;
  output: number;
  unmeasured: number;
  floor: boolean;
  harness_model?: string;
  effort?: string;
  components?: TokenComponentRecord;
}

export interface TokenWasteRecord {
  waste_class: string;
  tokens: number;
  share_percent: number;
  tasks_count: number;
  cause_summary?: string;
}

export interface TokenUsageRecord {
  total: number;
  input: number;
  output: number;
  duration_seconds: number;
  workers_count: number;
  models_count: number;
  tasks_measured: number;
  tasks_total: number;
  is_floor: boolean;
  unmeasured_reason?: string;
  by_phase: TokenBreakdownItem[];
  by_role: TokenBreakdownItem[];
  by_model: TokenBreakdownItem[];
  waste_records: TokenWasteRecord[];
  total_waste_tokens: number;
  waste_share_percent: number;
}

export interface PlanCompletionReport {
  plan_id: string;
  title: string;
  from: ReportFrom;
  outcome: string;
  terminal_reason?: string;
  pull_request_number?: number;
  pull_request_url?: string;
  reason?: string;
  started_at: string;
  ended_at: string;
  duration_seconds: number;
  paused_seconds: number;
  paused_by?: string;
  deliverables: DeliverableSummary[];
  replanning_records: ReplanningRecord[];
  decision_records: DecisionRecord[];
  token_usage?: TokenUsageRecord;
  ended?: string;
  ran?: string;
}

export interface QuorumVote {
  role: string;
  model: string;
  confidence_percent: number;
}

export interface QuorumSupport {
  votes_count: number;
  total_votes: number;
  models_count: number;
  band: string;
  dissent_summary?: string;
  votes?: QuorumVote[];
}

export interface DecisionOption {
  key: string;
  label: string;
  tradeoff_description?: string;
  consequences?: string;
  preview_diff?: string;
  quorum_support?: QuorumSupport;
}

export interface DecisionPart {
  part_id: string;
  question: string;
  answer_type: string;
  options: DecisionOption[];
  recommendation?: string;
  recommendation_rationale?: string;
  recommending_role?: string;
  dependency?: string;
  allow_own_words?: boolean;
  allow_note?: boolean;
}

export interface DecisionEnvelope {
  id: string;
  revision: number;
  title: string;
  header?: string;
  subject: string;
  scope: string;
  asking_phase?: string;
  asking_role?: string;
  blocks: string;
  reversibility: string;
  description?: string;
  evidence?: Record<string, unknown>;
  parts: DecisionPart[];
  delivered_at?: string;
  is_waiver?: boolean;
  waiver_gate?: string;
  waiver_result?: string;
  waiver_reason?: string;
}

export interface DecisionAnswerPayload {
  part_answers?: Record<string, string[] | string>;
  option?: string;
  words?: string;
  note?: string;
  action?: string;
  reason?: string;
  channel?: string;
  device?: string;
}

export interface DecisionInterpretation {
  interpretation_type: string;
  mapped_option_key?: string;
  condition?: string;
  read_back_text?: string;
  clarification_question?: string;
}

const STORAGE_KEY_SECRET = 'pm_device_secret';
const STORAGE_KEY_DEVICE_ID = 'pm_device_id';
const STORAGE_KEY_LABEL = 'pm_device_label';

export class ApiClient {
  private readonly baseUrl: string;
  private onRevokedCallback?: (info?: string | RevokedEventData) => void;
  private onOfflineCallback?: () => void;

  constructor(baseUrl = '') {
    this.baseUrl = baseUrl;
  }

  public setOnRevoked(callback: (info?: string | RevokedEventData) => void): void {
    this.onRevokedCallback = callback;
  }

  public setOnOffline(callback: () => void): void {
    this.onOfflineCallback = callback;
  }

  public getSecret(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEY_SECRET);
  }

  public getLabel(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEY_LABEL);
  }

  public getDeviceId(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEY_DEVICE_ID);
  }

  public isPaired(): boolean {
    return Boolean(this.getSecret());
  }

  public clearAuth(): void {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(STORAGE_KEY_SECRET);
    localStorage.removeItem(STORAGE_KEY_DEVICE_ID);
    localStorage.removeItem(STORAGE_KEY_LABEL);
  }

  public async pair(code: string): Promise<PairingResponse> {
    const cleanCode = code.trim();
    const url = `${this.baseUrl}/api/v1/pair`;

    let res: Response;
    try {
      res = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ code: cleanCode }),
      });
    } catch {
      this.onOfflineCallback?.();
      throw new Error('network_offline');
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || 'invalid_code');
    }

    const data: PairingResponse = await res.json();
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_SECRET, data.device_secret);
      localStorage.setItem(STORAGE_KEY_DEVICE_ID, data.device_id);
      localStorage.setItem(STORAGE_KEY_LABEL, data.label);
    }

    return data;
  }

  public async getAvailableMessages(): Promise<{ locales: string[]; areas: string[] }> {
    return this.request<{ locales: string[]; areas: string[] }>('GET', '/api/v1/messages');
  }

  public async getMessages(locale: string, area = 'all'): Promise<Record<string, string>> {
    return this.request<Record<string, string>>('GET', `/api/v1/messages/${locale}/${area}`);
  }

  public async getStatus(): Promise<StatusResponse> {
    return this.request<StatusResponse>('GET', '/api/v1/status');
  }

  public async getDevices(): Promise<PairedDevice[]> {
    return this.request<PairedDevice[]>('GET', '/api/v1/web/devices');
  }

  public async getProjects(): Promise<ProjectOverviewResponse> {
    return this.request<ProjectOverviewResponse>('GET', '/api/v1/projects');
  }

  public async getProjectDetail(projectId: string): Promise<ProjectDetailResponse> {
    return this.request<ProjectDetailResponse>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}`,
    );
  }

  public async getEndedWork(projectId: string, page = 1): Promise<EndedWorkPage> {
    return this.request<EndedWorkPage>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}/ended?page=${page}`,
    );
  }

  public async renameDevice(deviceId: string, label: string): Promise<PairedDevice> {
    return this.request<PairedDevice>(
      'PUT',
      `/api/v1/web/devices/${encodeURIComponent(deviceId)}`,
      { label },
    );
  }

  public async revokeDevice(deviceId: string): Promise<void> {
    await this.request('DELETE', `/api/v1/web/devices/${encodeURIComponent(deviceId)}`);
  }

  public async revokeOtherDevices(): Promise<void> {
    await this.request('DELETE', '/api/v1/web/devices?others');
  }

  public async turnOffLanListener(): Promise<void> {
    try {
      await this.request('DELETE', '/api/v1/web/listeners/lan');
    } catch (err) {
      const msg = (err as Error)?.message;
      if (msg !== 'HTTP 404' && msg !== 'HTTP 405') throw err;
      await this.request('PUT', '/api/v1/web', { lan: false });
    }
  }

  public async getReports(older = false, project?: string): Promise<ReportsPage> {
    let path = '/api/v1/reports';
    const params = new URLSearchParams();
    if (older) params.set('older', 'true');
    if (project) params.set('project', project);
    const qs = params.toString();
    if (qs) path += `?${qs}`;
    const data = await this.request<
      ReportsPage | ReportItem[] | { reports?: ReportItem[]; unread_count?: number }
    >('GET', path);
    if (Array.isArray(data)) {
      return {
        items: data,
        unread_count: data.length,
        has_older: false,
        older_count: 0,
      };
    }
    const rawObj = data as {
      reports?: ReportItem[];
      items?: ReportItem[];
      unread_count?: number;
      oldest_unread?: string;
      oldest_unread_time?: string;
      has_older?: boolean;
      older_count?: number;
    };
    const items = rawObj.items || rawObj.reports || [];
    return {
      items,
      unread_count: rawObj.unread_count ?? items.length,
      oldest_unread_time: rawObj.oldest_unread_time || rawObj.oldest_unread,
      has_older: Boolean(rawObj.has_older || (rawObj.older_count && rawObj.older_count > 0)),
      older_count: rawObj.older_count || 0,
    };
  }

  public async markReportRead(reportId: string): Promise<void> {
    await this.request('POST', `/api/v1/reports/${encodeURIComponent(reportId)}/read`, {});
  }

  public async markAllReportsRead(ids?: string[]): Promise<{ marked_count: number }> {
    const body = ids && ids.length > 0 ? { report_ids: ids } : { all: true };
    return this.request<{ marked_count: number }>('POST', '/api/v1/reports/read', body);
  }

  public async getPlanCompletion(planId: string): Promise<PlanCompletionReport> {
    return this.request<PlanCompletionReport>(
      'GET',
      `/api/v1/plans/${encodeURIComponent(planId)}/completion`,
    );
  }

  public async getDecisions(params?: {
    project?: string;
    plan?: string;
  }): Promise<DecisionEnvelope[]> {
    let path = '/api/v1/decisions';
    const q = new URLSearchParams();
    if (params?.project) q.set('project', params.project);
    if (params?.plan) q.set('plan', params.plan);
    const qs = q.toString();
    if (qs) path += `?${qs}`;
    return this.request<DecisionEnvelope[]>('GET', path);
  }

  public async getDecision(id: string, plan?: string): Promise<DecisionEnvelope> {
    const safeQid = encodeURIComponent(id);
    const path = plan
      ? `/api/v1/plans/${encodeURIComponent(plan)}/decisions/${safeQid}`
      : `/api/v1/decisions/${safeQid}`;
    return this.request<DecisionEnvelope>('GET', path);
  }

  public async answerDecision(
    id: string,
    payload: DecisionAnswerPayload,
    plan?: string,
  ): Promise<Record<string, unknown>> {
    const safeQid = encodeURIComponent(id);
    const path = plan
      ? `/api/v1/plans/${encodeURIComponent(plan)}/decisions/${safeQid}/answer`
      : `/api/v1/decisions/${safeQid}/answer`;
    return this.request<Record<string, unknown>>('POST', path, payload);
  }

  public async interpretDecision(id: string, words: string): Promise<DecisionInterpretation> {
    const safeQid = encodeURIComponent(id);
    return this.request<DecisionInterpretation>('POST', `/api/v1/decisions/${safeQid}/interpret`, {
      words,
    });
  }

  public async withdrawDecision(id: string): Promise<Record<string, unknown>> {
    const safeQid = encodeURIComponent(id);
    return this.request<Record<string, unknown>>(
      'POST',
      `/api/v1/decisions/${safeQid}/withdraw`,
      {},
    );
  }

  public async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    if (
      !path.startsWith('/') ||
      path.startsWith('//') ||
      path.startsWith('/\\') ||
      /[\t\n\r]/.test(path)
    ) {
      throw new Error(`Invalid request path: ${path}`);
    }

    const secret = this.getSecret();
    const headers: Record<string, string> = {};

    if (secret) {
      headers['Authorization'] = `Bearer ${secret}`;
    }

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }

    let res: Response;
    try {
      res = await fetch(`${this.baseUrl}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch {
      this.onOfflineCallback?.();
      throw new Error('network_offline');
    }

    if (res.status === 401) {
      this.clearAuth();
      this.onRevokedCallback?.('unauthorized');
      throw new Error('unauthorized');
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `HTTP ${res.status}`);
    }

    return res.json() as Promise<T>;
  }

  public async followLink(
    link: HypermediaLink | string,
    body?: unknown,
  ): Promise<StateRepresentation> {
    const href = typeof link === 'string' ? link : link.href;
    const method = typeof link === 'string' ? 'POST' : link.method || 'POST';
    return this.request<StateRepresentation>(method, href, body);
  }

  public subscribeEvents(
    onEvent: (event: string, data: unknown) => void,
    signal?: AbortSignal,
  ): () => void {
    let active = true;
    let currentReader: ReadableStreamDefaultReader<Uint8Array> | undefined;
    const internalController = new AbortController();

    if (signal) {
      if (signal.aborted) {
        internalController.abort();
      } else {
        signal.addEventListener('abort', () => internalController.abort(), { once: true });
      }
    }

    const abortAll = () => {
      active = false;
      internalController.abort();
      if (currentReader) {
        void currentReader.cancel().catch(() => {});
        currentReader = undefined;
      }
    };

    const connect = async () => {
      const secret = this.getSecret();
      if (!secret || !active || internalController.signal.aborted) return;

      try {
        const res = await fetch(`${this.baseUrl}/api/v1/events`, {
          headers: {
            Authorization: `Bearer ${secret}`,
          },
          signal: internalController.signal,
        });

        if (res.status === 401) {
          this.clearAuth();
          this.onRevokedCallback?.('unauthorized');
          abortAll();
          return;
        }

        if (!res.ok || !res.body) {
          throw new Error(`SSE failed: ${res.status}`);
        }

        const reader = res.body.getReader();
        currentReader = reader;
        const decoder = new TextDecoder();
        let buffer = '';

        while (active && !internalController.signal.aborted) {
          const { done, value } = await reader.read();
          if (done) {
            currentReader = undefined;
            if (active && !internalController.signal.aborted) {
              setTimeout(() => {
                void connect();
              }, 2000);
            }
            return;
          }

          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split(/(?:\r\n|\r|\n){2}/);
          buffer = parts.pop() || '';

          for (const chunk of parts) {
            const parsed = parseSseChunk(chunk);
            if (!parsed) continue;

            const revoked = handleSseMessage(
              parsed.eventType,
              parsed.eventData,
              onEvent,
              (reason) => {
                this.clearAuth();
                this.onRevokedCallback?.(reason);
              },
            );

            if (revoked) {
              abortAll();
              return;
            }
          }
        }
      } catch (err: unknown) {
        currentReader = undefined;
        if ((err as Error)?.name === 'AbortError') return;
        if (active && !internalController.signal.aborted) {
          setTimeout(() => {
            void connect();
          }, 2000);
        }
      }
    };

    void connect();

    return () => {
      abortAll();
    };
  }
}

function parseSseChunk(chunk: string): { eventType: string; eventData: string } | null {
  const lines = chunk.split(/\r\n|\r|\n/);
  let eventType = 'message';
  let eventData = '';

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      eventData += line.slice(5).trim();
    }
  }

  return eventData ? { eventType, eventData } : null;
}

function handleSseMessage(
  eventType: string,
  eventData: string,
  onEvent: (event: string, data: unknown) => void,
  onRevoke: (info?: string | RevokedEventData) => void,
): boolean {
  try {
    const parsed = JSON.parse(eventData);
    onEvent(eventType, parsed);
    if (eventType === 'stream.closed') {
      if (parsed.reason === 'device_revoked' || parsed.reason === 'device_expired') {
        onRevoke(parsed);
        return true;
      }
    }
    if (eventType === 'web_device_revoked') {
      onRevoke(parsed.reason || parsed);
      return true;
    }
  } catch {
    onEvent(eventType, eventData);
  }
  return false;
}

export const apiClient = new ApiClient();
