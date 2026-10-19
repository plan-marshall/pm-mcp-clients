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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.source.git.ChangedFile;
import de.planmarshall.api.start.AdoptableEpic;
import de.planmarshall.api.start.AdoptionFailure;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.RecipeParameter;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.ScreeningSpan;
import de.planmarshall.api.start.ScreeningVerdict;
import de.planmarshall.api.start.SourceFailure;
import de.planmarshall.api.start.SourceFailureCause;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartReadyResource;
import de.planmarshall.api.start.StartRefusal;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.start.UnscreenedDisposition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The renderer of the starting dialogue on its own: every source state and cause, the flagged
 * spans, the ready step of a plan and an epic, recipes and their parameters, adoption, and the
 * refusal of the setup gate. No terminal is opened; the prompt writes into a buffer.
 */
@DisplayName("starting renderer")
class StartRendererTest {

    @Test
    @DisplayName("renders every source state, cause, span, and the untrusted mark")
    void rendersSources() {
        var failed = source("s-1", SourceKind.ISSUE, "https://example.test/1", SourceState.FAILED,
                            SourceTrust.UNTRUSTED, null, SourceFailure.of(SourceFailureCause.NOT_FOUND, "1"),
                            List.of(), null, null, List.of());
        var rate = source("s-2", SourceKind.PULL_REQUEST, "https://example.test/pr/2", SourceState.FAILED,
                            SourceTrust.UNTRUSTED, null,
                            SourceFailure.rateLimited(Instant.ofEpochSecond(1_792_507_200L), "GitHub"),
                            List.of(), null, null, List.of());
        var flagged = source("s-3", SourceKind.ISSUE, "https://example.test/3", SourceState.FLAGGED,
                            SourceTrust.UNTRUSTED, ScreeningVerdict.FLAGGED, null,
                            List.of(new ScreeningSpan(0, 4, "ignore", "an instruction to a model")),
                            null, null, List.of());
        var unscreened = source("s-4", SourceKind.ISSUE, "https://example.test/4", SourceState.UNSCREENED,
                            SourceTrust.UNTRUSTED, ScreeningVerdict.UNSCREENED,
                            SourceFailure.of(SourceFailureCause.NO_SCREENER), List.of(),
                            UnscreenedDisposition.ACCEPT_UNTRUSTED, "read as data", List.of());
        var pullRequest = source("s-5", SourceKind.PULL_REQUEST, "https://example.test/pr/5", SourceState.STORED,
                            SourceTrust.UNTRUSTED, ScreeningVerdict.CLEAN, null, List.of(), null, null,
                            List.of(new ChangedFile("a/A.java", 1, 0, true, false)));

        var rendered = render(sourcesState(entry(EntryKind.PLAN, StartState.SOURCES,
                            List.of(failed, rate, flagged, unscreened, pullRequest),
                            List.of(SourceKindOffer.available(SourceKind.DESCRIPTION),
                                                SourceKindOffer.refused(SourceKind.LOCAL_CHANGES,
                                                                    SourceKindOffer.REASON_LOCAL_CHANGES_LANDING,
                                                                    Map.of("landing", "landing.files"))))));

        assertAll(
                            () -> assertTrue(rendered.contains("[1/2] Sources"), rendered),
                            () -> assertTrue(rendered.contains("not found"), rendered),
                            () -> assertTrue(rendered.contains("rate limited until"), rendered),
                            () -> assertTrue(rendered.contains("resets_at 1792507200"), rendered),
                            () -> assertTrue(rendered.contains("This text may contain instructions to a model"),
                                                rendered),
                            () -> assertTrue(rendered.contains("ignore"), rendered),
                            () -> assertTrue(rendered.contains("untrusted"), rendered),
                            () -> assertTrue(rendered.contains("changed file"), rendered),
                            () -> assertTrue(rendered.contains("landing that commits"), rendered));
    }

    @Test
    @DisplayName("renders the ready step of a plan and of an epic with the values in force")
    void rendersReady() {
        var plan = entry(EntryKind.PLAN, StartState.READY, List.of(), List.of());
        var planReady = new StartReadyResource("A plan", "a-plan", true, true,
                            new RecipeRef("audit", "audit", "audit", "project", true,
                                                List.of(new RecipeParameter("modules", "all", List.of("all"),
                                                                    true, true, null))),
                            List.of(new RecipeParameter("modules", "all", List.of("all"), true, true, null)),
                            "landing.pull-request", false, null, null, null, null);
        var planState = StartStateResource.of(plan, planReady, null, null, null, List.of());

        var epic = entry(EntryKind.EPIC, StartState.READY, List.of(), List.of());
        var epicReady = StartReadyResource.ofEpic("Relay hardening", "relay-hardening", true,
                            ConfigOrigin.PROJECT, 2, 3);
        var epicState = StartStateResource.of(epic, epicReady, null, null, null, List.of());

        var planRendered = render(planState);
        var epicRendered = render(epicState);
        assertAll(
                            () -> assertTrue(planRendered.contains("[2/2] Ready"), planRendered),
                            () -> assertTrue(planRendered.contains("Parameters"), planRendered),
                            () -> assertTrue(planRendered.contains("a-plan"), planRendered),
                            () -> assertTrue(epicRendered.contains("[2/3] Ready"), epicRendered),
                            () -> assertTrue(epicRendered.contains("from the project"), epicRendered));
    }

    @Test
    @DisplayName("renders recipes, their parameters, adoption, failures, and refusals")
    void rendersRecipesAdoptionAndRefusals() {
        var recipes = List.of(
                            new RecipeRef("code-review", "code-review", "a review", "domain", false,
                                                List.of(new RecipeParameter("depth", "standard",
                                                                    List.of("quick", "standard", "deep"),
                                                                    true, true, null))),
                            new RecipeRef("no-modules", "no-modules", "a blocked recipe", "project", false,
                                                List.of(new RecipeParameter("modules", null, List.of(),
                                                                    false, false, "none the recipe can work on"))));
        var renderedRecipes = renderRecipes(recipes);

        var adoptable = List.of(new AdoptableEpic("relay-hardening", "Relay hardening"));
        var adoption = entry(EntryKind.EPIC, StartState.EPIC_ADOPT, List.of(), List.of(), adoptable, null);
        var adoptionRendered = render(StartStateResource.of(adoption, null, null, null, null, List.of()));

        var failure = AdoptionFailure.unreadable("relay-hardening", ".marshall/epic.json", "permission denied");
        var adoptionFailure = entry(EntryKind.EPIC, StartState.EPIC_ADOPT_FAILED, List.of(), List.of());
        var failureRendered = render(StartStateResource.of(adoptionFailure, null, null, null, failure,
                            List.of()));

        var refused = StartStateResource.of(entry(EntryKind.PLAN, StartState.REFUSED, List.of(), List.of()),
                            null, null, StartRefusal.projectNotReady("pm-mcp-clients", "credential sonar"),
                            null, List.of());
        var refusedRendered = render(refused);
        var machineRefused = StartStateResource.of(entry(EntryKind.PLAN, StartState.REFUSED, List.of(),
                            List.of()), null, null, StartRefusal.machineNotOnboarded(null, "providers"),
                            null, List.of());
        var machineRendered = render(machineRefused);

        assertAll(
                            () -> assertTrue(renderedRecipes.contains("domain"), renderedRecipes),
                            () -> assertTrue(renderedRecipes.contains("none the recipe can work on"),
                                                renderedRecipes),
                            () -> assertTrue(renderedRecipes.contains("proposed"), renderedRecipes),
                            () -> assertTrue(adoptionRendered.contains("adopt relay-hardening"),
                                                adoptionRendered),
                            () -> assertTrue(failureRendered.contains("cannot be adopted"), failureRendered),
                            () -> assertTrue(refusedRendered.contains("is not ready: credential sonar"),
                                                refusedRendered),
                            () -> assertTrue(machineRendered.contains("not fully onboarded"), machineRendered));
    }

    private static final OperatorContext CONTEXT =
                        new OperatorContext(Map.of("LANG", "en"), Path.of("."), true);

    private static String renderRecipes(List<RecipeRef> recipes) {
        var buffer = new StringWriter();
        var prompt = new StartPrompt(CONTEXT, new PrintWriter(buffer), "en", true);
        var renderer = new StartRenderer(prompt);
        renderer.recipes(recipes);
        renderer.parameters(recipes.getFirst(), recipes.getFirst().parameters());
        renderer.parameters(recipes.get(1), recipes.get(1).parameters());
        return buffer.toString();
    }

    private static String render(StartStateResource state) {
        var buffer = new StringWriter();
        var prompt = new StartPrompt(CONTEXT, new PrintWriter(buffer), "en", true);
        var renderer = new StartRenderer(prompt);
        switch (state.state()) {
            case SOURCES -> renderer.sources(state, 1, 2, "pm-mcp-clients", false);
            case READY -> renderer.ready(state, 2, state.entry() != null && state.entry().kind().epic() ? 3 : 2);
            case EPIC_ADOPT -> renderer.adoption(state.entry());
            case EPIC_ADOPT_FAILED -> renderer.adoptionFailure(state.adoptionFailure());
            case REFUSED -> renderer.refused(state.refusal());
            default -> {
                // the test renders only these states
            }
        }
        return buffer.toString();
    }

    private static StartStateResource sourcesState(EntryResource entry) {
        return StartStateResource.of(entry, null, null, null, null, List.of());
    }

    private static EntryResource entry(EntryKind kind, StartState state, List<SourceResource> sources,
                        List<SourceKindOffer> offers) {
        return entry(kind, state, sources, offers, List.of(), null);
    }

    private static EntryResource entry(EntryKind kind, StartState state, List<SourceResource> sources,
                        List<SourceKindOffer> offers, List<AdoptableEpic> adoptable,
                        AdoptionFailure failure) {
        return new EntryResource("ent-1", kind, "pm-mcp-clients", state, "A title", "a-title", sources,
                            offers, true, true, null, null, adoptable, failure);
    }

    private static SourceResource source(String id, SourceKind kind, String reference, SourceState state,
                        SourceTrust trust, ScreeningVerdict verdict, SourceFailure failure,
                        List<ScreeningSpan> spans, UnscreenedDisposition disposition, String reason,
                        List<ChangedFile> changedFiles) {
        return new SourceResource(id, kind, reference, null, state, trust, verdict, failure, spans,
                            disposition, reason, changedFiles, "sha256:x");
    }
}
