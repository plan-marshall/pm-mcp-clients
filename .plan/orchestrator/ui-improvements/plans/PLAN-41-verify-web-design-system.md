# PLAN-41: Verify the Web Design System v2

epic: ui-improvements
workstream: WS-02

> Staged VERIFICATION plan — it runs checks and records results; it fixes nothing.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Run the design-system checks against the merged code and record the result. Every check below comes from the verification documents of the review (`doc/claude_review/`); each one is run as written there, against the mock runtime (`pm-mock-runtime/start.sh --scenario <name> --web-port 20410`, `POST /mock/goto/<state>`), in Chrome for the web app (both locales, both themes, 1440 px and 390 px), and for the terminal client through the screen capture harness `ScreenCaptureIT` of `cli-pty-dialogue-harness` (`en_US.UTF-8` and `de_DE.UTF-8`, 120 × 30 and 80 × 24) — never by running `pm-operator` in the agent's own shell, which the operator gate refuses by design (`PM-SEC-3`); a check that needs a person at a real terminal asks the operator. Plus the Maven test runs the check names.

## Deliverables

1. **Run the checks**
   - What: run each check and record its verdict (`pass`, `fail`, `not verified` with the reason) and its evidence (command and output, screenshot path, or test report).
   - Persona: plan-marshall:persona-auditor
   - Done when:
     - `VD-V-01` of `doc/claude_review/visual-design/verification.adoc` (the VD-S-00 decisions are recorded with their date) has a verdict with evidence (evidence order: `none`)
     - `VD-V-02` of `doc/claude_review/visual-design/verification.adoc` (WCAG 2.1 AA in both themes; allowlist empty or reasoned) has a verdict with evidence (evidence order: `none`)
     - `VD-V-03` of `doc/claude_review/visual-design/verification.adoc` (theme survives reload; system follows the OS) has a verdict with evidence (evidence order: `none`)
     - `VD-V-04` of `doc/claude_review/visual-design/verification.adoc` (no emoji anywhere; monograms for harnesses) has a verdict with evidence (evidence order: `none`)
     - `VD-V-05` of `doc/claude_review/visual-design/verification.adoc` (each component tested and shown on /dev/components) has a verdict with evidence (evidence order: `none`)
     - `CC-V-06` of `doc/claude_review/cross-cutting/verification.adoc` (Escape, focus return, disabled reasons; T2 by keyboard) has a verdict with evidence (evidence order: `none`)
     - `CC-V-07` of `doc/claude_review/cross-cutting/verification.adoc` (axe-core audit finds no serious issue) has a verdict with evidence (evidence order: `none`)
2. **Record the result**
   - What: write `doc/claude_review/verification-results/verify-web-design-system.adoc`: one row per check with verdict and evidence, the commit verified, the date, and the environment. Move the verdicts of the analysis matrices named in the Expected Surface where the checks prove a change (for example a use case from `partial` to `conforms`).
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

- Ports of this plan: mock web port `20410`, app port `20411` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Verification only: no change to production code, tests, or design documents. A defect is reported, never fixed in this plan; a check that cannot run is `not verified` with its reason, never `pass`.
- Run every check of deliverable 1; a check is never dropped or narrowed. Mock limits (`doc/claude_review/README.adoc#limits`) are named where they decide a verdict.
- A check that asks for the operator or for a second person records their answer, or `not verified` when they were not available.

## Claim Labels

- OBSERVED: the checks are stated in the verification documents of the review — read at `doc/claude_review/visual-design/verification.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/visual-design/verification.adoc exists on origin/main with checks VD-V-01..VD-V-10
- Verify-first clause: before running, confirm that every plan named under Depends on has landed on `main`; a dependency that has not landed makes its checks `not verified`.

## Expected Surface

- OBSERVED: `doc/claude_review/verification-results/verify-web-design-system.adoc` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/claude_review/visual-design/analysis.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-12` (`web-design-tokens-v2`), `PLAN-19` (`web-ui-icon-set`), `PLAN-25` (`web-core-components-controls`), `PLAN-31` (`web-core-components-containers`), `PLAN-35` (`web-data-components`), `PLAN-27` (`web-brand`), `PLAN-37` (`web-accessibility-baseline`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: other verification plans may update the same analysis matrix (one shared file)
- Adjacent to: the plans verified; their code stays untouched

## Hand-Off Command

Lane: `plan-marshall` (verification plans run in the plan lifecycle, separate from the implementation runs).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-41-verify-web-design-system.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
