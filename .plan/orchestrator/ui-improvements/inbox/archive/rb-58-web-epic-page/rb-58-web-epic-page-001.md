envelope_version=1
sender_type=plan
sender_id=rb-58-web-epic-page
epic=ui-improvements
kind=landing
created=2026-11-16T16:19:10Z

```landing-facts
schema=landing-facts/1
plan_id=rb-58-web-epic-page
epic=ui-improvements
pr=#456
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=7
review_body_findings_handled=7
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-58 — the epic page, on the runbook lane

Merged as #456 (`8fe7f327ef3bf013088ad7d0c1e424842c547dd2`). The epic is an object page: head on `ObjectPage`, one `ActionBar` from `byState`, the tokens line with its one-step disclosure, and the five tabs of D6-1. `deliverables_done` is 3 of 4: deliverable 2's "a plan opened to the right with the epic as list column" is not met — the two-column chain is PLAN-144's, and PLAN-144 has not landed. The operator chose that scope before implementation.

## 1. Deliverables

**D1 — Tree and summary** — met, order `mutation-after`.
- `pm-web-e2e/tests/epic-view.spec.js` "draws the plans with the title first, the id as a chip and the wait with its since"; unit twin `EpicView.test.ts` "draws the workstream plans…". Failing run against the reverted fix: `.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-16-094734.log` (e2e) and `…/pm-web-app/maven-2026-11-16-094409.log` (unit). Green at `b567064e` (e2e `maven-2026-11-16-121844.log`, 427 passed) and at the merge head `53bdb8e3` (`maven-2026-11-16-152316.log`, 432 passed). Commits `2dc584dd`, `b08a9774`, `1dd7145a`, `b567064e`, `c0720779`.
- Browser check, order `none`, artifacts in `.plan/temp/playwright-mcp/`: `58-web-epic-page-control.epic-before-1440-en-light.png`, `…-control.epic-after-1440-en-light.png`, `…-control.epic-before-390-de-dark.png`, `…-control.epic-after-390-de-dark.png` (the "before" pair from the `origin/main` app on 20581 against the mock on 20580, the "after" pair from this branch), plus the accessibility-tree snapshots. `control.review`: `…-control.review-before-{1440-en-light,390-de-dark}.png` and `…-control.review-after-{1440-en-light,390-de-dark}.png`.

**D2 — Tabs and Intent (D6-1)** — met except the two-column clause, order `mutation-after`.
- The unit cases (`five tabs…`, `writes the chosen tab…`, `opens the Intent tab…`, `shows the Reviews tab counts…`, `leaves the Reviews tab without a badge…`, `draws the list-column form…`, `marks an ended epic…read-only`) and the e2e cases (`shows the five tabs…`, `writes the chosen tab into the address…`) fail against the reverted strip/tab-write and pass at `53bdb8e3`.
- **Not met:** "a plan opened to the right with the epic as list column". The epic renders its compact list-column form when the address opens a plan in place, but no plan column stands to its right and clicking a plan still opens the plan page. PLAN-144 owns that chain. The browser clause inherits the same gap for the list-column targets.
- Browser check for the tabs: the same `control.epic` after-shots show the five tabs; order `none`.

**D3 — Counts and findings review** — met, order `mutation-after`.
- `EpicView.test.ts` "counts the landings from the shipped rows…", "opens the findings review on its first open item, with the counts the card shows"; `FindingsReviewModal.test.ts` "opens on its first open item, never on a decided one" and "opens on its first open item again after a parent closed it". Failing run: `maven-2026-11-16-094409.log`. Commits `30000855`, `2dc584dd`, `1dd7145a`, `c0720779`.
- The review's browser evidence is the after-shots above plus the head's own e2e captures `pm-web-e2e/test-results/theme-modal-{light,dark}-{desktop,mobile,narrow}.png`.

**D4 — A failed epic-resource read** — met, order `mutation-after`.
- `EpicView.test.ts` "shows the error of a failed epic-resource read, retries it, and stops claiming the read runs" and "says a rejected refresh failed while the last read stays on screen". Failing run: `maven-2026-11-16-094409.log`. Commits `30000855`, `2dc584dd`, `c0720779`.

Per-head tests: `pm-web-app` 2784 unit tests and `pm-api`'s suite at `b567064e` and at `53bdb8e3`; Playwright `-Pe2e` on ports 20580/20581 at `1dd7145a` (427), `53bdb8e3` (432).

## 2. Personas and skills

- Personas: `plan-marshall:persona-integration-tester` for D1 and D2 (their rows and tabs are end-to-end behaviour), `plan-marshall:persona-implementer` for D3 and D4; `persona-code-reviewer` for both self-review rounds and for triaging every PR comment; `persona-module-tester` applied as the standard for the unit tests.
- Domain skills: D1 and D2 `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security`; D3 adds `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`; D4 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- Skills added for surface drift: `pm-dev-frontend:javascript` and `pm-dev-frontend:jest-testing` again for the mock scenario fixtures (`pm-mock-runtime/…/scenarios/*.json`) and the pm-api `WorkRow` contract test — reason: the row's `wait_since` needed a served fixture and a JSON round-trip test outside the spec's Expected Surface.

## 3. Hypotheses

- Claim 1 (PLAN-47 landed, so the re-grounding's "not landed" was contradicted): confirmed — `pm-web-app/src/lib/actions/byState.ts:249` (`EPIC_ACTS`) and `WorkControlPanel.svelte:831` (the `ActionBar`).
- The head "Landungen" count can differ from the rows: confirmed — the old view printed `resource.landings` while the list derived from rows; the view now counts the shipped rows (`EpicView.svelte`, `landings`).
- `FindingsReviewModal` does not select the first open item: confirmed — `selectedId` started `null` with no selecting effect before this plan.

## 4. Findings

- Self-review round 1 (19 findings): fixed the ended-epic read-only gap, the analysis mislabel, the design text tables and the replaced tokens key, the vacuous "titles first" fixture, the shipped-only landings, the tab/panel a11y wiring, the selected-item marking, the German term, the separators, and the stale data-table guard.
- Self-review round 2 (12 findings): fixed the review closing with the epic, the un-clearable badge, the `epic-alongside` naming, two a11y assertions, the `merged`/`completed` mapping, the separator guard, the scenario title parity, and the advisory key name.
- CodeRabbit, 7 inline findings on `b567064e`, all handled in `c0720779` and their threads replied and resolved (`coderabbit inline inline-1…7`); `review-gate` at `53bdb8e3`: 7 of 7 handled, 0 unresolved threads.
- SonarCloud: one open new-code issue at `b567064e` (`typescript:S5906` in `EpicView.test.ts`) — fixed in `c0720779`; re-read at `53bdb8e3`: 0 confirmed, quality gate passed.

## 5. Reviewers

- `coderabbit` (required, quota-limited, `review_complete_after_handling`): reviewed `b567064e` (marker `coveredCommitId=b567064e`, `kind=reviewed`); 7 findings, all fixed in `c0720779` and answered. Standing authorization applies: later heads raise no obligation, so it was not re-triggered. Its own `@coderabbitai review` reply on the later head said "Already reviewed the last commit".
- `sourcery` (optional): refused on its own review budget ("used your own review budget… request another review in 2 days 22 hours"); never gating.

## 6. Operator questions

1. "PLAN-58's binding text (D6-1) requires the epic 'as a list column, while one of its plans is open to its right' … PLAN-144 has no landing file and is not in flight … How should I proceed?" — options: {Epic's own column now, report the list-column clause not met; also wire the two-column chain here; stop as blocked}; **selected: "Epic's own column now, report the list-column clause not met"**.
2. "Cleanup step 1 refuses: `git worktree remove` reports modified or untracked files — and the only entries are the six shared orchestrator-ledger files that come through the worktree's `.plan` symlink …" — options: {Leave the worktree, report cleanup_owed; Force-remove the worktree; Remove the six ledger modifications first}; **selected: "Force-remove the worktree"** (done; main pulled to `8fe7f327`).

Free-text operator messages: none beyond the two option selections.

## 7. Rule check

- `guard: none` on this harness (unenforced) — no pre-tool-use hook recorded anything, so nothing is claimed as "kept".
- `executor-skew`: `match` (executor 0.1.2135, lane 0.1.2135) — answered, recorded, no question.
- Spec read: `spec_source: ledger_worktree`; no `spec_differ` marker was reported at either resolve.
- Shell rule breaks: one — a `curl` was issued directly for the `/mock/goto/<state>` and `/mock/pairing` calls the browser check prescribes (AGENTS.md § Browser checks), which is not an executor script but also not one of the forbidden classes; recorded here so it is not hidden. Everything else ran through the executor, the build seam and `ci`.
- Builds: every build went through the resolved commands with the default routing; mechanisms were `daemon_longpoll` except the base tree's `in_process_fallback` (its own executor is absent) and none degraded. One bound expiry (1400 s) was recorded and the gate re-run with a higher explicit bound rather than re-submitting the same one. No `killed` job.
- Restarts, killed sub-agents, re-dispatches: none; the two self-review rounds are one sub-agent continued once.
- Write-boundary: no file under `.plan/orchestrator/` was written. One merge staged six ledger files (through the shared `.plan` symlink) and they were unstaged before the merge commit, so the PR carries none of them.

## 8. Build server

- Setup preflight: `ready` (registered, version 1). Per-build mechanisms: `daemon_longpoll` for every routed build; one `in_process_fallback` — the detached base tree's compile, whose executor the seam reports `absent` (that tree exists only for the browser baseline).
- Fallbacks and retries: `none` degraded, no launch-fault retry. One `timeout` bound expiry on `verify -Ppre-commit` at 1400 s (`timeout_source: explicit`), re-run at 2400 s and green.
- `runtime_stamp_reason`: absent; the stamp's `plugin_version` is `unknown` because the flat install's dist-manifest carries none.

## 9. Residue

- **D2's two-column clause** — PLAN-144 must land before the epic can stand as a list column with the plan's column to its right; the epic side is ready and tested.
- **`wait_since` on the wire** — the client contract and the mock carry it; the daemon (plan-marshall-mcp / pm-mcp-core) must serve it for the since to appear outside the mock. Until then the client shows no since rather than a wrong one.
- **The epic's analysis** — the runtime serves no per-landing analysis; the Landings tab now says so (`epic.views.analysis_missing`) instead of leaving the gap ambiguous.
- **The project hop** — the epic page draws no path of its own by design; the top bar's breadcrumb returns the ancestor segments with PLAN-144.
- **`pm-web-e2e/tests/data-table.spec.js`** — its stale visible-word assertion was repaired here (the row menu has been an icon button since PLAN-149); it was outside this plan's surface and is recorded in the PR body.
- **Cleanup** — the plan worktree was force-removed on the operator's selection; the branch is pruned locally and remotely and main is at `8fe7f327`.
