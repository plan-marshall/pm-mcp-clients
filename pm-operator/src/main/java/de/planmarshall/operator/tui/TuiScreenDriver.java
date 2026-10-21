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
import org.jspecify.annotations.Nullable;

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

    /** The longest wait for a key before the view looks for a changed model again. */
    static final long POLL_MILLIS = 250L;

    /** How long an {@code Esc} waits for the rest of a terminal sequence before it acts. */
    static final long AMBIGUOUS_TIMEOUT_MILLIS = 100L;

    private static final String MENU_CLOSE = "\u0000close";
    private static final String MENU_UP = "\u0000up";
    private static final String MENU_DOWN = "\u0000down";
    private static final String MENU_ENTER = "\u0000enter";

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
         * Receives a mnemonic of the modal actions menu while it is open. The menu owns the keys:
         * only its mnemonics, the arrows with {@code Enter}, and {@code a} or {@code Esc} arrive
         * here, and none of the level's keys is dispatched until the menu is closed.
         *
         * @param trigger the mnemonic, or the empty string when the operator closed the menu
         * @return {@code true} to keep the view open, {@code false} to leave it
         */
        default boolean menu(String trigger) {
            return true;
        }

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
    private @Nullable Display activeDisplay;
    private boolean suspended;

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
            alternate = true;
            enterAlternate();
            var display = new Display(terminal, true);
            activeDisplay = display;
            var reader = new BindingReader(terminal.reader());
            TuiLevel.@Nullable Kind keyMapKind = null;
            KeyMap<TuiKeyAction> keyMap = keyMap(TuiLevel.Kind.OVERVIEW);
            TuiScreen previous = null;
            int previousColumns = -1;
            int previousRows = -1;
            boolean running = true;
            while (running) {
                display.resize(terminal);
                var screen = renderer.screen();
                if (screen.kind() != keyMapKind) {
                    keyMap = keyMap(screen.kind());
                    keyMapKind = screen.kind();
                }
                int columns = terminal.getColumns();
                int rows = terminal.getRows();
                previous = renderIfChanged(display, screen, previous, previousColumns, previousRows);
                previousColumns = columns;
                previousRows = rows;
                int peeked = terminal.reader().peek(POLL_MILLIS);
                if (peeked == NonBlockingReader.EOF) {
                    break;
                }
                if (peeked == NonBlockingReader.READ_EXPIRED) {
                    continue;
                }
                var menu = screen.menu();
                if (menu != null) {
                    var binding = reader.readBinding(menuKeyMap(menu), null, false);
                    if (binding == null) {
                        continue;
                    }
                    suspended = false;
                    running = dispatchMenu(handler, menu, binding);
                    if (suspended) {
                        previous = null;
                    }
                    continue;
                }
                var action = reader.readBinding(keyMap, null, false);
                if (action == null || !boundOn(screen.kind(), action)) {
                    continue;
                }
                if (action == TuiKeyAction.FILTER) {
                    suspend(display);
                    filter(handler);
                    resume(display);
                    // The alternate screen was left and entered again; its content is gone even
                    // when the model did not change, so the next turn must draw a full frame.
                    previous = null;
                    continue;
                }
                suspended = false;
                running = handler.handle(action);
                if (suspended) {
                    previous = null;
                }
            }
        } finally {
            activeDisplay = null;
            if (alternate) {
                restore();
            }
            terminal.setAttributes(previousAttributes);
            terminal.handle(Terminal.Signal.WINCH, previousWinch);
            terminal.flush();
        }
        return Result.LEFT;
    }

    private @Nullable TuiScreen renderIfChanged(Display display, TuiScreen screen,
                        @Nullable TuiScreen previous, int previousColumns, int previousRows) {
        int columns = terminal.getColumns();
        int rows = terminal.getRows();
        if (screen.equals(previous) && columns == previousColumns && rows == previousRows) {
            return previous;
        }
        var rendered = layout.render(screen, columns, rows);
        // A level that loses or gains rows (a closed node, a hidden token panel) must redraw every
        // line: Display's diff only moves the tail and would leave the old, longer body in place.
        // A reset makes the next update a full frame.
        if (previous == null || previous.rows().size() != screen.rows().size()
                            || !Objects.equals(previous.menu(), screen.menu())) {
            display.reset();
        }
        display.update(rendered, 0);
        return screen;
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

    private static boolean boundOn(TuiLevel.Kind kind, TuiKeyAction action) {
        return TuiKeyRegistry.bindings(kind).stream().anyMatch(binding -> binding.action() == action);
    }

    /**
     * Runs a dialogue while the view is suspended: the alternate screen is left, the cursor is
     * shown again, and the dialogue owns the terminal; on return the view enters the alternate
     * screen again and the next turn draws a full frame, because the screen's content is gone.
     *
     * <p>The handler calls this from inside {@link KeyHandler#handle(TuiKeyAction)} or
     * {@link KeyHandler#menu(String)}. Outside a running view the task simply runs.
     *
     * @param task the dialogue
     */
    public void suspendFor(Runnable task) {
        Objects.requireNonNull(task, "task");
        var display = activeDisplay;
        if (display == null) {
            task.run();
            return;
        }
        suspend(display);
        try {
            task.run();
        } finally {
            suspended = true;
            resume(display);
        }
    }

    private KeyMap<String> menuKeyMap(TuiMenu menu) {
        var map = new KeyMap<String>();
        map.setAmbiguousTimeout(AMBIGUOUS_TIMEOUT_MILLIS);
        for (var entry : menu.entries()) {
            map.bindIfNotBound(entry.trigger(), entry.trigger());
        }
        map.bind(MENU_CLOSE, "\u001b");
        map.bindIfNotBound(MENU_CLOSE, "a");
        map.bind(MENU_ENTER, "\r");
        map.bindIfNotBound(MENU_ENTER, "\n");
        bindMenuCapability(map, MENU_UP, InfoCmp.Capability.key_up);
        bindMenuCapability(map, MENU_DOWN, InfoCmp.Capability.key_down);
        return map;
    }

    private void bindMenuCapability(KeyMap<String> map, String target, InfoCmp.Capability capability) {
        var sequence = KeyMap.key(terminal, capability);
        if (sequence != null && !sequence.isEmpty()) {
            map.bindIfNotBound(target, sequence);
        }
    }

    private static boolean dispatchMenu(KeyHandler handler, TuiMenu menu, String binding) {
        return switch (binding) {
            case MENU_CLOSE -> handler.menu("");
            case MENU_UP -> handler.handle(TuiKeyAction.MOVE_UP);
            case MENU_DOWN -> handler.handle(TuiKeyAction.MOVE_DOWN);
            case MENU_ENTER -> {
                var selected = menu.selected();
                yield selected != null ? handler.menu(selected.trigger()) : true;
            }
            default -> handler.menu(binding);
        };
    }

    KeyMap<TuiKeyAction> keyMap(TuiLevel.Kind kind) {
        var map = new KeyMap<TuiKeyAction>();
        // A lone Esc is also the prefix of the arrow and page sequences; the terminal sends a
        // sequence in one write, so a short wait tells them apart without a visible lag on Esc.
        map.setAmbiguousTimeout(AMBIGUOUS_TIMEOUT_MILLIS);
        for (var binding : TuiKeyRegistry.bindings(kind)) {
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
