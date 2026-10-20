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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.ConsoleNotice;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OperatorUrlUtil;
import de.planmarshall.operator.OverviewSavedState;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiLevel;
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
    private static final String DOT_SEPARATOR = " \u00b7 ";

    /**
     * Initial CLI selection state.
     *
     * @param project optional initial project ID
     * @param by      optional initial view grouping (epic/plan)
     * @param ended   whether initial level is ended work
     */
    public record InitialSelection(@Nullable String project, @Nullable String by, boolean ended) {
        public static final InitialSelection DEFAULT = new InitialSelection(null, null, false);
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
    private final AtomicReference<ProjectDetailResponse> currentProjectDetail = new AtomicReference<>();
    private final AtomicBoolean currentByEpic = new AtomicBoolean(false);
    private final AtomicReference<EndedWorkLevel> currentEndedLevel = new AtomicReference<>();

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
            return driver.run(this::buildScreen, new OverviewKeyHandler());
        } finally {
            running.set(false);
            sseThread.interrupt();
            saveState();
            noticeBuffer.deactivate();
            var drained = noticeBuffer.drain(locale);
            if (!drained.summary().isEmpty()) {
                out.println(drained.summary());
            }
            for (var line : drained.lines()) {
                out.println(line);
            }
            out.flush();
        }
    }

    void loadInitialData(RuntimeClient runtime) {
        fetchProjects(runtime);
        fetchUnreadReports(runtime);

        var cached = cachedProjects.get();
        var projects = cached != null ? cached.projects() : List.<ProjectSummary>of();
        var rootLevel = new ProjectsOverviewLevel(projects);
        var rootRows = rootLevel.rows(locale, clock);

        var saved = stateStore.read().orElse(OverviewSavedState.empty());

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

    void fetchUnreadReports(RuntimeClient runtime) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/reports"));
            if (response.status() == 200) {
                var page = ReportsPage.fromJson(response.bodyText());
                unreadReports.set(page.unreadCount());
            }
        } catch (IOException | IllegalArgumentException _) {
            // unread count defaults to 0
        }
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

    synchronized TuiScreen buildScreen() {
        var lvl = stack.level();
        var breadcrumb = lvl != null ? lvl.title() : "";
        var kind = lvl != null ? lvl.kind() : TuiLevel.Kind.OVERVIEW;
        return new TuiScreen(
                            breadcrumb,
                            kind,
                            stack.visibleRows(),
                            stack.selectedIndex(),
                            stack.filter(),
                            stack.helpVisible(),
                            connected.get(),
                            unreadReports.get()
        );
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
        if (data == null || data.isBlank()) {
            return;
        }
        Map<String, Object> eventMap;
        try {
            eventMap = JsonTree.asObject(JsonTree.parse(data));
        } catch (IOException _) {
            return;
        }
        if (eventMap == null) {
            return;
        }

        var notice = ConsoleNotice.fromEventMap(locale, eventMap);
        if (notice != null && !context.isConsoleActive()) {
            var category = categoryOf(eventMap);
            int count = countOf(eventMap, category);
            noticeBuffer.hold(category, count);
        }

        var name = eventName != null ? eventName : "";
        var type = String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));

        if (isStructuralChange(name, type)) {
            refreshActiveLevels();
        }
    }

    static boolean isStructuralChange(String name, String type) {
        if (name.startsWith("project.") || name.startsWith("phase.") || name.startsWith("scope.")
                            || name.startsWith("hold.") || name.startsWith("condition.") || name.startsWith("task.")
                            || "needs_operator.changed".equals(name) || "reset".equals(name)) {
            return true;
        }
        return type.startsWith("project.") || type.startsWith("phase.") || type.startsWith("scope.")
                            || type.startsWith("hold.") || type.startsWith("condition.") || "reset".equals(type);
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

        try {
            var runtime = context.runtime(false);
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

    void saveState() {
        try {
            var existing = stateStore.read().orElse(OverviewSavedState.empty());
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            var kind = lvl.kind();
            var sel = stack.selected();
            var selectedId = sel != null ? sel.id() : null;

            String projectId = null;
            String projectView = null;

            if (kind == TuiLevel.Kind.PROJECT_BY_PLAN || kind == TuiLevel.Kind.PROJECT_BY_EPIC) {
                projectId = lvl.id();
                projectView = kind == TuiLevel.Kind.PROJECT_BY_EPIC ? "epic" : "plan";
            }

            var updated = new OverviewSavedState(
                                projectId,
                                projectView,
                                selectedId,
                                existing.systemWindow(),
                                existing.systemGroupBy(),
                                existing.systemProjectId()
            );
            stateStore.write(updated);
        } catch (IOException _) {
            // best effort persistence
        }
    }

    static ConsoleNoticeBuffer.Category categoryOf(Map<String, Object> eventMap) {
        var type = String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));
        return switch (type) {
            case "question", "decision", "waiver_request" -> ConsoleNoticeBuffer.Category.DECISION;
            case "report", "report.sent" -> ConsoleNoticeBuffer.Category.REPORT;
            default -> ConsoleNoticeBuffer.Category.NEED;
        };
    }

    static int countOf(Map<String, Object> eventMap, ConsoleNoticeBuffer.Category category) {
        if (category == ConsoleNoticeBuffer.Category.REPORT
                            && eventMap.get("count") instanceof Number number && number.intValue() > 1) {
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

    private final class OverviewKeyHandler implements TuiScreenDriver.KeyHandler {

        @Override
        public boolean handle(TuiKeyAction action) {
            synchronized (OverviewFullScreenApp.this) {
                return handleLocked(action);
            }
        }

        private boolean handleLocked(TuiKeyAction action) {
            switch (action) {
                case MOVE_UP -> {
                    stack.move(-1);
                    checkEndedPagination();
                }
                case MOVE_DOWN -> {
                    stack.move(1);
                    checkEndedPagination();
                }
                case PAGE_UP -> {
                    stack.page(-1);
                    checkEndedPagination();
                }
                case PAGE_DOWN -> {
                    stack.page(1);
                    checkEndedPagination();
                }
                case FIRST -> stack.first();
                case LAST -> {
                    stack.last();
                    checkEndedPagination();
                }
                case HELP -> {
                    stack.toggleHelp();
                    refreshHelpRows();
                }
                case SWITCH_VIEW -> switchView();
                case EPIC -> jumpToEpic();
                case OPEN -> openSelected();
                case ENDED -> openEndedLevel();
                case RELOAD -> refreshActiveLevels();
                case BACK -> handleBack();
                case LEAVE -> {
                    return false;
                }
                case REPORTS -> openReports();
                case SYSTEM -> openSystem();
                case ACTIONS, WAITS -> {
                    // hooks for actions and decision answering
                }
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
            var lvl = stack.level();
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

        private void openSelected() {
            var selected = stack.selected();
            if (selected == null || !selected.openable()) {
                return;
            }
            var lvl = stack.level();
            if (lvl == null) {
                return;
            }
            var kind = lvl.kind();
            var selectedId = selected.id();

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
        }

        private void drillDownWorkItem(String itemId) {
            // Drill down to read-only inspection level
            var lvl = stack.level();
            var levelId = lvl != null ? lvl.id() : "";
            var title = levelId + DOT_SEPARATOR + itemId;
            var detail = currentProjectDetail.get();
            var isEpic = detail != null && detail.epics().stream().anyMatch(e -> e.epicId().equals(itemId));
            var levelKind = isEpic ? TuiLevel.Kind.EPIC : TuiLevel.Kind.PLAN;
            var inspectLevel = TuiLevel.of(levelKind, itemId, title);
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

        private void openReports() {
            var title = MessageCatalogue.format(locale, "report.list.empty", Map.of());
            var reportsLevel = TuiLevel.of(TuiLevel.Kind.REPORTS, "reports", "Reports");
            var rows = List.of(new TuiRow("rep-0", title, null, false, false));
            stack.push(reportsLevel, rows, null);
        }

        private void openSystem() {
            var systemLevel = TuiLevel.of(TuiLevel.Kind.SYSTEM, "system", "System");
            var rows = List.of(new TuiRow("sys-0", "System view", null, false, false));
            stack.push(systemLevel, rows, null);
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

        private void handleBack() {
            if (stack.helpVisible()) {
                stack.toggleHelp();
                refreshHelpRows();
            } else if (!stack.filter().isEmpty()) {
                stack.back();
            } else if (stack.depth() > 1) {
                stack.back();
            }
        }
    }
}
