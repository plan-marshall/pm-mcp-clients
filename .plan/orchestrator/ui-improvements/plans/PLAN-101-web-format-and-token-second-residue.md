# PLAN-101: Web: What the Format and Token Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-93 (`landings/PLAN-93.md`: PR #319 merged with 3 open new-code Sonar issues against the zero-findings policy, a mailbox item ignored, a chat-load race only half fixed, tests that cannot fail, and a cleanup-record wire change the conflict dialog does not handle). The Sonar fix of `MockRuntimeEngine.java` and `OnboardingScenarioTest.java` is PLAN-95 deliverable 7, not here. One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-93 moved the token marks into the catalogue and sent the cleanup durations as one record, but it left the conflict dialog printing raw JSON, ignored the palette item of its mailbox, answered a CodeRabbit follow-up (a generation counter for the decision chat) with a claim that does not hold, and wrote tests that pass without the code they cover. This plan finishes those, so that a conflict on the cleanup durations is readable and resends only what the operator changed, a stale chat load cannot overwrite a newer one, and every test named here fails when its code is reverted.

## Deliverables

1. **The cleanup durations record: conflict, refusal and resend**
   - What: (a) `MachineSettingsView.svelte` (~281-284) renders `conflict.currentValue` and `wantedValue` as strings; for `cleanup_durations` they are now JSON records: show the three categories in words (the catalogue), not JSON. (b) "Apply yours again" (~127) resends the stale record including the categories the operator did not change: resend the operator's categories on top of the current record. (c) A refused value shows its minimum (`system.cleanup.durations.minimum` of the design), not the generic `update_failed`. (d) With plans set to "never" (-1) the reminder (`days < 7` check) can still be saved. (e) The wire form of the record has one bullet in `machine-settings.adoc` (~189): state the record, its keys, its minimums and which of the daemon and the mock accept which form; the daemon side (`plan-marshall/plan-marshall-mcp`) is an open item to name with its owner in the pull request, not to build here.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: a conflict on `cleanup_durations` shows words per category and no `{`; apply-again sends only the operator's categories over the current record; a refusal shows the minimum; "never" plans do not block saving the reminder (evidence order: `test-first`)
     - The design records the record's wire form and the open daemon item (evidence order: `none`)
2. **The decision chat load cannot be overtaken**
   - What: `DecisionView.svelte` `openChat` (~611-651) guards a load with `chatDecisionId === id`: open A, open B, reopen A makes the id equal again, so the first load of A passes the check and overwrites the new one, and its error closes the reopened panel. Add a generation (request counter) per open and check it at every continuation (the load, the answer, the error). The lane's reply to the CodeRabbit follow-up said reopening "sets chat = null first"; that does not stop the race.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test that fails before: A, B, A with the first A load resolved last does not overwrite the second A's chat, and a late error of the first load does not close the reopened panel (evidence order: `test-first`)
3. **The palette selector (PR 314 thread) and the weak tests of PLAN-93**
   - What: (a) `CommandPalette.test.ts` (~76-77) `focusable` selector gets `iframe`, `object`, `summary` and a case that fails when one sits inside an option; reply on the CodeRabbit thread of PR 314 (`CommandPalette.test.ts:77`, review `5775714988`) and resolve it (mailbox item of PLAN-93 that was not done). (b) `ProjectOnboardingView.test.ts`: `vi.spyOn(..., 'clientWidth').mockReturnValue(box)` is evaluated when `box === 0`, the later assignment never reaches the mock, so the locale part of the test cannot fail; make the mock a getter that reads the variable. (c) The `DecisionView` test that relies on one `tick()` to flush the release: wait on the condition. (d) `check-build-output.mjs` `readText` returns `''` on a read error, so the check fails open: it fails. `DEV_MARKERS` is a hand-written list and `strictPortIsBound` a regex over the config text: derive the markers from the source they guard, or state in the test why not.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`)
     - The PR 314 thread has a reply with the commit and is resolved (evidence order: `none`)
4. **The tokens word and the stated reasons**
   - What: (a) `formatSplit` prints `412 k tokens (…)` per the design table (`texts-and-languages.adoc` ~223-240) while the labels beside it already say "tokens so far" (`plan_view.en`), "Tokens:" (`EpicView`) and "Tokens (in · out)" (`system.models.col.tokens`): the design settles which one carries the word; change the design and the labels together, in `en` and `de`. (b) The decision not to build catalogue rollback retention cited "CC-P-05", which in this repository is CLI-CC-P-05 (the operator gate) and unrelated: the reason to record is the operator's (`persistCatalogue` keeps one catalogue per locale on purpose; a missing entry is fetched from the runtime); fix the PR or code comment and the design note that carry it. (c) The eight CodeRabbit inline threads of PR 319 each have one comment and the answer sits in a PR-level comment: reply in each thread with the commit (they are resolved already).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-documents:ref-asciidoc
   - Done when:
     - The design names one place for the word "tokens" and the labels in `en` and `de` agree with it (evidence order: `none`)
     - The pull request lists the PR 319 and PR 314 threads with the commit or reason of each, and none is unresolved at the head (list them through the GraphQL `reviewThreads` read) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0`: every open issue is fixed in this pull request (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-web-app verify` and, with `PM_MOCK_WEB_PORT=21010 PM_WEB_PORT=21011`, `./mvnw -pl pm-web-e2e verify -Pe2e` pass at the head (a failure beyond the base names is yours).
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, flat ESLint config, no `innerHTML`/`{@html}`, no `waitForTimeout` and no `setTimeout` that only passes time; the npm scripts `lint`, `format`, `test`, `test:ci-strict` and `quality` stay.
- Every user-facing text goes through the catalogue (`$t(...)`) and sits in both `en` and `de`; the text guard (`text-guard-baseline.json`) only shrinks.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `21010`, app port `21011`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Browser check with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario running-work --web-port 21010"`), then `PM_MOCK_WEB_PORT=21010 ./pm-web-app/start.sh -p 21011`; pair again every session with a fresh single-use code.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues; every open issue is fixed in a commit of THIS pull request (never on a side branch), none is dismissed without the operator's word, and an undecidable read is a stop ("Wait and resume later"), not a merge. State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads. Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head in your landing message (four lanes in a row reported "all handled" with threads open).

## Claim Labels

- OBSERVED: `CommandPalette.test.ts` `focusable` selector has no `iframe`, `object`, `summary` at main `4dcf77b1` — read by the orchestrator on 2026-11-05
- OBSERVED: `DecisionView.svelte` has no generation counter; the chat continuations compare `chatDecisionId` only (lines ~357, 376, 611, 636-651) — read by the orchestrator at `4dcf77b1`
- OBSERVED: the PR 314 thread `CommandPalette.test.ts:77` is unresolved — read through GraphQL
- HYPOTHESIS: the conflict dialog prints the record as raw JSON, "apply yours again" resends the stale record, a refusal shows `update_failed` instead of the minimum, "never" plans block the reminder — by a read-only analysing agent's reading at `4dcf77b1` (`MachineSettingsView.svelte` ~127, ~281-284), not run
- HYPOTHESIS: the clientWidth mock of `ProjectOnboardingView.test.ts` is evaluated at `box === 0`, the single `tick()` of the `DecisionView` test may pass without the guard, `check-build-output.mjs` fails open — same reading
- HYPOTHESIS: the daemon (`plan-marshall-mcp`) does not yet accept the `cleanup_durations` record — not checked (another repository)
- Source: `landings/PLAN-93.md`, `landings/PLAN-89.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.svelte` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.test.ts` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.test.ts` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/format.ts` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/scripts/check-build-output.mjs` — present at HEAD 4dcf77b1
- OBSERVED: `pm-web-app/src/lib/buildOutput.test.ts` — present at HEAD 4dcf77b1
- OBSERVED: `doc/design/shared/machine-settings.adoc` — present at HEAD 4dcf77b1
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD 4dcf77b1

## Dependencies and Sequencing

- Depends on: `PLAN-93` (shipped)
- Overlaps with: `PLAN-95` (deliverable 7 edits the mock for the Sonar fix: disjoint files), `PLAN-98` (running; the CLI console: disjoint), `PLAN-99` and `PLAN-100` (running; other views: disjoint)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-101-web-format-and-token-second-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
