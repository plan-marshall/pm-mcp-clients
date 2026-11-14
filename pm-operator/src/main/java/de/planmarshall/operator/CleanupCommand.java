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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.runtime.RuntimeUnavailableException;
import de.planmarshall.api.system.CleanupDecidedEvent;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupKeptItem;
import de.planmarshall.api.system.CleanupKind;
import de.planmarshall.api.system.CleanupResidue;
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

    /** The items one project group prints before the rest is left to the project's own command. */
    private static final int PAGE_SIZE = 20;

    /** The width a printed item line may not exceed. */
    private static final int MAX_LINE = 80;

    /** The column the title starts in, where a wrapped mark is aligned. */
    private static final int TITLE_COLUMN = 8;

    /** The width of the title column; a longer title is cut rather than shifting the columns. */
    private static final int TITLE_WIDTH = 18;

    /** The catalogue key of the "no such item" refusal the three lookups share. */
    private static final String NOT_IN_JOB = "operator.cleanup.not_in_job";

    /** The word an item's end carries in front of the runtime's date. */
    private static final String ENDED_PREFIX = "ended ";

    /** The cleanup resource of the runtime's API. */
    private static final String CLEANUP_API = "/api/v1/system/cleanup";
    private static final String KEPT_API = CLEANUP_API + "/kept";
    private static final String COUNT_PARAM = "count";

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

    /** The durations in force as the last read of the cleanup named them, or none it named. */
    @Nullable
    CleanupDurations durations;

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

        if (exclude != null && !exclude.isEmpty() && !isDeleteVerb()) {
            // --exclude belongs to --delete alone and is refused with every other form
            // (26-cleanup.adoc#commands, #t-remind): a usage error, whose text the catalogue owns like
            // every other refusal of the verb.
            err.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                "operator.cleanup.exclude_delete_only", Map.of()));
            err.flush();
            return 2;
        }

        try {
            if (kept) {
                return showKept(out);
            }
            if (release != null || all) {
                return executeRelease(out);
            }
            if (remind) {
                return executeRemind(out);
            }
            if (ignore) {
                return executeIgnore(out);
            }
            if (delete) {
                return executeDelete(out);
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

    /**
     * @return whether the options name the delete, the one form {@code --exclude} goes with
     */
    private boolean isDeleteVerb() {
        return delete && !kept && release == null && !all && !remind && !ignore;
    }

    private int showKept(PrintWriter out) throws IOException {
        var items = keptItems(context.runtime(false), "cleanup --kept");
        var locale = context.clientEnvironment().locale();
        if (items.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "system.cleanup.kept.none", Map.of()));
        } else {
            out.printf("%s · %d%n", MessageCatalogue.format(locale, "system.cleanup.kept.title", Map.of()),
                                items.size());
            int idx = 1;
            for (var item : items) {
                var glyph = glyphForKind(item.kind());
                var title = OutputSafety.stripControlSequences(item.title());
                // how is the wire value ignore or exclude; the catalogue names it
                // (cleanup-texts.adoc#texts-kept)
                var why = MessageCatalogue.format(locale, "system.cleanup.kept.why", Map.of(
                                    "how", OutputSafety.stripControlSequences(item.how()),
                                    "time", OutputSafety.stripControlSequences(item.keptAt()),
                                    "who", OutputSafety.stripControlSequences(item.who())));
                out.printf("  %d) %s %-32s %s%n", idx++, glyph, title, why);
            }
        }
        out.flush();
        return 0;
    }

    /**
     * @param runtime the runtime client
     * @param verb    the verb named in a refusal
     * @return the items kept for good, as the runtime lists them
     * @throws IOException on transport failure, or the runtime's refusal
     */
    private static List<CleanupKeptItem> keptItems(RuntimeClient runtime, String verb)
                        throws IOException {
        var res = runtime.send(HttpRequest.get(KEPT_API));
        if (res.status() != 200) {
            throw new OperatorRefusedException(verb, res);
        }
        return CleanupKeptItem.parseList(res.bodyText());
    }

    private int executeRelease(PrintWriter out) throws IOException {
        var runtime = context.runtime(true);
        var keptItems = keptItems(runtime, all ? "cleanup --release --all" : "cleanup --release");
        return all ? releaseAll(out, runtime, keptItems.size()) : releaseNamed(out, runtime, keptItems);
    }

    private int releaseAll(PrintWriter out, RuntimeClient runtime, int keptCount) throws IOException {
        if (!yes && !confirmReleaseAll(out, keptCount)) {
            return noInput ? 3 : 0;
        }
        var putBody = JsonTree.write(Map.of("item_ids", List.of()));
        var res = runtime.send(HttpRequest.put(KEPT_API).withBody(putBody));
        if (res.status() == 200) {
            printReleased(out, keptCount);
            return 0;
        }
        throw new OperatorRefusedException("cleanup --release --all", res);
    }

    /** Releases the kept items the operator named by their number in the list, their id, or kind:id. */
    private int releaseNamed(PrintWriter out, RuntimeClient runtime, List<CleanupKeptItem> keptItems)
                        throws IOException {
        // without --all the verb was dispatched for --release, so its list is present
        var named = Objects.requireNonNull(release, "release");
        var remainingIds = new ArrayList<String>();
        int countReleased = 0;
        for (int i = 0; i < keptItems.size(); i++) {
            var item = keptItems.get(i);
            if (isNamedForRelease(named, i + 1, item)) {
                countReleased++;
            } else {
                remainingIds.add(item.id());
            }
        }

        var putBody = JsonTree.write(Map.of("item_ids", remainingIds));
        var res = runtime.send(HttpRequest.put(KEPT_API).withBody(putBody));
        if (res.status() == 200) {
            printReleased(out, countReleased);
            return 0;
        }
        throw new OperatorRefusedException("cleanup --release", res);
    }

    /**
     * @param named  the tokens of {@code --release}
     * @param number the item's number in the list, from one
     * @param item   the kept item
     * @return whether a token names the item by its number, its id, or its kind and id; a token that
     *         reads as a number names a position only
     */
    private static boolean isNamedForRelease(List<String> named, int number, CleanupKeptItem item) {
        var kindAndId = kindWord(item.kind()) + ":" + item.id();
        for (var token : named) {
            var position = positionOf(token);
            var names = position != null ? position == number : token.equals(item.id()) || token.equals(kindAndId);
            if (names) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Integer positionOf(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    /**
     * Asks the one confirmation of {@code --release --all} ({@code 26-cleanup.adoc#t-kept}): with
     * {@code --no-input} it names the question and the option that answers it, and otherwise asks
     * {@code y}/{@code n} on the interactive terminal the operator checks require.
     *
     * @param out   the verb's writer
     * @param count how many items are kept
     * @return {@code true} when the release may be sent
     */
    private boolean confirmReleaseAll(PrintWriter out, int count) {
        var locale = context.clientEnvironment().locale();
        var question = MessageCatalogue.format(locale, "system.cleanup.kept.confirm_all", Map.of(COUNT_PARAM, count));
        if (noInput) {
            out.println(question);
            out.println("Needs a choice: --yes");
            out.flush();
            return false;
        }
        var consent = prompts.create(CONFIRM_STATE, cleanupLabel(locale), out);
        consent.title(question);
        return consent.askConsent();
    }

    private void printReleased(PrintWriter out, int count) {
        out.println("✓ " + MessageCatalogue.format(context.clientEnvironment().locale(),
                            "system.cleanup.kept.released", Map.of(COUNT_PARAM, count)));
        out.flush();
    }

    private int executeRemind(PrintWriter out) throws IOException {
        return executeRemind(out, null);
    }

    /**
     * Asks again after the reminder's days, on the job the operator chose or on the one the runtime
     * reports.
     *
     * @param out       the verb's writer
     * @param displayed the job the operator saw, or {@code null} to read the open one
     * @return the exit code
     * @throws IOException on transport failure
     */
    private int executeRemind(PrintWriter out, @Nullable CleanupJob displayed) throws IOException {
        var job = displayed != null ? displayed : openJob(out, "cleanup --remind");
        if (job == null) {
            return 0;
        }
        if (displayed == null) {
            printWholeJobActionNote(out);
        }
        var res = context.runtime(true).send(HttpRequest.post(CLEANUP_API + "/" + job.jobId() + "/remind"));
        if (res.status() == 200) {
            printRemindDone(out, res.bodyText());
            return 0;
        }
        throw new OperatorRefusedException("cleanup --remind", res);
    }

    /**
     * Names the date the reminder brings the items back, read from the act's own answer: the
     * response's {@code remind_at}, else its {@code remind_after_days} counted from today, and a
     * line without a date where it names neither.
     *
     * @param out  the verb's writer
     * @param body the act's answer
     */
    private void printRemindDone(PrintWriter out, String body) {
        var locale = context.clientEnvironment().locale();
        var when = remindWhen(body, locale);
        out.println(MessageCatalogue.format(locale,
                            when != null ? "operator.cleanup.remind.done" : "operator.cleanup.remind.done_undated",
                            when != null ? Map.of("when", when) : Map.of()));
        out.flush();
    }

    private @Nullable String remindWhen(String body, String locale) {
        try {
            var parsed = JsonTree.asObject(JsonTree.parse(body));
            if (parsed == null) {
                return null;
            }
            var at = JsonTree.string(parsed, "remind_at");
            if (at != null && !at.isBlank()) {
                return formatWhen(at, locale, context.clock().getZone());
            }
            var days = JsonTree.integer(parsed, "remind_after_days");
            if (days == null) {
                return null;
            }
            return LocalDate.now(context.clock()).plusDays(days)
                                .format(DateTimeFormatter.ofPattern(dayMonthPattern(locale), Locale.forLanguageTag(locale)));
        } catch (IOException | IllegalArgumentException _) {
            return null;
        }
    }

    /**
     * Renders a date the runtime names: an ISO instant becomes the day of the client's clock zone,
     * and any other form is shown as the runtime sent it.
     *
     * @param at     the runtime's date
     * @param locale the interface locale
     * @param zone   the zone the client counts days in
     * @return the rendered date
     */
    static String formatWhen(String at, String locale, ZoneId zone) {
        var text = OutputSafety.stripControlSequences(at);
        try {
            var day = Instant.parse(text).atZone(zone).toLocalDate();
            return day.format(DateTimeFormatter.ofPattern(dayMonthPattern(locale), Locale.forLanguageTag(locale)));
        } catch (DateTimeParseException _) {
            return text;
        }
    }

    private static String dayMonthPattern(String locale) {
        return locale.toLowerCase(Locale.ROOT).startsWith("de") ? "d. MMM" : "d MMM";
    }

    private int executeIgnore(PrintWriter out) throws IOException {
        return executeIgnore(out, null);
    }

    /**
     * Keeps everything the job lists for good, on the job the operator chose or on the one the
     * runtime reports.
     *
     * @param out       the verb's writer
     * @param displayed the job the operator saw, or {@code null} to read the open one
     * @return the exit code
     * @throws IOException on transport failure
     */
    private int executeIgnore(PrintWriter out, @Nullable CleanupJob displayed) throws IOException {
        var job = displayed != null ? displayed : openJob(out, "cleanup --ignore");
        if (job == null) {
            return 0;
        }
        if (displayed == null) {
            printWholeJobActionNote(out);
        }
        var res = context.runtime(true).send(HttpRequest.post(CLEANUP_API + "/" + job.jobId() + "/ignore"));
        if (res.status() == 200) {
            printIgnoreDone(out, job);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --ignore", res);
    }

    /**
     * Names what the ignore keeps: the job's item count where the whole job is shown, and no count
     * where a project filter hides part of it.
     *
     * @param out the verb's writer
     * @param job the job the operator answered
     */
    private void printIgnoreDone(PrintWriter out, CleanupJob job) {
        var locale = context.clientEnvironment().locale();
        if (project != null && !".".equals(project)) {
            out.println(MessageCatalogue.format(locale, "operator.cleanup.ignored.whole_job", Map.of()));
            return;
        }
        int totalItems = job.projectItems().stream().mapToInt(p -> p.items().size()).sum();
        out.println(MessageCatalogue.format(locale, "operator.cleanup.ignored.count",
                            Map.of(COUNT_PARAM, totalItems)));
    }

    /**
     * @param out  the verb's writer
     * @param verb the verb named in a refusal
     * @return the open cleanup job, or {@code null} when nothing is due (the line is printed)
     * @throws IOException on transport failure, or the runtime's refusal
     */
    private @Nullable CleanupJob openJob(PrintWriter out, String verb) throws IOException {
        var state = cleanupState(context.runtime(true), CLEANUP_API, verb);
        if (isNothingDue(state)) {
            out.println(nothingDue(state, verb));
            out.flush();
            return null;
        }
        return CleanupJob.fromMap(state);
    }

    /**
     * Reads the cleanup resource: the open job, or none with the next check, the durations, the
     * kept count, and the residue ({@code cleanup.adoc#requests}).
     *
     * @param runtime the runtime client
     * @param path    the resource, with its project filter where one is given
     * @param verb    the verb named in a refusal
     * @return the resource's object
     * @throws IOException on transport failure, or the runtime's refusal
     */
    private static Map<String, Object> cleanupState(RuntimeClient runtime, String path, String verb)
                        throws IOException {
        var res = runtime.send(HttpRequest.get(path));
        if (res.status() != 200) {
            throw new OperatorRefusedException(verb, res);
        }
        var parsed = JsonTree.asObject(JsonTree.parse(res.bodyText()));
        if (parsed == null) {
            throw new OperatorRefusedException(verb + ": the runtime's cleanup is no object");
        }
        return parsed;
    }

    private static boolean isNothingDue(Map<String, Object> state) {
        return Boolean.FALSE.equals(state.get("open"));
    }

    /**
     * @param state the cleanup resource without an open job
     * @param verb  the verb named in a refusal
     * @return the line that nothing is due, with the next check the runtime names
     * @throws OperatorRefusedException where the runtime names no next check
     */
    private String nothingDue(Map<String, Object> state, String verb) throws OperatorRefusedException {
        var nextCheck = JsonTree.string(state, "next_check_at");
        if (nextCheck == null || nextCheck.isBlank()) {
            throw new OperatorRefusedException(verb + ": the runtime's cleanup names no next check");
        }
        return MessageCatalogue.format(context.clientEnvironment().locale(), "system.cleanup.none",
                            Map.of("time", OutputSafety.stripControlSequences(nextCheck)));
    }

    private int executeDelete(PrintWriter out) throws IOException {
        var job = openJob(out, "cleanup --delete");
        if (job == null) {
            return 0;
        }
        var uncheckedIds = resolveExclude(out, job);
        if (uncheckedIds == null) {
            return 1;
        }

        var preview = deletePreview(job, uncheckedIds);
        if (!yes) {
            if (noInput || !context.isInteractiveTerminal()) {
                // A delete that nothing can answer sends nothing (01-commands.adoc#direct-verbs):
                // the preview is printed, and the verb stops at the confirmation with exit code 3.
                return previewThenStop(out, preview, uncheckedIds.size());
            }
            // The consent prompt prints the preview itself, after it opened the reader: a
            // preview printed before JLine takes the terminal is erased by its first draw.
            return askDeleteConsent(out, preview, uncheckedIds.size())
                                ? sendDelete(out, job, uncheckedIds, preview) : 0;
        }
        return sendDelete(out, job, uncheckedIds, preview);
    }

    /**
     * Prints the delete's preview and stops at its confirmation, for a run that has neither a
     * terminal to answer it nor {@code --yes}.
     *
     * @param out      the verb's writer
     * @param preview  the counts the delete would remove
     * @param unchecked how many items are kept for good
     * @return the exit code of a run stopped at a choice it was not given
     */
    int previewThenStop(PrintWriter out, DeletePreview preview, int unchecked) {
        out.println(previewText(preview, unchecked));
        printWholeJobNote(out);
        out.println("Needs a choice: --yes");
        out.flush();
        return 3;
    }

    /**
     * Resolves every {@code --exclude} token to an item of the open job: a number as the job prints
     * it, an id, or {@code kind:id}. A token the job does not carry is reported
     * ({@code operator.cleanup.not_in_job}) and stops the delete.
     *
     * @param out the verb's writer
     * @param job the open cleanup job
     * @return the ids the delete leaves out, or {@code null} when a token names no item
     */
    private @Nullable List<String> resolveExclude(PrintWriter out, CleanupJob job) {
        if (exclude == null || exclude.isEmpty()) {
            return List.of();
        }
        var items = allItems(job);
        var ids = new ArrayList<String>();
        for (var token : exclude) {
            var raw = OutputSafety.stripControlSequences(token);
            var item = findItem(items, raw);
            if (item == null) {
                out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                    NOT_IN_JOB, Map.of("item", raw)));
                out.flush();
                return null;
            }
            if (!ids.contains(item.id())) {
                // Two tokens may name the same item: the job keeps it once, so the request carries it once.
                ids.add(item.id());
            }
        }
        return ids;
    }

    private void printWholeJobNote(PrintWriter out) {
        if (project != null && !".".equals(project)) {
            out.println("  " + MessageCatalogue.format(context.clientEnvironment().locale(),
                                "operator.cleanup.choose.whole_job", Map.of()));
        }
    }

    private void printWholeJobActionNote(PrintWriter out) {
        if (project != null && !".".equals(project)) {
            out.println("  " + MessageCatalogue.format(context.clientEnvironment().locale(),
                                "operator.cleanup.choose.whole_job_action", Map.of()));
        }
    }

    private int showCleanup(PrintWriter out) throws IOException {
        var path = project != null && !project.isBlank() && !".".equals(project)
                            ? CLEANUP_API + "?project=" + OperatorUrlUtil.encodePathSegment(project)
                            : CLEANUP_API;
        var state = cleanupState(context.runtime(false), path, "cleanup");
        durations = durationsOf(state);
        if (isNothingDue(state)) {
            printNothingDue(out, state);
            return 0;
        }

        var job = CleanupJob.fromMap(state);
        if (!context.isConsoleActive() || noInput) {
            printJob(out, job, Set.of());
            return 0;
        }
        return decide(out, spec.commandLine().getErr(), job);
    }

    /**
     * Prints that nothing is due, then what the runtime names beside it: the kept count, the
     * durations in force, and the residue per sweep step ({@code 26-cleanup.adoc#t-none}). A part
     * the runtime does not send is left out rather than stood in for.
     *
     * @param out   the verb's writer
     * @param state the cleanup resource without an open job
     * @throws IOException where a residue entry names no step, or the runtime names no next check
     */
    private void printNothingDue(PrintWriter out, Map<String, Object> state) throws IOException {
        var locale = context.clientEnvironment().locale();
        out.println(MessageCatalogue.format(locale, "system.cleanup.title", Map.of()) + " · "
                            + nothingDue(state, "cleanup"));
        var parts = new ArrayList<String>();
        var keptCount = JsonTree.integer(state, "kept_count");
        if (keptCount != null) {
            parts.add(String.format("Kept for good %d (cleanup --kept)", keptCount));
        }
        if (durations != null) {
            parts.add("durations " + durationsText(locale, durations));
        }
        if (!parts.isEmpty()) {
            out.println(String.join(" · ", parts));
        }
        printResidue(out, residuesOf(state), locale);
        out.flush();
    }

    private static @Nullable CleanupDurations durationsOf(Map<String, Object> state) throws IOException {
        var object = JsonTree.asObject(state.get("durations"));
        return object != null ? CleanupDurations.fromMap(object) : null;
    }

    private static List<CleanupResidue> residuesOf(Map<String, Object> state) throws IOException {
        var entries = JsonTree.asArray(state.get("residue"));
        var residues = new ArrayList<CleanupResidue>();
        if (entries != null) {
            for (var entry : entries) {
                var object = JsonTree.asObject(entry);
                if (object != null) {
                    residues.add(CleanupResidue.fromMap(object));
                }
            }
        }
        return residues;
    }

    /**
     * Renders the durations in force: the one figure of every kind with the reminder where the
     * kinds agree ({@code system.cleanup.durations.summary}), and each kind with its own value where
     * they differ or one is never offered.
     *
     * @param locale    the interface locale
     * @param durations the durations the runtime names
     * @return the rendered durations
     */
    static String durationsText(String locale, CleanupDurations durations) {
        var days = List.of(durations.plansAfterDays(), durations.epicsAfterDays(), durations.logsAfterDays(),
                            durations.reportsAfterDays(), durations.unenrolledAfterDays());
        if (days.stream().distinct().count() == 1 && days.getFirst() > 0) {
            return MessageCatalogue.format(locale, "system.cleanup.durations.summary",
                                Map.of("days", days.getFirst(), "remind", durations.remindAfterDays(), "own", 0));
        }
        var kinds = List.of("plans", "epics", "logs", "reports", "unenrolled");
        var parts = new ArrayList<String>();
        for (int i = 0; i < kinds.size(); i++) {
            var value = days.get(i);
            parts.add(MessageCatalogue.format(locale, "system.cleanup.durations.kind", Map.of("kind", kinds.get(i)))
                                + " " + MessageCatalogue.format(locale, "system.cleanup.durations.value",
                                Map.of("never", value < 0 ? "true" : "false", "days", Math.max(value, 0))));
        }
        parts.add(MessageCatalogue.format(locale, "system.cleanup.durations.remind_at",
                            Map.of("days", durations.remindAfterDays())));
        return String.join(", ", parts);
    }

    /**
     * Prints the residue the sweep removed by itself, as the runtime's figures per step
     * ({@code cleanup.adoc#residue}): the time of the last sweep in the heading where every step
     * names the same one, and on the step's own line otherwise, {@code not run yet} for a step
     * without one.
     *
     * @param out      the verb's writer
     * @param residues the runtime's residue per sweep step
     * @param locale   the interface locale
     */
    static void printResidue(PrintWriter out, List<CleanupResidue> residues, String locale) {
        if (residues.isEmpty()) {
            return;
        }
        var sweeps = residues.stream().map(CleanupResidue::lastSweep).distinct().toList();
        var common = sweeps.size() == 1 ? sweeps.getFirst() : null;
        var title = MessageCatalogue.format(locale, "system.cleanup.residue.title", Map.of());
        out.println(common != null ? title + " · " + lastSweepText(locale, common) : title);
        for (var residue : residues) {
            var line = new StringBuilder(String.format("  %-37s %3d",
                                MessageCatalogue.format(locale, "system.cleanup.residue.step",
                                                    Map.of("step", OutputSafety.stripControlSequences(residue.step()))),
                                residue.count()));
            if (residue.bytes() > 0) {
                line.append(" · ").append(SystemCommand.formatBytes(residue.bytes()));
            }
            if (common == null) {
                line.append(" · ").append(lastSweepText(locale, residue.lastSweep()));
            }
            out.println(line);
        }
    }

    private static String lastSweepText(String locale, @Nullable String lastSweep) {
        return lastSweep != null
                            ? MessageCatalogue.format(locale, "system.cleanup.residue.last",
                            Map.of("time", OutputSafety.stripControlSequences(lastSweep)))
                            : MessageCatalogue.format(locale, "system.cleanup.residue.not_run", Map.of());
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
            var code = switch (Decision.of(chosen.key())) {
                case DELETE -> deleteFromDialogue(out, job, leftOut);
                case REMIND -> executeRemind(out, job);
                case IGNORE -> executeIgnore(out, job);
                case LEAVE_OUT -> {
                    leaveOut(out, items, leftOut);
                    yield ASK_AGAIN;
                }
                case OPEN -> {
                    openItem(out, err, items);
                    yield ASK_AGAIN;
                }
            };
            if (code != ASK_AGAIN) {
                return code;
            }
            items = printJob(out, job, leftOut);
        }
    }

    /**
     * One decision of the bar: its key and the catalogue suffix of its label. The bar and its
     * dispatch read this one definition, so a decision can never be offered without a handler or
     * handled without being offered.
     */
    enum Decision {
        DELETE("d", "delete"),
        REMIND("r", "remind"),
        IGNORE("i", "ignore"),
        LEAVE_OUT("x", "leave_out"),
        OPEN("o", "open");

        private final String key;
        private final String labelKey;

        Decision(String key, String labelKey) {
            this.key = key;
            this.labelKey = labelKey;
        }

        /**
         * @return the key the operator types
         */
        String key() {
            return key;
        }

        /**
         * @param locale the interface locale
         * @return the rendered label
         */
        String label(String locale) {
            return MessageCatalogue.format(locale, "operator.cleanup.choose." + labelKey, Map.of());
        }

        /**
         * @param key the key of a choice the bar offered: the prompt answers only those, and the
         *            bar offers exactly the decisions
         * @return the decision
         * @throws IllegalArgumentException where the key names no decision
         */
        static Decision of(String key) {
            for (var decision : values()) {
                if (decision.key.equalsIgnoreCase(key)) {
                    return decision;
                }
            }
            throw new IllegalArgumentException("no decision of the cleanup has the key " + key);
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
        printWholeJobActionNote(out);

        int itemIndex = 1;
        for (var prj : job.projectItems()) {
            var isUnenrolled = isUnenrolled(prj.projectId());
            out.println(isUnenrolled ? "Unenrolled projects"
                                : OutputSafety.stripControlSequences(prj.projectId()));
            int shown = 0;
            for (var item : prj.items()) {
                items.add(item);
                if (shown < PAGE_SIZE) {
                    printItem(out, item, leftOut, isUnenrolled, itemIndex);
                    shown++;
                }
                itemIndex++;
            }
            if (project == null && prj.items().size() > PAGE_SIZE) {
                printMore(out, prj.items().size() - PAGE_SIZE, prj.projectId());
            }
        }

        var locale = context.clientEnvironment().locale();
        var footer = new StringBuilder(String.format("Kept for good %d (cleanup --kept)", s.keptForGoodCount()));
        if (durations != null) {
            footer.append(" · durations ").append(durationsText(locale, durations));
        }
        if (s.lastSweepAt() != null) {
            footer.append(" · ").append(lastSweepText(locale, s.lastSweepAt()));
        }
        out.println(footer);
        out.flush();
        return items;
    }

    private static boolean isUnenrolled(String projectId) {
        return "unenrolled".equalsIgnoreCase(projectId);
    }

    /**
     * The word an item is named by in front of its id ({@code 26-cleanup.adoc#commands}):
     * {@code plan:}, {@code epic:}, {@code logs:}, {@code reports:}, {@code project:}. Read reports
     * and an unenrolled project carry a longer kind on the wire than the word the operator types.
     *
     * @param kind the item's kind as the runtime sends it
     * @return the word that names the kind
     */
    static String kindWord(String kind) {
        var known = CleanupKind.fromValue(kind);
        return switch (known) {
            case READ_REPORTS -> "reports";
            case UNENROLLED_PROJECT -> "project";
            case PLAN, EPIC, LOGS -> known.value();
            case null -> kind;
        };
    }

    /**
     * Names what a project group holds beyond the page it printed, and the command that prints it
     * ({@code operator.cleanup.more}, {@code 26-cleanup.adoc#t-job}).
     *
     * @param out       the verb's writer
     * @param remaining the items the page did not print
     * @param projectId the project group they belong to
     */
    private void printMore(PrintWriter out, int remaining, String projectId) {
        var command = "cleanup --project " + OutputSafety.stripControlSequences(projectId);
        out.println("   …  " + MessageCatalogue.format(context.clientEnvironment().locale(),
                            "operator.cleanup.more", Map.of(COUNT_PARAM, remaining, "command", command)));
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
        var title = titleText(item.title());
        var outcome = item.outcome() != null ? OutputSafety.stripControlSequences(item.outcome()) : "";
        var endedAt = item.endedAt();
        var ended = endedAt != null ? endedText(endedAt) : "";
        var size = item.sizeBytes() > 0 ? SystemCommand.formatBytes(item.sizeBytes()) : "";
        var logs = item.logsCount() > 0 ? item.logsCount() + (item.logsCount() == 1 ? " log" : " logs") : "";
        var note = item.keptUntil() != null ? OutputSafety.stripControlSequences(item.keptUntil()) : "";
        var mark = leftOut.contains(item.id())
                            ? MessageCatalogue.format(context.clientEnvironment().locale(),
                            "operator.cleanup.choose.kept_for_good", Map.of())
                            : "";

        var kind = CleanupKind.fromValue(item.kind());
        final String line;
        if (kind == CleanupKind.READ_REPORTS) {
            line = String.format("   %2d)   %-18s %s", index, title, note);
        } else if (kind == CleanupKind.LOGS) {
            line = String.format("   %2d)   %-18s %-32s %6s %8s", index, title, note, size, logs);
        } else if (isUnenrolled) {
            line = String.format("  %3d) %s %-18s %-36s %s", index, glyph, title, note, size);
        } else {
            line = String.format("   %2d) %s %-18s %-17s %-14s %6s %8s", index, glyph, title, outcome, ended,
                                size, logs);
        }
        printWithMark(out, stripTrailing(line), mark);
        return index + 1;
    }

    /**
     * Prints an item line with its leave-out mark: on the line where both fit, and on a continuation
     * line aligned with the title where the mark would push it past the dialogue width.
     *
     * @param out   the verb's writer
     * @param line  the item line, without its mark
     * @param mark  the rendered mark, or the empty string
     */
    private static void printWithMark(PrintWriter out, String line, String mark) {
        if (mark.isEmpty()) {
            out.println(line);
            return;
        }
        if (line.length() + 1 + mark.length() <= MAX_LINE) {
            out.println(line + " " + mark);
            return;
        }
        out.println(line);
        out.println(" ".repeat(TITLE_COLUMN) + mark);
    }

    /**
     * Cuts a title to its column, so a long title shifts no other column: the last character it
     * keeps becomes the ellipsis that marks the cut.
     *
     * @param title the item's title
     * @return the title as the column carries it
     */
    private static String titleText(String title) {
        var text = OutputSafety.stripControlSequences(title);
        return text.length() > TITLE_WIDTH ? text.substring(0, TITLE_WIDTH - 1) + "…" : text;
    }

    private static String stripTrailing(String text) {
        var end = text.length();
        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }
        return text.substring(0, end);
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
        return Arrays.stream(Decision.values())
                            .map(decision -> ChoicePrompt.Choice.of(decision.key(), decision.label(locale)))
                            .toList();
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
        consent.title(consentTitle(preview, unchecked, locale, project != null && !".".equals(project)));
        return consent.askConsent();
    }

    /**
     * @param preview   the counts the delete would remove
     * @param unchecked how many items are kept for good
     * @param locale    the interface locale
     * @return the consent prompt's title: the preview and its own question
     */
    static String consentTitle(DeletePreview preview, int unchecked, String locale, boolean wholeJob) {
        var text = previewText(preview, unchecked);
        if (wholeJob) {
            text = text + "\n  " + MessageCatalogue.format(locale, "operator.cleanup.choose.whole_job", Map.of());
        }
        return text + "\n" + MessageCatalogue.format(locale, "operator.cleanup.choose.delete_confirm", Map.of());
    }

    private int sendDelete(PrintWriter out, CleanupJob job, List<String> uncheckedIds, DeletePreview preview) throws IOException {
        var deleteBody = JsonTree.write(Map.of("unchecked_ids", uncheckedIds));
        var res = context.runtime(true).send(
                            HttpRequest.post(CLEANUP_API + "/" + job.jobId() + "/delete").withBody(deleteBody));
        if (res.status() == 409) {
            return deleteRefused(out, res.bodyText());
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
     * Reports a delete the runtime refused with {@code 409}: a job another client already decided
     * prints the decision taken and who took it ({@code 26-cleanup.adoc#t-none}), and a runtime that
     * is stopping says what it does with every new act.
     *
     * @param out  the verb's writer
     * @param body the refusal's body
     * @return the exit code of the refusal
     */
    private int deleteRefused(PrintWriter out, String body) {
        var decided = decidedElsewhere(body);
        if (decided != null) {
            out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                "system.cleanup.decided_elsewhere",
                                Map.of("who", OutputSafety.stripControlSequences(decided.who()),
                                                    "decision", decided.decision())));
            out.flush();
            return 1;
        }
        out.println("The runtime is stopping; nothing new starts.");
        out.flush();
        return 1;
    }

    /**
     * @param body a refusal's body
     * @return the decision another client took, or {@code null} when the body names none
     */
    private static @Nullable CleanupDecidedEvent decidedElsewhere(String body) {
        try {
            var parsed = JsonTree.asObject(JsonTree.parse(body));
            if (parsed == null || JsonTree.string(parsed, "decision") == null
                                || JsonTree.string(parsed, "who") == null) {
                return null;
            }
            return CleanupDecidedEvent.fromMap(parsed);
        } catch (IOException _) {
            return null;
        }
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
            switch (CleanupKind.fromValue(item.kind())) {
                case PLAN -> plans--;
                case EPIC -> epics--;
                case UNENROLLED_PROJECT -> projects--;
                case READ_REPORTS -> reports -= item.readReportsCount() > 0 ? item.readReportsCount() : 1;
                case LOGS -> {
                    // a logs entry removes its logs, counted below
                }
                case null -> {
                    // an unknown kind subtracts none
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
            out.println(MessageCatalogue.format(locale, NOT_IN_JOB, Map.of("item", unknown)));
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
            out.println(MessageCatalogue.format(locale, NOT_IN_JOB, Map.of("item", answer)));
            out.flush();
            return;
        }
        var target = openTarget(item);
        if (target != null) {
            PmOperator.execute(context, out, err, target, workId(item));
            return;
        }
        out.println(MessageCatalogue.format(locale, "operator.cleanup.choose.open_work_only",
                            Map.of("item", OutputSafety.stripControlSequences(item.title()))));
        out.flush();
    }

    /**
     * @param item an item of the open job
     * @return the work id the item names, without the {@code kind:} prefix a cleanup identity may
     *         carry
     */
    static String workId(CleanupItemEntry item) {
        var prefix = item.kind() + ":";
        return item.id().regionMatches(true, 0, prefix, 0, prefix.length())
                            ? item.id().substring(prefix.length())
                            : item.id();
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
            if (item.id().equals(token) || (kindWord(item.kind()) + ":" + item.id()).equalsIgnoreCase(token)) {
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

    /**
     * @param kind the item's kind as the runtime sends it
     * @return the glyph of a plan, an epic, or an unenrolled project, and a blank for the rest
     */
    static String glyphForKind(String kind) {
        return switch (CleanupKind.fromValue(kind)) {
            case PLAN -> "▸";
            case EPIC -> "◇";
            case UNENROLLED_PROJECT -> "▪";
            case LOGS, READ_REPORTS -> " ";
            case null -> " ";
        };
    }
}
