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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.SseEvent;
import de.planmarshall.api.json.JsonTree;

/**
 * The operator's interactive console REPL (04-console.adoc#console).
 * Manages badged prompts, pushed runtime events via {@code LineReader.printAbove(...)},
 * history persistence, and built-in navigation/control verbs.
 */
public final class PmConsole {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final OperatorContext context;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final List<String> badges = new CopyOnWriteArrayList<>();

    private volatile String activePairingCode;
    private volatile String activePairingUrl;
    private volatile int webPort = EntryFlow.DEFAULT_WEB_PORT;

    /**
     * @param context the operator context
     */
    public PmConsole(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Runs the console entry flow and main REPL loop.
     *
     * @return exit code (0 on normal exit)
     * @throws Exception on fatal console or terminal error
     */
    public int run() throws Exception {
        var terminal = context.createTerminal();
        var out = new PrintWriter(terminal.writer(), true);

        var historyFile = context.stateDirectory().historyFile();
        try {
            context.stateDirectory().ensureFile(historyFile);
        } catch (IOException e) {
            // best-effort history file
        }

        var completer = new StringsCompleter(
                "exit", "quit", "help", "status", "plans", "plan", "epic", "questions", "answer",
                "web", "runtime", "config", "projects", "project", "client", "drain"
        );

        var reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .variable(LineReader.HISTORY_FILE, historyFile)
                .build();

        var entryFlow = new EntryFlow(context);
        var entryResult = entryFlow.run(out, reader);

        activePairingCode = entryResult.activeCode();
        activePairingUrl = entryResult.activeUrl();
        webPort = EntryFlow.getWebPort(entryResult.status());
        badges.addAll(entryResult.initialBadges());

        Thread sseThread = startEventListener(reader);

        try {
            runRepl(out, reader);
        } finally {
            running.set(false);
            if (sseThread != null) {
                sseThread.interrupt();
            }
            out.flush();
        }

        return 0;
    }

    private void runRepl(PrintWriter out, LineReader reader) {
        while (running.get()) {
            var prompt = ConsolePrompt.format(badges, reconnecting.get());
            String line;
            try {
                line = reader.readLine(prompt);
            } catch (UserInterruptException e) {
                // Ctrl+C: cancel current line and redisplay prompt
                continue;
            } catch (EndOfFileException e) {
                // Ctrl+D: exit console
                break;
            }
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                handleEmptyEnter(out, reader);
                continue;
            }
            if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                break;
            }
            dispatchCommand(line, out, reader);
        }
    }

    private void handleEmptyEnter(PrintWriter out, LineReader reader) {
        boolean hasSetup = false;
        for (var b : badges) {
            if (b.startsWith("setup")) {
                hasSetup = true;
                break;
            }
        }
        if (hasSetup) {
            out.println("Set up this machine for plan-marshall");
            out.flush();
            String beginChoice = null;
            try {
                beginChoice = reader.readLine("Begin with step 1? [Enter]   a) decide everything for me   ?   › ");
            } catch (EndOfFileException | UserInterruptException e) {
                // abort continuation on interrupt/EOF
            }
            if (beginChoice != null && "q".equalsIgnoreCase(beginChoice.trim())) {
                out.println("Left the setup before step 1. Nothing was sent. Enter continues the setup.");
                out.flush();
            }
        }
    }

    private void dispatchCommand(String line, PrintWriter out, LineReader reader) {
        var parts = line.split("\\s+");
        var verb = parts[0];

        switch (verb) {
            case "help" -> printHelp(out, parts);
            case "plans" -> PmOperator.execute(context, out, out, "plan");
            case "web" -> handleWebCommand(out, parts);
            default -> {
                var args = parseArgs(line);
                PmOperator.execute(context, out, out, args.toArray(new String[0]));
            }
        }
        out.flush();
    }

    private void handleWebCommand(PrintWriter out, String[] parts) {
        if (parts.length >= 2 && "qr".equals(parts[1])) {
            printWebQr(out);
            return;
        }
        if (parts.length >= 3 && "copy".equals(parts[1]) && "ssh".equals(parts[2])) {
            copySshCommand(out);
            return;
        }
        if (parts.length >= 2 && "pair".equals(parts[1])) {
            issueNewPairingCode(out);
            return;
        }
        var args = parseArgs(String.join(" ", parts));
        PmOperator.execute(context, out, out, args.toArray(new String[0]));
    }

    private void printWebQr(PrintWriter out) {
        var url = activePairingUrl != null ? activePairingUrl : "http://127.0.0.1:" + webPort;
        var validUntil = LocalTime.now().plusMinutes(10).format(TIME_FORMATTER);
        var sideLines = new ArrayList<String>();
        sideLines.add(url);
        if (activePairingCode != null) {
            sideLines.add("Code " + activePairingCode + ", valid until " + validUntil);
        }
        out.println(QrCodeRenderer.render(url, sideLines));
    }

    private void copySshCommand(PrintWriter out) {
        var user = context.environment().getOrDefault("USER", System.getProperty("user.name", "user"));
        var host = getHostname();
        var sshCmd = String.format("ssh -L %d:127.0.0.1:%d %s@%s", webPort, webPort, user, host);
        // ANSI OSC 52 clipboard escape sequence
        var base64 = Base64.getEncoder().encodeToString(sshCmd.getBytes(StandardCharsets.UTF_8));
        out.print("\033]52;c;" + base64 + "\007");
        out.flush();
        out.println("✓ Copied, if your terminal allows it; the text is above.");
    }

    private void issueNewPairingCode(PrintWriter out) {
        try {
            var pairing = new EntryFlow(context).issuePairingCode(context.runtime(true), webPort);
            activePairingCode = pairing.code();
            activePairingUrl = pairing.url();
            var validUntil = LocalTime.now().plusMinutes(10).format(TIME_FORMATTER);
            out.printf("✓ New code: %s, valid until %s. The previous code no longer works.%n",
                    pairing.code(), validUntil);
            out.printf("  %s%n", pairing.url());
        } catch (IOException e) {
            out.println("✗ Failed to issue new pairing code: " + e.getMessage());
        }
    }


    private void printHelp(PrintWriter out, String[] parts) {
        if (parts.length >= 2) {
            out.println("Help for " + parts[1] + ": run 'pm-operator " + parts[1] + " --help'");
            return;
        }
        out.println("Commands:");
        out.println("  plans                  overview of running, held, and paused plans");
        out.println("  plan <id>              details and actions of a plan");
        out.println("  epic <id>              details and actions of an epic");
        out.println("  questions              open questions and waiver requests");
        out.println("  answer <id> [<opt>]    answer a question or waiver request");
        out.println("  status                 runtime and listener status");
        out.println("  web [pair|qr|copy]     web app pairing and access");
        out.println("  exit                   leave the console");
        out.println("Any operator verb (runtime, config, projects, client, etc.) can be typed here.");
    }

    private Thread startEventListener(LineReader reader) {
        var thread = new Thread(() -> {
            while (running.get()) {
                try {
                    var runtime = context.runtime(false);
                    try (var exchange = runtime.open(HttpRequest.get("/api/v1/events"))) {
                        reconnecting.set(false);
                        var sseReader = exchange.events();
                        SseEvent event;
                        while (running.get() && (event = sseReader.next()) != null) {
                            handleSseEvent(event, reader);
                        }
                    }
                } catch (IOException e) {
                    if (!running.get()) {
                        break;
                    }
                    reconnecting.set(true);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "pm-console-sse");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void handleSseEvent(SseEvent event, LineReader reader) {
        if (event == null || event.data() == null || event.data().isBlank()) {
            return;
        }
        try {
            var parsed = JsonTree.parse(event.data());
            if (parsed instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                var eventMap = (Map<String, Object>) map;
                var notice = ConsoleNotice.fromEventMap(eventMap);
                if (notice != null) {
                    reader.printAbove(notice);
                }
                var type = String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));
                if ("paired".equals(type)) {
                    var label = String.valueOf(eventMap.getOrDefault("label", "local"));
                    reader.printAbove("✓ Paired: " + label);
                } else if ("fresh_code".equals(type)) {
                    activePairingCode = (String) eventMap.get("code");
                    activePairingUrl = (String) eventMap.get("url");
                }
            }
        }
        /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (Exception e) {
            // invalid json in event stream
        }
    }

    /**
     * Reads a password or secret masked with {@code '*'} without echo.
     *
     * @param reader the line reader
     * @param prompt the prompt message
     * @return the entered secret
     */
    public static String readPassword(LineReader reader, String prompt) {
        return Objects.requireNonNull(reader, "reader").readLine(prompt, '*');
    }

    private static List<String> parseArgs(String line) {
        var args = new ArrayList<String>();
        var sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (sb.length() > 0) {
                    args.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            args.add(sb.toString());
        }
        return args;
    }

    private String getHostname() {
        var host = context.environment().get("HOSTNAME");
        if (host != null && !host.isBlank()) {
            return host;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (Exception e) {
            return "localhost";
        }
    }
}
