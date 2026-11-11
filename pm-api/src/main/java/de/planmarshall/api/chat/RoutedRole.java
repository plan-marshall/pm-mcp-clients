/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.planmarshall.api.json.JsonTree;

/**
 * The planning role a chat is routed to, with the model it runs on
 * ({@code state-catalogue.adoc#chat}, Routing): {@code replan} for a plan, {@code decompose} for an
 * epic, its decomposition and its review, {@code refine} or {@code decompose} for a start, and for a
 * decision the role of the task that asked.
 *
 * @param name  the role's name
 * @param model the model the role runs on, as the clients show it
 */
public record RoutedRole(String name, String model) {

    /** The wire name of the role's name. */
    public static final String FIELD_NAME = "name";
    /** The wire name of the role's model. */
    public static final String FIELD_MODEL = "model";

    private static final String TYPE = "RoutedRole";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_NAME, FIELD_MODEL);

    /**
     * @param name  the role's name, not blank
     * @param model the model, not blank
     */
    public RoutedRole {
        Objects.requireNonNull(name, FIELD_NAME);
        Objects.requireNonNull(model, FIELD_MODEL);
        if (name.isBlank() || model.isBlank()) {
            throw new IllegalArgumentException("name and model must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NAME, name);
        map.put(FIELD_MODEL, model);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed role
     * @throws IOException if a member is unknown or missing
     */
    public static RoutedRole fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var name = ChatJson.requireText(map, FIELD_NAME, TYPE);
        var model = ChatJson.requireText(map, FIELD_MODEL, TYPE);
        return ChatJson.construct(TYPE, () -> new RoutedRole(name, model));
    }

    /**
     * @param json the JSON object string
     * @return the parsed role
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static RoutedRole fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
