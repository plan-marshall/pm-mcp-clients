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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The answers a starting verb carries on its command line (design <em>Starting Work</em>, a direct
 * verb). Every answer is optional: what the operator did not answer is asked in the dialogue, or,
 * with {@code --no-input}, stops the run with the state it needs.
 * <p>
 * The description {@code -} is the one value that is not literal: it is read from standard input by
 * {@link StartDialogue}.
 */
final class StartAnswers {

    /** The description argument that reads the text from standard input. */
    static final String STDIN = "-";

    @Nullable
    String project;
    @Nullable
    String description;
    final List<String> documents = new ArrayList<>();
    final List<String> issues = new ArrayList<>();
    @Nullable
    String lesson;
    boolean localChanges;
    @Nullable
    String pullRequest;
    boolean copy;
    boolean move;
    @Nullable
    String title;
    @Nullable
    String id;
    @Nullable
    String recipeKey;
    boolean recipeCommand;
    final Map<String, String> parameters = new LinkedHashMap<>();
    @Nullable
    String adopt;
    @Nullable
    String entryId;
    @Nullable
    String instruction;
    boolean noInput;

    /**
     * @return {@code true} if the verb carries no source at all
     */
    boolean noSources() {
        return description == null && documents.isEmpty() && issues.isEmpty() && lesson == null
                            && !localChanges && pullRequest == null;
    }

    /**
     * @return the documents mode the verb names, or {@code null} when it names none
     */
    @Nullable
    String documentsMode() {
        if (copy && move) {
            throw new IllegalArgumentException("--copy and --move exclude each other");
        }
        if (copy) {
            return "copy";
        }
        return move ? "move" : null;
    }

    /**
     * @param value the value of {@code --description}
     * @return this builder
     */
    StartAnswers description(@Nullable String value) {
        this.description = value;
        return this;
    }

    /**
     * @param value the project id
     * @return this builder
     */
    StartAnswers project(@Nullable String value) {
        this.project = value;
        return this;
    }

    /**
     * @param value the recipe key
     * @return this builder
     */
    StartAnswers recipe(@Nullable String value) {
        this.recipeKey = value;
        return this;
    }

    /**
     * @param name  the parameter name
     * @param value the parameter value
     * @return this builder
     */
    StartAnswers parameter(String name, String value) {
        parameters.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }
}
