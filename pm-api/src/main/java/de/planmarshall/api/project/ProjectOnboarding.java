/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of a project onboarding instance (UC-PRJ-1).
 *
 * @param onboardingId       unique onboarding session or entry ID
 * @param checkoutPath       filesystem path of the checkout
 * @param state              current state of project onboarding
 * @param detectedDomains    detected skill domains (e.g. "maven", "java-cui")
 * @param discoveredModules  discovered module names/paths
 * @param credentialsNeeded  missing credentials needing operator attention
 */
public record ProjectOnboarding(
        String onboardingId,
        String checkoutPath,
        ProjectOnboardingState state,
        List<String> detectedDomains,
        List<String> discoveredModules,
        List<String> credentialsNeeded
) {
    public static final String FIELD_ONBOARDING_ID = "onboarding_id";
    public static final String FIELD_CHECKOUT_PATH = "checkout_path";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_DETECTED_DOMAINS = "detected_domains";
    public static final String FIELD_DISCOVERED_MODULES = "discovered_modules";
    public static final String FIELD_CREDENTIALS_NEEDED = "credentials_needed";

    public ProjectOnboarding {
        Objects.requireNonNull(onboardingId, "onboardingId");
        Objects.requireNonNull(checkoutPath, "checkoutPath");
        state = state != null ? state : ProjectOnboardingState.CHECKOUT;
        detectedDomains = detectedDomains != null ? List.copyOf(detectedDomains) : List.of();
        discoveredModules = discoveredModules != null ? List.copyOf(discoveredModules) : List.of();
        credentialsNeeded = credentialsNeeded != null ? List.copyOf(credentialsNeeded) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ONBOARDING_ID, onboardingId);
        map.put(FIELD_CHECKOUT_PATH, checkoutPath);
        map.put(FIELD_STATE, state.value());
        map.put(FIELD_DETECTED_DOMAINS, new ArrayList<>(detectedDomains));
        map.put(FIELD_DISCOVERED_MODULES, new ArrayList<>(discoveredModules));
        map.put(FIELD_CREDENTIALS_NEEDED, new ArrayList<>(credentialsNeeded));
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
     * Parses from JSON tree map.
     *
     * @param map input map
     * @return parsed onboarding instance
     * @throws IOException on format failure
     */
    public static ProjectOnboarding fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var onboardingId = JsonTree.string(map, FIELD_ONBOARDING_ID);
        if (onboardingId == null) {
            onboardingId = JsonTree.string(map, "id");
        }
        if (onboardingId == null || onboardingId.isBlank()) {
            throw new JsonFormatException("Missing onboarding_id in ProjectOnboarding");
        }

        var checkoutPath = JsonTree.string(map, FIELD_CHECKOUT_PATH);
        if (checkoutPath == null) {
            checkoutPath = JsonTree.string(map, "path");
        }
        if (checkoutPath == null || checkoutPath.isBlank()) {
            throw new JsonFormatException("Missing checkout_path in ProjectOnboarding");
        }

        var stateStr = JsonTree.string(map, FIELD_STATE);
        var state = ProjectOnboardingState.fromValue(stateStr);
        if (state == null) {
            state = ProjectOnboardingState.CHECKOUT;
        }

        var domains = new ArrayList<String>();
        var domainArray = JsonTree.asArray(map.get(FIELD_DETECTED_DOMAINS));
        if (domainArray != null) {
            for (var item : domainArray) {
                if (item instanceof String s) {
                    domains.add(s);
                }
            }
        }

        var modules = new ArrayList<String>();
        var moduleArray = JsonTree.asArray(map.get(FIELD_DISCOVERED_MODULES));
        if (moduleArray != null) {
            for (var item : moduleArray) {
                if (item instanceof String s) {
                    modules.add(s);
                }
            }
        }

        var creds = new ArrayList<String>();
        var credArray = JsonTree.asArray(map.get(FIELD_CREDENTIALS_NEEDED));
        if (credArray != null) {
            for (var item : credArray) {
                if (item instanceof String s) {
                    creds.add(s);
                }
            }
        }

        return new ProjectOnboarding(onboardingId, checkoutPath, state, domains, modules, creds);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed onboarding instance
     * @throws IOException on format failure
     */
    public static ProjectOnboarding fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ProjectOnboarding");
    }
}
