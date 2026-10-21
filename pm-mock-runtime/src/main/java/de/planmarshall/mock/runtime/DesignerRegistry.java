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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.designer.BuildingBlock;
import de.planmarshall.api.designer.UnitPlacingResult;
import de.planmarshall.api.designer.WorkflowCatalogue;
import de.planmarshall.api.designer.WorkflowFinding;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.designer.WorkflowValidationRequest;
import de.planmarshall.api.designer.WorkflowValidationResult;
import de.planmarshall.api.interaction.OperatorStateResource;
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

    private static final String DEFAULT_VERSION = "0.0.0";
    private static final String BASE_STATE = "designer.ready";
    private static final String FIXTURE_RUNTIME = "runtime";
    private static final String FIXTURE_DESIGNER = "designer";
    private static final String KEY_UNITS = "units";
    private static final String KEY_BLOCKS = "blocks";
    private static final String KEY_VERSION = "version";
    private static final String KEY_PROJECTS = "projects";
    private static final String KEY_RUNTIME_VERSION = "runtime_version";
    private static final String KEY_MACHINE_UNITS = "machine_units";
    private static final String KEY_MACHINE_BLOCKS = "machine_blocks";
    private static final String KEY_PROJECT_UNITS = "project_units";
    private static final String KEY_PROJECT_BLOCKS = "project_blocks";
    private static final String PATH_WORKFLOWS = "/api/v1/workflows";
    private static final String SEGMENT_CATALOGUE = "/catalogue";
    private static final String SEGMENT_WORKFLOWS = "/workflows";
    private static final String PREFIX_PROJECTS = "/api/v1/projects/";
    private static final String KEYWORD_ENTRY = "entry";
    private static final String KEYWORD_EXIT = "exit";
    private static final String PREFIX_STATE = "state ";
    private static final String PREFIX_CALL = "call ";
    private static final String FRONTMATTER_FENCE = "---";
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

    private String runtimeVersion = DEFAULT_VERSION;

    /**
     * Resets and seeds this registry from the scenario's standard routes and states.
     * <p>
     * The installed units and catalogues are the representations of the designer's {@code GET}
     * routes, exactly as a client reads them: {@code GET /api/v1/workflows} and
     * {@code GET /api/v1/workflows/catalogue} for the machine scope, and
     * {@code GET /api/v1/projects/<id>/workflows} with its catalogue for every enrolled project.
     * The states contribute only what a route cannot: the enrolled project list and the runtime
     * version of a case, read from {@code facts.runtime.designer} of the scenario's base state
     * ({@code designer.ready}) merged over the state the scenario starts in.
     *
     * @param scenario the active scenario
     */
    synchronized void init(Scenario scenario) {
        clear();
        if (scenario == null) {
            return;
        }
        var override = designerOverrideOf(scenario);
        var machineCatalogue = routeRepresentation(scenario, "GET", PATH_WORKFLOWS + SEGMENT_CATALOGUE);
        runtimeVersion = string(override, KEY_RUNTIME_VERSION, versionOf(machineCatalogue));
        machineUnits.addAll(parseUnits(unitsOf(routeRepresentation(scenario, "GET", PATH_WORKFLOWS))));
        machineBlocks.addAll(parseBlocks(blocksOf(machineCatalogue)));
        var projects = projectIdsOf(scenario);
        for (var project : projects) {
            projectUnits.put(project, parseUnits(unitsOf(routeRepresentation(scenario, "GET",
                    projectPath(project)))));
            projectBlocks.put(project, parseBlocks(blocksOf(routeRepresentation(scenario, "GET",
                    projectPath(project) + SEGMENT_CATALOGUE))));
        }
        applyUnitsOverride(override, KEY_MACHINE_UNITS, machineUnits);
        applyBlocksOverride(override, KEY_MACHINE_BLOCKS, machineBlocks);
        applyProjectUnitsOverride(override, KEY_PROJECT_UNITS, projectUnits);
        applyProjectBlocksOverride(override, KEY_PROJECT_BLOCKS, projectBlocks);
        addProjects(override.get(KEY_PROJECTS), projects);
        revisions.put(MACHINE, 1);
        projectUnits.keySet().forEach(project -> revisions.put(project, 1));
    }

    private static void applyUnitsOverride(Map<String, Object> override, String key, List<WorkflowUnit> target) {
        if (!override.containsKey(key)) {
            return;
        }
        target.clear();
        target.addAll(parseUnits(objectList(override.get(key))));
    }

    private static void applyBlocksOverride(Map<String, Object> override, String key, List<BuildingBlock> target) {
        if (!override.containsKey(key)) {
            return;
        }
        target.clear();
        target.addAll(parseBlocks(objectList(override.get(key))));
    }

    private static void applyProjectUnitsOverride(Map<String, Object> override, String key,
            Map<String, List<WorkflowUnit>> target) {
        if (!override.containsKey(key)) {
            return;
        }
        target.clear();
        var map = JsonTree.asObject(override.get(key));
        if (map != null) {
            map.forEach((project, value) -> target.put(project, parseUnits(objectList(value))));
        }
    }

    private static void applyProjectBlocksOverride(Map<String, Object> override, String key,
            Map<String, List<BuildingBlock>> target) {
        if (!override.containsKey(key)) {
            return;
        }
        target.clear();
        var map = JsonTree.asObject(override.get(key));
        if (map != null) {
            map.forEach((project, value) -> target.put(project, parseBlocks(objectList(value))));
        }
    }

    /**
     * @param scenario the active scenario
     * @return the state-level designer overrides: the base state's fixture merged under the start
     *         state's, or an empty map when neither carries one
     */
    private static Map<String, Object> designerOverrideOf(Scenario scenario) {
        var override = new LinkedHashMap<String, Object>();
        mergeDesignerOverride(scenario, BASE_STATE, override);
        mergeDesignerOverride(scenario, scenario.start(), override);
        return override;
    }

    private static void mergeDesignerOverride(Scenario scenario, String stateId, Map<String, Object> target) {
        var state = scenario.state(stateId);
        if (state == null) {
            return;
        }
        var representation = JsonTree.asObject(state.representation());
        if (representation == null) {
            return;
        }
        var facts = JsonTree.asObject(representation.get(OperatorStateResource.FIELD_FACTS));
        var runtime = facts != null ? JsonTree.asObject(facts.get(FIXTURE_RUNTIME)) : null;
        var designer = runtime != null ? JsonTree.asObject(runtime.get(FIXTURE_DESIGNER)) : null;
        if (designer != null) {
            target.putAll(designer);
        }
    }

    private static @Nullable Map<String, Object> routeRepresentation(Scenario scenario, String method, String path) {
        var route = scenario.route(method, path);
        return route != null ? JsonTree.asObject(route.representation()) : null;
    }

    private static String versionOf(@Nullable Map<String, Object> catalogue) {
        var version = catalogue != null ? JsonTree.string(catalogue, KEY_VERSION) : null;
        return version != null && !version.isBlank() ? version : DEFAULT_VERSION;
    }

    private static List<Map<String, Object>> unitsOf(@Nullable Map<String, Object> representation) {
        return representation != null ? objectList(representation.get(KEY_UNITS)) : List.of();
    }

    private static List<Map<String, Object>> blocksOf(@Nullable Map<String, Object> representation) {
        return representation != null ? objectList(representation.get(KEY_BLOCKS)) : List.of();
    }

    private static List<String> projectIdsOf(Scenario scenario) {
        var prefix = PREFIX_PROJECTS;
        var suffix = SEGMENT_WORKFLOWS;
        return scenario.routes().keySet().stream()
                .filter(key -> key.startsWith("GET " + prefix) && key.endsWith(suffix))
                .map(key -> key.substring(("GET " + prefix).length(), key.length() - suffix.length()))
                .filter(id -> !id.isEmpty() && !id.contains("/"))
                .distinct()
                .toList();
    }

    private static String projectPath(String projectId) {
        return PREFIX_PROJECTS + projectId + SEGMENT_WORKFLOWS;
    }

    private void addProjects(@Nullable Object declared, List<String> routeProjects) {
        if (declared instanceof List<?> projects) {
            projects.stream().filter(Objects::nonNull).map(String::valueOf).forEach(enrolledProjects::add);
            return;
        }
        enrolledProjects.addAll(routeProjects);
    }

    private void clear() {
        machineUnits.clear();
        projectUnits.clear();
        machineBlocks.clear();
        projectBlocks.clear();
        revisions.clear();
        enrolledProjects.clear();
        runtimeVersion = DEFAULT_VERSION;
    }

    private static List<WorkflowUnit> parseUnits(List<Map<String, Object>> maps) {
        return maps.stream().map(DesignerRegistry::toUnit)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<BuildingBlock> parseBlocks(List<Map<String, Object>> maps) {
        return maps.stream().map(DesignerRegistry::toBlock)
                .collect(Collectors.toCollection(ArrayList::new));
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
        return list.stream().filter(unit -> unit.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * Validates a workflow unit exactly as the runtime would load it.
     *
     * @param request the validation request
     * @return the validation result
     */
    synchronized WorkflowValidationResult validate(WorkflowValidationRequest request) {
        var lines = strippedLines(request.text());
        var headerIndex = firstNonBlank(lines);
        var header = lines.get(headerIndex);
        if (!HEADER.matcher(header).matches()) {
            return rejected(WorkflowFinding.STAGE_PARSE, List.of(new WorkflowFinding(
                    WorkflowFinding.STAGE_PARSE, "Rule 1", headerIndex + 1, 1,
                    WorkflowFinding.SEVERITY_ERROR,
                    "the first line must be: workflow <name> version <n> format <n>",
                    request.project())), null);
        }

        var name = nameFrom(header, request.unitName());
        var structure = parseStructure(request.text());

        var ruleFindings = staticRules(request.text(), request.project());
        if (!ruleFindings.isEmpty()) {
            return rejected(WorkflowFinding.STAGE_RULES, ruleFindings, structure);
        }
        var invariantFindings = compiledInvariants(name, request.text(), request.project());
        if (!invariantFindings.isEmpty()) {
            return rejected(WorkflowFinding.STAGE_INVARIANTS, invariantFindings, structure);
        }
        if (request.project() == null && enrolledProjects.isEmpty()) {
            return new WorkflowValidationResult(true, stagesNoComposition(), List.of(), structure, null);
        }
        return new WorkflowValidationResult(true, stages(STAGE_NAMES.size()), List.of(), structure,
                compose(request.project()));
    }

    private static WorkflowValidationResult rejected(String stage, List<WorkflowFinding> findings,
            WorkflowValidationResult.Structure structure) {
        return new WorkflowValidationResult(false, stages(STAGE_NAMES.indexOf(stage)), findings,
                structure, null);
    }

    private static int firstNonBlank(List<String> lines) {
        return IntStream.range(0, lines.size()).filter(index -> !lines.get(index).isEmpty())
                .findFirst().orElse(0);
    }

    /**
     * Places a unit at the machine level or in a project.
     *
     * @param name    the unit's id
     * @param target  {@code machine} or the project's id
     * @param text    the unit's text
     * @param author  the operator's name
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
        bump(target);
        var steps = isMachine
                ? List.of(UnitPlacingResult.STEP_WRITTEN)
                : List.of(UnitPlacingResult.STEP_WRITTEN, UnitPlacingResult.STEP_COMMITTED,
                UnitPlacingResult.STEP_LANDED);
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
        bump(target);
        var steps = isMachine
                ? List.of(UnitPlacingResult.STEP_WRITTEN)
                : List.of(UnitPlacingResult.STEP_WRITTEN, UnitPlacingResult.STEP_COMMITTED,
                UnitPlacingResult.STEP_LANDED);
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
        return IntStream.range(0, STAGE_NAMES.size())
                .mapToObj(index -> new WorkflowValidationResult.Stage(STAGE_NAMES.get(index),
                        stageStatus(index, failedIndex)))
                .toList();
    }

    private static String stageStatus(int index, int failedIndex) {
        if (index < failedIndex) {
            return WorkflowValidationResult.Stage.STATUS_PASSED;
        }
        return index == failedIndex
                ? WorkflowValidationResult.Stage.STATUS_FAILED
                : WorkflowValidationResult.Stage.STATUS_NOT_RUN;
    }

    private static List<WorkflowValidationResult.Stage> stagesNoComposition() {
        var stages = new ArrayList<>(stages(STAGE_NAMES.size()));
        stages.set(STAGE_NAMES.size() - 1, new WorkflowValidationResult.Stage(
                WorkflowFinding.STAGE_COMPOSITION, WorkflowValidationResult.Stage.STATUS_NOT_RUN));
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
        var lines = strippedLines(text);
        var findings = new ArrayList<WorkflowFinding>();
        if (lines.stream().noneMatch(FRONTMATTER_FENCE::equals)) {
            findings.add(new WorkflowFinding(WorkflowFinding.STAGE_RULES, "frontmatter.missing", 1, 1,
                    WorkflowFinding.SEVERITY_ERROR, "this unit has no frontmatter", project));
        }
        findings.addAll(ruleFindings(lines, project));
        return findings;
    }

    private static List<WorkflowFinding> ruleFindings(List<String> lines, String project) {
        return IntStream.range(0, lines.size())
                .mapToObj(index -> lineFindings(lines.get(index), index + 1, project))
                .flatMap(List::stream)
                .toList();
    }

    private static List<WorkflowFinding> lineFindings(String stripped, int line, String project) {
        var findings = new ArrayList<WorkflowFinding>();
        if (stripped.startsWith("author:") && stripped.toLowerCase().contains("plan-marshall")) {
            findings.add(new WorkflowFinding(WorkflowFinding.STAGE_RULES, "frontmatter.author", line, 1,
                    WorkflowFinding.SEVERITY_ERROR, "the author is still plan-marshall; name yourself",
                    project));
        }
        if ("goto".equals(stripped) || stripped.startsWith("goto ")) {
            findings.add(new WorkflowFinding(WorkflowFinding.STAGE_RULES, "Rule 6", line, 1,
                    WorkflowFinding.SEVERITY_ERROR, "'goto' is not a keyword of the language", project));
        }
        return findings;
    }

    private static List<WorkflowFinding> compiledInvariants(String name, String text, String project) {
        if ("phase-6-finalize".equals(name) && !text.contains("finalize.archive-plan")) {
            return List.of(new WorkflowFinding(WorkflowFinding.STAGE_INVARIANTS, "INV-FIN-1",
                    lineOf(text, "state finalize.review"), 3, WorkflowFinding.SEVERITY_ERROR,
                    "a mandatory finalize step is missing", project));
        }
        return List.of();
    }

    private static int lineOf(String text, String needle) {
        var lines = strippedLines(text);
        return IntStream.range(0, lines.size()).filter(index -> lines.get(index).contains(needle))
                .findFirst().orElse(0) + 1;
    }

    private static WorkflowValidationResult.Structure parseStructure(String text) {
        var frontmatter = parseFrontmatterBlock(text);
        var lines = strippedLines(text);
        var stateIndices = IntStream.range(0, lines.size())
                .filter(index -> lines.get(index).startsWith(PREFIX_STATE))
                .boxed()
                .toList();
        var states = IntStream.range(0, stateIndices.size())
                .mapToObj(ordinal -> structureState(lines, stateIndices, ordinal))
                .toList();
        return new WorkflowValidationResult.Structure(frontmatter.displayName(), frontmatter.entries(),
                frontmatter.exits(), states);
    }

    private static WorkflowValidationResult.StructureState structureState(List<String> lines,
            List<Integer> stateIndices, int ordinal) {
        var start = stateIndices.get(ordinal);
        var end = ordinal + 1 < stateIndices.size() ? stateIndices.get(ordinal + 1) : lines.size();
        var body = lines.subList(start + 1, end);
        var id = lines.get(start).substring(PREFIX_STATE.length()).strip().split("\\s+")[0];
        var outcomes = body.stream().filter(line -> line.contains("->")).toList();
        var calls = body.stream().filter(line -> line.startsWith(PREFIX_CALL))
                .map(line -> line.substring(PREFIX_CALL.length()).strip()).toList();
        return new WorkflowValidationResult.StructureState(id, "state", outcomes, calls);
    }

    private static WorkflowUnit.Frontmatter parseFrontmatter(String text) {
        var frontmatter = parseFrontmatterBlock(text);
        if (!frontmatter.present()) {
            return null;
        }
        return new WorkflowUnit.Frontmatter(frontmatter.displayName(), frontmatter.description(),
                frontmatter.type(), frontmatter.entries(), frontmatter.exits(), frontmatter.author(),
                frontmatter.release(), false);
    }

    private static FrontmatterBlock parseFrontmatterBlock(String text) {
        var lines = strippedLines(text);
        var open = lines.indexOf(FRONTMATTER_FENCE);
        if (open < 0) {
            return FrontmatterBlock.ABSENT;
        }
        var close = lines.subList(open + 1, lines.size()).indexOf(FRONTMATTER_FENCE);
        var block = lines.subList(open + 1, close < 0 ? lines.size() : open + 1 + close);
        return new FrontmatterBlock(true,
                firstValue(block, "name:"),
                firstValue(block, "description:"),
                firstValue(block, "type:"),
                firstValue(block, "author:"),
                firstValue(block, "release:"),
                keywordValues(block, KEYWORD_ENTRY),
                keywordValues(block, KEYWORD_EXIT));
    }

    private static String firstValue(List<String> block, String prefix) {
        return block.stream().filter(line -> line.startsWith(prefix)).map(DesignerRegistry::valueOf)
                .findFirst().orElse("");
    }

    private static List<String> keywordValues(List<String> block, String keyword) {
        return block.stream().filter(line -> line.startsWith(keyword))
                .map(DesignerRegistry::entryExitValue).toList();
    }

    private static String valueOf(String line) {
        var index = line.indexOf(':');
        return index >= 0 ? line.substring(index + 1).strip() : line.strip();
    }

    private static String entryExitValue(String line) {
        var parts = line.split("\\s+", 2);
        return parts.length > 1 ? parts[1].strip() : "";
    }

    private static ProcessTree compose(String project) {
        var projectId = project != null ? project : MACHINE;
        var nodes = COMPOSED_PHASES.stream()
                .map(phase -> new ProcessNode(phase, phase, "phase", "planned", 1, 1, 0L, null,
                        List.of(), List.of(), null, null, Map.of()))
                .toList();
        return new ProcessTree("plan", "If a plan started now",
                new ProcessProjectRef(projectId, projectId, null), null, "execute", "planned", null,
                false, 1, new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false), nodes, List.of(),
                null, null, 0L);
    }

    private static List<String> strippedLines(String text) {
        return Arrays.stream(text.split("\n", -1)).map(String::strip).toList();
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

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(JsonTree::asObject).filter(Objects::nonNull).toList();
    }

    private record FrontmatterBlock(
    boolean present,
    String displayName,
    String description,
    String type,
    String author,
    String release,
    List<String> entries,
    List<String> exits) {

        static final FrontmatterBlock ABSENT =
                new FrontmatterBlock(false, "", "", "", "", "", List.of(), List.of());
    }
}
