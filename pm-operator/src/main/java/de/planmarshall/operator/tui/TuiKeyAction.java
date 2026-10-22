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
    /** Close the selected node, or move to its parent. */
    CLOSE("close"),
    /** Back one level, or clear the filter or the help overlay. */
    BACK("back"),
    /** Open every level below the selected node. */
    EXPAND_ALL("expand_all"),
    /** Jump to where the plan currently executes. */
    WHERE("where"),
    /** Show or hide the nodes not in this plan's lane. */
    NON_LANE("not_in_plan"),
    /** Show or hide the token breakdown by phase and by role. */
    TOKENS("tokens"),
    /** Jump to the plan's project. */
    PROJECT("project"),
    /** Open an ended plan's completion report. */
    COMPLETION("completion"),
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
    /** Mark the selected report read. */
    MARK_READ("read"),
    /** Mark every report the level shows read. */
    MARK_ALL_READ("read_all"),
    /** Cycle the system view's models window. */
    WINDOW("window"),
    /** Switch the system view's models grouping. */
    GROUP_BY("by"),
    /** Switch the system view between all projects and one project. */
    PER_PROJECT("per_project"),
    /** Filter the rows. */
    FILTER("filter"),
    /** Read everything again from the runtime. */
    RELOAD("reload"),
    /** Mark or unmark the selected project as a favourite. */
    FAVOURITE("favourite"),
    /** Move the selected favourite down in the operator's order. */
    MOVE_FAVOURITE_DOWN("move_favourite_down"),
    /** Move the selected favourite up in the operator's order. */
    MOVE_FAVOURITE_UP("move_favourite_up"),
    /** Switch the project list between by name and by most used. */
    SORT("sort"),
    /** Leave the explorer and start onboarding a project. */
    ADD_PROJECT("add_project"),
    /** Leave the explorer and start an epic in the selected project. */
    ADD_EPIC("add_epic"),
    /** Leave the explorer and start a plan in the selected project. */
    ADD_PLAN("add_plan"),
    /** Open the selected project's level of the overview. */
    RUNNING_WORK("running_work"),
    /** Leave the explorer and run the project's setup. */
    SETUP("setup"),
    /** Continue the selected onboarding, setup, or open start entry. */
    CONTINUE("continue"),
    /** Abandon the selected onboarding run. */
    ABANDON("abandon"),
    /** Enrol a released project again from its identity. */
    ENROL_AGAIN("enrol_again"),
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
