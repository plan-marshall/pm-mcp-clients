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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.display.Display;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.FindMatch;
import de.planmarshall.api.overview.FindPage;
import de.planmarshall.api.overview.WorkLocation;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator find <text>}: goes to work by name (UC-PLN-8, CLI-UX-S-04).
 *
 * <p>The runtime answers {@code GET /api/v1/find?q=} with the matches of running and ended work,
 * grouped by kind in the order it serves them. The client searches nothing itself: it sends the
 * text it was given and shows what comes back — each match with its title, where it belongs
 * ({@code project › epic › plan}), and the command that opens it, ended work marked.
 *
 * <p>The verb is a read: it opens no act, so it runs without the operator checks. In a terminal the
 * matches are a choice, and selecting one opens it where it belongs — the plan view, the epic view,
 * the decision, the project, or the report; {@code --print} prints and exits.
 */
@Command(name = "find", description = "Goes to work by name: part of an id or a title, or a pull request number.")
@OperatorVerb
public final class FindCommand implements Callable<Integer> {

    /** The verb that opens a plan match. */
    static final String OPEN_PLAN = "plan";

    /** The verb that opens an epic match. */
    static final String OPEN_EPIC = "epic";

    /** The verb that opens a decision match. */
    static final String OPEN_DECISION = "answer";

    /** The verb whose sub-verb opens a report match. */
    static final String OPEN_REPORT = "reports";

    /** The sub-verb of {@link #OPEN_REPORT} that opens one report. */
    static final String OPEN_REPORT_VERB = "open";

    /** The verb that opens a project match; no verb opens one project, so the explorer filters. */
    static final String OPEN_PROJECT = "projects";

    /** The option of {@link #OPEN_PROJECT} that opens the one project in the explorer. */
    static final String OPEN_PROJECT_FILTER = "--filter";

    /** The word that leaves the choice without opening a match. */
    static final String LEAVE = "q";

    /** The key that leaves the console's find field with nothing sent. */
    private static final int ESCAPE = 27;

    /** {@code Ctrl+C} leaves the console's find field as {@code Esc} does. */
    private static final int INTERRUPT = 3;

    private static final int CARRIAGE_RETURN = '\r';

    private static final int LINE_FEED = '\n';

    private static final int BACKSPACE = 8;

    private static final int DELETE = 127;

    /** How long a character after an {@code ESC} is still taken as part of its sequence. */
    private static final long PEEK_MILLIS = 20;

    private static final String SEPARATOR = " \u00b7 ";

    private static final String PATH_SEPARATOR = " \u203a ";

    private static final String TITLE_LINE = "  %-3s %-9s %s%n";

    private static final String DETAIL_LINE = "  %-3s %-9s %s%n";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(arity = "0..*",
                        paramLabel = "<text>", description = "Part of an id or a title, or a pull request number.")
    List<String> text = new ArrayList<>();

    @Option(names = "--print", description = "Prints the matches into the scrollback instead of choosing one.")
    boolean print;

    FindCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();
        var query = String.join(" ", text).strip();
        if (query.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "overview.find.no_text", Map.of()));
            out.flush();
            return 0;
        }
        return executeFind(context.runtime(true)::send, out, locale, query);
    }

    /**
     * Reads the matches of the text and shows them, then offers the choice a terminal takes.
     *
     * @param sender the runtime the request goes to
     * @param out    the writer the matches are printed to
     * @param locale the interface locale
     * @param query  the text to look for, never empty
     * @return the exit code
     * @throws IOException when the runtime or the terminal fails
     */
    Integer executeFind(RuntimeSender sender, PrintWriter out, String locale, String query) throws IOException {
        var response = sender.send(HttpRequest.get(findPath(query)));
        if (response.status() != 200) {
            throw new OperatorRefusedException("find " + query, response);
        }
        var page = FindPage.fromJson(response.bodyText());
        var chooses = chooses();
        printMatches(out, page, locale, chooses);
        out.flush();
        if (!chooses || page.matches().isEmpty()) {
            return 0;
        }
        return choose(page, locale, out);
    }

    /**
     * @param query the text to look for
     * @return the request path of the search, with the text as its query value
     */
    static String findPath(String query) {
        return MachineApiPaths.FIND + "?" + MachineApiPaths.QUERY_PARAM + "="
                            + OperatorUrlUtil.encodeQueryValue(query);
    }

    /**
     * @return {@code true} when the operator picks a match here: a terminal, and no {@code --print}
     */
    private boolean chooses() {
        return !print && context.isInteractiveTerminal();
    }

    private void printMatches(PrintWriter out, FindPage page, String locale, boolean numbered) {
        if (page.matches().isEmpty()) {
            out.println(MessageCatalogue.format(locale, "overview.find.empty", Map.of("text", page.query())));
            return;
        }
        out.println(MessageCatalogue.format(locale, "overview.find.title", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.find.header",
                            Map.of("text", page.query(), "count", page.matches().size())));
        var index = 0;
        for (var match : page.matches()) {
            index++;
            printMatch(out, match, locale, numbered ? String.valueOf(index) : "");
        }
    }

    private void printMatch(PrintWriter out, FindMatch match, String locale, String number) {
        out.printf(TITLE_LINE, number, match.kind().value(), TerminalSanitizer.sanitizeLine(match.title()));
        out.printf(DETAIL_LINE, "", "", details(match, locale));
    }

    /**
     * Renders what stands beside a match's title: where it belongs, whether it ended, and the
     * command that opens it.
     *
     * @param match  the match
     * @param locale the interface locale
     * @return the rendered details
     */
    static String details(FindMatch match, String locale) {
        var details = new StringBuilder(locationOf(match, locale));
        appendWithSeparator(details, endedOf(match, locale));
        appendWithSeparator(details, MessageCatalogue.format(locale, "operator.find.open",
                            Map.of("command", TerminalSanitizer.sanitizeLine(openCommand(match)))));
        return details.toString();
    }

    private static void appendWithSeparator(StringBuilder details, String part) {
        if (part.isBlank()) {
            return;
        }
        if (!details.isEmpty()) {
            details.append(SEPARATOR);
        }
        details.append(part);
    }

    /**
     * Renders where a match belongs, as the path of the titles it sits under: a plan under its
     * project and epic, an epic under its project, a decision or a report under all three, a
     * project under nothing.
     *
     * @param match  the match
     * @param locale the interface locale
     * @return the rendered path, empty for a project
     */
    static String locationOf(FindMatch match, String locale) {
        var parts = switch (match.kind()) {
            case PLAN -> new WorkLocation[]{match.project(), match.epic()};
            case EPIC -> new WorkLocation[]{match.project()};
            case PROJECT -> new WorkLocation[0];
            case DECISION, REPORT -> new WorkLocation[]{match.project(), match.epic(), match.plan()};
        };
        var rendered = new ArrayList<String>();
        for (var part : parts) {
            if (part == null) {
                continue;
            }
            var title = titleOf(part, locale);
            if (!title.isBlank()) {
                rendered.add(title);
            }
        }
        return String.join(PATH_SEPARATOR, rendered);
    }

    private static String titleOf(WorkLocation location, String locale) {
        var title = location.title();
        if (title != null && !title.isBlank()) {
            return TerminalSanitizer.sanitizeLine(title);
        }
        // A location without a title renders through Display where the catalogue knows the unit, and
        // as nothing otherwise: the id alone never reaches the screen.
        var unitKey = "unit." + location.id();
        return MessageCatalogue.hasKey(locale, unitKey) ? Display.title(locale, null, location.id()) : "";
    }

    /**
     * @param match  the match
     * @param locale the interface locale
     * @return the word {@code ended} with the outcome, or an empty string for running work
     */
    static String endedOf(FindMatch match, String locale) {
        if (!match.ended()) {
            return "";
        }
        var ended = MessageCatalogue.format(locale, "overview.find.ended", Map.of());
        var outcome = match.outcome();
        if (outcome == null || outcome.isBlank()) {
            return ended;
        }
        return ended + SEPARATOR + MessageCatalogue.format(locale, "overview.ended.outcome",
                            Map.of("outcome", outcome));
    }

    /**
     * @param match the match
     * @return the command that opens the match where it belongs
     */
    static String openCommand(FindMatch match) {
        var id = match.id();
        return switch (match.kind()) {
            case PLAN -> OPEN_PLAN + " " + id;
            case EPIC -> OPEN_EPIC + " " + id;
            case DECISION -> OPEN_DECISION + " " + id;
            case REPORT -> OPEN_REPORT + " " + OPEN_REPORT_VERB + " " + id;
            case PROJECT -> OPEN_PROJECT + " " + OPEN_PROJECT_FILTER + " " + id;
        };
    }

    /**
     * Offers the matches as a choice and opens the one selected.
     *
     * @param page   the page of matches
     * @param locale the interface locale
     * @param out    the writer the prompt is drawn to
     * @return the exit code of the opened view, {@code 0} when the operator leaves
     * @throws IOException when the terminal fails
     */
    private int choose(FindPage page, String locale, PrintWriter out) throws IOException {
        var matches = page.matches();
        try (var lease = context.openTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
            while (true) {
                out.print(MessageCatalogue.format(locale, "operator.find.choice",
                                    Map.of("count", matches.size())) + StartPrompt.SUFFIX);
                out.flush();
                var line = readChoice(reader);
                if (line == null || ChoicePrompt.isLeave(locale, line)) {
                    return 0;
                }
                var selected = numberOf(line, matches.size());
                if (selected != null) {
                    return open(matches.get(selected - 1), out);
                }
                out.println(MessageCatalogue.format(locale, "operator.dialogue.not_offered",
                                    Map.of("input", TerminalSanitizer.sanitizeLine(line),
                                                        "offered", offered(matches.size(), locale))));
                out.flush();
            }
        } catch (UserInterruptException _) {
            return StartPrompt.EXIT_INTERRUPTED;
        }
    }

    private static String offered(int count, String locale) {
        return "1-" + count + ", " + leaveKey(locale);
    }

    /**
     * @param locale the interface locale
     * @return the key that leaves the choice, as the catalogue text names it
     */
    static String leaveKey(String locale) {
        return locale.toLowerCase(Locale.ROOT).startsWith("de") ? "v" : LEAVE;
    }

    private static @Nullable Integer numberOf(String line, int count) {
        try {
            var number = Integer.parseInt(line);
            return number >= 1 && number <= count ? number : null;
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static @Nullable String readChoice(LineReader reader) {
        try {
            return reader.readLine("").strip();
        } catch (EndOfFileException _) {
            return null;
        }
    }

    /**
     * Opens one match where it belongs, through the verb of its kind.
     *
     * @param match the match to open
     * @param out   the writer the view draws on
     * @return the exit code of the view
     */
    private int open(FindMatch match, PrintWriter out) {
        return PmOperator.execute(context, out, out, openCommand(match).split(" "));
    }

    /**
     * Reads one line for the console's {@code /} field and answers {@code Esc} by leaving the field
     * with nothing sent, and {@code Ctrl+C} like {@code Esc}: the operator typed a name, not a
     * command line, so the field is read key by key and leaves on the first one that is not text.
     *
     * @param reader the console's line reader, whose terminal draws the field
     * @param prompt the drawn prompt
     * @return the typed text, or {@code null} when the operator left the field
     */
    static @Nullable String readField(LineReader reader, String prompt) {
        var terminal = reader.getTerminal();
        var attributes = terminal.enterRawMode();
        try {
            terminal.writer().print(prompt);
            terminal.writer().flush();
            return readKeyedText(terminal);
        } finally {
            terminal.setAttributes(attributes);
            terminal.flush();
        }
    }

    private static @Nullable String readKeyedText(Terminal terminal) {
        var chars = terminal.reader();
        var text = new StringBuilder();
        while (true) {
            var typed = readKey(chars);
            if (leavesField(typed)) {
                endField(terminal);
                return null;
            }
            if (acceptsText(typed)) {
                endField(terminal);
                return text.toString().strip();
            }
            if (erases(typed)) {
                eraseLast(terminal, text);
            } else if (!Character.isISOControl(typed)) {
                text.append((char) typed);
                echo(terminal, (char) typed);
            }
        }
    }

    /**
     * Reads one key. An {@code Esc} also drains what a longer sequence left behind: an arrow key or
     * an Alt combination sends more characters, and they would be read as typed text at the next
     * prompt.
     *
     * @param chars the terminal's characters
     * @return the key, or {@code -1} when the stream ended or failed
     */
    private static int readKey(NonBlockingReader chars) {
        int typed;
        try {
            typed = chars.read();
        } catch (IOException _) {
            return -1;
        }
        if (typed == ESCAPE) {
            discardPending(chars);
        }
        return typed;
    }

    private static boolean leavesField(int typed) {
        return typed < 0 || typed == ESCAPE || typed == INTERRUPT;
    }

    private static boolean acceptsText(int typed) {
        return typed == CARRIAGE_RETURN || typed == LINE_FEED;
    }

    private static boolean erases(int typed) {
        return typed == BACKSPACE || typed == DELETE;
    }

    private static void echo(Terminal terminal, char typed) {
        terminal.writer().print(typed);
        terminal.writer().flush();
    }

    private static void eraseLast(Terminal terminal, StringBuilder text) {
        if (text.isEmpty()) {
            return;
        }
        text.deleteCharAt(text.length() - 1);
        terminal.writer().print("\b \b");
        terminal.writer().flush();
    }

    private static void endField(Terminal terminal) {
        terminal.writer().println();
        terminal.writer().flush();
    }

    /**
     * Drops what a terminal sequence left behind after its {@code ESC}: an arrow key or an Alt
     * combination sends more characters, and they would be read as typed text at the next prompt.
     *
     * @param chars the terminal's characters
     */
    private static void discardPending(NonBlockingReader chars) {
        try {
            var pending = chars.peek(PEEK_MILLIS);
            while (pending >= 0) {
                pending = discardOne(chars);
            }
        } catch (IOException _) {
            // Nothing more to discard: the field leaves as it is.
        }
    }

    private static int discardOne(NonBlockingReader chars) throws IOException {
        var discarded = chars.read();
        return discarded < 0 ? -1 : chars.peek(PEEK_MILLIS);
    }

    /** The runtime the request goes to, so a case can hand one in. */
    @FunctionalInterface
    interface RuntimeSender {
        HttpResponse send(HttpRequest request) throws IOException;
    }
}
