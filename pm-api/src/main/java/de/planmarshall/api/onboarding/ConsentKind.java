/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import org.jspecify.annotations.Nullable;

/**
 * The closed list of consent kinds of the use cases (UC-ONB-1..10, consents table): every action
 * that looks at the machine or acts on it is preceded by a consent state of its kind.
 *
 * <p>The title of a kind is the name of the action in lists and remembered lines; whether it can
 * be remembered and what <em>no</em> does are part of the kind.
 */
public enum ConsentKind {

    /** Look for the executables of the harnesses the profiles know, and run their version probe. */
    SCAN_HARNESSES("scan_harnesses", true, NoBehavior.STAYS),

    /** Download the makers' icons of the found harnesses once, through the runtime. */
    FETCH_ICONS("fetch_icons", true, NoBehavior.GOES_ON),

    /** Run the login probe of each chosen harness. */
    PROBE_HARNESS_LOGIN("probe_harness_login", true, NoBehavior.STAYS),

    /** Run an unknown harness to verify its draft profile. */
    VERIFY_HARNESS_PROFILE("verify_harness_profile", false, NoBehavior.STAYS),

    /** Run the model probe of each enrolled harness. */
    PROBE_HARNESS_MODELS("probe_harness_models", true, NoBehavior.STAYS),

    /** A task of the steward on the default harness, with its expected tokens. */
    MODEL_ADVICE("model_advice", true, NoBehavior.GOES_ON),

    /** Write, read, and delete one test entry in the chosen OS credential store. */
    CHECK_CREDENTIAL_STORE("check_credential_store", true, NoBehavior.STAYS),

    /** Look for the CLI tools of the providers and the credentials already stored. */
    SCAN_PROVIDERS("scan_providers", true, NoBehavior.STAYS),

    /** Call each chosen provider with its credential, and run its CLI tool. */
    VERIFY_PROVIDERS("verify_providers", true, NoBehavior.STAYS),

    /** Read a project's checkout and run its host tools' version probes. */
    INSPECT_CHECKOUT("inspect_checkout", true, NoBehavior.STAYS),

    /** Run the named scripts of a project's checkout, each with its arguments. */
    RUN_CHECKOUT_SCRIPTS("run_checkout_scripts", true, NoBehavior.GOES_ON),

    /** A role task whose expected tokens are shown first. */
    MODEL_COST("model_cost", true, NoBehavior.GOES_ON),

    /** Ask the organisation's registry for a newer release of this machine's platform. */
    CHECK_UPDATES("check_updates", true, NoBehavior.GOES_ON),

    /** The automatic run: every consent of the steps it needs, listed in full, for this run only. */
    AUTO_RUN("auto_run", false, NoBehavior.BACK_TO_MODE);

    /** What <em>no</em> does with a consent of the kind. */
    public enum NoBehavior {

        /** For a required action the state stays and nothing was done. */
        STAYS("stays"),

        /** For an optional action the process goes on without it. */
        GOES_ON("goes_on"),

        /** The automatic run's <em>no</em> goes back to {@code onboard.mode}. */
        BACK_TO_MODE("back_to_mode");

        private final String wireValue;

        NoBehavior(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the wire value
         */
        public String wireValue() {
            return wireValue;
        }
    }

    private final String wireValue;
    private final boolean rememberable;
    private final NoBehavior noBehavior;

    ConsentKind(String wireValue, boolean rememberable, NoBehavior noBehavior) {
        this.wireValue = wireValue;
        this.rememberable = rememberable;
        this.noBehavior = noBehavior;
    }

    /**
     * @return the wire value of the kind, e.g. {@code scan_harnesses}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the consent can be remembered ({@code yes, and don't ask again})
     */
    public boolean rememberable() {
        return rememberable;
    }

    /**
     * @return what <em>no</em> does with a consent of the kind
     */
    public NoBehavior noBehavior() {
        return noBehavior;
    }

    /**
     * @return the message key of the kind's title in lists and remembered lines
     */
    public String titleKey() {
        return this == AUTO_RUN ? "onboard.auto.consent.title" : "common.consent.kind." + wireValue;
    }

    /**
     * Resolves a consent kind from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code scan_harnesses}, or enum name
     * @return the kind, or {@code null} if unmatched
     */
    public static @Nullable ConsentKind fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var kind : values()) {
            if (kind.wireValue.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Resolves a consent kind from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the kind
     * @throws IllegalArgumentException if the value is unknown
     */
    public static ConsentKind parse(String value) {
        var kind = fromWire(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown ConsentKind: " + value);
        }
        return kind;
    }
}
