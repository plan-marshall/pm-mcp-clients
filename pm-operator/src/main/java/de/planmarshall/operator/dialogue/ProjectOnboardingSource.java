/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.dialogue;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.OperatorGate;
import de.planmarshall.operator.OperatorUrlUtil;

/**
 * A project's onboarding as a {@link StateDialogue.Source}: the run a project's setup reports
 * ({@code CLI-UC-P-01}). For an enrolled project the resource is the project's setup, and a chosen
 * link is sent as the same request with the link's id, so the runtime keeps the run it already
 * holds.
 *
 * <p>A run that has not been enrolled yet has no project id: it is addressed by its entry, the
 * resource {@code /api/v1/entries/{entry_id}} the shared design names for a pre-enrolment run, and
 * an act is sent to the entry's link surface. See {@link #forEntry}.
 */
public final class ProjectOnboardingSource implements StateDialogue.Source {

    /** The default step total of a project's onboarding when the runtime reports none. */
    static final int TOTAL_STEPS = BeingAddedProject.DEFAULT_TOTAL_STEPS;

    private static final String PROJECTS_API = "/api/v1/projects/";
    private static final String ENTRIES_API = "/api/v1/entries/";
    private static final String LINKS = "/links/";

    private final RuntimeClient runtime;
    private final OperatorGate gate;
    private final String path;
    private final boolean entryScoped;

    /**
     * @param runtime   the runtime client
     * @param gate      the operator gate an answer is sent through
     * @param projectId the project whose run is continued
     */
    public ProjectOnboardingSource(RuntimeClient runtime, OperatorGate gate, String projectId) {
        this(runtime, gate, PROJECTS_API + OperatorUrlUtil.encodePathSegment(
                            Objects.requireNonNull(projectId, "projectId")) + "/setup", false);
    }

    private ProjectOnboardingSource(RuntimeClient runtime, OperatorGate gate, String path, boolean entryScoped) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.path = path;
        this.entryScoped = entryScoped;
    }

    /**
     * A run that has not been enrolled yet, addressed by the entry its continue link names
     * ({@code project-onboarding.adoc#several}).
     *
     * @param runtime the runtime client
     * @param gate    the operator gate an answer is sent through
     * @param entryId the entry the run is addressed by
     * @return the entry-scoped source
     */
    public static ProjectOnboardingSource forEntry(RuntimeClient runtime, OperatorGate gate, String entryId) {
        return new ProjectOnboardingSource(runtime, gate, ENTRIES_API + OperatorUrlUtil.encodePathSegment(
                            Objects.requireNonNull(entryId, "entryId")), true);
    }

    @Override
    public StateDialogue.Frame fetch() throws IOException {
        var response = runtime.send(HttpRequest.get(path));
        if (response.status() != 200) {
            throw new IOException("the project setup answered HTTP " + response.status());
        }
        return StateDialogue.fromJson(response.bodyText(), TOTAL_STEPS, true);
    }

    @Override
    public StateDialogue.Frame choose(String linkId) throws IOException {
        gate.verifyAct();
        var response = entryScoped
                            ? runtime.send(HttpRequest.json("POST",
                            path + LINKS + OperatorUrlUtil.encodePathSegment(linkId), "{}"))
                            : runtime.send(HttpRequest.json("POST", path,
                            JsonTree.write(Map.of("link", linkId))));
        if (response.status() != 200) {
            throw new IOException("the project setup answered HTTP " + response.status());
        }
        return StateDialogue.fromJson(response.bodyText(), TOTAL_STEPS, true);
    }
}
