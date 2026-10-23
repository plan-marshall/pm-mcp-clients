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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.PmVersion;
import de.planmarshall.api.runtime.RuntimeAccess;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.layout.OperatorLayout;

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
    private final ConsoleNoticeBuffer noticeBuffer = new ConsoleNoticeBuffer();
    private final AtomicBoolean consoleActive = new AtomicBoolean(false);
    private final AtomicReference<@Nullable Terminal> scopedTerminal = new AtomicReference<>();

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
                        ClientEnvironment clientEnvironment, @Nullable TerminalProvider terminalProvider, @Nullable BrowserOpener browserOpener) {
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

    /** @return the buffer holding notices while a full-screen view is active */
    public ConsoleNoticeBuffer noticeBuffer() {
        return noticeBuffer;
    }

    /**
     * @return {@code true} when running inside an interactive PmConsole session
     */
    public boolean isConsoleActive() {
        return consoleActive.get();
    }

    /**
     * @param active whether a PmConsole session is running
     */
    public void setConsoleActive(boolean active) {
        consoleActive.set(active);
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
        return RuntimeAccess.local(environment, userHome, headers, startOnDemand,
                            () -> OperatorLayout.current(environment).runtimeExecutable());
    }

    /**
     * Creates or returns the JLine terminal.
     *
     * <p>While a scoped terminal is set, this is that terminal: the console lends its own terminal
     * to a full-screen verb it dispatches, so the nested verb draws on the terminal that already
     * owns the process instead of opening a second one (which JLine falls back to a dumb terminal
     * for). Every direct CLI run has no scope and gets the provider's terminal as before.
     *
     * @return the terminal instance
     * @throws IOException on creation failure
     */
    public Terminal createTerminal() throws IOException {
        var scoped = scopedTerminal.get();
        return scoped != null ? scoped : terminalProvider.createTerminal();
    }

    /**
     * Lends a terminal to the commands dispatched until {@link #clearTerminal()}: the caller keeps
     * ownership and closes it, the commands only draw on it.
     *
     * @param terminal the terminal the nested commands use
     */
    public void useTerminal(Terminal terminal) {
        scopedTerminal.set(Objects.requireNonNull(terminal, "terminal"));
    }

    /** Withdraws the scoped terminal; {@link #createTerminal()} uses the provider again. */
    public void clearTerminal() {
        scopedTerminal.set(null);
    }

    /**
     * Tells whether this context created the terminal and must close it, as opposed to one lent by
     * {@link #useTerminal(Terminal)}. Every {@code finally} that closes a terminal consults this
     * predicate, so a nested verb never closes the console's terminal.
     *
     * @param terminal the terminal a command obtained from {@link #createTerminal()}
     * @return {@code true} when the caller owns the terminal and must close it
     */
    public boolean ownsTerminal(@Nullable Terminal terminal) {
        return terminal != null && terminal != scopedTerminal.get();
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
            new ProcessBuilder("/usr/bin/open", uri.toString()).start();
        } else if (os.contains("win")) {
            var systemRoot = System.getenv("SystemRoot");
            var rundll = systemRoot != null ? systemRoot + "\\System32\\rundll32.exe" : "C:\\Windows\\System32\\rundll32.exe";
            new ProcessBuilder(rundll, "url.dll,FileProtocolHandler", uri.toString()).start();
        } else {
            new ProcessBuilder("/usr/bin/xdg-open", uri.toString()).start();
        }
    }
}
