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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.operator.TerminalSanitizer;

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
    private static final String NAME = "name";
    private static final String SUMMARY = "summary";
    private static final String VALUE = "value";
    private static final String REASON = "reason";
    private static final String PROVIDER = "provider";
    private static final String ASPECT = "aspect";
    private static final String STATE = "state";
    private static final String FACTS = "facts";
    private static final String LINKS = "links";
    private static final String STEP_KEY_PREFIX = "onboard.step.";
    private static final String STEP_OWN = "own";
    private static final String STEP_ENROLMENT = "enrolment";
    private static final String OPERATOR = "operator";
    private static final String STEP_ROLES = "roles";
    private static final String STEP_DONE = "onboard.step.done";
    private static final String DONE = "done";
    private static final String UNKNOWN = "onboard.state.unknown";
    private static final String LABEL = "label";
    private static final int CONTENT_WIDTH = 76;
    private static final Set<String> BARE_FIELDS = Set.of(
                        NAME, SUMMARY, VALUE, REASON, "model_id", "role", "effort",
                        ASPECT, PROVIDER, "version", "current", "default", "cause", "fix");

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
            facts = Collections.unmodifiableMap(new LinkedHashMap<>(
                                Objects.requireNonNull(facts, FACTS)));
            links = List.copyOf(Objects.requireNonNull(links, LINKS));
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
                deadline = System.nanoTime() + WORKING_TIMEOUT_NANOS;
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
        throw new IllegalStateException("the prompt answered a choice the state does not offer");
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
    public static List<String> wrap(String text, int width) {
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
        if ("onboard.mode".equals(frame.stateId())) {
            return modeContent();
        }
        if ("onboard.completed".equals(frame.stateId())) {
            return List.of(MessageCatalogue.format(locale, "onboard.completed.next", Map.of()));
        }
        var lines = new ArrayList<String>();
        if (!hasTitle(frame) && frame.summary() != null && !frame.summary().isBlank()) {
            lines.add(TerminalSanitizer.sanitizeLine(frame.summary()));
        }
        addFacts(lines, frame);
        return lines;
    }

    private List<String> modeContent() {
        var lines = new ArrayList<String>();
        lines.add(MessageCatalogue.format(locale, "onboard.entry.intro", Map.of()));
        addStepOverview(lines);
        return lines;
    }

    private void addFacts(List<String> lines, Frame frame) {
        for (var entry : frame.facts().entrySet()) {
            var label = factLabel(frame.stateId(), entry.getKey());
            if (label == null) {
                continue;
            }
            if (isScalar(entry.getValue())) {
                addFact(lines, label, entry.getValue());
            } else if (entry.getValue() instanceof List<?> items) {
                addList(lines, label, items);
            }
        }
    }

    private void addFact(List<String> lines, String label, Object raw) {
        var value = rendered(raw);
        if (value != null) {
            lines.add(MessageCatalogue.format(locale, "onboard.state.fact_line",
                                Map.of(LABEL, label, VALUE, TerminalSanitizer.sanitizeLine(value))));
        }
    }

    /**
     * Renders a list a state reports, one item per line: the fields a catalogue label names, and
     * the bare content fields (a name, a value, a model) as they stand. A field with neither is left
     * out, so no id or wire token of the runtime reaches the screen.
     *
     * @param lines the content lines to complete
     * @param label the list's own label
     * @param items the reported items
     */
    private void addList(List<String> lines, String label, List<?> items) {
        lines.add(label);
        for (var item : items) {
            if (item instanceof Map<?, ?> map) {
                addItem(lines, map);
            }
        }
    }

    private void addItem(List<String> lines, Map<?, ?> map) {
        var parts = new ArrayList<String>();
        for (var entry : map.entrySet()) {
            addItemPart(parts, map, String.valueOf(entry.getKey()), entry.getValue());
        }
        if (!parts.isEmpty()) {
            lines.add(MessageCatalogue.format(locale, "onboard.state.item_line",
                                Map.of("item", String.join(" \u00b7 ", parts))));
        }
    }

    private void addItemPart(List<String> parts, Map<?, ?> map, String field, Object raw) {
        var value = rendered(raw);
        if (value == null || (NAME.equals(field) && map.containsKey(PROVIDER))) {
            return;
        }
        var own = itemLabel(field);
        if (own != null) {
            parts.add(own + " " + TerminalSanitizer.sanitizeLine(value));
        } else if (BARE_FIELDS.contains(field)) {
            parts.add(TerminalSanitizer.sanitizeLine(itemValue(field, value)));
        }
    }

    private @Nullable String itemLabel(String field) {
        var key = "onboard.item." + field;
        if (MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return null;
    }

    /**
     * @param field the item's field
     * @param value the rendered value
     * @return the value as its own catalogue name where the field names an aspect or a reason
     */
    private String itemValue(String field, String value) {
        if (ASPECT.equals(field)) {
            return autoText("onboard.auto.aspect.", value);
        }
        if (REASON.equals(field)) {
            return autoText("onboard.auto.reason.", value);
        }
        if (PROVIDER.equals(field)) {
            return autoText("onboard.provider.", value);
        }
        return value;
    }

    private String autoText(String prefix, String token) {
        var key = prefix + token;
        if (MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return MessageCatalogue.format(locale, UNKNOWN, Map.of());
    }

    private void addStepOverview(List<String> lines) {
        for (var aspect : OnboardingAspect.values()) {
            var suffix = aspect.stepNameKey().substring(STEP_KEY_PREFIX.length());
            lines.add(MessageCatalogue.format(locale, "onboard.state.step_entry", Map.of(
                                "number", aspect.step(),
                                "name", MessageCatalogue.format(locale, aspect.stepNameKey(), Map.of()),
                                "what", MessageCatalogue.format(locale,
                                                    "onboard.entry.step." + suffix, Map.of()))));
        }
    }

    /**
     * @param value a reported value
     * @return the value as the screen may show it, or {@code null} where the value is a wire token
     *         the catalogue does not name; such a value is left out rather than printed raw
     */
    static @Nullable String rendered(String locale, Object value) {
        if (value instanceof Boolean flag) {
            return MessageCatalogue.format(locale, "onboard.state.boolean." + flag, Map.of());
        }
        var text = String.valueOf(value);
        var own = "onboard.value." + text;
        if (MessageCatalogue.hasKey(locale, own)) {
            return MessageCatalogue.format(locale, own, Map.of());
        }
        if (!isRawToken(text)) {
            return text;
        }
        var unit = "unit." + text;
        if (MessageCatalogue.hasKey(locale, unit)) {
            return MessageCatalogue.format(locale, unit, Map.of());
        }
        return null;
    }

    private @Nullable String rendered(Object value) {
        return rendered(locale, value);
    }

    /**
     * @param value a reported value
     * @return whether the value is a raw wire token (a dotted enum such as {@code landing.pull-request})
     *         that only the catalogue may render; such a value is left out rather than printed raw
     */
    private static boolean isRawToken(String value) {
        var dot = value.indexOf('.');
        if (dot <= 0 || dot == value.length() - 1) {
            return false;
        }
        return isTokenPart(value.substring(0, dot))
                            && isTokenTail(value.substring(dot + 1));
    }

    private static boolean isTokenTail(String tail) {
        var start = 0;
        for (var i = 0; i < tail.length(); i++) {
            var c = tail.charAt(i);
            if (c == '.') {
                if (!isTokenPart(tail.substring(start, i))) {
                    return false;
                }
                start = i + 1;
            }
        }
        return isTokenPart(tail.substring(start));
    }

    private static boolean isTokenPart(String part) {
        if (part.isEmpty() || part.charAt(0) < 'a' || part.charAt(0) > 'z') {
            return false;
        }
        for (var i = 1; i < part.length(); i++) {
            var c = part.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-')) {
                return false;
            }
        }
        return true;
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
                return TerminalSanitizer.sanitizeLine(rendered);
            }
        }
        if (frame.summary() != null && !frame.summary().isBlank() && frame.summary().indexOf('{') < 0) {
            return TerminalSanitizer.sanitizeLine(frame.summary());
        }
        return MessageCatalogue.format(locale, "onboard.state.untitled", Map.of());
    }

    /**
     * @param frame the state
     * @return whether the catalogue names a title for the state
     */
    private boolean hasTitle(Frame frame) {
        return MessageCatalogue.hasKey(locale,
                            prefix(frame.stateId()) + normalise(frame.stateId()) + ".title");
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
        var mode = "onboard.mode." + linkId;
        if (MessageCatalogue.hasKey(locale, mode)) {
            return MessageCatalogue.format(locale, mode, Map.of());
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
            return stepText("project.step." + projectStepOf(stateId));
        }
        return stepText(STEP_KEY_PREFIX + machineStepOf(stateId));
    }

    private String stepText(String key) {
        if (MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return MessageCatalogue.format(locale, STEP_DONE, Map.of());
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
        for (var aspect : OnboardingAspect.values()) {
            if (stateId.startsWith(aspect.wireValue() + ".")) {
                return aspect.stepNameKey().substring(STEP_KEY_PREFIX.length());
            }
        }
        if (stateId.startsWith("github.") || stateId.startsWith("sonar.")) {
            return "providers";
        }
        return DONE;
    }

    /**
     * @param stateId the project onboarding state
     * @return the catalogue suffix of its step name
     */
    static String projectStepOf(String stateId) {
        var id = stateId.substring(PROJECT_PREFIX.length());
        return switch (id) {
            case "consent", "detect", "run-consent", "probe", "checkout" -> "checkout";
            case "enrol", STEP_ENROLMENT -> STEP_ENROLMENT;
            case OPERATOR, STEP_OWN -> STEP_OWN;
            case "identity", "team", "architecture", "landing",
                STEP_ROLES, "descriptions", "commit" -> id;
            default -> DONE;
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
        var envelope = JsonTree.asObject(map.get(OPERATOR));
        var source = envelope != null ? envelope : map;
        var stateId = stateIdOf(source, map);
        return new Frame(stateId, kindOf(source), stepOf(source, map, stateId, totalSteps, project),
                            totalSteps, summaryOf(source), factsOf(source), linksOf(source));
    }

    private static @Nullable String kindOf(Map<String, Object> source) {
        var stateBlock = JsonTree.asObject(source.get(STATE));
        return stateBlock != null ? JsonTree.string(stateBlock, "kind") : null;
    }

    private static int stepOf(Map<String, Object> source, Map<String, Object> map, String stateId,
                        int totalSteps, boolean project) {
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
        return step;
    }

    private static @Nullable String summaryOf(Map<String, Object> source) {
        var summary = JsonTree.string(source, SUMMARY);
        if (summary == null) {
            var factsBlock = JsonTree.asObject(source.get(FACTS));
            summary = factsBlock != null ? JsonTree.string(factsBlock, SUMMARY) : null;
        }
        return summary;
    }

    private static String stateIdOf(Map<String, Object> source, Map<String, Object> map)
                        throws IOException {
        var stateBlock = JsonTree.asObject(source.get(STATE));
        var stateId = stateBlock != null ? JsonTree.string(stateBlock, "state_id") : null;
        if (stateId == null) {
            stateId = JsonTree.string(source, STATE);
        }
        if (stateId == null) {
            stateId = JsonTree.string(map, STATE);
        }
        if (stateId == null) {
            throw new IOException("the runtime reported no state id");
        }
        return stateId;
    }

    private static Map<String, Object> factsOf(Map<String, Object> source) {
        var facts = new LinkedHashMap<String, Object>();
        var block = JsonTree.asObject(source.get(FACTS));
        var from = block != null ? block : source;
        for (var entry : from.entrySet()) {
            if (!isStructural(entry.getKey())) {
                facts.put(entry.getKey(), entry.getValue());
            }
        }
        return facts;
    }

    private static boolean isStructural(String key) {
        return switch (key) {
            case STATE, "scope", LINKS, FACTS, SUMMARY, "runtime", OPERATOR,
                "completed_steps", "steps", "step", "total_steps", "auto",
                "step_summaries", "verification", "usage", "link" -> true;
            default -> false;
        };
    }

    /**
     * @param source the runtime representation
     * @return the links in the runtime's order; the operator envelope's array carries the
     *         preselection, the flat map does not and marks the last link as the primary one
     */
    private static List<Link> linksOf(Map<String, Object> source) {
        if (source.get(LINKS) instanceof List<?> reported) {
            return reportedLinks(reported);
        }
        return flatLinks(JsonTree.object(source, LINKS));
    }

    private static List<Link> reportedLinks(List<?> reported) {
        var links = new ArrayList<Link>();
        for (var item : reported) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                var id = JsonTree.string(map, "id");
                if (id != null) {
                    links.add(new Link(id, JsonTree.string(map, "rel"),
                                        Boolean.TRUE.equals(JsonTree.bool(map, "preselected"))));
                }
            }
        }
        return links;
    }

    private static List<Link> flatLinks(@Nullable Map<String, Object> flat) {
        var links = new ArrayList<Link>();
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
