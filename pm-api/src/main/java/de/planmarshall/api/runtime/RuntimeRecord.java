/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Read-only codec of the runtime record {@code <PM_MCP_BASE>/state/runtime.json}, one of the files
 * the client binaries read directly. It serves only the "not running" answer: a record whose
 * process is gone, whose start instant it does not carry, or whose start instant differs (PID reuse)
 * names no live runtime.
 *
 * @param pid           the runtime's process id
 * @param startInstant  the process start instant, {@code null} when not recorded
 * @param version       the runtime version, {@code null} when not recorded
 */
public record RuntimeRecord(long pid, Instant startInstant, String version) {

    /**
     * Reads the record.
     *
     * @param file {@code state/runtime.json}
     * @return the record, empty if the file is absent or unreadable as a record
     * @throws IOException on a read failure other than absence
     */
    public static Optional<RuntimeRecord> read(Path file) throws IOException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
        try {
            var root = JsonTree.asObject(JsonTree.parse(bytes));
            if (root == null || !(root.get("pid") instanceof JsonNumber pid)) {
                return Optional.empty();
            }
            var start = JsonTree.string(root, "start_instant");
            return Optional.of(new RuntimeRecord(pid.longValue(), start == null ? null : Instant.parse(start),
                    JsonTree.string(root, "version")));
        } catch (IOException | NumberFormatException | DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * @return {@code true} if the record names a live process that it can identify by its start
     *         instant; a record with no start instant, or a process that reports none, names no live
     *         runtime
     */
    public boolean processAlive() {
        return ProcessHandle.of(pid).filter(ProcessHandle::isAlive).map(this::identifies).orElse(false);
    }

    /**
     * The record identifies a process only when the handle names the recorded process id and the
     * record carries a start instant the process reports equal to the second. A reused process id is
     * a different process: nothing else is proof enough to signal it.
     *
     * @param handle the process the record's process id names
     * @return {@code true} if the process is the one the record names
     */
    public boolean identifies(ProcessHandle handle) {
        return handle.pid() == pid && startInstant != null && handle.info().startInstant()
                .map(actual -> actual.truncatedTo(ChronoUnit.SECONDS)
                        .equals(startInstant.truncatedTo(ChronoUnit.SECONDS)))
                .orElse(false);
    }
}
