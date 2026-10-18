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
        var denied = canonicalDenied(root, deniedPaths);
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
                } else if (!overlapsAny(entry, denied)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * Whether a rule on the given path would grant access to a read-denied path other than the first.
     * The runtime names the job-private directory, the runtime socket, the daemon store, and a worker's
     * data directory with explicit {@code --read}/{@code --write} rules, and all of them lie inside
     * {@code <PM_MCP_BASE>}, the first denied path; no rule may reopen the session-bus socket, a
     * container-engine socket, or a path inside one. The check runs again when the rule is created, so a
     * path swapped in after enumeration is caught before the rule is added.
     *
     * @param path        the path a rule would name
     * @param deniedPaths the read-denied paths, {@code <PM_MCP_BASE>} first
     * @return whether the rule must be withheld
     */
    static boolean reopensDenied(Path path, List<Path> deniedPaths) {
        var canonical = canonical(path);
        return deniedPaths.stream().skip(1).map(ReadSet::canonical)
                .anyMatch(denied -> overlaps(canonical, denied));
    }

    private static List<Path> canonicalDenied(Path root, List<Path> deniedPaths) throws UsageException {
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
        return denied;
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

    private static boolean overlapsAny(Path entry, List<Path> denied) {
        return denied.stream().anyMatch(deniedPath -> overlaps(entry, deniedPath));
    }

    /**
     * Whether the entry is a denied path, lies inside one, contains one, or is the same filesystem
     * object (a symbolic or hard link to it). Compared by resolved path and by inode, so a link to a
     * denied path from anywhere in an enumerated directory is left out.
     */
    private static boolean overlaps(Path entry, Path denied) {
        Path real;
        try {
            real = entry.toRealPath();
        } catch (IOException _) {
            // Dangling or unreachable: the kernel could not open it either.
            return true;
        }
        if (real.startsWith(denied) || denied.startsWith(real)) {
            return true;
        }
        return sameFile(entry, denied);
    }

    private static boolean sameFile(Path entry, Path denied) {
        try {
            return Files.exists(entry) && Files.exists(denied) && Files.isSameFile(entry, denied);
        } catch (IOException _) {
            return false;
        }
    }
}
