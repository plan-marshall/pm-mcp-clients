/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a plan's composition instance changes ({@code instance.changed}).
 *
 * @param planId          plan identifier
 * @param instanceVersion new composition version number
 * @param reason          trigger description
 */
public record InstanceChangedEvent(
        String planId,
        int instanceVersion,
        String reason
) {
    public static final String EVENT_NAME = "instance.changed";

    public InstanceChangedEvent {
        Objects.requireNonNull(planId, "planId");
        reason = reason != null ? reason : "composition updated";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("instance_version", instanceVersion);
        map.put("reason", reason);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static InstanceChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in InstanceChangedEvent");

        var verNum = JsonTree.number(map, "instance_version");
        int instanceVersion = verNum != null ? verNum.intValue() : 1;

        var reason = JsonTree.string(map, "reason");

        return new InstanceChangedEvent(planId, instanceVersion, reason != null ? reason : "composition updated");
    }

    public static InstanceChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for InstanceChangedEvent");
        return fromMap(obj);
    }
}
