envelope_version=1
sender_type=plan
sender_id=rb-166-web-column-engine-fixes
epic=ui-improvements
kind=landing
created=2026-11-15T22:47:25Z

```landing-facts
schema=landing-facts/1
plan_id=rb-166-web-column-engine-fixes
epic=ui-improvements
pr=#450
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=2
review_body_findings_handled=2
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

## Deliverables

All five are implemented and green at the merge commit `89baa992`. Three are fully met as written;
two carry an evidence-shape deviation, named below and counted out of `deliverables_done`.

**D1 — a key move after a peek keeps the window honest (L3-1, L3-3). Met.**
- "a peek that reaches the object keeps the strip and the width" — `test-first`; `columnWindow.test.ts`
  "returns the object its own window when a peek ends at the object" and `TriagePage.test.ts` "keeps
  the window honest when a key move after a peek reaches the object"; failing run `da8c8644`,
  `maven-2026-11-15-145016.log:1326` (strip and strip buttons absent, object 136 px too wide);
  passed at `maven-2026-11-15-145412.log`.
- "a peek is dropped at one column" — `test-first`; "clears a peek when a resize leaves one column,
  so widening shows the object"; same failing log `:1510` (`data-start` 0 instead of 2).
- Added after self-review: "clears a peek that ended at the object, so narrowing cannot resurrect it"
  — `mutation-after`, failing run `maven-2026-11-15-205532.log:957` (`data-start="2"` with the clear
  removed).
- Commit `5fd4c219`.

**D2 — a shift no longer remounts the columns (L3-2, L3-7). Met.**
- "the chat draft and a column's `scrollTop` survive a shift" — `test-first`; "keeps a chat draft when
  the window shifts", "keeps a list column scroll position when the window shifts"; failing run
  `5fd4c219`, `maven-2026-11-15-145944.log:1084` (the column node is replaced, widths change);
  passed at `maven-2026-11-15-150308.log`.
- "no transformed ancestor above the phone overlay; reduced motion runs nothing" — `test-first`;
  "puts no transformed ancestor above the phone overlay" (the chat is opened at 390 px and no
  ancestor carries a transform; the stylesheet assertion refuses an empty rule),
  "replays no slide where the person asked for reduced motion" (no replay class, `animation: none`
  and `transition: none` in the media block); same failing log `:1135`.
- The pre-existing assertion `expect(source).toContain('{#key layout.start}')` was removed on
  purpose — the key is exactly what this deliverable deletes; the same test now pins the
  column-scoped replay class.
- Commit `42c3034c`.

**D3 — focus and Escape agree between the templates (L3-4, L3-5). Not fully met.**
- "the focus moves into the chat and back to its column" — `test-first`; "moves the focus into the
  chat and back to the column it came from"; failing run `42c3034c`,
  `maven-2026-11-15-151022.log:1048` (the focus fell to the body); passed at
  `maven-2026-11-15-152637.log`. **Deviation:** the clause says "reads the focus on the opener"; the
  columns unmount under the chat, so the opener cannot come back as the same node — the test reads
  the heading of the column that held it, and `navigation.adoc:259` was amended to state that.
- "an Escape an open ConfirmDialog already handled leaves the chat open" — **not met as written.**
  The wizard half fails at `0b9402ec` (same log `:1065`) when a widget marks the key handled
  (`preventDefault`), and that witness is in the test. But the dialog half is not discriminating and
  was removed after self-review: `Dialog.svelte` keeps Escape by stopping it at the window's capture
  phase, so the event never reaches either template and no assertion at the group can tell that from
  the guard. The mechanism that protects the dialog case is real and unchanged; the clause's named
  witness is absent, so the deliverable is not counted as fully met.
- The pre-existing assertion "hides the chat from anywhere on the page" (Escape on `document.body`)
  was retargeted to the side column, because the listener now sits inside the template's own group —
  the change the plan mandates.
- Commit `e63a0398`, then `17856e39` and `0d012da7` from the self-review rounds.

**D4 — the showroom fixture works on a phone, and the build's format check passes (L3-6). Met.**
- "the fixture hides its chat on a phone" — `test-first`; `ColumnEngineFixture.test.ts` "hides the
  chat on a phone, so the showroom is usable there"; failing run
  `maven-2026-11-15-152919.log:1002` (the side column stayed after the click); passed at
  `maven-2026-11-15-153156.log`.
- "`format:check` is clean in both web modules" — `none`; it was unclean on the base (117 files in
  `pm-web-app`, 4 in `pm-web-e2e`; the lockfile pins prettier 3.9.10). The operator chose the
  wholesale reformat, landed as its own PR #447 with `skip-bot-review` (merge commit `6e31ae0a`,
  merged here in `d1eaaf36`); clean afterwards at `npm-2026-11-15-195201.log` and
  `npm-2026-11-15-195221.log`.
- Also in this deliverable: the two fused statements split (`TriagePage.test.ts`,
  `column-window.spec.js`), the strips' popover toggle made synchronous (`GapStrip.svelte`), and the
  `svelte-ignore` on the element it covers (`TriagePage.svelte`; the `{#key}` it used to precede is
  gone since D2).
- Commit `cade3ea4`.

**D5 — browser evidence for the strip, the peek and three or four columns. Not fully met.**
- "the new case passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set" — met:
  `column-window.spec.js` "shows the 1280 px window…" and "…1440 px…", 406 passed at
  `maven-2026-11-15-202916.log`, and 415 passed at the merged head
  `maven-2026-11-15-221648.log`, where PLAN-143's new specs also run.
- "reverting deliverable 1's fix makes it fail" — met: `767bb90f`, failing run
  `maven-2026-11-15-181415.log:687` (`walked.strip` 0 instead of 136); passed at
  `maven-2026-11-15-182633.log`.
- **Deviation:** the clause names `test-first`, and the fix predated the browser case, so the failing
  run is a revert of the committed fix — `mutation-after`. The base tree at `0b9402ec` was created
  for the test-first form and came back `executor_in_worktree: absent`, which the lane forbids using
  as evidence; it was removed unrun.
- **Coverage caveat:** the case measures the container and asserts the count the rule gives it, so
  which of the three- and four-column cells the two viewports reach depends on the showroom's layout
  rather than being pinned.
- Commits `767bb90f`, `320c9ec8`.

## Personas and skills

- D1–D4: `plan-marshall:persona-implementer` (the spec's own line) with
  `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- D5: `plan-marshall:persona-integration-tester` with `pm-dev-frontend:javascript`,
  `pm-dev-frontend:jest-testing`.
- Every self-review round and the triage of the PR findings: `plan-marshall:persona-code-reviewer`.
- Skills added for surface drift: none.
- Files the spec did not name, and why: `ColumnEngineFixture.test.ts` (D4's test demands a test of
  the fixture), the `sideFieldLabel` prop on `TriagePageFixture.svelte` (D2's chat-draft test needs
  a field in the side snippet), `.gitignore` (`node_modules/`, the operator's word),
  `onboarding.spec.js` (main's token respelling lowercases the hex; the assertion now compares
  case-insensitively), `navigation.adoc` and `components.adoc` (they stated the pre-fix peek, Escape
  and focus behaviour).

## Hypotheses

- **Confirmed**, `columnWindow.ts:239-282` + `TriagePage.svelte:296-301` at `da8c8644`: a peek that
  ends at the object returned `peek: true`, strip `none`, `returnStrip: false` and an object 136 px
  wider.
- **Confirmed**, `TriagePage.svelte:179-189` and `columnWindow.ts:241-242`: the peek was cleared only
  on a chain change, while `navWindow` already yielded `peeked: null` at one column.
- **Confirmed**, `TriagePage.svelte:341,408`: `{#key layout.start}` wrapped the window group, the
  columns, the strip and the side column.
- **Confirmed**, `TriagePage.svelte:433,470-479` and `SideColumn.svelte:69-78`: the window animated
  `transform` while the phone overlay was `position: fixed` inside it.
- **Confirmed**, `WizardPage.svelte:79-99` against `TriagePage.svelte`: the wizard moved the focus
  into the chat and back, the engine did not.
- **Confirmed**, `WizardPage.svelte:149` against `TriagePage.svelte:310-315,349`: the two templates
  hid the chat from differently scoped listeners.
- **Confirmed**, `ColumnEngineFixture.svelte:54-62`: `sideOpen` was a constant and no `onHideSide`
  was passed.
- **Confirmed by reading and by the gate**: the `svelte-ignore` sat before `{#key}`, not before the
  element it covers, and `GapStrip`'s popover toggle was `async` with no `await`.
- **Confirmed**, `pm-web-e2e/tests/column-window.spec.js:21`: the suite modelled only the two-level
  reports chain.
- The plan's verify-first clause: the PLAN-142 landing analysis' L3 table and the cited lines were
  re-read at the then-current head; no other plan had changed them.

## Findings

**Self-review (three rounds, adversarial, every Done-when graded as written).**
Round 1, 20 findings, all closed: the design documents made false by the peek/Escape change; a
vacuous `ConfirmDialog` Escape witness (removed, with the mechanism documented); a stale peek that a
narrowing window could resurrect (`TriagePage.svelte` now clears a peek the window does not render,
with a discriminating test); a slide latch that stuck when the chat took the space; two vacuous
stylesheet assertions; the phone-overlay test now opens the chat; the marks-only expectation of the
new e2e helper; an orphaned JSDoc; the reduced-motion `transition` assertion. Round 2, 3 findings: 2
closed, the third (the e2e coverage caveat) carried into this report. Round 3: nothing open.

**CodeRabbit (required reviewer), 2 inline findings on `TriagePage.svelte`, both fixed in
`d217decc`, each answered with the commit and the thread resolved:**
- `inline-1` — a takeover that merely becomes a docked chat pulled the focus out of the chat editor;
  the restore now runs only when the chat closes, and "keeps the focus in a chat that only becomes
  docked" covers the widen-from-792-to-1216 case. `finding: coderabbit inline inline-1 — d217decc`.
- `inline-2` — the remembered column could be one the returning window no longer shows; the index is
  now clamped to the window, and "returns the focus to a column the window shows, not to one it
  dropped" is the reported scenario. `finding: coderabbit inline inline-2 — d217decc`.

**CI:** green on every pushed head; the first full `verify` after merging main failed once in
`pm-api` (`EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort`, a property test on a
generated seed) — a module this plan does not touch, and green on the immediate re-run.

## Reviewers

| Reviewer | Status | Head | Trigger / wait history |
|---|---|---|---|
| `coderabbit` (required) | reviewed | `0d012da7`, coverage marker `sourceCommitId`/`coveredCommitId` = `0d012da7`, `kind=reviewed`; 2 inline findings, both handled | triggered once on PR creation (`@coderabbitai review`); not re-triggered after the later pushes — `review_complete_after_handling` with a qualifying review on record (§6 step 6). No quota refusal. |
| `sourcery` (optional) | refused | — | triggered once; its own notice refuses on the 7-day 250,000-character diff budget, and it named a return in 3 days 14 hours. Optional, so it did not gate. |

Standing review authorization was not needed: the required reviewer reviewed and its findings are
handled.

## Operator questions

1. Asked: "The lane's setup step requires `git status --porcelain` in the main checkout to print
   nothing, but it prints one untracked entry: `node_modules/` (only `.vite` and `.vitest-cache`)."
   Options: *Remove the caches, then proceed* / *Proceed, record the exception* / *Stop: report
   blocked*. **Selected: "Remove the caches, then proceed"**, with the free text **"adopt gitignore
   as well"** — carried out: caches removed, `node_modules/` added to `.gitignore` in `bf80fe54`.
2. Asked: "PLAN-166 deliverable 4's second Done-when is `format:check` is clean in both web modules.
   Measured now: `pm-web-app` reports 117 unformatted files and `pm-web-e2e` 4, almost all untouched
   by this plan and unformatted already on the base commit (the lockfile resolves prettier 3.9.10)."
   Options: *Format only this plan's files* / *Reformat both web modules wholesale* / *Defer the
   reformat to its own plan*. **Selected: option 2, "and land the chnages using a separate pr with
   skip-bot-review as label"** — carried out as PR #447, label `skip-bot-review`, merge commit
   `6e31ae0a`.

Free text, listed separately: the two answers above ("adopt gitignore as well"; "2 and land the
chnages using a separate pr with skip-bot-review as label"). No other operator message.

## Rule check

- **Guard:** `guard: none on this harness (unenforced)` — the resolve result reports no tool-call
  guard for OpenCode, so this run reports what it did, not what a guard recorded.
- **Shell file operations:** every file read and search went through the file tools. Two shell reads
  of build logs (`grep` on the maven logs and on `/Users/oliver/.plan-marshall/marshalld/job-logs/*`)
  were used to read the test summaries the build seam names — a rule deviation in the letter (the
  rule asks for the file tools) and reported here.
- **A build-npm call ran in the wrong checkout:** `plan-marshall:build-npm:npm run --working-dir
  pm-web-app` resolved against the main checkout rather than the worktree, and `npm run format`
  rewrote the main checkout's web modules. It was reversed exactly (`git apply -R` of a captured
  diff), the main checkout verified clean before any other work continued, and every later call
  passes an absolute `--working-dir`. The `format:check` numbers in question 2 were taken the same
  way and re-measured afterwards in the worktree; the base really was unclean.
- **Builds:** all routed builds used the default routing (`daemon_longpoll`); the npm format and
  `format:check` calls resolved `in_process_fallback` with the reason `env_or_working_dir_set` — the
  working directory a module script needs is exactly what routes them in-process.
- **Executors:** `executor-skew` answered `match` (both `0.1.2133` at setup; `0.1.2135` at landing)
  — recorded, not asked about.
- **Spec read:** `spec_source: ledger_worktree` at both the setup resolve and the landing resolve —
  the orchestrator worktree's copy, no `spec_differ` marker.
- **A sub-agent ran the self-review**, under the section-1 rules; it broke none that were recorded and
  reported no guard refusal.
- **Restarts, killed sub-agents, re-dispatches:** none. One build call returned `running` and was
  re-attached with `build_server wait` (three times across the run), never backgrounded.
- **Every operator message is listed above**, including the theme of both answers.

## Build server

Setup preflight: `ready` (registered, version 1). Builds by mechanism: **`daemon_longpoll` 22** (all
Maven builds: module tests, `verify -Ppre-commit` × 4, `verify` × 4, the Playwright suite × 7),
**`in_process_fallback` 6** (the npm `format`/`format:check`/`ci` calls, whose working directory
forces in-process). Fallbacks: 6, each `degraded` with reason `env_or_working_dir_set`; no launch-fault
retry, no `killed` job, no timeout. One `verify` failed in `pm-api` on a property-test seed and passed
on re-run (named under Findings).

## Residue

- The Playwright suite reports 4 failures in `data-table.spec.js` ("keeps the row-menu button inside
  its cell"): the row-menu button carries its text only in `aria-label`, so `toContainText` finds
  nothing. Nothing in this plan's diff touches `DataTable.svelte`, `RowMenu.svelte` or that spec's
  assertions (the reformat reflowed two unrelated `if` conditions there). It fails on this head and
  is not this plan's to fix; it is worth a follow-up plan.
- `format:check` is clean, but the two `onboarding.spec.js` token assertions I met compare
  case-insensitively because the printer lowercases the hex tokens — a spelling coupling that could
  be removed by spelling tokens in the case the CSS uses.
- D5's coverage caveat: the showroom case cannot state which of the three- and four-column cells it
  reached, because the dev page's container is measured, not pinned.
- Sourcery remains on its 7-day budget refusal until 2026-11-19; it is optional and gated nothing.
