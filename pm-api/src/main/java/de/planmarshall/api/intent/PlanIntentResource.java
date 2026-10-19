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

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A plan's intent record as both clients show it (Phase Workflows, The Intent Record; design
 * <em>Intent and Outline</em>): the goal, the EARS criteria with the deliverables that satisfy them
 * and their self-review verdicts, the non-goals, the decisions with who took each, the open questions
 * and the deferred items, and the builder check.
 * <p>
 * The separation of the decisions is load-bearing: an operator's or a quorum's decision is shown
 * apart from the model's own calls and the facts the repository settled. The criteria are shown
 * before the self-review without a verdict and afterwards with {@link CriterionStatus}, so a client
 * never presents an unevaluated criterion as met.
 *
 * @param planId       the plan's id
 * @param revision     the intent record's revision, from 1
 * @param goal         the plan's goal in one sentence
 * @param criteria     the EARS criteria, at least one
 * @param nonGoals     what the plan deliberately leaves out, at least one
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
        List<String> nonGoals,
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
     */
    public record Decision(
            String id,
            String text,
            IntentDecider decider,
            List<String> optionsWeighed,
            @Nullable String questionId
    ) {

        private static final String TYPE = "Decision";
        private static final Set<String> KNOWN_FIELDS = Set.of(
                "id", "text", "decider", "options_weighed", "question_id");

        public Decision {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(decider, "decider");
            if (id.isBlank() || text.isBlank()) {
                throw new IllegalArgumentException("id and text must not be blank");
            }
            optionsWeighed = optionsWeighed == null ? List.of() : List.copyOf(optionsWeighed);
            if (optionsWeighed.stream().anyMatch(option -> option == null || option.isBlank())) {
                throw new IllegalArgumentException("options_weighed must not carry a blank option");
            }
            if (decider.operatorFacing() && (questionId == null || questionId.isBlank())) {
                throw new IllegalArgumentException("a " + decider.wireValue() + " decision requires its question_id");
            }
            if (!decider.operatorFacing() && questionId != null) {
                throw new IllegalArgumentException("only an operator or quorum decision names a question");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("id", id);
            map.put("text", text);
            map.put("decider", decider.wireValue());
            map.put("options_weighed", optionsWeighed);
            if (questionId != null) {
                map.put("question_id", questionId);
            }
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("id", id);
            generator.writeStringField("text", text);
            generator.writeStringField("decider", decider.wireValue());
            IntentJson.writeStrings(generator, "options_weighed", optionsWeighed);
            IntentJson.writeOptionalString(generator, "question_id", questionId);
            generator.writeEndObject();
        }

        private static Decision fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new Decision(
                        IntentJson.requireText(map, "id", TYPE),
                        IntentJson.requireText(map, "text", TYPE),
                        IntentDecider.fromWire(IntentJson.requireText(map, "decider", TYPE)),
                        IntentJson.readStrings(map, "options_weighed"),
                        IntentJson.optionalText(map, "question_id", TYPE));
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

        private static final String TYPE = "DeferredItem";
        private static final Set<String> KNOWN_FIELDS = Set.of("id", "text", "reason");

        public DeferredItem {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(reason, "reason");
            if (id.isBlank() || text.isBlank() || reason.isBlank()) {
                throw new IllegalArgumentException("id, text, and reason must not be blank");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("id", id);
            map.put("text", text);
            map.put("reason", reason);
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("id", id);
            generator.writeStringField("text", text);
            generator.writeStringField("reason", reason);
            generator.writeEndObject();
        }

        private static DeferredItem fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new DeferredItem(
                        IntentJson.requireText(map, "id", TYPE),
                        IntentJson.requireText(map, "text", TYPE),
                        IntentJson.requireText(map, "reason", TYPE));
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
        if (nonGoals.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("non_goals must not carry a blank entry");
        }
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
     * @return the model's own calls, which are shown apart from the operator's decisions
     */
    public List<Decision> modelCalls() {
        return decisions.stream().filter(decision -> decision.decider() == IntentDecider.MODEL).toList();
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
        map.put(FIELD_NON_GOALS, nonGoals);
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
        IntentJson.writeStrings(generator, FIELD_NON_GOALS, nonGoals);
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
                    IntentJson.readStrings(map, FIELD_NON_GOALS),
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
