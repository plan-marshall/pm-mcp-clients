/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.runtime.Authenticator;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpServer;
import de.planmarshall.mock.runtime.MockRuntimeEngine;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * The mock runtime server standing in for {@code pm-mcpd} towards operator clients.
 * <p>
 * Binds a Unix domain socket under {@code <PM_MCP_BASE>/run/runtime.sock}, generates the
 * {@code runtime.token} and {@code runtime.json} files expected by {@code pm-api}, and optionally
 * exposes a loopback web port. Driven by hypermedia scenario files.
 */
public final class MockRuntime implements AutoCloseable {

    private final Path base;
    private final Path cleanupDir;
    private final MachinePaths paths;
    private final String token;
    private final String deviceSecret;
    private final boolean cleanupOnClose;
    private final MockRuntimeEngine engine;
    private final MockHttpServer unixServer;
    private final MockHttpServer tcpServer;

    private record RuntimeContext(
            Path base,
            Path cleanupDir,
            MachinePaths paths,
            String token,
            String deviceSecret,
            boolean cleanupOnClose,
            MockRuntimeEngine engine,
            MockHttpServer unixServer,
            MockHttpServer tcpServer) {
    }

    private MockRuntime(RuntimeContext ctx) {
        this.base = ctx.base();
        this.cleanupDir = ctx.cleanupDir();
        this.paths = ctx.paths();
        this.token = ctx.token();
        this.deviceSecret = ctx.deviceSecret();
        this.cleanupOnClose = ctx.cleanupOnClose();
        this.engine = ctx.engine();
        this.unixServer = ctx.unixServer();
        this.tcpServer = ctx.tcpServer();
    }

    /**
     * Starts a mock runtime instance with the default configuration and default scenario.
     *
     * @return the running mock runtime
     * @throws IOException on startup failure
     */
    public static MockRuntime start() throws IOException {
        return start(MockRuntimeConfig.builder().build());
    }

    /**
     * Starts a mock runtime instance for the given scenario name.
     *
     * @param scenario the initial scenario name or path
     * @return the running mock runtime
     * @throws IOException on startup failure
     */
    public static MockRuntime start(String scenario) throws IOException {
        return start(MockRuntimeConfig.builder().scenario(scenario).build());
    }

    @SuppressWarnings({"java:S5443", "java:S7467"})
    private static Path createSecureTempDir() throws IOException {
        try {
            var attrs = PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwx------"));
            return Files.createTempDirectory("pm-mock", attrs);
        } catch (UnsupportedOperationException _) {
            var dir = Files.createTempDirectory("pm-mock"); // NOSONAR
            var file = dir.toFile();
            var permissionsApplied = file.setReadable(true, true)
                    && file.setWritable(true, true)
                    && file.setExecutable(true, true);
            if (!permissionsApplied) {
                // Operating system or filesystem does not support setting file permissions
            }
            return dir;
        }
    }

    /**
     * Starts a mock runtime with the given configuration.
     *
     * @param config the configuration
     * @return the running mock runtime
     * @throws IOException on startup failure
     */
    @SuppressWarnings("java:S2095")
    public static MockRuntime start(MockRuntimeConfig config) throws IOException {
        Objects.requireNonNull(config, "config");

        var autoCreatedBase = config.getBase() == null;
        Path baseDir;
        Path cleanupDir;
        if (autoCreatedBase) {
            var parent = createSecureTempDir();
            baseDir = parent.resolve("b");
            cleanupDir = parent;
        } else {
            baseDir = config.getBase();
            cleanupDir = baseDir;
        }

        var cleanup = config.getCleanupBaseOnClose() != null ? config.getCleanupBaseOnClose() : autoCreatedBase;
        var os = MachinePaths.Os.current();
        var paths = new MachinePaths(baseDir, os);

        if (!paths.socketPathFits()) {
            throw new IllegalStateException("Socket path exceeds platform sun_path limit (" + os.sunPathLimit()
                    + " bytes): " + paths.socket());
        }

        var token = config.effectiveRuntimeToken();
        var deviceSecret = config.effectiveDeviceSecret();

        // 1. Initialize filesystem layout (<base>/run, <base>/state)
        setupFilesystem(paths, token);

        // 2. Load scenario
        var registry = config.getRegistry() != null ? config.getRegistry() : new ScenarioRegistry();
        Scenario initialScenario;
        if (config.getInitialScenario() != null) {
            initialScenario = config.getInitialScenario();
            registry.register(initialScenario);
        } else {
            initialScenario = registry.resolve(config.getScenario());
        }

        // 3. Initialize engine
        var engine = new MockRuntimeEngine(registry, initialScenario, token, deviceSecret);

        // 4. Start Unix domain socket listener
        MockHttpServer unixServer;
        try {
            unixServer = MockHttpServer.startUnix(paths.socket(), (req, res) -> engine.dispatch(req, res, false));
        } catch (IOException e) {
            engine.close();
            throw e;
        }

        // 5. Start optional loopback TCP listener
        MockHttpServer tcpServer = null;
        if (config.getWebPort() >= 0) {
            try {
                tcpServer = MockHttpServer.startTcp(config.getWebPort(), (req, res) -> engine.dispatch(req, res, true));
            } catch (IOException e) {
                unixServer.close();
                engine.close();
                throw e;
            }
        }

        return new MockRuntime(new RuntimeContext(
                baseDir, cleanupDir, paths, token, deviceSecret, cleanup, engine, unixServer, tcpServer));
    }

    private static void setupFilesystem(MachinePaths paths, String token) throws IOException {
        var base = paths.base();
        var isPosix = Files.getFileStore(base.getParent()).supportsFileAttributeView("posix");

        if (isPosix) {
            var dirMode = PosixFilePermissions.fromString("rwx------");
            Files.createDirectories(paths.runDir(), PosixFilePermissions.asFileAttribute(dirMode));
            Files.createDirectories(paths.stateDir(), PosixFilePermissions.asFileAttribute(dirMode));
            Files.setPosixFilePermissions(base, dirMode);
            Files.setPosixFilePermissions(paths.runDir(), dirMode);
            Files.setPosixFilePermissions(paths.stateDir(), dirMode);

            // runtime.token
            var tempToken = paths.runDir().resolve("runtime.token.tmp");
            Files.writeString(tempToken, token);
            Files.setPosixFilePermissions(tempToken, PosixFilePermissions.fromString("rw-------"));
            Files.move(tempToken, paths.runtimeToken(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            // state/runtime.json
            var runtimeJson = "{\"pid\":" + ProcessHandle.current().pid()
                    + ",\"start_instant\":\"" + Instant.now().toString() + "\""
                    + ",\"version\":\"0.1.0-SNAPSHOT\""
                    + ",\"listener\":\"unix\""
                    + ",\"socket_path\":\"" + paths.socket().toString().replace("\\", "\\\\") + "\"}";
            var tempRecord = paths.stateDir().resolve("runtime.json.tmp");
            Files.writeString(tempRecord, runtimeJson);
            Files.setPosixFilePermissions(tempRecord, PosixFilePermissions.fromString("rw-------"));
            Files.move(tempRecord, paths.runtimeRecord(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.createDirectories(paths.runDir());
            Files.createDirectories(paths.stateDir());
            Files.writeString(paths.runtimeToken(), token);
            var runtimeJson = "{\"pid\":" + ProcessHandle.current().pid()
                    + ",\"start_instant\":\"" + Instant.now().toString() + "\""
                    + ",\"version\":\"0.1.0-SNAPSHOT\""
                    + ",\"listener\":\"unix\""
                    + ",\"socket_path\":\"" + paths.socket().toString().replace("\\", "\\\\") + "\"}";
            Files.writeString(paths.runtimeRecord(), runtimeJson);
        }
    }

    /**
     * Switches the active scenario using a name-based lookup.
     *
     * @param scenarioName the name of the new scenario (e.g. {@code onboarding-harness-not-logged-in})
     */
    public void switchScenario(String scenarioName) {
        engine.switchScenario(scenarioName);
    }

    /**
     * Resets the active scenario to its starting state.
     */
    public void reset() {
        engine.reset();
    }

    /**
     * Directly transitions to a state within the current scenario.
     *
     * @param stateId the state identifier
     */
    public void goToState(String stateId) {
        engine.goToState(stateId);
    }

    /**
     * @return the active scenario name
     */
    public String currentScenarioName() {
        return engine.currentScenario().name();
    }

    /**
     * @return the active state identifier
     */
    public String currentStateName() {
        return engine.currentStateId();
    }

    /**
     * @return the currently active scenario definition
     */
    public Scenario currentScenario() {
        return engine.currentScenario();
    }

    /**
     * @return the machine root base path
     */
    public Path base() {
        return base;
    }

    /**
     * @return the machine paths
     */
    public MachinePaths paths() {
        return paths;
    }

    /**
     * @return the Unix domain socket path
     */
    public Path socketPath() {
        return paths.socket();
    }

    /**
     * @return the loopback web TCP port, or -1 if disabled
     */
    public int webPort() {
        return tcpServer != null ? tcpServer.port() : -1;
    }

    /**
     * @return the accepted runtime token
     */
    public String token() {
        return token;
    }

    /**
     * @return the accepted device secret for web clients
     */
    public String deviceSecret() {
        return deviceSecret;
    }

    /**
     * @return the accepted pairing code for web clients
     */
    public String pairingCode() {
        return engine.currentScenario().pairingCode();
    }

    /**
     * @return an environment map defining {@code PM_MCP_BASE}
     */
    public Map<String, String> environment() {
        var env = new HashMap<String, String>();
        env.put(MachinePaths.ENV_BASE, base.toString());
        return env;
    }

    /**
     * Creates a pre-authenticated {@link RuntimeClient} targeting this mock runtime's Unix domain socket.
     *
     * @return a runtime client targeting this runtime
     */
    public RuntimeClient client() {
        return client(Map.of());
    }

    /**
     * Creates a pre-authenticated {@link RuntimeClient} targeting this mock runtime's Unix domain socket
     * with additional fixed headers.
     *
     * @param headers additional headers to include on each request
     * @return a runtime client targeting this runtime
     */
    public RuntimeClient client(Map<String, String> headers) {
        var authenticator = new Authenticator() {
            private final Credential cred = new Credential("Authorization", "Bearer " + token);

            @Override
            public Credential credential() {
                return cred;
            }

            @Override
            public boolean renew() {
                return false;
            }
        };
        return new RuntimeClient(
                new UnixSocketHttpClient(paths.socket()),
                authenticator,
                null,
                headers != null ? headers : Map.of()
        );
    }

    /**
     * @return all requests received by the runtime across both listeners
     */
    public List<MockHttpRequest> requests() {
        var all = new ArrayList<>(unixServer.requests());
        if (tcpServer != null) {
            all.addAll(tcpServer.requests());
        }
        all.sort(Comparator.comparingLong(MockHttpRequest::receivedNanos));
        return List.copyOf(all);
    }

    /**
     * Awaits the next request on the Unix domain socket.
     *
     * @param timeout max wait duration
     * @return the request, or {@code null}
     * @throws InterruptedException if interrupted
     */
    public MockHttpRequest awaitRequest(Duration timeout) throws InterruptedException {
        return unixServer.awaitRequest(timeout);
    }

    @Override
    public void close() {
        try {
            if (tcpServer != null) {
                tcpServer.close();
            }
        } catch (IOException _) {
            // Ignore error when closing TCP server
        }
        try {
            unixServer.close();
        } catch (IOException _) {
            // Ignore error when closing Unix socket server
        }
        engine.close();

        if (cleanupOnClose && cleanupDir != null && Files.exists(cleanupDir)) {
            try (var walk = Files.walk(cleanupDir)) {
                for (var path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            } catch (IOException _) {
                // Ignore failure during temp directory cleanup
            }
        }
    }
}
