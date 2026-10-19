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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.jline.terminal.Terminal;

import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.OperatorContext;

/**
 * Drives a pm-operator dialogue in a test: it types a script into a JLine terminal, captures
 * everything the program renders on that terminal, and supplies an {@link OperatorContext} whose
 * terminal provider is the scripted terminal and whose interactivity flag is set for this test
 * alone. Production never sees the flag, because it comes from {@link OperatorContext#system()},
 * which reads the real console.
 *
 * <p>A test types the whole script before the dialogue starts. The driver is deliberately not
 * thread-safe: a transcript is a sequence, and a test that typed from another thread could not
 * say what the dialogue saw.
 */
public final class TerminalTestDriver implements AutoCloseable {

    private static final OperatorContext.BrowserOpener NO_BROWSER = _ -> {
        throw new IOException("a transcript test opens no browser");
    };

    private final ScriptedInput input = new ScriptedInput();
    private final ByteArrayOutputStream rendered = new ByteArrayOutputStream();
    private final TranscriptTerminal terminal;
    private OperatorContext context;

    private TerminalTestDriver() throws IOException {
        terminal = new TranscriptTerminal(input, rendered);
    }

    /**
     * @return a driver with an empty script and an empty capture
     * @throws IOException if the terminal cannot be created
     */
    public static TerminalTestDriver create() throws IOException {
        return new TerminalTestDriver();
    }

    /**
     * @param keys the keys to type verbatim, for example a control character
     * @return this driver
     */
    public TerminalTestDriver type(String keys) {
        input.type(keys);
        return this;
    }

    /**
     * @param line the line to type, followed by {@code Enter}
     * @return this driver
     */
    public TerminalTestDriver typeLine(String line) {
        return type(line + "\n");
    }

    /**
     * @return this driver, after pressing {@code Enter} on an empty line
     */
    public TerminalTestDriver pressEnter() {
        return type("\n");
    }

    /**
     * Ends the script, so that a dialogue which reads past it sees an end of file.
     *
     * @return this driver
     */
    public TerminalTestDriver endOfInput() {
        input.endOfScript();
        return this;
    }

    /**
     * Binds the driver to an operator context over the given environment and home directory. The
     * context is interactive only here: the flag is passed by this test, never read from a process.
     *
     * @param environment the process environment, usually {@code PM_MCP_BASE} of a mock runtime
     * @param userHome    the user's home directory
     * @return this driver
     */
    public TerminalTestDriver project(Map<String, String> environment, Path userHome) {
        this.context = new OperatorContext(environment, userHome, true,
                            ClientEnvironment.capture(environment), this::sharedTerminal, NO_BROWSER);
        return this;
    }

    /**
     * A dialogue that opens its own terminal for one prompt closes it again; production builds a
     * fresh terminal for that call, but a transcript has exactly one, shared with the console. This
     * returns a view of the transcript terminal whose {@code close} is a no-op, so a nested prompt
     * does not end the transcript, while every other call is forwarded to it.
     *
     * @return the shared terminal, without a closing effect
     */
    private Terminal sharedTerminal() {
        return (Terminal) java.lang.reflect.Proxy.newProxyInstance(
                            Terminal.class.getClassLoader(), new Class<?>[]{Terminal.class},
                            (proxy, method, args) -> {
                                if ("close".equals(method.getName())) {
                                    terminal.writer().flush();
                                    return null;
                                }
                                try {
                                    return method.invoke(terminal, args);
                                } catch (java.lang.reflect.InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            });
    }

    /**
     * @return the operator context of the dialogue
     * @throws IllegalStateException if {@link #project(Map, Path)} was not called
     */
    public OperatorContext context() {
        if (context == null) {
            throw new IllegalStateException("project(environment, userHome) was not called");
        }
        return context;
    }

    /**
     * @return the JLine terminal the dialogue reads from and writes to
     */
    public Terminal terminal() {
        return terminal;
    }

    /**
     * @return the writer of the dialogue; standard output and standard error are captured on the
     *         same terminal, as they are on a real one
     */
    public PrintWriter writer() {
        return new PrintWriter(new OutputStreamWriter(rendered, StandardCharsets.UTF_8), true);
    }

    /**
     * @return everything the dialogue rendered so far
     */
    public String render() {
        terminal.writer().flush();
        return rendered.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }
}
