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
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.config.AspectOwnedKeys;
import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.config.ConfigValueResource;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.ConsentRecord;
import de.planmarshall.api.onboarding.DocumentsMode;
import de.planmarshall.api.onboarding.MachineCredentialStatus;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.MachineSettingsUpdateRequest;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;
import de.planmarshall.api.onboarding.OnboardingAspect;

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
    private static final String LANGUAGE = "language";
    private static final String HARNESSES = "harnesses";
    private static final String MODELS = "models";
    private static final String ROLES = "roles";
    private static final String PROVIDERS = "providers";
    private static final String PLAN_DEFAULTS = "plan-defaults";
    private static final String HARNESS_PREFIX = "harness.";
    private static final String PLAN_DEFAULTS_PREFIX = "plan_defaults.";
    private static final String DOCUMENTS_MODE_KEY = "sources.documents_mode";
    private static final String DOCUMENTS_TEXT_PREFIX = "onboard.settings.documents.";
    private static final String ONE_LEVEL = "operator.config.one_level";
    private static final String CONFIG_VERB = "config";
    private static final String DOCUMENTS = "documents";
    private static final String CREDENTIALS = "credentials";
    private static final String CONSENTS = "consents";
    private static final String CLEANUP = "cleanup";
    private static final String SHOW = "show";
    private static final String ROTATE = "rotate";
    private static final String STATUS = "status";
    private static final String VALUE = "value";
    private static final String INDEX = "index";
    private static final String TITLE = "title";
    private static final String RANGE = "range";
    private static final String ASPECT = "aspect";
    private static final String SETTINGS_FAILED = "operator.settings.list_failed";

    /**
     * @param aspect an aspect of the machine onboarding
     * @return the {@code config <aspect>} verb that runs it again
     */
    static String verbOf(OnboardingAspect aspect) {
        return switch (aspect) {
            case LANGUAGE -> LANGUAGE;
            case HARNESSES -> HARNESSES;
            case MODELS -> MODELS;
            case ROLES -> ROLES;
            case CREDENTIALS -> CREDENTIALS;
            case PROVIDERS -> PROVIDERS;
            case PLANS -> PLAN_DEFAULTS;
        };
    }

    /** The key prefix each `config <aspect>` name owns, for `config show <aspect>`. */
    private static final Map<String, String> ASPECT_PREFIXES = Map.of(
                        LANGUAGE, LANGUAGE + ".",
                        HARNESSES, HARNESS_PREFIX,
                        MODELS, MODELS + ".",
                        ROLES, ROLES + ".",
                        CREDENTIALS, CREDENTIALS + ".",
                        PROVIDERS, PROVIDERS + ".",
                        PLAN_DEFAULTS, PLAN_DEFAULTS_PREFIX);

    /** The catalogue prefixes a configuration value's text is looked up in, in this order. */
    private static final List<String> VALUE_TEXT_KEYS = List.of(
                        "onboard.value.",
                        "unit.",
                        DOCUMENTS_TEXT_PREFIX);

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    ConfigCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();
        var settings = readSettingsQuiet();
        List<Item> items;
        try {
            items = settings != null ? itemsOf(locale, settings) : itemsOfValues(locale);
        } catch (ReadFailedException failure) {
            out.println(failure.status() > 0
                                ? MessageCatalogue.format(locale, SETTINGS_FAILED, Map.of(STATUS, failure.status()))
                                : MessageCatalogue.format(locale, "operator.config.show.unreadable", Map.of()));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }
        printSettings(out, locale, settings, items);
        if (context.isInteractiveTerminal() && !items.isEmpty()) {
            out.flush();
            openItem(out, items);
        }
        out.flush();
        return 0;
    }

    /** A settings read the runtime refused: the status it answered with, or a body it could not read. */
    private static final class ReadFailedException extends IOException {

        private static final long serialVersionUID = 1L;
        private final int status;

        ReadFailedException(int status) {
            super("HTTP " + status);
            this.status = status;
        }

        int status() {
            return status;
        }
    }

    /** The marker {@link ReadFailedException} carries for a 200 body that is not a value list. */
    private static final int UNREADABLE = -1;

    /**
     * One row of the settings overview: the group it belongs to, the label, the value, and the
     * command the number runs.
     *
     * @param group   the catalogue key of the group's title, or {@code null} for a row without one
     * @param label   the row's label as the catalogue names it
     * @param value   the row's value as the screen shows it
     * @param command the verb with its arguments, without the program name
     */
    private record Item(@Nullable String group, String label, String value, List<String> command) {
    }

    /** The widest label column: a longer label is shortened, so the value column stays narrow. */
    private static final int LABEL_COLUMN = 24;

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

    /**
     * @param locale the interface locale
     * @param settings the machine's settings
     * @return the overview's rows, in the order the prompt offers them
     */
    private static List<Item> itemsOf(String locale, MachineSettingsRecord settings) {
        var items = new ArrayList<Item>();
        for (int index = 0; index < settings.aspects().size(); index++) {
            var aspect = settings.aspects().get(index);
            var verb = verbOf(aspect.aspect());
            items.add(new Item("operator.settings.aspects.title",
                                MessageCatalogue.format(locale, aspect.aspect().stepNameKey(), Map.of()),
                                TerminalSanitizer.sanitizeLine(aspect.summary()),
                                List.of(CONFIG_VERB, verb)));
        }
        for (var credential : settings.credentials()) {
            items.add(new Item("operator.settings.tokens.title", credential.key(),
                                providerLine(locale, credential),
                                List.of(CREDENTIALS, ROTATE, credential.key())));
        }
        for (var consent : settings.consents()) {
            items.add(new Item("operator.settings.consents.title",
                                MessageCatalogue.format(locale, consent.titleKey(), Map.of()),
                                consent.kind().wireValue(), List.of(CONFIG_VERB, CONSENTS)));
        }
        items.add(new Item("operator.settings.documents.title", MessageCatalogue.format(locale,
                                                DOCUMENTS_TEXT_PREFIX + settings.documentsMode().wireValue(), Map.of()),
                            settings.documentsMode().wireValue(), List.of(CONFIG_VERB, DOCUMENTS)));
        items.add(new Item(null,
                            MessageCatalogue.format(locale, "operator.settings.cleanup.title", Map.of()),
                            MessageCatalogue.format(locale, "operator.settings.cleanup.value", Map.of(
                                                "days", settings.cleanupDurations().plansAfterDays(),
                                                "remind", settings.cleanupDurations().remindAfterDays())),
                            List.of(CLEANUP)));
        return List.copyOf(items);
    }

    /**
     * The overview of a runtime that reports no machine settings: the effective values it serves
     * through {@code GET /api/v1/config} ({@code 02-levels.adoc#origins}).
     *
     * @param locale the interface locale
     * @return the overview's rows
     * @throws ReadFailedException when the runtime refuses the read
     * @throws IOException         on transport failure
     */
    private List<Item> itemsOfValues(String locale) throws IOException {
        var values = readValues();
        var items = new ArrayList<Item>();
        for (var value : values) {
            items.add(new Item(null, value.key(), renderedValue(locale, value),
                                List.of(CONFIG_VERB, SHOW, value.key())));
        }
        return List.copyOf(items);
    }

    private static String providerLine(String locale, MachineCredentialStatus credential) {
        return credential.provider() + " \u00b7 " + credential.kind() + " \u00b7 " + expiryText(locale, credential);
    }

    /**
     * Renders the overview with the values in one column and the prompt on a line of its own
     * ({@code CLI-UC-F-05}): the index is right-aligned and the label padded to the widest one, so a
     * value starts in the same column on every row.
     */
    private static void printSettings(PrintWriter out, String locale, @Nullable MachineSettingsRecord settings,
                        List<Item> items) {
        out.println(MessageCatalogue.format(locale, "onboard.settings.title", Map.of()));
        if (settings != null) {
            out.println("  " + MessageCatalogue.format(locale, "operator.settings.revision_short",
                                Map.of("revision", settings.revision())));
        }
        printItems(out, locale, items);
        if (!items.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "operator.settings.open_prompt",
                                Map.of(RANGE, "1-" + items.size()))
                                + "   q) " + MessageCatalogue.format(locale, "operator.devices.leave", Map.of())
                                + "   ?   \u203a ");
        } else {
            out.println(MessageCatalogue.format(locale, "operator.settings.none", Map.of()));
        }
    }

    private static void printItems(PrintWriter out, String locale, List<Item> items) {
        int indexWidth = String.valueOf(Math.max(items.size(), 1)).length();
        int labelWidth = 0;
        for (var item : items) {
            var length = item.label().length();
            labelWidth = Math.max(labelWidth, length > LABEL_COLUMN ? LABEL_COLUMN : length);
        }
        int number = 1;
        @Nullable String group = null;
        for (var item : items) {
            if (item.group() != null && !item.group().equals(group)) {
                group = item.group();
                out.println(MessageCatalogue.format(locale, group, Map.of()));
            }
            out.println(MessageCatalogue.format(locale, "operator.settings.row", Map.of(
                                INDEX, rightAligned(String.valueOf(number++), indexWidth),
                                "name", pad(shortened(item.label()), labelWidth),
                                VALUE, item.value())));
        }
    }

    /**
     * @param label a row's label
     * @return the label, shortened to the label column so a long one never pushes the value column
     *         past the screen's width
     */
    private static String shortened(String label) {
        if (label.length() <= LABEL_COLUMN) {
            return label;
        }
        return label.substring(0, LABEL_COLUMN - 1) + "\u2026";
    }

    private static String rightAligned(String text, int width) {
        var padded = new StringBuilder();
        for (int index = text.length(); index < width; index++) {
            padded.append(' ');
        }
        return padded + text;
    }

    private static String pad(String text, int width) {
        var padded = new StringBuilder(text);
        for (int index = text.length(); index < width; index++) {
            padded.append(' ');
        }
        return padded.toString();
    }

    /**
     * @return the effective values the runtime serves through {@code GET /api/v1/config}
     * @throws IOException when the runtime refuses the read or serves none
     */
    private List<ConfigValueResource> readValues() throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(CONFIG_PATH));
        if (response.status() != 200) {
            throw new ReadFailedException(response.status());
        }
        try {
            return parseValues(response.bodyText());
        } catch (IOException _) {
            throw new ReadFailedException(UNREADABLE);
        }
    }

    /**
     * @param body the {@code GET /api/v1/config} body
     * @return the values it carries
     * @throws IOException when the body is neither a list of values nor one value
     */
    private static List<ConfigValueResource> parseValues(String body) throws IOException {
        var parsed = JsonTree.parse(body);
        var elements = JsonTree.asArray(parsed);
        if (elements != null) {
            var values = new ArrayList<ConfigValueResource>();
            for (var element : elements) {
                var map = JsonTree.asObject(element);
                if (map == null) {
                    throw new IOException("the configuration carried no value object");
                }
                values.add(ConfigValueResource.fromMap(map));
            }
            return List.copyOf(values);
        }
        var single = JsonTree.asObject(parsed);
        if (single == null) {
            throw new IOException("the configuration carried no value");
        }
        return List.of(ConfigValueResource.fromMap(single));
    }

    /**
     * @param values the effective values
     * @param key    a key, a key prefix or an aspect name, or {@code null} for all
     * @return the values the argument selects
     */
    private static List<ConfigValueResource> select(List<ConfigValueResource> values, @Nullable String key) {
        if (key == null || key.isBlank()) {
            return values;
        }
        var aspectPrefix = ASPECT_PREFIXES.get(key);
        var selected = new ArrayList<ConfigValueResource>();
        for (var value : values) {
            if (value.key().equals(key) || value.key().startsWith(key + ".")
                                || (aspectPrefix != null && value.key().startsWith(aspectPrefix))) {
                selected.add(value);
            }
        }
        return List.copyOf(selected);
    }

    /**
     * Renders the effective values: the key in one column, the value in the next, and the origin in a
     * column of its own so it survives a copy into an issue ({@code 02-levels.adoc#origins}).
     */
    private static void printValues(PrintWriter out, String locale, List<ConfigValueResource> values) {
        out.println(MessageCatalogue.format(locale, "operator.config.show.title", Map.of()));
        int keyWidth = 0;
        int valueWidth = 0;
        for (var value : values) {
            keyWidth = Math.max(keyWidth, value.key().length());
            valueWidth = Math.max(valueWidth, renderedValue(locale, value).length());
        }
        for (var value : values) {
            var line = MessageCatalogue.format(locale, "operator.config.show.row", Map.of(
                                "key", pad(value.key(), keyWidth),
                                VALUE, pad(renderedValue(locale, value), valueWidth),
                                "origin", levelText(locale, value.origin())));
            if (value.overrides() != null) {
                line += "   " + MessageCatalogue.format(locale, "operator.config.show.overrides",
                                    Map.of("overridden", levelText(locale, value.overrides())));
            }
            out.println(line);
        }
    }

    /**
     * @param project the project level's id, or {@code null}
     * @param plan    the plan level's id, or {@code null}
     * @return the level a write addresses
     */
    private static ConfigOrigin writeLevel(@Nullable String project, @Nullable String plan) {
        if (plan != null) {
            return ConfigOrigin.PLAN;
        }
        if (project != null) {
            return ConfigOrigin.PROJECT;
        }
        return ConfigOrigin.MACHINE;
    }

    /**
     * @param locale the interface locale
     * @param origin a level, or {@code null}
     * @return the level's own word in the interface language
     */
    private static String levelText(String locale, @Nullable ConfigOrigin origin) {
        if (origin == null) {
            return "";
        }
        var key = "operator.config.origin." + origin.wire();
        if (MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return origin.wire();
    }

    private static String renderedValue(String locale, ConfigValueResource value) {
        if (value.value() == null) {
            return "";
        }
        var text = value.value() instanceof String string ? string : JsonTree.write(value.value());
        return TerminalSanitizer.sanitizeLine(renderedText(locale, text));
    }

    /**
     * @param locale the interface locale
     * @param text   a value's wire text
     * @return the catalogue's text for the value, or the value itself where the catalogue names none,
     *         so no raw wire token reaches the screen
     */
    private static String renderedText(String locale, String text) {
        for (var prefix : VALUE_TEXT_KEYS) {
            var key = prefix + text;
            if (MessageCatalogue.hasKey(locale, key)) {
                return MessageCatalogue.format(locale, key, Map.of());
            }
        }
        return text;
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

    /**
     * Reads the number the prompt offers and runs the act it names. The prompt lists every number it
     * offers, so a number outside the list opens nothing.
     */
    private void openItem(PrintWriter out, List<Item> items) throws IOException {
        try (var lease = context.openTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
            var line = reader.readLine("");
            if (line == null || "q".equalsIgnoreCase(line.trim()) || line.isBlank()) {
                return;
            }
            int choice;
            try {
                choice = Integer.parseInt(line.trim());
            } catch (NumberFormatException _) {
                return;
            }
            if (choice < 1 || choice > items.size()) {
                return;
            }
            var item = items.get(choice - 1);
            var command = item.command().toArray(new String[0]);
            PmOperator.execute(context, out, out, command);
        }
    }

    static boolean isAspectOwned(@Nullable String key) {
        return AspectOwnedKeys.isAspectOwned(key);
    }

    static String getOwningAspect(@Nullable String key) {
        if (key == null) return CONFIG_VERB;
        if (key.startsWith(LANGUAGE + ".")) return LANGUAGE;
        if (key.startsWith(HARNESS_PREFIX)) return HARNESSES;
        if (key.startsWith(MODELS + ".")) return MODELS;
        if (key.startsWith(CREDENTIALS + ".")) return CREDENTIALS;
        if (key.startsWith(PROVIDERS + ".")) return PROVIDERS;
        if (key.startsWith(PLAN_DEFAULTS_PREFIX)) return PLAN_DEFAULTS;
        if (key.startsWith(DOCUMENTS_MODE_KEY)) return DOCUMENTS;
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
            var locale = context.clientEnvironment().locale();
            var out = spec.commandLine().getOut();
            var path = OperatorUrlUtil.appendScope(CONFIG_PATH, project, plan);
            var response = context.runtime(true).send(HttpRequest.get(path));
            if (response.status() != 200) {
                out.println(MessageCatalogue.format(locale, "operator.config.setelines_failed",
                                    Map.of(STATUS, response.status())));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            List<ConfigValueResource> values;
            try {
                values = ConfigCommand.parseValues(response.bodyText());
            } catch (IOException _) {
                out.println(MessageCatalogue.format(locale, "operator.config.show.unreadable", Map.of()));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            var selected = ConfigCommand.select(values, key);
            if (selected.isEmpty()) {
                out.println(MessageCatalogue.format(locale, "operator.config.show.unknown",
                                    Map.of("key", Objects.toString(key, ""))));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            ConfigCommand.printValues(out, locale, selected);
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

        @Option(names = "--machine", description = "Write at the machine level (the default)")
        boolean machine;

        @Option(names = "--project", description = "Write at this project's level")
        @Nullable
        String project;

        @Option(names = "--plan", description = "Write at this plan's level")
        @Nullable
        String plan;

        SetCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var locale = context.clientEnvironment().locale();
            var out = spec.commandLine().getOut();
            if (isAspectOwned(key)) {
                var aspect = getOwningAspect(key);
                var err = spec.commandLine().getErr();
                err.println(MessageCatalogue.format(locale, "operator.config.set_refused",
                                    Map.of("key", key, ASPECT, aspect)));
                err.flush();
                return 1;
            }
            if (project != null && plan != null) {
                out.println(MessageCatalogue.format(locale, ONE_LEVEL, Map.of()));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            if (machine && (project != null || plan != null)) {
                out.println(MessageCatalogue.format(locale, ONE_LEVEL, Map.of()));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            var body = new LinkedHashMap<String, Object>();
            body.put(VALUE, value);
            var path = OperatorUrlUtil.appendScope(levelPath(), project, plan);
            var response = context.runtime(true).send(HttpRequest.json("PUT", path, JsonTree.write(body)));
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

        private String levelPath() {
            return CONFIG_PATH + "/" + writeLevel(project, plan).wire() + "/" + OperatorUrlUtil.encodePathSegment(key);
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

        @Option(names = "--machine", description = "Remove the machine level's value (the default)")
        boolean machine;

        @Option(names = "--project", description = "Remove this project's value")
        @Nullable
        String project;

        @Option(names = "--plan", description = "Remove this plan's value")
        @Nullable
        String plan;

        UnsetCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var locale = context.clientEnvironment().locale();
            var out = spec.commandLine().getOut();
            if (isAspectOwned(key)) {
                var err = spec.commandLine().getErr();
                err.println(MessageCatalogue.format(locale, "operator.config.set_refused",
                                    Map.of("key", key, ASPECT, getOwningAspect(key))));
                err.flush();
                return 1;
            }
            if (project != null && plan != null) {
                out.println(MessageCatalogue.format(locale, ONE_LEVEL, Map.of()));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            if (machine && (project != null || plan != null)) {
                out.println(MessageCatalogue.format(locale, ONE_LEVEL, Map.of()));
                out.flush();
                return PmOperator.EXIT_FAILURE;
            }
            var path = OperatorUrlUtil.appendScope(levelPath(), project, plan);
            var response = context.runtime(true).send(new HttpRequest("DELETE", path, Map.of(), null));
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

        private String levelPath() {
            return CONFIG_PATH + "/" + writeLevel(project, plan).wire() + "/" + OperatorUrlUtil.encodePathSegment(key);
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
            var facts = new LinkedHashMap<String, String>();
            if (interfaceLocale != null) facts.put("interface", interfaceLocale);
            if (modelLocale != null) facts.put("model", modelLocale);
            return ConfigAspectRun.run(context, spec.commandLine().getOut(), spec.commandLine().getErr(), LANGUAGE,
                                ConfigAspectRun.Answers.of(facts));
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
            var facts = new LinkedHashMap<String, String>();
            if (add != null && !add.isEmpty()) {
                facts.put("add", String.join(",", add));
            }
            if (remove != null && !remove.isEmpty()) {
                facts.put("remove", String.join(",", remove));
            }
            if (defaultHarness != null) facts.put("default", defaultHarness);
            var answers = new ConfigAspectRun.Answers(facts,
                                consent != null ? consent : List.of(), noIcons, false, false, noInput);
            return ConfigAspectRun.run(context, spec.commandLine().getOut(), spec.commandLine().getErr(), HARNESSES,
                                answers);
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
            var facts = new LinkedHashMap<String, String>();
            if (allow != null && !allow.isEmpty()) {
                facts.put("allow", String.join(",", allow));
            }
            var answers = new ConfigAspectRun.Answers(facts, List.of(), false, noAdvice, false, false);
            return ConfigAspectRun.run(context, spec.commandLine().getOut(), spec.commandLine().getErr(), MODELS,
                                answers);
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
            var facts = new LinkedHashMap<String, String>();
            if (defaults) facts.put("defaults", "true");
            if (set != null && !set.isEmpty()) facts.put("set", String.join(",", set));
            if (reset != null) facts.put("reset", reset);
            if (resetAll) facts.put("reset_all", "true");
            return ConfigAspectRun.run(context, out, spec.commandLine().getErr(), "roles",
                                ConfigAspectRun.Answers.of(facts));
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
            var facts = new LinkedHashMap<String, String>();
            if (store != null) facts.put("current", store);
            return ConfigAspectRun.run(context, spec.commandLine().getOut(), spec.commandLine().getErr(), CREDENTIALS,
                                ConfigAspectRun.Answers.of(facts));
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
                return PlanSettingsDialogue.run(context, out, plan, PROVIDERS, false);
            }
            var facts = new LinkedHashMap<String, String>();
            if (use != null && !use.isEmpty()) facts.put("use", String.join(",", use));
            if (without != null && !without.isEmpty()) facts.put("without", String.join(",", without));
            if (verify) facts.put("verify", "true");
            return ConfigAspectRun.run(context, out, spec.commandLine().getErr(), PROVIDERS,
                                ConfigAspectRun.Answers.of(facts));
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
                return PlanSettingsDialogue.run(context, out, plan, PLAN_DEFAULTS, false);
            }
            var facts = new LinkedHashMap<String, String>();
            if (landing != null) facts.put("landing", landing);
            if (method != null) facts.put("method", method);
            if (asking != null) facts.put("asking", asking);
            if (posture != null) facts.put("posture", posture);
            var answers = new ConfigAspectRun.Answers(facts, List.of(), false, false, yes, false);
            return ConfigAspectRun.run(context, out, spec.commandLine().getErr(), PLAN_DEFAULTS, answers);
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
                out.println(MessageCatalogue.format(locale, SETTINGS_FAILED,
                                    Map.of(STATUS, "unreachable")));
                out.flush();
                return PmOperator.EXIT_FAILURE;
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
            try (var lease = context.openTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
                var line = reader.readLine(MessageCatalogue.format(locale, "operator.settings.apply_again", Map.of()));
                out.flush();
                return line == null || !"k".equalsIgnoreCase(line.trim());
            }
        }

        private void interact(PrintWriter out, String locale, MachineSettingsRecord settings) throws IOException {
            try (var lease = context.openTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
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
                out.println(MessageCatalogue.format(locale, "operator.config.documents_unavailable", Map.of()));
                out.flush();
                return PmOperator.EXIT_FAILURE;
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

        private void printChoice(PrintWriter out, String locale, DocumentsMode current) {
            out.println(MessageCatalogue.format(locale, "onboard.settings.documents.title", Map.of()));
            int idx = 1;
            for (var candidate : DocumentsMode.values()) {
                var name = MessageCatalogue.format(locale,
                                    DOCUMENTS_TEXT_PREFIX + candidate.wireValue(), Map.of());
                var text = MessageCatalogue.format(locale,
                                    DOCUMENTS_TEXT_PREFIX + candidate.wireValue() + "_text", Map.of());
                out.println("  " + idx++ + ") " + name + "            " + text
                                    + (candidate == current ? " ←" : ""));
            }
            out.flush();
        }

        private @Nullable String readChoice(String locale, DocumentsMode current) throws IOException {
            if (!context.isInteractiveTerminal()) {
                return current.wireValue();
            }
            try (var lease = context.openTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
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
            try (var lease = context.openTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
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
