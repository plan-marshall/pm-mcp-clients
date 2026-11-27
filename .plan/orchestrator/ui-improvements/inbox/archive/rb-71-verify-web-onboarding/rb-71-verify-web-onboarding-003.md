envelope_version=1
sender_type=plan
sender_id=rb-71-verify-web-onboarding
epic=ui-improvements
kind=landing
created=2026-11-27T14:38:55Z
revision=2
amended=2026-11-27T20:33:24Z

```landing-facts
schema=landing-facts/1
plan_id=rb-71-verify-web-onboarding
epic=ui-improvements
pr=#599
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=3
review_body_findings_handled=3
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

# PLAN-71 (verify the web onboarding): landed as `3f0af941`, PR #599

The three checks ran against `da3cafdf`; the result file and the nine-row matrix move are on `main`; the two failing
checks are filed to this epic's inbox. The PR was merged through the queue as
`3f0af941905548d2a3ec74becb9beae6e63c40d2` (PR #599, head `0c2edf15`), with the `skip-bot-review` label on the
operator's word after the CodeRabbit quota refused four reviews.

**One operator-directed change outside the plan's write boundary.** `main` at `5cb9cf0a` was red: `PlanViewCatalogueTest`
(six keys) and `OverviewCatalogueTest` (`overview.needs.overdue`) require the de/en packs to hold every key of the
design's text tables. The operator directed "Fix directly and land your changes", so the branch carries `2f7759a0`,
which adds the six `plan.*` keys and `overview.needs.overdue` with the design's own texts
(`plan-view.adoc:339,403-413,463-469`; `plan-overview.adoc:238-240`). Without it no head could be green, and the plan's
"verification only" constraint was bent on that word.

## Deliverables

Every Done-when of the spec declares `evidence order: none`.

- **D1 — run the checks: met.**
  - `UC-V-02`: verdict `fail`; the registry test green (188 files, 3481 tests in `pm-web-app` at `da3cafdf`), the
    92-state walk, the three regex hits, and the state-id clause `not verified` for the 17 auto-advancing screens.
  - `UC-V-03`: verdict `pass` on the re-grounding (fallback names `pm-operator`, no state id).
  - `UC-V-05`: verdict `fail` on the `harness.none` row; ten of eleven screens pass; the header and language clauses
    pass; three clauses `not verified` (the mock does not feed them).
  - Artifacts: `.plan/temp/screens/` — 96 `matrix-*.png`, 92 `sweep-*.png`, the step-bar and interaction captures.
- **D2 — record the result: met.** `doc/claude_review/verification-results/verify-web-onboarding.adoc` (new) and the
  nine-row move in `doc/claude_review/use-cases/analysis.adoc` (totals 14/12/2/1/10). Commits `6928b44c`, `42d2a59e`,
  `629c9fac`, `b4600c1c`, `c3541d93`, `17dd25e4`, `3e2b4c22`. The `doc-review` clause is satisfied by the AsciiDoc
  validators on the operator's recorded substitution (question of 2026-11-26).
- **D3 — route what failed: met.** Two `fail` verdicts, two messages: `rb-71-verify-web-onboarding-001.md`
  (revision 1) and `-002.md` (revision 5). Both validated; neither has a repository path.

## Personas and skills

- D1 and D3: `plan-marshall:persona-auditor`; D2: `plan-marshall:persona-documenter` with
  `pm-documents:ref-asciidoc`. Self-review: `plan-marshall:persona-code-reviewer`.
- Skills added: none.

## Hypotheses

The spec carries no HYPOTHESIS line. Its Claim Labels stay true; the verify-first clause holds (PLAN-64 `2fc22f3d`,
PLAN-66 `98ca84d0`, PLAN-76 `15b7780c` landed).

## Findings

- Self-review rounds 1 to 3 plus a confirming round: 30 findings (5 major, 16 minor, 9 nit) — all fixed; the operator
  accepted the close.
- CodeRabbit, review of `c3541d93` (its one review): 3 findings, all fixed (`17dd25e4`, `3e2b4c22`), each replied to
  and its thread resolved.
- Sourcery: approved, nothing to fix.

## Reviewers

- `coderabbit` (required): refused on quota at 11:52Z, 13:34Z, 13:50Z and 14:21Z; reviewed `c3541d93` at 16:14Z after
  the quota window; reviewed `16c91702` at 18:14Z with one more finding; then skipped by the `skip-bot-review` label
  (operator's word) for the final heads.
- `sourcery` (optional): approved `d10b68ab`; skipped by the label afterwards.

## Operator questions

1. 2026-11-26 — D2's `doc-review` clause: {Accept the validator substitution, Run the full doc-review skill, Record
   D2 as not fully met} → "Accept the validator substitution".
2. 2026-11-27 — closing the self-review at the three-round limit: {Run one confirming round, Accept the fixes and
   record clear, Record budget-exhausted} → "Run one confirming round".
3. 2026-11-27 — closing after the confirming round's two nits: {Accept the two nit fixes and record clear, Run one more
   confirming round, Record budget-exhausted} → "Accept the two nit fixes, record clear".
4. 2026-11-27 — the two merge-gate gaps: {Stop and retry the reviewer later, Merge despite the two gaps, Close the PR
   unmerged} → "Stop and retry the reviewer later".
5. 2026-11-27 — Sonar absent at `d10b68ab`: {Enqueue despite the absent Sonar analysis, Leave the PR open, Close the PR
   unmerged} → "Enqueue despite the absent Sonar analysis".
6. 2026-11-27 — red base at `5cb9cf0a`: {Wait for main to be fixed, Enqueue on the red base anyway, Close the PR
   unmerged} → "Close the PR unmerged".
7. Then, as free text, the operator overrode that: "Fix directly and land your changes. If all is green, there is no
   need for pr-reviews, use skip-bot-review as pr label for that."

## Rule check

Guard: `none` on this harness (unenforced).

- Shell file operations issued by the run: two `ls` at the start, one `rm -f` of superseded screenshots under
  `.plan/temp/screens/`, two `grep` over landing files, and one `gh pr edit` to apply the `skip-bot-review` label —
  the `ci` tool has no label verb, and the operator directed that label. Every other read and search used
  Read/Grep/Glob.
- Builds: 3 daemon-routed attempts (one bound expiry, one failure at 1249 s with a log outside this repository, one
  exit 1) and 8 in-process runs; every head pushed to the PR has a green in-process `verify` at that head.
- Executor form held for every script; no `gh`/`glab`/`mvnw` invocation for a routed action other than the label.
- Spec read: `ledger_worktree`, no `spec_differ`. `executor-skew`: `match` (0.1.2183).
- Two rule bends, both on the operator's word: the plan's "verification only" boundary (the base-branch repair) and
  the required-reviewer condition (the `skip-bot-review` label).

## Build server

Setup preflight `ready`. `daemon_longpoll` 3, `in_process_fallback` 8. Fallbacks: `module-tests` degraded after the
daemon's failure at 1249 s (reason unreadable); `quality-gate` after the 900 s bound; `module-tests` at its 1800 s
bound after 5684 green tests. No `killed` job.

## Residue

- `UC-ONB-4` and `UC-ENT-1` stay `partial` in the matrix; `UC-F-03` is retired; two `not verified` clauses (the model
  recommendation, the settings conflict notice) and the unavailable-provider variant await a mock that feeds them.
- `total_tokens` and `plugin_version` are `unknown` (nothing measured them); `sonar_open_new_issues` is the measured
  `0` of `0c2edf15`.
- The base-branch red is repaired in this PR: the six `plan.*` keys and `overview.needs.overdue` now exist in both
  packs, so the two catalogue tests are green at `main` after the merge.
