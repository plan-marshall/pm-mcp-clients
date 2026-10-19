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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.decomposition.StagedPlanRecord;
import de.planmarshall.api.decomposition.WorkstreamRecord;
import de.planmarshall.api.message.MessageCatalogue;

/**
 * The third step of starting an epic: its decomposition (design <em>Terminal Client: Starting
 * Work</em>, the decomposition). The planning role's draft is shown with its checks; a plan opens
 * for its detail, the operator's words rewrite one plan or the whole draft, and <em>Enter</em>
 * agrees the cut once every check passes.
 * <p>
 * How many plans run at once is never asked here: the epic takes the project's
 * {@code epic.parallelization_scope}, which the ready step showed as the value in force.
 */
final class EpicDecompositionDialogue {

    private static final String KEY_CHECKED = "decomposition.checks_pass";
    private static final String KEY_CHECKS_FAIL = "decomposition.checks_fail";
    private static final String KEY_PLAN_OPTION = "start.decomposition.plan_option";
    private static final String KEY_PROMPT = "start.decomposition.prompt";
    private static final String KEY_PLAN_PROMPT = "start.decomposition.plan_prompt";
    private static final String KEY_INSTRUCT = "start.decomposition.instruct_prompt";
    private static final String KEY_READ_BACK_ACCEPT = "start.decomposition.read_back_accept";
    private static final String KEY_LEFT = "start.decomposition.left_kept";
    private static final String KEY_UNAVAILABLE = "start.decomposition.unavailable";
    private static final String KEY_DONE = "start.decomposition.done";
    private static final String KEY_OBJECTIVE = "decomposition.objective";
    private static final String KEY_SURFACE = "decomposition.surface";
    private static final String KEY_DEPENDS = "decomposition.depends_on";
    private static final String KEY_NONE = "decomposition.none";
    private static final String KEY_WORKSTREAM = "decomposition.workstream_label";
    private static final String STATE_ID = "start.step.decomposition";
    private static final String KEY_COUNT = "count";
    private static final String KEY_CHECKS = "checks";
    private static final String KEY_PLAN = "plan";

    private final StartApi api;
    private final StartPrompt prompt;
    private final String project;
    private final String epicId;
    private final @Nullable String instruction;

    /**
     * @param api         the start contract
     * @param prompt      the dialogue's input and output surface
     * @param project     the project
     * @param epicId      the epic
     * @param instruction the words of {@code draft again}, or {@code null}
     */
    EpicDecompositionDialogue(StartApi api, StartPrompt prompt, String project, String epicId,
                        @Nullable String instruction) {
        this.api = Objects.requireNonNull(api, "api");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.project = Objects.requireNonNull(project, "project");
        this.epicId = Objects.requireNonNull(epicId, "epicId");
        this.instruction = instruction;
    }

    /**
     * Runs the decomposition dialogue.
     *
     * @return the exit code
     * @throws IOException on transport failure
     */
    int run() throws IOException {
        var cut = api.decomposition(project, epicId);
        if (cut == null) {
            prompt.raw(text(KEY_UNAVAILABLE, Map.of("epic", clean(epicId))));
            return 0;
        }
        if (instruction != null && !instruction.isBlank()) {
            var after = api.instruct(project, epicId,
                                new DecompositionInstructRequest(null, instruction, InstructAction.REWRITE));
            prompt.raw(readBack(cut, after, null));
            cut = after;
        }
        if (!prompt.asks()) {
            show(cut);
            prompt.raw(text(KEY_LEFT, Map.of("epic", clean(epicId))));
            return 0;
        }
        return loop(cut);
    }

    private int loop(EpicDecomposition start) throws IOException {
        var current = start;
        while (true) {
            show(current);
            var answer = prompt.askRaw(options(current), STATE_ID,
                                text("decomposition.agree", Map.of()), epicId);
            if (answer.isEmpty()) {
                var checked = agree();
                if (checked == null) {
                    return 0;
                }
                current = checked;
            } else if ("q".equalsIgnoreCase(answer)) {
                prompt.raw(text(KEY_LEFT, Map.of("epic", clean(epicId))));
                return 0;
            } else if ("r".equalsIgnoreCase(answer)) {
                current = rewrite(current);
            } else if ("+".equalsIgnoreCase(answer)) {
                current = add(current);
            } else {
                var target = planAt(current, answer);
                if (target != null) {
                    current = openPlan(current, target);
                }
            }
        }
    }

    private @Nullable EpicDecomposition agree() throws IOException {
        var checked = api.checkDecomposition(project, epicId);
        if (checked == null) {
            return null;
        }
        if (checked.everyCheckPasses() && api.commit(project, epicId) != null) {
            prompt.raw(text(KEY_DONE, Map.of("epic", clean(epicId),
                                "workstreams", checked.workstreams().size(),
                                "plans", checked.stagedPlanCount())));
            return null;
        }
        return checked;
    }

    private EpicDecomposition rewrite(EpicDecomposition before) throws IOException {
        var words = askWords(KEY_INSTRUCT, Map.of());
        if (words.isEmpty()) {
            return before;
        }
        var after = api.instruct(project, epicId,
                            new DecompositionInstructRequest(null, words, InstructAction.REWRITE));
        prompt.raw(readBack(before, after, null));
        return after;
    }

    private EpicDecomposition add(EpicDecomposition before) throws IOException {
        var words = askWords(KEY_INSTRUCT, Map.of());
        if (words.isEmpty()) {
            return before;
        }
        var after = api.instruct(project, epicId,
                            new DecompositionInstructRequest(null, words, InstructAction.ADD));
        prompt.raw(readBack(before, after, null));
        return after;
    }

    private EpicDecomposition openPlan(EpicDecomposition cut, StagedPlanRecord plan) throws IOException {
        printPlan(cut, plan);
        var answer = prompt.askRaw(text(KEY_PLAN_PROMPT, Map.of()), STATE_ID,
                            text("decomposition.edit", Map.of()), epicId);
        if ("e".equalsIgnoreCase(answer)) {
            return edit(cut, plan);
        }
        return cut;
    }

    private EpicDecomposition edit(EpicDecomposition before, StagedPlanRecord plan) throws IOException {
        var after = instructEdit(before, plan);
        var answer = readBackDecision();
        while ("i".equalsIgnoreCase(answer)) {
            after = instructEdit(after, plan);
            answer = readBackDecision();
        }
        if ("x".equalsIgnoreCase(answer)) {
            prompt.raw(text("start.decomposition.discarded", Map.of()));
            var reloaded = api.decomposition(project, epicId);
            return reloaded != null ? reloaded : before;
        }
        return after;
    }

    private EpicDecomposition instructEdit(EpicDecomposition before, StagedPlanRecord plan) throws IOException {
        var words = askWords("start.decomposition.instruct.title",
                            Map.of(KEY_PLAN, clean(plan.planId())));
        if (words.isEmpty()) {
            return before;
        }
        var after = api.instruct(project, epicId,
                            new DecompositionInstructRequest(plan.planId(), words, InstructAction.EDIT));
        prompt.raw(readBack(before, after, plan.planId()));
        return after;
    }

    private String readBackDecision() {
        return prompt.askRaw(text(KEY_READ_BACK_ACCEPT, Map.of()), STATE_ID,
                            text("decomposition.accept", Map.of()), epicId);
    }

    private String askWords(String key, Map<String, ?> args) {
        return prompt.askRaw(text(key, args), STATE_ID,
                            text("decomposition.instruct.prompt", Map.of()), epicId);
    }

    private void show(EpicDecomposition cut) {
        prompt.raw(text("start.step.counter", Map.of("step", 3, "total", 3)) + " "
                            + text(STATE_ID, Map.of()) + " \u00b7 " + clean(cut.epicId()));
        var index = 1;
        for (var workstream : cut.workstreams()) {
            prompt.raw("  " + clean(workstream.id()) + "  " + clean(workstream.name()));
            for (var plan : workstream.stagedPlans()) {
                prompt.raw("  " + text(KEY_PLAN_OPTION, Map.of(
                                    "index", index,
                                    "plan", clean(plan.planId()),
                                    "surface", surface(plan),
                                    "suffix", suffix(plan))));
                index++;
            }
        }
        prompt.raw(checkLine(cut));
    }

    private String options(EpicDecomposition cut) {
        var checks = cut.everyCheckPasses() ? text(KEY_CHECKED, Map.of())
                            : text(KEY_CHECKS_FAIL, Map.of(KEY_COUNT, cut.findings().size()));
        return text(KEY_PROMPT, Map.of(KEY_COUNT, cut.stagedPlanCount(), KEY_CHECKS, checks));
    }

    private String checkLine(EpicDecomposition cut) {
        if (cut.everyCheckPasses()) {
            return "  " + text(KEY_CHECKED, Map.of());
        }
        var lines = new ArrayList<String>();
        cut.findings().forEach(finding -> lines.add("  \u2717 " + clean(finding.message())));
        lines.add("  " + text(KEY_CHECKS_FAIL, Map.of(KEY_COUNT, cut.findings().size())));
        return String.join("\n", lines);
    }

    private void printPlan(EpicDecomposition cut, StagedPlanRecord plan) {
        prompt.raw("  " + clean(plan.planId()));
        prompt.raw("    " + text(KEY_OBJECTIVE, Map.of()) + "     " + value(plan.objective()));
        prompt.raw("    " + text(KEY_SURFACE, Map.of()) + "       " + surface(plan));
        prompt.raw("    " + text(KEY_DEPENDS, Map.of()) + "    "
                            + (plan.dependencies().isEmpty() ? text(KEY_NONE, Map.of())
                            : clean(String.join(", ", plan.dependencies()))));
        var workstream = workstreamOf(cut, plan);
        if (workstream != null) {
            prompt.raw("    " + text(KEY_WORKSTREAM, Map.of()) + "    "
                                + clean(workstream.id() + " " + workstream.name()));
        }
    }

    private static @Nullable WorkstreamRecord workstreamOf(EpicDecomposition cut, StagedPlanRecord plan) {
        for (var workstream : cut.workstreams()) {
            if (workstream.stagedPlans().stream().anyMatch(candidate -> candidate.planId().equals(plan.planId()))) {
                return workstream;
            }
        }
        return null;
    }

    private @Nullable StagedPlanRecord planAt(EpicDecomposition cut, String answer) {
        try {
            var index = Integer.parseInt(answer);
            var plans = new ArrayList<StagedPlanRecord>();
            for (var workstream : cut.workstreams()) {
                plans.addAll(workstream.stagedPlans());
            }
            return index >= 1 && index <= plans.size() ? plans.get(index - 1) : null;
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private String surface(StagedPlanRecord plan) {
        return plan.expectedSurface().isEmpty() ? text(KEY_NONE, Map.of())
                            : clean(String.join(", ", plan.expectedSurface()));
    }

    private String suffix(StagedPlanRecord plan) {
        var parts = new ArrayList<String>();
        if (!plan.dependencies().isEmpty()) {
            parts.add(text(KEY_DEPENDS, Map.of()) + " " + clean(String.join(", ", plan.dependencies())));
        }
        if (plan.isLocked()) {
            parts.add(text("decomposition.locked", Map.of()));
        } else if (!StagedPlanRecord.STATUS_STAGED.equals(plan.status())
                            && !StagedPlanRecord.STATUS_COMMITTED.equals(plan.status())) {
            parts.add(text("decomposition.change", Map.of("change", clean(plan.status()))));
        }
        return parts.isEmpty() ? "" : "  " + String.join(" \u00b7 ", parts);
    }

    private String value(@Nullable String value) {
        return value == null || value.isBlank() ? text(KEY_NONE, Map.of()) : clean(value);
    }

    private String readBack(EpicDecomposition before, EpicDecomposition after, @Nullable String planId) {
        var buffer = new StringWriter();
        try (var writer = new PrintWriter(buffer)) {
            DecompositionSupport.printReadBack(writer, before, after, planId);
        }
        return buffer.toString().stripTrailing();
    }

    private static String clean(@Nullable String value) {
        return TerminalSanitizer.sanitizeLine(value != null ? value : "");
    }

    private String text(String key, Map<String, ?> args) {
        return MessageCatalogue.format(prompt.locale(), key, args);
    }
}
