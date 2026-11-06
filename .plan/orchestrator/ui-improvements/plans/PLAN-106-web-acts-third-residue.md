# PLAN-106: Web: What the Acts Second Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off (a SMALL plan: about eight files; the lane form, by the operator's rule of 2026-11-06).
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-06 from the landing analysis of PLAN-100 (`landings/PLAN-100.md`: PR #331 merged with 0 unresolved threads and 0 Sonar issues, but a CodeRabbit review (`5793110125`) the landing never mentions and no one answered, a dead branch shipped as a decided deliverable, two view-level guard tests that cannot reach the guard they name, four fixes without a test, and evidence nobody can open). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-100 bounded the settings reads and the drain acts and fixed the work panel, but the hand-written act-id lists CodeRabbit flagged in a third review are still there, the "unknown running count" of its deliverable 3c can never occur in the product, a failed epic read is never the one that is reported, two of its guard tests claim to test a guard they never reach, four CodeRabbit fixes can be reverted with the suite green, an unknown withdraw is drawn as a definite failure, and the pull request body is stale. This plan makes the act ids derive from their tables, the unknown-count state either real or removed, the guard tests honest, the fixes pinned, and the open review item answered.

## Deliverables

1. **The handled act ids come from the action tables**
   - What: `WorkControlPanel.svelte` ~132-168 holds `PLAN_ACTS_HANDLED` and `EPIC_ACTS_HANDLED`, hand lists that repeat identifiers of `PLAN_ACTS` and `EPIC_ACTS` in `pm-web-app/src/lib/actions/byState.ts` (CodeRabbit review `5793110125`, outside-diff item, never answered; the repository's path instruction calls a hand-written list that must mirror a set defined elsewhere a defect unless it is derived). Derive the permitted ids from the tables with an explicit exclusion for the plan page's `decision` act, so a supported act added to a table appears in the panel or fails a test.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test that fails before: an act added to a table (in the test) appears in the panel's menu and is posted; the handled ids equal the table ids minus `decision` for both subjects (evidence order: `test-first`)
2. **The unknown running count is a real state, or the dead branch goes**
   - What: PLAN-100 D3c made the raise dialogue say the running count is unknown instead of naming zero, but `EpicView.svelte` ~608-609 passes `scopeKnown={resource !== null}` and `running={resource?.running ?? null}` while `EpicResource.running` is typed `number` (`client.ts:598`): `running === null` happens only while `resource === null`, when Next plan is disabled, so `scope > 0 && running === null` is unreachable from any view and the browser check never showed it (all eight "raise" before/after captures are byte-identical). Decide in the design and the code: either an absent `running` from the daemon is a typed, reachable state (the type allows `null`/absence, the view passes it through, a test and a capture show the dialogue text), or the branch, the `control.epic.scope_unknown` text (en, de) and the design sentence are deleted. In both cases say in the pull request that the reply on PR 331's thread `4484094188` ("there the unknown count does ask") was wrong, and reply there. Verify first whether the real `pm-mcpd` omits `running` (another repository: `plan-marshall/plan-marshall-mcp`; if it cannot be read, say `not checkable` and keep the safer reading: a missing count must not start a plan).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
   - Done when:
     - The chosen branch has a test that fails before it and a capture of the state (or the dead code is gone: `git grep -n "scope_unknown" -- pm-web-app/src pm-api/src/main` names none) (evidence order: `test-first`)
3. **The guard tests are honest and the fixes are pinned**
   - What: (a) `layout.test.ts` "refuses a stop-now confirm…" and `SettingsView.test.ts` "refuses a confirm…" never reach `confirmStopNow`'s `drainingBusy` or `confirmRevoke`'s `revoking` (`ConfirmDialog` refuses first through its busy prop), yet their comment says "the dialog's busy prop and the view's own guard both stand… either alone must keep the request single"; rename and re-comment them for what they prove, and prove the view guard with a test that reaches it (the view's handler called directly, or the dialog's guard bypassed in the test) or remove the claim. CodeRabbit nitpick N3 of review `5789601500` asked for exactly this and was declined without fixing the text. The shrink test ("shrinks the drain list when the runtime reports one item less") showed green in the mutation run (log `104112`): show it failing when the shrink code is reverted. (b) Four fixes of PLAN-100 can be reverted with the suite green: `pairBrowser` keeps the lifecycle fields (`runtime.ts`, `lifecycleFieldsOf`; `initRuntime` path too), and CodeRabbit's other three are PLAN-60 deliverable 5's (the destroy guard, abort-in-catch, `onDestroy` abort): pin `pairBrowser` and the `initRuntime` count-only drain here. (c) The `ConfirmDialog` test comment "which the browser's own disabled handling does not stop" is probably false in a real browser (a disabled button swallows the click; jsdom does not): correct it.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each changed or new test fails when the code it covers is reverted, with the reverted code named in the pull request body and the failing assertion quoted (evidence order: `mutation-after`)
4. **Withdraw and stop agree with each other, and the term is the area's**
   - What: (a) An unknown withdraw (`network_offline`, a timeout) is drawn as `system.stopping.withdraw_failed` with the raw error as reason (the browser's English "The operation timed out." in a `de` UI); stop-now has an unknown text, withdraw has none: add `system.stopping.withdraw_unknown` (en, de) and use it. (b) `refreshLifecycle` (`+layout.svelte` ~67-95) races with an act: the epoch advances only in `handleLifecycleEvent` (`runtime.ts`), so a withdraw that times out, starts the 5 s refresh, is retried and succeeds writes `isDraining:false` without advancing it, and the stale snapshot re-shows the banner until the next event: advance the epoch on an act's success. (c) `control.epic.next_waiting` (de) says "Die Epik" where `control.*.de` and `control.cancel_epic.*` say "das Epic" (57 key hits): follow the area, and fix the other key if the area is mixed in the same file.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: the unknown withdraw shows its own text in en and de; a stale refresh snapshot does not overwrite a later successful withdraw (evidence order: `test-first`)
5. **Every item is answered where the reviewer reads it, and the evidence can be opened**
   - What: (a) Reply IN the thread to CodeRabbit's review `5793110125` with the commit or the reason, and to the nitpick N3 and the half-fixed N5 (`SettingsView.test.ts` ~273: PLAN-60 deliverable 5 owns the pending-read test: say so). Thread `z3dUn` of PR 320 (`+layout.svelte:122-125`): CodeRabbit's last word is "remains open" (it could not find `27a4d0d0` on PR 320's head); the code is on `main` now: reply with the merged PR and commit. (b) The 14 of 16 byte-identical before/after captures of PLAN-100 proved nothing for the new states (`next_waiting`, `scope_unknown`, the listener "unknown"): take captures of the states this plan changes, differing before and after, and name them in the pull request (they are gitignored: cite the content of what they show). (c) Keep the pull request body current at merge: the build gate at the final head, the threads and reviews it answers, the open-issue read. (d) The landing message carries `unresolved_review_threads=N` and `review_body_findings_total/handled` as lines of its landing-facts block.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The pull request lists every thread and body item it answers with the commit or reason, and none of its own is unresolved at the head (list the threads through the GraphQL `reviewThreads` read for your own pull request and for PRs 331 and 320) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only for the gate (CLAUDE.md); the web unit tests run through the module's npm scripts under Maven (`pm-web-app` `test:ci`), the browser tests through `pm-web-e2e`.
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, no `innerHTML`/`{@html}`, no arbitrary timer wait in a test (`waitForTimeout`, `setTimeout` that only passes time), the npm scripts `lint`, `format`, `test`, `test:ci-strict`, `quality` stay.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `21060`, app port `21061`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Every `test-first` clause in your landing message cites a FAILING run and a PASSING run of the named test (a passing log is not a failing run); every `mutation-after` clause cites the reverted code, and the evidence is readable from the pull request.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request (never on a side branch), none dismissed without the operator's word, an undecidable read is a stop ("Wait and resume later"). State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES (Follow-ups, Nitpicks and the outside-diff section) as well as the threads: PLAN-100 handled the bodies of two reviews and none of the third, and reported "all handled". Reply IN the thread on GitHub: a list in your pull request body or in a progress file does not answer a thread, and a thread whose last comment is CodeRabbit's is not resolved by you. Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head, and `review_body_findings_total/handled`, as lines of the landing-facts block of your landing message.

## Claim Labels

- OBSERVED: `WorkControlPanel.svelte` ~132-157 holds the two `*_ACTS_HANDLED` sets at `1a36ed5e`; `EpicView.svelte` ~608-609 passes `scopeKnown={resource !== null}` and `running={resource?.running ?? null}`; `EpicResource.running` is `number` (`client.ts:598`); a failed initial `getEpicResource` leaves `resource = null` (`EpicView.svelte` ~158-160); PR 331 has 10 threads, 0 unresolved, SonarCloud 0 new issues, three CodeRabbit reviews with bodies of which `5793110125` has no reply — read by the orchestrator at `1a36ed5e` through git, GraphQL and the bot comment on 2026-11-06
- HYPOTHESIS: a withdraw that succeeds on retry leaves a stale `refreshLifecycle` snapshot able to re-show the banner for under 5 s; the two view-level guard tests never reach the view guard (the dialog's busy prop refuses first); `pairBrowser`/`lifecycleFieldsOf` and the `initRuntime` count-only path have no test; a `de` UI shows the browser's English timeout text in the withdraw failure; the real `pm-mcpd` may omit `running` on an epic resource — by a read-only analysing agent's reading at `1a36ed5e`, not run
- Source: `landings/PLAN-100.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/actions/byState.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.test.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.test.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 1a36ed5e
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at HEAD 1a36ed5e
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD 1a36ed5e
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD 1a36ed5e
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD 1a36ed5e
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at HEAD 1a36ed5e
- OBSERVED: `doc/design/shared/system.adoc` — present at HEAD 1a36ed5e

## Dependencies and Sequencing

- Depends on: `PLAN-100` (shipped)
- Overlaps with: `PLAN-105` (`client.ts`, shared with PR 331's changes: whichever starts second merges main and rebases), `PLAN-60` (deliverable 5 there owns the `SettingsView` loads and its missing tests), `PLAN-45` (deliverable 5 owns the client-level time limit; this plan adds no timeout constant), `PLAN-58` (deliverable 4 owns the failed epic-resource state in `EpicView.svelte`; this plan touches that file at one prop only)
- Should land before: `PLAN-45`, `PLAN-58`, `PLAN-60`, `PLAN-66`, `PLAN-85` (same files)
- Adjacent to: `PLAN-97` (`25-system.adoc` and the CLI drain output must agree with the `system.adoc` sentence that a drain item is a name, not a step)

## Hand-Off Command

Lane: `runbook` (a small plan, by the operator's rule of 2026-11-06).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-106-web-acts-third-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
