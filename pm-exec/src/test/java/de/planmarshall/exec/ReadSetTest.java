/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ReadSet: everything except PM_MCP_BASE")
class ReadSetTest {

    @TempDir
    Path temp;

    private Path root;
    private Path base;
    private Path bus;

    /**
     * root/{bin/, etc/, home/{bob/, alice/{.bashrc, projects/, .plan-marshall-mcp/run/runtime.token,
     * to-base -> .plan-marshall-mcp, to-home -> root/home, to-file -> .bashrc, dangling -> missing}}},
     * run/user/1000/{bus, docker.sock, systemd/}}.
     */
    @BeforeEach
    void tree() throws IOException {
        root = temp.toRealPath();
        Files.createDirectories(root.resolve("bin"));
        Files.createDirectories(root.resolve("etc"));
        Files.createDirectories(root.resolve("home/bob"));
        var alice = Files.createDirectories(root.resolve("home/alice"));
        Files.writeString(alice.resolve(".bashrc"), "x");
        Files.createDirectories(alice.resolve("projects"));
        base = Files.createDirectories(alice.resolve(".plan-marshall-mcp"));
        Files.createDirectories(base.resolve("run"));
        Files.writeString(base.resolve("run/runtime.token"), "secret");
        Files.createSymbolicLink(alice.resolve("to-base"), Path.of(".plan-marshall-mcp"));
        Files.createSymbolicLink(alice.resolve("to-home"), root.resolve("home"));
        Files.createSymbolicLink(alice.resolve("to-file"), Path.of(".bashrc"));
        Files.createSymbolicLink(alice.resolve("dangling"), Path.of("missing"));
        var runtime = Files.createDirectories(root.resolve("run/user/1000"));
        bus = Files.writeString(runtime.resolve("bus"), "socket");
        Files.writeString(runtime.resolve("docker.sock"), "socket");
        Files.createDirectories(runtime.resolve("systemd"));
    }

    @Nested
    @DisplayName("enumerate")
    class Enumerate {

        @Test
        @DisplayName("one denied path: every sibling on the path, never the base or a link into or above it")
        void siblings() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(base));

            assertEquals(List.of(
                    root.resolve("bin"),
                    root.resolve("etc"),
                    root.resolve("run"),
                    root.resolve("home/bob"),
                    root.resolve("home/alice/.bashrc"),
                    root.resolve("home/alice/projects"),
                    root.resolve("home/alice/to-file")), readSet);
        }

        @Test
        @DisplayName("two disjoint denied paths: both holes carved out, their siblings kept")
        void disjoint() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(base, bus));

            assertEquals(List.of(
                    root.resolve("bin"),
                    root.resolve("etc"),
                    root.resolve("home/bob"),
                    root.resolve("home/alice/.bashrc"),
                    root.resolve("home/alice/projects"),
                    root.resolve("home/alice/to-file"),
                    root.resolve("run/user/1000/docker.sock"),
                    root.resolve("run/user/1000/systemd")), readSet);
        }

        @Test
        @DisplayName("only the second denied path: its siblings stay readable, the far subtree unconstrained")
        void secondOnly() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(bus));

            assertEquals(List.of(
                    root.resolve("bin"),
                    root.resolve("etc"),
                    root.resolve("home"),
                    root.resolve("run/user/1000/docker.sock"),
                    root.resolve("run/user/1000/systemd")), readSet);
        }

        @Test
        @DisplayName("a nested denied path adds no hole to one already covered by its ancestor")
        void nested() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(base, base.resolve("run")));

            assertEquals(ReadSet.enumerate(root, List.of(base)), readSet);
        }

        @Test
        @DisplayName("canonicalizes a denied path given as a symbolic link")
        void canonicalized() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(root.resolve("home/alice/to-base")));

            assertEquals(ReadSet.enumerate(root, List.of(base)), readSet);
        }

        @Test
        @DisplayName("a duplicate denied path is carved out once")
        void duplicate() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(base, base));

            assertEquals(ReadSet.enumerate(root, List.of(base)), readSet);
        }

        @Test
        @DisplayName("stops at a missing directory on the path (fail closed)")
        void missingBase() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(root.resolve("home/carol/.plan-marshall-mcp")));

            assertEquals(List.of(root.resolve("bin"), root.resolve("etc"), root.resolve("run"),
                    root.resolve("home/alice"), root.resolve("home/bob")), readSet);
        }

        @Test
        @DisplayName("refuses the root itself as a denied path")
        void baseIsRoot() {
            assertThrows(UsageException.class, () -> ReadSet.enumerate(root, List.of(root)));
        }

        @Test
        @DisplayName("refuses a denied path outside the root, also beside a valid one")
        void baseOutsideRoot() {
            assertThrows(UsageException.class, () -> ReadSet.enumerate(root.resolve("bin"), List.of(base)));
            assertThrows(UsageException.class,
                    () -> ReadSet.enumerate(root.resolve("bin"), List.of(root.resolve("bin/usr"), base)));
        }
    }

    @Nested
    @DisplayName("canonical")
    class Canonical {

        @Test
        @DisplayName("resolves links of the existing prefix and keeps a missing tail")
        void missingTail() {
            var path = root.resolve("home/alice/to-home/carol/.plan-marshall-mcp");

            assertEquals(root.resolve("home/carol/.plan-marshall-mcp"), ReadSet.canonical(path));
        }

        @Test
        @DisplayName("resolves an existing path to its real path")
        void existing() {
            assertEquals(base, ReadSet.canonical(root.resolve("home/alice/to-base")));
        }

        @Test
        @DisplayName("keeps a path without any existing prefix")
        void nothingExists() {
            var relative = Path.of("nowhere/at/all");

            assertEquals(relative, ReadSet.canonical(relative));
        }
    }
}
