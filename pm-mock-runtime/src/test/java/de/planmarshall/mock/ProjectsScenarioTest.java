/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.FavouritesUpdateRequest;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.api.project.ProjectReleaseRequest;
import de.planmarshall.api.project.ProjectReleaseResult;

@DisplayName("ProjectsScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ProjectsScenarioTest {

    private static final String SCENARIO = StandardScenarios.PROJECTS_AND_START;
    private static final String PROJECTS_STATE = SCENARIO + "#projects.explorer";

    @Test
    @DisplayName("loads projects scenario and returns complete project explorer")
    void shouldLoadScenarioAndReturnExplorer() throws Exception {
        try (var runtime = MockRuntime.start(PROJECTS_STATE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(SCENARIO, runtime.currentScenarioName());
            assertEquals("projects.explorer", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var explorer = ProjectExplorer.fromJson(res.bodyText());
            assertNotNull(explorer);

            assertAll(
                    () -> assertEquals(2, explorer.favourites().size()),
                    () -> assertEquals("pm-mcp-clients", explorer.favourites().get(0).projectId()),
                    () -> assertEquals("pm-mcp-core", explorer.favourites().get(1).projectId()),
                    () -> assertEquals(2, explorer.recent().size()),
                    () -> assertEquals("plan-marshall", explorer.recent().get(0).projectId()),
                    () -> assertEquals("cui-http", explorer.recent().get(1).projectId()),
                    () -> assertEquals(6, explorer.allProjects().size()),
                    () -> assertEquals(1, explorer.beingAdded().size()),
                    () -> assertEquals("~/git/cui-parent", explorer.beingAdded().get(0).checkoutPath())
            );
        }
    }

    @Test
    @DisplayName("updates favourites ordering and toggles project favourite")
    void shouldUpdateFavourites() throws Exception {
        try (var runtime = MockRuntime.start(PROJECTS_STATE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // 1. Reorder favourites and add cui-http
            var favReq = new FavouritesUpdateRequest(List.of("pm-mcp-core", "pm-mcp-clients", "cui-http"));
            var putRes = client.send(HttpRequest.json("PUT", "/api/v1/explorer/favourites", favReq.toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, putRes.status());

            // 2. Verify explorer reflects new order
            var expRes = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            var explorer = ProjectExplorer.fromJson(expRes.bodyText());
            assertEquals(3, explorer.favourites().size());
            assertEquals("pm-mcp-core", explorer.favourites().get(0).projectId());
            assertEquals("pm-mcp-clients", explorer.favourites().get(1).projectId());
            assertEquals("cui-http", explorer.favourites().get(2).projectId());

            // 3. Toggle favourite off for cui-http
            var toggleRes = client.send(HttpRequest.json("PUT", "/api/v1/projects/cui-http/favourite", "{\"favourite\":false}")
                    .withHeader("Authorization", auth));
            assertEquals(200, toggleRes.status());

            // 4. Verify cui-http removed from favourites
            var expRes2 = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            var explorer2 = ProjectExplorer.fromJson(expRes2.bodyText());
            assertEquals(2, explorer2.favourites().size());
            assertFalse(explorer2.favourites().stream().anyMatch(p -> "cui-http".equals(p.projectId())));
        }
    }

    @Test
    @DisplayName("handles project onboarding and abandon flow")
    void shouldHandleOnboardingAndAbandon() throws Exception {
        try (var runtime = MockRuntime.start(PROJECTS_STATE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // 1. Start onboarding
            var startRes = client.send(HttpRequest.json("POST", "/api/v1/project-onboardings", "{\"path\":\"~/git/new-service\"}")
                    .withHeader("Authorization", auth));
            assertEquals(201, startRes.status());

            // 2. Verify being added in explorer
            var expRes = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            var explorer = ProjectExplorer.fromJson(expRes.bodyText());
            assertEquals(2, explorer.beingAdded().size());
            assertTrue(explorer.beingAdded().stream().anyMatch(b -> "~/git/new-service".equals(b.checkoutPath())));

            // 3. Abandon onboarding
            var abandonRes = client.send(HttpRequest.delete("/api/v1/entries/ent-cui/abandon").withHeader("Authorization", auth));
            assertEquals(200, abandonRes.status());

            // 4. Verify only new-service remains
            var expRes2 = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            var explorer2 = ProjectExplorer.fromJson(expRes2.bodyText());
            assertEquals(1, explorer2.beingAdded().size());
            assertEquals("~/git/new-service", explorer2.beingAdded().get(0).checkoutPath());
        }
    }

    @Test
    @DisplayName("releases project ending active plans and epics")
    void shouldReleaseProject() throws Exception {
        try (var runtime = MockRuntime.start(PROJECTS_STATE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var relReq = new ProjectReleaseRequest("pm-mcp-clients", true, false, "Lifecycle completed");
            var relRes = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/release", relReq.toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, relRes.status());

            var result = ProjectReleaseResult.fromJson(relRes.bodyText());
            assertEquals("pm-mcp-clients", result.projectId());
            assertEquals(3, result.plansEndedCount());
            assertEquals(1, result.epicsEndedCount());

            // Check single project endpoint
            var prjRes = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients").withHeader("Authorization", auth));
            assertEquals(200, prjRes.status());
            var project = EnrolledProject.fromJson(prjRes.bodyText());
            assertEquals(ProjectReadiness.RELEASED, project.readiness());
            assertTrue(project.isReleased());
            assertEquals(0, project.activePlansCount());
            assertEquals(0, project.activeEpicsCount());
        }
    }

    @Test
    @DisplayName("serves the empty project explorer when no project is enrolled")
    void shouldServeEmptyExplorer() throws Exception {
        try (var runtime = MockRuntime.start(SCENARIO + "#projects.empty")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var explorer = ProjectExplorer.fromJson(res.bodyText());
            assertAll(
                    () -> assertTrue(explorer.favourites().isEmpty()),
                    () -> assertTrue(explorer.recent().isEmpty()),
                    () -> assertTrue(explorer.allProjects().isEmpty()),
                    () -> assertTrue(explorer.beingAdded().isEmpty())
            );
        }
    }

    @Test
    @DisplayName("serves the project that needs the operator's attention")
    void shouldServeAttentionNeededProject() throws Exception {
        try (var runtime = MockRuntime.start(SCENARIO + "#projects.attention")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var explorer = ProjectExplorer.fromJson(res.bodyText());
            var attention = explorer.allProjects().stream()
                    .filter(project -> "pm-mcp-clients".equals(project.projectId()))
                    .findFirst().orElseThrow();
            assertAll(
                    () -> assertEquals(ProjectReadiness.SETUP_NEEDS, attention.readiness()),
                    () -> assertEquals("decision", attention.setupNeedsSummary()),
                    () -> assertEquals(2, attention.needsOperatorCount())
            );
        }
    }

    @Test
    @DisplayName("serves a second project being added beside the one in progress")
    void shouldServeProjectBeingAdded() throws Exception {
        try (var runtime = MockRuntime.start(SCENARIO + "#projects.being-added")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var explorer = ProjectExplorer.fromJson(res.bodyText());
            assertEquals(2, explorer.beingAdded().size());
            assertTrue(explorer.beingAdded().stream()
                    .anyMatch(being -> "~/git/new-service".equals(being.checkoutPath())));
        }
    }

    @Test
    @DisplayName("serves a released and an enrolled project in the unenrolled case")
    void shouldServeUnenrolledCase() throws Exception {
        try (var runtime = MockRuntime.start(SCENARIO + "#projects.unenrolled")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var explorer = ProjectExplorer.fromJson(res.bodyText());
            var released = explorer.allProjects().stream()
                    .filter(project -> "legacy-portal".equals(project.projectId()))
                    .findFirst().orElseThrow();
            assertTrue(released.isReleased());
            assertEquals(ProjectReadiness.RELEASED, released.readiness());
        }
    }

    @Test
    @DisplayName("serves the ready case at both its explorer and its ready state")
    void shouldServeReadyProjectState() throws Exception {
        try (var runtime = MockRuntime.start(SCENARIO + "#projects.ready")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals("projects.ready", runtime.currentStateName());
            var res = client.send(HttpRequest.get("/api/v1/projects").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            assertEquals(6, ProjectExplorer.fromJson(res.bodyText()).allProjects().size());
        }
    }

    @Test
    @DisplayName("unenrols project only when no active work runs")
    void shouldUnenrolProjectWithSafetyCheck() throws Exception {
        try (var runtime = MockRuntime.start(PROJECTS_STATE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // 1. Attempt unenrol on pm-mcp-clients (has active plans/epics) -> refused 409
            var refusedRes = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/unenrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(409, refusedRes.status());
            var refusedJson = JsonTree.asObject(JsonTree.parse(refusedRes.bodyText()));
            assertNotNull(refusedJson);
            assertEquals("scope_active", refusedJson.get("error"));

            // 2. Unenrol cui-http (0 active plans, 0 active epics) -> success 200
            var unenrolRes = client.send(HttpRequest.json("POST", "/api/v1/projects/cui-http/unenrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, unenrolRes.status());

            // 3. Verify cui-http no longer exists
            var getRes = client.send(HttpRequest.get("/api/v1/projects/cui-http").withHeader("Authorization", auth));
            assertEquals(404, getRes.status());
        }
    }
}
