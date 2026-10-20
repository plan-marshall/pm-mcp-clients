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

/**
 * What a key of a full-screen level does. The name of the action is also the suffix of its text
 * key in the message catalogue, {@code operator.overview.keys.<name>} in lower case, so a new
 * action cannot exist without a text for the footer.
 */
public enum TuiKeyAction {

    /** Move the selection up. */
    MOVE_UP("move"),
    /** Move the selection down. */
    MOVE_DOWN("move"),
    /** A page up. */
    PAGE_UP("page"),
    /** A page down. */
    PAGE_DOWN("page"),
    /** The first row. */
    FIRST("first"),
    /** The last row. */
    LAST("last"),
    /** Open the selected row as a level. */
    OPEN("open"),
    /** Back one level, or clear the filter or the help overlay. */
    BACK("back"),
    /** Switch a project's view between by plan and by epic. */
    SWITCH_VIEW("switch_view"),
    /** Open the selected plan's epic. */
    EPIC("epic"),
    /** Open the actions of the selected plan or epic. */
    ACTIONS("actions"),
    /** Open what waits for the operator in the selected row. */
    WAITS("waits"),
    /** Open the ended work. */
    ENDED("ended"),
    /** Open the system level. */
    SYSTEM("system"),
    /** Open the unread reports. */
    REPORTS("reports"),
    /** Filter the rows. */
    FILTER("filter"),
    /** Read everything again from the runtime. */
    RELOAD("reload"),
    /** Show the keys and the selected row's details. */
    HELP("help"),
    /** Leave the view to the prompt. */
    LEAVE("leave");

    private final String textName;

    TuiKeyAction(String textName) {
        this.textName = textName;
    }

    /**
     * @return the suffix of the action's text key below {@code operator.overview.keys.}
     */
    public String textName() {
        return textName;
    }

    /**
     * @return the message-catalogue key of the action's footer text
     */
    public String textKey() {
        return "operator.overview.keys." + textName;
    }
}
