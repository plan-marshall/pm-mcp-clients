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

import java.util.List;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatOrigin;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.QueuedRevision;

/**
 * What a chat reads of, and changes in, its subject: the context the chat's role is given
 * ({@code state-catalogue.adoc#chat-context}) and the plan's {@code queued_revision}
 * ({@code state-catalogue.adoc#chat-proposal-states}).
 * <p>
 * The engine implements it from the scenario's fixtures and stores; the chat registry asks it so the
 * chat keeps one implementation of the subject's facts instead of a second one. The write verbs only
 * record the plan's state: the registry announces the events they imply
 * ({@code plan.revision_queued}, {@code activity.changed}) beside its own, so the order of a chat's
 * events stays the chat's.
 */
interface ChatFacts {

    /**
     * Renders what the role of a chat is given of its subject, as message keys {@code chat.knows.*}
     * with their arguments ({@code state-catalogue.adoc#chat-context}, The Context Summary).
     *
     * @param subject what the chat is about
     * @param scope   the chat's scope
     * @return the items, in order; empty where the engine knows nothing of the subject
     */
    List<MessageText> knows(ChatSubject subject, ChatScopeKind scope);

    /**
     * @param planId the plan's id
     * @return the plan's instance version as it is now
     */
    int instanceVersion(String planId);

    /**
     * @param planId the plan's id
     * @return whether the plan still takes a revision: it runs in execute, or in finalize before its
     *         merge
     */
    boolean revisionWindowOpen(String planId);

    /**
     * @param subject the subject an act would be offered on
     * @param act     the act's name
     * @return whether the subject offers the act, so a reply may name it
     *         ({@code state-catalogue.adoc#chat-names-acts})
     */
    boolean offersAct(ChatSubject subject, String act);

    /**
     * @param planId the plan's id
     * @return the plan's queued revision, or {@code null} when none is queued
     */
    @Nullable
    QueuedRevision queuedRevision(String planId);

    /**
     * Records the revision accepted in a chat as the plan's {@code queued_revision}.
     *
     * @param planId   the plan's id
     * @param revision the revision, its chat still open
     */
    void queueRevision(String planId, QueuedRevision revision);

    /**
     * Records that the chat that proposed the queued revision ended, so its {@code chat_open} is
     * false.
     *
     * @param planId the plan's id
     */
    void revisionChatEnded(String planId);

    /**
     * Applies the queued revision at the plan's boundary: the plan's instance version moves on by one
     * and the change records its origin ({@code state-catalogue.adoc#chat-origin}).
     *
     * @param planId the plan's id
     * @param origin the origin of the change
     */
    void applyRevision(String planId, ChatOrigin origin);

    /**
     * Clears the plan's queued revision, refused at its boundary or lapsed.
     *
     * @param planId the plan's id
     */
    void clearRevision(String planId);
}
