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
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.ConsentRecord;
import de.planmarshall.api.onboarding.DocumentsMode;
import de.planmarshall.api.onboarding.MachineCredentialStatus;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.MachineSettingsUpdateRequest;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;

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
@SuppressWarnings("java:S1075")
public final class ConfigCommand implements Callable<Integer> {

    static final String CONFIG_PATH = "/api/v1/config";
    static final String MACHINE_SETTINGS_PATH = "/api/v1/machine/settings";
    private static final String ROLES = "roles";
    private static final String CONFIG_VERB = "config";
    private static final String DOCUMENTS = "documents";
    private static final String CREDENTIALS = "credentials";
    private static final String STATUS = "status";
    private static final String VALUE = "value";
    private static final String INDEX = "index";
    private static final String TITLE = "title";
    private static final String RANGE = "range";
    private static final String ASPECT = "aspect";
    private static final String SETTINGS_FAILED = "operator.settings.list_failed";
    private static final String ONBOARD_SETTINGS = "onboard.settings.";
    private static final List<String> ASPECT_COMMANDS = List.of(
                        "language", "harnesses", "models", ROLES, CREDENTIALS, "providers", "plan-defaults");

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
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();
        var settings = readSettingsQuiet();
        if (settings == null) {
            var code = printLegacyConfig(out, locale);
            out.flush();
            return code;
        }
        printSettings(out, locale, settings);
        if (context.isInteractiveTerminal()) {
            openAspect(out, locale, settings);
        }
        out.flush();
        return 0;
    }

    private @Nullable MachineSettingsRecord readSettingsQuiet() throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(MACHINE_SETTINGS_PATH));
        if (response.status() != 200) {
            return null;
        }
        try {
            return MachineSettingsRecord.fromJson(response.bodyText());
        } catch (IOException _) {
            return null;
        }
    }

    private int printLegacyConfig(PrintWriter out, String locale) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(CONFIG_PATH));
        if (response.status() != 200) {
            out.println(MessageCatalogue.format(locale, "operator.config.setelines_failed",
                                Map.of(STATUS, response.status())));
            return PmOperator.EXIT_FAILURE;
        }
        out.println(MessageCatalogue.format(locale, "operator.config.machine_settings", Map.of()));
        var json = JsonTree.parse(response.body());
        if (json instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                out.println(MessageCatalogue.format(locale, "operator.config.machine_settings_row",
                                    Map.of("key", entry.getKey(), VALUE, entry.getValue())));
            }
        } else {
            out.println(response.bodyText());
        }
        return 0;
    }

    private @Nullable MachineSettingsRecord readSettings(PrintWriter out, String locale) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(MACHINE_SETTINGS_PATH));
        if (response.status() != 200) {
            out.println(MessageCatalogue.format(locale, SETTINGS_FAILED,
                                Map.of(STATUS, response.status())));
            out.flush();
            return null;
        }
        try {
            return MachineSettingsRecord.fromJson(response.bodyText());
        } catch (IOException _) {
            out.println(MessageCatalogue.format(locale, SETTINGS_FAILED,
                                Map.of(STATUS, response.status())));
            out.flush();
            return null;
        }
    }

    private void printSettings(PrintWriter out, String locale, MachineSettingsRecord settings) {
        out.println(MessageCatalogue.format(locale, "onboard.settings.title", Map.of()));
        out.println("  " + MessageCatalogue.format(locale, "operator.settings.revision_short",
                            Map.of("revision", settings.revision())));
        int index = 1;
        out.println(MessageCatalogue.format(locale, "operator.settings.aspects.title", Map.of()));
        for (var aspect : settings.aspects()) {
            out.println(MessageCatalogue.format(locale, "operator.settings.aspects.row", Map.of(
                                INDEX, index++,
                                "name", MessageCatalogue.format(locale, aspect.aspect().stepNameKey(), Map.of()),
                                "summary", TerminalSanitizer.sanitizeLine(aspect.summary()))));
        }
        if (!settings.credentials().isEmpty()) {
            out.println(MessageCatalogue.format(locale, "operator.settings.tokens.title", Map.of()));
            for (var credential : settings.credentials()) {
                out.println(MessageCatalogue.format(locale, "operator.settings.tokens.row", Map.of(
                                    INDEX, index++, "key", credential.key(), "provider", credential.provider(),
                                    "kind", credential.kind(), "expiry", expiryText(locale, credential))));
            }
        }
        if (!settings.consents().isEmpty()) {
            out.println(MessageCatalogue.format(locale, "operator.settings.consents.title", Map.of()));
            for (var consent : settings.consents()) {
                out.println(MessageCatalogue.format(locale, "operator.settings.consents.row", Map.of(
                                    INDEX, index++,
                                    TITLE, MessageCatalogue.format(locale, consent.titleKey(), Map.of()),
                                    "kind", consent.kind().wireValue())));
            }
        }
        out.println(MessageCatalogue.format(locale, "operator.settings.documents.title", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.settings.documents.row", Map.of(
                            INDEX, index++, "name", MessageCatalogue.format(locale,
                                                ONBOARD_SETTINGS + "documents." + settings.documentsMode().wireValue(),
                                                Map.of()),
                            VALUE, settings.documentsMode().wireValue())));
        out.println(MessageCatalogue.format(locale, "operator.settings.open_prompt",
                            Map.of(RANGE, "1-" + (index - 1))));
    }

    private static String expiryText(String locale, MachineCredentialStatus credential) {
        String text;
        if (credential.expired()) {
            text = MessageCatalogue.format(locale, "operator.settings.tokens.expired",
                                Map.of("count", credential.heldPlan() != null ? 1 : 0));
        } else if (credential.expiresInDays() != null) {
            text = MessageCatalogue.format(locale, "operator.settings.tokens.expires",
                                Map.of("days", credential.expiresInDays()));
            if (credential.expiresSoon()) {
                text = MessageCatalogue.format(locale, "operator.settings.tokens.expires_soon", Map.of("text", text));
            }
        } else {
            text = MessageCatalogue.format(locale, "operator.settings.tokens.no_expiry", Map.of());
        }
        return text;
    }

    private void openAspect(PrintWriter out, String locale, MachineSettingsRecord settings) throws IOException {
        int aspectCount = settings.aspects().size();
        int credentialCount = settings.credentials().size();
        boolean consentsListed = !settings.consents().isEmpty();
        int consentsIndex = aspectCount + credentialCount + 1;
        int documentsIndex = consentsIndex + (consentsListed ? 1 : 0);
        try (var terminal = context.createTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var line = reader.readLine(MessageCatalogue.format(locale, "operator.settings.open_prompt",
                                Map.of(RANGE, "1-" + documentsIndex))
                                + "   q) " + MessageCatalogue.format(locale, "operator.devices.leave", Map.of())
                                + "   ?   \u203a ");
            if (line == null || "q".equalsIgnoreCase(line.trim()) || line.isBlank()) {
                return;
            }
            int choice;
            try {
                choice = Integer.parseInt(line.trim());
            } catch (NumberFormatException _) {
                return;
            }
            if (choice >= 1 && choice <= aspectCount) {
                PmOperator.execute(context, out, out, CONFIG_VERB, ASPECT_COMMANDS.get(choice - 1));
            } else if (choice > aspectCount && choice <= aspectCount + credentialCount) {
                var credential = settings.credentials().get(choice - aspectCount - 1);
                PmOperator.execute(context, out, out, CREDENTIALS, "rotate", credential.key());
            } else if (consentsListed && choice == consentsIndex) {
                PmOperator.execute(context, out, out, CONFIG_VERB, "consents");
            } else if (choice == documentsIndex) {
                PmOperator.execute(context, out, out, CONFIG_VERB, DOCUMENTS);
            }
            // Any other number opens nothing: the prompt lists what exists.
        }
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
        if (key == null) return CONFIG_VERB;
        if (key.startsWith("language.")) return "language";
        if (key.startsWith("credentials.")) return CREDENTIALS;
        if (key.startsWith("providers.")) return "providers";
        if (key.startsWith("plan_defaults.")) return "plan-defaults";
        if (key.startsWith("sources.documents_mode")) return DOCUMENTS;
        if (key.startsWith(ROLES)) return ROLES;
        return CONFIG_VERB;
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
            var locale = context.clientEnvironment().locale();
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
                out.println(MessageCatalogue.format(locale, "operator.config.setelines_failed", Map.of(STATUS, response.status())));
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
            var locale = context.clientEnvironment().locale();
            if (isAspectOwned(key)) {
                var aspect = getOwningAspect(key);
                var err = spec.commandLine().getErr();
                err.println(MessageCatalogue.format(locale, "operator.config.set_refused",
                                    Map.of("key", key, ASPECT, aspect)));
                err.flush();
                return 1;
            }
            var body = new LinkedHashMap<String, Object>();
            body.put("key", key);
            body.put(VALUE, value);
            if (project != null) body.put("project", project);
            if (plan != null) body.put("plan", plan);

            var response = context.runtime(true).send(HttpRequest.json("PUT", CONFIG_PATH, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.config.set_done", Map.of("key", key, VALUE, value)));
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.println(MessageCatalogue.format(locale, "operator.config.set_failed", Map.of("key", key, STATUS, response.status())));
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
            var locale = context.clientEnvironment().locale();
            var path = CONFIG_PATH + "/" + OperatorUrlUtil.encodePathSegment(key);
            path = OperatorUrlUtil.appendScope(path, project, null);
            var response = context.runtime(true).send(new HttpRequest("DELETE", path, Map.of(), null));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.config.unset_done", Map.of("key", key)));
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.println(MessageCatalogue.format(locale, "operator.config.unset_failed", Map.of("key", key, STATUS, response.status())));
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
            var locale = context.clientEnvironment().locale();
            var body = new LinkedHashMap<String, Object>();
            if (interfaceLocale != null) body.put("interface", interfaceLocale);
            if (modelLocale != null) body.put("model", modelLocale);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/language", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.language_done", Map.of()));
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.println(MessageCatalogue.format(locale, "operator.config.language_failed", Map.of(STATUS, response.status())));
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
            var locale = context.clientEnvironment().locale();
            if (noInput && (consent == null || !consent.contains("scan_harnesses"))) {
                var out = spec.commandLine().getOut();
                out.println(MessageCatalogue.format(locale, "operator.config.harnesses_title", Map.of()));
                out.println(MessageCatalogue.format(locale, "operator.config.needs_choice",
                                    Map.of("option", "--consent scan_harnesses")));
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
                out.println(MessageCatalogue.format(locale, "operator.config.harnesses_done", Map.of()));
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.println(MessageCatalogue.format(locale, "operator.config.update_failed", Map.of(ASPECT, "harnesses", STATUS, response.status())));
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
            var locale = context.clientEnvironment().locale();
            var body = new LinkedHashMap<String, Object>();
            if (allow != null) body.put("allow", allow);
            body.put("no_advice", noAdvice);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/models", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.models_done", Map.of()));
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

        @Option(names = "--plan", description = "The plan whose own settings are shown and changed")
        @Nullable
        String plan;

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
            var out = spec.commandLine().getOut();
            if (plan != null) {
                if (plan.isBlank()) {
                    return blankPlan(context, out);
                }
                if (defaults || set != null || reset != null || resetAll) {
                    return exclusive(context, out);
                }
                return PlanSettingsDialogue.run(context, out, plan, "roles", false);
            }
            var locale = context.clientEnvironment().locale();
            var body = new LinkedHashMap<String, Object>();
            body.put("defaults", defaults);
            if (set != null) body.put("set", set);
            if (reset != null) body.put("reset", reset);
            body.put("reset_all", resetAll);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/roles", JsonTree.write(body)));
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.roles_done", Map.of()));
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
            var locale = context.clientEnvironment().locale();
            var body = new LinkedHashMap<String, Object>();
            if (store != null) body.put("store", store);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/credentials", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.credential_store_done", Map.of("store", store != null ? store : "default")));
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

        @Option(names = "--plan", description = "The plan whose own settings are shown and changed")
        @Nullable
        String plan;

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
            var out = spec.commandLine().getOut();
            if (plan != null) {
                if (plan.isBlank()) {
                    return blankPlan(context, out);
                }
                if (use != null || without != null || verify) {
                    return exclusive(context, out);
                }
                return PlanSettingsDialogue.run(context, out, plan, "providers", false);
            }
            var locale = context.clientEnvironment().locale();
            var body = new LinkedHashMap<String, Object>();
            if (use != null) body.put("use", use);
            if (without != null) body.put("without", without);
            body.put("verify", verify);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/providers", JsonTree.write(body)));
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.providers_done", Map.of()));
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

        @Option(names = "--plan", description = "The plan whose own settings are shown and changed")
        @Nullable
        String plan;

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
            var out = spec.commandLine().getOut();
            if (plan != null) {
                if (plan.isBlank()) {
                    return blankPlan(context, out);
                }
                if (landing != null || method != null || asking != null || posture != null || yes) {
                    return exclusive(context, out);
                }
                return PlanSettingsDialogue.run(context, out, plan, "plan-defaults", false);
            }
            var locale = context.clientEnvironment().locale();
            var body = new LinkedHashMap<String, Object>();
            if (landing != null) body.put("landing", landing);
            if (method != null) body.put("method", method);
            if (asking != null) body.put("asking", asking);
            if (posture != null) body.put("posture", posture);
            body.put("confirmed", yes);

            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/config/plan-defaults", JsonTree.write(body)));
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.plan_defaults_title", Map.of()));
                if (landing != null) {
                    out.println(MessageCatalogue.format(locale, "operator.config.plan_defaults.landing_row",
                                        Map.of("name", MessageCatalogue.format(locale, "operator.config.plan_defaults.landing", Map.of()),
                                                            VALUE, landing)));
                }
                if (method != null) {
                    out.println(MessageCatalogue.format(locale, "operator.config.plan_defaults.method_row",
                                        Map.of("name", MessageCatalogue.format(locale, "operator.config.plan_defaults.method", Map.of()), VALUE, method)));
                }
                if (asking != null) {
                    out.println(MessageCatalogue.format(locale, "operator.config.plan_defaults.asking_row",
                                        Map.of("name", MessageCatalogue.format(locale, "operator.config.plan_defaults.asking", Map.of()), VALUE, asking)));
                }
                out.println(MessageCatalogue.format(locale, "operator.config.plan_defaults_recorded", Map.of()));
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "consents", description = "Lists the remembered consents to withdraw one.")
    @OperatorVerb
    static final class ConsentsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--withdraw", description = "Withdraw the remembered consent of this kind")
        @Nullable
        String withdraw;

        @Option(names = {"-y", "--yes"}, description = "Withdraw without the confirmation")
        boolean yes;

        ConsentsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var locale = context.clientEnvironment().locale();
            var owner = new ConfigCommand(context);
            var settings = owner.readSettingsQuiet();
            if (settings == null) {
                return legacyConsents(out, locale);
            }
            var consents = settings.consents();
            if (consents.isEmpty()) {
                out.println(MessageCatalogue.format(locale, "operator.consents.none", Map.of()));
                out.flush();
                return 0;
            }
            printConsents(out, locale, consents);
            out.println();
            out.println(MessageCatalogue.format(locale, "operator.consents.asked_again", Map.of()));
            if (withdraw != null && !withdraw.isBlank()) {
                return withdrawByKind(out, locale, settings, withdraw);
            }
            if (context.isInteractiveTerminal()) {
                interact(out, locale, settings);
            }
            out.flush();
            return 0;
        }

        private int legacyConsents(PrintWriter out, String locale) throws IOException {
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/config/consents"));
            if (response.status() != 200) {
                return PmOperator.EXIT_FAILURE;
            }
            var json = JsonTree.parse(response.body());
            if (json instanceof List<?> list && !list.isEmpty()) {
                out.println(MessageCatalogue.format(locale, "operator.config.consents_title", Map.of()));
                int idx = 1;
                for (var c : list) {
                    out.println(MessageCatalogue.format(locale, "operator.config.consents_row",
                                        Map.of(INDEX, idx++, TITLE, TerminalSanitizer.sanitizeLine(String.valueOf(c)))));
                }
            } else {
                out.println(MessageCatalogue.format(locale, "operator.config.consents_none", Map.of()));
            }
            out.flush();
            return 0;
        }

        private void printConsents(PrintWriter out, String locale, List<ConsentRecord> consents) {
            out.println(MessageCatalogue.format(locale, "operator.consents.title", Map.of()));
            int idx = 1;
            for (var consent : consents) {
                out.println(MessageCatalogue.format(locale, "operator.consents.row", Map.of(
                                    INDEX, idx++, TITLE, MessageCatalogue.format(locale, consent.titleKey(), Map.of()),
                                    "kind", consent.kind().wireValue())));
                out.println(MessageCatalogue.format(locale, "operator.consents.row_detail", Map.of(
                                    "when", consent.grantedAt() != null ? consent.grantedAt() : "",
                                    "where", TerminalSanitizer.sanitizeLine(consent.channel() != null
                                                        ? consent.channel() : "terminal"))));
            }
        }

        private int withdrawByKind(PrintWriter out, String locale, MachineSettingsRecord settings, String kind)
                            throws IOException {
            for (var consent : settings.consents()) {
                if (consent.kind().wireValue().equals(kind)) {
                    return withdraw(out, locale, settings, consent);
                }
            }
            out.println(MessageCatalogue.format(locale, "operator.consents.none", Map.of()));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }

        private int withdraw(PrintWriter out, String locale, MachineSettingsRecord settings, ConsentRecord consent)
                            throws IOException {
            var title = MessageCatalogue.format(locale, consent.titleKey(), Map.of());
            out.println(MessageCatalogue.format(locale, "operator.consents.withdraw_confirm", Map.of(TITLE, title)));
            out.println(MessageCatalogue.format(locale, "operator.consents.withdraw_explain",
                                Map.of("step", MessageCatalogue.format(locale,
                                                    consent.kind().titleKey().startsWith("common") ? "onboard.harness"
                                                                        : consent.kind().titleKey(), Map.of()))));
            if (!yes && context.isInteractiveTerminal() && !confirm(out, locale)) {
                return 0;
            }
            var request = MachineSettingsUpdateRequest.withdrawConsent(consent.kind(), settings.revision());
            var response = context.runtime(true).send(
                                HttpRequest.json("PUT", MACHINE_SETTINGS_PATH, request.toJson()));
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.consents.withdrawn", Map.of("count", 1)));
                out.flush();
                return 0;
            }
            if (response.status() == 409) {
                out.println(MessageCatalogue.format(locale, "operator.settings.kept", Map.of(VALUE, "")));
                out.flush();
                return 0;
            }
            out.println(MessageCatalogue.format(locale, SETTINGS_FAILED,
                                Map.of(STATUS, response.status())));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }

        private boolean confirm(PrintWriter out, String locale) throws IOException {
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                var line = reader.readLine(MessageCatalogue.format(locale, "operator.settings.apply_again", Map.of()));
                out.flush();
                return line == null || !"k".equalsIgnoreCase(line.trim());
            }
        }

        private void interact(PrintWriter out, String locale, MachineSettingsRecord settings) throws IOException {
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                var line = reader.readLine(MessageCatalogue.format(locale, "operator.consents.withdraw_prompt",
                                    Map.of(RANGE, "1-" + settings.consents().size()))
                                    + "   q) " + MessageCatalogue.format(locale, "operator.consents.quit", Map.of())
                                    + "   ?   \u203a ");
                if (line == null || line.isBlank() || "q".equalsIgnoreCase(line.trim())) {
                    return;
                }
                int choice;
                try {
                    choice = Integer.parseInt(line.trim());
                } catch (NumberFormatException _) {
                    return;
                }
                if (choice >= 1 && choice <= settings.consents().size()) {
                    withdraw(out, locale, settings, settings.consents().get(choice - 1));
                }
            }
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

        @Option(names = {"-y", "--yes"}, description = "Apply again on a conflict with the current value")
        boolean yes;

        DocumentsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var locale = context.clientEnvironment().locale();
            var owner = new ConfigCommand(context);
            var settings = owner.readSettingsQuiet();
            if (settings == null) {
                return legacyDocuments(out, locale);
            }
            var chosen = mode;
            printChoice(out, locale, settings.documentsMode());
            if (chosen == null || chosen.isBlank()) {
                chosen = readChoice(locale, settings.documentsMode());
                if (chosen == null) {
                    out.println(MessageCatalogue.format(locale, "operator.config.documents_unchanged", Map.of()));
                    out.flush();
                    return 0;
                }
            }
            return apply(out, locale, settings, chosen);
        }

        private int legacyDocuments(PrintWriter out, String locale) throws IOException {
            var body = new LinkedHashMap<String, Object>();
            if (mode != null) body.put("mode", mode);
            var response = context.runtime(true).send(
                                HttpRequest.json("POST", "/api/v1/config/documents", JsonTree.write(body)));
            if (response.status() == 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.documents_done",
                                    Map.of("mode", mode != null ? mode : "ask")));
                out.flush();
                return 0;
            }
            return PmOperator.EXIT_FAILURE;
        }

        private void printChoice(PrintWriter out, String locale, DocumentsMode current) {
            out.println(MessageCatalogue.format(locale, "onboard.settings.documents.title", Map.of()));
            int idx = 1;
            for (var candidate : DocumentsMode.values()) {
                var name = MessageCatalogue.format(locale,
                                    "onboard.settings.documents." + candidate.wireValue(), Map.of());
                var text = MessageCatalogue.format(locale,
                                    "onboard.settings.documents." + candidate.wireValue() + "_text", Map.of());
                out.println("  " + idx++ + ") " + name + "            " + text
                                    + (candidate == current ? " ←" : ""));
            }
            out.flush();
        }

        private @Nullable String readChoice(String locale, DocumentsMode current) throws IOException {
            if (!context.isInteractiveTerminal()) {
                return current.wireValue();
            }
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                var line = reader.readLine(MessageCatalogue.format(locale, "operator.config.documents.prompt", Map.of()));
                if (line == null || line.isBlank()) {
                    return null;
                }
                var values = DocumentsMode.values();
                int choice;
                try {
                    choice = Integer.parseInt(line.trim());
                } catch (NumberFormatException _) {
                    return null;
                }
                return choice >= 1 && choice <= values.length ? values[choice - 1].wireValue() : null;
            }
        }

        private int apply(PrintWriter out, String locale, MachineSettingsRecord settings, String value)
                            throws IOException {
            var request = new MachineSettingsUpdateRequest("documents_mode", value, settings.revision());
            var response = context.runtime(true).send(HttpRequest.json("PUT", MACHINE_SETTINGS_PATH, request.toJson()));
            if (response.status() == 409) {
                var conflict = MachineSettingsUpdateResult.fromJson(response.bodyText());
                out.println(MessageCatalogue.format(locale, "web.settings.conflict.refused",
                                    Map.of(VALUE, Objects.toString(conflict.currentValue(), ""))));
                out.flush();
                if (yes || (context.isInteractiveTerminal() && confirmRetry(locale))) {
                    var retry = new MachineSettingsUpdateRequest("documents_mode", value, conflict.revision());
                    response = context.runtime(true).send(HttpRequest.json("PUT", MACHINE_SETTINGS_PATH, retry.toJson()));
                } else {
                    out.println(MessageCatalogue.format(locale, "operator.settings.no_setting", Map.of()));
                    out.flush();
                    return 0;
                }
            }
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.config.documents_done", Map.of("mode", value)));
                out.flush();
                return 0;
            }
            out.println(MessageCatalogue.format(locale, "operator.config.update_failed",
                                Map.of(ASPECT, DOCUMENTS, STATUS, response.status())));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }

        private boolean confirmRetry(String locale) throws IOException {
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                var line = reader.readLine(MessageCatalogue.format(locale, "operator.settings.apply_again", Map.of()));
                return line == null || !"k".equalsIgnoreCase(line.trim());
            }
        }
    }

    /**
     * @param out the verb's output
     * @return the exit code of a verb that combined {@code --plan} with other options
     */
    private static int blankPlan(OperatorContext context, PrintWriter out) {
        out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                            "operator.actions.plan_blank", Map.of()));
        out.flush();
        return 1;
    }

    private static int exclusive(OperatorContext context, PrintWriter out) {
        out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                            "operator.actions.plan_option_exclusive", Map.of()));
        out.flush();
        return 1;
    }
}
