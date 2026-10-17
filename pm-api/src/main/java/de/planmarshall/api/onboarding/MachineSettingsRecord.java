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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupDurations;

/**
 * What the machine recorded, as the machine's settings show it and change it (UC-ONB-10,
 * {@code settings.choose}).
 *
 * <p>Every change carries the revision the dialogue was opened with; a change made against an
 * older revision is refused with {@code conflict} and the current value is returned.
 *
 * @param revision         the settings revision
 * @param aspects          the recorded aspects with their summaries
 * @param languageInterface the interface language, e.g. {@code de}
 * @param languageModel     the model language, or {@code same}
 * @param documentsMode     how documents are taken into a plan
 * @param cleanupDurations  how long ended work is kept
 * @param planDefaults      how plans run by default
 * @param consents          the remembered consents
 * @param credentials       the credentials the machine keeps
 */
public record MachineSettingsRecord(
        int revision,
        List<MachineSettingsAspectSummary> aspects,
        String languageInterface,
        String languageModel,
        DocumentsMode documentsMode,
        CleanupDurations cleanupDurations,
        PlanDefaults planDefaults,
        List<ConsentRecord> consents,
        List<MachineCredentialStatus> credentials
) {
    public MachineSettingsRecord {
        aspects = aspects != null ? List.copyOf(aspects) : List.of();
        languageInterface = languageInterface != null && !languageInterface.isBlank() ? languageInterface : "en";
        languageModel = languageModel != null && !languageModel.isBlank() ? languageModel : "same";
        documentsMode = documentsMode != null ? documentsMode : DocumentsMode.ASK;
        cleanupDurations = cleanupDurations != null ? cleanupDurations : CleanupDurations.defaults();
        planDefaults = planDefaults != null ? planDefaults : PlanDefaults.shipped();
        consents = consents != null ? List.copyOf(consents) : List.of();
        credentials = credentials != null ? List.copyOf(credentials) : List.of();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("revision", revision);
        map.put("aspects", aspects.stream().map(MachineSettingsAspectSummary::toMap).toList());
        map.put("language_interface", languageInterface);
        map.put("language_model", languageModel);
        map.put("documents_mode", documentsMode.wireValue());
        map.put("cleanup_durations", cleanupDurations.toMap());
        map.put("plan_defaults", planDefaults.toMap());
        map.put("consents", consents.stream().map(ConsentRecord::toMap).toList());
        map.put("credentials", credentials.stream().map(MachineCredentialStatus::toMap).toList());
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
    public static MachineSettingsRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var revision = JsonTree.integer(map, "revision");
        var aspects = readList(map, "aspects", MachineSettingsAspectSummary::fromMap);
        var consents = readList(map, "consents", ConsentRecord::fromMap);
        var credentials = readList(map, "credentials", MachineCredentialStatus::fromMap);
        var cleanupObj = JsonTree.object(map, "cleanup_durations");
        var planObj = JsonTree.object(map, "plan_defaults");
        return new MachineSettingsRecord(
                revision != null ? revision : 0,
                aspects,
                JsonTree.string(map, "language_interface"),
                JsonTree.string(map, "language_model"),
                DocumentsMode.fromWire(JsonTree.string(map, "documents_mode")),
                cleanupObj != null ? CleanupDurations.fromMap(cleanupObj) : null,
                planObj != null ? PlanDefaults.fromMap(planObj) : null,
                consents,
                credentials);
    }

    private static <T> List<T> readList(Map<String, Object> map, String name, MapReader<T> reader)
            throws IOException {
        var result = new ArrayList<T>();
        var list = JsonTree.asArray(map.get(name));
        if (list != null) {
            for (var element : list) {
                var obj = JsonTree.asObject(element);
                if (obj != null) {
                    result.add(reader.read(obj));
                }
            }
        }
        return result;
    }

    /** Reads one element of a JSON array into a record that may fail to parse. */
    @FunctionalInterface
    private interface MapReader<T> {
        T read(Map<String, Object> map) throws IOException;
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static MachineSettingsRecord fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for MachineSettingsRecord");
        }
        return fromMap(obj);
    }
}
