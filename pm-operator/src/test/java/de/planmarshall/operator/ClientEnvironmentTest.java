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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ClientEnvironment")
class ClientEnvironmentTest {

    @Nested
    @DisplayName("locale resolution and formatting")
    class LocaleResolution {

        @Test
        @DisplayName("defaults to 'en' when no locale variable is present")
        void defaultsToEnglish() {
            assertEquals("en", ClientEnvironment.resolveLocale(Map.of()));
            assertEquals("en", ClientEnvironment.resolveLocale(Map.of("LC_ALL", "", "LANG", " ")));
        }

        @Test
        @DisplayName("normalizes 'C' and 'POSIX' to 'en'")
        void normalizesPosixToEnglish() {
            assertAll(
                          () -> assertEquals("en", ClientEnvironment.resolveLocale(Map.of("LC_ALL", "C"))),
                          () -> assertEquals("en", ClientEnvironment.resolveLocale(Map.of("LANG", "POSIX"))),
                          () -> assertEquals("en", ClientEnvironment.resolveLocale(Map.of("LANG", "C.UTF-8")))
            );
        }

        @Test
        @DisplayName("formats POSIX locale string to RFC language tag")
        void formatsLanguageTag() {
            assertAll(
                          () -> assertEquals("de-DE", ClientEnvironment.resolveLocale(Map.of("LC_ALL", "de_DE.UTF-8"))),
                          () -> assertEquals("fr-CA", ClientEnvironment.resolveLocale(Map.of("LC_ALL", "fr_CA.ISO8859-1"))),
                          () -> assertEquals("es-ES", ClientEnvironment.resolveLocale(Map.of("LC_ALL", "es_ES@euro"))),
                          () -> assertEquals("ja", ClientEnvironment.resolveLocale(Map.of("LC_ALL", "ja")))
            );
        }

        @Test
        @DisplayName("observes priority: LC_ALL -> LC_MESSAGES -> LANG")
        void observesPriority() {
            assertAll(
                          () -> assertEquals("de-DE", ClientEnvironment.resolveLocale(Map.of(
                                        "LC_ALL", "de_DE.UTF-8", "LC_MESSAGES", "fr_FR", "LANG", "en_US"))),
                          () -> assertEquals("fr-FR", ClientEnvironment.resolveLocale(Map.of(
                                        "LC_MESSAGES", "fr_FR", "LANG", "en_US"))),
                          () -> assertEquals("en-US", ClientEnvironment.resolveLocale(Map.of(
                                        "LANG", "en_US")))
            );
        }
    }

    @Nested
    @DisplayName("SSH session detection")
    class SshDetection {

        @Test
        @DisplayName("detects SSH session via SSH_CONNECTION or SSH_TTY")
        void detectsSshSession() {
            assertAll(
                          () -> assertTrue(ClientEnvironment.capture(Map.of("SSH_CONNECTION", "10.0.0.1 22")).sshSession()),
                          () -> assertTrue(ClientEnvironment.capture(Map.of("SSH_TTY", "/dev/pts/1")).sshSession()),
                          () -> assertFalse(ClientEnvironment.capture(Map.of("USER", "test")).sshSession())
            );
        }
    }

    @Nested
    @DisplayName("login PATH capture")
    class PathCapture {

        @Test
        @DisplayName("splits PATH by path separator and preserves order")
        void splitsPath() {
            var sep = File.pathSeparator;
            var pathStr = "/usr/bin" + sep + "/usr/local/bin" + sep + "" + sep + "/bin";
            var env = ClientEnvironment.capture(Map.of("PATH", pathStr));

            assertEquals(List.of("/usr/bin", "/usr/local/bin", "/bin"), env.loginPath());
        }

        @Test
        @DisplayName("returns empty list when PATH is missing or blank")
        void emptyPath() {
            assertAll(
                          () -> assertTrue(ClientEnvironment.capture(Map.of()).loginPath().isEmpty()),
                          () -> assertTrue(ClientEnvironment.capture(Map.of("PATH", "  ")).loginPath().isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("build environment sanitization")
    class BuildEnvSanitization {

        @ParameterizedTest
        @ValueSource(strings = {
                      "SECRET_TOKEN", "GITHUB_TOKEN", "APP_PASSWORD", "CLIENT_SECRET", "API_KEY", "USER_AUTH",
                      "GH_TOKEN", "GH_ENTERPRISE_TOKEN", "GITLAB_PRIVATE_TOKEN", "GIT_ASKPASS", "SSH_ASKPASS",
                      "GIT_CONFIG_PARAMETERS", "GIT_CONFIG_COUNT", "ANTHROPIC_API_KEY", "CODEX_API_KEY",
                      "DBUS_SESSION_BUS_ADDRESS", "XDG_RUNTIME_DIR", "MVND_DAEMON_STORAGE",
                      "MAVEN_OPTS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS",
                      "CLAUDE_CODE", "CLAUDE_PROJECT_ROOT", "CLAUDECODE", "ANTIGRAVITY_AGENT", "OPENCODE", "CODEX_SANDBOX",
                      "AI_AGENT", "PM_MCP_JOB_TOKEN", "PM_MCP_TOKEN"
        })
        @DisplayName("filters out sensitive, system, flag-injecting, agent, and token variables")
        void filtersSensitiveVariables(String name) {
            assertTrue(ClientEnvironment.isExcludedFromBuildEnvironment(name));

            var captured = ClientEnvironment.capture(Map.of(name, "secret_value", "SAFE_VAR", "safe_value"));
            assertAll(
                          () -> assertFalse(captured.buildEnvironment().containsKey(name)),
                          () -> assertEquals("safe_value", captured.buildEnvironment().get("SAFE_VAR"))
            );
        }

        @Test
        @DisplayName("preserves valid build variables like JAVA_HOME and DOCKER_HOST")
        void preservesSafeVariables() {
            var env = Map.of(
                          "JAVA_HOME", "/opt/java/jdk-25",
                          "GRADLE_USER_HOME", "/home/user/.gradle",
                          "DOCKER_HOST", "unix:///var/run/docker.sock",
                          "CUSTOM_FLAG", "enabled"
            );
            var captured = ClientEnvironment.capture(env);

            assertEquals(env, captured.buildEnvironment());
        }
    }
}
