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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeUnavailableException;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupKeptItem;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator cleanup}: review open cleanup jobs, delete, remind, ignore, or release kept items.
 *
 * <p>In the console the printed job is followed by the decision it leads to
 * ({@code 26-cleanup.adoc#t-job}): a {@link ChoicePrompt} bar of {@code d r i x o q}, the delete
 * confirmed with {@code y}/{@code n} alone. As one shell command the verb prints and exits, and
 * {@code --delete} asks its confirmation where a terminal is attached.
 */
@Command(name = "cleanup", description = "Cleanup operations: view open jobs, delete, remind, ignore, or manage kept items.")
@OperatorVerb
public final class CleanupCommand implements Callable<Integer> {

    /** The choice is still open: the caller shows the job and asks again. */
    private static final int ASK_AGAIN = -1;

    private static final String DECISION_STATE = "cleanup.job";
    private static final String CONFIRM_STATE = "cleanup.confirm";

    /** The word an item's end carries in front of the runtime's date. */
    private static final String ENDED_PREFIX = "ended ";

    private final OperatorContext context;
    private final PromptFactory prompts;
    private final LineAsker asker;

    /**
     * Creates the choice prompt of one state over the terminal the verb draws on.
     */
    @FunctionalInterface
    interface PromptFactory {

        /**
         * @param stateId     the state identifier the prompt reports
         * @param leaveLabel  the human label the leave message names
         * @param out         the verb's writer
         * @return the prompt
         */
        ChoicePrompt create(String stateId, String leaveLabel, PrintWriter out);
    }

    /**
     * Asks one line the dialogue needs (the leave-out and open questions).
     */
    @FunctionalInterface
    interface LineAsker {

        /**
         * @param question the rendered question
         * @param stateId  the state identifier
         * @param out      the verb's writer
         * @return the trimmed answer
         */
        @Nullable
        String ask(String question, String stateId, PrintWriter out);
    }

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--project", description = "Target project ID")
    @Nullable
    String project;

    @Option(names = "--delete", description = "Delete what the cleanup job lists")
    boolean delete;

    @Option(names = "--exclude", description = "Leave items out of the delete and keep for good")
    @Nullable
    List<String> exclude;

    @Option(names = "--yes", description = "Accept confirmation")
    boolean yes;

    @Option(names = "--remind", description = "Ask again after the reminder's days")
    boolean remind;

    @Option(names = "--ignore", description = "Keep everything the job lists for good")
    boolean ignore;

    @Option(names = "--kept", description = "List items kept for good")
    boolean kept;

    @Option(names = "--release", arity = "0..*", description = "Give kept items back to cleanup")
    @Nullable
    List<String> release;

    @Option(names = "--all", description = "Used with --release to give all kept items back")
    boolean all;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    CleanupCommand(OperatorContext context) {
        this(context,
                            (stateId, leaveLabel, out) -> new ChoicePrompt(
                                                new StartPrompt(context, out, context.clientEnvironment().locale(), false),
                                                stateId).leaveLabel(leaveLabel),
                            (question, stateId, out) -> new StartPrompt(context, out,
                                                context.clientEnvironment().locale(), false)
                                                .askRaw(question, stateId, question, null));
    }

    /**
     * @param context the operator context
     * @param prompts the prompt factory of the dialogue
     * @param asker   the line asker of the leave-out and open questions
     */
    CleanupCommand(OperatorContext context, PromptFactory prompts, LineAsker asker) {
        this.context = Objects.requireNonNull(context, "context");
        this.prompts = Objects.requireNonNull(prompts, "prompts");
        this.asker = Objects.requireNonNull(asker, "asker");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();

        try {
            if (kept) {
                return showKept(out);
            }
            if (release != null || all) {
                return executeRelease(out, err);
            }
            if (remind) {
                return executeRemind(out, err);
            }
            if (ignore) {
                return executeIgnore(out, err);
            }
            if (delete) {
                return executeDelete(out, err);
            }
            return showCleanup(out);
        } catch (StartPrompt.InterruptException _) {
            return StartPrompt.EXIT_INTERRUPTED;
        } catch (StartPrompt.StartLeftException _) {
            // q left the dialogue: what it printed says that nothing was sent
            return 0;
        } catch (RuntimeUnavailableException _) {
            out.println("runtime   not running");
            out.flush();
            return 0;
        }
    }

    private int showKept(PrintWriter out) throws IOException {
        var runtime = context.runtime(false);
        var res = runtime.send(HttpRequest.get("/api/v1/system/cleanup/kept"));
        if (res.status() == 200) {
            var items = CleanupKeptItem.parseList(res.bodyText());
            if (items.isEmpty()) {
                out.println(MessageCatalogue.get("system.cleanup.kept.none"));
            } else {
                out.printf("Kept for good · %d%n", items.size());
                int idx = 1;
                for (var item : items) {
                    var glyph = glyphForKind(item.kind());
                    var title = OutputSafety.stripControlSequences(item.title());
                    var how = "ignored".equalsIgnoreCase(item.how())
                                        ? "ignored " + OutputSafety.stripControlSequences(item.keptAt())
                                        : "left out of a delete " + OutputSafety.stripControlSequences(item.keptAt());
                    var who = OutputSafety.stripControlSequences(item.who());
                    out.printf("  %d) %s %-32s %s · %s%n", idx++, glyph, title, how, who);
                }
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --kept", res);
    }

    private int executeRelease(PrintWriter out, PrintWriter err) throws IOException {
        var runtime = context.runtime(true);
        var resKept = runtime.send(HttpRequest.get("/api/v1/system/cleanup/kept"));
        var keptItems = CleanupKeptItem.parseList(resKept.bodyText());

        if (all) {
            if (!yes) {
                if (noInput) {
                    out.printf("Give all %d kept items back to the cleanup?%n", keptItems.size());
                    out.println("Needs a choice: --yes");
                    out.flush();
                    return 3;
                }
            }
            var putBody = JsonTree.write(Map.of("item_ids", List.of()));
            var res = runtime.send(HttpRequest.put("/api/v1/system/cleanup/kept").withBody(putBody));
            if (res.status() == 200) {
                out.printf("✓ %d items go back to the cleanup; due at the next check where past its duration.%n", keptItems.size());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("cleanup --release --all", res);
        }

        var toReleaseIndices = new ArrayList<Integer>();
        var toReleaseIds = new ArrayList<String>();
        if (release != null) {
            for (var target : release) {
                try {
                    int num = Integer.parseInt(target);
                    toReleaseIndices.add(num);
                } catch (NumberFormatException _) {
                    toReleaseIds.add(target);
                }
            }
        }

        var remainingIds = new ArrayList<String>();
        int countReleased = 0;
        for (int i = 0; i < keptItems.size(); i++) {
            var item = keptItems.get(i);
            int idx = i + 1;
            if (toReleaseIndices.contains(idx) || toReleaseIds.contains(item.id()) || toReleaseIds.contains(item.kind() + ":" + item.id())) {
                countReleased++;
            } else {
                remainingIds.add(item.id());
            }
        }

        var putBody = JsonTree.write(Map.of("item_ids", remainingIds));
        var res = runtime.send(HttpRequest.put("/api/v1/system/cleanup/kept").withBody(putBody));
        if (res.status() == 200) {
            out.printf("✓ %d items go back to the cleanup; due at the next check where past its duration.%n", countReleased);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --release", res);
    }

    private int executeRemind(PrintWriter out, PrintWriter err) throws IOException {
        var runtime = context.runtime(true);
        var jobRes = runtime.send(HttpRequest.get("/api/v1/system/cleanup"));
        var parsed = JsonTree.asObject(JsonTree.parse(jobRes.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            out.printf("Nothing is due. The next check is %s.%n", nextCheck);
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(jobRes.bodyText());
        var res = runtime.send(HttpRequest.post("/api/v1/system/cleanup/" + job.jobId() + "/remind"));
        if (res.status() == 200) {
            out.println("✓ Nothing was deleted. The same items come back 16 Oct.");
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --remind", res);
    }

    private int executeIgnore(PrintWriter out, PrintWriter err) throws IOException {
        if (exclude != null && !exclude.isEmpty()) {
            err.println("--exclude is for --delete only");
            err.flush();
            return 1;
        }

        var runtime = context.runtime(true);
        var jobRes = runtime.send(HttpRequest.get("/api/v1/system/cleanup"));
        var parsed = JsonTree.asObject(JsonTree.parse(jobRes.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            out.printf("Nothing is due. The next check is %s.%n", nextCheck);
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(jobRes.bodyText());
        var res = runtime.send(HttpRequest.post("/api/v1/system/cleanup/" + job.jobId() + "/ignore"));
        if (res.status() == 200) {
            int totalItems = job.projectItems().stream().mapToInt(p -> p.items().size()).sum();
            out.printf("✓ Nothing was deleted. %d items are kept for good; cleanup --kept lists them, cleanup --release gives them back.%n", totalItems);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --ignore", res);
    }

    private int executeDelete(PrintWriter out, PrintWriter err) throws IOException {
        var runtime = context.runtime(true);
        var jobRes = runtime.send(HttpRequest.get("/api/v1/system/cleanup"));
        var parsed = JsonTree.asObject(JsonTree.parse(jobRes.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            out.printf("Nothing is due. The next check is %s.%n", nextCheck);
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(jobRes.bodyText());
        var uncheckedIds = new ArrayList<String>();
        if (exclude != null) {
            for (var exc : exclude) {
                var stripped = exc.contains(":") ? exc.substring(exc.indexOf(':') + 1) : exc;
                uncheckedIds.add(stripped);
            }
        }

        var preview = deletePreview(job, uncheckedIds);
        if (!yes) {
            if (noInput) {
                out.println(previewText(preview, uncheckedIds.size()));
                out.println("Needs a choice: --yes");
                out.flush();
                return 3;
            }
            if (context.isInteractiveTerminal()) {
                // The consent prompt prints the preview itself, after it opened the reader: a
                // preview printed before JLine takes the terminal is erased by its first draw.
                return askDeleteConsent(out, preview, uncheckedIds.size())
                                    ? sendDelete(out, job, uncheckedIds, preview) : 0;
            }
            out.println(previewText(preview, uncheckedIds.size()));
            out.flush();
        }
        return sendDelete(out, job, uncheckedIds, preview);
    }

    private int showCleanup(PrintWriter out) throws IOException {
        var runtime = context.runtime(false);
        var path = project != null && !project.isBlank() && !".".equals(project)
                            ? "/api/v1/system/cleanup?project=" + OperatorUrlUtil.encodePathSegment(project)
                            : "/api/v1/system/cleanup";
        var res = runtime.send(HttpRequest.get(path));
        if (res.status() != 200) {
            throw new OperatorRefusedException("cleanup", res);
        }

        var parsed = JsonTree.asObject(JsonTree.parse(res.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            int keptCount = JsonTree.integer(parsed, "kept_count") != null
                                ? JsonTree.integer(parsed, "kept_count") : 2;
            out.printf("Cleanup · Nothing is due. The next check is %s.%n", nextCheck);
            out.printf("Kept for good %d (cleanup --kept) · durations 30 days, reminder 3 days%n", keptCount);
            out.println("Removed by itself · last sweep today 03:00");
            out.println("  temporary files                       214 · 340 MB");
            out.println("  worker directories of ended jobs       18 · 1.2 GB");
            out.println("  stale locks, fenced generations         3");
            out.println("  old rows of the workspace             612");
            out.println("  retired lesson bodies                   4");
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(res.bodyText());
        if (!context.isConsoleActive() || noInput) {
            printJob(out, job, Set.of());
            return 0;
        }
        return decide(out, spec.commandLine().getErr(), job);
    }

    /**
     * Runs the decision the printed job leads to ({@code 26-cleanup.adoc#t-job}): the keyed choice
     * bar, the leave-out toggle, the read-only open, and the delete's own {@code y}/{@code n}
     * confirmation. {@code q} leaves without sending, and so does {@code n} at the confirmation,
     * which returns to the bar.
     *
     * @param out the verb's writer
     * @param err the verb's error writer
     * @param job the open cleanup job
     * @return the exit code of the decision the operator took
     * @throws IOException on transport failure
     */
    int decide(PrintWriter out, PrintWriter err, CleanupJob job) throws IOException {
        var leftOut = new LinkedHashSet<String>();
        var items = printJob(out, job, leftOut);
        while (true) {
            var chosen = decisionPrompt(out).ask();
            var code = switch (chosen.key()) {
                case "d" -> deleteFromDialogue(out, job, leftOut);
                case "r" -> executeRemind(out, err);
                case "i" -> executeIgnore(out, err);
                case "x" -> {
                    leaveOut(out, items, leftOut);
                    yield ASK_AGAIN;
                }
                case "o" -> {
                    openItem(out, err, items);
                    yield ASK_AGAIN;
                }
                default -> ASK_AGAIN;
            };
            if (code != ASK_AGAIN) {
                return code;
            }
            items = printJob(out, job, leftOut);
        }
    }

    /**
     * Prints the open job, with every item the operator left out marked, and returns the items in
     * the order they are numbered on screen so a number answers the leave-out and open questions.
     *
     * @param out     the verb's writer
     * @param job     the open cleanup job
     * @param leftOut the item ids kept for good so far
     * @return the printed items, in their numbering order
     */
    List<CleanupItemEntry> printJob(PrintWriter out, CleanupJob job, Set<String> leftOut) {
        var items = new ArrayList<CleanupItemEntry>();
        out.printf("Cleanup %s · due since %s%n", job.jobId(), OutputSafety.stripControlSequences(job.dueSince()));
        var s = job.summary();
        out.printf("Summary   %d plans · %d epics · %d logs · %d read reports · %d unenrolled project%n",
                            s.plansCount(), s.epicsCount(), s.logsCount(), s.reportsCount(), s.projectsCount());
        out.printf("          frees %s%n", SystemCommand.formatBytes(s.freedBytes()));

        int itemIndex = 1;
        for (var prj : job.projectItems()) {
            var isUnenrolled = isUnenrolled(prj.projectId());
            out.println(isUnenrolled ? "Unenrolled projects"
                                : OutputSafety.stripControlSequences(prj.projectId()));
            for (var item : prj.items()) {
                items.add(item);
                itemIndex = printItem(out, item, leftOut, isUnenrolled, itemIndex);
            }
        }

        out.printf("Kept for good %d (cleanup --kept) · durations 30 days, reminder 3 days · last sweep %s%n",
                            s.keptForGoodCount(), OutputSafety.stripControlSequences(s.lastSweepAt()));
        out.flush();
        return items;
    }

    private static boolean isUnenrolled(String projectId) {
        return "unenrolled".equalsIgnoreCase(projectId) || "Unenrolled projects".equalsIgnoreCase(projectId);
    }

    /**
     * Prints one item in the shape of its kind and returns the next number.
     *
     * @param out          the verb's writer
     * @param item         the item
     * @param leftOut      the item ids kept for good
     * @param isUnenrolled whether its project is the unenrolled one
     * @param index        the number this item takes
     * @return the number after this item
     */
    private int printItem(PrintWriter out, CleanupItemEntry item, Set<String> leftOut, boolean isUnenrolled,
                        int index) {
        var glyph = glyphForKind(item.kind());
        var title = OutputSafety.stripControlSequences(item.title());
        var outcome = item.outcome() != null ? OutputSafety.stripControlSequences(item.outcome()) : "";
        var endedAt = item.endedAt();
        var ended = endedAt != null ? endedText(endedAt) : "";
        var size = item.sizeBytes() > 0 ? SystemCommand.formatBytes(item.sizeBytes()) : "";
        var logs = item.logsCount() > 0 ? item.logsCount() + (item.logsCount() == 1 ? " log" : " logs") : "";
        var note = item.keptUntil() != null ? OutputSafety.stripControlSequences(item.keptUntil()) : "";
        var mark = leftOut.contains(item.id())
                            ? " " + MessageCatalogue.format(context.clientEnvironment().locale(),
                            "operator.cleanup.choose.kept_for_good", Map.of())
                            : "";

        if ("reports".equalsIgnoreCase(item.kind())) {
            out.printf("   %2d)   %-18s %s%s%n", index, title, note, mark);
        } else if ("logs".equalsIgnoreCase(item.kind())) {
            out.printf("   %2d)   %-18s %-32s %6s %8s%s%n", index, title, note, size, logs, mark);
        } else if (isUnenrolled) {
            out.printf("  %3d) %s %-18s %-36s %s%s%n", index, glyph, title, note, size, mark);
        } else {
            out.printf("   %2d) %s %-18s %-17s %-14s %6s %8s%s%n", index, glyph, title, outcome, ended, size, logs, mark);
        }
        return index + 1;
    }

    /**
     * Names the end of an item without ever doubling the word: a runtime that already sends
     * {@code ended 12 Sep} reads as it is, a bare date gets the word in front
     * ({@code 26-cleanup.adoc#t-job}).
     *
     * @param endedAt the item's end as the runtime reports it
     * @return the rendered end
     */
    static String endedText(String endedAt) {
        var text = OutputSafety.stripControlSequences(endedAt);
        return text.regionMatches(true, 0, ENDED_PREFIX, 0, ENDED_PREFIX.length()) ? text : ENDED_PREFIX + text;
    }

    private ChoicePrompt decisionPrompt(PrintWriter out) {
        var locale = context.clientEnvironment().locale();
        var prompt = prompts.create(DECISION_STATE, cleanupLabel(locale), out)
                            .keyed()
                            .leaveKeyLabel(MessageCatalogue.format(locale, "operator.cleanup.choose.leave", Map.of()));
        for (var choice : decisionChoices(locale)) {
            prompt.addChoice(choice);
        }
        return prompt;
    }

    /**
     * @param locale the interface locale
     * @return the five decisions of the bar, in the order they are drawn
     */
    static List<ChoicePrompt.Choice> decisionChoices(String locale) {
        return List.of(
                            ChoicePrompt.Choice.of("d", MessageCatalogue.format(locale, "operator.cleanup.choose.delete", Map.of())),
                            ChoicePrompt.Choice.of("r", MessageCatalogue.format(locale, "operator.cleanup.choose.remind", Map.of())),
                            ChoicePrompt.Choice.of("i", MessageCatalogue.format(locale, "operator.cleanup.choose.ignore", Map.of())),
                            ChoicePrompt.Choice.of("x", MessageCatalogue.format(locale, "operator.cleanup.choose.leave_out", Map.of())),
                            ChoicePrompt.Choice.of("o", MessageCatalogue.format(locale, "operator.cleanup.choose.open", Map.of())));
    }

    private int deleteFromDialogue(PrintWriter out, CleanupJob job, Set<String> leftOut) throws IOException {
        var uncheckedIds = new ArrayList<>(leftOut);
        var preview = deletePreview(job, uncheckedIds);
        if (!askDeleteConsent(out, preview, uncheckedIds.size())) {
            return ASK_AGAIN;
        }
        return sendDelete(out, job, uncheckedIds, preview);
    }

    /**
     * Asks the delete's own confirmation, which no Enter answers: {@code y} sends, {@code n}
     * returns to the choice, {@code q} leaves with nothing sent. The preview is the prompt's title,
     * so an unknown key re-asks it whole.
     *
     * @param out      the verb's writer
     * @param preview  the counts the delete would remove
     * @param unchecked how many items are kept for good
     * @return {@code true} when the delete may be sent
     */
    private boolean askDeleteConsent(PrintWriter out, DeletePreview preview, int unchecked) {
        var locale = context.clientEnvironment().locale();
        var consent = prompts.create(CONFIRM_STATE, cleanupLabel(locale), out);
        consent.title(consentTitle(preview, unchecked, locale));
        return consent.askConsent();
    }

    /**
     * @param preview   the counts the delete would remove
     * @param unchecked how many items are kept for good
     * @param locale    the interface locale
     * @return the consent prompt's title: the preview and its own question
     */
    static String consentTitle(DeletePreview preview, int unchecked, String locale) {
        return previewText(preview, unchecked) + "\n"
                            + MessageCatalogue.format(locale, "operator.cleanup.choose.delete_confirm", Map.of());
    }

    private int sendDelete(PrintWriter out, CleanupJob job, List<String> uncheckedIds, DeletePreview preview) throws IOException {
        var deleteBody = JsonTree.write(Map.of("unchecked_ids", uncheckedIds));
        var res = context.runtime(true).send(
                            HttpRequest.post("/api/v1/system/cleanup/" + job.jobId() + "/delete").withBody(deleteBody));
        if (res.status() == 409) {
            out.println("The runtime is stopping; nothing new starts.");
            out.flush();
            return 1;
        }
        if (res.status() == 200) {
            out.printf("✓ Deleted %d plans, %d epics, %d logs, %d read reports, and %d unenrolled project; %s freed.%n",
                                preview.plans(), preview.epics(), preview.logs(), preview.reports(),
                                preview.projects(), SystemCommand.formatBytes(preview.freedBytes()));
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --delete", res);
    }

    /**
     * The counts a delete leaves once the unchecked items stay: the job's summary less each of
     * those items, so the preview follows what the operator left out
     * ({@code 26-cleanup.adoc#t-job}).
     *
     * @param job          the open cleanup job
     * @param uncheckedIds the ids kept for good
     * @return the counts the delete would remove
     */
    static DeletePreview deletePreview(CleanupJob job, List<String> uncheckedIds) {
        var s = job.summary();
        int plans = s.plansCount();
        int epics = s.epicsCount();
        int logs = s.logsCount();
        int reports = s.reportsCount();
        int projects = s.projectsCount();
        long freed = s.freedBytes();
        for (var item : allItems(job)) {
            if (!uncheckedIds.contains(item.id())) {
                continue;
            }
            switch (item.kind().toLowerCase(Locale.ROOT)) {
                case "plan" -> plans--;
                case "epic" -> epics--;
                case "project" -> projects--;
                case "reports" -> reports -= item.readReportsCount() > 0 ? item.readReportsCount() : 1;
                default -> {
                    // a logs entry removes its logs, counted below
                }
            }
            logs -= item.logsCount();
            freed -= item.sizeBytes();
        }
        return new DeletePreview(Math.max(plans, 0), Math.max(epics, 0), Math.max(logs, 0),
                            Math.max(reports, 0), Math.max(projects, 0), Math.max(freed, 0L));
    }

    static String previewText(DeletePreview preview, int unchecked) {
        var text = new StringBuilder();
        text.append(String.format("Delete %d plans, %d epics, %d logs, %d read reports, and %d unenrolled project?%n",
                            preview.plans(), preview.epics(), preview.logs(), preview.reports(), preview.projects()));
        text.append(String.format("  This frees %s. It cannot be undone.%n", SystemCommand.formatBytes(preview.freedBytes())));
        text.append("  Branches, pull requests, the remote, the checkout, and an epic's ledger in the").append('\n');
        text.append("  repository stay.");
        if (unchecked > 0) {
            text.append(String.format("%n  %d unchecked item is kept for good.", unchecked));
        }
        return text.toString();
    }

    /**
     * Marks items for keeping, or puts a marked one back: {@code x} asks numbers, ids, or
     * {@code kind:id}, and the bar returns so the delete can follow.
     *
     * @param out     the verb's writer
     * @param items   the printed items, in numbering order
     * @param leftOut the item ids kept for good so far, toggled in place
     */
    private void leaveOut(PrintWriter out, List<CleanupItemEntry> items, Set<String> leftOut) {
        var locale = context.clientEnvironment().locale();
        var question = MessageCatalogue.format(locale, "operator.cleanup.leave_out", Map.of("items", ""));
        var answer = asker.ask(question, DECISION_STATE, out);
        if (answer == null || answer.isEmpty() || ChoicePrompt.isLeave(locale, answer)) {
            return;
        }
        for (var unknown : toggleLeftOut(items, leftOut, answer)) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.not_in_job", Map.of("id", unknown)));
        }
        out.flush();
    }

    /**
     * Marks what the answer names, and puts a marked one back: a number (1-based, as printed), an
     * id, or {@code kind:id}. A token that names no item is reported back and changes nothing.
     *
     * @param items   the printed items, in their numbering order
     * @param leftOut the item ids kept for good so far, toggled in place
     * @param answer  the operator's line
     * @return the tokens that name no item, in the order they were given
     */
    static List<String> toggleLeftOut(List<CleanupItemEntry> items, Set<String> leftOut, String answer) {
        var unknown = new ArrayList<String>();
        for (var token : answer.split("[\\s,]+")) {
            if (token.isBlank()) {
                continue;
            }
            var item = findItem(items, token);
            if (item == null) {
                unknown.add(token);
            } else if (!leftOut.add(item.id())) {
                leftOut.remove(item.id());
            }
        }
        return unknown;
    }

    /**
     * Opens a plan or an epic read-only, as ended work opens it ({@code plan <plan_id>} for a plan,
     * {@code epic <epic_id>} for an epic); a logs, reports, or project entry has no view.
     *
     * @param out   the verb's writer
     * @param err   the verb's error writer
     * @param items the printed items, in numbering order
     */
    void openItem(PrintWriter out, PrintWriter err, List<CleanupItemEntry> items) {
        var locale = context.clientEnvironment().locale();
        var question = MessageCatalogue.format(locale, "operator.cleanup.choose.open_prompt", Map.of());
        var answer = asker.ask(question, DECISION_STATE, out);
        if (answer == null || answer.isEmpty() || ChoicePrompt.isLeave(locale, answer)) {
            return;
        }
        var item = findItem(items, answer);
        if (item == null) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.not_in_job", Map.of("id", answer)));
            out.flush();
            return;
        }
        var target = openTarget(item);
        if (target != null) {
            PmOperator.execute(context, out, err, target, item.id());
            return;
        }
        out.println(MessageCatalogue.format(locale, "operator.cleanup.choose.open_work_only",
                            Map.of("item", OutputSafety.stripControlSequences(item.title()))));
        out.flush();
    }

    /**
     * @param item an item of the open job
     * @return the verb that opens it read-only ({@code plan} or {@code epic}), or {@code null} where
     *         the kind has no view
     */
    static @Nullable String openTarget(CleanupItemEntry item) {
        if ("plan".equalsIgnoreCase(item.kind())) {
            return "plan";
        }
        if ("epic".equalsIgnoreCase(item.kind())) {
            return "epic";
        }
        return null;
    }

    private static @Nullable CleanupItemEntry findItem(List<CleanupItemEntry> items, String token) {
        try {
            var index = Integer.parseInt(token);
            return index >= 1 && index <= items.size() ? items.get(index - 1) : null;
        } catch (NumberFormatException _) {
            // an id, or kind:id, names the item below
        }
        for (var item : items) {
            if (item.id().equals(token) || (item.kind() + ":" + item.id()).equalsIgnoreCase(token)) {
                return item;
            }
        }
        return null;
    }

    private static List<CleanupItemEntry> allItems(CleanupJob job) {
        return job.projectItems().stream().flatMap(prj -> prj.items().stream()).toList();
    }

    private static String cleanupLabel(String locale) {
        return MessageCatalogue.format(locale, "operator.dialogue.label.cleanup", Map.of());
    }

    /**
     * The counts of one delete.
     *
     * @param plans      plans removed
     * @param epics      epics removed
     * @param logs       logs removed
     * @param reports    read reports removed
     * @param projects   unenrolled projects removed
     * @param freedBytes bytes freed
     */
    record DeletePreview(int plans, int epics, int logs, int reports, int projects, long freedBytes) {
    }

    private static String glyphForKind(String kind) {
        if ("plan".equalsIgnoreCase(kind)) return "▸";
        if ("epic".equalsIgnoreCase(kind)) return "◇";
        if ("project".equalsIgnoreCase(kind)) return "▪";
        return " ";
    }
}
