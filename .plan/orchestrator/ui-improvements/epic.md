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
- PLAN-09 (`web-display-layer`) — shipped (PR #287, merge `5f9e60da`). All four deliverables met. It changed 51 files against
  26 declared entries: 22 changed, four untouched (`ReportsView`, `StartView`, `overview.{en,de}.properties`) and 29
  undeclared (seven more catalogue areas, the other views and components, `i18n/index.ts`, four `pm-operator` files, the mock
  scenarios and engine, three e2e files, four design tables). `lib/display` has `displayState`, `displayError` and
  `displayUnit` (17 tests); the enum guard fails on a raw server value in any `.svelte` file; nine views read their values
  through it; the text-guard baseline lost every raw-enum entry. Sonar gate `OK`, 0 open issues (coverage 100);
  CodeRabbit reviewed an earlier head and the merge ran under the standing authorization after the operator said "Merge
  now". It reached into the terminal client (`SystemLevel`, `StartCommand`, `StartDialogue`), which the running PLAN-17
  declares.
- PLAN-86 (`shared-error-texts`) — shipped (PR #283, merge `1b6c6ae3`). Its one deliverable was met. It changed 6 files
  against 6 declared entries: five changed or created (`error.{en,de}.properties`, `ErrorTexts`, `ErrorTextsTest`,
  `texts-and-languages.adoc`), `ApiErrorCode` untouched, and `MessageCatalogue` undeclared. The area `error` has title,
  cause and fix for the 42 members of `ApiErrorCode` plus the `unknown` fallback; the spec's "43 codes" was my miscount of
  the members. Sonar gate `OK`, 0 open issues (coverage 100); CodeRabbit reviewed an earlier head and the merge ran under
  the standing authorization. Self-review budget exhausted with one item open (R-02, the German glossary). Every routed
  build failed and ran in process.
- PLAN-77 (`cui-standards-foundation`) — shipped (PR #282, merge `e83a9ee2`). All four deliverables met. It changed 33
  files against 23 declared entries: 22 changed, one untouched (`MockRuntimeExtensionTest`; the run wrote
  `MockRuntimeExtensionLoggingTest` beside it) and three undeclared (the root `pom.xml`, `pm-mock-runtime/README.adoc`, the
  new test). The four modules declare the CUI test artifacts, `CLAUDE.md` states the rule, `pm-mock-runtime` logs through
  its one `MockRuntimeLogMessages` with a `LogAsserts` test, all 17 `package-info.java` of `pm-operator` and
  `pm-mock-runtime` are `@NullMarked`, and `pm-relay` and `pm-exec` tests draw their input from `Generators` (10 of 11
  classes; `LandlockTest` is input-free). The operator decided two points in the run: the root enforcer's allow-list now
  admits the test stack, and the close-failure record is asserted through a package-private log seam. Sonar gate `OK`, 0
  open issues; CodeRabbit reviewed an earlier head and the merge ran under the standing authorization after the operator
  said "Merge now". Every build ran on the daemon; no bare-`mvn` failure.
- PLAN-80 (`web-standards-conformance`) — shipped (PR #281, merge `bf8ddb31`). All three deliverables met. It changed 11
  files against 10 declared entries: eight changed (`marshal.json`, `CLAUDE.md`, `pm-web-e2e/package.json`, the two unit
  tests, `OverviewView.svelte`, `textGuard.js`, `contrast.js`), two untouched (`pm-web-app/package.json`, `Chip.test.ts`)
  and three undeclared specs (`chrome`, `overview`, `theme-readability`). The `javascript` domain now matches `.ts` and
  `.svelte`, the additive domain `javascript-cui` carries `pm-dev-frontend-cui`, `CLAUDE.md` has a Web Standards section of
  its own, `pm-web-e2e` has `quality` and `test:ci-strict`, and the timer waits are gone. The `any` hypothesis was refuted:
  the 28 casts were in `OverviewView.svelte`, not `Chip.test.ts`, and are typed now. Sonar gate `OK`, 0 open issues;
  CodeRabbit reviewed an earlier head and the merge ran under the standing authorization. Node versions agree (`v22.23.1`).
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
- Merge rule for a pull request with CodeRabbit (the operator's word, 2026-11-02): at least ONE CodeRabbit review of some head must exist and its findings
  must be handled; heads pushed after it need no review of their own. The lane re-triggers once and merges without waiting for a further review. A landing
  that fits this is conforming, not an operator decision; every CodeRabbit review that exists is handled, a late one included, and a finding in a review body counts as much as a thread (a lane that checks threads only misses it); one with no CodeRabbit review at all (a `skip-bot-review` label aside) or with unhandled findings of
  the review that exists is reported as outside the rule.
  Unresolved-thread count (the operator's word, 2026-11-05, "Yes"): before the merge the lane lists the review threads of the pull request at the head it merges
  (GraphQL `repository.pullRequest.reviewThreads { isResolved path line }`) and states the number of unresolved threads in its landing message, with the
  path and line of each one that stays open and the reason; a count of zero is stated as zero. A landing message without the count is incomplete; the
  orchestrator reads the thread list itself first, and a stated count that differs from it is reported as a false claim. The count does not replace the
  review bodies: findings in a body are listed too.
- ZERO Sonar findings on the new code (the operator's word, 2026-11-05): a plan lands with 0 open SonarCloud issues (`OPEN` and `CONFIRMED`, new code) at the merge head; a green quality gate does not satisfy it (PLAN-93 merged with the gate green and 3 new issues: `java:S1192`, `java:S6213` twice). The lane fixes each issue in the pull request that merges (never on a side branch), does not dismiss one on its own, and states `sonar_open_new_issues=N` in the landing message; an undecidable Sonar read is a stop ("Wait and resume later"), not a merge on recorded evidence. The orchestrator reads the count itself at every landing (the SonarCloud bot comment, or the issues API) and reports a landing with N>0 as outside the rule, routed into a follow-up pull request. Mailbox message delivered to the running lanes rb-98, rb-99, rb-100 (rb-95 with its own residue).
- Emit form by plan size (the operator's word, 2026-11-06: "Small plans are always lanes, normal plans always plan-marshall until noted otherwise"):
  a SMALL plan is emitted as `/plan-marshall-runbook-lane plan=…`, a NORMAL plan as `/plan-marshall task="implement …"`. The orchestrator judges the size
  from the spec (deliverables, Expected Surface, fix or verify plan), names its judgment with the emit and prints only that form; the operator may
  overrule per plan. The queue row `lane` is set to the form the operator started, when the operator says "#NN is running".
- Landing analysis in three layers (the operator's rule, 2026-11-01; it applies to every lane result, a follow-up included).
  The analysis reads and never builds: the lane's test runs, logs and screenshots stay its account. The landing report states a
  verdict per layer. (1) SPECIFIED: what the spec and the documents it binds require, clause by clause: each Done-when, the Standing
  Constraints (the design document is updated in the same pull request), the design text, and for a fix the findings it answers.
  (2) IMPLEMENTED: what the merge diff contains against (1), file by file: what is missing, what is added beyond the declaration, and
  what the PR body or the message claims that the diff does not do (a stale comment, a document that should have changed).
  (3) CORRECT: whether the change is right. Read the changed code for defects (logic, state, error and empty paths, the semantics of
  the markup, the keyboard and focus walk) and read each test for whether it would fail without the change and asserts the harm, not a
  proxy. A test whose name matches a finding is not evidence that the finding is fixed. A defect found by reading is recorded as
  'read, not run' with file and line, and gets an owner or an operator decision.
- Landing analysis runs in a sub-agent (the operator's rule, 2026-11-02: "For analyzing, always use a sub-agent"). The orchestrator briefs a
  read-only agent with the merge commit, the spec path and the three layers, and gives it the code to read, not its own conclusion; the lane's
  message is named as a claim to compare against after the agent has read the diff. The agent writes nothing. The orchestrator verifies every
  finding it records against git or the CI read side, and keeps all ledger writes, the merge-rule check and the decision what to emit.

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
- 2026-11-06 — The operator started PLAN-106 ("#106 started", the lane form printed for it; row `running`, plan id not yet known). Running: PLAN-18 (plan-marshall, PR #349), PLAN-97 (plan-marshall), PLAN-104 (lane), PLAN-106 (lane) = 4 of N=4: no free slot.
- 2026-11-06 — Landing of PLAN-105 (PR #348, `f5ab6dd2`, merged 09:54Z, 15 files; lane message 09:56Z) analysed by a read-only sub-agent (`landings/PLAN-105.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. FIRST landing to state both counts as facts lines (`unresolved_review_threads=0`, `review_body_findings_total=3/handled=3`, Sonar 0) and they are true (0 of 6 threads; 0 of 10 on PR 340 and PR 325; Sonar 0), but hollow: two of the three nitpicks of review `5797372610` were declined with FALSE reasons (V: `client.test.ts` has no bulk-read body test, the cited JS test mocks the client; the Java scenario test still asserts counts), the third deferred; CodeRabbit has the last word on 5 of the 6 threads of PR 348 and on 4 of the 6 PR 340 threads; "tracked elsewhere" was claimed twice and tracks nothing; PR 325's ten threads got no reply. Defects: after "Mark all shown read" the page keeps "Show N older" but its next page is an offset into the shorter unread list (reports unreachable until a reload; mock-verified, the real daemon's paging unreadable); the CLI `reports read --all` still posts `{"all":true}` (V: `ReportsCommand.java:210`) and the mock now answers 200 with 0 marked, against `24-reports.adoc:28`; an in-flight reload double-subtracts; a deep-linked report is under-counted; the e2e test for mark-all expects the old outcome. FIXED from PLAN-102: the by-ids rule, the click guard, the dead Escape guard, N1-N5 (N4 brittle), the h1 while loading, fixture names, `:focus` vs `:focus-visible`. ROUTED: PLAN-107 `web-reports-fourth-residue` (WS-03, 21070/21071, staged, NORMAL size: `plan-marshall` form, start AFTER PLAN-97 lands; default for the list after mark-all: reload the first page, the operator may overrule with a cursor); PLAN-74 deliverable 3 gains the RowMenu/bottom-navigation positioning; PLAN-45 deliverable 4 stays the reload mechanism. Row PLAN-105 `shipped`, message archived. Seventh lane in a row to report "all handled" with a real defect open.
- 2026-11-06 — Landing of PLAN-100 (PR #331, `1a36ed5e`, merged 08:34Z, 23 files; lane message 08:36Z, amended 08:37Z) analysed by a read-only sub-agent (`landings/PLAN-100.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. Counts hold: 0 of 10 threads unresolved, Sonar 0. BUT: a THIRD CodeRabbit review (`5793110125`, on `2ce7f9f1`, one outside-diff item: the hand-written `PLAN_ACTS_HANDLED`/`EPIC_ACTS_HANDLED` lists in `WorkControlPanel.svelte`) is unmentioned in the landing, unanswered and unhandled (V: the lists are still there); D3c (the unknown running count) is DEAD in the product (V: `EpicResource.running` is typed `number`, `resource?.running ?? null` is null only while `resource` is null, when Next plan is disabled) and its reply on thread `4484094188` is wrong; a failed epic-resource read leaves Next plan disabled with a false text and no retry; two view-level guard tests never reach the guard they name (`ConfirmDialog` refuses first) and say the opposite; four CodeRabbit fixes can be reverted with the suite green; an unknown withdraw is drawn as a definite failure; the PR body is stale. Retracted lane claim: the `AbortSignal.timeout` spy does not leak (global `restoreAllMocks`). ROUTED: PLAN-106 `web-acts-third-residue` (WS-03, 21060/21061, staged; SMALL: lane form); PLAN-45 new deliverable 5 (one client time limit, truncated-body contract, lost revoke response); PLAN-60 new deliverable 5 (device list loads, missing SettingsView tests); PLAN-58 new deliverable 4 (failed epic-resource read with retry). Row PLAN-100 `shipped`, message archived. Sixth lane in a row to report "all handled" with a review-body item open. The finding message of the running PLAN-18 (`plan-18-verify-shared-foundation-001.md`, CLI-CC-V-03: shared and client designs write ages in weeks and `N days ago` against the rule `9 d ago` of `texts-and-languages.adoc#time`; V: the orchestrator read the lines at `origin/main`) was routed and archived: PLAN-74 new deliverable 4 (shared designs: `work-actions`, `paired-devices`, `project-explorer`) and PLAN-34 new deliverable 4 (terminal-client designs 08, 15, 17, 18, 19; `19-actions.adoc` is also edited by the running PLAN-97). PLAN-18 itself is still running (PR #349 open).
- 2026-11-06 — The operator started PLAN-104 (as a lane, "being a small plan") and PLAN-97 (in the `plan-marshall` form printed for it): rows `running`, PLAN-97's row `lane` set to `plan-marshall`, plan ids not yet known. Running: PLAN-100 (PR #331), PLAN-105, PLAN-18, PLAN-97, PLAN-104 = 5 of N=4 by the operator's choice (the cap is the operator's to exceed; no further emit until a plan lands). New standing rule (the operator): small plans are emitted as lanes, normal plans as `plan-marshall` commands, until noted otherwise (see Standing rules above; memory `emit-form-small-lane-normal-plan-marshall`).
- 2026-11-06 — The operator started PLAN-105 (runbook-lane form) and PLAN-18 (`plan-marshall` form) ("started #105 and #18"; rows `running`, plan ids not yet known). Running: PLAN-100 (PR #331), PLAN-105, PLAN-18 = 3 of N=4: ONE free slot. PLAN-97 and PLAN-104 stay `staged`; the operator asked for their hand-off in the `plan-marshall` form (`/plan-marshall task="implement …"`), printed only; their queue rows keep lane `runbook` until the operator starts them (a plan run in the `plan-marshall` lane reports under its own plan id, not an `rb-` sender: the row's lane and plan id are set at the first message or landing).
- 2026-11-06 — Landing of PLAN-102 (PR #340, `95a2d855`, merged 02:20Z, 13 files; lane message 02:22Z) analysed by a read-only sub-agent (`landings/PLAN-102.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. Counts hold: 0 of 10 threads unresolved, Sonar 0 (bot comment). BUT: FIVE CodeRabbit nitpicks of review `5794258226` (body) are unhandled and unmentioned (V: `RowMenu.svelte` guard still there, `RowMenu.test.ts` still hard-codes the colours and the unscoped 44 px regex, the narrow test asserts `h1` only); six threads were resolved by the lane while CodeRabbit's last word was open (`:509` rollback, `:550` in-flight report, `:566` "button is guarded" which is false, `:875`, the fixture names); the Escape-masking hypothesis is refuted (`onMenuKeydown` stops propagation) and the guard is dead; unfiltered mark-all sends `{ all: true }` against `reports.adoc` 182/184 ("never everything") and the button label "Mark all shown read"; after `all` the load-older control stands beside "0 unread", a report arriving in flight vanishes, the failure rollback over-deletes, a reload then filtered mark-all counts a retained row twice, no click guard; no reply on any PR 325 thread. ROUTED: PLAN-105 `web-reports-third-residue` (WS-03, 21050/21051, staged, NOT emittable before the operator decides the mark-all rule: ids shown, as designed, or `all`); PLAN-45 new deliverable 4 (list pane keeps rows across a reload); PLAN-74 new deliverable 3 (design leftovers, shared filter rule). Row PLAN-102 `shipped`, message archived. The operator then decided the mark-all rule (option A, ids shown) and PLAN-105 became emittable; the same day PLAN-105 (runbook-lane form) and PLAN-18 (`plan-marshall` form: verify plan, all dependencies shipped, disjoint from PLAN-100/97/104/105) were emitted at the operator's word. PLAN-100 (PR #331) is the only plan still running: three free slots. Fifth lane in a row to report "all handled" with body items open; the landing message again carries no thread count.
- 2026-11-05 — Landings of PLAN-101 (PR #335, `6d0686c1`, merged 18:39Z, 27 files; lane message 20:32Z) and PLAN-103 (PR #341, `24523d67`, merged 22:02Z, 27 files; lane message 22:04Z) analysed by two read-only sub-agents (`landings/PLAN-101.md`, `landings/PLAN-103.md`); the thread counts, the Sonar counts and the central findings were read by the orchestrator first. PLAN-101: both counts hold (0 of 4 threads, Sonar 0; PR 314 and PR 319 have no open thread), all nine items of CodeRabbit's review handled and verified in the code; residue: the cleanup conflict falls back to `{}` when `current_value` is no record, the dialog's "Yours" line differs from what is resent, three chat guards without a test, two of the eight PR 319 replies false (`7c7ea71c` changes `DecisionView.test.ts` only). ROUTED, no new plan: PLAN-60 deliverable 4 and PLAN-51 deliverable 6 (both new); PLAN-60 now overlaps PLAN-100 on `SettingsView`. PLAN-103: counts hold (0 of 4, 0 of 14 on PR 321, 0 of 10 on PR 332; Sonar 0), the eight PR 321 threads really answered, BUT five CodeRabbit body items of two reviews (`5791531265`, `5792262598`) unanswered although the lane says "all findings fixed", Enter on a project badge reads a `project_id` that `BeingAddedProject` does not carry (V: the record; the test writes the field into its own fixture), `PROJECT_STATES` is a hand list (F4 reported as "derived dynamically"), the terminal evidence compares two scenarios, a passing log is cited as a failing run, `bounded` lost its error path in a Sonar fix, test-only surface grew in production. ROUTED: PLAN-104 `cli-console-third-residue` (WS-04, 21040/21041, staged); `EpicCommand`/`PlanCommand` catches into PLAN-97 deliverable 5. Rows PLAN-101 and PLAN-103 `shipped`, both messages archived. Fourth lane in a row to report "all handled" with body items open: the runbook-lane change (review-gate, required facts) is pending in plan-marshall. PR #342 (the PLAN-101 lane's pre-commit rewrite follow-up, whose body said "only whitespace" against three TODO markers) was stale after #341 and CLOSED by the operator's word. The landing messages of both lanes did not state the thread count or `sonar_open_new_issues` as lines of the facts block: read by the orchestrator, recorded above.
- 2026-11-05 — The operator started PLAN-102 and PLAN-103 ("#102 and #103 are running"; rows `running`, plan ids not yet known). Running: PLAN-100 (PR #331), PLAN-101 (PR #335), PLAN-102, PLAN-103 = 4 of N=4, no free slot. PLAN-97 waits for PLAN-103 (the `settings` golden).
- 2026-11-05 — Note from another session (relayed by the operator): the `deploy-snapshot` job of the main run for `9bf4b936` (PR #327 merge, run `42058015857`) failed with a 409 when publishing `pm-web-e2e` ("Could not transfer artifact de.planmarshall:pm-web-e2e:pom:0.1.0-2026…"). Read by the orchestrator: the failure is in the publish step, not in a build or test of PLAN-95; the later main runs (`fc8f0490` cancelled, `357eaae5` success) published again. Earlier red main runs of the day (`4dcf77b1`, `2508a47a`, `935ea29a`, `8f5d1385`) failed in `sonar-build`, the nullness gate that PRs #329/#334 of the other session fixed. Cause of the 409 (I): two pushes to main minutes apart publish the same SNAPSHOT timestamp; not verified. The orchestrator does not re-run CI of main; nothing owed by PLAN-95.
- 2026-11-05 — Landing of PLAN-95 (PR #327, `9bf4b936`, merged 15:44Z, 8 files; lane message 15:47Z) analysed by a read-only sub-agent (`landings/PLAN-95.md`). The landing message states both counts and both hold (V): 0 of 15 threads unresolved, Sonar 0 new issues; D7 (the PLAN-93 Sonar items) done in `d549d0b6` after the mailbox message. D1, D4, D5, D7 met; D2, D3, D6 mostly. Defects: a LOCK-ORDER INVERSION introduced (V on the code: `systemLock` then the engine monitor in stop/withdraw, the engine monitor then `systemLock` in `goToState`/`reset`/`setScenario`), a dead generation guard (`drain_generation` has no producer, V), `cancelPendingTasks()` cancels all scenario tasks, "ends" not delivered (the list empties, the state stays DRAINING), the kept/residue switch not atomic, the needs-page clause never answered. All 15 threads were answered with deferrals and no commit within two minutes; three were resolved while the bot's last word keeps the concern (`60EE-P` the lock order, `60EE9o`, `60EE-Z`); the body's follow-up and 3 nitpicks unanswered. ROUTED: PLAN-97 new deliverable 4 "The mock's drain, locks and switch" (PLAN-97 edits `MockRuntimeEngine.java` for the drain signal; no new plan). Row PLAN-95 `shipped`. PLAN-97 is now emittable (PLAN-95, 96, 98 landed) but conflicts with PLAN-103 on the `settings` golden: start it after PLAN-103 lands. First lane whose message counts were true and complete (it also read its mailbox).
- 2026-11-05 — Landing of PLAN-98 (PR #332, `6be3c48f`, merged 14:52Z, 33 files; lane message 14:54Z) analysed by a read-only sub-agent (`landings/PLAN-98.md`). Counts hold in form (V): 0 of 10 threads unresolved, Sonar 0 new issues. NOT conforming in substance: CodeRabbit review `5785729266` has TEN body findings (F1-F4, N1-N6) that no thread answers and the fix commit did not touch; thread `EntryFlow.java:712` was resolved while the bot said the concern remains. THE LANE'S "answered all 8 threads on PR 321" IS FALSE ON GITHUB (V: GraphQL, all 8 still unresolved with one comment each; the "answer" is a list in its PR body). Code: D1 and D2 met (injected clock, structured badges, bounded reads, test-first logs real), D3 partly (second step-count source, intro "Seven steps", `04-console.adoc` NOT in the diff and line 24 differs from the code, the cited anchor does not exist), D4 code present but `mutation-after` evidence mostly absent (log 093048 shows only the revoke tests), terminal check captures identical; `catch (Exception)` in production (EntryFlow :394/:582/:819). ROUTED: all of it into PLAN-103 `cli-console-second-residue` (WS-04, 21030/21031, staged). Row PLAN-98 `shipped`. Second lane after PLAN-93 to report work done that GitHub or the repository does not show; the runbook-lane change the operator passed on (review-gate, required facts) answers exactly this. PLAN-97 may now read PLAN-98's landing but still waits for PLAN-95 (`MockRuntimeEngine.java`) and conflicts with PLAN-103 on the `settings` golden.
- 2026-11-05 — The operator started PLAN-101 ("#101 is running"; row `running`, plan id not yet known). Running: PLAN-95 (PR #327), PLAN-98 (PR #332), PLAN-100 (PR #331), PLAN-101 = 4 of N=4, no free slot. PLAN-102 (staged) and PLAN-97 wait. The operator passed the proposed `runbook-lane` change (review-gate verb, required thread/Sonar facts, no lane dismissals, mailbox step) to the plan-marshall orchestrator; until it ships the orchestrator reads thread count, review bodies and the Sonar count itself at every landing.
- 2026-11-05 — Landing of PLAN-99 (PR #325, `bbf67794`, merged 12:36Z, 8 files; lane message 12:38Z) analysed by a read-only sub-agent (`landings/PLAN-99.md`). FIRST landing to state both counts, and both hold (V): 0 of 10 threads unresolved (GraphQL), Sonar 0 new issues (bot comment = head `50b4024b`). Not conforming in substance: CodeRabbit review `5782656169` has FIVE body findings (2 follow-ups, 3 nitpicks) that no thread answers and the code does not handle (V: `RowMenu.svelte` `choose()` still `closeMenu()`), four threads were resolved while the bot kept the substance open. Code: D1a, b, e, D2b-d, D3 formally, D4 met; HIGH defects: the `{#if}` duplicate `<TriagePage>` remounts the page on every reload (V), the heading move leaves the phone detail view with NO `h1` (V), `.row-menu__item:focus { outline: none }` removes the focus cue (V), no 44 px phone target in the menu; D2e not addressed, mutation logs "task-583"/"task-693" and the e2e run of D2a missing, the browser check has 13 snapshots and no screenshot. ROUTED: all of it into PLAN-102 `web-reports-second-residue` (WS-03, 21020/21021, staged, not emitted). Row PLAN-99 `shipped`. PLAN-101 (staged 2026-11-05) is EMITTED: disjoint from the running PLAN-95, 98, 100 by `corpus surfaces`; it waits for the operator's start.
- 2026-11-05 — Landing of PLAN-93 (PR #319, `4dcf77b1`, merged 09:57Z, 43 files; lane message 09:59Z) analysed by a read-only sub-agent (`landings/PLAN-93.md`); the first landing under the zero-Sonar rule. SONAR POLICY NOT MET: gate green but "3 New issues" (bot comment 09:44Z, V); the lane's fix sits on a side branch (`fix/93-sonar-residue`, `8f3ca376`) and does not close `java:S6213` (the rule is about the identifier `record`, `OnboardingScenarioTest.java:373`, `:398`). Threads: 0 unresolved of 13 (V: GraphQL), but two lane answers do not hold: no generation counter in `DecisionView.svelte` (the stale chat load still wins) and the PR 314 palette thread unhandled (mailbox 002 ignored). Code: D1, D2, D3, D5 (a), (d), (f)-(h) met; D4's Sonar clause not met; D5 (b), (c), (e), (i) partly (a test of (c) cannot fail, the reason of (e) cites the wrong requirement). The cleanup record is a wire change only the mock accepts (open for `pm-mcpd`, another repository). ROUTED: Sonar into PLAN-95 deliverable 7 (+ mailbox to rb-95); the rest into PLAN-101 `web-format-and-token-second-residue` (WS-03, 21010/21011, staged, not emitted: no free slot). Row PLAN-93 `shipped`. PR #323 (`fix/web-app-csp-boot`, another session, `vite.config.ts` and `BrandMark`) is not a plan of this epic: PLAN-27 and PLAN-76 rebase after it.
- 2026-11-05 — The operator started PLAN-100 ("#100 is running"; row `running`, plan id not yet known): running PLAN-93, 95, 98, 99, 100 = FIVE lanes, one above N=4, raised by the operator's start. PLAN-100 is disjoint from the others by `corpus surfaces`; its mock threads are answered after PLAN-95 D6 lands. PLAN-97 still waits for PLAN-98 and PLAN-95.
- 2026-11-05 — The operator started PLAN-98, PLAN-99 and PLAN-95 ("all three are running"; rows `running`; plan ids not yet known, set at their first lane message or landing); running: PLAN-93, 95, 98, 99 (four of N=4, no free slot). PLAN-97 waits for PLAN-98's landing (shared golden `console-settings.transcript`) and for PLAN-95 (`MockRuntimeEngine.java`); PLAN-100 waits for a slot and follows PLAN-95. RULE ADDED by the operator ("Yes" to the proposal): the landing message states the count of unresolved review threads at the merge head, read through GraphQL `reviewThreads`; recorded in the merge rule above and in memory. It is already in the specs of PLAN-93, 95, 98, 99 and 100 as the instruction to list the unresolved threads before the merge; the running lanes read the wording in their spec, no mailbox message is needed.
- 2026-11-05 — Landing of PLAN-96 (PR #320, `6cf1aac0`, merged 06:44Z, 33 files; lane message filed 06:46Z) analysed by a read-only sub-agent (`landings/PLAN-96.md`). All nine Done-when clauses have code and tests (D4a's scope only partly: only `decision` is filtered from the bar; order `mutation-after` for two D1 items); MERGE RULE NOT SATISFIED: CodeRabbit's second review (`5778430658` on `eea5204e`: four inline threads plus one outside-diff body finding) was never handled, four threads are unresolved (V: GraphQL: `MockRuntimeEngine.java:1330` and `:8383`, `SettingsView.svelte:159` and `:189`), one more finding sits in a resolved thread with no reply (`+layout.svelte:122-125`, `network_offline` not unknown) and one in the review body (`withdrawStop()` passes no signal); the lane's "all review threads resolved" is false (V). Verified at `6cf1aac0`: `scheduleDrainShrink` schedules `seconds <= 3` against a four-item default drain; `SettingsView.loadData` has no bound or sequence guard; `withdrawStop` has no signal. ROUTED: the two mock findings into PLAN-95 deliverable 6 (new; PLAN-95 waited for PLAN-96, which has now landed); everything else into PLAN-100 `web-acts-second-residue` (WS-03, 21000/21001, staged): the bounded and ordered reads, the guard tests that can fail (the busy guard of `ConfirmDialog` is unreachable through the disabled button), the work panel's small defects, the act-id scope and a reply to every thread. Row PLAN-96 `shipped`. Third lane in a row to merge with unresolved threads while reporting them resolved (PLAN-89 #314, PLAN-94, PLAN-96): the follow-up specs and the mailboxes say to list the threads through GraphQL before the merge. The operator's merge rule does not stop this by itself: a candidate rule for the operator: the lane lists `reviewThreads` at the head and states the count in the landing message; the orchestrator reads it first.
- 2026-11-05 — Landings of PLAN-54 (PR #313, `96286321`), PLAN-89's follow-up (PR #314, `bfbcbf1f`) and PLAN-94 (PR #321, `159c310c`); lane messages in the inbox, each analysed by a read-only sub-agent and its key claims re-read by the orchestrator (`landings/PLAN-54.md`, `PLAN-89.md` second section, `PLAN-94.md`). PLAN-54: all four Done-when clauses met in code (order of clause 1 `mutation-after`, not `test-first`), 0 unresolved CodeRabbit threads, merge rule conforms; open: `markAllShownRead` marks hidden rows read under a filter (V), no re-measure of the clamped body on resize (V), the row menu is built from raw buttons instead of `DataTable`, and tests that cannot fail. PLAN-89's #314: the three body findings of `2bbe1206` handled and strengthened; ONE CodeRabbit thread open (`focusable` selector lacks `iframe`/`object`/`summary`, V) although the lane says all handled; the regenerated golden `console-settings.transcript` is clock-dependent (`formatLastUsed` switches at 56 days; the other mock rows cross it around 2026-12-08 and 2026-12-11, by reading); the two `svelte-ignore` lines are a production change the PR body denies. PLAN-94: D1 to D5 mostly met, D1c/D1d/D2 tie/D5d/D5g only in part, and EIGHT CodeRabbit threads of review `5779338709` unresolved and unanswered (V: GraphQL), against the lane's "every thread resolved". Pattern, third time (PLAN-92, PLAN-89 #314, PLAN-94): the lane's claim that every finding is handled does not hold; every follow-up spec now says to list the unresolved threads (GraphQL `reviewThreads`) before merge. ROUTED: PLAN-98 `cli-console-badges-residue` (WS-04, 20980/20981: the golden's clock first, the badges bounded and read as data, one source for the step list, the partly-met clauses, the eight threads answered) and PLAN-99 `web-reports-residue` (WS-03, 20990/20991: the reports list defects, tests that can fail, the row-menu component) both staged and EMITTED (disjoint from the running PLAN-93 and PLAN-96 and from each other, `corpus surfaces` on 2026-11-05); the CodeRabbit thread of #314 into PLAN-93 D5 (i) (delivered to its mailbox). Rows: PLAN-54, 89, 94 `shipped`. Running: PLAN-93 (PR #319 open) and PLAN-96 (PR #320 open); PLAN-95 and PLAN-97 wait for PLAN-96 (`MockRuntimeEngine.java`).
- 2026-11-05 — The operator accepted all three proposals on the decisions embedded in plans: (1) PLAN-95 D3: `NeedsOperatorItem` gets NO item id (the specification's per-item list at `runtime-model/01-processes-and-transport.adoc:320` and the `pm-api` record have none); the design sentence `shared/needs-and-find.adoc:46` changes to selection on the item's `link`, with `kind` plus the project/epic/plan ids where the link is absent. (2) PLAN-95 D2: `broadcastReset()` is NOT called after `goToState` (it is for a scenario switch; a jump keeps its scheduled events replayable); a test pins it; the reason goes to the call site and the progress record. (3) PLAN-93 D5 (e): the catalogue-rollback retention is NOT built; answered with the reason (one catalogue per locale is the documented design; a missing entry is fetched from the runtime); the lane confirms the specification has no rule for it. PLAN-93 is running, so the decision (3) was delivered to its mailbox (`rb-93-web-format-and-token-residue`); the specs of PLAN-93 and PLAN-95 carry all three. Nothing embedded in a plan is open for the operator now except the PLAN-97 items decided earlier and the German glossary / MOCK-201 (earlier).
- 2026-11-04 — The operator agreed to the orchestrator's proposals for PLAN-97 deliverable 3 ("agreed"): `enrol` folds into `project add [<path>]` (`--pilot` moves to the design line), `settings` is removed (`config`, `config documents`), `git-source` folds into `plan add --pull-request <n>` (check at outline that it reaches the adopt lane), `lesson check` (UC-STA-8) is kept with a new design line, `plans`/`questions`/`answer`/`plan <id>` stay as direct shell verbs (the design's "In the console only" block changes), `client` stays and joins the design's "other operator verbs" line, `recipe run` needs nothing. Written into the spec of PLAN-97 deliverable 3 (b) and (d); the lane no longer asks. Not decided and outside PLAN-97: the design's `harness` naming against the code's `client` (open package `harness-names-in-code`). Still open for the operator: the `NeedsOperatorItem` item id and `broadcastReset` (PLAN-95), the catalogue-rollback retention (PLAN-93 D5).
- 2026-11-04 — The operator answered the open question of design-audit-002 ("make it a plan, or add to an existing"): the three packages `operator-drain-signal` (D25), `operator-command-tree` (D26) and `accept-edit-enrolment` (R7-7) are ONE new staged plan, PLAN-97 `cli-command-tree-drain-and-accept-edit` (WS-04, 20970/20971, three deliverables in that order: drain signal and `runtime stop --withdraw`, `accept-edit enrolment <project_id>` with the mock's `enrolment_tampered` refusal, the design's command tree with the other top-level verbs folded or removed). No existing plan fits (all `pm-operator` command-tree work; PLAN-42 and PLAN-79 wait for it, PLAN-30 moves its `OnboardCommand` strings after it). Grounded at `c7d9dcb8`: `DrainCommand` POSTs `/api/v1/runtime/stop` and carries `--withdraw`; `AcceptEditCommand` takes `<store-path> --project`; `PmOperator` registers 29 subcommands; the mock has `/stores/accept-edit` but no `enrolment_tampered` (the first draft of the spec said it did: corrected). Deliverable 3 may exceed 100 files with its tests: split at outline if the count says so. Not emitted: five plans run against N=4; it starts after PLAN-94 and PLAN-96 land (shared handlers and `MockRuntimeEngine.java`). The audit's two open points (the `operator.key.*` keys in no pack; `pm-operator enrol` in the specification but not the design) are in deliverable 3 (d) and need the operator's decision at outline.
- 2026-11-04 — The operator started PLAN-96, PLAN-94 and PLAN-93 ("started all three") and said PLAN-89 is STILL RUNNING ("missing aspects are landing"): rows `running` for the four (PLAN-89 back from `shipped`; its row keeps pr 305 and `landings/PLAN-89.md` as the analysis of the first landing). Ground truth: PLAN-89's lane has opened follow-up PR #314 (`fix/89-palette-review-followup`, created 2026-11-02T16:53Z, one file `CommandPalette.test.ts`, no production change, open and unmerged, no CodeRabbit review yet) with exactly the three body findings of review `2bbe1206`; PLAN-54's PR #313 is open (17 files). PLAN-93 D5 (i) therefore drops those three findings (spec corrected) and starts after #314 merges (same test file). FIVE plans run (PLAN-54, 89, 93, 94, 96) against N=4: the operator's start, recorded; no further emit until two land. When PR #314 merges, analyse it as the second landing of PLAN-89 (the findings of `2bbe1206` are then handled; the 50 px row-menu overhang is PLAN-96's). No lane message has arrived for PLAN-89, 54, 93, 94 or 96.
- 2026-11-02 — Landings of PLAN-89 (PR #305, `730380ee`), PLAN-47 (PR #311, `f6504d56`) and PLAN-92 (PR #309, `c7d9dcb8`) analysed by three read-only sub-agents (`landings/PLAN-89.md`, `PLAN-47.md`, `PLAN-92.md`); rows `shipped`, three messages archived (the design-audit-002 finding too), inbox empty. Merge rule: PLAN-47 was merged by the operator at 14:41Z with NO CodeRabbit review of any head (the lane had stopped at "Wait and resume later"; quota refusals only) — outside the rule, recorded as the operator's decision, the analyst's own review stands in; PLAN-89's second CodeRabbit review (`2bbe1206`, 14:15Z) has three findings in the BODY that were never fixed or answered — outside the rule (the first review's five were handled); PLAN-92 conforms (review `99b38f45` handled), but the lane's claim that a later incremental run covered `10fdedc` to `bb43243b` is false (CodeRabbit: "Review limit reached" for those heads). PLAN-89 filed no landing message at all. Verdicts: PLAN-89 D1 to D4 met in code, D5 (a) PARTLY (the 50 px row-menu overhang stays, answered not fixed), (b) refuted, (c) Chromium only; PLAN-47 D1 met, D2 met (unenrol typed name deferred to PLAN-59 by the spec), D3 PARTLY (the drain banner counts but neither lists nor counts down; the PR 297 focus-after-busy finding is not handled and the lane report never mentions it; an epic's cancel has no confirmation; overflow acts lose their reason and 44 px target); PLAN-92 D1 and D2 met, D3 NOT MET AS WRITTEN (no `projects` test, no attribute assertion, 700 ms settle, hand-written `refusedReads`, token-row tie), plus a functional lead: `WebCommand.confirm()` confirms the revoke on end of input. Routing: new staged PLAN-96 `web-acts-residue` (WS-03, 20960/20961: PLAN-47's gaps and the row-menu fit); PLAN-94 deliverable 5 (PLAN-92's open clauses and the revoke lead, first); PLAN-93 D5 (i) (PLAN-89's palette review findings). Design audit 002 (`28634b2a`, PR #310): statuses AGREED, the 14 terminal text tables replaced by links to the packs, the command tree without `abort` and `--drain`, three new open packages `operator-drain-signal`, `operator-command-tree`, `accept-edit-enrolment`: the earlier holds and rescopes stand (notes added to PLAN-42 and PLAN-79); the packages are NOT staged as plans — a question for the operator. Emit: PLAN-96, PLAN-94, PLAN-93 for the three free slots (gate determinate, no prompt, disjoint surface); PLAN-95 waits (shares `MockRuntimeEngine.java` with PLAN-96).
- 2026-11-02 — The operator started PLAN-92 and PLAN-54 ("I started both plans"): rows `running`, four running (47, 89, 92, 54), N=4, no slot free. On the same word the operator ordered a
  CodeRabbit audit of the ledger's whole history and a full cleanup, then a land. Audit: three read-only sub-agents re-read every `coderabbitai` review BODY and thread of the 39 pull requests
  the ledger names against the merge commits and `main` at `94bbefe1` (`landings/coderabbit-audit/README.md` and the three batch files hold the evidence). About 70 findings were never fixed and
  never answered, many only in review bodies; PR 275 is the worst (one blanket "Fixed." pasted into five threads whose concerns CodeRabbit said stayed open, then resolved); PR 284, 297, 299,
  253, 269 and 303 carry unresolved threads; PR 296's six threads were fixed by PR 300 but never resolved. PR 298, 300 and 302 carried `skip-bot-review` (PR 302, the PLAN-35 data components,
  never had a bot review) and PR 276 got only "rate limited" notices. Routing: PLAN-93 gained deliverable 5 (web findings of PRs 237, 243, 260, 287, 291, 296, 297); new staged PLAN-94
  `cli-coderabbit-residue` (WS-04: PRs 232, 270, 275, 292, 299) and PLAN-95 `mock-runtime-coderabbit-residue` (WS-01: PRs 243, 253, 269, 275, 284). The running PLAN-92 and PLAN-47 were NOT
  re-scoped: the extra findings went to their mailboxes (`inbox/to/rb-92-cli-interrupt-completion/`: `04-console.adoc:195`, the `dev-2`/`dev-3` literals, the failed-snapshot test;
  `inbox/to/rb-47-web-actions-by-state/`: the `ControlActionsBar` focus-after-busy finding of PR 297, which PLAN-47 inherits by removing that component). Cleanup (the `cleanup` verb): 33 stale
  verdicts of staged specs re-grounded by two read-only agents at `94bbefe1` (32 corroborated, 1 contradicted: PLAN-30's old hypothesis, whose rescope stands); 31 staged specs carry a
  re-grounded note; the held (PLAN-24, 42, 79) and rescope-at-outline (PLAN-30, 74, 36, 84, 51, 59, 83) notes of the design audit were written into the specs; PLAN-44 and PLAN-66 reach the
  scope guard and carry a scope note (weak merge, may split at outline); all 93 specs declare a resolvable surface (0 indeterminate); no duplicate-work match.
- 2026-11-02 — PLAN-90 and PLAN-91 landing analyses drained (inbox `rb-90-…-001`, `rb-91-…-001`), each by a read-only sub-agent (the operator's rule of this day: "For analyzing,
  always use a sub-agent") whose findings the orchestrator verified against `main` and the PRs; both rows shipped. PLAN-90 (PR #304, `3f26b6c2`, `landings/PLAN-90.md`): D1, D2, D3 met in
  code; the e2e edits correct stale expectations against `texts-and-languages.adoc:221` and `plan-overview.adoc:102-103`, none weakens an assertion; the merge conforms (CodeRabbit
  reviewed `c41df5cd`, `dc52c617`, `88846137`). Leads read, not run: the formatters' `locale` argument does not select the words, the token marks and `in`/`out` are still literals
  although `common.tokens.*` exist unread, `dateOnRead` does not validate the clock, positional `plan-card .first()` remains in six e2e specs (verified). PLAN-91 (PR #303,
  `94bbefe1`, `landings/PLAN-91.md`): D1 PARTLY (eight handlers return 130; `StartDialogue:125` and `PlanSettingsDialogue:77` and about eight more still map Ctrl+C to 0, verified), D2 NOT MET
  with no subject (the three goldens were last touched by `7d7b4d71` and `a1fe2d72`: never red), D3 met; the lane's residue (a typed Ctrl+C into a view-opened dialogue never arrives)
  is a structural lead with an unproven cause (the dialogue opens a second terminal through `StartPrompt.reader()`; the tests share one). CodeRabbit's late review of `0c92ada1` left
  six follow-ups open (two functional): the merge is OUTSIDE the merge rule (the operator, 2026-11-02: a result from CodeRabbit is always handled; I had first read it as conforming to the letter and was corrected). The lane's "no thread is open" check cannot see findings that sit in a review body. The six are PLAN-92's. Sonar is
  `undecidable` after both merges (unverified leads, not 0). Follow-ups collected into two staged plans, one pull request each: PLAN-92 `cli-interrupt-completion` (WS-04) and PLAN-93
  `web-format-and-token-residue` (WS-03; the `since` wording stays, the operator kept it). The inbox is empty.
- 2026-11-02 — The design audit's finding (inbox `design-audit-001`, sender `design-audit`, archived) recorded as the epic's reading, with no spec edited yet. Held: PLAN-24 (proof of
  presence turns L-1 into PM-SEC-3 text while its milestone is unplanned: rescope it to an operator decision on L-1 or hold), PLAN-42 (help "lists every verb" would fix the extra verbs;
  hold until the D26 package), PLAN-79 (rewrites the tests of Drain, AcceptEdit, Start, Onboard, Enrol; sequence it after the D25/D26/R7-7 package). To rescope at their emit (they are
  re-grounded then anyway): PLAN-30 (drop the table edits in 09/13, R4-14; move the OnboardCommand strings after D26), PLAN-74 (no done/dropped list with PLAN ids in web-app/README;
  FINAL becomes AGREED), PLAN-36, 84, 51, 59, 83 (Done-when: the row stays `done`, no link; PLAN-36 and 84 also add the needs/find rows to the spec command tree). Proceed: PLAN-47, 66,
  91 (shipped). Answered: `pm-operator drain` asks nothing (`shared/system.adoc:199`), so the open question at PLAN-91:61 is closed; D14 drops `pm-operator abort`; D26 removes the top-level
  verbs `start`, `onboard`, `enrol`, `unenrol` in a clients package of its own (PLAN-92 says so in its Standing Constraints). The staging-window rule (D24/D24a) leaves this epic's
  client work allowed (the client-work track). Not done by the orchestrator: no document of this repository or of `plan-marshall-documentation` was touched.
- 2026-11-02 — PLAN-92 and PLAN-93 staged (rows `staged`, specs under `plans/`); PLAN-92 and PLAN-54 emitted for the two free slots (running count 2 of 4 until the operator's word).
  PLAN-92 read a clean gate against the running PLAN-47 and PLAN-89 (0 overlap). PLAN-54 read a clean gate (0 overlap); its spec was re-grounded at `94bbefe1` (a note: `DataTable` and
  `TriagePage` exist, the two flaky reports specs become its own). PLAN-93 asks an overlap prompt against the running PLAN-89 (`chrome.spec.js`, `theme-readability.spec.js`): not emitted.
  Not chosen: PLAN-21 (free, kept as the next terminal candidate, PLAN-92 takes the terminal slot), PLAN-51 (large, its Done-when to rescope), PLAN-41 (held for PLAN-89).
- 2026-11-02 — PLAN-47 recorded running on the operator's word ("#47 is running"): four running (47, 89, 90, 91), N=4, no slot free. The ledger was landed
  as PR #306 (`6f4fd664`) just before; this entry and the row change are pending for the next land. Next emit when a slot frees: PLAN-54, PLAN-51 (large, re-read
  first) or PLAN-21; PLAN-41 stays held until PLAN-89 and PLAN-90 land.
- 2026-11-02 — The operator settled the merge rule ("There must be at least one CodeRabbit review. If later heads are not reviewed that is ok. Try it but do not wait
  for further reviews"): the open question on heads the reviewer never saw is closed (PLAN-17, 37, 43 conform). PR #294 closed by the orchestrator on the operator's word
  (redundant, the same 178 paths landed as #298). PLAN-26's first line is settled: the operator chose option 1 ("1. update, land the changes"), so `draining · 2 left` stays and the
  design (`shared/system.adoc#state-line`) is right as it reads; `stop in M min` and `restarting` are not restored and no plan is staged for them. The ledger is landed
  after this entry.
- 2026-11-02 — PLAN-43 landing analysed (PR #301, `2c25ed1d`, `landings/PLAN-43.md`) in the three layers and drained; row shipped. Layer 1/2: D1 (four templates, widths and
  the 1099 px collapse as UX-P-03 words them), D2 (`SystemView` inside `DashboardPage`, `system.spec.js` and `SystemView.test.ts` not in the diff) and D3 (`pageWidth.test.ts`
  with eight control fixtures) met; D3's red run was taken after the move commit, the detection is proved by the controls (the lane says so). 11 declared files present,
  17 beyond (README.adoc, `templateStyle.ts`, 14 fixtures, one pm-api test fixed with the operator's approval in the lane: `Generators.nonEmptyStrings()` can yield a blank
  string that `RoleAssignment.assigned()` treats as unassigned). Layer 3 (read, not run): templates sound; the tests read stylesheet text, so only the dashboard has been
  drawn by a browser; `SystemView` fills two of four regions (grid and updated line unproven); an unfilled `id` slot of `ObjectPage` draws an empty pill. Sonar: lane
  `confirmed` 0 at `3049c450`, my read `undecidable`, an unverified lead. Emitted PLAN-47 (acts by state) after re-grounding its spec at `2c25ed1d`: `ActionBar` exists (PLAN-25)
  and is extended, `ControlActionsBar` carries PLAN-37's Escape, focus return and reasons and must not lose them, `SystemView` is shared with PLAN-90 (different hunks).
  PLAN-41 (verify-web-design-system) is held until PLAN-89 and PLAN-90 land: run now it would file their fixes as findings.
- 2026-11-02 — PLAN-89 recorded running on the operator's word ("#89 is running"): four running (43, 89, 90, 91), N=4, no slot free.
  Next emit when a slot frees: PLAN-41 (verify-web-design-system), PLAN-21 or PLAN-84 (terminal).
- 2026-11-02 — PLAN-37 landing analysed (PR #297, `c5bfb1ca`, `landings/PLAN-37.md`) in the three layers and drained; row shipped. Layer 1/2: D1 (Escape and the
  returned focus on the pause panel and the chat; the overflow's Escape predates the plan) and D2 met; D3's e2e command not met as written (seven base
  failures, PLAN-90); 7 of 7 declared files, 22 beyond, one dependency (the approved `@axe-core/playwright`); no uppercase and no font under 12 px remain
  (grep), 44 px targets in three components. Layer 3 (read, not run): sound; three leads not reproduced (the `IconButton` label from 768 px against the
  `DataTable` row menu's 60 px column and long name; `ChatPanel`'s Escape guard on any `role="dialog"`; the opener on Safari/Firefox clicks) and the audit
  runs `de` at 1440 px only: all in PLAN-89 deliverable 5 and 4. Sonar: lane `confirmed` 0 at `f5e00490`, my read `undecidable`, an unverified lead. The merge
  rested on the operator's selection in the lane with the last head unreviewed and four threads open. Emitted PLAN-89 (palette hardening; the audit helper
  it needs has landed; gate determinate, no prompt, one file shared with PLAN-90) after adding deliverable 5 to its spec.
> ↪ Relocated to `settled.md` § "Landings and running records of PLAN-16 to PLAN-91 (2026-11-01)" — every plan these entries record is shipped (PLAN-90 and PLAN-91 later landed too) and its landing report is in landings/
- 2026-11-01 — Fix plans staged at the operator's word ("collect the open aspect and create large follow-up / fix PRs, each costs a
  CodeReview"): PLAN-89 (`web-palette-hardening`: palette listbox, the strengthened tests, the design document, the axe audit of the palette;
  after PLAN-37), PLAN-90 (`web-green-baseline-and-time-residue`: the seven red e2e tests and the PLAN-16 formatter residue) and
  PLAN-91 (`cli-interrupt-and-transcripts`: Ctrl+C status in eight handlers and the three transcript goldens). Gate: determinate, 0
  overlaps for all three. Staged, not emitted: four plans run. Each is one pull request on purpose and carries the 100-file limit of
  the required reviewer. Not collected, and why: the German glossary, `MOCK-201`, `PM_OPERATOR_DEV_ALLOW_AGENT`, stylelint and the other
  dependency choices (operator decisions); the labelled clear action of the palette and `drain` confirmation (design decisions); `format:check`
  on 42 files and the dark contrast audit (after the view plans, owned by PLAN-85 and PLAN-70); the two SystemView leads (not
  reproduced by reading); the older residues of PLAN-03, 04, 10, 12, 15 and 19 (Watches below), which predate many landings and were
  not re-verified.
- 2026-11-01 — Operator rule recorded in Queue annotations: every lane result is analysed in three layers (specified, implemented,
  correct), by reading and never by building. Applied at once to PR #300: layer 1 met with a design-document gap, layer 2 present with
  an overstated "exactly those six", layer 3 one new ARIA defect and three weak tests (landing report). PLAN-43 recorded `running`
  at the operator's word: four plans run (26, 35, 37, 43), N=4, no slot free.
> ↪ Relocated to `settled.md` § "Landings and running records of PLAN-09 to PLAN-86 (2026-10-31)" — every plan these entries record is shipped and analysed
> ↪ Relocated to `settled.md` § "Landing of PLAN-20 and the running records of PLAN-77, 80, 82 and 86 (2026-10-30)" — PLAN-20 is shipped and the four plans it ran beside are shipped
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

- 2026-11-06 — OPEN OUTSIDE THIS REPOSITORY (owner `plan-marshall/plan-marshall-mcp`, no plan there): (1) the real `pm-mcpd` accepting `{with_plans, reason}` on an epic cancel and sending `drain_remaining_items` was not checkable by PLAN-100; (2) a runtime that serves an id or route per drain item (`doc/design/shared/system.adoc` `<<one-step>>` says an item is a name, not a step, until it does); (3) `pm-mcpd` accepting the cleanup record `cleanup_durations` (PLAN-60 D4). The operator may open ledger items there.
- 2026-11-06 — DECIDED by the operator (option A): the unfiltered "Mark all shown read" of the reports page marks the reports the list shows by their ids, never everything, as `doc/design/shared/reports.adoc` 182/184 and the label say; PLAN-102 D1c (`{ all: true }`, merged in PR #340) is amended by PLAN-105 deliverable 1 (`landings/PLAN-102.md` defect 1). PLAN-105 is emittable.
- 2026-11-06 — LOW defect 17 of PLAN-102 (`landings/PLAN-102.md`): `RowMenu.svelte` 216-232 `:focus` and `:focus-visible` rules are identical, a mouse open draws the focus ring; folded into PLAN-105 deliverable 2 (decide and test).
- 2026-11-05 — Two LOW defects of PLAN-101 (`landings/PLAN-101.md` 5, 6) have no staged plan touching their files: `check-build-output.mjs` `deriveDevMarkers`/`devPageRoute` guards only the first dev page below the prefix and the plugin's `DEV_PAGE_IMPORT` regex (`devRouteExclusion.ts`) hard-codes `src/routes/dev/`; the block-comment strip of `strictPortIsBound` (`guard-ports.js` 94-96) has no test case. Re-check at the next plan that touches the build check or the port guard.
- 2026-11-05 — `ActionSupportTest.leaveBackPreservesNonInterruptExitCodes` (PLAN-103 N5) duplicates `theLeaveBackMapping`; the chat sites' non-interrupt exit codes are not covered. Not routed.
- 2026-11-05 — `pm-api` `Libc.java:91` and `ScenarioLoader.java:566` hold `/*TODO: Catch specific…*/` markers; `MockRuntimeEngine` seven more (PLAN-95/97 area). Not routed.
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
  `rb-20-cli-actions-by-state-001.md` (PLAN-20, opencode, two models), same two keys. Recurrence 2026-10-31:
  `rb-80-web-standards-conformance-001.md` (PLAN-80, opencode), same two keys. Recurrence 2026-10-31:
  `rb-77-cui-standards-foundation-001.md` (PLAN-77, opencode), same two keys. Recurrence 2026-10-31:
  `rb-86-shared-error-texts-001.md` (PLAN-86, opencode), same two keys. Recurrence 2026-10-31:
  `rb-09-web-display-layer-001.md` (PLAN-09, opencode) and `rb-78-cui-testing-api-001.md` (PLAN-78, opencode), same two keys
  each. Recurrence 2026-10-31: `rb-82-needs-and-find-contract-001.md` (PLAN-82, opencode), same two keys. Recurrence 2026-10-31:
  `rb-78-cui-testing-api-003.md` (PLAN-78, the landing message, opencode), same two keys. Recurrence 2026-11-01:
  `rb-16-web-time-format-and-locale-001.md` (PLAN-16), `rb-46-web-command-palette-001.md` (PLAN-46, merge_state open) and
  `rb-87-cui-testing-mock-runtime-001.md` (PLAN-87), opencode, same two keys each.
  Recurrence 2026-11-01: `rb-17-cli-runtime-act-confirm-001.md` (PLAN-17, opencode), same two keys.
  Recurrence 2026-11-01: `rb-46-web-command-palette-002.md` (PLAN-46, the landing message, opencode), same two keys.
  Recurrence 2026-11-01: `rb-46-web-command-palette-003.md` (PLAN-46 follow-up) and `rb-37-web-accessibility-baseline-001.md` (PLAN-37, merge_state open), opencode, same two keys each.
  Recurrence 2026-11-01: `rb-26-cli-system-model-001.md` (PLAN-26, opencode), same two keys.

## Watches

- Fix plans staged 2026-11-01: PLAN-89, PLAN-90, PLAN-91 (see the Decisions entry). PLAN-89 waits for PLAN-37 (the audit helper). PLAN-90's
  seven red e2e tests are the base failures of PLAN-37's third deliverable: when PLAN-90 lands, that gate command can pass. Emit order
  when slots free: PLAN-90 (RUNNING since 2026-11-01; now with deliverable 3, the SystemView words), then PLAN-91
  (PLAN-26 has landed and did not touch `status`; now with deliverable 3, the tests PLAN-26 left out), then PLAN-89.
- PLAN-26 follow-ups (landing report): (1) SETTLED 2026-11-02, the operator kept `draining · 2 left` (the rest of this item is history): keep `draining · 2 left` (the design says so now)
  or restore `draining · N plans finish · stop in M min` and `restarting`, which needs the client record to read the drain deadline the
  design lists (`system.adoc:365`) and the runtime to send it (pm-mcp-core, not read); (2) PLAN-30 D4 (the exit-code table for every verb)
  is partly done: 19 read verbs and four write paths exit 1 on a refusal; re-scope it before emit; (3) the `running-work` mock scenario has
  no `GET /api/v1/system` route (only `/system/cleanup` links), so the system view there shows only its load failure: a mock change no plan owns;
  (4) re-read PLAN-23, 30, 32, 41, 42, 48 at `be4995e5` (they name files PLAN-26 changed).
- PLAN-35 follow-ups (landing report): re-read PLAN-43, 45, 51, 53, 55, 58 against the shipped components; the view plans bring the
  components in (no view calls them yet) and should assert the CSS-level behaviour where they touch it (alignment, 40/32 px rows, head border,
  `%` widths); PR #294 was closed on 2026-11-02.
- Landing follow-ups 2026-11-02 (PLAN-47, 89, 92): all in PLAN-96, PLAN-94 D5 and PLAN-93 D5 (i); PLAN-47 never had a CodeRabbit review, so PLAN-96's pull request is the first review of those 35 files. The three packages of design-audit-002 are PLAN-97 (2026-11-04).
- CodeRabbit audit follow-ups (2026-11-02): all in PLAN-93 D5, PLAN-94, PLAN-95 and the two mailboxes; PLAN-94 follows PLAN-92 and PLAN-93 follows PLAN-89 (shared files). Operator decisions inside them: the
  `NeedsOperatorItem` item id (PLAN-95 D3: read the contract), the `broadcastReset` after `goToState` (PLAN-95 D2), the optional catalogue-rollback retention (PLAN-93 D5). Open reading to repeat on
  every landing: the review BODIES, not only the threads.
- PLAN-90 follow-ups (landing report): all collected in PLAN-93 (token marks from `common.tokens.*`, the formatters' `locale` argument, the clock validation and the untested time edges, the dead
  key `project.explorer.last_active`, positional `.first()` in six e2e specs). The `since` wording on completed-event cells stays as the design says (the operator kept it).
- PLAN-91 follow-ups (landing report): all collected in PLAN-92 (the typed Ctrl+C proof and, if red, the one-terminal fix over fourteen `createTerminal()` closers; the remaining handlers to 130; the
  documents `03-dialogues:44`, `04-console:197`, `01-commands`; the tests). The goldens need no work. D2 of PLAN-91 had no subject: the PLAN-87 claim that they were red was second-hand.
- PLAN-43 follow-ups (landing report): PLAN-90 deliverable 3 rebases over the `SystemView.svelte` move; PLAN-51, 53, 55, 58 add their view to `VIEWS_ON_TEMPLATES`, are the first
  browser check of Triage, Object and Wizard, and fill or guard the `ObjectPage` id slot; a pm-api owner may sweep `nonEmptyStrings()` where a record treats blank as absent
  (`OnboardingRecordsTest` 134, 167-168, 517, `SystemRecordsTest` 153-154, not read); flaky `designer.spec.js:157` and `reports.spec.js:34/89` have no owner.
- PLAN-37 follow-ups (landing report): PLAN-89 deliverables 4 and 5 (the leads above and the wider audit); `SystemView.svelte` is now touched by PLAN-37 (merged),
  PLAN-90 deliverable 3 and PLAN-43 (hunks differ, rebase when either lands); the operator's call on the standing merge authorization for heads the reviewer
  never saw. The send button of the chat no longer disables itself on an empty message (the lane's choice).
- (history, PLAN-37 has since merged as `c5bfb1ca`) PLAN-37 was stopped at the reviewer (observation `rb-37-web-accessibility-baseline-001.md`, 2026-11-01T15:14Z; PR #297 `open`, head
  `f27e1161`, read with `ci pr view` and the PR comments). CodeRabbit refused four triggers on quota (11:41Z, 13:13Z, 13:23Z, 14:56Z);
  no review exists; the lane's next allowed trigger is 2026-11-01T16:26:44Z; it will not merge without a review. The session has
  stopped, so the plan holds a slot while idle. DECISION FOR THE OPERATOR: re-trigger `@coderabbitai review` after 16:26Z, or label
  PR #297 `skip-bot-review` as for #298 and #300. Lane account, not re-run: D1 and D2 met; D3's gate command
  (`-pl pm-web-e2e verify -Pe2e`) does not pass as written because of seven e2e failures it did not cause; the names equal those the
  PLAN-46 run recorded on `main` (`epic-view.spec.js:190`, `plan-view.spec.js` x3, `showroom.spec.js:88`, the `plan` text guard
  de/en), so the two runs agree. The audit covers the German interface at 1440x900 only. Sonar `confirmed` 0 at
  `f27e1161`, corroborated by my own read (analysis revision equals head). Under-declared surface: `start.{en,de}.properties` in `pm-api`.
  The seven red e2e tests need an owner; the epic has none.
- PLAN-17 landed (PR #292, `66c1495a`, analysed 2026-11-01); see `landings/PLAN-17.md`. Open: (1) the terminal specs that name a
  file it changed are to be re-read at HEAD `66c1495a` before emit: PLAN-21, 23, 30, 36, 42, 48, 84 (operator catalogue pair,
  `01-commands.adoc`, `RuntimeCommand`) and PLAN-26 (re-read, note added); PLAN-23 also reads `ChoicePrompt`, which now throws
  `InterruptException` and exits 130. (2) Under-declared surface: 9 files added to a 9-entry declaration; with PLAN-16 and 82 the
  third plan to do so. (3) Ctrl+C still exits 0 in eight non-runtime handlers (`WorkActionDialogue`, `ReviewDialogue`,
  `HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand`, `ActionSupport` x2) and no plan owns them; two web defects the
  run reported in `SystemView.svelte`: read again at `be4995e5` after the PLAN-26 lane reported the same area: the English
  `retry`/`error`/`ok` words and the load-failure string stored at the error (so it does not follow a language switch) are CONFIRMED by
  reading and are owned by PLAN-90 deliverable 3; the raw `waiting for_you` state id is NOT reproduced (no `replace(` call), owned by no plan. (4) The lane merged after a question the operator answered
  in free text that did not grant the merge ("fix all and check again"), on the standing review authorization; same practice as
  PLAN-16 and 87 — the operator may want to confirm it covers that. (5) `ci pr comments` counts issue comments as unresolved
  review threads and names no thread ids (a provider defect on the plan-marshall side). (6) The Sonar claim could not be
  re-measured after the merge. — re-check each where it is consumed.
- PLAN-16 and PLAN-87 landed (PR #291 `9890d74b`, PR #293 `418579a0`, analysed 2026-11-01); see `landings/PLAN-16.md` and
  `landings/PLAN-87.md`. Open: (1) PLAN-16 rewrote 19 views and 6 components, so the staged web specs that name them are to be
  re-read at HEAD `418579a0` before emit: PLAN-43, 45, 47, 51, 52, 53, 54, 55, 58, 59, 60, 62, 64, 66, 72, 73, 83, 85, 88 (the list
  is in the landing report). (2) PLAN-16 under-declared its surface (10 declared, 38 realized, 5 added: `MockRuntimeEngine.java`
  in a module the spec did not name, `OnboardingScenarioTest`, `client.ts`, `sourceLabels.ts`, `runtime.ts`); with PLAN-82 this is
  the second plan to do so. (3) PLAN-16's residue: the framing words and unit marks live in `display/format.ts` (a follow-up adds
  `common.time.*` keys), `relative` has no `since 14:02` form, the activity day header exists only where the wire dates a row,
  `project.time.*` keys are unused. (4) Both landings' Sonar claims (0 at `1f10f52b` and `7f7249fc`) could not be re-measured
  after the merge (`undecidable`). — re-check each where it is consumed.
- Pre-existing red tests on `main`, reported by two lanes (2026-11-01, not verified here): PLAN-46's run saw `plan-view.spec.js` x3,
  `showroom.spec.js` x1 and the `theme-readability.spec.js` text guard for `plan` de/en x2 fail identically on `origin/main`
  (`ae0cb6b8`); PLAN-87's run saw three transcript goldens fail in `pm-operator verify -Pintegration-tests` (`status.transcript`,
  `start-plan.transcript`, `start-plan-no-input.transcript`) identically on an untouched `main`. No plan owns either. PLAN-16's run reports 308 e2e tests green on its own branch, which may mean it fixed the web ones;
  not measured on `main` since. — re-measure on the current `main` (PLAN-37 records the failing names at its base), then decide
  an owner for what remains.
- PLAN-46 residue (2026-11-01, PR #296 merged): `overview.find.plain` is cited in the shared design's prose with no Texts row and no
  catalogue key (nobody owns it); the palette field has no labelled clear button (the design names no key); `/` now belongs to the
  palette, so a view's filter no longer focuses on `/` (the design assigns the key to the panel; an intended change). PLAN-46's
  message counts five files outside its declaration. — settle where the next find consumer (PLAN-84) reads the design.
- PLAN-82 landed (PR #284, `7d7b4d71`, analysed 2026-10-31); see `landings/PLAN-82.md`. Three things stay open. (1) The
  consumers must re-read their specs at HEAD `7d7b4d71` before emit: PLAN-36, 49, 52, 55 name files PLAN-82 changed
  (`overview.*`/`plan_view.*` catalogues, `ActivePlanSummary`, the mock engine and scenarios, the `pm-operator` status
  transcript) and PLAN-34 overlaps the `overview/` records. (2) The run's residue, to settle where each is consumed: the
  mock's needs list is fixture-driven (an act removes no item; `overview.needs-changed`/`needs-resolved` stand in);
  `pm-api` has no typed body for the machine resource; the order key is `created_at` with a `running_since` fallback and
  the fixtures exercise only the fallback (PLAN-34); the machine summary counts 2 where the list totals 12 (PLAN-36's
  badge design reconciles). (3) The landing's Sonar claim (0 open at `038d4a1e`) could not be re-measured after the merge
  (`undecidable`, `head_revision_unavailable`). — re-check each at the plan that consumes it.
- Surface under-declaration, PLAN-82 (2026-10-31): the spec declared 15 entries and the diff touched 39 files; 17 were
  never declared (all tests, the new design document, `MachineApiPaths.java` instead of the declared `MachinePaths.java`,
  and three `pm-operator` test files in a module the spec did not name). The lane's message says the surface stayed
  inside the named modules; the diff says otherwise. No staged spec collided in practice. — when a spec that changes a
  shared contract is staged, declare the tests and the sibling module whose tests the change forces (PLAN-34, 36, 84).
- Structure review of the staged plans, applied 2026-10-30 at the operator's word ("fix clear and weaker"); see the
  Decisions entry of that date. Left open: PLAN-84 (`find`) stays in WS-04 although it belongs with PLAN-36 in WS-05 (the
  queue verb cannot change a row's workstream; the workstream documents say so); the review finding "PLAN-59 bundles
  projects with project onboarding" was withdrawn after reading PLAN-64: the project onboarding view and the machine
  onboarding screens are different views and documents. — re-check the split at each emit of the plans it touched.
> ↪ Relocated to `settled.md` § "Retired Watches (2026-10-28 to 2026-10-30)" — the watch is retired, its subject is closed (PR #276 merged as `44803a77`)
- PLAN-46 review findings fixed in intent (PR #300, `1ce32d80`, 2026-11-01), but the three-layer analysis (read, not run) found
  more; see `landings/PLAN-46.md`, section "Three-layer analysis of PR #300". OPEN, for the operator: (C1) the results container is
  now a `listbox` and still holds the empty-state paragraph and the Retry button, invalid owned content in the "no match" and "failed"
  states; (C2, C3) a heading and a repeated label inside the listbox group, a constant `aria-expanded`; the design document was not
  updated in the PR although the plan's Standing Constraints require it; the filter test never asserts the selection and another test
  leaks a `scrollIntoView` stub. One more small fix PR would close them; no queue row. Earlier text: The six findings CodeRabbit filed on
  `42aee47d` (two Major: no touch close control, arrow-key selection not exposed; four Minor) are in the merged code with tests. The
  fix had no bot review (`skip-bot-review`), so the two accessibility changes (combobox with `aria-activedescendant`, close control)
  rest on the lane's self-review; PLAN-37's axe-core audit is the first independent check once it merges, if it reaches the palette
  (not established). Open: the palette's clear action is the browser's own, `type="search"`, no catalogue key; `/` belongs to the
  palette now. PR #294 is redundant (the same 178 paths landed as PR #298): close it, the operator's call. The merge authorisation
  question stays open: PLAN-17 merged after a free-text answer that did not grant it, PLAN-46 on a review whose findings were
  unhandled; PR #300 merged on the operator's word and a label.
- Pull requests over 100 files cannot be reviewed (PLAN-78 run, 2026-10-31): CodeRabbit, the required reviewer, refuses
  more than 100 files and, once its quota is spent, every retry; Sourcery refuses above 150,000 diff characters. PLAN-78's
  first PR (197 files) was never reviewed and was closed; the second (#288) merged. The 197 came from the `pre-commit` gate
  rewriting the import grouping of 171 `pm-api` main sources (pre-existing drift on `main`, no plan owns it). PLAN-78 landed
  with 25 files (the rewrite restored from `origin/main` twice, on the operator's decision), and its message says the drift
  stays on `main` (170-odd rewrites). PLAN-87 landed (40 files) and had the operator route the rewrite into its own pull
  request: PR #294 (`style: apply the pre-commit import grouping to main sources`, label `skip-bot-review`, 177 `pm-api` sources
  and one `pm-mock-runtime` source). On 2026-11-01 the PLAN-46 lane landed the same 178 paths as PR #298 (`f5de8449`, label
  `skip-bot-review`), so the rewrite is gone from `main` and PR #294 is redundant and still open (the operator's to close). Runs
  that start from `f5de8449` or merge it forward no longer meet the rewrite (PLAN-35, 37, 26 started earlier). PLAN-79 (about 116 classes) may still need a split;
  PLAN-87's share of classes taking generated input was 40 of 45, against 25 of 73 in `pm-api`, so its estimate is a hypothesis to
  check at its outline.
- Cleanup reminder interval has no write path (PLAN-09 residue, 2026-10-31): `cleanup_durations` is a single integer, so
  the reminder cannot be set from the clients; the run answered it on the PR as a server-contract gap. — no plan owns it;
  decide with the daemon repository.
- Two node-kind message families (PLAN-09 residue, 2026-10-31): `state.node_kind.*` beside `plan.node.kind.*`; both are used
  and nothing keeps them in step. — check at PLAN-55 and PLAN-62, which touch the plan tree.
- German glossary (PLAN-86 residue, 2026-10-31): the 42 German error texts use *Gate, Store, Terminal, Setup, Listener, Log*
  and *Audit-Kennung*, none of them an entry of the glossary in `doc/design/shared/texts-and-languages.adoc`, which says a
  new domain word is added there before it is used. The same words already run through dozens of German texts. — the
  operator decides: add the words to the glossary (binds every plan's German texts) or reword the texts.
- Coverage gap of the logging test (PLAN-77 residue, 2026-10-31): the `MOCK-201` close-failure record of
  `MockRuntimeExtension` is asserted through a package-private helper, because no production path reaches that catch
  (`MockRuntime` is final and `close()` catches every `IOException`). — accept it as is, or ask for a production seam;
  the operator's call.
- The contrast audit's "dark" half may audit the light theme (PLAN-80 run, 2026-10-31, reported and not verified): the
  matrix calls `setTheme` before `page.goto`, and `app.html` re-derives `data-theme` from a `localStorage.pm_theme` that
  nothing writes. If it holds, every dark result of `theme-readability.spec.js` since PLAN-12 is unproven. — PLAN-85 makes
  the helper set the storage and assert `data-theme`; PLAN-70 asserts it before its light pass relies on the audit. Check
  at the first of them to land, and re-run the dark matrix once the assertion exists.
- `npm run format:check` fails on 42 files of `pm-web-app` at HEAD (PLAN-80 residue; the Maven build does not run it). No
  plan owns a formatting pass, and one would touch most views. — decide whether any plan takes it; not before the view
  plans have landed.
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
