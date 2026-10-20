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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.operator.tui.TuiRow;

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
        return level.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
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
    @DisplayName("a live update replaces the stream and keeps the task id")
    void updates() throws Exception {
        var level = level();
        var fresh = TaskActivity.fromJson(ACTIVITY.replace("14:02:11", "14:09:00"));
        level.update(fresh);
        assertAll(
                            () -> assertTrue(text(level).contains("14:09:00")),
                            () -> assertTrue(level.breadcrumb().startsWith("fix-relay")),
                            () -> assertTrue(level.breadcrumb().endsWith("TASK-003")),
                            () -> assertEquals("fix-relay", level.planId()),
                            () -> assertEquals("execute.envelope", level.nodeId()),
                            () -> assertEquals("TASK-003", level.taskId())
        );
    }

    @Test
    @DisplayName("the gap and the server-only note are named, and the rich kinds render")
    void gapServerOnlyAndKinds() throws Exception {
        var json = """
                            {
                              "task_id": "TASK-9", "role": "audit", "harness": "opencode", "model": "deepseek",
                              "attempt": 2, "duration_seconds": 120, "tokens_so_far": 100,
                              "entries": [
                                {"timestamp": "12:00:00", "kind": "file_changed", "files": ["pom.xml", "README.md"]},
                                {"timestamp": "12:00:01", "kind": "link", "tool_target": "submit:TASK-9"},
                                {"timestamp": "12:00:02", "kind": "answer_ack", "content": "received"},
                                {"timestamp": "12:00:03", "kind": "tool_call", "tool_name": "pytest",
                                 "tool_target": "tests/"}
                              ],
                              "server_only": true, "gap_count": 3
                            }
                            """;
        var level = new WorkerActivityLevel("fix-relay", "execute", TaskActivity.fromJson(json));
        var text = text(level);
        assertAll(
                            () -> assertTrue(text.contains("3 older entries gave way"), text),
                            () -> assertTrue(text.contains("This harness reports no steps"), text),
                            () -> assertTrue(text.contains("pom.xml, README.md"), text),
                            () -> assertTrue(text.contains("submit:TASK-9"), text),
                            () -> assertTrue(text.contains("answer received"), text),
                            () -> assertTrue(text.contains("pytest tests/"), text)
        );
    }

    @Test
    @DisplayName("an empty stream says so, and the last row is the newest entry")
    void emptyAndLastRow() throws Exception {
        var empty = new WorkerActivityLevel("fix-relay", "execute",
                            TaskActivity.fromJson("{\"task_id\":\"T-1\",\"entries\":[]}"));
        assertTrue(text(empty).contains("No activity entries yet."));
        var level = level();
        assertTrue(level.lastRowId().startsWith("activity:entry:"));
    }
}
