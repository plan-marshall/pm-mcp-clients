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
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.decomposition.CheckSeverity;
import de.planmarshall.api.decomposition.DecompositionCommitResult;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.decomposition.StagedPlanRecord;
import de.planmarshall.api.decomposition.WorkstreamRecord;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator epic decompose}: opens, reviews, shapes, and agrees an epic's decomposition
 * (UC-STA-6).
 * <p>
 * {@code decompose} shows the cut and continues a left decomposition; {@code review} runs the
 * checks and, with {@code --agree}, commits the cut; {@code instruct} passes the operator's words
 * for a plan and prints the read-back, old against new. Every verb is an operator verb and refuses
 * outside an interactive terminal ({@link OperatorGate}, PM-SEC-3). The epic id names the epic
 * ({@code 01-commands.adoc}); its project is the one the runtime's epic names, and
 * {@code --project} names it where the operator wants to.
 */
@Command(name = "decompose", description = "Opens, or drafts again, the epic's decomposition.")
@OperatorVerb
public final class DecomposeCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Epic ID")
    String epicId = "";

    @Option(names = "--project", description = "Target project ID (default: the epic's own)")
    @Nullable
    String project;

    @Option(names = "--instruction", description = "The words of 'draft again'")
    @Nullable
    String instruction;

    private final OperatorContext context;

    DecomposeCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var projectId = DecompositionSupport.project(context, project, epicId);
        var cut = projectId != null ? DecompositionSupport.load(context, projectId, epicId) : null;
        if (projectId == null || cut == null) {
            return DecompositionSupport.notFound(out, epicId);
        }
        if (instruction != null && !instruction.isBlank()) {
            var requested = new DecompositionInstructRequest(null, instruction, InstructAction.REWRITE);
            cut = DecompositionSupport.instruct(context, projectId, epicId, requested);
        }
        DecompositionSupport.printTree(out, context.clientEnvironment().locale(), cut);
        out.flush();
        return 0;
    }
}

/**
 * {@code pm-operator epic review}: runs the decomposition's checks and agrees the cut.
 */
@Command(name = "review", description = "Checks the decomposition and agrees the cut.")
@OperatorVerb
final class ReviewCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Epic ID")
    String epicId = "";

    @Option(names = "--project", description = "Target project ID (default: the epic's own)")
    @Nullable
    String project;

    @Option(names = "--agree", description = "Agree and commit the cut once every check passes")
    boolean agree;

    private final OperatorContext context;

    ReviewCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();
        var projectId = DecompositionSupport.project(context, project, epicId);
        var cut = projectId != null ? DecompositionSupport.check(context, projectId, epicId) : null;
        if (projectId == null || cut == null) {
            return DecompositionSupport.notFound(out, epicId);
        }
        DecompositionSupport.printTree(out, locale, cut);
        if (!agree) {
            out.flush();
            return 0;
        }
        if (!cut.everyCheckPasses()) {
            out.println("✗ " + MessageCatalogue.format(locale, "decomposition.checks_fail",
                                Map.of("count", cut.findings().size())));
            out.flush();
            return 1;
        }
        // a commit the runtime refuses is its refusal, with the reason it gives
        DecompositionSupport.commit(context, projectId, epicId);
        out.println("✓ " + MessageCatalogue.format(locale, "decomposition.committed",
                            Map.of("epic", OutputSafety.stripControlSequences(cut.epicId()),
                                                "workstreams", cut.workstreams().size(),
                                                "plans", cut.stagedPlanCount())));
        out.flush();
        return 0;
    }
}

/**
 * {@code pm-operator epic instruct}: passes the operator's words for a plan and shows the
 * read-back, old against new.
 */
@Command(name = "instruct", description = "Shapes one staged plan and shows the read-back.")
@OperatorVerb
final class InstructCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Epic ID")
    String epicId = "";

    @Option(names = "--project", description = "Target project ID (default: the epic's own)")
    @Nullable
    String project;

    @Option(names = "--plan", description = "The staged plan the words concern")
    @Nullable
    String plan;

    @Option(names = "--instruction", description = "The operator's words")
    @Nullable
    String instruction;

    @Option(names = "--action", description = "edit, add, or rewrite (default: edit)")
    @Nullable
    String action;

    private final OperatorContext context;

    InstructCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();
        if (instruction == null || instruction.isBlank()) {
            out.println(MessageCatalogue.format(locale, "decomposition.instruct.prompt", Map.of()) + " (--instruction)");
            out.flush();
            return 1;
        }
        var resolvedAction = parseAction(action);
        if (resolvedAction == InstructAction.EDIT && (plan == null || plan.isBlank())) {
            out.println("--plan is required to edit a staged plan.");
            out.flush();
            return 1;
        }
        var projectId = DecompositionSupport.project(context, project, epicId);
        var before = projectId != null ? DecompositionSupport.load(context, projectId, epicId) : null;
        if (projectId == null || before == null) {
            return DecompositionSupport.notFound(out, epicId);
        }
        var requested = new DecompositionInstructRequest(plan, instruction, resolvedAction);
        var after = DecompositionSupport.instruct(context, projectId, epicId, requested);
        DecompositionSupport.printReadBack(out, locale, before, after, plan);
        out.flush();
        return 0;
    }

    private static InstructAction parseAction(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return InstructAction.EDIT;
        }
        return InstructAction.fromWire(value);
    }
}

/**
 * The shared HTTP and printing surface of the decomposition verbs. The resource paths are the runtime's
 * wire contract, not a setting of the client.
 */
@SuppressWarnings("java:S1075")
final class DecompositionSupport {

    private static final String PATH_PROJECTS = "/api/v1/projects/";
    private static final String PATH_EPICS = "/api/v1/epics/";
    private static final String KEY_NONE = "decomposition.none";

    private DecompositionSupport() {
        // Utility class
    }

    /**
     * The project of the epic: the one {@code --project} names, else the one the runtime's epic
     * names ({@code GET /api/v1/epics/{epic_id}}).
     *
     * @param context the operator context
     * @param project the project the operator named, or {@code null}
     * @param epicId  the epic
     * @return the project, or {@code null} where the runtime knows no such epic
     * @throws IOException on transport failure, or the runtime's refusal
     */
    static @Nullable String project(OperatorContext context, @Nullable String project, String epicId)
                        throws IOException {
        if (project != null && !project.isBlank()) {
            return project;
        }
        var response = context.runtime(true).send(HttpRequest.get(PATH_EPICS + OperatorUrlUtil.encodePathSegment(epicId)));
        if (response.status() == 404) {
            return null;
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic " + epicId, response);
        }
        var projectId = JsonTree.string(JsonTree.asObject(JsonTree.parse(response.bodyText())), "project_id");
        if (projectId == null || projectId.isBlank()) {
            throw new OperatorRefusedException("epic " + OutputSafety.stripControlSequences(epicId)
                                + ": the runtime's epic names no project");
        }
        return projectId;
    }

    /**
     * @param out    the verb's writer
     * @param epicId the epic the runtime does not know
     * @return the exit code of an epic not found
     */
    static int notFound(PrintWriter out, String epicId) {
        out.printf("Epic %s not found.%n", OutputSafety.stripControlSequences(epicId));
        out.flush();
        return 1;
    }

    private static String basePath(String projectId, String epicId) {
        return PATH_PROJECTS + OperatorUrlUtil.encodePathSegment(projectId) + "/epics/"
                            + OperatorUrlUtil.encodePathSegment(epicId) + "/decomposition";
    }

    static @Nullable EpicDecomposition load(OperatorContext context, String projectId, String epicId)
                        throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(basePath(projectId, epicId)));
        if (response.status() == 404) {
            return null;
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic decompose", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    static @Nullable EpicDecomposition check(OperatorContext context, String projectId, String epicId)
                        throws IOException {
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", basePath(projectId, epicId) + "/check", "{}"));
        if (response.status() == 404) {
            return null;
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic review", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    /**
     * @return the cut after the words; the verbs read the decomposition first, so an epic the
     *         runtime no longer knows here is its refusal
     */
    static EpicDecomposition instruct(OperatorContext context, String projectId, String epicId,
                        DecompositionInstructRequest request) throws IOException {
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            basePath(projectId, epicId) + "/instruct", request.toJson()));
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic instruct", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    static DecompositionCommitResult commit(OperatorContext context, String projectId, String epicId)
                        throws IOException {
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", basePath(projectId, epicId) + "/commit", "{}"));
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic review", response);
        }
        return DecompositionCommitResult.fromJson(response.bodyText());
    }

    static void printTree(PrintWriter out, String locale, EpicDecomposition cut) {
        var epicId = OutputSafety.stripControlSequences(cut.epicId());
        out.println(MessageCatalogue.format(locale, "decomposition.title", Map.of("epic", epicId)));
        for (var workstream : cut.workstreams()) {
            printWorkstream(out, locale, workstream);
        }
        for (var finding : cut.findings()) {
            var mark = finding.severity() == CheckSeverity.ERROR ? "✗" : "!";
            out.printf("  %s %s%n", mark, OutputSafety.stripControlSequences(finding.message()));
        }
        if (cut.everyCheckPasses()) {
            out.println("  " + MessageCatalogue.format(locale, "decomposition.checks_pass", Map.of()));
        } else {
            out.println("  " + MessageCatalogue.format(locale, "decomposition.checks_fail",
                                Map.of("count", cut.findings().size())));
        }
    }

    private static void printWorkstream(PrintWriter out, String locale, WorkstreamRecord workstream) {
        out.printf("  %s  %s%n", OutputSafety.stripControlSequences(workstream.id()),
                            OutputSafety.stripControlSequences(workstream.name()));
        var index = 0;
        for (var plan : workstream.stagedPlans()) {
            index++;
            printPlan(out, locale, index, plan);
        }
    }

    private static void printPlan(PrintWriter out, String locale, int index, StagedPlanRecord plan) {
        var surface = plan.expectedSurface().isEmpty()
                            ? MessageCatalogue.format(locale, KEY_NONE, Map.of())
                            : String.join(", ", plan.expectedSurface());
        var suffix = new StringBuilder();
        if (!plan.dependencies().isEmpty()) {
            suffix.append("  after ").append(OutputSafety.stripControlSequences(String.join(", ", plan.dependencies())));
        }
        if (plan.isLocked()) {
            suffix.append("  ").append(MessageCatalogue.format(locale, "decomposition.locked", Map.of()));
        } else if (!StagedPlanRecord.STATUS_STAGED.equals(plan.status())
                            && !StagedPlanRecord.STATUS_COMMITTED.equals(plan.status())) {
            suffix.append("  ").append(MessageCatalogue.format(locale, "decomposition.change",
                                Map.of("change", OutputSafety.stripControlSequences(plan.status()))));
        }
        out.printf("    %d) %-28s %-30s%s%n", index,
                            OutputSafety.stripControlSequences(plan.planId()),
                            OutputSafety.stripControlSequences(surface), suffix);
    }

    /**
     * Prints the read-back of the words, old against new: the plan the words named, else the plan
     * they added; words that named no plan and added none or several (a rewrite of the cut) show the cut again.
     *
     * @param out    the writer
     * @param locale the interface locale
     * @param before the cut before the words
     * @param after  the cut after them
     * @param planId the plan the words named, or {@code null}
     */
    static void printReadBack(PrintWriter out, String locale, EpicDecomposition before, EpicDecomposition after,
                        @Nullable String planId) {
        var target = planId != null ? planId : addedPlanId(before, after);
        if (target == null || planId == null && addedPlanCount(before, after) > 1) {
            printTree(out, locale, after);
            return;
        }
        var newPlan = after.plan(target);
        out.println(MessageCatalogue.format(locale, "decomposition.read_back.title",
                            Map.of("plan", OutputSafety.stripControlSequences(target))));
        if (newPlan == null) {
            out.println("  " + MessageCatalogue.format(locale, "decomposition.plan_not_found",
                                Map.of("plan", OutputSafety.stripControlSequences(target))));
            return;
        }
        // a plan the words added had no old values: each line reads none against new
        var oldPlan = before.plan(target);
        diffLine(out, locale, "decomposition.objective", oldPlan != null ? oldPlan.objective() : null,
                            newPlan.objective());
        diffLine(out, locale, "decomposition.surface",
                            oldPlan != null ? String.join(", ", oldPlan.expectedSurface()) : null,
                            String.join(", ", newPlan.expectedSurface()));
        diffLine(out, locale, "decomposition.depends_on",
                            oldPlan != null ? String.join(", ", oldPlan.dependencies()) : null,
                            String.join(", ", newPlan.dependencies()));
    }

    private static void diffLine(PrintWriter out, String locale, String labelKey, @Nullable String before,
                        @Nullable String after) {
        var none = MessageCatalogue.format(locale, KEY_NONE, Map.of());
        var oldValue = before == null || before.isBlank() ? none : before;
        var newValue = after == null || after.isBlank() ? none : after;
        if (oldValue.equals(newValue)) {
            return;
        }
        var label = MessageCatalogue.format(locale, labelKey, Map.of());
        out.printf("  - %-14s %s%n", label, OutputSafety.stripControlSequences(oldValue));
        out.printf("  + %-14s %s%n", label, OutputSafety.stripControlSequences(newValue));
    }

    private static int addedPlanCount(EpicDecomposition before, EpicDecomposition after) {
        var count = 0;
        for (var workstream : after.workstreams()) {
            for (var plan : workstream.stagedPlans()) {
                if (before.plan(plan.planId()) == null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static @Nullable String addedPlanId(EpicDecomposition before, EpicDecomposition after) {
        for (var workstream : after.workstreams()) {
            for (var plan : workstream.stagedPlans()) {
                if (before.plan(plan.planId()) == null) {
                    return plan.planId();
                }
            }
        }
        return null;
    }
}
