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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import de.planmarshall.api.message.MessageCatalogue;

import picocli.CommandLine.Command;

/**
 * The one source of the operator's verb names: the subcommands {@link PmOperator} registers. The
 * console's completer and its {@code help} text both read this list, so a verb is added or removed
 * in one place and nowhere else.
 * <p>
 * The design's command tree ({@code doc/design/terminal-client/01-commands.adoc}) stays the
 * contract: the test compares this list with the verbs the design declares, so a verb registered
 * here without the design fails, and the console's {@code help} names every verb registered here.
 */
final class OperatorVerbs {

    /** The commands only the console offers, beside the verbs of the command tree. */
    private static final List<String> CONSOLE_ONLY = List.of("exit", "quit", "help", "tui");

    private OperatorVerbs() {
    }

    /**
     * @return the verbs {@link PmOperator} registers, in the order it declares them
     */
    static List<String> registered() {
        var declared = PmOperator.class.getAnnotation(Command.class);
        var verbs = new ArrayList<String>();
        for (var type : declared.subcommands()) {
            verbs.add(type.getAnnotation(Command.class).name());
        }
        return List.copyOf(verbs);
    }

    /**
     * @return the words the console completes: the registered verbs and the console's own commands,
     *         each once and in one order
     */
    static List<String> completer() {
        var words = new TreeSet<>(registered());
        words.addAll(CONSOLE_ONLY);
        return List.copyOf(words);
    }

    /**
     * @param locale the interface locale
     * @return the sentence that names every verb of the command tree, derived from the registry
     */
    static String helpMore(String locale) {
        return MessageCatalogue.format(locale, "operator.console.help.more",
                            Map.of("verbs", String.join(", ", registered())));
    }
}
