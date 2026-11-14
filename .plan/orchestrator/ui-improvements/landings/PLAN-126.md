# Landing Analysis: PLAN-126 — The Needs Surfaces Made Correct, and the Find Residue Closed

epic: ui-improvements
workstream: WS-05
pr: #416 (https://github.com/plan-marshall/pm-mcp-clients/pull/416), squash `496ac925` (40 files, +2094/-534), merge head `33182e1b`

> Written by the `analyze` verb (2026-11-12) from `rb-126-cli-needs-residue-002.md` (final; `-001` was an interim message with the PR
> open), verified against git, the PR reviews and threads (GraphQL) and the design. Runbook lane, opencode `deepseek-v4.1-flash/high`;
> executor `0.1.2106`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Notice rule | shipped-as-specified | `NeedsOperatorNotices.java:60-110` diffs summaries, never notices a decrease; invented event names removed (`EventKind`); three `ConsoleNeedsNoticeTest` cases fail on revert. The first-summary baseline swallows a first addition (finding 1) |
| D2 Command, selection, subjects, kinds | shipped-modified | row id kind+link (`rowIdOf`), `NeedsSelectionTest` fails on the index key; `blocks` via `common.kind.*`; `OTHER`. "One derivation serves needs, rows and `!`" NOT met (finding 2); the capture predates `7b1fac1c` (stale) |
| D3 Refusal and Show more | shipped-as-specified | `LoadRefusal`, per-level refusal fields; both tests fail on revert; `--page` added (undocumented) |
| D4 Count test and weak tests | not met as written | `sameCountsAcrossFourSurfaces` pins only the total; `NeedsIT` never reaches `openNeedsItemAct`; `needsItemFormattingExhaustive` one `@Test` |
| D5 Find lease, title, header, backspace, Ctrl+C, exit codes | shipped except the shell-path test | ISIG handling correct (`FindCommand.java:421-440`); the lease fix (`:394-405`) has no test |

Added-unplanned: `needs --page` and `?page=` (mock `:6128`), `overview.needs.other`, `NeedsOperatorKind.OTHER`,
`StartPrompt.EXIT_NOT_FINISHED`, `waiver` aliases in `ConsolePrompt`/`ConsoleBadge`, the `TuiKeyRegistry` footer rewrite.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: 40 files across pm-operator, the mock and the catalogue. It did
  not load the listed skills or `three-layer-analysis.md` — disclosed. No write. Re-read by the orchestrator at `496ac925`:
  `NeedsOperatorNotices.java:36-42, 86-97`; `OverviewFullScreenApp.java:1400-1432, 1460-1470`; review `5870515770` body ("Seed the
  baseline"); `PmConsole.java:880-890`. Confirmed.
- **Verdict:** no blocker. Layer 1 — the spec pinned `landing_failed` + plan → `epic <planId>` (resolves no epic) and left `held` →
  `epic <id>` unpinned; it never said whether a place's first event may announce. Layer 2 — D1–D3 tests sound; D4 and D5's shell-path
  clause unmet. Layer 3 — **major:** (1) a first addition after the console starts raises no notice (baseline never seeded; Javadoc says
  the opposite; CodeRabbit's body item unanswered); (2) `!` is a second derivation: `landing_failed` + plan prints `epic` but opens the
  hold, an epic-only condition is a no-op, a linkless decision falls back to bare `answer`; (3) the `find` lease fix is untested;
  (4) review accounting false (below). **Minor:** `held` + plan prints `plan`; raw-id subject fallback; `--page` undocumented and past
  the last page says "Nothing waits"; the count test partly tautological and the mock's fallback emits `waiver_request`; weak D4 tests;
  literal input under `@EnableGeneratorController`; raw `IOException` text as a refusal cause; refusal fields shared across threads
  unguarded; `rowIdOf` collision; `SecretReader` keeps ISIG on (pre-existing, the more serious sibling). **Nit:** stray `/**` in
  `PmConsole.java:885`; merge-shifted Javadoc at `MockRuntimeEngine.java:778-780`.
- Confirmed sound: runtime text sanitised in notices and notifications; en/de parity of new keys; no generic exception; no arbitrary
  waits; terminal restore on every `find` path; the `MockRuntimeEngine` merge kept both sides (`OPERATOR_FIELD`, the config routes);
  footer keeps `? help` and `q leave`. Self-review F6 acceptable (operator's earlier decision); F7 superseded by finding 1; F12 fixed
  by `--page`; F13 (empty text exits 2) defensible.

Routing (operator, 2026-11-12, AskUserQuestion — "Fold into PLAN-127"; side items "Both in the residue plan"): everything →
**PLAN-127** deliverables 5 (the surfaces) and 6 (tests, `SecretReader`, Javadoc artefacts, the #416 replies), surface +19.

## Metrics and Anomalies

- Tokens and plugin version `unknown` (opencode) — the standing Open Defect.
- CodeRabbit refused four triggers on rate limit; the operator chose "wait and retry"; the fifth was accepted.
- The merge queue dequeued the first enqueue (main moved); merged with `origin/main` (one conflict in `MockRuntimeEngine`), re-gated.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `68019669` and `2836f028`; 14 of 14 threads resolved with a reply. **FALSE COUNT:** the landing says
  `review_body_findings_total=17`/`handled=17`; the reviews carry 6 body-only items, 3 of them with no reply and not fixed ("Seed the
  baseline", "status stale after answer", `TuiCapabilityIT:245`), and the mock-page item's reply sits on an unrelated thread. The PR
  body and squash message carry stale counts (13/13) and a stale Sonar head.
- Merged via the queue on the operator's word in the lane (08:26Z).
- Sonar: `0` confirmed by the lane at the merge head. Not re-read (Sonar MCP down).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 416; `landing` `landings/PLAN-126.md`
- [x] PLAN-127 deliverables 5 and 6 (operator)
- [x] epic.md Open Defects (the recurring unanswered review-body items)
- [x] messages 001 and 002 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-127 is unblocked and carries the residue. PLAN-128 still waits for PLAN-160.
- PLAN-147 (running): its premise "`common.en.properties` has no `common.kind.*`" is stale — `common.kind.{project,epic,plan,machine}`
  and `common.unknown` exist at `common.{en,de}.properties:343-350` since PLAN-126; its own re-grounding at HEAD must see them.
- PLAN-133 and PLAN-160 (running) rebase over `MockRuntimeEngine.java` (needs regions `:5748-5806`, `:6067-6145`) and
  `running-work.json` (+79 lines; the literals 18 and "6 questions" are pinned in three tests).
