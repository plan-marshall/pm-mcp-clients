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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.designer.BlockKind;
import de.planmarshall.api.designer.UnitPlacingResult;
import de.planmarshall.api.designer.WorkflowCatalogue;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.designer.WorkflowValidationResult;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

@DisplayName("DesignerScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class DesignerScenarioTest {

    private static final String VALID_UNIT = """
            workflow phase-6-finalize version 5 format 1
            ---
            name: Finalize
            description: Settles and reviews the plan's change, lands it.
            type: subworkflow
            author: Oliver Wolff
            release: 0.9.0
            entry start
            exit merged
            ---
            state finalize.review
              done -> finalize.archive-plan
            state finalize.archive-plan
              call git.commit_and_push
              done -> merged
            """;

    private static final String MACHINE_UNIT = """
            workflow landing.custom version 1 format 1
            ---
            name: Custom Landing
            description: Lands the plan a custom way.
            author: Oliver Wolff
            ---
            state landing.open
              done -> merged
            """;

    @Test
    @DisplayName("loads designer scenario and serves the installed units of both scopes")
    void shouldServeInstalledUnits() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.DESIGNER, runtime.currentScenarioName());
            assertEquals("designer.ready", runtime.currentStateName());

            var machineRes = client.send(HttpRequest.get("/api/v1/workflows").withHeader("Authorization", auth));
            assertEquals(200, machineRes.status());
            var machineUnits = WorkflowUnit.parseList(new String(machineRes.body()));
            assertEquals(3, machineUnits.size());
            assertTrue(machineUnits.stream().anyMatch(u -> "epic".equals(u.name())
                    && WorkflowUnit.GROUP_COMPILED_ONLY.equals(u.group())));

            var projectRes = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/workflows")
                    .withHeader("Authorization", auth));
            assertEquals(200, projectRes.status());
            var projectUnits = WorkflowUnit.parseList(new String(projectRes.body()));
            assertEquals(2, projectUnits.size());
            var finalize = projectUnits.stream().filter(u -> "phase-6-finalize".equals(u.name()))
                    .findFirst().orElseThrow();
            assertEquals(WorkflowUnit.LEVEL_PROJECT, finalize.levelInForce());
            assertEquals(3, finalize.levels().size());
            assertNotNull(finalize.frontmatter());
            assertEquals("Finalize", finalize.frontmatter().displayName());
        }
    }

    @Test
    @DisplayName("serves the catalogue of the machine scope with a block of every kind")
    void shouldServeMachineCatalogue() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/workflows/catalogue").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var catalogue = WorkflowCatalogue.fromJson(new String(res.body()));
            assertEquals(WorkflowCatalogue.SCOPE_MACHINE, catalogue.scope());
            assertEquals("0.9.0", catalogue.runtimeVersion());
            assertEquals(BlockKind.wireValues().size(), catalogue.blocks().size());

            var kinds = new TreeSet<String>();
            catalogue.blocks().forEach(block -> kinds.add(block.kind().wire()));
            assertEquals(new TreeSet<>(BlockKind.wireValues()), kinds);

            var projectRes = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/workflows/catalogue")
                    .withHeader("Authorization", auth));
            var projectCatalogue = WorkflowCatalogue.fromJson(new String(projectRes.body()));
            assertEquals("pm-mcp-clients", projectCatalogue.scope());
            assertFalse(projectCatalogue.blocks().isEmpty());
        }
    }

    @Test
    @DisplayName("validates a valid unit and returns its structure and composed process")
    void shouldValidateAValidUnit() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of(
                    "text", VALID_UNIT,
                    "unit_kind", "subworkflow",
                    "unit_name", "phase-6-finalize",
                    "project", "pm-mcp-clients"));
            var res = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", body)
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var result = WorkflowValidationResult.fromJson(new String(res.body()));
            assertTrue(result.valid());
            assertTrue(result.findings().isEmpty());
            assertNotNull(result.structure());
            assertEquals(2, result.structure().states().size());
            assertTrue(result.structure().entries().contains("start"));
            assertNotNull(result.composed());
            assertEquals(6, result.composed().nodes().size());
            assertEquals("planned", result.composed().nodes().getFirst().state());
        }
    }

    @Test
    @DisplayName("reports a parse error at its line and leaves the later stages not run")
    void shouldReportParseError() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of("text", "this is not a workflow"));
            var res = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", body)
                    .withHeader("Authorization", auth));
            var result = WorkflowValidationResult.fromJson(new String(res.body()));

            assertFalse(result.valid());
            assertEquals(WorkflowValidationResult.Stage.STATUS_FAILED, result.stages().getFirst().status());
            assertEquals(WorkflowValidationResult.Stage.STATUS_NOT_RUN, result.stages().get(1).status());
            assertEquals("Rule 1", result.findings().getFirst().rule());
            assertNull(result.structure());
        }
    }

    @Test
    @DisplayName("finds a missing frontmatter and a forbidden keyword in the static rules")
    void shouldFindStaticRuleViolations() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var noFrontmatter = JsonTree.write(Map.of(
                    "text", "workflow landing.custom version 1 format 1\nstate landing.open\n"));
            var res = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", noFrontmatter)
                    .withHeader("Authorization", auth));
            var result = WorkflowValidationResult.fromJson(new String(res.body()));
            assertFalse(result.valid());
            assertEquals(WorkflowValidationResult.Stage.STATUS_FAILED, result.stages().get(1).status());
            assertEquals("frontmatter.missing", result.findings().getFirst().rule());

            var forbidden = JsonTree.write(Map.of(
                    "text", "workflow landing.custom version 1 format 1\n---\nname: Custom\nauthor: Oliver Wolff\n---\ngoto merged\n"));
            var forbiddenRes = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", forbidden)
                    .withHeader("Authorization", auth));
            var forbiddenResult = WorkflowValidationResult.fromJson(new String(forbiddenRes.body()));
            assertFalse(forbiddenResult.valid());
            assertEquals("Rule 6", forbiddenResult.findings().getFirst().rule());
            assertEquals(6, forbiddenResult.findings().getFirst().line());
        }
    }

    @Test
    @DisplayName("places and removes a unit in a project and broadcasts the events")
    void shouldPlaceAndRemoveInProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var body = JsonTree.write(Map.of(
                        "text", MACHINE_UNIT,
                        "unit_name", "landing.custom",
                        "unit_kind", "subworkflow",
                        "author", "Oliver Wolff",
                        "release", "0.9.0"));
                var placed = client.send(HttpRequest.json("PUT",
                        "/api/v1/projects/pm-mcp-clients/workflows/landing.custom", body)
                        .withHeader("Authorization", auth));
                assertEquals(200, placed.status(), new String(placed.body()));
                var result = UnitPlacingResult.fromJson(new String(placed.body()));
                assertEquals(UnitPlacingResult.STATUS_LANDED, result.status());
                assertEquals(3, result.steps().size());
                assertEquals("sha256:pm-mcp-clients-r2", result.catalogueDigest());

                var placedEvent = sse.next();
                assertNotNull(placedEvent);
                assertEquals("unit.placed", placedEvent.event());
                assertEquals("unit.landed", sse.next().event());

                // The changed catalogue digest is announced; the list shows the new unit
                var listRes = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/workflows")
                        .withHeader("Authorization", auth));
                var units = WorkflowUnit.parseList(new String(listRes.body()));
                assertEquals(3, units.size());

                var removed = client.send(new HttpRequest("DELETE",
                        "/api/v1/projects/pm-mcp-clients/workflows/landing.custom",
                        Map.of("Authorization", auth), null));
                assertEquals(200, removed.status());
                assertEquals("unit.removed", sse.next().event());

                var afterRemoval = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/workflows")
                        .withHeader("Authorization", auth));
                assertEquals(2, WorkflowUnit.parseList(new String(afterRemoval.body())).size());
            }
        }
    }

    @Test
    @DisplayName("refuses a unit name outside the canonical grammar and a compiled-only unit")
    void shouldRefuseInvalidNamesAndCompiledOnlyUnits() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of("text", MACHINE_UNIT));
            var badName = client.send(HttpRequest.json("PUT", "/api/v1/workflows/Bad-Name", body)
                    .withHeader("Authorization", auth));
            assertEquals(400, badName.status());
            assertEquals("unit_name_invalid",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(badName.body())), "error"));

            var traversal = client.send(HttpRequest.json("PUT", "/api/v1/workflows/..evil", body)
                    .withHeader("Authorization", auth));
            assertEquals(400, traversal.status());

            var compiledOnly = client.send(new HttpRequest("DELETE", "/api/v1/workflows/epic",
                    Map.of("Authorization", auth), null));
            assertEquals(409, compiledOnly.status());
            assertEquals("unit_not_designable",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(compiledOnly.body())), "error"));
        }
    }

    @Test
    @DisplayName("refuses a placing whose unit does not validate again")
    void shouldRefuseAnInvalidPlacing() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of("text", "no header here"));
            var res = client.send(HttpRequest.json("PUT",
                    "/api/v1/projects/pm-mcp-clients/workflows/landing.custom", body)
                    .withHeader("Authorization", auth));
            assertEquals(409, res.status());
            var error = JsonTree.asObject(JsonTree.parse(res.body()));
            assertEquals("unit_invalid", JsonTree.string(error, "error"));
            assertNotNull(error.get("findings"));
        }
    }

    @Test
    @DisplayName("the block kinds match the closed enumeration")
    void shouldExposeTheClosedBlockKinds() {
        assertEquals(Set.of("keyword", "primitive", "guard", "link_relation", "condition",
                "sub_workflow", "role", "parameter"), new TreeSet<>(BlockKind.wireValues()));
    }

    @Test
    @DisplayName("places and removes a unit at the machine level, in force at once")
    void shouldPlaceAndRemoveOnTheMachine() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of("text", MACHINE_UNIT, "unit_name", "landing.custom"));
            var placed = client.send(HttpRequest.json("PUT", "/api/v1/workflows/landing.custom", body)
                    .withHeader("Authorization", auth));
            assertEquals(200, placed.status());
            var result = UnitPlacingResult.fromJson(new String(placed.body()));
            assertEquals("machine", result.target());
            assertEquals("sha256:machine-r2", result.catalogueDigest());

            var removed = client.send(new HttpRequest("DELETE", "/api/v1/workflows/landing.custom",
                    Map.of("Authorization", auth), null));
            assertEquals(200, removed.status());
            var machine = client.send(HttpRequest.get("/api/v1/workflows").withHeader("Authorization", auth));
            assertEquals(3, WorkflowUnit.parseList(new String(machine.body())).size());
        }
    }

    @Test
    @DisplayName("refuses placing a compiled-only unit and a unit that changed meanwhile")
    void shouldRefuseCompiledOnlyAndConflictingPlacings() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var compiledOnly = client.send(HttpRequest.json("PUT", "/api/v1/workflows/epic",
                    JsonTree.write(Map.of("text", MACHINE_UNIT))).withHeader("Authorization", auth));
            assertEquals(409, compiledOnly.status());
            assertEquals("unit_not_designable",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(compiledOnly.body())), "error"));

            var conflict = client.send(HttpRequest.json("PUT",
                    "/api/v1/projects/pm-mcp-clients/workflows/phase-6-finalize",
                    JsonTree.write(Map.of("text", VALID_UNIT, "digest", "sha256:stale")))
                    .withHeader("Authorization", auth));
            assertEquals(409, conflict.status());
            assertEquals("conflict",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(conflict.body())), "error"));
        }
    }

    @Test
    @DisplayName("reports an author and a compiled-invariant finding")
    void shouldReportAuthorAndInvariantFindings() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var author = JsonTree.write(Map.of("text",
                    "workflow landing.custom version 1 format 1\n---\nname: X\nauthor: plan-marshall\n---\nstate s\n"));
            var authorRes = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", author)
                    .withHeader("Authorization", auth));
            var authorResult = WorkflowValidationResult.fromJson(new String(authorRes.body()));
            assertFalse(authorResult.valid());
            assertEquals("frontmatter.author", authorResult.findings().getFirst().rule());
            assertEquals(4, authorResult.findings().getFirst().line());

            var invariant = JsonTree.write(Map.of("text",
                    "workflow phase-6-finalize version 5 format 1\n---\nname: F\nauthor: Oliver Wolff\n---\nstate finalize.review\n  done -> merged\n"));
            var invariantRes = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", invariant)
                    .withHeader("Authorization", auth));
            var invariantResult = WorkflowValidationResult.fromJson(new String(invariantRes.body()));
            assertFalse(invariantResult.valid());
            assertEquals("INV-FIN-1", invariantResult.findings().getFirst().rule());
            assertEquals(WorkflowValidationResult.Stage.STATUS_FAILED, invariantResult.stages().get(2).status());
        }
    }

    @Test
    @DisplayName("rejects malformed validation, placing, and removal requests")
    void shouldRejectMalformedRequests() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var empty = client.send(new HttpRequest("POST", "/api/v1/workflows/validate",
                    Map.of("Authorization", auth), new byte[0]));
            assertEquals(400, empty.status());

            var noText = client.send(HttpRequest.json("PUT", "/api/v1/workflows/landing.custom", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(400, noText.status());

            var emptyName = client.send(new HttpRequest("DELETE", "/api/v1/workflows/",
                    Map.of("Authorization", auth), null));
            assertEquals(400, emptyName.status());
        }
    }

    @Test
    @DisplayName("validates against the machine target and serves an unknown project's scope")
    void shouldValidateAgainstTheMachineAndServeUnknownProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of("text", MACHINE_UNIT, "unit_name", "landing.custom"));
            var res = client.send(HttpRequest.json("POST", "/api/v1/workflows/validate", body)
                    .withHeader("Authorization", auth));
            var result = WorkflowValidationResult.fromJson(new String(res.body()));
            assertTrue(result.valid());
            assertNotNull(result.composed());
            assertEquals("machine", result.composed().project().projectId());

            var unknown = client.send(HttpRequest.get("/api/v1/projects/unknown/workflows/catalogue")
                    .withHeader("Authorization", auth));
            assertEquals(200, unknown.status());
            var catalogue = WorkflowCatalogue.fromJson(new String(unknown.body()));
            assertEquals("unknown", catalogue.scope());
            assertTrue(catalogue.blocks().isEmpty());
        }
    }

    @Test
    @DisplayName("replaces an existing unit when the digest matches and parses its frontmatter")
    void shouldReplaceAnExistingUnit() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of(
                    "text", VALID_UNIT,
                    "unit_name", "phase-6-finalize",
                    "unit_kind", "subworkflow",
                    "author", "Oliver Wolff",
                    "release", "0.9.0",
                    "digest", "sha256:pf6-p4"));
            var res = client.send(HttpRequest.json("PUT",
                    "/api/v1/projects/pm-mcp-clients/workflows/phase-6-finalize", body)
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status(), new String(res.body()));

            var list = client.send(HttpRequest.get("/api/v1/projects/pm-mcp-clients/workflows")
                    .withHeader("Authorization", auth));
            var units = WorkflowUnit.parseList(new String(list.body()));
            assertEquals(2, units.size());
            var finalize = units.stream().filter(u -> "phase-6-finalize".equals(u.name()))
                    .findFirst().orElseThrow();
            assertEquals("sha256:placed-phase-6-finalize", finalize.levels().getFirst().digest());
            assertEquals("Finalize", finalize.frontmatter().displayName());
        }
    }

    @Test
    @DisplayName("refuses an invalid removal name and ignores non-designer methods")
    void shouldIgnoreNonDesignerMethods() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DESIGNER)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var badRemoval = client.send(new HttpRequest("DELETE", "/api/v1/workflows/Bad-Name",
                    Map.of("Authorization", auth), null));
            assertEquals(400, badRemoval.status());

            assertEquals(404, client.send(new HttpRequest("PUT", "/api/v1/workflows",
                    Map.of("Authorization", auth), "{}".getBytes())).status());
            assertEquals(404, client.send(new HttpRequest("DELETE", "/api/v1/workflows/catalogue",
                    Map.of("Authorization", auth), null)).status());
            assertEquals(404, client.send(new HttpRequest("GET", "/api/v1/workflows/validate",
                    Map.of("Authorization", auth), null)).status());
            assertEquals(404, client.send(new HttpRequest("PATCH", "/api/v1/workflows/landing.custom",
                    Map.of("Authorization", auth), "{}".getBytes())).status());
            assertEquals(404, client.send(new HttpRequest("GET", "/api/v1/projects/a/b/workflows",
                    Map.of("Authorization", auth), null)).status());
        }
    }
}
