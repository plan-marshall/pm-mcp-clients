/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import lombok.experimental.UtilityClass;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Loads and validates scenario files against the closed scenario format specification.
 */
@UtilityClass
public class ScenarioLoader {

    /** Default classpath directory for scenario files. */
    public static final String SCENARIOS_RESOURCE_DIR = "scenarios/";

    /**
     * Loads a scenario from a classpath resource or file system.
     *
     * @param nameOrPath scenario name (e.g. {@code onboarding-happy}) or file path
     * @return the parsed and validated scenario
     */
    public static Scenario load(String nameOrPath) {
        Objects.requireNonNull(nameOrPath, "nameOrPath");
        var path = Path.of(nameOrPath);
        if (Files.exists(path)) {
            return load(path);
        }
        var resourceName = nameOrPath.endsWith(".json") ? nameOrPath : nameOrPath + ".json";
        var fullResource = resourceName.startsWith(SCENARIOS_RESOURCE_DIR) ? resourceName : SCENARIOS_RESOURCE_DIR + resourceName;
        try (var in = ScenarioLoader.class.getClassLoader().getResourceAsStream(fullResource)) {
            if (in == null) {
                // Also try absolute resource without prefix
                try (var directIn = ScenarioLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
                    if (directIn == null) {
                        throw new IllegalArgumentException("Scenario not found on classpath or filesystem: " + nameOrPath);
                    }
                    return load(directIn, nameOrPath);
                }
            }
            return load(in, nameOrPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read scenario resource: " + nameOrPath, e);
        }
    }

    /**
     * Loads a scenario from a filesystem path.
     *
     * @param path the scenario file path
     * @return the parsed and validated scenario
     */
    public static Scenario load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            var bytes = Files.readAllBytes(path);
            return load(new String(bytes, StandardCharsets.UTF_8), path.getFileName().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read scenario file: " + path, e);
        }
    }

    /**
     * Loads a scenario from an input stream.
     *
     * @param in                 the stream
     * @param scenarioIdentifier scenario identifier for error reporting
     * @return the parsed and validated scenario
     * @throws IOException on read failure
     */
    public static Scenario load(InputStream in, String scenarioIdentifier) throws IOException {
        var bytes = in.readAllBytes();
        return load(new String(bytes, StandardCharsets.UTF_8), scenarioIdentifier);
    }

    /**
     * Loads and validates a scenario from JSON text.
     *
     * @param json               the scenario JSON content
     * @param scenarioIdentifier scenario identifier for error reporting
     * @return the validated scenario
     */
    public static Scenario load(String json, String scenarioIdentifier) {
        Objects.requireNonNull(json, "json");
        var identifier = scenarioIdentifier == null || scenarioIdentifier.isBlank() ? "unnamed" : scenarioIdentifier;

        Object parsed;
        try {
            parsed = JsonTree.parse(json);
        } catch (IOException e) {
            throw new ScenarioValidationException(identifier, null, null, "Malformed JSON: " + e.getMessage());
        }

        var root = JsonTree.asObject(parsed);
        if (root == null) {
            throw new ScenarioValidationException(identifier, null, null, "Root JSON value must be an object");
        }

        // 1. format_version
        var formatVersionObj = root.get("format_version");
        if (formatVersionObj == null) {
            throw new ScenarioValidationException(identifier, null, "format_version", "missing format_version");
        }
        var formatVersion = asInt(formatVersionObj, -1);
        if (formatVersion != Scenario.SUPPORTED_FORMAT_VERSION) {
            throw new ScenarioValidationException(identifier, null, "format_version",
                    "unsupported format_version " + formatVersion + ", expected " + Scenario.SUPPORTED_FORMAT_VERSION);
        }

        // 2. name
        var name = JsonTree.string(root, "name");
        if (name == null || name.isBlank()) {
            throw new ScenarioValidationException(identifier, null, "name", "missing or blank name");
        }

        // 3. description
        var description = JsonTree.string(root, "description");

        // 4. start state
        var start = JsonTree.string(root, "start");
        if (start == null || start.isBlank()) {
            throw new ScenarioValidationException(identifier, null, "start", "missing or blank start state");
        }

        // 5. pairing code
        var pairingCode = JsonTree.string(root, "pairing_code");

        // 6. routes
        var routes = parseRoutes(root.get("routes"), identifier);

        // 7. states
        var statesObj = root.get("states");
        if (!(statesObj instanceof Map<?, ?> statesMap) || statesMap.isEmpty()) {
            throw new ScenarioValidationException(identifier, null, "states", "states must be a non-empty object");
        }

        var states = parseStates(statesMap, identifier);

        // Validate that start state exists
        if (!states.containsKey(start)) {
            throw new ScenarioValidationException(identifier, null, "start",
                    "start state '" + start + "' does not exist in states");
        }

        validateTransitions(states, identifier);

        return new Scenario(formatVersion, name, description, start, pairingCode, routes, states);
    }

    private static void validateTransitions(Map<String, StateDefinition> states, String identifier) {
        for (var entry : states.entrySet()) {
            var stateId = entry.getKey();
            var stateDef = entry.getValue();

            for (var linkEntry : stateDef.links().entrySet()) {
                var linkName = linkEntry.getKey();
                var linkDef = linkEntry.getValue();
                if (!states.containsKey(linkDef.next())) {
                    throw new ScenarioValidationException(identifier, stateId, "links." + linkName + ".next",
                            "dangling next: target state '" + linkDef.next() + "' does not exist");
                }
            }

            if (stateDef.auto() != null) {
                var autoNext = stateDef.auto().next();
                if (!states.containsKey(autoNext)) {
                    throw new ScenarioValidationException(identifier, stateId, "auto.next",
                            "dangling next: target state '" + autoNext + "' does not exist");
                }
            }
        }
    }

    private static Map<String, RouteDefinition> parseRoutes(Object routesObj, String scenario) {
        if (!(routesObj instanceof Map<?, ?> map)) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, RouteDefinition>();
        for (var entry : map.entrySet()) {
            var routeKey = String.valueOf(entry.getKey()).trim();
            if (!(entry.getValue() instanceof Map<?, ?> routeMap)) {
                throw new ScenarioValidationException(scenario, null, "routes." + routeKey,
                        "route definition must be an object");
            }
            var rep = routeMap.get("representation");
            if (!(rep instanceof Map<?, ?>)) {
                throw new ScenarioValidationException(scenario, null, "routes." + routeKey + ".representation",
                        "route representation must be a JSON object");
            }
            var status = asInt(routeMap.get("status"), 200);
            result.put(routeKey, new RouteDefinition(rep, status));
        }
        return result;
    }

    private static Map<String, StateDefinition> parseStates(Map<?, ?> statesMap, String scenario) {
        var states = new LinkedHashMap<String, StateDefinition>();
        for (var entry : statesMap.entrySet()) {
            var stateId = String.valueOf(entry.getKey()).trim();
            if (stateId.isEmpty()) {
                throw new ScenarioValidationException(scenario, null, "states", "empty state id");
            }
            if (!(entry.getValue() instanceof Map<?, ?> stateMapRaw)) {
                throw new ScenarioValidationException(scenario, stateId, null, "state definition must be an object");
            }
            @SuppressWarnings("unchecked")
            var stateMap = (Map<String, Object>) stateMapRaw;

            // representation
            var representation = stateMap.get("representation");
            if (!(representation instanceof Map<?, ?>)) {
                throw new ScenarioValidationException(scenario, stateId, "representation",
                        "state representation must be a JSON object");
            }

            // links
            var links = parseLinks(stateMap.get("links"), scenario, stateId);

            // events
            var events = parseEvents(stateMap.get("events"), scenario, stateId);

            // auto
            var auto = parseAuto(stateMap.get("auto"), scenario, stateId);

            states.put(stateId, new StateDefinition(representation, links, events, auto));
        }
        return states;
    }

    private static Map<String, LinkDefinition> parseLinks(Object linksObj, String scenario, String stateId) {
        if (!(linksObj instanceof Map<?, ?> linksMap)) {
            return Map.of();
        }
        var links = new LinkedHashMap<String, LinkDefinition>();
        for (var entry : linksMap.entrySet()) {
            var linkName = String.valueOf(entry.getKey()).trim();
            if (linkName.isEmpty()) {
                throw new ScenarioValidationException(scenario, stateId, "links", "empty link name");
            }
            if (!(entry.getValue() instanceof Map<?, ?> linkMapRaw)) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkName,
                        "link definition must be an object");
            }
            @SuppressWarnings("unchecked")
            var linkMap = (Map<String, Object>) linkMapRaw;

            var next = JsonTree.string(linkMap, "next");
            if (next == null || next.isBlank()) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkName + ".next",
                        "missing or blank next state");
            }

            var method = JsonTree.string(linkMap, "method");
            var href = JsonTree.string(linkMap, "href");
            var delayMs = asLong(linkMap.get("delay_ms"), 0);
            if (delayMs < 0) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkName + ".delay_ms",
                        "delay_ms cannot be negative");
            }
            var status = asInt(linkMap.get("status"), 200);
            var error = JsonTree.string(linkMap, "error");

            links.put(linkName, new LinkDefinition(next, method, href, delayMs, status, error));
        }
        return links;
    }

    private static List<EventDefinition> parseEvents(Object eventsObj, String scenario, String stateId) {
        if (!(eventsObj instanceof List<?> list)) {
            return List.of();
        }
        var events = new ArrayList<EventDefinition>();
        for (var i = 0; i < list.size(); i++) {
            var item = list.get(i);
            if (!(item instanceof Map<?, ?> eventMapRaw)) {
                throw new ScenarioValidationException(scenario, stateId, "events[" + i + "]",
                        "event definition must be an object");
            }
            @SuppressWarnings("unchecked")
            var eventMap = (Map<String, Object>) eventMapRaw;

            var afterMs = asLong(eventMap.get("after_ms"), -1);
            if (afterMs < 0) {
                throw new ScenarioValidationException(scenario, stateId, "events[" + i + "].after_ms",
                        "missing or negative after_ms");
            }

            var event = JsonTree.string(eventMap, "event");
            if (event == null || event.isBlank()) {
                throw new ScenarioValidationException(scenario, stateId, "events[" + i + "].event",
                        "missing or blank event name");
            }

            var data = eventMap.get("data");
            if (data == null) {
                throw new ScenarioValidationException(scenario, stateId, "events[" + i + "].data",
                        "missing event data");
            }

            events.add(new EventDefinition(afterMs, event, data));
        }
        return events;
    }

    private static AutoTransitionDefinition parseAuto(Object autoObj, String scenario, String stateId) {
        if (autoObj == null) {
            return null;
        }
        if (!(autoObj instanceof Map<?, ?> autoMapRaw)) {
            throw new ScenarioValidationException(scenario, stateId, "auto", "auto definition must be an object");
        }
        @SuppressWarnings("unchecked")
        var autoMap = (Map<String, Object>) autoMapRaw;

        var afterMs = asLong(autoMap.get("after_ms"), -1);
        if (afterMs < 0) {
            throw new ScenarioValidationException(scenario, stateId, "auto.after_ms", "missing or negative after_ms");
        }

        var next = JsonTree.string(autoMap, "next");
        if (next == null || next.isBlank()) {
            throw new ScenarioValidationException(scenario, stateId, "auto.next", "missing or blank next state");
        }

        return new AutoTransitionDefinition(afterMs, next);
    }

    private static int asInt(Object val, int defaultVal) {
        if (val instanceof JsonNumber jn) {
            return (int) jn.longValue();
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        return defaultVal;
    }

    private static long asLong(Object val, long defaultVal) {
        if (val instanceof JsonNumber jn) {
            return jn.longValue();
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        return defaultVal;
    }
}
