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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Chat read model: records, codecs and closed lists")
@EnableGeneratorController
class ChatReadModelTest {

    private static final String GOLDEN_CHAT = """
            {"id":"chat-7","subject":{"kind":"plan","id":"fix-relay"},
             "role":{"name":"replan","model":"opus"},"scope":"plan","state":"chat.read-back",
             "context":{"knows":[{"key":"chat.knows.phase","args":{"phase":"execute"}}],
                        "as_of":"2026-11-10T18:00:00Z"},
             "proposal":{"turn_id":"t-3","reply_turn_id":"t-2","kind":"revision","summary":"Split TASK-4",
                         "applies":{"key":"chat.applies.after_task","args":{"task_id":"TASK-3"}},
                         "before":"one task","after":"two tasks","state":"open"},
             "turns":[
              {"turn_id":"t-1","kind":"message","author":"operator","status":"complete","format":"plain",
               "untrusted":false,"text":"Split TASK-4","steps":[],"refs":[],"at":"2026-11-10T18:01:00Z"},
              {"turn_id":"t-2","kind":"message","author":"replan","status":"complete","format":"markdown",
               "untrusted":false,"model":"opus","text":"Done.","steps":[{"key":"chat.step.read","args":{"target":"TASK-4"}}],
               "refs":[{"kind":"task","id":"TASK-4"}],"at":"2026-11-10T18:02:00Z"},
              {"turn_id":"t-4","kind":"message","author":"replan","status":"failed","format":"markdown",
               "untrusted":false,"text":"Half","steps":[],"refs":[],"at":"2026-11-10T18:03:00Z",
               "reason":{"key":"chat.reason.lost"}},
              {"turn_id":"t-5","kind":"notice","author":"runtime","status":"complete","format":"plain",
               "untrusted":false,"text":"","steps":[],"refs":[],"at":"2026-11-10T18:04:00Z",
               "lines":[{"key":"chat.line.task_ended","args":{"task_id":"TASK-3","outcome":"done"},
                         "refs":[{"kind":"task","id":"TASK-3"},{"kind":"file","id":"a/B.java:12"},
                                 {"kind":"decision","id":"fix-relay/Q-1"},{"kind":"plan","id":"fix-relay"},
                                 {"kind":"hold","id":"H-1"},
                                 {"kind":"act","act":"resume","subject":{"kind":"plan","id":"fix-relay"}}],
                         "at":"2026-11-10T18:03:30Z"}]}],
             "before":"t-0"}
            """;

    @FunctionalInterface
    private interface Parser {
        Object parse(Map<String, Object> map) throws IOException;
    }

    private static String id() {
        return Generators.nonBlankStrings().next();
    }

    private static String unknownWire() {
        return "zz_" + Generators.letterStrings(3, 10).next();
    }

    private static ChatSubject subject(ChatSubjectKind kind) {
        return new ChatSubject(kind, id());
    }

    private static MessageText message(String key) {
        return new MessageText(key, Map.of("id", id()));
    }

    private static TurnRef ref(RefKind kind) {
        return kind == RefKind.ACT ? TurnRef.ofAct(id(), subject(ChatSubjectKind.PLAN)) : TurnRef.of(kind, id());
    }

    private static List<TurnRef> refsOfEachKind() {
        return Arrays.stream(RefKind.values()).map(ChatReadModelTest::ref).toList();
    }

    private static NarrativeLine line() {
        return new NarrativeLine("chat.line.task_ended", Map.of("task_id", id(), "outcome", "done"), refsOfEachKind(),
                id());
    }

    private static ChatTurn turn(TurnKind kind, TurnStatus status, List<NarrativeLine> lines) {
        var reason = status == TurnStatus.FAILED ? message("chat.reason.too_long") : null;
        var author = id();
        var untrusted = Generators.booleans().next();
        var format = plainOnly(kind, author, untrusted) ? TurnFormat.PLAIN : TurnFormat.MARKDOWN;
        return new ChatTurn(id(), kind, author, status, format, untrusted, id(),
                id(), List.of(message("chat.step.read"), message("chat.step.search")),
                refsOfEachKind(), id(), reason, lines);
    }

    private static boolean plainOnly(TurnKind kind, String author, boolean untrusted) {
        return kind == TurnKind.NOTICE || untrusted || ChatTurn.AUTHOR_OPERATOR.equals(author);
    }

    private static ChatTurn messageTurn() {
        return turn(TurnKind.MESSAGE, TurnStatus.COMPLETE, List.of());
    }

    private static ChatProposal proposal(ProposalState state) {
        var finding = state == ProposalState.REFUSED ? message("chat.finding.lapsed") : null;
        return new ChatProposal(id(), id(), ProposalKind.REVISION, id(), message("chat.applies.on_start"),
                id(), id(), state, finding);
    }

    private static ChatContext context(int size) {
        var knows = new ArrayList<MessageText>();
        for (var i = 0; i < size; i++) {
            knows.add(message("chat.knows.decision"));
        }
        return new ChatContext(knows, id());
    }

    private static ChatResource chat(@Nullable ChatProposal proposal, List<ChatTurn> turns, @Nullable String before) {
        return new ChatResource(id(), subject(ChatSubjectKind.EPIC), new RoutedRole(id(), id()), ChatScopeKind.DRAFT,
                ChatStatus.TURN, context(2), proposal, turns, before);
    }

    private static Map<String, Object> with(Map<String, Object> map, String key, Object value) {
        var copy = new LinkedHashMap<>(map);
        copy.put(key, value);
        return copy;
    }

    private static Map<String, Object> without(Map<String, Object> map, String key) {
        var copy = new LinkedHashMap<>(map);
        copy.remove(key);
        return copy;
    }

    @Test
    @DisplayName("The golden chat of the routes reads and writes back with every member")
    void goldenChat() throws Exception {
        var chat = ChatResource.fromJson(GOLDEN_CHAT);

        assertAll(
                () -> assertEquals(JsonTree.parse(GOLDEN_CHAT), JsonTree.parse(chat.toJson())),
                () -> assertEquals(new ChatSubject(ChatSubjectKind.PLAN, "fix-relay"), chat.subject()),
                () -> assertEquals(ChatStatus.READ_BACK, chat.state()),
                () -> assertEquals("t-0", chat.before()),
                () -> assertTrue(chat.hasProposal()),
                () -> assertTrue(chat.proposal().isOpen()),
                () -> assertEquals(4, chat.turns().size()),
                () -> assertTrue(chat.turns().getFirst().isOperator()),
                () -> assertEquals(MessageText.of("chat.reason.lost"), chat.turns().get(2).reason()),
                () -> assertEquals(6, chat.turns().get(3).lines().getFirst().refs().size()),
                () -> assertEquals(TurnRef.ofAct("resume", new ChatSubject(ChatSubjectKind.PLAN, "fix-relay")),
                        chat.turns().get(3).lines().getFirst().refs().get(5)));
    }

    @Nested
    @DisplayName("Closed lists")
    class ClosedLists {

        static Stream<Arguments> closedLists() {
            return Stream.of(
                    Arguments.of("ChatSubjectKind", List.of("plan", "epic", "entry", "decision"),
                            (Supplier<List<String>>) ChatSubjectKind::wireValues,
                            (Function<String, Object>) ChatSubjectKind::fromWire,
                            (Function<String, Object>) ChatSubjectKind::fromWireOrNull),
                    Arguments.of("ChatScopeKind",
                            List.of("plan", "epic", "draft", "item", "decision", "entry", "archive", "register"),
                            (Supplier<List<String>>) ChatScopeKind::wireValues,
                            (Function<String, Object>) ChatScopeKind::fromWire,
                            (Function<String, Object>) ChatScopeKind::fromWireOrNull),
                    Arguments.of("ChatStatus", List.of("chat.open", "chat.turn", "chat.reply", "chat.route",
                                    "chat.read-back", "chat.apply", "chat.end", "chat.carried"),
                            (Supplier<List<String>>) ChatStatus::wireValues,
                            (Function<String, Object>) ChatStatus::fromWire,
                            (Function<String, Object>) ChatStatus::fromWireOrNull),
                    Arguments.of("TurnKind", List.of("message", "proposal", "notice"),
                            (Supplier<List<String>>) TurnKind::wireValues,
                            (Function<String, Object>) TurnKind::fromWire,
                            (Function<String, Object>) TurnKind::fromWireOrNull),
                    Arguments.of("TurnStatus", List.of("streaming", "complete", "stopped", "failed"),
                            (Supplier<List<String>>) TurnStatus::wireValues,
                            (Function<String, Object>) TurnStatus::fromWire,
                            (Function<String, Object>) TurnStatus::fromWireOrNull),
                    Arguments.of("TurnFormat", List.of("plain", "markdown"),
                            (Supplier<List<String>>) TurnFormat::wireValues,
                            (Function<String, Object>) TurnFormat::fromWire,
                            (Function<String, Object>) TurnFormat::fromWireOrNull),
                    Arguments.of("RefKind", List.of("task", "file", "decision", "plan", "hold", "act"),
                            (Supplier<List<String>>) RefKind::wireValues,
                            (Function<String, Object>) RefKind::fromWire,
                            (Function<String, Object>) RefKind::fromWireOrNull),
                    Arguments.of("ProposalKind",
                            List.of("description", "revision", "rows", "draft", "item", "answer", "follow_up"),
                            (Supplier<List<String>>) ProposalKind::wireValues,
                            (Function<String, Object>) ProposalKind::fromWire,
                            (Function<String, Object>) ProposalKind::fromWireOrNull),
                    Arguments.of("ProposalState",
                            List.of("open", "replaced", "accepted", "rejected", "applied", "refused", "lapsed"),
                            (Supplier<List<String>>) ProposalState::wireValues,
                            (Function<String, Object>) ProposalState::fromWire,
                            (Function<String, Object>) ProposalState::fromWireOrNull));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("closedLists")
        @DisplayName("names exactly its catalogue values, resolves each, and refuses an unknown value")
        void closedList(String name, List<String> expected, Supplier<List<String>> wireValues,
                        Function<String, Object> fromWire, Function<String, Object> fromWireOrNull) {
            var unknown = unknownWire();

            assertEquals(expected, wireValues.get(), name);
            for (var value : expected) {
                assertEquals(fromWire.apply(value), fromWireOrNull.apply(value), value);
            }
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> fromWire.apply(unknown)),
                    () -> assertNull(fromWireOrNull.apply(unknown)),
                    () -> assertNull(fromWireOrNull.apply(null)));
        }

        static Stream<Arguments> recordsWithClosedMembers() {
            var turn = messageTurn();
            var proposal = proposal(ProposalState.OPEN);
            var chat = chat(null, List.of(), null);
            return Stream.of(
                    Arguments.of("ChatSubject.kind", subject(ChatSubjectKind.ENTRY).toMap(), "kind",
                            (Parser) ChatSubject::fromMap),
                    Arguments.of("ChatResource.scope", chat.toMap(), "scope", (Parser) ChatResource::fromMap),
                    Arguments.of("ChatResource.state", chat.toMap(), "state", (Parser) ChatResource::fromMap),
                    Arguments.of("ChatTurn.kind", turn.toMap(), "kind", (Parser) ChatTurn::fromMap),
                    Arguments.of("ChatTurn.status", turn.toMap(), "status", (Parser) ChatTurn::fromMap),
                    Arguments.of("ChatTurn.format", turn.toMap(), "format", (Parser) ChatTurn::fromMap),
                    Arguments.of("TurnRef.kind", ref(RefKind.TASK).toMap(), "kind", (Parser) TurnRef::fromMap),
                    Arguments.of("ChatProposal.kind", proposal.toMap(), "kind", (Parser) ChatProposal::fromMap),
                    Arguments.of("ChatProposal.state", proposal.toMap(), "state", (Parser) ChatProposal::fromMap));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("recordsWithClosedMembers")
        @DisplayName("a record refuses an unknown value of its closed member")
        void recordRefusesUnknownValue(String member, Map<String, Object> valid, String key, Parser parser) {
            var unknown = with(valid, key, unknownWire());

            assertThrows(JsonFormatException.class, () -> parser.parse(unknown), member);
        }
    }

    @Nested
    @DisplayName("Small records")
    class SmallRecords {

        @Test
        @DisplayName("a subject of each kind and a routed role round-trip; a blank id is refused")
        void subjectAndRole() throws Exception {
            for (var kind : ChatSubjectKind.values()) {
                var subject = subject(kind);
                assertEquals(subject, ChatSubject.fromJson(subject.toJson()));
            }
            var role = new RoutedRole(id(), id());
            var roleId = id();

            assertAll(
                    () -> assertEquals(role, RoutedRole.fromJson(role.toJson())),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatSubject(ChatSubjectKind.PLAN, " ")),
                    () -> assertThrows(IllegalArgumentException.class, () -> new RoutedRole(roleId, " ")),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatSubject.fromMap(without(subject(ChatSubjectKind.PLAN).toMap(), "id"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> RoutedRole.fromMap(with(role.toMap(), "extra", id()))));
        }

        @Test
        @DisplayName("a message text round-trips with and without arguments; an invalid key is refused")
        void messageText() {
            var withArgs = new MessageText("chat.knows.decision", Map.of("id", id(), "title", id()));
            var bare = MessageText.of("chat.knows.description");

            assertAll(
                    () -> assertEquals(withArgs, MessageText.fromJson(withArgs.toJson())),
                    () -> assertEquals(bare, MessageText.fromJson(bare.toJson())),
                    () -> assertFalse(bare.toMap().containsKey("args")),
                    () -> assertThrows(IllegalArgumentException.class, () -> MessageText.of("Not A Key")),
                    () -> assertThrows(JsonFormatException.class,
                            () -> MessageText.fromMap(Map.of("key", "nokey"))));
        }

        @Test
        @DisplayName("a context round-trips with none, one, many and a duplicate; absent knows reads as none")
        void contextRoundTrips() throws Exception {
            var duplicate = message("chat.knows.decision");
            var contexts = List.of(context(0), context(1), context(3),
                    new ChatContext(List.of(duplicate, duplicate), id()));
            for (var context : contexts) {
                assertEquals(context, ChatContext.fromJson(context.toJson()));
            }
            var asOf = id();

            assertAll(
                    () -> assertEquals(new ChatContext(List.of(), asOf),
                            ChatContext.fromMap(Map.of("as_of", asOf))),
                    () -> assertEquals(2, ChatContext.fromJson(contexts.get(3).toJson()).knows().size()),
                    () -> assertThrows(JsonFormatException.class, () -> ChatContext.fromMap(Map.of())),
                    () -> assertThrows(IllegalArgumentException.class, () -> new ChatContext(null, " ")));
        }

        @Test
        @DisplayName("a queued revision of a plan or a decision round-trips; any other subject is refused")
        void queuedRevision() throws Exception {
            for (var kind : List.of(ChatSubjectKind.PLAN, ChatSubjectKind.DECISION)) {
                var revision = new QueuedRevision(id(), id(), subject(kind), Generators.booleans().next(),
                        message("chat.applies.after_task"));
                assertEquals(revision, QueuedRevision.fromJson(revision.toJson()));
            }
            var applies = message("chat.applies.on_start");
            var chatId = id();
            var turnId = id();
            var epic = subject(ChatSubjectKind.EPIC);
            var withoutChatOpen = without(
                    new QueuedRevision(id(), id(), subject(ChatSubjectKind.PLAN), false, applies).toMap(), "chat_open");

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new QueuedRevision(chatId, turnId, epic, true, applies)),
                    () -> assertThrows(JsonFormatException.class, () -> QueuedRevision.fromMap(withoutChatOpen)));
        }
    }

    @Nested
    @DisplayName("References and narrative lines")
    class References {

        @Test
        @DisplayName("a reference of each kind round-trips, the act with its act and subject")
        void eachKindRoundTrips() throws Exception {
            for (var ref : refsOfEachKind()) {
                assertEquals(ref, TurnRef.fromJson(ref.toJson()));
            }
            var act = ref(RefKind.ACT);

            assertAll(
                    () -> assertFalse(act.toMap().containsKey("id")),
                    () -> assertFalse(ref(RefKind.FILE).toMap().containsKey("act")));
        }

        @Test
        @DisplayName("the constructor refuses an act without act and subject and any other kind without id")
        void constructorRefusals() {
            var subject = subject(ChatSubjectKind.PLAN);
            var refId = id();
            var act = id();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new TurnRef(RefKind.ACT, null, null, subject)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new TurnRef(RefKind.ACT, null, act, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new TurnRef(RefKind.ACT, refId, act, subject)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new TurnRef(RefKind.TASK, null, null, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new TurnRef(RefKind.HOLD, " ", null, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new TurnRef(RefKind.PLAN, refId, act, null)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> TurnRef.fromMap(Map.of("kind", "act", "act", id()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> TurnRef.fromMap(Map.of("kind", "decision"))));
        }

        @Test
        @DisplayName("a narrative line round-trips with refs of each kind, with none, and with a duplicate")
        void narrativeLine() throws Exception {
            var duplicate = ref(RefKind.TASK);
            var lines = List.of(line(),
                    new NarrativeLine("chat.line.row_started", null, List.of(), id()),
                    new NarrativeLine("chat.line.row_started", Map.of(), List.of(duplicate, duplicate), id()));
            for (var line : lines) {
                assertEquals(line, NarrativeLine.fromJson(line.toJson()));
            }
            var at = id();

            assertAll(
                    () -> assertEquals(new NarrativeLine("chat.line.row_started", null, null, at),
                            NarrativeLine.fromMap(Map.of("key", "chat.line.row_started", "at", at))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new NarrativeLine("bad", null, null, at)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> NarrativeLine.fromMap(without(line().toMap(), "at"))));
        }
    }

    @Nested
    @DisplayName("Turns")
    class Turns {

        @Test
        @DisplayName("a turn of each kind and status round-trips with every member")
        void eachKindAndStatus() throws Exception {
            for (var kind : TurnKind.values()) {
                for (var status : TurnStatus.values()) {
                    var lines = kind == TurnKind.NOTICE ? List.of(line(), line()) : List.<NarrativeLine>of();
                    var turn = turn(kind, status, lines);
                    assertEquals(turn, ChatTurn.fromJson(turn.toJson()), kind + "/" + status);
                }
            }
        }

        @Test
        @DisplayName("absent optional members read as absent or empty; empty text is kept")
        void absentMembers() throws Exception {
            var turn = new ChatTurn(id(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnStatus.STREAMING,
                    TurnFormat.PLAIN, false, null, null, null, null, id(), null, null);
            var map = turn.toMap();
            var read = ChatTurn.fromJson(turn.toJson());

            assertAll(
                    () -> assertEquals(turn, read),
                    () -> assertTrue(read.isOperator()),
                    () -> assertEquals("", read.text()),
                    () -> assertFalse(map.containsKey("model")),
                    () -> assertFalse(map.containsKey("reason")),
                    () -> assertFalse(map.containsKey("lines")),
                    () -> assertEquals(turn, ChatTurn.fromMap(without(without(map, "steps"), "refs"))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurn.fromMap(without(map, "text"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurn.fromMap(without(map, "untrusted"))));
        }

        @Test
        @DisplayName("a reason is required exactly for a failed turn, and only a notice carries lines")
        void kindAndStatusRefusals() {
            var failed = turn(TurnKind.MESSAGE, TurnStatus.FAILED, List.of()).toMap();
            var complete = messageTurn().toMap();
            var noticeLines = List.of(line());

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurn.fromMap(without(failed, "reason"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurn.fromMap(with(complete, "reason", MessageText.of("chat.reason.lost").toMap()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurn.fromMap(with(complete, "lines", List.of(line().toMap())))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> turn(TurnKind.PROPOSAL, TurnStatus.COMPLETE, noticeLines)));
        }

        static Stream<Arguments> alwaysPlainTurns() {
            return Stream.of(
                    Arguments.of("the operator's turn", TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, false),
                    Arguments.of("an untrusted turn", TurnKind.MESSAGE, id(), true),
                    Arguments.of("a notice", TurnKind.NOTICE, id(), false));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("alwaysPlainTurns")
        @DisplayName("an operator's turn, an untrusted turn and a notice are plain; markdown is refused")
        void alwaysPlain(String name, TurnKind kind, String author, boolean untrusted) {
            var plain = new ChatTurn(id(), kind, author, TurnStatus.COMPLETE, TurnFormat.PLAIN, untrusted, null,
                    id(), null, null, id(), null, null);
            var markdown = with(plain.toMap(), "format", "markdown");
            var turnId = id();
            var text = id();
            var at = id();

            assertAll(
                    () -> assertEquals(plain, ChatTurn.fromJson(plain.toJson()), name),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurn(turnId, kind, author, TurnStatus.COMPLETE, TurnFormat.MARKDOWN,
                                    untrusted, null, text, null, null, at, null, null), name),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurn.fromMap(markdown), name),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurn.fromJson(JsonTree.write(markdown)), name));
        }

        @Test
        @DisplayName("a trusted role's message may be markdown")
        void roleMessageMayBeMarkdown() throws Exception {
            var turn = new ChatTurn(id(), TurnKind.MESSAGE, id(), TurnStatus.COMPLETE, TurnFormat.MARKDOWN, false,
                    id(), id(), null, null, id(), null, null);

            assertEquals(turn, ChatTurn.fromJson(turn.toJson()));
        }

        @Test
        @DisplayName("an operator's turn with a model or steps is refused, as chat.turn.started refuses it")
        void operatorTurnCarriesNoModelOrSteps() {
            var operator = new ChatTurn(id(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnStatus.COMPLETE,
                    TurnFormat.PLAIN, false, null, id(), null, null, id(), null, null);
            var map = operator.toMap();
            var turnId = id();
            var model = id();
            var text = id();
            var at = id();
            var steps = List.of(message("chat.step.read"));

            assertAll(
                    () -> assertEquals(operator, ChatTurn.fromJson(operator.toJson())),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurn(turnId, TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnStatus.COMPLETE,
                                    TurnFormat.PLAIN, false, model, text, null, null, at, null, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurn(turnId, TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnStatus.COMPLETE,
                                    TurnFormat.PLAIN, false, null, text, steps, null, at, null, null)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurn.fromMap(with(map, "model", model))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurn.fromMap(with(map, "steps", List.of(steps.getFirst().toMap())))));
        }
    }

    @Nested
    @DisplayName("Proposals")
    class Proposals {

        @Test
        @DisplayName("a proposal in each state round-trips, a refused one with its finding")
        void eachState() throws Exception {
            for (var state : ProposalState.values()) {
                var proposal = proposal(state);
                assertEquals(proposal, ChatProposal.fromJson(proposal.toJson()), state.wireValue());
                assertEquals(state == ProposalState.OPEN, proposal.isOpen());
            }
        }

        @Test
        @DisplayName("absent applies, before and after read as absent; a finding is tied to refused")
        void absentAndRefusals() {
            var bare = new ChatProposal(id(), id(), ProposalKind.FOLLOW_UP, id(), null, null, null,
                    ProposalState.REJECTED, null);
            var refused = proposal(ProposalState.REFUSED).toMap();

            assertAll(
                    () -> assertEquals(bare, ChatProposal.fromJson(bare.toJson())),
                    () -> assertEquals(List.of("turn_id", "reply_turn_id", "kind", "summary", "state"),
                            List.copyOf(bare.toMap().keySet())),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatProposal.fromMap(without(refused, "finding"))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatProposal.fromMap(
                            with(bare.toMap(), "finding", MessageText.of("chat.finding.lapsed").toMap()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatProposal.fromMap(without(bare.toMap(), "summary"))));
        }
    }

    @Nested
    @DisplayName("Chats")
    class Chats {

        @Test
        @DisplayName("a chat round-trips with no turn, one, many and a duplicate, with and without proposal")
        void turnPages() throws Exception {
            var duplicate = messageTurn();
            var chats = List.of(chat(null, List.of(), null),
                    chat(proposal(ProposalState.OPEN), List.of(messageTurn()), id()),
                    chat(proposal(ProposalState.REPLACED), List.of(messageTurn(),
                            turn(TurnKind.NOTICE, TurnStatus.COMPLETE, List.of(line())),
                            turn(TurnKind.MESSAGE, TurnStatus.FAILED, List.of())), id()),
                    chat(null, List.of(duplicate, duplicate), null));
            for (var chat : chats) {
                assertEquals(chat, ChatResource.fromJson(chat.toJson()));
            }

            assertAll(
                    () -> assertFalse(chats.getFirst().hasProposal()),
                    () -> assertFalse(chats.getFirst().toMap().containsKey("proposal")),
                    () -> assertFalse(chats.getFirst().toMap().containsKey("before")),
                    () -> assertEquals(2, ChatResource.fromJson(chats.get(3).toJson()).turns().size()));
        }

        @Test
        @DisplayName("only an open proposal awaits accept or reject; a replaced one does not")
        void hasProposalOnlyWhenOpen() throws Exception {
            var replaced = chat(proposal(ProposalState.REPLACED), List.of(messageTurn()), null);

            assertFalse(ChatResource.fromJson(replaced.toJson()).hasProposal());
            for (var state : ProposalState.values()) {
                assertEquals(state == ProposalState.OPEN, chat(proposal(state), List.of(), null).hasProposal(),
                        state.wireValue());
            }
        }

        @Test
        @DisplayName("a missing required member, an unknown member and a blank before are refused")
        void refusals() {
            var map = chat(null, List.of(), null).toMap();

            assertAll(
                    () -> assertEquals(List.of(), ChatResource.fromMap(without(map, "turns")).turns()),
                    () -> assertThrows(JsonFormatException.class, () -> ChatResource.fromMap(without(map, "role"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatResource.fromMap(without(map, "context"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatResource.fromMap(with(map, "extra", id()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatResource.fromMap(with(map, "before", " "))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatResource.fromJson("[]")));
        }
    }
}
