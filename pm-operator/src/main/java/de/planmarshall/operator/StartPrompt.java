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
import java.util.Map;
import java.util.Objects;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * The input and output surface of the starting dialogue: every line the operator reads comes from
 * the message catalogue, and the one terminal of the context supplies the answers.
 * <p>
 * With {@code --no-input} nothing is read: a question that has no answer among the arguments stops
 * the run with {@link MissingInputException}, which names the state and what it needs.
 */
public final class StartPrompt {

    /** The suffix after every question, as the terminal client draws it. */
    public static final String SUFFIX = "  \u203a ";
    private static final String EMPTY = "";

    private final OperatorContext context;
    private final PrintWriter out;
    private final String locale;
    private final boolean noInput;
    private @Nullable LineReader reader;

    /**
     * @param context the operator context
     * @param out     the output of the verb
     * @param locale  the interface locale
     * @param noInput whether the verb may ask nothing
     */
    public StartPrompt(OperatorContext context, PrintWriter out, String locale, boolean noInput) {
        this.context = Objects.requireNonNull(context, "context");
        this.out = Objects.requireNonNull(out, "out");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.noInput = noInput;
    }

    /**
     * @return the interface locale
     */
    public String locale() {
        return locale;
    }

    /**
     * @return the writer of the dialogue
     */
    public PrintWriter out() {
        return out;
    }

    /**
     * @return {@code true} if the verb may ask
     */
    public boolean asks() {
        return !noInput;
    }

    /**
     * Prints a catalogue line.
     *
     * @param key  the message key
     * @param args the arguments
     */
    void line(String key, Map<String, ?> args) {
        out.println(MessageCatalogue.format(locale, key, args));
        out.flush();
    }

    /**
     * Prints a line that is already rendered from the catalogue.
     *
     * @param text the line
     */
    void raw(String text) {
        out.println(text);
        out.flush();
    }

    /**
     * Prints an empty line.
     */
    void blank() {
        out.println();
        out.flush();
    }

    /**
     * Asks the operator a question of the catalogue.
     *
     * @param key       the message key of the question
     * @param args      the arguments of the question
     * @param state     the state id, named when {@code --no-input} stops the run
     * @param need      what the state needs, named when {@code --no-input} stops the run
     * @param entryId   the entry, named when {@code --no-input} stops the run
     * @return the trimmed answer, possibly empty
     * @throws MissingInputException with {@code --no-input}
     */
    String ask(String key, Map<String, ?> args, String state, String need, @Nullable String entryId) {
        requireAsk(state, need, entryId);
        var question = MessageCatalogue.format(locale, key, args);
        out.print(question + SUFFIX);
        out.flush();
        return readLine();
    }

    /**
     * Asks a question whose text is already rendered.
     *
     * @param question the question
     * @param state    the state id
     * @param need     what the state needs
     * @param entryId  the entry
     * @return the trimmed answer
     */
    String askRaw(String question, String state, String need, @Nullable String entryId) {
        requireAsk(state, need, entryId);
        out.print(question + SUFFIX);
        out.flush();
        return readLine();
    }

    /**
     * Asks with a prompt that already carries its own terminator, such as the chat's {@code you ›}.
     *
     * @param rendered the rendered prompt
     * @param state    the state id
     * @param need     what the state needs
     * @param entryId  the entry
     * @return the trimmed answer
     */
    String askInline(String rendered, String state, String need, @Nullable String entryId) {
        requireAsk(state, need, entryId);
        out.print(rendered);
        out.flush();
        return readLine();
    }

    /**
     * Asks for a reason, which an acceptance records.
     *
     * @param state   the state id
     * @param need    what the state needs
     * @param entryId the entry
     * @return the reason
     */
    String askReason(String state, String need, @Nullable String entryId) {
        return ask("start.source.reason", Map.of(), state, need, entryId);
    }

    /**
     * @param state   the state id
     * @param need    what the state needs
     * @param entryId the entry
     * @throws MissingInputException always
     */
    static void missing(String state, String need, @Nullable String entryId) {
        throw new MissingInputException(state, need, entryId);
    }

    private void requireAsk(String state, String need, @Nullable String entryId) {
        if (noInput) {
            missing(state, need, entryId);
        }
    }

    private String readLine() {
        try {
            return Objects.requireNonNullElse(reader().readLine(), EMPTY).trim();
        } catch (EndOfFileException | UserInterruptException _) {
            throw new StartLeftException();
        }
    }

    public LineReader reader() {
        if (reader == null) {
            try {
                reader = LineReaderBuilder.builder().terminal(context.createTerminal()).build();
            } catch (IOException e) {
                throw new IllegalStateException("cannot open the terminal: " + e.getMessage(), e);
            }
        }
        return reader;
    }

    /** The run stopped because the operator left the dialogue. */
    public static final class StartLeftException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StartLeftException() {
            super();
        }
    }

    /** The run stopped because {@code --no-input} met a state that needs the operator. */
    public static final class MissingInputException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        private final String state;
        private final String need;
        private final @Nullable String entryId;

        public MissingInputException(String state, String need, @Nullable String entryId) {
            super(state + " needs " + need);
            this.state = state;
            this.need = need;
            this.entryId = entryId;
        }

        public String state() {
            return state;
        }

        public String need() {
            return need;
        }

        public @Nullable String entryId() {
            return entryId;
        }
    }
}
