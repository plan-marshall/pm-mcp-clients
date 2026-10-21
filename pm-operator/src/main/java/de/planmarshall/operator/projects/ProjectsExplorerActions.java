/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.projects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.operator.OutputSafety;
import de.planmarshall.operator.tui.TuiMenuEntry;

/**
 * The entries of a project's inline actions menu ({@code doc/design/terminal-client/17-projects.adoc}):
 * the actions of a ready project, the disabled _Add_ actions of one that is not ready, the two
 * choices of an onboarding that has not enrolled yet, and the two choices of a released project.
 *
 * <p>The menu is the same for {@code Enter} on the row and for the direct keys; it names every
 * action the row's state offers, in the design's order.
 */
public final class ProjectsExplorerActions {

    /** Continue an open start entry. */
    public static final String CONTINUE_STARTING = "continue_starting";

    /** Continue the setup of a project that is not ready. */
    public static final String CONTINUE_SETUP = "continue_setup";

    /** Continue an onboarding that has not enrolled yet. */
    public static final String CONTINUE_ONBOARDING = "continue_onboarding";

    /** Add an epic to the project. */
    public static final String ADD_EPIC = "add_epic";

    /** Add a plan to the project. */
    public static final String ADD_PLAN = "add_plan";

    /** Open the project's running work. */
    public static final String RUNNING_WORK = "running_work";

    /** Run the project's setup. */
    public static final String SETUP = "setup";

    /** Mark or unmark the project as a favourite. */
    public static final String FAVOURITE = "favourite";

    /** Abandon an onboarding that has not enrolled yet. */
    public static final String ABANDON = "abandon";

    /** Enrol a released project again. */
    public static final String ENROL_AGAIN = "enrol_again";

    /** Open a released project's ended work. */
    public static final String ENDED_WORK = "ended_work";

    private ProjectsExplorerActions() {
    }

    /**
     * @param locale      the interface locale
     * @param project     the project of the row
     * @param openEntries the project's open start entries
     * @return the project's actions in the design's order
     */
    public static List<TuiMenuEntry> forProject(String locale, EnrolledProject project,
                        List<ProjectOpenEntry> openEntries) {
        if (project.isReleased() || project.readiness() == ProjectReadiness.RELEASED) {
            return released(locale, project);
        }
        var entries = new ArrayList<TuiMenuEntry>();
        if (project.readiness() == ProjectReadiness.SETUP_NEEDS) {
            entries.add(entry("c", CONTINUE_SETUP,
                                MessageCatalogue.format(locale, "project.action.continue_setup", Map.of())));
            var reason = notReadyReason(locale, project);
            entries.add(TuiMenuEntry.disabled("e", ADD_EPIC,
                                MessageCatalogue.format(locale, "project.action.add_epic", Map.of()), reason));
            entries.add(TuiMenuEntry.disabled("p", ADD_PLAN,
                                MessageCatalogue.format(locale, "project.action.add_plan", Map.of()), reason));
        } else {
            addContinueStarting(entries, locale, openEntries);
            entries.add(entry("e", ADD_EPIC,
                                MessageCatalogue.format(locale, "project.action.add_epic", Map.of())));
            entries.add(entry("p", ADD_PLAN,
                                MessageCatalogue.format(locale, "project.action.add_plan", Map.of())));
        }
        entries.add(entry("r", RUNNING_WORK,
                            MessageCatalogue.format(locale, "project.action.running_work", Map.of())));
        entries.add(entry("u", SETUP,
                            MessageCatalogue.format(locale, "project.action.setup", Map.of())));
        entries.add(entry("*", FAVOURITE, favouriteLabel(locale, project)));
        return List.copyOf(entries);
    }

    /**
     * @param locale the interface locale
     * @param being  the onboarding run of the row
     * @return the run's two choices
     */
    public static List<TuiMenuEntry> forBeingAdded(String locale, BeingAddedProject being) {
        return List.of(
                            entry("c", CONTINUE_ONBOARDING,
                                                MessageCatalogue.format(locale, "operator.projects.action.continue", Map.of())),
                            entry("x", ABANDON,
                                                MessageCatalogue.format(locale, "operator.projects.action.abandon", Map.of()))
        );
    }

    /**
     * @param locale  the interface locale
     * @param project the project that is not ready
     * @return why its _Add_ actions cannot run now
     */
    public static String notReadyReason(String locale, EnrolledProject project) {
        var need = project.setupNeedsSummary() != null && !project.setupNeedsSummary().isBlank()
                            ? OutputSafety.stripControlSequences(project.setupNeedsSummary())
                            : MessageCatalogue.format(locale, "project.state.setup", Map.of());
        return MessageCatalogue.format(locale, "project.action.disabled_not_ready",
                            Map.of("project", OutputSafety.stripControlSequences(project.projectId()), "need", need));
    }

    private static List<TuiMenuEntry> released(String locale, EnrolledProject project) {
        return List.of(
                            entry("a", ENROL_AGAIN,
                                                MessageCatalogue.format(locale, "project.action.enrol_again", Map.of())),
                            entry("E", ENDED_WORK,
                                                MessageCatalogue.format(locale, "project.action.ended_work", Map.of()))
        );
    }

    private static void addContinueStarting(List<TuiMenuEntry> entries, String locale,
                        List<ProjectOpenEntry> openEntries) {
        if (openEntries.isEmpty()) {
            return;
        }
        var count = openEntries.getFirst().sourceCount();
        entries.add(entry("c", CONTINUE_STARTING,
                            MessageCatalogue.format(locale, "start.entry.continue", Map.of("count", count))));
    }

    private static String favouriteLabel(String locale, EnrolledProject project) {
        return MessageCatalogue.format(locale,
                            project.isFavourite() ? "project.action.unfavourite" : "project.action.favourite",
                            Map.of());
    }

    private static TuiMenuEntry entry(String trigger, String action, String label) {
        return TuiMenuEntry.enabled(trigger, action, label);
    }
}
