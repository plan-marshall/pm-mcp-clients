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

import java.io.PrintStream;
import java.nio.file.Path;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;

/**
 * Command-line entrypoint for starting a standalone mock runtime process.
 */
public final class MockRuntimeCli {

    private MockRuntimeCli() {
    }

    /**
     * CLI entrypoint.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        var exitCode = run(args, System.out, System.err);
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

    /**
     * Parses command-line arguments without starting the server.
     *
     * @param args command-line arguments
     * @param out  standard output stream
     * @param err  error output stream
     * @return parse result
     */
    public static CliParseResult parse(String[] args, PrintStream out, PrintStream err) {
        String scenario = "status-only";
        Path base = null;
        int webPort = -1;
        String token = null;

        for (var i = 0; i < args.length; i++) {
            var arg = args[i];
            switch (arg) {
                case "--help", "-h" -> {
                    printUsage(out);
                    return new CliParseResult(0, null);
                }
                case "--scenario", "-s" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: missing argument for " + arg);
                        return new CliParseResult(2, null);
                    }
                    scenario = args[++i];
                }
                case "--base", "-b" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: missing argument for " + arg);
                        return new CliParseResult(2, null);
                    }
                    base = Path.of(args[++i]);
                }
                case "--web-port", "-p" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: missing argument for " + arg);
                        return new CliParseResult(2, null);
                    }
                    try {
                        webPort = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        err.println("Error: invalid port: " + args[i]);
                        return new CliParseResult(2, null);
                    }
                }
                case "--token", "-t" -> {
                    if (i + 1 >= args.length) {
                        err.println("Error: missing argument for " + arg);
                        return new CliParseResult(2, null);
                    }
                    token = args[++i];
                }
                default -> {
                    err.println("Error: unrecognized argument: " + arg);
                    printUsage(err);
                    return new CliParseResult(2, null);
                }
            }
        }

        var configBuilder = MockRuntimeConfig.builder()
                .scenario(scenario)
                .base(base)
                .webPort(webPort);

        if (token != null) {
            configBuilder.runtimeToken(token);
        }

        return new CliParseResult(0, configBuilder.build());
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
        } catch (Exception e) {
            err.println("Error starting mock runtime: " + e.getMessage());
            return 1;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            out.println("\nShutting down mock runtime...");
            runtime.close();
        }));

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
        out.println();
        out.println("Use 'pm-operator' with PM_MCP_BASE=" + runtime.base() + " to interact.");
        out.println("Press Ctrl+C to terminate.");
        out.flush();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return 0;
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: pm-mock-runtime [options]");
        out.println("Options:");
        out.println("  -s, --scenario <name|path>  The initial scenario name or file path (default: status-only)");
        out.println("  -b, --base <dir>            Target machine directory <PM_MCP_BASE> (default: temporary dir)");
        out.println("  -p, --web-port <port>       Loopback TCP port for web client testing (default: disabled)");
        out.println("  -t, --token <token>         Fixed runtime token for authentication");
        out.println("  -h, --help                  Print this help message");
    }
}
