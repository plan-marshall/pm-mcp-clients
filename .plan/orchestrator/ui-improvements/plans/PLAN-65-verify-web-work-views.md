# PLAN-65: Verify the Web Work Views

epic: ui-improvements
workstream: WS-03

> Staged VERIFICATION plan — it runs checks and records results; it fixes nothing.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Run the checks of the inbox, running work, start, epic, projects, reports, designer and settings. Every check below comes from the verification documents of the review (`doc/claude_review/`); each one is run as written there, against the mock runtime (`pm-mock-runtime/start.sh --scenario <name> --web-port 20650`, `POST /mock/goto/<state>`), in Chrome for the web app (both locales, both themes, 1440 px and 390 px), and for the terminal client through the screen capture harness `ScreenCaptureIT` of `cli-pty-dialogue-harness` (`en_US.UTF-8` and `de_DE.UTF-8`, 120 × 30 and 80 × 24) — never by running `pm-operator` in the agent's own shell, which the operator gate refuses by design (`PM-SEC-3`); a check that needs a person at a real terminal asks the operator. Plus the Maven test runs the check names.

## Deliverables

1. **Run the checks**
   - What: run each check and record its verdict (`pass`, `fail`, `not verified` with the reason) and its evidence (command and output, screenshot path, or test report).
   - Persona: plan-marshall:persona-auditor
   - Done when:
     - `UX-V-02` of `doc/claude_review/ux/verification.adoc` (inbox lists the five items, blocking first; acts open their place) has a verdict with evidence (evidence order: `none`)
     - `UX-V-09` of `doc/claude_review/ux/verification.adoc` (running work across projects; aligned columns; ended table) has a verdict with evidence (evidence order: `none`)
     - `UX-V-10` of `doc/claude_review/ux/verification.adoc` (start steps with one primary; Back; no empty entry) has a verdict with evidence (evidence order: `none`)
     - `UX-V-11` of `doc/claude_review/ux/verification.adoc` (epic since; project overflow; unread reports; designer results) has a verdict with evidence (evidence order: `none`)
     - `UX-V-12` of `doc/claude_review/ux/verification.adoc` (settings redirect; one save; plain names) has a verdict with evidence (evidence order: `none`)
     - `UC-V-08` of `doc/claude_review/use-cases/verification.adoc` (overview groupings agree; epic counts; read-only chat; Intent) has a verdict with evidence (evidence order: `none`)
     - `UC-V-10` of `doc/claude_review/use-cases/verification.adoc` (identity fields; terminal step; start gate; lazy entry) has a verdict with evidence (evidence order: `none`)
     - `CC-V-08` of `doc/claude_review/cross-cutting/verification.adoc` (every number pair of CC-F-09 agrees or says showing n of m) has a verdict with evidence (evidence order: `none`)
     - `UX-V-01` of `doc/claude_review/ux/verification.adoc` (rail groups and counts; phone bottom bar; live indicator) has a verdict with evidence (from the former `PLAN-61`, superseded) (evidence order: `none`)
     - `UX-V-03` of `doc/claude_review/ux/verification.adoc` (palette finds relay and PR 214, through the runtime's search of `PLAN-82`) has a verdict with evidence (from the former `PLAN-61`) (evidence order: `none`)
     - `UX-V-04` of `doc/claude_review/ux/verification.adoc` (no view sets its own page width; the views on their templates) has a verdict with evidence (from the former `PLAN-61`) (evidence order: `none`)
     - `UX-V-05` of `doc/claude_review/ux/verification.adoc` (at most one primary act per state; no red at rest) has a verdict with evidence (from the former `PLAN-61`) (evidence order: `none`)
     - `UX-V-06` of `doc/claude_review/ux/verification.adoc` (error pattern within 15 s; plan_not_found/project_not_found) has a verdict with evidence (from the former `PLAN-61`) (evidence order: `none`)
     - `UC-V-04` of `doc/claude_review/use-cases/verification.adoc` (no completion report while running; draining acts) has a verdict with evidence (from the former `PLAN-61`) (evidence order: `none`)
     - `UX-V-07` of `doc/claude_review/ux/verification.adoc` (plan head, attention strip, Settings tab, no empty LINK row) has a verdict with evidence (from the former `PLAN-67`, superseded) (evidence order: `none`)
     - `UX-V-08` of `doc/claude_review/ux/verification.adoc` (decisions ordered; keys decide; next selected; toast; one notification for a burst, none after a reconnect) has a verdict with evidence (from the former `PLAN-67`) (evidence order: `none`)
     - `UC-V-06` of `doc/claude_review/use-cases/verification.adoc` (phase workflows, expanders, Intent, Not in this plan) has a verdict with evidence (from the former `PLAN-67`) (evidence order: `none`)
     - `UC-V-07` of `doc/claude_review/use-cases/verification.adoc` (recommendation by name; evidence fields; own answer; elsewhere) has a verdict with evidence (from the former `PLAN-67`) (evidence order: `none`)
2. **Record the result**
   - What: write `doc/claude_review/verification-results/verify-web-work-views.adoc`: one row per check with verdict and evidence, the commit verified, the date, and the environment. Move the verdicts of the analysis matrices named in the Expected Surface where the checks prove a change (for example a use case from `partial` to `conforms`).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc
   - Done when:
     - The result file exists, lists every check of deliverable 1, and passes `doc-review` (evidence order: `none`)
3. **Route what failed**
   - What: every check with verdict `fail` becomes one `finding` message to the epic inbox (`orchestrator inbox write --kind finding`), naming the check, the evidence, and the plan whose deliverable it concerns. Nothing is fixed here.
   - Persona: plan-marshall:persona-auditor
   - Done when:
     - The number of findings filed equals the number of `fail` verdicts (evidence order: `none`)

## Standing Constraints

- Ports of this plan: mock web port `20650`, app port `20651` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Verification only: no change to production code, tests, or design documents. A defect is reported, never fixed in this plan; a check that cannot run is `not verified` with its reason, never `pass`.
- Run every check of deliverable 1; a check is never dropped or narrowed. Mock limits (`doc/claude_review/README.adoc#limits`) are named where they decide a verdict.
- A check that asks for the operator or for a second person records their answer, or `not verified` when they were not available.

## Claim Labels

- OBSERVED: the checks are stated in the verification documents of the review — read at `doc/claude_review/ux/verification.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main:doc/claude_review/ux/verification.adoc exists; check table rows UX-S-01..UX-S-13 (inbox, running-work, decisions, start) at lines 59-132
- Verify-first clause: before running, confirm that every plan named under Depends on has landed on `main`; a dependency that has not landed makes its checks `not verified`.

## Expected Surface

- OBSERVED: `doc/claude_review/verification-results/verify-web-work-views.adoc` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/claude_review/ux/analysis.adoc` — present at HEAD c4dc9bb4 (matrix of the checks of the former `PLAN-61` and `PLAN-67` too)
- OBSERVED: `doc/claude_review/use-cases/analysis.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-49` (`web-attention-inbox`), `PLAN-52` (`web-running-work`), `PLAN-53` (`web-start-wizard`), `PLAN-58` (`web-epic-page`), `PLAN-59` (`web-projects-and-onboarding`), `PLAN-54` (`web-reports-and-designer`, reports only now), `PLAN-88` (`web-designer-panes`), `PLAN-60` (`web-settings-one-place`), `PLAN-76` (`shared-port-assignment`, shipped), and, from the former `PLAN-61` and `PLAN-67` (superseded), `PLAN-39` (shipped), `PLAN-43`, `PLAN-45`, `PLAN-47`, `PLAN-46`, `PLAN-55`, `PLAN-62`, `PLAN-51`, `PLAN-83`, `PLAN-85`
- Overlaps with: other verification plans may update the same analysis matrix (one shared file)
- Adjacent to: the plans verified; their code stays untouched

## Hand-Off Command

Lane: `plan-marshall` (verification plans run in the plan lifecycle, separate from the implementation runs).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-65-verify-web-work-views.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
