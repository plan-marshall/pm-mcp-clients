/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import java.io.IOException;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.overview.ChangeSize;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.OperatorUrlUtil;

/**
 * The plan's figures, read where a client shows the changes: the size of change of the plan's last
 * capture comes from the plan's process, never from a sum over the rows a client holds
 * ({@code doc/design/shared/plan-view.adoc} § The Changes).
 */
public final class PlanChangeSizes {

    private static final String PLANS_API = "/api/v1/plans/";

    /**
     * What a read of the plan's figures answered.
     *
     * @param landed {@code true} when the plan's process was read, so {@code size} is the truth even
     *               when it is {@code null}
     * @param size   the size of change the plan carries, or {@code null} when it carries none
     */
    public record Probe(boolean landed, @Nullable ChangeSize size) {

        /** A read that did not land: the last figures stay the truth. */
        public static final Probe NOT_LANDED = new Probe(false, null);

        /**
         * @param size the size the plan carries, or {@code null} when it carries none
         * @return a read that landed
         */
        public static Probe of(@Nullable ChangeSize size) {
            return new Probe(true, size);
        }
    }

    private PlanChangeSizes() {
    }

    /**
     * @param runtime the runtime client
     * @param planId  the plan
     * @return the size of change the plan carries, or {@code null} when it carries none or the read
     *         did not land
     */
    public static @Nullable ChangeSize sizeOf(RuntimeClient runtime, String planId) {
        return probe(runtime, planId).size();
    }

    /**
     * @param runtime the runtime client
     * @param planId  the plan
     * @return the probe: whether the process was read, and the size it names
     */
    public static Probe probe(RuntimeClient runtime, String planId) {
        try {
            var response = runtime.send(HttpRequest.get(
                                PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/process"));
            if (response.status() != 200) {
                return Probe.NOT_LANDED;
            }
            return Probe.of(ProcessTree.fromJson(response.bodyText()).change());
        } catch (IOException | IllegalArgumentException _) {
            // The figures are the plan's; without them the list still shows the head.
            return Probe.NOT_LANDED;
        }
    }
}
