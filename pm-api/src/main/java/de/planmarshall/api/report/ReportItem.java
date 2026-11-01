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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Immutable report item notifying the operator of an event (UC-PLN-6).
 *
 * @param id            unique identifier (e.g. {@code "rep-0142"})
 * @param from          origin component
 * @param kind          report kind from closed enumeration
 * @param subject       subject line (uncatalogued or template rendered)
 * @param subjectParams parameter map for catalogue text substitution
 * @param body          optional body text
 * @param createdAt     when the underlying event happened
 * @param sentAt        when made available to the client
 * @param readAt        when marked read
 * @param readChannel   channel that marked read ({@code "web"} or {@code "cli"})
 * @param readDevice    device label or id that marked read
 * @param detailLink    hypermedia detail action URI
 */
public record ReportItem(
        String id,
        ReportFrom from,
        ReportKind kind,
        String subject,
        Map<String, Object> subjectParams,
        @Nullable String body,
        Instant createdAt,
        @Nullable Instant sentAt,
        @Nullable Instant readAt,
        @Nullable String readChannel,
        @Nullable String readDevice,
        @Nullable String detailLink,
        @Nullable String rawCreated,
        @Nullable String rawSent
) {
    public ReportItem(
            String id,
            ReportFrom from,
            ReportKind kind,
            String subject,
            Map<String, Object> subjectParams,
            @Nullable String body,
            Instant createdAt,
            @Nullable Instant sentAt,
            @Nullable Instant readAt,
            @Nullable String readChannel,
            @Nullable String readDevice,
            @Nullable String detailLink
    ) {
        this(id, from, kind, subject, subjectParams, body, createdAt, sentAt, readAt, readChannel, readDevice, detailLink, null, null);
    }
    private static final String FIELD_ID = "id";
    private static final String FIELD_FROM = "from";
    private static final String FIELD_KIND = "kind";
    private static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_SUBJECT_PARAMS = "subject_params";
    private static final String FIELD_BODY = "body";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_SENT_AT = "sent_at";
    private static final String FIELD_READ_AT = "read_at";
    private static final String FIELD_READ_CHANNEL = "read_channel";
    private static final String FIELD_READ_DEVICE = "read_device";
    private static final String FIELD_DETAIL_LINK = "detail_link";

    public ReportItem {
        Objects.requireNonNull(id, FIELD_ID);
        Objects.requireNonNull(from, FIELD_FROM);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(subject, FIELD_SUBJECT);
        subjectParams = subjectParams != null ? Collections.unmodifiableMap(new LinkedHashMap<>(subjectParams)) : Map.of();
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * True if this report has been marked read.
     *
     * @return boolean
     */
    public boolean isRead() {
        return readAt != null;
    }

    /**
     * True if sentAt was significantly delayed (> 60 seconds) compared to createdAt.
     *
     * @return boolean
     */
    public boolean isSentLater() {
        if (rawSent != null && rawCreated != null && !rawSent.equals(rawCreated)) {
            return true;
        }
        return sentAt != null && sentAt.isAfter(createdAt.plusSeconds(60));
    }

    /**
     * Optional hypermedia detail target.
     *
     * @return detail URI or null
     */
    public @Nullable String detail() {
        return detailLink;
    }

    /**
     * Display-formatted creation time string (e.g. {@code "today 11:20"}).
     *
     * @return formatted string
     */
    public String created() {
        return rawCreated != null ? rawCreated : formatTime(createdAt);
    }

    /**
     * Display-formatted sent time string.
     *
     * @return formatted string
     */
    public String sent() {
        if (rawSent != null) return rawSent;
        return sentAt != null ? formatTime(sentAt) : created();
    }

    public static String formatTime(Instant instant) {
        var zone = ZoneId.systemDefault();
        var localDate = instant.atZone(zone).toLocalDate();
        var today = LocalDate.now(zone);
        var timeStr = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(instant);
        if (localDate.equals(today)) {
            return "today " + timeStr;
        } else if (localDate.equals(today.minusDays(1))) {
            return "yesterday " + timeStr;
        } else {
            long days = ChronoUnit.DAYS.between(localDate, today);
            return days + " d ago";
        }
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_FROM, from.toMap());
        map.put(FIELD_KIND, kind.value());
        map.put(FIELD_SUBJECT, subject);
        if (!subjectParams.isEmpty()) {
            map.put(FIELD_SUBJECT_PARAMS, subjectParams);
        }
        if (body != null) map.put(FIELD_BODY, body);
        map.put(FIELD_CREATED_AT, createdAt.toString());
        if (sentAt != null) map.put(FIELD_SENT_AT, sentAt.toString());
        if (readAt != null) map.put(FIELD_READ_AT, readAt.toString());
        if (readChannel != null) map.put(FIELD_READ_CHANNEL, readChannel);
        if (readDevice != null) map.put(FIELD_READ_DEVICE, readDevice);
        if (detailLink != null) map.put(FIELD_DETAIL_LINK, detailLink);
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
     * Parses a report item from map.
     *
     * @param map tree map
     * @return parsed ReportItem
     * @throws IOException on parse failure
     */
    public static ReportItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, FIELD_ID);
        if (id == null) throw new JsonFormatException("Missing id in ReportItem");

        var kindStr = JsonTree.string(map, FIELD_KIND);
        if (kindStr == null) {
            kindStr = "plan.ended";
        }
        var kind = ReportKind.fromValue(kindStr);
        var from = parseFrom(map.get(FIELD_FROM), kindStr);

        var subject = JsonTree.string(map, FIELD_SUBJECT);
        if (subject == null) subject = "";

        var subjectParams = parseSubjectParams(map);

        var body = JsonTree.string(map, FIELD_BODY);
        if (body == null) {
            body = JsonTree.string(map, "summary");
        }

        var created = parseCreatedAt(map);
        var sent = parseSentAt(map, created.instant());
        var readAt = parseReadAt(map);

        var readChannel = JsonTree.string(map, FIELD_READ_CHANNEL);
        var readDevice = JsonTree.string(map, FIELD_READ_DEVICE);
        var detailLink = JsonTree.string(map, FIELD_DETAIL_LINK);
        if (detailLink == null) {
            detailLink = JsonTree.string(map, "detail");
        }

        return new ReportItem(id, from, kind, subject, subjectParams, body, created.instant(), sent.instant(), readAt, readChannel, readDevice, detailLink, created.raw(), sent.raw());
    }

    private static ReportFrom parseFrom(Object fromObj, String kindStr) throws IOException {
        if (fromObj == null) {
            return new ReportFrom("", null, null);
        }
        if (fromObj instanceof String path) {
            var parts = path.split("[>›]");
            var proj = parts[0].trim();
            String epic = null;
            String plan = null;
            if (parts.length >= 3) {
                epic = parts[1].trim();
                plan = parts[2].trim();
            } else if (parts.length == 2) {
                var second = parts[1].trim();
                if (kindStr.startsWith("plan")) {
                    plan = second;
                } else {
                    epic = second;
                }
            }
            return new ReportFrom(proj, epic, plan);
        }
        var fromMap = JsonTree.asObject(fromObj);
        if (fromMap == null) {
            throw new JsonFormatException("Expected JSON object or string for from in ReportItem");
        }
        return ReportFrom.fromMap(fromMap);
    }

    private static Map<String, Object> parseSubjectParams(Map<String, Object> map) {
        var paramsObj = map.get(FIELD_SUBJECT_PARAMS);
        if (paramsObj instanceof Map<?, ?> pm) {
            var m = new LinkedHashMap<String, Object>();
            for (var entry : pm.entrySet()) {
                m.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return m;
        }
        return Map.of();
    }

    private record TimeParseResult(@Nullable Instant instant, @Nullable String raw) {
    }

    private static TimeParseResult parseCreatedAt(Map<String, Object> map) {
        var createdAtStr = JsonTree.string(map, FIELD_CREATED_AT);
        String rawCreated = null;
        if (createdAtStr == null) {
            createdAtStr = JsonTree.string(map, "created");
            if (createdAtStr != null && !createdAtStr.contains("T")) {
                rawCreated = createdAtStr;
            }
        }
        Instant createdAt;
        if (createdAtStr != null) {
            try {
                createdAt = Instant.parse(createdAtStr);
            } catch (DateTimeParseException _) {
                rawCreated = createdAtStr;
                createdAt = Instant.now();
            }
        } else {
            createdAt = Instant.now();
        }
        return new TimeParseResult(createdAt, rawCreated);
    }

    private static TimeParseResult parseSentAt(Map<String, Object> map, Instant createdAt) {
        var sentAtStr = JsonTree.string(map, FIELD_SENT_AT);
        String rawSent = null;
        if (sentAtStr == null) {
            sentAtStr = JsonTree.string(map, "sent");
            if (sentAtStr != null && !sentAtStr.contains("T")) {
                rawSent = sentAtStr;
            }
        }
        Instant sentAt = null;
        if (sentAtStr != null) {
            try {
                sentAt = Instant.parse(sentAtStr);
            } catch (DateTimeParseException _) {
                rawSent = sentAtStr;
                sentAt = createdAt.plusSeconds(120);
            }
        }
        return new TimeParseResult(sentAt, rawSent);
    }

    private static @Nullable Instant parseReadAt(Map<String, Object> map) {
        var readAtStr = JsonTree.string(map, FIELD_READ_AT);
        if (readAtStr != null) {
            try {
                return Instant.parse(readAtStr);
            } catch (DateTimeParseException _) {
                return null;
            }
        }
        return null;
    }


    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed item
     * @throws IOException on error
     */
    public static ReportItem fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReportItem");
        }
        return fromMap(obj);
    }

    /**
     * Parses a list of report items from JSON array.
     *
     * @param json JSON array
     * @return list of items
     * @throws IOException on parse failure
     */
    public static List<ReportItem> parseList(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (!(parsed instanceof List<?> list)) {
            throw new JsonFormatException("Expected JSON array for ReportItem list");
        }
        var result = new ArrayList<ReportItem>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(fromMap(map));
            }
        }
        return result;
    }

    /**
     * Serializes a list of items to JSON array string.
     *
     * @param items list of items
     * @return JSON string
     */
    public static String writeList(List<ReportItem> items) {
        var list = items.stream().map(ReportItem::toMap).toList();
        return JsonTree.write(list);
    }
}
