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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.DecisionOption;
import de.planmarshall.api.decision.DecisionPart;
import de.planmarshall.api.decision.InterpretationType;
import de.planmarshall.api.decision.QuorumVote;
import de.planmarshall.api.decision.Reversibility;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator answer <id> [<option>]}: answers an open question, decision, or waiver request (UC-PLN-2).
 */
@Command(name = "answer", description = "Answers an open question or decision.")
@OperatorVerb
public final class AnswerCommand implements Callable<Integer> {

    private static final String ACTION_KEY = "action";
    private static final String REASON_KEY = "reason";
    private static final String CHANNEL_KEY = "channel";
    private static final String CHANNEL_CLI = "cli";
    private static final String LOCALE_DE = "de";
    private static final String GATE_KEY = "gate";
    private static final String PLAN_KEY = "plan";
    private static final String OPTION_KEY = "option";
    private static final String ROLE_KEY = "role";
    private static final String SCOPE_KEY = "scope";
    private static final String ID_KEY = "id";
    private static final String HINT_KEY = "hint";
    private static final String EVIDENCE_KEY = "evidence";
    private static final String BLOCKS_KEY = "blocks";
    private static final String REVERSIBILITY_KEY = "reversibility";
    private static final String ACTION_WAIVE = "waive";
    private static final String ACTION_DECLINE = "decline";
    private static final String ACTION_LATER = "later";
    private static final String ACTION_WITHDRAW = "withdraw";

    private static final String API_V1_DECISIONS = "/api/v1/decisions/";
    private static final String API_V1_PLANS = "/api/v1/plans/";
    private static final String PATH_ANSWER = "/answer";
    private static final String PATH_WITHDRAW = "/withdraw";
    private static final String PATH_INTERPRET = "/interpret";
    private static final String API_V1_QUESTIONS = "/api/v1/questions/";
    private static final String PATH_ANSWERS = "/answers";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Question ID (e.g. Q-007 or WR-2)")
    String questionId = "";

    @Parameters(index = "1", arity = "0..1", description = "Option chosen")
    @Nullable
    String option;

    @Option(names = "--words", description = "Answer in operator's own words")
    @Nullable
    String words;

    @Option(names = "--note", description = "Note to accompany the answer")
    @Nullable
    String note;

    @Option(names = "--reason", description = "Reason for waiver or decline")
    @Nullable
    String reason;

    @Option(names = "--action", description = "Action (answer, waive, decline, withdraw)")
    @Nullable
    String action;

    @Option(names = "--plan", description = "Scope plan ID")
    @Nullable
    String plan;

    @Option(names = {"-i", "--interactive"}, description = "Interactive prompt mode")
    boolean interactive;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    AnswerCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);
        var locale = MessageCatalogue.normalizeLocale(context.clientEnvironment().locale());

        if (option == null && (words == null || words.isBlank()) && (action == null || action.isBlank())) {
            var safeQid = OperatorUrlUtil.encodePathSegment(questionId);
            var qPath = plan != null
                                ? API_V1_PLANS + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid
                                : API_V1_DECISIONS + safeQid;
            var qResponse = runtime.send(HttpRequest.get(qPath));
            if (qResponse.status() == 404) {
                out.println(MessageCatalogue.format(locale, "operator.decision.not_found", Map.of(ID_KEY, questionId)));
                out.flush();
                return 1;
            }

            var envelope = DecisionEnvelope.fromJson(qResponse.bodyText());
            if (noInput) {
                printDecisionDetail(out, envelope, locale);
                out.println(getMessage(locale, "operator.decision.choice_required"));
                out.flush();
                return 3;
            }
            if (interactive || context.isInteractiveTerminal()) {
                return handleInteractive(runtime, out, envelope, locale);
            }
            printDecisionDetail(out, envelope, locale);
            out.flush();
            return 0;
        }

        return submitAnswer(runtime, out, locale);
    }

    private void printDecisionDetail(PrintWriter out, DecisionEnvelope envelope, String locale) {
        if (envelope.isWaiver()) {
            printWaiverDetail(out, envelope, locale);
        } else {
            printQuestionDetail(out, envelope, locale);
        }
        out.flush();
    }

    private void printWaiverDetail(PrintWriter out, DecisionEnvelope envelope, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.header.waiver", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, envelope.subject(),
                            GATE_KEY, envelope.waiverGate() != null ? envelope.waiverGate() : GATE_KEY
        )));
        if (envelope.waiverReason() != null) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        printEvidenceHintIfPresent(out, envelope.evidence(), locale);
    }

    private void printQuestionDetail(PrintWriter out, DecisionEnvelope envelope, String locale) {
        var blocksKey = "decision.blocks." + (envelope.blocks() != null ? envelope.blocks().toLowerCase() : PLAN_KEY);
        var revKey = "decision.reversibility." + (envelope.reversibility() != null ? envelope.reversibility().value() : "reversible");
        var blocksMsg = MessageCatalogue.hasKey(blocksKey) ? getMessage(locale, blocksKey) : "blocks the plan";
        var revMsg = MessageCatalogue.hasKey(revKey) ? getMessage(locale, revKey) : "can be undone";

        var headerKey = envelope.askingPhase() != null && !envelope.askingPhase().isBlank()
                            ? "operator.decision.header.question"
                            : "operator.decision.header.question_short";
        var params = new LinkedHashMap<String, Object>();
        params.put(ID_KEY, envelope.id());
        params.put(SCOPE_KEY, envelope.subject());
        if (envelope.askingPhase() != null && !envelope.askingPhase().isBlank()) {
            params.put("phase", envelope.askingPhase());
        }
        params.put(BLOCKS_KEY, blocksMsg);
        params.put(REVERSIBILITY_KEY, revMsg);
        out.println(MessageCatalogue.format(locale, headerKey, params));

        var titleOrDesc = envelope.description() != null ? envelope.description() : envelope.title();
        if (titleOrDesc != null && !titleOrDesc.isBlank()) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(titleOrDesc));
        }
        printEvidenceHintIfPresent(out, envelope.evidence(), locale);
    }

    private static void printEvidenceHintIfPresent(PrintWriter out, @Nullable Map<String, Object> evidence, String locale) {
        if (evidence != null && !evidence.isEmpty()) {
            var hint = formatEvidenceHint(evidence);
            if (!hint.isBlank()) {
                out.println("  " + MessageCatalogue.format(locale, "operator.decision.evidence_hint", Map.of(HINT_KEY, hint)));
            }
        }
    }

    static String formatEvidenceHint(@Nullable Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "";
        }
        var file = getEvidenceString(evidence, "file");
        var lines = getEvidenceString(evidence, "lines");
        var summary = getEvidenceString(evidence, "summary");
        if (file != null) {
            return formatFileEvidence(file, lines, summary);
        }
        var run = getEvidenceString(evidence, "run");
        var test = getEvidenceString(evidence, "test");
        if (run != null && test != null) {
            return test + " (" + run + ")";
        }
        if (summary != null) {
            return summary;
        }
        return formatFallbackEvidence(evidence);
    }

    private static @Nullable String getEvidenceString(Map<String, Object> evidence, String key) {
        var val = evidence.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private static String formatFileEvidence(String file, @Nullable String lines, @Nullable String summary) {
        if (lines != null && summary != null) {
            return file + ":" + lines + " (" + summary + ")";
        }
        if (lines != null) {
            return file + ":" + lines;
        }
        if (summary != null) {
            return file + " (" + summary + ")";
        }
        return file;
    }

    private static String formatFallbackEvidence(Map<String, Object> evidence) {
        return evidence.values().stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.joining(", "));
    }

    private static String formatRationale(@Nullable DecisionOption recommendedOpt, String rationale) {
        if (recommendedOpt != null && !rationale.startsWith(recommendedOpt.label())) {
            return recommendedOpt.label() + ". " + rationale;
        }
        return rationale;
    }

    private int submitAnswer(RuntimeClient runtime, PrintWriter out, String locale) throws IOException {
        var safeQid = OperatorUrlUtil.encodePathSegment(questionId);
        var effectiveAction = resolveEffectiveAction(action, option, questionId, locale);

        if (ACTION_LATER.equalsIgnoreCase(effectiveAction)) {
            out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                                ID_KEY, questionId,
                                SCOPE_KEY, PLAN_KEY,
                                "project", "pm-mcp-clients"
            )));
            out.flush();
            return 0;
        }

        if (ACTION_WITHDRAW.equalsIgnoreCase(effectiveAction)) {
            var withPath = API_V1_DECISIONS + safeQid + PATH_WITHDRAW;
            var resp = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (resp.status() == 200 || resp.status() == 202 || resp.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                                    ID_KEY, questionId,
                                    SCOPE_KEY, PLAN_KEY
                )));
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("withdraw " + questionId, resp);
        }

        if (effectiveAction != null
                            && (ACTION_WAIVE.equalsIgnoreCase(effectiveAction) || ACTION_DECLINE.equalsIgnoreCase(effectiveAction))
                            && (reason == null || reason.isBlank())) {
            out.println(getMessage(locale, "operator.decision.reason_required"));
            out.flush();
            return 3;
        }

        return submitAnswerPayload(runtime, out, safeQid, effectiveAction, locale);
    }

    private static @Nullable String resolveEffectiveAction(@Nullable String act, @Nullable String opt, String qid, String locale) {
        if (act != null) {
            return act;
        }
        if (ACTION_WAIVE.equalsIgnoreCase(opt) || ("y".equalsIgnoreCase(opt) && qid.startsWith("WR-"))) {
            return ACTION_WAIVE;
        }
        if (ACTION_DECLINE.equalsIgnoreCase(opt) || ("d".equalsIgnoreCase(opt) && qid.startsWith("WR-"))) {
            return ACTION_DECLINE;
        }
        if (ACTION_LATER.equalsIgnoreCase(opt) || "l".equalsIgnoreCase(opt)
                            || ("s".equalsIgnoreCase(opt) && LOCALE_DE.equals(locale))) {
            return ACTION_LATER;
        }
        if (ACTION_WITHDRAW.equalsIgnoreCase(opt) || "none fits".equalsIgnoreCase(opt)
                            || "x".equalsIgnoreCase(opt)) {
            return ACTION_WITHDRAW;
        }
        return null;
    }

    private int submitAnswerPayload(RuntimeClient runtime, PrintWriter out, String safeQid,
                        @Nullable String effectiveAction, String locale) throws IOException {
        DecisionEnvelope envelope = fetchEnvelopeSafe(runtime, safeQid);

        var payload = new LinkedHashMap<String, Object>();
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        if (effectiveAction != null && (ACTION_WAIVE.equalsIgnoreCase(effectiveAction) || ACTION_DECLINE.equalsIgnoreCase(effectiveAction))) {
            payload.put(ACTION_KEY, effectiveAction);
            payload.put(REASON_KEY, reason);
        } else {
            if (option != null) payload.put(OPTION_KEY, option);
            if (words != null) payload.put("words", words);
        }
        if (note != null) payload.put("note", note);

        var answerPath = plan != null
                            ? API_V1_PLANS + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid + PATH_ANSWER
                            : API_V1_DECISIONS + safeQid + PATH_ANSWER;

        var response = runtime.send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (response.status() == 404) {
            response = runtime.send(HttpRequest.json("POST", API_V1_QUESTIONS + safeQid + PATH_ANSWERS, JsonTree.write(payload)));
        }
        if (response.status() == 200 || response.status() == 202 || response.status() == 204) {
            printAnswerSuccess(out, envelope, effectiveAction, locale);
            return 0;
        }
        throw new OperatorRefusedException("answer " + questionId, response);
    }

    private @Nullable DecisionEnvelope fetchEnvelopeSafe(RuntimeClient runtime, String safeQid) {
        var qPath = plan != null
                            ? API_V1_PLANS + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid
                            : API_V1_DECISIONS + safeQid;
        try {
            var qResponse = runtime.send(HttpRequest.get(qPath));
            if (qResponse.status() == 200) {
                return DecisionEnvelope.fromJson(qResponse.bodyText());
            }
        } catch (IOException _) {
            // best-effort envelope
        }
        return null;
    }

    private void printAnswerSuccess(PrintWriter out, @Nullable DecisionEnvelope envelope,
                        @Nullable String effectiveAction, String locale) {
        if (ACTION_WAIVE.equalsIgnoreCase(effectiveAction)) {
            String planNoun = PLAN_KEY;
            if (envelope != null) {
                planNoun = extractPlanNoun(envelope);
            } else if (plan != null) {
                planNoun = plan;
            }
            out.println(MessageCatalogue.format(locale, "operator.decision.waived", Map.of(
                                GATE_KEY, envelope != null && envelope.waiverGate() != null ? envelope.waiverGate() : GATE_KEY,
                                PLAN_KEY, planNoun
            )));
        } else if (ACTION_DECLINE.equalsIgnoreCase(effectiveAction)) {
            out.println(MessageCatalogue.format(locale, "operator.decision.declined", Map.of(ID_KEY, questionId)));
        } else {
            String label;
            if (words != null && !words.isBlank()) {
                label = words;
            } else if (envelope != null && option != null) {
                label = findOptionLabel(envelope, option);
            } else {
                label = option != null ? option : "";
            }
            out.println(MessageCatalogue.format(locale, "operator.decision.answered", Map.of(
                                ID_KEY, questionId,
                                "label", label
            )));
        }
        out.flush();
    }

    private static @Nullable String readLineSafe(LineReader reader, String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (UserInterruptException _) {
            // The interrupt is the exit-code table's 130, never an unanswered question.
            throw new StartPrompt.InterruptException();
        } catch (EndOfFileException _) {
            return null;
        }
    }

    private int handleInteractive(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope, String locale) {
        try (var lease = context.openTerminal()) {
            var terminal = lease.terminal();
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            if (envelope.isWaiver()) {
                return runInteractiveWaiver(runtime, out, reader, envelope, locale);
            }
            return runInteractiveDecision(runtime, out, reader, envelope, locale);
        } catch (StartPrompt.InterruptException interrupt) {
            throw interrupt;
        } catch (IOException e) {
            out.println("Error reading interactive input: " + e.getMessage());
            out.flush();
            return 1;
        }
    }

    private int runInteractiveWaiver(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String locale) throws IOException {
        out.println(MessageCatalogue.format(locale, "operator.decision.header.waiver", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, envelope.subject(),
                            GATE_KEY, envelope.waiverGate() != null ? envelope.waiverGate() : GATE_KEY
        )));
        if (envelope.waiverReason() != null) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        printEvidenceHintIfPresent(out, envelope.evidence(), locale);
        out.println();
        out.flush();

        var prompt = getMessage(locale, "operator.decision.waiver_prompt_full");
        while (true) {
            var choice = readLineSafe(reader, prompt);
            if (choice == null) {
                return 0;
            }
            if (choice.isBlank()) {
                printWaiverNeedsAnswer(out, locale);
                continue;
            }
            choice = choice.trim();

            if (isQuitChoice(choice)) {
                printLeftDialogue(out, envelope.id(), locale);
                return 0;
            }
            if (isLaterChoice(choice, locale)) {
                printLaterDone(out, envelope, locale);
                return 0;
            }
            if ("?".equalsIgnoreCase(choice)) {
                printEvidenceDetails(out, envelope.evidence(), locale);
                continue;
            }
            if (isWaiverActChoice(choice, locale)) {
                return executeWaiverAct(runtime, out, reader, envelope, choice, locale);
            }
            printWaiverNeedsAnswer(out, locale);
        }
    }

    private static boolean isQuitChoice(String choice) {
        return "q".equalsIgnoreCase(choice) || "v".equalsIgnoreCase(choice);
    }

    private static void printLeftDialogue(PrintWriter out, String id, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.dialogue.left", Map.of("state", id)));
        out.flush();
    }

    private static void printLaterDone(PrintWriter out, DecisionEnvelope envelope, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, extractScopeNoun(envelope),
                            "project", extractProjectNoun(envelope)
        )));
        out.flush();
    }

    private static void printWaiverNeedsAnswer(PrintWriter out, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.waiver_needs_answer", Map.of(
                            ACTION_WAIVE, "y", ACTION_DECLINE, "d", ACTION_LATER, LOCALE_DE.equals(locale) ? "s" : "l")));
        out.flush();
    }

    private static boolean isLaterChoice(String choice, String locale) {
        return "l".equalsIgnoreCase(choice) || ACTION_LATER.equalsIgnoreCase(choice)
                            || ("s".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale))
                            || ("später".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale));
    }

    private static boolean isWaiverActChoice(String choice, String locale) {
        return "y".equalsIgnoreCase(choice) || "d".equalsIgnoreCase(choice)
                            || ACTION_WAIVE.equalsIgnoreCase(choice) || ACTION_DECLINE.equalsIgnoreCase(choice)
                            || ("g".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale))
                            || ("a".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale));
    }

    private int executeWaiverAct(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String choice, String locale) throws IOException {
        String reqReason = promptWaiverReason(reader, out, locale);
        if (reqReason == null) {
            return 0;
        }
        var act = "d".equalsIgnoreCase(choice) || ACTION_DECLINE.equalsIgnoreCase(choice) || "a".equalsIgnoreCase(choice)
                            ? ACTION_DECLINE : ACTION_WAIVE;
        return sendWaiverAnswer(runtime, out, envelope, act, reqReason, locale);
    }

    private static @Nullable String promptWaiverReason(LineReader reader, PrintWriter out, String locale) {
        String reqReason = "";
        while (reqReason.isBlank()) {
            var line = readLineSafe(reader, getMessage(locale, "operator.decision.reason_prompt") + " › ");
            if (line == null) {
                return null;
            }
            if (line.isBlank()) {
                out.println(getMessage(locale, "operator.decision.reason_required"));
                out.flush();
            } else {
                reqReason = line.trim();
            }
        }
        return reqReason;
    }

    private int sendWaiverAnswer(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope,
                        String act, String reqReason, String locale) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put(ACTION_KEY, act);
        payload.put(REASON_KEY, reqReason);
        payload.put(CHANNEL_KEY, CHANNEL_CLI);

        var ansPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_ANSWER;
        var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
        if (ansRes.status() == 200 || ansRes.status() == 202 || ansRes.status() == 204) {
            if (ACTION_DECLINE.equals(act)) {
                out.println(MessageCatalogue.format(locale, "operator.decision.declined", Map.of(ID_KEY, envelope.id())));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.decision.waived", Map.of(
                                    GATE_KEY, envelope.waiverGate() != null ? envelope.waiverGate() : GATE_KEY,
                                    PLAN_KEY, extractPlanNoun(envelope)
                )));
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + envelope.id(), ansRes);
    }

    private static void printEvidenceDetails(PrintWriter out, @Nullable Map<String, Object> evidence, String locale) {
        var ev = formatEvidenceHint(evidence);
        if (!ev.isBlank()) {
            out.println(MessageCatalogue.format(locale, "operator.decision.evidence_details", Map.of(EVIDENCE_KEY, ev)));
        } else {
            out.println(getMessage(locale, "operator.decision.evidence_none"));
        }
        out.flush();
    }

    private int runInteractiveDecision(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String locale) throws IOException {
        printQuestionDetail(out, envelope, locale);
        out.println();

        var part = envelope.parts().isEmpty() ? null : envelope.parts().get(0);
        DecisionOption recommendedOpt = renderOptionsAndSupport(out, part, locale);

        String currentNote = null;
        var isReversible = envelope.reversibility() == Reversibility.REVERSIBLE;
        int optCount = part != null ? part.options().size() : 0;
        String rangeStr = optCount > 1 ? "1-" + optCount : "1";

        while (true) {
            var prompt = buildDecisionPrompt(recommendedOpt, isReversible, optCount, rangeStr, currentNote, locale);
            var line = readLineSafe(reader, prompt);
            if (line == null) {
                return 0;
            }
            var choice = line.trim();

            if (choice.isEmpty() && recommendedOpt != null && isReversible) {
                return submitChoice(runtime, out, envelope, recommendedOpt.key(), recommendedOpt.label(), currentNote, locale);
            }
            if (isQuitChoice(choice)) {
                printLeftDialogue(out, envelope.id(), locale);
                return 0;
            }
            if (isLaterChoice(choice, locale)) {
                printLaterDone(out, envelope, locale);
                return 0;
            }
            if (isNoneFitsChoice(choice, locale)) {
                if (handleWithdrawChoice(runtime, out, reader, envelope, locale)) {
                    return 0;
                }
                continue;
            }
            if ("m".equalsIgnoreCase(choice)) {
                currentNote = promptNote(reader, locale, currentNote);
                continue;
            }
            if ("w".equalsIgnoreCase(choice)) {
                return handleInteractiveOwnWords(runtime, out, reader, envelope, locale);
            }
            if ("?".equalsIgnoreCase(choice)) {
                printEvidenceDetails(out, envelope.evidence(), locale);
                continue;
            }
            if ("?r".equalsIgnoreCase(choice) && part != null) {
                printRationaleAndDissent(out, part, recommendedOpt, locale);
                continue;
            }
            var result = tryPickOption(runtime, out, reader, envelope, part, choice, isReversible, currentNote, locale);
            if (result != null) {
                if (result.intValue() == -1) {
                    continue;
                }
                return result.intValue();
            }
            out.println(getMessage(locale, "operator.decision.invalid_choice"));
            out.flush();
        }
    }

    private static boolean isNoneFitsChoice(String choice, String locale) {
        return "x".equalsIgnoreCase(choice) || "none fits".equalsIgnoreCase(choice)
                            || ("nichts passt".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale));
    }

    private static @Nullable String promptNote(LineReader reader, String locale, @Nullable String currentNote) {
        var noteInput = readLineSafe(reader, getMessage(locale, "operator.decision.note_prompt") + " › ");
        if (noteInput != null && !noteInput.isBlank()) {
            return noteInput.trim();
        }
        return currentNote;
    }

    private @Nullable Integer tryPickOption(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, @Nullable DecisionPart part,
                        String choice, boolean isReversible, @Nullable String currentNote,
                        String locale) throws IOException {
        if (part == null) {
            return null;
        }
        var picked = part.options().stream()
                            .filter(o -> o.key().equalsIgnoreCase(choice))
                            .findFirst().orElse(null);
        if (picked != null) {
            if (!isReversible && !confirmNonReversible(reader, out, envelope, picked, locale)) {
                return -1;
            }
            return submitChoice(runtime, out, envelope, picked.key(), picked.label(), currentNote, locale);
        }
        return null;
    }

    private static @Nullable DecisionOption renderOptionsAndSupport(PrintWriter out,
                        @Nullable DecisionPart part,
                        String locale) {
        if (part == null) {
            return null;
        }
        DecisionOption recommendedOpt = null;
        var recKey = part.recommendation();
        for (var opt : part.options()) {
            var isRec = opt.key().equals(recKey);
            if (isRec) {
                recommendedOpt = opt;
            }
            var recMark = isRec ? " ←" : "  ";
            var supportStr = formatOptionSupport(opt, isRec, part, locale);
            out.printf("    %s) %-25s%s  %s%n", opt.key(), OutputSafety.stripControlSequences(opt.label()), recMark, supportStr);
        }
        printRationaleIfPresent(out, part, recommendedOpt, locale);
        printDissentIfPresent(out, part, locale);
        out.println();
        out.flush();
        return recommendedOpt;
    }

    private static String formatOptionSupport(DecisionOption opt, boolean isRec, @Nullable DecisionPart part, String locale) {
        if (opt.quorumSupport() != null) {
            var qs = opt.quorumSupport();
            var bandKey = "decision.support.band." + qs.band().value();
            var bandStr = MessageCatalogue.hasKey(bandKey) ? getMessage(locale, bandKey) : qs.band().value().replace('_', ' ');
            var votesStr = MessageCatalogue.format(locale, "decision.support.votes", Map.of(
                                "votes", qs.votesCount(),
                                "total", qs.totalVotes(),
                                "models", qs.modelsCount()
            ));
            return votesStr + "   " + bandStr;
        }
        var role = part != null ? part.recommendingRole() : null;
        if (isRec && role != null) {
            return MessageCatalogue.format(locale, "operator.decision.recommended_by", Map.of(ROLE_KEY, role));
        }
        return "";
    }

    private static void printRationaleIfPresent(PrintWriter out, DecisionPart part,
                        @Nullable DecisionOption recommendedOpt, String locale) {
        var role = part.recommendingRole();
        var rationale = part.recommendationRationale();
        if (role != null && rationale != null) {
            var rationaleOption = formatRationale(recommendedOpt, rationale);
            out.println("  " + MessageCatalogue.format(locale, "decision.recommendation.by", Map.of(
                                ROLE_KEY, role,
                                OPTION_KEY, OutputSafety.stripControlSequences(rationaleOption)
            )));
        }
    }

    private static void printDissentIfPresent(PrintWriter out, DecisionPart part, String locale) {
        for (var opt : part.options()) {
            if (opt.quorumSupport() != null && opt.quorumSupport().dissentSummary() != null) {
                var qs = opt.quorumSupport();
                var dissentRole = qs.votes().stream()
                                    .filter(v -> v.reason() != null && !v.reason().isBlank())
                                    .map(QuorumVote::role)
                                    .findFirst().orElse("quorum-b");
                out.println("  " + MessageCatalogue.format(locale, "decision.dissent", Map.of(
                                    ROLE_KEY, dissentRole,
                                    REASON_KEY, OutputSafety.stripControlSequences(qs.dissentSummary())
                )));
                break;
            }
        }
    }

    private static void printRationaleAndDissent(PrintWriter out, DecisionPart part,
                        @Nullable DecisionOption recommendedOpt, String locale) {
        printRationaleIfPresent(out, part, recommendedOpt, locale);
        printDissentIfPresent(out, part, locale);
        out.flush();
    }

    private static String buildDecisionPrompt(@Nullable DecisionOption recommendedOpt, boolean isReversible,
                        int optCount, String rangeStr, @Nullable String currentNote,
                        String locale) {
        var ownWordsStr = getMessage(locale, "operator.decision.prompt.own_words");
        var chatStr = getMessage(locale, "operator.decision.prompt.chat");
        var noneFitsStr = getMessage(locale, "operator.decision.prompt.none_fits");
        var laterStr = getMessage(locale, "operator.decision.prompt.later");

        if (recommendedOpt != null && isReversible) {
            var otherStr = optCount > 1 ? MessageCatalogue.format(locale, "operator.decision.prompt.other", Map.of("range", rangeStr)) : "";
            var noteStr = currentNote != null
                                ? "m) " + getMessage(locale, "operator.decision.note_set")
                                : getMessage(locale, "operator.decision.prompt.add_note");
            var sb = new StringBuilder();
            sb.append(recommendedOpt.label()).append("? [Enter]");
            if (!otherStr.isBlank()) sb.append("   ").append(otherStr);
            sb.append("   ").append(ownWordsStr);
            sb.append("   ").append(noteStr);
            sb.append("   ").append(chatStr);
            sb.append("   ").append(noneFitsStr);
            sb.append("   ").append(laterStr);
            sb.append("   ?   › ");
            return sb.toString();
        }
        var chooseStr = MessageCatalogue.format(locale, "operator.decision.prompt.choose", Map.of("range", rangeStr));
        return chooseStr + "   " + ownWordsStr + "   " + chatStr + "   " + noneFitsStr + "   " + laterStr + "   ?   › ";
    }

    private static boolean handleWithdrawChoice(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String locale) throws IOException {
        var confirmPrompt = MessageCatalogue.format(locale, "operator.decision.none_fits_confirm", Map.of(
                            SCOPE_KEY, extractScopeNoun(envelope),
                            "project", extractProjectNoun(envelope)
        )) + " [Enter]   " + (LOCALE_DE.equals(locale) ? "z) zurück   › " : "b) back   › ");
        var confirm = readLineSafe(reader, confirmPrompt);
        if (confirm == null) return true;
        if (confirm.isBlank() || "y".equalsIgnoreCase(confirm.trim()) || "j".equalsIgnoreCase(confirm.trim())) {
            var withPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_WITHDRAW;
            runtime.send(HttpRequest.json("POST", withPath, "{}"));
            out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                                ID_KEY, envelope.id(),
                                SCOPE_KEY, extractScopeNoun(envelope)
            )));
            out.flush();
            return true;
        }
        return false;
    }

    private static boolean confirmNonReversible(LineReader reader, PrintWriter out,
                        DecisionEnvelope envelope, DecisionOption picked,
                        String locale) {
        out.println();
        out.println(MessageCatalogue.format(locale, "operator.decision.confirm_title", Map.of(ID_KEY, envelope.id())));
        out.printf("  %s%n", OutputSafety.stripControlSequences(picked.label()));
        if (picked.consequences() != null) {
            out.println("  " + MessageCatalogue.format(locale, "operator.decision.follows", Map.of("consequence", OutputSafety.stripControlSequences(picked.consequences()))));
        }
        var confirm = readLineSafe(reader, getMessage(locale, "operator.decision.confirm_prompt"));
        if (confirm == null) return false;
        return confirm.isBlank() || "y".equalsIgnoreCase(confirm.trim()) || "j".equalsIgnoreCase(confirm.trim());
    }

    private int handleInteractiveOwnWords(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String locale) throws IOException {
        var wordsPrompt = getMessage(locale, "operator.decision.own_words_prompt") + " › ";
        var wordsInput = readLineSafe(reader, wordsPrompt);
        if (wordsInput == null || wordsInput.isBlank()) {
            return 0;
        }

        var interpPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_INTERPRET;
        var interpRes = runtime.send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of("words", wordsInput.trim()))));
        var interp = DecisionInterpretation.fromJson(interpRes.bodyText());

        if (interp.interpretationType() == InterpretationType.UNCLEAR && interp.clarificationQuestion() != null) {
            out.println(MessageCatalogue.format(locale, "decision.clarify.asks", Map.of(
                                ROLE_KEY, "interpret",
                                "question", interp.clarificationQuestion()
            )));
            var reply = readLineSafe(reader, getMessage(locale, "operator.decision.reply_prompt") + " › ");
            var combinedWords = wordsInput.trim() + ". " + (reply != null ? reply.trim() : "");
            interpRes = runtime.send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of("words", combinedWords))));
            interp = DecisionInterpretation.fromJson(interpRes.bodyText());
        }

        out.println();
        var reading = interp.readBackText() != null ? interp.readBackText() : wordsInput.trim();
        out.println(MessageCatalogue.format(locale, "decision.readback.understood", Map.of("reading", reading)));
        var confirm = readLineSafe(reader, getMessage(locale, "operator.decision.readback_confirm"));
        if (confirm == null || (!confirm.isBlank() && !"y".equalsIgnoreCase(confirm.trim()) && !"j".equalsIgnoreCase(confirm.trim()))) {
            return 0;
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("words", wordsInput.trim());
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        var ansPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_ANSWER;
        runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));

        if (interp.readBackText() != null) {
            out.println(MessageCatalogue.format(locale, "operator.decision.answered", Map.of(
                                ID_KEY, envelope.id(),
                                "label", interp.readBackText()
            )));
        } else {
            out.println(MessageCatalogue.format(locale, "operator.decision.answered_own_words", Map.of(ID_KEY, envelope.id())));
        }
        out.flush();
        return 0;
    }

    private int submitChoice(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope,
                        String key, String label, @Nullable String noteText, String locale) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put(OPTION_KEY, key);
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        if (noteText != null) payload.put("note", noteText);

        var ansPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_ANSWER;
        var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
        if (ansRes.status() == 200 || ansRes.status() == 202 || ansRes.status() == 204) {
            var noteSuffix = noteText != null && !noteText.isBlank() ? " " + getMessage(locale, "operator.decision.with_note") : "";
            var goesOn = MessageCatalogue.format(locale, "operator.decision.goes_on", Map.of(
                                SCOPE_KEY, extractScopeNoun(envelope),
                                "project", extractProjectNoun(envelope)
            ));
            out.println(MessageCatalogue.format(locale, "operator.decision.answered_goes_on", Map.of(
                                ID_KEY, envelope.id(),
                                "label", label,
                                "note", noteSuffix,
                                "goes_on", goesOn
            )));
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + envelope.id(), ansRes);
    }

    private static String findOptionLabel(DecisionEnvelope envelope, String key) {
        if (!envelope.parts().isEmpty()) {
            for (var part : envelope.parts()) {
                for (var opt : part.options()) {
                    if (opt.key().equalsIgnoreCase(key)) {
                        return OutputSafety.stripControlSequences(opt.label());
                    }
                }
            }
        }
        return key;
    }

    private static String extractScopeNoun(DecisionEnvelope envelope) {
        var subj = envelope.subject().toLowerCase();
        if (subj.startsWith("epic")) return "epic";
        if (subj.contains("setup")) return "setup";
        if (subj.contains("start")) return "start";
        return PLAN_KEY;
    }

    private static String extractPlanNoun(DecisionEnvelope envelope) {
        var subj = envelope.subject();
        if (subj.startsWith("plan ")) {
            return subj.substring("plan ".length()).trim();
        }
        return subj;
    }

    private static String extractProjectNoun(DecisionEnvelope envelope) {
        var subj = envelope.subject();
        if (subj.contains("pm-mcp-core")) return "pm-mcp-core";
        if (subj.contains("plan-marshall")) return "plan-marshall";
        return "pm-mcp-clients";
    }

    private static String getMessage(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }
}
