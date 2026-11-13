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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * How the mock runtime plays the events it sends on its own: a state's scenario events and the events
 * of a chat's reply.
 * <p>
 * {@link #REALISTIC} plays them as a running runtime would: each after its delay, a reply's pieces at
 * a reading pace. {@link #ORDERED} sends them at once and in order, without a timer, so a test asserts
 * a sequence without waiting on time.
 */
public enum MockTiming {

    /** Each event after its delay, a reply's pieces at a reading pace. */
    REALISTIC("realistic"),

    /** Every event at once and in order, without a timer. */
    ORDERED("ordered");

    private final String wireValue;

    MockTiming(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the value of the {@code --timing} option
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @param value the value of the {@code --timing} option
     * @return the timing
     * @throws IllegalArgumentException if the value names no timing
     */
    public static MockTiming fromWire(String value) {
        Objects.requireNonNull(value, "value");
        for (var timing : values()) {
            if (timing.wireValue.equals(value)) {
                return timing;
            }
        }
        throw new IllegalArgumentException("Unknown timing '" + value + "', expected one of " + wireValues());
    }

    /**
     * @return the values of the {@code --timing} option, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(MockTiming::wireValue).toList();
    }
}
