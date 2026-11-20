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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.HarnessEnrolmentResource;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator enrol-harness <client>}: records one machine-wide harness enrolment
 * (CLI &amp; Security § Harness Enrolment; PM-EXEC-1).
 * <p>
 * It is an operator verb (PM-SEC-3): the {@link OperatorGate} refuses it outside an interactive
 * terminal or inside a coding-agent environment before the executable is probed. The verb shows
 * the executable path, the version the probe reports, and the SHA-256 digest side by side with any
 * recorded values, and records nothing before the operator confirms the displayed facts. A changed
 * digest is the re-baseline of a tampered or self-updated enrolment, never a silent overwrite.
 */
@Command(name = "enrol-harness",
                    description = "Enrols the headless CLI of a client profile after showing path, version "
                                        + "and SHA-256.")
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class EnrolHarnessCommand implements Callable<Integer> {

    private static final String PATH_HARNESSES = "/api/v1/harnesses/";
    private static final String SUFFIX_ENROL = "/enrol";
    private static final String ERR_HARNESS_VERSION_UNSUPPORTED = "harness_version_unsupported";
    private static final String KEY_REASON = "reason";

    private static final int LABEL_WIDTH = 12;
    private static final int VALUE_WIDTH = 66;

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", arity = "0..1",
                        description = "Harness client id: claude, opencode, codex, or antigravity")
    String client = "";

    @Option(names = {"-y", "--yes"},
                        description = "Record the displayed enrolment without the interactive confirmation")
    boolean yes;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    EnrolHarnessCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        var locale = context.clientEnvironment().locale();
        var safeClient = OutputSafety.stripControlSequences(client);

        if (!HarnessDiscovery.isSupported(client)) {
            err.println(MessageCatalogue.format(locale, "enrolment.harness.unsupported_client",
                                Map.of(HarnessEnrolmentResource.FIELD_CLIENT, safeClient)));
            err.flush();
            return 2;
        }

        HarnessDiscovery.Harness harness;
        try {
            harness = HarnessDiscovery.discover(client, context.environment());
        } catch (HarnessDiscovery.HarnessNotFoundException _) {
            err.println(MessageCatalogue.format(locale, "enrolment.harness.not_found",
                                Map.of(HarnessEnrolmentResource.FIELD_CLIENT, safeClient)));
            err.flush();
            return 1;
        } catch (HarnessDiscovery.HarnessProbeException e) {
            err.println(MessageCatalogue.format(locale, "enrolment.harness.probe_failed",
                                Map.of(HarnessEnrolmentResource.FIELD_CLIENT, safeClient,
                                                    KEY_REASON, OutputSafety.stripControlSequences(e.getMessage()))));
            err.flush();
            return 1;
        }

        var recorded = fetchRecorded(client);
        render(out, locale, safeClient, recorded, harness);

        if (!yes) {
            if (noInput) {
                out.println(MessageCatalogue.get(locale, "enrolment.harness.confirm"));
                out.println("  " + MessageCatalogue.get(locale, "enrolment.harness.confirm_required"));
                out.flush();
                return 3;
            }
            if (!confirm(locale)) {
                out.println(MessageCatalogue.get(locale, "enrolment.harness.declined"));
                out.flush();
                return 1;
            }
        }

        return sendEnrolment(out, err, locale, safeClient, recorded, harness);
    }

    private int sendEnrolment(PrintWriter out, PrintWriter err, String locale, String safeClient,
                        @Nullable HarnessEnrolmentResource recorded, HarnessDiscovery.Harness harness)
                        throws IOException {
        var requested = new HarnessEnrolmentResource(client, harness.executable().toString(),
                            harness.version(), harness.sha256Digest(),
                            HarnessEnrolmentResource.Status.DETECTED);
        var target = PATH_HARNESSES + OperatorUrlUtil.encodePathSegment(client) + SUFFIX_ENROL;
        var response = context.runtime(true).send(HttpRequest.json("POST", target, requested.toJson()));

        if (response.status() == 200 || response.status() == 201) {
            printEnrolmentResult(out, locale, safeClient, harness,
                                recordedStatus(recorded, harness) == HarnessEnrolmentResource.Status.UPDATED);
            return 0;
        }

        var reason = refusalReason(response.bodyText());
        if (response.status() == 400 || response.status() == 404 || response.status() == 409) {
            if (ERR_HARNESS_VERSION_UNSUPPORTED.equals(reason)) {
                err.println(MessageCatalogue.format(locale, "enrolment.harness.version_unsupported",
                                    Map.of(HarnessEnrolmentResource.FIELD_CLIENT, safeClient,
                                                        HarnessEnrolmentResource.FIELD_VERSION,
                                                        OutputSafety.stripControlSequences(harness.version()))));
            } else {
                err.println(MessageCatalogue.format(locale, "enrolment.harness.invalid",
                                    Map.of(HarnessEnrolmentResource.FIELD_CLIENT, safeClient,
                                                        KEY_REASON, OutputSafety.stripControlSequences(reason))));
            }
            err.flush();
            return 1;
        }
        throw new OperatorRefusedException("enrol-harness " + safeClient, response);
    }

    private static void printEnrolmentResult(PrintWriter out, String locale, String safeClient,
                        HarnessDiscovery.Harness harness, boolean updated) {
        var key = updated ? "enrolment.harness.updated" : "enrolment.harness.done";
        out.println(MessageCatalogue.format(locale, key, Map.of(
                            HarnessEnrolmentResource.FIELD_CLIENT, safeClient,
                            HarnessEnrolmentResource.FIELD_EXECUTABLE_PATH,
                            OutputSafety.stripControlSequences(harness.executable().toString()),
                            HarnessEnrolmentResource.FIELD_VERSION,
                            OutputSafety.stripControlSequences(harness.version()))));
        out.flush();
    }

    private @Nullable HarnessEnrolmentResource fetchRecorded(String clientId) throws IOException {
        var target = PATH_HARNESSES + OperatorUrlUtil.encodePathSegment(clientId);
        var response = context.runtime(true).send(HttpRequest.get(target));
        if (response.status() == 404) {
            return null;
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("enrol-harness " + OutputSafety.stripControlSequences(clientId),
                                response);
        }
        if (response.bodyText().isBlank()) {
            return null;
        }
        return HarnessEnrolmentResource.fromJson(response.bodyText());
    }

    private static HarnessEnrolmentResource.Status recordedStatus(
                        @Nullable HarnessEnrolmentResource recorded, HarnessDiscovery.Harness harness) {
        if (recorded != null && !recorded.sha256Digest().equals(harness.sha256Digest())) {
            return HarnessEnrolmentResource.Status.UPDATED;
        }
        return HarnessEnrolmentResource.Status.ENROLLED;
    }

    private static void render(PrintWriter out, String locale, String client,
                        @Nullable HarnessEnrolmentResource recorded, HarnessDiscovery.Harness harness) {
        out.println(MessageCatalogue.format(locale, "enrolment.harness.title",
                            Map.of(HarnessEnrolmentResource.FIELD_CLIENT, client)));
        var labels = List.of(
                            MessageCatalogue.get(locale, "enrolment.harness.metric"),
                            MessageCatalogue.get(locale, "enrolment.harness.path"),
                            MessageCatalogue.get(locale, "enrolment.harness.version"),
                            MessageCatalogue.get(locale, "enrolment.harness.sha256"));
        // One width from the localized labels, so a language with a longer word keeps the columns
        // apart; Java's format width is only a minimum, never a separator.
        var labelWidth = Math.max(LABEL_WIDTH, labels.stream().mapToInt(String::length).max().orElse(LABEL_WIDTH) + 2);
        var rowFormat = "%-" + labelWidth + "s%-" + VALUE_WIDTH + "s%s%n";
        out.printf(Locale.ROOT, rowFormat,
                            MessageCatalogue.get(locale, "enrolment.harness.metric"),
                            MessageCatalogue.get(locale, "enrolment.harness.recorded"),
                            MessageCatalogue.get(locale, "enrolment.harness.detected"));
        out.println("-".repeat(labelWidth + 2 * VALUE_WIDTH));

        var notRecorded = MessageCatalogue.get(locale, "enrolment.harness.not_recorded");
        var recordedPath = recorded != null ? recorded.executablePath() : notRecorded;
        var recordedVersion = recorded != null ? recorded.version() : notRecorded;
        var recordedDigest = recorded != null ? recorded.sha256Digest() : notRecorded;

        out.printf(Locale.ROOT, rowFormat,
                            MessageCatalogue.get(locale, "enrolment.harness.path"),
                            fit(recordedPath), fit(harness.executable().toString()));
        out.printf(Locale.ROOT, rowFormat,
                            MessageCatalogue.get(locale, "enrolment.harness.version"),
                            fit(recordedVersion), fit(harness.version()));
        out.printf(Locale.ROOT, rowFormat,
                            MessageCatalogue.get(locale, "enrolment.harness.sha256"),
                            fit(recordedDigest), fit(harness.sha256Digest()));
        out.flush();
    }

    private static String fit(@Nullable String value) {
        var safe = OutputSafety.stripControlSequences(value == null ? "" : value);
        if (safe.length() <= VALUE_WIDTH) {
            return safe;
        }
        return safe.substring(0, VALUE_WIDTH - 1) + "…";
    }

    private boolean confirm(String locale) throws IOException {
        try (var lease = context.openTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
            String line;
            try {
                line = reader.readLine(MessageCatalogue.get(locale, "enrolment.harness.confirm") + " \u203a ");
            } catch (UserInterruptException _) {
                // The interrupt is the exit-code table's 130, not a declined confirmation.
                throw new StartPrompt.InterruptException();
            } catch (EndOfFileException _) {
                return false;
            }
            if (line == null) {
                return false;
            }
            var answer = line.trim().toLowerCase(Locale.ROOT);
            return answer.startsWith("y") || answer.startsWith("j");
        }
    }

    private static String refusalReason(String body) {
        try {
            var map = JsonTree.asObject(JsonTree.parse(body));
            var reason = JsonTree.string(map, KEY_REASON);
            if (reason == null) {
                reason = JsonTree.string(map, "error");
            }
            if (reason == null) {
                reason = JsonTree.string(map, "code");
            }
            return reason != null ? reason : "";
        } catch (IOException _) {
            return "";
        }
    }
}
