# Landing Analysis: PLAN-120 — Terminal: an Unknown Running Count Is Unknown

epic: ui-improvements
workstream: WS-04
pr: #382 (https://github.com/plan-marshall/pm-mcp-clients/pull/382), merged as `c0db368d` (squash through the merge queue, 15 files, +441/-50, 2026-11-09; parent `d5439199`)

> Runbook lane (harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2080). Inbox message
> `rb-120-cli-unknown-running-count-001.md`. Checked against git, the PR (`ci pr comments`), SonarCloud and a read-only
> leaf; the key points re-read by the orchestrator at `c0db368d`. Analysed 2026-11-09.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 record: `running` unknown, never 0 | met | `EpicResource.java:59` `@Nullable Integer`; `ControlJson.optCount` `:124-144` (BigDecimal; absent, null, negative, fractional incl. `2.0000000000000001`, out of range → null); `toMap` omits it (`:122-124`); `ControlResourcesTest.RunningCount` |
| D1 renderers: `? of N` | met, one call site untested | `EpicViewFormatting` `:86-93`, `:111-135` (`UNKNOWN_COUNT`); `EpicCommandTest`/`EpicViewLevelTest` cover the head and `EpicViewLevel:450`; the wait text at `EpicCommand:191` has no test, so the spec's "each fails if its call site goes back to 0" is not met there |
| D1 Next plan asks (`scope_unknown`), sends nothing until answered | met (dialogue) | `WorkActionDialogue.java:297-316`: `running == null || running >= scope` → `askRaise` (`:307`), `raise_scope: true` only on the raise key (`:310`), scope+1 as `long`; null epic → `control.epic.next_waiting` (`:299`); `ControlDialoguesTest.workActionNextUnknownRunning*` |
| D1 evidence order `test-first` | not met (lane's own verdict) | evidence `mutation-after`; `deliverables_done=1` of 2 |
| D2 en/de texts pinned | met, with an extra key | `ControlCatalogueTest:113-127`, `EpicViewCatalogueTest:137-140`; unplanned key `control.epic.next_invalid` (re-prompt loop, `WorkActionDialogue:336-345`) pinned in `en` only (`ControlCatalogueTest:125`) |
| Added unplanned | `ControlJson.java` (justified: `optInteger` truncates); `control.epic.next_invalid` and the re-prompt (safer: only the raise key raises) | diff |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`); the first dispatch was refused for a malformed envelope (both `workflow` and `instructions`) and re-sent.
- **Verdict:** layer 1 findings — 1; layer 2 findings — 2; layer 3 findings — 1 (pre-existing, made visible by this landing's design sentence).
- **Re-read by the orchestrator at `c0db368d`:** `EpicCommand.java:396-428` (the `epic next` verb posts `raise_scope` from its flag, `--no-raise` declared and unused); `19-actions.adoc:188-190`; `WorkActionDialogue.java:297-312`; `EpicCommand.java:189-192`; `ControlCatalogueTest.java:61,125`. All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | The spec names only the dialogue's _Next plan_ as the asking path; the separate `epic next` verb, which the design says asks (`19-actions.adoc:190`), is not in it | checked | refutes | spec Objective/D1; `EpicCommand.java:396-428` |

**Layer 1 verdict:** findings — 1 (premises otherwise consistent with `work-actions.adoc:222`, `epic-view.adoc:82`)

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained / unborne | Verdict |
|-------------|-----------|--------------------|-----------------------|---------|
| D1 | `ControlJson:124-144`, `EpicViewFormatting:86-135`, `WorkActionDialogue:297-353` | `ControlResourcesTest`, `EpicCommandTest`, `EpicViewLevelTest`, `ControlDialoguesTest` | `EpicCommand:191` untested | checked |
| D2 | catalogues en/de | `ControlCatalogueTest`, `EpicViewCatalogueTest` | `next_invalid` de not pinned | checked |
| Evidence orders | — | — | not readable from a squash commit | not_shown_by_reading |

**Layer 2 verdict:** findings — 2

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 (pre-existing) | `EpicCommand.java:396-428`; `19-actions.adoc:188,190` | `pm-operator epic next <id>` with an unknown or full running count, or with `--no-raise`/`--no-input` | the verb never reads the epic and never asks; it posts `raise_scope` = the `--raise-scope` flag; `--no-raise` is ignored; the design sentence PLAN-120 added ("`epic next` asks") over-claims | minor |

**Layer 3 not shown by reading:** none of the changed code; EOF/Ctrl+C end the re-prompt loop through `StartPrompt.read` exceptions.

**Layer 3 verdict:** findings — 1 (outside the changed call site; the changed code reads sound)

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | no generic Exception | conforms | `ControlJson` catches `NumberFormatException`, `ArithmeticException` |
| CLAUDE.md § Code Standards | Generators for input data | deviates | new tests use literal ids and counts; `ControlDialoguesTest` without `@EnableGeneratorController` (pre-existing style) |
| CLAUDE.md § Code Standards | `@ParameterizedTest` for 3+ variants | deviates | `ControlResourcesTest.RunningCount`: 5 same-shape single-assert tests |
| junit-core review rules | no hard-coded mirror of a set defined elsewhere | deviates | `ControlCatalogueTest.requiredKeys` (pre-existing; the lane added two keys) |
| java-null-safety | `@Nullable`, no `Optional` components | conforms | `EpicResource.running`, `atOnce`, `waitText`, `askRaw`, `started` |

## Metrics and Anomalies

- Tokens: `unknown` (opencode measures none); `plugin_version` `unknown` → landing-check `complete: false` (recurrence of the opencode Open Defect).
- Anomalies: the routed `verify` hit the daemon bound (758 s) and was split; two `test` jobs re-run after the 300 s bound; `verify -Pcoverage -pl pm-api` branch coverage 0.72 (< 0.80), pre-existing per the lane, not checked here.

## Routing and Merge Behavior

- **Review — FALSE CLAIM.** The lane reports "CodeRabbit, 10 findings, all fixed or answered and all 10 threads resolved". Re-read from `ci pr comments`: 6 CodeRabbit review bodies (3+1+1+2+3+2 = 12 actionable), **12 threads, 2 unresolved and unanswered**, both from the review of the final head `176610d2` (11:23Z): (a) `ControlCatalogueTest.java:61` derive `requiredKeys` from the specification — valid, minor, pre-existing; (b) `WorkActionDialogue.java:307` "`epic next` sends `raise_scope: false` without asking" — refuted for the dialogue, valid for the `epic next` verb (L3-1). The lane's report also says the current head was reviewed "indirectly"; it was reviewed directly and left with two open findings. **Merge rule NOT met** (all review bodies answered). One review-body item on the scope wait answered (11:28Z). Sourcery: quota refusals throughout.
- **CI/merge:** green; merge queue on the operator's word; no conflicts.
- **Sonar:** `sonar_open_new_issues=0` corroborated: SonarCloud PR #382 0 issues, gate OK, new coverage 100%.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-120 --status shipped`
- [x] row `pr` stamped `382` (stamped at resume) — `queue --set-row`
- [x] row `landing` stamped — `queue --set-row PLAN-120 --field landing --value landings/PLAN-120.md`
- [x] row `plan_marshall_plan_id` stamped `rb-120-cli-unknown-running-count` (from the sender)
- [x] epic.md: Watch for PLAN-120 (false review count, two open threads, L3-1); Open Defect (opencode facts) recurrence
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- L3-1 and thread (b): the `epic next` verb asks on an unknown or full scope and honours `--raise-scope`/`--no-raise`/`--no-input` (or the `19-actions.adoc:190` sentence is softened) — reported to the operator; a fix plan only on the operator's word.
- Thread (a) and `next_invalid` de pin, `EpicCommand:191` test — minor; candidates for the same fix plan.
- Unblocks PLAN-123 together with PLAN-124 (still running).
