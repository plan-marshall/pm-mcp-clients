/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorChangedEvent;
import de.planmarshall.api.overview.NeedsOperatorItemRef;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.NeedsOperatorSummary;

/**
 * The console's notice rule for a {@code needs_operator} change (04-console.adoc#pushed): a notice is
 * raised for each item a change <em>adds</em> of a kind no dedicated event announces — and for
 * nothing else. A decrease raises no notice and no bell, and an item that
 * {@code decision.opened}, {@code hold.opened} or {@code condition.raised} already announced is
 * never announced a second time.
 *
 * <p>The added items are found by comparing the machine summary the event carries
 * ({@link NeedsOperatorChangedEvent#summary()}) with the one the previous event of the same place
 * carried: the contract's event names no item of its own, so the summary is the only place the
 * addition can be read.
 *
 * <p>A place seen for the first time has no previous summary of its own. Its baseline is what the
 * status read at start already showed ({@link #seed(Map)}): an item of a kind that read counted is
 * no addition, up to the count it showed, because it was badged before the console or the view
 * opened. Every other item the first summary carries is an addition — the first credential that
 * arrives after a start whose status read showed none is announced like any later one. Without a
 * seed nothing was shown, so every item of a first summary is an addition; an empty summary seen
 * first adds nothing.
 */
public final class NeedsOperatorNotices {

    /**
     * One notice the rule raises: the kind it names, its rendered text, and the place it names.
     *
     * @param kind  the kind the notice names
     * @param text  the rendered notice
     * @param place the scope the notice names, empty for a due cleanup
     */
    public record Notice(String kind, String text, String place) {
    }

    /** The kinds no other event announces, so {@code needs_operator.changed} is their only notice. */
    static final Set<NeedsOperatorKind> UNANNOUNCED = Set.of(
                        NeedsOperatorKind.CREDENTIAL,
                        NeedsOperatorKind.CLEANUP_DUE,
                        NeedsOperatorKind.SETUP,
                        NeedsOperatorKind.FINDINGS,
                        // A kind this version does not know: the operator still has to see it wait.
                        NeedsOperatorKind.OTHER);

    private static final String MACHINE = "machine";

    /** The last summary read per place, keyed by the place the event carries. */
    private final Map<String, Map<String, Integer>> lastSeen = new ConcurrentHashMap<>();

    /** What the status read at start showed, per kind: the baseline of a place seen first. */
    private final Map<NeedsOperatorKind, Integer> shownAtStart = new ConcurrentHashMap<>();

    /**
     * Seeds the baseline from the status read at start: the {@code needs_operator} counts per kind
     * that read reported. A later seed replaces an earlier one.
     *
     * @param shown the count per kind the status read showed; a kind it did not name showed nothing
     */
    public void seed(Map<NeedsOperatorKind, Integer> shown) {
        Objects.requireNonNull(shown, "shown");
        shownAtStart.clear();
        shown.forEach((kind, count) -> {
            if (count != null && count > 0) {
                shownAtStart.merge(kind, count, Integer::sum);
            }
        });
    }

    /**
     * Seeds the baseline from the runtime's status ({@code GET /api/v1/status}), whose
     * {@code needs_operator} member counts what waits per kind. A status that names no such member
     * showed nothing.
     *
     * @param status the status the runtime reported
     */
    public void seedFromStatus(Map<String, Object> status) {
        Objects.requireNonNull(status, "status");
        var shown = new LinkedHashMap<NeedsOperatorKind, Integer>();
        if (status.get("needs_operator") instanceof Map<?, ?> needs) {
            for (var entry : needs.entrySet()) {
                var count = countOf(entry.getValue());
                if (count > 0) {
                    shown.merge(NeedsOperatorKind.fromWireOrOther(String.valueOf(entry.getKey())), count,
                                        Integer::sum);
                }
            }
        }
        seed(shown);
    }

    /**
     * @param value the count the status reports for one kind
     * @return the count, or {@code 0} when the value is no whole number
     */
    private static int countOf(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof JsonNumber jsonNumber) {
            try {
                return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, jsonNumber.longValue()));
            } catch (NumberFormatException _) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Reads the notices one event raises: one per item it adds of a kind no other event announces.
     *
     * @param locale    the interface locale
     * @param eventName the SSE event name, or {@code null}
     * @param event     the parsed event frame
     * @return the notices, in the summary's order; empty for an event that adds nothing
     */
    public List<Notice> noticesOf(String locale, @Nullable String eventName, Map<String, Object> event) {
        Objects.requireNonNull(locale, "locale");
        if (!isNeedsOperatorChanged(eventName, event)) {
            return List.of();
        }
        var summary = summaryOf(event);
        if (summary == null) {
            return List.of();
        }
        var place = placeOf(event);
        var current = new LinkedHashMap<String, Integer>();
        var notices = new ArrayList<Notice>();
        for (var item : summary.items()) {
            current.merge(identityOf(item), 1, Integer::sum);
        }
        var previous = lastSeen.put(place, current);
        if (previous == null) {
            // A place seen first: what the status read at start already showed was badged before,
            // so it is announced once only; anything beyond it arrived since and is an addition.
            previous = baselineOf(summary);
        }
        var seen = new LinkedHashMap<String, Integer>();
        for (var item : summary.items()) {
            var identity = identityOf(item);
            var count = seen.merge(identity, 1, Integer::sum);
            // The identity counts, not a set of them: two link-less items of one kind are two
            // items, and the second one is an addition like any other.
            if (count <= previous.getOrDefault(identity, 0) || !UNANNOUNCED.contains(item.kind())) {
                continue;
            }
            var where = placeOfItem(item);
            var text = ConsoleNotice.formatHoldNotice(locale, item.kind().value(), where, "");
            var scope = where.isBlank() ? MessageCatalogue.format(locale, "common.kind.machine", Map.of()) : where;
            notices.add(new Notice(item.kind().value(), text, scope));
        }
        return notices;
    }

    /**
     * @param eventName the SSE event name, or {@code null}
     * @param event     the parsed event frame
     * @return {@code true} for a change of what waits
     */
    static boolean isNeedsOperatorChanged(@Nullable String eventName, Map<String, Object> event) {
        var name = NeedsOperatorChangedEvent.EVENT_NAME;
        return name.equals(eventName) || name.equals(JsonTree.string(event, "event"));
    }

    private static @Nullable NeedsOperatorSummary summaryOf(Map<String, Object> event) {
        var payload = ConsoleNotice.payloadOf(event);
        var raw = JsonTree.asObject(payload.get("needs_operator"));
        if (raw == null) {
            raw = JsonTree.asObject(event.get("needs_operator"));
        }
        if (raw == null) {
            return null;
        }
        try {
            return NeedsOperatorSummary.fromMap(raw);
        } catch (IOException | IllegalArgumentException _) {
            return null;
        }
    }

    private static String placeOf(Map<String, Object> event) {
        for (var key : List.of("project_id", "plan_id", "epic_id")) {
            var value = JsonTree.string(event, key);
            if (value == null) {
                value = JsonTree.string(ConsoleNotice.payloadOf(event), key);
            }
            if (value != null && !value.isBlank()) {
                return key + ":" + value;
            }
        }
        return MACHINE;
    }

    /**
     * @param summary the first summary of a place
     * @return the items of it the status read at start already showed, counted per identity: of each
     *         kind, the first as many as that read counted, in the summary's order
     */
    private Map<String, Integer> baselineOf(NeedsOperatorSummary summary) {
        var baseline = new LinkedHashMap<String, Integer>();
        var takenPerKind = new LinkedHashMap<NeedsOperatorKind, Integer>();
        for (var item : summary.items()) {
            var taken = takenPerKind.getOrDefault(item.kind(), 0);
            if (taken < shownAtStart.getOrDefault(item.kind(), 0)) {
                takenPerKind.put(item.kind(), taken + 1);
                baseline.merge(identityOf(item), 1, Integer::sum);
            }
        }
        return baseline;
    }

    private static String identityOf(NeedsOperatorItemRef item) {
        return item.kind().value() + "|" + (item.link() != null ? item.link() : "");
    }

    /**
     * @param item the added item
     * @return the place the notice names: the key of a credential, the checkout of a setup, the epic
     *         of new findings, and nothing for an item that belongs to no place
     */
    private static String placeOfItem(NeedsOperatorItemRef item) {
        return item.kind() == NeedsOperatorKind.CLEANUP_DUE ? "" : tailOf(item.link());
    }

    private static String tailOf(@Nullable String link) {
        if (link == null || link.isBlank()) {
            return "";
        }
        var trimmed = link.endsWith("/") ? link.substring(0, link.length() - 1) : link;
        var slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }
}
