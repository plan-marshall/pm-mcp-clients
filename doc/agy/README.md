# Orchestrator Wave Index & Implementation Dashboard

This document provides the central, real-time dashboard for agent orchestration under [`doc/agy/runbook.md`](runbook.md), [`doc/meta/emit_rules.adoc`](../meta/emit_rules.adoc), and [`doc/meta/verification-protocol.adoc`](../meta/verification-protocol.adoc).

It tracks active worktrees, the 3-stream parallel wave model, and the sequential execution order for all remaining implementation packages.

---

## 1. Governing Framework & Architectural Layers

Every work package in `doc/agy/` is governed by coordinated architectural layers:
1. **Roadmap & Specifications (`doc/plans/`)**: Declares formal package boundaries and requirements.
2. **Task Emission & Worktree Orchestration (`doc/meta/emit_rules.adoc`)**: Governs Step 1 worktree setup and the canonical two-liner worker launch prompt standard (zero plan duplication).
3. **Technical Implementation Recipe (`doc/meta/implement_use-cases.adoc`)**: Governs the 5-module build flow, Jackson streaming constraints, and test standards.
4. **Operational Lifecycle & SOP (`doc/agy/runbook.md`)**: Governs worktree isolation, cognitive persona adoption, pre-commit vs. documentation distinction, PR creation (NEVER `skip-bot-review` on worker PRs), and automated bot triage.
5. **Post-Landing Verification Protocol (`doc/meta/verification-protocol.adoc`)**: Governs active, independent Step 8 verification across 4 pillars (Traceability Matrix, Anti-Pattern Scans, Subprocess Lifecycle Pre-Flight, Independent Diff Auditing).
6. **Post-Run Wave Review (`doc/meta/post_run_review.adoc`)**: Governs multi-plan reviews and de-collided remediation DAG synthesis.

---

## 2. Active Wave: 3-Stream Parallel Model

Waves execute across three modularly disjoint streams (1 Mock, 1 CLI, 1 Web):

| Stream | Active Plan | Isolated Worktree Directory | Topic Branch | State |
|---|---|---|---|---|
| **Mock** | [`mock-runtime-projects-and-start-plan.md`](mock-runtime-projects-and-start-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-mock-projects-start` | `feat/mock-projects-start` | Ready to launch |
| **CLI** | [`cli-fullscreen-actions-plan.md`](cli-fullscreen-actions-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-actions` | `feat/cli-fullscreen-actions` | Active implementation |
| **Web** | [`web-reports-system-and-designer-plan.md`](web-reports-system-and-designer-plan.md) | — | — | Awaiting mock prerequisite (`mock-runtime-reports-system-designer-plan.md`) |

---

## 3. Sequential Execution Pipeline

As each active plan lands on `main` and passes Step 8 verification, subsequent plans are launched according to this dependency graph:

### Stream A: Mock Runtime Remediation (`pm-mock-runtime`)
1. **Foundation (Landed & Verified)**: [`mock-runtime-foundation-and-consolidation-plan.md`](implemented/mock-runtime-foundation-and-consolidation-plan.md) ([Report](verified/mock-runtime-foundation-and-consolidation-plan-report.md) — PR #125, commit `589bfaf`)
   *(Core engine, closed schema, scoped SSE replay, JSON-RPC `/mcp`, 5-scenario architecture)*
2. **Wave 2a (Landed & Verified)**: [`mock-runtime-onboarding-and-entry-plan.md`](implemented/mock-runtime-onboarding-and-entry-plan.md) ([Report](verified/mock-runtime-onboarding-and-entry-plan-report.md) — PR #131, commit `77f75eb`)
   *(Consolidated 23 scenario files into 80-state onboarding.json; full OperatorStateResource closure)*
3. **Wave 2b (Landed & Verified)**: [`mock-runtime-running-work-plan.md`](implemented/mock-runtime-running-work-plan.md) ([Report](verified/mock-runtime-running-work-plan-report.md) — PR #136, commit `49d0ef6`)
   *(Consolidated running-work, plan-view, decisions, and control files into rich running-work.json)*
4. **Fully Onboarded Showroom Scenario (Landed & Verified)**: [`mock-runtime-fully-onboarded-scenario-plan.md`](implemented/mock-runtime-fully-onboarded-scenario-plan.md) ([Report](verified/mock-runtime-fully-onboarded-scenario-plan-report.md) — PR #143, commit `8484347`)
   *(Omni-domain showroom scenario with all UI views, projects, running work, decisions, reports, system, settings, start, and designer active simultaneously)*
5. **Wave 3 Consolidation (Active)**: [`mock-runtime-projects-and-start-plan.md`](mock-runtime-projects-and-start-plan.md) & [`mock-runtime-reports-system-designer-plan.md`](mock-runtime-reports-system-designer-plan.md)
   *(Migrates projects, starting work, reports, and system views into standard scenarios)*

### Stream B: CLI Full-Screen TUI (`pm-operator`)
1. **Foundation (Landed & Verified)**: [`cli-fullscreen-foundation-plan.md`](implemented/cli-fullscreen-foundation-plan.md) ([Report](verified/cli-fullscreen-foundation-plan-report.md) — PR #124, commit `e3ae1c0`)
   *(Terminal screen harness, raw mode safety, resize signal traps, notice-holding)*
2. **Overview (Landed & Verified)**: [`cli-fullscreen-overview-plan.md`](implemented/cli-fullscreen-overview-plan.md) ([Report](verified/cli-fullscreen-overview-plan-report.md) — PR #130, commit `ad06ef3`)
   *(Full-screen overview across projects, Level 1 Projects level, Level 2 Project detail by-plan & by-epic, Level 3 Ended work with pagination, OSC 8 PR links, live SSE updates, 0600 state persistence)*
3. **Plan View (Landed & Verified)**: [`cli-fullscreen-plan-view-plan.md`](implemented/cli-fullscreen-plan-view-plan.md) ([Report](verified/cli-fullscreen-plan-view-plan-report.md) — PR #137, commit `a057c1f`)
   *(Full-screen Plan View Level 3, 6-phase expandable tree, worker live activity plain-text stream, token split, and 1-key cross-level jumps)*
4. **Actions & Controlling (Active)**: [`cli-fullscreen-actions-plan.md`](cli-fullscreen-actions-plan.md)
5. **Projects**: `cli-fullscreen-projects-plan.md`
6. **Reports & System**: `cli-fullscreen-reports-system-plan.md`

### Stream C: Web Client Completion (`pm-web-app`)
1. **Foundation (Landed & Verified)**: [`web-foundation-routing-and-catalogue-plan.md`](implemented/web-foundation-routing-and-catalogue-plan.md) ([Report](verified/web-foundation-routing-and-catalogue-plan-report.md) — PR #126, commit `6b3defb`)
   *(Design spec completion, full URL routing/history, elimination of fallbackMessages, retirement of WorkspaceView)*
2. **Onboarding, Entry & Projects (Landed & Verified)**: [`web-onboarding-entry-and-projects-plan.md`](implemented/web-onboarding-entry-and-projects-plan.md) ([Report](verified/web-onboarding-entry-and-projects-plan-report.md) — PR #132, commit `cff749e`)
   *(10-step project onboarding, build environment display, favourite ordering, setup conflict resolution)*
3. **Overview, Plan & Epic View (Landed & Verified)**: [`web-overview-plan-and-epic-view-plan.md`](implemented/web-overview-plan-and-epic-view-plan.md) ([Report](verified/web-overview-plan-and-epic-view-plan-report.md) — PR #134, commit `aad7e44`)
4. **Arrival Banners & i18n Cache (Landed & Verified)**: [`web-arrival-banners-and-i18n-cache-plan.md`](implemented/web-arrival-banners-and-i18n-cache-plan.md) ([Report](verified/web-arrival-banners-and-i18n-cache-plan-report.md) — PR #142, commit `8db8629`)
   *(Complete removal of redundant top arrival notice banners; localStorage catalogue caching with SWR, synchronous hydration, and missing-key shielding)*
5. **Decisions & Actions (Landed & Verified)**: [`web-decisions-and-actions-plan.md`](implemented/web-decisions-and-actions-plan.md) ([Report](verified/web-decisions-and-actions-plan-report.md) — PR #151, commit `1090bbc`)
   *(Decisions tab count, notifications, one-step reveal, control actions bar, hold resolution, settings conflict dialogue, epic resource sections, findings/watches review, and ended-work chat)*
6. **Reports, System & Designer (Pending Mock Prerequisite)**: `web-reports-system-and-designer-plan.md`

---

## 4. Archive References

- **Implemented Plans Archive**: [`doc/agy/implemented/README.md`](implemented/README.md)
- **Verified Completion Reports Archive**: [`doc/agy/verified/README.md`](verified/README.md)
