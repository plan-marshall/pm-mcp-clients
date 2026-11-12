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

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import org.jspecify.annotations.Nullable;

/**
 * The one timing seam every event the mock plays on its own goes through: a state's scenario events
 * and the events of a chat's reply. Under {@link MockTiming#REALISTIC} an event is scheduled after its
 * delay on the engine's scheduler; under {@link MockTiming#ORDERED} it is sent at once, in the order it
 * is played, without a timer.
 */
final class EventTiming {

    private final MockTiming timing;
    private final ScheduledExecutorService scheduler;

    /**
     * @param timing    the timing
     * @param scheduler the engine's scheduler
     */
    EventTiming(MockTiming timing, ScheduledExecutorService scheduler) {
        this.timing = Objects.requireNonNull(timing, "timing");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /**
     * @return the timing
     */
    MockTiming timing() {
        return timing;
    }

    /**
     * Plays one event.
     *
     * @param action  what sends the event
     * @param afterMs the delay under {@link MockTiming#REALISTIC}
     * @return the scheduled task, whose result is always {@code null}, or {@code null} when the event was
     *         sent at once
     */
    @Nullable
    ScheduledFuture<Object> play(Runnable action, long afterMs) {
        if (timing == MockTiming.ORDERED) {
            action.run();
            return null;
        }
        return scheduler.schedule(Executors.callable(action), afterMs, TimeUnit.MILLISECONDS);
    }
}
