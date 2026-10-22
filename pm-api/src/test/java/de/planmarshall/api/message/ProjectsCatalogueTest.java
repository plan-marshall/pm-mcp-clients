/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectsCatalogueTest")
class ProjectsCatalogueTest {

    @Test
    @DisplayName("loads projects area in English and German with exact key parity")
    void shouldLoadProjectsAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "projects");
        var de = MessageCatalogue.getMessages("de", "projects");

        assertFalse(en.isEmpty(), "English projects messages must not be empty");
        assertFalse(de.isEmpty(), "German projects messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German projects message packs must match exactly");
    }

    @Test
    @DisplayName("contains all required specification keys")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "projects");

        String[] requiredKeys = {
                "project.explorer.title",
                "project.explorer.add",
                "project.explorer.favourites",
                "project.explorer.recent",
                "project.explorer.all",
                "project.explorer.by_name",
                "project.explorer.most_used",
                "project.explorer.filter",
                "project.explorer.empty",
                "project.explorer.last_active",
                "project.adding.title",
                "project.adding.step",
                "project.adding.continue",
                "project.adding.abandon",
                "project.adding.abandon_confirm",
                "project.state.ready",
                "project.state.setup_needs",
                "project.state.released",
                "project.action.add_epic",
                "project.action.add_plan",
                "project.action.running_work",
                "project.action.ended_work",
                "project.action.setup",
                "project.action.continue_setup",
                "project.action.favourite",
                "project.action.unfavourite",
                "project.action.enrol_again",
                "project.action.disabled_not_ready",
                "project.release.offer_release",
                "project.release.offer_unenrol",
                "project.release.title",
                "project.release.what_ends",
                "project.release.ends",
                "project.release.entries",
                "project.release.onboarding",
                "project.release.waiting",
                "project.release.wait_refused",
                "project.release.what_stays",
                "project.release.stays",
                "project.release.reason",
                "project.release.afterwards",
                "project.release.afterwards_release",
                "project.release.afterwards_unenrol",
                "project.release.keep",
                "project.release.action_release",
                "project.release.action_unenrol",
                "project.release.refused",
                "project.release.refused_hint",
                "project.release.back",
                "project.release.unenrolled",
                "project.step.checkout",
                "project.step.identity",
                "project.step.enrolment",
                "project.step.own",
                "project.step.team",
                "project.step.architecture",
                "project.step.landing",
                "project.step.roles",
                "project.step.descriptions",
                "project.step.commit",
                "project.step.skipped",
                "project.setup.step_out",
                "project.repair.retry",
                "project.checkout.title",
                "project.checkout.path",
                "project.consent.title",
                "project.consent.reads",
                "project.consent.runs",
                "project.consent.never",
                "project.run_consent.unknown",
                "project.identity.title",
                "project.identity.id",
                "project.identity.name",
                "project.identity.pilot",
                "project.enrolment.title",
                "project.origin.detected",
                "project.origin.machine",
                "project.origin.default",
                "project.origin.team",
                "project.enrolment.container",
                "project.enrolment.container_warning",
                "project.enrolment.build_env.login",
                "project.enrolment.build_env.captured",
                "project.enrolment.build_env.none",
                "project.enrolment.action",
                "project.team.title",
                "project.team.build_map",
                "project.team.build_map_confirm",
                "project.own.title",
                "project.own.action_keep",
                "project.own.action_save",
                "project.own.refused",
                "project.setup.first_run",
                "project.setup.operator_files",
                "project.landing.title",
                "project.landing.refused",
                "project.landing.merge_queue",
                "project.landing.recheck",
                "project.landing.skip",
                "project.architecture.title",
                "project.architecture.domains",
                "project.architecture.domain_because",
                "project.architecture.general_dev",
                "project.architecture.modules",
                "project.architecture.change",
                "project.qualify.consent.title",
                "project.qualify.consent.runs",
                "project.qualify.consent.never",
                "project.qualify.consent.if_not",
                "project.qualify.progress",
                "project.enrich.consent.title",
                "project.enrich.consent.runs",
                "project.enrich.consent.writes",
                "project.enrich.consent.if_not",
                "project.enrich.progress",
                "project.commit.title",
                "project.commit.files",
                "project.commit.action",
                "project.commit.leave",
                "project.commit.done",
                "project.commit.left",
                "project.ready.title",
                "project.ready.not_ready",
                "project.add.machine_not_ready",
                "project.add.continue_onboarding"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in projects.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("formats parameterized messages in English and German")
    void shouldFormatParameterizedMessages() {
        var enLastActive = MessageCatalogue.format("en", "project.explorer.last_active", Map.of("duration", "2 min"));
        assertEquals("2 min ago", enLastActive);

        var deLastActive = MessageCatalogue.format("de", "project.explorer.last_active", Map.of("duration", "2 Min."));
        assertEquals("vor 2 Min.", deLastActive);

        var enSetupNeeds = MessageCatalogue.format("en", "project.state.setup_needs", Map.of("need", "credential sonar"));
        assertEquals("setup needs: credential sonar", enSetupNeeds);

        var deSetupNeeds = MessageCatalogue.format("de", "project.state.setup_needs", Map.of("need", "Zugang sonar"));
        assertEquals("Einrichtung braucht: Zugang sonar", deSetupNeeds);

        var enStep = MessageCatalogue.format("en", "project.adding.step", Map.of("step", 2, "total", 10, "name", "Identity"));
        assertEquals("step 2 of 10: Identity", enStep);
    }
}
