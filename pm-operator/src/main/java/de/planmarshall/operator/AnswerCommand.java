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
                                ? "/api/v1/plans/" + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid
                                : "/api/v1/decisions/" + safeQid;
            var qResponse = runtime.send(HttpRequest.get(qPath));
            if (qResponse.status() == 404) {
                out.println(MessageCatalogue.format(locale, "operator.decision.not_found", Map.of("id", questionId)));
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
            out.println(MessageCatalogue.format(locale, "operator.decision.header.waiver", Map.of(
                                "id", envelope.id(),
                                "scope", envelope.subject(),
                                "gate", envelope.waiverGate() != null ? envelope.waiverGate() : "gate"
            )));
            if (envelope.waiverReason() != null) {
                out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
            }
            if (envelope.evidence() != null && !envelope.evidence().isEmpty()) {
                var hint = formatEvidenceHint(envelope.evidence());
                if (!hint.isBlank()) {
                    out.println("  " + MessageCatalogue.format(locale, "operator.decision.evidence_hint", Map.of("hint", hint)));
                }
            }
        } else {
            var blocksKey = "decision.blocks." + (envelope.blocks() != null ? envelope.blocks().toLowerCase() : "plan");
            var revKey = "decision.reversibility." + (envelope.reversibility() != null ? envelope.reversibility().value() : "reversible");
            var blocksMsg = MessageCatalogue.hasKey(blocksKey) ? getMessage(locale, blocksKey) : "blocks the plan";
            var revMsg = MessageCatalogue.hasKey(revKey) ? getMessage(locale, revKey) : "can be undone";

            if (envelope.askingPhase() != null && !envelope.askingPhase().isBlank()) {
                out.println(MessageCatalogue.format(locale, "operator.decision.header.question", Map.of(
                                    "id", envelope.id(),
                                    "scope", envelope.subject(),
                                    "phase", envelope.askingPhase(),
                                    "blocks", blocksMsg,
                                    "reversibility", revMsg
                )));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.decision.header.question_short", Map.of(
                                    "id", envelope.id(),
                                    "scope", envelope.subject(),
                                    "blocks", blocksMsg,
                                    "reversibility", revMsg
                )));
            }

            var titleOrDesc = envelope.description() != null ? envelope.description() : envelope.title();
            if (titleOrDesc != null && !titleOrDesc.isBlank()) {
                out.printf("  %s%n", OutputSafety.stripControlSequences(titleOrDesc));
            }
            if (envelope.evidence() != null && !envelope.evidence().isEmpty()) {
                var hint = formatEvidenceHint(envelope.evidence());
                if (!hint.isBlank()) {
                    out.println("  " + MessageCatalogue.format(locale, "operator.decision.evidence_hint", Map.of("hint", hint)));
                }
            }
        }
        out.flush();
    }

    static String formatEvidenceHint(@Nullable Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "";
        }
        var file = evidence.get("file") != null ? String.valueOf(evidence.get("file")) : null;
        var lines = evidence.get("lines") != null ? String.valueOf(evidence.get("lines")) : null;
        var summary = evidence.get("summary") != null ? String.valueOf(evidence.get("summary")) : null;
        if (file != null && lines != null && summary != null) {
            return file + ":" + lines + " (" + summary + ")";
        }
        if (file != null && lines != null) {
            return file + ":" + lines;
        }
        if (file != null && summary != null) {
            return file + " (" + summary + ")";
        }
        var run = evidence.get("run") != null ? String.valueOf(evidence.get("run")) : null;
        var test = evidence.get("test") != null ? String.valueOf(evidence.get("test")) : null;
        if (run != null && test != null) {
            return test + " (" + run + ")";
        }
        if (file != null) return file;
        if (summary != null) return summary;
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

        var effectiveAction = action;
        if (effectiveAction == null) {
            if ("waive".equalsIgnoreCase(option) || ("y".equalsIgnoreCase(option) && questionId.startsWith("WR-"))) {
                effectiveAction = "waive";
            } else if ("decline".equalsIgnoreCase(option) || ("d".equalsIgnoreCase(option) && questionId.startsWith("WR-"))) {
                effectiveAction = "decline";
            } else if ("later".equalsIgnoreCase(option) || "l".equalsIgnoreCase(option)
                                || ("s".equalsIgnoreCase(option) && "de".equals(locale))) {
                effectiveAction = "later";
            } else if ("withdraw".equalsIgnoreCase(option) || "none fits".equalsIgnoreCase(option)
                                || "x".equalsIgnoreCase(option)) {
                effectiveAction = "withdraw";
            }
        }

        if ("later".equalsIgnoreCase(effectiveAction)) {
            out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                                "id", questionId,
                                "scope", "plan",
                                "project", "pm-mcp-clients"
            )));
            out.flush();
            return 0;
        }

        if ("withdraw".equalsIgnoreCase(effectiveAction)) {
            var withPath = "/api/v1/decisions/" + safeQid + "/withdraw";
            var resp = runtime.send(HttpRequest.json("POST", withPath, "{}"));
            if (resp.status() == 200 || resp.status() == 202 || resp.status() == 204) {
                out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                                    "id", questionId,
                                    "scope", "plan"
                )));
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("withdraw " + questionId, resp);
        }

        if (effectiveAction != null && ("waive".equalsIgnoreCase(effectiveAction) || "decline".equalsIgnoreCase(effectiveAction))) {
            if (reason == null || reason.isBlank()) {
                out.println(getMessage(locale, "operator.decision.reason_required"));
                out.flush();
                return 3;
            }
        }

        // Fetch envelope to resolve option label if option is specified
        DecisionEnvelope envelope = null;
        var qPath = plan != null
                            ? "/api/v1/plans/" + OperatorUrlUtil.encodePathSegment(plan) + "/decisions/" + safeQid
                            : "/api/v1/decisions/" + safeQid;
        var qResponse = runtime.send(HttpRequest.get(qPath));
        if (qResponse.status() == 200) {
            envelope = DecisionEnvelope.fromJson(qResponse.bodyText());
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("channel", "cli");
        if (effectiveAction != null && ("waive".equalsIgnoreCase(effectiveAction) || "decline".equalsIgnoreCase(effectiveAction))) {
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
            if ("waive".equalsIgnoreCase(effectiveAction)) {
                out.println(MessageCatalogue.format(locale, "operator.decision.waived", Map.of(
                                    "gate", envelope != null && envelope.waiverGate() != null ? envelope.waiverGate() : "gate",
                                    "plan", envelope != null ? extractPlanNoun(envelope) : (plan != null ? plan : "plan")
                )));
            } else if ("decline".equalsIgnoreCase(effectiveAction)) {
                out.println(MessageCatalogue.format(locale, "operator.decision.declined", Map.of("id", questionId)));
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
                                    "id", questionId,
                                    "label", label
                )));
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("answer " + questionId, response);
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

    private int runInteractiveWaiver(RuntimeClient runtime, PrintWriter out, LineReader reader, DecisionEnvelope envelope, String locale) throws IOException {
        out.println(MessageCatalogue.format(locale, "operator.decision.header.waiver", Map.of(
                            "id", envelope.id(),
                            "scope", envelope.subject(),
                            "gate", envelope.waiverGate() != null ? envelope.waiverGate() : "gate"
        )));
        if (envelope.waiverReason() != null) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(envelope.waiverReason()));
        }
        if (envelope.evidence() != null && !envelope.evidence().isEmpty()) {
            var hint = formatEvidenceHint(envelope.evidence());
            if (!hint.isBlank()) {
                out.println("  " + MessageCatalogue.format(locale, "operator.decision.evidence_hint", Map.of("hint", hint)));
            }
        }
        out.println();
        out.flush();

        var prompt = getMessage(locale, "operator.decision.waiver_prompt_full");
        while (true) {
            var choice = readLineSafe(reader, prompt);
            if (choice == null) {
                return 0;
            }
            if (choice.isBlank()) {
                out.println(MessageCatalogue.format(locale, "operator.decision.waiver_needs_answer", Map.of(
                                    "waive", "y", "decline", "d", "later", "de".equals(locale) ? "s" : "l")));
                out.flush();
                continue;
            }
            choice = choice.trim();

            if ("q".equalsIgnoreCase(choice) || "v".equalsIgnoreCase(choice)) {
                out.println(MessageCatalogue.format(locale, "operator.dialogue.left", Map.of("state", envelope.id())));
                out.flush();
                return 0;
            }

            if ("l".equalsIgnoreCase(choice) || "later".equalsIgnoreCase(choice)
                                || ("s".equalsIgnoreCase(choice) && "de".equals(locale))
                                || ("später".equalsIgnoreCase(choice) && "de".equals(locale))) {
                out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                                    "id", envelope.id(),
                                    "scope", extractScopeNoun(envelope),
                                    "project", extractProjectNoun(envelope)
                )));
                out.flush();
                return 0;
            }

            if ("?".equalsIgnoreCase(choice)) {
                var ev = formatEvidenceHint(envelope.evidence());
                if (!ev.isBlank()) {
                    out.println(MessageCatalogue.format(locale, "operator.decision.evidence_details", Map.of("evidence", ev)));
                } else {
                    out.println(getMessage(locale, "operator.decision.evidence_none"));
                }
                out.flush();
                continue;
            }

            if ("y".equalsIgnoreCase(choice) || "d".equalsIgnoreCase(choice)
                                || "waive".equalsIgnoreCase(choice) || "decline".equalsIgnoreCase(choice)
                                || ("g".equalsIgnoreCase(choice) && "de".equals(locale))
                                || ("a".equalsIgnoreCase(choice) && "de".equals(locale))) {
                String reqReason = "";
                while (reqReason.isBlank()) {
                    var line = readLineSafe(reader, getMessage(locale, "operator.decision.reason_prompt") + " › ");
                    if (line == null) {
                        return 0;
                    }
                    if (line.isBlank()) {
                        out.println(getMessage(locale, "operator.decision.reason_required"));
                        out.flush();
                    } else {
                        reqReason = line.trim();
                    }
                }
                var act = "d".equalsIgnoreCase(choice) || "decline".equalsIgnoreCase(choice) || "a".equalsIgnoreCase(choice) ? "decline" : "waive";
                var payload = new LinkedHashMap<String, Object>();
                payload.put("action", act);
                payload.put("reason", reqReason);
                payload.put("channel", "cli");

                var ansPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/answer";
                var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
                if (ansRes.status() == 200 || ansRes.status() == 202 || ansRes.status() == 204) {
                    if ("decline".equals(act)) {
                        out.println(MessageCatalogue.format(locale, "operator.decision.declined", Map.of("id", envelope.id())));
                    } else {
                        out.println(MessageCatalogue.format(locale, "operator.decision.waived", Map.of(
                                            "gate", envelope.waiverGate() != null ? envelope.waiverGate() : "gate",
                                            "plan", extractPlanNoun(envelope)
                        )));
                    }
                    out.flush();
                    return 0;
                }
                throw new OperatorRefusedException("answer " + envelope.id(), ansRes);
            }

            out.println(MessageCatalogue.format(locale, "operator.decision.waiver_needs_answer", Map.of(
                                "waive", "y", "decline", "d", "later", "de".equals(locale) ? "s" : "l")));
            out.flush();
        }
    }

    private int runInteractiveDecision(RuntimeClient runtime, PrintWriter out, LineReader reader, DecisionEnvelope envelope, String locale) throws IOException {
        var blocksKey = "decision.blocks." + (envelope.blocks() != null ? envelope.blocks().toLowerCase() : "plan");
        var revKey = "decision.reversibility." + (envelope.reversibility() != null ? envelope.reversibility().value() : "reversible");
        var blocksMsg = MessageCatalogue.hasKey(blocksKey) ? getMessage(locale, blocksKey) : "blocks the plan";
        var revMsg = MessageCatalogue.hasKey(revKey) ? getMessage(locale, revKey) : "can be undone";

        if (envelope.askingPhase() != null && !envelope.askingPhase().isBlank()) {
            out.println(MessageCatalogue.format(locale, "operator.decision.header.question", Map.of(
                                "id", envelope.id(),
                                "scope", envelope.subject(),
                                "phase", envelope.askingPhase(),
                                "blocks", blocksMsg,
                                "reversibility", revMsg
            )));
        } else {
            out.println(MessageCatalogue.format(locale, "operator.decision.header.question_short", Map.of(
                                "id", envelope.id(),
                                "scope", envelope.subject(),
                                "blocks", blocksMsg,
                                "reversibility", revMsg
            )));
        }

        var titleOrDesc = envelope.description() != null ? envelope.description() : envelope.title();
        if (titleOrDesc != null && !titleOrDesc.isBlank()) {
            out.printf("  %s%n", OutputSafety.stripControlSequences(titleOrDesc));
        }
        if (envelope.evidence() != null && !envelope.evidence().isEmpty()) {
            var hint = formatEvidenceHint(envelope.evidence());
            if (!hint.isBlank()) {
                out.println("  " + MessageCatalogue.format(locale, "operator.decision.evidence_hint", Map.of("hint", hint)));
            }
        }
        out.println();

        var part = envelope.parts().isEmpty() ? null : envelope.parts().get(0);
        DecisionOption recommendedOpt = null;
        if (part != null) {
            var recKey = part.recommendation();
            for (var opt : part.options()) {
                var isRec = opt.key().equals(recKey);
                if (isRec) {
                    recommendedOpt = opt;
                }
                var recMark = isRec ? " ←" : "  ";
                var supportStr = "";
                if (opt.quorumSupport() != null) {
                    var qs = opt.quorumSupport();
                    var bandKey = "decision.support.band." + qs.band().value();
                    var bandStr = MessageCatalogue.hasKey(bandKey) ? getMessage(locale, bandKey) : qs.band().value().replace('_', ' ');
                    var votesStr = MessageCatalogue.format(locale, "decision.support.votes", Map.of(
                                        "votes", qs.votesCount(),
                                        "total", qs.totalVotes(),
                                        "models", qs.modelsCount()
                    ));
                    supportStr = votesStr + "   " + bandStr;
                } else if (isRec && part.recommendingRole() != null) {
                    supportStr = MessageCatalogue.format(locale, "operator.decision.recommended_by", Map.of("role", part.recommendingRole()));
                }
                out.printf("    %s) %-25s%s  %s%n", opt.key(), OutputSafety.stripControlSequences(opt.label()), recMark, supportStr);
            }
            if (part.recommendationRationale() != null && part.recommendingRole() != null) {
                var rationaleOption = formatRationale(recommendedOpt, part.recommendationRationale());
                out.println("  " + MessageCatalogue.format(locale, "decision.recommendation.by", Map.of(
                                    "role", part.recommendingRole(),
                                    "option", OutputSafety.stripControlSequences(rationaleOption)
                )));
            }
            // Check dissent
            for (var opt : part.options()) {
                if (opt.quorumSupport() != null && opt.quorumSupport().dissentSummary() != null) {
                    var qs = opt.quorumSupport();
                    var dissentRole = qs.votes().stream()
                                        .filter(v -> v.reason() != null && !v.reason().isBlank())
                                        .map(QuorumVote::role)
                                        .findFirst().orElse("quorum-b");
                    out.println("  " + MessageCatalogue.format(locale, "decision.dissent", Map.of(
                                        "role", dissentRole,
                                        "reason", OutputSafety.stripControlSequences(qs.dissentSummary())
                    )));
                    break;
                }
            }
            out.println();
        }
        out.flush();

        String currentNote = null;
        var isReversible = envelope.reversibility() == Reversibility.REVERSIBLE;
        int optCount = part != null ? part.options().size() : 0;
        String rangeStr = optCount > 1 ? "1-" + optCount : "1";

        while (true) {
            String prompt;
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
                prompt = sb.toString();
            } else {
                var chooseStr = MessageCatalogue.format(locale, "operator.decision.prompt.choose", Map.of("range", rangeStr));
                prompt = chooseStr + "   " + ownWordsStr + "   " + chatStr + "   " + noneFitsStr + "   " + laterStr + "   ?   › ";
            }

            var line = readLineSafe(reader, prompt);
            if (line == null) {
                return 0;
            }
            var choice = line.trim();

            if (choice.isEmpty() && recommendedOpt != null && isReversible) {
                return submitChoice(runtime, out, envelope, recommendedOpt.key(), recommendedOpt.label(), currentNote, locale);
            }

            if ("q".equalsIgnoreCase(choice) || "v".equalsIgnoreCase(choice)) {
                out.println(MessageCatalogue.format(locale, "operator.dialogue.left", Map.of("state", envelope.id())));
                out.flush();
                return 0;
            }

            if ("l".equalsIgnoreCase(choice) || "later".equalsIgnoreCase(choice)
                                || ("s".equalsIgnoreCase(choice) && "de".equals(locale))
                                || ("später".equalsIgnoreCase(choice) && "de".equals(locale))) {
                out.println(MessageCatalogue.format(locale, "operator.decision.later_done", Map.of(
                                    "id", envelope.id(),
                                    "scope", extractScopeNoun(envelope),
                                    "project", extractProjectNoun(envelope)
                )));
                out.flush();
                return 0;
            }

            if ("x".equalsIgnoreCase(choice) || "none fits".equalsIgnoreCase(choice)
                                || ("nichts passt".equalsIgnoreCase(choice) && "de".equals(locale))) {
                var confirmPrompt = MessageCatalogue.format(locale, "operator.decision.none_fits_confirm", Map.of(
                                    "scope", extractScopeNoun(envelope),
                                    "project", extractProjectNoun(envelope)
                )) + " [Enter]   " + ("de".equals(locale) ? "z) zurück   › " : "b) back   › ");
                var confirm = readLineSafe(reader, confirmPrompt);
                if (confirm == null) return 0;
                if (confirm.isBlank() || "y".equalsIgnoreCase(confirm.trim()) || "j".equalsIgnoreCase(confirm.trim())) {
                    var withPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/withdraw";
                    runtime.send(HttpRequest.json("POST", withPath, "{}"));
                    out.println(MessageCatalogue.format(locale, "operator.decision.none_fits_done", Map.of(
                                        "id", envelope.id(),
                                        "scope", extractScopeNoun(envelope)
                    )));
                    out.flush();
                    return 0;
                }
                continue;
            }

            if ("m".equalsIgnoreCase(choice)) {
                var noteInput = readLineSafe(reader, getMessage(locale, "operator.decision.note_prompt") + " › ");
                if (noteInput == null) return 0;
                if (!noteInput.isBlank()) {
                    currentNote = noteInput.trim();
                }
                continue;
            }

            if ("w".equalsIgnoreCase(choice)) {
                return handleInteractiveOwnWords(runtime, out, reader, envelope, locale);
            }

            if ("?".equalsIgnoreCase(choice)) {
                var ev = formatEvidenceHint(envelope.evidence());
                if (!ev.isBlank()) {
                    out.println(MessageCatalogue.format(locale, "operator.decision.evidence_details", Map.of("evidence", ev)));
                } else {
                    out.println(getMessage(locale, "operator.decision.evidence_none"));
                }
                out.flush();
                continue;
            }

            if ("?r".equalsIgnoreCase(choice) && part != null) {
                if (part.recommendationRationale() != null && part.recommendingRole() != null) {
                    var rationaleOption = formatRationale(recommendedOpt, part.recommendationRationale());
                    out.println("  " + MessageCatalogue.format(locale, "decision.recommendation.by", Map.of(
                                        "role", part.recommendingRole(),
                                        "option", OutputSafety.stripControlSequences(rationaleOption)
                    )));
                }
                for (var opt : part.options()) {
                    if (opt.quorumSupport() != null && opt.quorumSupport().dissentSummary() != null) {
                        var qs = opt.quorumSupport();
                        var dissentRole = qs.votes().stream()
                                            .filter(v -> v.reason() != null && !v.reason().isBlank())
                                            .map(QuorumVote::role)
                                            .findFirst().orElse("quorum-b");
                        out.println("  " + MessageCatalogue.format(locale, "decision.dissent", Map.of(
                                            "role", dissentRole,
                                            "reason", OutputSafety.stripControlSequences(qs.dissentSummary())
                        )));
                        break;
                    }
                }
                out.flush();
                continue;
            }

            // Number or key choice
            if (part != null) {
                var picked = part.options().stream()
                                    .filter(o -> o.key().equalsIgnoreCase(choice))
                                    .findFirst().orElse(null);
                if (picked != null) {
                    if (!isReversible) {
                        out.println();
                        out.println(MessageCatalogue.format(locale, "operator.decision.confirm_title", Map.of("id", envelope.id())));
                        out.printf("  %s%n", OutputSafety.stripControlSequences(picked.label()));
                        if (picked.consequences() != null) {
                            out.println("  " + MessageCatalogue.format(locale, "operator.decision.follows", Map.of("consequence", OutputSafety.stripControlSequences(picked.consequences()))));
                        }
                        var confirm = readLineSafe(reader, getMessage(locale, "operator.decision.confirm_prompt"));
                        if (confirm == null) return 0;
                        if (!confirm.isBlank() && !"y".equalsIgnoreCase(confirm.trim()) && !"j".equalsIgnoreCase(confirm.trim())) {
                            continue;
                        }
                    }
                    return submitChoice(runtime, out, envelope, picked.key(), picked.label(), currentNote, locale);
                }
            }

            out.println(getMessage(locale, "operator.decision.invalid_choice"));
            out.flush();
        }
    }

    private int handleInteractiveOwnWords(RuntimeClient runtime, PrintWriter out, LineReader reader, DecisionEnvelope envelope, String locale) throws IOException {
        var wordsPrompt = getMessage(locale, "operator.decision.own_words_prompt") + " › ";
        var wordsInput = readLineSafe(reader, wordsPrompt);
        if (wordsInput == null || wordsInput.isBlank()) {
            return 0;
        }

        var interpPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/interpret";
        var interpRes = runtime.send(HttpRequest.json("POST", interpPath, JsonTree.write(Map.of("words", wordsInput.trim()))));
        var interp = DecisionInterpretation.fromJson(interpRes.bodyText());

        if (interp.interpretationType() == InterpretationType.UNCLEAR && interp.clarificationQuestion() != null) {
            out.println(MessageCatalogue.format(locale, "decision.clarify.asks", Map.of(
                                "role", "interpret",
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
        payload.put("channel", "cli");
        var ansPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/answer";
        runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));

        if (interp.readBackText() != null) {
            out.println(MessageCatalogue.format(locale, "operator.decision.answered", Map.of(
                                "id", envelope.id(),
                                "label", interp.readBackText()
            )));
        } else {
            out.println(MessageCatalogue.format(locale, "operator.decision.answered_own_words", Map.of("id", envelope.id())));
        }
        out.flush();
        return 0;
    }

    private int submitChoice(RuntimeClient runtime, PrintWriter out, DecisionEnvelope envelope, String key, String label, @Nullable String noteText, String locale) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("option", key);
        payload.put("channel", "cli");
        if (noteText != null) payload.put("note", noteText);

        var ansPath = "/api/v1/decisions/" + OperatorUrlUtil.encodePathSegment(envelope.id()) + "/answer";
        var ansRes = runtime.send(HttpRequest.json("POST", ansPath, JsonTree.write(payload)));
        if (ansRes.status() == 200 || ansRes.status() == 202 || ansRes.status() == 204) {
            var noteSuffix = noteText != null && !noteText.isBlank() ? " " + getMessage(locale, "operator.decision.with_note") : "";
            var goesOn = MessageCatalogue.format(locale, "operator.decision.goes_on", Map.of(
                                "scope", extractScopeNoun(envelope),
                                "project", extractProjectNoun(envelope)
            ));
            out.println(MessageCatalogue.format(locale, "operator.decision.answered_goes_on", Map.of(
                                "id", envelope.id(),
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

    private static String getMessage(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }
}
