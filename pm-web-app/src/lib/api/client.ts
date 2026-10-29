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

export interface ConnectionStatus {
  loopback_url?: string;
  loopback_open: boolean;
  lan_enabled: boolean;
  lan_url?: string;
  lan_fingerprint?: string;
  paired_devices_count: number;
  onboarded: boolean;
}

/**
 * The state of the shared event stream, for the live indicator (UX-P-12).
 *
 * `connecting` is the first attempt, `connected` a live stream, `reconnecting` a dropped stream
 * that retries, and `offline` the state after the retries gave up.
 */
export type EventStreamState = 'connecting' | 'connected' | 'reconnecting' | 'offline';

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

export type ProjectReadiness = 'ready' | 'setup_needs' | 'released';

export interface EnrolledProject {
  project_id: string;
  display_name: string;
  remote_url?: string | null;
  readiness: ProjectReadiness;
  setup_needs_summary?: string | null;
  needs_operator_count: number;
  active_plans_count: number;
  active_epics_count: number;
  last_active?: string | null;
  is_favourite: boolean;
  is_released: boolean;
}

export interface BeingAddedProject {
  checkout_path: string;
  current_step: string;
  step_number: number;
  total_steps: number;
  continue_link?: string | null;
  abandon_link?: string | null;
}

export interface ProjectExplorer {
  favourites: EnrolledProject[];
  recent: EnrolledProject[];
  all_projects: EnrolledProject[];
  being_added: BeingAddedProject[];
}

/** How far one step of a project's onboarding has come. */
export type ProjectOnboardingStepStatus =
  'done' | 'current' | 'waiting' | 'skipped' | 'needs_repair';

/** One step of a project's onboarding, as the runtime reports it. */
export interface ProjectOnboardingStep {
  /** The step's 1-based number, 1 to 10. */
  step: number;
  /** The catalogue suffix of the step's name, e.g. `identity`. */
  key?: string | null;
  /** The state that renders the step, e.g. `project.identity`. */
  state?: string | null;
  status: ProjectOnboardingStepStatus;
  summary?: string | null;
  reason?: string | null;
}

/**
 * The build environment of a project's jobs: the operator's login environment as the terminal
 * client captured it, never the runtime's own.
 */
export interface ProjectBuildEnvironment {
  /** When the terminal client last captured it. */
  captured_at?: string | null;
  /** Who captured it, `terminal` or `web`; absent on the runtime's own copy. */
  source?: string | null;
  /** The command that captures it, for a machine that has none yet. */
  command?: string | null;
  /** The variables, for the line that opens to them. */
  variables?: Record<string, string>;
}

/** One turn of the setup question chat. */
export interface ProjectChatTurn {
  role: string;
  text: string;
  at: string;
  untrusted: boolean;
}

/**
 * The representation of a project's onboarding (UC-PRJ-1).
 *
 * A run before its enrolment is addressed by its onboarding instance; after the enrolment the
 * same run is reached by its project id. The view reads one shape either way.
 */
export interface ProjectOnboardingState {
  project_id?: string | null;
  onboarding_id?: string | null;
  checkout_path?: string | null;
  /** The step the run waits at, 1 to 10. */
  step?: number | null;
  total_steps?: number | null;
  /** The state that renders the step, e.g. `project.identity`. */
  state?: string | null;
  summary?: string | null;
  steps?: ProjectOnboardingStep[];
  build_environment?: ProjectBuildEnvironment | null;
  /** Whether the setup question chat is offered, and why not where it is not. */
  chat_available?: boolean;
  chat_disabled_reason?: string | null;
  chat?: ProjectChatTurn[];
  /** A value changed in another client meanwhile. */
  conflict?: boolean;
  current_value?: string | null;
  wanted_value?: string | null;
  detected_domains?: string[];
  discovered_modules?: string[];
  credentials_needed?: string[];
  links?: Record<string, OnboardingLink | string>;
  /** What the state reports of its own, for a step without a screen here. */
  [key: string]: unknown;
}

/** One value of a project's own settings, with where it came from. */
export interface ProjectSettingsValue {
  aspect: string;
  value?: string | null;
  origin: string;
  choices?: string[];
}

/** A project's own settings, at one revision. */
export interface ProjectSettingsRecord {
  project_id: string;
  revision: number;
  values: ProjectSettingsValue[];
}

/** The outcome of saving one value of a project's settings. */
export interface ProjectSettingsUpdateResult {
  applied: boolean;
  conflict: boolean;
  revision: number;
  aspect?: string | null;
  current_value?: string | null;
}

/** One value the operator wanted to change while another client changed it first. */
export interface ProjectSettingsConflict {
  aspect: string;
  currentValue: string;
  wantedValue: string;
  revision: number;
}

export interface ProjectReleaseRequest {
  project_id: string;
  confirm_release: boolean;
  force?: boolean;
  reason?: string | null;
}

export interface ProjectReleaseResult {
  project_id: string;
  plans_ended_count: number;
  epics_ended_count: number;
  entries_deleted_count: number;
  released_at?: string | null;
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
  /** The open starts of the project, when the runtime reports them. */
  open_entries?: OpenEntrySummary[];
}

/** An open start, as the project's actions name it. */
export interface OpenEntrySummary {
  entry_id: string;
  kind: StartKind;
  source_count: number;
}

export interface ProcessTokens {
  total: number;
  input: number;
  output: number;
  floor: boolean;
  tasks_total: number;
  tasks_measured: number;
  none_yet: boolean;
  not_measured: boolean;
  /** The same figure by phase, when the runtime reports the breakdown. */
  by_phase?: TokenBreakdownItem[] | null;
  /** The same figure by role, when the runtime reports the breakdown. */
  by_role?: TokenBreakdownItem[] | null;
}

/** How an ended plan ended, when the process resource carries it. */
export interface ProcessEndedInfo {
  outcome?: string | null;
  reason?: string | null;
  ended_at?: string | null;
}

export interface OffTrackInfo {
  signal: string;
  action: string;
  value?: number | null;
  threshold?: number | null;
}

export interface LeafTask {
  task_id: string;
  role: string;
  harness: string;
  model: string;
  attempt: number;
  status: string;
  duration_seconds: number;
  tokens_so_far: number;
  activity_link?: string | null;
  off_track?: OffTrackInfo | null;
}

export interface LeafJob {
  job_id: string;
  kind: string;
  status: string;
  duration_seconds: number;
  tokens_so_far: number;
  log_link?: string | null;
}

export interface ProcessNode {
  node_id: string;
  title: string;
  kind: string;
  state: string;
  run_count: number;
  current_run: number;
  duration_seconds: number;
  prune_reason?: string | null;
  sub_nodes: ProcessNode[];
  leaf_tasks: LeafTask[];
  leaf_job?: LeafJob | null;
  waits_for?: string | null;
  links?: Record<string, string>;
}

export interface ProcessTree {
  plan_id: string;
  title: string;
  project: {
    project_id: string;
    display_name?: string;
    link?: string;
  };
  epic?: {
    epic_id: string;
    title?: string;
    link?: string;
  } | null;
  current_phase: string;
  phase_state: string;
  activity?: string | null;
  needs_operator: boolean;
  instance_version: number;
  tokens_so_far: ProcessTokens;
  nodes: ProcessNode[];
  current_position_path: string[];
  pull_request_number?: number | null;
  pull_request_url?: string | null;
  duration_seconds: number;
  /** How the plan ended, when the process resource carries it. */
  ended?: ProcessEndedInfo | null;
}

export interface ActivityEntry {
  timestamp: string;
  kind: string;
  tool_name?: string | null;
  tool_target?: string | null;
  tool_failed?: boolean;
  content?: string | null;
  size?: string | null;
  files?: string[];
  questions?: string[];
}

export interface TaskActivity {
  task_id: string;
  role: string;
  harness: string;
  model: string;
  attempt: number;
  duration_seconds: number;
  tokens_so_far: number;
  entries: ActivityEntry[];
  off_track?: OffTrackInfo | null;
}

export interface EpicProcessPlan {
  plan_id: string;
  title: string;
  current_phase: string;
  phase_state: string;
  tasks_done: number;
  tasks_total: number;
  running: boolean;
}

export interface EpicProcess {
  epic_id: string;
  project: string;
  title: string;
  state: string;
  running_plans: EpicProcessPlan[];
  staged_plans: EpicProcessPlan[];
  alongside_landings: string[];
  tokens_so_far?: ProcessTokens | null;
  review_ready: boolean;
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

export interface ActionAvailability {
  action: string;
  available: boolean;
  reason?: string | null;
}

export interface WorkActions {
  subject_id: string;
  subject_kind: string;
  state: string;
  pausing_point?: string | null;
  hold_kind?: string | null;
  epic_id?: string | null;
  /** Whether an epic starts its plans by itself, where the runtime reports it. */
  auto_enabled?: boolean | null;
  /** How many files a plan may share with a running one, where the runtime reports it. */
  overlap_max_files?: number | null;
  actions: ActionAvailability[];
}

/** One row of an epic's workstream, as the epic resource carries it. */
export interface EpicWorkRow {
  plan_id: string;
  title: string;
  status: string;
  wait_reason?: string | null;
  wait_plan_id?: string | null;
  overlap_files?: string[];
  pull_request_number?: number | null;
  pull_request_url?: string | null;
  change?: string | null;
  objective?: string | null;
  deliverables?: string[];
  surface?: string[];
  dependencies?: string[];
}

/** A workstream of an epic, with its rows. */
export interface EpicWorkstream {
  id: string;
  name: string;
  open: boolean;
  rows: EpicWorkRow[];
}

/** How many findings or watches an epic carries, by their state. */
export interface RegisterSummary {
  new: number;
  open: number;
  kept: number;
}

/** One token figure: a plan's, an epic's own tasks', or the epic's total. */
export interface TokenFigure {
  total: number;
  input: number;
  output: number;
  measured: boolean;
  floor: boolean;
  measured_tasks: number;
  not_measured_tasks: number;
}

/** One plan's share of an epic's tokens. */
export interface PlanTokenFigure {
  plan_id: string;
  figure: TokenFigure;
}

/** The epic's tokens: its plans' sum and its own tasks, with the per-plan detail. */
export interface EpicTokens {
  total: TokenFigure;
  own: TokenFigure;
  plans: PlanTokenFigure[];
  not_measured: boolean;
  not_measured_count: number;
}

/** One event of the epic's history. */
export interface EpicEvent {
  at: string;
  text: string;
}

/** The epic as a resource (UC-PLN-5). */
export interface EpicResource {
  epic_id: string;
  project_id: string;
  display_name: string;
  link?: string | null;
  state: string;
  auto_start: boolean;
  overlap_max_files: number;
  scope: number;
  running: number;
  terminal_reason?: string | null;
  workstreams: EpicWorkstream[];
  sources: string[];
  landings: number;
  findings: RegisterSummary;
  watches: RegisterSummary;
  tokens: EpicTokens;
  events: EpicEvent[];
}

/** One line of the archive check. */
export interface ArchiveCheckLine {
  label: string;
  passed: boolean;
  note?: string | null;
  action?: string | null;
}

/** Whether an epic is through, with the lines the runtime checked. */
export interface ArchiveCheck {
  epic_id: string;
  lines: ArchiveCheckLine[];
  carried_findings: number;
  carried_watches: number;
  completion_review_first: boolean;
  passed: boolean;
}

/** One item of a findings or watches review. */
export interface ReviewItem {
  item_id: string;
  register: string;
  statement: string;
  rules: string[];
  origin: string;
  trust: string;
  state: string;
  proposal?: string | null;
}

/** A plan the planning role drafts for a promoted item. */
export interface DraftedPlan {
  plan_id: string;
  objective?: string | null;
  deliverables: string[];
  surface: string[];
  dependencies: string[];
}

/** The old against the new of an edit or a redraft. */
export interface ReviewReadBack {
  kind: string;
  before?: string | null;
  after?: string | null;
  plan?: DraftedPlan | null;
}

/** The review of one register of an epic. */
export interface ReviewResource {
  review_id: string;
  epic_id: string;
  register: string;
  items: ReviewItem[];
  decided: number;
  total: number;
  state: string;
  drafted_plan?: DraftedPlan | null;
  read_back?: ReviewReadBack | null;
}

/** One chat of the planning role, about a plan, an epic, a decision, or a register. */
export interface ChatReadBack {
  chat_id: string;
  subject_id: string;
  subject_kind: string;
  role: string;
  scope: string;
  state: string;
  turns: ChatTurn[];
  proposal?: ChatProposal | null;
}

export interface HoldRecord {
  hold_id: string;
  subject_id: string;
  subject_kind: string;
  kind: string;
  cause: string;
  attempts: number;
  last_tried_at?: string | null;
  file_path?: string | null;
  recorded_size?: number | null;
  recorded_digest?: string | null;
  current_size?: number | null;
  current_digest?: string | null;
  acts: string[];
  pull_request_url?: string | null;
}

export interface HoldResolutionResult {
  hold_id: string;
  released: boolean;
  cause?: string | null;
  subject: string;
}

export interface PlanSettingsValue {
  aspect: string;
  value?: string | null;
  origin: string;
  choices?: string[];
}

export interface PlanSettingsRecord {
  plan_id: string;
  revision: number;
  values: PlanSettingsValue[];
}

export interface PlanSettingsUpdateResult {
  applied: boolean;
  conflict: boolean;
  revision: number;
  current_value?: string | null;
  state?: string | null;
}

export interface ReportFrom {
  project?: string;
  epic?: string;
  plan?: string;
  project_title?: string;
  epic_title?: string;
  plan_title?: string;
  /** The phase the report names, where the wire carries it. */
  phase?: string;
  /** The role the report names, where the wire carries it. */
  role?: string;
  /** The harness the report names, where the wire carries it. */
  harness?: string;
  /** The model the report names, where the wire carries it. */
  model?: string;
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
  /** The kind of the subject the report is about, where the runtime names it. */
  subject_kind?: string;
  /** Whether the cleanup has deleted the report's subject since it was written. */
  subject_deleted?: boolean;
  /** The report's own headline, where the wire carries one beside the subject. */
  title?: string;
  /** The state the report names, where the wire carries one. */
  status?: string;
}

/**
 * Normalises the origin of a report from either wire spelling.
 *
 * The runtime writes the identifiers as `project_id`, `epic_id`, and `plan_id`; older fixtures
 * and the browser's own tests use `project`, `epic`, and `plan`. The views read the short names.
 *
 * @param raw the origin as the runtime sent it
 * @returns the same origin under the short names
 */
function normaliseReportFrom(raw: unknown): ReportFrom {
  const from = (raw ?? {}) as Record<string, unknown>;
  const text = (key: string, fallbackKey?: string): string | undefined => {
    const value = from[key] ?? (fallbackKey ? from[fallbackKey] : undefined);
    return typeof value === 'string' && value.length > 0 ? value : undefined;
  };
  return {
    project: text('project', 'project_id'),
    epic: text('epic', 'epic_id'),
    plan: text('plan', 'plan_id'),
    project_title: text('project_title'),
    epic_title: text('epic_title'),
    plan_title: text('plan_title'),
    phase: text('phase'),
    role: text('role'),
    harness: text('harness'),
    model: text('model'),
  };
}

/**
 * Normalises a token breakdown line from the runtime's wire names.
 *
 * The record names the line `label` and its figures `tokens`, `input_tokens`, and
 * `output_tokens`; the completion report renders them as `name`, `total`, `input`, and
 * `output`.
 *
 * @param raw the line as the runtime sent it
 * @returns the line under the names the views read
 */
function normaliseTokenBreakdown(raw: unknown): TokenBreakdownItem {
  const line = (raw ?? {}) as Record<string, unknown>;
  const number = (key: string, fallbackKey?: string): number => {
    const value = line[key] ?? (fallbackKey ? line[fallbackKey] : undefined);
    return typeof value === 'number' ? value : 0;
  };
  const text = (key: string, fallbackKey?: string): string | undefined => {
    const value = line[key] ?? (fallbackKey ? line[fallbackKey] : undefined);
    return typeof value === 'string' ? value : undefined;
  };
  return {
    name: text('name', 'label') ?? '',
    total: number('total', 'tokens'),
    input: number('input', 'input_tokens'),
    output: number('output', 'output_tokens'),
    unmeasured: number('unmeasured', 'unmeasured_tasks'),
    floor: line['floor'] === true || line['is_floor'] === true,
    harness_model: text('harness_model'),
    effort: text('effort'),
    components: (line['components'] ?? line['details']) as TokenComponentRecord | undefined,
  };
}

/**
 * Normalises the completion report's token usage from the runtime's wire names.
 *
 * @param raw the token usage as the runtime sent it
 * @returns the usage under the names the views read
 */
function normaliseTokenUsage(raw: unknown): TokenUsageRecord | undefined {
  if (!raw || typeof raw !== 'object') return undefined;
  const usage = raw as Record<string, unknown>;
  const number = (key: string, fallbackKey?: string): number => {
    const value = usage[key] ?? (fallbackKey ? usage[fallbackKey] : undefined);
    return typeof value === 'number' ? value : 0;
  };
  const lines = (key: string): TokenBreakdownItem[] =>
    Array.isArray(usage[key]) ? (usage[key] as unknown[]).map(normaliseTokenBreakdown) : [];
  return {
    total: number('total', 'total_tokens'),
    input: number('input', 'input_tokens'),
    output: number('output', 'output_tokens'),
    duration_seconds: number('duration_seconds'),
    workers_count: number('workers_count'),
    models_count: number('models_count'),
    tasks_measured: number('tasks_measured'),
    tasks_total: number('tasks_total'),
    is_floor: usage['is_floor'] === true,
    unmeasured_reason:
      typeof usage['unmeasured_reason'] === 'string' ? usage['unmeasured_reason'] : undefined,
    by_phase: lines('by_phase'),
    by_role: lines('by_role'),
    by_model: lines('by_model'),
    waste_records: Array.isArray(usage['waste_records'])
      ? (usage['waste_records'] as TokenWasteRecord[])
      : [],
    total_waste_tokens: number('total_waste_tokens'),
    waste_share_percent: number('waste_share_percent'),
  };
}

/**
 * Normalises a report as the runtime sends it, including the origin spelling.
 *
 * @param raw the report as the runtime sent it
 * @returns the report with its origin under the short names
 */
export function normaliseReportItem(raw: ReportItem): ReportItem {
  return {
    ...raw,
    from: normaliseReportFrom(raw.from),
    subject_deleted:
      raw.subject_deleted === true ||
      (raw as ReportItem & { detail_disabled?: boolean }).detail_disabled === true,
  };
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
  own_calls?: OwnCallRecord[];
  change_map?: ChangeMapRecord;
  token_usage?: TokenUsageRecord;
  ended?: string;
  ran?: string;
}

export interface OwnCallRecord {
  kind: string;
  description: string;
  limit?: string;
  ends_when?: string;
  judgment: string;
  conflict_target?: string;
}

export interface ChangeMapRecord {
  diagram: string;
  state: string;
  cause?: string;
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
  /** What changed in a later revision, where the runtime reports it. */
  change?: string;
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

export interface RuntimeMetrics {
  version: string;
  build_date?: string;
  git_commit?: string;
  uptime_seconds?: number;
  uptime_formatted?: string;
  memory_used_bytes?: number;
  memory_max_bytes?: number;
  memory_resident_bytes?: number;
  cpu_percent?: number | null;
  cpu_measured?: boolean;
  machine_store_bytes?: number;
  disk_free_bytes?: number;
  disk_measured_at?: string;
  socket_listener?: boolean;
  loopback_address?: string | null;
  lan_address?: string | null;
  lan_devices_count?: number;
  relays_behind_count?: number;
  relays_behind_version?: string | null;
  upgrade_available_version?: string | null;
  upgrade_available_date?: string | null;
  upgrade_checked_at?: string | null;
  upgrade_release_notes_url?: string | null;
  upgrade_possible?: boolean;
  upgrade_disabled_reason?: string | null;
  /** The worktrees holding uncommitted changes, where the runtime reports them. */
  dirty_worktrees?: number | null;
  /** A one-line summary of the newer release's notes, where the runtime reports one. */
  upgrade_release_notes_summary?: string | null;
}

export interface WorkMetrics {
  plans_by_state?: Record<string, number>;
  epics_by_state?: Record<string, number>;
  tasks_running?: number;
  tasks_queued?: number;
  active_projects_count?: number;
  recovering_projects?: string[];
}

export interface WorkerMetric {
  role: string;
  harness: string;
  model: string;
  state: string;
  current_plan_title?: string | null;
}

export interface ModelMetric {
  id: string;
  role?: string;
  calls_count: number;
  tokens_total: number;
  tokens_in: number;
  tokens_out: number;
  errors_count: number;
  retries_count: number;
  tokens_unmeasured?: boolean;
  unmeasured_calls?: number;
}

export interface CleanupSummary {
  plans_count: number;
  epics_count: number;
  logs_count: number;
  read_reports_count?: number;
  unenrolled_projects_count?: number;
  freed_bytes: number;
  kept_for_good_count: number;
  last_sweep_at?: string;
  next_check_at?: string;
}

/** The windows the system view offers. */
export type SystemWindow = 'hour' | 'today' | '7d';

export interface SystemSnapshot {
  runtime: RuntimeMetrics;
  work: WorkMetrics;
  workers: WorkerMetric[];
  jobs: {
    build?: { slots_used: number; slots_total: number; queued?: number };
    test?: { slots_used: number; slots_total: number; queued?: number };
    locks_held?: number;
  };
  models: ModelMetric[];
  cleanup: CleanupSummary;
  window: SystemWindow;
  project?: string | null;
}

export interface CleanupItemEntry {
  id: string;
  kind: 'plan' | 'epic' | 'logs' | 'report' | 'unenrolled' | string;
  title: string;
  ended_at?: string;
  freed_bytes: number;
  detail?: string;
}

export interface CleanupProjectItem {
  project_id: string;
  display_name: string;
  items: CleanupItemEntry[];
}

export interface CleanupKeptItem {
  item_id: string;
  kind: string;
  title: string;
  why: 'ignore' | 'exclude' | string;
  kept_at: string;
  who: string;
}

/** The kept list as the runtime sends it: a bare array or an `items` wrapper. */
type CleanupKeptResponse =
  CleanupKeptItem[] | { items?: CleanupKeptItem[] } | Record<string, unknown>[];

export interface CleanupResidue {
  step: string;
  last_sweep_at?: string;
  items_removed?: number;
}

export interface CleanupDurations {
  plans_days: number;
  epics_days: number;
  logs_days: number;
  reports_days: number;
  unenrolled_days: number;
  remind_days: number;
}

export interface CleanupJob {
  job_id: string;
  due_since?: string;
  summary: CleanupSummary;
  projects: CleanupProjectItem[];
  kept: CleanupKeptItem[];
  residue: CleanupResidue[];
  durations: CleanupDurations;
}

export interface MachineSettingsUpdateResult {
  applied: boolean;
  conflict: boolean;
  revision: number;
  current_value?: string;
}

export interface OnboardingLink {
  href: string;
  method?: string;
  next?: string;
}

export interface OnboardingState {
  scope?: string;
  state?: string;
  summary?: string;
  completed_steps?: number;
  auto?: boolean;
  links?: Record<string, OnboardingLink>;
  /** The recorded summary of each done step, keyed by step number (1 to 7). */
  step_summaries?: Record<string, string>;
  /** The enrolled harnesses, with the name the model screens group by. */
  harnesses?: Array<Record<string, unknown>>;
  [key: string]: unknown;
}

export interface MachineSettingsAspectSummary {
  aspect: string;
  step: number;
  summary: string;
  current?: string;
}

export interface ConsentRecord {
  kind: string;
  status: string;
  remember: boolean;
  granted_at?: string;
  channel?: string;
  project?: string;
  script_count?: number;
}

export interface MachineCredentialStatus {
  key: string;
  provider: string;
  kind: string;
  expires_in_days?: number;
  expired: boolean;
  held_plan?: string;
}

export interface PlanDefaults {
  landing: string;
  method: string;
  asking: string;
  posture?: string;
}

export interface MachineSettingsRecord {
  revision: number;
  aspects: MachineSettingsAspectSummary[];
  language_interface: string;
  language_model: string;
  documents_mode: string;
  cleanup_durations: Record<string, number>;
  plan_defaults: PlanDefaults;
  consents: ConsentRecord[];
  credentials: MachineCredentialStatus[];
}

export type BlockKind =
  | 'keyword'
  | 'primitive'
  | 'guard'
  | 'link_relation'
  | 'condition'
  | 'sub_workflow'
  | 'role'
  | 'parameter';

export interface BuildingBlock {
  kind: BlockKind;
  name: string;
  summary: string;
  template: string;
  parameters: string[];
  constraints: string[];
  outcomes: string[];
  used_in: string[];
  compiled_only: boolean;
}

export interface WorkflowUnitLevel {
  level: string;
  text: string;
  revision: number;
  digest: string;
}

export interface WorkflowFrontmatter {
  display_name: string;
  description: string;
  type: string;
  icon: string;
  entries: string[];
  exits: string[];
  author: string;
  release: string;
  signed: boolean;
}

export interface WorkflowUnit {
  name: string;
  group: string;
  level_in_force: string;
  frontmatter?: WorkflowFrontmatter | null;
  levels: WorkflowUnitLevel[];
}

export interface WorkflowCatalogue {
  scope: string;
  runtime_version: string;
  digest: string;
  blocks: BuildingBlock[];
}

export interface WorkflowFinding {
  stage: string;
  rule: string;
  line: number;
  column: number;
  severity: string;
  message: string;
  project?: string | null;
}

export interface WorkflowStage {
  name: string;
  status: string;
}

export interface WorkflowStructureState {
  id: string;
  kind: string;
  outcomes: string[];
  calls: string[];
}

export interface WorkflowStructure {
  display_name: string;
  entries: string[];
  exits: string[];
  states: WorkflowStructureState[];
}

export interface WorkflowValidationResult {
  valid: boolean;
  stages: WorkflowStage[];
  findings: WorkflowFinding[];
  structure?: WorkflowStructure | null;
  composed?: ProcessTree | null;
}

export interface UnitPlacingResult {
  unit: string;
  target: string;
  status: string;
  steps: string[];
  catalogue_digest?: string;
  author?: string;
  release?: string;
  digest?: string;
  reason?: string;
}

export interface ValidateWorkflowPayload {
  text: string;
  unit_name?: string | null;
  unit_kind?: string | null;
  project?: string | null;
  digest?: string | null;
}

export interface PlaceWorkflowPayload {
  text: string;
  unit_name?: string | null;
  unit_kind?: string | null;
  author?: string | null;
  release?: string | null;
  digest?: string | null;
}

const STORAGE_KEY_SECRET = 'pm_device_secret';
const STORAGE_KEY_DEVICE_ID = 'pm_device_id';
const STORAGE_KEY_LABEL = 'pm_device_label';

export interface StartPlanRequest {
  project_id: string;
  objective?: string;
  recipe_id?: string;
  parameters?: Record<string, string>;
  documents?: string[];
  issue?: string;
  title?: string;
}

export interface StartEpicRequest {
  project_id: string;
  objective?: string;
  auto_start?: boolean;
  title?: string;
}

export interface PlanStartResult {
  id: string;
  kind: string;
  title: string;
  state: string;
  follow_href?: string;
  project_id?: string;
  recipe_id?: string;
  auto_start?: boolean;
}

export type StartKind = 'plan' | 'epic';

export type SourceState =
  | 'checking'
  | 'fetching'
  | 'screening'
  | 'stored'
  | 'copied'
  | 'moved'
  | 'flagged'
  | 'unscreened'
  | 'failed';

export type SourceTrust = 'trusted' | 'untrusted';

export type ScreeningOutcome = 'clean' | 'flagged' | 'unscreened';

export type UnscreenedDisposition = 'try_again' | 'remove' | 'accept_untrusted';

export interface StartSourceFailure {
  cause: string;
  detail?: string | null;
  /** Epoch seconds, present exactly for a rate limit. */
  resets_at?: number | null;
}

export interface ScreeningSpan {
  start: number;
  end: number;
  text: string;
  reason: string;
}

export interface StartSource {
  source_id: string;
  kind: string;
  reference: string;
  title?: string | null;
  state: SourceState;
  trust: SourceTrust;
  verdict?: ScreeningOutcome | null;
  failure?: StartSourceFailure | null;
  spans?: ScreeningSpan[];
  disposition?: UnscreenedDisposition | null;
  disposition_reason?: string | null;
  changed_files?: ChangedFile[];
  digest?: string | null;
}

export interface SourceKindOffer {
  kind: string;
  available: boolean;
  reason_key?: string | null;
  reason_args?: Record<string, string | number>;
}

export interface AdoptableEpic {
  epic_id: string;
  title: string;
}

export type AdoptionFailureReason = 'ledger_unreadable' | 'ledger_invalid' | 'ledger_terminal';

export interface AdoptionFailure {
  epic_id: string;
  reason: AdoptionFailureReason;
  file?: string | null;
  detail?: string | null;
}

export interface StartEntryResource {
  entry_id: string;
  kind: StartKind;
  project_id: string;
  /** One of the `start.*` states, e.g. `start.sources` or `start.ready`. */
  state: string;
  title?: string | null;
  plan_id?: string | null;
  sources: StartSource[];
  kind_offers: SourceKindOffer[];
  can_continue: boolean;
  chat_available: boolean;
  chat_disabled_reason?: string | null;
  recipe?: RecipeRef | null;
  adoptable_epics?: AdoptableEpic[];
  adoption_failure?: AdoptionFailure | null;
}

export interface StartReadyResource {
  title?: string | null;
  plan_id: string;
  id_editable: boolean;
  id_from_title: boolean;
  recipe?: RecipeRef | null;
  parameters: RecipeParameter[];
  landing?: string | null;
  landing_asked: boolean;
  epic_auto_start?: boolean | null;
  epic_auto_start_origin?: string | null;
  parallelization_scope?: number | null;
  overlap_max_files?: number | null;
}

export interface ChatTurn {
  role: string;
  text: string;
  at: string;
  untrusted: boolean;
}

export interface ChatProposal {
  summary: string;
  before?: string | null;
  after: string;
  applies?: string | null;
}

export interface StartChatResource {
  entry_id: string;
  role: string;
  scope: string;
  available: boolean;
  disabled_reason_key?: string | null;
  chat_id?: string | null;
  state?: string | null;
  turns: ChatTurn[];
  proposal?: ChatProposal | null;
  transcript: string;
  transcript_path?: string | null;
}

export interface StartRefusal {
  reason: 'machine_not_onboarded' | 'project_not_ready';
  project_id?: string | null;
  need?: string | null;
  action_key: string;
  action_href: string;
}

/** The `start` member of the entry's representation: every screen of the start. */
export interface StartState {
  entry?: StartEntryResource;
  ready?: StartReadyResource;
  chat?: StartChatResource;
  refusal?: StartRefusal;
  adoption_failure?: AdoptionFailure;
  recipes?: RecipeRef[];
}

export interface StartStateResource {
  scope?: unknown;
  state?: unknown;
  links?: unknown;
  start: StartState;
}

export interface EntryOpenRequest {
  kind: StartKind;
  objective?: string;
  title?: string;
  recipe_key?: string;
  from_epic?: string;
}

export interface ConfinedFile {
  name: string;
  path: string;
  directory: boolean;
  size: number;
  readable: boolean;
  refusal?: StartSourceFailure | null;
}

export interface ConfinedFileList {
  project_id: string;
  root: 'checkout' | 'home';
  path: string;
  parent?: string | null;
  entries: ConfinedFile[];
  truncated: boolean;
}

export interface FollowUpProposal {
  subject_id: string;
  subject_kind: StartKind;
  role: string;
  scope: string;
  available: boolean;
  disabled_reason_key?: string | null;
  proposal?: ChatProposal | null;
}

export interface RecipeParameter {
  name: string;
  value?: string;
  options: string[];
  proposed: boolean;
  available: boolean;
  reason?: string;
}

export interface RecipeRef {
  key: string;
  name: string;
  description: string;
  origin: string;
  takes_request: boolean;
  parameters: RecipeParameter[];
}

export interface SourceInput {
  kind: string;
  location: string;
  title?: string;
}

export interface IntakeDraft {
  id: string;
  project_id: string;
  prompt?: string;
  sources: SourceInput[];
  created_at: string;
}

export interface StagedPlanRecord {
  plan_id: string;
  title: string;
  objective?: string | null;
  expected_surface: string[];
  dependencies: string[];
  status: string;
}

export interface WorkstreamRecord {
  id: string;
  name: string;
  description?: string | null;
  order: number;
  staged_plans: StagedPlanRecord[];
}

export interface DecompositionCheckFinding {
  severity: 'info' | 'warning' | 'error';
  rule: string;
  affected_plans: string[];
  message: string;
}

export interface EpicDecomposition {
  epic_id: string;
  status: string;
  revision: number;
  workstreams: WorkstreamRecord[];
  findings: DecompositionCheckFinding[];
}

export interface DecompositionInstructRequest {
  plan_id?: string | null;
  instruction: string;
  action: 'edit' | 'add' | 'rewrite';
}

export interface DecompositionCommitResult {
  status: string;
  ledger_revision: number;
  launchable_plan_count: number;
}

export interface UnclaimedLessonSummary {
  lesson_id: string;
  title: string;
  summary: string;
  recorded_at: string;
  citing_plan_id?: string;
  symbols_cited: string[];
}

export interface LessonPremiseEvidence {
  kind: string;
  name: string;
  expected_path?: string;
  last_verified_commit?: string;
}

export interface LessonPremiseCheckResult {
  lesson_id: string;
  status: string;
  evidence: LessonPremiseEvidence[];
  cause?: string;
}

export interface LessonClaimResult {
  lesson_id: string;
  status: string;
  plan_id?: string;
}

export interface LessonRetirementNotice {
  lesson_id: string;
  retired: boolean;
  reason?: string;
}

export interface ChangedFile {
  path: string;
  additions: number;
  deletions: number;
  untrusted: boolean;
  operator_authored: boolean;
}

export interface RateLimitStatus {
  limit: number;
  remaining: number;
  reset_at?: string;
  limited: boolean;
}

export interface PullRequestObservation {
  url: string;
  number: number;
  title: string;
  author: string;
  branch: string;
  state: string;
  writable: boolean;
  changed_files: ChangedFile[];
  screening: ScreeningOutcome;
  rate_limit: RateLimitStatus;
}

export interface PrAdoptRequest {
  project_id: string;
  pr_url: string;
  screening_bypass_reason?: string;
}

export interface PrAdoptResult {
  plan_id: string;
  lane: string;
  branch: string;
  status: string;
}

export interface LocalChangesPreview {
  status: string;
  preview_id?: string;
  branch?: string;
  changed_files: ChangedFile[];
  landing?: string;
  landing_commits: boolean;
  eligible: boolean;
  reason?: string;
}

export interface LocalChangesCaptureResult {
  plan_id: string;
  lane: string;
  snapshot_commit: string;
  status: string;
}

/**
 * Normalises the runtime metrics from the runtime's wire names.
 *
 * The snapshot names the release fields `newer_version` and `upgrade_supported`, and the memory
 * fields `heap_*`; the view reads the upgrade names it offers and the memory names it shows.
 *
 * @param raw the runtime metrics as the runtime sent them
 * @returns the metrics under the names the view reads
 */
function normaliseRuntimeMetrics(raw: unknown): RuntimeMetrics {
  const runtime = (raw ?? {}) as Record<string, unknown>;
  const str = (key: string, fallbackKey?: string): string | undefined => {
    const value = runtime[key] ?? (fallbackKey ? runtime[fallbackKey] : undefined);
    return typeof value === 'string' ? value : undefined;
  };
  const num = (key: string, fallbackKey?: string): number | undefined => {
    const value = runtime[key] ?? (fallbackKey ? runtime[fallbackKey] : undefined);
    return typeof value === 'number' ? value : undefined;
  };
  const returned = (key: string): string | null => {
    const value = str(key);
    return value && value !== 'off' ? value : null;
  };
  const cpu = num('cpu_percent');
  return {
    version: str('version') ?? '',
    build_date: str('build_date', 'build'),
    git_commit: str('git_commit'),
    uptime_seconds: num('uptime_seconds'),
    uptime_formatted: str('uptime_formatted', 'uptime'),
    memory_used_bytes: num('memory_used_bytes', 'heap_used_bytes'),
    memory_max_bytes: num('memory_max_bytes', 'heap_max_bytes'),
    memory_resident_bytes: num('memory_resident_bytes', 'resident_memory_bytes'),
    cpu_percent: cpu ?? null,
    cpu_measured: runtime['cpu_measured'] === true || cpu !== undefined,
    machine_store_bytes: num('machine_store_bytes'),
    disk_free_bytes: num('disk_free_bytes'),
    disk_measured_at: str('disk_measured_at', 'measured_at'),
    socket_listener: runtime['socket_listener'] === true,
    loopback_address: returned('loopback_address') ?? returned('loopback_listener'),
    lan_address: returned('lan_address') ?? returned('network_listener'),
    lan_devices_count: num('lan_devices_count', 'paired_devices_count'),
    relays_behind_count: num('relays_behind_count'),
    relays_behind_version: str('relays_behind_version'),
    upgrade_available_version: str('upgrade_available_version', 'newer_version'),
    upgrade_available_date: str('upgrade_available_date', 'newer_version_date'),
    upgrade_checked_at: str('upgrade_checked_at', 'newer_version_checked_at'),
    upgrade_release_notes_url: str('upgrade_release_notes_url', 'release_notes_link'),
    upgrade_possible: runtime['upgrade_possible'] === true || runtime['upgrade_supported'] === true,
    upgrade_disabled_reason: str('upgrade_disabled_reason', 'upgrade_unsupported_reason'),
    dirty_worktrees: num('dirty_worktrees'),
    upgrade_release_notes_summary: str('upgrade_release_notes_summary'),
  };
}

/**
 * @param model a model metric as the runtime sent it
 * @returns its id, from the short name or the harness-and-model pair
 */
function modelIdOf(model: Record<string, unknown>): string {
  if (typeof model['id'] === 'string') return model['id'];
  return typeof model['harness_and_model'] === 'string' ? model['harness_and_model'] : '';
}

/**
 * Normalises a model's figures from the runtime's wire names.
 *
 * @param raw the model metric as the runtime sent it
 * @returns the metric under the names the table reads
 */
function normaliseModelMetric(raw: unknown): ModelMetric {
  const model = (raw ?? {}) as Record<string, unknown>;
  const num = (key: string): number =>
    typeof model[key] === 'number' ? (model[key] as number) : 0;
  return {
    id: modelIdOf(model),
    role: typeof model['role'] === 'string' ? model['role'] : undefined,
    calls_count: num('calls_count'),
    tokens_total: num('tokens_total'),
    tokens_in: num('tokens_in'),
    tokens_out: num('tokens_out'),
    errors_count: num('errors_count'),
    retries_count: num('retries_count'),
    unmeasured_calls: num('unmeasured_calls'),
    tokens_unmeasured: model['tokens_unmeasured'] === true || model['is_floor'] === true,
  };
}

/**
 * Normalises the cleanup summary from the runtime's wire names.
 *
 * @param raw the summary as the runtime sent it
 * @returns the summary under the names the views read
 */
function normaliseCleanupSummary(raw: unknown): CleanupSummary {
  const cleanup = (raw ?? {}) as Record<string, unknown>;
  const num = (key: string, fallbackKey?: string): number => {
    const value = cleanup[key] ?? (fallbackKey ? cleanup[fallbackKey] : undefined);
    return typeof value === 'number' ? value : 0;
  };
  const str = (key: string, fallbackKey?: string): string | undefined => {
    const value = cleanup[key] ?? (fallbackKey ? cleanup[fallbackKey] : undefined);
    return typeof value === 'string' ? value : undefined;
  };
  return {
    plans_count: num('plans_count'),
    epics_count: num('epics_count'),
    logs_count: num('logs_count'),
    read_reports_count: num('read_reports_count', 'reports_count'),
    unenrolled_projects_count: num('unenrolled_projects_count', 'projects_count'),
    freed_bytes: num('freed_bytes'),
    kept_for_good_count: num('kept_for_good_count'),
    last_sweep_at: str('last_sweep_at'),
    next_check_at: str('next_check_at'),
  };
}

/**
 * Normalises the system snapshot from the runtime's wire names.
 *
 * @param raw the snapshot as the runtime sent it
 * @returns the snapshot under the names the system view reads
 */
function normaliseSystemSnapshot(raw: SystemSnapshot): SystemSnapshot {
  const snapshot = (raw ?? {}) as unknown as Record<string, unknown>;
  return {
    runtime: normaliseRuntimeMetrics(snapshot['runtime']),
    work: (snapshot['work'] ?? {}) as WorkMetrics,
    workers: (Array.isArray(snapshot['workers']) ? snapshot['workers'] : []) as WorkerMetric[],
    jobs: (snapshot['jobs'] ?? {}) as SystemSnapshot['jobs'],
    models: (Array.isArray(snapshot['models']) ? snapshot['models'] : []).map(normaliseModelMetric),
    cleanup: normaliseCleanupSummary(snapshot['cleanup']),
    window: (snapshot['window'] ?? 'hour') as SystemWindow,
    project: typeof snapshot['project'] === 'string' ? snapshot['project'] : null,
  };
}

/**
 * Normalises a cleanup item from the runtime's wire names.
 *
 * @param raw the item as the runtime sent it
 * @returns the item under the names the cleanup view reads
 */
function normaliseCleanupItem(raw: unknown): CleanupItemEntry {
  const item = (raw ?? {}) as Record<string, unknown>;
  const text = (key: string): string | undefined =>
    typeof item[key] === 'string' ? (item[key] as string) : undefined;
  const number = (key: string, fallbackKey?: string): number => {
    const value = item[key] ?? (fallbackKey ? item[fallbackKey] : undefined);
    return typeof value === 'number' ? value : 0;
  };
  return {
    id: text('id') ?? '',
    kind: text('kind') ?? '',
    title: text('title') ?? text('id') ?? '',
    ended_at: text('ended_at'),
    freed_bytes: number('freed_bytes', 'size_bytes'),
    detail: text('detail') ?? text('outcome') ?? text('kept_until') ?? undefined,
  };
}

/**
 * @param value a value as the runtime sent it
 * @returns the value where it is text, else an empty string
 */
function textOf(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

/**
 * Normalises a kept cleanup item from the runtime's wire names.
 *
 * @param raw the item as the runtime sent it
 * @returns the item under the names the cleanup view reads
 */
function normaliseKeptItem(raw: unknown): CleanupKeptItem {
  const item = (raw ?? {}) as Record<string, unknown>;
  return {
    item_id: textOf(item['item_id']) || textOf(item['id']),
    kind: textOf(item['kind']),
    title: textOf(item['title']),
    why: typeof item['why'] === 'string' ? item['why'] : textOf(item['how']),
    kept_at: textOf(item['kept_at']),
    who: textOf(item['who']),
  };
}

/**
 * Normalises a cleanup project from the runtime's wire names.
 *
 * @param raw the project as the runtime sent it
 * @returns the project under the names the cleanup view reads
 */
function normaliseCleanupProject(raw: unknown): CleanupProjectItem {
  const project = (raw ?? {}) as Record<string, unknown>;
  const id = textOf(project['project_id']);
  return {
    project_id: id,
    display_name: textOf(project['display_name']) || id,
    items: (Array.isArray(project['items']) ? project['items'] : []).map(normaliseCleanupItem),
  };
}

/**
 * Normalises a cleanup job from the runtime's wire names.
 *
 * @param raw the job as the runtime sent it
 * @returns the job under the names the cleanup view reads
 */
function normaliseCleanupJob(raw: CleanupJob): CleanupJob {
  const job = (raw ?? {}) as unknown as Record<string, unknown>;
  const projectItems = (job['projects'] ?? job['project_items'] ?? []) as unknown[];
  const kept = (job['kept'] ?? []) as unknown[];
  return {
    job_id: typeof job['job_id'] === 'string' ? job['job_id'] : '',
    due_since: typeof job['due_since'] === 'string' ? job['due_since'] : undefined,
    summary: normaliseCleanupSummary(job['summary']),
    projects: projectItems.map(normaliseCleanupProject),
    kept: kept.map(normaliseKeptItem),
    residue: Array.isArray(job['residue']) ? (job['residue'] as CleanupResidue[]) : [],
    durations: (job['durations'] as CleanupDurations) ?? {
      plans_days: 0,
      epics_days: 0,
      logs_days: 0,
      reports_days: 0,
      unenrolled_days: 0,
      remind_days: 0,
    },
  };
}

/**
 * @param message the runtime's error code or the HTTP status
 * @param status the response status
 * @returns an error carrying the status, so a caller can tell a refusal from an unavailable route
 */
function httpError(message: string, status: number): Error {
  const error = new Error(message) as Error & { status?: number };
  error.status = status;
  return error;
}

export class ApiClient {
  private readonly baseUrl: string;
  private onRevokedCallback?: (info?: string | RevokedEventData) => void;
  private onOfflineCallback?: () => void;
  private onOnlineCallback?: () => void;
  /** Called with the state of the shared event stream, for the live indicator. */
  private onStreamStateCallback?: (state: EventStreamState) => void;
  /** The listeners of the one shared `/api/v1/events` connection. */
  private readonly eventSubscribers = new Set<(event: string, data: unknown) => void>();
  private eventStreamAbort: (() => void) | null = null;
  /** Retries the shared stream now, after it gave up; `null` while no stream is open. */
  private eventStreamRetry: (() => void) | null = null;
  /** The last state the shared stream reported, so a late subscriber hears it at once. */
  private streamState: EventStreamState = 'connecting';

  constructor(baseUrl = '') {
    this.baseUrl = baseUrl;
  }

  public setOnRevoked(callback: (info?: string | RevokedEventData) => void): void {
    this.onRevokedCallback = callback;
  }

  public setOnOffline(callback: () => void): void {
    this.onOfflineCallback = callback;
  }

  /**
   * Registers the callback for a successful request or event-stream reconnect.
   *
   * The runtime store clears its offline state from it, so a transient blip does not leave the
   * offline banner up until a manual reload.
   *
   * @param callback called on every successful request and on every SSE connect
   */
  public setOnOnline(callback: () => void): void {
    this.onOnlineCallback = callback;
  }

  /**
   * Registers the callback for the state of the shared event stream.
   *
   * The runtime store mirrors it, so the live indicator names what the stream does instead of
   * inferring it from a request that happened to fail.
   *
   * @param callback called on every state change of the stream
   */
  public setOnStreamState(callback: (state: EventStreamState) => void): void {
    this.onStreamStateCallback = callback;
    // The stream may already be live when the runtime registers: a subscriber that missed the
    // transition is told the current state instead of waiting for the next one.
    callback(this.streamState);
  }

  /**
   * Retries the shared event stream at once, after it gave up.
   *
   * The stream reports `connecting` and then either `connected` or, after its limit, `offline`
   * again; a call while the stream is live does nothing.
   */
  public retryEventStream(): void {
    this.eventStreamRetry?.();
  }

  /**
   * Records a state of the shared stream and tells the subscriber.
   *
   * @param state the state the stream entered
   */
  private reportStreamState(state: EventStreamState): void {
    if (this.streamState === state) {
      return;
    }
    this.streamState = state;
    this.onStreamStateCallback?.(state);
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

  public async getOnboardingState(): Promise<OnboardingState> {
    return this.request<OnboardingState>('GET', '/api/v1/onboarding/state');
  }

  public async stepOnboarding(link: string): Promise<OnboardingState> {
    return this.request<OnboardingState>('POST', '/api/v1/onboarding/step', { link });
  }

  public async startOnboardingAuto(): Promise<OnboardingState> {
    return this.request<OnboardingState>('POST', '/api/v1/onboarding/auto', {});
  }

  public async getMachineSettings(): Promise<MachineSettingsRecord> {
    return this.request<MachineSettingsRecord>('GET', '/api/v1/machine/settings');
  }

  public async updateMachineSetting(
    key: string,
    value: string | null,
    revision: number,
  ): Promise<MachineSettingsUpdateResult> {
    return this.requestWithConflict<MachineSettingsUpdateResult>(
      'PUT',
      '/api/v1/machine/settings',
      {
        key,
        value,
        revision,
      },
    );
  }

  public async withdrawConsent(
    kind: string,
    revision: number,
  ): Promise<MachineSettingsUpdateResult> {
    return this.updateMachineSetting(`consent:${kind}`, null, revision);
  }

  public async getRecipes(projectId?: string): Promise<RecipeRef[]> {
    const query = projectId ? `?project=${encodeURIComponent(projectId)}` : '';
    const res = await this.request<{ recipes: RecipeRef[] }>('GET', `/api/v1/recipes${query}`);
    return res.recipes ?? [];
  }

  /**
   * Opens an entry under its project, addressed by its id from then on.
   *
   * @param projectId the project the entry starts in
   * @param request the kind of work and what the operator already said
   * @returns the entry's representation
   */
  public async openEntry(
    projectId: string,
    request: EntryOpenRequest,
  ): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(projectId)}/entries`,
      request,
    );
  }

  /**
   * @param entryId the entry's id
   * @returns the entry's current representation
   */
  public async getEntry(entryId: string): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'GET',
      `/api/v1/entries/${encodeURIComponent(entryId)}`,
    );
  }

  /**
   * Applies one of the entry's acts, such as adding a source or continuing to the ready step.
   *
   * The act's id is part of the runtime's contract (`add_description`, `remove:{source}`,
   * `documents_mode:copy`, `choose:{recipe}`, `chat`, `create`); it is sent verbatim.
   *
   * @param entryId the entry's id
   * @param act the act's link id
   * @param body the act's body
   * @returns the entry's new representation
   */
  public async actEntry(
    entryId: string,
    act: string,
    body: Record<string, unknown> = {},
  ): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/entries/${encodeURIComponent(entryId)}/links/${act}`,
      body,
    );
  }

  /**
   * @param entryId the entry's id
   * @param text the operator's message
   * @returns the entry's new representation, with the role's answer and a proposal where it has one
   */
  public async chatTurn(entryId: string, text: string): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/entries/${encodeURIComponent(entryId)}/chat/turns`,
      { text },
    );
  }

  /**
   * Ends the entry's chat before the work is created or the entry is abandoned.
   *
   * @param entryId the entry's id
   * @returns the entry's new representation, with the transcript complete
   */
  public async endEntryChat(entryId: string): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/entries/${encodeURIComponent(entryId)}/chat/end`,
      {},
    );
  }

  /**
   * Accepts or rejects the chat's proposal.
   *
   * @param entryId the entry's id
   * @param accept `true` to store the proposed description, `false` to discard it
   * @returns the entry's new representation
   */
  public async answerChatProposal(entryId: string, accept: boolean): Promise<StartStateResource> {
    const suffix = accept ? 'accept' : 'reject';
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/entries/${encodeURIComponent(entryId)}/chat/proposal/${suffix}`,
      {},
    );
  }

  /**
   * Lists the files the picker may show, confined to the checkout or the operator's home.
   *
   * @param projectId the project
   * @param root the root to list
   * @param path the directory relative to the root, empty for the root itself
   * @returns the listing
   */
  public async listProjectFiles(
    projectId: string,
    root: 'checkout' | 'home' = 'checkout',
    path = '',
  ): Promise<ConfinedFileList> {
    const query = new URLSearchParams({ root });
    if (path) {
      query.set('path', path);
    }
    return this.request<ConfinedFileList>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}/files?${query.toString()}`,
    );
  }

  /**
   * @param subjectId an ended plan or epic
   * @returns the follow-up chat the ended work offers, or `null` where it offers none
   */
  public async getEndedChat(
    projectId: string,
    subjectId: string,
  ): Promise<FollowUpProposal | null> {
    try {
      return await this.request<FollowUpProposal>(
        'GET',
        `/api/v1/projects/${encodeURIComponent(projectId)}/ended/${encodeURIComponent(subjectId)}/chat`,
      );
    } catch {
      return null;
    }
  }

  /**
   * Opens the follow-up entry of an accepted proposal.
   *
   * @param projectId the project the follow-up starts in
   * @param subjectId the ended plan or epic
   * @returns the opened entry
   */
  public async acceptFollowUp(projectId: string, subjectId: string): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(projectId)}/ended/${encodeURIComponent(subjectId)}/chat/accept`,
      { project_id: projectId },
    );
  }

  public async startPlan(request: StartPlanRequest): Promise<PlanStartResult> {
    return this.request<PlanStartResult>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(request.project_id)}/plans/start`,
      request,
    );
  }

  public async startEpic(request: StartEpicRequest): Promise<PlanStartResult> {
    return this.request<PlanStartResult>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(request.project_id)}/epics/start`,
      request,
    );
  }

  public async draftIntake(request: {
    project_id: string;
    prompt?: string;
    sources?: SourceInput[];
  }): Promise<IntakeDraft> {
    return this.request<IntakeDraft>('POST', '/api/v1/intake/draft', request);
  }

  private decompositionPath(projectId: string, epicId: string): string {
    return `/api/v1/projects/${encodeURIComponent(projectId)}/epics/${encodeURIComponent(epicId)}/decomposition`;
  }

  public async getDecomposition(projectId: string, epicId: string): Promise<EpicDecomposition> {
    return this.request<EpicDecomposition>('GET', this.decompositionPath(projectId, epicId));
  }

  public async checkDecomposition(projectId: string, epicId: string): Promise<EpicDecomposition> {
    return this.request<EpicDecomposition>(
      'POST',
      `${this.decompositionPath(projectId, epicId)}/check`,
      {},
    );
  }

  private lessonBase(projectId: string, lessonId: string): string {
    return (
      `/api/v1/projects/${encodeURIComponent(projectId)}/sources/lessons/` +
      encodeURIComponent(lessonId)
    );
  }

  public async getUnclaimedLessons(projectId: string): Promise<UnclaimedLessonSummary[]> {
    const res = await this.request<{ lessons: UnclaimedLessonSummary[] }>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}/sources/lessons`,
    );
    return res.lessons ?? [];
  }

  public async checkLessonPremise(
    projectId: string,
    lessonId: string,
  ): Promise<LessonPremiseCheckResult> {
    return this.request<LessonPremiseCheckResult>(
      'POST',
      `${this.lessonBase(projectId, lessonId)}/premise-check`,
      {},
    );
  }

  public async instructDecomposition(
    projectId: string,
    epicId: string,
    request: DecompositionInstructRequest,
  ): Promise<EpicDecomposition> {
    return this.request<EpicDecomposition>(
      'POST',
      `${this.decompositionPath(projectId, epicId)}/instruct`,
      request,
    );
  }

  public async claimLesson(
    projectId: string,
    lessonId: string,
    useAnyway: boolean,
  ): Promise<LessonClaimResult> {
    return this.request<LessonClaimResult>(
      'POST',
      `${this.lessonBase(projectId, lessonId)}/claim`,
      {
        use_anyway: useAnyway,
      },
    );
  }

  public async retireLesson(
    projectId: string,
    lessonId: string,
    reason?: string,
  ): Promise<LessonRetirementNotice> {
    return this.request<LessonRetirementNotice>(
      'POST',
      `${this.lessonBase(projectId, lessonId)}/retire`,
      reason ? { reason } : {},
    );
  }

  public async observePullRequest(
    projectId: string,
    reference: string,
  ): Promise<PullRequestObservation> {
    return this.request<PullRequestObservation>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(projectId)}/sources/pr/observe`,
      { pr_url: reference },
    );
  }

  public async adoptPullRequest(request: PrAdoptRequest): Promise<PrAdoptResult> {
    return this.request<PrAdoptResult>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(request.project_id)}/sources/pr/adopt`,
      request,
    );
  }

  public async commitDecomposition(
    projectId: string,
    epicId: string,
  ): Promise<DecompositionCommitResult> {
    return this.request<DecompositionCommitResult>(
      'POST',
      `${this.decompositionPath(projectId, epicId)}/commit`,
      {},
    );
  }

  public async previewLocalChanges(projectId: string): Promise<LocalChangesPreview> {
    return this.request<LocalChangesPreview>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}/sources/local-changes/preview`,
    );
  }

  public async captureLocalChanges(
    projectId: string,
    previewId?: string,
  ): Promise<LocalChangesCaptureResult> {
    return this.request<LocalChangesCaptureResult>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(projectId)}/sources/local-changes/capture`,
      { project_id: projectId, preview_id: previewId, confirmed: true },
    );
  }

  public async getConnectionStatus(): Promise<ConnectionStatus> {
    return this.request<ConnectionStatus>('GET', '/api/v1/status/connection');
  }

  public async setCredential(credentialKey: string, secret: string): Promise<void> {
    await this.request('PUT', `/api/v1/credentials/${encodeURIComponent(credentialKey)}`, {
      secret,
    });
  }

  public async getProjects(): Promise<ProjectOverviewResponse> {
    return this.request<ProjectOverviewResponse>('GET', '/api/v1/projects');
  }

  public async getProjectExplorer(): Promise<ProjectExplorer> {
    return this.request<ProjectExplorer>('GET', '/api/v1/projects?all=true');
  }

  public async updateFavourites(
    projectIds: string[],
  ): Promise<{ status: string; project_ids: string[] }> {
    return this.request<{ status: string; project_ids: string[] }>(
      'PUT',
      '/api/v1/explorer/favourites',
      {
        project_ids: projectIds,
      },
    );
  }

  public async toggleFavourite(projectId: string, favourite: boolean): Promise<void> {
    await this.request('PUT', `/api/v1/projects/${encodeURIComponent(projectId)}/favourite`, {
      favourite,
    });
  }

  /**
   * Persists the operator's order of the favourite projects.
   *
   * The order is the runtime's, so both clients and every paired browser show the same; the
   * runtime records it with the marks (`PUT /api/v1/explorer/favourites`).
   *
   * @param projectIds the favourites in their new order
   * @returns the order the runtime recorded
   */
  public async reorderFavouriteProjects(
    projectIds: string[],
  ): Promise<{ status: string; project_ids: string[] }> {
    return this.updateFavourites(projectIds);
  }

  /**
   * Reads a project's onboarding, driven by the runtime's setup state.
   *
   * @param projectId the project whose setup runs
   * @returns the run's current representation
   */
  public async getProjectOnboarding(projectId: string): Promise<ProjectOnboardingState> {
    return this.request<ProjectOnboardingState>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}/onboarding`,
    );
  }

  /**
   * Applies one step of a project's onboarding.
   *
   * A value changed in another client meanwhile is refused with `conflict`, which this call
   * returns rather than throws: the screen shows the current value beside the operator's choice.
   *
   * @param projectId the project whose setup runs
   * @param step the step's id, e.g. `identity`, or `chat` for a question
   * @param body the step's values
   * @returns the run's new representation
   */
  public async stepProjectOnboarding(
    projectId: string,
    step: string,
    body: Record<string, unknown> = {},
  ): Promise<ProjectOnboardingState> {
    return this.requestWithConflict<ProjectOnboardingState>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(projectId)}/onboarding/step`,
      { step, ...body },
    );
  }

  /**
   * Reads the values a project sets of its own, with the revision the change applies to.
   *
   * @param projectId the project
   * @returns its settings at one revision
   */
  public async getProjectSettings(projectId: string): Promise<ProjectSettingsRecord> {
    return this.request<ProjectSettingsRecord>(
      'GET',
      `/api/v1/projects/${encodeURIComponent(projectId)}/settings`,
    );
  }

  /**
   * Saves one value of a project's settings against the revision it was read at.
   *
   * @param projectId the project
   * @param aspect the value's aspect, e.g. `landing`
   * @param value the operator's choice
   * @param expectedRevision the revision the screen showed
   * @returns what happened; a refused change carries `conflict` and the current value
   */
  public async saveProjectSettings(
    projectId: string,
    aspect: string,
    value: string | null,
    expectedRevision: number,
  ): Promise<ProjectSettingsUpdateResult> {
    return this.requestWithConflict<ProjectSettingsUpdateResult>(
      'PUT',
      `/api/v1/projects/${encodeURIComponent(projectId)}/settings`,
      { aspect, value, expected_revision: expectedRevision },
    );
  }

  public async startProjectOnboarding(path: string): Promise<{ ok: boolean }> {
    return this.request<{ ok: boolean }>('POST', '/api/v1/project-onboardings', {
      path,
    });
  }

  public async releaseProject(
    projectId: string,
    reason?: string,
    force = false,
  ): Promise<ProjectReleaseResult> {
    return this.request<ProjectReleaseResult>(
      'POST',
      `/api/v1/projects/${encodeURIComponent(projectId)}/release`,
      {
        project_id: projectId,
        confirm_release: true,
        force,
        reason,
      },
    );
  }

  public async unenrolProject(projectId: string): Promise<void> {
    await this.request('POST', `/api/v1/projects/${encodeURIComponent(projectId)}/unenrol`, {});
  }

  public async abandonOnboarding(abandonPathOrId: string): Promise<void> {
    const target = abandonPathOrId.startsWith('/')
      ? abandonPathOrId
      : `/api/v1/project-onboardings/${encodeURIComponent(abandonPathOrId)}`;
    await this.request('DELETE', target);
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

  private workflowBase(projectId?: string | null): string {
    return projectId
      ? `/api/v1/projects/${encodeURIComponent(projectId)}/workflows`
      : '/api/v1/workflows';
  }

  public async getWorkflows(projectId?: string | null): Promise<WorkflowUnit[]> {
    return this.request<WorkflowUnit[]>('GET', this.workflowBase(projectId));
  }

  public async getWorkflowCatalogue(projectId?: string | null): Promise<WorkflowCatalogue> {
    return this.request<WorkflowCatalogue>('GET', `${this.workflowBase(projectId)}/catalogue`);
  }

  public async validateWorkflow(
    payload: ValidateWorkflowPayload,
  ): Promise<WorkflowValidationResult> {
    return this.request<WorkflowValidationResult>('POST', '/api/v1/workflows/validate', payload);
  }

  public async placeWorkflowUnit(
    projectId: string | null,
    unitId: string,
    payload: PlaceWorkflowPayload,
  ): Promise<UnitPlacingResult> {
    const path = `${this.workflowBase(projectId)}/${encodeURIComponent(unitId)}`;
    return this.request<UnitPlacingResult>('PUT', path, payload);
  }

  public async removeWorkflowUnit(
    projectId: string | null,
    unitId: string,
  ): Promise<UnitPlacingResult> {
    const path = `${this.workflowBase(projectId)}/${encodeURIComponent(unitId)}`;
    return this.request<UnitPlacingResult>('DELETE', path);
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
        items: data.map(normaliseReportItem),
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
    const items = (rawObj.items || rawObj.reports || []).map(normaliseReportItem);
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
    const report = await this.request<PlanCompletionReport>(
      'GET',
      `/api/v1/plans/${encodeURIComponent(planId)}/completion`,
    );
    return {
      ...report,
      from: normaliseReportFrom(report.from),
      token_usage: normaliseTokenUsage(report.token_usage),
    };
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

  public async getSystemSnapshot(window?: SystemWindow, project?: string): Promise<SystemSnapshot> {
    const params = new URLSearchParams();
    if (window) params.set('window', window);
    if (project) params.set('project', project);
    const qs = params.toString();
    let path = '/api/v1/system';
    if (qs) path += `?${qs}`;
    const snapshot = await this.request<SystemSnapshot>('GET', path);
    return normaliseSystemSnapshot(snapshot);
  }

  public async stopRuntime(
    when: 'drain' | 'now',
    then?: 'restart' | 'upgrade',
  ): Promise<{ lifecycle_state: string }> {
    const body: Record<string, unknown> = {
      now: when === 'now',
      drain: when === 'drain',
    };
    if (then) {
      // NOSONAR: typescript:S7739 - external wire contract requires 'then' property
      Reflect.set(body, 'then', then);
    }
    return this.request<{ lifecycle_state: string }>('POST', '/api/v1/runtime/stop', body);
  }

  public async withdrawStop(): Promise<void> {
    await this.request('DELETE', '/api/v1/runtime/stop');
  }

  public async upgradeRuntime(
    version?: string,
    when: 'drain' | 'now' = 'drain',
  ): Promise<{ status: string }> {
    const body: Record<string, unknown> = {
      when,
    };
    if (version) body['version'] = version;
    return this.request<{ status: string }>('POST', '/api/v1/operator/runtime/upgrade', body);
  }

  public async withdrawUpgrade(): Promise<void> {
    try {
      await this.request('DELETE', '/api/v1/operator/runtime/upgrade');
    } catch (err: unknown) {
      // The operator channel is the runtime's own withdraw. A deployment that offers only the
      // system route is the fallback; a refused withdraw (the links are switched) is not retried.
      const status = (err as Error & { status?: number })?.status;
      const message = (err as Error)?.message ?? '';
      if (status === 409 || message.includes('not_withdrawable')) throw err;
      const routeUnavailable =
        status === 404 ||
        status === 405 ||
        message.includes('mock_no_route') ||
        message.includes('unknown_route') ||
        message.includes('no route');
      if (!routeUnavailable) throw err;
      try {
        await this.request('POST', '/api/v1/system/upgrade/withdraw', {});
      } catch {
        throw err;
      }
    }
  }

  public async checkUpgrade(): Promise<{
    current_version: string;
    latest_version?: string;
    release_notes_url?: string;
  }> {
    return this.request('POST', '/api/v1/operator/runtime/upgrade/check', {});
  }

  public async getCleanupJob(): Promise<CleanupJob> {
    const job = await this.request<CleanupJob>('GET', '/api/v1/system/cleanup');
    return normaliseCleanupJob(job);
  }

  public async executeCleanup(
    jobId: string,
    excludeIds?: string[],
  ): Promise<{ status: string; freed_bytes: number }> {
    const body = { unchecked_ids: excludeIds || [] };
    return this.request<{ status: string; freed_bytes: number }>(
      'POST',
      `/api/v1/system/cleanup/${encodeURIComponent(jobId)}/delete`,
      body,
    );
  }

  public async remindCleanup(jobId: string): Promise<void> {
    await this.request('POST', `/api/v1/system/cleanup/${encodeURIComponent(jobId)}/remind`, {});
  }

  public async ignoreCleanup(jobId: string): Promise<void> {
    await this.request('POST', `/api/v1/system/cleanup/${encodeURIComponent(jobId)}/ignore`, {});
  }

  public async getKeptCleanup(): Promise<{ items: CleanupKeptItem[] }> {
    const data = await this.request<CleanupKeptResponse>('GET', '/api/v1/system/cleanup/kept');
    const rawItems = Array.isArray(data) ? data : (data?.items ?? []);
    return { items: rawItems.map(normaliseKeptItem) };
  }

  public async releaseKeptCleanup(
    itemIds?: string[],
    all = false,
  ): Promise<{ released_count: number }> {
    const body = all ? { all: true } : { item_ids: itemIds || [] };
    return this.request<{ released_count: number }>('PUT', '/api/v1/system/cleanup/kept', body);
  }

  public async getCleanupDurations(): Promise<CleanupDurations> {
    return this.request<CleanupDurations>('GET', '/api/v1/system/cleanup/durations');
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
      // Only an authenticated request can lose a pairing: an unauthenticated 401 means the route
      // needs pairing, not that the device was revoked.
      if (secret) {
        this.clearAuth();
        this.onRevokedCallback?.('unauthorized');
      }
      throw new Error('unauthorized');
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw httpError(err.error || `HTTP ${res.status}`, res.status);
    }

    this.onOnlineCallback?.();
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

  public async getPlanProcess(planId: string): Promise<ProcessTree> {
    return this.request<ProcessTree>('GET', `/api/v1/plans/${encodeURIComponent(planId)}/process`);
  }

  public async getTaskActivity(planId: string, taskId: string): Promise<TaskActivity> {
    return this.request<TaskActivity>(
      'GET',
      `/api/v1/plans/${encodeURIComponent(planId)}/tasks/${encodeURIComponent(taskId)}/activity`,
    );
  }

  public async getEpicProcess(epicId: string): Promise<EpicProcess> {
    return this.request<EpicProcess>('GET', `/api/v1/epics/${encodeURIComponent(epicId)}/process`);
  }

  /**
   * @param epicId the epic
   * @returns the epic as a resource: its header, workstreams, sources, landings, registers,
   *     tokens, and events
   */
  public async getEpicResource(epicId: string): Promise<EpicResource> {
    return this.request<EpicResource>('GET', `/api/v1/epics/${encodeURIComponent(epicId)}`);
  }

  /**
   * @param epicId the epic
   * @returns the archive check the runtime ran for it
   */
  public async getEpicArchiveCheck(epicId: string): Promise<ArchiveCheck> {
    return this.request<ArchiveCheck>(
      'GET',
      `/api/v1/epics/${encodeURIComponent(epicId)}/archive-check`,
    );
  }

  /**
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @returns that register's review
   */
  public async getEpicReview(
    epicId: string,
    register: 'findings' | 'watches',
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'GET',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review?register=${register}`,
    );
  }

  /**
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @param itemId the item to keep
   * @param as `finding` or `watch`
   * @returns the review after the disposition
   */
  public async keepReviewItem(
    epicId: string,
    register: 'findings' | 'watches',
    itemId: string,
    as: 'finding' | 'watch',
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/items/${encodeURIComponent(itemId)}/keep`,
      { register, as },
    );
  }

  /**
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @param itemId the item to drop
   * @param reason why the operator drops it
   * @returns the review after the disposition
   */
  public async dropReviewItem(
    epicId: string,
    register: 'findings' | 'watches',
    itemId: string,
    reason: string,
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/items/${encodeURIComponent(itemId)}/drop`,
      { register, reason },
    );
  }

  /**
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @param itemId the item to promote
   * @param instruction what the planning role should change, or `null` for the first draft
   * @returns the review with the drafted plan
   */
  public async promoteReviewItem(
    epicId: string,
    register: 'findings' | 'watches',
    itemId: string,
    instruction?: string,
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/items/${encodeURIComponent(itemId)}/promote`,
      instruction ? { register, instruction } : { register },
    );
  }

  /**
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @param itemId the item whose statement and rules are rewritten
   * @param instruction what to adapt or add, in the operator's words
   * @returns the review with the read-back
   */
  public async editReviewItem(
    epicId: string,
    register: 'findings' | 'watches',
    itemId: string,
    instruction: string,
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/items/${encodeURIComponent(itemId)}/edit`,
      { register, instruction },
    );
  }

  /**
   * Applies the read-back the planning role proposed.
   *
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @returns the review after the change
   */
  public async acceptReviewReadBack(
    epicId: string,
    register: 'findings' | 'watches',
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/read-back/accept`,
      { register },
    );
  }

  /**
   * Discards the read-back and returns to the item as it was.
   *
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @returns the review before the change
   */
  public async discardReviewReadBack(
    epicId: string,
    register: 'findings' | 'watches',
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/read-back/discard`,
      { register },
    );
  }

  /**
   * Stages the plan the planning role drafted for a promoted item.
   *
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @returns the review after the plan was staged
   */
  public async stageReviewPlan(
    epicId: string,
    register: 'findings' | 'watches',
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/plan/stage`,
      { register },
    );
  }

  /**
   * @param epicId the epic
   * @param register `findings` or `watches`
   * @returns the review after the decisions were written to the epic's ledger
   */
  public async commitReview(
    epicId: string,
    register: 'findings' | 'watches',
  ): Promise<ReviewResource> {
    return this.request<ReviewResource>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/review/commit`,
      { register },
    );
  }

  /**
   * @param subjectId a plan or an epic
   * @returns the planning role's chat about it
   */
  public async getChat(subjectId: string): Promise<ChatReadBack> {
    if (subjectId.startsWith('epic:')) {
      const epicId = subjectId.slice('epic:'.length);
      return this.request<ChatReadBack>('GET', `/api/v1/epics/${encodeURIComponent(epicId)}/chat`);
    }
    return this.request<ChatReadBack>('GET', `/api/v1/plans/${encodeURIComponent(subjectId)}/chat`);
  }

  /**
   * @param subjectId a plan or an epic
   * @param text the operator's message
   * @returns the chat with the role's answer
   */
  public async chatWithSubject(subjectId: string, text: string): Promise<ChatReadBack> {
    if (subjectId.startsWith('epic:')) {
      const epicId = subjectId.slice('epic:'.length);
      return this.request<ChatReadBack>(
        'POST',
        `/api/v1/epics/${encodeURIComponent(epicId)}/chat/turns`,
        { text },
      );
    }
    return this.request<ChatReadBack>(
      'POST',
      `/api/v1/plans/${encodeURIComponent(subjectId)}/chat/turns`,
      { text },
    );
  }

  /**
   * @param subjectId a plan or an epic
   * @param accept `true` to apply the proposal, `false` to drop it
   * @returns the chat after the answer
   */
  public async answerSubjectChatProposal(
    subjectId: string,
    accept: boolean,
  ): Promise<ChatReadBack> {
    const suffix = accept ? 'accept' : 'reject';
    if (subjectId.startsWith('epic:')) {
      const epicId = subjectId.slice('epic:'.length);
      return this.request<ChatReadBack>(
        'POST',
        `/api/v1/epics/${encodeURIComponent(epicId)}/chat/proposal/${suffix}`,
        {},
      );
    }
    return this.request<ChatReadBack>(
      'POST',
      `/api/v1/plans/${encodeURIComponent(subjectId)}/chat/proposal/${suffix}`,
      {},
    );
  }

  /**
   * @param subjectId a plan or an epic
   * @returns the chat after it was closed
   */
  public async endChat(subjectId: string): Promise<ChatReadBack> {
    if (subjectId.startsWith('epic:')) {
      const epicId = subjectId.slice('epic:'.length);
      return this.request<ChatReadBack>(
        'POST',
        `/api/v1/epics/${encodeURIComponent(epicId)}/chat/end`,
        {},
      );
    }
    return this.request<ChatReadBack>(
      'POST',
      `/api/v1/plans/${encodeURIComponent(subjectId)}/chat/end`,
      {},
    );
  }

  /**
   * @param decisionId the decision
   * @returns the planning role's chat about it
   */
  public async getDecisionChat(decisionId: string): Promise<ChatReadBack> {
    return this.request<ChatReadBack>(
      'GET',
      `/api/v1/decisions/${encodeURIComponent(decisionId)}/chat`,
    );
  }

  /**
   * @param decisionId the decision
   * @param text the operator's message
   * @returns the chat with the role's answer
   */
  public async decisionChatTurn(decisionId: string, text: string): Promise<ChatReadBack> {
    return this.request<ChatReadBack>(
      'POST',
      `/api/v1/decisions/${encodeURIComponent(decisionId)}/chat/turns`,
      { text },
    );
  }

  /**
   * @param decisionId the decision
   * @param accept `true` to take the proposed answer, `false` to drop it
   * @returns the chat after the answer
   */
  public async answerDecisionChatProposal(
    decisionId: string,
    accept: boolean,
  ): Promise<ChatReadBack> {
    const suffix = accept ? 'accept' : 'reject';
    return this.request<ChatReadBack>(
      'POST',
      `/api/v1/decisions/${encodeURIComponent(decisionId)}/chat/proposal/${suffix}`,
      {},
    );
  }

  /**
   * @param decisionId the decision
   * @returns the chat after it was closed
   */
  public async endDecisionChat(decisionId: string): Promise<ChatReadBack> {
    return this.request<ChatReadBack>(
      'POST',
      `/api/v1/decisions/${encodeURIComponent(decisionId)}/chat/end`,
      {},
    );
  }

  public async getPlanActions(planId: string): Promise<WorkActions> {
    return this.request<WorkActions>('GET', `/api/v1/plans/${encodeURIComponent(planId)}/actions`);
  }

  public async applyPlanAction(
    planId: string,
    action: string,
    body?: Record<string, unknown>,
  ): Promise<WorkActions> {
    return this.request<WorkActions>(
      'POST',
      `/api/v1/plans/${encodeURIComponent(planId)}/actions/${encodeURIComponent(action)}`,
      body ?? {},
    );
  }

  public async getEpicActions(epicId: string): Promise<WorkActions> {
    return this.request<WorkActions>('GET', `/api/v1/epics/${encodeURIComponent(epicId)}/actions`);
  }

  public async applyEpicAction(
    epicId: string,
    action: string,
    body?: Record<string, unknown>,
  ): Promise<WorkActions> {
    return this.request<WorkActions>(
      'POST',
      `/api/v1/epics/${encodeURIComponent(epicId)}/actions/${encodeURIComponent(action)}`,
      body ?? {},
    );
  }

  public async getPlanHolds(planId: string): Promise<HoldRecord[]> {
    return this.request<HoldRecord[]>('GET', `/api/v1/plans/${encodeURIComponent(planId)}/holds`);
  }

  public async resolveHold(
    planId: string,
    holdId: string,
    act: string,
    reason?: string,
  ): Promise<HoldResolutionResult> {
    return this.request<HoldResolutionResult>(
      'POST',
      `/api/v1/plans/${encodeURIComponent(planId)}/holds/${encodeURIComponent(holdId)}/resolve`,
      reason ? { act, reason } : { act },
    );
  }

  public async getPlanSettings(planId: string): Promise<PlanSettingsRecord> {
    return this.request<PlanSettingsRecord>(
      'GET',
      `/api/v1/plans/${encodeURIComponent(planId)}/settings`,
    );
  }

  public async updatePlanSettings(
    planId: string,
    aspect: string,
    value: string | null,
    revision: number,
  ): Promise<PlanSettingsUpdateResult> {
    const path = `/api/v1/plans/${encodeURIComponent(planId)}/settings`;
    return this.requestWithConflict<PlanSettingsUpdateResult>('PUT', path, {
      aspect,
      value,
      revision,
    });
  }

  private async requestWithConflict<T>(method: string, path: string, body: unknown): Promise<T> {
    const secret = this.getSecret();
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (secret) {
      headers['Authorization'] = `Bearer ${secret}`;
    }
    let res: Response;
    try {
      res = await fetch(`${this.baseUrl}${path}`, {
        method,
        headers,
        body: JSON.stringify(body),
      });
    } catch {
      this.onOfflineCallback?.();
      throw new Error('network_offline');
    }
    if (res.status === 401) {
      // As in `request`: only a 401 to a request that carried a secret is auth loss.
      if (secret) {
        this.clearAuth();
        this.onRevokedCallback?.('unauthorized');
      }
      throw new Error('unauthorized');
    }
    if (res.status === 409 || res.ok) {
      this.onOnlineCallback?.();
      return (await res.json()) as T;
    }
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `HTTP ${res.status}`);
  }

  public subscribeTaskActivity(
    planId: string,
    taskId: string,
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
        const path = `/api/v1/plans/${encodeURIComponent(planId)}/tasks/${encodeURIComponent(taskId)}/activity/events`;
        const res = await fetch(`${this.baseUrl}${path}`, {
          headers: {
            Authorization: `Bearer ${secret}`,
          },
          signal: internalController.signal,
        });

        if (!res.ok || !res.body) {
          return;
        }

        const reader = res.body.getReader();
        currentReader = reader;
        const decoder = new TextDecoder();
        let buffer = '';

        while (active && !internalController.signal.aborted) {
          const { done, value } = await reader.read();
          if (done) {
            currentReader = undefined;
            return;
          }

          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split(/(?:\r\n|\r|\n){2}/);
          buffer = parts.pop() || '';

          for (const chunk of parts) {
            const parsed = parseSseChunk(chunk);
            if (!parsed) continue;
            handleSseMessage(parsed.eventType, parsed.eventData, onEvent, () => {});
          }
        }
      } catch {
        // Ignore stream closing
      }
    };

    void connect();
    return abortAll;
  }

  /**
   * Subscribes to the runtime's events.
   *
   * All subscribers share one `/api/v1/events` connection: the first listener starts it, the last
   * unsubscribe aborts it, and one auth loss is reported once. A second stream could otherwise
   * observe the same `401` and independently invalidate the session.
   *
   * @param onEvent the callback per event
   * @param signal an optional abort signal
   * @returns the unsubscribe function
   */
  public subscribeEvents(
    onEvent: (event: string, data: unknown) => void,
    signal?: AbortSignal,
  ): () => void {
    this.eventSubscribers.add(onEvent);
    if (signal) {
      if (signal.aborted) {
        this.eventSubscribers.delete(onEvent);
        return () => {};
      }
      signal.addEventListener('abort', () => this.unsubscribeEvent(onEvent), { once: true });
    }
    this.eventStreamAbort ??= this.subscribeStream(
      '/api/v1/events',
      (event, data) => {
        this.eventSubscribers.forEach((listener) => {
          try {
            listener(event, data);
          } catch {
            // One broken listener must not starve the others or look like a stream failure.
          }
        });
      },
      undefined,
      (state) => this.reportStreamState(state),
      (retry) => {
        this.eventStreamRetry = retry;
      },
    );
    return () => this.unsubscribeEvent(onEvent);
  }

  /**
   * Removes one listener; the last one to leave closes the shared stream.
   *
   * @param onEvent the listener to remove
   */
  private unsubscribeEvent(onEvent: (event: string, data: unknown) => void): void {
    const removed = this.eventSubscribers.delete(onEvent);
    if (removed && this.eventSubscribers.size === 0 && this.eventStreamAbort) {
      this.eventStreamAbort();
      this.eventStreamAbort = null;
      this.eventStreamRetry = null;
    }
  }

  /**
   * Subscribes to the events of one open entry: the source progress while it is gathered and
   * screened, and the chat's turns.
   *
   * @param entryId the entry's id
   * @param onEvent the callback per event
   * @param signal an optional abort signal
   * @returns the unsubscribe function
   */
  public subscribeEntryEvents(
    entryId: string,
    onEvent: (event: string, data: unknown) => void,
    signal?: AbortSignal,
  ): () => void {
    return this.subscribeStream(
      `/api/v1/entries/${encodeURIComponent(entryId)}/events`,
      onEvent,
      signal,
    );
  }

  /**
   * The one server-sent-events reader of the client.
   *
   * A dropped stream retries at once after a short pause, reports `reconnecting` while it does,
   * and gives up after {@link STREAM_RETRY_LIMIT} attempts with `offline` — the state whose
   * `Try again` calls {@link retryEventStream}. The retry is the stream's own, so a subscription
   * that is already open is revived instead of being replaced.
   *
   * @param path the stream's path
   * @param onEvent the callback per event
   * @param signal an optional abort signal
   * @param onState the reporter of the stream's state
   * @param registerRetry receives the function that retries the stream now
   * @returns the unsubscribe function
   */
  private subscribeStream(
    path: string,
    onEvent: (event: string, data: unknown) => void,
    signal?: AbortSignal,
    onState?: (state: EventStreamState) => void,
    registerRetry?: (retry: () => void) => void,
  ): () => void {
    let active = true;
    let currentReader: ReadableStreamDefaultReader<Uint8Array> | undefined;
    let connecting = false;
    let failures = 0;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    const internalController = new AbortController();

    if (signal) {
      if (signal.aborted) {
        internalController.abort();
      } else {
        signal.addEventListener('abort', () => internalController.abort(), { once: true });
      }
    }

    const clearRetry = () => {
      if (retryTimer !== null) {
        clearTimeout(retryTimer);
        retryTimer = null;
      }
    };

    const abortAll = () => {
      active = false;
      clearRetry();
      internalController.abort();
      if (currentReader) {
        void currentReader.cancel().catch(() => {});
        currentReader = undefined;
      }
    };

    /** Schedules the next attempt, or reports the stream offline once the limit is spent. */
    const reconnect = () => {
      failures += 1;
      if (failures >= STREAM_RETRY_LIMIT) {
        onState?.('offline');
        return;
      }
      onState?.('reconnecting');
      clearRetry();
      retryTimer = setTimeout(() => {
        retryTimer = null;
        void connect();
      }, STREAM_RETRY_DELAY_MS);
    };

    const connect = async () => {
      const secret = this.getSecret();
      if (!secret || !active || internalController.signal.aborted) return;
      if (connecting || currentReader) return;
      connecting = true;
      onState?.(failures === 0 ? 'connecting' : 'reconnecting');

      try {
        const res = await fetch(`${this.baseUrl}${path}`, {
          headers: {
            Authorization: `Bearer ${secret}`,
          },
          signal: internalController.signal,
        });

        if (res.status === 401) {
          // The stream always carries the secret, so this is a genuine auth loss.
          if (secret) {
            this.clearAuth();
            this.onRevokedCallback?.('unauthorized');
          }
          abortAll();
          return;
        }

        if (!res.ok || !res.body) {
          throw new Error(`SSE failed: ${res.status}`);
        }

        failures = 0;
        onState?.('connected');
        this.onOnlineCallback?.();
        const reader = res.body.getReader();
        currentReader = reader;
        const decoder = new TextDecoder();
        let buffer = '';

        while (active && !internalController.signal.aborted) {
          const { done, value } = await reader.read();
          if (done) {
            currentReader = undefined;
            if (active && !internalController.signal.aborted) {
              reconnect();
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
          reconnect();
        }
      } finally {
        connecting = false;
      }
    };

    registerRetry?.(() => {
      if (!active || internalController.signal.aborted || currentReader || connecting) return;
      clearRetry();
      failures = 0;
      onState?.('connecting');
      void connect();
    });

    void connect();

    return () => {
      abortAll();
    };
  }
}

/** The pause between two attempts of a dropped stream. */
const STREAM_RETRY_DELAY_MS = 2000;

/** The attempts after which the stream reports `offline` and waits for `Try again`. */
const STREAM_RETRY_LIMIT = 10;

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
