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
import de.planmarshall.api.resource.UnenrolRequest;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator unenrol <project_id>}: removes a project's enrolment, refused while work of it
 * runs (PM-EXEC-2; CLI &amp; Security § Enrolment, Lifecycle Gate &amp; Release).
 * <p>
 * It is an operator verb (PM-SEC-3); the enrolment and nothing else is removed, the project's
 * machine store stays for the operator's cleanup.
 */
@Command(name = "unenrol", description = "Removes a project's enrolment.")
@OperatorVerb
public final class UnenrolCommand implements Callable<Integer> {

    private static final String PREFIX_PROJECTS = "/api/v1/projects/";
    private static final String SUFFIX_UNENROL = "/unenrol";
    private static final String ERR_SCOPE_ACTIVE = "scope_active";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Project ID")
    String projectId;

    @Option(names = {"-y", "--yes"}, description = "Confirm unenrolment without an interactive prompt")
    boolean yes;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    UnenrolCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        var safeId = OutputSafety.stripControlSequences(projectId);
        var encodedId = OperatorUrlUtil.encodePathSegment(projectId);

        if (!yes && noInput) {
            err.println(MessageCatalogue.get("enrolment.unenrol.confirm_required"));
            err.flush();
            return 3;
        }

        var request = new UnenrolRequest(projectId);
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", PREFIX_PROJECTS + encodedId + SUFFIX_UNENROL, request.toJson()));

        if (response.status() == 200 || response.status() == 204) {
            out.println(MessageCatalogue.format("enrolment.unenrol.done", Map.of("id", safeId)));
            out.flush();
            return 0;
        }
        if (response.status() == 404) {
            err.println(MessageCatalogue.format("enrolment.unenrol.not_found", Map.of("id", safeId)));
            err.flush();
            return 1;
        }
        if (response.status() == 409) {
            if (ERR_SCOPE_ACTIVE.equals(refusalReason(response.bodyText()))) {
                err.println(MessageCatalogue.format("enrolment.unenrol.scope_active", Map.of("id", safeId)));
            } else {
                err.println(MessageCatalogue.format("enrolment.unenrol.failed",
                                    Map.of("id", safeId, "status", response.status())));
            }
            err.flush();
            return 1;
        }
        throw new OperatorRefusedException("unenrol " + safeId, response);
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
