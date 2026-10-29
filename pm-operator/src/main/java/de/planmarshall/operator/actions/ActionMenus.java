/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.tui.TuiMenuEntry;

/**
 * Builds the entries of the actions menu from what the runtime offers for one subject.
 *
 * <p>The entries follow what the runtime offers: only an action it lists is shown, and a disabled
 * one keeps the reason the runtime names. The decision a waiting plan waits for comes first, named
 * by the runtime's decisions read because the actions resource does not carry it
 * ({@code doc/design/terminal-client/19-actions.adoc#full-screen}); the acts of a hold come next and
 * are taken from the hold record, so a hold that clears or gains an act changes the menu without a
 * new client release.
 */
public final class ActionMenus {

    private static final String KEY_PREFIX = "operator.actions.key.";
    private static final String ELLIPSIS = "\u2026";
    private static final String ACTION_CHAT = "chat";
    private static final String ACTION_DECIDE = "decide";
    private static final Set<String> WAITING_STATES = Set.of("waiting", "waiting_for_you");

    private ActionMenus() {
    }

    /**
     * @param state the plan's state as the runtime names it
     * @return {@code true} when the design gives that state the decision it waits for first
     */
    public static boolean waitsForDecision(String state) {
        return WAITING_STATES.contains(state);
    }

    /**
     * @param locale       the interface locale
     * @param actions      the actions the runtime offers for the plan
     * @param hold         the plan's hold, or {@code null} when it has none
     * @param openDecision the decision a waiting plan waits for, or {@code null}
     * @param rowId        the row the menu is drawn below, or {@code null}
     * @return the plan's menu
     */
    public static ActionsMenu plan(String locale, WorkActions actions, @Nullable HoldRecord hold,
                        @Nullable String openDecision, @Nullable String rowId) {
        var entries = new ArrayList<TuiMenuEntry>();
        var used = new HashSet<String>();
        var dropped = new ArrayList<String>();
        addDecision(entries, used, dropped, locale, actions.state(), openDecision);
        boolean holdEnds = false;
        if (hold != null) {
            holdEnds = addHoldActs(entries, used, dropped, locale, hold);
        }
        for (var availability : actions.actions()) {
            var action = availability.action();
            if (holdEnds && ("cancel".equals(action) || "abandon".equals(action))) {
                continue;
            }
            add(entries, used, dropped, availability, planLabel(locale, action), locale);
        }
        return new ActionsMenu(actions.subjectId(), ActionsMenu.Subject.PLAN, rowId, entries, dropped);
    }

    /**
     * @param locale          the interface locale
     * @param actions         the actions the runtime offers for the epic
     * @param nextPlan        the plan the next-work action would start, or {@code null}
     * @param waitReasons     why no plan can start, one line per row
     * @param autoStart       whether the epic starts its plans by itself
     * @param overlapMaxFiles how many files a plan may share with a running one
     * @param findingsNew     the number of new findings
     * @param watchesOpen     the number of open watches
     * @param rowId           the row the menu is drawn below, or {@code null}
     * @return the epic's menu
     */
    public static ActionsMenu epic(String locale, WorkActions actions, @Nullable String nextPlan,
                        List<String> waitReasons, boolean autoStart, int overlapMaxFiles, int findingsNew,
                        int watchesOpen, @Nullable String rowId) {
        var entries = new ArrayList<TuiMenuEntry>();
        var used = new HashSet<String>();
        var dropped = new ArrayList<String>();
        boolean holdCancels = addEpicHoldActs(entries, used, dropped, locale, actions.holdKind());
        var labels = new EpicLabels(nextPlan, waitReasons, autoStart, overlapMaxFiles, findingsNew, watchesOpen);
        for (var availability : actions.actions()) {
            var action = availability.action();
            if (holdCancels && "cancel".equals(action)) {
                continue;
            }
            add(entries, used, dropped, availability, epicLabel(locale, action, labels), locale);
        }
        return new ActionsMenu(actions.subjectId(), ActionsMenu.Subject.EPIC, rowId, entries, dropped);
    }

    private record EpicLabels(@Nullable String nextPlan, List<String> waitReasons, boolean autoStart,
    int overlapMaxFiles, int findingsNew, int watchesOpen) {
        private EpicLabels {
            waitReasons = List.copyOf(waitReasons);
        }
    }

    /**
     * @param locale    the interface locale
     * @param subjectId the id of the ended plan or epic
     * @param subject   the ended subject's display name
     * @param rowId     the row the menu is drawn below, or {@code null}
     * @return the ended item's menu, which offers the read-only chat alone
     */
    public static ActionsMenu ended(String locale, String subjectId, String subject, @Nullable String rowId) {
        var label = MessageCatalogue.format(locale, "operator.actions.ended", Map.of("name", subject));
        var chat = MessageCatalogue.format(locale, "control.plan.chat", Map.of()) + ELLIPSIS;
        var entry = TuiMenuEntry.enabled(key(locale, ACTION_CHAT), ACTION_CHAT, label + "   " + chat);
        return new ActionsMenu(subjectId, ActionsMenu.Subject.ENDED, rowId, List.of(entry), List.of());
    }

    /**
     * @param locale   the interface locale
     * @param landingId the id of the alongside landing
     * @param holdKind the kind of hold, or {@code null}
     * @param rowId    the row the menu is drawn below, or {@code null}
     * @return the landing's menu
     */
    public static ActionsMenu landing(String locale, String landingId, @Nullable String holdKind,
                        @Nullable String rowId) {
        var entries = new ArrayList<TuiMenuEntry>();
        var used = new HashSet<String>();
        var dropped = new ArrayList<String>();
        addHoldAct(entries, used, dropped, locale, "retry");
        addHoldAct(entries, used, dropped, locale, HoldActs.SHOW_PR);
        if ("landing_failed".equals(holdKind)) {
            addHoldAct(entries, used, dropped, locale, HoldActs.GIVE_UP_REMOVAL);
        }
        if (entries.isEmpty()) {
            addHoldAct(entries, used, dropped, locale, "retry");
        }
        return new ActionsMenu(landingId, ActionsMenu.Subject.LANDING, rowId, entries, dropped);
    }

    private static void addDecision(List<TuiMenuEntry> entries, Set<String> used, List<String> dropped,
                        String locale, String state, @Nullable String openDecision) {
        if (openDecision == null || openDecision.isBlank() || !waitsForDecision(state)) {
            return;
        }
        var trigger = key(locale, ACTION_DECIDE);
        if (!used.add(trigger)) {
            dropped.add(ACTION_DECIDE);
            return;
        }
        entries.add(TuiMenuEntry.enabled(trigger, ACTION_DECIDE,
                            MessageCatalogue.format(locale, "operator.actions.decide",
                                                Map.of("id", openDecision))));
    }

    private static boolean addHoldActs(List<TuiMenuEntry> entries, Set<String> used, List<String> dropped,
                        String locale, HoldRecord hold) {
        boolean ends = hold.offers(HoldActs.END_PLAN) || hold.offers(HoldActs.ABORT);
        for (var act : hold.acts()) {
            // A plan's hold never carries a landing's acts (show the pull request, give up the
            // removal): those belong to a failed alongside landing or an epic's landing.
            if (HoldActs.isLandingAct(act) || HoldActs.CANCEL_EPIC.equals(act)) {
                continue;
            }
            addHoldAct(entries, used, dropped, locale, act);
        }
        return ends;
    }

    private static boolean addEpicHoldActs(List<TuiMenuEntry> entries, Set<String> used,
                        List<String> dropped, String locale, @Nullable String holdKind) {
        if (holdKind == null) {
            return false;
        }
        switch (holdKind) {
            case "landing_failed" -> {
                addHoldAct(entries, used, dropped, locale, "retry");
                addHoldAct(entries, used, dropped, locale, HoldActs.SHOW_PR);
                addHoldAct(entries, used, dropped, locale, HoldActs.GIVE_UP_REMOVAL);
            }
            case "integrity_pause" -> {
                addHoldAct(entries, used, dropped, locale, "retry");
                addHoldAct(entries, used, dropped, locale, HoldActs.ACCEPT);
            }
            case "stalled" -> {
                addHoldAct(entries, used, dropped, locale, "retry");
                addHoldAct(entries, used, dropped, locale, "recycle");
            }
            case "condition" -> {
                addHoldAct(entries, used, dropped, locale, "retry");
                addHoldAct(entries, used, dropped, locale, "grant");
                addHoldAct(entries, used, dropped, locale, "add_credential");
                addHoldAct(entries, used, dropped, locale, "raise_budget");
            }
            default -> addHoldAct(entries, used, dropped, locale, "retry");
        }
        addHoldAct(entries, used, dropped, locale, HoldActs.CANCEL_EPIC);
        return true;
    }

    private static void addHoldAct(List<TuiMenuEntry> entries, Set<String> used, List<String> dropped,
                        String locale, String act) {
        var trigger = key(locale, act);
        if (!used.add(trigger)) {
            dropped.add(act);
            return;
        }
        entries.add(TuiMenuEntry.enabled(trigger, act, holdLabel(locale, act)));
    }

    private static void add(List<TuiMenuEntry> entries, Set<String> used, List<String> dropped,
                        ActionAvailability availability, String label, String locale) {
        var trigger = key(locale, availability.action());
        if (!used.add(trigger)) {
            dropped.add(availability.action());
            return;
        }
        if (availability.available()) {
            entries.add(TuiMenuEntry.enabled(trigger, availability.action(), label));
        } else {
            entries.add(TuiMenuEntry.disabled(trigger, availability.action(), label,
                                availability.reason() != null ? availability.reason() : ""));
        }
    }

    private static String planLabel(String locale, String action) {
        return switch (action) {
            case "pause" -> text(locale, "control.plan.pause") + ELLIPSIS;
            case "start" -> text(locale, "control.plan.start");
            case "cancel" -> text(locale, "control.plan.cancel") + ELLIPSIS;
            case "abandon" -> text(locale, "control.plan.abandon") + ELLIPSIS;
            case "chat" -> text(locale, "control.plan.chat") + ELLIPSIS;
            case "settings" -> text(locale, "control.plan.settings") + ELLIPSIS;
            default -> action;
        };
    }

    private static String epicLabel(String locale, String action, EpicLabels labels) {
        return switch (action) {
            case "next" -> labels.nextPlan() != null
                                ? MessageCatalogue.format(locale, "control.epic.next",
                                Map.of("plan", labels.nextPlan()))
                                : MessageCatalogue.format(locale, "control.epic.next_none",
                                Map.of("reasons", String.join(", ", labels.waitReasons())));
            case "auto" -> MessageCatalogue.format(locale, "control.epic.auto",
                                Map.of("mode", labels.autoStart() ? "on" : "off"));
            case "common_files" -> MessageCatalogue.format(locale, "control.epic.overlap",
                                Map.of("count", labels.overlapMaxFiles()));
            case "decompose" -> text(locale, "control.epic.decompose_again") + ELLIPSIS;
            case "findings" -> count(text(locale, "control.epic.findings_review"), labels.findingsNew());
            case "watches" -> count(text(locale, "control.epic.watches_review"), labels.watchesOpen());
            case "chat" -> text(locale, "control.plan.chat") + ELLIPSIS;
            case "archive" -> text(locale, "control.epic.archive") + ELLIPSIS;
            case "cancel" -> text(locale, "control.epic.cancel") + ELLIPSIS;
            default -> action;
        };
    }

    private static String count(String label, int count) {
        return count > 0 ? label + " (" + count + ")" : label;
    }

    private static String holdLabel(String locale, String act) {
        return HoldActs.label(locale, act);
    }

    private static String key(String locale, String action) {
        var name = switch (action) {
            case "decompose" -> "decompose_again";
            case "findings" -> "findings_review";
            case "watches" -> "watches_review";
            default -> HoldActs.keyName(action);
        };
        return MessageCatalogue.format(locale, KEY_PREFIX + name, Map.of());
    }

    private static String text(String locale, String key) {
        return MessageCatalogue.format(locale, key, Map.of());
    }
}
