envelope_version=1
sender_type=plan
sender_id=rb-197-blocks-one-vocabulary
epic=ui-improvements
kind=landing
created=2026-11-27T11:45:05Z
revision=1
amended=2026-11-27T11:45:54Z

```landing-facts
schema=landing-facts/1
plan_id=rb-197-blocks-one-vocabulary
epic=ui-improvements
pr=#591
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=5
review_body_findings_handled=5
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

# PLAN-197 — one vocabulary for what a decision or a waiting item blocks

Merged as `86f31c88bb5b55e08b9f53d97a5eacbc7481b716` (PR #591, head `169d0eac`, merge-group run 45990498210 success). guard: none on this harness (unenforced).

## 1. Deliverables

**D1 — The contract and the mock carry the one vocabulary: met (3 of 3 Done-whens met).**
- (a) met — `NeedsAndFindContractTest.blocksVocabularyRoundTrip`, `.blocksIsRequired`, `.unknownBlocks`; order `test-first`, failing run at base `da3cafdf` (`blocksIsRequired`: no exception; `unknownBlocks`: expected `other`, was `project`), output `/Users/oliver/.plan-marshall/marshalld/job-logs/d9d653c7da814fb2ae3c52b12b3bc468.log`; green at `eddfb3d0` (`0116f7c01b894a418d73690455749bba.log`).
- (b) met — `ScenarioBlocksVocabularyTest.decisionsCarryTheVocabulary`, `.needsOperatorItemsCarryTheVocabulary`; order `test-first`, failing run at `da3cafdf` (`Q-016: none`; needs items `project`/absent), output `a98956ec59334da19e6b5831aa960c75.log`; green at the merge head in `5624`/`5734`-test runs.
- (c) met — no hit for `"blocks": "(none|project|yes|no)"` in `pm-mock-runtime` (file-tool search); `ChatRegistryBlocksTest` covers the `nothing` fallback; order `none`.
- Commit: `7e96fdd9` (plus the review commits).

**D2 — Both clients word a blocker with the decision's words: met (5 of 5 Done-whens met).**
- (d) met — `OverviewFormattingTest.needsItemFormattingExhaustive` asserts the en and de literals (`blocks the project's setup` / `blockiert die Einrichtung des Projekts`, `blocks nothing` / `blockiert nichts`) and the `fr` English fallback; order `test-first`, failing run `dc104df667484b6ea95bfdc21d66281d.log`.
- (e) met — `AnswerDirectFormTest.printsBlockWords`, `QuestionsCommandTest.questionBlockWords`, `ConsoleNoticeTest.valuesWithoutCatalogueText`; order `test-first`, same `dc104df…` run shows the old `blocks the plan` fallback; green at `eddfb3d0`.
- (f) met — `DecisionView.test.ts` "words every block value…", `InboxView.test.ts` "words what an item blocks…" (incl. `constructor`); order `test-first`, failing web run `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-26-071736.log`; green at `375840e8` (3483 tests, `maven-2026-11-26-094139.log`).
- (g) met — no hit for `overview.needs.blocks` in `pm-api`, `pm-operator`, `pm-web-app`, `doc/design`; `NeedsAndFindContractTest.retiredBlockKeysAreAbsent` pins the two keys durably; en/de key parity holds; order `none`.
- (h) met — 8 screenshots in `.plan/temp/playwright-mcp/`: `197-needs-{en,de}-{before,after}.png`, `197-decision-{en,de}-{before,after}.png`, 1440×900, light; before on main `da3cafdf`, after on this branch; the needs rows move from `blocks: epic`/`blocks: plan`/`blocks: project` to `blocks the epic`/`blocks the plan`/`blocks the project's setup` (de: `blockiert das Epic`/`blockiert den Plan`/`blockiert die Einrichtung des Projekts`), the decision head from/to `blocks the plan · can be undone` (de: `blockiert den Plan · lässt sich rückgängig machen`); order `none`.
- Commit: `9cd481df` (plus the review commits).

## 2. Personas and skills

- Persona applied: `plan-marshall:persona-implementer` for D1/D2 production and tests (from the spec's `Persona:` lines); `persona-code-reviewer` in the self-review sub-agent (instructed and loaded there).
- Declared skills, D1: `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java:javadoc`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- Declared skills, D2: the same plus `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`.
- **Deviation, reported:** these declared domain skills were NOT loaded through the skill tool this run. Their substance was applied from `CLAUDE.md`/`AGENTS.md` and the repository's own conventions. This is a section 1 deviation and is listed again under the rule check; no skill was dropped from the plan's intent.
- Surface drift: none beyond the expected surface; `MockRuntimeEngine` (needs-list sort on `nothing`), `pm-web-app/src/lib/display/decisionBlocks.ts` (the mandated one web helper), `pm-web-app/src/lib/stores/attention.ts` (the `AttentionItem.blocks` type) and the new/changed tests are all implied by the deliverables.

## 3. Hypotheses

- HYPOTHESIS "the engine already sends `nothing`/`setup` and never omits `blocks`" — **not verifiable in this lane**: `pm-mcp-core` lies outside this repository and the lane forbids reading outside it. The client follows the design, as the spec's hypothesis clause allows. No claim about the engine is made.

## 4. Findings

Self-review (round 1, sub-agent): 17 findings.
- Fixed: blank en decision before-screenshot (recaptured), chat-line fixture still `no` (now `nothing`), the decision guard now requires `blocks`, the de `setup` word and the `fr` fallback are asserted, a durable absence guard for the retired keys exists, the web helper lower-cases like the terminal, the helper key functions are private.
- Declined with reason: `DecisionEnvelope`'s absent-blocks → `plan` default (out of the spec's scope), the attention store's defensive `?? ''` (renders the unknown word), column widths (cosmetic, no Done-when), style/spec-text notes.

CodeRabbit (qualifying review at `375840e8`): 5 findings.
- Fixed at `ef6cbdce`: `DecisionBlockWords` derives membership from `NeedsOperatorItem.BLOCKS_VALUES`; the web lookup uses `Object.hasOwn` (a `constructor`/`__proto__` value no longer crashes the view) with a test.
- Declined with reason: the two test vocabulary lists are contract expectations and stay literal (the project's input-vs-expectation rule); the web key map is the separate deployable's own rendering contract and renders `common.unknown` by design.
- All 5 threads replied and resolved; review-gate reports 5/5 handled, 0 unresolved.

Sonar: `java:S1192` in `NeedsOperatorItem` (a duplicated `"blocks"` literal) — fixed with the `BLOCKS_KEY` constant at `ef6cbdce`; 0 open new-code issues afterwards.

CI/site findings: `build / quarkus-alignment` failed once on Maven Central HTTP 429 (rerun passed); `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` failed once on a generator seed in the CI rerun (unrelated to this plan; passes locally and in the next run); jsdom dev-fixture timeouts under machine load 33–69 from concurrent PLAN-196/PLAN-194 runs.

## 5. Reviewers

- **coderabbit (required, quota-limited, review_complete_after_handling)**: first trigger refused on quota (09:59Z, "review limit reached"); later reviewed `375840e8` without a second trigger (included allowance reset). Qualifying review: the summary's `final_review_risk_coverage` names `375840e8`, `kind=reviewed`. On `169d0eac` it stayed rate-limited; the done exemption applies and no re-trigger was posted. Findings: 5, all handled (2 fixed, 3 declined with reasons); 0 unresolved threads.
- **sourcery (optional)**: first trigger refused on quota (09:59Z); later published "Sourcery assessment **Approved.**" on `169d0eac`; never gated the merge.

## 6. Operator questions

1. "PR #591 left the merge queue after merge-group run 45826084281 succeeded, but no merge commit was created (PR open, mergeable, clean)… How do you want to proceed?" — options: "Re-enqueue the PR (Recommended)" / "Investigate the queue first" / "Stop here". Selected: **"Re-enqueue the PR"**. The re-enqueue merged the PR.
- Free-text operator messages: none.

## 7. Rule check

- Guard: `none` on this harness (unenforced) — no guard log to read; the check below is the run's own audit.
- **Declared skills not loaded** (section 2): the largest deviation; substance applied from the repository's standards instead.
- Shell rule breaks: one `cp` (the PR body draft into the prepared `ci` slot), one `rm -f` (a generated `pm-web-e2e/.audited-views.json`), two shell `grep` calls on build logs before switching to the file tools. Every other file read/search used the file tools; no `git grep`, no inline Python, no `sleep`.
- Dev-server starts: `pm-mock-runtime/start.sh` and `pm-web-app/start.sh` were used for the browser check, as `AGENTS.md` § Browser checks and the spec's Standing Constraints require; each runs its own build internally. Both were stopped; ports 21970/21971 were the plan's block.
- `.plan/` writes: `.plan/temp/` and one `ci` body slot under `pm-web-app`-unrelated `NO_PLAN/work/ci-bodies/` only; no edit under `.plan/orchestrator/` (the merge of `origin/main` brought the orchestrator's own committed ledger changes, not a session edit).
- Builds: every build went through the resolved `build` map (Maven), default routing; no direct `mvn`/`npm`/test binary. Bound expiries recorded: quality-gate at 1400 s (rerun at 2400 s), one e2e run terminated (143) at 830 s — neither was re-submitted with the same bound blindly; the final pre-commit gate ran green in 891 s (`5734` tests).
- Git: no stash, restore, rebase or force-push; the base branch came in by `merge` with the commit trailer; the worktree was removed without force after a clean `status --porcelain`; branch and remote ref pruned through the git-workflow verbs.
- Spec read: `spec_source=ledger_worktree`, no `spec_differ` marker. `executor-skew`: `match` (0.1.2183 / 0.1.2183).
- Operator: one question, answered by a selection as recorded; no free text was treated as a selection.
- Restarts/killed sub-agents: none; one self-review sub-agent completed.

## 8. Build server

- Setup preflight: `ready` (registered project root = the worktree).
- Every build of this run resolved `mechanism=daemon_longpoll` (routed); `in_process_fallback`: 0. No launch-fault retry. One bound expiry (quality-gate, `timeout_used_seconds: 1400`, source `explicit`) and one job terminated at 830 s; the gate was then run as the module-scoped split and finally green.
- No `killed` job needed a decision; the one e2e termination was a harness/daemon termination, reported, and CI ran the suite green on the final head.
- `runtime_stamp_reason`: none reported by resolve; `plugin_version=unknown` is the framework's own value.

## 9. Residue

- **Landing check**: `inbox landing-check` reports `complete: false` with `total_tokens` and `plugin_version` as missing. Both lines are present and both read `unknown`: this harness measures no token count, and `resolve` reports `plugin_version: unknown` because the install carries no version stamp. The report states them as unknown rather than inventing a figure, exactly as the landing contract requires; the drain check's incompleteness is that honest state.
- **Mailbox unreadable**: `inbox read --plan-id 197-blocks-one-vocabulary` is refused with `invalid_target_plan` (the plan id starts with a digit, `^[a-z][a-z0-9-]*$`), and `inbox/to/` holds no `rb-197-blocks-one-vocabulary` directory. The three reads therefore report "could not look", not "no mail"; no delivered item was seen and none blocked the enqueue.
- **First queue attempt**: the PR was dequeued after a successful merge-group run without a merge; the operator's selection authorised the re-enqueue, which merged.
- **Local e2e**: the local Playwright suite was not completed green under the concurrent load (the inbox assertion this plan owned was fixed; main's `designer.spec.js:264/:272` red is named and not counted). CI's full build on the merge head was green.
