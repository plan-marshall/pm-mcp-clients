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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.process.ProcessTree;

/**
 * The result of validating a workflow unit (UC-PLN-3).
 * <p>
 * The validation runs the stages (parse, static rules, compiled invariants, composition), each
 * passed, failed, or not run; it reports the findings with their rule, line, column, and message;
 * it returns the parsed structure of the unit wherever the text parses; and, when valid, the
 * composed process in the representation of a plan's process, every node planned.
 *
 * @param valid      whether the runtime would load the unit
 * @param stages     the stages, in order, each passed, failed, or not run
 * @param findings   the findings of every stage that ran
 * @param structure  the parsed structure of the unit, or {@code null} when the text does not parse
 * @param composed   the composed process when valid, or {@code null}
 */
public record WorkflowValidationResult(
        boolean valid,
        List<Stage> stages,
        List<WorkflowFinding> findings,
        Structure structure,
        ProcessTree composed
) {
    private static final String FIELD_VALID = "valid";
    private static final String FIELD_STAGES = "stages";
    private static final String FIELD_FINDINGS = "findings";
    private static final String FIELD_STRUCTURE = "structure";
    private static final String FIELD_COMPOSED = "composed";

    /**
     * Compact constructor validating required properties.
     */
    public WorkflowValidationResult {
        stages = stages != null ? List.copyOf(stages) : List.of();
        findings = findings != null ? List.copyOf(findings) : List.of();
    }

    /**
     * One validation stage and its outcome.
     *
     * @param name   the stage ({@code parse}, {@code rules}, {@code invariants}, or {@code composition})
     * @param status {@link #STATUS_PASSED}, {@link #STATUS_FAILED}, or {@link #STATUS_NOT_RUN}
     */
    public record Stage(String name, String status) {
        /** The stage statuses. */
        public static final String STATUS_PASSED = "passed";
        /** The stage failed. */
        public static final String STATUS_FAILED = "failed";
        /** The stage was not run because an earlier one failed. */
        public static final String STATUS_NOT_RUN = "not_run";

        /**
         * Compact constructor validating required properties.
         */
        public Stage {
            Objects.requireNonNull(name, "name");
            status = status != null ? status : STATUS_NOT_RUN;
        }

        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("name", name);
            map.put("status", status);
            return map;
        }

        static Stage fromMap(Map<String, Object> map) throws IOException {
            if (map == null) {
                throw new JsonFormatException("Stage requires an object");
            }
            var name = JsonTree.string(map, "name");
            if (name == null) {
                throw new JsonFormatException("Stage requires a name");
            }
            return new Stage(name, JsonTree.string(map, "status"));
        }
    }

    /**
     * The parsed structure of a unit, as the structure pane shows it: no runtime state, each state
     * in the order of the unit with its outcomes and its calls.
     *
     * @param displayName the unit's display name
     * @param entries     the unit's entries
     * @param exits       the unit's exits
     * @param states      the states, in the order of the unit
     */
    public record Structure(
            String displayName,
            List<String> entries,
            List<String> exits,
            List<StructureState> states
    ) {
        /**
         * Compact constructor validating required properties.
         */
        public Structure {
            displayName = displayName != null ? displayName : "";
            entries = entries != null ? List.copyOf(entries) : List.of();
            exits = exits != null ? List.copyOf(exits) : List.of();
            states = states != null ? List.copyOf(states) : List.of();
        }

        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("display_name", displayName);
            map.put("entries", entries);
            map.put("exits", exits);
            var stateList = states.stream().map(StructureState::toMap).toList();
            map.put("states", stateList);
            return map;
        }

        static Structure fromMap(Map<String, Object> map) throws IOException {
            if (map == null) {
                throw new JsonFormatException("Structure requires an object");
            }
            var states = new ArrayList<StructureState>();
            for (var stateMap : DesignerJson.objectList(map, "states")) { // NOSONAR: the nested codec declares a checked IOException a stream cannot carry
                states.add(StructureState.fromMap(stateMap));
            }
            return new Structure(
                    JsonTree.string(map, "display_name"),
                    DesignerJson.stringList(map, "entries"),
                    DesignerJson.stringList(map, "exits"),
                    states
            );
        }
    }

    /**
     * One state of a unit's parsed structure.
     *
     * @param id       the state's name in the unit
     * @param kind     the state's kind (e.g. {@code state}, {@code job}, {@code sub_workflow})
     * @param outcomes the state's outcomes with their transitions (e.g. {@code done → finalize.review})
     * @param calls    the units the state calls
     */
    public record StructureState(String id, String kind, List<String> outcomes, List<String> calls) {
        /**
         * Compact constructor validating required properties.
         */
        public StructureState {
            Objects.requireNonNull(id, "id");
            kind = kind != null ? kind : "state";
            outcomes = outcomes != null ? List.copyOf(outcomes) : List.of();
            calls = calls != null ? List.copyOf(calls) : List.of();
        }

        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("id", id);
            map.put("kind", kind);
            map.put("outcomes", outcomes);
            map.put("calls", calls);
            return map;
        }

        static StructureState fromMap(Map<String, Object> map) throws IOException {
            if (map == null) {
                throw new JsonFormatException("StructureState requires an object");
            }
            var id = JsonTree.string(map, "id");
            if (id == null) {
                throw new JsonFormatException("StructureState requires an id");
            }
            return new StructureState(
                    id,
                    JsonTree.string(map, "kind"),
                    DesignerJson.stringList(map, "outcomes"),
                    DesignerJson.stringList(map, "calls")
            );
        }
    }

    /**
     * @return this result as a JSON tree
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_VALID, valid);
        var stageList = stages.stream().map(Stage::toMap).toList();
        map.put(FIELD_STAGES, stageList);
        map.put(FIELD_FINDINGS, findings.stream().map(WorkflowFinding::toMap).toList());
        if (structure != null) {
            map.put(FIELD_STRUCTURE, structure.toMap());
        }
        if (composed != null) {
            map.put(FIELD_COMPOSED, composed.toMap());
        }
        return map;
    }

    /**
     * @return this result as compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a result from a JSON tree.
     *
     * @param map the input tree
     * @return the result
     * @throws IOException on a malformed tree
     */
    public static WorkflowValidationResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var stages = new ArrayList<Stage>();
        for (var stageMap : DesignerJson.objectList(map, FIELD_STAGES)) { // NOSONAR: the nested codec declares a checked IOException a stream cannot carry
            stages.add(Stage.fromMap(stageMap));
        }
        var findings = new ArrayList<WorkflowFinding>();
        for (var findingMap : DesignerJson.objectList(map, FIELD_FINDINGS)) { // NOSONAR: the nested codec declares a checked IOException a stream cannot carry
            findings.add(WorkflowFinding.fromMap(findingMap));
        }
        var structureMap = JsonTree.asObject(map.get(FIELD_STRUCTURE));
        var structure = structureMap != null ? Structure.fromMap(structureMap) : null;
        var composedMap = JsonTree.asObject(map.get(FIELD_COMPOSED));
        var composed = composedMap != null ? ProcessTree.fromMap(composedMap) : null;
        return new WorkflowValidationResult(
                Boolean.TRUE.equals(map.get(FIELD_VALID)),
                stages,
                findings,
                structure,
                composed
        );
    }

    /**
     * Parses a result from JSON.
     *
     * @param json the JSON text
     * @return the result
     * @throws IOException on a malformed document
     */
    public static WorkflowValidationResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected a JSON object for WorkflowValidationResult");
        }
        return fromMap(obj);
    }
}
