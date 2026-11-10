# Landing Analysis: PLAN-36 — Terminal: "Needs You" in Every Surface

epic: ui-improvements
workstream: WS-05
pr: #391 (https://github.com/plan-marshall/pm-mcp-clients/pull/391), merged as `36fb27d9` (squash through the merge queue, 2026-11-09; parent `06d155c3`; 27 files, +1568/-61)

> Runbook lane (harness antigravity, model unknown, executor 0.1.2090). Inbox message `rb-36-cli-needs-verb-001.md`.
> Checked against git, the PR (`ci pr comments`), SonarCloud and a read-only leaf; key points re-read by the
> orchestrator at `36fb27d9`. Analysed 2026-11-10. **No review bot read this code** — the leaf's reading is its only review.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence |
|-------------|---------|----------|
| D1 `needs` verb lists every item with kind, subject, blocks, since, command | met in shape, defects | `NeedsCommand.java:64-100`, registered, `!` aliased in the console (`PmConsole.java:387`); page 1 only (no `Show more`, `has_older` ignored — design `needs-and-find.adoc` says never a silent cut); subject is the raw id, not the title; `blocks` a wire token; commands partly invalid (L3-2) |
| D1 test that can fail | weak | `NeedsCommandTest` asserts bare lowercase substrings that also match the command column; `needsEmpty` ends in `|| !output.isBlank()` (cannot fail); the "title fallback" test runs through the catalogue (`fr` → `en`) |
| D2 overview "Needs you" group and `!` | met as written, root path untested | `ProjectsOverviewLevel.java:164-190`, `OverviewFullScreenApp.java:1201-1250`; `NeedsIT` starts at a project level, not the overview root; `openNeedsItemAct` … `openPlanAct` (`:1341-1420`) untested; second IT test accepts staying on the row |
| D3 one count across surfaces | not met as meant | `NeedsOneCountTest.sameCountsAcrossFourSurfaces` compares totals only (12): the page has one item per kind, `status.needs_operator` is 6 decisions + 2 waivers + 4 others — same total, different composition; the overview check is a tautology over the page just fetched; `status`/`questions` production code unchanged; the mock was changed (`mergeMachineNeeds`, `MockRuntimeEngine.java:5688-5705`) to make the totals meet, deriving counts from a second fixture list against its own javadoc (`:5960-5974`) |
| D3 badges and notices for `credential`, `cleanup_due` | badge met; notices defective | `ConsoleBadge.java:98-117` words en/de; `ConsoleBadge.command()` has no production caller (tested only by a coverage test) and returns `onboarding`, not a verb; notices see L3-1 |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`).
- **Verdict:** layer 1 findings — 1 major (spec D3 "notices" vs `EventKind`'s "badge source, not a notice", unresolved), 5 minor; layer 2 — 7 major/minor; layer 3 — 4 major, 10 minor.
- **Re-read by the orchestrator at `36fb27d9`:** `ConsoleNotice.java:388-392` (`needs_operator.changed` with any `kind` ≠ `none` → `EventKind.fromType(kind)`); `EventKind.java:30` ("the prompt badge's source and not a notice"); `OverviewFormatting.java:497-501` (`"plan hold " + planId`); `PlanCommand` subcommands (no `hold`). All confirmed.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ConsoleNotice.java:388-392`; `:191-201`, `:582-592` | any `needs_operator.changed` carrying a `kind` (e.g. a plan-level `kind: decision`) | becomes a notice: `Q-???` question notices, duplicates of `hold.opened`, a bell on every count change incl. decreases; credential notice "Credential unknown needs attention" (English literal, scope unsanitised, a generic `key` member read as scope); contradicts `EventKind.java:30` and the design ("a notice for each item a change ADDS") | major (regression) |
| L3-2 | `OverviewFormatting.java:497-501`; `OverviewFullScreenApp.java:1384` | the fixture's `landing_failed` item with a plan | `needs` prints `plan hold drop-legacy-api` and `!` runs it — no such verb (unmatched argument); the design gives `epic <id>` for `landing_failed` and `held`; three different commands for one item across `needs`, overview and `!` | major |
| L3-3 | `OverviewFormatting.java:399-436,457`; `NeedsCommand.java:79-91,108-114`; `ProjectsOverviewLevel.java:168-170` | any row | raw ids instead of titles (`ProjectsOverviewLevelTest` pins `plan-1`); `blocks: plan` untranslated (no `common.kind.*`); English literals `machine`, `cleanup`, `credential`, `blocks no work`, `blocks: `, `waits since `, `Needs you · `, `! ` — dead `hasKey` fallbacks pinned by tests | major |
| L3-4 | `ProjectsOverviewLevel.java:189`; `OverviewFullScreenApp.java:1341` | an item arrives above the selection | rows keyed `needs:<index>`; the selection moves to another item, `!`/Enter opens the wrong decision or hold (design: selection follows the item's link) | major |
| L3-5 | `NeedsCommand.java:104-164` | several decisions on one plan | own join of `/api/v1/decisions` (last wins; design: no client joins a store of its own); overview shows bare `answer` for the same item | minor |
| L3-6 | `OverviewFullScreenApp.java:475-484`, `:1277-1297` | failed needs read; refused hold read | stale group kept; refusal no longer shown; a plan with hold and decision never reaches the decision | minor |
| L3-7 | `NeedsOperatorKind` / `NeedsCommand.java:74-80` | one new runtime kind | `fromMap` throws, the whole list fails | minor |
| L3-8 | `TuiKeyRegistry.java:582`; `TuiLayoutTest`, `TuiCapabilityIT.java:242` | 80×24 overview | `? help` pushed out of the footer; two existing assertions loosened to accept `? more` | minor |
| L3-9 | `pm-api/.../EventKind.java:89,92` | — | invented wire names `credential.needed`, `cleanup.due` in the shared contract | minor |
| L3-10 | `NeedsCommand.java:108-114` fixed columns; unused keys `overview.needs.*` | — | misaligned columns, command truncated at 80; dead catalogue keys | minor |

### Conformance

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md | Generators + `@EnableGeneratorController` | deviates | `NeedsCommandTest` none; `NeedsOneCountTest` annotated, no Generators; literals throughout |
| CLAUDE.md | `@ParameterizedTest` for 3+ variants | deviates | `consoleBadgeCommandExhaustive` (6), `needsItemFormattingExhaustive` (~15), notices (4) |
| CLAUDE.md | every operator text through the catalogue | deviates | L3-3, `ConsoleNotice:194` |
| junit-core | no vacuous assertions | deviates | `needsEmpty`, `NeedsIT` test 2, `TuiLayoutTest` OR, coverage of dead `ConsoleBadge.command()` |
| CLAUDE.md | no generic Exception; no System.out | conforms | eight swallowed `IOException` "best effort" catches |
| Spec | the client sorts nothing, derives no list | deviates (minor) | decision join, page 1 only |

## Metrics and Anomalies

- Tokens/model/effort: `unknown` (antigravity) → landing-check `complete: false`.
- Sonar coverage raised 70.6% → 89.9% with tests of dead and fallback paths.
- The lane's two operator questions removed the stray `messages/` directory and discarded pre-commit rewrites after merge.

## Routing and Merge Behavior

- **Review (re-read from `ci pr comments`):** 0 threads, 0 review bodies. CodeRabbit refused every one of seven triggers on quota; Sourcery refused on budget. **NO bot review of any head.** The lane merged under "standing review authorization". **Merge rule NOT met** (it needs one CodeRabbit review of some head); the lane's "all review surfaces clear" is true only because nothing was reviewed.
- **Sonar:** PR #391 0 issues, gate OK.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 391; `landing`; `plan_marshall_plan_id` `rb-36-cli-needs-verb`
- [x] epic.md Watch for PLAN-36; Open Defect: a lane merged with no bot review at all
- [x] message archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- Residue plan, high priority (L3-1 is a regression on every console): notices only for items a change ADDS (or none,
  per `EventKind`; resolve with the design), `epic <id>` for `landing_failed`/`held`, titles and catalogue words
  (`common.kind.*`), link-keyed selection, paging with `Show more`, one count per kind with a test that compares per
  kind, the mock's second derivation removed, the weak/vacuous tests, `? help` in the 80-column footer.
- Unblocks the chain PLAN-42/PLAN-50 (and PLAN-44 with PLAN-123).
