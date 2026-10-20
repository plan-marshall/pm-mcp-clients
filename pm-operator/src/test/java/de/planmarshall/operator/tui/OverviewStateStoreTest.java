/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import de.planmarshall.operator.OperatorStateDirectory;
import de.planmarshall.operator.OverviewSavedState;
import de.planmarshall.operator.OverviewStateStore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("OverviewStateStore")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OverviewStateStoreTest {

    @TempDir
    Path tempDir;

    private OverviewStateStore store() {
        return new OverviewStateStore(new OperatorStateDirectory(tempDir.resolve("state")));
    }

    private OverviewStateStore emptyStrictStore() throws IOException {
        var store = store();
        Files.createDirectories(store.file().getParent());
        Files.setPosixFilePermissions(store.file().getParent(), PosixFilePermissions.fromString("rwx------"));
        return store;
    }

    @Test
    @DisplayName("round-trips every field")
    void roundTrip() throws Exception {
        var store = store();
        var state = new OverviewSavedState("pm-mcp-clients", "epic", "plan-7", "7d", "role",
                            "pm-mcp-core");
        store.write(state);
        var read = store.read();
        assertAll(
                            () -> assertTrue(read.isPresent()),
                            () -> assertEquals(state, read.orElseThrow())
        );
    }

    @Test
    @DisplayName("absent fields stay absent")
    void roundTripsPartialState() throws Exception {
        var store = store();
        store.write(new OverviewSavedState("p1", null, null, null, null, null));
        assertEquals(new OverviewSavedState("p1", null, null, null, null, null),
                            store.read().orElseThrow());
    }

    @Nested
    @DisplayName("fallbacks")
    class Fallbacks {

        @Test
        @DisplayName("a missing file is empty")
        void missingFile() {
            assertTrue(store().read().isEmpty());
        }

        @Test
        @DisplayName("a corrupt file is empty")
        void corruptFile() throws Exception {
            var store = emptyStrictStore();
            Files.writeString(store.file(), "{not json", StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(store.file(), PosixFilePermissions.fromString("rw-------"));
            assertTrue(store.read().isEmpty());
        }

        @Test
        @DisplayName("trailing content after the object is corrupt")
        void trailingContent() throws Exception {
            var store = emptyStrictStore();
            Files.writeString(store.file(), "{} {}", StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(store.file(), PosixFilePermissions.fromString("rw-------"));
            assertTrue(store.read().isEmpty());
        }

        @Test
        @DisplayName("an oversized file is not read")
        void oversizedFile() throws Exception {
            var store = emptyStrictStore();
            Files.write(store.file(), new byte[(int) OverviewStateStore.MAX_BYTES + 1]);
            Files.setPosixFilePermissions(store.file(), PosixFilePermissions.fromString("rw-------"));
            assertTrue(store.read().isEmpty());
        }
    }

    @Test
    @DisplayName("unknown fields and values outside a closed set are ignored")
    void ignoresUnknownAndInvalidValues() throws Exception {
        var store = emptyStrictStore();
        Files.writeString(store.file(), """
                            {"project_id":"p1","project_view":"sideways","numbers":[1,2,3],
                             "nested":{"a":1},"system_window":"hour","future":"field"}
                            """, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(store.file(), PosixFilePermissions.fromString("rw-------"));
        assertEquals(new OverviewSavedState("p1", null, null, "hour", null, null),
                            store.read().orElseThrow());
    }

    @Nested
    @DisplayName("security")
    class Security {

        @Test
        @DisplayName("writes the file with mode 0600 and the directory with 0700")
        void writesStrictPermissions() throws Exception {
            var store = store();
            store.write(OverviewSavedState.empty());
            assertAll(
                                () -> assertEquals(PosixFilePermissions.fromString("rw-------"),
                                                    Files.getPosixFilePermissions(store.file())),
                                () -> assertEquals(PosixFilePermissions.fromString("rwx------"),
                                                    Files.getPosixFilePermissions(store.file().getParent()))
            );
        }

        @Test
        @DisplayName("a symbolic link is not followed")
        void refusesSymlink() throws Exception {
            var store = emptyStrictStore();
            var target = tempDir.resolve("elsewhere.json");
            Files.writeString(target, "{\"project_id\":\"p1\"}", StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
            Files.createSymbolicLink(store.file(), target);
            assertTrue(store.read().isEmpty());
        }

        @Test
        @DisplayName("a file with group or other permissions is not trusted")
        void refusesLoosePermissions() throws Exception {
            var store = emptyStrictStore();
            Files.writeString(store.file(), "{\"project_id\":\"p1\"}", StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(store.file(), PosixFilePermissions.fromString("rw-r--r--"));
            assertTrue(store.read().isEmpty());
        }

        @Test
        @DisplayName("a write to a directory that is a symbolic link fails")
        void writeRefusesSymlinkDirectory() throws Exception {
            var real = tempDir.resolve("real");
            Files.createDirectories(real);
            var link = tempDir.resolve("linked-state");
            Files.createSymbolicLink(link, real);
            var store = new OverviewStateStore(new OperatorStateDirectory(link));
            assertThrows(IOException.class, () -> store.write(OverviewSavedState.empty()));
        }
    }
}
