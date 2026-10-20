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
 * One level of the full-screen view: the kind of screen it is and the identifier of the item it
 * shows (a project id, a plan id, an epic id), if any.
 *
 * <p>The level carries no rows and no selection; those belong to the {@link TuiViewStack}, so the
 * same level can be drawn again after a refresh without losing where the operator was.
 *
 * @param kind  the kind of level
 * @param id    the id of the project, plan, or epic the level shows, or {@code null} for a root
 * @param title the already localized title shown in the breadcrumb
 */
public record TuiLevel(Kind kind, @Nullable String id, String title) {

    /** The kinds of level of the terminal's views. */
    public enum Kind {
        /** The Overview root: the running work of the machine. */
        OVERVIEW,
        /** The Explorer root: the projects of the machine. */
        PROJECTS,
        /** One project, its plans grouped by plan. */
        PROJECT_BY_PLAN,
        /** One project, its plans grouped by epic. */
        PROJECT_BY_EPIC,
        /** One plan's process. */
        PLAN,
        /** One epic's view. */
        EPIC,
        /** The ended work of a project, or of the machine. */
        ENDED,
        /** The unread reports. */
        REPORTS,
        /** The system view. */
        SYSTEM
    }

    /**
     * @param kind  the kind of level
     * @param id    the id of the shown item, or {@code null}
     * @param title the localized title of the level
     */
    public TuiLevel {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
    }

    /**
     * @param kind  the kind of a root level
     * @param title the localized title of the level
     * @return a root level, which has no item and no level below it
     */
    public static TuiLevel root(Kind kind, String title) {
        return new TuiLevel(kind, null, title);
    }

    /**
     * @param kind  the kind of level
     * @param id    the id of the shown item
     * @param title the localized title of the level
     * @return a level that shows one item
     */
    public static TuiLevel of(Kind kind, String id, String title) {
        return new TuiLevel(kind, Objects.requireNonNull(id, "id"), title);
    }

    /** @return {@code true} when this level is a root of the navigation stack */
    public boolean isRoot() {
        return kind == Kind.PROJECTS || kind == Kind.OVERVIEW;
    }
}
