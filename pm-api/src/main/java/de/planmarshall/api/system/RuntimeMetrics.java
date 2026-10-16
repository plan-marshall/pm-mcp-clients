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

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("version", version);
        map.put("build", build);
        map.put("started_at", startedAt);
        map.put("uptime_seconds", uptimeSeconds);
        map.put("heap_used_bytes", heapUsedBytes);
        map.put("heap_max_bytes", heapMaxBytes);
        map.put("resident_memory_bytes", residentMemoryBytes);
        if (cpuPercent != null) {
            map.put("cpu_percent", cpuPercent);
        }
        map.put("machine_store_bytes", machineStoreBytes);
        map.put("disk_free_bytes", diskFreeBytes);
        map.put("measured_at", measuredAt);
        map.put("socket_listener", socketListener);
        map.put("loopback_listener", loopbackListener);
        map.put("network_listener", networkListener);
        map.put("paired_devices_count", pairedDevicesCount);
        map.put("relays_behind_count", relaysBehindCount);
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

    /**
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed metrics
     * @throws IOException on parse failure
     */
    public static RuntimeMetrics fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var version = JsonTree.string(map, "version");
        if (version == null) {
            throw new JsonFormatException("Missing version in RuntimeMetrics");
        }
        var build = JsonTree.string(map, "build");
        var startedAt = JsonTree.string(map, "started_at");
        var uptimeSeconds = JsonTree.number(map, "uptime_seconds") != null ? JsonTree.number(map, "uptime_seconds").longValue() : 0L;
        var heapUsedBytes = JsonTree.number(map, "heap_used_bytes") != null ? JsonTree.number(map, "heap_used_bytes").longValue() : 0L;
        var heapMaxBytes = JsonTree.number(map, "heap_max_bytes") != null ? JsonTree.number(map, "heap_max_bytes").longValue() : 0L;
        var residentMemoryBytes = JsonTree.number(map, "resident_memory_bytes") != null ? JsonTree.number(map, "resident_memory_bytes").longValue() : 0L;
        Double cpuPercent = null;
        var cpuObj = map.get("cpu_percent");
        if (cpuObj instanceof Number n) {
            cpuPercent = n.doubleValue();
        } else if (cpuObj instanceof JsonNumber jn) {
            try {
                cpuPercent = Double.parseDouble(jn.text());
            } catch (NumberFormatException _) {
                // Ignore parse exception
            }
        } else if (cpuObj instanceof String s) {
            try {
                cpuPercent = Double.parseDouble(s);
            } catch (NumberFormatException _) {
                // Ignore parse exception
            }
        }
        var machineStoreBytes = JsonTree.number(map, "machine_store_bytes") != null ? JsonTree.number(map, "machine_store_bytes").longValue() : 0L;
        var diskFreeBytes = JsonTree.number(map, "disk_free_bytes") != null ? JsonTree.number(map, "disk_free_bytes").longValue() : 0L;
        var measuredAt = JsonTree.string(map, "measured_at");
        var socketListener = !Boolean.FALSE.equals(map.get("socket_listener"));
        var loopbackListener = JsonTree.string(map, "loopback_listener");
        var networkListener = JsonTree.string(map, "network_listener");
        var pairedDevicesCount = JsonTree.integer(map, "paired_devices_count") != null ? (int) JsonTree.integer(map, "paired_devices_count") : 0;
        var relaysBehindCount = JsonTree.integer(map, "relays_behind_count") != null ? (int) JsonTree.integer(map, "relays_behind_count") : 0;
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
                uptimeSeconds,
                heapUsedBytes,
                heapMaxBytes,
                residentMemoryBytes,
                cpuPercent,
                machineStoreBytes,
                diskFreeBytes,
                measuredAt != null ? measuredAt : "",
                socketListener,
                loopbackListener != null ? loopbackListener : "off",
                networkListener != null ? networkListener : "off",
                pairedDevicesCount,
                relaysBehindCount,
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
