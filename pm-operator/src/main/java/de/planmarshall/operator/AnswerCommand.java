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
import java.util.concurrent.Callable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.impl.DumbTerminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.decision.DecisionEnvelope;
import de.planmarshall.api.decision.DecisionInterpretation;
import de.planmarshall.api.decision.InterpretationType;
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

        if (option == null && (words == null || words.isBlank()) && (action == null || action.isBlank())) {
            var safeQid = OperatorUrlUtil.encodePathSegment(questionId);
            var qPath = plan != null
                                ? "/api/v1/plans/" + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid
                                : "/api/v1/decisions/" + safeQid;
            var qResponse = runtime.send(HttpRequest.get(qPath));
            if (qResponse.status() == 404) {
                out.printf("Question %s not found.%n", questionId);
                out.flush();
                return 1;
            }

            var envelope = DecisionEnvelope.fromJson(qResponse.bodyText());
            if (noInput) {
                printDecisionDetail(out, envelope);
                out.println("Choice or option required (--no-input specified).");
                out.flush();
                return 3;
            }
            if (interactive && context.isInteractiveTerminal()) {
                return handleInteractive(runtime, out, envelope);
            }
            printDecisionDetail(out, envelope);
            out.flush();
            return 0;
        }

        return submitAnswer(runtime, out);
    }

    private void printDecisionDetail(PrintWriter out, DecisionEnvelope envelope) {
        if (envelope.isWaiver()) {
            out.printf("Waiver request %s · %s · gate %s%n",
                                envelope.id(), envelope.subject(),
                                envelope.waiverGate() != null ? envelope.waiverGate() : "gate");
            if (envelope.waiverReason() != null) {
                out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
            }
        } else {
            var blocksKey = "decision.blocks." + (envelope.blocks() != null ? envelope.blocks().toLowerCase() : "plan");
            var revKey = "decision.reversibility." + (envelope.reversibility() != null ? envelope.reversibility().value() : "reversible");
            var blocksMsg = MessageCatalogue.hasKey(blocksKey) ? MessageCatalogue.get(blocksKey) : "blocks the plan";
            var revMsg = MessageCatalogue.hasKey(revKey) ? MessageCatalogue.get(revKey) : "can be undone";
            out.printf("Question %s · %s · %s · %s · %s%n",
                                envelope.id(), envelope.subject(),
                                envelope.askingPhase() != null ? envelope.askingPhase() : "",
                                blocksMsg, revMsg);
            var titleOrDesc = envelope.description() != null ? envelope.description() : envelope.title();
            if (titleOrDesc != null && !titleOrDesc.isBlank()) {
                out.printf("  %s%n", OutputSafety.stripControlSequences(titleOrDesc));
            }
        }
        out.flush();
    }

    private int submitAnswer(RuntimeClient runtime, PrintWriter out) throws IOException {
        var safeQid = OperatorUrlUtil.encodePathSegment(questionId);
        var effectiveAction = action;
        if (effectiveAction == null) {
            if ("waive".equalsIgnoreCase(option) || ("y".equalsIgnoreCase(option) && questionId.startsWith("WR-"))) {
                effectiveAction = "waive";
            } else if ("decline".equalsIgnoreCase(option) || ("d".equalsIgnoreCase(option) && questionId.startsWith("WR-"))) {
                effectiveAction = "decline";
            } else if ("later".equalsIgnoreCase(option) || "l".equalsIgnoreCase(option)) {
                effectiveAction = "later";
            } else if ("withdraw".equalsIgnoreCase(option) || "none fits".equalsIgnoreCase(option) || "x".equalsIgnoreCase(option)) {
                effectiveAction = "withdraw";
            }
        }

        if ("later".equalsIgnoreCase(effectiveAction)) {
            out.printf(MessageCatalogue.format("operator.decision.later_done", Map.of(
                                "id", questionId,
                                "scope", "plan",
                                "project", "pm-mcp-clients"
            )) + "%n");
            out.flush();
            return 0;
        }

        if ("withdraw".equalsIgnoreCase(effectiveAction)) {
            var withPath = "/api/v1/decisions/" + safeQid + "/withdraw";
            var resp = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (resp.status() == 200 || resp.status() == 202 || resp.status() == 204) {
                out.printf("✓ %s: you asked to think again.%n", questionId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("withdraw " + questionId, resp);
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("channel", "cli");
        if (effectiveAction != null && ("waive".equalsIgnoreCase(effectiveAction) || "decline".equalsIgnoreCase(effectiveAction))) {
            if (reason == null || reason.isBlank()) {
                out.println(MessageCatalogue.get("operator.decision.reason_required"));
                out.flush();
                return 3;
            }
            payload.put("action", effectiveAction);
            payload.put("reason", reason);
        } else {
            if (option != null) payload.put("option", option);
            if (words != null) payload.put("words", words);
        }
        if (note != null) payload.put("note", note);

        var answerPath = plan != null
                            ? "/api/v1/plans/" + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid + "/answer"
                            : "/api/v1/decisions/" + safeQid + "/answer";

        var response = runtime.send(HttpRequest.json("POST", answerPath, JsonTree.write(payload)));
        if (response.status() == 404) {
            response = runtime.send(HttpRequest.json("POST", "/api/v1/questions/" + safeQid + "/answers", JsonTree.write(payload)));
        }
        if (response.status() == 200 || response.status() == 202 || response.status() == 204) {
            var answerDisplay = option != null ? option : words;
            if (answerDisplay == null) answerDisplay = effectiveAction;
            out.printf("✓ Answered %s with option %s.%n", questionId, answerDisplay);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + questionId, response);
    }

    private static @Nullable String readLineSafe(LineReader reader, String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (EndOfFileException | UserInterruptException e) {
            return null;
        }
    }

    private int handleInteractive(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope) {
        try (var terminal = context.createTerminal()) {
            if (System.console() == null && !(terminal instanceof DumbTerminal)) {
                printDecisionDetail(out, envelope);
                out.flush();
                return 0;
            }
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            if (envelope.isWaiver()) {
                return runInteractiveWaiver(runtime, out, reader, envelope);
            }
            return runInteractiveDecision(runtime, out, reader, envelope);
        }
        // cui-rewrite:disable InvalidExceptionUsageRecipe
        catch (IOException | RuntimeException e) {
            out.println("Error reading interactive input: " + e.getMessage());
            out.flush();
            return 1;
        }
    }

    private int runInteractiveWaiver(RuntimeClient runtime, PrintWriter out, LineReader reader, DecisionEnvelope envelope) throws IOException {
        out.printf("Waiver request %s · %s · gate %s%n",
                            envelope.id(), envelope.subject(),
                            envelope.waiverGate() != null ? envelope.waiverGate() : "gate");
        if (envelope.waiverReason() != null) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        if (envelope.evidence() != null && !envelope.evidence().isEmpty()) {
            out.println("  ? shows the evidence: the failing run, the test");
        }
        out.println();
        out.flush();

        while (true) {
            var prompt = "Waive the gate or decline the request?   y) waive, with a reason   d) decline, with a reason   l) later   ?   › ";
            var choice = readLineSafe(reader, prompt);
            if (choice == null) {
                return 0;
            }
            if (choice.isBlank()) {
                out.println("Type y, d or l.");
                out.flush();
                continue;
            }
            choice = choice.trim();

            if ("l".equalsIgnoreCase(choice) || "later".equalsIgnoreCase(choice)) {
                out.printf("%s stays open; the plan waits for it. questions lists it.%n", envelope.id());
                out.flush();
                return 0;
            }

            if ("?".equalsIgnoreCase(choice)) {
                out.println("Evidence: " + envelope.evidence());
                out.flush();
                continue;
            }

            if ("y".equalsIgnoreCase(choice) || "d".equalsIgnoreCase(choice)
                                || "waive".equalsIgnoreCase(choice) || "decline".equalsIgnoreCase(choice)) {
                String reqReason = "";
                while (reqReason.isBlank()) {
                    var line = readLineSafe(reader, "Reason (required): › ");
                    if (line == null) {
                        return 0;
                    }
                    if (line.isBlank()) {
                        out.println(MessageCatalogue.get("operator.decision.reason_required"));
                        out.flush();
                    } else {
                        reqReason = line.trim();
                    }
                }
                var act = "d".equalsIgnoreCase(choice) || "decline".equalsIgnoreCase(choice) ? "decline" : "waive";
                var payload = new LinkedHashMap<String, Object>();
                payload.put("action", act);
                payload.put("reason", reqReason);
                payload.put("channel", "cli");

                var ansPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/answer";
                var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
                if (ansRes.status() == 200) {
                    if ("decline".equals(act)) {
                        out.println(MessageCatalogue.format("operator.decision.declined", Map.of("id", envelope.id())));
                    } else {
                        out.println(MessageCatalogue.format("operator.decision.waived", Map.of(
                                            "gate", envelope.waiverGate() != null ? envelope.waiverGate() : "gate",
                                            "plan", extractPlanNoun(envelope)
                        )));
                    }
                    out.flush();
                    return 0;
                }
                throw new OperatorRefusedException("answer " + envelope.id(), ansRes);
            }

            out.println("Type y, d or l.");
            out.flush();
        }
    }

    private int runInteractiveDecision(RuntimeClient runtime, PrintWriter out, LineReader reader, DecisionEnvelope envelope) throws IOException {
        var blocksKey = "decision.blocks." + envelope.blocks().toLowerCase();
        var revKey = "decision.reversibility." + envelope.reversibility().value();
        out.printf("Question %s · %s · %s · %s · %s%n",
                            envelope.id(), envelope.subject(),
                            envelope.askingPhase() != null ? envelope.askingPhase() : "",
                            MessageCatalogue.get(blocksKey), MessageCatalogue.get(revKey));
        out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.description() != null ? envelope.description() : envelope.title()));
        if (envelope.evidence() != null && !envelope.evidence().isEmpty()) {
            out.println("  ? shows the evidence: the conflict, both versions of the method");
        }
        out.println();

        var part = envelope.parts().isEmpty() ? null : envelope.parts().get(0);
        if (part != null) {
            for (var opt : part.options()) {
                var isRec = opt.key().equals(part.recommendation());
                var recMark = isRec ? " ←" : "  ";
                var supportStr = "";
                if (opt.quorumSupport() != null) {
                    var qs = opt.quorumSupport();
                    var bandStr = qs.band().value().replace('_', ' ');
                    supportStr = String.format("%d of %d votes · %d model%s   %s",
                                        qs.votesCount(), qs.totalVotes(), qs.modelsCount(),
                                        qs.modelsCount() > 1 ? "s" : "", bandStr);
                } else if (isRec) {
                    supportStr = "recommended by " + part.recommendingRole();
                }
                out.printf("    %s) %-25s%s  %s%n", opt.key(), OutputSafety.stripControlSequences(opt.label()), recMark, supportStr);
            }
            if (part.recommendationRationale() != null) {
                out.printf("  Recommended by %s: %s%n", part.recommendingRole(), OutputSafety.stripControlSequences(part.recommendationRationale()));
            }
            // Check dissent
            for (var opt : part.options()) {
                if (opt.quorumSupport() != null && opt.quorumSupport().dissentSummary() != null) {
                    out.printf("  Against it (quorum-b): %s%n", OutputSafety.stripControlSequences(opt.quorumSupport().dissentSummary()));
                    break;
                }
            }
            out.println();
        }
        out.flush();

        String currentNote = null;
        var recKey = part != null ? part.recommendation() : null;
        var isReversible = envelope.reversibility() == Reversibility.REVERSIBLE;
        var recommendedOpt = recKey != null && part != null
                            ? part.options().stream().filter(o -> recKey.equals(o.key())).findFirst().orElse(null)
                            : null;

        while (true) {
            String prompt;
            if (recommendedOpt != null && isReversible) {
                prompt = String.format("%s? [Enter]   other: 1-3   w) in my own words   %s   h) chat   x) none fits   l) later   ?   › ",
                                    recommendedOpt.label(),
                                    currentNote != null ? "m) " + MessageCatalogue.get("operator.decision.note_set") : "m) add a note");
            } else {
                prompt = "Choose: 1-3   w) in my own words   h) chat   x) none fits   l) later   ?   › ";
            }

            var line = readLineSafe(reader, prompt);
            if (line == null) {
                return 0;
            }
            var choice = line.trim();

            if (choice.isEmpty() && recommendedOpt != null && isReversible) {
                return submitChoice(runtime, out, envelope, recommendedOpt.key(), recommendedOpt.label(), currentNote);
            }

            if ("l".equalsIgnoreCase(choice) || "later".equalsIgnoreCase(choice)) {
                out.printf(MessageCatalogue.format("operator.decision.later_done", Map.of(
                                    "id", envelope.id(),
                                    "scope", extractScopeNoun(envelope),
                                    "project", extractProjectNoun(envelope)
                )) + "%n");
                out.flush();
                return 0;
            }

            if ("x".equalsIgnoreCase(choice) || "none fits".equalsIgnoreCase(choice)) {
                var confirm = readLineSafe(reader, String.format("None fits: the %s thinks again and asks anew. Send? [Enter]   b) back   › ", extractScopeNoun(envelope)));
                if (confirm == null) return 0;
                if (confirm.isBlank() || "y".equalsIgnoreCase(confirm.trim())) {
                    var withPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/withdraw";
                    runtime.send(HttpRequest.json("POST", withPath, "{}"));
                    out.printf("✓ %s: you asked the %s to think again.%n", envelope.id(), extractScopeNoun(envelope));
                    out.flush();
                    return 0;
                }
                continue;
            }

            if ("m".equalsIgnoreCase(choice)) {
                var noteInput = readLineSafe(reader, MessageCatalogue.get("operator.decision.note_prompt") + " › ");
                if (noteInput == null) return 0;
                if (!noteInput.isBlank()) {
                    currentNote = noteInput.trim();
                }
                continue;
            }

            if ("w".equalsIgnoreCase(choice)) {
                return handleInteractiveOwnWords(runtime, out, reader, envelope);
            }

            if ("?".equalsIgnoreCase(choice)) {
                out.println("Evidence details: " + envelope.evidence());
                out.flush();
                continue;
            }

            // Number choice
            if (part != null) {
                var picked = part.options().stream().filter(o -> o.key().equalsIgnoreCase(choice)).findFirst().orElse(null);
                if (picked != null) {
                    if (!isReversible) {
                        out.println();
                        out.printf("Your decision · %s · cannot be undone%n", envelope.id());
                        out.printf("  %s%n", picked.label());
                        if (picked.consequences() != null) {
                            out.printf("  Follows: %s%n", picked.consequences());
                        }
                        var confirm = readLineSafe(reader, "Decide? [Enter]   b) back   ?   › ");
                        if (confirm == null) return 0;
                        if (!confirm.isBlank() && !"y".equalsIgnoreCase(confirm.trim())) {
                            continue;
                        }
                    }
                    return submitChoice(runtime, out, envelope, picked.key(), picked.label(), currentNote);
                }
            }

            out.println("Please choose a valid option or action.");
            out.flush();
        }
    }

    private int handleInteractiveOwnWords(RuntimeClient runtime, PrintWriter out, LineReader reader, DecisionEnvelope envelope) throws IOException {
        var wordsPrompt = MessageCatalogue.get("operator.decision.own_words_prompt") + " › ";
        var wordsInput = readLineSafe(reader, wordsPrompt);
        if (wordsInput == null || wordsInput.isBlank()) {
            return 0;
        }

        var interpPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/interpret";
        var interpRes = runtime.send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of("words", wordsInput.trim()))));
        var interp = DecisionInterpretation.fromJson(interpRes.bodyText());

        if (interp.interpretationType() == InterpretationType.UNCLEAR && interp.clarificationQuestion() != null) {
            out.printf("interpret asks: %s%n", interp.clarificationQuestion());
            var reply = readLineSafe(reader, MessageCatalogue.get("operator.decision.reply_prompt") + " › ");
            var combinedWords = wordsInput.trim() + ". " + (reply != null ? reply.trim() : "");
            interpRes = runtime.send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of("words", combinedWords))));
            interp = DecisionInterpretation.fromJson(interpRes.bodyText());
        }

        out.println();
        out.printf("I understood: %s%n", interp.readBackText() != null ? interp.readBackText() : wordsInput.trim());
        var confirm = readLineSafe(reader, "Correct? [Enter]   e) edit my words   o) choose an option instead   ?   › ");
        if (confirm == null || (!confirm.isBlank() && !"y".equalsIgnoreCase(confirm.trim()))) {
            return 0;
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("words", wordsInput.trim());
        payload.put("channel", "cli");
        var ansPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/answer";
        runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));

        if (interp.readBackText() != null) {
            out.printf("✓ %s answered: %s%n", envelope.id(), interp.readBackText());
        } else {
            out.println(MessageCatalogue.format("operator.decision.answered_own_words", Map.of("id", envelope.id())));
        }
        out.flush();
        return 0;
    }

    private int submitChoice(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope, String key, String label, @Nullable String noteText) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("option", key);
        payload.put("channel", "cli");
        if (noteText != null) payload.put("note", noteText);

        var ansPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/answer";
        var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
        if (ansRes.status() == 200 || ansRes.status() == 202 || ansRes.status() == 204) {
            var noteSuffix = noteText != null && !noteText.isBlank() ? " " + MessageCatalogue.get("operator.decision.with_note") : "";
            var goesOn = MessageCatalogue.format("operator.decision.goes_on", Map.of(
                                "scope", extractScopeNoun(envelope),
                                "project", extractProjectNoun(envelope)
            ));
            out.printf("✓ %s answered: %s%s. %s%n", envelope.id(), label, noteSuffix, goesOn);
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
        return "plan";
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
}
