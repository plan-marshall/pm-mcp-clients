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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QrCodeRenderer")
class QrCodeRendererTest {

    @Test
    @DisplayName("rejects null text")
    void rejectsNullText() {
        assertThrows(NullPointerException.class, () -> QrCodeRenderer.render(null, null));
    }

    @Test
    @DisplayName("renders QR code with Unicode half-block characters")
    void rendersUnicodeHalfBlocks() {
        var output = QrCodeRenderer.render("http://127.0.0.1:7420/pair#code=KXQB-MRTN", null);

        assertNotNull(output);
        assertFalse(output.isBlank());
        assertAll(
                            () -> assertTrue(output.contains("▄") || output.contains("█") || output.contains("▀"),
                                                "Output should contain Unicode block characters"),
                            () -> assertTrue(output.lines().count() > 10,
                                                "QR code should span multiple terminal lines")
        );
    }

    @Test
    @DisplayName("renders QR code with side lines")
    void rendersWithSideLines() {
        var sideLines = List.of(
                            "http://127.0.0.1:7420/pair#code=TBZC-HQPL",
                            "Code TBZC-HQPL, valid until 14:35"
        );
        var output = QrCodeRenderer.render("http://127.0.0.1:7420/pair#code=TBZC-HQPL", sideLines);

        assertAll(
                            () -> assertTrue(output.contains("http://127.0.0.1:7420/pair#code=TBZC-HQPL")),
                            () -> assertTrue(output.contains("Code TBZC-HQPL, valid until 14:35"))
        );
    }
}
