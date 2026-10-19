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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * An epic's intent as both clients show it (Model Work, Epic Intent; design <em>Intent and
 * Outline</em>): where the epic ends, the done-when checks with their verdicts and evidence, the
 * non-goals every plan inherits, what is not yet specified, and each plan's own done-when criterion.
 * <p>
 * The completion review judges each check against evidence, never against the epic's rows. The plans'
 * criteria are shown with the epic's intent, because a plan cannot silently take on what the epic
 * excluded or promise what its own criterion does not state.
 *
 * @param epicId          the epic's id
 * @param destination     one sentence: where the epic ends
 * @param doneWhen        the epic's done-when checks, three to six
 * @param nonGoals        what the epic deliberately leaves out, at least one
 * @param notYetSpecified the questions that can be stated but are not yet answered
 * @param plans           each staged plan with its own done-when criterion
 */
public record EpicIntentResource(
        String epicId,
        String destination,
        List<DoneWhenCheck> doneWhen,
        List<String> nonGoals,
        List<String> notYetSpecified,
        List<PlanDoneWhen> plans
) {

    public static final String FIELD_EPIC_ID = "epic_id";
    public static final String FIELD_DESTINATION = "destination";
    public static final String FIELD_DONE_WHEN = "done_when";
    public static final String FIELD_NON_GOALS = "non_goals";
    public static final String FIELD_NOT_YET_SPECIFIED = "not_yet_specified";
    public static final String FIELD_PLANS = "plans";

    private static final String TYPE = "EpicIntentResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_EPIC_ID, FIELD_DESTINATION, FIELD_DONE_WHEN, FIELD_NON_GOALS, FIELD_NOT_YET_SPECIFIED,
            FIELD_PLANS);

    /**
     * One staged plan's done-when criterion.
     *
     * @param planId  the plan's id
     * @param title   the plan's title
     * @param doneWhen the observable criterion of that plan
     */
    public record PlanDoneWhen(String planId, String title, List<String> doneWhen) {

        private static final String FIELD_PLAN_ID = "plan_id";
        private static final String FIELD_TITLE = "title";
        private static final String TYPE = "PlanDoneWhen";
        private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_PLAN_ID, FIELD_TITLE, FIELD_DONE_WHEN);

        public PlanDoneWhen {
            Objects.requireNonNull(planId, FIELD_PLAN_ID);
            Objects.requireNonNull(title, FIELD_TITLE);
            if (planId.isBlank() || title.isBlank()) {
                throw new IllegalArgumentException("plan_id and title must not be blank");
            }
            doneWhen = doneWhen == null ? List.of() : List.copyOf(doneWhen);
            if (doneWhen.isEmpty()) {
                throw new IllegalArgumentException("done_when must not be empty");
            }
            if (doneWhen.stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new IllegalArgumentException("done_when must not carry a blank entry");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_PLAN_ID, planId);
            map.put(FIELD_TITLE, title);
            map.put(FIELD_DONE_WHEN, doneWhen);
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(FIELD_PLAN_ID, planId);
            generator.writeStringField(FIELD_TITLE, title);
            IntentJson.writeStrings(generator, FIELD_DONE_WHEN, doneWhen);
            generator.writeEndObject();
        }

        private static PlanDoneWhen fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new PlanDoneWhen(
                        IntentJson.requireText(map, FIELD_PLAN_ID, TYPE),
                        IntentJson.requireText(map, FIELD_TITLE, TYPE),
                        IntentJson.readStrings(map, FIELD_DONE_WHEN));
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
            }
        }
    }

    public EpicIntentResource {
        Objects.requireNonNull(epicId, FIELD_EPIC_ID);
        Objects.requireNonNull(destination, FIELD_DESTINATION);
        if (epicId.isBlank()) {
            throw new IllegalArgumentException("epic_id must not be blank");
        }
        if (destination.isBlank()) {
            throw new IllegalArgumentException("destination must not be blank");
        }
        doneWhen = requireDoneWhen(doneWhen);
        nonGoals = requireNonGoals(nonGoals);
        notYetSpecified = copyTexts(notYetSpecified, FIELD_NOT_YET_SPECIFIED);
        plans = requirePlans(plans);
    }

    private static List<DoneWhenCheck> requireDoneWhen(List<DoneWhenCheck> value) {
        var checks = value == null ? List.<DoneWhenCheck>of() : List.copyOf(value);
        if (checks.size() < 3 || checks.size() > 6) {
            throw new IllegalArgumentException("done_when must hold three to six checks");
        }
        return checks;
    }

    private static List<String> requireNonGoals(List<String> value) {
        var goals = copyTexts(value, FIELD_NON_GOALS);
        if (goals.isEmpty()) {
            throw new IllegalArgumentException("non_goals must not be empty");
        }
        return goals;
    }

    private static List<String> copyTexts(List<String> value, String name) {
        var texts = value == null ? List.<String>of() : List.copyOf(value);
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException(name + " must not carry a blank entry");
        }
        return texts;
    }

    private static List<PlanDoneWhen> requirePlans(List<PlanDoneWhen> value) {
        var rows = value == null ? List.<PlanDoneWhen>of() : List.copyOf(value);
        var planIds = new HashSet<String>();
        for (var plan : rows) {
            if (!planIds.add(plan.planId())) {
                throw new IllegalArgumentException("duplicate plan id: " + plan.planId());
            }
        }
        return rows;
    }

    /**
     * @return the number of checks the completion review decided
     */
    public long decidedCount() {
        return doneWhen.stream().filter(DoneWhenCheck::decided).count();
    }

    /**
     * @return {@code true} if every check is met
     */
    public boolean allMet() {
        return !doneWhen.isEmpty() && doneWhen.stream().allMatch(DoneWhenCheck::met);
    }

    /**
     * @return the checks the completion review has not decided yet
     */
    public List<DoneWhenCheck> pendingChecks() {
        return doneWhen.stream().filter(check -> !check.decided()).toList();
    }

    /**
     * @param planId the plan's id
     * @return the plan's done-when criterion, or {@code null}
     */
    public PlanDoneWhen plan(String planId) {
        return plans.stream().filter(plan -> plan.planId().equals(planId)).findFirst().orElse(null);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_EPIC_ID, epicId);
        map.put(FIELD_DESTINATION, destination);
        map.put(FIELD_DONE_WHEN, doneWhen.stream().map(DoneWhenCheck::toMap).toList());
        map.put(FIELD_NON_GOALS, nonGoals);
        map.put(FIELD_NOT_YET_SPECIFIED, notYetSpecified);
        map.put(FIELD_PLANS, plans.stream().map(PlanDoneWhen::toMap).toList());
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this epic intent to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_EPIC_ID, epicId);
        generator.writeStringField(FIELD_DESTINATION, destination);
        IntentJson.writeArray(generator, FIELD_DONE_WHEN, doneWhen, DoneWhenCheck::writeTo);
        IntentJson.writeStrings(generator, FIELD_NON_GOALS, nonGoals);
        IntentJson.writeStrings(generator, FIELD_NOT_YET_SPECIFIED, notYetSpecified);
        IntentJson.writeArray(generator, FIELD_PLANS, plans, PlanDoneWhen::writeTo);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed epic intent
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static EpicIntentResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new EpicIntentResource(
                    IntentJson.requireText(map, FIELD_EPIC_ID, TYPE),
                    IntentJson.requireText(map, FIELD_DESTINATION, TYPE),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_DONE_WHEN),
                            DoneWhenCheck::fromMap),
                    IntentJson.readStrings(map, FIELD_NON_GOALS),
                    IntentJson.readStrings(map, FIELD_NOT_YET_SPECIFIED),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_PLANS), PlanDoneWhen::fromMap));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed epic intent
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static EpicIntentResource fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one epic intent from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed epic intent
     * @throws IOException on format failure
     */
    public static EpicIntentResource readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
