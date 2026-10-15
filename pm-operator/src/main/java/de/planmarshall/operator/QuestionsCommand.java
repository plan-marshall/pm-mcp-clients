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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator questions}: lists open decisions and questions across plans and projects.
 */
@Command(name = "questions", description = "Lists open decisions and questions.")
@OperatorVerb
public final class QuestionsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--project", description = "Filter by project ID")
    @Nullable
    String project;

    @Option(names = "--plan", description = "Filter by plan ID")
    @Nullable
    String plan;

    QuestionsCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var path = OperatorUrlUtil.appendScope("/api/v1/decisions", project, plan);
        var response = context.runtime(true).send(HttpRequest.get(path));
        if (response.status() == 404) {
            response = context.runtime(true).send(
                    HttpRequest.get(OperatorUrlUtil.appendScope("/api/v1/questions", project, plan)));
        }
        if (response.status() == 200) {
            printQuestions(out, response.body());
            return 0;
        }
        throw new OperatorRefusedException("questions", response);
    }

    private static void printQuestions(PrintWriter out, byte[] body) throws IOException {
        var parsed = JsonTree.parse(body);
        if (parsed instanceof List<?> list && !list.isEmpty()) {
            out.printf("Open Questions · %d pending%n", list.size());
            for (var item : list) {
                printQuestionItem(out, item);
            }
        } else {
            out.println("No open questions.");
        }
        out.flush();
    }

    private static void printQuestionItem(PrintWriter out, Object item) {
        if (item instanceof Map<?, ?> q) {
            Object scopeObj = q.get("plan");
            if (scopeObj == null) {
                scopeObj = q.get("scope");
            }
            var scope = scopeObj != null ? scopeObj.toString() : "";
            Object textObj = q.get("title");
            if (textObj == null) {
                textObj = q.get("question");
            }
            var text = textObj != null ? textObj.toString() : "";
            out.printf("  %-8s %-20s %s%n", q.get("id"), scope, text);
        } else {
            out.println("  " + item);
        }
    }
}
