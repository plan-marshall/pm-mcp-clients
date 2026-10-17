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
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.source.git.LocalChangesCaptureRequest;
import de.planmarshall.api.source.git.LocalChangesCaptureResult;
import de.planmarshall.api.source.git.LocalChangesPreview;
import de.planmarshall.api.source.git.PrAdoptRequest;
import de.planmarshall.api.source.git.PrAdoptResult;
import de.planmarshall.api.source.git.PullRequestObservation;
import de.planmarshall.api.source.git.ScreeningOutcome;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator git-source}: adopt a pull request in the {@code adopt} lane and capture the
 * checkout's local changes in the {@code land} lane (UC-STA-7, UC-STA-9).
 * <p>
 * The verb observes the pull request before it is adopted (open, writable head, rate limit, and the
 * screening of its external text), and it previews the local changes against their branch before
 * they are captured. A capture is a confirmation gate (PM-SEC-3): {@code --yes} answers it, and
 * {@code --no-input} fails rather than prompt. Both flows are also offered as the options
 * {@code --adopt-pr} and {@code --local-changes} of {@code start plan}.
 */
@Command(name = "git-source", description = "Adopt a pull request or capture the local changes.",
                    subcommands = {
                                        GitSourceCommand.AdoptPrCommand.class,
                                        GitSourceCommand.LocalChangesCommand.class
                    })
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class GitSourceCommand implements Callable<Integer> {

    /** Exit code of a verb that needs an answer and was told not to prompt. */
    static final int EXIT_NEEDS_INPUT = 3;

    private static final String PATH_PROJECTS_PREFIX = "/api/v1/projects/";
    private static final String SUFFIX_PR_OBSERVE = "/sources/pr/observe";
    private static final String SUFFIX_PR_ADOPT = "/sources/pr/adopt";
    private static final String SUFFIX_LOCAL_PREVIEW = "/sources/local-changes/preview";
    private static final String SUFFIX_LOCAL_CAPTURE = "/sources/local-changes/capture";
    private static final String KEY_PR_URL = "pr_url";
    private static final String PR_ACCEPT_FLAGGED = "git_sources.pr.flagged.accept";
    private static final String PR_ACCEPT_UNSCREENED = "git_sources.pr.unscreened.accept";
    private static final String CONFIRM_REQUIRED = "git_sources.confirm.required";
    private static final String REASON_RATE_LIMITED = "rate_limited";
    private static final String REASON_NOT_OPEN = "not_open";
    private static final String REASON_HEAD_NOT_WRITABLE = "head_not_writable";
    private static final String REASON_SCREENING_FLAGGED = "screening_flagged";
    private static final String REASON_UNSCREENED = "unscreened";
    private static final String REASON_CONFIRMATION_REQUIRED = "confirmation_required";
    private static final String REASON_LANDING_FILES = "landing_files";
    private static final String REASON_PREVIEW_STALE = "preview_stale";
    private static final String REASON_NO_LOCAL_CHANGES = "no_local_changes";
    private static final String ACCEPTED_BY_OPERATOR = "accepted by the operator";

    /** The outcome of a confirmation gate. */
    private enum Acceptance {
        ACCEPTED, DECLINED, NEEDS_INPUT
    }

    @Spec
    CommandSpec spec = CommandSpec.create();

    GitSourceCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "adopt-pr", description = "Adopts a pull request in the adopt lane.")
    @OperatorVerb
    static final class AdoptPrCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();

        @Parameters(index = "0", arity = "0..1", description = "Pull request number or URL")
        @Nullable
        String reference;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String projectOption;

        @Option(names = {"-y", "--yes"}, description = "Answer the confirmation with yes")
        boolean yes;

        @Option(names = "--no-input", description = "Do not prompt; fail if a choice is needed")
        boolean noInput;

        @Option(names = "--accept-screening", description = "Adopt a flagged or unscreened pull request with this reason")
        @Nullable
        String acceptScreening;

        private final OperatorContext context;

        AdoptPrCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (reference == null || reference.isBlank()) {
                out.println(MessageCatalogue.get("git_sources.pr.field"));
                out.flush();
                return 1;
            }
            return adoptPr(context, out, resolveProject(projectOption), reference, yes, noInput,
                                acceptScreening);
        }
    }

    @Command(name = "local-changes", description = "Captures the checkout's local changes in the land lane.")
    @OperatorVerb
    static final class LocalChangesCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String projectOption;

        @Option(names = {"-y", "--yes"}, description = "Answer the confirmation with yes")
        boolean yes;

        @Option(names = "--no-input", description = "Do not prompt; fail if a choice is needed")
        boolean noInput;

        private final OperatorContext context;

        LocalChangesCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            return captureLocalChanges(context, out, resolveProject(projectOption), yes, noInput);
        }
    }

    private static String resolveProject(@Nullable String projectOption) {
        return projectOption != null && !projectOption.isBlank() ? projectOption : "default";
    }

    /**
     * Observes and adopts a pull request (UC-STA-7).
     *
     * @param context         the operator context
     * @param out             the output writer
     * @param projectId       the project
     * @param reference       the pull request's number or URL
     * @param yes             whether the confirmation is answered with yes
     * @param noInput         whether prompting is forbidden
     * @param acceptScreening the explicit acceptance of a flagged or unscreened pull request
     * @return the exit code
     * @throws IOException on a network or runtime error
     */
    static int adoptPr(OperatorContext context, PrintWriter out, String projectId, String reference,
                        boolean yes, boolean noInput, @Nullable String acceptScreening) throws IOException {
        var runtime = context.runtime(true);
        var base = PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId);
        var observation = observe(runtime, base, reference);
        printObservation(out, observation);
        var unadoptable = unadoptableReason(observation);
        if (unadoptable != null) {
            out.println("✗ " + MessageCatalogue.get(unadoptable));
            out.flush();
            return 1;
        }

        var bypass = acceptScreening;
        if (observation.screening().needsAcceptance() && (bypass == null || bypass.isBlank())) {
            var acceptance = askAcceptance(context, out, observation, yes, noInput);
            if (acceptance == Acceptance.DECLINED) {
                out.println(MessageCatalogue.get("git_sources.local.declined"));
                out.flush();
                return 0;
            }
            if (acceptance == Acceptance.NEEDS_INPUT) {
                return EXIT_NEEDS_INPUT;
            }
            bypass = ACCEPTED_BY_OPERATOR;
        }
        return sendAdopt(runtime, out, base, projectId, reference, bypass);
    }

    /**
     * Previews and captures the checkout's local changes (UC-STA-9).
     *
     * @param context   the operator context
     * @param out       the output writer
     * @param projectId the project
     * @param yes       whether the confirmation is answered with yes
     * @param noInput   whether prompting is forbidden
     * @return the exit code
     * @throws IOException on a network or runtime error
     */
    static int captureLocalChanges(OperatorContext context, PrintWriter out, String projectId, boolean yes,
                        boolean noInput) throws IOException {
        var runtime = context.runtime(true);
        var base = PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId);
        var preview = previewLocalChanges(runtime, base);
        printPreview(out, preview);
        if (!preview.eligible()) {
            var landing = preview.landing() != null ? preview.landing() : "?";
            out.println("✗ " + MessageCatalogue.format("git_sources.local.ineligible", Map.of("landing", landing)));
            out.println("  " + MessageCatalogue.get("git_sources.local.pm_wf_25"));
            out.flush();
            return 1;
        }

        var confirmation = confirmCapture(out, context, yes, noInput);
        if (confirmation == Acceptance.DECLINED) {
            out.println(MessageCatalogue.get("git_sources.local.declined"));
            out.flush();
            return 0;
        }
        if (confirmation == Acceptance.NEEDS_INPUT) {
            return EXIT_NEEDS_INPUT;
        }
        return sendCapture(runtime, out, base, projectId, preview.previewId());
    }

    private static PullRequestObservation observe(RuntimeClient runtime, String base, String reference)
                        throws IOException {
        var response = runtime.send(HttpRequest.json("POST", base + SUFFIX_PR_OBSERVE,
                            JsonTree.write(Map.of(KEY_PR_URL, reference))));
        if (response.status() != 200) {
            throw new OperatorRefusedException("observe pull request", response);
        }
        return PullRequestObservation.fromJson(response.bodyText());
    }

    private static @Nullable String unadoptableReason(PullRequestObservation observation) {
        if (!observation.open()) {
            return "git_sources.pr.not_open";
        }
        if (!observation.writable()) {
            return "git_sources.pr.head_not_writable";
        }
        if (observation.rateLimit().limited()) {
            return "git_sources.refused.rate_limited";
        }
        return null;
    }

    private static Acceptance askAcceptance(OperatorContext context, PrintWriter out,
                        PullRequestObservation observation, boolean yes, boolean noInput) throws IOException {
        var promptKey = observation.screening() == ScreeningOutcome.FLAGGED ? PR_ACCEPT_FLAGGED
                            : PR_ACCEPT_UNSCREENED;
        if (noInput) {
            out.println("✗ " + MessageCatalogue.get(promptKey));
            out.println("  " + MessageCatalogue.get(CONFIRM_REQUIRED));
            out.flush();
            return Acceptance.NEEDS_INPUT;
        }
        return yes || confirm(context, MessageCatalogue.get(promptKey)) ? Acceptance.ACCEPTED
                            : Acceptance.DECLINED;
    }

    private static int sendAdopt(RuntimeClient runtime, PrintWriter out, String base, String projectId,
                        String reference, @Nullable String bypass) throws IOException {
        var response = runtime.send(HttpRequest.json("POST", base + SUFFIX_PR_ADOPT,
                            new PrAdoptRequest(projectId, reference, bypass).toJson()));
        if (response.status() == 200 || response.status() == 201) {
            var result = PrAdoptResult.fromJson(response.bodyText());
            out.println("✓ " + MessageCatalogue.format("git_sources.pr.adopted",
                                Map.of("name", OutputSafety.stripControlSequences(result.planId()))));
            out.println("    " + laneLine("git_sources.lane.adopt"));
            out.flush();
            return 0;
        }
        return refusal(out, "adopt pull request", response);
    }

    private static LocalChangesPreview previewLocalChanges(RuntimeClient runtime, String base)
                        throws IOException {
        var response = runtime.send(HttpRequest.get(base + SUFFIX_LOCAL_PREVIEW));
        if (response.status() != 200) {
            throw new OperatorRefusedException("preview local changes", response);
        }
        return LocalChangesPreview.fromJson(response.bodyText());
    }

    private static Acceptance confirmCapture(PrintWriter out, OperatorContext context, boolean yes,
                        boolean noInput) throws IOException {
        if (yes) {
            return Acceptance.ACCEPTED;
        }
        if (noInput) {
            out.println(MessageCatalogue.get("git_sources.local.confirm"));
            out.println("  " + MessageCatalogue.get(CONFIRM_REQUIRED));
            out.flush();
            return Acceptance.NEEDS_INPUT;
        }
        return confirm(context, MessageCatalogue.get("git_sources.local.confirm")) ? Acceptance.ACCEPTED
                            : Acceptance.DECLINED;
    }

    private static int sendCapture(RuntimeClient runtime, PrintWriter out, String base, String projectId,
                        @Nullable String previewId) throws IOException {
        var response = runtime.send(HttpRequest.json("POST", base + SUFFIX_LOCAL_CAPTURE,
                            new LocalChangesCaptureRequest(projectId, previewId, true).toJson()));
        if (response.status() == 200 || response.status() == 201) {
            var result = LocalChangesCaptureResult.fromJson(response.bodyText());
            out.println("✓ " + MessageCatalogue.format("git_sources.local.captured",
                                Map.of("name", OutputSafety.stripControlSequences(result.planId()))));
            out.println("    " + MessageCatalogue.format("git_sources.local.snapshot",
                                Map.of("sha", OutputSafety.stripControlSequences(result.snapshotCommit()))));
            out.println("    " + laneLine("git_sources.lane.land"));
            out.flush();
            return 0;
        }
        return refusal(out, "capture local changes", response);
    }

    private static void printObservation(PrintWriter out, PullRequestObservation observation) {
        out.println(MessageCatalogue.format("git_sources.pr.observation", Map.of(
                            "title", OutputSafety.stripControlSequences(observation.title()),
                            "author", OutputSafety.stripControlSequences(observation.author()),
                            "branch", OutputSafety.stripControlSequences(observation.branch()))));
        out.println("  " + MessageCatalogue.format("git_sources.pr.files_untrusted",
                            Map.of("count", observation.untrustedCount())));
    }

    private static void printPreview(PrintWriter out, LocalChangesPreview preview) {
        var branch = preview.branch() != null ? OutputSafety.stripControlSequences(preview.branch()) : "?";
        out.println(MessageCatalogue.format("git_sources.local.preview",
                            Map.of("count", preview.changedFiles().size(), "branch", branch)));
        for (var file : preview.changedFiles()) {
            out.printf("  %s %s %s%n", OutputSafety.stripControlSequences(file.path()),
                                MessageCatalogue.format("git_sources.local.line_additions",
                                                    Map.of("additions", file.additions())),
                                MessageCatalogue.format("git_sources.local.line_deletions",
                                                    Map.of("deletions", file.deletions())));
        }
    }

    private static String laneLine(String laneKey) {
        return MessageCatalogue.format("git_sources.lane", Map.of("lane", MessageCatalogue.get(laneKey)));
    }

    private static int refusal(PrintWriter out, String verb, HttpResponse response)
                        throws IOException {
        if (response.status() == 409) {
            out.println("✗ " + refusalReason(response));
            out.flush();
            return 1;
        }
        throw new OperatorRefusedException(verb, response);
    }

    private static String refusalReason(HttpResponse response) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(response.body()));
        var reason = JsonTree.string(map, "reason");
        if (reason == null) {
            return "refused (HTTP " + response.status() + ")";
        }
        return switch (reason) {
            case REASON_RATE_LIMITED -> MessageCatalogue.get("git_sources.refused.rate_limited");
            case REASON_NOT_OPEN -> MessageCatalogue.get("git_sources.pr.not_open");
            case REASON_HEAD_NOT_WRITABLE -> MessageCatalogue.get("git_sources.pr.head_not_writable");
            case REASON_SCREENING_FLAGGED -> MessageCatalogue.get("git_sources.refused.flagged");
            case REASON_UNSCREENED -> MessageCatalogue.get("git_sources.refused.unscreened");
            case REASON_CONFIRMATION_REQUIRED -> MessageCatalogue.get(CONFIRM_REQUIRED);
            case REASON_LANDING_FILES -> MessageCatalogue.get("git_sources.local.pm_wf_25");
            case REASON_PREVIEW_STALE -> MessageCatalogue.get("git_sources.refused.preview_stale");
            case REASON_NO_LOCAL_CHANGES -> MessageCatalogue.get("git_sources.refused.no_local_changes");
            default -> reason;
        };
    }

    private static boolean confirm(OperatorContext context, String prompt) throws IOException {
        try (var terminal = context.createTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var line = reader.readLine(prompt + " \u203a ");
            if (line == null) {
                return false;
            }
            var answer = line.trim().toLowerCase();
            return answer.startsWith("y") || answer.startsWith("j");
        }
    }
}
