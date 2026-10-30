/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.ChatReadBackResource;
import de.planmarshall.api.control.ChatState;
import de.planmarshall.api.control.ChatTurnResource;
import de.planmarshall.api.control.DecompositionCheck;
import de.planmarshall.api.control.DecompositionPlan;
import de.planmarshall.api.control.DecompositionResource;
import de.planmarshall.api.control.DecompositionWorkstream;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.HoldChangedEvent;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.HoldResolutionRequest;
import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.control.PlanAction;
import de.planmarshall.api.control.PlanActionRequest;
import de.planmarshall.api.control.PlanControlEvent;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateRequest;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.PlanSettingsValue;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.decision.DecisionAnswerRequest;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.InterpretationType;
import de.planmarshall.api.decomposition.CheckSeverity;
import de.planmarshall.api.decomposition.DecompositionCheckFinding;
import de.planmarshall.api.decomposition.DecompositionCommitResult;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.DecompositionState;
import de.planmarshall.api.decomposition.DecompositionValidator;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.decomposition.StagedPlanRecord;
import de.planmarshall.api.decomposition.WorkstreamRecord;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.designer.WorkflowValidationRequest;
import de.planmarshall.api.entry.DeviceListResource;
import de.planmarshall.api.entry.DevicePairedEvent;
import de.planmarshall.api.entry.DeviceRenamedEvent;
import de.planmarshall.api.entry.DeviceRevokedEvent;
import de.planmarshall.api.entry.ListenerChangedEvent;
import de.planmarshall.api.entry.ListenerKind;
import de.planmarshall.api.entry.ListenerResource;
import de.planmarshall.api.entry.ListenerState;
import de.planmarshall.api.entry.PairedDeviceResource;
import de.planmarshall.api.entry.PairingCode;
import de.planmarshall.api.entry.PairingRedemptionResponse;
import de.planmarshall.api.entry.PairingResource;
import de.planmarshall.api.entry.WebCredentialResource;
import de.planmarshall.api.event.EventEnvelope;
import de.planmarshall.api.event.EventName;
import de.planmarshall.api.event.EventScope;
import de.planmarshall.api.event.EventScopeKind;
import de.planmarshall.api.event.StreamClosedEvent;
import de.planmarshall.api.intent.EpicIntentResource;
import de.planmarshall.api.intent.OutlineResource;
import de.planmarshall.api.intent.OverSizeDistributionResource;
import de.planmarshall.api.intent.PlanIntentResource;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.ConsentKind;
import de.planmarshall.api.onboarding.DocumentsMode;
import de.planmarshall.api.onboarding.MachineOnboardingState;
import de.planmarshall.api.onboarding.MachineSettingsChangedEvent;
import de.planmarshall.api.onboarding.MachineSettingsUpdateRequest;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.onboarding.OnboardingStepCompletedEvent;
import de.planmarshall.api.onboarding.PlanDefaults;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.FavouritesUpdateRequest;
import de.planmarshall.api.project.ProjectChangedEvent;
import de.planmarshall.api.project.ProjectEnrolledEvent;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.api.project.ProjectReleaseResult;
import de.planmarshall.api.project.ProjectReleasedEvent;
import de.planmarshall.api.project.ReleaseWaitStatus;
import de.planmarshall.api.report.ChangeMapRecord;
import de.planmarshall.api.report.DecisionRecord;
import de.planmarshall.api.report.DeliverableSummary;
import de.planmarshall.api.report.OwnCallRecord;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReadReportsRequest;
import de.planmarshall.api.report.ReplanningRecord;
import de.planmarshall.api.report.ReportFrom;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.api.report.ReportKind;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.report.TokenBreakdownItem;
import de.planmarshall.api.report.TokenComponentRecord;
import de.planmarshall.api.report.TokenUsageRecord;
import de.planmarshall.api.report.TokenWasteRecord;
import de.planmarshall.api.resource.AcceptEditRequest;
import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.EnrolRequest;
import de.planmarshall.api.resource.EnrolResult;
import de.planmarshall.api.resource.StoreDigestComparison;
import de.planmarshall.api.resource.UnenrolResult;
import de.planmarshall.api.source.git.ChangedFile;
import de.planmarshall.api.source.git.LocalChangesCaptureRequest;
import de.planmarshall.api.source.git.LocalChangesCaptureResult;
import de.planmarshall.api.source.git.LocalChangesPreview;
import de.planmarshall.api.source.git.PrAdoptRequest;
import de.planmarshall.api.source.git.PrAdoptResult;
import de.planmarshall.api.source.git.PullRequestObservation;
import de.planmarshall.api.source.git.RateLimitStatus;
import de.planmarshall.api.source.git.ScreeningOutcome;
import de.planmarshall.api.source.git.SourceLane;
import de.planmarshall.api.source.lesson.LessonClaimRequest;
import de.planmarshall.api.source.lesson.LessonClaimResult;
import de.planmarshall.api.source.lesson.LessonPremiseCheckResult;
import de.planmarshall.api.source.lesson.LessonPremiseEvidence;
import de.planmarshall.api.source.lesson.LessonRetirementNotice;
import de.planmarshall.api.source.lesson.PremiseEvidenceKind;
import de.planmarshall.api.source.lesson.UnclaimedLessonSummary;
import de.planmarshall.api.start.ConfinedRoot;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.IntakeDraft;
import de.planmarshall.api.start.PlanStartResult;
import de.planmarshall.api.start.RecipeParameter;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.SourceInput;
import de.planmarshall.api.start.StartEpicRequest;
import de.planmarshall.api.start.StartPlanRequest;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.CleanupKeptItem;
import de.planmarshall.api.system.CleanupProjectItem;
import de.planmarshall.api.system.CleanupResidue;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeLifecycleState;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.api.web.PairedDevice;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockEventStream;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.LinkDefinition;
import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * The core state machine and request dispatcher for the mock runtime.
 */
@SuppressWarnings("java:S1075")
public final class MockRuntimeEngine implements AutoCloseable {

    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_STATE = "state";
    private static final String KEY_SCENARIO = "scenario";

    /** The cleanup item kinds the delete counts report separately. */
    private static final String CLEANUP_KIND_PLAN = "plan";
    private static final String CLEANUP_KIND_EPIC = "epic";

    /** The default sweep coordinates every cleanup fixture carries. */
    private static final String SWEEP_TODAY = "today 03:00";
    private static final String SWEEP_TOMORROW = "tomorrow 03:00";

    /** The member the cleanup resources report the freed size under. */
    private static final String FIELD_FREED_BYTES = "freed_bytes";

    /** The fixture member the cleanup residues are read from. */
    private static final String FIXTURE_RESIDUE = "residue";

    /** The fixture members the cleanup job and its sweep durations are read from. */
    private static final String FIXTURE_JOB = "job";
    private static final String FIXTURE_DURATIONS = "durations";

    /** The prefix that names a fixture member inside the operator facts, for failure messages. */
    private static final String PATH_FACTS = "facts.";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LOOPBACK = "loopback";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_PAIRED_BY = "paired_by";
    private static final String KEY_MOCK_DEVICE_1 = "mock-device-1";
    private static final String PATH_DEVICES = "/api/v1/web/devices";
    private static final String PATH_DEVICES_SLASH = "/api/v1/web/devices/";
    private static final String METHOD_DELETE = "DELETE";
    private static final String EVENT_STREAM_CLOSED = "stream.closed";
    private static final String KEY_REASON = "reason";
    private static final String KEY_DETAILS = "details";
    private static final String CONST_FIX_RELAY = "fix-relay";
    private static final String CONST_RELAY_HARDENING = "relay-hardening";
    private static final String CONST_PM_MCP_CLIENTS = "pm-mcp-clients";
    private static final String CONST_OUTCOME = "outcome";
    private static final String CONST_TIME_11_20 = "2026-10-16T11:20:00Z";
    private static final String CONST_RELAY_TOKEN_STORE = "relay-token-store";
    private static final String CONST_TIME_11_21 = "2026-10-16T11:21:00Z";
    private static final String CONST_RELAY_AUTH_V2 = "relay-auth-v2";
    private static final String CONST_DOCS_TYPO = "docs-typo";
    private static final String CONST_PM_MCP_CORE = "pm-mcp-core";
    private static final String PATH_REPORTS_PREFIX = "/api/v1/reports/";
    private static final String KEY_TERMINAL = "terminal";
    private static final String KEY_COUNT = "count";
    private static final String KEY_MARKED_COUNT = "marked_count";
    private static final String CONST_PLAN_MARSHALL = "plan-marshall";
    private static final String STEP_IDENTITY = "Identity";
    private static final String SUFFIX_ABANDON = "/abandon";
    private static final String PATH_ENTRIES_PREFIX = "/api/v1/entries/";
    private static final String SEGMENT_EVENTS = "/events";
    private static final String SEGMENT_LINKS = "/links/";
    private static final String SEGMENT_ENTRY_CHAT = "/chat";
    private static final String SEGMENT_ENTRY_CHAT_TURNS = SEGMENT_ENTRY_CHAT + "/turns";
    private static final String SEGMENT_ENTRY_CHAT_END = "/chat/end";
    private static final String SEGMENT_ENTRY_CHAT_PROPOSAL = SEGMENT_ENTRY_CHAT + "/proposal/";
    private static final String ACTION_ACCEPT = "accept";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_ABANDON = "abandon";
    private static final String ACTION_COMMON_FILES = "common_files";
    private static final String ACTION_ARCHIVE = "archive";
    private static final String ACTION_START = "start";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_AUTO = "auto";
    private static final Set<String> PLAN_CONTROL_ACTIONS = Set.of(
            PlanAction.PAUSE.value(), PlanAction.START.value(),
            PlanAction.CANCEL.value(), PlanAction.ABANDON.value());
    private static final Set<String> EPIC_FLAT_ACTIONS = Set.of(ACTION_AUTO);
    private static final String ACTION_RETRY = "retry";
    private static final String ACTION_ABORT = "abort";
    private static final String SEGMENT_ENTRY_CHAT_PROPOSAL_ACCEPT = SEGMENT_ENTRY_CHAT_PROPOSAL + ACTION_ACCEPT;
    private static final String SEGMENT_ENTRY_CHAT_PROPOSAL_REJECT = SEGMENT_ENTRY_CHAT_PROPOSAL + "reject";
    private static final String SEGMENT_PROJECT_ENTRIES = "/entries";
    private static final String SEGMENT_PROJECT_FILES = "/files";
    private static final String PATH_ENDED = "/api/v1/projects/";
    private static final String SEGMENT_ENDED_CHAT = "/ended/";
    private static final String SEGMENT_ENDED_CHAT_ACCEPT = "/ended/accept";
    private static final String ERR_PROJECT_NOT_FOUND = "project_not_found";
    private static final String ERR_SCOPE_NOT_FOUND = "scope_not_found";
    private static final String EVENT_PROJECT_CHANGED = "project.changed";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_NODE_ID = "node_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PHASE = "phase";
    private static final String PHASE_EXECUTE = "execute";
    private static final String KEY_EVENT = "event";
    private static final String CONST_GITHUB_PM_PREFIX = "https://github.com/plan-marshall/";
    private static final String CONST_TIME_10_14 = "2026-10-14T10:00:00Z";
    private static final String CONST_PROJECTS = "projects";
    private static final String PATH_PROJECTS = "/api/v1/projects";
    private static final String CONST_LEGACY_PORTAL = "legacy-portal";
    private static final String CONST_UNENROLLED = "unenrolled";
    private static final String PATH_ENROL = "/api/v1/enrol";
    private static final String SEGMENT_ENROL = "/enrol";
    private static final String SEGMENT_STORES_DIGEST = "/stores/digest";
    private static final String SEGMENT_STORES_ACCEPT_EDIT = "/stores/accept-edit";
    private static final String EVENT_PROJECT_UNENROLLED = "project.unenrolled";
    private static final String EVENT_STORE_EDIT_ACCEPTED = "store.edit_accepted";
    private static final String KEY_STORE_PATH = "store_path";
    private static final String KEY_CURRENT_SHA256 = "current_sha256";
    private static final String KEY_NEW_SHA256 = "new_sha256";
    private static final String KEY_STORE_PATH_QUERY = "path";
    private static final String ERR_STORE_NOT_FOUND = "store_not_found";
    private static final String ERR_CONFIRMATION_MISMATCH = "confirmation_mismatch";
    private static final String ENROL_STORE_PATH = ".plan/marshal.json";
    private static final String ENROL_RECORDED_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String ENROL_CURRENT_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final String CONST_DECISION = "decision";
    private static final String PATH_CONNECTION_STATUS = "/api/v1/status/connection";
    private static final String PATH_LAN_LISTENER = "/api/v1/web/listeners/lan";
    private static final String PATH_CREDENTIALS = "/api/v1/credentials/";
    private static final String EVENT_DEVICE_PAIRED = "device.paired";
    private static final String EVENT_LISTENER_CHANGED = "listener.changed";
    private static final String ERR_CLI_ONLY = "cli_only";
    private static final String ERR_WEB_LISTENER_CONFLICT = "web_listener_conflict";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_OVERLAP_MAX_FILES = "overlap_max_files";
    private static final String FIELD_PAIRED_FROM = "paired_from";
    private static final String FIELD_ONBOARDED = "onboarded";
    private static final String CONST_LAN_HOST = "mbp-oliver.local";
    private static final String CONST_LAN_FINGERPRINT = "4F:1A:9C:6D:2B:88:71:03:C5:9E:44:AF:20:DB:16:E2:07";
    private static final int DEFAULT_LAN_PORT = 7421;
    private static final String KEY_LANDING = "landing";
    private static final String KEY_METHOD = "method";
    private static final String KEY_LINKS = "links";
    private static final String KEY_HREF = "href";
    private static final String KEY_TEXT = "text";
    private static final String KEY_ASKING = "asking";
    private static final String KEY_POSTURE = "posture";
    private static final String KEY_DOCUMENTS_MODE = "documents_mode";
    private static final String KEY_CLEANUP_DURATIONS = "cleanup_durations";
    private static final String KEY_PLAN_DEFAULTS = "plan_defaults";
    private static final String KEY_PLANS_AFTER_DAYS = "plans_after_days";
    private static final String KEY_EPICS_AFTER_DAYS = "epics_after_days";
    private static final String KEY_LOGS_AFTER_DAYS = "logs_after_days";
    private static final String KEY_REPORTS_AFTER_DAYS = "reports_after_days";
    private static final String KEY_UNENROLLED_AFTER_DAYS = "unenrolled_after_days";
    private static final String KEY_REMIND_AFTER_DAYS = "remind_after_days";
    private static final String STATE_ONBOARD_COMPLETED = "onboard.completed";
    private static final String PROJECT_SETUP_IDENTITY = "project.identity";
    private static final String PROJECT_SETUP_COMPLETED = "project.completed";
    private static final String PROJECT_SETUP_READY_SUFFIX = " is ready";
    private static final String STATE_ONBOARD_SUMMARY = "onboard.summary";
    /** The engine-only fixture data a state's operator facts carry beside its domain facts. */
    private static final String FIXTURE_RUNTIME = "runtime";
    private static final String FIXTURE_DEVICES = "devices";
    private static final String FIXTURE_WEB = "web";
    private static final String FIXTURE_ONBOARD = "onboard";
    private static final String FIXTURE_SUMMARY = "summary";
    private static final String FIXTURE_LAN_CONFLICT = "lan_conflict";
    /** The runtime snapshot representation a system state shows. */
    private static final String FIXTURE_SNAPSHOT = "snapshot";
    /** The state-specific route overrides a state's runtime fixture may carry ("METHOD /path"). */
    private static final String FIXTURE_ROUTES = "routes";
    /** The state whose runtime fixture is the base every other state of the scenario inherits. */
    private static final String BASE_FIXTURE_STATE = "running_work.active";
    private static final String FIXTURE_CONTROL = "control";
    private static final String FIXTURE_DECISIONS = "decisions";
    private static final String FIXTURE_DECOMPOSITIONS = "decompositions";
    private static final String FIXTURE_REPORTS = "reports";
    private static final String FIXTURE_COMPLETION_REPORTS = "completion_reports";
    private static final String FIXTURE_PROJECTS = "projects";
    private static final String FIXTURE_BEING_ADDED = "being_added";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_MACHINE = "machine";
    private static final String FIELD_LISTENER = "listener";
    private static final String FIELD_STEPS = "steps";
    private static final String FIELD_COMPLETED = "completed";
    private static final String FIELD_LAN = "lan";
    private static final String FIELD_LAN_PORT = "lan_port";
    private static final String FIELD_LAN_FINGERPRINT = "lan_fingerprint";
    private static final String STATE_SETTINGS_PREFIX = "settings.";
    private static final String ERR_LOOPBACK_ONLY = "loopback_only";
    private static final String FIELD_SCOPE_KIND = "kind";
    private static final String CONSENT_PREFIX = "consent:";
    private static final String CREDENTIAL_PREFIX = "credential:";
    private static final String CONST_CLAUDE = "claude";
    private static final String CONST_SONNET = "sonnet-4.5";
    private static final List<String> CLEANUP_DAY_KEYS = List.of(KEY_PLANS_AFTER_DAYS,
            KEY_EPICS_AFTER_DAYS, KEY_LOGS_AFTER_DAYS, KEY_REPORTS_AFTER_DAYS, KEY_UNENROLLED_AFTER_DAYS);
    private static final String PREFIX_PROJECTS = "/api/v1/projects/";
    private static final String PATH_RECIPES = "/api/v1/recipes";
    private static final String PATH_INTAKE_DRAFT = "/api/v1/intake/draft";
    private static final String SUFFIX_PLANS_START = "/plans/start";
    private static final String SUFFIX_EPICS_START = "/epics/start";
    private static final String SUFFIX_PR_OBSERVE = "/sources/pr/observe";
    private static final String SUFFIX_PR_ADOPT = "/sources/pr/adopt";
    private static final String SUFFIX_LOCAL_PREVIEW = "/sources/local-changes/preview";
    private static final String SUFFIX_LOCAL_CAPTURE = "/sources/local-changes/capture";
    private static final String EVENT_PLAN_STARTED = "plan.started";
    private static final String EVENT_EPIC_STARTED = "epic.started";
    private static final String EVENT_INTAKE_CREATED = "intake.created";
    private static final String SEGMENT_EPICS = "/epics/";
    private static final String PATH_DECOMPOSITION = "/decomposition";
    private static final String SUFFIX_DECOMPOSITION_CHECK = "/decomposition/check";
    private static final String SUFFIX_DECOMPOSITION_INSTRUCT = "/decomposition/instruct";
    private static final String SUFFIX_DECOMPOSITION_COMMIT = "/decomposition/commit";
    private static final String EVENT_DECOMPOSITION_UPDATED = "epic.decomposition_updated";
    private static final String EVENT_EPIC_LAUNCHABLE = "epic.launchable";
    private static final String ERR_EPIC_NOT_FOUND = "epic_not_found";
    private static final String ERR_CHECKS_FAIL = "checks_fail";
    private static final String ERR_PLAN_NOT_FOUND = "plan_not_found";
    private static final String MESSAGE_PLAN_NOT_FOUND = "Plan not found: ";
    private static final String EVENT_PR_OBSERVED = "source.pr_observed";
    private static final String EVENT_LOCAL_CAPTURED = "source.local_changes_captured";
    private static final String ERR_PROJECT_NOT_READY = "project_not_ready";
    private static final String ERR_START_REFUSED = "start_refused";
    private static final String ERR_INVALID_REQUEST = "invalid_request";
    private static final String ERR_CONFIRMATION_REQUIRED = "confirmation_required";
    private static final String ERR_RATE_LIMITED = "rate_limited";
    private static final String ERR_NOT_OPEN = "not_open";
    private static final String ERR_HEAD_NOT_WRITABLE = "head_not_writable";
    private static final String ERR_SCREENING_FLAGGED = "screening_flagged";
    private static final String ERR_UNSCREENED = "unscreened";
    private static final String ERR_LANDING_FILES = "landing_files";
    private static final String KEY_PR_REFERENCE = "pr_url";
    private static final String KEY_PLAN_ID = "plan_id";
    private static final String KEY_SNAPSHOT_COMMIT = "snapshot_commit";
    private static final String KEY_LANE = "lane";
    private static final String ERR_PREVIEW_STALE = "preview_stale";
    private static final String ERR_NO_LOCAL_CHANGES = "no_local_changes";
    private static final String CONST_LANDING_PR = "pr";
    private static final String CONST_LANDING_FILES = "landing.files";
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");
    private static final String KEY_RECIPES = "recipes";
    private static final String SEGMENT_LESSON_SOURCES = "/sources/lessons";
    private static final String KEY_LESSONS = "lessons";
    private static final String KEY_LESSON_ID = "lesson_id";
    private static final String EVENT_LESSON_CLAIMED = "source.lesson_claimed";
    private static final String EVENT_LESSON_RETIRED = "source.lesson_retired";
    private static final String ERR_LESSON_NOT_FOUND = "lesson_not_found";
    private static final String ERR_LESSON_CLAIM_REFUSED = "lesson_claim_refused";
    private static final String REASON_LESSON_CLAIMED_ELSEWHERE = "lesson_claimed_elsewhere";
    private static final String REASON_LESSON_RETIRED = "lesson_retired";
    private static final String REASON_LESSON_PREMISE_BROKEN = "lesson_premise_broken";
    private static final String PATH_PLANS_PREFIX = "/api/v1/plans/";
    private static final String SEGMENT_ACTIONS = "/actions";
    private static final String SEGMENT_HOLDS = "/holds";
    private static final String SEGMENT_HOLDS_PATH = "/holds/";
    private static final String SEGMENT_DECOMPOSITION = "/decomposition";
    private static final String PATH_EPICS_PREFIX = "/api/v1/epics/";
    private static final String SUFFIX_INTENT = "/intent";
    private static final String SUFFIX_OUTLINE = "/outline";
    private static final String SEGMENT_METRICS_OVERSIZE = "/metrics/oversize";
    private static final String FIXTURE_PLAN_INTENT = "/api/v1/plans/fix-relay/intent";
    private static final String FIXTURE_PLAN_OUTLINE = "/api/v1/plans/fix-relay/outline";
    private static final String FIXTURE_EPIC_INTENT = "/api/v1/epics/relay-hardening/intent";
    private static final String FIXTURE_OVERSIZE = "/api/v1/projects/pm-mcp-clients/metrics/oversize";
    private static final String SEGMENT_DECISIONS = "/decisions/";
    private static final String SUFFIX_PROCESS = "/process";
    private static final String CONST_DOMAIN = "domain";
    private static final String CONST_STANDARD = "standard";
    private static final String CONST_COVERAGE = "coverage";
    private static final String CONST_MODULES = "modules";
    private static final String CONST_ALL_MODULES = "all 9";
    private static final DateTimeFormatter RECIPE_ID_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    private static final String PATH_WORKFLOWS = "/api/v1/workflows";
    private static final String PATH_PROJECTS_PREFIX = "/api/v1/projects/";
    private static final String SEGMENT_SETUP = "/setup";
    private static final String METHOD_POST = "POST";
    private static final String PATH_SYSTEM_CLEANUP = "/api/v1/system/cleanup/";
    private static final String SEGMENT_DELETE = "/delete";
    private static final String SEGMENT_WORKFLOWS = "/workflows";
    private static final String SEGMENT_CATALOGUE = "/catalogue";
    private static final String SEGMENT_VALIDATE = "/validate";
    private static final String EVENT_UNIT_PLACED = "unit.placed";
    private static final String EVENT_UNIT_LANDED = "unit.landed";
    private static final String EVENT_UNIT_REMOVED = "unit.removed";
    private static final String ERR_UNIT_NAME_INVALID = "unit_name_invalid";
    private static final String ERR_UNIT_NOT_DESIGNABLE = "unit_not_designable";
    private static final String ERR_NO_PROJECT_TO_COMPOSE = "no_project_to_compose";
    private static final String ERR_CONFLICT = "conflict";
    private static final String ERR_UNIT_INVALID = "unit_invalid";
    private static final String ERR_BAD_REQUEST = "bad_request";
    private static final String FIELD_UNIT = "unit";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_TARGET = "target";
    private static final String FIELD_FINDINGS = "findings";
    private static final String FIELD_CATALOGUE_DIGEST = "catalogue_digest";
    private static final String FIELD_DIGEST = "digest";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_RELEASE = "release";
    private static final String FIELD_UNIT_KIND = "unit_kind";
    private static final Pattern UNIT_NAME = Pattern.compile("^[a-z][a-z0-9_.-]*$");

    private final ScenarioRegistry registry;
    private final String runtimeToken;
    private final String deviceSecret;
    private final ScheduledExecutorService scheduler;

    /** The bounded ring buffer capacity per stream scope ({@code 05-mock-runtime.adoc}). */
    private static final int EVENT_BUFFER_CAPACITY = 64;
    private static final String STREAM_MACHINE = "machine";
    private static final String STREAM_REPORTS = "reports";
    private static final String STREAM_PLAN_PREFIX = "plan:";
    private static final String STREAM_EPIC_PREFIX = "epic:";
    private static final String STREAM_ENTRY_PREFIX = "entry:";
    private static final String STREAM_WORKSPACE_PREFIX = "workspace:";
    private static final String STREAM_ACTIVITY_PREFIX = "activity:";

    /** Open streams and the subscription scope each one asked for. */
    private final Map<MockEventStream, String> activeStreams = new ConcurrentHashMap<>();
    private final Object eventLock = new Object();
    private final Map<String, Deque<BufferedEvent>> eventBuffers = new HashMap<>();
    private final Map<String, Long> streamSequences = new HashMap<>();
    /** Frames broadcast between stream registration and the end of its replay, in arrival order. */
    private final Map<MockEventStream, List<FramedEvent>> pendingFrames = new ConcurrentHashMap<>();
    private final Set<MockEventStream> readyStreams = ConcurrentHashMap.newKeySet();
    private final Map<MockEventStream, ReplayPlan> replayPlans = new ConcurrentHashMap<>();
    private final List<ScheduledFuture<?>> pendingTasks = new ArrayList<>();

    /**
     * One buffered event of a stream scope, replayable after a {@code Last-Event-ID}.
     *
     * @param id    the id within the stream scope
     * @param event the catalogue name
     * @param json  the serialized envelope
     */
    private record BufferedEvent(long id, String event, String json) {
    }

    /**
     * The replay a new stream owes before live delivery starts.
     *
     * @param events the buffered events after {@code Last-Event-ID}
     * @param reset  {@code true} when the id is stale and a {@code reset} is owed instead
     */
    private record ReplayPlan(List<BufferedEvent> events, boolean reset) {
    }

    /**
     * A plan a start flow created in this run, so its process view stays retrievable.
     *
     * @param title     the plan's title
     * @param projectId the project the start named
     */
    private record StartedPlan(String title, String projectId) {
    }

    private final AtomicReference<Scenario> currentScenario = new AtomicReference<>();
    private final AtomicReference<String> currentStateId = new AtomicReference<>();
    /**
     * The state whose operator fixture seeds the engine's domain state: the scenario's start state
     * until a jump enters another state, then the state the jump entered (UC-S-01).
     */
    private final AtomicReference<String> fixtureStateId = new AtomicReference<>();
    /**
     * The operator links offered by the states entered so far, as {@code METHOD href} to the state id
     * that offered it. A request that matches no link of the current state but one of these, offered by
     * a state left meanwhile, is an act against a state left meanwhile, answered {@code act_not_available}
     * (409) as the real runtime answers it.
     */
    private final Map<String, String> offeredOperatorLinks = new ConcurrentHashMap<>();
    /**
     * The operator acts recorded since the last reset: every state-changing request an operator
     * client sent, in arrival order, with the state it was sent in. {@code GET /mock/acts} serves
     * them so a dialogue test can prove the act arrived at the runtime rather than only on the
     * screen (CLI-CC-S-01, CLI-CC-P-07).
     */
    private final List<Map<String, Object>> operatorActs = new CopyOnWriteArrayList<>();
    private final DesignerRegistry designer = new DesignerRegistry();
    private final Object devicesLock = new Object();
    private final List<PairedDevice> pairedDevices = new ArrayList<>();
    private final Map<String, String> devicePairedBy = new LinkedHashMap<>();
    private final Object reportsLock = new Object();
    private final List<ReportItem> mockReports = new ArrayList<>();
    private final Map<String, PlanCompletionReport> completionReports = new ConcurrentHashMap<>();
    private final Object decisionsLock = new Object();
    private final Map<String, DecisionEnvelope> openDecisions = new LinkedHashMap<>();
    private final Object systemLock = new Object();
    private volatile RuntimeLifecycleState lifecycleState = RuntimeLifecycleState.RUNNING;
    private volatile String drainingBy;
    private volatile boolean canWithdrawStop;
    /** The snapshot representation the seeding state shows, or {@code null} for the engine default. */
    private @Nullable Map<String, Object> systemSnapshotFixture;
    private CleanupJob activeCleanupJob;
    private final List<CleanupKeptItem> keptItems = new ArrayList<>();
    private CleanupDurations cleanupDurations;
    private final List<CleanupResidue> cleanupResidues = new ArrayList<>();
    private final Object projectsLock = new Object();
    private final List<EnrolledProject> enrolledProjects = new ArrayList<>();
    private final List<String> favouriteProjectIds = new ArrayList<>();
    private final List<BeingAddedProject> beingAddedProjects = new ArrayList<>();
    private final Map<String, String> projectSetupStates = new ConcurrentHashMap<>();
    private final Object storeLock = new Object();
    private final Map<String, MockStore> mockStores = new LinkedHashMap<>();
    private final StartRegistry startRegistry;
    private final Object startLock = new Object();
    private final Set<String> startedPlanIds = new LinkedHashSet<>();
    private final Map<String, StartedPlan> startedPlans = new ConcurrentHashMap<>();
    private final Set<String> startedEpicIds = new LinkedHashSet<>();
    private int intakeDraftCounter;
    private final Object lessonSourceLock = new Object();
    private final Map<String, List<LessonSeed>> lessonSourceSeeds = new LinkedHashMap<>();
    private int gitSourceCounter;
    private final Map<String, String> localChangePreviews = new ConcurrentHashMap<>();
    private final Set<String> capturedProjects = ConcurrentHashMap.newKeySet();
    private volatile boolean lanListenerOn = true;
    private volatile int lanListenerPort = DEFAULT_LAN_PORT;
    private volatile int webPort = 7420;
    private volatile String currentPairingCode;
    private long pairingCounter;
    private volatile boolean closed;

    private final Object onboardingLock = new Object();
    private int machineSettingsRevision = 1;
    private String machineDocumentsMode = "ask";
    private Map<String, Object> machinePlanDefaults = new LinkedHashMap<>();
    private Map<String, Object> machineCleanupDurations = new LinkedHashMap<>();
    private final List<Map<String, Object>> machineRememberedConsents = new ArrayList<>();
    private final List<Map<String, Object>> machineCredentials = new ArrayList<>();
    private volatile boolean onboardingAuto;

    private final Object controlLock = new Object();
    private final Map<String, String> controlPlanStates = new LinkedHashMap<>();
    private final Map<String, String> controlPlanPausePoints = new LinkedHashMap<>();
    private final Map<String, String> controlPlanEpics = new LinkedHashMap<>();
    private final Map<String, String> controlEpicStates = new LinkedHashMap<>();
    private final Map<String, Boolean> controlEpicAuto = new LinkedHashMap<>();
    private final Map<String, Integer> controlEpicScope = new LinkedHashMap<>();
    private final Map<String, Integer> controlEpicRunning = new LinkedHashMap<>();
    private final Map<String, Integer> controlEpicOverlap = new LinkedHashMap<>();
    private final Map<String, HoldRecord> controlHolds = new LinkedHashMap<>();
    private final Map<String, Integer> controlSettingsRevisions = new LinkedHashMap<>();
    private final Map<String, Map<String, PlanSettingsValue>> controlSettings = new LinkedHashMap<>();
    private final Map<String, EpicResource> controlEpics = new LinkedHashMap<>();
    private final Map<String, ChatReadBackResource> controlChats = new LinkedHashMap<>();
    private final Map<String, ReviewResource> controlReviews = new LinkedHashMap<>();
    private final Map<String, ArchiveCheck> controlArchiveChecks = new LinkedHashMap<>();

    private final Object decompositionLock = new Object();
    private final Map<String, EpicDecomposition> decompositions = new LinkedHashMap<>();

    /**
     * @param registry     the scenario registry
     * @param scenario     the initial scenario
     * @param runtimeToken the accepted runtime token on the Unix domain socket
     * @param deviceSecret the accepted device secret on the web port
     */
    public MockRuntimeEngine(ScenarioRegistry registry, Scenario scenario, String runtimeToken, String deviceSecret) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.runtimeToken = Objects.requireNonNull(runtimeToken, "runtimeToken");
        this.deviceSecret = Objects.requireNonNull(deviceSecret, "deviceSecret");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("mock-engine-scheduler").factory());
        this.startRegistry = new StartRegistry(event -> broadcastEvent(event.name(), event.data()));
        setScenario(scenario);
    }

    /**
     * Switches the active scenario by name.
     *
     * @param nameOrPath scenario name or path
     * @return the newly activated scenario
     */
    public synchronized Scenario switchScenario(String nameOrPath) {
        var scenario = registry.resolve(nameOrPath);
        setScenario(scenario);
        return scenario;
    }

    private synchronized void setScenario(Scenario scenario) {
        // Validate the candidate's fixtures before anything is cleared, so a failed switch leaves
        // the active scenario and every stateful fixture untouched.
        validateRuntimeFixture(scenario);
        this.currentScenario.set(scenario);
        this.currentStateId.set(scenario.start());
        this.fixtureStateId.set(scenario.start());
        this.currentPairingCode = scenario.pairingCode();
        this.pairingCounter = 0;
        offeredOperatorLinks.clear();
        operatorActs.clear();
        seedStateFixtures();
        designer.init(scenario);
        transitionTo(scenario.start());
        broadcastReset();
    }

    /**
     * Resets the active scenario back to its start state.
     */
    public synchronized void reset() {
        offeredOperatorLinks.clear();
        operatorActs.clear();
        this.currentPairingCode = currentScenario.get().pairingCode();
        this.pairingCounter = 0;
        this.fixtureStateId.set(currentScenario.get().start());
        seedStateFixtures();
        designer.init(currentScenario.get());
        transitionTo(currentScenario.get().start());
        broadcastReset();
    }

    /**
     * Seeds every engine-owned domain store from the current fixture state, so the state's operator
     * facts reach the resources that are served from the engine rather than from a scenario route
     * (UC-S-01).
     */
    private void seedStateFixtures() {
        initDefaultDevices();
        initDefaultReports();
        initDefaultDecisions();
        initDefaultSystem();
        initDefaultProjects();
        initDefaultEnrolment();
        initDefaultControl();
        initDefaultOnboarding();
        initDefaultStart();
        initDefaultDecomposition();
        initDefaultLessonSource();
    }

    private void initDefaultDevices() {
        synchronized (devicesLock) {
            pairedDevices.clear();
            devicePairedBy.clear();
            if (!seedDevicesFromStateFixture() && !seedDevicesFromFixture()) {
                pairedDevices.add(new PairedDevice("dev-1", "Firefox on mbp-oliver", KEY_LOOPBACK,
                        Instant.parse("2026-05-12T10:00:00Z"), Instant.parse("2026-10-16T14:02:00Z")));
                pairedDevices.add(new PairedDevice("dev-2", "Safari on iPad", "network",
                        Instant.parse("2026-05-20T10:00:00Z"), Instant.parse("2026-10-13T10:00:00Z")));
                pairedDevices.add(new PairedDevice("dev-3", "Chrome on build-box", KEY_LOOPBACK,
                        Instant.parse("2026-06-02T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z")));
            }
            seedListenerFromStatus();
        }
    }

    /**
     * Seeds the paired devices from a fixture the start state carries under its operator state's
     * {@code facts.runtime.devices}, so a consolidated scenario can show a different device list per
     * state. The state on start is the one a legacy alias resolves to, so a case that starts at its
     * own state receives its own fixture.
     *
     * @return {@code true} when the state fixture seeded the devices
     */
    private boolean seedDevicesFromStateFixture() {
        var fixtureState = fixtureStateId.get();
        var runtime = stateRuntimeFixture(fixtureState);
        if (runtime == null || !runtime.containsKey(FIXTURE_DEVICES)) {
            return false;
        }
        if (!(runtime.get(FIXTURE_DEVICES) instanceof List<?> devices)) {
            throw new IllegalStateException("facts.runtime.devices in state '"
                    + fixtureState + "' is no array");
        }
        try {
            var list = DeviceListResource.fromTree(devices);
            for (var device : list.devices()) {
                pairedDevices.add(new PairedDevice(
                        device.deviceId(),
                        device.label(),
                        device.pairedFrom(),
                        device.createdAt(),
                        device.lastUsedAt()));
                devicePairedBy.put(device.deviceId(), device.pairedBy());
            }
            return true;
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Malformed facts.runtime.devices in state '"
                    + fixtureState + "'", e);
        }
    }

    /**
     * Seeds the paired devices from a scenario's {@code GET /api/v1/web/devices} fixture, so a case
     * scenario starts with the devices it shows. The route is a fixture: the live list is served by
     * {@link #handleListDevices}.
     *
     * @return {@code true} when a fixture seeded the devices
     */
    private boolean seedDevicesFromFixture() {
        var route = currentScenario.get().route("GET", PATH_DEVICES);
        if (route == null) {
            return false;
        }
        try {
            var list = DeviceListResource.fromTree(route.representation());
            for (var device : list.devices()) {
                pairedDevices.add(new PairedDevice(
                        device.deviceId(),
                        device.label(),
                        device.pairedFrom(),
                        device.createdAt(),
                        device.lastUsedAt()));
                devicePairedBy.put(device.deviceId(), device.pairedBy());
            }
            return true;
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IOException | RuntimeException _) {
            pairedDevices.clear();
            devicePairedBy.clear();
            return false;
        }
    }

    /**
     * Seeds the LAN listener's live state from the scenario's {@code GET /api/v1/status} fixture: a
     * {@code web} object with {@code lan} and {@code lan_port}. A scenario without a {@code web}
     * object keeps the default of an open listener on {@link #DEFAULT_LAN_PORT}.
     */
    private void seedListenerFromStatus() {
        var runtime = stateRuntimeFixture(fixtureStateId.get());
        var web = runtime != null ? JsonTree.asObject(runtime.get(FIXTURE_WEB)) : null;
        if (web == null) {
            var route = currentScenario.get().route("GET", "/api/v1/status");
            web = route != null ? JsonTree.object(JsonTree.asObject(route.representation()), FIXTURE_WEB) : null;
        }
        if (web == null) {
            lanListenerOn = true;
            lanListenerPort = DEFAULT_LAN_PORT;
            return;
        }
        lanListenerOn = !Boolean.FALSE.equals(web.get(FIELD_LAN));
        var port = JsonTree.integer(web, FIELD_LAN_PORT);
        lanListenerPort = port != null && port > 0 ? port : DEFAULT_LAN_PORT;
    }

    /**
     * @param stateId a state of the active scenario
     * @return the engine-only fixture data the state's operator facts carry under {@code runtime},
     *         or {@code null} when the state or the fixture is absent
     */
    private @Nullable Map<String, Object> stateRuntimeFixture(@Nullable String stateId) {
        if (stateId == null) {
            return null;
        }
        return runtimeFixtureOf(currentScenario.get(), stateId);
    }

    /**
     * @param scenario a scenario
     * @param stateId  a state of the scenario
     * @return the engine-only fixture data the state's operator facts carry under {@code runtime},
     *         or {@code null} when the state or the fixture is absent
     */
    private static @Nullable Map<String, Object> runtimeFixtureOf(Scenario scenario, @Nullable String stateId) {
        if (stateId == null) {
            return null;
        }
        var state = scenario.state(stateId);
        if (state == null) {
            return null;
        }
        var representation = JsonTree.asObject(state.representation());
        if (representation == null) {
            return null;
        }
        var facts = JsonTree.asObject(representation.get(OperatorStateResource.FIELD_FACTS));
        if (facts == null) {
            return null;
        }
        return JsonTree.asObject(facts.get(FIXTURE_RUNTIME));
    }

    /**
     * @return the engine-only fixture data of the current state, or {@code null}
     */
    private @Nullable Map<String, Object> currentRuntimeFixture() {
        return stateRuntimeFixture(currentStateId.get());
    }

    /**
     * The engine-only fixture of the state that seeds the engine, merged over the scenario's base
     * fixture so a case state declares only what differs. The base is the {@code running_work.active}
     * state of the consolidated running-work scenario; a scenario that has no such state is seeded
     * by its own fixture state alone.
     *
     * @return the merged fixture, or {@code null} when neither the state nor the base carries one
     */
    private @Nullable Map<String, Object> startRuntimeFixture() {
        return mergedRuntimeFixture(currentScenario.get(), fixtureStateId.get());
    }

    /**
     * Merges the start state's fixture of a scenario over that scenario's base fixture, without
     * touching the engine state, so a candidate scenario can be validated before it is activated.
     *
     * @param scenario the candidate scenario
     * @return the merged fixture, or {@code null} when neither state carries one
     */
    private static @Nullable Map<String, Object> startRuntimeFixtureOf(Scenario scenario) {
        return mergedRuntimeFixture(scenario, scenario.start());
    }

    /**
     * @param scenario the scenario
     * @param stateId  the state whose fixture seeds the engine
     * @return that state's runtime fixture merged over the scenario's base fixture, or {@code null}
     *         when neither carries one
     */
    private static @Nullable Map<String, Object> mergedRuntimeFixture(Scenario scenario,
            @Nullable String stateId) {
        var base = runtimeFixtureOf(scenario, BASE_FIXTURE_STATE);
        var own = runtimeFixtureOf(scenario, stateId);
        if (base == null || own == null || own == base) {
            return own != null ? own : base;
        }
        var merged = new LinkedHashMap<String, Object>(base);
        own.forEach((key, value) -> {
            if (FIXTURE_CONTROL.equals(key) && value instanceof Map<?, ?> ownControl
                    && merged.get(key) instanceof Map<?, ?> baseControl) {
                var control = new LinkedHashMap<String, Object>();
                baseControl.forEach((baseKey, baseValue) -> control.put(String.valueOf(baseKey), baseValue));
                ownControl.forEach((ownKey, ownValue) -> control.put(String.valueOf(ownKey), ownValue));
                merged.put(key, control);
            } else {
                merged.put(key, value);
            }
        });
        return merged;
    }

    /**
     * @param method the HTTP method
     * @param path   the request path
     * @return the route the current state declares for the path first, the scenario route otherwise
     */
    private @Nullable RouteDefinition activeRoute(String method, String path) {
        var runtime = currentRuntimeFixture();
        if (runtime != null && runtime.get(FIXTURE_ROUTES) instanceof Map<?, ?> overrides) {
            var override = overrides.get(method.toUpperCase(Locale.ROOT) + " " + path);
            if (override instanceof Map<?, ?> representation) {
                return RouteDefinition.of(representation);
            }
        }
        return currentScenario.get().route(method, path);
    }

    /**
     * @param fixture the runtime fixture, may be {@code null}
     * @param key     the fixture member
     * @return the member's array of objects, empty when the member is absent
     * @throws IllegalStateException when the member is present but is not an array of objects
     */
    private static List<Map<String, Object>> fixtureObjects(@Nullable Map<String, Object> fixture, String key) {
        return fixtureObjects(fixture, key, "facts.runtime." + key);
    }

    private static List<Map<String, Object>> fixtureObjects(@Nullable Map<String, Object> fixture, String key,
            String path) {
        if (fixture == null || !fixture.containsKey(key)) {
            return List.of();
        }
        if (!(fixture.get(key) instanceof List<?> list)) {
            throw new IllegalStateException(path + " must be an array");
        }
        var result = new ArrayList<Map<String, Object>>(list.size());
        for (var element : list) {
            var map = JsonTree.asObject(element);
            if (map == null) {
                throw new IllegalStateException(path + " must be an array of objects");
            }
            result.add(map);
        }
        return result;
    }

    /**
     * @param fixture the runtime fixture, may be {@code null}
     * @param key     the fixture member
     * @return the member's object, or {@code null} when the member is absent
     * @throws IllegalStateException when the member is present but is not an object
     */
    private static @Nullable Map<String, Object> fixtureObject(@Nullable Map<String, Object> fixture, String key) {
        return fixtureObject(fixture, key, "facts.runtime." + key);
    }

    private static @Nullable Map<String, Object> fixtureObject(@Nullable Map<String, Object> fixture, String key,
            String path) {
        if (fixture == null || !fixture.containsKey(key)) {
            return null;
        }
        var map = JsonTree.asObject(fixture.get(key));
        if (map == null) {
            throw new IllegalStateException(path + " must be an object");
        }
        return map;
    }

    /**
     * Overlays the current state's fixture on the {@code GET /api/v1/status} representation: the
     * status and version of the case, the machine's onboarding progress, whether it completed, and
     * the enabled listeners. A state whose fixture names none keeps what the scenario route says.
     *
     * @param status the status representation to complete
     */
    private void applyStateRuntimeToStatus(Map<String, Object> status) {
        if (!StandardScenarios.ONBOARDING.equals(currentScenario.get().name())) {
            // The legacy scenarios declare their status in the route; only the consolidated
            // onboarding scenario derives it from the state it is in.
            return;
        }
        var stateId = currentStateId.get();
        var runtime = stateRuntimeFixture(stateId);
        applyRuntimeMembers(status, runtime);
        applyOnboardingProgress(status, stateId, runtime);
    }

    /**
     * Copies the fixture's status, version, machine, listener, projects, and web members to the
     * status representation. A {@code web} member of {@code null} removes the route's web object.
     *
     * @param status  the status representation to complete
     * @param runtime the state's runtime fixture, may be {@code null}
     */
    private void applyRuntimeMembers(Map<String, Object> status, @Nullable Map<String, Object> runtime) {
        if (runtime == null) {
            return;
        }
        copyStatusMember(status, runtime, KEY_STATUS);
        copyStatusMember(status, runtime, FIELD_VERSION);
        copyStatusMember(status, runtime, FIELD_MACHINE);
        copyStatusMember(status, runtime, FIELD_LISTENER);
        copyStatusMember(status, runtime, CONST_PROJECTS);
        if (!runtime.containsKey(FIXTURE_WEB)) {
            return;
        }
        if (runtime.get(FIXTURE_WEB) instanceof Map<?, ?> web) {
            status.put(FIXTURE_WEB, mergedWeb(web, status.get(FIXTURE_WEB)));
        } else {
            status.remove(FIXTURE_WEB);
        }
    }

    private Map<String, Object> mergedWeb(Map<?, ?> web, @Nullable Object routeWeb) {
        var merged = new LinkedHashMap<String, Object>();
        if (routeWeb instanceof Map<?, ?> currentWeb) {
            currentWeb.forEach((key, value) -> merged.put(String.valueOf(key), value));
        }
        web.forEach((key, value) -> merged.put(String.valueOf(key), value));
        merged.put(FIELD_LAN, lanListenerOn);
        merged.put(FIELD_LAN_PORT, lanListenerPort);
        if (lanListenerOn) {
            merged.put(FIELD_LAN_FINGERPRINT, CONST_LAN_FINGERPRINT);
        }
        return merged;
    }

    /**
     * Writes the onboarding progress and the {@code onboarded} flag the current state implies.
     *
     * @param status  the status representation to complete
     * @param stateId the current state
     * @param runtime the state's runtime fixture, may be {@code null}
     */
    private static void applyOnboardingProgress(Map<String, Object> status, @Nullable String stateId,
            @Nullable Map<String, Object> runtime) {
        var progress = onboardingProgressFor(stateId);
        var onboard = runtime != null && runtime.get(FIXTURE_ONBOARD) instanceof Map<?, ?> fixture
                ? fixture
                : progress;
        if (onboard != null) {
            status.put(FIXTURE_ONBOARD, onboard);
        }
        if ((runtime != null && runtime.get(FIELD_ONBOARDED) instanceof Boolean) || progress != null) {
            status.put(FIELD_ONBOARDED, machineOnboardedFor(stateId, runtime));
        }
    }

    private static void copyStatusMember(Map<String, Object> status, Map<String, Object> runtime, String member) {
        if (runtime.containsKey(member)) {
            status.put(member, runtime.get(member));
        }
    }

    /**
     * The onboarding progress the current state implies, for the states of the guided run whose id
     * names an aspect. A state of the entry carries its own progress in its runtime fixture instead.
     *
     * @param stateId the current state
     * @return the progress map, or {@code null} when the state belongs to no onboarding aspect
     */
    private static @Nullable Map<String, Object> onboardingProgressFor(@Nullable String stateId) {
        if (stateId == null) {
            return null;
        }
        if (STATE_ONBOARD_COMPLETED.equals(stateId) || stateId.startsWith(STATE_SETTINGS_PREFIX)) {
            return progressOf(STATE_ONBOARD_COMPLETED, 7, true);
        }
        if (STATE_ONBOARD_SUMMARY.equals(stateId)) {
            return progressOf(stateId, 7, false);
        }
        var aspect = MachineOnboardingState.aspectOf(stateId);
        return aspect == null ? null : progressOf(stateId, aspect.step() - 1, false);
    }

    private static Map<String, Object> progressOf(String stateId, int step, boolean completed) {
        return Map.of(KEY_STATE, stateId, "step", step, FIELD_STEPS, 7, FIELD_COMPLETED, completed);
    }

    /**
     * @param stateId the current state
     * @param runtime the state's runtime fixture, may be {@code null}
     * @return whether the machine's onboarding has completed in this state
     */
    private static boolean machineOnboardedFor(@Nullable String stateId,
            @Nullable Map<String, Object> runtime) {
        if (runtime != null && runtime.get(FIELD_ONBOARDED) instanceof Boolean onboarded) {
            return onboarded;
        }
        return stateId != null && (STATE_ONBOARD_COMPLETED.equals(stateId)
                || stateId.startsWith(STATE_SETTINGS_PREFIX));
    }

    /**
     * Jumps directly to a state within the current scenario.
     * <p>
     * The jump is a re-seed, not only a move: the domain stores — plans, epics, holdings, settings,
     * chats, reviews, decisions, reports, projects, start entries, the system lifecycle with its
     * cleanup job, and the paired devices — are wiped and re-seeded from the state's runtime
     * fixture, so operator-made state does not survive it.
     *
     * @param stateId the state identifier
     * @throws IllegalArgumentException if the state does not exist in the current scenario
     */
    public synchronized void goToState(String stateId) {
        var scenario = currentScenario.get();
        if (!scenario.states().containsKey(stateId)) {
            throw new IllegalArgumentException("State '" + stateId + "' does not exist in scenario '" + scenario.name() + "'");
        }
        var fixture = mergedRuntimeFixture(scenario, stateId);
        if (fixture != null) {
            validateFixtureMembers(fixture);
        }
        validateStateFacts(scenario, stateId);
        var previousStateId = this.fixtureStateId.get();
        this.fixtureStateId.set(stateId);
        try {
            seedStateFixtures();
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IllegalStateException failure) {
            this.fixtureStateId.set(previousStateId);
            seedStateFixtures();
            throw failure;
        }
        transitionTo(stateId);
    }

    /**
     * Decodes the state's operator facts before any store is touched, so a malformed fixture fails
     * the jump instead of leaving the engine seeded for a state it never entered.
     *
     * @param scenario the active scenario
     * @param stateId  the state being entered
     */
    private void validateStateFacts(Scenario scenario, String stateId) {
        var state = scenario.state(stateId);
        var representation = state != null ? JsonTree.asObject(state.representation()) : null;
        var facts = representation != null
                ? JsonTree.asObject(representation.get(OperatorStateResource.FIELD_FACTS))
                : null;
        if (facts == null) {
            return;
        }
        var job = JsonTree.object(facts, FIXTURE_JOB);
        if (job != null) {
            readFixture(() -> CleanupJob.fromMap(job), PATH_FACTS + FIXTURE_JOB);
        }
        var durations = JsonTree.object(facts, FIXTURE_DURATIONS);
        if (durations != null) {
            readFixture(() -> CleanupDurations.fromMap(durations), PATH_FACTS + FIXTURE_DURATIONS);
        }
    }

    private synchronized void transitionTo(String nextStateId) {
        if (closed) {
            return;
        }
        cancelPendingTasks();
        this.currentStateId.set(nextStateId);

        var stateDef = currentScenario.get().state(nextStateId);
        if (stateDef == null) {
            return;
        }

        recordOfferedOperatorLinks(stateDef, nextStateId);

        // Schedule events
        for (var eventDef : stateDef.events()) {
            var task = scheduler.schedule(() -> broadcastEvent(eventDef.event(), eventDef.data()),
                    eventDef.afterMs(), TimeUnit.MILLISECONDS);
            pendingTasks.add(task);
        }

        // Schedule auto transition
        if (stateDef.auto() != null) {
            var auto = stateDef.auto();
            var task = scheduler.schedule(() -> {
                synchronized (this) {
                    if (Objects.equals(this.currentStateId.get(), nextStateId)) {
                        transitionTo(auto.next());
                    }
                }
            }, auto.afterMs(), TimeUnit.MILLISECONDS);
            pendingTasks.add(task);
        }
    }

    private synchronized void cancelPendingTasks() {
        for (var task : pendingTasks) {
            task.cancel(false);
        }
        pendingTasks.clear();
    }

    /**
     * Records the operator links a state offers, so that an act against a later state's stale link is
     * refused {@code act_not_available} like the real runtime refuses it. Only a state whose
     * representation carries an operator {@code links} array contributes; the legacy scenarios whose
     * representations name no operator link keep answering {@code mock_no_route}.
     *
     * @param stateDef the state entered
     * @param stateId  the id of the state entered
     */
    private void recordOfferedOperatorLinks(StateDefinition stateDef, String stateId) {
        var representation = JsonTree.asObject(stateDef.representation());
        if (representation == null) {
            return;
        }
        var links = JsonTree.asArray(representation.get(KEY_LINKS));
        if (links == null) {
            return;
        }
        for (var element : links) {
            var link = JsonTree.asObject(element);
            if (link == null) {
                continue;
            }
            var method = JsonTree.string(link, KEY_METHOD);
            var href = JsonTree.string(link, KEY_HREF);
            if (method != null && href != null) {
                offeredOperatorLinks.put(method.toUpperCase() + " " + href, stateId);
            }
        }
    }

    /**
     * Broadcasts an event to all open SSE connections.
     *
     * @param event the event type
     * @param data  the event payload
     */
    public void broadcast(String event, Object data) {
        broadcastEvent(event, data);
    }

    /**
     * Broadcasts one event as a valid {@link EventEnvelope} to every stream whose scope it belongs
     * to, buffers it in that scope's bounded ring buffer, and drops a stream that fails to write.
     * The runtime-wide {@code /api/v1/events} stream receives every event; a scoped stream receives
     * only the events of its scope. A scenario event whose {@code data} already carries the envelope
     * members ({@code event}, {@code at}, {@code scope}) keeps them, so a fixture can pin the instant
     * and the scope it plays.
     *
     * @param event the event name
     * @param data  the event payload
     */
    private void broadcastEvent(String event, Object data) {
        var payload = payloadOf(data);
        var scope = scopeOf(event, payload);

        var targets = streamTargetsOf(event, scope, payload);
        var framed = new ArrayList<FramedEvent>();
        synchronized (eventLock) {
            for (var target : targets) {
                var id = nextStreamId(target);
                var envelope = envelopeJson(event, id, scope, payload);
                var buffer = eventBuffers.computeIfAbsent(target, key -> new ArrayDeque<>());
                buffer.addLast(new BufferedEvent(id, event, envelope));
                while (buffer.size() > EVENT_BUFFER_CAPACITY) {
                    buffer.removeFirst();
                }
                for (var entry : activeStreams.entrySet()) {
                    if (entry.getValue().equals(target)) {
                        framed.add(new FramedEvent(entry.getKey(), id, event, envelope));
                    }
                }
            }
            // Send under the same lock that assigned the ids: two broadcasts cannot interleave their
            // frames and make a stream's ids go backwards.
            framed.forEach(this::writeOrDrop);
        }
    }

    /**
     * @param stream the stream to write to
     * @param event  the event name
     * @param id     the event id
     * @param json   the envelope JSON
     */
    private record FramedEvent(MockEventStream stream, long id, String event, String json) {
    }

    private void writeOrDrop(FramedEvent framed) {
        synchronized (eventLock) {
            if (!readyStreams.contains(framed.stream())) {
                pendingFrames.computeIfAbsent(framed.stream(), key -> new ArrayList<>()).add(framed);
                return;
            }
            tryWrite(framed);
        }
    }

    private void tryWrite(FramedEvent framed) {
        try {
            framed.stream().sendJson(framed.event(), framed.id(), framed.json());
        } catch (IOException _) {
            dropStream(framed.stream());
        }
    }

    private void dropStream(MockEventStream stream) {
        activeStreams.remove(stream);
        readyStreams.remove(stream);
        pendingFrames.remove(stream);
        replayPlans.remove(stream);
    }

    private static Map<String, Object> payloadOf(Object data) {
        var payload = new LinkedHashMap<String, Object>();
        if (data instanceof Map<?, ?> map) {
            map.forEach((key, value) -> payload.put(String.valueOf(key), value));
        } else if (data != null) {
            payload.put("data", data);
        }
        return payload;
    }

    private long nextStreamId(String scope) {
        var id = streamSequences.merge(scope, 1L, Long::sum);
        return id;
    }

    /**
     * Derives the envelope scope from the payload: a plan, epic, or entry identifier the event
     * names, else the project's workspace scope, else the machine's reports, else the runtime.
     */
    private static EventScope scopeOf(String event, Map<String, Object> payload) {
        if (payload.get(EventEnvelope.FIELD_SCOPE) instanceof Map<?, ?> declared) {
            @SuppressWarnings("unchecked")
            var typed = (Map<String, Object>) declared;
            try {
                return EventScope.fromMap(typed);
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (RuntimeException | IOException _) {
                // A malformed fixture scope falls back to the identifiers in the payload.
            }
        }
        var planId = stringMember(payload, KEY_PLAN_ID);
        if (planId != null) {
            return new EventScope(EventScopeKind.PLAN, planId, stringMember(payload, KEY_PROJECT_ID));
        }
        var epicId = stringMember(payload, "epic_id");
        if (epicId != null) {
            return new EventScope(EventScopeKind.EPIC, epicId, stringMember(payload, KEY_PROJECT_ID));
        }
        var entryId = stringMember(payload, "entry_id");
        if (entryId != null) {
            return new EventScope(EventScopeKind.ENTRY, entryId, stringMember(payload, KEY_PROJECT_ID));
        }
        if (event.startsWith("report.")) {
            return new EventScope(EventScopeKind.MACHINE, null, null);
        }
        var projectId = stringMember(payload, KEY_PROJECT_ID);
        if (projectId != null) {
            return new EventScope(EventScopeKind.WORKSPACE, null, projectId);
        }
        return new EventScope(EventScopeKind.RUNTIME, null, null);
    }

    private static String stringMember(Map<String, Object> payload, String key) {
        var value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    /**
     * Builds the serialized envelope. A catalogue event is built through {@link EventEnvelope}, so a
     * malformed payload fails the same way the clients' codec fails it; an engine-internal name that
     * the closed catalogue does not yet carry is wrapped in the same {@code event}/{@code id}/{@code data}
     * shape without claiming catalogue membership.
     */
    private static String envelopeJson(String event, long id, EventScope scope, Map<String, Object> payload) {
        var at = Instant.now();
        var scopeMap = scope.toMap();
        if (payload.get(EventEnvelope.FIELD_AT) instanceof String text) {
            try {
                at = Instant.parse(text);
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (RuntimeException _) {
                // A fixture with a malformed instant keeps the current instant instead of failing the stream.
            }
        }
        if (payload.get(EventEnvelope.FIELD_SCOPE) instanceof Map<?, ?> fixedScope) {
            @SuppressWarnings("unchecked")
            var typed = (Map<String, Object>) fixedScope;
            try {
                scopeMap = EventScope.fromMap(typed).toMap();
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (RuntimeException | IOException _) {
                // Keep the derived scope when the fixture's scope is not a valid EventScope.
            }
        }

        var payloadWithoutEnvelope = new LinkedHashMap<String, Object>();
        for (var entry : payload.entrySet()) {
            if (!EventEnvelope.FIELD_EVENT.equals(entry.getKey())
                    && !EventEnvelope.FIELD_AT.equals(entry.getKey())
                    && !EventEnvelope.FIELD_SCOPE.equals(entry.getKey())) {
                payloadWithoutEnvelope.put(entry.getKey(), entry.getValue());
            }
        }

        var name = EventName.fromWireOrNull(event);
        if (name != null) {
            try {
                var rebuiltScope = EventScope.fromMap(scopeMap);
                var envelope = new EventEnvelope(name, id, at, rebuiltScope, payloadWithoutEnvelope).toMap();
                // The SSE event and id fields carry the envelope's outer members; the data line is the
                // envelope's data object, the shape every client's SseReader expects.
                return JsonTree.write(envelope.get(EventEnvelope.FIELD_DATA));
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (RuntimeException | IOException _) {
                // Fall through to the shape below: the payload keeps a member the envelope reserves.
            }
        }

        var data = new LinkedHashMap<String, Object>();
        data.put(EventEnvelope.FIELD_EVENT, event);
        data.put(EventEnvelope.FIELD_AT, at.toString());
        data.put(EventEnvelope.FIELD_SCOPE, scopeMap);
        data.putAll(payloadWithoutEnvelope);
        return JsonTree.write(data);
    }

    /**
     * @return every stream scope the event belongs to: the runtime-wide stream always, the scope it
     *         names, and the reports or task-activity stream it belongs to
     */
    private static List<String> streamTargetsOf(String event, EventScope scope, Map<String, Object> payload) {
        var targets = new LinkedHashSet<String>();
        targets.add(STREAM_MACHINE);
        switch (scope.kind()) {
            case PLAN -> targets.add(STREAM_PLAN_PREFIX + scope.id());
            case EPIC -> targets.add(STREAM_EPIC_PREFIX + scope.id());
            case ENTRY -> targets.add(STREAM_ENTRY_PREFIX + scope.id());
            case WORKSPACE -> targets.add(STREAM_WORKSPACE_PREFIX + scope.projectId());
            default -> {
                // The runtime and machine scopes are served by the runtime-wide stream.
            }
        }
        if (event.startsWith("report.")) {
            targets.add(STREAM_REPORTS);
        }
        if ("task.activity".equals(event) && scope.kind() == EventScopeKind.PLAN) {
            var taskId = stringMember(payload, "task_id");
            if (taskId != null) {
                targets.add(STREAM_ACTIVITY_PREFIX + scope.id() + ":" + taskId);
            }
        }
        return List.copyOf(targets);
    }

    /**
     * Dispatches an incoming request.
     *
     * @param request  the request
     * @param response the response
     * @param isWeb    {@code true} if arriving on the web listener, {@code false} if on the Unix socket
     * @throws IOException on I/O error
     */
    public void dispatch(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();

        // 1. Control endpoints under /mock/
        if (path.startsWith("/mock/")) {
            handleControlRequest(request, response);
            return;
        }

        // 2. Authentication check
        if (!isAuthorized(request, response, isWeb)) {
            return;
        }
        recordOperatorAct(request);

        // 2b. An act against a state left meanwhile is refused before any dynamic route serves it.
        if (handleStaleOperatorLink(request, response)) {
            return;
        }

        // 3. A scoped SSE subscription, before a domain handler can swallow the path.
        var streamScope = "GET".equals(request.method()) ? streamScopeOf(path) : null;
        if (streamScope != null) {
            handleEventStream(request, response, streamScope);
            return;
        }

        // 4. The MCP endpoint of the relay, beside the operator API.
        if ("/mcp".equals(path)) {
            handleMcp(request, response);
            return;
        }

        // 5. A scenario link or static route outside the engine's live domain areas wins over the
        //    domain defaults: a scenario definition is never shadowed by a Java fixture, and an
        //    operator link under /links/ is always served as the scenario declares it.
        var route = activeRoute(request.method(), path);
        if (!isLiveDomainPath(path)) {
            if (handleStateLink(request, response)) {
                return;
            }
            if (route != null) {
                handleRouteWithDynamicStatus(route, path, response);
                return;
            }
        }

        // 6. Dynamic routes of the domain areas.
        if (handleDynamicRoutes(request, response, isWeb)) {
            return;
        }

        // 7. A live path no domain handler claimed falls back to the scenario's link or route.
        if (handleStateLink(request, response)) {
            return;
        }
        if (route != null) {
            handleRouteWithDynamicStatus(route, path, response);
            return;
        }

        // 8. No match: answer mock_no_route
        handleNoRoute(request, response);
    }

    /**
     * The operator API families the engine serves with stateful handlers (mutations, revisions,
     * live lists). A scenario that declares a link or route on one of these paths describes the
     * state the handler plays; the handler is the richer answer, so it wins. Every other path — in
     * particular an operator link below {@code /links/} — is served by the scenario first.
     *
     * @param path the request path
     * @return {@code true} when a stateful domain handler owns the family
     */
    static boolean isLiveDomainPath(String path) {
        return LIVE_DOMAIN_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * The path families owned by the stateful domain handlers, in one place. A family appears here
     * exactly when {@link #handleDynamicRoutes} has a handler for it; a representative path of every
     * family is asserted in {@code MockRuntimeEngineTest} so a new family cannot be forgotten.
     */
    private static final List<String> LIVE_DOMAIN_PREFIXES = List.of(
            "/api/v1/reports",
            "/api/v1/plans",
            "/api/v1/epics",
            "/api/v1/entries",
            "/api/v1/projects",
            "/api/v1/project-onboardings",
            "/api/v1/explorer",
            "/api/v1/overview",
            "/api/v1/workflows",
            "/api/v1/decisions",
            "/api/v1/questions",
            "/api/v1/system",
            "/api/v1/operator/runtime",
            "/api/v1/runtime",
            "/api/v1/recipes",
            "/api/v1/intake",
            "/api/v1/onboarding",
            "/api/v1/web",
            "/api/v1/status/",
            "/api/v1/enrol",
            "/api/v1/credentials",
            "/api/v1/machine/decisions",
            "/api/v1/machine/settings");

    private boolean handleDynamicRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb)
            throws IOException {
        return handleDeviceRoutes(request, response, isWeb)
                || handleOnboardingRoutes(request, response)
                || handleMachineSettingsRoutes(request, response)
                || handleDesignerRoutes(request, response, isWeb)
                || handleEnrolmentRoutes(request, response)
                || handleProjectExplorerRoutes(request, response)
                || handleProjectOverviewRoutes(request, response)
                || handleReportRoutes(request, response, isWeb)
                || handleIntentRoutes(request, response)
                || (declaresDecisions() && handleDecisionRoutes(request, response, isWeb))
                || (declaresSystem() && handleSystemRoutes(request, response, isWeb))
                || handleProcessRoutes(request, response)
                || handleDecompositionRoutes(request, response)
                || handleGitSourceRoutes(request, response)
                || handleStartRoutes(request, response, isWeb)
                || handleLessonSourceRoutes(request, response)
                || handleControlRoutes(request, response);
    }

    /**
     * @return {@code true} when the active scenario declares the system domain (by name, route, or
     *         state prefix), so its stateful handlers serve it
     */
    private boolean declaresSystem() {
        var scenario = currentScenario.get();
        return StandardScenarios.SYSTEM.equals(scenario.name())
                || StandardScenarios.REPORTS_AND_SYSTEM.equals(scenario.name())
                || StandardScenarios.FULLY_ONBOARDED.equals(scenario.name())
                || scenario.routes().keySet().stream().anyMatch(route -> route.contains("/api/v1/system"))
                || scenario.states().keySet().stream().anyMatch(state -> state.startsWith("system."));
    }

    /**
     * @return {@code true} when the active scenario declares the decisions domain (by name, route,
     *         or state id), so its stateful handlers serve it
     */
    private boolean declaresDecisions() {
        var scenario = currentScenario.get();
        return StandardScenarios.DECISIONS.equals(scenario.name())
                || StandardScenarios.RUNNING_WORK.equals(scenario.name())
                || StandardScenarios.FULLY_ONBOARDED.equals(scenario.name())
                || scenario.routes().keySet().stream().anyMatch(route -> route.contains("/decisions"))
                || scenario.states().keySet().stream().anyMatch(state -> state.contains("decision"));
    }

    private boolean isAuthorized(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        if ("/api/v1/messages".equals(path) || path.startsWith("/api/v1/messages/")) {
            handleMessages(request, response);
            return false;
        }

        return isWeb ? authorizeWeb(request, response) : authorizeLocal(request, response);
    }

    private boolean authorizeWeb(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("/api/v1/pair".equals(path) || "/pair".equals(path)) {
            handlePairing(request, response);
            return false;
        }
        var auth = request.header("Authorization");
        if (auth == null || !auth.equals("Bearer " + deviceSecret)) {
            response.json(401, Map.of(KEY_ERROR, "unauthorized", KEY_MESSAGE, "Invalid or missing device secret"));
            return false;
        }
        return true;
    }

    private boolean authorizeLocal(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var auth = request.header("Authorization");
        if (auth == null || !auth.equals("Bearer " + runtimeToken)) {
            var jobToken = request.header("PM-MCP-Job-Token");
            if (jobToken == null || !request.path().startsWith("/mcp")) {
                response.json(401, Map.of(KEY_ERROR, "unauthorized", KEY_MESSAGE, "Invalid or missing runtime token"));
                return false;
            }
        }
        return true;
    }

    private boolean handleStateLink(MockHttpRequest request, MockHttpResponse response) throws IOException {
        StateDefinition state;
        String activeStateId;
        synchronized (this) {
            activeStateId = this.currentStateId.get();
            state = currentScenario.get().state(activeStateId);
        }

        if (state == null) {
            return false;
        }

        var matched = findMatchingLink(request, state);
        if (matched == null) {
            return false;
        }

        var linkName = matched.getKey();
        var linkDef = matched.getValue();

        if (linkDef.delayMs() > 0) {
            try {
                Thread.sleep(linkDef.delayMs());
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }

        if (linkDef.status() >= 400) {
            var errorPayload = Map.of(
                    KEY_ERROR, linkDef.error() != null ? linkDef.error() : "rejected",
                    KEY_STATE, activeStateId,
                    "link", linkName
            );
            response.json(linkDef.status(), errorPayload);
            return true;
        }

        synchronized (this) {
            if (!Objects.equals(this.currentStateId.get(), activeStateId)) {
                var stale = new LinkedHashMap<String, Object>();
                stale.put(KEY_ERROR, "act_not_available");
                stale.put(KEY_MESSAGE, "The state '" + activeStateId + "' was left meanwhile");
                stale.put(KEY_STATE, this.currentStateId.get());
                response.json(409, stale);
                return true;
            }
            transitionTo(linkDef.next());
            state = currentScenario.get().state(this.currentStateId.get());
        }

        var nextRep = state != null ? state.representation() : Map.of();
        response.json(linkDef.status(), nextRep);
        return true;
    }

    /**
     * Answers an act against a state left meanwhile. A request that is not offered by the current
     * state but was offered by an earlier state of this scenario is refused {@code act_not_available};
     * a request the current state offers (for example with another method) is left to the normal
     * routing, and a request never offered falls through to {@code mock_no_route}.
     *
     * @param request  the request
     * @param response the response
     * @return {@code true} if the request was answered here
     * @throws IOException on write failure
     */
    private boolean handleStaleOperatorLink(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String activeStateId;
        StateDefinition state;
        synchronized (this) {
            activeStateId = this.currentStateId.get();
            state = currentScenario.get().state(activeStateId);
        }

        var requestKey = request.method().toUpperCase() + " " + request.path();
        var offeredBy = offeredOperatorLinks.get(requestKey);
        if (offeredBy == null || offeredBy.equals(activeStateId)) {
            return false;
        }
        if (state != null && findMatchingLink(request, state) != null) {
            return false;
        }

        var body = new LinkedHashMap<String, Object>();
        body.put(KEY_ERROR, "act_not_available");
        body.put(KEY_MESSAGE, requestKey + " belongs to the state '" + offeredBy + "' left meanwhile");
        body.put(KEY_STATE, activeStateId);
        response.json(409, body);
        return true;
    }

    private void handleNoRoute(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String activeStateId = this.currentStateId.get();
        var state = currentScenario.get().state(activeStateId);
        var offeredLinks = state != null ? new ArrayList<>(state.links().keySet()) : List.<String>of();
        var errorBody = new LinkedHashMap<String, Object>();
        errorBody.put(KEY_ERROR, "mock_no_route");
        errorBody.put(KEY_MESSAGE, "No route matched for " + request.method() + " " + request.path() + " in state '" + activeStateId + "'");
        errorBody.put(KEY_STATE, activeStateId);
        errorBody.put("offered_links", offeredLinks);

        response.json(404, errorBody);
    }

    private Map.Entry<String, LinkDefinition> findMatchingLink(MockHttpRequest request, StateDefinition state) {
        var method = request.method();
        var path = request.path();

        for (var entry : state.links().entrySet()) {
            var linkName = entry.getKey();
            var linkDef = entry.getValue();

            if (!method.equalsIgnoreCase(linkDef.method())) {
                continue;
            }
            if (matchesLinkTarget(linkDef, linkName, path, state)) {
                return entry;
            }
        }
        return null;
    }

    private boolean matchesLinkTarget(LinkDefinition linkDef, String linkName, String path, StateDefinition state) {
        if (linkDef.href() != null && linkDef.href().equals(path)) {
            return true;
        }
        // The operator link convention puts the id below /links/; a deeper domain path that merely
        // ends in the same word (…/reports/rep-0142/read) is not this link.
        if (path.endsWith(SEGMENT_LINKS + linkName)) {
            return true;
        }
        return matchesRepresentationLink(state, linkName, path);
    }

    private boolean matchesRepresentationLink(StateDefinition state, String linkName, String path) {
        if (!(state.representation() instanceof Map<?, ?> repMap)) {
            return false;
        }
        if (checkLinkGroup(repMap.get(KEY_LINKS), linkName, path)) {
            return true;
        }
        return checkLinkGroup(repMap.get("_links"), linkName, path);
    }

    private boolean checkLinkGroup(Object groupObj, String linkName, String path) {
        if (!(groupObj instanceof Map<?, ?> links) || !links.containsKey(linkName)) {
            return false;
        }
        var targetObj = links.get(linkName);
        if (targetObj instanceof Map<?, ?> targetMap && path.equals(targetMap.get(KEY_HREF))) {
            return true;
        }
        return path.equals(String.valueOf(targetObj));
    }

    private static String extractPairingCode(MockHttpRequest request) {
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    var code = JsonTree.string(parsed, "code");
                    if (code != null) {
                        return code;
                    }
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore parse errors from invalid body
            }
        }
        if (request.query() != null) {
            for (var part : request.query().split("&")) {
                if (part.startsWith("code=")) {
                    return part.substring(5);
                }
            }
        }
        return null;
    }

    private synchronized void handlePairing(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var supplied = extractPairingCode(request);
        var open = currentPairingCode;
        if (supplied == null || open == null
                || !PairingCode.normalize(open).equals(PairingCode.normalize(supplied))) {
            response.json(400, Map.of(
                    KEY_ERROR, ApiErrorCode.PAIRING_INVALID.wire(),
                    KEY_MESSAGE, "The pairing code is invalid"));
            return;
        }
        // The code is single use: it is consumed whether or not a device results from it.
        currentPairingCode = null;
        var label = "Mock Browser";
        synchronized (devicesLock) {
            boolean found = false;
            for (var d : pairedDevices) {
                if (KEY_MOCK_DEVICE_1.equals(d.deviceId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                pairedDevices.add(new PairedDevice(KEY_MOCK_DEVICE_1, label, KEY_LOOPBACK, Instant.now(), Instant.now()));
                devicePairedBy.put(KEY_MOCK_DEVICE_1, "pair");
            }
        }
        var event = new DevicePairedEvent(KEY_MOCK_DEVICE_1, label, KEY_LOOPBACK);
        broadcastEvent(EVENT_DEVICE_PAIRED, event.toMap());
        response.json(200, new PairingRedemptionResponse(
                deviceSecret, KEY_MOCK_DEVICE_1, label, KEY_LOOPBACK).toMap());
    }

    private synchronized void handleCreatePairing(MockHttpResponse response) throws IOException {
        if (currentPairingCode == null || !PairingCode.isValid(currentPairingCode)) {
            currentPairingCode = nextPairingCode();
        }
        var code = new PairingCode(currentPairingCode);
        var resource = PairingResource.of(
                code,
                loopbackUrl() + "/pair#code=" + code.display());
        response.json(201, resource.toMap());
    }

    /**
     * Generates a fresh, deterministic pairing code, never equal to the code that is open.
     *
     * @return the generated code, normalized without the dash
     */
    private String nextPairingCode() {
        String generated;
        do {
            pairingCounter++;
            var value = pairingCounter;
            var characters = new char[PairingCode.LENGTH];
            for (var i = 0; i < PairingCode.LENGTH; i++) {
                characters[i] = PairingCode.ALPHABET.charAt((int) (value % PairingCode.ALPHABET.length()));
                value = value / PairingCode.ALPHABET.length() + 7;
            }
            generated = new String(characters);
        } while (generated.equals(currentPairingCode));
        return generated;
    }

    private void handleListDevices(MockHttpResponse response) throws IOException {
        var list = new ArrayList<Map<String, Object>>();
        synchronized (devicesLock) {
            for (var dev : pairedDevices) {
                var map = new LinkedHashMap<String, Object>(dev.toMap());
                map.put(KEY_PAIRED_BY, devicePairedBy.getOrDefault(
                        dev.deviceId(), PairedDeviceResource.DEFAULT_PAIRED_BY));
                list.add(map);
            }
        }
        response.json(200, list);
    }

    private void handleRenameDevice(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        var sub = path.substring(PATH_DEVICES_SLASH.length());
        if (sub.endsWith("/rename")) {
            sub = sub.substring(0, sub.length() - "/rename".length());
        }
        var targetId = sub;
        String newLabel = null;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    newLabel = JsonTree.string(parsed, KEY_LABEL);
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore parse errors from invalid body
            }
        }
        if (newLabel == null || newLabel.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, "invalid_label", KEY_MESSAGE, "Device label must not be blank"));
            return;
        }

        PairedDevice updated = null;
        synchronized (devicesLock) {
            for (int i = 0; i < pairedDevices.size(); i++) {
                var dev = pairedDevices.get(i);
                if (dev.deviceId().equals(targetId) || String.valueOf(i + 1).equals(targetId)) {
                    updated = new PairedDevice(dev.deviceId(), newLabel, dev.pairedFrom(), dev.createdAt(), Instant.now());
                    pairedDevices.set(i, updated);
                    break;
                }
            }
        }

        if (updated != null) {
            var event = new DeviceRenamedEvent(
                    updated.deviceId(),
                    updated.label(),
                    updated.pairedFrom());
            broadcastEvent(DeviceRenamedEvent.EVENT_NAME, event.toMap());
            var body = new LinkedHashMap<String, Object>(updated.toMap());
            body.put(KEY_PAIRED_BY, devicePairedBy.getOrDefault(
                    updated.deviceId(), PairedDeviceResource.DEFAULT_PAIRED_BY));
            response.json(200, body);
        } else {
            response.json(404, Map.of(KEY_ERROR, "device_not_found", KEY_MESSAGE, "Device not found: " + targetId));
        }
    }

    private void handleRevokeSingleDevice(MockHttpResponse response, String path, boolean isWeb) throws IOException {
        var targetId = path.substring(PATH_DEVICES_SLASH.length());
        PairedDevice removed = null;
        synchronized (devicesLock) {
            for (int i = 0; i < pairedDevices.size(); i++) {
                var dev = pairedDevices.get(i);
                if (dev.deviceId().equals(targetId) || String.valueOf(i + 1).equals(targetId)) {
                    removed = pairedDevices.remove(i);
                    devicePairedBy.remove(removed.deviceId());
                    break;
                }
            }
        }

        if (removed != null) {
            broadcastDeviceRevoked(removed, isWeb);
            response.json(200, Map.of(KEY_STATUS, "revoked", KEY_DEVICE_ID, removed.deviceId(), KEY_LABEL, removed.label()));
        } else {
            response.json(404, Map.of(KEY_ERROR, "device_not_found", KEY_MESSAGE, "Device not found: " + targetId));
        }
    }

    private void handleRevokeAllDevices(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var query = request.query() != null ? request.query().toLowerCase() : "";
        boolean isOthers = query.contains("others");

        var removedList = new ArrayList<PairedDevice>();
        PairedDevice kept = null;
        synchronized (devicesLock) {
            if (isOthers && isWeb) {
                if (!pairedDevices.isEmpty()) {
                    kept = pairedDevices.getFirst();
                    for (int i = pairedDevices.size() - 1; i >= 1; i--) {
                        var removed = pairedDevices.remove(i);
                        devicePairedBy.remove(removed.deviceId());
                        removedList.add(removed);
                    }
                }
            } else {
                for (var dev : pairedDevices) {
                    devicePairedBy.remove(dev.deviceId());
                }
                removedList.addAll(pairedDevices);
                pairedDevices.clear();
            }
        }

        for (var dev : removedList) {
            broadcastDeviceRevoked(dev, isWeb);
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put(KEY_STATUS, "revoked");
        resp.put("revoked_count", removedList.size());
        resp.put("remaining_count", pairedDevices.size());
        if (kept != null) {
            resp.put("kept_label", kept.label());
        }
        response.json(200, resp);
    }

    private void broadcastDeviceRevoked(PairedDevice device, boolean isWeb) {
        var revoked = new DeviceRevokedEvent(device.deviceId(), device.label(), device.pairedFrom());
        broadcastEvent(DeviceRevokedEvent.EVENT_NAME, revoked.toMap());
        broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                KEY_REASON, StreamClosedEvent.DEVICE_REVOKED.wire(),
                KEY_DETAILS, "Revoked",
                "revoked_by", isWeb ? "another device" : "the terminal client",
                "revoked_at", "today at 14:10"
        ));
    }

    private void handleCloseLanListener(MockHttpResponse response) throws IOException {
        if (!lanListenerOn) {
            response.json(409, Map.of(
                    KEY_ERROR, ApiErrorCode.ACT_NOT_AVAILABLE.wire(),
                    KEY_MESSAGE, "The network listener is already closed"));
            return;
        }
        lanListenerOn = false;
        broadcastListenerChanged(ListenerState.CLOSED);
        broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                KEY_REASON, StreamClosedEvent.LISTENER_CLOSED.wire(),
                KEY_DETAILS, "The network listener was turned off"
        ));
        response.json(200, Map.of(FIELD_LAN, false, KEY_STATUS, "listener_off"));
    }

    private void handleUpdateWebConfig(MockHttpRequest request, MockHttpResponse response) throws IOException {
        boolean previousLan = lanListenerOn;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null && parsed.containsKey(FIELD_LAN)) {
                    lanListenerOn = Boolean.TRUE.equals(parsed.get(FIELD_LAN));
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore body parsing failures and retain existing listener state
            }
        }
        if (previousLan != lanListenerOn) {
            broadcastListenerChanged(lanListenerOn ? ListenerState.OPEN : ListenerState.CLOSED);
        }
        var resp = new LinkedHashMap<String, Object>();
        resp.put(FIELD_ENABLED, true);
        resp.put(FIELD_LAN, lanListenerOn);
        resp.put("port", webPort > 0 ? webPort : 7420);
        resp.put("open", true);
        response.json(200, resp);
    }

    private Map<String, Object> cliOnly(String act) {
        return Map.of(KEY_ERROR, ERR_CLI_ONLY, KEY_MESSAGE, act + " is a CLI-only act");
    }

    private void handleConnectionStatus(MockHttpResponse response) throws IOException {
        var resp = new LinkedHashMap<String, Object>();
        resp.put("loopback_url", loopbackUrl());
        resp.put("loopback_open", true);
        resp.put("lan_enabled", lanListenerOn);
        resp.put(FIELD_LAN_PORT, lanListenerPort);
        if (lanListenerOn) {
            resp.put("lan_url", lanListenerUrl());
            resp.put(FIELD_LAN_FINGERPRINT, CONST_LAN_FINGERPRINT);
        }
        synchronized (devicesLock) {
            resp.put("paired_devices_count", pairedDevices.size());
        }
        resp.put(FIELD_ONBOARDED, isMachineOnboarded());
        resp.put("listeners", listenerMaps());
        response.json(200, resp);
    }

    private List<Map<String, Object>> listenerMaps() {
        var port = webPort > 0 ? webPort : 7420;
        var loopback = new ListenerResource(ListenerKind.LOOPBACK, ListenerState.OPEN, port,
                loopbackUrl(), null);
        var lan = new ListenerResource(
                ListenerKind.LAN,
                lanListenerOn ? ListenerState.OPEN : ListenerState.CLOSED,
                lanListenerPort,
                lanListenerOn ? lanListenerUrl() : null,
                lanListenerOn ? CONST_LAN_FINGERPRINT : null);
        return List.of(loopback.toMap(), lan.toMap());
    }

    private void handleOpenLanListener(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var requestedPort = extractLanPort(request);
        var liveLoopbackPort = webPort > 0 ? webPort : 7420;
        if (requestedPort == liveLoopbackPort) {
            broadcastListenerChanged(ListenerState.PORT_OCCUPIED, requestedPort);
            response.json(409, Map.of(
                    KEY_ERROR, ERR_WEB_LISTENER_CONFLICT,
                    KEY_MESSAGE, "Port " + liveLoopbackPort + " is held by the loopback listener"));
            return;
        }
        lanListenerPort = requestedPort;
        lanListenerOn = true;
        broadcastListenerChanged(ListenerState.OPEN);
        var resp = new LinkedHashMap<String, Object>();
        resp.put(FIELD_ENABLED, true);
        resp.put("port", lanListenerPort);
        resp.put("host", CONST_LAN_HOST);
        resp.put("fingerprint", CONST_LAN_FINGERPRINT);
        resp.put("url", lanListenerUrl());
        response.json(200, resp);
    }

    private int extractLanPort(MockHttpRequest request) {
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                var port = parsed != null ? JsonTree.integer(parsed, "port") : null;
                if (port != null && port > 0) {
                    return port;
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore invalid body and keep the default port
            }
        }
        return DEFAULT_LAN_PORT;
    }

    private void handleStoreCredential(MockHttpResponse response, String path) throws IOException {
        var key = path.substring(PATH_CREDENTIALS.length());
        response.json(200, WebCredentialResource.stored(key).toMap());
    }

    private void broadcastListenerChanged(ListenerState state) {
        var event = new ListenerChangedEvent(ListenerKind.LAN, state, lanListenerPort);
        broadcastEvent(EVENT_LISTENER_CHANGED, event.toMap());
    }

    private void broadcastListenerChanged(ListenerState state, int port) {
        var event = new ListenerChangedEvent(ListenerKind.LAN, state, port);
        broadcastEvent(EVENT_LISTENER_CHANGED, event.toMap());
    }

    private String loopbackUrl() {
        var port = webPort > 0 ? webPort : 7420;
        return "http://127.0.0.1:" + port;
    }

    private String lanListenerUrl() {
        return "https://" + CONST_LAN_HOST + ":" + lanListenerPort;
    }

    private boolean isMachineOnboarded() {
        var stateId = currentStateId.get();
        var runtime = stateRuntimeFixture(stateId);
        if (runtime != null && runtime.get(FIELD_ONBOARDED) instanceof Boolean onboarded) {
            return onboarded;
        }
        if (onboardingProgressFor(stateId) != null) {
            return machineOnboardedFor(stateId, runtime);
        }
        var route = currentScenario.get().route("GET", "/api/v1/status");
        if (route == null || !(route.representation() instanceof Map<?, ?> map)) {
            return true;
        }
        if (Boolean.TRUE.equals(map.get(FIELD_ONBOARDED))) {
            return true;
        }
        if (map.get(FIXTURE_ONBOARD) instanceof Map<?, ?> onboard
                && (STATE_ONBOARD_COMPLETED.equals(onboard.get(KEY_STATE))
                || Boolean.TRUE.equals(onboard.get(FIELD_COMPLETED)))) {
            return true;
        }
        return map.get(FIELD_MACHINE) instanceof Map<?, ?> machine && Boolean.TRUE.equals(machine.get(FIELD_ONBOARDED));
    }

    private boolean handleDeviceRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        return switch (request.method()) {
            case "GET" -> handleGetDeviceRoutes(path, response);
            case METHOD_POST -> handlePostDeviceRoutes(request, response, path, isWeb);
            case "PATCH", "PUT" -> handlePutDeviceRoutes(request, response, path, isWeb);
            case METHOD_DELETE -> handleDeleteDeviceRoutes(request, response, path, isWeb);
            default -> false;
        };
    }

    private boolean handleGetDeviceRoutes(String path, MockHttpResponse response) throws IOException {
        if (PATH_DEVICES.equals(path)) {
            handleListDevices(response);
            return true;
        }
        if (PATH_CONNECTION_STATUS.equals(path)) {
            handleConnectionStatus(response);
            return true;
        }
        if ("/api/v1/web".equals(path)) {
            var resp = new LinkedHashMap<String, Object>();
            resp.put(FIELD_ENABLED, true);
            resp.put(FIELD_LAN, lanListenerOn);
            resp.put("port", webPort > 0 ? webPort : 7420);
            resp.put("open", true);
            response.json(200, resp);
            return true;
        }
        return false;
    }

    private boolean handlePostDeviceRoutes(MockHttpRequest request, MockHttpResponse response, String path, boolean isWeb) throws IOException {
        if ("/api/v1/web/pairings".equals(path)) {
            if (isWeb) {
                response.json(403, cliOnly("Issuing a pairing code"));
                return true;
            }
            handleCreatePairing(response);
            return true;
        }
        if (path.startsWith(PATH_DEVICES_SLASH)) {
            handleRenameDevice(request, response, path);
            return true;
        }
        return false;
    }

    private boolean handlePutDeviceRoutes(MockHttpRequest request, MockHttpResponse response, String path, boolean isWeb) throws IOException {
        if (PATH_LAN_LISTENER.equals(path)) {
            return handleLanListenerPut(request, response, isWeb);
        }
        if (path.startsWith(PATH_CREDENTIALS)) {
            return handleCredentialPut(request, response, path);
        }
        if (path.startsWith(PATH_DEVICES_SLASH)) {
            handleRenameDevice(request, response, path);
            return true;
        }
        if ("/api/v1/web".equals(path)) {
            handleUpdateWebConfig(request, response);
            return true;
        }
        return false;
    }

    private boolean handleLanListenerPut(MockHttpRequest request, MockHttpResponse response, boolean isWeb)
            throws IOException {
        if (isWeb) {
            response.json(403, cliOnly("Opening the network listener"));
            return true;
        }
        var runtime = currentRuntimeFixture();
        if (runtime != null && runtime.get(FIXTURE_LAN_CONFLICT) instanceof Map<?, ?> conflict) {
            broadcastListenerChanged(ListenerState.PORT_OCCUPIED, extractLanPort(request));
            response.json(409, Map.of(KEY_ERROR, ERR_WEB_LISTENER_CONFLICT, KEY_MESSAGE,
                    String.valueOf(conflict.get(KEY_MESSAGE))));
            return true;
        }
        var override = currentScenario.get().route("PUT", PATH_LAN_LISTENER);
        if (override != null) {
            handleRouteWithDynamicStatus(override, PATH_LAN_LISTENER, response);
            if (override.status() == 409) {
                broadcastListenerChanged(ListenerState.PORT_OCCUPIED, extractLanPort(request));
            }
            return true;
        }
        handleOpenLanListener(request, response);
        return true;
    }

    private boolean handleCredentialPut(MockHttpRequest request, MockHttpResponse response, String path)
            throws IOException {
        var runtime = currentRuntimeFixture();
        if (runtime != null && Boolean.TRUE.equals(runtime.get(ERR_LOOPBACK_ONLY))) {
            response.json(403, Map.of(KEY_ERROR, ERR_LOOPBACK_ONLY, KEY_MESSAGE,
                    "A credential is entered on the loopback listener only"));
            return true;
        }
        var override = currentScenario.get().route("PUT", path);
        if (override != null) {
            handleRouteWithDynamicStatus(override, path, response);
            return true;
        }
        handleStoreCredential(response, path);
        return true;
    }

    private boolean handleDeleteDeviceRoutes(MockHttpRequest request, MockHttpResponse response, String path, boolean isWeb) throws IOException {
        if (path.startsWith(PATH_DEVICES_SLASH)) {
            handleRevokeSingleDevice(response, path, isWeb);
            return true;
        }
        if (PATH_DEVICES.equals(path)) {
            handleRevokeAllDevices(request, response, isWeb);
            return true;
        }
        if (PATH_LAN_LISTENER.equals(path)) {
            handleCloseLanListener(response);
            return true;
        }
        return false;
    }

    private boolean handleDesignerRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb)
            throws IOException {
        var path = request.path();
        String projectId = null;
        String sub;
        if (path.startsWith(PATH_WORKFLOWS)) {
            sub = path.substring(PATH_WORKFLOWS.length());
        } else if (path.startsWith(PATH_PROJECTS_PREFIX)) {
            var rest = path.substring(PATH_PROJECTS_PREFIX.length());
            var marker = rest.indexOf(SEGMENT_WORKFLOWS);
            if (marker <= 0) {
                return false;
            }
            projectId = rest.substring(0, marker);
            if (projectId.isEmpty() || projectId.contains("/")) {
                return false;
            }
            sub = rest.substring(marker + SEGMENT_WORKFLOWS.length());
        } else {
            return false;
        }
        return dispatchDesignerSub(request, response, sub, projectId, isWeb);
    }

    private boolean dispatchDesignerSub(MockHttpRequest request, MockHttpResponse response, String sub,
            String projectId, boolean isWeb) throws IOException {
        var method = request.method();
        if (sub.isEmpty()) {
            return serveDesignerUnits(response, method, projectId);
        }
        if (SEGMENT_CATALOGUE.equals(sub)) {
            return serveDesignerCatalogue(response, method, projectId);
        }
        if (SEGMENT_VALIDATE.equals(sub)) {
            if (!METHOD_POST.equals(method)) {
                return false;
            }
            handleDesignerValidate(request, response);
            return true;
        }
        if (!sub.startsWith("/")) {
            return false;
        }
        return dispatchDesignerUnit(request, response, sub.substring(1), projectId, method, isWeb);
    }

    private boolean serveDesignerUnits(MockHttpResponse response, String method, String projectId)
            throws IOException {
        if (!"GET".equals(method)) {
            return false;
        }
        var units = projectId == null ? designer.machineUnits() : designer.projectUnits(projectId);
        response.json(200, units.stream().map(WorkflowUnit::toMap).toList());
        return true;
    }

    private boolean serveDesignerCatalogue(MockHttpResponse response, String method, String projectId)
            throws IOException {
        if (!"GET".equals(method)) {
            return false;
        }
        var catalogue = projectId == null ? designer.machineCatalogue() : designer.projectCatalogue(projectId);
        response.json(200, catalogue.toMap());
        return true;
    }

    private boolean dispatchDesignerUnit(MockHttpRequest request, MockHttpResponse response, String unit,
            String projectId, String method, boolean isWeb) throws IOException {
        if (unit.isEmpty() || unit.contains("/")) {
            response.json(400, unitNameInvalid(unit));
            return true;
        }
        var target = projectId == null ? DesignerRegistry.MACHINE : projectId;
        if ("PUT".equals(method)) {
            handleDesignerPlace(request, response, unit, target, isWeb);
            return true;
        }
        if (METHOD_DELETE.equals(method)) {
            handleDesignerRemove(response, unit, target);
            return true;
        }
        return false;
    }

    private static boolean isValidUnitName(String unit) {
        return unit != null && UNIT_NAME.matcher(unit).matches() && !unit.contains("..");
    }

    private static Map<String, Object> unitNameInvalid(String unit) {
        return Map.of(KEY_ERROR, ERR_UNIT_NAME_INVALID, KEY_MESSAGE,
                "The name is not a unit name in the canonical grammar", FIELD_UNIT, unit == null ? "" : unit);
    }

    private void handleDesignerValidate(MockHttpRequest request, MockHttpResponse response) throws IOException {
        WorkflowValidationRequest validationRequest;
        try {
            validationRequest = WorkflowValidationRequest.fromMap(readBodyMap(request));
        } catch (IOException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_BAD_REQUEST, KEY_MESSAGE, "the text is required"));
            return;
        }
        response.json(200, designer.validate(validationRequest).toMap());
    }

    private void handleDesignerPlace(MockHttpRequest request, MockHttpResponse response, String unit,
            String target, boolean isWeb) throws IOException {
        if (!isValidUnitName(unit)) {
            response.json(400, unitNameInvalid(unit));
            return;
        }
        var body = readBodyMap(request);
        var text = JsonTree.string(body, FIELD_TEXT);
        if (text == null) {
            response.json(400, Map.of(KEY_ERROR, ERR_BAD_REQUEST, KEY_MESSAGE, "the text is required"));
            return;
        }
        var existing = designer.findUnit(target, unit);
        if (existing != null && WorkflowUnit.GROUP_COMPILED_ONLY.equals(existing.group())) {
            response.json(409, Map.of(KEY_ERROR, ERR_UNIT_NOT_DESIGNABLE, KEY_MESSAGE,
                    unit + " is compiled only and can never be replaced"));
            return;
        }
        if (DesignerRegistry.MACHINE.equals(target) && designer.enrolledProjects().isEmpty()) {
            response.json(409, Map.of(KEY_ERROR, ERR_NO_PROJECT_TO_COMPOSE, KEY_MESSAGE,
                    "No project is enrolled: a unit for this machine is placed once a project exists"));
            return;
        }
        var bodyDigest = JsonTree.string(body, FIELD_DIGEST);
        if (bodyDigest != null && existing != null && !existing.levels().isEmpty()) {
            var currentDigest = existing.levels().getFirst().digest();
            if (currentDigest != null && !currentDigest.isBlank() && !currentDigest.equals(bodyDigest)) {
                response.json(409, Map.of(KEY_ERROR, ERR_CONFLICT, KEY_MESSAGE,
                        unit + " changed since the comparison", "current", existing.toMap()));
                return;
            }
        }
        var project = DesignerRegistry.MACHINE.equals(target) ? null : target;
        var result = designer.validate(new WorkflowValidationRequest(text,
                JsonTree.string(body, FIELD_UNIT_KIND), unit, project, bodyDigest));
        if (!result.valid()) {
            var resultMap = result.toMap();
            var payload = new LinkedHashMap<String, Object>();
            payload.put(KEY_ERROR, ERR_UNIT_INVALID);
            payload.put(KEY_MESSAGE, "the unit does not validate against its target");
            payload.put(FIELD_FINDINGS, resultMap.get(FIELD_FINDINGS));
            payload.put("stages", resultMap.get("stages"));
            response.json(409, payload);
            return;
        }
        var placed = designer.place(unit, target, text, JsonTree.string(body, FIELD_AUTHOR),
                JsonTree.string(body, FIELD_RELEASE));
        broadcastEvent(EVENT_UNIT_PLACED, Map.of(FIELD_UNIT, unit, FIELD_TARGET, target,
                FIELD_CATALOGUE_DIGEST, placed.catalogueDigest()));
        broadcastEvent(EVENT_UNIT_LANDED, Map.of(FIELD_UNIT, unit, FIELD_TARGET, target,
                KEY_STATUS, placed.status(), FIELD_CATALOGUE_DIGEST, placed.catalogueDigest(),
                "by_web", isWeb));
        response.json(200, placed.toMap());
    }

    private void handleDesignerRemove(MockHttpResponse response, String unit, String target) throws IOException {
        if (!isValidUnitName(unit)) {
            response.json(400, unitNameInvalid(unit));
            return;
        }
        var existing = designer.findUnit(target, unit);
        if (existing != null && WorkflowUnit.GROUP_COMPILED_ONLY.equals(existing.group())) {
            response.json(409, Map.of(KEY_ERROR, ERR_UNIT_NOT_DESIGNABLE, KEY_MESSAGE,
                    unit + " is compiled only and can never be removed"));
            return;
        }
        var removed = designer.remove(unit, target);
        broadcastEvent(EVENT_UNIT_REMOVED, Map.of(FIELD_UNIT, unit, FIELD_TARGET, target,
                FIELD_CATALOGUE_DIGEST, removed.catalogueDigest()));
        response.json(200, removed.toMap());
    }

    /**
     * Seeds the stateful control fixtures from the active scenario's runtime fixture. The fixture is
     * declared by the scenario ({@code facts.runtime.control}) so a case state shows its own plans,
     * epics, holds, settings, and chats; a scenario that declares none has no control state.
     */
    private void initDefaultControl() {
        synchronized (controlLock) {
            controlPlanStates.clear();
            controlPlanPausePoints.clear();
            controlPlanEpics.clear();
            controlEpicStates.clear();
            controlEpicAuto.clear();
            controlEpicScope.clear();
            controlEpicRunning.clear();
            controlEpicOverlap.clear();
            controlHolds.clear();
            controlSettingsRevisions.clear();
            controlSettings.clear();
            controlEpics.clear();
            controlChats.clear();
            controlReviews.clear();
            controlArchiveChecks.clear();

            var control = fixtureObject(startRuntimeFixture(), FIXTURE_CONTROL);
            if (control == null) {
                return;
            }
            var seed = readControlSeed(control);
            controlPlanStates.putAll(seed.planStates());
            controlPlanPausePoints.putAll(seed.pausePoints());
            controlPlanEpics.putAll(seed.planEpics());
            controlEpicStates.putAll(seed.epicStates());
            controlEpicAuto.putAll(seed.epicAuto());
            controlEpicScope.putAll(seed.epicScope());
            controlEpicRunning.putAll(seed.epicRunning());
            controlEpicOverlap.putAll(seed.epicOverlap());
            controlHolds.putAll(seed.holds());
            controlSettingsRevisions.putAll(seed.settingsRevisions());
            controlSettings.putAll(seed.settings());
            controlEpics.putAll(seed.epics());
            controlChats.putAll(seed.chats());
            controlReviews.putAll(seed.reviews());
            controlArchiveChecks.putAll(seed.archiveChecks());
        }
    }

    /**
     * The parsed control fixture, kept apart from the engine maps so a candidate scenario can be
     * read (and refused) before the active scenario's state is cleared.
     */
    private record ControlSeed(
    Map<String, String> planStates,
    Map<String, String> pausePoints,
    Map<String, String> planEpics,
    Map<String, String> epicStates,
    Map<String, Boolean> epicAuto,
    Map<String, Integer> epicScope,
    Map<String, Integer> epicRunning,
    Map<String, Integer> epicOverlap,
    Map<String, HoldRecord> holds,
    Map<String, Integer> settingsRevisions,
    Map<String, Map<String, PlanSettingsValue>> settings,
    Map<String, EpicResource> epics,
    Map<String, ChatReadBackResource> chats,
    Map<String, ReviewResource> reviews,
    Map<String, ArchiveCheck> archiveChecks) {
    }

    private static ControlSeed readControlSeed(Map<String, Object> control) {
        var planStates = new LinkedHashMap<String, String>();
        var pausePoints = new LinkedHashMap<String, String>();
        var planEpics = new LinkedHashMap<String, String>();
        for (var plan : fixtureObjects(control, "plans", "facts.runtime.control.plans")) {
            var planId = JsonTree.string(plan, "plan_id");
            if (planId == null || planId.isBlank()) {
                throw new IllegalStateException("facts.runtime.control.plans names a plan without plan_id");
            }
            var state = JsonTree.string(plan, "state");
            planStates.put(planId, state != null ? state : "running");
            var epicId = JsonTree.string(plan, "epic_id");
            if (epicId != null) {
                planEpics.put(planId, epicId);
            }
            var pausePoint = JsonTree.string(plan, "pause_point");
            if (pausePoint != null) {
                pausePoints.put(planId, pausePoint);
            }
        }

        var epicStates = new LinkedHashMap<String, String>();
        var epicAuto = new LinkedHashMap<String, Boolean>();
        var epicScope = new LinkedHashMap<String, Integer>();
        var epicRunning = new LinkedHashMap<String, Integer>();
        var epicOverlap = new LinkedHashMap<String, Integer>();
        var epics = new LinkedHashMap<String, EpicResource>();
        var archiveChecks = new LinkedHashMap<String, ArchiveCheck>();
        var reviews = new LinkedHashMap<String, ReviewResource>();
        for (var epic : fixtureObjects(control, "epics", "facts.runtime.control.epics")) {
            var epicId = JsonTree.string(epic, "epic_id");
            if (epicId == null || epicId.isBlank()) {
                throw new IllegalStateException("facts.runtime.control.epics names an epic without epic_id");
            }
            var state = JsonTree.string(epic, "state");
            epicStates.put(epicId, state != null ? state : "running");
            epicAuto.put(epicId, Boolean.TRUE.equals(epic.get("auto_start")));
            epicScope.put(epicId, intMember(epic, "scope"));
            epicRunning.put(epicId, intMember(epic, "running"));
            epicOverlap.put(epicId, intMember(epic, FIELD_OVERLAP_MAX_FILES));

            var resource = fixtureObject(epic, "resource",
                    "facts.runtime.control.epics." + epicId + ".resource");
            if (resource != null) {
                epics.put(epicId, readFixture(() -> EpicResource.fromMap(resource),
                        "facts.runtime.control.epics." + epicId + ".resource"));
            }
            var archiveCheck = fixtureObject(epic, "archive_check",
                    "facts.runtime.control.epics." + epicId + ".archive_check");
            if (archiveCheck != null) {
                archiveChecks.put(epicId, readFixture(() -> ArchiveCheck.fromMap(archiveCheck),
                        "facts.runtime.control.epics." + epicId + ".archive_check"));
            }
            if (epic.containsKey("reviews")) {
                var epicReviews = JsonTree.asObject(epic.get("reviews"));
                if (epicReviews == null) {
                    throw new IllegalStateException("facts.runtime.control.epics." + epicId
                            + ".reviews must be an object");
                }
                epicReviews.forEach((register, value) -> {
                    var map = JsonTree.asObject(value);
                    if (map == null) {
                        throw new IllegalStateException("facts.runtime.control.epics." + epicId
                                + ".reviews." + register + " must be an object");
                    }
                    reviews.put(reviewKey(epicId, String.valueOf(register)),
                            readFixture(() -> ReviewResource.fromMap(map),
                                    "facts.runtime.control.epics." + epicId + ".reviews." + register));
                });
            }
        }

        var holds = new LinkedHashMap<String, HoldRecord>();
        for (var hold : fixtureObjects(control, "holds", "facts.runtime.control.holds")) {
            var record = readFixture(() -> HoldRecord.fromMap(hold), "facts.runtime.control.holds");
            holds.put(holdKey(record.subjectId(), record.holdId()), record);
        }

        var settingsRevisions = new LinkedHashMap<String, Integer>();
        var settings = new LinkedHashMap<String, Map<String, PlanSettingsValue>>();
        for (var entry : fixtureObjects(control, "settings", "facts.runtime.control.settings")) {
            var planId = JsonTree.string(entry, "plan_id");
            if (planId == null || planId.isBlank()) {
                throw new IllegalStateException("facts.runtime.control.settings names no plan_id");
            }
            var record = readFixture(() -> PlanSettingsRecord.fromMap(entry),
                    "facts.runtime.control.settings." + planId);
            settingsRevisions.put(planId, (int) record.revision());
            var values = new LinkedHashMap<String, PlanSettingsValue>();
            record.values().forEach(value -> values.put(value.aspect(), value));
            settings.put(planId, values);
        }

        var chats = new LinkedHashMap<String, ChatReadBackResource>();
        for (var chat : fixtureObjects(control, "chats", "facts.runtime.control.chats")) {
            var record = readFixture(() -> ChatReadBackResource.fromMap(chat), "facts.runtime.control.chats");
            chats.put(record.subjectId(), record);
        }

        return new ControlSeed(planStates, pausePoints, planEpics, epicStates, epicAuto, epicScope,
                epicRunning, epicOverlap, holds, settingsRevisions, settings, epics, chats, reviews,
                archiveChecks);
    }

    private static String reviewKey(String epicId, String register) {
        return epicId + ":" + register;
    }

    private static int intMember(Map<String, Object> map, String key) {
        var value = JsonTree.integer(map, key);
        return value != null ? value : 0;
    }

    /**
     * Reads and refuses the candidate scenario's runtime fixture without activating it, so a
     * malformed fixture cannot leave the engine half-switched.
     *
     * @param scenario the candidate scenario
     */
    private static void validateRuntimeFixture(Scenario scenario) {
        var fixture = startRuntimeFixtureOf(scenario);
        if (fixture == null) {
            return;
        }
        validateFixtureMembers(fixture);
    }

    /**
     * Refuses one runtime fixture's malformed members, so a jump can be validated before it seeds
     * the engine.
     *
     * @param fixture the merged runtime fixture
     */
    private static void validateFixtureMembers(Map<String, Object> fixture) {
        var control = fixtureObject(fixture, FIXTURE_CONTROL);
        if (control != null) {
            readControlSeed(control);
        }
        for (var entry : fixtureObjects(fixture, FIXTURE_DECISIONS)) {
            DecisionEnvelope.fromMap(entry);
        }
        for (var cut : fixtureObjects(fixture, FIXTURE_DECOMPOSITIONS)) {
            readFixture(() -> EpicDecomposition.fromMap(cut), "facts.runtime.decompositions");
        }
        for (var report : fixtureObjects(fixture, FIXTURE_REPORTS)) {
            readFixture(() -> ReportItem.fromMap(report), "facts.runtime.reports");
        }
        for (var report : fixtureObjects(fixture, FIXTURE_COMPLETION_REPORTS)) {
            readFixture(() -> PlanCompletionReport.fromMap(report), "facts.runtime.completion_reports");
        }
        for (var project : fixtureObjects(fixture, FIXTURE_PROJECTS)) {
            readFixture(() -> EnrolledProject.fromMap(project), "facts.runtime.projects");
        }
        for (var beingAdded : fixtureObjects(fixture, FIXTURE_BEING_ADDED)) {
            readFixture(() -> BeingAddedProject.fromMap(beingAdded), "facts.runtime.being_added");
        }
        if (fixture.containsKey(FIXTURE_DEVICES)) {
            readFixture(() -> DeviceListResource.fromTree(fixture.get(FIXTURE_DEVICES)), "facts.runtime.devices");
        }
    }

    /**
     * Reads a fixture with a {@code pm-api} codec, refusing a malformed fixture with the fixture
     * path so the failure names the scenario data rather than an anonymous parse error.
     *
     * @param reader the codec call
     * @param path   the fixture path for the failure message
     * @param <T>    the decoded type
     * @return the decoded fixture
     */
    private static <T> T readFixture(FixtureSupplier<T> reader, String path) {
        try {
            return reader.get();
        } catch (IOException e) {
            throw new IllegalStateException("Malformed " + path + ": " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface FixtureSupplier<T> {

        /**
         * @return the decoded fixture
         * @throws IOException if the fixture is not the contract's shape
         */
        T get() throws IOException;
    }

    private void initDefaultStart() {
        synchronized (startLock) {
            startedPlanIds.clear();
            startedEpicIds.clear();
            intakeDraftCounter = 0;
            gitSourceCounter = 0;
            localChangePreviews.clear();
            capturedProjects.clear();
        }
        startRegistry.init(currentScenario.get(), fixtureStateId.get());
    }

    private void initDefaultLessonSource() {
        synchronized (lessonSourceLock) {
            lessonSourceSeeds.clear();
            lessonSourceSeeds.put(CONST_PM_MCP_CLIENTS, new ArrayList<>(List.of(
                    new LessonSeed("les-0007", "Retire lessons when a plan lands",
                            "A landed plan retires the lessons it claimed; any other ending releases them.",
                            Instant.parse("2026-10-14T15:36:00Z"), CONST_FIX_RELAY,
                            List.of("src/main/java/de/planmarshall/Lesson.java",
                                    "de.planmarshall.store.LessonStore#retire"),
                            List.of(), false, null, null, false),
                    new LessonSeed("les-0009", "Keep the lesson store append-only",
                            "The lesson store is append-only; a retirement is a record, never a rewrite.",
                            Instant.parse("2026-10-13T09:12:00Z"), CONST_RELAY_AUTH_V2,
                            List.of("src/main/java/de/planmarshall/store/LessonStore.java",
                                    "src/main/java/de/planmarshall/store/OldStore.java"),
                            List.of(new LessonPremiseEvidence(PremiseEvidenceKind.FILE,
                                            "src/main/java/de/planmarshall/store/OldStore.java",
                                            "src/main/java/de/planmarshall/store/OldStore.java", "a1b2c3d"),
                                    new LessonPremiseEvidence(PremiseEvidenceKind.SYMBOL,
                                            "de.planmarshall.store.OldStore#rewrite", null, "a1b2c3d")),
                            false, null, null, false),
                    new LessonSeed("les-0011", "Wire the cache with an ETag",
                            "Every cached read carries an ETag so a stale entry is recognized.",
                            Instant.parse("2026-10-10T11:00:00Z"), "cache-rewrite",
                            List.of("src/main/java/de/planmarshall/http/Cache.java"),
                            List.of(), false, null, "cache-rewrite", false),
                    new LessonSeed("les-0012", "Drop the legacy portal",
                            "The legacy portal was removed with the release.",
                            Instant.parse("2026-10-08T08:00:00Z"), CONST_RELAY_AUTH_V2,
                            List.of(), List.of(), false, null, null, true)
            )));
            lessonSourceSeeds.put(CONST_PM_MCP_CORE, new ArrayList<>(List.of(
                    new LessonSeed("les-0021", "Seed the store with its digest",
                            "A store is seeded with the digest of the records it holds.",
                            Instant.parse("2026-10-12T10:00:00Z"), CONST_PM_MCP_CORE,
                            List.of("src/main/java/de/planmarshall/store/Seeded.java"),
                            List.of(), false, null, null, false)
            )));
        }
    }

    private boolean handleLessonSourceRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if (!path.startsWith(PREFIX_PROJECTS)) {
            return false;
        }
        var rest = path.substring(PREFIX_PROJECTS.length());
        var marker = rest.indexOf(SEGMENT_LESSON_SOURCES);
        if (marker <= 0) {
            return false;
        }
        var projectId = rest.substring(0, marker);
        if (projectId.isEmpty() || projectId.contains("/")) {
            return false;
        }
        var sub = rest.substring(marker + SEGMENT_LESSON_SOURCES.length());
        if (sub.isEmpty()) {
            if (!"GET".equals(request.method())) {
                return false;
            }
            handleListUnclaimedLessons(response, projectId);
            return true;
        }
        if (!sub.startsWith("/")) {
            return false;
        }
        var tail = sub.substring(1);
        var slash = tail.lastIndexOf('/');
        if (slash <= 0 || slash == tail.length() - 1) {
            return false;
        }
        if (!METHOD_POST.equals(request.method())) {
            return false;
        }
        var lessonId = tail.substring(0, slash);
        var action = tail.substring(slash + 1);
        return switch (action) {
            case "premise-check" -> {
                handlePremiseCheck(response, projectId, lessonId);
                yield true;
            }
            case "claim" -> {
                handleClaimLesson(request, response, projectId, lessonId);
                yield true;
            }
            case "retire" -> {
                handleRetireLesson(request, response, projectId, lessonId);
                yield true;
            }
            default -> false;
        };
    }

    private void handleListUnclaimedLessons(MockHttpResponse response, String projectId) throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var lessons = new ArrayList<UnclaimedLessonSummary>();
        synchronized (lessonSourceLock) {
            var seeds = lessonSourceSeeds.getOrDefault(projectId, List.of());
            for (var seed : seeds) {
                if (seed.unclaimed()) {
                    lessons.add(seed.toSummary());
                }
            }
        }
        lessons.sort(Comparator.comparing(UnclaimedLessonSummary::recordedAt).reversed());
        response.json(200, Map.of(KEY_LESSONS, lessons.stream().map(UnclaimedLessonSummary::toMap).toList()));
    }

    private void handlePremiseCheck(MockHttpResponse response, String projectId, String lessonId)
            throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var seed = findLesson(projectId, lessonId);
        if (seed == null) {
            response.json(404, Map.of(KEY_ERROR, ERR_LESSON_NOT_FOUND, KEY_LESSON_ID, lessonId,
                    KEY_MESSAGE, "No lesson carries that id in " + projectId));
            return;
        }
        var result = new LessonPremiseCheckResult(lessonId, premiseStatus(seed), seed.evidence(),
                seed.premiseCause());
        response.json(200, result.toMap());
    }

    private void handleClaimLesson(MockHttpRequest request, MockHttpResponse response, String projectId,
            String lessonId) throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        LessonClaimRequest claim;
        try {
            claim = LessonClaimRequest.fromMap(bodyMap.isEmpty()
                    ? Map.of(KEY_PROJECT_ID, projectId, KEY_LESSON_ID, lessonId)
                    : withDefaults(bodyMap, projectId, lessonId));
        } catch (IOException | IllegalArgumentException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST,
                    KEY_MESSAGE, "The claim request could not be read"));
            return;
        }
        synchronized (lessonSourceLock) {
            var seeds = lessonSourceSeeds.get(projectId);
            var index = indexOfLesson(seeds, lessonId);
            if (index < 0) {
                response.json(404, Map.of(KEY_ERROR, ERR_LESSON_NOT_FOUND, KEY_LESSON_ID, lessonId,
                        KEY_MESSAGE, "No lesson carries that id in " + projectId));
                return;
            }
            var seed = seeds.get(index);
            if (seed.retired()) {
                refuseClaim(response, lessonId, REASON_LESSON_RETIRED);
                return;
            }
            if (seed.claimedBy() != null) {
                refuseClaim(response, lessonId, REASON_LESSON_CLAIMED_ELSEWHERE);
                return;
            }
            if (!LessonPremiseCheckResult.STATUS_HOLDS.equals(premiseStatus(seed)) && !claim.isUseAnyway()) {
                refuseClaim(response, lessonId, REASON_LESSON_PREMISE_BROKEN);
                return;
            }
            var planId = JsonTree.string(bodyMap, "plan_id");
            if (planId == null || planId.isBlank()) {
                planId = "lesson-" + lessonId;
            }
            seeds.set(index, seed.claimedBy(planId));
            broadcastEvent(EVENT_LESSON_CLAIMED, Map.of(KEY_LESSON_ID, lessonId, "plan_id", planId,
                    KEY_PROJECT_ID, projectId));
            response.json(200, new LessonClaimResult(lessonId, LessonClaimResult.STATUS_CLAIMED, planId).toMap());
        }
    }

    private void handleRetireLesson(MockHttpRequest request, MockHttpResponse response, String projectId,
            String lessonId) throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        synchronized (lessonSourceLock) {
            var seeds = lessonSourceSeeds.get(projectId);
            var index = indexOfLesson(seeds, lessonId);
            if (index < 0) {
                response.json(404, Map.of(KEY_ERROR, ERR_LESSON_NOT_FOUND, KEY_LESSON_ID, lessonId,
                        KEY_MESSAGE, "No lesson carries that id in " + projectId));
                return;
            }
            var seed = seeds.get(index);
            if (!seed.retired()) {
                seeds.set(index, seed.retiredNow());
                broadcastEvent(EVENT_LESSON_RETIRED, Map.of(KEY_LESSON_ID, lessonId, "retired", true,
                        KEY_PROJECT_ID, projectId));
            }
        }
        var reason = JsonTree.string(bodyMap, KEY_REASON);
        response.json(200, new LessonRetirementNotice(lessonId, true, reason).toMap());
    }

    private void refuseClaim(MockHttpResponse response, String lessonId, String reason) throws IOException {
        response.json(409, Map.of(KEY_ERROR, ERR_LESSON_CLAIM_REFUSED, KEY_REASON, reason,
                KEY_LESSON_ID, lessonId));
    }

    private static Map<String, Object> withDefaults(Map<String, Object> bodyMap, String projectId,
            String lessonId) {
        bodyMap.putIfAbsent(KEY_PROJECT_ID, projectId);
        bodyMap.putIfAbsent(KEY_LESSON_ID, lessonId);
        return bodyMap;
    }

    private LessonSeed findLesson(String projectId, String lessonId) {
        synchronized (lessonSourceLock) {
            var seeds = lessonSourceSeeds.get(projectId);
            var index = indexOfLesson(seeds, lessonId);
            return index < 0 ? null : seeds.get(index);
        }
    }

    private static int indexOfLesson(List<LessonSeed> seeds, String lessonId) {
        if (seeds == null) {
            return -1;
        }
        for (var i = 0; i < seeds.size(); i++) {
            if (seeds.get(i).lessonId().equals(lessonId)) {
                return i;
            }
        }
        return -1;
    }

    private static String premiseStatus(LessonSeed seed) {
        if (seed.premiseUnavailable()) {
            return LessonPremiseCheckResult.STATUS_UNAVAILABLE;
        }
        return seed.evidence().isEmpty() ? LessonPremiseCheckResult.STATUS_HOLDS
                : LessonPremiseCheckResult.STATUS_BROKEN;
    }

    private record LessonSeed(
    String lessonId,
    String title,
    String summary,
    Instant recordedAt,
    String citingPlanId,
    List<String> symbolsCited,
    List<LessonPremiseEvidence> evidence,
    boolean premiseUnavailable,
    String premiseCause,
    String claimedBy,
    boolean retired
    ) {

        boolean unclaimed() {
            return claimedBy == null && !retired;
        }

        UnclaimedLessonSummary toSummary() {
            return new UnclaimedLessonSummary(lessonId, title, summary, recordedAt, citingPlanId, symbolsCited);
        }

        LessonSeed claimedBy(String planId) {
            return new LessonSeed(lessonId, title, summary, recordedAt, citingPlanId, symbolsCited, evidence,
                    premiseUnavailable, premiseCause, planId, retired);
        }

        LessonSeed retiredNow() {
            return new LessonSeed(lessonId, title, summary, recordedAt, citingPlanId, symbolsCited, evidence,
                    premiseUnavailable, premiseCause, claimedBy, true);
        }
    }

    /**
     * Seeds the epic decomposition fixtures from the active scenario's runtime fixture
     * ({@code facts.runtime.decompositions}), so the draft, its checks, and its later changes are
     * declarative.
     */
    private void initDefaultDecomposition() {
        synchronized (decompositionLock) {
            decompositions.clear();
            seedDecompositions(startRuntimeFixture());
            if (decompositions.isEmpty()) {
                // The start scenarios have no decomposition of their own: a started epic shows the
                // canonical draft the running-work scenario declares, the fixture every scenario
                // carried before the consolidation.
                seedDecompositions(defaultRuntimeFixture());
            }
        }
    }

    private void seedDecompositions(@Nullable Map<String, Object> fixture) {
        for (var cut : fixtureObjects(fixture, FIXTURE_DECOMPOSITIONS)) {
            var decomposition = readFixture(() -> EpicDecomposition.fromMap(cut),
                    "facts.runtime.decompositions");
            decompositions.put(decomposition.epicId(), decomposition);
        }
    }

    /**
     * @return the runtime fixture of the scenario the default fixtures live in, or {@code null}
     */
    private @Nullable Map<String, Object> defaultRuntimeFixture() {
        if (StandardScenarios.RUNNING_WORK.equals(currentScenario.get().name())) {
            return null;
        }
        var scenario = registry.resolve(StandardScenarios.RUNNING_WORK + ScenarioRegistry.STATE_SEPARATOR
                + BASE_FIXTURE_STATE);
        return runtimeFixtureOf(scenario, scenario.start());
    }

    private boolean handleDecompositionRoutes(MockHttpRequest request, MockHttpResponse response)
            throws IOException {
        var path = request.path();
        if (!path.startsWith(PREFIX_PROJECTS)) {
            return false;
        }
        var action = "";
        var base = path;
        for (var candidate : List.of(SUFFIX_DECOMPOSITION_CHECK, SUFFIX_DECOMPOSITION_INSTRUCT,
                SUFFIX_DECOMPOSITION_COMMIT)) {
            if (path.endsWith(candidate)) {
                action = candidate.substring(PATH_DECOMPOSITION.length() + 1);
                base = path.substring(0, path.length() - candidate.length());
                break;
            }
        }
        if (action.isEmpty()) {
            if (!path.endsWith(PATH_DECOMPOSITION)) {
                return false;
            }
            base = path.substring(0, path.length() - PATH_DECOMPOSITION.length());
        }
        var rest = base.substring(PREFIX_PROJECTS.length());
        var marker = rest.indexOf(SEGMENT_EPICS);
        if (marker < 0) {
            return false;
        }
        var projectId = rest.substring(0, marker);
        var epicId = rest.substring(marker + SEGMENT_EPICS.length());
        if (projectId.isBlank() || epicId.isBlank()) {
            response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND, KEY_MESSAGE, "No epic in the request"));
            return true;
        }
        if (!Set.of(CONST_PM_MCP_CLIENTS, CONST_PM_MCP_CORE).contains(projectId)) {
            response.json(404, Map.of(KEY_ERROR, ERR_PROJECT_NOT_FOUND, KEY_MESSAGE, "Unknown project: " + projectId));
            return true;
        }
        switch (action) {
            case "" -> {
                if (!"GET".equals(request.method())) {
                    return false;
                }
                handleGetDecomposition(response, projectId, epicId);
            }
            case "check" -> {
                if (!METHOD_POST.equals(request.method())) {
                    return false;
                }
                handleCheckDecomposition(response, projectId, epicId);
            }
            case "instruct" -> {
                if (!METHOD_POST.equals(request.method())) {
                    return false;
                }
                handleInstructDecomposition(request, response, projectId, epicId);
            }
            case "commit" -> {
                if (!METHOD_POST.equals(request.method())) {
                    return false;
                }
                handleCommitDecomposition(response, projectId, epicId);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleGetDecomposition(MockHttpResponse response, String projectId, String epicId)
            throws IOException {
        synchronized (decompositionLock) {
            var decomposition = decompositions.get(epicId);
            if (decomposition == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND,
                        KEY_MESSAGE, "No decomposition for epic: " + epicId, KEY_PROJECT_ID, projectId));
                return;
            }
            // The cut is served with the findings of its current shape, so both clients show the
            // conflicts before the operator agrees; the check endpoint persists them.
            var withFindings = new EpicDecomposition(epicId, decomposition.status(), decomposition.revision(),
                    decomposition.workstreams(), DecompositionValidator.validate(decomposition.workstreams()));
            response.json(200, withFindings.toMap());
        }
    }

    private void handleCheckDecomposition(MockHttpResponse response, String projectId, String epicId)
            throws IOException {
        synchronized (decompositionLock) {
            var current = decompositions.get(epicId);
            if (current == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND,
                        KEY_MESSAGE, "No decomposition for epic: " + epicId, KEY_PROJECT_ID, projectId));
                return;
            }
            var findings = DecompositionValidator.validate(current.workstreams());
            var status = findings.isEmpty() ? DecompositionState.REVIEW : DecompositionState.CHECK;
            var updated = new EpicDecomposition(epicId, status, current.revision(), current.workstreams(),
                    findings);
            decompositions.put(epicId, updated);
            broadcastEvent(EVENT_DECOMPOSITION_UPDATED, Map.of(KEY_EVENT, EVENT_DECOMPOSITION_UPDATED,
                    "epic_id", epicId, KEY_PROJECT_ID, projectId, "status", status.wireValue()));
            response.json(200, updated.toMap());
        }
    }

    private void handleInstructDecomposition(MockHttpRequest request, MockHttpResponse response, String projectId,
            String epicId) throws IOException {
        var bodyMap = readBodyObject(request);
        DecompositionInstructRequest instruct;
        try {
            instruct = DecompositionInstructRequest.fromMap(bodyMap);
        }
        catch (IOException | IllegalArgumentException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE,
                    "The instruction could not be read"));
            return;
        }
        synchronized (decompositionLock) {
            var current = decompositions.get(epicId);
            if (current == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND,
                        KEY_MESSAGE, "No decomposition for epic: " + epicId, KEY_PROJECT_ID, projectId));
                return;
            }
            EpicDecomposition updated;
            if (instruct.action() == InstructAction.ADD) {
                updated = applyAdd(current, instruct);
            } else {
                var targetId = instruct.planId() != null ? instruct.planId() : firstOpenPlanId(current);
                if (targetId == null || current.plan(targetId) == null) {
                    response.json(404, Map.of(KEY_ERROR, ERR_PLAN_NOT_FOUND,
                            KEY_MESSAGE, "No plan to change: " + instruct.planId()));
                    return;
                }
                updated = applyRewrite(current, targetId, instruct.instruction());
            }
            decompositions.put(epicId, updated);
            broadcastEvent(EVENT_DECOMPOSITION_UPDATED, Map.of(KEY_EVENT, EVENT_DECOMPOSITION_UPDATED,
                    "epic_id", epicId, KEY_PROJECT_ID, projectId, "status", updated.status().wireValue()));
            response.json(200, updated.toMap());
        }
    }

    private static EpicDecomposition applyRewrite(EpicDecomposition current, String planId, String instruction) {
        var workstreams = new ArrayList<WorkstreamRecord>();
        for (var workstream : current.workstreams()) {
            var plans = new ArrayList<StagedPlanRecord>();
            for (var plan : workstream.stagedPlans()) {
                plans.add(plan.planId().equals(planId) ? rewritePlan(plan, instruction) : plan);
            }
            workstreams.add(new WorkstreamRecord(workstream.id(), workstream.name(), workstream.description(),
                    workstream.order(), plans));
        }
        return new EpicDecomposition(current.epicId(), DecompositionState.READ_BACK, current.revision(),
                workstreams, current.findings());
    }

    private static EpicDecomposition applyAdd(EpicDecomposition current, DecompositionInstructRequest instruct) {
        var workstreams = new ArrayList<WorkstreamRecord>(current.workstreams());
        if (workstreams.isEmpty()) {
            workstreams.add(new WorkstreamRecord("WS-1", "New work", null, 1, List.of()));
        }
        var lastIndex = workstreams.size() - 1;
        var last = workstreams.get(lastIndex);
        var planId = uniqueAddedPlanId(current, instruct.instruction());
        var newPlan = new StagedPlanRecord(planId, planId, instruct.instruction(),
                surfaceHint(instruct.instruction(), planId), List.of(), StagedPlanRecord.STATUS_NEW);
        var plans = new ArrayList<>(last.stagedPlans());
        plans.add(newPlan);
        workstreams.set(lastIndex, new WorkstreamRecord(last.id(), last.name(), last.description(),
                last.order(), plans));
        return new EpicDecomposition(current.epicId(), DecompositionState.READ_BACK, current.revision(),
                workstreams, current.findings());
    }

    private static StagedPlanRecord rewritePlan(StagedPlanRecord plan, String instruction) {
        var surface = surfaceHint(instruction, plan.planId());
        var resulting = surface.isEmpty() ? plan.expectedSurface() : surface;
        return new StagedPlanRecord(plan.planId(), plan.title(), instruction, resulting, plan.dependencies(),
                StagedPlanRecord.STATUS_CHANGED);
    }

    private static List<String> surfaceHint(String instruction, String planId) {
        if (instruction == null) {
            return List.of();
        }
        String chosen = null;
        var lastIndex = -1;
        for (var module : List.of("pm-api", "pm-relay", "pm-mcp-clients", "pm-mcp-core")) {
            var index = instruction.lastIndexOf(module);
            if (index > lastIndex) {
                lastIndex = index;
                chosen = module;
            }
        }
        return chosen == null ? List.of() : List.of(chosen + "/src/" + planId);
    }

    private static String uniqueAddedPlanId(EpicDecomposition current, String instruction) {
        var base = instruction != null && instruction.contains("docs") ? "relay-docs" : "relay-plan";
        var candidate = base;
        var suffix = 1;
        while (current.plan(candidate) != null) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private static String firstOpenPlanId(EpicDecomposition current) {
        for (var workstream : current.workstreams()) {
            for (var plan : workstream.stagedPlans()) {
                if (!plan.isDropped() && !plan.isLocked()) {
                    return plan.planId();
                }
            }
        }
        return null;
    }

    private void handleCommitDecomposition(MockHttpResponse response, String projectId, String epicId)
            throws IOException {
        synchronized (decompositionLock) {
            var current = decompositions.get(epicId);
            if (current == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND,
                        KEY_MESSAGE, "No decomposition for epic: " + epicId, KEY_PROJECT_ID, projectId));
                return;
            }
            var findings = DecompositionValidator.validate(current.workstreams());
            if (findings.stream().anyMatch(finding -> finding.severity() == CheckSeverity.ERROR)) {
                response.json(409, Map.of(KEY_ERROR, ERR_CHECKS_FAIL, KEY_PROJECT_ID, projectId,
                        "epic_id", epicId, FIELD_FINDINGS,
                        findings.stream().map(DecompositionCheckFinding::toMap).toList()));
                return;
            }
            var revision = current.revision() + 1;
            var committed = new EpicDecomposition(epicId, DecompositionState.COMMIT, revision,
                    current.workstreams(), findings);
            decompositions.put(epicId, committed);
            var launchable = committed.stagedPlanCount();
            broadcastEvent(EVENT_DECOMPOSITION_UPDATED, Map.of(KEY_EVENT, EVENT_DECOMPOSITION_UPDATED,
                    "epic_id", epicId, KEY_PROJECT_ID, projectId, "status",
                    DecompositionState.COMMIT.wireValue()));
            broadcastEvent(EVENT_EPIC_LAUNCHABLE, Map.of(KEY_EVENT, EVENT_EPIC_LAUNCHABLE,
                    "epic_id", epicId, KEY_PROJECT_ID, projectId, "plans", launchablePlanIds(committed),
                    "count", launchable));
            response.json(200, new DecompositionCommitResult(DecompositionCommitResult.STATUS_COMMITTED,
                    revision, launchable).toMap());
        }
    }

    private static List<String> launchablePlanIds(EpicDecomposition decomposition) {
        var ids = new ArrayList<String>();
        for (var workstream : decomposition.workstreams()) {
            for (var plan : workstream.stagedPlans()) {
                if (plan.dependencies().isEmpty()) {
                    ids.add(plan.planId());
                }
            }
        }
        return ids;
    }

    private boolean handleStartRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb)
            throws IOException {
        var path = request.path();
        if (handleStartEntryRoutes(request, response)
                || handleStartProjectRoutes(request, response, isWeb)
                || handleStartEndedRoutes(request, response)) {
            return true;
        }
        if ("GET".equals(request.method()) && PATH_RECIPES.equals(path)) {
            handleListRecipes(response);
            return true;
        }
        if (METHOD_POST.equals(request.method()) && PATH_INTAKE_DRAFT.equals(path)) {
            handleIntakeDraft(request, response);
            return true;
        }
        if (METHOD_POST.equals(request.method()) && path.startsWith(PREFIX_PROJECTS)) {
            if (path.endsWith(SUFFIX_PLANS_START)) {
                handleStartPlan(request, response, projectIdOf(path, SUFFIX_PLANS_START));
                return true;
            }
            if (path.endsWith(SUFFIX_EPICS_START)) {
                handleStartEpic(request, response, projectIdOf(path, SUFFIX_EPICS_START));
                return true;
            }
        }
        return false;
    }

    private boolean handleStartProjectRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb)
            throws IOException {
        var path = request.path();
        if (!path.startsWith(PATH_PROJECTS_PREFIX)) {
            return false;
        }
        if ("GET".equals(request.method()) && path.endsWith(SEGMENT_PROJECT_FILES)) {
            var projectId = path.substring(PATH_PROJECTS_PREFIX.length(),
                    path.length() - SEGMENT_PROJECT_FILES.length());
            handleProjectFiles(request, response, projectId, isWeb);
            return true;
        }
        if (METHOD_POST.equals(request.method()) && path.endsWith(SEGMENT_PROJECT_ENTRIES)) {
            var projectId = path.substring(PATH_PROJECTS_PREFIX.length(),
                    path.length() - SEGMENT_PROJECT_ENTRIES.length());
            handleOpenEntry(request, response, projectId);
            return true;
        }
        return false;
    }

    private boolean handleStartEndedRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        var method = request.method();
        if (!path.startsWith(PATH_ENDED) || !path.contains(SEGMENT_ENDED_CHAT)) {
            return false;
        }
        if ("GET".equals(method) && path.endsWith(SEGMENT_ENTRY_CHAT)) {
            var subjectId = endedSubjectId(path.substring(PATH_ENDED.length()),
                    SEGMENT_ENTRY_CHAT.length());
            if (subjectId == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_SCOPE_NOT_FOUND, KEY_MESSAGE,
                        "No ended work is named"));
                return true;
            }
            handleEndedChat(response, subjectId);
            return true;
        }
        if (METHOD_POST.equals(method) && path.endsWith(SEGMENT_ENDED_CHAT_ACCEPT)) {
            var rest = path.substring(PATH_ENDED.length());
            var marker = rest.indexOf(SEGMENT_ENDED_CHAT);
            var projectId = marker > 0 ? rest.substring(0, marker) : null;
            var subjectId = endedSubjectId(rest, SEGMENT_ENDED_CHAT_ACCEPT.length());
            if (projectId == null || subjectId == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_SCOPE_NOT_FOUND, KEY_MESSAGE,
                        "No ended work is named"));
                return true;
            }
            handleAcceptFollowUp(response, projectId, subjectId);
            return true;
        }
        return false;
    }

    private static @Nullable String endedSubjectId(String rest, int suffixLength) {
        var marker = rest.indexOf(SEGMENT_ENDED_CHAT);
        var start = marker + SEGMENT_ENDED_CHAT.length();
        var end = rest.length() - suffixLength;
        if (marker <= 0 || end <= start) {
            return null;
        }
        return rest.substring(start, end);
    }

    /**
     * Serves the top-level entry routes (UC-STA-1..5): the entry itself, its events, the operator
     * acts of its states, its chat, and the confined file listing. An entry is addressed by its id
     * once opened; only the opening request names the project.
     *
     * @param request  the request
     * @param response the response
     * @return {@code true} if the request was answered here
     * @throws IOException on write failure
     */
    private boolean handleStartEntryRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if (!path.startsWith(PATH_ENTRIES_PREFIX)) {
            return false;
        }
        var rest = path.substring(PATH_ENTRIES_PREFIX.length());
        if (rest.isEmpty() || rest.endsWith(SEGMENT_EVENTS)) {
            return false;
        }
        if (handleEntryChatRoutes(request, response, rest)) {
            return true;
        }
        var entryId = rest;
        String act = null;
        var marker = rest.indexOf(SEGMENT_LINKS);
        if (marker > 0) {
            entryId = rest.substring(0, marker);
            act = rest.substring(marker + SEGMENT_LINKS.length());
        }
        if (entryId.isEmpty() || entryId.contains("/")) {
            return false;
        }
        if ("GET".equals(request.method()) && act == null) {
            var state = startRegistry.find(entryId);
            if (state == null) {
                sendEntryNotFound(response, entryId);
                return true;
            }
            response.json(200, state.toMap());
            return true;
        }
        if (METHOD_POST.equals(request.method()) && act != null) {
            handleEntryAct(request, response, entryId, act);
            return true;
        }
        return false;
    }

    private boolean handleEntryChatRoutes(MockHttpRequest request, MockHttpResponse response, String rest)
            throws IOException {
        if (!METHOD_POST.equals(request.method())) {
            return false;
        }
        if (rest.endsWith(SEGMENT_ENTRY_CHAT_TURNS)) {
            handleEntryChatTurn(request, response, rest, SEGMENT_ENTRY_CHAT_TURNS);
            return true;
        }
        if (rest.endsWith(SEGMENT_ENTRY_CHAT_END)) {
            handleEntryChatEnd(response, rest, SEGMENT_ENTRY_CHAT_END);
            return true;
        }
        if (rest.endsWith(SEGMENT_ENTRY_CHAT_PROPOSAL_ACCEPT)) {
            handleEntryChatProposal(response, rest, SEGMENT_ENTRY_CHAT_PROPOSAL_ACCEPT, true);
            return true;
        }
        if (rest.endsWith(SEGMENT_ENTRY_CHAT_PROPOSAL_REJECT)) {
            handleEntryChatProposal(response, rest, SEGMENT_ENTRY_CHAT_PROPOSAL_REJECT, false);
            return true;
        }
        return false;
    }

    private void handleEntryChatEnd(MockHttpResponse response, String rest, String suffix) throws IOException {
        var state = startRegistry.endChat(rest.substring(0, rest.length() - suffix.length()));
        if (state == null) {
            sendEntryNotFound(response, rest.substring(0, rest.length() - suffix.length()));
            return;
        }
        response.json(200, state.toMap());
    }

    private void handleOpenEntry(MockHttpRequest request, MockHttpResponse response, String projectId)
            throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        EntryOpenRequest parsed;
        try {
            var bodyMap = readBodyObject(request);
            bodyMap.putIfAbsent(KEY_PROJECT_ID, projectId);
            bodyMap.remove(KEY_PROJECT_ID);
            parsed = EntryOpenRequest.fromMap(bodyMap);
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IOException | RuntimeException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST,
                    KEY_MESSAGE, "The opening request could not be read"));
            return;
        }
        var state = startRegistry.open(projectId, parsed);
        if (state == null) {
            sendProjectNotFound(response, projectId);
            return;
        }
        broadcastEvent("entry.opened", Map.of("entry_id", state.entryId(), KEY_PROJECT_ID, projectId,
                "kind", parsed.kind().wireValue()));
        response.json(201, state.toMap());
    }

    private void handleEntryAct(MockHttpRequest request, MockHttpResponse response, String entryId, String act)
            throws IOException {
        try {
            var state = startRegistry.act(entryId, act, readBodyObject(request));
            if (state == null) {
                sendEntryNotFound(response, entryId);
                return;
            }
            response.json(200, state.toMap());
        } catch (StartRegistry.ActRefused refused) {
            var status = switch (refused.error()) {
                case ERR_INVALID_REQUEST, "invalid_arguments" -> 400;
                case ERR_LOOPBACK_ONLY, ERR_CLI_ONLY -> 403;
                case ERR_SCOPE_NOT_FOUND -> 404;
                default -> 409;
            };
            response.json(status, Map.of(KEY_ERROR, refused.error(), KEY_MESSAGE, refused.detail(),
                    KEY_STATE, entryId));
        }
    }

    private void handleEntryChatTurn(MockHttpRequest request, MockHttpResponse response, String rest,
            String suffix) throws IOException {
        var entryId = rest.substring(0, rest.length() - suffix.length());
        var text = JsonTree.string(readBodyObject(request), KEY_TEXT);
        if (text == null || text.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE, "A chat turn carries its text"));
            return;
        }
        try {
            var state = startRegistry.chatTurn(entryId, text);
            if (state == null) {
                sendEntryNotFound(response, entryId);
                return;
            }
            response.json(200, state.toMap());
        } catch (StartRegistry.ActRefused refused) {
            response.json(409, Map.of(KEY_ERROR, refused.error(), KEY_MESSAGE, refused.detail()));
        }
    }

    private void handleEntryChatProposal(MockHttpResponse response, String rest, String suffix, boolean accept)
            throws IOException {
        var entryId = rest.substring(0, rest.length() - suffix.length());
        try {
            var state = startRegistry.chatProposal(entryId, accept);
            if (state == null) {
                sendEntryNotFound(response, entryId);
                return;
            }
            response.json(200, state.toMap());
        } catch (StartRegistry.ActRefused refused) {
            response.json(409, Map.of(KEY_ERROR, refused.error(), KEY_MESSAGE, refused.detail()));
        }
    }

    private void handleProjectFiles(MockHttpRequest request, MockHttpResponse response, String projectId,
            boolean isWeb) throws IOException {
        var rootWire = queryParameter(request, "root");
        var root = ConfinedRoot.fromWireOrNull(rootWire != null ? rootWire : ConfinedRoot.CHECKOUT.wireValue());
        if (root == null) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE,
                    "Unknown root: " + rootWire));
            return;
        }
        try {
            var listing = startRegistry.listFiles(projectId, root, queryParameter(request, "path"), isWeb);
            if (listing == null) {
                sendProjectNotFound(response, projectId);
                return;
            }
            response.json(200, listing.toMap());
        } catch (StartRegistry.ActRefused refused) {
            var status = ERR_LOOPBACK_ONLY.equals(refused.error()) ? 403 : 400;
            response.json(status, Map.of(KEY_ERROR, refused.error(), KEY_MESSAGE, refused.detail()));
        }
    }

    private void handleEndedChat(MockHttpResponse response, String subjectId) throws IOException {
        var chat = startRegistry.endedChat(subjectId);
        if (chat == null) {
            response.json(404, Map.of(KEY_ERROR, ERR_SCOPE_NOT_FOUND, KEY_MESSAGE,
                    "No ended work with the id " + subjectId));
            return;
        }
        response.json(200, chat.toMap());
    }

    private void handleAcceptFollowUp(MockHttpResponse response, String projectId, String subjectId)
            throws IOException {
        var state = startRegistry.acceptFollowUp(projectId, subjectId);
        if (state == null) {
            response.json(404, Map.of(KEY_ERROR, ERR_SCOPE_NOT_FOUND, KEY_MESSAGE,
                    "No ended work with the id " + subjectId));
            return;
        }
        response.json(201, state.toMap());
    }

    private static @Nullable String queryParameter(MockHttpRequest request, String name) {
        var query = request.query();
        if (query == null) {
            return null;
        }
        for (var part : query.split("&")) {
            var eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            if (name.equals(part.substring(0, eq))) {
                return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void sendEntryNotFound(MockHttpResponse response, String entryId) throws IOException {
        response.json(404, Map.of(KEY_ERROR, ERR_SCOPE_NOT_FOUND, KEY_MESSAGE, "No entry with the id " + entryId));
    }

    private static String projectIdOf(String path, String suffix) {
        return path.substring(PREFIX_PROJECTS.length(), path.length() - suffix.length());
    }

    private boolean gateAllows(String projectId, MockHttpResponse response) throws IOException {
        if (projectId == null || projectId.isBlank()) {
            response.json(404, Map.of(KEY_ERROR, ERR_PROJECT_NOT_FOUND, KEY_MESSAGE, "No project id in the request"));
            return false;
        }
        if (CONST_PLAN_MARSHALL.equals(projectId)) {
            response.json(409, Map.of(KEY_ERROR, ERR_START_REFUSED, KEY_REASON, ERR_PROJECT_NOT_READY,
                    KEY_PROJECT_ID, projectId, "need", "credential sonar"));
            return false;
        }
        if (!Set.of(CONST_PM_MCP_CLIENTS, CONST_PM_MCP_CORE).contains(projectId)) {
            response.json(404, Map.of(KEY_ERROR, ERR_PROJECT_NOT_FOUND, KEY_MESSAGE, "Unknown project: " + projectId));
            return false;
        }
        return true;
    }

    private void handleListRecipes(MockHttpResponse response) throws IOException {
        var recipes = startRegistry.recipes(CONST_PM_MCP_CLIENTS);
        if (recipes.isEmpty()) {
            recipes = defaultRecipes();
        }
        response.json(200, Map.of(KEY_RECIPES, recipes.stream().map(RecipeRef::toMap).toList()));
    }

    private static List<RecipeRef> defaultRecipes() {
        return List.of(
                new RecipeRef("agentfile-hygiene", "agentfile-hygiene",
                        "classify and clean the agent files against the rubric", CONST_DOMAIN, false, List.of()),
                new RecipeRef("code-review", "code-review",
                        "a structural and quality review of the current footprint", CONST_DOMAIN, false,
                        List.of(new RecipeParameter("depth", CONST_STANDARD,
                                List.of("quick", CONST_STANDARD, "deep"), true, true, null))),
                new RecipeRef("lesson-cleanup", "lesson-cleanup",
                        "turn one lesson into a slim, deterministic plan", CONST_DOMAIN, false, List.of()),
                new RecipeRef(CONST_COVERAGE, CONST_COVERAGE, "raise the test coverage of modules", CONST_DOMAIN, false,
                        List.of(
                                new RecipeParameter(CONST_MODULES, CONST_ALL_MODULES,
                                        List.of(CONST_ALL_MODULES, "pm-api", "pm-operator"), true, true, null),
                                new RecipeParameter("thoroughness", CONST_STANDARD,
                                        List.of("quick", CONST_STANDARD, "thorough"), true, true, null))),
                new RecipeRef("audit", "audit", "audit the code against a request", "project", true,
                        List.of(new RecipeParameter(CONST_MODULES, CONST_ALL_MODULES, List.of(CONST_ALL_MODULES), true, true, null))),
                new RecipeRef("no-modules", "no-modules", "a recipe whose parameter has no option", "project",
                        false, List.of(new RecipeParameter(CONST_MODULES, null, List.of(), false, false,
                                "none the recipe can work on"))));
    }

    private void handleStartPlan(MockHttpRequest request, MockHttpResponse response, String projectId) throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        bodyMap.putIfAbsent(KEY_PROJECT_ID, projectId);
        StartPlanRequest parsed;
        try {
            parsed = StartPlanRequest.fromMap(bodyMap);
        }
        catch (IOException | IllegalArgumentException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE, "The start request could not be read"));
            return;
        }
        var title = deriveTitle(parsed.title(), parsed.objective(), parsed.issue(), parsed.documents());
        var idSource = parsed.recipeId() != null
                ? "recipe-" + parsed.recipeId() + "-" + LocalDateTime.now(ZoneId.systemDefault()).format(RECIPE_ID_TIMESTAMP)
                : title;
        var id = uniquePlanId(slug(idSource));
        startedPlans.put(id, new StartedPlan(title, projectId));
        var body = new LinkedHashMap<String, Object>(new PlanStartResult(id, PlanStartResult.KIND_PLAN,
                title, PlanStartResult.STATE_STARTED, PATH_PLANS_PREFIX + id).toMap());
        body.put(KEY_PROJECT_ID, projectId);
        if (parsed.recipeId() != null) {
            body.put("recipe_id", parsed.recipeId());
        }
        broadcastEvent(EVENT_PLAN_STARTED, Map.of(KEY_PLAN_ID, id, KEY_LABEL, title, KEY_PROJECT_ID, projectId));
        response.json(201, body);
    }

    private void handleStartEpic(MockHttpRequest request, MockHttpResponse response, String projectId) throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        bodyMap.putIfAbsent(KEY_PROJECT_ID, projectId);
        StartEpicRequest parsed;
        try {
            parsed = StartEpicRequest.fromMap(bodyMap);
        }
        catch (IOException | IllegalArgumentException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE, "The epic request could not be read"));
            return;
        }
        var title = deriveTitle(parsed.title(), parsed.objective(), null, List.of());
        var id = uniqueEpicId(slug(title));
        var body = new LinkedHashMap<String, Object>(new PlanStartResult(id, PlanStartResult.KIND_EPIC,
                title, PlanStartResult.STATE_STARTED, PATH_EPICS_PREFIX + id).toMap());
        body.put(KEY_PROJECT_ID, projectId);
        body.put("auto_start", parsed.autoStart() != null ? parsed.autoStart() : Boolean.TRUE);
        broadcastEvent(EVENT_EPIC_STARTED, Map.of("epic_id", id, KEY_LABEL, title, KEY_PROJECT_ID, projectId));
        response.json(201, body);
    }

    private void handleIntakeDraft(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var bodyMap = readBodyObject(request);
        var projectId = JsonTree.string(bodyMap, KEY_PROJECT_ID);
        if (projectId == null || projectId.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE, "project_id is required"));
            return;
        }
        String id;
        synchronized (startLock) {
            intakeDraftCounter++;
            id = "draft-" + intakeDraftCounter;
        }
        var draft = new IntakeDraft(id, projectId, JsonTree.string(bodyMap, "prompt"),
                readSources(bodyMap), Instant.now());
        broadcastEvent(EVENT_INTAKE_CREATED, Map.of("draft_id", id, KEY_PROJECT_ID, projectId));
        response.json(201, draft.toMap());
    }

    private boolean handleGitSourceRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        var method = request.method();
        if (!path.startsWith(PREFIX_PROJECTS)) {
            return false;
        }
        if (METHOD_POST.equals(method) && path.endsWith(SUFFIX_PR_OBSERVE)) {
            handlePrObserve(request, response, projectIdOf(path, SUFFIX_PR_OBSERVE));
            return true;
        }
        if (METHOD_POST.equals(method) && path.endsWith(SUFFIX_PR_ADOPT)) {
            handlePrAdopt(request, response, projectIdOf(path, SUFFIX_PR_ADOPT));
            return true;
        }
        if ("GET".equals(method) && path.endsWith(SUFFIX_LOCAL_PREVIEW)) {
            handleLocalChangesPreview(response, projectIdOf(path, SUFFIX_LOCAL_PREVIEW));
            return true;
        }
        if (METHOD_POST.equals(method) && path.endsWith(SUFFIX_LOCAL_CAPTURE)) {
            handleLocalChangesCapture(request, response, projectIdOf(path, SUFFIX_LOCAL_CAPTURE));
            return true;
        }
        return false;
    }

    private void handlePrObserve(MockHttpRequest request, MockHttpResponse response, String projectId)
            throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        var number = parsePrNumber(JsonTree.string(bodyMap, KEY_PR_REFERENCE));
        if (number <= 0) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE,
                    "The pull request reference names no number"));
            return;
        }
        var observation = observationFor(number);
        broadcastEvent(EVENT_PR_OBSERVED, observation.toMap());
        response.json(200, observation.toMap());
    }

    private void handlePrAdopt(MockHttpRequest request, MockHttpResponse response, String projectId)
            throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        bodyMap.putIfAbsent(KEY_PROJECT_ID, projectId);
        PrAdoptRequest parsed;
        try {
            parsed = PrAdoptRequest.fromMap(bodyMap);
        }
        catch (IOException | IllegalArgumentException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE,
                    "The adopt request could not be read"));
            return;
        }
        var number = parsePrNumber(parsed.prUrl());
        if (number <= 0) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE,
                    "The pull request reference names no number"));
            return;
        }
        var observation = observationFor(number);
        var refusal = adoptRefusal(observation, parsed.screeningBypassReason());
        if (refusal != null) {
            response.json(409, Map.of(KEY_ERROR, ERR_START_REFUSED, KEY_REASON, refusal, KEY_PROJECT_ID, projectId));
            return;
        }
        var planId = uniquePlanId(slug("adopt-pr-" + observation.number()));
        startedPlans.put(planId, new StartedPlan(observation.title(), projectId));
        var body = new LinkedHashMap<String, Object>(new PrAdoptResult(planId, SourceLane.ADOPT,
                observation.branch(), null).toMap());
        body.put(KEY_PROJECT_ID, projectId);
        body.put(KEY_PR_REFERENCE, observation.url());
        body.put("screening", observation.screening().wireValue());
        broadcastEvent(EVENT_PLAN_STARTED, Map.of(KEY_PLAN_ID, planId, KEY_PROJECT_ID, projectId,
                KEY_LANE, SourceLane.ADOPT.wireValue()));
        response.json(201, body);
    }

    private void handleLocalChangesPreview(MockHttpResponse response, String projectId) throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var landing = landingFor(projectId);
        var commits = CONST_LANDING_PR.equals(landing);
        if (capturedProjects.contains(projectId)) {
            response.json(200, new LocalChangesPreview(null, null, "main", List.of(), landing, commits, false,
                    ERR_NO_LOCAL_CHANGES).toMap());
            return;
        }
        var previewId = issuePreviewId(projectId);
        var preview = commits
                ? new LocalChangesPreview(null, previewId, "main", localChangedFiles(), landing, true, true, null)
                : new LocalChangesPreview(null, previewId, "main", localChangedFiles(), landing, false, false,
                landing);
        response.json(200, preview.toMap());
    }

    private String issuePreviewId(String projectId) {
        String previewId;
        synchronized (startLock) {
            gitSourceCounter++;
            previewId = "preview-" + projectId + "-" + gitSourceCounter;
        }
        localChangePreviews.put(projectId, previewId);
        return previewId;
    }

    private void handleLocalChangesCapture(MockHttpRequest request, MockHttpResponse response, String projectId)
            throws IOException {
        if (!gateAllows(projectId, response)) {
            return;
        }
        var bodyMap = readBodyObject(request);
        bodyMap.putIfAbsent(KEY_PROJECT_ID, projectId);
        LocalChangesCaptureRequest parsed;
        try {
            parsed = LocalChangesCaptureRequest.fromMap(bodyMap);
        }
        catch (IOException | IllegalArgumentException _) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE,
                    "The capture request could not be read"));
            return;
        }
        if (!parsed.confirmed()) {
            response.json(409, Map.of(KEY_ERROR, ERR_START_REFUSED, KEY_REASON, ERR_CONFIRMATION_REQUIRED,
                    KEY_PROJECT_ID, projectId));
            return;
        }
        if (capturedProjects.contains(projectId)) {
            response.json(409, Map.of(KEY_ERROR, ERR_START_REFUSED, KEY_REASON, ERR_NO_LOCAL_CHANGES,
                    KEY_PROJECT_ID, projectId));
            return;
        }
        var refusal = captureRefusal(projectId, parsed.previewId());
        if (refusal != null) {
            response.json(409, Map.of(KEY_ERROR, ERR_START_REFUSED, KEY_REASON, refusal, KEY_PROJECT_ID, projectId));
            return;
        }
        String sha;
        synchronized (startLock) {
            gitSourceCounter++;
            sha = String.format("%040x", gitSourceCounter);
        }
        capturedProjects.add(projectId);
        localChangePreviews.remove(projectId);
        var planId = uniquePlanId(slug("capture-local-changes-" + projectId));
        startedPlans.put(planId, new StartedPlan(planId, projectId));
        var body = new LinkedHashMap<String, Object>(new LocalChangesCaptureResult(planId, SourceLane.LAND,
                sha, null).toMap());
        body.put(KEY_PROJECT_ID, projectId);
        broadcastEvent(EVENT_LOCAL_CAPTURED, Map.of(KEY_PLAN_ID, planId, KEY_SNAPSHOT_COMMIT, sha,
                KEY_PROJECT_ID, projectId, KEY_LANE, SourceLane.LAND.wireValue()));
        response.json(201, body);
    }

    private @Nullable String captureRefusal(String projectId, @Nullable String previewId) {
        if (!CONST_LANDING_PR.equals(landingFor(projectId))) {
            return ERR_LANDING_FILES;
        }
        var expected = localChangePreviews.get(projectId);
        if (expected == null || !expected.equals(previewId)) {
            return ERR_PREVIEW_STALE;
        }
        return null;
    }

    private static String landingFor(String projectId) {
        return CONST_PM_MCP_CORE.equals(projectId) ? CONST_LANDING_FILES : CONST_LANDING_PR;
    }

    private static int parsePrNumber(@Nullable String reference) {
        if (reference == null || reference.isBlank()) {
            return -1;
        }
        var clean = reference.trim();
        var query = clean.indexOf('?');
        if (query >= 0) {
            clean = clean.substring(0, query);
        }
        var fragment = clean.indexOf('#');
        if (fragment >= 0) {
            clean = clean.substring(0, fragment);
        }
        var slash = clean.lastIndexOf('/');
        var tail = slash >= 0 ? clean.substring(slash + 1) : clean;
        if (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit)) {
            return parsePositiveInt(tail);
        }
        var matcher = DIGITS.matcher(clean);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last != null ? parsePositiveInt(last) : -1;
    }

    private static int parsePositiveInt(String digits) {
        try {
            var value = Integer.parseInt(digits);
            return value > 0 ? value : -1;
        }
        catch (NumberFormatException _) {
            return -1;
        }
    }

    private static @Nullable String adoptRefusal(PullRequestObservation observation, @Nullable String bypassReason) {
        if (observation.rateLimit().limited()) {
            return ERR_RATE_LIMITED;
        }
        if (!observation.open()) {
            return ERR_NOT_OPEN;
        }
        if (!observation.writable()) {
            return ERR_HEAD_NOT_WRITABLE;
        }
        if (observation.screening().needsAcceptance() && (bypassReason == null || bypassReason.isBlank())) {
            return observation.screening() == ScreeningOutcome.FLAGGED ? ERR_SCREENING_FLAGGED : ERR_UNSCREENED;
        }
        return null;
    }

    private static PullRequestObservation observationFor(int number) {
        return switch (number) {
            case 7001 -> pullRequest(number, "Update the relay handshake", "relay-handshake",
                    PullRequestObservation.STATE_CLOSED, true, ScreeningOutcome.CLEAN, RateLimitStatus.unlimited());
            case 7002 -> pullRequest(number, "Rotate the web certificate", "cert-rotation",
                    PullRequestObservation.STATE_OPEN, true, ScreeningOutcome.CLEAN,
                    new RateLimitStatus(60, 0, Instant.parse("2026-10-17T15:00:00Z"), true));
            case 7003 -> pullRequest(number, "Tighten the unix socket", "socket-permissions",
                    PullRequestObservation.STATE_OPEN, false, ScreeningOutcome.CLEAN, RateLimitStatus.unlimited());
            case 6677 -> pullRequest(number, "Document the Forgejo setup", "forgejo-docs",
                    PullRequestObservation.STATE_OPEN, true, ScreeningOutcome.FLAGGED, RateLimitStatus.unlimited());
            default -> pullRequest(number, "Add a Codeberg provider for issues", "codeberg-provider",
                    PullRequestObservation.STATE_OPEN, true, ScreeningOutcome.CLEAN, RateLimitStatus.unlimited());
        };
    }

    private static PullRequestObservation pullRequest(int number, String title, String branch, String state,
            boolean writable, ScreeningOutcome screening, RateLimitStatus rateLimit) {
        return new PullRequestObservation(
                "https://codeberg.org/plan-marshall/pm-mcp-clients/pulls/" + number, number, title, "octocat",
                branch, state, writable, pullRequestChangedFiles(), screening, rateLimit);
    }

    private static List<ChangedFile> pullRequestChangedFiles() {
        return List.of(
                new ChangedFile("doc/concepts/forgejo.adoc", 12, 3, true, false),
                new ChangedFile("pm-api/src/main/java/de/planmarshall/api/start/StartPlanRequest.java", 4, 1,
                        false, true));
    }

    private static List<ChangedFile> localChangedFiles() {
        return List.of(
                new ChangedFile("pm-operator/src/main/java/de/planmarshall/operator/GitSourceCommand.java", 88, 0,
                        false, true),
                new ChangedFile("doc/agy/runbook.md", 5, 2, false, true));
    }

    private static List<SourceInput> readSources(Map<String, Object> bodyMap) throws IOException {
        var raw = JsonTree.asArray(bodyMap.get("sources"));
        if (raw == null) {
            return List.of();
        }
        var sources = new ArrayList<SourceInput>();
        for (var item : raw) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                sources.add(SourceInput.fromMap(obj));
            }
        }
        return sources;
    }

    private static Map<String, Object> readBodyObject(MockHttpRequest request) throws IOException {
        if (request.body().length == 0) {
            return new LinkedHashMap<>();
        }
        var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
        return parsed != null ? new LinkedHashMap<>(parsed) : new LinkedHashMap<>();
    }

    private static String deriveTitle(@Nullable String title, @Nullable String objective, @Nullable String issue,
            List<String> documents) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        if (objective != null && !objective.isBlank()) {
            return firstLine(objective);
        }
        if (issue != null && !issue.isBlank()) {
            return "issue-" + lastSegment(issue);
        }
        if (!documents.isEmpty() && !documents.getFirst().isBlank()) {
            return fileName(documents.getFirst());
        }
        return "New work";
    }

    private static String firstLine(String text) {
        var trimmed = text.strip();
        int newline = trimmed.indexOf('\n');
        var line = newline >= 0 ? trimmed.substring(0, newline).strip() : trimmed;
        return line.isBlank() ? "New work" : line;
    }

    private static String lastSegment(String reference) {
        var clean = reference.strip();
        int slash = clean.lastIndexOf('/');
        return slash >= 0 ? clean.substring(slash + 1) : clean.replace("#", "");
    }

    private static String fileName(String path) {
        var clean = path.strip();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        int slash = clean.lastIndexOf('/');
        return slash >= 0 ? clean.substring(slash + 1) : clean;
    }

    private String uniquePlanId(String base) {
        synchronized (startLock) {
            var candidate = base;
            int suffix = 1;
            while (startedPlanIds.contains(candidate) || knowsPlan(candidate)) {
                suffix++;
                candidate = base + "-" + suffix;
            }
            startedPlanIds.add(candidate);
            return candidate;
        }
    }

    private String uniqueEpicId(String base) {
        synchronized (startLock) {
            var candidate = base;
            int suffix = 1;
            while (startedEpicIds.contains(candidate)) {
                suffix++;
                candidate = base + "-" + suffix;
            }
            startedEpicIds.add(candidate);
            return candidate;
        }
    }

    private static String slug(String text) {
        var sb = new StringBuilder();
        boolean pendingDash = false;
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (pendingDash && !sb.isEmpty()) {
                    sb.append('-');
                }
                sb.append(c);
                pendingDash = false;
            } else {
                pendingDash = true;
            }
        }
        return sb.isEmpty() ? "started-work" : sb.toString();
    }

    private static String holdKey(String subjectId, String holdId) {
        return subjectId + ":" + holdId;
    }

    private static String controlSubjectId(String path, String prefix, String suffix) {
        var sub = path.substring(prefix.length());
        if (suffix != null && sub.endsWith(suffix)) {
            sub = sub.substring(0, sub.length() - suffix.length());
        }
        return sub;
    }

    private WorkActions buildPlanActions(String planId) {
        var state = controlPlanStates.getOrDefault(planId, "running");
        var epicId = controlPlanEpics.get(planId);
        var actions = new ArrayList<ActionAvailability>();
        switch (state) {
            case "pausing" -> {
                actions.add(ActionAvailability.enabled(ACTION_START));
                actions.add(ActionAvailability.enabled(ACTION_CANCEL));
                addChatAndSettings(planId, actions);
            }
            case "paused" -> {
                actions.add(ActionAvailability.enabled(ACTION_START));
                actions.add(ActionAvailability.enabled(ACTION_ABANDON));
                addChatAndSettings(planId, actions);
            }
            case "ended" -> actions.add(ActionAvailability.enabled("chat"));
            default -> {
                actions.add(ActionAvailability.enabled(ACTION_PAUSE));
                actions.add(ActionAvailability.enabled(ACTION_CANCEL));
                addChatAndSettings(planId, actions);
            }
        }
        var hold = holdFor(planId);
        return new WorkActions(planId, "plan", state, controlPlanPausePoints.get(planId),
                hold != null ? hold.kind() : null, epicId, actions);
    }

    private void addChatAndSettings(String planId, List<ActionAvailability> actions) {
        if ("relay-metrics".equals(planId)) {
            actions.add(ActionAvailability.disabled("chat", "no configuration of replan qualifies yet"));
        } else {
            actions.add(ActionAvailability.enabled("chat"));
        }
        actions.add(ActionAvailability.enabled("settings"));
    }

    private WorkActions buildEpicActions(String epicId) {
        var state = controlEpicStates.getOrDefault(epicId, "running");
        var actions = new ArrayList<ActionAvailability>();
        if ("ended".equals(state)) {
            actions.add(ActionAvailability.enabled("chat"));
        } else {
            actions.add(ActionAvailability.enabled(ACTION_NEXT));
            actions.add(ActionAvailability.enabled(ACTION_AUTO));
            actions.add(ActionAvailability.enabled(ACTION_COMMON_FILES));
            actions.add(ActionAvailability.enabled("decompose"));
            actions.add(ActionAvailability.enabled(FIELD_FINDINGS));
            actions.add(ActionAvailability.enabled("watches"));
            actions.add(ActionAvailability.enabled("chat"));
            if ("landing".equals(state)) {
                actions.add(ActionAvailability.disabled(ACTION_ARCHIVE, "the ledger is landing"));
            } else {
                actions.add(ActionAvailability.enabled(ACTION_ARCHIVE));
            }
            actions.add(ActionAvailability.enabled("cancel"));
        }
        return new WorkActions(epicId, "epic", state, null, null, null, actions);
    }

    private boolean handleControlRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        var method = request.method();
        var isPlan = path.startsWith(PATH_PLANS_PREFIX);
        var isEpic = path.startsWith(PATH_EPICS_PREFIX);
        if (!isPlan && !isEpic) {
            return false;
        }

        if ("GET".equals(method) && path.endsWith(SEGMENT_ACTIONS)) {
            if (isPlan) {
                handleGetPlanActions(response, controlSubjectId(path, PATH_PLANS_PREFIX, SEGMENT_ACTIONS));
            } else {
                handleGetEpicActions(response, controlSubjectId(path, PATH_EPICS_PREFIX, SEGMENT_ACTIONS));
            }
            return true;
        }

        if (METHOD_POST.equals(method)) {
            var prefix = isPlan ? PATH_PLANS_PREFIX : PATH_EPICS_PREFIX;
            var sub = path.substring(prefix.length());
            String subjectId = null;
            String action = null;
            var marker = sub.indexOf(SEGMENT_ACTIONS + "/");
            if (marker >= 0) {
                subjectId = sub.substring(0, marker);
                action = sub.substring(marker + SEGMENT_ACTIONS.length() + 1);
            } else {
                var slash = sub.indexOf('/');
                if (slash > 0 && sub.indexOf('/', slash + 1) < 0) {
                    var candidate = sub.substring(slash + 1);
                    var controlAction = (isPlan && PLAN_CONTROL_ACTIONS.contains(candidate))
                            || (isEpic && EPIC_FLAT_ACTIONS.contains(candidate));
                    if (controlAction) {
                        subjectId = sub.substring(0, slash);
                        action = candidate;
                    }
                }
            }
            if (subjectId != null && action != null && !action.contains("/")) {
                if (isPlan) {
                    handlePlanAction(request, response, subjectId, action);
                } else {
                    handleEpicAction(request, response, subjectId, action);
                }
                return true;
            }
        }

        if (isPlan && "GET".equals(method) && path.endsWith(SEGMENT_HOLDS)) {
            handleListHolds(response, controlSubjectId(path, PATH_PLANS_PREFIX, SEGMENT_HOLDS));
            return true;
        }

        if (isPlan && METHOD_POST.equals(method) && path.contains(SEGMENT_HOLDS_PATH)
                && path.endsWith("/resolve")) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var marker = sub.indexOf(SEGMENT_HOLDS_PATH);
            var subjectId = sub.substring(0, marker);
            var holdId = sub.substring(marker + SEGMENT_HOLDS_PATH.length(),
                    sub.length() - "/resolve".length());
            handleResolveHold(request, response, subjectId, holdId);
            return true;
        }

        if (isPlan && path.endsWith("/settings")) {
            var planId = controlSubjectId(path, PATH_PLANS_PREFIX, "/settings");
            if ("GET".equals(method)) {
                handleGetPlanSettings(response, planId);
                return true;
            }
            if ("PUT".equals(method)) {
                handleUpdatePlanSettings(request, response, planId);
                return true;
            }
        }

        if ("GET".equals(method) && path.endsWith("/tokens")) {
            var prefix = isPlan ? PATH_PLANS_PREFIX : PATH_EPICS_PREFIX;
            handleGetTokens(response, controlSubjectId(path, prefix, "/tokens"));
            return true;
        }

        if ("GET".equals(method) && isEpic && path.endsWith("/archive-check")) {
            handleGetArchiveCheck(response, controlSubjectId(path, PATH_EPICS_PREFIX, "/archive-check"));
            return true;
        }

        if ("GET".equals(method) && isEpic && path.endsWith(SEGMENT_DECOMPOSITION)) {
            handleGetControlDecomposition(response,
                    controlSubjectId(path, PATH_EPICS_PREFIX, SEGMENT_DECOMPOSITION));
            return true;
        }

        if ("GET".equals(method) && isEpic && path.endsWith("/review")) {
            handleGetReview(request, response, controlSubjectId(path, PATH_EPICS_PREFIX, "/review"));
            return true;
        }

        if ("GET".equals(method) && path.endsWith(SEGMENT_ENTRY_CHAT)) {
            var prefix = isPlan ? PATH_PLANS_PREFIX : PATH_EPICS_PREFIX;
            handleGetChat(response, controlSubjectId(path, prefix, SEGMENT_ENTRY_CHAT));
            return true;
        }

        if (isPlan && METHOD_POST.equals(method) && path.endsWith(SEGMENT_ENTRY_CHAT_TURNS)) {
            handleChatTurn(request, response, controlSubjectId(path, PATH_PLANS_PREFIX,
                    SEGMENT_ENTRY_CHAT_TURNS));
            return true;
        }

        if (isPlan && METHOD_POST.equals(method) && (path.endsWith("/chat/proposal/accept")
                || path.endsWith(SEGMENT_ENTRY_CHAT_PROPOSAL_REJECT))) {
            var accept = path.endsWith(SEGMENT_ENTRY_CHAT_PROPOSAL_ACCEPT);
            var suffix = accept ? SEGMENT_ENTRY_CHAT_PROPOSAL_ACCEPT : SEGMENT_ENTRY_CHAT_PROPOSAL_REJECT;
            handleChatProposal(response, controlSubjectId(path, PATH_PLANS_PREFIX, suffix), accept);
            return true;
        }

        if ("GET".equals(method) && isEpic) {
            var epicId = path.substring(PATH_EPICS_PREFIX.length());
            if (!epicId.isEmpty() && !epicId.contains("/")) {
                handleGetEpicResource(response, epicId);
                return true;
            }
        }

        return false;
    }

    private Map<String, Object> readBodyMap(MockHttpRequest request) {
        var body = request.body();
        var parsed = body == null || body.length == 0 ? null : parseBody(body);
        return parsed != null ? parsed : new LinkedHashMap<>();
    }

    private @Nullable Map<String, Object> parseBody(byte[] body) {
        try {
            return JsonTree.asObject(JsonTree.parse(body));
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException _) {
            return null;
        }
    }

    private @Nullable HoldRecord holdFor(String planId) {
        for (var hold : controlHolds.values()) {
            if (hold.subjectId().equals(planId)) {
                return hold;
            }
        }
        return null;
    }

    private void removeHolds(String planId) {
        controlHolds.values().removeIf(hold -> hold.subjectId().equals(planId));
    }

    private void handleGetPlanActions(MockHttpResponse response, String planId) throws IOException {
        synchronized (controlLock) {
            response.json(200, buildPlanActions(planId).toMap());
        }
    }

    private void handleGetEpicActions(MockHttpResponse response, String epicId) throws IOException {
        synchronized (controlLock) {
            response.json(200, buildEpicActions(epicId).toMap());
        }
    }

    private void handlePlanAction(MockHttpRequest request, MockHttpResponse response, String planId, String action)
            throws IOException {
        var actionRequest = PlanActionRequest.fromMap(readBodyMap(request));
        synchronized (controlLock) {
            if (!controlPlanStates.containsKey(planId)) {
                response.json(404, Map.of(KEY_ERROR, ERR_PLAN_NOT_FOUND, KEY_MESSAGE, MESSAGE_PLAN_NOT_FOUND + planId));
                return;
            }
            var availability = buildPlanActions(planId).action(action);
            if (availability == null) {
                response.json(409, Map.of(KEY_ERROR, "action_not_allowed", KEY_MESSAGE,
                        planId + " is " + controlPlanStates.get(planId)));
                return;
            }
            if (!availability.available()) {
                response.json(409, Map.of(KEY_ERROR, "action_disabled",
                        KEY_REASON, availability.reason() != null ? availability.reason() : ""));
                return;
            }
            switch (action) {
                case ACTION_PAUSE -> {
                    var point = actionRequest.point() != null ? actionRequest.point() : "task";
                    controlPlanStates.put(planId, "pausing");
                    controlPlanPausePoints.put(planId, point);
                    broadcastEvent(PlanControlEvent.EVENT_PAUSED, new PlanControlEvent(
                            PlanControlEvent.EVENT_PAUSED, planId, "pausing", point, null).toMap());
                }
                case ACTION_START -> {
                    controlPlanStates.put(planId, "running");
                    controlPlanPausePoints.remove(planId);
                    broadcastEvent(PlanControlEvent.EVENT_RESUMED, new PlanControlEvent(
                            PlanControlEvent.EVENT_RESUMED, planId, "running", null, null).toMap());
                }
                case ACTION_CANCEL, ACTION_ABANDON -> {
                    controlPlanStates.put(planId, "ended");
                    controlPlanPausePoints.remove(planId);
                    removeHolds(planId);
                    broadcastEvent(PlanControlEvent.EVENT_CANCELLED, new PlanControlEvent(
                            PlanControlEvent.EVENT_CANCELLED, planId, "ended", null,
                            actionRequest.reason() != null ? actionRequest.reason() : "Cancelled by operator").toMap());
                }
                default -> {
                    // chat and settings open a dialogue; the state is unchanged
                }
            }
            var resp = new LinkedHashMap<String, Object>();
            resp.put(KEY_STATUS, "ok");
            resp.putAll(buildPlanActions(planId).toMap());
            response.json(200, resp);
        }
    }

    private void handleEpicAction(MockHttpRequest request, MockHttpResponse response, String epicId, String action)
            throws IOException {
        var actionRequest = PlanActionRequest.fromMap(readBodyMap(request));
        synchronized (controlLock) {
            if (!controlEpicStates.containsKey(epicId)) {
                response.json(404, Map.of(KEY_ERROR, "epic_not_found", KEY_MESSAGE, "Epic not found: " + epicId));
                return;
            }
            var availability = buildEpicActions(epicId).action(action);
            if (availability == null) {
                response.json(409, Map.of(KEY_ERROR, "action_not_allowed", KEY_MESSAGE,
                        epicId + " is " + controlEpicStates.get(epicId)));
                return;
            }
            if (!availability.available()) {
                response.json(409, Map.of(KEY_ERROR, "action_disabled",
                        KEY_REASON, availability.reason() != null ? availability.reason() : ""));
                return;
            }
            switch (action) {
                case ACTION_AUTO -> applyAuto(epicId, actionRequest);
                case ACTION_COMMON_FILES -> {
                    var value = actionRequest.extra().get("count");
                    if (value instanceof Number number) {
                        controlEpicOverlap.put(epicId, number.intValue());
                    }
                }
                case ACTION_CANCEL -> controlEpicStates.put(epicId, "ended");
                default -> {
                    // next, decompose, findings, watches, archive, chat: the epic rests between its events
                }
            }
            var resp = new LinkedHashMap<String, Object>();
            resp.put(KEY_STATUS, "ok");
            resp.put("auto_enabled", controlEpicAuto.getOrDefault(epicId, true));
            resp.put(FIELD_OVERLAP_MAX_FILES, controlEpicOverlap.getOrDefault(epicId, 0));
            resp.putAll(buildEpicActions(epicId).toMap());
            response.json(200, resp);
        }
    }

    private void applyAuto(String epicId, PlanActionRequest actionRequest) {
        var enabled = actionRequest.extra().get(FIELD_ENABLED);
        if (enabled instanceof Boolean flag) {
            controlEpicAuto.put(epicId, flag);
        } else {
            var mode = actionRequest.extra().get("mode");
            if (mode != null) {
                controlEpicAuto.put(epicId, "on".equalsIgnoreCase(String.valueOf(mode)));
            }
        }
        var commonFiles = actionRequest.extra().get(ACTION_COMMON_FILES);
        if (commonFiles instanceof Number number) {
            controlEpicOverlap.put(epicId, number.intValue());
        }
    }

    private void handleListHolds(MockHttpResponse response, String planId) throws IOException {
        var list = new ArrayList<Map<String, Object>>();
        synchronized (controlLock) {
            for (var hold : controlHolds.values()) {
                if (hold.subjectId().equals(planId)) {
                    list.add(hold.toMap());
                }
            }
        }
        response.json(200, list);
    }

    private void handleResolveHold(MockHttpRequest request, MockHttpResponse response, String planId, String holdId)
            throws IOException {
        var resolution = HoldResolutionRequest.fromMap(readBodyMap(request));
        synchronized (controlLock) {
            var hold = controlHolds.get(holdKey(planId, holdId));
            if (hold == null) {
                response.json(404, Map.of(KEY_ERROR, "hold_not_found", KEY_MESSAGE, "Hold not found: " + holdId));
                return;
            }
            if (!hold.offers(resolution.act())) {
                response.json(409, Map.of(KEY_ERROR, "act_not_allowed", KEY_MESSAGE,
                        "Hold " + holdId + " does not offer act " + resolution.act()));
                return;
            }
            var act = resolution.act();
            var ending = ACTION_ABORT.equals(act) || "end_plan".equals(act) || "cancel_epic".equals(act);
            var released = ACTION_ACCEPT.equals(act) || ACTION_RETRY.equals(act) || "grant".equals(act)
                    || "add_credential".equals(act) || "raise_budget".equals(act)
                    || "give_up_removal".equals(act) || "recycle".equals(act);
            if ("integrity_pause".equals(hold.kind()) && ACTION_RETRY.equals(act)) {
                released = false;
            }
            HoldResolutionResult result;
            if (ending) {
                clearHoldsOf(planId);
                controlPlanStates.put(planId, "ended");
                controlPlanPausePoints.remove(planId);
                var endReason = resolution.reason();
                if (endReason == null) {
                    endReason = ACTION_ABORT.equals(act) ? "Integrity pause aborted" : "Ended by operator";
                }
                broadcastEvent(PlanControlEvent.EVENT_CANCELLED, new PlanControlEvent(
                        PlanControlEvent.EVENT_CANCELLED, planId, "ended", null, endReason).toMap());
                result = new HoldResolutionResult(holdId, true, null, planId);
            } else if (released) {
                controlHolds.remove(holdKey(planId, holdId));
                if ("held".equals(controlPlanStates.get(planId))) {
                    controlPlanStates.put(planId, "running");
                }
                broadcastEvent(HoldChangedEvent.EVENT_CLEARED, new HoldChangedEvent(HoldChangedEvent.EVENT_CLEARED,
                        hold.holdId(), hold.subjectId(), hold.subjectKind(), null, null).toMap());
                result = new HoldResolutionResult(holdId, true, null, planId);
            } else {
                var updated = new HoldRecord(hold.holdId(), hold.subjectId(), hold.subjectKind(), hold.kind(),
                        hold.cause(), hold.attempts() + 1, Instant.now(), hold.filePath(), hold.recordedSize(),
                        hold.recordedDigest(), hold.currentSize(), hold.currentDigest(), hold.acts(),
                        hold.pullRequestUrl());
                controlHolds.put(holdKey(planId, holdId), updated);
                broadcastEvent(HoldChangedEvent.EVENT_CHANGED, new HoldChangedEvent(HoldChangedEvent.EVENT_CHANGED,
                        updated.holdId(), updated.subjectId(), updated.subjectKind(), updated.kind(),
                        updated.cause()).toMap());
                result = new HoldResolutionResult(holdId, false, updated.cause(), planId);
            }
            response.json(200, result.toMap());
        }
    }

    /**
     * Clears every hold the plan carries, because ending the plan ends all of them.
     *
     * @param planId the plan being ended
     */
    private void clearHoldsOf(String planId) {
        var cleared = new ArrayList<HoldRecord>();
        var iterator = controlHolds.values().iterator();
        while (iterator.hasNext()) {
            var candidate = iterator.next();
            if (planId.equals(candidate.subjectId())) {
                cleared.add(candidate);
                iterator.remove();
            }
        }
        for (var done : cleared) {
            broadcastEvent(HoldChangedEvent.EVENT_CLEARED, new HoldChangedEvent(HoldChangedEvent.EVENT_CLEARED,
                    done.holdId(), done.subjectId(), done.subjectKind(), null, null).toMap());
        }
    }

    private void handleGetPlanSettings(MockHttpResponse response, String planId) throws IOException {
        synchronized (controlLock) {
            var values = controlSettings.get(planId);
            var revision = controlSettingsRevisions.getOrDefault(planId, 1);
            var list = values != null ? new ArrayList<>(values.values()) : new ArrayList<PlanSettingsValue>();
            response.json(200, new PlanSettingsRecord(planId, revision, list).toMap());
        }
    }

    private void handleUpdatePlanSettings(MockHttpRequest request, MockHttpResponse response, String planId)
            throws IOException {
        var update = PlanSettingsUpdateRequest.fromMap(readBodyMap(request));
        synchronized (controlLock) {
            var revision = controlSettingsRevisions.getOrDefault(planId, 1);
            if ("ended".equals(controlPlanStates.get(planId))) {
                response.json(409, PlanSettingsUpdateResult.tooLate("ended").toMap());
                return;
            }
            if (update.revision() != revision) {
                response.json(409, PlanSettingsUpdateResult.conflict(revision, currentValue(planId, update.aspect())).toMap());
                return;
            }
            if ("landing".equals(update.aspect()) && "landing.main".equals(update.value())) {
                response.json(400, Map.of(KEY_ERROR, "refused",
                        KEY_REASON, "the base branch of pm-mcp-clients is protected",
                        "value", update.value()));
                return;
            }
            var values = controlSettings.computeIfAbsent(planId, _ -> new LinkedHashMap<>());
            var newRevision = revision + 1;
            values.put(update.aspect(), new PlanSettingsValue(update.aspect(), update.value(), "plan", List.of()));
            controlSettingsRevisions.put(planId, newRevision);
            response.json(200, PlanSettingsUpdateResult.applied(newRevision).toMap());
        }
    }

    private String currentValue(String planId, String aspect) {
        var values = controlSettings.get(planId);
        if (values != null && values.get(aspect) != null && values.get(aspect).value() != null) {
            return values.get(aspect).value();
        }
        return "";
    }

    private void handleGetEpicResource(MockHttpResponse response, String epicId) throws IOException {
        synchronized (controlLock) {
            var resource = controlEpics.get(epicId);
            if (resource == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND, KEY_MESSAGE, "Epic not found: " + epicId));
                return;
            }
            var map = new LinkedHashMap<>(resource.toMap());
            var overlap = controlEpicOverlap.get(epicId);
            if (overlap != null) {
                map.put(FIELD_OVERLAP_MAX_FILES, overlap);
            }
            response.json(200, map);
        }
    }

    private void handleGetTokens(MockHttpResponse response, String subjectId) throws IOException {
        synchronized (controlLock) {
            var epic = controlEpics.get(subjectId);
            if (epic != null) {
                response.json(200, epic.tokens().toMap());
                return;
            }
            for (var resource : controlEpics.values()) {
                for (var plan : resource.tokens().plans()) {
                    if (plan.planId().equals(subjectId)) {
                        response.json(200, plan.figure().toMap());
                        return;
                    }
                }
            }
            response.json(404, Map.of(KEY_ERROR, "tokens_not_found", KEY_MESSAGE, "No tokens for " + subjectId));
        }
    }

    private void handleGetArchiveCheck(MockHttpResponse response, String epicId) throws IOException {
        synchronized (controlLock) {
            var check = controlArchiveChecks.get(epicId);
            if (check == null) {
                response.json(404, Map.of(KEY_ERROR, "epic_not_found",
                        KEY_MESSAGE, "No archive check for epic: " + epicId));
                return;
            }
            response.json(200, check.toMap());
        }
    }

    private void handleGetReview(MockHttpRequest request, MockHttpResponse response, String epicId)
            throws IOException {
        var register = queryValue(request, "register");
        var wanted = register != null && !register.isBlank() ? register : FIELD_FINDINGS;
        synchronized (controlLock) {
            var review = controlReviews.get(reviewKey(epicId, wanted));
            if (review == null) {
                response.json(404, Map.of(KEY_ERROR, "review_not_found",
                        KEY_MESSAGE, "No " + wanted + " review for epic: " + epicId));
                return;
            }
            response.json(200, review.toMap());
        }
    }

    private void handleGetControlDecomposition(MockHttpResponse response, String epicId) throws IOException {
        synchronized (decompositionLock) {
            var cut = decompositions.get(epicId);
            if (cut == null) {
                response.json(404, Map.of(KEY_ERROR, ERR_EPIC_NOT_FOUND,
                        KEY_MESSAGE, "No decomposition for epic: " + epicId));
                return;
            }
            response.json(200, toControlDecomposition(cut).toMap());
        }
    }

    private static DecompositionResource toControlDecomposition(EpicDecomposition cut) {
        var workstreams = new ArrayList<DecompositionWorkstream>();
        var workstreamOrder = 1;
        for (var workstream : cut.workstreams()) {
            var plans = new ArrayList<DecompositionPlan>();
            var planOrder = 1;
            for (var plan : workstream.stagedPlans()) {
                var change = StagedPlanRecord.STATUS_STAGED.equals(plan.status()) ? null : plan.status();
                plans.add(new DecompositionPlan(plan.planId(), plan.title(), plan.objective(), List.of(),
                        plan.expectedSurface(), plan.dependencies(), planOrder, change, plan.isLocked()));
                planOrder++;
            }
            workstreams.add(new DecompositionWorkstream(workstream.id(), workstream.name(),
                    workstream.description(), workstreamOrder, plans));
            workstreamOrder++;
        }
        var checks = new ArrayList<DecompositionCheck>();
        for (var finding : cut.findings()) {
            var planId = finding.affectedPlans().isEmpty() ? null : finding.affectedPlans().getFirst();
            checks.add(new DecompositionCheck(planId, finding.severity().wireValue(), finding.message()));
        }
        return new DecompositionResource(cut.epicId(), cut.status().wireValue(), cut.revision(), workstreams,
                checks);
    }

    private void handleGetChat(MockHttpResponse response, String subjectId) throws IOException {
        synchronized (controlLock) {
            var chat = controlChats.get(subjectId);
            if (chat == null) {
                response.json(404, Map.of(KEY_ERROR, "chat_not_found", KEY_MESSAGE, "No chat for " + subjectId));
                return;
            }
            response.json(200, chat.toMap());
        }
    }

    private void handleChatTurn(MockHttpRequest request, MockHttpResponse response, String planId)
            throws IOException {
        var body = readBodyMap(request);
        var text = body.get(FIELD_TEXT) instanceof String value ? value : "";
        synchronized (controlLock) {
            var chat = controlChats.get(planId);
            if (chat == null) {
                response.json(404, Map.of(KEY_ERROR, "chat_not_found", KEY_MESSAGE, "No chat for " + planId));
                return;
            }
            var at = Instant.now().toString();
            var turns = new ArrayList<>(chat.turns());
            turns.add(new ChatTurnResource(ChatTurnResource.ROLE_OPERATOR, text, at, false));
            turns.add(new ChatTurnResource(chat.role(),
                    "I have read the plan; say what should change, or ask on.", at, false));
            var updated = new ChatReadBackResource(chat.chatId(), chat.subjectId(), chat.subjectKind(),
                    chat.role(), chat.scope(), ChatState.TURN.wireValue(), turns, null);
            controlChats.put(planId, updated);
            response.json(200, updated.toMap());
        }
    }

    private void handleChatProposal(MockHttpResponse response, String planId, boolean accept) throws IOException {
        synchronized (controlLock) {
            var chat = controlChats.get(planId);
            if (chat == null) {
                response.json(404, Map.of(KEY_ERROR, "chat_not_found", KEY_MESSAGE, "No chat for " + planId));
                return;
            }
            if (!chat.hasProposal()) {
                response.json(409, Map.of(KEY_ERROR, "no_proposal",
                        KEY_MESSAGE, "No open proposal for " + planId));
                return;
            }
            var state = accept ? ChatState.APPLY.wireValue() : ChatState.TURN.wireValue();
            var updated = new ChatReadBackResource(chat.chatId(), chat.subjectId(), chat.subjectKind(),
                    chat.role(), chat.scope(), state, chat.turns(), null);
            controlChats.put(planId, updated);
            response.json(200, updated.toMap());
        }
    }

    private static String queryValue(MockHttpRequest request, String name) {
        var query = request.query();
        if (query == null) {
            return null;
        }
        for (var part : query.split("&")) {
            var separator = part.indexOf('=');
            var key = separator < 0 ? part : part.substring(0, separator);
            if (key.equals(name)) {
                return separator < 0 ? "" : URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void initDefaultReports() {
        var fixture = startRuntimeFixture();
        var reportsDeclared = fixture != null && fixture.containsKey(FIXTURE_REPORTS);
        synchronized (reportsLock) {
            mockReports.clear();
            var fixtureReports = fixtureObjects(fixture, FIXTURE_REPORTS);
            if (!reportsDeclared) {
                mockReports.add(new ReportItem(
                        "rep-0142",
                        new ReportFrom(CONST_PM_MCP_CLIENTS, CONST_RELAY_HARDENING, CONST_FIX_RELAY),
                        ReportKind.PLAN_ENDED,
                        "fix-relay ended: merged (#214)",
                        Map.of("plan", CONST_FIX_RELAY, CONST_OUTCOME, "merged (#214)"),
                        "3 of 3 deliverables · 2 h 40 min",
                        Instant.parse(CONST_TIME_11_20),
                        Instant.parse(CONST_TIME_11_20),
                        null, null, null,
                        "/api/v1/plans/fix-relay/completion"
                ));
                mockReports.add(new ReportItem(
                        "rep-0143",
                        new ReportFrom(CONST_PM_MCP_CLIENTS, CONST_RELAY_HARDENING, CONST_RELAY_TOKEN_STORE),
                        ReportKind.PLAN_STARTED,
                        "relay-token-store started by relay-hardening (2 of 3 at once)",
                        Map.of("plan", CONST_RELAY_TOKEN_STORE, "epic", CONST_RELAY_HARDENING, "running", 2, "scope", 3, "by", "epic"),
                        null,
                        Instant.parse(CONST_TIME_11_21),
                        Instant.parse(CONST_TIME_11_21),
                        null, null, null,
                        "/api/v1/plans/relay-token-store"
                ));
                mockReports.add(new ReportItem(
                        "rep-0144",
                        new ReportFrom(CONST_PM_MCP_CLIENTS, CONST_RELAY_HARDENING, "relay-docs"),
                        ReportKind.EPIC_WAITS_OVERLAP,
                        "relay-docs waits: overlap with relay-auth-v2 (2 files)",
                        Map.of("plan", "relay-docs", "other", CONST_RELAY_AUTH_V2, "files", 2),
                        null,
                        Instant.parse(CONST_TIME_11_21),
                        Instant.parse(CONST_TIME_11_21),
                        null, null, null,
                        "/api/v1/epics/relay-hardening"
                ));
                mockReports.add(new ReportItem(
                        "rep-0139",
                        new ReportFrom(CONST_PM_MCP_CLIENTS, null, CONST_DOCS_TYPO),
                        ReportKind.PLAN_ENDED,
                        "docs-typo ended: no change",
                        Map.of("plan", CONST_DOCS_TYPO, CONST_OUTCOME, "no change"),
                        null,
                        Instant.parse("2026-10-16T09:02:00Z"),
                        Instant.parse("2026-10-16T09:40:00Z"),
                        null, null, null,
                        "/api/v1/plans/docs-typo/completion"
                ));
                mockReports.add(new ReportItem(
                        "rep-0131",
                        new ReportFrom(CONST_PM_MCP_CORE, null, null),
                        ReportKind.PROJECT_ONBOARDED,
                        "pm-mcp-core is onboarded",
                        Map.of("project", CONST_PM_MCP_CORE),
                        null,
                        Instant.parse("2026-10-15T18:40:00Z"),
                        Instant.parse("2026-10-15T18:40:00Z"),
                        null, null, null,
                        "/api/v1/projects/pm-mcp-core"
                ));
                mockReports.add(new ReportItem(
                        "rep-0120",
                        new ReportFrom(CONST_PM_MCP_CLIENTS, CONST_RELAY_HARDENING, CONST_RELAY_AUTH_V2),
                        ReportKind.PLAN_ENDED,
                        "relay-auth-v2 ended: merged (#210)",
                        Map.of("plan", CONST_RELAY_AUTH_V2, CONST_OUTCOME, "merged (#210)"),
                        null,
                        Instant.parse(CONST_TIME_10_14),
                        Instant.parse(CONST_TIME_10_14),
                        null, null, null,
                        "/api/v1/plans/relay-auth-v2/completion"
                ));
                mockReports.add(new ReportItem(
                        "rep-0110",
                        new ReportFrom(CONST_PM_MCP_CORE, null, null),
                        ReportKind.SOURCE_FLAGGED,
                        "A source of codeberg-issues was flagged: issue #4711",
                        Map.of("item", "codeberg-issues", "source", "issue #4711"),
                        null,
                        Instant.parse("2026-10-13T08:00:00Z"),
                        Instant.parse("2026-10-13T08:00:00Z"),
                        null, null, null,
                        "/api/v1/sources/codeberg-issues"
                ));
            } else {
                for (var report : fixtureReports) {
                    mockReports.add(readFixture(() -> ReportItem.fromMap(report), "facts.runtime.reports"));
                }
            }
        }

        completionReports.clear();
        if (seedCompletionReportsFromFixture(fixture)) {
            return;
        }
        var fixRelayDeliverables = List.of(
                new DeliverableSummary("cursor write order", "done"),
                new DeliverableSummary("a test per transport, websocket and SSE", "done"),
                new DeliverableSummary("fsync of the cursor file", "done")
        );
        var fixRelayReplanning = List.of(
                new ReplanningRecord("the base moved", "09:55", "TASK-5 added")
        );
        var fixRelayDecisions = List.of(
                new DecisionRecord("Q-003", "Rebase onto main", "rebase", "operator", "web", "iPad"),
                new DecisionRecord("Q-005", "Pick transport", "sse", "delegated", null, null)
        );
        var fixRelayOwnCalls = List.of(
                new OwnCallRecord("shortcut", "reuse the existing SSE adapter for the websocket path",
                        "the single-subscriber path only", "a second subscriber connects", "conflicts",
                        "a test per transport, websocket and SSE"),
                new OwnCallRecord("design", "the relay writes the cursor, the client never does",
                        null, null, "consistent", null),
                new OwnCallRecord("dependency", "a file-backed cursor replaces the in-memory one",
                        null, null, "consistent", null)
        );
        var fixRelayChangeMap = new ChangeMapRecord(
                "relay ──> cursor ──> fsync\n  └────> sse ──────> websocket (not traced)",
                "partial", "the vendored tree is not tracked");
        var fixRelayComponents = new TokenComponentRecord(310000L, 62000L, 8000L, 29000L, 3000L);
        var fixRelayPhase = List.of(
                new TokenBreakdownItem("refine", 38000, 35000, 3000, 0, false, null, null),
                new TokenBreakdownItem("outline", 96000, 90000, 6000, 0, false, null, null),
                new TokenBreakdownItem(PHASE_EXECUTE, 241000, 222000, 19000, 3, true, null, fixRelayComponents),
                new TokenBreakdownItem("finalize", 37000, 33000, 4000, 0, false, null, null)
        );
        var fixRelayRole = List.of(
                new TokenBreakdownItem("planner", 128000, 120000, 8000, 0, false, "claude:opus-5.5 · high", null),
                new TokenBreakdownItem("implementer", 270000, 247000, 23000, 0, false, "claude:sonnet-5.5 · medium", null),
                new TokenBreakdownItem("reviewer", 14000, 13000, 1000, 3, true, "codex:gpt-5 · low", null)
        );
        var fixRelayModel = List.of(
                new TokenBreakdownItem("claude:opus-5.5", 128000, 120000, 8000, 0, false, null, null),
                new TokenBreakdownItem("claude:sonnet-5.5", 270000, 247000, 23000, 0, false, null, null),
                new TokenBreakdownItem("codex:gpt-5", 14000, 13000, 1000, 3, true, null, null)
        );
        var fixRelayWaste = List.of(
                new TokenWasteRecord("no_product", 18000, 4, 2, "2 tasks"),
                new TokenWasteRecord("retries", 21000, 5, 2, "retryable 1 · stall 1"),
                new TokenWasteRecord("remediation", 13000, 4, 1, "returned with findings 1")
        );
        var fixRelayTokens = new TokenUsageRecord(
                412000, 380000, 32000, 9600, 6, 3, 38, 41, true,
                "their worker was terminated",
                fixRelayPhase, fixRelayRole, fixRelayModel, fixRelayWaste, 52000, 13
        );
        completionReports.put(CONST_FIX_RELAY, new PlanCompletionReport(
                CONST_FIX_RELAY,
                CONST_FIX_RELAY,
                new ReportFrom(CONST_PM_MCP_CLIENTS, CONST_RELAY_HARDENING, CONST_FIX_RELAY),
                "merged",
                "the pull request #214 was merged into main",
                214,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                "—",
                Instant.parse("2026-10-16T08:40:00Z"),
                Instant.parse(CONST_TIME_11_20),
                9600,
                1200,
                "you",
                fixRelayDeliverables,
                fixRelayReplanning,
                fixRelayDecisions,
                fixRelayTokens,
                fixRelayOwnCalls,
                fixRelayChangeMap
        ));

        completionReports.put(CONST_DOCS_TYPO, new PlanCompletionReport(
                CONST_DOCS_TYPO,
                CONST_DOCS_TYPO,
                new ReportFrom(CONST_PM_MCP_CLIENTS, null, CONST_DOCS_TYPO),
                "no_change",
                "no changes produced",
                null,
                null,
                "—",
                Instant.parse("2026-10-16T08:40:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                1200,
                0,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        ));
    }

    /**
     * Seeds the completion reports from the active scenario's {@code facts.runtime.completion_reports}
     * fixture, so a case state serves the outcome, waivers, phase timeline, and token breakdown it
     * declares.
     *
     * @param fixture the start state's runtime fixture, or {@code null}
     * @return {@code true} when the fixture declares the completion reports, so the Java defaults are
     *         not used; an explicitly empty array declares none
     */
    private boolean seedCompletionReportsFromFixture(@Nullable Map<String, Object> fixture) {
        if (fixture == null || !fixture.containsKey(FIXTURE_COMPLETION_REPORTS)) {
            return false;
        }
        for (var entry : fixtureObjects(fixture, FIXTURE_COMPLETION_REPORTS)) {
            var report = readFixture(() -> PlanCompletionReport.fromMap(entry),
                    "facts.runtime.completion_reports");
            completionReports.put(report.planId(), report);
        }
        return true;
    }

    /**
     * Helper to inject and broadcast a new report (for tests or simulations).
     *
     * @param item report item
     */
    public void addReportAndBroadcast(ReportItem item) {
        synchronized (reportsLock) {
            mockReports.add(0, item);
        }
        broadcastEvent("report.sent", Map.of("report", item.toMap()));
    }

    /**
     * Serves the intent and outline records and the {@code over_size} distribution. The fixtures are
     * static routes of a scenario; a request for another plan, epic, or project id is answered from
     * the same fixture with its id replaced, so the client's dynamic address is exercised without a
     * fixture per id.
     *
     * @param request  the request
     * @param response the response
     * @return {@code true} if the request was answered here
     * @throws IOException on write failure
     */
    private boolean handleIntentRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (!"GET".equals(request.method())) {
            return false;
        }
        var path = request.path();
        if (hasIdBetween(path, PATH_PLANS_PREFIX, SUFFIX_INTENT)) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var planId = sub.substring(0, sub.length() - SUFFIX_INTENT.length());
            return !planId.contains("/")
                    && serveIntentFixture(response, FIXTURE_PLAN_INTENT, PlanIntentResource::fromMap,
                    PlanIntentResource.FIELD_PLAN_ID, planId);
        }
        if (hasIdBetween(path, PATH_PLANS_PREFIX, SUFFIX_OUTLINE)) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var planId = sub.substring(0, sub.length() - SUFFIX_OUTLINE.length());
            return !planId.contains("/")
                    && serveIntentFixture(response, FIXTURE_PLAN_OUTLINE, OutlineResource::fromMap,
                    OutlineResource.FIELD_PLAN_ID, planId);
        }
        if (hasIdBetween(path, PATH_EPICS_PREFIX, SUFFIX_INTENT)) {
            var sub = path.substring(PATH_EPICS_PREFIX.length());
            var epicId = sub.substring(0, sub.length() - SUFFIX_INTENT.length());
            return !epicId.contains("/")
                    && serveIntentFixture(response, FIXTURE_EPIC_INTENT, EpicIntentResource::fromMap,
                    EpicIntentResource.FIELD_EPIC_ID, epicId);
        }
        if (hasIdBetween(path, PREFIX_PROJECTS, SEGMENT_METRICS_OVERSIZE)) {
            var sub = path.substring(PREFIX_PROJECTS.length());
            var projectId = sub.substring(0, sub.length() - SEGMENT_METRICS_OVERSIZE.length());
            return !projectId.contains("/")
                    && serveIntentFixture(response, FIXTURE_OVERSIZE, OverSizeDistributionResource::fromMap,
                    OverSizeDistributionResource.FIELD_PROJECT_ID, projectId);
        }
        return false;
    }

    /**
     * @param path   the request path
     * @param prefix the fixed prefix
     * @param suffix the fixed suffix
     * @return {@code true} if the path carries at least one character between prefix and suffix
     */
    private static boolean hasIdBetween(String path, String prefix, String suffix) {
        return path.startsWith(prefix) && path.endsWith(suffix)
                && path.length() > prefix.length() + suffix.length();
    }

    private boolean serveIntentFixture(MockHttpResponse response, String fixturePath, FixtureReader reader,
            String idField, String id) throws IOException {
        var route = activeRoute("GET", fixturePath);
        if (route == null) {
            return false;
        }
        var representation = JsonTree.asObject(route.representation());
        reader.read(representation);
        var copy = new LinkedHashMap<String, Object>(representation);
        copy.put(idField, id);
        response.json(200, copy);
        return true;
    }

    @FunctionalInterface
    private interface FixtureReader {

        /**
         * @param representation the fixture's representation
         * @throws IOException if the fixture is not the contract's shape
         */
        void read(Map<String, Object> representation) throws IOException;
    }

    private boolean handleReportRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        var method = request.method();

        if ("GET".equals(method) && ("/api/v1/reports".equals(path) || PATH_REPORTS_PREFIX.equals(path))) {
            handleListReports(request, response);
            return true;
        }
        if (METHOD_POST.equals(method) && "/api/v1/reports/read".equals(path)) {
            handleMarkReportsRead(request, response, isWeb);
            return true;
        }
        if (path.startsWith(PATH_REPORTS_PREFIX)) {
            var sub = path.substring(PATH_REPORTS_PREFIX.length());
            if (sub.endsWith("/read") && METHOD_POST.equals(method)) {
                var reportId = sub.substring(0, sub.length() - "/read".length());
                handleMarkSingleReportRead(response, reportId, isWeb);
                return true;
            }
            if ("GET".equals(method) && !sub.contains("/")) {
                handleGetReport(response, sub);
                return true;
            }
        }
        if ("GET".equals(method) && path.startsWith(PATH_PLANS_PREFIX) && path.endsWith("/completion")) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var planId = sub.substring(0, sub.length() - "/completion".length());
            handleGetPlanCompletion(response, planId);
            return true;
        }
        return false;
    }

    private void handleListReports(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var query = request.query() != null ? request.query() : "";
        boolean older = query.contains("older=true");
        String projectFilter = null;
        for (var part : query.split("&")) {
            if (part.startsWith("project=")) {
                projectFilter = part.substring("project=".length());
            }
        }
        if (".".equals(projectFilter)) {
            projectFilter = CONST_PM_MCP_CLIENTS;
        }

        List<ReportItem> allUnread = new ArrayList<>();
        synchronized (reportsLock) {
            for (var rep : mockReports) {
                if (!rep.isRead() && (projectFilter == null || projectFilter.isBlank() || projectFilter.equals(rep.from().projectId()))) {
                    allUnread.add(rep);
                }
            }
        }

        allUnread.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

        var pagedResult = paginateUnread(allUnread, older, 5);

        Instant oldestUnread = null;
        if (!allUnread.isEmpty()) {
            oldestUnread = allUnread.getLast().createdAt();
        }

        var page = new ReportsPage(pagedResult.paged(), allUnread.size(), oldestUnread, pagedResult.hasOlder(), pagedResult.olderCount());
        response.json(200, page.toMap());
    }

    private record PagedResult(List<ReportItem> paged, boolean hasOlder, int olderCount) {
    }

    private static PagedResult paginateUnread(List<ReportItem> allUnread, boolean older, int pageSize) {
        if (!older) {
            if (allUnread.size() > pageSize) {
                return new PagedResult(new ArrayList<>(allUnread.subList(0, pageSize)), true, allUnread.size() - pageSize);
            }
            return new PagedResult(new ArrayList<>(allUnread), false, 0);
        }
        if (allUnread.size() > pageSize) {
            return new PagedResult(new ArrayList<>(allUnread.subList(pageSize, allUnread.size())), false, 0);
        }
        return new PagedResult(List.of(), false, 0);
    }

    private void handleGetReport(MockHttpResponse response, String reportId) throws IOException {
        ReportItem target = null;
        synchronized (reportsLock) {
            for (var rep : mockReports) {
                if (rep.id().equals(reportId)) {
                    target = rep;
                    break;
                }
            }
        }
        if (target != null) {
            response.json(200, target.toMap());
        } else {
            response.json(404, Map.of(KEY_ERROR, "report_not_found", KEY_MESSAGE, "Report not found: " + reportId));
        }
    }

    private static ReportItem markAsRead(ReportItem rep, boolean isWeb) {
        return new ReportItem(
                rep.id(), rep.from(), rep.kind(), rep.subject(), rep.subjectParams(),
                rep.body(), rep.createdAt(), rep.sentAt(), Instant.now(),
                isWeb ? "web" : "cli", isWeb ? "iPad" : KEY_TERMINAL, rep.detailLink()
        );
    }

    private void broadcastReportRead(String reportId, boolean isWeb) {
        broadcastEvent("report.read", Map.of(
                "report_id", reportId,
                "read_at", Instant.now().toString(),
                "channel", isWeb ? "web" : "cli",
                "device", isWeb ? "iPad" : KEY_TERMINAL
        ));
    }

    private void handleMarkSingleReportRead(MockHttpResponse response, String reportId, boolean isWeb) throws IOException {
        ReportItem target = null;
        synchronized (reportsLock) {
            for (int i = 0; i < mockReports.size(); i++) {
                var rep = mockReports.get(i);
                if (rep.id().equals(reportId)) {
                    target = rep;
                    if (!rep.isRead()) {
                        mockReports.set(i, markAsRead(rep, isWeb));
                    }
                    break;
                }
            }
        }
        if (target == null) {
            response.json(404, Map.of(KEY_ERROR, "report_not_found", KEY_MESSAGE, "Report not found: " + reportId));
            return;
        }
        if (target.isRead()) {
            response.json(200, Map.of(KEY_COUNT, 0, KEY_MARKED_COUNT, 0, KEY_STATUS, "already_read"));
            return;
        }
        broadcastReportRead(target.id(), isWeb);
        response.json(200, Map.of(KEY_COUNT, 1, KEY_MARKED_COUNT, 1, KEY_STATUS, "ok"));
    }

    private void handleMarkReportsRead(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        boolean all = false;
        List<String> ids = List.of();
        if (request.body().length > 0) {
            try {
                var req = ReadReportsRequest.fromJson(request.bodyText());
                all = req.all();
                ids = req.reportIds();
            } catch (IOException _) {
                // Ignore parse errors from malformed body
            }
        }
        var marked = new ArrayList<ReportItem>();
        synchronized (reportsLock) {
            for (int i = 0; i < mockReports.size(); i++) {
                var rep = mockReports.get(i);
                if (!rep.isRead() && (all || ids.contains(rep.id()))) {
                    var readItem = markAsRead(rep, isWeb);
                    mockReports.set(i, readItem);
                    marked.add(readItem);
                }
            }
        }
        for (var m : marked) {
            broadcastReportRead(m.id(), isWeb);
        }
        response.json(200, Map.of(KEY_COUNT, marked.size(), KEY_MARKED_COUNT, marked.size(), KEY_STATUS, "ok"));
    }

    private void handleGetPlanCompletion(MockHttpResponse response, String planId) throws IOException {
        if (CONST_RELAY_TOKEN_STORE.equals(planId) || "running-plan".equals(planId)) {
            response.json(409, Map.of(
                    KEY_ERROR, "plan_running",
                    KEY_MESSAGE, planId + " is running; its completion report follows when it ends",
                    "plan", planId,
                    KEY_STATE, "running"
            ));
            return;
        }
        var report = completionReports.get(planId);
        if (report != null) {
            response.json(200, report.toMap());
            return;
        }
        response.json(404, Map.of(KEY_ERROR, ERR_PLAN_NOT_FOUND, KEY_MESSAGE, "Completion report not found for plan: " + planId));
    }

    /**
     * Serves {@code GET /api/v1/status}: the route's representation with the live listener and
     * device data, and the onboarding fixture of the current state where the scenario has one.
     *
     * @param route    the status route
     * @param map      the route representation
     * @param response the response
     * @throws IOException on write failure
     */
    private void handleStatusRoute(RouteDefinition route, Map<?, ?> map, MockHttpResponse response)
            throws IOException {
        var copy = new LinkedHashMap<String, Object>();
        for (var entry : map.entrySet()) {
            if (FIXTURE_WEB.equals(entry.getKey()) && entry.getValue() instanceof Map<?, ?> webMap) {
                copy.put(FIXTURE_WEB, mergedWeb(webMap, null));
            } else {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        applyStateRuntimeToStatus(copy);
        copy.put("lifecycle_state", lifecycleState.name().toLowerCase());
        copy.put("listeners", listenerMaps());
        synchronized (devicesLock) {
            copy.put("paired_devices_count", pairedDevices.size());
        }
        if (drainingBy != null) {
            copy.put("draining_by", drainingBy);
            copy.put("can_withdraw", canWithdrawStop);
        }
        if (lifecycleState == RuntimeLifecycleState.DRAINING) {
            var facts = stateFacts(currentStateId.get());
            var blockers = facts != null ? JsonTree.asArray(facts.get("blockers")) : null;
            if (blockers != null) {
                copy.put("drain_remaining_count", blockers.size());
            }
        }
        response.json(route.status(), copy);
    }

    private void handleRouteWithDynamicStatus(RouteDefinition route, String path, MockHttpResponse response) throws IOException {
        var rep = route.representation();
        if ("/api/v1/status".equals(path) && rep instanceof Map<?, ?> map) {
            handleStatusRoute(route, map, response);
            return;
        }
        if (PATH_PROJECTS.equals(path) && rep instanceof Map<?, ?> map && map.get(CONST_PROJECTS) instanceof List<?> list) {
            var sorted = new ArrayList<>(list);
            sorted.sort((a, b) -> {
                if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
                    var countA = JsonTree.integer(JsonTree.asObject(ma), "needs_operator_count");
                    boolean needA = (countA != null && countA > 0) || Boolean.TRUE.equals(ma.get("setup_needs_operator"));
                    var countB = JsonTree.integer(JsonTree.asObject(mb), "needs_operator_count");
                    boolean needB = (countB != null && countB > 0) || Boolean.TRUE.equals(mb.get("setup_needs_operator"));
                    if (needA != needB) {
                        return needA ? -1 : 1;
                    }
                }
                return 0;
            });
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            copy.put(CONST_PROJECTS, sorted);
            response.json(route.status(), copy);
            return;
        }
        response.json(route.status(), rep);
    }

    private boolean handleProjectOverviewRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("GET".equals(request.method()) && path.startsWith("/api/v1/projects/")) {
            var route = activeRoute("GET", path);
            if (route != null) {
                if (path.endsWith("/ended")) {
                    handleEndedPageQuery(route, request.query() != null ? request.query() : "page=1", response);
                    return true;
                }
                return false;
            }
            if (path.endsWith("/ended")) {
                var sub = path.substring("/api/v1/projects/".length());
                var prj = sub.substring(0, sub.length() - "/ended".length());
                response.json(200, Map.of(
                        KEY_PROJECT_ID, prj,
                        "page", 1,
                        "total_pages", 1,
                        "has_newer", false,
                        "has_older", false,
                        "items", List.of()
                ));
                return true;
            }
            var prj = path.substring("/api/v1/projects/".length());
            if (!prj.contains("/")) {
                response.json(200, Map.of(
                        KEY_PROJECT_ID, prj,
                        "display_name", prj,
                        "plans", List.of(),
                        "epics", List.of(),
                        "alongside_landings", List.of()
                ));
                return true;
            }
        }
        return false;
    }

    private void handleEndedPageQuery(RouteDefinition route, String query, MockHttpResponse response) throws IOException {
        if (route.status() < 200 || route.status() >= 300) {
            response.json(route.status(), route.representation());
            return;
        }
        int page = 1;
        for (var part : query.split("&")) {
            if (part.startsWith("page=")) {
                try {
                    page = Integer.parseInt(part.substring(5));
                } catch (NumberFormatException _) {
                    page = 1;
                }
            }
        }
        var rep = route.representation();
        if (rep instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            var totalNum = JsonTree.number(JsonTree.asObject(map), "total_pages");
            int total = totalNum != null ? totalNum.intValue() : 1;
            copy.put("page", page);
            copy.put("has_newer", page > 1);
            copy.put("has_older", page < total);
            if (map.get("pages") instanceof Map<?, ?> pagesMap) {
                var pageItems = pagesMap.get(String.valueOf(page));
                if (pageItems instanceof List<?> list) {
                    copy.put("items", list);
                } else if (page == 1 && map.get("items") instanceof List<?> list) {
                    copy.put("items", list);
                } else {
                    copy.put("items", List.of());
                }
            }
            copy.remove("pages");
            response.json(route.status(), copy);
            return;
        }
        response.json(route.status(), rep);
    }

    private void handleMessages(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (!"GET".equalsIgnoreCase(request.method())) {
            response.json(405, Map.of(KEY_ERROR, "method_not_allowed", KEY_MESSAGE, "Only GET is supported for messages"));
            return;
        }

        var path = request.path();
        if ("/api/v1/messages".equals(path)) {
            response.json(200, MessageCatalogue.getAvailableLocalesAndAreas());
            return;
        }

        var sub = path.substring("/api/v1/messages/".length());
        var parts = sub.split("/");
        if (parts.length > 2) {
            response.json(400, Map.of(KEY_ERROR, ERR_BAD_REQUEST, KEY_MESSAGE, "Unexpected path segments"));
            return;
        }
        var locale = parts[0];
        var area = parts.length > 1 ? parts[1] : "all";

        var messages = MessageCatalogue.getMessages(locale, area);
        response.json(200, messages);
    }

    /**
     * The relay-facing MCP endpoint ({@code POST /mcp}). The relay sends the JSON-RPC request body and
     * names the method in the {@code Mcp-Method} header; a plain client may omit the header and carry
     * the method in the body. {@code initialize} and {@code tools/call} answer with one JSON-RPC
     * object, {@code tools/list} with a chunked SSE progress notification followed by the
     * {@code message} frame, and a notification with {@code 202} and no body. A scenario may fix any
     * answer with a static {@code POST /mcp} route.
     *
     * @param request  the request
     * @param response the response
     * @throws IOException on write failure
     */
    private void handleMcp(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (!METHOD_POST.equals(request.method())) {
            response.send(405, "application/json",
                    JsonTree.write(Map.of(KEY_ERROR, "method_not_allowed", KEY_MESSAGE, "POST /mcp only")));
            return;
        }

        var body = request.bodyText();
        Map<String, Object> message;
        try {
            message = JsonTree.asObject(JsonTree.parse(body));
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IOException | RuntimeException _) {
            response.json(200, jsonRpcError(null, -32700, "parse_error"));
            return;
        }
        if (message == null) {
            response.json(200, jsonRpcError(null, -32600, "invalid_request"));
            return;
        }

        var method = request.header("Mcp-Method");
        if (method == null || method.isBlank()) {
            method = JsonTree.string(message, "method");
        }
        var id = message.get("id");
        var params = JsonTree.object(message, "params");

        if (method == null || method.isBlank()) {
            response.json(200, jsonRpcError(id, -32600, "invalid_request"));
            return;
        }

        if (method.startsWith("notifications/")) {
            response.send(202, null, "");
            return;
        }

        var scenarioRoute = currentScenario.get().route(METHOD_POST, request.path());
        if (scenarioRoute != null && !"tools/list".equals(method)) {
            response.json(scenarioRoute.status(), scenarioRoute.representation());
            return;
        }

        switch (method) {
            case "initialize" -> response.json(200, jsonRpcResult(id, Map.of(
                    "protocolVersion", protocolVersionOf(params),
                    "capabilities", Map.of("tools", Map.of()),
                    "serverInfo", Map.of("name", "pm-mock-runtime", FIELD_VERSION, "0.1.0-SNAPSHOT"))));
            case "tools/list" -> {
                try (var events = response.events(200)) {
                    events.send(null, JsonTree.write(Map.of(
                            "jsonrpc", "2.0",
                            "method", "notifications/progress",
                            "params", Map.of("progress", 1, "total", 1))));
                    events.send("message", JsonTree.write(jsonRpcResult(id, Map.of(
                            "tools", List.of(Map.of(
                                    "name", "pm_wait",
                                    "inputSchema", Map.of("type", "object", "properties", Map.of())))))));
                }
            }
            case "tools/call" -> response.json(200, jsonRpcResult(id, Map.of("content", List.of())));
            default -> response.json(200, jsonRpcResult(id, Map.of()));
        }
    }

    private static Map<String, Object> jsonRpcResult(Object id, Object result) {
        var body = new LinkedHashMap<String, Object>();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("result", result);
        return body;
    }

    private static Map<String, Object> jsonRpcError(Object id, int code, String message) {
        var body = new LinkedHashMap<String, Object>();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("error", Map.of("code", code, "message", message));
        return body;
    }

    private static String protocolVersionOf(Map<String, Object> params) {
        var version = params == null ? null : JsonTree.string(params, "protocolVersion");
        return version == null || version.isBlank() ? "2025-11-25" : version;
    }

    /**
     * Serves one scoped Server-Sent Events stream. The stream is registered in the response's
     * pre-head callback, so a broadcast that follows the request cannot be missed. A
     * {@code Last-Event-ID} that the scope's ring buffer still holds replays every later event in
     * order; a stale, evicted, or unknown id receives one {@code reset} envelope instead, as does a
     * client that reconnects after {@code /mock/reset} or a scenario switch.
     *
     * @param request  the request carrying the optional {@code Last-Event-ID}
     * @param response the response to write
     * @param scope    the subscription scope key
     * @throws IOException on write failure
     */
    private void handleEventStream(MockHttpRequest request, MockHttpResponse response, String scope) throws IOException {
        var lastEventId = parseLastEventId(request.header("Last-Event-ID"));
        // Register before the head is written, so a broadcast that the client issues right after it
        // reads the head is queued for this stream instead of being missed. The snapshot is taken in
        // the same lock section as the registration: a broadcast either lands in the buffer and the
        // replay, or is queued live after it, never both.
        var stream = response.events(200, created -> {
            created.scope(scope);
            synchronized (eventLock) {
                replayPlans.put(created, replayPlanFor(scope, lastEventId));
                activeStreams.put(created, scope);
            }
        });

        synchronized (eventLock) {
            var plan = replayPlans.remove(stream);
            if (plan != null) {
                if (plan.reset()) {
                    var resetId = nextBufferedReset(scope);
                    tryWrite(new FramedEvent(stream, resetId, EventName.RESET.wire(),
                            envelopeJson(EventName.RESET.wire(), resetId, resetScopeOf(scope),
                                    new LinkedHashMap<>())));
                } else {
                    for (var item : plan.events()) {
                        tryWrite(new FramedEvent(stream, item.id(), item.event(), item.json()));
                    }
                }
            }
            readyStreams.add(stream);
            var queued = pendingFrames.remove(stream);
            if (queued != null) {
                queued.forEach(this::tryWrite);
            }
        }

        try {
            stream.sendComment("connected to mock runtime");
            if (scope.startsWith(STREAM_ACTIVITY_PREFIX)) {
                sendActivitySeed(stream, scope);
            }
        } catch (IOException _) {
            dropStream(stream);
            return;
        }

        // Keep the stream alive until client disconnects or closed
        while (!closed && !stream.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        dropStream(stream);
    }

    /**
     * Computes the replay a stream owes. Must be called under {@link #eventLock}, in the same section
     * that registers the stream.
     */
    private ReplayPlan replayPlanFor(String scope, Long lastEventId) {
        if (lastEventId == null) {
            return new ReplayPlan(List.of(), false);
        }
        var buffer = eventBuffers.get(scope);
        var found = buffer != null && buffer.stream().anyMatch(item -> item.id() == lastEventId);
        if (!found) {
            return new ReplayPlan(List.of(), true);
        }
        var replay = new ArrayList<BufferedEvent>();
        for (var item : buffer) {
            if (item.id() > lastEventId) {
                replay.add(item);
            }
        }
        return new ReplayPlan(List.copyOf(replay), false);
    }

    private static Long parseLastEventId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException _) {
            return -1L;
        }
    }

    private long nextBufferedReset(String scope) {
        synchronized (eventLock) {
            var id = nextStreamId(scope);
            var buffer = eventBuffers.computeIfAbsent(scope, key -> new ArrayDeque<>());
            var envelope = envelopeJson(EventName.RESET.wire(), id, resetScopeOf(scope), new LinkedHashMap<>());
            buffer.addLast(new BufferedEvent(id, EventName.RESET.wire(), envelope));
            while (buffer.size() > EVENT_BUFFER_CAPACITY) {
                buffer.removeFirst();
            }
            return id;
        }
    }

    private static EventScope resetScopeOf(String scope) {
        if (scope.startsWith(STREAM_PLAN_PREFIX)) {
            return new EventScope(EventScopeKind.PLAN, scope.substring(STREAM_PLAN_PREFIX.length()), null);
        }
        if (scope.startsWith(STREAM_EPIC_PREFIX)) {
            return new EventScope(EventScopeKind.EPIC, scope.substring(STREAM_EPIC_PREFIX.length()), null);
        }
        if (scope.startsWith(STREAM_ENTRY_PREFIX)) {
            return new EventScope(EventScopeKind.ENTRY, scope.substring(STREAM_ENTRY_PREFIX.length()), null);
        }
        if (scope.startsWith(STREAM_ACTIVITY_PREFIX)) {
            var rest = scope.substring(STREAM_ACTIVITY_PREFIX.length());
            var separator = rest.indexOf(':');
            var planId = separator > 0 ? rest.substring(0, separator) : rest;
            return new EventScope(EventScopeKind.PLAN, planId, null);
        }
        if (scope.startsWith(STREAM_WORKSPACE_PREFIX)) {
            return new EventScope(EventScopeKind.WORKSPACE, null,
                    scope.substring(STREAM_WORKSPACE_PREFIX.length()));
        }
        if (STREAM_REPORTS.equals(scope)) {
            return new EventScope(EventScopeKind.MACHINE, null, null);
        }
        return new EventScope(EventScopeKind.RUNTIME, null, null);
    }

    private void sendActivitySeed(MockEventStream stream, String scope) {
        var rest = scope.substring(STREAM_ACTIVITY_PREFIX.length());
        var separator = rest.indexOf(':');
        var planId = separator > 0 ? rest.substring(0, separator) : rest;
        var taskId = separator > 0 ? rest.substring(separator + 1) : null;
        if (taskId != null && hasEmptyActivityRoute(planId, taskId)) {
            return;
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("kind", "assistant_text");
        payload.put("text", "Analyzing module dependencies...");
        payload.put("tokens", 450);
        payload.put("timestamp", Instant.now().toString());
        if (taskId != null) {
            payload.put("task_id", taskId);
        }
        long id;
        String envelope;
        synchronized (eventLock) {
            id = nextStreamId(scope);
            envelope = envelopeJson("task.activity", id,
                    new EventScope(EventScopeKind.PLAN, planId, null), payload);
        }
        try {
            stream.sendJson("task.activity", id, envelope);
        } catch (IOException _) {
            dropStream(stream);
        }
    }

    /**
     * @param planId the plan named by the activity scope
     * @param taskId the task named by the activity scope
     * @return {@code true} when the scenario declares an explicit activity route with no entries
     */
    boolean hasEmptyActivityRoute(String planId, String taskId) {
        var route = activeRoute("GET", "/api/v1/plans/" + planId + "/tasks/" + taskId + "/activity");
        return route != null
                && route.representation() instanceof Map<?, ?> representation
                && representation.get("entries") instanceof List<?> entries
                && entries.isEmpty();
    }

    /**
     * Emits one {@code reset} to every open stream and clears every ring buffer. Called by
     * {@code /mock/reset} and by a scenario switch: the envelopes of the previous scenario must not
     * be replayed into the new one.
     */
    private void broadcastReset() {
        var framed = new ArrayList<FramedEvent>();
        synchronized (eventLock) {
            eventBuffers.clear();
            // Sequences stay monotonic across a reset: a client that reconnects with an old id must
            // not find that id in the new buffer and mistake the new scenario's events for a replay.
            var resetEvents = new HashMap<String, BufferedEvent>();
            for (var entry : activeStreams.entrySet()) {
                var scope = entry.getValue();
                var reset = resetEvents.computeIfAbsent(scope, key -> {
                    var id = nextStreamId(key);
                    var envelope = envelopeJson(EventName.RESET.wire(), id, resetScopeOf(key),
                            new LinkedHashMap<>());
                    var buffered = new BufferedEvent(id, EventName.RESET.wire(), envelope);
                    eventBuffers.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(buffered);
                    return buffered;
                });
                framed.add(new FramedEvent(entry.getKey(), reset.id(), reset.event(), reset.json()));
            }
        }
        framed.forEach(this::writeOrDrop);
    }

    /**
     * Derives the subscription scope of an SSE path.
     *
     * @param path the request path
     * @return the scope key, or {@code null} when the path is not an event stream
     */
    private static String streamScopeOf(String path) {
        if ("/api/v1/events".equals(path) || "/api/v1/machine/events".equals(path)) {
            return STREAM_MACHINE;
        }
        if ("/api/v1/reports/events".equals(path)) {
            return STREAM_REPORTS;
        }
        var activity = ACTIVITY_EVENTS_PATH.matcher(path);
        if (activity.matches()) {
            return STREAM_ACTIVITY_PREFIX + activity.group(1) + ":" + activity.group(2);
        }
        var plan = PLAN_EVENTS_PATH.matcher(path);
        if (plan.matches()) {
            return STREAM_PLAN_PREFIX + plan.group(1);
        }
        var epic = EPIC_EVENTS_PATH.matcher(path);
        if (epic.matches()) {
            return STREAM_EPIC_PREFIX + epic.group(1);
        }
        var entry = ENTRY_EVENTS_PATH.matcher(path);
        if (entry.matches()) {
            return STREAM_ENTRY_PREFIX + entry.group(1);
        }
        var workspace = WORKSPACE_EVENTS_PATH.matcher(path);
        if (workspace.matches()) {
            return STREAM_WORKSPACE_PREFIX + workspace.group(1);
        }
        return path.endsWith(SEGMENT_EVENTS) ? STREAM_MACHINE : null;
    }

    private static final Pattern ACTIVITY_EVENTS_PATH =
            Pattern.compile("^/api/v1/plans/([^/]+)/tasks/([^/]+)/activity/events$");
    private static final Pattern PLAN_EVENTS_PATH =
            Pattern.compile("^/api/v1/plans/([^/]+)/events$");
    private static final Pattern EPIC_EVENTS_PATH =
            Pattern.compile("^/api/v1/epics/([^/]+)/events$");
    private static final Pattern ENTRY_EVENTS_PATH =
            Pattern.compile("^/api/v1/entries/([^/]+)/events$");
    private static final Pattern WORKSPACE_EVENTS_PATH =
            Pattern.compile("^/api/v1/projects/([^/]+)/workspace/events$");

    private void handleControlRequest(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("/mock/state".equals(path) && "GET".equals(request.method())) {
            handleControlState(response);
            return;
        }
        if ("/mock/reset".equals(path) && METHOD_POST.equals(request.method())) {
            handleControlReset(response);
            return;
        }
        if ("/mock/scenario".equals(path) && METHOD_POST.equals(request.method())) {
            handleControlScenario(request, response);
            return;
        }
        if (path.startsWith("/mock/goto/") && METHOD_POST.equals(request.method())) {
            handleControlGoto(path.substring("/mock/goto/".length()), response);
            return;
        }
        if ("/mock/broadcast".equals(path) && METHOD_POST.equals(request.method())) {
            handleControlBroadcast(request, response);
            return;
        }
        if ("/mock/acts".equals(path) && "GET".equals(request.method())) {
            handleControlActs(response);
            return;
        }
        // The development loop has no terminal client to ask for a replacement code. This route
        // returns the open code or issues a fresh one; it never makes a consumed code valid again.
        if ("/mock/pairing".equals(path) && METHOD_POST.equals(request.method())) {
            handleCreatePairing(response);
            return;
        }
        response.json(404, Map.of(KEY_ERROR, "unknown_control_endpoint"));
    }

    private void handleControlState(MockHttpResponse response) throws IOException {
        StateDefinition state;
        String stateId;
        String scenarioName;
        String pairingCode;
        synchronized (this) {
            stateId = this.currentStateId.get();
            scenarioName = this.currentScenario.get().name();
            // The live code, never the scenario's static one: after a redemption the scenario
            // code is consumed, and a freshly issued code replaces it.
            var code = this.currentPairingCode;
            pairingCode = code != null ? code : "";
            state = currentScenario.get().state(stateId);
        }
        var offered = state != null ? new ArrayList<>(state.links().keySet()) : List.<String>of();
        response.json(200, Map.of(
                KEY_SCENARIO, scenarioName,
                KEY_STATE, stateId,
                "pairing_code", pairingCode,
                "offered_links", offered
        ));
    }

    private void handleControlReset(MockHttpResponse response) throws IOException {
        reset();
        response.json(200, Map.of(
                KEY_STATUS, "reset",
                KEY_SCENARIO, currentScenario.get().name(),
                KEY_STATE, currentStateId.get()
        ));
    }

    private void handleControlScenario(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String scenarioName = extractScenarioName(request);
        if (scenarioName == null || scenarioName.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, "missing_scenario_name"));
            return;
        }
        try {
            switchScenario(scenarioName);
            response.json(200, Map.of(
                    KEY_STATUS, "switched",
                    KEY_SCENARIO, currentScenario.get().name(),
                    KEY_STATE, currentStateId.get()
            ));
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (RuntimeException e) {
            response.json(404, Map.of(KEY_ERROR, "scenario_not_found", KEY_MESSAGE, e.getMessage()));
        }
    }

    private String extractScenarioName(MockHttpRequest request) {
        String scenarioName = null;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    scenarioName = JsonTree.string(parsed, KEY_SCENARIO);
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore parse errors when extracting scenario name from body
            }
        }
        if (scenarioName == null && request.query() != null) {
            for (var part : request.query().split("&")) {
                if (part.startsWith("name=") || part.startsWith("scenario=")) {
                    scenarioName = part.substring(part.indexOf('=') + 1);
                    break;
                }
            }
        }
        return scenarioName;
    }

    private void handleControlGoto(String targetState, MockHttpResponse response) throws IOException {
        try {
            goToState(targetState);
            response.json(200, Map.of(
                    KEY_STATUS, "transitioned",
                    KEY_SCENARIO, currentScenario.get().name(),
                    KEY_STATE, currentStateId.get()
            ));
        } catch (IllegalArgumentException e) {
            response.json(404, Map.of(KEY_ERROR, "state_not_found", KEY_MESSAGE, e.getMessage()));
        } catch (IllegalStateException e) {
            response.json(500, Map.of(KEY_ERROR, "state_fixture_invalid", KEY_MESSAGE, e.getMessage()));
        }
    }

    private void handleControlBroadcast(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String event = KEY_MESSAGE;
        Object data = Map.of();
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    event = Objects.requireNonNullElse(JsonTree.string(parsed, KEY_EVENT), KEY_MESSAGE);
                    data = Objects.requireNonNullElse(parsed.get("data"), Map.of());
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore parse errors from broadcast body
            }
        }
        broadcastEvent(event, data);
        response.json(200, Map.of(KEY_STATUS, "broadcasted", KEY_EVENT, event));
    }

    /**
     * Records one operator act: an authenticated request that is not a read. A dialogue sends one
     * when it sends what the operator chose. A request is recorded whether the runtime accepts or
     * refuses it, so the log proves what was sent, not what took effect. The log is what
     * {@code GET /mock/acts} serves.
     *
     * @param request the authenticated request
     */
    private void recordOperatorAct(MockHttpRequest request) {
        var method = request.method().toUpperCase(Locale.ROOT);
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }
        if (request.path().startsWith("/mcp")) {
            // A JSON-RPC call of the relay's MCP endpoint is no operator act.
            return;
        }
        var act = new LinkedHashMap<String, Object>();
        act.put(KEY_METHOD, method);
        act.put("path", request.path());
        act.put(KEY_STATE, currentStateId.get());
        operatorActs.add(act);
    }

    private void handleControlActs(MockHttpResponse response) throws IOException {
        var acts = List.copyOf(operatorActs);
        var body = new LinkedHashMap<String, Object>();
        body.put(KEY_COUNT, acts.size());
        body.put("acts", acts);
        response.json(200, body);
    }

    private void initDefaultOnboarding() {
        synchronized (onboardingLock) {
            machineSettingsRevision = 4;
            machineDocumentsMode = DocumentsMode.ASK.wireValue();
            onboardingAuto = false;
            machinePlanDefaults = new LinkedHashMap<>();
            machinePlanDefaults.put(KEY_LANDING, PlanDefaults.LANDING_PULL_REQUEST);
            machinePlanDefaults.put(KEY_METHOD, PlanDefaults.METHOD_TEST_FIRST);
            machinePlanDefaults.put(KEY_ASKING, PlanDefaults.ASK_KEY_POINTS);
            machineCleanupDurations = new LinkedHashMap<>();
            machineCleanupDurations.put(KEY_PLANS_AFTER_DAYS, 30);
            machineCleanupDurations.put(KEY_EPICS_AFTER_DAYS, 30);
            machineCleanupDurations.put(KEY_LOGS_AFTER_DAYS, 30);
            machineCleanupDurations.put(KEY_REPORTS_AFTER_DAYS, 30);
            machineCleanupDurations.put(KEY_UNENROLLED_AFTER_DAYS, 30);
            machineCleanupDurations.put(KEY_REMIND_AFTER_DAYS, 3);
            machineRememberedConsents.clear();
            machineRememberedConsents.add(rememberedConsent(ConsentKind.SCAN_HARNESSES.wireValue(),
                    KEY_TERMINAL, "2026-10-12T09:00:00Z"));
            machineRememberedConsents.add(rememberedConsent(ConsentKind.PROBE_HARNESS_LOGIN.wireValue(),
                    "Firefox", "2026-10-12T09:12:00Z"));
            machineCredentials.clear();
            machineCredentials.add(credential("github", "GitHub", "fine-grained", 9, false));
            machineCredentials.add(credential("sonar", "SonarCloud", "user token", null, false));
        }
    }

    private static Map<String, Object> rememberedConsent(String kind, String channel, String grantedAt) {
        var map = new LinkedHashMap<String, Object>();
        map.put("kind", kind);
        map.put(KEY_STATUS, "remembered");
        map.put("remember", true);
        map.put("granted_at", grantedAt);
        map.put("channel", channel);
        return map;
    }

    private static Map<String, Object> credential(String key, String provider, String kind,
            @Nullable Integer expiresInDays, boolean expired) {
        var map = new LinkedHashMap<String, Object>();
        map.put("key", key);
        map.put("provider", provider);
        map.put("kind", kind);
        if (expiresInDays != null) {
            map.put("expires_in_days", expiresInDays);
        }
        map.put("expired", expired);
        return map;
    }

    private boolean handleOnboardingRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        var method = request.method();
        if ("/api/v1/onboarding/state".equals(path) && "GET".equals(method)) {
            handleOnboardingState(response);
            return true;
        }
        if ("/api/v1/onboarding/step".equals(path) && METHOD_POST.equals(method)) {
            handleOnboardingStep(request, response);
            return true;
        }
        if ("/api/v1/onboarding/auto".equals(path) && METHOD_POST.equals(method)) {
            handleOnboardingAuto(response);
            return true;
        }
        return false;
    }

    private void handleOnboardingState(MockHttpResponse response) throws IOException {
        StateDefinition state;
        String stateId;
        synchronized (this) {
            stateId = currentStateId.get();
            state = currentScenario.get().state(stateId);
        }
        response.json(200, mergeRepresentation(state, stateId));
    }

    /**
     * Serves the current onboarding state. The state's representation is the operator state the
     * scenario file carries; this endpoint keeps the flat member names the onboarding clients read
     * (the summary, the domain facts, the state id, the link map) and preserves the whole operator
     * envelope under {@code operator}.
     *
     * @param state   the state definition, may be {@code null}
     * @param stateId the state id
     * @return the response body
     */
    private Map<String, Object> mergeRepresentation(@Nullable StateDefinition state, @Nullable String stateId) {
        var body = new LinkedHashMap<String, Object>();
        if (state != null && state.representation() instanceof Map<?, ?> rep) {
            copyOperatorFacts(rep, body);
            copyOperatorScope(rep, body);
            body.put("operator", rep);
        }
        var links = onboardingLinkMap(state);
        if (!links.isEmpty()) {
            body.put(KEY_LINKS, links);
        }
        body.put(KEY_STATE, stateId);
        body.put("completed_steps", completedStepsFor(stateId));
        body.put("auto", onboardingAuto);
        body.put("step_summaries", stepSummariesFor(stateId));
        return body;
    }

    /**
     * Copies the operator state's facts to the flat members the onboarding clients read.
     *
     * @param representation the operator state representation
     * @param body           the response body to complete
     */
    private static void copyOperatorFacts(Map<?, ?> representation, Map<String, Object> body) {
        var facts = JsonTree.asObject(representation.get(OperatorStateResource.FIELD_FACTS));
        if (facts == null) {
            return;
        }
        for (var entry : facts.entrySet()) {
            if (!FIXTURE_RUNTIME.equals(entry.getKey()) && !FIXTURE_SUMMARY.equals(entry.getKey())) {
                body.put(entry.getKey(), entry.getValue());
            }
        }
        var summary = JsonTree.string(facts, FIXTURE_SUMMARY);
        if (summary != null) {
            body.put(FIXTURE_SUMMARY, summary);
        }
    }

    /**
     * Copies the operator scope's kind to the flat {@code scope} member.
     *
     * @param representation the operator state representation
     * @param body           the response body to complete
     */
    private static void copyOperatorScope(Map<?, ?> representation, Map<String, Object> body) {
        var scope = JsonTree.asObject(representation.get(OperatorStateResource.FIELD_SCOPE));
        if (scope == null) {
            return;
        }
        var kind = JsonTree.string(scope, FIELD_SCOPE_KIND);
        if (kind != null) {
            body.put(OperatorStateResource.FIELD_SCOPE, kind);
        }
    }

    /**
     * @param state the state definition, may be {@code null}
     * @return the state's scenario links keyed by operator link id, in order
     */
    private static Map<String, Object> onboardingLinkMap(@Nullable StateDefinition state) {
        var links = new LinkedHashMap<String, Object>();
        if (state == null) {
            return links;
        }
        for (var entry : state.links().entrySet()) {
            var link = entry.getValue();
            var target = new LinkedHashMap<String, Object>();
            target.put(KEY_HREF, link.href() != null ? link.href() : "/api/v1/onboarding/step");
            target.put(KEY_METHOD, link.method());
            target.put("next", link.next());
            links.put(entry.getKey(), target);
        }
        return links;
    }

    /**
     * The summary the runtime reports for each done step: the recorded value of an aspect the
     * onboarding has already passed, keyed by the step number (1 to 7).
     *
     * @param stateId the current state
     * @return the summaries of the done steps
     */
    private Map<String, String> stepSummariesFor(@Nullable String stateId) {
        var summaries = new LinkedHashMap<String, String>();
        if (stateId == null) {
            return summaries;
        }
        var current = MachineOnboardingState.aspectOf(stateId);
        var upTo = current != null ? current.step() - 1 : 0;
        if (STATE_ONBOARD_COMPLETED.equals(stateId) || STATE_ONBOARD_SUMMARY.equals(stateId)
                || stateId.startsWith(STATE_SETTINGS_PREFIX)) {
            upTo = 7;
        }
        if (upTo >= 1) {
            summaries.put("1", "Deutsch, models English");
        }
        if (upTo >= 2) {
            summaries.put("2", "Claude Code 2.4.1 (default), OpenCode 1.9.0");
        }
        if (upTo >= 3) {
            summaries.put("3", "4 models on 2 harnesses");
        }
        if (upTo >= 4) {
            summaries.put("4", "31 roles assigned");
        }
        if (upTo >= 5) {
            summaries.put("5", "macOS Keychain");
        }
        if (upTo >= 6) {
            summaries.put("6", "GitHub (gh)");
        }
        if (upTo >= 7) {
            summaries.put("7", "Pull Request \u00b7 Test first \u00b7 key points");
        }
        return summaries;
    }

    private static int completedStepsFor(@Nullable String stateId) {
        if (stateId == null) {
            return 0;
        }
        if (STATE_ONBOARD_COMPLETED.equals(stateId) || STATE_ONBOARD_SUMMARY.equals(stateId)
                || stateId.startsWith(STATE_SETTINGS_PREFIX)) {
            return 7;
        }
        var aspect = MachineOnboardingState.aspectOf(stateId);
        return aspect != null ? aspect.step() - 1 : 0;
    }

    private void handleOnboardingStep(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var body = readBodyMap(request);
        var linkName = JsonTree.string(body, "link");
        StateDefinition state;
        String previousState;
        synchronized (this) {
            previousState = currentStateId.get();
            state = currentScenario.get().state(previousState);
        }
        if (state == null || state.links().isEmpty()) {
            response.json(409, Map.of(KEY_ERROR, "no_step", KEY_STATE,
                    previousState != null ? previousState : ""));
            return;
        }
        var chosen = chooseLink(state, linkName);
        var link = chosen.getValue();
        if (link.status() >= 400) {
            response.json(link.status(), Map.of(KEY_ERROR,
                    link.error() != null ? link.error() : "rejected", KEY_STATE, previousState));
            return;
        }
        String newState;
        synchronized (this) {
            transitionTo(link.next());
            newState = currentStateId.get();
            state = currentScenario.get().state(newState);
        }
        var previousAspect = MachineOnboardingState.aspectOf(previousState);
        var newAspect = MachineOnboardingState.aspectOf(newState);
        if (previousAspect != null && newAspect != previousAspect) {
            broadcastEvent(OnboardingStepCompletedEvent.EVENT_NAME, new OnboardingStepCompletedEvent(
                    previousAspect, newState, completedStepsFor(newState)).toMap());
        }
        response.json(link.status(), mergeRepresentation(state, newState));
    }

    private static Map.Entry<String, LinkDefinition> chooseLink(StateDefinition state, @Nullable String linkName) {
        if (linkName != null) {
            for (var entry : state.links().entrySet()) {
                if (entry.getKey().equals(linkName)) {
                    return entry;
                }
            }
        }
        return state.links().entrySet().iterator().next();
    }

    private void handleOnboardingAuto(MockHttpResponse response) throws IOException {
        onboardingAuto = true;
        synchronized (this) {
            if (currentScenario.get().states().containsKey("auto.consent")) {
                transitionTo("auto.consent");
            }
        }
        handleOnboardingState(response);
    }

    private boolean handleMachineSettingsRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (!"/api/v1/machine/settings".equals(request.path())) {
            return false;
        }
        if ("GET".equals(request.method())) {
            response.json(200, buildMachineSettings());
            return true;
        }
        if ("PUT".equals(request.method()) || METHOD_POST.equals(request.method())) {
            handleUpdateMachineSettings(request, response);
            return true;
        }
        return false;
    }

    private Map<String, Object> buildMachineSettings() {
        synchronized (onboardingLock) {
            var body = new LinkedHashMap<String, Object>();
            body.put("revision", machineSettingsRevision);
            body.put("aspects", List.of(
                    aspectSummary(OnboardingAspect.LANGUAGE, "Deutsch, models English", "de"),
                    aspectSummary(OnboardingAspect.HARNESSES, "Claude Code 2.4.1 (default), OpenCode 1.9.0", CONST_CLAUDE),
                    aspectSummary(OnboardingAspect.MODELS, "4 models on 2 harnesses", "4"),
                    aspectSummary(OnboardingAspect.ROLES, "31 roles", "31"),
                    aspectSummary(OnboardingAspect.CREDENTIALS, "macOS Keychain", "os"),
                    aspectSummary(OnboardingAspect.PROVIDERS, "GitHub (gh); SonarCloud not available", "github"),
                    aspectSummary(OnboardingAspect.PLANS, "Pull Request \u00b7 Test first \u00b7 key points",
                            String.valueOf(machinePlanDefaults.get(KEY_LANDING)))));
            body.put("language_interface", "de");
            body.put("language_model", "en");
            body.put(KEY_DOCUMENTS_MODE, machineDocumentsMode);
            body.put(KEY_CLEANUP_DURATIONS, machineCleanupDurations);
            body.put(KEY_PLAN_DEFAULTS, machinePlanDefaults);
            body.put("consents", machineRememberedConsents);
            body.put("credentials", machineCredentials);
            return body;
        }
    }

    private static Map<String, Object> aspectSummary(OnboardingAspect aspect, String summary, String current) {
        var map = new LinkedHashMap<String, Object>();
        map.put("aspect", aspect.wireValue());
        map.put("step", aspect.step());
        map.put(FIXTURE_SUMMARY, summary);
        map.put("current", current);
        return map;
    }

    private void handleUpdateMachineSettings(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var update = MachineSettingsUpdateRequest.fromMap(readBodyMap(request));
        synchronized (onboardingLock) {
            if (update.revision() != machineSettingsRevision) {
                response.json(409, MachineSettingsUpdateResult.conflict(
                        machineSettingsRevision, currentMachineValue(update.key())).toMap());
                return;
            }
            applyMachineSetting(update.key(), update.value());
            machineSettingsRevision++;
            broadcastEvent(MachineSettingsChangedEvent.EVENT_NAME, new MachineSettingsChangedEvent(
                    update.key(), update.value(), machineSettingsRevision, "web").toMap());
            response.json(200, MachineSettingsUpdateResult.applied(machineSettingsRevision).toMap());
        }
    }

    private void applyMachineSetting(String key, @Nullable String value) {
        if (KEY_DOCUMENTS_MODE.equals(key) && value != null) {
            applyDocumentsMode(value);
        } else if (KEY_CLEANUP_DURATIONS.equals(key) && value != null) {
            applyCleanupDurations(value);
        } else if (KEY_PLAN_DEFAULTS.equals(key) && value != null) {
            applyPlanDefaults(value);
        } else if (key.startsWith(CONSENT_PREFIX)) {
            applyConsentWithdrawal(key);
        } else if (key.startsWith(CREDENTIAL_PREFIX)) {
            applyCredentialRotation(key, value);
        }
    }

    private void applyCredentialRotation(String key, @Nullable String value) {
        var credentialKey = key.substring(CREDENTIAL_PREFIX.length());
        for (var credential : machineCredentials) {
            if (credentialKey.equals(credential.get("key"))) {
                if (value != null && !value.isBlank()) {
                    credential.put("kind", value);
                }
                credential.remove("expires_in_days");
                credential.put("expired", false);
                credential.put("rotated", true);
                return;
            }
        }
    }

    private void applyDocumentsMode(String value) {
        if (DocumentsMode.COPY.wireValue().equals(value) || DocumentsMode.MOVE.wireValue().equals(value)
                || DocumentsMode.ASK.wireValue().equals(value)) {
            machineDocumentsMode = value;
        }
    }

    private void applyCleanupDurations(String value) {
        try {
            int days = Integer.parseInt(value.trim());
            for (var field : CLEANUP_DAY_KEYS) {
                machineCleanupDurations.put(field, days);
            }
        } catch (NumberFormatException _) {
            // keep the current durations when the value is not a number
        }
    }

    private void applyPlanDefaults(String value) {
        if (value.startsWith("landing.")) {
            machinePlanDefaults.put(KEY_LANDING, value);
        } else if (value.startsWith("method.")) {
            machinePlanDefaults.put(KEY_METHOD, value);
        } else if (value.startsWith("ask.")) {
            machinePlanDefaults.put(KEY_ASKING, value);
        } else if (value.startsWith("posture.")) {
            machinePlanDefaults.put(KEY_POSTURE, value.substring("posture.".length()));
        }
    }

    private void applyConsentWithdrawal(String key) {
        var kind = ConsentKind.fromWire(key.substring(CONSENT_PREFIX.length()));
        if (kind != null) {
            machineRememberedConsents.removeIf(c -> kind.wireValue().equals(c.get("kind")));
        }
    }

    private String currentMachineValue(String key) {
        if (KEY_DOCUMENTS_MODE.equals(key)) {
            return machineDocumentsMode;
        }
        if (KEY_PLAN_DEFAULTS.equals(key)) {
            return String.valueOf(machinePlanDefaults.get(KEY_LANDING));
        }
        if (KEY_CLEANUP_DURATIONS.equals(key)) {
            return String.valueOf(machineCleanupDurations.get(KEY_PLANS_AFTER_DAYS));
        }
        if (key.startsWith(CREDENTIAL_PREFIX)) {
            return currentCredentialKind(key.substring(CREDENTIAL_PREFIX.length()));
        }
        return "";
    }

    private String currentCredentialKind(String credentialKey) {
        for (var credential : machineCredentials) {
            if (credentialKey.equals(credential.get("key"))) {
                return String.valueOf(credential.get("kind"));
            }
        }
        return "";
    }

    /** @return the current scenario */
    public Scenario currentScenario() {
        return currentScenario.get();
    }

    /** @return the current state id */
    public String currentStateId() {
        return currentStateId.get();
    }

    /**
     * @return the pairing code the web listener accepts right now, or {@code null} after it was
     *         consumed and until a fresh one is issued
     */
    public String pairingCode() {
        return currentPairingCode;
    }

    /**
     * Sets the live loopback web TCP port bound by the server.
     *
     * @param webPort the bound port
     */
    public void setWebPort(int webPort) {
        this.webPort = webPort;
    }

    /** @return the accepted runtime token */
    public String runtimeToken() {
        return runtimeToken;
    }

    /** @return the accepted device secret */
    public String deviceSecret() {
        return deviceSecret;
    }

    /** @return active paired devices */
    public List<PairedDevice> pairedDevices() {
        synchronized (devicesLock) {
            return List.copyOf(pairedDevices);
        }
    }

    /** @return whether LAN listener is active */
    public boolean lanListenerOn() {
        return lanListenerOn;
    }

    private void initDefaultSystem() {
        synchronized (systemLock) {
            lifecycleState = RuntimeLifecycleState.RUNNING;
            drainingBy = null;
            canWithdrawStop = false;

            keptItems.clear();
            keptItems.add(new CleanupKeptItem("token-usage", "plan", "token-usage", "left out of a delete", "13 Oct", "the console"));
            keptItems.add(new CleanupKeptItem("relay-auth", "epic", "relay-auth", "ignored", "2 Oct", "the web app (iPad)"));
            keptItems.add(new CleanupKeptItem("reports-pm-mcp-clients", "read_reports", "3 read reports", "ignored", "2 Oct", "the web app (iPad)"));
            keptItems.add(new CleanupKeptItem("old-sandbox", "unenrolled_project", "old-sandbox", "ignored", "2 Oct", "the web app (iPad)"));

            cleanupDurations = new CleanupDurations(30, 30, 30, 30, 30, 3);
            cleanupResidues.clear();
            cleanupResidues.add(new CleanupResidue("temporary_files", 214, 356515840L, SWEEP_TODAY));
            cleanupResidues.add(new CleanupResidue("worker_directories", 18, 1288490188L, SWEEP_TODAY));
            cleanupResidues.add(new CleanupResidue("stale_locks", 3, 0L, SWEEP_TODAY));
            cleanupResidues.add(new CleanupResidue("old_workspace_rows", 612, 0L, SWEEP_TODAY));
            cleanupResidues.add(new CleanupResidue("retired_lessons", 4, 0L, SWEEP_TODAY));

            var clientsEntries = List.of(
                    new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged (#214)", "ended 12 Sep", 88080384L, 6, 0, null),
                    new CleanupItemEntry("docs-typo", "plan", "docs-typo", "no change", "ended 11 Sep", 2097152L, 1, 0, null),
                    new CleanupItemEntry("relay-hardening", "epic", "relay-hardening", "completed", "ended 10 Sep", 12582912L, 0, 0, null),
                    new CleanupItemEntry("logs-relay-docs", "logs", "logs of relay-docs", null, null, 32505856L, 14, 0, "plan kept until 24 Oct"),
                    new CleanupItemEntry("reports-clients", "read_reports", "3 read reports", null, null, 0L, 0, 3, "read before 13 Sep")
            );
            var coreEntries = List.of(
                    new CleanupItemEntry("token-usage", "plan", "token-usage", "merged (#88)", "ended 9 Sep", 125829120L, 11, 0, null),
                    new CleanupItemEntry("reports-core", "read_reports", "2 read reports", null, null, 0L, 0, 2, "read before 10 Sep")
            );
            var unenrolledEntries = List.of(
                    new CleanupItemEntry(CONST_LEGACY_PORTAL, "unenrolled_project", CONST_LEGACY_PORTAL, "unenrolled 2 Sep · 7 plans, 1 epic", null, 1181116000L, 0, 0, null)
            );

            var projectItems = List.of(
                    new CleanupProjectItem(CONST_PM_MCP_CLIENTS, 2, 1, 21, 3, 135266304L, clientsEntries),
                    new CleanupProjectItem("pm-mcp-core", 10, 1, 19, 2, 681574400L, coreEntries),
                    new CleanupProjectItem(CONST_UNENROLLED, 0, 0, 0, 0, 1181116000L, unenrolledEntries)
            );

            var summary = new CleanupSummary(
                    12, 2, 40, 5, 1, 1997956704L, 4, SWEEP_TODAY, SWEEP_TOMORROW
            );

            var links = Map.of(
                    "delete", "/api/v1/system/cleanup/cln-0012/delete",
                    "remind", "/api/v1/system/cleanup/cln-0012/remind",
                    "ignore", "/api/v1/system/cleanup/cln-0012/ignore"
            );

            activeCleanupJob = new CleanupJob(
                    "cln-0012",
                    CleanupJobState.OPEN,
                    SWEEP_TODAY,
                    summary,
                    projectItems,
                    links
            );

            applySystemStateFacts();
        }
    }

    /**
     * Applies the seeding state's system facts: the lifecycle the state stands for, the runtime
     * snapshot it shows, and the cleanup job and sweep state it describes (UC-S-01). A state that
     * declares none of them keeps the engine's default world.
     */
    private void applySystemStateFacts() {
        systemSnapshotFixture = null;
        var facts = stateFacts(fixtureStateId.get());
        if (facts == null) {
            return;
        }
        lifecycleState = RuntimeLifecycleState.fromValue(JsonTree.string(facts, "lifecycle_state"));
        canWithdrawStop = Boolean.TRUE.equals(JsonTree.bool(facts, "can_withdraw_stop"));
        drainingBy = JsonTree.string(facts, "draining_by");
        var runtime = JsonTree.object(facts, FIXTURE_RUNTIME);
        systemSnapshotFixture = runtime != null ? JsonTree.asObject(runtime.get(FIXTURE_SNAPSHOT)) : null;
        applyCleanupFacts(facts);
    }

    /**
     * Applies the cleanup facts a state declares: whether a job is open, the job itself, the sweep
     * durations, the kept items, and the residues. A fact the state does not declare keeps the
     * engine's default.
     *
     * @param facts the seeding state's operator facts
     */
    private void applyCleanupFacts(@Nullable Map<String, Object> facts) {
        if (facts == null) {
            return;
        }
        if (Boolean.FALSE.equals(JsonTree.bool(facts, "open"))) {
            activeCleanupJob = null;
        }
        var job = JsonTree.object(facts, FIXTURE_JOB);
        if (job != null) {
            activeCleanupJob = readFixture(() -> CleanupJob.fromMap(job), PATH_FACTS + FIXTURE_JOB);
        }
        if (JsonTree.object(facts, "progress") != null && activeCleanupJob != null) {
            var running = activeCleanupJob;
            activeCleanupJob = new CleanupJob(running.jobId(), CleanupJobState.DELETING, running.dueSince(),
                    running.summary(), running.projectItems(), running.links());
        }
        var durations = JsonTree.object(facts, FIXTURE_DURATIONS);
        if (durations != null) {
            cleanupDurations = readFixture(() -> CleanupDurations.fromMap(durations), PATH_FACTS + FIXTURE_DURATIONS);
        }
        if (facts.containsKey("kept")) {
            keptItems.clear();
            keptItems.addAll(CleanupFixtures.keptItems(fixtureObjects(facts, "kept", "facts.kept")));
        }
        if (facts.containsKey(FIXTURE_RESIDUE)) {
            cleanupResidues.clear();
            cleanupResidues.addAll(CleanupFixtures.residues(
                    fixtureObjects(facts, FIXTURE_RESIDUE, PATH_FACTS + FIXTURE_RESIDUE)));
        }
    }

    /**
     * @param stateId a state of the active scenario, may be {@code null}
     * @return that state's operator facts, or {@code null} when the state or its facts are absent
     */
    private @Nullable Map<String, Object> stateFacts(@Nullable String stateId) {
        if (stateId == null) {
            return null;
        }
        var scenario = currentScenario.get();
        if (scenario == null) {
            return null;
        }
        var state = scenario.state(stateId);
        var representation = state != null ? JsonTree.asObject(state.representation()) : null;
        return representation != null
                ? JsonTree.asObject(representation.get(OperatorStateResource.FIELD_FACTS))
                : null;
    }

    private void initDefaultProjects() {
        synchronized (projectsLock) {
            enrolledProjects.clear();
            favouriteProjectIds.clear();
            beingAddedProjects.clear();

            var fixture = startRuntimeFixture();
            var fixtureProjects = fixtureObjects(fixture, FIXTURE_PROJECTS);
            if (fixture != null && fixture.containsKey(FIXTURE_PROJECTS)) {
                for (var project : fixtureProjects) {
                    var enrolled = readFixture(() -> EnrolledProject.fromMap(project), "facts.runtime.projects");
                    enrolledProjects.add(enrolled);
                    if (enrolled.isFavourite()) {
                        favouriteProjectIds.add(enrolled.projectId());
                    }
                }
                for (var beingAdded : fixtureObjects(fixture, FIXTURE_BEING_ADDED)) {
                    beingAddedProjects.add(readFixture(() -> BeingAddedProject.fromMap(beingAdded),
                            "facts.runtime.being_added"));
                }
                return;
            }

            favouriteProjectIds.add(CONST_PM_MCP_CLIENTS);
            favouriteProjectIds.add(CONST_PM_MCP_CORE);

            enrolledProjects.add(new EnrolledProject(
                    CONST_PM_MCP_CLIENTS,
                    "plan-marshall/" + CONST_PM_MCP_CLIENTS,
                    CONST_GITHUB_PM_PREFIX + CONST_PM_MCP_CLIENTS,
                    ProjectReadiness.READY,
                    CONST_DECISION,
                    2,
                    3,
                    1,
                    Instant.parse("2026-10-17T10:04:00Z"),
                    true,
                    false
            ));
            enrolledProjects.add(new EnrolledProject(
                    CONST_PM_MCP_CORE,
                    "plan-marshall/" + CONST_PM_MCP_CORE,
                    CONST_GITHUB_PM_PREFIX + CONST_PM_MCP_CORE,
                    ProjectReadiness.READY,
                    null,
                    0,
                    2,
                    0,
                    Instant.parse("2026-10-17T09:57:00Z"),
                    true,
                    false
            ));
            enrolledProjects.add(new EnrolledProject(
                    CONST_PLAN_MARSHALL,
                    CONST_PLAN_MARSHALL,
                    CONST_GITHUB_PM_PREFIX + CONST_PLAN_MARSHALL,
                    ProjectReadiness.SETUP_NEEDS,
                    "credential sonar",
                    1,
                    0,
                    0,
                    Instant.parse("2026-10-16T10:00:00Z"),
                    false,
                    false
            ));
            enrolledProjects.add(new EnrolledProject(
                    "cui-http",
                    "de.cuioss:cui-http",
                    "https://github.com/cuioss/cui-http",
                    ProjectReadiness.READY,
                    null,
                    0,
                    0,
                    0,
                    Instant.parse(CONST_TIME_10_14),
                    false,
                    false
            ));
            enrolledProjects.add(new EnrolledProject(
                    "API-Sheriff",
                    "API-Sheriff",
                    CONST_GITHUB_PM_PREFIX + "api-sheriff",
                    ProjectReadiness.READY,
                    null,
                    0,
                    0,
                    0,
                    Instant.parse("2026-09-12T10:00:00Z"),
                    false,
                    false
            ));
            enrolledProjects.add(new EnrolledProject(
                    CONST_LEGACY_PORTAL,
                    CONST_LEGACY_PORTAL,
                    CONST_GITHUB_PM_PREFIX + CONST_LEGACY_PORTAL,
                    ProjectReadiness.RELEASED,
                    null,
                    0,
                    0,
                    0,
                    Instant.parse("2026-08-17T10:00:00Z"),
                    false,
                    true
            ));

            beingAddedProjects.add(new BeingAddedProject(
                    "~/git/cui-parent",
                    STEP_IDENTITY,
                    2,
                    10,
                    "/api/v1/entries/ent-cui/continue",
                    "/api/v1/entries/ent-cui" + SUFFIX_ABANDON
            ));
        }
    }

    private void initDefaultEnrolment() {
        synchronized (storeLock) {
            mockStores.clear();
            mockStores.put(CONST_PM_MCP_CLIENTS, new MockStore(CONST_PM_MCP_CLIENTS, ENROL_STORE_PATH,
                    1248L, ENROL_RECORDED_SHA256, 1512L, ENROL_CURRENT_SHA256));
            mockStores.put(CONST_PLAN_MARSHALL, new MockStore(CONST_PLAN_MARSHALL, ENROL_STORE_PATH,
                    2048L, ENROL_RECORDED_SHA256, 2048L, ENROL_RECORDED_SHA256));
        }
    }

    /**
     * A store the mock runtime keeps for enrolment integrity: the size and SHA-256 its digest sidecar
     * records and the size and SHA-256 the file currently has.
     */
    private record MockStore(String projectId, String storePath, long recordedSize, String recordedSha256,
    long currentSize, String currentSha256) {

        /** @return the store re-baselined to its current file. */
        MockStore rebased() {
            return new MockStore(projectId, storePath, currentSize, currentSha256, currentSize, currentSha256);
        }
    }

    private boolean handleEnrolmentRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        var method = request.method();

        if (METHOD_POST.equals(method) && PATH_ENROL.equals(path)) {
            handleEnrol(request, response, null);
            return true;
        }
        if (METHOD_POST.equals(method) && path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_ENROL)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(), path.length() - SEGMENT_ENROL.length());
            handleEnrol(request, response, prjId);
            return true;
        }
        if ("GET".equals(method) && path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_STORES_DIGEST)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(), path.length() - SEGMENT_STORES_DIGEST.length());
            handleStoreDigest(request, response, prjId);
            return true;
        }
        if (METHOD_POST.equals(method) && path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_STORES_ACCEPT_EDIT)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(),
                    path.length() - SEGMENT_STORES_ACCEPT_EDIT.length());
            handleAcceptEdit(request, response, prjId);
            return true;
        }
        return false;
    }

    private void handleEnrol(MockHttpRequest request, MockHttpResponse response, @Nullable String pathProjectId)
            throws IOException {
        String projectPath = null;
        boolean pilot = false;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    projectPath = JsonTree.string(parsed, EnrolRequest.FIELD_PATH);
                    if (projectPath == null) {
                        projectPath = JsonTree.string(parsed, "project_path");
                    }
                    pilot = Boolean.TRUE.equals(JsonTree.bool(parsed, EnrolRequest.FIELD_PILOT));
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // ignore a malformed payload and report the missing path below
            }
        }

        boolean hasId = pathProjectId != null && !pathProjectId.isBlank();
        if (!hasId && (projectPath == null || projectPath.isBlank())) {
            response.json(400, Map.of(KEY_ERROR, ERR_INVALID_REQUEST, KEY_MESSAGE, "A checkout path is required"));
            return;
        }
        var projectId = hasId ? pathProjectId : projectIdFromPath(projectPath);

        synchronized (projectsLock) {
            boolean known = enrolledProjects.stream().anyMatch(p -> p.projectId().equals(projectId));
            if (!known) {
                enrolledProjects.add(new EnrolledProject(projectId, projectId, null, ProjectReadiness.READY,
                        null, 0, 0, 0, Instant.now(), false, false));
            }
        }
        synchronized (storeLock) {
            mockStores.computeIfAbsent(projectId, id -> new MockStore(id, ENROL_STORE_PATH,
                    2048L, ENROL_RECORDED_SHA256, 2048L, ENROL_RECORDED_SHA256));
        }

        var timestamp = Instant.now();
        broadcastEvent(ProjectEnrolledEvent.EVENT_NAME,
                new ProjectEnrolledEvent(projectId, projectId, timestamp).toMap());
        broadcastEvent(ProjectChangedEvent.EVENT_NAME,
                Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, KEY_PROJECT_ID, projectId, KEY_STATE, "enrolled"));
        response.json(200, new EnrolResult(EnrolResult.STATUS_ENROLLED, projectId, pilot).toMap());
    }

    private static String projectIdFromPath(@Nullable String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        var trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        var slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    private void handleStoreDigest(MockHttpRequest request, MockHttpResponse response, String projectId)
            throws IOException {
        var requestedPath = readQueryParam(request.query(), KEY_STORE_PATH_QUERY);
        synchronized (storeLock) {
            var store = mockStores.get(projectId);
            if (store == null) {
                sendStoreNotFound(response, projectId);
                return;
            }
            var storePath = requestedPath != null && !requestedPath.isBlank() ? requestedPath : store.storePath();
            var mismatch = store.recordedSize() != store.currentSize()
                    || !store.recordedSha256().equals(store.currentSha256());
            var details = mismatch ? "the current file differs from the recorded sidecar" : null;
            response.json(200, new StoreDigestComparison(storePath, store.recordedSize(), store.recordedSha256(),
                    store.currentSize(), store.currentSha256(), details).toMap());
        }
    }

    private void handleAcceptEdit(MockHttpRequest request, MockHttpResponse response, String projectId)
            throws IOException {
        var accept = parseAcceptEdit(request.body());
        synchronized (storeLock) {
            var store = mockStores.get(projectId);
            if (store == null) {
                sendStoreNotFound(response, projectId);
                return;
            }
            if (accept == null || !store.currentSha256().equals(accept.confirmedSha256())) {
                response.json(409, Map.of(
                        KEY_ERROR, ERR_CONFIRMATION_MISMATCH,
                        KEY_MESSAGE, "The confirmed SHA-256 does not match the current file",
                        KEY_CURRENT_SHA256, store.currentSha256()));
                return;
            }
            var rebased = store.rebased();
            mockStores.put(projectId, rebased);
            broadcastEvent(EVENT_STORE_EDIT_ACCEPTED, Map.of(
                    KEY_PROJECT_ID, projectId,
                    KEY_STORE_PATH, accept.storePath(),
                    KEY_NEW_SHA256, rebased.currentSha256()));
            response.json(200, new AcceptEditResult(AcceptEditResult.STATUS_ACCEPTED, rebased.currentSha256()).toMap());
        }
    }

    private static @Nullable AcceptEditRequest parseAcceptEdit(byte[] body) {
        if (body.length == 0) {
            return null;
        }
        try {
            return AcceptEditRequest.fromJson(new String(body, StandardCharsets.UTF_8));
        } catch (IOException _) {
            return null;
        }
    }

    private static @Nullable String readQueryParam(@Nullable String query, String name) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (var part : query.split("&")) {
            if (part.startsWith(name + "=")) {
                return URLDecoder.decode(part.substring(name.length() + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void sendStoreNotFound(MockHttpResponse response, String projectId) throws IOException {
        response.json(404, Map.of(KEY_ERROR, ERR_STORE_NOT_FOUND,
                KEY_MESSAGE, "No store recorded for project " + projectId));
    }

    private boolean handleProjectExplorerRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        var method = request.method();

        return switch (method) {
            case "GET" -> handleProjectExplorerGet(request, response, path);
            case "PUT" -> handleProjectExplorerPut(request, response, path);
            case METHOD_POST -> handleProjectExplorerPost(request, response, path);
            case METHOD_DELETE -> handleProjectExplorerDelete(response, path);
            default -> false;
        };
    }

    /**
     * @return {@code true} when the active scenario declares the project explorer domain, so its
     *         stateful handler serves the live explorer and the single project
     */
    private boolean declaresProjects() {
        var name = currentScenario.get().name();
        return StandardScenarios.PROJECTS.equals(name) || StandardScenarios.PROJECTS_AND_START.equals(name);
    }

    private boolean handleProjectExplorerGet(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        if ("/api/v1/explorer".equals(path)
                || (PATH_PROJECTS.equals(path)
                && ((request.query() != null && request.query().contains("all"))
                || declaresProjects()))) {
            handleGetProjectsExplorer(response);
            return true;
        }
        if (path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_SETUP)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(),
                    path.length() - SEGMENT_SETUP.length());
            response.json(200, projectSetupState(prjId,
                    projectSetupStates.getOrDefault(prjId, PROJECT_SETUP_IDENTITY)));
            return true;
        }
        if (declaresProjects()
                && path.startsWith("/api/v1/projects/")
                && !path.endsWith("/ended")
                && !path.endsWith(SEGMENT_EVENTS)
                && !path.endsWith(SEGMENT_PROJECT_FILES)
                && !path.endsWith(SEGMENT_PROJECT_ENTRIES)) {
            var prjId = path.substring("/api/v1/projects/".length());
            if (!prjId.contains("/")) {
                handleGetSingleProject(response, prjId);
                return true;
            }
        }
        if ("/api/v1/project-onboardings".equals(path)) {
            handleListProjectOnboardings(response);
            return true;
        }
        return false;
    }

    private boolean handleProjectExplorerPut(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        if ("/api/v1/explorer/favourites".equals(path)) {
            handleUpdateFavourites(request, response);
            return true;
        }
        if (path.startsWith("/api/v1/projects/") && path.endsWith("/favourite")) {
            var prjId = path.substring("/api/v1/projects/".length(), path.length() - "/favourite".length());
            handleToggleProjectFavourite(request, response, prjId);
            return true;
        }
        return false;
    }

    private boolean handleProjectExplorerPost(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        if ("/api/v1/project-onboardings".equals(path) || PATH_PROJECTS.equals(path)) {
            handleStartProjectOnboarding(request, response);
            return true;
        }
        if (path.startsWith("/api/v1/projects/") && path.endsWith("/release")) {
            var prjId = path.substring("/api/v1/projects/".length(), path.length() - "/release".length());
            handleReleaseProject(request, response, prjId);
            return true;
        }
        if (path.startsWith("/api/v1/projects/") && path.endsWith("/unenrol")) {
            var prjId = path.substring("/api/v1/projects/".length(), path.length() - "/unenrol".length());
            handleUnenrolProject(response, prjId);
            return true;
        }
        if (path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_SETUP)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(),
                    path.length() - SEGMENT_SETUP.length());
            handleSetupProject(request, response, prjId);
            return true;
        }
        return false;
    }

    private boolean handleProjectExplorerDelete(MockHttpResponse response, String path) throws IOException {
        if (path.startsWith("/api/v1/project-onboardings/")) {
            var onbId = path.substring("/api/v1/project-onboardings/".length());
            handleAbandonProjectOnboarding(response, onbId);
            return true;
        }
        if (path.startsWith(PATH_ENTRIES_PREFIX) && path.endsWith(SUFFIX_ABANDON)) {
            var entId = path.substring(PATH_ENTRIES_PREFIX.length(), path.length() - SUFFIX_ABANDON.length());
            handleAbandonProjectOnboarding(response, entId);
            return true;
        }
        return false;
    }

    private void handleGetProjectsExplorer(MockHttpResponse response) throws IOException {
        synchronized (projectsLock) {
            var favs = favouriteProjectIds.stream()
                    .flatMap(favId -> enrolledProjects.stream().filter(p -> p.projectId().equals(favId)))
                    .toList();

            var cutoff = Instant.parse("2026-09-17T00:00:00Z");
            var recents = enrolledProjects.stream()
                    .filter(p -> !p.isFavourite() && !p.isReleased() && p.lastActive() != null && p.lastActive().isAfter(cutoff))
                    .sorted((a, b) -> b.lastActive().compareTo(a.lastActive()))
                    .limit(5)
                    .toList();

            var all = enrolledProjects.stream()
                    .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                    .toList();

            var being = new ArrayList<>(beingAddedProjects);

            var explorer = new ProjectExplorer(favs, recents, all, being);
            var map = new LinkedHashMap<String, Object>(explorer.toMap());
            var projectsList = all.stream().map(EnrolledProject::toMap).toList();
            map.put(CONST_PROJECTS, projectsList);

            response.json(200, map);
        }
    }

    private static void sendProjectNotFound(MockHttpResponse response, String projectId) throws IOException {
        response.json(404, Map.of(KEY_ERROR, ERR_PROJECT_NOT_FOUND, KEY_MESSAGE, "Project " + projectId + " not found"));
    }

    private void handleGetSingleProject(MockHttpResponse response, String projectId) throws IOException {
        synchronized (projectsLock) {
            for (var p : enrolledProjects) {
                if (p.projectId().equals(projectId)) {
                    var map = new LinkedHashMap<String, Object>(p.toMap());
                    map.put("plans", List.of());
                    map.put("epics", List.of());
                    map.put("alongside_landings", List.of());
                    response.json(200, map);
                    return;
                }
            }
            sendProjectNotFound(response, projectId);
        }
    }

    private void handleListProjectOnboardings(MockHttpResponse response) throws IOException {
        synchronized (projectsLock) {
            var list = beingAddedProjects.stream().map(BeingAddedProject::toMap).toList();
            response.json(200, Map.of("onboardings", list));
        }
    }

    private void handleUpdateFavourites(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var ids = parseFavouriteIds(request.body());
        synchronized (projectsLock) {
            favouriteProjectIds.clear();
            favouriteProjectIds.addAll(ids);

            var updatedList = enrolledProjects.stream()
                    .map(p -> new EnrolledProject(
                            p.projectId(), p.displayName(), p.remoteUrl(), p.readiness(),
                            p.setupNeedsSummary(), p.needsOperatorCount(), p.activePlansCount(),
                            p.activeEpicsCount(), p.lastActive(), favouriteProjectIds.contains(p.projectId()), p.isReleased()
                    ))
                    .toList();
            enrolledProjects.clear();
            enrolledProjects.addAll(updatedList);

            broadcastEvent(ProjectChangedEvent.EVENT_NAME, Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, "favourites", favouriteProjectIds));
            response.json(200, Map.of(KEY_STATUS, "ok", "project_ids", favouriteProjectIds));
        }
    }

    private static List<String> parseFavouriteIds(byte[] body) {
        if (body.length == 0) {
            return List.of();
        }
        try {
            var req = FavouritesUpdateRequest.fromJson(new String(body, StandardCharsets.UTF_8));
            return req.projectIds();
        } catch (IOException _) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(body));
                if (parsed != null && parsed.get("project_ids") instanceof List<?> list) {
                    return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
                }
            } catch (IOException _) {
                // ignore malformed payload
            }
        }
        return List.of();
    }

    private void handleToggleProjectFavourite(MockHttpRequest request, MockHttpResponse response, String projectId) throws IOException {
        boolean fav = true;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null && parsed.containsKey("favourite")) {
                    fav = Boolean.TRUE.equals(parsed.get("favourite"));
                }
            } catch (IOException _) {
                // ignore malformed payload
            }
        }
        synchronized (projectsLock) {
            if (fav && !favouriteProjectIds.contains(projectId)) {
                favouriteProjectIds.add(projectId);
            } else if (!fav) {
                favouriteProjectIds.remove(projectId);
            }
            var updatedList = new ArrayList<EnrolledProject>();
            boolean found = false;
            for (var p : enrolledProjects) {
                if (p.projectId().equals(projectId)) {
                    found = true;
                    updatedList.add(new EnrolledProject(
                            p.projectId(), p.displayName(), p.remoteUrl(), p.readiness(),
                            p.setupNeedsSummary(), p.needsOperatorCount(), p.activePlansCount(),
                            p.activeEpicsCount(), p.lastActive(), fav, p.isReleased()
                    ));
                } else {
                    updatedList.add(p);
                }
            }
            if (!found) {
                sendProjectNotFound(response, projectId);
                return;
            }
            enrolledProjects.clear();
            enrolledProjects.addAll(updatedList);

            broadcastEvent(ProjectChangedEvent.EVENT_NAME, Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, KEY_PROJECT_ID, projectId, "is_favourite", fav));
            response.json(200, Map.of(KEY_PROJECT_ID, projectId, "is_favourite", fav));
        }
    }

    private void handleStartProjectOnboarding(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String path = "~/git/new-project";
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    if (parsed.get("path") instanceof String p) path = p;
                    else if (parsed.get("checkout_path") instanceof String cp) path = cp;
                }
            } catch (IOException _) {
                // ignore malformed payload
            }
        }
        String id = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        if (id.isBlank()) id = "new-project";

        synchronized (projectsLock) {
            var beingAdded = new BeingAddedProject(
                    path,
                    STEP_IDENTITY,
                    2,
                    10,
                    "/api/v1/entries/ent-" + id + "/continue",
                    "/api/v1/entries/ent-" + id + SUFFIX_ABANDON
            );
            beingAddedProjects.add(beingAdded);
            broadcastEvent(ProjectChangedEvent.EVENT_NAME, Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, "being_added", path));
            response.json(201, Map.of(
                    KEY_STATUS, "started",
                    "onboarding_id", "onb-" + id,
                    "checkout_path", path,
                    "current_step", STEP_IDENTITY,
                    "step_number", 2,
                    "total_steps", 10
            ));
        }
    }

    private void handleReleaseProject(MockHttpRequest request, MockHttpResponse response, String projectId) throws IOException {
        synchronized (projectsLock) {
            EnrolledProject target = null;
            for (var p : enrolledProjects) {
                if (p.projectId().equals(projectId)) {
                    target = p;
                    break;
                }
            }
            if (target == null) {
                sendProjectNotFound(response, projectId);
                return;
            }

            int plans = target.activePlansCount();
            int epics = target.activeEpicsCount();
            String reason = "Released by operator";
            if (request.body().length > 0) {
                try {
                    var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                    if (parsed != null && parsed.get(KEY_REASON) instanceof String r) {
                        reason = r;
                    }
                } catch (IOException _) {
                    // ignore malformed payload
                }
            }

            var releasedProj = new EnrolledProject(
                    target.projectId(),
                    target.displayName(),
                    target.remoteUrl(),
                    ProjectReadiness.RELEASED,
                    null,
                    0,
                    0,
                    0,
                    Instant.now(),
                    target.isFavourite(),
                    true
            );

            var updatedList = new ArrayList<EnrolledProject>();
            for (var p : enrolledProjects) {
                if (p.projectId().equals(projectId)) {
                    updatedList.add(releasedProj);
                } else {
                    updatedList.add(p);
                }
            }
            enrolledProjects.clear();
            enrolledProjects.addAll(updatedList);

            var result = new ProjectReleaseResult(projectId, plans, epics, 0, Instant.now(), reason, ReleaseWaitStatus.NONE);
            broadcastEvent(ProjectReleasedEvent.EVENT_NAME, result.toMap());
            broadcastEvent(ProjectChangedEvent.EVENT_NAME, Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, KEY_PROJECT_ID, projectId, "readiness", "released"));
            response.json(200, result.toMap());
        }
    }

    private void handleUnenrolProject(MockHttpResponse response, String projectId) throws IOException {
        synchronized (projectsLock) {
            EnrolledProject target = null;
            for (var p : enrolledProjects) {
                if (p.projectId().equals(projectId)) {
                    target = p;
                    break;
                }
            }
            if (target == null) {
                sendProjectNotFound(response, projectId);
                return;
            }
            if (target.activePlansCount() > 0 || target.activeEpicsCount() > 0) {
                response.json(409, Map.of(
                        KEY_ERROR, "scope_active",
                        KEY_MESSAGE, projectId + " cannot be unenrolled while work runs",
                        "active_plans", target.activePlansCount(),
                        "active_epics", target.activeEpicsCount()
                ));
                return;
            }
            enrolledProjects.removeIf(p -> p.projectId().equals(projectId));
            favouriteProjectIds.remove(projectId);
            synchronized (storeLock) {
                mockStores.remove(projectId);
            }
            broadcastEvent(ProjectChangedEvent.EVENT_NAME, Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, KEY_PROJECT_ID, projectId, KEY_STATUS, CONST_UNENROLLED));
            broadcastEvent(EVENT_PROJECT_UNENROLLED, Map.of(KEY_EVENT, EVENT_PROJECT_UNENROLLED, KEY_PROJECT_ID, projectId));
            var body = new LinkedHashMap<String, Object>(
                    new UnenrolResult(UnenrolResult.STATUS_UNENROLLED).toMap());
            body.put(KEY_PROJECT_ID, projectId);
            response.json(200, body);
        }
    }

    private void handleAbandonProjectOnboarding(MockHttpResponse response, String id) throws IOException {
        synchronized (projectsLock) {
            beingAddedProjects.removeIf(b -> b.checkoutPath().contains(id) || (b.abandonLink() != null && b.abandonLink().contains(id)));
            broadcastEvent(ProjectChangedEvent.EVENT_NAME, Map.of(KEY_EVENT, EVENT_PROJECT_CHANGED, "abandoned", id));
            response.json(200, Map.of(KEY_STATUS, "abandoned", "id", id));
        }
    }

    /**
     * Serves a project's onboarding run as the state dialogue reads it ({@code CLI-UC-P-01}): the
     * first call reports the run's current state, a call that names a link advances it. A run of a
     * project that is only being added has no enrolment yet, so its id is the one the caller named.
     *
     * @param request   the setup call
     * @param response  the response to complete
     * @param projectId the project or checkout the run belongs to
     * @throws IOException if the body cannot be read
     */
    private void handleSetupProject(MockHttpRequest request, MockHttpResponse response,
            String projectId) throws IOException {
        var body = readBodyMap(request);
        var linkName = JsonTree.string(body, "link");
        var current = projectSetupStates.getOrDefault(projectId, PROJECT_SETUP_IDENTITY);
        if (linkName != null && PROJECT_SETUP_IDENTITY.equals(current)) {
            current = PROJECT_SETUP_COMPLETED;
        }
        projectSetupStates.put(projectId, current);
        response.json(200, projectSetupState(projectId, current));
    }

    private Map<String, Object> projectSetupState(String projectId, String stateId) {
        var body = new LinkedHashMap<String, Object>();
        body.put(KEY_PROJECT_ID, projectId);
        body.put(KEY_STATE, stateId);
        body.put("total_steps", 10);
        if (PROJECT_SETUP_COMPLETED.equals(stateId)) {
            body.put("step", 10);
            body.put("summary", projectId + PROJECT_SETUP_READY_SUFFIX);
            body.put(KEY_LINKS, Map.of());
            return body;
        }
        body.put("step", 2);
        body.put("id", projectId);
        body.put("name", displayNameOf(projectId));
        body.put("pilot", false);
        body.put(KEY_LINKS, Map.of("set", Map.of(KEY_HREF,
                PATH_PROJECTS_PREFIX + projectId + SEGMENT_SETUP, KEY_METHOD, METHOD_POST)));
        return body;
    }

    private String displayNameOf(String projectId) {
        synchronized (projectsLock) {
            return enrolledProjects.stream()
                    .filter(project -> project.projectId().equals(projectId))
                    .map(EnrolledProject::displayName)
                    .findFirst()
                    .orElse(projectId);
        }
    }

    private boolean handleSystemRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        var method = request.method();

        if ("GET".equals(method) && "/api/v1/system".equals(path)) {
            handleGetSystem(request, response);
            return true;
        }
        if ("GET".equals(method) && "/api/v1/system/release-notes".equals(path)) {
            handleGetReleaseNotes(request, response);
            return true;
        }
        if (METHOD_POST.equals(method) && ("/api/v1/operator/runtime/stop".equals(path) || "/api/v1/runtime/stop".equals(path))) {
            handlePostStopRuntime(request, response, isWeb);
            return true;
        }
        if (METHOD_DELETE.equals(method) && ("/api/v1/operator/runtime/stop".equals(path) || "/api/v1/runtime/stop".equals(path))) {
            handleDeleteStopRuntime(response);
            return true;
        }
        if (METHOD_POST.equals(method) && "/api/v1/operator/runtime/upgrade".equals(path)) {
            handlePostUpgradeRuntime(request, response);
            return true;
        }
        if (METHOD_DELETE.equals(method) && "/api/v1/operator/runtime/upgrade".equals(path)) {
            handleDeleteUpgradeRuntime(response);
            return true;
        }
        if (METHOD_POST.equals(method) && "/api/v1/operator/runtime/upgrade/check".equals(path)) {
            handleCheckUpgradeRuntime(response);
            return true;
        }
        if ("GET".equals(method) && "/api/v1/system/cleanup".equals(path)) {
            handleGetCleanup(response);
            return true;
        }
        if ("GET".equals(method) && "/api/v1/system/cleanup/kept".equals(path)) {
            handleGetKeptCleanup(response);
            return true;
        }
        if ("PUT".equals(method) && "/api/v1/system/cleanup/kept".equals(path)) {
            handlePutKeptCleanup(request, response);
            return true;
        }
        if (path.startsWith(PATH_SYSTEM_CLEANUP) && METHOD_POST.equals(method)) {
            var sub = path.substring(PATH_SYSTEM_CLEANUP.length());
            if (sub.endsWith(SEGMENT_DELETE)) {
                var jobId = sub.substring(0, sub.length() - SEGMENT_DELETE.length());
                handleDeleteCleanupJob(request, response, jobId, isWeb);
                return true;
            }
            if (sub.endsWith("/remind")) {
                var jobId = sub.substring(0, sub.length() - "/remind".length());
                handleRemindCleanupJob(response, jobId, isWeb);
                return true;
            }
            if (sub.endsWith("/ignore")) {
                var jobId = sub.substring(0, sub.length() - "/ignore".length());
                handleIgnoreCleanupJob(response, jobId, isWeb);
                return true;
            }
        }
        return false;
    }

    private void handleGetSystem(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var query = request.query() != null ? request.query() : "";
        Map<String, Object> snapshotFixture;
        synchronized (systemLock) {
            snapshotFixture = systemSnapshotFixture;
        }
        if (snapshotFixture != null && query.isBlank()) {
            response.json(200, snapshotFixture);
            return;
        }
        SystemWindow window = SystemWindow.HOUR;
        String projectFilter = null;
        for (var part : query.split("&")) {
            if (part.startsWith("window=")) {
                window = SystemWindow.fromValue(part.substring("window=".length()));
            } else if (part.startsWith("project=")) {
                projectFilter = part.substring("project=".length());
            }
        }
        if (".".equals(projectFilter)) {
            projectFilter = CONST_PM_MCP_CLIENTS;
        }

        var runtimeMetrics = new RuntimeMetrics(
                "0.9.1",
                "2026-10-02 (a1b2c3d)",
                "today 08:14",
                22320L,
                432013312L,
                1073741824L,
                713031680L,
                4.0,
                2469606195L,
                40802189312L,
                "13:40",
                true,
                "127.0.0.1:" + (webPort > 0 ? webPort : 7420),
                lanListenerOn ? "192.168.1.20:7421" : "off",
                2,
                0,
                null,
                "0.10.0",
                "2026-10-16",
                "today 08:14",
                "/api/v1/system/release-notes?version=0.10.0",
                true,
                null
        );

        Map<String, Integer> plansByState;
        Map<String, Integer> epicsByState;
        int tasksRunning;
        int tasksQueued;
        int activeProjects;
        if (projectFilter != null && !projectFilter.isBlank()) {
            plansByState = Map.of("running", 2, "waiting_for_you", 1);
            epicsByState = Map.of("running", 1);
            tasksRunning = 2;
            tasksQueued = 3;
            activeProjects = 1;
        } else {
            plansByState = Map.of("running", 3, "waiting_for_you", 1, "held", 1, "paused", 2);
            epicsByState = Map.of("running", 1, "waiting_for_you", 1);
            tasksRunning = 4;
            tasksQueued = 6;
            activeProjects = 3;
        }
        var workMetrics = new WorkMetrics(plansByState, epicsByState, tasksRunning, tasksQueued, activeProjects, List.of());

        var workers = List.of(
                new WorkerMetric("planner", CONST_CLAUDE, "opus-4.5", WorkerState.BUSY, "fix-relay"),
                new WorkerMetric("implementer", CONST_CLAUDE, CONST_SONNET, WorkerState.BUSY, "relay-docs"),
                new WorkerMetric("implementer", "codex", "gpt-5", WorkerState.IDLE, null),
                new WorkerMetric("implementer", CONST_CLAUDE, CONST_SONNET, WorkerState.BUSY, "token-usage"),
                new WorkerMetric("reviewer", CONST_CLAUDE, CONST_SONNET, WorkerState.IDLE, null)
        );

        var jobs = Map.<String, Object>of(
                "build", Map.of("slots_used", 1, "slots_total", 2, "queued", 1),
                "test", Map.of("slots_used", 0, "slots_total", 1, "queued", 0),
                "locks_held", 3
        );

        List<ModelMetric> models;
        switch (window) {
            case TODAY -> models = List.of(
                    new ModelMetric("claude:opus-4.5", "planner", 164, 7890000L, 7420000L, 470000L, 1, 3, false, 0),
                    new ModelMetric("claude:sonnet-4.5", "implementer", 430, 13150000L, 11650000L, 1500000L, 0, 2, false, 0),
                    new ModelMetric("codex:gpt-5", "implementer", 48, 0L, 0L, 0L, 3, 0, true, 48)
            );
            case SEVEN_DAYS -> models = List.of(
                    new ModelMetric("claude:opus-4.5", "planner", 980, 48200000L, 45300000L, 2900000L, 4, 15, false, 0),
                    new ModelMetric("claude:sonnet-4.5", "implementer", 2840, 86700000L, 76800000L, 9900000L, 2, 8, false, 0),
                    new ModelMetric("codex:gpt-5", "implementer", 310, 0L, 0L, 0L, 12, 1, true, 310)
            );
            default -> models = List.of(
                    new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L, 0, 1, false, 0),
                    new ModelMetric("claude:sonnet-4.5", "implementer", 118, 3610000L, 3200000L, 410000L, 0, 0, false, 0),
                    new ModelMetric("codex:gpt-5", "implementer", 12, 0L, 0L, 0L, 1, 0, true, 12)
            );
        }

        boolean hasOpenJob;
        CleanupSummary cleanupSummary;
        synchronized (systemLock) {
            hasOpenJob = activeCleanupJob != null && activeCleanupJob.isOpen();
            cleanupSummary = hasOpenJob
                    ? activeCleanupJob.summary()
                    : new CleanupSummary(0, 0, 0, 0, 0, 0L, keptItems.size(), SWEEP_TODAY, SWEEP_TOMORROW);
        }

        var snapshot = new SystemSnapshot(runtimeMetrics, workMetrics, workers, jobs, models, cleanupSummary, window, projectFilter);
        response.json(200, snapshot.toMap());
    }

    private void handleGetReleaseNotes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String version = "0.10.0";
        if (request.query() != null) {
            for (var part : request.query().split("&")) {
                if (part.startsWith("version=")) {
                    version = part.substring("version=".length());
                }
            }
        }
        if ("0.10.0".equals(version) || version.isBlank()) {
            String notes = """
                    pm-mcpd 0.10.0 Release Notes

                    Highlights:
                    - System overview and runtime lifecycle controls (stop, restart, drain, upgrade).
                    - Automated and interactive cleanup of ended work history.
                    - Granular token metrics per model and role.
                    - Improved network listener isolation and paired devices management.
                    """;
            response.send(200, "text/plain; charset=utf-8", notes);
        } else {
            response.json(404, Map.of(KEY_ERROR, "version_not_found", KEY_MESSAGE, "No release notes available for version: " + version));
        }
    }

    private void handlePostStopRuntime(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        String when = "drain";
        String thenVal = "exit";
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    var w = JsonTree.string(parsed, "when");
                    if (w != null && !w.isBlank()) {
                        when = w;
                    }
                    var t = JsonTree.string(parsed, "then");
                    if (t != null && !t.isBlank()) {
                        thenVal = t;
                    }
                }
            }
            /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (IOException | RuntimeException _) {
                // Ignore parse errors from body
            }
        }

        synchronized (systemLock) {
            if ("now".equalsIgnoreCase(when)) {
                lifecycleState = RuntimeLifecycleState.STOPPING;
                broadcastEvent("runtime.stopping", Map.of(
                        "lifecycle_state", "stopping",
                        "then", thenVal
                ));
                response.json(200, Map.of(
                        KEY_STATUS, "stopping",
                        KEY_STATE, "stopping",
                        "then", thenVal
                ));
            } else {
                lifecycleState = RuntimeLifecycleState.DRAINING;
                drainingBy = isWeb ? "the web app" : "the console";
                canWithdrawStop = true;
                broadcastEvent("runtime.draining", Map.of(
                        "lifecycle_state", "draining",
                        "draining_by", drainingBy,
                        "remaining_work_count", 4,
                        "can_withdraw", true
                ));
                response.json(200, Map.of(
                        KEY_STATUS, "draining",
                        KEY_STATE, "draining",
                        "draining_by", drainingBy,
                        "remaining_work_count", 4,
                        "can_withdraw", true
                ));
            }
        }
    }

    private void handleDeleteStopRuntime(MockHttpResponse response) throws IOException {
        synchronized (systemLock) {
            if (lifecycleState == RuntimeLifecycleState.DRAINING) {
                lifecycleState = RuntimeLifecycleState.RUNNING;
                drainingBy = null;
                canWithdrawStop = false;
                broadcastEvent("runtime.drain_withdrawn", Map.of(
                        "lifecycle_state", "running",
                        KEY_STATUS, "drain_withdrawn"
                ));
                response.json(200, Map.of(
                        KEY_STATUS, "withdrawn",
                        "lifecycle_state", "running"
                ));
            } else {
                response.json(409, Map.of(
                        KEY_ERROR, "cannot_withdraw",
                        KEY_MESSAGE, "Cannot withdraw stop: runtime is not draining"
                ));
            }
        }
    }

    private void handlePostUpgradeRuntime(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String targetVersion = "0.10.0";
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    var v = JsonTree.string(parsed, FIELD_VERSION);
                    if (v != null && !v.isBlank()) {
                        targetVersion = v;
                    }
                }
            }
            /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (IOException | RuntimeException _) {
                // Ignore parse errors from body
            }
        }
        synchronized (systemLock) {
            lifecycleState = RuntimeLifecycleState.UPGRADING;
            broadcastEvent("runtime.upgrading", Map.of(
                    "lifecycle_state", "upgrading",
                    FIELD_VERSION, targetVersion,
                    "step", "preparing"
            ));
            response.json(200, Map.of(
                    KEY_STATUS, "upgrading",
                    KEY_STATE, "upgrading",
                    FIELD_VERSION, targetVersion,
                    "step", "preparing"
            ));
        }
    }

    private void handleDeleteUpgradeRuntime(MockHttpResponse response) throws IOException {
        synchronized (systemLock) {
            if (lifecycleState == RuntimeLifecycleState.UPGRADING) {
                lifecycleState = RuntimeLifecycleState.RUNNING;
                response.json(200, Map.of(
                        KEY_STATUS, "withdrawn",
                        "lifecycle_state", "running"
                ));
            } else {
                response.json(409, Map.of(
                        KEY_ERROR, "cannot_withdraw",
                        KEY_MESSAGE, "Cannot withdraw upgrade: runtime is not upgrading"
                ));
            }
        }
    }

    private void handleCheckUpgradeRuntime(MockHttpResponse response) throws IOException {
        response.json(200, Map.of(
                FIELD_VERSION, "0.10.0",
                "available", true,
                "release_date", "2026-10-16",
                "release_notes_url", "/api/v1/system/release-notes?version=0.10.0"
        ));
    }

    private void handleGetCleanup(MockHttpResponse response) throws IOException {
        synchronized (systemLock) {
            if (activeCleanupJob != null && activeCleanupJob.isOpen()) {
                response.json(200, activeCleanupJob.toMap());
            } else {
                var resp = new LinkedHashMap<String, Object>();
                resp.put("open", false);
                resp.put("next_check_at", SWEEP_TOMORROW);
                resp.put("kept_count", keptItems.size());
                resp.put(FIXTURE_DURATIONS, cleanupDurations.toMap());
                resp.put(FIXTURE_RESIDUE, cleanupResidues.stream().map(CleanupResidue::toMap).toList());
                response.json(200, resp);
            }
        }
    }

    private void handleDeleteCleanupJob(MockHttpRequest request, MockHttpResponse response, String jobId, boolean isWeb) throws IOException {
        synchronized (systemLock) {
            if (lifecycleState == RuntimeLifecycleState.DRAINING || lifecycleState == RuntimeLifecycleState.STOPPING) {
                response.json(409, Map.of(
                        KEY_ERROR, "runtime_stopping",
                        KEY_MESSAGE, "The runtime is stopping; nothing new starts."
                ));
                return;
            }
            if (activeCleanupJob == null || !activeCleanupJob.jobId().equals(jobId)) {
                response.json(404, Map.of(KEY_ERROR, "job_not_found", KEY_MESSAGE, "Cleanup job not found: " + jobId));
                return;
            }
            if (activeCleanupJob.state() == CleanupJobState.DONE) {
                var done = activeCleanupJob.summary();
                response.json(200, Map.of(
                        KEY_STATUS, "deleted",
                        "job_id", jobId,
                        CONST_DECISION, "delete",
                        FIELD_FREED_BYTES, done.freedBytes(),
                        "kept_count", keptItems.size()
                ));
                return;
            }

            List<String> unchecked = List.of();
            if (request.body().length > 0) {
                try {
                    var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                    if (parsed != null && parsed.get("unchecked_ids") instanceof List<?> list) {
                        unchecked = list.stream().map(String::valueOf).toList();
                    }
                }
                /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
                catch (IOException | RuntimeException _) {
                    // Ignore parse errors from body
                }
            }

            for (var un : unchecked) {
                var entry = cleanupEntry(un);
                if (entry != null) {
                    keptItems.add(new CleanupKeptItem(un, entry.kind(), entry.title(),
                            "left out of a delete", "today", isWeb ? "the web app" : "the console"));
                }
            }

            activeCleanupJob = new CleanupJob(
                    activeCleanupJob.jobId(),
                    CleanupJobState.DONE,
                    activeCleanupJob.dueSince(),
                    activeCleanupJob.summary(),
                    activeCleanupJob.projectItems(),
                    activeCleanupJob.links()
            );

            var decided = activeCleanupJob.summary();
            var freedBytes = decided.freedBytes();
            var deletedPlans = decided.plansCount();
            var deletedEpics = decided.epicsCount();
            var deletedLogs = decided.logsCount();
            for (var projectItem : activeCleanupJob.projectItems()) {
                for (var entry : projectItem.items()) {
                    if (!unchecked.contains(entry.id())) {
                        continue;
                    }
                    freedBytes -= entry.sizeBytes();
                    deletedLogs -= entry.logsCount();
                    if (CLEANUP_KIND_PLAN.equals(entry.kind())) {
                        deletedPlans--;
                    } else if (CLEANUP_KIND_EPIC.equals(entry.kind())) {
                        deletedEpics--;
                    }
                }
            }

            broadcastEvent("cleanup.decided", Map.of(
                    "job_id", jobId,
                    CONST_DECISION, "delete",
                    "by", "operator",
                    "channel", isWeb ? "web" : "cli",
                    "device", isWeb ? "iPad" : KEY_TERMINAL,
                    FIELD_FREED_BYTES, freedBytes
            ));
            broadcastEvent("cleanup.done", Map.of(
                    "job_id", jobId,
                    FIELD_FREED_BYTES, freedBytes,
                    "deleted_plans", deletedPlans,
                    "deleted_epics", deletedEpics,
                    "deleted_logs", deletedLogs
            ));

            response.json(200, Map.of(
                    KEY_STATUS, "deleted",
                    "job_id", jobId,
                    CONST_DECISION, "delete",
                    FIELD_FREED_BYTES, freedBytes,
                    "kept_count", keptItems.size()
            ));
        }
    }

    private void handleRemindCleanupJob(MockHttpResponse response, String jobId, boolean isWeb) throws IOException {
        synchronized (systemLock) {
            if (activeCleanupJob == null || !activeCleanupJob.jobId().equals(jobId)) {
                response.json(404, Map.of(KEY_ERROR, "job_not_found", KEY_MESSAGE, "Cleanup job not found: " + jobId));
                return;
            }
            if (activeCleanupJob.state() == CleanupJobState.POSTPONED) {
                response.json(200, Map.of(
                        KEY_STATUS, "postponed",
                        "job_id", jobId,
                        CONST_DECISION, "remind",
                        "remind_after_days", 3,
                        "remind_at", "2026-10-19T03:00:00Z"
                ));
                return;
            }

            activeCleanupJob = new CleanupJob(
                    activeCleanupJob.jobId(),
                    CleanupJobState.POSTPONED,
                    activeCleanupJob.dueSince(),
                    activeCleanupJob.summary(),
                    activeCleanupJob.projectItems(),
                    activeCleanupJob.links()
            );

            broadcastEvent("cleanup.decided", Map.of(
                    "job_id", jobId,
                    CONST_DECISION, "remind",
                    "by", "operator",
                    "channel", isWeb ? "web" : "cli",
                    "remind_after_days", 3
            ));

            response.json(200, Map.of(
                    KEY_STATUS, "postponed",
                    "job_id", jobId,
                    CONST_DECISION, "remind",
                    "remind_after_days", 3,
                    "remind_at", "2026-10-19T03:00:00Z"
            ));
        }
    }

    private void handleIgnoreCleanupJob(MockHttpResponse response, String jobId, boolean isWeb) throws IOException {
        synchronized (systemLock) {
            if (activeCleanupJob == null || !activeCleanupJob.jobId().equals(jobId)) {
                response.json(404, Map.of(KEY_ERROR, "job_not_found", KEY_MESSAGE, "Cleanup job not found: " + jobId));
                return;
            }
            if (activeCleanupJob.state() == CleanupJobState.DONE) {
                response.json(200, Map.of(
                        KEY_STATUS, "done",
                        "job_id", jobId,
                        CONST_DECISION, "ignore",
                        "kept_count", keptItems.size()
                ));
                return;
            }

            for (var prj : activeCleanupJob.projectItems()) {
                for (var entry : prj.items()) {
                    keptItems.add(new CleanupKeptItem(entry.id(), entry.kind(), entry.title(), "ignored", "today", isWeb ? "the web app" : "the console"));
                }
            }

            activeCleanupJob = new CleanupJob(
                    activeCleanupJob.jobId(),
                    CleanupJobState.DONE,
                    activeCleanupJob.dueSince(),
                    activeCleanupJob.summary(),
                    activeCleanupJob.projectItems(),
                    activeCleanupJob.links()
            );

            broadcastEvent("cleanup.decided", Map.of(
                    "job_id", jobId,
                    CONST_DECISION, "ignore",
                    "by", "operator",
                    "channel", isWeb ? "web" : "cli"
            ));

            response.json(200, Map.of(
                    KEY_STATUS, "done",
                    "job_id", jobId,
                    CONST_DECISION, "ignore",
                    "kept_count", keptItems.size()
            ));
        }
    }

    /**
     * @param id the id of an item the operator left out of a delete
     * @return the job's own item entry, or {@code null} when the job carries no such item
     */
    private @Nullable CleanupItemEntry cleanupEntry(String id) {
        if (activeCleanupJob != null) {
            for (var projectItem : activeCleanupJob.projectItems()) {
                for (var entry : projectItem.items()) {
                    if (entry.id().equals(id)) {
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    private void handleGetKeptCleanup(MockHttpResponse response) throws IOException {
        var list = new ArrayList<Map<String, Object>>();
        synchronized (systemLock) {
            for (var k : keptItems) {
                list.add(k.toMap());
            }
        }
        response.json(200, list);
    }

    private void handlePutKeptCleanup(MockHttpRequest request, MockHttpResponse response) throws IOException {
        List<String> retainedIds = new ArrayList<>();
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null && parsed.get("item_ids") instanceof List<?> list) {
                    for (var obj : list) {
                        retainedIds.add(String.valueOf(obj));
                    }
                }
            }
            /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (IOException | RuntimeException _) {
                // Ignore parse errors from body
            }
        }
        var updatedList = new ArrayList<Map<String, Object>>();
        synchronized (systemLock) {
            keptItems.removeIf(item -> !retainedIds.contains(item.id()));
            for (var k : keptItems) {
                updatedList.add(k.toMap());
            }
        }
        response.json(200, updatedList);
    }

    /** @return the current runtime lifecycle state */
    public RuntimeLifecycleState lifecycleState() {
        return lifecycleState;
    }

    /** @return active cleanup job or null */
    public CleanupJob activeCleanupJob() {
        synchronized (systemLock) {
            return activeCleanupJob;
        }
    }

    /** @return list of items kept for good */
    public List<CleanupKeptItem> keptItems() {
        synchronized (systemLock) {
            return List.copyOf(keptItems);
        }
    }

    /** @return current cleanup durations */
    public CleanupDurations cleanupDurations() {
        synchronized (systemLock) {
            return cleanupDurations;
        }
    }

    /** @return current cleanup residue figures */
    public List<CleanupResidue> cleanupResidues() {
        synchronized (systemLock) {
            return List.copyOf(cleanupResidues);
        }
    }

    /**
     * Seeds the open decisions from the active scenario's runtime fixture
     * ({@code facts.runtime.decisions}), so a case state shows its own decisions, waivers, and
     * quorum votes instead of a Java literal.
     */
    private void initDefaultDecisions() {
        synchronized (decisionsLock) {
            openDecisions.clear();
            var fixture = startRuntimeFixture();
            for (var entry : fixtureObjects(fixture, FIXTURE_DECISIONS)) {
                var decision = DecisionEnvelope.fromMap(entry);
                openDecisions.put(decision.id(), decision);
            }
        }
    }

    /**
     * Adds an open decision and broadcasts {@code decision.opened}.
     *
     * @param decision the decision envelope
     */
    public void addDecisionAndBroadcast(DecisionEnvelope decision) {
        synchronized (decisionsLock) {
            openDecisions.put(decision.id(), decision);
        }
        broadcastEvent("decision.opened", Map.of(CONST_DECISION, decision.toMap()));
    }

    private static @Nullable String extractPlan(DecisionEnvelope entry) {
        var sub = entry.subject();
        if (sub.startsWith("plan ")) {
            return sub.substring("plan ".length()).trim();
        }
        return null;
    }

    private static String extractProject(DecisionEnvelope entry) {
        var sub = entry.subject();
        if (sub.contains("session-log") || sub.contains("pm-mcp-core")) {
            return "pm-mcp-core";
        }
        if (sub.contains(CONST_PLAN_MARSHALL)) {
            return CONST_PLAN_MARSHALL;
        }
        return "pm-mcp-clients";
    }

    private boolean handleDecisionRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        var method = request.method();

        // 1. List decisions
        if ("GET".equals(method) && ("/api/v1/decisions".equals(path) || "/api/v1/questions".equals(path))) {
            handleListDecisions(request, response, null);
            return true;
        }
        if ("GET".equals(method) && path.startsWith(PATH_PLANS_PREFIX) && path.endsWith("/decisions")) {
            var plan = path.substring(PATH_PLANS_PREFIX.length(), path.length() - "/decisions".length());
            handleListDecisions(request, response, plan);
            return true;
        }

        // 2. Decision specific routes
        String decisionId = null;
        String action = null;
        if (path.startsWith("/api/v1/decisions/")) {
            var sub = path.substring("/api/v1/decisions/".length());
            int slash = sub.indexOf('/');
            if (slash < 0) {
                decisionId = sub;
            } else {
                decisionId = sub.substring(0, slash);
                action = sub.substring(slash + 1);
            }
        } else if (path.startsWith("/api/v1/questions/")) {
            var sub = path.substring("/api/v1/questions/".length());
            int slash = sub.indexOf('/');
            if (slash < 0) {
                decisionId = sub;
            } else {
                decisionId = sub.substring(0, slash);
                action = "answer";
            }
        } else if (path.startsWith(PATH_PLANS_PREFIX) && path.contains(SEGMENT_DECISIONS)) {
            var sub = path.substring(path.indexOf(SEGMENT_DECISIONS) + SEGMENT_DECISIONS.length());
            int slash = sub.indexOf('/');
            if (slash < 0) {
                decisionId = sub;
            } else {
                decisionId = sub.substring(0, slash);
                action = sub.substring(slash + 1);
            }
        }

        if (decisionId != null) {
            if ("GET".equals(method) && action == null) {
                handleGetDecision(response, decisionId);
                return true;
            }
            if (METHOD_POST.equals(method) && ("answer".equals(action) || "answers".equals(action))) {
                handleAnswerDecision(request, response, decisionId, isWeb);
                return true;
            }
            if (METHOD_POST.equals(method) && "interpret".equals(action)) {
                handleInterpretDecision(request, response, decisionId);
                return true;
            }
            if (METHOD_POST.equals(method) && "withdraw".equals(action)) {
                handleWithdrawDecision(request, response, decisionId);
                return true;
            }
        }

        return false;
    }

    private void handleListDecisions(MockHttpRequest request, MockHttpResponse response, @Nullable String planFromPath) throws IOException {
        var query = request.query() != null ? request.query() : "";
        String planFilter = planFromPath;
        String projectFilter = null;
        for (var part : query.split("&")) {
            if (part.startsWith("plan=")) {
                planFilter = part.substring("plan=".length());
            } else if (part.startsWith("project=")) {
                projectFilter = part.substring("project=".length());
            }
        }
        if (".".equals(projectFilter)) {
            projectFilter = "pm-mcp-clients";
        }

        var list = new ArrayList<Map<String, Object>>();
        synchronized (decisionsLock) {
            for (var entry : openDecisions.values()) {
                var itemPlan = extractPlan(entry);
                if (planFilter != null && !planFilter.isBlank() && !planFilter.equals(itemPlan)) {
                    continue;
                }
                if (projectFilter != null && !projectFilter.isBlank()) {
                    var itemProject = extractProject(entry);
                    if (!projectFilter.equals(itemProject)) {
                        continue;
                    }
                }
                var map = new LinkedHashMap<String, Object>(entry.toMap());
                if (itemPlan != null) {
                    map.put("plan", itemPlan);
                }
                map.put("project", extractProject(entry));
                list.add(map);
            }
        }
        response.json(200, list);
    }

    private void handleGetDecision(MockHttpResponse response, String decisionId) throws IOException {
        DecisionEnvelope envelope;
        synchronized (decisionsLock) {
            envelope = openDecisions.get(decisionId);
        }
        if (envelope != null) {
            var map = new LinkedHashMap<String, Object>(envelope.toMap());
            var itemPlan = extractPlan(envelope);
            if (itemPlan != null) {
                map.put("plan", itemPlan);
            }
            map.put("project", extractProject(envelope));
            response.json(200, map);
        } else {
            response.json(404, Map.of(KEY_ERROR, "not_found", KEY_MESSAGE, "Question " + decisionId + " not found."));
        }
    }

    private void handleAnswerDecision(MockHttpRequest request, MockHttpResponse response, String decisionId, boolean isWeb) throws IOException {
        DecisionEnvelope envelope;
        synchronized (decisionsLock) {
            envelope = openDecisions.get(decisionId);
        }
        if (envelope == null) {
            response.json(404, Map.of(KEY_ERROR, "not_found", KEY_MESSAGE, "Question " + decisionId + " not found."));
            return;
        }

        DecisionAnswerRequest answerReq;
        try {
            answerReq = DecisionAnswerRequest.fromJson(request.bodyText());
        } catch (IOException | IllegalArgumentException | IllegalStateException _) {
            answerReq = new DecisionAnswerRequest(Map.of(), null, null, null, null, null, isWeb ? "web" : "cli", null);
        }

        if (envelope.isWaiver()) {
            handleWaiverAnswer(response, decisionId, answerReq);
        } else {
            handleRegularDecisionAnswer(response, decisionId, answerReq);
        }
    }

    private void handleWaiverAnswer(MockHttpResponse response, String decisionId, DecisionAnswerRequest answerReq) throws IOException {
        var action = answerReq.action() != null ? answerReq.action() : (answerReq.option() != null ? answerReq.option() : "waive");
        if ("later".equalsIgnoreCase(action) || "later".equalsIgnoreCase(answerReq.option())) {
            response.json(200, Map.of(KEY_STATUS, "open", "id", decisionId));
            return;
        }
        var reason = answerReq.reason();
        if (reason == null || reason.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, "invalid_answer", KEY_MESSAGE, "A reason is required."));
            return;
        }
        var outcome = "decline".equalsIgnoreCase(action) || "d".equalsIgnoreCase(action) ? "declined" : "waived";
        synchronized (decisionsLock) {
            openDecisions.remove(decisionId);
        }
        broadcastEvent("decision.answered", Map.of(
                "id", decisionId,
                "answer", outcome + " (" + reason + ")",
                "channel", answerReq.channel(),
                "device", answerReq.device() != null ? answerReq.device() : ""
        ));
        broadcastEvent("decision.closed", Map.of(
                "id", decisionId,
                "outcome", outcome,
                "reason", reason
        ));
        response.json(200, Map.of(KEY_STATUS, outcome, "id", decisionId, "reason", reason));
    }

    private void handleRegularDecisionAnswer(MockHttpResponse response, String decisionId, DecisionAnswerRequest answerReq) throws IOException {
        var chosen = answerReq.option();
        if (chosen == null && answerReq.ownWords() != null) {
            chosen = answerReq.ownWords();
        }
        if (chosen == null && !answerReq.partAnswers().isEmpty()) {
            chosen = answerReq.partAnswers().values().iterator().next().toString();
        }
        if ("later".equalsIgnoreCase(answerReq.action()) || "later".equalsIgnoreCase(chosen) || "l".equalsIgnoreCase(chosen)) {
            response.json(200, Map.of(KEY_STATUS, "open", "id", decisionId));
            return;
        }
        if (chosen == null) {
            chosen = "1";
        }

        synchronized (decisionsLock) {
            openDecisions.remove(decisionId);
        }

        var channel = answerReq.channel();
        var device = answerReq.device() != null ? answerReq.device() : "";
        broadcastEvent("decision.answered", Map.of(
                "id", decisionId,
                "answer", chosen,
                "channel", channel,
                "device", device
        ));
        broadcastEvent("decision.closed", Map.of(
                "id", decisionId,
                "outcome", "answered",
                "answer", chosen
        ));
        response.json(200, Map.of(KEY_STATUS, "answered", "id", decisionId, "answer", chosen));
    }

    private void handleInterpretDecision(MockHttpRequest request, MockHttpResponse response, String decisionId) throws IOException {
        String words = "";
        try {
            var map = JsonTree.asObject(JsonTree.parse(request.body()));
            if (map != null) {
                words = JsonTree.string(map, "words");
                if (words == null) words = JsonTree.string(map, "own_words");
            }
        }
        /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (Exception _) {
            // Ignore
        }
        if (words == null) words = "";
        var lower = words.toLowerCase();

        DecisionInterpretation interp;
        if (lower.contains("release/0.9") || lower.contains("provided") || lower.contains("frozen") || lower.contains("untouched")) {
            interp = new DecisionInterpretation(
                    InterpretationType.OPTION_WITH_CONDITION, "2", "release/0.9 stays as it is",
                    "Rebase onto main, provided release/0.9 stays as it is.", null,
                    List.of("rebase", "release/0.9")
            );
        } else if (lower.contains("whatever") || lower.contains("reviewer") || lower.contains("unclear")) {
            interp = new DecisionInterpretation(
                    InterpretationType.UNCLEAR, null, null, null,
                    "Which reviewer do you mean, and what would they do here: merge main into the branch, or something else?",
                    List.of()
            );
        } else if (lower.contains("neither") || lower.contains("squash")) {
            interp = new DecisionInterpretation(
                    InterpretationType.OWN_OPTION, null, null,
                    "your own answer: squash the branch into one commit on main.", null,
                    List.of("squash the branch into one commit on main")
            );
        } else if (lower.contains("leave") || lower.contains("delegate")) {
            interp = new DecisionInterpretation(
                    InterpretationType.DELEGATION, "2", null,
                    "you leave it to the plan", null,
                    List.of("leave it to the plan")
            );
        } else {
            interp = new DecisionInterpretation(
                    InterpretationType.OPTION, "2", null,
                    "Rebase onto main", null,
                    List.of(words)
            );
        }

        response.json(200, interp.toMap());
    }

    private void handleWithdrawDecision(MockHttpRequest request, MockHttpResponse response, String decisionId) throws IOException {
        DecisionEnvelope removed;
        synchronized (decisionsLock) {
            removed = openDecisions.remove(decisionId);
        }
        if (removed == null) {
            response.json(404, Map.of(KEY_ERROR, "not_found", KEY_MESSAGE, "Question " + decisionId + " not found."));
            return;
        }

        broadcastEvent("decision.closed", Map.of(
                "id", decisionId,
                "outcome", "withdrawn",
                "reason", "none_fits"
        ));

        // When Q-007 is withdrawn, ask anew as Q-009 or replacement
        var replacement = new DecisionEnvelope(
                "Q-009", 1, "Resolve rebase conflict (asked anew)", null, removed.subject(),
                removed.scope(), removed.askingPhase(), removed.askingRole(), removed.blocks(),
                removed.reversibility(), "The role implement asked anew after " + decisionId,
                removed.evidence(), removed.parts(), Instant.now(), false, null, null, null
        );
        synchronized (decisionsLock) {
            openDecisions.put("Q-009", replacement);
        }
        broadcastEvent("decision.opened", Map.of(CONST_DECISION, replacement.toMap()));

        response.json(200, Map.of(KEY_STATUS, "withdrawn", "id", decisionId));
    }

    private boolean handleProcessRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if (!"GET".equals(request.method())) {
            return false;
        }

        var isPlanActivity = path.startsWith(PATH_PLANS_PREFIX) && path.contains("/tasks/") && path.endsWith("/activity");
        var isPlanProcess = path.startsWith(PATH_PLANS_PREFIX) && path.endsWith(SUFFIX_PROCESS);
        var isEpicProcess = path.startsWith(PATH_EPICS_PREFIX) && path.endsWith(SUFFIX_PROCESS);

        if (!isPlanActivity && !isPlanProcess && !isEpicProcess) {
            return false;
        }

        // 1. If static route is defined for this exact process path, return it directly
        var route = activeRoute("GET", path);
        if (route != null) {
            response.json(route.status(), route.representation());
            return true;
        }

        // 2. Dynamic plan task activity: /api/v1/plans/{planId}/tasks/{taskId}/activity. A plan the
        // scenario serves either by its own route or by its plan-list entry exists; any other id is
        // plan_not_found here too.
        if (isPlanActivity) {
            var parts = path.split("/");
            if (parts.length >= 7 && "tasks".equals(parts[5])) {
                var planId = parts[4];
                if (!knowsPlan(planId)) {
                    response.json(404, Map.of(KEY_ERROR, ERR_PLAN_NOT_FOUND,
                            KEY_MESSAGE, MESSAGE_PLAN_NOT_FOUND + planId));
                    return true;
                }
                var taskId = parts[6];
                var defaultRoute = activeRoute("GET", "/api/v1/plans/fix-relay/tasks/TASK-003/activity");
                if (defaultRoute != null && defaultRoute.representation() instanceof Map<?, ?> repMap) {
                    var copy = new LinkedHashMap<String, Object>();
                    repMap.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    copy.put("task_id", taskId);
                    response.json(200, copy);
                    return true;
                }
            }
        }

        // 4. Dynamic plan process route: /api/v1/plans/{planId}/process. A plan the scenario serves
        // by its own route is answered at step 1; a listed plan is answered from its summary entry
        // when the template route is active; any other id is plan_not_found.
        if (isPlanProcess) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var planId = sub.substring(0, sub.length() - SUFFIX_PROCESS.length());
            if (!planId.contains("/")) {
                var summary = planSummary(planId);
                if (summary == null) {
                    response.json(404, Map.of(KEY_ERROR, ERR_PLAN_NOT_FOUND,
                            KEY_MESSAGE, MESSAGE_PLAN_NOT_FOUND + planId));
                    return true;
                }
                var defaultRoute = activeRoute("GET", "/api/v1/plans/fix-relay/process");
                var template = defaultRoute != null && defaultRoute.representation() instanceof Map<?, ?> rep
                        ? rep
                        : null;
                response.json(200, synthesizedPlan(template, planId, summary));
                return true;
            }
        }

        // 5. Dynamic epic process route: /api/v1/epics/{epicId}/process
        if (isEpicProcess) {
            var sub = path.substring(PATH_EPICS_PREFIX.length());
            var epicId = sub.substring(0, sub.length() - SUFFIX_PROCESS.length());
            if (!epicId.contains("/")) {
                var defaultRoute = activeRoute("GET", "/api/v1/epics/relay-hardening/process");
                if (defaultRoute != null && defaultRoute.representation() instanceof Map<?, ?> repMap) {
                    var copy = new LinkedHashMap<String, Object>();
                    repMap.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    copy.put("epic_id", epicId);
                    copy.put(KEY_TITLE, epicId);
                    response.json(200, copy);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param planId the requested plan id
     * @return {@code true} when the scenario serves the plan by a non-error route or by its summary
     */
    private boolean knowsPlan(String planId) {
        var processRoute = activeRoute("GET", PATH_PLANS_PREFIX + planId + SUFFIX_PROCESS);
        return (processRoute != null && processRoute.status() < 400) || planSummary(planId) != null;
    }

    /**
     * Looks a plan id up in the plan list the active scenario (or its state) serves, or among the
     * plans a start flow created in this run.
     *
     * @param planId the requested plan id
     * @return the plan's summary entry, or {@code null} when the scenario does not serve the id
     */
    private @Nullable Map<String, Object> planSummary(String planId) {
        var started = startedPlans.get(planId);
        if (started != null) {
            var summary = new LinkedHashMap<String, Object>();
            summary.put("id", planId);
            summary.put(KEY_TITLE, started.title());
            summary.put("project", started.projectId());
            summary.put(KEY_PHASE, PHASE_EXECUTE);
            summary.put("state", "running");
            return summary;
        }
        var listRoute = activeRoute("GET", "/api/v1/plans");
        if (listRoute == null || !(listRoute.representation() instanceof Map<?, ?> list)
                || !(list.get("plans") instanceof List<?> plans)) {
            return null;
        }
        for (var element : plans) {
            var plan = JsonTree.asObject(element);
            if (plan != null && planId.equals(JsonTree.string(plan, "id"))) {
                return plan;
            }
        }
        return null;
    }

    /**
     * @param summary the plan's summary entry
     * @param member  the member to read
     * @param fallback the value to use when the member is absent or blank
     * @return the member's text, or the fallback
     */
    private static @Nullable String summaryText(Map<String, Object> summary, String member,
            @Nullable String fallback) {
        var text = JsonTree.string(summary, member);
        return text != null && !text.isBlank() ? text : fallback;
    }

    /**
     * @param projectId the project id a plan summary names
     * @return the process tree's project reference for it
     */
    private static Map<String, Object> planProjectRef(String projectId) {
        var project = new LinkedHashMap<String, Object>();
        project.put(KEY_PROJECT_ID, projectId);
        project.put("display_name", projectId);
        project.put("link", "/api/v1/projects/" + projectId);
        return project;
    }

    /**
     * Builds the process-tree detail of a plan the scenario lists without a route of its own. The
     * tree carries the plan's own identity, its phase list is the template's, and every phase is
     * marked from the plan's own phase and state; nothing that identifies another plan (its epic,
     * pull request, activity, tokens, duration or project) is copied.
     *
     * @param template the template representation
     * @param planId   the requested plan id
     * @param summary  the plan's summary entry
     * @return the representation to answer
     */
    private static Map<String, Object> synthesizedPlan(@Nullable Map<?, ?> template, String planId,
            Map<String, Object> summary) {
        var phase = summaryText(summary, KEY_PHASE, PHASE_EXECUTE);
        var phaseState = summaryText(summary, "state", "running");
        var copy = new LinkedHashMap<String, Object>();
        copy.put(KEY_PLAN_ID, planId);
        copy.put(KEY_TITLE, summaryText(summary, KEY_TITLE, planId));
        var projectId = summaryText(summary, "project", null);
        if (projectId != null) {
            copy.put("project", planProjectRef(projectId));
        }
        copy.put("current_phase", phase);
        copy.put("phase_state", phaseState);
        if ("waiting_for_you".equals(phaseState)) {
            copy.put("needs_operator", true);
        }
        copy.put("instance_version", 1);
        copy.put("nodes", phaseNodes(template, phase, phaseState));
        copy.put("current_position_path", List.of());
        return copy;
    }

    /**
     * @param template   the template representation
     * @param currentPhase the plan's current phase
     * @param phaseState the plan's state
     * @return the template's phase nodes, each marked done, current or planned
     */
    private static List<Map<String, Object>> phaseNodes(@Nullable Map<?, ?> template, String currentPhase,
            String phaseState) {
        if (template == null || !(template.get("nodes") instanceof List<?> nodes)) {
            var state = "running".equals(phaseState) ? "running" : "waiting_for_operator";
            return List.of(phaseNode(currentPhase, state, "phase " + currentPhase));
        }
        var current = currentPhaseIndex(nodes, currentPhase);
        var result = new ArrayList<Map<String, Object>>(nodes.size() + 1);
        for (var index = 0; index < nodes.size(); index++) {
            var node = JsonTree.asObject(nodes.get(index));
            if (node == null) {
                continue;
            }
            result.add(phaseNode(JsonTree.string(node, KEY_NODE_ID),
                    phaseNodeState(index, current, phaseState),
                    JsonTree.string(node, KEY_TITLE)));
        }
        if (current < 0) {
            // The listed phase is not in the template, so it gets its own node rather than
            // leaving every template node planned while current_phase names another phase.
            result.add(phaseNode(currentPhase, phaseNodeState(0, 0, phaseState),
                    "phase " + currentPhase));
        }
        return result;
    }

    /**
     * @param nodeId the node's id
     * @param state  the node's state
     * @param title  the node's title
     * @return one phase node with no sub-nodes or leaf tasks
     */
    private static Map<String, Object> phaseNode(@Nullable String nodeId, String state,
            @Nullable String title) {
        var ran = "planned".equals(state) ? 0 : 1;
        var phase = new LinkedHashMap<String, Object>();
        phase.put(KEY_NODE_ID, nodeId);
        phase.put(KEY_TITLE, title);
        phase.put("kind", KEY_PHASE);
        phase.put("state", state);
        phase.put("run_count", ran);
        phase.put("current_run", ran);
        phase.put("duration_seconds", 0);
        phase.put("sub_nodes", List.of());
        phase.put("leaf_tasks", List.of());
        return phase;
    }

    /**
     * @param nodes        the template's phase nodes
     * @param currentPhase the plan's current phase
     * @return the index of the current phase, or {@code -1} when the template has no such node
     */
    private static int currentPhaseIndex(List<?> nodes, String currentPhase) {
        for (var index = 0; index < nodes.size(); index++) {
            var node = JsonTree.asObject(nodes.get(index));
            if (node != null && currentPhase.equals(JsonTree.string(node, KEY_NODE_ID))) {
                return index;
            }
        }
        return -1;
    }

    /**
     * @param index      the node's position in the template
     * @param current    the current phase's position, or {@code -1}
     * @param phaseState the plan's state
     * @return the node's state: done before the current phase, the current phase's own, planned after
     */
    private static String phaseNodeState(int index, int current, String phaseState) {
        if (current >= 0 && index < current) {
            return "done";
        }
        if (index == current) {
            return "running".equals(phaseState) ? "running" : "waiting_for_operator";
        }
        return "planned";
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            cancelPendingTasks();
            scheduler.shutdownNow();
            for (var stream : activeStreams.keySet()) {
                try {
                    stream.close();
                } catch (IOException _) {
                    // Ignore error when closing active stream during engine shutdown
                }
            }
            activeStreams.clear();
            readyStreams.clear();
            pendingFrames.clear();
            replayPlans.clear();
        }
    }
}
