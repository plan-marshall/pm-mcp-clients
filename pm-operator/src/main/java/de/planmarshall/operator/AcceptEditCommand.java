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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.AcceptEditRequest;
import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.StoreDigestComparison;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator accept-edit <store-path>}: accepts a manual store edit, re-baselining its
 * digest (Store Schemas § Store Digest Verification Against Out-of-Band Edits; PM-IMPL-7).
 * <p>
 * Before it asks, it renders the store's path and, side by side, the recorded and the current size
 * and SHA-256. It restores nothing and shows no diff, because the prior content is not kept, and it
 * never sends the acceptance unless the operator confirms it (PM-SEC-3): the confirmation names the
 * SHA-256 the operator saw, and the runtime refuses the acceptance of any other digest.
 */
@Command(name = "accept-edit",
                    description = "Accepts a manual store edit after showing its recorded and current size and SHA-256.")
@OperatorVerb
public final class AcceptEditCommand implements Callable<Integer> {

    private static final String PREFIX_PROJECTS = "/api/v1/projects/";
    private static final String PATH_DIGEST = "/stores/digest?path=";
    private static final String PATH_ACCEPT_EDIT = "/stores/accept-edit";
    private static final String ERR_CONFIRMATION_MISMATCH = "confirmation_mismatch";

    private static final int LABEL_WIDTH = 12;
    private static final int RECORDED_WIDTH = 68;
    private static final int CURRENT_WIDTH = 64;
    private static final String SEPARATOR = "-".repeat(LABEL_WIDTH + RECORDED_WIDTH + CURRENT_WIDTH);

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Store path to accept, relative to its scope root")
    String storePath;

    @Option(names = "--project", description = "Project that owns the store")
    @Nullable
    String project;

    @Option(names = "--force", description = "Accept the edit without the interactive confirmation")
    boolean force;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    AcceptEditCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();
        var safeStore = OutputSafety.stripControlSequences(storePath);

        var projectId = resolveProjectId(project, storePath);
        if (projectId.isBlank()) {
            err.println("--project is required, or <store-path> must begin with <project_id>/");
            err.flush();
            return 3;
        }
        var relativeStore = relativeStore(project, storePath);
        var base = PREFIX_PROJECTS + OperatorUrlUtil.encodePathSegment(projectId);

        var digestResponse = context.runtime(true).send(
                            HttpRequest.get(base + PATH_DIGEST + OperatorUrlUtil.encodeQueryValue(relativeStore)));
        if (digestResponse.status() == 404) {
            err.println(MessageCatalogue.format("enrolment.accept.not_found", Map.of("path", safeStore)));
            err.flush();
            return 1;
        }
        if (digestResponse.status() != 200) {
            err.println(MessageCatalogue.format("enrolment.accept.failed",
                                Map.of("path", safeStore, "status", digestResponse.status())));
            err.flush();
            return 1;
        }

        var comparison = StoreDigestComparison.fromJson(digestResponse.bodyText());
        renderComparison(out, comparison);

        if (!comparison.mismatch()) {
            out.println(MessageCatalogue.get("enrolment.accept.match"));
            out.flush();
            return 0;
        }

        boolean accepted;
        if (force) {
            accepted = true;
        } else if (noInput) {
            out.println(MessageCatalogue.get("enrolment.accept.confirm"));
            out.println("  " + MessageCatalogue.get("enrolment.accept.confirm_required"));
            out.flush();
            return 3;
        } else {
            accepted = confirm();
        }
        if (!accepted) {
            out.println(MessageCatalogue.get("enrolment.accept.declined"));
            out.flush();
            return 1;
        }

        var request = new AcceptEditRequest(comparison.storePath(), comparison.currentSha256());
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", base + PATH_ACCEPT_EDIT, request.toJson()));
        if (response.status() == 200) {
            var result = AcceptEditResult.fromJson(response.bodyText());
            out.println(MessageCatalogue.format("enrolment.accept.done",
                                Map.of("sha", OutputSafety.stripControlSequences(result.newSha256()))));
            out.flush();
            return 0;
        }
        if (response.status() == 409) {
            err.println(MessageCatalogue.format("enrolment.accept.refused",
                                Map.of("reason", refusalReason(response.bodyText()))));
            err.flush();
            return 1;
        }
        throw new OperatorRefusedException("accept-edit " + safeStore, response);
    }

    private static void renderComparison(PrintWriter out, StoreDigestComparison comparison) {
        var path = OutputSafety.stripControlSequences(comparison.storePath());
        out.println(MessageCatalogue.format("enrolment.accept.title", Map.of("path", path)));

        var recordedSize = MessageCatalogue.format("enrolment.accept.bytes",
                            Map.of("size", formatSize(comparison.recordedSize())));
        var currentSize = MessageCatalogue.format("enrolment.accept.bytes",
                            Map.of("size", formatSize(comparison.currentSize())));
        var recordedSha = OutputSafety.stripControlSequences(comparison.recordedSha256());
        var currentSha = OutputSafety.stripControlSequences(comparison.currentSha256());

        out.printf(Locale.ROOT, "%-" + LABEL_WIDTH + "s%-" + RECORDED_WIDTH + "s%s%n",
                            MessageCatalogue.get("enrolment.accept.metric"),
                            MessageCatalogue.get("enrolment.accept.recorded"),
                            MessageCatalogue.get("enrolment.accept.current"));
        out.println(SEPARATOR);
        out.printf(Locale.ROOT, "%-" + LABEL_WIDTH + "s%-" + RECORDED_WIDTH + "s%s%n",
                            MessageCatalogue.get("enrolment.accept.size"), recordedSize, currentSize);
        out.printf(Locale.ROOT, "%-" + LABEL_WIDTH + "s%-" + RECORDED_WIDTH + "s%s%n",
                            MessageCatalogue.get("enrolment.accept.sha256"), recordedSha, currentSha);
        if (comparison.mismatchDetails() != null) {
            out.println(MessageCatalogue.format("enrolment.accept.mismatch",
                                Map.of("details", OutputSafety.stripControlSequences(comparison.mismatchDetails()))));
        }
        out.flush();
    }

    private static String formatSize(long size) {
        return String.format(Locale.ROOT, "%,d", size);
    }

    /**
     * @param project   the {@code --project} value, or {@code null}
     * @param storePath the store path
     * @return the project id, from {@code --project} or the path's leading segment
     */
    static String resolveProjectId(@Nullable String project, String storePath) {
        if (project != null && !project.isBlank()) {
            return project;
        }
        var slash = storePath.indexOf('/');
        return slash > 0 ? storePath.substring(0, slash) : "";
    }

    /**
     * @param project   the {@code --project} value, or {@code null}
     * @param storePath the store path
     * @return the store path relative to the project's scope root
     */
    static String relativeStore(@Nullable String project, String storePath) {
        if (project != null && !project.isBlank()) {
            return storePath;
        }
        var slash = storePath.indexOf('/');
        return slash > 0 ? storePath.substring(slash + 1) : storePath;
    }

    private boolean confirm() throws IOException {
        try (var terminal = context.createTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var line = reader.readLine(MessageCatalogue.get("enrolment.accept.confirm") + " \u203a ");
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
            var reason = JsonTree.string(map, "reason");
            if (reason == null) {
                reason = JsonTree.string(map, "error");
            }
            if (ERR_CONFIRMATION_MISMATCH.equals(reason)) {
                return MessageCatalogue.get("enrolment.accept.refused.stale");
            }
            return reason != null ? reason : "";
        } catch (IOException _) {
            return "";
        }
    }
}
