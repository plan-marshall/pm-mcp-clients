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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;

/**
 * Resolves {@code --project ?} (and {@code --project} without a value) before picocli parses the
 * verb ({@code doc/design/terminal-client/02-levels.adoc#addressing}): it shows the explorer's
 * projects, favourites first, and replaces the question with the chosen project id.
 *
 * <p>It runs in {@link PmOperator#execute} for every verb that names {@code --project}, so a verb
 * implements the selector once and nowhere repeats it. In the console {@link PmConsole} passes its
 * own {@link LineReader}; outside it the selector reads from the context's terminal.
 */
public final class ProjectSelector {

    private static final String OPTION = "--project";
    private static final String OPTION_PREFIX = "--project=";
    private static final String QUESTION = "?";
    private static final String PROMPT_SUFFIX = "  \u203a ";
    private static final String LEAVE = "q";
    private static final int LEAVE_CODE = 0;
    private static final int NEEDS_INPUT_CODE = 3;
    private static final int FAILURE_CODE = 1;
    private static final int MAX_ATTEMPTS = 3;

    private ProjectSelector() {
    }

    /**
     * What {@link PmOperator#execute} does next.
     *
     * @param args     the arguments to parse, with the selector resolved
     * @param exitCode the exit code to return when {@code proceed} is {@code false}
     * @param proceed  {@code true} to continue parsing the verb
     */
    public record Outcome(List<String> args, int exitCode, boolean proceed) {

        /**
         * @param args the arguments to parse
         * @return an outcome that continues
         */
        public static Outcome proceeding(List<String> args) {
            return new Outcome(List.copyOf(args), 0, true);
        }

        /**
         * @param exitCode the code to return
         * @return an outcome that stops the verb
         */
        public static Outcome stopping(int exitCode) {
            return new Outcome(List.of(), exitCode, false);
        }
    }

    /**
     * @param context the operator context
     * @param out     standard output
     * @param err     standard error
     * @param args    the arguments of the verb
     * @return whether to parse the verb and with which arguments
     */
    public static Outcome rewrite(OperatorContext context, PrintWriter out, PrintWriter err, String[] args) {
        return rewrite(context, out, err, args, null);
    }

    /**
     * @param context the operator context
     * @param out     standard output
     * @param err     standard error
     * @param args    the arguments of the verb
     * @param reader  the console's reader, or {@code null} to read from the context's terminal
     * @return whether to parse the verb and with which arguments
     */
    public static Outcome rewrite(OperatorContext context, PrintWriter out, PrintWriter err, String[] args,
                        @Nullable LineReader reader) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(args, "args");
        if (!asksForProject(args)) {
            return Outcome.proceeding(List.of(args));
        }
        var locale = context.clientEnvironment().locale();
        if (!context.isInteractiveTerminal() || List.of(args).contains("--no-input")) {
            err.println(MessageCatalogue.format(locale, "operator.projects.select.no_input", Map.of()));
            err.flush();
            return Outcome.stopping(NEEDS_INPUT_CODE);
        }
        var projects = fetch(context, out, err, locale);
        if (projects == null) {
            return Outcome.stopping(FAILURE_CODE);
        }
        if (projects.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "project.explorer.empty", Map.of()));
            out.flush();
            return Outcome.stopping(FAILURE_CODE);
        }
        printList(out, locale, projects);
        var decision = choose(context, err, locale, projects, reader);
        if (!decision.proceed()) {
            return Outcome.stopping(decision.exitCode());
        }
        var resolved = replace(args, decision.project().projectId());
        return Outcome.proceeding(resolved);
    }

    /**
     * @param explorer the explorer's report
     * @return the projects in the explorer's order: favourites, recent, then the rest
     */
    public static List<EnrolledProject> order(ProjectExplorer explorer) {
        Objects.requireNonNull(explorer, "explorer");
        var ordered = new ArrayList<EnrolledProject>();
        var seen = new HashSet<String>();
        append(ordered, seen, explorer.favourites());
        append(ordered, seen, explorer.recent());
        append(ordered, seen, explorer.allProjects());
        return List.copyOf(ordered);
    }

    /**
     * @param text     the operator's answer
     * @param projects the projects as they were printed
     * @return the chosen project, or {@code null} for _leave_
     */
    public static @Nullable EnrolledProject parseChoice(@Nullable String text, List<EnrolledProject> projects) {
        if (text == null) {
            return null;
        }
        var answer = text.trim();
        if (answer.isEmpty() || LEAVE.equalsIgnoreCase(answer)) {
            return null;
        }
        try {
            int index = Integer.parseInt(answer);
            return index >= 1 && index <= projects.size() ? projects.get(index - 1) : null;
        } catch (NumberFormatException _) {
            return projects.stream().filter(project -> project.projectId().equals(answer)).findFirst().orElse(null);
        }
    }

    private static boolean asksForProject(String[] args) {
        for (int index = 0; index < args.length; index++) {
            var arg = args[index];
            if (OPTION.equals(arg)) {
                if (index + 1 >= args.length) {
                    return true;
                }
                var value = args[index + 1];
                if (value.isBlank() || QUESTION.equals(value) || value.startsWith("-")) {
                    return true;
                }
            } else if (arg.startsWith(OPTION_PREFIX)) {
                var value = arg.substring(OPTION_PREFIX.length());
                if (value.isBlank() || QUESTION.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> replace(String[] args, String projectId) {
        var resolved = new ArrayList<String>(args.length);
        int index = 0;
        while (index < args.length) {
            var arg = args[index];
            if (OPTION.equals(arg)) {
                resolved.add(OPTION);
                resolved.add(projectId);
                if (index + 1 < args.length && (QUESTION.equals(args[index + 1]) || args[index + 1].isBlank())) {
                    index++;
                }
            } else if (arg.startsWith(OPTION_PREFIX)) {
                resolved.add(OPTION_PREFIX + projectId);
            } else {
                resolved.add(arg);
            }
            index++;
        }
        return List.copyOf(resolved);
    }

    private static @Nullable List<EnrolledProject> fetch(OperatorContext context, PrintWriter out, PrintWriter err,
                        String locale) {
        try {
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
            if (response.status() != 200) {
                err.println(MessageCatalogue.format(locale, "operator.projects.select.load_failed",
                                    Map.of("status", response.status())));
                err.flush();
                return null;
            }
            return order(ProjectExplorer.fromJson(response.bodyText()));
        } catch (IOException failure) {
            err.println(OutputSafety.stripControlSequences(failure.getMessage()));
            err.flush();
            return null;
        }
    }

    private static void printList(PrintWriter out, String locale, List<EnrolledProject> projects) {
        out.println(MessageCatalogue.format(locale, "operator.projects.select.title", Map.of()));
        for (int index = 0; index < projects.size(); index++) {
            var project = projects.get(index);
            out.printf("  %d) %-28s %s%n", index + 1,
                                OutputSafety.stripControlSequences(project.projectId()),
                                readiness(locale, project));
        }
        out.flush();
    }

    /**
     * @param project  the chosen project, or {@code null} when the selector does not proceed
     * @param proceed  {@code true} to replace the question with the project
     * @param exitCode the code to return when it does not proceed
     */
    private record Decision(@Nullable EnrolledProject project, boolean proceed, int exitCode) {

        private static Decision chosen(EnrolledProject project) {
            return new Decision(Objects.requireNonNull(project, "project"), true, LEAVE_CODE);
        }

        private static Decision left() {
            return new Decision(null, false, LEAVE_CODE);
        }

        private static Decision noInput() {
            return new Decision(null, false, NEEDS_INPUT_CODE);
        }

        private static Decision exhausted() {
            return new Decision(null, false, FAILURE_CODE);
        }
    }

    private static Decision choose(OperatorContext context, PrintWriter err, String locale,
                        List<EnrolledProject> projects, @Nullable LineReader reader) {
        if (reader != null) {
            return choose(reader, err, locale, projects);
        }
        Terminal terminal;
        try {
            terminal = context.createTerminal();
        } catch (IOException _) {
            err.println(MessageCatalogue.format(locale, "operator.projects.select.no_input", Map.of()));
            err.flush();
            return Decision.noInput();
        }
        try {
            return choose(LineReaderBuilder.builder().terminal(terminal).build(), err, locale, projects);
        } finally {
            // A terminal lent by the console is not this selector's to close.
            if (context.ownsTerminal(terminal)) {
                try {
                    terminal.close();
                } catch (IOException _) {
                    // The dialogue is over; a terminal that cannot be closed changes no answer.
                }
            }
        }
    }

    private static Decision choose(LineReader reader, PrintWriter err, String locale,
                        List<EnrolledProject> projects) {
        var prompt = MessageCatalogue.format(locale, "operator.projects.select.prompt",
                            Map.of("count", projects.size())) + PROMPT_SUFFIX;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String answer;
            try {
                answer = reader.readLine(prompt);
            } catch (EndOfFileException | UserInterruptException _) {
                return Decision.left();
            }
            if (answer == null) {
                return Decision.left();
            }
            var trimmed = answer.trim();
            if (trimmed.isEmpty() || LEAVE.equalsIgnoreCase(trimmed)) {
                return Decision.left();
            }
            var chosen = parseChoice(trimmed, projects);
            if (chosen != null) {
                return Decision.chosen(chosen);
            }
            err.println(MessageCatalogue.format(locale, "operator.projects.select.invalid",
                                Map.of("input", OutputSafety.stripControlSequences(answer))));
            err.flush();
        }
        return Decision.exhausted();
    }

    private static String readiness(String locale, EnrolledProject project) {
        if (project.isReleased() || project.readiness() == ProjectReadiness.RELEASED) {
            return MessageCatalogue.format(locale, "project.state.released", Map.of());
        }
        if (project.readiness() == ProjectReadiness.SETUP_NEEDS) {
            var need = project.setupNeedsSummary() != null && !project.setupNeedsSummary().isBlank()
                                ? OutputSafety.stripControlSequences(project.setupNeedsSummary())
                                : MessageCatalogue.format(locale, "project.state.setup", Map.of());
            return MessageCatalogue.format(locale, "project.state.setup_needs", Map.of("need", need));
        }
        return MessageCatalogue.format(locale, "project.state.ready", Map.of());
    }

    private static void append(List<EnrolledProject> ordered, HashSet<String> seen, List<EnrolledProject> projects) {
        for (var project : projects) {
            if (seen.add(project.projectId())) {
                ordered.add(project);
            }
        }
    }
}
