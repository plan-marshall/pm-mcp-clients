envelope_version=1
sender_type=plan
sender_id=rb-93-web-format-and-token-residue
epic=ui-improvements
kind=landing
created=2026-11-05T09:59:30Z

```landing-facts
schema=landing-facts/1
plan_id=rb-93-web-format-and-token-residue
epic=ui-improvements
pr=#319
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=3
step.sonar-roundtrip.open_issue_rules=java:S1192,java:S6213
```

guard: none on this harness (unenforced)

# PLAN-93 — Web: token marks from the catalogue, locale-safe formatters, order-independent e2e selectors

Merged as `4dcf77b1` (merge queue, merge-group run `42004975195` success). One pull request, #319,
from `feature/93-web-format-and-token-residue`. Spec read from the ledger worktree
(`spec_source: ledger_worktree`, `spec_differ: true` — the main checkout's copy was the older one).

## 1. Deliverables

**D1 — the token marks and labels come from the catalogue: NOT fully met.**
- One case per form (thousand, million, split, at-least) in `de` and `en`, each failing against the
  literal: **met**, order `test-first`; test `format.test.ts` (`formatTokens`, `formatSplit`, the
  four catalogue-override cases); failing run at `70cd9a82` — 8 failures, `maven-2026-11-04-194600.log`
  ("expected '≥ 0' to be '0'"); green at `70cd9a8`.
- Browser check, "the text is unchanged": **not met as written**. The design fixes the split as
  `{total} tokens ({in} in · {out} out)` and the pack says so, so `formatSplit` now renders `412 k
  tokens (380 k in · 32 k out)` where it used to render `412 k (…)`; every other mark is
  byte-identical. The views were not checked in a browser for this deliverable.

**D2 — the formatters select the catalogue by their `locale` argument: fully met.**
- Test `format.test.ts` "the locale argument selects the catalogue", order `test-first`; failing run at
  `70cd9a82` (`maven-2026-11-04-194857.log`, `expected '9 d ago' to be 'vor 9 T'`); green at
  `a922493`.

**D3 — the activity list and the time helpers cover their edges: fully met.**
- One test per edge, order `mutation-after`: `PlanView.test.ts` (malformed clock, clock without
  seconds, `source.time`), `format.test.ts` (`dayOf` clock-only, year-old age, today under a minute
  and under an hour). Mutated run at `a9224931` — 10 failures including all six covered edges
  (`maven-2026-11-04-195724.log`); restored and green at `bd900e4`.
- Dead key removed from both packs, `ProjectsCatalogueTest` and the design table: **met**, order
  `none`.

**D4 — no e2e selector depends on the order the runtime serves: NOT fully met.**
- Plans picked by title, "some card" specs say so: **met**, order `none` (`tests/helpers/plans.js`,
  `plan-view`, `chrome`, `theme-readability`, `showroom`, `overview`, `mobile`).
- `PM_MOCK_WEB_PORT=20930 PM_WEB_PORT=20931 … verify -Pe2e`: **met** at `a6fc6c7` (359 passed,
  `maven-2026-11-04-200259.log`) and again after the D5 e2e changes at `04948b7` (359 passed,
  `maven-2026-11-04-220752.log`).
- The open-issue read at the head: **not met as written** — the read at the head before the merge was
  `confirmed 3` open new-code issues (`java:S1192`, `java:S6213`), not `0` and not dismissed. The fix
  exists as commit `8f3ca37` on `fix/93-sonar-residue`, pushed but not merged (see Residue).

**D5 — the web findings of CodeRabbit that nobody handled: NOT fully met.**
- One test per item (a)–(d) and (e)'s zero floor that fails before it: **met** for (a), (b), (c) and
  (e)'s zero floor, order `test-first` — failing runs `maven-2026-11-04-201626.log` (a, the reminder
  not in the record) and `maven-2026-11-04-202329.log` (b: the chat transcript gone after a late
  answer; c: the step line not re-measured). (d)'s two are regression pins of code that already
  worked (`findWork` forwarded `signal`; `adoptRouteFromLocation` read `?decision=`), which the
  clause's "fails before it" cannot apply to.
- Item (i), order `mutation-after`: **met** — `CommandPalette.test.ts` (activedescendant moves on
  `ArrowDown`; Tab walks Close → field → Retry); mutated run at `a6fc6c7` with `ArrowDown` a no-op and
  Retry dropped from the cycle (`maven-2026-11-04-203717.log`).
- (f) waits for the loading state to end, (g) the port guard, (h) the build-output check, order
  `mutation-after`: **met** — e2e run with the waits removed and a slow runtime
  (`maven-2026-11-04-204025.log`, 12 failed); the guard failing on a reverted `ports.js`
  (`maven-2026-11-04-215935.log`) and on a removed `strictPort` (`-220011.log`); the build-output
  suite failing with the markers removed (`maven-2026-11-04-220044.log`).
- Browser check: **not met as written** — (a) the reminder survived a reload in `running-work` (5
  days) and `fully-onboarded` (6 days); (c) the step line's Tab stop followed its measurement in `de`
  and `en`; (b) decision chats are not served by the mock (`/api/v1/decisions/<id>/chat` answers
  404), so the two-decision chat could not be exercised in a browser.
- (e)'s optional catalogue-rollback: **met** as a recorded decision — not done, because keeping every
  older catalogue would let a stale pack render after a version could not be read (CC-P-05); the
  rollback path is the runtime re-serving the pack.

## 2. Personas and skills

| Step | Persona |
|---|---|
| D1, D2, D3, D5 | `plan-marshall:persona-implementer` (as the spec declares) |
| D4 | `plan-marshall:persona-integration-tester` (as the spec declares) |
| every self-review round, PR triage | `plan-marshall:persona-code-reviewer` |

Per deliverable, the skills the spec named — all loaded: D1 and D5 `pm-dev-frontend:javascript`,
`pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`, D5
additionally `pm-dev-frontend:lint-config`; D2 and D3 `pm-dev-frontend:javascript`,
`pm-dev-frontend:jest-testing`; D4 the same two. `skills added: none` — no deliverable left the
spec's Expected Surface except `pm-mock-runtime` (see Residue).

## 3. Hypotheses

- *The token marks and `in`/`out` labels are literals and `common.tokens.*` (other than `coverage`,
  `not_measured`) are read by no code in `pm-web-app/src`*: **confirmed** — `format.ts:283-290` and
  `format.ts:336-337` held the literals; the keys had readers only on the Java side
  (`TokenFormat.java:53-67`, `SystemView.java:536`).
- *The formatters' `locale` argument does not select the catalogue words*: **confirmed** —
  `i18n/index.ts:715-727` read `get(messagesStore)` first whatever locale was asked for.
- *`dateOnRead` prints a constructed ISO-like string for a malformed clock*: **confirmed** —
  `PlanView.svelte:335-342` built `…T25:99:00` and `entryClock` (then `:552-553`) fell back to it.
- *`project.explorer.last_active` has no production reader*: **confirmed** — packs, design table and
  `ProjectsCatalogueTest` only; the views render the age through `common.time.*`.
- *The CodeRabbit web findings are present on `main`*: **confirmed** one by one for the files D5
  touches; (d)'s "decision ids may not be unique across plans" was **refuted** — an id is
  `<scope_id>/<question_id>` (`doc/design/shared/decisions.adoc:31`).

## 4. Findings

- Self-review round 1: 12 findings — 6 fixed (doc drift in `word()`/`expectBothLocales`/the session
  history comment, the reload assertion, the guard moved into `scripts/guard-ports.js` with the spec
  importing it, the path scan in the build-output check, the locale reset in `try/finally`, the
  palette's decision-id comment); 2 kept with a reason (the pm-operator import rewrites the
  pre-commit profile produces; the JSON record the operator chose); 4 were preferences or covered by
  a reason above.
- Self-review round 2: 3 findings — fixed 2 (`endTheChat` closes only its own chat; `chatBusyId`
  belongs to one decision's panel); 1 recorded (PLAN-89's PR #314 edits the same test file).
- Self-review round 3: no open findings — `self-review: clear`.
- Sourcery (optional bot, review of `5d9a4de`): 5 findings — 4 fixed in `0ca2f28` (empty input,
  unshown categories, the reload assertion's injected state, a malformed record answering "saved"),
  1 answered with a reason (the locale's own session text before the English fallback: the other
  order would render a German value in English). All 5 threads resolved.
- CodeRabbit (review of `65aa5c7`): 20 findings — 8 inline, 4 follow-ups, 8 nitpicks. All handled in
  `7c7ea71` and answered in PR comment `6651918991`: the mock's conflict value is the record; the
  contract minimums are enforced on both sides; the remind act answers the configured interval; the
  step line re-measures when the catalogue changes; the `strictPort` guard checks the wiring of a
  tested function; the follow-ups (derived `toMap()` entries, the trimmed mock port, the derived
  forbidden-port patterns, the chat generation guard) and the eight test strengthenings are in. One
  answered rather than changed: a token figure rendered before any catalogue shows the key's leaf —
  the app's first paint for every catalogue text. All 8 inline threads resolved.
- No CI finding of this PR's own: the one red test was `ConsoleSettingsTranscriptTest` (see §8).

## 5. Reviewers

| Reviewer | Required | Reviewed | Notes |
|---|---|---|---|
| `coderabbit` | yes (quota-limited) | **yes**, `65aa5c7` (2026-11-05 06:49) | its 20 findings handled and its threads resolved; after the next push (`7c7ea71`) the re-trigger was refused on quota (07:02, "next included review in 30 minutes") and no later review exists. Standing authorization applies: a qualifying review exists, every finding is handled, it was re-triggered once after the last push and refused. |
| `sourcery` | no (quota-limited) | **yes**, `5d9a4de` | 5 findings, all handled, threads resolved |

## 6. Operator questions

1. "PLAN-93 D5 item (a) … the wire cannot carry it … How should I proceed for item (a)?" — options:
   record not met / extend the wire value to a JSON object / add a second setting key / stop.
   **Selected: extend the wire value to a JSON object.**
2. "PR #319's only CI failures are `ConsoleSettingsTranscriptTest.machineSettings` … How do you want
   the transcript failure handled?" — options: fix the stale transcript here / merge despite the red
   checks / stop and hand it back. **Selected: fix the stale transcript here.**
3. "PR #319 is blocked on evidence I cannot obtain: GitHub Actions created no run for the last three
   heads … the Sonar read returns `undecidable` … How do you want to proceed?" — options: you fix the
   workflow triggering / merge on the recorded evidence / stop. **Selected: merge on the recorded
   evidence.** (The workflows started again on the next push; CI and Sonar were green before the
   queue took the PR.)

No free-text operator messages were sent.

## 7. Rule check

- **`executor-skew`**: `match` (executor and lane text both `0.1.2036`), read at the start and
  recorded here.
- **Spec read**: `spec_source: ledger_worktree`, `spec_differ: true` — the main checkout's copy is
  the older one and was not read for the work. The spec was amended during the run (the deliverable-5
  item list gained the (i) clause and the Expected Surface grew); the re-read was recorded in the
  progress file.
- **Guard**: `guard: none on this harness (unenforced)`. `.plan/temp/guard.log` carries no line for
  this run because nothing enforces on this harness; this is not a statement that every rule was
  kept.
- **Rules this run broke** (the run's own actions, not a guard log):
  - One shell call used `git … grep -n "text-guard-baseline" -- pm-web-app` before the file tools
    were used for the same question. It is a `git grep` the lane forbids; the answer was re-derived
    with `glob`/`grep` immediately after.
  - No other shell file operation, redirect, loop, pipe or inline interpreter call was issued; every
    build went through `maven run`, every CI action through `ci`, every script through
    `execute-script.py`.
- **Restarts and re-dispatches**: none; the self-review subagent ran three times (rounds 1–3) as the
  lane prescribes, each re-reading the same diff.
- **Builds**: all in-process; `build_server` was `down (unreachable)` at setup and `ready` at landing
  time; no build was routed to the daemon, so no `degraded`/`killed` job exists.

## 8. Build server and gates

- Setup preflight: `down` (`unreachable`) for the worktree; every build ran `in_process`
  (`mechanism: in_process_fallback`).
- Gate at the merged head `e34987d`: `verify -Ppre-commit -pl pm-api,pm-mock-runtime,pm-web-app,pm-web-e2e`
  green — 1179 tests (`maven-2026-11-05-092202.log`), including the build-output check inside
  `npm run build`. `pm-operator` was excluded from that run only because its transcript test failed
  for a date, and that test was fixed in `65aa5c7`.
- CI on `e34987d`: every workflow green (`build / build (25)`, `build / build (26)`,
  `sonar-build`, `integration-tests / test`, `dependency-review`, SonarCloud analysis); merge-group
  run `42004975195` success.
- One defect of the environment, not of this branch: `ConsoleSettingsTranscriptTest.machineSettings`
  and `TranscriptDialogueIT.consoleSettingsDialogue` failed on untouched `main` because the fixture's
  `8 Sept` device date crossed `formatLastUsed`'s 8-week window. The operator chose to fix it here;
  `65aa5c7` regenerates the one line.

## 9. Residue

- **`fix/93-sonar-residue` (commit `8f3ca37`) is pushed but not merged.** The Sonar read at the merge
  head reported 3 open new-code issues, all on this PR's mock changes: `java:S1192` (the
  `remind_after_days` literal instead of the existing constant) at `MockRuntimeEngine`, and
  `java:S6213` twice (two near-identical refusal tests) in `OnboardingScenarioTest`. The fix commit
  names the key by its constant and merges the two tests into one parameterized test; it could not be
  pushed to the PR branch because the merge queue had taken the PR, and it is not on `main`. **A
  follow-up PR from `fix/93-sonar-residue` closes the three issues.**
- **`pm-mcpd` must accept the `cleanup_durations` record.** The operator chose the record as the wire
  value; `pm-api`'s `CleanupDurations` already models it and the mock accepts both forms, but the
  daemon in `plan-marshall/plan-marshall-mcp` has to be changed to match — until it is, a real
  runtime would refuse (or ignore) the record the web page now sends.
- **PLAN-89's PR #314** still edits `CommandPalette.test.ts`; that file merged here in the same
  shape, so whichever lands second resolves a conflict.
- **D1's "the text is unchanged"** should read "matches the design's split form": the design's own
  pack adds the `tokens` word, which the old code did not print.
- **The decision-chat browser check** stays unverifiable against the mock, which serves chats for
  plan subjects only.
- The `pm-mock-runtime` and `pm-operator` files this PR touches are outside the spec's Expected
  Surface: the mock mirrors the record contract, and the operator files are the pre-commit profile's
  import order plus the regenerated transcript.
