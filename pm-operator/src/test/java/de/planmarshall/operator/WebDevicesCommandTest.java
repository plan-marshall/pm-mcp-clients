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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.jline.terminal.impl.DumbTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.api.testsupport.RuntimeFixture;

import picocli.CommandLine;

@DisplayName("WebCommand.DevicesCommand")
class WebDevicesCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private OperatorContext createContext(String input) throws IOException {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        var terminal = new DumbTerminal(inStream, outStream);
        var clientEnv = ClientEnvironment.capture(fixture.environment());
        return new OperatorContext(fixture.environment(), tempDir, true, clientEnv, () -> terminal, null);
    }

    private void seedRuntimeRoutes(boolean lanOn) throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            var path = request.target();
            var method = request.method();
            if ("/api/v1/status".equals(path)) {
                response.json(200, """
                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"lan":%s,"port":7420}}
                        """.formatted(lanOn));
            } else if ("/api/v1/web/devices".equals(path) && "GET".equals(method)) {
                response.json(200, """
                        [
                          {
                            "device_id": "dev-1",
                            "label": "Firefox on mbp-oliver",
                            "paired_from": "loopback",
                            "created_at": "2026-05-12T10:00:00Z",
                            "last_used_at": "2026-10-16T14:02:00Z"
                          },
                          {
                            "device_id": "dev-2",
                            "label": "Safari on iPad",
                            "paired_from": "network",
                            "created_at": "2026-05-20T10:00:00Z",
                            "last_used_at": "2026-10-13T10:00:00Z"
                          },
                          {
                            "device_id": "dev-3",
                            "label": "Chrome on build-box",
                            "paired_from": "loopback",
                            "created_at": "2026-06-02T10:00:00Z",
                            "last_used_at": "2026-09-08T10:00:00Z"
                          }
                        ]
                        """);
            } else if (path.startsWith("/api/v1/web/devices/dev-2") && ("PUT".equals(method) || "PATCH".equals(method))) {
                response.json(200, """
                        {"device_id":"dev-2","label":"iPad Oliver"}
                        """);
            } else if (path.startsWith("/api/v1/web/devices/dev-3") && "DELETE".equals(method)) {
                response.json(200, """
                        {"status":"revoked","device_id":"dev-3","label":"Chrome on build-box"}
                        """);
            } else if (path.startsWith("/api/v1/web/devices/dev-1") && "DELETE".equals(method)) {
                response.json(200, """
                        {"status":"revoked","device_id":"dev-1","label":"Firefox on mbp-oliver"}
                        """);
            } else if (path.startsWith("/api/v1/web/devices?scope=others") && "DELETE".equals(method)) {
                response.json(200, """
                        {"status":"revoked","revoked_count":3}
                        """);
            } else if ("/api/v1/web".equals(path) && "PUT".equals(method)) {
                response.json(200, """
                        {"status":"ok"}
                        """);
            } else {
                response.send(404, null, "Not found: " + path);
            }
        });
    }

    @Test
    @DisplayName("lists paired devices non-interactively with formatted columns and listener status")
    void listsDevicesNonInteractively() throws Exception {
        seedRuntimeRoutes(true);

        var context = createContext("");
        var writer = new StringWriter();
        var cmd = new CommandLine(new WebCommand.DevicesCommand(context));
        cmd.setOut(new PrintWriter(writer));

        int code = cmd.execute();

        var output = writer.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("Paired devices · The browsers that can act on this machine")),
                () -> assertTrue(output.contains("Firefox on mbp-oliver")),
                () -> assertTrue(output.contains("paired 12 May from loopback")),
                () -> assertTrue(output.contains("last used today 14:02")),
                () -> assertTrue(output.contains("Safari on iPad")),
                () -> assertTrue(output.contains("paired 20 May from network")),
                () -> assertTrue(output.contains("last used 3 d ago")),
                () -> assertTrue(output.contains("Chrome on build-box")),
                () -> assertTrue(output.contains("paired 2 Jun from loopback")),
                () -> assertTrue(output.contains("last used 5 weeks ago")),
                () -> assertTrue(output.contains("Network listener: on."))
        );
    }

    @Test
    @DisplayName("renames device directly via CLI arguments")
    void renamesDeviceDirectly() throws Exception {
        seedRuntimeRoutes(true);

        var context = createContext("");
        var writer = new StringWriter();
        var cmd = new CommandLine(new WebCommand.DevicesCommand(context));
        cmd.setOut(new PrintWriter(writer));

        int code = cmd.execute("rename", "dev-2", "iPad Oliver");

        var output = writer.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("✓ Renamed: iPad Oliver"))
        );
    }

    @Test
    @DisplayName("revokes device 3")
    void revokesDevice3() throws Exception {
        seedRuntimeRoutes(true);

        var context = createContext("");
        var writer = new StringWriter();
        var cmd = new CommandLine(new WebCommand.DevicesCommand(context));
        cmd.setOut(new PrintWriter(writer));

        int code = cmd.execute("revoke", "dev-3");

        var output = writer.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("✓ Revoked paired device(s)"))
        );
    }

    @Test
    @DisplayName("revokes others")
    void revokesOthers() throws Exception {
        seedRuntimeRoutes(true);

        var context = createContext("");
        var writer = new StringWriter();
        var cmd = new CommandLine(new WebCommand.DevicesCommand(context));
        cmd.setOut(new PrintWriter(writer));

        int code = cmd.execute("revoke", "--others");

        var output = writer.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("✓ Revoked paired device(s)"))
        );
    }

    @Test
    @DisplayName("turns network listener off via listener-off action")
    void turnsNetworkListenerOff() throws Exception {
        seedRuntimeRoutes(true);

        var context = createContext("");
        var writer = new StringWriter();
        var cmd = new CommandLine(new WebCommand.DevicesCommand(context));
        cmd.setOut(new PrintWriter(writer));

        int code = cmd.execute("listener-off");

        var output = writer.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("✓ Network listener turned off (loopback remains open)"))
        );
    }

    @Test
    @DisplayName("handles interactive rename flow faithfully matching transcript 08-transcripts-later.adoc#t-devices")
    void interactiveRenameFlow() throws Exception {
        seedRuntimeRoutes(true);

        // Input sequence: "2" -> "r" -> "iPad Oliver"
        var context = createContext("2\nr\niPad Oliver\n");

        var writer = new StringWriter();
        var cmd = new CommandLine(new WebCommand.DevicesCommand(context));
        cmd.setOut(new PrintWriter(writer));

        int code = cmd.execute("-i");

        var output = writer.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("Paired devices · The browsers that can act on this machine")),
                () -> assertTrue(output.contains("2) Safari on iPad")),
                () -> assertTrue(output.contains("✓ Renamed: iPad Oliver"))
        );
    }
}
