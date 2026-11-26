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

import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorItem;

/**
 * The one mapping from a wire {@code blocks} value to the decision's own words, shared by every
 * terminal reader of it (`needs-and-find.adoc`, What It Blocks; `texts-and-languages.adoc`):
 * {@code plan}, {@code epic}, {@code setup}, {@code entry} and {@code task} word themselves, the
 * wire value {@code nothing} is shown through {@code decision.blocks.none}, and a value outside
 * the vocabulary shows the client's unknown word — never a raw key and never a blocking claim.
 */
public final class DecisionBlockWords {

    private static final String PREFIX = "decision.blocks.";

    private static final String NOTHING_KEY = PREFIX + "none";

    private static final String UNKNOWN_KEY = "common.unknown";

    private DecisionBlockWords() {
    }

    /**
     * @param wire the wire value of {@code blocks}, or {@code null} where a legacy payload names none
     * @return the catalogue key of the decision's word for it
     */
    private static String keyOf(@Nullable String wire) {
        var value = wire == null ? "" : wire.trim().toLowerCase(Locale.ROOT);
        if ("nothing".equals(value)) {
            return NOTHING_KEY;
        }
        return NeedsOperatorItem.BLOCKS_VALUES.contains(value) ? PREFIX + value : UNKNOWN_KEY;
    }

    /**
     * @param locale the interface locale
     * @param wire   the wire value of {@code blocks}, or {@code null} where a legacy payload names none
     * @return the word the terminal shows for it
     */
    public static String text(String locale, @Nullable String wire) {
        return MessageCatalogue.format(locale, keyOf(wire), Map.of());
    }
}
