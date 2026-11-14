# PLAN-29: Verify the Web Texts, Titles, Theme, and Time

epic: ui-improvements
workstream: WS-02

> Staged VERIFICATION plan — it runs checks and records results; it fixes nothing.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Run the cross-cutting checks of the web texts plans against the merged code and record the result. Every check below comes from the verification documents of the review (`doc/claude_review/`); each one is run as written there, against the mock runtime (`pm-mock-runtime/start.sh --scenario <name> --web-port 20290`, `POST /mock/goto/<state>`), in Chrome for the web app (both locales, both themes, 1440 px and 390 px), and for the terminal client through the screen capture harness `ScreenCaptureIT` of `cli-pty-dialogue-harness` (`en_US.UTF-8` and `de_DE.UTF-8`, 120 × 30 and 80 × 24) — never by running `pm-operator` in the agent's own shell, which the operator gate refuses by design (`PM-SEC-3`); a check that needs a person at a real terminal asks the operator. Plus the Maven test runs the check names.

## Deliverables

1. **Run the checks**
   - What: run each check and record its verdict (`pass`, `fail`, `not verified` with the reason) and its evidence (command and output, screenshot path, or test report).
   - Persona: plan-marshall:persona-auditor
   - Done when:
     - `CC-V-01 (report mode)` of `doc/claude_review/cross-cutting/verification.adoc` (guard runs in de and en and lists CC-F-01 to CC-F-05) has a verdict with evidence (evidence order: `none`)
     - `CC-V-02` of `doc/claude_review/cross-cutting/verification.adoc` (plain state words in system, cleanup, plan defaults; every enum has keys) has a verdict with evidence (evidence order: `none`)
     - `CC-V-03` of `doc/claude_review/cross-cutting/verification.adoc` (every string of CC-F-05 reads as expected) has a verdict with evidence (evidence order: `none`)
     - `CC-V-04` of `doc/claude_review/cross-cutting/verification.adoc` (tab titles filled; theme survives reload; no flash) has a verdict with evidence (evidence order: `none`)
     - `CC-V-05` of `doc/claude_review/cross-cutting/verification.adoc` (no ISO time; relative with hover; locale switch clean; recorded language applied) has a verdict with evidence (evidence order: `none`)
2. **Record the result**
   - What: write `doc/claude_review/verification-results/verify-web-texts.adoc`: one row per check with verdict and evidence, the commit verified, the date, and the environment. Move the verdicts of the analysis matrices named in the Expected Surface where the checks prove a change (for example a use case from `partial` to `conforms`).
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

- Ports of this plan: mock web port `20290`, app port `20291` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Verification only: no change to production code, tests, or design documents. A defect is reported, never fixed in this plan; a check that cannot run is `not verified` with its reason, never `pass`.
- Run every check of deliverable 1; a check is never dropped or narrowed. Mock limits (`doc/claude_review/README.adoc#limits`) are named where they decide a verdict.
- A check that asks for the operator or for a second person records their answer, or `not verified` when they were not available.

## Claim Labels

- OBSERVED: the checks are stated in the verification documents of the review — read at `doc/claude_review/cross-cutting/verification.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: cross-cutting/verification.adoc holds CC-V-01..05 at lines 12-40 (file also has V-06..08); result file verify-web-texts.adoc absent in verification-results/
- Verify-first clause: before running, confirm that every plan named under Depends on has landed on `main`; a dependency that has not landed makes its checks `not verified`.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: process clause about dependency landing; PLAN-85 and the web wave are not decidable by reading this clause
- HYPOTHESIS: the text guard declares a view list that can be extended without changing the guard's rules — confirm/refute at `pm-web-e2e/tests/text-guard.spec.js` § `declared views` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: view list is DECLARED_VIEWS (23) at theme-readability.spec.js:31, rule-independent, baseline keyed by it (:1053); text-guard.spec.js has no declared-views section (location drift)

## Expected Surface


- OBSERVED: `doc/claude_review/verification-results/verify-web-texts.adoc` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/claude_review/cross-cutting/analysis.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-01` (`web-text-guard-report`), `PLAN-04` (`web-theme-and-titles`), `PLAN-09` (`web-display-layer`), `PLAN-16` (`web-time-format-and-locale`), `PLAN-76` (`shared-port-assignment`), `PLAN-85` (`web-theme-in-browser-settings`: the theme and the language are chosen on the pairing page and in "This browser", and remembered; checked here beside the titles and the time format)
- Overlaps with: other verification plans may update the same analysis matrix (one shared file)
- Adjacent to: the plans verified; their code stays untouched

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (merged from messages 001–009). **Gate:
waits with PLAN-85** (the verification checks the texts, theme and language placement that PLAN-85 and the design sessions
change). Queue status: `parked` — not emitted until its gate is met; the orchestrator re-grounds this spec against the
AGREED documents when it is restarted. The Expected Surface above is deliberately NOT widened now: the files and component
names are the design sessions' output, and a parked row is not in the disjointness gate population.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- The 2026-11-10 re-grounding says new claim bullets were added to Claim Labels; none exist. View list now `DECLARED_VIEWS` (23) at `theme-readability.spec.js:31`.

## Hand-Off Command

Lane: `plan-marshall` (verification plans run in the plan lifecycle, separate from the implementation runs).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-29-verify-web-texts.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: re-grounding of 2026-11-10 at HEAD 1cc0e10e. The three deliverables stand unchanged. A verification plan runs after the code it checks; its gate is the web wave, no longer only the design sessions (D1–D5 are `AGREED` and merged).

What each check must find is read against the agreed documents; the check ids and their "How" stay:

- `CC-V-01`: in failing mode `pm-web-e2e/tests/text-guard-baseline.json` is empty (it holds entries today, for example `EpicView` `de` `paused`). The declared views include every view this wave adds or moves (the What waits list, the project page, the Intent tab views).
- `CC-V-02`: the plain words are the agreed catalogue's: the reasons `overview.needs.*` (`doc/design/shared/needs-and-find.adoc#waits`), the kinds `common.kind.*` (`doc/design/web-app/components-tags.adoc#kind-mark`), the states `state.*`.
- `CC-V-04`: the tab titles come from `ROUTE_TITLES` of `pm-web-app/src/lib/routes.ts`, including the title of What waits that carries the total (`web.inbox.title`, `doc/design/web-app/needs-and-find.adoc#route`). The theme survives a reload (`pm_theme`, `doc/design/web-app/README.adoc#themes`); where the choice is offered is the placement `PLAN-85` builds ("This browser" and the pairing page), **not** the rail foot that `README.adoc#themes` still names (design inconsistency, to be corrected in the design first).
- `CC-V-05`: times follow `doc/design/shared/texts-and-languages.adoc#time`.

Dependencies (all must have landed before it is emitted): `PLAN-85`; the D1 foundation package (catalogue clean-up moves keys); the D2 navigation engine (routes and titles); the views package (System, Cleanup, CompletionReport, Reports strings); the web plans `PLAN-49`, `51`, `52`, `53`, `55`, `58`, `59`, `60`, `62`.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.
