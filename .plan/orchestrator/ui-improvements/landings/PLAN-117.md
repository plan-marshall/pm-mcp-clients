# Landing Analysis: PLAN-117 — Web: Acts Fourth Residue

epic: ui-improvements
workstream: WS-03
pr: #370 (https://github.com/plan-marshall/pm-mcp-clients/pull/370), merged as `bf24091c` (squash, 8 files, +109/-12; merge head `23ba7ff4`; branch commits `dd957aa6` D1, `cd870271` D2, `23ba7ff4` D3)

> Landing record for one shipped plan. Written by the `analyze` verb (inbox scan, message
> `rb-117-web-acts-fourth-residue-001.md`, 2026-11-08) after checking the lane's claims against git, the PRs
> (read through the `ci` abstraction) and Sonar. The lane's report is a lead, never a fact.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Two review fixes pinned by tests that fail on a revert | shipped | The production fixes already existed at the parent (`SystemView.svelte:281,289`, `+layout.svelte:160,162`, `EpicView.svelte:609`, `WorkControlPanel.svelte:132`); `dd957aa6` adds tests only. Each named line, reverted, fails its test: `SystemView.test.ts:545`, `layout.test.ts:288`, `EpicView.test.ts:635`, `WorkControlPanel.test.ts:996`. Gaps: (a) the test checks against a test-local copy of the `+layout.svelte:92` guard, depends on microtask order, and leaves the restart branch `:289` unpinned |
| D2 The unknown running count: checked against the daemon, stated in the design | shipped-modified | The design states `? of N at once` (`epic-view.adoc:82`) and the unknown prompt (`work-actions.adoc:222`). The daemon check was reported "not checkable … outside repository root", with no owner named, although the sibling repositories are checked out. The orchestrator ran the check itself (layer 3) |
| D3 The replies and the wording | shipped | "Epik" is gone from every `pm-api` message catalogue (`git grep` at `bf24091c`); `epic_view.de.properties:32,33,35` read "Epic". The reply on PR 331 is per the lane (not re-read) |

All 8 changed files are in the Expected Surface. `SystemView.svelte` and `+layout.svelte` are listed but untouched, which is right because their fixes already existed.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`). Reason: every full ship gets its own leaf.
- **Verdict:**
  - layer 1: minor gaps;
  - layer 2: findings;
  - layer 3: one major finding: the terminal client reads an unknown running count as 0.
- **Re-read by the orchestrator:**
  - at `bf24091c`: `EpicResource.java:55-60,160-166,185-191` (`int running`; `integer()` returns 0 when the field is absent); `WorkActionDialogue.java:291-295` (`epic.running() >= epic.scope()`); `epic-view.adoc:82`; `work-actions.adoc:222`; `git grep scope_unknown` under `src/test` (no match);
  - outside the repository: `plan-marshall-documentation` `doc/specification/runtime-model/01-processes-and-transport.adoc:346` (the `GET /api/v1/epics/{epic_id}` header lists title, state, automatic mode, `parallelization_scope`, `needs_operator` and project, with no running count); `plan-marshall-mcp` `origin/main` (`ad5f608`), where no production `"running"` literal exists.

  All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | HYPOTHESIS: `SystemView.svelte:281,289` and the stop-now success path are pinned by no test; the design never says when the count is unknown; "Epik" stands in `epic_view.de.properties:32,33,35` | checked | confirms | at `bf24091c^`: `layout.test.ts:246-290` asserted only the banner; `epic-view.adoc:47` had no rule; the Epik lines exist |
| L1-2 | The spec treats a shared design (`doc/design/shared/`, binding both clients) as web-only: no `pm-api` or `pm-operator` file is in its surface | checked | refutes (gap) | the Expected Surface |
| L1-3 | D2 allows `not checkable` only "if the repositories are not checked out"; they are | checked | refutes | the spec's D2 Done-when |

**Layer 1 verdict:** findings — 3 minor; the premises hold.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1(a) epoch | `SystemView.svelte:281` | `SystemView.test.ts:518-545` | it checks a test-local copy of the guard (`:530-535`); removing the production guard `+layout.svelte:92` stays green; it depends on microtask order (`:537-545`); the restart branch `:289` is unpinned | checked |
| D1(b) `isStopped` | `+layout.svelte:162` | `layout.test.ts:288,290` | `isInitialized: false` was added to 12 more draining tests that do not need it (harmless) | checked |
| D1(c) unknown `running` | `EpicView.svelte:609` | `EpicView.test.ts:635` | the lane quotes `:620` (wrong line) | checked |
| D1(d) epic subject | `WorkControlPanel.svelte:132` | `WorkControlPanel.test.ts:996` | the remaining `byState.test.ts:210` comparison mirrors the implementation; the injected-`decision` block (`:212-229`) is what discriminates | checked |
| D2 design | `epic-view.adoc:82`, `work-actions.adoc:222` | — | the key table row now holds prose that is not the catalogue value (`overview.*.properties:123`); `work-actions.adoc:222` copies the German text of `work-actions-texts.adoc:193-195` (a second source) | checked |
| PR answer to nitpick 1 | — | — | "German keys covered by the `pm-api` catalogue tests" is only partly true: `EpicViewCatalogueTest:134-136` formats `2 von 3 gleichzeitig` only, never `? von N`; `control.epic.scope_unknown` is named by no test (only the en/de key-set equality, `ControlCatalogueTest:36`) | checked |
| Lane "Residue: None" | — | — | hides the terminal-client gap that the two CodeRabbit threads named | checked |
| Mutation-after failing runs at `dd957aa6` | — | — | `dd957aa6` changes tests only; a failing run needs a mutated tree, so it cannot be shown | not_shown_by_reading |

**Layer 2 verdict:** findings — D1(a) weak; D2 overstated (the daemon check was feasible, no owner named); the nitpick answer is partly untrue; the German unknown form is untested; residue was misreported.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `pm-api` `EpicResource.java:59,164,188-191`; `pm-operator` `EpicViewFormatting.java:86-89` (callers `EpicCommand.java:155`, `epicview/EpicViewLevel.java:629`); `WorkActionDialogue.java:292` | an epic resource without `running`, or with `running: null`. That is the specified shape: the API spec's epic header carries no running count, and the daemon has no production `running` field | the terminal shows `0 of N at once` and starts the next plan with `raise_scope: false` without asking; it never prints `control.epic.scope_unknown`. The merged shared design says "no screen reads an absent count as none". This is the defect PLAN-106 fixed in the web app | major |
| L3-2 | contract (outside the repository) | — | the API spec does not carry a running count in the epic resource, so the web shows `?` always and the "how many run" figure is never available on that resource. A contract question for `plan-marshall-documentation` / `pm-mcpd` | info (Open Defect) |
| L3-3 | `pm-web-app` `stores/runtime.ts:455-480` | init read vs an act | `initRuntime` applies the lifecycle fields without the epoch check; unreachable today (spinner until init). Not a defect | info |

**Layer 3 verdict:** findings — 1 major (the terminal reads an unknown count as 0), 2 info.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Web Standards | ES modules; no `{@html}` | conforms | no production web file changed |
| CLAUDE.md § Web Standards | no arbitrary timer wait in a test | conforms | no `setTimeout` or `waitForTimeout` in the 5 changed tests; microtask ordering noted |
| CLAUDE.md § Web Standards | npm scripts stay | conforms | no `package.json` in the diff |
| Spec D2 Done-when | daemon check, or `not checkable` with its owner | deviates | the reason given was "outside repository root"; no owner; the repositories are present |
| Spec merge rule | state review-body totals; list every answered item | deviates | the PR body states threads and Sonar only; the 2/2 body count is in the lane report only |

## Metrics and Anomalies

- Tokens: `unknown`; harness `antigravity`, model `unknown`, effort `unknown`; `plugin_version=0.1.0` (looks like a placeholder); executor `0.1.2080`.
- Anomalies:
  - The landing facts are incomplete (`missing_keys` `total_tokens`, `model`, `effort`): a recurrence on the standing Open Defect.
  - The lane report's "Residue: None" is wrong (see L3-1).

## Routing and Merge Behavior

- **Review.** CodeRabbit reviewed the merge head `23ba7ff4` (12:07Z).
  - Threads: 2, both resolved. Both were declined as out of scope; on substance, both were right (L3-1).
  - Review bodies: 1, with 2 nitpicks, both answered in `IC_kwDOVBOtss8AAAABkKC6iA`. Answer 1 claims test coverage that exists only in part.
  - Sourcery skipped on quota.
  - The merge rule is met in form.
- **CI and merge.** Merged through the merge queue as `bf24091c`, right after PLAN-118 (#369), with no reported collision.
- **Sonar.** `read_open_issues --pr 370`: `confirmed`, `open_issue_count` 0, analysis revision = head revision `23ba7ff4`. Lane: 0.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 370; `landing` `landings/PLAN-117.md`; `plan_marshall_plan_id` `rb-117-web-acts-fourth-residue`
- [x] claim 1 of the spec (HYPOTHESIS) stamped `corroborated`
- [x] epic.md: incomplete-landing recurrence; a contract Open Defect (the epic resource lacks a running count); a Watch for the follow-ups
- [x] layer-3 finding L3-1 reported to the operator 2026-11-08
- [x] resume anchor updated; `queue-view.md` regenerated and committed

## Follow-Ups

- **L3-1** (the terminal reads an unknown running count as 0), with the untested German `? von N` form and the untested `control.epic.scope_unknown` text: reported to the operator. A fix plan only on the operator's word. It touches `pm-operator/.../epicview/`, so it overlaps PLAN-34 (running).
- **L3-2** (the API spec's epic resource carries no running count): Open Defect, outside the repository.
- **D1(a) test weakness** (local guard copy, `:289` unpinned, microtask order): Watch, for the next web plan that touches `SystemView`.
- **The `epic-view.adoc:82` table row and the duplicated German text in `work-actions.adoc:222`:** Watch, for the D2 design owner.
