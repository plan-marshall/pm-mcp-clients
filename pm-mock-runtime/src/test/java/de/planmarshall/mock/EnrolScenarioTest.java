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

import java.io.IOException;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.AcceptEditRequest;
import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.EnrolRequest;
import de.planmarshall.api.resource.EnrolResult;
import de.planmarshall.api.resource.StoreDigestComparison;
import de.planmarshall.api.resource.UnenrolResult;

@DisplayName("EnrolScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class EnrolScenarioTest {

    private static final String SCENARIO = StandardScenarios.PROJECTS_AND_START;
    private static final String ENROLMENT = SCENARIO + "#enrol.digest-mismatch";
    private static final String ENROL_SCAN = SCENARIO + "#enrol.scan";

    private static final String RECORDED_SHA =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String CURRENT_SHA =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    private static StoreDigestComparison digest(UnixSocketHttpClient client, String auth, String projectId,
            String storePath) throws IOException {
        var target = "/api/v1/projects/" + projectId + "/stores/digest?path=" + storePath;
        var response = client.send(HttpRequest.get(target).withHeader("Authorization", auth));
        assertEquals(200, response.status());
        return StoreDigestComparison.fromJson(response.bodyText());
    }

    @Test
    @DisplayName("loads the enrolment scenario and reports a mismatching store digest")
    void shouldLoadScenarioAndReportMismatch() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(SCENARIO, runtime.currentScenarioName());
            assertEquals("enrol.digest-mismatch", runtime.currentStateName());

            var comparison = digest(client, auth, "pm-mcp-clients", ".plan%2Fmarshal.json");
            assertAll(
                    () -> assertEquals(".plan/marshal.json", comparison.storePath()),
                    () -> assertEquals(1248L, comparison.recordedSize()),
                    () -> assertEquals(RECORDED_SHA, comparison.recordedSha256()),
                    () -> assertEquals(1512L, comparison.currentSize()),
                    () -> assertEquals(CURRENT_SHA, comparison.currentSha256()),
                    () -> assertTrue(comparison.mismatch()),
                    () -> assertNotNull(comparison.mismatchDetails())
            );

            var matched = digest(client, auth, "plan-marshall", ".plan%2Fmarshal.json");
            assertFalse(matched.mismatch(), "a store matching its sidecar reports no mismatch");
        }
    }

    @Test
    @DisplayName("accepts an edit only for the confirmed current digest and re-baselines the store")
    void shouldAcceptEditAndRebaseline() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var target = "/api/v1/projects/pm-mcp-clients/stores/accept-edit";

            // 1. A confirmation of a different digest is refused (PM-SEC-3)
            var refused = client.send(HttpRequest.json("POST", target,
                    new AcceptEditRequest(".plan/marshal.json", RECORDED_SHA).toJson())
                    .withHeader("Authorization", auth));
            assertEquals(409, refused.status());
            var refusedJson = JsonTree.asObject(JsonTree.parse(refused.bodyText()));
            assertEquals("confirmation_mismatch", refusedJson.get("error"));

            // 2. The current digest is accepted and re-baselines the sidecar
            var accepted = client.send(HttpRequest.json("POST", target,
                    new AcceptEditRequest(".plan/marshal.json", CURRENT_SHA).toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, accepted.status());
            var result = AcceptEditResult.fromJson(accepted.bodyText());
            assertAll(
                    () -> assertEquals(AcceptEditResult.STATUS_ACCEPTED, result.status()),
                    () -> assertEquals(CURRENT_SHA, result.newSha256())
            );

            // 3. The store now matches its recorded digest
            var comparison = digest(client, auth, "pm-mcp-clients", ".plan%2Fmarshal.json");
            assertFalse(comparison.mismatch());
            assertEquals(CURRENT_SHA, comparison.recordedSha256());
        }
    }

    @Test
    @DisplayName("enrols a project on the top-level verb and seeds its store")
    void shouldEnrolProject() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new EnrolRequest("/home/oliver/git/new-service", true);
            var response = client.send(HttpRequest.json("POST", "/api/v1/enrol", request.toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());

            var result = EnrolResult.fromJson(response.bodyText());
            assertAll(
                    () -> assertEquals(EnrolResult.STATUS_ENROLLED, result.status()),
                    () -> assertEquals("new-service", result.projectId()),
                    () -> assertTrue(result.pilot())
            );

            var comparison = digest(client, auth, "new-service", ".plan%2Fmarshal.json");
            assertFalse(comparison.mismatch(), "a freshly enrolled project's store matches its sidecar");
        }
    }

    @Test
    @DisplayName("enrols a project on the project-scoped alias without a body path")
    void shouldEnrolOnProjectScopedAlias() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var response = client.send(HttpRequest.json("POST", "/api/v1/projects/alias-app/enrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());
            assertEquals("alias-app", EnrolResult.fromJson(response.bodyText()).projectId());
        }
    }

    @Test
    @DisplayName("unenrols a project without active work and drops its store")
    void shouldUnenrolProjectAndDropStore() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var response = client.send(HttpRequest.json("POST", "/api/v1/projects/plan-marshall/unenrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());
            var result = UnenrolResult.fromJson(response.bodyText());
            assertEquals(UnenrolResult.STATUS_UNENROLLED, result.status());

            var missing = client.send(HttpRequest.get(
                    "/api/v1/projects/plan-marshall/stores/digest").withHeader("Authorization", auth));
            assertEquals(404, missing.status());
        }
    }

    @Test
    @DisplayName("reports store_not_found for a project without a recorded store")
    void shouldReportStoreNotFound() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var response = client.send(HttpRequest.get("/api/v1/projects/unknown/stores/digest")
                    .withHeader("Authorization", auth));
            assertEquals(404, response.status());
            var json = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            assertEquals("store_not_found", json.get("error"));
        }
    }

    @Test
    @DisplayName("refuses an accept-edit whose body names no store")
    void shouldRefuseMalformedAcceptEdit() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var response = client.send(HttpRequest.json("POST",
                    "/api/v1/projects/pm-mcp-clients/stores/accept-edit", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(409, response.status());
        }
    }

    @Test
    @DisplayName("serves the enrolment scan state with the same enrolment verbs")
    void shouldServeEnrolScanState() throws Exception {
        try (var runtime = MockRuntime.start(ENROL_SCAN)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals("enrol.scan", runtime.currentStateName());
            var response = client.send(HttpRequest.json("POST", "/api/v1/projects/scan-app/enrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());
            assertEquals("scan-app", EnrolResult.fromJson(response.bodyText()).projectId());
        }
    }
}
