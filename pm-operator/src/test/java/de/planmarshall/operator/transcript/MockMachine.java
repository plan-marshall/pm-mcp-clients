/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import de.planmarshall.mock.MockRuntime;

/**
 * Machine-root helpers for a transcript test. The mock runtime records the instant it started its
 * filesystem, not the instant its process started, so a verb that checks the liveness of the
 * recorded runtime (as every operator verb does) would see a stale record. The test writes the
 * truthful record of the process that hosts the mock runtime, as the existing operator tests do.
 */
final class MockMachine {

    private MockMachine() {
    }

    /**
     * Records the running mock runtime with the start instant of the host process.
     *
     * @param runtime the started mock runtime
     * @throws IOException if the record cannot be written
     */
    static void recordLiveRuntime(MockRuntime runtime) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow();
        var record = "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}";
        Files.writeString(runtime.paths().runtimeRecord(), record, StandardCharsets.UTF_8);
    }
}
