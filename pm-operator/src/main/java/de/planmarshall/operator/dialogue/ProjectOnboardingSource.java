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
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.OperatorUrlUtil;

/**
 * A project's onboarding as a {@link StateDialogue.Source}: the run a project's setup reports
 * ({@code CLI-UC-P-01}). The resource is the project's setup, and a chosen link is sent as the same
 * request with the link's id, so the runtime keeps the run it already holds.
 */
public final class ProjectOnboardingSource implements StateDialogue.Source {

    private static final String PROJECTS_API = "/api/v1/projects/";
    private static final int TOTAL_STEPS = 10;

    private final RuntimeClient runtime;
    private final String path;

    /**
     * @param runtime   the runtime client
     * @param projectId the project whose run is continued
     */
    public ProjectOnboardingSource(RuntimeClient runtime, String projectId) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.path = PROJECTS_API + OperatorUrlUtil.encodePathSegment(
                            Objects.requireNonNull(projectId, "projectId")) + "/setup";
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
        var response = runtime.send(HttpRequest.json("POST", path,
                            JsonTree.write(Map.of("link", linkId))));
        if (response.status() != 200) {
            throw new IOException("the project setup answered HTTP " + response.status());
        }
        return StateDialogue.fromJson(response.bodyText(), TOTAL_STEPS, true);
    }
}
