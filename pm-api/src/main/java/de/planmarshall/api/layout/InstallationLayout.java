/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.layout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The installation layout seen from a client binary (PM-ARCH-1, PM-ARCH-7, PM-DIST-1): the
 * versioned installation directory that is the running binary's {@code <PM_MCP_HOME>}, the sibling
 * binaries beside it in {@code bin/}, and whether the installation is runtime-upgradable.
 * <p>
 * {@code <PM_MCP_HOME>} is the environment variable of the same name when it is set; otherwise it
 * is the parent of the {@code bin/} directory that contains the running executable, with every
 * symbolic link resolved, so a binary reached through one of the two {@code PATH} links - or
 * through a chain of links - finds its own installation. A release is unpacked into a directory
 * named by its version, {@code <install_root>/<version>/}, which is its {@code <PM_MCP_HOME>}; the
 * installation root {@code <install_root>} holds the versioned installation directories side by
 * side. One upgrade switches both {@code PATH} links to the new version directory and the next
 * command resolves the new {@code <PM_MCP_HOME>} through the switched link.
 * <p>
 * The four binaries of a release - {@code pm-mcp}, {@code pm-operator}, {@code pm-mcpd}, and
 * {@code pm-exec} - lie side by side in {@code bin/} and start one another by absolute path in
 * that directory, never through {@code PATH}, so a same-named executable earlier on {@code PATH}
 * is never started (see {@link #locateSibling(String)}).
 * <p>
 * An installation is <em>runtime-upgradable</em> when it was resolved from the running binary
 * (not named by {@code PM_MCP_HOME}), its directory is named by a version, and its installation
 * root is a user-writable directory (see {@link #runtimeUpgradable()}). A layout that cannot be
 * resolved - an unknown executable, an executable outside a {@code bin/} directory, a dangling
 * link - and an installation directory, its {@code bin/} directory, or its root writable by group
 * or others are refused with an {@link InstallationLayoutException} (PM-SEC-4).
 */
public final class InstallationLayout {

    /** Environment variable naming the installation directory. */
    public static final String ENV_HOME = "PM_MCP_HOME";

    /** Name of the directory that holds the binaries of one installation. */
    public static final String BIN_DIRECTORY = "bin";

    /** File name of the harness- and model-facing client binary. */
    public static final String CLIENT_BINARY = "pm-mcp";

    /** File name of the operator binary. */
    public static final String OPERATOR_BINARY = "pm-operator";

    /** File name of the always-on runtime binary. */
    public static final String DAEMON_BINARY = "pm-mcpd";

    /** File name of the job launcher binary. */
    public static final String LAUNCHER_BINARY = "pm-exec";

    /** The four binaries of a release; a running executable is one of them (PM-ARCH-1). */
    private static final Set<String> RELEASE_BINARIES = Set.of(CLIENT_BINARY, OPERATOR_BINARY, DAEMON_BINARY,
            LAUNCHER_BINARY);

    /** Prefix of the runtime-upgradability refusals that name the installation root. */
    private static final String INSTALLATION_ROOT = "the installation root ";

    private static final Set<PosixFilePermission> BROADER_THAN_OWNER = Set.of(PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.OTHERS_WRITE);

    private final Path home;
    private final Path installRoot;
    private final String version;
    private final boolean resolvedFromBinary;

    private InstallationLayout(Path home, Path installRoot, String version, boolean resolvedFromBinary) {
        this.home = home;
        this.installRoot = installRoot;
        this.version = version;
        this.resolvedFromBinary = resolvedFromBinary;
    }

    /**
     * Resolves the layout of the running installation.
     *
     * @param environment the process environment
     * @param executable  the running executable, if known
     * @return the layout
     * @throws InstallationLayoutException if neither the environment nor the executable names a
     *                                     resolvable installation, or the installation or its
     *                                     {@code bin/} directory is writable by group or others
     * @throws IOException                 if a symbolic link on the way cannot be read
     */
    public static InstallationLayout resolve(Map<String, String> environment, Optional<Path> executable)
            throws IOException {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(executable, "executable");
        var configured = environment.get(ENV_HOME);
        if (configured != null && !configured.isBlank()) {
            var named = Path.of(configured).toAbsolutePath().normalize();
            return layout(named, named.getParent(), false);
        }
        var binary = executable.orElseThrow(() -> new InstallationLayoutException(
                "Cannot resolve the installation: the running executable is unknown and " + ENV_HOME + " is not set"));
        var real = realPath(binary);
        var name = real.getFileName() == null ? null : real.getFileName().toString();
        if (name == null || !RELEASE_BINARIES.contains(name)) {
            throw new InstallationLayoutException("The running executable " + binary
                    + " is not one of the binaries of an installation (" + CLIENT_BINARY + ", " + OPERATOR_BINARY
                    + ", " + DAEMON_BINARY + ", " + LAUNCHER_BINARY + ")");
        }
        var bin = real.getParent();
        if (bin == null || !BIN_DIRECTORY.equals(bin.getFileName() == null ? null : bin.getFileName().toString())) {
            throw new InstallationLayoutException("The running executable " + binary + " does not lie in a "
                    + BIN_DIRECTORY + "/ directory of an installation");
        }
        var home = bin.getParent();
        if (home == null) {
            throw new InstallationLayoutException(
                    "The running executable " + binary + " lies too high in the filesystem to name an installation");
        }
        var installRoot = home.getParent();
        if (writableByOthers(bin) || writableByOthers(home) || writableByOthers(installRoot)) {
            throw new InstallationLayoutException("Refusing the installation " + home
                    + ": its " + BIN_DIRECTORY + "/ directory, the installation directory, or its root is writable"
                    + " by group or others");
        }
        return layout(home, installRoot, true);
    }

    /**
     * Resolves the layout of the running process.
     *
     * @param environment the process environment
     * @return the layout
     * @throws IOException if it cannot be resolved
     */
    public static InstallationLayout current(Map<String, String> environment) throws IOException {
        return resolve(environment, ProcessHandle.current().info().command().map(Path::of));
    }

    /**
     * Resolves the installation directory of an executable.
     *
     * @param environment the process environment
     * @param executable  the running executable, if known
     * @return the installation directory {@code <PM_MCP_HOME>}, absolute and normalized
     * @throws IOException if it cannot be resolved
     */
    public static Path resolveHome(Map<String, String> environment, Optional<Path> executable) throws IOException {
        return resolve(environment, executable).home();
    }

    /**
     * Extracts the version token of the installation directory an executable lies in; the token is
     * the name of that directory when it is a version (see the class documentation).
     *
     * @param executable the running executable
     * @return the version token, or empty when the executable names no versioned installation
     * @throws IOException if a symbolic link on the way cannot be read
     */
    public static Optional<String> resolveVersion(Path executable) throws IOException {
        Objects.requireNonNull(executable, "executable");
        var bin = realPath(executable).getParent();
        if (bin == null || !BIN_DIRECTORY.equals(bin.getFileName() == null ? null : bin.getFileName().toString())
                || bin.getParent() == null) {
            return Optional.empty();
        }
        return versionOf(bin.getParent());
    }

    private static InstallationLayout layout(Path home, Path installRoot, boolean resolvedFromBinary)
            throws InstallationLayoutException {
        if (home.getFileName() == null) {
            throw new InstallationLayoutException(
                    "Refusing the installation " + home + ": the filesystem root cannot name an installation");
        }
        return new InstallationLayout(home, installRoot, versionOf(home).orElse(null), resolvedFromBinary);
    }

    private static Path realPath(Path executable) throws InstallationLayoutException {
        try {
            return executable.toRealPath();
        } catch (IOException e) {
            throw new InstallationLayoutException("Cannot resolve the running executable " + executable, e);
        }
    }

    private static Optional<String> versionOf(Path directory) {
        if (directory == null || directory.getFileName() == null) {
            return Optional.empty();
        }
        var name = directory.getFileName().toString();
        return isVersion(name) ? Optional.of(name) : Optional.empty();
    }

    /**
     * A version is one or more dot-separated non-empty digit segments, optionally followed by a
     * {@code -} or {@code +} suffix of letters, digits, dots, hyphens, and plus signs, as in
     * {@code 1.2.3} or {@code 0.1.0-SNAPSHOT}. Parsed in one linear pass rather than by a nested
     * regular expression, which can backtrack catastrophically on a long menu of dots.
     */
    private static boolean isVersion(String name) {
        var index = numericPrefixEnd(name);
        if (index < 0) {
            return false;
        }
        if (index == name.length()) {
            return true;
        }
        var separator = name.charAt(index);
        return (separator == '-' || separator == '+') && isVersionSuffix(name.substring(index + 1));
    }

    /**
     * @param name the directory name
     * @return the index after one or more dot-separated non-empty digit segments, or {@code -1}
     *         when the name does not start with one
     */
    private static int numericPrefixEnd(String name) {
        var index = 0;
        while (index < name.length()) {
            var start = index;
            while (index < name.length() && Character.isDigit(name.charAt(index))) {
                index++;
            }
            if (index == start) {
                return -1;
            }
            if (index == name.length()) {
                return index;
            }
            if (name.charAt(index) != '.') {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * @param suffix the text after the version separator
     * @return {@code true} for a non-empty run of letters, digits, dots, hyphens, and plus signs
     */
    private static boolean isVersionSuffix(String suffix) {
        if (suffix.isEmpty()) {
            return false;
        }
        for (var index = 0; index < suffix.length(); index++) {
            var character = suffix.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '.' && character != '-'
                    && character != '+') {
                return false;
            }
        }
        return true;
    }

    private static boolean writableByOthers(Path directory) {
        if (directory == null) {
            return false;
        }
        try {
            return !Collections.disjoint(Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS),
                    BROADER_THAN_OWNER);
        } catch (IOException | UnsupportedOperationException _) {
            return false;
        }
    }

    /**
     * @return the installation directory {@code <PM_MCP_HOME>}, absolute and normalized
     */
    public Path home() {
        return home;
    }

    /**
     * @return the installation root that holds the versioned installation directories side by
     *         side, or empty when the installation directory has no parent
     */
    public Optional<Path> installRoot() {
        return Optional.ofNullable(installRoot);
    }

    /**
     * @return the version token the installation directory is named by, or empty for an
     *         installation that is not laid out in versioned directories
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    /**
     * @return {@code true} if the installation was resolved from the running binary rather than
     *         named by {@link #ENV_HOME}
     */
    public boolean resolvedFromBinary() {
        return resolvedFromBinary;
    }

    /**
     * @param name a binary name, such as {@link #DAEMON_BINARY}
     * @return {@code <PM_MCP_HOME>/bin/<name>}, the absolute path a binary starts its sibling by
     */
    public Path binary(String name) {
        return home.resolve(BIN_DIRECTORY).resolve(Objects.requireNonNull(name, "name"));
    }

    /**
     * Locates a sibling binary of the running one by its absolute path in the same version
     * directory, regardless of the symbolic link the running binary was invoked through.
     *
     * @param name the sibling binary name, such as {@link #DAEMON_BINARY}
     * @return {@code <PM_MCP_HOME>/bin/<name>}
     */
    public Path locateSibling(String name) {
        return binary(name);
    }

    /**
     * @return {@code true} if the installation may be upgraded by the runtime: it was resolved
     *         from the running binary, its directory is named by a version, and its installation
     *         root is a user-writable directory
     */
    public boolean runtimeUpgradable() {
        return upgradabilityRefusal().isEmpty();
    }

    /**
     * @return the reason the installation is not runtime-upgradable, or empty when it is
     */
    public Optional<String> upgradabilityRefusal() {
        if (!resolvedFromBinary) {
            return Optional.of("the installation was named by " + ENV_HOME + ", not resolved from the running binary");
        }
        if (version == null) {
            return Optional.of("the installation directory " + home + " is not named by a version");
        }
        if (installRoot == null) {
            return Optional.of("the installation directory " + home + " has no installation root");
        }
        if (Files.isSymbolicLink(installRoot)) {
            return Optional.of(INSTALLATION_ROOT + installRoot + " is a symbolic link");
        }
        if (!Files.isDirectory(installRoot, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.of(INSTALLATION_ROOT + installRoot + " is not a directory");
        }
        if (!Files.isWritable(installRoot)) {
            return Optional.of(INSTALLATION_ROOT + installRoot + " is not writable by the current user");
        }
        if (writableByOthers(installRoot)) {
            return Optional.of(INSTALLATION_ROOT + installRoot + " is writable by group or others");
        }
        return Optional.empty();
    }
}
