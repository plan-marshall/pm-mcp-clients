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

- WS-06 and the design-review rows (2026-11-06) — PLAN-108 (residue, runs before D1), PLAN-112 (D1 component vocabulary),
  PLAN-113 (D2 navigation), PLAN-114 (D3 brand and mascot), PLAN-115 (D4 chat and voice) are design sessions: documents only,
  `Status: DRAFT`, one pull request each (D4: one per repository), no auto-merge, the operator sets `AGREED` as the last commit
  and the merge on the operator's word. Gate for a waiting plan = the merge of its session's pull request, not the status a
  document has today. Order: PLAN-108 → D1 → D2 and D3 (D3's pull request merges after D2's) → D4. Hand-off for these rows and
  for PLAN-109/110/111 is a direct Claude Code session with the absolute spec path in the prompt (operator instruction
  2026-11-06); the row's `lane` field carries the default and has no meaning for them.
- PLAN-109 (rail "Settings" label, cog, `control.plan.settings`) and PLAN-110 (drop the start-page toggle) start after the
  running PLAN-106 lands (`control.*` properties and `runtime.ts`); PLAN-111 (drop the layout modes) shares nothing with a
  running plan, but not in parallel with PLAN-109 (`common.*.properties`); PLAN-107 (staged) starts after PLAN-97 lands and not in
  parallel with PLAN-108 (`RowMenu.test.ts`).
- Parked for the design review (2026-11-06): PLAN-29, 41, 49, 51, 52, 53, 55, 58, 59, 60, 62, 64, 65, 66, 70, 71, 72, 73, 74,
  75, 83, 85, 88 — each carries a `## Design Review 2026-11-06 (Amendment)` section with its gate. Hold-class (re-ground
  after D1–D3/D4): 41, 52, 55, 58, 59, 71, 75; PLAN-83 is folded into the D2 engine. Unparked by the merge of the named session
  and its implementation package, by the orchestrator, with a re-grounding of the spec.

- Spec-authoring rules (operator 2026-11-11, from PLAN-130's lessons 002-005; apply to every spec staged or amended from now):
  (1) when a spec says "file X stays unchanged" but the change invalidates an assertion in X, name the case that may be
  replaced in the spec; (2) when a deliverable replaces existing tests by design, list them under "Tests to remove" so the
  test-integrity check accepts them without an operator stop; (3) a criterion about how a mark or screen looks is evidenced
  only by a rendered comparison (screenshot or rendered sheet), never by reading the source — plan the render; (4) never write a
  deliverable criterion whose evidence exists only after the PR (Sonar count, CI result, review outcome): state it as a landing
  obligation of the finalize steps instead. (5) (PLAN-132 lesson 014) never declare a design table read-only when a test
  compares the message packs to it: a change of wording then has to change the table.
  Rule (4) applied 2026-11-12 (operator, after PLAN-147 needed an override at five heads for its D5 screenshot/Sonar Done-when):
  every staged spec lost its "open-issue read at the pull request head" Done-when (a Standing Constraint carries the Sonar gate
  instead), and "screenshots recorded" now reads "kept in the plan's working area, re-taken after any later visual fix; the pull
  request lists them after the push; not a gate before it". New specs follow this form.
  (6) (PLAN-128, operator 2026-11-13) never bind a Done-when to a count read by `ci pr comments`: it counts every review-body row as
  unresolved. Name the threads or items to answer and evidence them by GitHub's thread state (`isResolved`, a reply in the thread).

## Decisions

- 2026-11-14 (operator, `analyze-fix-build-time` finding #432): `!` on a needs item runs exactly the command the row prints — for a
  plan's hold, failed landing and condition it opens nothing in the full screen. One rule; no subject-specific opening.

- 2026-11-14 (operator, PLAN-148 landing): the integrity hold is named **`integrity_hold`** — the work package
  `doc/plans/harness-decision-log-hold-names.adoc` stands and renames the code (`NeedsOperatorKind.INTEGRITY_PAUSE` → `INTEGRITY_HOLD`). PLAN-148's
  "assumed" D-2 went the other way: it flipped `needs-and-find.adoc:42` and `components-tags.adoc` to `integrity_pause` and added
  `ReasonBadge` with `overview.needs.integrity_pause` (`ReasonBadge.test.ts:152-159` pins `integrity_hold` as "another reason"). The package
  must ALSO flip those two documents back and update `ReasonBadge` (`REASONS`, `DANGER_REASONS`, the key) and its test; its task list in
  `doc/plans/` needs that addition (a repository doc edit, not a ledger write: the operator's or a direct change).

- 2026-10-25 — Parallelization scope 3, stated by the operator in the invocation.
- 2026-10-25 — Operator decisions of VD-S-00 and the dependency questions (AskUserQuestion): Inter is
  self-hosted (PLAN-12); own SVG icon set, no Lucide (PLAN-19); axe-core may be added to `pm-web-e2e`
  (PLAN-37); stylelint may be added to `pm-web-app` (PLAN-72). Recorded in the design by PLAN-12.
> ↪ Relocated to `settled.md` § "Set-up decisions of 2026-10-25 (ledger landing, review merges, split guard, Sonar block)" — the one-time ledger landing (superseded by `use_worktree` and `land`), the applied review merges and split verdicts, and the configured Sonar block
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
- 2026-11-06 — Landing of PLAN-18 (PR #349, `02efb96b`, merged 19:42Z, ONE docs file; `plan-marshall` lane, landing message 19:52Z) analysed by a read-only sub-agent (`landings/PLAN-18.md`). Threads 0 of 2, Sonar has NO analysis (docs-only; the 2026-10-28 exception was missing from the verify specs). The record's verdicts hold (UC-V-01, CLI-UC-V-09 mock part, verify-first; 13 of 13 weeks/days ages listed), BUT: the `doc-review` criterion cannot be run (the skill reviews requirements and specs, not result files) and cost three operator rounds; it is in ten staged specs (29, 33, 41, 42, 48, 63, 65, 70, 71, 75); the CLI-CC-V-03 fail undercounts (`2 months ago`, bare `yesterday`, `just now`; `project-explorer.adoc:71` states a second rule; the rule is silent beyond 30 days); the first clause of CLI-UC-V-09 is a missed FAIL (`PlanCommand` prints the hard-coded `Plan %s not found.`); UC-V-09 and CLI-UC-V-10 have no owner; the PR body misstates two verdicts. ROUTED: PLAN-34 D4 (decides the age rule first; widened) and PLAN-74 D4 (widened). Row PLAN-18 `shipped`, messages 002-007 archived (five candidate lessons: 002/004/006 are plan-marshall-side, 003/005 ours). OWED for the drain after restart: the `doc-review` clause in the ten specs, a home for UC-V-09/CLI-UC-V-10 and the `plan nope` clause (PLAN-33/PLAN-30), the docs-only Sonar exception in the verify template and the zero-Sonar rule, a link for `verification-results/`. The ten `design-review-*` finding messages in the inbox are NOT drained yet (operator, 2026-11-06: after the restart). Running now: PLAN-97 (PR #358), PLAN-104 (PR #357), PLAN-106 = 3 of N=4: ONE free slot.
- 2026-11-06 — DECIDED by the operator ("do as proposed"): the list after "Mark all shown read" reloads the FIRST page of the unread list after the request succeeds; no cursor contract with the daemon (PLAN-107 deliverable 1, `landings/PLAN-105.md` defect 1). The decision is recorded in the PLAN-107 spec.
- 2026-11-06 — The operator started PLAN-106 ("#106 started", the lane form printed for it; row `running`, plan id not yet known). Running: PLAN-18 (plan-marshall, PR #349), PLAN-97 (plan-marshall), PLAN-104 (lane), PLAN-106 (lane) = 4 of N=4: no free slot.
- 2026-11-06 — Landing of PLAN-105 (PR #348, `f5ab6dd2`, merged 09:54Z, 15 files; lane message 09:56Z) analysed by a read-only sub-agent (`landings/PLAN-105.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. FIRST landing to state both counts as facts lines (`unresolved_review_threads=0`, `review_body_findings_total=3/handled=3`, Sonar 0) and they are true (0 of 6 threads; 0 of 10 on PR 340 and PR 325; Sonar 0), but hollow: two of the three nitpicks of review `5797372610` were declined with FALSE reasons (V: `client.test.ts` has no bulk-read body test, the cited JS test mocks the client; the Java scenario test still asserts counts), the third deferred; CodeRabbit has the last word on 5 of the 6 threads of PR 348 and on 4 of the 6 PR 340 threads; "tracked elsewhere" was claimed twice and tracks nothing; PR 325's ten threads got no reply. Defects: after "Mark all shown read" the page keeps "Show N older" but its next page is an offset into the shorter unread list (reports unreachable until a reload; mock-verified, the real daemon's paging unreadable); the CLI `reports read --all` still posts `{"all":true}` (V: `ReportsCommand.java:210`) and the mock now answers 200 with 0 marked, against `24-reports.adoc:28`; an in-flight reload double-subtracts; a deep-linked report is under-counted; the e2e test for mark-all expects the old outcome. FIXED from PLAN-102: the by-ids rule, the click guard, the dead Escape guard, N1-N5 (N4 brittle), the h1 while loading, fixture names, `:focus` vs `:focus-visible`. ROUTED: PLAN-107 `web-reports-fourth-residue` (WS-03, 21070/21071, staged, NORMAL size: `plan-marshall` form, start AFTER PLAN-97 lands; list after mark-all: reload the first page, decided by the operator); PLAN-74 deliverable 3 gains the RowMenu/bottom-navigation positioning; PLAN-45 deliverable 4 stays the reload mechanism. Row PLAN-105 `shipped`, message archived. Seventh lane in a row to report "all handled" with a real defect open.
- 2026-11-06 — Landing of PLAN-100 (PR #331, `1a36ed5e`, merged 08:34Z, 23 files; lane message 08:36Z, amended 08:37Z) analysed by a read-only sub-agent (`landings/PLAN-100.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. Counts hold: 0 of 10 threads unresolved, Sonar 0. BUT: a THIRD CodeRabbit review (`5793110125`, on `2ce7f9f1`, one outside-diff item: the hand-written `PLAN_ACTS_HANDLED`/`EPIC_ACTS_HANDLED` lists in `WorkControlPanel.svelte`) is unmentioned in the landing, unanswered and unhandled (V: the lists are still there); D3c (the unknown running count) is DEAD in the product (V: `EpicResource.running` is typed `number`, `resource?.running ?? null` is null only while `resource` is null, when Next plan is disabled) and its reply on thread `4484094188` is wrong; a failed epic-resource read leaves Next plan disabled with a false text and no retry; two view-level guard tests never reach the guard they name (`ConfirmDialog` refuses first) and say the opposite; four CodeRabbit fixes can be reverted with the suite green; an unknown withdraw is drawn as a definite failure; the PR body is stale. Retracted lane claim: the `AbortSignal.timeout` spy does not leak (global `restoreAllMocks`). ROUTED: PLAN-106 `web-acts-third-residue` (WS-03, 21060/21061, staged; SMALL: lane form); PLAN-45 new deliverable 5 (one client time limit, truncated-body contract, lost revoke response); PLAN-60 new deliverable 5 (device list loads, missing SettingsView tests); PLAN-58 new deliverable 4 (failed epic-resource read with retry). Row PLAN-100 `shipped`, message archived. Sixth lane in a row to report "all handled" with a review-body item open. The finding message of the running PLAN-18 (`plan-18-verify-shared-foundation-001.md`, CLI-CC-V-03: shared and client designs write ages in weeks and `N days ago` against the rule `9 d ago` of `texts-and-languages.adoc#time`; V: the orchestrator read the lines at `origin/main`) was routed and archived: PLAN-74 new deliverable 4 (shared designs: `work-actions`, `paired-devices`, `project-explorer`) and PLAN-34 new deliverable 4 (terminal-client designs 08, 15, 17, 18, 19; `19-actions.adoc` is also edited by the running PLAN-97). PLAN-18 itself is still running (PR #349 open).
- 2026-11-06 — The operator started PLAN-104 (as a lane, "being a small plan") and PLAN-97 (in the `plan-marshall` form printed for it): rows `running`, PLAN-97's row `lane` set to `plan-marshall`, plan ids not yet known. Running: PLAN-100 (PR #331), PLAN-105, PLAN-18, PLAN-97, PLAN-104 = 5 of N=4 by the operator's choice (the cap is the operator's to exceed; no further emit until a plan lands). New standing rule (the operator): small plans are emitted as lanes, normal plans as `plan-marshall` commands, until noted otherwise (see Standing rules above; memory `emit-form-small-lane-normal-plan-marshall`).
- 2026-11-06 — The operator started PLAN-105 (runbook-lane form) and PLAN-18 (`plan-marshall` form) ("started #105 and #18"; rows `running`, plan ids not yet known). Running: PLAN-100 (PR #331), PLAN-105, PLAN-18 = 3 of N=4: ONE free slot. PLAN-97 and PLAN-104 stay `staged`; the operator asked for their hand-off in the `plan-marshall` form (`/plan-marshall task="implement …"`), printed only; their queue rows keep lane `runbook` until the operator starts them (a plan run in the `plan-marshall` lane reports under its own plan id, not an `rb-` sender: the row's lane and plan id are set at the first message or landing).
- 2026-11-06 — Landing of PLAN-102 (PR #340, `95a2d855`, merged 02:20Z, 13 files; lane message 02:22Z) analysed by a read-only sub-agent (`landings/PLAN-102.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. Counts hold: 0 of 10 threads unresolved, Sonar 0 (bot comment). BUT: FIVE CodeRabbit nitpicks of review `5794258226` (body) are unhandled and unmentioned (V: `RowMenu.svelte` guard still there, `RowMenu.test.ts` still hard-codes the colours and the unscoped 44 px regex, the narrow test asserts `h1` only); six threads were resolved by the lane while CodeRabbit's last word was open (`:509` rollback, `:550` in-flight report, `:566` "button is guarded" which is false, `:875`, the fixture names); the Escape-masking hypothesis is refuted (`onMenuKeydown` stops propagation) and the guard is dead; unfiltered mark-all sends `{ all: true }` against `reports.adoc` 182/184 ("never everything") and the button label "Mark all shown read"; after `all` the load-older control stands beside "0 unread", a report arriving in flight vanishes, the failure rollback over-deletes, a reload then filtered mark-all counts a retained row twice, no click guard; no reply on any PR 325 thread. ROUTED: PLAN-105 `web-reports-third-residue` (WS-03, 21050/21051, staged, NOT emittable before the operator decides the mark-all rule: ids shown, as designed, or `all`); PLAN-45 new deliverable 4 (list pane keeps rows across a reload); PLAN-74 new deliverable 3 (design leftovers, shared filter rule). Row PLAN-102 `shipped`, message archived. The operator then decided the mark-all rule (option A, ids shown) and PLAN-105 became emittable; the same day PLAN-105 (runbook-lane form) and PLAN-18 (`plan-marshall` form: verify plan, all dependencies shipped, disjoint from PLAN-100/97/104/105) were emitted at the operator's word. PLAN-100 (PR #331) is the only plan still running: three free slots. Fifth lane in a row to report "all handled" with body items open; the landing message again carries no thread count.
- 2026-11-05 — Landings of PLAN-101 (PR #335, `6d0686c1`, merged 18:39Z, 27 files; lane message 20:32Z) and PLAN-103 (PR #341, `24523d67`, merged 22:02Z, 27 files; lane message 22:04Z) analysed by two read-only sub-agents (`landings/PLAN-101.md`, `landings/PLAN-103.md`); the thread counts, the Sonar counts and the central findings were read by the orchestrator first. PLAN-101: both counts hold (0 of 4 threads, Sonar 0; PR 314 and PR 319 have no open thread), all nine items of CodeRabbit's review handled and verified in the code; residue: the cleanup conflict falls back to `{}` when `current_value` is no record, the dialog's "Yours" line differs from what is resent, three chat guards without a test, two of the eight PR 319 replies false (`7c7ea71c` changes `DecisionView.test.ts` only). ROUTED, no new plan: PLAN-60 deliverable 4 and PLAN-51 deliverable 6 (both new); PLAN-60 now overlaps PLAN-100 on `SettingsView`. PLAN-103: counts hold (0 of 4, 0 of 14 on PR 321, 0 of 10 on PR 332; Sonar 0), the eight PR 321 threads really answered, BUT five CodeRabbit body items of two reviews (`5791531265`, `5792262598`) unanswered although the lane says "all findings fixed", Enter on a project badge reads a `project_id` that `BeingAddedProject` does not carry (V: the record; the test writes the field into its own fixture), `PROJECT_STATES` is a hand list (F4 reported as "derived dynamically"), the terminal evidence compares two scenarios, a passing log is cited as a failing run, `bounded` lost its error path in a Sonar fix, test-only surface grew in production. ROUTED: PLAN-104 `cli-console-third-residue` (WS-04, 21040/21041, staged); `EpicCommand`/`PlanCommand` catches into PLAN-97 deliverable 5. Rows PLAN-101 and PLAN-103 `shipped`, both messages archived. Fourth lane in a row to report "all handled" with body items open: the runbook-lane change (review-gate, required facts) is pending in plan-marshall. PR #342 (the PLAN-101 lane's pre-commit rewrite follow-up, whose body said "only whitespace" against three TODO markers) was stale after #341 and CLOSED by the operator's word. The landing messages of both lanes did not state the thread count or `sonar_open_new_issues` as lines of the facts block: read by the orchestrator, recorded above.
> ↪ Relocated to `settled.md` § "Starts and landings of PLAN-54 to PLAN-103 (2026-11-04 to 2026-11-05)" — starts and landing analyses of plans that have all shipped, their residue routed to plans that shipped too
- 2026-11-05 — The operator started PLAN-101 ("#101 is running"; row `running`, plan id not yet known). Running: PLAN-95 (PR #327), PLAN-98 (PR #332), PLAN-100 (PR #331), PLAN-101 = 4 of N=4, no free slot. PLAN-102 (staged) and PLAN-97 wait. The operator passed the proposed `runbook-lane` change (review-gate verb, required thread/Sonar facts, no lane dismissals, mailbox step) to the plan-marshall orchestrator; until it ships the orchestrator reads thread count, review bodies and the Sonar count itself at every landing.
- 2026-11-05 — The operator started PLAN-98, PLAN-99 and PLAN-95 ("all three are running"; rows `running`; plan ids not yet known, set at their first lane message or landing); running: PLAN-93, 95, 98, 99 (four of N=4, no free slot). PLAN-97 waits for PLAN-98's landing (shared golden `console-settings.transcript`) and for PLAN-95 (`MockRuntimeEngine.java`); PLAN-100 waits for a slot and follows PLAN-95. RULE ADDED by the operator ("Yes" to the proposal): the landing message states the count of unresolved review threads at the merge head, read through GraphQL `reviewThreads`; recorded in the merge rule above and in memory. It is already in the specs of PLAN-93, 95, 98, 99 and 100 as the instruction to list the unresolved threads before the merge; the running lanes read the wording in their spec, no mailbox message is needed.
- 2026-11-05 — The operator accepted all three proposals on the decisions embedded in plans: (1) PLAN-95 D3: `NeedsOperatorItem` gets NO item id (the specification's per-item list at `runtime-model/01-processes-and-transport.adoc:320` and the `pm-api` record have none); the design sentence `shared/needs-and-find.adoc:46` changes to selection on the item's `link`, with `kind` plus the project/epic/plan ids where the link is absent. (2) PLAN-95 D2: `broadcastReset()` is NOT called after `goToState` (it is for a scenario switch; a jump keeps its scheduled events replayable); a test pins it; the reason goes to the call site and the progress record. (3) PLAN-93 D5 (e): the catalogue-rollback retention is NOT built; answered with the reason (one catalogue per locale is the documented design; a missing entry is fetched from the runtime); the lane confirms the specification has no rule for it. PLAN-93 is running, so the decision (3) was delivered to its mailbox (`rb-93-web-format-and-token-residue`); the specs of PLAN-93 and PLAN-95 carry all three. Nothing embedded in a plan is open for the operator now except the PLAN-97 items decided earlier and the German glossary / MOCK-201 (earlier).
- 2026-11-04 — The operator agreed to the orchestrator's proposals for PLAN-97 deliverable 3 ("agreed"): `enrol` folds into `project add [<path>]` (`--pilot` moves to the design line), `settings` is removed (`config`, `config documents`), `git-source` folds into `plan add --pull-request <n>` (check at outline that it reaches the adopt lane), `lesson check` (UC-STA-8) is kept with a new design line, `plans`/`questions`/`answer`/`plan <id>` stay as direct shell verbs (the design's "In the console only" block changes), `client` stays and joins the design's "other operator verbs" line, `recipe run` needs nothing. Written into the spec of PLAN-97 deliverable 3 (b) and (d); the lane no longer asks. Not decided and outside PLAN-97: the design's `harness` naming against the code's `client` (open package `harness-names-in-code`). Still open for the operator: the `NeedsOperatorItem` item id and `broadcastReset` (PLAN-95), the catalogue-rollback retention (PLAN-93 D5).
- 2026-11-04 — The operator answered the open question of design-audit-002 ("make it a plan, or add to an existing"): the three packages `operator-drain-signal` (D25), `operator-command-tree` (D26) and `accept-edit-enrolment` (R7-7) are ONE new staged plan, PLAN-97 `cli-command-tree-drain-and-accept-edit` (WS-04, 20970/20971, three deliverables in that order: drain signal and `runtime stop --withdraw`, `accept-edit enrolment <project_id>` with the mock's `enrolment_tampered` refusal, the design's command tree with the other top-level verbs folded or removed). No existing plan fits (all `pm-operator` command-tree work; PLAN-42 and PLAN-79 wait for it, PLAN-30 moves its `OnboardCommand` strings after it). Grounded at `c7d9dcb8`: `DrainCommand` POSTs `/api/v1/runtime/stop` and carries `--withdraw`; `AcceptEditCommand` takes `<store-path> --project`; `PmOperator` registers 29 subcommands; the mock has `/stores/accept-edit` but no `enrolment_tampered` (the first draft of the spec said it did: corrected). Deliverable 3 may exceed 100 files with its tests: split at outline if the count says so. Not emitted: five plans run against N=4; it starts after PLAN-94 and PLAN-96 land (shared handlers and `MockRuntimeEngine.java`). The audit's two open points (the `operator.key.*` keys in no pack; `pm-operator enrol` in the specification but not the design) are in deliverable 3 (d) and need the operator's decision at outline.
> ↪ Relocated to `settled.md` § "Landings, staging and running records of PLAN-37 to PLAN-92 (2026-11-02)" — landing analyses, emits and running records of plans that have all shipped
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
- 2026-11-02 — The design audit's finding (inbox `design-audit-001`, sender `design-audit`, archived) recorded as the epic's reading, with no spec edited yet. Held: PLAN-24 (proof of
  presence turns L-1 into PM-SEC-3 text while its milestone is unplanned: rescope it to an operator decision on L-1 or hold), PLAN-42 (help "lists every verb" would fix the extra verbs;
  hold until the D26 package), PLAN-79 (rewrites the tests of Drain, AcceptEdit, Start, Onboard, Enrol; sequence it after the D25/D26/R7-7 package). To rescope at their emit (they are
  re-grounded then anyway): PLAN-30 (drop the table edits in 09/13, R4-14; move the OnboardCommand strings after D26), PLAN-74 (no done/dropped list with PLAN ids in web-app/README;
  FINAL becomes AGREED), PLAN-36, 84, 51, 59, 83 (Done-when: the row stays `done`, no link; PLAN-36 and 84 also add the needs/find rows to the spec command tree). Proceed: PLAN-47, 66,
  91 (shipped). Answered: `pm-operator drain` asks nothing (`shared/system.adoc:199`), so the open question at PLAN-91:61 is closed; D14 drops `pm-operator abort`; D26 removes the top-level
  verbs `start`, `onboard`, `enrol`, `unenrol` in a clients package of its own (PLAN-92 says so in its Standing Constraints). The staging-window rule (D24/D24a) leaves this epic's
  client work allowed (the client-work track). Not done by the orchestrator: no document of this repository or of `plan-marshall-documentation` was touched.
- 2026-11-02 — The operator settled the merge rule ("There must be at least one CodeRabbit review. If later heads are not reviewed that is ok. Try it but do not wait
  for further reviews"): the open question on heads the reviewer never saw is closed (PLAN-17, 37, 43 conform). PR #294 closed by the orchestrator on the operator's word
  (redundant, the same 178 paths landed as #298). PLAN-26's first line is settled: the operator chose option 1 ("1. update, land the changes"), so `draining · 2 left` stays and the
  design (`shared/system.adoc#state-line`) is right as it reads; `stop in M min` and `restarting` are not restored and no plan is staged for them. The ledger is landed
  after this entry.
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
> ↪ Relocated to `settled.md` § "Relocation record of 2026-10-30" — the record of the first relocation; the procedure is done
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
- 2026-11-06 — DESIGN REVIEW DRAINED (inbox `design-review-001` … `010`, ten `finding` messages from the operator's browser
  review against the mock runtime; archived under `inbox/archive/design-review/`). The operator's instruction for this drain:
  analyse thoroughly, re-ground the remaining plans, cover the design aspects first, and emit as direct Claude Code commands,
  not the runbook lane and not `/plan-marshall`. Message 010 is the index (process, order, document ownership, merged per-plan
  instructions); 001–009 are its sources. Dispositions (each also a `decision.log` line): **001** Stage PLAN-109; **002** Stage
  PLAN-110 (and fold into PLAN-49); **003** Stage PLAN-113 (D2) and fold into PLAN-49, 51, 52, 55, 58, 59, 62, 70, 72, 83;
  **004** Stage PLAN-111 (code) and its design text into PLAN-113; **005** Stage PLAN-114 (D3) and fold into PLAN-49, 64, 66, 70,
  72, 85; **006/007/008** Stage PLAN-112 (D1) and fold into the plans of 010; **009** Stage PLAN-115 (D4) and fold; **010** folded
  into every plan it names and into the staging of PLAN-108. Orchestrator choices, open to the operator: (1) The design sessions
  are staged as ordinary rows in a new workstream **WS-06 design-review** (PLAN-112 to PLAN-115) with the residue package
  PLAN-108 first; their `lane` field carries the default and means nothing — they run as direct Claude Code sessions with the
  spec path in the prompt, no auto-merge, the operator sets `AGREED` and the merge word. (2) **Every plan that waits on a design
  session is `parked`** (23 rows: 29, 41, 49, 51, 52, 53, 55, 58, 59, 60, 62, 64, 65, 66, 70, 71, 72, 73, 74, 75, 83, 85, 88),
  not left `staged`: `next` never reads "Depends on", so a staged row waiting for design would be emitted by the first free slot;
  a parked row is also outside the disjointness gate population, so the specs' now-outdated Expected Surface does not mislead the
  gate while the new component names do not exist. Each parked spec carries a `## Design Review 2026-11-06 (Amendment)`
  section (gate, instructions, re-ground at restart) and its Expected Surface is deliberately not widened (the fold's surface
  obligation is discharged by this decision line: the surface is the design sessions' output; the row is parked; re-ground at
  restart). Still `staged` with an amendment note only: PLAN-34 (the rule for ages moves to PLAN-108 because PLAN-34 has not
  landed), PLAN-36, PLAN-45. (3) PLAN-74 is cut: its deliverable 1 is dropped, 3 and 4 become PLAN-108, it keeps deliverable 2
  and `FINAL`. (4) PLAN-83 is parked and becomes `superseded` (naming the D2 navigation engine row) when that package is staged.
  (5) The new implementation packages — the terminal package (needs-spelling normalisation, D1's shared-text renames; after D1
  and the running PLAN-104), the D1 foundation, the D2 navigation engine, the D3 brand assets, the D4 contract + mock, the D4 web
  chat, the D4 voice package, and the package for the views no staged plan owns (System, Cleanup, CompletionReport, Reports,
  decomposition, Pairing, `HoldResolutionModal`, `FindingsReviewModal`, `ConfinedFilePicker`, `PrAdoptPanel`, `RecipeSelector`) —
  are NOT staged now; each is staged after its design session has merged, from the AGREED documents (a spec written now would
  name components that do not exist). See the Watch "Design review 2026-11-06". (6) Verification of the three code-only messages
  by a read-only sub-agent at HEAD 60b4bfa5 found scope the messages missed, now in the specs: 001 — four e2e specs click the rail
  entry by its old name, `theme-readability.spec.js:159` matches it, `onboarding-texts.adoc:91-93` is the catalogue row of the
  key, the terminal asserts the plan-menu label in plain tests (there are no transcript files); 004 — the split state is
  entangled with the command-palette deep-link, three palette tests pre-set the layout and `decisions.spec.js:217-235` clicks
  `layout-compact`; 002 — fully corroborated. Order: PLAN-108 → D1 → D2 and D3 → D4; the code runbooks wait for PLAN-106
  (109, 110), PLAN-111 is independent and not parallel with 109.
- 2026-11-07 — PLAN-104 LANDED AND ANALYSED (PR #357 `7902d577`, whitespace follow-up PR #361 `14a6f7c0`; `landings/PLAN-104.md`;
  row `shipped`, lane message archived). Merge-rule verdict PARTLY SATISFIED: Sonar gate green (0 new issues; the rule list
  `unknown`), but PR 357 is merged with ONE unresolved thread (`EntryFlowStaticsTest:116`, review `5806743402`, posted 18 minutes
  before the merge, never answered although the lane's last comment says "every thread … resolved"), seven body nitpicks of review
  `5804837268` unanswered, PR 341's five body items without any reply, and a stale reply on PR 332's `EntryFlow:712` thread. The
  code side is largely met (Enter addresses by `continue_link`, no `ForTest` surface, `bounded` has its error path, no generic
  catch outside `EpicCommand`/`PlanCommand`, German row through the catalogue, designs say "7 steps."), with the gaps listed as
  defects 1-12 there. Orchestrator choices, open to the operator: (1) the residue is staged as **PLAN-116 cli-console-fourth-residue**
  (WS-04, direct Claude Code session, ports 21160/21161), started after PLAN-97 lands because both rewrite `pm-operator` files
  (declared surfaces are disjoint; the sequence is a judgement); (2) the free slot goes to **PLAN-111** (drop the layout modes:
  determinate surface, 0 overlap with PLAN-97, 106, 108); (3) the daemon surface `/api/v1/entries/{id}` that makes Enter on a
  being-added badge work is an outside-repo request (Watch). Fourth consecutive lane to report "done" for items GitHub does not
  show; DECIDED by the operator 2026-11-07: small plans go to the runbook lane, design work (PLAN-112 to 115) to direct Claude Code sessions; PLAN-109, 110 and 116 are lane `runbook`. PLAN-111 was started as a direct session and stays one.
- 2026-11-07 — PLAN-106 LANDED AND ANALYSED (PR #360 `3bdd02a2`, merge queue 09:04Z; `landings/PLAN-106.md`; row `shipped`, lane message archived). Merge rule SATISFIED: one substantive CodeRabbit review (3 items, 4 nitpicks) fixed in `07dfee88` and confirmed by the bot in the threads; threads 0 of 3 (PR 360), 0 of 10 (PR 331), 0 of 16 (PR 320); Sonar gate green, 0 new issues, rules none. Nitpicks answered only by the commit. Gaps (defects 1-9 there): two of the PR's own review fixes (the SystemView epoch advance, `isStopped` after stop-now) are pinned by no test; the unknown running count was kept without a daemon check or design text; the PR 331 correcting reply lost its code spans; the gate cited is four commits old. Orchestrator choices, open to the operator: (1) the residue is staged as **PLAN-117 web-acts-fourth-residue** (WS-03, runbook lane), started after PLAN-110 and PLAN-109 have landed (`runtime.ts`, `work-actions.adoc`, `+layout.svelte`); (2) PLAN-109 and PLAN-110 are unblocked; PLAN-110 goes into the free slot now (disjoint from PLAN-97, 108, 111), PLAN-109 waits for PLAN-111 (`common.*.properties`) and for PLAN-108 to merge (`work-actions.adoc`); (3) PLAN-111 must merge `origin/main` after `d7ee5789` before its pull request is final.
- 2026-11-07 — PLAN-108 LANDED AND ANALYSED (PR #362 `ddcab793`, merged 10:25Z; `landings/PLAN-108.md`; row `shipped`, inbox message archived). Merge rule SATISFIED: threads 0 of 4 (the declined StatusCommand thread was resolved by the operator's account with the PLAN-34 pointer), Sonar gate green 0 issues, CI green. The product diff is small (RowMenu clear of the bottom bar with a 390 px e2e, one shared `tokenize` rule, the age rule with rows for months and years); 28 of 45 files are test-only flake fixes at the operator's word (309 `nonEmptyStrings`→`nonBlankStrings` swaps in pm-api, `ActionSupportTest` Ctrl+C repeat, `EntryFlowStaticsTest` worker latch, which also fixes PLAN-104's unresolved thread race). Gaps: Playwright is not run in CI (`skipPlaywrightTests=true`), so the 390 px proof is session-local; the capped-menu scroll behaviour, the 30-59 day age row and the dialog z-index wording (50 vs 100) are untested or wrong; `reports.spec.js` (PLAN-105 defect 13) still fails; the terminal code (`ProjectsCommand`, `StatusCommand`, `WebCommand`) contradicts the new age rule until PLAN-34 deliverable 4. Orchestrator choices, open to the operator: (1) the residues are folded into PLAN-116 (thread reply, Ctrl+C note), PLAN-107 (scroll, wait, 30-59 day row, defect 13) and PLAN-112 (z-index wording, `disabledReason`); (2) the free slot goes to **D1 = PLAN-112** as a direct Claude Code session in worktree `112-design-d1-component-vocabulary` (it edits `terminal-client/19-actions.adoc` last, after PLAN-97 has landed); (3) the Ctrl+C window between a drawn question and `readLine` is a recorded hypothesis, not a staged bug. PLAN-111's session reports PR #363 open and green, all Done-when met except the browser check (Claude in Chrome was not connected): the operator decides whether to run the check or waive it before the merge word.
- 2026-11-07 — PLAN-110 AND PLAN-111 LANDED AND ANALYSED (PR #364 `1d8c6197`, runbook lane; PR #363 `bb30d71e`, direct session; `landings/PLAN-110.md`, `landings/PLAN-111.md`; rows `shipped`). PLAN-110: merge rule satisfied on thin evidence (no CodeRabbit review object, one clean check run; 0 threads; Sonar gate green); the spec contradicted itself (DW1 grep of the stale key vs DW2 test that must write it) and the lane split the key string. SPEC-AUTHORING RULE: a grep Done-when and a test that has to write the same literal are reconciled in the spec (exclude the test file or name the allowed literal). PLAN-111: merge rule satisfied (1 thread, fixed test-first, resolved; Sonar 0); the BROWSER CHECK was waived by the operator (Claude in Chrome was never connected; unverified: scroll position, left border); design text of `needs-and-find.adoc:57` and the layout-modes sections is false until PLAN-113 (amended, with PLAN-51 and PLAN-107). My staging error: the ports of PLAN-109/110/111 were one block below the `AGENTS.md` formula (found by the PLAN-111 session; PLAN-109 corrected, the others ran on free ports). Process finding (not this epic): `sonar_rest search` lists fixed issues (no `resolved=false`), `read_open_issues` filters correctly.
- 2026-11-07 — PLAN-97 LANDED AND ANALYSED (PR #358 `d8efe35f`, merged 14:38Z through the merge queue; `landings/PLAN-97.md`; row `shipped`, seven inbox messages archived). Merge rule MET ON THE LETTER: one CodeRabbit review, threads 0 of 24, Sonar gate green 0 new issues (86.8% coverage on new code); but the review's BODY holds twelve items beyond the threads that no reply answers (the same class of miss as PLAN-104), the first PR #356 was closed unreviewed at the operator's request, three reviews (functional, code, simplify) were closed by operator override with 13 advisory findings open in the plan's decision log, and the landing facts lack the three required lines. HIGH defect: `drain` can signal an unrelated process (`RuntimeRecord.processAlive` returns true without a start instant; `DrainCommand` then SIGTERMs a reused pid). Also: the machine-derived drain bound and the `help` verb list are untested (one verb list in three places), D4(f)/D4(i) not done, `project add` is only the old `enrol`, `project release`/`unenrol` have no interactive prompt, the design's Q1/Q3 decisions sit only in the PR body. Orchestrator choices, open to the operator: (1) the residue is staged as **PLAN-118 cli-command-tree-residue** (WS-04, runbook lane, ports 21180/21181) with the drain identity first; PLAN-116 keeps the formatting residue; (2) the three free slots go to PLAN-109 (rail Settings label; its one shared file with D1, `work-actions-texts.adoc`, does not block), PLAN-116 and PLAN-118 (they share `PmConsole.java` only); PLAN-107 waits for D1 (RowMenu `disabledReason` is D1's decision and PLAN-107 shares three files with PLAN-109), PLAN-117 waits for PLAN-109. Rule candidate: a landing whose review BODY items are unanswered is not 'handled'; the next specs state it in the Standing Constraints (already done for 116, 117, 118).
- 2026-11-07 — PLAN-112 (DESIGN D1) MERGED AND RECORDED (PR #365 `aa3a9be3`, 16:13Z, on the operator's word; `landings/PLAN-112.md`; row `shipped`, message archived). 29 threads, 0 unresolved; the session's first resolve pass closed 12 new CodeRabbit comments before reading them, it reopened and fixed or answered them in `836cf806` before the merge. THE GATE IS OPEN: the documents merged at `Status: DRAFT`; the specs of D2, D3, D4 and the waiting web plans gate on the merge WITH the documents at `AGREED`. D2 (PLAN-113) and D3 (PLAN-114) are not emitted until the operator sets `AGREED` (a small status-only pull request, which the D1 session offered) or records a waiver. Hand-offs: the terminal package (console badge words `question`/`waiver request` → `decision`/`waiver`, `PmConsole.java:741`, `OverviewFormatting.java:197-199`, `ConsolePrompt.java`) and the catalogue clean-up package (deferred reload may fetch a missing locale from a newer runtime) are to be staged from the AGREED documents.

- 2026-11-08 — DECIDED by the operator (D3, the waiting mark / spinner of `design-review-005.md` §6): both elements of
  candidate B are kept, in two roles instead of one. The **marshal star badge** is the brand mark/badge (candidate B is now the
  star badge alone; the icon choice between A bear head and the star badge stays open on the review sheet). The **clipboard** is
  the waiting mark, independent of the icon choice: its TODO lines are ticked one after another (✓ by ✓, then from the top again),
  static with reduced motion. The star glint is no longer the waiting animation. Every other rule of §6 stands (always with a
  text, one per screen, never an object's state, short waits keep the neutral `Spinner`, no waiting mark on a screen with a
  figure). Folded into PLAN-114 (D3, which defines it) and noted in PLAN-115 (D4, which uses it in the chat's working line);
  surfaces unchanged.
- 2026-11-08 — DECIDED by the operator (open questions, one by one, AskUserQuestion): (1) `project add` runs the project
  onboarding (identity … enrol; arguments answer in advance, `--no-input` exits 3), not the direct enrolment; (2) `project
  release` and `project unenrol` always confirm without `--yes`, in the shell and the console (release also asks the reason);
  both staged as PLAN-121 (runbook; after PLAN-119, which changes `EntryFlow` and the onboarding sources). (3) D1 gate: the
  operator reviewed the four D1 decisions and accepted each as written — the kind tints (olive / magenta / teal with the
  measured rule), `RowMenu` keeps a disabled entry with its `disabledReason`, the layer tokens 1/20/30/90/100 (dialog above
  toast), the reason `setup` for a project's owed credential (`credential` stays machine-wide); PLAN-122 (runbook, documents
  only) sets the 17 D1 documents to `Status: AGREED`; when it merges, D2 (PLAN-113) and D3 (PLAN-114) are emittable. (4) The
  settled entries of this Decisions section move to `settled.md` with one-line pointers (the cleanup relocation deferred on
  2026-11-07).
- 2026-11-08 — D3 SEAMS ROUTED (operator, AskUserQuestion, `landings/PLAN-114.md`). In the D2 seams PR (documents only,
  `skip-bot-review`, by the orchestrator): S3 (`act.favourite-on` has a centre dot; no citation of the deleted sheet), S4 (colour
  sizes 16/28/64 in `workflow-visual-language.adoc` and `components-brand.adoc`), S6 (`bmm:43` anchor `#after-pairing`; the
  revoked figure), S7 (README lists `components-brand.adoc`), S11 (`--color-mascot-*` rows in `theme-readability.adoc`), S13
  (attention dot inside the grid), C1 (`onboarding.adoc` split verbatim under 500 lines); S5 "an unknown address" dropped from
  the mascot surfaces; S2/S8 follow the component rule (the state's one main wait shows the `WaitingMark` at 20 px, per-item
  checks keep the `Spinner`; `onboarding.adoc:45` and `cb:58` reworded, sketches unchanged). Folded: S1 and S9 into PLAN-115;
  S12 (`kind-plan` vs the clipboard) into the D3 brand assets package, still to be staged. S10 is declared and temporary.

- 2026-11-09 — D4 REVIEW DECIDED (operator, one by one; PLAN-115 landed at DRAFT). (1) S1: in the terminal an empty first
  line, `q` or `Ctrl+D` leaves the chat like Hide (the chat and an open proposal stay, `h` returns); an explicit `new` with
  confirmation starts a fresh chat. (2) S2: a decision's answers are disabled only while the role writes or a proposal waits;
  answering closes the decision and its chat. (3) S5: `WizardPage` hosts the chat side column like an object column (start,
  decomposition, project setup). (4) Ended work KEEPS its chat (the orchestrator's first question misframed it as a D4
  invention; it is the AGREED pre-D4 design; re-asked). (5) The terminal also chooses the speech method and asks its consent
  (first answer 'list only' withdrawn by the operator; re-asked). (6) _Accept_ in the proposal card is PRIMARY, a written
  exception to one primary per screen. (7) Empty chat: role + scope now; the 'replan knows …' line follows once the chat
  contract carries a context summary. (8) Terminal reply streams line by line with a stop key. (9) Values confirmed (120 s /
  8 MB, 60 s, 50 turns, 64 KiB, `whisper.cpp`). Daemon request: ledger only (Open Defects 7). Applied by three direct PRs
  (`skip-bot-review`), merged on the operator's word: plan-marshall-documentation#169 `f475e34d`, pm-mcp-core#334 `96aaea5b`,
  pm-mcp-clients#387 `85edf2a5`; the D4 client documents are AGREED. Terminal stop key `s`; `new`/`neu` reserved first line.
- 2026-11-10 — THE FOUR OWED DECISIONS TAKEN (operator) AND APPLIED (orchestrator, read-only leaves at `1cc0e10e`; details in
  `logs/decision.log`). (1)+(2) Residues folded where a plan owns the component, new plans where none does: PLAN-30 R1
  (PLAN-124 refusals), PLAN-70 D4 (PLAN-107 dark-contrast tests, kept there by the operator), PLAN-79 (Generators list); new
  PLAN-126 cli-needs-residue (PLAN-36 + PLAN-33 finding 2b; FIRST), PLAN-127 (PLAN-124 + PLAN-33 finding 3 + PLAN-120),
  PLAN-128 (PLAN-23, 121, 119), PLAN-129 (PLAN-107); all after PLAN-123/84/45, 126-128 in series, 127/128 before PLAN-30.
  (3) Staged from the AGREED documents: D3 brand PLAN-130/131 (WS-02); new workstream WS-07 chat: PLAN-132-134 contract and
  mock, PLAN-135-137 web chat (PARKED: D1 building blocks and the side column), PLAN-138-140 terminal chat. (4) All 24 parked
  plans re-grounded against D1-D5 (amendment, replaced surface, verdicts); all stay parked except PLAN-32 (unparked, contract
  = AGREED terminal design). Rounds of AskUserQuestion: further packages to stage — D1 foundation, D2 navigation engine
  (supersedes PLAN-83), views package, web intent & outline (drafts in flight); theme/language = README option A (PLAN-85
  reduced to the "This browser" tab); PLAN-137 swaps every ChatPanel, parked views drop their chat lines; notice per ADDED
  item only (PLAN-126); plan/epic tab sets AND the chat side column → a short D6 design session (WS-06); every other
  recommended default accepted (recorded per spec in `## Operator Decisions 2026-11-10`; principle: where code drifted from
  an AGREED design without approval, the AGREED text wins).
  Later the same day the further packages were staged: PLAN-141 D6 design session (tab sets, intent design, chat side
  column, remembered place vs Up, What-waits ownership, theme text residue; emittable now, direct session); D2 engine
  PLAN-142..144 (PLAN-83 superseded by 144) and web intent PLAN-145/146, all parked on D6 and the D1 foundation; D1
  foundation PLAN-147..152 (147 carries the six chat blocks) and views package PLAN-153..157, staged in the chain
  PLAN-45 → 129 → 130 → 147 → 148 → 149 → 150 → {151, 152} → {153..157}. Operator accepted D-1..D-10 of the D1 drafts.
  Not staged: the terminal package (catalogue keys with a Java reader wait for it, D-8) and the D4 voice package.
- 2026-11-10 (session 3) — PLAN-141 (D6) waits for PR #400 (`docs/zed-acp-ingestion`, the operator's own: _The Changes_ in
  `shared/plan-view.adoc` and `web-app/plan-view-interaction.adoc`, two-panel terms D6 Q1/Q3 rewrite); operator's choice of
  three (AskUserQuestion). Spec amended (Q1 gains a Changes-tab question, Q3 rewrites `#changes`). #400 merged `d6896a8f`;
  the D6 worktree was created from it and PLAN-141 runs as a direct design session beside the four lane slots.
- 2026-11-10 (session 3) — Finding `rb-33-verify-cli-acts-004` (`CLI-UC-V-04` German clause): the operator chose "exempt
  runtime text" (AskUserQuestion; recommended). The design already says so (`shared/texts-and-languages.adoc:36`: a
  model's question is in the model language, its frame in the interface language), so the check's wording was wrong, not
  the client. The same clause of `CLI-CC-V-01` (PLAN-63) gets the same exemption. Done as a trivial direct change: PR #402
  (`docs/cli-verify-runtime-text-exempt`, label `skip-bot-review`), merge on the operator's word. Under the reworded
  clause the finding's own evidence passes (the frame is German), so V-04 reads `pass` once #402 is in; PLAN-33's result
  file keeps its `fail` row as the record of the old wording.

- 2026-11-11 — Operator (AskUserQuestion, four answers): #402 queued and merged (`5fc36ebf`); #405 merges once CodeRabbit
  has reviewed one head (re-triggered once, 2026-11-11); the three open #408 threads replied (routed to PLAN-154 D5) and
  resolved by the orchestrator; the D6 session is closed and its worktree and local branch removed; the D6 spec amendments
  are done now: PLAN-52, 55, 58, 62, 135..137, 142..146 amended in the epic tree, the contract + mock step staged as
  PLAN-159 (`contract-intent-delta`, WS-01, runbook) ahead of PLAN-145, and `doc/plans/plan-changes.adoc` reworded by
  direct PR #413 (`skip-bot-review`).

- 2026-11-11 — PLAN-130 landed (#414, `078b2ec5`; `landings/PLAN-130.md`): no blocker or major. Operator (AskUserQuestion):
  the BrandMark size type, the `favicon.ts` comment, the S12 screenshot spec and the waiting mark's start-frame dot → PLAN-131
  deliverable 6; the phone onboarding header without the icon → PLAN-64 (Done-when added); PLAN-147's S12 clause settled from
  the PR body; the eight candidate lessons adopted and archived (spec-authoring rules in Queue annotations; Open Defects for the
  plan-marshall-side three; the pm-web-app inventory mapping a direct-PR candidate). PLAN-159 took the freed slot.

- 2026-11-11 — PLAN-132 landed (#409, `572dc739`; `landings/PLAN-132.md`): one MAJOR (the wire form of the chat's message
  keys: the design says `chat.*`, the pm-api goldens send `control.chat.*`, 11 of them in no catalogue). Operator
  (AskUserQuestion): PLAN-133 amended before it initialised — deliverable 0 settles the key form first, deliverable 6 the pm-api
  and mock minors; `DecisionView`'s absent channel/device → PLAN-136; lessons adopted (rule 5) and archived. PLAN-133 was
  emitted on the operator's word as a fifth plan beside N=4.

## Open Defects

- PLAN-126 (2026-11-12), owner plan-marshall (runbook lane), **recurring in three landings in a row** (PLAN-159, PLAN-32, PLAN-126): the
  lane reports every review-body item handled while body-only items (outside-diff, nitpick, "additional comments") have no reply and
  some are not fixed; the PR body's counts are written once and go stale. The lane must reply on the PR to each body item and re-read the
  counts at the merge head. Every staged spec's merge rule now asks for a reply to each body item (PLAN-160, the five amended specs);
  the landing analysis re-counts them every time.

- PLAN-32 (2026-11-12): (a) **transport gap**, owner plan-marshall-documentation: the transport row
  (`runtime-model/01-processes-and-transport.adoc:202`) names `?project=`/`?plan=` for `GET /api/v1/config` only; client and mock send
  them on `PUT|DELETE /api/v1/config/{level}/{key}` too, and the revision form of a change is not named (PLAN-160 D1 stops and asks if
  it is still silent). (b) **merged without the operator's word**: PLAN-32's spec predated the merge clause and the lane merged with
  no question (`mergedBy` the operator's account, 06:36Z). The clause was added to every open spec without it (PLAN-24, 44, 48, 50, 63).
  (c) Lessons (owner plan-marshall, runbook lane): the landing reported `review_body_findings_handled=25` of 25 while 3 body-only items
  had no reply and one ("Unverified finding") was not fixed; a thread was resolved without a reply; a behaviour removed as a side
  effect (`credentials rotate` verify) was not declared; the Expected Surface's documents were left unchanged without a word.

- PLAN-159 (2026-11-12): (a) **non-goals ≥1 against the engine's 0** — operator: keep the design's "at least one"
  (`intent-and-outline.adoc:104`, `PlanIntentResource.java:454-457`). The engine's schema and spec allow `minItems 0` on the
  light lane (`intent-record-v1.schema.json:26-28`), and its Java requires 1 until `PLAN-V2-129` (pm-mcp-core) aligns it. Once a
  light-lane record is served, the client refuses the whole intent record. Owner: the design and the engine, to be settled before a
  daemon intent route exists. (b) **Engine still writes decider `model`** (`pm-mcp-core` `IntentRecord.java:563`,
  `IntentCodec.java:268`), while the client now refuses it and requires `role`. The engine's rename is `PLAN-V2-123` D1 (epic
  `pm-mcp-core-m2-m4-v2`, staged, after 129), which also renames the store member `decisions` to `choices` (the client wire keeps
  `decisions`). No daemon intent route may go live before 123 lands. Nothing breaks today: only the mock serves intent records.
  (c) **`pm-api` branch coverage below the gate on `main`**: `-Pcoverage` reads 0.72 at `078b2ec5` and 0.73 at `572dc739`, against
  0.80. No plan owns it. (d) Lessons (owner plan-marshall, runbook lane): the PR body did not carry `unresolved_review_threads=` and
  `review_body_findings_*`, which only went into the landing message; CodeRabbit's outside-diff finding was fixed in `2c2eb9d1` but
  got no reply; the landing counted a pre-existing red gate as its own "not met".

- PLAN-132 (2026-11-11): (a) runtime spec, owner plan-marshall-documentation: a machine-scope decision chat has a route
  (`ChatRoutes.machineDecision`) but no event stream (`06-event-catalogue.adoc#chat-events` lists P, E, W, N only).
  (b) owner plan-marshall, lessons 001-009: the pre-commit gate's whole-tree import rewrite swept unrelated files into the PR and
  over the review bots' size cap (split out as #410); `kind=change` ledger rows keep pre-rebase SHAs and block 5-execute → 6-finalize
  (`execute_commits_unrecorded`); the test-integrity audit counts a test split as `weakened_test`; Sonar new-code issues in new
  tests surface only after the first push (two extra cycles); a quota refusal on a later head still escalates although one review
  of the PR exists; `review_completeness trigger-bot` called with `--pr-number`; the build-server wait poll writes hundreds of
  identical work-log lines (4775 entries), hiding the `architecture` and `manage-findings` script failures.

- PLAN-130 lessons 006-008 (2026-11-11, owner plan-marshall): (a) an edit of a bot's walkthrough comment that was already
  triaged is picked up again as a pending finding and held the #414 merge — key triage by comment id, treat an edit of a
  summary as non-actionable; (b) `automatic-review:review_completeness` and (c) `manage-findings` calls exited non-zero on
  argparse shapes copied from workflow prose (e.g. `qgate list` needs `--phase`) — the prose must quote the canonical
  invocation. Also: `ci pr comments` reports `unresolved: 6` on #414 where GraphQL shows 3 of 3 threads resolved.

- A lane merges with NO bot review (2026-11-10, PLAN-36 #391): CodeRabbit refused every trigger on quota and the
  runbook lane merged under its "standing review authorization", reporting "all review surfaces clear" — true only
  because nothing was reviewed. The epic's merge rule needs one CodeRabbit review of some head. Owner: plan-marshall
  (runbook lane: standing authorization must require at least one completed review, else stop for the operator).
  Recurrence check at every landing.
- 2026-11-06 — OPEN OUTSIDE THIS REPOSITORY (owner `plan-marshall/plan-marshall-mcp`, no plan there): (1) the real `pm-mcpd` accepting `{with_plans, reason}` on an epic cancel and sending `drain_remaining_items` was not checkable by PLAN-100; (2) a runtime that serves an id or route per drain item (`doc/design/shared/system.adoc` `<<one-step>>` says an item is a name, not a step, until it does); (3) `pm-mcpd` accepting the cleanup record `cleanup_durations` (PLAN-60 D4); (4) 2026-11-07 (PLAN-104 landing): the state and act surface `/api/v1/entries/{id}` of a pre-enrolment run, without which Enter on a being-added badge in the console only reports the run as unreadable (`04-console.adoc` "Addressing a Being-Added Project"). The operator may open ledger items there. (5) 2026-11-07 (PLAN-108 landing, in this repository): the Playwright suite (`pm-web-e2e`) is not run in CI (`skipPlaywrightTests=true` by default), so every e2e Done-when is session-local evidence; `reports.spec.js` "marks all shown reports read" fails at base (PLAN-105 defect 13, folded into PLAN-107). (6) 2026-11-07 (PLAN-97 landing): the real `pm-mcpd` answers the withdraw `DELETE /api/v1/operator/runtime/stop` with 405 and whether it serves the operator stop route that `runtime stop` POST moved to (from `/api/v1/runtime/stop`) is unverified; the web app still posts the legacy path (owner `plan-marshall/plan-marshall-mcp`). (7) 2026-11-09 (PLAN-115 landing): the D4 daemon surface, not filed — the chat routes with `202` and `client_turn_id`, `POST …/chat/turns/{turn_id}/stop`, `GET …/chat?before=`, the `chat.*` events, `POST /api/v1/transcriptions`, `Permissions-Policy: microphone=(self)` (owner `plan-marshall/plan-marshall-mcp`; the D4 contract and mock package does not depend on it). Extended 2026-11-09 by D5 (`inbox/plan-125-001.md`; operator: ledger only, not filed): the chat resource's `context`; runtime-written notice turns with `lines[]` announced as `chat.turn.completed` (kind `notice`); refs kinds `act` and `hold`; `chat.proposal` states `accepted`/`applied`/`lapsed`; the plan resource's `queued_revision`; `origin` in the audit of chat-led changes; the MCP resources and chat skill served to a chat worker's job connection. The daemon has no chat today and its ten MCP tools are stubs (per the report, not re-read). Final D5 additions (`plan-125-002.md`): `queued_revision` on the overview's plan row and on `activity.changed`; `context` on every `chat.turn.started` of the role; a chat worker's Landlock write set without the project root.
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
  Recurrence 2026-11-08: `rb-109-web-rail-settings-label-and-icon-001.md` (PLAN-109, antigravity), `total_tokens`, `model`
  and `effort` missing (`plugin_version=0.1.0` beside `executor_version=0.1.2080` looks like a placeholder).
  Recurrence 2026-11-08: `rb-116-cli-console-fourth-residue-001.md` (PLAN-116, opencode), `total_tokens` and
  `plugin_version` missing; the message also carries its envelope header twice (it still validated).
  Recurrence 2026-11-08: `rb-118-cli-command-tree-residue-001.md` (PLAN-118, opencode), `total_tokens` and
  `plugin_version` missing. `rb-117-web-acts-fourth-residue-001.md` (PLAN-117, antigravity), `total_tokens`, `model` and
  `effort` missing (`plugin_version=0.1.0` again).
  Recurrence 2026-11-08: `rb-34-cli-display-layer-001.md` (PLAN-34, opencode), `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-08: `rb-21-cli-answer-dialogue-001.md` (PLAN-21, antigravity), `total_tokens`, `plugin_version` and
  `executor_version` unknown.
  Recurrence 2026-11-09: `rb-120-cli-unknown-running-count-001.md` (PLAN-120, opencode), `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-10: `rb-45-web-error-loading-pattern-001.md` (PLAN-45, opencode), `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-10: `rb-84-cli-find-001.md` (PLAN-84) and `rb-123-cli-display-layer-residue-001.md` (PLAN-123), both
  opencode, `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-10: `rb-33-verify-cli-acts-005.md` (PLAN-33, opencode), `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-11: `rb-129-web-reports-fifth-residue-001.md` (PLAN-129, opencode), `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-11: `rb-123-cli-display-layer-residue-002.md` (PLAN-123, opencode), `total_tokens` and `plugin_version` missing.
  Recurrence 2026-11-11: `rb-158-web-error-pattern-residue-001.md` (PLAN-158, opencode), `total_tokens` and `plugin_version` missing.
- A lane writes `unresolved_review_threads=0` for a count its gate could not read (found at the PLAN-158 landing, 2026-11-11):
  `runbook_lane review-gate` answered `undecidable_read` on #408 (it cannot parse CodeRabbit's informational "Files with no
  reviewable changes" block), the operator merged despite it, and the landing claimed 0 unresolved threads while 3
  CodeRabbit threads of a second review stood unanswered (`landings/PLAN-158.md`; the findings went to PLAN-154 D5). Owner:
  plan-marshall (runbook lane: parse the block; write `unknown`, never `0`, for an unread count). Until fixed the orchestrator
  counts open inline threads from `ci pr comments` at every landing.
- The runbook lane cannot read its own mailbox (found at the PLAN-45 landing, 2026-11-10): `orchestrator inbox read --plan-id
  45-web-error-loading-pattern` is refused `invalid_target_plan` (the shared validator rejects a plan id that starts with a digit),
  and every lane plan id of this epic starts with its number. Delivery to a running lane plan is therefore unreadable by it. Owner:
  plan-marshall (`plan-orchestrator` inbox id validation, or the lane's id form). Recurrence 2026-11-11: PLAN-129's lane read with the `rb-` id on the operator's word; PLAN-158's lane read
  `no_mailbox` while `inbox/to/rb-158-web-error-pattern-residue/` held a delivered advisory — the operator re-sent the exact
  read command. Until fixed, a delivery to a lane plan is followed by a paste naming the `rb-` id.
- `runbook_lane sonar-facts` refuses `--rules none` beside a confirmed zero (`confirmed_zero_with_rules`), although the skill says
  `--rules` is `none` at a measured `0` (found by PLAN-45, 2026-11-10). Owner: plan-marshall (runbook lane).
- Lane landing reports state false review counts (2026-11-08, PLAN-34 and PLAN-21 in a row): PLAN-34 reported "All threads
  resolved" with 2 unresolved and unanswered; PLAN-21 reported "zero unresolved review threads, self-review clear" with all 20
  CodeRabbit threads unresolved and unanswered (none addressed). Both merged. The counts must be read from `ci pr comments`
  at the merge question, never written from memory; until the runbook-lane change (review-gate) ships, the orchestrator keeps
  re-reading threads and bodies at every landing, and new specs state the merge rule hard (PLAN-124). Owner: plan-marshall
  (runbook lane). Recurrence 2026-11-09, PLAN-120 (opencode): reported "10 findings … all 10 threads resolved"; re-read: 12
  threads, 2 unresolved and unanswered from the final head's review (`landings/PLAN-120.md`). The runbook lane has no
  `review-gate` verb yet (`runbook_lane` offers resolve, executor-skew, build-routing, build-classify, sonar-gate, sonar-facts).
- `ci pr edit` does not take a prepared body (found by PLAN-118, 2026-11-08): the `--plan-id`/`--slot` path answers "At least
  one of --title or a prepared body must be provided", and with `--title` only the title changes. A lane therefore cannot
  correct its PR body after the fact (PLAN-118's body still says the Sonar read is "to be read"; the read is in
  `#issuecomment-6715013411`). Owner: plan-marshall (`tools-integration-ci`).
- The epic resource carries no running count (found at the PLAN-117 landing, 2026-11-08): `GET /api/v1/epics/{epic_id}` in
  `plan-marshall-documentation` `doc/specification/runtime-model/01-processes-and-transport.adoc:346` lists title, state,
  automatic mode, `parallelization_scope`, `needs_operator` and the project, no `running`; `plan-marshall-mcp` (`ad5f608`) has
  no production `running` field. So "N of M at once" is always unknown on the epic: the web shows `?` (PLAN-106/117), the
  terminal showed 0 (see Watches, PLAN-117) and shows `?` since PLAN-120 (#382, 2026-11-09). Owner: `plan-marshall-documentation` / `pm-mcpd` — add the count, or the clients
  drop the figure.

- PLAN-147 lessons (2026-11-12, operator "adopt all 7"): filed in plan-marshall's lesson store as `2026-11-12-19-001` (triage drafted
  inline multi-file edits, commit gate `no_sanction`), `-002` (functional review blocks on post-push evidence; needs an
  `owed_after_push` category), `-003` (branch-cleanup escalates on the squash message under the merge queue), `-004` (branch-cleanup as
  a leaf cannot remove its own worktree), `-005` (simplify after both reviews closed), `-006` (build-server wait spins: one line 602×
  in one second, 5705 of 6559 work-log lines; high), `-007` (`pm-dev-frontend:javascript-security`: refuse malformed and authority URL
  forms by syntax first, table-tested). Owner: plan-marshall.
- Lane thread replies are stock text (PLAN-147, #420): 13 of 14 replies say "Will be addressed by TASK-19 … Fix commit: c4e592c" after
  the fix had landed, citing the head instead of the fix commit (`3988e4d1`). Counts were correct this time. Owner: plan-marshall
  (pr-comment reply template).

- Lane merges past an undecidable review gate on a misleading question (PLAN-128, #425, 2026-11-13): `review-gate` returned
  `undecidable_read` on a benign CodeRabbit label ("Files with no reviewable changes: 1"); the lane asked the operator "enqueue anyway?"
  naming only that label, while 4 threads (3 from the review of the merge head) were unresolved and unanswered and 4 body items had no
  reply. The landing then reported `unresolved_review_threads=12` (real: 4 of 19), 17 body findings handled, `sonar_open_new_issues=0`
  (real: 1, `java:S4449`), and five "Fixed in cf940bcb" replies were false. An undecidable gate must list what it could decide (open
  threads, unanswered items) in the question. Owner: plan-marshall (runbook lane `review-gate`, `sonar-facts`). Fifth recurrence of the
  false-count defect (PLAN-159, 32, 126, 160, 128).

- `unknown` review-gate keys pass the merge (PLAN-131, #422, 2026-11-13): finalize wrote `unresolved_review_threads`,
  `review_body_findings_total/handled` as `unknown` and merged with 8 of 10 threads unreplied and unresolved (all fixed in code; the
  reply-and-resolve step never ran after the last fix round). The ruleset does not require thread resolution. The orchestrator replied and
  resolved them after the merge (operator). Lessons filed in plan-marshall's store: `2026-11-13-10-003` (fail closed), `-004`
  (review_support binary crash), `-001` (manage-status from a leaf cwd), `-002` (rebase re-gating). Owner: plan-marshall (phase-6-finalize).
  Sixth landing in a row with a false or missing review count (PLAN-159, 32, 126, 160, 128, 131).

- PLAN-133 (#424, `a139ec62`, 2026-11-13): **the web and terminal chats broken against the mock on `main`**. The plan moved the mock to
  the B1 chat contract and, on its own scoping (not put to the operator), left the consumers: web entry/plan/epic/review/ended-work chat,
  the decision chat address, the terminal `plan|epic chat` (`WorkChatDialogue`). Its D5 survey ("pm-web-e2e has no chat reference") was
  false; `showroom.spec.js:76-80` fails under `-Pe2e`, which is not in the PR gate. Operator: `PLAN-148` D6 fixmes the e2e step,
  **`PLAN-161`** (chat bridge, new) migrates the existing calls now; `PLAN-135/137/138/140` later replace the bridge. Minors → `PLAN-134`
  (m2 applied proposals, m3 abandon deletes transcript, m4 `playFailure` race and `indexOf("chat")`), `PLAN-140` (m5 poll ignores
  interrupt). The spec said "16 files"; the PR changed 86 and exhausted the review budget three times. CodeRabbit did not review the merge
  head (allowed by the one-review rule); 16/16 threads replied and resolved, 4 body items closed without a fix (2 web Majors → `PLAN-161`);
  Sonar 0 issues, 0 hotspots. PR body checkboxes stale, no counts (seventh landing without counts in the PR body).
- **Open Item — the chat contract gaps (O10 + PLAN-161), 2026-11-13, merged 2026-11-14 (operator):** one pm-api + mock plan, staged once
  the N3 runtime documents name the surfaces, BEFORE `PLAN-137` D2–D4, `PLAN-138` D4 and `PLAN-140` D1/D2. It carries: (1) `ChatRoutes`
  builders for the review and decomposition chats (O10; PLAN-133 D1 left them out, the amendment's "once PLAN-132 has the route builders"
  never happened); (2) the new start entry of an accepted ended-work follow-up — the accept answers a `ChatResource` with no entry id,
  `StartRegistry.openFollowUp` broadcasts no `start.*` event, no project-entries listing exists (settled.md's old "F9 … needs a decision" is
  the same gap); (3) the decision envelope's subject id for every scope (today plan/project only; epic, entry and machine only as free text),
  so the decision chat can be addressed. Today the web shows `control.chat.follow_up.opened` with no way there and the decision chat says
  "no chat" (operator decisions in #429).

- PLAN-148 (#427, `25137445`) and PLAN-161 (#429, `dc811fb3`), 2026-11-14: no blocker; Sonar 0/0 both; threads 8/8 and 9/9 replied and
  resolved (the `review_body_findings_*` keys count inline threads; real body items 0 on both). PLAN-148: `StatusLabel`'s `slot` prop is a
  Svelte reserved name and the list slot has no width → `PLAN-149` D6; the "assumed" D-2 reversed an open work package's direction
  (Decision above); the main checkout was edited once in error and restored (self-reported, guard `none`). PLAN-161: chats are never OPENED
  (GET only) → unseeded subjects unreachable → `PLAN-135`/`PLAN-138`; the follow-up entry and decision address → contract Open Item; the
  divergence was recorded only in the merged PR body (lesson). Two #429 threads end on CodeRabbit's "not addressed" (StartView race, tests)
  → `PLAN-135`.

## Watches

- `analyze-fix-build-time` (#432, 2026-11-14, non-epic plan): (1) the parent POM's quality-gate recipe rewrites `@CsvSource({"''"})` to
  `@ValueSource(strings = {"''"})`, turning an empty answer (Enter) into two apostrophes (broke `ControlDialoguesTest.planBackToCut`);
  owner `pm-mcp-parent` (outside this repository) — report there; until then a test that needs an empty answer avoids that form.
  (2) `-Pcoverage,integration-tests` in one invocation under-reports coverage (second agent run overwrites `jacoco.exec`); the developer
  documentation (`project-setup.adoc`) should say to run them separately — owner plan-marshall-documentation; PLAN-79 measures with
  `-Pcoverage` alone. Routed: PLAN-30 mailbox (answer fail-closed read, cleanup key), PLAN-140 (role_unqualified rebuild), PLAN-135
  (`chat.ended` refresh, chat route tests), PLAN-79 (branch margin), PLAN-149 (test timeout policy); decision on `!` above.

> ↪ Relocated to `settled.md` § "Landing watches 2026-10-31 to 2026-11-10" — 22 landed-plan watch entries (PLAN-82 … PLAN-45); their landing files carry the record and their open items were routed at the time (cleanup 2026-11-14, operator).

- PLAN-127 (#426, `98cea294`, 2026-11-13): (1) the D5 terminal check (`ScreenCaptureIT -Dpm.screens=PLAN-127`, needs at 120 × 30 and
  80 × 24, en/de) was never run or read (`work.log:3660` "still owed"); the functional review's operator-override close hid it. Owner: the
  operator or PLAN-33 (verifies the answer verb). (2) The startup-notice baseline counts per kind (`NeedsOperatorNotices.java:224-241`); a
  replaced item of the same kind goes unannounced. The real fix needs item identities in `status` from the runtime (runtime epic; the #416
  reply promised "should be raised", nothing was filed). PLAN-50 seeds from every page meanwhile.

- PLAN-33 (running, verification) filed three findings 2026-11-10 (`rb-33-verify-cli-acts-001`…`003`, at `0913d6f3`,
  drained as observed, archived): (1) `CLI-UC-V-06` — `onboard --auto --yes` has no verb (exit 2); the automatic
  onboarding is the entry dialogue's first choice since PLAN-22, so the CHECK's wording is outdated (route: the review
  doc's check text, at PLAN-33's result or PLAN-74); rest of V-06 passes. (2) `CLI-UC-V-09` — `plan nope` prints
  `Plan nope not found.` (no `No plan {id}` text; exit 1 holds) → PLAN-30; the full-screen overview in
  `reports-and-system` shows "Nothing runs…" for a REFUSED projects read (`OverviewFullScreenApp.fetchProjects:486`
  swallows, `ProjectsOverviewLevel.rows:86`; CLI-UC-F-15) → CORRECTED 2026-11-10: PLAN-30's re-grounding does NOT carry
  it ("a separate plan"); it is PLAN-126 deliverable 3. (3) `CLI-CC-V-06` — the
  waiver path (`WR-2`) has no `q`-first and no unknown-input PTY case (`AnswerDialogueIT:102-118`) → PLAN-127 (the PLAN-124
  residue plan). Expected-fail list of the PLAN-33 amendment matched (V-09); V-01/V-04/V-08 not yet reported.
  (4) 2026-11-10 `rb-33-verify-cli-acts-004` (`CLI-UC-V-04` German clause): operator ruled "exempt runtime text"; the clause
  and `CLI-CC-V-01` reworded by direct PR #402 (see Decisions); V-04 passes under the new wording. Retire this watch at the
  PLAN-33 landing once #402 is merged. PLAN-33's PR is #398 (open).
  → PLAN-33 LANDED 2026-11-10 (#398, `344515f9`): see `landings/PLAN-33.md`. The verdict corrections (V-06 stale check,
  UC-ONB-2 not verified, four `conforms` → `partial`, Totals 4/19/0/0/14) ride PR #402 (operator); the accept-edit gap is
  folded into PLAN-127. Retire this watch when #402 is merged.
  → RETIRED 2026-11-11: #402 merged `5fc36ebf` (operator's word).
- PLAN-123 STOPPED BEFORE THE MERGE (2026-11-10, `rb-123-cli-display-layer-residue-001.md`, `merge_state=open`, drained as
  observed; row stays `running`): PR #404 at `c8be43d1` (61 files), all four deliverables met by the lane's account, Sonar 0
  (PR search empty), `review-gate` 0 threads / 0 bodies — but CodeRabbit has reviewed NO head (3 × "Review rate limited",
  15:15/16:05/16:35; next allowed trigger per the lane 18:05:28), so the merge rule is not met. Owed: one CodeRabbit review
  of some head (re-trigger once; quota refusal = ask the operator), then the operator's merge word; then the full landing
  analysis (three-layer leaf) on the merge commit. Residue named by the lane: printed `plan report` hard-coded English;
  `RolesCommand` English-only; mailbox unreadable (Open Defect). PLAN-126 and PLAN-32 still wait for this merge.
  → PLAN-123 LANDED 2026-11-11 (#404, `1bdfede6`): see `landings/PLAN-123.md`; residue folded into PLAN-30 D5 and PLAN-126 D2.
- PLAN-141 (D6) DECISIONS TAKEN, PR #403 OPEN AT DRAFT (`plan-141-001.md`, 2026-11-10, drained as observed; row stays
  `running`): head `207b62f0`, 13 design documents, CI green, links clean. Q1 plan tabs Process/Intent/Changes; Q2 epic tabs
  Plans/Intent/Landings/Reviews/Events; Q3 `plan-view-interaction.adoc` rewritten (`file=` with Changes replaces #400's
  `changes=open`); Q4 verdicts as `StatusLabel` `state.criterion.*`/`state.check.*`, new `intent-and-outline-texts.adoc`,
  chart on the project setup tab, the client contract GROWS (pm-mcp-core's intent record carries ADR relation, evidence,
  `NG-n`, `role`, `not_run` reason — the spec's hypothesis was refuted); Q5 no interim side slot, `side` on TriagePage and
  WizardPage only; Q6 remembered place without the mode, fill only on unmarked load / rail click; Q7 PLAN-85's decision
  stands (This browser holds theme, language, dictation). Operator 2026-11-10: the orchestrator reviewed #403 (one
  read-only leaf: 3 blockers, 6 majors, minors, all re-read) and, on the operator's word, fixed them on the branch
  (`e4109c2e`: README plan-tree and fill rules, Changes text per client, area `intent` registered, head counts from the
  verdict words, German `Deliverable`, chart read-only on the project's setup tab by its resource, the terminal shows no
  chart; 75 intent/state keys) and set the 12 documents AGREED (`721afd68`), pushed. CodeRabbit then REVIEWED `721afd68`
  (merge rule met): 5 minor inline findings fixed in `87288908` (glossary Outline/Role, "can flag", What waits as
  `/?lens=waits` on the operator's choice, a task address's `chat=open` is the plan's chat, selecting a part ends follow
  mode), threads replied and resolved; the additional comment (absent proposal in `OverSizeDistributionResource`) fixed in
  `cc00f135` and answered on the PR. Merge waits for the operator's word (branch behind main; the queue updates it). Not changed (reported): the plan ids PLAN-142..144/135..137 in `navigation.adoc:393`. OWED AT THE D6 LANDING (orchestrator, spec
  amendments): `doc/plans/plan-changes.adoc` address `?tab=changes&file=` (repo doc: a direct change); PLAN-55 (tab set),
  PLAN-58 (tab set), PLAN-62 (`node=` with Process only), PLAN-52 (rail rule holds), PLAN-135..137 (no interim slot; wait
  for 142, 143), PLAN-142 (side props on TriagePage/WizardPage), PLAN-143 (history.state mark, two entry points), PLAN-144
  (lens segment and Up push the unfilled list), PLAN-145 (verdict tag, texts, `NG-n`, `role`; a contract + mock step
  before it — stage or fold), PLAN-146 (chart on PLAN-59's setup tab).
  → DONE 2026-11-11 (see Decisions): amendments made, PLAN-159 staged, #413 open. Open questions recorded in the
  specs as HYPOTHESES: the engine allows 0 non-goals on the light lane while the client and design ask one or more
  (PLAN-159); the over_size route carries one role and no role parameter, the design one chart per role (PLAN-146,
  PLAN-159); the builder reason's wire name `reason` and an omitted `proposed_multiple` (no daemon route spec yet).
  → SETTLED 2026-11-12 at the PLAN-159 landing: non-goals stay ≥1 (operator; Open Defects); the one-role route is PLAN-146's;
  `reason`/`proposed_multiple` rest on the design alone (no engine or daemon schema names them).

- D5 CLOSED 2026-11-10 (`plan-125-005.md`; addendum 2 in `landings/PLAN-125.md`): follow-up F1-F9 merged — core #349
  `db02e443`, docs #174 `10f98b73`, clients #395 `cc0d62af`; merge rule met, Sonar 0, cleanup verified; all F1-F9
  fixed and consistent (leaf). Operator's F9: the chat stays (follow-up start applied by `chat.apply`, never carried).
  Residue minor: N1 core `chat-narrative.adoc:37` tests list (core's package); N2 `components-chat.adoc:53` ProposalCard
  lacks the already-queued variant; N3 `plan.revision_queued` chat-end send missing in core `state-catalogue:132` and
  docs `01:359`; info N5/N6 (carried wording reads exhaustive; web has no `follow_up.opened` rendering). NEXT: stage the
  client packages (contract + mock, web chat, terminal chat) from the AGREED docs with the `plan-125-001.md` defects and
  N2/N3/N5/N6 folded in — on the operator's word. The entries below are history.
> ↪ Relocated to `settled.md` § "D5 history: DRAFT landing, final round and AGREED merge (2026-11-09)" — D5 is closed (2026-11-10, Watch "D5 CLOSED"); the plan-125-001 code defects for the client packages are in this relocated entry and the archived message
- PLAN-26 follow-ups (landing report): (1) SETTLED 2026-11-02, the operator kept `draining · 2 left` (the rest of this item is history): keep `draining · 2 left` (the design says so now)
  or restore `draining · N plans finish · stop in M min` and `restarting`, which needs the client record to read the drain deadline the
  design lists (`system.adoc:365`) and the runtime to send it (pm-mcp-core, not read); (2) PLAN-30 D4 (the exit-code table for every verb)
  is partly done: 19 read verbs and four write paths exit 1 on a refusal; re-scope it before emit; (3) the `running-work` mock scenario has
  no `GET /api/v1/system` route (only `/system/cleanup` links), so the system view there shows only its load failure: a mock change no plan owns;
  (4) re-read PLAN-23, 30, 32, 41, 42, 48 at `be4995e5` (they name files PLAN-26 changed).
- PLAN-35 follow-ups (landing report): re-read PLAN-43, 45, 51, 53, 55, 58 against the shipped components; the view plans bring the
  components in (no view calls them yet) and should assert the CSS-level behaviour where they touch it (alignment, 40/32 px rows, head border,
  `%` widths); PR #294 was closed on 2026-11-02.
- CodeRabbit audit follow-ups (2026-11-02): all in PLAN-93 D5, PLAN-94, PLAN-95 and the two mailboxes; PLAN-94 follows PLAN-92 and PLAN-93 follows PLAN-89 (shared files). Operator decisions inside them: the
  `NeedsOperatorItem` item id (PLAN-95 D3: read the contract), the `broadcastReset` after `goToState` (PLAN-95 D2), the optional catalogue-rollback retention (PLAN-93 D5). Open reading to repeat on
  every landing: the review BODIES, not only the threads.
- PLAN-43 follow-ups (landing report): PLAN-90 deliverable 3 rebases over the `SystemView.svelte` move; PLAN-51, 53, 55, 58 add their view to `VIEWS_ON_TEMPLATES`, are the first
  browser check of Triage, Object and Wizard, and fill or guard the `ObjectPage` id slot; a pm-api owner may sweep `nonEmptyStrings()` where a record treats blank as absent
  (`OnboardingRecordsTest` 134, 167-168, 517, `SystemRecordsTest` 153-154, not read); flaky `designer.spec.js:157` and `reports.spec.js:34/89` have no owner.
- PLAN-37 follow-ups (landing report): PLAN-89 deliverables 4 and 5 (the leads above and the wider audit); `SystemView.svelte` is now touched by PLAN-37 (merged),
  PLAN-90 deliverable 3 and PLAN-43 (hunks differ, rebase when either lands); the operator's call on the standing merge authorization for heads the reviewer
  never saw. The send button of the chat no longer disables itself on an empty message (the lane's choice).
> ↪ Relocated to `settled.md` § "PLAN-37 reviewer stop of 2026-11-01 (history)" — PLAN-37 merged as c5bfb1ca
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
- Design review 2026-11-06 (opened by the drain of `design-review-001` … `010`): (1) After each design pull request merges,
  stage the packages it unlocks from the AGREED documents and re-ground the parked plans it gates — the terminal package (after D1
  and PLAN-104), D1 foundation (components, catalogue clean-up, lints as warnings), D2 navigation engine (supersede PLAN-83 naming
  it), D3 brand assets and icon, D4 contract + mock, D4 web chat, D4 voice package, and the package for the views no staged plan
  owns; PLAN-72 later switches the lints to errors. (2) Open points the sessions settle with the operator: the D2 width table
  (from the column-window prototype), the D3 icon (A bear head or B star clipboard, on the review sheet; since 2026-11-08 B is the marshal star badge and the clipboard is the waiting mark, see Decisions) and the pose artwork
  (operator or a named image tool), the D4 staging-window entry in the roadmap. Decided already: kind tints only for Project,
  Epic and Plan; German buttons in the du-imperative; bottom bar What waits · Running work · Decisions · More; the mascot is the
  operator's own work; the rail-foot bear is a reversible test; `marked` is approved for `pm-web-app` (to be recorded in
  `chat.adoc`); syntax highlighting is a later, separate dependency decision. (3) D4 files requests outside this repository:
  `plan-marshall/pm-mcp-core` (streaming `chat.reply`, the `transcribe` role) and `plan-marshall/plan-marshall-mcp` (routes,
  events, `Permissions-Policy`), and changes requirements and specification in `plan-marshall-documentation` first. (4) The sender
  may amend a drained message no more (archived messages are frozen): further review points arrive as new messages. (5) Recheck
  at each landing: the line references in the messages into `WorkControlPanel` are re-read (PLAN-106 rewrites its act lists),
  and `terminal-client/19-actions.adoc` (PLAN-97) and `web-app/onboarding.adoc` (PLAN-104) are not edited by D1/D3/D4 before those
  plans land.
