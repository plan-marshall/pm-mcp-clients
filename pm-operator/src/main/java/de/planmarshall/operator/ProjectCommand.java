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

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectReleaseRequest;
import de.planmarshall.api.resource.UnenrolRequest;
import de.planmarshall.operator.dialogue.ChoicePrompt;
import de.planmarshall.operator.dialogue.ProjectOnboardingSource;
import de.planmarshall.operator.dialogue.StateDialogue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator project}: manages a project's enrolment and lifecycle (01-commands.adoc#commands,
 * 17-projects.adoc).
 */
@Command(name = "project", description = "Manages project enrolments and lifecycle.",
                    subcommands = {
                                        ProjectCommand.AddCommand.class,
                                        ProjectCommand.SetupCommand.class,
                                        ProjectCommand.FavouriteCommand.class,
                                        ProjectCommand.ReleaseCommand.class,
                                        ProjectCommand.UnenrolCommand.class
                    })
@OperatorVerb
public final class ProjectCommand implements Callable<Integer> {

    private static final String PROJECTS_API = "/api/v1/projects/";
    private static final String KEY_PROJECT_NOT_FOUND = "operator.project.not_found";
    private static final String PARAM_PROJECT = "project";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_VALUE = "value";
    private static final String ERR_ENROLMENT_ROOT_CONFLICT = "enrolment_root_conflict";
    private static final String ERR_PROJECT_STORE_ROOT_MISMATCH = "project_store_root_mismatch";
    private static final String ERR_SCOPE_ACTIVE = "scope_active";

    /** The catalogue key of the refusal a run that cannot close its entry reports. */
    private static final String KEY_CLOSE_FAILED = "project.add.close_failed";

    /** The wire member the start of a project's onboarding carries the operator's login environment in. */
    private static final String PARAM_BUILD_ENVIRONMENT = "build_environment";

    /** The width of a confirmation block's label column. */
    private static final int LABEL_WIDTH = 15;

    /** The dialogue width a confirmation block wraps at. */
    private static final int BLOCK_WIDTH = 80;

    /** The exit code of a run that stopped before its done state (01-commands.adoc#exit-codes). */
    static final int EXIT_NEEDS_INPUT = 3;

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", arity = "0..1", description = "Project ID to inspect")
    @Nullable
    String projectId;

    ProjectCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        if (projectId == null || projectId.isBlank()) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }

        var safeId = OperatorUrlUtil.encodePathSegment(projectId);
        var response = context.runtime(true).send(HttpRequest.get(PROJECTS_API + safeId));
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();

        if (response.status() == 404) {
            out.println(MessageCatalogue.format(locale, KEY_PROJECT_NOT_FOUND,
                                Map.of(PARAM_PROJECT, OutputSafety.stripControlSequences(projectId))));
            out.flush();
            return 1;
        }

        if (response.status() == 200) {
            var project = EnrolledProject.fromJson(response.bodyText());
            renderProjectDetail(out, locale, project, context.clock());
            return 0;
        }

        throw new OperatorRefusedException("project " + OutputSafety.stripControlSequences(projectId), response);
    }

    /**
     * @param locale the client's locale
     * @param key    the catalogue key of a text without parameters
     * @return the text in the client's locale
     */
    private static String text(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }

    private static void renderProjectDetail(PrintWriter out, String locale, EnrolledProject project,
                        Clock clock) {
        var name = OutputSafety.stripControlSequences(project.displayName());
        var pId = OutputSafety.stripControlSequences(project.projectId());
        var remote = project.remoteUrl() != null ? OutputSafety.stripControlSequences(project.remoteUrl()) : "";
        var readiness = switch (project.readiness()) {
            case READY -> text(locale, "operator.project.readiness.ready");
            case RELEASED -> text(locale, "operator.project.readiness.released");
            case SETUP_NEEDS -> MessageCatalogue.format(locale, "operator.project.readiness.setup_needs",
                                Map.of("summary", OutputSafety.stripControlSequences(project.setupNeedsSummary() != null
                                                    ? project.setupNeedsSummary()
                                                    : text(locale, "operator.project.readiness.setup"))));
        };
        var yes = text(locale, "operator.project.detail.yes");
        var no = text(locale, "operator.project.detail.no");

        out.println(MessageCatalogue.format(locale, "operator.project.detail.project", Map.of("name", name, "id", pId)));
        if (!remote.isBlank()) {
            out.println(MessageCatalogue.format(locale, "operator.project.detail.remote", Map.of("remote", remote)));
        }
        out.println(MessageCatalogue.format(locale, "operator.project.detail.readiness",
                            Map.of("readiness", readiness)));
        if (project.needsOperatorCount() > 0) {
            out.println(MessageCatalogue.format(locale, "operator.project.detail.operator",
                                Map.of("count", String.valueOf(project.needsOperatorCount()))));
        }
        out.println(MessageCatalogue.format(locale, "operator.project.detail.work",
                            Map.of("plans", String.valueOf(project.activePlansCount()),
                                                "epics", String.valueOf(project.activeEpicsCount()))));
        if (project.lastActive() != null) {
            out.println(MessageCatalogue.format(locale, "operator.project.detail.last_seen",
                                Map.of("when", ProjectsCommand.formatRelativeTime(locale, project.lastActive(), clock))));
        }
        out.println(MessageCatalogue.format(locale, "operator.project.detail.favourite", Map.of(PARAM_VALUE,
                            project.isFavourite() ? text(locale, "operator.project.detail.favourite_yes") : no)));
        out.println(MessageCatalogue.format(locale, "operator.project.detail.released",
                            Map.of(PARAM_VALUE, project.isReleased() ? yes : no)));
        out.flush();
    }

    /**
     * {@code project add [<path>] [--pilot]}: the direct verb of a project's onboarding, its
     * arguments the answers of {@code project.checkout} and {@code project.identity} (PM-EXEC-2; CLI
     * &amp; Security § Enrolment, Lifecycle Gate &amp; Release; 17-projects.adoc#commands).
     * <p>
     * It reaches the stores only through the runtime's local API, and the operator gate refuses it
     * outside an interactive terminal or inside a coding-agent environment (PM-SEC-3).
     */
    @Command(name = "add", description = "Enrols a project from the specified or current directory.")
    @OperatorVerb
    static final class AddCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", arity = "0..1", defaultValue = ".",
                            description = "Checkout path to enrol; the working directory by default")
        String path = ".";

        @Option(names = "--pilot", description = "Enrol the project in pilot mode (the lifecycle gate)")
        boolean pilot;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        AddCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var safePath = OutputSafety.stripControlSequences(path);
            var locale = context.clientEnvironment().locale();

            if (noInput && !pilot) {
                err.println(text(locale, "operator.project.add.pilot_required"));
                err.flush();
                return EXIT_NEEDS_INPUT;
            }

            var startPayload = startBody();
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/project-onboardings", startPayload));

            var refusalExit = checkResponse(response, safePath, locale, out, err);
            if (refusalExit != null) {
                return refusalExit;
            }

            Started started = null;
            var startPrompt = new StartPrompt(context, out, locale, noInput);
            try {
                started = resolveSource(response);
                var dialogue = new StateDialogue(started.source(), out, locale, stateId -> {
                    if ("project.identity".equals(stateId) && pilot) {
                        return new ChoicePrompt(new BufferedReader(new StringReader("\n")), out, locale, stateId);
                    }
                    return new ChoicePrompt(startPrompt, stateId);
                }, context.projectSetupWorkingTimeout());
                var finalFrame = dialogue.run();
                return finalFrame.isFinal() ? 0 : EXIT_NEEDS_INPUT;
            } catch (StartPrompt.MissingInputException missing) {
                return stopAtMissingInput(out, locale, missing, started);
            } catch (StartPrompt.InterruptException _) {
                return StartPrompt.EXIT_INTERRUPTED;
            } catch (StartPrompt.StartLeftException _) {
                return 0;
            } catch (IOException _) {
                err.println(MessageCatalogue.format(locale, "project.setup.unreadable",
                                    Map.of(PARAM_PROJECT, safePath)));
                err.flush();
                return 1;
            }
        }

        /**
         * The contract's start body: the checkout path resolved absolutely, and the build environment
         * the operator's login shell captured ({@code 01-cli-and-installer.adoc#client-environment}).
         * {@code --pilot} is no field of the contract; it answers the identity state instead.
         *
         * @return the body as JSON
         */
        private String startBody() {
            var body = new LinkedHashMap<String, Object>();
            body.put("path", Path.of(path).toAbsolutePath().normalize().toString());
            body.put(PARAM_BUILD_ENVIRONMENT, context.clientEnvironment().buildEnvironment());
            return JsonTree.write(body);
        }

        /**
         * Stops the run at a choice it was not given: the state is printed from the catalogue, the
         * started entry is closed so no being-added project is left behind, and the exit code says
         * that the run did not reach its done state.
         *
         * @param out     the verb's writer
         * @param locale  the interface locale
         * @param missing the choice the run was not given
         * @param started the source and the entry the run opened
         * @return the exit code of a run stopped before its done state
         */
        private int stopAtMissingInput(PrintWriter out, String locale, StartPrompt.MissingInputException missing,
                            @Nullable Started started) {
            out.println(MessageCatalogue.format(locale, "project.add.no_input",
                                Map.of("title", stateTitle(locale, missing.state()), "need", missing.need())));
            if (started != null && !closeEntry(started.entryId())) {
                out.println(MessageCatalogue.format(locale, KEY_CLOSE_FAILED,
                                    Map.of("entry", String.valueOf(started.entryId()))));
            }
            out.flush();
            return EXIT_NEEDS_INPUT;
        }

        /**
         * Ends the entry the run opened, so the project does not stay listed as being added: the
         * onboarding's own {@code abandon} route, which changes no enrolment because none exists yet.
         *
         * @param entryId the entry the start opened, or {@code null} when the runtime named none
         * @return whether the entry is closed; a refusal is reported so the operator can abandon it
         */
        private boolean closeEntry(@Nullable String entryId) {
            if (entryId == null) {
                return true;
            }
            try {
                var response = context.runtime(true).send(HttpRequest.delete(
                                    "/api/v1/entries/" + OperatorUrlUtil.encodePathSegment(entryId) + "/abandon"));
                return response.status() == 200 || response.status() == 204;
            } catch (IOException _) {
                return false;
            }
        }

        /**
         * @param locale  the interface locale
         * @param stateId the state the run stopped at
         * @return the state's title, or the state id where the catalogue names none
         */
        private static String stateTitle(String locale, String stateId) {
            var key = stateId + ".title";
            return MessageCatalogue.hasKey(locale, key) ? MessageCatalogue.format(locale, key, Map.of()) : stateId;
        }

        private @Nullable Integer checkResponse(HttpResponse response, String safePath, String locale,
                            PrintWriter out, PrintWriter err) throws IOException {
            if (response.status() == 409) {
                var reason = refusalReason(response.bodyText());
                if (ERR_ENROLMENT_ROOT_CONFLICT.equals(reason)) {
                    out.println(MessageCatalogue.format(locale, "enrolment.enrol.refused.root_conflict",
                                        Map.of("path", safePath)));
                } else if (ERR_PROJECT_STORE_ROOT_MISMATCH.equals(reason)) {
                    out.println(MessageCatalogue.format(locale, "enrolment.enrol.refused.store_root_mismatch",
                                        Map.of("id", safePath)));
                } else {
                    out.println(MessageCatalogue.format(locale, "enrolment.enrol.failed",
                                        Map.of("path", safePath, PARAM_STATUS, response.status())));
                }
                out.flush();
                return 1;
            }
            if (response.status() >= 400 && response.status() < 500) {
                err.println(MessageCatalogue.format(locale, "enrolment.enrol.failed",
                                    Map.of("path", safePath, PARAM_STATUS, response.status())));
                err.flush();
                return 1;
            }
            if (response.status() != 200 && response.status() != 201) {
                throw new OperatorRefusedException("project add " + safePath, response);
            }
            return null;
        }

        private Started resolveSource(HttpResponse response) throws IOException {
            String entryId = null;
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(response.bodyText()));
                if (parsed != null) {
                    var continueLink = JsonTree.string(parsed, "continue_link");
                    if (continueLink != null) {
                        entryId = EntryFlow.entryId(continueLink);
                    }
                    if (entryId == null) {
                        entryId = JsonTree.string(parsed, "entry_id");
                    }
                }
            } catch (IOException _) {
                // no entry named below: the run has no address to continue
            }
            if (entryId == null) {
                // The contract opens an entry and names it; without one there is nothing to continue,
                // and the checkout directory's name is no address the runtime serves.
                throw new IOException("the project onboarding answered no entry");
            }
            return new Started(ProjectOnboardingSource.forEntry(context.runtime(true), context.gate(), entryId),
                                entryId);
        }
    }

    /**
     * The run a start opened: the source its states are read from, and the entry it is addressed by.
     *
     * @param source  the onboarding's states
     * @param entryId the entry the runtime opened, or {@code null} where it named none
     */
    private record Started(StateDialogue.Source source, @Nullable String entryId) {
    }

    @Command(name = "setup", description = "Continues a project's onboarding setup.")
    @OperatorVerb
    static final class SetupCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID, or the entry id with --entry")
        String projectId = "";

        @Parameters(index = "1", arity = "0..1", description = "Setup step name or number")
        @Nullable
        String step;

        /** A run that has not been enrolled yet: the parameter is the entry the continue link names. */
        @Option(names = "--entry", description = "Continue a pre-enrolment run by its entry id")
        boolean entry;

        SetupCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var sanitizedId = OutputSafety.stripControlSequences(projectId);
            var locale = context.clientEnvironment().locale();
            var source = entry
                                ? ProjectOnboardingSource.forEntry(context.runtime(true), context.gate(), projectId)
                                : new ProjectOnboardingSource(context.runtime(true), context.gate(), projectId);
            var startPrompt = new StartPrompt(context, out, locale, false);
            var dialogue = new StateDialogue(source, out, locale,
                                stateId -> new ChoicePrompt(startPrompt, stateId),
                                context.projectSetupWorkingTimeout());
            try {
                if (step != null && !step.isBlank()) {
                    var frame = source.choose(step);
                    out.println(MessageCatalogue.format(locale, "project.setup.step_line",
                                        Map.of("step", OutputSafety.stripControlSequences(step),
                                                            "title", dialogue.title(frame))));
                    out.flush();
                    return 0;
                }
                // A project setup that ends without reaching its final state did not complete: the
                // exit-code table's 3, the same code the onboarding run leaves with.
                return dialogue.run().isFinal() ? 0 : EXIT_NEEDS_INPUT;
            } catch (StartPrompt.InterruptException _) {
                return StartPrompt.EXIT_INTERRUPTED;
            } catch (StartPrompt.StartLeftException _) {
                return 0;
            } catch (IOException _) {
                err.println(MessageCatalogue.format(locale, "project.setup.unreadable",
                                    Map.of(PARAM_PROJECT, sanitizedId)));
                err.flush();
                return 1;
            }
        }
    }

    @Command(name = "favourite", description = "Marks or unmarks a project as favourite.")
    @OperatorVerb
    static final class FavouriteCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        @Option(names = "--off", description = "Remove favourite mark")
        boolean off;

        FavouriteCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);
            var body = new LinkedHashMap<String, Object>();
            body.put("favourite", !off);
            var path = PROJECTS_API + safeId + "/favourite";
            var response = context.runtime(true).send(HttpRequest.json("PUT", path, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                var locale = context.clientEnvironment().locale();
                var state = text(locale, off ? "operator.project.favourite.off" : "operator.project.favourite.on");
                out.println(MessageCatalogue.format(locale, "operator.project.favourite.done",
                                    Map.of(PARAM_PROJECT, sanitizedId, "state", state)));
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "release", description = "Releases a project: no new work starts in it.")
    @OperatorVerb
    static final class ReleaseCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        @Option(names = "--reason", description = "Reason for releasing project")
        @Nullable
        String reason;

        @Option(names = {"-y", "--yes"}, description = "Confirm release without interactive prompt")
        boolean yes;

        @Option(names = "--force", description = "Force release even if short landings are pending")
        boolean force;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        ReleaseCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);
            var locale = context.clientEnvironment().locale();

            if ((reason == null || reason.isBlank()) && noInput) {
                err.println(text(locale, "operator.project.release.reason_required"));
                err.flush();
                return EXIT_NEEDS_INPUT;
            }

            if (!yes && noInput) {
                err.println(text(locale, "operator.project.release.confirm_required"));
                err.flush();
                return EXIT_NEEDS_INPUT;
            }

            if (!yes) {
                var confirmationExit = promptReleaseConfirmation(out, err, locale);
                if (confirmationExit != null) {
                    return confirmationExit;
                }
            } else if (reason == null || reason.isBlank()) {
                reason = text(locale, "project.release.reason_default");
            }

            var req = new ProjectReleaseRequest(projectId, true, force, reason);
            var path = PROJECTS_API + safeId + "/release";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, req.toJson()));
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.project.release.done",
                                    Map.of(PARAM_PROJECT, sanitizedId)));
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                err.println(MessageCatalogue.format(locale, KEY_PROJECT_NOT_FOUND,
                                    Map.of(PARAM_PROJECT, sanitizedId)));
                err.flush();
                return 1;
            }
            if (response.status() == 409) {
                err.println(MessageCatalogue.format(locale, "operator.project.release.pending",
                                    Map.of(PARAM_PROJECT, sanitizedId)));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("release project " + sanitizedId, response);
        }

        private @Nullable Integer promptReleaseConfirmation(PrintWriter out, PrintWriter err, String locale)
                            throws IOException {
            var project = interactiveProject();
            if (project == null) {
                // A release that cannot show what it ends is never confirmed: it would end work blind.
                err.println(MessageCatalogue.format(locale, "project.release.summary_unavailable",
                                    Map.of(PARAM_PROJECT, OutputSafety.stripControlSequences(projectId))));
                err.flush();
                return 1;
            }
            try (var lease = context.openTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
                printReleaseBlock(out, locale, project);
                if (reason == null || reason.isBlank()) {
                    var prompt = text(locale, "project.release.reason") + "  \u203a ";
                    var line = readLineSafe(reader, prompt);
                    reason = line != null && !line.isBlank() ? line.trim()
                                        : text(locale, "project.release.reason_default");
                }
                printRow(out, locale, "project.release.afterwards",
                                    MessageCatalogue.format(locale, "project.release.afterwards_release",
                                                        Map.of(PARAM_PROJECT, OutputSafety.stripControlSequences(projectId))));
                out.flush();
                if (!confirm(out, reader, locale, text(locale, "project.release.action_release"),
                                    text(locale, "project.release.explain"))) {
                    return 0;
                }
                return null;
            } catch (StartPrompt.InterruptException _) {
                return StartPrompt.EXIT_INTERRUPTED;
            }
        }

        /**
         * Prints what a release ends and what it keeps, before the reason and the confirmation are
         * asked ({@code 17-projects.adoc#release}). Each line appears only where there is something
         * to name.
         *
         * @param out     the verb's writer
         * @param locale  the interface locale
         * @param project the project as the explorer reports it, or {@code null}
         */
        private void printReleaseBlock(PrintWriter out, String locale, EnrolledProject project) {
            out.println(MessageCatalogue.format(locale, "project.release.title",
                                Map.of("action", "release",
                                                    PARAM_PROJECT, OutputSafety.stripControlSequences(projectId))));
            var plans = project.activePlansCount();
            var epics = project.activeEpicsCount();
            if (plans > 0 || epics > 0) {
                printRow(out, locale, "project.release.what_ends", MessageCatalogue.format(locale,
                                    "project.release.ends", Map.of("plans", plans, "epics", epics)));
            }
            printRow(out, locale, "project.release.what_stays",
                                MessageCatalogue.format(locale, "project.release.stays", Map.of()));
            out.flush();
        }

        /**
         * @return the project as the explorer reports it, or {@code null} when the read does not answer
         */
        private @Nullable EnrolledProject interactiveProject() {
            try {
                var response = context.runtime(true).send(HttpRequest.get(
                                    PROJECTS_API + OperatorUrlUtil.encodePathSegment(projectId)));
                return response.status() == 200 ? EnrolledProject.fromJson(response.bodyText()) : null;
            } catch (IOException _) {
                return null;
            }
        }
    }

    /**
     * {@code project unenrol <project_id>}: removes a project's enrolment, refused while work of it
     * runs (PM-EXEC-2; 17-projects.adoc#release). The enrolment and nothing else is removed; the
     * project's machine store stays for the operator's cleanup.
     */
    @Command(name = "unenrol", description = "Removes a project's enrolment.")
    @OperatorVerb
    static final class UnenrolCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        @Option(names = {"-y", "--yes"}, description = "Confirm unenrolment without interactive prompt")
        boolean yes;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        UnenrolCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);
            var locale = context.clientEnvironment().locale();

            if (!yes && noInput) {
                err.println(text(locale, "enrolment.unenrol.confirm_required"));
                err.flush();
                return EXIT_NEEDS_INPUT;
            }

            if (!yes) {
                try (var lease = context.openTerminal()) {
                    var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
                    printUnenrolBlock(out, locale);
                    out.flush();
                    if (!confirm(out, reader, locale, text(locale, "project.release.action_unenrol"),
                                        text(locale, "project.release.unenrol_explain"))) {
                        return 0;
                    }
                } catch (StartPrompt.InterruptException _) {
                    return StartPrompt.EXIT_INTERRUPTED;
                }
            }

            var request = new UnenrolRequest(projectId);
            var path = PROJECTS_API + safeId + "/unenrol";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, request.toJson()));
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "enrolment.unenrol.done", Map.of("id", sanitizedId)));
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                err.println(MessageCatalogue.format(locale, "enrolment.unenrol.not_found",
                                    Map.of("id", sanitizedId)));
                err.flush();
                return 1;
            }
            if (response.status() == 409) {
                if (ERR_SCOPE_ACTIVE.equals(refusalReason(response.bodyText()))) {
                    err.println(MessageCatalogue.format(locale, "enrolment.unenrol.scope_active",
                                        Map.of("id", sanitizedId)));
                } else {
                    err.println(MessageCatalogue.format(locale, "enrolment.unenrol.failed",
                                        Map.of("id", sanitizedId, PARAM_STATUS, response.status())));
                }
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("project unenrol " + sanitizedId, response);
        }

        /**
         * Prints what an unenrolment ends and what it keeps, before the confirmation
         * ({@code 17-projects.adoc#release}).
         *
         * @param out    the verb's writer
         * @param locale the interface locale
         */
        private void printUnenrolBlock(PrintWriter out, String locale) {
            var safeId = OutputSafety.stripControlSequences(projectId);
            out.println(MessageCatalogue.format(locale, "project.release.unenrol_title",
                                Map.of(PARAM_PROJECT, safeId)));
            printRow(out, locale, "project.release.what_ends",
                                MessageCatalogue.format(locale, "project.release.afterwards_unenrol",
                                                    Map.of(PARAM_PROJECT, safeId)));
            printRow(out, locale, "project.release.afterwards",
                                MessageCatalogue.format(locale, "project.release.afterwards_unenrol_next",
                                                    Map.of(PARAM_PROJECT, safeId)));
        }
    }

    /**
     * @param body the body of a refusal
     * @return the refusal's reason, or its error code where it names no reason, or the empty string
     */
    static String refusalReason(String body) {
        try {
            var map = JsonTree.asObject(JsonTree.parse(body));
            var reason = JsonTree.string(map, "reason");
            if (reason == null) {
                reason = JsonTree.string(map, "error");
            }
            return reason != null ? reason : "";
        } catch (IOException _) {
            return "";
        }
    }

    private static @Nullable String readLineSafe(LineReader reader, String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (UserInterruptException _) {
            throw new StartPrompt.InterruptException();
        } catch (EndOfFileException | IOError _) {
            return null;
        }
    }

    /**
     * Prints one row of a confirmation block: the label in its column and the value wrapped at the
     * dialogue width, so a long value never runs past it ({@code 17-projects.adoc#release}).
     *
     * @param out      the verb's writer
     * @param locale   the interface locale
     * @param labelKey the catalogue key of the label
     * @param value    the rendered value
     */
    private static void printRow(PrintWriter out, String locale, String labelKey, String value) {
        out.println(row(MessageCatalogue.format(locale, labelKey, Map.of()), value));
    }

    /**
     * @param label the rendered label
     * @param value the rendered value
     * @return the row, its value wrapped and its continuation lines aligned under the label
     */
    private static String row(String label, String value) {
        var lines = wrap(value, BLOCK_WIDTH - LABEL_WIDTH - 2);
        var text = new StringBuilder("  ").append(pad(label, LABEL_WIDTH)).append(lines.get(0));
        for (var line : lines.subList(1, lines.size())) {
            text.append('\n').append(" ".repeat(LABEL_WIDTH + 2)).append(line);
        }
        return text.toString();
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text + " " : text + " ".repeat(width - text.length());
    }

    private static List<String> wrap(String text, int width) {
        var lines = new ArrayList<String>();
        var current = new StringBuilder();
        for (var word : text.split(" ")) {
            if (!current.isEmpty() && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }
        lines.add(current.toString());
        return lines;
    }

    /**
     * Asks the confirmation of a release or an unenrolment: {@code y} yes, {@code n} keeps the
     * project, {@code ?} explains, and anything else asks again with {@code Type y or n.} rather than
     * declining.
     *
     * @param out         the verb's writer
     * @param reader      the open line reader
     * @param locale      the interface locale
     * @param question    the rendered question, without its question mark
     * @param explanation what {@code ?} prints
     * @return {@code true} when the act may run
     */
    private static boolean confirm(PrintWriter out, LineReader reader, String locale, String question,
                        String explanation) {
        var yesText = MessageCatalogue.format(locale, "operator.project.detail.yes", Map.of());
        var keepItText = MessageCatalogue.format(locale, "operator.projects.keep_it", Map.of());
        var prompt = question + "? " + yesKey(locale) + ") " + yesText + "   n) " + keepItText + "   ?   \u203a ";
        while (true) {
            var line = readLineSafe(reader, prompt);
            if (line == null) {
                return false;
            }
            var answer = line.trim();
            if ("?".equals(answer)) {
                out.println("  " + explanation);
                out.flush();
                continue;
            }
            if (Answers.isYes(answer)) {
                return true;
            }
            if (Answers.isNo(answer)) {
                return false;
            }
            out.println(MessageCatalogue.format(locale, "operator.dialogue.consent.type_yn", Map.of()));
            out.flush();
        }
    }

    /**
     * @param locale the interface locale
     * @return the mnemonic of yes in that language: {@code j} where the locale starts with {@code de}
     */
    private static String yesKey(String locale) {
        return locale.toLowerCase(Locale.ROOT).startsWith("de") ? "j" : "y";
    }
}
