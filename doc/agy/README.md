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

## 2. Active Wave: Post-Run Remediation & Web Remediation

Post-run wave review under [`doc/meta/post_run_review.adoc`](../meta/post_run_review.adoc) identified four cross-cutting findings (CLI signal restoration, interactive entry flag, dynamic route codec validation closure, and mobile viewport E2E coverage) unified into one remediation plan, which has landed on `main` and passed Step 8 verification (PR #168). A second remediation plan for the web-client findings in [`doc/findings-web.txt`](../findings-web.txt) has landed and passed Step 8 verification (PR #173); its report is archived in `verified/`. Its follow-up fixes plan has landed and passed Step 8 verification (PR #175); its report is archived in `verified/`. A new plan addresses the live Vite host-allowlist rejection and the dev session secret that changes with the checkout.

| Stream | Active / Last Completed Plan | Isolated Worktree Directory | Topic Branch | State |
|---|---|---|---|---|
| **Post-Run Remediation** | [`post-run-remediation-plan.md`](implemented/post-run-remediation-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-post-run-remediation` | `feat/post-run-remediation` | Landed & Verified (PR #168) |
| **Web Remediation (Stream D)** | [`web-session-pairing-and-dev-host-plan.md`](implemented/web-session-pairing-and-dev-host-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-web-session-pairing-dev-host` | `fix/web-session-pairing-and-dev-host` | Landed & Verified (PR #173); [report](verified/web-session-pairing-and-dev-host-plan-report.md) |
| **Web Session Follow-Up Fixes** | [`web-session-follow-up-fixes-plan.md`](implemented/web-session-follow-up-fixes-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-web-session-follow-up-fixes` | `fix/web-session-follow-up-fixes` | Landed & Verified (PR #175); [report](verified/web-session-follow-up-fixes-plan-report.md) |
| **Web Dev Host & Session Persistence** | [`web-dev-host-and-session-persistence-plan.md`](web-dev-host-and-session-persistence-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-web-dev-host-and-session-persistence` | `fix/web-dev-host-and-session-persistence` | Step 1 allocated; ready to launch |
| **Workflow Visual Language & Icons** | [`workflow-visual-language-and-icons-plan.md`](workflow-visual-language-and-icons-plan.md) | `/Users/oliver/git/worktrees/pm-mcp-clients-workflow-visual-language` | `feat/workflow-visual-language` | Staged; schema sibling `pm-mcp-core`; default-units sibling blocked |

*(Note: The web dev host and session persistence plan and the workflow visual language plan are the open work packages. All previously planned client deliverables, full-screen TUI levels, and roadmap packages are complete. PR #176 separately aligned the stale Playwright specs and landed the onboarding-step, start-choice, and decomposition fixes; it has no separate `doc/agy` plan/report.)*

---

## 3. Sequential Execution Pipeline

As each active plan lands on `main` and passes Step 8 verification, subsequent plans are launched according to this dependency graph:

### Stream A: Core Contracts & Mock Runtime Remediation (`pm-api` / `pm-mock-runtime`)
1. **Foundation (Landed & Verified)**: [`mock-runtime-foundation-and-consolidation-plan.md`](implemented/mock-runtime-foundation-and-consolidation-plan.md) ([Report](verified/mock-runtime-foundation-and-consolidation-plan-report.md) — PR #125, commit `589bfaf`)
   *(Core engine, closed schema, scoped SSE replay, JSON-RPC `/mcp`, 5-scenario architecture)*
2. **Wave 2a (Landed & Verified)**: [`mock-runtime-onboarding-and-entry-plan.md`](implemented/mock-runtime-onboarding-and-entry-plan.md) ([Report](verified/mock-runtime-onboarding-and-entry-plan-report.md) — PR #131, commit `77f75eb`)
   *(Consolidated 23 scenario files into 80-state onboarding.json; full OperatorStateResource closure)*
3. **Wave 2b (Landed & Verified)**: [`mock-runtime-running-work-plan.md`](implemented/mock-runtime-running-work-plan.md) ([Report](verified/mock-runtime-running-work-plan-report.md) — PR #136, commit `49d0ef6`)
   *(Consolidated running-work, plan-view, decisions, and control files into rich running-work.json)*
4. **Fully Onboarded Showroom Scenario (Landed & Verified)**: [`mock-runtime-fully-onboarded-scenario-plan.md`](implemented/mock-runtime-fully-onboarded-scenario-plan.md) ([Report](verified/mock-runtime-fully-onboarded-scenario-plan-report.md) — PR #143, commit `8484347`)
   *(Omni-domain showroom scenario with all UI views, projects, running work, decisions, reports, system, settings, start, and designer active simultaneously)*
5. **Wave 3a Projects & Start (Landed & Verified)**: [`mock-runtime-projects-and-start-plan.md`](implemented/mock-runtime-projects-and-start-plan.md) ([Report](verified/mock-runtime-projects-and-start-plan-report.md) — PR #150, commit `50e9015`)
   *(Consolidated 20 project, enrolment, git/lesson source, and start files into projects-and-start.json; migrated StartRegistry)*
6. **Wave 3b Reports, System & Designer (Landed & Verified)**: [`mock-runtime-reports-system-designer-plan.md`](implemented/mock-runtime-reports-system-designer-plan.md) ([Report](verified/mock-runtime-reports-system-designer-plan-report.md) — PR #158, commit `2d87d77`)
   *(Consolidated reports, system, and designer scenarios into canonical scenarios 4 & 5; scenario-driven web test fetch)*
7. **Client Profile Schema (Landed & Verified)**: [`client-profile-schema-plan.md`](implemented/client-profile-schema-plan.md) ([Report](verified/client-profile-schema-plan-report.md) — PR #161 commit `725f62a`, PR #166 commit `a9e9122`)
   *(Full profile schema across 5 profiles; antigravity version probe and OpenCode {{mcp_config}} slot verified)*

### Stream B: CLI Full-Screen TUI (`pm-operator`)
1. **Foundation (Landed & Verified)**: [`cli-fullscreen-foundation-plan.md`](implemented/cli-fullscreen-foundation-plan.md) ([Report](verified/cli-fullscreen-foundation-plan-report.md) — PR #124, commit `e3ae1c0`)
   *(Terminal screen harness, raw mode safety, resize signal traps, notice-holding)*
2. **Overview (Landed & Verified)**: [`cli-fullscreen-overview-plan.md`](implemented/cli-fullscreen-overview-plan.md) ([Report](verified/cli-fullscreen-overview-plan-report.md) — PR #130, commit `ad06ef3`)
   *(Full-screen overview across projects, Level 1 Projects level, Level 2 Project detail by-plan & by-epic, Level 3 Ended work with pagination, OSC 8 PR links, live SSE updates, 0600 state persistence)*
3. **Plan View (Landed & Verified)**: [`cli-fullscreen-plan-view-plan.md`](implemented/cli-fullscreen-plan-view-plan.md) ([Report](verified/cli-fullscreen-plan-view-plan-report.md) — PR #137, commit `a057c1f`)
   *(Full-screen Plan View Level 3, 6-phase expandable tree, worker live activity plain-text stream, token split, and 1-key cross-level jumps)*
4. **Actions & Controlling (Landed & Verified)**: [`cli-fullscreen-actions-plan.md`](implemented/cli-fullscreen-actions-plan.md) ([Report](verified/cli-fullscreen-actions-plan-report.md) — PR #146, commit `735713b`)
   *(Full-screen epic view, modal actions menu, hold dialogue, settings conflict, findings/watches review, ended-work chat)*
5. **Projects (Landed & Verified)**: [`cli-fullscreen-projects-plan.md`](implemented/cli-fullscreen-projects-plan.md) ([Report](verified/cli-fullscreen-projects-plan-report.md) — PR #160, commit `beeea77`)
   *(Full-screen project explorer, enrol wizard, digest mismatch, git and lesson sources, interactive `--project ?` selector)*
6. **Reports & System (Landed & Verified)**: [`cli-fullscreen-reports-system-plan.md`](implemented/cli-fullscreen-reports-system-plan.md) ([Report](verified/cli-fullscreen-reports-system-plan-report.md) — PR #165, commit `07f2c87`)
   *(Full-screen reports browser and report view, system view, runtime lifecycle controls)*

### Stream C: Cross-Module Use Cases & Web Client Completion
1. **Foundation (Landed & Verified)**: [`web-foundation-routing-and-catalogue-plan.md`](implemented/web-foundation-routing-and-catalogue-plan.md) ([Report](verified/web-foundation-routing-and-catalogue-plan-report.md) — PR #126, commit `6b3defb`)
2. **Onboarding, Entry & Projects (Landed & Verified)**: [`web-onboarding-entry-and-projects-plan.md`](implemented/web-onboarding-entry-and-projects-plan.md) ([Report](verified/web-onboarding-entry-and-projects-plan-report.md) — PR #132, commit `cff749e`)
3. **Overview, Plan & Epic View (Landed & Verified)**: [`web-overview-plan-and-epic-view-plan.md`](implemented/web-overview-plan-and-epic-view-plan.md) ([Report](verified/web-overview-plan-and-epic-view-plan-report.md) — PR #134, commit `aad7e44`)
4. **Arrival Banners & i18n Cache (Landed & Verified)**: [`web-arrival-banners-and-i18n-cache-plan.md`](implemented/web-arrival-banners-and-i18n-cache-plan.md) ([Report](verified/web-arrival-banners-and-i18n-cache-plan-report.md) — PR #142, commit `8db8629`)
5. **Decisions & Actions (Landed & Verified)**: [`web-decisions-and-actions-plan.md`](implemented/web-decisions-and-actions-plan.md) ([Report](verified/web-decisions-and-actions-plan-report.md) — PR #151, commit `1090bbc`)
6. **Reports, System & Designer (Landed & Verified)**: [`web-reports-system-and-designer-plan.md`](implemented/web-reports-system-and-designer-plan.md) ([Report](verified/web-reports-system-and-designer-plan-report.md) — PR #154, commit `2e4ae7d`)
7. **Follow an Epic UC-PLN-5 (Landed & Verified)**: [`uc-pln-5-plan.md`](implemented/uc-pln-5-plan.md) ([Report](verified/uc-pln-5-plan-report.md) — PR #159, commit `84b6680`)
8. **Own Calls & Change Map (Landed & Verified)**: [`completion-own-calls-change-map-plan.md`](implemented/completion-own-calls-change-map-plan.md) ([Report](verified/completion-own-calls-change-map-plan-report.md) — PR #163, commit `673067f`)
   *(Completion report own calls and ASCII change map across API, mock runtime, operator and web app)*
   *(Complete removal of redundant top arrival notice banners; localStorage catalogue caching with SWR, synchronous hydration, and missing-key shielding)*
5. **Decisions & Actions (Landed & Verified)**: [`web-decisions-and-actions-plan.md`](implemented/web-decisions-and-actions-plan.md) ([Report](verified/web-decisions-and-actions-plan-report.md) — PR #151, commit `1090bbc`)
   *(Decisions tab count, notifications, one-step reveal, control actions bar, hold resolution, settings conflict dialogue, epic resource sections, findings/watches review, and ended-work chat)*
6. **Reports, System & Designer (Landed & Verified)**: [`web-reports-system-and-designer-plan.md`](implemented/web-reports-system-and-designer-plan.md) ([Report](verified/web-reports-system-and-designer-plan-report.md) — PR #154, commit `2e4ae7d`)
   *(Complete reports view, system view, cleanup inspection, and process designer in pm-web-app)*

### Stream D: Web Session, Pairing & Dev Host Remediation (`pm-web-app` / `pm-mock-runtime`)
1. **Landed & Verified (PR #173)**: [`web-session-pairing-and-dev-host-plan.md`](implemented/web-session-pairing-and-dev-host-plan.md) ([Report](verified/web-session-pairing-and-dev-host-plan-report.md) — Step 8 complete; three follow-ups recorded)
   *(Session persistence across mock restarts; authenticated-only `401` handling; system-name Vite/start-script host for phone access; already-paired code probe; fresh-code control endpoint and second-device pairing; single-use pairing copy; redundant-banner removal, clickable brand, action hover advisories, and discoverable process designer)*
2. **Follow-up fixes (Landed & Verified, PR #175)**: [`web-session-follow-up-fixes-plan.md`](implemented/web-session-follow-up-fixes-plan.md) ([Report](verified/web-session-follow-up-fixes-plan-report.md) — Step 8 complete)
   *(Loopback dev-host output, designer route/dirty-draft reconciliation, and removal of the unreachable pre-pairing status probe)*
3. **Dev host & session persistence (Staged)**: [`web-dev-host-and-session-persistence-plan.md`](web-dev-host-and-session-persistence-plan.md)
   *(Lower-case Vite allowed-host matching with curl proof, and one machine-wide dev device secret that survives `mvn clean` and worktree switches)*

### Stream E: Workflow Visual Language & Icons (`pm-mcp-core` / `pm-mcp-clients` / `plan-marshall-mcp`)
1. **Staged**: [`workflow-visual-language-and-icons-plan.md`](workflow-visual-language-and-icons-plan.md)
   *(Optional `icon` frontmatter field in the schema; visual specification; one distinct delivered icon per known workflow/phase; designer and phase-badge usage; shipped-unit `icon:` lines gated by the blocked `plan-marshall-mcp` default-units package)*

---

## 4. Archive References

- **Implemented Plans Archive**: [`doc/agy/implemented/README.md`](implemented/README.md)
- **Verified Completion Reports Archive**: [`doc/agy/verified/README.md`](verified/README.md)
