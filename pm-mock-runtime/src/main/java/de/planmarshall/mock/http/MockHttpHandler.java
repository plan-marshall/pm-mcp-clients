/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.http;

import java.io.IOException;

/**
 * Request handler interface for {@link MockHttpServer}.
 */
@FunctionalInterface
public interface MockHttpHandler {

    /**
     * Handles one HTTP request.
     *
     * @param request  the request
     * @param response the response to write
     * @throws IOException on an I/O error
     */
    void handle(MockHttpRequest request, MockHttpResponse response) throws IOException;
}
