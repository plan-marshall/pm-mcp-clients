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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.SseEvent;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

/**
 * The operator's interactive console REPL (04-console.adoc#console).
 * Manages badged prompts, pushed runtime events via {@code LineReader.printAbove(...)},
 * history persistence, and built-in navigation/control verbs.
 *
 * <p>The prompt, the badges, and every notice come from the message catalogue, and every text the
 * runtime supplies passes through {@link TerminalSanitizer} before it is rendered or placed in the
 * window title. The window title is set with {@link TerminalTitle}, which neutralizes the text it
 * is given; a text that reaches the console therefore cannot move the cursor, write the clipboard,
 * or fake a prompt.
 */
public final class PmConsole {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final OperatorContext context;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicInteger controlDCounter = new AtomicInteger();
    private final List<String> badges = new CopyOnWriteArrayList<>();
    private final ConsoleNoticeBuffer noticeBuffer;

    private volatile @Nullable String activePairingCode;
    private volatile @Nullable String activePairingUrl;
    private volatile int webPort = EntryFlow.DEFAULT_WEB_PORT;
    private volatile boolean notify = true;

    /**
     * @param context the operator context
     */
    public PmConsole(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.noticeBuffer = context.noticeBuffer();
    }

    private String locale() {
        return context.clientEnvironment().locale();
    }

    /**
     * Runs the console entry flow and main REPL loop.
     *
     * @return exit code (0 on normal exit)
     * @throws IOException on fatal console or terminal error
     */
    public int run() throws IOException {
        context.setConsoleActive(true);
        var terminal = context.createTerminal();
        var out = new PrintWriter(terminal.writer(), true);

        var historyFile = context.stateDirectory().historyFile();
        try {
            context.stateDirectory().ensureFile(historyFile);
        } catch (IOException _) {
            // best-effort history file
        }

        var completer = new StringsCompleter(
                            "exit", "quit", "help", "status", "plans", "plan", "epic", "questions", "answer",
                            "web", "runtime", "config", "credentials", "projects", "project", "client", "drain"
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
        updateWindowTitle(terminal);

        Thread sseThread = startEventListener(reader);

        try {
            runRepl(out, reader);
        } finally {
            context.setConsoleActive(false);
            running.set(false);
            sseThread.interrupt();
            out.flush();
        }

        return 0;
    }

    private void runRepl(PrintWriter out, LineReader reader) {
        var locale = locale();
        while (running.get()) {
            var line = readCommand(reader, out, locale);
            if (line == null) {
                break;
            }
            if (line.isEmpty()) {
                handleEmptyEnter(out, reader);
            } else if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                break;
            } else {
                dispatchCommand(line, out, reader);
            }
        }
    }

    /**
     * Reads one line at the prompt and answers the interrupt keys: {@code Ctrl+C} on an empty line
     * prints the hint, {@code Ctrl+D} names the questions that stay open once before it leaves.
     *
     * @return the trimmed line, or {@code null} when the console is to leave
     */
    private @Nullable String readCommand(LineReader reader, PrintWriter out, String locale) {
        while (running.get()) {
            try {
                var line = reader.readLine(ConsolePrompt.format(locale, badges, reconnecting.get()));
                return line == null ? null : line.trim();
            } catch (UserInterruptException e) {
                if (e.getPartialLine() == null || e.getPartialLine().isEmpty()) {
                    out.println(MessageCatalogue.format(locale, "operator.console.ctrl_c_hint", Map.of()));
                    out.flush();
                }
            } catch (EndOfFileException e) {
                if (!hasOpenQuestions() || controlDCounter.getAndIncrement() > 0) {
                    return null;
                }
                out.println(MessageCatalogue.format(locale, "operator.console.exit_open",
                                    Map.of("count", questionCount())));
                out.flush();
            }
        }
        return null;
    }

    private boolean hasOpenQuestions() {
        for (var badge : badges) {
            var lower = badge.toLowerCase(Locale.ROOT);
            if (lower.contains("question") || lower.contains("frage") || lower.contains("waiver")) {
                return true;
            }
        }
        return false;
    }

    private int questionCount() {
        int total = 0;
        for (var badge : badges) {
            var lower = badge.toLowerCase(Locale.ROOT);
            if (lower.contains("question") || lower.contains("frage") || lower.contains("waiver")) {
                total += leadingCount(badge);
            }
        }
        return total;
    }

    private static int leadingCount(String badge) {
        int start = -1;
        for (int i = 0; i < badge.length(); i++) {
            if (Character.isDigit(badge.charAt(i))) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return 1;
        }
        int end = start;
        while (end < badge.length() && Character.isDigit(badge.charAt(end))) {
            end++;
        }
        return Integer.parseInt(badge.substring(start, end));
    }

    private void handleEmptyEnter(PrintWriter out, LineReader reader) {
        var locale = locale();
        boolean hasSetup = false;
        for (var b : badges) {
            if (b.startsWith("setup") || b.startsWith("Einrichtung")) {
                hasSetup = true;
                break;
            }
        }
        if (hasSetup) {
            out.println(MessageCatalogue.format(locale, "operator.entry.setup_title", Map.of()));
            out.flush();
            String beginChoice = null;
            try {
                beginChoice = reader.readLine(MessageCatalogue.format(locale,
                                    "operator.console.setup.begin_prompt", Map.of()));
            } catch (EndOfFileException | UserInterruptException _) {
                // abort continuation on interrupt/EOF
            }
            if (beginChoice != null && "q".equalsIgnoreCase(beginChoice.trim())) {
                out.println(MessageCatalogue.format(locale, "operator.console.setup.left", Map.of()));
                out.flush();
            }
        }
    }

    private void dispatchCommand(String line, PrintWriter out, LineReader reader) {
        var locale = locale();
        var parts = line.split("\\s+");
        var verb = parts[0];

        if (verb.matches("\\d+")) {
            out.println(MessageCatalogue.format(locale, "operator.console.number_not_a_command",
                                Map.of("input", verb)));
            out.flush();
            return;
        }

        switch (verb) {
            case "help" -> printHelp(out, parts);
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
        var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);
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
        out.println(MessageCatalogue.format(locale(), "operator.console.copy_done", Map.of()));
    }

    private void issueNewPairingCode(PrintWriter out) {
        var locale = locale();
        try {
            var pairing = new EntryFlow(context).issuePairingCode(context.runtime(true), webPort);
            activePairingCode = pairing.code();
            activePairingUrl = pairing.url();
            var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);
            out.println(MessageCatalogue.format(locale, "operator.console.new_code",
                                Map.of("code", pairing.code(), "time", validUntil)));
            out.println(MessageCatalogue.format(locale, "operator.console.url_indent",
                                Map.of("url", pairing.url())));
        } catch (IOException e) {
            out.println(MessageCatalogue.format(locale, "operator.console.code_failed",
                                Map.of("reason", TerminalSanitizer.sanitize(e.getMessage()))));
        }
    }

    private void printHelp(PrintWriter out, String[] parts) {
        var locale = locale();
        if (parts.length >= 2) {
            out.println(MessageCatalogue.format(locale, "operator.console.help.for", Map.of("verb", parts[1])));
            return;
        }
        out.println(MessageCatalogue.format(locale, "operator.console.help.title", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.console.help.body", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.console.help.more", Map.of()));
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
                } catch (IOException _) {
                    if (!running.get()) {
                        break;
                    }
                    reconnecting.set(true);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException _) {
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
        if (event.data() == null || event.data().isBlank()) {
            return;
        }
        Map<String, Object> eventMap;
        try {
            eventMap = JsonTree.asObject(JsonTree.parse(event.data()));
        } catch (IOException _) {
            // invalid json in the event stream: drop this event, keep the stream
            return;
        }
        if (eventMap != null) {
            applyConsoleEvent(event.event(), eventMap, reader);
        }
    }

    /**
     * Applies one parsed console event: the arrival notices, and the pairing events the entry
     * follows ({@code device.paired} with a fresh code, the legacy {@code paired}/{@code fresh_code}
     * notices).
     *
     * @param eventName the SSE event name, or {@code null}
     * @param eventMap  the parsed event payload
     * @param reader    the line reader the notices are printed above
     */
    void applyConsoleEvent(@Nullable String eventName, Map<String, Object> eventMap, LineReader reader) {
        var locale = locale();
        var notice = ConsoleNotice.fromEventMap(locale, eventMap);
        if (notice != null) {
            var category = categoryOf(eventMap);
            if (!noticeBuffer.hold(category, countOf(eventMap, category))) {
                reader.printAbove(notice);
            }
            notifyOperator(reader.getTerminal(), eventMap);
        }
        var name = eventName != null ? eventName : "";
        var type = String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));
        if ("device.paired".equals(name) || "paired".equals(type)) {
            var label = JsonTree.string(eventMap, "label");
            printAboveOrHold(reader, MessageCatalogue.format(locale, "operator.console.paired",
                                Map.of("label", TerminalSanitizer.sanitize(label != null ? label : "local"))));
            if ("device.paired".equals(name)) {
                issueFreshCodeAbove(reader);
            }
        } else if ("fresh_code".equals(type)) {
            activePairingCode = JsonTree.string(eventMap, "code");
            activePairingUrl = JsonTree.string(eventMap, "url");
        }
    }

    private static int countOf(Map<String, Object> eventMap, ConsoleNoticeBuffer.Category category) {
        if (category == ConsoleNoticeBuffer.Category.REPORT
                            && eventMap.get("count") instanceof Number number && number.intValue() > 1) {
            return Math.min(number.intValue(), 10_000);
        }
        return 1;
    }

    /**
     * Prints a line above the prompt, or keeps it for the end of the full-screen view.
     *
     * @param reader the line reader
     * @param line   the line to print
     */
    private void printAboveOrHold(LineReader reader, String line) {
        if (!noticeBuffer.holdLine(line)) {
            reader.printAbove(line);
        }
    }

    private static ConsoleNoticeBuffer.Category categoryOf(Map<String, Object> eventMap) {
        var type = String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));
        return switch (type) {
            case "question", "decision", "waiver_request" -> ConsoleNoticeBuffer.Category.DECISION;
            case "report", "report.sent" -> ConsoleNoticeBuffer.Category.REPORT;
            default -> ConsoleNoticeBuffer.Category.NEED;
        };
    }

    /**
     * @return the buffer that holds notices while a full-screen view is open
     */
    public ConsoleNoticeBuffer noticeBuffer() {
        return noticeBuffer;
    }

    /** Marks the start of a full-screen view: notices are held instead of printed above it. */
    public void enterFullScreen() {
        noticeBuffer.activate();
    }

    /**
     * Marks the end of a full-screen view and prints one line of what arrived meanwhile.
     *
     * @param out the console's writer
     */
    public void leaveFullScreen(PrintWriter out) {
        noticeBuffer.deactivate();
        var drained = noticeBuffer.drain(locale());
        if (!drained.summary().isEmpty()) {
            out.println(drained.summary());
        }
        for (var line : drained.lines()) {
            out.println(line);
        }
        out.flush();
    }

    private void notifyOperator(Terminal terminal, Map<String, Object> eventMap) {
        if (!notify) {
            return;
        }
        var kind = String.valueOf(eventMap.getOrDefault("type", eventMap.getOrDefault("event", "")));
        var scope = String.valueOf(eventMap.getOrDefault("scope", ""));
        var payload = MessageCatalogue.format(locale(), "operator.console.notification.line",
                            Map.of("scope", TerminalSanitizer.sanitize(scope),
                                                "kind", TerminalSanitizer.sanitize(kind)));
        terminal.writer().write("\u001B]9;" + TerminalSanitizer.sanitizeNotification(payload) + "\u0007");
        terminal.writer().flush();
    }


    /**
     * Asks for a fresh pairing code after a redemption and reports it above the prompt, so the
     * first device to pair never uses the code up for the other ways in
     * (03-web-server.adoc#fresh-code).
     *
     * @param reader the line reader the notice is printed above
     */
    private void issueFreshCodeAbove(LineReader reader) {
        var locale = locale();
        try {
            var pairing = new EntryFlow(context).issuePairingCode(context.runtime(true), webPort);
            activePairingCode = pairing.code();
            activePairingUrl = pairing.url();
            var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);
            printAboveOrHold(reader, MessageCatalogue.format(locale, "operator.console.new_code_above",
                                Map.of("code", pairing.code(), "time", validUntil)));
            printAboveOrHold(reader, MessageCatalogue.format(locale, "operator.console.url_indent",
                                Map.of("url", pairing.url())));
        } catch (IOException e) {
            printAboveOrHold(reader, MessageCatalogue.format(locale, "operator.console.code_failed_fresh",
                                Map.of("reason", TerminalSanitizer.sanitize(e.getMessage()))));
        }
    }

    private void updateWindowTitle(Terminal terminal) {
        var title = new StringBuilder("pm-operator");
        if (!badges.isEmpty()) {
            title.append(" [").append(String.join(" \u00b7 ", badges)).append(']');
        }
        TerminalTitle.set(terminal, title.toString());
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

    static List<String> parseArgs(@Nullable String line) {
        if (line == null) {
            return List.of();
        }
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
        catch (UnknownHostException _) {
            return "localhost";
        }
    }
}
