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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
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
import de.planmarshall.api.decision.QuorumSupport;
import de.planmarshall.api.decision.QuorumVote;
import de.planmarshall.api.decision.Reversibility;
import de.planmarshall.api.display.Display;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
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
    private static final String RANGE_KEY = "range";
    private static final String PROJECT_KEY = "project";
    private static final String WORDS_KEY = "words";
    private static final String INVALID_CHOICE_KEY = "operator.decision.invalid_choice";
    private static final String BLOCKS_KEY = "blocks";
    private static final String REVERSIBILITY_KEY = "reversibility";
    private static final String ACTION_WAIVE = "waive";
    private static final String ACTION_DECLINE = "decline";
    private static final String ACTION_LATER = "later";
    private static final String ACTION_WITHDRAW = "withdraw";
    private static final String ACTION_ANSWER = "answer";
    private static final String KEY_OWN_WORDS = "w";
    private static final String KEY_NONE_FITS = "x";
    private static final String KEY_CHOOSE_OPTION = "o";
    /** The acts a waiver request offers ({@code doc/design/shared/decisions.adoc#waiver}). */
    private static final List<String> WAIVER_ACTIONS = List.of(ACTION_WAIVE, ACTION_DECLINE, ACTION_LATER);
    /** The acts a decision offers besides its options: later and none fits. */
    private static final List<String> DECISION_ACTIONS = List.of(ACTION_ANSWER, ACTION_LATER, ACTION_WITHDRAW);

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
        return isDirectAnswer() ? submitAnswer(runtime, out, locale) : showDecision(runtime, out, locale);
    }

    private boolean isDirectAnswer() {
        return option != null || (words != null && !words.isBlank()) || (action != null && !action.isBlank());
    }

    private int showDecision(RuntimeClient runtime, PrintWriter out, String locale) throws IOException {
        var safeQid = OperatorUrlUtil.encodePathSegment(questionId);
        var qResponse = runtime.send(HttpRequest.get(resolveDecisionPath(safeQid)));
        if (qResponse.status() == 404) {
            out.println(MessageCatalogue.format(locale, "operator.decision.not_found", Map.of(ID_KEY, questionId)));
            out.flush();
            return 1;
        }
        var envelope = qResponse.status() == 200 ? identified(qResponse) : null;
        if (envelope == null) {
            // Any other first read is a failed read: its body is no decision to show or answer.
            return unreadable(out, locale);
        }
        if (noInput) {
            printNoInput(out, envelope, locale);
            return 3;
        }
        if (interactive || context.isInteractiveTerminal()) {
            return handleInteractive(runtime, out, envelope, locale);
        }
        printDecisionDetail(out, envelope, locale);
        out.flush();
        return 0;
    }

    private void printNoInput(PrintWriter out, DecisionEnvelope envelope, String locale) {
        printDecisionDetail(out, envelope, locale);
        if (envelope.isWaiver()) {
            out.println(getMessage(locale, "operator.decision.waiver_prompt_full"));
        } else {
            for (var part : effectiveParts(envelope)) {
                out.println(MessageCatalogue.format(locale, "operator.decision.prompt.choose",
                                    Map.of(RANGE_KEY, optionRange(part))));
            }
        }
        out.println(getMessage(locale, "operator.decision.choice_required"));
        out.flush();
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
            out.println("  " + OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        printEvidenceHintIfPresent(out, envelope.evidence(), locale);
    }

    private void printQuestionDetail(PrintWriter out, DecisionEnvelope envelope, String locale) {
        // blocks and reversibility are never null in an envelope, and every reversibility has its text.
        var blocksKey = "decision.blocks." + envelope.blocks().toLowerCase(Locale.ROOT);
        String blocksMsg;
        if (MessageCatalogue.hasKey(blocksKey)) {
            blocksMsg = getMessage(locale, blocksKey);
        } else {
            blocksMsg = getMessage(locale, "decision.blocks.plan");
        }
        var revMsg = getMessage(locale, "decision.reversibility." + envelope.reversibility().value());

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
            out.println("  " + OutputSafety.stripControlSequences(titleOrDesc));
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
        var envelope = readEnvelope(runtime, safeQid);
        if (envelope == null) {
            return unreadable(out, locale);
        }
        var offered = envelope.isWaiver() ? WAIVER_ACTIONS : DECISION_ACTIONS;
        var effectiveAction = namedAction(envelope);
        if (!offered.contains(effectiveAction)) {
            return notOffered(out, envelope, locale);
        }

        if (isWaiveOrDecline(effectiveAction) && (reason == null || reason.isBlank())) {
            out.println(getMessage(locale, "operator.decision.reason_required"));
            out.flush();
            return 3;
        }

        if (ACTION_LATER.equals(effectiveAction)) {
            var laterPath = resolveDecisionPath(safeQid) + PATH_ANSWER;
            var laterPayload = new LinkedHashMap<String, Object>();
            laterPayload.put(ACTION_KEY, ACTION_LATER);
            laterPayload.put(CHANNEL_KEY, CHANNEL_CLI);
            var laterResponse = runtime.send(HttpRequest.json("POST", laterPath, JsonTree.write(laterPayload)));
            if (!isAccepted(laterResponse.status())) {
                throw new OperatorRefusedException("later " + questionId, laterResponse);
            }
            printLaterDone(out, envelope, locale);
            return 0;
        }

        if (ACTION_WITHDRAW.equals(effectiveAction)) {
            var withPath = resolveDecisionPath(safeQid) + PATH_WITHDRAW;
            var resp = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (isAccepted(resp.status())) {
                printNoneFitsDone(out, envelope, locale);
                return 0;
            }
            throw new OperatorRefusedException("withdraw " + questionId, resp);
        }

        return submitAnswerPayload(runtime, out, safeQid, envelope, effectiveAction, locale);
    }

    /**
     * What the direct form asks for. {@code --action} names an act; a positional value is the
     * decision's option, and only a value that is no option of it names an act. The values are the
     * same in every language ({@code doc/design/terminal-client/01-commands.adoc#language}): the
     * dialogue's one-letter keys are no values of the command line, so an option keyed {@code l},
     * {@code x}, {@code y} or {@code d} is answered as that option.
     *
     * @return the act, or {@link #ACTION_ANSWER} for an answer with the option or the own words
     */
    private String namedAction(DecisionEnvelope envelope) {
        if (action != null && !action.isBlank()) {
            return action.trim().toLowerCase(Locale.ROOT);
        }
        if (option != null && !isOptionKey(envelope, option)) {
            var acts = envelope.isWaiver() ? WAIVER_ACTIONS : DECISION_ACTIONS;
            for (var act : acts) {
                if (act.equalsIgnoreCase(option)) {
                    return act;
                }
            }
        }
        return ACTION_ANSWER;
    }

    private static boolean isOptionKey(DecisionEnvelope envelope, String value) {
        return envelope.parts().stream().anyMatch(part -> pickOption(part, value) != null);
    }

    /**
     * A direct answer the request does not offer: a waiver is answered by waive, decline or later
     * alone, a decision by its option, later or withdraw. Nothing is sent.
     */
    private static int notOffered(PrintWriter out, DecisionEnvelope envelope, String locale) {
        if (envelope.isWaiver()) {
            out.println(MessageCatalogue.format(locale, "operator.decision.waiver_needs_answer", Map.of(
                                ACTION_WAIVE, ACTION_WAIVE, ACTION_DECLINE, ACTION_DECLINE, ACTION_LATER, ACTION_LATER)));
        } else {
            out.println(getMessage(locale, INVALID_CHOICE_KEY));
        }
        out.flush();
        return 3;
    }

    private int submitAnswerPayload(RuntimeClient runtime, PrintWriter out, String safeQid,
                        DecisionEnvelope envelope, String effectiveAction, String locale) throws IOException {
        var ownWords = words != null && !words.isBlank();
        if (ACTION_ANSWER.equals(effectiveAction) && option == null && !ownWords) {
            // --action answer names no option and no words: there is nothing to send.
            return notOffered(out, envelope, locale);
        }
        if (ACTION_ANSWER.equals(effectiveAction) && envelope.parts().size() > 1) {
            out.println(MessageCatalogue.format(locale, "operator.decision.parts_direct_refused", Map.of(
                                ID_KEY, questionId,
                                "count", envelope.parts().size())));
            out.flush();
            return 3;
        }
        var response = sendAnswer(runtime, safeQid, directPayload(effectiveAction));
        if (isAccepted(response.status())) {
            printAnswerSuccess(out, envelope, effectiveAction, locale);
            return 0;
        }
        throw new OperatorRefusedException("answer " + questionId, response);
    }

    private LinkedHashMap<String, Object> directPayload(String effectiveAction) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        if (isWaiveOrDecline(effectiveAction)) {
            payload.put(ACTION_KEY, effectiveAction);
            payload.put(REASON_KEY, reason);
        } else {
            if (option != null) {
                payload.put(OPTION_KEY, option);
            }
            if (words != null) {
                payload.put(WORDS_KEY, words);
            }
        }
        if (note != null) {
            payload.put("note", note);
        }
        return payload;
    }

    private static boolean isWaiveOrDecline(@Nullable String action) {
        return action != null && (ACTION_WAIVE.equalsIgnoreCase(action) || ACTION_DECLINE.equalsIgnoreCase(action));
    }

    private HttpResponse sendAnswer(RuntimeClient runtime, String safeQid, Map<String, Object> payload)
                        throws IOException {
        var body = JsonTree.write(payload);
        var response = runtime.send(HttpRequest.json("POST", resolveDecisionPath(safeQid) + PATH_ANSWER, body));
        if (response.status() == 404 && plan == null) {
            // With --plan the operator named the scope: an unscoped answer would leave it.
            response = runtime.send(HttpRequest.json("POST", API_V1_QUESTIONS + safeQid + PATH_ANSWERS, body));
        }
        return response;
    }

    private String resolveDecisionPath(String safeQid) {
        if (plan != null) {
            return API_V1_PLANS + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid;
        }
        return API_V1_DECISIONS + safeQid;
    }

    /**
     * Reads the decision the request names before anything is sent. A decision the runtime does not
     * know is its refusal; any other read that returns no decision of the id asked for fails closed.
     *
     * @return the decision, or {@code null} when the read failed or returned another decision
     */
    private @Nullable DecisionEnvelope readEnvelope(RuntimeClient runtime, String safeQid) throws IOException {
        var response = runtime.send(HttpRequest.get(resolveDecisionPath(safeQid)));
        if (response.status() == 404) {
            throw new OperatorRefusedException("answer " + questionId, response);
        }
        return response.status() == 200 ? identified(response) : null;
    }

    /**
     * The id of the request is authoritative: a body that is no decision, has no id, or names another
     * decision is never shown or answered as the decision that was asked for.
     *
     * @return the decision, or {@code null} when the body is no decision of the id asked for
     */
    private @Nullable DecisionEnvelope identified(HttpResponse response) {
        try {
            var envelope = DecisionEnvelope.fromJson(response.bodyText());
            return questionId.equals(envelope.id()) ? envelope : null;
        } catch (IOException _) {
            return null;
        }
    }

    /**
     * Says that the decision could not be read, so that it is not known what it holds and nothing was
     * sent.
     *
     * @return the exit code of a failed read
     */
    private int unreadable(PrintWriter out, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.parts_unreadable", Map.of(ID_KEY, questionId)));
        out.flush();
        return 1;
    }

    private void printAnswerSuccess(PrintWriter out, DecisionEnvelope envelope, String effectiveAction,
                        String locale) {
        if (ACTION_WAIVE.equals(effectiveAction)) {
            out.println(MessageCatalogue.format(locale, "operator.decision.waived", Map.of(
                                GATE_KEY, waiverGate(envelope, locale),
                                PLAN_KEY, planNoun(envelope, locale))));
        } else if (ACTION_DECLINE.equals(effectiveAction)) {
            out.println(MessageCatalogue.format(locale, "operator.decision.declined", Map.of(ID_KEY, questionId)));
        } else {
            printDirectAnswerSuccess(out, envelope, locale);
        }
        out.flush();
    }

    private void printDirectAnswerSuccess(PrintWriter out, DecisionEnvelope envelope, String locale) {
        var label = words != null && !words.isBlank() ? words
                            : findOptionLabel(envelope, Objects.requireNonNull(option));
        out.println(MessageCatalogue.format(locale, "operator.decision.answered", Map.of(
                            ID_KEY, questionId,
                            LABEL_KEY, label
        )));
    }

    /**
     * Reads one answer line. The end of the input at any prompt leaves the dialogue as {@code q}
     * does: {@code handleInteractive} prints the {@code Left} line and exits 0.
     */
    private static String readLineSafe(LineReader reader, String prompt) {
        try {
            return Objects.requireNonNullElse(reader.readLine(TerminalSanitizer.sanitizeLine(prompt)), "");
        } catch (UserInterruptException _) {
            // The interrupt is the exit-code table's 130, never an unanswered question.
            throw new StartPrompt.InterruptException();
        } catch (EndOfFileException _) {
            throw new StartPrompt.StartLeftException();
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

    private record PartContext(DecisionPart part, int index, DecisionStake stake,
    @Nullable DecisionOption recommended, @Nullable PartAnswer previous,
    @Nullable String currentNote) {
    }

    private record Progress(int index, @Nullable String note) {
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
        } catch (StartPrompt.StartLeftException _) {
            // The input ended at a prompt: nothing more was sent, the decision stays open.
            printLeftDialogue(out, envelope.id(), locale);
            return 0;
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
            session.out().println("  " + OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        printEvidenceHintIfPresent(session.out(), envelope.evidence(), session.locale());
        session.out().println();
        session.out().flush();

        var prompt = getMessage(session.locale(), "operator.decision.waiver_prompt_full");
        while (true) {
            var choice = readLineSafe(session.reader(), prompt);
            if (choice.isBlank()) {
                printWaiverNeedsAnswer(session.out(), session.locale());
                continue;
            }
            choice = choice.trim();

            var keys = Keys.of(session.locale());
            if (isQuitChoice(choice, session.locale())) {
                printLeftDialogue(session.out(), envelope.id(), session.locale());
                return 0;
            }
            if (isLaterChoice(choice, session.locale())) {
                printLaterDone(session.out(), envelope, session.locale());
                return 0;
            }
            if ("?".equals(choice)) {
                printEvidenceDetails(session.out(), envelope.evidence(), session.locale());
                continue;
            }
            if (keys.waive().equalsIgnoreCase(choice)) {
                return executeWaiverAct(session, envelope, ACTION_WAIVE);
            }
            if (keys.decline().equalsIgnoreCase(choice)) {
                return executeWaiverAct(session, envelope, ACTION_DECLINE);
            }
            printWaiverNeedsAnswer(session.out(), session.locale());
        }
    }

    /**
     * The keys of the decision dialogue in the interface language
     * ({@code doc/design/terminal-client/03-dialogues.adoc#keys}): a key is the mnemonic of its
     * label, so it follows the language, and a key of the other language is not offered. Own words
     * ({@code w}), none fits ({@code x}) and choose an option instead ({@code o}) are the same in
     * both.
     *
     * @param leave   leave the dialogue
     * @param back    back to the previous part, or from a confirmation
     * @param note    add a note
     * @param later   leave the decision open
     * @param waive   waive the gate
     * @param decline decline the waiver request
     * @param edit    edit the own words at the read-back
     */
    private record Keys(String leave, String back, String note, String later, String waive, String decline,
    String edit) {

        private static final Keys ENGLISH = new Keys("q", "b", "m", "l", "y", "d", "e");
        private static final Keys GERMAN = new Keys("v", "z", "n", "s", "w", "a", "b");

        static Keys of(String locale) {
            return LOCALE_DE.equals(locale) ? GERMAN : ENGLISH;
        }
    }

    private static boolean isQuitChoice(String choice, String locale) {
        return Keys.of(locale).leave().equalsIgnoreCase(choice);
    }

    private static void printLeftDialogue(PrintWriter out, String id, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.dialogue.left", Map.of("state", id)));
        out.flush();
    }

    private static void printLaterDone(PrintWriter out, DecisionEnvelope envelope, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, scopeSelector(envelope),
                            PROJECT_KEY, projectNoun(envelope, locale)
        )));
        out.flush();
    }

    private static void printWaiverNeedsAnswer(PrintWriter out, String locale) {
        var keys = Keys.of(locale);
        out.println(MessageCatalogue.format(locale, "operator.decision.waiver_needs_answer", Map.of(
                            ACTION_WAIVE, keys.waive(), ACTION_DECLINE, keys.decline(), ACTION_LATER, keys.later())));
        out.flush();
    }

    private static boolean isLaterChoice(String choice, String locale) {
        return Keys.of(locale).later().equalsIgnoreCase(choice);
    }

    private int executeWaiverAct(DialogueSession session, DecisionEnvelope envelope, String act) throws IOException {
        String reqReason = promptWaiverReason(session.reader(), session.out(), session.locale());
        return sendWaiverAnswer(session.runtime(), session.out(), envelope, act, reqReason, session.locale());
    }

    private static String promptWaiverReason(LineReader reader, PrintWriter out, String locale) {
        String reqReason = "";
        while (reqReason.isBlank()) {
            var line = readLineSafe(reader, getMessage(locale, "operator.decision.reason_prompt") + " › ");
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

        var ansPath = resolveDecisionPath(OperatorUrlUtil.encodePathSegment(envelope.id())) + PATH_ANSWER;
        var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
        if (isAccepted(ansRes.status())) {
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
        var progress = new Progress(0, null);

        while (true) {
            var result = askRemaining(session, envelope, parts, answers, progress);
            if (result.exitCode().isPresent()) {
                return result.exitCode().getAsInt();
            }
            progress = result.progress();
            if (parts.size() == 1 && envelope.reversibility() == Reversibility.REVERSIBLE) {
                return submitAnswers(session, envelope, parts, answers, progress.note());
            }
            var confirm = confirmAnswers(session, envelope, parts, answers);
            if (confirm.submit()) {
                return submitAnswers(session, envelope, parts, answers, progress.note());
            }
            var reopenIndex = confirm.reopenIndex();
            if (reopenIndex == null) {
                return confirm.exitCode();
            }
            progress = new Progress(reopenIndex, progress.note());
            answers.keySet().removeIf(key -> key >= reopenIndex);
        }
    }

    private record WalkOutcome(OptionalInt exitCode, Progress progress) {
    }

    private WalkOutcome askRemaining(DialogueSession session, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers, Progress progress) throws IOException {
        int index = progress.index();
        String currentNote = progress.note();
        while (index < parts.size()) {
            var part = parts.get(index);
            var stake = stakeOf(envelope, part);
            var recommended = recommendedOption(part);
            printPartPosition(session.out(), part, index, parts.size(), session.locale());
            renderOptions(session.out(), part, stake, session.locale());

            var partContext = new PartContext(part, index, stake, recommended, answers.get(index), currentNote);
            var outcome = askPart(session, envelope, partContext);
            if (outcome.signal() == PartSignal.DONE) {
                return new WalkOutcome(OptionalInt.of(outcome.exitCode()), new Progress(index, currentNote));
            }
            if (outcome.signal() == PartSignal.NOTE) {
                currentNote = outcome.note();
            } else if (outcome.signal() == PartSignal.BACK) {
                index = Math.max(0, index - 1);
            } else {
                answers.put(index, outcome.answer());
                index++;
            }
        }
        return new WalkOutcome(OptionalInt.empty(), new Progress(index, currentNote));
    }

    private PartOutcome askPart(DialogueSession session, DecisionEnvelope envelope, PartContext context) throws IOException {
        while (true) {
            var prompt = buildPartPrompt(context.part(), context.stake(), context.recommended(),
                                context.index(), context.currentNote(), session.locale());
            var choice = readLineSafe(session.reader(), prompt).trim();
            if (handleInfo(session, envelope, context, choice)) {
                continue;
            }
            var outcome = answerOrLeave(session, envelope, context, choice);
            if (outcome != null) {
                return outcome;
            }
        }
    }

    private static boolean handleInfo(DialogueSession session, DecisionEnvelope envelope,
                        PartContext context, String choice) {
        var out = session.out();
        var locale = session.locale();
        if ("?".equals(choice)) {
            printEvidenceDetails(out, envelope.evidence(), locale);
            return true;
        }
        if ("?r".equalsIgnoreCase(choice)) {
            printRationaleAndDissent(out, context.part(), context.recommended(), locale);
            return true;
        }
        if (isVoteView(choice, context.part())) {
            printVotes(out, context.part(), choice.substring(1), locale);
            return true;
        }
        return false;
    }

    private @Nullable PartOutcome answerOrLeave(DialogueSession session, DecisionEnvelope envelope,
                        PartContext context, String choice) throws IOException {
        var locale = session.locale();
        if (context.index() > 0 && isBackChoice(choice, locale)) {
            return new PartOutcome(PartSignal.BACK, null, context.currentNote(), 0);
        }
        if (Keys.of(locale).note().equalsIgnoreCase(choice) && context.part().allowNote()) {
            return new PartOutcome(PartSignal.NOTE, null,
                                promptNote(session.reader(), locale, context.currentNote()), 0);
        }
        if (KEY_OWN_WORDS.equalsIgnoreCase(choice) && context.part().allowOwnWords()) {
            var ownWordsAnswer = readOwnWords(session, envelope, locale);
            return ownWordsAnswer == null ? null
                                : new PartOutcome(PartSignal.ANSWER,
                                new PartAnswer(ownWordsAnswer.value(), true), context.currentNote(), 0);
        }
        if (KEY_NONE_FITS.equalsIgnoreCase(choice)) {
            return handleWithdrawChoice(session.runtime(), session.out(), session.reader(), envelope, locale)
                                ? done(context.currentNote()) : null;
        }
        var leave = leaveOutcome(session, envelope, choice, context.currentNote());
        return leave != null ? leave : pickOutcome(session, context, choice);
    }

    private static @Nullable PartOutcome leaveOutcome(DialogueSession session, DecisionEnvelope envelope,
                        String choice, @Nullable String note) {
        var locale = session.locale();
        if (isQuitChoice(choice, locale)) {
            printLeftDialogue(session.out(), envelope.id(), locale);
            return done(note);
        }
        if (isLaterChoice(choice, locale)) {
            printLaterDone(session.out(), envelope, locale);
            return done(note);
        }
        return null;
    }

    private @Nullable PartOutcome pickOutcome(DialogueSession session, PartContext context, String choice) {
        var out = session.out();
        var locale = session.locale();
        if (choice.isEmpty()) {
            return emptyChoice(session, context);
        }
        var picked = pickOption(context.part(), choice);
        if (picked != null) {
            return new PartOutcome(PartSignal.ANSWER, new PartAnswer(picked.key(), false), context.currentNote(), 0);
        }
        out.println(getMessage(locale, INVALID_CHOICE_KEY));
        out.flush();
        return null;
    }

    private static @Nullable PartOutcome emptyChoice(DialogueSession session, PartContext context) {
        if (context.previous() != null) {
            return new PartOutcome(PartSignal.ANSWER, context.previous(), context.currentNote(), 0);
        }
        if (context.stake() == DecisionStake.CONSENSUS && context.recommended() != null) {
            return new PartOutcome(PartSignal.ANSWER,
                                new PartAnswer(context.recommended().key(), false), context.currentNote(), 0);
        }
        session.out().println(getMessage(session.locale(), INVALID_CHOICE_KEY));
        session.out().flush();
        return null;
    }

    private static PartOutcome done(@Nullable String note) {
        return new PartOutcome(PartSignal.DONE, null, note, 0);
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
            out.printf("    %s) %-25s%s  %s", opt.key(), TerminalSanitizer.sanitizeLine(opt.label()), recMark, supportStr);
            out.println();
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
        var summary = recommendedOpt.quorumSupport() != null
                            ? recommendedOpt.quorumSupport().dissentSummary() : null;
        var matched = summary != null ? dissentForSummary(part, recommendedOpt, summary) : null;
        var carried = matched != null ? matched : dissentFromAnotherOption(part, recommendedOpt);
        return carried != null ? carried : dissentFromVoteReason(part, recommendedOpt);
    }

    /**
     * The voter of a dissent summary: the disagreeing vote whose reason is the summary names it; only
     * when no disagreeing reason matches does the first disagreeing vote with a reason stand in. A
     * vote for the recommended option is never the voter, even when it carries the same reason.
     */
    private static @Nullable Dissent dissentForSummary(DecisionPart part, DecisionOption recommendedOpt,
                        String summary) {
        var matched = disagreeingVotes(part, recommendedOpt).stream()
                            .filter(vote -> vote.reason() != null && vote.reason().trim().equalsIgnoreCase(summary.trim()))
                            .map(vote -> new Dissent(vote.role(), summary))
                            .findFirst();
        if (matched.isPresent()) {
            return matched.get();
        }
        var fallback = dissentFromVoteReason(part, recommendedOpt);
        return fallback == null ? null : new Dissent(fallback.role(), summary);
    }

    private static @Nullable Dissent dissentFromAnotherOption(DecisionPart part, DecisionOption recommendedOpt) {
        return part.options().stream()
                            .filter(opt -> !opt.key().equalsIgnoreCase(recommendedOpt.key()))
                            .map(AnswerCommand::dissentFromOption)
                            .filter(Objects::nonNull)
                            .findFirst().orElse(null);
    }

    private static @Nullable Dissent dissentFromOption(DecisionOption option) {
        var support = option.quorumSupport();
        if (support == null) {
            return null;
        }
        var summary = support.dissentSummary();
        if (summary == null) {
            return null;
        }
        var role = firstVoteRole(support);
        return role == null ? null : new Dissent(role, summary);
    }

    private static @Nullable Dissent dissentFromVoteReason(DecisionPart part, DecisionOption recommendedOpt) {
        return disagreeingVotes(part, recommendedOpt).stream()
                            .filter(vote -> vote.reason() != null && !vote.reason().isBlank())
                            .findFirst()
                            .map(vote -> new Dissent(vote.role(), vote.reason()))
                            .orElse(null);
    }

    private static @Nullable String firstVoteRole(QuorumSupport support) {
        return support.votes().stream()
                            .filter(vote -> vote.reason() != null && !vote.reason().isBlank())
                            .map(QuorumVote::role)
                            .findFirst().orElse(null);
    }

    /** The votes attached to every option other than the recommended one: the votes against it. */
    private static List<QuorumVote> disagreeingVotes(DecisionPart part, DecisionOption recommendedOpt) {
        return part.options().stream()
                            .filter(opt -> !opt.key().equalsIgnoreCase(recommendedOpt.key()))
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
                                REASON_KEY, reason)));
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
        return Keys.of(locale).back().equalsIgnoreCase(choice);
    }

    private static String buildPartPrompt(DecisionPart part, DecisionStake stake,
                        @Nullable DecisionOption recommended, int index, @Nullable String currentNote,
                        String locale) {
        var rangeStr = optionRange(part);
        var sb = new StringBuilder();
        if (stake == DecisionStake.CONSENSUS && recommended != null) {
            sb.append(TerminalSanitizer.sanitizeLine(recommended.label())).append("? [Enter]");
            if (part.options().size() > 1) {
                sb.append("   ").append(MessageCatalogue.format(locale, "operator.decision.prompt.other", Map.of(RANGE_KEY, rangeStr)));
            }
        } else {
            sb.append(MessageCatalogue.format(locale, "operator.decision.prompt.choose", Map.of(RANGE_KEY, rangeStr)));
        }
        if (part.allowOwnWords()) {
            sb.append("   ").append(getMessage(locale, "operator.decision.prompt.own_words"));
        }
        if (part.allowNote()) {
            sb.append("   ").append(currentNote != null
                                ? Keys.of(locale).note() + ") " + getMessage(locale, "operator.decision.note_set")
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
        // Bounded as the confirmation's part number is: ten digits and more are no int, so no range.
        var allNumeric = keys.stream().allMatch(key -> key.matches("\\d{1,9}"));
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

    private static @Nullable String promptNote(LineReader reader, String locale, @Nullable String currentNote) {
        var noteInput = readLineSafe(reader, getMessage(locale, "operator.decision.note_prompt") + " › ");
        if (!noteInput.isBlank()) {
            return noteInput.trim();
        }
        return currentNote;
    }

    private ConfirmOutcome confirmAnswers(DialogueSession session, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers) {
        if (parts.size() == 1 && !answers.get(0).ownWords()) {
            return confirmSingle(session, envelope, parts.get(0), answers.get(0));
        }
        return confirmMultipart(session, envelope, parts, answers);
    }

    private static ConfirmOutcome confirmSingle(DialogueSession session, DecisionEnvelope envelope,
                        DecisionPart part, PartAnswer answer) {
        // An answer that is no own words was picked from the part's options, so it names one.
        var picked = Objects.requireNonNull(pickOption(part, answer.value()));
        return confirmNonReversible(session.reader(), session.out(), envelope, picked, session.locale());
    }

    private ConfirmOutcome confirmMultipart(DialogueSession session, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers) {
        var out = session.out();
        var locale = session.locale();
        while (true) {
            printDecisionConfirmation(out, envelope, parts, answers, locale);
            var prompt = parts.size() > 1
                                ? MessageCatalogue.format(locale, "operator.decision.confirm_parts_prompt",
                                Map.of(RANGE_KEY, "1-" + parts.size()))
                                : getMessage(locale, "operator.decision.confirm_single_prompt");
            var line = readLineSafe(session.reader(), prompt);
            var result = confirmChoice(line.trim(), parts, out, envelope, locale);
            if (result != null) {
                return result;
            }
        }
    }

    private static @Nullable ConfirmOutcome confirmChoice(String choice, List<DecisionPart> parts,
                        PrintWriter out, DecisionEnvelope envelope, String locale) {
        if (choice.isEmpty()) {
            return new ConfirmOutcome(true, null, 0);
        }
        if ("?".equals(choice)) {
            printEvidenceDetails(out, envelope.evidence(), locale);
            return null;
        }
        if (isQuitChoice(choice, locale)) {
            printLeftDialogue(out, envelope.id(), locale);
            return new ConfirmOutcome(false, null, 0);
        }
        if (choice.matches("\\d{1,9}") && parts.size() > 1) {
            int reopened = Integer.parseInt(choice);
            if (reopened >= 1 && reopened <= parts.size()) {
                return new ConfirmOutcome(false, reopened - 1, 0);
            }
        }
        out.println(getMessage(locale, INVALID_CHOICE_KEY));
        out.flush();
        return null;
    }

    private static void printDecisionConfirmation(PrintWriter out, DecisionEnvelope envelope,
                        List<DecisionPart> parts, Map<Integer, PartAnswer> answers, String locale) {
        if (parts.size() > 1) {
            out.println(MessageCatalogue.format(locale, confirmTitleKey(envelope), Map.of(ID_KEY, envelope.id())));
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
            return submitOwnWordsAnswer(session, envelope, answer.value(), note);
        }
        var picked = pickOption(parts.get(0), answer.value());
        var label = picked != null ? picked.label() : answer.value();
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
        var answerPath = resolveDecisionPath(OperatorUrlUtil.encodePathSegment(envelope.id())) + PATH_ANSWER;
        var response = session.runtime().send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (isAccepted(response.status())) {
            session.out().println(MessageCatalogue.format(session.locale(), "operator.decision.answered_parts",
                                Map.of(ID_KEY, envelope.id())));
            session.out().flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + envelope.id(), response);
    }

    private int submitOwnWordsAnswer(DialogueSession session, DecisionEnvelope envelope, String words,
                        @Nullable String note) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put(WORDS_KEY, words);
        payload.put(CHANNEL_KEY, CHANNEL_CLI);
        if (note != null) {
            payload.put("note", note);
        }
        var answerPath = resolveDecisionPath(OperatorUrlUtil.encodePathSegment(envelope.id())) + PATH_ANSWER;
        var response = session.runtime().send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (isAccepted(response.status())) {
            session.out().println(MessageCatalogue.format(session.locale(), "operator.decision.answered", Map.of(
                                ID_KEY, envelope.id(),
                                LABEL_KEY, TerminalSanitizer.sanitizeLine(words))));
            session.out().flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + envelope.id(), response);
    }

    private boolean handleWithdrawChoice(RuntimeClient runtime, PrintWriter out, LineReader reader,
                        DecisionEnvelope envelope, String locale) throws IOException {
        var confirmPrompt = MessageCatalogue.format(locale, "operator.decision.none_fits_confirm", Map.of(
                            SCOPE_KEY, scopeSelector(envelope),
                            PROJECT_KEY, projectNoun(envelope, locale)
        )) + " [Enter]   " + getMessage(locale, "operator.decision.prompt.back") + "   › ";
        while (true) {
            var confirm = readLineSafe(reader, confirmPrompt);
            var choice = confirm.trim();
            if (isBackChoice(choice, locale)) {
                return false;
            }
            if (!choice.isEmpty()) {
                // The confirmation offers Enter and back alone; nothing else withdraws the decision.
                out.println(getMessage(locale, INVALID_CHOICE_KEY));
                out.flush();
                continue;
            }
            var withPath = resolveDecisionPath(OperatorUrlUtil.encodePathSegment(envelope.id())) + PATH_WITHDRAW;
            var withResponse = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (!isAccepted(withResponse.status())) {
                throw new OperatorRefusedException("withdraw " + envelope.id(), withResponse);
            }
            printNoneFitsDone(out, envelope, locale);
            return true;
        }
    }

    private static void printNoneFitsDone(PrintWriter out, DecisionEnvelope envelope, String locale) {
        out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                            ID_KEY, envelope.id(),
                            SCOPE_KEY, scopeSelector(envelope),
                            PROJECT_KEY, projectNoun(envelope, locale)
        )));
        out.flush();
    }

    /**
     * {@code decision.confirm} of a decision that is not reversible: {@code Enter} decides, the back
     * key returns to the part, {@code ?} shows the evidence, and every other input is not offered.
     */
    private static ConfirmOutcome confirmNonReversible(LineReader reader, PrintWriter out,
                        DecisionEnvelope envelope, DecisionOption picked,
                        String locale) {
        out.println();
        out.println(MessageCatalogue.format(locale, confirmTitleKey(envelope), Map.of(ID_KEY, envelope.id())));
        out.println("  " + OutputSafety.stripControlSequences(picked.label()));
        if (picked.consequences() != null) {
            out.println("  " + MessageCatalogue.format(locale, "operator.decision.follows", Map.of("consequence", OutputSafety.stripControlSequences(picked.consequences()))));
        }
        while (true) {
            var confirm = readLineSafe(reader, getMessage(locale, "operator.decision.confirm_prompt"));
            var choice = confirm.trim();
            if (choice.isEmpty()) {
                return new ConfirmOutcome(true, null, 0);
            }
            if (isBackChoice(choice, locale)) {
                return new ConfirmOutcome(false, 0, 0);
            }
            if (isQuitChoice(choice, locale)) {
                printLeftDialogue(out, envelope.id(), locale);
                return new ConfirmOutcome(false, null, 0);
            }
            if ("?".equals(choice)) {
                printEvidenceDetails(out, envelope.evidence(), locale);
            } else {
                out.println(getMessage(locale, INVALID_CHOICE_KEY));
                out.flush();
            }
        }
    }

    private record ReadBack(boolean repeat, @Nullable PartAnswer answer) {
    }

    private @Nullable PartAnswer readOwnWords(DialogueSession session, DecisionEnvelope envelope,
                        String locale) throws IOException {
        var wordsPrompt = getMessage(locale, "operator.decision.own_words_prompt") + " › ";
        while (true) {
            var wordsInput = readLineSafe(session.reader(), wordsPrompt).trim();
            if (wordsInput.isEmpty()) {
                return null;
            }
            var interp = interpret(session, envelope, wordsInput);
            var interpretedWords = wordsInput;
            if (interp.interpretationType() == InterpretationType.UNCLEAR) {
                var question = interp.clarificationQuestion();
                if (question == null) {
                    // Unclear words with nothing to ask are never posted as understood: ask again.
                    printUnclear(session, locale);
                    continue;
                }
                var clarification = clarified(session, envelope, wordsInput, question, locale);
                if (clarification == null) {
                    return null;
                }
                interp = clarification.interpretation();
                interpretedWords = clarification.words();
            }
            var readBack = readBack(session, envelope, interp, interpretedWords, locale);
            if (!readBack.repeat()) {
                return readBack.answer();
            }
        }
    }

    private static ReadBack readBack(DialogueSession session, DecisionEnvelope envelope,
                        DecisionInterpretation interp, String wordsInput, String locale) {
        var out = session.out();
        out.println();
        var reading = Objects.requireNonNullElse(interp.readBackText(), wordsInput);
        var understood = MessageCatalogue.format(locale, "decision.readback.understood",
                            Map.of("reading", TerminalSanitizer.sanitizeLine(reading)));
        out.println(understood);
        while (true) {
            var answer = readLineSafe(session.reader(), getMessage(locale, "operator.decision.readback_confirm")).trim();
            if (KEY_CHOOSE_OPTION.equalsIgnoreCase(answer)) {
                // The operator chooses an option instead: nothing is recorded.
                return new ReadBack(false, null);
            }
            if (answer.isEmpty()) {
                return new ReadBack(false, new PartAnswer(reading, true));
            }
            if (Keys.of(locale).edit().equalsIgnoreCase(answer)) {
                return new ReadBack(true, null);
            }
            // ? repeats what was understood; any other input is not offered at the read-back.
            out.println("?".equals(answer) ? understood : getMessage(locale, INVALID_CHOICE_KEY));
            out.flush();
        }
    }

    /**
     * A clarified reading: the interpretation and the words it was actually made from, the original
     * words with the operator's reply, so the read-back never drops the reply.
     */
    private record Clarification(DecisionInterpretation interpretation, String words) {
    }

    private @Nullable Clarification clarified(DialogueSession session, DecisionEnvelope envelope,
                        String words, String question, String locale) throws IOException {
        session.out().println(MessageCatalogue.format(locale, "decision.clarify.asks", Map.of(
                            ROLE_KEY, "interpret",
                            "question", TerminalSanitizer.sanitizeLine(question)
        )));
        var reply = readLineSafe(session.reader(), getMessage(locale, "operator.decision.reply_prompt") + " › ");
        var combinedWords = words + ". " + reply.trim();
        var clarifiedInterp = interpret(session, envelope, combinedWords);
        if (clarifiedInterp.interpretationType() != InterpretationType.UNCLEAR) {
            return new Clarification(clarifiedInterp, combinedWords);
        }
        printUnclear(session, locale);
        return null;
    }

    private static void printUnclear(DialogueSession session, String locale) {
        session.out().println(MessageCatalogue.format(locale, "decision.interpret.unclear_bound", Map.of(
                            REASON_KEY, getMessage(locale, "decision.interpret.unclear_reason"))));
        session.out().flush();
    }

    private DecisionInterpretation interpret(DialogueSession session, DecisionEnvelope envelope,
                        String words) throws IOException {
        var interpPath = resolveDecisionPath(OperatorUrlUtil.encodePathSegment(envelope.id())) + PATH_INTERPRET;
        var interpRes = session.runtime().send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of(WORDS_KEY, words))));
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

        var ansPath = resolveDecisionPath(OperatorUrlUtil.encodePathSegment(envelope.id())) + PATH_ANSWER;
        var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
        if (isAccepted(ansRes.status())) {
            var noteSuffix = noteText != null && !noteText.isBlank() ? " " + getMessage(locale, "operator.decision.with_note") : "";
            var goesOn = MessageCatalogue.format(locale, "operator.decision.goes_on", Map.of(
                                SCOPE_KEY, scopeSelector(envelope),
                                PROJECT_KEY, projectNoun(envelope, locale)
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
        for (var part : envelope.parts()) {
            var opt = pickOption(part, key);
            if (opt != null) {
                return OutputSafety.stripControlSequences(opt.label());
            }
        }
        return key;
    }

    /**
     * The selector value the catalogue's noun templates branch on. It comes from the envelope's
     * scope and blocks, never from the subject text: an entry decision selects {@code entry}, an
     * epic one {@code epic}, a project setup {@code setup}, everything else {@code plan}.
     *
     * @param envelope the decision envelope
     * @return the selector value
     */
    private static String scopeSelector(DecisionEnvelope envelope) {
        if ("entry".equalsIgnoreCase(envelope.blocks())) {
            return "entry";
        }
        return switch (envelope.scope()) {
            case EPIC -> "epic";
            case PROJECT_SETUP -> "setup";
            default -> PLAN_KEY;
        };
    }

    private static String projectNoun(DecisionEnvelope envelope, String locale) {
        var project = envelope.project();
        if (project != null && !project.isBlank()) {
            return project;
        }
        return getMessage(locale, "decision.project.fallback");
    }

    private String planNoun(DecisionEnvelope envelope, String locale) {
        var planId = envelope.plan();
        if (planId != null && !planId.isBlank()) {
            return planId;
        }
        if (plan != null && !plan.isBlank()) {
            return plan;
        }
        return getMessage(locale, "decision.waiver.plan_fallback");
    }

    private static String waiverGate(DecisionEnvelope envelope, String locale) {
        if (envelope.waiverGate() != null && !envelope.waiverGate().isBlank()) {
            return Display.gate(locale, envelope.waiverGate());
        }
        return getMessage(locale, "decision.waiver.gate_fallback");
    }

    private static boolean isAccepted(int status) {
        return status == 200 || status == 202 || status == 204;
    }

    /**
     * The title of the confirmation names the stake: costly, cannot be undone, or — only a
     * reversible decision of several parts is confirmed at all — no stake.
     */
    private static String confirmTitleKey(DecisionEnvelope envelope) {
        if (envelope.reversibility() == Reversibility.COSTLY) {
            return "operator.decision.confirm_title_costly";
        }
        if (envelope.reversibility() == Reversibility.REVERSIBLE) {
            return "operator.decision.confirm_title_parts";
        }
        return "operator.decision.confirm_title";
    }

    private static String getMessage(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }
}
