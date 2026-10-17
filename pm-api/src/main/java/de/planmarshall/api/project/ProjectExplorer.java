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

        var favList = new ArrayList<Map<String, Object>>();
        for (var p : favourites) {
            favList.add(p.toMap());
        }
        map.put(FIELD_FAVOURITES, favList);

        var recList = new ArrayList<Map<String, Object>>();
        for (var p : recent) {
            recList.add(p.toMap());
        }
        map.put(FIELD_RECENT, recList);

        var allList = new ArrayList<Map<String, Object>>();
        for (var p : allProjects) {
            allList.add(p.toMap());
        }
        map.put(FIELD_ALL_PROJECTS, allList);

        var addList = new ArrayList<Map<String, Object>>();
        for (var b : beingAdded) {
            addList.add(b.toMap());
        }
        map.put(FIELD_BEING_ADDED, addList);

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

        var favs = new ArrayList<EnrolledProject>();
        var favArray = JsonTree.asArray(map.get(FIELD_FAVOURITES));
        if (favArray != null) {
            for (var item : favArray) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    var m = (Map<String, Object>) itemMap;
                    favs.add(EnrolledProject.fromMap(m));
                }
            }
        }

        var recents = new ArrayList<EnrolledProject>();
        var recArray = JsonTree.asArray(map.get(FIELD_RECENT));
        if (recArray != null) {
            for (var item : recArray) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    var m = (Map<String, Object>) itemMap;
                    recents.add(EnrolledProject.fromMap(m));
                }
            }
        }

        var all = new ArrayList<EnrolledProject>();
        var allArray = JsonTree.asArray(map.get(FIELD_ALL_PROJECTS));
        if (allArray == null) {
            allArray = JsonTree.asArray(map.get("projects"));
        }
        if (allArray != null) {
            for (var item : allArray) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    var m = (Map<String, Object>) itemMap;
                    all.add(EnrolledProject.fromMap(m));
                }
            }
        }

        var adding = new ArrayList<BeingAddedProject>();
        var addArray = JsonTree.asArray(map.get(FIELD_BEING_ADDED));
        if (addArray != null) {
            for (var item : addArray) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    var m = (Map<String, Object>) itemMap;
                    adding.add(BeingAddedProject.fromMap(m));
                }
            }
        }

        return new ProjectExplorer(favs, recents, all, adding);
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
