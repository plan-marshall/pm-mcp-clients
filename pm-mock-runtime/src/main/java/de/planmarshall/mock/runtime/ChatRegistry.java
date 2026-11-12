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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatContext;
import de.planmarshall.api.chat.ChatEndedEvent;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatProposalEvent;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnDeltaEvent;
import de.planmarshall.api.chat.ChatTurnProgressEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.RoutedRole;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnRef;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.event.EventScope;
import de.planmarshall.api.event.EventScopeKind;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.StateDefinition.ChatFailure;

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
 * The operator's message is announced by {@code chat.turn.started} alone. The role's reply is played
 * through the engine's timing seam ({@link EventTiming}): {@code chat.turn.started} with the model, a
 * {@code chat.turn.progress} per step, the text as {@code chat.turn.delta} pieces at a reading pace, and
 * {@code chat.turn.completed} with the whole text; while it plays, the chat is in {@code chat.reply} and
 * takes no message. A stop ends the reply with its text so far, a replaced worker restarts it from the
 * working line, and {@code chat.ended} names what ended a chat: the operator, a decision's carried
 * proposal, a decision answered elsewhere, the subject's end, or the entry's close. Every event goes on
 * the stream of the chat's subject and on {@code /api/v1/events}. Every message key the registry writes
 * is one {@link de.planmarshall.api.chat.ChatMessageKeys} maps.
 * <p>
 * The state is seeded from the active state ({@link #seed(Seed)}) and lives in memory; a scenario
 * switch or a reset re-seeds it.
 * <p>
 * {@code POST /mock/inject} ({@link #handleInject}) and a state's {@code chat_failures} arm a
 * {@link ChatFailure}, played once: the next reply ends {@code failed} ({@code infeasible},
 * {@code lost}, {@code too_long}), is stopped or restarted by a replaced worker after its first piece,
 * or the next request of a chat answers with an injected error ({@code http_error}) or late
 * ({@code slow}). A re-seed disarms what was not played.
 * <p>
 * The role's replies follow the scripted conversation of {@link ChatScripts}; a reply that makes a
 * proposal adds the proposal's own turn and announces it by {@code chat.proposal} ({@code open}), and
 * an older proposal still open is {@code replaced}. Accept and reject act: every answer is announced by
 * {@code chat.proposal}, a revision of a plan is {@code accepted} (queued for the plan's next task
 * boundary), every other kind is {@code applied} and, where it changes its subject, reaches the
 * subject's resource through the hook the registry is built with; a rejected proposal changes nothing,
 * and a proposal answered once is answered for good. {@code POST /mock/broadcast} of a chat event
 * ({@link #handleBroadcast}) plays another device: an operator's message, an answered proposal or the
 * chat's end is applied to the chat and sent on the stream of its subject.
 */
final class ChatRegistry {

    /** The most turns a page of the transcript holds. */
    private static final int PAGE_SIZE = 50;

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
    private static final String CHAT_ID_PREFIX = "chat-";
    private static final String TURN_ID_PREFIX = "turn-";
    private static final String MESSAGE_NO_CHAT = "No chat is open about ";
    private static final String REASON_PREFIX = "chat.reason.";
    private static final String KEY_ARMED = "armed";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_STATUS = "status";
    private static final String KEY_EVENT = "event";

    /** The control route that arms a chat failure. */
    private static final String PATH_INJECT = "/mock/inject";

    /** The delay of a reply's {@code chat.turn.started} after the operator's message, realistic timing. */
    private static final long REPLY_START_MS = 300;
    /** The pace of a reply's steps, realistic timing. */
    private static final long STEP_PACE_MS = 400;
    /** The pace a reply's pieces are written at, realistic timing: about a word each. */
    private static final long READING_PACE_MS = 80;

    private static final String CHAT_EVENT_PREFIX = "chat.";
    private static final String EVENT_DECISION_ANSWERED = "decision.answered";
    private static final String EVENT_SCOPE_ENDED = "scope.ended";
    private static final String EVENT_ENTRY_ABANDONED = "entry.abandoned";
    private static final String EVENT_PLAN_STARTED = "plan.started";
    private static final String EVENT_EPIC_STARTED = "epic.started";
    private static final String KEY_ID = "id";
    private static final String KEY_PLAN_ID = "plan_id";
    private static final String KEY_EPIC_ID = "epic_id";
    private static final String KEY_ENTRY_ID = "entry_id";

    /** The callback a chat uses to tell its clients of an event. */
    private final Consumer<ChatEvent> broadcaster;
    /** Whether an entry opens an epic: its chat is routed to {@code decompose}, a plan's to {@code refine}. */
    private final Predicate<String> epicEntry;
    /** The timing seam a reply's events are played through. */
    private final EventTiming timing;
    /** The hook through which an applied proposal changes its subject's resource. */
    private final Consumer<AppliedProposal> applied;

    private final Map<ChatSubject, Chat> chats = new LinkedHashMap<>();
    private final Set<ChatSubject> endedWork = new HashSet<>();
    private final Set<String> usedChatIds = new HashSet<>();
    private final Set<String> usedTurnIds = new HashSet<>();
    /** The armed failures the next chat requests play, oldest first. */
    private final Deque<ChatFailure> requestFailures = new ArrayDeque<>();
    /** The armed failures the next replies play, oldest first. */
    private final Deque<ChatFailure> replyFailures = new ArrayDeque<>();
    private long chatCounter;
    private long turnCounter;

    /**
     * @param broadcaster the callback that broadcasts a chat event on its subject's stream
     * @param epicEntry   whether the entry of an id opens an epic
     * @param timing      the timing seam a reply's events are played through
     * @param applied     the hook through which an applied proposal changes its subject's resource,
     *                    called outside the registry's lock
     */
    ChatRegistry(Consumer<ChatEvent> broadcaster, Predicate<String> epicEntry, EventTiming timing,
            Consumer<AppliedProposal> applied) {
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
        this.epicEntry = Objects.requireNonNull(epicEntry, "epicEntry");
        this.timing = Objects.requireNonNull(timing, "timing");
        this.applied = Objects.requireNonNull(applied, "applied");
    }

    /**
     * A proposal applied in a chat, handed to the hook that changes its subject's resource: an entry's
     * description, an epic's staged rows or decomposition draft, or the start a follow-up plan of ended
     * work opens.
     *
     * @param subject what the chat is about
     * @param kind    the kind of change
     * @param after   the text after the change, or {@code null}
     */
    record AppliedProposal(ChatSubject subject, ProposalKind kind, @Nullable String after) {
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
        chats.values().forEach(Chat::cancelPlay);
        chats.clear();
        endedWork.clear();
        usedChatIds.clear();
        usedTurnIds.clear();
        requestFailures.clear();
        replyFailures.clear();
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
        rememberStream(target.address());
        var failure = takeRequestFailure();
        if (failure != null && playRequestFailure(failure, response)) {
            return true;
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
        if (outcome.then() != null) {
            outcome.then().run();
        }
        response.json(outcome.status(), outcome.body());
        return true;
    }

    /** A seeded chat learns its stream from the first route that addresses it. */
    private synchronized void rememberStream(Address address) {
        var chat = chats.get(address.subject());
        if (chat != null && chat.stream == null) {
            chat.stream = address.stream();
        }
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
        chat.stream = address.stream();
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
        var at = Instant.now().toString();
        var operatorTurn = new ChatTurn(nextTurnId(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                TurnStatus.COMPLETE, TurnFormat.PLAIN, false, null, message.text(), List.of(), List.of(), at, null,
                List.of());
        chat.turns.add(operatorTurn);
        chat.clientTurns.put(message.clientTurnId(), operatorTurn.turnId());
        var operatorStarted = event(address, ChatTurnStartedEvent.EVENT_NAME, new ChatTurnStartedEvent(chat.id,
                operatorTurn.turnId(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnFormat.PLAIN, false, null,
                null, message.text()).toMap());

        chat.context = new ChatContext(chat.context.knows(), at);
        var script = ChatScripts.reply(chat.subject.kind(), chat.scope, chat.replyCount());
        var reply = new ChatTurn(nextTurnId(), TurnKind.MESSAGE, chat.role.name(), TurnStatus.STREAMING,
                script.format(), script.untrusted(), chat.role.model(), "", List.of(), List.of(), at, null,
                List.of());
        chat.turns.add(reply);
        chat.state = ChatStatus.REPLY;
        var play = chat.startPlay(address, reply.turnId(), replyFailures.pollFirst(), script);
        return new Outcome(ChatTurnAccepted.HTTP_STATUS, new ChatTurnAccepted(operatorTurn.turnId()).toMap(),
                List.of(operatorStarted), () -> playReply(chat, play));
    }

    /**
     * Plays a reply through the timing seam: {@code chat.turn.started} with the model, a
     * {@code chat.turn.progress} per step, the text as {@code chat.turn.delta} pieces at a reading pace,
     * and {@code chat.turn.completed} with the whole text and its references, followed by
     * {@code chat.proposal} when the reply makes a proposal. Every step runs only while the play is the
     * chat's current one, so a stop or a replaced worker leaves the rest of it unplayed.
     *
     * @param chat the chat
     * @param play the play
     */
    private void playReply(Chat chat, Play play) {
        var script = play.script();
        var text = script.text();
        var failure = play.failure();
        var pieces = failure == null ? pieces(text) : pieces(text).subList(0, 1);
        var at = REPLY_START_MS;
        play.schedule(timing, at, () -> playStep(chat, play, () -> {
            var turn = chat.turns.get(chat.indexOf(play.turnId()));
            return List.of(event(play.address(), ChatTurnStartedEvent.EVENT_NAME, new ChatTurnStartedEvent(chat.id,
                    turn.turnId(), turn.kind(), turn.author(), turn.format(), turn.untrusted(), turn.model(),
                    chat.context, null).toMap()));
        }));
        for (var step : script.steps()) {
            at += STEP_PACE_MS;
            play.schedule(timing, at, () -> playStep(chat, play, () -> {
                chat.updateReply(play.turnId(), turn -> withProgress(turn, step));
                return List.of(event(play.address(), ChatTurnProgressEvent.EVENT_NAME,
                        new ChatTurnProgressEvent(chat.id, play.turnId(), step).toMap()));
            }));
        }
        for (var piece : pieces) {
            at += READING_PACE_MS;
            play.schedule(timing, at, () -> playStep(chat, play, () -> {
                chat.updateReply(play.turnId(), turn -> withText(turn, turn.text() + piece, turn.status()));
                return List.of(event(play.address(), ChatTurnDeltaEvent.EVENT_NAME,
                        new ChatTurnDeltaEvent(chat.id, play.turnId(), piece).toMap()));
            }));
        }
        if (failure != null) {
            play.schedule(timing, at + READING_PACE_MS, () -> playFailure(chat, play, failure.kind()));
            return;
        }
        play.schedule(timing, at + READING_PACE_MS, () -> playStep(chat, play, () -> {
            chat.updateReply(play.turnId(), turn -> withCompletion(turn, text, script.refs()));
            chat.endPlay();
            var turn = chat.turns.get(chat.indexOf(play.turnId()));
            var events = new ArrayList<ChatEvent>();
            events.add(event(play.address(), ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(chat.id,
                    turn.turnId(), turn.kind(), TurnStatus.COMPLETE, turn.untrusted(), turn.text(), turn.refs(),
                    null, List.of()).toMap()));
            var proposal = script.proposal();
            if (proposal != null) {
                events.addAll(propose(chat, play.address(), turn, proposal));
            }
            chat.state = chat.idleState();
            return events;
        }));
    }

    /**
     * Records the proposal a reply makes as a turn of its own after the reply, open until it is
     * answered; an older proposal still open is replaced.
     *
     * @return the {@code chat.proposal} events, the replaced one first
     */
    private List<ChatEvent> propose(Chat chat, Address address, ChatTurn reply, ChatScripts.Proposal scripted) {
        var events = new ArrayList<ChatEvent>();
        var open = chat.proposal;
        if (open != null && open.isOpen()) {
            chat.proposal = withState(open, ProposalState.REPLACED);
            events.add(proposalEvent(chat, address.stream(), chat.proposal));
        }
        var proposalTurn = new ChatTurn(nextTurnId(), TurnKind.PROPOSAL, reply.author(), TurnStatus.COMPLETE,
                TurnFormat.MARKDOWN, false, reply.model(), scripted.summary(), List.of(), List.of(),
                Instant.now().toString(), null, List.of());
        chat.turns.add(proposalTurn);
        chat.proposal = new ChatProposal(proposalTurn.turnId(), reply.turnId(), scripted.kind(), scripted.summary(),
                scripted.applies(), scripted.before(), scripted.after(), ProposalState.OPEN, null);
        events.add(proposalEvent(chat, address.stream(), chat.proposal));
        return events;
    }

    private static ChatProposal withState(ChatProposal proposal, ProposalState state) {
        return new ChatProposal(proposal.turnId(), proposal.replyTurnId(), proposal.kind(), proposal.summary(),
                proposal.applies(), proposal.before(), proposal.after(), state, null);
    }

    private static ChatEvent proposalEvent(Chat chat, EventScope stream, ChatProposal proposal) {
        return new ChatEvent(ChatProposalEvent.EVENT_NAME, stream, new ChatProposalEvent(chat.id, proposal.turnId(),
                proposal.replyTurnId(), proposal.kind(), proposal.state(), null).toMap());
    }

    /**
     * Runs one step of a play under the registry's lock and broadcasts its events, unless the play is no
     * longer the chat's current one.
     */
    private synchronized void playStep(Chat chat, Play play, Supplier<List<ChatEvent>> step) {
        if (!isCurrent(chat, play)) {
            return;
        }
        step.get().forEach(broadcaster);
    }

    /** Whether the play is the current one of a chat the registry still holds; call under the lock. */
    private boolean isCurrent(Chat chat, Play play) {
        return chat.play == play && chats.get(chat.subject) == chat;
    }

    /**
     * Plays an injected failure after the reply's first piece: the reply ends {@code failed} with the
     * reason {@code chat.reason.<kind>}, is stopped with its text so far, or restarts for a replaced
     * worker. Nothing plays when the play is no longer the chat's current one.
     */
    private void playFailure(Chat chat, Play play, ChatFailure.Kind kind) {
        if (kind == ChatFailure.Kind.WORKER_REPLACED) {
            synchronized (this) {
                if (!isCurrent(chat, play)) {
                    return;
                }
            }
            replaceWorker(chat.subject);
            return;
        }
        playStep(chat, play, () -> {
            var reason = kind.failsReply() ? MessageText.of(REASON_PREFIX + kind.wireValue()) : null;
            var status = reason != null ? TurnStatus.FAILED : TurnStatus.STOPPED;
            chat.updateReply(play.turnId(), turn -> new ChatTurn(turn.turnId(), turn.kind(), turn.author(), status,
                    turn.format(), turn.untrusted(), turn.model(), turn.text(), turn.steps(), turn.refs(), turn.at(),
                    reason, turn.lines()));
            chat.endPlay();
            chat.state = chat.idleState();
            var turn = chat.turns.get(chat.indexOf(play.turnId()));
            return List.of(event(play.address(), ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(
                    chat.id, turn.turnId(), turn.kind(), status, turn.untrusted(), turn.text(), turn.refs(), reason,
                    List.of()).toMap()));
        });
    }

    /**
     * Arms failures, each played once by the next request or reply it matches.
     *
     * @param failures the failures, in order
     */
    synchronized void arm(List<ChatFailure> failures) {
        for (var failure : failures) {
            (failure.kind().isReply() ? replyFailures : requestFailures).addLast(failure);
        }
    }

    /**
     * Serves {@code POST /mock/inject}: arms the failure the body names, played once by the next chat
     * request or reply it matches; a failure of an unknown name, or with a member its kind does not
     * take, is refused {@code invalid_arguments}.
     *
     * @param request  the request
     * @param response the response
     * @return {@code true} if the request was the inject route's and has been answered
     * @throws IOException if the answer cannot be written
     */
    boolean handleInject(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (!PATH_INJECT.equals(request.path()) || !METHOD_POST.equals(request.method())) {
            return false;
        }
        ChatFailure failure;
        try {
            failure = ChatFailure.fromMap(readBody(request));
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IOException | IllegalArgumentException e) {
            var refusal = refused(ApiErrorCode.INVALID_ARGUMENTS, e.getMessage());
            response.json(refusal.status(), refusal.body());
            return true;
        }
        List<String> pending;
        synchronized (this) {
            arm(List.of(failure));
            pending = new ArrayList<String>();
            requestFailures.forEach(armed -> pending.add(armed.kind().wireValue()));
            replyFailures.forEach(armed -> pending.add(armed.kind().wireValue()));
        }
        response.json(200, Map.of(KEY_ARMED, failure.kind().wireValue(), KEY_PENDING, pending));
        return true;
    }

    /**
     * @return the failure the next chat request plays, or {@code null}
     */
    private synchronized @Nullable ChatFailure takeRequestFailure() {
        return requestFailures.pollFirst();
    }

    /**
     * Plays a request failure: an {@code http_error} answers with its status and code, a {@code slow}
     * request is answered after its delay.
     *
     * @return {@code true} if the failure answered the request
     */
    private static boolean playRequestFailure(ChatFailure failure, MockHttpResponse response) throws IOException {
        if (failure.kind() == ChatFailure.Kind.HTTP_ERROR) {
            response.json(failure.status(), Map.of(KEY_ERROR, Objects.requireNonNull(failure.code()), KEY_MESSAGE,
                    "Injected failure: " + failure.code()));
            return true;
        }
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(failure.delayMs()));
        return false;
    }

    /**
     * Replaces the worker writing a chat's reply: the play is cancelled, the reply's text is cleared,
     * and the reply is announced again and played from the working line.
     *
     * @param subject what the chat is about
     * @return {@code true} if a reply was being played and restarts
     */
    boolean replaceWorker(ChatSubject subject) {
        Chat chat;
        Play play;
        synchronized (this) {
            chat = chats.get(subject);
            if (chat == null || chat.play == null) {
                return false;
            }
            var turnId = chat.play.turnId();
            var address = chat.play.address();
            var script = chat.play.script();
            chat.cancelPlay();
            chat.updateReply(turnId, turn -> new ChatTurn(turn.turnId(), turn.kind(), turn.author(),
                    TurnStatus.STREAMING, turn.format(), turn.untrusted(), turn.model(), "", List.of(),
                    turn.refs(), turn.at(), null, turn.lines()));
            play = chat.startPlay(address, turnId, null, script);
        }
        playReply(chat, play);
        return true;
    }

    /**
     * Ends every open chat an event of the runtime ends: a decision answered elsewhere ends the chats
     * about it, a plan or an epic that ended ends its chat, and an entry abandoned or created ends the
     * entry's chat. The chat's own events are not observed.
     *
     * @param event   the event's name
     * @param payload the event's payload
     */
    void observe(String event, Map<String, Object> payload) {
        if (event.startsWith(CHAT_EVENT_PREFIX)) {
            return;
        }
        switch (event) {
            case EVENT_DECISION_ANSWERED -> {
                var decisionId = JsonTree.string(payload, KEY_ID);
                if (decisionId != null) {
                    endWhere(subject -> subject.kind() == ChatSubjectKind.DECISION
                            && subject.id().endsWith("/" + decisionId), ChatEndedEvent.EndedBy.ANSWERED_ELSEWHERE);
                }
            }
            case EVENT_SCOPE_ENDED -> {
                endWork(ChatSubjectKind.PLAN, JsonTree.string(payload, KEY_PLAN_ID));
                endWork(ChatSubjectKind.EPIC, JsonTree.string(payload, KEY_EPIC_ID));
            }
            case EVENT_ENTRY_ABANDONED, EVENT_PLAN_STARTED, EVENT_EPIC_STARTED -> {
                var entryId = JsonTree.string(payload, KEY_ENTRY_ID);
                if (entryId != null) {
                    endWhere(new ChatSubject(ChatSubjectKind.ENTRY, entryId)::equals,
                            ChatEndedEvent.EndedBy.ENTRY_CLOSED);
                }
            }
            default -> {
                // no chat ends with any other event
            }
        }
    }

    private void endWork(ChatSubjectKind kind, @Nullable String id) {
        if (id == null) {
            return;
        }
        var subject = new ChatSubject(kind, id);
        synchronized (this) {
            endedWork.add(subject);
        }
        endWhere(subject::equals, ChatEndedEvent.EndedBy.SUBJECT_ENDED);
    }

    private void endWhere(Predicate<ChatSubject> subjects, ChatEndedEvent.EndedBy endedBy) {
        var events = new ArrayList<ChatEvent>();
        synchronized (this) {
            for (var chat : chats.values()) {
                if (subjects.test(chat.subject) && !chat.state.isEnded()) {
                    events.add(endChat(chat, ChatStatus.END, endedBy));
                }
            }
        }
        events.forEach(broadcaster);
    }

    /**
     * Ends a chat: a reply being played stops where it is, and {@code chat.ended} names what ended it.
     *
     * @return the chat's {@code chat.ended} event
     */
    private ChatEvent endChat(Chat chat, ChatStatus state, ChatEndedEvent.EndedBy endedBy) {
        var play = chat.play;
        if (play != null) {
            chat.cancelPlay();
            chat.updateReply(play.turnId(), turn -> withText(turn, turn.text(), TurnStatus.STOPPED));
        }
        chat.state = state;
        return new ChatEvent(ChatEndedEvent.EVENT_NAME, streamOf(chat),
                new ChatEndedEvent(chat.id, endedBy, null, null).toMap());
    }

    /**
     * The stream of a chat's events: the one its routes named, or for a seeded chat no route has named
     * yet, the stream its subject names — a plan's, an epic's, an entry's, the machine's for a machine
     * decision, and the runtime-wide one for a decision whose scope the subject alone does not tell.
     */
    private static EventScope streamOf(Chat chat) {
        if (chat.stream != null) {
            return chat.stream;
        }
        var id = chat.subject.id();
        return switch (chat.subject.kind()) {
            case PLAN -> new EventScope(EventScopeKind.PLAN, id, null);
            case EPIC -> new EventScope(EventScopeKind.EPIC, id, null);
            case ENTRY -> new EventScope(EventScopeKind.ENTRY, id, null);
            default -> id.startsWith(MACHINE_SCOPE_ID + "/")
                    ? new EventScope(EventScopeKind.MACHINE, null, null)
                    : new EventScope(EventScopeKind.RUNTIME, null, null);
        };
    }

    private static ChatTurn withText(ChatTurn turn, String text, TurnStatus status) {
        return new ChatTurn(turn.turnId(), turn.kind(), turn.author(), status, turn.format(), turn.untrusted(),
                turn.model(), text, turn.steps(), turn.refs(), turn.at(), null, turn.lines());
    }

    private static ChatTurn withCompletion(ChatTurn turn, String text, List<TurnRef> refs) {
        return new ChatTurn(turn.turnId(), turn.kind(), turn.author(), TurnStatus.COMPLETE, turn.format(),
                turn.untrusted(), turn.model(), text, turn.steps(), refs, turn.at(), null, turn.lines());
    }

    private static ChatTurn withProgress(ChatTurn turn, MessageText step) {
        var steps = new ArrayList<>(turn.steps());
        steps.add(step);
        return new ChatTurn(turn.turnId(), turn.kind(), turn.author(), turn.status(), turn.format(),
                turn.untrusted(), turn.model(), turn.text(), steps, turn.refs(), turn.at(), null, turn.lines());
    }

    /**
     * @return the text in pieces of a word with the space after it, in order
     */
    private static List<String> pieces(String text) {
        var pieces = new ArrayList<String>();
        var start = 0;
        for (var index = 0; index < text.length(); index++) {
            if (text.charAt(index) == ' ') {
                pieces.add(text.substring(start, index + 1));
                start = index + 1;
            }
        }
        if (start < text.length()) {
            pieces.add(text.substring(start));
        }
        return pieces;
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
        if (chat.play != null && chat.play.turnId().equals(turnId)) {
            chat.cancelPlay();
        }
        var stopped = withText(turn, turn.text(), TurnStatus.STOPPED);
        chat.turns.set(index, stopped);
        chat.state = chat.idleState();
        var event = event(address, ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(chat.id,
                stopped.turnId(), stopped.kind(), TurnStatus.STOPPED, stopped.untrusted(), stopped.text(),
                stopped.refs(), null, List.of()).toMap());
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(event), null);
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
        chat.proposal = withState(proposal, state);
        var answered = proposalEvent(chat, address.stream(), chat.proposal);
        if (accept && chat.subject.kind() == ChatSubjectKind.DECISION) {
            // A decision's chat carries the accepted proposal to the decision and ends with it.
            var ended = endChat(chat, ChatStatus.CARRIED, ChatEndedEvent.EndedBy.PROPOSAL_CARRIED);
            return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(answered, ended), null);
        }
        chat.state = ChatStatus.TURN;
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(answered),
                applyHook(chat.subject, chat.proposal));
    }

    /**
     * @return the call of the hook that changes the subject's resource when the proposal is applied, or
     *         {@code null} when it is not
     */
    private @Nullable Runnable applyHook(ChatSubject subject, ChatProposal proposal) {
        if (proposal.state() != ProposalState.APPLIED || subject.kind() == ChatSubjectKind.DECISION) {
            return null;
        }
        var change = new AppliedProposal(subject, proposal.kind(), proposal.after());
        return () -> applied.accept(change);
    }

    /**
     * Serves {@code POST /mock/broadcast} of a chat event, so a test plays what another device did: the
     * event's {@code chat_id} names the chat, the event is applied to it and sent on the stream of its
     * subject. {@code chat.turn.started} of the operator records the message, {@code chat.proposal}
     * answers the chat's proposal (an applied one changes its subject's resource), and {@code chat.ended}
     * ends the chat; every other chat event is sent as it is. A chat nobody opened is refused
     * {@code target_not_found}, a payload its event's record refuses {@code invalid_arguments}, and an
     * act the chat no longer offers {@code act_not_available}.
     *
     * @param event    the event's name
     * @param data     the event's payload
     * @param response the response
     * @return {@code true} if the event was a chat's and has been answered
     * @throws IOException if the answer cannot be written
     */
    boolean handleBroadcast(String event, Object data, MockHttpResponse response) throws IOException {
        if (!event.startsWith(CHAT_EVENT_PREFIX)) {
            return false;
        }
        var payload = JsonTree.asObject(data);
        Outcome outcome;
        try {
            outcome = payload == null
                    ? refused(ApiErrorCode.INVALID_ARGUMENTS, "The payload of " + event + " is not an object")
                    : playBroadcast(event, payload);
        } catch (IOException e) {
            outcome = refused(ApiErrorCode.INVALID_ARGUMENTS, e.getMessage());
        }
        outcome.events().forEach(broadcaster);
        if (outcome.then() != null) {
            outcome.then().run();
        }
        response.json(outcome.status(), outcome.body());
        return true;
    }

    private synchronized Outcome playBroadcast(String event, Map<String, Object> payload) throws IOException {
        var chatId = JsonTree.string(payload, ChatEndedEvent.FIELD_CHAT_ID);
        var chat = chats.values().stream().filter(candidate -> candidate.id.equals(chatId)).findFirst().orElse(null);
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, "No chat " + chatId);
        }
        Runnable then = null;
        if (ChatTurnStartedEvent.EVENT_NAME.equals(event)) {
            var started = ChatTurnStartedEvent.fromMap(payload);
            if (started.isOperator()) {
                if (chat.state.isEnded() || chat.state == ChatStatus.REPLY) {
                    return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The chat takes no message now");
                }
                if (!usedTurnIds.add(started.turnId())) {
                    return refused(ApiErrorCode.CONFLICT, "The turn " + started.turnId() + " exists");
                }
                chat.turns.add(new ChatTurn(started.turnId(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                        TurnStatus.COMPLETE, TurnFormat.PLAIN, started.untrusted(), null,
                        Objects.requireNonNullElse(started.text(), ""), List.of(),
                        List.of(), Instant.now().toString(), null, List.of()));
            }
        } else if (ChatProposalEvent.EVENT_NAME.equals(event)) {
            var answered = ChatProposalEvent.fromMap(payload);
            var proposal = chat.proposal;
            if (chat.state.isEnded() || proposal == null || !proposal.isOpen()) {
                return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "No proposal is open in the chat");
            }
            if (!proposal.turnId().equals(answered.turnId())) {
                return refused(ApiErrorCode.CONFLICT, "The proposal " + answered.turnId() + " is not the open one");
            }
            chat.proposal = new ChatProposal(proposal.turnId(), proposal.replyTurnId(), proposal.kind(),
                    proposal.summary(), proposal.applies(), proposal.before(), proposal.after(), answered.state(),
                    answered.finding());
            if (!chat.proposal.isOpen() && chat.state == ChatStatus.READ_BACK) {
                chat.state = ChatStatus.TURN;
            }
            then = applyHook(chat.subject, chat.proposal);
        } else if (ChatEndedEvent.EVENT_NAME.equals(event)) {
            var ended = ChatEndedEvent.fromMap(payload);
            if (chat.state.isEnded()) {
                return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The chat has ended");
            }
            endChat(chat, ended.endedBy() == ChatEndedEvent.EndedBy.PROPOSAL_CARRIED ? ChatStatus.CARRIED
                    : ChatStatus.END, ended.endedBy());
        }
        return new Outcome(200, Map.of(KEY_STATUS, "broadcasted", KEY_EVENT, event),
                List.of(new ChatEvent(event, streamOf(chat), payload)), then);
    }

    private synchronized Outcome end(Address address) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        if (chat.state.isEnded()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The chat has ended");
        }
        var event = endChat(chat, ChatStatus.END, ChatEndedEvent.EndedBy.OPERATOR);
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(event), null);
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
     * @param then   what plays after the events, before the answer is written, or {@code null}
     */
    private record Outcome(int status, Map<String, Object> body, List<ChatEvent> events, @Nullable Runnable then) {

        static Outcome of(int status, Map<String, Object> body) {
            return new Outcome(status, body, List.of(), null);
        }
    }

    /**
     * One play of a reply: the turn it writes, the scripted reply it plays, where its events go, and its
     * scheduled steps. A play that is no longer its chat's current one plays nothing more.
     */
    private static final class Play {
        private final Address address;
        private final String turnId;
        private final @Nullable ChatFailure failure;
        private final ChatScripts.Reply script;
        private final List<Future<?>> tasks = new ArrayList<>();

        Play(Address address, String turnId, @Nullable ChatFailure failure, ChatScripts.Reply script) {
            this.address = address;
            this.turnId = turnId;
            this.failure = failure;
            this.script = script;
        }

        ChatScripts.Reply script() {
            return script;
        }

        @Nullable
        ChatFailure failure() {
            return failure;
        }

        Address address() {
            return address;
        }

        String turnId() {
            return turnId;
        }

        void schedule(EventTiming timing, long afterMs, Runnable step) {
            var task = timing.play(step, afterMs);
            if (task != null) {
                synchronized (tasks) {
                    tasks.add(task);
                }
            }
        }

        void cancel() {
            synchronized (tasks) {
                tasks.forEach(task -> task.cancel(false));
                tasks.clear();
            }
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
        /** The stream the chat's events go to, once known. */
        private @Nullable EventScope stream;
        /** The reply being played, or {@code null}. */
        private @Nullable Play play;

        Play startPlay(Address address, String turnId, @Nullable ChatFailure failure, ChatScripts.Reply script) {
            cancelPlay();
            stream = address.stream();
            play = new Play(address, turnId, failure, script);
            return play;
        }

        /**
         * @return how many replies the role has written in the chat
         */
        int replyCount() {
            return (int) turns.stream().filter(turn -> turn.kind() == TurnKind.MESSAGE
                    && !ChatTurn.AUTHOR_OPERATOR.equals(turn.author())).count();
        }

        void cancelPlay() {
            if (play != null) {
                play.cancel();
                play = null;
            }
        }

        void endPlay() {
            play = null;
        }

        /**
         * @return the state of a chat whose reply is over: reading back while a proposal is open
         */
        ChatStatus idleState() {
            return proposal != null && proposal.isOpen() ? ChatStatus.READ_BACK : ChatStatus.TURN;
        }

        void updateReply(String turnId, UnaryOperator<ChatTurn> change) {
            var index = indexOf(turnId);
            if (index >= 0) {
                turns.set(index, change.apply(turns.get(index)));
            }
        }

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
