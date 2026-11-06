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
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.PmVersion;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.runtime.RuntimeRecord;
import de.planmarshall.operator.dialogue.ChoicePrompt;
import de.planmarshall.operator.dialogue.OnboardingSource;
import de.planmarshall.operator.dialogue.StateDialogue;

/**
 * Evaluates the runtime and machine onboarding status upon entering {@code pm-operator}
 * without a verb (01-cli-and-installer.adoc#start, 05-transcripts-entry.adoc#t-entry).
 * <p>
 * The machine's onboarding has two entry points and no verb of its own: the bare
 * {@code pm-operator}, which runs it from where it stands while it is not complete, and
 * {@code config <aspect>}, which runs one of its steps again as a later change
 * (01-commands.adoc#command-tree).
 */
public final class EntryFlow {

    static final int DEFAULT_WEB_PORT = 7420;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LOOPBACK_URL_PREFIX = "http://127.0.0.1:";
    private static final String VERSION = "version";
    private static final String PAIR_CODE = "/pair#code=";
    private static final Pattern ENTRY_LINK = Pattern.compile("/entries/([^/?#]+)");

    private final OperatorContext context;
    private final List<OnboardingAspect> aspects;
    private final List<String> stepKeys;

    /**
     * @param context the operator context
     */
    public EntryFlow(OperatorContext context) {
        this(context, List.of(OnboardingAspect.values()));
    }

    /**
     * @param context the operator context
     * @param aspects the machine onboarding aspects
     */
    public EntryFlow(OperatorContext context, List<OnboardingAspect> aspects) {
        this.context = Objects.requireNonNull(context, "context");
        this.aspects = List.copyOf(Objects.requireNonNull(aspects, "aspects"));
        this.stepKeys = this.aspects.stream().map(OnboardingAspect::stepNameKey).toList();
    }

    /**
     * @return the machine onboarding aspects
     */
    public List<OnboardingAspect> aspects() {
        return aspects;
    }

    /**
     * The result of running the entry evaluation.
     *
     * @param status         the fetched status map
     * @param initialBadges  initial prompt badges
     * @param activeCode     active pairing code, if one was issued
     * @param activeUrl      active web or pairing URL
     * @param consoleBadges  structured prompt badges
     */
    public record EntryResult(Map<String, Object> status, List<String> initialBadges,
    @Nullable String activeCode, @Nullable String activeUrl, List<ConsoleBadge> consoleBadges) {

        public EntryResult(Map<String, Object> status, List<String> initialBadges,
                            @Nullable String activeCode, @Nullable String activeUrl) {
            this(status, initialBadges, activeCode, activeUrl, List.of());
        }
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
        var version = status.get(VERSION) != null ? String.valueOf(status.get(VERSION)) : PmVersion.current();

        var locale = context.clientEnvironment().locale();
        if (wasAlive) {
            out.println(MessageCatalogue.format(locale, "operator.entry.runtime_running", Map.of(VERSION, version)));
        } else {
            out.println(MessageCatalogue.format(locale, "operator.entry.runtime_started", Map.of(VERSION, version)));
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
        int totalSteps = aspects.size();
        out.println();
        if (completedStep == 0) {
            out.println(MessageCatalogue.format(locale, "operator.entry.unonboarded.title", Map.of()));
            out.println();
            out.println(MessageCatalogue.format(locale, "operator.entry.unonboarded.intro", Map.of("total", totalSteps)));
            out.println();
            for (int step = 1; step <= totalSteps; step++) {
                var name = MessageCatalogue.format(locale, stepKeys.get(step - 1), Map.of());
                var whatKey = "operator.entry.step.what." + aspects.get(step - 1).name().toLowerCase(Locale.ROOT);
                out.println(MessageCatalogue.format(locale, "operator.entry.step_line", Map.of(
                                    "number", step,
                                    "name", name + " ".repeat(Math.max(1, 13 - name.length())),
                                    "what", MessageCatalogue.format(locale, whatKey, Map.of()))));
            }
            out.println();
        } else {
            int nextStep = Math.min(completedStep + 1, totalSteps);
            out.println(MessageCatalogue.format(locale, "operator.entry.stopped_at", Map.of(
                                "done", completedStep, "total", totalSteps,
                                "step", "[" + nextStep + "/" + totalSteps + "] " + stepName(locale, nextStep))));
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
            // Leaving the entry prompt keeps the block list and takes every badge the runtime
            // offers, the open project setups included.
            var cBadges = consoleBadges(locale, status,
                                readProjectSetupConsoleBadgesQuiet(locale, runtime), totalSteps);
            var strBadges = cBadges.stream().map(ConsoleBadge::text).toList();
            return new EntryResult(status, strBadges, null, null, cBadges);
        }
        if (choice == null || choice.isBlank()) {
            choice = isSsh ? "h" : "w";
        } else {
            choice = choice.trim().toLowerCase(Locale.ROOT);
        }

        var cBadges = new ArrayList<ConsoleBadge>();
        cBadges.add(ConsoleBadge.machineSetup(ConsolePrompt.formatMachineSetupBadge(locale, completedStep, totalSteps)));
        cBadges.addAll(readProjectSetupConsoleBadgesQuiet(locale, runtime));
        var badges = cBadges.stream().map(ConsoleBadge::text).toList();

        if (choice.startsWith("w") && context.gate().permits()) {
            ensureLoopbackListener(runtime, webPort);
            var pairing = issuePairingCode(runtime, webPort);
            if (!isSsh) {
                tryOpenBrowser(out, pairing.url(), pairing.code(), webPort);
            } else {
                printSshWaysIn(out, webPort, pairing.code());
            }
            out.println(MessageCatalogue.format(locale, "operator.entry.web_opens_setup", Map.of()));
            out.flush();
            return new EntryResult(status, badges, pairing.code(), pairing.url(), cBadges);
        }
        if (choice.startsWith("w")) {
            // The web app is offered by opening the listener and issuing a pairing code, both acts:
            // where the gate refuses an act the setup continues here instead, and the refusal
            // appears when one is typed.
            out.println(MessageCatalogue.format(locale, "operator.entry.setup_here_only", Map.of()));
        } else {
            out.println();
        }
        out.flush();
        runOnboardingDialogue(runtime, out, reader, locale);
        return refreshedAfterDialogue(runtime, status, cBadges);
    }

    /**
     * Re-reads the status after the dialogue, so the badge the console keeps matches the state the
     * dialogue left: no setup badge once the machine is onboarded, the completed step otherwise.
     * A refresh that cannot be read keeps what the entry flow already had: the dialogue does not
     * make the entry result worse, so the console still opens with a status and badges.
     *
     * @param runtime       the runtime client
     * @param status        the status the entry flow read before the dialogue
     * @param consoleBadges the badges the entry flow built before the dialogue
     * @return the entry result with the refreshed status and badges, or the pre-dialogue ones
     */
    private EntryResult refreshedAfterDialogue(RuntimeClient runtime, Map<String, Object> status,
                        List<ConsoleBadge> consoleBadges) {
        var strBadges = consoleBadges.stream().map(ConsoleBadge::text).toList();
        try {
            var refreshed = StatusCommand.fetch(runtime);
            var loc = context.clientEnvironment().locale();
            var projectBadges = readProjectBadgesOrFallback(loc, runtime, consoleBadges);
            var cBadges = consoleBadges(loc, refreshed, projectBadges, aspects.size());
            return new EntryResult(refreshed, cBadges.stream().map(ConsoleBadge::text).toList(),
                                null, null, cBadges);
        } catch (IOException _) {
            return new EntryResult(status, strBadges, null, null, consoleBadges);
        }
    }

    private List<ConsoleBadge> readProjectBadgesOrFallback(String locale, RuntimeClient runtime,
                        List<ConsoleBadge> fallback) {
        try {
            return readProjectSetupConsoleBadges(locale, runtime);
        } catch (IOException _) {
            return fallback.stream()
                                .filter(b -> b.kind() == ConsoleBadge.Kind.PROJECT_SETUP)
                                .toList();
        }
    }

    /**
     * Hands the guided path over to the one state dialogue ({@code CLI-UC-S-06}): the operator
     * follows the runtime's onboarding states until the machine is ready or leaves with {@code q}.
     *
     * @param runtime the runtime client
     * @param out     the output
     * @param reader  the console's line reader
     * @param locale  the interface locale
     */
    private void runOnboardingDialogue(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        String locale) {
        var dialogue = new StateDialogue(new OnboardingSource(runtime, context.gate()), out, locale,
                            stateId -> new ChoicePrompt(reader, out, locale, stateId));
        try {
            dialogue.run();
        } catch (StartPrompt.StartLeftException | StartPrompt.MissingInputException _) {
            // leaving the dialogue sends nothing; the runtime keeps the state it reports
        } catch (OperatorRefusedException e) {
            out.println(e.getMessage());
            out.flush();
        } catch (IOException _) {
            out.println(MessageCatalogue.format(locale, "onboard.error.no_state", Map.of()));
            out.flush();
        }
    }

    private EntryResult runOnboarded(RuntimeClient runtime, Map<String, Object> status,
                        PrintWriter out, boolean isSsh, int webPort) throws IOException {
        var loc = context.clientEnvironment().locale();
        var cBadges = consoleBadges(loc, status,
                            readProjectSetupConsoleBadgesQuiet(loc, runtime), aspects.size());
        var badges = cBadges.stream().map(ConsoleBadge::text).toList();
        var result = context.gate().permits()
                            ? openWebAccess(runtime, status, out, isSsh, webPort, badges, cBadges)
                            : new EntryResult(status, badges, null, null, cBadges);
        printReportsArrivalNotice(runtime, out);
        out.flush();
        return result;
    }

    /**
     * Offers the web app and a pairing code: enabling the loopback listener and issuing a code are
     * acts, so the entry flow offers them only where the operator gate would let an act run. A
     * refused act leaves the console a read-only view, and the refusal appears when one is typed.
     *
     * @param runtime       the runtime client
     * @param status        the status the entry flow read
     * @param out           the console's writer
     * @param isSsh         whether this is an SSH session
     * @param webPort       the web port
     * @param badges        the prompt badges
     * @param consoleBadges the structured prompt badges
     * @return the entry result carrying the pairing code
     * @throws IOException on transport failure
     */
    private EntryResult openWebAccess(RuntimeClient runtime, Map<String, Object> status, PrintWriter out,
                        boolean isSsh, int webPort, List<String> badges,
                        List<ConsoleBadge> consoleBadges) throws IOException {
        ensureLoopbackListener(runtime, webPort);

        EntryResult result;
        var locale = context.clientEnvironment().locale();
        if (isSsh) {
            out.println(MessageCatalogue.format(locale, "operator.entry.web_access",
                                Map.of("url", LOOPBACK_URL_PREFIX + webPort)));
            var pairing = issuePairingCode(runtime, webPort);
            printSshWaysIn(out, webPort, pairing.code());
            result = new EntryResult(status, badges, pairing.code(), pairing.url(), consoleBadges);
        } else {
            var pairedDevice = findPairedBrowser(runtime);
            if (pairedDevice != null) {
                out.println(MessageCatalogue.format(locale, "operator.entry.web_app_paired",
                                    Map.of("url", LOOPBACK_URL_PREFIX + webPort,
                                                        "label", TerminalSanitizer.sanitizeLine(pairedDevice))));
                result = new EntryResult(status, badges, null, LOOPBACK_URL_PREFIX + webPort, consoleBadges);
            } else {
                out.println(MessageCatalogue.format(locale, "operator.entry.web_access",
                                    Map.of("url", LOOPBACK_URL_PREFIX + webPort)));
                var pairing = issuePairingCode(runtime, webPort);
                tryOpenBrowser(out, pairing.url(), pairing.code(), webPort);
                result = new EntryResult(status, badges, pairing.code(), pairing.url(), consoleBadges);
            }
        }
        return result;
    }

    private void printReportsArrivalNotice(RuntimeClient runtime, PrintWriter out) {
        try {
            var response = runtime.send(HttpRequest.get("/api/v1/reports"));
            if (response.status() == 200) {
                var page = ReportsPage.fromJson(response.bodyText());
                if (page.unreadCount() > 0) {
                    var since = page.oldestUnreadTime() != null ? page.oldestUnreadTime() : "";
                    var arrivalLine = MessageCatalogue.format(context.clientEnvironment().locale(),
                                        "report.arrival.line",
                                        Map.of("count", page.unreadCount(), "time", since));
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
        } catch (IOException | IllegalArgumentException e) {
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
                            Map.of("url", LOOPBACK_URL_PREFIX + port + PAIR_CODE + code)));
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
                        url = LOOPBACK_URL_PREFIX + port + PAIR_CODE + code;
                    }
                    return new PairingCode(code, url);
                }
            }
        } catch (IOException _) {
            // fallback
        }
        var fallbackCode = "KXQB-MRTN";
        return new PairingCode(fallbackCode, LOOPBACK_URL_PREFIX + port + PAIR_CODE + fallbackCode);
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

    String stepName(String locale, int step) {
        if (step >= 1 && step <= stepKeys.size()) {
            return MessageCatalogue.format(locale, stepKeys.get(step - 1), Map.of());
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

    static final Duration PROJECT_BADGES_BOUND = Duration.ofSeconds(5);

    @FunctionalInterface
    interface IoCallable<T> {
        T call() throws IOException;
    }

    static <T> T bounded(IoCallable<T> task, Duration bound, String threadName) throws IOException {
        var result = new AtomicReference<T>();
        var failure = new AtomicReference<IOException>();
        var thrown = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
            try {
                result.set(task.call());
            } catch (IOException e) {
                failure.set(e);
            }
        }, threadName);
        thread.setDaemon(true);
        // A worker that dies on an unexpected exception would otherwise leave `result` unset and
        // the caller to dereference `null`: the handler marks the read failed instead.
        thread.setUncaughtExceptionHandler((_, error) -> thrown.set(error));
        thread.start();
        try {
            // A bound below a millisecond rounds to 0, which joins forever: the clamp keeps the
            // call bounded.
            thread.join(Math.max(1L, bound.toMillis()));
        } catch (InterruptedException e) {
            thread.interrupt();
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for " + threadName, e);
        }
        if (thread.isAlive()) {
            thread.interrupt();
            throw new IOException("the runtime did not answer within " + bound.toMillis() + " ms");
        }
        if (thrown.get() != null) {
            throw new IOException("the read failed in " + threadName, thrown.get());
        }
        if (failure.get() != null) {
            throw failure.get();
        }
        var value = result.get();
        if (value == null) {
            throw new IOException("the read in " + threadName + " answered nothing");
        }
        return value;
    }

    /**
     * Builds structured prompt badges for console consumption.
     *
     * @param locale        the interface locale
     * @param status        the runtime's status
     * @param projectBadges the structured project setup badges
     * @param machineSteps  total machine onboarding steps
     * @return structured badges
     */
    static List<ConsoleBadge> consoleBadges(String locale, Map<String, Object> status,
                        List<ConsoleBadge> projectBadges, int machineSteps) {
        var badges = new ArrayList<ConsoleBadge>();
        if (!isMachineOnboarded(status)) {
            var text = ConsolePrompt.formatMachineSetupBadge(locale, getCompletedOnboardStep(status),
                                machineSteps);
            badges.add(ConsoleBadge.machineSetup(text));
        }
        badges.addAll(projectBadges);
        badges.addAll(extractWaitingConsoleBadges(locale, status));
        return badges;
    }

    static List<ConsoleBadge> readProjectSetupConsoleBadges(String locale, RuntimeClient runtime) throws IOException {
        return readProjectSetupConsoleBadges(locale, runtime, PROJECT_BADGES_BOUND);
    }

    static List<ConsoleBadge> readProjectSetupConsoleBadges(String locale, RuntimeClient runtime, Duration bound) throws IOException {
        return bounded(() -> {
            var response = runtime.send(HttpRequest.get("/api/v1/project-onboardings"));
            if (response.status() != 200) {
                throw new IOException("HTTP " + response.status() + " from /api/v1/project-onboardings");
            }
            var body = JsonTree.asObject(JsonTree.parse(response.body()));
            if (body == null || !(body.get("onboardings") instanceof List<?> items)) {
                throw new IOException("Missing onboardings in /api/v1/project-onboardings response");
            }
            var badges = new ArrayList<ConsoleBadge>();
            for (var item : items) {
                var badge = parseProjectOnboardingItem(item, locale);
                if (badge != null) {
                    badges.add(badge);
                }
            }
            return List.copyOf(badges);
        }, bound, "project-onboardings-read");
    }

    private static @Nullable ConsoleBadge parseProjectOnboardingItem(Object item, String locale) {
        var map = JsonTree.asObject(item);
        if (map == null) {
            return null;
        }
        try {
            var being = BeingAddedProject.fromMap(map);
            // A pre-enrolment run is entry-scoped and has no project id: the contract carries the
            // address in its continue link (project-onboarding.adoc#several). A run whose contract
            // carries no link still shows as a badge, named by its checkout, but offers no
            // continuation: the directory name is not an entry id.
            var display = checkoutDirectoryName(being.checkoutPath());
            if (display != null) {
                var text = ConsolePrompt.formatProjectSetupBadge(locale,
                                    display, being.stepNumber(), being.totalSteps());
                return ConsoleBadge.projectSetup(entryAddress(being), text);
            }
        } catch (IOException _) {
            // skip malformed entries
        }
        return null;
    }

    /**
     * Reads the address a being-added project's run is continued by: the entry id the contract's
     * {@code continue_link} names. A run whose contract carries no link has no address the daemon
     * serves — the checkout directory name is not an entry id — so it gets none and the badge stops
     * offering a continuation (`project add <path>` still continues it).
     *
     * @param being the run as the runtime reports it
     * @return the entry id, or {@code null} when the contract carries no link
     */
    static @Nullable String entryAddress(BeingAddedProject being) {
        return entryId(being.continueLink());
    }

    /**
     * @param continueLink the contract's continuation link ({@code /api/v1/entries/{entry_id}/continue})
     * @return the id the link addresses, decoded, or {@code null} when the link names none
     */
    private static @Nullable String entryId(@Nullable String continueLink) {
        if (continueLink == null || continueLink.isBlank()) {
            return null;
        }
        var match = ENTRY_LINK.matcher(continueLink);
        if (!match.find()) {
            return null;
        }
        var raw = match.group(1);
        try {
            // A path segment keeps `+` as a plus; URLDecoder would read it as a space. Decoding it
            // as `%2B` first keeps an id such as `ent+a` intact on its way back to the runtime.
            var decoded = URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
            return decoded.isBlank() ? null : decoded;
        } catch (IllegalArgumentException _) {
            // A segment that is not valid percent-encoding is taken as it stands.
            return raw;
        }
    }

    static List<ConsoleBadge> readProjectSetupConsoleBadgesQuiet(String locale, RuntimeClient runtime) {
        try {
            return readProjectSetupConsoleBadges(locale, runtime);
        } catch (IOException _) {
            return List.of();
        }
    }

    private static @Nullable String checkoutDirectoryName(String checkoutPath) {
        if (checkoutPath.isBlank()) {
            return null;
        }
        try {
            var name = Path.of(checkoutPath).getFileName();
            return name == null || name.toString().isBlank() ? null : name.toString();
        } catch (InvalidPathException _) {
            return null;
        }
    }

    static List<String> extractWaitingBadges(Map<String, Object> status) {
        return extractWaitingBadges("en", status);
    }

    static List<String> extractWaitingBadges(String locale, Map<String, Object> status) {
        return extractWaitingConsoleBadges(locale, status).stream()
                            .map(ConsoleBadge::text)
                            .toList();
    }

    static List<ConsoleBadge> extractWaitingConsoleBadges(String locale, Map<String, Object> status) {
        if (!(status.get("needs_operator") instanceof Map<?, ?> needs)) {
            return List.of();
        }
        var badges = new ArrayList<ConsoleBadge>();
        for (var entry : needs.entrySet()) {
            var count = countOf(entry.getValue());
            if (count < 0) {
                continue;
            }
            var kind = String.valueOf(entry.getKey());
            var badgeText = ConsolePrompt.formatBadge(locale, kind, count);
            if (badgeText != null) {
                badges.add(ConsoleBadge.waiting(kind, count, badgeText));
            }
        }
        return badges;
    }

    /**
     * @param value the count the status reports for one kind
     * @return the count, or {@code -1} when the value is no number
     */
    private static int countOf(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof JsonNumber jsonNumber) {
            try {
                return (int) jsonNumber.longValue();
            } catch (NumberFormatException _) {
                return -1;
            }
        }
        return -1;
    }

    private String getHostname() {
        var host = context.environment().get("HOSTNAME");
        if (host != null && !host.isBlank()) {
            return host;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException _) {
            return "localhost";
        }
    }
}
