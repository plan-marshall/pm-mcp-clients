envelope_version=1
sender_type=plan
sender_id=rb-08-use-case-documents
epic=ui-improvements
kind=landing
created=2026-10-29T11:42:35Z

```landing-facts
schema=landing-facts/1
plan_id=rb-08-use-case-documents
epic=ui-improvements
pr=#259
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=unknown
step.sonar-roundtrip.open_issue_rules=unknown
```

guard: none on this harness (unenforced)

# PLAN-08 — Use-Case Document Changes of Both Reviews

Landed as `pm-mcp-clients` PR #259, merge commit `eba84c17b313535be5c0de72d6d3da314ad7b9a7`. The main change landed first in `plan-marshall-documentation` PR #128, squash-merged at 2026-10-29T07:16Z as `24eabd4`, so the anchors this PR links exist on `main` there.

## 1. Deliverables

| Deliverable / Done-when | Verdict | Order | Evidence |
|---|---|---|---|
| **D1** — both use cases exist with status `DRAFT`, follow the template of `UseCases.adoc`, are listed in the index | met | `none` | `plan-marshall-documentation` `doc/use-cases/03-plans.adoc` UC-PLN-7 (after UC-PLN-6) and UC-PLN-8, `Status:: `DRAFT``, template parts in order (Status, Actor, Goal, Trigger, Preconditions, Flow, Variations, Outcome, Changes, Requirements, Design); index row `UC-PLN` in `doc/UseCases.adoc` names both with `DRAFT`. Commits `6c0d788`, `700f6c3`, `c0e939f` |
| **D2** — each variation is in its use case, neutral between the clients | met | `none` | UC-PLN-2 (several open, ordered; own answer confirmed in words), UC-PLN-4 (a script asks for the same picture, in the terminal client), UC-PRJ-1 (a step only the machine's terminal can do), UC-ONB-1 (only the onboarding until onboarded). Commits `9a6b5c`, `700f6c3` |
| **D3a** — the pull request records the operator's agreement or the requested changes; the changed modules pass `doc-review` | met | `none` | The agreement is recorded in the PR body of #259 ("Agree as written"); `doc-review`'s checker `trace.py` reported the same findings as the baseline before and after every edit batch, and `asciidoc validate` stayed at the baseline for the three changed modules. Commits `06a1dc2`, `2f6ac37`, `5fd1dea`, `700f6c3`, `c0e939f`, `0b38cc3` |
| **D3b** — the open-issue read at the pull request head is recorded (`unknown` for a failed read, never `0`); confirmed `0` or every open issue dismissed | met under the plan's docs-only exception | `none` | The Sonar comment on #259 records `undecidable (docs-only skip)`, `analysis_revision_unavailable`, `open_issue_count: unknown`; the head changes only `doc/**` `.adoc` files, `build / conclusion` passed and `build / sonar-build` is skipped, which the spec's Standing Constraints name as the expected outcome that does not block. Re-read at every head |

No Done-when names a test and none carries a "fails at `{sha}`" clause, so no Done-when is graded `test-first` or `mutation-after`.

Per-head test runs (local `verify -Ppre-commit`, all green): `06a1dc2` 2177 tests, `2f6ac37` 2177, `a05aa24` 2208, `3aceb315` 2208. The project publishes no `module-tests` and no `test-compile`, so the gate canonical is the complete gate.

## 2. Personas and skills

- Persona per deliverable: `plan-marshall:persona-documenter`, loaded as a standard before D1 and applied through D3; `persona-code-reviewer` for every self-review round and for triaging the PR comment.
- Domain skills per deliverable (all three, as the spec declares): `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- No skill was added for surface drift: the design files touched (`doc/design/README.adoc`, `doc/design/shared/plan-overview.adoc`) are both named in the spec's Expected Surface.

## 3. Hypotheses

- HYPOTHESIS "UC-PLN-7 and UC-PLN-8 do not exist yet" — **confirmed**: `plan-marshall-documentation` `doc/use-cases/03-plans.adoc` defined UC-PLN-1 to UC-PLN-6 only before the change (the file ended at UC-PLN-6, `:310`).
- The OBSERVED claims were re-read and hold: the review proposals exist (`doc/claude_review/use-cases/proposals.adoc#documents`, `doc/claude_review/cli/use-cases/proposals.adoc#documents`), and the use cases live in `../plan-marshall-documentation/doc/use-cases/`.

## 4. Findings

Self-review (3 rounds, `persona-code-reviewer` sub-agent, report-only):

- **F-01** D3's evidence absent (no PR yet) — closed by the PR body of #259 carrying the agreement, the `doc-review` result and the Sonar read.
- **F-02** UC-PLN-7 named a credential and a cleanup that the closed `needs_operator` list does not carry — fixed by a `Changes::` line and a `Changes to the Specification` part in `03-plans.adoc` (`700f6c3`).
- **F-03** the `operator_wait` kind was omitted — fixed (`700f6c3`).
- **F-04** UC-PLN-7's `Design::` omitted the terminal client's design — fixed (`700f6c3`).
- **F-05** the web-app phrase is a hybrid form — kept: a shared design exists, no web design document and no web package exists to link; recorded in the PR body.
- **F-06** the terminal variation named the prompt and the badge — fixed to neutral wording (`700f6c3`).
- **F-07** the machine-readable variation of UC-PLN-4 named no client — fixed ("in the terminal client", `700f6c3`).
- **F-08** UC-ONB-1's quoted variation lost its label — kept as prose, because that use case's `Variations` is a paragraph; recorded in the PR body.
- **F-09** status handling — kept `DRAFT`, because D1's Done-when requires that status; the flip to `AGREED` is a separate act.
- **F-10** the design links depend on the sibling landing first — done: #128 merged before #259.
- **F-11** the change record omitted the requirement and the machine scope — fixed (`c0e939f`).
- **F-12** the two client designs were not named in the change record — fixed (`c0e939f`).
- **F-13** UC-PLN-7 said `Requirements:: none yet` while PM-ARCH-11 serves it — fixed: PM-ARCH-11 and PM-SEC-7 (`c0e939f`).
- **F-14** no review of the current head (both bots on quota) — open at the round limit; resolved through the standing review authorization and the operator's enqueue selection (see 5).
- **F-15** the PR body overstated the UC-ONB-1 variation — fixed in the body.

PR review comments: one inline finding from CodeRabbit ("do not map the overview rows to the items of UC-PLN-7") — a real accuracy defect; fixed in `2f6ac37`, replied to and resolved. Every other comment on the PR is a bot quota notice or this run's own comment.

## 5. Reviewers

| Reviewer | Required? | Outcome | Head reviewed | Trigger / wait history |
|---|---|---|---|---|
| `coderabbit` | yes (quota-limited) | reviewed twice; refused four times | `06a1dc2` (06:45Z) and `2f6ac37` (08:30Z, `coveredCommitId` marker, `kind=reviewed`, no actionable comments) — **not** `3aceb315` | triggered 21:42Z, 06:40Z, 06:56Z, 08:27Z, 08:58Z, 10:29Z, and after the last push; refusals at 21:42Z, 06:56Z, 08:58Z, 10:29Z; re-triggered never sooner than 90 minutes after each refusal |
| `sourcery` | no (optional, quota-limited) | refused (own 7-day budget, ~4 days) | none | triggered 21:42Z; refused |

**Standing review authorization** (CodeRabbit is `quota_limited`): a qualifying review exists on a PR head (`2f6ac37`); its one finding is fixed in a pushed commit and its thread is resolved; it was re-triggered after every push and every refusal is recorded; and every other gate condition holds on the current head `3aceb315` — CI green, local `verify -Ppre-commit` green (2208 tests), all comments handled. The operator then selected **"Enqueue now"** in a question that named that exact gap.

## 6. Operator questions

1. "How should I proceed?" on the executor skew (executor 0.1.2021 vs lane 0.1.2028) — options *Regenerate executor, then resume* / *Stop as blocked* → free-text answer "already regenerated"; the re-check answered `match` (0.1.2028).
2. "Do you agree to these drafts as written?" — options *Agree as written* / *Agree with the changes I describe* / *Do not agree yet* → **Agree as written**.
3. "How should the sibling PR be handled?" — options *Open it with gh* / *You open and merge it* / *Reference it without a PR* → **Open it with gh**; a later free-text answer "check coderabbit noq" did not settle the merge half.
4. "Should I merge it?" (sibling #128) — options *Merge #128 with gh* / *Leave #128 to me* → **Merge #128 with gh**.
5. "May I enqueue #259 into the merge queue?" — options *Enqueue now* / *Wait for a review of this head* / *Stop before the merge* → **Enqueue now**.

Free text: "already regenerated" (Q1), "check coderabbit noq" (between Q3 and Q4).

## 7. Rule check

- **Guard:** `guard: none on this harness (unenforced)` — no guard log exists at `.plan/temp/guard.log`; nothing was enforced, so this section reports the run's own actions rather than a log.
- **Rule bend, authorized twice:** `gh` was used for the sibling repository's PR — `gh pr create` and `gh pr merge --squash` on `plan-marshall/plan-marshall-documentation`. This lane's `ci` surface cannot target another checkout; the operator selected "Open it with gh" and later "Merge #128 with gh".
- **Shell file operations (rule breaks):** the run issued `ls` (sibling repository listings), `wc -l`, `grep` (build logs and a use-case file), `grep -c`, `which mvn`, `date` and `git -C … status/log/diff/rev-list` through the shell. Reading and searching repository files through the shell is what section 1 forbids; the counts above are the run's own, not a log's.
- **Build routing:** the first routed build failed with `execution_failed` (exit -1, 0 s) because the relative `--project-dir` was re-resolved inside the worktree, where that path does not exist; the absolute worktree path was used for every build after it. One routed run hit the daemon's supervisory bound (`timeout_used_seconds: 731`, `timeout_source: learned`, 1220 tests passed at that point) and the rerun with `--timeout 1200` passed — recorded, not blind-retried.
- **Restarts:** one server restart at 2026-10-29T09:23Z, mid-wait. The lane text was re-read then, the progress file was re-read, and no completed step was repeated.
- **Spec read:** the setup-time `resolve` read the spec from the **main checkout** (`spec_source: main_checkout`, no `spec_differ` marker); the landing-time `resolve` read the **ledger worktree** copy (`spec_source: ledger_worktree`), which is the copy the epic landed on `main` during the run.
- **`executor-skew` verdict:** `skew` at the session start (0.1.2021 vs 0.1.2028) → blocked, operator asked, operator regenerated; re-check `match` (0.1.2028). The landing-time executor reports 0.1.2032, so the plugin moved again during the run; no skew check was run against that version.

## 8. Build server

- Setup preflight: `ready` (registered, version 1). No degradation, no `in_process` fallback.
- Every build was routed (`requested=auto, resolved=routed, mechanism=daemon_longpoll`). One bound expiry (above) with the raised-timeout rerun; no `killed` job.
- `runtime_stamp_reason` was not reported; `plugin_version` is `unknown`, which is the resolve result's own value.

## 9. Residue

- **Open at the self-review round limit:** F-14 (no review of the final head). It is the state the standing authorization and the operator's selection cover; it is reported here rather than closed as "clear".
- **CodeRabbit has not reviewed `3aceb315`** (the merge of `origin/main` `53718209` into the branch); its review credit resets hourly and its next window was ~11:59Z.
- **Sourcery** did not review (optional; its own 7-day budget is spent for ~4 days).
- **`plan-marshall-documentation` trace drift:** its checker now reports 18 broken watch anchors that come from `pm-mcp-core`'s new commits (the unchanged sibling `main` checkout reported 19 on the same day), none in a file this change touches.
- **Follow-ups the drafts themselves name:** UC-PLN-7 and UC-PLN-8 stay `DRAFT` until an explicit agreement act flips them to `AGREED`; the `Changes to the Specification` part then moves into the requirements and specifications (PM-ARCH-11 and the runtime-model specification), and the two client designs gain the `credential` and `cleanup_due` kinds.
