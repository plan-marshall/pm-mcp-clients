/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import de.planmarshall.api.process.TaskActivity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkerActivityLevel")
class WorkerActivityLevelTest {

    private static final String ACTIVITY = """
                        {
                          "task_id": "TASK-003",
                          "role": "implement",
                          "harness": "claude",
                          "model": "sonnet",
                          "attempt": 1,
                          "duration_seconds": 360,
                          "tokens_so_far": 48000,
                          "entries": [
                            {"timestamp": "14:02:11", "kind": "text",
                             "content": "Reading the relay's transport before changing it."},
                            {"timestamp": "14:02:14", "kind": "tool_call", "tool_name": "read",
                             "tool_target": "pm-relay/src/main/java/Transport.java"},
                            {"timestamp": "14:02:20", "kind": "tool_call", "tool_name": "bash",
                             "tool_target": "./mvnw test", "tool_failed": true, "size": "4 KiB"},
                            {"timestamp": "14:03:41", "kind": "question",
                             "content": "Retain v1 token compatibility?"}
                          ],
                          "off_track": {"signal": "repeating", "action": "show", "value": 5, "threshold": 5}
                        }
                        """;

    private static WorkerActivityLevel level() throws Exception {
        return new WorkerActivityLevel("fix-relay", "execute.envelope", TaskActivity.fromJson(ACTIVITY));
    }

    private static String text(WorkerActivityLevel level) {
        return level.rows("en").stream().map(row -> row.line()).collect(Collectors.joining("\n"));
    }

    @Test
    @DisplayName("the head names role, harness, model, attempt, duration and tokens")
    void head() throws Exception {
        var text = text(level());
        assertAll(
                            () -> assertTrue(text.contains("implement \u00b7 claude sonnet \u00b7 attempt 1")),
                            () -> assertTrue(text.contains("6 min")),
                            () -> assertTrue(text.contains("48 k tokens"))
        );
    }

    @Test
    @DisplayName("the entries are plain text with their kind, target, failure and size")
    void entries() throws Exception {
        var text = text(level());
        assertAll(
                            () -> assertTrue(text.contains("wrote")),
                            () -> assertTrue(text.contains("Reading the relay's transport")),
                            () -> assertTrue(text.contains("read pm-relay/src/main/java/Transport.java")),
                            () -> assertTrue(text.contains("failed \u00b7 4 KiB")),
                            () -> assertTrue(text.contains("asked")),
                            () -> assertTrue(text.contains("Retain v1 token compatibility?"))
        );
    }

    @Test
    @DisplayName("an off-track task names the signal, its value and what the server did")
    void offTrack() throws Exception {
        var text = text(level());
        assertTrue(text.contains("off track"), text);
        assertTrue(text.contains("repeating"), text);
        assertTrue(text.contains("5 of 5"), text);
        assertTrue(text.contains("shown"), text);
    }

    @Test
    @DisplayName("a control sequence in any entry is stripped, never interpreted")
    void sanitizesControlSequences() throws Exception {
        var injected = ACTIVITY.replace("Reading the relay's transport",
                            "\\u001B[31mInjected\\u001B[0m Reading");
        var text = text(new WorkerActivityLevel("fix-relay", "execute.envelope", TaskActivity.fromJson(injected)));
        assertAll(
                            () -> assertFalse(text.contains("\u001B[31m"), "control sequence must be stripped"),
                            () -> assertFalse(text.contains("\u001B]"), "OSC sequence must be stripped"),
                            () -> assertTrue(text.contains("Injected Reading"))
        );
    }

    @Test
    @DisplayName("an empty stream says so, and the last row is the newest entry")
    void emptyAndLastRow() throws Exception {
        var empty = new WorkerActivityLevel("fix-relay", "execute",
                            TaskActivity.fromJson("{\"task_id\":\"T-1\",\"entries\":[]}"));
        assertTrue(text(empty).contains("No activity entries yet."));
        var level = level();
        assertTrue(level.lastRowId("en").startsWith("activity:entry:"));
    }
}
