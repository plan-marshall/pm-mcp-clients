/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The part of the runtime status the entry reads: the runtime's version and lifecycle state, the
 * machine and its onboarding's progress, the two web listeners, and how many devices are paired
 * (01-processes-and-transport.adoc#local-api, the runtime status). It carries no worker, slot, or
 * finding of the machine; those belong to the system view.
 *
 * @param version            the runtime's version, or {@code null} when the status names none
 * @param status             the runtime's status, or {@code null}
 * @param lifecycleState     the runtime's lifecycle state, or {@code null}
 * @param host               the machine's host name, or {@code null}
 * @param onboarded          whether the machine's onboarding has completed
 * @param onboarding         the onboarding's progress
 * @param listeners          the web listeners the runtime serves
 * @param pairedDevicesCount the number of paired devices
 */
public record RuntimeEntryResource(
        @Nullable String version,
        @Nullable String status,
        @Nullable String lifecycleState,
        @Nullable String host,
        boolean onboarded,
        OnboardingProgress onboarding,
        List<ListenerResource> listeners,
        int pairedDevicesCount
) {

    private static final String FIELD_VERSION = "version";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_LIFECYCLE_STATE = "lifecycle_state";
    private static final String FIELD_HOST = "host";
    private static final String FIELD_MACHINE = "machine";
    private static final String FIELD_ONBOARDED = "onboarded";
    private static final String FIELD_ONBOARD = "onboard";
    private static final String FIELD_LISTENERS = "listeners";
    private static final String FIELD_WEB = "web";
    private static final String FIELD_PAIRED_DEVICES = "paired_devices_count";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_OPEN = "open";
    private static final String FIELD_LAN = "lan";
    private static final String FIELD_PORT = "port";
    private static final String FIELD_LAN_PORT = "lan_port";
    private static final String FIELD_LAN_FINGERPRINT = "lan_fingerprint";
    private static final String COMPLETED_STATE = "onboard.completed";

    public RuntimeEntryResource {
        Objects.requireNonNull(onboarding, "onboarding");
        listeners = listeners == null ? List.of() : List.copyOf(listeners);
        if (pairedDevicesCount < 0) {
            throw new IllegalArgumentException(
                    "pairedDevicesCount must not be negative in RuntimeEntryResource: " + pairedDevicesCount);
        }
    }

    /**
     * Converts to a JSON tree map, omitting absent optional fields.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (version != null) {
            map.put(FIELD_VERSION, version);
        }
        if (status != null) {
            map.put(FIELD_STATUS, status);
        }
        if (lifecycleState != null) {
            map.put(FIELD_LIFECYCLE_STATE, lifecycleState);
        }
        if (host != null) {
            map.put(FIELD_MACHINE, host);
        }
        map.put(FIELD_ONBOARDED, onboarded);
        map.put(FIELD_ONBOARD, onboarding.toMap());
        map.put(FIELD_LISTENERS, listeners.stream().map(ListenerResource::toMap).toList());
        map.put(FIELD_PAIRED_DEVICES, pairedDevicesCount);
        return map;
    }

    /**
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this resource to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses the entry part of a runtime status from a JSON tree map. A status that carries no
     * {@code listeners} array (the legacy shape) derives them from its {@code web} object.
     *
     * @param map the input map
     * @return the runtime entry resource
     * @throws IOException on a malformed member
     */
    public static RuntimeEntryResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var onboard = JsonTree.object(map, FIELD_ONBOARD);
        var onboarded = isOnboarded(map, onboard);
        var onboarding = onboard != null
                ? OnboardingProgress.fromMap(onboard)
                : (onboarded ? OnboardingProgress.complete() : OnboardingProgress.notStarted());
        var listeners = parseListeners(map.get(FIELD_LISTENERS));
        if (listeners == null) {
            listeners = deriveListeners(map);
        }
        return new RuntimeEntryResource(
                JsonTree.string(map, FIELD_VERSION),
                JsonTree.string(map, FIELD_STATUS),
                JsonTree.string(map, FIELD_LIFECYCLE_STATE),
                hostOf(map),
                onboarded,
                onboarding,
                listeners,
                pairedDevicesOf(map));
    }

    /**
     * Parses the entry part of a runtime status from a JSON string.
     *
     * @param json the JSON string
     * @return the runtime entry resource
     * @throws IOException on parse failure
     */
    public static RuntimeEntryResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RuntimeEntryResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads the entry part of a runtime status from a streaming parser positioned at its first
     * token.
     *
     * @param parser the parser
     * @return the runtime entry resource
     * @throws IOException on parse failure
     */
    public static RuntimeEntryResource readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RuntimeEntryResource");
        }
        return fromMap(obj);
    }

    private static boolean isOnboarded(Map<String, Object> map, @Nullable Map<String, Object> onboard) {
        if (Boolean.TRUE.equals(JsonTree.bool(map, FIELD_ONBOARDED))) {
            return true;
        }
        if (onboard != null) {
            if (Boolean.TRUE.equals(JsonTree.bool(onboard, "completed"))) {
                return true;
            }
            var state = JsonTree.string(onboard, "state");
            if (COMPLETED_STATE.equals(state)) {
                return true;
            }
        }
        var machine = JsonTree.object(map, FIELD_MACHINE);
        return machine != null && Boolean.TRUE.equals(JsonTree.bool(machine, FIELD_ONBOARDED));
    }

    private static @Nullable String hostOf(Map<String, Object> map) {
        var host = JsonTree.string(map, FIELD_HOST);
        if (host != null) {
            return host;
        }
        var machineText = JsonTree.string(map, FIELD_MACHINE);
        if (machineText != null) {
            return machineText;
        }
        var machine = JsonTree.object(map, FIELD_MACHINE);
        return machine != null ? JsonTree.string(machine, FIELD_HOST) : null;
    }

    private static int pairedDevicesOf(Map<String, Object> map) {
        var count = JsonTree.integer(map, FIELD_PAIRED_DEVICES);
        if (count == null) {
            count = JsonTree.integer(JsonTree.object(map, FIELD_WEB), FIELD_PAIRED_DEVICES);
        }
        return count != null ? Math.max(0, count) : 0;
    }

    private static @Nullable List<ListenerResource> parseListeners(Object value) throws IOException {
        if (value == null) {
            return null;
        }
        var array = JsonTree.asArray(value);
        if (array == null) {
            throw new JsonFormatException("listeners must be an array in RuntimeEntryResource");
        }
        var listeners = new ArrayList<ListenerResource>(array.size());
        for (var element : array) {
            var listener = JsonTree.asObject(element);
            if (listener == null) {
                throw new JsonFormatException("A listeners entry must be an object in RuntimeEntryResource");
            }
            listeners.add(ListenerResource.fromMap(listener));
        }
        return listeners;
    }

    private static List<ListenerResource> deriveListeners(Map<String, Object> map) {
        var web = JsonTree.object(map, FIELD_WEB);
        if (web == null) {
            return List.of();
        }
        var listeners = new ArrayList<ListenerResource>(2);
        var loopbackOpen = Boolean.TRUE.equals(JsonTree.bool(web, FIELD_OPEN))
                || Boolean.TRUE.equals(JsonTree.bool(web, FIELD_ENABLED));
        var loopbackPort = JsonTree.integer(web, FIELD_PORT);
        listeners.add(new ListenerResource(
                ListenerKind.LOOPBACK,
                loopbackOpen ? ListenerState.OPEN : ListenerState.CLOSED,
                loopbackPort,
                loopbackOpen && loopbackPort != null ? "http://127.0.0.1:" + loopbackPort : null,
                null));
        var lanOpen = Boolean.TRUE.equals(JsonTree.bool(web, FIELD_LAN));
        listeners.add(new ListenerResource(
                ListenerKind.LAN,
                lanOpen ? ListenerState.OPEN : ListenerState.CLOSED,
                JsonTree.integer(web, FIELD_LAN_PORT),
                null,
                JsonTree.string(web, FIELD_LAN_FINGERPRINT)));
        return listeners;
    }
}
