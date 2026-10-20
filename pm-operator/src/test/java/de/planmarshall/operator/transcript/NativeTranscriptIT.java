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

import io.nayuki.qrcodegen.QrCode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.PmOperator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * The transcript dialogues against the native binary (PM-TEST-7). The binary is driven through a
 * pseudo terminal, because it has no test seam and checks for a real interactive terminal. The
 * test runs only when {@code target/pm-operator} was built with {@code -Pnative}; on the JVM the
 * in-process transcript tests cover the same transcripts.
 */
@DisplayName("pm-operator native transcript dialogues")
@EnabledOnOs({OS.MAC, OS.LINUX})
class NativeTranscriptIT {

    private static final String TERM = "TERM";
    private static final String PATH = "PATH";

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @Test
    @DisplayName("status matches the recorded transcript on the native binary")
    void statusMatchesRecordedTranscript() throws Exception {
        Assumptions.assumeTrue(binary.nativeImage(), "target/pm-operator is not built; run -Pnative package first");
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.RUNNING_WORK).build())) {
            MockMachine.recordLiveRuntime(runtime);

            var rendered = PseudoTerminal.run(binary.with("status"), environment(runtime),
                                Path.of("").toAbsolutePath());

            TranscriptAssert.assertMatches("status", rendered,
                                new TranscriptNormalizer().replacing(runtime.base(), "<machine-root>"));
        }
    }

    @Test
    @DisplayName("plan fix-relay matches the recorded transcript on the native binary")
    void planViewMatchesRecordedTranscript() throws Exception {
        Assumptions.assumeTrue(binary.nativeImage(), "target/pm-operator is not built; run -Pnative package first");
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.PLAN_VIEW).build())) {
            MockMachine.recordLiveRuntime(runtime);

            var rendered = PseudoTerminal.run(binary.with("plan", "fix-relay", "--all"),
                                environment(runtime), Path.of("").toAbsolutePath());

            TranscriptAssert.assertMatches("plan-view", rendered,
                                new TranscriptNormalizer().replacing(runtime.base(), "<machine-root>"));
        }
    }

    @Test
    @DisplayName("web pair matches the recorded transcript on the native binary")
    void webPairMatchesRecordedTranscript() throws Exception {
        Assumptions.assumeTrue(binary.nativeImage(), "target/pm-operator is not built; run -Pnative package first");
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.WEB_PAIRING).build())) {
            MockMachine.recordLiveRuntime(runtime);

            var rendered = PseudoTerminal.run(binary.with("web", "pair"), environment(runtime),
                                Path.of("").toAbsolutePath());

            TranscriptAssert.assertMatches("web-pair", rendered,
                                new TranscriptNormalizer().replacing(runtime.base(), "<machine-root>"));
        }
    }

    @Test
    @DisplayName("plan add --no-input matches the recorded transcript on the native binary")
    void startPlanMatchesRecordedTranscript() throws Exception {
        Assumptions.assumeTrue(binary.nativeImage(), "target/pm-operator is not built; run -Pnative package first");
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.START_SOURCES).build())) {
            MockMachine.recordLiveRuntime(runtime);

            var rendered = PseudoTerminal.run(binary.with("plan", "add", "--project", "pm-mcp-clients",
                                                    "--description", "Add a Codeberg provider", "--no-input"),
                                environment(runtime), Path.of("").toAbsolutePath());

            TranscriptAssert.assertMatches("start-plan-no-input", rendered,
                                new TranscriptNormalizer().replacing(runtime.base(), "<machine-root>"));
        }
    }

    private static Map<String, String> environment(MockRuntime runtime) {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "transcript-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put(TERM, "xterm-256color");
        environment.put(PATH, "/usr/bin:/bin");
        return environment;
    }
}
