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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.host.HostDetection;

/**
 * Discovers and probes the headless CLI of one of the client profiles the terminal client knows
 * (CLI &amp; Security § Harness Enrolment). It resolves the executable through the absolute entries
 * of the login {@code PATH}, never through a shell, computes its SHA-256 digest, and runs the
 * profile's version probe with a bounded timeout, a bounded output, and a minimal environment.
 * <p>
 * The probe reports only facts the operator sees before the enrolment is recorded; it never hands
 * the operator's environment to the probed executable and never stores a harness credential. A
 * profile that declares no version probe (the antigravity profile does not) is enrolled with the
 * version {@value #UNREPORTED_VERSION} instead of running an unsupported flag.
 */
final class HarnessDiscovery {

    /** The version recorded for a client whose profile declares no version probe. */
    static final String UNREPORTED_VERSION = "unknown";

    private static final String PATH_VARIABLE = "PATH";

    /** The client ids the terminal client knows how to discover, from the shipped profiles. */
    static final List<String> SUPPORTED_CLIENTS = List.copyOf(HostDetection.clientIds());

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READER_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_OUTPUT_BYTES = 8 * 1024;
    private static final int MAX_VERSION_LENGTH = 128;
    private static final int BUFFER_SIZE = 1024;

    /**
     * The environment variables a version probe may see. The operator's environment is never handed
     * to the probed executable, which could otherwise read and exfiltrate an inherited credential.
     */
    private static final List<String> PROBE_ENVIRONMENT = List.of(
                        "PATH", "HOME", "LANG", "LC_ALL", "LC_CTYPE", "TZ", "TERM", "USER", "LOGNAME",
                        "SHELL", "TMPDIR", "SystemRoot", "windir", "COMSPEC", "PATHEXT", "USERPROFILE",
                        "APPDATA", "LOCALAPPDATA");

    private HarnessDiscovery() {
        // Utility class
    }

    /**
     * One discovered harness.
     *
     * @param client       the client id
     * @param executable   the resolved absolute executable path
     * @param version      the version the probe reported, or {@link #UNREPORTED_VERSION}
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
     * Resolves and probes one client's executable. The digest is read before and after the probe, so
     * a version and a digest that describe different binaries are never combined.
     *
     * @param client      the client id, one of {@link #SUPPORTED_CLIENTS}
     * @param environment the process environment, read for {@code PATH} and the probe allowlist only
     * @return the discovered harness
     * @throws HarnessNotFoundException if no absolute {@code PATH} entry holds the executable
     * @throws HarnessProbeException    if the version probe fails, reports no version, or the binary changes
     * @throws IOException              on another read failure
     */
    static Harness discover(String client, Map<String, String> environment) throws IOException {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(environment, "environment");
        if (!isSupported(client)) {
            throw new IllegalArgumentException("Unknown harness client: " + client);
        }
        var probe = HostDetection.host(client).map(HostDetection.Host::versionProbe).orElse(List.of());
        if (probe.size() <= 1) {
            // the profile declares no version probe (antigravity); record the path and digest only
            var executable = resolveExecutable(client, environment);
            return new Harness(client, executable, UNREPORTED_VERSION, sha256(executable));
        }
        var executable = resolveExecutable(probe.getFirst(), environment);
        var before = sha256(executable);
        var version = probeVersion(executable, probe.subList(1, probe.size()), environment);
        var after = sha256(executable);
        if (!before.equals(after)) {
            throw new HarnessProbeException(
                                "the executable of " + client + " changed while it was probed");
        }
        return new Harness(client, executable, version, after);
    }

    private static Path resolveExecutable(String name, Map<String, String> environment)
                        throws HarnessNotFoundException {
        var path = environment.get(PATH_VARIABLE);
        if (path == null || path.isBlank()) {
            throw new HarnessNotFoundException(name + " is not on PATH");
        }
        for (var entry : path.split(Pattern.quote(File.pathSeparator))) {
            var resolved = candidateIn(entry, name);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new HarnessNotFoundException(name + " is not on PATH");
    }

    private static @Nullable Path candidateIn(String entry, String name) {
        if (entry.isBlank()) {
            return null;
        }
        var directory = Path.of(entry);
        if (!directory.isAbsolute()) {
            // a relative PATH entry is resolved against the working directory, which the operator
            // did not name; never execute from it
            return null;
        }
        var candidate = directory.resolve(name);
        if (!Files.isRegularFile(candidate) || !Files.isExecutable(candidate)) {
            return null;
        }
        try {
            return candidate.toRealPath();
        } catch (IOException _) {
            // an unresolvable entry is no candidate
            return null;
        }
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

    private static String probeVersion(Path executable, List<String> argv, Map<String, String> environment)
                        throws IOException {
        var command = new ArrayList<String>(argv.size() + 1);
        command.add(executable.toString());
        command.addAll(argv);
        var builder = new ProcessBuilder(command).redirectErrorStream(true);
        var processEnvironment = builder.environment();
        processEnvironment.clear();
        for (var name : PROBE_ENVIRONMENT) {
            var value = environment.get(name);
            if (value != null) {
                processEnvironment.put(name, value);
            }
        }
        var process = builder.start();

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
        boolean joined = awaitProbe(process, reader, executable);
        if (!joined) {
            process.destroyForcibly();
            throw new HarnessProbeException(
                                "version probe of " + executable + " left its output open");
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

    private static boolean awaitProbe(Process process, Thread reader, Path executable)
                        throws HarnessProbeException {
        try {
            if (!process.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                throw new HarnessProbeException("version probe of " + executable + " timed out");
            }
            reader.join(READER_TIMEOUT.toMillis());
            return !reader.isAlive();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new HarnessProbeException("version probe of " + executable + " was interrupted");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static String versionOf(Path executable, String output) throws HarnessProbeException {
        var safe = OutputSafety.stripControlSequences(output).strip();
        if (safe.isEmpty()) {
            throw new HarnessProbeException("version probe of " + executable + " reported no output");
        }
        var matcher = dottedVersion(safe);
        var version = matcher != null ? matcher : safe.lines().findFirst().orElse("").strip();
        if (version.isEmpty()) {
            throw new HarnessProbeException("version probe of " + executable + " reported no version");
        }
        return version.length() > MAX_VERSION_LENGTH ? version.substring(0, MAX_VERSION_LENGTH) : version;
    }

    private static @Nullable String dottedVersion(String text) {
        var start = -1;
        var dotted = false;
        for (var index = 0; index < text.length(); index++) {
            var character = text.charAt(index);
            if (Character.isDigit(character)) {
                if (start < 0) {
                    start = index;
                }
            } else if (character == '.' && start >= 0
                                && index + 1 < text.length() && Character.isDigit(text.charAt(index + 1))) {
                dotted = true;
            } else if (start >= 0) {
                if (dotted) {
                    return text.substring(start, index);
                }
                start = -1;
                dotted = false;
            }
        }
        return start >= 0 && dotted ? text.substring(start) : null;
    }
}
