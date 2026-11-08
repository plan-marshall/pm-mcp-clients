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
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongFunction;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator drain}: the signal of the specification (01-cli-and-installer.adoc#cli-without-runtime,
 * 25-system.adoc#commands). It signals the process recorded in {@code state/runtime.json}, which drains
 * and exits with its drain timeout, and waits for the exit at most the drain timeout plus the grace
 * period plus a fixed {@code 10} seconds (timeouts/02-runtime-leases-operator-web.adoc#drain-timeout).
 * It sends no request to the runtime's API, asks nothing, runs no operator checks, and cannot be
 * withdrawn; a record whose process is gone names no runtime, and nothing is signalled. A process is
 * signalled only when the record carries a start instant and the process reports an equal one, to
 * the second: otherwise the runtime is not identifiable, and nothing is signalled either.
 */
@Command(name = "drain", description = "Signals the runtime to drain and exit; waits for the exit.")
public final class DrainCommand implements Callable<Integer> {

    /** The drain timeout without a valid {@code drain_timeout_seconds}, in seconds. */
    static final long DEFAULT_DRAIN_TIMEOUT_SECONDS = 600;

    /** The largest drain timeout {@code machine-config.json} may set, in seconds (100 years of 365 days). */
    static final long MAX_DRAIN_TIMEOUT_SECONDS = 3_153_600_000L;

    /** The grace period the runtime gives a terminated job group. */
    static final Duration GRACE_PERIOD = Duration.ofSeconds(5);

    /** The fixed margin the wait adds beyond the drain timeout and the grace period. */
    static final Duration MARGIN = Duration.ofSeconds(10);

    private static final String DRAIN_TIMEOUT_FIELD = "drain_timeout_seconds";
    private static final String PID = "pid";
    private static final String KEY_CONFIG_UNREADABLE = "operator.drain.config_unreadable";

    private final OperatorContext context;
    private final @Nullable Duration bound;
    private final LongFunction<Optional<ProcessHandle>> processes;

    @Spec
    CommandSpec spec = CommandSpec.create();

    /**
     * @param context the operator context; the bound of the wait comes from its machine configuration
     */
    DrainCommand(OperatorContext context) {
        this(context, null);
    }

    /**
     * @param context the operator context
     * @param bound   the bound of the wait for the exit, or {@code null} for the one of the machine
     *                configuration
     */
    DrainCommand(OperatorContext context, @Nullable Duration bound) {
        this(context, bound, ProcessHandle::of);
    }

    /**
     * @param context   the operator context
     * @param bound     the bound of the wait for the exit, or {@code null} for the one of the machine
     *                  configuration
     * @param processes the lookup of a process by its id, the operating system's outside a test
     */
    DrainCommand(OperatorContext context, @Nullable Duration bound,
                        LongFunction<Optional<ProcessHandle>> processes) {
        this.context = Objects.requireNonNull(context, "context");
        this.bound = bound;
        this.processes = Objects.requireNonNull(processes, "processes");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var stored = context.directFiles().readRuntimeRecord();
        if (stored.isEmpty()) {
            return notRunning(out);
        }
        var runtimeRecord = stored.get();
        var handle = processes.apply(runtimeRecord.pid());
        if (handle.isEmpty() || !handle.get().isAlive()
                            || handle.get().pid() == ProcessHandle.current().pid()) {
            return notRunning(out);
        }
        var runtime = handle.get();
        // the process id is an identifier, never a number to group
        var pid = Map.of(PID, String.valueOf(runtime.pid()));
        if (!runtimeRecord.identifies(runtime)) {
            var err = spec.commandLine().getErr();
            err.println(text("operator.drain.not_identifiable", pid));
            err.flush();
            return PmOperator.EXIT_FAILURE;
        }
        if (!runtime.destroy()) {
            if (!runtime.isAlive()) {
                // The runtime exited between the lookup and the signal: it is not running any more.
                return notRunning(out);
            }
            var err = spec.commandLine().getErr();
            err.println(text("operator.drain.signal_failed", pid));
            err.flush();
            return PmOperator.EXIT_FAILURE;
        }
        out.println(text("operator.drain.signalled", pid));
        out.flush();
        var wait = bound != null ? bound : boundOfMachine(spec.commandLine().getErr());
        out.println(awaitExit(runtime, wait) ? text("operator.drain.ended", Map.of())
                            : text("operator.system.still_draining", pid));
        out.flush();
        return 0;
    }

    private int notRunning(PrintWriter out) {
        out.println(text("operator.drain.not_running", Map.of()));
        out.flush();
        return 0;
    }

    /**
     * @param runtime the signalled runtime
     * @param wait    the longest wait
     * @return {@code true} if the runtime exited within the wait
     */
    private static boolean awaitExit(ProcessHandle runtime, Duration wait) {
        try {
            runtime.onExit().get(wait.toNanos(), TimeUnit.NANOSECONDS);
            return true;
        } catch (TimeoutException | ExecutionException _) {
            return !runtime.isAlive();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return !runtime.isAlive();
        }
    }

    /**
     * The drain timeout of {@code machine-config.json} plus the grace period plus the fixed margin. An
     * absent file or key takes the default; a file that cannot be read as one JSON object, or a value
     * that is no integer from {@code 0} to {@link #MAX_DRAIN_TIMEOUT_SECONDS}, takes the default and
     * is named in a warning.
     *
     * @param err where the warning goes
     * @return the bound of the wait
     */
    Duration boundOfMachine(PrintWriter err) {
        var file = context.directFiles().machineConfigPath();
        var seconds = DEFAULT_DRAIN_TIMEOUT_SECONDS;
        try {
            var raw = context.directFiles().readMachineConfig();
            if (raw.isPresent()) {
                seconds = drainTimeout(raw.get(), file.toString(), err);
            }
        } catch (IOException _) {
            warn(err, KEY_CONFIG_UNREADABLE, file.toString());
        }
        return Duration.ofSeconds(seconds).plus(GRACE_PERIOD).plus(MARGIN);
    }

    private long drainTimeout(String raw, String file, PrintWriter err) {
        final Map<String, Object> config;
        try {
            config = JsonTree.asObject(JsonTree.parse(raw));
        } catch (IOException _) {
            warn(err, KEY_CONFIG_UNREADABLE, file);
            return DEFAULT_DRAIN_TIMEOUT_SECONDS;
        }
        if (config == null) {
            warn(err, KEY_CONFIG_UNREADABLE, file);
            return DEFAULT_DRAIN_TIMEOUT_SECONDS;
        }
        if (!config.containsKey(DRAIN_TIMEOUT_FIELD)) {
            return DEFAULT_DRAIN_TIMEOUT_SECONDS;
        }
        if (config.get(DRAIN_TIMEOUT_FIELD) instanceof String) {
            warn(err, "operator.drain.config_invalid", file);
            return DEFAULT_DRAIN_TIMEOUT_SECONDS;
        }
        try {
            return JsonTree.integralNumber(config, DRAIN_TIMEOUT_FIELD, 0, MAX_DRAIN_TIMEOUT_SECONDS);
        } catch (IOException _) {
            warn(err, "operator.drain.config_invalid", file);
            return DEFAULT_DRAIN_TIMEOUT_SECONDS;
        }
    }

    private void warn(PrintWriter err, String key, String file) {
        err.println(text(key, Map.of("file", file, "seconds", DEFAULT_DRAIN_TIMEOUT_SECONDS)));
        err.flush();
    }

    private String text(String key, Map<String, ?> arguments) {
        return MessageCatalogue.format(context.clientEnvironment().locale(), key, arguments);
    }
}
