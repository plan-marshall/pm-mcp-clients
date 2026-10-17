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
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectExplorer;

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
        var response = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
        var out = spec.commandLine().getOut();
        if (response.status() == 200) {
            var parsed = JsonTree.parse(response.body());
            ProjectExplorer explorer;
            if (parsed instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                var m = (Map<String, Object>) map;
                explorer = ProjectExplorer.fromMap(m);
            } else if (parsed instanceof List<?> list) {
                var all = new ArrayList<EnrolledProject>();
                var favs = new ArrayList<EnrolledProject>();
                for (var item : list) {
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
                explorer = new ProjectExplorer(favs, List.of(), all, List.of());
            } else {
                explorer = new ProjectExplorer(List.of(), List.of(), List.of(), List.of());
            }
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
            var query = filter.trim().toLowerCase(Locale.ROOT);
            var matches = explorer.allProjects().stream()
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
            return;
        }

        // 1. Being added
        if (!explorer.beingAdded().isEmpty()) {
            out.println(MessageCatalogue.hasKey("project.adding.title")
                                ? MessageCatalogue.format(locale, "project.adding.title", Map.of())
                                : "Being added");
            for (var b : explorer.beingAdded()) {
                var safePath = OutputSafety.stripControlSequences(b.checkoutPath());
                var safeStep = OutputSafety.stripControlSequences(b.currentStep());
                out.printf("    %-30s %s (%d of %d)%n", safePath, safeStep, b.stepNumber(), b.totalSteps());
            }
        }

        // 2. Favourites
        if (!explorer.favourites().isEmpty()) {
            out.println(MessageCatalogue.hasKey("project.explorer.favourites")
                                ? MessageCatalogue.format(locale, "project.explorer.favourites", Map.of())
                                : "Favourites");
            for (var p : explorer.favourites()) {
                printProjectRow(out, p);
            }
        }

        // 3. Recent
        if (!explorer.recent().isEmpty()) {
            out.println(MessageCatalogue.hasKey("project.explorer.recent")
                                ? MessageCatalogue.format(locale, "project.explorer.recent", Map.of())
                                : "Recent");
            for (var p : explorer.recent()) {
                printProjectRow(out, p);
            }
        }

        // 4. All projects (by name)
        if (!explorer.allProjects().isEmpty()) {
            var allTitle = MessageCatalogue.hasKey("project.explorer.all")
                                ? MessageCatalogue.format(locale, "project.explorer.all", Map.of())
                                : "All projects";
            var byName = MessageCatalogue.hasKey("project.explorer.by_name")
                                ? MessageCatalogue.format(locale, "project.explorer.by_name", Map.of())
                                : "by name";
            out.println(allTitle + " (" + byName + ")");
            for (var p : explorer.allProjects()) {
                printProjectRow(out, p);
            }
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

    public static String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        try {
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
        } catch (DateTimeParseException _) {
            return instant.toString();
        }
    }
}
