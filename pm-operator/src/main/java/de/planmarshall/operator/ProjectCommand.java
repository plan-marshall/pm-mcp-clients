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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
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
            renderProjectDetail(out, locale, project);
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

    private static void renderProjectDetail(PrintWriter out, String locale, EnrolledProject project) {
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
                                Map.of("when", ProjectsCommand.formatRelativeTime(locale, project.lastActive()))));
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

            var startPayload = JsonTree.write(Map.of("path", path, "pilot", pilot));
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/project-onboardings", startPayload));

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

            String entryId = null;
            String projectId = null;
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
                    projectId = JsonTree.string(parsed, "project_id");
                    if (projectId == null) {
                        projectId = JsonTree.string(parsed, "id");
                    }
                }
            } catch (IOException _) {
                // fall back below
            }

            if (entryId == null && projectId == null) {
                try {
                    var name = Path.of(path).getFileName();
                    projectId = name != null ? name.toString() : path;
                } catch (Exception _) {
                    projectId = path;
                }
            }

            var source = entryId != null
                    ? ProjectOnboardingSource.forEntry(context.runtime(true), context.gate(), entryId)
                    : new ProjectOnboardingSource(context.runtime(true), context.gate(), projectId);

            var startPrompt = new StartPrompt(context, out, locale, noInput);
            var dialogue = new StateDialogue(source, out, locale, stateId -> {
                if ("project.identity".equals(stateId) && pilot) {
                    return new ChoicePrompt(new BufferedReader(new StringReader("\n")), out, locale, stateId);
                }
                return new ChoicePrompt(startPrompt, stateId);
            });

            try {
                var finalFrame = dialogue.run();
                return finalFrame.isFinal() ? 0 : EXIT_NEEDS_INPUT;
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
                                stateId -> new ChoicePrompt(startPrompt, stateId));
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

            if (reason == null || reason.isBlank()) {
                if (noInput) {
                    err.println(text(locale, "operator.project.release.reason_required"));
                    err.flush();
                    return EXIT_NEEDS_INPUT;
                }
                // The reason the release request records when the operator gave none: a value of the
                // request the runtime stores, not a text the terminal shows.
                reason = "Released by operator";
            }

            if (!yes && noInput) {
                err.println(text(locale, "operator.project.release.confirm_required"));
                err.flush();
                return EXIT_NEEDS_INPUT;
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
}
