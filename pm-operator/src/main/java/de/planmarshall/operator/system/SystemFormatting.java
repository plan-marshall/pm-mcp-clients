/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.system;

import java.util.Locale;

/**
 * The figures of the system view, formatted once for the printed view
 * ({@code SystemCommand}) and the full-screen view ({@link SystemLevel}).
 *
 * <p>The byte and token figures are the ones {@code doc/design/terminal-client/25-system.adoc#print}
 * shows; the unit abbreviations are symbols and read the same in every language.
 */
public final class SystemFormatting {

    private SystemFormatting() {
    }

    /**
     * @param bytes a size in bytes
     * @return the size with its largest unit, rounded as the printed view rounds it
     */
    public static String bytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return Math.round(kb) + " KB";
        }
        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return Math.round(mb) + " MB";
        }
        double gb = mb / 1024.0;
        if (gb >= 10.0) {
            return Math.round(gb) + " GB";
        }
        double rounded = Math.round(gb * 10.0) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return (long) rounded + " GB";
        }
        return String.format(Locale.ROOT, "%.1f GB", rounded);
    }

    /**
     * @param seconds an uptime in seconds
     * @return the uptime as {@code 3 d 4 h}, {@code 6 h 12 min}, or {@code 12 min}
     */
    public static String uptime(long seconds) {
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        if (days > 0) {
            return days + " d " + hours + " h";
        }
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        return minutes + " min";
    }
}
