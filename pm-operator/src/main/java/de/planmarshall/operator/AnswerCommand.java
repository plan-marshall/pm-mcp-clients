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
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator answer <id> [<option>]}: answers an open question or decision.
 */
@Command(name = "answer", description = "Answers an open question or decision.")
@OperatorVerb
public final class AnswerCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Question ID (e.g. Q-007)")
    String questionId = "";

    @Parameters(index = "1", arity = "0..1", description = "Option chosen")
    @Nullable
    String option;

    @Option(names = "--words", description = "Answer in operator's own words")
    @Nullable
    String words;

    @Option(names = "--note", description = "Note to accompany the answer")
    @Nullable
    String note;

    @Option(names = "--plan", description = "Scope plan ID")
    @Nullable
    String plan;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    AnswerCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if (option == null && (words == null || words.isBlank())) {
            // Read question detail
            var qPath = plan != null
                    ? "/api/v1/plans/" + plan + "/decisions/" + questionId
                    : "/api/v1/decisions/" + questionId;
            var qResponse = runtime.send(HttpRequest.get(qPath));
            if (qResponse.status() == 200) {
                out.println(qResponse.bodyText());
            } else if (qResponse.status() == 404) {
                out.printf("Question %s not found.%n", questionId);
                out.flush();
                return 1;
            }
            if (noInput) {
                out.println("Choice or option required (--no-input specified).");
                out.flush();
                return 3;
            }
            out.flush();
            return 0;
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("channel", "cli");
        if (option != null) payload.put("option", option);
        if (words != null) payload.put("words", words);
        if (note != null) payload.put("note", note);

        var answerPath = plan != null
                ? "/api/v1/plans/" + plan + "/decisions/" + questionId + "/answer"
                : "/api/v1/decisions/" + questionId + "/answer";

        var response = runtime.send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (response.status() == 404) {
            response = runtime.send(HttpRequest.json("POST", "/api/v1/questions/" + questionId + "/answers", JsonTree.write(payload)));
        }
        if (response.status() == 200 || response.status() == 202 || response.status() == 204) {
            var answerDisplay = option != null ? option : words;
            out.printf("✓ Answered %s with option %s.%n", questionId, answerDisplay);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + questionId, response);
    }
}
