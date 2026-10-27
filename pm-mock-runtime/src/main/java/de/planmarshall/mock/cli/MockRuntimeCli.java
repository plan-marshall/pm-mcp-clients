/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.cli;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;

/**
 * Command-line entrypoint for starting a standalone mock runtime process.
 */
public final class MockRuntimeCli {

    private static final String ERR_MISSING_ARG = "Error: missing argument for ";

    private MockRuntimeCli() {
    }

    /**
     * CLI entrypoint.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        var out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        var err = new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);
        var exitCode = run(args, out, err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Result of parsing command-line arguments.
     *
     * @param exitCode the exit code (0 for success or help, non-zero for error)
     * @param config   the configuration if arguments were valid and help was not requested
     */
    public record CliParseResult(int exitCode, MockRuntimeConfig config) {
        /** @return {@code true} if configuration was successfully parsed */
        public boolean isSuccess() {
            return exitCode == 0 && config != null;
        }
    }

    private static boolean isValuedOption(String arg) {
        return switch (arg) {
            case "--scenario", "-s", "--base", "-b", "--web-port", "-p", "--token", "-t",
                "--device-secret", "-d" -> true;
            default -> false;
        };
    }

    /**
     * Parses command-line arguments without starting the server, using the system environment.
     *
     * @param args command-line arguments
     * @param out  standard output stream
     * @param err  error output stream
     * @return parse result
     */
    public static CliParseResult parse(String[] args, PrintStream out, PrintStream err) {
        return parse(args, out, err, System.getenv());
    }

    private static final class CliOptions {
        private String scenario = StandardScenarios.STATUS_ONLY;
        private Path base;
        private Integer webPort;
        private String token;
        private String deviceSecret;
    }

    /**
     * Parses command-line arguments without starting the server.
     *
     * @param args command-line arguments
     * @param out  standard output stream
     * @param err  error output stream
     * @param env  environment map for resolving environment variable fallbacks
     * @return parse result
     */
    public static CliParseResult parse(String[] args, PrintStream out, PrintStream err, Map<String, String> env) {
        var opts = new CliOptions();
        var parseExit = parseArguments(args, opts, out, err);
        if (parseExit != null) {
            return new CliParseResult(parseExit, null);
        }

        var webPort = opts.webPort != null ? opts.webPort : resolveFallbackWebPort(env, err);
        if (webPort == null) {
            return new CliParseResult(2, null);
        }

        var configBuilder = MockRuntimeConfig.builder()
                .scenario(opts.scenario)
                .base(opts.base)
                .webPort(webPort);

        if (opts.token != null) {
            configBuilder.runtimeToken(opts.token);
        }

        if (opts.deviceSecret != null) {
            configBuilder.deviceSecret(opts.deviceSecret);
        }

        return new CliParseResult(0, configBuilder.build());
    }

    private static Integer parseArguments(String[] args, CliOptions opts, PrintStream out, PrintStream err) {
        var index = new int[]{0};
        while (index[0] < args.length) {
            var arg = args[index[0]++];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage(out);
                return 0;
            }
            var errCode = parseOption(arg, args, index, opts, err);
            if (errCode != null) {
                return errCode;
            }
        }
        return null;
    }

    private static Integer parseOption(String arg, String[] args, int[] index, CliOptions opts, PrintStream err) {
        if (index[0] >= args.length && isValuedOption(arg)) {
            err.println(ERR_MISSING_ARG + arg);
            return 2;
        }
        switch (arg) {
            case "--scenario", "-s" -> opts.scenario = args[index[0]++];
            case "--base", "-b" -> opts.base = Path.of(args[index[0]++]);
            case "--token", "-t" -> opts.token = args[index[0]++];
            case "--device-secret", "-d" -> opts.deviceSecret = args[index[0]++];
            case "--web-port", "-p" -> {
                opts.webPort = parsePortArg(args[index[0]++], err);
                if (opts.webPort == null) {
                    return 2;
                }
            }
            default -> {
                err.println("Error: unrecognized argument: " + arg);
                printUsage(err);
                return 2;
            }
        }
        return null;
    }

    /**
     * Executes the CLI with specified output streams.
     *
     * @param args command-line arguments
     * @param out  standard output stream
     * @param err  error output stream
     * @return exit code (0 for success, non-zero for error)
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        var result = parse(args, out, err);
        if (!result.isSuccess()) {
            return result.exitCode();
        }

        MockRuntime runtime;
        try {
            runtime = MockRuntime.start(result.config());
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IOException | RuntimeException e) {
            err.println("Error starting mock runtime: " + e.getMessage());
            return 1;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            out.println("\nShutting down mock runtime...");
            runtime.close();
        }));

        printRuntimeInfo(runtime, out);

        try {
            Thread.currentThread().join();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        return 0;
    }

    private static void printRuntimeInfo(MockRuntime runtime, PrintStream out) {
        out.println("Mock runtime started successfully:");
        out.println("  Base (PM_MCP_BASE): " + runtime.base());
        out.println("  Unix Socket:        " + runtime.socketPath());
        if (runtime.webPort() >= 0) {
            out.println("  Loopback Web Port:  " + runtime.webPort() + " (http://127.0.0.1:" + runtime.webPort() + ")");
        } else {
            out.println("  Loopback Web Port:  disabled");
        }
        out.println("  Scenario:           " + runtime.currentScenarioName());
        out.println("  Initial State:      " + runtime.currentStateName());
        out.println("  Runtime Token:      " + runtime.token());
        var pairingCode = runtime.pairingCode();
        if (pairingCode != null && !pairingCode.isBlank() && runtime.webPort() >= 0) {
            var appPort = System.getenv("PM_WEB_PORT");
            var effectiveAppPort = appPort != null && !appPort.isBlank() ? appPort.trim() : "5173";
            out.println("  Pairing Code:       " + pairingCode);
            out.println("  Web App URL:        http://" + systemHost() + ":" + effectiveAppPort + "/pair#code=" + pairingCode);
        }
        out.println();
        out.println("Use 'pm-operator' with PM_MCP_BASE=" + runtime.base() + " to interact.");
        out.println("Press Ctrl+C to terminate.");
        out.flush();
    }

    /**
     * The reachable name of this machine for the printed development URL.
     * <p>
     * The name is preferred over an address: it survives DHCP changes and resolves through mDNS.
     * {@code PM_WEB_HOST} overrides it, so an operator can point the URL at another interface. The
     * local host name is read without a reverse lookup, which can block for tens of seconds on a
     * network with a slow DNS resolver.
     *
     * @return the machine's host name, or {@code localhost} as the fallback
     */
    static String systemHost() {
        var override = System.getenv("PM_WEB_HOST");
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        try {
            var name = InetAddress.getLocalHost().getHostName();
            if (name != null && !name.isBlank()) {
                return name;
            }
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (UnknownHostException _) {
            // A machine without a resolvable name still has the loopback coordinate.
        }
        return "localhost";
    }

    private static Integer parsePortArg(String portStr, PrintStream err) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException _) {
            err.println("Error: invalid port: " + portStr);
            return null;
        }
    }

    private static Integer resolveFallbackWebPort(Map<String, String> env, PrintStream err) {
        var envPort = env != null ? env.get("PM_MOCK_WEB_PORT") : null;
        if (envPort == null || envPort.isBlank()) {
            return 7420;
        }
        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException _) {
            err.println("Error: invalid port in PM_MOCK_WEB_PORT: " + envPort);
            return null;
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: pm-mock-runtime [options]");
        out.println("Options:");
        out.println("  -s, --scenario <name|path>  The initial scenario name or file path (default: status-only)");
        out.println("  -b, --base <dir>            Target machine directory <PM_MCP_BASE> (default: temporary dir)");
        out.println("  -p, --web-port <port>       Loopback TCP port for web client testing (default: 7420 or PM_MOCK_WEB_PORT)");
        out.println("  -t, --token <token>         Fixed runtime token for authentication");
        out.println("  -d, --device-secret <secret> Fixed web device secret, bypassing the persisted one");
        out.println("  -h, --help                  Print this help message");
    }
}
