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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.source.lesson.LessonPremiseCheckResult;
import de.planmarshall.api.source.lesson.LessonPremiseEvidence;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator lesson}: verify a lesson's premise before a plan starts from it (UC-STA-8).
 * <p>
 * {@code lesson check <lesson_id>} runs the premise check of a lesson against the repository tree
 * and prints what no longer holds. A plan starts from a lesson with {@code plan add --lesson <id>},
 * whose start offers the recovery of a broken premise ({@code use it anyway}, {@code retire},
 * {@code remove}) as states of the runtime (18-starting.adoc).
 */
@Command(name = "lesson", description = "Verify a lesson's premise before a plan starts from it.",
                    subcommands = {LessonSourceCommand.CheckCommand.class})
@OperatorVerb
public final class LessonSourceCommand implements Callable<Integer> {

    private static final String PATH_PROJECTS_PREFIX = "/api/v1/projects/";
    private static final String SEGMENT_SOURCES_LESSONS = "/sources/lessons/";
    private static final String SUFFIX_PREMISE_CHECK = "/premise-check";
    private static final String NONE = "none";
    private static final String PARAM_LESSON = "lesson";

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

    /**
     * Verifies a lesson's premise against the repository tree.
     *
     * @param context   the operator context
     * @param projectId the project the lesson belongs to
     * @param lessonId  the lesson to check
     * @return the premise check result
     * @throws IOException on transport error or a refused check
     */
    static LessonPremiseCheckResult checkPremise(OperatorContext context, String projectId, String lessonId)
                        throws IOException {
        var response = context.runtime(true).send(HttpRequest.json("POST", lessonPath(projectId, lessonId,
                            SUFFIX_PREMISE_CHECK), "{}"));
        requireOk(response);
        return LessonPremiseCheckResult.fromJson(response.bodyText());
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
                out.println(MessageCatalogue.format("lesson_source.check.holds", Map.of(PARAM_LESSON, lesson)));
            case LessonPremiseCheckResult.STATUS_UNAVAILABLE -> out.println(MessageCatalogue.format(
                                "lesson_source.check.unavailable",
                                Map.of(PARAM_LESSON, lesson, "cause", safe(check.cause()))));
            default -> {
                out.println(MessageCatalogue.format("lesson_source.check.broken", Map.of(PARAM_LESSON, lesson)));
                printEvidence(out, check);
            }
        }
        out.flush();
    }

    private static void printEvidence(PrintWriter out, LessonPremiseCheckResult check) {
        out.println("  " + MessageCatalogue.format("lesson_source.broken.title",
                            Map.of(PARAM_LESSON, safe(check.lessonId()), "count", check.evidence().size())));
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

    private static String lessonPath(String projectId, String lessonId, String suffix) {
        return PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId) + SEGMENT_SOURCES_LESSONS
                            + OperatorUrlUtil.encodePathSegment(lessonId) + suffix;
    }

    private static void requireOk(HttpResponse response) throws IOException {
        if (response.status() < 200 || response.status() >= 300) {
            throw new OperatorRefusedException("lesson check", response);
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
