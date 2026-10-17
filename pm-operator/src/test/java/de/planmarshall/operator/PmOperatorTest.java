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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.PmVersion;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("pm-operator command line")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PmOperatorTest {

    static final String STATUS = "{\"version\":\"0.1\",\"pid\":42,\"listener\":\"unix\",\"socket_path\":\"/s\","
                        + "\"started_at\":\"2026-10-05T10:00:00Z\",\"web\":{\"enabled\":true,\"lan\":false,\"port\":7420,\"open\":true}}";

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private final List<URI> openedUris = new ArrayList<>();
    private boolean simulateBrowserError = false;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("pm.test.no.browser", "true");
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
        openedUris.clear();
        simulateBrowserError = false;
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("pm.test.no.browser");
        fixture.close();
    }

    private int run(String... args) {
        var context = new OperatorContext(
                            fixture.environment(),
                            fixture.base().getParent(),
                            true,
                            ClientEnvironment.capture(fixture.environment()),
                            null,
                            uri -> {
                                if (simulateBrowserError) {
                                    throw new IOException("simulated browser failure");
                                }
                                openedUris.add(uri);
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    /** Writes a live runtime record naming this process. */
    static void writeLiveRecord(RuntimeFixture fixture) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        writeLiveRecord(fixture);
        return fixture.start(handler);
    }

    @Test
    @DisplayName("--version prints the release version")
    void version() {
        assertEquals(0, run("--version"));
        assertEquals("pm-operator " + PmVersion.current(), out.toString().strip());
    }

    @Test
    @DisplayName("bare pm-operator opens console and executes exit")
    void bareOperatorOpensConsole() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"version\":\"0.1\",\"pid\":42,\"onboarded\":true,\"web\":{\"enabled\":true,\"lan\":false,\"port\":7420,\"open\":true}}");
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[]");
            } else if ("/api/v1/web/pairings".equals(request.target())) {
                response.json(201, "{\"code\":\"TEST-CODE\",\"url\":\"http://127.0.0.1:7420/pair#code=TEST-CODE\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var inStream = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
        var context = new OperatorContext(
                            fixture.environment(), fixture.base().getParent(), true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(inStream, new ByteArrayOutputStream()),
                            openedUris::add
        );

        int exitCode = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err));
        assertEquals(0, exitCode);
    }

    @Nested
    @DisplayName("status")
    class Status {

        @Test
        @DisplayName("reports not running without a runtime record, starting and writing nothing")
        void notRunning() {
            assertEquals(0, run("status"));
            assertEquals("runtime   not running", out.toString().strip());
            assertFalse(Files.exists(fixture.base()));
        }

        @Test
        @DisplayName("reports not running for a record of a dead process")
        void deadRecord() throws Exception {
            Files.createDirectories(fixture.paths().stateDir());
            Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + (Integer.MAX_VALUE - 1) + "}");

            assertEquals(0, run("status", "--json"));
            assertEquals("{\"running\":false}", out.toString().strip());
        }

        @Test
        @DisplayName("reports not running when the recorded runtime does not answer")
        void silentRecord() throws Exception {
            writeLiveRecord(fixture);
            fixture.writeToken("t");

            assertEquals(0, run("status"));
            assertEquals("runtime   not running", out.toString().strip());
        }

        @Test
        @DisplayName("fetches and prints the runtime status")
        void running() throws Exception {
            var server = runtime((request, response) -> response.json(200, STATUS));

            assertEquals(0, run("status"));
            var text = out.toString();
            assertTrue(text.contains("runtime   running"));
            assertTrue(text.contains("pid       42"));
            assertTrue(text.contains("web       enabled (loopback), port 7420, open"));
            assertTrue(server.awaitRequest(Duration.ofSeconds(1)).header("User-Agent").startsWith("pm-operator/"));

            out.getBuffer().setLength(0);
            assertEquals(0, run("status", "--json"));
            assertEquals(STATUS, out.toString().strip());
        }

        @Test
        @DisplayName("fails on an unexpected answer")
        void refused() throws Exception {
            runtime((request, response) -> response.json(500, "{\"code\":\"internal_fault\"}"));

            assertEquals(PmOperator.EXIT_FAILURE, run("status"));
            assertTrue(err.toString().contains("HTTP 500"));

            fixture.stop();
            fixture.start((request, response) -> response.json(200, "[]"));
            assertEquals(PmOperator.EXIT_FAILURE, run("status"));
        }
    }

    @Nested
    @DisplayName("web")
    class Web {

        @Test
        @DisplayName("bare web prints usage")
        void bareWeb() {
            assertEquals(0, run("web"));
            assertTrue(out.toString().contains("Usage: pm-operator web"));
        }

        @Test
        @DisplayName("enable sends PUT /api/v1/web with lan and port and prints the listener")
        void enable() throws Exception {
            var server = runtime((request, response) -> response.json(200,
                                "{\"enabled\":true,\"lan\":true,\"port\":8443,\"open\":true}"));

            assertEquals(0, run("web", "enable", "--lan", "--port", "8443"));

            var request = server.awaitRequest(Duration.ofSeconds(1));
            assertEquals("PUT", request.method());
            assertEquals("/api/v1/web", request.target());
            assertEquals("{\"enabled\":true,\"lan\":true,\"port\":8443}", request.bodyText());
            assertEquals("en", request.header("accept-language"));
            assertEquals("web       enabled (lan), port 8443, open", out.toString().strip());
        }

        @Test
        @DisplayName("enable --lan reports the LAN URL and certificate fingerprint")
        void enableLanReportsFingerprint() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/status/connection".equals(request.target())) {
                    response.json(200, """
                                        {"loopback_open":true,"lan_enabled":true,
                                         "lan_url":"https://mbp-oliver.local:7421",
                                         "lan_fingerprint":"4F:1A:9C","paired_devices_count":0,"onboarded":true}
                                        """);
                } else {
                    response.json(200, "{\"enabled\":true,\"lan\":true,\"port\":7420,\"open\":true}");
                }
            });

            assertEquals(0, run("web", "enable", "--lan"));

            var output = out.toString();
            assertTrue(output.contains("web       enabled (lan), port 7420, open"));
            assertTrue(output.contains("https://mbp-oliver.local:7421"), "LAN URL is reported");
            assertTrue(output.contains("4F:1A:9C"), "Certificate fingerprint is reported");
        }

        @Test
        @DisplayName("disable sends enabled false with the default port")
        void disable() throws Exception {
            var server = runtime((request, response) -> response.json(200,
                                "{\"enabled\":false,\"lan\":false,\"port\":7420,\"open\":false}"));

            assertEquals(0, run("web", "disable"));

            assertEquals("{\"enabled\":false,\"lan\":false,\"port\":7420}",
                                server.awaitRequest(Duration.ofSeconds(1)).bodyText());
            assertEquals("web       disabled", out.toString().strip());
        }

        @Test
        @DisplayName("reports a conflict")
        void conflict() throws Exception {
            runtime((request, response) -> response.json(409, "{\"code\":\"web_listener_conflict\"}"));

            assertEquals(PmOperator.EXIT_FAILURE, run("web", "enable"));
            assertTrue(err.toString().contains("web_listener_conflict"));
        }

        @Test
        @DisplayName("describes a missing and a closed listener")
        void describe() {
            assertEquals("-", WebCommand.describe(null));
            assertEquals("enabled (loopback), port 1, not open", WebCommand.describe(Map.of("enabled", true,
                                "port", 1)));
        }

        @Test
        @DisplayName("refuses execution when token file has insecure permissions (PM-SEC-4)")
        void insecureTokenFile() throws Exception {
            runtime((request, response) -> response.json(200, "{\"enabled\":true}"));
            Files.setPosixFilePermissions(fixture.paths().runtimeToken(),
                                PosixFilePermissions.fromString("rw-r--r--"));

            assertEquals(PmOperator.EXIT_FAILURE, run("web", "enable"));
            assertTrue(err.toString().contains("has mode 0644, expected 0600"));
        }

        @Test
        @DisplayName("web pair issues pairing code and prints QR code")
        void pair() throws Exception {
            var server = runtime((request, response) -> {
                if ("/api/v1/web/pairings".equals(request.target()) && "POST".equals(request.method())) {
                    response.json(201, "{\"code\":\"CODE-999\",\"url\":\"http://127.0.0.1:7420/pair#code=CODE-999\"}");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "pair", "--label", "my-phone"));
            var req = server.awaitRequest(Duration.ofSeconds(1));
            assertTrue(req.bodyText().contains("\"label\":\"my-phone\""));
            assertTrue(out.toString().contains("✓ Pairing code: CODE-999"));
            assertTrue(out.toString().contains("http://127.0.0.1:7420/pair#code=CODE-999"));
        }

        @Test
        @DisplayName("web pair reports refusal on failure")
        void pairRefused() throws Exception {
            runtime((request, response) -> response.send(500, null, ""));
            assertEquals(PmOperator.EXIT_FAILURE, run("web", "pair"));
        }

        @Test
        @DisplayName("web qr displays pairing QR code")
        void qr() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, STATUS);
                } else if ("/api/v1/web/pairings".equals(request.target())) {
                    response.json(201, "{\"code\":\"QR-42\",\"url\":\"http://127.0.0.1:7420/pair#code=QR-42\"}");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "qr"));
            assertTrue(out.toString().contains("http://127.0.0.1:7420/pair#code=QR-42"));
        }

        @Test
        @DisplayName("web open opens browser with paired url or new code")
        void open() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, STATUS);
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, "[{\"label\":\"paired-device\",\"paired_from\":\"loopback\"}]");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "open"));
            assertEquals(1, openedUris.size());
            assertEquals("http://127.0.0.1:7420", openedUris.get(0).toString());
            assertTrue(out.toString().contains("✓ Browser opened: http://127.0.0.1:7420"));
        }

        @Test
        @DisplayName("web open with --pair forces new pairing code")
        void openForcePair() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, STATUS);
                } else if ("/api/v1/web/pairings".equals(request.target())) {
                    response.json(201, "{\"code\":\"FORCE-1\",\"url\":\"http://127.0.0.1:7420/pair#code=FORCE-1\"}");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "open", "--pair"));
            assertEquals(1, openedUris.size());
            assertEquals("http://127.0.0.1:7420/pair#code=FORCE-1", openedUris.get(0).toString());
            assertTrue(out.toString().contains("✓ Browser opened: http://127.0.0.1:7420/pair#code=FORCE-1"));
        }

        @Test
        @DisplayName("web open reports manual link when browser opener fails")
        void openBrowserFailure() throws Exception {
            simulateBrowserError = true;
            runtime((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, STATUS);
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, "[{\"label\":\"dev\"}]");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "open"));
            assertTrue(out.toString().contains("– No browser opened: simulated browser failure"));
            assertTrue(out.toString().contains("Open manually: http://127.0.0.1:7420"));
        }

        @Test
        @DisplayName("web devices lists paired devices or empty message")
        void devicesList() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/web/devices".equals(request.target()) && "GET".equals(request.method())) {
                    response.json(200, """
                                        [
                                          {"label":"Work Laptop","paired_from":"127.0.0.1"},
                                          {"label":"Mobile","paired_from":"192.168.1.100"}
                                        ]
                                        """);
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "devices"));
            var text = out.toString();
            assertTrue(text.contains("Paired devices"));
            assertTrue(text.contains("Work Laptop"));
            assertTrue(text.contains("192.168.1.100"));

            out.getBuffer().setLength(0);
            fixture.stop();
            fixture.start((request, response) -> response.json(200, "[]"));
            assertEquals(0, run("web", "devices", "list"));
            assertTrue(out.toString().contains("No paired devices."));
        }

        @Test
        @DisplayName("web devices rename updates device label")
        void devicesRename() throws Exception {
            var server = runtime((request, response) -> {
                if ("/api/v1/web/devices/dev-123".equals(request.target()) && "PUT".equals(request.method())) {
                    response.send(200, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "devices", "rename", "dev-123", "New Desktop"));
            assertTrue(out.toString().contains("✓ Renamed: New Desktop"));
            var req = server.awaitRequest(Duration.ofSeconds(1));
            assertTrue(req.bodyText().contains("\"label\":\"New Desktop\""));
        }

        @Test
        @DisplayName("web devices rename requires arguments")
        void devicesRenameMissingArgs() throws Exception {
            runtime((request, response) -> response.send(200, null, ""));
            assertEquals(2, run("web", "devices", "rename"));
        }

        @Test
        @DisplayName("web devices revoke removes devices by id, others, or all")
        void devicesRevoke() throws Exception {
            var server = runtime((request, response) -> {
                if (request.target().startsWith("/api/v1/web/devices") && "DELETE".equals(request.method())) {
                    response.send(204, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "devices", "revoke", "dev-1"));
            assertTrue(out.toString().contains("✓ Revoked paired device(s)"));
            assertEquals("/api/v1/web/devices/dev-1", server.awaitRequest(Duration.ofSeconds(1)).target());

            out.getBuffer().setLength(0);
            assertEquals(0, run("web", "devices", "revoke", "--others"));
            assertTrue(out.toString().contains("✓ Revoked paired device(s)"));
            assertEquals("/api/v1/web/devices?scope=others", server.awaitRequest(Duration.ofSeconds(1)).target());

            out.getBuffer().setLength(0);
            assertEquals(0, run("web", "devices", "revoke", "--all"));
            assertTrue(out.toString().contains("✓ Revoked paired device(s)"));
            assertEquals("/api/v1/web/devices?scope=all", server.awaitRequest(Duration.ofSeconds(1)).target());
        }

        @Test
        @DisplayName("web devices revoke requires arguments")
        void devicesRevokeMissingArgs() throws Exception {
            runtime((request, response) -> response.send(200, null, ""));
            assertEquals(2, run("web", "devices", "revoke"));
        }

        @Test
        @DisplayName("web devices listener-off turns off network listener")
        void devicesListenerOff() throws Exception {
            var server = runtime((request, response) -> {
                if ("/api/v1/web".equals(request.target()) && "PUT".equals(request.method())) {
                    response.send(200, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("web", "devices", "listener-off"));
            assertTrue(out.toString().contains("✓ Network listener turned off (loopback remains open)"));
            assertTrue(server.awaitRequest(Duration.ofSeconds(1)).bodyText().contains("\"lan\":false"));
        }
    }

    @Nested
    @DisplayName("runtime")
    class Runtime {

        @Test
        @DisplayName("bare runtime prints usage")
        void bareRuntime() {
            assertEquals(0, run("runtime"));
            assertTrue(out.toString().contains("Usage: pm-operator runtime"));
        }

        @Test
        @DisplayName("start reports a runtime that answers")
        void start() throws Exception {
            runtime((request, response) -> response.json(200, STATUS));

            assertEquals(0, run("runtime", "start"));
            assertEquals("runtime   running (pid 42)", out.toString().strip());
        }

        @Test
        @DisplayName("stop posts the stop request")
        void stop() throws Exception {
            var server = runtime((request, response) -> response.send(202, null, ""));

            assertEquals(0, run("runtime", "stop"));

            var request = server.awaitRequest(Duration.ofSeconds(1));
            assertEquals("POST", request.method());
            assertEquals("/api/v1/runtime/stop", request.target());
            assertEquals("runtime   stopping", out.toString().strip());
        }

        @Test
        @DisplayName("stop with --drain reports draining")
        void stopDrain() throws Exception {
            var server = runtime((request, response) -> response.send(202, null, ""));

            assertEquals(0, run("runtime", "stop", "--drain"));
            var req = server.awaitRequest(Duration.ofSeconds(1));
            assertTrue(req.bodyText().contains("\"drain\":true"));
            assertEquals("runtime   draining", out.toString().strip());
        }

        @Test
        @DisplayName("stop with --now posts now flag")
        void stopNow() throws Exception {
            var server = runtime((request, response) -> response.send(200, null, ""));

            assertEquals(0, run("runtime", "stop", "--now"));
            var req = server.awaitRequest(Duration.ofSeconds(1));
            assertTrue(req.bodyText().contains("\"now\":true"));
            assertEquals("runtime   stopping", out.toString().strip());
        }

        @Test
        @DisplayName("stop with --withdraw withdraws drain")
        void stopWithdraw() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/runtime/stop".equals(request.target()) && "DELETE".equals(request.method())) {
                    response.send(200, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("runtime", "stop", "--withdraw"));
            assertEquals("runtime   drain withdrawn", out.toString().strip());
        }

        @Test
        @DisplayName("stop with --withdraw reports error on failure")
        void stopWithdrawFailed() throws Exception {
            runtime((request, response) -> response.send(500, null, ""));

            assertEquals(1, run("runtime", "stop", "--withdraw"));
            assertTrue(err.toString().contains("Failed to withdraw drain: HTTP 500"));
        }

        @Test
        @DisplayName("stop without a runtime reports not running")
        void stopWithoutRuntime() {
            assertEquals(0, run("runtime", "stop"));
            assertEquals("runtime   not running", out.toString().strip());
        }

        @Test
        @DisplayName("stop reports an unexpected answer")
        void stopRefused() throws Exception {
            runtime((request, response) -> response.send(500, null, ""));

            assertEquals(PmOperator.EXIT_FAILURE, run("runtime", "stop"));
        }

        @Test
        @DisplayName("restart posts restart request")
        void restart() throws Exception {
            var server = runtime((request, response) -> {
                if ("/api/v1/operator/runtime/stop".equals(request.target()) && "POST".equals(request.method())) {
                    response.send(202, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("runtime", "restart", "--drain", "--yes"));
            assertTrue(out.toString().contains("runtime   restarting"));
            var req = server.awaitRequest(Duration.ofSeconds(1));
            assertTrue(req.bodyText().contains("\"then\":\"restart\""));
            assertTrue(req.bodyText().contains("\"drain\":true"));
        }

        @Test
        @DisplayName("upgrade posts upgrade request")
        void upgrade() throws Exception {
            var server = runtime((request, response) -> {
                if ("/api/v1/operator/runtime/upgrade".equals(request.target()) && "POST".equals(request.method())) {
                    response.send(200, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("runtime", "upgrade", "1.5.0", "--drain", "--yes"));
            assertTrue(out.toString().contains("runtime   upgrading"));
            var req = server.awaitRequest(Duration.ofSeconds(1));
            assertTrue(req.bodyText().contains("\"version\":\"1.5.0\""));
            assertTrue(req.bodyText().contains("\"drain\":true"));
        }

        @Test
        @DisplayName("upgrade --check performs version check")
        void upgradeCheck() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/operator/runtime/upgrade/check".equals(request.target())) {
                    response.send(200, null, "");
                } else {
                    response.send(500, null, "");
                }
            });

            assertEquals(0, run("runtime", "upgrade", "--check"));
            assertTrue(out.toString().contains("✓ Version check completed"));
        }

        @Test
        @DisplayName("upgrade --notes displays release notes")
        void upgradeNotes() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/system/release-notes".equals(request.target())) {
                    response.send(200, null, "Release Notes: v1.5.0 introduces new features.");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("runtime", "upgrade", "--notes"));
            assertTrue(out.toString().contains("Release Notes: v1.5.0 introduces new features."));
        }

        @Test
        @DisplayName("upgrade --notes reports when not found")
        void upgradeNotesNotFound() throws Exception {
            runtime((request, response) -> response.send(404, null, ""));

            assertEquals(0, run("runtime", "upgrade", "--notes"));
            assertTrue(out.toString().contains("No release notes found."));
        }

        @Test
        @DisplayName("upgrade --withdraw withdraws pending upgrade")
        void upgradeWithdraw() throws Exception {
            runtime((request, response) -> {
                if ("/api/v1/operator/runtime/upgrade".equals(request.target()) && "DELETE".equals(request.method())) {
                    response.send(200, null, "");
                } else {
                    response.send(404, null, "");
                }
            });

            assertEquals(0, run("runtime", "upgrade", "--withdraw"));
            assertTrue(out.toString().contains("runtime   upgrade withdrawn"));
        }

        @Test
        @DisplayName("upgrade --withdraw reports failure")
        void upgradeWithdrawFailed() throws Exception {
            runtime((request, response) -> response.send(500, null, ""));

            assertEquals(0, run("runtime", "upgrade", "--withdraw"));
            assertTrue(out.toString().contains("Failed to withdraw upgrade: HTTP 500"));
        }
    }
}
