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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

import de.planmarshall.api.source.git.ChangedFile;
import de.planmarshall.api.source.git.LocalChangesCaptureResult;
import de.planmarshall.api.source.git.LocalChangesPreview;
import de.planmarshall.api.source.git.PrAdoptResult;
import de.planmarshall.api.source.git.PullRequestObservation;
import de.planmarshall.api.source.git.RateLimitStatus;
import de.planmarshall.api.source.git.ScreeningOutcome;
import de.planmarshall.api.source.git.SourceLane;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("GitSourceCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class GitSourceCommandTest {

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private int run(String... args) {
        var context = new OperatorContext(fixture.environment(), fixture.base().getParent());
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        return fixture.start(handler);
    }

    private static List<ChangedFile> files() {
        return List.of(new ChangedFile("doc/concepts/forgejo.adoc", 12, 3, true, false));
    }

    private static String observation(int number, String state, boolean writable, ScreeningOutcome screening) {
        return new PullRequestObservation("https://codeberg.org/plan-marshall/pm-mcp-clients/pulls/" + number,
                            number, "Add a Codeberg provider for issues", "octocat", "codeberg-provider",
                            state, writable, files(), screening, RateLimitStatus.unlimited()).toJson();
    }

    private static String eligiblePreview() {
        return new LocalChangesPreview(null, "main", files(), "pr", true, true, null).toJson();
    }

    private static String ineligiblePreview() {
        return new LocalChangesPreview(null, "main", files(), "landing.files", false, false,
                            "landing.files").toJson();
    }

    private static String adoptResult() {
        return new PrAdoptResult("adopt-pr-4711", SourceLane.ADOPT, "codeberg-provider", null).toJson();
    }

    private static String captureResult() {
        return new LocalChangesCaptureResult("capture-local-changes-my-proj", SourceLane.LAND,
                            "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678", null).toJson();
    }

    private static FakeRuntimeServer.Handler gitSourceHandler(String preview) {
        return (request, response) -> {
            var target = request.target();
            if (target.endsWith("/sources/pr/observe")) {
                var flagged = request.bodyText().contains("6677");
                response.json(200, observation(4711, PullRequestObservation.STATE_OPEN, true,
                                    flagged ? ScreeningOutcome.FLAGGED : ScreeningOutcome.CLEAN));
            } else if (target.endsWith("/sources/pr/adopt")) {
                response.json(201, adoptResult());
            } else if (target.endsWith("/sources/local-changes/preview")) {
                response.json(200, preview);
            } else if (target.endsWith("/sources/local-changes/capture")) {
                response.json(201, captureResult());
            } else {
                response.send(404, null, "");
            }
        };
    }

    @Test
    @DisplayName("git-source without subcommands prints usage")
    void usage() {
        assertEquals(0, run("git-source"));
        assertTrue(out.toString().contains("Usage: pm-operator git-source"), out.toString());
    }

    @Test
    @DisplayName("adopt-pr observes, adopts, and reports the created plan")
    void adoptCleanPullRequest() throws Exception {
        var server = runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(0, run("git-source", "adopt-pr", "4711", "--project", "my-proj", "--yes"));
        assertTrue(out.toString().contains("adopt-pr-4711 adopted the pull request"), out.toString());
        assertTrue(out.toString().contains("adopt lane"), out.toString());

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.bodyText().contains("\"pr_url\":\"4711\""), recorded.bodyText());
    }

    @Test
    @DisplayName("adopt-pr needs an answer for a flagged pull request and refuses --no-input")
    void adoptFlaggedNeedsAcceptance() throws Exception {
        runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(GitSourceCommand.EXIT_NEEDS_INPUT,
                            run("git-source", "adopt-pr", "6677", "--project", "my-proj", "--no-input"));
        assertTrue(out.toString().contains("Adopt it anyway"), out.toString());
        assertTrue(out.toString().contains("Confirmation required (--yes)."), out.toString());
    }

    @Test
    @DisplayName("adopt-pr accepts a flagged pull request with an explicit screening acceptance")
    void adoptFlaggedWithAcceptance() throws Exception {
        var server = runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(0, run("git-source", "adopt-pr", "6677", "--project", "my-proj",
                            "--accept-screening", "I read the flagged text"));
        assertTrue(out.toString().contains("adopted the pull request"), out.toString());

        var observe = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(observe.bodyText().contains("\"pr_url\":\"6677\""));
        var adopt = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(adopt.bodyText().contains("\"screening_bypass_reason\""), adopt.bodyText());
    }

    @Test
    @DisplayName("adopt-pr reports a pull request that is not open")
    void adoptClosedPullRequest() throws Exception {
        runtime((request, response) -> response.json(200,
                            observation(7001, PullRequestObservation.STATE_CLOSED, true, ScreeningOutcome.CLEAN)));

        assertEquals(1, run("git-source", "adopt-pr", "7001", "--project", "my-proj", "--yes"));
        assertTrue(out.toString().contains("not open"), out.toString());
    }

    @Test
    @DisplayName("local-changes previews, captures, and reports the snapshot commit")
    void captureLocalChanges() throws Exception {
        var server = runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(0, run("git-source", "local-changes", "--project", "my-proj", "--yes"));
        assertTrue(out.toString().contains("captured the local changes"), out.toString());
        assertTrue(out.toString().contains("snapshot commit a1b2c3d4"), out.toString());
        assertTrue(out.toString().contains("land lane"), out.toString());

        var preview = server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("GET", preview.method());
        var capture = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(capture.bodyText().contains("\"confirmed\":true"), capture.bodyText());
    }

    @Test
    @DisplayName("local-changes needs the confirmation and refuses --no-input")
    void captureNeedsConfirmation() throws Exception {
        runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(GitSourceCommand.EXIT_NEEDS_INPUT,
                            run("git-source", "local-changes", "--project", "my-proj", "--no-input"));
        assertTrue(out.toString().contains("Confirmation required (--yes)."), out.toString());
    }

    @Test
    @DisplayName("local-changes refuses where the landing is landing.files (PM-WF-25)")
    void captureRefusesLandingFiles() throws Exception {
        runtime(gitSourceHandler(ineligiblePreview()));

        assertEquals(1, run("git-source", "local-changes", "--project", "my-proj", "--yes"));
        assertTrue(out.toString().contains("cannot land"), out.toString());
        assertTrue(out.toString().contains("PM-WF-25"), out.toString());
    }

    @Test
    @DisplayName("start plan --adopt-pr delegates to the git source flow")
    void startPlanAdoptDelegates() throws Exception {
        var server = runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(0, run("start", "plan", "my-proj", "--adopt-pr", "4711", "--yes"));
        assertTrue(out.toString().contains("adopt-pr-4711 adopted the pull request"), out.toString());

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.target().endsWith("/api/v1/projects/my-proj/sources/pr/observe"),
                            recorded.target());
    }

    @Test
    @DisplayName("start plan --local-changes delegates to the git source flow")
    void startPlanLocalChangesDelegates() throws Exception {
        runtime(gitSourceHandler(eligiblePreview()));

        assertEquals(0, run("start", "plan", "my-proj", "--local-changes", "--yes"));
        assertTrue(out.toString().contains("captured the local changes"), out.toString());
    }

    @Test
    @DisplayName("adopt-pr reports a runtime refusal with the reason")
    void adoptRefused() throws Exception {
        runtime((request, response) -> {
            if (request.target().endsWith("/sources/pr/observe")) {
                response.json(200, observation(7002, PullRequestObservation.STATE_OPEN, true,
                                    ScreeningOutcome.CLEAN));
            } else {
                response.json(409, "{\"error\":\"start_refused\",\"reason\":\"rate_limited\"}");
            }
        });

        assertEquals(1, run("git-source", "adopt-pr", "7002", "--project", "my-proj", "--yes"));
        assertTrue(out.toString().contains("rate limited"), out.toString());
    }
}
