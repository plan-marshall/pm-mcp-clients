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
  actions: ActionAvailability[];
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
  window: 'hour' | 'today' | '7d';
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
  public async answerChatProposal(
    entryId: string,
    accept: boolean,
  ): Promise<StartStateResource> {
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
  public async getEndedChat(subjectId: string): Promise<FollowUpProposal | null> {
    try {
      return await this.request<FollowUpProposal>(
        'GET',
        `/api/v1/ended/${encodeURIComponent(subjectId)}/chat`,
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
  public async acceptFollowUp(
    projectId: string,
    subjectId: string,
  ): Promise<StartStateResource> {
    return this.request<StartStateResource>(
      'POST',
      `/api/v1/ended/${encodeURIComponent(subjectId)}/chat/accept`,
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

  public async getSystemSnapshot(
    window?: 'hour' | 'today' | '7d',
    project?: string,
  ): Promise<SystemSnapshot> {
    const params = new URLSearchParams();
    if (window) params.set('window', window);
    if (project) params.set('project', project);
    const qs = params.toString();
    return this.request<SystemSnapshot>('GET', `/api/v1/system${qs ? `?${qs}` : ''}`);
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
    await this.request('DELETE', '/api/v1/operator/runtime/upgrade');
  }

  public async checkUpgrade(): Promise<{
    current_version: string;
    latest_version?: string;
    release_notes_url?: string;
  }> {
    return this.request('POST', '/api/v1/operator/runtime/upgrade/check', {});
  }

  public async getCleanupJob(): Promise<CleanupJob> {
    return this.request<CleanupJob>('GET', '/api/v1/system/cleanup');
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
    return this.request<{ items: CleanupKeptItem[] }>('GET', '/api/v1/system/cleanup/kept');
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
    const res = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: JSON.stringify(body),
    });
    if (res.status === 409 || res.ok) {
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

  public subscribeEvents(
    onEvent: (event: string, data: unknown) => void,
    signal?: AbortSignal,
  ): () => void {
    return this.subscribeStream('/api/v1/events', onEvent, signal);
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
   * @param path the stream's path
   * @param onEvent the callback per event
   * @param signal an optional abort signal
   * @returns the unsubscribe function
   */
  private subscribeStream(
    path: string,
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
        const res = await fetch(`${this.baseUrl}${path}`, {
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
