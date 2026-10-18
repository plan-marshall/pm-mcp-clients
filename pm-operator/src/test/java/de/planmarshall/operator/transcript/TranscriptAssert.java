/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares a rendered terminal buffer with its recorded transcript and fails with a unified diff
 * on the smallest drift. The recorded file is never rewritten during a normal run; a test that
 * wants to record a new transcript must say so with {@code -Dpm.transcript.update=true}, and the
 * changed file then shows up in the review like any other change.
 */
public final class TranscriptAssert {

    /** The system property that switches a run from comparing to recording. */
    public static final String UPDATE_PROPERTY = "pm.transcript.update";

    private static final String EXTENSION = ".transcript";

    private TranscriptAssert() {
    }

    /**
     * @param name     the transcript name below {@code src/test/resources/transcript}
     * @param rendered the rendered terminal buffer
     */
    public static void assertMatches(String name, String rendered) {
        assertMatches(name, rendered, new TranscriptNormalizer());
    }

    /**
     * @param name       the transcript name below {@code src/test/resources/transcript}
     * @param rendered   the rendered terminal buffer
     * @param normalizer the normalizer for the transient parts of the buffer
     */
    public static void assertMatches(String name, String rendered, TranscriptNormalizer normalizer) {
        var actual = normalizer.normalize(rendered);
        if (!actual.endsWith("\n")) {
            actual = actual + "\n";
        }
        var fixture = fixture(name);
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            write(fixture, actual);
            return;
        }
        if (!Files.exists(fixture)) {
            throw new AssertionError("no transcript recorded at " + fixture.toAbsolutePath()
                                + "; record it with -D" + UPDATE_PROPERTY + "=true and review the file");
        }
        var expected = read(fixture);
        if (!expected.equals(actual)) {
            var diff = UnifiedDiff.between(fixture.getFileName().toString(),
                                TranscriptNormalizer.lines(expected),
                                "rendered", TranscriptNormalizer.lines(actual));
            throw new AssertionError("the rendered terminal no longer matches " + fixture.toAbsolutePath()
                                + "; review the change and record it with -D" + UPDATE_PROPERTY + "=true:\n" + diff);
        }
    }

    /**
     * @param name the transcript name below {@code src/test/resources/transcript}
     * @return the recorded file
     */
    static Path fixture(String name) {
        var moduleRelative = Path.of("src", "test", "resources", "transcript", name + EXTENSION);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        var rootRelative = Path.of("pm-operator").resolve(moduleRelative);
        return Files.exists(rootRelative) ? rootRelative : moduleRelative;
    }

    private static String read(Path fixture) {
        try {
            return Files.readString(fixture, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the transcript " + fixture, e);
        }
    }

    private static void write(Path fixture, String transcript) {
        try {
            Files.createDirectories(fixture.toAbsolutePath().getParent());
            Files.writeString(fixture, transcript, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot record the transcript " + fixture, e);
        }
    }
}
