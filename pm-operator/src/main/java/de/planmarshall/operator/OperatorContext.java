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

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.PmVersion;
import de.planmarshall.api.runtime.RuntimeAccess;
import de.planmarshall.api.runtime.RuntimeClient;

/**
 * What an operator command takes from its process: the environment and the user's home directory,
 * whether the process is attached to an interactive terminal, the client environment, the security
 * gate, the operator state directory, and the runtime clients built from them. Tests pass their own.
 */
public final class OperatorContext {

    /** Supplies or builds a JLine terminal. */
    @FunctionalInterface
    public interface TerminalProvider {
        /**
         * @return the terminal instance
         * @throws IOException on terminal creation failure
         */
        Terminal createTerminal() throws IOException;
    }

    /** Opens an external browser window or tab. */
    @FunctionalInterface
    public interface BrowserOpener {
        /**
         * @param uri the URI to open
         * @throws IOException on failure
         */
        void open(URI uri) throws IOException;
    }

    private final Map<String, String> environment;
    private final Path userHome;
    private final boolean interactiveTerminal;
    private final ClientEnvironment clientEnvironment;
    private final OperatorGate gate;
    private final OperatorStateDirectory stateDirectory;
    private final DirectFileAccess directFiles;
    private final TerminalProvider terminalProvider;
    private final BrowserOpener browserOpener;

    /**
     * Creates an operator context assuming an interactive terminal and capturing client environment.
     *
     * @param environment the process environment
     * @param userHome    the user's home directory
     */
    public OperatorContext(Map<String, String> environment, Path userHome) {
        this(environment, userHome, true);
    }

    /**
     * Creates an operator context with explicit terminal interactivity flag.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     */
    public OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal) {
        this(environment, userHome, interactiveTerminal, ClientEnvironment.capture(environment));
    }

    /**
     * Creates an operator context with full explicit configuration.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     * @param clientEnvironment   the captured client environment
     */
    public OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal,
            ClientEnvironment clientEnvironment) {
        this(environment, userHome, interactiveTerminal, clientEnvironment, null, null);
    }

    /**
     * Creates an operator context with terminal provider and browser opener injection.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     * @param clientEnvironment   the captured client environment
     * @param terminalProvider    provider for JLine terminal, {@code null} for default
     * @param browserOpener       browser opener, {@code null} for default
     */
    public OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal,
            ClientEnvironment clientEnvironment, TerminalProvider terminalProvider, BrowserOpener browserOpener) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
        this.userHome = Objects.requireNonNull(userHome, "userHome");
        this.interactiveTerminal = interactiveTerminal;
        this.clientEnvironment = Objects.requireNonNull(clientEnvironment, "clientEnvironment");
        this.gate = new OperatorGate(this);
        this.stateDirectory = OperatorStateDirectory.resolve(environment, userHome);
        this.directFiles = new DirectFileAccess(paths());
        this.terminalProvider = terminalProvider != null ? terminalProvider
                : () -> defaultTerminal(interactiveTerminal);
        this.browserOpener = browserOpener != null ? browserOpener : OperatorContext::defaultBrowserOpen;
    }

    /** @return the context of the running process */
    static OperatorContext system() {
        var console = System.console();
        var interactive = console != null && console.isTerminal();
        return new OperatorContext(System.getenv(), Path.of(System.getProperty("user.home")), interactive);
    }

    /** @return the process environment */
    public Map<String, String> environment() {
        return environment;
    }

    /** @return the user's home directory */
    public Path userHome() {
        return userHome;
    }

    /** @return {@code true} if attached to an interactive terminal */
    public boolean isInteractiveTerminal() {
        return interactiveTerminal;
    }

    /** @return the captured client environment */
    public ClientEnvironment clientEnvironment() {
        return clientEnvironment;
    }

    /** @return the operator gate for security verifications */
    public OperatorGate gate() {
        return gate;
    }

    /** @return the state directory manager */
    public OperatorStateDirectory stateDirectory() {
        return stateDirectory;
    }

    /** @return the direct file access enforcer */
    public DirectFileAccess directFiles() {
        return directFiles;
    }

    /** @return the machine paths */
    public MachinePaths paths() {
        return MachinePaths.resolve(environment, userHome, MachinePaths.Os.current());
    }

    /**
     * @param startOnDemand {@code true} to start the runtime when none answers
     * @return the runtime client of this command
     * @throws IOException if the client cannot be set up
     */
    public RuntimeClient runtime(boolean startOnDemand) throws IOException {
        var headers = Map.of(
                "User-Agent", "pm-operator/" + PmVersion.current(),
                "Accept-Language", clientEnvironment.locale());
        return RuntimeAccess.local(environment, userHome, headers, startOnDemand);
    }

    /**
     * Creates or returns the JLine terminal.
     *
     * @return the terminal instance
     * @throws IOException on creation failure
     */
    public Terminal createTerminal() throws IOException {
        return terminalProvider.createTerminal();
    }

    /**
     * @return the browser opener instance
     */
    public BrowserOpener browserOpener() {
        return browserOpener;
    }

    /**
     * Opens the given URI in an external browser.
     *
     * @param uri the URI to open
     * @throws IOException on failure
     */
    public void openBrowser(URI uri) throws IOException {
        browserOpener.open(uri);
    }

    private static Terminal defaultTerminal(boolean interactive) throws IOException {
        return TerminalBuilder.builder()
                .system(interactive)
                .ffm(true)
                .dumb(true)
                .build();
    }

    static void defaultBrowserOpen(URI uri) throws IOException {
        if (Boolean.getBoolean("pm.test.no.browser")
                || System.getenv("PM_TEST_NO_BROWSER") != null
                || GraphicsEnvironment.isHeadless()) {
            throw new IOException("Headless or non-desktop environment: browser not supported");
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
            return;
        }
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            new ProcessBuilder("open", uri.toString()).start();
        } else if (os.contains("win")) {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString()).start();
        } else {
            new ProcessBuilder("xdg-open", uri.toString()).start();
        }
    }
}
