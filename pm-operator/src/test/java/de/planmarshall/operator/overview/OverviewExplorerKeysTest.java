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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.projects.ProjectsExplorerLevel;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiMenuEntry;
import de.planmarshall.operator.tui.TuiRow;
import de.planmarshall.operator.tui.TuiScreen;
import de.planmarshall.operator.tui.TuiScreenDriver;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The keys of the projects explorer ({@code doc/design/terminal-client/17-projects.adoc#explorer}),
 * driven through the view's key handler without a screen driver: each key's effect is read from the
 * view's next drawing and from the calls the runtime received.
 */
@DisplayName("OverviewExplorerKeysTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewExplorerKeysTest {

    private static final String PROJECTS_PATH = "/api/v1/projects";
    private static final String FAVOURITES_PATH = "/api/v1/explorer/favourites";

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> statusOf = new ConcurrentHashMap<>();
    private final Map<String, String> bodyOf = new ConcurrentHashMap<>();

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private OperatorContext context;
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;

    private String ready;
    private String second;
    private String notReady;
    private String released;
    private String checkout;

    @BeforeEach
    void setUp() throws IOException {
        ready = "ready-" + Generators.letterStrings(4, 8).next().toLowerCase();
        second = "second-" + Generators.letterStrings(4, 8).next().toLowerCase();
        notReady = "setup-" + Generators.letterStrings(4, 8).next().toLowerCase();
        released = "released-" + Generators.letterStrings(4, 8).next().toLowerCase();
        checkout = "/home/op/git/" + Generators.letterStrings(4, 8).next().toLowerCase();
        bodyOf.put(PROJECTS_PATH, explorer(true));
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        fixture.start(this::handle);
        virtual = VirtualScreenTerminal.create(120, 30);
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        context = new OperatorContext(env, fixture.base().getParent(), true, ClientEnvironment.capture(env),
                            virtual::terminal, null);
        // The view lends its terminal to the dialogues it opens; the confirmation reads from it.
        context.useTerminal(virtual.terminal());
        app = new OverviewFullScreenApp(context, virtual.terminal(),
                            OverviewFullScreenApp.InitialSelection.explorerRoot(), clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
    }

    @AfterEach
    void tearDown() throws IOException {
        virtual.close();
        fixture.close();
    }

    private String explorer(boolean withBeingAdded) {
        var row = "{\"project_id\":\"%s\",\"display_name\":\"%s\",\"readiness\":\"%s\",\"is_favourite\":%s,"
                            + "\"is_released\":%s%s}";
        var readyRow = row.formatted(ready, ready, "ready", true, false, "");
        var secondRow = row.formatted(second, second, "ready", true, false, "");
        var notReadyRow = row.formatted(notReady, notReady, "setup_needs", false, false,
                            ",\"setup_needs_summary\":\"credential sonar\"");
        var releasedRow = row.formatted(released, released, "released", false, true, "");
        var being = withBeingAdded
                            ? "{\"checkout_path\":\"" + checkout + "\",\"current_step\":\"Identity\",\"step_number\":2,"
                            + "\"total_steps\":10,\"abandon_link\":\"/api/v1/entries/ent-1/abandon\"}"
                            : "";
        return "{\"favourites\":[" + readyRow + "," + secondRow + "],\"recent\":[" + notReadyRow + "],"
                            + "\"all_projects\":[" + readyRow + "," + secondRow + "," + notReadyRow + "," + releasedRow + "],"
                            + "\"being_added\":[" + being + "]}";
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        calls.add(request.method() + " " + path + (request.bodyText().isEmpty() ? "" : " " + request.bodyText()));
        var status = statusOf.get(path);
        if (status != null) {
            response.json(status, "{\"error\":\"internal_fault\"}");
            return;
        }
        var body = bodyOf.get(path);
        if (body != null) {
            response.json(200, body);
        } else if (path.endsWith("/favourite") || FAVOURITES_PATH.equals(path)) {
            response.json(200, "{}");
        } else if (path.startsWith("/api/v1/entries/") || path.startsWith("/api/v1/project-onboardings/")) {
            response.send(204, null, "");
        } else if (path.endsWith("/ended?page=1")) {
            response.json(200, "{\"project_id\":\"" + released + "\",\"page\":1,\"total_pages\":1,"
                                + "\"has_newer\":false,\"has_older\":false,\"items\":[]}");
        } else if (path.startsWith(PROJECTS_PATH + "/")) {
            var id = path.substring(PROJECTS_PATH.length() + 1);
            response.json(200, "{\"project_id\":\"" + id + "\",\"display_name\":\"" + id + "\",\"open_entries\":[],"
                                + "\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
        } else if ("/api/v1/reports".equals(path)) {
            response.json(200, "{\"unread_count\":0,\"items\":[]}");
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
    }

    private void select(String group, String projectId) {
        app.stack().select(ProjectsExplorerLevel.projectRowId(group, projectId));
        assertEquals(ProjectsExplorerLevel.projectRowId(group, projectId), app.stack().selected().id());
    }

    private void selectBeingAdded() {
        app.stack().select(ProjectsExplorerLevel.BEING_ADDED_PREFIX + checkout);
        assertEquals(ProjectsExplorerLevel.BEING_ADDED_PREFIX + checkout, app.stack().selected().id());
    }

    private TuiScreen screen() {
        return app.buildScreen();
    }

    private String statusLine() {
        var rows = screen().rows();
        return rows.isEmpty() ? "" : rows.getLast().line().trim();
    }

    private List<String> sent(String fragment) {
        return calls.stream().filter(call -> !call.startsWith("GET ") && call.contains(fragment)).toList();
    }

    private List<String> triggers() {
        var menu = screen().menu();
        assertNotNull(menu, "the menu is open");
        return menu.entries().stream().map(TuiMenuEntry::trigger).toList();
    }

    @Nested
    @DisplayName("the actions menu of a row")
    class ActionsMenu {

        @Test
        @DisplayName("Enter on a ready project opens its actions, and r there opens its running work")
        void runningWorkFromMenu() {
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);

            assertTrue(keys.handle(TuiKeyAction.OPEN));
            assertEquals(List.of("e", "p", "r", "u", "*"), triggers());

            assertTrue(keys.menu("r"));
            assertNull(screen().menu());
            assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
            assertEquals(ready, app.stack().level().id());
        }

        @Test
        @DisplayName("r on a project whose read the runtime refuses stays in the explorer and names the refusal")
        void runningWorkRefused() {
            statusOf.put(PROJECTS_PATH + "/" + ready, 500);
            var refusal = ErrorTexts.title("en", "internal_fault") + " " + ErrorTexts.cause("en", "internal_fault");
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);

            keys.handle(TuiKeyAction.OPEN);
            assertTrue(keys.menu("r"));
            var fromMenu = statusLine();
            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
            assertTrue(keys.handle(TuiKeyAction.RUNNING_WORK));

            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
            assertEquals(refusal, fromMenu);
            assertEquals(refusal, statusLine());
        }

        @Test
        @DisplayName("a disabled entry shows its reason and keeps the menu open")
        void disabledEntryNamesReason() {
            select(ProjectsExplorerLevel.GROUP_RECENT, notReady);
            keys.handle(TuiKeyAction.OPEN);
            assertEquals(List.of("c", "e", "p", "r", "u", "*"), triggers());

            assertTrue(keys.menu("e"));

            assertEquals(notReady + " is not ready: credential sonar", statusLine());
            assertNotNull(screen().menu());
            assertEquals(List.of(), sent("/"));
        }

        @Test
        @DisplayName("a key the menu does not hold leaves it open, Esc closes it, and the arrows move in it")
        void unknownKeyEscAndArrows() {
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);
            keys.handle(TuiKeyAction.OPEN);

            assertTrue(keys.menu(Generators.fixedValues("z", "q", "x", "a").next()));
            assertNotNull(screen().menu());

            assertTrue(keys.menuNavigate(2));
            assertEquals(2, screen().menu().selectedIndex());
            assertTrue(keys.menuNavigate(-5));
            assertEquals(0, screen().menu().selectedIndex());

            assertTrue(keys.menu(""));
            assertNull(screen().menu());
            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
            assertEquals(List.of(), sent("/"));
        }

        @Test
        @DisplayName("* in the menu unmarks a favourite and closes the menu")
        void favouriteFromMenu() {
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);
            keys.handle(TuiKeyAction.OPEN);

            keys.menu("*");

            assertNull(screen().menu());
            assertEquals(List.of("PUT /api/v1/projects/" + ready + "/favourite {\"favourite\":false}"),
                                sent("/favourite"));
        }

        @Test
        @DisplayName("E in a released project's menu opens its ended work")
        void endedWorkFromMenu() {
            select(ProjectsExplorerLevel.GROUP_ALL, released);
            keys.handle(TuiKeyAction.OPEN);
            assertEquals(List.of("a", "E"), triggers());

            keys.menu("E");

            assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
        }

        @Test
        @DisplayName("Enter on a project being added offers Continue and Abandon, and x there asks first")
        void beingAddedMenu() {
            selectBeingAdded();
            keys.handle(TuiKeyAction.OPEN);
            assertEquals(List.of("c", "x"), triggers());
            virtual.type("n\n");

            keys.menu("x");

            assertNull(screen().menu());
            assertEquals(List.of(), sent("/abandon"));
        }

        @Test
        @DisplayName("Enter on a group header opens no menu")
        void headerOpensNoMenu() {
            app.stack().select(ProjectsExplorerLevel.GROUP_FAVOURITES_ID);

            assertTrue(keys.handle(TuiKeyAction.OPEN));

            assertNull(screen().menu());
        }
    }

    @Nested
    @DisplayName("the entries of the menu hand over to the dialogue they name")
    class MenuHandOffs {

        private void runEntry(String group, String projectId, String trigger) {
            select(group, projectId);
            keys.handle(TuiKeyAction.OPEN);
            assertTrue(triggers().contains(trigger), triggers().toString());
            virtual.endOfInput();
            calls.clear();

            assertTrue(keys.menu(trigger));

            assertNull(screen().menu());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"e starts an epic, e", "p starts a plan, p"})
        @DisplayName("e and p start work in the ready project")
        void startWork(String name, String trigger) {
            runEntry(ProjectsExplorerLevel.GROUP_FAVOURITES, ready, trigger);

            assertFalse(sent("/api/v1/projects/" + ready + "/entries").isEmpty(), name + ": " + calls);
        }

        @Test
        @DisplayName("u runs the ready project's setup")
        void setup() {
            runEntry(ProjectsExplorerLevel.GROUP_FAVOURITES, ready, "u");

            assertTrue(calls.contains("GET " + PROJECTS_PATH + "/" + ready + "/setup"), calls.toString());
        }

        @Test
        @DisplayName("c on a project whose setup waits continues that setup")
        void continueSetup() {
            runEntry(ProjectsExplorerLevel.GROUP_RECENT, notReady, "c");

            assertTrue(calls.contains("GET " + PROJECTS_PATH + "/" + notReady + "/setup"), calls.toString());
        }

        @Test
        @DisplayName("a on a released project enrols it again")
        void enrolAgain() {
            runEntry(ProjectsExplorerLevel.GROUP_ALL, released, "a");

            assertFalse(sent(released).isEmpty(), calls.toString());
        }

        @Test
        @DisplayName("c on a ready project with a start still open reads that start to continue it")
        void continueOpenStart() {
            var entry = "ent-" + Generators.letterStrings(4, 8).next().toLowerCase();
            bodyOf.put(PROJECTS_PATH + "/" + ready, "{\"project_id\":\"" + ready + "\",\"display_name\":\"" + ready
                                + "\",\"open_entries\":[{\"entry_id\":\"" + entry + "\",\"kind\":\"plan\","
                                + "\"source_count\":1}],\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);
            virtual.endOfInput();
            calls.clear();

            assertTrue(keys.handle(TuiKeyAction.CONTINUE));

            assertEquals(List.of("GET " + PROJECTS_PATH + "/" + ready, "GET /api/v1/entries/" + entry), calls);
        }

        @Test
        @DisplayName("c on a project being added continues its onboarding")
        void continueOnboarding() {
            selectBeingAdded();
            keys.handle(TuiKeyAction.OPEN);
            virtual.endOfInput();
            calls.clear();

            assertTrue(keys.menu("c"));

            assertNull(screen().menu());
            assertTrue(calls.stream().anyMatch(call -> !call.startsWith("GET ") && call.contains(checkout)),
                                calls.toString());
        }
    }

    @Nested
    @DisplayName("the favourite keys")
    class Favourites {

        @Test
        @DisplayName("* marks a project that is not a favourite")
        void marks() {
            select(ProjectsExplorerLevel.GROUP_RECENT, notReady);

            keys.handle(TuiKeyAction.FAVOURITE);

            assertEquals(List.of("PUT /api/v1/projects/" + notReady + "/favourite {\"favourite\":true}"),
                                sent("/favourite"));
            assertFalse(statusLine().contains("Failed"), statusLine());
        }

        @Test
        @DisplayName("* on a released project sends nothing")
        void releasedSendsNothing() {
            select(ProjectsExplorerLevel.GROUP_ALL, released);

            keys.handle(TuiKeyAction.FAVOURITE);

            assertEquals(List.of(), sent("/favourite"));
        }

        @Test
        @DisplayName("* the runtime refuses says that the favourite was not updated")
        void refusedToggle() {
            statusOf.put(PROJECTS_PATH + "/" + ready + "/favourite", 500);
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);

            keys.handle(TuiKeyAction.FAVOURITE);

            assertEquals("Failed to update favourite", statusLine());
        }

        @Test
        @DisplayName("K moves the second favourite up and saves the new order")
        void movesUp() {
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, second);

            keys.handle(TuiKeyAction.MOVE_FAVOURITE_UP);

            assertEquals(List.of("PUT " + FAVOURITES_PATH + " {\"project_ids\":[\"" + second + "\",\"" + ready + "\"]}"),
                                sent(FAVOURITES_PATH));
            assertEquals(ProjectsExplorerLevel.projectRowId(ProjectsExplorerLevel.GROUP_FAVOURITES, second),
                                app.stack().selected().id());
        }

        @Test
        @DisplayName("J on the last favourite and K on the first stay where they are and send nothing")
        void edgesStay() {
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, second);
            keys.handle(TuiKeyAction.MOVE_FAVOURITE_DOWN);
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);
            keys.handle(TuiKeyAction.MOVE_FAVOURITE_UP);

            assertEquals(List.of(), sent(FAVOURITES_PATH));
        }

        @Test
        @DisplayName("J on a project that is not a favourite sends nothing")
        void notAFavourite() {
            select(ProjectsExplorerLevel.GROUP_RECENT, notReady);

            keys.handle(TuiKeyAction.MOVE_FAVOURITE_DOWN);

            assertEquals(List.of(), sent(FAVOURITES_PATH));
        }

        @Test
        @DisplayName("a refused reorder restores the order the operator had and says so")
        void refusedReorder() {
            statusOf.put(FAVOURITES_PATH, 500);
            select(ProjectsExplorerLevel.GROUP_FAVOURITES, ready);
            var before = favouriteOrder();

            keys.handle(TuiKeyAction.MOVE_FAVOURITE_DOWN);

            assertEquals(1, sent(FAVOURITES_PATH).size());
            assertEquals(before, favouriteOrder());
            assertEquals("The new order of the favourites could not be saved", statusLine());
        }

        private List<String> favouriteOrder() {
            var prefix = ProjectsExplorerLevel.PROJECT_PREFIX + ProjectsExplorerLevel.GROUP_FAVOURITES + ":";
            return app.stack().rows().stream().map(TuiRow::id).filter(id -> id.startsWith(prefix)).toList();
        }
    }

    @Nested
    @DisplayName("the direct keys of a row")
    class DirectKeys {

        @Test
        @DisplayName("e and p on a project that is not ready name why and start nothing")
        void notReadyAdd() {
            select(ProjectsExplorerLevel.GROUP_RECENT, notReady);

            keys.handle(Generators.fixedValues(TuiKeyAction.ADD_EPIC, TuiKeyAction.ADD_PLAN).next());

            assertEquals(notReady + " is not ready: credential sonar", statusLine());
        }

        @Test
        @DisplayName("e, p, r, u and c on a released project do nothing")
        void releasedOffersNoWork() {
            select(ProjectsExplorerLevel.GROUP_ALL, released);
            var before = calls.size();

            for (var action : List.of(TuiKeyAction.ADD_EPIC, TuiKeyAction.ADD_PLAN, TuiKeyAction.RUNNING_WORK,
                                TuiKeyAction.SETUP, TuiKeyAction.CONTINUE)) {
                assertTrue(keys.handle(action));
            }

            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
            assertEquals(before, calls.size(), calls.toString());
        }

        @Test
        @DisplayName("r opens the selected project's running work")
        void runningWork() {
            select(ProjectsExplorerLevel.GROUP_ALL, second);

            keys.handle(TuiKeyAction.RUNNING_WORK);

            assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
            assertEquals(second, app.stack().level().id());
        }

        @Test
        @DisplayName("E opens a released project's ended work and does nothing on a ready one")
        void endedOnlyWhenReleased() {
            select(ProjectsExplorerLevel.GROUP_ALL, ready);
            keys.handle(TuiKeyAction.ENDED);
            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());

            select(ProjectsExplorerLevel.GROUP_ALL, released);
            keys.handle(TuiKeyAction.ENDED);
            assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
        }

        @Test
        @DisplayName("c on a ready project without an open start reads its entries and starts nothing")
        void continueWithoutEntry() {
            select(ProjectsExplorerLevel.GROUP_ALL, ready);

            keys.handle(TuiKeyAction.CONTINUE);

            assertTrue(calls.contains("GET " + PROJECTS_PATH + "/" + ready), calls.toString());
            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
        }

        @Test
        @DisplayName("a and x act only on the rows they belong to")
        void enrolAgainAndAbandonOnOtherRows() {
            select(ProjectsExplorerLevel.GROUP_ALL, ready);
            var before = calls.size();

            keys.handle(TuiKeyAction.ENROL_AGAIN);
            keys.handle(TuiKeyAction.ABANDON);

            assertEquals(before, calls.size(), calls.toString());
            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
        }

        @Test
        @DisplayName("s orders All projects by most used and back by name, keeping the selection")
        void sortKeepsSelection() {
            select(ProjectsExplorerLevel.GROUP_ALL, second);

            keys.handle(TuiKeyAction.SORT);
            assertTrue(screen().rows().stream().anyMatch(row -> row.line().contains("most used")));
            keys.handle(TuiKeyAction.SORT);

            assertTrue(screen().rows().stream().anyMatch(row -> row.line().contains("by name")));
            assertEquals(ProjectsExplorerLevel.projectRowId(ProjectsExplorerLevel.GROUP_ALL, second),
                                app.stack().selected().id());
        }
    }

    @Nested
    @DisplayName("abandoning an onboarding")
    class Abandon {

        @Test
        @DisplayName("y deletes the run at the link the runtime names")
        void confirmed() {
            selectBeingAdded();
            virtual.type("y\n");

            keys.handle(TuiKeyAction.ABANDON);

            assertEquals(List.of("DELETE /api/v1/entries/ent-1/abandon"), sent("/abandon"));
            assertFalse(statusLine().contains("Failed"), statusLine());
        }

        @Test
        @DisplayName("the end of the input leaves the confirmation and deletes nothing")
        void endOfInput() {
            selectBeingAdded();
            virtual.endOfInput();

            keys.handle(TuiKeyAction.ABANDON);

            assertEquals(List.of(), sent("/abandon"));
        }

        @Test
        @DisplayName("a refused delete says that the onboarding was not abandoned")
        void refused() {
            statusOf.put("/api/v1/entries/ent-1/abandon", 500);
            selectBeingAdded();
            virtual.type("yes\n");

            keys.handle(TuiKeyAction.ABANDON);

            assertEquals("Failed to abandon onboarding", statusLine());
        }
    }

    @Test
    @DisplayName("a run without an abandon link is deleted at its onboarding, and the explorer refuses no more")
    void abandonWithoutLink() throws Exception {
        bodyOf.put(PROJECTS_PATH, explorer(true).replace(",\"abandon_link\":\"/api/v1/entries/ent-1/abandon\"", ""));
        app = new OverviewFullScreenApp(context, virtual.terminal(),
                            OverviewFullScreenApp.InitialSelection.explorerRoot(), clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
        selectBeingAdded();
        virtual.type("y\n");

        keys.handle(TuiKeyAction.ABANDON);

        assertEquals(1, sent("/api/v1/project-onboardings/").size(), calls.toString());
        assertTrue(sent("/api/v1/project-onboardings/").getFirst().startsWith("DELETE "), calls.toString());
    }
}
