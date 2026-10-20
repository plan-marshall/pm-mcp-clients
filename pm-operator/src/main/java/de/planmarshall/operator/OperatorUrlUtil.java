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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;

/**
 * Utility methods for URL encoding and safe path/query construction across operator commands.
 */
public final class OperatorUrlUtil {

    private OperatorUrlUtil() {
    }

    /**
     * Percent-encodes a path segment for HTTP requests.
     * Unlike {@link URLEncoder#encode(String, java.nio.charset.Charset)}, spaces are encoded as
     * {@code %20} rather than {@code +}.
     *
     * @param segment the raw path segment
     * @return the percent-encoded path segment
     */
    public static String encodePathSegment(String segment) {
        if (segment.isEmpty()) {
            return "";
        }
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Percent-encodes a query parameter value.
     *
     * @param value the query parameter value
     * @return the encoded query value
     */
    static String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Appends project and/or plan scope parameters to a base URL path.
     *
     * @param basePath the base request path
     * @param project  optional project identifier
     * @param plan     optional plan identifier
     * @return the combined URL path with encoded query parameters
     */
    static String appendScope(String basePath, @Nullable String project, @Nullable String plan) {
        var sb = new StringBuilder(basePath);
        var hasQuery = basePath.contains("?");
        if (project != null && !project.isBlank() && !".".equals(project)) {
            sb.append(hasQuery ? '&' : '?').append("project=").append(encodeQueryValue(project));
            hasQuery = true;
        }
        if (plan != null && !plan.isBlank()) {
            sb.append(hasQuery ? '&' : '?').append("plan=").append(encodeQueryValue(plan));
        }
        return sb.toString();
    }
}
