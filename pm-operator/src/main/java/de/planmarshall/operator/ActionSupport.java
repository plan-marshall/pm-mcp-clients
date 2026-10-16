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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shared action execution and formatting support for plan, epic, and abort verbs.
 */
final class ActionSupport {

    private ActionSupport() {
    }

    static class TerminationOptions {
        @Parameters(index = "0", arity = "0..1", description = "Work item ID")
        @Nullable
        String id;

        @Option(names = "--reason", description = "Reason for termination")
        @Nullable
        String reason;

        @Option(names = "--close-pr", description = "Close open pull request")
        boolean closePr;

        @Option(names = "--keep-pr", description = "Keep open pull request")
        boolean keepPr;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;
    }

    static class CommonAddOptions {
        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--title", description = "Title")
        @Nullable
        String title;

        @Option(names = "--id", description = "Explicit ID")
        @Nullable
        String id;

        @Option(names = "--description", description = "Description")
        @Nullable
        String description;
    }

    static void populateCommonAdd(Map<String, Object> payload, CommonAddOptions opts) {
        if (opts.project != null && !".".equals(opts.project)) {
            payload.put("project", opts.project);
        }
        if (opts.title != null) payload.put("title", opts.title);
        if (opts.id != null) payload.put("id", opts.id);
        if (opts.description != null) payload.put("description", opts.description);
    }

    static final String CANCEL = "cancel";

    /**
     * Executes work cancellation or abandonment (plan, epic).
     *
     * @param context    the operator context
     * @param spec       the picocli command specification
     * @param kind       "plan" or "epic"
     * @param options    the termination options
     * @param actionVerb "cancel" or "abandon"
     * @return exit code (0 on success, 3 on missing input)
     * @throws IOException on network or runtime error
     */
    static int endWork(OperatorContext context, CommandSpec spec, String kind,
                                    TerminationOptions options, String actionVerb) throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var id = options.id != null ? options.id : "";
        String effectiveReason = options.reason;
        if (effectiveReason == null || effectiveReason.isBlank()) {
            if (options.noInput) {
                var label = CANCEL.equalsIgnoreCase(actionVerb) ? "Cancellation" : "Abandon";
                out.printf("%s reason required (--reason)%n", label);
                out.flush();
                return 3;
            }
            effectiveReason = (CANCEL.equalsIgnoreCase(actionVerb) ? "Cancelled" : "Abandoned") + " by operator";
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("reason", effectiveReason);
        payload.put("close_pr", !options.keepPr);

        var endpoint = "/api/v1/" + kind + "s/" + OperatorUrlUtil.encodePathSegment(id) + "/" + actionVerb;
        var response = context.runtime(true).send(HttpRequest.json("POST", endpoint, JsonTree.write(payload)));
        if (response.status() == 200 || response.status() == 202) {
            var past = CANCEL.equalsIgnoreCase(actionVerb) ? "cancelled" : "abandoned";
            out.printf("✓ %s %s and archived%n", id, past);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException(kind + " " + actionVerb, response);
    }

    /**
     * Executes plan or epic chat communication.
     *
     * @param context the operator context
     * @param spec    the picocli command specification
     * @param kind    "plan" or "epic"
     * @param id      the work item ID
     * @param message the chat message, or {@code null} to fetch history
     * @return exit code
     * @throws IOException on network or runtime error
     */
    static int chat(OperatorContext context, CommandSpec spec, String kind, String id,
                                    @Nullable String message) throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var endpoint = "/api/v1/" + kind + "s/" + OperatorUrlUtil.encodePathSegment(id) + "/chat";
        if (message != null && !message.isBlank()) {
            var response = context.runtime(true).send(
                                            HttpRequest.json("POST", endpoint, JsonTree.write(Map.of("message", message))));
            if (response.status() == 200 || response.status() == 201) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException(kind + " chat", response);
        }

        var response = context.runtime(true).send(HttpRequest.get(endpoint));
        if (response.status() == 200) {
            out.println(response.bodyText());
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException(kind + " chat", response);
    }

    /**
     * Renders a list of running work items (plans, epics).
     *
     * @param out      the output writer
     * @param heading  the list header
     * @param stateKey the property name for state/phase
     * @param list     the list of work item maps
     */
    static void printWorkList(PrintWriter out, String heading, String stateKey, List<?> list) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(stateKey, "stateKey");
        if (list.isEmpty()) {
            out.println("No " + heading.toLowerCase() + " running.");
            return;
        }
        out.println(heading);
        for (var item : list) {
            if (item instanceof Map<?, ?> work) {
                var title = work.get("title");
                out.printf("  %-20s %-12s %s%n",
                                                work.get("id"),
                                                work.get(stateKey),
                                                title != null ? title : "");
            } else {
                out.println("  " + item);
            }
        }
    }
}
