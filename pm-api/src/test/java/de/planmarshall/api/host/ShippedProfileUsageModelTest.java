/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.host;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.planmarshall.api.json.JsonTree;

/**
 * The {@code headless.usage.model} member of the shipped client profiles (Client Profiles
 * Specification, the {@code usage} object of the shipped declarations): the required RFC 6901 pointer
 * into the harness's turn output that names the answering model, or {@code null} where the profile
 * declares that the harness reports no model.
 * <p>
 * The documents are read through the shipped catalog and {@code profiles/*.json}, as the
 * {@link ProfileDocumentReader} reads them, and parsed with the streaming API of {@code jackson-core}
 * only ({@link JsonTree}). The table's values are the expectations of the contract and stay literal.
 */
@DisplayName("The shipped profiles' headless usage model")
class ShippedProfileUsageModelTest {

    /** One row of the contract table: a shipped profile and the usage model it must declare. */
    private record ExpectedModel(String clientId, String model) {
    }

    private static final List<ExpectedModel> EXPECTED_MODELS = List.of(
            new ExpectedModel("claude", "/message/model"),
            new ExpectedModel("antigravity", null),
            new ExpectedModel("codex", null),
            new ExpectedModel("neutral", null),
            new ExpectedModel("opencode", null));

    /** The longest pointer the schema accepts. */
    private static final int MAX_POINTER_LENGTH = 256;

    static Stream<Arguments> expectedModels() {
        return EXPECTED_MODELS.stream().map(row -> Arguments.of(row.clientId(), row.model()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expectedModels")
    @DisplayName("declares the usage model of the contract table")
    void declaresTheUsageModel(String clientId, String expected) throws Exception {
        var usage = usageOf(clientId);

        assertTrue(usage.containsKey("model"), clientId + " declares headless.usage.model");
        var model = usage.get("model");
        assertEquals(expected, model, clientId + " headless.usage.model");
        if (model instanceof String pointer) {
            assertAll(
                    () -> assertTrue(pointer.startsWith("/"), clientId + " usage model is an RFC 6901 pointer"),
                    () -> assertTrue(pointer.length() <= MAX_POINTER_LENGTH,
                            clientId + " usage model is at most " + MAX_POINTER_LENGTH + " characters"));
        }
    }

    /**
     * Pins the table to the shipped catalog, so a profile added to {@code profiles.index} cannot keep a
     * {@code usage} object without a {@code model} member while this test stays green. What it adds
     * beyond the catalog-versus-directory parity of {@code ProfileDocumentReaderTest} is the
     * table-versus-catalog link: the per-profile assertions above cover exactly the cataloged profiles.
     */
    @Test
    @DisplayName("covers every shipped profile, every one and no other")
    void coversEveryShippedProfile() {
        var table = EXPECTED_MODELS.stream().map(ExpectedModel::clientId).collect(Collectors.toSet());

        assertEquals(Set.copyOf(HostDetection.clientIds()), table);
    }

    private static Map<?, ?> usageOf(String clientId) throws IOException {
        var resource = "profiles/" + clientId + ".json";
        Map<?, ?> document;
        try (InputStream input = ShippedProfileUsageModelTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "the shipped profiles directory holds no " + resource);
            document = object(JsonTree.parse(input.readAllBytes()), resource);
        }
        var headless = object(document.get("headless"), resource + " headless");
        return object(headless.get("usage"), resource + " headless.usage");
    }

    private static Map<?, ?> object(Object value, String what) {
        assertInstanceOf(Map.class, value, what + " is a JSON object");
        return (Map<?, ?>) value;
    }
}
