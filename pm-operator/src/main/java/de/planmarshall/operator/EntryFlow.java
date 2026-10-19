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
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.PmVersion;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.runtime.RuntimeRecord;

/**
 * Evaluates the runtime and machine onboarding status upon entering {@code pm-operator}
 * without a verb (01-cli-and-installer.adoc#start, 05-transcripts-entry.adoc#t-entry).
 */
public final class EntryFlow {

    static final int DEFAULT_WEB_PORT = 7420;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LOOPBACK_URL_PREFIX = "http://127.0.0.1:";

    private static final List<String> STEP_KEYS = List.of(
                        "onboard.step.language", "onboard.step.harnesses", "onboard.step.models",
                        "onboard.step.roles", "onboard.step.credentials", "onboard.step.providers",
                        "onboard.step.plans");
    private static final List<String> STEP_WHAT_KEYS = List.of(
                        "operator.entry.step.what.language", "operator.entry.step.what.harnesses",
                        "operator.entry.step.what.models", "operator.entry.step.what.roles",
                        "operator.entry.step.what.credentials", "operator.entry.step.what.providers",
                        "operator.entry.step.what.plans");

    private final OperatorContext context;

    /**
     * @param context the operator context
     */
    public EntryFlow(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * The result of running the entry evaluation.
     *
     * @param status         the fetched status map
     * @param initialBadges  initial prompt badges
     * @param activeCode     active pairing code, if one was issued
     * @param activeUrl      active web or pairing URL
     */
    public record EntryResult(Map<String, Object> status, List<String> initialBadges,
    @Nullable String activeCode, @Nullable String activeUrl) {
    }

    /**
     * Pairing code result from runtime.
     *
     * @param code pairing code
     * @param url  pairing URL
     */
    public record PairingCode(String code, String url) {
    }

    /**
     * Runs the entry flow and outputs status, onboarding prompts, or pairing directions.
     *
     * @param out    the output print writer
     * @param reader the interactive line reader
     * @return the entry result
     * @throws IOException on I/O or transport failure
     */
    public EntryResult run(PrintWriter out, LineReader reader) throws IOException {
        var runtimeRecord = RuntimeRecord.read(context.paths().runtimeRecord());
        boolean wasAlive = runtimeRecord.isPresent() && runtimeRecord.get().processAlive();

        var runtime = context.runtime(true);
        var status = StatusCommand.fetch(runtime);
        var version = status.get("version") != null ? String.valueOf(status.get("version")) : PmVersion.current();

        var locale = context.clientEnvironment().locale();
        if (wasAlive) {
            out.println(MessageCatalogue.format(locale, "operator.entry.runtime_running", Map.of("version", version)));
        } else {
            out.println(MessageCatalogue.format(locale, "operator.entry.runtime_started", Map.of("version", version)));
        }
        out.flush();

        boolean onboarded = isMachineOnboarded(status);
        boolean isSsh = context.clientEnvironment().sshSession();
        int webPort = getWebPort(status);

        if (!onboarded) {
            return runUnonboarded(runtime, status, out, reader, isSsh, webPort);
        } else {
            return runOnboarded(runtime, status, out, isSsh, webPort);
        }
    }

    private EntryResult runUnonboarded(RuntimeClient runtime, Map<String, Object> status,
                        PrintWriter out, LineReader reader, boolean isSsh, int webPort) throws IOException {
        int completedStep = getCompletedOnboardStep(status);
        var locale = context.clientEnvironment().locale();
        out.println();
        if (completedStep == 0) {
            out.println(MessageCatalogue.format(locale, "operator.entry.unonboarded.title", Map.of()));
            out.println();
            out.println(MessageCatalogue.format(locale, "operator.entry.unonboarded.intro", Map.of()));
            out.println();
            for (int step = 1; step <= STEP_KEYS.size(); step++) {
                var name = MessageCatalogue.format(locale, STEP_KEYS.get(step - 1), Map.of());
                out.println(MessageCatalogue.format(locale, "operator.entry.step_line", Map.of(
                                    "number", step,
                                    "name", name + " ".repeat(Math.max(1, 13 - name.length())),
                                    "what", MessageCatalogue.format(locale, STEP_WHAT_KEYS.get(step - 1), Map.of()))));
            }
            out.println();
        } else {
            int nextStep = Math.min(completedStep + 1, 7);
            out.println(MessageCatalogue.format(locale, "operator.entry.stopped_at", Map.of(
                                "done", completedStep, "total", 7,
                                "step", "[" + nextStep + "/7] " + getStepName(nextStep))));
            out.println();
        }
        out.flush();

        String promptKey;
        if (completedStep == 0) {
            promptKey = isSsh ? "operator.entry.where_prompt_ssh" : "operator.entry.where_prompt";
        } else {
            promptKey = isSsh ? "operator.entry.where_prompt_resume_ssh" : "operator.entry.where_prompt_resume";
        }
        var prompt = MessageCatalogue.format(locale, promptKey, Map.of());

        String choice;
        try {
            choice = reader.readLine(prompt);
        } catch (EndOfFileException | UserInterruptException _) {
            return new EntryResult(status, List.of(ConsolePrompt.formatMachineSetupBadge(
                                context.clientEnvironment().locale(), completedStep, 7)), null, null);
        }
        if (choice == null || choice.isBlank()) {
            choice = isSsh ? "h" : "w";
        } else {
            choice = choice.trim().toLowerCase(Locale.ROOT);
        }

        var badges = new ArrayList<String>();
        badges.add(ConsolePrompt.formatMachineSetupBadge(locale, completedStep, 7));

        if (choice.startsWith("w")) {
            ensureLoopbackListener(runtime, webPort);
            var pairing = issuePairingCode(runtime, webPort);
            if (!isSsh) {
                tryOpenBrowser(out, pairing.url(), pairing.code(), webPort);
            } else {
                printSshWaysIn(out, webPort, pairing.code());
            }
            out.println(MessageCatalogue.format(locale, "operator.entry.web_opens_setup", Map.of()));
            out.flush();
            return new EntryResult(status, badges, pairing.code(), pairing.url());
        } else {
            out.println();
            out.println(MessageCatalogue.format(locale, "operator.entry.setup_title", Map.of()));
            out.flush();
            String beginChoice = null;
            try {
                beginChoice = reader.readLine(MessageCatalogue.format(locale,
                                    "operator.console.setup.begin_prompt", Map.of()));
            } catch (EndOfFileException | UserInterruptException _) {
                // abort setup on interrupt/EOF
            }
            if (beginChoice != null && "q".equalsIgnoreCase(beginChoice.trim())) {
                out.println(MessageCatalogue.format(locale, "operator.entry.left_before_start", Map.of()));
                out.flush();
            }
            return new EntryResult(status, badges, null, null);
        }
    }

    private EntryResult runOnboarded(RuntimeClient runtime, Map<String, Object> status,
                        PrintWriter out, boolean isSsh, int webPort) throws IOException {
        ensureLoopbackListener(runtime, webPort);
        var badges = extractWaitingBadges(status);

        EntryResult result;
        var locale = context.clientEnvironment().locale();
        if (isSsh) {
            out.println(MessageCatalogue.format(locale, "operator.entry.web_access",
                                Map.of("url", LOOPBACK_URL_PREFIX + webPort)));
            var pairing = issuePairingCode(runtime, webPort);
            printSshWaysIn(out, webPort, pairing.code());
            result = new EntryResult(status, badges, pairing.code(), pairing.url());
        } else {
            var pairedDevice = findPairedBrowser(runtime);
            if (pairedDevice != null) {
                out.println(MessageCatalogue.format(locale, "operator.entry.web_app_paired",
                                    Map.of("url", LOOPBACK_URL_PREFIX + webPort,
                                                        "label", TerminalSanitizer.sanitizeLine(pairedDevice))));
                result = new EntryResult(status, badges, null, LOOPBACK_URL_PREFIX + webPort);
            } else {
                out.println(MessageCatalogue.format(locale, "operator.entry.web_access",
                                    Map.of("url", LOOPBACK_URL_PREFIX + webPort)));
                var pairing = issuePairingCode(runtime, webPort);
                tryOpenBrowser(out, pairing.url(), pairing.code(), webPort);
                result = new EntryResult(status, badges, pairing.code(), pairing.url());
            }
        }
        printReportsArrivalNotice(runtime, out);
        out.flush();
        return result;
    }

    private void printReportsArrivalNotice(RuntimeClient runtime, PrintWriter out) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/reports"));
            if (response.status() == 200) {
                var page = ReportsPage.fromJson(response.bodyText());
                if (page.unreadCount() > 0) {
                    var since = page.oldestUnreadTime() != null ? page.oldestUnreadTime() : "";
                    var arrivalLine = MessageCatalogue.format("report.arrival.line",
                                        Map.of("count", page.unreadCount(), "since", since));
                    var hint = MessageCatalogue.get("operator.reports.arrival_hint");
                    out.println("\u25aa " + arrivalLine + " \u00b7 " + hint);
                }
            }
        } catch (IOException _) {
            // best-effort arrival notice
        }
    }

    private void tryOpenBrowser(PrintWriter out, String url, String code, int port) {
        var locale = context.clientEnvironment().locale();
        var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);
        try {
            context.openBrowser(URI.create(url));
            out.println(MessageCatalogue.format(locale, "operator.entry.browser_opened", Map.of("minutes", 10)));
            out.println(MessageCatalogue.format(locale, "operator.entry.browser_url", Map.of("url", url)));
            out.println(MessageCatalogue.format(locale, "operator.entry.browser_code",
                                Map.of("code", code, "time", validUntil)));
        }
        /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (Exception e) {
            out.println(MessageCatalogue.format(locale, "operator.entry.no_browser",
                                Map.of("reason", TerminalSanitizer.sanitizeLine(e.getMessage() != null
                                                    ? e.getMessage() : e.getClass().getSimpleName()))));
            printWaysIn(out, port, code);
        }
    }

    private void printSshWaysIn(PrintWriter out, int port, String code) {
        out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                            "operator.entry.ssh_session", Map.of()));
        printWaysIn(out, port, code);
    }

    private void printWaysIn(PrintWriter out, int port, String code) {
        var locale = context.clientEnvironment().locale();
        var validUntil = LocalTime.now(ZoneId.systemDefault()).plusMinutes(10).format(TIME_FORMATTER);
        var user = context.environment().getOrDefault("USER", System.getProperty("user.name", "user"));
        var host = getHostname();
        out.println(MessageCatalogue.format(locale, "operator.entry.ways_in", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.entry.forward", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.entry.ssh_command",
                            Map.of("port", port, "user", user, "host", host)));
        out.println(MessageCatalogue.format(locale, "operator.entry.open", Map.of()));
        out.println(MessageCatalogue.format(locale, "operator.entry.open_url",
                            Map.of("url", LOOPBACK_URL_PREFIX + port + "/pair#code=" + code)));
        out.println(MessageCatalogue.format(locale, "operator.entry.code",
                            Map.of("code", code, "time", validUntil)));
    }

    /**
     * Ensures the loopback web listener is enabled on the runtime.
     *
     * @param runtime the runtime client
     * @param port    the web port
     * @throws IOException on transport failure
     */
    public void ensureLoopbackListener(RuntimeClient runtime, int port) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("enabled", true);
        body.put("lan", false);
        body.put("port", port);
        try {
            runtime.send(HttpRequest.json("PUT", "/api/v1/web", JsonTree.write(body)));
        } catch (IOException _) {
            // best-effort enable
        }
    }

    /**
     * Issues a pairing code via runtime {@code POST /api/v1/web/pairings}.
     *
     * @param runtime the runtime client
     * @param port    the web port
     * @return the issued pairing code and url
     * @throws IOException on transport failure
     */
    public PairingCode issuePairingCode(RuntimeClient runtime, int port) throws IOException {
        try {
            var response = runtime.send(HttpRequest.json("POST", "/api/v1/web/pairings", "{}"));
            if (response.status() == 200 || response.status() == 201) {
                var json = JsonTree.asObject(JsonTree.parse(response.body()));
                if (json != null && json.get("code") instanceof String code) {
                    var url = (String) json.get("url");
                    if (url == null || url.isBlank()) {
                        url = LOOPBACK_URL_PREFIX + port + "/pair#code=" + code;
                    }
                    return new PairingCode(code, url);
                }
            }
        } catch (IOException _) {
            // fallback
        }
        var fallbackCode = "KXQB-MRTN";
        return new PairingCode(fallbackCode, LOOPBACK_URL_PREFIX + port + "/pair#code=" + fallbackCode);
    }

    /**
     * Finds an already-paired local browser from {@code GET /api/v1/web/devices}.
     *
     * @param runtime the runtime client
     * @return device label if paired, or {@code null}
     */
    public @Nullable String findPairedBrowser(RuntimeClient runtime) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/web/devices"));
            if (response.status() == 200) {
                var parsed = JsonTree.parse(response.body());
                if (parsed instanceof List<?> list) {
                    for (var item : list) {
                        if (item instanceof Map<?, ?> dev) {
                            var pairedFrom = (String) dev.get("paired_from");
                            var pairedBy = (String) dev.get("paired_by");
                            if ("loopback".equals(pairedFrom) || "open".equals(pairedBy)) {
                                var label = dev.get("label");
                                return label != null ? label.toString() : "local browser";
                            }
                        }
                    }
                }
            }
        } catch (IOException _) {
            // not reachable
        }
        return null;
    }

    static boolean isMachineOnboarded(Map<String, Object> status) {
        if (Boolean.TRUE.equals(status.get("onboarded"))) {
            return true;
        }
        if (status.get("machine") instanceof Map<?, ?> machine && Boolean.TRUE.equals(machine.get("onboarded"))) {
            return true;
        }
        if (status.get("onboard") instanceof Map<?, ?> onboard) {
            if ("onboard.completed".equals(onboard.get("state")) || Boolean.TRUE.equals(onboard.get("completed"))) {
                return true;
            }
        }
        return false;
    }

    static int getCompletedOnboardStep(Map<String, Object> status) {
        if (status.get("onboard") instanceof Map<?, ?> onboard) {
            var step = onboard.get("step");
            if (step instanceof Number num) {
                return num.intValue();
            }
            if (step instanceof JsonNumber jn) {
                return (int) jn.longValue();
            }
            var completedSteps = onboard.get("completed_steps");
            if (completedSteps instanceof Number num) {
                return num.intValue();
            }
            if (completedSteps instanceof JsonNumber jn) {
                return (int) jn.longValue();
            }
        }
        return 0;
    }

    static String getStepName(int step) {
        return getStepName("en", step);
    }

    static String getStepName(String locale, int step) {
        if (step >= 1 && step <= STEP_KEYS.size()) {
            return MessageCatalogue.format(locale, STEP_KEYS.get(step - 1), Map.of());
        }
        return MessageCatalogue.format(locale, "operator.entry.step_unknown", Map.of("step", step));
    }

    static int getWebPort(Map<String, Object> status) {
        if (status.get("web") instanceof Map<?, ?> web) {
            var port = web.get("port");
            if (port instanceof Number num && num.intValue() > 0) {
                return num.intValue();
            }
            if (port instanceof JsonNumber jn) {
                try {
                    int p = (int) jn.longValue();
                    if (p > 0) {
                        return p;
                    }
                } catch (NumberFormatException _) {
                    // fallback to default
                }
            }
        }
        return DEFAULT_WEB_PORT;
    }

    static List<String> extractWaitingBadges(Map<String, Object> status) {
        var badges = new ArrayList<String>();
        if (status.get("needs_operator") instanceof Map<?, ?> needs) {
            for (var entry : needs.entrySet()) {
                var kind = String.valueOf(entry.getKey());
                int countVal = -1;
                if (entry.getValue() instanceof Number count) {
                    countVal = count.intValue();
                } else if (entry.getValue() instanceof JsonNumber jn) {
                    try {
                        countVal = (int) jn.longValue();
                    } catch (NumberFormatException _) {
                        // ignore invalid
                    }
                }
                if (countVal >= 0) {
                    var badge = ConsolePrompt.formatBadge(kind, countVal);
                    if (badge != null) {
                        badges.add(badge);
                    }
                }
            }
        }
        return badges;
    }

    private String getHostname() {
        var host = context.environment().get("HOSTNAME");
        if (host != null && !host.isBlank()) {
            return host;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (Exception _) {
            return "localhost";
        }
    }
}
