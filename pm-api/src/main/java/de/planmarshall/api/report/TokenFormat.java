/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * Shared formatter for token counts across both terminal and web clients according to
 * {@code texts-and-languages.adoc#numbers}.
 */
public final class TokenFormat {

    private TokenFormat() {
    }

    /**
     * Formats a token count into a localized string (e.g. {@code "412"}, {@code "4.2 k"}, {@code "830 k"},
     * {@code "1.24 M"}), applying floor rounding and {@code "≥"} prefix when {@code floor} is {@code true}.
     *
     * @param count  the token count
     * @param floor  whether the count represents a floor (derived with unmeasured records)
     * @param locale the target locale (e.g. "en" or "de")
     * @return the formatted token count
     */
    public static String formatTokens(long count, boolean floor, @Nullable String locale) {
        var normLocale = MessageCatalogue.normalizeLocale(locale);
        var roundingMode = floor ? RoundingMode.DOWN : RoundingMode.HALF_UP;

        String formattedValue;
        if (count < 1_000) {
            formattedValue = Long.toString(count);
        } else if (count < 10_000) {
            var val = BigDecimal.valueOf(count)
                    .divide(BigDecimal.valueOf(1_000), 1, roundingMode);
            var numStr = formatDecimal(val, 1, normLocale);
            formattedValue = MessageCatalogue.format(normLocale, "common.tokens.thousands", Map.of("value", numStr));
        } else if (count < 1_000_000) {
            var val = BigDecimal.valueOf(count)
                    .divide(BigDecimal.valueOf(1_000), 0, roundingMode);
            var numStr = formatDecimal(val, 0, normLocale);
            formattedValue = MessageCatalogue.format(normLocale, "common.tokens.thousands", Map.of("value", numStr));
        } else {
            var val = BigDecimal.valueOf(count)
                    .divide(BigDecimal.valueOf(1_000_000), 2, roundingMode);
            var numStr = formatDecimal(val, 2, normLocale);
            formattedValue = MessageCatalogue.format(normLocale, "common.tokens.millions", Map.of("value", numStr));
        }

        if (floor) {
            return MessageCatalogue.format(normLocale, "common.tokens.at_least", Map.of("value", formattedValue));
        }
        return formattedValue;
    }

    /**
     * Formats total and in/out split (e.g. {@code "≥ 412 k (≥ 380 k in · ≥ 32 k out)"}).
     *
     * @param total the total tokens
     * @param in    the in tokens
     * @param out   the out tokens
     * @param floor whether total or parts are a floor
     * @param locale the target locale
     * @return formatted split string
     */
    public static String formatSplit(long total, long in, long out, boolean floor, @Nullable String locale) {
        var normLocale = MessageCatalogue.normalizeLocale(locale);
        var totalStr = formatTokens(total, floor, normLocale);
        var inStr = formatTokens(in, floor, normLocale);
        var outStr = formatTokens(out, floor, normLocale);

        if ("de".equals(normLocale)) {
            return totalStr + " (" + inStr + " rein · " + outStr + " raus)";
        }
        return totalStr + " (" + inStr + " in · " + outStr + " out)";
    }

    private static String formatDecimal(BigDecimal value, int fractionDigits, String locale) {
        var symbols = new DecimalFormatSymbols("de".equals(locale) ? Locale.GERMANY : Locale.ENGLISH);
        var df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setMinimumFractionDigits(fractionDigits);
        df.setMaximumFractionDigits(fractionDigits);
        df.setGroupingUsed(false);
        return df.format(value);
    }
}
