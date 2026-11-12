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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.RecipeParameter;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.operator.dialogue.ChoicePrompt;

/**
 * The terminal client's starting dialogue (design <em>Terminal Client: Starting Work</em>): the
 * direct verbs {@code plan add}, {@code epic add}, and {@code recipe run} drive it. Every state the
 * server represents is rendered here; every answer the verb carries is applied before the operator
 * is asked, and a state without an answer stops a {@code --no-input} run with the state it needs.
 */
final class StartDialogue {

    /** The exit code of a verb stopped by {@code --no-input} at a state that needs the operator. */
    static final int EXIT_MISSING_INPUT = 3;
    private static final int EXIT_FAILURE = 1;
    private static final int EXIT_OK = 0;

    private static final String KEY_KIND = "kind";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_REASON = "reason";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";
    private static final String KEY_REFERENCE = "reference";
    private static final String KEY_PATH = "path";
    private static final String KEY_PARAMETERS = "parameters";
    private static final String KEY_PLAN_ID = "plan_id";
    private static final String KEY_INDEX = "index";
    private static final String KEY_ORIGIN = "origin";
    private static final String KEY_STATE = "state";
    private static final String KEY_STATE_SOURCES = "start.sources";
    private static final String KEY_STATE_READY = "start.ready";
    private static final String KEY_STATE_RECIPE_PARAMETERS = "recipe.parameters";
    private static final String KEY_STATE_ADOPT = "start-epic.adopt";
    private static final String KEY_STATE_ADOPT_FAILED = "start-epic.adopt-failed";
    private static final String KEY_DOCUMENTS = "start.source.documents";
    private static final String KEY_ADOPT_PROMPT = "start.adopt.prompt";

    private final EntryKind kind;
    private final Map<String, String> environment;
    private final StartAnswers answers;
    private final StartApi api;
    private final StartPrompt prompt;
    private final StartRenderer renderer;
    private final Deque<SourceIntent> pending = new ArrayDeque<>();
    private final Map<String, String> parameterValues = new LinkedHashMap<>();

    private String project = "";
    private boolean fromWorkingDirectory;
    private boolean recipeMode;
    private boolean recipeSelected;
    private boolean adopted;
    private @Nullable String titleOverride;
    private @Nullable String idOverride;
    private @Nullable StartStateResource state;

    private StartDialogue(OperatorContext context, PrintWriter out, EntryKind kind, StartAnswers answers) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.environment = Map.copyOf(context.environment());
        this.answers = Objects.requireNonNull(answers, "answers");
        this.api = new StartApi(Objects.requireNonNull(context, "context"));
        this.prompt = new StartPrompt(context, Objects.requireNonNull(out, "out"),
                            context.clientEnvironment().locale(), answers.noInput);
        this.renderer = new StartRenderer(prompt);
    }

    /**
     * Runs one starting verb.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param kind    the kind of work
     * @param answers the answers the verb carries
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, EntryKind kind, StartAnswers answers)
                        throws IOException {
        var dialogue = new StartDialogue(context, out, kind, answers);
        if (answers.copy && answers.move) {
            dialogue.prompt.line("start.documents.mode.conflict", Map.of());
            return EXIT_FAILURE;
        }
        try {
            return dialogue.execute();
        } catch (StartPrompt.MissingInputException missing) {
            dialogue.printMissing(missing);
            return EXIT_MISSING_INPUT;
        } catch (StartPrompt.InterruptException _) {
            return StartPrompt.EXIT_INTERRUPTED;
        } catch (StartPrompt.StartLeftException _) {
            dialogue.prompt.line("start.entry.left", Map.of());
            return EXIT_OK;
        } catch (StartApi.StartActException refused) {
            dialogue.prompt.raw(OutputSafety.stripControlSequences(refused.getMessage()));
            return EXIT_FAILURE;
        }
    }

    private int execute() throws IOException {
        project = resolveProject();
        prepareAnswers();
        state = answers.entryId != null ? api.fetch(answers.entryId) : openEntry();
        if (kind == EntryKind.PLAN) {
            recipeMode = answers.recipeCommand || (answers.recipeKey != null && !answers.recipeKey.isBlank());
            if (!recipeMode && state.state() == StartState.RECIPE) {
                state = act("sources", Map.of());
            }
        }
        return loop();
    }

    private StartStateResource openEntry() throws IOException {
        var request = new EntryOpenRequest(kind, answers.description, answers.title,
                            kind.epic() ? null : answers.recipeKey, null);
        return api.open(project, request);
    }

    private int loop() throws IOException {
        while (true) {
            var current = Objects.requireNonNull(state, KEY_STATE);
            switch (current.state()) {
                case CHOOSE, SOURCES -> sources();
                case READY -> {
                    if (adopted) {
                        prompt.line("start.started.follow", Map.of());
                        return EXIT_OK;
                    }
                    if (ready()) {
                        return EXIT_OK;
                    }
                }
                case RECIPE, RECIPE_CHOOSE -> recipeChoice();
                case RECIPE_PARAMETERS -> recipeParameters();
                case RECIPE_CREATE, RECIPE_STEP_CREATE -> {
                    create();
                    return EXIT_OK;
                }
                case EPIC_ADOPT -> adoption();
                case EPIC_ADOPT_FAILED -> adoptionFailed();
                case REFUSED -> {
                    renderer.refused(Objects.requireNonNull(current.refusal(), "refusal"));
                    return EXIT_FAILURE;
                }
                default -> throw new IllegalStateException("unknown start state: " + current.state());
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Sources

    private void sources() throws IOException {
        var entry = entry();
        var documentsChecking = entry.sources().stream()
                            .filter(source -> source.kind() == SourceKind.DOCUMENTS
                                                && source.state() == SourceState.CHECKING)
                            .findFirst().orElse(null);
        if (documentsChecking != null) {
            documentsMode();
            return;
        }
        var pendingSource = entry.sources().stream()
                            .filter(source -> source.state() == SourceState.FAILED
                                                || ((source.state() == SourceState.FLAGGED
                                                || source.state() == SourceState.UNSCREENED)
                                                && source.disposition() == null))
                            .findFirst().orElse(null);
        if (pendingSource != null) {
            resolvePending(pendingSource);
            return;
        }
        if (!pending.isEmpty()) {
            addSource(pending.poll());
            return;
        }
        if (recipeMode && !recipeSelected) {
            state = act("recipe", Map.of());
            return;
        }
        if (!prompt.asks()) {
            renderer.sources(state, 1, totalSteps(), project, fromWorkingDirectory);
            if (entry.canContinue()) {
                continueToReady();
                return;
            }
            StartPrompt.missing(KEY_STATE_SOURCES, text("start.need.source", Map.of()),
                                entry.entryId());
            return;
        }
        renderer.sources(state, 1, totalSteps(), project, fromWorkingDirectory);
        var answer = prompt.askRaw(text("start.sources.key_prompt", Map.of()), KEY_STATE_SOURCES,
                            text("start.sources.key_prompt", Map.of()), entry.entryId());
        handleSourceKey(answer);
    }

    private void handleSourceKey(String answer) throws IOException {
        var entry = entry();
        if (answer.isEmpty()) {
            if (entry.canContinue()) {
                continueToReady();
            }
            return;
        }
        if (leaves(answer)) {
            throw new StartPrompt.StartLeftException();
        }
        if ("?".equals(answer)) {
            prompt.line("start.help.sources", Map.of());
            return;
        }
        var key = answer.toLowerCase(Locale.ROOT);
        var sourceKind = kindOfKey(key);
        if (sourceKind != null && offered(sourceKind)) {
            switch (sourceKind) {
                case DESCRIPTION -> addDescription();
                case DOCUMENTS -> addDocument();
                case ISSUE -> addExternal(SourceKind.ISSUE, "start.issue.field");
                case LESSON -> addExternal(SourceKind.LESSON, "start.source.lesson");
                case LOCAL_CHANGES -> addSource(new SourceIntent(SourceKind.LOCAL_CHANGES, null));
                case PULL_REQUEST -> addExternal(SourceKind.PULL_REQUEST, "start.pr.field");
            }
            return;
        }
        if ("h".equals(key)) {
            openChat();
            return;
        }
        if ("x".equals(key) && !entry.sources().isEmpty()) {
            removeSource();
            return;
        }
        // A kind the list does not offer, x without a source, and any other input change nothing.
        notOffered(answer, sourceKeys(entry));
    }

    private static @Nullable SourceKind kindOfKey(String key) {
        for (var sourceKind : SourceKind.values()) {
            if (StartRenderer.keyOf(sourceKind).equals(key)) {
                return sourceKind;
            }
        }
        return null;
    }

    private String sourceKeys(EntryResource entry) {
        var keys = new ArrayList<String>();
        entry.kindOffers().stream().filter(SourceKindOffer::available)
                            .forEach(offer -> keys.add(StartRenderer.keyOf(offer.kind())));
        if (entry.chatAvailable()) {
            keys.add("h");
        }
        if (!entry.sources().isEmpty()) {
            keys.add("x");
        }
        keys.add(leaveKey());
        keys.add("?");
        return String.join(", ", keys);
    }

    private void addDescription() throws IOException {
        prompt.raw(text("start.source.description", Map.of()) + " " + text("start.description.ends", Map.of()));
        var text = new StringBuilder();
        while (true) {
            var line = prompt.askRaw("", KEY_STATE_SOURCES, text("start.source.description", Map.of()),
                                entry().entryId());
            if (line.isEmpty()) {
                break;
            }
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(line);
        }
        addSource(new SourceIntent(SourceKind.DESCRIPTION, text.toString()));
    }

    private void addDocument() throws IOException {
        var path = prompt.askRaw(text(KEY_DOCUMENTS, Map.of()), KEY_STATE_SOURCES,
                            text(KEY_DOCUMENTS, Map.of()), entry().entryId());
        if (!path.isEmpty()) {
            addSource(new SourceIntent(SourceKind.DOCUMENTS, path));
        }
    }

    private void addExternal(SourceKind sourceKind, String questionKey) throws IOException {
        var value = prompt.askRaw(text(questionKey, Map.of()), KEY_STATE_SOURCES,
                            text(questionKey, Map.of()), entry().entryId());
        if (!value.isEmpty()) {
            addSource(new SourceIntent(sourceKind, value));
        }
    }

    private void documentsMode() throws IOException {
        var mode = answers.documentsMode();
        if (mode == null) {
            if (!prompt.asks()) {
                mode = "copy";
            } else {
                mode = askDocumentsMode();
            }
        }
        state = act("documents_mode:" + mode, Map.of());
    }

    private String askDocumentsMode() {
        while (true) {
            var answer = prompt.askRaw(text("start.documents.mode.prompt", Map.of()), KEY_STATE_SOURCES,
                                text(KEY_DOCUMENTS, Map.of()), entry().entryId());
            // Enter takes the preselected copy; 1, 2 and a are the keys the prompt shows.
            switch (answer.toLowerCase(Locale.ROOT)) {
                case "", "1" -> {
                    return "copy";
                }
                case "2" -> {
                    return "move";
                }
                case "a" -> {
                    return "copy_always";
                }
                default -> {
                    if (leaves(answer)) {
                        throw new StartPrompt.StartLeftException();
                    }
                    notOffered(answer, "1-2, a, " + leaveKey());
                }
            }
        }
    }

    private void resolvePending(SourceResource source) throws IOException {
        renderer.sourceLine(source);
        var keys = pendingKeys(source);
        var options = pendingOptions(keys);
        if (!prompt.asks()) {
            StartPrompt.missing(KEY_STATE_SOURCES, options, entry().entryId());
            return;
        }
        var answer = prompt.askRaw(options, KEY_STATE_SOURCES, options, entry().entryId());
        var key = answer.toLowerCase(Locale.ROOT);
        if (!keys.contains(key)) {
            if (leaves(answer)) {
                throw new StartPrompt.StartLeftException();
            }
            // Only the keys this source's state shows are offered; the source is shown again.
            unoffered(answer, String.join(", ", keys) + ", " + leaveKey());
            return;
        }
        switch (key) {
            case "t" -> state = act("retry:" + source.sourceId(), Map.of());
            case "r" -> state = act("remove:" + source.sourceId(), Map.of());
            default -> {
                var reason = prompt.askReason(KEY_STATE_SOURCES, text("start.source.reason", Map.of()),
                                    entry().entryId());
                state = act("accept:" + source.sourceId(), Map.of(KEY_REASON, reason));
            }
        }
    }

    /**
     * The keys a source that waits for the operator offers: a failed one tries again when its cause
     * allows it, a flagged one is used anyway ({@code u}), an unscreened one accepted as untrusted
     * ({@code a}), and every one can be removed.
     */
    private static List<String> pendingKeys(SourceResource source) {
        var keys = new ArrayList<String>();
        var retryable = source.failure() == null || source.failure().retryable();
        if (source.state() == SourceState.FAILED) {
            if (retryable) {
                keys.add("t");
            }
        } else if (source.state() == SourceState.FLAGGED) {
            keys.add("u");
        } else {
            if (retryable) {
                keys.add("t");
            }
            keys.add("a");
        }
        keys.add("r");
        return keys;
    }

    private String pendingOptions(List<String> keys) {
        var options = new ArrayList<String>();
        for (var key : keys) {
            options.add(text(switch (key) {
                case "t" -> "start.source.try_again";
                case "u" -> "start.source.use_anyway";
                case "a" -> "start.source.accept_untrusted";
                default -> "start.source.remove_option";
            }, Map.of()));
        }
        return String.join("   ", options);
    }

    private void removeSource() throws IOException {
        var sources = entry().sources();
        while (true) {
            var answer = prompt.askRaw(text("start.sources.remove_which",
                                Map.of("count", sources.size())), KEY_STATE_SOURCES,
                                text("start.sources.remove_one", Map.of()), entry().entryId());
            if (answer.isEmpty()) {
                // Enter names no source: the list is shown again with nothing removed.
                return;
            }
            if (leaves(answer)) {
                throw new StartPrompt.StartLeftException();
            }
            var index = indexIn(answer, sources.size());
            if (index > 0) {
                state = act("remove:" + sources.get(index - 1).sourceId(), Map.of());
                return;
            }
            notOffered(answer, range(sources.size()) + ", " + leaveKey());
        }
    }

    private void addSource(SourceIntent intent) throws IOException {
        var offer = offer(intent.kind());
        if (offer == null || !offer.available()) {
            var reasonKey = offer != null ? offer.reasonKey() : null;
            if (reasonKey != null) {
                prompt.raw(text(reasonKey, offer.reasonArgs()));
            }
            return;
        }
        var body = new LinkedHashMap<String, Object>();
        var link = "add_" + intent.kind().wireValue();
        switch (intent.kind()) {
            case DESCRIPTION -> body.put(KEY_TEXT, intent.value() != null ? intent.value() : "");
            case DOCUMENTS -> body.put(KEY_PATH, intent.value() != null ? intent.value() : "");
            case ISSUE, LESSON, PULL_REQUEST -> body.put(KEY_REFERENCE,
                                intent.value() != null ? intent.value() : "");
            default -> {
                // no body
            }
        }
        state = act(link, body);
    }

    private void continueToReady() throws IOException {
        state = act("start", Map.of());
    }

    // ---------------------------------------------------------------------------------------------
    // Ready

    /**
     * @return {@code true} if the work was created and the dialogue is done
     */
    private boolean ready() throws IOException {
        if (!pending.isEmpty()) {
            addSource(pending.poll());
            return false;
        }
        var entry = entry();
        renderer.ready(state, 2, totalSteps());
        if (!prompt.asks()) {
            create();
            return true;
        }
        var answer = prompt.askRaw(text("start.ready.prompt", Map.of(KEY_KIND, kind.wireValue())),
                            KEY_STATE_READY, text("start.ready.prompt", Map.of(KEY_KIND, kind.wireValue())),
                            entry.entryId());
        if (answer.isEmpty()) {
            create();
            return true;
        }
        if ("e".equalsIgnoreCase(answer)) {
            editReady(entry);
            return false;
        }
        if (leaves(answer)) {
            throw new StartPrompt.StartLeftException();
        }
        // The ready step offers Enter, e and ?; anything else changes nothing and is asked again.
        unoffered(answer, "e, " + leaveKey() + ", ?");
        return false;
    }

    private void editReady(EntryResource entry) {
        var title = prompt.askRaw(text("start.ready.title_row", Map.of(KEY_TITLE, "",
                            KEY_ORIGIN, text("start.ready.title_origin", Map.of()))), KEY_STATE_READY,
                            text("start.ready.title_row", Map.of(KEY_TITLE, "",
                                                KEY_ORIGIN, text("start.ready.title_origin", Map.of()))),
                            entry.entryId());
        if (!title.isEmpty()) {
            titleOverride = title;
        }
        var ready = state.ready();
        if (ready != null && ready.idEditable()) {
            var id = prompt.askRaw(text("start.ready.id_row", Map.of("id", ready.planId(),
                                KEY_ORIGIN, text("start.ready.id_origin_free", Map.of()))), KEY_STATE_READY,
                                text("start.ready.id_row", Map.of("id", ready.planId(),
                                                    KEY_ORIGIN, text("start.ready.id_origin_free", Map.of()))),
                                entry.entryId());
            if (!id.isEmpty()) {
                idOverride = id;
            }
        }
    }

    private void create() throws IOException {
        var body = new LinkedHashMap<String, Object>();
        var title = titleOverride != null ? titleOverride : answers.title;
        var id = idOverride != null ? idOverride : answers.id;
        if (title != null && !title.isBlank()) {
            body.put(KEY_TITLE, title);
        }
        if (id != null && !id.isBlank()) {
            body.put(KEY_PLAN_ID, id);
        }
        var created = api.act(entry().entryId(), "create", body);
        var entry = created.entry();
        var name = entry != null && entry.planId() != null ? entry.planId() : id;
        var display = name != null ? name : "";
        prompt.line("start.started.console_title", Map.of("name", OutputSafety.stripControlSequences(display)));
        prompt.line("start.started.follow", Map.of());
        if (kind.epic() && name != null) {
            new EpicDecompositionDialogue(api, prompt, project, name, answers.instruction).run();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Recipes

    private void recipeChoice() throws IOException {
        var recipes = state.recipes().isEmpty() ? api.recipes() : state.recipes();
        if (recipes.isEmpty()) {
            prompt.line("start.recipe.empty", Map.of(KEY_PROJECT, project));
            throw new StartPrompt.StartLeftException();
        }
        var key = answers.recipeKey;
        if (key == null || key.isBlank()) {
            if (!prompt.asks()) {
                StartPrompt.missing(state.state().wireValue(), text("start.recipe.choose.title", Map.of()),
                                    entry().entryId());
            }
            renderer.recipes(recipes);
            var answer = prompt.askRaw(text("start.source.prompt", Map.of()), state.state().wireValue(),
                                text("start.recipe.choose.title", Map.of()), entry().entryId());
            if (leaves(answer)) {
                throw new StartPrompt.StartLeftException();
            }
            key = recipeKeyAt(recipes, answer);
            if (key == null) {
                // An answer that names no recipe of the list chooses nothing; the list is shown again.
                unoffered(answer, range(recipes.size()) + ", " + leaveKey());
                return;
            }
        }
        state = act("choose:" + key, Map.of());
        if (answers.recipeKey != null && answers.recipeKey.equals(key)) {
            answers.recipeKey = null;
        }
        recipeSelected = true;
    }

    private @Nullable String recipeKeyAt(List<RecipeRef> recipes, String answer) {
        try {
            var index = Integer.parseInt(answer);
            return index >= 1 && index <= recipes.size() ? recipes.get(index - 1).key() : null;
        } catch (NumberFormatException _) {
            return recipes.stream().filter(recipe -> recipe.key().equals(answer)).map(RecipeRef::key)
                                .findFirst().orElse(null);
        }
    }

    private void recipeParameters() throws IOException {
        var entry = entry();
        var recipe = entry.recipe();
        if (recipe == null) {
            state = act("back", Map.of());
            return;
        }
        var parameters = resolvedParameters(recipe);
        renderer.parameters(recipe, parameters);
        if (parameters.stream().anyMatch(parameter -> !parameter.available())) {
            var answer = prompt.askRaw(text("start.source.prompt", Map.of()), KEY_STATE_RECIPE_PARAMETERS,
                                text("start.recipe.no_option", Map.of("parameter", "", KEY_REASON, "")),
                                entry.entryId());
            if ("b".equalsIgnoreCase(answer)) {
                recipeSelected = false;
                state = act("back", Map.of());
                return;
            }
            if (leaves(answer)) {
                throw new StartPrompt.StartLeftException();
            }
            // Only b and q remain while a parameter has no option.
            unoffered(answer, "b, " + leaveKey());
            return;
        }
        if (prompt.asks()) {
            var answer = prompt.askRaw(text("start.recipe.set", Map.of("range",
                                "1-" + parameters.size())), KEY_STATE_RECIPE_PARAMETERS,
                                text("start.recipe.parameters.title", Map.of("recipe", recipe.key())),
                                entry.entryId());
            if ("b".equalsIgnoreCase(answer)) {
                recipeSelected = false;
                state = act("back", Map.of());
                return;
            }
            if (leaves(answer)) {
                throw new StartPrompt.StartLeftException();
            }
            if (!answer.isEmpty() && !changeParameter(parameters, answer)) {
                // An answer that names no parameter sends nothing; the parameters are shown again.
                unoffered(answer, range(parameters.size()) + ", b, " + leaveKey() + ", ?");
                return;
            }
        }
        var body = new LinkedHashMap<String, Object>();
        var values = new LinkedHashMap<String, Object>();
        for (var parameter : parameters) {
            var value = parameterValues.getOrDefault(parameter.name(),
                                parameter.value() != null ? parameter.value() : "");
            values.put(parameter.name(), value);
        }
        body.put(KEY_PARAMETERS, values);
        state = act("set", body);
    }

    private List<RecipeParameter> resolvedParameters(RecipeRef recipe) {
        var parameters = new ArrayList<RecipeParameter>();
        for (var parameter : recipe.parameters()) {
            var value = parameterValues.getOrDefault(parameter.name(), parameter.value());
            parameters.add(new RecipeParameter(parameter.name(), value, parameter.options(),
                                parameter.proposed(), parameter.available(), parameter.reason()));
        }
        return parameters;
    }

    /**
     * Applies {@code n=value} or asks the value of parameter {@code n}.
     *
     * @return {@code false} if the answer names no parameter of the list
     */
    private boolean changeParameter(List<RecipeParameter> parameters, String answer) {
        var separator = answer.indexOf('=');
        if (separator > 0) {
            var index = indexIn(answer.substring(0, separator).trim(), parameters.size());
            if (index == 0) {
                return false;
            }
            parameterValues.put(parameters.get(index - 1).name(), answer.substring(separator + 1).trim());
            return true;
        }
        var index = indexIn(answer, parameters.size());
        if (index == 0) {
            return false;
        }
        var parameter = parameters.get(index - 1);
        var value = prompt.askRaw(text("start.recipe.param_plain", Map.of(KEY_INDEX, index,
                            "name", parameter.name(), "value", parameter.value() != null
                                                ? parameter.value() : "")),
                            KEY_STATE_RECIPE_PARAMETERS, text("start.recipe.parameters.title", Map.of()),
                            entry().entryId());
        if (!value.isEmpty()) {
            parameterValues.put(parameter.name(), value);
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // Epic adoption

    private void adoption() throws IOException {
        var entry = entry();
        if (answers.adopt != null && !answers.adopt.isBlank()) {
            adopt(answers.adopt);
            return;
        }
        if (!prompt.asks()) {
            StartPrompt.missing(KEY_STATE_ADOPT, text(KEY_ADOPT_PROMPT, Map.of()), entry.entryId());
            return;
        }
        renderer.adoption(entry);
        var answer = prompt.askRaw(text(KEY_ADOPT_PROMPT, Map.of()), KEY_STATE_ADOPT,
                            text(KEY_ADOPT_PROMPT, Map.of()), entry.entryId());
        if ("n".equalsIgnoreCase(answer)) {
            state = act("new", Map.of());
            return;
        }
        if (leaves(answer)) {
            throw new StartPrompt.StartLeftException();
        }
        var index = indexIn(answer, entry.adoptableEpics().size());
        if (index > 0) {
            adopt(entry.adoptableEpics().get(index - 1).epicId());
            return;
        }
        // An answer that names no epic of the offer adopts nothing; the offer is shown again.
        unoffered(answer, range(entry.adoptableEpics().size()) + ", n, " + leaveKey() + ", ?");
    }

    private void adopt(String epicId) throws IOException {
        var entryId = entry().entryId();
        try {
            state = api.act(entryId, "adopt:" + epicId, Map.of());
        } catch (StartApi.StartActException _) {
            answers.adopt = null;
            state = api.fetch(entryId);
            return;
        }
        prompt.line("start.adopt.done", Map.of("epic", OutputSafety.stripControlSequences(epicId)));
        adopted = true;
    }

    private void adoptionFailed() throws IOException {
        var failure = Objects.requireNonNull(state.adoptionFailure(), "adoption failure");
        renderer.adoptionFailure(failure);
        if (!prompt.asks()) {
            StartPrompt.missing(KEY_STATE_ADOPT_FAILED, text("start.adopt.failed.retry", Map.of()),
                                entry().entryId());
            return;
        }
        var answer = prompt.askRaw(text(KEY_ADOPT_PROMPT, Map.of()), KEY_STATE_ADOPT_FAILED,
                            text("start.adopt.failed.retry", Map.of()), entry().entryId());
        switch (answer.toLowerCase(Locale.ROOT)) {
            case "t" -> {
                answers.adopt = null;
                state = act("retry", Map.of());
            }
            case "b" -> {
                answers.adopt = null;
                state = act("back", Map.of());
            }
            case "x" -> {
                state = act("abandon", Map.of());
                prompt.line("start.entry.abandoned", Map.of());
                throw new StartPrompt.StartLeftException();
            }
            default -> {
                if (leaves(answer)) {
                    throw new StartPrompt.StartLeftException();
                }
                // the cause is shown again with the keys it offers
                unoffered(answer, "t, b, x, " + leaveKey());
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Chat

    private void openChat() throws IOException {
        var entry = entry();
        if (!entry.chatAvailable()) {
            var disabled = entry.chatDisabledReason();
            var reason = disabled != null ? text(disabled, Map.of(KEY_KIND, kind.wireValue())) : "";
            prompt.line("start.chat.waits_title", Map.of(KEY_REASON, reason));
            return;
        }
        // The chat is the entry's own chat resource (Starting Work, Chatting About the Request): it is
        // opened on the entry's chat route, and a refusal is said and leaves the sources as they are.
        var entryId = entry.entryId();
        try {
            new ChatDialogue(api, prompt, kind, project).run(entryId, api.openEntryChat(entryId));
        } catch (StartApi.StartActException refused) {
            prompt.raw(OutputSafety.stripControlSequences(refused.getMessage()));
        }
        state = api.fetch(entryId);
    }

    // ---------------------------------------------------------------------------------------------
    // Answers, project, and helpers

    private void prepareAnswers() {
        if (StartAnswers.STDIN.equals(answers.description)) {
            answers.description = readStandardInput();
        }
        if (answers.description != null && !answers.description.isBlank()) {
            pending.add(new SourceIntent(SourceKind.DESCRIPTION, answers.description));
        }
        answers.documents.forEach(path -> pending.add(new SourceIntent(SourceKind.DOCUMENTS, path)));
        answers.issues.forEach(issue -> pending.add(new SourceIntent(SourceKind.ISSUE, issue)));
        if (answers.lesson != null) {
            pending.add(new SourceIntent(SourceKind.LESSON, answers.lesson));
        }
        if (answers.localChanges) {
            pending.add(new SourceIntent(SourceKind.LOCAL_CHANGES, null));
        }
        if (answers.pullRequest != null) {
            pending.add(new SourceIntent(SourceKind.PULL_REQUEST, answers.pullRequest));
        }
        answers.parameters.forEach(parameterValues::put);
    }

    private String resolveProject() throws IOException {
        if (answers.project != null && !answers.project.isBlank() && !".".equals(answers.project)) {
            return answers.project;
        }
        var configured = environment.get("PM_PROJECT");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        var ids = api.projects();
        var directory = Path.of("").toAbsolutePath().getFileName();
        var name = directory != null ? directory.toString() : "";
        var exact = ids.stream().filter(id -> id.equalsIgnoreCase(name)).findFirst().orElse(null);
        if (exact != null) {
            fromWorkingDirectory = true;
            return exact;
        }
        var prefix = ids.stream().filter(id -> name.toLowerCase(Locale.ROOT)
                            .startsWith(id.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
        if (prefix != null) {
            fromWorkingDirectory = true;
            return prefix;
        }
        if (ids.size() == 1) {
            return ids.getFirst();
        }
        return "default";
    }

    private StartStateResource act(String linkId, Map<String, Object> body) throws IOException {
        return api.act(entry().entryId(), linkId, body);
    }

    private EntryResource entry() {
        return Objects.requireNonNull(state.entry(), "entry");
    }

    private boolean offered(SourceKind sourceKind) {
        return entry().kindOffers().stream()
                            .anyMatch(offer -> offer.kind() == sourceKind && offer.available());
    }

    private @Nullable SourceKindOffer offer(SourceKind sourceKind) {
        return entry().kindOffers().stream().filter(offer -> offer.kind() == sourceKind)
                            .findFirst().orElse(null);
    }

    private boolean leaves(String answer) {
        return ChoicePrompt.isLeave(prompt.locale(), answer);
    }

    private String leaveKey() {
        return prompt.locale().toLowerCase(Locale.ROOT).startsWith("de") ? "v" : "q";
    }

    /**
     * Answers an input the prompt does not offer: Enter where the prompt has no primary link, {@code ?}
     * where the state has nothing more to explain, and any other input with the keys it offers
     * (design <em>Dialogues</em>, links as keys). Nothing is sent.
     */
    private void unoffered(String answer, String offered) {
        if (answer.isEmpty()) {
            prompt.line("operator.dialogue.enter_no_default", Map.of());
        } else if ("?".equals(answer)) {
            prompt.line("operator.dialogue.no_explanation", Map.of());
        } else {
            notOffered(answer, offered);
        }
    }

    private void notOffered(String answer, String offered) {
        prompt.line("operator.dialogue.not_offered", Map.of("input", TerminalSanitizer.sanitizeLine(answer),
                            "offered", offered));
    }

    /**
     * @return the 1-based index the answer names within {@code 1..size}, or {@code 0} if it names none
     */
    private static int indexIn(String answer, int size) {
        try {
            var index = Integer.parseInt(answer.trim());
            return index >= 1 && index <= size ? index : 0;
        } catch (NumberFormatException _) {
            return 0;
        }
    }

    private static String range(int size) {
        return size == 1 ? "1" : "1-" + size;
    }

    private int totalSteps() {
        return kind.epic() ? 3 : 2;
    }

    private void printMissing(StartPrompt.MissingInputException missing) {
        prompt.line("start.no_input.need", Map.of(
                            KEY_STATE, missing.state(),
                            "need", missing.need()));
        if (missing.entryId() != null) {
            prompt.raw(JsonTree.write(Map.of(
                                KEY_STATE, missing.state(),
                                "need", missing.need(),
                                "entry", missing.entryId())));
        }
    }

    private static String readStandardInput() {
        try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            var text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(line);
            }
            return text.toString().strip();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read standard input: " + e.getMessage(), e);
        }
    }

    private String text(String key, Map<String, ?> args) {
        return MessageCatalogue.format(prompt.locale(), Objects.requireNonNull(key, "key"),
                            Objects.requireNonNull(args, "args"));
    }

    /** One source the verb carries, applied in the order the command line names them. */
    private record SourceIntent(SourceKind kind, @Nullable String value) {
    }
}
