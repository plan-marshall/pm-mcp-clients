/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.process.ProcessNode;
import de.planmarshall.api.process.ProcessProjectRef;
import de.planmarshall.api.process.ProcessTokens;
import de.planmarshall.api.process.ProcessTree;

class DesignerRecordsTest {

    @Test
    void shouldExposeTheClosedBlockKindEnumeration() {
        assertEquals(
                List.of("keyword", "primitive", "guard", "link_relation", "condition", "sub_workflow",
                        "role", "parameter"),
                BlockKind.wireValues());

        assertEquals(BlockKind.SUB_WORKFLOW, BlockKind.fromWire("sub_workflow"));
        assertEquals(BlockKind.LINK_RELATION, BlockKind.fromWire("LINK_RELATION"));
        assertEquals(BlockKind.GUARD, BlockKind.requireWire("guard"));
        assertNull(BlockKind.fromWire("not_a_kind"));
        assertThrows(IllegalArgumentException.class, () -> BlockKind.requireWire("not_a_kind"));
        assertNull(BlockKind.fromWire(null));
    }

    @Test
    void shouldRoundTripBuildingBlock() throws Exception {
        var block = new BuildingBlock(
                BlockKind.SUB_WORKFLOW,
                "git.commit_and_push",
                "Commits the changes of the plan's worktree and pushes its branch.",
                "call git.commit_and_push entry commit",
                List.of("message_mode enum", "push_mode enum"),
                List.of("a push remote"),
                List.of("done (success)", "stepped_out"),
                List.of("the call clause of a state"),
                false);

        var parsed = BuildingBlock.fromJson(block.toJson());

        assertEquals(block, parsed);
        assertEquals(BlockKind.SUB_WORKFLOW, parsed.kind());
        assertEquals(List.of("done (success)", "stepped_out"), parsed.outcomes());
        assertFalse(parsed.compiledOnly());
    }

    @Test
    void shouldRejectBuildingBlockWithUnknownKind() {
        assertThrows(JsonFormatException.class,
                () -> BuildingBlock.fromJson("{\"kind\":\"wizardry\",\"name\":\"x\"}"));
        assertThrows(JsonFormatException.class,
                () -> BuildingBlock.fromJson("{\"kind\":\"primitive\"}"));
    }

    @Test
    void shouldRoundTripWorkflowUnitWithLevelsAndFrontmatter() throws Exception {
        var frontmatter = new WorkflowUnit.Frontmatter(
                "Finalize",
                "Settles and reviews the plan's change, lands it",
                "subworkflow",
                List.of("start", "adopt"),
                List.of("merged", "no_change"),
                "Oliver Wolff",
                "0.9.0",
                false);
        var unit = new WorkflowUnit(
                "phase-6-finalize",
                WorkflowUnit.GROUP_PROJECT_OWNED,
                WorkflowUnit.LEVEL_PROJECT,
                frontmatter,
                List.of(
                        new WorkflowUnit.UnitLevel(WorkflowUnit.LEVEL_PROJECT, "workflow phase-6-finalize", 4, "sha256:aa"),
                        new WorkflowUnit.UnitLevel(WorkflowUnit.LEVEL_MACHINE, "workflow phase-6-finalize", 2, "sha256:bb"),
                        new WorkflowUnit.UnitLevel(WorkflowUnit.LEVEL_DEFAULT, "workflow phase-6-finalize", 1, "sha256:cc")));

        var parsed = WorkflowUnit.fromJson(unit.toJson());

        assertEquals(unit, parsed);
        assertEquals("Finalize", parsed.frontmatter().displayName());
        assertEquals(3, parsed.levels().size());
        assertEquals(WorkflowUnit.LEVEL_PROJECT, parsed.levelInForce());

        // A unit without frontmatter parses with a null frontmatter
        var noFrontmatter = WorkflowUnit.fromJson("{\"name\":\"land\"}");
        assertNull(noFrontmatter.frontmatter());
        assertEquals(WorkflowUnit.LEVEL_DEFAULT, noFrontmatter.levelInForce());
    }

    @Test
    void shouldRoundTripCatalogueWithEveryKind() throws Exception {
        var blocks = new ArrayList<BuildingBlock>();
        for (var kind : BlockKind.values()) {
            blocks.add(new BuildingBlock(kind, kind.wire() + "-block", "s", "t",
                    List.of(), List.of(), List.of(), List.of(), kind == BlockKind.SUB_WORKFLOW));
        }
        var catalogue = new WorkflowCatalogue(WorkflowCatalogue.SCOPE_MACHINE, "0.9.0", "sha256:cat", blocks);

        var parsed = WorkflowCatalogue.fromJson(catalogue.toJson());

        assertEquals(catalogue, parsed);
        assertEquals(8, parsed.blocks().size());
        assertEquals(WorkflowCatalogue.SCOPE_MACHINE, parsed.scope());
    }

    @Test
    void shouldRoundTripValidationRequest() throws Exception {
        var request = new WorkflowValidationRequest("workflow x version 1 format 1", "subworkflow", "x",
                "pm-mcp-clients", "sha256:old");
        var parsed = WorkflowValidationRequest.fromJson(request.toJson());
        assertEquals(request, parsed);
        assertEquals("pm-mcp-clients", parsed.project());

        var machineTarget = new WorkflowValidationRequest("text", null, null, null, null);
        assertNull(WorkflowValidationRequest.fromJson(machineTarget.toJson()).project());
        assertThrows(JsonFormatException.class, () -> WorkflowValidationRequest.fromJson("{}"));
    }

    @Test
    void shouldRoundTripValidationResultWithStructureAndComposedProcess() throws Exception {
        var project = new ProcessProjectRef("pm-mcp-clients", "pm-mcp-clients", null);
        var composed = new ProcessTree(
                "plan", "If a plan started now", project, null, "execute", "planned", null, false, 1,
                new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false),
                List.of(new ProcessNode("init", "Init", "phase", "planned", 1, 1, 0L, null,
                        List.of(), List.of(), null, null, Map.of())),
                List.of("init"), null, null, 0L);
        var result = new WorkflowValidationResult(
                true,
                List.of(
                        new WorkflowValidationResult.Stage(WorkflowFinding.STAGE_PARSE,
                                WorkflowValidationResult.Stage.STATUS_PASSED),
                        new WorkflowValidationResult.Stage(WorkflowFinding.STAGE_COMPOSITION,
                                WorkflowValidationResult.Stage.STATUS_PASSED)),
                List.of(),
                new WorkflowValidationResult.Structure(
                        "Finalize",
                        List.of("start"),
                        List.of("merged"),
                        List.of(new WorkflowValidationResult.StructureState("finalize.review", "state",
                                List.of("done → finalize.archive"), List.of("git.commit_and_push")))),
                composed);

        var parsed = WorkflowValidationResult.fromJson(result.toJson());

        assertEquals(result, parsed);
        assertTrue(parsed.valid());
        assertNotNull(parsed.structure());
        assertEquals(1, parsed.structure().states().size());
        assertNotNull(parsed.composed());
        assertEquals("plan", parsed.composed().planId());

        // An invalid result carries findings and no composed process
        var invalid = new WorkflowValidationResult(
                false,
                List.of(new WorkflowValidationResult.Stage(WorkflowFinding.STAGE_PARSE,
                                WorkflowValidationResult.Stage.STATUS_FAILED),
                        new WorkflowValidationResult.Stage(WorkflowFinding.STAGE_RULES,
                                WorkflowValidationResult.Stage.STATUS_NOT_RUN)),
                List.of(new WorkflowFinding(WorkflowFinding.STAGE_PARSE, "Rule 6", 3, 1,
                        WorkflowFinding.SEVERITY_ERROR, "expected a state", null)),
                null,
                null);
        var parsedInvalid = WorkflowValidationResult.fromJson(invalid.toJson());
        assertFalse(parsedInvalid.valid());
        assertEquals(1, parsedInvalid.findings().size());
        assertNull(parsedInvalid.composed());
        assertEquals("Rule 6", parsedInvalid.findings().getFirst().rule());
    }

    @Test
    void shouldRoundTripUnitAndBlockLists() throws Exception {
        var units = List.of(
                new WorkflowUnit("landing.x", WorkflowUnit.GROUP_AXIS_VARIANT, WorkflowUnit.LEVEL_MACHINE,
                        null, List.of()));
        assertEquals(units, WorkflowUnit.parseList(WorkflowUnit.writeList(units)));

        var blocks = List.of(new BuildingBlock(BlockKind.ROLE, "implementer", "s", "t",
                List.of(), List.of(), List.of(), List.of(), false));
        assertEquals(blocks, BuildingBlock.parseList(BuildingBlock.writeList(blocks)));
    }

    @Test
    void shouldRejectMalformedDocuments() {
        assertThrows(JsonFormatException.class, () -> WorkflowUnit.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> WorkflowUnit.fromJson("{}"));
        assertThrows(JsonFormatException.class, () -> WorkflowUnit.parseList("{}"));
        assertThrows(JsonFormatException.class, () -> BuildingBlock.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> BuildingBlock.parseList("{}"));
        assertThrows(JsonFormatException.class, () -> WorkflowCatalogue.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> WorkflowValidationResult.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> UnitPlacingResult.fromJson("{}"));
        assertThrows(JsonFormatException.class, () -> WorkflowFinding.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> WorkflowFinding.parseList("{}"));
    }

    @Test
    void shouldApplyCodecDefaultsAndAlternativeNames() throws Exception {
        var json = "{\"name\":\"u\",\"group\":null,\"level_in_force\":null,"
                + "\"frontmatter\":{\"displayName\":\"D\",\"entries\":[\"start\",null]},"
                + "\"levels\":[{\"level\":\"machine\"}]}";
        var unit = WorkflowUnit.fromJson(json);

        assertEquals("D", unit.frontmatter().displayName());
        assertEquals(List.of("start"), unit.frontmatter().entries());
        assertEquals(1, unit.levels().getFirst().revision());
        assertEquals(WorkflowUnit.LEVEL_DEFAULT, unit.levelInForce());

        assertThrows(JsonFormatException.class, () -> WorkflowUnit.fromJson("{\"name\":\"u\",\"levels\":[{}]}"));
    }

    @Test
    void shouldRejectStructureAndStageErrors() {
        assertThrows(JsonFormatException.class, () -> WorkflowValidationResult.fromJson("{\"stages\":[{}]}"));
        var map = new LinkedHashMap<String, Object>();
        map.put("structure", Map.of("states", List.of(Map.of())));
        assertThrows(JsonFormatException.class, () -> WorkflowValidationResult.fromMap(map));
    }

    @Test
    void shouldRoundTripFindingListAndPlacingResult() throws Exception {
        var findings = List.of(
                new WorkflowFinding(WorkflowFinding.STAGE_COMPOSITION, "INV-FIN-1", 42, 3,
                        WorkflowFinding.SEVERITY_ERROR, "a mandatory finalize step is missing",
                        "pm-mcp-clients"));
        var parsedFindings = WorkflowFinding.parseList(WorkflowFinding.writeList(findings));
        assertEquals(findings, parsedFindings);
        assertEquals("pm-mcp-clients", parsedFindings.getFirst().project());

        var placed = new UnitPlacingResult(
                "phase-6-finalize",
                "pm-mcp-clients",
                UnitPlacingResult.STATUS_LANDED,
                List.of(UnitPlacingResult.STEP_WRITTEN, UnitPlacingResult.STEP_COMMITTED,
                        UnitPlacingResult.STEP_LANDED),
                "sha256:cat2",
                "Oliver Wolff",
                "0.9.0",
                "sha256:3f9a",
                null);
        var parsedPlaced = UnitPlacingResult.fromJson(placed.toJson());
        assertEquals(placed, parsedPlaced);
        assertEquals(3, parsedPlaced.steps().size());
        assertNull(parsedPlaced.reason());

        var failed = UnitPlacingResult.fromJson(
                "{\"unit\":\"x\",\"target\":\"machine\",\"status\":\"failed\",\"reason\":\"no project\"}");
        assertEquals(UnitPlacingResult.STATUS_FAILED, failed.status());
        assertEquals("no project", failed.reason());
    }
}
