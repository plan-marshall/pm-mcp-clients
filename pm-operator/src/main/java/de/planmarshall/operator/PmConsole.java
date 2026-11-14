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
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
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
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.dialogue.ChoicePrompt;
import de.planmarshall.operator.dialogue.OnboardingSource;
import de.planmarshall.operator.dialogue.StateDialogue;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

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

    private static final String PLANS = "plans";

    /** The verbs that draw their full-screen view on the terminal the console already owns. */
    private static final Set<String> FULL_SCREEN_VERBS = Set.of(PLANS, "plan", "projects", "system");

    /** The notice kind of a question, named once for the switch labels and the badge-text scan. */
    private static final String KIND_QUESTION = "question";

    /** The kinds that only report or withdraw something: nothing is put in front of the operator. */
    private static final Set<String> UNNOTIFIED_KINDS = Set.of("report", "answered_elsewhere", "paired",
                        "fresh_code");

    private final OperatorContext context;

    /** How long one refresh of the badges may take, unless the console is built with its own bound. */
    static final Duration REFRESH_BOUND = Duration.ofSeconds(5);
    private final Duration refreshBound;
    private final EntryFlow entryFlow;
    private final ReentrantLock badgeRefreshLock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicInteger controlDCounter = new AtomicInteger();
    /** The prompt's badges, replaced as a whole so a draw never sees a half-updated set. */
    private final AtomicReference<List<ConsoleBadge>> badges = new AtomicReference<>(List.of());
    private final ConsoleNoticeBuffer noticeBuffer;
    private final NeedsOperatorNotices needsNotices = new NeedsOperatorNotices();

    private volatile @Nullable String activePairingCode;
    private volatile @Nullable String activePairingUrl;
    private volatile int webPort = EntryFlow.DEFAULT_WEB_PORT;
    private volatile boolean notify = true;
    private final int machineSteps;

    /**
     * @param context the operator context
     */
    public PmConsole(OperatorContext context) {
        this(context, new EntryFlow(context));
    }

    /**
     * @param context   the operator context
     * @param entryFlow the entry flow the console runs and takes its machine step count from
     */
    public PmConsole(OperatorContext context, EntryFlow entryFlow) {
        this(context, entryFlow, REFRESH_BOUND);
    }

    /**
     * @param context      the operator context
     * @param entryFlow    the entry flow the console runs and takes its machine step count from
     * @param refreshBound how long one refresh of the badges may take before it keeps what it had
     */
    PmConsole(OperatorContext context, EntryFlow entryFlow, Duration refreshBound) {
        this.context = Objects.requireNonNull(context, "context");
        this.noticeBuffer = context.noticeBuffer();
        this.entryFlow = Objects.requireNonNull(entryFlow, "entryFlow");
        this.machineSteps = entryFlow.aspects().size();
        this.refreshBound = Objects.requireNonNull(refreshBound, "refreshBound");
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

        // The console commands and the verbs of the design's command tree (01-commands.adoc), and
        // nothing else: the completer is the registered registry, never a second copy of it.
        var completer = new StringsCompleter(OperatorVerbs.completer());

        var reader = LineReaderBuilder.builder()
                            .terminal(terminal)
                            .completer(completer)
                            .variable(LineReader.HISTORY_FILE, historyFile)
                            .build();

        var entryResult = entryFlow.run(out, reader);

        activePairingCode = entryResult.activeCode();
        activePairingUrl = entryResult.activeUrl();
        webPort = EntryFlow.getWebPort(entryResult.status());
        // What the start's status read showed was badged before: only items beyond it are announced.
        needsNotices.seedFromStatus(entryResult.status());
        List<ConsoleBadge> initialBadges = entryResult.consoleBadges();
        if (initialBadges.isEmpty() && !entryResult.initialBadges().isEmpty()) {
            initialBadges = entryResult.initialBadges().stream()
                                .map(t -> ConsoleBadge.waiting("unknown", 0, t))
                                .toList();
        }
        badges.set(List.copyOf(initialBadges));
        updateWindowTitle(terminal);
        printFullScreenHint(out, terminal);

        Thread sseThread = startEventListener(reader, terminal);

        try {
            runRepl(out, reader, terminal);
        } finally {
            context.setConsoleActive(false);
            stop();
            sseThread.interrupt();
            out.flush();
        }

        return 0;
    }

    /**
     * Ends the console's own loops, so the event stream stops reconnecting and the prompt loop
     * leaves at its next turn. Idempotent.
     */
    void stop() {
        running.set(false);
    }

    private void runRepl(PrintWriter out, LineReader reader, Terminal terminal) {
        var locale = locale();
        while (running.get()) {
            var line = readCommand(reader, out, locale);
            if (line == null) {
                break;
            }
            if (line.isEmpty()) {
                handleEmptyEnter(out, reader, terminal);
            } else if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                break;
            } else {
                dispatchCommand(line, out, reader, terminal);
            }
        }
    }

    /**
     * Reads one line at the prompt and answers the interrupt keys: {@code Ctrl+C} on an empty line
     * prints the hint, {@code Ctrl+D} names the questions that stay open once before it leaves.
     *
     * @return the trimmed line, or {@code null} when the console is to leave
     */
    @Nullable
    String readCommand(LineReader reader, PrintWriter out, String locale) {
        while (running.get()) {
            try {
                // JLine reads `%` in a prompt as its own escape introducer (`%{...%}` is zero-width
                // text): a checkout directory such as `%{repo%}` must reach the screen as typed, so
                // the badges are escaped here, where the prompt is built, and never in the badge
                // itself — the window title carries the unescaped text.
                var promptBadges = badges.get().stream()
                                    .map(ConsoleBadge::text)
                                    .map(badge -> badge.replace("%", "%%"))
                                    .toList();
                var line = reader.readLine(ConsolePrompt.format(locale, promptBadges, reconnecting.get()));
                return line == null ? null : line.trim();
            } catch (UserInterruptException e) {
                var partial = e.getPartialLine();
                if (partial == null || partial.isBlank()) {
                    out.println(MessageCatalogue.format(locale, "operator.console.ctrl_c_hint", Map.of()));
                    out.flush();
                } else {
                    // Ctrl+C clears the typed line, and Up brings it back (04-console.adoc#console-keys):
                    // JLine keeps no interrupted line in the history, so the console puts it there.
                    reader.getHistory().add(partial);
                }
            } catch (EndOfFileException _) {
                var handled = handleControlD(out);
                if (handled == null) {
                    return null;
                }
            }
        }
        return null;
    }

    @Nullable
    String handleControlD(PrintWriter out) {
        if (!hasOpenQuestions() || controlDCounter.getAndIncrement() > 0) {
            return null;
        }
        out.println(MessageCatalogue.format(locale(), "operator.console.exit_open",
                            Map.of("count", questionCount())));
        out.flush();
        return "";
    }

    boolean hasOpenQuestions() {
        for (var badge : badges.get()) {
            if (badge.isQuestion()) {
                return true;
            }
        }
        return false;
    }

    int questionCount() {
        int total = 0;
        for (var badge : badges.get()) {
            if (badge.isQuestion()) {
                total += badge.count();
            }
        }
        return total;
    }

    void handleEmptyEnter(PrintWriter out, LineReader reader, Terminal terminal) {
        var locale = locale();
        for (var b : badges.get()) {
            // Enter on the empty prompt takes what the badge offers first (04-console.adoc#prompt):
            // an unfinished onboarding continues where it stopped, the machine's first while it is
            // not complete, then an open project's.
            if (b.kind() == ConsoleBadge.Kind.MACHINE_SETUP) {
                continueOnboarding(out, reader, locale, terminal);
                return;
            }
            // A run without an address (its contract carries no continue link) offers no
            // continuation; EntryFlow.entryId never answers an empty address.
            var pid = b.projectId();
            if (b.kind() == ConsoleBadge.Kind.PROJECT_SETUP && pid != null) {
                continueProjectOnboarding(pid, out, reader, terminal);
                return;
            }
        }
    }

    private void continueProjectOnboarding(String address, PrintWriter out, LineReader reader, Terminal terminal) {
        context.useTerminal(terminal);
        try {
            // The address travels as an argument, never through a command line the console would
            // have to quote and parse back: an id with a quote or a backslash reaches the verb as it is.
            // Passing -- ensures that an entry id starting with -- is never read as an option or project selector.
            runOperatorVerb(List.of("project", "setup", "--entry", "--", address), out, reader, terminal);
        } finally {
            try {
                context.clearTerminal(terminal);
            } catch (IllegalStateException _) {
                // Scope already cleared
            }
        }
    }

    /**
     * Continues the open onboarding on the empty prompt: the one state dialogue reads the state the
     * runtime reports and follows it ({@code CLI-UC-S-06}).
     *
     * @param out    the console's writer
     * @param reader the console's line reader
     * @param locale the interface locale
     * @param terminal the terminal whose window title carries the badges
     */
    private void continueOnboarding(PrintWriter out, LineReader reader, String locale, Terminal terminal) {
        try {
            var runtime = context.runtime(true);
            var dialogue = new StateDialogue(new OnboardingSource(runtime, context.gate(), entryFlow.aspects()), out, locale,
                                stateId -> new ChoicePrompt(reader, out, locale, stateId));
            dialogue.run();
        } catch (StartPrompt.StartLeftException | StartPrompt.MissingInputException _) {
            // leaving the dialogue sends nothing; the runtime keeps the state it reports, and the
            // badge still follows whatever the dialogue left behind
        } catch (OperatorRefusedException e) {
            RefusalRenderer.render(out, locale, e);
            out.flush();
        } catch (IOException _) {
            out.println(MessageCatalogue.format(locale, "onboard.error.no_state", Map.of()));
            out.flush();
        } finally {
            // Every way out of the dialogue ends here, `q` included: the badge and the title never
            // state a step the operator has already left. (PR 275 called this low value; the lane
            // does it rather than answering it: one refresh closes the window either way.)
            refreshSetupBadge(locale, terminal);
        }
    }

    /**
     * Replaces the setup badge and the window title after the dialogue, so a completed onboarding
     * leaves no badge, a stopped one names the step it stopped at, and the title never carries a
     * badge the prompt has already dropped.
     *
     * @param locale   the interface locale
     * @param terminal the terminal whose window title carries the badges
     */
    void refreshSetupBadge(String locale, Terminal terminal) {
        long startNanos = System.nanoTime();
        try {
            if (!badgeRefreshLock.tryLock(refreshBound.toNanos(), TimeUnit.NANOSECONDS)) {
                return;
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            long remainingNanos = refreshBound.toNanos() - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                return;
            }
            var bound = Duration.ofNanos(remainingNanos);
            // Never starts the runtime: a refresh after `runtime stop` must not start it again.
            var runtime = context.runtime(false);
            var status = EntryFlow.bounded(() -> StatusCommand.fetch(runtime), bound, "status-read");

            remainingNanos = refreshBound.toNanos() - (System.nanoTime() - startNanos);
            List<ConsoleBadge> projectBadges = remainingNanos > 0
                                ? fetchProjectBadgesSafe(locale, runtime, Duration.ofNanos(remainingNanos))
                                : previousProjectBadges();
            badges.set(List.copyOf(EntryFlow.consoleBadges(locale, status, projectBadges, machineSteps)));
        } catch (IOException _) {
            // the badge keeps what the entry flow reported
            return;
        } finally {
            badgeRefreshLock.unlock();
        }
        updateWindowTitle(terminal);
    }

    private void dispatchCommand(String line, PrintWriter out, LineReader reader, Terminal terminal) {
        var locale = locale();
        var parts = line.split("\\s+");
        var verb = parts[0];

        if (verb.matches("\\d+")) {
            out.println(MessageCatalogue.format(locale, "operator.console.number_not_a_command",
                                Map.of("input", verb)));
            out.flush();
            return;
        }

        // `/` at the prompt opens the find field (shared/needs-and-find.adoc#terminal): the text
        // after it, or what the field takes, is the verb `find`'s text. `/` is free at the prompt
        // and is no verb of its own; inside the overview it stays the list filter.
        if (line.startsWith("/")) {
            openFind(line.substring(1).strip(), out, reader, terminal);
            return;
        }

        // `tui` is the alias of `plans`; `!` is the alias of `needs`. The rewrite keeps trailing arguments.
        String command = line;
        if ("tui".equals(verb)) {
            command = PLANS + line.substring(verb.length());
        } else if ("!".equals(verb)) {
            command = "needs" + line.substring(verb.length());
        }
        if (OperatorActs.isAct(line) && !checkAct(out)) {
            return;
        }
        switch (verb) {
            case "help" -> printHelp(out, parts);
            case "web" -> handleWebCommand(line, out, parts, reader, terminal);
            default -> runOperatorVerb(command, out, reader, terminal);
        }
        out.flush();
    }

    /**
     * Runs the console's {@code /} field: the text already typed after the slash, or the field the
     * prompt opens, plus the verb {@code find} (shared/needs-and-find.adoc#terminal).
     *
     * <p>{@code Esc} at the field returns to the prompt with nothing sent, and an empty text is
     * refused with the catalogue's own text, never with a usage line: the operator typed a name,
     * not a syntax.
     *
     * @param text     the text typed after the slash, empty when the field is to open
     * @param out      the console's writer
     * @param reader   the console's line reader, which draws the field
     * @param terminal the terminal the console owns
     */
    private void openFind(String text, PrintWriter out, LineReader reader, Terminal terminal) {
        var locale = locale();
        var query = text;
        if (query.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "overview.find.title", Map.of()));
            out.flush();
            query = FindCommand.readField(reader, MessageCatalogue.format(locale, "overview.find.hint", Map.of())
                                + StartPrompt.SUFFIX);
            if (query == null) {
                // Esc leaves the field: nothing is sent, and the prompt is drawn again as it was.
                out.println();
                out.flush();
                return;
            }
        }
        if (query.isBlank()) {
            out.println(MessageCatalogue.format(locale, "overview.find.no_text", Map.of()));
            out.flush();
            return;
        }
        // `--` ends the options: the typed text is a name, so one that starts with `-` is never read
        // as an option or as the project selector.
        runOperatorVerb(List.of("find", "--", query), out, reader, terminal);
    }

    /**
     * Checks the operator gate for one act the console is about to dispatch (PM-SEC-3). The console
     * is entered without the gate, so the act is where the refusal belongs: the operator reads why
     * the act is refused and the console stays open.
     *
     * @param out the console's writer
     * @return {@code true} when the act may run
     */
    private boolean checkAct(PrintWriter out) {
        try {
            context.gate().verify();
            return true;
        } catch (OperatorSecurityException e) {
            out.println(e.getMessage());
            out.flush();
            return false;
        }
    }

    /**
     * Runs one operator verb of the console, with the project selector in front of it.
     *
     * <p>A full-screen verb draws on the console's own terminal: the terminal is scoped on the
     * context, so the nested command never opens a second JLine terminal (which would fall back to
     * a dumb 0&times;0 terminal and refuse the view). While the view is open the notice buffer holds
     * what the runtime pushes; leaving it prints one coalesced meanwhile line. {@code --json} and a
     * terminal that cannot open the view run without the notice hold.
     *
     * @param line     the command line to run
     * @param out      the console's writer
     * @param reader   the line reader
     * @param terminal the terminal the console owns
     */
    private void runOperatorVerb(String line, PrintWriter out, LineReader reader, Terminal terminal) {
        runOperatorVerb(parseArgs(line), out, reader, terminal);
    }

    private void runOperatorVerb(List<String> args, PrintWriter out, LineReader reader, Terminal terminal) {
        var selection = ProjectSelector.rewrite(context, out, out, args.toArray(String[]::new), reader);
        if (!selection.proceed()) {
            return;
        }
        var effective = selection.args().toArray(String[]::new);
        if (!isFullScreenVerb(effective)) {
            context.useTerminal(terminal);
            try {
                PmOperator.execute(context, out, out, effective);
            } finally {
                try {
                    context.clearTerminal(terminal);
                } catch (IllegalStateException _) {
                    // Scope already cleared
                }
            }
            // A verb can change what waits (a project setup advances, an onboarding completes):
            // the badge and the title follow what the runtime says after it, not before it.
            refreshSetupBadge(locale(), terminal);
            return;
        }
        var json = List.of(effective).contains("--json");
        var hold = !json
                            && TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS).available();
        context.useTerminal(terminal);
        if (hold) {
            enterFullScreen();
        }
        try {
            PmOperator.execute(context, out, out, effective);
        } finally {
            if (hold) {
                leaveFullScreen(out);
            }
            try {
                context.clearTerminal(terminal);
            } catch (IllegalStateException _) {
                // Scope already cleared
            }
            // A view's acts can change what waits too: the prompt and the title are rebuilt from
            // the state the runtime reports once the view is gone.
            refreshSetupBadge(locale(), terminal);
            out.flush();
        }
    }

    private static boolean isFullScreenVerb(String[] args) {
        return args.length > 0 && FULL_SCREEN_VERBS.contains(args[0]);
    }

    /**
     * Runs {@code web}: {@code qr}, {@code copy ssh} and {@code pair} are the console's own, every
     * other form is the verb, run as every verb of the console is — from the typed line, on the
     * console's terminal, with the badges read again afterwards.
     */
    private void handleWebCommand(String line, PrintWriter out, String[] parts, LineReader reader,
                        Terminal terminal) {
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
        runOperatorVerb(line, out, reader, terminal);
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
        out.println(OperatorVerbs.helpMore(locale));
    }

    /**
     * Starts the event stream the console follows, on a daemon thread.
     *
     * @param reader   the console's line reader the events act on
     * @param terminal the terminal whose window title carries the badges
     * @return the thread, so the caller can stop and join it
     */
    Thread startEventListener(LineReader reader, Terminal terminal) {
        var thread = new Thread(() -> {
            while (running.get()) {
                if (!followEventStream(reader, terminal)) {
                    return;
                }
            }
        }, "pm-console-sse");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Follows the event stream until it ends or the console leaves.
     *
     * @param reader          the console's line reader the events act on
     * @param terminal        the terminal whose window title carries the badges
     * @return {@code true} when the console may follow the stream again, {@code false} when it
     *         leaves
     */
    private boolean followEventStream(LineReader reader, Terminal terminal) {
        try (var exchange = context.runtime(false).open(HttpRequest.get("/api/v1/events"))) {
            // A stream may have missed every event held before it opened — including answers given
            // while it was unreachable, and the first connection's own: read the state now.
            refreshBadges(terminal);
            reconnecting.set(false);
            var sseReader = exchange.events();
            SseEvent event;
            while (running.get() && (event = sseReader.next()) != null) {
                handleSseEvent(event, reader);
            }
            return reconnectDelay();
        } catch (IOException _) {
            return reconnectDelay();
        }
    }

    /**
     * Waits out the back-off before the next connection.
     *
     * @return {@code true} while the console still runs, {@code false} when it leaves
     */
    private boolean reconnectDelay() {
        if (!running.get()) {
            return false;
        }
        reconnecting.set(true);
        try {
            Thread.sleep(1000);
            return true;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Applies one event of the stream: data that is no JSON object (empty, malformed, or another
     * value) is dropped, and the stream goes on.
     *
     * @param event  the event as the stream delivered it
     * @param reader the line reader the notices are printed above
     */
    void handleSseEvent(SseEvent event, LineReader reader) {
        Map<String, Object> eventMap;
        try {
            eventMap = JsonTree.asObject(JsonTree.parse(event.data()));
        } catch (IOException _) {
            // empty or invalid json in the event stream: drop this event, keep the stream
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
        var notices = noticesOf(locale, eventName, eventMap);
        for (var notice : notices) {
            var category = categoryOf(notice.kind());
            if (!noticeBuffer.hold(category, countOf(eventMap, category))) {
                reader.printAbove(notice.text());
            }
            notifyOperator(reader.getTerminal(), notice.kind(), eventMap, notice.place());
        }
        if (!notices.isEmpty() || changesWaitingItems(eventName)
                            || NeedsOperatorNotices.isNeedsOperatorChanged(eventName, eventMap)) {
            refreshBadges(reader.getTerminal());
        }
        var name = eventName != null ? eventName : "";
        var payload = ConsoleNotice.payloadOf(eventMap);
        var type = notices.isEmpty() ? "" : notices.getFirst().kind();
        if ("device.paired".equals(name)) {
            // A legacy paired notice above already named the device; the line is printed once.
            if (!"paired".equals(type)) {
                var label = JsonTree.string(payload, "label");
                printAboveOrHold(reader, ConsoleNotice.formatPairedNotice(locale, label != null && !label.isBlank()
                                    ? label
                                    : MessageCatalogue.format(locale, "common.unknown", Map.of())));
            }
            issueFreshCodeAbove(reader);
        } else if ("fresh_code".equals(type)) {
            activePairingCode = JsonTree.string(payload, "code");
            activePairingUrl = JsonTree.string(payload, "url");
        }
    }

    /**
     * The notices one event raises: the single notice of an event that announces one thing itself,
     * or one notice per item a {@code needs_operator} change adds of a kind no other event announces.
     *
     * @param locale    the interface locale
     * @param eventName the SSE event name, or {@code null}
     * @param eventMap  the parsed event payload
     * @return the notices, empty when the event raises none
     */
    private List<NeedsOperatorNotices.Notice> noticesOf(String locale, @Nullable String eventName,
                        Map<String, Object> eventMap) {
        var notice = ConsoleNotice.fromEventMap(locale, eventName, eventMap);
        if (notice != null) {
            var kind = ConsoleNotice.noticeKindOf(eventName, eventMap);
            return List.of(new NeedsOperatorNotices.Notice(kind != null ? kind.notice() : "",
                                notice, ConsoleNotice.scopeOf(eventMap)));
        }
        return needsNotices.noticesOf(locale, eventName, eventMap);
    }

    /**
     * Rebuilds the prompt badges from the runtime's {@code needs_operator} summary after an event,
     * so a question answered in the web app leaves the badge at once (04-console.adoc#prompt). The
     * counts stay the runtime's; the client never counts them itself.
     *
     * <p>JLine offers no way to replace the prompt of a line that is already being read, so the
     * prompt itself shows the refreshed badges at its next draw; the window title, which carries
     * the same badges, is updated here and now.
     *
     * @param terminal the terminal whose window title carries the badges
     */
    void refreshBadges(Terminal terminal) {
        long startNanos = System.nanoTime();
        try {
            if (!badgeRefreshLock.tryLock(refreshBound.toNanos(), TimeUnit.NANOSECONDS)) {
                return;
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return;
        }
        List<ConsoleBadge> refreshed;
        try {
            long remainingNanos = refreshBound.toNanos() - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                return;
            }
            var bound = Duration.ofNanos(remainingNanos);
            var runtime = context.runtime(false);
            var status = EntryFlow.bounded(() -> StatusCommand.fetch(runtime), bound, "status-read");

            remainingNanos = refreshBound.toNanos() - (System.nanoTime() - startNanos);
            List<ConsoleBadge> projectBadges = remainingNanos > 0
                                ? fetchProjectBadgesSafe(locale(), runtime, Duration.ofNanos(remainingNanos))
                                : previousProjectBadges();
            refreshed = EntryFlow.consoleBadges(locale(), status, projectBadges, machineSteps);
            if (!refreshed.equals(badges.get())) {
                badges.set(List.copyOf(refreshed));
            }
        } catch (IOException _) {
            // the runtime did not answer this read: keep the badges it last gave us
            return;
        } finally {
            badgeRefreshLock.unlock();
        }
        updateWindowTitle(terminal);
    }

    private List<ConsoleBadge> fetchProjectBadgesSafe(String locale, RuntimeClient runtime, Duration bound) {
        try {
            return EntryFlow.readProjectSetupConsoleBadges(locale, runtime, bound);
        } catch (IOException _) {
            return previousProjectBadges();
        }
    }

    private List<ConsoleBadge> previousProjectBadges() {
        return badges.get().stream()
                            .filter(b -> b.kind() == ConsoleBadge.Kind.PROJECT_SETUP)
                            .toList();
    }

    /**
     * @param kind the notice kind
     * @return the catalogue key of the label a desktop notification names the kind by
     */
    private static String notificationLabelKey(String kind) {
        return KIND_QUESTION.equals(kind) ? "overview.needs.decision" : "overview.needs." + kind;
    }

    /**
     * @param eventName the SSE event name, or {@code null}
     * @return {@code true} when the event can change what waits for the operator
     */
    private static boolean changesWaitingItems(@Nullable String eventName) {
        return eventName != null && (eventName.startsWith("decision.") || eventName.startsWith("hold.")
                            || eventName.startsWith("condition.") || eventName.startsWith("needs_operator."));
    }

    private static int countOf(Map<String, Object> eventMap, ConsoleNoticeBuffer.Category category) {
        var payload = ConsoleNotice.payloadOf(eventMap);
        var count = ConsoleNotice.wholeCount(payload.get("count"));
        if (category == ConsoleNoticeBuffer.Category.REPORT && count > 1) {
            return Math.min(count, 10_000);
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

    private static ConsoleNoticeBuffer.Category categoryOf(@Nullable String kind) {
        return switch (kind != null ? kind : "") {
            case KIND_QUESTION, "decision", "waiver", "answered_elsewhere" -> ConsoleNoticeBuffer.Category.DECISION;
            case "report" -> ConsoleNoticeBuffer.Category.REPORT;
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

    /**
     * Sends the desktop notification of one notice, where the terminal supports one, and the bell
     * everywhere else ({@code 04-console.adoc#notifications}): the scope and the kind, never the
     * text of a question, and never the item's own content.
     *
     * @param terminal the terminal to notify on
     * @param kind     the notice kind, or {@code null}
     * @param eventMap the event frame
     * @param scope    the scope to name, or {@code null} to read it from the event frame
     */
    private void notifyOperator(Terminal terminal, @Nullable String kind, Map<String, Object> eventMap,
                        @Nullable String scope) {
        if (!notify || kind == null || UNNOTIFIED_KINDS.contains(kind)) {
            return;
        }
        var named = namedScope(scope, eventMap);
        if (named.isBlank()) {
            return;
        }
        var label = MessageCatalogue.format(locale(), notificationLabelKey(kind), Map.of());
        var payload = MessageCatalogue.format(locale(), "operator.console.notification.line",
                            Map.of("scope", TerminalSanitizer.sanitize(named),
                                                "kind", TerminalSanitizer.sanitize(label)));
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
        if (!badges.get().isEmpty()) {
            title.append(" [")
                                .append(String.join(" \u00b7 ", badges.get().stream().map(ConsoleBadge::text).toList()))
                                .append(']');
        }
        TerminalTitle.set(terminal, title.toString());
    }

    private static String namedScope(@Nullable String scope, Map<String, Object> eventMap) {
        if (scope != null) {
            return scope;
        }
        return ConsoleNotice.scopeOf(eventMap);
    }

    /**
     * Tells the operator once, in the greeting, that the full-screen overview is one word away.
     * The hint is left out where the view cannot open, so it never names a verb that falls back.
     *
     * @param out      the console's writer
     * @param terminal the terminal the console runs on
     */
    private void printFullScreenHint(PrintWriter out, Terminal terminal) {
        if (TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS).available()) {
            out.println(MessageCatalogue.format(locale(), "operator.console.hint.tui", Map.of()));
            out.flush();
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

    /**
     * Splits a typed console line into arguments. Quoted text stays one argument, and a
     * backslash escapes the next character inside a quote only: text typed outside quotes, such as
     * a path or a label, keeps its backslashes. As in a shell, a quoted empty text is an argument
     * of its own ({@code config set key ""} gives the verb an empty value), while whitespace alone
     * separates and gives none.
     *
     * @param line the typed line
     * @return the arguments
     */
    static List<String> parseArgs(@Nullable String line) {
        if (line == null) {
            return List.of();
        }
        var args = new ArrayList<String>();
        var sb = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                appendEscaped(sb, c);
                escaped = false;
            } else if (c == '\\' && inQuotes) {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
                quoted = true;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                flushToken(args, sb, quoted);
                quoted = false;
            } else {
                sb.append(c);
            }
        }
        if (escaped) {
            sb.append('\\');
        }
        flushToken(args, sb, quoted);
        return args;
    }

    /**
     * @param sb the argument being built
     * @param c  the character that followed a backslash inside a quote
     */
    private static void appendEscaped(StringBuilder sb, char c) {
        // Only a quote and a backslash are escapes; any other backslash is a literal one, so a
        // quoted path keeps its separators.
        if (c != '\\' && c != '"') {
            sb.append('\\');
        }
        sb.append(c);
    }

    /**
     * @param args   the arguments read so far
     * @param sb     the argument being built
     * @param quoted whether the argument had a quote, which makes even an empty one an argument
     */
    private static void flushToken(List<String> args, StringBuilder sb, boolean quoted) {
        if (quoted || !sb.isEmpty()) {
            args.add(sb.toString());
            sb.setLength(0);
        }
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

    List<String> currentBadges() {
        return badges.get().stream().map(ConsoleBadge::text).toList();
    }
}
