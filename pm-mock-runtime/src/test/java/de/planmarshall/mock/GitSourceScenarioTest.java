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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.source.git.LocalChangesCaptureResult;
import de.planmarshall.api.source.git.LocalChangesPreview;
import de.planmarshall.api.source.git.PrAdoptResult;
import de.planmarshall.api.source.git.PullRequestObservation;
import de.planmarshall.api.source.git.ScreeningOutcome;
import de.planmarshall.api.source.git.SourceLane;

@DisplayName("GitSourceScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class GitSourceScenarioTest {

    private static final String AUTH = "Authorization";

    @Nested
    @DisplayName("Pull request adoption")
    class PullRequestAdoption {

        @Test
        @DisplayName("loads the git_sources scenario and observes a clean pull request")
        void shouldObservePullRequest() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(StandardScenarios.GIT_SOURCES, runtime.currentScenarioName());
                assertEquals("git_sources.intake", runtime.currentStateName());

                try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH, auth))) {
                    var sse = exchange.events();

                    var observation = observe(client, auth, "pm-mcp-clients", "4711");
                    assertEquals(PullRequestObservation.STATE_OPEN, observation.state());
                    assertTrue(observation.writable());
                    assertEquals(ScreeningOutcome.CLEAN, observation.screening());
                    assertEquals(1, observation.untrustedCount());

                    var event = sse.next();
                    assertNotNull(event);
                    assertEquals("source.pr_observed", event.event());
                }
            }
        }

        @Test
        @DisplayName("adopts a clean pull request in the adopt lane")
        void shouldAdoptCleanPullRequest() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/sources/pr/adopt",
                        "{\"project_id\":\"pm-mcp-clients\",\"pr_url\":\"4711\"}")
                        .withHeader(AUTH, auth));
                assertEquals(201, res.status());

                var result = PrAdoptResult.fromJson(res.bodyText());
                assertEquals(SourceLane.ADOPT, result.lane());
                assertEquals(PrAdoptResult.STATUS_ADOPTED, result.status());
                assertEquals("codeberg-provider", result.branch());
                assertEquals("adopt-pr-4711", result.planId());
            }
        }

        @Test
        @DisplayName("refuses a flagged pull request until it is accepted explicitly")
        void shouldRefuseFlaggedUntilAccepted() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var refused = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/sources/pr/adopt",
                        "{\"project_id\":\"pm-mcp-clients\",\"pr_url\":\"6677\"}")
                        .withHeader(AUTH, auth));
                assertEquals(409, refused.status());
                assertEquals("screening_flagged", reason(refused.bodyText()));

                var accepted = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/sources/pr/adopt",
                        "{\"project_id\":\"pm-mcp-clients\",\"pr_url\":\"6677\","
                                + "\"screening_bypass_reason\":\"I read the flagged text\"}")
                        .withHeader(AUTH, auth));
                assertEquals(201, accepted.status());
                assertEquals(SourceLane.ADOPT, PrAdoptResult.fromJson(accepted.bodyText()).lane());
            }
        }

        @Test
        @DisplayName("refuses a rate-limited, a closed, and a non-writable pull request")
        void shouldRefuseUnadoptablePullRequests() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals("rate_limited", refuseAdopt(client, auth, "7002"));
                assertEquals("not_open", refuseAdopt(client, auth, "7001"));
                assertEquals("head_not_writable", refuseAdopt(client, auth, "7003"));
            }
        }

        @Test
        @DisplayName("refuses an adoption in a project that is not ready")
        void shouldRefuseNotReadyProject() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/plan-marshall/sources/pr/adopt",
                        "{\"project_id\":\"plan-marshall\",\"pr_url\":\"4711\"}")
                        .withHeader(AUTH, auth));
                assertEquals(409, res.status());
                assertEquals("project_not_ready", reason(res.bodyText()));
            }
        }
    }

    @Nested
    @DisplayName("Local changes capture")
    class LocalChangesCapture {

        @Test
        @DisplayName("previews local changes and captures them into the land lane")
        void shouldCaptureLocalChanges() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var preview = preview(client, auth, "pm-mcp-clients");
                assertTrue(preview.eligible());
                assertTrue(preview.landingCommits());
                assertEquals("main", preview.branch());
                assertEquals(2, preview.changedFiles().size());
                assertEquals(93, preview.totalAdditions());

                try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH, auth))) {
                    var sse = exchange.events();

                    var res = client.send(HttpRequest.json("POST",
                            "/api/v1/projects/pm-mcp-clients/sources/local-changes/capture",
                            "{\"project_id\":\"pm-mcp-clients\",\"confirmed\":true}")
                            .withHeader(AUTH, auth));
                    assertEquals(201, res.status());

                    var result = LocalChangesCaptureResult.fromJson(res.bodyText());
                    assertEquals(SourceLane.LAND, result.lane());
                    assertEquals(LocalChangesCaptureResult.STATUS_CAPTURED, result.status());
                    assertEquals(40, result.snapshotCommit().length());

                    var event = sse.next();
                    assertNotNull(event);
                    assertEquals("source.local_changes_captured", event.event());
                }
            }
        }

        @Test
        @DisplayName("refuses to capture without the confirmation")
        void shouldRefuseWithoutConfirmation() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/sources/local-changes/capture",
                        "{\"project_id\":\"pm-mcp-clients\",\"confirmed\":false}")
                        .withHeader(AUTH, auth));
                assertEquals(409, res.status());
                assertEquals("confirmation_required", reason(res.bodyText()));
            }
        }

        @Test
        @DisplayName("refuses local changes where the landing is landing.files (PM-WF-25)")
        void shouldRefuseLandingFiles() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.GIT_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var preview = preview(client, auth, "pm-mcp-core");
                assertFalse(preview.eligible());
                assertFalse(preview.landingCommits());
                assertEquals("landing.files", preview.landing());
                assertEquals("landing.files", preview.reason());

                var res = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-core/sources/local-changes/capture",
                        "{\"project_id\":\"pm-mcp-core\",\"confirmed\":true}")
                        .withHeader(AUTH, auth));
                assertEquals(409, res.status());
                assertEquals("landing_files", reason(res.bodyText()));
            }
        }
    }

    private static PullRequestObservation observe(UnixSocketHttpClient client, String auth, String project,
                                                  String reference) throws Exception {
        var res = client.send(HttpRequest.json("POST",
                "/api/v1/projects/" + project + "/sources/pr/observe",
                JsonTree.write(Map.of("pr_url", reference))).withHeader(AUTH, auth));
        assertEquals(200, res.status());
        return PullRequestObservation.fromJson(res.bodyText());
    }

    private static String refuseAdopt(UnixSocketHttpClient client, String auth, String reference)
            throws Exception {
        var res = client.send(HttpRequest.json("POST",
                "/api/v1/projects/pm-mcp-clients/sources/pr/adopt",
                JsonTree.write(Map.of("project_id", "pm-mcp-clients", "pr_url", reference)))
                .withHeader(AUTH, auth));
        assertEquals(409, res.status());
        return reason(res.bodyText());
    }

    private static LocalChangesPreview preview(UnixSocketHttpClient client, String auth, String project)
            throws Exception {
        var res = client.send(HttpRequest.get(
                "/api/v1/projects/" + project + "/sources/local-changes/preview").withHeader(AUTH, auth));
        assertEquals(200, res.status());
        return LocalChangesPreview.fromJson(res.bodyText());
    }

    private static String reason(String body) throws Exception {
        var map = JsonTree.asObject(JsonTree.parse(body));
        return JsonTree.string(map, "reason");
    }
}
