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

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One entry of the modal actions menu that a level opens below a row or its header.
 *
 * <p>The trigger is the entry's mnemonic, already taken from the message catalogue in the language
 * of the client; the label is the already localized text of the action. An action that applies but
 * cannot run now stays in the menu, disabled, with the reason it names.
 *
 * @param trigger the mnemonic that runs the entry
 * @param action  the wire action the entry sends, or an internal menu action
 * @param label   the already localized label shown beside the mnemonic
 * @param enabled {@code true} when the entry can run now
 * @param reason  why the entry cannot run now, or {@code null} when it is enabled
 */
public record TuiMenuEntry(String trigger, String action, String label, boolean enabled,
@Nullable String reason) {

    /**
     * @param trigger the mnemonic that runs the entry
     * @param action  the wire action the entry sends
     * @param label   the already localized label
     * @param enabled whether the entry can run now
     * @param reason  why it cannot run now, or {@code null}
     */
    public TuiMenuEntry {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(label, "label");
        if (trigger.isEmpty()) {
            throw new IllegalArgumentException("a menu entry needs a mnemonic");
        }
    }

    /**
     * @param trigger the mnemonic
     * @param action  the wire action
     * @param label   the already localized label
     * @return an entry that can run now
     */
    public static TuiMenuEntry enabled(String trigger, String action, String label) {
        return new TuiMenuEntry(trigger, action, label, true, null);
    }

    /**
     * @param trigger the mnemonic
     * @param action  the wire action
     * @param label   the already localized label
     * @param reason  why the entry cannot run now
     * @return an entry that is shown but refuses to run
     */
    public static TuiMenuEntry disabled(String trigger, String action, String label, String reason) {
        return new TuiMenuEntry(trigger, action, label, false, reason);
    }
}
