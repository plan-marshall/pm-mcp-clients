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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.resource.HarnessEnrolmentResource;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("EnrolHarnessCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class EnrolHarnessCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private Path binRoot;
    private Path bin;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        binRoot = Files.createTempDirectory(Path.of("/tmp"), "pm-harness");
        bin = Files.createDirectories(binRoot.resolve("bin"));
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
        if (binRoot != null) {
            try (var walk = Files.walk(binRoot)) {
                for (var path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private Path executable(String name, String output, int exitCode) throws IOException {
        var path = bin.resolve(name);
        Files.writeString(path, "#!/bin/sh\nprintf '%s\\n' \"" + output + "\"\nexit " + exitCode + "\n");
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        return path.toRealPath();
    }

    private int execute(String input, String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("PATH", bin.toString());
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(
                                                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                                                new ByteArrayOutputStream()),
                            _ -> {
                            });
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(path)));
    }

    @Test
    @DisplayName("shows path, version and digest and records the confirmed harness")
    void enrolsHarness() throws Exception {
        var executable = executable("claude", "2.4.1 (Claude Code)", 0);
        var digest = sha256(executable);
        var server = fixture.start((request, response) -> {
            if ("/api/v1/harnesses/claude".equals(request.target())
                                && "GET".equals(request.method())) {
                response.send(404, null, "");
            } else if ("/api/v1/harnesses/claude/enrol".equals(request.target())
                                && "POST".equals(request.method())) {
                response.json(200, new HarnessEnrolmentResource("claude", executable.toString(),
                                    "2.4.1", digest, HarnessEnrolmentResource.Status.ENROLLED).toJson());
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("", "enrol-harness", "claude", "--yes"));

        var output = out.toString();
        assertTrue(output.contains("Harness: claude"), output);
        assertTrue(output.contains(executable.toString()), output);
        assertTrue(output.contains(digest), output);
        assertTrue(output.contains("2.4.1"), output);
        assertTrue(output.contains("✓ Harness claude enrolled"), output);

        var get = server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("GET", get.method());
        var post = server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("POST", post.method());
        assertEquals("/api/v1/harnesses/claude/enrol", post.target());
        assertTrue(post.bodyText().contains("\"client\":\"claude\""), post.bodyText());
        assertTrue(post.bodyText().contains("\"executable_path\":\"" + executable + "\""), post.bodyText());
        assertTrue(post.bodyText().contains("\"version\":\"2.4.1\""), post.bodyText());
        assertTrue(post.bodyText().contains("\"sha256_digest\":\"" + digest + "\""), post.bodyText());
        assertTrue(post.bodyText().contains("\"status\":\"detected\""), post.bodyText());
    }

    @Test
    @DisplayName("shows the recorded and detected values side by side and reports a re-baseline")
    void rebaselinesChangedHarness() throws Exception {
        var executable = executable("claude", "2.5.0", 0);
        var digest = sha256(executable);
        var recordedSha = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        fixture.start((request, response) -> {
            if ("/api/v1/harnesses/claude".equals(request.target())) {
                response.json(200, new HarnessEnrolmentResource("claude", "/old/claude", "2.4.1",
                                    recordedSha, HarnessEnrolmentResource.Status.ENROLLED).toJson());
            } else {
                response.json(200, "");
            }
        });

        assertEquals(0, execute("", "enrol-harness", "claude", "--yes"));

        var output = out.toString();
        assertTrue(output.contains("Recorded"), output);
        assertTrue(output.contains("/old/claude"), output);
        assertTrue(output.contains(recordedSha), output);
        assertTrue(output.contains(digest), output);
        assertTrue(output.contains("✓ Harness claude re-baselined"), output);
    }

    @Test
    @DisplayName("a declined confirmation sends nothing")
    void declinesConfirmation() throws Exception {
        executable("claude", "2.4.1", 0);
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        assertEquals(1, execute("n\n", "enrol-harness", "claude"));

        assertTrue(out.toString().contains("The harness was not enrolled"), out.toString());
        assertTrue(server.awaitRequest(Duration.ofSeconds(1)) != null, "the recorded values are read");
        assertEquals(1, server.requests().size(), "no enrolment request may be sent");
    }

    @Test
    @DisplayName("an interactive yes records the harness")
    void confirmsInteractively() throws Exception {
        executable("claude", "2.4.1", 0);
        var server = fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.send(404, null, "");
            } else {
                response.json(200, "");
            }
        });

        assertEquals(0, execute("y\n", "enrol-harness", "claude"));

        var post = server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("GET", post.method());
        assertEquals("POST", server.awaitRequest(Duration.ofSeconds(1)).method());
    }

    @Test
    @DisplayName("--no-input without --yes needs the confirmation and sends nothing")
    void noInputNeedsYes() throws Exception {
        executable("claude", "2.4.1", 0);
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        assertEquals(3, execute("", "enrol-harness", "claude", "--no-input"));

        assertTrue(out.toString().contains("Harness enrolment confirmation required (--yes)"), out.toString());
        assertEquals(1, server.requests().size(), "no enrolment request may be sent");
    }

    @Test
    @DisplayName("refuses an unknown client before it starts the runtime")
    void refusesUnknownClient() {
        assertEquals(2, execute("", "enrol-harness", "cursor"));
        assertTrue(err.toString().contains("Unknown harness client: cursor"), err.toString());
    }

    @Test
    @DisplayName("reports an executable that is not on PATH")
    void reportsMissingExecutable() {
        assertEquals(1, execute("", "enrol-harness", "claude", "--yes"));
        assertTrue(err.toString().contains("not found on PATH"), err.toString());
    }

    @Test
    @DisplayName("reports a failed version probe")
    void reportsFailedProbe() throws Exception {
        executable("claude", "boom", 1);

        assertEquals(1, execute("", "enrol-harness", "claude", "--yes"));
        assertTrue(err.toString().contains("version probe of claude failed"), err.toString());
    }

    @Test
    @DisplayName("reports a version outside the supported range")
    void reportsUnsupportedVersion() throws Exception {
        executable("claude", "99.0.0", 0);
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.send(404, null, "");
            } else {
                response.json(409, "{\"error\":\"harness_version_unsupported\"}");
            }
        });

        assertEquals(1, execute("", "enrol-harness", "claude", "--yes"));
        assertTrue(err.toString().contains("outside the supported range"), err.toString());
    }

    @Test
    @DisplayName("refuses a coding-agent environment before the executable is probed (PM-SEC-3)")
    void refusesCodingAgentEnvironment() throws Exception {
        var executable = executable("claude", "2.4.1", 0);
        var environment = new HashMap<>(fixture.environment());
        environment.put("PATH", bin.toString());
        environment.put("CLAUDE_CODE", "1");
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment), null, _ -> {
                            });

        var writer = new StringWriter();
        var error = new StringWriter();
        var code = PmOperator.execute(context, new PrintWriter(writer), new PrintWriter(error),
                            "enrol-harness", "claude", "--yes");

        assertEquals(4, code);
        assertTrue(error.toString().contains("coding-agent environment detected (claude)"), error.toString());
        assertTrue(writer.toString().isEmpty());
        assertTrue(Files.exists(executable), "the probe was never reached");
        assertFalse(Files.exists(fixture.paths().socket()), "no request reached a runtime");
    }
}
