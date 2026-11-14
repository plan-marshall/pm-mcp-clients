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

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.MachineSettingsUpdateRequest;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code config cleanup}: how long ended work is kept — the machine's durations, or a project's own
 * plans, epics and logs ({@code 26-cleanup.adoc#t-durations}, {@code settings.cleanup-durations},
 * {@code settings.record}).
 */
@Command(name = "cleanup", description = "How long ended work is kept: the durations.")
@OperatorVerb
final class ConfigCleanupCommand implements Callable<Integer> {

    /** The value that means nothing is ever removed. */
    static final int NEVER = -1;

    private static final String VALUE = "value";
    private static final int RECORD_KEPT = -1;

    private static final String KIND_PLANS = "plans";
    private static final String KIND_EPICS = "epics";
    private static final String KIND_LOGS = "logs";
    private static final String KIND_REPORTS = "reports";
    private static final String KIND_UNENROLLED = "unenrolled";
    private static final String KIND_REMIND = "remind";

    /** The machine's durations: one per kind, and the postponement. */
    private static final List<String> MACHINE_KINDS = List.of(KIND_PLANS, KIND_EPICS, KIND_LOGS,
                        KIND_REPORTS, KIND_UNENROLLED, KIND_REMIND);

    /** A project's own durations: plans, epics and logs only. */
    private static final List<String> PROJECT_KINDS = List.of(KIND_PLANS, KIND_EPICS, KIND_LOGS);

    private static final String CLEANUP_KEY_PREFIX = "cleanup.";
    private static final String AFTER_DAYS_SUFFIX = "_after_days";
    private static final String KEY_CLEANUP_DURATIONS = "cleanup_durations";
    private static final String DURATIONS_STATE = "cleanup.durations";

    @Spec
    CommandSpec spec = CommandSpec.create();
    private final OperatorContext context;

    @Option(names = "--project", description = "Edit this project's own durations")
    @Nullable
    String project;

    @Option(names = "--plans", description = "Days a plan is kept after it ended, or never")
    @Nullable
    String plans;

    @Option(names = "--epics", description = "Days an epic is kept after it ended, or never")
    @Nullable
    String epics;

    @Option(names = "--logs", description = "Days the logs of ended work are kept, or never")
    @Nullable
    String logs;

    @Option(names = "--reports", description = "Days a read report is kept, or never")
    @Nullable
    String reports;

    @Option(names = "--unenrolled", description = "Days an unenrolled project's store is kept, or never")
    @Nullable
    String unenrolled;

    @Option(names = "--remind", description = "Days after which the cleanup asks again")
    @Nullable
    String remind;

    @Option(names = {"-y", "--yes"}, description = "Record the durations without the confirmation")
    boolean yes;

    @Option(names = "--no-input", description = "Never ask; stop at the first choice not given")
    boolean noInput;

    ConfigCleanupCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        var locale = context.clientEnvironment().locale();
        if (project != null && (reports != null || unenrolled != null || remind != null)) {
            err.println(MessageCatalogue.format(locale, "operator.cleanup.machine_only",
                                Map.of("option", machineOnlyOption())));
            err.flush();
            return 2;
        }
        var settings = new ConfigCommand(context).readSettingsQuiet();
        if (settings == null) {
            out.println(MessageCatalogue.format(locale, "operator.settings.list_failed",
                                Map.of("status", "unreachable")));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }
        var durations = project == null
                            ? machineDurations(settings.cleanupDurations())
                            : projectDurations(settings.cleanupDurations());
        var changes = argumentChanges(locale);
        if (changes == null) {
            out.flush();
            return 3;
        }
        if (!changes.isEmpty()) {
            durations.putAll(changes);
            return write(out, locale, settings, durations, changes);
        }
        if (!context.isInteractiveTerminal() || noInput) {
            printRows(out, locale, durations);
            out.flush();
            return 0;
        }
        return edit(out, locale, settings, durations, Map.of());
    }

    private String machineOnlyOption() {
        if (reports != null) {
            return "--reports";
        }
        return unenrolled != null ? "--unenrolled" : "--remind";
    }

    private static Map<String, Integer> machineDurations(CleanupDurations machine) {
        var durations = new LinkedHashMap<String, Integer>();
        durations.put(KIND_PLANS, machine.plansAfterDays());
        durations.put(KIND_EPICS, machine.epicsAfterDays());
        durations.put(KIND_LOGS, machine.logsAfterDays());
        durations.put(KIND_REPORTS, machine.reportsAfterDays());
        durations.put(KIND_UNENROLLED, machine.unenrolledAfterDays());
        durations.put(KIND_REMIND, machine.remindAfterDays());
        return durations;
    }

    /**
     * @param machine the machine's durations
     * @return the durations the project view offers: the machine's, a project's own value winning
     * @throws IOException on transport failure
     */
    private Map<String, Integer> projectDurations(CleanupDurations machine) throws IOException {
        var durations = machineDurations(machine);
        var response = context.runtime(true).send(HttpRequest.get(
                            OperatorUrlUtil.appendScope(ConfigCommand.CONFIG_PATH, project, null)));
        if (response.status() != 200) {
            return durations;
        }
        try {
            for (var value : ConfigCommand.parseValues(response.bodyText())) {
                var kind = kindOfKey(value.key());
                var days = numberValue(value.value());
                if (kind != null && days != null) {
                    durations.put(kind, days);
                }
            }
        } catch (IOException _) {
            // an unreadable value list leaves the machine's durations in place
        }
        return durations;
    }

    /**
     * @param value a configuration value as the tree holds it
     * @return its whole number, or {@code null} when it is none
     */
    private static @Nullable Integer numberValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNumber(String text)) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException _) {
                return null;
            }
        }
        return value instanceof Number number ? number.intValue() : null;
    }

    private static @Nullable String kindOfKey(String key) {
        if (!key.startsWith(CLEANUP_KEY_PREFIX) || !key.endsWith(AFTER_DAYS_SUFFIX)) {
            return null;
        }
        var kind = key.substring(CLEANUP_KEY_PREFIX.length(), key.length() - AFTER_DAYS_SUFFIX.length());
        return MACHINE_KINDS.contains(kind) ? kind : null;
    }

    /**
     * @param locale the interface locale
     * @return the durations the arguments name, or {@code null} when one of them is refused
     */
    private @Nullable Map<String, Integer> argumentChanges(String locale) {
        var err = spec.commandLine().getErr();
        var changes = new LinkedHashMap<String, Integer>();
        for (var entry : givenValues().entrySet()) {
            var days = parseDays(entry.getValue());
            if (days == null || !isAllowed(entry.getKey(), days)) {
                err.println(MessageCatalogue.format(locale, "operator.cleanup.durations.minimum",
                                    Map.of("min", minimumOf(entry.getKey()))));
                err.flush();
                return null;
            }
            changes.put(entry.getKey(), days);
        }
        return changes;
    }

    private Map<String, String> givenValues() {
        var values = new LinkedHashMap<String, String>();
        putIfGiven(values, KIND_PLANS, plans);
        putIfGiven(values, KIND_EPICS, epics);
        putIfGiven(values, KIND_LOGS, logs);
        putIfGiven(values, KIND_REPORTS, reports);
        putIfGiven(values, KIND_UNENROLLED, unenrolled);
        putIfGiven(values, KIND_REMIND, remind);
        return values;
    }

    private static void putIfGiven(Map<String, String> values, String kind, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            values.put(kind, value);
        }
    }

    private void printRows(PrintWriter out, String locale, Map<String, Integer> durations) {
        out.println(project == null
                            ? MessageCatalogue.format(locale, "operator.cleanup.durations.title", Map.of())
                            : MessageCatalogue.format(locale, "operator.cleanup.durations.title_project",
                            Map.of("project", OutputSafety.stripControlSequences(project))));
        var kinds = project == null ? MACHINE_KINDS : PROJECT_KINDS;
        int index = 1;
        for (var kind : kinds) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.durations.row", Map.of(
                                "index", index++,
                                "label", MessageCatalogue.format(locale,
                                                    "operator.cleanup.durations.kind." + kind, Map.of()),
                                VALUE, rendered(locale, daysOf(durations, kind)))));
        }
        out.println(MessageCatalogue.format(locale, "operator.cleanup.durations.explain", Map.of()));
    }

    private int edit(PrintWriter out, String locale, MachineSettingsRecord settings,
                        Map<String, Integer> durations, Map<String, Integer> pending) throws IOException {
        var prompt = new StartPrompt(context, out, locale, false);
        var kinds = project == null ? MACHINE_KINDS : PROJECT_KINDS;
        var changes = new LinkedHashMap<>(pending);
        while (true) {
            printRows(out, locale, durations);
            out.flush();
            var promptKey = project == null ? "operator.cleanup.durations.keep" : "operator.cleanup.durations.keep_project";
            var answer = prompt.askRaw(MessageCatalogue.format(locale, promptKey,
                                Map.of("range", "1-" + kinds.size())), DURATIONS_STATE, "durations", null);
            if (ChoicePrompt.isLeave(locale, answer)) {
                // q leaves the dialogue with nothing sent, whatever was changed on screen.
                return 0;
            }
            if (answer.isBlank() || "k".equalsIgnoreCase(answer)) {
                return changes.isEmpty() ? 0 : write(out, locale, settings, durations, changes);
            }
            if (project != null && "d".equalsIgnoreCase(answer.trim())) {
                return deleteProjectDurations(out, locale);
            }
            changeOne(prompt, out, locale, kinds, answer, durations, changes);
        }
    }

    private int deleteProjectDurations(PrintWriter out, String locale) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(
                            OperatorUrlUtil.appendScope(ConfigCommand.CONFIG_PATH, project, null)));
        if (response.status() != 200) {
            return 0;
        }
        var values = ConfigCommand.parseValues(response.bodyText());
        for (var value : values) {
            if (value.origin() == ConfigOrigin.PROJECT && kindOfKey(value.key()) != null) {
                var write = ConfigCommand.CONFIG_PATH + "/" + ConfigOrigin.PROJECT.wire() + "/"
                                    + OperatorUrlUtil.encodePathSegment(value.key());
                var change = new ConfigCommand.Change(out, spec.commandLine().getErr(), locale,
                                    OperatorUrlUtil.appendScope(ConfigCommand.CONFIG_PATH, project, null),
                                    OperatorUrlUtil.appendScope(write, project, null), value.key(), "",
                                    "operator.config.unset_failed");
                var outcome = ConfigCommand.writeWithRevision(context, change, true, null, noInput);
                if (outcome == ConfigCommand.WriteOutcome.FAILED) {
                    return PmOperator.EXIT_FAILURE;
                }
                if (outcome == ConfigCommand.WriteOutcome.NEEDS_INPUT) {
                    return ConfigAspectRun.EXIT_MISSING_INPUT;
                }
            }
        }
        return 0;
    }

    /**
     * Applies one answer of the editor: {@code ?} explains, a number or a kind name asks its days,
     * and an answer naming neither says what is offered.
     *
     * @param prompt    the dialogue's prompt
     * @param out       the verb's writer
     * @param locale    the interface locale
     * @param kinds     the kinds the editor offers
     * @param answer    the operator's line
     * @param durations the durations being edited
     * @param changes   the changes taken so far
     */
    private void changeOne(StartPrompt prompt, PrintWriter out, String locale, List<String> kinds,
                        String answer, Map<String, Integer> durations, Map<String, Integer> changes) {
        if ("?".equals(answer)) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.durations.explain", Map.of()));
            return;
        }
        var chosen = kindOf(kinds, answer);
        if (chosen == null) {
            out.println(MessageCatalogue.format(locale, "operator.dialogue.not_offered",
                                Map.of("input", answer, "offered", "1-" + kinds.size())));
            return;
        }
        var days = askDays(prompt, locale, chosen);
        if (days != null) {
            durations.put(chosen, days);
            changes.put(chosen, days);
        }
    }

    private @Nullable Integer askDays(StartPrompt prompt, String locale, String kind) {
        var out = spec.commandLine().getOut();
        var askKey = KIND_REMIND.equals(kind) ? "operator.cleanup.ask_days.remind" : "operator.cleanup.ask_days";
        var minKey = KIND_REMIND.equals(kind) ? "operator.cleanup.durations.minimum.remind" : "operator.cleanup.durations.minimum";
        while (true) {
            var answer = prompt.askRaw(MessageCatalogue.format(locale, askKey,
                                Map.of("min", minimumOf(kind))), DURATIONS_STATE, kind, null);
            if (answer.isBlank()) {
                return null;
            }
            var days = parseDays(answer);
            if (days != null && isAllowed(kind, days)) {
                return days;
            }
            out.println(MessageCatalogue.format(locale, minKey,
                                Map.of("min", minimumOf(kind))));
        }
    }

    private int write(PrintWriter out, String locale, MachineSettingsRecord settings,
                        Map<String, Integer> durations, Map<String, Integer> changes) throws IOException {
        if (!yes) {
            if (!context.isInteractiveTerminal() || noInput) {
                printRows(out, locale, durations);
                out.println(MessageCatalogue.format(locale, "operator.config.needs_choice", Map.of("option", "--yes")));
                out.flush();
                return 3;
            }
            var prompt = new StartPrompt(context, out, locale, false);
            if (!prompt.askRaw(MessageCatalogue.format(locale, "operator.cleanup.durations.confirm", Map.of()),
                                DURATIONS_STATE, "confirmation", null).isBlank()) {
                return edit(out, locale, settings, durations, changes);
            }
        }
        if (project == null) {
            var recorded = recordMachine(out, locale, settings, durations);
            if (recorded == RECORD_KEPT) {
                return 0;
            }
            if (recorded != 0) {
                return recorded;
            }
            out.println(MessageCatalogue.format(locale, "operator.cleanup.recorded",
                                Map.of("what", whatOf(locale, changes))));
            out.flush();
            return 0;
        }
        return recordProject(out, locale, changes);
    }

    private int recordMachine(PrintWriter out, String locale, MachineSettingsRecord settings,
                        Map<String, Integer> durations) throws IOException {
        var recordDurations = new CleanupDurations(daysOf(durations, KIND_PLANS), daysOf(durations, KIND_EPICS),
                            daysOf(durations, KIND_LOGS), daysOf(durations, KIND_REPORTS),
                            daysOf(durations, KIND_UNENROLLED), daysOf(durations, KIND_REMIND));
        var targetRevision = settings.revision();
        while (true) {
            var request = new MachineSettingsUpdateRequest(KEY_CLEANUP_DURATIONS, recordDurations.toJson(),
                                targetRevision);
            var response = context.runtime(true).send(
                                HttpRequest.json("PUT", ConfigCommand.MACHINE_SETTINGS_PATH, request.toJson()));
            if (response.status() == 200 || response.status() == 204) {
                return 0;
            }
            if (response.status() == 409) {
                var conflict = MachineSettingsUpdateResult.fromJson(response.bodyText());
                var rendered = renderedCleanupConflict(locale, conflict.currentValue());
                if (noInput) {
                    out.println(MessageCatalogue.format(locale, "common.conflict.no_input", Map.of(VALUE, rendered)));
                    out.flush();
                    return ConfigAspectRun.EXIT_MISSING_INPUT;
                }
                out.println(MessageCatalogue.format(locale, "common.conflict.changed", Map.of(VALUE, rendered)));
                out.flush();
                if (!context.isInteractiveTerminal()) {
                    out.println(MessageCatalogue.format(locale, ConfigCommand.NO_SETTING, Map.of()));
                    out.flush();
                    return ConfigAspectRun.EXIT_MISSING_INPUT;
                }
                if (!ConfigCommand.applyAgain(context, out, locale)) {
                    out.println(MessageCatalogue.format(locale, ConfigCommand.NO_SETTING, Map.of()));
                    out.flush();
                    return RECORD_KEPT;
                }
                targetRevision = conflict.revision();
                continue;
            }
            out.println(MessageCatalogue.format(locale, "operator.settings.list_failed",
                                Map.of("status", response.status())));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }
    }

    private static String renderedCleanupConflict(String locale, @Nullable String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return "";
        }
        try {
            var durations = CleanupDurations.fromJson(currentValue);
            return MessageCatalogue.format(locale, "operator.settings.cleanup.value",
                                Map.of("days", durations.plansAfterDays(), KIND_REMIND, durations.remindAfterDays()));
        } catch (IOException _) {
            return currentValue;
        }
    }

    private int recordProject(PrintWriter out, String locale, Map<String, Integer> changes) throws IOException {
        var applied = new LinkedHashMap<String, Integer>();
        var kept = new LinkedHashMap<String, Integer>();
        for (var entry : changes.entrySet()) {
            var key = CLEANUP_KEY_PREFIX + entry.getKey() + AFTER_DAYS_SUFFIX;
            var write = ConfigCommand.CONFIG_PATH + "/" + ConfigOrigin.PROJECT.wire() + "/"
                                + OperatorUrlUtil.encodePathSegment(key);
            var change = new ConfigCommand.Change(out, spec.commandLine().getErr(), locale,
                                OperatorUrlUtil.appendScope(ConfigCommand.CONFIG_PATH, project, null),
                                OperatorUrlUtil.appendScope(write, project, null), key, "",
                                "operator.config.set_failed");
            var outcome = ConfigCommand.writeWithRevision(context, change, false, entry.getValue(), noInput);
            if (outcome == ConfigCommand.WriteOutcome.FAILED) {
                return PmOperator.EXIT_FAILURE;
            }
            if (outcome == ConfigCommand.WriteOutcome.NEEDS_INPUT) {
                return ConfigAspectRun.EXIT_MISSING_INPUT;
            }
            if (outcome == ConfigCommand.WriteOutcome.APPLIED) {
                applied.put(entry.getKey(), entry.getValue());
            } else if (outcome == ConfigCommand.WriteOutcome.KEPT) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        if (!applied.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.recorded",
                                Map.of("what", whatOf(locale, applied))));
        }
        if (!kept.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.durations.kept",
                                Map.of("what", whatOf(locale, kept))));
        }
        out.flush();
        return 0;
    }

    private static @Nullable String kindOf(List<String> kinds, String answer) {
        var trimmed = answer.trim();
        try {
            var index = Integer.parseInt(trimmed);
            return index >= 1 && index <= kinds.size() ? kinds.get(index - 1) : null;
        } catch (NumberFormatException _) {
            return kinds.contains(trimmed) ? trimmed : null;
        }
    }

    private String whatOf(String locale, Map<String, Integer> changes) {
        var parts = new ArrayList<String>();
        for (var entry : changes.entrySet()) {
            parts.add(MessageCatalogue.format(locale, "operator.cleanup.durations.kind." + entry.getKey(), Map.of())
                                + " " + rendered(locale, entry.getValue()));
        }
        return String.join(", ", parts);
    }

    private static String rendered(String locale, int days) {
        return days == NEVER
                            ? MessageCatalogue.format(locale, "operator.cleanup.durations.never", Map.of())
                            : MessageCatalogue.format(locale, "operator.cleanup.durations.after", Map.of("days", days));
    }

    private static int daysOf(Map<String, Integer> durations, String kind) {
        return Objects.requireNonNullElse(durations.get(kind), CleanupDurations.DEFAULT_DAYS);
    }

    private static int minimumOf(String kind) {
        return KIND_REMIND.equals(kind) ? CleanupDurations.MINIMUM_REMIND_DAYS
                            : CleanupDurations.MINIMUM_RETENTION_DAYS;
    }

    private static boolean isAllowed(String kind, int days) {
        // The postponement always names days: `never` is a retention value alone (26-cleanup.adoc#commands).
        return days >= minimumOf(kind) || (days == NEVER && !KIND_REMIND.equals(kind));
    }

    /**
     * @param text a day count, or {@code never}
     * @return the days, {@link #NEVER} for never, or {@code null} when the text is no such value
     */
    static @Nullable Integer parseDays(String text) {
        var trimmed = text.trim();
        if ("never".equalsIgnoreCase(trimmed) || "nie".equalsIgnoreCase(trimmed)) {
            return NEVER;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException _) {
            return null;
        }
    }
}
