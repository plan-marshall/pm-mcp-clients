/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.message.MessageCatalogue;

@DisplayName("Decision Records and Codecs (UC-PLN-2)")
@EnableGeneratorController
class DecisionRecordsTest {

    @Test
    @DisplayName("DecisionScope resolves wire values and handles case-insensitivity")
    void scopeEnum() {
        assertEquals(DecisionScope.PLAN, DecisionScope.fromValue("plan"));
        assertEquals(DecisionScope.EPIC, DecisionScope.fromValue("epic"));
        assertEquals(DecisionScope.PROJECT_SETUP, DecisionScope.fromValue("setup"));
        assertEquals(DecisionScope.PROJECT_SETUP, DecisionScope.fromValue("project_setup"));
        assertEquals(DecisionScope.MACHINE, DecisionScope.fromValue("machine"));
        assertThrows(IllegalArgumentException.class, () -> DecisionScope.fromValue("unknown"));
    }

    @Test
    @DisplayName("Reversibility resolves wire values")
    void reversibilityEnum() {
        assertEquals(Reversibility.REVERSIBLE, Reversibility.fromValue("reversible"));
        assertEquals(Reversibility.COSTLY, Reversibility.fromValue("costly"));
        assertEquals(Reversibility.IRREVERSIBLE, Reversibility.fromValue("irreversible"));
        assertThrows(IllegalArgumentException.class, () -> Reversibility.fromValue("unknown"));
    }

    @Test
    @DisplayName("AnswerType and InterpretationType resolve wire values")
    void typesEnum() {
        assertEquals(AnswerType.ONE_OPTION, AnswerType.fromValue("one_option"));
        assertEquals(AnswerType.SEVERAL_OPTIONS, AnswerType.fromValue("several_options"));
        assertEquals(AnswerType.VALUE, AnswerType.fromValue("value"));

        assertEquals(InterpretationType.OPTION, InterpretationType.fromValue("option"));
        assertEquals(InterpretationType.OPTION_WITH_CONDITION, InterpretationType.fromValue("option_with_condition"));
        assertEquals(InterpretationType.OWN_OPTION, InterpretationType.fromValue("own_option"));
        assertEquals(InterpretationType.DELEGATION, InterpretationType.fromValue("delegation"));
        assertEquals(InterpretationType.UNCLEAR, InterpretationType.fromValue("unclear"));
    }

    @Test
    @DisplayName("QuorumBand resolves wire values")
    void quorumBandEnum() {
        assertEquals(QuorumBand.UNANIMOUS, QuorumBand.fromValue("unanimous"));
        assertEquals(QuorumBand.CLEAR_MAJORITY, QuorumBand.fromValue("clear_majority"));
        assertEquals(QuorumBand.SPLIT, QuorumBand.fromValue("split"));
        assertEquals(QuorumBand.NO_CONSENSUS, QuorumBand.fromValue("no_consensus"));
    }

    @Test
    @DisplayName("DecisionId parses coordinates and single IDs")
    void decisionIdCoordinates() {
        var id1 = DecisionId.parse("fix-relay/Q-007");
        assertEquals("fix-relay", id1.scopeId());
        assertEquals("Q-007", id1.questionId());
        assertEquals("fix-relay/Q-007", id1.asCoordinate());

        var id2 = DecisionId.parse("WR-2");
        assertEquals("", id2.scopeId());
        assertEquals("WR-2", id2.questionId());
        assertEquals("WR-2", id2.asCoordinate());
    }

    @Test
    @DisplayName("QuorumVote and QuorumSupport serialize and round-trip")
    void quorumSupportRoundTrip() {
        var role = Generators.nonEmptyStrings().next();
        var model = Generators.nonEmptyStrings().next();
        var confidence = Generators.integers(0, 100).next();
        var reason = Generators.nonEmptyStrings().next();
        var dissentSummary = Generators.nonEmptyStrings().next();

        var vote1 = new QuorumVote(role, model, confidence, reason);
        var vote2 = new QuorumVote("implement", "claude-3.7-sonnet", 90, null);
        var support = new QuorumSupport(2, 3, 2, QuorumBand.CLEAR_MAJORITY, dissentSummary,
                List.of(vote1, vote2));

        var map = support.toMap();
        var roundTrip = QuorumSupport.fromMap(map);

        assertAll(
                () -> assertEquals(2, roundTrip.votesCount()),
                () -> assertEquals(3, roundTrip.totalVotes()),
                () -> assertEquals(2, roundTrip.modelsCount()),
                () -> assertEquals(QuorumBand.CLEAR_MAJORITY, roundTrip.band()),
                () -> assertEquals(dissentSummary, roundTrip.dissentSummary()),
                () -> assertEquals(2, roundTrip.votes().size()),
                () -> assertEquals(role, roundTrip.votes().get(0).role()),
                () -> assertEquals(confidence, roundTrip.votes().get(0).confidencePercent()),
                () -> assertEquals(reason, roundTrip.votes().get(0).reason())
        );
    }

    @Test
    @DisplayName("DecisionEnvelope round-trips through JSON")
    void decisionEnvelopeJsonRoundTrip() throws Exception {
        var vote1 = new QuorumVote("quorum-a", "model-1", 75, "Standard practice");
        var support = new QuorumSupport(2, 3, 2, QuorumBand.CLEAR_MAJORITY, "Rewrite hazard", List.of(vote1));
        var opt1 = new DecisionOption("1", "Merge main", "Preserves branch history", "Creates merge commit", null, null);
        var opt2 = new DecisionOption("2", "Rebase onto main", "Linear history", "Rewrites commits", "diff --git a/b", support);

        var part1 = new DecisionPart("1", "How to resolve conflict?", AnswerType.ONE_OPTION, List.of(opt1, opt2),
                "2", "Keeps history clean", "implement", null, true, true);

        var revision = Generators.integers(1, 100).next();
        var title = Generators.nonEmptyStrings().next();
        var subject = "plan " + Generators.nonEmptyStrings().next();
        var now = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var envelope = new DecisionEnvelope(
                "Q-007", revision, title, "Conflict", subject,
                DecisionScope.PLAN, "execute", "implement", "plan", Reversibility.REVERSIBLE,
                "The change conflicts with main", Map.of("file", "RelayServer.java", "conflict_line", 42),
                List.of(part1), now, false, null, null, null
        );

        var json = envelope.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Q-007"));
        assertTrue(json.contains("Rebase onto main"));

        var parsed = DecisionEnvelope.fromJson(json);
        assertAll(
                () -> assertEquals("Q-007", parsed.id()),
                () -> assertEquals(revision, parsed.revision()),
                () -> assertEquals(title, parsed.title()),
                () -> assertEquals(subject, parsed.subject()),
                () -> assertEquals(DecisionScope.PLAN, parsed.scope()),
                () -> assertEquals("execute", parsed.askingPhase()),
                () -> assertEquals("implement", parsed.askingRole()),
                () -> assertEquals("plan", parsed.blocks()),
                () -> assertEquals(Reversibility.REVERSIBLE, parsed.reversibility()),
                () -> assertEquals(now, parsed.deliveredAt()),
                () -> assertFalse(parsed.isWaiver()),
                () -> assertEquals(1, parsed.parts().size())
        );

        var parsedPart = parsed.parts().get(0);
        assertAll(
                () -> assertEquals("1", parsedPart.partId()),
                () -> assertEquals("2", parsedPart.recommendation()),
                () -> assertEquals("Keeps history clean", parsedPart.recommendationRationale()),
                () -> assertTrue(parsedPart.allowOwnWords()),
                () -> assertTrue(parsedPart.allowNote()),
                () -> assertEquals(2, parsedPart.options().size())
        );

        var parsedOpt2 = parsedPart.options().get(1);
        assertNotNull(parsedOpt2.quorumSupport());
        assertEquals(QuorumBand.CLEAR_MAJORITY, parsedOpt2.quorumSupport().band());
    }

    @Test
    @DisplayName("Waiver request envelope sets isWaiver and parses gate fields")
    void waiverRequestEnvelope() throws Exception {
        var waiverGate = "coverage:" + Generators.nonEmptyStrings().next();
        var waiverResult = Generators.nonEmptyStrings().next();
        var waiverReason = Generators.nonEmptyStrings().next();
        var envelope = new DecisionEnvelope(
                "WR-2", 1, "Waiver for coverage gate", null, "plan fix-relay",
                DecisionScope.PLAN, "execute", "implement", "plan", Reversibility.COSTLY,
                "Coverage below 80%", null, List.of(), Instant.now(),
                true, waiverGate, waiverResult, waiverReason
        );

        var json = envelope.toJson();
        var parsed = DecisionEnvelope.fromJson(json);
        assertTrue(parsed.isWaiver());
        assertEquals(waiverGate, parsed.waiverGate());
        assertEquals(waiverResult, parsed.waiverResult());
        assertEquals(waiverReason, parsed.waiverReason());
    }

    @Test
    @DisplayName("DecisionAnswerRequest serializes and parses properly")
    void answerRequestJson() throws Exception {
        var ownWords = Generators.nonEmptyStrings().next();
        var note = Generators.nonEmptyStrings().next();
        var device = Generators.nonEmptyStrings().next();
        var req = new DecisionAnswerRequest(Map.of("1", "2"), "2", ownWords, note, "answer", null, "cli",
                device);
        var json = req.toJson();
        var parsed = DecisionAnswerRequest.fromJson(json);

        assertAll(
                () -> assertEquals("2", parsed.option()),
                () -> assertEquals(ownWords, parsed.ownWords()),
                () -> assertEquals(note, parsed.note()),
                () -> assertEquals("cli", parsed.channel()),
                () -> assertEquals(device, parsed.device())
        );
    }

    @Test
    @DisplayName("DecisionInterpretation serializes and parses properly")
    void interpretationJson() throws Exception {
        var condition = Generators.nonEmptyStrings().next();
        var passages = List.of(Generators.nonEmptyStrings().next(), Generators.nonEmptyStrings().next());
        var interp = new DecisionInterpretation(
                InterpretationType.OPTION_WITH_CONDITION, "2", condition,
                "Rebase onto main, provided release/0.9 stays as it is", null,
                passages
        );

        var json = interp.toJson();
        var parsed = DecisionInterpretation.fromJson(json);

        assertAll(
                () -> assertEquals(InterpretationType.OPTION_WITH_CONDITION, parsed.interpretationType()),
                () -> assertEquals("2", parsed.mappedOptionKey()),
                () -> assertEquals(condition, parsed.condition()),
                () -> assertEquals(passages.size(), parsed.underlyingPassages().size())
        );
    }

    @Test
    @DisplayName("Decision SSE events round trip through JSON")
    void eventRoundTrips() throws Exception {
        var changeSummary = Generators.nonEmptyStrings().next();
        var answer = Generators.nonEmptyStrings().next();
        var env = new DecisionEnvelope("Q-001", 1, "Title", null, "plan pln-1", DecisionScope.PLAN,
                "refine", "planner", "plan", Reversibility.REVERSIBLE, null, null, List.of(), Instant.now(),
                false, null, null, null);

        var opened = new DecisionOpenedEvent(env);
        assertEquals(DecisionOpenedEvent.EVENT_NAME, "decision.opened");
        var openedParsed = DecisionOpenedEvent.fromJson(opened.toJson());
        assertEquals("Q-001", openedParsed.decision().id());

        var changed = new DecisionChangedEvent(env, changeSummary);
        assertEquals(DecisionChangedEvent.EVENT_NAME, "decision.changed");
        var changedParsed = DecisionChangedEvent.fromJson(changed.toJson());
        assertEquals(changeSummary, changedParsed.changeSummary());

        var answered = new DecisionAnsweredEvent("Q-001", answer, "cli", "mbp-oliver");
        assertEquals(DecisionAnsweredEvent.EVENT_NAME, "decision.answered");
        var answeredParsed = DecisionAnsweredEvent.fromJson(answered.toJson());
        assertEquals(answer, answeredParsed.answer());

        var closed = new DecisionClosedEvent("Q-001", "answered", null);
        assertEquals(DecisionClosedEvent.EVENT_NAME, "decision.closed");
        var closedParsed = DecisionClosedEvent.fromJson(closed.toJson());
        assertEquals("answered", closedParsed.outcome());
    }

    @Test
    @DisplayName("DecisionPart handles dependency and id fallbacks")
    void decisionPartFallbacks() {
        var dependency = Generators.nonEmptyStrings().next();
        var customId = Generators.nonEmptyStrings().next();
        var part = new DecisionPart("part-1", "q", AnswerType.ONE_OPTION, List.of(), "rec", "rationale", "role",
                dependency, true, true);
        var map = part.toMap();
        assertEquals(dependency, map.get("dependency"));

        var fromId = DecisionPart.fromMap(Map.of("id", customId, "question", "q"));
        assertEquals(customId, fromId.partId());

        var fromDefault = DecisionPart.fromMap(Map.of("question", "q"));
        assertEquals("1", fromDefault.partId());
    }

    @Test
    @DisplayName("DecisionAnswerRequest serializes reason and parses option fallback")
    void decisionAnswerRequestReasonAndOptionFallback() throws Exception {
        var req = new DecisionAnswerRequest(Map.of(), null, null, null, "waive", "build passed", "cli", null);
        var json = req.toJson();
        assertTrue(json.contains("\"reason\":\"build passed\""));

        var parsed = DecisionAnswerRequest.fromJson("{\"option\":\"opt1\"}");
        assertEquals("opt1", parsed.partAnswers().get("1"));
    }

    @Test
    @DisplayName("MessageCatalogue hasKey checks")
    void messageCatalogueHasKey() {
        assertTrue(MessageCatalogue.hasKey("decision.blocks.plan"));
        assertFalse(MessageCatalogue.hasKey("non.existent.key"));
        assertTrue(MessageCatalogue.hasKey(null, "decision.blocks.plan"));
        assertFalse(MessageCatalogue.hasKey("en", null));
    }
}
