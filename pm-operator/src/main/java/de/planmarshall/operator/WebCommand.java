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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
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
    private static final String STATUS = "status";
    private static final String INDEX = "index";
    private static final String LOOPBACK = "loopback";

    private static String localeFor(OperatorContext context) {
        return context.clientEnvironment().locale();
    }

    @Spec
    CommandSpec spec = CommandSpec.create();

    WebCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
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
            int code = put(context, spec, true, lan, port);
            if (lan) {
                printLanDetails(context, spec, port);
            }
            return code;
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

                var locale = localeFor(context);
                out.println(MessageCatalogue.format(locale, "operator.web.pair_code",
                                    Map.of("code", code, "time", validUntil)));
                out.println(MessageCatalogue.format(locale, "operator.web.pair_url", Map.of("url", url)) + "\n");
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
                out.println(MessageCatalogue.format(localeFor(context), "operator.web.browser_opened", Map.of("url", url)));
            } catch (IOException e) {
                var locale = localeFor(context);
                out.println(MessageCatalogue.format(locale, "operator.web.no_browser",
                                    Map.of("reason", TerminalSanitizer.sanitize(e.getMessage()))));
                out.println(MessageCatalogue.format(locale, "operator.web.open_manually", Map.of("url", url)));
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

        @Option(names = {"-y", "--yes"}, description = "Confirm without prompting")
        boolean yes;

        @Option(names = {"-i", "--interactive"}, description = "Interactive mode")
        boolean interactive;

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
            } else if ("revoke".equalsIgnoreCase(action) || others || all) {
                handleRevoke(runtime, out);
            } else if ("listener-off".equalsIgnoreCase(action) || "listener_off".equalsIgnoreCase(action)) {
                handleListenerOff(runtime, out);
            } else {
                handleList(runtime, out);
            }
            out.flush();
            return 0;
        }

        private void handleRename(RuntimeClient runtime, PrintWriter out) throws IOException {
            var locale = context.clientEnvironment().locale();
            if (deviceId == null || newLabel == null) {
                throw new CommandLine.ParameterException(
                                    spec.commandLine(), "rename requires <deviceId> <newLabel>");
            }
            var cleanLabel = TerminalSanitizer.sanitizeLine(newLabel);
            var body = JsonTree.write(Map.of(LABEL, cleanLabel));
            var response = runtime.send(HttpRequest.json("PUT",
                                "/api/v1/web/devices/" + OperatorUrlUtil.encodePathSegment(deviceId), body));
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.devices.renamed", Map.of(LABEL, cleanLabel)));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.devices.rename_failed",
                                    Map.of(STATUS, response.status())));
            }
        }

        private void handleRevoke(RuntimeClient runtime, PrintWriter out) throws IOException {
            var locale = context.clientEnvironment().locale();
            if (!others && !all && deviceId == null) {
                throw new CommandLine.ParameterException(
                                    spec.commandLine(), "revoke requires <deviceId>, --others, or --all");
            }

            if (interactive && !yes && context.isInteractiveTerminal() && !confirm()) {
                return;
            }

            var scope = others ? "others" : "all";
            var path = deviceId != null
                                ? "/api/v1/web/devices/" + OperatorUrlUtil.encodePathSegment(deviceId)
                                : "/api/v1/web/devices?scope=" + scope;
            var response = runtime.send(new HttpRequest("DELETE", path, Map.of(), null));
            if (response.status() == 200 || response.status() == 204) {
                printRevocation(out, locale, response.body());
            } else {
                out.println(MessageCatalogue.format(locale, "operator.devices.revoke_failed",
                                    Map.of(STATUS, response.status())));
            }
        }

        private void printRevocation(PrintWriter out, String locale, byte[] body) {
            String label = null;
            int revokedCount = 1;
            try {
                var json = JsonTree.asObject(JsonTree.parse(body));
                if (json != null) {
                    label = JsonTree.string(json, LABEL);
                    var count = JsonTree.integer(json, "revoked_count");
                    if (count != null && count > 0) {
                        revokedCount = count;
                    }
                }
            } catch (IOException _) {
                // a body-less 204 revokes the one entry named on the command line
            }
            if (deviceId != null && !all) {
                out.println(MessageCatalogue.format(locale, "operator.devices.revoked_this",
                                    Map.of(LABEL, TerminalSanitizer.sanitizeLine(label != null ? label : deviceId))));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.devices.revoked_all",
                                    Map.of("count", revokedCount)));
            }
        }

        private void handleListenerOff(RuntimeClient runtime, PrintWriter out) throws IOException {
            var locale = context.clientEnvironment().locale();
            var body = JsonTree.write(Map.of("lan", false));
            var response = runtime.send(HttpRequest.json("PUT", "/api/v1/web", body));
            if (response.status() == 200 || response.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.devices.listener_off_done", Map.of()));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.devices.listener_off_failed",
                                    Map.of(STATUS, response.status())));
            }
        }

        private void handleList(RuntimeClient runtime, PrintWriter out) throws IOException {
            var locale = context.clientEnvironment().locale();
            var response = runtime.send(HttpRequest.get("/api/v1/web/devices"));
            if (response.status() != 200) {
                out.println(MessageCatalogue.format(locale, "operator.devices.list_failed",
                                    Map.of(STATUS, response.status())));
                return;
            }

            var statusRes = runtime.send(HttpRequest.get("/api/v1/status"));
            boolean lanOn = true;
            if (statusRes.status() == 200) {
                var statusObj = JsonTree.asObject(JsonTree.parse(statusRes.body()));
                var webObj = JsonTree.object(statusObj, "web");
                if (webObj != null && webObj.containsKey("lan")) {
                    lanOn = Boolean.TRUE.equals(webObj.get("lan"));
                }
            }

            renderDevices(response.body(), out, locale, lanOn);
            if (interactive && context.isInteractiveTerminal()) {
                var parsed = JsonTree.parse(response.body());
                if (parsed instanceof List<?> list && !list.isEmpty()) {
                    handleInteractive(runtime, out, list);
                }
            }
        }

        private void handleInteractive(RuntimeClient runtime, PrintWriter out, List<?> list) {
            var locale = context.clientEnvironment().locale();
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                var prompt = MessageCatalogue.format(locale, "operator.devices.act_prompt",
                                    Map.of("range", "1-" + list.size()))
                                    + MessageCatalogue.format(locale, "operator.devices.act_options",
                                    Map.of("others", MessageCatalogue.format(locale,
                                                                            "operator.devices.others", Map.of()),
                                                        "listener_off", MessageCatalogue.format(locale,
                                                                            "operator.devices.listener_off", Map.of()),
                                                        "leave", MessageCatalogue.format(locale,
                                                                            "operator.devices.leave", Map.of())));
                var line = reader.readLine(prompt);
                if (line != null) {
                    processInteractiveChoice(runtime, out, reader, list, line.trim());
                }
            }
            // cui-rewrite:disable InvalidExceptionUsageRecipe
            catch (IOException | RuntimeException _) {
                // Ignore interactive terminal read exception
            }
        }

        private void processInteractiveChoice(RuntimeClient runtime, PrintWriter out, LineReader reader, List<?> list, String choice) throws IOException {
            if ("q".equalsIgnoreCase(choice) || "leave".equalsIgnoreCase(choice) || choice.isEmpty()) {
                return;
            }
            if ("o".equalsIgnoreCase(choice)) {
                this.others = true;
                handleRevoke(runtime, out);
                return;
            }
            if ("n".equalsIgnoreCase(choice)) {
                handleListenerOff(runtime, out);
                return;
            }
            processDeviceIndexChoice(runtime, out, reader, list, choice);
        }

        private void processDeviceIndexChoice(RuntimeClient runtime, PrintWriter out, LineReader reader, List<?> list, String choice) throws IOException {
            var locale = context.clientEnvironment().locale();
            int num;
            try {
                num = Integer.parseInt(choice);
            } catch (NumberFormatException _) {
                return;
            }
            if (num < 1 || num > list.size()) {
                return;
            }
            var item = list.get(num - 1);
            var devInfo = extractDeviceInfo(item, num);
            out.println(MessageCatalogue.format(locale, "operator.devices.selected",
                                Map.of(INDEX, num, LABEL, devInfo.label())));
            out.flush();
            var actLine = reader.readLine(MessageCatalogue.format(locale,
                                "operator.devices.rename_or_revoke_prompt", Map.of()));
            if (actLine == null) {
                return;
            }
            var act = actLine.trim();
            if ("r".equalsIgnoreCase(act) || "rename".equalsIgnoreCase(act)) {
                var newLbl = reader.readLine(MessageCatalogue.format(locale, "operator.devices.label_input",
                                    Map.of(LABEL, devInfo.label())));
                if (newLbl != null && !newLbl.trim().isEmpty()) {
                    this.deviceId = devInfo.id();
                    this.newLabel = newLbl.trim();
                    handleRename(runtime, out);
                }
            } else if ("x".equalsIgnoreCase(act) || "revoke".equalsIgnoreCase(act)) {
                this.deviceId = devInfo.id();
                handleRevoke(runtime, out);
            }
        }

        private record DeviceInfo(String id, String label) {
        }

        private static DeviceInfo extractDeviceInfo(Object item, int fallbackIdx) {
            String label = "device " + fallbackIdx;
            String id = String.valueOf(fallbackIdx);
            if (item instanceof Map<?, ?> map) {
                var rawLabel = map.get(LABEL);
                if (rawLabel != null) {
                    label = TerminalSanitizer.sanitizeLine(rawLabel.toString());
                }
                var idObj = map.get("device_id");
                if (idObj != null) {
                    id = idObj.toString();
                }
            }
            return new DeviceInfo(id, label);
        }

        private static void renderDevices(byte[] body, PrintWriter out, String locale, boolean lanOn) throws IOException {
            var parsed = JsonTree.parse(body);
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                out.println(MessageCatalogue.format(locale, "devices.list.title", Map.of())
                                    + " \u00b7 "
                                    + MessageCatalogue.format(locale, "devices.list.subtitle", Map.of()));
                int idx = 1;
                for (var item : list) {
                    printDevice(out, idx++, item, locale);
                }
                out.println();
                out.println(MessageCatalogue.format(locale, "operator.devices.console_not_listed",
                                    Map.of("state", lanOn ? "on" : "off")));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.devices.none", Map.of()));
            }
        }

        private static void printDevice(PrintWriter out, int idx, Object item, String locale) {
            if (item instanceof Map<?, ?> dev) {
                var rawLabel = dev.get(LABEL);
                var labelVal = rawLabel != null ? TerminalSanitizer.sanitizeLine(rawLabel.toString()) : "browser";
                var pairedFrom = dev.get("paired_from");
                var fromStr = "network";
                if (LOOPBACK.equals(pairedFrom)) {
                    fromStr = LOOPBACK;
                } else if (pairedFrom != null) {
                    fromStr = TerminalSanitizer.sanitizeLine(pairedFrom.toString());
                }
                var pairedDate = formatPairedDate(dev.get("created_at"));
                var lastUsed = formatLastUsed(dev.get("last_used_at"));

                out.println(MessageCatalogue.format(locale, "operator.devices.row",
                                    Map.of(INDEX, idx, LABEL, labelVal, "date", pairedDate,
                                                        "from", fromStr, "last_used", lastUsed)));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.devices.row_plain",
                                    Map.of(INDEX, idx, LABEL, TerminalSanitizer.sanitizeLine(String.valueOf(item)))));
            }
        }

        private static String formatPairedDate(@Nullable Object raw) {
            if (raw == null) {
                return "recently";
            }
            var s = raw.toString();
            try {
                var instant = Instant.parse(s);
                return DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
                                    .withZone(ZoneId.of("UTC"))
                                    .format(instant);
            } catch (DateTimeParseException _) {
                return s;
            }
        }

        private static String formatLastUsed(@Nullable Object raw) {
            if (raw == null) {
                return "just now";
            }
            var s = raw.toString();
            try {
                var instant = Instant.parse(s);
                var now = Instant.now();
                var diff = Duration.between(instant, now);
                var days = diff.toDays();
                var hours = diff.toHours();
                if (days == 0 && hours < 24) {
                    var timeStr = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
                                        .withZone(ZoneId.of("UTC"))
                                        .format(instant);
                    return "today " + timeStr;
                }
                if (days < 7) {
                    return days + " d ago";
                }
                var weeks = days / 7;
                if (weeks < 8) {
                    return weeks + (weeks == 1 ? " week ago" : " weeks ago");
                }
                return DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)
                                    .withZone(ZoneId.of("UTC"))
                                    .format(instant);
            } catch (DateTimeParseException _) {
                return s;
            }
        }

        private boolean confirm() {
            try (var terminal = context.createTerminal()) {
                var reader = LineReaderBuilder.builder().terminal(terminal).build();
                var answer = reader.readLine(MessageCatalogue.format(context.clientEnvironment().locale(),
                                    "operator.devices.revoke_prompt", Map.of()));
                return answer == null || (!"b".equalsIgnoreCase(answer.trim()) && !"back".equalsIgnoreCase(answer.trim()));
            }
            // cui-rewrite:disable InvalidExceptionUsageRecipe
            catch (IOException | RuntimeException _) {
                return true;
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
        out.println(MessageCatalogue.format(context.clientEnvironment().locale(), "operator.web.status_line",
                            Map.of("state", describe(JsonTree.asObject(JsonTree.parse(response.body()))))));
        out.flush();
        return 0;
    }

    /**
     * Reports the LAN listener's URL and certificate fingerprint after it was opened, so the
     * operator can compare the fingerprint with the browser's certificate details
     * (03-web-server.adoc#web-listener).
     *
     * @param context      the operator context
     * @param spec         the command spec, for its output
     * @param loopbackPort the loopback port that stays open beside the LAN listener
     */
    static void printLanDetails(OperatorContext context, CommandSpec spec, int loopbackPort) {
        var out = spec.commandLine().getOut();
        var locale = context.clientEnvironment().locale();
        try {
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/status/connection"));
            if (response.status() != 200) {
                return;
            }
            var json = JsonTree.asObject(JsonTree.parse(response.body()));
            if (json == null) {
                return;
            }
            var lanUrl = JsonTree.string(json, "lan_url");
            if (lanUrl == null) {
                return;
            }
            out.println("  " + MessageCatalogue.format(locale, "entry.listener.lan_open", Map.of(
                                "url", lanUrl,
                                LOOPBACK, "http://127.0.0.1:" + loopbackPort)));
            var fingerprint = JsonTree.string(json, "lan_fingerprint");
            if (fingerprint != null) {
                out.println("  " + MessageCatalogue.format(locale, "entry.listener.fingerprint", Map.of())
                                    + "  " + fingerprint);
            }
            out.flush();
        } catch (IOException _) {
            // best-effort fingerprint report
        }
    }

    /**
     * @param web the {@code web} object of the status, may be {@code null}
     * @return a readable line
     */
    static String describe(@Nullable Map<String, Object> web) {
        return describe("en", web);
    }

    static String describe(String locale, @Nullable Map<String, Object> web) {
        if (web == null) {
            return MessageCatalogue.format(locale, "operator.web.state.empty", Map.of());
        }
        if (!Boolean.TRUE.equals(web.get("enabled"))) {
            return MessageCatalogue.format(locale, "operator.web.state.disabled", Map.of());
        }
        return MessageCatalogue.format(locale, "operator.web.state.enabled", Map.of(
                            "mode", Boolean.TRUE.equals(web.get("lan")) ? "lan" : LOOPBACK,
                            "port", web.get("port"),
                            "open", Boolean.TRUE.equals(web.get("open")) ? "open" : "not open"));
    }
}
