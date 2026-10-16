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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of the runtime's lifecycle status, draining state, and update availability.
 */
public record RuntimeStatus(
        String version,
        RuntimeLifecycleState state,
        @Nullable String drainingBy,
        boolean drainWithdrawable,
        int drainRemainingCount,
        List<String> drainRemainingItems,
        @Nullable String drainStartedAt,
        @Nullable String newerVersion,
        @Nullable String versionCheckedAt,
        @Nullable String checkFailedReason,
        boolean upgradeSupported,
        @Nullable String upgradeUnsupportedReason,
        @Nullable String lastUpgradeResult
) {
    public RuntimeStatus {
        Objects.requireNonNull(version, "version");
        state = state != null ? state : RuntimeLifecycleState.RUNNING;
        drainRemainingItems = drainRemainingItems != null ? List.copyOf(drainRemainingItems) : List.of();
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("version", version);
        map.put("state", state.value());
        if (drainingBy != null) {
            map.put("draining_by", drainingBy);
        }
        map.put("drain_withdrawable", drainWithdrawable);
        map.put("drain_remaining_count", drainRemainingCount);
        map.put("drain_remaining_items", drainRemainingItems);
        if (drainStartedAt != null) {
            map.put("drain_started_at", drainStartedAt);
        }
        if (newerVersion != null) {
            map.put("newer_version", newerVersion);
        }
        if (versionCheckedAt != null) {
            map.put("version_checked_at", versionCheckedAt);
        }
        if (checkFailedReason != null) {
            map.put("check_failed_reason", checkFailedReason);
        }
        map.put("upgrade_supported", upgradeSupported);
        if (upgradeUnsupportedReason != null) {
            map.put("upgrade_unsupported_reason", upgradeUnsupportedReason);
        }
        if (lastUpgradeResult != null) {
            map.put("last_upgrade_result", lastUpgradeResult);
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
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed status
     * @throws IOException on error
     */
    public static RuntimeStatus fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var version = JsonTree.string(map, "version");
        if (version == null) {
            throw new JsonFormatException("Missing version in RuntimeStatus");
        }
        var stateStr = JsonTree.string(map, "state");
        var state = RuntimeLifecycleState.fromValue(stateStr);
        var drainingBy = JsonTree.string(map, "draining_by");
        var drainWithdrawable = Boolean.TRUE.equals(map.get("drain_withdrawable"));
        var drainRemainingCount = JsonTree.integer(map, "drain_remaining_count") != null ? (int) JsonTree.integer(map, "drain_remaining_count") : 0;
        var drainRemainingItems = new ArrayList<String>();
        if (map.get("drain_remaining_items") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) drainRemainingItems.add(String.valueOf(item));
            }
        }
        var drainStartedAt = JsonTree.string(map, "drain_started_at");
        var newerVersion = JsonTree.string(map, "newer_version");
        var versionCheckedAt = JsonTree.string(map, "version_checked_at");
        var checkFailedReason = JsonTree.string(map, "check_failed_reason");
        var upgradeSupported = !Boolean.FALSE.equals(map.get("upgrade_supported"));
        var upgradeUnsupportedReason = JsonTree.string(map, "upgrade_unsupported_reason");
        var lastUpgradeResult = JsonTree.string(map, "last_upgrade_result");

        return new RuntimeStatus(
                version,
                state,
                drainingBy,
                drainWithdrawable,
                drainRemainingCount,
                drainRemainingItems,
                drainStartedAt,
                newerVersion,
                versionCheckedAt,
                checkFailedReason,
                upgradeSupported,
                upgradeUnsupportedReason,
                lastUpgradeResult
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed status
     * @throws IOException on error
     */
    public static RuntimeStatus fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RuntimeStatus");
        }
        return fromMap(obj);
    }
}
