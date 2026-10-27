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

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.operator.PmOperator;

import org.junit.jupiter.api.Assumptions;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * One acting dialogue on a real pseudo-terminal against the mock runtime (CLI-CC-S-01, CLI-CC-P-07):
 * it starts the mock with a scenario, hands the operator process a cleared environment, and reads
 * the acts the process sent from {@code GET /mock/acts}, so a case can prove that an act reached the
 * runtime rather than only the screen.
 */
final class DialogueHarness implements AutoCloseable {

    /** The wait every dialogue case allows its process to reach the next state. */
    static final Duration TIMEOUT = Duration.ofSeconds(20);

    private static final String SCRIPT = "/usr/bin/script";

    private final MockRuntime runtime;
    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    /**
     * One operator act the mock recorded.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @param state  the state the request was sent in
     */
    record Act(String method, String path, String state) {
    }

    private DialogueHarness(MockRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Starts the mock on the scenario.
     *
     * @param scenario the standard scenario name
     * @return the harness, to be closed with the test
     * @throws IOException if the mock cannot start
     */
    static DialogueHarness start(String scenario) throws IOException {
        Assumptions.assumeTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not available");
        var runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(scenario).build());
        try {
            MockMachine.recordLiveRuntime(runtime);
            return new DialogueHarness(runtime);
        } catch (IOException e) {
            runtime.close();
            throw e;
        }
    }

    /**
     * Moves the mock to the state the dialogue is offered in.
     *
     * @param stateId the state of the active scenario
     * @return this harness
     */
    DialogueHarness goTo(String stateId) {
        try {
            runtime.goToState(stateId);
            return this;
        } catch (IllegalArgumentException e) {
            runtime.close();
            throw e;
        }
    }

    /**
     * @param arguments the operator verb and its arguments
     * @return the running session on a 120 x 30 pseudo-terminal
     * @throws IOException if the process cannot be started
     */
    PtySession open(String... arguments) throws IOException {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "dialogue-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        // The views keep a saved selection outside the machine base; keep it in the test's tree.
        environment.put("XDG_STATE_HOME", runtime.base().resolve("xdg-state").toString());
        return PtySession.start(binary.with(arguments), environment, Path.of("").toAbsolutePath(), 120, 30);
    }

    /**
     * @return the acts the mock recorded since the scenario started, oldest first
     * @throws IOException if the read fails
     */
    List<Act> acts() throws IOException {
        var response = runtime.client().send(HttpRequest.get("/mock/acts"));
        if (response.status() != 200) {
            throw new IllegalStateException("GET /mock/acts answered " + response.status()
                                + ": " + response.bodyText());
        }
        var body = JsonTree.asObject(JsonTree.parse(response.bodyText()));
        var acts = new ArrayList<Act>();
        if (body != null && body.get("acts") instanceof List<?> entries) {
            for (var entry : entries) {
                var map = JsonTree.asObject(entry);
                if (map != null) {
                    acts.add(new Act(JsonTree.string(map, "method"), JsonTree.string(map, "path"),
                                        JsonTree.string(map, "state")));
                }
            }
        }
        return acts;
    }

    /**
     * Asserts that the runtime received no act at all.
     *
     * @throws IOException if the read fails
     */
    void assertNoAct() throws IOException {
        var recorded = acts();
        assertTrue(recorded.isEmpty(), "expected no act at the runtime, got " + recorded);
    }

    /**
     * Asserts that the runtime received at least one such act.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @throws IOException if the read fails
     */
    void assertAct(String method, String path) throws IOException {
        var recorded = acts();
        assertTrue(recorded.stream().anyMatch(act -> method.equals(act.method()) && path.equals(act.path())),
                            "expected " + method + " " + path + ", got " + recorded);
    }

    /**
     * Types the keys into the session. A dialogue that already left before it could read them is
     * reported: the case is about whether it left with nothing sent, so a send into a closed session
     * fails the test rather than hiding a dialogue that never consumed the key.
     *
     * @param session the running session
     * @param keys    the keys to type
     * @throws IOException if the dialogue already left
     */
    static void type(PtySession session, String keys) throws IOException {
        session.send(keys);
    }

    /**
     * @param text the screen text
     * @param part the text to count
     * @return how often the text carries the part
     */
    static int times(String text, String part) {
        var count = 0;
        var index = text.indexOf(part);
        while (index >= 0) {
            count++;
            index = text.indexOf(part, index + part.length());
        }
        return count;
    }

    @Override
    public void close() {
        runtime.close();
    }
}
