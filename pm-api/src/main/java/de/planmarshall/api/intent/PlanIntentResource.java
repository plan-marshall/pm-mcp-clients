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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A plan's intent record as both clients show it (Phase Workflows, The Intent Record; design
 * <em>Intent and Outline</em>): the goal, the EARS criteria with the deliverables that satisfy them
 * and their self-review verdicts, the non-goals, the decisions with who took each, the open questions
 * and the deferred items, and the builder check.
 * <p>
 * The separation of the decisions is load-bearing: an operator's or a quorum's decision is shown
 * apart from a role's own call and the facts the repository settled. The criteria are shown
 * before the self-review without a verdict and afterwards with {@link CriterionStatus}, so a client
 * never presents an unevaluated criterion as met.
 *
 * @param planId       the plan's id
 * @param revision     the intent record's revision, from 1
 * @param goal         the plan's goal in one sentence
 * @param criteria     the EARS criteria, at least one
 * @param nonGoals     what the plan deliberately leaves out, at least one, each with its record id
 * @param decisions    the decisions the record carries, each with its decider
 * @param openQuestions the questions the plan still carries
 * @param deferred     the items the plan defers, each with its reason
 * @param builderCheck the builder check, or {@code null} before it ran
 */
public record PlanIntentResource(
        String planId,
        int revision,
        String goal,
        List<IntentCriterion> criteria,
        List<NonGoal> nonGoals,
        List<Decision> decisions,
        List<String> openQuestions,
        List<DeferredItem> deferred,
        @Nullable BuilderCheckResource builderCheck
) {

    public static final String FIELD_PLAN_ID = "plan_id";
    public static final String FIELD_REVISION = "revision";
    public static final String FIELD_GOAL = "goal";
    public static final String FIELD_CRITERIA = "criteria";
    public static final String FIELD_NON_GOALS = "non_goals";
    public static final String FIELD_DECISIONS = "decisions";
    public static final String FIELD_OPEN_QUESTIONS = "open_questions";
    public static final String FIELD_DEFERRED = "deferred";
    public static final String FIELD_BUILDER_CHECK = "builder_check";

    private static final String TYPE = "PlanIntentResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_PLAN_ID, FIELD_REVISION, FIELD_GOAL, FIELD_CRITERIA, FIELD_NON_GOALS, FIELD_DECISIONS,
            FIELD_OPEN_QUESTIONS, FIELD_DEFERRED, FIELD_BUILDER_CHECK);

    /**
     * One decision of the intent record, with who took it (Phase Workflows, The Intent Record).
     *
     * @param id             the decision's id ({@code DC-n})
     * @param text           the decision
     * @param decider        who took it
     * @param optionsWeighed the options weighed, in the record's order
     * @param questionId     the answered question for an operator or quorum decision, otherwise
     *                       {@code null}
     * @param evidence       the evidence a {@code fact} decision settles it with, a path or a
     *                       {@code pm://} URI; {@code null} for every other decider
     * @param adr            the architecture decision record the decision follows or contradicts, or
     *                       {@code null} where it names none
     */
    public record Decision(
            String id,
            String text,
            IntentDecider decider,
            List<String> optionsWeighed,
            @Nullable String questionId,
            @Nullable String evidence,
            @Nullable AdrReference adr
    ) {

        private static final String FIELD_ID = "id";
        private static final String FIELD_TEXT = "text";
        private static final String FIELD_DECIDER = "decider";
        private static final String FIELD_OPTIONS_WEIGHED = "options_weighed";
        private static final String FIELD_QUESTION_ID = "question_id";
        private static final String FIELD_EVIDENCE = "evidence";
        private static final String FIELD_ADR = "adr";
        private static final String TYPE = "Decision";
        private static final Set<String> KNOWN_FIELDS = Set.of(
                FIELD_ID, FIELD_TEXT, FIELD_DECIDER, FIELD_OPTIONS_WEIGHED, FIELD_QUESTION_ID, FIELD_EVIDENCE,
                FIELD_ADR);

        public Decision {
            Objects.requireNonNull(id, FIELD_ID);
            Objects.requireNonNull(text, FIELD_TEXT);
            Objects.requireNonNull(decider, FIELD_DECIDER);
            if (id.isBlank() || text.isBlank()) {
                throw new IllegalArgumentException("id and text must not be blank");
            }
            optionsWeighed = optionsWeighed == null ? List.of() : List.copyOf(optionsWeighed);
            if (optionsWeighed.stream().anyMatch(option -> option == null || option.isBlank())) {
                throw new IllegalArgumentException(FIELD_OPTIONS_WEIGHED + " must not carry a blank option");
            }
            if (decider.operatorFacing() && (questionId == null || questionId.isBlank())) {
                throw new IllegalArgumentException(
                        "a " + decider.wireValue() + " decision requires its " + FIELD_QUESTION_ID);
            }
            if (!decider.operatorFacing() && questionId != null) {
                throw new IllegalArgumentException("only an operator or quorum decision names a question");
            }
            if (decider == IntentDecider.FACT && (evidence == null || evidence.isBlank())) {
                throw new IllegalArgumentException("a fact decision requires its " + FIELD_EVIDENCE);
            }
            if (decider != IntentDecider.FACT && evidence != null) {
                throw new IllegalArgumentException("only a fact decision names its " + FIELD_EVIDENCE);
            }
            if (adr != null && adr.relation() == AdrRelation.CONTRADICTS
                    && decider != IntentDecider.OPERATOR) {
                throw new IllegalArgumentException("a contradicts decision is only the operator's, never the "
                        + decider.wireValue() + " decision's");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_ID, id);
            map.put(FIELD_TEXT, text);
            map.put(FIELD_DECIDER, decider.wireValue());
            map.put(FIELD_OPTIONS_WEIGHED, optionsWeighed);
            if (questionId != null) {
                map.put(FIELD_QUESTION_ID, questionId);
            }
            if (evidence != null) {
                map.put(FIELD_EVIDENCE, evidence);
            }
            if (adr != null) {
                map.put(FIELD_ADR, adr.toMap());
            }
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(FIELD_ID, id);
            generator.writeStringField(FIELD_TEXT, text);
            generator.writeStringField(FIELD_DECIDER, decider.wireValue());
            IntentJson.writeStrings(generator, FIELD_OPTIONS_WEIGHED, optionsWeighed);
            IntentJson.writeOptionalString(generator, FIELD_QUESTION_ID, questionId);
            IntentJson.writeOptionalString(generator, FIELD_EVIDENCE, evidence);
            if (adr != null) {
                generator.writeFieldName(FIELD_ADR);
                adr.writeTo(generator);
            }
            generator.writeEndObject();
        }

        private static Decision fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            var adrMap = IntentJson.optionalObject(map, FIELD_ADR, TYPE);
            try {
                return new Decision(
                        IntentJson.requireText(map, FIELD_ID, TYPE),
                        IntentJson.requireText(map, FIELD_TEXT, TYPE),
                        IntentDecider.fromWire(IntentJson.requireText(map, FIELD_DECIDER, TYPE)),
                        IntentJson.readStrings(map, FIELD_OPTIONS_WEIGHED),
                        IntentJson.optionalText(map, FIELD_QUESTION_ID, TYPE),
                        IntentJson.optionalText(map, FIELD_EVIDENCE, TYPE),
                        adrMap != null ? AdrReference.fromMap(adrMap) : null);
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * The architecture decision record a decision follows or contradicts (design <em>Intent and
     * Outline</em>, Choices and Who Made Them).
     *
     * @param adrId    the record's identifier, not blank
     * @param relation whether the decision follows or contradicts it
     */
    public record AdrReference(String adrId, AdrRelation relation) {

        public static final String FIELD_ADR_ID = "adr_id";
        public static final String FIELD_RELATION = "relation";

        private static final String TYPE = "AdrReference";
        private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_ADR_ID, FIELD_RELATION);

        public AdrReference {
            Objects.requireNonNull(adrId, FIELD_ADR_ID);
            Objects.requireNonNull(relation, FIELD_RELATION);
            if (adrId.isBlank()) {
                throw new IllegalArgumentException(FIELD_ADR_ID + " must not be blank");
            }
        }

        /**
         * @return the map representation
         */
        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_ADR_ID, adrId);
            map.put(FIELD_RELATION, relation.wireValue());
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(FIELD_ADR_ID, adrId);
            generator.writeStringField(FIELD_RELATION, relation.wireValue());
            generator.writeEndObject();
        }

        private static AdrReference fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new AdrReference(
                        IntentJson.requireText(map, FIELD_ADR_ID, TYPE),
                        AdrRelation.fromWire(IntentJson.requireText(map, FIELD_RELATION, TYPE)));
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
            }
        }
    }

    /** The closed list of how a decision relates to an architecture decision record. */
    public enum AdrRelation {

        /** The decision follows the record. */
        FOLLOWS("follows"),

        /** The decision departs from the record. */
        CONTRADICTS("contradicts");

        private final String wireValue;

        AdrRelation(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the wire value used in representations
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * @param value the wire value
         * @return the matching relation, or {@code null}
         */
        public static AdrRelation fromWireOrNull(String value) {
            if (value == null) {
                return null;
            }
            for (var relation : values()) {
                if (relation.wireValue.equalsIgnoreCase(value.trim())) {
                    return relation;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the matching relation
         * @throws IllegalArgumentException if no relation carries that value (fail-closed)
         */
        public static AdrRelation fromWire(String value) {
            var relation = fromWireOrNull(value);
            if (relation == null) {
                throw new IllegalArgumentException("Unknown ADR relation: " + value);
            }
            return relation;
        }

        /**
         * @return the wire values of every relation, in catalogue order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(AdrRelation::wireValue).toList();
        }
    }

    /**
     * One non-goal of the intent record: what the plan deliberately leaves out, with the record's
     * own id and never a position (design <em>Intent and Outline</em>, Non-Goals).
     *
     * @param id   the non-goal's id ({@code NG-n})
     * @param text the claim the plan does not make
     */
    public record NonGoal(String id, String text) {

        public static final String FIELD_ID = "id";
        public static final String FIELD_TEXT = "text";

        /** The engine's non-goal id form ({@code IntentRecord.NON_GOAL_ID}). */
        public static final Pattern ID = Pattern.compile("NG-[0-9]{1,4}");

        private static final String TYPE = "NonGoal";
        private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_ID, FIELD_TEXT);

        public NonGoal {
            Objects.requireNonNull(id, FIELD_ID);
            Objects.requireNonNull(text, FIELD_TEXT);
            if (!ID.matcher(id).matches()) {
                throw new IllegalArgumentException(FIELD_ID + " is NG-<n>: " + id);
            }
            if (text.isBlank()) {
                throw new IllegalArgumentException(FIELD_TEXT + " must not be blank");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_ID, id);
            map.put(FIELD_TEXT, text);
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(FIELD_ID, id);
            generator.writeStringField(FIELD_TEXT, text);
            generator.writeEndObject();
        }

        private static NonGoal fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new NonGoal(
                        IntentJson.requireText(map, FIELD_ID, TYPE),
                        IntentJson.requireText(map, FIELD_TEXT, TYPE));
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * One deferred item of the intent record.
     *
     * @param id     the item's id ({@code DF-n})
     * @param text   the item
     * @param reason why the plan defers it
     */
    public record DeferredItem(String id, String text, String reason) {
        private static final String FIELD_ID = "id";
        private static final String FIELD_TEXT = "text";
        private static final String FIELD_REASON = "reason";
        private static final String TYPE = "DeferredItem";
        private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_ID, FIELD_TEXT, FIELD_REASON);

        public DeferredItem {
            Objects.requireNonNull(id, FIELD_ID);
            Objects.requireNonNull(text, FIELD_TEXT);
            Objects.requireNonNull(reason, FIELD_REASON);
            if (id.isBlank() || text.isBlank() || reason.isBlank()) {
                throw new IllegalArgumentException("id, text, and reason must not be blank");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_ID, id);
            map.put(FIELD_TEXT, text);
            map.put(FIELD_REASON, reason);
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(FIELD_ID, id);
            generator.writeStringField(FIELD_TEXT, text);
            generator.writeStringField(FIELD_REASON, reason);
            generator.writeEndObject();
        }

        private static DeferredItem fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new DeferredItem(
                        IntentJson.requireText(map, FIELD_ID, TYPE),
                        IntentJson.requireText(map, FIELD_TEXT, TYPE),
                        IntentJson.requireText(map, FIELD_REASON, TYPE));
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
            }
        }
    }

    public PlanIntentResource {
        Objects.requireNonNull(planId, FIELD_PLAN_ID);
        Objects.requireNonNull(goal, FIELD_GOAL);
        if (planId.isBlank()) {
            throw new IllegalArgumentException("plan_id must not be blank");
        }
        if (goal.isBlank()) {
            throw new IllegalArgumentException("goal must not be blank");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("criteria must not be empty");
        }
        requireUnique(criteria.stream().map(IntentCriterion::id).toList(), "criterion");
        nonGoals = nonGoals == null ? List.of() : List.copyOf(nonGoals);
        if (nonGoals.isEmpty()) {
            throw new IllegalArgumentException("non_goals must not be empty");
        }
        requireUnique(nonGoals.stream().map(NonGoal::id).toList(), "non-goal");
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        requireUnique(decisions.stream().map(Decision::id).toList(), "decision");
        openQuestions = openQuestions == null ? List.of() : List.copyOf(openQuestions);
        if (openQuestions.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("open_questions must not carry a blank entry");
        }
        deferred = deferred == null ? List.of() : List.copyOf(deferred);
        requireUnique(deferred.stream().map(DeferredItem::id).toList(), "deferred item");
    }

    private static void requireUnique(List<String> ids, String kind) {
        var seen = new HashSet<String>();
        for (var id : ids) {
            if (!seen.add(id)) {
                throw new IllegalArgumentException("duplicate " + kind + " id: " + id);
            }
        }
    }

    /**
     * @param id the criterion's id
     * @return the criterion, or {@code null}
     */
    public @Nullable IntentCriterion criterion(String id) {
        return criteria.stream().filter(criterion -> criterion.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * @param status the verdict
     * @return the criteria with that verdict
     */
    public List<IntentCriterion> criteria(CriterionStatus status) {
        return criteria.stream().filter(criterion -> criterion.status() == status).toList();
    }

    /**
     * @return the number of criteria the self-review judged met
     */
    public long metCount() {
        return criteria(CriterionStatus.MET).size();
    }

    /**
     * @return the number of criteria the self-review judged not met
     */
    public long notMetCount() {
        return criteria(CriterionStatus.NOT_MET).size();
    }

    /**
     * @return the number of criteria the plan could not verify
     */
    public long notVerifiedCount() {
        return criteria(CriterionStatus.NOT_VERIFIED).size();
    }

    /**
     * @return {@code true} if at least one criterion is not verified
     */
    public boolean hasNotVerified() {
        return criteria.stream().anyMatch(IntentCriterion::notVerified);
    }

    /**
     * @return the decisions the operator or a quorum took
     */
    public List<Decision> operatorDecisions() {
        return decisions.stream().filter(decision -> decision.decider().operatorFacing()).toList();
    }

    /**
     * @return the role's own calls, which are shown apart from the operator's decisions
     */
    public List<Decision> roleCalls() {
        return decisions.stream().filter(decision -> decision.decider() == IntentDecider.ROLE).toList();
    }

    /**
     * @return the builder check's open questions, or an empty list before the check ran
     */
    public List<BuilderQuestion> openBuilderQuestions() {
        return builderCheck != null ? builderCheck.openQuestions() : List.of();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_REVISION, revision);
        map.put(FIELD_GOAL, goal);
        map.put(FIELD_CRITERIA, criteria.stream().map(IntentCriterion::toMap).toList());
        map.put(FIELD_NON_GOALS, nonGoals.stream().map(NonGoal::toMap).toList());
        map.put(FIELD_DECISIONS, decisions.stream().map(Decision::toMap).toList());
        map.put(FIELD_OPEN_QUESTIONS, openQuestions);
        map.put(FIELD_DEFERRED, deferred.stream().map(DeferredItem::toMap).toList());
        if (builderCheck != null) {
            map.put(FIELD_BUILDER_CHECK, builderCheck.toMap());
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this intent record to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_PLAN_ID, planId);
        generator.writeNumberField(FIELD_REVISION, revision);
        generator.writeStringField(FIELD_GOAL, goal);
        IntentJson.writeArray(generator, FIELD_CRITERIA, criteria, IntentCriterion::writeTo);
        IntentJson.writeArray(generator, FIELD_NON_GOALS, nonGoals, NonGoal::writeTo);
        IntentJson.writeArray(generator, FIELD_DECISIONS, decisions, Decision::writeTo);
        IntentJson.writeStrings(generator, FIELD_OPEN_QUESTIONS, openQuestions);
        IntentJson.writeArray(generator, FIELD_DEFERRED, deferred, DeferredItem::writeTo);
        if (builderCheck != null) {
            generator.writeFieldName(FIELD_BUILDER_CHECK);
            builderCheck.writeTo(generator);
        }
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed intent record
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static PlanIntentResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var builderMap = JsonTree.objectOrNull(map, FIELD_BUILDER_CHECK, TYPE);
        try {
            return new PlanIntentResource(
                    IntentJson.requireText(map, FIELD_PLAN_ID, TYPE),
                    IntentJson.requireInt(map, FIELD_REVISION, 1, Integer.MAX_VALUE, TYPE),
                    IntentJson.requireText(map, FIELD_GOAL, TYPE),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_CRITERIA), IntentCriterion::fromMap),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_NON_GOALS), NonGoal::fromMap),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_DECISIONS), Decision::fromMap),
                    IntentJson.readStrings(map, FIELD_OPEN_QUESTIONS),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_DEFERRED), DeferredItem::fromMap),
                    builderMap != null ? BuilderCheckResource.fromMap(builderMap) : null);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed intent record
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static PlanIntentResource fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one intent record from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed intent record
     * @throws IOException on format failure
     */
    public static PlanIntentResource readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
