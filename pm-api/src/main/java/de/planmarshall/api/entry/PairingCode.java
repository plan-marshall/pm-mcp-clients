/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * A pairing code with the grammar of the identifier registry
 * (03-web-server.adoc#web-pairing): eight characters of the alphabet
 * {@code BCDFGHJKLMNPQRSTVWXZ}, shown as {@code XXXX-XXXX} and entered without the dash, in any
 * case, and with spaces. The alphabet leaves out the letters that are easy to confuse when read
 * from a terminal.
 */
public record PairingCode(String value) {

    /** The alphabet of a pairing code. */
    public static final String ALPHABET = "BCDFGHJKLMNPQRSTVWXZ";

    /** The number of characters of a pairing code. */
    public static final int LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    public PairingCode {
        Objects.requireNonNull(value, "value");
        value = normalize(value);
        if (!isNormalizedValid(value)) {
            throw new IllegalArgumentException("Not a pairing code: " + value);
        }
    }

    /**
     * @return the code shown to the operator as {@code XXXX-XXXX}
     */
    public String display() {
        return value.substring(0, LENGTH / 2) + '-' + value.substring(LENGTH / 2);
    }

    /**
     * Normalizes an operator's input: upper case, without spaces and dashes.
     *
     * @param raw the input
     * @return the normalized code
     */
    public static String normalize(String raw) {
        Objects.requireNonNull(raw, "raw");
        var normalized = new StringBuilder(LENGTH);
        for (var character : raw.toCharArray()) {
            if (character == '-' || Character.isWhitespace(character)) {
                continue;
            }
            normalized.append(Character.toUpperCase(character));
        }
        return normalized.toString();
    }

    /**
     * @param raw the input, with or without the dash and in any case
     * @return {@code true} when the input is a pairing code
     */
    public static boolean isValid(String raw) {
        if (raw == null) {
            return false;
        }
        return isNormalizedValid(normalize(raw));
    }

    /**
     * Compares an operator's input with this code without regarding case, dashes, or spaces.
     *
     * @param raw the input
     * @return {@code true} when the input names this code
     */
    public boolean matches(String raw) {
        if (raw == null) {
            return false;
        }
        var normalized = normalize(raw);
        return MessageDigest.isEqual(
                value.getBytes(StandardCharsets.US_ASCII),
                normalized.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Generates a code from {@link SecureRandom}.
     *
     * @return a new pairing code
     */
    public static PairingCode generate() {
        var code = new char[LENGTH];
        for (var i = 0; i < LENGTH; i++) {
            code[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new PairingCode(new String(code));
    }

    private static boolean isNormalizedValid(String normalized) {
        if (normalized.length() != LENGTH) {
            return false;
        }
        for (var i = 0; i < normalized.length(); i++) {
            if (ALPHABET.indexOf(normalized.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return display();
    }
}
