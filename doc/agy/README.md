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
| **Mock** | [`mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-mock-running-work` | `feat/mock-runtime-running-work` | Ready to launch (Wave 2b) |
| **CLI** | [`cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-overview` | `feat/cli-fullscreen-overview` | Active (PR #130) |
| **Web** | [`web-overview-plan-and-epic-view-plan.md`](web-overview-plan-and-epic-view-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-web-overview-plan-view` | `feat/web-overview-plan-and-epic-view` | Ready to launch (Wave 3) |

---

## 3. Sequential Execution Pipeline

As each active plan lands on `main` and passes Step 8 verification, subsequent plans are launched according to this dependency graph:

### Stream A: Mock Runtime Remediation (`pm-mock-runtime`)
1. **Foundation (Landed & Verified)**: [`mock-runtime-foundation-and-consolidation-plan.md`](implemented/mock-runtime-foundation-and-consolidation-plan.md) ([Report](verified/mock-runtime-foundation-and-consolidation-plan-report.md) — PR #125, commit `589bfaf`)
   *(Core engine, closed schema, scoped SSE replay, JSON-RPC `/mcp`, 5-scenario architecture)*
2. **Wave 2a (Landed & Verified)**: [`mock-runtime-onboarding-and-entry-plan.md`](implemented/mock-runtime-onboarding-and-entry-plan.md) ([Report](verified/mock-runtime-onboarding-and-entry-plan-report.md) — PR #131, commit `77f75eb`)
   *(Consolidated 23 scenario files into 80-state onboarding.json; full OperatorStateResource closure)*
3. **Wave 2b (Next Active)**: `mock-runtime-running-work-plan.md`
   *(Consolidates running-work, plan-view, decisions, and control files into rich running-work.json)*
4. **Wave 3**: `mock-runtime-projects-and-start-plan.md` & `mock-runtime-reports-system-designer-plan.md`
   *(Migrates projects, starting work, reports, and system views into standard scenarios)*

### Stream B: CLI Full-Screen TUI (`pm-operator`)
1. **Foundation (Landed & Verified)**: [`cli-fullscreen-foundation-plan.md`](implemented/cli-fullscreen-foundation-plan.md) ([Report](verified/cli-fullscreen-foundation-plan-report.md) — PR #124, commit `e3ae1c0`)
   *(Terminal screen harness, raw mode safety, resize signal traps, notice-holding)*
2. **Overview (Active in PR #130)**: `cli-fullscreen-overview-plan.md`
3. **Plan View**: `cli-fullscreen-plan-view-plan.md`
4. **Projects**: `cli-fullscreen-projects-plan.md`
5. **Reports & System**: `cli-fullscreen-reports-system-plan.md`
6. **Actions & Controlling**: `cli-fullscreen-actions-plan.md`

### Stream C: Web Client Completion (`pm-web-app`)
1. **Foundation (Landed & Verified)**: [`web-foundation-routing-and-catalogue-plan.md`](implemented/web-foundation-routing-and-catalogue-plan.md) ([Report](verified/web-foundation-routing-and-catalogue-plan-report.md) — PR #126, commit `6b3defb`)
   *(Design spec completion, full URL routing/history, elimination of fallbackMessages, retirement of WorkspaceView)*
2. **Onboarding, Entry & Projects (Landed & Verified)**: [`web-onboarding-entry-and-projects-plan.md`](implemented/web-onboarding-entry-and-projects-plan.md) ([Report](verified/web-onboarding-entry-and-projects-plan-report.md) — PR #132, commit `cff749e`)
   *(10-step project onboarding, build environment display, favourite ordering, setup conflict resolution)*
3. **Overview, Plan & Epic View (Next Active)**: `web-overview-plan-and-epic-view-plan.md`
4. **Decisions & Actions**: `web-decisions-and-actions-plan.md`
5. **Reports, System & Designer**: `web-reports-system-and-designer-plan.md`

---

## 4. Archive References

- **Implemented Plans Archive**: [`doc/agy/implemented/README.md`](implemented/README.md)
- **Verified Completion Reports Archive**: [`doc/agy/verified/README.md`](verified/README.md)
