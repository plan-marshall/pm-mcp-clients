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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
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
 * terminal is never left in the alternate screen or with the cursor hidden. While the alternate
 * screen is open a shutdown hook restores it as well, so an interrupt, a {@code SIGTERM}, or any
 * other shutdown that skips the {@code finally} does not leave the operator's terminal unreadable;
 * the two paths are idempotent and may race.
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
         * Moves the modal menu's own selection; the level's row selection does not move while the
         * menu is open.
         *
         * @param delta {@code -1} for up, {@code 1} for down
         * @return {@code true} to keep the view open
         */
        default boolean menuNavigate(int delta) {
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
    private final AtomicBoolean alternateActive = new AtomicBoolean();
    private final AtomicBoolean restored = new AtomicBoolean(true);
    private final AtomicBoolean stopping = new AtomicBoolean();
    private @Nullable Display activeDisplay;
    private final AtomicReference<@Nullable Attributes> savedAttributes = new AtomicReference<>();
    private final AtomicReference<@Nullable Thread> shutdownHook = new AtomicReference<>();
    private final AtomicBoolean suspended = new AtomicBoolean();

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
        savedAttributes.set(previousAttributes);
        var previousWinch = terminal.handle(Terminal.Signal.WINCH, _ -> {
            // The loop resizes and redraws on its next turn; the handler only has to exist so the
            // signal does not reach the default, which would interrupt the process.
        });
        boolean alternate = false;
        try {
            alternate = true;
            stopping.set(false);
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
            while (running && !stopping.get()) {
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
                    running = false;
                } else if (peeked != NonBlockingReader.READ_EXPIRED) {
                    var turn = readTurn(reader, display, screen, keyMap, handler, previous);
                    previous = turn.previous();
                    running = turn.running();
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

    /**
     * Reads and dispatches one turn of the view: an open menu owns the keys, otherwise the level's
     * key map does. The filter hands the terminal to a dialogue and leaves the alternate screen, so
     * the next turn draws a full frame; a handler that suspends the view does the same.
     *
     * @param reader   the binding reader of the terminal
     * @param display  the display of the view
     * @param screen   the screen of the turn
     * @param keyMap   the key map of the level
     * @param handler  the receiver of the keys
     * @param previous the screen drawn before, or {@code null}
     * @return the screen to remember and whether the view stays open
     */
    private Turn readTurn(BindingReader reader, Display display, TuiScreen screen,
                        KeyMap<TuiKeyAction> keyMap, KeyHandler handler, @Nullable TuiScreen previous) {
        var menu = screen.menu();
        if (menu != null) {
            var binding = reader.readBinding(menuKeyMap(menu), null, false);
            if (binding == null) {
                return new Turn(previous, true);
            }
            suspended.set(false);
            boolean keep = dispatchMenu(handler, menu, binding);
            return new Turn(suspended.get() ? null : previous, keep);
        }
        var action = reader.readBinding(keyMap, null, false);
        if (action == null || !boundOn(screen.kind(), action)) {
            return new Turn(previous, true);
        }
        if (action == TuiKeyAction.FILTER) {
            suspend(display);
            filter(handler);
            resume(display);
            return new Turn(null, true);
        }
        suspended.set(false);
        boolean keep = handler.handle(action);
        return new Turn(suspended.get() ? null : previous, keep);
    }

    /**
     * What one turn of the view decided.
     *
     * @param previous the screen to remember for the next diff, or {@code null} to draw a full frame
     * @param running  whether the view stays open
     */
    private record Turn(@Nullable TuiScreen previous, boolean running) {
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
        if (alternateActive.compareAndSet(false, true)) {
            restored.set(false);
            registerShutdownHook();
        }
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
        alternateActive.set(false);
        restoreImmediately();
        removeShutdownHook();
    }

    /**
     * Leaves the alternate screen, shows the cursor again, restores the keypad mode, and restores
     * the terminal attributes the raw mode replaced. This is the path the shutdown hook runs on the
     * JVM's shutdown thread, so it is idempotent: the normal exit and the hook may race, and the
     * closed terminal's failure to take the attributes back is swallowed rather than turning a
     * clean exit into a failed one.
     *
     * <p>The flag is the idempotence latch: the first caller writes the sequences, every later
     * caller returns without touching the terminal. A run that opens the alternate screen again
     * clears the latch.
     */
    void restoreImmediately() {
        if (!restored.compareAndSet(false, true)) {
            return;
        }
        stopping.set(true);
        try {
            terminal.puts(InfoCmp.Capability.keypad_local);
            terminal.puts(InfoCmp.Capability.cursor_normal);
            terminal.puts(InfoCmp.Capability.exit_ca_mode);
            terminal.flush();
        } finally {
            restoreAttributes();
        }
    }

    private void restoreAttributes() {
        var attributes = savedAttributes.get();
        if (attributes == null) {
            return;
        }
        try {
            terminal.setAttributes(attributes);
        } catch (IllegalStateException _) {
            // The terminal was already closed; there is no attribute state left to restore.
        }
    }

    /**
     * Registers the shutdown hook that restores the terminal when the process is interrupted or
     * terminated with {@code SIGINT} or {@code SIGTERM}. A hook cannot be added while the JVM is
     * already shutting down; the {@code finally} of {@link #run} still restores the terminal in
     * that case.
     */
    private void registerShutdownHook() {
        var hook = new Thread(this::restoreImmediately, "tui-terminal-restore");
        shutdownHook.set(hook);
        try {
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (IllegalStateException _) {
            shutdownHook.set(null);
        }
    }

    /**
     * Removes the shutdown hook after a clean exit. The JVM refuses the removal while it shuts
     * down, and then the hook is already running or has run; the idempotence latch keeps the two
     * paths from writing the sequences twice.
     */
    private void removeShutdownHook() {
        var hook = shutdownHook.getAndSet(null);
        if (hook == null) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException _) {
            // The JVM is already shutting down; the hook restores, or has restored, the terminal.
        }
    }

    /**
     * @return {@code true} while the shutdown hook of an open view is registered
     */
    boolean shutdownHookRegistered() {
        return shutdownHook.get() != null;
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
            suspended.set(true);
            resume(display);
        }
    }

    private KeyMap<String> menuKeyMap(TuiMenu menu) {
        var map = new KeyMap<String>();
        map.setAmbiguousTimeout(AMBIGUOUS_TIMEOUT_MILLIS);
        bindMenuCapability(map, MENU_UP, InfoCmp.Capability.key_up);
        bindMenuCapability(map, MENU_DOWN, InfoCmp.Capability.key_down);
        for (var entry : menu.entries()) {
            map.bindIfNotBound(entry.trigger(), entry.trigger());
        }
        map.bind(MENU_CLOSE, "\u001b");
        map.bindIfNotBound(MENU_CLOSE, "a");
        map.bind(MENU_ENTER, "\r");
        map.bindIfNotBound(MENU_ENTER, "\n");
        return map;
    }

    private void bindMenuCapability(KeyMap<String> map, String target, InfoCmp.Capability capability) {
        var sequence = KeyMap.key(terminal, capability);
        if (sequence != null && !sequence.isEmpty()) {
            map.bind(target, sequence);
        }
    }

    private static boolean dispatchMenu(KeyHandler handler, TuiMenu menu, String binding) {
        return switch (binding) {
            case MENU_CLOSE -> handler.menu("");
            case MENU_UP -> handler.menuNavigate(-1);
            case MENU_DOWN -> handler.menuNavigate(1);
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
        // The capability sequences come first: a lone Esc is their prefix, and a binding that is
        // already a prefix would keep bindIfNotBound from adding the longer arrow sequence.
        bindCapability(map, TuiKeyAction.MOVE_UP, InfoCmp.Capability.key_up);
        bindCapability(map, TuiKeyAction.MOVE_DOWN, InfoCmp.Capability.key_down);
        bindCapability(map, TuiKeyAction.PAGE_UP, InfoCmp.Capability.key_ppage);
        bindCapability(map, TuiKeyAction.PAGE_DOWN, InfoCmp.Capability.key_npage);
        bindCapability(map, TuiKeyAction.OPEN, InfoCmp.Capability.key_enter);
        bindCapability(map, TuiKeyAction.BACK, InfoCmp.Capability.key_left);
        bindCapability(map, TuiKeyAction.OPEN, InfoCmp.Capability.key_right);
        for (var binding : TuiKeyRegistry.bindings(kind)) {
            map.bindIfNotBound(binding.action(), binding.trigger());
        }
        return map;
    }

    private void bindCapability(KeyMap<TuiKeyAction> map, TuiKeyAction action, InfoCmp.Capability capability) {
        var sequence = KeyMap.key(terminal, capability);
        if (sequence != null && !sequence.isEmpty()) {
            map.bind(action, sequence);
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
