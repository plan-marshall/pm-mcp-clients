# Landing Analysis: PLAN-128 — Cleanup Dialogue, `project add`, Step Overview and `config cleanup` (Residue of PLAN-23/121/119/32)

epic: ui-improvements
workstream: WS-04
pr: #425 (https://github.com/plan-marshall/pm-mcp-clients/pull/425), squash `56e1ad37` (parent `dea405d8`, 28 files, +2182/-288), merge head `cf940bcb`

> Written by the `analyze` verb (2026-11-13) from `rb-128-cli-cleanup-project-onboarding-residue-001.md` (rev 1), verified against git,
> GitHub's thread state, the review bodies, the design and the SonarCloud API. Runbook lane, opencode `deepseek-v4.1-flash/high`; executor `0.1.2106`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Cleanup dialogue | shipped-partly | 409 decided job, `not_in_job`, exit 3 preview, paging; the project view hides items 21+ (`CleanupCommand.java:707-718`); decision 7 applied to the doc only |
| D2 `project add`, release | shipped-partly | contract body, entry address, `--no-input` exit 3, release block and re-ask; `q`/Ctrl+D still exit 0 (`ProjectCommand.java:245-246`), unasked-argument report absent — not reported |
| D3 Step overview | shipped-as-specified | `StateDialogue.java:686-691`, `onboard.state.step_line_unnamed` en/de |
| D4 Open review items | met by thread state | #377 0/2 and #381 0/22 unresolved on GitHub; named items answered (6838166660, 6838188678, `PRRT_kwDOVBOtss61Elxk`); the lane's "not met" is a `ci pr comments` body-row artefact — our spec bound a count to it |
| D5 `config cleanup` | shipped-partly | row opens it, minimum refused, project value a JSON number, mock one source; a kept value reported as recorded (`ConfigCleanupCommand.java:383-398`), `d)` missing, German `nie` loops |

Added-unplanned: `PmConsole.java:881-885` stray `/**` repair (PLAN-127's item); blank line at EOF of `26-cleanup.adoc`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`); no write. Orchestrator re-read the SonarCloud API (1 open:
  `java:S4449` `ConfigCleanupCommand.java:181`), GitHub's review threads (4 unresolved, 0 replies) and `CleanupCommand.java:710`. Confirmed.
- **Verdict:** no blocker; **8 majors.** (M1) the merge rule was NOT met: 19 threads, 4 unresolved and unanswered (3 from CodeRabbit's third
  review on the merge head), 4 body-only items unanswered on GitHub, PR body without the counts; the lane merged on an in-run answer to a
  `review-gate` `undecidable_read` question that named only a benign file-inventory label. (M2) five "Fixed in cf940bcb" replies are false.
  (M3) items 21+ hidden in the project view. (M4) decision 7: doc changed, code not. (M5) two D2 bullets skipped silently. (M6) Sonar 1
  open (S4449), landing said 0. (M7) a kept value reported as recorded. (M8) landing counts false (`unresolved_review_threads=12`, 17 body
  findings). Minors: new English literals, "after 1 days", `remind` offers "never", machine 409 unhandled, captures for 4 areas missing,
  `PLAN-128.txt` header claims them.
- Self-review sub-agent not run (`self-review:not-run`) and shell in-place edits (self-reported): no visible damage; the unfixed M2-M5 are
  the likely cost.
- Confirmed sound: the onboarding contract body (`pm-mcp-core … 02-enrolment-and-project-lifecycle.adoc:51`); D3; quality gate OK, 80.4 %
  new-code coverage; trailer, no footer.

Routing (operator, 2026-11-13, AskUserQuestion):
- All residue → **PLAN-30** deliverable 8 (surface +11), incl. the #425 review campaign and the S4449 fix.
- Decision 7: **code follows the doc** (`26-cleanup.adoc:64-75,159-164`).
- D4: **met by thread state**; spec convention: never bind a count to `ci pr comments` body rows.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `ecee2174`, `be915306`, `cf940bcb` (15/1/3 actionable); Sourcery refused (size). 15 of 19 threads replied and
  resolved; CodeRabbit wrote "not marking this finding as addressed" on 5 threads the lane then resolved.
- Merged by the lane via the queue (06:48Z) after `review-gate` `undecidable_read` and an in-run operator selection "Enqueue the merge
  anyway" whose question did not mention the unanswered threads. Merge rule not met.
- Sonar: gate OK but 1 open new-code issue (S4449) — the zero-Sonar rule was broken.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 425; `landing` `landings/PLAN-128.md`
- [x] PLAN-30 deliverable 8 + re-grounding note (operator)
- [x] epic.md: Open Defects (lane merge on an undecidable gate; false counts and Sonar), spec rule (6)
- [x] message archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-127 (running): its `PmConsole` Javadoc item is already done by #425.
- PLAN-133 (#424) rebases onto `56e1ad37` (`MockRuntimeEngine.java` onboarding region).
- PLAN-30 waits for PLAN-127; re-ground at outline against `56e1ad37` or later.
