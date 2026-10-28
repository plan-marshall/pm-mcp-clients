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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TuiKeyRegistry")
class TuiKeyRegistryTest {

    @Test
    @DisplayName("the registry binds no key twice and advertises no unbound key")
    void registryIsConsistent() {
        TuiKeyRegistry.validateAll();
        for (var kind : TuiLevel.Kind.values()) {
            var triggers = new HashSet<String>();
            for (var binding : TuiKeyRegistry.bindings(kind)) {
                assertTrue(triggers.add(binding.trigger()),
                                    kind + " binds " + binding.trigger() + " twice");
            }
            for (var entry : TuiKeyRegistry.footerEntries(kind)) {
                var bound = TuiKeyRegistry.bindings(kind).stream()
                                    .map(TuiKeyRegistry.Binding::trigger).toList();
                for (var trigger : triggersOf(entry.display())) {
                    assertTrue(bound.contains(trigger),
                                        kind + " advertises " + entry.display() + " but does not bind " + trigger);
                }
            }
        }
    }

    @Test
    @DisplayName("every level has a footer with at least one key")
    void everyLevelHasFooter() {
        for (var kind : TuiLevel.Kind.values()) {
            assertFalse(TuiKeyRegistry.footerEntries(kind).isEmpty(), kind.name());
            assertFalse(TuiKeyRegistry.footer(kind, "en", 0).isBlank(), kind.name());
        }
    }

    @Test
    @DisplayName("the footer names the actions in the operator's language")
    void footerIsLocalized() {
        var en = TuiKeyRegistry.footer(TuiLevel.Kind.OVERVIEW, "en", 5);
        var de = TuiKeyRegistry.footer(TuiLevel.Kind.OVERVIEW, "de", 5);
        assertAll(
                            () -> assertTrue(en.contains("move"), en),
                            () -> assertTrue(en.contains("open"), en),
                            () -> assertTrue(en.contains("ended work"), en),
                            () -> assertTrue(en.contains("5 reports"), en),
                            () -> assertTrue(de.contains("bewege"), de),
                            () -> assertTrue(de.contains("beendete Arbeit"), de),
                            () -> assertTrue(de.contains("5 Berichte"), de)
        );
    }

    @Test
    @DisplayName("the switch entry names the view the key leads to")
    void switchViewNamesTheOtherView() {
        assertAll(
                            () -> assertTrue(TuiKeyRegistry.footer(TuiLevel.Kind.PROJECT_BY_PLAN, "en", 0)
                                                .contains("by epic")),
                            () -> assertTrue(TuiKeyRegistry.footer(TuiLevel.Kind.PROJECT_BY_EPIC, "en", 0)
                                                .contains("by plan"))
        );
    }

    @Test
    @DisplayName("the guard fails for a footer without a binding")
    void guardRejectsUnboundFooterEntry() {
        var bindings = List.of(new TuiKeyRegistry.Binding("x", TuiKeyAction.OPEN));
        var entries = List.of(new TuiKeyRegistry.FooterEntry("q", TuiKeyAction.LEAVE));
        assertThrows(IllegalStateException.class, () -> validate(bindings, entries));
    }

    @Test
    @DisplayName("the guard fails for two bindings of one key")
    void guardRejectsDuplicateBinding() {
        var bindings = List.of(new TuiKeyRegistry.Binding("x", TuiKeyAction.OPEN),
                            new TuiKeyRegistry.Binding("x", TuiKeyAction.LEAVE));
        var entries = List.of(new TuiKeyRegistry.FooterEntry("x", TuiKeyAction.OPEN));
        assertThrows(IllegalStateException.class, () -> validate(bindings, entries));
    }

    @Test
    @DisplayName("a binding needs a non-empty trigger")
    void bindingNeedsTrigger() {
        assertThrows(IllegalArgumentException.class,
                            () -> new TuiKeyRegistry.Binding("", TuiKeyAction.OPEN));
    }

    @Test
    @DisplayName("a binding carries the priority of its action")
    void bindingCarriesPriority() {
        assertAll(
                            () -> assertEquals(TuiKeyRegistry.priorityOf(TuiKeyAction.MOVE_UP),
                                                new TuiKeyRegistry.Binding("j", TuiKeyAction.MOVE_UP).priority()),
                            () -> assertTrue(TuiKeyRegistry.priorityOf(TuiKeyAction.MOVE_UP)
                                                < TuiKeyRegistry.priorityOf(TuiKeyAction.LEAVE)),
                            () -> assertTrue(TuiKeyRegistry.priorityOf(TuiKeyAction.LEAVE)
                                                < TuiKeyRegistry.priorityOf(TuiKeyAction.PAGE_UP))
        );
    }

    @Test
    @DisplayName("at 80 columns the footer keeps move, open, back, actions, help and leave and ends ? more")
    void footerKeepsThePriorityKeysAt80Columns() {
        var footer = TuiKeyRegistry.footer(TuiLevel.Kind.PLAN, "en", 0, 79);
        var cells = cells(footer);

        assertAll(
                            () -> assertTrue(cells.contains("\u2191\u2193 move"), footer),
                            () -> assertTrue(cells.contains("Enter open"), footer),
                            () -> assertTrue(cells.contains("Esc back"), footer),
                            () -> assertTrue(cells.contains("a actions"), footer),
                            () -> assertTrue(cells.contains("q leave"), footer),
                            () -> assertTrue(footer.endsWith(TuiKeyRegistry.moreCell("en")), footer),
                            () -> assertFalse(footer.contains("\u2026"), footer),
                            () -> assertTrue(footer.indexOf("q leave") < footer.indexOf("? more"), footer)
        );
    }

    @Test
    @DisplayName("a wider footer shows more keys and keeps the narrower one's order")
    void widerFooterShowsMoreKeysInPriorityOrder() {
        var narrow = cells(TuiKeyRegistry.footer(TuiLevel.Kind.PLAN, "en", 0, 79));
        var wide = cells(TuiKeyRegistry.footer(TuiLevel.Kind.PLAN, "en", 0, 118));
        var narrowKeys = narrow.subList(0, narrow.size() - 1);
        var wideWithoutHelp = wide.stream()
                            .filter(cell -> !cell.equals(TuiKeyRegistry.helpCell("en")))
                            .toList();

        assertAll(
                            () -> assertTrue(wide.size() > narrow.size(), wide.toString()),
                            () -> assertEquals(narrowKeys, wideWithoutHelp.subList(0, narrowKeys.size())),
                            () -> assertTrue(narrow.getLast().equals(TuiKeyRegistry.moreCell("en")),
                                                narrow.toString())
        );
    }

    @Test
    @DisplayName("the footer of a level that fits keeps every key and no ? more")
    void footerWithoutOmissionsKeepsEveryKey() {
        var footer = TuiKeyRegistry.footer(TuiLevel.Kind.OVERVIEW, "en", 7, 200);

        assertAll(
                            () -> assertEquals(TuiKeyRegistry.footer(TuiLevel.Kind.OVERVIEW, "en", 7), footer),
                            () -> assertTrue(footer.contains(TuiKeyRegistry.helpCell("en")), footer),
                            () -> assertTrue(footer.contains("q leave"), footer),
                            () -> assertFalse(footer.contains(TuiKeyRegistry.moreCell("en")), footer)
        );
    }

    private static List<String> cells(String footer) {
        return List.of(footer.split(TuiKeyRegistry.SEPARATOR));
    }

    /**
     * The rules of {@link TuiKeyRegistry#validateAll()} over an arbitrary pair, so the guard test
     * proves that the guard itself fails on the two defects.
     */
    private static void validate(List<TuiKeyRegistry.Binding> bindings, List<TuiKeyRegistry.FooterEntry> footer) {
        var triggers = new HashSet<String>();
        var actions = new HashSet<TuiKeyAction>();
        for (var binding : bindings) {
            if (!triggers.add(binding.trigger())) {
                throw new IllegalStateException("duplicate trigger " + binding.trigger());
            }
            actions.add(binding.action());
        }
        for (var entry : footer) {
            if (!actions.contains(entry.action())) {
                throw new IllegalStateException("unbound footer entry " + entry.display());
            }
        }
    }

    private static List<String> triggersOf(String display) {
        var triggers = new ArrayList<String>();
        switch (display) {
            case "Enter" -> {
                triggers.add("\r");
                triggers.add("\n");
            }
            case "Esc" -> triggers.add("\u001b");
            case "Tab" -> triggers.add("\t");
            case "↑↓" -> {
                triggers.add("↑");
                triggers.add("↓");
            }
            default -> {
                for (int index = 0; index < display.length(); ) {
                    int codePoint = display.codePointAt(index);
                    triggers.add(new String(Character.toChars(codePoint)));
                    index += Character.charCount(codePoint);
                }
            }
        }
        return triggers;
    }

    @Test
    @DisplayName("the key set of a level is closed over its footer")
    void footersUseTheClosedSet() {        Set<TuiKeyAction> seen = new HashSet<>();
        for (var kind : TuiLevel.Kind.values()) {
            for (var entry : TuiKeyRegistry.footerEntries(kind)) {
                seen.add(entry.action());
            }
        }
        assertAll(
                            () -> assertTrue(seen.contains(TuiKeyAction.OPEN)),
                            () -> assertTrue(seen.contains(TuiKeyAction.BACK)),
                            () -> assertTrue(seen.contains(TuiKeyAction.LEAVE)),
                            () -> assertFalse(seen.contains(TuiKeyAction.PAGE_UP))
        );
    }
}
