/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.nio.file.Path;

/**
 * The confinement of the file picker's paths (State Catalogue, Documents as a Source; Runtime &amp;
 * Interaction Model, Local API).
 * <p>
 * A requested path is a path <em>relative</em> to one of the two roots, the project's checkout or the
 * operator's home directory. The guard is deterministic and total:
 * <ul>
 *   <li>a {@code null} path is the root itself;</li>
 *   <li>an absolute path, a Windows drive prefix, a NUL character, or a {@code ..} segment that
 *       would leave the root is refused;</li>
 *   <li>the normalized path is resolved against the root and must still start with it.</li>
 * </ul>
 * The caller resolves the result with {@code toRealPath} and re-checks it, so a symbolic link that
 * leads out of the root is refused as well. The guard never touches the filesystem itself.
 */
public final class ConfinedPath {

    /** The maximum length of a requested path. */
    public static final int MAX_PATH_LENGTH = 4096;

    private ConfinedPath() {
        // Utility class
    }

    /**
     * Resolves a requested path inside a root.
     *
     * @param root      the absolute, normalized root
     * @param requested the path relative to the root, or {@code null} for the root itself
     * @return the resolved absolute, normalized path inside the root
     * @throws IllegalArgumentException if the path is absolute, malformed, or leaves the root
     */
    public static Path resolve(Path root, String requested) {
        if (root == null || !root.isAbsolute()) {
            throw new IllegalArgumentException("The confined root must be an absolute path");
        }
        var normalizedRoot = root.normalize();
        if (requested == null || requested.isEmpty() || ".".equals(requested)) {
            return normalizedRoot;
        }
        if (requested.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("The path is longer than " + MAX_PATH_LENGTH + " characters");
        }
        if (requested.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("The path carries a NUL character");
        }
        if (requested.startsWith("/") || requested.startsWith("\\")) {
            throw new IllegalArgumentException("The path is relative to its root, never absolute: " + requested);
        }
        if (requested.length() > 1 && requested.charAt(1) == ':') {
            throw new IllegalArgumentException("The path names a drive, not a path in its root: " + requested);
        }
        var relative = Path.of(requested);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("The path is relative to its root, never absolute: " + requested);
        }
        var resolved = normalizedRoot.resolve(relative).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("The path leaves its root: " + requested);
        }
        return resolved;
    }

    /**
     * Checks that a resolved path, after its links were followed, still lies inside the root.
     *
     * @param root     the absolute, normalized root
     * @param resolved the real path to check
     * @return {@code true} if the real path lies inside the root
     */
    public static boolean contained(Path root, Path resolved) {
        return root != null && resolved != null
                && resolved.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize());
    }
}
