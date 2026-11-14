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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.PmVersion;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * The operator command line {@code pm-operator}: the verbs a human runs at a terminal.
 * <p>
 * The subcommands are the verbs of the design's command tree (01-commands.adoc#command-tree) and
 * nothing else: without a verb the console opens, and what to do first follows from the state of
 * the machine, so there is no verb {@code start} and no verb {@code onboard}. The console commands
 * {@code plans}, {@code plan <plan_id>}, {@code questions} and {@code answer} also run as direct
 * verbs.
 */
@Command(name = "pm-operator", mixinStandardHelpOptions = true, versionProvider = PmOperator.Version.class,
                    description = "Operator command line of PM-MCP.",
                    subcommands = {
                                        StatusCommand.class,
                                        PlansCommand.class,
                                        WebCommand.class,
                                        RuntimeCommand.class,
                                        DrainCommand.class,
                                        ConfigCommand.class,
                                        CredentialsCommand.class,
                                        ProjectCommand.class,
                                        ProjectsCommand.class,
                                        ClientCommand.class,
                                        PlanCommand.class,
                                        EpicCommand.class,
                                        RecipeCommand.class,
                                        LessonSourceCommand.class,
                                        QuestionsCommand.class,
                                        AnswerCommand.class,
                                        ReportsCommand.class,
                                        SystemCommand.class,
                                        CleanupCommand.class,
                                        EnrolHarnessCommand.class,
                                        RolesCommand.class,
                                        AcceptEditCommand.class,
                                        NeedsCommand.class,
                                        FindCommand.class
                    })
@OperatorVerb
public final class PmOperator implements Callable<Integer> {

    /** Exit code of a failed verb. */
    static final int EXIT_FAILURE = 1;

    private final @Nullable OperatorContext context;

    /** Launches the full-screen overview instead of the console. */
    @Option(names = {"--tui"}, description = "Launch the full-screen overview TUI directly")
    boolean tui;

    /** Supplies the release version to {@code --version}. */
    static final class Version implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{"pm-operator " + PmVersion.current()};
        }
    }

    /** Default constructor for CLI reflection / tooling. */
    public PmOperator() {
        this(null);
    }

    /**
     * @param context the operator context
     */
    public PmOperator(@Nullable OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws Exception {
        if (context == null) {
            return 0;
        }
        if (tui) {
            var code = runFullScreen();
            if (code != null) {
                return code;
            }
        }
        var entryFlow = new EntryFlow(context);
        return new PmConsole(context, entryFlow).run();
    }

    /**
     * Opens the overview directly on the terminal when the machine and the terminal allow it.
     *
     * @return the exit code the view ended with, or {@code null} when the view did not open
     * @throws IOException if the terminal cannot be opened or the view cannot be drawn
     */
    @SuppressWarnings("java:S2093") // the terminal is closed only when this context owns it
    private @Nullable Integer runFullScreen() throws IOException {
        var terminal = context.createTerminal();
        var ownsTerminal = context.ownsTerminal(terminal);
        context.useTerminal(terminal);
        try {
            if (!TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS).available()) {
                return null;
            }
            var out = new PrintWriter(terminal.writer(), true);
            var app = new OverviewFullScreenApp(context, terminal, null, null, false);
            app.run(out);
            return app.interrupted() ? StartPrompt.EXIT_INTERRUPTED : 0;
        } finally {
            try {
                context.clearTerminal(terminal);
            } catch (IllegalStateException _) {
                // Scope already cleared
            }
            if (ownsTerminal) {
                try {
                    terminal.close();
                } catch (IOException _) {
                    // Terminal close failure ignored
                }
            }
        }
    }

    /**
     * Runs the command line.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        var out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out),
                            StandardCharsets.UTF_8), false);
        var err = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.err),
                            StandardCharsets.UTF_8), true);
        System.exit(execute(OperatorContext.system(), out, err, args));
    }

    /**
     * @param context the process context
     * @param out     standard output
     * @param err     standard error
     * @param args    the arguments
     * @return the exit code
     */
    public static int execute(OperatorContext context, PrintWriter out, PrintWriter err, String... args) {
        var selection = ProjectSelector.rewrite(context, out, err, args);
        if (!selection.proceed()) {
            return selection.exitCode();
        }
        var commandLine = new CommandLine(new PmOperator(context), new Factory(context));
        commandLine.setOut(out);
        commandLine.setErr(err);
        var bundle = MessageCatalogue.resourceBundle(context.locale());
        applyResourceBundle(commandLine, bundle);
        commandLine.setParameterExceptionHandler((ex, parseArgs) -> {
            var writer = ex.getCommandLine().getErr();
            var locale = context.locale();
            if (ex instanceof CommandLine.UnmatchedArgumentException uae && !uae.getUnmatched().isEmpty()) {
                var first = TerminalSanitizer.sanitizeLine(uae.getUnmatched().get(0)).strip();
                String msg;
                if (ex.getCommandLine().getParent() == null) {
                    if (!first.startsWith("-")) {
                        msg = MessageCatalogue.format(locale, "operator.cli.unknown_verb", Map.of("verb", first));
                    } else {
                        msg = MessageCatalogue.format(locale, "operator.cli.unknown_option", Map.of("option", first));
                    }
                } else {
                    if (first.startsWith("-")) {
                        msg = MessageCatalogue.format(locale, "operator.cli.unknown_option", Map.of("option", first));
                    } else {
                        msg = MessageCatalogue.format(locale, "operator.cli.unknown_argument", Map.of("argument", first));
                    }
                }
                writer.println(msg);
                writer.flush();
                return CommandLine.ExitCode.USAGE;
            }
            writer.println(TerminalSanitizer.sanitizeLine(ex.getMessage()));
            writer.flush();
            return CommandLine.ExitCode.USAGE;
        });
        commandLine.setExecutionExceptionHandler((exception, command, parseResult) -> {
            if (exception instanceof StartPrompt.InterruptException
                                || exception instanceof UserInterruptException) {
                return StartPrompt.EXIT_INTERRUPTED;
            }
            if (exception instanceof OperatorSecurityException ose) {
                err.println(command.getCommandName() + ": " + TerminalSanitizer.sanitizeLine(ose.getMessage()));
                err.flush();
                return ose.exitCode();
            }
            if (exception instanceof OperatorRefusedException ore) {
                if (ore.exitCode() == 4) {
                    err.println(command.getCommandName() + ": " + TerminalSanitizer.sanitizeLine(ore.getMessage()));
                    err.flush();
                    return 4;
                }
                RefusalRenderer.render(err, context.locale(), ore);
                err.flush();
                return ore.exitCode();
            }
            err.println(command.getCommandName() + ": " + TerminalSanitizer.sanitizeLine(exception.getMessage()));
            err.flush();
            return EXIT_FAILURE;
        });
        var code = commandLine.execute(selection.args().toArray(String[]::new));
        out.flush();
        err.flush();
        return code;
    }

    private static void applyResourceBundle(CommandLine commandLine, ResourceBundle bundle) {
        commandLine.setResourceBundle(bundle);
        for (var sub : commandLine.getSubcommands().values()) {
            applyResourceBundle(sub, bundle);
        }
    }

    /** Creates the commands with the process context. */
    private record Factory(OperatorContext context) implements CommandLine.IFactory {
        @Override
        public <K> K create(Class<K> type) throws Exception {
            try {
                var ctor = type.getDeclaredConstructor(OperatorContext.class);
                return ctor.newInstance(context);
            } catch (NoSuchMethodException _) {
                return CommandLine.defaultFactory().create(type);
            }
        }
    }
}
