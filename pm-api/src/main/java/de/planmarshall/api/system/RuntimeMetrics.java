/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Immutable metrics representation of the runtime daemon process and its environment.
 */
public record RuntimeMetrics(
        String version,
        String build,
        String startedAt,
        long uptimeSeconds,
        long heapUsedBytes,
        long heapMaxBytes,
        long residentMemoryBytes,
        @Nullable Double cpuPercent,
        long machineStoreBytes,
        long diskFreeBytes,
        String measuredAt,
        boolean socketListener,
        String loopbackListener,
        String networkListener,
        int pairedDevicesCount,
        int relaysBehindCount,
        @Nullable String relaysBehindVersion,
        @Nullable String newerVersion,
        @Nullable String newerVersionDate,
        @Nullable String newerVersionCheckedAt,
        @Nullable String releaseNotesLink,
        boolean upgradeSupported,
        @Nullable String upgradeUnsupportedReason
) {
    public RuntimeMetrics {
        Objects.requireNonNull(version, "version");
        build = build != null ? build : "";
        startedAt = startedAt != null ? startedAt : "";
        measuredAt = measuredAt != null ? measuredAt : "";
        loopbackListener = loopbackListener != null ? loopbackListener : "off";
        networkListener = networkListener != null ? networkListener : "off";
    }

    private static final String KEY_VERSION = "version";
    private static final String KEY_UPTIME_SECONDS = "uptime_seconds";
    private static final String KEY_HEAP_USED_BYTES = "heap_used_bytes";
    private static final String KEY_HEAP_MAX_BYTES = "heap_max_bytes";
    private static final String KEY_RESIDENT_MEMORY_BYTES = "resident_memory_bytes";
    private static final String KEY_MACHINE_STORE_BYTES = "machine_store_bytes";
    private static final String KEY_DISK_FREE_BYTES = "disk_free_bytes";
    private static final String KEY_PAIRED_DEVICES_COUNT = "paired_devices_count";
    private static final String KEY_RELAYS_BEHIND_COUNT = "relays_behind_count";

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(KEY_VERSION, version);
        map.put("build", build);
        map.put("started_at", startedAt);
        map.put(KEY_UPTIME_SECONDS, uptimeSeconds);
        map.put(KEY_HEAP_USED_BYTES, heapUsedBytes);
        map.put(KEY_HEAP_MAX_BYTES, heapMaxBytes);
        map.put(KEY_RESIDENT_MEMORY_BYTES, residentMemoryBytes);
        if (cpuPercent != null) {
            map.put("cpu_percent", cpuPercent);
        }
        map.put(KEY_MACHINE_STORE_BYTES, machineStoreBytes);
        map.put(KEY_DISK_FREE_BYTES, diskFreeBytes);
        map.put("measured_at", measuredAt);
        map.put("socket_listener", socketListener);
        map.put("loopback_listener", loopbackListener);
        map.put("network_listener", networkListener);
        map.put(KEY_PAIRED_DEVICES_COUNT, pairedDevicesCount);
        map.put(KEY_RELAYS_BEHIND_COUNT, relaysBehindCount);
        if (relaysBehindVersion != null) {
            map.put("relays_behind_version", relaysBehindVersion);
        }
        if (newerVersion != null) {
            map.put("newer_version", newerVersion);
        }
        if (newerVersionDate != null) {
            map.put("newer_version_date", newerVersionDate);
        }
        if (newerVersionCheckedAt != null) {
            map.put("newer_version_checked_at", newerVersionCheckedAt);
        }
        if (releaseNotesLink != null) {
            map.put("release_notes_link", releaseNotesLink);
        }
        map.put("upgrade_supported", upgradeSupported);
        if (upgradeUnsupportedReason != null) {
            map.put("upgrade_unsupported_reason", upgradeUnsupportedReason);
        }
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

    private static @Nullable Double parseCpuPercent(@Nullable Object cpuObj) {
        if (cpuObj instanceof Number n) {
            return n.doubleValue();
        } else if (cpuObj instanceof JsonNumber(String text)) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException _) {
                return null;
            }
        } else if (cpuObj instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException _) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed metrics
     * @throws IOException on parse failure
     */
    public static RuntimeMetrics fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var version = JsonTree.string(map, KEY_VERSION);
        if (version == null) {
            throw new JsonFormatException("Missing version in RuntimeMetrics");
        }
        var build = JsonTree.string(map, "build");
        var startedAt = JsonTree.string(map, "started_at");
        var uptimeVal = JsonTree.number(map, KEY_UPTIME_SECONDS);
        var heapUsedVal = JsonTree.number(map, KEY_HEAP_USED_BYTES);
        var heapMaxVal = JsonTree.number(map, KEY_HEAP_MAX_BYTES);
        var resMemVal = JsonTree.number(map, KEY_RESIDENT_MEMORY_BYTES);
        var cpuPercent = parseCpuPercent(map.get("cpu_percent"));
        var storeVal = JsonTree.number(map, KEY_MACHINE_STORE_BYTES);
        var diskVal = JsonTree.number(map, KEY_DISK_FREE_BYTES);
        var measuredAt = JsonTree.string(map, "measured_at");
        var socketListener = !Boolean.FALSE.equals(map.get("socket_listener"));
        var loopbackListener = JsonTree.string(map, "loopback_listener");
        var networkListener = JsonTree.string(map, "network_listener");
        var pairedVal = JsonTree.integer(map, KEY_PAIRED_DEVICES_COUNT);
        var relaysVal = JsonTree.integer(map, KEY_RELAYS_BEHIND_COUNT);
        var relaysBehindVersion = JsonTree.string(map, "relays_behind_version");
        var newerVersion = JsonTree.string(map, "newer_version");
        var newerVersionDate = JsonTree.string(map, "newer_version_date");
        var newerVersionCheckedAt = JsonTree.string(map, "newer_version_checked_at");
        var releaseNotesLink = JsonTree.string(map, "release_notes_link");
        var upgradeSupported = !Boolean.FALSE.equals(map.get("upgrade_supported"));
        var upgradeUnsupportedReason = JsonTree.string(map, "upgrade_unsupported_reason");

        return new RuntimeMetrics(
                version,
                build != null ? build : "",
                startedAt != null ? startedAt : "",
                uptimeVal != null ? uptimeVal.longValue() : 0L,
                heapUsedVal != null ? heapUsedVal.longValue() : 0L,
                heapMaxVal != null ? heapMaxVal.longValue() : 0L,
                resMemVal != null ? resMemVal.longValue() : 0L,
                cpuPercent,
                storeVal != null ? storeVal.longValue() : 0L,
                diskVal != null ? diskVal.longValue() : 0L,
                measuredAt != null ? measuredAt : "",
                socketListener,
                loopbackListener != null ? loopbackListener : "off",
                networkListener != null ? networkListener : "off",
                pairedVal != null ? pairedVal.intValue() : 0,
                relaysVal != null ? relaysVal.intValue() : 0,
                relaysBehindVersion,
                newerVersion,
                newerVersionDate,
                newerVersionCheckedAt,
                releaseNotesLink,
                upgradeSupported,
                upgradeUnsupportedReason
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed metrics
     * @throws IOException on parse failure
     */
    public static RuntimeMetrics fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RuntimeMetrics");
        }
        return fromMap(obj);
    }
}
