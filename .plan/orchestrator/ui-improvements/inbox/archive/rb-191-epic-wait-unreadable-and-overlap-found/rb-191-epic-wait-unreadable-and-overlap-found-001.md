envelope_version=1
sender_type=plan
sender_id=rb-191-epic-wait-unreadable-and-overlap-found
epic=ui-improvements
kind=landing
created=2026-11-26T00:19:13Z
revision=1
amended=2026-11-26T00:20:12Z

```landing-facts
schema=landing-facts/1
plan_id=rb-191-epic-wait-unreadable-and-overlap-found
epic=ui-improvements
pr=#582
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=0.1.2183
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=8
review_body_findings_handled=8
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-191 — landing

Merged as `458f9e3c55921cc49e48a3ffde201636f7e17bc8` (PR #582, branch `chore/191-epic-wait-unreadable-and-overlap-found`, deleted locally and remotely; the worktree is removed).

## 1. Deliverables

**D1 — the wait reason `unreadable` replaces `stale`, and every reason is shown**

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| the `stale` grep names no wait-reason use | met, with every surviving `stale` named in § 4 | none | `ControlCataloguesTest.epicWaitReasons`, `EpicViewCatalogueTest` | — | 035b9390 |
| the negative control and the two `waitText` tests fail before, pass after | met | test-first | `ControlCataloguesTest.epicWaitReasons`; `EpicViewCatalogueTest.shouldKeepAWaitTextForEveryClosedReason`, `shouldFormatHeadStatusAndWaitTexts`, `shouldCarryEveryEpicViewKeyInBothLanguages`; `EpicViewFormattingTest` | pre-fix tree, tests and fix carry in 035b9390: `job-logs/8e9b1430a8fd4a59bc553d0aa04599bc.log` (`expected: <[none, …, unreadable, held]> but was: <[none, …, stale, held]>`; `Key 'overview.epic.waits.unreadable' must exist … expected: <true> but was: <false>`), `job-logs/b729fa45b23c4bb8949de4c48a4bc8c3.log` (`heldRowSaysItIsHeld expected: <waits: held> but was: <can start>`, the same for `unreadable` and for a word outside the set) | 035b9390, 6e0f50ed, 4eae07b3 |
| the design texts exist and the catalogue matches them word for word | met | none | `EpicViewCatalogueTest.shouldFormatHeadStatusAndWaitTexts` (en and de, literal) | — | 035b9390, 4eae07b3 |

Browser check (Standing Constraints): ports 21910/21911, scenario `fully-onboarded`, en and de, before and after — artifacts `.plan/temp/playwright-mcp/191-after-en.png`, `191-after-de.png`, `191-before-en.png`, `191-before-de.png`. After: `relay-spec-docs waits: its specification cannot be read` / `wartet: seine Spezifikation ist nicht lesbar`, `relay-held-docs waits: held` / `wartet: aufgehalten`. Before (the case removed in the dev server, i.e. the base client): `relay-spec-docs` draws no wait, never `can start`.

**D2 — the report kind `epic.overlap_found` is removed**

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| the `overlap_found` grep names nothing outside the design's history notes | met, with the three hits this deliverable itself requires: `ReportRecordsTest.java:54`, `ReportCatalogueTest.java:78` and `:80`, `EpicViewCatalogueTest.java:230` | none | those three tests | — | 735aa6c1 |
| the negative control fails before, passes after | met | test-first | `ReportRecordsTest.shouldVerifyAll15ReportKinds` (`ReportKind.fromValue("epic.overlap_found")` throws) | pre-fix tree, tests and fix carry in 735aa6c1: `job-logs/a59b41ef2873449fa8b40357f9bb607a.log` (`expected: <15> but was: <16>`) | 735aa6c1 |

**D3 — traceability and the work package index**

| Done-when | Verdict | Order | Evidence |
|---|---|---|---|
| the traceability links stay valid | met | none | no link added or removed; `verify` green at the head; this repository's build holds no link checker, so the clause is met by "no link changed" rather than by a checker this checkout does not carry |

**Per-head test runs against pushed heads:** `package` at `4eae07b3` (job `5a9b2427`, green) and the full `verify -Ppre-commit` at every head from `6e0f50ed` to `815bc756` (green each time; the last one, job `36ca3ceb`, 661 s). `verify -Pcoverage` green at `4eae07b3`, `c84a0df3`, `a8850cd0`, `8dc030c0`, `7a85f183`. `-Pe2e` at `6e0f50ed`, `4eae07b3` and the merged base `c84a0df3`: 471 passed, 1 failed each time — `designer.spec.js:264` ("Then in force: the compiled default." while the dialog says "No copy is in force afterwards"), one of the base-branch `designer` reds the spec names at `52aa9e4b`; every `epic-view.spec.js` test passes.

## 2. Personas and skills

- D1: `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-frontend:javascript`, `pm-documents:ref-asciidoc` — all named by the spec, all loaded.
- D2: `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`, `pm-dev-java:junit-core`.
- D3: `plan-marshall:persona-documenter`; skills `pm-documents:ref-asciidoc`, `pm-requirements:traceability`.
- Self-review rounds: `plan-marshall:persona-code-reviewer` (sub-agent `ses_deccbc552ffetqRe4uWx9rpcR5`).
- Skills added for surface drift: none — no changed file lies outside the spec's Expected Surface plus the modules it names (the mock runtime and the chat registry it carries were already named by the browser-check constraint).

## 3. Hypotheses

- **HYPOTHESIS "the engine emits `unreadable` and no longer `stale` or `epic.overlap_found`" — CONFIRMED** at pm-mcp-core `origin/main` `50773c49` (#607): `doc/specification/model-work/08-epic-coordination.adoc` drops the `reground` task and the `stale` row verdict, keeps `unreadable` as the claim-section state that holds a row ("an unreadable section holds the row", "the reason is stated in the epic's view"), and removes the `epic.overlap_found` report, its watch event and the row's `overlap_found` marker while keeping `epic.waits_overlap`. The sibling checkout `../pm-mcp-core` sits on `chore/167-retarget-moved-spec-links` and still carries the pre-#607 text; the reading was taken at `origin/main`, where #607 landed — recorded in the progress file at the time.
- **Claim refuted (reported, re-scoped):** the Expected Surface's "`doc/design/web-app/epic-view.adoc` — present at HEAD". The file is `doc/design/shared/epic-view.adoc`, which owns the `overview.epic.waits.*` text table; D1's design edit went there. Second, smaller refutation: the spec's "the web epic view's wait texts in `epic-view.adoc`" — that document carries the `overview.*` keys, not the `epic.views.wait.*` family, which no design table holds (pre-existing; see § 9).

## 4. Findings

**Self-review round 1** (`persona-code-reviewer`): `EpicViewFormatting.waitReasonLine`'s comment made false by the rename — fixed in 4eae07b3; `WorkRow`'s class and `waits()` Javadoc made false — fixed in 4eae07b3; `work-actions.adoc`'s held example drifted from the terminal's reason text — fixed in 4eae07b3; German `Vorgabe` means "default" in these catalogues — changed to `Spezifikation` in 4eae07b3; the web test pinned the old text rather than the absence of a `stale` case — fixed in 4eae07b3.

**Self-review round 2:** the only open items were evidence and record items (no build yet at the reviewed head, a stale progress record, stale `de` screenshots) — all closed by the later gate runs and the retaken screenshots; no code or documentation item remained open. `self-review: clear`.

**Sonar (2 findings on new code, both fixed):** `java:S1192` on `EpicViewFormatting.waitText` (the repeated `overview.epic.waits.none` literal) — named once as `WAITS_NONE` in c9dd71cd; `java:S5961` on `EpicViewCatalogueTest` (too many assertions in one method) — the wait texts moved into their own test in c9dd71cd. The read at the head is `0` (confirmed).

**PR comments (CodeRabbit, 5 review passes; 3 threads, all replied and resolved; 8 review-body findings, 8 handled):**

| Section | Finding | Disposition |
|---|---|---|
| inline-1 | derive the every-reason coverage test from `EpicWaitReason` | fixed c9dd71cd |
| outside-diff-1 | derive the `overview.epic.waits.*` key checks from the enum | fixed a8850cd0 |
| nitpick-1 | generate the placeholder inputs in the formatting assertions | declined: the catalogue sentence is the contract and stays literal, and the plan ids used are the design's own examples, not generated input |
| inline-2 | the terminal's `waitReasonLine` dispatches on the wire string with a default | fixed a8850cd0 (exhaustive enum switch) |
| outside-diff-2 | a stored report with the retired kind can fail the whole unread page | fixed 8dc030c0, after the operator's selection (see § 6) |
| inline-3 | the mock runtime maps a decision's blocker to `yes`/`no` while the catalogue now selects on plan/epic/setup/entry/task | fixed 8dc030c0 (`blocksOf` passes the payload value through, `ChatContextNarrativeTest` pins `blocks=plan`) |
| outside-diff-3 | the bare-array form of `ReportsPage.fromJson` lacks the retired-kind guard | fixed 7a85f183 |
| outside-diff-4 | the unread count can stay above zero with no client action to clear it | declined: the count is the runtime's own figure and round-trips through the contract (`ReportRecordsTest.shouldReadEmptyPage` pins it); the client cannot clear what it does not render, and the runtime owns the stored reports it counts |

**The surviving `stale` meanings the D1 grep names** (none of them a wait reason): `enrolment.accept.refused.stale`, `system.figure.stale`, `preview_stale` (git sources), the cleanup step `stale_locks`, the stale runtime-token prose in `doc/implementation-watch/runtime-model.adoc`, the lesson-retirement reason in `client.ts`/`client.test.ts`, and the two negative-control fixtures (`EpicViewCatalogueTest`'s absence assertion for `epic.views.wait.stale`, `EpicView.test.ts`'s row with `wait_reason: 'stale'`).

**Carried in by the merge with `main`:** #585 gave `control.chat.line.decision_opened` a richer `blocks` select in the design table but left the en/de packs at the old `yes`/`other` pair, so `ChatTextsCatalogueTest` failed on the merged head. Fixed in c84a0df3 (packs follow the design) and 8dc030c0 (the mock runtime passes the blocker through).

## 5. Reviewers

| Reviewer | Required | Result | Heads reviewed | History |
|---|---|---|---|---|
| coderabbit | yes | reviewed | `4eae07b3`, `c9dd71cd`, `a8850cd0`, `8dc030c0`, `7a85f183` | triggered with `@coderabbitai review` at 16:20 on the PR's first head; each later push was reviewed automatically until the bot paused its own automatic reviews on the branch (its check is `SKIPPED` on `815bc756`); no quota refusal on this PR |
| sourcery | no | refused on its own review budget ("used your own review budget of 250,000 diff characters for the last 7 days") | none | triggered once with `@sourcery-ai review`; optional, so the refusal does not gate the merge |

Standing review authorization was not needed: CodeRabbit is `review_complete_after_handling` and holds qualifying reviews with a `kind=reviewed` marker naming this PR's heads, and every finding of them is handled.

## 6. Operator questions

1. "PLAN-191 D1 changes `EpicViewFormatting.waitText` so that an unknown non-`none` wait reason no longer renders as 'can start'… The design gives no overview text for an unknown reason. What should such a row show?" — options: *Echo the reason* (recommended) / *Generic "waits"* / *Show no label*. **Selected: "Echo the reason"** → `overview.epic.waits.other=waits: {reason}` / `wartet: {reason}`, added to the design and both catalogues.
2. "CodeRabbit raised a Major point on D2 that the design does not answer… one report stored by an older runtime with the retired kind makes the whole unread-report page fail to parse. Which behaviour should the client have?" — options: *Skip it at the page* (recommended) / *Placeholder kind in the parser* / *Accept the loss*. **Selected: "Skip it at the page"** → `ReportKind.knows` + the guard in both parse paths.

No free-text operator message was sent during the run.

## 7. Rule check

- `spec_source`: `ledger_worktree` at both resolves (setup and landing time); no `spec_differ` marker was reported.
- `executor-skew`: `match` (executor 0.1.2183, lane 0.1.2183) — read before `resolve`.
- Guard: `guard: none` on OpenCode, so nothing was enforced and no guard-log line exists to report; the rule check below is the run's own count.
- Shell/file discipline: one `cp` by shell into a `.plan/` CI body slot (the body prepared with `--for edit`, copied from the `--for create` body already written) — a shell file operation, the one deviation from "files are read and searched with the file tools". Everything else went through the file tools.
- Every build ran through the build map, routed to the daemon; every CI action through `ci`; no `gh`, no build wrapper, no bare test binary, no inline Python, no `sleep`.
- Every commit ends with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the PR body carries no attribution footer.
- The pre-commit formatter rewrote two files outside this plan in each of its runs (`MockRuntimeEngine`, `OnboardingScenarioTest`); each rewrite was reverted with the file tools, four times in total.
- One branch was merged into this one (the base, before the enqueue); no rebase, no force-push, no `git checkout --`.

## 8. Build server

Preflight at setup: `ready` (project registered). Every build of this run was routed — mechanism `daemon_longpoll` on every `[BUILD-SERVER] resolved build` line, `in_process_fallback: 0`, no degraded result, no launch-fault retry, no `killed` job.

One routing note: two combined runs (`verify -Ppre-commit,coverage`) were terminated by the machine with exit 143 inside `pm-web-app`'s npm step (jobs `d77ef611`, `f159f014`). Neither is a launch fault (`execution_failed`/`log_file_failed`), so they were not retried in-process; the gate was run as its two parts instead (`verify -Ppre-commit`, `verify -Pcoverage`), each green.

`runtime_stamp_reason`: none reported by `resolve`. `plugin_version` is `0.1.2183`, the version the installed lane text carries (`runbook_lane executor-skew` read `lane_version: 0.1.2183`, matching the executor); `resolve` itself reports `plugin_version: unknown`, so the value comes from the lane text rather than from its stamp.

`total_tokens`: `unknown`, and it is the one fact this landing could not measure — the lane has no plan directory and no metrics store, so no usage figure exists to sum. `inbox landing-check` therefore reports the message INCOMPLETE for exactly that key (it rejects `unknown` at every key by design); the gap is stated here rather than papered over with a guess or an `n/a`.

## 9. Residue

- `epic.event.overlap` (`epic_view.{en,de}.properties:11`) names no closed report kind and is referenced by no code; it is not `overlap_found`, so the spec did not name it.
- `EpicViewFormatting.waitReasonLine` returns an empty line for a reason outside the closed set (the terminal's one-line dialogue reason); only `waitText` echoes such a reason.
- `EpicViewLevel.nextLaunchable()` still treats an unknown wait word as launchable (its only caller today is a test).
- The whole `epic.views.*` key family has no row in any design text table (pre-existing; the spec's clause assumed otherwise).
- A report stored by an older runtime with the retired kind stays unread in the runtime's own count, while the list leaves it out — reported to the operator and declined as a client-side change (outside-diff-4 above).
- An enum reason this client does not know is echoed by the terminal but draws no web label; the operator's selection covered the terminal's `waitText`.
