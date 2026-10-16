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
import de.planmarshall.api.runtime.RuntimeUnavailableException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator drain}: signals the runtime to stop by its drain (01-commands.adoc#commands).
 * Keeps running without operator checks. Never starts the runtime.
 */
@Command(name = "drain", description = "Signals the runtime to stop after completing active work.")
public final class DrainCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    DrainCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        try {
            var body = JsonTree.write(Map.of("drain", true));
            var response = context.runtime(false).send(
                                HttpRequest.json("POST", "/api/v1/runtime/stop", body));
            if (response.status() == 200 || response.status() == 202) {
                out.println("runtime   draining");
            } else {
                var err = spec.commandLine().getErr();
                err.printf("Failed to drain runtime: HTTP %d%n", response.status());
                err.flush();
                return 1;
            }
        } catch (RuntimeUnavailableException _) {
            out.println("runtime   not running");
        }
        out.flush();
        return 0;
    }
}
