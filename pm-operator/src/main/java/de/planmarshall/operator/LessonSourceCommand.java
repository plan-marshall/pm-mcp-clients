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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.source.lesson.LessonClaimResult;
import de.planmarshall.api.source.lesson.LessonPremiseCheckResult;
import de.planmarshall.api.source.lesson.LessonPremiseEvidence;
import de.planmarshall.api.source.lesson.LessonRetirementNotice;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator lesson}: verify a lesson's premise before a plan starts from it (UC-STA-8).
 * <p>
 * {@code lesson check <id>} runs the premise check of a lesson against the repository tree and
 * prints what no longer holds. The three-way failure recovery ({@code use it anyway}, {@code
 * retire}, {@code remove}) is offered by {@code start plan --lesson <id>} when the check fails,
 * through {@link #adoptForPlan}.
 */
@Command(name = "lesson", description = "Verify a lesson's premise before a plan starts from it.",
                    subcommands = {LessonSourceCommand.CheckCommand.class})
@OperatorVerb
public final class LessonSourceCommand implements Callable<Integer> {

    private static final String PATH_PROJECTS_PREFIX = "/api/v1/projects/";
    private static final String SEGMENT_SOURCES_LESSONS = "/sources/lessons/";
    private static final String SUFFIX_PREMISE_CHECK = "/premise-check";
    private static final String SUFFIX_CLAIM = "/claim";
    private static final String SUFFIX_RETIRE = "/retire";
    private static final String NONE = "none";

    @Spec
    CommandSpec spec = CommandSpec.create();

    LessonSourceCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    /** The three choices a broken premise offers. */
    enum PremiseChoice {

        /** Carry the broken premise's evidence into the plan and claim the lesson. */
        USE_ANYWAY,

        /** Retire the lesson in the store; nothing is started. */
        RETIRE,

        /** Leave the lesson untouched; nothing is started. */
        REMOVE
    }

    /**
     * Verifies a lesson's premise against the repository tree.
     *
     * @param context  the operator context
     * @param projectId the project the lesson belongs to
     * @param lessonId  the lesson to check
     * @return the premise check result
     * @throws IOException on transport error or a refused check
     */
    static LessonPremiseCheckResult checkPremise(OperatorContext context, String projectId, String lessonId)
                        throws IOException {
        var response = context.runtime(true).send(HttpRequest.json("POST", lessonPath(projectId, lessonId,
                            SUFFIX_PREMISE_CHECK), "{}"));
        requireOk("lesson check", response);
        return LessonPremiseCheckResult.fromJson(response.bodyText());
    }

    /**
     * Claims a lesson for the plan that is about to start.
     *
     * @param context   the operator context
     * @param projectId the project the lesson belongs to
     * @param lessonId  the lesson to claim
     * @param useAnyway whether the broken premise was overridden
     * @param planId    the claiming plan id, or {@code null} to let the runtime assign one
     * @return the claim result
     * @throws IOException on transport error or a refused claim
     */
    static LessonClaimResult claim(OperatorContext context, String projectId, String lessonId,
                        boolean useAnyway, @Nullable String planId) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("use_anyway", useAnyway);
        if (planId != null && !planId.isBlank()) {
            body.put("plan_id", planId);
        }
        var response = context.runtime(true).send(HttpRequest.json("POST", lessonPath(projectId, lessonId,
                            SUFFIX_CLAIM), JsonTree.write(body)));
        requireOk("lesson claim", response);
        return LessonClaimResult.fromJson(response.bodyText());
    }

    /**
     * Retires a lesson in the store.
     *
     * @param context   the operator context
     * @param projectId the project the lesson belongs to
     * @param lessonId  the lesson to retire
     * @param reason    why the lesson is retired, or {@code null}
     * @return the retirement notice
     * @throws IOException on transport error or a refused retirement
     */
    static LessonRetirementNotice retire(OperatorContext context, String projectId, String lessonId,
                        @Nullable String reason) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason);
        }
        var response = context.runtime(true).send(HttpRequest.json("POST", lessonPath(projectId, lessonId,
                            SUFFIX_RETIRE), JsonTree.write(body)));
        requireOk("lesson retire", response);
        return LessonRetirementNotice.fromJson(response.bodyText());
    }

    /**
     * Runs the lesson premise check and claim ahead of a plan start, offering the three-way
     * failure recovery when the premise is broken (UC-STA-8).
     *
     * @param context   the operator context
     * @param out       the client's output
     * @param projectId the project the plan starts in
     * @param lessonId  the lesson the plan starts from
     * @param useAnyway whether the operator already overrode a broken premise
     * @return {@code true} if the plan should proceed, {@code false} if the operator chose to
     *         retire the lesson or to leave it untouched
     * @throws IOException if the claim is refused by the runtime
     */
    static boolean adoptForPlan(OperatorContext context, PrintWriter out, String projectId, String lessonId,
                        boolean useAnyway) throws IOException {
        var check = checkPremise(context, projectId, lessonId);
        printCheck(out, check);
        var override = useAnyway;
        if (!check.holds() && !useAnyway) {
            var choice = promptChoice(context, out, check);
            if (choice == PremiseChoice.RETIRE) {
                var reason = MessageCatalogue.format("lesson_source.retire.reason",
                                    Map.of("count", check.evidence().size()));
                var notice = retire(context, projectId, lessonId, reason);
                out.println("✓ " + MessageCatalogue.format("lesson_source.retired",
                                    Map.of("lesson", safe(notice.lessonId()))));
                out.flush();
                return false;
            }
            if (choice == PremiseChoice.REMOVE) {
                out.println(MessageCatalogue.format("lesson_source.unchanged", Map.of("lesson", safe(lessonId))));
                out.flush();
                return false;
            }
            override = true;
        }
        var result = claim(context, projectId, lessonId, override, null);
        var plan = result.planId() == null ? "" : safe(result.planId());
        out.println("✓ " + MessageCatalogue.format("lesson_source.claimed",
                            Map.of("lesson", safe(result.lessonId()), "plan", plan)));
        out.flush();
        return true;
    }

    /**
     * Prints the outcome of a premise check, with the missing files and symbols of a broken one.
     *
     * @param out   the client's output
     * @param check the premise check result
     */
    static void printCheck(PrintWriter out, LessonPremiseCheckResult check) {
        var lesson = safe(check.lessonId());
        switch (check.status()) {
            case LessonPremiseCheckResult.STATUS_HOLDS ->
                out.println(MessageCatalogue.format("lesson_source.check.holds", Map.of("lesson", lesson)));
            case LessonPremiseCheckResult.STATUS_UNAVAILABLE -> out.println(MessageCatalogue.format(
                                "lesson_source.check.unavailable",
                                Map.of("lesson", lesson, "cause", safe(check.cause()))));
            default -> {
                out.println(MessageCatalogue.format("lesson_source.check.broken", Map.of("lesson", lesson)));
                printEvidence(out, check);
            }
        }
        out.flush();
    }

    private static void printEvidence(PrintWriter out, LessonPremiseCheckResult check) {
        out.println("  " + MessageCatalogue.format("lesson_source.broken.title",
                            Map.of("lesson", safe(check.lessonId()), "count", check.evidence().size())));
        for (var evidence : check.evidence()) {
            out.println("    " + MessageCatalogue.format("lesson_source.evidence.line", evidenceParams(evidence)));
        }
    }

    private static Map<String, Object> evidenceParams(LessonPremiseEvidence evidence) {
        var params = new LinkedHashMap<String, Object>();
        params.put("kind", MessageCatalogue.get("lesson_source.evidence." + evidence.kind().wireValue()));
        params.put("name", safe(evidence.name()));
        params.put("path", evidence.expectedPath() == null ? NONE : safe(evidence.expectedPath()));
        params.put("commit", evidence.lastVerifiedCommit() == null ? NONE : safe(evidence.lastVerifiedCommit()));
        return params;
    }

    private static PremiseChoice promptChoice(OperatorContext context, PrintWriter out,
                        LessonPremiseCheckResult check) throws IOException {
        out.println(MessageCatalogue.format("lesson_source.broken.choice",
                            Map.of("lesson", safe(check.lessonId()))));
        out.println("  1) " + MessageCatalogue.get("lesson_source.broken.use_anyway") + " — "
                            + MessageCatalogue.get("lesson_source.broken.use_anyway_hint"));
        out.println("  2) " + MessageCatalogue.get("lesson_source.broken.retire") + " — "
                            + MessageCatalogue.get("lesson_source.broken.retire_hint"));
        out.println("  3) " + MessageCatalogue.get("lesson_source.broken.remove") + " — "
                            + MessageCatalogue.get("lesson_source.broken.remove_hint"));
        out.flush();
        try (var terminal = context.createTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var line = reader.readLine("1/2/3 \u203a ");
            var clean = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
            return switch (clean) {
                case "1", "u", "use", "y", "yes" -> PremiseChoice.USE_ANYWAY;
                case "2", "r", "retire" -> PremiseChoice.RETIRE;
                default -> PremiseChoice.REMOVE;
            };
        }
    }

    private static String lessonPath(String projectId, String lessonId, String suffix) {
        return PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId) + SEGMENT_SOURCES_LESSONS
                            + OperatorUrlUtil.encodePathSegment(lessonId) + suffix;
    }

    private static void requireOk(String verb, HttpResponse response) throws IOException {
        if (response.status() < 200 || response.status() >= 300) {
            throw new OperatorRefusedException(verb, response);
        }
    }

    private static String safe(@Nullable String value) {
        return OutputSafety.stripControlSequences(value == null ? "" : value);
    }

    @Command(name = "check", description = "Verifies a lesson's premise against the repository.")
    @OperatorVerb
    static final class CheckCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();

        @Parameters(index = "0", arity = "0..1", description = "Lesson id")
        @Nullable
        String lesson;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String projectOption;

        private final OperatorContext context;

        CheckCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            var out = spec.commandLine().getOut();
            if (lesson == null || lesson.isBlank()) {
                out.println(MessageCatalogue.get("lesson_source.action.lesson_required"));
                out.flush();
                return 1;
            }
            var projectId = projectOption != null && !projectOption.isBlank() ? projectOption : "default";
            var check = checkPremise(context, projectId, lesson);
            printCheck(out, check);
            return check.holds() ? 0 : 1;
        }
    }
}
