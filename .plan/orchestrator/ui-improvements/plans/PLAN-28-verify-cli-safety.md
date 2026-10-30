# PLAN-28: Verify the Terminal Safety Fixes

> **SUPERSEDED on 2026-10-30 (structure review, operator decision): do not emit.** Its four checks moved into `PLAN-33` (`verify-cli-acts`): the two verified the same acting dialogues, nothing waits for either, and one result file per stage is enough. The text below is kept for the record.

epic: ui-improvements
workstream: WS-04

> Staged VERIFICATION plan — it runs checks and records results; it fixes nothing.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Run the safety checks of the terminal client against the merged code, in a real terminal and through the PTY tests. Every check below comes from the verification documents of the review (`doc/claude_review/`); each one is run as written there, against the mock runtime (`pm-mock-runtime/start.sh --scenario <name> --web-port 20280`, `POST /mock/goto/<state>`), in Chrome for the web app (both locales, both themes, 1440 px and 390 px), and for the terminal client through the screen capture harness `ScreenCaptureIT` of `cli-pty-dialogue-harness` (`en_US.UTF-8` and `de_DE.UTF-8`, 120 × 30 and 80 × 24) — never by running `pm-operator` in the agent's own shell, which the operator gate refuses by design (`PM-SEC-3`); a check that needs a person at a real terminal asks the operator. Plus the Maven test runs the check names.

## Deliverables

1. **Run the checks**
   - What: run each check and record its verdict (`pass`, `fail`, `not verified` with the reason) and its evidence (command and output, screenshot path, or test report).
   - Persona: plan-marshall:persona-auditor
   - Done when:
     - `CLI-CC-V-06` of `doc/claude_review/cli/cross-cutting/verification.adoc` (every acting dialogue has a PTY test; q sends nothing) has a verdict with evidence (evidence order: `none`)
     - `CLI-UC-V-01` of `doc/claude_review/cli/use-cases/verification.adoc` (pause: q leaves; 9 re-asks; Enter preselection) has a verdict with evidence (evidence order: `none`)
     - `CLI-UC-V-02` of `doc/claude_review/cli/use-cases/verification.adoc` (runtime stop asks; k keeps; --no-input exits 3) has a verdict with evidence (evidence order: `none`)
     - `CLI-UC-V-03` of `doc/claude_review/cli/use-cases/verification.adoc` (waiting plan offers the decision first) has a verdict with evidence (evidence order: `none`)
2. **Record the result**
   - What: write `doc/claude_review/verification-results/verify-cli-safety.adoc`: one row per check with verdict and evidence, the commit verified, the date, and the environment. Move the verdicts of the analysis matrices named in the Expected Surface where the checks prove a change (for example a use case from `partial` to `conforms`).
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

- Ports of this plan: mock web port `20280`, app port `20281` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Verification only: no change to production code, tests, or design documents. A defect is reported, never fixed in this plan; a check that cannot run is `not verified` with its reason, never `pass`.
- Run every check of deliverable 1; a check is never dropped or narrowed. Mock limits (`doc/claude_review/README.adoc#limits`) are named where they decide a verdict.
- A check that asks for the operator or for a second person records their answer, or `not verified` when they were not available.

## Claim Labels

- OBSERVED: the checks are stated in the verification documents of the review — read at `doc/claude_review/cli/cross-cutting/verification.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/cli/cross-cutting/verification.adoc exists (CLI-CC-V-03 at line 27, CLI-CC-S-02 row at line 39)
- Verify-first clause: before running, confirm that every plan named under Depends on has landed on `main`; a dependency that has not landed makes its checks `not verified`.

## Expected Surface

- OBSERVED: `doc/claude_review/verification-results/verify-cli-safety.adoc` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/claude_review/cli/use-cases/analysis.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-02` (`cli-pty-dialogue-harness`), `PLAN-10` (`cli-choice-prompt-pause`), `PLAN-17` (`cli-runtime-act-confirm`), `PLAN-20` (`cli-actions-by-state`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: other verification plans may update the same analysis matrix (one shared file)
- Adjacent to: the plans verified; their code stays untouched

## Hand-Off Command

Lane: `plan-marshall` (verification plans run in the plan lifecycle, separate from the implementation runs).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-28-verify-cli-safety.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
