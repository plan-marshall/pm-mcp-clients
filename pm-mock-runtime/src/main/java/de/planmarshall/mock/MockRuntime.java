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

    /**
     * The legacy case names of Scenario 3 mapped to their consolidated state. The registry's own
     * coarse alias table points every project/start alias at the scenario's primary state; a client
     * (and a transcript test) that still names a case receives the case it names. The registry table
     * is owned by the Wave 3 finalization; this runtime seam is removed with the transitional
     * {@code startCase} fallback when that wave makes the mapping authoritative.
     */
    /** The owner-only mode of the runtime token and the web device secret. */
    private static final String FILE_MODE = "rw-------";

    private static final Map<String, String> LEGACY_CASE_STATES = Map.ofEntries(
            Map.entry(StandardScenarios.PROJECTS, "projects.explorer"),
            Map.entry(StandardScenarios.ENROLMENT, "enrol.scan"),
            Map.entry(StandardScenarios.GIT_SOURCES, "sources.git"),
            Map.entry(StandardScenarios.LESSON_SOURCE, "sources.lessons"),
            Map.entry(StandardScenarios.START, "start.sources"),
            Map.entry(StandardScenarios.START_SOURCES, "start.sources"),
            Map.entry(StandardScenarios.START_CHAT, "start.chat"),
            Map.entry(StandardScenarios.START_DOCUMENTS, "start.documents"),
            Map.entry(StandardScenarios.START_ENTRY_CONTINUE, "start.entry-continue"),
            Map.entry(StandardScenarios.START_EPIC, "start.epic"),
            Map.entry(StandardScenarios.START_EPIC_ADOPT, "start.epic-adopt"),
            Map.entry(StandardScenarios.START_FOLLOW_UP, "start.follow-up"),
            Map.entry(StandardScenarios.START_ISSUE_FAILED, "start.issue-failed"),
            Map.entry(StandardScenarios.START_LESSON, "start.lesson"),
            Map.entry(StandardScenarios.START_LOCAL_CHANGES, "start.local-changes"),
            Map.entry(StandardScenarios.START_PULL_REQUEST, "start.pull-request"),
            Map.entry(StandardScenarios.START_RATE_LIMITED, "start.rate-limited"),
            Map.entry(StandardScenarios.START_RECIPE, "start.recipe"),
            Map.entry(StandardScenarios.START_REFUSED, "start.refused"),
            Map.entry(StandardScenarios.START_UNSCREENED, "start.unscreened"));

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

        // 1. Initialize filesystem layout (<base>/run, <base>/state)
        setupFilesystem(paths, token);

        // The device secret survives a restart on a stable base: a paired browser keeps its
        // access across the mock's restart. An explicit secret still wins over the stored one.
        var deviceSecret = resolveDeviceSecret(config, paths);

        // 2. Load scenario
        var registry = config.getRegistry() != null ? config.getRegistry() : new ScenarioRegistry();
        registerLegacyCaseStates(registry);
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
                engine.setWebPort(tcpServer.port());
            } catch (IOException e) {
                unixServer.close();
                engine.close();
                throw e;
            }
        }

        return new MockRuntime(new RuntimeContext(
                baseDir, cleanupDir, paths, token, deviceSecret, cleanup, engine, unixServer, tcpServer));
    }

    private static void registerLegacyCaseStates(ScenarioRegistry registry) {
        LEGACY_CASE_STATES.forEach((caseName, stateId) -> registry.registerLegacyAlias(caseName,
                StandardScenarios.PROJECTS_AND_START + "#" + stateId));
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
            Files.setPosixFilePermissions(tempToken, PosixFilePermissions.fromString(FILE_MODE));
            Files.move(tempToken, paths.runtimeToken(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            // state/runtime.json
            var runtimeJson = "{\"pid\":" + ProcessHandle.current().pid()
                    + ",\"start_instant\":\"" + Instant.now().toString() + "\""
                    + ",\"version\":\"0.1.0-SNAPSHOT\""
                    + ",\"listener\":\"unix\""
                    + ",\"socket_path\":\"" + paths.socket().toString().replace("\\", "\\\\") + "\"}";
            var tempRecord = paths.stateDir().resolve("runtime.json.tmp");
            Files.writeString(tempRecord, runtimeJson);
            Files.setPosixFilePermissions(tempRecord, PosixFilePermissions.fromString(FILE_MODE));
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
     * The name of the file that keeps the web device secret on a stable base.
     */
    static final String DEVICE_SECRET_FILE = "mock-web-device-secret";

    /**
     * Resolves the device secret the web listener accepts.
     * <p>
     * An explicitly configured secret wins. Otherwise the secret of a previous start on the same
     * base is reused, so a browser that paired once stays paired across a mock restart; only a
     * base without a stored secret gets a freshly generated one, written atomically with owner-only
     * permissions. Deleting the base (or {@code target/machine}) is the explicit "fresh machine".
     *
     * @param config the runtime configuration
     * @param paths  the machine paths of the base
     * @return the device secret to accept
     * @throws IOException when the stored secret cannot be read or the new one cannot be written
     */
    private static String resolveDeviceSecret(MockRuntimeConfig config, MachinePaths paths) throws IOException {
        var explicit = config.getDeviceSecret();
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        var secretFile = paths.stateDir().resolve(DEVICE_SECRET_FILE);
        if (Files.exists(secretFile)) {
            var stored = Files.readString(secretFile).trim();
            if (!stored.isEmpty()) {
                return stored;
            }
        }
        var generated = config.effectiveDeviceSecret();
        publishSecret(secretFile, generated);
        return generated;
    }

    /**
     * Publishes the device secret atomically: a temporary file in the same directory, owner-only
     * permissions, then an atomic move, so a failed start never leaves a partial secret behind.
     *
     * @param secretFile the live secret file
     * @param secret     the secret to write
     * @throws IOException when the file cannot be written or moved into place
     */
    private static void publishSecret(Path secretFile, String secret) throws IOException {
        var temp = secretFile.resolveSibling(secretFile.getFileName() + ".tmp");
        try {
            Files.writeString(temp, secret);
            try {
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString(FILE_MODE));
            } catch (UnsupportedOperationException _) {
                // A non-POSIX file system has no mode to set: the state directory guards the file.
            }
            Files.move(temp, secretFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
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
     * @return the underlying mock runtime engine
     */
    public MockRuntimeEngine engine() {
        return engine;
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
     * @return the pairing code the web listener accepts right now, or {@code null} after it was
     *         consumed and until a fresh one is issued
     */
    public String pairingCode() {
        return engine.pairingCode();
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
