/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Paged representation of unread reports from {@code GET /api/v1/reports}.
 *
 * @param items            reports on this page (newest first)
 * @param unreadCount      total count of unread reports across all pages
 * @param oldestUnreadTime timestamp of the oldest unread report
 * @param hasOlder         true if more older reports exist
 * @param olderCount       count of older unread reports beyond this page
 */
public record ReportsPage(
        List<ReportItem> items,
        int unreadCount,
        @Nullable Instant oldestUnreadTime,
        boolean hasOlder,
        int olderCount
) {
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_UNREAD_COUNT = "unread_count";
    private static final String FIELD_OLDEST_UNREAD_TIME = "oldest_unread_time";
    private static final String FIELD_HAS_OLDER = "has_older";
    private static final String FIELD_OLDER_COUNT = "older_count";

    public ReportsPage {
        items = items != null ? Collections.unmodifiableList(new ArrayList<>(items)) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ITEMS, items.stream().map(ReportItem::toMap).toList());
        map.put(FIELD_UNREAD_COUNT, unreadCount);
        if (oldestUnreadTime != null) {
            map.put(FIELD_OLDEST_UNREAD_TIME, oldestUnreadTime.toString());
        }
        map.put(FIELD_HAS_OLDER, hasOlder);
        map.put(FIELD_OLDER_COUNT, olderCount);
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
     * Parses from a JSON map or List.
     *
     * @param map tree map
     * @return parsed ReportsPage
     * @throws IOException on format failure
     */
    public static ReportsPage fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");

        List<ReportItem> items = parseItems(map);
        int unreadCount = parseCount(map.get(FIELD_UNREAD_COUNT), items.size());

        var oldestStr = JsonTree.string(map, FIELD_OLDEST_UNREAD_TIME);
        if (oldestStr == null) {
            oldestStr = JsonTree.string(map, "oldest_unread");
        }
        Instant oldest = parseInstant(oldestStr);

        int olderCount = parseCount(map.get(FIELD_OLDER_COUNT), 0);
        boolean hasOlder = Boolean.TRUE.equals(map.get(FIELD_HAS_OLDER)) || olderCount > 0;

        return new ReportsPage(items, unreadCount, oldest, hasOlder, olderCount);
    }

    private static List<ReportItem> parseItems(Map<String, Object> map) throws IOException {
        var itemsObj = map.get(FIELD_ITEMS);
        if (itemsObj == null) {
            itemsObj = map.get("reports");
        }
        if (itemsObj instanceof List<?> list) {
            var parsedItems = new ArrayList<ReportItem>(list.size());
            for (var elem : list) {
                var obj = JsonTree.asObject(elem);
                if (obj != null) {
                    parsedItems.add(ReportItem.fromMap(obj));
                }
            }
            return parsedItems;
        }
        return List.of();
    }

    private static int parseCount(@Nullable Object obj, int defaultValue) {
        if (obj instanceof JsonNumber jn) {
            return (int) jn.longValue();
        } else if (obj instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    @Nullable
    private static Instant parseInstant(@Nullable String str) {
        if (str == null) {
            return null;
        }
        try {
            return Instant.parse(str);
        } catch (DateTimeParseException _) {
            // Ignore non-ISO date string formats like "yesterday 18:40"
            return null;
        }
    }

    /**
     * Parses from JSON string. Accepts both a paginated object or bare array for compatibility.
     *
     * @param json JSON string
     * @return ReportsPage
     * @throws IOException on error
     */
    public static ReportsPage fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof List<?> list) {
            var items = new ArrayList<ReportItem>(list.size());
            for (var elem : list) {
                var obj = JsonTree.asObject(elem);
                if (obj != null) {
                    items.add(ReportItem.fromMap(obj));
                }
            }
            return new ReportsPage(items, items.size(), null, false, 0);
        }
        var obj = JsonTree.asObject(parsed);
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object or array for ReportsPage");
        }
        return fromMap(obj);
    }
}
