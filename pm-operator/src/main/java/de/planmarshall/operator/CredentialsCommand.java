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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator credentials}: the operator's credential acts (02-operator-channel.adoc#operator-credential-cli).
 * {@code rotate <credential_key>} replaces the secret of an existing entry and keeps its kind and
 * its other fields; {@code set <credential_key>} writes an entry of another kind under the same key;
 * {@code status} reports an entry without revealing the secret; {@code remove} retires it.
 *
 * <p>A secret is read with the echo off into a {@code char[]} ({@link SecretReader}) that is cleared
 * after the request and never reaches an argument, the history, a log, or a {@code String}.
 */
@Command(name = "credentials", description = "The operator's credential acts.",
                    subcommands = {
                                        CredentialsCommand.RotateCommand.class,
                                        CredentialsCommand.SetCommand.class,
                                        CredentialsCommand.StatusCommand.class,
                                        CredentialsCommand.RemoveCommand.class
                    })
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class CredentialsCommand implements Callable<Integer> {

    static final String PATH = "/api/v1/credentials/";
    private static final String SECRET = "secret";
    private static final String KIND = "kind";
    private static final String PROJECT = "project";
    private static final String STATUS = "status";
    private static final String MODE = "mode";
    private static final String ROTATE = "rotate";
    private static final String CREDENTIALS = "credentials";
    private static final String KEY = "key";
    private static final String STORED = "operator.credentials.stored";
    private static final String FAILED = "operator.credentials.failed";
    private static final String READ_FAILED = "operator.credentials.read_failed";
    private static final String REASON = "reason";
    private static final String COUNT = "count";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    /** The kind of the entry, for {@code set}; {@code null} keeps the entry's own. */
    @Nullable
    String entryKind;

    /** The project scope, or {@code null} for the machine. */
    @Nullable
    String projectScope;

    CredentialsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    private String locale() {
        return context.clientEnvironment().locale();
    }

    private static String path(String key) {
        return PATH + OperatorUrlUtil.encodePathSegment(key);
    }

    @Nullable
    private Map<String, Object> readStatus(PrintWriter out, String key) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(path(key)));
        if (response.status() == 200) {
            return JsonTree.asObject(JsonTree.parse(response.body()));
        }
        if (response.status() == 404) {
            return null;
        }
        out.println(MessageCatalogue.format(locale(), "operator.credentials.status_failed",
                            Map.of(KEY, key, STATUS, response.status())));
        out.flush();
        return null;
    }

    private static boolean isPresent(@Nullable Map<String, Object> status) {
        if (status == null) {
            return false;
        }
        var wire = JsonTree.string(status, STATUS);
        return "present".equalsIgnoreCase(wire) || "verified".equalsIgnoreCase(wire)
                            || "unverified".equalsIgnoreCase(wire) || "expiring".equalsIgnoreCase(wire);
    }

    private void printHeader(PrintWriter out, String key, String mode, @Nullable Map<String, Object> status) {
        var locale = locale();
        out.println(MessageCatalogue.format(locale, "onboard.settings.credential.title",
                            Map.of("mode", mode, "key", key)));
        out.println(MessageCatalogue.format(locale, "operator.credentials.entry", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.credentials.entry_value",
                            Map.of(KEY, key, "providers", providerHint(status, locale))));
        out.println(MessageCatalogue.format(locale, ROTATE.equals(mode)
                            ? "onboard.settings.credential.rotate" : "onboard.settings.credential.replace", Map.of()));
        out.println(MessageCatalogue.format(locale, ROTATE.equals(mode)
                                                ? "onboard.settings.credential.rotate_value" : "onboard.settings.credential.replace_value",
                            Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.credentials.now", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.credentials.store_hint",
                            Map.of("store", storeHint(status))));
        out.flush();
    }

    private static String providerHint(@Nullable Map<String, Object> status, String locale) {
        var provider = status != null ? JsonTree.string(status, "provider") : null;
        if (provider != null && !provider.isBlank()) {
            return provider;
        }
        var tokenPath = status != null ? JsonTree.string(status, "token_path") : null;
        return tokenPath != null && !tokenPath.isBlank()
                            ? tokenPath
                            : MessageCatalogue.format(locale, "common.none", Map.of());
    }

    private static String storeHint(@Nullable Map<String, Object> status) {
        var path = status != null ? JsonTree.string(status, "token_path") : null;
        return path != null && !path.isBlank() ? path : "os";
    }

    private int store(PrintWriter out, PrintWriter err, String key, String mode, boolean yes,
                        @Nullable Map<String, Object> status) throws IOException {
        var locale = locale();
        try (var lease = context.openTerminal()) {
            var terminal = lease.terminal();
            char[] secret;
            try {
                secret = SecretReader.read(terminal, out, locale);
            } catch (UserInterruptException _) {
                // The interrupt at the secret prompt is the exit-code table's 130 as well.
                throw new StartPrompt.InterruptException();
            } catch (IOException e) {
                out.println(MessageCatalogue.format(locale, READ_FAILED,
                                    Map.of(REASON, TerminalSanitizer.sanitize(e.getMessage()))));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            try {
                if (secret.length == 0) {
                    out.println(MessageCatalogue.format(locale, READ_FAILED,
                                        Map.of(REASON, "empty")));
                    out.flush();
                    return PmOperator.EXIT_FAILURE;
                }
                if (!yes && !confirm(terminal)) {
                    return 0;
                }
                var body = new LinkedHashMap<String, Object>();
                body.put(MODE, mode);
                body.put(SECRET, new String(secret));
                if (entryKind != null && !entryKind.isBlank()) {
                    body.put(KIND, entryKind);
                }
                if (projectScope != null && !projectScope.isBlank()) {
                    body.put(PROJECT, projectScope);
                }
                var response = context.runtime(true).send(
                                    HttpRequest.json("PUT", path(key), JsonTree.write(body)));
                return reportStored(out, err, key, response, storeHint(status), terminal);
            } finally {
                SecretReader.dispose(secret);
            }
        }
    }

    private int reportStored(PrintWriter out, PrintWriter err, String key, HttpResponse response, String store,
                        Terminal terminal) throws IOException {
        var locale = locale();
        if (response.status() != 200 && response.status() != 201 && response.status() != 204) {
            out.println(MessageCatalogue.format(locale, FAILED,
                                Map.of(KEY, key, STATUS, response.status())));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }
        var dropped = 0;
        try {
            var json = JsonTree.asObject(JsonTree.parse(response.body()));
            var count = json != null ? JsonTree.integer(json, "derived_tokens_dropped") : null;
            dropped = count != null ? count : 0;
        } catch (IOException _) {
            // a body-less 204 is fine
        }
        out.println(MessageCatalogue.format(locale, STORED,
                            Map.of(KEY, key, COUNT, dropped)));
        out.flush();
        return verifyProviders(out, err, key, store, terminal);
    }

    /**
     * Runs the provider check the design follows a stored token with
     * ({@code 08-transcripts-later.adoc:262-273}) through the machine onboarding's later run: the
     * providers that use the key report their lines, and the last line names the plans that go on.
     * A check that fails prints the failure and leaves the hold as it is.
     *
     * @param out   the verb's output
     * @param key   the credential key whose providers are checked
     * @param store the store the secret was written to
     * @return the exit code
     * @throws IOException on transport failure
     */
    private int verifyProviders(PrintWriter out, PrintWriter err, String key, String store, Terminal terminal)
                        throws IOException {
        var locale = locale();
        out.println("\u2713 " + MessageCatalogue.format(locale, "common.consent.remembered",
                            Map.of("action", MessageCatalogue.format(locale,
                                                "common.consent.kind.verify_providers", Map.of()))));
        out.flush();
        var facts = new LinkedHashMap<String, String>();
        facts.put("current", store);
        var outcome = ConfigAspectRun.runFor(context, out, err, CREDENTIALS,
                            new ConfigAspectRun.Answers(facts, List.of("verify_providers"), false, false, false,
                                                false),
                            terminal);
        if (outcome.exitCode() != 0) {
            return outcome.exitCode();
        }
        var reported = outcome.facts();
        if (Boolean.TRUE.equals(reported.get("failed"))) {
            out.println("\u2717 " + MessageCatalogue.format(locale, "operator.credentials.verify_failed",
                                Map.of("reason", TerminalSanitizer.sanitize(
                                                    Objects.toString(reported.get("cause"), "")))));
            out.flush();
            return 0;
        }
        var plans = reported.get("plans") instanceof String text ? text : "";
        var line = new StringBuilder("\u2713 ").append(MessageCatalogue.format(locale,
                            "onboard.settings.credential.verified", Map.of(KEY, key)));
        if (!plans.isBlank()) {
            line.append(' ').append(MessageCatalogue.format(locale,
                                "onboard.settings.credential.plans_go_on",
                                Map.of(COUNT, plans.split(",").length, "plans", plans)));
        }
        out.println(line);
        out.flush();
        return 0;
    }

    private boolean confirm(Terminal terminal) {
        var reader = LineReaderBuilder.builder().terminal(terminal).build();
        try {
            var line = reader.readLine(MessageCatalogue.format(locale(),
                                "operator.credentials.store_confirm", Map.of()));
            return line == null || !"b".equalsIgnoreCase(line.trim());
        } catch (UserInterruptException _) {
            // The interrupt is the exit-code table's 130, not a declined confirmation.
            throw new StartPrompt.InterruptException();
        } catch (EndOfFileException _) {
            return false;
        }
    }

    @Command(name = "rotate", description = "Replaces the secret of an existing credential entry.")
    @OperatorVerb
    static final class RotateCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Credential key")
        String key = "";

        @Option(names = "--project", description = "Project scope")
        @Nullable
        String project;

        @Option(names = {"-y", "--yes"}, description = "Store without the confirmation")
        boolean yes;

        RotateCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var command = new CredentialsCommand(context);
            var status = command.readStatus(out, key);
            if (!isPresent(status)) {
                out.println(MessageCatalogue.format(command.locale(), "operator.credentials.not_present",
                                    Map.of(KEY, key)));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            command.printHeader(out, key, ROTATE, status);
            return command.store(out, spec.commandLine().getErr(), key, ROTATE, yes, status);
        }
    }

    @Command(name = "set", description = "Writes an entry of another kind under the same key.")
    @OperatorVerb
    static final class SetCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Credential key")
        String key = "";

        @Option(names = "--kind", description = "Kind of the token, e.g. fine-grained")
        @Nullable
        String kind;

        @Option(names = "--project", description = "Project scope")
        @Nullable
        String project;

        @Option(names = {"-y", "--yes"}, description = "Store without the confirmation")
        boolean yes;

        SetCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var command = new CredentialsCommand(context);
            command.entryKind = kind;
            command.projectScope = project;
            var status = command.readStatus(out, key);
            command.printHeader(out, key, "set", status);
            if (kind != null && !kind.isBlank()) {
                out.println(MessageCatalogue.format(command.locale(), "operator.credentials.kind", Map.of())
                                    + "  " + TerminalSanitizer.sanitize(kind));
            }
            out.flush();
            return command.store(out, spec.commandLine().getErr(), key, "set", yes, status);
        }
    }

    @Command(name = "status", description = "Reports an entry without revealing the secret.")
    @OperatorVerb
    static final class StatusCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Credential key")
        String key = "";

        StatusCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            var out = spec.commandLine().getOut();
            var command = new CredentialsCommand(context);
            var status = command.readStatus(out, key);
            if (!isPresent(status)) {
                out.println(MessageCatalogue.format(command.locale(), "operator.credentials.not_present",
                                    Map.of(KEY, key)));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            out.println(MessageCatalogue.format(command.locale(), "operator.credentials.entry",
                                Map.of()) + "  " + providerHint(status, command.locale()));
            out.flush();
            return 0;
        }
    }

    @Command(name = "remove", description = "Retires a credential entry.")
    @OperatorVerb
    static final class RemoveCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Credential key")
        String key = "";

        RemoveCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var response = context.runtime(true).send(new HttpRequest("DELETE", path(key), Map.of(), null));
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                    STORED, Map.of(KEY, key, COUNT, 0)));
                out.flush();
                return 0;
            }
            out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                FAILED, Map.of(KEY, key, STATUS, response.status())));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }
    }
}
