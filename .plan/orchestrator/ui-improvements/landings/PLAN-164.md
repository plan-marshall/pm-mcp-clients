# Landing Analysis: PLAN-164 — Web: A Plan's Changes as Its Own Tab

epic: ui-improvements
workstream: WS-03
pr: #477 (https://github.com/plan-marshall/pm-mcp-clients/pull/477); follow-up #481 open, not merged (https://github.com/plan-marshall/pm-mcp-clients/pull/481)

> Landing record, written by `analyze` on 2026-11-18 from the inbox message `rb-164-web-plan-changes-tab-001.md` (a lead, not a fact), git, the provider, and a read-only three-layer leaf. The orchestrator re-read the points marked "re-read" on `origin/main`. Main has not moved in `pm-web-app`, `pm-mock-runtime`, `pm-api`, `doc/design` or `pm-web-e2e` since the merge, so HEAD equals the merged state for every file cited.

Merged as `744755b5` (#477). Sender `rb-164-web-plan-changes-tab`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated). The lane reports `deliverables_done=2` of 3 (D0 the mock's first capture, D1 the client and the Changes view, D2 `model_differs`; D0 "not fully met" on the `unavailable` coherence) and `self-review: budget-exhausted` (the third round reports six findings open). **The lane is not finished:** the operator chose "fix F1/F2 in a follow-up PR now", and PR #481 (`fix/plan-164-review-residue`, `BEHIND` main) is open; its landing message will follow. The row therefore stays `running`.

## Deliverable Fidelity vs Spec

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D0 — the mock's first capture | delivered, F2 mock-only | `applyCarriedCapture` (`MockRuntimeEngine.java:8110-8168`) moves `not_captured` to captured and a captured plan to `unavailable`; `/process` and `/changes` are one measurement (`applyCaptureSize:8185-8192`); a contradiction is refused with positive controls; a scheduled capture is validated by the loader (`ScenarioLoader.java:1062-1100`); the README states the limit. Tests: `movesAPlanBeforeItsFirstCapture`, `movesACapturedPlanToUnavailable`, `servesANewListAndSize`, `keepsTheLastCaptureSize`, the four `refuses…`, `validatesAScheduledCapture` |
| D1 — the client and the Changes view | shipped, with partly pinned clauses | list, file, cut file, binary row, a markup line as text (`PlanChanges.svelte:325-358,374-377,396-400`; `PlanChanges.test.ts:92,119,145,175,196`); `?tab=changes&file=` reload (`PlanView.svelte:79-100,221-229`, `runtime.ts` `selectPlanChangeFile`; the unit case sets the store directly, the real reload is the e2e `plan-changes.spec.js`); `not_captured` hides the tab (`PlanView.svelte:79,329-337`), `unavailable` shows its reason (`PlanChanges.svelte:312-318`). **Emit Amendment:** the race is stated and pinned (below), the full path is **partly** delivered, the PLAN-51 overlap is clean (`client.ts` +103/-0, `client.test.ts` +27/-0; `request_id` still at `client.ts:1206`) |
| D2 — `model_differs` | shipped, pinned on inline JSON | both models and the head keeping the launch model (`PlanView.svelte:1303-1313`); the values match `running-work.json:1056-1058` but are read from inline JSON, not the fixture (as in PLAN-163 L1-4); the `^claude sonnet$` head assertion would also pass before D2 |

**Files outside the Expected Surface:** `runtime.ts`, `display/index.ts`, `utils/outputSafety.ts` (the lane names them). **Keys:** the six `plan.changes.*` keys are reused; three new keys (`plan.tab.process`, `plan.tab.changes`, `plan.tabs`, en and de) are added and documented (the spec's "adds none" is about the `plan.changes.*` family). `text-guard-baseline.json` is untouched; no `{@html}` or `innerHTML` in the new files.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf — reason: the Dispatch Decision Rule; the stack spans the mock, the loader, the web store and a new component. Build logs, CI and screenshots were not opened ("not shown by reading"). The orchestrator re-read the points below.
- **Verdict:** layer 1 minor (the `head` term and the full path); layer 2 the lane's F1 to F6 all confirmed open at HEAD, F1 narrower and F2 mock-only, and a stack-wide contract gap; layer 3: **no blocker, no major**, 12 minor, 2 leads.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | the file/list race is stated and pinned (Emit Amendment) | checked | partly | stated in code comments (`PlanChanges.svelte:112-136,259-262`) and the PR body, not in the design; the identity is `planId, path, head, status, added, removed, binary, renamed_from` (re-read `:121-136`); both race tests change the head AND `added` (48 to 60), so deleting `read.head` from the identity keeps every suite green: the case "head moved, counts equal, lines differ" is unpinned |
| L1-2 | a full path is shown and copyable (Emit Amendment) | checked | partly | the row shows the full text with a CSS ellipsis and a `title` (`:337-339`); the panel shows it (`:379`); a binary row's panel shows no path (`:374-377`); a row path cannot be selected inside a `<button>`; the tests assert substrings only (`'Transport.java'`), so an abbreviating revert stays green |
| L1-3 | VD-S-06 v2 components | checked | partly | the size line is a plain `<button class="change-size-link">` (`PlanView.svelte:1029`, styled `:1487-1491`), the rows plain buttons (`PlanChanges.svelte:328`), `unplanned` and `binary` bare spans; the spec named `Chip`, `Link`, `StatusLabel`; `Button variant="ghost"` exists |
| L1-4 | the design document is updated in the same PR | checked | partly | `components-tags`, `plan-view-interaction` and `theme-readability` changed; `plan-view.adoc` was not (stale lines below) |
| L1-5 | `plan.changes.*` reused | checked | confirms | `plan_view.en/de.properties:56-58`; three tab keys added, documented |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| F1 (the lane's medium): reselecting a row whose read failed | **confirmed, narrower** | re-read `PlanChanges.svelte:246`: `if (failedIdentity === identity \|\| inFlightIdentity === identity \|\| disposed) return;` fires before `fileError` and `currentFile` are touched. Sequence: X fails; click Y (`:254` clears the error, Y in flight); click X before Y answers: `:246` returns; Y resolves and is dropped at `:262`; the panel renders nothing (no error, no retry). The lane's "previous file's hunks" variant is not reproducible by reading (success clears `failedIdentity`, a new row nulls `currentFile`, `:251`). Recoverable by selecting another row: the leaf rates it minor |
| F2 (the lane's medium): a carried `unavailable` announces 0/0/0/0 | **mock-only** | re-read: the engine specification says `unavailable` is only the answer to a read (`01-processes-and-transport.adoc:336`) and `plan.change_captured` is "each capture" with the new `head` and size (`:337`; `06-event-catalogue.adoc:70`); `PlanChangeCapturedEvent` has no state; the mock's `capture` member is a mock extension the README states. The real head figures of a plan whose capture is gone stay the last measured ones (`store-schemas/13-leases-integrity-testing.adoc:69`: the size is shown only while `realized_state.state == measured`). The latent web weakness: `changeSize = changeFromEvent ?? plan?.change` (`PlanView.svelte:79`, re-read) keeps the event's figures until a plan switch and lets them win over a fresher `/process` |
| F3: no test emits `plan.change_captured` into `PlanView` | confirmed | `subscribeEvents` is mocked to `() => {}` in `PlanView.test.ts`; the e2e has no live step; reverting the subscription (`PlanView.svelte:203,229-232`), the `plan_id` filter (`:307`) or the token bump keeps everything green |
| F4: a superseded read's `finally` clears `isFileLoading` | confirmed | re-read `PlanChanges.svelte:277-282`: unconditional `if (!disposed) isFileLoading = false`; in the very race the tab pins, the panel is blank until the new read lands |
| F5: `outputSafety.ts` overclaims "every control or format character" | confirmed | the regex strips C0/DEL, C1, U+061C, U+200E/F, U+202A-E, U+2066-9, U+2028/9, U+FEFF; it keeps U+200C/D by design and does not strip U+00AD, U+200B, U+2060-64 or the tag block; the engine already strips hidden characters from every served string (`01-processes-and-transport.adoc:146`), so the client strip is defence in depth |
| F6: `tokens.test.ts` guards one token | confirmed | only `--color-text-primary` on the two diff grounds; muted line numbers and the success/attention/secondary tokens on the selected row's ground are not guarded (the leaf's arithmetic: the muted numbers pass at about 4.9:1 dark) |
| "encoded exactly as `PlanChangesRoutes` does" | overstated, harmless | `client.ts` uses `encodeURIComponent`, where the server's `URLEncoder` plus `%20` also encodes `! ' ( ) ~`; the server decodes both alike; the test covers `/`, `.`, space and `ü` |

**Contract per fact (pm-api record, then the engine's specification).** The list (`state`, `head`, `base_ref`, `files[]`), a file row (`path`, `renamed_from`, `status`, `added`, `removed`, `binary`, `unplanned`), the file answer (`path`, `hunks`, `binary`, `truncated`, `lines_omitted`; the record's Javadoc says it carries no head), the hunks and line kinds, `unavailable.reason` (free text, shown through the strip only), the event payload (`plan_id`, `head`, `files_total`, `insertions_total`, `deletions_total`, `unplanned_total`: the catalogue says only "the count of unplanned changes", so the name `unplanned_total` is published in `pm-api` alone), the process `change`, and `launch_model`/`served_model` are all stated in `pm-api` and `runtime-model/01-processes-and-transport.adoc:331-337`. The file-route refusals `scope_not_found` and `target_not_found` are in `errors.ts` and the error packs, not in the runtime-model specification (they are PLAN-163's). **One stack-wide gap:** the specification says a list over its bound (1000 files, the timeouts specification's `plan-changes-bounds`) answers the files up to it with `truncated` and the count left out (the leaf's reading of `:331`; re-read on the records side: `truncated` exists in `PlanChangeFile` only, not in `PlanChangesCaptured`/`PlanChangesAnswer`, the client's type or the design). A real runtime would be shown as a complete list of at most 1000 files. Against a runtime that sends the published shape only the tab works; a `truncated` file without `lines_omitted` reads "0 more lines" (`PlanChanges.svelte:398`), an unknown `status` falls to `M`, an unknown line `kind` becomes the class `diff-line--<kind>`.

Not shown by reading: the failing-run logs, CI, screenshots, whether the SSE data of the mock carries `capture` (lead).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `PlanChanges.svelte:121-136` (re-read), the race tests `:282,:408` | someone removes `read.head`; a capture changes a line but not the counts | the file is no longer re-read and the old hunks stay under the new head; the existing tests change the counts too | minor |
| L3-2 | `PlanView.svelte:203,229-232,307,326` | revert the subscription, the filter or the token bump | no test fails (F3) | minor (test) |
| L3-3 | `runtime.ts:1912-1918` and `navigation/history.ts:291-296` (re-read: `switchTab` deletes `file` and `node`) | open file X, go to Process, go back to Changes | the store keeps `changeFile`, so the view reopens X while the address is `?tab=changes`; a reload loses X; the design ("Switching the tab…") wants the view to keep the selection: the bar and the store disagree | minor |
| L3-4 | `PlanChanges.svelte:528`, `tokens.css:94-96`, `tokens.test.ts:216-247` | the `unplanned` word in the light theme | `--color-attention` (#b86e00), a state-dot token held to 3:1, instead of `--color-attention-text` (#8a5200): about 4.0:1 on white and 3.4:1 on the selected row (the leaf's arithmetic); dark is fine | minor |
| L3-5 | `PlanView.svelte:1487-1491`, `PlanChanges.svelte:467`, `components.adoc:30` | the size line and the rows | `padding:0` text-xs and `min-block-size:32px` against the design's 24 px (44 px under 768 px) | minor |
| L3-6 | `PlanChanges.svelte:374-377` | a binary row at 390 px | its path is ellipsised in the row and absent in the panel: it cannot be read in full or copied; no test pins the full path | minor |
| L3-7 | `PlanChanges.svelte:246,279-281` (F1, F4) | the sequences above | a blank panel; a blank panel until the new read lands | minor |
| L3-8 | `PlanView.svelte:79` | an event's figures, then a fresher `/process` | the event's figures win until a plan switch | minor (latent) |
| L3-9 | `PlanChanges.svelte:587-590` (lead) | below 769 px | each diff line scrolls on its own, the hunks sit under the whole list, no `scrollIntoView` or focus move, the list is an `<ol>` of buttons with no arrow-key navigation | minor (lead) |
| L3-10 | `routes.test.ts`; the D2 test | — | no `file=` round trip test with `#`, `?`, space, `%`, unicode (reading shows `encodeValue` and `URLSearchParams` round-trip faithfully); D2 reads inline JSON | minor (test) |
| L3-11 | `PlanChanges.svelte:326` `{#each answer.files as file (file.path)}` and the `:`-joined identity (lead) | the engine's hidden-character stripping makes two distinct paths identical; an id or path containing `:` | duplicate keys throw; an identity collision | minor (lead) |
| L3-12 | the list-level `truncated` (layer 2) | a plan over the 1000-file bound | silently a complete list; the stack-wide gap | minor (contract) |

The race itself is sound by reading: the identity includes the plan, path, head and counts; the check runs after the await (`:262,:269`); generation guards cover the list read (`:186,:198`); `disposed` is set in `onDestroy` (`:286-289`); a file answer for another plan is dropped (test `:445`); a capture that turns the list `unavailable` or `not_captured` clears the address through `onSelectFile(null)` (`:202-205`). The residual window: a file served at capture C2 shows under C1's title for one round trip until the event-driven list read changes the head and triggers a re-read: bounded, and it depends on the event arriving.

**Layer 3 verdict:** findings — no blocker, no major; 12 minor, 2 leads. The follow-up #481 for F1 and F2 is justified: F2 is a mock-only artefact (the engine announces no `unavailable`), F1 a narrow race with a recoverable result.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}` with untrusted text; no timer waits | conforms | the leaf read the whole component |
| CLAUDE.md Dependencies | none added | conforms | no `pom.xml` or `package.json` in the file list |
| spec Standing | VD-S-06 v2 components | partly | L1-3 |
| spec Standing | the design document is updated in the same PR | partly | L1-4 |
| spec Standing | text guard baseline only shrinks | conforms | untouched |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | the lane states none; #477's file list holds none (not re-read) |

**Stale design lines:** `doc/design/shared/plan-view.adoc:191` ("an open file is read again only when its row changed": the code re-reads on every new head, and the rule is itself unsound (same counts, different lines: PLAN-163 L3-2); the same sentence is in `PlanChangeFile.java:30-32`; the race mechanism is stated nowhere in the design); `plan-view.adoc:176-183` (the picture with abbreviated `…/` paths against the amendment's full path); `plan-view.adoc:187` ("a binary file shows only its row" against the panel's binary line); `plan-view-interaction.adoc` "Addresses" (the tab-switch bullet: L3-3); the list-level `truncated` is stated by neither the design nor `pm-api`.

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `unknown`; executor `0.1.2149`. `landing-check`: `complete: false`, `missing_keys` `total_tokens`, `plugin_version`, `sonar_open_new_issues`.
- Self-review: three rounds (round 1: H1 vacuous race pin, M2 stale size, M3 scheduled semantics, M4 diff tokens, M5 stripping, M6 unretryable file error, L1 to L13, all fixed; round 2: the M2 follow-on; round 3 on the merged diff: F1 to F6 open). CodeRabbit: 13 findings at `62b81ccc` (9) and `d06d818d` (4), all fixed; re-read: `review-gate --pr-number 477 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=13` (its `handled:0` is the gate lacking `--disposition`). Sonar: three rounds (6, 1, 0); the Sonar MCP search for pull request 477 returns no issues.
- Three operator questions: the executor skew (regenerate and continue), the local e2e with the plan's ports (in-process), fix F1/F2 in a follow-up PR now.
- Builds: every Maven build `daemon_longpoll` except the e2e, run `in_process` on the operator's decision (the daemon's child environment cannot carry `PM_MOCK_WEB_PORT` and `PM_WEB_PORT`: the cause the PLAN-49, 51, 66 lanes also reported; for the plan-marshall repository); one e2e run hit the 827 s bound and was re-submitted at 1800 s; no `killed` job.
- The lane's rule breaks, by its own count: shell reads of build logs (`grep -c`, `wc`, `tail`, `find`, `ls`), one `grep | head`; **one edit to the main checkout by mistake** (`PlanChangesScenarioTest.java`), reverted with the file tools (`git status --porcelain` in the main checkout was empty afterwards; re-read by the orchestrator: still empty); no guard enforced.
- The mailbox read with a digit-leading plan id is refused: not reported by this lane.

## Routing and Merge Behavior

- Review: CodeRabbit (required, `quota_limited`, `review_complete_after_handling`): triggered 00:54, refused on quota at 01:04, accepted after the 90-minute window (review 03:52), an automatic review at 05:10; 13 findings, all handled. Sourcery refused on its hard quota, optional.
- Sonar: `sonar_open_new_issues=0`; the MCP search returns no issues.
- Merged through the merge queue; the merge-group run green.

## Reconciliation Actions

- [x] row `status` stays `running` (the lane's follow-up #481 is open); `pr` stamped `477`, `landing` stamped `landings/PLAN-164.md`, `plan_marshall_plan_id` stamped `rb-164-web-plan-changes-tab`; the next landing message restamps
- [x] epic.md: Open Defect `PLAN-164 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-164 residue** (named, NOT staged): under the operator's rule of 2026-11-18 the residue folds as a D0 of the next plan touching `PlanView` or the Changes tab (PLAN-55 carries the tab into its set; PLAN-135 and PLAN-137 edit the plan view): the `head` term and the live event test (L3-1, L3-2), the tab-switch divergence (L3-3), the attention contrast (L3-4), the target sizes (L3-5), the binary path (L3-6), the stale design lines, the list-level `truncated`. F1 and F2 are the lane's follow-up #481.
- **A contract gap for the engine side (named, nothing staged):** the list-level `truncated` of the 1000-file bound is stated by the engine's specification and carried by no `pm-api` record, the mock, the client or the design. The unspecified spelling of `unplanned_total` in the event catalogue is the same family.
- **For the plan-marshall repository (named):** the follow-up below adds one (the merge with unresolved threads).

---

# Second landing: the follow-up #481 (the review residue)

> Written by `analyze` on 2026-11-18 from the inbox message `rb-164-web-plan-changes-tab-002.md` (kind `finding`, an amendment to the first landing; a lead, not a fact), git, the provider, `gh api graphql` (the review threads) and a read-only leaf. The orchestrator re-read the points marked "re-read" on `origin/main`.

Merged as `f688d7be` (#481, 2026-11-18T11:28Z, 9 files, +429/-71, head `b6a6c3d`). The lane reports F1 to F6 closed, "CodeRabbit's eleven minor findings on that head answered in the same PR", **D0 fully met and the deliverable count 3 of 3**, `self-review: clear`, verification green and Sonar 0 (Sonar MCP search for #481 returns no issues). **The row is `shipped`.**

## F1 to F6 at HEAD

| Finding | Closed? | Evidence |
|---------|---------|----------|
| F1 reselecting a row whose read failed | **closed** | the failure is cached per row identity (`PlanChanges.svelte:49,224-231`); a failed row clears `currentFile` and the spinner and shows its own error and retry (re-read `:266-271`); test "shows a failed row's own error and retry, never another row's hunks" (`:483`) fails on the old code. The orchestrator's narrow variant (X fails, click Y, click X before Y answers) is fixed by reading and untested |
| F2 the carried `unavailable` announcement | **closed for the mock only** | the mock announces the plan's last successful figures (`MockRuntimeEngine.java` about `:8216-8231,8261`, `ScenarioLoader.java` about `:385-399,479-488`; `PlanChangesScenarioTest`: `movesACapturedPlanToUnavailable`, `refusesAContradictingUnavailableSize`, `refusesAGoneCaptureWithoutFigures`, `validatesAGoneFixture`). The real contract still has no such announcement (`01-processes-and-transport.adoc:336-337`); the web weakness `changeSize = changeFromEvent ?? plan?.change` (`PlanView.svelte:79`) is unchanged (L3-8) |
| F3 no test emits `plan.change_captured` | **closed** | `PlanView.test.ts` "takes the size a capture announces and reads the list again" and "ignores a capture another plan announces"; removing the subscription, the filter or the token bump turns a test red |
| F4 a superseded read clears the open read's spinner | **code closed, NOT pinned** | the request token (`PlanChanges.svelte:287,294,302,309-312`, re-read: `finally` clears only `if (request === fileRequest)`); no test asserts the spinner (re-read: no `loading`, `spinner` or `busy` in `PlanChanges.test.ts`), so reverting the guard keeps every suite green |
| F5 the `outputSafety.ts` header overclaims | **partly** | the header is a comment-only change and still opens "stripping ANSI escape sequences" and "the C0 and C1 controls" (re-read `:1-11`) without saying the regex keeps TAB, LF and CR or that an OSC payload up to BEL is not removed as a unit |
| F6 `tokens.test.ts` guards one token | **half closed** | secondary and muted on both diff grounds, success on `--color-diff-added`, danger on `--color-diff-removed` are added (re-read `:243-251`); the selected row's ground `--color-brand-subtle` is still guarded for primary only, and `--color-attention` as text is unguarded (L3-4) |

Of the previous L3-1 to L3-12: L3-2 (the live event test) is closed; L3-7 (F1, F4) is closed in code; L3-11's identity half changed (the identity is now a JSON tuple with `base_ref`, `:128-139`, and no test varies the new term); L3-1 (the `head` term), L3-3 to L3-6, L3-8 to L3-10, L3-12 and the stale design lines are unchanged.

## The nine unresolved CodeRabbit threads (re-read through `gh api graphql`)

`review-gate --pr-number 481` still answers `unresolved_review_threads=9`, `review_body_findings_total=19` (its `handled:0` is the gate lacking `--disposition`); all nine have exactly one comment (CodeRabbit's) and **no reply**; CodeRabbit's last review (10:49:58 on `b6a6c3d`) came 38 minutes before the merge. The lane's "eleven minor findings answered in the same PR" is the review of `d77e819` only: of its 11 threads 7 were fixed and resolved, 2 were fixed in code and never resolved (the outdated `MockRuntimeEngine` and `ScenarioLoader` ones) and 2 were not fixed (`outputSafety.ts:2` and `:3`); the 5 threads of the final review are new.

| Thread | Still holds? | Severity |
|--------|--------------|----------|
| `MockRuntimeEngine` (outdated): name all four totals | fixed (`sizeText` prints all four; the head-only refusals still print none: a nit) | none |
| `ScenarioLoader` (outdated): an unavailable fixture declaring totals | fixed for scenarios with `plan_changes` (`retainedSizes`) | none |
| `outputSafety.ts:2` (the ANSI claim) | holds, doc only | minor |
| `outputSafety.ts:3` (the preserved C0 controls) | holds, doc only (the test "preserves clean text, tabs, and newlines" pins the behaviour) | minor |
| `MockRuntimeEngine:8221` (save the validated figures of an unavailable announcement) | holds, narrow: needs a fixture whose `/process` and project row disagree; the bundled fixtures have none | minor, mock-only |
| `MockRuntimeEngine:8237` (project-row figures before scheduled playback) | holds: `declaredChangeOf` reads only `/process`, the loader takes sizes from every route; the scheduled event is dropped at playback with no prior read; not reachable in the bundled scenarios (`docs-typo` declares no size) | minor, mock-only |
| `ScenarioLoader:287` (one rule for scheduled and manual announcements without `plan_changes`) | holds: a loud load failure of an unusual scenario, never silent | minor, mock-only |
| `ScenarioLoader:396` (route order selects the last-capture figures) | holds: `getFirst()`; needs contradictory declarations | minor, mock-only |
| `PlanChanges.svelte:273` (a pending read reused after its row leaves the list) | holds, narrow: the null-path branch neither clears `inFlightIdentity` nor bumps `fileRequest` (re-read `:255-262`); a slow read spanning a gone and restored capture at the same head can cache a 404 on a row that could load; the retry button recovers | minor, race only |

## Contract

#481 adds no wire fact to `pm-api` or the client (the client change is internal: the request token, the per-row errors, the identity). It extends the mock's own `capture` member of `/mock/broadcast` with a rule the engine's specification does not state (an `unavailable` announcement carrying the last figures). The mock README line (`:140`) now says a captured plan "may turn `unavailable` (naming the figures of its last successful capture…)" inside the broadcast bullet, so it is mock-scoped by location only; it does not say that the real runtime announces no `unavailable` and that `capture` is not part of the contract. The earlier landing's "the README states the limit" is therefore overstated. The old gaps are unchanged: the unspecified `unplanned_total` spelling and the list-level `truncated`.

## Layer 3 (second landing)

No blocker and no major. Minor: the F4 token and CodeRabbit's A, B, A race have no test; the identity terms `head` and `base_ref` are unpinned (reverting to `.join(':')` stays green); the F1 narrow case is untested; F6's selected-row ground is unguarded; `fileErrors` is never pruned (intended per-row account, small memory growth); the engine's `declaredChangeOf` fallback and the `applyServedChange` seed have no runtime test (only the loader side is covered by `validatesAGoneFixture`); the five final-head mock and race threads; the `outputSafety` header. Lead, unverified: `declaredChangeOf` calls `activeRoute` under `planChangesLock` (the lock order was not traced). The request token itself is sound by reading (reselect while pending, a superseded read, A, B, A all traced).

**Verdict:** D0 "fully met, 3 of 3" is acceptable for the mock's first capture, with F2 mock-only; the follow-up is complete for F1, F2 and F3; "F1 to F6 closed" is overstated in three places (F4 untested, F5's header, F6 half); "CodeRabbit's findings answered" does not match the provider (9 unresolved threads without a reply). **The merge left five real but minor defects** (the final-head threads) and a doc overclaim; no blocker or major.

## Metrics and Anomalies (second landing)

- The message is `kind=finding` (an amendment), not a landing revision: `landing-check` reports `complete: false` with 17 missing keys because it carries no landing facts block; the facts of the first landing stand.
- **A lane-process finding for the plan-marshall repository:** the lane merged #481 with nine unresolved, unanswered review threads (five of them on the head the last review covered) and reported them as answered. The merge rule's gate (`review-gate`: no unresolved thread, every body item dispositioned) would answer `false`; the lane's standing authorization ("`review_complete_after_handling`: one handled review is the review") does not cover unhandled findings of a later review of the same head. The recurrence class is the false-count defect already named (items 4, 17, 20, 25), now with "answered" claimed over unreplied threads.
- Verification at the merged head `b6a6c3d`: `verify -Ppre-commit` and `-Pcoverage` green, Playwright 454 passed and 75 skipped, CI green, Sonar's gate OK with 0 issues, 0 hotspots and 81.4% new-code coverage: the lane's claims, not re-measured. The worktree and branch were removed after the merge.

## Reconciliation Actions (second landing)

- [x] row `status` → `shipped`; `pr` restamped `481` (the first landing's `477` is in this file); `landing` stays `landings/PLAN-164.md`
- [x] epic.md: the PLAN-164 Open Defect extended; the inbox message archived on consume; resume anchor and `queue-view.md`

## Follow-Ups (second landing)

- **The PLAN-164 residue folds as a D0** of the next plan touching `PlanView` or the Changes tab (the operator's rule of 2026-11-18), now with: the F4 and A, B, A tests, the identity-term tests, the `outputSafety` header and the README mock-only wording, F6's selected-row ground, the five final-head mock threads (all mock-only), `PlanChanges.svelte:273`, the `head` term, the tab-switch divergence, the attention contrast, the target sizes, the binary row's path, `changeFromEvent` (L3-8), the stale design lines, the list-level `truncated`.
- **The nine CodeRabbit threads are not replied to and not resolved on the provider.** Nothing in this verb answers a thread; whoever picks up the residue answers them with the fixing commit.
- **For the plan-marshall repository (named):** a lane must not merge while `review-gate` reports an unresolved thread or an undisposed body item, and must not report threads as answered without a reply on each.
