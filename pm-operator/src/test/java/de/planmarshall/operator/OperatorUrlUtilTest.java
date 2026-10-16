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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperatorUrlUtil")
class OperatorUrlUtilTest {

    @Test
    @DisplayName("encodePathSegment handles empty and percent-encodes spaces with %20")
    void encodePathSegment() {
        assertEquals("", OperatorUrlUtil.encodePathSegment(""));
        assertEquals("simple", OperatorUrlUtil.encodePathSegment("simple"));
        assertEquals("hello%20world", OperatorUrlUtil.encodePathSegment("hello world"));
        assertEquals("a%20b%20c", OperatorUrlUtil.encodePathSegment("a b c"));
        assertEquals("plan%2F123", OperatorUrlUtil.encodePathSegment("plan/123"));
    }

    @Test
    @DisplayName("encodeQueryValue percent-encodes query values")
    void encodeQueryValue() {
        assertEquals("hello+world", OperatorUrlUtil.encodeQueryValue("hello world"));
        assertEquals("foo%26bar", OperatorUrlUtil.encodeQueryValue("foo&bar"));
    }

    @Test
    @DisplayName("appendScope correctly appends project and plan parameters")
    void appendScope() {
        assertEquals("/api/v1/items", OperatorUrlUtil.appendScope("/api/v1/items", null, null));
        assertEquals("/api/v1/items", OperatorUrlUtil.appendScope("/api/v1/items", "", ""));
        assertEquals("/api/v1/items", OperatorUrlUtil.appendScope("/api/v1/items", ".", ""));
        assertEquals("/api/v1/items?project=my-project",
                            OperatorUrlUtil.appendScope("/api/v1/items", "my-project", null));
        assertEquals("/api/v1/items?plan=my-plan",
                            OperatorUrlUtil.appendScope("/api/v1/items", null, "my-plan"));
        assertEquals("/api/v1/items?project=my-project&plan=my-plan",
                            OperatorUrlUtil.appendScope("/api/v1/items", "my-project", "my-plan"));
        assertEquals("/api/v1/items?filter=active&project=my-project&plan=my-plan",
                            OperatorUrlUtil.appendScope("/api/v1/items?filter=active", "my-project", "my-plan"));
    }
}
