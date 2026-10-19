/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.intent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The codec and validation contract of the intent records: every closed list resolves every member
 * and refuses an unknown value, every record round-trips through its map, JSON, and streaming
 * codecs, and a malformed representation fails closed with a {@link JsonFormatException}.
 */
@DisplayName("Intent contract")
class IntentContractTest {

    private static final JsonFactory FACTORY = new JsonFactory();

    @Nested
    @DisplayName("Closed catalogues")
    class Catalogues {

        @Test
        @DisplayName("every member of every closed list resolves and an unknown value is refused")
        void everyMemberResolves() {
            assertAll(
                    () -> eachMember(CriterionStatus.wireValues(), CriterionStatus::fromWire,
                            CriterionStatus::fromWireOrNull),
                    () -> eachMember(IntentDecider.wireValues(), IntentDecider::fromWire,
                            IntentDecider::fromWireOrNull),
                    () -> eachMember(BuilderQuestion.Sort.wireValues(), BuilderQuestion.Sort::fromWire,
                            BuilderQuestion.Sort::fromWireOrNull),
                    () -> eachMember(BuilderQuestion.State.wireValues(), BuilderQuestion.State::fromWire,
                            BuilderQuestion.State::fromWireOrNull),
                    () -> eachMember(BuilderCheckResource.Resolution.wireValues(),
                            BuilderCheckResource.Resolution::fromWire,
                            BuilderCheckResource.Resolution::fromWireOrNull),
                    () -> eachMember(DeliverableRecord.FileIntent.wireValues(),
                            DeliverableRecord.FileIntent::fromWire,
                            DeliverableRecord.FileIntent::fromWireOrNull),
                    () -> eachMember(DoneWhenCheck.Verdict.wireValues(), DoneWhenCheck.Verdict::fromWire,
                            DoneWhenCheck.Verdict::fromWireOrNull));
        }

        @Test
        @DisplayName("the derived properties of the closed lists hold")
        void derivedProperties() {
            assertAll(
                    () -> assertTrue(CriterionStatus.NOT_MET.requiresReason()),
                    () -> assertTrue(CriterionStatus.NOT_VERIFIED.requiresReason()),
                    () -> assertFalse(CriterionStatus.MET.requiresReason()),
                    () -> assertTrue(IntentDecider.OPERATOR.operatorFacing()),
                    () -> assertTrue(IntentDecider.QUORUM.operatorFacing()),
                    () -> assertFalse(IntentDecider.MODEL.operatorFacing()),
                    () -> assertTrue(BuilderQuestion.Sort.OPERATOR.needsOperator()),
                    () -> assertFalse(BuilderQuestion.Sort.MINOR.needsOperator()),
                    () -> assertTrue(DeliverableRecord.FileIntent.WRITE_NEW.writes()),
                    () -> assertFalse(DeliverableRecord.FileIntent.READ.writes()),
                    () -> assertFalse(DoneWhenCheck.Verdict.PENDING.decided()),
                    () -> assertTrue(DoneWhenCheck.Verdict.MET.decided()));
        }
    }

    @Nested
    @DisplayName("Records")
    class Records {

        @Test
        @DisplayName("every record round-trips through its map, JSON, and streaming codecs")
        void everyRecord() {
            assertAll(
                    () -> verify(planIntent(), PlanIntentResource::toJson, PlanIntentResource::toMap,
                            PlanIntentResource::fromJson, PlanIntentResource::fromMap,
                            PlanIntentResource::readFrom, PlanIntentResource::writeTo),
                    () -> verify(criterion(), IntentCriterion::toJson, IntentCriterion::toMap,
                            IntentCriterion::fromJson, IntentCriterion::fromMap,
                            IntentCriterion::readFrom, IntentCriterion::writeTo),
                    () -> verify(builderCheck(), BuilderCheckResource::toJson, BuilderCheckResource::toMap,
                            BuilderCheckResource::fromJson, BuilderCheckResource::fromMap,
                            BuilderCheckResource::readFrom, BuilderCheckResource::writeTo),
                    () -> verify(question(), BuilderQuestion::toJson, BuilderQuestion::toMap,
                            BuilderQuestion::fromJson, BuilderQuestion::fromMap,
                            BuilderQuestion::readFrom, BuilderQuestion::writeTo),
                    () -> verify(outline(), OutlineResource::toJson, OutlineResource::toMap,
                            OutlineResource::fromJson, OutlineResource::fromMap,
                            OutlineResource::readFrom, OutlineResource::writeTo),
                    () -> verify(deliverable(), DeliverableRecord::toJson, DeliverableRecord::toMap,
                            DeliverableRecord::fromJson, DeliverableRecord::fromMap,
                            DeliverableRecord::readFrom, DeliverableRecord::writeTo),
                    () -> verify(dependency(), DependencyJustification::toJson, DependencyJustification::toMap,
                            DependencyJustification::fromJson, DependencyJustification::fromMap,
                            DependencyJustification::readFrom, DependencyJustification::writeTo),
                    () -> verify(epicIntent(), EpicIntentResource::toJson, EpicIntentResource::toMap,
                            EpicIntentResource::fromJson, EpicIntentResource::fromMap,
                            EpicIntentResource::readFrom, EpicIntentResource::writeTo),
                    () -> verify(doneWhen(), DoneWhenCheck::toJson, DoneWhenCheck::toMap,
                            DoneWhenCheck::fromJson, DoneWhenCheck::fromMap,
                            DoneWhenCheck::readFrom, DoneWhenCheck::writeTo),
                    () -> verify(distribution(), OverSizeDistributionResource::toJson,
                            OverSizeDistributionResource::toMap, OverSizeDistributionResource::fromJson,
                            OverSizeDistributionResource::fromMap, OverSizeDistributionResource::readFrom,
                            OverSizeDistributionResource::writeTo),
                    () -> verify(bucket(), TokenBucket::toJson, TokenBucket::toMap, TokenBucket::fromJson,
                            TokenBucket::fromMap, TokenBucket::readFrom, TokenBucket::writeTo));
        }

        @Test
        @DisplayName("the plan intent keeps its criteria, decisions, and builder check through the round trip")
        void planIntentDetails() throws Exception {
            var value = planIntent();
            var parsed = PlanIntentResource.fromJson(value.toJson());

            assertEquals(4, parsed.criteria().size());
            assertEquals("AC-002", parsed.criterion("AC-002").id());
            assertEquals(CriterionStatus.NOT_VERIFIED, parsed.criterion("AC-002").status());
            assertEquals("The harness cannot observe the timeout.", parsed.criterion("AC-002").statusReason());
            assertEquals(2, parsed.operatorDecisions().size());
            assertEquals(1, parsed.modelCalls().size());
            assertEquals(1, parsed.openBuilderQuestions().size());
            assertEquals(1, parsed.notVerifiedCount());
            assertTrue(parsed.hasNotVerified());
            assertNull(parsed.criterion("AC-999"));
        }

        @Test
        @DisplayName("the outline keeps its deliverables, files, and missing key link")
        void outlineDetails() throws Exception {
            var parsed = OutlineResource.fromJson(outline().toJson());

            assertEquals(2, parsed.deliverables().size());
            assertNotNull(parsed.deliverable(1));
            assertNull(parsed.deliverable(9));
            assertEquals(1, parsed.satisfying("AC-001").size());
            assertEquals(3, parsed.affectedFiles().size());
            assertTrue(parsed.hasMissingKeyLink());
            assertEquals(2, parsed.deliverable(1).writtenFiles().size());
        }

        @Test
        @DisplayName("the epic intent keeps its done-when evidence and its plans' criteria")
        void epicIntentDetails() throws Exception {
            var parsed = EpicIntentResource.fromJson(epicIntent().toJson());

            assertEquals(3, parsed.doneWhen().size());
            assertEquals(2, parsed.decidedCount());
            assertFalse(parsed.allMet());
            assertEquals(1, parsed.pendingChecks().size());
            assertEquals(2, parsed.plans().size());
            assertEquals("relay-hardening", parsed.epicId());
            assertNotNull(parsed.plan("fix-relay"));
            assertNull(parsed.plan("no-such-plan"));
        }

        @Test
        @DisplayName("the over-size distribution derives its totals and flagged share")
        void distributionDetails() throws Exception {
            var parsed = OverSizeDistributionResource.fromJson(distribution().toJson());

            assertEquals(20, parsed.totalTasks());
            assertEquals(7, parsed.flaggedTasks());
            assertEquals(0.35, parsed.flaggedShare(), 0.0001);
            assertEquals(2, parsed.flaggedBuckets().size());
            assertTrue(parsed.flagsMore());
            assertEquals(4, parsed.buckets().size());
        }

        @Test
        @DisplayName("the criteria a plan has not evaluated carry no verdict")
        void unevaluatedCriterion() throws Exception {
            var value = new IntentCriterion("AC-004", "The system shall keep the old endpoint.",
                    List.of(1), null, null);
            var parsed = IntentCriterion.fromJson(value.toJson());

            assertFalse(parsed.evaluated());
            assertNull(parsed.status());
            assertNull(parsed.statusReason());
        }
    }

    @Nested
    @DisplayName("Fail-closed parsing")
    class FailClosed {

        @Test
        @DisplayName("an unknown field is refused")
        void unknownField() throws Exception {
            var map = JsonTree.asObject(JsonTree.parse(JsonTree.write(planIntent().toMap())));
            map.put("surprise", "value");
            assertThrows(JsonFormatException.class, () -> PlanIntentResource.fromMap(map));
        }

        @Test
        @DisplayName("an unknown closed value is refused")
        void unknownClosedValue() {
            assertThrows(JsonFormatException.class, () -> IntentCriterion.fromJson(
                    "{\"id\":\"AC-1\",\"text\":\"x\",\"satisfies\":[],\"status\":\"maybe\"}"));
            assertThrows(JsonFormatException.class, () -> BuilderQuestion.fromJson(
                    "{\"id\":\"BQ-1\",\"text\":\"x\",\"sort\":\"nobody\",\"recommended\":false,\"state\":\"open\"}"));
            assertThrows(JsonFormatException.class, () -> TokenBucket.fromJson(
                    "{\"label\":\"x\",\"from_multiple\":1.0,\"to_multiple\":2.0,\"task_count\":1,"
                            + "\"flagged_tasks\":0,\"extra\":1}"));
        }

        @Test
        @DisplayName("a criterion that is not met without its reason is refused")
        void missingReason() {
            assertThrows(IllegalArgumentException.class,
                    () -> new IntentCriterion("AC-1", "x", List.of(), CriterionStatus.NOT_VERIFIED, null));
            assertThrows(JsonFormatException.class, () -> IntentCriterion.fromJson(
                    "{\"id\":\"AC-1\",\"text\":\"x\",\"satisfies\":[],\"status\":\"not_met\"}"));
        }

        @Test
        @DisplayName("a plan intent without a goal, criteria, or unique ids is refused")
        void invalidPlanIntent() {
            var repeated = List.of(criterion(), criterion());
            var goals = List.of("none");
            List<PlanIntentResource.Decision> noDecisions = List.of();
            List<String> noQuestions = List.of();
            List<PlanIntentResource.DeferredItem> noDeferred = List.of();
            assertThrows(JsonFormatException.class, () -> PlanIntentResource.fromJson(
                    "{\"plan_id\":\"p\",\"revision\":1,\"goal\":\"\",\"criteria\":[]}"));
            assertThrows(IllegalArgumentException.class, () -> new PlanIntentResource("p", 1, "goal",
                    repeated, goals, noDecisions, noQuestions, noDeferred, null));
        }

        @Test
        @DisplayName("an answered builder question without an answer is refused")
        void invalidBuilderQuestion() {
            assertThrows(IllegalArgumentException.class, () -> new BuilderQuestion("BQ-1", "x",
                    BuilderQuestion.Sort.OPERATOR, false, BuilderQuestion.State.ANSWERED, null));
            assertThrows(JsonFormatException.class, () -> BuilderQuestion.fromJson(
                    "{\"id\":\"BQ-1\",\"text\":\"x\",\"sort\":\"operator\",\"recommended\":false,"
                            + "\"state\":\"answered\"}"));
        }

        @Test
        @DisplayName("an outline with a duplicate deliverable or an orphan dependency is refused")
        void invalidOutline() {
            var first = deliverable();
            var second = deliverable();
            var duplicates = List.of(first, second);
            var one = List.of(first);
            var orphan = new DependencyJustification("x", null, "needed", 7);
            var orphans = List.of(orphan);
            List<DependencyJustification> noDependencies = List.of();
            List<String> noTexts = List.of();
            assertThrows(IllegalArgumentException.class,
                    () -> new OutlineResource("p", 1, "summary", duplicates, noDependencies, noTexts,
                            noTexts, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new OutlineResource("p", 1, "summary", one, orphans, noTexts, noTexts, null));
        }

        @Test
        @DisplayName("a decided done-when check without evidence is refused")
        void invalidDoneWhen() {
            assertThrows(IllegalArgumentException.class, () -> new DoneWhenCheck("x",
                    DoneWhenCheck.Verdict.MET, List.of()));
            assertThrows(JsonFormatException.class, () -> DoneWhenCheck.fromJson(
                    "{\"text\":\"x\",\"verdict\":\"met\"}"));
        }

        @Test
        @DisplayName("an epic intent with too few checks or no non-goal is refused")
        void invalidEpicIntent() {
            var check = doneWhen();
            var one = List.of(check);
            var three = List.of(check, check, check);
            var goals = List.of("none");
            List<String> noTexts = List.of();
            List<EpicIntentResource.PlanDoneWhen> noPlans = List.of();
            assertThrows(IllegalArgumentException.class,
                    () -> new EpicIntentResource("e", "destination", one, goals, noTexts, noPlans));
            assertThrows(IllegalArgumentException.class,
                    () -> new EpicIntentResource("e", "destination", three, noTexts, noTexts, noPlans));
        }

        @Test
        @DisplayName("an over-size distribution with a bad bucket or a bad share is refused")
        void invalidDistribution() {
            var overlapping = List.of(new TokenBucket("a", 1.0, 3.0, 1, 0),
                    new TokenBucket("b", 2.0, 4.0, 1, 0));
            var valid = List.of(bucket());
            assertThrows(IllegalArgumentException.class,
                    () -> new TokenBucket("x", 1.0, 2.0, 3, 4));
            assertThrows(IllegalArgumentException.class,
                    () -> new TokenBucket("x", 2.0, 2.0, 1, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> new OverSizeDistributionResource("p", "r", 3.0, 2.0, overlapping));
            assertThrows(IllegalArgumentException.class,
                    () -> new OverSizeDistributionResource("p", "r", 3.0, 0.0, valid));
        }

        @Test
        @DisplayName("a non-object JSON value is refused")
        void nonObject() {
            assertThrows(JsonFormatException.class, () -> PlanIntentResource.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> OutlineResource.fromJson("42"));
        }

        @Test
        @DisplayName("a non-string element in a string array is refused")
        void nonStringElement() {
            assertThrows(JsonFormatException.class, () -> OutlineResource.fromJson(
                    "{\"plan_id\":\"p\",\"revision\":1,\"summary\":\"summary\",\"deliverables\":["
                            + "{\"number\":1,\"title\":\"t\",\"intent_gloss\":\"g\",\"depends_on\":[],"
                            + "\"satisfies\":[7]}],\"interfaces\":[],\"key_links\":[]}"));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    private static <T> void verify(T value, JsonSource<T> json, MapSource<T> map, JsonReader<T> fromJson,
                                   MapReader<T> fromMap, ParserReader<T> readFrom, GeneratorWriter<T> writeTo)
            throws Exception {
        var text = json.json(value);
        assertEquals(value, fromJson.read(text));
        assertEquals(value, fromMap.read(map.map(value)));
        try (JsonParser parser = FACTORY.createParser(text)) {
            parser.nextToken();
            assertEquals(value, readFrom.read(parser));
        }
        var out = new StringWriter();
        try (JsonGenerator generator = FACTORY.createGenerator(out)) {
            writeTo.write(value, generator);
        }
        assertEquals(text, out.toString());
    }

    private static void eachMember(List<String> wires, Function<String, ?> parse, Function<String, ?> orNull) {
        for (var wire : wires) {
            assertNotNull(parse.apply(wire));
            assertNotNull(orNull.apply(wire));
            assertNotNull(orNull.apply(wire.toUpperCase()));
        }
        assertNull(orNull.apply("no-such-member"));
        assertNull(orNull.apply(null));
        assertThrows(IllegalArgumentException.class, () -> parse.apply("no-such-member"));
        assertThrows(IllegalArgumentException.class, () -> parse.apply(null));
    }

    private static IntentCriterion criterion() {
        return new IntentCriterion("AC-001", "When the relay reconnects, the queue shall keep every event.",
                List.of(1, 2), CriterionStatus.MET, "The reconnection tests cover the queued events.");
    }

    private static BuilderQuestion question() {
        return new BuilderQuestion("BQ-001", "Which timeout applies to the reconnect attempt?",
                BuilderQuestion.Sort.OPERATOR, true, BuilderQuestion.State.OPEN, null);
    }

    private static BuilderCheckResource builderCheck() {
        return new BuilderCheckResource(BuilderCheckResource.Resolution.PENDING, List.of(question()));
    }

    private static PlanIntentResource planIntent() {
        return new PlanIntentResource("fix-relay", 3,
                "Make the relay reconnect without losing queued events.",
                List.of(criterion(),
                        new IntentCriterion("AC-002", "The relay shall expose its reconnect count.",
                                List.of(2), CriterionStatus.NOT_VERIFIED,
                                "The harness cannot observe the timeout."),
                        new IntentCriterion("AC-003", "The queue shall stay on disk.",
                                List.of(1), CriterionStatus.NOT_MET, "The format moved in memory."),
                        new IntentCriterion("AC-004", "The old endpoint shall keep working.",
                                List.of(2), null, null)),
                List.of("Do not change the queue format."),
                List.of(new PlanIntentResource.Decision("DC-001", "Retry with exponential backoff",
                                IntentDecider.OPERATOR, List.of("fixed", "exponential"), "Q-007"),
                        new PlanIntentResource.Decision("DC-002", "A quorum chose the backoff ceiling",
                                IntentDecider.QUORUM, List.of("30 s", "5 min"), "Q-008"),
                        new PlanIntentResource.Decision("DC-003", "The transport stays HTTP/2",
                                IntentDecider.MODEL, List.of("HTTP/2", "HTTP/1.1"), null)),
                List.of("Does the relay need IPv6 support?"),
                List.of(new PlanIntentResource.DeferredItem("DF-001", "Metrics for reconnects",
                        "No metrics endpoint is in scope.")),
                builderCheck());
    }

    private static DeliverableRecord deliverable() {
        return new DeliverableRecord(1, "Reconnect the relay", "Reconnect with backoff.",
                List.of(new DeliverableRecord.AffectedFile("pm-relay/src/main/java/Relay.java",
                                DeliverableRecord.FileIntent.WRITE_REPLACE),
                        new DeliverableRecord.AffectedFile("pm-relay/src/test/java/RelayTest.java",
                                DeliverableRecord.FileIntent.WRITE_NEW)),
                List.of(), List.of("AC-001"), "The reconnection tests.");
    }

    private static DependencyJustification dependency() {
        return new DependencyJustification("de.cuioss:cui-http", "2.1.0",
                "The HTTP client carries the reconnect policy.", 1);
    }

    private static OutlineResource outline() {
        return new OutlineResource("fix-relay", 2, "Reconnect the relay and keep the queue on disk.",
                List.of(deliverable(),
                        new DeliverableRecord(2, "Keep the queue", "Persist the queue.",
                                List.of(new DeliverableRecord.AffectedFile("pm-relay/src/main/java/Queue.java",
                                        DeliverableRecord.FileIntent.WRITE_NEW)),
                                List.of(1), List.of("AC-003"), null)),
                List.of(dependency()), List.of("RelayConnection"), List.of("queue → relay"),
                "queue → relay");
    }

    private static DoneWhenCheck doneWhen() {
        return new DoneWhenCheck("Every plan of the epic landed and its criterion is met.",
                DoneWhenCheck.Verdict.MET, List.of("pm://plans/fix-relay/report"));
    }

    private static EpicIntentResource epicIntent() {
        return new EpicIntentResource("relay-hardening", "The relay survives a dropped connection.",
                List.of(doneWhen(),
                        new DoneWhenCheck("The reconnect count is observable.",
                                DoneWhenCheck.Verdict.NOT_MET, List.of("pm://plans/fix-relay/report")),
                        new DoneWhenCheck("The old endpoint still answers.", DoneWhenCheck.Verdict.PENDING,
                                List.of())),
                List.of("Do not change the queue format."),
                List.of("Does the relay need IPv6 support?"),
                List.of(new EpicIntentResource.PlanDoneWhen("fix-relay", "Fix Relay Transport",
                                List.of("The relay reconnects within five seconds.")),
                        new EpicIntentResource.PlanDoneWhen("queue-format", "Keep the Queue",
                                List.of("The queue survives a restart."))));
    }

    private static TokenBucket bucket() {
        return new TokenBucket("3x–5x", 3.0, 5.0, 4, 4);
    }

    private static OverSizeDistributionResource distribution() {
        return new OverSizeDistributionResource("pm-mcp-clients", "implement", 3.0, 2.0,
                List.of(new TokenBucket("0–1x", 0.0, 1.0, 8, 0),
                        new TokenBucket("1–2x", 1.0, 2.0, 5, 0),
                        new TokenBucket("2–3x", 2.0, 3.0, 3, 3),
                        new TokenBucket("3x+", 3.0, 5.0, 4, 4)));
    }

    @FunctionalInterface
    private interface JsonSource<T> {
        String json(T value) throws Exception;
    }

    @FunctionalInterface
    private interface MapSource<T> {
        Map<String, Object> map(T value) throws Exception;
    }

    @FunctionalInterface
    private interface JsonReader<T> {
        T read(String json) throws Exception;
    }

    @FunctionalInterface
    private interface MapReader<T> {
        T read(Map<String, Object> map) throws Exception;
    }

    @FunctionalInterface
    private interface ParserReader<T> {
        T read(JsonParser parser) throws Exception;
    }

    @FunctionalInterface
    private interface GeneratorWriter<T> {
        void write(T value, JsonGenerator generator) throws Exception;
    }
}
