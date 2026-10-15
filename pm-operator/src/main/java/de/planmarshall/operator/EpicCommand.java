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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator epic}: manage epics, their child plans, reviews, and lifecycle.
 * Subcommands: {@code add}, {@code next}, {@code auto}, {@code decompose}, {@code archive},
 * {@code findings}, {@code watches}, {@code chat}, {@code cancel}.
 * Without a subcommand, displays epic details or lists epics.
 */
@Command(name = "epic", description = "Epic operations and actions.",
        subcommands = {
                EpicCommand.AddCommand.class,
                EpicCommand.NextCommand.class,
                EpicCommand.AutoCommand.class,
                EpicCommand.DecomposeCommand.class,
                EpicCommand.ArchiveCommand.class,
                EpicCommand.FindingsCommand.class,
                EpicCommand.WatchesCommand.class,
                EpicCommand.ChatCommand.class,
                EpicCommand.CancelCommand.class
        })
@OperatorVerb
public final class EpicCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", arity = "0..1", description = "Epic ID")
    @Nullable
    String epicId;

    @Option(names = "--tokens", description = "Show tokens per plan")
    boolean tokens;

    EpicCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if (epicId != null && !epicId.isBlank()) {
            var path = "/api/v1/epics/" + epicId;
            if (tokens) path += "?tokens=true";
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                out.println(response.bodyText());
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

        // List epics
        var response = runtime.send(HttpRequest.get("/api/v1/epics"));
        if (response.status() == 200) {
            var parsed = JsonTree.parse(response.body());
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                out.println("Epics:");
                for (var item : list) {
                    if (item instanceof Map<?, ?> epic) {
                        var title = epic.get("title");
                        out.printf("  %-20s %-12s %s%n",
                                epic.get("id"),
                                epic.get("state"),
                                title != null ? title : "");
                    } else {
                        out.println("  " + item);
                    }
                }
            } else {
                out.println("No epics running.");
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("epic list", response);
    }

    @Command(name = "add", description = "Starts an epic from its sources or adopts one.")
    @OperatorVerb
    static final class AddCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
        private final OperatorContext context;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--title", description = "Epic title")
        @Nullable
        String title;

        @Option(names = "--id", description = "Explicit epic ID")
        @Nullable
        String id;

        @Option(names = "--adopt", description = "Adopt an existing epic from the ledger")
        @Nullable
        String adopt;

        @Option(names = "--description", description = "Epic description")
        @Nullable
        String description;

        AddCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = new LinkedHashMap<String, Object>();
            if (project != null && !".".equals(project)) {
                payload.put("project", project);
            }
            if (title != null) payload.put("title", title);
            if (id != null) payload.put("id", id);
            if (adopt != null) payload.put("adopt", adopt);
            if (description != null) payload.put("description", description);

            var path = project != null && !".".equals(project)
                    ? "/api/v1/projects/" + project + "/entries"
                    : "/api/v1/epics";

            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 201) {
                var epicName = adopt != null ? adopt : (id != null ? id : (title != null ? title : "Epic"));
                out.printf("✓ %s has started%n", epicName);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic add", response);
        }
    }

    @Command(name = "next", description = "Advances or plans the next workstream/plan.")
    @OperatorVerb
    static final class NextCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/epics/" + epicId + "/next", JsonTree.write(payload)));
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
        CommandSpec spec;
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
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/epics/" + epicId + "/auto", JsonTree.write(payload)));
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

    @Command(name = "decompose", description = "Decomposes the epic again with review.")
    @OperatorVerb
    static final class DecomposeCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        @Option(names = "--instruction", description = "Decomposition instruction")
        @Nullable
        String instruction;

        DecomposeCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = new LinkedHashMap<String, Object>();
            if (instruction != null) payload.put("instruction", instruction);

            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/epics/" + epicId + "/decompose", JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s decomposing%n", epicId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic decompose", response);
        }
    }

    @Command(name = "archive", description = "Archives an ended epic.")
    @OperatorVerb
    static final class ArchiveCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/epics/" + epicId + "/archive", "{}"));
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ %s archived%n", epicId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic archive", response);
        }
    }

    @Command(name = "findings", description = "Reviews findings of the epic.")
    @OperatorVerb
    static final class FindingsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/epics/" + epicId + "/findings"));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic findings", response);
        }
    }

    @Command(name = "watches", description = "Reviews watches of the epic.")
    @OperatorVerb
    static final class WatchesCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/epics/" + epicId + "/watches"));
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
        CommandSpec spec;
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
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (message != null && !message.isBlank()) {
                var response = context.runtime(true).send(
                        HttpRequest.json("POST", "/api/v1/epics/" + epicId + "/chat",
                                JsonTree.write(Map.of("message", message))));
                if (response.status() == 200 || response.status() == 201) {
                    out.println(response.bodyText());
                    out.flush();
                    return 0;
                }
                throw new OperatorRefusedException("epic chat", response);
            }

            var response = context.runtime(true).send(HttpRequest.get("/api/v1/epics/" + epicId + "/chat"));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic chat", response);
        }
    }

    @Command(name = "cancel", description = "Cancels an epic.")
    @OperatorVerb
    static final class CancelCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
        private final OperatorContext context;

        @Parameters(index = "0", description = "Epic ID")
        String epicId = "";

        @Option(names = "--reason", description = "Reason for cancellation")
        @Nullable
        String reason;

        @Option(names = "--with-plans", description = "Cancel child plans as well")
        boolean withPlans;

        @Option(names = "--without-plans", description = "Leave child plans running")
        boolean withoutPlans;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        CancelCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (reason == null || reason.isBlank()) {
                if (noInput) {
                    out.println("Cancellation reason required (--reason)");
                    out.flush();
                    return 3;
                }
                reason = "Cancelled by operator";
            }
            var payload = new LinkedHashMap<String, Object>();
            payload.put("reason", reason);
            payload.put("with_plans", !withoutPlans);

            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/epics/" + epicId + "/cancel", JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s cancelled%n", epicId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("epic cancel", response);
        }
    }
}
