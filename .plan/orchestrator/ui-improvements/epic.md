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
- PLAN-76 (`shared-port-assignment`) gates every staged plan that names a port block: those specs pass
  their ports (`PM_MOCK_WEB_PORT`, `PM_WEB_PORT`, `--web-port`, `-p`) only once PLAN-76 has landed, and
  each lists it under Depends on. Emit none of them before it lands.
- Cross-repo plans — PLAN-08 and PLAN-24 land their main change in `plan-marshall-documentation`; their
  Expected Surface names only the files of this repository.

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

## Open Defects

- 2026-10-27 — Incomplete landing facts: `rb-02-cli-pty-dialogue-harness-001.md` (PLAN-02) reports
  `missing_keys` `total_tokens` and `plugin_version` (both `unknown` on opencode). It was reconciled as far
  as its facts go. The runbook lane on opencode cannot measure tokens or read the plugin version, so every
  opencode landing will repeat this until the lane can. Recurrence 2026-10-27:
  `rb-04-web-theme-and-titles-001.md` (PLAN-04), same two keys.

## Watches

- Operator decisions still open: the development switch `PM_OPERATOR_DEV_ALLOW_AGENT` (CLI-CC-P-05,
  security; PLAN-14 does not build it); the config contract (PLAN-32); the proof of presence (PLAN-24
  writes the record for the decision). — re-check when PLAN-14 and PLAN-24 land.
- Web event handling should read the `pm-api` event table of PLAN-13 (CLI-UC-S-05 step 4). No web plan
  owns it yet. — re-check at the landing of PLAN-13; stage a web plan if the table lands.
- The mock limits (`doc/claude_review/README.adoc#limits`) can make view checks `not verified` until
  PLAN-03 and PLAN-11 land. — re-check at the first verification landing.
- Runbook lane on opencode: `executor-skew` still answers `could_not_check` (the lane text version cannot
  be derived from the flat install path), and the guard is `none` (unenforced). PLAN-02 proceeded by
  operator choice. — re-check at every opencode landing; retire when a landing reports `match` and a
  guard. Recurrence: PLAN-04 (same `could_not_check`, guard `none`).
- Build server: the routed `verify -Ppre-commit` of PLAN-02 failed with exit -1 because `marshalld`
  invoked a bare `mvn` instead of the project's `./mvnw`. The project was enrolled and the daemon
  upgraded to 0.1.2017 on 2026-10-26, after that run. Recurrence: PLAN-04's one routed build failed the
  same way (`mvn` not on the daemon's PATH). — re-check at the next landing that routes through the
  daemon.
- Tab titles name the identifier (`drop-legacy-api`), not the display title (PLAN-04 residue). No
  staged plan owns a title store for the shell. — decide when PLAN-09 (`web-display-layer`) is emitted,
  or at the PLAN-29 verification (`CC-V-04`).
- The e2e suite on `main` has one failing case before any epic change: the `CleanupView` contrast check
  in light at narrow width (seen in PLAN-04's base run at `65973291`). — re-check at PLAN-09
  (changes `CleanupView`) and PLAN-70 (`web-light-theme-pass`).
