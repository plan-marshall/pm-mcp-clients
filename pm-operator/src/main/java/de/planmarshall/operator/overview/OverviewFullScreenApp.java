/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.event.EventKind;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.EndedWorkItem;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.FavouritesUpdateRequest;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReadReportsRequest;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.api.report.ReportKind;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.system.RuntimeDrainingEvent;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.operator.ConsoleNotice;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorActions;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OperatorUrlUtil;
import de.planmarshall.operator.OutputSafety;
import de.planmarshall.operator.OverviewSavedState;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.PmOperator;
import de.planmarshall.operator.actions.ActionMenus;
import de.planmarshall.operator.actions.ActionsMenu;
import de.planmarshall.operator.actions.WorkActionsClient;
import de.planmarshall.operator.epicview.EpicViewLevel;
import de.planmarshall.operator.planview.PlanCompletionLevel;
import de.planmarshall.operator.planview.PlanViewLevel;
import de.planmarshall.operator.planview.PlanViewLevel.EndedInfo;
import de.planmarshall.operator.planview.WorkerActivityLevel;
import de.planmarshall.operator.projects.ProjectOpenEntry;
import de.planmarshall.operator.projects.ProjectsExplorerActions;
import de.planmarshall.operator.projects.ProjectsExplorerLevel;
import de.planmarshall.operator.reports.ReportsLevel;
import de.planmarshall.operator.system.SystemLevel;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiMenu;
import de.planmarshall.operator.tui.TuiMenuEntry;
import de.planmarshall.operator.tui.TuiRow;
import de.planmarshall.operator.tui.TuiScreen;
import de.planmarshall.operator.tui.TuiScreenDriver;
import de.planmarshall.operator.tui.TuiViewStack;

/**
 * Coordinates the full-screen overview application
 * ({@code doc/design/terminal-client/15-overview.adoc}):
 * view stack, TUI screen driver, live SSE updates, state persistence, and held notices.
 */
public final class OverviewFullScreenApp {

    private static final String PROJECTS_API_PREFIX = "/api/v1/projects/";
    private static final String PLANS_API_PREFIX = "/api/v1/plans/";
    private static final String PROJECT_VERB = "project";
    private static final String STATUS_ROW_ID = "__status__";

    /**
     * Initial CLI selection state.
     *
     * @param project      optional initial project ID
     * @param by           optional initial view grouping (epic/plan)
     * @param ended        whether initial level is ended work
     * @param plan         optional plan to open in its full-screen plan view
     * @param explorer     whether the explorer is the root instead of running work
     * @param system       whether the system view opens above the root
     * @param systemWindow optional system view window ({@code hour}, {@code today}, {@code 7d})
     * @param systemGroup  optional system view grouping ({@code model} or {@code role})
     * @param systemProject optional project the system view shows, or {@code null} for the saved one
     */
    public record InitialSelection(@Nullable String project, @Nullable String by, boolean ended,
    @Nullable String plan, boolean explorer, boolean system, @Nullable String systemWindow,
    @Nullable String systemGroup, @Nullable String systemProject) {
        public static final InitialSelection DEFAULT =
                            new InitialSelection(null, null, false, null, false, false, null, null, null);

        /**
         * @param project optional initial project ID
         * @param by      optional initial view grouping
         * @param ended   whether the initial level is ended work
         */
        public InitialSelection(@Nullable String project, @Nullable String by, boolean ended) {
            this(project, by, ended, null, false, false, null, null, null);
        }

        /**
         * @param plan an existing plan to open at its plan view
         * @return the selection
         */
        public static InitialSelection plan(String plan) {
            return new InitialSelection(null, null, false, plan, false, false, null, null, null);
        }

        /**
         * @return the selection that opens the projects explorer as the root
         */
        public static InitialSelection explorerRoot() {
            return new InitialSelection(null, null, false, null, true, false, null, null, null);
        }

        /**
         * @param window  the system view's window, or {@code null} for the saved one
         * @param group   the system view's grouping, or {@code null} for the saved one
         * @param project the project the system view shows, or {@code null} for the saved one
         * @return the selection that opens the system view above the overview's root
         */
        public static InitialSelection system(@Nullable String window, @Nullable String group,
                            @Nullable String project) {
            return new InitialSelection(null, null, false, null, false, true, window, group, project);
        }
    }

    private final OperatorContext context;
    private final Terminal terminal;
    private final String locale;
    private final Clock clock;
    private final OverviewStateStore stateStore;
    private final ConsoleNoticeBuffer noticeBuffer;
    private final TuiViewStack stack = new TuiViewStack();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger unreadReports = new AtomicInteger(0);

    private final InitialSelection initialSelection;

    private final AtomicReference<ProjectOverviewResponse> cachedProjects = new AtomicReference<>();
    private final AtomicReference<ProjectsExplorerLevel> explorerLevel = new AtomicReference<>();
    private final AtomicReference<ProjectDetailResponse> currentProjectDetail = new AtomicReference<>();
    private final AtomicBoolean currentByEpic = new AtomicBoolean(false);
    private final AtomicReference<EndedWorkLevel> currentEndedLevel = new AtomicReference<>();
    private final AtomicReference<PlanViewLevel> currentPlanView = new AtomicReference<>();
    private final AtomicReference<WorkerActivityLevel> currentActivity = new AtomicReference<>();
    private final AtomicReference<PlanCompletionLevel> currentCompletion = new AtomicReference<>();
    private final AtomicReference<EpicViewLevel> currentEpicView = new AtomicReference<>();
    private final AtomicReference<ReportsLevel> currentReports = new AtomicReference<>();
    private final AtomicReference<SystemLevel> currentSystem = new AtomicReference<>();
    private final AtomicBoolean activityFollow = new AtomicBoolean(true);
    private volatile @Nullable String pendingAnswerPlanId;
    private @Nullable ActionsMenu actionsMenu;
    private @Nullable TuiMenu runtimeMenu;
    private @Nullable TuiMenu projectMenu;
    private @Nullable EnrolledProject projectMenuProject;
    private @Nullable BeingAddedProject projectMenuBeing;
    private List<ProjectOpenEntry> projectMenuOpenEntries = List.of();
    private @Nullable HoldRecord menuHold;
    private @Nullable EpicResource menuEpic;
    private @Nullable String menuKind;
    private @Nullable String menuLandingUnit;
    private @Nullable String statusMessage;
    private @Nullable String systemBanner;
    private @Nullable PrintWriter consoleOut;
    private @Nullable TuiScreenDriver activeDriver;
    private @Nullable OverviewKeyHandler activeKeyHandler;

    /**
     * Creates the full-screen overview application with system clock and context defaults.
     *
     * @param context        the operator context
     * @param terminal       the JLine terminal
     * @param initialProject optional initial project ID from CLI ({@code --project})
     * @param initialBy      optional initial view grouping ({@code --by epic} or {@code plan})
     * @param initialEnded   whether initial level is ended work ({@code --ended})
     */
    public OverviewFullScreenApp(OperatorContext context, Terminal terminal,
                        @Nullable String initialProject, @Nullable String initialBy,
                        boolean initialEnded) {
        this(context, terminal, new InitialSelection(initialProject, initialBy, initialEnded), Clock.systemUTC(),
                            new OverviewStateStore(context.stateDirectory()), context.noticeBuffer());
    }

    /**
     * Creates the full-screen overview application with explicit clock, store, and notice buffer
     * for deterministic testing.
     *
     * @param context          the operator context
     * @param terminal         the JLine terminal
     * @param initialSelection initial selection parameters
     * @param clock            reference clock
     * @param stateStore       overview state store
     * @param noticeBuffer     notice buffer
     */
    public OverviewFullScreenApp(OperatorContext context, Terminal terminal,
                        InitialSelection initialSelection, Clock clock, OverviewStateStore stateStore,
                        ConsoleNoticeBuffer noticeBuffer) {
        this.context = Objects.requireNonNull(context, "context");
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.locale = context.clientEnvironment().locale();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.noticeBuffer = Objects.requireNonNull(noticeBuffer, "noticeBuffer");
        this.initialSelection = Objects.requireNonNull(initialSelection, "initialSelection");
    }

    /**
     * Runs the full-screen overview.
     *
     * @param out output writer for notice summaries and fallback messages
     * @return the driver execution result
     * @throws IOException on terminal or network error
     */
    public TuiScreenDriver.Result run(PrintWriter out) throws IOException {
        if (!TuiScreenDriver.canOpenFullScreen(context, terminal, TuiLayout.MIN_COLUMNS)) {
            var driver = new TuiScreenDriver(terminal, locale);
            return driver.run(this::buildScreen, _ -> false);
        }

        var runtime = context.runtime(true);
        loadInitialData(runtime);

        noticeBuffer.activate();
        Thread sseThread = startEventListener();

        try {
            var driver = new TuiScreenDriver(terminal, locale);
            activeDriver = driver;
            consoleOut = out;
            var keyHandler = new OverviewKeyHandler();
            activeKeyHandler = keyHandler;
            var result = driver.run(this::buildScreen, keyHandler);
            openPendingDecision(out);
            return result;
        } finally {
            activeDriver = null;
            activeKeyHandler = null;
            running.set(false);
            sseThread.interrupt();
            saveState();
            noticeBuffer.deactivate();
            var where = leavingPlace();
            var drained = noticeBuffer.drain(locale, where);
            out.println(drained.summary().isEmpty()
                                ? ConsoleNoticeBuffer.leavingLine(locale, where)
                                : drained.summary());
            for (var line : drained.lines()) {
                out.println(line);
            }
            out.flush();
        }
    }

    /**
     * Names where the operator was when the view closed, so the line the view leaves behind says
     * where and not only what arrived: the live header of the level the operator left, or the
     * overview itself when no level names a narrower place.
     *
     * @return the localized place
     */
    String leavingPlace() {
        synchronized (this) {
            var lvl = stack.level();
            if (lvl == null || lvl.kind() == TuiLevel.Kind.OVERVIEW) {
                return ConsoleNoticeBuffer.overviewPlace(locale);
            }
            var selected = stack.selected();
            var title = levelBreadcrumb(lvl, selected != null ? selected.id() : null);
            return title.isBlank() ? ConsoleNoticeBuffer.overviewPlace(locale) : title;
        }
    }

    /**
     * The header the level shows, recomputed from the live model: a report count or a system
     * window can change while the view is open, so the pushed level title may be stale.
     *
     * @param lvl        the level, or {@code null}
     * @param selectedId the selected row, or {@code null}
     * @return the header, empty when there is no level
     */
    private String levelBreadcrumb(@Nullable TuiLevel lvl, @Nullable String selectedId) {
        if (lvl == null) {
            return "";
        }
        var title = lvl.title();
        return switch (lvl.kind()) {
            case PLAN -> header(currentPlanView.get(), title, view -> view.breadcrumb(locale, selectedId));
            case PLAN_ACTIVITY -> header(currentActivity.get(), title, view -> view.breadcrumb());
            case PLAN_COMPLETION -> header(currentCompletion.get(), title, view -> view.breadcrumb(locale));
            case EPIC -> header(currentEpicView.get(), title, view -> view.level(locale).title());
            case REPORTS -> header(currentReports.get(), title, view -> view.breadcrumb(locale));
            case SYSTEM -> header(currentSystem.get(), title,
                                view -> view.breadcrumb(locale, SystemLevel.sectionOf(lvl.id())));
            default -> title;
        };
    }

    /**
     * @param view     the model of the level, or {@code null} when the level has none yet
     * @param fallback the title the level was pushed with
     * @param header   the header the live model renders
     * @param <T>      the model type
     * @return the live header, or the fallback when the model is missing
     */
    private static <T> String header(@Nullable T view, String fallback, Function<T, String> header) {
        return view != null ? header.apply(view) : fallback;
    }

    /**
     * @param lvl the level, or {@code null}
     * @return the footer the level shows, or {@code null} when it has none
     */
    private @Nullable String levelFooter(@Nullable TuiLevel lvl) {
        if (lvl == null) {
            return null;
        }
        var kind = lvl.kind();
        if (kind == TuiLevel.Kind.PLAN) {
            var view = currentPlanView.get();
            return view != null ? view.footer(locale) : null;
        }
        if (kind == TuiLevel.Kind.EPIC) {
            var view = currentEpicView.get();
            return view != null ? view.footer(locale) : null;
        }
        if (kind == TuiLevel.Kind.SYSTEM) {
            var view = currentSystem.get();
            if (view != null && SystemLevel.sectionOf(lvl.id()) == SystemLevel.Section.ROOT) {
                return view.footer(locale);
            }
        }
        return null;
    }

    /**
     * A node that waits for the operator leaves the view and hands the decision to the console:
     * the alternate screen is gone, and the plan's open questions are printed so that
     * {@code answer <id>} can open the decision.
     *
     * @param out the output writer of the console
     */
    private void openPendingDecision(PrintWriter out) {
        var plan = pendingAnswerPlanId;
        pendingAnswerPlanId = null;
        if (plan != null) {
            PmOperator.execute(context, out, out, "questions", "--plan", plan);
        }
    }

    void loadInitialData(RuntimeClient runtime) {
        if (initialSelection.explorer()) {
            fetchExplorer(runtime);
            fetchUnreadReports(runtime);
            var level = explorerLevel.get();
            if (level != null) {
                var rows = level.rows(locale, clock);
                stack.replaceRoot(ProjectsExplorerLevel.level(locale), rows,
                                    ProjectsExplorerLevel.firstSelectableId(rows));
            }
            return;
        }
        fetchProjects(runtime);
        fetchUnreadReports(runtime);

        var cached = cachedProjects.get();
        var projects = cached != null ? cached.projects() : List.<ProjectSummary>of();
        var rootLevel = new ProjectsOverviewLevel(projects);
        var rootRows = rootLevel.rows(locale, clock);

        if (initialSelection.system()) {
            stack.replaceRoot(ProjectsOverviewLevel.level(locale), rootRows, null);
            openSystem(initialSelection.systemWindow(), initialSelection.systemGroup(),
                                initialSelection.systemProject());
            return;
        }

        var saved = stateStore.read().orElse(OverviewSavedState.empty());

        var initialPlan = initialSelection.plan();
        if (initialPlan != null && !initialPlan.isBlank()) {
            openPlanAsRoot(runtime, initialPlan);
            return;
        }

        if (initialSelection.ended()) {
            loadInitialEnded(runtime, rootRows, projects);
            return;
        }

        if (initialSelection.project() != null && !initialSelection.project().isBlank()) {
            stack.replaceRoot(ProjectsOverviewLevel.level(locale), rootRows, null);
            boolean byEpic = "epic".equalsIgnoreCase(initialSelection.by());
            openProject(runtime, initialSelection.project(), byEpic, null);
            return;
        }

        loadInitialSavedOrRoot(runtime, rootRows, projects, saved);
    }

    void loadInitialEnded(RuntimeClient runtime, List<TuiRow> rootRows, List<ProjectSummary> projects) {
        stack.replaceRoot(ProjectsOverviewLevel.level(locale), rootRows, null);
        String targetProject = initialSelection.project();
        if ((targetProject == null || targetProject.isBlank()) && !projects.isEmpty()) {
            targetProject = projects.getFirst().projectId();
        }
        if (targetProject != null && !targetProject.isBlank()) {
            openEnded(runtime, targetProject);
        }
    }

    void loadInitialSavedOrRoot(RuntimeClient runtime, List<TuiRow> rootRows,
                        List<ProjectSummary> projects, OverviewSavedState saved) {
        var savedProjectId = saved.projectId();
        if (savedProjectId != null && projects.stream().anyMatch(p -> p.projectId().equals(savedProjectId))) {
            stack.replaceRoot(ProjectsOverviewLevel.level(locale), rootRows, savedProjectId);
            boolean byEpic = "epic".equalsIgnoreCase(initialSelection.by() != null ? initialSelection.by() : saved.projectView());
            openProject(runtime, savedProjectId, byEpic, saved.selectedId());
        } else {
            stack.replaceRoot(ProjectsOverviewLevel.level(locale), rootRows, saved.selectedId());
        }
    }

    void fetchProjects(RuntimeClient runtime) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/projects"));
            if (response.status() == 200) {
                cachedProjects.set(ProjectOverviewResponse.fromJson(response.bodyText()));
            }
        } catch (IOException _) {
            // Keep existing cached projects if runtime read fails
        }
    }

    void fetchExplorer(RuntimeClient runtime) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/projects"));
            if (response.status() == 200) {
                var explorer = ProjectExplorer.fromJson(response.bodyText());
                var current = explorerLevel.get();
                boolean byMostUsed = current != null && current.byMostUsed();
                explorerLevel.set(new ProjectsExplorerLevel(explorer, byMostUsed));
            }
        } catch (IOException | IllegalArgumentException _) {
            // Keep the last report of the projects when the runtime read fails.
        }
    }

    void fetchUnreadReports(RuntimeClient runtime) {
        var page = fetchReportsPage(runtime);
        if (page != null) {
            unreadReports.set(page.unreadCount());
        }
    }

    /**
     * Reads the first page of unread reports.
     *
     * @param runtime the runtime client
     * @return the page, or {@code null} when the runtime did not serve it
     */
    @Nullable
    ReportsPage fetchReportsPage(RuntimeClient runtime) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/reports"));
            if (response.status() == 200) {
                var page = ReportsPage.fromJson(response.bodyText());
                unreadReports.set(page.unreadCount());
                return page;
            }
        } catch (IOException | IllegalArgumentException _) {
            // The last page the view holds stays until the runtime answers again.
        }
        return null;
    }

    /**
     * Reads the system snapshot for one choice of window and project.
     *
     * @param runtime   the runtime client
     * @param window    the models' window
     * @param projectId the project, or {@code null} for all projects
     * @return the snapshot, or {@code null} when the runtime did not serve it
     */
    @Nullable
    SystemSnapshot fetchSystemSnapshot(RuntimeClient runtime, SystemWindow window,
                        @Nullable String projectId) {
        try {
            var path = "/api/v1/system?window=" + OperatorUrlUtil.encodePathSegment(window.value())
                                + (projectId != null
                                ? "&project=" + OperatorUrlUtil.encodePathSegment(projectId) : "");
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                return SystemSnapshot.fromJson(response.bodyText());
            }
        } catch (IOException | IllegalArgumentException _) {
            // The view keeps the snapshot it showed and says it could not reload.
        }
        return null;
    }

    boolean isCurrentProjectSetupNeedingOperator() {
        var detail = currentProjectDetail.get();
        var cached = cachedProjects.get();
        if (detail == null || cached == null) {
            return false;
        }
        var currentProjectId = detail.projectId();
        return cached.projects().stream()
                            .filter(p -> p.projectId().equals(currentProjectId))
                            .findFirst()
                            .map(ProjectSummary::setupNeedsOperator)
                            .orElse(false);
    }

    void openProject(RuntimeClient runtime, @Nullable String projectId, boolean byEpic, @Nullable String selectedId) {
        if (projectId == null || projectId.isBlank()) {
            return;
        }
        try {
            var path = PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(projectId);
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                var detail = ProjectDetailResponse.fromJson(response.bodyText());
                currentProjectDetail.set(detail);
                currentByEpic.set(byEpic);
                var levelModel = new ProjectWorkLevel(detail, byEpic, isCurrentProjectSetupNeedingOperator());
                var rows = levelModel.rows(locale, clock, false);
                stack.push(levelModel.level(locale), rows, selectedId);
            }
        } catch (IOException _) {
            // Fall back to staying at project list
        }
    }

    void openEnded(RuntimeClient runtime, @Nullable String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return;
        }
        try {
            var path = PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(projectId) + "/ended?page=1";
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                var endedResponse = EndedWorkPage.fromJson(response.bodyText());
                var endedLevel = new EndedWorkLevel(endedResponse);
                currentEndedLevel.set(endedLevel);
                stack.push(endedLevel.level(locale), endedLevel.rows(locale, clock, false), null);
            }
        } catch (IOException _) {
            // Fall back
        }
    }

    /**
     * Opens an epic's view as a level of the full-screen view.
     *
     * @param runtime the runtime client
     * @param epicId  the epic
     * @return {@code true} when the runtime served the epic
     */
    boolean openEpicView(String epicId) {
        try {
            var resource = new WorkActionsClient(context).epic(epicId);
            var view = new EpicViewLevel(resource);
            currentEpicView.set(view);
            stack.push(view.level(locale), view.rows(locale), view.initialRowId());
            return true;
        } catch (IOException _) {
            return false;
        }
    }

    /**
     * Opens an ended epic whose resource the runtime no longer serves, from its ended work row.
     *
     * @param item the ended work row
     */
    void openEndedEpicView(EndedWorkItem item) {
        var projectId = currentEndedLevel.get() != null ? currentEndedLevel.get().projectId() : "";
        var note = new EpicViewLevel.EndedNote(
                            item.terminalReason() != null ? item.terminalReason() : "completed",
                            item.reason(),
                            OverviewFormatting.formatEndedTime(locale, item.endedAt(), clock));
        var view = EpicViewLevel.ended(item.id(), projectId, item.title(), note);
        currentEpicView.set(view);
        stack.push(view.level(locale), view.rows(locale), view.initialRowId());
    }

    void openPlanAsRoot(RuntimeClient runtime, String planId) {
        if (!fetchPlan(runtime, planId, null)) {
            return;
        }
        var view = currentPlanView.get();
        if (view == null) {
            return;
        }
        // The plan is the whole view when the operator asked for it by name; there is no caller
        // to return to, so Esc leaves the view.
        var level = TuiLevel.of(TuiLevel.Kind.PLAN, planId, view.breadcrumb(locale, null));
        stack.replaceRoot(level, view.rows(locale), view.initialRowId());
    }

    boolean openPlan(RuntimeClient runtime, String planId, @Nullable EndedInfo ended) {
        return fetchPlan(runtime, planId, ended);
    }

    boolean fetchPlan(RuntimeClient runtime, String planId, @Nullable EndedInfo ended) {
        try {
            var path = PLANS_API_PREFIX + OperatorUrlUtil.encodePathSegment(planId) + "/process";
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() != 200) {
                return false;
            }
            var tree = ProcessTree.fromJson(response.bodyText());
            var view = new PlanViewLevel(tree, ended);
            currentPlanView.set(view);
            var level = TuiLevel.of(TuiLevel.Kind.PLAN, planId,
                                view.breadcrumb(locale, null));
            stack.push(level, view.rows(locale), view.initialRowId());
            return true;
        } catch (IOException | IllegalArgumentException _) {
            return false;
        }
    }

    void saveState() {
        try {
            var existing = stateStore.read().orElse(OverviewSavedState.empty());
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            var kind = lvl.kind();
            if (kind == TuiLevel.Kind.PROJECTS) {
                // The explorer is a root of its own; it does not move the overview's start point.
                return;
            }
            if (kind == TuiLevel.Kind.PLAN_ACTIVITY || kind == TuiLevel.Kind.PLAN_COMPLETION
                                || kind == TuiLevel.Kind.REPORTS) {
                // A sub-level of a plan, or the reports, does not describe a place of the
                // overview; keep the snapshot the operator had before opening it.
                return;
            }
            if (kind == TuiLevel.Kind.SYSTEM) {
                stateStore.write(withSystemChoice(existing, existing.projectId(), existing.projectView(),
                                    existing.selectedId()));
                return;
            }
            var sel = stack.selected();
            var selectedId = sel != null ? sel.id() : null;

            String projectId = null;
            String projectView = null;

            if (kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) {
                projectId = lvl.id();
                projectView = kind == TuiLevel.Kind.PROJECT_BY_EPIC ? "epic" : "plan";
            } else if (kind == TuiLevel.Kind.PLAN) {
                var view = currentPlanView.get();
                if (view != null) {
                    projectId = view.projectId();
                    projectView = view.epicId() != null ? "epic" : "plan";
                    selectedId = view.planId();
                }
            } else if (kind == TuiLevel.Kind.EPIC) {
                var view = currentEpicView.get();
                if (view != null) {
                    projectId = view.projectId();
                    projectView = "epic";
                    selectedId = view.epicId();
                }
            }

            var updated = withSystemChoice(existing, projectId, projectView, selectedId);
            stateStore.write(updated);
        } catch (IOException _) {
            // best effort persistence
        }
    }

    /**
     * The saved state with the operator's system choice carried over, whether or not the view
     * ended on a system level: {@code w}, {@code b} and {@code p} must survive leaving the view
     * from any level ({@code doc/design/terminal-client/15-overview.adoc#overview-persistence}).
     *
     * @param existing  the state as it was
     * @param projectId the project the overview ended in, or {@code null}
     * @param view      the overview's view, or {@code null}
     * @param selected  the overview's selection, or {@code null}
     * @return the state to write
     */
    private OverviewSavedState withSystemChoice(OverviewSavedState existing, @Nullable String projectId,
                        @Nullable String view, @Nullable String selected) {
        var system = currentSystem.get();
        return new OverviewSavedState(
                            projectId,
                            view,
                            selected,
                            system != null ? system.window().value() : existing.systemWindow(),
                            system != null ? system.groupBy() : existing.systemGroupBy(),
                            system != null ? system.projectId() : existing.systemProjectId());
    }

    /**
     * @param kind the notice kind resolved from the event name, or {@code null}
     * @return the buffer category the notice counts as
     */
    static ConsoleNoticeBuffer.Category categoryOf(@Nullable String kind) {
        return switch (kind != null ? kind : "") {
            case "question", "decision", "waiver_request", "answered_elsewhere" -> ConsoleNoticeBuffer.Category.DECISION;
            case "report" -> ConsoleNoticeBuffer.Category.REPORT;
            default -> ConsoleNoticeBuffer.Category.NEED;
        };
    }

    static int countOf(Map<String, Object> eventMap, ConsoleNoticeBuffer.Category category) {
        var payload = ConsoleNotice.payloadOf(eventMap);
        if (category == ConsoleNoticeBuffer.Category.REPORT
                            && payload.get("count") instanceof Number number && number.intValue() > 1) {
            return Math.min(number.intValue(), 10_000);
        }
        return 1;
    }

    TuiScreenDriver.KeyHandler keyHandler() {
        return new OverviewKeyHandler();
    }

    TuiViewStack stack() {
        return stack;
    }

    void stop() {
        running.set(false);
    }

    synchronized TuiScreen buildScreen() {
        var lvl = stack.level();
        var kind = lvl != null ? lvl.kind() : TuiLevel.Kind.OVERVIEW;
        var selected = stack.selected();
        var selectedId = selected != null ? selected.id() : null;
        var breadcrumb = levelBreadcrumb(lvl, selectedId);
        var footer = levelFooter(lvl);
        var rows = stack.visibleRows();
        if (statusMessage != null && !statusMessage.isBlank()) {
            var withStatus = new ArrayList<TuiRow>(rows.size() + 1);
            withStatus.addAll(rows);
            withStatus.add(TuiRow.leaf(STATUS_ROW_ID, "  " + statusMessage));
            rows = withStatus;
        }
        TuiMenu menu = null;
        if (projectMenu != null) {
            menu = projectMenu;
        } else if (runtimeMenu != null) {
            menu = runtimeMenu;
        } else if (actionsMenu != null) {
            menu = actionsMenu.screenMenu();
        }
        return new TuiScreen(
                            breadcrumb,
                            kind,
                            rows,
                            stack.selectedIndex(),
                            stack.filter(),
                            stack.helpVisible(),
                            connected.get(),
                            unreadReports.get(),
                            footer,
                            menu,
                            kind == TuiLevel.Kind.SYSTEM ? systemBanner : null
        );
    }

    /** The dialogue surface a suspended view hands its action to. */
    @FunctionalInterface
    private interface DialogueTask {
        void run() throws IOException;
    }

    private WorkActionsClient actionsClient() {
        return new WorkActionsClient(context);
    }

    private PrintWriter dialogueOut() {
        return consoleOut != null ? consoleOut : new PrintWriter(System.out, true);
    }

    private void runDialogue(DialogueTask task) {
        Runnable guarded = () -> {
            try {
                task.run();
            } catch (IOException refused) {
                statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
            }
        };
        var driver = activeDriver;
        if (driver != null) {
            driver.suspendFor(guarded);
        } else {
            guarded.run();
        }
    }

    private void openActions() {
        var lvl = stack.level();
        var selected = stack.selected();
        if (lvl == null || selected == null) {
            return;
        }
        var selectedId = selected.id();
        var kind = lvl.kind();
        if (kind == TuiLevel.Kind.SYSTEM) {
            openRuntimeActions();
            return;
        }
        try {
            if (kind == TuiLevel.Kind.ENDED) {
                var ended = currentEndedLevel.get();
                var item = ended != null ? ended.items().stream()
                                    .filter(candidate -> candidate.id().equals(selectedId)).findFirst()
                                    .orElse(null) : null;
                if (item == null) {
                    return;
                }
                menuKind = "epic".equalsIgnoreCase(item.kind()) ? "epic" : "plan";
                actionsMenu = ActionMenus.ended(locale, item.id(), item.title(), selectedId);
                return;
            }
            if (kind == TuiLevel.Kind.PLAN) {
                var view = currentPlanView.get();
                if (view == null) {
                    return;
                }
                if (view.isEnded()) {
                    menuKind = "plan";
                    actionsMenu = ActionMenus.ended(locale, view.planId(), view.planId(), selectedId);
                } else {
                    openPlanActions(view.planId(), selectedId);
                }
                return;
            }
            if (kind == TuiLevel.Kind.PLAN_ACTIVITY) {
                var activity = currentActivity.get();
                if (activity != null) {
                    openPlanActions(activity.planId(), selectedId);
                }
                return;
            }
            if (kind == TuiLevel.Kind.PLAN_COMPLETION) {
                var completion = currentCompletion.get();
                if (completion != null) {
                    menuKind = "plan";
                    actionsMenu = ActionMenus.ended(locale, completion.planId(), completion.planId(),
                                        selectedId);
                }
                return;
            }
            if (kind == TuiLevel.Kind.EPIC) {
                openEpicRowActions(selectedId);
                return;
            }
            if (kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) {
                openProjectRowActions(selectedId);
            }
        } catch (IOException refused) {
            statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
        }
    }

    private void openEpicRowActions(String selectedId) throws IOException {
        var view = currentEpicView.get();
        if (view == null) {
            return;
        }
        if (view.isEnded()) {
            menuKind = "epic";
            menuHold = null;
            menuEpic = null;
            actionsMenu = ActionMenus.ended(locale, view.epicId(), view.epicId(), selectedId);
            return;
        }
        var planId = view.planIdOf(selectedId);
        if (planId != null && !view.isStagedPlanRow(selectedId)) {
            openPlanActions(planId, selectedId);
            return;
        }
        openEpicActions(view.epicId(), selectedId);
    }

    private void openProjectRowActions(String selectedId) throws IOException {
        var detail = currentProjectDetail.get();
        if (detail == null) {
            return;
        }
        if (selectedId.startsWith("landing:")) {
            openLandingActions(selectedId.substring("landing:".length()), selectedId);
            return;
        }
        boolean epic = detail.epics().stream().anyMatch(candidate -> candidate.epicId().equals(selectedId));
        if (epic) {
            openEpicActions(selectedId, selectedId);
            return;
        }
        boolean plan = detail.plans().stream().anyMatch(candidate -> candidate.planId().equals(selectedId));
        if (plan) {
            openPlanActions(selectedId, selectedId);
        }
    }

    private void openPlanActions(String planId, @Nullable String rowId) throws IOException {
        var client = actionsClient();
        var actions = client.actions("plan", planId);
        HoldRecord hold = null;
        try {
            hold = client.hold(planId);
        } catch (IOException _) {
            // A plan without a readable hold list still offers its actions.
        }
        if (actions.actions().isEmpty()) {
            statusMessage = MessageCatalogue.format(locale, "operator.actions.none", Map.of("name", planId));
            actionsMenu = null;
            return;
        }
        menuKind = "plan";
        menuHold = hold;
        menuEpic = null;
        actionsMenu = ActionMenus.plan(locale, actions, hold, rowId);
        reportDroppedActions(actionsMenu);
    }

    private void openEpicActions(String epicId, @Nullable String rowId) throws IOException {
        var client = actionsClient();
        var actions = client.actions("epic", epicId);
        EpicResource resource = null;
        try {
            resource = client.epic(epicId);
        } catch (IOException _) {
            // The actions still offer what the state allows; the head facts default.
        }
        if (actions.actions().isEmpty()) {
            statusMessage = MessageCatalogue.format(locale, "operator.actions.none", Map.of("name", epicId));
            actionsMenu = null;
            return;
        }
        menuKind = "epic";
        menuHold = null;
        menuEpic = resource;
        var next = resource != null ? nextLaunchable(resource) : null;
        var reasons = currentEpicView.get() != null
                            ? currentEpicView.get().waitReasons(locale) : List.<String>of();
        actionsMenu = ActionMenus.epic(locale, actions, next, reasons,
                            resource != null && resource.autoStart(),
                            resource != null ? resource.overlapMaxFiles() : 0,
                            resource != null ? resource.findings().newCount() : 0,
                            resource != null ? resource.watches().open() : 0, rowId);
        reportDroppedActions(actionsMenu);
    }

    private void openLandingActions(String unit, String rowId) throws IOException {
        var detail = currentProjectDetail.get();
        if (detail == null) {
            return;
        }
        var landing = detail.alongsideLandings().stream()
                            .filter(candidate -> unit.equals(candidate.unit()))
                            .findFirst().orElse(null);
        menuKind = "landing";
        menuLandingUnit = unit;
        menuHold = null;
        menuEpic = null;
        actionsMenu = ActionMenus.landing(locale, unit,
                            landing != null ? landing.holdKind() : null, rowId);
    }

    private void reportDroppedActions(ActionsMenu menu) {
        if (menu.droppedActions().isEmpty()) {
            return;
        }
        statusMessage = MessageCatalogue.format(locale, "operator.actions.dropped",
                            Map.of("actions", String.join(", ", menu.droppedActions())));
    }

    private static @Nullable String nextLaunchable(EpicResource epic) {
        for (var workstream : epic.workstreams()) {
            for (var row : workstream.rows()) {
                if ("staged".equals(normalizeStatus(row.status()))
                                    && (row.waitReason() == null || "none".equals(row.waitReason()))) {
                    return row.planId();
                }
            }
        }
        return null;
    }

    private static String normalizeStatus(@Nullable String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /**
     * @param trigger the menu's mnemonic, or the empty string to close the menu
     * @return {@code true} to keep the view open
     */
    private boolean handleMenu(String trigger) {
        if (runtimeMenu != null) {
            handleRuntimeMenu(trigger);
            return true;
        }
        if (projectMenu != null) {
            var handler = activeKeyHandler;
            if (handler != null) {
                handler.handleProjectMenu(trigger);
            }
            return true;
        }
        if (actionsMenu == null) {
            return true;
        }
        if (trigger.isEmpty()) {
            actionsMenu = null;
            return true;
        }
        var entry = actionsMenu.entry(trigger);
        if (entry == null) {
            return true;
        }
        if (!entry.enabled()) {
            statusMessage = entry.reason() != null ? entry.reason() : "";
            return true;
        }
        var menu = actionsMenu;
        actionsMenu = null;
        runMenuEntry(menu, entry);
        refreshActiveLevels();
        return true;
    }

    private void runMenuEntry(ActionsMenu menu, TuiMenuEntry entry) {
        var subject = menu.subject();
        if (subject == ActionsMenu.Subject.LANDING) {
            if ("show_pr".equals(entry.action())) {
                statusMessage = entry.label();
                return;
            }
            var project = currentProjectId();
            if (project != null && menuLandingUnit != null) {
                runDialogue(() -> actionsClient().performLanding(project, menuLandingUnit, entry.action()));
            }
            return;
        }
        var kind = menuKind != null ? menuKind : "plan";
        if (subject == ActionsMenu.Subject.ENDED) {
            runDialogue(() -> OperatorActions.chatAboutEnded(context, dialogueOut(), kind, menu.subjectId(),
                                currentProjectId(), locale));
            return;
        }
        var holdId = menuHold != null ? menuHold.holdId() : null;
        var epic = menuEpic;
        runDialogue(() -> OperatorActions.run(context, dialogueOut(), kind, menu.subjectId(), entry.action(),
                            epic, holdId));
    }

    private @Nullable String currentProjectId() {
        var lvl = stack.level();
        if (lvl == null) {
            return null;
        }
        return switch (lvl.kind()) {
            case EPIC -> {
                var view = currentEpicView.get();
                yield view != null && view.epicId().equals(lvl.id()) ? view.projectId() : null;
            }
            case PLAN -> {
                var view = currentPlanView.get();
                yield view != null && view.planId().equals(lvl.id()) ? view.projectId() : null;
            }
            case PLAN_ACTIVITY, PLAN_COMPLETION -> {
                var view = currentPlanView.get();
                yield view != null ? view.projectId() : null;
            }
            case PROJECT_BY_PLAN, PROJECT_BY_EPIC -> {
                var detail = currentProjectDetail.get();
                yield detail != null && detail.projectId().equals(lvl.id()) ? detail.projectId() : null;
            }
            case ENDED -> {
                var ended = currentEndedLevel.get();
                yield ended != null && ended.projectId().equals(lvl.id()) ? ended.projectId() : null;
            }
            default -> null;
        };
    }

    /** Opens what waits for the operator in the selected row. */
    private void openWhatWaits() {
        var lvl = stack.level();
        var selected = stack.selected();
        if (lvl == null || selected == null) {
            return;
        }
        if (lvl.kind() == TuiLevel.Kind.EPIC) {
            var view = currentEpicView.get();
            if (view != null) {
                var planId = view.planIdOf(selected.id());
                if (planId != null) {
                    try {
                        var hold = actionsClient().hold(planId);
                        if (hold != null) {
                            runDialogue(() -> OperatorActions.runHold(context, dialogueOut(), planId,
                                                hold.holdId()));
                        }
                    } catch (IOException refused) {
                        statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
                    }
                }
                return;
            }
        }
        openActions();
    }

    private Thread startEventListener() {
        var thread = new Thread(() -> {
            while (running.get()) {
                connectAndStreamEvents();
            }
        }, "pm-overview-sse");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void connectAndStreamEvents() {
        try {
            var runtime = context.runtime(false);
            try (var exchange = runtime.open(HttpRequest.get("/api/v1/events"))) {
                connected.set(true);
                terminal.raise(Terminal.Signal.WINCH);
                var sseReader = exchange.events();
                var event = sseReader.next();
                while (running.get() && event != null) {
                    handleLiveEvent(event.event(), event.data());
                    event = sseReader.next();
                }
            }
        } catch (IOException _) {
            handleSseDisconnect();
        }
    }

    void handleSseDisconnect() {
        connected.set(false);
        terminal.raise(Terminal.Signal.WINCH);
        if (running.get()) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                running.set(false);
            }
        }
    }

    void handleLiveEvent(@Nullable String eventName, @Nullable String data) {
        var eventMap = parseEvent(data);
        if (eventMap == null) {
            return;
        }
        var kind = ConsoleNotice.noticeKindOf(eventName, eventMap);
        holdNotice(eventName, kind, eventMap);

        var name = eventName != null ? eventName : "";
        var type = kind != null ? kind.notice()
                            : String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));

        if (isReportEvent(name, type)) {
            refreshReports();
            return;
        }
        if ("runtime.draining".equals(name) || "runtime.draining".equals(type)) {
            applyDrainingBanner(eventMap);
            return;
        }
        if ("system.metrics".equals(name) || "system.metrics".equals(type)) {
            refreshSystem();
            return;
        }

        if (isStructuralChange(name, type)) {
            refreshActiveLevels();
        }
    }

    /**
     * @param data the event's data member
     * @return the parsed event frame, or {@code null} when it is absent or no JSON object
     */
    private static @Nullable Map<String, Object> parseEvent(@Nullable String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return JsonTree.asObject(JsonTree.parse(data));
        } catch (IOException _) {
            return null;
        }
    }

    /**
     * Offers the event's notice to the buffer while a full-screen view is open.
     *
     * @param eventName the SSE event name, or {@code null}
     * @param kind      the notice kind the event resolves to, or {@code null}
     * @param eventMap  the parsed event frame
     */
    private void holdNotice(@Nullable String eventName, @Nullable EventKind kind, Map<String, Object> eventMap) {
        var notice = ConsoleNotice.fromEventMap(locale, eventName, eventMap);
        if (notice == null || context.isConsoleActive()) {
            return;
        }
        var category = categoryOf(kind != null ? kind.notice() : null);
        noticeBuffer.hold(category, countOf(eventMap, category));
    }

    static boolean isReportEvent(String name, String type) {
        return "report.sent".equals(name) || "report.read".equals(name)
                            || "report.sent".equals(type) || "report.read".equals(type);
    }

    /**
     * The progress line below the system view's header while the runtime drains: how many tasks and
     * jobs are left and which they are ({@code doc/design/terminal-client/25-system.adoc#full-screen}).
     *
     * @param eventMap the {@code runtime.draining} event
     */
    void applyDrainingBanner(Map<String, Object> eventMap) {
        try {
            var event = RuntimeDrainingEvent.fromMap(eventMap);
            var what = String.join(", ", event.remainingItems().stream()
                                .map(OutputSafety::stripControlSequences).toList());
            systemBanner = MessageCatalogue.format(locale, "system.stopping.line",
                                Map.of("count", event.remainingCount(), "what", what));
            terminal.raise(Terminal.Signal.WINCH);
        } catch (IOException _) {
            // A malformed event leaves the last line in place.
        }
    }

    static boolean isStructuralChange(String name, String type) {
        if (name.startsWith("project.") || name.startsWith("phase.") || name.startsWith("scope.")
                            || name.startsWith("hold.") || name.startsWith("condition.") || name.startsWith("task.")
                            || "activity.changed".equals(name) || "tokens.changed".equals(name)
                            || "state.entered".equals(name) || "state.left".equals(name)
                            || "instance.changed".equals(name)
                            || "needs_operator.changed".equals(name) || "reset".equals(name)) {
            return true;
        }
        return type.startsWith("project.") || type.startsWith("phase.") || type.startsWith("scope.")
                            || type.startsWith("hold.") || type.startsWith("condition.") || type.startsWith("task.")
                            || "activity.changed".equals(type) || "tokens.changed".equals(type)
                            || "state.entered".equals(type) || "state.left".equals(type)
                            || "instance.changed".equals(type) || "needs_operator.changed".equals(type)
                            || "reset".equals(type);
    }

    void refreshActiveLevels() {
        TuiLevel.Kind targetKind;
        String targetProjectId;
        synchronized (this) {
            var lvl = stack.level();
            targetKind = lvl != null ? lvl.kind() : null;
            targetProjectId = lvl != null ? lvl.id() : null;
        }
        if (targetKind == null) {
            return;
        }
        if (targetKind == TuiLevel.Kind.PROJECTS) {
            refreshExplorer();
            return;
        }
        if (targetKind == TuiLevel.Kind.REPORTS) {
            refreshReports();
            return;
        }
        if (targetKind == TuiLevel.Kind.SYSTEM) {
            refreshSystem();
            return;
        }

        try {
            var runtime = context.runtime(false);
            if (targetKind == TuiLevel.Kind.PLAN) {
                refreshPlan(runtime);
                return;
            }
            if (targetKind == TuiLevel.Kind.PLAN_ACTIVITY) {
                refreshActivity(runtime);
                return;
            }
            if (targetKind == TuiLevel.Kind.EPIC) {
                refreshEpic();
                return;
            }
            fetchProjects(runtime);
            fetchUnreadReports(runtime);

            ProjectDetailResponse fetchedDetail = fetchTargetDetail(runtime, targetKind, targetProjectId);
            EndedWorkPage fetchedEnded = fetchTargetEnded(runtime, targetKind, targetProjectId);

            synchronized (this) {
                applyRefreshedData(targetKind, targetProjectId, fetchedDetail, fetchedEnded);
            }
            terminal.raise(Terminal.Signal.WINCH);
        } catch (IOException _) {
            // best-effort refresh
        }
    }

    static @Nullable ProjectDetailResponse fetchTargetDetail(RuntimeClient runtime, TuiLevel.Kind kind, @Nullable String projectId) throws IOException {
        if ((kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) && projectId != null) {
            var response = runtime.send(HttpRequest.get(PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(projectId)));
            if (response.status() == 200) {
                return ProjectDetailResponse.fromJson(response.bodyText());
            }
        }
        return null;
    }

    static @Nullable EndedWorkPage fetchTargetEnded(RuntimeClient runtime, TuiLevel.Kind kind, @Nullable String projectId) throws IOException {
        if (kind == TuiLevel.Kind.ENDED && projectId != null) {
            var response = runtime.send(HttpRequest.get(PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(projectId) + "/ended?page=1"));
            if (response.status() == 200) {
                return EndedWorkPage.fromJson(response.bodyText());
            }
        }
        return null;
    }

    void applyRefreshedData(TuiLevel.Kind targetKind, @Nullable String targetProjectId,
                        @Nullable ProjectDetailResponse fetchedDetail,
                        @Nullable EndedWorkPage fetchedEnded) {
        var currentLvl = stack.level();
        if (currentLvl == null || currentLvl.kind() != targetKind || !Objects.equals(currentLvl.id(), targetProjectId)) {
            return;
        }

        if (targetKind == TuiLevel.Kind.OVERVIEW) {
            var cached = cachedProjects.get();
            var projects = cached != null ? cached.projects() : List.<ProjectSummary>of();
            var levelModel = new ProjectsOverviewLevel(projects);
            stack.refresh(levelModel.rows(locale, clock));
        } else if (targetKind == TuiLevel.Kind.PROJECT_BY_PLAN || targetKind == TuiLevel.Kind.PROJECT_BY_EPIC) {
            if (fetchedDetail != null) {
                currentProjectDetail.set(fetchedDetail);
                var levelModel = new ProjectWorkLevel(fetchedDetail, currentByEpic.get(), isCurrentProjectSetupNeedingOperator());
                stack.refresh(levelModel.rows(locale, clock, stack.helpVisible()));
            }
        } else if (targetKind == TuiLevel.Kind.ENDED && fetchedEnded != null) {
            var ended = currentEndedLevel.get();
            if (ended != null) {
                ended.refreshFirstPage(fetchedEnded);
                stack.refresh(ended.rows(locale, clock, stack.helpVisible()));
            }
        }
    }

    private void refreshExplorer() {
        var selected = stack.selected();
        var selectedId = selected != null ? selected.id() : null;
        try {
            var runtime = context.runtime(false);
            fetchExplorer(runtime);
            fetchUnreadReports(runtime);
        } catch (IOException _) {
            // The last report of the projects stays until the runtime is reachable again.
        }
        var level = explorerLevel.get();
        if (level != null) {
            synchronized (this) {
                stack.refresh(level.rows(locale, clock));
                if (selectedId != null) {
                    stack.select(selectedId);
                }
            }
        }
        terminal.raise(Terminal.Signal.WINCH);
    }

    private void refreshPlan(RuntimeClient runtime) {
        var view = currentPlanView.get();
        var level = stack.level();
        if (view == null || level == null) {
            return;
        }
        var planId = level.id();
        if (planId == null) {
            return;
        }
        try {
            var response = runtime.send(HttpRequest.get(PLANS_API_PREFIX
                                + OperatorUrlUtil.encodePathSegment(planId) + "/process"));
            if (response.status() != 200) {
                return;
            }
            var fresh = ProcessTree.fromJson(response.bodyText());
            synchronized (this) {
                view.update(fresh);
                stack.refresh(view.rows(locale));
            }
            terminal.raise(Terminal.Signal.WINCH);
        } catch (IOException | IllegalArgumentException _) {
            // best-effort refresh
        }
    }

    private void refreshEpic() {
        var view = currentEpicView.get();
        if (view == null || view.isEnded()) {
            return;
        }
        try {
            var resource = new WorkActionsClient(context).epic(view.epicId());
            synchronized (this) {
                view.update(resource);
                stack.refresh(view.rows(locale));
            }
            terminal.raise(Terminal.Signal.WINCH);
        } catch (IOException _) {
            // best-effort refresh
        }
    }

    private void refreshActivity(RuntimeClient runtime) {
        var view = currentActivity.get();
        if (view == null) {
            return;
        }
        try {
            var path = PLANS_API_PREFIX + OperatorUrlUtil.encodePathSegment(view.planId())
                                + "/tasks/" + OperatorUrlUtil.encodePathSegment(view.taskId()) + "/activity";
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() != 200) {
                return;
            }
            var fresh = TaskActivity.fromJson(response.bodyText());
            synchronized (this) {
                view.update(fresh);
                var rows = view.rows(locale);
                boolean follow = activityFollow.get();
                stack.refresh(rows);
                if (follow && !rows.isEmpty()) {
                    stack.select(rows.getLast().id());
                }
            }
            terminal.raise(Terminal.Signal.WINCH);
        } catch (IOException | IllegalArgumentException _) {
            // best-effort refresh
        }
    }


    private void openRuntimeActions() {
        var level = currentSystem.get();
        if (level == null) {
            return;
        }
        var entries = new ArrayList<TuiMenuEntry>();
        entries.add(TuiMenuEntry.enabled("s", "stop",
                            MessageCatalogue.format(locale, "system.action.stop", Map.of())));
        entries.add(TuiMenuEntry.enabled("r", "restart",
                            MessageCatalogue.format(locale, "system.action.restart", Map.of())));
        if (level.upgradeSupported()) {
            entries.add(TuiMenuEntry.enabled("u", "upgrade",
                                MessageCatalogue.format(locale, "system.action.upgrade", Map.of())));
        }
        if (level.versionUnknown()) {
            entries.add(TuiMenuEntry.enabled("c", "check",
                                MessageCatalogue.format(locale, "operator.system.action.check", Map.of())));
        }
        runtimeMenu = TuiMenu.opening(SystemLevel.ROW_RUNTIME, entries);
    }

    /**
     * @param trigger the menu's mnemonic, or the empty string to close the menu
     */
    private void handleRuntimeMenu(String trigger) {
        if (trigger.isEmpty()) {
            runtimeMenu = null;
            return;
        }
        var menu = runtimeMenu;
        runtimeMenu = null;
        if (menu == null) {
            return;
        }
        var entry = menu.entry(trigger);
        if (entry == null) {
            return;
        }
        switch (entry.action()) {
            case "stop" -> runDialogue(() -> PmOperator.execute(context, dialogueOut(), dialogueOut(),
                                "runtime", "stop"));
            case "restart" -> runDialogue(() -> PmOperator.execute(context, dialogueOut(), dialogueOut(),
                                "runtime", "restart"));
            case "upgrade" -> runDialogue(() -> PmOperator.execute(context, dialogueOut(), dialogueOut(),
                                "runtime", "upgrade"));
            case "check" -> runDialogue(() -> PmOperator.execute(context, dialogueOut(), dialogueOut(),
                                "runtime", "upgrade", "--check"));
            default -> {
                // An entry this view does not know leaves the view unchanged.
            }
        }
        refreshSystem();
    }

    /**
     * The {@code R} key and the console's {@code reports}: reads the first page of unread reports
     * and opens the reports level above the current one
     * ({@code doc/design/terminal-client/24-reports.adoc#full-screen}).
     */
    void openReports() {
        ReportsPage page = null;
        try {
            page = fetchReportsPage(context.runtime(true));
        } catch (IOException _) {
            // The level opens with what it has; the status line says the read failed.
        }
        var level = page != null
                            ? new ReportsLevel(page.items(), page.unreadCount())
                            : new ReportsLevel(List.of(), unreadReports.get());
        currentReports.set(level);
        var rows = level.rows(locale);
        stack.push(TuiLevel.of(TuiLevel.Kind.REPORTS, "reports", level.breadcrumb(locale)), rows,
                            firstReportRowId(rows));
        if (page == null) {
            statusMessage = MessageCatalogue.format(locale, "report.list.load_failed", Map.of());
        }
        skipReportHeadings(1);
    }

    /**
     * The {@code S} key and the console's {@code system}: reads the snapshot and opens the system
     * level above the current one ({@code doc/design/terminal-client/25-system.adoc#full-screen}).
     *
     * @param initialWindow the window the caller asked for, or {@code null} for the saved one
     * @param initialGroup  the grouping the caller asked for, or {@code null} for the saved one
     */
    void openSystem(@Nullable String initialWindow, @Nullable String initialGroup,
                        @Nullable String initialProject) {
        var saved = stateStore.read().orElse(OverviewSavedState.empty());
        var window = initialWindow != null ? SystemWindow.fromValue(initialWindow)
                            : SystemWindow.fromValue(saved.systemWindow());
        var groupBy = initialGroup != null ? initialGroup : saved.systemGroupBy();
        var projectId = initialProject != null ? initialProject : saved.systemProjectId();
        SystemSnapshot snapshot = null;
        try {
            snapshot = fetchSystemSnapshot(context.runtime(true), window, projectId);
        } catch (IOException _) {
            // The level opens with the failure line; r reads it again.
        }
        var level = snapshot != null
                            ? SystemLevel.root(snapshot, window, groupBy, projectId)
                            : SystemLevel.unavailable(window, groupBy, projectId);
        currentSystem.set(level);
        systemBanner = null;
        stack.push(TuiLevel.of(TuiLevel.Kind.SYSTEM, SystemLevel.LEVEL_ID, level.breadcrumb(locale)),
                            level.rows(locale), SystemLevel.ROW_RUNTIME);
    }

    /**
     * Reads the reports again and shows them without moving the selection off its report.
     */
    void refreshReports() {
        ReportsPage page;
        try {
            page = fetchReportsPage(context.runtime(false));
        } catch (IOException _) {
            return;
        }
        if (page == null) {
            return;
        }
        var level = new ReportsLevel(page.items(), page.unreadCount());
        currentReports.set(level);
        synchronized (this) {
            var lvl = stack.level();
            if (lvl != null && lvl.kind() == TuiLevel.Kind.REPORTS) {
                var selected = stack.selected();
                stack.refresh(level.rows(locale));
                if (selected != null && level.reportOf(selected.id()) != null) {
                    stack.select(selected.id());
                }
                skipReportHeadings(1);
            }
        }
        terminal.raise(Terminal.Signal.WINCH);
    }

    /**
     * Reads the system snapshot again with the operator's window and project.
     */
    void refreshSystem() {
        var level = currentSystem.get();
        if (level == null) {
            return;
        }
        SystemSnapshot snapshot;
        try {
            snapshot = fetchSystemSnapshot(context.runtime(false), level.window(), level.projectId());
        } catch (IOException _) {
            return;
        }
        if (snapshot == null) {
            return;
        }
        var fresh = level.withSnapshot(snapshot);
        if (!currentSystem.compareAndSet(level, fresh)) {
            // The operator changed the window, grouping, or project during the read; their choice
            // and its own read win.
            return;
        }
        synchronized (this) {
            var lvl = stack.level();
            if (lvl == null || lvl.kind() != TuiLevel.Kind.SYSTEM) {
                return;
            }
            var section = SystemLevel.sectionOf(lvl.id());
            var selected = stack.selected();
            stack.refresh(fresh.rows(locale, section));
            if (selected != null) {
                stack.select(selected.id());
            }
        }
        terminal.raise(Terminal.Signal.WINCH);
    }

    /**
     * @param reportId the report
     * @return {@code true} when the runtime accepted the read or the report was read already
     */
    boolean markReportRead(String reportId) {
        try {
            var path = "/api/v1/reports/" + OperatorUrlUtil.encodePathSegment(reportId) + "/read";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, "{}"));
            return response.status() >= 200 && response.status() < 300 || response.status() == 404;
        } catch (IOException _) {
            return false;
        }
    }

    /**
     * @param reportIds the reports the level shows
     * @return {@code true} when the runtime accepted the read
     */
    boolean markReportsRead(List<String> reportIds) {
        if (reportIds.isEmpty()) {
            return false;
        }
        try {
            var request = new ReadReportsRequest(reportIds, false);
            var response = context.runtime(true).send(
                                HttpRequest.json("POST", "/api/v1/reports/read", request.toJson()));
            return response.status() >= 200 && response.status() < 300;
        } catch (IOException _) {
            return false;
        }
    }

    private static @Nullable String firstReportRowId(List<TuiRow> rows) {
        for (var row : rows) {
            if (ReportsLevel.reportIdOf(row.id()) != null) {
                return row.id();
            }
        }
        return null;
    }

    /**
     * Moves the selection off a group heading, as the explorer does for its own headings.
     *
     * @param delta the direction of the movement: {@code 1} down, {@code -1} up
     */
    private void skipReportHeadings(int delta) {
        var lvl = stack.level();
        if (lvl == null || lvl.kind() != TuiLevel.Kind.REPORTS) {
            return;
        }
        int direction = delta == 0 ? 1 : delta;
        boolean reversed = false;
        int guard = 0;
        while (stack.selected() != null && ReportsLevel.isHeading(stack.selected().id()) && guard++ < 100) {
            int before = stack.selectedIndex();
            stack.move(direction);
            if (stack.selectedIndex() == before) {
                if (reversed) {
                    return;
                }
                direction = -direction;
                reversed = true;
            }
        }
    }

    private final class OverviewKeyHandler implements TuiScreenDriver.KeyHandler {

        private OverviewKeyHandler() {
        }

        @Override
        public boolean handle(TuiKeyAction action) {
            synchronized (OverviewFullScreenApp.this) {
                statusMessage = null;
                return handleLocked(action);
            }
        }

        @Override
        public boolean menu(String trigger) {
            synchronized (OverviewFullScreenApp.this) {
                statusMessage = null;
                return handleMenu(trigger);
            }
        }

        @Override
        public boolean menuNavigate(int delta) {
            synchronized (OverviewFullScreenApp.this) {
                if (projectMenu != null) {
                    projectMenu = projectMenu.move(delta);
                } else if (runtimeMenu != null) {
                    runtimeMenu = runtimeMenu.move(delta);
                } else if (actionsMenu != null) {
                    actionsMenu.move(delta);
                }
                return true;
            }
        }

        private boolean handleLocked(TuiKeyAction action) {
            switch (action) {
                case MOVE_UP -> {
                    if (isActivityLevel()) {
                        activityFollow.set(false);
                    }
                    stack.move(-1);
                    skipExplorerHeaders(-1);
                    skipReportHeadings(-1);
                    checkEndedPagination();
                }
                case MOVE_DOWN -> {
                    stack.move(1);
                    skipExplorerHeaders(1);
                    skipReportHeadings(1);
                    if (isActivityLevel()) {
                        activityFollow.set(true);
                    }
                    checkEndedPagination();
                }
                case PAGE_UP -> {
                    if (isActivityLevel()) {
                        activityFollow.set(false);
                    }
                    stack.page(-1);
                    skipExplorerHeaders(-1);
                    skipReportHeadings(-1);
                    checkEndedPagination();
                }
                case PAGE_DOWN -> {
                    stack.page(1);
                    skipExplorerHeaders(1);
                    skipReportHeadings(1);
                    if (isActivityLevel()) {
                        activityFollow.set(true);
                    }
                    checkEndedPagination();
                }
                case FIRST -> {
                    stack.first();
                    skipExplorerHeaders(1);
                    skipReportHeadings(1);
                }
                case LAST -> {
                    stack.last();
                    skipExplorerHeaders(-1);
                    skipReportHeadings(-1);
                    if (isActivityLevel()) {
                        activityFollow.set(true);
                    }
                    checkEndedPagination();
                }
                case HELP -> {
                    stack.toggleHelp();
                    refreshHelpRows();
                }
                case SWITCH_VIEW -> switchView();
                case EPIC -> jumpToEpic();
                case OPEN -> {
                    return openSelected();
                }
                case CLOSE -> closeSelected();
                case ENDED -> openEndedLevel();
                case RELOAD -> refreshActiveLevels();
                case BACK -> {
                    return handleBack();
                }
                case LEAVE -> {
                    return false;
                }
                case REPORTS -> openReports();
                case SYSTEM -> openSystem(null, null, null);
                case MARK_READ -> markSelectedReportRead();
                case MARK_ALL_READ -> markShownReportsRead();
                case WINDOW -> systemCycleWindow();
                case GROUP_BY -> systemToggleGroup();
                case PER_PROJECT -> systemToggleProject();
                case EXPAND_ALL -> expandSelected();
                case WHERE -> planJumpToWhere();
                case NON_LANE -> planToggleNonLane();
                case TOKENS -> planToggleTokens();
                case PROJECT -> jumpToProject();
                case COMPLETION -> planOpenCompletion();
                case ACTIONS -> openActions();
                case WAITS -> openWhatWaits();
                case FAVOURITE -> toggleFavourite();
                case MOVE_FAVOURITE_DOWN -> moveFavourite(1);
                case MOVE_FAVOURITE_UP -> moveFavourite(-1);
                case SORT -> toggleExplorerSort();
                case ADD_PROJECT -> runVerb(PROJECT_VERB, "add");
                case ADD_EPIC -> startWork(true);
                case ADD_PLAN -> startWork(false);
                case RUNNING_WORK -> openRunningWork();
                case SETUP -> openProjectSetup();
                case CONTINUE -> continueProject();
                case ABANDON -> abandonOnboarding();
                case ENROL_AGAIN -> enrolAgain();
                default -> {
                    // unhandled key actions
                }
            }
            return true;
        }

        @Override
        public void filter(String text) {
            synchronized (OverviewFullScreenApp.this) {
                stack.filter(text);
            }
        }

        private void refreshHelpRows() {
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            var kind = lvl.kind();
            var detail = currentProjectDetail.get();
            var ended = currentEndedLevel.get();
            if ((kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) && detail != null) {
                var levelModel = new ProjectWorkLevel(detail, currentByEpic.get(), isCurrentProjectSetupNeedingOperator());
                stack.refresh(levelModel.rows(locale, clock, stack.helpVisible()));
            } else if (kind == TuiLevel.Kind.ENDED && ended != null) {
                stack.refresh(ended.rows(locale, clock, stack.helpVisible()));
            }
        }

        private void switchView() {
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            var kind = lvl.kind();
            if (kind != TuiLevel.Kind.PROJECT_BY_PLAN && kind != TuiLevel.Kind.PROJECT_BY_EPIC) {
                return;
            }
            var detail = currentProjectDetail.get();
            if (detail == null) {
                return;
            }
            boolean newByEpic = !currentByEpic.get();
            currentByEpic.set(newByEpic);
            var sel = stack.selected();
            var selectedId = sel != null ? sel.id() : null;
            var currentFilter = stack.filter();
            var newLevelModel = new ProjectWorkLevel(detail, newByEpic, isCurrentProjectSetupNeedingOperator());
            var newLevel = newLevelModel.level(locale);
            var newRows = newLevelModel.rows(locale, clock, stack.helpVisible());

            if (stack.depth() > 1) {
                stack.filter("");
                stack.back();
                stack.push(newLevel, newRows, selectedId);
                if (!currentFilter.isEmpty()) {
                    stack.filter(currentFilter);
                }
            } else {
                stack.replaceRoot(newLevel, newRows, selectedId);
            }
        }

        private void jumpToEpic() {
            if (isReportsLevel()) {
                openReportEpic();
                return;
            }
            var lvl = stack.level();
            if (lvl != null && lvl.kind() == TuiLevel.Kind.PLAN) {
                try {
                    jumpToPlanEpic(context.runtime(true));
                } catch (IOException _) {
                    // best effort
                }
                return;
            }
            var detail = currentProjectDetail.get();
            if (lvl == null || lvl.kind() != TuiLevel.Kind.PROJECT_BY_PLAN || detail == null) {
                return;
            }
            var selected = stack.selected();
            if (selected == null) {
                return;
            }
            var planId = selected.id();
            var plan = detail.plans().stream()
                                .filter(p -> p.planId().equals(planId))
                                .findFirst()
                                .orElse(null);
            if (plan == null || plan.epicId() == null || plan.epicId().isBlank()) {
                return;
            }

            currentByEpic.set(true);
            var currentFilter = stack.filter();
            var newLevelModel = new ProjectWorkLevel(detail, true, isCurrentProjectSetupNeedingOperator());
            var newLevel = newLevelModel.level(locale);
            var newRows = newLevelModel.rows(locale, clock, stack.helpVisible());

            if (stack.depth() > 1) {
                stack.filter("");
                stack.back();
                stack.push(newLevel, newRows, plan.epicId());
                if (!currentFilter.isEmpty()) {
                    stack.filter(currentFilter);
                }
            } else {
                stack.replaceRoot(newLevel, newRows, plan.epicId());
            }
        }

        private boolean openSelected() {
            var selected = stack.selected();
            var lvl = stack.level();
            if (selected == null || lvl == null) {
                return true;
            }
            var kind = lvl.kind();
            var selectedId = selected.id();

            if (kind == TuiLevel.Kind.PROJECTS) {
                openProjectMenu();
                return true;
            }
            if (kind == TuiLevel.Kind.PLAN) {
                return planOpenSelected(selectedId);
            }
            if (kind == TuiLevel.Kind.ENDED) {
                openEndedSelected(selectedId);
                return true;
            }
            if (kind == TuiLevel.Kind.EPIC) {
                return epicOpenSelected(selectedId);
            }
            if (kind == TuiLevel.Kind.PLAN_ACTIVITY || kind == TuiLevel.Kind.PLAN_COMPLETION) {
                return true;
            }
            if (kind == TuiLevel.Kind.REPORTS) {
                return openSelectedReport();
            }
            if (kind == TuiLevel.Kind.SYSTEM) {
                systemOpenSelected();
                return true;
            }
            if (!selected.openable()) {
                return true;
            }

            if (kind == TuiLevel.Kind.OVERVIEW) {
                try {
                    openProject(context.runtime(true), selectedId, false, null);
                } catch (IOException _) {
                    // best effort
                }
            } else if (kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) {
                // Drill-down to plan or epic detail
                drillDownWorkItem(selectedId);
            }
            return true;
        }

        private boolean planOpenSelected(String selectedId) {
            var view = currentPlanView.get();
            if (view == null) {
                return true;
            }
            var taskId = view.taskIdOf(selectedId);
            if (taskId != null) {
                try {
                    openActivity(context.runtime(true), view.planId(), taskId);
                } catch (IOException _) {
                    // best effort
                }
                return true;
            }
            var waitingPlan = view.waitingPlanOf(selectedId);
            if (waitingPlan != null) {
                pendingAnswerPlanId = waitingPlan;
                return false;
            }
            if (view.openRow(selectedId)) {
                refreshPlanRows();
            }
            return true;
        }

        private void openEndedSelected(String selectedId) {
            var ended = currentEndedLevel.get();
            if (ended == null) {
                return;
            }
            var item = ended.items().stream().filter(candidate -> candidate.id().equals(selectedId))
                                .findFirst().orElse(null);
            if (item == null) {
                return;
            }
            if ("epic".equalsIgnoreCase(item.kind())) {
                if (!openEpicView(item.id())) {
                    openEndedEpicView(item);
                }
                return;
            }
            var endedInfo = new EndedInfo(
                                item.outcome() != null ? item.outcome() : "completed",
                                item.reason(),
                                OverviewFormatting.formatEndedTime(locale, item.endedAt(), clock));
            try {
                openPlan(context.runtime(true), item.id(), endedInfo);
            } catch (IOException _) {
                // best effort
            }
        }

        /**
         * @param rowId the row of the epic's view
         * @return {@code true} to keep the view open
         */
        private boolean epicOpenSelected(String rowId) {
            var view = currentEpicView.get();
            if (view == null) {
                return false;
            }
            var register = view.registerOf(rowId);
            if (register != null) {
                runDialogue(() -> OperatorActions.runReview(context, dialogueOut(), view.epicId(), register));
                return true;
            }
            var planId = view.planIdOf(rowId);
            if (planId != null && !view.isStagedPlanRow(rowId)) {
                try {
                    if (openPlan(context.runtime(true), planId, null)) {
                        return true;
                    }
                } catch (IOException _) {
                    // fall back to the row's details
                }
            }
            if (view.openable(rowId)) {
                view.toggle(rowId, locale);
                stack.refresh(view.rows(locale));
            }
            return true;
        }

        private void closeSelected() {
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            if (lvl.kind() == TuiLevel.Kind.EPIC) {
                var view = currentEpicView.get();
                if (view == null) {
                    return;
                }
                var selected = stack.selected();
                var parent = view.closeRow(selected != null ? selected.id() : null);
                stack.refresh(view.rows(locale));
                if (parent != null) {
                    stack.select(parent);
                }
                return;
            }
            closePlanRow();
        }

        private void expandSelected() {
            var lvl = stack.level();
            var selected = stack.selected();
            if (lvl != null && lvl.kind() == TuiLevel.Kind.EPIC) {
                var view = currentEpicView.get();
                if (view != null) {
                    view.expandAll();
                    stack.refresh(view.rows(locale));
                }
                return;
            }
            if (lvl != null && lvl.kind() == TuiLevel.Kind.PLAN) {
                var view = currentPlanView.get();
                if (view != null) {
                    view.expandAll(selected != null ? selected.id() : null);
                    refreshPlanRows();
                }
            }
        }

        private void jumpToProject() {
            if (isReportsLevel()) {
                openReportProject();
                return;
            }
            var lvl = stack.level();
            if (lvl != null && lvl.kind() == TuiLevel.Kind.EPIC) {
                var view = currentEpicView.get();
                if (view == null) {
                    return;
                }
                // p pushes the project's level, so that Esc returns to the epic the operator came
                // from even when the epic itself was opened from that project.
                try {
                    openProject(context.runtime(true), view.projectId(), false, null);
                } catch (IOException refused) {
                    statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
                }
                return;
            }
            planJumpToProject();
        }

        private boolean openActivity(RuntimeClient runtime, String planId, String taskId) {
            var view = currentPlanView.get();
            if (view == null) {
                return false;
            }
            try {
                var path = PLANS_API_PREFIX + OperatorUrlUtil.encodePathSegment(planId)
                                    + "/tasks/" + OperatorUrlUtil.encodePathSegment(taskId) + "/activity";
                var response = runtime.send(HttpRequest.get(path));
                if (response.status() != 200) {
                    return false;
                }
                var activity = new WorkerActivityLevel(planId, view.nodeIdOfTask(taskId), TaskActivity.fromJson(response.bodyText()));
                currentActivity.set(activity);
                activityFollow.set(true);
                var level = TuiLevel.of(TuiLevel.Kind.PLAN_ACTIVITY, planId + "/" + taskId,
                                    activity.breadcrumb());
                var rows = activity.rows(locale);
                stack.push(level, rows, rows.isEmpty() ? null : rows.getLast().id());
                return true;
            } catch (IOException | IllegalArgumentException _) {
                return false;
            }
        }

        private boolean openCompletion(RuntimeClient runtime, String planId) {
            try {
                var path = PLANS_API_PREFIX + OperatorUrlUtil.encodePathSegment(planId) + "/completion";
                var response = runtime.send(HttpRequest.get(path));
                if (response.status() != 200) {
                    return false;
                }
                var report = PlanCompletionReport.fromJson(response.bodyText());
                var levelModel = new PlanCompletionLevel(report);
                currentCompletion.set(levelModel);
                stack.push(TuiLevel.of(TuiLevel.Kind.PLAN_COMPLETION, planId, levelModel.breadcrumb(locale)),
                                    levelModel.rows(locale), null);
                return true;
            } catch (IOException | IllegalArgumentException _) {
                return false;
            }
        }

        private boolean belowIsProject(TuiLevel.Kind kind, String projectId) {
            if (stack.depth() < 2) {
                return false;
            }
            var below = stack.levelAt(stack.depth() - 2);
            return below != null && below.kind() == kind && projectId.equals(below.id());
        }

        private void jumpToPlanProject(RuntimeClient runtime) {
            var view = currentPlanView.get();
            if (view == null) {
                return;
            }
            if (belowIsProject(TuiLevel.Kind.PROJECT_BY_PLAN, view.projectId())) {
                stack.back();
                return;
            }
            openProject(runtime, view.projectId(), false, null);
        }

        private void jumpToPlanEpic(RuntimeClient runtime) {
            var view = currentPlanView.get();
            if (view == null || view.epicId() == null) {
                return;
            }
            // When the plan was opened from its epic's view, e returns to that view: it is the
            // level below the plan.
            if (belowIsEpic(view.epicId())) {
                stack.back();
                return;
            }
            if (openEpicView(view.epicId())) {
                return;
            }
            // A runtime that does not serve the epic as a resource still shows its plans in the
            // project's view by epic.
            if (belowIsProject(TuiLevel.Kind.PROJECT_BY_EPIC, view.projectId())) {
                stack.back();
                return;
            }
            openProject(runtime, view.projectId(), true, view.epicId());
        }

        private boolean belowIsEpic(String epicId) {
            if (stack.depth() < 2) {
                return false;
            }
            var below = stack.levelAt(stack.depth() - 2);
            return below != null && below.kind() == TuiLevel.Kind.EPIC && epicId.equals(below.id());
        }

        private void refreshPlanRows() {
            var view = currentPlanView.get();
            if (view != null) {
                stack.refresh(view.rows(locale));
            }
        }

        private void closePlanRow() {
            var view = currentPlanView.get();
            var selected = stack.selected();
            if (view == null || selected == null) {
                return;
            }
            var parent = view.closeRow(selected.id());
            refreshPlanRows();
            if (parent != null) {
                stack.select(parent);
            }
        }

        private void planExpandAll() {
            var view = currentPlanView.get();
            var selected = stack.selected();
            if (view == null) {
                return;
            }
            view.expandAll(selected != null ? selected.id() : null);
            refreshPlanRows();
        }

        private void planJumpToWhere() {
            var view = currentPlanView.get();
            if (view == null) {
                return;
            }
            var active = view.jumpToActive();
            refreshPlanRows();
            stack.select(active);
        }

        private void planToggleNonLane() {
            var view = currentPlanView.get();
            if (view == null) {
                return;
            }
            view.toggleNonLane();
            refreshPlanRows();
        }

        private void planToggleTokens() {
            var view = currentPlanView.get();
            if (view == null) {
                return;
            }
            view.toggleTokens();
            refreshPlanRows();
        }

        private void planJumpToProject() {
            var view = currentPlanView.get();
            if (view == null) {
                return;
            }
            try {
                jumpToPlanProject(context.runtime(true));
            } catch (IOException _) {
                // best effort
            }
        }

        private void planOpenCompletion() {
            var view = currentPlanView.get();
            if (view == null || !view.isEnded()) {
                return;
            }
            try {
                openCompletion(context.runtime(true), view.planId());
            } catch (IOException _) {
                // best effort
            }
        }

        private boolean isActivityLevel() {
            var lvl = stack.level();
            return lvl != null && lvl.kind() == TuiLevel.Kind.PLAN_ACTIVITY;
        }

        private void drillDownWorkItem(String itemId) {
            var detail = currentProjectDetail.get();
            var isEpic = detail != null && detail.epics().stream().anyMatch(e -> e.epicId().equals(itemId));
            if (isEpic) {
                // An epic opens its own view; its plans and sections are one level below.
                if (openEpicView(itemId)) {
                    return;
                }
                try {
                    openProject(context.runtime(true), detail.projectId(), true, itemId);
                } catch (IOException _) {
                    // best effort
                }
                return;
            }
            try {
                if (openPlan(context.runtime(true), itemId, null)) {
                    return;
                }
            } catch (IOException _) {
                // fall back to the read-only inspection level
            }
            var lvl = stack.level();
            var levelId = lvl != null ? lvl.id() : "";
            var title = levelId + " \u00b7 " + itemId;
            var inspectLevel = TuiLevel.of(TuiLevel.Kind.PLAN, itemId, title);
            var itemRows = new ArrayList<TuiRow>();
            itemRows.add(new TuiRow(itemId, itemId, null, false, true));
            stack.push(inspectLevel, itemRows, itemId);
        }

        private void openEndedLevel() {
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            var kind = lvl.kind();
            if (kind == TuiLevel.Kind.PROJECTS) {
                var project = selectedExplorerProject();
                if (project != null && isReleased(project)) {
                    openEndedFor(project.projectId());
                }
                return;
            }
            String targetProjectId = null;
            if (kind == TuiLevel.Kind.OVERVIEW) {
                var sel = stack.selected();
                targetProjectId = sel != null ? sel.id() : null;
            } else if (kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) {
                targetProjectId = lvl.id();
            }
            if (targetProjectId != null && !targetProjectId.startsWith("__")) {
                try {
                    openEnded(context.runtime(true), targetProjectId);
                } catch (IOException _) {
                    // best effort
                }
            }
        }

        private boolean isReportsLevel() {
            var lvl = stack.level();
            return lvl != null && lvl.kind() == TuiLevel.Kind.REPORTS;
        }

        private boolean isSystemLevel() {
            var lvl = stack.level();
            return lvl != null && lvl.kind() == TuiLevel.Kind.SYSTEM;
        }

        private void markSelectedReportRead() {
            var level = currentReports.get();
            var selected = stack.selected();
            if (level == null || selected == null) {
                return;
            }
            var reportId = ReportsLevel.reportIdOf(selected.id());
            if (reportId == null) {
                return;
            }
            if (!markReportRead(reportId)) {
                statusMessage = MessageCatalogue.format(locale, "report.list.mark_read_failed", Map.of());
                return;
            }
            var neighbour = level.neighbourOf(reportId, locale);
            applyReports(level.without(reportId), neighbour);
        }

        private void markShownReportsRead() {
            var level = currentReports.get();
            var ids = ReportsLevel.reportIdsIn(stack.visibleRows());
            if (level == null || ids.isEmpty()) {
                return;
            }
            if (!markReportsRead(ids)) {
                statusMessage = MessageCatalogue.format(locale, "report.list.mark_all_failed", Map.of());
                return;
            }
            applyReports(level.withoutAll(ids), null);
        }

        private void applyReports(ReportsLevel level, @Nullable String selectedRowId) {
            currentReports.set(level);
            unreadReports.set(level.unreadCount());
            stack.refresh(level.rows(locale));
            if (selectedRowId != null) {
                stack.select(selectedRowId);
            }
            skipReportHeadings(1);
        }

        /**
         * Opens the report's detail in the same view and reads the report in the same act
         * ({@code doc/design/terminal-client/24-reports.adoc#full-screen}).
         *
         * @return {@code true} to keep the view open
         */
        private boolean openSelectedReport() {
            var level = currentReports.get();
            var selected = stack.selected();
            if (level == null || selected == null) {
                return true;
            }
            var report = level.reportOf(selected.id());
            if (report == null) {
                return true;
            }
            if (!openReportDetail(report)) {
                statusMessage = MessageCatalogue.format(locale, "report.list.load_failed", Map.of());
                return true;
            }
            if (!markReportRead(report.id())) {
                statusMessage = MessageCatalogue.format(locale, "report.list.mark_read_failed", Map.of());
                return true;
            }
            var updated = level.without(report.id());
            currentReports.set(updated);
            unreadReports.set(updated.unreadCount());
            return true;
        }

        private boolean openReportDetail(ReportItem report) {
            var runtime = runtimeQuietly();
            if (runtime == null) {
                return false;
            }
            var planId = trimToNull(report.from().planId());
            var epicId = trimToNull(report.from().epicId());
            if (report.kind() == ReportKind.PLAN_ENDED) {
                return planId != null && openCompletion(runtime, planId);
            }
            if (report.kind() == ReportKind.SYSTEM_CLEANUP_DUE) {
                openSystem(null, null, null);
                return pushSystemSection(SystemLevel.Section.CLEANUP);
            }
            if (planId != null && openPlan(runtime, planId, null)) {
                return true;
            }
            if (epicId != null && openEpicView(epicId)) {
                return true;
            }
            if (!report.from().isSystem()) {
                openProject(runtime, report.from().projectId(), false, null);
                return true;
            }
            return false;
        }

        private void openReportProject() {
            var report = selectedReport();
            if (report == null || report.from().isSystem()) {
                return;
            }
            try {
                openProject(context.runtime(true), report.from().projectId(), false, null);
            } catch (IOException refused) {
                statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
            }
        }

        private void openReportEpic() {
            var report = selectedReport();
            if (report == null) {
                return;
            }
            var epicId = trimToNull(report.from().epicId());
            if (epicId == null) {
                return;
            }
            if (openEpicView(epicId)) {
                return;
            }
            try {
                openProject(context.runtime(true), report.from().projectId(), true, epicId);
            } catch (IOException refused) {
                statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
            }
        }

        private @Nullable ReportItem selectedReport() {
            var level = currentReports.get();
            var selected = stack.selected();
            return level != null && selected != null ? level.reportOf(selected.id()) : null;
        }

        private @Nullable RuntimeClient runtimeQuietly() {
            try {
                return context.runtime(true);
            } catch (IOException _) {
                return null;
            }
        }

        private boolean pushSystemSection(SystemLevel.Section section) {
            var level = currentSystem.get();
            if (level == null) {
                return false;
            }
            var rows = level.rows(locale, section);
            stack.push(TuiLevel.of(TuiLevel.Kind.SYSTEM, SystemLevel.levelId(section),
                                level.breadcrumb(locale, section)), rows, null);
            return true;
        }

        private void systemOpenSelected() {
            var lvl = stack.level();
            var selected = stack.selected();
            if (lvl == null || selected == null) {
                return;
            }
            var section = SystemLevel.sectionOf(lvl.id());
            if (section == SystemLevel.Section.JOBS) {
                if (SystemLevel.ROW_LOCKS.equals(selected.id())) {
                    pushSystemSection(SystemLevel.Section.LOCKS);
                }
                return;
            }
            if (section != SystemLevel.Section.ROOT) {
                return;
            }
            var figure = SystemLevel.figureOf(selected.id());
            if (figure == null) {
                return;
            }
            switch (figure) {
                case RUNTIME -> openRuntimeActions();
                case WORK -> followWorkToOverview();
                case WORKERS -> pushSystemSection(SystemLevel.Section.WORKERS);
                case JOBS -> pushSystemSection(SystemLevel.Section.JOBS);
                case MODELS -> pushSystemSection(SystemLevel.Section.MODELS);
                case CLEANUP -> pushSystemSection(SystemLevel.Section.CLEANUP);
            }
        }

        /**
         * The work figure leads to the overview of the running work: back to it where the system
         * level sits above one, otherwise the overview opens as a level of its own.
         */
        private void followWorkToOverview() {
            if (stack.depth() > 1) {
                var below = stack.levelAt(stack.depth() - 2);
                if (below != null && below.kind() == TuiLevel.Kind.OVERVIEW) {
                    stack.back();
                    refreshPoppedLevel();
                    return;
                }
            }
            var cached = cachedProjects.get();
            var projects = cached != null ? cached.projects() : List.<ProjectSummary>of();
            var levelModel = new ProjectsOverviewLevel(projects);
            stack.push(ProjectsOverviewLevel.level(locale), levelModel.rows(locale, clock), null);
        }

        private void systemCycleWindow() {
            var level = currentSystem.get();
            if (level == null || !isSystemLevel()) {
                return;
            }
            applySystemChoice(level.withChoice(level.nextWindow(), level.groupBy(), level.projectId()));
        }

        private void systemToggleGroup() {
            var level = currentSystem.get();
            if (level == null || !isSystemLevel()) {
                return;
            }
            applySystemChoice(level.withChoice(level.window(), level.nextGroupBy(), level.projectId()));
        }

        private void systemToggleProject() {
            var level = currentSystem.get();
            if (level == null || !isSystemLevel()) {
                return;
            }
            if (level.projectId() != null) {
                applySystemChoice(level.withChoice(level.window(), level.groupBy(), null));
                return;
            }
            var projectId = projectForSystem();
            if (projectId == null) {
                statusMessage = MessageCatalogue.format(locale, "operator.system.per_project_unavailable", Map.of());
                return;
            }
            applySystemChoice(level.withChoice(level.window(), level.groupBy(), projectId));
        }

        private @Nullable String projectForSystem() {
            var initial = initialSelection.project();
            if (initial != null && !initial.isBlank()) {
                return initial;
            }
            var cached = cachedProjects.get();
            return cached != null && !cached.projects().isEmpty()
                                ? cached.projects().getFirst().projectId() : null;
        }

        private void applySystemChoice(SystemLevel chosen) {
            SystemSnapshot snapshot = null;
            try {
                snapshot = fetchSystemSnapshot(context.runtime(true), chosen.window(), chosen.projectId());
            } catch (IOException _) {
                // The choice stands; the figures stay as they were.
            }
            var level = snapshot != null
                                ? chosen.withSnapshot(snapshot)
                                : SystemLevel.unavailable(chosen.window(), chosen.groupBy(), chosen.projectId());
            currentSystem.set(level);
            var lvl = stack.level();
            var section = lvl != null ? SystemLevel.sectionOf(lvl.id()) : SystemLevel.Section.ROOT;
            stack.refresh(level.rows(locale, section));
        }

        private void refreshPoppedLevel() {
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            if (lvl.kind() == TuiLevel.Kind.REPORTS) {
                var level = currentReports.get();
                if (level != null) {
                    stack.refresh(level.rows(locale));
                    skipReportHeadings(1);
                }
            } else if (lvl.kind() == TuiLevel.Kind.SYSTEM) {
                var level = currentSystem.get();
                if (level != null) {
                    stack.refresh(level.rows(locale, SystemLevel.sectionOf(lvl.id())));
                }
            }
        }

        private static @Nullable String trimToNull(@Nullable String value) {
            return value == null || value.isBlank() ? null : value;
        }

        private boolean isExplorerLevel() {
            var level = stack.level();
            return level != null && level.kind() == TuiLevel.Kind.PROJECTS;
        }

        private void skipExplorerHeaders(int delta) {
            if (!isExplorerLevel()) {
                return;
            }
            int direction = delta == 0 ? 1 : delta;
            boolean reversed = false;
            int guard = 0;
            while (stack.selected() != null && ProjectsExplorerLevel.isHeader(stack.selected().id())
                                && guard++ < 100) {
                int before = stack.selectedIndex();
                stack.move(direction);
                if (stack.selectedIndex() == before) {
                    if (reversed) {
                        return;
                    }
                    direction = -direction;
                    reversed = true;
                }
            }
        }

        private @Nullable EnrolledProject selectedExplorerProject() {
            var level = explorerLevel.get();
            if (level == null) {
                return null;
            }
            var selected = stack.selected();
            return selected != null ? level.projectOf(selected.id()) : null;
        }

        private @Nullable BeingAddedProject selectedBeingAdded() {
            var level = explorerLevel.get();
            if (level == null) {
                return null;
            }
            var selected = stack.selected();
            return selected != null ? level.beingAddedOf(selected.id()) : null;
        }

        private static boolean isReleased(EnrolledProject project) {
            return project.isReleased() || project.readiness() == ProjectReadiness.RELEASED;
        }

        private void runVerb(String... args) {
            runDialogue(() -> PmOperator.execute(context, dialogueOut(), dialogueOut(), args));
        }

        private void openProjectMenu() {
            var level = explorerLevel.get();
            var selected = stack.selected();
            if (level == null || selected == null) {
                return;
            }
            var project = level.projectOf(selected.id());
            var being = level.beingAddedOf(selected.id());
            if (project != null) {
                projectMenuProject = project;
                projectMenuBeing = null;
                projectMenuOpenEntries = isReleased(project) ? List.of() : openEntries(project.projectId());
                projectMenu = TuiMenu.opening(selected.id(),
                                    ProjectsExplorerActions.forProject(locale, project, projectMenuOpenEntries));
            } else if (being != null) {
                projectMenuProject = null;
                projectMenuBeing = being;
                projectMenuOpenEntries = List.of();
                projectMenu = TuiMenu.opening(selected.id(),
                                    ProjectsExplorerActions.forBeingAdded(locale));
            } else {
                projectMenu = null;
            }
        }

        private void handleProjectMenu(String trigger) {
            if (trigger.isEmpty()) {
                closeProjectMenu();
                return;
            }
            var menu = projectMenu;
            if (menu == null) {
                return;
            }
            var entry = menu.entry(trigger);
            if (entry == null) {
                return;
            }
            if (!entry.enabled()) {
                statusMessage = entry.reason() != null ? entry.reason() : "";
                return;
            }
            var project = projectMenuProject;
            var being = projectMenuBeing;
            var entries = projectMenuOpenEntries;
            closeProjectMenu();
            runProjectAction(entry.action(), project, being, entries);
            refreshActiveLevels();
        }

        private void closeProjectMenu() {
            projectMenu = null;
            projectMenuProject = null;
            projectMenuBeing = null;
            projectMenuOpenEntries = List.of();
        }

        private void runProjectAction(String action, @Nullable EnrolledProject project,
                            @Nullable BeingAddedProject being, List<ProjectOpenEntry> entries) {
            switch (action) {
                case ProjectsExplorerActions.CONTINUE_STARTING -> continueOpenEntry(project, entries);
                case ProjectsExplorerActions.CONTINUE_SETUP -> runProjectSetup(project);
                case ProjectsExplorerActions.CONTINUE_ONBOARDING -> continueOnboarding(being);
                case ProjectsExplorerActions.ADD_EPIC -> startWork(project, true);
                case ProjectsExplorerActions.ADD_PLAN -> startWork(project, false);
                case ProjectsExplorerActions.RUNNING_WORK -> openRunningWork(project);
                case ProjectsExplorerActions.SETUP -> runProjectSetup(project);
                case ProjectsExplorerActions.FAVOURITE -> toggleFavourite(project);
                case ProjectsExplorerActions.ABANDON -> abandon(being);
                case ProjectsExplorerActions.ENROL_AGAIN -> enrolAgain(project);
                case ProjectsExplorerActions.ENDED_WORK -> openEndedForProject(project);
                default -> {
                    // An action this view does not know leaves the view unchanged.
                }
            }
        }

        private void continueOnboarding(@Nullable BeingAddedProject being) {
            Optional.ofNullable(being).ifPresent(run -> runVerb(PROJECT_VERB, "add", run.checkoutPath()));
        }

        private void openEndedForProject(@Nullable EnrolledProject project) {
            Optional.ofNullable(project).ifPresent(run -> openEndedFor(run.projectId()));
        }

        private void toggleFavourite() {
            toggleFavourite(selectedExplorerProject());
        }

        private void toggleFavourite(@Nullable EnrolledProject project) {
            if (project == null || isReleased(project)) {
                return;
            }
            try {
                var body = new LinkedHashMap<String, Object>();
                body.put("favourite", !project.isFavourite());
                var path = PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(project.projectId())
                                    + "/favourite";
                var response = context.runtime(true).send(HttpRequest.json("PUT", path, JsonTree.write(body)));
                if (response.status() != 200 && response.status() != 204) {
                    statusMessage = MessageCatalogue.format(locale, "project.explorer.favourite_failed", Map.of());
                }
            } catch (IOException _) {
                statusMessage = MessageCatalogue.format(locale, "project.explorer.favourite_failed", Map.of());
            }
            refreshExplorer();
            skipExplorerHeaders(1);
        }

        private void moveFavourite(int delta) {
            var project = selectedExplorerProject();
            var level = explorerLevel.get();
            if (project == null || level == null || !project.isFavourite()) {
                return;
            }
            var selected = stack.selected();
            var selectedRowId = selected != null ? selected.id() : null;
            var favourites = new ArrayList<>(level.explorer().favourites());
            int from = -1;
            for (int index = 0; index < favourites.size(); index++) {
                if (favourites.get(index).projectId().equals(project.projectId())) {
                    from = index;
                    break;
                }
            }
            if (from < 0) {
                return;
            }
            int to = Math.clamp((long) from + delta, 0, favourites.size() - 1);
            if (to == from) {
                return;
            }
            var moved = favourites.remove(from);
            favourites.add(to, moved);
            var previous = level;
            var next = level.withFavourites(favourites);
            explorerLevel.set(next);
            stack.refresh(next.rows(locale, clock));
            if (selectedRowId != null) {
                stack.select(selectedRowId);
            }
            try {
                var ids = favourites.stream().map(EnrolledProject::projectId).toList();
                var response = context.runtime(true).send(HttpRequest.json("PUT", "/api/v1/explorer/favourites",
                                    new FavouritesUpdateRequest(ids).toJson()));
                if (response.status() != 200 && response.status() != 204) {
                    revertFavourites(previous);
                }
            } catch (IOException _) {
                revertFavourites(previous);
            }
        }

        private void revertFavourites(ProjectsExplorerLevel previous) {
            explorerLevel.set(previous);
            stack.refresh(previous.rows(locale, clock));
            statusMessage = MessageCatalogue.format(locale, "project.explorer.reorder_failed", Map.of());
        }

        private void toggleExplorerSort() {
            var level = explorerLevel.get();
            var selected = stack.selected();
            if (level == null) {
                return;
            }
            var next = level.withSortToggled();
            explorerLevel.set(next);
            stack.refresh(next.rows(locale, clock));
            if (selected != null) {
                stack.select(selected.id());
            }
        }

        private List<ProjectOpenEntry> openEntries(String projectId) {
            try {
                var path = PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(projectId);
                var response = context.runtime(true).send(HttpRequest.get(path));
                if (response.status() != 200) {
                    return List.of();
                }
                var detail = JsonTree.asObject(JsonTree.parse(response.bodyText()));
                return detail == null ? List.of() : ProjectOpenEntry.listFromProjectDetail(detail);
            } catch (IOException | IllegalArgumentException _) {
                return List.of();
            }
        }

        private void startWork(boolean epic) {
            startWork(selectedExplorerProject(), epic);
        }

        private void startWork(@Nullable EnrolledProject project, boolean epic) {
            if (project == null || isReleased(project)) {
                return;
            }
            if (project.readiness() == ProjectReadiness.SETUP_NEEDS) {
                statusMessage = ProjectsExplorerActions.notReadyReason(locale, project);
                return;
            }
            runVerb(epic ? "epic" : "plan", "add", "--project", project.projectId());
        }

        private void openRunningWork() {
            openRunningWork(selectedExplorerProject());
        }

        private void openRunningWork(@Nullable EnrolledProject project) {
            if (project == null || isReleased(project)) {
                return;
            }
            try {
                openProject(context.runtime(true), project.projectId(), false, null);
            } catch (IOException refused) {
                statusMessage = OutputSafety.stripControlSequences(refused.getMessage());
            }
        }

        private void openProjectSetup() {
            runProjectSetup(selectedExplorerProject());
        }

        private void runProjectSetup(@Nullable EnrolledProject project) {
            if (project == null || isReleased(project)) {
                return;
            }
            runVerb(PROJECT_VERB, "setup", project.projectId());
        }

        private void enrolAgain() {
            enrolAgain(selectedExplorerProject());
        }

        private void enrolAgain(@Nullable EnrolledProject project) {
            if (project == null || !isReleased(project)) {
                return;
            }
            runVerb(PROJECT_VERB, "setup", project.projectId(), "identity");
        }

        private void continueProject() {
            var being = selectedBeingAdded();
            if (being != null) {
                runVerb(PROJECT_VERB, "add", being.checkoutPath());
                return;
            }
            var project = selectedExplorerProject();
            if (project == null || isReleased(project)) {
                return;
            }
            if (project.readiness() == ProjectReadiness.SETUP_NEEDS) {
                runProjectSetup(project);
                return;
            }
            continueOpenEntry(project, openEntries(project.projectId()));
        }

        private void continueOpenEntry(@Nullable EnrolledProject project, List<ProjectOpenEntry> entries) {
            if (project == null || entries.isEmpty()) {
                return;
            }
            var entry = entries.getFirst();
            var verb = "epic".equalsIgnoreCase(entry.kind()) ? "epic" : "plan";
            runVerb(verb, "add", "--project", project.projectId(), "--entry", entry.entryId());
        }

        private void abandonOnboarding() {
            abandon(selectedBeingAdded());
        }

        private void abandon(@Nullable BeingAddedProject being) {
            if (being == null) {
                return;
            }
            var prompt = MessageCatalogue.format(locale, "operator.projects.select.abandon.confirm",
                                Map.of("path", OutputSafety.stripControlSequences(being.checkoutPath()),
                                                    "keys", MessageCatalogue.format(locale, "operator.actions.yes_no",
                                                                        Map.of())));
            var confirmed = new boolean[1];
            runDialogue(() -> confirmed[0] = askYes(prompt));
            if (!confirmed[0]) {
                return;
            }
            var target = being.abandonLink();
            if (target == null || target.isBlank()) {
                target = "/api/v1/project-onboardings/" + OperatorUrlUtil.encodePathSegment(being.checkoutPath());
            }
            try {
                var response = context.runtime(true).send(HttpRequest.delete(target));
                if (response.status() < 200 || response.status() >= 300) {
                    statusMessage = MessageCatalogue.format(locale, "project.adding.abandon_failed", Map.of());
                }
            } catch (IOException _) {
                statusMessage = MessageCatalogue.format(locale, "project.adding.abandon_failed", Map.of());
            }
            refreshExplorer();
        }

        private boolean askYes(String prompt) throws IOException {
            var driver = activeDriver;
            var confirmTerminal = driver != null ? driver.terminal() : context.createTerminal();
            // A driver owns its terminal; of a terminal the confirmation opened itself, only the
            // one the context owns may be closed (never the console's lent terminal).
            boolean owned = driver == null && context.ownsTerminal(confirmTerminal);
            try {
                var reader = LineReaderBuilder.builder().terminal(confirmTerminal).build();
                var answer = reader.readLine(prompt + "  \u203a ");
                return answer != null && ("y".equalsIgnoreCase(answer.trim())
                                    || "yes".equalsIgnoreCase(answer.trim()));
            } catch (EndOfFileException | UserInterruptException _) {
                return false;
            } finally {
                if (owned) {
                    confirmTerminal.close();
                }
            }
        }

        private void openEndedFor(String projectId) {
            try {
                openEnded(context.runtime(true), projectId);
            } catch (IOException _) {
                // best effort
            }
        }

        private void checkEndedPagination() {
            var lvl = stack.level();
            var ended = currentEndedLevel.get();
            if (lvl == null || lvl.kind() != TuiLevel.Kind.ENDED || ended == null || !ended.hasOlder()) {
                return;
            }
            int idx = stack.selectedIndex();
            int total = stack.visibleRows().size();
            if (idx >= total - 1) {
                try {
                    var nextPage = ended.currentPage() + 1;
                    var path = PROJECTS_API_PREFIX + OperatorUrlUtil.encodePathSegment(ended.projectId())
                                        + "/ended?page=" + nextPage;
                    var response = context.runtime(false).send(HttpRequest.get(path));
                    if (response.status() == 200) {
                        var olderPage = EndedWorkPage.fromJson(response.bodyText());
                        ended.appendOlderPage(olderPage);
                        stack.refresh(ended.rows(locale, clock, stack.helpVisible()));
                    }
                } catch (IOException _) {
                    // ignore pagination fetch failures
                }
            }
        }

        /**
         * @return {@code true} to keep the view open, {@code false} to leave it
         */
        private boolean handleBack() {
            if (stack.helpVisible()) {
                stack.toggleHelp();
                refreshHelpRows();
                return true;
            }
            if (!stack.filter().isEmpty()) {
                stack.back();
                return true;
            }
            if (stack.depth() > 1) {
                stack.back();
                refreshPoppedLevel();
                return true;
            }
            // A plan opened by name is the whole view; there is no caller to return to.
            var lvl = stack.level();
            return lvl == null || lvl.kind() != TuiLevel.Kind.PLAN;
        }
    }
}
