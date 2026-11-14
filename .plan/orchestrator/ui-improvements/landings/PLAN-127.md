# Landing Analysis: PLAN-127 — `answer`, `epic next` and the Needs Surfaces (PLAN-21/PLAN-126 Residue)

epic: ui-improvements
workstream: WS-04
pr: #426 (https://github.com/plan-marshall/pm-mcp-clients/pull/426), squash `98cea294` (73 files, +3709/-775), head `c26785cc`

> Written by the `analyze` verb on 2026-11-13. Source: `plan-127-cli-answer-and-epic-next-residue-012.md` and 11 candidate lessons
> (`-001` to `-011`). Verified against git, the threads of #426, #382, #385 and #416, and the SonarCloud public API.
> Run: /plan-marshall with claude-sonnet-5-5 at effort medium, plugin `0.1.2106`, at least 51 M tokens, about 23.5 h wall time.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence (at `98cea294`) |
|---|---|---|
| D1 Answer verb | shipped-as-specified | Dissent matched by summary (`AnswerCommand.java:1019-1030`). The parts guard fails closed (`:404-422`). A scoped 404 is not retried unscoped (`:452`). A non-200 first read is refused (`:153-156`). Long option keys are handled (`:1161`, `:1279`). The UNCLEAR, read-back and clarified paths are covered (`:1434-1505`). The confirm title follows the stake (`:1623`). EOF prints "Left" and exits 0. German `dem Projekt`. |
| D2 Sanitizer, PTY | shipped (one spec defect) | `TerminalSanitizer.java:106-135` and `SanitizingPrintWriter` (`AnswerCommand.java:135`). `AnswerSanitizationTest:98-137`. `AnswerDialogueIT`. For `[y/N]`, "unknown input re-asked" is wrong in the spec: leaving is right (F9). The four `@Test`s were accepted. |
| D3 `epic next` | shipped (residual F2) | `WorkActionDialogue.next` `:329-348`. `--no-input` exits 3. Contradicting flags are a usage error (`EpicCommand.java:497-505`). The texts come from the catalogue. |
| D4 Replies owed on #382, #385, #416 | shipped | Every owed thread and item has a reply (see below). The functional review graded it unevidenced, and the operator closed it. |
| D5 Needs surfaces | shipped, terminal check unevidenced (F1) | `NeedsOperatorNotices` announces the first addition. `!` runs the printed command (`OverviewFormatting.needsItemCommand:464-479`). `waiver_request` is renamed to `waiver`. `needs --page` past the last page is handled. The mock serves one list. |
| D6 Residue | shipped-as-specified | `FindCommandTest` lease. `SecretReader` clears ISIG, with a test. `NeedsOneCountTest`. `TuiCapabilityIT` derives its cut list. `rowIdOf` has its full identity. `LoadRefusal.unreachable`. |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`). The functional review recorded no verdict counts because the operator closed it by override at every head, so the leaf graded each deliverable itself.
- **Verdict:** no blocker.
- **Layer 1 (spec):**
  - D2 asked "unknown input re-asked" for a `[y/N]` prompt; leaving is right there.
  - The spec did not list PLAN-127 under PLAN-30's "Depends on".
- **Layer 3, major:**
  - F1: the D5 terminal check (`ScreenCaptureIT` captures at 120 × 30 and 80 × 24) is unevidenced. `work.log:3660` marks it "still owed", and the override close hid that.
- **Layer 3, minors:**
  - F2: `epic next` asks to raise even when a plan is launchable, and leaving prints nothing.
  - F3: the documents list only some of the `epic next` flags.
  - F4: `isAccepted` repeated at 4 sites; the mock `recordActBodies` path guard; two #382 nitpicks noted but not changed.
  - F5: the startup-notice baseline is a per-kind count seeded from the first page only.
  - F6: U+2028 and U+2029 are dropped, not spaced.
  - F7: the literal `"attention"`, and a second rule for unknown values.
  - F8: `integrity_hold` drift (already tracked).
  - F10: reply hygiene (internal task id; "13 actionable" against 12 threads).
- **Confirmed sound:** the D1–D6 code, the test classes (`@EnableGeneratorController`), Sonar, and every reply.

## Routing and Merge Behavior

| PR | Threads | Resolved | Reply on every thread | Body-only items answered |
|---|---|---|---|---|
| #426 | 15 | 15 | yes | 3/3 |
| #382 | 12 | 12 | yes | 3/3 (2 "valid, not changed") |
| #385 | 18 | 18 | yes | 2/2 (1 deferred) |
| #416 | 14 | 14 | yes | 6/6 (status count declined with a reason) |

- Real values for the keys the landing wrote as `unknown`: `review_body_findings_total=14`, `handled=14`.
- Sonar: 0 open issues and 0 hotspots. The 6 issues of the first analysis were fixed: S4449, S1845, 3× S1192, S6885. None was dismissed. Quality gate OK; 93.4 % coverage on new code.
- Merge gate:
  - It refused with `merge_participation_unproven` because Sourcery, an optional bot, refused on size.
  - The operator granted `barrier-ask-override`.
  - CodeRabbit, the required reviewer, reviewed the head.
- Merged by the run through the queue.

## Reconciliation Actions

Routing as recommended by the analysis (operator: "Analyze the other one (#127) and emit next"):
- [x] **PLAN-30**:
  - New D9: F2, the 4 `isAccepted` sites, F6, F7, the `ControlResourcesTest` nitpick, the mock `recordActBodies` narrowing, and the `MockRuntimeCli.java` formatter rewrite.
  - A re-grounding amendment with 11 facts (D1 `NextCommand` void, the per-verb picocli bundle, the rethrow sites, the renamed keys, the catalogue-test coupling to `doc/design/`, and others).
  - "Depends on" now names `PLAN-127`. The pm-mock-runtime build and `-Ppre-commit` are added.
  - **The merge rule, which the spec lacked, is added.** It includes the optional-bot clause and a size stop at about 60 files.
- [x] PLAN-42: F3 documents. PLAN-79: the #382 `ControlDialoguesTest` nitpick.
- [x] Re-grounding notes: PLAN-44, PLAN-48, PLAN-50 (plus seeding the baseline from every page, F5), PLAN-138, PLAN-140.
- [x] Epic Watches: F1 captures (operator or PLAN-33); F5 needs item identities in `status` (runtime epic).
- [x] Lessons:
  - New in plan-marshall's store: `2026-11-13-21-001` (bare `./mvnw` does not stamp the change ledger), `-002` (optional-bot structural refusal blocks the merge), `-003` (the run's own PR comment is ingested as a finding).
  - Evidence added to `2026-11-13-14-003` (#5), `-14-015` (#6), `-10-001` (#1, stray store) and `-10-002` (#2 and #3).
  - #7 and #11 (pm-mcp-clients-local) were refused by the store (`wrong_store`, no owning bundle). Their content is in PLAN-30 D9 and re-grounding item 11.
  - #10 was dropped (the later review caught it; covered by "claims are leads").
- [x] Row set to `shipped`, pr 426, landing. Messages 001–012 archived.
- [x] **PLAN-30 emitted** on the runbook lane.

## Follow-Ups

- PLAN-134 (#428, running): the stop report `rb-134-…-001` stays in the inbox. The lane is retrying on the same PR.
