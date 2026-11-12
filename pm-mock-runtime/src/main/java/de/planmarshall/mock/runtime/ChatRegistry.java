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

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatContext;
import de.planmarshall.api.chat.ChatEndedEvent;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.RoutedRole;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.event.EventScope;
import de.planmarshall.api.event.EventScopeKind;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;

/**
 * Every chat of the mock runtime on the contract of {@code 01-processes-and-transport.adoc#chat-routes}:
 * the chat of a plan, an epic, an entry, a decision of each scope (a plan, an epic, an entry, a project's
 * workspace, the machine) and of ended work, served on the paths {@link ChatRoutes} builds.
 * <p>
 * {@code GET …/chat} reads the chat with its newest page of turns ({@code ?before=<turn_id>} the page
 * before a turn), {@code POST …/chat} opens it, {@code POST …/chat/turns} records the operator's message
 * and answers {@code 202} before the role answers, {@code …/turns/{turn_id}/stop} stops the reply being
 * written, {@code …/proposal/accept} and {@code …/proposal/reject} answer the open proposal, and
 * {@code …/end} ends the chat. A subject has one chat at a time; a chat opened after the last one ended
 * is a new chat.
 * <p>
 * The role's reply completes at once: the operator's message is announced by {@code chat.turn.started}
 * alone, the reply by {@code chat.turn.started} and {@code chat.turn.completed}, and the end by
 * {@code chat.ended}, each on the stream of the chat's subject. Every message key the registry writes is
 * one {@link de.planmarshall.api.chat.ChatMessageKeys} maps.
 * <p>
 * The state is seeded from the active state ({@link #seed(Seed)}) and lives in memory; a scenario
 * switch or a reset re-seeds it.
 */
final class ChatRegistry {

    /** The most turns a page of the transcript holds. */
    static final int PAGE_SIZE = 50;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String QUERY_BEFORE = "before";

    private static final String SEGMENT_PLANS = "plans";
    private static final String SEGMENT_EPICS = "epics";
    private static final String SEGMENT_ENTRIES = "entries";
    private static final String SEGMENT_PROJECTS = "projects";
    private static final String SEGMENT_WORKSPACE = "workspace";
    private static final String SEGMENT_MACHINE = "machine";
    private static final String SEGMENT_DECISIONS = "decisions";
    private static final String SEGMENT_CHAT = "chat";
    private static final String SEGMENT_TURNS = "turns";
    private static final String SEGMENT_STOP = "stop";
    private static final String SEGMENT_PROPOSAL = "proposal";
    private static final String SEGMENT_ACCEPT = "accept";
    private static final String SEGMENT_REJECT = "reject";
    private static final String SEGMENT_END = "end";

    private static final String ROLE_REPLAN = "replan";
    private static final String ROLE_DECOMPOSE = "decompose";
    private static final String ROLE_REFINE = "refine";
    private static final String DEFAULT_MODEL = "sonnet";
    private static final String MACHINE_SCOPE_ID = "machine";
    private static final String KNOWS_PREFIX = "chat.knows.";
    private static final String KNOWS_ARCHIVE = "archive";
    private static final String STEP_READ = "chat.step.read";
    private static final String CHAT_ID_PREFIX = "chat-";
    private static final String TURN_ID_PREFIX = "turn-";
    private static final String MESSAGE_NO_CHAT = "No chat is open about ";

    /** The callback a chat uses to tell its clients of an event. */
    private final Consumer<ChatEvent> broadcaster;
    /** Whether an entry opens an epic: its chat is routed to {@code decompose}, a plan's to {@code refine}. */
    private final Predicate<String> epicEntry;

    private final Map<ChatSubject, Chat> chats = new LinkedHashMap<>();
    private final Set<ChatSubject> endedWork = new HashSet<>();
    private final Set<String> usedChatIds = new HashSet<>();
    private final Set<String> usedTurnIds = new HashSet<>();
    private long chatCounter;
    private long turnCounter;

    /**
     * @param broadcaster the callback that broadcasts a chat event on its subject's stream
     * @param epicEntry   whether the entry of an id opens an epic
     */
    ChatRegistry(Consumer<ChatEvent> broadcaster, Predicate<String> epicEntry) {
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
        this.epicEntry = Objects.requireNonNull(epicEntry, "epicEntry");
    }

    /**
     * One event a chat broadcasts: the event's name, the stream of the chat's subject it belongs on, and
     * its payload as the event's {@code pm-api} record writes it.
     *
     * @param name  the event name
     * @param scope the scope of the chat's subject
     * @param data  the payload
     */
    record ChatEvent(String name, EventScope scope, Map<String, Object> data) {
    }

    /**
     * What the registry is seeded with from the active state.
     *
     * @param chats     the chats of the state, one per subject, as {@code GET …/chat} reads them with their
     *                  whole transcript
     * @param endedWork the ended plans and epics, whose chats are about the archived record
     */
    record Seed(List<ChatResource> chats, Set<ChatSubject> endedWork) {

        /**
         * @param chats     the chats, {@code null} read as none
         * @param endedWork the ended work, {@code null} read as none
         */
        Seed {
            chats = chats == null ? List.of() : List.copyOf(chats);
            endedWork = endedWork == null ? Set.of() : Set.copyOf(endedWork);
        }
    }

    /**
     * Replaces the chats with those of the active state.
     *
     * @param seed the seed of the active state
     */
    synchronized void seed(Seed seed) {
        Objects.requireNonNull(seed, "seed");
        chats.clear();
        endedWork.clear();
        usedChatIds.clear();
        usedTurnIds.clear();
        chatCounter = 0;
        turnCounter = 0;
        endedWork.addAll(seed.endedWork());
        for (var resource : seed.chats()) {
            var chat = new Chat(resource.id(), resource.subject(), resource.role(), resource.scope(),
                    resource.state(), resource.context(), resource.proposal(), resource.turns());
            usedChatIds.add(chat.id);
            chat.turns.forEach(turn -> usedTurnIds.add(turn.turnId()));
            if (resource.proposal() != null) {
                usedTurnIds.add(resource.proposal().turnId());
            }
            chats.put(chat.subject, chat);
        }
    }

    /**
     * Serves a request when its path is one of a chat's routes.
     *
     * @param request  the request
     * @param response the response
     * @return {@code true} if the request was a chat's and has been answered
     * @throws IOException if the answer cannot be written
     */
    boolean handle(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var target = target(request.path());
        if (target == null) {
            return false;
        }
        var outcome = switch (target.action()) {
            case CHAT -> switch (request.method()) {
                case METHOD_GET -> read(target.address(), queryValue(request, QUERY_BEFORE));
                case METHOD_POST -> open(target.address());
                default -> null;
            };
            case TURNS -> METHOD_POST.equals(request.method()) ? turn(target.address(), request) : null;
            case STOP -> METHOD_POST.equals(request.method())
                    ? stop(target.address(), Objects.requireNonNull(target.turnId()))
                    : null;
            case ACCEPT -> METHOD_POST.equals(request.method()) ? answer(target.address(), request, true) : null;
            case REJECT -> METHOD_POST.equals(request.method()) ? answer(target.address(), request, false) : null;
            case END -> METHOD_POST.equals(request.method()) ? end(target.address()) : null;
        };
        if (outcome == null) {
            return false;
        }
        outcome.events().forEach(broadcaster);
        response.json(outcome.status(), outcome.body());
        return true;
    }

    private synchronized Outcome read(Address address, @Nullable String beforeTurnId) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        if (beforeTurnId == null) {
            return Outcome.of(200, chat.toResource(chat.turns.size()).toMap());
        }
        var index = chat.indexOf(beforeTurnId);
        if (index < 0) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, "No turn " + beforeTurnId + " in the chat");
        }
        return Outcome.of(200, chat.toResource(index).toMap());
    }

    private synchronized Outcome open(Address address) {
        var existing = chats.get(address.subject());
        if (existing != null && !existing.state.isEnded()) {
            return Outcome.of(200, existing.toResource(existing.turns.size()).toMap());
        }
        var chat = new Chat(nextChatId(), address.subject(), new RoutedRole(address.role(), DEFAULT_MODEL),
                address.scope(), ChatStatus.TURN, context(address), null, List.of());
        chats.put(chat.subject, chat);
        return Outcome.of(201, chat.toResource(chat.turns.size()).toMap());
    }

    private Outcome turn(Address address, MockHttpRequest request) {
        ChatTurnRequest message;
        try {
            message = ChatTurnRequest.fromMap(readBody(request));
        } catch (IOException e) {
            return refused(ApiErrorCode.INVALID_ARGUMENTS, e.getMessage());
        }
        return record(address, message);
    }

    private synchronized Outcome record(Address address, ChatTurnRequest message) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        var repeated = chat.clientTurns.get(message.clientTurnId());
        if (repeated != null) {
            return Outcome.of(ChatTurnAccepted.HTTP_STATUS, new ChatTurnAccepted(repeated).toMap());
        }
        if (chat.state.isEnded()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The chat has ended");
        }
        if (chat.state == ChatStatus.REPLY) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "A reply is being written");
        }
        var events = new ArrayList<ChatEvent>();
        var at = Instant.now().toString();
        var operatorTurn = new ChatTurn(nextTurnId(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                TurnStatus.COMPLETE, TurnFormat.PLAIN, false, null, message.text(), List.of(), List.of(), at, null,
                List.of());
        chat.turns.add(operatorTurn);
        chat.clientTurns.put(message.clientTurnId(), operatorTurn.turnId());
        events.add(event(address, ChatTurnStartedEvent.EVENT_NAME, new ChatTurnStartedEvent(chat.id,
                operatorTurn.turnId(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnFormat.PLAIN, false, null,
                null, message.text()).toMap()));

        chat.context = new ChatContext(chat.context.knows(), at);
        var reply = new ChatTurn(nextTurnId(), TurnKind.MESSAGE, chat.role.name(), TurnStatus.COMPLETE,
                TurnFormat.MARKDOWN, false, chat.role.model(), replyText(chat.subject.kind()),
                List.of(MessageText.of(STEP_READ)), List.of(), at, null, List.of());
        chat.turns.add(reply);
        events.add(event(address, ChatTurnStartedEvent.EVENT_NAME, new ChatTurnStartedEvent(chat.id,
                reply.turnId(), TurnKind.MESSAGE, reply.author(), reply.format(), false, reply.model(),
                chat.context, null).toMap()));
        events.add(event(address, ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(chat.id,
                reply.turnId(), TurnKind.MESSAGE, TurnStatus.COMPLETE, false, reply.text(), reply.refs(), null,
                List.of()).toMap()));
        chat.state = chat.proposal != null && chat.proposal.isOpen() ? ChatStatus.READ_BACK : ChatStatus.TURN;
        return new Outcome(ChatTurnAccepted.HTTP_STATUS, new ChatTurnAccepted(operatorTurn.turnId()).toMap(),
                events);
    }

    private synchronized Outcome stop(Address address, String turnId) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        var index = chat.indexOf(turnId);
        if (index < 0) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, "No turn " + turnId + " in the chat");
        }
        var turn = chat.turns.get(index);
        if (turn.status() != TurnStatus.STREAMING) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The turn " + turnId + " is not being written");
        }
        var stopped = new ChatTurn(turn.turnId(), turn.kind(), turn.author(), TurnStatus.STOPPED, turn.format(),
                turn.untrusted(), turn.model(), turn.text(), turn.steps(), turn.refs(), turn.at(), null,
                turn.lines());
        chat.turns.set(index, stopped);
        chat.state = chat.proposal != null && chat.proposal.isOpen() ? ChatStatus.READ_BACK : ChatStatus.TURN;
        var event = event(address, ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(chat.id,
                stopped.turnId(), stopped.kind(), TurnStatus.STOPPED, stopped.untrusted(), stopped.text(),
                stopped.refs(), null, List.of()).toMap());
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(event));
    }

    private Outcome answer(Address address, MockHttpRequest request, boolean accept) {
        ProposalAnswerRequest answer;
        try {
            answer = ProposalAnswerRequest.fromMap(readBody(request));
        } catch (IOException e) {
            return refused(ApiErrorCode.INVALID_ARGUMENTS, e.getMessage());
        }
        return answer(address, answer, accept);
    }

    private synchronized Outcome answer(Address address, ProposalAnswerRequest answer, boolean accept) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        var proposal = chat.proposal;
        if (chat.state.isEnded() || proposal == null || !proposal.isOpen()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "No proposal is open in the chat");
        }
        if (!proposal.turnId().equals(answer.turnId())) {
            return refused(ApiErrorCode.CONFLICT, "The proposal " + answer.turnId() + " is not the open one");
        }
        var state = accept ? acceptedState(proposal.kind()) : ProposalState.REJECTED;
        chat.proposal = new ChatProposal(proposal.turnId(), proposal.replyTurnId(), proposal.kind(),
                proposal.summary(), proposal.applies(), proposal.before(), proposal.after(), state, null);
        chat.state = ChatStatus.TURN;
        return Outcome.of(200, chat.toResource(chat.turns.size()).toMap());
    }

    private synchronized Outcome end(Address address) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        if (chat.state.isEnded()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The chat has ended");
        }
        chat.state = ChatStatus.END;
        var event = event(address, ChatEndedEvent.EVENT_NAME,
                new ChatEndedEvent(chat.id, ChatEndedEvent.EndedBy.OPERATOR, null, null).toMap());
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(event));
    }

    /**
     * A revision of a plan is accepted and queued for the plan's next task boundary; every other kind
     * applies at once.
     */
    private static ProposalState acceptedState(ProposalKind kind) {
        return kind == ProposalKind.REVISION ? ProposalState.ACCEPTED : ProposalState.APPLIED;
    }

    private ChatContext context(Address address) {
        var item = address.scope() == ChatScopeKind.ARCHIVE ? KNOWS_ARCHIVE : address.subject().kind().wireValue();
        return new ChatContext(List.of(MessageText.of(KNOWS_PREFIX + item)), Instant.now().toString());
    }

    private static String replyText(ChatSubjectKind kind) {
        return "I have read the " + kind.wireValue() + "; say what should change, or ask on.";
    }

    private static ChatEvent event(Address address, String name, Map<String, Object> data) {
        return new ChatEvent(name, address.stream(), data);
    }

    private static Outcome refused(ApiErrorCode code, @Nullable String message) {
        return Outcome.of(code.status(), Map.of(KEY_ERROR, code.wire(), KEY_MESSAGE,
                message != null ? message : code.wire()));
    }

    private String nextChatId() {
        String id;
        do {
            id = CHAT_ID_PREFIX + ++chatCounter;
        } while (!usedChatIds.add(id));
        return id;
    }

    private String nextTurnId() {
        String id;
        do {
            id = TURN_ID_PREFIX + ++turnCounter;
        } while (!usedTurnIds.add(id));
        return id;
    }

    private static Map<String, Object> readBody(MockHttpRequest request) throws IOException {
        if (request.body().length == 0) {
            return Map.of();
        }
        if (JsonTree.parse(request.body()) instanceof Map<?, ?> map) {
            var body = new LinkedHashMap<String, Object>();
            map.forEach((key, value) -> body.put(String.valueOf(key), value));
            return body;
        }
        throw new IOException("The body is not a JSON object");
    }

    private static @Nullable String queryValue(MockHttpRequest request, String name) {
        var query = request.query();
        if (query == null) {
            return null;
        }
        for (var part : query.split("&")) {
            var separator = part.indexOf('=');
            var key = separator < 0 ? part : part.substring(0, separator);
            if (key.equals(name)) {
                return separator < 0 ? "" : URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Reads the chat route a path names: its subject from the path's segments, and the route's action
     * from the segments after {@code /chat}. The path is a chat's only when {@link ChatRoutes} builds it
     * exactly for that subject and action.
     *
     * @param path the request's path
     * @return the route, or {@code null} if the path is no chat's
     */
    private @Nullable Target target(String path) {
        var prefix = ChatRoutes.API_PREFIX + "/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        var segments = Arrays.asList(path.substring(prefix.length()).split("/", -1));
        var chatAt = segments.indexOf(SEGMENT_CHAT);
        if (chatAt < 2) {
            return null;
        }
        try {
            var address = address(segments.subList(0, chatAt));
            if (address == null) {
                return null;
            }
            var target = action(address, segments.subList(chatAt + 1, segments.size()));
            if (target == null || !path.equals(target.expectedPath())) {
                return null;
            }
            return target;
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    private @Nullable Address address(List<String> segments) {
        var first = segments.getFirst();
        return switch (first) {
            case SEGMENT_PLANS, SEGMENT_EPICS, SEGMENT_ENTRIES -> scopedAddress(first, segments);
            case SEGMENT_PROJECTS -> segments.size() == 5 && SEGMENT_WORKSPACE.equals(segments.get(2))
                    && SEGMENT_DECISIONS.equals(segments.get(3))
                    ? workspaceDecision(decode(segments.get(1)), decode(segments.get(4)))
                    : null;
            case SEGMENT_MACHINE -> segments.size() == 3 && SEGMENT_DECISIONS.equals(segments.get(1))
                    ? machineDecision(decode(segments.get(2)))
                    : null;
            default -> null;
        };
    }

    private @Nullable Address scopedAddress(String first, List<String> segments) {
        var id = decode(segments.get(1));
        if (segments.size() == 2) {
            return switch (first) {
                case SEGMENT_PLANS -> work(ChatRoutes.plan(id), ChatSubjectKind.PLAN, id, ChatScopeKind.PLAN,
                        ROLE_REPLAN, EventScopeKind.PLAN);
                case SEGMENT_EPICS -> work(ChatRoutes.epic(id), ChatSubjectKind.EPIC, id, ChatScopeKind.EPIC,
                        ROLE_DECOMPOSE, EventScopeKind.EPIC);
                default -> new Address(ChatRoutes.entry(id), new ChatSubject(ChatSubjectKind.ENTRY, id),
                        ChatScopeKind.ENTRY, epicEntry.test(id) ? ROLE_DECOMPOSE : ROLE_REFINE,
                        new EventScope(EventScopeKind.ENTRY, id, null));
            };
        }
        if (segments.size() != 4 || !SEGMENT_DECISIONS.equals(segments.get(2))) {
            return null;
        }
        var questionId = decode(segments.get(3));
        var decision = new ChatSubject(ChatSubjectKind.DECISION, id + "/" + questionId);
        return switch (first) {
            case SEGMENT_PLANS -> new Address(ChatRoutes.planDecision(id, questionId), decision,
                    ChatScopeKind.DECISION, ROLE_REPLAN, new EventScope(EventScopeKind.PLAN, id, null));
            case SEGMENT_EPICS -> new Address(ChatRoutes.epicDecision(id, questionId), decision,
                    ChatScopeKind.DECISION, ROLE_DECOMPOSE, new EventScope(EventScopeKind.EPIC, id, null));
            default -> new Address(ChatRoutes.entryDecision(id, questionId), decision, ChatScopeKind.DECISION,
                    ROLE_REFINE, new EventScope(EventScopeKind.ENTRY, id, null));
        };
    }

    /**
     * A plan or an epic, running or ended: ended work is addressed like the running one, and its chat is
     * about the archived record.
     */
    private Address work(ChatRoutes routes, ChatSubjectKind kind, String id, ChatScopeKind runningScope,
            String role, EventScopeKind streamKind) {
        var subject = new ChatSubject(kind, id);
        var scope = isEnded(subject) ? ChatScopeKind.ARCHIVE : runningScope;
        return new Address(routes, subject, scope, role, new EventScope(streamKind, id, null));
    }

    private synchronized boolean isEnded(ChatSubject subject) {
        return endedWork.contains(subject);
    }

    private static Address workspaceDecision(String projectId, String questionId) {
        return new Address(ChatRoutes.workspaceDecision(projectId, questionId),
                new ChatSubject(ChatSubjectKind.DECISION, projectId + "/" + questionId), ChatScopeKind.DECISION,
                ROLE_REFINE, new EventScope(EventScopeKind.WORKSPACE, null, projectId));
    }

    private static Address machineDecision(String questionId) {
        return new Address(ChatRoutes.machineDecision(questionId),
                new ChatSubject(ChatSubjectKind.DECISION, MACHINE_SCOPE_ID + "/" + questionId),
                ChatScopeKind.DECISION, ROLE_REFINE, new EventScope(EventScopeKind.MACHINE, null, null));
    }

    private static @Nullable Target action(Address address, List<String> tail) {
        if (tail.isEmpty()) {
            return new Target(address, Action.CHAT, null);
        }
        var first = tail.getFirst();
        if (tail.size() == 1) {
            return switch (first) {
                case SEGMENT_TURNS -> new Target(address, Action.TURNS, null);
                case SEGMENT_END -> new Target(address, Action.END, null);
                default -> null;
            };
        }
        if (tail.size() == 2 && SEGMENT_PROPOSAL.equals(first)) {
            return switch (tail.get(1)) {
                case SEGMENT_ACCEPT -> new Target(address, Action.ACCEPT, null);
                case SEGMENT_REJECT -> new Target(address, Action.REJECT, null);
                default -> null;
            };
        }
        if (tail.size() == 3 && SEGMENT_TURNS.equals(first) && SEGMENT_STOP.equals(tail.get(2))) {
            return new Target(address, Action.STOP, decode(tail.get(1)));
        }
        return null;
    }

    private static String decode(String segment) {
        return URLDecoder.decode(segment, StandardCharsets.UTF_8);
    }

    /** The routes of a chat. */
    private enum Action {
        CHAT, TURNS, STOP, ACCEPT, REJECT, END
    }

    /**
     * Where a chat is addressed and what it is about.
     *
     * @param routes  the chat's routes
     * @param subject the chat's subject
     * @param scope   the scope of a chat opened about the subject
     * @param role    the role a chat about the subject is routed to
     * @param stream  the scope of the stream the chat's events belong on
     */
    private record Address(ChatRoutes routes, ChatSubject subject, ChatScopeKind scope, String role,
    EventScope stream) {
    }

    /**
     * One chat route a request names.
     *
     * @param address the chat's address
     * @param action  the route
     * @param turnId  the turn a stop names, {@code null} for every other route
     */
    private record Target(Address address, Action action, @Nullable String turnId) {

        String expectedPath() {
            var routes = address.routes();
            return switch (action) {
                case CHAT -> routes.chat();
                case TURNS -> routes.turns();
                case STOP -> routes.stop(Objects.requireNonNull(turnId));
                case ACCEPT -> routes.accept();
                case REJECT -> routes.reject();
                case END -> routes.end();
            };
        }
    }

    /**
     * The answer to a chat's request and the events it broadcasts.
     *
     * @param status the HTTP status
     * @param body   the JSON body
     * @param events the events, in order
     */
    private record Outcome(int status, Map<String, Object> body, List<ChatEvent> events) {

        static Outcome of(int status, Map<String, Object> body) {
            return new Outcome(status, body, List.of());
        }
    }

    /**
     * The mutable state of one chat with its whole transcript.
     */
    private static final class Chat {
        private final String id;
        private final ChatSubject subject;
        private final RoutedRole role;
        private final ChatScopeKind scope;
        private ChatStatus state;
        private ChatContext context;
        private @Nullable ChatProposal proposal;
        private final List<ChatTurn> turns;
        private final Map<String, String> clientTurns = new LinkedHashMap<>();

        Chat(String id, ChatSubject subject, RoutedRole role, ChatScopeKind scope, ChatStatus state,
                ChatContext context, @Nullable ChatProposal proposal, List<ChatTurn> turns) {
            this.id = id;
            this.subject = subject;
            this.role = role;
            this.scope = scope;
            this.state = state;
            this.context = context;
            this.proposal = proposal;
            this.turns = new ArrayList<>(turns);
        }

        int indexOf(String turnId) {
            for (var index = 0; index < turns.size(); index++) {
                if (turns.get(index).turnId().equals(turnId)) {
                    return index;
                }
            }
            return -1;
        }

        /**
         * @param end the index the page ends before
         * @return the chat with the page of at most {@link #PAGE_SIZE} turns before that index
         */
        ChatResource toResource(int end) {
            var start = Math.max(0, end - PAGE_SIZE);
            var before = start > 0 ? turns.get(start).turnId() : null;
            return new ChatResource(id, subject, role, scope, state, context, proposal,
                    List.copyOf(turns.subList(start, end)), before);
        }
    }
}
