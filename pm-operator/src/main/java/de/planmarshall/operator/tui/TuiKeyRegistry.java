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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

        /**
         * How early the binding's footer entry is kept when the footer has to leave keys out; the
         * footer is ordered by it, so the priority is a property of the binding, not of the cell.
         *
         * @return the priority, a lower value kept earlier
         */
        public int priority() {
            return priorityOf(action);
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

    /**
     * The groups the help overlay lists the level's bindings in.
     */
    public enum HelpGroup {

        /** Moving the selection. */
        MOVE("operator.overview.keys.move"),
        /** Opening a row or a node. */
        OPEN("operator.overview.keys.open"),
        /** Acting on the selected row or the view. */
        ACT("operator.overview.keys.actions"),
        /** Changing the view. */
        VIEWS("operator.overview.help.views"),
        /** Leaving the overlay or the view. */
        LEAVE("operator.overview.keys.leave");

        private final String labelKey;

        HelpGroup(String labelKey) {
            this.labelKey = labelKey;
        }

        /**
         * @return the message-catalogue key of the group's label
         */
        public String labelKey() {
            return labelKey;
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
    private static final String TAB = "\t";
    private static final String FOOTER_SEPARATOR = "   ";

    private static final String ENTER_LABEL = "Enter";
    private static final String ESC_LABEL = "Esc";
    private static final String TAB_LABEL = "Tab";
    private static final String MOVE_LABEL = "\u2191\u2193";

    /**
     * The separator between two footer cells, so that a caller which fits its own footer line
     * splits it the way the registry joins it.
     */
    public static final String SEPARATOR = FOOTER_SEPARATOR;

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
        return String.join(FOOTER_SEPARATOR, footerCells(kind, locale, reportsCount));
    }

    /**
     * @param kind         the kind of level
     * @param locale       the interface locale
     * @param reportsCount the number of unread reports
     * @return the cells of the footer, in the order they are drawn
     */
    private static List<String> footerCells(TuiLevel.Kind kind, String locale, int reportsCount) {
        var parts = new ArrayList<String>();
        for (var entry : footerEntries(kind)) {
            if (entry.action() == TuiKeyAction.REPORTS && kind == TuiLevel.Kind.OVERVIEW
                                && reportsCount == 0) {
                // The overview names the reports only while there are unread ones.
                continue;
            }
            parts.add(entry.display() + ' ' + label(entry.action(), kind, locale, reportsCount));
        }
        return List.copyOf(parts);
    }

    /**
     * Renders the footer of a level into the width the terminal offers: the entries are kept in
     * priority order until the next one would not fit whole, and the line then ends with the
     * {@code ? more} cell that names the keys the overlay adds. A key is never cut in the middle.
     *
     * @param kind         the kind of level
     * @param locale       the interface locale
     * @param reportsCount the number of unread reports, for the {@code R} entry
     * @param width        the columns the footer may use
     * @return the footer line, without a leading or trailing separator
     */
    public static String footer(TuiLevel.Kind kind, String locale, int reportsCount, int width) {
        return fit(footerCells(kind, locale, reportsCount), width, locale);
    }

    /**
     * The lines of the help overlay, one per group the level binds a key in: the group's label and
     * the keys that reach it, so that every binding of the level is named, whether the footer had
     * room for it or not.
     *
     * @param kind   the kind of level
     * @param locale the interface locale
     * @return the group lines, without a box; empty when the level binds nothing
     */
    public static List<String> helpGroups(TuiLevel.Kind kind, String locale) {
        var cells = new LinkedHashMap<HelpGroup, String>();
        for (var group : HelpGroup.values()) {
            var actions = groupKeys(kind, group);
            if (!actions.isEmpty()) {
                cells.put(group, String.join("   ", actions));
            }
        }
        if (cells.isEmpty()) {
            return List.of();
        }
        var labels = cells.keySet().stream()
                            .map(group -> MessageCatalogue.format(locale, group.labelKey(), Map.of()))
                            .toList();
        var labelWidth = labels.stream().mapToInt(TuiLayout::displayWidth).max().orElse(0);
        var lines = new ArrayList<String>();
        int index = 0;
        for (var keys : cells.values()) {
            var label = labels.get(index++);
            lines.add(label + " ".repeat(Math.max(1, labelWidth - TuiLayout.displayWidth(label) + 2))
                                + keys);
        }
        return List.copyOf(lines);
    }

    /**
     * @param kind  the kind of level
     * @param group the group
     * @return the keys the level reaches the group with, one cell per action
     */
    private static List<String> groupKeys(TuiLevel.Kind kind, HelpGroup group) {
        var actions = new ArrayList<String>();
        for (var action : TuiKeyAction.values()) {
            if (groupOf(action) == group) {
                var displays = new LinkedHashSet<String>();
                for (var binding : bindings(kind)) {
                    if (binding.action() == action) {
                        displays.add(displayOf(binding.trigger()));
                    }
                }
                if (!displays.isEmpty()) {
                    actions.add(String.join(" ", displays));
                }
            }
        }
        return List.copyOf(actions);
    }

    /**
     * @param action the action
     * @return the group of the help overlay the action is listed in
     */
    static HelpGroup groupOf(TuiKeyAction action) {
        return switch (action) {
            case MOVE_UP, MOVE_DOWN, PAGE_UP, PAGE_DOWN, FIRST, LAST -> HelpGroup.MOVE;
            case OPEN, CLOSE -> HelpGroup.OPEN;
            case BACK, LEAVE -> HelpGroup.LEAVE;
            case HELP, SWITCH_VIEW, EPIC -> HelpGroup.VIEWS;
            // Every remaining action acts on the selected row or the view; there is no default, so
            // an action added later has to be classified before the build passes.
            case ACTIONS, WAITS, ENDED, SYSTEM, REPORTS, MARK_READ, MARK_ALL_READ, WINDOW, GROUP_BY,
                PER_PROJECT, FAVOURITE, MOVE_FAVOURITE_UP, MOVE_FAVOURITE_DOWN, SORT, ADD_PROJECT,
                ADD_EPIC, ADD_PLAN, RUNNING_WORK, SETUP, CONTINUE, ABANDON, ENROL_AGAIN, FILTER,
                RELOAD, EXPAND_ALL, WHERE, NON_LANE, TOKENS, PROJECT, COMPLETION -> HelpGroup.ACT;
        };
    }

    /**
     * @param trigger the trigger sequence of a binding
     * @return how the help overlay names that key
     */
    public static String displayOf(String trigger) {
        return switch (trigger) {
            case ENTER, NEWLINE -> ENTER_LABEL;
            case ESC -> ESC_LABEL;
            case TAB -> TAB_LABEL;
            case PAGE_UP -> "PgUp";
            case PAGE_DOWN -> "PgDn";
            default -> trigger;
        };
    }

    /**
     * @param locale the interface locale
     * @return the title of the help overlay
     */
    public static String helpTitle(String locale) {
        return MessageCatalogue.format(locale, "operator.overview.help.title", Map.of());
    }

    /**
     * @param locale the interface locale
     * @return the footer cell the help key is drawn with while nothing is left out
     */
    public static String helpCell(String locale) {
        return "? " + MessageCatalogue.format(locale, "operator.overview.keys.help", Map.of());
    }

    /**
     * @param locale the interface locale
     * @return the footer cell that stands for the keys the width left out
     */
    public static String moreCell(String locale) {
        return "? " + MessageCatalogue.format(locale, "operator.overview.keys.more", Map.of());
    }

    /**
     * @param locale the interface locale
     * @return the footer of an open help overlay
     */
    public static String helpFooter(String locale) {
        return "Esc " + MessageCatalogue.format(locale, "operator.overview.help.close", Map.of());
    }

    /**
     * Fills a footer of already rendered cells into the width the terminal offers. All cells fit
     * when their line does; otherwise the cells are kept in their order until the next one would
     * not fit whole beside the {@code ? more} cell, the help cell is replaced by that cell, and the
     * line ends with it. A key is never cut in the middle.
     *
     * <p>A line too narrow even for one cell beside {@code ? more} carries that cell alone when it
     * fits, and is empty when even it does not: the line never ends in a cut key.
     *
     * @param cells  the cells, in the order they are kept
     * @param width  the columns the footer may use
     * @param locale the interface locale, for the help and {@code more} cells
     * @return the footer line, without a leading or trailing separator
     */
    public static String fit(List<String> cells, int width, String locale) {
        var joined = String.join(FOOTER_SEPARATOR, cells);
        if (displayWidth(joined) <= width) {
            return joined;
        }
        var more = moreCell(locale);
        var kept = keepCells(cells, width, helpCell(locale), more);
        if (kept.isEmpty()) {
            // Only the overflow cell is left, and only when it fits: a narrower line shows nothing
            // rather than a cut key.
            return displayWidth(more) <= width ? more : "";
        }
        kept.add(more);
        return String.join(FOOTER_SEPARATOR, kept);
    }

    /**
     * @param cells the cells, in the order they are kept
     * @param width the columns the footer may use
     * @param help  the help cell, which the {@code more} cell replaces
     * @param more  the cell that names what was left out
     * @return the cells that fit beside {@code more}, in their order
     */
    private static List<String> keepCells(List<String> cells, int width, String help, String more) {
        var kept = new ArrayList<String>();
        for (var cell : cells) {
            if (!help.equals(cell)) {
                var candidate = new ArrayList<>(kept);
                candidate.add(cell);
                candidate.add(more);
                if (displayWidth(String.join(FOOTER_SEPARATOR, candidate)) > width) {
                    return kept;
                }
                kept.add(cell);
            }
        }
        return kept;
    }

    /**
     * @param text the text
     * @return the number of terminal columns the text occupies
     */
    private static int displayWidth(String text) {
        return TuiLayout.displayWidth(text);
    }

    /**
     * The order in which a footer keeps its keys when the width does not hold all of them: the
     * movement first, then open, back, the level's primary act, help, and leave; every other key
     * follows in the order the level declares it.
     *
     * @param action the action
     * @return the priority of the action's footer entry, a lower value kept earlier
     */
    static int priorityOf(TuiKeyAction action) {
        return switch (action) {
            case MOVE_UP, MOVE_DOWN -> 10;
            case OPEN -> 20;
            case BACK -> 30;
            case ACTIONS -> 40;
            case HELP -> 50;
            case LEAVE -> 60;
            // The level's own acts on its row are kept before the generic tree keys.
            case EPIC -> 70;
            case COMPLETION -> 75;
            case PROJECT -> 78;
            default -> 100;
        };
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
        if (kind == TuiLevel.Kind.PROJECTS) {
            return projectKey(action);
        }
        if (kind == TuiLevel.Kind.REPORTS) {
            return reportsKey(action);
        }
        if (kind == TuiLevel.Kind.SYSTEM) {
            return systemKey(action);
        }
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
     * The explorer's footer names its keys in the {@code operator.projects.keys.*} area
     * ({@code doc/design/terminal-client/20-texts-projects.adoc}), not the overview's.
     */
    private static String projectKey(TuiKeyAction action) {
        var key = switch (action) {
            case OPEN -> "operator.projects.keys.actions";
            case MOVE_UP, MOVE_DOWN -> "operator.projects.keys.move";
            case FAVOURITE -> "operator.projects.keys.favourite";
            case SORT -> "operator.projects.keys.sort";
            case FILTER -> "operator.projects.keys.filter";
            case ADD_PROJECT -> "operator.projects.keys.add";
            case LEAVE -> "operator.projects.keys.leave";
            default -> null;
        };
        return key != null ? key : action.textKey();
    }

    /**
     * The reports level names its keys as {@code doc/design/terminal-client/24-reports.adoc#full-screen}
     * lists them: {@code Enter} is the report's details, {@code m} reads, {@code M} reads all shown.
     */
    private static String reportsKey(TuiKeyAction action) {
        var key = switch (action) {
            case OPEN -> "operator.reports.keys.details";
            case MARK_READ -> "operator.reports.keys.read";
            case MARK_ALL_READ -> "operator.reports.keys.read_all";
            case PROJECT -> "operator.plan.keys.project";
            case EPIC -> "operator.plan.keys.epic";
            default -> null;
        };
        return key != null ? key : action.textKey();
    }

    /**
     * The system level names its keys in the {@code operator.system.keys.*} area
     * ({@code doc/design/terminal-client/25-system.adoc#texts}).
     */
    private static String systemKey(TuiKeyAction action) {
        var key = switch (action) {
            case WINDOW -> "operator.system.keys.window";
            case GROUP_BY -> "operator.system.keys.by";
            case PER_PROJECT -> "operator.system.keys.per_project";
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
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("E", TuiKeyAction.ENDED), footer("R", TuiKeyAction.REPORTS),
                            footer("/", TuiKeyAction.FILTER), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        registry.put(TuiLevel.Kind.PROJECTS, level(projectsNavigation(),
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("*", TuiKeyAction.FAVOURITE), footer("s", TuiKeyAction.SORT),
                            footer("/", TuiKeyAction.FILTER), footer("+", TuiKeyAction.ADD_PROJECT),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

        var projectNavigation = new ArrayList<>(navigation);
        projectNavigation.add(new Binding("\t", TuiKeyAction.SWITCH_VIEW));
        projectNavigation.add(new Binding("e", TuiKeyAction.EPIC));
        projectNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        projectNavigation.add(new Binding("!", TuiKeyAction.WAITS));
        projectNavigation.add(new Binding("E", TuiKeyAction.ENDED));
        projectNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        projectNavigation.add(new Binding("R", TuiKeyAction.REPORTS));

        registry.put(TuiLevel.Kind.PROJECT_BY_PLAN, level(projectNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("e", TuiKeyAction.EPIC),
                            footer("!", TuiKeyAction.WAITS), footer(TAB_LABEL, TuiKeyAction.SWITCH_VIEW),
                            footer("E", TuiKeyAction.ENDED), footer(ESC_LABEL, TuiKeyAction.BACK),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

        var epicNavigation = new ArrayList<>(projectNavigation);
        epicNavigation.removeIf(binding -> binding.action() == TuiKeyAction.EPIC);
        registry.put(TuiLevel.Kind.PROJECT_BY_EPIC, level(epicNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("!", TuiKeyAction.WAITS),
                            footer(TAB_LABEL, TuiKeyAction.SWITCH_VIEW), footer("E", TuiKeyAction.ENDED),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        var planNavigation = planNavigation();
        registry.put(TuiLevel.Kind.PLAN, level(planNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("→", TuiKeyAction.OPEN), footer("←", TuiKeyAction.CLOSE),
                            footer(".", TuiKeyAction.WHERE), footer("*", TuiKeyAction.EXPAND_ALL),
                            footer("n", TuiKeyAction.NON_LANE), footer("t", TuiKeyAction.TOKENS),
                            footer("p", TuiKeyAction.PROJECT), footer("e", TuiKeyAction.EPIC),
                            footer("c", TuiKeyAction.COMPLETION), footer("a", TuiKeyAction.ACTIONS),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("q", TuiKeyAction.LEAVE)));

        var activityNavigation = new ArrayList<>(navigation);
        activityNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        activityNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        activityNavigation.add(new Binding("R", TuiKeyAction.REPORTS));
        registry.put(TuiLevel.Kind.PLAN_ACTIVITY, level(activityNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("a", TuiKeyAction.ACTIONS),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

        registry.put(TuiLevel.Kind.PLAN_COMPLETION, level(activityNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("a", TuiKeyAction.ACTIONS),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

        var epicLevel = new ArrayList<>(navigation);
        epicLevel.add(new Binding("a", TuiKeyAction.ACTIONS));
        epicLevel.add(new Binding("p", TuiKeyAction.PROJECT));
        epicLevel.add(new Binding("!", TuiKeyAction.WAITS));
        epicLevel.add(new Binding("*", TuiKeyAction.EXPAND_ALL));
        registry.put(TuiLevel.Kind.EPIC, level(epicLevel,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("p", TuiKeyAction.PROJECT),
                            footer("!", TuiKeyAction.WAITS),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        var endedNavigation = new ArrayList<>(navigation);
        endedNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        endedNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        endedNavigation.add(new Binding("R", TuiKeyAction.REPORTS));
        registry.put(TuiLevel.Kind.ENDED, level(endedNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("a", TuiKeyAction.ACTIONS), footer("/", TuiKeyAction.FILTER),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        var reportsNavigation = new ArrayList<>(navigation);
        reportsNavigation.add(new Binding("S", TuiKeyAction.SYSTEM));
        reportsNavigation.add(new Binding("m", TuiKeyAction.MARK_READ));
        reportsNavigation.add(new Binding("M", TuiKeyAction.MARK_ALL_READ));
        reportsNavigation.add(new Binding("p", TuiKeyAction.PROJECT));
        reportsNavigation.add(new Binding("e", TuiKeyAction.EPIC));
        registry.put(TuiLevel.Kind.REPORTS, level(reportsNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("m", TuiKeyAction.MARK_READ),
                            footer("M", TuiKeyAction.MARK_ALL_READ),
                            footer("p", TuiKeyAction.PROJECT), footer("e", TuiKeyAction.EPIC),
                            footer(ESC_LABEL, TuiKeyAction.BACK), footer("?", TuiKeyAction.HELP),
                            footer("q", TuiKeyAction.LEAVE)));

        var systemNavigation = new ArrayList<>(navigation);
        systemNavigation.add(new Binding("R", TuiKeyAction.REPORTS));
        systemNavigation.add(new Binding("a", TuiKeyAction.ACTIONS));
        systemNavigation.add(new Binding("w", TuiKeyAction.WINDOW));
        systemNavigation.add(new Binding("b", TuiKeyAction.GROUP_BY));
        systemNavigation.add(new Binding("p", TuiKeyAction.PER_PROJECT));
        registry.put(TuiLevel.Kind.SYSTEM, level(systemNavigation,
                            footer(MOVE_LABEL, TuiKeyAction.MOVE_UP), footer(ENTER_LABEL, TuiKeyAction.OPEN),
                            footer("w", TuiKeyAction.WINDOW), footer("b", TuiKeyAction.GROUP_BY),
                            footer("p", TuiKeyAction.PER_PROJECT), footer("a", TuiKeyAction.ACTIONS),
                            footer("?", TuiKeyAction.HELP), footer("q", TuiKeyAction.LEAVE)));

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

    /**
     * The keys of the explorer ({@code doc/design/terminal-client/17-projects.adoc#explorer}): the
     * keys of navigation, the project actions, and the keys that leave the view for a dialogue.
     * {@code r} is <em>Running work</em> here, not the overview's reload.
     */
    private static List<Binding> projectsNavigation() {
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
        bindings.add(new Binding(ESC, TuiKeyAction.BACK));
        bindings.add(new Binding(LEFT, TuiKeyAction.BACK));
        bindings.add(new Binding("h", TuiKeyAction.BACK));
        bindings.add(new Binding("/", TuiKeyAction.FILTER));
        bindings.add(new Binding("?", TuiKeyAction.HELP));
        bindings.add(new Binding("q", TuiKeyAction.LEAVE));
        bindings.add(new Binding("*", TuiKeyAction.FAVOURITE));
        bindings.add(new Binding("J", TuiKeyAction.MOVE_FAVOURITE_DOWN));
        bindings.add(new Binding("K", TuiKeyAction.MOVE_FAVOURITE_UP));
        bindings.add(new Binding("s", TuiKeyAction.SORT));
        bindings.add(new Binding("+", TuiKeyAction.ADD_PROJECT));
        bindings.add(new Binding("e", TuiKeyAction.ADD_EPIC));
        bindings.add(new Binding("p", TuiKeyAction.ADD_PLAN));
        bindings.add(new Binding("r", TuiKeyAction.RUNNING_WORK));
        bindings.add(new Binding("u", TuiKeyAction.SETUP));
        bindings.add(new Binding("c", TuiKeyAction.CONTINUE));
        bindings.add(new Binding("x", TuiKeyAction.ABANDON));
        bindings.add(new Binding("a", TuiKeyAction.ENROL_AGAIN));
        bindings.add(new Binding("E", TuiKeyAction.ENDED));
        return bindings;
    }

    private static LevelKeys level(List<Binding> bindings, FooterEntry... footer) {
        var listed = List.copyOf(bindings);
        var ordered = new ArrayList<>(List.of(footer));
        // The footer is drawn in priority order, so that a narrower terminal drops the last cell
        // first and the key that matters most is never the one it loses: a cell takes the priority
        // of the binding that carries its action on this level.
        ordered.sort(Comparator.comparingInt(entry -> entryPriority(listed, entry)));
        return new LevelKeys(listed, List.copyOf(ordered));
    }

    /**
     * @param bindings the level's bindings
     * @param entry    the footer cell
     * @return the priority of the cell: the lowest one among the bindings that carry its action
     */
    private static int entryPriority(List<Binding> bindings, FooterEntry entry) {
        return bindings.stream()
                            .filter(binding -> binding.action() == entry.action())
                            .mapToInt(Binding::priority)
                            .min()
                            .orElse(priorityOf(entry.action()));
    }
}
