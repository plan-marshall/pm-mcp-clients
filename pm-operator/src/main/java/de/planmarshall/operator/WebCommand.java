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
import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeClient;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator web}: manages the web listener, pairing codes, QR rendering, and devices
 * (01-commands.adoc#commands, 03-web-server.adoc).
 */
@Command(name = "web", description = "Enables, pairs, or configures the runtime's web access.",
        subcommands = {
                WebCommand.EnableCommand.class,
                WebCommand.DisableCommand.class,
                WebCommand.PairCommand.class,
                WebCommand.OpenCommand.class,
                WebCommand.QrCommand.class,
                WebCommand.DevicesCommand.class
        })
@OperatorVerb
public final class WebCommand implements Callable<Integer> {

    /** The default port of the web listener ({@code machine-config.json}, {@code web.port}). */
    static final int DEFAULT_PORT = 7420;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LABEL = "label";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    WebCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "enable", description = "Opens the web listener (loopback, or all interfaces with --lan).")
    @OperatorVerb
    static final class EnableCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--lan", description = "HTTPS on all interfaces.")
        boolean lan;

        @Option(names = "--port", paramLabel = "N", defaultValue = "" + DEFAULT_PORT,
                description = "TCP port, default ${DEFAULT-VALUE}.")
        int port;

        EnableCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            return put(context, spec, true, lan, port);
        }
    }

    @Command(name = "disable", description = "Closes the web listener.")
    @OperatorVerb
    static final class DisableCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--port", paramLabel = "N", defaultValue = "" + DEFAULT_PORT,
                description = "TCP port, default ${DEFAULT-VALUE}.")
        int port;

        DisableCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            return put(context, spec, false, false, port);
        }
    }

    @Command(name = "pair", description = "Issues a pairing code, its URL, and prints its QR code.")
    @OperatorVerb
    static final class PairCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--label", description = "Device label to record with pairing")
        @Nullable
        String label;

        PairCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var runtime = context.runtime(true);
            var body = new LinkedHashMap<String, Object>();
            if (label != null) body.put(LABEL, label);

            var response = runtime.send(HttpRequest.json("POST", "/api/v1/web/pairings", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 201) {
                var json = JsonTree.asObject(JsonTree.parse(response.body()));
                var code = json != null ? (String) json.get("code") : "CODE";
                var url = json != null ? (String) json.get("url") : "http://127.0.0.1:7420/pair#code=" + code;
                var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);

                out.printf("✓ Pairing code: %s (valid until %s)%n", code, validUntil);
                out.printf("  URL: %s%n%n", url);
                var sideLines = List.of(url, "Code " + code + ", valid until " + validUntil);
                out.println(QrCodeRenderer.render(url, sideLines));
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("web pair", response);
        }
    }

    @Command(name = "open", description = "Opens the web app in the local browser.")
    @OperatorVerb
    static final class OpenCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--pair", description = "Force new pairing code even if browser already paired")
        boolean pair;

        OpenCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var runtime = context.runtime(true);
            var status = StatusCommand.fetch(runtime);
            var port = EntryFlow.getWebPort(status);
            new EntryFlow(context).ensureLoopbackListener(runtime, port);

            String url = "http://127.0.0.1:" + port;
            if (pair) {
                var pairing = new EntryFlow(context).issuePairingCode(runtime, port);
                url = pairing.url();
            } else {
                var paired = new EntryFlow(context).findPairedBrowser(runtime);
                if (paired == null) {
                    var pairing = new EntryFlow(context).issuePairingCode(runtime, port);
                    url = pairing.url();
                }
            }

            var out = spec.commandLine().getOut();
            try {
                context.openBrowser(URI.create(url));
                out.println("✓ Browser opened: " + url);
            } catch (IOException e) {
                out.println("– No browser opened: " + e.getMessage());
                out.println("  Open manually: " + url);
            }
            out.flush();
            return 0;
        }
    }

    @Command(name = "qr", description = "Displays QR code for active web pairing.")
    @OperatorVerb
    static final class QrCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        QrCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var runtime = context.runtime(true);
            var status = StatusCommand.fetch(runtime);
            var port = EntryFlow.getWebPort(status);
            var pairing = new EntryFlow(context).issuePairingCode(runtime, port);
            var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);
            var sideLines = List.of(pairing.url(), "Code " + pairing.code() + ", valid until " + validUntil);
            var out = spec.commandLine().getOut();
            out.println(QrCodeRenderer.render(pairing.url(), sideLines));
            out.flush();
            return 0;
        }
    }

    @Command(name = "devices", description = "Lists, renames, or revokes paired devices.")
    @OperatorVerb
    static final class DevicesCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", arity = "0..1", defaultValue = "list",
                description = "Action: list, rename, revoke, listener-off")
        @Nullable
        String action;

        @Parameters(index = "1", arity = "0..1", description = "Device ID for rename or revoke")
        @Nullable
        String deviceId;

        @Parameters(index = "2", arity = "0..1", description = "New label for rename")
        @Nullable
        String newLabel;

        @Option(names = "--others", description = "Revoke all other paired devices")
        boolean others;

        @Option(names = "--all", description = "Revoke all paired devices")
        boolean all;

        DevicesCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var runtime = context.runtime(true);
            var out = spec.commandLine().getOut();

            if ("rename".equalsIgnoreCase(action)) {
                handleRename(runtime, out);
            } else if ("revoke".equalsIgnoreCase(action)) {
                handleRevoke(runtime, out);
            } else if ("listener-off".equalsIgnoreCase(action)) {
                handleListenerOff(runtime, out);
            } else {
                handleList(runtime, out);
            }
            out.flush();
            return 0;
        }

        private void handleRename(RuntimeClient runtime, PrintWriter out) throws IOException {
            if (deviceId == null || newLabel == null) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(), "rename requires <deviceId> <newLabel>");
            }
            var body = JsonTree.write(Map.of(LABEL, newLabel));
            var response = runtime.send(HttpRequest.json("PUT",
                    "/api/v1/web/devices/" + OperatorUrlUtil.encodePathSegment(deviceId), body));
            if (response.status() == 200 || response.status() == 204) {
                out.println("✓ Renamed: " + newLabel);
            } else {
                out.printf("Failed to rename device: HTTP %d%n", response.status());
            }
        }

        private void handleRevoke(RuntimeClient runtime, PrintWriter out) throws IOException {
            if (!others && !all && deviceId == null) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(), "revoke requires <deviceId>, --others, or --all");
            }
            String path = "/api/v1/web/devices";
            if (others) {
                path += "?scope=others";
            } else if (all) {
                path += "?scope=all";
            } else {
                path += "/" + OperatorUrlUtil.encodePathSegment(deviceId);
            }
            var response = runtime.send(new HttpRequest("DELETE", path, Map.of(), null));
            if (response.status() == 200 || response.status() == 204) {
                out.println("✓ Revoked paired device(s)");
            } else {
                out.printf("Failed to revoke: HTTP %d%n", response.status());
            }
        }

        private void handleListenerOff(RuntimeClient runtime, PrintWriter out) throws IOException {
            var body = JsonTree.write(Map.of("lan", false));
            var response = runtime.send(HttpRequest.json("PUT", "/api/v1/web", body));
            if (response.status() == 200) {
                out.println("✓ Network listener turned off (loopback remains open)");
            } else {
                out.printf("Failed to turn listener off: HTTP %d%n", response.status());
            }
        }

        private void handleList(RuntimeClient runtime, PrintWriter out) throws IOException {
            var response = runtime.send(HttpRequest.get("/api/v1/web/devices"));
            if (response.status() == 200) {
                var parsed = JsonTree.parse(response.body());
                if (parsed instanceof List<?> list && !list.isEmpty()) {
                    out.println("Paired devices · The browsers that can act on this machine");
                    int idx = 1;
                    for (var item : list) {
                        if (item instanceof Map<?, ?> dev) {
                            var labelVal = dev.get(LABEL);
                            var pairedFrom = dev.get("paired_from");
                            out.printf("    %d) %-25s paired from %s%n",
                                    idx++,
                                    labelVal != null ? labelVal : "browser",
                                    pairedFrom != null ? pairedFrom : "loopback");
                        } else {
                            out.printf("    %d) %s%n", idx++, item);
                        }
                    }
                } else {
                    out.println("No paired devices.");
                }
            } else {
                out.printf("Failed to list devices: HTTP %d%n", response.status());
            }
        }
    }

    static int put(OperatorContext context, CommandSpec spec, boolean enabled, boolean lan, int port)
            throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("enabled", enabled);
        body.put("lan", lan);
        body.put("port", port);
        var response = context.runtime(true).send(HttpRequest.json("PUT", "/api/v1/web", JsonTree.write(body)));
        if (response.status() != 200) {
            throw new OperatorRefusedException("web", response);
        }
        var out = spec.commandLine().getOut();
        out.println("web       " + describe(JsonTree.asObject(JsonTree.parse(response.body()))));
        out.flush();
        return 0;
    }

    /**
     * @param web the {@code web} object of the status, may be {@code null}
     * @return a readable line
     */
    static String describe(@Nullable Map<String, Object> web) {
        if (web == null) {
            return "-";
        }
        if (!Boolean.TRUE.equals(web.get("enabled"))) {
            return "disabled";
        }
        return "enabled (" + (Boolean.TRUE.equals(web.get("lan")) ? "lan" : "loopback") + "), port "
                + web.get("port") + ", " + (Boolean.TRUE.equals(web.get("open")) ? "open" : "not open");
    }
}
