/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.dialogue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

/**
 * One dialogue over the states a runtime reports ({@code CLI-UC-S-06}, {@code CLI-UC-P-01}): fetch
 * the state, render its frame (step line, content, prompt), read the choice, send the chosen link,
 * and repeat until the state is final or the operator leaves with {@code q}.
 *
 * <p>The texts of a frame come from the catalogue by state id: the title from
 * {@code onboard.<state id>.title} (or {@code project.<state id>.title}), the label of a reported
 * fact from the state's own label key, the label of a link from {@code onboard.link.<link id>}.
 * Nothing the runtime reports is ever printed as a raw identifier: a state without a catalogue
 * title shows the labelled fallback, a field without a label is not printed.
 *
 * <p>The same renderer serves the machine onboarding and the onboarding of a project; what differs
 * is the {@link Source} the dialogue reads, never the frame. A state with no link is a state whose
 * runtime is still working: the dialogue re-reads it after a bounded pause instead of asking.
 */
public final class StateDialogue {

    /** How long one working state may stay without a link before the dialogue gives up. */
    private static final long WORKING_TIMEOUT_NANOS = 60_000_000_000L;
    private static final long POLL_PAUSE_NANOS = 100_000_000L;
    private static final String PROJECT_PREFIX = "project.";
    private static final String ONBOARD_PREFIX = "onboard.";
    private static final String COMPLETED_KIND = "completed";
    private static final int CONTENT_WIDTH = 76;
    private static final List<String> STEP_KEYS = List.of(
                        "language", "harnesses", "models", "roles", "credentials", "providers", "plans");

    /**
     * One link a state offers.
     *
     * @param id          the link's id as the runtime reports it
     * @param rel         the link relation, if the runtime reports one
     * @param preselected whether Enter takes this link
     */
    public record Link(String id, @Nullable String rel, boolean preselected) {

        public Link {
            Objects.requireNonNull(id, "id");
        }
    }

    /**
     * One state as the runtime reports it.
     *
     * @param stateId    the state id
     * @param kind       the state's kind, {@code completed} for a final state
     * @param step       the step the state belongs to, 1-based; 0 when the state has no step
     * @param totalSteps the number of steps of the run
     * @param summary    the summary the runtime reports, if any
     * @param facts      the scalar facts the state reports
     * @param links      the links the state offers, in the runtime's order
     */
    public record Frame(String stateId, @Nullable String kind, int step, int totalSteps,
    @Nullable String summary, Map<String, Object> facts, List<Link> links) {

        public Frame {
            Objects.requireNonNull(stateId, "stateId");
            facts = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(
                                Objects.requireNonNull(facts, "facts")));
            links = List.copyOf(Objects.requireNonNull(links, "links"));
        }

        /**
         * @return whether this state ends the dialogue
         */
        public boolean isFinal() {
            return COMPLETED_KIND.equals(kind) || "onboard.completed".equals(stateId)
                                || "project.completed".equals(stateId);
        }

        /**
         * @return the link Enter takes, or {@code null} when the state declares no preselection
         */
        public @Nullable Link primary() {
            for (var link : links) {
                if (link.preselected()) {
                    return link;
                }
            }
            return null;
        }
    }

    /**
     * The runtime a dialogue reads and acts on.
     */
    public interface Source {

        /**
         * @return the state the runtime reports now
         * @throws IOException on transport failure
         */
        Frame fetch() throws IOException;

        /**
         * Sends the chosen link.
         *
         * @param linkId the link's id
         * @return the state the runtime reports afterwards
         * @throws IOException on transport failure
         */
        Frame choose(String linkId) throws IOException;
    }

    private final Source source;
    private final PrintWriter out;
    private final String locale;
    private final Function<String, ChoicePrompt> prompts;

    /**
     * @param source  the runtime to read and act on
     * @param out     the output of the dialogue
     * @param locale  the interface locale
     * @param prompts creates the strict choice prompt of one state, by its state id
     */
    public StateDialogue(Source source, PrintWriter out, String locale,
                        Function<String, ChoicePrompt> prompts) {
        this.source = Objects.requireNonNull(source, "source");
        this.out = Objects.requireNonNull(out, "out");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.prompts = Objects.requireNonNull(prompts, "prompts");
    }

    /**
     * Runs the dialogue until the state is final or the operator leaves it.
     *
     * @return the final state
     * @throws IOException on transport failure
     */
    public Frame run() throws IOException {
        var frame = source.fetch();
        @Nullable Frame last = null;
        var deadline = System.nanoTime() + WORKING_TIMEOUT_NANOS;
        while (true) {
            if (!frame.equals(last)) {
                render(frame);
                last = frame;
            }
            if (frame.isFinal()) {
                return frame;
            }
            if (frame.links().isEmpty()) {
                if (System.nanoTime() > deadline) {
                    out.println(MessageCatalogue.format(locale, "onboard.state.waits", Map.of()));
                    out.flush();
                    return frame;
                }
                LockSupport.parkNanos(POLL_PAUSE_NANOS);
                frame = source.fetch();
                continue;
            }
            var chosen = ask(frame);
            frame = source.choose(chosen.id());
        }
    }

    private Link ask(Frame frame) {
        var prompt = prompts.apply(frame.stateId())
                            .leaveLabel(title(frame));
        for (var link : frame.links()) {
            prompt.addChoice(new ChoicePrompt.Choice(link.id(), label(frame, link.id()), null, null,
                                link.preselected()));
        }
        var chosen = prompt.ask();
        for (var link : frame.links()) {
            if (link.id().equals(chosen.key())) {
                return link;
            }
        }
        return frame.links().getFirst();
    }

    private void render(Frame frame) {
        out.println(stepLine(frame));
        for (var line : content(frame)) {
            for (var wrapped : wrap(line, CONTENT_WIDTH)) {
                out.println("  " + wrapped);
            }
        }
        out.flush();
    }

    /**
     * @param text  the line to wrap
     * @param width the width of the content column
     * @return the line broken at its spaces, never inside a word
     */
    private static List<String> wrap(String text, int width) {
        var lines = new ArrayList<String>();
        var current = new StringBuilder();
        for (var word : text.split(" ")) {
            if (!current.isEmpty() && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }
        lines.add(current.toString());
        return lines;
    }

    /**
     * The frame's step line: {@code [n/7] Step · Title}, and the title alone where the state is not
     * one step of the run.
     *
     * @param frame the state
     * @return the rendered line
     */
    String stepLine(Frame frame) {
        var title = title(frame);
        if (frame.step() < 1 || frame.totalSteps() < 1) {
            return title;
        }
        return MessageCatalogue.format(locale, "onboard.state.step_line", Map.of(
                            "step", frame.step(), "total", frame.totalSteps(),
                            "name", stepName(frame), "title", title));
    }

    /**
     * The frame's content: the summary the runtime reports and every scalar fact whose field has a
     * catalogue label. A field without a label is left out, so no raw identifier reaches the screen.
     *
     * @param frame the state
     * @return the rendered content lines
     */
    List<String> content(Frame frame) {
        var lines = new ArrayList<String>();
        if ("onboard.mode".equals(frame.stateId())) {
            lines.add(MessageCatalogue.format(locale, "onboard.entry.intro", Map.of()));
            addStepOverview(lines);
            return lines;
        }
        if ("onboard.completed".equals(frame.stateId())) {
            lines.add(MessageCatalogue.format(locale, "onboard.completed.next", Map.of()));
            return lines;
        }
        var title = title(frame);
        if (frame.summary() != null && !frame.summary().isBlank() && !frame.summary().equals(title)) {
            lines.add(frame.summary());
        }
        for (var entry : frame.facts().entrySet()) {
            var label = factLabel(frame.stateId(), entry.getKey());
            if (label != null && isScalar(entry.getValue())) {
                var value = rendered(entry.getValue());
                if (!isRawToken(value)) {
                    lines.add(MessageCatalogue.format(locale, "onboard.state.fact_line",
                                        Map.of("label", label, "value", value)));
                }
            }
        }
        return lines;
    }

    private void addStepOverview(List<String> lines) {
        for (var i = 0; i < STEP_KEYS.size(); i++) {
            lines.add(MessageCatalogue.format(locale, "onboard.state.step_entry", Map.of(
                                "number", i + 1,
                                "name", MessageCatalogue.format(locale, "onboard.step." + STEP_KEYS.get(i),
                                                    Map.of()),
                                "what", MessageCatalogue.format(locale,
                                                    "onboard.entry.step." + STEP_KEYS.get(i), Map.of()))));
        }
    }

    /**
     * @param value a reported value
     * @return whether the value is a raw wire token (a dotted enum such as {@code landing.pull-request})
     *         that only the catalogue may render; such a value is left out rather than printed raw
     */
    private static boolean isRawToken(String value) {
        return value.matches("[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)+");
    }

    private String rendered(Object value) {
        if (value instanceof Boolean flag) {
            return MessageCatalogue.format(locale, "onboard.state.boolean." + flag, Map.of());
        }
        return String.valueOf(value);
    }

    private static boolean isScalar(@Nullable Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    /**
     * @param frame the state
     * @return the catalogue title of the state
     */
    public String title(Frame frame) {
        return titleOf(locale, frame);
    }

    /**
     * @param locale the interface locale
     * @param frame  the state
     * @return the state's catalogue title, with the state's own facts as its parameters; the
     *         summary where the catalogue text needs a value the state does not report, and the
     *         labelled fallback where the state has no text at all
     */
    public static String titleOf(String locale, Frame frame) {
        var key = prefix(frame.stateId()) + normalise(frame.stateId()) + ".title";
        if (MessageCatalogue.hasKey(locale, key)) {
            var rendered = MessageCatalogue.format(locale, key, frame.facts());
            if (rendered.indexOf('{') < 0) {
                return rendered;
            }
        }
        if (frame.summary() != null && !frame.summary().isBlank() && frame.summary().indexOf('{') < 0) {
            return frame.summary();
        }
        return MessageCatalogue.format(locale, "onboard.state.untitled", Map.of());
    }

    private @Nullable String factLabel(String stateId, String field) {
        var prefix = prefix(stateId);
        var shortId = normalise(stateId);
        var own = prefix + shortId + "." + field;
        if (MessageCatalogue.hasKey(locale, own)) {
            return MessageCatalogue.format(locale, own, Map.of());
        }
        var shared = "onboard.field." + field;
        if (MessageCatalogue.hasKey(locale, shared)) {
            return MessageCatalogue.format(locale, shared, Map.of());
        }
        var consent = "common.consent.field." + field;
        return MessageCatalogue.hasKey(locale, consent)
                            ? MessageCatalogue.format(locale, consent, Map.of())
                            : null;
    }

    /**
     * @param stateId the state
     * @param linkId  the link
     * @return the link's label from the catalogue
     */
    String label(Frame frame, String linkId) {
        var own = prefix(frame.stateId()) + normalise(frame.stateId()) + ".option." + linkId;
        if (MessageCatalogue.hasKey(locale, own)) {
            return MessageCatalogue.format(locale, own, Map.of());
        }
        var shared = "onboard.link." + linkId;
        if (MessageCatalogue.hasKey(locale, shared)) {
            return MessageCatalogue.format(locale, shared, Map.of());
        }
        var consent = "common.consent." + linkId;
        if (MessageCatalogue.hasKey(locale, consent)) {
            return MessageCatalogue.format(locale, consent, Map.of());
        }
        return MessageCatalogue.format(locale, "onboard.link.default", Map.of());
    }

    /**
     * @param frame the state
     * @return the name of the step the state belongs to
     */
    String stepName(Frame frame) {
        var stateId = frame.stateId();
        if (stateId.startsWith(PROJECT_PREFIX)) {
            var key = "project.step." + projectStepOf(stateId);
            return MessageCatalogue.hasKey(locale, key)
                                ? MessageCatalogue.format(locale, key, Map.of())
                                : MessageCatalogue.format(locale, "onboard.step.done", Map.of());
        }
        var key = "onboard.step." + machineStepOf(stateId);
        return MessageCatalogue.hasKey(locale, key)
                            ? MessageCatalogue.format(locale, key, Map.of())
                            : MessageCatalogue.format(locale, "onboard.step.done", Map.of());
    }

    private static String prefix(String stateId) {
        return stateId.startsWith(PROJECT_PREFIX) ? PROJECT_PREFIX : ONBOARD_PREFIX;
    }

    /**
     * @param stateId the state
     * @return the state id as the catalogue spells it: the leading {@code onboard.} or
     *         {@code project.} dropped
     */
    static String normalise(String stateId) {
        if (stateId.startsWith(ONBOARD_PREFIX)) {
            return stateId.substring(ONBOARD_PREFIX.length());
        }
        if (stateId.startsWith(PROJECT_PREFIX)) {
            return stateId.substring(PROJECT_PREFIX.length());
        }
        return stateId;
    }

    /**
     * @param stateId the machine onboarding state
     * @return the catalogue suffix of its step name
     */
    static String machineStepOf(String stateId) {
        if (stateId.startsWith("language.")) {
            return "language";
        }
        if (stateId.startsWith("harness.")) {
            return "harnesses";
        }
        if (stateId.startsWith("catalogue.")) {
            return "models";
        }
        if (stateId.startsWith("roles.")) {
            return "roles";
        }
        if (stateId.startsWith("store.")) {
            return "credentials";
        }
        if (stateId.startsWith("providers.") || stateId.startsWith("github.")
                            || stateId.startsWith("sonar.")) {
            return "providers";
        }
        if (stateId.startsWith("defaults.")) {
            return "plans";
        }
        return "done";
    }

    /**
     * @param stateId the project onboarding state
     * @return the catalogue suffix of its step name
     */
    static String projectStepOf(String stateId) {
        var id = stateId.substring(PROJECT_PREFIX.length());
        return switch (id) {
            case "checkout", "identity", "enrolment", "own", "team", "architecture", "landing",
                                    "roles", "descriptions", "commit" -> id;
            default -> "done";
        };
    }

    /**
     * Reads one state of a runtime representation: the flat body the runtime reports, the operator
     * envelope it carries, or the project onboarding resource.
     *
     * @param body        the JSON body
     * @param totalSteps  the run's number of steps
     * @param project     whether the body is a project's onboarding resource
     * @return the state
     * @throws IOException if the body carries no state id
     */
    public static Frame fromJson(String body, int totalSteps, boolean project) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(body));
        if (map == null) {
            throw new IOException("the runtime reported no state");
        }
        var envelope = JsonTree.asObject(map.get("operator"));
        var source = envelope != null ? envelope : map;
        var stateId = stateIdOf(source, map);
        var stateBlock = JsonTree.asObject(source.get("state"));
        var kind = stateBlock != null ? JsonTree.string(stateBlock, "kind") : null;
        var facts = factsOf(source, stateId);
        var links = linksOf(source);
        var completed = JsonTree.integer(map, "completed_steps");
        int step = completed != null ? Math.min(completed + 1, totalSteps) : 0;
        var reportedStep = JsonTree.integer(source, "step");
        if (reportedStep != null) {
            step = reportedStep;
        }
        if (project && step <= 0) {
            step = 1;
        }
        if ("onboard.mode".equals(stateId) || stateId.startsWith("auto.")) {
            step = 0;
        }
        var factsBlock = JsonTree.asObject(source.get("facts"));
        var summary = JsonTree.string(source, "summary");
        if (summary == null && factsBlock != null) {
            summary = JsonTree.string(factsBlock, "summary");
        }
        return new Frame(stateId, kind, step, totalSteps, summary, facts, links);
    }

    private static String stateIdOf(Map<String, Object> source, Map<String, Object> map)
                        throws IOException {
        var stateBlock = JsonTree.asObject(source.get("state"));
        var stateId = stateBlock != null ? JsonTree.string(stateBlock, "state_id") : null;
        if (stateId == null) {
            stateId = JsonTree.string(source, "state");
        }
        if (stateId == null) {
            stateId = JsonTree.string(map, "state");
        }
        if (stateId == null) {
            throw new IOException("the runtime reported no state id");
        }
        return stateId;
    }

    private static Map<String, Object> factsOf(Map<String, Object> source, String stateId) {
        var facts = new LinkedHashMap<String, Object>();
        var block = JsonTree.asObject(source.get("facts"));
        var from = block != null ? block : source;
        for (var entry : from.entrySet()) {
            if (isStructural(entry.getKey())) {
                continue;
            }
            facts.put(entry.getKey(), entry.getValue());
        }
        return facts;
    }

    private static boolean isStructural(String key) {
        return switch (key) {
            case "state", "scope", "links", "facts", "summary", "runtime", "operator",
                                    "completed_steps", "steps", "step", "total_steps", "auto",
                                    "step_summaries", "harnesses", "models", "roles",
                                    "stores", "providers", "checks", "aspects", "verification",
                                    "usage", "link" -> true;
            default -> false;
        };
    }

    /**
     * @param source the runtime representation
     * @return the links in the runtime's order; the operator envelope's array carries the
     *         preselection, the flat map does not and marks the last link as the primary one
     */
    private static List<Link> linksOf(Map<String, Object> source) {
        var links = new ArrayList<Link>();
        if (source.get("links") instanceof List<?> reported) {
            for (var item : reported) {
                var map = JsonTree.asObject(item);
                if (map == null) {
                    continue;
                }
                var id = JsonTree.string(map, "id");
                if (id != null) {
                    links.add(new Link(id, JsonTree.string(map, "rel"),
                                        Boolean.TRUE.equals(JsonTree.bool(map, "preselected"))));
                }
            }
            return links;
        }
        var flat = JsonTree.object(source, "links");
        if (flat == null) {
            return links;
        }
        for (var entry : flat.entrySet()) {
            var map = JsonTree.asObject(entry.getValue());
            var rel = map != null ? JsonTree.string(map, "rel") : null;
            var preselected = map != null && Boolean.TRUE.equals(JsonTree.bool(map, "preselected"));
            links.add(new Link(entry.getKey(), rel, preselected));
        }
        if (!links.isEmpty() && links.stream().noneMatch(Link::preselected)) {
            var last = links.removeLast();
            links.add(new Link(last.id(), last.rel(), true));
        }
        return links;
    }
}
