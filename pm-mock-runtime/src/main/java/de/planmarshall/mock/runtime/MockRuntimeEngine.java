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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.HoldChangedEvent;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.HoldResolutionRequest;
import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.control.PlanActionRequest;
import de.planmarshall.api.control.PlanControlEvent;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateRequest;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.PlanSettingsValue;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.decision.AnswerType;
import de.planmarshall.api.decision.DecisionAnswerRequest;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.DecisionOption;
import de.planmarshall.api.decision.DecisionPart;
import de.planmarshall.api.decision.DecisionScope;
import de.planmarshall.api.decision.InterpretationType;
import de.planmarshall.api.decision.QuorumBand;
import de.planmarshall.api.decision.QuorumSupport;
import de.planmarshall.api.decision.QuorumVote;
import de.planmarshall.api.decision.Reversibility;
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
import de.planmarshall.api.report.DecisionRecord;
import de.planmarshall.api.report.DeliverableSummary;
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
    private static final String KEY_STATUS = "status";
    private static final String KEY_LOOPBACK = "loopback";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LABEL = "label";
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
    private static final String ERR_PROJECT_NOT_FOUND = "project_not_found";
    private static final String EVENT_PROJECT_CHANGED = "project.changed";
    private static final String KEY_PROJECT_ID = "project_id";
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
    private static final String ERR_LOOPBACK_ONLY = "loopback_only";
    private static final String ERR_WEB_LISTENER_CONFLICT = "web_listener_conflict";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_PAIRED_FROM = "paired_from";
    private static final String FIELD_DEVICE_SECRET = "device_secret";
    private static final String FIELD_CREDENTIAL_KEY = "credential_key";
    private static final String FIELD_ONBOARDED = "onboarded";
    private static final String CONST_LAN_HOST = "mbp-oliver.local";
    private static final String CONST_LAN_FINGERPRINT = "4F:1A:9C:6D:2B:88:71:03:C5:9E:44:AF:20:DB:16:E2:07";
    private static final String CONST_LOOPBACK_URL = "http://127.0.0.1:7420";
    private static final int DEFAULT_LAN_PORT = 7421;
    private static final String KEY_LANDING = "landing";
    private static final String KEY_METHOD = "method";
    private static final String KEY_LINKS = "links";
    private static final String KEY_HREF = "href";
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
    private static final String CONSENT_PREFIX = "consent:";
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
    private static final String CONST_EPIC_RELAY_HARDENING = "relay-hardening";
    private static final String CONST_EPIC_STORAGE_MIGRATION = "storage-migration";
    private static final String CONST_PLAN_RELAY_AUTH_V2 = "relay-auth-v2";
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
    private static final String PATH_EPICS_PREFIX = "/api/v1/epics/";
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
    private final Set<MockEventStream> activeStreams = ConcurrentHashMap.newKeySet();
    private final List<ScheduledFuture<?>> pendingTasks = new ArrayList<>();

    private final AtomicReference<Scenario> currentScenario = new AtomicReference<>();
    private final AtomicReference<String> currentStateId = new AtomicReference<>();
    /**
     * The operator links offered by the states entered so far, as {@code METHOD href} to the state id
     * that offered it. A request that matches no link of the current state but one of these, offered by
     * a state left meanwhile, is an act against a state left meanwhile, answered {@code act_not_available}
     * (409) as the real runtime answers it.
     */
    private final Map<String, String> offeredOperatorLinks = new ConcurrentHashMap<>();
    private final DesignerRegistry designer = new DesignerRegistry();
    private final Object devicesLock = new Object();
    private final List<PairedDevice> pairedDevices = new ArrayList<>();
    private final Object reportsLock = new Object();
    private final List<ReportItem> mockReports = new ArrayList<>();
    private final Map<String, PlanCompletionReport> completionReports = new ConcurrentHashMap<>();
    private final Object decisionsLock = new Object();
    private final Map<String, DecisionEnvelope> openDecisions = new LinkedHashMap<>();
    private final Object systemLock = new Object();
    private volatile RuntimeLifecycleState lifecycleState = RuntimeLifecycleState.RUNNING;
    private volatile String drainingBy;
    private volatile boolean canWithdrawStop;
    private CleanupJob activeCleanupJob;
    private final List<CleanupKeptItem> keptItems = new ArrayList<>();
    private CleanupDurations cleanupDurations;
    private final List<CleanupResidue> cleanupResidues = new ArrayList<>();
    private final Object projectsLock = new Object();
    private final List<EnrolledProject> enrolledProjects = new ArrayList<>();
    private final List<String> favouriteProjectIds = new ArrayList<>();
    private final List<BeingAddedProject> beingAddedProjects = new ArrayList<>();
    private final Object storeLock = new Object();
    private final Map<String, MockStore> mockStores = new LinkedHashMap<>();
    private final Object startLock = new Object();
    private final Set<String> startedPlanIds = new LinkedHashSet<>();
    private final Set<String> startedEpicIds = new LinkedHashSet<>();
    private int intakeDraftCounter;
    private final Object lessonSourceLock = new Object();
    private final Map<String, List<LessonSeed>> lessonSourceSeeds = new LinkedHashMap<>();
    private int gitSourceCounter;
    private final Map<String, String> localChangePreviews = new ConcurrentHashMap<>();
    private final Set<String> capturedProjects = ConcurrentHashMap.newKeySet();
    private volatile boolean lanListenerOn = true;
    private volatile int lanListenerPort = DEFAULT_LAN_PORT;
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
        this.currentScenario.set(scenario);
        offeredOperatorLinks.clear();
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
        designer.init(scenario);
        transitionTo(scenario.start());
    }

    /**
     * Resets the active scenario back to its start state.
     */
    public synchronized void reset() {
        offeredOperatorLinks.clear();
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
        designer.init(currentScenario.get());
        transitionTo(currentScenario.get().start());
    }

    private void initDefaultDevices() {
        synchronized (devicesLock) {
            pairedDevices.clear();
            pairedDevices.add(new PairedDevice("dev-1", "Firefox on mbp-oliver", KEY_LOOPBACK,
                    Instant.parse("2026-05-12T10:00:00Z"), Instant.parse("2026-10-16T14:02:00Z")));
            pairedDevices.add(new PairedDevice("dev-2", "Safari on iPad", "network",
                    Instant.parse("2026-05-20T10:00:00Z"), Instant.parse("2026-10-13T10:00:00Z")));
            pairedDevices.add(new PairedDevice("dev-3", "Chrome on build-box", KEY_LOOPBACK,
                    Instant.parse("2026-06-02T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z")));
            lanListenerOn = true;
            lanListenerPort = DEFAULT_LAN_PORT;
        }
    }

    /**
     * Jumps directly to a state within the current scenario.
     *
     * @param stateId the state identifier
     * @throws IllegalArgumentException if the state does not exist in the current scenario
     */
    public synchronized void goToState(String stateId) {
        var scenario = currentScenario.get();
        if (!scenario.states().containsKey(stateId)) {
            throw new IllegalArgumentException("State '" + stateId + "' does not exist in scenario '" + scenario.name() + "'");
        }
        transitionTo(stateId);
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

    private void broadcastEvent(String event, Object data) {
        var iterator = activeStreams.iterator();
        while (iterator.hasNext()) {
            var stream = iterator.next();
            try {
                stream.sendJson(event, data);
            } catch (IOException _) {
                iterator.remove();
            }
        }
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

        // 2b. An act against a state left meanwhile is refused before any dynamic route serves it.
        if (handleStaleOperatorLink(request, response)) {
            return;
        }

        // 3. Dynamic routes of the domain areas
        if (handleDynamicRoutes(request, response, isWeb)) {
            return;
        }

        // 4. SSE event stream subscription
        if ("GET".equals(request.method()) && ("/api/v1/events".equals(path) || path.endsWith("/events"))) {
            handleEventStream(response);
            return;
        }

        // 5. Match against current state links
        if (handleStateLink(request, response)) {
            return;
        }

        // 6. Match against static routes
        var route = currentScenario.get().route(request.method(), path);
        if (route != null) {
            handleRouteWithDynamicStatus(route, path, response);
            return;
        }

        // 7. No match: answer mock_no_route
        handleNoRoute(request, response);
    }

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
                || handleDecisionRoutes(request, response, isWeb)
                || handleSystemRoutes(request, response, isWeb)
                || handleProcessRoutes(request, response)
                || handleDecompositionRoutes(request, response)
                || handleGitSourceRoutes(request, response)
                || handleStartRoutes(request, response)
                || handleLessonSourceRoutes(request, response)
                || handleControlRoutes(request, response);
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
        if (path.endsWith("/" + linkName)) {
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

    private void handlePairing(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var code = extractPairingCode(request);
        var expectedCode = currentScenario.get().pairingCode();
        if (code == null || !code.replace("-", "").equalsIgnoreCase(expectedCode.replace("-", ""))) {
            response.json(400, Map.of(KEY_ERROR, "invalid_code", KEY_MESSAGE, "The pairing code does not match"));
            return;
        }
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
            }
        }
        var body = new LinkedHashMap<String, Object>();
        body.put(FIELD_DEVICE_SECRET, deviceSecret);
        body.put(KEY_DEVICE_ID, KEY_MOCK_DEVICE_1);
        body.put(KEY_LABEL, label);
        body.put(FIELD_PAIRED_FROM, KEY_LOOPBACK);
        broadcastEvent(EVENT_DEVICE_PAIRED, Map.of(
                KEY_DEVICE_ID, KEY_MOCK_DEVICE_1,
                KEY_LABEL, label,
                FIELD_PAIRED_FROM, KEY_LOOPBACK));
        response.json(200, body);
    }

    private void handleCreatePairing(MockHttpResponse response) throws IOException {
        var code = currentScenario.get().pairingCode();
        if (code == null || code.isBlank()) {
            code = "KXQB-MRTN";
        }
        var url = "http://127.0.0.1:7420/pair#code=" + code;
        response.json(201, Map.of("code", code, "url", url, "valid_until", "10m"));
    }

    private void handleListDevices(MockHttpResponse response) throws IOException {
        var list = new ArrayList<Map<String, Object>>();
        synchronized (devicesLock) {
            for (var dev : pairedDevices) {
                list.add(dev.toMap());
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
            broadcastEvent("device.renamed", Map.of(KEY_DEVICE_ID, updated.deviceId(), KEY_LABEL, updated.label()));
            response.json(200, updated.toMap());
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
                    break;
                }
            }
        }

        if (removed != null) {
            broadcastEvent("device.revoked", Map.of(KEY_DEVICE_ID, removed.deviceId()));
            broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                    KEY_REASON, "device_revoked",
                    KEY_DETAILS, "Revoked",
                    "revoked_by", isWeb ? "another device" : "the terminal client",
                    "revoked_at", "today at 14:10"
            ));
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
                        removedList.add(pairedDevices.remove(i));
                    }
                }
            } else {
                removedList.addAll(pairedDevices);
                pairedDevices.clear();
            }
        }

        for (var dev : removedList) {
            broadcastEvent("device.revoked", Map.of(KEY_DEVICE_ID, dev.deviceId()));
            broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                    KEY_REASON, "device_revoked",
                    KEY_DETAILS, "Revoked",
                    "revoked_by", isWeb ? "another device" : "the terminal client",
                    "revoked_at", "today at 14:10"
            ));
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

    private void handleCloseLanListener(MockHttpResponse response) throws IOException {
        lanListenerOn = false;
        broadcastListenerChanged(false);
        broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                KEY_REASON, "listener_closed",
                KEY_DETAILS, "The network listener was turned off"
        ));
        response.json(200, Map.of("lan", false, KEY_STATUS, "listener_off"));
    }

    private void handleUpdateWebConfig(MockHttpRequest request, MockHttpResponse response) throws IOException {
        boolean previousLan = lanListenerOn;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null && parsed.containsKey("lan")) {
                    lanListenerOn = Boolean.TRUE.equals(parsed.get("lan"));
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore body parsing failures and retain existing listener state
            }
        }
        if (previousLan != lanListenerOn) {
            broadcastListenerChanged(lanListenerOn);
        }
        var resp = new LinkedHashMap<String, Object>();
        resp.put(FIELD_ENABLED, true);
        resp.put("lan", lanListenerOn);
        resp.put("port", 7420);
        resp.put("open", true);
        response.json(200, resp);
    }

    private Map<String, Object> cliOnly(String act) {
        return Map.of(KEY_ERROR, ERR_CLI_ONLY, KEY_MESSAGE, act + " is a CLI-only act");
    }

    private void handleConnectionStatus(MockHttpResponse response) throws IOException {
        var resp = new LinkedHashMap<String, Object>();
        resp.put("loopback_url", CONST_LOOPBACK_URL);
        resp.put("loopback_open", true);
        resp.put("lan_enabled", lanListenerOn);
        if (lanListenerOn) {
            resp.put("lan_url", lanListenerUrl());
            resp.put("lan_fingerprint", CONST_LAN_FINGERPRINT);
        }
        synchronized (devicesLock) {
            resp.put("paired_devices_count", pairedDevices.size());
        }
        resp.put(FIELD_ONBOARDED, isMachineOnboarded());
        response.json(200, resp);
    }

    private void handleOpenLanListener(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var requestedPort = extractLanPort(request);
        if (requestedPort == 7420) {
            response.json(409, Map.of(
                    KEY_ERROR, ERR_WEB_LISTENER_CONFLICT,
                    KEY_MESSAGE, "Port 7420 is held by the loopback listener"));
            return;
        }
        lanListenerPort = requestedPort;
        lanListenerOn = true;
        broadcastListenerChanged(true);
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

    private void handleStoreCredential(MockHttpResponse response, String path, boolean isWeb) throws IOException {
        if (isWeb) {
            response.json(403, Map.of(
                    KEY_ERROR, ERR_LOOPBACK_ONLY,
                    KEY_MESSAGE, "A credential is entered on the loopback listener only"));
            return;
        }
        var key = path.substring(PATH_CREDENTIALS.length());
        response.json(200, Map.of(KEY_STATUS, "stored", FIELD_CREDENTIAL_KEY, key));
    }

    private void broadcastListenerChanged(boolean enabled) {
        var data = new LinkedHashMap<String, Object>();
        data.put("listener", "lan");
        data.put(FIELD_ENABLED, enabled);
        if (enabled) {
            data.put("url", lanListenerUrl());
            data.put("fingerprint", CONST_LAN_FINGERPRINT);
        }
        broadcastEvent(EVENT_LISTENER_CHANGED, data);
    }

    private String lanListenerUrl() {
        return "https://" + CONST_LAN_HOST + ":" + lanListenerPort;
    }

    private boolean isMachineOnboarded() {
        var route = currentScenario.get().route("GET", "/api/v1/status");
        if (route == null || !(route.representation() instanceof Map<?, ?> map)) {
            return true;
        }
        if (Boolean.TRUE.equals(map.get(FIELD_ONBOARDED))) {
            return true;
        }
        if (map.get("onboard") instanceof Map<?, ?> onboard
                && ("onboard.completed".equals(onboard.get(KEY_STATE))
                || Boolean.TRUE.equals(onboard.get("completed")))) {
            return true;
        }
        return map.get("machine") instanceof Map<?, ?> machine && Boolean.TRUE.equals(machine.get(FIELD_ONBOARDED));
    }

    private boolean handleDeviceRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        return switch (request.method()) {
            case "GET" -> handleGetDeviceRoutes(path, response);
            case "POST" -> handlePostDeviceRoutes(request, response, path, isWeb);
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
            resp.put("lan", lanListenerOn);
            resp.put("port", 7420);
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
            if (isWeb) {
                response.json(403, cliOnly("Opening the network listener"));
                return true;
            }
            handleOpenLanListener(request, response);
            return true;
        }
        if (path.startsWith(PATH_CREDENTIALS)) {
            handleStoreCredential(response, path, isWeb);
            return true;
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
            if (!"POST".equals(method)) {
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

            controlPlanStates.put(CONST_FIX_RELAY, "running");
            controlPlanEpics.put(CONST_FIX_RELAY, CONST_RELAY_HARDENING);
            controlPlanStates.put("relay-cursor-order", "held");
            controlPlanEpics.put("relay-cursor-order", CONST_RELAY_HARDENING);
            controlPlanStates.put(CONST_RELAY_AUTH_V2, "waiting");
            controlPlanEpics.put(CONST_RELAY_AUTH_V2, CONST_RELAY_HARDENING);
            controlPlanStates.put("relay-docs", "held");
            controlPlanEpics.put("relay-docs", CONST_RELAY_HARDENING);
            controlPlanStates.put("relay-metrics", "paused");
            controlPlanEpics.put("relay-metrics", CONST_RELAY_HARDENING);
            controlPlanStates.put(CONST_DOCS_TYPO, "ended");

            controlEpicStates.put(CONST_RELAY_HARDENING, "running");
            controlEpicAuto.put(CONST_RELAY_HARDENING, true);
            controlEpicScope.put(CONST_RELAY_HARDENING, 3);
            controlEpicRunning.put(CONST_RELAY_HARDENING, 3);
            controlEpicOverlap.put(CONST_RELAY_HARDENING, 0);
            controlEpicStates.put("relay-docs-epic", "landing");

            controlHolds.put(holdKey("relay-cursor-order", "H-0001"), new HoldRecord(
                    "H-0001", "relay-cursor-order", "plan", "integrity_pause",
                    "the plan's store file references.json changed outside the runtime",
                    0, null, "references.json", 4812L, "3f9a6b2cc21e", 5034L, "b70d41aa9a44",
                    List.of("retry", "accept", "abort"), null));
            controlHolds.put(holdKey("relay-docs", "H-0002"), new HoldRecord(
                    "H-0002", "relay-docs", "plan", "held",
                    "waits: overlap with relay-auth-v2 (2 files)",
                    1, Instant.parse(CONST_TIME_10_14), null, null, null, null, null,
                    List.of("retry", "end_plan"), null));

            controlSettingsRevisions.put(CONST_FIX_RELAY, 7);
            var values = new LinkedHashMap<String, PlanSettingsValue>();
            values.put("landing", new PlanSettingsValue("landing", "landing.pull-request", "project",
                    List.of("landing.pull-request", "landing.main", CONST_LANDING_FILES)));
            values.put(KEY_METHOD, new PlanSettingsValue(KEY_METHOD, "method.test-first", "machine",
                    List.of("method.test-first", "method.tests-after")));
            values.put(KEY_ASKING, new PlanSettingsValue(KEY_ASKING, "ask.key-points", "plan",
                    List.of("ask.every-phase", "ask.key-points", "ask.questions-only")));
            values.put(KEY_POSTURE, new PlanSettingsValue(KEY_POSTURE, CONST_STANDARD, "plan", List.of()));
            values.put("base", new PlanSettingsValue("base", "main", "plan", List.of()));
            values.put("role.implement", new PlanSettingsValue("role.implement",
                    "Claude Code - sonnet-5.5", "plan", List.of()));
            values.put("role.review", new PlanSettingsValue("role.review",
                    "Codex - gpt-5.3", "project", List.of()));
            values.put("integration.git", new PlanSettingsValue("integration.git", "native", "project", List.of()));
            values.put("provider.issues", new PlanSettingsValue("provider.issues", "GitHub", "project", List.of()));
            controlSettings.put(CONST_FIX_RELAY, values);
        }
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
        if (!"POST".equals(request.method())) {
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

    private void initDefaultDecomposition() {
        synchronized (decompositionLock) {
            decompositions.clear();
            decompositions.put(CONST_EPIC_RELAY_HARDENING, relayHardeningCut());
            decompositions.put(CONST_EPIC_STORAGE_MIGRATION, storageMigrationCut());
        }
    }

    private static EpicDecomposition relayHardeningCut() {
        var reconnectBackoff = new StagedPlanRecord("relay-reconnect-backoff", "relay-reconnect-backoff",
                "reconnect the relay with backoff", List.of("pm-relay/src/reconnect"), List.of(),
                StagedPlanRecord.STATUS_STAGED);
        var cursorOrder = new StagedPlanRecord("relay-cursor-order", "relay-cursor-order",
                "order the cursors across the nodes",
                List.of("pm-relay/src/cursor", "pm-api/src/cursor"), List.of("relay-reconnect-backoff"),
                StagedPlanRecord.STATUS_STAGED);
        var authV2 = new StagedPlanRecord(CONST_PLAN_RELAY_AUTH_V2, CONST_PLAN_RELAY_AUTH_V2,
                "a token exchange the relay renews by itself", List.of("pm-api/src/cursor"), List.of(),
                StagedPlanRecord.STATUS_STAGED);
        var tokenRotation = new StagedPlanRecord("relay-token-rotation", "relay-token-rotation",
                "rotate the relay token", List.of("pm-relay/src/token"), List.of(CONST_PLAN_RELAY_AUTH_V2),
                StagedPlanRecord.STATUS_STAGED);
        return new EpicDecomposition(CONST_EPIC_RELAY_HARDENING, DecompositionState.DRAFT, 3,
                List.of(new WorkstreamRecord("WS-1", "Reconnect", "the reconnect work", 1,
                                List.of(reconnectBackoff, cursorOrder)),
                        new WorkstreamRecord("WS-2", "Authentication", "the token work", 2,
                                List.of(authV2, tokenRotation))),
                List.of());
    }

    private static EpicDecomposition storageMigrationCut() {
        var schema = new StagedPlanRecord("storage-schema", "storage-schema", "the new schema",
                List.of("pm-api/src/store"), List.of("storage-migrate"), StagedPlanRecord.STATUS_STAGED);
        var migrate = new StagedPlanRecord("storage-migrate", "storage-migrate", "migrate the store",
                List.of("pm-relay/src/store"), List.of("storage-schema"), StagedPlanRecord.STATUS_STAGED);
        return new EpicDecomposition(CONST_EPIC_STORAGE_MIGRATION, DecompositionState.DRAFT, 1,
                List.of(new WorkstreamRecord("WS-1", "Storage", "the store work", 1, List.of(schema, migrate))),
                List.of());
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
                if (!"POST".equals(request.method())) {
                    return false;
                }
                handleCheckDecomposition(response, projectId, epicId);
            }
            case "instruct" -> {
                if (!"POST".equals(request.method())) {
                    return false;
                }
                handleInstructDecomposition(request, response, projectId, epicId);
            }
            case "commit" -> {
                if (!"POST".equals(request.method())) {
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

    private boolean handleStartRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("GET".equals(request.method()) && PATH_RECIPES.equals(path)) {
            handleListRecipes(response);
            return true;
        }
        if ("POST".equals(request.method()) && PATH_INTAKE_DRAFT.equals(path)) {
            handleIntakeDraft(request, response);
            return true;
        }
        if ("POST".equals(request.method()) && path.startsWith(PREFIX_PROJECTS)) {
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
        response.json(200, Map.of(KEY_RECIPES, defaultRecipes().stream().map(RecipeRef::toMap).toList()));
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
        if ("POST".equals(method) && path.endsWith(SUFFIX_PR_OBSERVE)) {
            handlePrObserve(request, response, projectIdOf(path, SUFFIX_PR_OBSERVE));
            return true;
        }
        if ("POST".equals(method) && path.endsWith(SUFFIX_PR_ADOPT)) {
            handlePrAdopt(request, response, projectIdOf(path, SUFFIX_PR_ADOPT));
            return true;
        }
        if ("GET".equals(method) && path.endsWith(SUFFIX_LOCAL_PREVIEW)) {
            handleLocalChangesPreview(response, projectIdOf(path, SUFFIX_LOCAL_PREVIEW));
            return true;
        }
        if ("POST".equals(method) && path.endsWith(SUFFIX_LOCAL_CAPTURE)) {
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
            while (startedPlanIds.contains(candidate)) {
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
                actions.add(ActionAvailability.enabled("start"));
                actions.add(ActionAvailability.enabled("cancel"));
                addChatAndSettings(planId, actions);
            }
            case "paused" -> {
                actions.add(ActionAvailability.enabled("start"));
                actions.add(ActionAvailability.enabled("abandon"));
                addChatAndSettings(planId, actions);
            }
            case "ended" -> actions.add(ActionAvailability.enabled("chat"));
            default -> {
                actions.add(ActionAvailability.enabled("pause"));
                actions.add(ActionAvailability.enabled("cancel"));
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
            actions.add(ActionAvailability.enabled("next"));
            actions.add(ActionAvailability.enabled("auto"));
            actions.add(ActionAvailability.enabled("common_files"));
            actions.add(ActionAvailability.enabled("decompose"));
            actions.add(ActionAvailability.enabled(FIELD_FINDINGS));
            actions.add(ActionAvailability.enabled("watches"));
            actions.add(ActionAvailability.enabled("chat"));
            if ("landing".equals(state)) {
                actions.add(ActionAvailability.disabled("archive", "the ledger is landing"));
            } else {
                actions.add(ActionAvailability.enabled("archive"));
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

        if ("GET".equals(method) && path.endsWith("/actions")) {
            if (isPlan) {
                handleGetPlanActions(response, controlSubjectId(path, PATH_PLANS_PREFIX, "/actions"));
            } else {
                handleGetEpicActions(response, controlSubjectId(path, PATH_EPICS_PREFIX, "/actions"));
            }
            return true;
        }

        if ("POST".equals(method) && path.contains("/actions/")) {
            var prefix = isPlan ? PATH_PLANS_PREFIX : PATH_EPICS_PREFIX;
            var sub = path.substring(prefix.length());
            var marker = sub.indexOf("/actions/");
            if (marker < 0) {
                return false;
            }
            var subjectId = sub.substring(0, marker);
            var action = sub.substring(marker + "/actions/".length());
            if (action.contains("/")) {
                return false;
            }
            if (isPlan) {
                handlePlanAction(request, response, subjectId, action);
            } else {
                handleEpicAction(request, response, subjectId, action);
            }
            return true;
        }

        if (isPlan && "GET".equals(method) && path.endsWith("/holds")) {
            handleListHolds(response, controlSubjectId(path, PATH_PLANS_PREFIX, "/holds"));
            return true;
        }

        if (isPlan && "POST".equals(method) && path.contains("/holds/") && path.endsWith("/resolve")) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var marker = sub.indexOf("/holds/");
            var subjectId = sub.substring(0, marker);
            var holdId = sub.substring(marker + "/holds/".length(), sub.length() - "/resolve".length());
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

        return false;
    }

    private Map<String, Object> readBodyMap(MockHttpRequest request) {
        if (request.body() == null || request.body().length == 0) {
            return new LinkedHashMap<>();
        }
        try {
            var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
            return parsed != null ? parsed : new LinkedHashMap<>();
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException _) {
            return new LinkedHashMap<>();
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
                response.json(404, Map.of(KEY_ERROR, "plan_not_found", KEY_MESSAGE, "Plan not found: " + planId));
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
                case "pause" -> {
                    var point = actionRequest.point() != null ? actionRequest.point() : "task";
                    controlPlanStates.put(planId, "pausing");
                    controlPlanPausePoints.put(planId, point);
                    broadcastEvent(PlanControlEvent.EVENT_PAUSED, new PlanControlEvent(
                            PlanControlEvent.EVENT_PAUSED, planId, "pausing", point, null).toMap());
                }
                case "start" -> {
                    controlPlanStates.put(planId, "running");
                    controlPlanPausePoints.remove(planId);
                    broadcastEvent(PlanControlEvent.EVENT_RESUMED, new PlanControlEvent(
                            PlanControlEvent.EVENT_RESUMED, planId, "running", null, null).toMap());
                }
                case "cancel", "abandon" -> {
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
                case "auto" -> {
                    var enabled = actionRequest.extra().get(FIELD_ENABLED);
                    if (enabled instanceof Boolean flag) {
                        controlEpicAuto.put(epicId, flag);
                    } else {
                        var mode = actionRequest.extra().get("mode");
                        if (mode != null) {
                            controlEpicAuto.put(epicId, "on".equalsIgnoreCase(String.valueOf(mode)));
                        }
                    }
                }
                case "common_files" -> {
                    var value = actionRequest.extra().get("count");
                    if (value instanceof Number number) {
                        controlEpicOverlap.put(epicId, number.intValue());
                    }
                }
                case "cancel" -> controlEpicStates.put(epicId, "ended");
                default -> {
                    // next, decompose, findings, watches, archive, chat: the epic rests between its events
                }
            }
            var resp = new LinkedHashMap<String, Object>();
            resp.put(KEY_STATUS, "ok");
            resp.put("auto_enabled", controlEpicAuto.getOrDefault(epicId, true));
            resp.put("overlap_max_files", controlEpicOverlap.getOrDefault(epicId, 0));
            resp.putAll(buildEpicActions(epicId).toMap());
            response.json(200, resp);
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
            var ending = "abort".equals(act) || "end_plan".equals(act) || "cancel_epic".equals(act);
            var released = "accept".equals(act) || "retry".equals(act) || "grant".equals(act)
                    || "add_credential".equals(act) || "raise_budget".equals(act)
                    || "give_up_removal".equals(act) || "recycle".equals(act);
            if ("integrity_pause".equals(hold.kind()) && "retry".equals(act)) {
                released = false;
            }
            HoldResolutionResult result;
            if (ending) {
                controlHolds.remove(holdKey(planId, holdId));
                controlPlanStates.put(planId, "ended");
                controlPlanPausePoints.remove(planId);
                broadcastEvent(HoldChangedEvent.EVENT_CLEARED, new HoldChangedEvent(HoldChangedEvent.EVENT_CLEARED,
                        hold.holdId(), hold.subjectId(), hold.subjectKind(), null, null).toMap());
                broadcastEvent(PlanControlEvent.EVENT_CANCELLED, new PlanControlEvent(
                        PlanControlEvent.EVENT_CANCELLED, planId, "ended", null,
                        resolution.reason() != null ? resolution.reason()
                                : ("abort".equals(act) ? "Integrity pause aborted" : "Ended by operator")).toMap());
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

    private void initDefaultReports() {
        synchronized (reportsLock) {
            mockReports.clear();
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
        }

        completionReports.clear();
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
        var fixRelayComponents = new TokenComponentRecord(310000L, 62000L, 8000L, 29000L, 3000L);
        var fixRelayPhase = List.of(
                new TokenBreakdownItem("refine", 38000, 35000, 3000, 0, false, null, null),
                new TokenBreakdownItem("outline", 96000, 90000, 6000, 0, false, null, null),
                new TokenBreakdownItem("execute", 241000, 222000, 19000, 3, true, null, fixRelayComponents),
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
                fixRelayTokens
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

    private boolean handleReportRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        var method = request.method();

        if ("GET".equals(method) && ("/api/v1/reports".equals(path) || PATH_REPORTS_PREFIX.equals(path))) {
            handleListReports(request, response);
            return true;
        }
        if ("POST".equals(method) && "/api/v1/reports/read".equals(path)) {
            handleMarkReportsRead(request, response, isWeb);
            return true;
        }
        if (path.startsWith(PATH_REPORTS_PREFIX)) {
            var sub = path.substring(PATH_REPORTS_PREFIX.length());
            if (sub.endsWith("/read") && "POST".equals(method)) {
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
        response.json(404, Map.of(KEY_ERROR, "plan_not_found", KEY_MESSAGE, "Completion report not found for plan: " + planId));
    }

    private void handleRouteWithDynamicStatus(RouteDefinition route, String path, MockHttpResponse response) throws IOException {
        var rep = route.representation();
        if ("/api/v1/status".equals(path) && rep instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                if ("web".equals(entry.getKey()) && entry.getValue() instanceof Map<?, ?> webMap) {
                    var webCopy = new LinkedHashMap<String, Object>();
                    for (var w : webMap.entrySet()) {
                        webCopy.put(String.valueOf(w.getKey()), w.getValue());
                    }
                    webCopy.put("lan", lanListenerOn);
                    copy.put("web", webCopy);
                } else {
                    copy.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            copy.put("lifecycle_state", lifecycleState.name().toLowerCase());
            if (drainingBy != null) {
                copy.put("draining_by", drainingBy);
                copy.put("can_withdraw", canWithdrawStop);
            }
            response.json(route.status(), copy);
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
            var route = currentScenario.get().route("GET", path);
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

    private void handleEventStream(MockHttpResponse response) throws IOException {
        var stream = response.events(200);
        activeStreams.add(stream);
        try {
            stream.sendComment("connected to mock runtime");
        } catch (IOException _) {
            activeStreams.remove(stream);
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
        activeStreams.remove(stream);
    }

    private void handleTaskActivityEventStream(MockHttpResponse response) throws IOException {
        var stream = response.events(200);
        activeStreams.add(stream);
        try {
            stream.sendComment("connected to mock runtime");
            stream.sendJson("task.activity", Map.of(
                    "kind", "assistant_text",
                    "text", "Analyzing module dependencies...",
                    "tokens", 450,
                    "timestamp", Instant.now().toString()
            ));
        } catch (IOException _) {
            activeStreams.remove(stream);
            return;
        }

        while (!closed && !stream.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        activeStreams.remove(stream);
    }

    private void handleControlRequest(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("/mock/state".equals(path) && "GET".equals(request.method())) {
            handleControlState(response);
            return;
        }
        if ("/mock/reset".equals(path) && "POST".equals(request.method())) {
            handleControlReset(response);
            return;
        }
        if ("/mock/scenario".equals(path) && "POST".equals(request.method())) {
            handleControlScenario(request, response);
            return;
        }
        if (path.startsWith("/mock/goto/") && "POST".equals(request.method())) {
            handleControlGoto(path.substring("/mock/goto/".length()), response);
            return;
        }
        if ("/mock/broadcast".equals(path) && "POST".equals(request.method())) {
            handleControlBroadcast(request, response);
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
            var code = this.currentScenario.get().pairingCode();
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
        if ("/api/v1/onboarding/step".equals(path) && "POST".equals(method)) {
            handleOnboardingStep(request, response);
            return true;
        }
        if ("/api/v1/onboarding/auto".equals(path) && "POST".equals(method)) {
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

    private Map<String, Object> mergeRepresentation(@Nullable StateDefinition state, @Nullable String stateId) {
        var body = new LinkedHashMap<String, Object>();
        if (state != null && state.representation() instanceof Map<?, ?> rep) {
            for (var entry : rep.entrySet()) {
                body.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        if (state != null && !state.links().isEmpty()) {
            var links = new LinkedHashMap<String, Object>();
            for (var entry : state.links().entrySet()) {
                var link = entry.getValue();
                var target = new LinkedHashMap<String, Object>();
                target.put(KEY_HREF, link.href() != null ? link.href() : "/api/v1/onboarding/step");
                target.put(KEY_METHOD, link.method());
                target.put("next", link.next());
                links.put(entry.getKey(), target);
            }
            body.put(KEY_LINKS, links);
        }
        body.put(KEY_STATE, stateId);
        body.put("completed_steps", completedStepsFor(stateId));
        body.put("auto", onboardingAuto);
        return body;
    }

    private static int completedStepsFor(@Nullable String stateId) {
        if (stateId == null) {
            return 0;
        }
        if ("onboard.completed".equals(stateId) || stateId.startsWith("settings.")) {
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
                goToState("auto.consent");
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
        if ("PUT".equals(request.method()) || "POST".equals(request.method())) {
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
        map.put("summary", summary);
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
            cleanupResidues.add(new CleanupResidue("temporary_files", 214, 356515840L, "today 03:00"));
            cleanupResidues.add(new CleanupResidue("worker_directories", 18, 1288490188L, "today 03:00"));
            cleanupResidues.add(new CleanupResidue("stale_locks", 3, 0L, "today 03:00"));
            cleanupResidues.add(new CleanupResidue("old_workspace_rows", 612, 0L, "today 03:00"));
            cleanupResidues.add(new CleanupResidue("retired_lessons", 4, 0L, "today 03:00"));

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
                    12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00"
            );

            var links = Map.of(
                    "delete", "/api/v1/system/cleanup/cln-0012/delete",
                    "remind", "/api/v1/system/cleanup/cln-0012/remind",
                    "ignore", "/api/v1/system/cleanup/cln-0012/ignore"
            );

            activeCleanupJob = new CleanupJob(
                    "cln-0012",
                    CleanupJobState.OPEN,
                    "today 03:00",
                    summary,
                    projectItems,
                    links
            );
        }
    }

    private void initDefaultProjects() {
        synchronized (projectsLock) {
            enrolledProjects.clear();
            favouriteProjectIds.clear();
            beingAddedProjects.clear();

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

        if ("POST".equals(method) && PATH_ENROL.equals(path)) {
            handleEnrol(request, response, null);
            return true;
        }
        if ("POST".equals(method) && path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_ENROL)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(), path.length() - SEGMENT_ENROL.length());
            handleEnrol(request, response, prjId);
            return true;
        }
        if ("GET".equals(method) && path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_STORES_DIGEST)) {
            var prjId = path.substring(PATH_PROJECTS_PREFIX.length(), path.length() - SEGMENT_STORES_DIGEST.length());
            handleStoreDigest(request, response, prjId);
            return true;
        }
        if ("POST".equals(method) && path.startsWith(PATH_PROJECTS_PREFIX) && path.endsWith(SEGMENT_STORES_ACCEPT_EDIT)) {
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
            case "POST" -> handleProjectExplorerPost(request, response, path);
            case METHOD_DELETE -> handleProjectExplorerDelete(response, path);
            default -> false;
        };
    }

    private boolean handleProjectExplorerGet(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        if ("/api/v1/explorer".equals(path)
                || (PATH_PROJECTS.equals(path)
                && ((request.query() != null && request.query().contains("all"))
                || StandardScenarios.PROJECTS.equals(currentScenario.get().name())))) {
            handleGetProjectsExplorer(response);
            return true;
        }
        if (StandardScenarios.PROJECTS.equals(currentScenario.get().name())
                && path.startsWith("/api/v1/projects/")
                && !path.endsWith("/ended")
                && !path.endsWith("/events")) {
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
        if (path.startsWith("/api/v1/projects/") && path.endsWith("/setup")) {
            var prjId = path.substring("/api/v1/projects/".length(), path.length() - "/setup".length());
            handleSetupProject(response, prjId);
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
        if (path.startsWith("/api/v1/entries/") && path.endsWith(SUFFIX_ABANDON)) {
            var entId = path.substring("/api/v1/entries/".length(), path.length() - SUFFIX_ABANDON.length());
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

    private void handleSetupProject(MockHttpResponse response, String projectId) throws IOException {
        response.json(200, Map.of(KEY_STATUS, "setup_started", KEY_PROJECT_ID, projectId));
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
        if ("POST".equals(method) && ("/api/v1/operator/runtime/stop".equals(path) || "/api/v1/runtime/stop".equals(path))) {
            handlePostStopRuntime(request, response, isWeb);
            return true;
        }
        if (METHOD_DELETE.equals(method) && ("/api/v1/operator/runtime/stop".equals(path) || "/api/v1/runtime/stop".equals(path))) {
            handleDeleteStopRuntime(response);
            return true;
        }
        if ("POST".equals(method) && "/api/v1/operator/runtime/upgrade".equals(path)) {
            handlePostUpgradeRuntime(request, response);
            return true;
        }
        if (METHOD_DELETE.equals(method) && "/api/v1/operator/runtime/upgrade".equals(path)) {
            handleDeleteUpgradeRuntime(response);
            return true;
        }
        if ("POST".equals(method) && "/api/v1/operator/runtime/upgrade/check".equals(path)) {
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
        if (path.startsWith("/api/v1/system/cleanup/") && "POST".equals(method)) {
            var sub = path.substring("/api/v1/system/cleanup/".length());
            if (sub.endsWith("/delete")) {
                var jobId = sub.substring(0, sub.length() - "/delete".length());
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
                "127.0.0.1:7420",
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
        synchronized (systemLock) {
            hasOpenJob = activeCleanupJob != null && activeCleanupJob.isOpen();
        }
        var cleanupSummary = new CleanupSummary(
                hasOpenJob ? 12 : 0,
                hasOpenJob ? 2 : 0,
                hasOpenJob ? 40 : 0,
                hasOpenJob ? 5 : 0,
                hasOpenJob ? 1 : 0,
                hasOpenJob ? 1932735283L : 0L,
                keptItems.size(),
                "today 03:00",
                "tomorrow 03:00"
        );

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
                    var v = JsonTree.string(parsed, "version");
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
                    "version", targetVersion,
                    "step", "preparing"
            ));
            response.json(200, Map.of(
                    KEY_STATUS, "upgrading",
                    KEY_STATE, "upgrading",
                    "version", targetVersion,
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
                "version", "0.10.0",
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
                resp.put("next_check_at", "tomorrow 03:00");
                resp.put("kept_count", keptItems.size());
                resp.put("durations", cleanupDurations.toMap());
                resp.put("residue", cleanupResidues.stream().map(CleanupResidue::toMap).toList());
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
                response.json(200, Map.of(
                        KEY_STATUS, "deleted",
                        "job_id", jobId,
                        CONST_DECISION, "delete",
                        "freed_bytes", 1825361100L,
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
                keptItems.add(new CleanupKeptItem(un, "item", un, "left out of a delete", "today", isWeb ? "the web app" : "the console"));
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
                    CONST_DECISION, "delete",
                    "by", "operator",
                    "channel", isWeb ? "web" : "cli",
                    "device", isWeb ? "iPad" : KEY_TERMINAL,
                    "freed_bytes", 1825361100L
            ));
            broadcastEvent("cleanup.done", Map.of(
                    "job_id", jobId,
                    "freed_bytes", 1825361100L,
                    "deleted_plans", 11,
                    "deleted_epics", 2,
                    "deleted_logs", 29
            ));

            response.json(200, Map.of(
                    KEY_STATUS, "deleted",
                    "job_id", jobId,
                    CONST_DECISION, "delete",
                    "freed_bytes", 1825361100L,
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

    private void initDefaultDecisions() {
        synchronized (decisionsLock) {
            openDecisions.clear();

            // Q-007 (fix-relay)
            var q007VotesOpt2 = List.of(
                    new QuorumVote("implement", "claude-3.7-sonnet", 88, "Keeps git history linear"),
                    new QuorumVote("architecture", "gemini-2.5-pro", 85, "Standard clean rebase")
            );
            var q007SupportOpt2 = new QuorumSupport(2, 3, 2, QuorumBand.CLEAR_MAJORITY,
                    "the branch was pushed; a rebase rewrites what others pulled.", q007VotesOpt2);
            var q007VotesOpt1 = List.of(
                    new QuorumVote("quorum-b", "gpt-4o", 70, "The branch was pushed; a rebase rewrites what others pulled.")
            );
            var q007SupportOpt1 = new QuorumSupport(1, 3, 1, QuorumBand.CLEAR_MAJORITY, null, q007VotesOpt1);

            var q007Opt1 = new DecisionOption("1", "Merge main into the branch", "Preserves commit SHAs", null, null, q007SupportOpt1);
            var q007Opt2 = new DecisionOption("2", "Rebase onto main", "Keeps history linear", null, null, q007SupportOpt2);
            var q007Opt3 = new DecisionOption("3", "Stop the plan and ask me later", "Pauses execution", null, null, null);

            var q007Part = new DecisionPart("1", "How should the conflict be resolved?", AnswerType.ONE_OPTION,
                    List.of(q007Opt1, q007Opt2, q007Opt3), "2", "Rebase onto main. Keeps the history linear.",
                    "implement", null, true, true);

            var q007 = new DecisionEnvelope(
                    "Q-007", 1, "Resolve rebase conflict", "Conflict", "plan fix-relay",
                    DecisionScope.PLAN, "execute", "implement", "plan", Reversibility.REVERSIBLE,
                    "The change conflicts with main in RelayServer.java.",
                    Map.of("file", "RelayServer.java", "lines", "42-58", "summary", "both versions of the method"),
                    List.of(q007Part), Instant.parse("2026-10-16T14:02:00Z"),
                    false, null, null, null
            );
            openDecisions.put("Q-007", q007);

            // Q-009 (drop-legacy-api)
            var q009Opt1 = new DecisionOption("1", "Delete them now", null, "Breaking change for existing v1 consumers", null,
                    new QuorumSupport(1, 3, 1, QuorumBand.SPLIT, "two clients of v1 are known; both migrated last week.",
                            List.of(new QuorumVote("quorum-a", "model-a", 65, "Fast cleanup"))));
            var q009Opt2 = new DecisionOption("2", "Deprecate them, delete in 0.11", null, "Deprecation header on v1 responses", null,
                    new QuorumSupport(1, 3, 1, QuorumBand.SPLIT, null,
                            List.of(new QuorumVote("architecture", "model-b", 80, "Graceful migration"))));
            var q009Opt3 = new DecisionOption("3", "Keep them", null, "Maintains legacy code indefinitely", null,
                    new QuorumSupport(1, 3, 1, QuorumBand.SPLIT, null,
                            List.of(new QuorumVote("quorum-c", "model-c", 60, "Zero disruption"))));

            var q009Part = new DecisionPart("1", "Delete the published v1 endpoints from the release branch?", AnswerType.ONE_OPTION,
                    List.of(q009Opt1, q009Opt2, q009Opt3), "2", "Deprecate them, delete in 0.11.",
                    "architecture", null, true, false);

            var q009 = new DecisionEnvelope(
                    "Q-009", 1, "Delete published v1 endpoints?", null, "plan drop-legacy-api",
                    DecisionScope.PLAN, "finalize", "architecture", "plan", Reversibility.IRREVERSIBLE,
                    "Delete the published v1 endpoints from the release branch?",
                    Map.of("endpoints", "/api/v1/legacy, /api/v1/old"),
                    List.of(q009Part), Instant.parse("2026-10-16T14:15:00Z"),
                    false, null, null, null
            );
            openDecisions.put("Q-009", q009);

            // Q-012 (storage-v2) - Multi-part
            var q012Part1 = new DecisionPart("1", "Where does the data live?", AnswerType.ONE_OPTION,
                    List.of(new DecisionOption("1", "A file per plan", "Isolated files", null, null, null),
                            new DecisionOption("2", "One database for the machine", "Central storage", null, null, null)),
                    "1", "A file per plan. Simple isolation.", "outline", null, true, false);
            var q012Part2 = new DecisionPart("2", "How is it migrated?", AnswerType.ONE_OPTION,
                    List.of(new DecisionOption("1", "At the first start of the new release", "Automatic migration", null, null, null),
                            new DecisionOption("2", "By a verb the operator runs", "Manual control", null, null, null)),
                    "1", "Automated transition.", "outline", "depends on part 1: A file per plan", true, false);
            var q012Part3 = new DecisionPart("3", "Which format?", AnswerType.ONE_OPTION,
                    List.of(new DecisionOption("1", "JSON lines", "Append-only text", null, null, null),
                            new DecisionOption("2", "SQLite per plan", "Structured queries", null, null, null)),
                    "1", "JSON lines matches repository standards.", "outline", null, true, false);

            var q012 = new DecisionEnvelope(
                    "Q-012", 1, "How should the new store be introduced?", null, "plan storage-v2",
                    DecisionScope.PLAN, "outline", "outline", "plan", Reversibility.REVERSIBLE,
                    "How should the new store be introduced?",
                    Map.of("measurements", "current store size: 12MB"),
                    List.of(q012Part1, q012Part2, q012Part3), Instant.parse("2026-10-16T14:30:00Z"),
                    false, null, null, null
            );
            openDecisions.put("Q-012", q012);

            // Q-031 (setup of pm-mcp-clients)
            var q031Part = new DecisionPart("1", "Should the generated sources of pm-api be described as a module of their own?", AnswerType.ONE_OPTION,
                    List.of(new DecisionOption("1", "yes", "Dedicated module entry", null, null, null),
                            new DecisionOption("2", "no, with pm-api", "Keep bundled with pm-api", null, null, null)),
                    "2", "no, with pm-api", "enrich", null, true, true);

            var q031 = new DecisionEnvelope(
                    "Q-031", 1, "Module description for generated sources", null, "setup of pm-mcp-clients",
                    DecisionScope.PROJECT_SETUP, "descriptions", "enrich", "setup", Reversibility.REVERSIBLE,
                    "Should the generated sources of pm-api be described as a module of their own?",
                    null, List.of(q031Part), Instant.parse("2026-10-16T14:45:00Z"),
                    false, null, null, null
            );
            openDecisions.put("Q-031", q031);

            // WR-2 (fix-relay waiver)
            var wr2 = new DecisionEnvelope(
                    "WR-2", 1, "fix-relay asks for a waiver", null, "plan fix-relay",
                    DecisionScope.PLAN, "execute", "implement", "plan", Reversibility.COSTLY,
                    "The test needs a second machine the CI does not have.",
                    Map.of("run", "CI run #412", "test", "RelayIntegrationTest.java"),
                    List.of(), Instant.parse("2026-10-16T13:10:00Z"),
                    true, "ci_check:integration-tests", "failing: needs second machine",
                    "The test needs a second machine the CI does not have."
            );
            openDecisions.put("WR-2", wr2);

            // WR-3 (session-log waiver)
            var wr3 = new DecisionEnvelope(
                    "WR-3", 1, "session-log asks for a waiver", null, "plan session-log",
                    DecisionScope.PLAN, "execute", "implement", "plan", Reversibility.COSTLY,
                    "the classes are generated by the annotation processor",
                    Map.of(CONST_COVERAGE, "76%", FIELD_TARGET, "80%"),
                    List.of(), Instant.parse("2026-10-16T14:02:00Z"),
                    true, "coverage:pm-api", "76 %: 3 classes without a test",
                    "the classes are generated by the annotation processor"
            );
            openDecisions.put("WR-3", wr3);
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
            if ("POST".equals(method) && ("answer".equals(action) || "answers".equals(action))) {
                handleAnswerDecision(request, response, decisionId, isWeb);
                return true;
            }
            if ("POST".equals(method) && "interpret".equals(action)) {
                handleInterpretDecision(request, response, decisionId);
                return true;
            }
            if ("POST".equals(method) && "withdraw".equals(action)) {
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

        var isPlanActivityEvents = path.startsWith(PATH_PLANS_PREFIX) && path.endsWith("/activity/events");
        var isPlanActivity = path.startsWith(PATH_PLANS_PREFIX) && path.contains("/tasks/") && path.endsWith("/activity");
        var isPlanProcess = path.startsWith(PATH_PLANS_PREFIX) && path.endsWith(SUFFIX_PROCESS);
        var isEpicProcess = path.startsWith(PATH_EPICS_PREFIX) && path.endsWith(SUFFIX_PROCESS);

        if (!isPlanActivityEvents && !isPlanActivity && !isPlanProcess && !isEpicProcess) {
            return false;
        }

        // 1. If static route is defined for this exact process path, return it directly
        var route = currentScenario.get().route("GET", path);
        if (route != null) {
            response.json(route.status(), route.representation());
            return true;
        }

        // 2. Dynamic plan task activity events SSE stream
        if (isPlanActivityEvents) {
            handleTaskActivityEventStream(response);
            return true;
        }

        // 3. Dynamic plan task activity: /api/v1/plans/{planId}/tasks/{taskId}/activity
        if (isPlanActivity) {
            var parts = path.split("/");
            if (parts.length >= 7 && "tasks".equals(parts[5])) {
                var taskId = parts[6];
                var defaultRoute = currentScenario.get().route("GET", "/api/v1/plans/fix-relay/tasks/TASK-003/activity");
                if (defaultRoute != null && defaultRoute.representation() instanceof Map<?, ?> repMap) {
                    var copy = new LinkedHashMap<String, Object>();
                    repMap.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    copy.put("task_id", taskId);
                    response.json(200, copy);
                    return true;
                }
            }
        }

        // 4. Dynamic plan process route: /api/v1/plans/{planId}/process
        if (isPlanProcess) {
            var sub = path.substring(PATH_PLANS_PREFIX.length());
            var planId = sub.substring(0, sub.length() - SUFFIX_PROCESS.length());
            if (!planId.contains("/")) {
                var defaultRoute = currentScenario.get().route("GET", "/api/v1/plans/fix-relay/process");
                if (defaultRoute != null && defaultRoute.representation() instanceof Map<?, ?> repMap) {
                    var copy = new LinkedHashMap<String, Object>();
                    repMap.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    copy.put(KEY_PLAN_ID, planId);
                    copy.put("title", planId);
                    response.json(200, copy);
                    return true;
                }
            }
        }

        // 5. Dynamic epic process route: /api/v1/epics/{epicId}/process
        if (isEpicProcess) {
            var sub = path.substring(PATH_EPICS_PREFIX.length());
            var epicId = sub.substring(0, sub.length() - SUFFIX_PROCESS.length());
            if (!epicId.contains("/")) {
                var defaultRoute = currentScenario.get().route("GET", "/api/v1/epics/relay-hardening/process");
                if (defaultRoute != null && defaultRoute.representation() instanceof Map<?, ?> repMap) {
                    var copy = new LinkedHashMap<String, Object>();
                    repMap.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    copy.put("epic_id", epicId);
                    copy.put("title", epicId);
                    response.json(200, copy);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            cancelPendingTasks();
            scheduler.shutdownNow();
            for (var stream : activeStreams) {
                try {
                    stream.close();
                } catch (IOException _) {
                    // Ignore error when closing active stream during engine shutdown
                }
            }
            activeStreams.clear();
        }
    }
}
