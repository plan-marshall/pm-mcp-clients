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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.OperatorGate;

/**
 * The machine onboarding as a {@link StateDialogue.Source}: the runtime's onboarding state machine
 * ({@code CLI-UC-S-06}, {@code CLI-UC-P-01}). A chosen link is sent as {@code POST
 * /api/v1/onboarding/step} with the link's id, which is how both clients drive the machine
 * onboarding.
 */
@SuppressWarnings("java:S1075")
public final class OnboardingSource implements StateDialogue.Source {

    /** The path of the current onboarding state. */
    public static final String STATE_PATH = "/api/v1/onboarding/state";

    /** The path a chosen link is sent to. */
    public static final String STEP_PATH = "/api/v1/onboarding/step";

    private final RuntimeClient runtime;
    private final OperatorGate gate;
    private final List<OnboardingAspect> aspects;
    private final int totalSteps;

    /**
     * @param runtime the runtime client
     * @param gate    the operator gate an answer is sent through
     */
    public OnboardingSource(RuntimeClient runtime, OperatorGate gate) {
        this(runtime, gate, List.of(OnboardingAspect.values()));
    }

    /**
     * @param runtime the runtime client
     * @param gate    the operator gate an answer is sent through
     * @param aspects the machine onboarding aspects
     */
    public OnboardingSource(RuntimeClient runtime, OperatorGate gate, List<OnboardingAspect> aspects) {
        this(runtime, gate, aspects, Objects.requireNonNull(aspects, "aspects").size());
    }

    /**
     * @param runtime    the runtime client
     * @param gate       the operator gate an answer is sent through
     * @param totalSteps the total steps of the onboarding run
     */
    OnboardingSource(RuntimeClient runtime, OperatorGate gate, int totalSteps) {
        this(runtime, gate, List.of(OnboardingAspect.values()), totalSteps);
    }

    private OnboardingSource(RuntimeClient runtime, OperatorGate gate, List<OnboardingAspect> aspects, int totalSteps) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.aspects = List.copyOf(Objects.requireNonNull(aspects, "aspects"));
        this.totalSteps = totalSteps;
    }

    /**
     * @return the machine onboarding aspects
     */
    List<OnboardingAspect> aspects() {
        return aspects;
    }

    @Override
    public StateDialogue.Frame fetch() throws IOException {
        var response = runtime.send(HttpRequest.get(STATE_PATH));
        if (response.status() != 200) {
            throw new IOException("the onboarding state answered HTTP " + response.status());
        }
        return StateDialogue.fromJson(response.bodyText(), totalSteps, false);
    }

    @Override
    public StateDialogue.Frame choose(String linkId) throws IOException {
        gate.verifyAct();
        var response = runtime.send(HttpRequest.json("POST", STEP_PATH,
                            JsonTree.write(Map.of("link", linkId))));
        if (response.status() != 200) {
            throw new IOException("the onboarding step answered HTTP " + response.status());
        }
        return StateDialogue.fromJson(response.bodyText(), totalSteps, false);
    }
}
