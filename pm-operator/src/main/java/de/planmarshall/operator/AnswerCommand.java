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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.decision.AnswerType;
import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.DecisionOption;
import de.planmarshall.api.decision.DecisionPart;
import de.planmarshall.api.decision.InterpretationType;
import de.planmarshall.api.decision.QuorumBand;
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
    private static final String LABEL_KEY = "label";

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
        var out = new SanitizingPrintWriter(spec.commandLine().getOut());
        var runtime = context.runtime(true);
        var locale = MessageCatalogue.normalizeLocale(context.clientEnvironment().locale());

        if (option == null && (words == null || words.isBlank()) && (action == null || action.isBlank())) {
            var safeQid = OperatorUrlUtil.encodePathSegment(questionId);
            var qPath = resolveDecisionPath(safeQid);
            var qResponse = runtime.send(HttpRequest.get(qPath));
            if (qResponse.status() == 404) {
                out.println(MessageCatalogue.format(locale, "operator.decision.not_found", Map.of(ID_KEY, questionId)));
                out.flush();
                return 1;
            }

            var envelope = DecisionEnvelope.fromJson(qResponse.bodyText());
            if (noInput) {
                printDecisionDetail(out, envelope, locale);
                if (!envelope.isWaiver()) {
                    for (var part : effectiveParts(envelope)) {
                        out.println(MessageCatalogue.format(locale, "operator.decision.prompt.choose",
                                            Map.of("range", optionRange(part))));
                    }
                }
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
            var parts = effectiveParts(envelope);
            for (int i = 0; i < parts.size(); i++) {
                var part = parts.get(i);
                printPartPosition(out, part, i, parts.size(), locale);
                renderOptions(out, part, stakeOf(envelope, part), locale);
            }
        }
        out.flush();
    }

    private void printWaiverDetail(PrintWriter out, DecisionEnvelope envelope, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.header.waiver", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, envelope.subject(),
                            GATE_KEY, waiverGate(envelope, locale)
        )));
        if (envelope.waiverReason() != null) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        printEvidenceHintIfPresent(out, envelope.evidence(), locale);
    }

    private void printQuestionDetail(PrintWriter out, DecisionEnvelope envelope, String locale) {
        var blocksKey = "decision.blocks." + (envelope.blocks() != null ? envelope.blocks().toLowerCase() : PLAN_KEY);
        var revKey = "decision.reversibility." + (envelope.reversibility() != null ? envelope.reversibility().value() : "reversible");
        var blocksMsg = MessageCatalogue.hasKey(blocksKey) ? getMessage(locale, blocksKey)
                            : getMessage(locale, "decision.blocks.plan");
        var revMsg = MessageCatalogue.hasKey(revKey) ? getMessage(locale, revKey)
                            : getMessage(locale, "decision.reversibility.reversible");

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
            var laterEnvelope = fetchEnvelopeSafe(runtime, safeQid);
            var laterPath = API_V1_DECISIONS + safeQid + PATH_ANSWER;
            var laterPayload = new LinkedHashMap<String, Object>();
            laterPayload.put(ACTION_KEY, ACTION_LATER);
            laterPayload.put(CHANNEL_KEY, CHANNEL_CLI);
            var laterResponse = runtime.send(HttpRequest.json("POST", laterPath, JsonTree.write(laterPayload)));
            if (!isAccepted(laterResponse.status())) {
                throw new OperatorRefusedException("later " + questionId, laterResponse);
            }
            out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                                ID_KEY, questionId,
                                SCOPE_KEY, scopeSelector(laterEnvelope),
                                "project", projectNoun(laterEnvelope, locale)
            )));
            out.flush();
            return 0;
        }

        if (ACTION_WITHDRAW.equalsIgnoreCase(effectiveAction)) {
            var withdrawEnvelope = fetchEnvelopeSafe(runtime, safeQid);
            var withPath = API_V1_DECISIONS + safeQid + PATH_WITHDRAW;
            var resp = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (isAccepted(resp.status())) {
                out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                                    ID_KEY, questionId,
                                    SCOPE_KEY, scopeSelector(withdrawEnvelope),
                                    "project", projectNoun(withdrawEnvelope, locale)
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

        if (envelope != null && envelope.parts().size() > 1 && (option != null || (words != null && !words.isBlank()))) {
            out.println(MessageCatalogue.format(locale, "operator.decision.parts_direct_refused", Map.of(
                                ID_KEY, questionId,
                                "count", envelope.parts().size())));
            out.flush();
            return 3;
        }

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

        var answerPath = resolveDecisionPath(safeQid) + PATH_ANSWER;

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

    private String resolveDecisionPath(String safeQid) {
        if (plan != null) {
            return API_V1_PLANS + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid;
        }
        return API_V1_DECISIONS + safeQid;
    }

    private @Nullable DecisionEnvelope fetchEnvelopeSafe(RuntimeClient runtime, String safeQid) {
        var qPath = resolveDecisionPath(safeQid);
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
            printWaivedSuccess(out, envelope, locale);
        } else if (ACTION_DECLINE.equalsIgnoreCase(effectiveAction)) {
            out.println(MessageCatalogue.format(locale, "operator.decision.declined", Map.of(ID_KEY, questionId)));
        } else {
            printDirectAnswerSuccess(out, envelope, locale);
        }
        out.flush();
    }

    private void printWaivedSuccess(PrintWriter out, @Nullable DecisionEnvelope envelope, String locale) {
        String resolvedPlan = envelope != null ? planNoun(envelope, locale)
                            : (plan != null ? plan : getMessage(locale, "decision.waiver.plan_fallback"));
        out.println(MessageCatalogue.format(locale, "operator.decision.waived", Map.of(
                            GATE_KEY, envelope != null ? waiverGate(envelope, locale) : getMessage(locale, "decision.waiver.gate_fallback"),
                            PLAN_KEY, resolvedPlan
        )));
    }

    private void printDirectAnswerSuccess(PrintWriter out, @Nullable DecisionEnvelope envelope, String locale) {
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
                            LABEL_KEY, label
        )));
    }

    private static @Nullable String readLineSafe(LineReader reader, String prompt) {
        try {
            return reader.readLine(TerminalSanitizer.sanitizeLine(prompt));
        } catch (UserInterruptException _) {
            // The interrupt is the exit-code table's 130, never an unanswered question.
            throw new StartPrompt.InterruptException();
        } catch (EndOfFileException _) {
            return null;
        }
    }

    private record DialogueSession(RuntimeClient runtime, PrintWriter out, LineReader reader, String locale) {
    }

    private enum DecisionStake {
        /** Reversible, the votes agree or there is no quorum: the recommendation is preselected. */
        CONSENSUS,
        /** Reversible, the votes are split or have no consensus: nothing is preselected. */
        SPLIT,
        /** Costly or irreversible: the recommendation waits behind {@code ?r}. */
        COSTLY
    }

    private enum PartSignal {
        ANSWER,
        NOTE,
        BACK,
        DONE
    }

    private record PartAnswer(String value, boolean ownWords) {
    }

    private record PartOutcome(PartSignal signal, @Nullable PartAnswer answer, @Nullable String note, int exitCode) {
    }

    private record ConfirmOutcome(boolean submit, @Nullable Integer reopenIndex, int exitCode) {
    }

    private int handleInteractive(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope,
                        String locale) throws IOException {
        try (var lease = context.openTerminal()) {
            var terminal = lease.terminal();
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var session = new DialogueSession(runtime, out, reader, locale);
            if (envelope.isWaiver()) {
                return runInteractiveWaiver(session, envelope);
            }
            return runInteractiveDecision(session, envelope);
        } catch (StartPrompt.InterruptException interrupt) {
            throw interrupt;
        } catch (OperatorRefusedException refused) {
            // An answer the runtime refused is the design's refusal, never an input error.
            throw refused;
        } catch (IOException e) {
            out.println(MessageCatalogue.format(locale, "operator.decision.input_error",
                                Map.of("message", TerminalSanitizer.sanitizeLine(e.getMessage()))));
            out.flush();
            return 1;
        }
    }

    private int runInteractiveWaiver(DialogueSession session, DecisionEnvelope envelope) throws IOException {
        session.out().println(MessageCatalogue.format(session.locale(), "operator.decision.header.waiver", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, envelope.subject(),
                            GATE_KEY, waiverGate(envelope, session.locale())
        )));
        if (envelope.waiverReason() != null) {
            session.out().printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        printEvidenceHintIfPresent(session.out(), envelope.evidence(), session.locale());
        session.out().println();
        session.out().flush();

        var prompt = getMessage(session.locale(), "operator.decision.waiver_prompt_full");
        while (true) {
            var choice = readLineSafe(session.reader(), prompt);
            if (choice == null) {
                return 0;
            }
            if (choice.isBlank()) {
                printWaiverNeedsAnswer(session.out(), session.locale());
                continue;
            }
            choice = choice.trim();

            if (isQuitChoice(choice)) {
                printLeftDialogue(session.out(), envelope.id(), session.locale());
                return 0;
            }
            if (isLaterChoice(choice, session.locale())) {
                printLaterDone(session.out(), envelope, session.locale());
                return 0;
            }
            if ("?".equalsIgnoreCase(choice)) {
                printEvidenceDetails(session.out(), envelope.evidence(), session.locale());
                continue;
            }
            if (isWaiverActChoice(choice, session.locale())) {
                return executeWaiverAct(session, envelope, choice);
            }
            printWaiverNeedsAnswer(session.out(), session.locale());
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
                            SCOPE_KEY, scopeSelector(envelope),
                            "project", projectNoun(envelope, locale)
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

    private int executeWaiverAct(DialogueSession session, DecisionEnvelope envelope, String choice) throws IOException {
        String reqReason = promptWaiverReason(session.reader(), session.out(), session.locale());
        if (reqReason == null) {
            return 0;
        }
        var act = "d".equalsIgnoreCase(choice) || ACTION_DECLINE.equalsIgnoreCase(choice) || "a".equalsIgnoreCase(choice)
                            ? ACTION_DECLINE : ACTION_WAIVE;
        return sendWaiverAnswer(session.runtime(), session.out(), envelope, act, reqReason, session.locale());
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
                                    GATE_KEY, waiverGate(envelope, locale),
                                    PLAN_KEY, planNoun(envelope, locale)
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

    private int runInteractiveDecision(DialogueSession session, DecisionEnvelope envelope) throws IOException {
        printQuestionDetail(session.out(), envelope, session.locale());
        session.out().println();

        var parts = effectiveParts(envelope);
        var answers = new LinkedHashMap<Integer, PartAnswer>();
        String currentNote = null;
        int index = 0;

        while (true) {
            while (index < parts.size()) {
                var part = parts.get(index);
                var stake = stakeOf(envelope, part);
                var recommended = recommendedOption(part);
                printPartPosition(session.out(), part, index, parts.size(), session.locale());
                renderOptions(session.out(), part, stake, session.locale());

                var outcome = askPart(session, envelope, part, index, stake, recommended, answers.get(index), currentNote);
                switch (outcome.signal()) {
                    case ANSWER -> {
                        answers.put(index, outcome.answer());
                        index++;
                    }
                    case NOTE -> currentNote = outcome.note();
                    case BACK -> {
                        if (index > 0) {
                            index--;
                        }
                    }
                    case DONE -> {
                        return outcome.exitCode();
                    }
                }
            }

            var needsConfirm = parts.size() > 1 || envelope.reversibility() != Reversibility.REVERSIBLE;
            if (!needsConfirm) {
                return submitAnswers(session, envelope, parts, answers, currentNote);
            }
            var confirm = confirmAnswers(session, envelope, parts, answers);
            if (confirm.submit()) {
                return submitAnswers(session, envelope, parts, answers, currentNote);
            }
            if (confirm.reopenIndex() == null) {
                return confirm.exitCode();
            }
            index = confirm.reopenIndex();
            for (var key : List.copyOf(answers.keySet())) {
                if (key >= index) {
                    answers.remove(key);
                }
            }
        }
    }

    private PartOutcome askPart(DialogueSession session, DecisionEnvelope envelope, DecisionPart part,
                        int index, DecisionStake stake, @Nullable DecisionOption recommended,
                        @Nullable PartAnswer previous, @Nullable String currentNote) throws IOException {
        var out = session.out();
        var locale = session.locale();
        while (true) {
            var prompt = buildPartPrompt(part, stake, recommended, index, currentNote, locale);
            var line = readLineSafe(session.reader(), prompt);
            if (line == null) {
                return new PartOutcome(PartSignal.DONE, null, currentNote, 0);
            }
            var choice = line.trim();

            if ("?".equals(choice)) {
                printEvidenceDetails(out, envelope.evidence(), locale);
                continue;
            }
            if ("?r".equalsIgnoreCase(choice)) {
                printRationaleAndDissent(out, part, recommended, locale);
                continue;
            }
            if (isVoteView(choice, part)) {
                printVotes(out, part, choice.substring(1), locale);
                continue;
            }
            if (index > 0 && isBackChoice(choice, locale)) {
                return new PartOutcome(PartSignal.BACK, null, currentNote, 0);
            }
            if ("m".equalsIgnoreCase(choice) && part.allowNote()) {
                return new PartOutcome(PartSignal.NOTE, null, promptNote(session.reader(), locale, currentNote), 0);
            }
            if ("w".equalsIgnoreCase(choice) && part.allowOwnWords()) {
                var words = readOwnWords(session, envelope, locale);
                if (words != null) {
                    return new PartOutcome(PartSignal.ANSWER, new PartAnswer(words.value(), true), currentNote, 0);
                }
                continue;
            }
            if (isQuitChoice(choice)) {
                printLeftDialogue(out, envelope.id(), locale);
                return new PartOutcome(PartSignal.DONE, null, currentNote, 0);
            }
            if (isLaterChoice(choice, locale)) {
                printLaterDone(out, envelope, locale);
                return new PartOutcome(PartSignal.DONE, null, currentNote, 0);
            }
            if (isNoneFitsChoice(choice, locale)) {
                boolean done = handleWithdrawChoice(session.runtime(), out, session.reader(), envelope, locale);
                if (done) {
                    return new PartOutcome(PartSignal.DONE, null, currentNote, 0);
                }
                continue;
            }
            if (choice.isEmpty()) {
                if (previous != null) {
                    return new PartOutcome(PartSignal.ANSWER, previous, currentNote, 0);
                }
                if (stake == DecisionStake.CONSENSUS && recommended != null) {
                    return new PartOutcome(PartSignal.ANSWER, new PartAnswer(recommended.key(), false), currentNote, 0);
                }
                out.println(getMessage(locale, "operator.decision.invalid_choice"));
                out.flush();
                continue;
            }
            var picked = pickOption(part, choice);
            if (picked != null) {
                return new PartOutcome(PartSignal.ANSWER, new PartAnswer(picked.key(), false), currentNote, 0);
            }
            out.println(getMessage(locale, "operator.decision.invalid_choice"));
            out.flush();
        }
    }

    private static void printPartPosition(PrintWriter out, DecisionPart part, int index, int total, String locale) {
        var question = TerminalSanitizer.sanitizeLine(part.question());
        if (total > 1) {
            out.println("[" + MessageCatalogue.format(locale, "decision.part.position",
                                Map.of("n", index + 1, "total", total)) + "] " + question);
            if (part.dependency() != null && !part.dependency().isBlank()) {
                out.println("          " + TerminalSanitizer.sanitizeLine(part.dependency()));
            }
        } else if (!question.isBlank()) {
            out.println("  " + question);
        }
        out.flush();
    }

    private static void renderOptions(PrintWriter out, DecisionPart part, DecisionStake stake, String locale) {
        var recommended = recommendedOption(part);
        for (var opt : part.options()) {
            var isRec = stake != DecisionStake.COSTLY
                                && recommended != null && opt.key().equalsIgnoreCase(recommended.key());
            var recMark = isRec ? " ←" : "  ";
            var supportStr = formatOptionSupport(opt, isRec, part, locale);
            out.printf("    %s) %-25s%s  %s%n", opt.key(), TerminalSanitizer.sanitizeLine(opt.label()), recMark, supportStr);
        }
        if (stake == DecisionStake.COSTLY) {
            if (recommended != null) {
                out.println("  " + getMessage(locale, "decision.recommendation.on_request"));
            }
        } else {
            printRationaleIfPresent(out, part, recommended, locale);
            printDissentIfPresent(out, part, recommended, locale);
        }
        out.println();
        out.flush();
    }

    private static String formatOptionSupport(DecisionOption opt, boolean isRec, DecisionPart part, String locale) {
        var qs = opt.quorumSupport();
        if (qs == null) {
            var role = part.recommendingRole();
            if (isRec && role != null) {
                return MessageCatalogue.format(locale, "operator.decision.recommended_by",
                                    Map.of(ROLE_KEY, TerminalSanitizer.sanitizeLine(role)));
            }
            return getMessage(locale, "decision.support.not_voted");
        }
        var bandKey = "decision.support.band." + qs.band().value();
        var bandStr = MessageCatalogue.hasKey(bandKey) ? getMessage(locale, bandKey) : qs.band().value().replace('_', ' ');
        var votesStr = MessageCatalogue.format(locale, "decision.support.votes", Map.of(
                            "votes", qs.votesCount(),
                            "total", qs.totalVotes(),
                            "models", qs.modelsCount()
        ));
        return votesStr + "   " + bandStr;
    }

    private static void printRationaleIfPresent(PrintWriter out, DecisionPart part,
                        @Nullable DecisionOption recommendedOpt, String locale) {
        var role = part.recommendingRole();
        var rationale = part.recommendationRationale();
        if (role != null && rationale != null) {
            var rationaleOption = formatRationale(recommendedOpt, rationale);
            out.println("  " + MessageCatalogue.format(locale, "decision.recommendation.by", Map.of(
                                ROLE_KEY, role,
                                OPTION_KEY, TerminalSanitizer.sanitizeLine(rationaleOption)
            )));
        }
    }

    private record Dissent(String role, String reason) {
    }

    private static void printDissentIfPresent(PrintWriter out, DecisionPart part,
                        @Nullable DecisionOption recommendedOpt, String locale) {
        var dissent = dissentOf(part, recommendedOpt);
        if (dissent == null) {
            return;
        }
        out.println("  " + MessageCatalogue.format(locale, "decision.dissent", Map.of(
                            ROLE_KEY, TerminalSanitizer.sanitizeLine(dissent.role()),
                            REASON_KEY, TerminalSanitizer.sanitizeLine(dissent.reason())
        )));
    }

    private static @Nullable Dissent dissentOf(DecisionPart part, @Nullable DecisionOption recommendedOpt) {
        if (recommendedOpt == null) {
            return null;
        }
        // The runtime's own dissent summary wins, whichever option carries it; the role is a vote
        // that disagrees with the recommendation, never an invented placeholder role.
        var recommendedSummary = recommendedOpt.quorumSupport() != null
                            ? recommendedOpt.quorumSupport().dissentSummary() : null;
        if (recommendedSummary != null) {
            var disagreeing = part.options().stream()
                                .filter(opt -> !opt.key().equalsIgnoreCase(recommendedOpt.key()))
                                .filter(opt -> opt.quorumSupport() != null)
                                .flatMap(opt -> opt.quorumSupport().votes().stream())
                                .filter(vote -> vote.reason() != null && !vote.reason().isBlank())
                                .map(QuorumVote::role)
                                .findFirst();
            if (disagreeing.isPresent()) {
                return new Dissent(disagreeing.get(), recommendedSummary);
            }
            var matching = allVotes(part).stream()
                                .filter(vote -> vote.reason() != null && vote.reason().trim().equalsIgnoreCase(recommendedSummary.trim()))
                                .map(QuorumVote::role)
                                .findFirst();
            if (matching.isPresent()) {
                return new Dissent(matching.get(), recommendedSummary);
            }
        }
        for (var option : part.options()) {
            if (option.key().equalsIgnoreCase(recommendedOpt.key())) {
                continue;
            }
            var support = option.quorumSupport();
            if (support == null || support.dissentSummary() == null) {
                continue;
            }
            var role = support.votes().stream()
                                .filter(vote -> vote.reason() != null && !vote.reason().isBlank())
                                .map(QuorumVote::role)
                                .findFirst()
                                .orElse(null);
            if (role != null) {
                return new Dissent(role, support.dissentSummary());
            }
        }
        // No summary: name the dissenter with the reason that voter actually gave.
        return part.options().stream()
                            .filter(opt -> !opt.key().equalsIgnoreCase(recommendedOpt.key()))
                            .filter(opt -> opt.quorumSupport() != null)
                            .flatMap(opt -> opt.quorumSupport().votes().stream())
                            .filter(vote -> vote.reason() != null && !vote.reason().isBlank())
                            .findFirst()
                            .map(vote -> new Dissent(vote.role(), vote.reason()))
                            .orElse(null);
    }

    private static List<QuorumVote> allVotes(DecisionPart part) {
        return part.options().stream()
                            .map(DecisionOption::quorumSupport)
                            .filter(Objects::nonNull)
                            .flatMap(support -> support.votes().stream())
                            .toList();
    }

    private static void printRationaleAndDissent(PrintWriter out, DecisionPart part,
                        @Nullable DecisionOption recommendedOpt, String locale) {
        printRationaleIfPresent(out, part, recommendedOpt, locale);
        printDissentIfPresent(out, part, recommendedOpt, locale);
        out.flush();
    }

    private static void printVotes(PrintWriter out, DecisionPart part, String key, String locale) {
        var option = pickOption(part, key);
        if (option == null) {
            return;
        }
        var support = option.quorumSupport();
        if (support == null || support.votes().isEmpty()) {
            out.println("  " + MessageCatalogue.format(locale, "operator.decision.vote_none", Map.of(
                                OPTION_KEY, option.key(), LABEL_KEY, TerminalSanitizer.sanitizeLine(option.label()))));
            out.flush();
            return;
        }
        out.println("  " + MessageCatalogue.format(locale, "operator.decision.votes_for", Map.of(
                            OPTION_KEY, option.key(), LABEL_KEY, TerminalSanitizer.sanitizeLine(option.label()))));
        for (var vote : support.votes()) {
            var reason = vote.reason() != null ? TerminalSanitizer.sanitizeLine(vote.reason()) : "";
            out.println(MessageCatalogue.format(locale, "operator.decision.vote_line", Map.of(
                                ROLE_KEY, TerminalSanitizer.sanitizeLine(vote.role()),
                                "model", TerminalSanitizer.sanitizeLine(vote.model()),
                                "reason", reason)));
            if (vote.confidencePercent() != null) {
                out.println("    " + MessageCatalogue.format(locale, "decision.support.self_reported",
                                    Map.of("value", vote.confidencePercent())));
            }
        }
        out.flush();
    }

    private static boolean isVoteView(String choice, DecisionPart part) {
        return choice.length() > 1 && choice.charAt(0) == '?'
                            && !"?r".equalsIgnoreCase(choice)
                            && pickOption(part, choice.substring(1)) != null;
    }

    private static boolean isBackChoice(String choice, String locale) {
        return "b".equalsIgnoreCase(choice) || "back".equalsIgnoreCase(choice)
                            || ("z".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale))
                            || ("zurück".equalsIgnoreCase(choice) && LOCALE_DE.equals(locale));
    }

    private static String buildPartPrompt(DecisionPart part, DecisionStake stake,
                        @Nullable DecisionOption recommended, int index, @Nullable String currentNote,
                        String locale) {
        var rangeStr = optionRange(part);
        var sb = new StringBuilder();
        if (stake == DecisionStake.CONSENSUS && recommended != null) {
            sb.append(TerminalSanitizer.sanitizeLine(recommended.label())).append("? [Enter]");
            if (part.options().size() > 1) {
                sb.append("   ").append(MessageCatalogue.format(locale, "operator.decision.prompt.other", Map.of("range", rangeStr)));
            }
        } else {
            sb.append(MessageCatalogue.format(locale, "operator.decision.prompt.choose", Map.of("range", rangeStr)));
        }
        if (part.allowOwnWords()) {
            sb.append("   ").append(getMessage(locale, "operator.decision.prompt.own_words"));
        }
        if (part.allowNote()) {
            sb.append("   ").append(currentNote != null
                                ? "m) " + getMessage(locale, "operator.decision.note_set")
                                : getMessage(locale, "operator.decision.prompt.add_note"));
        }
        if (index > 0) {
            sb.append("   ").append(getMessage(locale, "operator.decision.prompt.back"));
        }
        sb.append("   ").append(getMessage(locale, "operator.decision.prompt.none_fits"));
        sb.append("   ").append(getMessage(locale, "operator.decision.prompt.later"));
        sb.append("   ").append(getMessage(locale, "operator.decision.prompt.help")).append("   › ");
        return sb.toString();
    }

    private static String optionRange(DecisionPart part) {
        var keys = part.options().stream().map(DecisionOption::key).toList();
        if (keys.isEmpty()) {
            return "";
        }
        var allNumeric = keys.stream().allMatch(key -> key.matches("\\d+"));
        if (!allNumeric) {
            return String.join(", ", keys);
        }
        var numbers = keys.stream().map(Integer::parseInt).sorted().toList();
        var consecutive = true;
        for (int i = 1; i < numbers.size(); i++) {
            if (numbers.get(i) != numbers.get(i - 1) + 1) {
                consecutive = false;
                break;
            }
        }
        if (consecutive) {
            return numbers.size() == 1 ? String.valueOf(numbers.get(0))
                                : numbers.get(0) + "-" + numbers.get(numbers.size() - 1);
        }
        return String.join(", ", keys);
    }

    private static List<DecisionPart> effectiveParts(DecisionEnvelope envelope) {
        if (!envelope.parts().isEmpty()) {
            return envelope.parts();
        }
        // The description or title is already printed above, so the synthetic part adds no question.
        return List.of(new DecisionPart("1", "", AnswerType.ONE_OPTION,
                            List.of(), null, null, null, null, true, true));
    }

    private static DecisionStake stakeOf(DecisionEnvelope envelope, DecisionPart part) {
        if (envelope.reversibility() != Reversibility.REVERSIBLE) {
            return DecisionStake.COSTLY;
        }
        var split = part.options().stream()
                            .map(DecisionOption::quorumSupport)
                            .filter(Objects::nonNull)
                            .anyMatch(support -> support.band() == QuorumBand.SPLIT
                                                || support.band() == QuorumBand.NO_CONSENSUS);
        return split ? DecisionStake.SPLIT : DecisionStake.CONSENSUS;
    }

    private static @Nullable DecisionOption recommendedOption(DecisionPart part) {
        var key = part.recommendation();
        if (key == null) {
            return null;
        }
        return part.options().stream()
                            .filter(opt -> opt.key().equalsIgnoreCase(key))
                            .findFirst().orElse(null);
    }

    private static @Nullable DecisionOption pickOption(DecisionPart part, String key) {
        return part.options().stream()
                            .filter(opt -> opt.key().equalsIgnoreCase(key))
                            .findFirst().orElse(null);
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

    private ConfirmOutcome confirmAnswers(DialogueSession session, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers) throws IOException {
        var out = session.out();
        var locale = session.locale();
        if (parts.size() == 1 && !answers.get(0).ownWords()) {
            var picked = pickOption(parts.get(0), answers.get(0).value());
            if (picked == null) {
                return new ConfirmOutcome(true, null, 0);
            }
            if (confirmNonReversible(session.reader(), out, envelope, picked, locale)) {
                return new ConfirmOutcome(true, null, 0);
            }
            return new ConfirmOutcome(false, 0, 0);
        }
        while (true) {
            printDecisionConfirmation(out, envelope, parts, answers, locale);
            var prompt = parts.size() > 1
                                ? MessageCatalogue.format(locale, "operator.decision.confirm_parts_prompt",
                                Map.of("range", "1-" + parts.size()))
                                : getMessage(locale, "operator.decision.confirm_single_prompt");
            var line = readLineSafe(session.reader(), prompt);
            if (line == null) {
                return new ConfirmOutcome(false, null, 0);
            }
            var choice = line.trim();
            if (choice.isEmpty()) {
                return new ConfirmOutcome(true, null, 0);
            }
            if ("?".equals(choice)) {
                printEvidenceDetails(out, envelope.evidence(), locale);
                continue;
            }
            if (isQuitChoice(choice)) {
                printLeftDialogue(out, envelope.id(), locale);
                return new ConfirmOutcome(false, null, 0);
            }
            if (parts.size() > 1 && choice.matches("\\d+")) {
                int reopened = Integer.parseInt(choice);
                if (reopened >= 1 && reopened <= parts.size()) {
                    return new ConfirmOutcome(false, reopened - 1, 0);
                }
            }
            out.println(getMessage(locale, "operator.decision.invalid_choice"));
            out.flush();
        }
    }

    private static void printDecisionConfirmation(PrintWriter out, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers, String locale) {
        if (parts.size() > 1) {
            out.println(MessageCatalogue.format(locale, "operator.decision.confirm_title_parts", Map.of(ID_KEY, envelope.id())));
            for (int i = 0; i < parts.size(); i++) {
                out.println(MessageCatalogue.format(locale, "operator.decision.part_answer_line", Map.of(
                                    "n", i + 1,
                                    "answer", answerLabel(parts.get(i), answers.get(i)))));
                var answer = answers.get(i);
                if (!answer.ownWords()) {
                    var option = pickOption(parts.get(i), answer.value());
                    if (option != null && option.consequences() != null) {
                        out.println("  " + MessageCatalogue.format(locale, "operator.decision.follows",
                                            Map.of("consequence", TerminalSanitizer.sanitizeLine(option.consequences()))));
                    }
                }
            }
        } else {
            out.println(MessageCatalogue.format(locale, confirmTitleKey(envelope), Map.of(ID_KEY, envelope.id())));
            var answer = answers.get(0);
            out.println("  " + answerLabel(parts.get(0), answer));
        }
        out.println();
        out.flush();
    }

    private static String answerLabel(DecisionPart part, PartAnswer answer) {
        if (answer.ownWords()) {
            return TerminalSanitizer.sanitizeLine(answer.value());
        }
        var option = pickOption(part, answer.value());
        return option != null ? TerminalSanitizer.sanitizeLine(option.label())
                            : TerminalSanitizer.sanitizeLine(answer.value());
    }

    private int submitAnswers(DialogueSession session, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers,
                        @Nullable String note) throws IOException {
        if (parts.size() > 1) {
            return submitPartAnswers(session, envelope, parts, answers, note);
        }
        var answer = answers.get(0);
        if (answer.ownWords()) {
            return submitOwnWordsAnswer(session, envelope, answer.value());
        }
        var option = pickOption(parts.get(0), answer.value());
        var label = option != null ? option.label() : answer.value();
        return submitChoice(session.runtime(), session.out(), envelope, answer.value(), label, note, session.locale());
    }

    private int submitPartAnswers(DialogueSession session, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers,
                        @Nullable String note) throws IOException {
        var partAnswers = new LinkedHashMap<String, Object>();
        for (int i = 0; i < parts.size(); i++) {
            partAnswers.put(parts.get(i).partId(), answers.get(i).value());
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("part_answers", partAnswers);
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        if (note != null) {
            payload.put("note", note);
        }
        var answerPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_ANSWER;
        var response = session.runtime().send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (response.status() == 200 || response.status() == 202 || response.status() == 204) {
            session.out().println(MessageCatalogue.format(session.locale(), "operator.decision.answered_parts",
                                Map.of(ID_KEY, envelope.id())));
            session.out().flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + envelope.id(), response);
    }

    private int submitOwnWordsAnswer(DialogueSession session, DecisionEnvelope envelope, String words) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("words", words);
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        var answerPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_ANSWER;
        var response = session.runtime().send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (response.status() == 200 || response.status() == 202 || response.status() == 204) {
            session.out().println(MessageCatalogue.format(session.locale(), "operator.decision.answered", Map.of(
                                ID_KEY, envelope.id(),
                                LABEL_KEY, TerminalSanitizer.sanitizeLine(words))));
            session.out().flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + envelope.id(), response);
    }

    private static boolean handleWithdrawChoice(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String locale) throws IOException {
        var confirmPrompt = MessageCatalogue.format(locale, "operator.decision.none_fits_confirm", Map.of(
                            SCOPE_KEY, scopeSelector(envelope),
                            "project", projectNoun(envelope, locale)
        )) + " [Enter]   " + getMessage(locale, "operator.decision.prompt.back") + "   › ";
        var confirm = readLineSafe(reader, confirmPrompt);
        if (confirm == null) return true;
        if (confirm.isBlank() || "y".equalsIgnoreCase(confirm.trim()) || "j".equalsIgnoreCase(confirm.trim())) {
            var withPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_WITHDRAW;
            var withResponse = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (!isAccepted(withResponse.status())) {
                throw new OperatorRefusedException("withdraw " + envelope.id(), withResponse);
            }
            out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                                ID_KEY, envelope.id(),
                                SCOPE_KEY, scopeSelector(envelope),
                                "project", projectNoun(envelope, locale)
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
        out.println(MessageCatalogue.format(locale, confirmTitleKey(envelope), Map.of(ID_KEY, envelope.id())));
        out.printf("  %s%n", OutputSafety.stripControlSequences(picked.label()));
        if (picked.consequences() != null) {
            out.println("  " + MessageCatalogue.format(locale, "operator.decision.follows", Map.of("consequence", OutputSafety.stripControlSequences(picked.consequences()))));
        }
        var confirm = readLineSafe(reader, getMessage(locale, "operator.decision.confirm_prompt"));
        if (confirm == null) return false;
        return confirm.isBlank() || "y".equalsIgnoreCase(confirm.trim()) || "j".equalsIgnoreCase(confirm.trim());
    }

    private @Nullable PartAnswer readOwnWords(DialogueSession session, DecisionEnvelope envelope,
                        String locale) throws IOException {
        var out = session.out();
        var reader = session.reader();
        var wordsPrompt = getMessage(locale, "operator.decision.own_words_prompt") + " › ";
        while (true) {
            var wordsInput = readLineSafe(reader, wordsPrompt);
            if (wordsInput == null || wordsInput.isBlank()) {
                return null;
            }
            var interp = interpret(session, envelope, wordsInput.trim());
            if (interp.interpretationType() == InterpretationType.UNCLEAR && interp.clarificationQuestion() != null) {
                out.println(MessageCatalogue.format(locale, "decision.clarify.asks", Map.of(
                                    ROLE_KEY, "interpret",
                                    "question", TerminalSanitizer.sanitizeLine(interp.clarificationQuestion())
                )));
                var reply = readLineSafe(reader, getMessage(locale, "operator.decision.reply_prompt") + " › ");
                var combinedWords = wordsInput.trim() + ". " + (reply != null ? reply.trim() : "");
                interp = interpret(session, envelope, combinedWords);
                if (interp.interpretationType() == InterpretationType.UNCLEAR) {
                    out.println(MessageCatalogue.format(locale, "decision.interpret.unclear_bound", Map.of(
                                        "reason", getMessage(locale, "decision.interpret.unclear_reason"))));
                    out.flush();
                    return null;
                }
            }

            out.println();
            var reading = interp.readBackText() != null ? interp.readBackText() : wordsInput.trim();
            out.println(MessageCatalogue.format(locale, "decision.readback.understood",
                                Map.of("reading", TerminalSanitizer.sanitizeLine(reading))));
            var confirm = readLineSafe(reader, getMessage(locale, "operator.decision.readback_confirm"));
            if (confirm == null) {
                return null;
            }
            var answer = confirm.trim();
            if (answer.isEmpty() || "y".equalsIgnoreCase(answer) || "j".equalsIgnoreCase(answer)) {
                return new PartAnswer(reading, true);
            }
            if ("e".equalsIgnoreCase(answer) || ("b".equalsIgnoreCase(answer) && LOCALE_DE.equals(locale))) {
                continue;
            }
            return null;
        }
    }

    private static DecisionInterpretation interpret(DialogueSession session, DecisionEnvelope envelope,
                        String words) throws IOException {
        var interpPath = API_V1_DECISIONS + OperatorUrlUtil.encodePathSegment(envelope.id()) + PATH_INTERPRET;
        var interpRes = session.runtime().send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of("words", words))));
        if (interpRes.status() != 200) {
            throw new OperatorRefusedException("interpret " + envelope.id(), interpRes);
        }
        return DecisionInterpretation.fromJson(interpRes.bodyText());
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
                                SCOPE_KEY, scopeSelector(envelope),
                                "project", projectNoun(envelope, locale)
            ));
            out.println(MessageCatalogue.format(locale, "operator.decision.answered_goes_on", Map.of(
                                ID_KEY, envelope.id(),
                                LABEL_KEY, label,
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

    /**
     * The selector value the catalogue's noun templates branch on. It comes from the envelope's
     * scope and blocks, never from the subject text: an entry decision selects {@code entry}, an
     * epic one {@code epic}, a project setup {@code setup}, everything else {@code plan}.
     *
     * @param envelope the decision envelope, may be {@code null}
     * @return the selector value
     */
    private static String scopeSelector(@Nullable DecisionEnvelope envelope) {
        if (envelope == null) {
            return PLAN_KEY;
        }
        if ("entry".equalsIgnoreCase(envelope.blocks())) {
            return "entry";
        }
        return switch (envelope.scope()) {
            case EPIC -> "epic";
            case PROJECT_SETUP -> "setup";
            default -> PLAN_KEY;
        };
    }

    private static String projectNoun(@Nullable DecisionEnvelope envelope, String locale) {
        if (envelope != null && envelope.project() != null && !envelope.project().isBlank()) {
            return envelope.project();
        }
        return getMessage(locale, "decision.project.fallback");
    }

    private static String planNoun(@Nullable DecisionEnvelope envelope, String locale) {
        if (envelope != null && envelope.plan() != null && !envelope.plan().isBlank()) {
            return envelope.plan();
        }
        return getMessage(locale, "decision.waiver.plan_fallback");
    }

    private static @Nullable String waiverGate(DecisionEnvelope envelope, String locale) {
        if (envelope.waiverGate() != null && !envelope.waiverGate().isBlank()) {
            return envelope.waiverGate();
        }
        return getMessage(locale, "decision.waiver.gate_fallback");
    }

    private static boolean isAccepted(int status) {
        return status == 200 || status == 202 || status == 204;
    }

    private static String confirmTitleKey(DecisionEnvelope envelope) {
        return envelope.reversibility() == Reversibility.COSTLY
                            ? "operator.decision.confirm_title_costly"
                            : "operator.decision.confirm_title";
    }

    private static String getMessage(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }
}
