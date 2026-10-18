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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs a command on a pseudo terminal, so that a client process sees a real interactive terminal.
 * The JVM transcript tests use the test seam of {@link TerminalTestDriver}; the native binary has
 * no such seam, so a native transcript test needs a terminal that the operating system provides.
 * {@code script(1)} allocates one and forwards the child's terminal to this process; the test
 * needs no pseudo-terminal library of its own.
 */
final class PseudoTerminal {

    private static final String SCRIPT = "/usr/bin/script";

    private PseudoTerminal() {
    }

    /**
     * @param command          the command line to run on the pseudo terminal
     * @param environment      the complete environment of the child
     * @param workingDirectory the working directory of the child
     * @return everything the child rendered on its terminal
     * @throws IOException          if the command cannot be started
     * @throws InterruptedException if interrupted while waiting
     */
    static String run(List<String> command, Map<String, String> environment, Path workingDirectory)
                        throws IOException, InterruptedException {
        var builder = new ProcessBuilder(onPseudoTerminal(command)).directory(workingDirectory.toFile());
        builder.environment().clear();
        builder.environment().putAll(environment);
        var process = builder.start();
        try (var stdin = process.getOutputStream()) {
            var rendered = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("the command exited with " + exitCode + ":\n" + rendered);
            }
            return rendered;
        }
    }

    private static List<String> onPseudoTerminal(List<String> command) {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("bsd")) {
            var wrapped = new ArrayList<String>();
            wrapped.add(SCRIPT);
            wrapped.add("-q");
            wrapped.add("/dev/null");
            wrapped.addAll(command);
            return wrapped;
        }
        var joined = command.stream().map(PseudoTerminal::quote).collect(Collectors.joining(" "));
        return List.of(SCRIPT, "-q", "-e", "-c", joined, "/dev/null");
    }

    private static String quote(String part) {
        return "'" + part.replace("'", "'\\''") + "'";
    }
}
