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
import java.util.Map;


import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.RefKind;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnRef;

/**
 * The scripted conversation of a chat of the mock runtime, one per subject kind: what the role answers
 * to the operator's first, second, third and every later message.
 * <p>
 * The first reply shows what a reply can hold: a list, a code block, a table, a fenced Mermaid block,
 * and references to a task, a file with its line, a decision and a plan, after one step. The second
 * reply makes the subject's proposal after the steps {@code search} and {@code check}: a change to the
 * entry's description, a revision of the plan, a change to the epic's staged rows, an answer in a
 * decision's chat, or a follow-up plan of ended work. The third reply is an untrusted turn, plain as
 * every turn from outside the machine is; in an epic's chat it proposes a change to the decomposition
 * draft, which replaces the rows proposal still open. Every later reply is a short answer without a
 * proposal. Every message key a reply writes is one {@link de.planmarshall.api.chat.ChatMessageKeys}
 * maps.
 */
@UtilityClass
final class ChatScripts {

    private static final String STEP_READ = "chat.step.read";
    private static final String STEP_SEARCH = "chat.step.search";
    private static final String STEP_CHECK = "chat.step.check";
    private static final String APPLIES_AFTER_TASK = "chat.applies.after_task";
    private static final String ARG_TARGET = "target";
    private static final String ARG_CHECK = "check";
    private static final String ARG_TASK_ID = "task_id";
    private static final String TASK_ID = "TASK-4";

    private static final String RICH_TEXT = """
            Three points:

            - the reconnect waits `250 ms`
            - TASK-4 has no test

            ```java
            backoff.next();
            ```

            | task | state |
            |------|-------|
            | TASK-4 | open |

            ```mermaid
            flowchart LR
              TASK-3-->TASK-4
            ```""";

    private static final String UNTRUSTED_TEXT = "The issue says: the relay drops the cursor after a restart.";
    private static final String LATER_TEXT = "Nothing more to add; ask on.";

    /**
     * One scripted reply of the role.
     *
     * @param text      the reply's whole text
     * @param format    the format of its text
     * @param untrusted whether the reply came from outside the machine, then always plain
     * @param steps     the steps the role reports, as message keys
     * @param refs      the references the reply names
     * @param proposal  the proposal the reply makes, or {@code null}
     */
    record Reply(String text, TurnFormat format, boolean untrusted, List<MessageText> steps, List<TurnRef> refs,
    @Nullable Proposal proposal) {

        /**
         * @param text      the text
         * @param format    the format
         * @param untrusted the untrusted mark
         * @param steps     the steps, copied
         * @param refs      the references, copied
         * @param proposal  the proposal, or {@code null}
         */
        Reply {
            steps = List.copyOf(steps);
            refs = List.copyOf(refs);
        }
    }

    /**
     * The proposal a scripted reply makes, read back old against new.
     *
     * @param kind    the kind of change
     * @param summary the change in one sentence
     * @param applies when it applies, as a message key, or {@code null}
     * @param before  the text before the change, or {@code null}
     * @param after   the text after the change, or {@code null}
     */
    record Proposal(ProposalKind kind, String summary, @Nullable MessageText applies, @Nullable String before,
    @Nullable String after) {
    }

    /**
     * @param subject the kind of the chat's subject
     * @param scope   the chat's scope
     * @param index   how many replies the role has written in the chat before this one
     * @return the reply the role writes
     */
    Reply reply(ChatSubjectKind subject, ChatScopeKind scope, int index) {
        return switch (index) {
            case 0 -> richReply();
            case 1 -> new Reply("Here is what I would change.", TurnFormat.MARKDOWN, false,
                    List.of(step(STEP_SEARCH, ARG_TARGET, "the tasks"), step(STEP_CHECK, ARG_CHECK, "the build")),
                    List.of(), proposal(subject, scope));
            case 2 -> new Reply(UNTRUSTED_TEXT, TurnFormat.PLAIN, true, List.of(), List.of(),
                    subject == ChatSubjectKind.EPIC && scope != ChatScopeKind.ARCHIVE ? draftProposal() : null);
            default -> new Reply(LATER_TEXT, TurnFormat.MARKDOWN, false, List.of(), List.of(), null);
        };
    }

    private static Reply richReply() {
        return new Reply(RICH_TEXT, TurnFormat.MARKDOWN, false, List.of(step(STEP_READ, ARG_TARGET, "the plan")),
                List.of(TurnRef.of(RefKind.TASK, TASK_ID),
                        TurnRef.of(RefKind.FILE, "pm-relay/src/main/java/de/planmarshall/relay/Reconnect.java:42"),
                        TurnRef.of(RefKind.DECISION, "Q-007"),
                        TurnRef.of(RefKind.PLAN, "fix-relay")),
                null);
    }

    /**
     * The proposal of the second reply: the kind follows the subject, and ended work proposes a
     * follow-up plan.
     */
    private static Proposal proposal(ChatSubjectKind subject, ChatScopeKind scope) {
        if (scope == ChatScopeKind.ARCHIVE) {
            return new Proposal(ProposalKind.FOLLOW_UP, "Start a follow-up plan for the missing test.", null, null,
                    "Add the reconnect test the ended work left out.");
        }
        return switch (subject) {
            case ENTRY -> new Proposal(ProposalKind.DESCRIPTION, "Make the request more precise.", null,
                    "Fix the relay.", "Fix the relay's reconnect: keep the cursor across a restart.");
            case PLAN -> new Proposal(ProposalKind.REVISION, "Add a test for TASK-4.",
                    new MessageText(APPLIES_AFTER_TASK, Map.of(ARG_TASK_ID, TASK_ID)), "4 tasks", "5 tasks");
            case EPIC -> new Proposal(ProposalKind.ROWS, "Stage the cursor plan before the token plan.", null,
                    "relay-token-rotation, relay-cursor-order", "relay-cursor-order, relay-token-rotation");
            default -> new Proposal(ProposalKind.ANSWER, "Answer with option B.", null, null,
                    "Option B: rebase onto main.");
        };
    }

    private static Proposal draftProposal() {
        return new Proposal(ProposalKind.DRAFT, "Split the reconnect workstream in two.", null, "1 workstream",
                "2 workstreams");
    }

    private static MessageText step(String key, String argument, String value) {
        return new MessageText(key, Map.of(argument, value));
    }
}
