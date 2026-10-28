/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;


import lombok.experimental.UtilityClass;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupKeptItem;
import de.planmarshall.api.system.CleanupResidue;

/**
 * The cleanup fixtures of a scenario state, read from the operator facts the state declares.
 * <p>
 * The engine reads its defaults from the same shapes, so an entry the fixture leaves incomplete is
 * skipped instead of half-applied: a kept item needs its id, kind and label, and a residue needs
 * its step. Everything else falls back to the model's own default.
 */
@UtilityClass
final class CleanupFixtures {

    private static final String KEY_ITEM_ID = "item_id";
    private static final String KEY_KIND = "kind";
    private static final String KEY_LABEL = "label";
    private static final String KEY_REASON = "reason";
    private static final String KEY_KEPT_AT = "kept_at";
    private static final String KEY_KEPT_BY = "kept_by";
    private static final String KEY_STEP = "step";
    private static final String KEY_COUNT = "count";
    private static final String KEY_BYTES = "bytes";
    private static final String KEY_LAST_RUN = "last_run";

    /**
     * @param entries the fixture's kept entries
     * @return the kept items they name, skipping an entry without its required members
     */
    static List<CleanupKeptItem> keptItems(List<Map<String, Object>> entries) {
        return entries.stream()
                .map(CleanupFixtures::keptItem)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * @param entries the fixture's residue entries
     * @return the residues they name, skipping an entry without a step
     */
    static List<CleanupResidue> residues(List<Map<String, Object>> entries) {
        return entries.stream()
                .map(CleanupFixtures::residue)
                .filter(Objects::nonNull)
                .toList();
    }

    private static CleanupKeptItem keptItem(Map<String, Object> entry) {
        var id = text(entry, KEY_ITEM_ID);
        var kind = text(entry, KEY_KIND);
        var label = text(entry, KEY_LABEL);
        if (id.isBlank() || kind.isBlank() || label.isBlank()) {
            return null;
        }
        return new CleanupKeptItem(id, kind, label,
                text(entry, KEY_REASON),
                text(entry, KEY_KEPT_AT),
                text(entry, KEY_KEPT_BY));
    }

    private static CleanupResidue residue(Map<String, Object> entry) {
        var step = text(entry, KEY_STEP);
        if (step.isBlank()) {
            return null;
        }
        var bytes = JsonTree.number(entry, KEY_BYTES);
        var count = JsonTree.integer(entry, KEY_COUNT);
        return new CleanupResidue(step, count != null ? count : 0,
                bytes != null ? bytes.longValue() : 0L, text(entry, KEY_LAST_RUN));
    }

    private static String text(Map<String, Object> entry, String key) {
        var value = JsonTree.string(entry, key);
        return value != null ? value : "";
    }
}
