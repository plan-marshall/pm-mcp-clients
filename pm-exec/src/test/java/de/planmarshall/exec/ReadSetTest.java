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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("ReadSet: everything except the read-denied paths")
@EnableGeneratorController
class ReadSetTest {

    @TempDir
    Path temp;

    private Path root;
    private Path base;
    private Path bus;
    private Path home;
    private Path alice;
    private Path bob;
    private Path carol;
    private Path runtime;
    private String carolName;

    /**
     * root/{bin/, etc/, home/{bob/, alice/{.bashrc, projects/, .plan-marshall-mcp/run/runtime.token,
     * to-base -> .plan-marshall-mcp, to-home -> root/home, to-file -> .bashrc, dangling -> missing}}},
     * run/user/{n}/{bus, docker.sock, systemd/}}.
     */
    @BeforeEach
    void tree() throws IOException {
        root = temp.toRealPath();
        Files.createDirectories(root.resolve("bin"));
        Files.createDirectories(root.resolve("etc"));
        home = Files.createDirectories(root.resolve("home"));
        bob = Files.createDirectories(home.resolve("b" + Generators.letterStrings(3, 8).next()));
        alice = Files.createDirectories(home.resolve("a" + Generators.letterStrings(3, 8).next()));
        carolName = "c" + Generators.letterStrings(3, 8).next();
        carol = home.resolve(carolName);
        Files.writeString(alice.resolve(".bashrc"), "x");
        Files.createDirectories(alice.resolve("projects"));
        base = Files.createDirectories(alice.resolve(".plan-marshall-mcp"));
        Files.createDirectories(base.resolve("run"));
        Files.writeString(base.resolve("run/runtime.token"), "secret");
        Files.createSymbolicLink(alice.resolve("to-base"), Path.of(".plan-marshall-mcp"));
        Files.createSymbolicLink(alice.resolve("to-home"), home);
        Files.createSymbolicLink(alice.resolve("to-file"), Path.of(".bashrc"));
        Files.createSymbolicLink(alice.resolve("dangling"), Path.of("missing"));
        runtime = Files.createDirectories(root.resolve("run/user/" + Generators.letterStrings(3, 8).next()));
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
                    bob,
                    alice.resolve(".bashrc"),
                    alice.resolve("projects"),
                    alice.resolve("to-file")), readSet);
        }

        @Test
        @DisplayName("two disjoint denied paths: both holes carved out, their siblings kept")
        void disjoint() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(base, bus));

            assertEquals(List.of(
                    root.resolve("bin"),
                    root.resolve("etc"),
                    bob,
                    alice.resolve(".bashrc"),
                    alice.resolve("projects"),
                    alice.resolve("to-file"),
                    runtime.resolve("docker.sock"),
                    runtime.resolve("systemd")), readSet);
        }

        @Test
        @DisplayName("only the second denied path: its siblings stay readable, the far subtree unconstrained")
        void secondOnly() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(bus));

            assertEquals(List.of(
                    root.resolve("bin"),
                    root.resolve("etc"),
                    home,
                    runtime.resolve("docker.sock"),
                    runtime.resolve("systemd")), readSet);
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
            var readSet = ReadSet.enumerate(root, List.of(alice.resolve("to-base")));

            assertEquals(ReadSet.enumerate(root, List.of(base)), readSet);
        }

        @Test
        @DisplayName("a duplicate denied path is carved out once")
        void duplicate() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(base, base));

            assertEquals(ReadSet.enumerate(root, List.of(base)), readSet);
        }

        @Test
        @DisplayName("a hard link to a denied path in an enumerated directory gets no rule")
        void hardLink() throws Exception {
            var alias = Files.createLink(alice.resolve("bus-alias"), bus);

            var readSet = ReadSet.enumerate(root, List.of(base, bus));

            assertFalse(readSet.contains(alias));
            assertEquals(List.of(
                    root.resolve("bin"),
                    root.resolve("etc"),
                    bob,
                    alice.resolve(".bashrc"),
                    alice.resolve("projects"),
                    alice.resolve("to-file"),
                    runtime.resolve("docker.sock"),
                    runtime.resolve("systemd")), readSet);
        }

        @Test
        @DisplayName("stops at a missing directory on the path (fail closed)")
        void missingBase() throws Exception {
            var readSet = ReadSet.enumerate(root, List.of(carol.resolve(".plan-marshall-mcp")));

            assertEquals(List.of(root.resolve("bin"), root.resolve("etc"), root.resolve("run"),
                    alice, bob), readSet);
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
            var path = alice.resolve("to-home/" + carolName + "/.plan-marshall-mcp");

            assertEquals(carol.resolve(".plan-marshall-mcp"), ReadSet.canonical(path));
        }

        @Test
        @DisplayName("resolves an existing path to its real path")
        void existing() {
            assertEquals(base, ReadSet.canonical(alice.resolve("to-base")));
        }

        @Test
        @DisplayName("keeps a path without any existing prefix")
        void nothingExists() {
            var relative = Path.of("nowhere/at/all");

            assertEquals(relative, ReadSet.canonical(relative));
        }
    }

    @Nested
    @DisplayName("reopensDenied")
    class ReopensDenied {

        @Test
        @DisplayName("withholds a rule on a denied socket")
        void socket() {
            assertTrue(ReadSet.reopensDenied(bus, List.of(base, bus)));
        }

        @Test
        @DisplayName("withholds a rule above a denied socket")
        void above() {
            assertTrue(ReadSet.reopensDenied(root.resolve("run"), List.of(base, bus)));
        }

        @Test
        @DisplayName("withholds a hard link to a denied socket")
        void hardLink() throws Exception {
            var alias = Files.createLink(alice.resolve("bus-alias"), bus);

            assertTrue(ReadSet.reopensDenied(alias, List.of(base, bus)));
        }

        @Test
        @DisplayName("keeps a rule inside the base, the documented exception")
        void insideBase() {
            assertFalse(ReadSet.reopensDenied(base.resolve("run"), List.of(base, bus)));
        }

        @Test
        @DisplayName("keeps a rule on the base itself")
        void baseItself() {
            assertFalse(ReadSet.reopensDenied(base, List.of(base, bus)));
        }

        @Test
        @DisplayName("withholds nothing when the base is the only denied path")
        void singleDeniedPath() {
            assertFalse(ReadSet.reopensDenied(bus, List.of(base)));
        }
    }
}
