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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.OperatorContext;

/**
 * Drives a full-screen view on a JLine 4 terminal: it enters the alternate screen, hides the
 * cursor, sets the keypad application mode, reads keys with {@link BindingReader} and a
 * {@link KeyMap}, draws through {@link Display}, and handles the resize signal.
 *
 * <p>Everything it changes is restored in a {@code finally}, also when the renderer or a handler
 * throws: the terminal's attributes, the keypad mode, the cursor, and the alternate screen. A
 * terminal is never left in the alternate screen or with the cursor hidden.
 *
 * <p>The driver knows nothing about the content of a view. A {@link Renderer} supplies the
 * {@link TuiScreen} to draw; a {@link KeyHandler} receives the actions and returns {@code false}
 * to leave. The filtering of the rows reads one line while the view is suspended, which is the
 * hand-off to a dialogue that the design describes.
 */
public final class TuiScreenDriver {

    /** The shortest poll wait; a key that arrives wakes the loop at once. */
    static final long POLL_MILLIS = 50L;

    /** What a run of the driver ended with. */
    public enum Result {
        /** The handler asked to leave the view. */
        LEFT,
        /** The terminal is narrower than {@link TuiLayout#MIN_COLUMNS}; nothing was drawn. */
        TOO_NARROW
    }

    /** Supplies what the next drawing shows. */
    @FunctionalInterface
    public interface Renderer {
        /**
         * @return the screen to draw
         */
        TuiScreen screen();
    }

    /** Receives the keys of the view. */
    public interface KeyHandler {
        /**
         * @param action the action of the key
         * @return {@code true} to keep the view open, {@code false} to leave it
         */
        boolean handle(TuiKeyAction action);

        /**
         * Receives the text the operator typed at the filter bar.
         *
         * @param text the filter text, never {@code null}
         */
        default void filter(String text) {
            // A level that does not filter ignores the text.
        }
    }

    private final Terminal terminal;
    private final TuiLayout layout;
    private final String locale;

    /**
     * @param terminal the terminal to draw on
     * @param locale   the interface locale
     */
    public TuiScreenDriver(Terminal terminal, String locale) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.locale = MessageCatalogue.normalizeLocale(locale);
        this.layout = new TuiLayout(this.locale);
    }

    /**
     * Decides whether a full-screen view may open on this terminal and under this machine
     * configuration.
     *
     * <p>It opens only on an interactive terminal whose type is not {@code dumb}, when
     * {@code console.full_screen} of {@code machine-config.json} is not {@code false} (the default
     * is {@code true}), and when the terminal is at least {@code minColumns} wide. {@code NO_COLOR}
     * does not keep the view from opening: the view uses no colour, only the inverse video of the
     * selected row, which every terminal that can address a cursor shows.
     *
     * @param context    the operator context
     * @param terminal   the terminal the view would draw on
     * @param minColumns the narrowest width the view accepts
     * @return {@code true} when the view may open
     */
    public static boolean canOpenFullScreen(OperatorContext context, Terminal terminal, int minColumns) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(terminal, "terminal");
        if (!context.isInteractiveTerminal()) {
            return false;
        }
        var type = terminal.getType();
        if (type == null || Terminal.TYPE_DUMB.equals(type) || "dumb-color".equals(type)) {
            return false;
        }
        if (!fullScreenConfigured(context)) {
            return false;
        }
        return terminal.getColumns() >= minColumns;
    }

    /**
     * Reads {@code console.full_screen} from {@code machine-config.json}. A missing file, a
     * malformed file, or a value that is not a boolean falls back to the default {@code true}.
     *
     * @param context the operator context
     * @return the configured value, or {@code true}
     */
    static boolean fullScreenConfigured(OperatorContext context) {
        try {
            var raw = context.directFiles().readMachineConfig();
            if (raw.isEmpty()) {
                return true;
            }
            var root = JsonTree.asObject(JsonTree.parse(raw.get()));
            if (root != null && root.get("console") instanceof Map<?, ?> console) {
                return !(console.get("full_screen") instanceof Boolean enabled) || enabled;
            }
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException _) {
            // An unreadable or malformed machine configuration keeps the compiled default.
        }
        return true;
    }

    /**
     * Runs the drawing loop until the handler returns {@code false} or the input ends.
     *
     * @param renderer the source of the next screen
     * @param handler  the receiver of the keys
     * @return how the run ended
     * @throws IOException if the terminal cannot be written or read
     */
    public Result run(Renderer renderer, KeyHandler handler) throws IOException {
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(handler, "handler");
        if (terminal.getColumns() < TuiLayout.MIN_COLUMNS) {
            terminal.writer().println(MessageCatalogue.format(locale,
                                "operator.overview.too_narrow", Map.of()));
            terminal.writer().flush();
            return Result.TOO_NARROW;
        }

        var previousAttributes = terminal.enterRawMode();
        var previousWinch = terminal.handle(Terminal.Signal.WINCH, _ -> {
            // The loop resizes and redraws on its next turn; the handler only has to exist so the
            // signal does not reach the default, which would interrupt the process.
        });
        boolean alternate = false;
        try {
            enterAlternate();
            alternate = true;
            var display = new Display(terminal, true);
            var reader = new BindingReader(terminal.reader());
            var keyMap = keyMap();
            boolean running = true;
            while (running) {
                display.resize(terminal);
                display.update(layout.render(renderer.screen(), terminal.getColumns(), terminal.getRows()), 0);
                int peeked = terminal.reader().peek(POLL_MILLIS);
                if (peeked == NonBlockingReader.EOF) {
                    break;
                }
                if (peeked == NonBlockingReader.READ_EXPIRED) {
                    continue;
                }
                var action = reader.readBinding(keyMap, null, false);
                if (action == null) {
                    continue;
                }
                if (action == TuiKeyAction.FILTER) {
                    suspend(display);
                    filter(handler);
                    resume(display);
                    continue;
                }
                running = handler.handle(action);
            }
        } finally {
            if (alternate) {
                restore();
            }
            terminal.setAttributes(previousAttributes);
            terminal.handle(Terminal.Signal.WINCH, previousWinch);
            terminal.flush();
        }
        return Result.LEFT;
    }

    private void filter(KeyHandler handler) {
        try {
            var lineReader = LineReaderBuilder.builder().terminal(terminal).build();
            var text = lineReader.readLine("/ ");
            handler.filter(text == null ? "" : text);
        } catch (EndOfFileException | UserInterruptException _) {
            handler.filter("");
        }
    }

    private void enterAlternate() {
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        terminal.puts(InfoCmp.Capability.cursor_invisible);
        terminal.puts(InfoCmp.Capability.keypad_xmit);
        terminal.flush();
    }

    private void suspend(Display display) {
        terminal.puts(InfoCmp.Capability.keypad_local);
        terminal.puts(InfoCmp.Capability.cursor_normal);
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        terminal.flush();
        display.reset();
    }

    private void resume(Display display) {
        enterAlternate();
        display.reset();
    }

    private void restore() {
        terminal.puts(InfoCmp.Capability.keypad_local);
        terminal.puts(InfoCmp.Capability.cursor_normal);
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        terminal.flush();
    }

    private KeyMap<TuiKeyAction> keyMap() {
        var map = new KeyMap<TuiKeyAction>();
        for (var binding : allBindings()) {
            map.bindIfNotBound(binding.action(), binding.trigger());
        }
        bindCapability(map, TuiKeyAction.MOVE_UP, InfoCmp.Capability.key_up);
        bindCapability(map, TuiKeyAction.MOVE_DOWN, InfoCmp.Capability.key_down);
        bindCapability(map, TuiKeyAction.PAGE_UP, InfoCmp.Capability.key_ppage);
        bindCapability(map, TuiKeyAction.PAGE_DOWN, InfoCmp.Capability.key_npage);
        bindCapability(map, TuiKeyAction.OPEN, InfoCmp.Capability.key_enter);
        bindCapability(map, TuiKeyAction.BACK, InfoCmp.Capability.key_left);
        bindCapability(map, TuiKeyAction.OPEN, InfoCmp.Capability.key_right);
        return map;
    }

    private void bindCapability(KeyMap<TuiKeyAction> map, TuiKeyAction action, InfoCmp.Capability capability) {
        var sequence = KeyMap.key(terminal, capability);
        if (sequence != null && !sequence.isEmpty()) {
            map.bindIfNotBound(action, sequence);
        }
    }

    private static List<TuiKeyRegistry.Binding> allBindings() {
        var bindings = new ArrayList<TuiKeyRegistry.Binding>();
        for (var kind : TuiLevel.Kind.values()) {
            for (var binding : TuiKeyRegistry.bindings(kind)) {
                if (bindings.stream().noneMatch(existing -> existing.trigger().equals(binding.trigger())
                                    && existing.action() == binding.action())) {
                    bindings.add(binding);
                }
            }
        }
        return bindings;
    }

    /**
     * @return the terminal the driver draws on
     */
    public Terminal terminal() {
        return terminal;
    }

    /**
     * @return the layout of the driver
     */
    public TuiLayout layout() {
        return layout;
    }
}
