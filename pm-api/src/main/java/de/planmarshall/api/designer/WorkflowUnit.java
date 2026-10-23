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

/**
 * A workflow unit the operator may own, as the designer shows it (UC-PLN-3).
 * <p>
 * The units are served in two scopes: a project's ({@code GET /api/v1/projects/{id}/workflows}) and
 * the machine's ({@code GET /api/v1/workflows}). Each unit carries its group, every level that has
 * it (project, machine, or the compiled default) with its text, revision, and digest, and the level
 * in force.
 *
 * @param name         the unit's id, from its header (e.g. {@code phase-6-finalize})
 * @param group        the group the designer sorts it into (see {@link #GROUP_*})
 * @param levelInForce the level that holds the unit now ({@link #LEVEL_PROJECT}, {@link #LEVEL_MACHINE}, or {@link #LEVEL_DEFAULT})
 * @param frontmatter  the unit's frontmatter as the panel shows it, or {@code null} when it has none
 * @param levels       every level that has the unit, the level in force first
 */
public record WorkflowUnit(
        String name,
        String group,
        String levelInForce,
        Frontmatter frontmatter,
        List<UnitLevel> levels
) {
    /** Group: a unit the operator owns for a project. */
    public static final String GROUP_PROJECT_OWNED = "project_owned";
    /** Group: a variant of a plan behaviour axis. */
    public static final String GROUP_AXIS_VARIANT = "axis_variant";
    /** Group: a named workflow of this machine or of a project. */
    public static final String GROUP_RECIPE = "recipe";
    /** Group: a compiled unit, read only and never replaced. */
    public static final String GROUP_COMPILED_ONLY = "compiled_only";

    /** Level: the project's copy, the team's file. */
    public static final String LEVEL_PROJECT = "project";
    /** Level: this machine's copy, the operator's. */
    public static final String LEVEL_MACHINE = "machine";
    /** Level: the compiled default. */
    public static final String LEVEL_DEFAULT = "default";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_GROUP = "group";
    private static final String FIELD_LEVEL_IN_FORCE = "level_in_force";
    private static final String FIELD_FRONTMATTER = "frontmatter";
    private static final String FIELD_LEVELS = "levels";

    /**
     * Compact constructor validating required properties.
     */
    public WorkflowUnit {
        Objects.requireNonNull(name, FIELD_NAME);
        group = group != null ? group : GROUP_PROJECT_OWNED;
        levelInForce = levelInForce != null ? levelInForce : LEVEL_DEFAULT;
        levels = levels != null ? List.copyOf(levels) : List.of();
    }

    /**
     * The frontmatter of a unit, as the {@code About this unit} panel shows it.
     *
     * @param displayName the display name from {@code name:}
     * @param description the description line
     * @param type        the type that a replacing unit keeps (e.g. {@code subworkflow})
     * @param icon        the stable icon id that the web app's visual language resolves
     * @param entries     the entries with their descriptions
     * @param exits       the exits
     * @param author      the author
     * @param release     the release the unit was modified in
     * @param signed      whether a signature is present
     */
    public record Frontmatter(
            String displayName,
            String description,
            String type,
            String icon,
            List<String> entries,
            List<String> exits,
            String author,
            String release,
            boolean signed
    ) {
        /**
         * Compact constructor.
         */
        public Frontmatter {
            displayName = displayName != null ? displayName : "";
            description = description != null ? description : "";
            type = type != null ? type : "";
            icon = icon != null ? icon : "";
            entries = entries != null ? List.copyOf(entries) : List.of();
            exits = exits != null ? List.copyOf(exits) : List.of();
            author = author != null ? author : "";
            release = release != null ? release : "";
        }

        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("display_name", displayName);
            map.put("description", description);
            map.put("type", type);
            map.put("icon", icon);
            map.put("entries", entries);
            map.put("exits", exits);
            map.put("author", author);
            map.put("release", release);
            map.put("signed", signed);
            return map;
        }

        static Frontmatter fromMap(Map<String, Object> map) throws IOException {
            if (map == null) {
                throw new JsonFormatException("Frontmatter requires an object");
            }
            return new Frontmatter(
                    DesignerJson.string(map, "display_name", "displayName"),
                    JsonTree.string(map, "description"),
                    JsonTree.string(map, "type"),
                    JsonTree.string(map, "icon"),
                    DesignerJson.stringList(map, "entries"),
                    DesignerJson.stringList(map, "exits"),
                    JsonTree.string(map, "author"),
                    JsonTree.string(map, "release"),
                    Boolean.TRUE.equals(map.get("signed"))
            );
        }
    }

    /**
     * One level that holds a unit, with the text as it is at that level.
     *
     * @param level    the level ({@code project}, {@code machine}, or {@code default})
     * @param text     the unit's DSL text at that level
     * @param revision the revision of the copy
     * @param digest   the digest of the copy
     */
    public record UnitLevel(String level, String text, int revision, String digest) {
        private static final String FIELD_LEVEL = "level";

        /**
         * Compact constructor validating required properties.
         */
        public UnitLevel {
            Objects.requireNonNull(level, FIELD_LEVEL);
            text = text != null ? text : "";
            digest = digest != null ? digest : "";
        }

        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_LEVEL, level);
            map.put("text", text);
            map.put("revision", revision);
            map.put("digest", digest);
            return map;
        }

        static UnitLevel fromMap(Map<String, Object> map) throws IOException {
            if (map == null) {
                throw new JsonFormatException("UnitLevel requires an object");
            }
            var level = JsonTree.string(map, FIELD_LEVEL);
            if (level == null) {
                throw new JsonFormatException("UnitLevel requires a level");
            }
            var revision = JsonTree.integer(map, "revision");
            return new UnitLevel(
                    level,
                    JsonTree.string(map, "text"),
                    revision != null ? revision : 1,
                    JsonTree.string(map, "digest")
            );
        }
    }

    /**
     * @return this unit as a JSON tree
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NAME, name);
        map.put(FIELD_GROUP, group);
        map.put(FIELD_LEVEL_IN_FORCE, levelInForce);
        if (frontmatter != null) {
            map.put(FIELD_FRONTMATTER, frontmatter.toMap());
        }
        var levelList = levels.stream().map(UnitLevel::toMap).toList();
        map.put(FIELD_LEVELS, levelList);
        return map;
    }

    /**
     * @return this unit as compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a unit from a JSON tree.
     *
     * @param map the input tree
     * @return the unit
     * @throws IOException on a malformed tree
     */
    public static WorkflowUnit fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var name = JsonTree.string(map, FIELD_NAME);
        if (name == null) {
            throw new JsonFormatException("WorkflowUnit requires a name");
        }
        var frontmatterMap = JsonTree.asObject(map.get(FIELD_FRONTMATTER));
        var frontmatter = frontmatterMap != null ? Frontmatter.fromMap(frontmatterMap) : null;
        var levels = new ArrayList<UnitLevel>();
        for (var levelMap : DesignerJson.objectList(map, FIELD_LEVELS)) { // NOSONAR: the nested codec declares a checked IOException a stream cannot carry
            levels.add(UnitLevel.fromMap(levelMap));
        }
        return new WorkflowUnit(
                name,
                JsonTree.string(map, FIELD_GROUP),
                JsonTree.string(map, FIELD_LEVEL_IN_FORCE),
                frontmatter,
                levels
        );
    }

    /**
     * Parses a unit from JSON.
     *
     * @param json the JSON text
     * @return the unit
     * @throws IOException on a malformed document
     */
    public static WorkflowUnit fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected a JSON object for WorkflowUnit");
        }
        return fromMap(obj);
    }

    /**
     * Parses a list of units from a JSON array string.
     *
     * @param json the JSON text
     * @return the units
     * @throws IOException on a malformed document
     */
    public static List<WorkflowUnit> parseList(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (!(parsed instanceof List<?> list)) {
            throw new JsonFormatException("Expected a JSON array for a WorkflowUnit list");
        }
        var result = new ArrayList<WorkflowUnit>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(fromMap(map));
            }
        }
        return result;
    }

    /**
     * Serializes a list of units to a JSON array string.
     *
     * @param units the units
     * @return the JSON text
     */
    public static String writeList(List<WorkflowUnit> units) {
        return JsonTree.write(units.stream().map(WorkflowUnit::toMap).toList());
    }
}
