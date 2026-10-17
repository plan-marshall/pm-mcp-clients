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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.planmarshall.api.designer.BuildingBlock;
import de.planmarshall.api.designer.UnitPlacingResult;
import de.planmarshall.api.designer.WorkflowCatalogue;
import de.planmarshall.api.designer.WorkflowFinding;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.designer.WorkflowValidationRequest;
import de.planmarshall.api.designer.WorkflowValidationResult;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.process.ProcessNode;
import de.planmarshall.api.process.ProcessProjectRef;
import de.planmarshall.api.process.ProcessTokens;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.mock.scenario.Scenario;

/**
 * The process-designer state of the mock runtime (UC-PLN-3): the installed workflow units in the
 * machine and project scopes, the catalogues of building blocks, and the validation, placing, and
 * removing the runtime serves. Seeded from the {@code designer} fixtures of the active scenario.
 */
final class DesignerRegistry {

    /** The machine target and catalogue scope. */
    static final String MACHINE = "machine";

    private static final Pattern HEADER =
            Pattern.compile("^workflow\\s+\\S+\\s+version\\s+\\S+\\s+format\\s+\\S+$");
    private static final List<String> STAGE_NAMES =
            List.of(WorkflowFinding.STAGE_PARSE, WorkflowFinding.STAGE_RULES,
                    WorkflowFinding.STAGE_INVARIANTS, WorkflowFinding.STAGE_COMPOSITION);
    private static final List<String> COMPOSED_PHASES = List.of(
            "phase-1-init", "phase-2-refine", "phase-3-outline", "phase-4-plan",
            "phase-5-execute", "phase-6-finalize");

    private final List<WorkflowUnit> machineUnits = new ArrayList<>();
    private final Map<String, List<WorkflowUnit>> projectUnits = new LinkedHashMap<>();
    private final List<BuildingBlock> machineBlocks = new ArrayList<>();
    private final Map<String, List<BuildingBlock>> projectBlocks = new LinkedHashMap<>();
    private final Map<String, Integer> revisions = new LinkedHashMap<>();
    private final Set<String> enrolledProjects = new LinkedHashSet<>();

    private String runtimeVersion = "0.0.0";

    /**
     * Resets and seeds this registry from the scenario's designer fixtures.
     *
     * @param scenario the active scenario
     */
    synchronized void init(Scenario scenario) {
        machineUnits.clear();
        projectUnits.clear();
        machineBlocks.clear();
        projectBlocks.clear();
        revisions.clear();
        enrolledProjects.clear();
        runtimeVersion = "0.0.0";

        var fixtures = scenario != null ? scenario.designer() : Map.<String, Object>of();
        if (fixtures.isEmpty()) {
            return;
        }
        runtimeVersion = string(fixtures, "runtime_version", "0.0.0");
        for (var unit : objectList(fixtures, "machine_units")) {
            machineUnits.add(toUnit(unit));
        }
        var projectUnitMap = JsonTree.asObject(fixtures.get("project_units"));
        if (projectUnitMap != null) {
            for (var entry : projectUnitMap.entrySet()) {
                var parsed = new ArrayList<WorkflowUnit>();
                for (var unit : objectList(entry.getValue())) {
                    parsed.add(toUnit(unit));
                }
                projectUnits.put(entry.getKey(), parsed);
            }
        }
        for (var block : objectList(fixtures, "machine_blocks")) {
            machineBlocks.add(toBlock(block));
        }
        var projectBlockMap = JsonTree.asObject(fixtures.get("project_blocks"));
        if (projectBlockMap != null) {
            for (var entry : projectBlockMap.entrySet()) {
                var parsed = new ArrayList<BuildingBlock>();
                for (var block : objectList(entry.getValue())) {
                    parsed.add(toBlock(block));
                }
                projectBlocks.put(entry.getKey(), parsed);
            }
        }
        if (fixtures.get("projects") instanceof List<?> projects) {
            for (var project : projects) {
                if (project != null) {
                    enrolledProjects.add(String.valueOf(project));
                }
            }
        }
        revisions.put(MACHINE, 1);
        for (var project : projectUnits.keySet()) {
            revisions.put(project, 1);
        }
    }

    synchronized List<WorkflowUnit> machineUnits() {
        return List.copyOf(machineUnits);
    }

    synchronized List<WorkflowUnit> projectUnits(String projectId) {
        return List.copyOf(projectUnits.getOrDefault(projectId, List.of()));
    }

    synchronized WorkflowCatalogue machineCatalogue() {
        return new WorkflowCatalogue(MACHINE, runtimeVersion, digest(MACHINE), machineBlocks);
    }

    synchronized WorkflowCatalogue projectCatalogue(String projectId) {
        return new WorkflowCatalogue(projectId, runtimeVersion, digest(projectId),
                projectBlocks.getOrDefault(projectId, List.of()));
    }

    synchronized String digest(String scope) {
        return "sha256:" + scope + "-r" + revisions.getOrDefault(scope, 1);
    }

    synchronized Set<String> enrolledProjects() {
        return Set.copyOf(enrolledProjects);
    }

    synchronized WorkflowUnit findUnit(String target, String name) {
        var list = MACHINE.equals(target) ? machineUnits : projectUnits.get(target);
        if (list == null) {
            return null;
        }
        for (var unit : list) {
            if (unit.name().equals(name)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * Validates a workflow unit exactly as the runtime would load it.
     *
     * @param request the validation request
     * @return the validation result
     */
    synchronized WorkflowValidationResult validate(WorkflowValidationRequest request) {
        var text = request.text();
        var lines = text.split("\n", -1);

        var header = "";
        var headerLine = 1;
        for (int i = 0; i < lines.length; i++) {
            var stripped = lines[i].strip();
            if (!stripped.isEmpty()) {
                header = stripped;
                headerLine = i + 1;
                break;
            }
        }
        if (!HEADER.matcher(header).matches()) {
            var finding = new WorkflowFinding(WorkflowFinding.STAGE_PARSE, "Rule 1", headerLine, 1,
                    WorkflowFinding.SEVERITY_ERROR,
                    "the first line must be: workflow <name> version <n> format <n>", request.project());
            return new WorkflowValidationResult(false, stages(0), List.of(finding), null, null);
        }

        var name = nameFrom(header, request.unitName());
        var structure = parseStructure(text);

        var ruleFindings = staticRules(text, request.project());
        if (!ruleFindings.isEmpty()) {
            return new WorkflowValidationResult(false, stages(1), ruleFindings, structure, null);
        }

        var invariantFindings = compiledInvariants(name, text, request.project());
        if (!invariantFindings.isEmpty()) {
            return new WorkflowValidationResult(false, stages(2), invariantFindings, structure, null);
        }

        if (request.project() == null && enrolledProjects.isEmpty()) {
            return new WorkflowValidationResult(true, stagesNoComposition(), List.of(), structure, null);
        }
        return new WorkflowValidationResult(true, stages(STAGE_NAMES.size()), List.of(), structure,
                compose(request.project()));
    }

    /**
     * Places a unit at the machine level or in a project.
     *
     * @param name   the unit's id
     * @param target {@code machine} or the project's id
     * @param text   the unit's text
     * @param author the operator's name
     * @param release the runtime's release
     * @return the placing result
     */
    synchronized UnitPlacingResult place(String name, String target, String text, String author,
                                         String release) {
        var isMachine = MACHINE.equals(target);
        var existing = findUnit(target, name);
        var group = existing != null ? existing.group() : WorkflowUnit.GROUP_PROJECT_OWNED;
        var level = isMachine ? WorkflowUnit.LEVEL_MACHINE : WorkflowUnit.LEVEL_PROJECT;
        var unit = new WorkflowUnit(name, group, level, parseFrontmatter(text),
                List.of(new WorkflowUnit.UnitLevel(level, text, 1, "sha256:placed-" + name)));
        var list = isMachine ? machineUnits : projectUnits.computeIfAbsent(target, _ -> new ArrayList<>());
        replaceOrAdd(list, unit);
        var steps = isMachine
                ? List.of(UnitPlacingResult.STEP_WRITTEN)
                : List.of(UnitPlacingResult.STEP_WRITTEN, UnitPlacingResult.STEP_COMMITTED,
                UnitPlacingResult.STEP_LANDED);
        bump(target);
        return new UnitPlacingResult(name, target, UnitPlacingResult.STATUS_LANDED, steps, digest(target),
                author, release, "sha256:placed-" + name, null);
    }

    /**
     * Removes a unit from the machine level or from a project.
     *
     * @param name   the unit's id
     * @param target {@code machine} or the project's id
     * @return the removal result
     */
    synchronized UnitPlacingResult remove(String name, String target) {
        var isMachine = MACHINE.equals(target);
        var list = isMachine ? machineUnits : projectUnits.get(target);
        if (list != null) {
            list.removeIf(unit -> unit.name().equals(name));
        }
        var steps = isMachine
                ? List.of(UnitPlacingResult.STEP_WRITTEN)
                : List.of(UnitPlacingResult.STEP_WRITTEN, UnitPlacingResult.STEP_COMMITTED,
                UnitPlacingResult.STEP_LANDED);
        bump(target);
        return new UnitPlacingResult(name, target, UnitPlacingResult.STATUS_LANDED, steps, digest(target),
                null, null, null, null);
    }

    private void bump(String scope) {
        revisions.merge(scope, 1, Integer::sum);
    }

    private static void replaceOrAdd(List<WorkflowUnit> list, WorkflowUnit unit) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name().equals(unit.name())) {
                list.set(i, unit);
                return;
            }
        }
        list.add(unit);
    }

    private static List<WorkflowValidationResult.Stage> stages(int failedIndex) {
        var stages = new ArrayList<WorkflowValidationResult.Stage>();
        for (int i = 0; i < STAGE_NAMES.size(); i++) {
            String status;
            if (i < failedIndex) {
                status = WorkflowValidationResult.Stage.STATUS_PASSED;
            } else if (i == failedIndex) {
                status = WorkflowValidationResult.Stage.STATUS_FAILED;
            } else {
                status = WorkflowValidationResult.Stage.STATUS_NOT_RUN;
            }
            stages.add(new WorkflowValidationResult.Stage(STAGE_NAMES.get(i), status));
        }
        return stages;
    }

    private static List<WorkflowValidationResult.Stage> stagesNoComposition() {
        var stages = new ArrayList<WorkflowValidationResult.Stage>();
        for (int i = 0; i < STAGE_NAMES.size() - 1; i++) {
            stages.add(new WorkflowValidationResult.Stage(STAGE_NAMES.get(i),
                    WorkflowValidationResult.Stage.STATUS_PASSED));
        }
        stages.add(new WorkflowValidationResult.Stage(WorkflowFinding.STAGE_COMPOSITION,
                WorkflowValidationResult.Stage.STATUS_NOT_RUN));
        return stages;
    }

    private static String nameFrom(String header, String fallback) {
        var parts = header.split("\\s+");
        if (parts.length >= 2 && !parts[1].isBlank()) {
            return parts[1];
        }
        return fallback != null ? fallback : "";
    }

    private static List<WorkflowFinding> staticRules(String text, String project) {
        var findings = new ArrayList<WorkflowFinding>();
        var lines = text.split("\n", -1);

        var hasFrontmatter = false;
        for (var line : lines) {
            if ("---".equals(line.strip())) {
                hasFrontmatter = true;
                break;
            }
        }
        if (!hasFrontmatter) {
            findings.add(new WorkflowFinding(WorkflowFinding.STAGE_RULES, "frontmatter.missing", 1, 1,
                    WorkflowFinding.SEVERITY_ERROR, "this unit has no frontmatter", project));
        }
        for (int i = 0; i < lines.length; i++) {
            var stripped = lines[i].strip();
            if (stripped.startsWith("author:") && stripped.toLowerCase().contains("plan-marshall")) {
                findings.add(new WorkflowFinding(WorkflowFinding.STAGE_RULES, "frontmatter.author", i + 1,
                        1, WorkflowFinding.SEVERITY_ERROR,
                        "the author is still plan-marshall; name yourself", project));
            }
            if ("goto".equals(stripped) || stripped.startsWith("goto ")) {
                findings.add(new WorkflowFinding(WorkflowFinding.STAGE_RULES, "Rule 6", i + 1, 1,
                        WorkflowFinding.SEVERITY_ERROR, "'goto' is not a keyword of the language",
                        project));
            }
        }
        return findings;
    }

    private static List<WorkflowFinding> compiledInvariants(String name, String text, String project) {
        var findings = new ArrayList<WorkflowFinding>();
        if ("phase-6-finalize".equals(name) && !text.contains("finalize.archive-plan")) {
            findings.add(new WorkflowFinding(WorkflowFinding.STAGE_INVARIANTS, "INV-FIN-1",
                    lineOf(text, "state finalize.review"), 3, WorkflowFinding.SEVERITY_ERROR,
                    "a mandatory finalize step is missing", project));
        }
        return findings;
    }

    private static int lineOf(String text, String needle) {
        var lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(needle)) {
                return i + 1;
            }
        }
        return 1;
    }

    private static WorkflowValidationResult.Structure parseStructure(String text) {
        var lines = text.split("\n", -1);
        var entries = new ArrayList<String>();
        var exits = new ArrayList<String>();
        var states = new ArrayList<WorkflowValidationResult.StructureState>();
        var displayName = "";
        String currentState = null;
        var outcomes = new ArrayList<String>();
        var calls = new ArrayList<String>();

        var inFrontmatter = false;
        var frontmatterSeen = false;
        for (var raw : lines) {
            var line = raw.strip();
            if ("---".equals(line)) {
                if (!frontmatterSeen) {
                    inFrontmatter = true;
                    frontmatterSeen = true;
                } else {
                    inFrontmatter = false;
                }
                continue;
            }
            if (inFrontmatter) {
                if (line.startsWith("name:")) {
                    displayName = valueOf(line);
                } else if (line.startsWith("entry")) {
                    entries.add(entryExitValue(line));
                } else if (line.startsWith("exit")) {
                    exits.add(entryExitValue(line));
                }
                continue;
            }
            if (line.startsWith("state ")) {
                if (currentState != null) {
                    states.add(new WorkflowValidationResult.StructureState(currentState, "state",
                            outcomes, calls));
                }
                var rest = line.substring("state ".length()).strip();
                currentState = rest.split("\\s+")[0];
                outcomes = new ArrayList<>();
                calls = new ArrayList<>();
            } else if (line.startsWith("call ") && currentState != null) {
                calls.add(line.substring("call ".length()).strip());
            } else if (line.contains("->") && currentState != null) {
                outcomes.add(line);
            }
        }
        if (currentState != null) {
            states.add(new WorkflowValidationResult.StructureState(currentState, "state", outcomes, calls));
        }
        return new WorkflowValidationResult.Structure(displayName, entries, exits, states);
    }

    private static WorkflowUnit.Frontmatter parseFrontmatter(String text) {
        var lines = text.split("\n", -1);
        var inFrontmatter = false;
        var seen = false;
        var displayName = "";
        var description = "";
        var author = "";
        var type = "";
        var release = "";
        var entries = new ArrayList<String>();
        var exits = new ArrayList<String>();
        for (var raw : lines) {
            var line = raw.strip();
            if ("---".equals(line)) {
                if (!seen) {
                    inFrontmatter = true;
                    seen = true;
                } else {
                    break;
                }
                continue;
            }
            if (!inFrontmatter) {
                continue;
            }
            if (line.startsWith("name:")) {
                displayName = valueOf(line);
            } else if (line.startsWith("description:")) {
                description = valueOf(line);
            } else if (line.startsWith("author:")) {
                author = valueOf(line);
            } else if (line.startsWith("type:")) {
                type = valueOf(line);
            } else if (line.startsWith("release:")) {
                release = valueOf(line);
            } else if (line.startsWith("entry")) {
                entries.add(entryExitValue(line));
            } else if (line.startsWith("exit")) {
                exits.add(entryExitValue(line));
            }
        }
        if (!seen) {
            return null;
        }
        return new WorkflowUnit.Frontmatter(displayName, description, type, entries, exits, author,
                release, false);
    }

    private static String valueOf(String line) {
        var idx = line.indexOf(':');
        return idx >= 0 ? line.substring(idx + 1).strip() : line.strip();
    }

    private static String entryExitValue(String line) {
        var parts = line.split("\\s+", 2);
        return parts.length > 1 ? parts[1].strip() : "";
    }

    private static ProcessTree compose(String project) {
        var projectId = project != null ? project : "machine";
        var nodes = new ArrayList<ProcessNode>();
        for (var phase : COMPOSED_PHASES) {
            nodes.add(new ProcessNode(phase, phase, "phase", "planned", 1, 1, 0L, null,
                    List.of(), List.of(), null, null, Map.of()));
        }
        return new ProcessTree("plan", "If a plan started now",
                new ProcessProjectRef(projectId, projectId, null), null, "execute", "planned", null,
                false, 1, new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false), nodes, List.of(),
                null, null, 0L);
    }

    private static WorkflowUnit toUnit(Map<String, Object> map) {
        try {
            return WorkflowUnit.fromMap(map);
        } catch (IOException e) {
            throw new UncheckedIOException("Malformed designer unit fixture", e);
        }
    }

    private static BuildingBlock toBlock(Map<String, Object> map) {
        try {
            return BuildingBlock.fromMap(map);
        } catch (IOException e) {
            throw new UncheckedIOException("Malformed designer block fixture", e);
        }
    }

    private static String string(Map<String, Object> map, String key, String fallback) {
        var value = JsonTree.string(map, key);
        return value != null ? value : fallback;
    }

    private static List<Map<String, Object>> objectList(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof List<?> list)) {
            return List.of();
        }
        var result = new ArrayList<Map<String, Object>>();
        for (var item : list) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                result.add(obj);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        var result = new ArrayList<Map<String, Object>>();
        for (var item : list) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                result.add(obj);
            }
        }
        return result;
    }
}
