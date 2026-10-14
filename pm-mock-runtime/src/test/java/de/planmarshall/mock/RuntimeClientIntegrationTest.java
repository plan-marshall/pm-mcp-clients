/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.runtime.RuntimeTokenFile;

@DisplayName("RuntimeClient Integration")
@EnabledOnOs({OS.MAC, OS.LINUX})
class RuntimeClientIntegrationTest {

    @Test
    @DisplayName("RuntimeClient seamlessly connects and interacts with MockRuntime over Unix domain socket")
    void integratesWithOfficialRuntimeClient() throws Exception {
        try (var runtime = MockRuntime.start("onboarding-happy")) {
            var uid = (Integer) Files.getAttribute(runtime.paths().runtimeToken(), "unix:uid", LinkOption.NOFOLLOW_LINKS);
            var tokenFile = new RuntimeTokenFile(runtime.paths(), uid);
            var client = RuntimeClient.local(runtime.paths(), tokenFile, null, Map.of("User-Agent", "pm-operator/0.1.0-mock-test"));

            // 1. Send status request via RuntimeClient
            var statusRes = client.send(HttpRequest.get("/api/v1/status"));
            assertEquals(200, statusRes.status());
            var statusBody = JsonTree.asObject(JsonTree.parse(statusRes.body()));
            assertNotNull(statusBody);
            assertEquals("onboarding", JsonTree.string(statusBody, "status"));

            // 2. Perform state transition via RuntimeClient (follow link 'begin')
            var beginRes = client.send(HttpRequest.json("POST", "/api/v1/operator/onboard/begin", "{}"));
            assertEquals(200, beginRes.status());
            var beginBody = JsonTree.asObject(JsonTree.parse(beginRes.body()));
            assertNotNull(beginBody);
            assertEquals("language.choose", JsonTree.string(beginBody, "state"));

            // 3. Verify MockRuntime recorded incoming requests with user-agent
            var recorded = runtime.requests();
            assertTrue(recorded.size() >= 2);
            assertEquals("pm-operator/0.1.0-mock-test", recorded.getFirst().header("User-Agent"));
        }
    }
}
