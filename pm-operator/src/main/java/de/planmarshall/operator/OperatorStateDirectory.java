/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.planmarshall.api.runtime.RuntimeTokenFile;

/**
 * Manages the operator state directory in {@code $XDG_STATE_HOME/pm-operator/}
 * (default {@code ~/.local/state/pm-operator/}, directory {@code 0700}, files {@code 0600}):
 * the console's history of typed commands and where the operator left its views.
 * <p>
 * It is the only directory a CLI binary writes; it holds no answer, secret, or state of the runtime,
 * and the runtime never reads it (PM-SEC-4).
 */
public final class OperatorStateDirectory {

    private final Path root;

    /**
     * @param root the state directory root
     */
    public OperatorStateDirectory(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    /**
     * Resolves the operator state directory from the process environment and user home directory.
     *
     * @param environment the process environment
     * @param userHome    the user's home directory
     * @return the resolved state directory
     */
    public static OperatorStateDirectory resolve(Map<String, String> environment, Path userHome) {
        var xdgState = environment.get("XDG_STATE_HOME");
        Path dir;
        if (xdgState != null && !xdgState.isBlank()) {
            dir = Path.of(xdgState).resolve("pm-operator");
        } else {
            dir = userHome.resolve(".local/state/pm-operator");
        }
        return new OperatorStateDirectory(dir);
    }

    /**
     * @return the state directory root
     */
    public Path root() {
        return root;
    }

    /**
     * @return the history file {@code <root>/history}
     */
    public Path historyFile() {
        return root.resolve("history");
    }

    /**
     * Ensures the directory exists with mode {@code 0700} and no symbolic links.
     *
     * @return the verified root path
     * @throws IOException on I/O failure or security verification failure
     */
    public Path ensureDirectory() throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            var parent = root.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try {
                Files.createDirectory(root, PosixFilePermissions.asFileAttribute(RuntimeTokenFile.DIRECTORY_MODE));
                setDirectoryPermissions(root);
            } catch (FileAlreadyExistsException e) {
                // created concurrently
            }
        }
        verifyDirectory(root);
        return root;
    }

    /**
     * Ensures the file exists with mode {@code 0600} within the state directory.
     *
     * @param file the file path
     * @return the verified file path
     * @throws IOException on I/O failure or security verification failure
     */
    public Path ensureFile(Path file) throws IOException {
        ensureDirectory();
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createFile(file, PosixFilePermissions.asFileAttribute(RuntimeTokenFile.FILE_MODE));
                setFilePermissions(file);
            } catch (FileAlreadyExistsException e) {
                // created concurrently
            }
        }
        verifyFile(file);
        return file;
    }

    /**
     * Verifies that the path is an existing directory, not a symbolic link, and has mode {@code 0700}.
     *
     * @param path the directory path
     * @throws IOException if verification fails
     */
    public static void verifyDirectory(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new InsecureOperatorFileException(path, "is a symbolic link");
        }
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new InsecureOperatorFileException(path, "is not a directory");
        }
        var posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posixView != null) {
            var perms = posixView.readAttributes().permissions();
            if (!perms.equals(RuntimeTokenFile.DIRECTORY_MODE)) {
                throw new InsecureOperatorFileException(path,
                                    "has mode " + octal(perms) + ", expected 0700");
            }
        }
    }

    /**
     * Verifies that a file is a regular file, not a symbolic link, and has mode {@code 0600}.
     *
     * @param path the file path
     * @throws IOException if verification fails
     */
    public static void verifyFile(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new InsecureOperatorFileException(path, "is a symbolic link");
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new InsecureOperatorFileException(path, "is not a regular file");
        }
        var posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posixView != null) {
            var perms = posixView.readAttributes().permissions();
            if (!perms.equals(RuntimeTokenFile.FILE_MODE)) {
                throw new InsecureOperatorFileException(path,
                                    "has mode " + octal(perms) + ", expected 0600");
            }
        }
    }

    static String octal(Set<PosixFilePermission> permissions) {
        var mode = 0;
        for (var permission : permissions) {
            mode |= 1 << 8 - permission.ordinal();
        }
        return String.format("%04o", mode);
    }

    /**
     * Sets directory permissions to {@code 0700} if POSIX is supported.
     *
     * @param path the directory path
     * @throws IOException on I/O failure
     */
    public static void setDirectoryPermissions(Path path) throws IOException {
        var posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posixView != null) {
            Files.setPosixFilePermissions(path, RuntimeTokenFile.DIRECTORY_MODE);
        }
    }

    /**
     * Sets file permissions to {@code 0600} if POSIX is supported.
     *
     * @param path the file path
     * @throws IOException on I/O failure
     */
    public static void setFilePermissions(Path path) throws IOException {
        var posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posixView != null) {
            Files.setPosixFilePermissions(path, RuntimeTokenFile.FILE_MODE);
        }
    }
}
