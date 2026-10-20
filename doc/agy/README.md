# Orchestrator Wave Index & Implementation Dashboard

This document provides the central, real-time dashboard for agent orchestration under [`doc/agy/runbook.md`](runbook.md) and [`doc/meta/verification-protocol.md`](../meta/verification-protocol.md).

It tracks active worktrees, the 3-stream parallel wave model, and the sequential execution order for all remaining implementation packages.

---

## 1. Governing Framework & The Four Pillars

Every work package in `doc/agy/` is governed by four coordinated architectural layers:
1. **Roadmap & Specifications (`doc/plans/`)**: Declares formal package boundaries and requirements.
2. **Technical Implementation Recipe (`doc/meta/implement_use-cases.md`)**: Governs the 5-module build flow, Jackson streaming constraints, and test standards.
3. **Operational Lifecycle & SOP (`doc/agy/runbook.md`)**: Governs worktree isolation, cognitive persona adoption, pre-commit vs. documentation distinction, PR creation (NEVER `skip-bot-review` on worker PRs), and automated bot triage.
4. **Post-Landing Verification Protocol (`doc/meta/verification-protocol.md`)**: Governs active, independent Step 8 verification across 4 pillars (Traceability Matrix, Anti-Pattern Scans, Subprocess Lifecycle Pre-Flight, Independent Diff Auditing).

---

## 2. Active Wave: 3-Stream Parallel Model

Waves execute across three modularly disjoint streams (1 Mock, 1 CLI, 1 Web):

| Stream | Active Plan | Isolated Worktree Directory | Topic Branch | State |
|---|---|---|---|---|
| **Mock** | [`mock-runtime-foundation-and-consolidation-plan.md`](implemented/mock-runtime-foundation-and-consolidation-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-mock-foundation` | `feat/mock-foundation` | Implementation complete (PR #125 in review) |
| **CLI** | [`cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-foundation` | `feat/cli-fullscreen-foundation` | Ready to execute (Step 1 complete) |
| **Web** | [`web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-web-foundation` | `feat/web-foundation-routing-and-catalogue` | Ready to execute (Step 1 complete) |

---

## 3. Sequential Execution Pipeline

As each active plan lands on `main` and passes Step 8 verification, subsequent plans are launched according to this dependency graph:

### Stream A: Mock Runtime Remediation (`pm-mock-runtime`)
1. **Foundation (In review, PR #125)**: [`mock-runtime-foundation-and-consolidation-plan.md`](implemented/mock-runtime-foundation-and-consolidation-plan.md)
   *(Core engine, closed schema, scoped SSE replay, JSON-RPC `/mcp`, 5-scenario architecture)*
2. **Wave 2**: `mock-runtime-onboarding-and-entry-plan.md` & `mock-runtime-running-work-plan.md`
   *(Migrates onboarding, paired devices, and running-work fixtures into standard scenarios)*
3. **Wave 3**: `mock-runtime-projects-and-start-plan.md` & `mock-runtime-reports-system-designer-plan.md`
   *(Migrates projects, starting work, reports, and system views into standard scenarios)*

### Stream B: CLI Full-Screen TUI (`pm-operator`)
1. **Foundation (Active)**: `cli-fullscreen-foundation-plan.md`
   *(Terminal screen harness, raw mode safety, resize signal traps, notice-holding)*
2. **Overview**: `cli-fullscreen-overview-plan.md`
3. **Plan View**: `cli-fullscreen-plan-view-plan.md`
4. **Projects**: `cli-fullscreen-projects-plan.md`
5. **Reports & System**: `cli-fullscreen-reports-system-plan.md`
6. **Actions & Controlling**: `cli-fullscreen-actions-plan.md`

### Stream C: Web Client Completion (`pm-web-app`)
1. **Foundation (Active)**: `web-foundation-routing-and-catalogue-plan.md`
   *(Design spec completion, full URL routing/history, elimination of fallbackMessages, retirement of WorkspaceView)*
2. **Onboarding, Entry & Projects**: `web-onboarding-entry-and-projects-plan.md`
3. **Overview, Plan & Epic View**: `web-overview-plan-and-epic-view-plan.md`
4. **Decisions & Actions**: `web-decisions-and-actions-plan.md`
5. **Reports, System & Designer**: `web-reports-system-and-designer-plan.md`

---

## 4. Archive References

- **Implemented Plans Archive**: [`doc/agy/implemented/README.md`](implemented/README.md)
- **Verified Completion Reports Archive**: [`doc/agy/verified/README.md`](verified/README.md)
