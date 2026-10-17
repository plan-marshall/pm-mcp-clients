/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Top-level response model for the Project Explorer (UC-PRJ-2).
 *
 * @param favourites  favourite projects in operator-defined order
 * @param recent      recently active projects (up to 5, excluding favourites)
 * @param allProjects complete list of enrolled projects
 * @param beingAdded  in-progress project onboardings awaiting enrolment
 */
public record ProjectExplorer(
        List<EnrolledProject> favourites,
        List<EnrolledProject> recent,
        List<EnrolledProject> allProjects,
        List<BeingAddedProject> beingAdded
) {
    public static final String FIELD_FAVOURITES = "favourites";
    public static final String FIELD_RECENT = "recent";
    public static final String FIELD_ALL_PROJECTS = "all_projects";
    public static final String FIELD_BEING_ADDED = "being_added";

    public ProjectExplorer {
        favourites = favourites != null ? List.copyOf(favourites) : List.of();
        recent = recent != null ? List.copyOf(recent) : List.of();
        allProjects = allProjects != null ? List.copyOf(allProjects) : List.of();
        beingAdded = beingAdded != null ? List.copyOf(beingAdded) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_FAVOURITES, favourites.stream().map(EnrolledProject::toMap).toList());
        map.put(FIELD_RECENT, recent.stream().map(EnrolledProject::toMap).toList());
        map.put(FIELD_ALL_PROJECTS, allProjects.stream().map(EnrolledProject::toMap).toList());
        map.put(FIELD_BEING_ADDED, beingAdded.stream().map(BeingAddedProject::toMap).toList());
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map input map
     * @return parsed ProjectExplorer
     * @throws IOException on format failure
     */
    public static ProjectExplorer fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");

        var favs = parseProjectList(map.get(FIELD_FAVOURITES));
        var recents = parseProjectList(map.get(FIELD_RECENT));

        var allArray = map.get(FIELD_ALL_PROJECTS);
        if (allArray == null) {
            allArray = map.get("projects");
        }
        var all = parseProjectList(allArray);

        var adding = parseBeingAddedList(map.get(FIELD_BEING_ADDED));

        return new ProjectExplorer(favs, recents, all, adding);
    }

    private static List<EnrolledProject> parseProjectList(Object raw) throws IOException {
        var list = new ArrayList<EnrolledProject>();
        var array = JsonTree.asArray(raw);
        if (array != null) {
            for (var item : array) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    var m = (Map<String, Object>) itemMap;
                    list.add(EnrolledProject.fromMap(m));
                }
            }
        }
        return list;
    }

    private static List<BeingAddedProject> parseBeingAddedList(Object raw) throws IOException {
        var list = new ArrayList<BeingAddedProject>();
        var array = JsonTree.asArray(raw);
        if (array != null) {
            for (var item : array) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    var m = (Map<String, Object>) itemMap;
                    list.add(BeingAddedProject.fromMap(m));
                }
            }
        }
        return list;
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed ProjectExplorer
     * @throws IOException on format failure
     */
    public static ProjectExplorer fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ProjectExplorer");
    }
}
