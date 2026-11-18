# Landing Analysis: PLAN-162 — A plan's changes: the contract and the mock runtime

epic: ui-improvements
workstream: WS-01
pr: #444

> Landing record for PLAN-162 (the plan-changes package, part 1; PLAN-163 is the CLI, PLAN-164 the web tab). Merged as `6b97acd2` (33 files,
> +2840/−30). Lane: runbook, sender `rb-162-plan-changes-contract-and-mock`; harness `opencode`, model `deepseek-v4.1-flash/high`, effort `high`,
> executor `0.1.2133`. Written by the `analyze` verb on 2026-11-15 from inbox message `rb-162-plan-changes-contract-and-mock-001.md`, after verifying
> the claims against git, the provider and the merged tree.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 the changes contract in `pm-api` | shipped-as-specified | Fourteen new records under `api/changes/` (list, file, binary file, truncated file, `not_captured`, `unavailable`, the event), `EventName` `plan.change_captured`, `ActivityEntryKind` `model_differs`; `PlanChangesContractTest` asserts each answer's literal wire string (leaf re-read, `:69-205`). No databind, Mockito or Hamcrest; `@NullMarked` package-info; no generic `Exception` catch. |
| D2 the mock serves the changes | shipped-modified | `PlanChangesScenarioTest` (521 lines) reads every answer from the routes, with matched positive and negative controls for the contradicting totals; `ScenarioLoader` validates the totals. "A broadcast … from scenario data" is met only through `/mock/broadcast`: no scenario schedules `plan.change_captured` and the loader validates no scheduled one (`MockRuntimeEngine.java:944`, `ScenarioLoader.java:597`). The mock cannot show a first capture (L3-1). |
| D3 the work package's status | shipped-as-specified | `doc/plans/README.adoc:416` says `in progress` and names `PLAN-162`..`PLAN-164`. |

## Three-Layer Analysis

Read on three layers by one read-only leaf (`execution-context-level-3`), with the orchestrator re-reading on the merged tree (`6b97acd2`) the points marked
[R]. **By reading, no build, no test run, no write.**

- **Verdict:** layer 1 findings (2 major, 4 minor); layer 2 findings (1 minor defect, 2 wrong lane claims, 6 minor test leads); layer 3 findings
  (3 major, 6 minor). D1 to D3 are met as written; the major items are what the consumers (PLAN-163, PLAN-164) will meet.

### Layer 1 — Specified

| Finding | Claim | Verdict | Evidence |
|---------|-------|---------|----------|
| L1-1 | **The six message keys `plan.changes.{title,unplanned,binary,truncated,not_captured,unavailable}` are in the design table (`plan-view.adoc:249-262`) and in no `pm-api` pack** — the spec never asks for them. [R: grep over `pm-api/src/main/resources/messages` is empty.] PLAN-163 and PLAN-164 each assume another plan adds them and name different packs (`operator.*` and `plan_view.*`). | checked, refutes | design table; packs |
| L1-2 | The Expected Surface listed 9 files; the merge touched 33. Needed and undeclared: `ScenarioLoader.java`, `pm-operator/.../WorkerActivityLevel.java` and its test (the exhaustive enum switch breaks on `MODEL_DIFFERS`), the four message packs, `EventEnvelopeTest`, `PlanViewCatalogueTest`, `ProcessTreeTest`, `pm-mock-runtime/README.adoc`. Declared and untouched: none. `pm-operator` was forced by D1's own signature change. | checked | stat |
| L1-3 | The event catalogue (`06-event-catalogue.adoc:68`) names "the count of unplanned changes" and no member; the lane invented `unplanned_total`, while `ChangeSize` uses `unplanned`. The catalogue lives in the other repository. | checked | leaf |
| L1-4 | The design says the client adds nothing up (`plan-view.adoc:170`), but `PlanChanges.totals()` and `PlanChangesCaptured.totals()` are public and the list carries no totals; the contract does not say the head line's figures come from `ProcessTree.change` or the event. | checked | leaf |

### Layer 2 — Implemented

Claims of the lane's report the tree does not bear out:
- **CodeRabbit inline-5 ("deriving the catalogue test's list needs an AsciiDoc reader, a new dependency this plan forbids") is false** [R]:
  `ChatTextsCatalogueTest` (`:38-70`, same package) already reads the design documents with `Files.readAllLines` and a regex, as do
  `ControlCatalogueTest` and `OverviewCatalogueTest`. The disposition rested on a wrong reason, and the hard-coded list in `PlanViewCatalogueTest` is why
  L1-1 went unnoticed.
- **The residue "`fromMap` throws `IllegalArgumentException` for a negative total and `@throws` names only `IOException`" is wrong** [R]:
  `requireFigure` decodes through `JsonTree.integralNumber(…, 0, MAX)` (`PlanChangeCapturedEvent.java:125-128`), which refuses a negative with an
  `IOException` before the constructor's guard (`:59`) is reached; that guard is reachable by direct construction only.
- "The design's texts table now describes the behaviour": the merge changed the README index row, not the design.
- The review gate answers `review_body_findings_handled: 0` / `merge_allowed: false` / `body_findings_undisposed` for #444 (ten findings named), while the
  progress file holds ten dispositions and every thread is resolved — the same mismatch as for #443. Sonar `read_open_issues --pr 444`: `undecidable`
  (`head_revision_unavailable`), so `sonar_open_new_issues=0` is the lane's word.

Other layer-2 findings:
- `PlanCommand.printActivity` (`pm-operator/.../PlanCommand.java:276-303`) has a `default` branch: a `model_differs` entry prints its kind and an empty
  content, both models lost; only `WorkerActivityLevel` was fixed. (PLAN-163 must cover it.)
- Test leads: `PlanChangesScenarioTest.java:484` asserts only that the message contains `fix-relay`; `loadsTheFixtures` checks only the activity entry; not pinned:
  the literal wire of `PlanChangeCapturedEvent.toMap`, the file route's refusals for `not_captured`, `unavailable` and an unknown plan, delivery on a
  stream, ids with `/`, `.`, space or non-ASCII, `lines_omitted` with `truncated: false`.
- The file route answers 404 `target_not_found` for an unknown plan, the list route `scope_not_found` for the same input.
- Untrusted text: paths and lines go through the Jackson generator, which escapes control characters on the wire (pinned by the contract test); U+0085,
  U+2028 and bidi characters reach the wire as UTF-8, so a client that strips only ESC misses them (lead for 163/164).

### Layer 3 — Correct

| Finding | Severity | Evidence |
|---------|----------|----------|
| L3-1 | major (for PLAN-164) | **The mock cannot show a first capture:** `applyCapturedChange` (`MockRuntimeEngine.java:7897-7922`, [R]) refuses a capture for any plan whose answer is not `PlanChangesCaptured` ("has no capture in the fixture for the announcement to move") and any size that differs from the fixture's files, so only the head can move; `not_captured` → captured and captured → `unavailable` cannot be exercised, and the files and size never change. The README states the refusal; neither spec nor lane records it as a limit of the Live clause. |
| L3-2 | major | **The `running-work.json` fixtures contradict themselves:** the first hunk of `PlanChanges.java` has `old_len` 0 beside a context line [R]; the leaf adds hunk lengths that disagree with the lines for `EventName.java` and `plan-changes.ts`, per-file counts that disagree with the hunks (`plan-changes.ts` added 0, removed 0 against two added and one removed line) and an order that is not the path order. The totals do sum (12 files, +340 −27, 2 unplanned). Counts and path order were **not** re-read by the orchestrator (lead). |
| L3-3 | major (contract) | **No coherence rules on decode:** `renamed` without `renamed_from` and the reverse, `binary` with counts or hunks, `truncated` with no `lines_omitted`, `lines_omitted` with `truncated: false` (dropped by `toMap`), hunk lengths against the lines, duplicate paths (last wins in the mock); `binary`, `unplanned`, `truncated` decode through `Boolean.TRUE.equals`, so a missing or non-boolean member reads as `false`. The inline-4 hardening covered arrays only. |
| L3-4 | minor | `JsonTree.integralNumber` accepts string numerals and `1.0`/`1E1`; `ChangeSize.fromMap` uses the truncating `integer`: the loader compares leniently decoded sizes against strict totals. |
| L3-5 | minor | `PlanChanges.fromJson` throws an NPE (`requireNonNull`) for a non-object JSON, not an `IOException`. |
| L3-6 | minor | A file answer carries no `head` or `plan_id`: after a capture a client cannot tell which head it belongs to; file refusals are bare 404s with no wire state. |
| L3-7 | minor | Loader validation checks only that `contents` decode: not that content paths are in the list, that every non-binary file has contents, duplicate `plan_changes` ids, or `ChangeSize` for `not_captured` and `unavailable`; `plan_changes` of a non-start state validates and is never served. |

Thread safety of the mock's changes state: sound (all access under `planChangesLock`, immutable records).

## Leads handed to PLAN-163, PLAN-164, PLAN-146 and PLAN-140

- **PLAN-163 (CLI):** decide one owner and one pack (`plan_view`) for the six keys and extend `PlanViewCatalogueTest` to derive its list from the design; its D2
  "`WorkerActivityLevel` shows both models" is already done by PLAN-162 (`WorkerActivityLevel.java:162`, inline JSON, not the fixture) — rescope it to
  `PlanCommand.printActivity` and the head's launch model; the head's figures come from `ProcessTree.change` or the event; `--file` meets a 404 for
  `not_captured` and `unavailable`.
- **PLAN-164 (web):** the spec's hypothesis "`PlanView` already reads `tab=`" looks refuted (no tab concept at HEAD, leaf); the TS `ActivityEntry` and the live
  `toActivityEntry` (`PlanView.svelte:~332-352`) drop `launch_model` and `served_model`; the "tab appears on the first capture" clause needs a mock
  extension (L3-1); strip more than ESC.
- **PLAN-146:** the hold on PLAN-162 is released. Its cited positions moved: `handleIntentRoutes` is at `MockRuntimeEngine.java:6232` (oversize route at 6258,
  not 5587), `FIXTURE_OVERSIZE` at 619, and in `running-work.json` the epic intent route is at 1400 and the oversize route at 1460 (shifted by the
  `model_differs` entry at ~1054); `applyStateEffectOf` now returns a refusal string D0 can reuse.
- **PLAN-140 D-R134:** every cited `MockRuntimeEngine` line shifted by +44 (`applyChatProposal` 918 → 962, `revisionWindowOpen` 994 → 1038, `planFor` 1024 → 1068);
  `ChatRegistry.java` is unchanged; the broadcast handler now calls `chatRegistry.handleBroadcast` first and then `applyStateEffectOf`, which can answer 400:
  re-verify the order before editing.

## Routing

Routed to `epic.md` (Decisions, Open Defects, Watches) and to the specs of PLAN-146, PLAN-163, PLAN-164 and PLAN-140.
