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

import java.util.List;
import java.util.Set;

/**
 * Which typed console line is an operator act (PM-SEC-3, CLI-CC-S-02).
 *
 * <p>The console is entered without the operator gate, so its dispatcher asks this class before it
 * runs a verb the operator typed and checks the gate for an act. A read-only verb is not an act;
 * neither is a verb whose act lives in a subcommand that reads. The act verbs themselves verify the
 * gate before they send anything, so a verb this class under-reports still refuses at the command.
 */
final class OperatorActs {

    private static final String FINDINGS = "findings";

    private static final String WATCHES = "watches";

    private static final String REPORT = "report";

    private static final String DEVICES = "devices";

    private static final String LIST = "list";

    private static final String CHAT = "chat";

    private static final String CANCEL = "cancel";

    private static final String ADD = "add";

    private static final String START = "start";

    private static final String SETTINGS = "settings";

    private static final String WEB = "web";

    private static final Set<String> PLAN_SUBCOMMANDS = Set.of(
                        ADD, "pause", START, FINDINGS, WATCHES, CANCEL, "abandon", "resolve",
                        SETTINGS, CHAT, REPORT);

    private static final Set<String> PLAN_READ_SUBCOMMANDS = Set.of(FINDINGS, WATCHES, REPORT);

    private static final Set<String> EPIC_SUBCOMMANDS = Set.of(
                        ADD, "next", "auto", "decompose", "review", "instruct", "archive", FINDINGS,
                        WATCHES, CHAT, CANCEL);

    private static final Set<String> EPIC_READ_SUBCOMMANDS = Set.of(FINDINGS, WATCHES);

    private static final Set<String> PROJECT_ACT_SUBCOMMANDS = Set.of(
                        ADD, "setup", "favourite", "release", "unenrol");

    private static final Set<String> CLIENT_ACT_SUBCOMMANDS = Set.of("install", "remove", "update");

    private static final Set<String> WEB_READ_ACTIONS = Set.of(LIST);

    private static final Set<String> CONFIG_READ_SUBCOMMANDS = Set.of("show");

    private static final Set<String> ACT_VERBS = Set.of(
                        "answer", "cleanup", "enrol", "unenrol", "enrol-harness", "accept-edit", "onboard",
                        "abort", "recipe", START, WEB, "git-source");

    private OperatorActs() {
        // Utility class
    }

    /**
     * @param line the line the operator typed at the console prompt
     * @return {@code true} when the line dispatches an act
     */
    static boolean isAct(String line) {
        var args = PmConsole.parseArgs(line);
        if (args.isEmpty()) {
            return false;
        }
        var verb = args.get(0);
        var rest = args.subList(1, args.size());
        return switch (verb) {
            case "plan" -> planAct(rest);
            case "epic" -> epicAct(rest);
            case "config" -> !rest.isEmpty() && !CONFIG_READ_SUBCOMMANDS.contains(rest.get(0));
            case SETTINGS -> settingsAct(rest);
            case "credentials" -> !rest.isEmpty() && !"status".equals(rest.get(0));
            case "roles" -> !rest.isEmpty() && "qualify".equals(rest.get(0));
            case "runtime" -> runtimeAct(rest);
            case "reports" -> !rest.isEmpty() && !rest.get(0).startsWith("-");
            case "project" -> !rest.isEmpty() && PROJECT_ACT_SUBCOMMANDS.contains(rest.get(0));
            case "client" -> !rest.isEmpty() && CLIENT_ACT_SUBCOMMANDS.contains(rest.get(0));
            case WEB -> webAct(rest);
            default -> ACT_VERBS.contains(verb);
        };
    }

    private static boolean webAct(List<String> rest) {
        if (rest.isEmpty() || !DEVICES.equals(rest.get(0))) {
            return true;
        }
        return rest.size() > 1 && !WEB_READ_ACTIONS.contains(rest.get(1));
    }

    private static boolean planAct(List<String> rest) {
        if (rest.isEmpty() || !PLAN_SUBCOMMANDS.contains(rest.get(0))) {
            return false;
        }
        var subcommand = rest.get(0);
        if (PLAN_READ_SUBCOMMANDS.contains(subcommand)) {
            return false;
        }
        if ("settings".equals(subcommand)) {
            return hasAny(rest, "--set", "--remove");
        }
        return true;
    }

    private static boolean epicAct(List<String> rest) {
        return !rest.isEmpty() && EPIC_SUBCOMMANDS.contains(rest.get(0))
                            && !EPIC_READ_SUBCOMMANDS.contains(rest.get(0));
    }

    private static boolean settingsAct(List<String> rest) {
        return !rest.isEmpty() && "machine".equals(rest.get(0)) && hasAny(rest, "--set", "--withdraw");
    }

    private static boolean runtimeAct(List<String> rest) {
        if (rest.isEmpty()) {
            return false;
        }
        var subcommand = rest.get(0);
        if ("upgrade".equals(subcommand)) {
            return !hasAny(rest, "--check", "--notes");
        }
        return "stop".equals(subcommand) || "restart".equals(subcommand);
    }

    private static boolean hasAny(List<String> args, String... flags) {
        for (var flag : flags) {
            if (args.contains(flag)) {
                return true;
            }
        }
        return false;
    }
}
