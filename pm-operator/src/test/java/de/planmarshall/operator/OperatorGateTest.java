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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("OperatorGate")
class OperatorGateTest {

    private final Path home = Path.of("/tmp/test-home");

    private static String agentRefusal(String locale, String marker) {
        return MessageCatalogue.format(locale, OperatorGate.AGENT_REFUSED_KEY,
                            Map.of(OperatorGate.MARKER_PARAM, marker));
    }

    private static String terminalRefusal(String locale) {
        return MessageCatalogue.format(locale, OperatorGate.TERMINAL_REFUSED_KEY, Map.of());
    }

    @Nested
    @DisplayName("interactive terminal verification")
    class TerminalGate {

        @Test
        @DisplayName("passes when interactive terminal is attached and no agent signals exist")
        void passesInteractiveTerminal() {
            var context = new OperatorContext(Map.of(), home, true);
            var gate = new OperatorGate(context);

            gate.verify();
        }

        @Test
        @DisplayName("refuses with exit code 4 when terminal is non-interactive")
        void refusesNonInteractiveTerminal() {
            var context = new OperatorContext(Map.of(), home, false);
            var gate = new OperatorGate(context);

            var ex = assertThrows(OperatorSecurityException.class, gate::verify);
            assertAll(
                                () -> assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, ex.exitCode()),
                                () -> assertEquals(terminalRefusal("en"), ex.getMessage())
            );
        }
    }

    @Nested
    @DisplayName("coding-agent host detection and refusal (PM-SEC-3)")
    class HostDetectionGate {

        @ParameterizedTest
        @ValueSource(strings = {
                            "CLAUDE_CODE_SESSION_ID", "CLAUDECODE", "CLAUDE_CODE", "CLAUDE_PROJECT_ROOT",
                            "ANTIGRAVITY_AGENT", "GEMINI_CLI",
                            "OPENCODE", "OPENCODE_PID", "OPENCODE_CLIENT",
                            "CODEX_SANDBOX", "CODEX_CI", "CODEX_THREAD_ID", "CODEX_ENV"})
        @DisplayName("detects and refuses the marker of every host")
        void detectsEveryHostMarker(String varName) {
            var env = Map.of(varName, "1");
            var context = new OperatorContext(env, home, true);
            var gate = new OperatorGate(context);

            var ex = assertThrows(OperatorSecurityException.class, gate::verify);
            assertAll(
                                () -> assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, ex.exitCode()),
                                () -> assertEquals(agentRefusal("en", varName), ex.getMessage())
            );
        }

        @Test
        @DisplayName("detects and refuses unmapped AI agent signal")
        void detectsUnmappedAgent() {
            var env = Map.of("AI_AGENT", "generic");
            var context = new OperatorContext(env, home, true);
            var gate = new OperatorGate(context);

            var ex = assertThrows(OperatorSecurityException.class, gate::verify);
            assertAll(
                                () -> assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, ex.exitCode()),
                                () -> assertEquals(agentRefusal("en", "AI_AGENT"), ex.getMessage())
            );
        }

        @Test
        @DisplayName("the English refusal names the act, the marker, and the way back")
        void englishRefusalText() {
            var env = Map.of("CLAUDECODE", "1", "LANG", "en_US.UTF-8");
            var gate = new OperatorGate(new OperatorContext(env, home, true));

            var ex = assertThrows(OperatorSecurityException.class, gate::verify);

            assertEquals("Operator acts are refused here: this terminal belongs to a coding agent"
                                + " (CLAUDECODE is set). Run pm-operator in a terminal of your own.",
                                ex.getMessage());
        }

        @Test
        @DisplayName("the German refusal names the act, the marker, and the way back")
        void germanRefusalText() {
            var env = Map.of("CLAUDECODE", "1", "LANG", "de_DE.UTF-8");
            var gate = new OperatorGate(new OperatorContext(env, home, true));

            var ex = assertThrows(OperatorSecurityException.class, gate::verify);

            assertEquals("Operatorische Aktionen werden hier abgelehnt: Dieses Terminal gehört zu einem"
                                + " Coding-Agenten (CLAUDECODE ist gesetzt). Führe pm-operator in einem eigenen"
                                + " Terminal aus.", ex.getMessage());
        }

        @Test
        @DisplayName("applies fixed normative precedence: antigravity -> opencode -> claude -> codex")
        void appliesPrecedence() {
            assertAll(
                                () -> assertEquals("antigravity", OperatorGate.detectHost(Map.of(
                                                    "ANTIGRAVITY_AGENT", "1", "OPENCODE", "1", "CLAUDE_CODE", "1")).orElseThrow()),
                                () -> assertEquals("opencode", OperatorGate.detectHost(Map.of(
                                                    "OPENCODE", "1", "CLAUDE_CODE", "1")).orElseThrow()),
                                () -> assertEquals("claude", OperatorGate.detectHost(Map.of(
                                                    "CLAUDE_CODE", "1", "CODEX_CI", "1")).orElseThrow()),
                                () -> assertEquals("codex", OperatorGate.detectHost(Map.of(
                                                    "CODEX_CI", "1")).orElseThrow()),
                                () -> assertFalse(OperatorGate.detectHost(Map.of("SAFE_VAR", "1")).isPresent())
            );
        }
    }

    @Nested
    @DisplayName("CLI command execution integration")
    class CommandIntegration {

        @Test
        @DisplayName("web enable is refused when agent environment is present")
        void webEnableRefusedByAgent() {
            var out = new StringWriter();
            var err = new StringWriter();
            var context = new OperatorContext(Map.of("CLAUDE_CODE", "1"), home, true);

            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "web", "enable");

            assertAll(
                                () -> assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, code),
                                () -> assertTrue(err.toString().contains(agentRefusal("en", "CLAUDE_CODE")))
            );
        }

        @Test
        @DisplayName("web disable is refused when terminal is non-interactive")
        void webDisableRefusedByTerminal() {
            var out = new StringWriter();
            var err = new StringWriter();
            var context = new OperatorContext(Map.of(), home, false);

            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "web", "disable");

            assertAll(
                                () -> assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, code),
                                () -> assertTrue(err.toString().contains(terminalRefusal("en")))
            );
        }

        @Test
        @DisplayName("runtime stop is refused when agent environment is present")
        void runtimeStopRefusedByAgent() {
            var out = new StringWriter();
            var err = new StringWriter();
            var context = new OperatorContext(Map.of("OPENCODE", "1"), home, true);

            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "runtime", "stop");

            assertAll(
                                () -> assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, code),
                                () -> assertTrue(err.toString().contains(agentRefusal("en", "OPENCODE")))
            );
        }

        @Test
        @DisplayName("a read-only verb is not an operator act and ignores agent presence")
        void statusPermittedInAgentEnvironment() {
            var out = new StringWriter();
            var err = new StringWriter();
            var context = new OperatorContext(Map.of("CLAUDE_CODE", "1"), home, false);

            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "status");

            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertEquals("runtime   not running", out.toString().strip())
            );
        }
    }
}
