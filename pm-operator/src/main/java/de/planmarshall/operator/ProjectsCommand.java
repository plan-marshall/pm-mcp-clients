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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.overview.OverviewFullScreenApp.InitialSelection;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator projects [--list] [--filter <text>]}: lists enrolled projects (01-commands.adoc#commands,
 * 17-projects.adoc).
 */
@Command(name = "projects", description = "Explores and lists projects on this machine.")
@OperatorVerb
public final class ProjectsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--list", description = "Prints the projects list into the terminal scrollback.")
    boolean list;

    @Option(names = {"-f", "--filter"}, description = "Filter projects by name or ID.")
    @Nullable
    String filter;

    ProjectsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        if (!list && (filter == null || filter.isBlank())) {
            Terminal terminal = null;
            try {
                terminal = context.createTerminal();
            } catch (IOException _) {
                // Without a terminal the scrollback list is the whole view.
            }
            try {
                var availability = terminal == null ? null
                                    : TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS);
                if (availability != null && availability.available()) {
                    var app = new OverviewFullScreenApp(context, terminal, InitialSelection.explorerRoot(),
                                        Clock.systemUTC(), new OverviewStateStore(context.stateDirectory()),
                                        context.noticeBuffer());
                    var result = app.run(spec.commandLine().getOut());
                    if (result != TuiScreenDriver.Result.TOO_NARROW) {
                        return 0;
                    }
                } else if (availability != null) {
                    TuiScreenDriver.printUnavailable(context, spec.commandLine().getErr(), availability);
                }
            } finally {
                if (context.ownsTerminal(terminal)) {
                    terminal.close();
                }
            }
        }
        return printList();
    }

    private int printList() throws IOException {
        var response = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
        var out = spec.commandLine().getOut();
        if (response.status() == 200) {
            var explorer = parseExplorer(response.body());
            renderExplorer(out, explorer, filter, context.clientEnvironment().locale());
        } else {
            var err = spec.commandLine().getErr();
            err.printf("Failed to list projects: HTTP %d%n", response.status());
            err.flush();
            return 1;
        }
        out.flush();
        return 0;
    }

    private static ProjectExplorer parseExplorer(byte[] body) throws IOException {
        var parsed = JsonTree.parse(body);
        return switch (parsed) {
            case Map<?, ?> map -> {
                @SuppressWarnings("unchecked")
                var m = (Map<String, Object>) map;
                yield ProjectExplorer.fromMap(m);
            }
            case List<?> items -> {
                var all = new ArrayList<EnrolledProject>();
                var favs = new ArrayList<EnrolledProject>();
                for (var item : items) {
                    if (item instanceof Map<?, ?> itemMap) {
                        @SuppressWarnings("unchecked")
                        var m = (Map<String, Object>) itemMap;
                        var p = EnrolledProject.fromMap(m);
                        all.add(p);
                        if (p.isFavourite()) {
                            favs.add(p);
                        }
                    }
                }
                yield new ProjectExplorer(favs, List.of(), all, List.of());
            }
            default -> new ProjectExplorer(List.of(), List.of(), List.of(), List.of());
        };
    }

    private static void renderExplorer(PrintWriter out, ProjectExplorer explorer, @Nullable String filter, String locale) {
        if (explorer.allProjects().isEmpty() && explorer.beingAdded().isEmpty() && explorer.favourites().isEmpty()) {
            out.println(MessageCatalogue.hasKey("project.explorer.empty")
                                ? MessageCatalogue.format(locale, "project.explorer.empty", Map.of())
                                : "No project is enrolled yet. Use 'pm-operator project add' to enrol one.");
            return;
        }

        out.println("Projects");
        out.println("─────────────────────────────────────────────────────────────────────────────");

        if (filter != null && !filter.isBlank()) {
            renderFiltered(out, explorer.allProjects(), filter);
            return;
        }

        renderBeingAdded(out, explorer.beingAdded(), locale);
        renderFavourites(out, explorer.favourites(), locale);
        renderRecent(out, explorer.recent(), locale);
        renderAll(out, explorer.allProjects(), locale);
    }

    private static void renderFiltered(PrintWriter out, List<EnrolledProject> allProjects, String filter) {
        var query = filter.trim().toLowerCase(Locale.ROOT);
        var matches = allProjects.stream()
                            .filter(p -> p.projectId().toLowerCase(Locale.ROOT).contains(query)
                                                || p.displayName().toLowerCase(Locale.ROOT).contains(query))
                            .toList();
        if (matches.isEmpty()) {
            out.printf("No projects matching '%s'.%n", OutputSafety.stripControlSequences(filter));
        } else {
            out.printf("Matches for '%s'%n", OutputSafety.stripControlSequences(filter));
            for (var p : matches) {
                printProjectRow(out, p);
            }
        }
    }

    private static void renderBeingAdded(PrintWriter out, List<BeingAddedProject> beingAdded, String locale) {
        if (beingAdded.isEmpty()) {
            return;
        }
        out.println(MessageCatalogue.hasKey("project.adding.title")
                            ? MessageCatalogue.format(locale, "project.adding.title", Map.of())
                            : "Being added");
        for (var b : beingAdded) {
            var safePath = OutputSafety.stripControlSequences(b.checkoutPath());
            var safeStep = OutputSafety.stripControlSequences(b.currentStep());
            out.printf("    %-30s %s (%d of %d)%n", safePath, safeStep, b.stepNumber(), b.totalSteps());
        }
    }

    private static void renderFavourites(PrintWriter out, List<EnrolledProject> favourites, String locale) {
        if (favourites.isEmpty()) {
            return;
        }
        out.println(MessageCatalogue.hasKey("project.explorer.favourites")
                            ? MessageCatalogue.format(locale, "project.explorer.favourites", Map.of())
                            : "Favourites");
        for (var p : favourites) {
            printProjectRow(out, p);
        }
    }

    private static void renderRecent(PrintWriter out, List<EnrolledProject> recent, String locale) {
        if (recent.isEmpty()) {
            return;
        }
        out.println(MessageCatalogue.hasKey("project.explorer.recent")
                            ? MessageCatalogue.format(locale, "project.explorer.recent", Map.of())
                            : "Recent");
        for (var p : recent) {
            printProjectRow(out, p);
        }
    }

    private static void renderAll(PrintWriter out, List<EnrolledProject> allProjects, String locale) {
        if (allProjects.isEmpty()) {
            return;
        }
        var allTitle = MessageCatalogue.hasKey("project.explorer.all")
                            ? MessageCatalogue.format(locale, "project.explorer.all", Map.of())
                            : "All projects";
        var byName = MessageCatalogue.hasKey("project.explorer.by_name")
                            ? MessageCatalogue.format(locale, "project.explorer.by_name", Map.of())
                            : "by name";
        out.println(allTitle + " (" + byName + ")");
        for (var p : allProjects) {
            printProjectRow(out, p);
        }
    }

    private static void printProjectRow(PrintWriter out, EnrolledProject project) {
        var mark = project.isFavourite() ? "  ★ " : "    ";
        var name = OutputSafety.stripControlSequences(project.displayName());
        var readiness = switch (project.readiness()) {
            case READY -> "ready";
            case RELEASED -> "released";
            case SETUP_NEEDS -> "setup needs: " + OutputSafety.stripControlSequences(
                                project.setupNeedsSummary() != null ? project.setupNeedsSummary() : "setup");
        };

        var sb = new StringBuilder();
        sb.append(mark).append(String.format("%-18s", name)).append("   ").append(readiness);

        if (project.needsOperatorCount() > 0) {
            sb.append("   needs you: ").append(project.needsOperatorCount());
        }

        if (project.activePlansCount() > 0 && project.activeEpicsCount() > 0) {
            sb.append("   ").append(project.activePlansCount()).append(" plans · ")
                                .append(project.activeEpicsCount()).append(project.activeEpicsCount() == 1 ? " epic" : " epics");
        } else if (project.activePlansCount() > 0) {
            sb.append("   ").append(project.activePlansCount())
                                .append(project.activePlansCount() == 1 ? " plan" : " plans");
        } else if (project.activeEpicsCount() > 0) {
            sb.append("   ").append(project.activeEpicsCount())
                                .append(project.activeEpicsCount() == 1 ? " epic" : " epics");
        }

        if (project.lastActive() != null) {
            sb.append("   ").append(formatRelativeTime(project.lastActive()));
        }

        out.println(sb.toString().stripTrailing());
    }

    public static String formatRelativeTime(@Nullable Instant instant) {
        if (instant == null) {
            return "";
        }
        var diff = Duration.between(instant, Instant.now());
        long days = diff.toDays();
        if (days == 0) {
            long hours = diff.toHours();
            if (hours > 0) {
                return hours + " h ago";
            }
            long minutes = diff.toMinutes();
            return minutes > 0 ? minutes + " min ago" : "just now";
        }
        if (days == 1) {
            return "yesterday";
        }
        if (days < 7) {
            return days + " d ago";
        }
        long weeks = days / 7;
        if (weeks < 5) {
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        }
        long months = days / 30;
        return months + (months == 1 ? " month ago" : " months ago");
    }
}
