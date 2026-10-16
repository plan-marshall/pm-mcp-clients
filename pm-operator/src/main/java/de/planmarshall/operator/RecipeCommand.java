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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * {@code pm-operator recipe}: run project recipes.
 */
@Command(name = "recipe", description = "Recipe operations.",
                                subcommands = {
                                                                RecipeCommand.RunCommand.class
                                })
@OperatorVerb
public final class RecipeCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec = CommandSpec.create();

    RecipeCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "run", description = "Runs a recipe of the project.")
    @OperatorVerb
    static final class RunCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", arity = "0..1", description = "Recipe key")
        @Nullable
        String recipeKey;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--param", description = "Recipe parameter key=value")
        Map<String, String> params = new LinkedHashMap<>();

        @Option(names = "--title", description = "Recipe plan title")
        @Nullable
        String title;

        @Option(names = "--source", description = "Source input")
        List<String> sources = new ArrayList<>();

        RunCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (recipeKey == null || recipeKey.isBlank()) {
                out.println("Recipe key required.");
                out.flush();
                return 1;
            }

            var payload = new LinkedHashMap<String, Object>();
            if (!params.isEmpty()) payload.put("params", params);
            if (title != null) payload.put("title", title);
            if (!sources.isEmpty()) payload.put("sources", sources);

            var proj = project != null && !".".equals(project) ? project : "default";
            var path = "/api/v1/projects/" + OperatorUrlUtil.encodePathSegment(proj)
                                            + "/recipes/" + OperatorUrlUtil.encodePathSegment(recipeKey) + "/run";

            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 201) {
                out.printf("✓ Recipe %s started%n", recipeKey);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("recipe run", response);
        }
    }
}
