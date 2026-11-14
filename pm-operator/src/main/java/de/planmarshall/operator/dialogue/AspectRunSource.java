/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.dialogue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.OperatorGate;
import de.planmarshall.operator.OperatorRefusedException;
import de.planmarshall.operator.StartPrompt;

/**
 * One aspect of the machine's onboarding as a later run ({@code onboard_run: reconfigure}) as a
 * {@link StateDialogue.Source}: the verb {@code config <aspect>} starts it from the machine settings
 * (the operator wait {@code settings.choose}, reached from {@code onboard.completed}) and walks it
 * until the aspect returns there, which ends the run.
 *
 * <p>The source answers a state before the dialogue asks: a consent state whose kind the verb was
 * given, a confirmation the verb carries {@code --yes} for, and a state whose own facts the
 * arguments name. An answer the verb carries and no state of the run asks for is reported as unused,
 * never silently dropped ({@code 01-commands.adoc#direct-verbs}).
 */
public final class AspectRunSource implements StateDialogue.Source {

    /** The path of the current onboarding state, as {@link OnboardingSource} reads it. */
    public static final String STATE_PATH = OnboardingSource.STATE_PATH;

    /** The machine settings' operator wait, from which a later run is called and where it ends. */
    public static final String SETTINGS_STATE = "settings.choose";

    /** The runtime refused a read or a step of the run: the status it answered with. */
    public static final class RefusedException extends IOException {

        private static final long serialVersionUID = 1L;
        private final int status;

        RefusedException(String what, int status) {
            super(what + " answered HTTP " + status);
            this.status = status;
        }

        /**
         * @return the HTTP status the runtime answered with
         */
        public int status() {
            return status;
        }
    }

    private static final String SETTINGS_LINK = "settings";
    private static final String RECONFIGURE_PREFIX = "reconfigure:";
    private static final String KIND = "kind";
    private static final String FETCH_ICONS = "fetch_icons";
    private static final String MODEL_ADVICE = "model_advice";
    private static final String NO = "no";
    private static final String YES = "yes";
    private static final String CONFIRM_MARKER = "confirm";
    /** The suffix of a later run's own state, whose submit link is the displayed change. */
    private static final String RECONFIGURE_SUFFIX = ".reconfigure";
    private static final String YES_FLAG = "yes";
    private static final int MAX_STEPS = 32;

    private final RuntimeClient runtime;
    private final OperatorGate gate;
    private final String linkId;
    private final Map<String, String> facts;
    private final Set<String> consents;
    private final Flags flags;
    private final Set<String> usedAnswers = new LinkedHashSet<>();
    private boolean started;
    private boolean returned;
    private Map<String, Object> lastFacts = Map.of();

    /**
     * The boolean answers a verb carries.
     *
     * @param noIcons  whether the icons consent is answered with no
     * @param noAdvice whether the model advice consent is answered with no
     * @param confirm  whether a displayed change is confirmed
     * @param noInput  whether the run stops at a state it may not be asked about
     */
    public record Flags(boolean noIcons, boolean noAdvice, boolean confirm, boolean noInput) {
    }

    /**
     * @param runtime  the runtime client
     * @param gate     the operator gate every answer is sent through
     * @param linkId   the id of the machine settings' link that calls the aspect again
     * @param facts    the facts the verb answers a state with, by field name
     * @param consents the consent kinds the verb carries
     * @param flags    the boolean answers the verb carries
     */
    public AspectRunSource(RuntimeClient runtime, OperatorGate gate, String linkId, Map<String, String> facts,
                        List<String> consents, Flags flags) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.linkId = Objects.requireNonNull(linkId, "linkId");
        this.facts = Map.copyOf(Objects.requireNonNull(facts, "facts"));
        this.consents = Set.copyOf(Objects.requireNonNull(consents, "consents"));
        this.flags = Objects.requireNonNull(flags, "flags");
    }

    /**
     * @param aspect the aspect's name as the verb spells it
     * @return the id of the machine settings' link that calls it again
     */
    public static String reconfigureLink(String aspect) {
        return RECONFIGURE_PREFIX + aspect;
    }

    /**
     * @return the arguments no state of the run asked for, in the order they were given
     */
    public List<String> unusedAnswers() {
        var unused = new ArrayList<String>();
        for (var field : facts.keySet()) {
            if (!usedAnswers.contains(field)) {
                unused.add(field);
            }
        }
        for (var kind : consents) {
            if (!usedAnswers.contains(kind)) {
                unused.add("consent:" + kind);
            }
        }
        if (flags.confirm() && !usedAnswers.contains(YES_FLAG)) {
            unused.add(YES_FLAG);
        }
        if (flags.noIcons() && !usedAnswers.contains(FETCH_ICONS)) {
            unused.add("no-icons");
        }
        if (flags.noAdvice() && !usedAnswers.contains(MODEL_ADVICE)) {
            unused.add("no-advice");
        }
        return List.copyOf(unused);
    }

    /**
     * @return whether the aspect returned to the machine settings, which ends the run
     */
    public boolean returnedToSettings() {
        return returned;
    }

    /**
     * @return the facts of the last state the run reported before it returned to the settings, so a
     *         caller can read what the check it ran reported (the plans a credential change released)
     */
    public Map<String, Object> lastFacts() {
        return lastFacts;
    }

    @Override
    public StateDialogue.Frame fetch() throws IOException {
        var frame = read();
        if (!started) {
            frame = enter(frame);
        }
        return advance(frame);
    }

    @Override
    public StateDialogue.Frame choose(String chosenLinkId) throws IOException {
        var frame = send(chosenLinkId);
        return advance(frame);
    }

    private StateDialogue.Frame advance(StateDialogue.Frame frame) throws IOException {
        var steps = 0;
        var current = frame;
        while (steps++ < MAX_STEPS) {
            if (returnedToSettings(current)) {
                returned = true;
                return asFinal(current);
            }
            lastFacts = current.facts();
            if (current.isFinal()) {
                return current;
            }
            var answer = answerFor(current);
            if (answer == null) {
                if (flags.noInput()) {
                    throw new StartPrompt.MissingInputException(current.stateId(), "a choice", null);
                }
                return current;
            }
            current = send(answer, current);
        }
        return current;
    }

    private boolean returnedToSettings(StateDialogue.Frame frame) {
        return started && SETTINGS_STATE.equals(frame.stateId());
    }

    /**
     * @param frame the machine settings' state the run returned to
     * @return the same frame as the run's last, so the dialogue stops there instead of asking
     */
    private static StateDialogue.Frame asFinal(StateDialogue.Frame frame) {
        return new StateDialogue.Frame(frame.stateId(), "completed", frame.step(), frame.totalSteps(),
                            frame.summary(), frame.facts(), frame.links());
    }

    /**
     * Walks from the state the runtime rests in to the aspect's later run: the machine settings are
     * the state {@code settings.choose}, reached from {@code onboard.completed} by its own link.
     *
     * @param frame the state the runtime reported
     * @return the first frame of the aspect's later run
     * @throws IOException on transport failure
     */
    private StateDialogue.Frame enter(StateDialogue.Frame frame) throws IOException {
        var current = frame;
        for (int steps = 0; steps < MAX_STEPS; steps++) {
            if (current.links().stream().anyMatch(link -> link.id().equals(linkId))) {
                started = true;
                return send(linkId, current);
            }
            if (current.links().stream().noneMatch(link -> SETTINGS_LINK.equals(link.id()))) {
                throw new OperatorRefusedException("the machine settings are not reachable: the runtime offers neither "
                                    + SETTINGS_LINK + " nor " + linkId);
            }
            current = send(SETTINGS_LINK);
        }
        throw new OperatorRefusedException("the machine settings were not reached");
    }

    private @Nullable String answerFor(StateDialogue.Frame frame) {
        var kind = frame.facts().get(KIND);
        if (kind instanceof String consentKind) {
            return answerForConsent(consentKind);
        }
        var primary = frame.primary();
        if (primary == null) {
            return null;
        }
        if (flags.confirm() && (frame.stateId().contains(CONFIRM_MARKER)
                            || frame.stateId().endsWith(RECONFIGURE_SUFFIX))) {
            usedAnswers.add("yes");
            markAnsweredFacts(frame);
            return primary.id();
        }
        return markAnsweredFacts(frame) ? primary.id() : null;
    }

    /**
     * @param frame the state a link answers
     * @return whether the verb's facts answer any field the frame reports
     */
    private boolean markAnsweredFacts(StateDialogue.Frame frame) {
        var answered = false;
        for (var field : facts.keySet()) {
            if (frame.facts().containsKey(field)) {
                usedAnswers.add(field);
                answered = true;
            }
        }
        return answered;
    }

    private @Nullable String answerForConsent(String consentKind) {
        if (flags.noIcons() && FETCH_ICONS.equals(consentKind)) {
            usedAnswers.add(FETCH_ICONS);
            return NO;
        }
        if (flags.noAdvice() && MODEL_ADVICE.equals(consentKind)) {
            usedAnswers.add(MODEL_ADVICE);
            return NO;
        }
        if (consents.contains(consentKind)) {
            usedAnswers.add(consentKind);
            return YES;
        }
        return null;
    }

    private StateDialogue.Frame read() throws IOException {
        var response = runtime.send(HttpRequest.get(STATE_PATH));
        if (response.status() != 200) {
            throw new OperatorRefusedException("the onboarding state", response);
        }
        return StateDialogue.fromJson(response.bodyText(), 1, false);
    }

    private StateDialogue.Frame send(String chosenLinkId) throws IOException {
        return send(chosenLinkId, null);
    }

    /**
     * @param chosenLinkId the chosen link
     * @param answered     the state the answer belongs to, or {@code null} where it answers the state
     *                     the runtime reports now
     * @return the state the runtime reports afterwards
     * @throws IOException on transport failure
     */
    private StateDialogue.Frame send(String chosenLinkId, StateDialogue.@Nullable Frame answered)
                        throws IOException {
        gate.verifyAct();
        var body = new LinkedHashMap<String, Object>();
        body.put("link", chosenLinkId);
        if (answered != null) {
            for (var entry : facts.entrySet()) {
                if (answered.facts().containsKey(entry.getKey())) {
                    body.put(entry.getKey(), entry.getValue());
                }
            }
        }
        var response = runtime.send(HttpRequest.json("POST", OnboardingSource.STEP_PATH, JsonTree.write(body)));
        if (response.status() != 200) {
            throw new OperatorRefusedException("the onboarding step", response);
        }
        return StateDialogue.fromJson(response.bodyText(), 1, false);
    }
}
