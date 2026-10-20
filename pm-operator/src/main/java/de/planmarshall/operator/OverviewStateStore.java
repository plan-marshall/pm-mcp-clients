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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Optional;

import de.planmarshall.api.runtime.RuntimeTokenFile;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Reads and writes {@code $XDG_STATE_HOME/pm-operator/overview.json}, where the operator left the
 * overview (doc/design/terminal-client/15-overview.adoc#overview-persistence).
 *
 * <p>The file is written atomically: a temporary file beside it, mode {@code 0600}, then a move
 * over the target, so a crash never leaves a half-written state. A read verifies the directory's
 * and the file's permissions first and falls back to an empty state when the file is missing,
 * unreadable, insecure, oversized, or corrupt — the view then opens at its default, never fails.
 *
 * <p>Like the history, the file lies outside {@code <PM_MCP_BASE>} and holds no content of a plan.
 */
public final class OverviewStateStore {

    /** The name of the file below the operator state directory. */
    public static final String FILE_NAME = "overview.json";

    /** The largest file the store reads; a hand-edited larger file is treated as corrupt. */
    public static final long MAX_BYTES = 64L * 1024L;

    private static final JsonFactory FACTORY = new JsonFactory();

    private final OperatorStateDirectory directory;

    /**
     * @param directory the operator state directory
     */
    public OverviewStateStore(OperatorStateDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    /** @return the path of the stored state */
    public Path file() {
        return directory.overviewFile();
    }

    /**
     * Reads the state.
     *
     * @return the stored state, or empty when there is none or it cannot be trusted
     */
    public Optional<OverviewSavedState> read() {
        var path = file();
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try {
            OperatorStateDirectory.verifyDirectory(directory.root());
            OperatorStateDirectory.verifyFile(path);
            if (Files.size(path) > MAX_BYTES) {
                return Optional.empty();
            }
            try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ);
                 var parser = FACTORY.createParser(input)) {
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    return Optional.empty();
                }
                var state = OverviewSavedState.fromJson(parser);
                if (parser.nextToken() != null) {
                    return Optional.empty();
                }
                return Optional.of(state);
            }
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException _) {
            // Missing, insecure, unreadable, or corrupt: the view opens at its default.
            return Optional.empty();
        }
    }

    /**
     * Writes the state atomically and verifies the resulting file.
     *
     * @param state the state to store
     * @throws IOException if the state cannot be written or fails verification
     */
    public void write(OverviewSavedState state) throws IOException {
        Objects.requireNonNull(state, "state");
        var root = directory.ensureDirectory();
        Path temporary = null;
        try {
            temporary = Files.createTempFile(root, ".overview", ".tmp",
                                PosixFilePermissions.asFileAttribute(RuntimeTokenFile.FILE_MODE));
            try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.WRITE,
                                     StandardOpenOption.TRUNCATE_EXISTING);
                 JsonGenerator generator = FACTORY.createGenerator(output)) {
                state.writeTo(generator);
            }
            OperatorStateDirectory.setFilePermissions(temporary);
            OperatorStateDirectory.verifyFile(temporary);
            try {
                Files.move(temporary, file(), StandardCopyOption.ATOMIC_MOVE,
                                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException _) {
                Files.move(temporary, file(), StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
            OperatorStateDirectory.setFilePermissions(file());
            OperatorStateDirectory.verifyFile(file());
        } finally {
            if (temporary != null) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
