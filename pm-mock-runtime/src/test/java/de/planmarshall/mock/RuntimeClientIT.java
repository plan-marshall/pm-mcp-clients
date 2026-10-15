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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.junit5.EnableMockRuntime;

@DisplayName("RuntimeClient Integration")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableMockRuntime
class RuntimeClientIT {

    @Test
    @DisplayName("RuntimeClient seamlessly connects and interacts with MockRuntime over Unix domain socket")
    @EnableMockRuntime(scenario = StandardScenarios.ONBOARDING_HAPPY)
    void integratesWithOfficialRuntimeClient(MockRuntime runtime) throws Exception {
        var client = runtime.client(Map.of("User-Agent", "pm-operator/0.1.0-mock-test"));

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

    @Test
    @DisplayName("serves message catalogue endpoints without authentication")
    @EnableMockRuntime(scenario = StandardScenarios.STATUS_ONLY, webPort = 0)
    void servesMessageCatalogueWithoutAuth(URI loopbackUrl) throws Exception {
        var client = HttpClient.newHttpClient();

        // 1. GET /api/v1/messages
        var metaReq = java.net.http.HttpRequest.newBuilder()
                .uri(loopbackUrl.resolve("/api/v1/messages"))
                .GET()
                .build();
        var metaRes = client.send(metaReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, metaRes.statusCode());
        assertTrue(metaRes.body().contains("locales"));
        assertTrue(metaRes.body().contains("areas"));

        // 2. GET /api/v1/messages/en/common
        var msgReq = java.net.http.HttpRequest.newBuilder()
                .uri(loopbackUrl.resolve("/api/v1/messages/en/common"))
                .GET()
                .build();
        var msgRes = client.send(msgReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, msgRes.statusCode());
        assertTrue(msgRes.body().contains("web.common.copy"));
        assertTrue(msgRes.body().contains("Copy"));

        // 3. GET /api/v1/messages/de/all
        var deReq = java.net.http.HttpRequest.newBuilder()
                .uri(loopbackUrl.resolve("/api/v1/messages/de/all"))
                .GET()
                .build();
        var deRes = client.send(deReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, deRes.statusCode());
        assertTrue(deRes.body().contains("Kopiere"));
    }
}
