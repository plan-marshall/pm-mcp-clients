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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;

@DisplayName("ProjectRecordsTest")
@EnableGeneratorController
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
        var projectId = Generators.nonEmptyStrings().next();
        var displayName = Generators.nonEmptyStrings().next();
        var needsOperatorCount = Generators.integers(0, 10).next();
        var activePlansCount = Generators.integers(0, 10).next();
        var activeEpicsCount = Generators.integers(0, 10).next();
        var now = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var project = new EnrolledProject(
                projectId,
                displayName,
                "https://github.com/plan-marshall/pm-mcp-clients",
                ProjectReadiness.READY,
                null,
                needsOperatorCount,
                activePlansCount,
                activeEpicsCount,
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
        var minimalId = Generators.nonEmptyStrings().next();
        var minimal = EnrolledProject.fromMap(Map.of("id", minimalId));
        assertEquals(minimalId, minimal.projectId());
        assertEquals(minimalId, minimal.displayName());
        assertEquals(ProjectReadiness.READY, minimal.readiness());

        assertThrows(JsonFormatException.class, () -> EnrolledProject.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> EnrolledProject.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates BeingAddedProject round-trip")
    void beingAddedProjectRoundTrip() throws Exception {
        var checkoutPath = "/git/" + Generators.nonEmptyStrings().next();
        var currentStep = Generators.nonEmptyStrings().next();
        var stepNumber = Generators.integers(1, 5).next();
        var totalSteps = stepNumber + Generators.integers(1, 5).next();
        var entryId = Generators.nonEmptyStrings().next();
        var beingAdded = new BeingAddedProject(
                checkoutPath,
                currentStep,
                stepNumber,
                totalSteps,
                "/api/v1/entries/" + entryId + "/continue",
                "/api/v1/entries/" + entryId + "/abandon"
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
        var favouriteId = Generators.nonEmptyStrings().next();
        var recentId = Generators.nonEmptyStrings().next();
        var favouriteName = Generators.nonEmptyStrings().next();
        var recentName = Generators.nonEmptyStrings().next();
        var checkoutPath = "/tmp/" + Generators.nonEmptyStrings().next();
        var fav = new EnrolledProject(
                favouriteId, favouriteName, null, ProjectReadiness.READY, null, 0, 1, 0, null, true, false
        );
        var recent = new EnrolledProject(
                recentId, recentName, null, ProjectReadiness.SETUP_NEEDS, "sonar credential", 1, 0, 0, null,
                false, false
        );
        var beingAdded = new BeingAddedProject(
                checkoutPath, "Checkout", 1, 10, null, null
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
                () -> assertEquals(favouriteId, roundTrip.favourites().getFirst().projectId()),
                () -> assertEquals(1, roundTrip.recent().size()),
                () -> assertEquals(recentId, roundTrip.recent().getFirst().projectId()),
                () -> assertEquals(2, roundTrip.allProjects().size()),
                () -> assertEquals(1, roundTrip.beingAdded().size()),
                () -> assertEquals(checkoutPath, roundTrip.beingAdded().getFirst().checkoutPath())
        );

        assertThrows(JsonFormatException.class, () -> ProjectExplorer.fromJson("[]"));
    }

    @Test
    @DisplayName("validates FavouritesUpdateRequest round-trip")
    void favouritesUpdateRequestRoundTrip() throws Exception {
        var projectIds = List.of(Generators.nonEmptyStrings().next(), Generators.nonEmptyStrings().next(),
                Generators.nonEmptyStrings().next());
        var request = new FavouritesUpdateRequest(projectIds);
        var json = request.toJson();
        assertNotNull(json);

        var roundTrip = FavouritesUpdateRequest.fromJson(json);
        assertEquals(projectIds, roundTrip.projectIds());

        assertThrows(JsonFormatException.class, () -> FavouritesUpdateRequest.fromJson("[]"));
    }

    @Test
    @DisplayName("validates ProjectOnboarding round-trip")
    void projectOnboardingRoundTrip() throws Exception {
        var onboardingId = Generators.nonEmptyStrings().next();
        var checkoutPath = "/home/" + Generators.nonEmptyStrings().next();
        var detectedDomains = List.of(Generators.nonEmptyStrings().next(), Generators.nonEmptyStrings().next());
        var discoveredModules = List.of(Generators.nonEmptyStrings().next(), Generators.nonEmptyStrings().next());
        var credentialsNeeded = List.of(Generators.nonEmptyStrings().next());
        var onboarding = new ProjectOnboarding(
                onboardingId,
                checkoutPath,
                ProjectOnboardingState.IDENTITY,
                detectedDomains,
                discoveredModules,
                credentialsNeeded
        );

        var json = onboarding.toJson();
        assertNotNull(json);

        var roundTrip = ProjectOnboarding.fromJson(json);
        assertAll(
                () -> assertEquals(onboardingId, roundTrip.onboardingId()),
                () -> assertEquals(checkoutPath, roundTrip.checkoutPath()),
                () -> assertEquals(ProjectOnboardingState.IDENTITY, roundTrip.state()),
                () -> assertEquals(detectedDomains, roundTrip.detectedDomains()),
                () -> assertEquals(discoveredModules, roundTrip.discoveredModules()),
                () -> assertEquals(credentialsNeeded, roundTrip.credentialsNeeded())
        );

        assertThrows(JsonFormatException.class, () -> ProjectOnboarding.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectOnboarding.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates ProjectReleaseRequest round-trip")
    void projectReleaseRequestRoundTrip() throws Exception {
        var projectId = Generators.nonEmptyStrings().next();
        var reason = Generators.nonEmptyStrings().next();
        var request = new ProjectReleaseRequest(projectId, true, false, reason);
        var json = request.toJson();
        assertNotNull(json);

        var roundTrip = ProjectReleaseRequest.fromJson(json);
        assertAll(
                () -> assertEquals(projectId, roundTrip.projectId()),
                () -> assertTrue(roundTrip.confirmRelease()),
                () -> assertFalse(roundTrip.force()),
                () -> assertEquals(reason, roundTrip.reason())
        );

        assertThrows(JsonFormatException.class, () -> ProjectReleaseRequest.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectReleaseRequest.fromMap(Map.of()));
    }

    @Test
    @DisplayName("validates ProjectReleaseResult round-trip")
    void projectReleaseResultRoundTrip() throws Exception {
        var projectId = Generators.nonEmptyStrings().next();
        var plansEndedCount = Generators.integers(0, 10).next();
        var epicsEndedCount = Generators.integers(0, 10).next();
        var entriesDeletedCount = Generators.integers(0, 10).next();
        var now = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var result = new ProjectReleaseResult(
                projectId, plansEndedCount, epicsEndedCount, entriesDeletedCount, now,
                "git.land-removal", ReleaseWaitStatus.COMPLETED
        );

        var json = result.toJson();
        assertNotNull(json);

        var roundTrip = ProjectReleaseResult.fromJson(json);
        assertAll(
                () -> assertEquals(projectId, roundTrip.projectId()),
                () -> assertEquals(plansEndedCount, roundTrip.plansEndedCount()),
                () -> assertEquals(epicsEndedCount, roundTrip.epicsEndedCount()),
                () -> assertEquals(entriesDeletedCount, roundTrip.entriesDeletedCount()),
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
        var changedId = Generators.nonEmptyStrings().next();
        var enrolledId = Generators.nonEmptyStrings().next();
        var releasedId = Generators.nonEmptyStrings().next();
        var displayName = Generators.nonEmptyStrings().next();
        var plansEndedCount = Generators.integers(0, 10).next();
        var epicsEndedCount = Generators.integers(0, 10).next();
        var now = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());

        var changed = new ProjectChangedEvent(changedId, ProjectReadiness.READY, now);
        var changedRoundTrip = ProjectChangedEvent.fromJson(changed.toJson());
        assertEquals(changedId, changedRoundTrip.projectId());
        assertEquals(ProjectReadiness.READY, changedRoundTrip.readiness());
        assertEquals(now, changedRoundTrip.timestamp());

        var enrolled = new ProjectEnrolledEvent(enrolledId, displayName, now);
        var enrolledRoundTrip = ProjectEnrolledEvent.fromJson(enrolled.toJson());
        assertEquals(enrolledId, enrolledRoundTrip.projectId());
        assertEquals(displayName, enrolledRoundTrip.displayName());
        assertEquals(now, enrolledRoundTrip.timestamp());

        var released = new ProjectReleasedEvent(releasedId, plansEndedCount, epicsEndedCount, now);
        var releasedRoundTrip = ProjectReleasedEvent.fromJson(released.toJson());
        assertEquals(releasedId, releasedRoundTrip.projectId());
        assertEquals(plansEndedCount, releasedRoundTrip.plansEndedCount());
        assertEquals(epicsEndedCount, releasedRoundTrip.epicsEndedCount());
        assertEquals(now, releasedRoundTrip.timestamp());

        assertThrows(JsonFormatException.class, () -> ProjectChangedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectEnrolledEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectReleasedEvent.fromJson("[]"));
    }
}
