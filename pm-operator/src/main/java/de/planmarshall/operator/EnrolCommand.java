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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.EnrolRequest;
import de.planmarshall.api.resource.EnrolResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator enrol}: the direct verb of a project's enrolment, its arguments the answers of
 * {@code project.identity} to {@code project.enrol} (PM-EXEC-2; CLI &amp; Security § Enrolment,
 * Lifecycle Gate &amp; Release).
 * <p>
 * It is an operator verb (PM-SEC-3): the {@link OperatorGate} refuses it outside an interactive
 * terminal or inside a coding-agent environment, and it reaches the stores only through the
 * runtime's local API.
 */
@Command(name = "enrol", description = "Enrols a project from the specified or current directory.")
@OperatorVerb
public final class EnrolCommand implements Callable<Integer> {

    private static final String PATH_ENROL = "/api/v1/enrol";
    private static final String ERR_ENROLMENT_ROOT_CONFLICT = "enrolment_root_conflict";
    private static final String ERR_PROJECT_STORE_ROOT_MISMATCH = "project_store_root_mismatch";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", arity = "0..1", defaultValue = ".",
                        description = "Checkout path to enrol; the working directory by default")
    String path = ".";

    @Option(names = "--pilot", description = "Enrol the project in pilot mode (the lifecycle gate)")
    boolean pilot;

    EnrolCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        var safePath = OutputSafety.stripControlSequences(path);

        var request = new EnrolRequest(path, pilot);
        var response = context.runtime(true).send(HttpRequest.json("POST", PATH_ENROL, request.toJson()));

        if (response.status() == 200 || response.status() == 201) {
            var result = EnrolResult.fromJson(response.bodyText());
            var id = OutputSafety.stripControlSequences(result.projectId());
            var mode = MessageCatalogue.get(result.pilot() ? "enrolment.enrol.mode.pilot"
                                : "enrolment.enrol.mode.standard");
            out.println(MessageCatalogue.format("enrolment.enrol.done", Map.of("id", id, "mode", mode)));
            out.flush();
            return 0;
        }

        if (response.status() == 409) {
            var reason = refusalReason(response.bodyText());
            if (ERR_ENROLMENT_ROOT_CONFLICT.equals(reason)) {
                out.println(MessageCatalogue.format("enrolment.enrol.refused.root_conflict",
                                    Map.of("path", safePath)));
            } else if (ERR_PROJECT_STORE_ROOT_MISMATCH.equals(reason)) {
                out.println(MessageCatalogue.format("enrolment.enrol.refused.store_root_mismatch",
                                    Map.of("id", safePath)));
            } else {
                out.println(MessageCatalogue.format("enrolment.enrol.failed",
                                    Map.of("path", safePath, "status", response.status())));
            }
            out.flush();
            return 1;
        }

        if (response.status() >= 400 && response.status() < 500) {
            err.println(MessageCatalogue.format("enrolment.enrol.failed",
                                Map.of("path", safePath, "status", response.status())));
            err.flush();
            return 1;
        }
        throw new OperatorRefusedException("enrol " + safePath, response);
    }

    private static String refusalReason(String body) {
        try {
            var map = JsonTree.asObject(JsonTree.parse(body));
            var reason = JsonTree.string(map, "reason");
            if (reason == null) {
                reason = JsonTree.string(map, "error");
            }
            return reason != null ? reason : "";
        } catch (IOException _) {
            return "";
        }
    }
}
