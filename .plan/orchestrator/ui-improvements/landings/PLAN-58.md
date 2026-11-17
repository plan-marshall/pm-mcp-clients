# Landing Analysis: PLAN-58 — Web epic page (object page, five tabs, counts, failed-read pattern)

epic: ui-improvements
workstream: WS-03
pr: #456 (https://github.com/plan-marshall/pm-mcp-clients/pull/456)

> Landing record, written by `analyze` on 2026-11-16 from the inbox message
> `rb-58-web-epic-page-001.md` (a lead, not a fact), the lane's progress file
> `.plan/temp/rb-58-web-epic-page/progress.md`, git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `8fe7f327`.

Merged as `8fe7f327ef3bf013088ad7d0c1e424842c547dd2`, merge head `53bdb8e3`, 36 files, +1375/-967. Sender `rb-58-web-epic-page`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 — tree and summary: titles first, id chip, wait with its since | shipped | `EpicView.svelte:970-1016` (title, `Chip`, `StatusLabel`, wait with `<time datetime>`, PR); `EpicView.test.ts:298` and `epic-view.spec.js:100` fail on the old id-first row; the since assertion at `EpicView.test.ts:307` accepts any digit and is near-vacuous, only the `datetime` attribute (`:306`) carries the check |
| D2 — five tabs and Intent (D6-1) | shipped except the list-column clause | tabs derived from `EPIC_TABS`, Plans default, `tab=` written by a replace (`runtime.ts:1547`, `history.ts:188-198`, default tab omitted by `formatAddress`); `EpicView.test.ts:349-447,389-403` would fail on a push |
| D2 clause — "a plan opened to the right with the epic as list column" | **not met**, as the lane says | `EpicView.svelte:74,922-926` render only the compact list form; no plan column; `openPlan` (`:287`) still pushes the plan route; the e2e (`epic-view.spec.js:286-299`) asserts only the compact form. The clause was not satisfiable by this plan: the column engine is PLAN-144's (the spec itself names PLAN-142/144) |
| D3 — counts and findings review | shipped | landings counted from shipped rows with a PR (`EpicView.svelte:226`); `EpicView.test.ts:319-333` sets `resource.landings=9` against one shipped row and fails on the old count; `FindingsReviewModal.svelte:72` selects the first open item, `FindingsReviewModal.test.ts` "never on a decided one" and "again after a parent closed it" |
| D4 — a failed epic-resource read | shipped | `EpicView.svelte:261,763-775,909-918`; `EpicView.test.ts:241-271,678-698` fail on the old code |

`deliverables_done=3` of 4 is borne out. The operator chose this scope before implementation (operator question 1 of the lane's report).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; 36 files across Svelte, TypeScript, the Java contract, the mock scenarios and the design documents. The orchestrator re-read the points marked on `8fe7f327`.
- **Verdict:** layer 1 findings (two majors: the list-column clause and the `wait_since` datum were not satisfiable inside the stated surface); layer 2 findings (one major: the PR carries six ledger files, contrary to the lane's own claim); layer 3 findings — no blocker, 2 major (re-read one), 8 minor, 1 info.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | D2's list-column clause is satisfiable by this plan | checked | refutes | no column engine at the base: `openObject` is set only from the address (`runtime.ts:574`); the spec's own Dependencies line names PLAN-142/144 |
| L1-2 | D1 "waits show since" is satisfiable inside the Expected Surface | checked | refutes | `WorkRow` and `EpicWorkRow` carry no `wait_since` at the base; the Expected Surface omits pm-api, the mock scenarios and `client.ts` |
| L1-3 | Objective/D1 "workstreams as a tree left, plan summary right" | checked | refutes | the 2026-11-10 re-grounding lists that layout under "Removed or replaced" |
| L1-4 | D4 "the error state of deliverable 3's pattern" | checked | refutes | D3 is counts and review; the pattern is `ErrorState` from PLAN-45 |
| L1-5 | the spec was emitted with its gate open | checked | refutes | the unpark line reads "Emit after PLAN-144, PLAN-146 and PLAN-156 (FindingsReviewModal first)"; the Staging Note says it "runs after" PLAN-156; PLAN-144 and PLAN-156 had not landed. **Orchestrator note:** the plan was emitted and run with that gate open; whether the operator waived it is not recorded in what this analysis read, and the spec text was never amended. The lane's operator question 1 settled only the scope of D2 |
| L1-6 | Claim 1 (PLAN-47 landed, so the re-grounding's "not landed" is contradicted) | checked | confirms | `byState.ts:249` `EPIC_ACTS`, `WorkControlPanel.svelte:827,831` |

**Layer 1 verdict:** findings — a spec bound to two plans' work (the column engine, the modal restyle) and to a datum outside its surface.

### Layer 2 — Implemented

Unexplained files and unborne claims:

| Item | Verdict | Evidence |
|------|---------|----------|
| **The PR carries six files under `.plan/orchestrator/ui-improvements`** (`queue-view.md`, `queue/PLAN-52.json`, `PLAN-53.json`, `PLAN-58.json`, `PLAN-60.json`, `resume_anchor.md`) | **major — the lane's claim "the PR carries none of the ledger files" is false** | re-read: `git diff --stat 8fe7f327^1 8fe7f327 -- .plan` lists the six, 38 insertions and 38 deletions; `queue/PLAN-58.json` changes `running` → `staged`. The lane's own report says it unstaged six ledger files before the merge commit; the merged result still has them |
| `wait_since` on `WorkRow.java:53`, `ControlResourcesTest`, `WorkRowTest.java:75-80`, both scenario JSONs, `client.ts:533` | explained | recorded in the lane's report; JSON round trip confirmed (`toJson` writes it only when non-null `:103`, `fromJson` `:157`); the daemon serving it is not shown |
| `FindingsReviewModal.svelte:189-191,683-692` (a new open-count element and a phone overlay-scroll rule) | **unexplained** | the spec's Ownership and Staging Note reserve the modal's restyle for PLAN-156; no deliverable names these |
| `pm-web-e2e/tests/data-table.spec.js:55` (repair of a stale visible-word assertion) | explained by the lane, outside this plan's surface | the PLAN-166 residue named it |
| `doc/design/shared/epic-view.adoc:64` | the design is edited to fit the code | the Landings tab no longer promises "the epic's analysis"; this weakens the design in the PR rather than the code |
| evidence order | all four deliverables report `mutation-after`; the spec fixes `test-first` for D1 to D4 (`spec-58.txt:36,43,50,56`) | the order deviates from the spec |

Test quality:

| Test | Finding |
|------|---------|
| `EpicView.test.ts:307` | the since assertion `/last|ago|since|\d/` accepts any digit; near-vacuous |
| `FindingsReviewModal.svelte:189` | "its counts match the card" is not enforced: the modal's figure comes from `review.items` (`:59`), the card from `resource.findings.open` (`EpicView.svelte:851`); they agree only because the fixtures are chosen to |
| `EpicView.test.ts:636-656` | "an ended epic is read-only" checks only `action-next` and the review buttons; `WorkControlPanel` gets `ended` from `resource?.state` only (`:711`) while the page's `ended` also uses `epic.state` (`:105`) |
| `EpicViewCatalogueTest.java:93` | the required-keys list omits `control.review.open_count`, `control.epic.next_unreadable`, `epic.views.tokens_detail.title`, `epic.views.review_watches.title`; en/de parity holds by reading |
| `EpicView.svelte:407` | `COUNTED_STATUSES` omits `handed_back` and `ended`, and the catalogue has no key for them; a workstream's counts line drops those rows and `EpicView.test.ts:631-633` asserts the drop |

Not shown by reading: the failing-before logs, the browser-check screenshots, the 432 e2e / 2784 unit counts, Sonar (the Sonar MCP search for pull request 456 returns no issues), CodeRabbit.

**Layer 2 verdict:** findings — 1 major (six ledger files in the merge, a false claim), 1 major unmet clause (named by the lane), 5 minor.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `EpicView.svelte:237-246` (re-read: `epicId` is `$derived` at `:38`; `loadEpic(true)` runs in `onMount` only, `:246`; the other calls are event refreshes and act callbacks) | the epic route changes from A to B in place (`CommandPalette.svelte:212` `openEpicView(B)`; `AppRouter.svelte:65-66` keeps one `EpicView`) | the address and `epicId` change but the head, rows, intent and acts stay epic A's until an event refresh; `EpicIntentPanel` also reads `epicId` once (`EpicIntentPanel.svelte:51-74`). The PLAN-146 landing named this ("PLAN-58 must handle epicId"); the diff does not touch it | major |
| L3-2 | `runtime.ts:1182` (re-read: `objectTab` is set from the address at `:573` and `:1351`, cleared only by a new address; no reset in `applyRoute`) | Reviews tab on epic A, then Running Work, then epic B | B opens on Reviews and the address carries `?tab=reviews`; a plan's Intent tab carries into an epic the same way | minor (leaf-checked, the reset was not found by search) |
| L3-3 | `EpicView.svelte:256,269,276` | two overlapping loads, A rejected finishing last | the stale banner stands beside current data; an older success can overwrite a newer resource | minor |
| L3-4 | `EpicView.svelte:260,766-773` | retry from the stale banner | the banner vanishes at once with no busy state; an event refresh on a failed initial read blanks the `ErrorState` and the bar says "still being read" while in flight | minor |
| L3-5 | `EpicView.svelte:763,909`; `ObjectPage.svelte:29-44` | `?open=plan:x` with a failed resource read (compact form) | neither the stale banner nor the `ErrorState` is drawn while the bar says "could not be read", with no retry | minor |
| L3-6 | `Tabs.svelte:126`; `EpicView.svelte:777,781,799,845,891` | screen reader | tabs carry no id and the panels no `aria-labelledby`, so `role=tabpanel` has no name; roving tabindex, `aria-selected`, `aria-controls` and arrow/Home/End are correct | minor |
| L3-7 | `WorkControlPanel.svelte:174`; `EpicView.svelte:277-279` | a failed refresh | the stale resource is kept, `scopeKnown` stays true, "Next plan" stays enabled and names `nextPlanId()` from the stale read | minor |
| L3-8 | `EpicView.svelte:988` | a non-ISO `wait_since` from the daemon | an invalid `datetime` attribute; the relative text is computed once per render and does not tick | info |
| L3-9 | `FindingsReviewModal.svelte:72,100-108` | a refresh removes the selected item mid-edit | the effect does not clear `dropReason`, `instruction`, `editing`, `promoting` (`select()` does); the next item may inherit the typed instruction; whether the daemon removes items is not shown | minor, not shown by reading |
| L3-10 | `EpicView.svelte:766,736,857,877,950,972` | — | plain `<button>`s where `Button.svelte` exists (the retry and review-open buttons are new or restyled here) | minor |

**Layer 3 verdict:** findings — no blocker; 1 major (an epic switched in place keeps the old epic), 8 minor, 1 info.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; text sinks only | conforms | no match; PR links guarded to http(s) by `pullRequestUrl` (`EpicView.svelte:454`) |
| CLAUDE.md Web | no timer waits in tests | conforms | `findBy*`, `waitFor`, `expect` only |
| CLAUDE.md Code | `Generators` for input | deviates | `WorkRowTest.java:76` builds the row from a literal `since` string and asserts it back (input, not an expectation); `ControlResourcesTest` passes the new null positionally |
| CLAUDE.md Code | `@EnableGeneratorController`; no new dependency; no shim | conforms | `WorkRowTest.java:33`; no pom or package.json in the diff |
| spec Standing | text-guard baseline only shrinks | conforms | drops the EpicView `de` "paused" entry, adds none |
| spec Standing | design system v2, no plain text button | deviates | L3-10 |
| spec Write-Boundary | no file under `.plan/orchestrator/` except the inbox message | **deviates** | the six files above (re-read) |
| spec Expected Surface | only the surface it lists | deviates | `WorkRow.java`, the mock scenarios, `client.ts`, `runtime.ts`, `theme.test.ts`, five e2e specs, `text-guard-baseline.json`, the six ledger files; most are recorded or follow from the tab move, the ledger files and the data-table repair are not |

## Metrics and Anomalies

- Tokens: `unknown` (unobserved, not zero). `plugin_version` `unknown`; executor `0.1.2135`.
- The lane's review: two self-review rounds (19 and 12 findings, all fixed); CodeRabbit 7 inline findings on `b567064e`, all handled in `c0720779`; one SonarCloud issue (`typescript:S5906`) fixed in `c0720779`.
- One `curl` was issued directly for the mock's `/mock/goto` and `/mock/pairing` calls (the lane's own record).
- The lane's cleanup force-removed its worktree on the operator's selection (the worktree reported six modified ledger files through the `.plan` symlink); main was pulled to `8fe7f327`. `cleanup_owed=false`.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `b567064e`; later heads were not re-reviewed (the standing rule allows it). Re-read: `review-gate --pr-number 456` shows `unresolved_review_threads=0` and `review_body_findings_total=7`, the same as the landing; 7 `finding:` dispositions are in the progress file.
- Sonar: `sonar_open_new_issues=0`, gate `ok`; the Sonar MCP search for pull request 456 returns no issues; `read_open_issues` is `undecidable` on the merged PR, its `analysis_revision` equals the merge head `53bdb8e3`.
- Landing check: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`, both `unknown`; recorded as an Open Defect.
- **Effect of the six ledger files on `main`:** `main` at `8fe7f327` carries the ledger as the lane's checkout saw it — PLAN-52, 53, 58 and 60 as `staged` and an older resume anchor — while the shared ledger worktree holds the newer state. The next `land` replays the worktree onto `origin/main`, so nothing is lost; until it lands, `main` shows the rolled-back ledger.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-58 --status shipped`
- [x] row `pr` stamped — `456`
- [x] row `landing` stamped — `landings/PLAN-58.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-58-web-epic-page`
- [x] epic.md: Open Defects `PLAN-58 landed` and `PLAN-58 write-boundary breach`; the PLAN-146 L3-2 note re-pointed
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **PLAN-144 now has two dependants waiting on it**: the epic's list-column clause (D2) and the breadcrumb the epic page leaves to the top bar.
- **A PLAN-58 residue plan** (named, NOT staged): L3-1 (epic switched in place, with the `EpicIntentPanel` `epicId` read of the PLAN-146 landing), L3-2 (tab leak), the refresh race and retry state (L3-3 to L3-5), the tabpanel names, the stale "Next plan", the plain buttons, the vacuous since assertion, the modal's count source, the missing catalogue-test keys.
- **PLAN-156** now owns a modal that already carries a count and an overlay rule from this plan; its spec should be re-grounded at emit.
- **The daemon must serve `wait_since`** (plan-marshall-mcp / pm-mcp-core) — a repository outside this epic; named in the lane's report.
- **The write-boundary breach** is a lead for the plan-marshall repository: the lane's own report says the ledger files came in through the worktree's `.plan` symlink and were unstaged before the merge commit, yet the merged result carries them. How they reached the merge is not shown by this analysis; see the Open Defect.
