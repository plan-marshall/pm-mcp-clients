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
import java.util.Set;


import lombok.experimental.UtilityClass;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.designer.WorkflowCatalogue;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.event.EventEnvelope;
import de.planmarshall.api.event.EventName;
import de.planmarshall.api.intent.EpicIntentResource;
import de.planmarshall.api.intent.OutlineResource;
import de.planmarshall.api.intent.PlanIntentResource;
import de.planmarshall.api.interaction.OperatorLink;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.process.EpicProcess;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.SystemSnapshot;

/**
 * Loads and validates scenario files against the closed scenario format specification
 * ({@code 05-mock-runtime.adoc#scenario-files}).
 * <p>
 * The schema is closed: an unknown member at the root, state, route, link, event, or auto level is
 * refused with the file, the state or route, and the offending field. Every state's
 * {@code representation} must be a valid {@link OperatorStateResource}: a scenario cannot declare a
 * state the operator contract does not allow, and every member the specification names is validated
 * against the {@code pm-api} codecs.
 */
@UtilityClass
public class ScenarioLoader {

    /** Default classpath directory for scenario files. */
    public static final String SCENARIOS_RESOURCE_DIR = "scenarios/";

    private static final Set<String> ROOT_FIELDS = Set.of(
            "format_version", "name", "description", "start", "pairing_code", "routes", "states");
    private static final Set<String> STATE_FIELDS = Set.of(
            "representation", "links", "events", "auto");
    private static final Set<String> ROUTE_FIELDS = Set.of("status", "representation", "headers");
    private static final Set<String> LINK_FIELDS = Set.of(
            "method", "href", "status", "next", "delay_ms", "error");
    private static final Set<String> EVENT_FIELDS = Set.of("after_ms", "event", "data");
    private static final Set<String> AUTO_FIELDS = Set.of("after_ms", "next");

    /** The engine-only fixture object a state's facts may carry. */
    private static final String RUNTIME_FIXTURE = "runtime";

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
        rejectUnknownFields(root, ROOT_FIELDS, identifier, null, "");

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

        // Validate that start state exists before parsing the states, so a missing start is named
        // as such rather than reported through the first state's representation.
        if (!statesMap.containsKey(start)) {
            throw new ScenarioValidationException(identifier, null, "start",
                    "start state '" + start + "' does not exist in states");
        }

        var states = parseStates(statesMap, identifier);

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
            @SuppressWarnings("unchecked")
            var typedRoute = (Map<String, Object>) routeMap;
            rejectUnknownFields(typedRoute, ROUTE_FIELDS, scenario, null, "routes." + routeKey);
            var rep = routeMap.get("representation");
            if (!(rep instanceof Map<?, ?>) && !(rep instanceof List<?>)) {
                throw new ScenarioValidationException(scenario, null, "routes." + routeKey + ".representation",
                        "route representation must be a JSON object or a list");
            }
            validateRouteRepresentation(routeKey, rep, scenario);
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
            rejectUnknownFields(stateMap, STATE_FIELDS, scenario, stateId, "");

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

            validateOperatorState(representation, links, events, scenario, stateId);

            states.put(stateId, new StateDefinition(representation, links, events, auto));
        }
        return states;
    }

    /**
     * Validates a state's representation as an operator representation, decoded with the codecs of
     * {@code pm-api}: every representation, event, and link relation must be one the contract allows,
     * every scenario link must agree with the operator link the state's representation offers by its
     * id, and every advertised operator link must have a scenario link, so a client that sends what the
     * representation offers is answered.
     * <p>
     * The operator representation is mandatory: a state without a valid {@code state.state_id} fails
     * the load, so every state of every scenario is one both clients can render.
     *
     * @param representation the state representation
     * @param links          the scenario links keyed by operator link id
     * @param events         the timed events of the state
     * @param scenario       the scenario identifier for error reporting
     * @param stateId        the state identifier
     */
    private static void validateOperatorState(Object representation, Map<String, LinkDefinition> links,
            List<EventDefinition> events, String scenario, String stateId) {
        var representationMap = JsonTree.asObject(representation);
        if (representationMap == null) {
            throw new ScenarioValidationException(scenario, stateId, "representation",
                    "the representation must be an operator state object");
        }
        var stateMap = JsonTree.asObject(representationMap.get(OperatorStateResource.FIELD_STATE));
        if (stateMap == null || !stateMap.containsKey(OperatorStateResource.FIELD_STATE_ID)) {
            throw new ScenarioValidationException(scenario, stateId, "representation.state.state_id",
                    "every state representation must be an OperatorStateResource with a state_id");
        }

        OperatorStateResource state;
        try {
            state = OperatorStateResource.fromMap(representationMap);
        } catch (IOException e) {
            throw new ScenarioValidationException(scenario, stateId, "representation", e.getMessage());
        }

        var facts = JsonTree.asObject(representationMap.get(OperatorStateResource.FIELD_FACTS));
        if (facts != null && facts.containsKey(RUNTIME_FIXTURE)
                && !(facts.get(RUNTIME_FIXTURE) instanceof Map<?, ?>)) {
            throw new ScenarioValidationException(scenario, stateId, "representation.facts.runtime",
                    "runtime fixture must be an object");
        }

        var advertised = new LinkedHashMap<String, OperatorLink>();
        state.links().forEach(link -> advertised.put(link.id(), link));
        state.items().forEach(item -> item.links().forEach(link -> advertised.put(link.id(), link)));

        links.forEach((linkId, definition) -> {
            var offered = advertised.get(linkId);
            if (offered == null) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkId,
                        "no operator link with this id is offered in the representation");
            }
            if (!offered.method().equalsIgnoreCase(definition.method())) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkId + ".method",
                        "the scenario method " + definition.method() + " disagrees with the offered " + offered.method());
            }
            var expectedHref = definition.href();
            if (expectedHref != null ? !expectedHref.equals(offered.href()) : !offered.href().endsWith("/" + linkId)) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkId + ".href",
                        "the scenario href disagrees with the offered " + offered.href());
            }
        });

        var withoutRoute = advertised.keySet().stream().filter(linkId -> !links.containsKey(linkId)).findFirst();
        if (withoutRoute.isPresent()) {
            throw new ScenarioValidationException(scenario, stateId, "representation.links",
                    "the advertised operator link '" + withoutRoute.get() + "' has no scenario link");
        }

        for (var i = 0; i < events.size(); i++) {
            validateOperatorEvent(events.get(i), i, scenario, stateId);
        }
    }

    private static void validateOperatorEvent(EventDefinition event, int index, String scenario, String stateId) {
        var name = event.event();
        if (!EventName.matchesGrammar(name) || !EventName.isCatalogueMember(name)) {
            throw new ScenarioValidationException(scenario, stateId, "events[" + index + "].event",
                    "unknown event name: " + name);
        }
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put(EventEnvelope.FIELD_EVENT, name);
        envelope.put(EventEnvelope.FIELD_ID, index + 1);
        envelope.put(EventEnvelope.FIELD_DATA, event.data());
        try {
            EventEnvelope.fromMap(envelope);
        } catch (IOException e) {
            throw new ScenarioValidationException(scenario, stateId, "events[" + index + "].data",
                    "the event is no envelope: " + e.getMessage());
        }
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
            rejectUnknownFields(linkMap, LINK_FIELDS, scenario, stateId, "links." + linkName);

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
            if (error != null && ApiErrorCode.fromWireOrNull(error) == null) {
                throw new ScenarioValidationException(scenario, stateId, "links." + linkName + ".error",
                        "unknown API error code: " + error);
            }

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
            rejectUnknownFields(eventMap, EVENT_FIELDS, scenario, stateId, "events[" + i + "]");

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
        rejectUnknownFields(autoMap, AUTO_FIELDS, scenario, stateId, "auto");

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

    /**
     * Refuses a member the closed schema does not define, naming the file, the state or route, and
     * the field.
     */
    private static void rejectUnknownFields(Map<String, Object> map, Set<String> allowed,
            String scenario, String stateId, String path) {
        for (var key : map.keySet()) {
            if (!allowed.contains(key)) {
                var field = path == null || path.isBlank() ? key : path + "." + key;
                throw new ScenarioValidationException(scenario, stateId, field,
                        "unknown field '" + key + "' in the closed scenario schema");
            }
        }
    }

    /**
     * Decodes a static route representation with the {@code pm-api} codec the endpoint's contract
     * names. A route the registry does not know is left to the callers; the known operator API
     * routes fail the load when their representation is not what the contract allows.
     */
    private static void validateRouteRepresentation(String routeKey, Object representation, String scenario) {
        try {
            if (representation instanceof List<?> units) {
                if (routeKey.endsWith("/workflows")) {
                    validateUnitList(units);
                }
                return;
            }
            var map = JsonTree.asObject(representation);
            if (map == null) {
                return;
            }
            switch (routeKey) {
                case "GET /api/v1/projects" -> ProjectExplorer.fromMap(map);
                case "GET /api/v1/reports" -> ReportsPage.fromMap(map);
                case "GET /api/v1/overview" -> ProjectOverviewResponse.fromJson(JsonTree.write(map));
                case "GET /api/v1/system" -> SystemSnapshot.fromMap(map);
                case "GET /api/v1/workflows/catalogue" -> WorkflowCatalogue.fromMap(map);
                case "GET /api/v1/system/cleanup" -> CleanupJob.fromMap(map);
                default -> decodePlanOrEpicRoute(routeKey, map);
            }
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException e) {
            throw new ScenarioValidationException(scenario, null, "routes." + routeKey + ".representation",
                    "representation does not decode with the pm-api codec: " + e.getMessage());
        }
    }

    /**
     * Decodes a route that names a plan or an epic by id with the codec of its resource. The
     * registry covers the detail, process, intent, outline, activity, and completion resources, so
     * a scenario cannot declare one the operator contract refuses; a route of another shape keeps
     * the generic fallback.
     *
     * @param routeKey the route key of the scenario
     * @param map      the route representation
     * @throws IOException if the representation does not decode with the resource's codec
     */
    private static void decodePlanOrEpicRoute(String routeKey, Map<String, Object> map) throws IOException {
        if (!routeKey.startsWith("GET ")) {
            return;
        }
        var path = routeKey.substring("GET ".length());
        if (!path.startsWith("/api/v1/plans/") && !path.startsWith("/api/v1/epics/")) {
            return;
        }
        var segments = path.substring(1).split("/", -1);
        var collection = segments[2];
        if (segments.length == 4) {
            if ("epics".equals(collection) && !segments[3].isBlank()) {
                EpicResource.fromMap(map);
            }
            // A plan's own detail has no codec of its own; its view is the process resource.
            return;
        }
        if (segments.length == 5 && !segments[3].isBlank()) {
            switch (segments[4]) {
                case "process" -> {
                    if ("plans".equals(collection)) {
                        ProcessTree.fromMap(map);
                    } else {
                        EpicProcess.fromMap(map);
                    }
                }
                case "intent" -> {
                    if ("plans".equals(collection)) {
                        PlanIntentResource.fromMap(map);
                    } else {
                        EpicIntentResource.fromMap(map);
                    }
                }
                case "outline" -> {
                    if ("plans".equals(collection)) {
                        OutlineResource.fromMap(map);
                    }
                }
                case "completion" -> {
                    if ("plans".equals(collection)) {
                        PlanCompletionReport.fromMap(map);
                    }
                }
                default -> {
                    // No closed codec is registered for this sub-resource yet.
                }
            }
            return;
        }
        if (segments.length == 7 && "plans".equals(collection) && "tasks".equals(segments[4])
                && "activity".equals(segments[6]) && !segments[3].isBlank()
                && !segments[5].isBlank()) {
            TaskActivity.fromMap(map);
        }
    }

    private static void validateUnitList(List<?> units) throws IOException {
        for (var unit : units) {
            WorkflowUnit.fromMap(JsonTree.asObject(unit));
        }
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
