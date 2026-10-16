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
import java.util.Objects;
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
 * {@code pm-operator config}: machine and project settings management (01-commands.adoc#commands,
 * 08-transcripts-later.adoc#t-settings).
 */
@Command(name = "config", description = "Manages machine and project configuration.",
                    subcommands = {
                                        ConfigCommand.ShowCommand.class,
                                        ConfigCommand.SetCommand.class,
                                        ConfigCommand.UnsetCommand.class,
                                        ConfigCommand.LanguageCommand.class,
                                        ConfigCommand.HarnessesCommand.class,
                                        ConfigCommand.ModelsCommand.class,
                                        ConfigCommand.RolesCommand.class,
                                        ConfigCommand.CredentialsCommand.class,
                                        ConfigCommand.ProvidersCommand.class,
                                        ConfigCommand.PlanDefaultsCommand.class,
                                        ConfigCommand.ConsentsCommand.class,
                                        ConfigCommand.DocumentsCommand.class
                    })
@OperatorVerb
public final class ConfigCommand implements Callable<Integer> {

    static final String CONFIG_PATH = "/api/v1/config";
    private static final String ROLES = "roles";

    static final List<String> ASPECT_OWNED_KEYS = List.of(
                        "language.", "credentials.store", "providers.", "plan_defaults.",
                        "sources.documents_mode", "roles.", ROLES
    );

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    ConfigCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var runtime = context.runtime(true);
        var response = runtime.send(HttpRequest.get(CONFIG_PATH));
        var out = spec.commandLine().getOut();
        if (response.status() == 200) {
            out.println("Machine Settings:");
            var json = JsonTree.parse(response.body());
            if (json instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    out.printf("  %-25s %s%n", entry.getKey(), entry.getValue());
                }
            } else {
                out.println("  " + response.bodyText());
            }
        } else {
            out.println("Unable to load settings: HTTP " + response.status());
        }
        out.flush();
        return 0;
    }

    static boolean isAspectOwned(@Nullable String key) {
        if (key == null) {
            return false;
        }
        for (var prefix : ASPECT_OWNED_KEYS) {
            if (key.startsWith(prefix) || key.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    static String getOwningAspect(@Nullable String key) {
        if (key == null) return "config";
        if (key.startsWith("language.")) return "language";
        if (key.startsWith("credentials.")) return "credentials";
        if (key.startsWith("providers.")) return "providers";
        if (key.startsWith("plan_defaults.")) return "plan-defaults";
        if (key.startsWith("sources.documents_mode")) return "documents";
        if (key.startsWith(ROLES)) return ROLES;
        return "config";
    }

    @Command(name = "show", description = "Shows effective configuration values.")
    @OperatorVerb
    static final class ShowCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", arity = "0..1", description = "Key or aspect name")
        @Nullable
        String key;

        @Option(names = "--project", description = "Project scope")
        @Nullable
        String project;

        @Option(names = "--plan", description = "Plan scope")
        @Nullable
        String plan;

        ShowCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var path = CONFIG_PATH;
            if (key != null && !key.isBlank()) {
                path += "/" + OperatorUrlUtil.encodePathSegment(key);
            }
            path = OperatorUrlUtil.appendScope(path, project, plan);
            var response = context.runtime(true).send(HttpRequest.get(path));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println(response.bodyText());
            } else {
                out.println("Configuration not found: HTTP " + response.status());
            }
            out.flush();
            return 0;
        }
    }

    @Command(name = "set", description = "Sets one configuration key at one level.")
    @OperatorVerb
    static final class SetCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Configuration key")
        String key = "";

        @Parameters(index = "1", description = "Value to set")
        String value = "";

        @Option(names = "--project", description = "Project scope")
        @Nullable
        String project;

        @Option(names = "--plan", description = "Plan scope")
        @Nullable
        String plan;

        SetCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            if (isAspectOwned(key)) {
                var aspect = getOwningAspect(key);
                var err = spec.commandLine().getErr();
                err.println("config set refused: 409 aspect_owned (" + key + " is owned by '" + aspect
                                    + "'; change it via 'pm-operator config " + aspect + "')");
                err.flush();
                return 1;
            }
            var body = new LinkedHashMap<String, Object>();
            body.put("key", key);
            body.put("value", value);
            if (project != null) body.put("project", project);
            if (plan != null) body.put("plan", plan);

            var response = context.runtime(true).send(HttpRequest.json("PUT", CONFIG_PATH, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ Config %s set to %s%n", key, value);
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to set %s: HTTP %d%n", key, response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "unset", description = "Unsets a configuration key.")
    @OperatorVerb
    static final class UnsetCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Configuration key")
        String key = "";

        @Option(names = "--project", description = "Project scope")
        @Nullable
        String project;

        UnsetCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var path = CONFIG_PATH + "/" + OperatorUrlUtil.encodePathSegment(key);
            path = OperatorUrlUtil.appendScope(path, project, null);
            var response = context.runtime(true).send(new HttpRequest("DELETE", path, Map.of(), null));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ Config %s unset%n", key);
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to unset %s: HTTP %d%n", key, response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "language", description = "Configures interface and model language.")
    @OperatorVerb
    static final class LanguageCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--interface", description = "Interface locale")
        @Nullable
        String interfaceLocale;

        @Option(names = "--model", description = "Model text locale or 'same'")
        @Nullable
        String modelLocale;

        LanguageCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (interfaceLocale != null) body.put("interface", interfaceLocale);
            if (modelLocale != null) body.put("model", modelLocale);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/language", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Language settings updated");
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.println("Failed to update language settings: HTTP " + response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "harnesses", description = "Configures coding agent harnesses.")
    @OperatorVerb
    static final class HarnessesCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--add", description = "Harness client ID to add")
        @Nullable
        List<String> add;

        @Option(names = "--remove", description = "Harness client ID to remove")
        @Nullable
        List<String> remove;

        @Option(names = "--default", description = "Default harness client ID")
        @Nullable
        String defaultHarness;

        @Option(names = "--no-icons", description = "Disable harness icon fetching")
        boolean noIcons;

        @Option(names = "--no-input", description = "Do not prompt if choices needed")
        boolean noInput;

        @Option(names = "--consent", description = "Grant consent for operation")
        @Nullable
        List<String> consent;

        HarnessesCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            if (noInput && (consent == null || !consent.contains("scan_harnesses"))) {
                var out = spec.commandLine().getOut();
                out.println("Harnesses · Look for harnesses on this machine?");
                out.println("Needs a choice: --consent scan_harnesses");
                out.flush();
                return 3;
            }
            var body = new LinkedHashMap<String, Object>();
            if (add != null) body.put("add", add);
            if (remove != null) body.put("remove", remove);
            if (defaultHarness != null) body.put("default", defaultHarness);
            body.put("no_icons", noIcons);

            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/harnesses", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Harnesses updated");
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.println("Failed to update harnesses: HTTP " + response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "models", description = "Configures allowed models.")
    @OperatorVerb
    static final class ModelsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--allow", description = "Allow client_id:model")
        @Nullable
        List<String> allow;

        @Option(names = "--no-advice", description = "Decline model advice")
        boolean noAdvice;

        ModelsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (allow != null) body.put("allow", allow);
            body.put("no_advice", noAdvice);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/models", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Models updated");
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "roles", description = "Configures role assignments.")
    @OperatorVerb
    static final class RolesCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--defaults", description = "Reset to default role assignments")
        boolean defaults;

        @Option(names = "--set", description = "Set role=client:model[:effort]")
        @Nullable
        List<String> set;

        @Option(names = "--reset", description = "Reset specific role")
        @Nullable
        String reset;

        @Option(names = "--reset-all", description = "Reset all roles")
        boolean resetAll;

        RolesCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            body.put("defaults", defaults);
            if (set != null) body.put("set", set);
            if (reset != null) body.put("reset", reset);
            body.put("reset_all", resetAll);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/roles", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Roles updated");
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "credentials", description = "Configures credential storage.")
    @OperatorVerb
    static final class CredentialsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--store", description = "Credential store type: os or file")
        @Nullable
        String store;

        CredentialsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (store != null) body.put("store", store);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/credentials", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Credential store updated: " + (store != null ? store : "default"));
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "providers", description = "Configures external providers.")
    @OperatorVerb
    static final class ProvidersCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--use", description = "Providers to use")
        @Nullable
        List<String> use;

        @Option(names = "--without", description = "Providers to exclude")
        @Nullable
        List<String> without;

        @Option(names = "--verify", description = "Verify recorded providers")
        boolean verify;

        ProvidersCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (use != null) body.put("use", use);
            if (without != null) body.put("without", without);
            body.put("verify", verify);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/providers", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Providers updated");
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "plan-defaults", description = "Configures plan execution defaults.")
    @OperatorVerb
    static final class PlanDefaultsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--landing", description = "Default landing method: pull-request, main, files")
        @Nullable
        String landing;

        @Option(names = "--method", description = "Default method: test-first, tests-after")
        @Nullable
        String method;

        @Option(names = "--asking", description = "Asking frequency: every-phase, key-points, questions-only")
        @Nullable
        String asking;

        @Option(names = "--posture", description = "Verification posture: minimal, standard, full, unset")
        @Nullable
        String posture;

        @Option(names = "--yes", description = "Confirm changes")
        boolean yes;

        PlanDefaultsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (landing != null) body.put("landing", landing);
            if (method != null) body.put("method", method);
            if (asking != null) body.put("asking", asking);
            if (posture != null) body.put("posture", posture);
            body.put("confirmed", yes);

            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/plan-defaults", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("Plans · How should plans run on this machine?");
                if (landing != null) out.println("  ✓ Landing   " + landing + "   (--landing " + landing + ")");
                if (method != null) out.println("  ✓ Method    " + method + "   (--method " + method + ")");
                if (asking != null) out.println("  ✓ Asking    " + asking + "   (--asking " + asking + ")");
                out.println("✓ Plan defaults recorded");
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "consents", description = "Shows and manages remembered consents.")
    @OperatorVerb
    static final class ConsentsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        ConsentsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/config/consents"));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                var json = JsonTree.parse(response.body());
                if (json instanceof List<?> list && !list.isEmpty()) {
                    out.println("Remembered consents:");
                    int idx = 1;
                    for (var c : list) {
                        out.printf("  %d) %s%n", idx++, c);
                    }
                } else {
                    out.println("No consent is remembered.");
                }
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "documents", description = "Configures documents mode.")
    @OperatorVerb
    static final class DocumentsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--mode", description = "Mode: copy, move, ask")
        @Nullable
        String mode;

        DocumentsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (mode != null) body.put("mode", mode);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/documents", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Documents mode updated: " + (mode != null ? mode : "ask"));
                out.flush();
                return 0;
            }
            return 1;
        }
    }
}
