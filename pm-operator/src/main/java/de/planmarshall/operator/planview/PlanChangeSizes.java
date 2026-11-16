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

    private PlanChangeSizes() {
    }

    /**
     * @param runtime the runtime client
     * @param planId  the plan
     * @return the size of change the plan carries, or {@code null} when it carries none
     */
    public static @Nullable ChangeSize sizeOf(RuntimeClient runtime, String planId) {
        try {
            var response = runtime.send(HttpRequest.get(
                                PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/process"));
            if (response.status() != 200) {
                return null;
            }
            return ProcessTree.fromJson(response.bodyText()).change();
        } catch (IOException | IllegalArgumentException _) {
            // The figures are the plan's; without them the list still shows the head.
            return null;
        }
    }
}
