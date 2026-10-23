/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Whether a full-screen view may open on a terminal, and why not when it may not
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-not-available}).
 *
 * <p>The verdict is computed once from the terminal that will actually draw, and the same verdict
 * decides the hint, the hand-off, and the refusal line. A refusal names exactly one reason, in the
 * precedence order of {@link Reason}: a non-interactive process cannot open a view at all, a dumb
 * terminal cannot address a cursor, the machine configuration may turn the view off, and a narrow
 * terminal cannot hold the layout.
 *
 * @param available whether the view may open
 * @param reason    why it may not, {@code null} when it may
 */
public record FullScreenAvailability(boolean available, @Nullable Reason reason) {

    /** Why a full-screen view cannot open. */
    public enum Reason {
        /** The process is not attached to an interactive terminal. */
        NON_INTERACTIVE,
        /** The terminal is a dumb terminal without cursor addressing. */
        DUMB_TERMINAL,
        /** {@code console.full_screen} of {@code machine-config.json} is {@code false}. */
        CONFIGURED_OFF,
        /** The terminal is narrower than the view's minimum. */
        TOO_NARROW
    }

    /**
     * @param reason why the view may not open
     */
    public FullScreenAvailability {
        if (available && reason != null) {
            throw new IllegalArgumentException("an available verdict carries no reason");
        }
        if (!available && reason == null) {
            throw new IllegalArgumentException("an unavailable verdict names its reason");
        }
    }

    /**
     * @return the verdict that the view may open
     */
    public static FullScreenAvailability open() {
        return new FullScreenAvailability(true, null);
    }

    /**
     * @param reason why the view may not open
     * @return the verdict that it may not
     */
    public static FullScreenAvailability refused(Reason reason) {
        return new FullScreenAvailability(false, Objects.requireNonNull(reason, "reason"));
    }

    /**
     * @return the catalogue key of the refusal line naming this reason
     * @throws IllegalStateException when the view is available and there is nothing to say
     */
    public String messageKey() {
        if (reason == null) {
            throw new IllegalStateException("an available verdict has no refusal text");
        }
        return "operator.tui.unavailable." + reason.name().toLowerCase(Locale.ROOT);
    }
}
