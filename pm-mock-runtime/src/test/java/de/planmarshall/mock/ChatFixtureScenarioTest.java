/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.NarrativeLine;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.mock.scenario.ScenarioLoader;

/**
 * Verifies the chat fixtures seeded in the two rich showroom scenarios ({@code running-work} and
 * {@code fully-onboarded}): a running plan with a running task and narrative lines, a plan decision
 * chat carrying an open revision proposal, an ended plan archive chat, and knows plus narrative
 * lines for an epic and an entry.
 */
@DisplayName("Chat fixtures in running-work and fully-onboarded")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ChatFixtureScenarioTest {

    private static final String AUTH_HEADER = "Authorization";

    static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of(StandardScenarios.RUNNING_WORK, "ent-running"),
                Arguments.of(StandardScenarios.FULLY_ONBOARDED, "ent-showroom"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    @DisplayName("loads cleanly and serves running plan, decision, ended plan, epic, and entry chats")
    void scenarioServesAllChatFixtures(String scenarioName, String entryId) throws Exception {
        var scenario = ScenarioLoader.load(scenarioName);
        assertNotNull(scenario, scenarioName + " loads via ScenarioLoader");

        try (var runtime = MockRuntime.start(scenarioName)) {
            var runningPlanTree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/storage-v2/process"));
            var runningTask = runningPlanTree.nodes().stream()
                    .flatMap(node -> node.subNodes().stream())
                    .flatMap(sub -> sub.leafTasks().stream())
                    .anyMatch(task -> "running".equals(task.status()));
            var runningPlanChat = readChat(runtime, ChatRoutes.plan("storage-v2"));

            var decisionChat = readChat(runtime, ChatRoutes.planDecision("fix-relay", "Q-007"));

            var endedActions = WorkActions.fromJson(get(runtime, "/api/v1/plans/docs-typo/actions"));
            var endedChat = readChat(runtime, ChatRoutes.endedPlan("docs-typo"));

            var epicChat = readChat(runtime, ChatRoutes.epic("relay-hardening"));
            var entryChat = readChat(runtime, ChatRoutes.entry(entryId));

            assertAll(
                    () -> assertTrue(runningTask, "running plan has a running leaf task"),
                    () -> assertChatHasNarrativeLines(runningPlanChat,
                            List.of("chat.line.phase_changed", "chat.line.task_ended", "chat.line.pr_merged")),
                    () -> assertEquals(List.of("chat.knows.description", "chat.knows.tasks",
                                    "chat.knows.phase", "chat.knows.version"),
                            knowsKeys(runningPlanChat)),

                    () -> assertEquals(ChatSubjectKind.DECISION, decisionChat.subject().kind()),
                    () -> assertEquals("fix-relay/Q-007", decisionChat.subject().id()),
                    () -> assertEquals(ChatScopeKind.DECISION, decisionChat.scope()),
                    () -> assertEquals(ChatStatus.READ_BACK, decisionChat.state()),
                    () -> assertTrue(decisionChat.hasProposal(), "decision chat carries an open proposal"),
                    () -> assertEquals(ProposalKind.REVISION,
                            Objects.requireNonNull(decisionChat.proposal()).kind()),
                    () -> assertTrue(Objects.requireNonNull(decisionChat.proposal()).isOpen()),
                    () -> assertTrue(knowsKeys(decisionChat).contains("chat.knows.decision"),
                            "decision chat knows its decision"),
                    () -> assertTrue(knowsKeys(decisionChat).contains("chat.knows.version"),
                            "decision chat knows its plan version"),

                    () -> assertEquals("ended", endedActions.state()),
                    () -> assertEquals(ChatScopeKind.ARCHIVE, endedChat.scope()),
                    () -> assertEquals(List.of("chat.knows.other"), knowsKeys(endedChat)),

                    () -> assertEquals(ChatScopeKind.EPIC, epicChat.scope()),
                    () -> assertEquals(List.of("chat.knows.description", "chat.knows.rows", "chat.knows.items"),
                            knowsKeys(epicChat)),
                    () -> assertChatHasNarrativeLines(epicChat,
                            List.of("chat.line.row_started", "chat.line.auto_changed")),

                    () -> assertEquals(ChatScopeKind.ENTRY, entryChat.scope()),
                    () -> assertEquals(List.of("chat.knows.description", "chat.knows.sources"),
                            knowsKeys(entryChat)),
                    () -> assertChatHasNarrativeLines(entryChat, List.of("chat.line.source_added")));
        }
    }

    private static List<String> knowsKeys(ChatResource chat) {
        return Objects.requireNonNull(chat.context()).knows().stream().map(MessageText::key).toList();
    }

    private static void assertChatHasNarrativeLines(ChatResource chat, List<String> expectedKeys) {
        var lineKeys = chat.turns().stream()
                .filter(turn -> turn.kind() == TurnKind.NOTICE)
                .flatMap(turn -> turn.lines().stream())
                .map(NarrativeLine::key)
                .toList();
        assertFalse(lineKeys.isEmpty(), chat.id() + " has a notice turn with narrative lines");
        assertTrue(lineKeys.containsAll(expectedKeys),
                chat.id() + " narrative lines " + lineKeys + " should contain " + expectedKeys);
    }

    private static ChatResource readChat(MockRuntime runtime, ChatRoutes routes) throws Exception {
        return ChatResource.fromJson(get(runtime, routes.chat()));
    }

    private static String get(MockRuntime runtime, String path) throws Exception {
        var client = new UnixSocketHttpClient(runtime.socketPath());
        var response = client.send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertEquals(200, response.status(), path + " answered " + response.status() + ": " + response.bodyText());
        return response.bodyText();
    }
}
