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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Discovers and probes the headless CLI of one of the client profiles the terminal client knows
 * (CLI &amp; Security § Harness Enrolment). It resolves the executable through the absolute entries
 * of the login {@code PATH}, never through a shell, computes its SHA-256 digest, and runs the
 * profile's version probe with a bounded timeout and a bounded output.
 * <p>
 * The probe reports only facts the operator sees before the enrolment is recorded; it captures no
 * environment value and never stores a harness credential.
 */
final class HarnessDiscovery {

    /** The client ids the terminal client knows how to discover. */
    static final List<String> SUPPORTED_CLIENTS = List.of("claude", "opencode", "codex", "antigravity");

    private static final Map<String, String> EXECUTABLES = Map.of(
                        "claude", "claude",
                        "opencode", "opencode",
                        "codex", "codex",
                        "antigravity", "antigravity");

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_OUTPUT_BYTES = 8 * 1024;
    private static final int MAX_VERSION_LENGTH = 128;
    private static final int BUFFER_SIZE = 1024;
    private static final Pattern VERSION = Pattern.compile("(\\d+(?:\\.\\d+)+)");

    private HarnessDiscovery() {
        // Utility class
    }

    /**
     * One discovered harness.
     *
     * @param client       the client id
     * @param executable   the resolved absolute executable path
     * @param version      the version the probe reported
     * @param sha256Digest the SHA-256 digest of the executable
     */
    record Harness(String client, Path executable, String version, String sha256Digest) {
    }

    /**
     * The executable of a client was not found on the absolute entries of {@code PATH}.
     */
    static final class HarnessNotFoundException extends IOException {
        HarnessNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * The version probe of a discovered executable failed.
     */
    static final class HarnessProbeException extends IOException {
        HarnessProbeException(String message) {
            super(message);
        }
    }

    /**
     * @param client the client id
     * @return whether the terminal client can discover that client
     */
    static boolean isSupported(String client) {
        return SUPPORTED_CLIENTS.contains(client);
    }

    /**
     * Resolves and probes one client's executable.
     *
     * @param client      the client id, one of {@link #SUPPORTED_CLIENTS}
     * @param environment the process environment, read for {@code PATH} only
     * @return the discovered harness
     * @throws HarnessNotFoundException if no absolute {@code PATH} entry holds the executable
     * @throws HarnessProbeException    if the version probe fails or reports no version
     * @throws IOException              on another read failure
     */
    static Harness discover(String client, Map<String, String> environment) throws IOException {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(environment, "environment");
        if (!isSupported(client)) {
            throw new IllegalArgumentException("Unknown harness client: " + client);
        }
        var executable = resolveExecutable(EXECUTABLES.get(client), environment);
        return new Harness(client, executable, probeVersion(executable), sha256(executable));
    }

    private static Path resolveExecutable(String name, Map<String, String> environment)
                        throws HarnessNotFoundException {
        var path = environment.get("PATH");
        if (path == null || path.isBlank()) {
            throw new HarnessNotFoundException(name + " is not on PATH");
        }
        for (var entry : path.split(Pattern.quote(File.pathSeparator))) {
            if (entry.isBlank()) {
                continue;
            }
            var directory = Path.of(entry);
            if (!directory.isAbsolute()) {
                // a relative PATH entry is resolved against the working directory, which the
                // operator did not name; never execute from it
                continue;
            }
            var candidate = directory.resolve(name);
            if (!Files.isRegularFile(candidate) || !Files.isExecutable(candidate)) {
                continue;
            }
            try {
                return candidate.toRealPath();
            } catch (IOException _) {
                // an unresolvable entry is no candidate
            }
        }
        throw new HarnessNotFoundException(name + " is not on PATH");
    }

    private static String sha256(Path executable) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        try (var input = Files.newInputStream(executable)) {
            var buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String probeVersion(Path executable) throws IOException {
        var process = new ProcessBuilder(executable.toString(), "--version")
                            .redirectErrorStream(true)
                            .start();
        var output = new ByteArrayOutputStream();
        var readFailure = new AtomicReference<IOException>();
        var reader = Thread.ofVirtual().name("harness-probe").start(() -> {
            try (InputStream in = process.getInputStream()) {
                var buffer = new byte[BUFFER_SIZE];
                var total = 0;
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    var remaining = MAX_OUTPUT_BYTES - total;
                    if (remaining > 0) {
                        var length = Math.min(read, remaining);
                        output.write(buffer, 0, length);
                        total += length;
                    }
                }
            } catch (IOException e) {
                readFailure.set(e);
            }
        });
        try {
            if (!process.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                throw new HarnessProbeException("version probe of " + executable + " timed out");
            }
            reader.join(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new HarnessProbeException("version probe of " + executable + " was interrupted");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        if (readFailure.get() != null) {
            throw new HarnessProbeException("version probe of " + executable
                                + " could not be read: " + readFailure.get().getMessage());
        }
        if (process.exitValue() != 0) {
            throw new HarnessProbeException(
                                "version probe of " + executable + " exited " + process.exitValue());
        }
        return versionOf(executable, output.toString(StandardCharsets.UTF_8));
    }

    private static String versionOf(Path executable, String output) throws HarnessProbeException {
        var safe = OutputSafety.stripControlSequences(output).strip();
        if (safe.isEmpty()) {
            throw new HarnessProbeException("version probe of " + executable + " reported no output");
        }
        var matcher = VERSION.matcher(safe);
        var version = matcher.find() ? matcher.group(1) : safe.lines().findFirst().orElse("").strip();
        if (version.isEmpty()) {
            throw new HarnessProbeException("version probe of " + executable + " reported no version");
        }
        return version.length() > MAX_VERSION_LENGTH ? version.substring(0, MAX_VERSION_LENGTH) : version;
    }
}
