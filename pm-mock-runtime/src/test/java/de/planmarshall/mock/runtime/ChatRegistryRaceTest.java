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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;


import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatOrigin;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.QueuedRevision;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.runtime.ChatRegistry.ChatEvent;
import de.planmarshall.mock.scenario.StateDefinition.ChatFailure;

/**
 * Races {@code playFailure} for {@link ChatFailure.Kind#WORKER_REPLACED} against an incoming
 * {@code stop} request directly against {@link ChatRegistry} so that checking the active play and
 * restarting the worker happen atomically and never resurrect a stopped turn.
 */
@DisplayName("Replaced-worker vs stop concurrency race")
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatRegistryRaceTest {

    private static final int ROUNDS = 400;
    private static final String HEADER_SEPARATOR = "\r\n\r\n";

    @Test
    @DisplayName("stop wins atomically against worker_replaced across repeated barrier races")
    void stopNeverResurrectedByReplacedWorker() throws Exception {
        var delegate = Executors.newSingleThreadScheduledExecutor();
        var stopExecutor = Executors.newSingleThreadExecutor();
        try {
            var stopped = 0;
            for (var round = 0; round < ROUNDS; round++) {
                if (runRound(round, delegate, stopExecutor)) {
                    stopped++;
                }
            }
            assertTrue(stopped > 0, "stop never succeeded against a streaming reply");
        } finally {
            stopExecutor.shutdownNow();
            delegate.shutdownNow();
        }
    }

    private static boolean runRound(int round, ScheduledExecutorService delegate, ExecutorService stopExecutor)
            throws Exception {
        var events = new CopyOnWriteArrayList<ChatEvent>();
        var postTurnsDone = new CountDownLatch(1);
        var lastInitialTask = new AtomicReference<Callable<?>>();
        var raceArmed = new AtomicBoolean(true);
        var stopReady = new CountDownLatch(1);
        var barrier = new CyclicBarrier(2);
        var uncaught = new AtomicReference<Throwable>();
        var scheduled = new CopyOnWriteArrayList<ScheduledFuture<?>>();

        var timingScheduler = new BarrierScheduler(delegate, scheduled, postTurnsDone, lastInitialTask, raceArmed,
                stopReady, barrier, uncaught);
        var registry = new ChatRegistry(events::add, _ -> false, new EventTiming(MockTiming.REALISTIC, timingScheduler),
                _ -> {
                }, stubFacts());

        registry.arm(List.of(ChatFailure.of(ChatFailure.Kind.WORKER_REPLACED)));
        var routes = ChatRoutes.plan("race-" + round + "-" + Generators.letterStrings(4, 6).next());
        assertEquals(201, send(registry, "POST", routes.chat(), "{}").status());

        var turnReq = new ChatTurnRequest(Generators.letterStrings(4, 24).next(),
                "client-" + round + "-" + Generators.letterStrings(4, 8).next()).toJson();
        var accepted = send(registry, "POST", routes.turns(), turnReq);
        assertEquals(202, accepted.status());
        var openedChat = send(registry, "GET", routes.chat(), "");
        assertEquals(200, openedChat.status());
        var replyTurnId = Objects.requireNonNull(lastTurnId(openedChat.body()),
                "missing assistant reply turn_id");
        postTurnsDone.countDown();

        var stopOut = new ByteArrayOutputStream();
        var stopReq = new MockHttpRequest("POST", routes.stop(replyTurnId), Map.of(),
                "{}".getBytes(StandardCharsets.UTF_8), System.nanoTime());
        var stopResp = new MockHttpResponse(stopOut);

        var stopFuture = CompletableFuture.supplyAsync(() -> {
            try {
                if (stopReady.await(2, TimeUnit.SECONDS)) {
                    barrier.await(500, TimeUnit.MILLISECONDS);
                    LockSupport.parkNanos((round % 12) * 30_000L);
                }
                assertTrue(registry.handle(stopReq, stopResp), "stop path not handled");
                return parseResult(stopOut);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                uncaught.compareAndSet(null, e);
                return new HttpResult(500, Map.of());
            } catch (BrokenBarrierException | TimeoutException | IOException e) {
                uncaught.compareAndSet(null, e);
                return new HttpResult(500, Map.of());
            }
        }, stopExecutor);

        var stopResult = stopFuture.get(5, TimeUnit.SECONDS);
        drainScheduled(scheduled);

        assertNull(uncaught.get(), "round " + round + " threw on scheduler/stop thread");
        assertTrue(stopResult.status() == 200 || stopResult.status() == 409,
                "unexpected stop status " + stopResult.status() + " on round " + round);

        if (stopResult.status() == 200) {
            var chat = send(registry, "GET", routes.chat(), "");
            assertEquals(200, chat.status());
            var status = turnStatus(chat.body(), replyTurnId);
            assertEquals(TurnStatus.STOPPED.wireValue(), status,
                    "stopped turn was resurrected on round " + round);
            assertNoReplayAfterStopCompletion(events, replyTurnId, round);
            return true;
        }
        return false;
    }

    private static void drainScheduled(List<ScheduledFuture<?>> scheduled) throws Exception {
        for (var i = 0; i < scheduled.size(); i++) {
            var future = scheduled.get(i);
            if (!future.isDone() && !future.isCancelled()) {
                try {
                    future.get(5, TimeUnit.SECONDS);
                } catch (CancellationException _) {
                    // Cancelled plays are expected when stop wins before later steps.
                }
            }
        }
    }

    private static void assertNoReplayAfterStopCompletion(List<ChatEvent> events, String turnId, int round) {
        var stoppedSeen = false;
        for (var event : events) {
            if (!turnId.equals(JsonTree.string(event.data(), "turn_id"))) {
                continue;
            }
            if ("chat.turn.completed".equals(event.name())
                    && TurnStatus.STOPPED.wireValue().equals(JsonTree.string(event.data(), "status"))) {
                stoppedSeen = true;
                continue;
            }
            if (stoppedSeen && ("chat.turn.progress".equals(event.name())
                    || "chat.turn.delta".equals(event.name())
                    || "chat.turn.completed".equals(event.name()))) {
                throw new AssertionError("event " + event.name() + " emitted after stop completion on round " + round);
            }
        }
        assertTrue(stoppedSeen, "stopped completion event missing on round " + round);
    }

    private static @Nullable String lastTurnId(Map<String, Object> chatBody) {
        if (!(chatBody.get("turns") instanceof List<?> turns) || turns.isEmpty()) {
            return null;
        }
        var last = JsonTree.asObject(turns.getLast());
        return last == null ? null : JsonTree.string(last, "turn_id");
    }

    private static @Nullable String turnStatus(Map<String, Object> chatBody, String turnId) {
        if (!(chatBody.get("turns") instanceof List<?> turns)) {
            return null;
        }
        for (var item : turns) {
            var map = JsonTree.asObject(item);
            if (map != null && turnId.equals(JsonTree.string(map, "turn_id"))) {
                return JsonTree.string(map, "status");
            }
        }
        return null;
    }

    private static HttpResult send(ChatRegistry registry, String method, String path, String jsonBody)
            throws IOException {
        var out = new ByteArrayOutputStream();
        var request = new MockHttpRequest(method, path, Map.of(),
                jsonBody.getBytes(StandardCharsets.UTF_8), System.nanoTime());
        var response = new MockHttpResponse(out);
        assertTrue(registry.handle(request, response), "path not handled: " + path);
        return parseResult(out);
    }

    private static HttpResult parseResult(ByteArrayOutputStream out) throws IOException {
        var raw = out.toString(StandardCharsets.UTF_8);
        var split = raw.indexOf(HEADER_SEPARATOR);
        var head = raw.substring(0, split);
        var firstLine = head.split("\r\n", 2)[0];
        var status = Integer.parseInt(firstLine.split(" ")[1]);
        var bodyText = raw.substring(split + HEADER_SEPARATOR.length());
        var parsed = bodyText.isBlank() ? Map.<String, Object>of()
                : Objects.requireNonNull(JsonTree.asObject(JsonTree.parse(bodyText)));
        return new HttpResult(status, parsed);
    }

    private static ChatFacts stubFacts() {
        return new ChatFacts() {
            @Override
            public List<MessageText> knows(ChatSubject subject, ChatScopeKind scope) {
                return List.of();
            }

            @Override
            public int instanceVersion(String planId) {
                return 1;
            }

            @Override
            public boolean revisionWindowOpen(String planId) {
                return true;
            }

            @Override
            public String activity(String planId) {
                return "running";
            }

            @Override
            public @Nullable String planFor(ChatSubject subject) {
                return subject.kind() == ChatSubjectKind.PLAN ? subject.id() : null;
            }

            @Override
            public boolean offersAct(ChatSubject subject, String act) {
                return false;
            }

            @Override
            public @Nullable QueuedRevision queuedRevision(String planId) {
                return null;
            }

            @Override
            public void queueRevision(String planId, QueuedRevision revision) {
            }

            @Override
            public void revisionChatEnded(String planId) {
            }

            @Override
            public boolean applyRevision(String planId, int expectedVersion, ChatOrigin origin) {
                return true;
            }

            @Override
            public void clearRevision(String planId) {
            }
        };
    }

    private record HttpResult(int status, Map<String, Object> body) {
    }

    /**
     * Scales realistic play delays into sub-millisecond waits and synchronizes the {@code playFailure}
     * step (always the last step scheduled during {@code POST .../turns}) with the concurrent stop thread.
     */
    private static final class BarrierScheduler extends AbstractExecutorService implements ScheduledExecutorService {

        private final ScheduledExecutorService delegate;
        private final List<ScheduledFuture<?>> scheduled;
        private final CountDownLatch postTurnsDone;
        private final AtomicReference<Callable<?>> lastInitialTask;
        private final AtomicBoolean raceArmed;
        private final CountDownLatch stopReady;
        private final CyclicBarrier barrier;
        private final AtomicReference<Throwable> uncaught;

        BarrierScheduler(ScheduledExecutorService delegate, List<ScheduledFuture<?>> scheduled,
                CountDownLatch postTurnsDone, AtomicReference<Callable<?>> lastInitialTask, AtomicBoolean raceArmed,
                CountDownLatch stopReady, CyclicBarrier barrier, AtomicReference<Throwable> uncaught) {
            this.delegate = delegate;
            this.scheduled = scheduled;
            this.postTurnsDone = postTurnsDone;
            this.lastInitialTask = lastInitialTask;
            this.raceArmed = raceArmed;
            this.stopReady = stopReady;
            this.barrier = barrier;
            this.uncaught = uncaught;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return schedule(Executors.callable(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            if (postTurnsDone.getCount() > 0) {
                lastInitialTask.set(callable);
            }
            var delayMicros = Math.max(0L, unit.toMillis(delay) * 5L);
            Callable<V> wrapped = () -> {
                postTurnsDone.await(5, TimeUnit.SECONDS);
                if (lastInitialTask.get() == callable && raceArmed.compareAndSet(true, false)) {
                    stopReady.countDown();
                    try {
                        barrier.await(500, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    } catch (BrokenBarrierException | TimeoutException _) {
                        // Continue running the scheduled step if the stop thread did not meet at the barrier.
                    }
                }
                return callable.call();
            };
            var future = delegate.schedule(wrapped, delayMicros, TimeUnit.MICROSECONDS);
            scheduled.add(future);
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }
    }
}
