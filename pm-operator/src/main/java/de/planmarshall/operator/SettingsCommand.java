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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.ConsentKind;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.MachineSettingsUpdateRequest;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator settings}: inspects and changes the machine's settings (UC-ONB-10).
 *
 * <p>A change is sent against the revision the settings were read at; a change made against an
 * older revision is refused with {@code conflict}, the current value is shown, and {@code --yes}
 * applies the operator's change again on top of it.
 */
@Command(name = "settings", description = "Inspects and changes the machine's settings.",
                    subcommands = {SettingsCommand.MachineCommand.class})
@OperatorVerb
public final class SettingsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    SettingsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "machine", description = "Shows or changes the machine's settings.")
    @OperatorVerb
    static final class MachineCommand implements Callable<Integer> {

        static final String MACHINE_PATH = "/api/v1/machine/settings";

        @Spec
        CommandSpec spec = CommandSpec.create();

        private final OperatorContext context;

        @Option(names = "--set", description = "Set key=value (e.g. documents_mode=copy)")
        @Nullable
        List<String> set;

        @Option(names = "--withdraw", description = "Withdraw a remembered consent by its kind")
        @Nullable
        String withdraw;

        @Option(names = "--yes", description = "Keep going through a conflict with the current value")
        boolean yes;

        MachineCommand(OperatorContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var settings = readSettings(out);
            if (settings == null) {
                return 1;
            }
            printOverview(out, settings);

            int revision = settings.revision();
            if (withdraw != null && !withdraw.isBlank()) {
                var kind = ConsentKind.fromWire(withdraw);
                if (kind == null) {
                    out.println(MessageCatalogue.format("operator.onboard.refused",
                                        Map.of("reason", "unknown consent kind")));
                    out.flush();
                    return 1;
                }
                revision = apply(out, MachineSettingsUpdateRequest.withdrawConsent(kind, revision), revision);
            }
            if (set != null) {
                for (var pair : set) {
                    var eq = pair.indexOf('=');
                    if (eq <= 0) {
                        out.println(MessageCatalogue.format("operator.onboard.refused",
                                            Map.of("reason", "expected key=value")));
                        out.flush();
                        return 1;
                    }
                    var request = new MachineSettingsUpdateRequest(pair.substring(0, eq),
                                        pair.substring(eq + 1), revision);
                    revision = apply(out, request, revision);
                }
            }
            out.flush();
            return 0;
        }

        private @Nullable MachineSettingsRecord readSettings(PrintWriter out) throws IOException {
            var response = context.runtime(true).send(HttpRequest.get(MACHINE_PATH));
            if (response.status() != 200) {
                out.println(MessageCatalogue.format("operator.onboard.refused",
                                    Map.of("reason", "HTTP " + response.status())));
                out.flush();
                return null;
            }
            try {
                return MachineSettingsRecord.fromJson(response.bodyText());
            } catch (IOException _) {
                out.println(MessageCatalogue.format("operator.onboard.refused",
                                    Map.of("reason", "malformed settings")));
                out.flush();
                return null;
            }
        }

        private void printOverview(PrintWriter out, MachineSettingsRecord settings) {
            var locale = context.clientEnvironment().locale();
            out.println(MessageCatalogue.format(locale, "operator.onboard.settings.title", Map.of()));
            out.println("  " + MessageCatalogue.format(locale, "operator.onboard.settings.revision",
                                Map.of("revision", settings.revision())));
            for (var aspect : settings.aspects()) {
                var name = MessageCatalogue.format(locale, aspect.aspect().stepNameKey(), Map.of());
                out.printf("  %d. %s: %s%n", aspect.aspect().step(), name, aspect.summary());
            }
            if (settings.consents().isEmpty()) {
                out.println("  " + MessageCatalogue.get("web.settings.consents.none"));
            } else {
                out.println("  " + MessageCatalogue.format(locale, "web.settings.consents.title", Map.of()));
                for (var consent : settings.consents()) {
                    out.printf("    %s%n", MessageCatalogue.format(locale, consent.titleKey(), Map.of()));
                }
            }
        }

        private int apply(PrintWriter out, MachineSettingsUpdateRequest request, int revision)
                            throws IOException {
            var response = context.runtime(true).send(
                                HttpRequest.json("PUT", MACHINE_PATH, request.toJson()));
            if (response.status() == 200) {
                var result = MachineSettingsUpdateResult.fromJson(response.bodyText());
                if (request.key().startsWith("consent:")) {
                    out.println("  " + MessageCatalogue.get("operator.onboard.settings.consent_withdrawn"));
                } else {
                    out.println("  " + MessageCatalogue.get("operator.onboard.settings.saved"));
                }
                out.flush();
                return result.revision();
            }
            if (response.status() == 409) {
                var conflict = MachineSettingsUpdateResult.fromJson(response.bodyText());
                out.println("  " + MessageCatalogue.format("operator.onboard.settings.conflict",
                                    Map.of("value", Objects.toString(conflict.currentValue(), ""))));
                out.flush();
                if (yes) {
                    var retry = new MachineSettingsUpdateRequest(request.key(), request.value(),
                                        conflict.revision());
                    return apply(out, retry, conflict.revision());
                }
                return revision;
            }
            out.println(MessageCatalogue.format("operator.onboard.refused",
                                Map.of("reason", "HTTP " + response.status())));
            out.flush();
            return revision;
        }
    }
}
