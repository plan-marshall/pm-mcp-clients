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
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
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
import de.planmarshall.api.chat.ChatOrigin;
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
import de.planmarshall.api.chat.NarrativeLine;
import de.planmarshall.api.chat.PlanRevisionQueuedEvent;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.QueuedRevision;
import de.planmarshall.api.chat.RefKind;
import de.planmarshall.api.chat.RoutedRole;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnRef;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.event.EventName;
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
 * proposal, a decision answered elsewhere, or the subject's end; the start of an entry's work ends its
 * chat and abandoning the entry deletes the transcript with it. Every event goes on the stream of the
 * chat's subject and on {@code /api/v1/events}. Every message key the registry writes is one
 * {@link de.planmarshall.api.chat.ChatMessageKeys} maps.
 * <p>
 * The subject's narrative reaches the transcript as {@code notice} turns ({@code chat.turn.completed}
 * carrying {@code lines[]}) between the role's replies: several lines that come between two replies are
 * one notice, and a line that arises while a reply is written is held until that reply ends, whether it
 * completed, failed or was stopped ({@code state-catalogue.adoc#chat-narrative}).
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
    /** The item of {@code control.chat.knows.item} whose branch is the archived record of ended work. */
    private static final String KNOWS_ARCHIVE = "other";
    private static final String KNOWS_DECISION = "decision";
    private static final String KNOWS_DESCRIPTION = "description";
    private static final String ARG_OUTCOME = "outcome";
    private static final String CHAT_ID_PREFIX = "chat-";
    private static final String TURN_ID_PREFIX = "turn-";
    private static final String MESSAGE_NO_CHAT = "No chat is open about ";
    private static final String MESSAGE_CHAT_ENDED = "The chat has ended";
    private static final String REASON_PREFIX = "chat.reason.";
    private static final String FINDING_CHECK_FAILED = "chat.finding.check_failed";
    private static final String FINDING_CHANGED_SINCE = "chat.finding.changed_since";
    private static final String FINDING_ALREADY_QUEUED = "chat.finding.already_queued";
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
    private static final String EVENT_DECISION_OPENED = "decision.opened";
    private static final String EVENT_DECISION_CLOSED = "decision.closed";
    private static final String EVENT_HOLD_OPENED = "hold.opened";
    private static final String EVENT_HOLD_CLEARED = "hold.cleared";
    private static final String EVENT_ACTIVITY_CHANGED = "activity.changed";
    private static final String EVENT_PHASE_CHANGED = "phase.changed";
    private static final String EVENT_INSTANCE_CHANGED = "instance.changed";
    private static final String EVENT_TASK_ENDED = "task.ended";
    private static final String EVENT_SCOPE_ENDED = "scope.ended";
    private static final String EVENT_ENTRY_ABANDONED = "entry.abandoned";
    private static final String EVENT_PLAN_STARTED = "plan.started";
    private static final String EVENT_EPIC_STARTED = "epic.started";
    private static final String LINE_PREFIX = "chat.line.";
    private static final String KEY_ID = "id";
    private static final String KEY_PLAN_ID = "plan_id";
    private static final String KEY_EPIC_ID = "epic_id";
    private static final String KEY_ENTRY_ID = "entry_id";
    private static final String KEY_TASK_ID = "task_id";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_VERSION = "version";
    private static final String KEY_ACTIVITY = "activity";
    private static final String KEY_HOLD_ID = "hold_id";
    private static final String KEY_SUBJECT_ID = "subject_id";
    private static final String ACTIVITY_PAUSING = "pausing";
    private static final String ACTIVITY_PAUSED = "paused";
    private static final String ACTIVITY_RUNNING = "running";
    /** A member the registry marks its own announcements with, which never reaches the wire. */
    private static final String INTERNAL_KEY = "$internal";

    /**
     * The {@code pm-api} record each chat event of the catalogue is read through, keyed by its
     * {@link EventName}; a broadcast {@code chat.*} name without an entry is refused.
     */
    private static final Map<EventName, ChatEventParser> CHAT_EVENT_PARSERS = Collections.unmodifiableMap(
            new EnumMap<>(Map.<EventName, ChatEventParser>of(
                    EventName.CHAT_TURN_STARTED, ChatTurnStartedEvent::fromMap,
                    EventName.CHAT_TURN_PROGRESS, ChatTurnProgressEvent::fromMap,
                    EventName.CHAT_TURN_DELTA, ChatTurnDeltaEvent::fromMap,
                    EventName.CHAT_TURN_COMPLETED, ChatTurnCompletedEvent::fromMap,
                    EventName.CHAT_PROPOSAL, ChatProposalEvent::fromMap,
                    EventName.CHAT_ENDED, ChatEndedEvent::fromMap)));

    /** The callback a chat uses to tell its clients of an event. */
    private final Consumer<ChatEvent> broadcaster;
    /** Whether an entry opens an epic: its chat is routed to {@code decompose}, a plan's to {@code refine}. */
    private final Predicate<String> epicEntry;
    /** The timing seam a reply's events are played through. */
    private final EventTiming timing;
    /** The hook through which an applied proposal changes its subject's resource. */
    private final Consumer<AppliedProposal> applied;
    /** What the registry reads of, and changes in, its subjects. */
    private final ChatFacts facts;

    private final Map<ChatSubject, Chat> chats = new LinkedHashMap<>();
    /** The ended plans and epics with the outcome each ended with, {@code null} where none was named. */
    private final Map<ChatSubject, @Nullable String> endedWork = new LinkedHashMap<>();
    /** The plan's instance version each queued revision was accepted against, by plan id. */
    private final Map<String, Integer> queuedBaseVersions = new LinkedHashMap<>();
    /** The last activity a chat's narrative reported, by plan id, so a repeated one writes no line. */
    private final Map<String, String> lastActivities = new LinkedHashMap<>();
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
     * @param facts       what the registry reads of, and changes in, its subjects
     */
    ChatRegistry(Consumer<ChatEvent> broadcaster, Predicate<String> epicEntry, EventTiming timing,
            Consumer<AppliedProposal> applied, ChatFacts facts) {
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
        this.epicEntry = Objects.requireNonNull(epicEntry, "epicEntry");
        this.timing = Objects.requireNonNull(timing, "timing");
        this.applied = Objects.requireNonNull(applied, "applied");
        this.facts = Objects.requireNonNull(facts, "facts");
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
     * @param endedWork the ended plans and epics, whose chats are about the archived record, each with the
     *                  wire value of the outcome it ended with, {@code null} where none is named
     */
    record Seed(List<ChatResource> chats, Map<ChatSubject, @Nullable String> endedWork) {

        /**
         * @param chats     the chats, {@code null} read as none
         * @param endedWork the ended work, {@code null} read as none
         */
        Seed {
            chats = chats == null ? List.of() : List.copyOf(chats);
            endedWork = endedWork == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(endedWork));
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
        queuedBaseVersions.clear();
        chatCounter = 0;
        turnCounter = 0;
        endedWork.putAll(seed.endedWork());
        for (var resource : seed.chats()) {
            var chat = new Chat(resource.id(),
                    new Routing(resource.subject(), resource.role(), resource.scope(),
                            seededPlanId(resource.subject(), resource.scope())),
                    resource.state(), resource.context(), resource.proposal(), resource.turns());
            usedChatIds.add(chat.id);
            chat.turns.forEach(turn -> usedTurnIds.add(turn.turnId()));
            if (resource.proposal() != null) {
                usedTurnIds.add(resource.proposal().turnId());
                chat.proposalPlanVersion = resource.proposal().kind() == ProposalKind.REVISION
                        ? planVersionOf(chat) : null;
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
        var chat = new Chat(nextChatId(),
                new Routing(address.subject(), new RoutedRole(address.role(), DEFAULT_MODEL), address.scope(),
                        address.planId()),
                ChatStatus.TURN, context(address), null, List.of());
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
        return recordTurn(address, message);
    }

    private synchronized Outcome recordTurn(Address address, ChatTurnRequest message) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        var repeated = chat.clientTurns.get(message.clientTurnId());
        if (repeated != null) {
            return Outcome.of(ChatTurnAccepted.HTTP_STATUS, new ChatTurnAccepted(repeated).toMap());
        }
        if (chat.state.isEnded()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, MESSAGE_CHAT_ENDED);
        }
        if (chat.state == ChatStatus.REPLY || chat.play != null) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "A reply is being written");
        }
        var at = Instant.now().toString();
        var operatorTurn = operatorTurn(nextTurnId(), false, message.text(), at);
        chat.turns.add(operatorTurn);
        chat.clientTurns.put(message.clientTurnId(), operatorTurn.turnId());
        var operatorStarted = event(address, ChatTurnStartedEvent.EVENT_NAME, new ChatTurnStartedEvent(chat.id,
                operatorTurn.turnId(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnFormat.PLAIN, false, null,
                null, message.text()).toMap());

        var script = ChatScripts.reply(chat.subject.kind(), chat.subject.id(), chat.scope, chat.replyCount());
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
            chat.context = context(play.address());
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
            var refs = offeredRefs(chat.subject, script.refs());
            chat.updateReply(play.turnId(), turn -> withCompletion(turn, text, refs));
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
            // The subject's narrative waited for the reply to end; it is written after it, as one notice.
            events.addAll(flushHeldLines(chat, play.address()));
            return events;
        }));
    }

    /**
     * Writes the narrative lines that waited for the reply being written to end as one notice turn
     * after it ({@code state-catalogue.adoc#chat-narrative}: a line that arises meanwhile is held until
     * the reply ends).
     *
     * @param chat    the chat
     * @param address where the chat is addressed
     * @return the {@code chat.turn.completed} of the notice, or no event when nothing was held
     */
    private List<ChatEvent> flushHeldLines(Chat chat, Address address) {
        if (chat.heldLines.isEmpty() || chat.state.isEnded()) {
            return List.of();
        }
        var lines = new ArrayList<>(chat.heldLines);
        chat.heldLines.clear();
        return notice(chat, address, lines);
    }

    /**
     * @param subject the chat's subject
     * @param refs    the references a scripted reply names
     * @return the references, without an act the subject does not offer
     *         ({@code state-catalogue.adoc#chat-names-acts})
     */
    private List<TurnRef> offeredRefs(ChatSubject subject, List<TurnRef> refs) {
        return refs.stream()
                .filter(ref -> ref.kind() != RefKind.ACT || facts.offersAct(Objects.requireNonNull(ref.subject()),
                        Objects.requireNonNull(ref.act())))
                .toList();
    }

    /**
     * Writes the subject's narrative lines as one notice turn between two replies
     * ({@code state-catalogue.adoc#chat-narrative}): a line that arrives between two replies extends
     * the notice that already stands at the transcript's end, so several lines that come at once are
     * one notice; a new one is a turn of its own.
     *
     * @param chat    the chat
     * @param address where the chat is addressed
     * @param lines   the lines to write
     * @return the {@code chat.turn.completed} of the notice
     */
    private List<ChatEvent> notice(Chat chat, Address address, List<NarrativeLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        var last = chat.turns.isEmpty() ? null : chat.turns.getLast();
        ChatTurn noticeTurn;
        if (last != null && last.kind() == TurnKind.NOTICE) {
            var extended = new ArrayList<>(last.lines());
            extended.addAll(lines);
            noticeTurn = new ChatTurn(last.turnId(), TurnKind.NOTICE, last.author(), TurnStatus.COMPLETE,
                    TurnFormat.PLAIN, false, null, "", List.of(), List.of(), last.at(), null, extended);
            chat.turns.set(chat.turns.size() - 1, noticeTurn);
        } else {
            noticeTurn = new ChatTurn(nextTurnId(), TurnKind.NOTICE, address.role(), TurnStatus.COMPLETE,
                    TurnFormat.PLAIN, false, null, "", List.of(), List.of(), Instant.now().toString(), null,
                    lines);
            chat.turns.add(noticeTurn);
        }
        return List.of(event(address, ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(chat.id,
                noticeTurn.turnId(), TurnKind.NOTICE, TurnStatus.COMPLETE, false, "", List.of(), null,
                noticeTurn.lines()).toMap()));
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
        chat.proposalPlanVersion = scripted.kind() == ProposalKind.REVISION ? planVersionOf(chat) : null;
        events.add(proposalEvent(chat, address.stream(), chat.proposal));
        return events;
    }

    /**
     * @param chat the chat
     * @return the instance version of the plan the chat's revisions belong to, or {@code null}
     */
    private @Nullable Integer planVersionOf(Chat chat) {
        return chat.planId == null ? null : facts.instanceVersion(chat.planId);
    }

    private static ChatProposal withState(ChatProposal proposal, ProposalState state) {
        return new ChatProposal(proposal.turnId(), proposal.replyTurnId(), proposal.kind(), proposal.summary(),
                proposal.applies(), proposal.before(), proposal.after(), state, null);
    }

    private static ChatEvent proposalEvent(Chat chat, EventScope stream, ChatProposal proposal) {
        return new ChatEvent(ChatProposalEvent.EVENT_NAME, stream, new ChatProposalEvent(chat.id, proposal.turnId(),
                proposal.replyTurnId(), proposal.kind(), proposal.state(), proposal.finding()).toMap());
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
            Play restart;
            synchronized (this) {
                // The check and the replacement are one step: a stop that lands between them would
                // restart a reply that is no longer being written.
                restart = isCurrent(chat, play) ? restartWorker(chat) : null;
            }
            if (restart != null) {
                playReply(chat, restart);
            }
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
            var events = new ArrayList<ChatEvent>();
            events.add(event(play.address(), ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(
                    chat.id, turn.turnId(), turn.kind(), status, turn.untrusted(), turn.text(), turn.refs(), reason,
                    List.of()).toMap()));
            // The reply ended, failed or stopped: the lines held for it are written after it.
            events.addAll(flushHeldLines(chat, play.address()));
            return events;
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
     * request is answered no earlier than its delay.
     *
     * @return {@code true} if the failure answered the request
     */
    private static boolean playRequestFailure(ChatFailure failure, MockHttpResponse response) throws IOException {
        if (failure.kind() == ChatFailure.Kind.HTTP_ERROR) {
            response.json(failure.status(), Map.of(KEY_ERROR, Objects.requireNonNull(failure.code()), KEY_MESSAGE,
                    "Injected failure: " + failure.code()));
            return true;
        }
        // parkNanos may return early, so the wait runs to its deadline.
        var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(failure.delayMs());
        for (var left = deadline - System.nanoTime(); left > 0; left = deadline - System.nanoTime()) {
            LockSupport.parkNanos(left);
        }
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
        Play restart;
        synchronized (this) {
            chat = chats.get(subject);
            if (chat == null || chat.play == null) {
                return false;
            }
            restart = restartWorker(chat);
        }
        playReply(chat, restart);
        return true;
    }

    /**
     * Cancels the play of a chat's reply and starts it again from the working line, with the reply's
     * text and steps cleared so two attempts never read as one.
     *
     * @param chat the chat whose reply is being written
     * @return the play that writes the reply anew
     */
    private Play restartWorker(Chat chat) {
        var play = Objects.requireNonNull(chat.play);
        var turnId = play.turnId();
        var address = play.address();
        var script = play.script();
        chat.cancelPlay();
        chat.updateReply(turnId, turn -> new ChatTurn(turn.turnId(), turn.kind(), turn.author(),
                TurnStatus.STREAMING, turn.format(), turn.untrusted(), turn.model(), "", List.of(),
                turn.refs(), turn.at(), null, turn.lines()));
        return chat.startPlay(address, turnId, null, script);
    }

    /**
     * Reads one event of the runtime for the chats it concerns: it ends the chats an event ends (a
     * decision answered elsewhere, a plan or an epic that ended, an entry created), deletes the
     * transcript of an abandoned entry ({@code starting-work.adoc}, The transcript), writes the
     * subject's narrative into the chat of its subject as notice turns
     * ({@code state-catalogue.adoc#chat-narrative}), and moves a plan's queued revision on at its task
     * boundary or closes it with the plan's revision window
     * ({@code state-catalogue.adoc#chat-proposal-states}). The chat's own events are not observed.
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
                var answeredDecision = decisionId(payload);
                if (answeredDecision != null) {
                    endWhere(subject -> subject.kind() == ChatSubjectKind.DECISION
                                    && subject.id().endsWith("/" + answeredDecision),
                            ChatEndedEvent.EndedBy.ANSWERED_ELSEWHERE);
                }
            }
            case EVENT_SCOPE_ENDED -> {
                var outcome = JsonTree.string(payload, ARG_OUTCOME);
                endWork(ChatSubjectKind.PLAN, JsonTree.string(payload, KEY_PLAN_ID), outcome);
                endWork(ChatSubjectKind.EPIC, JsonTree.string(payload, KEY_EPIC_ID), outcome);
            }
            case EVENT_ENTRY_ABANDONED -> {
                // The entry's transcript goes with its source directory: abandoning deletes it.
                var entryId = JsonTree.string(payload, KEY_ENTRY_ID);
                if (entryId != null) {
                    deleteWhere(new ChatSubject(ChatSubjectKind.ENTRY, entryId)::equals);
                }
            }
            case EVENT_PLAN_STARTED, EVENT_EPIC_STARTED -> {
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
        var events = new ArrayList<ChatEvent>();
        events.addAll(revisionAtBoundary(event, payload));
        events.addAll(narrate(event, payload));
        events.forEach(broadcaster);
    }

    /**
     * Deletes the chats a predicate names, with their transcripts, as abandoning an entry does. A play
     * being written is cancelled first.
     *
     * @param subjects the subjects whose chats are deleted
     */
    private void deleteWhere(Predicate<ChatSubject> subjects) {
        synchronized (this) {
            for (var chat : List.copyOf(chats.values())) {
                if (subjects.test(chat.subject)) {
                    chat.cancelPlay();
                    chats.remove(chat.subject);
                }
            }
        }
    }

    /**
     * Writes the subject's narrative into the chat of its subject: one line per event the closed
     * catalogue derives, each a message key {@code chat.line.*} with its arguments and the references
     * it names, held until the reply being written ends and written as one notice
     * ({@code state-catalogue.adoc#chat-narrative}). Ended work has no narrative, since its archive
     * never changes.
     *
     * @param event   the event's name
     * @param payload the event's payload
     * @return the {@code chat.turn.completed} events of the notices written
     */
    private List<ChatEvent> narrate(String event, Map<String, Object> payload) {
        var at = Instant.now().toString();
        var events = new ArrayList<ChatEvent>();
        synchronized (this) {
            for (var chat : chats.values()) {
                var line = narrativeLine(event, payload, chat, at);
                if (line == null) {
                    continue;
                }
                if (chat.play != null || chat.state == ChatStatus.REPLY) {
                    chat.heldLines.add(line);
                    continue;
                }
                events.addAll(notice(chat, streamAddress(chat), List.of(line)));
            }
        }
        return events;
    }

    /**
     * @param chat the chat the line would be written into
     * @return an address the chat's events can be scoped with
     */
    private static Address streamAddress(Chat chat) {
        return new Address(null, chat.subject, chat.scope, chat.role.name(), streamOf(chat), chat.planId);
    }

    /**
     * Derives the narrative line one event is for one chat, or {@code null} where the event is about
     * no subject of that chat or the catalogue holds no line for it.
     *
     * @param event the event's name
     * @param payload the event's payload
     * @param chat  the chat
     * @param at    the instant of the event
     * @return the line, or {@code null}
     */
    private @Nullable NarrativeLine narrativeLine(String event, Map<String, Object> payload, Chat chat,
            String at) {
        if (chat.scope == ChatScopeKind.ARCHIVE || chat.state.isEnded()) {
            return null;
        }
        return switch (event) {
            case EVENT_PHASE_CHANGED -> about(chat, planOrEpic(payload),
                    line("phase_changed", args(KEY_PHASE, JsonTree.string(payload, KEY_PHASE)), List.of(), at));
            case EVENT_TASK_ENDED -> about(chat, planOrEpic(payload), taskEndedLine(payload, at));
            case EVENT_DECISION_OPENED -> about(chat, planOrEpic(payload), new NarrativeLine(
                    LINE_PREFIX + "decision_opened", args2(KEY_ID, decisionId(payload), "blocks",
                    blocksThePlan(payload) ? "yes" : "no"),
                    ref(RefKind.DECISION, decisionId(payload)), at));
            case EVENT_DECISION_ANSWERED -> about(chat, planOrEpic(payload), decisionAnsweredLine(payload, at));
            case EVENT_DECISION_CLOSED -> about(chat, planOrEpic(payload), new NarrativeLine(
                    LINE_PREFIX + "decision_closed", args2(KEY_ID, decisionId(payload), "reason",
                    JsonTree.string(payload, "outcome")), ref(RefKind.DECISION, decisionId(payload)),
                    at));
            case EVENT_HOLD_OPENED -> about(chat, planOrEpic(payload),
                    holdLine("hold_opened", payload, at));
            case EVENT_HOLD_CLEARED -> about(chat, planOrEpic(payload),
                    holdLine("hold_cleared", payload, at));
            case EVENT_ACTIVITY_CHANGED -> about(chat, planOrEpic(payload), activityLine(payload, at));
            case EVENT_INSTANCE_CHANGED -> about(chat, planOrEpic(payload), versionLine(payload, at));
            default -> null;
        };
    }

    /**
     * @param chat    the chat
     * @param subject the event's subject, or {@code null}
     * @param line    the line, or {@code null}
     * @return the line when the event is about the chat's subject, else {@code null}
     */
    private static @Nullable NarrativeLine about(Chat chat, @Nullable ChatSubject subject,
            @Nullable NarrativeLine line) {
        if (line == null || subject == null) {
            return null;
        }
        var aboutTheSubject = chat.subject.equals(subject)
                || (chat.subject.kind() == ChatSubjectKind.DECISION
                && subject.kind() == ChatSubjectKind.PLAN
                && chat.subject.id().startsWith(subject.id() + "/"));
        return aboutTheSubject ? line : null;
    }

    /**
     * @param payload the event's payload
     * @return the plan or the epic the event is about: the id its payload names, else the subject its
     *         declared scope names, else its {@code subject} record
     */
    private static @Nullable ChatSubject planOrEpic(Map<String, Object> payload) {
        var planId = JsonTree.string(payload, KEY_PLAN_ID);
        if (planId == null) {
            planId = scopedId(payload, "plan");
        }
        if (planId != null) {
            return new ChatSubject(ChatSubjectKind.PLAN, planId);
        }
        var epicId = JsonTree.string(payload, KEY_EPIC_ID);
        if (epicId == null) {
            epicId = scopedId(payload, "epic");
        }
        return epicId == null ? null : new ChatSubject(ChatSubjectKind.EPIC, epicId);
    }

    /**
     * @param payload the event's payload
     * @param kind    the scope kind to read
     * @return the id the payload's declared scope names for that kind, or {@code null}
     */
    private static @Nullable String scopedId(Map<String, Object> payload, String kind) {
        var scope = JsonTree.asObject(payload.get("scope"));
        if (scope == null || !kind.equals(JsonTree.string(scope, "kind"))) {
            return null;
        }
        var id = JsonTree.string(scope, "id");
        return id != null ? id : JsonTree.string(scope, "project_id");
    }

    /**
     * @param payload the event's payload
     * @return the decision's id as the event names it, or {@code null}
     */
    private static @Nullable String decisionId(Map<String, Object> payload) {
        var id = JsonTree.string(payload, KEY_ID);
        if (id == null) {
            id = JsonTree.string(payload, "decision_id");
        }
        if (id == null) {
            var decision = JsonTree.asObject(payload.get("decision"));
            id = decision == null ? null : JsonTree.string(decision, KEY_ID);
        }
        return id;
    }

    private static NarrativeLine taskEndedLine(Map<String, Object> payload, String at) {
        var taskId = Objects.requireNonNullElse(JsonTree.string(payload, KEY_TASK_ID),
                Objects.requireNonNullElse(JsonTree.string(payload, KEY_ID), ""));
        var status = Objects.requireNonNullElse(JsonTree.string(payload, "status"),
                Objects.requireNonNullElse(JsonTree.string(payload, ARG_OUTCOME), "done"));
        var outcome = "done".equals(status) || "success".equals(status) || "merged".equals(status) ? "done"
                : "failed".equals(status) ? "failed" : "done";
        return new NarrativeLine(LINE_PREFIX + "task_ended", args2(KEY_TASK_ID, taskId, ARG_OUTCOME, outcome),
                ref(RefKind.TASK, taskId), at);
    }

    private static NarrativeLine decisionAnsweredLine(Map<String, Object> payload, String at) {
        var id = Objects.requireNonNullElse(decisionId(payload), "");
        var channel = JsonTree.string(payload, "channel");
        var device = JsonTree.string(payload, "device");
        var args = new LinkedHashMap<String, Object>();
        args.put(KEY_ID, id);
        if (channel != null) {
            args.put("channel", channel);
        }
        if (device != null) {
            args.put("device", device);
        }
        return new NarrativeLine(LINE_PREFIX + "decision_answered", args, ref(RefKind.DECISION, id), at);
    }

    /**
     * @param kind a reference's kind
     * @param id   the referenced id, may be {@code null} or blank
     * @return one reference, or none where the event named no id
     */
    private static List<TurnRef> ref(RefKind kind, @Nullable String id) {
        return id == null || id.isBlank() ? List.of() : List.of(TurnRef.of(kind, id));
    }

    private static NarrativeLine holdLine(String name, Map<String, Object> payload, String at) {
        var id = Objects.requireNonNullElse(JsonTree.string(payload, KEY_HOLD_ID),
                Objects.requireNonNullElse(JsonTree.string(payload, KEY_ID), ""));
        var reason = Objects.requireNonNullElse(JsonTree.string(payload, "reason"), "");
        return new NarrativeLine(LINE_PREFIX + name, args("reason", reason),
                id.isBlank() ? List.of() : List.of(TurnRef.of(RefKind.HOLD, id)), at);
    }

    private @Nullable NarrativeLine activityLine(Map<String, Object> payload, String at) {
        var planId = Objects.requireNonNullElse(JsonTree.string(payload, KEY_PLAN_ID), "");
        var activity = Objects.requireNonNullElse(JsonTree.string(payload, KEY_ACTIVITY), "");
        var previous = lastActivities.get(planId);
        lastActivities.put(planId, activity);
        if (payload.containsKey(INTERNAL_KEY)) {
            // The registry's own announcement of the plan's row: no narrative line.
            return null;
        }
        if (activity.equals(previous)) {
            // The plan's activity was reported as it already was: no line, whatever the event says.
            return null;
        }
        var channel = JsonTree.string(payload, "channel");
        var device = JsonTree.string(payload, "device");
        var args = new LinkedHashMap<String, Object>();
        args.put("plan", planId);
        if (channel != null) {
            args.put("channel", channel);
        }
        if (device != null) {
            args.put("device", device);
        }
        return switch (activity) {
            case ACTIVITY_PAUSING -> new NarrativeLine(LINE_PREFIX + "plan_pausing",
                    args2("plan", planId, "point",
                            Objects.requireNonNullElse(JsonTree.string(payload, "point"), "task")), List.of(), at);
            case ACTIVITY_PAUSED -> new NarrativeLine(LINE_PREFIX + "plan_paused", args, List.of(), at);
            case ACTIVITY_RUNNING -> new NarrativeLine(LINE_PREFIX + "plan_started", args, List.of(), at);
            default -> null;
        };
    }

    private static @Nullable NarrativeLine versionLine(Map<String, Object> payload, String at) {
        var version = JsonTree.integer(payload, "instance_version");
        if (version == null) {
            version = JsonTree.integer(payload, KEY_VERSION);
        }
        if (version == null) {
            return null;
        }
        var cause = Objects.requireNonNullElse(JsonTree.string(payload, "cause"), "other");
        return new NarrativeLine(LINE_PREFIX + "version_changed",
                args2(KEY_VERSION, String.valueOf(version), "cause", cause), List.of(), at);
    }

    /**
     * @param payload the event's payload
     * @return whether a decision the line names blocks the plan
     */
    private static boolean blocksThePlan(Map<String, Object> payload) {
        var blocks = JsonTree.string(payload, "blocks");
        return blocks != null && !blocks.isBlank() && !"no".equals(blocks);
    }

    private static NarrativeLine line(String name, Map<String, Object> args, List<TurnRef> refs, String at) {
        return new NarrativeLine(LINE_PREFIX + name, args, refs, at);
    }

    private static Map<String, Object> args(String name, @Nullable String value) {
        return value == null ? Map.of() : Map.of(name, value);
    }

    private static Map<String, Object> args2(String firstName, @Nullable String first, String secondName,
            @Nullable String second) {
        var args = new LinkedHashMap<String, Object>();
        if (first != null) {
            args.put(firstName, first);
        }
        if (second != null) {
            args.put(secondName, second);
        }
        return args;
    }

    /**
     * Moves the plan's queued revision on at its boundary, or closes it with the plan's revision
     * window: applied at the task boundary of the task it waits for, or when a paused plan starts
     * again; refused at that boundary where the plan moved on meanwhile; lapsed where the plan left
     * the revision window first, which wins where both hold
     * ({@code state-catalogue.adoc#chat-proposal-states}).
     *
     * @param event   the event's name
     * @param payload the event's payload
     * @return the events of the queue leaving
     */
    private List<ChatEvent> revisionAtBoundary(String event, Map<String, Object> payload) {
        var planId = JsonTree.string(payload, KEY_PLAN_ID);
        if (planId == null) {
            return List.of();
        }
        var events = new ArrayList<ChatEvent>();
        synchronized (this) {
            var queued = facts.queuedRevision(planId);
            var windowClosed = EVENT_SCOPE_ENDED.equals(event) || !facts.revisionWindowOpen(planId);
            if (windowClosed) {
                if (queued != null) {
                    queuedBaseVersions.remove(planId);
                    facts.clearRevision(planId);
                    events.add(revisionQueued(planId, null));
                    events.addAll(proposalOfChat(queued.chatId(), ProposalState.LAPSED, null));
                }
                events.addAll(lapseOpenRevisions(planId));
                return events;
            }
            if (queued == null) {
                return events;
            }
            var boundary = EVENT_TASK_ENDED.equals(event)
                    && Objects.equals(appliesTask(queued), JsonTree.string(payload, KEY_TASK_ID))
                    || EVENT_PLAN_STARTED.equals(event) && appliesOnStart(queued);
            if (!boundary) {
                return events;
            }
            events.addAll(settle(planId, queued));
        }
        return events;
    }

    /**
     * Settles a queued revision at its boundary: applied where it still passes the plan's guards, its
     * new instance version recorded with the change's origin, refused where the plan moved on
     * meanwhile.
     *
     * @param planId the plan's id
     * @param queued the queued revision
     * @return the events of its application or refusal
     */
    private List<ChatEvent> settle(String planId, QueuedRevision queued) {
        var events = new ArrayList<ChatEvent>();
        var base = queuedBaseVersions.remove(planId);
        var current = facts.instanceVersion(planId);
        if (base != null && base != current) {
            facts.clearRevision(planId);
            events.add(revisionQueued(planId, null));
            events.addAll(proposalOfChat(queued.chatId(), ProposalState.REFUSED, MessageText.of(FINDING_CHECK_FAILED)));
            return events;
        }
        var origin = new ChatOrigin(queued.chatId(), queued.turnId());
        facts.applyRevision(planId, origin);
        events.add(revisionQueued(planId, null));
        events.add(activityChanged(planId, ACTIVITY_RUNNING, null));
        events.addAll(proposalOfChat(queued.chatId(), ProposalState.APPLIED, null));
        events.addAll(narrateVersion(planId, current + 1));
        return events;
    }

    /**
     * Writes the plan's new version into its chat as a narrative line, held where a reply is being
     * written.
     *
     * @param planId  the plan's id
     * @param version the plan's new instance version
     * @return the notice events
     */
    private List<ChatEvent> narrateVersion(String planId, int version) {
        var events = new ArrayList<ChatEvent>();
        for (var chat : chats.values()) {
            if (chat.scope == ChatScopeKind.ARCHIVE || !new ChatSubject(ChatSubjectKind.PLAN, planId)
                    .equals(chat.subject)) {
                continue;
            }
            var line = new NarrativeLine(LINE_PREFIX + "version_changed",
                    args2(KEY_VERSION, String.valueOf(version), "cause", "chat"), List.of(),
                    Instant.now().toString());
            if (chat.play != null || chat.state == ChatStatus.REPLY) {
                chat.heldLines.add(line);
            } else {
                events.addAll(notice(chat, streamAddress(chat), List.of(line)));
            }
        }
        return events;
    }

    /**
     * Makes every open revision proposal of a plan {@code lapsed}, since the plan left the window.
     *
     * @param planId the plan's id
     * @return the {@code chat.proposal} events of the proposals
     */
    private List<ChatEvent> lapseOpenRevisions(String planId) {
        var events = new ArrayList<ChatEvent>();
        for (var chat : chats.values()) {
            var proposal = chat.proposal;
            if (chat.scope == ChatScopeKind.ARCHIVE || !planId.equals(chat.planId) || proposal == null
                    || !proposal.isOpen() || proposal.kind() != ProposalKind.REVISION) {
                continue;
            }
            chat.proposal = withState(proposal, ProposalState.LAPSED);
            events.add(proposalEvent(chat, streamOf(chat), chat.proposal));
        }
        return events;
    }

    /**
     * @param chatId the chat that proposed a queued revision
     * @return the events that answer its proposal, or none where that chat no longer holds it
     */
    private List<ChatEvent> proposalOfChat(String chatId, ProposalState state, @Nullable MessageText finding) {
        for (var chat : chats.values()) {
            if (!chat.id.equals(chatId) || chat.proposal == null) {
                continue;
            }
            chat.proposal = new ChatProposal(chat.proposal.turnId(), chat.proposal.replyTurnId(),
                    chat.proposal.kind(), chat.proposal.summary(), chat.proposal.applies(),
                    chat.proposal.before(), chat.proposal.after(), state, finding);
            return List.of(proposalEvent(chat, streamOf(chat), chat.proposal));
        }
        return List.of();
    }

    /**
     * @param planId   the plan's id
     * @param revision the plan's queued revision, or {@code null} when none is queued
     * @return the {@code plan.revision_queued} event
     */
    private static ChatEvent revisionQueued(String planId, @Nullable QueuedRevision revision) {
        return new ChatEvent(PlanRevisionQueuedEvent.EVENT_NAME,
                new EventScope(EventScopeKind.PLAN, planId, null),
                new PlanRevisionQueuedEvent(planId, revision).toMap());
    }

    private static ChatEvent activityChanged(String planId, String activity, @Nullable String point) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put(KEY_PLAN_ID, planId);
        payload.put(KEY_ACTIVITY, activity);
        payload.put(INTERNAL_KEY, true);
        if (point != null) {
            payload.put("point", point);
        }
        return new ChatEvent(EVENT_ACTIVITY_CHANGED, new EventScope(EventScopeKind.PLAN, planId, null), payload);
    }

    /**
     * @param revision the queued revision
     * @return the task it waits for, or {@code null} where it waits for the plan's start
     */
    private static @Nullable String appliesTask(QueuedRevision revision) {
        return revision.applies().args().get(KEY_TASK_ID) instanceof String taskId ? taskId : null;
    }

    /**
     * @param revision the queued revision
     * @return whether it waits for a paused plan to start again
     */
    private static boolean appliesOnStart(QueuedRevision revision) {
        return revision.applies().key().endsWith("on_start");
    }

    private void endWork(ChatSubjectKind kind, @Nullable String id, @Nullable String outcome) {
        if (id == null) {
            return;
        }
        var subject = new ChatSubject(kind, id);
        synchronized (this) {
            endedWork.put(subject, outcome);
        }
        endWhere(subject::equals, ChatEndedEvent.EndedBy.SUBJECT_ENDED);
    }

    private void endWhere(Predicate<ChatSubject> subjects, ChatEndedEvent.EndedBy endedBy) {
        var events = new ArrayList<ChatEvent>();
        synchronized (this) {
            for (var chat : chats.values()) {
                if (subjects.test(chat.subject) && !chat.state.isEnded()) {
                    events.addAll(endChat(chat, ChatStatus.END, endedBy));
                }
            }
        }
        events.forEach(broadcaster);
    }

    /**
     * Ends a chat: a reply being written, played or seeded, stops where it is, and {@code chat.ended}
     * names what ended it. The chat that proposed a plan's queued revision turns the revision's
     * {@code chat_open} false, which is announced by {@code plan.revision_queued}
     * ({@code 06-event-catalogue.adoc}).
     *
     * @return the chat's {@code chat.ended} event, and the {@code plan.revision_queued} of a queued
     *         revision the chat proposed
     */
    private List<ChatEvent> endChat(Chat chat, ChatStatus state, ChatEndedEvent.EndedBy endedBy) {
        chat.cancelPlay();
        // An ended chat has no transcript to write the held narrative into.
        chat.heldLines.clear();
        chat.turns.replaceAll(turn -> turn.status() == TurnStatus.STREAMING
                ? withText(turn, turn.text(), TurnStatus.STOPPED)
                : turn);
        chat.state = state;
        var events = new ArrayList<ChatEvent>();
        events.add(new ChatEvent(ChatEndedEvent.EVENT_NAME, streamOf(chat),
                new ChatEndedEvent(chat.id, endedBy, null, null).toMap()));
        var planId = chat.planId;
        if (planId != null) {
            var queued = facts.queuedRevision(planId);
            if (queued != null && queued.chatId().equals(chat.id)) {
                facts.revisionChatEnded(planId);
                events.add(revisionQueued(planId, new QueuedRevision(queued.chatId(), queued.turnId(),
                        queued.subject(), false, queued.applies())));
            }
        }
        return List.copyOf(events);
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
            default -> new EventScope(
                    id.startsWith(MACHINE_SCOPE_ID + "/") ? EventScopeKind.MACHINE : EventScopeKind.RUNTIME, null,
                    null);
        };
    }

    /**
     * The operator's own message as a turn of the transcript: complete, plain, on no model and with no
     * steps, references or reason.
     *
     * @param turnId    the turn's id
     * @param untrusted whether the message came from outside the machine
     * @param text      the message's text
     * @param at        the instant of the message, ISO-8601
     * @return the turn
     */
    private static ChatTurn operatorTurn(String turnId, boolean untrusted, String text, String at) {
        return new ChatTurn(turnId, TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR, TurnStatus.COMPLETE,
                TurnFormat.PLAIN, untrusted, null, text, List.of(), List.of(), at, null, List.of());
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
        // An ended chat stays ended, and a reply still being played keeps the chat in chat.reply.
        if (!chat.state.isEnded() && chat.play == null) {
            chat.state = chat.idleState();
        }
        var event = event(address, ChatTurnCompletedEvent.EVENT_NAME, new ChatTurnCompletedEvent(chat.id,
                stopped.turnId(), stopped.kind(), TurnStatus.STOPPED, stopped.untrusted(), stopped.text(),
                stopped.refs(), null, List.of()).toMap());
        var events = new ArrayList<ChatEvent>();
        events.add(event);
        // The reply ended: the narrative lines that waited for it are written after it.
        events.addAll(flushHeldLines(chat, address));
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.copyOf(events), null);
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
        if (accept && proposal.kind() == ProposalKind.REVISION && proposal.applies() == null) {
            return refused(ApiErrorCode.INVALID_ARGUMENTS, "The revision names no point it applies at");
        }
        if (accept && proposal.kind() == ProposalKind.REVISION && chat.planId == null) {
            // Another subject's proposal is refused by its own checks: no plan means no revision window.
            chat.proposal = new ChatProposal(proposal.turnId(), proposal.replyTurnId(), proposal.kind(),
                    proposal.summary(), proposal.applies(), proposal.before(), proposal.after(),
                    ProposalState.REFUSED, MessageText.of(FINDING_CHECK_FAILED));
            return new Outcome(200, chat.toResource(chat.turns.size()).toMap(),
                    List.of(proposalEvent(chat, address.stream(), chat.proposal)), null);
        }
        if (accept && proposal.kind() == ProposalKind.REVISION && chat.planId != null) {
            var refusal = revisionRefusal(chat, proposal);
            if (refusal != null) {
                return refusal;
            }
        }
        var state = accept ? acceptedState(proposal.kind()) : ProposalState.REJECTED;
        chat.proposal = withState(proposal, state);
        var answered = proposalEvent(chat, address.stream(), chat.proposal);
        if (accept && proposal.kind() == ProposalKind.REVISION && chat.planId != null) {
            // A plan's revision is accepted and queued for the plan's next task boundary.
            return queued(chat, address, answered);
        }
        if (accept && chat.subject.kind() == ChatSubjectKind.DECISION) {
            // A decision's chat carries the accepted proposal to the decision and ends with it.
            var events = new ArrayList<>(endChat(chat, ChatStatus.CARRIED, ChatEndedEvent.EndedBy.PROPOSAL_CARRIED));
            events.addFirst(answered);
            return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.copyOf(events), null);
        }
        // A reply still being written keeps the chat in chat.reply; it takes the turn when the reply ends.
        if (chat.state != ChatStatus.REPLY) {
            chat.state = ChatStatus.TURN;
        }
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(answered),
                applyHook(chat.subject, chat.proposal));
    }

    /**
     * Decides whether the accept of a plan's revision is refused or lapses it
     * ({@code state-catalogue.adoc#chat-proposal-states}): a proposal whose plan left the revision
     * window is {@code lapsed}, which wins where both hold since the window is closed; an accept while
     * another revision of that plan is queued is refused {@code conflict}, and so is one made against
     * an older instance version of the plan. The proposal stays open where it is refused, so its
     * reject is still taken.
     *
     * @param chat     the chat
     * @param proposal the open revision proposal
     * @return the refusal, or {@code null} when the accept goes through
     */
    private @Nullable Outcome revisionRefusal(Chat chat, ChatProposal proposal) {
        var planId = Objects.requireNonNull(chat.planId);
        if (!facts.revisionWindowOpen(planId)) {
            chat.proposal = withState(proposal, ProposalState.LAPSED);
            var lapsed = proposalEvent(chat, streamOf(chat), chat.proposal);
            return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), List.of(lapsed), null);
        }
        if (facts.queuedRevision(planId) != null) {
            return refused(ApiErrorCode.CONFLICT, FINDING_ALREADY_QUEUED);
        }
        var madeAgainst = chat.proposalPlanVersion;
        if (madeAgainst != null && madeAgainst != facts.instanceVersion(planId)) {
            return refused(ApiErrorCode.CONFLICT, FINDING_CHANGED_SINCE);
        }
        return null;
    }

    /**
     * Queues the accepted revision as the plan's {@code queued_revision} and announces it
     * ({@code plan.revision_queued}), the plan's row and activity moving with it.
     *
     * @param chat     the chat
     * @param address  where the chat is addressed
     * @param answered the {@code chat.proposal} of the accepted proposal
     * @return the answer
     */
    private Outcome queued(Chat chat, Address address, ChatEvent answered) {
        var planId = Objects.requireNonNull(chat.planId);
        var proposal = Objects.requireNonNull(chat.proposal);
        var revision = new QueuedRevision(chat.id, proposal.turnId(), chat.subject, true,
                Objects.requireNonNull(proposal.applies()));
        facts.queueRevision(planId, revision);
        var base = Objects.requireNonNullElse(chat.proposalPlanVersion, facts.instanceVersion(planId));
        queuedBaseVersions.put(planId, base);
        if (chat.state != ChatStatus.REPLY) {
            chat.state = ChatStatus.TURN;
        }
        var events = List.of(answered, revisionQueued(planId, revision),
                activityChanged(planId, ACTIVITY_RUNNING, null));
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), events, null);
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
     * ends the chat; {@code chat.turn.progress}, {@code chat.turn.delta} and {@code chat.turn.completed}
     * are sent as they are. Every payload is read through its event's {@code pm-api} record first: a
     * payload the record refuses, or a {@code chat.*} name no chat event has, is refused
     * {@code invalid_arguments}, a chat nobody opened {@code target_not_found}, and an act the chat no
     * longer offers {@code act_not_available}.
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
        var parsed = parseChatEvent(event, payload);
        var chatId = JsonTree.string(payload, ChatEndedEvent.FIELD_CHAT_ID);
        var chat = chats.values().stream().filter(candidate -> candidate.id.equals(chatId)).findFirst().orElse(null);
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, "No chat " + chatId);
        }
        if (parsed instanceof ChatTurnCompletedEvent completed) {
            requireOfferedActs(completed.refs());
        }
        if (parsed instanceof ChatTurnStartedEvent started) {
            return broadcastTurnStarted(chat, started, event, payload);
        }
        if (parsed instanceof ChatProposalEvent answered) {
            return broadcastProposal(chat, answered, event, payload);
        }
        if (parsed instanceof ChatEndedEvent ended) {
            return broadcastEnded(chat, ended, event, payload);
        }
        return broadcasted(chat, event, payload, null);
    }

    /**
     * Refuses a submitted turn that names an operator act its subject does not offer
     * ({@code state-catalogue.adoc#chat-names-acts}: the act is one of those the clients offer on that
     * subject, checked by the server at submit).
     *
     * @param refs the references a submitted turn names
     * @throws IOException if one of them names an act the subject does not offer
     */
    private void requireOfferedActs(List<TurnRef> refs) throws IOException {
        for (var ref : refs) {
            if (ref.kind() == RefKind.ACT) {
                var subject = Objects.requireNonNull(ref.subject());
                var act = Objects.requireNonNull(ref.act());
                if (!facts.offersAct(subject, act)) {
                    throw new IOException("The subject " + subject.kind().wireValue() + " '" + subject.id()
                            + "' offers no act " + act);
                }
            }
        }
    }

    /**
     * Applies a broadcast {@code chat.turn.started}: an operator's message is recorded in the chat, a
     * role's turn is sent as it is. Call under the lock.
     */
    private Outcome broadcastTurnStarted(Chat chat, ChatTurnStartedEvent started, String event,
            Map<String, Object> payload) {
        if (started.isOperator()) {
            if (chat.state.isEnded() || chat.state == ChatStatus.REPLY || chat.play != null) {
                return refused(ApiErrorCode.ACT_NOT_AVAILABLE, "The chat takes no message now");
            }
            if (!usedTurnIds.add(started.turnId())) {
                return refused(ApiErrorCode.CONFLICT, "The turn " + started.turnId() + " exists");
            }
            var startedText = started.text();
            var text = startedText != null ? startedText : "";
            chat.turns.add(operatorTurn(started.turnId(), started.untrusted(), text, Instant.now().toString()));
        }
        return broadcasted(chat, event, payload, null);
    }

    /**
     * Applies a broadcast {@code chat.proposal}: it answers the chat's open proposal, and an applied one
     * changes its subject's resource. Call under the lock.
     */
    private Outcome broadcastProposal(Chat chat, ChatProposalEvent answered, String event,
            Map<String, Object> payload) {
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
        return broadcasted(chat, event, payload, applyHook(chat.subject, chat.proposal));
    }

    /** Applies a broadcast {@code chat.ended}: it ends the chat. Call under the lock. */
    private Outcome broadcastEnded(Chat chat, ChatEndedEvent ended, String event, Map<String, Object> payload) {
        if (chat.state.isEnded()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, MESSAGE_CHAT_ENDED);
        }
        endChat(chat, ended.endedBy() == ChatEndedEvent.EndedBy.PROPOSAL_CARRIED ? ChatStatus.CARRIED
                : ChatStatus.END, ended.endedBy());
        return broadcasted(chat, event, payload, null);
    }

    /** The answer to a broadcast chat event that was applied: the event goes on the chat's stream. */
    private static Outcome broadcasted(Chat chat, String event, Map<String, Object> payload, @Nullable Runnable then) {
        return new Outcome(200, Map.of(KEY_STATUS, "broadcasted", KEY_EVENT, event),
                List.of(new ChatEvent(event, streamOf(chat), payload)), then);
    }

    /**
     * Reads a broadcast chat event through its {@code pm-api} record.
     *
     * @return the event's record
     * @throws IOException if the name is no chat event's or the record refuses the payload
     */
    private static Object parseChatEvent(String event, Map<String, Object> payload) throws IOException {
        var name = EventName.fromWireOrNull(event);
        var parser = name == null ? null : CHAT_EVENT_PARSERS.get(name);
        if (parser == null) {
            throw new IOException("No chat event is named " + event);
        }
        return parser.parse(payload);
    }

    /** Reads the payload of one chat event through its {@code pm-api} record. */
    @FunctionalInterface
    private interface ChatEventParser {

        /**
         * @param payload the event's payload
         * @return the event's record
         * @throws IOException if the record refuses the payload
         */
        Object parse(Map<String, Object> payload) throws IOException;
    }

    private synchronized Outcome end(Address address) {
        var chat = chats.get(address.subject());
        if (chat == null) {
            return refused(ApiErrorCode.TARGET_NOT_FOUND, MESSAGE_NO_CHAT + address.routes().subject());
        }
        if (chat.state.isEnded()) {
            return refused(ApiErrorCode.ACT_NOT_AVAILABLE, MESSAGE_CHAT_ENDED);
        }
        var events = endChat(chat, ChatStatus.END, ChatEndedEvent.EndedBy.OPERATOR);
        return new Outcome(200, chat.toResource(chat.turns.size()).toMap(), events, null);
    }

    /**
     * A revision of a plan is accepted and queued for the plan's next task boundary; every other kind
     * applies at once.
     */
    private static ProposalState acceptedState(ProposalKind kind) {
        return kind == ProposalKind.REVISION ? ProposalState.ACCEPTED : ProposalState.APPLIED;
    }

    /**
     * What the role of a chat knows, as items of {@code control.chat.knows.item}
     * ({@code state-catalogue.adoc#chat-context}): ended work its archived record with the outcome it
     * ended with (nothing where the end named no outcome), a decision the decision by its id, and a
     * plan, an epic or an entry what the engine renders of its subject's data — a plan's description,
     * its tasks, its phase and its version, an entry's description and sources, an epic's rows and
     * items — with {@code as_of} the instant it was rendered.
     */
    private synchronized ChatContext context(Address address) {
        var subject = address.subject();
        List<MessageText> knows;
        if (address.scope() == ChatScopeKind.ARCHIVE) {
            var outcome = endedWork.get(subject);
            knows = outcome == null ? List.of()
                    : List.of(new MessageText(KNOWS_PREFIX + KNOWS_ARCHIVE, Map.of(ARG_OUTCOME, outcome)));
        } else if (subject.kind() == ChatSubjectKind.DECISION) {
            var id = subject.id();
            knows = List.of(new MessageText(KNOWS_PREFIX + KNOWS_DECISION,
                    Map.of(KEY_ID, id.substring(id.indexOf('/') + 1))));
        } else {
            knows = facts.knows(subject, address.scope());
            if (knows.isEmpty()) {
                knows = List.of(MessageText.of(KNOWS_PREFIX + KNOWS_DESCRIPTION));
            }
        }
        return new ChatContext(knows, Instant.now().toString());
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
        // The chat segment is read by position, not by the first segment that reads `chat`: an id may
        // be `chat`, and then the id's segment is no route's. The segment whose prefix parses to an
        // address and whose path the address builds exactly is the chat's.
        for (var chatAt = 2; chatAt < segments.size(); chatAt++) {
            if (!SEGMENT_CHAT.equals(segments.get(chatAt))) {
                continue;
            }
            try {
                var address = address(segments.subList(0, chatAt));
                if (address == null) {
                    continue;
                }
                var target = action(address, segments.subList(chatAt + 1, segments.size()));
                if (target != null && path.equals(target.expectedPath())) {
                    return target;
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IllegalArgumentException _) {
                // a prefix that is no address is not the chat's segment
            }
        }
        return null;
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
                        new EventScope(EventScopeKind.ENTRY, id, null), null);
            };
        }
        if (segments.size() != 4 || !SEGMENT_DECISIONS.equals(segments.get(2))) {
            return null;
        }
        var questionId = decode(segments.get(3));
        var decision = new ChatSubject(ChatSubjectKind.DECISION, id + "/" + questionId);
        return switch (first) {
            case SEGMENT_PLANS -> new Address(ChatRoutes.planDecision(id, questionId), decision,
                    ChatScopeKind.DECISION, ROLE_REPLAN, new EventScope(EventScopeKind.PLAN, id, null), id);
            case SEGMENT_EPICS -> new Address(ChatRoutes.epicDecision(id, questionId), decision,
                    ChatScopeKind.DECISION, ROLE_DECOMPOSE, new EventScope(EventScopeKind.EPIC, id, null), null);
            default -> new Address(ChatRoutes.entryDecision(id, questionId), decision, ChatScopeKind.DECISION,
                    ROLE_REFINE, new EventScope(EventScopeKind.ENTRY, id, null), null);
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
        return new Address(routes, subject, scope, role, new EventScope(streamKind, id, null),
                kind == ChatSubjectKind.PLAN ? id : null);
    }

    private synchronized boolean isEnded(ChatSubject subject) {
        return endedWork.containsKey(subject);
    }

    /**
     * @param subject the subject of a seeded chat
     * @param scope   the chat's scope
     * @return the plan the chat's revisions belong to: the plan's own id, or a decision's plan, and
     *         {@code null} for ended work and for every other subject
     */
    private static @Nullable String seededPlanId(ChatSubject subject, ChatScopeKind scope) {
        if (scope == ChatScopeKind.ARCHIVE) {
            return null;
        }
        return switch (subject.kind()) {
            case PLAN -> subject.id();
            case DECISION -> subject.id().substring(0, subject.id().indexOf('/'));
            default -> null;
        };
    }

    private static Address workspaceDecision(String projectId, String questionId) {
        return new Address(ChatRoutes.workspaceDecision(projectId, questionId),
                new ChatSubject(ChatSubjectKind.DECISION, projectId + "/" + questionId), ChatScopeKind.DECISION,
                ROLE_REFINE, new EventScope(EventScopeKind.WORKSPACE, null, projectId), null);
    }

    private static Address machineDecision(String questionId) {
        return new Address(ChatRoutes.machineDecision(questionId),
                new ChatSubject(ChatSubjectKind.DECISION, MACHINE_SCOPE_ID + "/" + questionId),
                ChatScopeKind.DECISION, ROLE_REFINE, new EventScope(EventScopeKind.MACHINE, null, null), null);
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
     * @param planId  the plan the chat's revisions belong to — the plan's own id, or the plan of a
     *                decision's chat — {@code null} where the chat is about no plan
     */
    private record Address(ChatRoutes routes, ChatSubject subject, ChatScopeKind scope, String role,
    EventScope stream, @Nullable String planId) {
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
     * What a chat is about and where it is routed.
     *
     * @param subject what the chat is about
     * @param role    the role the chat is routed to, with its model
     * @param scope   the chat's scope
     * @param planId  the plan the chat's revisions belong to, or {@code null}
     */
    private record Routing(ChatSubject subject, RoutedRole role, ChatScopeKind scope, @Nullable String planId) {
    }

    /**
     * The mutable state of one chat with its whole transcript.
     */
    private static final class Chat {
        private final String id;
        private final ChatSubject subject;
        private final RoutedRole role;
        private final ChatScopeKind scope;
        /** The plan the chat's revisions belong to, or {@code null}. */
        private final @Nullable String planId;
        private ChatStatus state;
        private ChatContext context;
        private @Nullable ChatProposal proposal;
        /** The plan's instance version the open revision proposal was made against, or {@code null}. */
        private @Nullable Integer proposalPlanVersion;
        private final List<ChatTurn> turns;
        private final Map<String, String> clientTurns = new LinkedHashMap<>();
        /** The narrative lines held until the reply being written ends. */
        private final List<NarrativeLine> heldLines = new ArrayList<>();
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

        Chat(String id, Routing routing, ChatStatus state, ChatContext context, @Nullable ChatProposal proposal,
                List<ChatTurn> turns) {
            this.id = id;
            this.subject = routing.subject();
            this.role = routing.role();
            this.scope = routing.scope();
            this.planId = routing.planId();
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
