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
 * outside an interactive terminal ({@link OperatorGate}, PM-SEC-3).
 */
@Command(name = "decompose", description = "Opens, or drafts again, the epic's decomposition.")
@OperatorVerb
public final class DecomposeCommand implements Callable<Integer> {

    /** The default project of the decomposition verbs. */
    static final String DEFAULT_PROJECT = "pm-mcp-clients";

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Epic ID")
    String epicId = "";

    @Option(names = "--project", description = "Target project ID (default: " + DEFAULT_PROJECT + ")")
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
        var cut = DecompositionSupport.load(context, DecompositionSupport.project(project), epicId);
        if (cut == null) {
            out.printf("Epic %s not found.%n", OutputSafety.stripControlSequences(epicId));
            out.flush();
            return 1;
        }
        if (instruction != null && !instruction.isBlank()) {
            var requested = new DecompositionInstructRequest(null, instruction, InstructAction.REWRITE);
            cut = DecompositionSupport.instruct(context, DecompositionSupport.project(project), epicId,
                                requested);
            if (cut == null) {
                out.printf("Epic %s not found.%n", OutputSafety.stripControlSequences(epicId));
                out.flush();
                return 1;
            }
        }
        DecompositionSupport.printTree(out, cut);
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

    @Option(names = "--project", description = "Target project ID (default: "
                        + DecomposeCommand.DEFAULT_PROJECT + ")")
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
        var projectId = DecompositionSupport.project(project);
        var cut = DecompositionSupport.check(context, projectId, epicId);
        if (cut == null) {
            out.printf("Epic %s not found.%n", OutputSafety.stripControlSequences(epicId));
            out.flush();
            return 1;
        }
        DecompositionSupport.printTree(out, cut);
        if (!agree) {
            out.flush();
            return 0;
        }
        if (!cut.everyCheckPasses()) {
            out.println("\u2717 " + MessageCatalogue.format("decomposition.checks_fail",
                                Map.of("count", cut.findings().size())));
            out.flush();
            return 1;
        }
        var result = DecompositionSupport.commit(context, projectId, epicId);
        if (result == null) {
            out.println("\u2717 " + MessageCatalogue.format("decomposition.refused",
                                Map.of("epic", OutputSafety.stripControlSequences(epicId),
                                                    "cause", MessageCatalogue.get("decomposition.checks_fail"))));
            out.flush();
            return 1;
        }
        out.println("\u2713 " + MessageCatalogue.format("decomposition.committed",
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

    @Option(names = "--project", description = "Target project ID (default: "
                        + DecomposeCommand.DEFAULT_PROJECT + ")")
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
        if (instruction == null || instruction.isBlank()) {
            out.println(MessageCatalogue.get("decomposition.instruct.prompt") + " (--instruction)");
            out.flush();
            return 1;
        }
        var projectId = DecompositionSupport.project(project);
        var resolvedAction = parseAction(action);
        if (resolvedAction == InstructAction.ADD || resolvedAction == InstructAction.REWRITE) {
            if (instruction.isBlank()) {
                out.println(MessageCatalogue.get("decomposition.instruct.prompt") + " (--instruction)");
                out.flush();
                return 1;
            }
        } else if (plan == null || plan.isBlank()) {
            out.println("--plan is required to edit a staged plan.");
            out.flush();
            return 1;
        }
        var before = DecompositionSupport.load(context, projectId, epicId);
        if (before == null) {
            out.printf("Epic %s not found.%n", OutputSafety.stripControlSequences(epicId));
            out.flush();
            return 1;
        }
        var requested = new DecompositionInstructRequest(plan, instruction, resolvedAction);
        var after = DecompositionSupport.instruct(context, projectId, epicId, requested);
        if (after == null) {
            out.printf("Epic %s not found.%n", OutputSafety.stripControlSequences(epicId));
            out.flush();
            return 1;
        }
        DecompositionSupport.printReadBack(out, before, after, plan);
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
 * The shared HTTP and printing surface of the decomposition verbs.
 */
final class DecompositionSupport {

    private static final String PATH_PROJECTS = "/api/v1/projects/";

    private DecompositionSupport() {
        // Utility class
    }

    static String project(@Nullable String project) {
        return project != null && !project.isBlank() ? project : DecomposeCommand.DEFAULT_PROJECT;
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

    static @Nullable EpicDecomposition instruct(OperatorContext context, String projectId, String epicId,
                        DecompositionInstructRequest request) throws IOException {
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            basePath(projectId, epicId) + "/instruct", request.toJson()));
        if (response.status() == 404) {
            throw new OperatorRefusedException("epic instruct", response);
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic instruct", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    static @Nullable DecompositionCommitResult commit(OperatorContext context, String projectId, String epicId)
                        throws IOException {
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", basePath(projectId, epicId) + "/commit", "{}"));
        if (response.status() == 200) {
            return DecompositionCommitResult.fromJson(response.bodyText());
        }
        return null;
    }

    static void printTree(PrintWriter out, EpicDecomposition cut) {
        var epicId = OutputSafety.stripControlSequences(cut.epicId());
        out.println(MessageCatalogue.format("decomposition.title", Map.of("epic", epicId)));
        for (var workstream : cut.workstreams()) {
            printWorkstream(out, workstream);
        }
        for (var finding : cut.findings()) {
            var mark = finding.severity() == CheckSeverity.ERROR ? "\u2717" : "!";
            out.printf("  %s %s%n", mark, OutputSafety.stripControlSequences(finding.message()));
        }
        if (cut.everyCheckPasses()) {
            out.println("  " + MessageCatalogue.get("decomposition.checks_pass"));
        } else {
            out.println("  " + MessageCatalogue.format("decomposition.checks_fail",
                                Map.of("count", cut.findings().size())));
        }
    }

    private static void printWorkstream(PrintWriter out, WorkstreamRecord workstream) {
        out.printf("  %s  %s%n", OutputSafety.stripControlSequences(workstream.id()),
                            OutputSafety.stripControlSequences(workstream.name()));
        var index = 0;
        for (var plan : workstream.stagedPlans()) {
            index++;
            printPlan(out, index, plan);
        }
    }

    private static void printPlan(PrintWriter out, int index, StagedPlanRecord plan) {
        var surface = plan.expectedSurface().isEmpty()
                            ? MessageCatalogue.get("decomposition.none")
                            : String.join(", ", plan.expectedSurface());
        var suffix = new StringBuilder();
        if (!plan.dependencies().isEmpty()) {
            suffix.append("  after ").append(String.join(", ", plan.dependencies()));
        }
        if (plan.isLocked()) {
            suffix.append("  ").append(MessageCatalogue.get("decomposition.locked"));
        } else if (!StagedPlanRecord.STATUS_STAGED.equals(plan.status())
                            && !StagedPlanRecord.STATUS_COMMITTED.equals(plan.status())) {
            suffix.append("  ").append(MessageCatalogue.format("decomposition.change",
                                Map.of("change", plan.status())));
        }
        out.printf("    %d) %-28s %-30s%s%n", index,
                            OutputSafety.stripControlSequences(plan.planId()),
                            OutputSafety.stripControlSequences(surface), suffix);
    }

    static void printReadBack(PrintWriter out, EpicDecomposition before, EpicDecomposition after,
                        @Nullable String planId) {
        var target = planId != null ? planId : firstPlanId(after);
        if (target == null) {
            out.println(MessageCatalogue.get("decomposition.checks_pass"));
            return;
        }
        var oldPlan = before.plan(target);
        var newPlan = after.plan(target);
        var title = OutputSafety.stripControlSequences(target);
        out.println(MessageCatalogue.format("decomposition.read_back.title", Map.of("plan", title)));
        if (oldPlan == null || newPlan == null) {
            return;
        }
        diffLine(out, "decomposition.objective", oldPlan.objective(), newPlan.objective());
        diffLine(out, "decomposition.surface", String.join(", ", oldPlan.expectedSurface()),
                            String.join(", ", newPlan.expectedSurface()));
        diffLine(out, "decomposition.depends_on", String.join(", ", oldPlan.dependencies()),
                            String.join(", ", newPlan.dependencies()));
    }

    private static void diffLine(PrintWriter out, String labelKey, @Nullable String before, @Nullable String after) {
        var oldValue = before == null || before.isBlank() ? MessageCatalogue.get("decomposition.none") : before;
        var newValue = after == null || after.isBlank() ? MessageCatalogue.get("decomposition.none") : after;
        if (oldValue.equals(newValue)) {
            return;
        }
        var label = MessageCatalogue.get(labelKey);
        out.printf("  - %-14s %s%n", label, OutputSafety.stripControlSequences(oldValue));
        out.printf("  + %-14s %s%n", label, OutputSafety.stripControlSequences(newValue));
    }

    private static @Nullable String firstPlanId(EpicDecomposition cut) {
        if (cut.workstreams().isEmpty()) {
            return null;
        }
        var plans = cut.workstreams().getFirst().stagedPlans();
        return plans.isEmpty() ? null : plans.getFirst().planId();
    }
}
