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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.operator.overview.OverviewFormatting;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator needs}: prints the list of what waits across projects in the order the runtime
 * serves it (UC-PLN-7, CLI-UX-S-04, CLI-UX-P-01).
 */
@Command(name = "needs", description = "Shows what needs operator attention across projects.")
@OperatorVerb
public final class NeedsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--json", description = "Prints the needs list as JSON.")
    boolean json;

    @Option(names = "--page", description = "The page of the list to print, 1-based.")
    int page = 1;

    NeedsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var client = context.runtime(true);
        var locale = context.clientEnvironment().locale();
        return executeNeeds(client::send, out, locale);
    }

    Integer executeNeeds(RuntimeSender sender, PrintWriter out, String locale) throws IOException {
        var response = sender.send(HttpRequest.get(pagePath()));
        if (response.status() != 200) {
            throw new OperatorRefusedException("needs", response);
        }

        if (json) {
            out.println(new String(response.body(), StandardCharsets.UTF_8));
            out.flush();
            return 0;
        }

        var list = NeedsOperatorPage.fromJson(response.bodyText());

        if (list.items().isEmpty()) {
            var emptyKey = MessageCatalogue.hasKey(locale, "operator.needs.empty")
                                ? "operator.needs.empty"
                                : "overview.needs.empty";
            out.println(MessageCatalogue.format(locale, emptyKey, Map.of()));
            out.flush();
            return 0;
        }

        out.println(MessageCatalogue.format(locale, "operator.needs.title", Map.of("total", list.total())));

        for (var item : list.items()) {
            printItem(out, item, locale);
        }
        printShowMore(out, list, locale);
        out.flush();
        return 0;
    }

    /**
     * Names the end of the loaded pages as one step when the runtime has more
     * ({@code doc/design/shared/needs-and-find.adoc#waits}): a page is never a silent cut.
     *
     * @param out    the command's writer
     * @param page   the page the runtime served
     * @param locale the interface locale
     */
    private static void printShowMore(PrintWriter out, NeedsOperatorPage page, String locale) {
        if (page.hasOlder()) {
            // The step is actionable: the next page is one option away, never a silent cut.
            out.println(MessageCatalogue.format(locale, "overview.needs.more", Map.of())
                                + "  needs --page " + (page.page() + 1));
        }
    }

    /**
     * @return the path of the page this run prints, the base endpoint for page 1
     */
    private String pagePath() {
        return page <= 1 ? MachineApiPaths.NEEDS_OPERATOR : MachineApiPaths.NEEDS_OPERATOR + "?page=" + page;
    }

    private void printItem(PrintWriter out, NeedsOperatorItem item, String locale) {
        var kindLabel = OverviewFormatting.formatNeedsItemKind(item.kind(), locale);
        var badge = MessageCatalogue.format(locale, "overview.needs.badge", Map.of("count", 1, "reason", kindLabel));
        var subject = OverviewFormatting.formatNeedsItemSubject(item, locale);
        var blocks = OverviewFormatting.formatNeedsItemBlocks(item.blocks(), locale);
        var since = OverviewFormatting.formatNeedsItemSince(item.since(), locale, context.clock());
        var cmd = OverviewFormatting.formatNeedsItemCommand(item);

        out.printf("  %-16s %-16s %-19s %-13s %s%n",
                            badge,
                            subject,
                            blocks,
                            since,
                            cmd);
    }

    @FunctionalInterface
    interface RuntimeSender {
        HttpResponse send(HttpRequest request) throws IOException;
    }
}
