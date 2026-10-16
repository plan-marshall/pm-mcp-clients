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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Captures what only the operator's terminal client can see: terminal locale, SSH session detection,
 * login {@code PATH}, and sanitized non-secret build environment variables (PM-ARCH-1, PM-SEC-5).
 *
 * @param locale           the terminal locale (sent as {@code Accept-Language})
 * @param sshSession       {@code true} if running in an SSH session ({@code SSH_CONNECTION} or {@code SSH_TTY})
 * @param loginPath        the entries of the login {@code PATH}
 * @param buildEnvironment safe non-secret build environment variables for job provenance
 */
public record ClientEnvironment(
String locale,
boolean sshSession,
List<String> loginPath,
Map<String, String> buildEnvironment) {

    /** Default locale when none is specified. */
    public static final String DEFAULT_LOCALE = "en";

    private static final Set<String> EXCLUDED_CREDENTIAL_NAMES = Set.of(
                        "GH_TOKEN", "GITHUB_TOKEN", "GITLAB_TOKEN", "GH_ENTERPRISE_TOKEN", "GITLAB_PRIVATE_TOKEN",
                        "GIT_ASKPASS", "SSH_ASKPASS", "GIT_CONFIG_PARAMETERS", "GIT_CONFIG_COUNT",
                        "ANTHROPIC_API_KEY", "CODEX_API_KEY", "OPENAI_API_KEY", "GEMINI_API_KEY");

    private static final Set<String> EXCLUDED_SYSTEM_VARS = Set.of(
                        "DBUS_SESSION_BUS_ADDRESS", "XDG_RUNTIME_DIR", "MVND_DAEMON_STORAGE");

    private static final Set<String> EXCLUDED_FLAG_INJECTING_VARS = Set.of(
                        "MAVEN_OPTS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS");

    private static final Set<String> EXCLUDED_AGENT_VARS = Set.of(
                        "CLAUDE_CODE", "CLAUDE_PROJECT_ROOT", "CLAUDECODE", "CLAUDE_CODE_SESSION_ID", "CLAUDE_PID",
                        "ANTIGRAVITY_AGENT", "ANTIGRAVITY_CONVERSATION_ID", "GEMINI_CLI",
                        "OPENCODE", "OPENCODE_PID", "OPENCODE_CLIENT",
                        "AI_AGENT");

    private static final Set<String> EXCLUDED_PM_INTERNAL_VARS = Set.of(
                        "PM_MCP_JOB_TOKEN", "PM_MCP_JOB_ID", "PM_MCP_TOKEN");

    /**
     * Compact constructor creating immutable copies.
     */
    public ClientEnvironment {
        Objects.requireNonNull(locale, "locale");
        loginPath = List.copyOf(Objects.requireNonNull(loginPath, "loginPath"));
        buildEnvironment = Map.copyOf(Objects.requireNonNull(buildEnvironment, "buildEnvironment"));
    }

    /**
     * Captures the client environment from the process environment.
     *
     * @param environment the process environment
     * @return the captured client environment
     */
    public static ClientEnvironment capture(Map<String, String> environment) {
        var locale = resolveLocale(environment);
        var ssh = environment.containsKey("SSH_CONNECTION") || environment.containsKey("SSH_TTY");
        var rawPath = environment.get("PATH");
        var pathList = rawPath == null || rawPath.isBlank()
                            ? List.<String>of()
                            : Arrays.stream(rawPath.split(File.pathSeparator))
                            .filter(p -> !p.isBlank())
                            .toList();

        var buildEnv = new LinkedHashMap<String, String>();
        for (var entry : environment.entrySet()) {
            var key = entry.getKey();
            if (!isExcludedFromBuildEnvironment(key)) {
                buildEnv.put(key, entry.getValue());
            }
        }

        return new ClientEnvironment(locale, ssh, pathList, buildEnv);
    }

    /**
     * Resolves the locale from {@code LC_ALL}, {@code LC_MESSAGES}, and {@code LANG} in order.
     *
     * @param environment the environment map
     * @return language tag representation (e.g., "de-DE", "en")
     */
    public static String resolveLocale(Map<String, String> environment) {
        var raw = environment.get("LC_ALL");
        if (raw == null || raw.isBlank()) {
            raw = environment.get("LC_MESSAGES");
        }
        if (raw == null || raw.isBlank()) {
            raw = environment.get("LANG");
        }
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return formatLocale(raw);
    }

    /**
     * Formats a POSIX locale into an RFC-style language tag suitable for {@code Accept-Language}.
     *
     * @param raw the raw locale string
     * @return formatted language tag
     */
    static String formatLocale(String raw) {
        var trimmed = raw.trim();
        var dot = trimmed.indexOf('.');
        if (dot >= 0) {
            trimmed = trimmed.substring(0, dot);
        }
        var at = trimmed.indexOf('@');
        if (at >= 0) {
            trimmed = trimmed.substring(0, at);
        }
        trimmed = trimmed.replace('_', '-');
        if ("C".equalsIgnoreCase(trimmed) || "POSIX".equalsIgnoreCase(trimmed) || trimmed.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return trimmed;
    }

    /**
     * Checks whether an environment variable name matches any sensitive or prohibited patterns.
     *
     * @param name the environment variable name
     * @return {@code true} if the variable must not be captured in build environment
     */
    public static boolean isExcludedFromBuildEnvironment(String name) {
        var upper = name.toUpperCase(Locale.ROOT);
        if (upper.endsWith("_TOKEN") || upper.endsWith("_PASSWORD") || upper.endsWith("_SECRET")
                            || upper.endsWith("_KEY") || upper.endsWith("_AUTH")) {
            return true;
        }
        if (upper.startsWith("CODEX_")) {
            return true;
        }
        return EXCLUDED_CREDENTIAL_NAMES.contains(name)
                            || EXCLUDED_SYSTEM_VARS.contains(name)
                            || EXCLUDED_FLAG_INJECTING_VARS.contains(name)
                            || EXCLUDED_AGENT_VARS.contains(name)
                            || EXCLUDED_PM_INTERNAL_VARS.contains(name);
    }
}
