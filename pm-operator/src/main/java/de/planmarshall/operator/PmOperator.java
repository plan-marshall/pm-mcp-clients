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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.PmVersion;

import picocli.CommandLine;
import picocli.CommandLine.Command;

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
                                        QuestionsCommand.class,
                                        AnswerCommand.class,
                                        ReportsCommand.class,
                                        SystemCommand.class,
                                        CleanupCommand.class
                    })
@OperatorVerb
public final class PmOperator implements Callable<Integer> {

    /** Exit code of a failed verb. */
    static final int EXIT_FAILURE = 1;

    private final @Nullable OperatorContext context;

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
        if (context != null) {
            context.gate().verify();
            return new PmConsole(context).run();
        }
        return 0;
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
    static int execute(OperatorContext context, PrintWriter out, PrintWriter err, String... args) {
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
        var code = commandLine.execute(args);
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
