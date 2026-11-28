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

import java.io.IOError;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatEndedEvent;
import de.planmarshall.api.chat.ChatMessageKeys;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnDeltaEvent;
import de.planmarshall.api.chat.ChatTurnProgressEvent;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.event.EventEnvelope;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.SseEvent;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.actions.WorkActionsClient;
import de.planmarshall.operator.dialogue.ChoicePrompt;

/**
 * Follows a chat subject's SSE stream from before the chat is opened so live reply steps,
 * completed lines, worker replacements, and failures print above JLine's writing prompt via
 * {@link LineReader#printAbove(String)} while input stays line-based ({@code s} + Enter stops,
 * {@code q} + Enter leaves, any other line is held for the next first line).
 */
final class ChatStreamFollower implements AutoCloseable {

    private static final String EVENT_STARTED = "chat.turn.started";
    private static final String EVENT_PROGRESS = "chat.turn.progress";
    private static final String EVENT_DELTA = "chat.turn.delta";
    private static final String EVENT_COMPLETED = "chat.turn.completed";
    private static final String EVENT_ENDED = "chat.ended";
    private static final long INTERRUPT_JOIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static final long INTERRUPT_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(10);

    private enum StreamSignal {
        EVENT,
        STREAM_CLOSED,
        WAKEUP
    }

    private record StreamItem(StreamSignal signal, @Nullable String eventName, @Nullable Map<String, Object> payload) {
        static final StreamItem CLOSED = new StreamItem(StreamSignal.STREAM_CLOSED, null, null);
        static final StreamItem WAKEUP = new StreamItem(StreamSignal.WAKEUP, null, null);
    }

    record Outcome(
    boolean completedViaStream,
    @Nullable String heldFirstLine,
    boolean leftDialogue,
    @Nullable ChatEndedEvent endedEvent
    ) {
    }

    private final WorkActionsClient client;
    private final String kind;
    private final String subjectId;
    private final String locale;
    private final @Nullable HttpExchange exchange;
    private final BlockingQueue<StreamItem> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final @Nullable Thread sseThread;

    private ChatStreamFollower(WorkActionsClient client, String kind, String subjectId, String locale,
                        @Nullable HttpExchange exchange) {
        this.client = client;
        this.kind = kind;
        this.subjectId = subjectId;
        this.locale = locale;
        this.exchange = exchange;
        if (exchange != null) {
            this.sseThread = Thread.ofPlatform().daemon(true)
                                .name("chat-sse-" + subjectId)
                                .start(this::readStream);
        } else {
            this.sseThread = null;
        }
    }

    /**
     * Opens the subject's SSE stream before the chat is read. Returns an inactive follower when the
     * runtime serves no event stream so unit servers without {@code /events} can fall back cleanly.
     */
    static ChatStreamFollower open(WorkActionsClient client, String kind, String subjectId, String locale) {
        HttpExchange opened = null;
        try {
            opened = client.openEvents(kind, subjectId);
            if (opened.status() == 200) {
                return new ChatStreamFollower(client, kind, subjectId, locale, opened);
            }
            opened.close();
        } catch (IOException _) {
            if (opened != null) {
                closeQuietly(opened);
            }
        }
        return new ChatStreamFollower(client, kind, subjectId, locale, null);
    }

    boolean isActive() {
        return exchange != null && !closed.get();
    }

    void drainQueuedEvents() {
        while (true) {
            var next = queue.peek();
            if (next == null || next.signal() != StreamSignal.EVENT) {
                return;
            }
            queue.poll();
        }
    }

    Outcome followReply(LineReader reader, String chatId, String operatorTurnId, String fallbackRole,
                        Set<String> printedTurnIds) {
        var stopKey = MessageCatalogue.format(locale, "operator.actions.key.stop", Map.of());
        var leaveKey = leaveKey(locale);
        var writingPrompt = MessageCatalogue.format(locale, "operator.actions.chat.writing",
                            Map.of("stop", stopKey, "leave", leaveKey)) + StartPrompt.SUFFIX;
        var session = new FollowSession(reader, writingPrompt, stopKey, chatId, operatorTurnId,
                            new ReplyRenderState(reader, fallbackRole, printedTurnIds));

        var terminal = reader.getTerminal();
        var previousInt = terminal.handle(Terminal.Signal.INT, Terminal.SignalHandler.SIG_IGN);
        var inputThread = Thread.ofPlatform().daemon(true).name("chat-writing-input")
                            .start(() -> readWritingInput(session));

        try {
            drainReplyEvents(session);
        } finally {
            session.finished.set(true);
            joinInputThread(terminal, inputThread, previousInt);
        }

        if (session.interruptedByUser.get()) {
            throw new StartPrompt.InterruptException();
        }
        return new Outcome(session.state.completed, session.heldFirstLine.get(),
                            session.leftDuringReply.get(), session.state.endedEvent);
    }

    private void drainReplyEvents(FollowSession session) {
        while (!session.finished.get()) {
            StreamItem item;
            try {
                item = queue.take();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                session.finished.set(true);
                return;
            }
            if (!processNextItem(item, session)) {
                return;
            }
        }
    }

    private boolean processNextItem(StreamItem item, FollowSession session) {
        if (item.signal() == StreamSignal.WAKEUP) {
            return false;
        }
        if (item.signal() == StreamSignal.STREAM_CLOSED) {
            session.state.flushPendingPartial();
            return false;
        }
        handleEvent(item.eventName(), item.payload(), session);
        return true;
    }

    private static void joinInputThread(Terminal terminal, Thread inputThread,
                        Terminal.SignalHandler previousInt) {
        var intDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(150);
        var deadline = System.nanoTime() + INTERRUPT_JOIN_TIMEOUT_NANOS;
        while (inputThread.isAlive() && System.nanoTime() < deadline) {
            terminal.raise(Terminal.Signal.INT);
            if (System.nanoTime() >= intDeadline) {
                inputThread.interrupt();
            }
            LockSupport.parkNanos(INTERRUPT_POLL_NANOS);
        }
        terminal.handle(Terminal.Signal.INT, previousInt);
    }

    private void readWritingInput(FollowSession session) {
        while (!session.finished.get()) {
            var line = readWritingLine(session);
            if (line == null) {
                if (session.finished.get()) {
                    return;
                }
                continue;
            }
            if (!handleWritingLine(line, session)) {
                return;
            }
        }
    }

    private @Nullable String readWritingLine(FollowSession session) {
        try {
            return session.reader.readLine(session.writingPrompt);
        } catch (UserInterruptException _) {
            if (session.finished.compareAndSet(false, true)) {
                session.interruptedByUser.set(true);
                enqueue(StreamItem.WAKEUP);
            }
            return null;
        } catch (EndOfFileException | IOError _) {
            if (session.finished.compareAndSet(false, true)) {
                session.leftDuringReply.set(true);
                enqueue(StreamItem.WAKEUP);
            }
            return null;
        }
    }

    private boolean handleWritingLine(String line, FollowSession session) {
        var trimmed = line.trim();
        if (trimmed.equalsIgnoreCase(session.stopKey)) {
            var replyTurnId = session.activeReplyTurnId.get();
            if (replyTurnId != null) {
                stopQuietly(replyTurnId);
            } else {
                session.stopRequested.set(true);
            }
        } else if (ChoicePrompt.isLeave(locale, trimmed)) {
            session.leftDuringReply.set(true);
            session.finished.set(true);
            enqueue(StreamItem.WAKEUP);
            return false;
        } else if (!line.isBlank()) {
            session.heldFirstLine.compareAndSet(null, line);
        }
        return true;
    }

    private void handleEvent(@Nullable String eventName, @Nullable Map<String, Object> payload,
                        FollowSession session) {
        if (eventName == null || payload == null) {
            return;
        }
        switch (eventName) {
            case EVENT_STARTED -> onStarted(payload, session.chatId, session.activeReplyTurnId,
                                session.stopRequested, session.state);
            case EVENT_PROGRESS -> onProgress(payload, session.chatId, session.activeReplyTurnId.get(), session.state);
            case EVENT_DELTA -> onDelta(payload, session.chatId, session.activeReplyTurnId.get(), session.state);
            case EVENT_COMPLETED -> onCompleted(payload, session.chatId, session.operatorTurnId,
                                session.activeReplyTurnId, session.state, session.finished,
                                session.state.printedTurnIds);
            case EVENT_ENDED -> onEnded(payload, session.chatId, session.finished, session.state);
            default -> {
                // Other subject events belong to the background tree, not this chat turn.
            }
        }
    }

    private void onStarted(Map<String, Object> payload, String chatId,
                        AtomicReference<@Nullable String> activeReplyTurnId,
                        AtomicBoolean stopRequested, ReplyRenderState state) {
        ChatTurnStartedEvent started;
        try {
            started = ChatTurnStartedEvent.fromMap(stripEnvelope(payload));
        } catch (IOException _) {
            return;
        }
        if (!chatId.equals(started.chatId()) || started.kind() != TurnKind.MESSAGE
                            || ChatTurn.AUTHOR_OPERATOR.equals(started.author())) {
            return;
        }
        var previous = activeReplyTurnId.getAndSet(started.turnId());
        if (previous != null) {
            state.resetForReplacement();
            state.printAbove("  " + MessageCatalogue.format(locale, "operator.actions.chat.restarted", Map.of()));
        }
        state.beginReply(started.author(), started.untrusted());
        if (stopRequested.getAndSet(false)) {
            stopQuietly(started.turnId());
        }
    }

    private void onProgress(Map<String, Object> payload, String chatId, @Nullable String replyTurnId,
                        ReplyRenderState state) {
        ChatTurnProgressEvent progress;
        try {
            progress = ChatTurnProgressEvent.fromMap(stripEnvelope(payload));
        } catch (IOException _) {
            return;
        }
        if (!chatId.equals(progress.chatId()) || (replyTurnId != null && !replyTurnId.equals(progress.turnId()))) {
            return;
        }
        state.ensureHeader();
        var rendered = ChatMessageKeys.render(progress.step());
        var text = MessageCatalogue.format(locale, rendered.key(), rendered.args());
        state.printAbove("       " + OutputSafety.stripControlSequences(text));
    }

    private void onDelta(Map<String, Object> payload, String chatId, @Nullable String replyTurnId,
                        ReplyRenderState state) {
        ChatTurnDeltaEvent delta;
        try {
            delta = ChatTurnDeltaEvent.fromMap(stripEnvelope(payload));
        } catch (IOException _) {
            return;
        }
        if (!chatId.equals(delta.chatId()) || (replyTurnId != null && !replyTurnId.equals(delta.turnId()))) {
            return;
        }
        state.appendDelta(delta.text());
    }

    private void onCompleted(Map<String, Object> payload, String chatId, String operatorTurnId,
                        AtomicReference<@Nullable String> activeReplyTurnId, ReplyRenderState state,
                        AtomicBoolean finished, Set<String> printedTurnIds) {
        ChatTurnCompletedEvent completed;
        try {
            completed = ChatTurnCompletedEvent.fromMap(stripEnvelope(payload));
        } catch (IOException _) {
            return;
        }
        if (!chatId.equals(completed.chatId()) || completed.kind() != TurnKind.MESSAGE
                            || operatorTurnId.equals(completed.turnId())) {
            return;
        }
        var expected = activeReplyTurnId.get();
        if (expected == null && printedTurnIds.contains(completed.turnId())) {
            return;
        }
        if (expected != null && !expected.equals(completed.turnId())) {
            return;
        }
        state.completeReply(completed, locale);
        finished.set(true);
    }

    private static void onEnded(Map<String, Object> payload, String chatId, AtomicBoolean finished,
                        ReplyRenderState state) {
        try {
            var event = ChatEndedEvent.fromMap(stripEnvelope(payload));
            if (chatId.equals(event.chatId())) {
                state.endedEvent = event;
                finished.set(true);
            }
        } catch (IOException _) {
            // Ignore unrelated or malformed chat.ended payloads.
        }
    }

    private void stopQuietly(String turnId) {
        try {
            client.stopReply(kind, subjectId, turnId);
        } catch (IOException _) {
            // Best effort: the stream or subsequent snapshot shows the server state.
        }
    }

    private void readStream() {
        var activeExchange = exchange;
        if (activeExchange == null) {
            return;
        }
        try {
            var sse = activeExchange.events();
            SseEvent event;
            while (!closed.get() && (event = sse.next()) != null) {
                var parsed = parseMap(event.data());
                if (parsed == null) {
                    continue;
                }
                var name = event.event() != null && !event.event().isBlank()
                                    ? event.event()
                                    : stringMember(parsed, EventEnvelope.FIELD_EVENT);
                enqueue(new StreamItem(StreamSignal.EVENT, name, parsed));
            }
        } catch (IOException _) {
            // Stream closed or broken; signal completion loop below.
        } finally {
            enqueue(StreamItem.CLOSED);
        }
    }

    private void enqueue(StreamItem item) {
        if (!queue.offer(item)) {
            closed.set(true);
        }
    }

    private static @Nullable Map<String, Object> parseMap(@Nullable String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return JsonTree.asObject(JsonTree.parse(data));
        } catch (IOException _) {
            return null;
        }
    }

    private static @Nullable String stringMember(Map<String, Object> map, String key) {
        return map.get(key) instanceof String text ? text : null;
    }

    private static Map<String, Object> stripEnvelope(Map<String, Object> raw) {
        var stripped = new LinkedHashMap<String, Object>(raw);
        stripped.remove(EventEnvelope.FIELD_EVENT);
        stripped.remove(EventEnvelope.FIELD_ID);
        stripped.remove(EventEnvelope.FIELD_AT);
        stripped.remove(EventEnvelope.FIELD_SCOPE);
        return stripped;
    }

    private static String leaveKey(String locale) {
        return locale.toLowerCase(Locale.ROOT).startsWith("de") ? "v" : "q";
    }

    private static void closeQuietly(HttpExchange target) {
        try {
            target.close();
        } catch (IOException _) {
            // Closing best-effort on cleanup.
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (exchange != null) {
                closeQuietly(exchange);
            }
            if (sseThread != null) {
                sseThread.interrupt();
            }
            enqueue(StreamItem.CLOSED);
        }
    }

    private static final class FollowSession {

        private final LineReader reader;
        private final String writingPrompt;
        private final String stopKey;
        private final String chatId;
        private final String operatorTurnId;
        private final ReplyRenderState state;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private final AtomicBoolean leftDuringReply = new AtomicBoolean(false);
        private final AtomicBoolean interruptedByUser = new AtomicBoolean(false);
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);
        private final AtomicReference<@Nullable String> heldFirstLine = new AtomicReference<>();
        private final AtomicReference<@Nullable String> activeReplyTurnId = new AtomicReference<>();

        private FollowSession(LineReader reader, String writingPrompt, String stopKey, String chatId,
                            String operatorTurnId, ReplyRenderState state) {
            this.reader = reader;
            this.writingPrompt = writingPrompt;
            this.stopKey = stopKey;
            this.chatId = chatId;
            this.operatorTurnId = operatorTurnId;
            this.state = state;
        }
    }

    private final class ReplyRenderState {

        private final LineReader reader;
        private final Set<String> printedTurnIds;
        private final StringBuilder pendingLine = new StringBuilder();
        private String role;
        private boolean untrusted;
        private boolean headerPrinted;
        private boolean sawDelta;
        private boolean firstLinePrinted;
        private boolean completed;
        private @Nullable ChatEndedEvent endedEvent;

        private ReplyRenderState(LineReader reader, String fallbackRole, Set<String> printedTurnIds) {
            this.reader = reader;
            this.role = Objects.requireNonNullElse(fallbackRole, "");
            this.printedTurnIds = printedTurnIds;
        }

        private void beginReply(String author, boolean untrustedFlag) {
            this.role = author;
            this.untrusted = untrustedFlag;
            this.headerPrinted = false;
            this.sawDelta = false;
            this.firstLinePrinted = false;
            ensureHeader();
        }

        private void resetForReplacement() {
            this.pendingLine.setLength(0);
            this.sawDelta = false;
            this.firstLinePrinted = false;
            this.headerPrinted = false;
        }

        private void ensureHeader() {
            if (!headerPrinted) {
                headerPrinted = true;
                var safeRole = OutputSafety.stripControlSequences(role);
                printAbove("    \u25b6  " + MessageCatalogue.format(locale,
                                    "operator.actions.chat.answers", Map.of("role", safeRole)));
            }
        }

        private void appendDelta(String piece) {
            sawDelta = true;
            ensureHeader();
            pendingLine.append(piece);
            int nl;
            while ((nl = pendingLine.indexOf("\n")) >= 0) {
                var rawLine = pendingLine.substring(0, nl);
                pendingLine.delete(0, nl + 1);
                printReplyLine(rawLine, false);
            }
        }

        private void flushPendingPartial() {
            if (!pendingLine.isEmpty()) {
                var tail = pendingLine.toString();
                pendingLine.setLength(0);
                printReplyLine(tail, untrusted);
            }
        }

        private void completeReply(ChatTurnCompletedEvent event, String activeLocale) {
            printedTurnIds.add(event.turnId());
            this.untrusted = this.untrusted || event.untrusted();
            boolean isUntrusted = this.untrusted;
            if (!sawDelta && !event.text().isBlank()) {
                ensureHeader();
                var lines = event.text().split("\\R", -1);
                for (int i = 0; i < lines.length; i++) {
                    printReplyLine(lines[i], isUntrusted && i == lines.length - 1);
                }
            } else if (sawDelta) {
                if (!pendingLine.isEmpty()) {
                    var tail = pendingLine.toString();
                    pendingLine.setLength(0);
                    printReplyLine(tail, isUntrusted);
                } else if (isUntrusted && firstLinePrinted) {
                    var indent = " ".repeat(OutputSafety.stripControlSequences(role).length());
                    printAbove("  " + indent + "  \"" + untrustedTag(activeLocale));
                }
            }
            var reason = event.reason();
            if (event.status() == TurnStatus.STOPPED) {
                printAbove("  " + MessageCatalogue.format(activeLocale, "control.chat.stopped", Map.of()));
            } else if (event.status() == TurnStatus.FAILED && reason != null) {
                printFailureReason(reason, activeLocale);
            }
            completed = true;
        }

        private void printFailureReason(MessageText reason, String activeLocale) {
            var rendered = ChatMessageKeys.render(reason);
            var args = new LinkedHashMap<String, Object>(rendered.args());
            args.putIfAbsent("role", OutputSafety.stripControlSequences(role));
            args.putIfAbsent("reason", "");
            var formatted = MessageCatalogue.format(activeLocale, rendered.key(), args);
            printAbove("  " + OutputSafety.stripControlSequences(formatted));
        }

        private void printReplyLine(String rawLine, boolean closingQuote) {
            var safeRole = OutputSafety.stripControlSequences(role);
            var safeLine = OutputSafety.stripControlSequences(rawLine);
            var openQuote = untrusted ? "\"" : "";
            var prefix = firstLinePrinted
                                ? "  " + " ".repeat(safeRole.length()) + "  "
                                : "  " + safeRole + "  " + openQuote;
            firstLinePrinted = true;
            var suffix = closingQuote && untrusted ? "\"" + untrustedTag(locale) : "";
            printAbove(prefix + safeLine + suffix);
        }

        private String untrustedTag(String activeLocale) {
            return "   " + MessageCatalogue.format(activeLocale, "operator.actions.review.untrusted", Map.of());
        }

        private void printAbove(String line) {
            reader.printAbove(line);
        }
    }
}
