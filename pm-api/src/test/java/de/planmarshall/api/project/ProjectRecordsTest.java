/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("ProjectRecordsTest")
class ProjectRecordsTest {

    @Test
    @DisplayName("validates closed enumerations and wire parsing")
    void enumerations() {
        for (ProjectReadiness readiness : ProjectReadiness.values()) {
            assertNotNull(readiness.value());
            assertEquals(readiness, ProjectReadiness.fromValue(readiness.value()));
            assertEquals(readiness, ProjectReadiness.parse(readiness.value()));
        }
        assertNull(ProjectReadiness.fromValue("unknown"));
        assertNull(ProjectReadiness.fromValue(null));
        assertThrows(IllegalArgumentException.class, () -> ProjectReadiness.parse("unknown"));

        for (ProjectOnboardingState state : ProjectOnboardingState.values()) {
            assertNotNull(state.value());
            assertEquals(state, ProjectOnboardingState.fromValue(state.value()));
            assertEquals(state, ProjectOnboardingState.parse(state.value()));
        }
        assertNull(ProjectOnboardingState.fromValue("unknown"));
        assertNull(ProjectOnboardingState.fromValue(null));
        assertThrows(IllegalArgumentException.class, () -> ProjectOnboardingState.parse("unknown"));

        for (ReleaseWaitStatus status : ReleaseWaitStatus.values()) {
            assertNotNull(status.value());
            assertEquals(status, ReleaseWaitStatus.fromValue(status.value()));
            assertEquals(status, ReleaseWaitStatus.parse(status.value()));
        }
        assertNull(ReleaseWaitStatus.fromValue("unknown"));
        assertNull(ReleaseWaitStatus.fromValue(null));
        assertThrows(IllegalArgumentException.class, () -> ReleaseWaitStatus.parse("unknown"));
    }

    @Test
    @DisplayName("validates EnrolledProject serialization and deserialization round-trip")
    void enrolledProjectRoundTrip() throws Exception {
        var now = Instant.parse("2026-10-17T10:00:00Z");
        var project = new EnrolledProject(
                "pm-mcp-clients",
                "PM MCP Clients",
                "https://github.com/plan-marshall/pm-mcp-clients",
                ProjectReadiness.READY,
                null,
                2,
                3,
                1,
                now,
                true,
                false
        );

        var json = project.toJson();
        assertNotNull(json);

        var roundTrip = EnrolledProject.fromJson(json);
        assertAll(
                () -> assertEquals(project.projectId(), roundTrip.projectId()),
                () -> assertEquals(project.displayName(), roundTrip.displayName()),
                () -> assertEquals(project.remoteUrl(), roundTrip.remoteUrl()),
                () -> assertEquals(project.readiness(), roundTrip.readiness()),
                () -> assertEquals(project.setupNeedsSummary(), roundTrip.setupNeedsSummary()),
                () -> assertEquals(project.needsOperatorCount(), roundTrip.needsOperatorCount()),
                () -> assertEquals(project.activePlansCount(), roundTrip.activePlansCount()),
                () -> assertEquals(project.activeEpicsCount(), roundTrip.activeEpicsCount()),
                () -> assertEquals(project.lastActive(), roundTrip.lastActive()),
                () -> assertTrue(roundTrip.isFavourite()),
                () -> assertFalse(roundTrip.isReleased())
        );

        // Fallback checks
        var minimal = EnrolledProject.fromMap(Map.of("id", "prj-min"));
        assertEquals("prj-min", minimal.projectId());
        assertEquals("prj-min", minimal.displayName());
        assertEquals(ProjectReadiness.READY, minimal.readiness());

        assertThrows(JsonFormatException.class, () -> EnrolledProject.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> EnrolledProject.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates BeingAddedProject round-trip")
    void beingAddedProjectRoundTrip() throws Exception {
        var beingAdded = new BeingAddedProject(
                "~/git/cui-parent",
                "Identity",
                2,
                10,
                "/api/v1/entries/ent-1/continue",
                "/api/v1/entries/ent-1/abandon"
        );

        var json = beingAdded.toJson();
        assertNotNull(json);

        var roundTrip = BeingAddedProject.fromJson(json);
        assertAll(
                () -> assertEquals(beingAdded.checkoutPath(), roundTrip.checkoutPath()),
                () -> assertEquals(beingAdded.currentStep(), roundTrip.currentStep()),
                () -> assertEquals(beingAdded.stepNumber(), roundTrip.stepNumber()),
                () -> assertEquals(beingAdded.totalSteps(), roundTrip.totalSteps()),
                () -> assertEquals(beingAdded.continueLink(), roundTrip.continueLink()),
                () -> assertEquals(beingAdded.abandonLink(), roundTrip.abandonLink())
        );

        assertThrows(JsonFormatException.class, () -> BeingAddedProject.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> BeingAddedProject.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates ProjectExplorer round-trip")
    void projectExplorerRoundTrip() throws Exception {
        var fav = new EnrolledProject(
                "p1", "Project 1", null, ProjectReadiness.READY, null, 0, 1, 0, null, true, false
        );
        var recent = new EnrolledProject(
                "p2", "Project 2", null, ProjectReadiness.SETUP_NEEDS, "sonar credential", 1, 0, 0, null, false, false
        );
        var beingAdded = new BeingAddedProject(
                "/tmp/p3", "Checkout", 1, 10, null, null
        );

        var explorer = new ProjectExplorer(
                List.of(fav),
                List.of(recent),
                List.of(fav, recent),
                List.of(beingAdded)
        );

        var json = explorer.toJson();
        assertNotNull(json);

        var roundTrip = ProjectExplorer.fromJson(json);
        assertAll(
                () -> assertEquals(1, roundTrip.favourites().size()),
                () -> assertEquals("p1", roundTrip.favourites().getFirst().projectId()),
                () -> assertEquals(1, roundTrip.recent().size()),
                () -> assertEquals("p2", roundTrip.recent().getFirst().projectId()),
                () -> assertEquals(2, roundTrip.allProjects().size()),
                () -> assertEquals(1, roundTrip.beingAdded().size()),
                () -> assertEquals("/tmp/p3", roundTrip.beingAdded().getFirst().checkoutPath())
        );

        assertThrows(JsonFormatException.class, () -> ProjectExplorer.fromJson("[]"));
    }

    @Test
    @DisplayName("validates FavouritesUpdateRequest round-trip")
    void favouritesUpdateRequestRoundTrip() throws Exception {
        var request = new FavouritesUpdateRequest(List.of("p1", "p2", "p3"));
        var json = request.toJson();
        assertNotNull(json);

        var roundTrip = FavouritesUpdateRequest.fromJson(json);
        assertEquals(List.of("p1", "p2", "p3"), roundTrip.projectIds());

        assertThrows(JsonFormatException.class, () -> FavouritesUpdateRequest.fromJson("[]"));
    }

    @Test
    @DisplayName("validates ProjectOnboarding round-trip")
    void projectOnboardingRoundTrip() throws Exception {
        var onboarding = new ProjectOnboarding(
                "onb-123",
                "/home/user/repo",
                ProjectOnboardingState.IDENTITY,
                List.of("maven", "java-cui"),
                List.of("core", "web"),
                List.of("sonar")
        );

        var json = onboarding.toJson();
        assertNotNull(json);

        var roundTrip = ProjectOnboarding.fromJson(json);
        assertAll(
                () -> assertEquals("onb-123", roundTrip.onboardingId()),
                () -> assertEquals("/home/user/repo", roundTrip.checkoutPath()),
                () -> assertEquals(ProjectOnboardingState.IDENTITY, roundTrip.state()),
                () -> assertEquals(List.of("maven", "java-cui"), roundTrip.detectedDomains()),
                () -> assertEquals(List.of("core", "web"), roundTrip.discoveredModules()),
                () -> assertEquals(List.of("sonar"), roundTrip.credentialsNeeded())
        );

        assertThrows(JsonFormatException.class, () -> ProjectOnboarding.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectOnboarding.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates ProjectReleaseRequest round-trip")
    void projectReleaseRequestRoundTrip() throws Exception {
        var request = new ProjectReleaseRequest("prj-1", true, false, "End of lifecycle");
        var json = request.toJson();
        assertNotNull(json);

        var roundTrip = ProjectReleaseRequest.fromJson(json);
        assertAll(
                () -> assertEquals("prj-1", roundTrip.projectId()),
                () -> assertTrue(roundTrip.confirmRelease()),
                () -> assertFalse(roundTrip.force()),
                () -> assertEquals("End of lifecycle", roundTrip.reason())
        );

        assertThrows(JsonFormatException.class, () -> ProjectReleaseRequest.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectReleaseRequest.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates ProjectReleaseResult round-trip")
    void projectReleaseResultRoundTrip() throws Exception {
        var now = Instant.parse("2026-10-17T12:00:00Z");
        var result = new ProjectReleaseResult(
                "prj-1", 3, 1, 2, now, "git.land-removal", ReleaseWaitStatus.COMPLETED
        );

        var json = result.toJson();
        assertNotNull(json);

        var roundTrip = ProjectReleaseResult.fromJson(json);
        assertAll(
                () -> assertEquals("prj-1", roundTrip.projectId()),
                () -> assertEquals(3, roundTrip.plansEndedCount()),
                () -> assertEquals(1, roundTrip.epicsEndedCount()),
                () -> assertEquals(2, roundTrip.entriesDeletedCount()),
                () -> assertEquals(now, roundTrip.releasedAt()),
                () -> assertEquals("git.land-removal", roundTrip.landingWaiting()),
                () -> assertEquals(ReleaseWaitStatus.COMPLETED, roundTrip.waitStatus())
        );

        assertThrows(JsonFormatException.class, () -> ProjectReleaseResult.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectReleaseResult.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates SSE events round-trip")
    void eventsRoundTrip() throws Exception {
        var now = Instant.parse("2026-10-17T12:00:00Z");

        var changed = new ProjectChangedEvent("p1", ProjectReadiness.READY, now);
        var changedRoundTrip = ProjectChangedEvent.fromJson(changed.toJson());
        assertEquals("p1", changedRoundTrip.projectId());
        assertEquals(ProjectReadiness.READY, changedRoundTrip.readiness());
        assertEquals(now, changedRoundTrip.timestamp());

        var enrolled = new ProjectEnrolledEvent("p2", "Project Two", now);
        var enrolledRoundTrip = ProjectEnrolledEvent.fromJson(enrolled.toJson());
        assertEquals("p2", enrolledRoundTrip.projectId());
        assertEquals("Project Two", enrolledRoundTrip.displayName());
        assertEquals(now, enrolledRoundTrip.timestamp());

        var released = new ProjectReleasedEvent("p3", 2, 1, now);
        var releasedRoundTrip = ProjectReleasedEvent.fromJson(released.toJson());
        assertEquals("p3", releasedRoundTrip.projectId());
        assertEquals(2, releasedRoundTrip.plansEndedCount());
        assertEquals(1, releasedRoundTrip.epicsEndedCount());
        assertEquals(now, releasedRoundTrip.timestamp());

        assertThrows(JsonFormatException.class, () -> ProjectChangedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectEnrolledEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectReleasedEvent.fromJson("[]"));
    }
}
