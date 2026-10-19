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
 * confirmation but never the terminal check.
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
        context.gate().verify();
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
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var response = context.runtime(true).send(HttpRequest.get(rolesTarget(project)));

            if (response.status() == 200) {
                var roles = RoleSummaryResource.listFromJson(response.bodyText());
                if (roles.isEmpty()) {
                    out.println(MessageCatalogue.get("operator.roles.empty"));
                } else {
                    out.println(MessageCatalogue.format("operator.roles.list.title",
                                        Map.of("count", roles.size())));
                    printHeader(out);
                    roles.forEach(role -> printRow(out, role));
                }
                out.flush();
                return 0;
            }
            if (response.status() >= 400 && response.status() < 500) {
                err.println(MessageCatalogue.format("operator.roles.failed",
                                    Map.of(FIELD_STATUS, response.status())));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("roles list", response);
        }

        private static void printHeader(PrintWriter out) {
            printRow(out,
                                MessageCatalogue.get("operator.roles.header.role"),
                                MessageCatalogue.get("operator.roles.header.set"),
                                MessageCatalogue.get("operator.roles.header.class"),
                                MessageCatalogue.get("operator.roles.header.form"),
                                MessageCatalogue.get("operator.roles.header.harness"),
                                MessageCatalogue.get("operator.roles.header.model"),
                                MessageCatalogue.get("operator.roles.header.effort"),
                                MessageCatalogue.get("operator.roles.header.qualification"),
                                MessageCatalogue.get("operator.roles.header.guard"));
        }

        private static void printRow(PrintWriter out, RoleSummaryResource role) {
            var guard = MessageCatalogue.get("operator.roles.guard.none");
            if (role.startFailureGuardHeld()) {
                guard = MessageCatalogue.get("operator.roles.guard.held");
            }
            printRow(out,
                                role.role(),
                                valueOrDash(role.sourceSet()),
                                valueOrDash(role.roleClass()),
                                valueOrDash(role.form()),
                                valueWithLevel(role.harness(), role.harnessLevel()),
                                valueWithLevel(role.model(), role.modelLevel()),
                                valueWithLevel(role.effort(), role.effortLevel()),
                                role.qualificationState().wire(),
                                guard);
        }

        private static void printRow(PrintWriter out, String... values) {
            var line = new StringBuilder();
            for (var index = 0; index < values.length; index++) {
                if (index > 0) {
                    line.append(' ');
                }
                line.append(pad(values[index], LIST_WIDTHS[index]));
            }
            out.println(line);
        }

        private static String pad(@Nullable String value, int width) {
            var safe = OutputSafety.stripControlSequences(value == null ? "-" : value);
            if (safe.length() > width) {
                safe = width <= 1 ? safe.substring(0, width) : safe.substring(0, width - 1) + "…";
            }
            var padded = new StringBuilder(safe);
            while (padded.length() < width) {
                padded.append(' ');
            }
            return padded.toString();
        }

        private static String valueWithLevel(@Nullable String value, @Nullable String level) {
            var safeValue = valueOrDash(value);
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
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var response = context.runtime(true).send(HttpRequest.get(roleTarget(role, project)));

            if (response.status() == 200) {
                renderDetail(out, RoleDetailResource.fromJson(response.bodyText()));
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                err.println(MessageCatalogue.format("operator.roles.show.not_found",
                                    Map.of("role", OutputSafety.stripControlSequences(role))));
                err.flush();
                return 1;
            }
            if (response.status() >= 400 && response.status() < 500) {
                err.println(MessageCatalogue.format("operator.roles.show.failed",
                                    Map.of("role", OutputSafety.stripControlSequences(role),
                                                        FIELD_STATUS, response.status())));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("roles show " + role, response);
        }

        private static void renderDetail(PrintWriter out, RoleDetailResource detail) {
            out.println(MessageCatalogue.format("operator.roles.show.title",
                                Map.of("role", OutputSafety.stripControlSequences(detail.role()))));
            out.println(MessageCatalogue.format("operator.roles.show.description",
                                Map.of("description", OutputSafety.stripControlSequences(detail.description()))));
            out.println(MessageCatalogue.format("operator.roles.show.class", Map.of(
                                "class", valueOrDash(stringParameter(detail, "class")),
                                "form", valueOrDash(stringParameter(detail, "form")))));
            out.println(MessageCatalogue.format("operator.roles.show.effective", configuration(detail)));
            out.println(MessageCatalogue.format("operator.roles.show.tools",
                                Map.of("tools", detail.advisoryToolSet().isEmpty() ? "-"
                                                    : OutputSafety.stripControlSequences(
                                                    String.join(", ", detail.advisoryToolSet())))));
            out.println(MessageCatalogue.format("operator.roles.show.narrowed",
                                Map.of(PARAM_EFFORT, narrowedText(detail.narrowedEffort()))));
            out.println(MessageCatalogue.format("operator.roles.show.qualification",
                                Map.of("state", detail.qualification().state().wire())));
            if (detail.qualification().reason() != null) {
                out.println("  " + MessageCatalogue.format("operator.roles.show.reason",
                                    Map.of(FIELD_REASON, OutputSafety.stripControlSequences(
                                                        detail.qualification().reason()))));
            }
            if (!detail.qualification().criteria().isEmpty()) {
                out.println("  " + MessageCatalogue.format("operator.roles.show.criteria",
                                    Map.of("criteria", OutputSafety.stripControlSequences(
                                                        String.join(", ", detail.qualification().criteria())))));
            }
            if (!detail.configurationParameters().isEmpty()) {
                out.println(MessageCatalogue.get("operator.roles.show.parameters"));
                detail.configurationParameters().forEach((key, value) -> out.printf(Locale.ROOT,
                                    "  %-" + (LABEL_WIDTH + 8) + "s %s%n",
                                    OutputSafety.stripControlSequences(key), displayValue(value)));
            }
            out.flush();
        }

        private static String narrowedText(@Nullable NarrowedEffort narrowed) {
            if (narrowed == null) {
                return "-";
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
            var safeRole = OutputSafety.stripControlSequences(role);
            var detailResponse = context.runtime(true).send(HttpRequest.get(roleTarget(role, project)));

            if (detailResponse.status() == 404) {
                err.println(MessageCatalogue.format("operator.roles.qualify.not_found",
                                    Map.of("role", safeRole)));
                err.flush();
                return 1;
            }
            if (detailResponse.status() != 200) {
                err.println(MessageCatalogue.format("operator.roles.qualify.failed",
                                    Map.of("role", safeRole, FIELD_STATUS, detailResponse.status())));
                err.flush();
                return 1;
            }

            var detail = RoleDetailResource.fromJson(detailResponse.bodyText());
            renderQualificationPreview(out, detail);

            if (!yes) {
                if (noInput) {
                    out.println(MessageCatalogue.get("operator.roles.qualify.confirm"));
                    out.println("  " + MessageCatalogue.get("operator.roles.qualify.confirm_required"));
                    out.flush();
                    return 3;
                }
                if (!confirm()) {
                    out.println(MessageCatalogue.get("operator.roles.qualify.declined"));
                    out.flush();
                    return 1;
                }
            }

            var response = context.runtime(true).send(
                                HttpRequest.json("POST", qualifyTarget(role, project), "{}")
                                                    .withHeader("Idempotency-Key", UUID.randomUUID().toString()));
            if (response.status() == 202) {
                out.println(MessageCatalogue.format("operator.roles.qualify.pending",
                                    Map.of("role", safeRole)));
                out.flush();
                return 1;
            }
            if (response.status() == 200 || response.status() == 201) {
                var measurements = measurements(response.bodyText());
                if (measurements == null) {
                    err.println(MessageCatalogue.format("operator.roles.qualify.incomplete",
                                        Map.of("role", safeRole)));
                    err.flush();
                    return 1;
                }
                out.println(MessageCatalogue.format("operator.roles.qualify.done", Map.of("role", safeRole)));
                printMeasurements(out, measurements);
                out.flush();
                return 0;
            }
            if (response.status() == 400 || response.status() == 404 || response.status() == 409) {
                err.println(MessageCatalogue.format("operator.roles.qualify.refused",
                                    Map.of(FIELD_REASON, OutputSafety.stripControlSequences(
                                                        refusalReason(response.bodyText())))));
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("roles qualify " + safeRole, response);
        }

        private boolean confirm() throws IOException {
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                String line;
                try {
                    line = reader.readLine(MessageCatalogue.get("operator.roles.qualify.confirm") + " \u203a ");
                } catch (EndOfFileException | UserInterruptException _) {
                    return false;
                }
                if (line == null) {
                    return false;
                }
                var answer = line.trim().toLowerCase(Locale.ROOT);
                return answer.startsWith("y") || answer.startsWith("j");
            }
        }

        private static void renderQualificationPreview(PrintWriter out, RoleDetailResource detail) {
            out.println(MessageCatalogue.format("operator.roles.qualify.title",
                                Map.of("role", OutputSafety.stripControlSequences(detail.role()))));
            out.println(MessageCatalogue.format("operator.roles.qualify.configuration", configuration(detail)));
            printParameter(out, "operator.roles.qualify.topics", detail, "corpus_topics");
            printParameter(out, "operator.roles.qualify.items", detail, "item_counts");
            var tokens = displayValue(detail.configurationParameters().get("expected_tokens"));
            var time = displayValue(detail.configurationParameters().get("expected_time"));
            if (!"-".equals(tokens) || !"-".equals(time)) {
                out.println(MessageCatalogue.format("operator.roles.qualify.expected",
                                    Map.of("tokens", tokens, "time", time)));
            }
            out.flush();
        }

        private static void printParameter(PrintWriter out, String key, RoleDetailResource detail, String parameter) {
            var value = displayValue(detail.configurationParameters().get(parameter));
            if (!"-".equals(value)) {
                out.println(MessageCatalogue.format(key, Map.of("value", value)));
            }
        }

        private static @Nullable Map<String, Object> measurements(@Nullable String body) {
            if (body == null || body.isBlank()) {
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

        private static void printMeasurements(PrintWriter out, Map<String, Object> measurements) {
            out.println(MessageCatalogue.get("operator.roles.qualify.measurements"));
            MEASUREMENT_LABELS.forEach((field, key) -> printMeasurement(out, measurements, field, key));
            var recordPath = measurements.get(RECORD_PATH);
            if (recordPath != null) {
                out.println(MessageCatalogue.format("operator.roles.qualify.record",
                                    Map.of("path", OutputSafety.stripControlSequences(String.valueOf(recordPath)))));
            }
        }

        private static void printMeasurement(PrintWriter out, Map<String, Object> map, String parameter, String key) {
            var value = map.get(parameter);
            if (value != null) {
                out.println("  " + MessageCatalogue.format(key,
                                    Map.of("value", OutputSafety.stripControlSequences(displayValue(value)))));
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

    private static Map<String, Object> configuration(RoleDetailResource detail) {
        return Map.of(
                            PARAM_HARNESS, valueOrDash(stringParameter(detail, PARAM_HARNESS)),
                            PARAM_MODEL, valueOrDash(stringParameter(detail, PARAM_MODEL)),
                            PARAM_EFFORT, valueOrDash(stringParameter(detail, PARAM_EFFORT)));
    }

    private static @Nullable String stringParameter(RoleDetailResource detail, String parameter) {
        return detail.configurationParameters().get(parameter) instanceof String value ? value : null;
    }

    private static String displayValue(@Nullable Object value) {
        return switch (value) {
            case null -> "-";
            case JsonNumber(String text) -> text;
            case List<?> list -> list.isEmpty() ? "-"
                                : OutputSafety.stripControlSequences(
                                String.join(", ", list.stream().map(String::valueOf).toList()));
            case Map<?, ?> map -> OutputSafety.stripControlSequences(JsonTree.write(map));
            default -> OutputSafety.stripControlSequences(String.valueOf(value));
        };
    }

    private static String valueOrDash(@Nullable String value) {
        return value == null || value.isBlank() ? "-" : OutputSafety.stripControlSequences(value);
    }

    private static String roleTarget(String role, @Nullable String project) {
        return withProject(PATH_ROLES + "/" + OperatorUrlUtil.encodePathSegment(role), project);
    }

    private static String withProject(String target, @Nullable String project) {
        return project == null || project.isBlank() ? target
                            : target + QUERY_PROJECT + OperatorUrlUtil.encodeQueryValue(project);
    }


}
