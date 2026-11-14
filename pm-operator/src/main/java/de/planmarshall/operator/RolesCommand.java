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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.RoleDetailResource;
import de.planmarshall.api.resource.RoleDetailResource.NarrowedEffort;
import de.planmarshall.api.resource.RoleSummaryResource;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator roles}: reads the effective role set and runs a role's qualification
 * (CLI &amp; Security § Roles; PM-WORK-1, PM-WORK-6).
 * <p>
 * The verbs are operator verbs (PM-SEC-3). {@code list} and {@code show} are read-only; they show
 * the effective harness, model, and effort with the level each comes from, the qualification
 * state, and whether the start-failure guard holds. {@code qualify} shows the configuration and
 * the corpus topics first and runs only after the operator confirms; {@code --yes} skips the
 * confirmation but never the terminal check. Every text is in the interface language.
 */
@Command(name = "roles", description = "Reads the effective role set and qualifies a role.",
                    subcommands = {
                                        RolesCommand.ListCommand.class,
                                        RolesCommand.ShowCommand.class,
                                        RolesCommand.QualifyCommand.class
                    })
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class RolesCommand implements Callable<Integer> {

    private static final String PATH_ROLES = "/api/v1/roles";
    private static final String QUERY_PROJECT = "?project=";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_REASON = "reason";
    private static final String PARAM_HARNESS = "harness";
    private static final String PARAM_MODEL = "model";
    private static final String PARAM_EFFORT = "effort";
    private static final String RECORD_PATH = "record_path";
    private static final String KEY_NONE = "common.none";
    private static final Map<String, String> MEASUREMENT_LABELS = measurementLabels();
    private static final int LABEL_WIDTH = 16;
    private static final int[] LIST_WIDTHS = {18, 8, 10, 6, 16, 22, 14, 20, 6};

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    RolesCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "list", description = "Lists every role of the effective set.")
    @OperatorVerb
    static final class ListCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--project", description = "Project whose effective role set is read")
        @Nullable
        String project;

        ListCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var locale = context.clientEnvironment().locale();
            var response = context.runtime(true).send(HttpRequest.get(rolesTarget(project)));

            if (response.status() == 200) {
                var roles = RoleSummaryResource.listFromJson(response.bodyText());
                if (roles.isEmpty()) {
                    out.println(text(locale, "operator.roles.empty"));
                } else {
                    out.println(MessageCatalogue.format(locale, "operator.roles.list.title",
                                        Map.of("count", roles.size())));
                    printHeader(out, locale);
                    roles.forEach(role -> printRow(out, locale, role));
                }
                out.flush();
                return 0;
            }
            if (response.status() >= 400 && response.status() < 500) {
                err.println(MessageCatalogue.format(locale, "operator.roles.failed",
                                    Map.of(FIELD_STATUS, response.status())));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("roles list", response);
        }

        private static void printHeader(PrintWriter out, String locale) {
            printRow(out, locale,
                                text(locale, "operator.roles.header.role"),
                                text(locale, "operator.roles.header.set"),
                                text(locale, "operator.roles.header.class"),
                                text(locale, "operator.roles.header.form"),
                                text(locale, "operator.roles.header.harness"),
                                text(locale, "operator.roles.header.model"),
                                text(locale, "operator.roles.header.effort"),
                                text(locale, "operator.roles.header.qualification"),
                                text(locale, "operator.roles.header.guard"));
        }

        private static void printRow(PrintWriter out, String locale, RoleSummaryResource role) {
            var guard = text(locale, role.startFailureGuardHeld()
                                ? "operator.roles.guard.held" : "operator.roles.guard.none");
            printRow(out, locale,
                                role.role(),
                                valueOrDash(locale, role.sourceSet()),
                                valueOrDash(locale, role.roleClass()),
                                valueOrDash(locale, role.form()),
                                valueWithLevel(locale, role.harness(), role.harnessLevel()),
                                valueWithLevel(locale, role.model(), role.modelLevel()),
                                valueWithLevel(locale, role.effort(), role.effortLevel()),
                                role.qualificationState().wire(),
                                guard);
        }

        private static void printRow(PrintWriter out, String locale, String... values) {
            var line = new StringBuilder();
            for (var index = 0; index < values.length; index++) {
                if (index > 0) {
                    line.append(' ');
                }
                line.append(pad(locale, values[index], LIST_WIDTHS[index]));
            }
            out.println(line);
        }

        private static String pad(String locale, @Nullable String value, int width) {
            var safe = OutputSafety.stripControlSequences(value == null ? text(locale, KEY_NONE) : value);
            if (safe.length() > width) {
                safe = width <= 1 ? safe.substring(0, width) : safe.substring(0, width - 1) + "…";
            }
            var padded = new StringBuilder(safe);
            while (padded.length() < width) {
                padded.append(' ');
            }
            return padded.toString();
        }

        private static String valueWithLevel(String locale, @Nullable String value, @Nullable String level) {
            var safeValue = valueOrDash(locale, value);
            return level == null || level.isBlank() ? safeValue
                                : safeValue + " (" + OutputSafety.stripControlSequences(level) + ")";
        }

        private static String rolesTarget(@Nullable String project) {
            return withProject(PATH_ROLES, project);
        }
    }

    @Command(name = "show", description = "Shows one role's effective artifact, tools, effort and qualification.")
    @OperatorVerb
    static final class ShowCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Role identifier")
        String role = "";

        @Option(names = "--project", description = "Project whose effective role set is read")
        @Nullable
        String project;

        ShowCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var locale = context.clientEnvironment().locale();
            var response = context.runtime(true).send(HttpRequest.get(roleTarget(role, project)));

            if (response.status() == 200) {
                renderDetail(out, locale, RoleDetailResource.fromJson(response.bodyText()));
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                err.println(MessageCatalogue.format(locale, "operator.roles.show.not_found",
                                    Map.of("role", OutputSafety.stripControlSequences(role))));
                err.flush();
                return 1;
            }
            if (response.status() >= 400 && response.status() < 500) {
                err.println(MessageCatalogue.format(locale, "operator.roles.show.failed",
                                    Map.of("role", OutputSafety.stripControlSequences(role),
                                                        FIELD_STATUS, response.status())));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("roles show " + role, response);
        }

        private static void renderDetail(PrintWriter out, String locale, RoleDetailResource detail) {
            out.println(MessageCatalogue.format(locale, "operator.roles.show.title",
                                Map.of("role", OutputSafety.stripControlSequences(detail.role()))));
            out.println(MessageCatalogue.format(locale, "operator.roles.show.description",
                                Map.of("description", OutputSafety.stripControlSequences(detail.description()))));
            out.println(MessageCatalogue.format(locale, "operator.roles.show.class", Map.of(
                                "class", valueOrDash(locale, stringParameter(detail, "class")),
                                "form", valueOrDash(locale, stringParameter(detail, "form")))));
            out.println(MessageCatalogue.format(locale, "operator.roles.show.effective", configuration(locale, detail)));
            out.println(MessageCatalogue.format(locale, "operator.roles.show.tools",
                                Map.of("tools", detail.advisoryToolSet().isEmpty()
                                                    ? text(locale, KEY_NONE)
                                                    : OutputSafety.stripControlSequences(
                                                    String.join(", ", detail.advisoryToolSet())))));
            out.println(MessageCatalogue.format(locale, "operator.roles.show.narrowed",
                                Map.of(PARAM_EFFORT, narrowedText(locale, detail.narrowedEffort()))));
            out.println(MessageCatalogue.format(locale, "operator.roles.show.qualification",
                                Map.of("state", detail.qualification().state().wire())));
            if (detail.qualification().reason() != null) {
                out.println("  " + MessageCatalogue.format(locale, "operator.roles.show.reason",
                                    Map.of(FIELD_REASON, OutputSafety.stripControlSequences(
                                                        detail.qualification().reason()))));
            }
            if (!detail.qualification().criteria().isEmpty()) {
                out.println("  " + MessageCatalogue.format(locale, "operator.roles.show.criteria",
                                    Map.of("criteria", OutputSafety.stripControlSequences(
                                                        String.join(", ", detail.qualification().criteria())))));
            }
            if (!detail.configurationParameters().isEmpty()) {
                out.println(text(locale, "operator.roles.show.parameters"));
                detail.configurationParameters().forEach((key, value) -> out.printf(Locale.ROOT,
                                    "  %-" + (LABEL_WIDTH + 8) + "s %s%n",
                                    OutputSafety.stripControlSequences(key), displayValue(locale, value)));
            }
            out.flush();
        }

        private static String narrowedText(String locale, @Nullable NarrowedEffort narrowed) {
            if (narrowed == null) {
                return text(locale, KEY_NONE);
            }
            var text = OutputSafety.stripControlSequences(narrowed.category());
            if (narrowed.description() == null) {
                return text;
            }
            return text + " — " + OutputSafety.stripControlSequences(narrowed.description());
        }
    }

    @Command(name = "qualify", description = "Grades a role's configuration against the model verification corpus.")
    @OperatorVerb
    static final class QualifyCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Role identifier")
        String role = "";

        @Option(names = "--project", description = "Project whose effective role set is read")
        @Nullable
        String project;

        @Option(names = {"-y", "--yes"}, description = "Run the qualification without the interactive confirmation")
        boolean yes;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        QualifyCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var locale = context.clientEnvironment().locale();
            var safeRole = OutputSafety.stripControlSequences(role);
            var detailResponse = context.runtime(true).send(HttpRequest.get(roleTarget(role, project)));

            if (detailResponse.status() == 404) {
                err.println(MessageCatalogue.format(locale, "operator.roles.qualify.not_found",
                                    Map.of("role", safeRole)));
                err.flush();
                return 1;
            }
            if (detailResponse.status() != 200) {
                err.println(MessageCatalogue.format(locale, "operator.roles.qualify.failed",
                                    Map.of("role", safeRole, FIELD_STATUS, detailResponse.status())));
                err.flush();
                return 1;
            }

            var detail = RoleDetailResource.fromJson(detailResponse.bodyText());
            renderQualificationPreview(out, locale, detail);

            if (!yes) {
                if (noInput) {
                    out.println(text(locale, "operator.roles.qualify.confirm"));
                    out.println("  " + text(locale, "operator.roles.qualify.confirm_required"));
                    out.flush();
                    return 3;
                }
                if (!confirm(out, locale)) {
                    out.println(text(locale, "operator.roles.qualify.declined"));
                    out.flush();
                    return 1;
                }
            }

            var response = context.runtime(true).send(
                                HttpRequest.json("POST", qualifyTarget(role, project), "{}")
                                                    .withHeader("Idempotency-Key", UUID.randomUUID().toString()));
            if (response.status() == 202) {
                out.println(MessageCatalogue.format(locale, "operator.roles.qualify.pending",
                                    Map.of("role", safeRole)));
                out.flush();
                return 1;
            }
            if (response.status() == 200 || response.status() == 201) {
                var measurements = measurements(response.bodyText());
                if (measurements == null) {
                    err.println(MessageCatalogue.format(locale, "operator.roles.qualify.incomplete",
                                        Map.of("role", safeRole)));
                    err.flush();
                    return 1;
                }
                out.println(MessageCatalogue.format(locale, "operator.roles.qualify.done", Map.of("role", safeRole)));
                printMeasurements(out, locale, measurements);
                out.flush();
                return 0;
            }
            if (response.status() == 400 || response.status() == 404 || response.status() == 409) {
                err.println(MessageCatalogue.format(locale, "operator.roles.qualify.refused",
                                    Map.of(FIELD_REASON, OutputSafety.stripControlSequences(
                                                        refusalReason(response.bodyText())))));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("roles qualify " + safeRole, response);
        }

        /**
         * Asks the confirmation as every consent is asked ({@code 03-dialogues.adoc#keys}):
         * {@code y}/{@code n} in the interface language ({@code j}/{@code n} in German), an empty or
         * an unoffered answer named and asked again, and {@code q} ({@code v}) or the end of the
         * input leaving with nothing sent. {@code Ctrl+C} ends the run as an interrupt (exit code
         * {@code 130}).
         *
         * @param out    the verb's writer
         * @param locale the interface locale
         * @return {@code true} when the qualification may run
         */
        private boolean confirm(PrintWriter out, String locale) throws IOException {
            var yesKey = locale.toLowerCase(Locale.ROOT).startsWith("de") ? "j" : "y";
            var prompt = text(locale, "operator.roles.qualify.confirm") + "  "
                                + text(locale, "operator.dialogue.consent.prompt_yn") + " › ";
            try (var lease = context.openTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
                while (true) {
                    String line;
                    try {
                        line = reader.readLine(prompt);
                    } catch (UserInterruptException _) {
                        // The interrupt is the exit-code table's 130, not a declined confirmation.
                        throw new StartPrompt.InterruptException();
                    } catch (EndOfFileException _) {
                        return false;
                    }
                    var answer = line.trim();
                    if (yesKey.equalsIgnoreCase(answer)) {
                        return true;
                    }
                    if ("n".equalsIgnoreCase(answer) || ChoicePrompt.isLeave(locale, answer)) {
                        return false;
                    }
                    out.println(MessageCatalogue.format(locale, answer.isEmpty()
                                                            ? "operator.dialogue.consent.type_yn" : "operator.dialogue.not_offered",
                                        Map.of("input", OutputSafety.stripControlSequences(answer),
                                                            "offered", yesKey + ", n, " + ("j".equals(yesKey) ? "v" : "q"))));
                    out.flush();
                }
            }
        }

        private static void renderQualificationPreview(PrintWriter out, String locale, RoleDetailResource detail) {
            out.println(MessageCatalogue.format(locale, "operator.roles.qualify.title",
                                Map.of("role", OutputSafety.stripControlSequences(detail.role()))));
            out.println(MessageCatalogue.format(locale, "operator.roles.qualify.configuration",
                                configuration(locale, detail)));
            printParameter(out, locale, "operator.roles.qualify.topics", detail, "corpus_topics");
            printParameter(out, locale, "operator.roles.qualify.items", detail, "item_counts");
            var tokens = detail.configurationParameters().get("expected_tokens");
            var time = detail.configurationParameters().get("expected_time");
            if (!isAbsent(tokens) || !isAbsent(time)) {
                out.println(MessageCatalogue.format(locale, "operator.roles.qualify.expected",
                                    Map.of("tokens", displayValue(locale, tokens), "time", displayValue(locale, time))));
            }
            out.flush();
        }

        private static void printParameter(PrintWriter out, String locale, String key, RoleDetailResource detail,
                            String parameter) {
            var value = detail.configurationParameters().get(parameter);
            if (!isAbsent(value)) {
                out.println(MessageCatalogue.format(locale, key, Map.of("value", displayValue(locale, value))));
            }
        }

        /**
         * @param value a configuration parameter
         * @return whether the role names nothing for it: no value, or an empty list
         */
        private static boolean isAbsent(@Nullable Object value) {
            return value == null || value instanceof List<?> list && list.isEmpty();
        }

        private static @Nullable Map<String, Object> measurements(String body) {
            if (body.isBlank()) {
                return null;
            }
            Map<String, Object> map;
            try {
                map = JsonTree.asObject(JsonTree.parse(body));
            } catch (IOException _) {
                return null;
            }
            if (map == null || !(map.get(RECORD_PATH) instanceof String path) || path.isBlank()) {
                return null;
            }
            var measured = MEASUREMENT_LABELS.keySet().stream().anyMatch(key -> isMeasurement(map.get(key)));
            return measured ? map : null;
        }

        private static boolean isMeasurement(@Nullable Object value) {
            return switch (value) {
                case JsonNumber _, Number _, Boolean _ -> true;
                case null, default -> false;
            };
        }

        private static void printMeasurements(PrintWriter out, String locale, Map<String, Object> measurements) {
            out.println(text(locale, "operator.roles.qualify.measurements"));
            MEASUREMENT_LABELS.forEach((field, key) -> printMeasurement(out, locale, measurements, field, key));
            // measurements() admits only an answer that names its record
            out.println(MessageCatalogue.format(locale, "operator.roles.qualify.record",
                                Map.of("path", OutputSafety.stripControlSequences(String.valueOf(measurements.get(RECORD_PATH))))));
        }

        private static void printMeasurement(PrintWriter out, String locale, Map<String, Object> map, String parameter,
                            String key) {
            var value = map.get(parameter);
            if (value != null) {
                out.println("  " + MessageCatalogue.format(locale, key,
                                    Map.of("value", OutputSafety.stripControlSequences(displayValue(locale, value)))));
            }
        }

        private static String qualifyTarget(String role, @Nullable String project) {
            return withProject(PATH_ROLES + "/" + OperatorUrlUtil.encodePathSegment(role) + "/qualify", project);
        }

        private static String refusalReason(String body) {
            try {
                var map = JsonTree.asObject(JsonTree.parse(body));
                var reason = JsonTree.string(map, FIELD_REASON);
                if (reason == null) {
                    reason = JsonTree.string(map, "error");
                }
                if (reason == null) {
                    reason = JsonTree.string(map, "code");
                }
                return reason != null ? reason : "";
            } catch (IOException _) {
                return "";
            }
        }
    }

    private static Map<String, String> measurementLabels() {
        var labels = new LinkedHashMap<String, String>();
        labels.put("fresh_agreement", "operator.roles.qualify.fresh");
        labels.put("warm_agreement", "operator.roles.qualify.warm");
        labels.put("warm_fresh_gap", "operator.roles.qualify.gap");
        labels.put("qualified_fresh", "operator.roles.qualify.qualified_fresh");
        labels.put("qualified_warm", "operator.roles.qualify.qualified_warm");
        return Collections.unmodifiableMap(labels);
    }

    private static String text(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }

    private static Map<String, Object> configuration(String locale, RoleDetailResource detail) {
        return Map.of(
                            PARAM_HARNESS, valueOrDash(locale, stringParameter(detail, PARAM_HARNESS)),
                            PARAM_MODEL, valueOrDash(locale, stringParameter(detail, PARAM_MODEL)),
                            PARAM_EFFORT, valueOrDash(locale, stringParameter(detail, PARAM_EFFORT)));
    }

    private static @Nullable String stringParameter(RoleDetailResource detail, String parameter) {
        return detail.configurationParameters().get(parameter) instanceof String value ? value : null;
    }

    private static String displayValue(String locale, @Nullable Object value) {
        return switch (value) {
            case null -> text(locale, KEY_NONE);
            case JsonNumber(String text) -> text;
            case List<?> list -> list.isEmpty() ? text(locale, KEY_NONE)
                                : OutputSafety.stripControlSequences(
                                String.join(", ", list.stream().map(String::valueOf).toList()));
            case Map<?, ?> map -> OutputSafety.stripControlSequences(JsonTree.write(map));
            default -> OutputSafety.stripControlSequences(String.valueOf(value));
        };
    }

    private static String valueOrDash(String locale, @Nullable String value) {
        return value == null || value.isBlank()
                            ? text(locale, KEY_NONE)
                            : OutputSafety.stripControlSequences(value);
    }

    private static String roleTarget(String role, @Nullable String project) {
        return withProject(PATH_ROLES + "/" + OperatorUrlUtil.encodePathSegment(role), project);
    }

    private static String withProject(String target, @Nullable String project) {
        return project == null || project.isBlank() ? target
                            : target + QUERY_PROJECT + OperatorUrlUtil.encodeQueryValue(project);
    }
}
