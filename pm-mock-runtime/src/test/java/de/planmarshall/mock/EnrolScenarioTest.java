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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.AcceptEditRequest;
import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.EnrolRequest;
import de.planmarshall.api.resource.EnrolResult;
import de.planmarshall.api.resource.StoreDigestComparison;
import de.planmarshall.api.resource.UnenrolResult;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.ScenarioLoader;
import de.planmarshall.mock.scenario.ScenarioRegistry;

@DisplayName("EnrolScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class EnrolScenarioTest {

    private static final String SCENARIO = StandardScenarios.PROJECTS_AND_START;
    private static final String ENROLMENT = SCENARIO + "#enrol.digest-mismatch";
    private static final String ENROL_SCAN = SCENARIO + "#enrol.scan";
    private static final String TAMPERED = SCENARIO + "#enrol.enrolment-tampered";
    private static final String TAMPERED_PROJECT = "pm-mcp-clients";

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
            assertEquals("conflict", refusedJson.get("error"));

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

            var projectId = Generators.letterStrings(4, 10).next().toLowerCase();
            var checkout = "/home/" + Generators.letterStrings(3, 8).next().toLowerCase() + "/git/" + projectId;
            var request = new EnrolRequest(checkout, true);
            var response = client.send(HttpRequest.json("POST", "/api/v1/enrol", request.toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());

            var result = EnrolResult.fromJson(response.bodyText());
            assertAll(
                    () -> assertEquals(EnrolResult.STATUS_ENROLLED, result.status()),
                    () -> assertEquals(projectId, result.projectId()),
                    () -> assertTrue(result.pilot())
            );

            var comparison = digest(client, auth, projectId, ".plan%2Fmarshal.json");
            assertFalse(comparison.mismatch(), "a freshly enrolled project's store matches its sidecar");
        }
    }

    @Test
    @DisplayName("enrols a project on the project-scoped alias without a body path")
    void shouldEnrolOnProjectScopedAlias() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var aliasProject = Generators.letterStrings(4, 10).next().toLowerCase();
            var response = client.send(HttpRequest.json("POST", "/api/v1/projects/" + aliasProject + "/enrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());
            assertEquals(aliasProject, EnrolResult.fromJson(response.bodyText()).projectId());
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
    @DisplayName("reports project_not_enrolled for a project the machine does not know")
    void shouldReportStoreNotFound() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var unknownProject = Generators.letterStrings(4, 10).next().toLowerCase();
            var response = client.send(HttpRequest.get("/api/v1/projects/" + unknownProject + "/stores/digest")
                    .withHeader("Authorization", auth));
            assertEquals(404, response.status());
            var json = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            assertEquals("project_not_enrolled", json.get("error"));
        }
    }

    @Test
    @DisplayName("reports target_not_found for an enrolled project the scenario records no store for")
    void shouldReportMissingStoreOfAnEnrolledProject() throws Exception {
        try (var runtime = MockRuntime.start(ENROLMENT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // pm-mcp-core is enrolled in the explorer of the scenario but carries no store fixture.
            var response = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-core/stores/digest")
                    .withHeader("Authorization", auth));
            assertEquals(404, response.status());
            var json = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            assertEquals("target_not_found", json.get("error"));
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
            var scanProject = Generators.letterStrings(4, 10).next().toLowerCase();
            var response = client.send(HttpRequest.json("POST", "/api/v1/projects/" + scanProject + "/enrol", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, response.status());
            assertEquals(scanProject, EnrolResult.fromJson(response.bodyText()).projectId());
        }
    }

    @Test
    @DisplayName("refuses a project request with enrolment_tampered naming the accept-edit enrolment command")
    void shouldRefuseATamperedEnrolment() throws Exception {
        try (var runtime = MockRuntime.start(TAMPERED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var refused = client.send(HttpRequest.get("/api/v1/projects/" + TAMPERED_PROJECT)
                    .withHeader("Authorization", auth));
            assertEquals(409, refused.status(), refused.bodyText());
            var body = JsonTree.asObject(JsonTree.parse(refused.bodyText()));
            assertAll(
                    () -> assertEquals("enrolment_tampered", body.get("error")),
                    () -> assertEquals("pm-operator accept-edit enrolment " + TAMPERED_PROJECT, body.get("command")),
                    () -> assertTrue(String.valueOf(body.get("message"))
                            .contains("pm-operator accept-edit enrolment " + TAMPERED_PROJECT), refused.bodyText()));

            var untouched = client.send(HttpRequest.get("/api/v1/projects/plan-marshall")
                    .withHeader("Authorization", auth));
            assertEquals(200, untouched.status(), "another project's enrolment is intact: " + untouched.bodyText());
        }
    }

    @Test
    @DisplayName("serves the enrolment digest of a tampered enrolment file")
    void shouldServeTheEnrolmentDigest() throws Exception {
        try (var runtime = MockRuntime.start(TAMPERED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var comparison = enrolmentDigest(client, auth, TAMPERED_PROJECT);
            assertAll(
                    () -> assertEquals("enrolments/" + TAMPERED_PROJECT + ".json", comparison.storePath()),
                    () -> assertTrue(comparison.mismatch()),
                    () -> assertNotNull(comparison.mismatchDetails()));
            assertFalse(enrolmentDigest(client, auth, "plan-marshall").mismatch(),
                    "an intact enrolment file matches its sidecar");
        }
    }

    @Test
    @DisplayName("refuses a stale confirmation of the enrolment file and keeps the refusal")
    void shouldRefuseAStaleEnrolmentConfirmation() throws Exception {
        try (var runtime = MockRuntime.start(TAMPERED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var comparison = enrolmentDigest(client, auth, TAMPERED_PROJECT);
            var stale = Generators.fixedValues(comparison.recordedSha256(), "0".repeat(64)).next();

            var refused = client.send(HttpRequest.json("POST", enrolmentAccept(TAMPERED_PROJECT),
                    new AcceptEditRequest(comparison.storePath(), stale).toJson()).withHeader("Authorization", auth));
            assertEquals(409, refused.status(), refused.bodyText());
            assertEquals("conflict", JsonTree.asObject(JsonTree.parse(refused.bodyText())).get("error"));

            var still = client.send(HttpRequest.get("/api/v1/projects/" + TAMPERED_PROJECT)
                    .withHeader("Authorization", auth));
            assertEquals(409, still.status(), "a refused confirmation leaves the enrolment tampered");
        }
    }

    @Test
    @DisplayName("accepts the confirmed enrolment file, re-baselines it and serves the project again")
    void shouldAcceptTheEnrolmentAndServeTheProject() throws Exception {
        try (var runtime = MockRuntime.start(TAMPERED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var comparison = enrolmentDigest(client, auth, TAMPERED_PROJECT);

            var accepted = client.send(HttpRequest.json("POST", enrolmentAccept(TAMPERED_PROJECT),
                    new AcceptEditRequest(comparison.storePath(), comparison.currentSha256()).toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, accepted.status(), accepted.bodyText());
            assertEquals(comparison.currentSha256(), AcceptEditResult.fromJson(accepted.bodyText()).newSha256());

            assertFalse(enrolmentDigest(client, auth, TAMPERED_PROJECT).mismatch(), "the sidecar is re-baselined");
            var served = client.send(HttpRequest.get("/api/v1/projects/" + TAMPERED_PROJECT)
                    .withHeader("Authorization", auth));
            assertEquals(200, served.status(), served.bodyText());
        }
    }

    @Test
    @DisplayName("refuses the designer's project routes of a tampered enrolment")
    void shouldRefuseTheWorkflowsOfATamperedEnrolment() throws Exception {
        try (var runtime = MockRuntime.start(TAMPERED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var refused = client.send(HttpRequest.get("/api/v1/projects/" + TAMPERED_PROJECT + "/workflows")
                    .withHeader("Authorization", auth));

            assertEquals(409, refused.status(), refused.bodyText());
            assertEquals("enrolment_tampered", JsonTree.asObject(JsonTree.parse(refused.bodyText())).get("error"));
            assertEquals(200, enrolmentDigestResponse(client, auth, TAMPERED_PROJECT).status(),
                    "the enrolment routes stay reachable");
        }
    }

    @Test
    @DisplayName("reports the accepted enrolment file on the digest read after accept-edit")
    void shouldReportTheAcceptedEnrolmentDigest() throws Exception {
        try (var runtime = MockRuntime.start(TAMPERED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var tampered = enrolmentDigest(client, auth, TAMPERED_PROJECT);
            var accepted = client.send(HttpRequest.json("POST", enrolmentAccept(TAMPERED_PROJECT),
                    new AcceptEditRequest(tampered.storePath(), tampered.currentSha256()).toJson())
                    .withHeader("Authorization", auth));
            assertEquals(200, accepted.status(), accepted.bodyText());

            var after = enrolmentDigest(client, auth, TAMPERED_PROJECT);

            assertAll(
                    () -> assertEquals(tampered.currentSize(), after.recordedSize(), "the accepted size is recorded"),
                    () -> assertEquals(tampered.currentSha256(), after.recordedSha256(), "the accepted hash is recorded"),
                    () -> assertEquals(tampered.currentSize(), after.currentSize()),
                    () -> assertEquals(tampered.currentSha256(), after.currentSha256()),
                    () -> assertFalse(after.mismatch()));
            var served = client.send(HttpRequest.get("/api/v1/projects/" + TAMPERED_PROJECT)
                    .withHeader("Authorization", auth));
            assertEquals(200, served.status(), served.bodyText());
        }
    }

    @Test
    @DisplayName("rejects a malformed enrolment_tampered fact and leaves the previous scenario")
    void shouldRejectAMalformedTamperedFact() throws Exception {
        var registry = new ScenarioRegistry();
        var asString = brokenTamperedScenario(registry, "\"" + TAMPERED_PROJECT + "\"");
        var withNumber = brokenTamperedScenario(registry, "[ \"" + TAMPERED_PROJECT + "\", "
                + Generators.integers(1, 999).next() + " ]");
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder().registry(registry).scenario(TAMPERED)
                     .timing(MockTiming.ORDERED).build())) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var engine = runtime.engine();

            for (var broken : List.of(asString, withNumber)) {
                var failure = assertThrows(IllegalStateException.class, () -> engine.switchScenario(broken));
                assertTrue(failure.getMessage().contains("enrolment_tampered"), failure.getMessage());
                assertEquals(SCENARIO, runtime.currentScenarioName(), "the refused switch keeps the scenario");
                var still = client.send(HttpRequest.get("/api/v1/projects/" + TAMPERED_PROJECT)
                        .withHeader("Authorization", auth));
                assertEquals(409, still.status(), "the previous state's tampered enrolment stays: " + broken);
            }
        }
    }

    private static String brokenTamperedScenario(ScenarioRegistry registry, String fact) {
        var name = "tampered-" + Generators.letterStrings(4, 10).next().toLowerCase();
        var state = "s" + Generators.letterStrings(3, 6).next().toLowerCase();
        registry.register(ScenarioLoader.load("""
                {
                  "format_version": 1,
                  "name": "%1$s",
                  "start": "%2$s",
                  "states": {
                    "%2$s": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "%2$s" },
                        "facts": { "enrolment_tampered": %3$s },
                        "links": []
                      }
                    }
                  }
                }
                """.formatted(name, state, fact), name));
        return name;
    }

    private static HttpResponse enrolmentDigestResponse(UnixSocketHttpClient client, String auth, String projectId)
            throws IOException {
        return client.send(HttpRequest.get("/api/v1/projects/" + projectId + "/enrolment/digest")
                .withHeader("Authorization", auth));
    }

    private static StoreDigestComparison enrolmentDigest(UnixSocketHttpClient client, String auth, String projectId)
            throws IOException {
        var response = client.send(HttpRequest.get("/api/v1/projects/" + projectId + "/enrolment/digest")
                .withHeader("Authorization", auth));
        assertEquals(200, response.status(), response.bodyText());
        return StoreDigestComparison.fromJson(response.bodyText());
    }

    private static String enrolmentAccept(String projectId) {
        return "/api/v1/projects/" + projectId + "/enrolment/accept-edit";
    }
}
