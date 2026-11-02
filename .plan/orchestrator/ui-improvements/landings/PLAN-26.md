# Landing Analysis: PLAN-26 — Terminal: One System Model

epic: ui-improvements
workstream: WS-04
pr: #299 (merge commit `be4995e516787d418aa2e42d0e2263f213a342a2`, parent `1ce32d80`)

> Landing record for one shipped plan. Source: inbox message `rb-26-cli-system-model-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`, written 2026-11-01T19:03Z), analysed 2026-11-01 against
> the merged PR (`ci pr view`), the merge diff (`git show be4995e5`, 24 files, +1502/−686), the changed code and tests, and
> the Sonar read. Analysed in the three layers of the epic's rule (Queue annotations of `epic.md`): specified, implemented,
> correct. Everything in layer 3 was READ, not run; the per-head Maven runs, the screen captures and the browser check are the
> run's account.

## Verdict per layer

| Layer | Verdict |
|-------|---------|
| 1 Specified | **Met in intent, one clause changed by the run.** D1 (one model, one formatter) and D3's exit codes are as specified. D2's first line is **not** the specified text: the spec says `draining · 2 plans finish · stop in 4 min`, `restarting`, `stopped`; the run built `draining · 2 left`, `stopping`, `upgrading`, `stopped` (no deadline, no `restarting`) and **rewrote the design to match** (`doc/design/shared/system.adoc#state-line`). The reason is the run's account and is only half checked (below). D3's browser clause for `running-work` is not met, as the lane says. |
| 2 Implemented | **All three deliverables are in the diff; 16 files were added beyond the declaration**, among them four production changes the spec did not name (exit-code edits in write verbs, a wire-name fix in `pm-api`, the full-screen banner logic). The PR body states a line (`draining · 2 plans finish`) that the diff does not produce. |
| 3 Correct | **No defect found in the formatter.** Gaps are in the tests: the full-screen lifecycle logic and the write-verb exit codes have no test, the new tests use hand-written literals against the standing constraint, and the D2 test asserts the built line, not the specified one. Read, not run. |

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 SystemSnapshot: one model, one formatter | shipped-as-specified, with the model decision the spec asked for | `pm-operator/.../system/SystemView.java` (690 lines, new) is the one formatter; `SystemCommand` (−240) prints `SystemView.printedLines`, `SystemLevel` (−400) draws the same `Figure` list (`git show`). The spec's `pm-operator/.../SystemSnapshot.java` ("created by this plan") was refuted by the spec's own re-grounding note; the run used the `pm-api` record and said so (message, Hypotheses): correct, no second `SystemSnapshot`. `RuntimeActDialogue`'s private "what runs" formatter moved into `SystemView.runningNow` (the spec note asked for one copy): done, the two derivations are one. The done-when "`SystemFullScreenTest` and a printed-view test show the same listeners and token units" is carried by a **new** `SystemViewConsistencyTest` (3 tests: listeners, token units, the not-measured reason once); `SystemFullScreenTest` changed by one line. The test-first failing run (3 tests at `66c1495a`) is the run's account. |
| D2 Lifecycle line and repeats | **partly as specified** | Lifecycle on the first line: `SystemView.lifecycleLine` (`system.lifecycle.line={state}{finish, plural, =0 {} other { · # left}}`). Built text `draining · 2 left` vs specified `draining · 2 plans finish · stop in 4 min`; `restarting` is not a state (`RuntimeLifecycleState` has `running, draining, stopping, upgrading, stopped`; a restart shows as `stopping` then the next state). One line per job kind: done (`jobKindLines`, kinds sorted, locks on the figure line). "Not measured" once with its reason: done (`system.models.not_measured` per line, the summary says `summary_no_tokens`). Evidence order is `mutation-after`, not the spec's `test-first` (the run says so; the failing run came from reverting the fix at `436ec46`). |
| D3 Exit codes | shipped-as-specified for the 19 read verbs; browser clause **not met** | `ReadVerbExitCodesTest` (94 lines) runs 19 verbs against a runtime answering 500 and asserts exit 1; `ClientCommand`, `ConfigCommand`, `WebCommand` return `PmOperator.EXIT_FAILURE` on a non-200. Browser clause: `running-work` has no `GET /api/v1/system` route (read: `running-work.json` holds only the four `/api/v1/system/cleanup` links), so the clause cannot be met on that scenario, as the lane reports; the mock gap has no owner. Sonar: at `cfb44aef` `confirmed 0` (the run's account). |

`deliverables_done=2` of 3 is consistent with the diff (D3's browser clause).

**Surface delta** (`inbox landing-check`, declared = the 7 backticked paths of the spec's Expected Surface, realized = the 24 merge-diff
files, base `origin/main` = `be4995e5`): `expansion_detected`, 16 added, 1 declared and absent.

- **Declared, absent (1):** `pm-operator/.../SystemSnapshot.java`: not created, on purpose (the `pm-api` record is the model).
- **Added, never declared (16):** two design documents (`shared/system.adoc`, `shared/system-texts.adoc`); `RuntimeStatus.java` and
  `SystemRecordsTest` in `pm-api`; `ClientCommand`, `ConfigCommand`, `WebCommand`, `RuntimeActDialogue`, `OverviewFullScreenApp`;
  `PmOperatorProcessIT`, `SystemCommandTest`, `SystemFullScreenTest` (one-line edits); and the new tests and capture set
  (`ReadVerbExitCodesTest`, `SystemLifecycleLineTest`, `SystemViewConsistencyTest`, `26-cli-system-model.txt`). The tests and the
  capture follow from the deliverables; the production files are drift of the declaration:
  - `RuntimeStatus.fromMap` now reads `lifecycle_state` and `can_withdraw` beside `state` and `drain_withdrawable` (a wire-name
    fix: without it the status read always answered `running`); one `SystemRecordsTest` case (+11).
  - `OverviewFullScreenApp` (+49): the full-screen view reads `/api/v1/status` on entry and on refresh and keeps a flag
    `systemBannerFromEvent` so that a status read replaces a line the status wrote, leaves a progress event's line alone, and
    clears it when the runtime runs again.
  - `WebCommand` rename, revoke and listener-off, and `ConfigCommand` (set-elines), now return exit 1 on a refused request.
    These are **write** verbs; the spec said read verbs. They follow the exit-code table (`01-commands.adoc#exit-codes`, 1 =
    refused by the runtime) and are right in kind, but are not tested (layer 3).
- **Overlap to carry:** the spec's own claim label says `PLAN-30` D4 tests the exit-code table for every verb. After this
  landing the 19 read verbs and four write paths already exit 1 on a refusal; PLAN-30 D4 must be re-read before emit.

## Layer 1 detail: the specified first line

The spec writes `draining · 2 plans finish · stop in 4 min`. The run's residue says the status wire carries no drain deadline and
its remaining count counts blockers, not plans, so the line says what the runtime counted. Checked by reading:

- `RuntimeStatus` (client record) has `drainingBy`, `drainWithdrawable`, `drainRemainingCount`, `drainRemainingItems` (names) and
  `drainStartedAt`; **no deadline field**. True of the client record.
- The design's own request list (`doc/design/shared/system.adoc:365`) says `GET /api/v1/status` carries, for `draining`, "when the
  drain timeout ends". So "the wire carries no deadline" is the **client's reading**, not a verified fact about the runtime; the
  record may simply not parse a field the runtime sends. Whether the runtime sends it is in `pm-mcp-core` (not read here).
- `drain_remaining_items` (the names) is on the record and unused by the line; the design says the live form names the items.

The design now reads `draining · 2 left` and says "the runtime's count, not a count the client derives". The run changed the
specification text of its own deliverable to fit what it built. The change is disclosed (message Residue) and defensible, but it is
the operator's call whether `stop in 4 min` and `restarting` stay wanted.

**The PR body contradicts the diff:** it states `draining · 2 plans finish` in the D2 paragraph and again in the evidence table
("the after captures show `draining · 2 plans finish`"). The code, the catalogue (`# left`), the design and
`SystemLifecycleLineTest` (`"draining · 2 left"`) all say `2 left`. The body is wrong; the merge text on `main` carries the wrong
line.

## Layer 3 detail (read, not run)

Sound:

- `SystemCommand.readStatus` catches `IOException`; `JsonFormatException` and `RuntimeUnavailableException` both extend
  `IOException` (read), so a malformed or missing status never turns a served view into a failure. `OverviewFullScreenApp.fetchStatus`
  also catches `IllegalArgumentException`.
- `RuntimeLifecycleState.fromValue` maps an unknown state to `RUNNING`, so an unknown state prints no line (it states nothing it
  did not receive); every other value has a `system.lifecycle.*` key in both catalogues (diff).
- `jobKinds` skips `locks_held` and non-object entries and sorts, so a redraw does not move lines; `runningNow` counts the same
  slots as the code it replaced.
- The design's printed example matches the code (headline `Runtime pm-mcpd … · build …` then `running since …`, jobs as locks
  then one line per kind).

Gaps:

1. **The full-screen lifecycle logic has no unit test.** `SystemLifecycleLineTest` drives only the printed `system` verb
   (`execute(..., "system")`). The state rule of `systemBannerFromEvent` (replace, leave, clear) in `OverviewFullScreenApp`, 49
   lines of new state handling, is covered only by the screen-capture set `26-cli-system-model.txt`, which writes to
   `pm-operator/target/screens/` under the integration profile and is not part of the merge. The spec's D2 done-when ("a test on
   `system.draining`: the first line says draining") is met for the printed view only.
2. **The write-verb exit-1 paths are untested.** No test mentions `rename_failed`, `revoke_failed`, `listener_off_failed` or
   `setelines_failed`; `WebDevicesCommandTest` covers the success paths. The four edits are beyond the declaration and have no
   failing test.
3. **`ReadVerbExitCodesTest` fakes one refusal (HTTP 500 for everything).** It proves exit 1 on 500, not on 401/403/404, and not
   the not-running case; "not running" stays exit 0 (`system`, `client status`), which agrees with the table (0 = a read-only verb
   answered), so that is a reading, not a defect. `client status` still prints its `runtime   not running` as an English literal
   (pre-existing, not touched).
4. **The tests use hand-written literals against the Standing Constraint** ("input data comes from `Generators`, every test class
   that takes input data carries `@EnableGeneratorController`"). `SystemLifecycleLineTest` and `SystemViewConsistencyTest` carry
   the annotation but call `Generators` zero times (grep); `ReadVerbExitCodesTest` carries neither. The snapshot values, plan ids
   (`fix-relay`, `planner`) and counts are literals. The lane's account ("`@EnableGeneratorController` on the new test classes")
   holds for two of three classes and is nominal for those two.
5. **`SystemLifecycleLineTest` asserts the built line, not the specified one** (`"draining · 2 left"`), so it cannot fail on the
   spec's line; combined with the rewritten design, no document or test now holds `2 plans finish · stop in 4 min`.
6. `SystemCommand.printSnapshot` performs an HTTP request (`readStatus`) inside a rendering method; `SystemViewConsistencyTest`
   calls it without a started runtime and passes only because the failure is caught. A smell, not a defect.

Lane leads about the web, checked by reading `pm-web-app` at `be4995e5` (the residue says "the web app still ..."):

- `SystemView.svelte` writes `retry`/`retries`, `error`/`errors` and `ok` as literals (:1107, :1113, :1118): **confirmed**.
- Its load failure is stored as a string (`loadError = $t('system.view.load_failed')`, :148), so it keeps the language it had at the
  error: **confirmed as a mechanism** (whether English shows on the German page depends on when the catalogue arrived; a browser run
  would show it). This is the second lane to report the PLAN-17 lead in a different wording; the earlier "not reproduced" of the
  PLAN-17 Watch was too strong for this half.
- "The web app still formats tokens as `0.12 M`": **refuted** for this view by reading `lib/display/format.ts` (`tokenUnit`: below
  a million the unit is `k`, so 120000 reads `120 k`); the `0.34 M` of the red `epic-view.spec.js:190` test is another path.
- The `waiting for_you` raw state id: **not reproduced** (no `replace(` call in the file).

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown` (completeness check: both missing, the known opencode gap, recurrence recorded).
- Anomalies, the run's account: (1) one `perl -0pi` edit and two `grep` calls broke the lane's shell rule; (2) the pre-commit
  profile rewrites import order across about 180 untouched files on every run (the drift PR #298 landed on `main`); the lane
  reverted the churn with a reverse patch each time; (3) the routed `verify -Ppre-commit` hit the 536 s bound once
  (`timeout_source explicit`), the rerun at `--timeout 1500` finished in 472 s; the build server answered `ready` and the lane ran
  `in_process`, the recurring bare-mvn failure Watch; (4) `spec_differ: true`, the lane read the ledger's spec.
- Guard `none` on this harness (unenforced); the lane does not claim "all kept".
- Size: 24 files, under the 100-file limit of the required reviewer.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) 14 findings on the first pass, 7 on follow-ups, handled as the message states (13
  fixed, 5 answered with a reason, 3 covered by wording; the totals are the run's account). After the final push it answered
  `Review rate limited` (18:10Z); the standing review authorization was applied (qualifying review exists, findings handled, the
  last head's re-trigger refused on quota). Sourcery (optional) refused on its 7-day budget. No `skip-bot-review` label.
- CI/merge: PR #299 reads `merged`, merge commit `be4995e5` (`ci pr view`), on `origin/main`, single parent `1ce32d80` (the PR #300
  merge).
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `cfb44aef`. **Not re-corroborated:**
  `read_open_issues` for PR 299 answers `count_status: undecidable`, reason `head_revision_unavailable`, analysis revision
  `cfb44aef609632b3bfc750492b5c10cdbd015b90`, the revision the lane read. Recorded as an unverified lead, not as a `0`.
- Merge without a separate operator word: the merge used the standing authorization (conditions the lane lists held); no
  question was asked. Same practice as PLAN-17 and PLAN-46.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-26 --status shipped`
- [x] row `pr` stamped — `299`
- [x] row `landing` stamped — `landings/PLAN-26.md`
- [x] row `plan_marshall_plan_id` stamped — `26-cli-system-model` (from the sender `rb-26-cli-system-model`)
- [x] PLAN-90 re-scoped: deliverable 3 (the words of the system view's model column and its load failure) added from the confirmed web leads
- [x] epic.md narrative reconciled (Open Defect recurrence, Decisions, Watches, Queue annotation)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **Terminal specs naming a file this plan changed** (path-level match against every staged spec): `SystemCommand.java` (PLAN-42,
  48), `ConfigCommand.java` (PLAN-32), `system.{en,de}.properties` (PLAN-09, 23), `25-system.adoc` (PLAN-17 shipped),
  `shared/system.adoc` (PLAN-41), `WebCommand.java` and `PmOperatorProcessIT.java` (PLAN-76, shipped), `OverviewFullScreenApp.java`
  (PLAN-05, shipped). Re-read PLAN-23, 30, 32, 42, 48 and 41 at `be4995e5` before emit; PLAN-30 D4 (exit codes for every verb) is
  partly done and must be re-scoped.
- **The first-line decision for the operator:** keep `draining · 2 left` (the design now says so) or restore `draining · N plans
  finish · stop in M min` and `restarting`, which needs the client to read the drain deadline the design lists for the status
  (`system.adoc:365`) and the runtime to send it. Not collected into a fix plan: it is a design decision.
- **Untested paths of this landing** (layer 3, gaps 1, 2, 4, 5): collected as one deliverable of `PLAN-91` (terminal; it already
  covers the Ctrl+C handlers and the transcript goldens and touches the same module): tests for the full-screen lifecycle rule and
  for the four write-verb refusals, Generators for the new tests, and the specified line asserted by the D2 test if the operator
  keeps it.
- **The `running-work` mock lacks `GET /api/v1/system`**: the system view of that scenario can show only its load-failure line. No
  plan owns it; a mock change (`pm-mock-runtime`, `running-work.json`), carried as a Watch until a plan touching the mock takes it.
- **Web `SystemView` words and load failure**: owned by `PLAN-90` deliverable 3 (staged, not yet emitted).
- **The PR body of #299 states a line the code does not print**; the text is on `main`. Not corrected anywhere (a merged PR body);
  this report is the record.
- **`ci pr comments` unresolved count is unreliable**; the lane resolved the threads it could and dispositioned the rest in one
  comment (the known provider defect, not for this epic).
