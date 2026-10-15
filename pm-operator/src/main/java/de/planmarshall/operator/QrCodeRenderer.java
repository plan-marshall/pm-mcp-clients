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

import io.nayuki.qrcodegen.QrCode;
import java.util.List;
import java.util.Objects;

/**
 * Renders a QR code to the terminal using Unicode half-block characters ({@code ▀}, {@code ▄}, {@code █}, {@code  }).
 * Each terminal line renders two vertical modules, matching the normative terminal transcripts (05-transcripts-entry.adoc).
 */
public final class QrCodeRenderer {

    private static final int BORDER_MODULES = 2;

    private QrCodeRenderer() {
    }

    /**
     * Renders a QR code for the given text with optional side information lines.
     *
     * @param text      the URL or text to encode
     * @param sideLines lines to print to the right of the QR code, or {@code null}
     * @return the rendered multi-line string
     */
    public static String render(String text, List<String> sideLines) {
        Objects.requireNonNull(text, "text");
        var qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        var size = qr.size;
        var sb = new StringBuilder();

        int sideIndex = 0;
        int totalTerminalRows = (size + BORDER_MODULES * 2 + 1) / 2;
        int sideLinesCount = sideLines != null ? sideLines.size() : 0;
        int startSideRow = Math.max(1, (totalTerminalRows - sideLinesCount) / 2);

        int currentRow = 0;
        for (int y = -BORDER_MODULES; y < size + BORDER_MODULES; y += 2) {
            sb.append("  ");
            for (int x = -BORDER_MODULES; x < size + BORDER_MODULES; x++) {
                boolean top = isModuleDark(qr, x, y, size);
                boolean bottom = isModuleDark(qr, x, y + 1, size);

                if (top && bottom) {
                    sb.append('█');
                } else if (top) {
                    sb.append('▀');
                } else if (bottom) {
                    sb.append('▄');
                } else {
                    sb.append(' ');
                }
            }

            if (sideLines != null && sideIndex < sideLines.size() && currentRow >= startSideRow) {
                sb.append("      ").append(sideLines.get(sideIndex));
                sideIndex++;
            }
            sb.append('\n');
            currentRow++;
        }

        if (sideLines != null) {
            while (sideIndex < sideLines.size()) {
                sb.append("  ");
                sb.append(" ".repeat(size + BORDER_MODULES * 2));
                sb.append("      ").append(sideLines.get(sideIndex));
                sb.append('\n');
                sideIndex++;
            }
        }

        return sb.toString();
    }

    private static boolean isModuleDark(QrCode qr, int x, int y, int size) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return false;
        }
        return qr.getModule(x, y);
    }
}
