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
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The result of running an unknown harness to verify its draft profile (UC-ONB-4; Client Profiles,
 * Harness Scan): the status of each {@link HarnessVerificationStage} and, for the {@code usage}
 * stage, the {@link HarnessUsageReport} the run found. A profile whose every stage passed is the one
 * installed; one that found a figure its draft names but the harness does not report fails the
 * {@code usage} stage.
 *
 * @param stages the status of each stage that ran, or was left not run
 * @param usage  the result of the {@code usage} stage, or {@code null} while it did not run
 */
public record HarnessVerification(
        Map<HarnessVerificationStage, VerificationStageStatus> stages,
        @Nullable HarnessUsageReport usage
) {
    public HarnessVerification {
        var copy = new EnumMap<HarnessVerificationStage, VerificationStageStatus>(HarnessVerificationStage.class);
        if (stages != null) {
            copy.putAll(stages);
        }
        stages = Collections.unmodifiableMap(copy);
    }

    /**
     * @param stage the stage to look up
     * @return the status of the stage, or {@link VerificationStageStatus#NOT_RUN} when it is not named
     */
    public VerificationStageStatus statusOf(HarnessVerificationStage stage) {
        Objects.requireNonNull(stage, "stage");
        return stages.getOrDefault(stage, VerificationStageStatus.NOT_RUN);
    }

    /**
     * @return the first stage that did not pass, in catalogue order, or empty when every named stage passed
     */
    public Optional<HarnessVerificationStage> firstFailure() {
        for (var stage : HarnessVerificationStage.values()) {
            if (statusOf(stage) == VerificationStageStatus.FAILED) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }

    /**
     * @return {@code true} when at least one stage ran and every named stage passed
     */
    public boolean passed() {
        return !stages.isEmpty() && stages.values().stream()
                .allMatch(status -> status == VerificationStageStatus.PASSED);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        var stageMap = new LinkedHashMap<String, Object>();
        for (var entry : stages.entrySet()) {
            stageMap.put(entry.getKey().wireValue(), entry.getValue().wireValue());
        }
        map.put("stages", stageMap);
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
     * @return parsed verification
     * @throws IOException on format failure
     */
    public static HarnessVerification fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var stages = new EnumMap<HarnessVerificationStage, VerificationStageStatus>(HarnessVerificationStage.class);
        var stageMap = JsonTree.asObject(map.get("stages"));
        if (stageMap != null) {
            for (var entry : stageMap.entrySet()) {
                var stage = HarnessVerificationStage.fromWire(entry.getKey());
                if (stage == null) {
                    throw new JsonFormatException("Unknown verification stage: " + entry.getKey());
                }
                var status = VerificationStageStatus.fromWire(
                        entry.getValue() instanceof String text ? text : null);
                if (status == null) {
                    throw new JsonFormatException("Unknown verification status: " + entry.getValue());
                }
                stages.put(stage, status);
            }
        }
        HarnessUsageReport usage = null;
        var usageMap = JsonTree.asObject(map.get("usage"));
        if (usageMap != null) {
            usage = HarnessUsageReport.fromMap(usageMap);
        }
        return new HarnessVerification(stages, usage);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed verification
     * @throws IOException on format failure
     */
    public static HarnessVerification fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HarnessVerification");
        }
        return fromMap(obj);
    }
}
