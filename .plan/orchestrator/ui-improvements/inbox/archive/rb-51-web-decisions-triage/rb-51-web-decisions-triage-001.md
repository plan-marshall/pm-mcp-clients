envelope_version=1
sender_type=plan
sender_id=rb-51-web-decisions-triage
epic=ui-improvements
kind=landing
created=2026-11-17T10:27:23Z
revision=1
amended=2026-11-17T19:55:38Z

```landing-facts
schema=landing-facts/1
plan_id=rb-51-web-decisions-triage
epic=ui-improvements
pr=#469
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=6
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=9
review_body_findings_handled=9
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

This revision replaces the earlier "stopped before the PR" landing with the real one. The plan merged as PR #469, merge commit `deee9290356ee30eb18a38a47dca830d0850c6b4`.

## Deliverables, per Done-when

Every clause below was graded as written; the evidence order names how the failing output was obtained (`test-first`, `mutation-after`, `none`). All six deliverables are fully met; the clauses the operator superseded are listed as "not met as written (superseded)", and they do not belong to any deliverable's met set.

**D1 — triage list and detail.**
- `decisions.spec.js` on `decisions.open`: eight decisions, blocking first. Met, order `mutation-after`: the blocking term inverted, `decisions.spec.js` "lists the eight open decisions, blocking first and grouped by project" failed (`maven-2026-11-17-153453.log`). Test: `lists the eight open decisions, blocking first and grouped by project`; commit `53db20af`, fixed by `6a6d3fd2`.
- Browser check: met, order `none` (the clause declares none). `51-decisions-open-{de|en}-{light|dark}-{1440x900|390x844}-after.yml` (8 snapshots), `51-decisions-detail-*` (8), the screenshot `51-decisions-open-after.png`, and the matching `-before` set from the pre-change `main` checkout. 8 rows in 2 project groups at every combination, `data-columns="2"` at 1440 and `=1` at 390.
- "Group by project as a toggle", "blocks/reversible marks in the row", "recommended option in the row": not met as written (superseded by the operator's 2026-11-10 form).
- Detail without the repeated description, page on `TriagePage`, layout toggle gone: met (`DecisionView.svelte`, tests `shows the subject, phase, path and why it is there`, `pageWidth.test.ts`).

**D2 — keyboard and advance.**
- e2e: `Enter` decides, the next is selected, the line names the option. Met, order `mutation-after`: the key match reverted, "Enter decides a decision with a chosen option, names the answer and selects the next" failed (`maven-2026-11-17-153453.log`); a second mutation run after the review used the same test. The successor is read from the page, not a fixture id (`45ca23a4`).
- Browser check: met, order `none`: `51-decisions-enter-decides-*` (8 snapshots) and the screenshot, both languages: `Decided: Rebase onto main` / `Entschieden: …`, the successor in the object column, `/decisions/<successor>` by replaceState.
- `1`–`9`/`c`/`l` and the toast: not met as written (superseded).

**D3 — recommendation and evidence.**
- Unit tests: label, fields, the fallback. Met, order `mutation-after`: the folded JSON reverted, `folds a structure it does not know, expanded and as JSON` failed (`maven-2026-11-17-165328.log`). Tests: `names the block and every field with a plain label`, `shows a file with the lines it names`, `folds a structure it does not know`, plus `strips the control sequences…`, `keeps two keys that differ only by control sequences apart`, `folds a file record that carries more than the shortcut draws`.
- Browser check: met, order `none`: `51-decisions-detail-*` and `51-decisions-answered-elsewhere-*` (16 snapshots), `51-decisions-detail-after.png`, `51-decisions-answered-elsewhere-after.png`.
- `EvidenceView` in `doc/design/web-app/components.adoc` with its picture (`5ce5a9ae`, corrected `b7f657b1`).

**D4 — own answer confirmed.**
- e2e: own answer; a `cli` broadcast shows the console line. Met, order `mutation-after`: the own-answer attribution inverted, "answers a decision in the operator's own words with a read-back" failed (`maven-2026-11-17-153453.log`); "a decision answered from the console says where it was answered" passes with the console line (`answered in the console, terminal-01: …`).
- Browser check: met, order `none`: the same `answered-elsewhere` snapshots; the after phase reads `answered in the console, terminal-01: Deprecate them, delete in 0.11` / `beantwortet in die Konsole, …`, while the before phase (pre-change main) reads `Decided: …` — the defect this deliverable fixes, visible in `51-decisions-answered-elsewhere-before.png`.
- Limit: the daemon does not echo `request_id`, so the client keeps a pending-own-answer fallback; the console origin is rejected first (`52326315`). The own-answer path is proven against the mock, not the real daemon.

**D5 — one notification for a burst, none after a reconnect.**
- Unit tests with a fake clock: silent prime, burst of three → one that opens the list, single → that decision, answered-before-show dropped, background-tab gate. Met, order `mutation-after`: the prime reverted, `raises none for what a start, a reload or a reconnect finds by reading everything again` failed (`maven-2026-11-17-125558.log`); commit `9841c025`.
- e2e through the mock's events: met, order `mutation-after`: the prime reverted, "announces none for what a reload finds by reading everything again" failed (`maven-2026-11-17-130006.log`); the spec now reports the page hidden so the background-only notification is exercised (`b5861777`).
- The `web-moving-between-plans` row: met, order `none` — PLAN-83 is superseded and PLAN-59 is staged, so the row stays `open` and now records that the notification part landed and who sets it done.

**D6 — chat guards pinned, two replies true.**
- Tests that fail when each guard is reverted. Met, order `mutation-after`: per-decision marker (`maven-2026-11-17-114339.log`), `takeProposedAnswer` revision (`-115119.log`), `endTheChat` generation (`-115344.log`); the formerly skipped late-load and late-send flows run too. The `chatBusyToken` check itself is unreachable through the UI (controls are disabled while busy); the observable property is pinned by the cross-decision test — recorded as a note, not a met clause.
- The PR-319 threads and the `format.ts` decision: met, order `none` — threads `PRRT_kwDOVBOtss6z9Ddl` and `PRRT_kwDOVBOtss6z9Dds` carry corrections on 2026-11-17; `texts-and-languages.adoc#numbers` records the first paint and `format.ts` implements it.
- The landing facts: met — this message states `unresolved_review_threads=0` and `sonar_open_new_issues=0`.

## Personas and skills

`persona-implementer` (production), `persona-module-tester` (unit), `persona-integration-tester` (e2e), `persona-documenter` (design docs), `persona-code-reviewer` (both self-review rounds and the PR findings). Declared domain skills per deliverable: `pm-dev-frontend:javascript`, `javascript-security` (D1, D2, D4), plus `css` and `jest-testing` (D3, D5, D6) — `skills: none added` for the review rounds; surface drift recorded: `pm-mock-runtime` (D4's echo and the compile fix) and the four e2e specs the layout change touched (chrome, navigation, showroom, theme-readability) plus the text-guard baseline.

## Hypotheses

- The answer endpoint carries a client request id — **refuted at the spec's re-grounding**; implemented as the D4 contract change the operator chose.
- `PremiseEvidenceView` can carry the evidence — **refuted**; `EvidenceView` built.
- `DecisionView` does not sit on `TriagePage` and creates `Notification` inline — **confirmed** (the announcer now lives in the store).

## Findings

- Self-review round 1 (independent agent, `persona-code-reviewer`): 15 items. Fixed: the notification must not fire while the tab is in front (and the permission text is true), the "recommended" marker behind the costly/irreversible disclosure, the filtered selection as a pinned row, `EvidenceView` control-sequence stripping, its `h2` heading, dead `clearTimer`/`ownRequests.revision`, doc/SVG details, and three restored assertions (revision drops answers, own-words for one part, no permission banner before the first decision) plus the e2e tab title. `761a467d`.
- Round 2: 5 items. Fixed: the restored revision test was vacuous (now answers both parts first and asserts part 1 cleared), the part question rendered twice (the paragraph now only while a part waits), the restored own-words test did not assert the recorded answer, `plainLabel` stripping, and the notice-dismissal test. `b7f657b1`.
- PR #469 (CodeRabbit, review at `4b01a`): 9 inline findings + 1 outside-diff. Fixed: keyed-each identity, the file shortcut losing fields, part-scoped option labels, list reconciliation after a successful answer, the successor from the displayed order, the `UNCLEAR` readback, the burst's parallel posts, and the explicit `cli` origin rejected before the id fallback. Answered: the id-less own-answer fallback (the operator's decision, a stated limit) and the `value` part without options (the producer contract defines no value input; a limit). All 9 threads resolved after the reply.
- CI: one `pm-api` generator-driven `EntryRecordsTest` failure in the sonar-build job on `4b01a` (the known flaky case, the same one PLAN-66's PR records); the rerun was green, and every later head was green on the first run.

## Reviewers

- `coderabbit` (required): reviewed head `4b01a` with 9 inline findings and 1 outside-diff finding; every one fixed or answered, all threads resolved; the review is the review of this PR under `review_complete_after_handling`, so no re-trigger was posted for later heads. Quota refusals: one at 16:57Z ("next in 2 minutes") before the completed review; the policy's 90-minute wait was not needed after the review landed.
- `sourcery` (optional): refused — "your pull request is larger than the review limit of 150,000 diff characters". Optional, so not a gate.

## Operator questions

The two questions of the earlier session, unchanged: (1) which form binds D1/D2 — selected "amendment + design docs"; (2) which deliverables — selected "all six as written". This session asked none; the resume brief settled the scope, the fallback and the evidence expectations.

## Rule check

The guard reports `none on this harness (unenforced)`; the checks below are the run's own.

- **Previous session, carried and still true:** the first implementation pass edited the MAIN CHECKOUT (10 tracked files + 2 new), restored with `git checkout -- <paths>` and `git clean -f -- <paths>`, both forbidden; the shell was used for file reads (`ls`, `grep`, `wc`, `sed`); three test edits used heredocs/inline Python. None repeated here.
- **This session:** two shell file listings (`ls -la .plan/temp/playwright-mcp | grep '51-'`, and the same with `-c`) to confirm the screenshot files exist — the Glob tool does not traverse `.plan`, which is why the check went to the shell; one `page.waitForTimeout(2000)` in the MCP browser while diagnosing a page that had not loaded (an arbitrary wait, forbidden in tests; this was a manual check, not a test); one `git add -A pm-web-app/src/lib doc` (a whole-directory stage, broader than by name; the resulting 8-file commit was reviewed); `curl` was used for the mock's control route (pairing/state) outside the executor. Every other shell call was a single command; no `&&`, pipes or loops; no heredoc, no inline Python.
- **Executor discipline:** every build through the resolved `maven` notation, every CI action through `ci`, the spec read from the ledger worktree (`spec_source: ledger_worktree`, no `spec_differ`), `executor-skew: match` (0.1.2135 = 0.1.2135).
- **Git:** no `checkout --`, `restore`, `stash`, `rebase`, `clean` or forced push this session; the cleanup used `git worktree remove` without force after a clean status, `git-workflow prune-local-and-remote-ref`, and `git-workflow switch-and-pull` to move main.
- **Mailbox:** the inbox read verb refuses this plan id (`invalid_target_plan`, digit-leading id) and the on-disk mailbox is absent — the mailbox could not be looked at, which is not the same as empty. No disposition lines are possible; the landing states it.
- **Base tree:** a detached `51-web-decisions-triage-base` was created at `dd0e0047` and removed unused (its executor was absent, so it was not used as evidence); the browser "before" phase was taken from the pre-change `main` checkout instead, and that is what the screenshots are.
- **Sub-agents:** two self-review dispatches to the same agent session; it edited nothing.

## Build server

- Setup preflight: `ready`. Gate and unit runs were routed (`daemon_longpoll`); the e2e and the scoped runs that need the plan's ports in the environment ran `in_process_fallback` (the daemon takes its own environment, which cannot carry `PM_MOCK_WEB_PORT`/`PM_WEB_PORT`). No launch-fault retry, no `killed` job; several builds queued behind another session's for up to ~530 s.
- Green heads: gate `d90e463f`-head `45ca23a4`, full verify at `52326315` and at the merge head, e2e `maven-2026-11-17-190142.log` (454 passed, 75 skipped), unit suite 2 904+ cases.
- The merge of `origin/main` twice: PLAN-52 (`f7da05c7`) and PLAN-66/PLAN-163 (`fc307245`); both auto-resolved. The second merge exposed two e2e assertions that read fixture ids and a hidden-by-substring option; both rewritten to read the page (`45ca23a4`).

## Residue (open, reported)

- **Preselection** for a reversible decision with a unanimous or clear-majority vote (`decisions.adoc#recommendation` row 1) is not implemented; the recommendation is marked only. Not in the operator's residue list; changing it now would change agreed behaviour and the captured evidence.
- Own words use `TextArea`, not `Compose`; a disabled _Decide_ does not show its reason in `FormStatus`; "waiting since" shows the clock, not the elapsed time — design gaps of the same family, out of this run's settled scope.
- The `value` answer type has no control (the producer contract defines no value input); the chat side column has no e2e of its own (PLAN-137); the mock's only non-blocking decision is also its newest, so the e2e cannot separate the two orders alone (the unit fixture does).
- The `chatBusyToken` clear is unreachable through the UI (controls disabled while busy); the property it guards is pinned by the cross-decision test.
- The "before" browser phase comes from `main` at `f7da05c7`, not from this branch's own base sha `dd0e0047`; the decisions page is identical there for the checked targets.

## Self-review verdict

`budget-exhausted` is recorded because the round ended with the residue above open — not a session budget. Rounds 1 and 2 fixed every actionable finding; what remains is a named set of design gaps outside the operator's settled residue and one operator-decided fallback, reported rather than silently dropped. No round was stopped for context.
