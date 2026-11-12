/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;

/**
 * Definition of a single hypermedia state in a scenario.
 *
 * @param representation the hypermedia state payload as returned by the runtime
 * @param links          offered link relations mapped to their target transitions
 * @param events         timed events to emit over SSE while in this state
 * @param auto           optional automatic transition without a request
 * @param chatFailures   the chat failures armed when the state is entered, each played once
 */
public record StateDefinition(
Object representation,
Map<String, LinkDefinition> links,
List<EventDefinition> events,
@Nullable AutoTransitionDefinition auto,
List<ChatFailure> chatFailures) {

    /**
     * Compact constructor copying collections and ensuring non-null defaults.
     */
    public StateDefinition {
        Objects.requireNonNull(representation, "representation");
        links = links == null ? Map.of() : Map.copyOf(links);
        events = events == null ? List.of() : List.copyOf(events);
        chatFailures = chatFailures == null ? List.of() : List.copyOf(chatFailures);
    }

    /**
     * A state that arms no chat failure.
     *
     * @param representation the hypermedia state payload
     * @param links          the offered link relations
     * @param events         the timed events
     * @param auto           the optional automatic transition
     */
    public StateDefinition(Object representation, Map<String, LinkDefinition> links, List<EventDefinition> events,
            @Nullable AutoTransitionDefinition auto) {
        this(representation, links, events, auto, List.of());
    }

    /**
     * One failure of a chat the mock plays once, on demand through {@code POST /mock/inject} or armed
     * by a state: the next reply ends {@code failed} with the reason {@code chat.reason.<kind>}
     * ({@link Kind#INFEASIBLE}, {@link Kind#LOST}, {@link Kind#TOO_LONG}), is stopped
     * ({@link Kind#STOP}) or restarted by a replaced worker ({@link Kind#WORKER_REPLACED}) after its
     * first piece, or the next request of a chat answers with an error ({@link Kind#HTTP_ERROR}) or
     * late ({@link Kind#SLOW}).
     *
     * @param kind    what fails
     * @param status  the status of an {@link Kind#HTTP_ERROR}, {@code 0} for every other kind
     * @param code    the error code of an {@link Kind#HTTP_ERROR}, {@code null} for every other kind
     * @param delayMs how late a {@link Kind#SLOW} request answers, {@code 0} for every other kind
     */
    public record ChatFailure(Kind kind, int status, @Nullable String code, long delayMs) {

        /** The wire name of the failure's kind. */
        public static final String FIELD_FAILURE = "failure";
        /** The wire name of the status of an {@code http_error}. */
        public static final String FIELD_STATUS = "status";
        /** The wire name of the error code of an {@code http_error}. */
        public static final String FIELD_CODE = "code";
        /** The wire name of the delay of a {@code slow} request. */
        public static final String FIELD_DELAY_MS = "delay_ms";
        /** How late a {@code slow} request answers when the failure names no delay. */
        public static final long DEFAULT_DELAY_MS = 2_000;

        private static final Set<String> FIELDS = Set.of(FIELD_FAILURE, FIELD_STATUS, FIELD_CODE, FIELD_DELAY_MS);

        /**
         * The closed list of what fails.
         */
        public enum Kind {

            /** The next reply ends failed: the role found the request infeasible. */
            INFEASIBLE("infeasible", true),

            /** The next reply ends failed: its worker was lost. */
            LOST("lost", true),

            /** The next reply ends failed: it exceeded the chat reply bound. */
            TOO_LONG("too_long", true),

            /** The next request of a chat answers with an error status and code. */
            HTTP_ERROR("http_error", false),

            /** The next request of a chat answers late. */
            SLOW("slow", false),

            /** The next reply is stopped after its first piece. */
            STOP("stop", true),

            /** The next reply's worker is replaced after its first piece: the reply restarts. */
            WORKER_REPLACED("worker_replaced", true);

            private final String wireValue;
            private final boolean reply;

            Kind(String wireValue, boolean reply) {
                this.wireValue = wireValue;
                this.reply = reply;
            }

            /**
             * @return the value used on the wire
             */
            public String wireValue() {
                return wireValue;
            }

            /**
             * @return {@code true} if the failure plays on a reply, {@code false} if on a request
             */
            public boolean isReply() {
                return reply;
            }

            /**
             * @return {@code true} if the reply ends {@code failed} with the reason of this kind
             */
            public boolean failsReply() {
                return this == INFEASIBLE || this == LOST || this == TOO_LONG;
            }

            /**
             * @param value the wire value
             * @return the kind, or {@code null} when the value names none
             */
            public static @Nullable Kind fromWireOrNull(@Nullable String value) {
                for (var kind : values()) {
                    if (kind.wireValue.equals(value)) {
                        return kind;
                    }
                }
                return null;
            }

            /**
             * @return the wire values of the closed list, in declaration order
             */
            public static List<String> wireValues() {
                return Arrays.stream(values()).map(Kind::wireValue).toList();
            }
        }

        /**
         * @param kind    the kind
         * @param status  the status, {@code 400}–{@code 599} for an {@code http_error}
         * @param code    the API error code of an {@code http_error}
         * @param delayMs the delay, not negative
         * @throws IllegalArgumentException if a member does not fit the kind
         */
        public ChatFailure {
            Objects.requireNonNull(kind, FIELD_FAILURE);
            if (kind == Kind.HTTP_ERROR && (status < 400 || status > 599 || code == null
                    || ApiErrorCode.fromWireOrNull(code) == null)) {
                throw new IllegalArgumentException("an http_error names a status 400-599 and an API error code");
            }
            if (kind != Kind.HTTP_ERROR && (status != 0 || code != null)) {
                throw new IllegalArgumentException("only an http_error names a status and a code");
            }
            if (delayMs < 0 || (kind != Kind.SLOW && delayMs != 0)) {
                throw new IllegalArgumentException("only a slow request names a delay_ms, and not a negative one");
            }
        }

        /**
         * @param kind a kind that takes no member
         * @return the failure of that kind
         */
        public static ChatFailure of(Kind kind) {
            return new ChatFailure(kind, 0, null, kind == Kind.SLOW ? DEFAULT_DELAY_MS : 0);
        }

        /**
         * @param map the failure as {@code POST /mock/inject} and a scenario state name it
         * @return the failure
         * @throws IllegalArgumentException if a member is unknown or missing, or the kind is no failure
         */
        public static ChatFailure fromMap(Map<String, Object> map) {
            Objects.requireNonNull(map, "map");
            for (var key : map.keySet()) {
                if (!FIELDS.contains(key)) {
                    throw new IllegalArgumentException("unknown member '" + key + "' of a chat failure");
                }
            }
            var name = JsonTree.string(map, FIELD_FAILURE);
            var kind = Kind.fromWireOrNull(name);
            if (kind == null) {
                throw new IllegalArgumentException("unknown chat failure '" + name + "', expected one of "
                        + Kind.wireValues());
            }
            var status = status(number(map.get(FIELD_STATUS), 0));
            var delayMs = number(map.get(FIELD_DELAY_MS), kind == Kind.SLOW ? DEFAULT_DELAY_MS : 0);
            return new ChatFailure(kind, status, JsonTree.string(map, FIELD_CODE), delayMs);
        }

        /**
         * Checks the status before it is narrowed: a number beyond an {@code int} would wrap into the
         * range of an {@code http_error} and be taken for a status it is not.
         */
        private static int status(long status) {
            if (status < Integer.MIN_VALUE || status > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("a chat failure's status " + status + " is no HTTP status");
            }
            return (int) status;
        }

        private static long number(@Nullable Object value, long fallback) {
            if (value instanceof JsonNumber number) {
                return number.longValue();
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                throw new IllegalArgumentException("a chat failure's status and delay_ms are numbers");
            }
            return fallback;
        }
    }
}
