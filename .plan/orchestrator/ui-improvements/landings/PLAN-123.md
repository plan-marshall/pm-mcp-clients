# Landing Analysis: PLAN-123 — The Terminal's Display Layer, Residue

epic: ui-improvements
workstream: WS-05
pr: #404 (https://github.com/plan-marshall/pm-mcp-clients/pull/404), squash `1bdfede6`, merge head `595e1ef3`

> Written by the `analyze` verb (2026-11-11) from `rb-123-cli-display-layer-residue-002.md` (which supersedes `-001`,
> the stopped-before-merge report drained as observed on 2026-11-10), verified against git, `ci pr reviews`,
> `runbook_lane review-gate` and the progress file. Runbook lane, opencode, `deepseek-v4.1-flash/high`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Regressions and #371 | shipped-as-specified (with test gaps) | (a) `Display.java:336-339` `d. MMM`; (b) context clock through `StatusCommand`, `ProjectsCommand`, `ProjectCommand`; (c)/(d) `unit.unknown`, `common.time.unknown/never`; (e) `Display.eventTime` with a `Clock`; (f) German change lines. Gaps: `WebCommand:612-615` and `CleanupCommand:342` `d. MMM` and the ended list / `projects --list` ages have no revert-failing test |
| D2 One duration formatter | shipped-as-specified | the amendment's grep over `pm-operator/src/main` returns nothing; `2 d`/`2 T` pinned in three views |
| D3 No raw text reaches the operator | shipped-modified | 18 `Display.word/title` sites; placeholder sites into the catalogue; `Display.gate` for waivers. Left: `ProjectsOverviewLevel:177` English overload; `printResource` of the printed epic untouched; em-dash catalogue placeholders; `ZoneId.systemDefault()` at `EntryFlow`, `PmConsole`, `StartRenderer` |
| D4 PLAN-34's screen evidence | shipped (not shown by reading) | `screen-captures/PLAN-123.txt` (28 capture definitions); rendered screens only exist when `ScreenCaptureIT` runs |

Added-unplanned, explained: `QuestionsCommand` split into helpers and `isWaiver` also by `kind=waiver` (test added).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: 63 files of display code across `pm-api` and
  `pm-operator`. The leaf wrote six key-list files into the session scratchpad (a disclosed S1 breach, outside the repository
  and `.plan/`; deleted). Re-read by the orchestrator at `1bdfede6`: `Display.java:130-146`, `QuestionsCommand.java:77,86,
  133-139`, `ProjectsOverviewLevel.java:177`, `StatusCommand.java:469-473`. Confirmed.
- **Verdict:** layer 1 findings (minor: the spec states the unknown-value rule for `Display.title` only, so three divergent
  guards grew around `Display.word`); layer 2 findings (minor: the test gaps and leftovers above); layer 3 findings (all
  minor: `Display.gate` turns `coverage:pm-api` into `pm api` and humanises an unknown gate kind; unknown landing/task/job/
  outcome states fall back to raw; `QuestionsCommand` reads catalogue keys built from wire values in the default locale and
  can print a raw key, and keeps English literals; a non-`Z` ISO time passes through; the lane's named residue — completion
  outcome/deliverable states raw, `plan report` English, `RolesCommand` default locale). No blocker or major.
- Confirmed sound: the clock rule, the duration consolidation, `en`/`de` key parity for `common`, `operator`, `overview`; no
  generic `Exception` in production code.

Routing (operator, 2026-11-11): the text residue → **PLAN-30** deliverable 5 (surface +12 in the same edit, 44 paths); the
English `formatNeedsItemSubject` overload → **PLAN-126** deliverable 2 (no surface change: its files are declared). Not
routed (recorded): `printResource` of the printed epic, em-dash placeholders, `ZoneId.systemDefault()` sites, the generators
deviation in the new tests.

## Metrics and Anomalies

- Tokens / plugin version `unknown` (opencode) — `landing-check` `complete: false`; folded into the standing Open Defect.
- `self-review:budget-exhausted` (two pre-existing surfaces declined with reasons). Mailbox unreadable on all three reads
  (`invalid_target_plan`, the standing Open Defect).

## Routing and Merge Behavior

- Review: CodeRabbit refused five times on quota, then reviewed `c8be43d1` (5 inline + 2 body findings) and the merge head
  `595e1ef3` (no new findings); merge rule met. `review-gate --pr-number 404`: `unresolved_review_threads=0` (landing 0),
  `review_body_findings_total=5` (landing 5) — agree; the progress file carries 7 dispositions (3 declined with reasons —
  phase names identical by design, no raw plan ids in the printed epic, `RolesCommand` wholesale — and the rest fixed in
  `ac323f2f`). Sourcery refused (diff too large).
- CI/merge: merged via the queue on the operator's word ("Merge now").
- Sonar: claimed 0 (the lane: 9 → 3 → 0 across heads, new-code coverage 83.9 %). Re-read here `undecidable`
  (`head_revision_unavailable`, a merged PR; Sonar MCP down) — not re-measured.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 404; `landing` `landings/PLAN-123.md`; `plan_marshall_plan_id` `rb-123-cli-display-layer-residue`
- [x] PLAN-30 D5 and PLAN-126 D2 folds (operator)
- [x] epic.md: the PLAN-123 watch retired to this report; incomplete-landing recurrence
- [x] resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-126 (needs residue + find residue) and PLAN-32 (config contract) waited on this merge: unblocked (PLAN-126 also on #405).
