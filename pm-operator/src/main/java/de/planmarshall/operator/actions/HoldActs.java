/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.actions;

import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * The closed set of acts a hold offers, and their one label and mnemonic mapping.
 *
 * <p>A hold's acts are exactly these five kinds' acts
 * ({@code doc/design/shared/work-actions.adoc#holds}); the runtime names them in
 * {@code HoldRecord.acts()}, and the menu, the hold dialogue, and the dispatcher read the same
 * mapping from here, so they cannot drift apart.
 */
public final class HoldActs {

    /** The act that accepts an integrity pause's changed file. */
    public static final String ACCEPT = "accept";

    /** The act that aborts a plan held by an integrity pause. */
    public static final String ABORT = "abort";

    /** The act that ends a held plan. */
    public static final String END_PLAN = "end_plan";

    /** The act that cancels a held epic. */
    public static final String CANCEL_EPIC = "cancel_epic";

    /** The act that shows a failed landing's pull request. */
    public static final String SHOW_PR = "show_pr";

    /** The act that gives up a move's removal from git. */
    public static final String GIVE_UP_REMOVAL = "give_up_removal";

    private HoldActs() {
    }

    /**
     * @param locale the interface locale
     * @param act    the runtime's act
     * @return the localized label of the act
     */
    public static String label(String locale, String act) {
        var key = switch (act) {
            case ACCEPT -> "control.hold.act.accept_change";
            case ABORT -> "control.hold.act.abort_plan";
            case END_PLAN -> "control.hold.act.end_plan";
            case CANCEL_EPIC -> "control.hold.act.cancel_epic";
            case SHOW_PR -> "operator.actions.hold.act.show_pr";
            case GIVE_UP_REMOVAL -> "control.hold.act.give_up_removal";
            case "retry" -> "control.hold.act.retry";
            case "recycle" -> "control.hold.act.recycle";
            case "grant" -> "operator.actions.hold.act.grant";
            case "add_credential" -> "operator.actions.hold.act.add_credential";
            case "raise_budget" -> "operator.actions.hold.act.raise_budget";
            default -> null;
        };
        return key != null ? MessageCatalogue.format(locale, key, Map.of()) : act;
    }

    /**
     * @param act the runtime's act
     * @return the catalogue name of the act's mnemonic
     */
    public static String keyName(String act) {
        return switch (act) {
            case ACCEPT -> "accept_change";
            case ABORT -> "abort_plan";
            default -> act;
        };
    }

    /**
     * @param act the act
     * @return {@code true} when the subject is a landing and not a plan or an epic
     */
    public static boolean isLandingAct(String act) {
        return SHOW_PR.equals(act) || GIVE_UP_REMOVAL.equals(act);
    }
}
