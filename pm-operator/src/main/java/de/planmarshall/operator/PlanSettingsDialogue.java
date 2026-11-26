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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.PlanSettingsValue;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.operator.actions.WorkActionsClient;
import de.planmarshall.operator.dialogue.ChoicePrompt;

/**
 * A plan's own settings ({@code plan-settings.open}, {@code plan-settings.value},
 * {@code plan-settings.apply}, UC-CTL-5): the aspects grouped as the settings group them, the
 * value of one aspect with its origin, a refused value asked again, a change that is too late, and
 * a change another client made first.
 *
 * <p>A refusal that names a code the catalogue knows is shown with that code's title, never with
 * the runtime's own words, and a refusal of the act itself (a coded {@code 409}: the plan has
 * ended, the team owns the value) ends the dialogue instead of asking the value again.
 *
 * <p>The revision travels with the change: the dialogue keeps the revision it opened the settings
 * with, and on a conflict it offers to apply the operator's choice again against the current one or
 * to keep the current value. The client never compares values itself.
 */
final class PlanSettingsDialogue {

    private static final String STATE_ID = "control.settings";
    private static final String GROUP_PLAN_DEFAULTS = "plan-defaults";
    private static final String GROUP_ROLES = "roles";
    private static final String GROUP_INTEGRATIONS = "integrations";
    private static final String GROUP_PROVIDERS = "providers";
    private static final List<String> GROUPS = List.of(GROUP_PLAN_DEFAULTS, GROUP_ROLES,
                        GROUP_INTEGRATIONS, GROUP_PROVIDERS);
    private static final String PREFIX = "operator.actions.settings.aspect.";
    private static final String ASPECT_FIELD = "aspect";

    private final StartPrompt prompt;
    private final WorkActionsClient client;
    private final String planId;

    private PlanSettingsDialogue(OperatorContext context, PrintWriter out, String planId, boolean noInput) {
        this.planId = planId;
        this.prompt = new StartPrompt(context, out, context.clientEnvironment().locale(), noInput);
        this.client = new WorkActionsClient(context);
    }

    /**
     * Runs a plan's settings dialogue.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param planId  the plan
     * @param group   the aspect group, or {@code null} to ask it
     * @param noInput whether the verb may ask nothing
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String planId, @Nullable String group,
                        boolean noInput) throws IOException {
        var dialogue = new PlanSettingsDialogue(context, out, planId, noInput);
        try {
            return dialogue.execute(group);
        } catch (StartPrompt.MissingInputException missing) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), "operator.actions.settings.stopped",
                                Map.of("state", missing.state())));
            out.flush();
            return StartDialogue.EXIT_MISSING_INPUT;
        } catch (StartPrompt.InterruptException _) {
            return StartPrompt.EXIT_INTERRUPTED;
        } catch (StartPrompt.StartLeftException _) {
            return 0;
        }
    }

    private int execute(@Nullable String group) throws IOException {
        var settings = client.settings(planId);
        @Nullable String chosenGroup = group != null && !group.isBlank() ? normalizeGroup(group) : askGroup();
        if (chosenGroup == null) {
            return 0;
        }
        var values = valuesOf(settings, chosenGroup);
        if (values.isEmpty()) {
            return 0;
        }
        showGroup(chosenGroup, values);
        var aspect = askAspect(values);
        if (aspect == null) {
            return 0;
        }
        return change(settings, aspect);
    }

    private @Nullable String askGroup() {
        var options = new StringBuilder(MessageCatalogue.format(prompt.locale(),
                            "operator.actions.settings.which", Map.of("plan", planId)));
        for (int index = 0; index < GROUPS.size(); index++) {
            options.append("  ").append(index + 1).append(") ").append(groupLabel(GROUPS.get(index)));
        }
        while (true) {
            var answer = prompt.askRaw(options.toString(), STATE_ID,
                                MessageCatalogue.format(prompt.locale(), "control.settings.title",
                                                    Map.of("plan", planId)), planId);
            if (leaves(answer)) {
                return null;
            }
            int index = parseIndex(answer, GROUPS.size());
            if (index > 0) {
                return GROUPS.get(index - 1);
            }
            notOffered(answer, range(1, GROUPS.size()));
        }
    }

    private void showGroup(String group, List<PlanSettingsValue> values) {
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.settings.header",
                            Map.of(ASPECT_FIELD, groupLabel(group), "plan", planId)));
        for (var value : values) {
            var renderedValue = value.value() != null
                                ? OutputSafety.stripControlSequences(value.value())
                                : MessageCatalogue.format(prompt.locale(), "common.none", Map.of());
            prompt.raw("  " + OutputSafety.stripControlSequences(value.aspect()) + "  "
                                + renderedValue
                                + "   " + MessageCatalogue.format(prompt.locale(), "control.settings.origin",
                                Map.of("origin", value.origin())));
        }
    }

    private @Nullable String askAspect(List<PlanSettingsValue> values) {
        var options = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            options.append(index + 1).append(") ").append(OutputSafety.stripControlSequences(
                                values.get(index).aspect())).append("  ");
        }
        options.append(leaveKey()).append(") ").append(MessageCatalogue.format(prompt.locale(),
                            "control.settings.close", Map.of()));
        while (true) {
            var answer = prompt.askRaw(options.toString(), STATE_ID,
                                MessageCatalogue.format(prompt.locale(), "operator.actions.settings.value",
                                                    Map.of(ASPECT_FIELD, "", "plan", planId)), planId);
            if (leaves(answer)) {
                return null;
            }
            int index = parseIndex(answer, values.size());
            if (index > 0) {
                return values.get(index - 1).aspect();
            }
            notOffered(answer, range(1, values.size()));
        }
    }

    private int change(PlanSettingsRecord settings, String aspect) throws IOException {
        var value = settings.value(aspect);
        var choices = value != null ? value.choices() : List.<String>of();
        while (true) {
            var chosen = askValue(aspect, choices);
            if (chosen == null) {
                return 0;
            }
            var answer = client.updateSettings(planId, settings.revision(), aspect, chosen.value());
            var result = answer.result();
            if (result.applied()) {
                return applied(chosen, aspect, result);
            }
            var restoreState = result.restoreState();
            if (restoreState != null) {
                return tooLate(restoreState);
            }
            if (result.conflict()) {
                var next = conflict(aspect, settings, chosen, result);
                if (next == null) {
                    return 0;
                }
                settings = next;
                continue;
            }
            refused(chosen, aspect, answer);
        }
    }

    private int applied(ChosenValue chosen, String aspect, PlanSettingsUpdateResult result) {
        if (chosen.value() == null) {
            prompt.line("operator.actions.settings.removed", Map.of("plan", planId, ASPECT_FIELD, aspect));
        } else {
            prompt.line("operator.actions.settings.applied",
                                Map.of("plan", planId, "version", result.revision()));
        }
        return 0;
    }

    private int tooLate(String state) {
        prompt.line("control.settings.too_late", Map.of("plan", planId, "position", state));
        return 0;
    }

    private @Nullable PlanSettingsRecord conflict(String aspect, PlanSettingsRecord settings, ChosenValue chosen,
                        PlanSettingsUpdateResult result) throws IOException {
        var current = result.currentValue() != null ? result.currentValue() : "";
        prompt.line("common.conflict.changed", Map.of("value", current));
        if (!applyAgain()) {
            return null;
        }
        var again = client.updateSettings(planId, result.revision(), aspect, chosen.value());
        if (again.result().applied()) {
            prompt.line("operator.actions.settings.applied",
                                Map.of("plan", planId, "version", again.result().revision()));
            return null;
        }
        if (!again.result().conflict() && again.result().restoreState() == null) {
            // The retry is refused again: it names its cause before the value is asked once more.
            refused(chosen, aspect, again);
        }
        return new PlanSettingsRecord(planId, again.result().revision(), settings.values());
    }

    private void refused(ChosenValue chosen, String aspect, WorkActionsClient.SettingsAnswer answer) {
        // Only an asking dialogue reaches a change it may repeat: the group, the aspect and the value
        // are asked first, so a refused value is asked again.
        prompt.line("control.settings.refused",
                            Map.of("value", chosen.value() != null ? chosen.value() : aspect,
                                                "reason", refusalReason(answer)));
    }

    /**
     * Why a refused value is refused: a code the catalogue knows names its text in the operator's
     * language, a code it does not know keeps the reason the body carried, and a body without either
     * names none.
     *
     * @param answer the runtime's answer
     * @return the reason, never {@code null}
     */
    private String refusalReason(WorkActionsClient.SettingsAnswer answer) {
        var code = answer.refusalCode();
        if (code != null) {
            return ErrorTexts.title(prompt.locale(), code);
        }
        var reason = answer.refusalReason();
        return reason != null ? reason : "";
    }

    private @Nullable ChosenValue askValue(String aspect, List<String> choices) {
        var options = new StringBuilder();
        for (int index = 0; index < choices.size(); index++) {
            options.append(index + 1).append(") ").append(choices.get(index)).append("  ");
        }
        options.append("0) ").append(MessageCatalogue.format(prompt.locale(),
                            "operator.actions.settings.use_inherited", Map.of()));
        while (true) {
            var answer = prompt.askRaw(options.toString(), STATE_ID,
                                MessageCatalogue.format(prompt.locale(), "operator.actions.settings.value",
                                                    Map.of(ASPECT_FIELD, aspect, "plan", planId)), planId);
            if (leaves(answer)) {
                return null;
            }
            if ("0".equals(answer)) {
                return new ChosenValue(null);
            }
            int index = parseIndex(answer, choices.size());
            if (index > 0) {
                return new ChosenValue(choices.get(index - 1));
            }
            // A value is typed only where the runtime names no choices; where it names them, a value
            // outside them is not offered and is never sent.
            if (choices.isEmpty()) {
                return new ChosenValue(answer);
            }
            notOffered(answer, range(0, choices.size()));
        }
    }

    /**
     * Asks how a conflict goes on: {@code Enter} applies the operator's value again, the keep key of
     * the catalogue keeps the current value, and leaving sends nothing.
     *
     * @return whether the value is applied again
     */
    private boolean applyAgain() {
        var keep = MessageCatalogue.format(prompt.locale(), "operator.actions.key.keep_current", Map.of());
        while (true) {
            var decision = prompt.askRaw(conflictPrompt(), STATE_ID,
                                MessageCatalogue.format(prompt.locale(), "common.conflict.keep", Map.of()),
                                planId);
            if (decision.isEmpty()) {
                return true;
            }
            if (keep.equalsIgnoreCase(decision) || ChoicePrompt.isLeave(prompt.locale(), decision)) {
                return false;
            }
            notOffered(decision, keep + ", " + leaveKey());
        }
    }

    private boolean leaves(String answer) {
        return answer.isEmpty() || ChoicePrompt.isLeave(prompt.locale(), answer);
    }

    private String leaveKey() {
        return prompt.locale().toLowerCase(Locale.ROOT).startsWith("de") ? "v" : "q";
    }

    private String range(int first, int last) {
        return (first == last ? String.valueOf(first) : first + "-" + last) + ", " + leaveKey();
    }

    private void notOffered(String answer, String offered) {
        prompt.line("operator.dialogue.not_offered",
                            Map.of("input", OutputSafety.stripControlSequences(answer), "offered", offered));
    }

    private String conflictPrompt() {
        return MessageCatalogue.format(prompt.locale(), "common.conflict.apply_again", Map.of())
                            + " [Enter]   "
                            + MessageCatalogue.format(prompt.locale(), "operator.actions.key.keep_current",
                            Map.of())
                            + ") " + MessageCatalogue.format(prompt.locale(), "common.conflict.keep", Map.of())
                            + StartPrompt.SUFFIX;
    }

    private String groupLabel(String group) {
        return MessageCatalogue.format(prompt.locale(), PREFIX + group, Map.of());
    }

    private static String normalizeGroup(String group) {
        var clean = group.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (var known : GROUPS) {
            if (known.equals(clean)) {
                return known;
            }
        }
        return clean;
    }

    private static List<PlanSettingsValue> valuesOf(PlanSettingsRecord settings, String group) {
        var values = new ArrayList<PlanSettingsValue>();
        for (var value : settings.values()) {
            if (group.equals(groupOf(value.aspect()))) {
                values.add(value);
            }
        }
        return values;
    }

    private static String groupOf(String aspect) {
        if (aspect.startsWith("role.")) {
            return GROUP_ROLES;
        }
        if (aspect.startsWith("integration.")) {
            return GROUP_INTEGRATIONS;
        }
        if (aspect.startsWith("provider.")) {
            return GROUP_PROVIDERS;
        }
        return GROUP_PLAN_DEFAULTS;
    }

    private static int parseIndex(String answer, int size) {
        try {
            int index = Integer.parseInt(answer.trim());
            return index >= 1 && index <= size ? index : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }

    private record ChosenValue(@Nullable String value) {
    }
}
