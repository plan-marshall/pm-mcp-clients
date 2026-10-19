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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.AdoptionFailure;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.RecipeParameter;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.ScreeningSpan;
import de.planmarshall.api.start.SourceFailure;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartReadyResource;
import de.planmarshall.api.start.StartRefusal;
import de.planmarshall.api.start.StartRefusalReason;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;

/**
 * Renders the starting dialogue of the terminal client (design <em>Terminal Client: Starting
 * Work</em>). Every word comes from the message catalogue; every value that came from outside the
 * machine passes through {@link TerminalSanitizer} before it reaches the terminal.
 */
final class StartRenderer {

    private static final DateTimeFormatter RESET_FORMAT =
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());
    private static final String GLYPH_STORED = "\u2713";
    private static final String GLYPH_WORKING = "\u25b6";
    private static final String GLYPH_ATTENTION = "!";
    private static final String GLYPH_FAILED = "\u2717";
    private static final String SEPARATOR = "   ";
    private static final String KEY_CREDENTIAL = "credential";
    private static final String KEY_FORGE = "forge";
    private static final String KEY_TIME = "time";
    private static final String KEY_SIZE = "size";
    private static final String KEY_CAUSE = "cause";
    private static final String KEY_ORIGIN = "origin";
    private static final String KEY_VALUE = "value";
    private static final String KEY_INDEX = "index";
    private static final String KEY_REASON = "reason";
    private static final String KEY_PROJECT = "project";
    private static final String MIDDLE_DOT = " \u00b7 ";

    private final StartPrompt prompt;

    /**
     * @param prompt the dialogue's input and output surface
     */
    StartRenderer(StartPrompt prompt) {
        this.prompt = prompt;
    }

    /**
     * Renders the list of sources and the kinds that still apply.
     *
     * @param state   the entry's state
     * @param step    the step number
     * @param total   the number of steps
     * @param project the project
     * @param fromCwd whether the project came from the working directory
     */
    void sources(StartStateResource state, int step, int total, String project, boolean fromCwd) {
        var entry = requireEntry(state);
        prompt.raw(step(step, total) + " " + text("start.step.sources", Map.of()) + MIDDLE_DOT
                            + text("start.sources.title", Map.of("kind", entry.kind().wireValue()))
                            + SEPARATOR + text(fromCwd ? "start.sources.project" : "start.sources.project_only",
                            Map.of(KEY_PROJECT, sanitize(project))));
        for (var source : entry.sources()) {
            sourceLine(source);
        }
        if (entry.sources().isEmpty()) {
            prompt.raw("");
        }
        renderOffers(entry);
        var keys = new StringJoiner(SEPARATOR);
        keys.add(text("start.sources.continue", Map.of()));
        if (entry.chatAvailable()) {
            keys.add(text("start.sources.chat", Map.of()));
        }
        if (!entry.sources().isEmpty()) {
            keys.add(text("start.sources.remove_one", Map.of()));
        }
        keys.add(text("start.sources.help", Map.of()));
        prompt.raw("  " + keys);
    }

    /**
     * Renders the ready step.
     *
     * @param state the entry's state
     * @param step  the step number
     * @param total the number of steps
     */
    void ready(StartStateResource state, int step, int total) {
        var entry = requireEntry(state);
        var ready = state.ready();
        if (ready == null) {
            return;
        }
        prompt.raw(step(step, total) + " " + text("start.step.ready", Map.of()) + " \u00b7 "
                            + text("start.ready.heading", Map.of("kind", entry.kind().wireValue())));
        for (var source : entry.sources()) {
            var label = kindLabel(source.kind());
            var value = source.title() != null ? source.title() : source.reference();
            prompt.raw("  " + text("start.ready.row", Map.of("label", label, KEY_VALUE, sanitize(value))));
        }
        renderReadyValues(ready);
        renderEpicValues(ready);
        prompt.raw("  " + text("start.ready.prompt", Map.of("kind", entry.kind().wireValue())));
    }

    private void renderReadyValues(StartReadyResource ready) {
        if (!ready.parameters().isEmpty()) {
            var params = new StringJoiner(" \u00b7 ");
            for (var parameter : ready.parameters()) {
                params.add(text("start.ready.param_value", Map.of("name", tenant(parameter.name()),
                                    KEY_VALUE, tenant(parameter.value()))));
            }
            prompt.raw("  " + text("start.ready.recipe_parameters", Map.of("params", params.toString())));
        }
        if (ready.title() != null) {
            prompt.raw("  " + text("start.ready.title_row", Map.of("title", sanitize(ready.title()),
                                KEY_ORIGIN, text("start.ready.title_origin", Map.of()))));
        }
        if (ready.planId() != null) {
            prompt.raw("  " + text("start.ready.id_row", Map.of("id", sanitize(ready.planId()),
                                KEY_ORIGIN, idOrigin(ready))));
        }
    }

    private String idOrigin(StartReadyResource ready) {
        if (ready.recipe() != null) {
            return text("start.ready.id_origin_recipe", Map.of());
        }
        return text(ready.idFromTitle() ? "start.ready.id_origin_title" : "start.ready.id_origin_free", Map.of());
    }

    private void renderEpicValues(StartReadyResource ready) {
        if (ready.epicAutoStart() == null) {
            return;
        }
        var mode = Boolean.TRUE.equals(ready.epicAutoStart()) ? "on" : "off";
        var origin = ready.epicAutoStartOrigin() == ConfigOrigin.PROJECT ? KEY_PROJECT : "machine";
        prompt.raw("  " + text("start.ready.epic_auto", Map.of("mode", mode, KEY_ORIGIN, origin)));
    }

    /**
     * Renders the recipes of a project.
     *
     * @param recipes the recipes
     */
    void recipes(List<RecipeRef> recipes) {
        prompt.raw(text("start.recipe.choose.title", Map.of()));
        var index = 1;
        for (var recipe : recipes) {
            prompt.raw("  " + text("start.recipe.option", Map.of(
                                KEY_INDEX, index,
                                "key", sanitize(recipe.key()),
                                "description", sanitize(recipe.description()),
                                KEY_ORIGIN, origin(recipe))));
            index++;
        }
    }

    private String origin(RecipeRef recipe) {
        return switch (recipe.origin()) {
            case "domain" -> text("start.recipe.origin_domain", Map.of(KEY_ORIGIN, sanitize(recipe.origin())));
            case KEY_PROJECT -> text("start.recipe.origin_project", Map.of());
            default -> text("start.recipe.origin_machine", Map.of());
        };
    }

    /**
     * Renders a recipe's parameters.
     *
     * @param recipe     the recipe
     * @param parameters the resolved parameters
     */
    void parameters(RecipeRef recipe, List<RecipeParameter> parameters) {
        prompt.raw(text("start.recipe.parameters.title", Map.of("recipe", sanitize(recipe.key()))));
        for (int index = 1; index <= parameters.size(); index++) {
            renderParameter(index, parameters.get(index - 1));
        }
        prompt.raw("  " + text("start.recipe.parameters_prompt", Map.of("range", "1-" + parameters.size())));
    }

    private void renderParameter(int index, RecipeParameter parameter) {
        if (!parameter.available()) {
            prompt.raw("  " + text("start.recipe.param_unavailable", Map.of(
                                KEY_INDEX, index,
                                "name", sanitize(parameter.name()),
                                KEY_REASON, sanitize(parameter.reason() != null ? parameter.reason()
                                : text("start.recipe.no_module", Map.of())))));
            return;
        }
        var options = parameter.proposed() ? text("start.recipe.param_proposed", Map.of()) : "";
        var lineKey = parameter.proposed() ? "start.recipe.param" : "start.recipe.param_plain";
        prompt.raw("  " + text(lineKey, Map.of(
                            KEY_INDEX, index,
                            "name", sanitize(parameter.name()),
                            KEY_VALUE, sanitize(parameter.value() != null ? parameter.value() : ""),
                            "options", options)));
        if (!parameter.options().isEmpty()) {
            prompt.raw("  " + text("start.recipe.param_options",
                                Map.of("options", String.join(MIDDLE_DOT,
                                                    parameter.options().stream().map(StartRenderer::sanitize).toList()))));
        }
    }

    /**
     * Renders the adoptable epics of an epic's start.
     *
     * @param entry the entry
     */
    void adoption(EntryResource entry) {
        prompt.raw(step(1, 3) + " " + text("start.sources.title", Map.of("kind", entry.kind().wireValue())));
        var index = 1;
        for (var epic : entry.adoptableEpics()) {
            prompt.raw("  " + text("start.adopt.line", Map.of("epic", sanitize(epic.epicId()),
                                KEY_PROJECT, sanitize(entry.projectId()))));
            prompt.raw("  " + text("start.adopt.option", Map.of("key", index, "epic", sanitize(epic.epicId()))));
            index++;
        }
        prompt.raw("  " + text("start.adopt.new", Map.of()));
        prompt.raw("  " + text("start.sources.help", Map.of()));
    }

    /**
     * Renders why an epic cannot be adopted.
     *
     * @param failure the failure
     */
    void adoptionFailure(AdoptionFailure failure) {
        var reason = text("start.epic.adopt.failed", Map.of(
                            "epic", sanitize(failure.epicId()),
                            KEY_REASON, failure.reason().wireValue(),
                            "file", sanitize(failure.file() != null ? failure.file() : ""),
                            "detail", sanitize(failure.detail() != null ? failure.detail() : "")));
        prompt.raw(reason);
        prompt.raw("  " + text("start.adopt.failed.retry", Map.of()) + SEPARATOR
                            + text("start.adopt.failed.back", Map.of()) + SEPARATOR
                            + text("start.adopt.failed.abandon", Map.of()));
    }

    /**
     * Renders a refusal of the setup gate.
     *
     * @param refusal the refusal
     */
    void refused(StartRefusal refusal) {
        var message = refusal.reason() == StartRefusalReason.MACHINE_NOT_ONBOARDED
                            ? text("start.gate.machine", Map.of("need", tenant(refusal.need())))
                            : text("start.gate.project", Map.of(KEY_PROJECT, tenant(refusal.projectId()),
                            "need", tenant(refusal.need())));
        prompt.raw(text("start.refused.message", Map.of("message", message)));
        prompt.raw(text("start.refused.action", Map.of("action", text(refusal.actionKey(), Map.of()))));
    }

    /**
     * Renders one source line and its cause, spans, or changed files.
     *
     * @param source the source
     */
    void sourceLine(SourceResource source) {
        var label = kindLabel(source.kind());
        var line = source.title() != null
                            ? text("start.source.line_title", Map.of("glyph", glyph(source.state()),
                            "kind", label, "title", sanitize(source.title())))
                            : text("start.source.line", Map.of("glyph", glyph(source.state()),
                            "kind", label, "reference", sanitize(source.reference())));
        line += text("start.source.suffix", Map.of("state", stateLabel(source.state())));
        if (source.trust() == SourceTrust.UNTRUSTED) {
            line += SEPARATOR + text("start.state.untrusted", Map.of());
        }
        if (source.dispositionReason() != null) {
            line += SEPARATOR + sanitize(source.dispositionReason());
        }
        prompt.raw(line);
        if (!source.changedFiles().isEmpty()) {
            prompt.raw(text("start.source.changed", Map.of("count", source.changedFiles().size())));
        }
        if (source.state() == SourceState.FLAGGED) {
            prompt.raw("  " + text("start.source.flagged_title", Map.of()));
            for (var span : source.spans()) {
                prompt.raw(text("start.source.span", Map.of("text", spanText(span),
                                    KEY_REASON, sanitize(span.reason()))));
            }
        }
        if (source.failure() != null) {
            prompt.raw(text("start.source.failure", Map.of(KEY_CAUSE, cause(Objects.requireNonNull(source.failure(), "failure")))));
        }
    }

    private static String spanText(ScreeningSpan span) {
        return TerminalSanitizer.sanitizeLine(span.text() != null ? span.text() : "");
    }

    /**
     * @param failure the failure
     * @return the catalogue text of its cause
     */
    String cause(SourceFailure failure) {
        var args = new LinkedHashMap<String, Object>();
        if (failure.detail() != null) {
            args.put(KEY_FORGE, sanitize(failure.detail()));
            args.put(KEY_SIZE, sanitize(failure.detail()));
            args.put(KEY_CAUSE, sanitize(failure.detail()));
        }
        if (failure.resetsAt() != null) {
            args.put(KEY_TIME, RESET_FORMAT.format(failure.resetsAt()));
            args.put(KEY_CREDENTIAL, "some");
            return text("start.cause." + failure.cause().wireValue(), args)
                                + " (" + text("start.cause.rate_limited_epoch",
                                Map.of("epoch", failure.resetsAtEpochSecond())) + ")";
        }
        return text("start.cause." + failure.cause().wireValue(), args);
    }

    /**
     * Renders the options of every source the dialogue can offer next, and the reason of each kind
     * that does not combine with the list.
     *
     * @param entry the entry
     */
    void renderOffers(EntryResource entry) {
        var optionLine = new StringBuilder("  ");
        var first = true;
        for (var offer : entry.kindOffers()) {
            if (!offer.available()) {
                continue;
            }
            if (!first) {
                optionLine.append(SEPARATOR);
            }
            optionLine.append(text("start.source.option", Map.of("key", keyOf(offer.kind()),
                                "source", kindLabel(offer.kind()))));
            first = false;
        }
        if (!first) {
            prompt.raw(optionLine.toString());
        }
        var seenReasons = new LinkedHashSet<String>();
        for (var offer : entry.kindOffers()) {
            var reasonKey = offer.reasonKey();
            if (!offer.available() && reasonKey != null) {
                var reason = text(reasonKey, offer.reasonArgs());
                if (seenReasons.add(reason)) {
                    prompt.raw("  " + reason);
                }
            }
        }
    }

    /**
     * @param kind the source kind
     * @return the key of its option in the list
     */
    static String keyOf(SourceKind kind) {
        return switch (kind) {
            case DESCRIPTION -> "d";
            case DOCUMENTS -> "f";
            case ISSUE -> "i";
            case LESSON -> "l";
            case LOCAL_CHANGES -> "c";
            case PULL_REQUEST -> "p";
        };
    }

    /**
     * @param kind the source kind
     * @return its label
     */
    String kindLabel(SourceKind kind) {
        return text("start.source." + kind.wireValue(), Map.of());
    }

    private String stateLabel(SourceState state) {
        return text("start.state." + state.wireValue(), Map.of());
    }

    private static String glyph(SourceState state) {
        return switch (state) {
            case STORED, COPIED, MOVED -> GLYPH_STORED;
            case CHECKING, FETCHING, SCREENING -> GLYPH_WORKING;
            case FLAGGED, UNSCREENED -> GLYPH_ATTENTION;
            case FAILED -> GLYPH_FAILED;
        };
    }

    private String step(int step, int total) {
        return text("start.step.counter", Map.of("step", step, "total", total));
    }

    private String text(String key, Map<String, ?> args) {
        return MessageCatalogue.format(prompt.locale(), Objects.requireNonNull(key, "key"),
                            Objects.requireNonNull(args, "args"));
    }

    private static String sanitize(@Nullable String value) {
        return TerminalSanitizer.sanitizeLine(value != null ? value : "");
    }

    private static String tenant(@Nullable String value) {
        return sanitize(value);
    }

    private static EntryResource requireEntry(StartStateResource state) {
        var entry = state.entry();
        if (entry == null) {
            throw new IllegalStateException("the state " + state.state().wireValue() + " carries no entry");
        }
        return entry;
    }

    /**
     * @param state the state
     * @return {@code true} if the state is a source-gathering state
     */
    static boolean gathersSources(StartState state) {
        return state == StartState.SOURCES || state == StartState.CHOOSE || state == StartState.EPIC_ADOPT;
    }
}
