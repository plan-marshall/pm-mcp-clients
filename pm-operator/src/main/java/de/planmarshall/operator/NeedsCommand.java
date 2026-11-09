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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.display.Display;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.runtime.RuntimeClient;

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

    NeedsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var client = context.runtime(true);
        var response = client.send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR));
        if (response.status() != 200) {
            throw new OperatorRefusedException("needs", response);
        }

        if (json) {
            out.println(new String(response.body(), StandardCharsets.UTF_8));
            out.flush();
            return 0;
        }

        var page = NeedsOperatorPage.fromJson(response.bodyText());
        var locale = context.clientEnvironment().locale();

        if (page.items().isEmpty()) {
            var emptyKey = MessageCatalogue.hasKey(locale, "operator.needs.empty")
                    ? "operator.needs.empty"
                    : "overview.needs.empty";
            out.println(MessageCatalogue.format(locale, emptyKey, Map.of()));
            out.flush();
            return 0;
        }

        var decisionIds = fetchDecisionIds(client);
        if (MessageCatalogue.hasKey(locale, "operator.needs.title")) {
            out.println(MessageCatalogue.format(locale, "operator.needs.title", Map.of("total", page.total())));
        } else {
            out.printf("Needs you · %d%n", page.total());
        }

        for (var item : page.items()) {
            printItem(out, item, locale, decisionIds);
        }
        out.flush();
        return 0;
    }

    private void printItem(PrintWriter out, NeedsOperatorItem item, String locale, Map<String, String> decisionIds) {
        var kindLabel = OverviewFormatting.formatNeedsItemKind(item.kind(), locale);
        var subject = OverviewFormatting.formatNeedsItemSubject(item);
        var blocks = OverviewFormatting.formatNeedsItemBlocks(item.blocks(), locale);
        var since = OverviewFormatting.formatNeedsItemSince(item.since(), locale, context.clock());
        var cmd = OverviewFormatting.formatNeedsItemCommand(item, decisionIds);

        out.printf("  %-16s %-16s %-19s %-13s %s%n",
                "! " + kindLabel,
                subject,
                blocks,
                since,
                cmd);
    }

    private static Map<String, String> fetchDecisionIds(RuntimeClient client) {
        try {
            var resp = client.send(HttpRequest.get("/api/v1/decisions"));
            if (resp.status() != 200) {
                resp = client.send(HttpRequest.get("/api/v1/questions"));
            }
            if (resp.status() == 200) {
                var parsed = JsonTree.parse(resp.body());
                if (parsed instanceof List<?> list) {
                    var map = new HashMap<String, String>();
                    for (var el : list) {
                        if (el instanceof Map<?, ?> m) {
                            var id = m.get("id");
                            var plan = m.get("plan");
                            var proj = m.get("project");
                            if (id != null && plan != null) {
                                map.put(plan.toString(), id.toString());
                            } else if (id != null && proj != null) {
                                map.put(proj.toString(), id.toString());
                            }
                        }
                    }
                    return map;
                }
            }
        } catch (IOException _) {
            // Fall back gracefully to empty map if decisions route is unavailable
        }
        return Map.of();
    }
}
