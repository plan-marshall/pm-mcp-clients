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

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.operator.tui.TuiMenu;
import de.planmarshall.operator.tui.TuiMenuEntry;

/**
 * The open actions menu of one plan, epic, landing, or ended item
 * ({@code doc/design/terminal-client/19-actions.adoc#full-screen}).
 *
 * <p>The menu is modal and carries its own selection; the level below it keeps its row selection.
 * The entries are built from what the runtime offers, never from the client's own idea of what a
 * state allows.
 */
public final class ActionsMenu {

    /** What the menu acts on. */
    public enum Subject {
        /** A plan that has not ended. */
        PLAN,
        /** An epic that has not ended. */
        EPIC,
        /** A failed or held alongside landing. */
        LANDING,
        /** A plan or an epic that has ended: only the read-only chat is offered. */
        ENDED
    }

    private final String subjectId;
    private final Subject subject;
    private final @Nullable String subjectRowId;
    private final List<TuiMenuEntry> entries;
    private int selected;

    /**
     * @param subjectId    the id of the plan, epic, or landing
     * @param subject      what the menu acts on
     * @param subjectRowId the row the menu is drawn below, or {@code null} for the selected row
     * @param entries      the entries, in the order they are drawn
     */
    public ActionsMenu(String subjectId, Subject subject, @Nullable String subjectRowId,
                        List<TuiMenuEntry> entries) {
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.subjectRowId = subjectRowId;
        this.entries = List.copyOf(entries);
        if (this.entries.isEmpty()) {
            throw new IllegalArgumentException("an actions menu needs an entry");
        }
    }

    /** @return the id of the plan, epic, or landing the menu acts on */
    public String subjectId() {
        return subjectId;
    }

    /** @return what the menu acts on */
    public Subject subject() {
        return subject;
    }

    /** @return {@code true} when the subject has ended */
    public boolean ended() {
        return subject == Subject.ENDED;
    }

    /** @return the entries, in the order they are drawn */
    public List<TuiMenuEntry> entries() {
        return entries;
    }

    /** @return the index of the selected entry */
    public int selectedIndex() {
        return selected;
    }

    /**
     * @return the selected entry, or {@code null}
     */
    public @Nullable TuiMenuEntry selected() {
        return selected >= 0 && selected < entries.size() ? entries.get(selected) : null;
    }

    /**
     * Moves the menu's own selection.
     *
     * @param delta the number of entries, negative for up
     */
    public void move(int delta) {
        selected = Math.max(0, Math.min(entries.size() - 1, selected + delta));
    }

    /**
     * @param trigger the mnemonic
     * @return the entry with that mnemonic, or {@code null}
     */
    public @Nullable TuiMenuEntry entry(String trigger) {
        for (var entry : entries) {
            if (entry.trigger().equals(trigger)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * @return the menu as the screen draws it
     */
    public TuiMenu screenMenu() {
        return new TuiMenu(subjectRowId, entries, selected);
    }
}
