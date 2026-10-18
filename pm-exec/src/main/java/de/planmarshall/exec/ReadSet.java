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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Builds the Landlock read set: the whole filesystem except the read-denied paths ({@code <PM_MCP_BASE>}
 * and, without the container-engine opt-in, the session-bus socket and the container engine's sockets).
 * <p>
 * Landlock rules only grant access, so a rule on {@code /} would also grant the denied paths. The read set
 * is therefore one rule per entry of every directory that contains a denied path, leaving out the next
 * directory on the way down and the denied path itself
 * (pm-mcp-core/doc/specification/job-runtime/02-confinement-and-environment.adoc, Read set). Every directory
 * that contains or lies on the way to a denied path receives no rule, so a job can traverse it but not list
 * it; an entry whose real path (symbolic links resolved) lies inside a denied path or contains one is left
 * out as well, since the kernel attaches a rule to the inode the link points to. Entries created after
 * enumeration are not readable.
 */
@UtilityClass
class ReadSet {

    /**
     * Enumerates the read set around every denied path, breadth-first from the root.
     *
     * @param root        the filesystem root, {@code /} in production, a temporary directory in tests; real path
     * @param deniedPaths the denied paths; canonicalized here and each strictly below the root
     * @return the paths that get a read rule, in enumeration order
     * @throws UsageException if a denied path is not strictly below the root
     */
    static List<Path> enumerate(Path root, List<Path> deniedPaths) throws UsageException {
        var denied = new ArrayList<Path>();
        for (var path : deniedPaths) {
            var canonical = canonical(path);
            if (!canonical.startsWith(root) || canonical.equals(root)) {
                throw new UsageException("denied path " + canonical + " is not below " + root);
            }
            if (!denied.contains(canonical)) {
                denied.add(canonical);
            }
        }
        var result = new ArrayList<Path>();
        var directories = new ArrayDeque<Path>();
        directories.add(root);
        while (!directories.isEmpty()) {
            var directory = directories.removeFirst();
            for (var entry : list(directory)) {
                if (denied.contains(entry)) {
                    // The denied path itself: no rule, and its whole subtree stays out.
                    continue;
                }
                if (isAncestorOfAny(entry, denied)) {
                    // On the way to a denied path: descend, but grant nothing on the directory itself.
                    directories.addLast(entry);
                } else if (!overlaps(entry, denied)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * Canonicalizes a path whose tail may not exist yet: the longest existing prefix is resolved to
     * its real path and the rest appended.
     *
     * @param path an absolute, normalized path
     * @return the canonical path
     */
    static Path canonical(Path path) {
        var existing = path;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return path;
        }
        try {
            return existing.toRealPath().resolve(existing.relativize(path));
        } catch (IOException _) {
            return path;
        }
    }

    private static List<Path> list(Path directory) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.sorted().toList();
        } catch (IOException _) {
            // Missing or unlistable directory: none of its entries becomes readable (fail closed).
            return List.of();
        }
    }

    private static boolean isAncestorOfAny(Path entry, List<Path> denied) {
        return denied.stream().anyMatch(deniedPath -> deniedPath.startsWith(entry));
    }

    private static boolean overlaps(Path entry, List<Path> denied) {
        Path real;
        try {
            real = entry.toRealPath();
        } catch (IOException _) {
            // Dangling or unreachable: the kernel could not open it either.
            return true;
        }
        return denied.stream().anyMatch(deniedPath -> real.startsWith(deniedPath) || deniedPath.startsWith(real));
    }
}
