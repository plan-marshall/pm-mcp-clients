/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.EntryKind;

/**
 * The action dialogues as the full-screen views call them.
 *
 * <p>The views live in their own packages and know nothing of the dialogues' inputs and outputs;
 * this facade is the one seam between them. It runs the action's dialogue, the hold's acts, and the
 * read-only chat about ended work, including the follow-up plan a chat about ended work opens.
 */
public final class OperatorActions {

    private OperatorActions() {
    }

    /**
     * Runs one action of a plan or an epic.
     *
     * @param context the operator context
     * @param out     the console's output, where the dialogue writes its lines
     * @param kind    {@code plan} or {@code epic}
     * @param id      the subject
     * @param action  the action the runtime offered
     * @param epic    the epic as the view holds it, or {@code null}
     * @param holdId  the hold an act belongs to, or {@code null}
     * @return the dialogue's exit code
     * @throws IOException on transport failure
     */
    public static int run(OperatorContext context, PrintWriter out, String kind, String id, String action,
                        @Nullable EpicResource epic, @Nullable String holdId) throws IOException {
        return WorkActionDialogue.run(context, out, kind, id, action, false, epic, holdId);
    }

    /**
     * Runs a findings or watches review.
     *
     * @param context  the operator context
     * @param out      the console's output
     * @param epicId   the epic
     * @param register {@code findings} or {@code watches}
     * @return the exit code
     * @throws IOException on transport failure
     */
    public static int runReview(OperatorContext context, PrintWriter out, String epicId, String register)
                        throws IOException {
        return ReviewDialogue.run(context, out, epicId, register, false);
    }

    /**
     * Runs the dialogue of what holds a plan.
     *
     * @param context the operator context
     * @param out     the console's output
     * @param planId  the plan
     * @param holdId  the hold, or {@code null} for the first
     * @return the exit code
     * @throws IOException on transport failure
     */
    public static int runHold(OperatorContext context, PrintWriter out, String planId, @Nullable String holdId)
                        throws IOException {
        return HoldDialogue.run(context, out, planId, holdId, false);
    }

    /**
     * Runs the read-only chat about ended work, and starts the follow-up plan its proposal names.
     *
     * @param context   the operator context
     * @param out       the console's output
     * @param kind      {@code plan} or {@code epic}
     * @param id        the ended subject
     * @param projectId the project the follow-up is started in, or {@code null}
     * @param locale    the interface locale
     * @return the exit code
     * @throws IOException on transport failure
     */
    public static int chatAboutEnded(OperatorContext context, PrintWriter out, String kind, String id,
                        @Nullable String projectId, String locale) throws IOException {
        var description = WorkChatDialogue.run(context, out, kind, id, true, false);
        if (description == null) {
            return 0;
        }
        out.println(MessageCatalogue.format(locale, "operator.actions.chat.follow_up_started",
                            Map.of("description", OutputSafety.stripControlSequences(description))));
        out.flush();
        var answers = new StartAnswers().withDescription(description);
        if (projectId != null && !projectId.isBlank()) {
            answers.withProject(projectId);
        }
        return StartDialogue.run(context, out, EntryKind.PLAN, answers);
    }
}
