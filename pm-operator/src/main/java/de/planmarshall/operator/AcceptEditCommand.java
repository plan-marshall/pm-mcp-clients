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

import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.AcceptEditRequest;
import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.StoreDigestComparison;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator accept-edit plan <plan_id> <store>|epic <epic_id> <store>|workspace <store>|lesson
 * <lesson_id>|enrolment <project_id>}: accepts a manual edit of a store, a lesson, or a project's
 * enrolment file, re-baselining its digest or HMAC (02-operator-channel.adoc#operator-accept-edit;
 * PM-IMPL-7).
 * <p>
 * Every form is one act: it reads the digest, renders the file's path and, side by side, the recorded
 * and the current size and SHA-256, and sends the acceptance only after the operator confirms it
 * (PM-SEC-3). It restores nothing and shows no diff, because the prior content is not kept; the
 * confirmation names the SHA-256 the operator saw, and the runtime refuses the acceptance of any other
 * digest. {@code Ctrl+C} at the confirmation leaves with {@code 130}, and an end of input or a failed
 * read never confirms; neither sends anything.
 * <p>
 * The Local API names no route per form. The enrolment file has its own routes beside the project
 * ({@code /api/v1/projects/<project_id>/enrolment/digest|accept-edit}); every store form uses the
 * project store routes ({@code /api/v1/projects/<project_id>/stores/digest|accept-edit}) with the
 * store named relative to the project store root: {@code plans/<plan_id>/<store>},
 * {@code epics/<epic_id>/<store>}, {@code <store>} of the workspace, {@code lessons/<lesson_id>.md}
 * (filesystem-layout/03-project-store.adoc#scope-directories).
 */
@Command(name = "accept-edit",
                    description = "Accepts a manual edit after showing its recorded and current size and SHA-256.",
                    subcommands = {
                                        AcceptEditCommand.PlanForm.class,
                                        AcceptEditCommand.EpicForm.class,
                                        AcceptEditCommand.WorkspaceForm.class,
                                        AcceptEditCommand.LessonForm.class,
                                        AcceptEditCommand.EnrolmentForm.class
                    })
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class AcceptEditCommand implements Callable<Integer> {

    /** Exit code of a verb without its scope form. */
    static final int EXIT_USAGE = 2;

    /** Exit code of a confirmation {@code --no-input} cannot give. */
    static final int EXIT_NEEDS_INPUT = 3;

    private static final String PREFIX_PROJECTS = "/api/v1/projects/";
    private static final String PATH_STORE_DIGEST = "/stores/digest?path=";
    private static final String PATH_STORE_ACCEPT = "/stores/accept-edit";
    private static final String PATH_ENROLMENT_DIGEST = "/enrolment/digest";
    private static final String PATH_ENROLMENT_ACCEPT = "/enrolment/accept-edit";
    private static final String ERR_CONFIRMATION_MISMATCH = "confirmation_mismatch";
    private static final String PROJECT_OPTION = "--project";
    private static final String PROJECT_DESCRIPTION = "Project that owns the scope";
    private static final String NO_INPUT_OPTION = "--no-input";
    private static final String NO_INPUT_DESCRIPTION = "Non-interactive mode: stop at the confirmation";

    private static final int LABEL_WIDTH = 12;
    private static final int RECORDED_WIDTH = 68;
    private static final int CURRENT_WIDTH = 64;
    private static final String SEPARATOR = "-".repeat(LABEL_WIDTH + RECORDED_WIDTH + CURRENT_WIDTH);
    private static final String ROW_FORMAT = "%-" + LABEL_WIDTH + "s%-" + RECORDED_WIDTH + "s%s%n";

    @Spec
    CommandSpec spec = CommandSpec.create();

    /**
     * The verb without its scope form is a usage error.
     *
     * @return {@link #EXIT_USAGE}
     */
    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getErr());
        spec.commandLine().getErr().flush();
        return EXIT_USAGE;
    }

    /**
     * What one form accepts: the routes of its digest and its acceptance, and the store as the
     * operator named it.
     *
     * @param digest the path of the digest request
     * @param accept the path of the acceptance request
     * @param shown  the store as the operator named it, for the messages before the digest answered
     */
    record Target(String digest, String accept, String shown) {

        /**
         * @param project the project that owns the store
         * @param store   the store relative to the project store root
         * @param shown   the store as the operator named it
         * @return the target on the project store routes
         */
        static Target store(String project, String store, String shown) {
            var base = PREFIX_PROJECTS + OperatorUrlUtil.encodePathSegment(project);
            return new Target(base + PATH_STORE_DIGEST + OperatorUrlUtil.encodeQueryValue(store),
                                base + PATH_STORE_ACCEPT, shown);
        }

        /**
         * @param project the project whose enrolment file is accepted
         * @return the target on the enrolment routes
         */
        static Target enrolment(String project) {
            var base = PREFIX_PROJECTS + OperatorUrlUtil.encodePathSegment(project);
            return new Target(base + PATH_ENROLMENT_DIGEST, base + PATH_ENROLMENT_ACCEPT, project);
        }
    }

    /** {@code accept-edit plan <plan_id> <store>}: a store of a plan, relative to its scope directory. */
    @Command(name = "plan", description = "Accepts the manual edit of a plan's store.")
    @OperatorVerb
    static final class PlanForm implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", paramLabel = "<plan_id>", description = "The plan")
        String planId = "";

        @Parameters(index = "1", paramLabel = "<store>", description = "The store, relative to the plan's scope directory")
        String store = "";

        @Option(names = PROJECT_OPTION, required = true, paramLabel = "<project_id>", description = PROJECT_DESCRIPTION)
        String project = "";

        @Option(names = NO_INPUT_OPTION, description = NO_INPUT_DESCRIPTION)
        boolean noInput;

        PlanForm(OperatorContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        @Override
        public Integer call() throws IOException {
            return accept(spec, context, Target.store(project, "plans/" + planId + "/" + store, store), noInput);
        }
    }

    /** {@code accept-edit epic <epic_id> <store>}: a store of an epic, relative to its scope directory. */
    @Command(name = "epic", description = "Accepts the manual edit of an epic's store.")
    @OperatorVerb
    static final class EpicForm implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", paramLabel = "<epic_id>", description = "The epic")
        String epicId = "";

        @Parameters(index = "1", paramLabel = "<store>", description = "The store, relative to the epic's scope directory")
        String store = "";

        @Option(names = PROJECT_OPTION, required = true, paramLabel = "<project_id>", description = PROJECT_DESCRIPTION)
        String project = "";

        @Option(names = NO_INPUT_OPTION, description = NO_INPUT_DESCRIPTION)
        boolean noInput;

        EpicForm(OperatorContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        @Override
        public Integer call() throws IOException {
            return accept(spec, context, Target.store(project, "epics/" + epicId + "/" + store, store), noInput);
        }
    }

    /**
     * {@code accept-edit workspace <store>}: a store of the workspace, relative to the project store
     * root ({@code pending/<entry_id>/entry.json} for a pending entry record).
     */
    @Command(name = "workspace", description = "Accepts the manual edit of a workspace store.")
    @OperatorVerb
    static final class WorkspaceForm implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", paramLabel = "<store>", description = "The store, relative to the project store root")
        String store = "";

        @Option(names = PROJECT_OPTION, required = true, paramLabel = "<project_id>", description = PROJECT_DESCRIPTION)
        String project = "";

        @Option(names = NO_INPUT_OPTION, description = NO_INPUT_DESCRIPTION)
        boolean noInput;

        WorkspaceForm(OperatorContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        @Override
        public Integer call() throws IOException {
            return accept(spec, context, Target.store(project, store, store), noInput);
        }
    }

    /** {@code accept-edit lesson <lesson_id>}: one lesson of the project's lesson store. */
    @Command(name = "lesson", description = "Accepts the manual edit of a lesson.")
    @OperatorVerb
    static final class LessonForm implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", paramLabel = "<lesson_id>", description = "The lesson")
        String lessonId = "";

        @Option(names = PROJECT_OPTION, required = true, paramLabel = "<project_id>", description = PROJECT_DESCRIPTION)
        String project = "";

        @Option(names = NO_INPUT_OPTION, description = NO_INPUT_DESCRIPTION)
        boolean noInput;

        LessonForm(OperatorContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        @Override
        public Integer call() throws IOException {
            var store = "lessons/" + lessonId + ".md";
            return accept(spec, context, Target.store(project, store, store), noInput);
        }
    }

    /** {@code accept-edit enrolment <project_id>}: the project's enrolment file and its HMAC sidecar. */
    @Command(name = "enrolment", description = "Accepts the manual edit of a project's enrolment file.")
    @OperatorVerb
    static final class EnrolmentForm implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", paramLabel = "<project_id>", description = "The project")
        String project = "";

        @Option(names = NO_INPUT_OPTION, description = NO_INPUT_DESCRIPTION)
        boolean noInput;

        EnrolmentForm(OperatorContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        @Override
        public Integer call() throws IOException {
            return accept(spec, context, Target.enrolment(project), noInput);
        }
    }

    /**
     * The act every form shares: digest, comparison, confirmation, acceptance.
     *
     * @param spec    the specification of the form
     * @param context the operator context
     * @param target  what the form accepts
     * @param noInput whether the confirmation cannot be asked
     * @return the exit code
     * @throws IOException if the runtime cannot be reached or refuses the acceptance unexpectedly
     */
    static int accept(CommandSpec spec, OperatorContext context, Target target, boolean noInput) throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        var locale = context.clientEnvironment().locale();
        var shown = OutputSafety.stripControlSequences(target.shown());

        var digestResponse = context.runtime(true).send(HttpRequest.get(target.digest()));
        if (digestResponse.status() == 404) {
            err.println(MessageCatalogue.format(locale, "enrolment.accept.not_found", Map.of("path", shown)));
            err.flush();
            return PmOperator.EXIT_FAILURE;
        }
        if (digestResponse.status() != 200) {
            err.println(MessageCatalogue.format(locale, "enrolment.accept.failed",
                                Map.of("path", shown, "status", digestResponse.status())));
            err.flush();
            return PmOperator.EXIT_FAILURE;
        }

        var comparison = StoreDigestComparison.fromJson(digestResponse.bodyText());
        renderComparison(out, locale, comparison);

        if (!comparison.mismatch()) {
            out.println(MessageCatalogue.format(locale, "enrolment.accept.match", Map.of()));
            out.flush();
            return 0;
        }
        if (noInput) {
            out.println(MessageCatalogue.format(locale, "enrolment.accept.confirm", Map.of()));
            out.println("  " + MessageCatalogue.format(locale, "operator.accept_edit.confirm_required", Map.of()));
            out.flush();
            return EXIT_NEEDS_INPUT;
        }
        if (!confirm(context, locale)) {
            out.println(MessageCatalogue.format(locale, "enrolment.accept.declined", Map.of()));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }

        var request = new AcceptEditRequest(comparison.storePath(), comparison.currentSha256());
        var response = context.runtime(true).send(HttpRequest.json("POST", target.accept(), request.toJson()));
        if (response.status() == 200) {
            var result = AcceptEditResult.fromJson(response.bodyText());
            out.println(MessageCatalogue.format(locale, "enrolment.accept.done",
                                Map.of("sha", OutputSafety.stripControlSequences(result.newSha256()))));
            out.flush();
            return 0;
        }
        if (response.status() == 409) {
            err.println(MessageCatalogue.format(locale, "enrolment.accept.refused",
                                Map.of("reason", refusalReason(locale, response.bodyText()))));
            err.flush();
            return PmOperator.EXIT_FAILURE;
        }
        throw new OperatorRefusedException("accept-edit " + shown, response);
    }

    private static void renderComparison(PrintWriter out, String locale, StoreDigestComparison comparison) {
        var path = OutputSafety.stripControlSequences(comparison.storePath());
        out.println(MessageCatalogue.format(locale, "enrolment.accept.title", Map.of("path", path)));

        var recordedSize = MessageCatalogue.format(locale, "enrolment.accept.bytes",
                            Map.of("size", formatSize(comparison.recordedSize())));
        var currentSize = MessageCatalogue.format(locale, "enrolment.accept.bytes",
                            Map.of("size", formatSize(comparison.currentSize())));
        var recordedSha = OutputSafety.stripControlSequences(comparison.recordedSha256());
        var currentSha = OutputSafety.stripControlSequences(comparison.currentSha256());

        out.printf(Locale.ROOT, ROW_FORMAT,
                            MessageCatalogue.format(locale, "enrolment.accept.metric", Map.of()),
                            MessageCatalogue.format(locale, "enrolment.accept.recorded", Map.of()),
                            MessageCatalogue.format(locale, "enrolment.accept.current", Map.of()));
        out.println(SEPARATOR);
        out.printf(Locale.ROOT, ROW_FORMAT,
                            MessageCatalogue.format(locale, "enrolment.accept.size", Map.of()), recordedSize, currentSize);
        out.printf(Locale.ROOT, ROW_FORMAT,
                            MessageCatalogue.format(locale, "enrolment.accept.sha256", Map.of()), recordedSha, currentSha);
        if (comparison.mismatchDetails() != null) {
            out.println(MessageCatalogue.format(locale, "enrolment.accept.mismatch",
                                Map.of("details", OutputSafety.stripControlSequences(comparison.mismatchDetails()))));
        }
        out.flush();
    }

    private static String formatSize(long size) {
        return String.format(Locale.ROOT, "%,d", size);
    }

    /**
     * @return {@code true} only for a typed yes; an end of input and a failed read never confirm
     * @throws StartPrompt.InterruptException on {@code Ctrl+C}, the exit code table's {@code 130}
     */
    private static boolean confirm(OperatorContext context, String locale) throws IOException {
        try (var lease = context.openTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
            final String line;
            try {
                line = reader.readLine(MessageCatalogue.format(locale, "enrolment.accept.confirm", Map.of()) + " › ");
            } catch (UserInterruptException _) {
                throw new StartPrompt.InterruptException();
            } catch (EndOfFileException | IOError _) {
                return false;
            }
            if (line == null) {
                return false;
            }
            var answer = line.trim().toLowerCase(Locale.ROOT);
            return answer.startsWith("y") || answer.startsWith("j");
        }
    }

    private static String refusalReason(String locale, String body) {
        var reason = ProjectCommand.refusalReason(body);
        if (ERR_CONFIRMATION_MISMATCH.equals(reason)) {
            return MessageCatalogue.format(locale, "enrolment.accept.refused.stale", Map.of());
        }
        return OutputSafety.stripControlSequences(reason);
    }
}
