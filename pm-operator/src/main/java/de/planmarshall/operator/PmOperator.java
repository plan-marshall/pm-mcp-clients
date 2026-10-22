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
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.PmVersion;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * The operator command line {@code pm-operator}: the verbs a human runs at a terminal.
 */
@Command(name = "pm-operator", mixinStandardHelpOptions = true, versionProvider = PmOperator.Version.class,
                    description = "Operator command line of PM-MCP.",
                    subcommands = {
                                        StatusCommand.class,
                                        PlansCommand.class,
                                        WebCommand.class,
                                        RuntimeCommand.class,
                                        DrainCommand.class,
                                        OnboardCommand.class,
                                        ConfigCommand.class,
                                        CredentialsCommand.class,
                                        SettingsCommand.class,
                                        ProjectCommand.class,
                                        ProjectsCommand.class,
                                        ClientCommand.class,
                                        PlanCommand.class,
                                        EpicCommand.class,
                                        AbortCommand.class,
                                        RecipeCommand.class,
                                        StartCommand.class,
                                        LessonSourceCommand.class,
                                        GitSourceCommand.class,
                                        QuestionsCommand.class,
                                        AnswerCommand.class,
                                        ReportsCommand.class,
                                        SystemCommand.class,
                                        CleanupCommand.class,
                                        EnrolCommand.class,
                                        EnrolHarnessCommand.class,
                                        RolesCommand.class,
                                        UnenrolCommand.class,
                                        AcceptEditCommand.class
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
        context.gate().verify();
        if (tui && runFullScreen()) {
            return 0;
        }
        return new PmConsole(context).run();
    }

    /**
     * Opens the overview directly on the terminal when the machine and the terminal allow it.
     *
     * @return {@code true} when the full-screen view ran
     * @throws IOException if the terminal cannot be opened or the view cannot be drawn
     */
    private boolean runFullScreen() throws IOException {
        try (var terminal = context.createTerminal()) {
            if (!TuiScreenDriver.canOpenFullScreen(context, terminal, TuiLayout.MIN_COLUMNS)) {
                return false;
            }
            var out = new PrintWriter(terminal.writer(), true);
            new OverviewFullScreenApp(context, terminal, null, null, false).run(out);
            return true;
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
        commandLine.setExecutionExceptionHandler((exception, command, parseResult) -> {
            if (exception instanceof OperatorSecurityException ose) {
                err.println(command.getCommandName() + ": " + ose.getMessage());
                err.flush();
                return ose.exitCode();
            }
            err.println(command.getCommandName() + ": " + exception.getMessage());
            err.flush();
            return EXIT_FAILURE;
        });
        var code = commandLine.execute(selection.args().toArray(String[]::new));
        out.flush();
        err.flush();
        return code;
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
