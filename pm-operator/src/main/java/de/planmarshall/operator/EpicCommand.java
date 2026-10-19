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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.process.EpicProcess;
import de.planmarshall.api.start.EntryKind;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator epic}: inspects, adds, steps, archives, decomposes, watches, or cancels epics
 * (01-commands.adoc, 19-actions.adoc).
 */
@Command(name = "epic", description = "Inspects or controls epics.",
                    subcommands = {
                                        EpicCommand.AddCommand.class,
                                        EpicCommand.NextCommand.class,
                                        EpicCommand.AutoCommand.class,
                                        DecomposeCommand.class,
                                        ReviewCommand.class,
                                        InstructCommand.class,
                                        EpicCommand.ArchiveCommand.class,
                                        EpicCommand.FindingsCommand.class,
                                        EpicCommand.WatchesCommand.class,
                                        EpicCommand.ChatCommand.class,
                                        EpicCommand.CancelCommand.class
                    })
@OperatorVerb
public final class EpicCommand implements Callable<Integer> {

    private static final String KIND = "epic";
    private static final String EPICS_API = "/api/v1/epics/";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", arity = "0..1", description = "Epic ID")
    @Nullable
    String epicId;

    @Option(names = "--all", description = "Show all details")
    boolean all;

    @Option(names = "--depth", description = "Process tree depth: compact, standard, full")
    @Nullable
    String depth;

    @Option(names = "--watch", description = "Watch updates")
    boolean watch;

    EpicCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if (epicId != null && !epicId.isBlank()) {
            var path = EPICS_API + OperatorUrlUtil.encodePathSegment(epicId) + "/process";
            if (depth != null) {
                path += "?depth=" + depth;
            }
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 404) {
                // Fallback to direct /api/v1/epics/{epicId}
                response = runtime.send(HttpRequest.get(EPICS_API + OperatorUrlUtil.encodePathSegment(epicId)));
            }
            if (response.status() == 200) {
                var body = response.bodyText();
                if (body.contains("\"running_plans\"") || body.contains("\"epic_id\"")) {
                    try {
                        var epic = EpicProcess.fromJson(body);
                        printEpicProcess(out, epic);
                        return 0;
                    }
                    /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
                    catch (Exception ignored) {
                        // Fall through to print bodyText
                    }
                }
                out.println(body);
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                out.printf("Epic %s not found.%n", epicId);
                out.flush();
                return 1;
            }
            throw new OperatorRefusedException("epic " + epicId, response);
        }

        // List running epics
        var response = runtime.send(HttpRequest.get("/api/v1/epics"));
        if (response.status() == 200) {
            var parsed = JsonTree.parse(response.body());
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                ActionSupport.printWorkList(out, "Epics:", "state", list);
            } else {
                out.println("No epics running.");
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("epic list", response);
    }

    private void printEpicProcess(PrintWriter out, EpicProcess epic) {
        var project = OutputSafety.stripControlSequences(epic.project());
        var cleanEpicId = OutputSafety.stripControlSequences(epic.epicId());
        out.printf("%s › %s%n", project, cleanEpicId);

        var state = OutputSafety.stripControlSequences(epic.state());
        var runningCount = epic.runningPlans().size();
        var stagedCount = epic.stagedPlans().size();
        out.printf("%s · epic · %s · %d running · %d staged%n",
                            cleanEpicId, state, runningCount, stagedCount);

        out.printf("  in %-45s status --project %s%n", project, project);

        out.println("  running plans:");
        if (epic.runningPlans().isEmpty()) {
            out.println("    none");
        } else {
            for (var plan : epic.runningPlans()) {
                var planId = OutputSafety.stripControlSequences(plan.planId());
                var phase = OutputSafety.stripControlSequences(plan.currentPhase());
                var pState = OutputSafety.stripControlSequences(plan.phaseState());
                out.printf("    ▶ running   %-25s %s · %s%n", planId, phase, pState);
            }
        }

        out.println("  staged plans:");
        if (epic.stagedPlans().isEmpty()) {
            out.println("    none");
        } else {
            for (var plan : epic.stagedPlans()) {
                var planId = OutputSafety.stripControlSequences(plan.planId());
                var phase = OutputSafety.stripControlSequences(plan.currentPhase());
                var pState = OutputSafety.stripControlSequences(plan.phaseState());
                out.printf("    ○ staged    %-25s %s · %s%n", planId, phase, pState);
            }
        }

        var reviewStatus = epic.reviewReady() ? "ready" : "not ready";
        out.printf("  completion review: %s%n", reviewStatus);

        if (!epic.alongsideLandings().isEmpty()) {
            out.println("  alongside landings:");
            for (var landing : epic.alongsideLandings()) {
                out.printf("    ✓ %s%n", OutputSafety.stripControlSequences(landing));
            }
        }

        out.flush();
    }

    @Command(name = "add", description = "Starts an epic from its sources or adopts one.")
    @OperatorVerb
    static final class AddCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Mixin
        ActionSupport.CommonAddOptions common = new ActionSupport.CommonAddOptions();

        @Option(names = "--adopt", description = "Adopt an existing epic from the ledger")
        @Nullable
        String adopt;

        @Option(names = "--document", description = "Input documents")
        List<String> documents = new ArrayList<>();

        @Option(names = "--issue", description = "Input issue URL or number")
        List<String> issues = new ArrayList<>();

        @Option(names = "--copy", description = "Copy input documents")
        boolean copy;

        @Option(names = "--move", description = "Move input documents")
        boolean move;

        @Option(names = "--no-input", description = "Stop on missing required input")
        boolean noInput;

        @Option(names = "--entry", description = "Continue starting an open entry")
        @Nullable
        String entryId;

        @Option(names = "--instruction", description = "The words of 'draft again' for the decomposition")
        @Nullable
        String instruction;

        AddCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var answers = new StartAnswers()
                                .project(common.project)
                                .description(common.description);
            answers.title = common.title;
            answers.id = common.id;
            answers.adopt = adopt;
            answers.documents.addAll(documents);
            answers.issues.addAll(issues);
            answers.copy = copy;
            answers.move = move;
            answers.noInput = noInput;
            answers.entryId = entryId;
            answers.instruction = instruction;
            return StartDialogue.run(context, spec.commandLine().getOut(), EntryKind.EPIC, answers);
        }
    }

    @Command(name = "next", description = "Advances or plans the next workstream/plan.")
    @OperatorVerb
    static final class NextCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        @Option(names = "--raise-scope", description = "Raise concurrent plan scope")
        boolean raiseScope;

        @Option(names = "--no-raise", description = "Do not raise concurrent plan scope")
        boolean noRaise;

        NextCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = Map.of("raise_scope", raiseScope);

            var path = EPICS_API + OperatorUrlUtil.encodePathSegment(epicId) + "/next";
            var response = context.runtime(true).send(
                                HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ Next plan for %s started%n", epicId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic next", response);
        }
    }

    @Command(name = "auto", description = "Configures epic automatic execution.")
    @OperatorVerb
    static final class AutoCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        @Parameters(index = "1", arity = "0..1", description = "on|off")
        @Nullable
        String mode;

        @Option(names = "--common-files", description = "Max common files allowed")
        @Nullable
        Integer commonFiles;

        AutoCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = new LinkedHashMap<String, Object>();
            if (mode != null) {
                payload.put("enabled", "on".equalsIgnoreCase(mode));
            }
            if (commonFiles != null) {
                payload.put("common_files", commonFiles);
            }
            var path = EPICS_API + OperatorUrlUtil.encodePathSegment(epicId) + "/auto";
            var response = context.runtime(true).send(
                                HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                if (commonFiles != null) {
                    out.printf("✓ Common files allowed for %s: %d%n", epicId, commonFiles);
                } else {
                    out.printf("✓ Automatic for %s: %s%n", epicId, mode);
                }
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic auto", response);
        }
    }

    @Command(name = "archive", description = "Archives an ended epic.")
    @OperatorVerb
    static final class ArchiveCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        ArchiveCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var path = EPICS_API + OperatorUrlUtil.encodePathSegment(epicId) + "/archive";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, "{}"));
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ %s archived%n", epicId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic archive", response);
        }
    }

    @Command(name = "findings", description = "Displays findings across the epic's plans.")
    @OperatorVerb
    static final class FindingsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        FindingsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var endpoint = EPICS_API + OperatorUrlUtil.encodePathSegment(epicId) + "/findings";
            var response = context.runtime(true).send(HttpRequest.get(endpoint));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic findings", response);
        }
    }

    @Command(name = "watches", description = "Displays watches/listeners on an epic.")
    @OperatorVerb
    static final class WatchesCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        WatchesCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var endpoint = EPICS_API + OperatorUrlUtil.encodePathSegment(epicId) + "/watches";
            var response = context.runtime(true).send(HttpRequest.get(endpoint));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic watches", response);
        }
    }

    @Command(name = "chat", description = "Chats with the epic's planning role.")
    @OperatorVerb
    static final class ChatCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        @Option(names = "--message", description = "Message to send")
        @Nullable
        String message;

        ChatCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return ActionSupport.chat(context, spec, KIND, epicId, message);
        }
    }

    @Command(name = "cancel", description = "Cancels an epic.")
    @OperatorVerb
    static final class CancelCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Mixin
        ActionSupport.TerminationOptions options = new ActionSupport.TerminationOptions();

        @Option(names = "--with-plans", description = "Cancel child plans as well")
        boolean withPlans;

        @Option(names = "--without-plans", description = "Leave child plans running")
        boolean withoutPlans;

        CancelCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var effectiveReason = options.reason;
            if (effectiveReason == null || effectiveReason.isBlank()) {
                if (options.noInput) {
                    out.println("Cancellation reason required (--reason)");
                    out.flush();
                    return 3;
                }
                effectiveReason = "Cancelled by operator";
            }
            var payload = new LinkedHashMap<String, Object>();
            payload.put("reason", effectiveReason);
            payload.put("with_plans", !withoutPlans);

            var path = EPICS_API + OperatorUrlUtil.encodePathSegment(options.id != null ? options.id : "") + "/cancel";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s cancelled%n", options.id);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic cancel", response);
        }
    }
}
