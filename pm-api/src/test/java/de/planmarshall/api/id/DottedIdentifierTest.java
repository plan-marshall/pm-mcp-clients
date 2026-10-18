/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.id;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dotted identifier grammars")
class DottedIdentifierTest {

    @Test
    @DisplayName("the event name grammar accepts the catalogue names and refuses malformed ones")
    void eventName() {
        assertTrue(DottedIdentifier.eventName("reset"));
        assertTrue(DottedIdentifier.eventName("check.result"));
        assertTrue(DottedIdentifier.eventName("runtime.upgrade_failed"));
        assertTrue(DottedIdentifier.eventName("needs_operator.changed"));

        assertFalse(DottedIdentifier.eventName("Check.Result"));
        assertFalse(DottedIdentifier.eventName("check..result"));
        assertFalse(DottedIdentifier.eventName(".check"));
        assertFalse(DottedIdentifier.eventName("check."));
        assertFalse(DottedIdentifier.eventName("check1"));
        assertFalse(DottedIdentifier.eventName(""));
        assertFalse(DottedIdentifier.eventName(null));
        assertFalse(DottedIdentifier.eventName("a".repeat(65)));
    }

    @Test
    @DisplayName("the state id grammar accepts the repository forms and refuses malformed ones")
    void stateId() {
        assertTrue(DottedIdentifier.stateId("onboarding"));
        assertTrue(DottedIdentifier.stateId("remediation.choose"));
        assertTrue(DottedIdentifier.stateId("finalize.push-barrier"));
        assertTrue(DottedIdentifier.stateId("a_" + "b".repeat(30)));

        assertFalse(DottedIdentifier.stateId("Onboarding"));
        assertFalse(DottedIdentifier.stateId("a..b"));
        assertFalse(DottedIdentifier.stateId(".a"));
        assertFalse(DottedIdentifier.stateId("a.b."));
        assertFalse(DottedIdentifier.stateId("a" + "b".repeat(32)));
        assertFalse(DottedIdentifier.stateId("a".repeat(65)));
        assertFalse(DottedIdentifier.stateId(null));
    }

    @Test
    @DisplayName("the message key grammar needs at least two segments and accepts a leading digit")
    void messageKey() {
        assertTrue(DottedIdentifier.messageKey("check.login.missing"));
        assertTrue(DottedIdentifier.messageKey("onboarding.choose.title"));
        assertTrue(DottedIdentifier.messageKey("a.b"));
        assertTrue(DottedIdentifier.messageKey("a.1"));

        assertFalse(DottedIdentifier.messageKey("nodot"));
        assertFalse(DottedIdentifier.messageKey(".a"));
        assertFalse(DottedIdentifier.messageKey("a."));
        assertFalse(DottedIdentifier.messageKey("A.b"));
        assertFalse(DottedIdentifier.messageKey("a.b-c"));
        assertFalse(DottedIdentifier.messageKey("a".repeat(129)));
        assertFalse(DottedIdentifier.messageKey(null));
    }

    @Test
    @DisplayName("the scan stays linear on a long input (no catastrophic backtracking)")
    void linearOnLongMalformedInput() {
        // a within-limit run of segments that fails only at the last character: the nested-quantifier
        // regex this scan replaces would backtrack super-linearly on it
        var longEvent = "a" + ".a".repeat(30) + "!";
        assertFalse(DottedIdentifier.eventName(longEvent));
        assertFalse(DottedIdentifier.stateId(longEvent));
        assertFalse(DottedIdentifier.messageKey(longEvent));

        // a run far beyond the length bound is refused by the length guard alone
        var oversized = "a".repeat(1_000_000);
        assertFalse(DottedIdentifier.eventName(oversized));
        assertFalse(DottedIdentifier.stateId(oversized));
        assertFalse(DottedIdentifier.messageKey(oversized));
    }
}
