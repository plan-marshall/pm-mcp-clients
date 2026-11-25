# PLAN-188 landing record

epic: ui-improvements
plan: rb-188-profiles-usage-model (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` and `plugin_version` `unknown`; executor 0.1.2183)
pr: #566, squash `52aa9e4b` (18 files), merge queue (`merge_group` run 45597837489 success), merged 2026-11-25
analysed: 2026-11-25, by sub-agent in three layers, by reading only (no build); `OutlinePanel.svelte:12-16`, `ShippedProfileUsageModelTest.java:64-80` and the tree-wide grep for `OverSizeChart` / `intent.oversize` / `intent.outline.dependenc` (no hit outside `.plan`) re-read by the orchestrator at `52aa9e4b`

Message `rb-188-profiles-usage-model-001` (revision 0): `merge_state=merged`, `deliverables_done=1` of 1, 0 unresolved threads, 0 review-body findings, `sonar_open_new_issues=0`, quality gate ok, `self-review: clear` (three rounds; ten findings in round 1, four fixed in `ed5a457b`, two declined with reason). `landing-check`: `complete: false`, missing `total_tokens` and `plugin_version` (the 2026-10-27 Open Defect, opencode).

**Merge corroborated:** `ci pr view 566` reads `merged`, merge commit `52aa9e4b404a6ddea69bd14be199d750c25c7013`, first commit on `origin/main` after the ledger land #575.

**Review numbers re-read** (`runbook_lane review-gate --pr-number 566`, 2026-11-25T11:52Z, after the merge): 0 unresolved threads, 0 review-body findings, `merge_allowed: true` — equal to the message. CodeRabbit reviewed `ed5a457b` after three quota refusals and then the scope-extension head; Sourcery refused on budget (optional).

**Sonar.** PR: 0 new issues, gate passed (the lane's read at `ed5a457b`, the SonarCloud summary). Default branch, read 2026-11-25 ~11:53Z: quality gate OK, `read_open_issues` (project scope) `confirmed`, 9 open — the same 9 keys as `landings/PLAN-187.md` (5 `pm-operator`, `LessonSourceCommand` S1075, the 3 PLAN-181 keys); none in a file this PR touched. The read reports no analysed revision, so that the main analysis already covers `52aa9e4b` is **not shown**; the count is unchanged either way.

**SNAPSHOT deploy of `52aa9e4b`:** `unknown` (the `ci` surface has no commit-scoped run reader). pm-mcp-core PR #594 waits on it; its lane confirms it.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 profiles declare `headless.usage.model` | met: claude `"/message/model"`, antigravity/codex/neutral/opencode `null`; one member added per file, nothing else changed |
| Done-when 1 test-first | the test can fail (re-read `:70`, `:72`); the red run at `62155055` is the lane's (job logs cited), not shown by reading |
| Done-when 2 diff names only the profiles and the test | holds for D1; the PR as a whole has 12 more files from the operator-selected base repair (the lane asked, the operator chose "Fix the intent packs in this PR") |
| Done-when 3 `verify -pl pm-api -am` and full verify | the spec's premise of a green baseline was refuted by `20f971fe`; met only with the repair; the runs are the lane's |
| Done-when 4 SNAPSHOT deploy stated | met as `unknown` |

Unverifiable here: the pm-mcp-core validator rules and the `/message/model` value (HYPOTHESIS in the spec; pm-mcp-core #594 is the source). The sub-agent's doubt: claude's other `usage` pointers read the `result` line, while `/message/model` sits on an assistant line; whether the pointer base is the result line cannot be settled by reading. Reported, not acted on.

True count by reading: 1 of 1.

## Layer 2: implemented

D1 traces to five one-line profile edits and `ShippedProfileUsageModelTest` (6 tests): presence and value per profile, pointer shape (starts with `/`, ≤ 256), and `coversEveryShippedProfile` pinning the table to `HostDetection.clientIds()`. Every other file is the stated repair: the 11 orphan keys out of both intent packs, `IntentCatalogueTest`, `OverSizeChart` and its test deleted, the added-dependencies block out of `OutlinePanel` (and its test), the dev components page, `intent.spec.js`, the traceability link, Javadoc-only edits in `OverSizeDistributionResource.java` and `client.ts`. No unexplained file. The lane ran `-Pe2e` for the repair (458 passed, 9 failed in `designer` / `projects` / `theme-readability`, main's own; `intent.spec.js` 7/7) — the lane's run, not shown by reading.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. The repair leaves no dangling reference (re-read: zero hits outside `.plan`).

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | `OutlinePanel`'s component comment still describes "the added dependencies with the deliverable that adds each and the reason"; the block is gone (re-read) | `pm-web-app/src/lib/components/OutlinePanel.svelte:13-16` |
| L3-2 | minor | the test checks the pointer only for a leading `/` and the length; the validator's escape rule (`~0`/`~1` only) is not asserted, so `/a~2b` would pass here and fail pm-mcp-core (D1's Done-when does not ask for it) | `ShippedProfileUsageModelTest.java:73-78` |
| L3-3 | info | the over_size stack stays without a design after #570: `getOverSize` (`client.ts:4310`), `OverSizeDistributionResource`, the project page rows (`ProjectExplorerView.svelte:921-929,1614-1635`, key `project.page.oversize`) and their tests; `added_dependencies` is parsed but rendered nowhere. The lane's own residue; it is the doc-review message's item | as named |
| L3-4 | info | `throws Exception` on the parameterized test method (test code; the rule is for production code) | `ShippedProfileUsageModelTest.java:67` |

Conformance: jackson-core only, no dependency, JUnit 5 only, literal contract values, record, Javadoc, no `{@html}`, no timer wait, traceability link valid: conforms. Compile warnings, lint/format/svelte-check, commit trailers: not shown by reading.

## Residue for the plans that touch the same surface

- **main's red `IntentCatalogueTest` is repaired** by this merge (the packs carry exactly the document's 54 `intent.` keys); the Open Defect of 2026-11-25 is resolved as far as that test goes.
- **The over_size stack and `added_dependencies`** (L3-3) belong to the doc-review message `doc-review-2026-11-24-001` (code side of #570), drained next.
- **PLAN-179** (staged) still instructs mounting `OverSizeChart`, which no longer exists and has no design: re-ground before it is emitted.
- **main's Playwright reds** (`designer`, `projects`, `theme-readability`, 9 tests) recur, already recorded at PLAN-185/187; PLAN-182 touches `theme-readability.spec.js`.
- **Mailbox address for a digit-led plan id:** `resolve`'s `inbox_read_command` passes `--plan-id 188-profiles-usage-model`, which `inbox read` refuses (`invalid_target_plan`); the lane read at `rb-188-…` instead. A plan-marshall tool defect, not this repository's.
