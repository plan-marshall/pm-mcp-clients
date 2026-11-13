/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.config.AspectOwnedKeys;
import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.config.ConfigValueChange;
import de.planmarshall.api.config.ConfigValueResource;
import de.planmarshall.api.json.JsonTree;

/**
 * The mock's configuration values: the three routes the transport table keeps (Runtime Model, Local
 * API) — {@code GET /api/v1/config} with its {@code ?project=}/{@code ?plan=} scope, and {@code PUT}
 * and {@code DELETE /api/v1/config/{level}/{key}}.
 *
 * <p>The store resolves a value the way the runtime does: the nearest level wins, and a value that
 * shadows a farther level names it. The machine's own values are supplied per request by the engine,
 * so a change of the machine settings shows through; a value written through {@code PUT} at the
 * machine level is the nearer one and shadows it.
 *
 * <p>Every written value keeps the revision it was written at. A change carries the revision it was
 * made against as a body member ({@link ConfigValueChange}); the first change wins and a change
 * against an older revision is refused {@code 409 conflict} with the current value.
 */
@SuppressWarnings({"java:S1075", "java:S135"})
public final class MockConfigRoutes {

    /** The path of the configuration values. */
    public static final String PATH = "/api/v1/config";

    private static final String KEY_VALUE = "value";
    private static final String KEY_ERROR = "error";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_PLAN = "plan";
    private static final String SLASH = "/";
    private static final int LEVEL_AND_KEY = 2;
    private static final String CONFLICT = "conflict";
    private static final String ASPECT_OWNED = "aspect_owned";

    /** The keys an aspect owns, and the one family a project's own value writes at its level. */
    private static final String CLEANUP_KEY_PREFIX = "cleanup.";

    /** The levels an operator may write, from the one catalogue that defines them. */
    private static final List<ConfigOrigin> OPERATOR_LEVELS = ConfigOrigin.operatorLevels().stream()
            .map(ConfigOrigin::fromWire)
            .toList();

    private final Map<String, Map<String, Object>> written = new LinkedHashMap<>();
    private final Map<String, Map<String, Long>> revisions = new LinkedHashMap<>();
    private long revision;

    /**
     * @param request       the request
     * @param response      the response to write
     * @param machineValues the machine's own values, by effective key
     * @return whether this request is one of the configuration routes
     * @throws IOException on write failure
     */
    public boolean handle(MockHttpRequest request, MockHttpResponse response,
            Map<String, Object> machineValues) throws IOException {
        return handle(request, response, machineValues, Map.of());
    }

    /**
     * @param request       the request
     * @param response      the response to write
     * @param machineValues the machine's own values, by effective key
     * @param planProjects  the project each plan of the scenario belongs to, by plan id
     * @return whether this request is one of the configuration routes
     * @throws IOException on write failure
     */
    @SuppressWarnings({"java:S1075", "java:S135"})
    public boolean handle(MockHttpRequest request, MockHttpResponse response,
            Map<String, Object> machineValues, Map<String, String> planProjects) throws IOException {
        var collection = PATH.equals(request.path());
        if (!collection && !request.path().startsWith(PATH + SLASH)) {
            return false;
        }
        if ("GET".equals(request.method())) {
            if (!collection) {
                return false;
            }
            response.json(200, effective(machineValues, request.query(), planProjects));
            return true;
        }
        var segments = segments(request.path());
        if (segments.size() != LEVEL_AND_KEY) {
            response.json(400, Map.of(KEY_ERROR, "config_level_and_key_required"));
            return true;
        }
        var level = ConfigOrigin.fromWireOrNull(segments.get(0));
        if (level == null || !OPERATOR_LEVELS.contains(level)) {
            response.json(400, Map.of(KEY_ERROR, "config_level_unknown"));
            return true;
        }
        var key = segments.get(1);
        var scope = scopeOf(request.query(), level);
        if ("PUT".equals(request.method())) {
            put(response, level, key, readChange(request), scope, machineValues, planProjects);
            return true;
        }
        if ("DELETE".equals(request.method())) {
            delete(response, level, key, readChange(request), scope, machineValues, planProjects);
            return true;
        }
        return false;
    }

    /**
     * @param query the request's query string, or {@code null}
     * @param name  the parameter to read
     * @return the parameter's value, or {@code null} when the query names nothing
     */
    private static @Nullable String scopeValue(@Nullable String query, String name) {
        if (query == null) {
            return null;
        }
        for (var pair : query.split("&")) {
            var separator = pair.indexOf('=');
            if (separator > 0 && name.equals(pair.substring(0, separator))) {
                var value = pair.substring(separator + 1);
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    /**
     * @param query the request's query string, or {@code null}
     * @param level the level the write addresses
     * @return the scope that level's value is stored under, or {@code null} for the machine's store
     */
    private static @Nullable String scopeOf(@Nullable String query, ConfigOrigin level) {
        @Nullable String name = switch (level) {
            case PLAN -> KEY_PLAN;
            case PROJECT -> KEY_PROJECT;
            default -> null;
        };
        if (name == null) {
            return null;
        }
        var value = scopeValue(query, name);
        return value == null ? null : name + "=" + value;
    }

    private synchronized void put(MockHttpResponse response, ConfigOrigin level, String key,
            ConfigValueChange change, @Nullable String scope, Map<String, Object> machineValues,
            Map<String, String> planProjects) throws IOException {
        if (isAspectOwnedAt(level, key)) {
            response.json(409, Map.of(KEY_ERROR, ASPECT_OWNED));
            return;
        }
        var served = served(key, level, scope, machineValues, planProjects);
        if (change.revision() != revisionOf(served)) {
            conflict(response, served);
            return;
        }
        byLevel(level, scope).put(key, change.value());
        var next = ++revision;
        revisionsByLevel(level, scope).put(key, next);
        response.json(200, resource(key, level, change.value(), null, next));
    }

    private synchronized void delete(MockHttpResponse response, ConfigOrigin level, String key,
            ConfigValueChange change, @Nullable String scope, Map<String, Object> machineValues,
            Map<String, String> planProjects) throws IOException {
        if (isAspectOwnedAt(level, key)) {
            response.json(409, Map.of(KEY_ERROR, ASPECT_OWNED));
            return;
        }
        var values = byLevel(level, scope);
        if (!values.containsKey(key)) {
            response.json(404, Map.of(KEY_ERROR, "config_key_unknown"));
            return;
        }
        var served = served(key, level, scope, machineValues, planProjects);
        if (change.revision() != revisionOf(served)) {
            conflict(response, served);
            return;
        }
        values.remove(key);
        // The revision stays: a later change made against the value the read served is still stale.
        var next = ++revision;
        revisionsByLevel(level, scope).put(key, next);
        response.send(204, null, "");
    }

    /**
     * @param key           the key a change addresses
     * @param level         the level the change addresses
     * @param scope         that level's scope, or {@code null} for the machine's store
     * @param machineValues the machine's own values, by effective key
     * @param planProjects  the project each plan belongs to, by plan id
     * @return the value the change is made against: the nearest value visible from the addressed
     *         level down, so a machine write never compares itself with a project's value
     */
    private Map<String, Object> served(String key, ConfigOrigin level, @Nullable String scope,
            Map<String, Object> machineValues, Map<String, String> planProjects) {
        var project = scopePart(scope, KEY_PROJECT);
        var plan = scopePart(scope, KEY_PLAN);
        var candidate = effective(key, machineValues, project, plan, planProjects, level);
        var levelProject = levelProject(project, plan, planProjects);
        var levelScope = levelScope(level, levelProject, plan);
        if (!byLevel(level, levelScope).containsKey(key) && revisionsByLevel(level, levelScope).containsKey(key)
                && revisionOf(candidate) == 0) {
            // A deleted value keeps its revision, so a change made before the deletion stays stale.
            return resource(key, ConfigOrigin.DEFAULT, null, null, storedRevision(level, levelScope, key));
        }
        return candidate;
    }

    /**
     * @param scope a scope as {@link #scopeOf} composes it, or {@code null}
     * @param name  the parameter the scope carries
     * @return the parameter's value, or {@code null} when the scope names it not
     */
    private static @Nullable String scopePart(@Nullable String scope, String name) {
        if (scope == null) {
            return null;
        }
        var prefix = name + "=";
        return scope.startsWith(prefix) ? scope.substring(prefix.length()) : null;
    }

    private static long revisionOf(Map<String, Object> served) {
        return served.get(ConfigValueResource.FIELD_REVISION) instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * @param level a level
     * @param scope that level's scope, or {@code null} for the machine's store
     * @param key   the key
     * @return the revision the value at that level was last written at, {@code 0} when never written
     */
    private long storedRevision(ConfigOrigin level, @Nullable String scope, String key) {
        return revisionsByLevel(level, scope).getOrDefault(key, 0L);
    }

    private void conflict(MockHttpResponse response, Map<String, Object> served) throws IOException {
        var body = new LinkedHashMap<String, Object>(served);
        body.put(KEY_ERROR, CONFLICT);
        response.json(409, body);
    }

    /**
     * Forgets every value a {@code PUT} wrote, so a reset or a scenario switch does not carry a
     * previous state's configuration.
     */
    public void clear() {
        written.clear();
        revisions.clear();
        revision = 0;
    }

    private Map<String, Object> byLevel(ConfigOrigin level, @Nullable String scope) {
        return written.computeIfAbsent(keyOf(level, scope), _ -> new LinkedHashMap<>());
    }

    private Map<String, Long> revisionsByLevel(ConfigOrigin level, @Nullable String scope) {
        return revisions.computeIfAbsent(keyOf(level, scope), _ -> new LinkedHashMap<>());
    }

    private static String keyOf(ConfigOrigin level, @Nullable String scope) {
        return level.wire() + "|" + Objects.toString(scope, "");
    }

    /**
     * @param request the request
     * @return the change the body carries: a {@link ConfigValueChange}, a body that is the value
     *         itself (made against revision {@code 0}), or an empty change
     * @throws IOException on a malformed body
     */
    private static ConfigValueChange readChange(MockHttpRequest request) throws IOException {
        var body = request.bodyText();
        if (body.isBlank()) {
            return ConfigValueChange.deletion(0);
        }
        var parsed = JsonTree.parse(body);
        var map = JsonTree.asObject(parsed);
        if (map != null) {
            if (map.containsKey(ConfigValueChange.FIELD_REVISION)) {
                return ConfigValueChange.fromMap(map);
            }
            if (map.containsKey(KEY_VALUE)) {
                return new ConfigValueChange(map.get(KEY_VALUE), 0);
            }
        }
        return new ConfigValueChange(parsed, 0);
    }

    private List<Map<String, Object>> effective(Map<String, Object> machineValues, @Nullable String query,
            Map<String, String> planProjects) {
        var project = scopeValue(query, KEY_PROJECT);
        var plan = scopeValue(query, KEY_PLAN);
        var levelProject = levelProject(project, plan, planProjects);
        var keys = new TreeMap<String, Boolean>();
        machineValues.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        for (var level : OPERATOR_LEVELS) {
            byLevel(level, levelScope(level, levelProject, plan)).keySet()
                    .forEach(key -> keys.put(key, Boolean.TRUE));
        }
        var resources = new ArrayList<Map<String, Object>>();
        for (var key : keys.keySet()) {
            resources.add(effective(key, machineValues, project, plan, planProjects));
        }
        return resources;
    }

    /**
     * @param level   a level
     * @param project the project the read names, or {@code null}
     * @param plan    the plan the read names, or {@code null}
     * @return the scope that level's values are stored under: each level keeps its own identity, so a
     *         plan read inherits the machine's and its project's values
     */
    private static @Nullable String levelScope(ConfigOrigin level, @Nullable String project, @Nullable String plan) {
        return switch (level) {
            case MACHINE, DEFAULT, DERIVED -> null;
            case PROJECT, TEAM -> project == null ? null : KEY_PROJECT + "=" + project;
            case PLAN -> plan == null ? null : KEY_PLAN + "=" + plan;
        };
    }

    private Map<String, Object> effective(String key, Map<String, Object> machineValues,
            @Nullable String project, @Nullable String plan, Map<String, String> planProjects) {
        return effective(key, machineValues, project, plan, planProjects, null);
    }

    /**
     * @param key           the key
     * @param machineValues the machine's own values
     * @param project       the project the read names, or {@code null}
     * @param plan          the plan the read names, or {@code null}
     * @param planProjects  the project each plan belongs to, by plan id
     * @param ceiling       the nearest level a change may be compared with, or {@code null} for the
     *                      whole chain
     * @return the effective value
     */
    private Map<String, Object> effective(String key, Map<String, Object> machineValues,
            @Nullable String project, @Nullable String plan, Map<String, String> planProjects,
            @Nullable ConfigOrigin ceiling) {
        ConfigOrigin origin = null;
        Object value = null;
        ConfigOrigin overridden = null;
        long valueRevision = 0;
        var levelProject = levelProject(project, plan, planProjects);
        var candidates = new ArrayList<ConfigOrigin>(OPERATOR_LEVELS);
        candidates.sort((a, b) -> Integer.compare(b.rank(), a.rank()));
        for (var level : candidates) {
            if (ceiling != null && level.rank() > ceiling.rank()) {
                continue;
            }
            var scope = levelScope(level, levelProject, plan);
            var atLevel = levelValue(level, scope, key, machineValues);
            if (!atLevel.present()) {
                continue;
            }
            if (origin == null) {
                origin = level;
                value = atLevel.value();
                valueRevision = atLevel.fromMachine() ? 0 : storedRevision(level, scope, key);
            } else {
                overridden = level;
                break;
            }
        }
        return resource(key, Objects.requireNonNullElse(origin, ConfigOrigin.DEFAULT), value, overridden,
                valueRevision);
    }

    /**
     * @param project      the project the read names, or {@code null}
     * @param plan         the plan the read names, or {@code null}
     * @param planProjects the project each plan belongs to, by plan id
     * @return the project whose level a plan-only read inherits
     */
    private static @Nullable String levelProject(@Nullable String project, @Nullable String plan,
            Map<String, String> planProjects) {
        if (project != null) {
            return project;
        }
        return plan == null ? null : planProjects.get(plan);
    }

    /**
     * @param level         a level
     * @param scope         that level's scope, or {@code null} for the machine's store
     * @param key           the key
     * @param machineValues the machine's own values, by effective key
     * @return the value the level holds, and whether it came from the machine's own values
     */
    private LevelValue levelValue(ConfigOrigin level, @Nullable String scope, String key,
            Map<String, Object> machineValues) {
        var values = byLevel(level, scope);
        if (values.containsKey(key)) {
            return new LevelValue(true, values.get(key), false);
        }
        if (level == ConfigOrigin.MACHINE && machineValues.containsKey(key)) {
            return new LevelValue(true, machineValues.get(key), true);
        }
        return new LevelValue(false, null, false);
    }

    /**
     * @param present     whether the level holds a value
     * @param value       the value, or {@code null}
     * @param fromMachine whether the value came from the machine's own values
     */
    private record LevelValue(boolean present, @Nullable Object value, boolean fromMachine) {
    }

    private Map<String, Object> resource(String key, ConfigOrigin origin, @Nullable Object value,
            @Nullable ConfigOrigin overridden, long revisionOfValue) {
        var overrides = overridden != null && origin.overrides(overridden) ? overridden : null;
        return new ConfigValueResource(key, value, origin, overrides, revisionOfValue).toMap();
    }

    /**
     * @param path the request path
     * @return the segments after the route's own path
     */
    private static List<String> segments(String path) {
        var rest = path.substring(PATH.length());
        var segments = new ArrayList<String>();
        for (var segment : rest.split(SLASH)) {
            if (!segment.isBlank()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    /**
     * @param key a configuration key
     * @return {@code true} when an aspect owns the key and only its verb changes it
     */
    static boolean isAspectOwned(String key) {
        return AspectOwnedKeys.isAspectOwned(key);
    }

    /**
     * @param level the level the change addresses
     * @param key   the key a change addresses
     * @return whether an aspect owns the key at that level; a cleanup duration's PROJECT value is
     *         written by {@code config cleanup --project}, the aspect verb that owns it, while the
     *         machine's own value stays the machine settings' alone
     */
    static boolean isAspectOwnedAt(ConfigOrigin level, String key) {
        if (!AspectOwnedKeys.isAspectOwned(key)) {
            return false;
        }
        return !(key.startsWith(CLEANUP_KEY_PREFIX)
                && (level == ConfigOrigin.PROJECT || level == ConfigOrigin.TEAM));
    }
}
