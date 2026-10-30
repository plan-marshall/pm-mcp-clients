# Epic: UI improvements of both clients from the Claude review

slug: ui-improvements

> Hand-written narrative for one epic under `.plan/orchestrator/ui-improvements/`. The layout and
> authority contract live in the central standard — see
> `persona-plan-orchestrator/standards/orchestration-model.md`. The ledger JSON files
> (`status.json`, the `queue/{PLAN-ID}.json` rows) and `resume_anchor.md` are the machine
> authority; any statement here that conflicts with them is stale prose.
>
> START HERE and the Ordered Queue are not in this file. They live in the generated,
> git-tracked `queue-view.md` next to it (see the Persist / Stop-Resume Contract in that
> standard). `queue-view.md` is never hand-edited. A merge conflict in it is never merged by
> hand: merge the source files, run
> `python3 .plan/execute-script.py plan-marshall:plan-orchestrator:orchestrator regenerate-view --slug ui-improvements`
> on the merged tree, and `git add` the result.

## Vision

Implement the review of 2026-10-25 in `doc/claude_review/` (web app `pm-web-app` and terminal client
`pm-operator`): every plan step of its four aspect folders per client (use cases, UX, visual design,
cross-cutting) becomes a shippable plan, and every verification check becomes part of a separate
verification plan. The goal is two clients that serve the use cases, lead the operator to the next act,
and look like a product a team would pay for. The epic is too large for one plan: the review names
about seventy pull requests across five surfaces. It is done when every implementation plan has
landed, every verification plan has recorded its checks, and the analysis matrices of the review show
the moved verdicts — or a remaining item waits on an operator decision recorded here.

Source: `doc/claude_review/README.adoc` (web) and `doc/claude_review/cli/README.adoc` (terminal); each
plan spec names the plan, proposal and finding documents it comes from.

## Queue annotations

- Lanes — every implementation plan runs on the `runbook` lane (opencode, `/plan-marshall-runbook-lane`);
  every `verify-*` plan runs on the `plan-marshall` lane, separate from the implementation it checks.
- Three tracks — web (WS-02, WS-03), terminal (WS-04, WS-05), shared
  (WS-01) — with `parallelization_scope` raised from 3 to 4 on 2026-10-29 at the operator's word (the fifth plan
  past 3 in practice). The tracks touch disjoint modules (`pm-web-app`/`pm-web-e2e` against `pm-operator`), so one
  plan per track can run at a time, the fourth slot going to a second plan on a track whose specs are pairwise disjoint; the shared catalogue `pm-api/src/main/resources/messages/` is split
  per area, so cross-track overlap is mostly one file or none.
- Web chain — the web track is the critical path (17 levels): foundation (PLAN-01 to PLAN-41), shell and
  templates, views, then onboarding, light theme, cleanup, guard in failing mode, docs, final check.
- PLAN-02 (`cli-pty-dialogue-harness`) — shipped (PR #232). Its seven `*DialogueIT` classes are
  `@Disabled`, each naming its fixing plan: PLAN-10 (pause, cancel), PLAN-17 (runtime stop), PLAN-21
  (answer), PLAN-22 (onboarding, project setup), PLAN-23 (cleanup). Each of these removes its own
  `@Disabled`. The `pm-operator` coverage bundle stays below 80 % in `-Pcoverage,integration-tests`
  until they do.
- PLAN-32 (`cli-config-contract`) — parked: waits on the operator's decision which config contract is
  right (CLI-UC-P-07).
- PLAN-08 (`use-case-documents`) gated PLAN-46 (command palette), PLAN-49 (attention inbox) and
  PLAN-36 (`needs`): UC-PLN-7 and UC-PLN-8 had to be agreed first. They are `AGREED` (2026-10-30); the three plans now
  wait for PLAN-82 (`needs-and-find-contract`) instead.
- PLAN-81 to PLAN-84 (staged 2026-10-30) carry the three work packages of PR #278 (`overview-order-and-size`,
  `needs-and-find`, `web-moving-between-plans`) into the epic. PLAN-81 (stable order, size of change) comes first;
  PLAN-82 (design of the two views, accepted by the operator, then the contract and the mock) follows it; PLAN-36, 46, 49
  and 84 consume PLAN-82; PLAN-83 (work list beside a plan, Add a plan on the row, one notification for a burst) needs
  PLAN-81 and its work-list design accepted by the operator. Re-grounded notes sit in PLAN-34, 36, 46, 49, 51, 52, 54, 55,
  57 and 59.
- PLAN-76 (`shared-port-assignment`) shipped (PR #243). Every staged plan that names a port block passes its
  own ports (`PM_MOCK_WEB_PORT`, `PM_WEB_PORT`, `--web-port`, `-p`), so plans with blocks may run side by
  side. PLAN-03 predated the blocks and ran on the defaults `7420`/`5173`; it shipped (PR #253), so the defaults
  are free again.
- Cross-repo plans — PLAN-08 and PLAN-24 land their main change in `plan-marshall-documentation`; their
  Expected Surface names only the files of this repository.
- PLAN-03 (`mock-scenario-states`) — shipped (PR #253; PR #250 was closed unmerged and re-opened on a fresh
  branch for a fresh Sonar analysis). It changed 18 files against 9 declared, among them four `pm-web-e2e`
  specs and the `pm-operator` control-epic transcript. These specs declare files PLAN-03 changed and are to be
  re-checked at HEAD before emitting: PLAN-11 (six files), PLAN-47, PLAN-52, PLAN-55, PLAN-58, PLAN-62 (an e2e
  spec each) and PLAN-50 (the transcript). None of the in-flight plans (10, 15, 19) overlaps. PLAN-11 is free
  of its PLAN-03 dependency.
- PLAN-15 (`cli-footer-and-help-overlay`) — shipped (PR #256). It changed 44 files against 8 declared: all 28
  files of `doc/design/terminal-client/` (an operator decision, question 3), the `operator.{de,en}.properties`
  catalogue, three level classes and four more tests. `TuiScreen.java` was declared and not touched. Its D2
  clause for 80 columns is not met as written (the overview and the activity fit their whole footer, so they
  show `? help` and not `? more`); the code follows the design. Specs to re-read at HEAD before emitting,
  because they declare files PLAN-15 changed: PLAN-13, 17, 20, 21, 23, 26, 30, 32, 36, 42, 48. PLAN-38 and
  PLAN-56 have their PLAN-15 dependency met.
- PLAN-19 (`web-ui-icon-set`) — shipped (PR #255). It changed 77 files against 9 declared: 41 icons (not the 42
  its text claims), 17 views, components and utilities that drew an emoji, the `control.{de,en}.properties`
  catalogue and two e2e specs it repaired after merging `main`. `CleanupView.svelte` was declared and not
  touched (the banner sat in `SystemView`). The `emoji` entries are gone from `text-guard-baseline.json`.
  Specs to re-read at HEAD before emitting, because they declare files PLAN-19 changed: PLAN-09, 16, 39, 43,
  47, 51, 53, 54, 55, 57, 58, 59, 62, 64, 66, 73, and PLAN-68 and PLAN-69 for `HarnessChoice.svelte` (PLAN-69
  lists the monogram as its own deliverable; PLAN-19 delivered it). PLAN-25, 27 and 41 have their PLAN-19
  dependency met.
- PLAN-10 (`cli-choice-prompt-pause`) — shipped (PR #257, merge `81f84ee3`). It changed 21 files against 11
  declared: the mock engine and its test, four design documents (`consents.adoc`, `03-dialogues.adoc`,
  `19-actions.adoc`, `27-tui-testing.adoc`), `ActionSupport`, `PlanCommand`, `StartPrompt`, `PlanCommandTest`,
  `ScreenCaptureIT` and the capture text. `control.{de,en}.properties` were declared and not touched. D1
  (`ChoicePrompt`) is as specified; D2 shipped with two clauses not met as written, both approved by the operator:
  the browser regression check (the changed `operator.*` texts are terminal-only, so no web view shows them) and
  the per-point pause consequence (the actions resource carries no such data). Sonar gate `OK`, 0 open issues,
  `new_coverage` 87.6 %; CodeRabbit refused the last two heads on quota. Specs to re-read at HEAD before emitting,
  because they name `ChoicePrompt`: PLAN-17, 22, 23; because they declare the operator catalogue pair PLAN-10
  changed: PLAN-13, 14, 17, 21, 23, 30, 36, 42. The operator catalogue is no longer held by a plan in flight.
- PLAN-25 (`web-core-components-controls`) — shipped (PR #260, merge `8b19a4a7`). All four deliverables met. It
  changed 20 files against 8 declared and left none of the declared untouched: four component tests, the dev-route
  exclusion (`lib/dev/devRouteExclusion.ts` and its test, `vite.config.ts`), `lib/utils/uniqueId.ts`,
  `+layout.svelte`, `common.{de,en}.properties` and `interaction-affordances.adoc` (the spec asked for it).
  `size="small"` now renders at ten call sites in `DecisionView`, `ReportsView` and `ProjectExplorerView`. Sonar
  gate `OK`, 0 open issues. Specs to re-read at HEAD before emitting: PLAN-31 and PLAN-35 (both declare the dev
  page as created by themselves; it exists now), and those declaring `common.{de,en}.properties` (PLAN-04, 06, 34,
  39, 45, 46, 73), `+layout.svelte` (PLAN-39, 66) or `interaction-affordances.adoc` (PLAN-47, 74). PLAN-31 has its
  PLAN-25 dependency met (and PLAN-76); PLAN-41 still waits for 31 and others.
- PLAN-08 (`use-case-documents`) — shipped (PR #259, merge `eba84c17`; sibling `plan-marshall-documentation` PR #128,
  `24eabd4`). All three deliverables met. In this repository it changed exactly the two declared design files; the
  use cases themselves (UC-PLN-7 and UC-PLN-8 as `DRAFT`, variations in UC-PLN-2, UC-PLN-4, UC-PRJ-1, UC-ONB-1) are in
  the sibling repository, opened and merged with `gh` on the operator's two explicit answers. The operator agreed
  "as written" in the PR; the use cases stay `DRAFT` until a separate act flips them to `AGREED`. Sonar:
  `undecidable (docs-only skip)`, as decided 2026-10-28. PLAN-36, PLAN-46 and PLAN-49 have their PLAN-08 dependency
  met (each needs others as well).
- PLAN-27 (`web-brand`) — shipped (PR #262, merge `4c39c0f8`). All three deliverables met. It changed 18 files
  against 7 declared: six of the declared (all but `src/app.html`) and twelve undeclared (the favicon module and its
  test, `brand-mark-attention.svg`, the `+layout.svelte` wiring, `PairingView.scenarios.test.ts`, the pairing
  captions `pairing.{en,de}.properties`, three design pages and two brand diagrams). Two operator-accepted
  departures: the 64 px test rests on a mutation-after run, not test-first (question 3), and the QR now shows on
  every listener under a caption that tells the truth (question 2). The 28 px plate is visible in no artifact until
  PLAN-39 puts the rail head on screen. Sonar gate `OK`, 0 open issues (confirmed, not the docs-only exception).
  Notes added to PLAN-39 (rail head renders `<BrandMark size={24} plate />`) and PLAN-49 (point the badge, the tab
  title and the tab icon at the attention total). PLAN-39 has its PLAN-27 dependency met (it also needs PLAN-31).
- PLAN-31 (`web-core-components-containers`) — shipped (PR #266, merge `0351ce78`). All three deliverables met. It
  changed 23 files against 8 declared and left none of the declared untouched: the seven container components
  (`Card`, `KeyValueList`, `Tabs`, `Breadcrumb`, `Dialog`, `Toast`, `AttentionStrip`) with the dev page extended, plus
  fifteen undeclared files (seven tests, `shortenMiddle.ts` and its test, four fixtures, `common.{de,en}.properties`,
  the last named in its Standing Constraints). Insertions only. The components have no caller yet beyond the dev page.
  Sonar gate `OK`, 0 open issues. CodeRabbit never reviewed the final head (merge under the standing authorization,
  its review on `d04ffe00` had every finding fixed). One run slip: the properties were first edited in the main
  checkout and reverted. PLAN-39 (shell; note added), PLAN-37 (accessibility baseline) have their PLAN-31 dependency
  met; PLAN-35 still needs PLAN-16, PLAN-41 others.
- PLAN-11 (`mock-cli-fixes`) — shipped (PR #269, merge `bae3a8c8`). All three deliverables met. It changed 21 files
  against 7 declared entries: four declared entries changed (engine, `reports-and-system.json`, the README, the
  runtime test directory) and three left untouched (`mock/http/`, `plan-view.spec.js`, `system.spec.js`), plus
  fifteen undeclared files (three more scenario files, two scenario tests, `plan_view.{de,en}.properties`, the root and
  operator READMEs, six `pm-web-app` files so the app shows "not found" and the draining banner). The mock now serves
  a plan per id with `404 plan_not_found`, `/api/v1/plans` in every scenario, and the `kind="item"` residue is fixed.
  Sonar gate `OK`, 0 open issues. CodeRabbit never reviewed the merge head (standing authorization). Self-review ran
  out its budget (three rounds and a confirmation round). The two stale mock epic routes were not in the spec and are
  still stale.
- PLAN-13 (`cli-event-contract`) — shipped (PR #270, merge `7e2b4b61`). All three deliverables met. It changed 23 files
  against 10 declared entries: seven changed (`EventKind.java` in the declared `event/` directory, `ConsoleNotice`, its
  test, the new `ConsoleNoticeIT`, `04-console.adoc`, the operator catalogue pair) and three untouched (`EventName`,
  `ConsoleNoticeBuffer`, `ConsolePrompt`), plus sixteen undeclared files (`EntryFlow` and its test, `PmConsole` and two
  tests, `OverviewFullScreenApp` and its test, the mock engine and its test, `PtySession`, `ScreenCaptureIT`,
  `SummaryLineIT`, `TuiCapabilityIT`, `27-tui-testing.adoc`, the capture set, `EventKindTest`). The console now reads
  the event name first, shows badges at the prompt and prints the answered-elsewhere notice. Sonar gate `OK`, 0 open
  issues. CodeRabbit reviewed only the head `a0820cde`; later pushes went unreviewed (merge under the standing
  authorization). Four self-review rounds (the fourth on the operator's word; clear). Two run slips: a heredoc `python3`
  edit and a `perl -0pi` edit in the shell. The web alignment is only a note in the PR: no staged plan owns it.
- PLAN-20 (`cli-actions-by-state`) — shipped (PR #273, merge `a07a09b6`). Its one deliverable was met. It changed 11 files
  against 5 declared entries: three changed (`ActionMenus`, `ActionsMenu`, `ActionMenusTest`), two untouched (`HoldActs`,
  `19-actions.adoc`) and eight undeclared (`22-texts-actions.adoc`, the catalogue pair, `WorkActionsClient`,
  `OverviewFullScreenApp`, `ControlDialoguesTest`, `ActionsCatalogueTest`, the capture file). The `a` menu of a waiting plan
  now offers `!  Decide <question>…` first (one-character key, unique in every menu, both languages). Sonar gate `OK`, 0
  open issues; CodeRabbit reviewed the merge head in its summary marker. Six self-review rounds (the operator authorised
  four and five) and one merge-queue ejection on infrastructure, re-enqueued. The `decide` dispatch has no unit test; its
  PTY path is PLAN-21's.
- PLAN-14 (`cli-gate-scope`) — shipped (PR #277, merge `5348e89e`). All three deliverables met. It changed 45 files
  against 9 declared entries: eight changed (`OperatorGate`, `PmOperator`, `PmConsole`, `OperatorGateTest`, `PtySession`,
  the new `OperatorGateScopeTest`, the catalogue pair) and `OperatorContext` untouched, plus thirty-seven undeclared files
  (eighteen command and flow classes, `WorkActionsClient`, `OverviewFullScreenApp`, the two onboarding sources, four tests,
  the capture file, the README, nine design documents). The gate now guards acts only (shell verbs, console lines, full-screen
  keys); the refusal text comes from the catalogue and names the marker. Two decisions with the operator: `plan <id> --print`
  was added (PLAN-42's flag, present for `plan` only) and the whole `cleanup` verb stays an operator act (documents fixed).
  `drain` is not gated. Sonar gate `OK`, 0 open issues. CodeRabbit reviewed two earlier heads; the merge head went in under
  the standing authorization. The `PM_OPERATOR_DEV_ALLOW_AGENT` question is still open.
- PLAN-22 (`cli-state-dialogue-onboarding`) — shipped (PR #275, merge `fa178dec`). All four deliverables met; the
  `test-first` order of D1 to D3 is not evidenced (`mutation-after` for D1, none for the two ITs, which were already
  written and `@Disabled`). It changed 28 files against 13 declared entries: twelve changed (`StateDialogue`,
  `EntryFlow`, `OnboardCommand`, `ProjectCommand`, the unit test, both ITs, `OnboardCommandTest`, the `onboarding` and
  `projects` catalogue pairs) and one untouched (the `projects/` directory), plus sixteen undeclared files (two dialogue
  sources, three more tests, `PmConsole` and its test, the `operator.*` pair, two design documents, the mock engine and
  its test, two capture files). Sonar gate `OK`, 0 open issues; CodeRabbit reviewed the merge head. Self-review budget
  exhausted after three rounds: its two last findings went to the open follow-up PR #276 (see Watches). `OnboardingDialogueIT`
  and `ProjectSetupDialogueIT` are enabled; `RuntimeStopDialogueIT`, `AnswerDialogueIT` and `CleanupDialogueIT` stay
  `@Disabled` for PLAN-17, 21, 23.
- PLAN-39 (`web-app-shell`) — shipped (PR #274, merge `44b051ff`). All three deliverables met. It changed 37 files
  against 13 declared entries: all 13 changed (the `lib/shell/` directory with `AppShell`, `NavRail`, `TopBar`,
  `BottomBar`, `+layout.svelte`, `runtime.ts`, the three e2e specs, the design README, the catalogue pair) plus
  twenty-five undeclared files (`nav.ts` and test, `LiveIndicator` and test, `client.ts`, `streamState.test.ts`, four
  shared views and components, ten e2e files, `MockRuntimeEngine` and its test). `+layout.svelte` is now boot-only; the
  rail, top bar, bottom bar (phone, with a More sheet on `Dialog`) and the live indicator (four stream states, `Try
  again`) replace the header and the offline banner. The operator had the run fix a pre-existing e2e failure in the mock
  (a plan listed only in an epic answered `plan_not_found`). Sonar gate `OK`, 0 open issues. CodeRabbit reviewed two
  earlier heads and refused on quota for the merge head (merge under the standing authorization, confirmed by the
  operator). Phone Inbox item deferred to PLAN-49, search trigger disabled until PLAN-46.
- Cleanup 2026-10-28 (HEAD `cc5794c9`): the claims of the 64 staged and parked specs were re-grounded and 83
  verdicts are persisted (`ui-improvements/cleanup`); PLAN-10 (launched) was left alone. Two claims were
  contradicted and absorbed into the spec: PLAN-25 (today's `Button` already has the four variants and 18 views
  import it, so deliverable 1 is additive) and PLAN-30 (`LiteralTextGuardTest` already covers `askRaw`; the gap is
  its guarded list, which misses `AnswerCommand`, `CleanupCommand`, `RuntimeCommand`). PLAN-11 deliverable 2 is
  partly done by PLAN-03 (`lifecycle_state` is served for `running`, `draining`, `stopped`, `upgrading`,
  `stopping`) and now carries the `kind="item"` fix. One claim stays `unverifiable`: PLAN-16 claim 1 (a stale
  locale cache causes CC-F-10) needs a runtime repro. The verify-first clauses stay open by design: each plan
  settles its own.

## Decisions

- 2026-10-25 — Parallelization scope 3, stated by the operator in the invocation.
- 2026-10-25 — Operator decisions of VD-S-00 and the dependency questions (AskUserQuestion): Inter is
  self-hosted (PLAN-12); own SVG icon set, no Lucide (PLAN-19); axe-core may be added to `pm-web-e2e`
  (PLAN-37); stylelint may be added to `pm-web-app` (PLAN-72). Recorded in the design by PLAN-12.
- 2026-10-25 — Landing of the ledger: the store `.plan/orchestrator/` is git-ignored here and
  `orchestrator.use_worktree` is off, so the `land` verb cannot run. The operator chose: add
  `!.plan/orchestrator/` and `!.plan/archived-orchestrators/` to `.gitignore`, commit the ledger on a
  branch, open a pull request, and merge it through the merge queue once CI is green.
- 2026-10-25 — Merged review steps that touch the same surface (alternatives: one plan per step, which
  would serialize anyway): CC-S-03 with the theme part of VD-S-01; the format helpers of VD-S-04 built
  once in CC-S-04 (`lib/display/format.ts`, not `lib/utils/format.ts`); UX-S-05 with UC-S-03; CC-S-06
  folded into the plans of the views it names (running work, epic, actions by state); UX-S-11 split into
  epic, projects, and reports-and-designer; UC-S-09 split between start and projects; CLI-CC-S-03 with
  CLI-UX-S-03; CLI-UX-S-01 with CLI-UX-S-02; CLI-VD-S-03 with CLI-VD-S-05; the time rule of CLI-CC-S-04
  and the state words of CC-P-01/CLI-CC-P-02 pulled into one shared plan (PLAN-06) that both display
  layers consume; `/mock/acts` built with the PTY harness (PLAN-02).
- 2026-10-25 — Split-guard verdicts: VD-S-03 (13 components) split into controls and containers; UC-S-04
  (11 screens) split into three plans plus the settings conflict in PLAN-60. No staged spec has more than
  four deliverables.
- 2026-10-25 — VD-S-06 (apply components view by view) and the doc travel of UX-S-13/VD-S-09 are standing
  constraints of every view plan, not separate plans; PLAN-74 closes the documents.
- 2026-10-25 — Verification grouped per milestone (13 plans) rather than one per implementation plan; each
  writes `doc/claude_review/verification-results/{slug}.adoc` and files every failed check as a `finding`
  to the epic inbox.

- 2026-10-25 — Browser checks on every output change (operator request: opencode now has the Playwright MCP
  server of `opencode.json`). Every runbook deliverable that changes what the web app renders carries a
  Done-when "Browser check (Playwright MCP)" with its scenario, states and routes. The procedure is a
  standing constraint taken from `AGENTS.md` § Browser checks: mock first, then the app; fresh pairing
  per session; `de`/`en`, dark/light, 1440 and 390 px; baseline and after snapshots plus screenshots
  under `.plan/temp/browser/{plan}/`. It fails closed: no MCP server, no met clause. That covers 37
  plans: all web plans except `web-design-docs-closure`, which changes documents only, plus the two
  mock plans and the shared catalogue plan. Nine terminal plans that change catalogue areas the web
  also renders get one browser regression check. Verification plans (`plan-marshall` lane) keep their
  Chrome walks.
- 2026-10-25 — Sonar gate: added the non-secret Sonar block (`organization`, `project_key =
  plan-marshall_pm-mcp-clients`) to `.plan/marshal.json` through `manage-providers credentials edit`
  (operator request); `runbook_lane resolve` now reports `sonar: configured`. The specs keep stating the
  gate.
- 2026-10-25 — Terminal checks on every output change (operator request). There is no terminal MCP
  server, and the operator gate (`PM-SEC-3`) refuses coding agents by design, so the check runs through
  the PTY harness: PLAN-02 gains `ScreenCaptureIT`. It runs a named set of captures (scenario, states,
  command, keys, size, locale, environment) and writes the screen text and the per-cell attributes to
  files that the agent reads before and after its change. 21 terminal plans have a "Terminal check
  (screen capture)" Done-when on every output-changing deliverable. The two documentation-only
  terminal plans have none. PLAN-05 now depends on PLAN-02. The `cli` verification plans use the same
  harness and never run `pm-operator` in the agent's shell. Alternative not taken: driving
  `pm-operator` from the agent's shell with a cleared environment. That would work around the gate
  outside the tests.
- 2026-10-25 — `opencode.json` already starts Playwright MCP with `--isolated` and
  `--output-dir .plan/temp/playwright-mcp`; the browser checks name their files there.
> ↪ Relocated to `settled.md` § "Decision of 2026-10-26: round 1 cancelled and re-launched" — the runbook lane restart is done and the plans it concerned have shipped

- 2026-10-26 — `orchestrator.use_worktree` switched on (operator request, PR #230): the ledger lives in
  `.plan/local/worktrees/_orchestrator` on `chore/orchestrator-ledger` and reaches `main` through
  `land`, so the main checkout stays clean while plans run.
> ↪ Relocated to `settled.md` § "Landings of PLAN-02 and PLAN-04 (2026-10-27)" — both plans are shipped and their follow-ups sit in Watches and Open Defects

- 2026-10-27 — Port collisions between concurrent plans (operator report: the mock, the app and the
  terminal client connecting to the mock block each other's ports). Staged PLAN-76
  (`shared-port-assignment`, WS-01).
  - The caller passes the ports. The defaults `7420`/`5173` stay for the operator's own loop.
  - Every staged spec that starts the mock, the app or the e2e suite (56 specs) names its own block: mock
    `20000 + 10 × NN`, app `+1`. Its build commands and browser-check procedure pass these ports, and it
    depends on PLAN-76.
  - Specs that are in flight or shipped (PLAN-01 to PLAN-05) are left as they are. Terminal-only specs
    without a mock start are left too: their PTY harness runs an in-JVM mock with no web port.
  - Emitted at once by operator instruction, although all three slots are in use (a fourth concurrent
    plan). It overlaps the in-flight PLAN-01 (`pm-web-e2e/tests/`), PLAN-03 (`MockRuntimeEngine.java`,
    `pm-mock-runtime/README.adoc`) and PLAN-05 (`pm-operator` tests). The operator chose the emit; the
    later of each pair rebases.
> ↪ Relocated to `settled.md` § "Landings of PLAN-01, PLAN-76, PLAN-07 and PLAN-06 (2026-10-27 to 2026-10-28)" — all four plans are shipped; their unmet clauses and follow-ups were routed when drained

- 2026-10-28 — `doc-review` under the runbook lane: its checker (`trace.py`, in the sibling documentation
  repository) does not run through the executor. PLAN-06's operator accepted an equivalent review. The
  five staged runbook specs that ask for it (PLAN-15, 42, 48, 70, 74) now name that equivalent:
  `docs review` plus `asciidoc validate`/`verify-links`, clean. The 13 verification specs run on the
  `plan-marshall` lane, where the skill runs, and are unchanged.
> ↪ Relocated to `settled.md` § "Emits, launches and landings from PLAN-15 to PLAN-13 (2026-10-28 to 2026-10-30)" — sixteen drains and emits of plans that are all shipped (PLAN-15, 19, 12, 10, 25, 08, 27, 31, 11, 13) and two ledger lands

- 2026-10-30 — Standards conformance (operator request, pasted analysis task): the Java and web modules were checked
  against the marketplace standards the project is configured for (`java-cui` in `.plan/marshal.json`; `pm-dev-java`,
  `pm-dev-java-cui`, `pm-dev-frontend`, `pm-dev-frontend-cui`). Java: 0 of 267 test classes use `Generators` or
  `@EnableGeneratorController`; `pm-operator` declares `cui-test-generator` and `cui-test-juli-logger` and uses neither;
  `pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime` declare neither; the one logger (`MockRuntimeExtension`) logs two
  `ERROR` lines with string literals and has no `LogAsserts` test; `@NullMarked` is on 4 of 49 packages and the three
  modules without JSpecify cannot take it without a dependency. `cui-http-testing` does not fit (HTTP runs over a Unix
  socket or tests the repository's own server) and is not applied. Web: largely in line (ES modules, flat ESLint with the
  security plugin, Prettier, lockfiles, no HTML-parsing sink in production code); gaps are the `javascript` domain globs
  (no `.ts`, no `.svelte`), no `pm-dev-frontend-cui` domain, `pm-web-e2e` without `test:ci-strict` and `quality`, six timer
  waits in tests. The operator approved on the same day: test-scope `cui-test-generator` and `cui-test-juli-logger` for
  `pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime`; "foundation first, the rule goes forward" (no hard gate on the other
  plans); one `LogMessages` holder per module, never one for the repository. Staged: PLAN-77
  (`cui-standards-foundation`: dependencies and the `CLAUDE.md` rule, logging, `@NullMarked` in the two modules that have
  JSpecify, tests of `pm-relay`, `pm-exec`, `pm-mock-runtime`), PLAN-78 (`cui-testing-api`), PLAN-79
  (`cui-testing-operator`), PLAN-80 (`web-standards-conformance`), all gate-clean except PLAN-79 (overlap prompt with the
  running PLAN-14, 20, 22: emit when no terminal plan runs). PLAN-77 and PLAN-80 are the highest-priority emits and both
  add a paragraph to `CLAUDE.md`. Skills lines of the 39 staged and parked specs that carry a Java or web deliverable were
  adapted (Java: `javadoc`, `java-null-safety`, `cui-testing`, `cui-logging`, `cui-testing` on integration lines; web:
  `javascript-security`; PLAN-37 and PLAN-72: `lint-config`, `cui-javascript-project`) and a Standing Constraint states the
  rule; the shipped plans and the four running ones (14, 20, 22, 39) were left alone. No spec was blocked on PLAN-77.
> ↪ Relocated to `settled.md` § "Landings of PLAN-39, PLAN-22 and PLAN-14 (2026-10-30)" — all three plans are shipped; the open points they raised are in Watches

- 2026-10-30 — UI specification changes analysed and incorporated (operator request "analyze the last PRs"). Sources:
  `pm-mcp-clients` PR #278 (`07104678`, documents only: design `plan-overview`, `plan-view`, `completion-report`,
  `decisions`, `decisions-texts`, `project-explorer`, console; three new work packages `overview-order-and-size`,
  `needs-and-find`, `web-moving-between-plans`) and `plan-marshall-documentation` PR #135 (ThinkRail reading: UC-PLN-7 and
  UC-PLN-8 `AGREED` as two views, the machine in `needs_operator` with `credential` and `cleanup_due`, running work in a
  stable order, a plan's size of change; requirements, runtime-model specification and roadmap Milestone 11 updated). Read
  against the staged specs: the client-side derivations of PLAN-46 (search index from the stores) and PLAN-49 (inbox derived
  from the stores) are replaced by the runtime's resources (`GET /api/v1/find`, `GET /api/v1/machine/needs-operator`), the
  start-page role of PLAN-49 is open until the accepted design, and PLAN-36 reads the served list. Staged: PLAN-81
  (`overview-order-and-size`, WS-01), PLAN-82 (`needs-and-find-contract`, WS-01: design first, then contract and mock),
  PLAN-83 (`web-moving-between-plans`, WS-03; ports 20830/20831), PLAN-84 (`cli-find`, WS-04). All four read a clean gate
  (declarative, determinate, no blocking candidate, no overlap prompt with the running PLAN-20). Rescoped, not deleted:
  PLAN-36, 46, 49 (data source, dependency on PLAN-82, UC agreement discharged). Notes only: PLAN-34, 51, 52, 54, 55, 57,
  59. Mapping choices that are the orchestrator's and open to the operator: the package `needs-and-find` is split in two
  plans (design and contract first, consumers after) and its terminal "find" is its own plan (PLAN-84) rather than a
  deliverable of PLAN-36; PLAN-83 keeps the three parts of its package together. The packages' own rows in
  `doc/plans/README.adoc` stay `open` and are set to `done` by the plans that complete them.
- 2026-10-30 — PLAN-77, 80, 82 and 86 recorded launched and then running on the operator's word ("all four plans are
  running"), after the emit below; four running is N=4 and no slot is free. The operator asked to persist, land everything
  and prepare a restart: the ledger was landed and a restart note heads the resume anchor. PLAN-22's follow-up PR #276 was
  merged and cleaned up on the operator's word in between (`44803a77`).
- 2026-10-30 — PLAN-20 landing analysed (PR #273, `a07a09b6`) and drained; row shipped. Nothing runs now: all four slots
  of N=4 are free and the refactor sprint the operator announced ("drain all current plans, then the refactor sprint") can
  start. Emitted at the operator's request ("analyze landed and emit"): PLAN-77 (`cui-standards-foundation`), PLAN-80
  (`web-standards-conformance`), PLAN-82 (`needs-and-find-contract`) and PLAN-86 (`shared-error-texts`). All four are
  `declarative`, determinate, with no blocking candidate and no overlap prompt (PLAN-77 and PLAN-80 share only
  `CLAUDE.md`, one file); `corpus verdicts` has `blocking_count: 0` and the four specs carry no stale verdict. PLAN-80 now
  adds a `## Web Standards` section of its own after § Code Standards, so the two `CLAUDE.md` edits cannot conflict. The
  choice rests on: the operator's order (the refactor sprint first), and PLAN-82 and PLAN-86 each unblock several staged
  plans (PLAN-82: 34, 36, 46, 49, 52, 54, 55, 83, 84; PLAN-86: 09, 30, 45). Not emitted, though ready: PLAN-17, 21, 23, 24,
  37, 85 (PLAN-17, 21, 23 overlap each other by 2 to 4 files, so they run one after the other). PLAN-78, 79 and 87 wait for
  PLAN-77. Re-grounded notes added to PLAN-21 (the PTY path of `decide`) and PLAN-36 (the `!` key is one rule).
- 2026-10-30 — Settled narrative relocated (operator confirmed "yes" to the proposal). 26 Decisions entries (the
  landing analyses, emits, launches and ledger lands of shipped plans, and the cancelled round 1) and 3 retired Watches
  moved verbatim into `settled.md` under six headings, with eight pointers left here; `epic.md` went from 781 to about
  600 lines. Checked: every removed line is in `settled.md`, the only added lines are the pointers, and `compact` reports
  every pointer resolving. Kept here on purpose: the standing rules of the set-up decisions, the Queue annotations zone
  (never relocated), the chronic Open Defect, the live Watches and the decisions that still drive open work (standards
  conformance, UI spec changes, structure review, theme request). The "Structure review applied" entry just below was
  written before this move and is not relocated.
- 2026-10-30 — Structure review applied (operator: "fix clear and weaker"; nothing landed yet). Staged: PLAN-86
  (`shared-error-texts`, WS-01), PLAN-87 (`cui-testing-mock-runtime`, WS-01), PLAN-88 (`web-designer-panes`, WS-03).
  Superseded (spec kept with a banner, deliverables moved): PLAN-81 into PLAN-82 and PLAN-34, 52, 54, 55; PLAN-38 into
  PLAN-44; PLAN-57 into PLAN-51; PLAN-68 and 69 into PLAN-66; verify plans PLAN-28 into PLAN-33, PLAN-40 and 56 into
  PLAN-63, PLAN-61 and 67 into PLAN-65. Rescoped: PLAN-09 and 45 (error texts to PLAN-86, loading texts to PLAN-45),
  PLAN-30 (renders PLAN-86's texts), PLAN-83 (work list only; "Add a plan" to PLAN-59 D4, notifications to PLAN-51 D5),
  PLAN-43 (one reference view; PLAN-51, 53, 55, 58 move their own views), PLAN-47 (system, devices, shared panel; plan,
  epic, project acts to PLAN-55, 58, 59), PLAN-55 and 62 (the process tree is PLAN-62's), PLAN-54 (reports only),
  PLAN-77 (pilot modules only), PLAN-74 (design documents follow the work: view density to PLAN-43, interaction
  affordances to PLAN-47, components to PLAN-35), PLAN-17, 26, 30 (one exit-code table), PLAN-70, 71, 18, 29 (dependencies
  and coverage of the new plans). The eight verify plans left are PLAN-18, 29, 33, 41, 63, 65, 71, 75. The gate reads
  88 specs, all determinate, no blocking candidate. One slip: the PLAN-85 component name `LanguageChoice` collided with the
  onboarding component of the same name; PLAN-85 uses `LocaleSwitch`, and PLAN-66's `language.choose` no longer names a
  header toggle.
- 2026-10-30 — Operator design request: the theme choice (light, dark, system) stands on every page today (rail foot on
  a desktop, More sheet on a phone, put there by PLAN-39 and stored by PLAN-04 in `localStorage` key `pm_theme`). The
  operator wants it on the pairing page at the start and changeable in Settings in a new view of the client. A cookie
  was first proposed; the operator then said local storage is fine, so the storage stays `localStorage` `pm_theme`
  (PLAN-04) and the cookie is dropped. Staged PLAN-85 (`web-theme-in-browser-settings`, WS-03, ports 20850/20851; gate
  clean, no plan dependency left): the control leaves the rail and the More sheet and stands on the pairing page and the
  revoked page (the pages without a way to Settings), and Settings gets the view "This browser" (`/settings/browser`,
  applied at once, no Save). Notes added to PLAN-60 (the tab "This browser" joins its tabs; the one-save rules do not
  apply to it), PLAN-70 (the contrast audit's storage stays) and PLAN-16 (keeps the switch behaviour). The operator then
  asked why one setting type is spread over several plans: the language choice, first left in the rail, moves with the
  theme (same two places, `pm_locale` stays in local storage), so PLAN-85 owns every browser-only setting and its view
  is their one home; PLAN-16 keeps only how the switch refreshes the texts. The rail's collapse button stays on the rail
  (a layout toggle, not a preference). Orchestrator choices open to the operator: the view is called "This browser".

## Open Defects

- 2026-10-27 — Incomplete landing facts: `rb-02-cli-pty-dialogue-harness-001.md` (PLAN-02) reports
  `missing_keys` `total_tokens` and `plugin_version` (both `unknown` on opencode). It was reconciled as far
  as its facts go. The runbook lane on opencode cannot measure tokens or read the plugin version, so every
  opencode landing will repeat this until the lane can. Recurrence 2026-10-27:
  `rb-04-web-theme-and-titles-001.md` (PLAN-04), same two keys. Recurrence 2026-10-27:
  `rb-05-cli-summary-line-and-start-script-001.md` (PLAN-05), only `total_tokens` missing; it is the first
  opencode landing that carries `plugin_version` (`0.1.2017`, read at landing time). Recurrence 2026-10-27:
  `rb-01-web-text-guard-report-001.md` (PLAN-01), only `total_tokens` missing. Recurrence 2026-10-27:
  `rb-76-shared-port-assignment-001.md` (PLAN-76, harness `antigravity`), `total_tokens`, `model` and
  `effort` missing. Recurrence 2026-10-27: `rb-07-cli-terminal-design-docs-001.md` (PLAN-07, antigravity),
  only `total_tokens` missing. Recurrence 2026-10-28: `rb-06-shared-display-vocabulary-001.md` (PLAN-06,
  opencode), `total_tokens` and `plugin_version` missing. Recurrence 2026-10-28:
  `rb-12-web-design-tokens-v2-001.md` (PLAN-12, opencode), same two keys. Recurrence 2026-10-28:
  `rb-03-mock-scenario-states-001.md` (PLAN-03, opencode), same two keys. Recurrence 2026-10-28:
  `rb-15-cli-footer-and-help-overlay-001.md` (PLAN-15, opencode) and `rb-19-web-ui-icon-set-001.md` (PLAN-19,
  opencode), same two keys each. Recurrence 2026-10-29: `rb-10-cli-choice-prompt-pause-001.md` (PLAN-10,
  opencode), same two keys. Recurrence 2026-10-29: `rb-25-web-core-components-controls-001.md` (PLAN-25,
  opencode), same two keys. Recurrence 2026-10-29: `rb-01-web-text-guard-report-002.md` (PLAN-01, second message
  of a resumed run, opencode), same two keys. Recurrence 2026-10-29: `rb-08-use-case-documents-001.md` (PLAN-08,
  opencode), same two keys. Recurrence 2026-10-29: `rb-27-web-brand-001.md` (PLAN-27, opencode), same two keys. Recurrence 2026-10-29:
  `rb-31-web-core-components-containers-001.md` (PLAN-31, opencode), same two keys. Recurrence 2026-10-30:
  `rb-11-mock-cli-fixes-001.md` (PLAN-11, opencode), same two keys. Recurrence 2026-10-30:
  `rb-13-cli-event-contract-001.md` (PLAN-13, opencode), same two keys. Recurrence 2026-10-30:
  `rb-39-web-app-shell-001.md` (PLAN-39, opencode), same two keys. Recurrence 2026-10-30:
  `rb-22-cli-state-dialogue-onboarding-001.md` (PLAN-22, opencode), same two keys. Recurrence 2026-10-30:
  `rb-14-cli-gate-scope-001.md` (PLAN-14, opencode), same two keys. Recurrence 2026-10-30:
  `rb-20-cli-actions-by-state-001.md` (PLAN-20, opencode, two models), same two keys.

## Watches

- Structure review of the staged plans, applied 2026-10-30 at the operator's word ("fix clear and weaker"); see the
  Decisions entry of that date. Left open: PLAN-84 (`find`) stays in WS-04 although it belongs with PLAN-36 in WS-05 (the
  queue verb cannot change a row's workstream; the workstream documents say so); the review finding "PLAN-59 bundles
  projects with project onboarding" was withdrawn after reading PLAN-64: the project onboarding view and the machine
  onboarding screens are different views and documents. — re-check the split at each emit of the plans it touched.
> ↪ Relocated to `settled.md` § "Retired Watches (2026-10-28 to 2026-10-30)" — the watch is retired, its subject is closed (PR #276 merged as `44803a77`)
- Standards left to the operator (PLAN-77/80 analysis, 2026-10-30): `stylelint` (the `css` and `lint-config` skills expect
  it; 3 CSS files and component styles), `dependency-cruiser` (the `arch-gate-js` fitness check), `org.jspecify:jspecify`
  in `pm-api`, `pm-exec`, `pm-relay` (needed for `@NullMarked` there), `cui-test-value-objects` (only for types with
  hand-written equality; none found) and a sanitizer (no production code needs one). Each is a new dependency or plugin.
  — decide when a plan needs one; none is added by PLAN-77 to PLAN-80. `stylelint` is approved for PLAN-72 (its spec
  says so): the plan that adds it also applies the `css` and `lint-config` expectations of PLAN-80 to it.
- New specs and re-grounded specs must name the CUI skills and carry the standards Standing Constraint: Java deliverables
  `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`, `pm-dev-java-cui:cui-testing`, `pm-dev-java-cui:cui-logging`;
  web deliverables `pm-dev-frontend:javascript-security`. Specs not adapted: PLAN-24, 74 (documents only), the
  plan-marshall-lane verify plans (no Skills lines), PLAN-14, 20, 22, 39 (running) and the shipped ones.
- The CUI test standards are applied to inputs; the expected value of a contract (catalogue text, wire name, golden
  capture) stays literal. That is the orchestrator's reading of `cui-testing` ("no manual test data"), not the skill's
  text, and PLAN-78 and PLAN-79 report any class where it does not hold. — the operator may overrule it at emit.

- Operator decisions still open: the development switch `PM_OPERATOR_DEV_ALLOW_AGENT` (CLI-CC-P-05,
  security; PLAN-14 does not build it); the config contract (PLAN-32); the proof of presence (PLAN-24
  writes the record for the decision). — PLAN-14 landed 2026-10-30 and its PR body states the switch as an open
  decision for the operator; nothing builds it. Re-check when PLAN-24 lands.
- Web event handling should read the `pm-api` event table of PLAN-13 (CLI-UC-S-05 step 4). PLAN-13 landed
  2026-10-30 (`EventKind` in `pm-api`) and its PR names the six web files that still match event names themselves
  (`stores/runtime.ts`, `views/epicEvents.ts`, `OverviewView.svelte`, `DecisionView.svelte`, `ReportsView.svelte`,
  `api/client.test.ts`). No web plan owns the move; PLAN-09, 43, 45, 51, 52, 54, 57 and 58 name some of those files.
  — decide at the next web view plan to be emitted: fold it into one (PLAN-09 first) or stage a web plan. PLAN-39
  shipped 2026-10-30 and changed `stores/runtime.ts` and `api/client.ts` (stream state) without reading the table.
- `landing.failed` is accepted as an event name in the PLAN-13 tests but no `EventName` member carries it; the runtime
  sends `hold.opened` with kind `landing_failed` (PLAN-13 residue). — check where a later spec names it.
- The mock limits (`doc/claude_review/README.adoc#limits`) can make view checks `not verified` until
  PLAN-11 lands (PLAN-03 landed 2026-10-28: goto now applies a state's facts for the three scenarios, with
  four `system.*` lifecycle states still showing no view change). — re-check at the first verification
  landing.
- Runbook lane on opencode: `executor-skew` still answers `could_not_check` (the lane text version cannot
  be derived from the flat install path), and the guard is `none` (unenforced). PLAN-02 proceeded by
  operator choice. — re-check at every opencode landing; retire when a landing reports `match` and a
  guard. Recurrence: PLAN-04 (same `could_not_check`, guard `none`). Recurrence: PLAN-03 (executor `0.1.2021`, same
  `could_not_check`, guard `none`; the operator was asked in two sessions and proceeded both times).
  Recurrence: PLAN-15 and PLAN-19 (executor `0.1.2021`, `could_not_check`, guard `none`; the operator proceeded
  both times). Recurrence 2026-10-29: PLAN-10 (`could_not_check`, answered "Proceed" three times, guard `none`).
  Change 2026-10-29: PLAN-25 is the first run where `executor-skew` answered `skew` (0.1.2021 against 0.1.2028)
  and `match` after the operator regenerated the executor; the registry pin, the executor and the cache now all
  read `0.1.2028` (`cleanup restart-check`, `registry_parity: ready`). The guard is still `none`, so the watch
  stays until a landing reports a guard.
- Build server: the bare-`mvn` failure (PLAN-02, 04, 05, 01) is gone: PLAN-07's and every one of PLAN-06's
  routed builds ran on the daemon. What remains is the learned timeout bound. It expired below a full
  `verify -Ppre-commit` in both runs (876 s during `npm ci` for PLAN-07, 752 s for PLAN-06, against runs of
  663–793 s), and the lanes then passed `--timeout 1800`/`2400` by hand. But PLAN-12, running in the same
  hours, fell back to in-process for every build (`maven_not_on_daemon_path`, bare `mvn`), so the fix is
  not reliable. — re-check at the next routed landing, compare the daemon's PATH between runs, and raise
  both the PATH and the timeout bound with the build server. PLAN-03 (2026-10-28): every routed build ran on
  the daemon with no fallback (the PATH part held), but one routed build hit its explicit 920 s bound and was
  re-run at 1800 s, so the timeout bound is still open. PLAN-15 and PLAN-19 (2026-10-28): the bare-`mvn`
  failure is back. The first routed build of each failed (`execution_failed`; the daemon ran a bare `mvn` that
  is not on its PATH), and every later build ran `in_process`. So the PATH fix
  holds for some daemon runs and not others; compare the daemon's start (PLAN-03's runs, which held, against
  these two). Recurrence 2026-10-30: PLAN-22 and PLAN-14 (every routed build answered `execution_failed` / exit `-1`
  in 0 s, so every build ran `in_process`; the daemon bound also expired once in each: 555 s and 445 s). PLAN-39 (same day) reported
  no fallback, so it recurs on some daemon runs only: raise it with the build server.
- Playwright MCP browser shared between parallel sessions (PLAN-12): another session took over its tabs,
  and a tab left open on the app port broke one e2e run. The port blocks separate the mock and the app,
  not the browser. — re-check at the next landings of browser-checking plans running side by side. If it
  recurs, give each opencode session its own Playwright MCP profile or output directory, or run only one
  browser-checking plan at a time. PLAN-03 (2026-10-28) ran its 48-pass browser matrix with an isolated
  Playwright MCP profile and reports no interference; one profile per session looks sufficient.
- The Sonar credential cannot transition an issue (HTTP 400, PLAN-12). The Sonar clause of every spec
  allows a dismissal "transmitted through the plan-less transition verb", which can't be transmitted
  today, so any finding that is dismissed rather than fixed blocks its merge. — fix the token's
  permission; re-check at the first dismissal. Recurrence 2026-10-28: PLAN-03 (`falsepositive`, `accept` and
  `wontfix` all HTTP 400). Its finding stayed stale on PR #250 through seven rewrites of the construct, and
  was only cleared by closing the PR and re-opening the same commits as #253 for a fresh analysis. Two more
  facts: `sonar_rest search` carries no status field and returns resolved issues as line-0 rows, so it cannot
  tell a closed issue from an open one; the Sonar MCP search and `gate-status` can.
- PLAN-10's run (antigravity, conversation `e291b36e…`) dirtied the main checkout on 2026-10-27 at 22:26.
  From `/Users/oliver/git/pm-mcp-clients`, not its worktree, it ran Playwright directly
  (`npx playwright --version`, then
  `npx @playwright/test test -c .plan/temp/playwright.config.js` with an ad-hoc config it wrote there).
  That left the untracked `test-results/` (`.last-run.json`: `failed`, no tests). Its browser check
  should run through the Playwright MCP server, not a hand-run Playwright from the main checkout. — check
  PLAN-10's landing for how its browser regression was evidenced; the operator removes `test-results/`.
  Recurrence 2026-10-28: the runs of PLAN-15 and PLAN-19 each stopped to ask the operator about an untracked
  `test-results/.last-run.json` in the main checkout (PLAN-15: moved to `.plan/temp/`; PLAN-19: the operator
  answered "fixed"). The file keeps reappearing in the main checkout; which run writes it is not established.
  PLAN-10's landing (2026-10-29) is silent on it and shows its browser snapshots taken through the Playwright MCP
  server (after the operator repaired the output directory), so the PLAN-10 part of this watch is closed.
- `/plans/{id}` scrolls horizontally at 390 px (498 px measured; PLAN-12 residue, older than the epic).
  — check that PLAN-55 (`web-plan-page`) covers it before emitting it.
- Tab titles name the identifier (`drop-legacy-api`), not the display title (PLAN-04 residue). No
  staged plan owns a title store for the shell. — decide when PLAN-09 (`web-display-layer`) is emitted,
  or at the PLAN-29 verification (`CC-V-04`).
- The e2e suite on `main` has a failing case before any epic change: the `CleanupView` contrast check in
  light at narrow width (seen in PLAN-04's base run at `65973291`). PLAN-01 saw the same case for
  `SystemView`, failing on `origin/main` and green on re-run, so it may be flaky rather than failing.
  PLAN-76 changed `CleanupView`'s success colour to `--color-success-text` (not stated in its landing);
  that may fix the `CleanupView` case. — re-check at PLAN-09 (changes `CleanupView`) and PLAN-70
  (`web-light-theme-pass`).
- PLAN-76's deliverable 1 browser check is not evidenced. It asked for the system view showing
  `127.0.0.1:20760` while a second mock runs on `7420`; no Playwright file and no progress line exist.
  The behaviour is covered by `MockRuntimeTest` and the concurrency run. — check it in PLAN-18
  (`verify-shared-foundation`).
- First landing from the `antigravity` harness (PLAN-76): `model` and `effort` are `unknown`. Its message
  was thinner than the lane asks: no operator questions, no build-server section, no rule-break account.
  Its skew question named executor `0.1.0`, against `0.1.2017` in the facts. — re-check at the next
  antigravity landing. PLAN-07 (antigravity) reported its model and a full message, but
  `plugin_version=0.1.0` again.
- Sonar gate on documentation-only plans — decided 2026-10-28: the organisation's reusable build
  (`reusable-maven-build.yml`, default `skip-on-docs-only: true`) skips `build`, `quarkus-alignment` and
  `sonar-build` together when every changed file matches its docs-only ignore list (`**/*.adoc`, `**/*.md`,
  `doc/**`, `.plan/**`, …), so the Sonar read is `undecidable`. That is the expected outcome, not a defect:
  there is no code to analyse. PLAN-07 merged on operator authorization before this was decided. PLAN-08,
  PLAN-24 and PLAN-74 declare only `.adoc` files under `doc/`, and their specs now carry the exception
  (`undecidable (docs-only skip)` does not block when all changed files match the list and
  `build / conclusion` passed). Running the analysis in CI was rejected: it needs
  `maven-build.skip-on-docs-only: false` and would build on two JDKs for diffs with no code. — re-check at
  the landing of PLAN-08, PLAN-24 and PLAN-74 that the changed files stayed inside the list; retire then.
  PLAN-08 checked 2026-10-29: two `.adoc` files under `doc/`, `build / conclusion` passed, the gate read is
  HTTP 404 (no analysis), landing reports `unknown`; the exception held. Stays open for PLAN-24 and PLAN-74.
> ↪ Relocated to `settled.md` § "Retired Watches (2026-10-28 to 2026-10-30)" — the watch is retired, its subject is closed
- Brand residue (PLAN-27, 2026-10-29): three CodeRabbit nitpicks on `favicon.test.ts` were answered, not fixed
  (stroke half-width in the dot bounds, an exact palette set, the ring on the dot circle), and `<html lang>` stays
  `en` on the German pairing page (`src/app.html` hard-codes it, nothing writes `document.documentElement.lang`).
  No staged plan owns either. — the test tightening is low value; the `lang` attribute is an accessibility defect
  for the German pages: PLAN-39 shipped (2026-10-30) without it and `src/app.html` still reads `<html lang="en">`.
  — decide which plan takes it (a view plan, or stage a one-line fix).
- Container components without callers (PLAN-31, 2026-10-29): `Card`, `KeyValueList`, `Tabs`, `Breadcrumb`, `Dialog`,
  `Toast` and `AttentionStrip` are drawn on `/dev/components` only, the nine `*Modal.svelte` components are not
  migrated, and `Toast` has no host or store, so whichever plan raises the first toast must own it. — PLAN-39 (shipped
  2026-10-30) gave `Dialog` its first caller, the phone's More sheet; the rest is unchanged. Check at PLAN-43 and the
  view plans that name a modal; stage a host if none does.
- A run edited the main checkout (PLAN-31, 2026-10-29): `common.{de,en}.properties` were first changed there,
  reverted by hand and redone in the worktree; the main checkout was clean afterwards. The merged diff is right. —
  second report of a lane writing outside its worktree (PLAN-10's `test-results/` was the first, different cause);
  re-check at the next landing.
- The tab icon, the header badge and the tab title all read `runtimeStore.openDecisionsCount` (PLAN-27). — PLAN-49
  switches all three to the attention total; check at its emit (note added to its spec).
- First sibling-repository change of this epic (PLAN-08): the run opened and merged a pull request in
  `plan-marshall-documentation` with `gh`, because the lane's `ci` surface cannot target another checkout. The
  operator authorized both steps. — expect it again whenever a spec's deliverable writes to the sibling; the lane
  has no way to do it without a rule bend.
- `EpicProcessPlan` documents `phase_state` as `running/staged/planned/done` (`EpicProcessPlan.java:26`),
  but the scenarios serve `staged`/`ended` and `PlanActivity.parse("done")` throws (PLAN-06 residue).
  — fix in PLAN-34 (`cli-display-layer`), the next plan on the `pm-api` display contract; check before
  emitting it.
- Four lifecycle states are invisible in `pm-web-app` (PLAN-03 residue): `system.running`, `system.stopped`,
  `system.restarting` and `system.force-stop` change the API resource but no rendered view, because the app
  shows the lifecycle only as the draining banner. PLAN-11, PLAN-17 and PLAN-26 name `system.*` states but are
  mock and CLI plans, so no staged web plan owns rendering them. PLAN-39 shipped (2026-10-30) with a neutral machine
  pill and did not take them. — decide when PLAN-09 is emitted: fold the rendering into it or stage a web plan; at the
  latest at PLAN-41.
- PLAN-03 changed fixture values that other staged specs assert or declare: 18 files merged against 9 declared.
  PLAN-11 repeated it as predicted: 21 files against 7 declared entries, three of them untouched (the e2e specs
  stayed unchanged). — before emitting PLAN-47, PLAN-50, PLAN-52, PLAN-55, PLAN-58 or PLAN-62: re-read the spec
  against HEAD (PLAN-11 is struck from the list, shipped 2026-10-30).
> ↪ Relocated to `settled.md` § "Retired Watches (2026-10-28 to 2026-10-30)" — the watch is retired, its subject is closed
- PLAN-03 residue outside its boundary: `doc/design/terminal-client/26-cleanup.adoc` says 1.8 GB where the
  fixture now says 1.9, and `pm-web-app/src/lib/views/MachineSettingsView.scenarios.test.ts` still passes
  `36 roles` as an input. — fix the document in PLAN-23 (`cli-cleanup-dialogue`, which names it) and the test in PLAN-09 or
  PLAN-60, the staged specs that name `MachineSettingsView`; check there.
> ↪ Relocated to `settled.md` § "Retired Watches (2026-10-28 to 2026-10-30)" — the watch is retired, its subject is closed
- Mock residue named by the PLAN-11 run (2026-10-30), accepted and not fixed: the drain-reconcile race after the event
  stream connects (needs a connection-established signal the SSE client does not expose); a drain started through
  `POST /api/v1/operator/runtime/stop` leaves the status without `drain_remaining_count`; a paused plan's synthesized
  current node is `waiting_for_operator` because `NodeState` has no paused value; the draining banner's `what` stays
  generic; the synthesized plan's activity body is the template's for a listed plan without its own fixture. — check
  where a spec reads the mock plan detail or the drain status: PLAN-20, 47, 52, 55. PLAN-39 (2026-10-30) fixed one more
  case at the operator's word: a plan listed only inside an epic's rows now resolves (`planListedByAnEpic`), takes its
  state from the row (`shipped`/`merged`/`landed` read as `done`) and names its epic.
- Mock epic-action routes (PLAN-10 residue, 2026-10-29): `/api/v1/epics/{id}/actions/cancel` ignores `with_plans`
  and `/api/v1/epics/{id}/actions/next` is a no-op. The run fixed the other false successes (`common_files`,
  `with_plans`, `archive`, `next` as CodeRabbit found them) and left these two untouched. No staged plan owns
  them. — decide with PLAN-11 (`mock-cli-fixes`) at its emit whether to add them to its deliverable 2.
- Per-point pause consequence (PLAN-10 residue, 2026-10-29): the cancel/pause dialogue cannot show what each
  pause point costs, because the actions resource carries no per-point data; the spec clause was accepted as
  not met. — an operator decision: add the data to the daemon contract (another repository) or drop the clause
  from the design. Check whether PLAN-20 (`cli-actions-by-state`) repeats the clause.
- Browser regression clause on terminal-only plans (PLAN-10 residue): PLAN-10 D2 asked for web views "that render
  the changed catalogue areas (control)" but changed only `operator.*` texts, so the clause was not met. The
  same generated clause is in PLAN-05 (shipped), 17, 21, 22, 23, 26, 34 and 36. — covered by the terminal-check
  watch below: before emitting each, keep the clause only when the plan changes a catalogue the web renders.
- PLAN-04 was run a second time after it shipped (2026-10-27 about 12:00, opencode). The run found the spec
  already merged (`099a3b40`) and stopped with no commit and no PR. It left the worktree
  `.plan/local/worktrees/04-web-theme-and-titles` and the branch `feature/04-web-theme-and-titles`,
  locally and on origin, both at `394c627b`. The runbook lane does not check the queue row before setup:
  a `shipped` row should stop a run before it creates a worktree. The leftovers are removed (the run
  removed its worktree and local branch; the operator deleted the remote branch and the temp files on
  2026-10-27). Its landing message `rb-04-web-theme-and-titles-002.md` (`pr=n/a`, `deliverables_done=0`)
  was absorbed here; the row stays `shipped` on PR #231. — raise the lane check with the lane fix.
- Terminal-check targets per deliverable: the generator gave every output-changing terminal deliverable
  the plan's one terminal target, so a deliverable whose change no screen can show gets a clause it
  cannot meet (PLAN-05 D2.2, a `start.sh` change checked on `plans`, `q`). — before emitting each
  staged terminal spec, check every "Terminal check" clause against its deliverable. Correct it to the
  deliverable's own observable, or drop it for a non-rendering change and say so.
- Shared message catalogues with the plan in flight (PLAN-15, PLAN-19): PLAN-15 changed
  `operator.{de,en}.properties` and PLAN-19 changed `control.{de,en}.properties` in `pm-api`, neither declared.
  PLAN-10 declared all four files, and PLAN-13, 14, 17, 21, 23, 30, 36 and 42 declare the operator
  pair. PLAN-10 shipped (2026-10-29): it changed only `operator.{de,en}.properties`, merged `main` twice and
  reported no catalogue conflict. The part about PLAN-10 is retired; what stays: PLAN-10, 15 and 19 all changed
  the catalogue, so re-read PLAN-13 and 14 (next on the operator pair) and the other six at HEAD before emitting.
  PLAN-13 shipped (2026-10-30) and changed the pair too (`operator.console.*`) plus `PmConsole`, `EntryFlow`,
  `OverviewFullScreenApp`, `PtySession`, `ScreenCaptureIT`: re-read PLAN-14, 17, 21, 23, 30, 36, 42 and, for the
  terminal files, PLAN-34, 38, 44, 50 at HEAD before emitting. PLAN-22 (running) declares `EntryFlow`.
- CodeRabbit refuses a review of the final head (PLAN-10 too, 2026-10-29: refused twice, newest reviewed head `1c77501`
  against merged head `52275941`, after three qualifying reviews; the lane merged under the standing authorization;
  the quota refusal was recorded; PLAN-25, 2026-10-29: the re-trigger after the last push returned no review at all,
  the check stayed `NEUTRAL`, an earlier qualifying review on `1d494f9` stood): PLAN-03, PLAN-15 and PLAN-19 each merged a head that no
  CodeRabbit review covers (re-triggers refused as "Review rate limited": PLAN-15 at 17:25 and 18:03, PLAN-19 at
  17:44 on 2026-10-28, the next slot given as 19:14Z); the lanes merged under the standing review authorization after an earlier qualifying review and the
  operator's word. The quota is shared by every plan running side by side. — re-check at each landing; if a
  landing has no qualifying review at all, that is a defect rather than this watch.
- Transient provider failures in the run of PLAN-15 and PLAN-19, both cleared by a retry: the CI calls answered
  `Not authenticated` (the operator answered "retry" in both runs), and PLAN-15's first `build / sonar-build`
  failed on Sonar ("Organization is not allowed to access data from non main branches") and passed on re-run
  of the same head. — re-check at the next landing; if the authentication failure recurs, find why the CLI
  session lapses.
- PLAN-15's 80-column footer clause was written wider than the design: `? more` appears only where a cell had
  to be left out, so the overview and the activity (whole footer fits) do not end with it. — PLAN-56
  (`verify-cli-interaction`, `CLI-UX-V-02`) takes the design, not the clause, as its criterion; check the clause
  before emitting it.
