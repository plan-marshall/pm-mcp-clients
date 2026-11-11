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

import de.planmarshall.api.config.ConfigOrigin;
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

    /** The levels an operator may write, from the one catalogue that defines them. */
    private static final List<ConfigOrigin> OPERATOR_LEVELS = ConfigOrigin.operatorLevels().stream()
            .map(ConfigOrigin::fromWire)
            .toList();

    /** The keys an aspect owns, as the terminal client refuses them (`409 aspect_owned`). */
    private static final List<String> ASPECT_OWNED = List.of(
            "language.", "harness.", "models.", "credentials.store", "providers.",
            "plan_defaults.", "sources.documents_mode", "roles.");

    private final Map<String, Map<String, Object>> written = new LinkedHashMap<>();
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
        var scope = scopeOf(request.query());
        if ("PUT".equals(request.method())) {
            put(response, level, key, readValue(request), scope);
            return true;
        }
        if ("DELETE".equals(request.method())) {
            delete(response, level, key, scope);
            return true;
        }
        return false;
    }

    /**
     * @param query the request's query string, or {@code null}
     * @return the project or plan the query names, or {@code null} when it names neither
     */
    private static @Nullable String scopeOf(@Nullable String query) {
        if (query == null) {
            return null;
        }
        for (var pair : query.split("&")) {
            var separator = pair.indexOf('=');
            if (separator > 0) {
                var name = pair.substring(0, separator);
                var value = pair.substring(separator + 1);
                if ((KEY_PROJECT.equals(name) || KEY_PLAN.equals(name)) && !value.isBlank()) {
                    return name + "=" + value;
                }
            }
        }
        return null;
    }

    private void put(MockHttpResponse response, ConfigOrigin level, String key, @Nullable Object value,
            @Nullable String scope) throws IOException {
        if (isAspectOwned(key)) {
            response.json(409, Map.of(KEY_ERROR, "aspect_owned"));
            return;
        }
        byLevel(level, scope).put(key, value);
        revision++;
        response.json(200, resource(key, level, value));
    }

    private void delete(MockHttpResponse response, ConfigOrigin level, String key, @Nullable String scope)
            throws IOException {
        var removed = byLevel(level, scope).remove(key);
        if (removed == null) {
            response.json(404, Map.of(KEY_ERROR, "config_key_unknown"));
            return;
        }
        response.send(204, null, "");
    }

    private Map<String, Object> byLevel(ConfigOrigin level, @Nullable String scope) {
        return written.computeIfAbsent(level.wire() + "|" + Objects.toString(scope, ""),
                _ -> new LinkedHashMap<>());
    }

    /**
     * @param request the request
     * @return the value the body carries: the {@code value} field, or a body that is the value itself
     * @throws IOException on a malformed body
     */
    private static @Nullable Object readValue(MockHttpRequest request) throws IOException {
        var body = request.bodyText();
        if (body.isBlank()) {
            return null;
        }
        var parsed = JsonTree.parse(body);
        if (parsed instanceof Map<?, ?> map && map.containsKey(KEY_VALUE)) {
            return map.get(KEY_VALUE);
        }
        return parsed;
    }

    private List<Map<String, Object>> effective(Map<String, Object> machineValues, @Nullable String query) {
        var scope = scopeOf(query);
        var keys = new TreeMap<String, Boolean>();
        machineValues.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        for (var level : OPERATOR_LEVELS) {
            byLevel(level, scope).keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        }
        var resources = new ArrayList<Map<String, Object>>();
        for (var key : keys.keySet()) {
            resources.add(effective(key, machineValues, scope));
        }
        return resources;
    }

    private Map<String, Object> effective(String key, Map<String, Object> machineValues, @Nullable String scope) {
        ConfigOrigin origin = null;
        Object value = null;
        ConfigOrigin overridden = null;
        var candidates = new ArrayList<ConfigOrigin>(OPERATOR_LEVELS);
        candidates.sort((a, b) -> Integer.compare(b.rank(), a.rank()));
        for (var level : candidates) {
            var levelValue = byLevel(level, scope).get(key);
            if (levelValue == null && level == ConfigOrigin.MACHINE) {
                levelValue = machineValues.get(key);
            }
            if (levelValue == null) {
                continue;
            }
            if (origin == null) {
                origin = level;
                value = levelValue;
            } else {
                overridden = level;
                break;
            }
        }
        return resource(key, Objects.requireNonNullElse(origin, ConfigOrigin.DEFAULT), value, overridden);
    }

    private Map<String, Object> resource(String key, ConfigOrigin origin, @Nullable Object value) {
        return resource(key, origin, value, null);
    }

    private Map<String, Object> resource(String key, ConfigOrigin origin, @Nullable Object value,
            @Nullable ConfigOrigin overridden) {
        var overrides = overridden != null && origin.overrides(overridden) ? overridden : null;
        return new ConfigValueResource(key, value, origin, overrides, revision).toMap();
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
        for (var prefix : ASPECT_OWNED) {
            if (key.equals(prefix) || key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
