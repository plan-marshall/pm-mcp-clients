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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The pieces of the entry that the transcript does not reach directly: the step names in either
 * locale, the onboarding detection of the several status shapes, the badge extraction, and the web
 * port fallback.
 */
@DisplayName("EntryFlow statics")
class EntryFlowStaticsTest {

    @Test
    @DisplayName("step names follow the interface locale and the unknown step keeps its number")
    void stepNames() throws Exception {
        try (var fixture = RuntimeFixture.create()) {
            var flow = new EntryFlow(new OperatorContext(fixture.environment(), fixture.base().getParent(), true));
            assertEquals("Harnesses", flow.stepName("en", 2));
            assertEquals("Harnesses", flow.stepName("de", 2));
            assertEquals("Step 8", flow.stepName("en", 8));
            assertEquals("Schritt 8", flow.stepName("de", 8));
        }
    }

    @Test
    @DisplayName("a worker that throws marks the read failed instead of answering null")
    void aThrowingWorkerMarksTheReadFailed() {
        var failure = assertThrows(IOException.class, () -> EntryFlow.bounded(() -> {
            throw new IllegalStateException("boom");
        }, Duration.ofSeconds(1), "throwing-read"));

        assertTrue(failure.getMessage().contains("throwing-read"), failure.getMessage());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals("boom", failure.getCause().getMessage());
    }

    @Test
    @DisplayName("a sub-millisecond bound does not wait forever")
    void aSubMillisecondBoundDoesNotWaitForever() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var started = System.nanoTime();
            assertThrows(IOException.class, () -> EntryFlow.bounded(() -> {
                try {
                    // A worker that never gets a result of its own: it waits until it is interrupted.
                    new CountDownLatch(1).await();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
                return "late";
            }, Duration.ofNanos(1), "slow-read"));
            var elapsed = Duration.ofNanos(System.nanoTime() - started);
            assertTrue(elapsed.compareTo(Duration.ofSeconds(1)) < 0, elapsed.toString());
        });
    }

    @Test
    @DisplayName("a read interrupted while waiting interrupts its worker and keeps the flag")
    void anInterruptedWaitInterruptsTheWorker() throws Exception {
        var workerStarted = new CountDownLatch(1);
        var workerInterrupted = new AtomicBoolean();
        var workerLeft = new CountDownLatch(1);
        var callerFlagKept = new AtomicBoolean();
        var failure = new AtomicReference<IOException>();
        // The caller is interrupted only once the worker blocks, so the interrupt releases a worker
        // that is already waiting instead of racing the read's own start.
        var caller = Thread.ofPlatform().unstarted(() -> {
            try {
                EntryFlow.bounded(() -> {
                    workerStarted.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                    workerInterrupted.set(Thread.currentThread().isInterrupted());
                    workerLeft.countDown();
                    return "late";
                }, Duration.ofSeconds(30), "interrupted-read");
            } catch (IOException e) {
                callerFlagKept.set(Thread.currentThread().isInterrupted());
                failure.set(e);
            }
        });
        caller.start();
        assertTrue(workerStarted.await(5, TimeUnit.SECONDS), "the worker must have started");
        caller.interrupt();
        caller.join(5_000);
        // The read interrupts its worker and returns at once, so the worker records what it saw
        // after the caller has left: the check waits for the worker, never for the caller alone.
        assertTrue(workerLeft.await(5, TimeUnit.SECONDS), "the worker must have left its wait");

        assertAll(
                            () -> assertNotNull(failure.get(), "the read must fail when the caller is interrupted"),
                            () -> assertTrue(callerFlagKept.get(), "the caller's interrupt flag stays set"),
                            () -> assertTrue(workerInterrupted.get(), "the worker was interrupted, not left running"));
    }

    @Test
    @DisplayName("the machine is onboarded from the flat flag, the nested flag, or the completed state")
    void onboardingDetection() {
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboarded", true)));
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("machine", Map.of("onboarded", true))));
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboard", Map.of("state", "onboard.completed"))));
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboard", Map.of("completed", true))));
        assertFalse(EntryFlow.isMachineOnboarded(Map.of("onboarded", false)));
        assertFalse(EntryFlow.isMachineOnboarded(Map.of()));
    }

    @Test
    @DisplayName("the completed step and the web port read the numbers and fall back")
    void numbers() {
        assertEquals(3, EntryFlow.getCompletedOnboardStep(Map.of("onboard", Map.of("step", 3))));
        assertEquals(2, EntryFlow.getCompletedOnboardStep(
                            Map.of("onboard", Map.of("completed_steps", 2))));
        assertEquals(0, EntryFlow.getCompletedOnboardStep(Map.of()));
        assertEquals(9000, EntryFlow.getWebPort(Map.of("web", Map.of("port", 9000))));
        assertEquals(EntryFlow.DEFAULT_WEB_PORT, EntryFlow.getWebPort(Map.of()));
    }

    /**
     * The status comes off the wire as {@link JsonNumber}s: a fraction or a number beyond an int once
     * threw out of the entry (the step) or wrapped into another number (the port, a badge count).
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"2.5", "-3", "4294967297", "1e3"})
    @DisplayName("a step, a port or a count that is no whole number in range is not taken")
    void numbersOutOfRangeAreNotTaken(String wire) {
        var number = new JsonNumber(wire);

        assertAll(
                            () -> assertEquals(0, EntryFlow.getCompletedOnboardStep(Map.of("onboard", Map.of("step", number)))),
                            () -> assertEquals(4, EntryFlow.getCompletedOnboardStep(
                                                Map.of("onboard", Map.of("step", number, "completed_steps", new JsonNumber("4"))))),
                            () -> assertEquals(EntryFlow.DEFAULT_WEB_PORT, EntryFlow.getWebPort(Map.of("web", Map.of("port", number)))),
                            () -> assertEquals(List.of(), EntryFlow.extractWaitingBadges(
                                                Map.of("needs_operator", Map.of("question", number)))));
    }

    @Test
    @DisplayName("a port beyond 65535 is no port, and the wire's whole numbers are read")
    void portRangeAndWireNumbers() {
        assertAll(
                            () -> assertEquals(EntryFlow.DEFAULT_WEB_PORT, EntryFlow.getWebPort(Map.of("web", Map.of("port", 70_000)))),
                            () -> assertEquals(EntryFlow.DEFAULT_WEB_PORT,
                                                EntryFlow.getWebPort(Map.of("web", Map.of("port", new JsonNumber("0"))))),
                            () -> assertEquals(9123, EntryFlow.getWebPort(Map.of("web", Map.of("port", new JsonNumber("9123"))))),
                            () -> assertEquals(5, EntryFlow.getCompletedOnboardStep(
                                                Map.of("onboard", Map.of("completed_steps", new JsonNumber("5"))))),
                            () -> assertEquals(List.of("2 questions"), EntryFlow.extractWaitingBadges(
                                                Map.of("needs_operator", Map.of("question", new JsonNumber("2"))))));
    }

    @Test
    @DisplayName("badges come from the needs_operator summaries and drop the zero counts")
    void badges() {
        var badges = EntryFlow.extractWaitingBadges(Map.of("needs_operator", Map.of(
                            "question", 2, "waiver", 1, "stalled", 0)));
        assertTrue(badges.contains("2 questions"), badges.toString());
        assertTrue(badges.contains("1 waiver request"), badges.toString());
        assertEquals(2, badges.size());
    }

    @Test
    @DisplayName("a status that still names the waiver by the specification's spelling badges the waiver kind")
    void legacyWaiverSpellingBadgesTheWaiverKind() {
        var badges = EntryFlow.extractWaitingConsoleBadges("en", Map.of("needs_operator", Map.of(
                            "waiver_request", 3)));

        assertAll(
                            () -> assertEquals(1, badges.size(), badges.toString()),
                            () -> assertEquals("waiver", badges.getFirst().itemKind()),
                            () -> assertEquals("3 waiver requests", badges.getFirst().text()),
                            () -> assertTrue(badges.getFirst().isQuestion())
        );
    }

    @Test
    @DisplayName("an empty status yields no badge")
    void noBadges() {
        assertEquals(0, EntryFlow.extractWaitingBadges(Map.of()).size());
    }

    /**
     * A code the runtime does not issue is refused: the client once answered such a request with a
     * code of its own, which no device could redeem (operator decision, TASK-13).
     */
    @Test
    @DisplayName("a pairing code the runtime does not issue is refused, never made up")
    void pairingFallback() throws Exception {
        try (var fixture = RuntimeFixture.create()) {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.send(500, null, ""));
            var context = new OperatorContext(fixture.environment(), fixture.base().getParent(), true);
            var flow = new EntryFlow(context);
            var runtime = context.runtime(true);

            var refusal = assertThrows(IOException.class, () -> flow.issuePairingCode(runtime, 7420));

            assertEquals("the runtime issued no code (HTTP 500)", refusal.getMessage());
        }
    }
}
