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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine.Command;

@EnableGeneratorController
@DisplayName("ConfigCommand")
class ConfigCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private int execute(String... args) {
        return executeWithTyped(TYPED, args);
    }

    private int executeWithTyped(String typed, String... args) {
        var input = new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8));
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(input, new ByteArrayOutputStream()), null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    /** The keys a test types at a prompt: leaving it is enough for every case that reaches one. */
    private static final String TYPED = "q\n";

    @Test
    @DisplayName("bare config prints machine settings")
    void bareConfig() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[" + PORT_VALUE + "," + FLAG_VALUE + "]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("How this machine is set up"), out.toString()),
                            () -> assertTrue(out.toString().contains("Language"), out.toString())
        );
    }

    @Test
    @DisplayName("bare config falls back to the effective values the runtime serves")
    void bareConfigFallsBackToTheEffectiveValues() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[" + PORT_VALUE + "," + FLAG_VALUE + "]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("web.port"), out.toString()),
                            () -> assertTrue(out.toString().contains("7420"), out.toString())
        );
    }

    @Test
    @DisplayName("config show reads the effective values and names each origin")
    void configShowListsTheEffectiveValues() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[" + PORT_VALUE + "," + FLAG_VALUE + "," + OVERRIDDEN_VALUE + "]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "show");
        var text = out.toString();
        assertAll(
                            () -> assertEquals(0, code, text),
                            () -> assertTrue(text.contains("Configuration"), text),
                            () -> assertTrue(text.contains("web.port"), text),
                            () -> assertTrue(text.contains("7420"), text),
                            () -> assertTrue(text.contains("machine"), text),
                            () -> assertTrue(text.contains("team"), text),
                            () -> assertTrue(text.contains("overrides machine"), text)
        );
    }

    @Test
    @DisplayName("config show aligns the values in one column")
    void configShowAlignsTheValues() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[" + PORT_VALUE + "," + FLAG_VALUE + "]");
            } else {
                response.send(404, null, "");
            }
        });

        execute("config", "show");
        var text = out.toString();
        var port = rowOf(text, "web.port");
        var flag = rowOf(text, "console.full_screen");

        assertAll(
                            () -> assertNotNull(port, text),
                            () -> assertNotNull(flag, text),
                            () -> assertEquals(port.indexOf("7420"), flag.indexOf("true"), text),
                            () -> assertEquals("machine", port.substring(port.indexOf("7420")).replace("7420", "").trim(),
                                                text));
    }

    @Test
    @DisplayName("config show selects one key and leaves with 1 when it is not set")
    void configShowSelectsOneKey() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[" + PORT_VALUE + "," + FLAG_VALUE + "]");
            } else {
                response.send(404, null, "");
            }
        });

        int selected = execute("config", "show", "web.port");
        var text = out.toString();
        assertAll(
                            () -> assertEquals(0, selected, text),
                            () -> assertTrue(text.contains("web.port"), text),
                            () -> assertFalse(text.contains("console.full_screen"), text));

        out.getBuffer().setLength(0);
        int missing = execute("config", "show", "nope.key");
        assertAll(
                            () -> assertEquals(1, missing, out.toString()),
                            () -> assertTrue(out.toString().contains("is not set"), out.toString()));
    }

    @Test
    @DisplayName("config set writes one key at one level")
    void configSetWritesTheLevelRoute() throws Exception {
        var requests = new ArrayList<String>();
        fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ("GET".equals(request.method()) && "/api/v1/config".equals(request.target())) {
                response.json(200, "[" + valueOf("custom.timeout", "30", 4) + "]");
            } else if ("PUT".equals(request.method())
                                && "/api/v1/config/machine/custom.timeout".equals(request.target())) {
                response.json(200, "{\"key\":\"custom.timeout\",\"value\":\"60\",\"origin\":\"machine\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "set", "custom.timeout", "60");
        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains("✓ Config custom.timeout set to 60"),
                                                out.toString()),
                            () -> assertTrue(requests.contains("PUT /api/v1/config/machine/custom.timeout"),
                                                requests.toString()));
    }

    @Test
    @DisplayName("config set sends the revision the value was read with")
    void configSetSendsTheRevision() throws Exception {
        var bodies = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[" + valueOf("custom.timeout", "30", 4) + "]");
            } else if ("PUT".equals(request.method())) {
                bodies.add(request.bodyText());
                response.json(200, valueOf("custom.timeout", "60", 5));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "set", "custom.timeout", "60");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertEquals(1, bodies.size(), bodies.toString()),
                            () -> assertTrue(bodies.get(0).contains("\"revision\":4"), bodies.get(0)),
                            () -> assertTrue(bodies.get(0).contains("\"value\":\"60\""), bodies.get(0)));
    }

    @Test
    @DisplayName("config unset sends the revision the value was read with")
    void configUnsetSendsTheRevision() throws Exception {
        var bodies = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[" + valueOf("custom.flag", "on", 4) + "]");
            } else if ("DELETE".equals(request.method())) {
                bodies.add(request.bodyText());
                response.send(204, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "unset", "custom.flag");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertEquals(1, bodies.size(), bodies.toString()),
                            () -> assertTrue(bodies.get(0).contains("\"revision\":4"), bodies.get(0)),
                            () -> assertFalse(bodies.get(0).contains("\"value\""), bodies.get(0)));
    }

    @Test
    @DisplayName("a conflict prints the current value without the HTTP status and applies again")
    void configSetConflictAppliesAgain() throws Exception {
        var bodies = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[" + valueOf("custom.timeout", "30", 4) + "]");
            } else if ("PUT".equals(request.method())) {
                bodies.add(request.bodyText());
                if (bodies.size() == 1) {
                    response.json(409, conflictOf("custom.timeout", "30", 5));
                } else {
                    response.json(200, valueOf("custom.timeout", "60", 6));
                }
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithTyped("\n", "config", "set", "custom.timeout", "60");
        var text = out.toString();

        assertAll(
                            () -> assertEquals(0, code, text),
                            () -> assertTrue(text.contains(
                                                "Changed meanwhile: it is now 30. Yours was not applied."), text),
                            () -> assertFalse(text.contains("HTTP 409"), text),
                            () -> assertEquals(2, bodies.size(), bodies.toString()),
                            () -> assertTrue(bodies.get(1).contains("\"revision\":5"), bodies.get(1)));
    }

    @Test
    @DisplayName("a conflict kept with k leaves the current value")
    void configSetConflictKeepsTheCurrentValue() throws Exception {
        var bodies = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[" + valueOf("custom.timeout", "30", 4) + "]");
            } else if ("PUT".equals(request.method())) {
                bodies.add(request.bodyText());
                response.json(409, conflictOf("custom.timeout", "30", 5));
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithTyped("k\n", "config", "set", "custom.timeout", "60");
        var text = out.toString();

        assertAll(
                            () -> assertEquals(0, code, text),
                            () -> assertTrue(text.contains("No setting changed."), text),
                            () -> assertEquals(1, bodies.size(), bodies.toString()));
    }

    @Test
    @DisplayName("a conflict with --no-input leaves with 3")
    void configSetConflictWithNoInputLeavesWithThree() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[" + valueOf("custom.timeout", "30", 4) + "]");
            } else if ("PUT".equals(request.method())) {
                response.json(409, conflictOf("custom.timeout", "30", 5));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "set", "custom.timeout", "60", "--no-input");
        var text = out.toString();

        assertAll(
                            () -> assertEquals(3, code, text),
                            () -> assertTrue(text.contains("Changed meanwhile: it is now 30"), text),
                            () -> assertTrue(text.contains("--no-input stops here."), text));
    }

    @Test
    @DisplayName("config unset applies again against the revision the conflict named")
    void configUnsetConflictAppliesAgain() throws Exception {
        var bodies = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[" + valueOf("custom.flag", "on", 4) + "]");
            } else if ("DELETE".equals(request.method())) {
                bodies.add(request.bodyText());
                if (bodies.size() == 1) {
                    response.json(409, conflictOf("custom.flag", "off", 5));
                } else {
                    response.send(204, null, "");
                }
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithTyped("\n", "config", "unset", "custom.flag");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertEquals(2, bodies.size(), bodies.toString()),
                            () -> assertTrue(bodies.get(1).contains("\"revision\":5"), bodies.get(1)));
    }

    @Test
    @DisplayName("config set names a refused revision read and writes nothing")
    void configSetNamesARefusedRevisionRead() throws Exception {
        var writes = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(500, "{\"error\":\"refused\"}");
            } else if ("PUT".equals(request.method())) {
                writes.add(request.target());
                response.json(200, valueOf("custom.timeout", "60", 5));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "set", "custom.timeout", "60");

        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, code, out.toString()),
                            () -> assertTrue(out.toString().contains("Unable to load settings: HTTP 500"),
                                                out.toString()),
                            () -> assertTrue(writes.isEmpty(), writes.toString()));
    }

    @Test
    @DisplayName("config unset removes one key at one level")
    void configUnsetRemovesTheLevelRoute() throws Exception {
        var requests = new ArrayList<String>();
        fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ("GET".equals(request.method()) && "/api/v1/config".equals(request.target())) {
                response.json(200, "[" + valueOf("custom.flag", "on", 4) + "]");
            } else if ("DELETE".equals(request.method())
                                && "/api/v1/config/machine/custom.flag".equals(request.target())) {
                response.send(204, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "unset", "custom.flag");
        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains("✓ Config custom.flag unset"), out.toString()),
                            () -> assertTrue(requests.contains("DELETE /api/v1/config/machine/custom.flag"),
                                                requests.toString()));
    }

    @Test
    @DisplayName("a refused settings read names the refusal and leaves with 1")
    void aRefusedSettingsReadLeavesWithOne() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(500, "{\"error\":\"refused\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "show");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("Unable to load settings: HTTP 500"),
                                                out.toString())
        );
    }


    @Test
    @DisplayName("config set refuses only the keys the one table owns")
    void configSetOwnsOnlyTheSpecifiedKeys() throws Exception {
        var requests = new ArrayList<String>();
        fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ("GET".equals(request.method())) {
                response.json(200, "[]");
            } else if ("PUT".equals(request.method())) {
                response.json(200, "{\"key\":\"x\",\"value\":\"1\",\"origin\":\"machine\",\"revision\":1}");
            } else {
                response.send(404, null, "");
            }
        });

        int store = execute("config", "set", "credentials.store", "file");
        var refused = err.toString();
        err.getBuffer().setLength(0);
        int credential = execute("config", "set", "credentials.github", "anything");
        int roleish = execute("config", "set", "roleset", "1");

        assertAll(
                            () -> assertEquals(1, store, refused),
                            () -> assertTrue(refused.contains("pm-operator config credentials"), refused),
                            () -> assertEquals(0, credential, out.toString()),
                            () -> assertTrue(requests.contains("PUT /api/v1/config/machine/credentials.github"),
                                                requests.toString()),
                            () -> assertEquals(0, roleish, out.toString()),
                            () -> assertTrue(requests.contains("PUT /api/v1/config/machine/roleset"),
                                                requests.toString()));
    }

    @Test
    @DisplayName("config <aspect> --plan refuses the verb's other options")
    void planScopeIsExclusive() throws Exception {
        int code = execute("config", "roles", "--plan", "fix-relay", "--set", "implement=x");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains(
                                                                    "--plan cannot be combined with the other options"),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("config <aspect> --plan rejects a blank plan id instead of the machine scope")
    void blankPlanIsRejected() throws Exception {
        int code = execute("config", "roles", "--plan", "");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("--plan needs a plan id"), out.toString())
        );
    }

    @Test
    @DisplayName("config set refuses an aspect-owned key and names its verb")
    void configSetRefusesTheNewAspects() {
        int code = execute("config", "set", "models.allowed", "anthropic:claude");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("409 aspect_owned"), err.toString()),
                            () -> assertTrue(err.toString().contains("pm-operator config models"), err.toString())
        );
    }

    @Test
    @DisplayName("config set refuses aspect-owned keys with 409 aspect_owned")
    void configSetRefusesAspectOwned() {
        int code = execute("config", "set", "language.interface", "de");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("409 aspect_owned")),
                            () -> assertTrue(err.toString().contains("pm-operator config language"))
        );

        err.getBuffer().setLength(0);
        int code2 = execute("config", "set", "plan_defaults.method", "test-first");
        assertAll(
                            () -> assertEquals(1, code2),
                            () -> assertTrue(err.toString().contains("409 aspect_owned")),
                            () -> assertTrue(err.toString().contains("pm-operator config plan-defaults"))
        );
    }

    @Test
    @DisplayName("config harnesses stops with code 3 when --no-input meets the scan consent")
    void configHarnessesNoInputNeedsConsent() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, ONBOARD_COMPLETED);
            } else if ("/api/v1/onboarding/step".equals(request.target())) {
                response.json(200, request.bodyText().contains("reconfigure:harnesses")
                                    ? HARNESS_CONSENT : SETTINGS_CHOOSE);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "harnesses", "--add", "codex", "--no-input");
        assertAll(
                            () -> assertEquals(3, code, out.toString()),
                            () -> assertTrue(out.toString().contains("The run stopped at a choice it was not given"),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("config language runs the aspect's later run with its arguments as answers")
    void configLanguageRunsTheAspect() throws Exception {
        var steps = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, ONBOARD_COMPLETED);
            } else if ("/api/v1/onboarding/step".equals(request.target())) {
                var body = request.bodyText() == null ? "" : request.bodyText();
                steps.add(body);
                if (body.contains("reconfigure:language")) {
                    response.json(200, LANGUAGE_RECONFIGURE);
                } else if (body.contains("\"set\"")) {
                    response.json(200, SETTINGS_CHOOSE);
                } else {
                    response.json(200, SETTINGS_CHOOSE);
                }
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "language", "--interface", "de");
        var text = out.toString();
        assertAll(
                            () -> assertEquals(0, code, text),
                            () -> assertTrue(steps.get(0).contains("\"link\":\"settings\""), steps.toString()),
                            () -> assertTrue(steps.get(1).contains("\"link\":\"reconfigure:language\""),
                                                steps.toString()),
                            () -> assertTrue(steps.get(2).contains("\"link\":\"set\""), steps.toString()),
                            () -> assertTrue(steps.get(2).contains("\"interface\":\"de\""), steps.toString()),
                            () -> assertTrue(text.contains("✓ language run again"), text));
    }

    @Test
    @DisplayName("a config aspect carries its consent and answers a consent state with yes")
    void configHarnessesRunsTheAspect() throws Exception {
        var steps = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, ONBOARD_COMPLETED);
            } else if ("/api/v1/onboarding/step".equals(request.target())) {
                var body = request.bodyText() == null ? "" : request.bodyText();
                steps.add(body);
                if (body.contains("reconfigure:harnesses")) {
                    response.json(200, HARNESS_CONSENT);
                } else {
                    response.json(200, SETTINGS_CHOOSE);
                }
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "harnesses", "--consent", "scan_harnesses", "--no-icons");
        var text = out.toString();
        assertAll(
                            () -> assertEquals(0, code, text),
                            () -> assertTrue(steps.get(1).contains("\"link\":\"reconfigure:harnesses\""),
                                                steps.toString()),
                            () -> assertTrue(steps.get(2).contains("\"link\":\"yes\""), steps.toString()),
                            () -> assertTrue(text.contains("✓ harnesses run again"), text));
    }

    @Test
    @DisplayName("config consents shows remembered consents")
    void configConsents() throws Exception {
        fixture.start(sensibleSettings());

        int code = execute("config", "consents");
        assertEquals(0, code, out.toString());
        assertTrue(out.toString().contains("The consents you said not to ask again"), out.toString());
        assertTrue(out.toString().contains("scan_harnesses"), out.toString());
    }

    @Test
    @DisplayName("config consents names the failed settings read when there are none to list")
    void configConsentsWithoutSettings() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));

        int code = execute("config", "consents");
        assertEquals(1, code, out.toString());
        assertTrue(out.toString().contains("Unable to load settings"), out.toString());
    }

    @Test
    @DisplayName("an argument no state of the run asks for is reported, never dropped")
    void configReportsAnUnusedArgument() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, ONBOARD_COMPLETED);
            } else if ("/api/v1/onboarding/step".equals(request.target())) {
                var body = request.bodyText() == null ? "" : request.bodyText();
                response.json(200, body.contains("reconfigure:models") ? MODELS_RECONFIGURE : SETTINGS_CHOOSE);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "models", "--allow", "anthropic:claude");
        assertEquals(0, code, "out: " + out + " err: " + err);
        assertTrue(out.toString().contains("--allow was not used"), out.toString());
    }

    @Test
    @DisplayName("config documents updates documents mode")
    void configDocuments() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "documents", "--mode", "copy");
        assertEquals(0, code, out.toString());
        assertTrue(out.toString().contains("✓ Documents mode updated: copy"), out.toString());
    }

    @Test
    @DisplayName("selecting the Cleanup row opens config cleanup")
    void cleanupRowOpensTheDurations() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
            } else {
                response.send(404, null, "");
            }
        });
        execute("config");
        var index = rowIndex(out.toString(), "Cleanup");
        out.getBuffer().setLength(0);

        int code = executeWithTyped(index + "\n", "config");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains("How long should ended work be kept?"),
                                                out.toString()),
                            () -> assertTrue(out.toString().contains("Archived plans"), out.toString()));
    }

    @Test
    @DisplayName("config cleanup refuses a value under the minimum and writes nothing")
    void cleanupRefusesAValueUnderTheMinimum() throws Exception {
        var writes = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/machine/settings".equals(request.target())) {
                writes.incrementAndGet();
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "cleanup", "--plans", "3", "--yes");

        assertAll(
                            () -> assertEquals(3, code, out.toString()),
                            () -> assertTrue(err.toString().contains("At least 7 days, or never."), err.toString()),
                            () -> assertEquals(0, writes.get(), "no duration is written"));
    }

    @Test
    @DisplayName("config cleanup --yes records the machine's durations")
    void cleanupRecordsTheMachinesDurations() throws Exception {
        var posted = new AtomicReference<String>("");
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/machine/settings".equals(request.target())) {
                posted.set(request.bodyText());
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "cleanup", "--plans", "60", "--yes");

        var body = JsonTree.asObject(JsonTree.parse(posted.get()));
        var recorded = CleanupDurations.fromJson(
                            body != null ? String.valueOf(body.get("value")) : "{}");
        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains("Durations recorded"), out.toString()),
                            () -> assertEquals(60, recorded.plansAfterDays()),
                            () -> assertEquals(30, recorded.epicsAfterDays()),
                            () -> assertEquals(3, recorded.remindAfterDays()));
    }

    @Test
    @DisplayName("config cleanup --project . writes the project's own value")
    void cleanupWritesAProjectsOwnValue() throws Exception {
        var written = new AtomicReference<String>("");
        var target = new AtomicReference<String>("");
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/config".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[]");
            } else if (request.target().contains("/cleanup.plans_after_days")) {
                target.set(request.target());
                written.set(request.bodyText());
                response.json(200, "{\"key\":\"cleanup.plans_after_days\",\"revision\":1}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "cleanup", "--project", ".", "--plans", "60", "--yes");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(target.get().contains("/project/cleanup.plans_after_days"),
                                                target.get()),
                            () -> assertTrue(written.get().contains("\"value\":60"), written.get()));
    }

    @Test
    @DisplayName("the editor takes a kind's name and refuses an unreadable day count")
    void cleanupEditorTakesAKindNameAndRefusesBadDays() throws Exception {
        serveSettings(body -> {
            // left before anything is written
        });

        int code = executeWithTyped("plans\nabc\n60\n\n\n", "config", "cleanup");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains("At least 7 days, or never."), out.toString()),
                            () -> assertTrue(out.toString().contains("Durations recorded: Archived plans after 60 days."),
                                                out.toString()));
    }

    @Test
    @DisplayName("config cleanup --reports is the machine's only")
    void cleanupReportsIsMachineOnly() throws Exception {
        fixture.start((request, response) -> response.json(200, SETTINGS));

        int code = execute("config", "cleanup", "--project", "pm", "--reports", "60");

        assertAll(
                            () -> assertEquals(2, code, out.toString()),
                            () -> assertTrue(err.toString().contains("--reports"), err.toString()));
    }

    @Test
    @DisplayName("config cleanup --no-input without --yes stops at the confirmation")
    void cleanupNoInputWithoutYesStops() throws Exception {
        var writes = new AtomicInteger();
        serveSettings(body -> writes.incrementAndGet());

        int code = execute("config", "cleanup", "--plans", "60", "--no-input");

        assertAll(
                            () -> assertEquals(3, code, out.toString()),
                            () -> assertTrue(out.toString().contains("Needs a choice: --yes"), out.toString()),
                            () -> assertEquals(0, writes.get()));
    }

    @Test
    @DisplayName("config cleanup --project refuses a machine-only option")
    void cleanupProjectRefusesAMachineOnlyOption() throws Exception {
        fixture.start((request, response) -> response.json(200, SETTINGS));

        int code = execute("config", "cleanup", "--project", ".", "--remind", "4");

        assertAll(
                            () -> assertEquals(2, code, out.toString()),
                            () -> assertTrue(err.toString().contains("is the machine's only"), err.toString()));
    }

    /** A settings server that records the machine-settings write, or {@code null} when none came. */
    private void serveSettings(Consumer<String> onWrite) throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/machine/settings".equals(request.target())) {
                onWrite.accept(request.bodyText());
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("the editor keeps the durations on Enter and writes nothing")
    void cleanupEditorKeepsWhatIsThere() throws Exception {
        var writes = new AtomicInteger();
        serveSettings(body -> writes.incrementAndGet());

        int code = executeWithTyped("\n", "config", "cleanup");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals(0, writes.get(), "no duration is written"),
                            () -> assertTrue(out.toString().contains("How long should ended work be kept?"),
                                                out.toString()));
    }

    @Test
    @DisplayName("the editor changes one duration and records it on the confirmation")
    void cleanupEditorChangesADuration() throws Exception {
        var posted = new AtomicReference<String>("");
        serveSettings(posted::set);

        int code = executeWithTyped("1\n60\n\n\n", "config", "cleanup");

        var body = JsonTree.asObject(JsonTree.parse(posted.get()));
        var recorded = CleanupDurations.fromJson(body != null ? String.valueOf(body.get("value")) : "{}");
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals(60, recorded.plansAfterDays()),
                            () -> assertTrue(out.toString().contains("Durations recorded: Archived plans after 60 days."),
                                                out.toString()));
    }

    @Test
    @DisplayName("the editor explains with ? and reports an answer it does not offer")
    void cleanupEditorExplainsAndRejects() throws Exception {
        serveSettings(body -> {
            // the editor is left before anything is written
        });

        int code = executeWithTyped("?\nzz\n\n", "config", "cleanup");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains("An item is offered when its end is older"),
                                                out.toString()),
                            () -> assertTrue(out.toString().contains("is not offered"), out.toString()));
    }

    @Test
    @DisplayName("config cleanup --plans never records the never marker")
    void cleanupRecordsNever() throws Exception {
        var posted = new AtomicReference<String>("");
        serveSettings(posted::set);

        int code = execute("config", "cleanup", "--plans", "never", "--yes");

        var body = JsonTree.asObject(JsonTree.parse(posted.get()));
        var recorded = CleanupDurations.fromJson(body != null ? String.valueOf(body.get("value")) : "{}");
        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertEquals(-1, recorded.plansAfterDays()),
                            () -> assertTrue(out.toString().contains("Archived plans never."), out.toString()));
    }

    @Test
    @DisplayName("the project view shows the project's own value over the machine's")
    void cleanupProjectShowsItsOwnValue() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, SETTINGS);
            } else if (request.target().startsWith("/api/v1/config")) {
                response.json(200, """
                                    [{"key":"cleanup.plans_after_days","value":90,"origin":"project","revision":3},
                                     {"key":"custom.flag","value":"on","origin":"project","revision":4}]
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "cleanup", "--project", "pm", "--no-input");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains("Archived plans   after 90 days"), out.toString()),
                            () -> assertTrue(out.toString().contains("project pm"), out.toString()));
    }

    @Test
    @DisplayName("the overview aligns every value in one column")
    void overviewAlignsTheValues() throws Exception {
        fixture.start(sensibleSettings());

        execute("config");
        var text = out.toString();
        var language = rowOfLabel(text, "Language");
        int column = language.indexOf("English, models English");
        assertAll(
                            () -> assertTrue(column > 0, text),
                            () -> assertEquals("English, models English", valueAt(text, "Language", column), text),
                            () -> assertEquals("Claude Code 2.4.1", valueAt(text, "Harnesses", column), text),
                            () -> assertEquals("2", valueAt(text, "Remembered consents", column), text),
                            () -> assertEquals("Ask each time", valueAt(text, "Documents", column), text)
        );
    }

    @Test
    @DisplayName("the overview prints its prompt on a line of its own and offers every row it shows")
    void overviewPrintsThePromptOnItsOwnLine() throws Exception {
        fixture.start(sensibleSettings());

        execute("config");
        var text = out.toString();
        var lines = text.split("\n");
        var rows = rowsOf(text);
        int promptIndex = -1;
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].startsWith("Open: 1-")) {
                promptIndex = index;
            }
        }
        final int found = promptIndex;
        assertAll(
                            () -> assertEquals(7, rows.size(), text),
                            () -> assertTrue(found > 0, text),
                            () -> assertTrue(lines[found].startsWith("Open: 1-7"), lines[found]),
                            () -> assertTrue(lines[found - 1].stripLeading().matches("\\d+\\) .*"),
                                                lines[found - 1])
        );
    }

    private static FakeRuntimeServer.Handler sensibleSettings() {
        return (request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, DEVICES);
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"web\":{\"lan\":false}}");
            } else {
                response.send(404, null, "");
            }
        };
    }

    @Test
    @DisplayName("the overview survives a status it cannot read")
    void overviewSurvivesAMalformedStatus() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, DEVICES);
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "not a json object");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains("Paired devices"), out.toString()));
    }

    @Test
    @DisplayName("the overview collapses the remembered consents into one row with their count")
    void overviewCollapsesTheConsents() throws Exception {
        fixture.start(sensibleSettings());

        execute("config");
        var text = out.toString();
        var consents = rowOfLabel(text, "Remembered consents");

        assertAll(
                            () -> assertNotNull(consents, text),
                            () -> assertTrue(consents.stripTrailing().endsWith("2"), consents),
                            () -> assertFalse(text.contains("look for harnesses"), text),
                            () -> assertFalse(text.contains("fetch the icons"), text));
    }

    @Test
    @DisplayName("the overview shows the documents mode as the catalogue names it")
    void overviewShowsTheDocumentsModeText() throws Exception {
        fixture.start(sensibleSettings());

        execute("config");
        var documents = rowOfLabel(out.toString(), "Documents");

        assertAll(
                            () -> assertNotNull(documents, out.toString()),
                            () -> assertTrue(documents.contains("Ask each time"), documents),
                            () -> assertFalse(documents.stripTrailing().endsWith("ask"), documents));
    }

    @Test
    @DisplayName("the overview names the paired devices and the listener")
    void overviewNamesThePairedDevices() throws Exception {
        fixture.start(sensibleSettings());

        execute("config");
        var devices = rowOfLabel(out.toString(), "Paired devices");

        assertAll(
                            () -> assertNotNull(devices, out.toString()),
                            () -> assertTrue(devices.contains("3 devices \u00b7 network listener off"), devices));
    }

    @Test
    @DisplayName("the overview strips a control sequence from a runtime-served key")
    void overviewStripsAControlSequence() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[{\"key\":\"custom.\\u001B[31mkey\",\"value\":\"v\","
                                    + "\"origin\":\"machine\",\"revision\":0}]");
            } else {
                response.send(404, null, "");
            }
        });

        execute("config");
        var text = out.toString();

        assertAll(
                            () -> assertFalse(text.contains("\u001B"), text),
                            () -> assertTrue(text.contains("custom.key"), text));
    }

    @Test
    @DisplayName("the overview leaves the input on the prompt line")
    void overviewLeavesTheInputOnThePromptLine() throws Exception {
        fixture.start(sensibleSettings());

        execute("config");

        assertFalse(out.toString().endsWith("\n"), out.toString());
    }

    @Test
    @DisplayName("config show selects an aspect and renders a structured value")
    void configShowSelectsAnAspect() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "[" + PORT_VALUE + "," + OVERRIDDEN_VALUE + ","
                                    + "{\"key\":\"roles.assigned\",\"value\":{\"architect\":\"claude\"},\"origin\":\"machine\",\"revision\":1}]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "show", "plan-defaults");
        var text = out.toString();
        assertAll(
                            () -> assertEquals(0, code, text),
                            () -> assertTrue(text.contains("plan_defaults.landing"), text),
                            () -> assertFalse(text.contains("web.port"), text));

        out.getBuffer().setLength(0);
        int structured = execute("config", "show", "roles");
        assertAll(
                            () -> assertEquals(0, structured, out.toString()),
                            () -> assertTrue(out.toString().contains("architect"), out.toString()));
    }

    @Test
    @DisplayName("config names a 200 body it cannot read as unreadable")
    void configNamesAnUnreadableBody() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "\"not a value list\"");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");
        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, code),
                            () -> assertTrue(out.toString().contains("did not report its configuration values"),
                                                out.toString()));
    }

    @Test
    @DisplayName("config set and unset write at the project and plan level")
    void configWritesAtTheProjectAndPlanLevel() throws Exception {
        var requests = new ArrayList<String>();
        fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ("GET".equals(request.method()) && request.target().startsWith("/api/v1/config")) {
                response.json(200, "[" + valueOf("custom.timeout", "30", 4) + "]");
            } else {
                response.json(200, "{\"key\":\"custom.timeout\",\"value\":\"60\",\"origin\":\"project\"}");
            }
        });

        execute("config", "set", "custom.timeout", "60", "--project", "fix-relay");
        execute("config", "unset", "custom.timeout", "--plan", "p-7");

        assertAll(
                            () -> assertTrue(requests.contains(
                                                                    "PUT /api/v1/config/project/custom.timeout?project=fix-relay"),
                                                requests.toString()),
                            () -> assertTrue(requests.contains(
                                                                    "DELETE /api/v1/config/plan/custom.timeout?plan=p-7"),
                                                requests.toString()));
    }

    @Test
    @DisplayName("config set refuses two levels at once")
    void configRefusesTwoLevels() {
        int machineAndProject = execute("config", "set", "--machine", "--project", "p", "custom.key", "1");
        var text = out.toString();
        out.getBuffer().setLength(0);
        int both = execute("config", "unset", "custom.key", "--project", "p", "--plan", "q");

        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, machineAndProject, text),
                            () -> assertTrue(text.contains("Name one level"), text),
                            () -> assertEquals(PmOperator.EXIT_FAILURE, both, out.toString()));
    }

    @Test
    @DisplayName("the overview shortens a long label and right-aligns a two-digit index")
    void overviewShortensALongLabel() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, manyAspects());
            } else {
                response.send(404, null, "");
            }
        });

        execute("config");
        var text = out.toString();

        assertAll(
                            () -> assertTrue(text.contains("\u2026"), text),
                            () -> assertTrue(text.contains("  10)"), text));
    }

    @Test
    @DisplayName("config documents names the failed settings read")
    void configDocumentsWithoutSettings() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));

        int code = execute("config", "documents", "--mode", "copy");
        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, code),
                            () -> assertTrue(out.toString().contains("documents mode is a machine setting"),
                                                out.toString()));
    }

    @Test
    @DisplayName("config reports an unused consent and an unused confirmation")
    void configReportsAnUnusedConsentAndConfirmation() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, ONBOARD_COMPLETED);
            } else if ("/api/v1/onboarding/step".equals(request.target())) {
                var body = request.bodyText() == null ? "" : request.bodyText();
                response.json(200, body.contains("reconfigure:harnesses") ? HARNESS_CONSENT : SETTINGS_CHOOSE);
            } else {
                response.send(404, null, "");
            }
        });

        execute("config", "harnesses", "--consent", "unknown_kind", "--no-icons");
        var text = out.toString();
        assertAll(
                            () -> assertTrue(text.contains("--consent unknown_kind was not used"), text),
                            () -> assertTrue(text.contains("--no-icons was not used"), text));
    }

    @Test
    @DisplayName("a run that cannot reach the machine settings names the failure")
    void configAspectWithoutTheSettingsLink() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, "{\"state\":{\"state_id\":\"language.choose\",\"kind\":\"operator_wait\"},"
                                    + "\"links\":[{\"id\":\"set\",\"rel\":\"submit\",\"preselected\":true}]}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "language");
        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, code, err.toString()),
                            () -> assertTrue(err.toString().contains("The run of language failed"), err.toString()));
    }

    @Test
    @DisplayName("config show reads a single value the runtime serves alone")
    void configShowReadsASingleValue() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, PORT_VALUE);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "show");
        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains("web.port"), out.toString()));
    }

    @Test
    @DisplayName("every aspect's verb is a registered config subcommand")
    void everyAspectVerbIsRegistered() {
        var declared = ConfigCommand.class.getAnnotation(Command.class);
        var subcommands = new HashSet<String>();
        for (var type : declared.subcommands()) {
            subcommands.add(type.getAnnotation(Command.class).name());
        }

        for (var aspect : OnboardingAspect.values()) {
            var verb = ConfigCommand.verbOf(aspect);
            assertTrue(subcommands.contains(verb), aspect + " runs '" + verb + "', which is not a subcommand");
        }
    }

    private static String valueOf(String key, String value, int revision) {
        return "{\"key\":\"" + key + "\",\"value\":\"" + value + "\",\"origin\":\"machine\",\"revision\":"
                            + revision + "}";
    }

    private static String conflictOf(String key, String value, int revision) {
        return "{\"key\":\"" + key + "\",\"value\":\"" + value + "\",\"origin\":\"machine\",\"revision\":"
                            + revision + ",\"error\":\"conflict\"}";
    }

    private static String manyAspects() {
        var aspects = new StringBuilder();
        for (int index = 1; index <= 10; index++) {
            if (index > 1) {
                aspects.append(',');
            }
            aspects.append("{\"aspect\":\"language\",\"step\":").append(index)
                                .append(",\"summary\":\"A summary of the aspect\"}");
        }
        return "{\"revision\":4,\"aspects\":[" + aspects + "],"
                            + "\"credentials\":[{\"key\":\"a-very-long-credential-key-name\",\"provider\":\"GitHub\","
                            + "\"kind\":\"fine-grained\",\"expires_in_days\":9,\"expired\":false}],"
                            + "\"cleanup_durations\":{\"plans_after_days\":30,\"epics_after_days\":30,"
                            + "\"logs_after_days\":30,\"reports_after_days\":30,\"unenrolled_after_days\":30,"
                            + "\"remind_after_days\":3},\"documents_mode\":\"ask\"}";
    }

    private static List<String> rowsOf(String text) {
        var rows = new ArrayList<String>();
        for (var line : text.split("\n")) {
            if (line.stripLeading().matches("\\d+\\) .*")) {
                rows.add(line);
            }
        }
        return rows;
    }

    private static String rowOfLabel(String text, String label) {
        for (var line : text.split("\n")) {
            if (line.stripLeading().matches("\\d+\\) " + Pattern.quote(label) + "\\s+.*")) {
                return line;
            }
        }
        return null;
    }

    /**
     * @param text  the printed overview
     * @param label the row's label
     * @return the number the row is offered under, or {@code -1} when no row names the label
     */
    private static int rowIndex(String text, String label) {
        for (var line : text.split("\n")) {
            var stripped = line.stripLeading();
            if (stripped.matches("\\d+\\) " + Pattern.quote(label) + "\\s+.*")) {
                return Integer.parseInt(stripped.substring(0, stripped.indexOf(')')));
            }
        }
        return -1;
    }

    private static String rowOf(String text, String key) {
        for (var line : text.split("\n")) {
            if (line.stripLeading().startsWith(key + " ")) {
                return line;
            }
        }
        return null;
    }

    private static String valueAt(String text, String label, int column) {
        for (var line : text.split("\n")) {
            var stripped = line.stripLeading();
            if (stripped.matches("\\d+\\) " + Pattern.quote(label) + "\\s+.*")) {
                return line.substring(Math.min(column, line.length())).stripTrailing();
            }
        }
        return null;
    }

    private static final String SETTINGS = """
                        {"revision":4,
                         "aspects":[{"aspect":"language","step":1,"summary":"English, models English"},
                                    {"aspect":"harness","step":2,"summary":"Claude Code 2.4.1"}],
                         "language_interface":"de","language_model":"en","documents_mode":"ask",
                         "cleanup_durations":{"plans_after_days":30,"epics_after_days":30,"logs_after_days":30,"reports_after_days":30,"unenrolled_after_days":30,"remind_after_days":3},
                         "plan_defaults":{"landing":"landing.pull-request","method":"method.test-first","asking":"ask.key-points"},
                         "consents":[{"kind":"scan_harnesses","status":"remembered","remember":true,"channel":"terminal"},
                                     {"kind":"fetch_icons","status":"remembered","remember":true,"channel":"terminal"}],
                         "credentials":[{"key":"github","provider":"GitHub","kind":"fine-grained","expires_in_days":9,"expired":false}]}
                        """;

    private static final String PORT_VALUE =
                        "{\"key\":\"web.port\",\"value\":7420,\"origin\":\"machine\",\"revision\":4}";
    private static final String DEVICES = """
                        [{"id":"d-1","label":"mbp-oliver"},{"id":"d-2","label":"iPad"},{"id":"d-3","label":"Firefox"}]
                        """;
    private static final String FLAG_VALUE =
                        "{\"key\":\"console.full_screen\",\"value\":true,\"origin\":\"default\",\"revision\":0}";
    private static final String OVERRIDDEN_VALUE = "{\"key\":\"plan_defaults.landing\","
                        + "\"value\":\"landing.pull-request\",\"origin\":\"team\",\"overrides\":\"machine\",\"revision\":0}";

    private static final String ONBOARD_COMPLETED = """
                        {"state":{"state_id":"onboard.completed","kind":"completed","level":"machine"},
                         "links":[{"id":"settings","rel":"next","method":"POST","href":"/api/v1/onboarding/settings"}]}
                        """;
    private static final String SETTINGS_CHOOSE = """
                        {"state":{"state_id":"settings.choose","kind":"operator_wait","level":"machine"},
                         "links":[{"id":"reconfigure:language","rel":"alt","method":"POST","href":"/api/v1/onboarding/settings/reconfigure/language"},
                                  {"id":"reconfigure:harnesses","rel":"alt","method":"POST","href":"/api/v1/onboarding/settings/reconfigure/harnesses"},
                                  {"id":"reconfigure:models","rel":"alt","method":"POST","href":"/api/v1/onboarding/settings/reconfigure/models"},
                                  {"id":"done","rel":"next","method":"POST","href":"/api/v1/onboarding/settings/done","preselected":true}]}
                        """;
    private static final String LANGUAGE_RECONFIGURE = """
                        {"state":{"state_id":"language.reconfigure","kind":"operator_wait","level":"machine"},
                         "facts":{"summary":"The languages run again","aspect":"language","interface":"en","model":"same"},
                         "links":[{"id":"set","rel":"submit","method":"POST","href":"/api/v1/onboarding/language/set","preselected":true}]}
                        """;
    private static final String MODELS_RECONFIGURE = """
                        {"state":{"state_id":"catalogue.reconfigure","kind":"operator_wait","level":"machine"},
                         "facts":{"summary":"The models run again","aspect":"catalogue"},
                         "links":[{"id":"set","rel":"submit","method":"POST","href":"/api/v1/onboarding/catalogue/set","preselected":true}]}
                        """;
    private static final String HARNESS_CONSENT = """
                        {"state":{"state_id":"harness.consent","kind":"operator_wait","level":"machine"},
                         "facts":{"summary":"Look for harnesses?","kind":"scan_harnesses"},
                         "links":[{"id":"no","rel":"alt","method":"POST","href":"/api/v1/onboarding/harness/decline"},
                                  {"id":"yes","rel":"next","method":"POST","href":"/api/v1/onboarding/harness/scan","preselected":true}]}
                        """;
}
