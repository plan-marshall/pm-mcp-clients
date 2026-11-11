# Landing Analysis: PLAN-158 — What the Error and Loading Pattern Left Open (PLAN-45 Residue)

epic: ui-improvements
workstream: WS-03
pr: #408 (https://github.com/plan-marshall/pm-mcp-clients/pull/408), squash `4a84f727`, merge head `935db6e1`

> Written by the `analyze` verb (2026-11-11) from `rb-158-web-error-pattern-residue-001.md` (revision 1), verified against
> git, `ci pr comments` / `ci pr reviews`, `runbook_lane review-gate`, the mailbox state and the progress file. Runbook lane,
> opencode, `deepseek-v4.1-flash/high`; executor `0.1.2106` at landing (main's steward upgrade, #411).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 No raw code on an act path | shipped-as-specified (wider) | `errors.ts:90` `actFailureText` / `:105` `actFailureReason`; seven per-view `ClientError('timeout')` tests; the grep names no view; about twelve act surfaces beyond D1's list closed on the operator's word |
| D2 Banners, deadline, timer | shipped-modified (4/5) | `DecisionView.svelte:416-437` (owner-scoped clearing), `ProjectExplorerView.svelte:65-72`, `client.ts:3170-3178`; the fake-timer clause (`client.test.ts:633-662`) passes before the change too — not met as written; the failing-Retry focus arm has no test |
| D3 Mock codes from the catalogue | shipped-as-specified (per code) | remaining `_not_found`: members plus `scenario_`, `state_`, `version_not_found` (reported); per-site pins missing at several handlers |
| D4 Tests, keys, design text | shipped-as-specified | StartView/EpicDecomposition tests; e2e `timeout: 20000` (`error-pattern.spec.js:62`); four orphan keys removed; `components.adoc:463` refresh form; Generators in the two named tests |
| D5 Browser matrix | shipped (not shown by reading) | 20 screenshots and snapshots under the lane's `.plan/temp/playwright-mcp/` |
| Mailbox advisory `orchestrator-001` (PLAN-129 residue, 8 items) | 7 fixed, 1 partly | items 1–7 with tests (`ReportsView.svelte:83-113, 271-298, 443-500, 575-597`, `RowMenu.test.ts:542-567`); item 8: `ReportsCommandTest` draws Generators in 1 of 5 tests. The mailbox still reads `delivered_unconsumed` (the lane never marked it consumed) |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: 68 files of web, mock and catalogue code plus the
  advisory. The leaf wrote one scratch copy of `MockRuntimeEngine.java` by shell redirect (disclosed S1 breach, outside the
  repository; deleted) and a `git grep` printed two `.plan/` lines it did not use. Re-read by the orchestrator at
  `4a84f727`: `ProcessDesigner.svelte:111,821,858,1550`, `ReportsView.svelte:50,390,407,445,460,586-592`. Confirmed.
- **Verdict:** layer 1 findings (minor: the D2 fake-timer clause cannot fail before; the D1 grep is narrower than the
  objective; "next successful act" has two readings; the spec's scope/target sentence differs from the `ApiErrorCode`
  javadoc the lane followed); layer 2 findings (minor: the advisory item 8 partial, per-site mock pins, the failing-Retry focus
  arm, StartView's untested `decomposition-retry` raw button, the squash message's empty review placeholders, constant
  `announce` expressions); layer 3 findings (all minor: the three CodeRabbit findings of the second review — (a)
  `ProcessDesigner` stale `placeOutcomeUnknown`, (b) a local Mark read does not advance `listEpoch`, (c) a marker survives when
  both reads fail — still hold at `4a84f727`; `actFailureText` loses the act's own text for an unknown daemon code; an unknown
  plan's chat answers `target_not_found` while its other routes answer `scope_not_found`). No blocker or major.
- Confirmed sound: the generation guard, the epoch discard, the per-request pending counter, the mock's new lock order, no
  `{@html}`, no new dependency.

Routing (operator, 2026-11-11, AskUserQuestion — "Fold into PLAN-153..157"): reports, designer and helper items →
**PLAN-154** deliverable 5 (surface +5, 15 paths); StartView retry button and the per-site mock pins → **PLAN-155**
deliverable 5 (surface +5, 18 paths).

## Metrics and Anomalies

- Tokens / plugin version `unknown` (opencode) — `landing-check` `complete: false`; folded into the standing Open Defect.
- `self-review:budget-exhausted` after three rounds; the operator chose "close them all" twice.
- The lane first read the wrong mailbox; the operator re-sent the exact read (`rb-158-…`). The landing says it read and fixed
  the advisory; the mailbox was never marked consumed.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `e7a943a4` (9 inline + 1 outside-diff, all replied, resolved, fixed in `7bde4251`) — merge rule
  part one met. **FALSE CLAIM:** the landing states `unresolved_review_threads=0`; `ci pr comments --pr-number 408` shows 3
  unresolved CodeRabbit inline threads from a second review at 11:51Z on `7bde4251`
  (`PRRT_kwDOVBOtss62J59R`, `…59V`, `…59Z`), never answered — so "every thread replied and resolved" was not met at the
  merge. Cause: `runbook_lane review-gate` answered `undecidable_read` (it cannot parse CodeRabbit's "Files with no reviewable
  changes" block) and the operator chose "Merge despite the undecidable read"; the lane then wrote 0 into the facts.
  `review_body_findings_total=1` / `handled=1` could not be re-read by the gate (undecidable). The squash message carries the
  review placeholders empty.
- The three threads are still open on the merged PR; replying to them was not authorised in this session (a GitHub write the
  operator did not ask for) — the operator decides.
- CI/merge: via the queue on the operator's word. Sonar: the lane read `count_status: confirmed`, `0` at `935db6e1`, gate OK
  (new coverage 81.8 %); not re-read here (the Sonar MCP connection is down; the script read of a merged PR is undecidable).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 408; `landing` `landings/PLAN-158.md`
- [x] PLAN-154 D5 and PLAN-155 D5 folds (operator), surfaces updated in the same edit
- [x] epic.md: incomplete-landing recurrence; the `review-gate` parser gap and the false review count as an Open Defect
- [x] resume anchor; `queue-view.md` regenerated
- [ ] the three open threads on #408 — the operator's call

## Follow-Ups

- PLAN-154 and PLAN-155 carry the residue; they start after the D1 foundation chain (147 → 152).
- `runbook_lane review-gate` must parse CodeRabbit's informational blocks, and a lane must not write `0` for a count its gate
  could not read (Open Defect, owner plan-marshall).
