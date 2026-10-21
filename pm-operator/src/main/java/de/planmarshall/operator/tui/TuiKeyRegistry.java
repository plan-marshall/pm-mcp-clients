/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * The declarative key bindings of every full-screen level, and the footer generated from them.
 *
 * <p>The registry is the single source of the footer: a key is advertised only where the level
 * binds it, and a level cannot bind one key twice. {@link #validateAll()} fails for both, and
 * {@link TuiKeyRegistryTest} calls it, so a footer that advertises an unbound key or two actions
 * on one key cannot be merged.
 */
public final class TuiKeyRegistry {

    /**
     * One trigger of an action.
     *
     * @param trigger the character or terminal sequence that triggers the action
     * @param action  the action
     */
    public record Binding(String trigger, TuiKeyAction action) {

        /**
         * @param trigger the trigger sequence
         * @param action  the action
         */
        public Binding {
            Objects.requireNonNull(trigger, "trigger");
            Objects.requireNonNull(action, "action");
            if (trigger.isEmpty()) {
                throw new IllegalArgumentException("a binding needs a trigger");
            }
        }
    }

    /**
     * One entry of a footer.
     *
     * @param display the key as it is shown, for example {@code ↑↓}
     * @param action  the action the entry names
     */
    public record FooterEntry(String display, TuiKeyAction action) {

        /**
         * @param display the key as it is shown
         * @param action  the action the entry names
         */
        public FooterEntry {
            Objects.requireNonNull(display, "display");
            Objects.requireNonNull(action, "action");
        }
    }

    private record LevelKeys(List<Binding> bindings, List<FooterEntry> footer) {
    }

    private static final String UP = "\u2191";
    private static final String DOWN = "\u2193";
    private static final String ESC = "\u001b";
    private static final String ENTER = "\r";
    private static final String NEWLINE = "\n";
    private static final String RIGHT = "\u2192";
    private static final String LEFT = "\u2190";
    private static final String PAGE_UP = "\u001b[5~";
    private static final String PAGE_DOWN = "\u001b[6~";
    private static final String FOOTER_SEPARATOR = "   ";

    private static final Map<TuiLevel.Kind, LevelKeys> REGISTRY = build();

    private TuiKeyRegistry() {
    }

    /**
     * @param kind the kind of level
     * @return the bindings of the level, in no particular order
     */
    public static List<Binding> bindings(TuiLevel.Kind kind) {
        return REGISTRY.get(kind).bindings();
    }

    /**
     * @param kind the kind of level
     * @return the footer entries of the level, in the order they are drawn
     */
    public static List<FooterEntry> footerEntries(TuiLevel.Kind kind) {
        return REGISTRY.get(kind).footer();
    }

    /**
     * Renders the footer of a level in the given language.
     *
     * @param kind         the kind of level
     * @param locale       the interface locale
     * @param reportsCount the number of unread reports, for the {@code R} entry
     * @return the footer line, without a leading or trailing separator
     */
    public static String footer(TuiLevel.Kind kind, String locale, int reportsCount) {
        var parts = new ArrayList<String>();
        for (var entry : footerEntries(kind)) {
            parts.add(entry.display() + ' ' + label(entry.action(), kind, locale, reportsCount));
        }
        return String.join(FOOTER_SEPARATOR, parts);
    }

    /**
     * @param action       the action
     * @param kind         the level the action is shown on, or {@code null} when it is not a switch
     * @param locale       the interface locale
     * @param reportsCount the number of unread reports
     * @return the localized label of the action as the footer shows it
     */
    public static String label(TuiKeyAction action, TuiLevel.Kind kind, String locale, int reportsCount) {
        var params = new LinkedHashMap<String, Object>();
        if (action == TuiKeyAction.SWITCH_VIEW) {
            var other = kind == TuiLevel.Kind.PROJECT_BY_EPIC ? "by_plan" : "by_epic";
            params.put("view", MessageCatalogue.format(locale, "overview.view." + other, Map.of()));
        } else if (action == TuiKeyAction.REPORTS) {
            params.put("count", reportsCount);
        }
        return MessageCatalogue.format(locale, planKey(action, kind), params);
    }

    /**
     * The plan view names its keys from {@code operator.plan.keys.*} where the design lists them,
     * and falls back to the shared action keys for move, filter, help, leave, and back.
     */
    private static String planKey(TuiKeyAction action, TuiLevel.Kind kind) {
        if (kind != TuiLevel.Kind.PLAN) {
            return action.textKey();
        }
        var key = switch (action) {
            case OPEN -> "operator.plan.keys.open";
            case CLOSE -> "operator.plan.keys.close";
            case EXPAND_ALL -> "operator.plan.keys.all";
            case WHERE -> "operator.plan.keys.where";
            case NON_LANE -> "operator.plan.keys.not_in_plan";
            case TOKENS -> "operator.plan.keys.tokens";
            case PROJECT -> "operator.plan.keys.project";
            case EPIC -> "operator.plan.keys.epic";
            case COMPLETION -> "operator.plan.keys.completion";
            case ACTIONS -> "operator.plan.keys.actions";
            default -> null;
        };
        return key != null ? key : action.textKey();
    }

    /**
     * Verifies the registry: no level binds one key twice, and every footer entry names an action
     * the level actually binds.
     *
     * @throws IllegalStateException if the registry is inconsistent
     */
    public static void validateAll() {
        for (var entry : REGISTRY.entrySet()) {
            var kind = entry.getKey();
            var keys = entry.getValue();
            Set<String> triggers = new HashSet<>();
            Set<TuiKeyAction> boundActions = new HashSet<>();
            for (var binding : keys.bindings()) {
                if (!triggers.add(binding.trigger())) {
                    throw new IllegalStateException("level " + kind + " binds " + printable(binding.trigger())
                                        + " twice");
                }
                boundActions.add(binding.action());
            }
            for (var footer : keys.footer()) {
                if (!boundActions.contains(footer.action())) {
                    throw new IllegalStateException("the footer of " + kind + " advertises "
                                        + footer.action() + " without a binding");
                }
            }
        }
    }

    private static String printable(String trigger) {
        var sb = new StringBuilder();
        for (int i = 0; i < trigger.length(); i++) {
            char c = trigger.charAt(i);
            sb.append(c < 0x20 ? "\\u%04x".formatted((int) c) : String.valueOf(c));
        }
        return sb.toString();
    }

    private static Map<TuiLevel.Kind, LevelKeys> build() {
        var registry = new EnumMap<TuiLevel.Kind, LevelKeys>(TuiLevel.Kind.class);

        var navigation = new ArrayList<Binding>();
        navigation.add(new Binding(UP, TuiKeyAction.MOVE_UP));
        navigation.add(new Binding("k", TuiKeyAction.MOVE_UP));
        navigation.add(new Binding(DOWN, TuiKeyAction.MOVE_DOWN));
        navigation.add(new Binding("j", TuiKeyAction.MOVE_DOWN));
        navigation.add(new Binding(PAGE_UP, TuiKeyAction.PAGE_UP));
        navigation.add(new Binding(PAGE_DOWN, TuiKeyAction.PAGE_DOWN));
        navigation.add(new Binding("g", TuiKeyAction.FIRST));
        navigation.add(new Binding("G", TuiKeyAction.LAST));
        navigation.add(new Binding(ENTER, TuiKeyAction.OPEN));
        navigation.add(new Binding(NEWLINE, TuiKeyAction.OPEN));
        navigation.add(new Binding(RIGHT, TuiKeyAction.OPEN));
        navigation.add(new Binding("l", TuiKeyAction.OPEN));
        navigation.add(new Binding(ESC, TuiKeyAction.BACK));
        navigation.add(new Binding(LEFT, TuiKeyAction.BACK));
        navigation.add(new Binding("h", TuiKeyAction.BACK));
        navigation.add(new Binding("/", TuiKeyAction.FILTER));
        navigation.add(new Binding("r", TuiKeyAction.RELOAD));
        navigation.add(new Binding("?", TuiKeyAction.HELP));
        navigation.add(new Binding("q", TuiKeyAction.LEAVE));

        var overviewNavigation = new ArrayList<>(navigation);
        overviewNavigation.add(new Binding("E", TuiKeyAction.ENDED));
        overviewNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        overviewNavigation.add(new Binding("R", TuiKeyAction.REPORTS));
        registry.put(TuiLevel.Kind.OVERVIEW, level(overviewNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("E", TuiKeyAction.ENDED), footer("R", TuiKeyAction.REPORTS),
                            footer("/", TuiKeyAction.FILTER), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        registry.put(TuiLevel.Kind.PROJECTS, level(projectRootNavigation(navigation),
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("!", TuiKeyAction.WAITS),
                            footer("E", TuiKeyAction.ENDED), footer("R", TuiKeyAction.REPORTS),
                            footer("/", TuiKeyAction.FILTER), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        var projectNavigation = new ArrayList<>(navigation);
        projectNavigation.add(new Binding("\t", TuiKeyAction.SWITCH_VIEW));
        projectNavigation.add(new Binding("e", TuiKeyAction.EPIC));
        projectNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        projectNavigation.add(new Binding("!", TuiKeyAction.WAITS));
        projectNavigation.add(new Binding("E", TuiKeyAction.ENDED));
        projectNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        projectNavigation.add(new Binding("R", TuiKeyAction.REPORTS));

        registry.put(TuiLevel.Kind.PROJECT_BY_PLAN, level(projectNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("e", TuiKeyAction.EPIC),
                            footer("!", TuiKeyAction.WAITS), footer("Tab", TuiKeyAction.SWITCH_VIEW),
                            footer("E", TuiKeyAction.ENDED), footer("Esc", TuiKeyAction.BACK)));

        var epicNavigation = new ArrayList<>(projectNavigation);
        epicNavigation.removeIf(binding -> binding.action() == TuiKeyAction.EPIC);
        registry.put(TuiLevel.Kind.PROJECT_BY_EPIC, level(epicNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("!", TuiKeyAction.WAITS),
                            footer("Tab", TuiKeyAction.SWITCH_VIEW), footer("E", TuiKeyAction.ENDED),
                            footer("Esc", TuiKeyAction.BACK)));

        var planNavigation = planNavigation();
        registry.put(TuiLevel.Kind.PLAN, level(planNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("→", TuiKeyAction.OPEN), footer("←", TuiKeyAction.CLOSE),
                            footer(".", TuiKeyAction.WHERE), footer("*", TuiKeyAction.EXPAND_ALL),
                            footer("n", TuiKeyAction.NON_LANE), footer("t", TuiKeyAction.TOKENS),
                            footer("p", TuiKeyAction.PROJECT), footer("e", TuiKeyAction.EPIC),
                            footer("c", TuiKeyAction.COMPLETION), footer("a", TuiKeyAction.ACTIONS),
                            footer("Esc", TuiKeyAction.BACK), footer("q", TuiKeyAction.LEAVE)));

        var activityNavigation = new ArrayList<>(navigation);
        activityNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        activityNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        activityNavigation.add(new Binding("R", TuiKeyAction.REPORTS));
        registry.put(TuiLevel.Kind.PLAN_ACTIVITY, level(activityNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Esc", TuiKeyAction.BACK),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

        registry.put(TuiLevel.Kind.PLAN_COMPLETION, level(activityNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Esc", TuiKeyAction.BACK),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

        var epicLevel = new ArrayList<>(navigation);
        epicLevel.add(new Binding("a", TuiKeyAction.ACTIONS));
        epicLevel.add(new Binding("p", TuiKeyAction.PROJECT));
        epicLevel.add(new Binding("!", TuiKeyAction.WAITS));
        epicLevel.add(new Binding("*", TuiKeyAction.EXPAND_ALL));
        registry.put(TuiLevel.Kind.EPIC, level(epicLevel,
                            footer("\u2191\u2193", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("p", TuiKeyAction.PROJECT),
                            footer("!", TuiKeyAction.WAITS),
                            footer("Esc", TuiKeyAction.BACK), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        var endedNavigation = new ArrayList<>(navigation);
        endedNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        endedNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        endedNavigation.add(new Binding("R", TuiKeyAction.REPORTS));
        registry.put(TuiLevel.Kind.ENDED, level(endedNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("/", TuiKeyAction.FILTER),
                            footer("Esc", TuiKeyAction.BACK)));

        var reportsNavigation = new ArrayList<>(navigation);
        reportsNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        registry.put(TuiLevel.Kind.REPORTS, level(reportsNavigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("Enter", TuiKeyAction.OPEN),
                            footer("/", TuiKeyAction.FILTER), footer("Esc", TuiKeyAction.BACK)));

        registry.put(TuiLevel.Kind.SYSTEM, level(navigation,
                            footer("↑↓", TuiKeyAction.MOVE_UP), footer("/", TuiKeyAction.FILTER),
                            footer("r", TuiKeyAction.RELOAD), footer("Esc", TuiKeyAction.BACK)));

        return Map.copyOf(registry);
    }

    private static FooterEntry footer(String display, TuiKeyAction action) {
        return new FooterEntry(display, action);
    }

    /**
     * The keys of the plan view: the tree replaces the overview's open and back, and the plan adds
     * its own keys for open-all, the jump to where it is, the lane toggle, tokens, the project and
     * epic jumps, and the completion report.
     */
    private static List<Binding> planNavigation() {
        var bindings = new ArrayList<Binding>();
        bindings.add(new Binding(UP, TuiKeyAction.MOVE_UP));
        bindings.add(new Binding("k", TuiKeyAction.MOVE_UP));
        bindings.add(new Binding(DOWN, TuiKeyAction.MOVE_DOWN));
        bindings.add(new Binding("j", TuiKeyAction.MOVE_DOWN));
        bindings.add(new Binding(PAGE_UP, TuiKeyAction.PAGE_UP));
        bindings.add(new Binding(PAGE_DOWN, TuiKeyAction.PAGE_DOWN));
        bindings.add(new Binding("g", TuiKeyAction.FIRST));
        bindings.add(new Binding("G", TuiKeyAction.LAST));
        bindings.add(new Binding(ENTER, TuiKeyAction.OPEN));
        bindings.add(new Binding(NEWLINE, TuiKeyAction.OPEN));
        bindings.add(new Binding(RIGHT, TuiKeyAction.OPEN));
        bindings.add(new Binding("l", TuiKeyAction.OPEN));
        bindings.add(new Binding(LEFT, TuiKeyAction.CLOSE));
        bindings.add(new Binding("h", TuiKeyAction.CLOSE));
        bindings.add(new Binding(ESC, TuiKeyAction.BACK));
        bindings.add(new Binding("/", TuiKeyAction.FILTER));
        bindings.add(new Binding("r", TuiKeyAction.RELOAD));
        bindings.add(new Binding("?", TuiKeyAction.HELP));
        bindings.add(new Binding("q", TuiKeyAction.LEAVE));
        bindings.add(new Binding("a", TuiKeyAction.ACTIONS));
        bindings.add(new Binding("e", TuiKeyAction.EPIC));
        bindings.add(new Binding("p", TuiKeyAction.PROJECT));
        bindings.add(new Binding("*", TuiKeyAction.EXPAND_ALL));
        bindings.add(new Binding(".", TuiKeyAction.WHERE));
        bindings.add(new Binding("n", TuiKeyAction.NON_LANE));
        bindings.add(new Binding("t", TuiKeyAction.TOKENS));
        bindings.add(new Binding("c", TuiKeyAction.COMPLETION));
        return bindings;
    }

    private static List<Binding> projectRootNavigation(List<Binding> navigation) {
        var bindings = new ArrayList<>(navigation);
        bindings.add(new Binding("a", TuiKeyAction.ACTIONS));
        bindings.add(new Binding("!", TuiKeyAction.WAITS));
        bindings.add(new Binding("E", TuiKeyAction.ENDED));
        bindings.add(new Binding("S", TuiKeyAction.SYSTEM));
        bindings.add(new Binding("R", TuiKeyAction.REPORTS));
        return bindings;
    }

    private static LevelKeys level(List<Binding> bindings, FooterEntry... footer) {
        return new LevelKeys(List.copyOf(bindings), List.of(footer));
    }
}
