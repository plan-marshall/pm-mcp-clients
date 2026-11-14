# PLAN-75: Verify the Web App as a Product

epic: ui-improvements
workstream: WS-03

> Staged VERIFICATION plan — it runs checks and records results; it fixes nothing.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Run the closing checks of the web review: the guard in failing mode, the visual checks over all views, the design documents, and the key tasks with a person who has not seen the app. Every check below comes from the verification documents of the review (`doc/claude_review/`); each one is run as written there, against the mock runtime (`pm-mock-runtime/start.sh --scenario <name> --web-port 20750`, `POST /mock/goto/<state>`), in Chrome for the web app (both locales, both themes, 1440 px and 390 px), and for the terminal client through the screen capture harness `ScreenCaptureIT` of `cli-pty-dialogue-harness` (`en_US.UTF-8` and `de_DE.UTF-8`, 120 × 30 and 80 × 24) — never by running `pm-operator` in the agent's own shell, which the operator gate refuses by design (`PM-SEC-3`); a check that needs a person at a real terminal asks the operator. Plus the Maven test runs the check names.

## Deliverables

1. **Run the checks**
   - What: run each check and record its verdict (`pass`, `fail`, `not verified` with the reason) and its evidence (command and output, screenshot path, or test report).
   - Persona: plan-marshall:persona-auditor
   - Done when:
     - `CC-V-01 (failing mode)` of `doc/claude_review/cross-cutting/verification.adoc` (baseline empty; suite passes) has a verdict with evidence (evidence order: `none`)
     - `VD-V-06` of `doc/claude_review/visual-design/verification.adoc` (no danger at rest; Button classes; no uppercase; one primary) has a verdict with evidence (evidence order: `none`)
     - `VD-V-07` of `doc/claude_review/visual-design/verification.adoc` (visual regression per view, theme, 1440 and 390 px) has a verdict with evidence (evidence order: `none`)
     - `VD-V-08` of `doc/claude_review/visual-design/verification.adoc` (no horizontal scroll at 320 to 1920 px) has a verdict with evidence (evidence order: `none`)
     - `VD-V-09` of `doc/claude_review/visual-design/verification.adoc` (the lint rule fails on raw hex or an old token) has a verdict with evidence (evidence order: `none`)
     - `VD-V-10` of `doc/claude_review/visual-design/verification.adoc` (review by eye: "Would you pay for this tool?") has a verdict with evidence (evidence order: `none`)
     - `UX-V-13` of `doc/claude_review/ux/verification.adoc` (designs pass doc-review; every proposal done or dropped) has a verdict with evidence (evidence order: `none`)
     - `UX targets T1-T5` of `doc/claude_review/ux/verification.adoc#targets` (a second person does T1 to T5 after two minutes) has a verdict with evidence (evidence order: `none`)
2. **Record the result**
   - What: write `doc/claude_review/verification-results/verify-web-final.adoc`: one row per check with verdict and evidence, the commit verified, the date, and the environment. Move the verdicts of the analysis matrices named in the Expected Surface where the checks prove a change (for example a use case from `partial` to `conforms`).
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

- Ports of this plan: mock web port `20750`, app port `20751` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Verification only: no change to production code, tests, or design documents. A defect is reported, never fixed in this plan; a check that cannot run is `not verified` with its reason, never `pass`.
- Run every check of deliverable 1; a check is never dropped or narrowed. Mock limits (`doc/claude_review/README.adoc#limits`) are named where they decide a verdict.
- A check that asks for the operator or for a second person records their answer, or `not verified` when they were not available.

## Claim Labels

- OBSERVED: the checks are stated in the verification documents of the review — read at `doc/claude_review/cross-cutting/verification.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: cross-cutting/verification.adoc:12 CC-V-01; visual-design/verification.adoc VD-V-06..10 (:41-63); ux/verification.adoc UX-V-13 (:131) and #targets (:8)
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): mobile.spec.js changed (PLAN-90 green e2e baseline); verify-web-final.adoc and visual.spec.js still absent (created by this plan, correct). Dependencies PLAN-72/73/74 not landed.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: verify-web-final.adoc and pm-web-e2e/tests/visual.spec.js still absent; mobile.spec.js present; PLAN-72/73/74 not landed (aliases remain, baseline non-empty, no stylelint)
- Verify-first clause: before running, confirm that every plan named under Depends on has landed on `main`; a dependency that has not landed makes its checks `not verified`.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first clause; deps PLAN-72/73/74 not landed so checks would be not verified

## Expected Surface


- OBSERVED: `doc/claude_review/verification-results/verify-web-final.adoc` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/claude_review/README.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/visual.spec.js` — absent at HEAD 1cc0e10e; created by this plan if the outline keeps it
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-73` (`web-text-guard-failing`), `PLAN-72` (`web-token-cleanup-lint`), `PLAN-74` (`web-design-docs-closure`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: other verification plans may update the same analysis matrix (one shared file)
- Adjacent to: the plans verified; their code stays untouched

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions. **Hold.** It is re-grounded after D1–D4
(its checks are rewritten against the AGREED documents of the design review and the shipped packages). Queue status:
`parked`; the Expected Surface above is NOT widened now (a parked row is not in the disjointness gate population).

## Hand-Off Command

Lane: `plan-marshall` (verification plans run in the plan lifecycle, separate from the implementation runs).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-75-verify-web-final.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

- OBSERVED: `doc/claude_review/verification-results/verify-web-final.adoc` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/claude_review/README.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/visual.spec.js` — absent at HEAD 1cc0e10e; created by this plan if the outline keeps it
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 1cc0e10e
