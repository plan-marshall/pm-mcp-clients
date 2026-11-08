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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.dialogue.OnboardingSource;
import de.planmarshall.operator.dialogue.ProjectOnboardingSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The two dialogue sources ({@code CLI-UC-S-06}, {@code CLI-UC-P-01}): the machine onboarding reads
 * {@code GET /api/v1/onboarding/state} and sends {@code POST /api/v1/onboarding/step}, a project's
 * setup reads and advances its own resource, and a runtime that answers no state is refused.
 */
@DisplayName("The dialogue sources")
class DialogueSourceTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private OperatorContext context() {
        return new OperatorContext(fixture.environment(), tempDir, true);
    }

    /**
     * @param body a request body
     * @return the {@code link} the body sends, or {@code null} for an empty or unreadable body
     */
    private static @Nullable String linkOf(String body) {
        if (body.isBlank()) {
            return null;
        }
        try {
            return JsonTree.string(JsonTree.asObject(JsonTree.parse(body)), "link");
        } catch (IOException | IllegalArgumentException _) {
            return null;
        }
    }

    @Test
    @DisplayName("the machine onboarding reads its state and sends the chosen link")
    void readsAndSendsTheMachineOnboarding() throws Exception {
        fixture.start((request, response) -> {
            if (OnboardingSource.STATE_PATH.equals(request.target())) {
                response.json(200, """
                                    {"state":"onboard.mode","summary":"Set up this machine for plan-marshall",
                                     "completed_steps":0,
                                     "links":{"begin":{"href":"/api/v1/onboarding/language","method":"POST","next":"language.choose","preselected":true}}}
                                    """);
            } else if (OnboardingSource.STEP_PATH.equals(request.target())) {
                assertTrue(request.bodyText().contains("\"link\":\"begin\""), request.bodyText());
                response.json(200, """
                                    {"state":"language.choose","summary":"Choose your languages",
                                     "completed_steps":0}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var context = context();
        var source = new OnboardingSource(context.runtime(true), context.gate());
        var frame = source.fetch();
        var next = source.choose("begin");

        assertAll(
                            () -> assertEquals("onboard.mode", frame.stateId()),
                            () -> assertEquals("language.choose", next.stateId()));
    }

    @Test
    @DisplayName("a project's setup reads its identity and sends the chosen link")
    void readsAndSendsTheProjectSetup() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/cui-parent/setup".equals(request.target())) {
                if ("set".equals(linkOf(request.bodyText()))) {
                    response.json(200, """
                                        {"project_id":"cui-parent","state":"project.completed",
                                         "step":10,"total_steps":10,"summary":"cui-parent is ready","links":{}}
                                        """);
                } else {
                    response.json(200, """
                                        {"project_id":"cui-parent","state":"project.identity",
                                         "step":2,"total_steps":10,"id":"cui-parent",
                                         "name":"cui-parent","pilot":false,
                                         "links":{"set":{"href":"/api/v1/projects/cui-parent/setup","method":"POST"}}}
                                        """);
                }
            } else {
                response.send(404, null, "");
            }
        });

        var source = new ProjectOnboardingSource(context().runtime(true), context().gate(), "cui-parent");
        var frame = source.fetch();
        var next = source.choose("set");

        assertAll(
                            () -> assertEquals("project.identity", frame.stateId()),
                            () -> assertEquals("cui-parent", frame.facts().get("id")),
                            () -> assertEquals("project.completed", next.stateId()),
                            () -> assertTrue(next.isFinal()));
    }

    @Test
    @DisplayName("a runtime that answers no state is refused")
    void refusesAnUnreadableState() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));

        var onboarding = new OnboardingSource(context().runtime(true), context().gate());
        var project = new ProjectOnboardingSource(context().runtime(true), context().gate(), "cui-parent");
        assertAll(
                            () -> assertThrows(IOException.class, onboarding::fetch),
                            () -> assertThrows(IOException.class, project::fetch));
    }

    @Test
    @DisplayName("an onboarding source constructed with an aspect list follows that size")
    void onboardingSourceFollowsInjectedAspectList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, "{\"state\":\"onboard.mode\",\"completed_steps\":0,\"links\":{}}");
            } else {
                response.send(404, null, "");
            }
        });

        var context = context();
        var aspects = List.of(OnboardingAspect.LANGUAGE, OnboardingAspect.HARNESSES);
        var source = new OnboardingSource(context.runtime(true), context.gate(), aspects);
        var frame = source.fetch();

        assertEquals(2, frame.totalSteps());
    }

    @Test
    @DisplayName("a project setup with a custom total_steps reports that total")
    void projectSetupWithCustomTotalStepsReportsThatTotal() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/my-proj/setup".equals(request.target())) {
                response.json(200, "{\"state\":\"project.identity\",\"total_steps\":12,\"completed_steps\":0,\"links\":{}}");
            } else {
                response.send(404, null, "");
            }
        });

        var context = context();
        var source = new ProjectOnboardingSource(context.runtime(true), context.gate(), "my-proj");
        var frame = source.fetch();

        assertEquals(12, frame.totalSteps());
    }
}
