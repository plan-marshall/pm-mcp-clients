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

import de.planmarshall.api.start.EntryKind;

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

        @Option(names = "--description", description = "The request the recipe starts from")
        @Nullable
        String description;

        @Option(names = "--document", description = "Input documents")
        List<String> documents = new ArrayList<>();

        @Option(names = "--issue", description = "Input issue URL or number")
        List<String> issues = new ArrayList<>();

        @Option(names = "--copy", description = "Copy input documents")
        boolean copy;

        @Option(names = "--move", description = "Move input documents")
        boolean move;

        @Option(names = "--no-input", description = "Stop on missing required input")
        boolean noInput;

        @Option(names = "--entry", description = "Continue starting an open entry")
        @Nullable
        String entryId;

        RunCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var answers = new StartAnswers()
                                .withProject(project)
                                .withDescription(description)
                                .withRecipe(recipeKey);
            answers.recipeCommand = true;
            answers.title = title;
            answers.parameters.putAll(params);
            answers.documents.addAll(documents);
            answers.documents.addAll(sources);
            answers.issues.addAll(issues);
            answers.copy = copy;
            answers.move = move;
            answers.noInput = noInput;
            answers.entryId = entryId;
            return StartDialogue.run(context, spec.commandLine().getOut(), EntryKind.PLAN, answers);
        }
    }
}
