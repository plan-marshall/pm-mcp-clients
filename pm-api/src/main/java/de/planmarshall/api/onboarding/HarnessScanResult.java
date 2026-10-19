/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

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
 * One harness of the scan, with its version, whether it is supported and worker-eligible, and
 * what it reports of its token usage (UC-ONB-4; Client Profiles, Harness Scan and Tokens, Not
 * Money).
 *
 * @param harnessId      the client id of the profile, e.g. {@code claude}
 * @param name           the display name, e.g. {@code Claude Code}
 * @param path           the absolute path found, or {@code null} where it was not found
 * @param version        the version the probe reported, or {@code null} where none was found
 * @param found          {@code true} when the executable was found
 * @param supported      {@code true} when the version is in the supported range
 * @param workerEligible {@code true} when the harness can run workers
 * @param tokenUsage     what the harness and version report of their token usage
 * @param missingFigures the figures that are not reported when the coverage is partial
 * @param isDefault      {@code true} when this harness is the default of the run
 * @param usage          the per-figure usage report, or {@code null} while it is not known
 */
public record HarnessScanResult(
        String harnessId,
        String name,
        @Nullable String path,
        @Nullable String version,
        boolean found,
        boolean supported,
        boolean workerEligible,
        TokenUsageCoverage tokenUsage,
        List<String> missingFigures,
        boolean isDefault,
        @Nullable HarnessUsageReport usage
) {
    public HarnessScanResult {
        Objects.requireNonNull(harnessId, "harnessId");
        Objects.requireNonNull(name, "name");
        tokenUsage = tokenUsage != null ? tokenUsage : TokenUsageCoverage.NOT_REPORTED;
        missingFigures = missingFigures != null ? List.copyOf(missingFigures) : List.of();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("harness_id", harnessId);
        map.put("name", name);
        if (path != null) {
            map.put("path", path);
        }
        if (version != null) {
            map.put("version", version);
        }
        map.put("found", found);
        map.put("supported", supported);
        map.put("worker_eligible", workerEligible);
        map.put("token_usage", tokenUsage.wireValue());
        if (!missingFigures.isEmpty()) {
            map.put("missing_figures", missingFigures);
        }
        if (isDefault) {
            map.put("default", true);
        }
        if (usage != null) {
            map.put("usage", usage.toMap());
        }
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed record
     * @throws IOException on format failure
     */
    public static HarnessScanResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var harnessId = JsonTree.string(map, "harness_id");
        if (harnessId == null) {
            harnessId = JsonTree.string(map, "id");
        }
        if (harnessId == null) {
            throw new JsonFormatException("Missing harness_id in HarnessScanResult");
        }
        var name = JsonTree.string(map, "name");
        var missing = new ArrayList<String>();
        var list = JsonTree.asArray(map.get("missing_figures"));
        if (list != null) {
            for (var element : list) {
                if (element instanceof String text) {
                    missing.add(text);
                }
            }
        }
        HarnessUsageReport usage = null;
        var usageMap = JsonTree.asObject(map.get("usage"));
        if (usageMap != null) {
            usage = HarnessUsageReport.fromMap(usageMap);
        }
        return new HarnessScanResult(
                harnessId,
                name != null ? name : harnessId,
                JsonTree.string(map, "path"),
                JsonTree.string(map, "version"),
                Boolean.TRUE.equals(JsonTree.bool(map, "found")),
                Boolean.TRUE.equals(JsonTree.bool(map, "supported")),
                Boolean.TRUE.equals(JsonTree.bool(map, "worker_eligible")),
                TokenUsageCoverage.fromWire(JsonTree.string(map, "token_usage")),
                missing,
                Boolean.TRUE.equals(JsonTree.bool(map, "default")),
                usage);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static HarnessScanResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HarnessScanResult");
        }
        return fromMap(obj);
    }
}
