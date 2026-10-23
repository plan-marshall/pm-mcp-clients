#!/usr/bin/env bash
#
# Copyright © 2026-present Oliver Wolff
#
# SPDX-License-Identifier: LicenseRef-Proprietary
#
# All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
# No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
# the root of this repository.
#
# Runs the security audit's fixes and then restores the SvelteKit-generated `$app` configuration.
#
# `npm audit fix` prunes `node_modules`, and npm treats the generated `node_modules/$app`
# directory as an extraneous package. It disappears even when the audit found nothing to change,
# which removes the `tsconfig` the project extends. The prepare run is unconditional, so a failed
# audit still leaves the generated configuration in place; the audit's exit status is preserved.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.." || exit 1

npm audit fix
audit_status=$?

npm run prepare
prepare_status=$?

if [ "$audit_status" -ne 0 ]; then
    exit "$audit_status"
fi
exit "$prepare_status"
