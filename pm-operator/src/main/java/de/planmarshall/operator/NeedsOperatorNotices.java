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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

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
 * addition can be read. A scope seen for the first time has no previous summary: every item it
 * carries is an addition, and an empty summary seen first adds nothing.
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
    private final Map<String, Set<String>> lastSeen = new ConcurrentHashMap<>();

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
        var current = new LinkedHashSet<String>();
        var notices = new ArrayList<Notice>();
        for (var item : summary.items()) {
            current.add(identityOf(item));
        }
        var previous = lastSeen.put(place, current);
        if (previous == null) {
            // The first summary of a place is the baseline, never an addition: what already waited
            // when the console started was badged before, so it is announced once only.
            return List.of();
        }
        for (var item : summary.items()) {
            if (previous.contains(identityOf(item)) || !UNANNOUNCED.contains(item.kind())) {
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
        } catch (IOException _) {
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
