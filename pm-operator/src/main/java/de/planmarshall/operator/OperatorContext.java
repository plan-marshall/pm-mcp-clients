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
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.PmVersion;
import de.planmarshall.api.runtime.RuntimeAccess;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.dialogue.StateDialogue;
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
    private final Clock clock;
    private final Duration projectSetupWorkingTimeout;
    private final ConsoleNoticeBuffer noticeBuffer = new ConsoleNoticeBuffer();
    private final AtomicBoolean consoleActive = new AtomicBoolean(false);
    private final Deque<Terminal> scopedTerminals = new ArrayDeque<>();

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
        this(environment, userHome, interactiveTerminal, clientEnvironment, terminalProvider, browserOpener, Clock.systemDefaultZone());
    }

    /**
     * Creates an operator context with full injection including clock.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     * @param clientEnvironment   the captured client environment
     * @param terminalProvider    provider for JLine terminal, {@code null} for default
     * @param browserOpener       browser opener, {@code null} for default
     * @param clock               the clock to use, {@code null} for {@link Clock#systemDefaultZone()}
     */
    public OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal,
                        ClientEnvironment clientEnvironment, @Nullable TerminalProvider terminalProvider,
                        @Nullable BrowserOpener browserOpener, @Nullable Clock clock) {
        this(environment, userHome, interactiveTerminal, clientEnvironment,
                            new Seams(terminalProvider, browserOpener, clock, StateDialogue.WORKING_TIMEOUT));
    }

    /**
     * What a test injects into a context: the terminal, the browser, the clock and the bound after
     * which the project setup dialogue gives up on a working state.
     *
     * @param terminalProvider           provider for JLine terminal, {@code null} for default
     * @param browserOpener              browser opener, {@code null} for default
     * @param clock                      the clock to use, {@code null} for {@link Clock#systemDefaultZone()}
     * @param projectSetupWorkingTimeout how long a working state of a project setup may stay
     *                                   without a link before the dialogue gives up
     */
    record Seams(@Nullable TerminalProvider terminalProvider, @Nullable BrowserOpener browserOpener,
    @Nullable Clock clock, Duration projectSetupWorkingTimeout) {
    }

    /**
     * Creates an operator context whose project setup dialogue gives up on a working state after
     * the bound the seams carry instead of {@link StateDialogue#WORKING_TIMEOUT}.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     * @param clientEnvironment   the captured client environment
     * @param seams               what is injected: terminal, browser, clock and the working bound
     */
    OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal,
                        ClientEnvironment clientEnvironment, Seams seams) {
        var injectedTerminal = seams.terminalProvider();
        var injectedBrowser = seams.browserOpener();
        var injectedClock = seams.clock();
        this.projectSetupWorkingTimeout = Objects.requireNonNull(seams.projectSetupWorkingTimeout(),
                            "projectSetupWorkingTimeout");
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
        this.userHome = Objects.requireNonNull(userHome, "userHome");
        this.interactiveTerminal = interactiveTerminal;
        this.clientEnvironment = Objects.requireNonNull(clientEnvironment, "clientEnvironment");
        this.gate = new OperatorGate(this);
        this.stateDirectory = OperatorStateDirectory.resolve(environment, userHome);
        this.directFiles = new DirectFileAccess(paths());
        this.terminalProvider = injectedTerminal != null ? injectedTerminal
                            : () -> defaultTerminal(interactiveTerminal);
        this.browserOpener = injectedBrowser != null ? injectedBrowser : OperatorContext::defaultBrowserOpen;
        this.clock = injectedClock != null ? injectedClock : Clock.systemDefaultZone();
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

    /** @return the clock of this context */
    public Clock clock() {
        return clock;
    }

    /**
     * @return how long a working state of a project setup may stay without a link before the
     *         dialogue gives up; {@link StateDialogue#WORKING_TIMEOUT} unless a test names its own
     */
    public Duration projectSetupWorkingTimeout() {
        return projectSetupWorkingTimeout;
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
     * @return the normalized client locale
     */
    public String locale() {
        return clientEnvironment.locale();
    }

    /**
     * Creates or returns the JLine terminal.
     *
     * <p>While a scoped terminal is set, this is that terminal: the console, and every full-screen
     * verb, lend their terminal to the commands they dispatch, so a nested verb and the dialogues it
     * opens draw and read on the terminal that already owns the process instead of opening a second
     * one (which JLine falls back to a dumb terminal for, and which a typed {@code Ctrl+C} never
     * reaches). Every direct CLI run has no scope and gets the provider's terminal as before.
     *
     * @return the terminal instance
     * @throws IOException on creation failure
     */
    public Terminal createTerminal() throws IOException {
        var scoped = scopedTerminal();
        return scoped != null ? scoped : terminalProvider.createTerminal();
    }

    /**
     * Lends a terminal to the commands dispatched until {@link #clearTerminal(Terminal)}: the caller
     * keeps ownership and closes it, the commands only draw on it. The scopes nest, because a view
     * that a
     * console dispatched lends the same terminal to the dialogue it opens.
     *
     * @param terminal the terminal the nested commands use
     */
    public void useTerminal(Terminal terminal) {
        Objects.requireNonNull(terminal, "terminal");
        synchronized (scopedTerminals) {
            scopedTerminals.push(terminal);
        }
    }

    /**
     * Withdraws the innermost scoped terminal, which must be the one the caller lent:
     * {@link #createTerminal()} uses the next one, or the provider when no scope remains.
     *
     * @param terminal the terminal the caller set with {@link #useTerminal(Terminal)}
     * @throws IllegalStateException when the innermost scope is another terminal: popping it would
     *                               hand the caller's terminal to whoever comes next
     */
    public void clearTerminal(Terminal terminal) {
        synchronized (scopedTerminals) {
            if (scopedTerminals.peek() != terminal) {
                throw new IllegalStateException("the terminal to withdraw is not the innermost scope");
            }
            scopedTerminals.poll();
        }
    }

    /**
     * A terminal a nested command draws on, closed only when this context owns it. A command inside
     * a console or a full-screen view gets the lent terminal and leaves it open for the caller that
     * owns it; a command run on its own gets a terminal of its own, which the lease closes.
     */
    public final class TerminalLease implements AutoCloseable {

        private final Terminal leased;

        private TerminalLease(Terminal leased) {
            this.leased = leased;
        }

        /** @return the terminal to draw on */
        public Terminal terminal() {
            return leased;
        }

        @Override
        public void close() throws IOException {
            if (ownsTerminal(leased)) {
                leased.close();
            }
        }
    }

    /**
     * Opens the terminal a nested command reads from, with the ownership guard its closer needs.
     *
     * @return the lease, to be closed with the command
     * @throws IOException on terminal creation failure
     */
    public TerminalLease openTerminal() throws IOException {
        return new TerminalLease(createTerminal());
    }

    /**
     * @return the innermost scoped terminal, or {@code null} when no scope is set
     */
    private @Nullable Terminal scopedTerminal() {
        synchronized (scopedTerminals) {
            return scopedTerminals.peek();
        }
    }

    /**
     * Tells whether this context created the terminal and must close it, as opposed to one lent by
     * {@link #useTerminal(Terminal)}. Every {@code finally} that closes a terminal consults this
     * predicate, so a nested verb never closes the console's or a view's terminal.
     *
     * @param terminal the terminal a command obtained from {@link #createTerminal()}
     * @return {@code true} when the caller owns the terminal and must close it
     */
    public boolean ownsTerminal(@Nullable Terminal terminal) {
        if (terminal == null) {
            return false;
        }
        synchronized (scopedTerminals) {
            // Every scope counts, not only the innermost: a terminal an outer scope lent is
            // still not this context's to close.
            return !scopedTerminals.contains(terminal);
        }
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
