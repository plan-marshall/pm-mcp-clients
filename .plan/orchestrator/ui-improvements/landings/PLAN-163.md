# Landing Analysis: PLAN-163 — A Plan's Changes in the Terminal

epic: ui-improvements
workstream: WS-05
pr: #463 (https://github.com/plan-marshall/pm-mcp-clients/pull/463)

> Landing record, written by `analyze` on 2026-11-17 from the inbox message
> `rb-163-cli-plan-changes-001.md` (a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `ae410264`.

Merged as `ae410264243ff67bd58712a3c69decee81a5aead` (2026-11-17T17:35:42Z), 33 files, +3384/-234. Sender `rb-163-cli-plan-changes`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated). The lane reports `deliverables_done=3` of 3 (D0, the folded residue of the changes contract; D1; D2) and `self-review: clear` after four rounds.

## Deliverable Fidelity vs Spec

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D0 — the residue of the changes contract and its fixtures | shipped, four clauses partly | six `plan.changes.*` keys in both packs with a catalogue test derived from the design (`PlanViewCatalogueTest.designKeyIsInBothPacks`, `noChangesKeyBeyondTheDesign`, `designTableYieldsRows`); fixtures hold hunk lengths, counts, path order and the totals 12 / +340 / −27 / 2 (`PlanChangesScenarioTest.TheList.totalsAgreeWithThePlan:176`); 13 coherence-on-decode cases (`PlanChangesContractTest.CoherenceOnDecode:176-300`); the head's figures read once (`PlanChangeSizes.java:60-81`; `.totals()` has no client caller); untrusted text through `TerminalSanitizer` (C1, U+2028/2029, bidi; `ChangesLevelTest.UntrustedText`); the two pins (`eventWire:376`). **Partly:** the loader's hunk-length refusal is delivered through the codec only (re-read: `old_len` appears in `PlanChangesScenarioTest` once, `:812`, inside a valid builder; refused at decode, `PlanChangeHunk.java:45-64`, pinned `PlanChangesContractTest.hunkLengths:270`); the file route "names its head or the spec says why not": the reason is in `PlanChangeFile`'s Javadoc only (`:30-32`), no specification text; `printActivity`'s `model_differs` is pinned on inline JSON (`PlanCommandTest.modelDiffersNamesBothModels:233`), not on the fixture; several loader refusals assert only that the message contains a path |
| D1 — the changes level and the one-shot form | shipped, one clause partly | `d` opens the level, `Enter` the hunks, `Esc` closes hunks then the level (`OverviewFullScreenApp.java:2783,2813,3820`; `OverviewChangesKeysTest.Keys.opensAndCloses:176`); `plan <id> --changes [--file <path>]` (`PlanCommand.java:127-132,289,324`; `PlanCommandTest.Changes.*`); `not_captured` prints its text. **Partly:** the `ScreenCaptureIT` capture (six captures in `PLAN-163.txt`) asserts non-empty output and the exit code only (`ScreenCaptureIT:115-190`): evidence the capture exists, not of its content. The lane's own reading, `mutation-after` against the declared `test-first`, is disclosed |
| D2 — `model_differs` in the worker's activity | shipped | `WorkerActivityLevelTest.fixtureModels` reads the `running-work.json` route and pins both models; the scrollback half is D0's `printActivity` |

**Files the diff touches that the Expected Surface does not list:** `pm-mock-runtime/.../StartRegistry.java` (the lane: one line; two lines in fact, a constant removed and its use changed), `OverviewCatalogueTest`, `SpecificationTables`, `OverviewFullScreenApp.java`/`TuiKeyRegistry`/`TuiKeyAction`/`TuiLevel` (the level's wiring).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf — reason: the Dispatch Decision Rule; 33 files and 3384 added lines. Build logs, CI results and screenshots were not opened by the leaf ("not shown by reading"). The orchestrator re-read the points marked below on `ae410264`.
- **Verdict:** layer 1 minor (four clauses partly); layer 2 no contradicted claim; layer 3: no blocker, **2 major**, 10 minor.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | the loader refuses a hunk length that disagrees with its lines | checked | partly | re-read: the scenario test never mutates `old_len`; the codec refuses it (`PlanChangeHunk.java:45-64`) |
| L1-2 | the file answer names its head or the spec says why not | checked | partly | decided in Javadoc (`PlanChangeFile.java:30-32`); the rationale cites `plan-view.adoc` § Live, which says nothing of a head on a file answer; no design line |
| L1-3 | a `ScreenCaptureIT` capture at 80 columns | checked | partly | exists; no content assertion |
| L1-4 | the activity is read from the fixture | checked | partly | the scrollback pin uses inline JSON; only the level test reads the fixture |
| L1-5 | the six keys, fixtures, untrusted-text stripping, the two pins | checked | confirms | see D0 |
| L1-6 | the design document is updated in the same PR | checked | partly | see the stale lines below |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| "the capture identity check" | only on the refresh path | re-read: `readOpenFile` guards `changes.answer() == fresh && openPath.equals(...)` (`OverviewFullScreenApp.java:1892-1910`); `changesOpenSelected` (`:2813-2840`) reads the file outside the monitor and calls `showFile`, which checks only that the path is in the current list (`ChangesLevel.java:209-222`) |
| "live re-read on `plan.change_captured`" | broader than stated | `refreshActiveLevels` (`OverviewFullScreenApp.java:1620-1656`) dispatches by level kind with no event name: every structural event of any plan re-reads; harmless, not scoped as the design says |
| "one read of the figures" | borne out | `PlanChangeSizes` is the only reader; a failed read keeps the last figures |
| `TerminalSanitizer` reuse; the colour residue; the `reports-and-system` exemption | borne out | `TuiLayout.sanitizeCell`; the only `AttributedStyle` in `tui/` is the selection's `inverse()` (`TuiLayout.java:387,438`); `ScenarioLoader.java:277`, `exemptsAScenarioWithoutCaptures` |
| `ScenarioLoader` imports `MockRuntimeEngine` for `BASE_FIXTURE_STATE` | lead | the scenario package now depends on the runtime class that imports scenario types (`ScenarioLoader.java:66`); no architecture check read |

Not shown by reading: the failing-run logs, the mutation run at `9999f5aa`, the captures' content, the CI runs.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ChangesLevel.java:370-377` (re-read: segments beyond three become `first/…/second-last/last`), `PlanCommand.java:303-305` (re-read: the one-shot prints the same `level.rows`); `PlanCommandTest.printsTheList` asserts `A  pm-api/…/changes/PlanChangeFile.java` | `plan x --changes`, then `plan x --changes --file <what was printed>` | the list never prints a full path and `--file` needs the exact full path: "No such file"; the stated purpose "so that the operator can pipe them" (`16-plan-view.adoc:114`) is defeated; the design's picture abbreviates, so this conforms to the picture and not to the sentence | major |
| L3-2 | `OverviewFullScreenApp.java:2813-2840`, `ChangesLevel.java:147-153,209-222` | a capture lands between the file read and `showFile` | `update()` has replaced the answer with `openPath == null`; `showFile` then sets `openRow` to the new list's row with the old hunks; the next capture compares `row.equals(openRow)`, finds them equal and never re-reads: old hunks under new counts until that row changes again. The file answer carries no head to detect it (the D0 decision). Low likelihood (one round trip), silently wrong content | major (low likelihood) |
| L3-3 | `TuiLayout.rowLines`, `ChangesLevel.java:333-335,373` | a Java line over about 76 columns; a path of three or fewer segments | cut at the screen width with `…`, no wrap or scroll; a long name can push the counts off an 80-column row (lead, capture not read) | minor |
| L3-4 | `TuiLayout.sanitizeCell`, `ChangesLevel.java:53`, `PlanCommand.printRows` | piped hunks | tabs become one space; lines carry a space or U+2212 prefix and a two-space indent: the output cannot feed `patch`, `diffstat` or `git apply` | minor |
| L3-5 | `PlanCommand.java:348`, `:293`, `OverviewFullScreenApp.java:2790,2843-2855` | a file outside the list; a bad answer | English literals and codec or server text in the status line and the one-shot instead of the en/de catalogue (`:293` follows an existing convention) | minor |
| L3-6 | `ChangesLevel.java:128-134` | a captured list with zero files | no rows and no text; the one-shot prints the header only | minor |
| L3-7 | `PlanCommand.java:303-306,151` | `not_captured`, `unavailable`; `--file ""`; `--changes` with `--activity`, `--tokens`, `--watch`; no plan id | exit 0 for a refusal, so a script cannot tell; `--file ""` prints the whole list; `--changes` wins silently; no plan id falls to the plan list | minor |
| L3-8 | `ChangesLevel.java:163-169,407`, `OverviewFullScreenApp.java:3820-3826` | an open file whose row changed | the hunks are dropped until the re-read lands and `Esc` then leaves the whole level; the selected hunk line id is positional, so the cursor can sit on a different line after a re-read | minor |
| L3-9 | `ScenarioLoader.java:513,567-590` | non-ASCII paths; an understated omitted count | UTF-16 `compareTo` order (the lane noted it suits ASCII); the omitted-count check is a lower bound | minor |
| L3-10 | `OverviewFullScreenApp.java:1849-1851` | a plan that vanishes while the level is open | a non-200 list read returns silently: a stale level, no notice | minor |
| L3-11 | `ChangesLevelTest`, `PlanChangesScenarioTest.java:662,756,880,899` | — | inputs are hand literals (whether `@EnableGeneratorController` applies not checked: lead); refusals asserted by `contains(<path>)` only | minor |
| L3-12 | `16-plan-view.adoc:114` | — | the colour clause ("removed and added lines take the terminal's colours only where it has them") is not implemented (confirmed: no per-cell style channel); the lane records it; the design text was not changed | minor |

**Layer 3 verdict:** findings — no blocker; two major (L3-1, L3-2); 10 minor.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Java | no Mockito/Hamcrest, no generic exception, JUnit 5 | conforms | none seen in the leaf's reading |
| CLAUDE.md Java | input test data from `Generators`, `@EnableGeneratorController` | not shown by reading | L3-11 |
| CLAUDE.md Java | a hand-written literal is not input | not shown by reading | the expectation literals (catalogue text, wire, golden capture) are allowed |
| spec Standing | the design document is updated in the same PR | partly | see the stale lines below |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | the lane states none; #463's file list holds no `.plan/orchestrator` path (re-read: `git show --stat ae410264`) |

**Stale or misplaced design lines:** `doc/design/terminal-client/01-commands.adoc:117-122` (re-read: the new `plan <plan_id> --changes [--file <path>]` synopsis is inserted between the `plan` synopsis and its description, so the description "a plan's process as a tree, its tokens so far, what a task's worker does" now hangs under the `--changes` line); `16-plan-view.adoc:114` (the colour sentence; the piping sentence only partly true); `16-plan-view.adoc:165` (the terminal's Live paragraph does not describe the changes level's live read); `doc/design/shared/plan-view.adoc` § The Changes, "on the diff tokens of the client" (the terminal has none; not read at line level).

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `0.1.2135`; executor `0.1.2135`. `landing-check`: `complete: false`, `missing_keys` `total_tokens`; `surface_delta` `unmeasured`. The lane names the cause: the lane template writes `unknown` for tokens and the validator treats `unknown` as unsupplied at `total_tokens` (in 0.1.2135 and 0.1.2149): a defect of the plan-marshall repository, not read here.
- Self-review: four rounds (15, 10, 7 and 8 findings); the operator chose a fourth round at the three-round limit.
- Two commits were created without the trailer; one was caught and amended before the push (`git commit --amend`); the lane discloses it. `bb4d83f7` is red on its own (its assertion named a rendering the code it ships cannot produce); `cf660068` corrects it, so the range does not bisect between them.
- CI: two earlier failures were infrastructure (a Maven Central 429, an `npm ci` ECONNRESET), re-run.
- The lane's rule breaks: shell `grep`, `rg`, `ls`, `wc`, `date` in the early exploration and for a few counts; no guard enforced on this harness.
- The mailbox read with a digit-leading plan id is refused: not reported by this lane.

## Routing and Merge Behavior

- Review: CodeRabbit, 18 body findings handled by the lane (`b834e8c1` 9, `6df88d16` 2, `aef57fa7` 3, `ab752875` 1, `9e11c7e3` 1, `d345e6d9` 3; two answered with a stated reason); re-read: `review-gate --pr-number 463 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=18` (its `handled:0` is the gate lacking `--disposition`). Its last qualifying review was at `07262eb5`, risk Low; later heads were not re-triggered (rate limited, then paused automatic reviews). Sourcery refused (150,000 diff characters).
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull request 463 returns no issues; the lane reports the gate passed at the merged head, 84.6% coverage on new code.
- Merged through the queue (merge-group run 44136199068).

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-163 --status shipped`
- [x] row `pr` stamped — `463`
- [x] row `landing` stamped — `landings/PLAN-163.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-163-cli-plan-changes`
- [x] epic.md: Open Defect `PLAN-163 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-163 residue plan** (named, NOT staged): the one-shot's full paths for `--file` (L3-1), the stale-hunks guard on the key-driven open (L3-2), the colour channel or the design sentence (L3-12), the loader's hunk-length test, the specification text for the file answer's head, the English literals, the empty-list text, the exit codes, the `01-commands.adoc` synopsis, the `16-plan-view.adoc` lines, a content assertion in the capture.
- `PLAN-164` (the web changes tab) lost its dependency on PLAN-163; its spec predates D0 and must be re-grounded at the outline against the shipped contract (`PlanChangeFile`, `PlanChangedFile`, `PlanChanges`, the six keys). It shares `client.ts` and `client.test.ts` with the running PLAN-51.
- `PLAN-163`'s file answer carries no head: PLAN-164's web client has the same race on a file read; the spec of PLAN-164 should say how it avoids it.
