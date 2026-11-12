envelope_version=1
sender_type=plan
sender_id=rb-129-web-reports-fifth-residue
epic=ui-improvements
kind=landing
created=2026-11-11T06:38:54Z

## Landing: PLAN-129 web-reports-fifth-residue

guard: none on this harness (unenforced)

```landing-facts
schema=landing-facts/1
plan_id=rb-129-web-reports-fifth-residue
epic=ui-improvements
pr=#406
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=3
review_body_findings_handled=3
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

### 1. Deliverables

**D1 — Mark-all stays truthful in its races — met.**
- The rollback keeps a counted read. Test `keeps a counted read read when the mark-all request fails (D1 L3-2)`, order `test-first`: failing pre-fix at `pm-web-app/maven-2026-11-10-150744.log:460` (`data-unread="true"`), passing at `maven-2026-11-10-150935.log:293`; commit `f185238a`.
- The retention mechanism is pinned. Test `keeps the retained row and its counted read through a reload before the failed mark-all (D1)`, order `test-first`: failed pre-fix at `150744.log:477`, passes at `f185238a`; reverting the `loadReports` retention merge fails it at `maven-2026-11-10-151129.log:1386`.
- The in-flight reload is pinned. Test `keeps a reload in flight before the mark-all response from counting a read twice (D1)`, order `test-first`: failed pre-fix at `150744.log:494`, passes at `f185238a`; reverting the pending-marker guard fails it alone at `maven-2026-11-10-151310.log:448`; the current fixture's failing run is `maven-2026-11-10-163624.log:401`.
- The overstating docstring is rewritten to what the in-flight marking guarantees. The Skeleton-flash clause was re-read: PLAN-45 already sets `isLoading = !hadRows` (`ReportsView.svelte:394`) and `ReportsView.test.ts:271` asserts no skeleton on a reload, so no change was duplicated.

**D2 — RowMenu, the age text and the vacuous asserts — met.** Every change's test fails when its code is reverted (order `mutation-after`): RowMenu scroll guard `keeps its own scroll and its placement when the capped menu scrolls (D2a)` (`maven-2026-11-10-154735.log:692`); advisory suppression `drops an advisory that only repeats the visible word (L3-5)` (`154735.log:675`); completion-report focus `leaves the focus where the operator moved it while the fetch ran (L3-4)` (`maven-2026-11-10-155006.log:421`); the 30–59 day age `renders last month at 30/59 days` (`154735.log:615,616`); disabled contrast `derives the variant contrast …` (`154735.log:650`, and the pre-existing `last year` at `:633`). `row-menu.spec.js` run, order `none`: Playwright 378 passed including `row-menu.spec.js:30` at `pm-web-e2e/maven-2026-11-10-152907.log:351`. Two vacuous absence asserts deleted, order `none`. Commit `5f46c8bb`, fixes `a39adf13`/`dfe3a201`/`0d515770`/`6155b1be`.

**D3 — Tests, texts and the CLI hint — NOT fully met as written.** The Done-when demands `test-first`; only `mutation-after` failing runs exist for the two named tests. The tests themselves are present, green at `6155b1be`, and sensitive: `ReportsScenarioTest.shouldMarkAllReportsRead` fails when the mock ignores the requested ids (`ReportsScenarioTest.java:166`, job `df2d33f5`); `reportsReadAllWithOlderPage` fails without the CLI hint (`pm-operator/maven-2026-11-10-155824.log:255`). The theme labels, both designs and the message were updated, and one CodeRabbit finding re-pointed the hint from `reports --older` to `reports`. Commits `bca67b02`, `396ef322`, `0d515770`.

**D4 — Open review items answered — met.** `ci pr comments --pr-number 394` answered the review body `PRR_kwDOVBOtss8AAAABW7A4Ww` in issue comment 6772099913, one bullet per nitpick, each naming `0913d6f3`; order `none`. The open-issue read at this PR's head is a confirmed 0 (see §8).

Per-head test runs: `dfe3a201` — web 1405/1405 and `pm-api` green (`pm-api,pm-web-app/maven-2026-11-10-163832.log:637,651`); `a39adf13` — four modules green (`…/162020.log`); `396ef322`, `0d515770`, `6155b1be`, `bcbadae1` — CI green, the last also the full `verify -Ppre-commit` on its tree.

### 2. Personas and skills

- D1: `plan-marshall:persona-implementer`; skills `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D2: `plan-marshall:persona-implementer`; skills `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D3: `plan-marshall:persona-module-tester`; skills `pm-dev-frontend:jest-testing`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-documents:ref-asciidoc`.
- D4: `plan-marshall:persona-implementer`; skill `pm-dev-frontend:javascript`.
- Self-review: `plan-marshall:persona-code-reviewer`.
- `skills added: pm-dev-java:junit-core, reason: the D3 CLI and scenario tests are JUnit tests` (named in the spec's D3 line); no other surface drift. `common.de.properties` and `MessageCatalogueTest` follow operator decision 9; `ReportCatalogueTest` registers the new catalogue key.

### 3. Hypotheses

- "The retention mechanism and the in-flight reload have no test": **refuted for the retention mechanism** (`ReportsView.test.ts:804` pins the merge) and **confirmed for the mark-all in-flight reload** (only the single-read path at `:1170` existed).
- "`CompletionReportView` takes focus after the fetch although it moved": **confirmed** at `CompletionReportView.svelte:71-91` before the fix (the resolved report focused the heading unconditionally) and fixed.
- "The real `pm-mcpd` pages the unread list by offset": **not verifiable here** (another repository); the first-page reload does not depend on it.

### 4. Findings

Self-review round 1: 11 findings. Fixed: the false doc claim, the catalogue-store leak, the German intro wording, the plural forms, the incoherent reload fixture. Accepted and reported: the jsdom `scrollTop` assertion is vacuous while the cap assertion is load-bearing; green runs preceded their commits but the tree was clean. Round 2: 6 findings; fixed the causal doc clause, the store reset and the singular/plural assertions; re-recorded the rewritten reload test's own failing run (`maven-2026-11-10-163624.log:401`). Round 3: 2 open — D3's evidence order and D4's PR-head read (the latter produced by this PR's Sonar read).

CodeRabbit on `396ef322`: 3 inline threads and 1 outside-diff finding, all fixed in `0d515770` — the hint points at `reports` (the remaining reports become the first page), the notice takes the client locale, the age fixture subtracts exact milliseconds, and the theme contrast test reads the theme rules rather than the header comment. All three threads replied and resolved; the outside-diff finding answered in comment 6780341105. Dispositions: `coderabbit inline inline-1/2/3 — 0d515770`; `coderabbit outside-diff outside-diff-1 — 0d515770`.

### 5. Reviewers

- **coderabbit (required, quota-limited).** Refused on quota 16:47:12Z and 18:19:22Z; escalated trigger posted 20:48:42Z; reviewed `396ef322` at 21:42:50Z (qualifying review, 3 threads + 1 outside-diff, all handled). Re-triggered after each later push: refused 22:26:47Z, silent after the 23:0x and merge-push triggers (comment 6781708380). Standing review authorization used: qualifying review exists, every finding handled, re-trigger recorded, every other gate green on `bcbadae1`.
- **sourcery (optional).** Refused on quota (budget 1 day 4 hours at refusal); optional, not gating.

### 6. Operator questions

1. "resolve's `inbox_read_command` is rejected (`invalid_target_plan`); the mailbox dirs are `rb-<plan>`" — options {use rb-prefixed plan-id, stop}; selected **use rb-prefixed plan-id**.
2. "self-review hit its 3-round limit with two open items (D3 evidence order, D4 PR-head read)" — options {continue to the PR, stop}; selected **continue to the PR**.
3. "CodeRabbit refused on quota (no review)" — options {wait the next window and retry, stop}; selected **wait the next window and retry** (twice, first at 16:47→18:19, then the escalated trigger).
4. "Merge PR #406?" — options {merge and settle, do not merge}; selected **merge and settle**.

No free-text operator messages.

### 7. Rule check

- `guard: none on this harness (unenforced)` — nothing was enforced, so no "all kept" claim is made.
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differed at resolve time; the ledger copy was read).
- `executor-skew`: `match` (executor 0.1.2090, lane 0.1.2090).
- Rule breaks of this run, by its own actions: shell file operations were used where the file tools are required — `ls -t`, `wc -l`, `tail`, `grep -rn`/`grep -n` for content searches, `sed -n` once (i18n index), `git show`, `git log -S`, `git diff` for diff review, and `date -u` for the clock. No `&&`, `;`, pipe, loop or heredoc was used. One build log outside the repository was opened because the build result names it (`~/.plan-marshall/marshalld/job-logs/…`), which the build-gate rule prescribes.
- Restarts/killed sub-agents: none. Sub-agent round-2 first dispatch was refused for a missing `agent` argument and re-sent immediately.
- The inbox read quoted the resolve-emitted command, which the orchestrator refuses (`invalid_target_plan`); the operator selected the rb-prefixed plan-id form, recorded above — the emitted command is unfixed and is a residue item.

### 8. Build server

Setup preflight: `ready`. Every build was submitted with the default routing; each build's own `[BUILD-SERVER] resolved build` line named `mechanism=daemon_longpoll` (19 builds), with no `degraded` fallback, no `in_progress` re-attach and no `killed` job. `runtime_stamp_reason`: absent. Sonar: configured (`plan-marshall_pm-mcp-clients`), confirmed 0 open new-code issues at the merge head `bcbadae1`, quality gate `OK`.

### 9. Residue

- D3 is not fully met as written: its Done-when demands `test-first` evidence and only `mutation-after` failing runs exist. The two named tests pass and fail when the code they cover is reverted; the operator chose to continue with this stated.
- CodeRabbit's quota refused three times before it reviewed; the review is of `396ef322`, not of the final merge head, and the merge rested on the standing authorization (all findings handled, re-triggers recorded).
- Sourcery never reviewed (quota; optional reviewer).
- `resolve`'s `inbox_read_command` spells `--plan-id 129-web-reports-fifth-residue`, which the orchestrator rejects; the run used the operator-approved `rb-129-web-reports-fifth-residue` form.
