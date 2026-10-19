/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.interaction.OperatorLink;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A start refused at its setup gate (State Catalogue, {@code start.refused} and
 * {@code start-epic.refused}; design <em>Starting Work</em>, the setup gate).
 * <p>
 * Nothing is created. The refusal names what is missing ({@code need}) and the one action that goes
 * on where it is fixed: the machine's onboarding for {@link StartRefusalReason#MACHINE_NOT_ONBOARDED},
 * the project's onboarding for {@link StartRefusalReason#PROJECT_NOT_READY}.
 *
 * @param reason     the closed reason
 * @param projectId  the project the start was refused in, or {@code null} for a machine refusal
 * @param need       what is missing (a provider, a credential, a step), or {@code null}
 * @param actionKey  the message key of the action that goes on where it is fixed
 * @param actionHref the local API path the action opens
 */
public record StartRefusal(
        StartRefusalReason reason,
        @Nullable String projectId,
        @Nullable String need,
        String actionKey,
        String actionHref
) {

    public static final String FIELD_REASON = "reason";
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_NEED = "need";
    public static final String FIELD_ACTION_KEY = "action_key";
    public static final String FIELD_ACTION_HREF = "action_href";

    /** The message key of the machine's onboarding action. */
    public static final String ACTION_CONTINUE_MACHINE = "start.gate.continue_onboarding";

    /** The message key of the project's onboarding action. */
    public static final String ACTION_CONTINUE_PROJECT = "start.gate.continue_project";

    private static final Set<String> KNOWN_FIELDS =
            Set.of(FIELD_REASON, FIELD_PROJECT_ID, FIELD_NEED, FIELD_ACTION_KEY, FIELD_ACTION_HREF);

    public StartRefusal {
        Objects.requireNonNull(reason, FIELD_REASON);
        Objects.requireNonNull(actionKey, FIELD_ACTION_KEY);
        Objects.requireNonNull(actionHref, FIELD_ACTION_HREF);
        if (!DottedIdentifier.messageKey(actionKey)) {
            throw new IllegalArgumentException("Invalid action message key: " + actionKey);
        }
        if (!actionHref.startsWith(OperatorLink.LOCAL_API_PREFIX)) {
            throw new IllegalArgumentException("The action opens a path of the local API: " + actionHref);
        }
        if (reason == StartRefusalReason.PROJECT_NOT_READY && projectId == null) {
            throw new IllegalArgumentException("A project refusal names its project");
        }
    }

    /**
     * @param projectId the project
     * @param need      what is missing
     * @return a refusal because the machine is not fully onboarded
     */
    public static StartRefusal machineNotOnboarded(@Nullable String projectId, String need) {
        return new StartRefusal(StartRefusalReason.MACHINE_NOT_ONBOARDED, projectId, need,
                ACTION_CONTINUE_MACHINE, "/api/v1/machine");
    }

    /**
     * @param projectId the project
     * @param need      what is missing
     * @return a refusal because the project is not ready
     */
    public static StartRefusal projectNotReady(String projectId, String need) {
        return new StartRefusal(StartRefusalReason.PROJECT_NOT_READY, projectId, need,
                ACTION_CONTINUE_PROJECT, "/api/v1/projects/" + projectId);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_REASON, reason.wireValue());
        if (projectId != null) {
            map.put(FIELD_PROJECT_ID, projectId);
        }
        if (need != null) {
            map.put(FIELD_NEED, need);
        }
        map.put(FIELD_ACTION_KEY, actionKey);
        map.put(FIELD_ACTION_HREF, actionHref);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this refusal to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed refusal
     * @throws IOException if a required field is missing or a field is malformed
     */
    public static StartRefusal fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "StartRefusal");
        var reason = StartRefusalReason.fromWireOrNull(JsonTree.string(map, FIELD_REASON));
        if (reason == null) {
            throw new JsonFormatException("Unknown " + FIELD_REASON + " in StartRefusal: "
                    + JsonTree.string(map, FIELD_REASON));
        }
        var actionKey = JsonTree.string(map, FIELD_ACTION_KEY);
        var actionHref = JsonTree.string(map, FIELD_ACTION_HREF);
        if (actionKey == null || actionHref == null) {
            throw new JsonFormatException("Missing required fields for StartRefusal: action_key, action_href");
        }
        try {
            return new StartRefusal(reason, JsonTree.string(map, FIELD_PROJECT_ID),
                    JsonTree.string(map, FIELD_NEED), actionKey, actionHref);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid StartRefusal: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed refusal
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static StartRefusal fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StartRefusal");
        }
        return fromMap(obj);
    }

    /**
     * Reads one refusal from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed refusal
     * @throws IOException on format failure
     */
    public static StartRefusal readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for StartRefusal");
        }
        return fromMap(map);
    }
}
