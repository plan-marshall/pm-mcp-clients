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
- Three tracks for `parallelization_scope = 3` — web (WS-02, WS-03), terminal (WS-04, WS-05), shared
  (WS-01). The tracks touch disjoint modules (`pm-web-app`/`pm-web-e2e` against `pm-operator`), so one
  plan per track can run at a time; the shared catalogue `pm-api/src/main/resources/messages/` is split
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
- PLAN-08 (`use-case-documents`) gates PLAN-46 (command palette), PLAN-49 (attention inbox) and
  PLAN-36 (`needs`): UC-PLN-7 and UC-PLN-8 must be agreed first.
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
- 2026-10-26 — Round 1 cancelled and re-launched: the runbook lane failed on the opencode install
  (bundle resolution, `executor-skew`, empty architecture); after the lane fix PLAN-01, PLAN-02 and
  PLAN-04 were launched again (PRs #226, #229).
- 2026-10-26 — `orchestrator.use_worktree` switched on (operator request, PR #230): the ledger lives in
  `.plan/local/worktrees/_orchestrator` on `chore/orchestrator-ledger` and reaches `main` through
  `land`, so the main checkout stays clean while plans run.
- 2026-10-27 — PLAN-02 landing analysed (`landings/PLAN-02.md`). The two baseline defects it found (a
  `-` placeholder in the plan view's task line, a relative "today" in the status header) belong to
  PLAN-34 (`cli-display-layer`), not to PLAN-04/PLAN-16 as the message says; PLAN-34 already covers
  them, so nothing is folded. The undeclared files it touched (`pm-mock-runtime/README.adoc`, the
  test-scope `tui/AnsiScreen*`, `VirtualScreenTerminal`) collided with nothing.
- 2026-10-27 — PLAN-04 landing analysed (`landings/PLAN-04.md`). Spec correction across the corpus: the
  standing constraint named `./mvnw -pl pm-web-e2e verify -Pintegration-tests`, which activates no profile
  in `pm-web-e2e` and skips Playwright silently; the suite's profile is `-Pe2e`. Replaced in the 27 staged
  web specs (PLAN-01 running and PLAN-04 shipped left as they are). The files PLAN-04 touched beyond its
  declared surface (`lib/theme.ts`, `routeTitles.test.ts`, the `plan_view`/`epic_view`/`start` message
  areas, `workspace.spec.js`, five design docs) collided with nothing in flight.
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
- 2026-10-27 — PLAN-01 landing analysed (`landings/PLAN-01.md`). Its unmet clauses are spec wording, not
  missing code, so the gaps are folded into PLAN-73 (`web-text-guard-failing`), deliverable 1:
  - the guard does not scan chips, aria labels or glyphs;
  - CC-F-05 has no rule;
  - split tokens are not seen;
  - the browser clause becomes "every violation the guard lists appears in the snapshot".

  PLAN-73's Expected Surface is updated in the same act: the three guard files exist now, and
  `text-guard.spec.js` is added. The port collision with PLAN-03 cost PLAN-01 several hours, which
  confirms PLAN-76.
- 2026-10-27 — PLAN-76 landing analysed (`landings/PLAN-76.md`), and the operator's hold on new launches
  lifted. Emitted three plans, one per track, all with port blocks and disjoint from PLAN-03 and from each
  other: PLAN-06 (`shared-display-vocabulary`), PLAN-07 (`cli-terminal-design-docs`) and PLAN-12
  (`web-design-tokens-v2`).
  - With PLAN-03, that is four in flight, one above N=3, at the operator's request ("three if sensible").
    It is sensible because the port blocks remove the collision that motivated the limit.
  - Not chosen: PLAN-08 (same track as PLAN-06); PLAN-10, PLAN-13 and PLAN-14, which share
    `operator.{de,en}.properties` with each other; PLAN-15, which shares `15-overview.adoc` with PLAN-07.
- 2026-10-27 — PLAN-07 landing analysed (`landings/PLAN-07.md`). Emitted PLAN-10 (`cli-choice-prompt-pause`)
  into the terminal slot it freed. It comes first in order among the ready terminal plans and gates PLAN-20
  and PLAN-28.
  - Corrected before the emit, under the terminal-check watch: deliverable 1 (`ChoicePrompt`) gets no
    terminal check of its own, because it reaches the screen only through deliverable 2's dialogues.
  - Deliverable 2 now enables `CancelDialogueIT` as well as `PauseDialogueIT` (both name PLAN-10 since
    PLAN-02), and its capture covers the cancel dialogue.
  - Expected Surface: `PauseDialogueIT` relabelled present, `CancelDialogueIT` added.
  - Not chosen: PLAN-13 and PLAN-14 (share the operator catalogue with PLAN-10); PLAN-15 (next terminal
    candidate); PLAN-08 (Sonar watch open).
- 2026-10-28 — PLAN-06 landing analysed (`landings/PLAN-06.md`). Two operator decisions in its run change
  the shared contract, and later plans build on them:
  - The `state.<kind>.<value>` and `unit.<id>` keys are a documented carve-out from the three-part key
    rule of `texts-and-languages.adoc`.
  - `pm-api` gained the closed sets `EpicState`, `LandingState`, `CleanupKind`, `ModelStatus` and
    `GateKind`, and `PlanActivity` gained `STAGED`/`ENDED`. Before, five of the eleven state kinds existed
    only as literals in the mock and the views.

  The display layers PLAN-09 and PLAN-34 read these enums; their specs predate them, so each is checked
  against them before it is emitted.
- 2026-10-28 — `doc-review` under the runbook lane: its checker (`trace.py`, in the sibling documentation
  repository) does not run through the executor. PLAN-06's operator accepted an equivalent review. The
  five staged runbook specs that ask for it (PLAN-15, 42, 48, 70, 74) now name that equivalent:
  `docs review` plus `asciidoc validate`/`verify-links`, clean. The 13 verification specs run on the
  `plan-marshall` lane, where the skill runs, and are unchanged.
- 2026-10-28 — Emitted PLAN-15 (`cli-footer-and-help-overlay`) into the slot PLAN-06 freed (four in
  flight, as before).
  - The terminal-check watch applied: deliverables 2 (footer by priority) and 3 (help overlay) had the
    plan's shared residue target, which shows neither change. Each now names its own observable: the
    80-column footer keeping `a`, `Enter`, `Esc`, `?`, `q` and ending `? more`; and `?` opening the
    grouped box with `Esc close`.
  - Not chosen: PLAN-08 (the docs-only Sonar watch needs the operator's decision); PLAN-09 (check
    against the new enums and decide the tab-title watch first); PLAN-11 (5 files shared with the
    in-flight PLAN-03); PLAN-13 and PLAN-14 (share the operator catalogue with PLAN-10).
- 2026-10-28 — PLAN-12 landing analysed (`landings/PLAN-12.md`). Emitted PLAN-19 (`web-ui-icon-set`) into
  the web slot it freed. Its dependencies PLAN-12 and PLAN-76 are shipped, it is disjoint from PLAN-03,
  PLAN-10 and PLAN-15, and it carries PLAN-12's emoji residue. PLAN-09 was not chosen: the PLAN-06 enum
  check and the tab-title watch must be settled before its emit.
- 2026-10-28 — PLAN-15 and PLAN-19 landings analysed (`landings/PLAN-15.md`, `landings/PLAN-19.md`). With PLAN-10
  alone in flight, two of the three slots were free. Emitted PLAN-08 (`use-case-documents`, shared track) and
  PLAN-25 (`web-core-components-controls`, web track, ports 20250/20251). Both pass the gate: PLAN-08 depends on
  nothing and PLAN-25 on PLAN-12, 19 and 76 (all shipped); each shares no file with PLAN-10 or with the other;
  every verdict admits; every surface is declarative. PLAN-08 was held back before only for the docs-only Sonar
  watch, which was decided earlier today. No overlap prompt was asked.
  - Not chosen: PLAN-09 (the PLAN-06 enum check and the tab-title watch are still owed, and PLAN-19 changed five of
    its views); PLAN-11 (re-read against HEAD and add the e2e specs and the transcript to its Expected Surface
    first); PLAN-13 and PLAN-14 (each shares both operator catalogue files with PLAN-10, so the overlap prompt
    would fire); PLAN-27 (ready, but the web track is taken by PLAN-25).
- 2026-10-28 — PLAN-08, PLAN-25 and PLAN-27 recorded launched on the operator's confirmation (four launched with
  PLAN-10, one above N=3 by the operator's choice).
- 2026-10-29 — PLAN-10 landing analysed (`landings/PLAN-10.md`). Row `shipped` (PR #257), message archived. No plan
  emitted: three plans (08, 25, 27) stay launched, which is N. Next in line when a slot frees: PLAN-11, or the
  terminal plans PLAN-13 and PLAN-14 now that the operator catalogue is free (re-read at HEAD first).
- 2026-10-29 — PLAN-25 landing analysed (`landings/PLAN-25.md`). Row `shipped` (PR #260), message archived. No plan
  emitted: two plans (08, 27) stay launched, so one slot is free; the choice of what takes it is left to the
  operator: PLAN-31 (ready, but the web track is taken by PLAN-27 until it lands), PLAN-11 (mock track), or
  PLAN-13 / PLAN-14 (terminal track, free now that PLAN-10 shipped; they overlap on the operator catalogue).
- 2026-10-29 — Ledger landed as PR #263 (merge `a2f4d2c4`): PLAN-10 and PLAN-25 landings, the launch records of
  PLAN-08, 25 and 27, the anchor and this file.
- 2026-10-29 — Second PLAN-01 landing message analysed (`landings/PLAN-01.md`, addendum). It comes from the
  resumed run of the same plan after the server outage, not from a re-run of a shipped plan: PR #237 was merged
  by the operator during the outage, no new PR, no leftover branch or worktree. The row stays `shipped`. Its
  round-3 findings were folded into PLAN-73's deliverable 1; no plan emitted.
- 2026-10-29 — PLAN-08 and PLAN-27 recorded running on the operator's word (PLAN-08 had been recorded launched
  from a command emitted, with no proof of a live session; the operator found the session; PR #259 open, CI green).
- 2026-10-29 — Emitted PLAN-31 (`web-core-components-containers`, ports 20310/20311) and PLAN-11 (`mock-cli-fixes`,
  ports 20110/20111) at the operator's request. With PLAN-08 and PLAN-27 running that makes four in flight, one
  above N=3, by the operator's choice (the same exception as 2026-10-28). Gate: both specs are declarative with a
  determinate comparison, no blocking candidate and no overlap prompt (`corpus cross-check`); the four surfaces
  are pairwise disjoint (PLAN-08 `doc/design/`, PLAN-27 brand files, PLAN-31 containers and the dev page, PLAN-11
  the mock). PLAN-31 shares the web track with PLAN-27 on the operator's choice; the files are disjoint, but both
  plans may touch `pm-web-app` in the same period. Both dependencies are shipped (PLAN-31: 25, 76; PLAN-11: 03,
  76). Specs re-read at HEAD `a2f4d2c4` and corrected before the emit: PLAN-31 (the dev page exists, created by
  PLAN-25; the controls it reuses; the `dev.*` keys) and PLAN-11 (`kind="item"` is at `MockRuntimeEngine.java:7708`,
  not 7663; claim 1 re-confirmed: still one plan copy for any plan id, no `/api/v1/config` or `/roles` route).
  Not added to PLAN-11: the two stale mock epic routes (`actions/cancel` ignores `with_plans`, `actions/next`
  is a no-op); the operator did not decide it, so they stay a watch. Not recorded as launched: that waits for the
  operator's confirmation that the commands were started.

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
  of a resumed run, opencode), same two keys.

## Watches

- Operator decisions still open: the development switch `PM_OPERATOR_DEV_ALLOW_AGENT` (CLI-CC-P-05,
  security; PLAN-14 does not build it); the config contract (PLAN-32); the proof of presence (PLAN-24
  writes the record for the decision). — re-check when PLAN-14 and PLAN-24 land.
- Web event handling should read the `pm-api` event table of PLAN-13 (CLI-UC-S-05 step 4). No web plan
  owns it yet. — re-check at the landing of PLAN-13; stage a web plan if the table lands.
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
  these two).
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
- `EpicProcessPlan` documents `phase_state` as `running/staged/planned/done` (`EpicProcessPlan.java:26`),
  but the scenarios serve `staged`/`ended` and `PlanActivity.parse("done")` throws (PLAN-06 residue).
  — fix in PLAN-34 (`cli-display-layer`), the next plan on the `pm-api` display contract; check before
  emitting it.
- Four lifecycle states are invisible in `pm-web-app` (PLAN-03 residue): `system.running`, `system.stopped`,
  `system.restarting` and `system.force-stop` change the API resource but no rendered view, because the app
  shows the lifecycle only as the draining banner. PLAN-11, PLAN-17 and PLAN-26 name `system.*` states but are
  mock and CLI plans, so no staged web plan owns rendering them. — decide when PLAN-39 (`web-app-shell`) or
  PLAN-09 is emitted: fold the rendering into one of them or stage a web plan; at the latest at PLAN-41.
- PLAN-03 changed fixture values that other staged specs assert or declare: 18 files merged against 9 declared.
  PLAN-11 changes the same mock fixtures and routes but declares neither the e2e specs nor the transcript
  that assert the values, so it is likely to repeat the under-declaration. — before emitting PLAN-11, PLAN-47,
  PLAN-50, PLAN-52, PLAN-55, PLAN-58 or PLAN-62: re-read the spec against HEAD. PLAN-11's Expected Surface
  now names `plan-view.spec.js` and `system.spec.js` (cleanup 2026-10-28; the transcript was left out, nothing
  in PLAN-11 reaches it).
- Closed-PR branch left behind (retired watch, 2026-10-28): `feature/03-mock-scenario-states` (the closed PR #250's
  head, `8cf8cfb`) outlived the PLAN-03 landing, which reports `cleanup_owed=false`; the lane pruned only the `-b`
  branch. Deleted from the remote on the operator's word after checking that no open PR used it and that
  merge commit `4efa0314` contains every file it changed (plus later fixes). Open point that stays: the lane's
  cleanup does not treat a closed PR's branch as owed; re-check at the next landing that re-opens a PR.
- PLAN-03 residue outside its boundary: `doc/design/terminal-client/26-cleanup.adoc` says 1.8 GB where the
  fixture now says 1.9, and `pm-web-app/src/lib/views/MachineSettingsView.scenarios.test.ts` still passes
  `36 roles` as an input. — fix the document in PLAN-23 (`cli-cleanup-dialogue`, which names it) and the test in PLAN-09 or
  PLAN-60, the staged specs that name `MachineSettingsView`; check there.
- The mock writes a kept cleanup item with `kind="item"` (`MockRuntimeEngine:7663` at `4efa0314`;
  PLAN-03 moved it from 7462 and left it), outside `CleanupKind` (PLAN-06 residue). — folded into PLAN-11 (`mock-cli-fixes`) deliverable 2 on 2026-10-28 (its Expected Surface already names `MockRuntimeEngine`); retire at PLAN-11's landing. PLAN-10 changed `MockRuntimeEngine` after that (PR #257), so the line number is stale: the lane re-reads it at HEAD.
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
