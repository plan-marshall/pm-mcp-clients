/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("Chat routes: one builder per subject and every path below it")
@EnableGeneratorController
class ChatRoutesTest {

    /**
     * A subject's builder over a first and a second id; a builder that takes one id ignores the second.
     */
    private record Subject(String name, BiFunction<String, String, ChatRoutes> build, String template) {

        String expected(String first, String second) {
            return template.replace("{a}", first).replace("{b}", second);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Arguments> subjects() {
        return Stream.of(
                new Subject("a plan", (a, b) -> ChatRoutes.plan(a), "/api/v1/plans/{a}"),
                new Subject("an epic", (a, b) -> ChatRoutes.epic(a), "/api/v1/epics/{a}"),
                new Subject("an entry", (a, b) -> ChatRoutes.entry(a), "/api/v1/entries/{a}"),
                new Subject("an ended plan", (a, b) -> ChatRoutes.endedPlan(a), "/api/v1/plans/{a}"),
                new Subject("an ended epic", (a, b) -> ChatRoutes.endedEpic(a), "/api/v1/epics/{a}"),
                new Subject("a plan's decision", ChatRoutes::planDecision, "/api/v1/plans/{a}/decisions/{b}"),
                new Subject("an epic's decision", ChatRoutes::epicDecision, "/api/v1/epics/{a}/decisions/{b}"),
                new Subject("an entry's decision", ChatRoutes::entryDecision,
                        "/api/v1/entries/{a}/decisions/{b}"),
                new Subject("a workspace decision", ChatRoutes::workspaceDecision,
                        "/api/v1/projects/{a}/workspace/decisions/{b}"),
                new Subject("a machine decision", (a, b) -> ChatRoutes.machineDecision(b),
                        "/api/v1/machine/decisions/{b}"))
                .map(Arguments::of);
    }

    private static String plainId() {
        return Generators.letterStrings(2, 16).next();
    }

    private static String blank() {
        return Generators.fixedValues("", " ", "\t", " \n ").next();
    }

    @ParameterizedTest(name = "the paths of {0}")
    @MethodSource("subjects")
    @DisplayName("every subject builds its chat, turns, stop, proposal answers, end and page")
    void everyPathOfEverySubject(Subject subject) {
        var first = plainId();
        var second = plainId();
        var turnId = plainId();
        var before = plainId();
        var base = subject.expected(first, second);

        var routes = subject.build().apply(first, second);

        assertAll(subject.name(),
                () -> assertEquals(base, routes.subject()),
                () -> assertEquals(base + "/chat", routes.chat()),
                () -> assertEquals(base + "/chat/turns", routes.turns()),
                () -> assertEquals(base + "/chat/turns/" + turnId + "/stop", routes.stop(turnId)),
                () -> assertEquals(base + "/chat/proposal/accept", routes.accept()),
                () -> assertEquals(base + "/chat/proposal/reject", routes.reject()),
                () -> assertEquals(base + "/chat/end", routes.end()),
                () -> assertEquals(base + "/chat?before=" + before, routes.page(before)));
    }

    @ParameterizedTest(name = "a blank id of {0} is refused")
    @MethodSource("subjects")
    @DisplayName("a blank id is refused by every builder")
    void blankIdIsRefused(Subject subject) {
        var blank = blank();
        var plain = plainId();
        var firstBlank = subject.template().contains("{a}");

        assertThrows(IllegalArgumentException.class, () -> subject.build()
                .apply(firstBlank ? blank : plain, firstBlank ? plain : blank));
    }

    @ParameterizedTest(name = "a dot-segment id of {0} is refused")
    @MethodSource("subjects")
    @DisplayName("an id of . or .. is refused by every builder: it would not address one path segment")
    void dotSegmentIdIsRefused(Subject subject) {
        var dots = Generators.fixedValues(".", "..").next();
        var plain = plainId();
        var firstDots = subject.template().contains("{a}");

        assertThrows(IllegalArgumentException.class, () -> subject.build()
                .apply(firstDots ? dots : plain, firstDots ? plain : dots));
    }

    @ParameterizedTest(name = "a missing id of {0} is refused")
    @MethodSource("subjects")
    @DisplayName("a missing id is refused by every builder")
    void missingIdIsRefused(Subject subject) {
        var plain = plainId();
        var firstMissing = subject.template().contains("{a}");

        assertThrows(NullPointerException.class, () -> subject.build()
                .apply(firstMissing ? null : plain, firstMissing ? plain : null));
    }

    @Nested
    @DisplayName("Literal paths and encoding")
    class Literal {

        @Test
        @DisplayName("the prefix of every path is /api/v1")
        void prefix() {
            assertEquals("/api/v1", ChatRoutes.API_PREFIX);
        }

        @Test
        @DisplayName("a plan's chat and the decision of a project's workspace read as the routes name them")
        void literalPaths() {
            var plan = plainId();
            var project = plainId();
            var question = plainId();

            assertAll(
                    () -> assertEquals("/api/v1/plans/" + plan + "/chat", ChatRoutes.plan(plan).chat()),
                    () -> assertEquals("/api/v1/projects/" + project + "/workspace/decisions/" + question
                            + "/chat/turns", ChatRoutes.workspaceDecision(project, question).turns()),
                    () -> assertEquals("/api/v1/machine/decisions/" + question + "/chat/end",
                            ChatRoutes.machineDecision(question).end()));
        }

        @Test
        @DisplayName("an id is encoded as one path segment")
        void encodesSegments() {
            var head = plainId();
            var middle = plainId();
            var tail = plainId();

            var routes = ChatRoutes.planDecision(head + " " + middle, middle + "/" + tail);

            assertAll(
                    () -> assertEquals("/api/v1/plans/" + head + "%20" + middle + "/decisions/" + middle + "%2F"
                            + tail, routes.subject()),
                    () -> assertEquals(routes.chat() + "/turns/" + head + "%3F" + tail + "/stop",
                            routes.stop(head + "?" + tail)));
        }

        @Test
        @DisplayName("the before of a page read is encoded as a query value")
        void encodesBefore() {
            var head = plainId();
            var middle = plainId();
            var tail = plainId();
            var routes = ChatRoutes.epic(plainId());

            var page = routes.page(head + " " + middle + "&" + tail);

            assertEquals(routes.chat() + "?before=" + head + "%20" + middle + "%26" + tail, page);
        }

        @Test
        @DisplayName("a blank turn id of a stop and a blank before of a page are refused")
        void refusesBlankTurnIds() {
            var routes = ChatRoutes.entry(plainId());
            var blank = blank();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> routes.stop(blank)),
                    () -> assertThrows(IllegalArgumentException.class, () -> routes.page(blank)));
        }

        @Test
        @DisplayName("a turn id or before of . or .. is refused")
        void refusesDotSegmentTurnIds() {
            var routes = ChatRoutes.entry(plainId());
            var dots = Generators.fixedValues(".", "..").next();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> routes.stop(dots)),
                    () -> assertThrows(IllegalArgumentException.class, () -> routes.page(dots)));
        }
    }
}
