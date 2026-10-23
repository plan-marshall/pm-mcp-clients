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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import de.planmarshall.operator.tui.AnsiScreen;

/**
 * An interactive process on a real pseudo-terminal: {@code script(1)} allocates the terminal, the
 * session pins its size with {@code stty}, keeps stdin open so a test can inject keys mid-run,
 * accumulates everything the process writes, and parses it with {@link AnsiScreen}.
 *
 * <p>Unlike {@link PseudoTerminal}, which runs a command to completion and reads all of it, this
 * session lives as long as the test drives it: {@link #awaitText(String, Duration)} waits for a
 * state on the parsed screen, {@link #send(String)} types into the running process, and
 * {@link #awaitExit(Duration)} plus {@link #exitCode()} record how it left. The child's exit status
 * is written to a side file, because the macOS {@code script} does not propagate it.
 */
public final class PtySession implements AutoCloseable {

    private static final String SCRIPT = "/usr/bin/script";
    private static final String SHELL = "/bin/sh";
    private static final Duration POLL = Duration.ofMillis(25);

    private final Process process;
    private final OutputStream stdin;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Thread reader;
    private final Path statusFile;
    private final int columns;
    private final int rows;
    private final boolean bsdScript;
    private volatile boolean closed;

    private PtySession(Process process, Path statusFile, int columns, int rows) {
        this.process = process;
        this.stdin = process.getOutputStream();
        this.statusFile = statusFile;
        this.columns = columns;
        this.rows = rows;
        this.bsdScript = isBsdScript();
        this.reader = Thread.ofPlatform().daemon().name("pty-session-reader").start(this::readOutput);
    }

    /**
     * @param command  the command line to run on the pseudo-terminal
     * @param env      the complete environment of the child
     * @param cwd      the working directory of the child
     * @param columns  the width of the pseudo-terminal
     * @param rows     the height of the pseudo-terminal
     * @return the started session
     * @throws IOException if {@code script} or the child cannot be started
     */
    public static PtySession start(List<String> command, Map<String, String> env, Path cwd,
                        int columns, int rows) throws IOException {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(env, "env");
        Objects.requireNonNull(cwd, "cwd");
        var statusFile = Files.createTempFile("pm-pty-status", ".txt");
        try {
            var builder = new ProcessBuilder(onPseudoTerminal(command, statusFile, columns, rows))
                                .directory(cwd.toFile());
            builder.environment().clear();
            builder.environment().putAll(env);
            return new PtySession(builder.start(), statusFile, columns, rows);
        } catch (IOException e) {
            Files.deleteIfExists(statusFile);
            throw e;
        }
    }

    /**
     * Types the given keys into the running process and flushes them.
     *
     * @param keys the keys to type verbatim
     * @throws IOException if the process no longer accepts input
     */
    public synchronized void send(String keys) throws IOException {
        if (closed) {
            throw new IOException("the pseudo-terminal session is closed");
        }
        stdin.write(keys.getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    /**
     * Waits until the parsed screen contains the text.
     *
     * @param text    the text to look for
     * @param timeout the maximum wait
     * @return {@code true} when the text appeared, {@code false} on the timeout or an ended process
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitText(String text, Duration timeout) throws InterruptedException {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(timeout, "timeout");
        var deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            if (contains(text)) {
                return true;
            }
            if (!process.isAlive() || System.nanoTime() >= deadline) {
                return contains(text);
            }
            Thread.sleep(POLL.toMillis());
        }
    }

    /**
     * Waits for the process to end.
     *
     * @param timeout the maximum wait
     * @return {@code true} when the process ended
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitExit(Duration timeout) throws InterruptedException {
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            return false;
        }
        reader.join(2000L);
        return true;
    }

    /**
     * @return the child's exit code, or {@code null} while it still runs
     */
    public Integer exitCode() {
        if (process.isAlive()) {
            return null;
        }
        var recorded = childExit();
        if (recorded != null) {
            return recorded;
        }
        // macOS script(1) does not propagate the child status; a missing side file there is an
        // abnormal run and fails closed. util-linux script -e propagates it itself.
        return bsdScript ? -1 : process.exitValue();
    }

    /** @return everything the process wrote so far */
    public String rawOutput() {
        synchronized (output) {
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    /** @return the screen as it stands now */
    public AnsiScreen screen() {
        return AnsiScreen.parse(rawOutput(), columns, rows);
    }

    /** @return how often the process entered the alternate screen */
    public int alternateScreenEntries() {
        return screen().alternation().entries();
    }

    /** @return how often the process left the alternate screen */
    public int alternateScreenExits() {
        return screen().alternation().exits();
    }

    /** @return {@code true} while the alternate screen is active */
    public boolean alternateScreenActive() {
        return screen().alternateScreenActive();
    }

    /** @return {@code true} while the cursor is visible */
    public boolean cursorVisible() {
        return screen().cursorVisible();
    }

    /**
     * @return the screen's lines joined for an assertion message
     */
    public String screenText() {
        return String.join("\n", screen().lines());
    }

    private boolean contains(String text) {
        return screen().lines().stream().anyMatch(line -> line.contains(text));
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            stdin.close();
        } catch (IOException _) {
            // The child may already be gone; the terminal is cleaned up below either way.
        }
        process.destroyForcibly();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
        reader.interrupt();
        try {
            Files.deleteIfExists(statusFile);
        } catch (IOException _) {
            // Best-effort cleanup of the status side file.
        }
    }

    private void readOutput() {
        var buffer = new byte[4096];
        try (var in = process.getInputStream()) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                synchronized (output) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (IOException _) {
            // The process ended or the stream closed; what was read is on the screen.
        }
    }

    /**
     * @return the recorded child status, or {@code null} when it is absent or unreadable
     */
    private Integer childExit() {
        try {
            var text = Files.readString(statusFile, StandardCharsets.UTF_8).strip();
            return text.isEmpty() ? null : Integer.parseInt(text);
        } catch (IOException | NumberFormatException _) {
            return null;
        }
    }

    private static List<String> onPseudoTerminal(List<String> command, Path statusFile, int columns, int rows) {
        var inner = "stty rows " + rows + " cols " + columns + " 2>/dev/null; "
                            + command.stream().map(PtySession::quote).collect(Collectors.joining(" "))
                            + "; s=$?; printf %s \"$s\" > " + quote(statusFile.toString()) + "; exit \"$s\"";
        var wrapped = new ArrayList<String>();
        wrapped.add(SCRIPT);
        wrapped.add("-q");
        if (isBsdScript()) {
            wrapped.add("/dev/null");
            wrapped.add(SHELL);
            wrapped.add("-c");
            wrapped.add(inner);
        } else {
            wrapped.add("-e");
            wrapped.add("-c");
            wrapped.add(inner);
            wrapped.add("/dev/null");
        }
        return wrapped;
    }

    private static boolean isBsdScript() {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("mac") || os.contains("bsd");
    }

    private static String quote(String part) {
        return "'" + part.replace("'", "'\\''") + "'";
    }
}
