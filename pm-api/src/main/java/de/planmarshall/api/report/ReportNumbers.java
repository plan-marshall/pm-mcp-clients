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

import java.io.IOException;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;

/**
 * The count reader of the report records: a token count, a duration or a task count is a whole
 * number. A number with a fraction, or one beyond the range of a {@code long}, is refused with a
 * {@link JsonFormatException}; the reader never truncates it to a count the runtime did not write.
 */
@UtilityClass
class ReportNumbers {

    /**
     * Reads a whole number, or nothing.
     *
     * @param value the member value, may be {@code null}
     * @param field the member name for the refusal
     * @return the number, or {@code null} when the value is absent or no number
     * @throws IOException if the value is a number with a fraction or out of range
     */
    static @Nullable Long wholeOrNull(@Nullable Object value, String field) throws IOException {
        final String text;
        if (value instanceof JsonNumber(String raw)) {
            text = raw;
        } else if (value instanceof Number raw) {
            text = raw.toString();
        } else {
            return null;
        }
        try {
            return new BigDecimal(text).longValueExact();
        } catch (NumberFormatException | ArithmeticException _) {
            throw new JsonFormatException("Not a whole number for " + field + ": " + text);
        }
    }

    /**
     * Reads a whole number, zero when it is absent.
     *
     * @param value the member value, may be {@code null}
     * @param field the member name for the refusal
     * @return the number, or {@code 0} when the value is absent or no number
     * @throws IOException if the value is a number with a fraction or out of range
     */
    static long whole(@Nullable Object value, String field) throws IOException {
        var number = wholeOrNull(value, field);
        return number == null ? 0L : number;
    }
}
