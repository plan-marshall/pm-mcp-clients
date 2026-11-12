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
    @SuppressWarnings({"java:S1075", "java:S135"})
    public boolean handle(MockHttpRequest request, MockHttpResponse response,
            Map<String, Object> machineValues) throws IOException {
        var collection = PATH.equals(request.path());
        if (!collection && !request.path().startsWith(PATH + SLASH)) {
            return false;
        }
        if ("GET".equals(request.method())) {
            if (!collection) {
                return false;
            }
            response.json(200, effective(machineValues, request.query()));
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
            put(response, level, key, readChange(request), scope, machineValues);
            return true;
        }
        if ("DELETE".equals(request.method())) {
            delete(response, level, key, readChange(request), scope, machineValues);
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

    private void put(MockHttpResponse response, ConfigOrigin level, String key, ConfigValueChange change,
            @Nullable String scope, Map<String, Object> machineValues) throws IOException {
        if (isAspectOwned(key)) {
            response.json(409, Map.of(KEY_ERROR, ASPECT_OWNED));
            return;
        }
        var current = revisionOf(level, scope, key);
        if (change.revision() != current) {
            conflict(response, key, level, scope, current, machineValues);
            return;
        }
        byLevel(level, scope).put(key, change.value());
        var next = ++revision;
        revisionsByLevel(level, scope).put(key, next);
        response.json(200, resource(key, level, change.value(), null, next));
    }

    private void delete(MockHttpResponse response, ConfigOrigin level, String key, ConfigValueChange change,
            @Nullable String scope, Map<String, Object> machineValues) throws IOException {
        if (isAspectOwned(key)) {
            response.json(409, Map.of(KEY_ERROR, ASPECT_OWNED));
            return;
        }
        var values = byLevel(level, scope);
        if (!values.containsKey(key)) {
            response.json(404, Map.of(KEY_ERROR, "config_key_unknown"));
            return;
        }
        var current = revisionOf(level, scope, key);
        if (change.revision() != current) {
            conflict(response, key, level, scope, current, machineValues);
            return;
        }
        values.remove(key);
        revisionsByLevel(level, scope).remove(key);
        response.send(204, null, "");
    }

    /**
     * @param level the level a change addresses
     * @param scope that level's scope, or {@code null} for the machine's store
     * @param key   the key the change addresses
     * @return the revision the value at that level was last written at, {@code 0} when never written
     */
    private long revisionOf(ConfigOrigin level, @Nullable String scope, String key) {
        return revisionsByLevel(level, scope).getOrDefault(key, 0L);
    }

    private void conflict(MockHttpResponse response, String key, ConfigOrigin level, @Nullable String scope,
            long current, Map<String, Object> machineValues) throws IOException {
        var values = byLevel(level, scope);
        var present = values.containsKey(key);
        var origin = present ? level
                : (level == ConfigOrigin.MACHINE && machineValues.containsKey(key)
                        ? ConfigOrigin.MACHINE : ConfigOrigin.DEFAULT);
        var currentValue = present ? values.get(key)
                : (level == ConfigOrigin.MACHINE ? machineValues.get(key) : null);
        var body = new LinkedHashMap<String, Object>(
                new ConfigValueResource(key, currentValue, origin, null, current).toMap());
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

    private List<Map<String, Object>> effective(Map<String, Object> machineValues, @Nullable String query) {
        var project = scopeValue(query, KEY_PROJECT);
        var plan = scopeValue(query, KEY_PLAN);
        var keys = new TreeMap<String, Boolean>();
        machineValues.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        for (var level : OPERATOR_LEVELS) {
            byLevel(level, levelScope(level, project, plan)).keySet()
                    .forEach(key -> keys.put(key, Boolean.TRUE));
        }
        var resources = new ArrayList<Map<String, Object>>();
        for (var key : keys.keySet()) {
            resources.add(effective(key, machineValues, project, plan));
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
            @Nullable String project, @Nullable String plan) {
        ConfigOrigin origin = null;
        Object value = null;
        ConfigOrigin overridden = null;
        long valueRevision = 0;
        var candidates = new ArrayList<ConfigOrigin>(OPERATOR_LEVELS);
        candidates.sort((a, b) -> Integer.compare(b.rank(), a.rank()));
        for (var level : candidates) {
            var scope = levelScope(level, project, plan);
            var values = byLevel(level, scope);
            var present = values.containsKey(key);
            Object levelValue = present ? values.get(key) : null;
            var fromMachine = false;
            if (!present && level == ConfigOrigin.MACHINE) {
                present = machineValues.containsKey(key);
                levelValue = present ? machineValues.get(key) : null;
                fromMachine = present;
            }
            if (!present) {
                continue;
            }
            if (origin == null) {
                origin = level;
                value = levelValue;
                valueRevision = fromMachine ? 0 : revisionOf(level, scope, key);
            } else {
                overridden = level;
                break;
            }
        }
        return resource(key, Objects.requireNonNullElse(origin, ConfigOrigin.DEFAULT), value, overridden,
                valueRevision);
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
}
