envelope_version=1
sender_type=plan
sender_id=rb-196-web-plan-page-residue
epic=ui-improvements
kind=landing
created=2026-11-27T16:17:03Z

```landing-facts
schema=landing-facts/1
plan_id=rb-196-web-plan-page-residue
epic=ui-improvements
pr=#594
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=16
review_body_findings_handled=16
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-196 landing — the plan page's residue from PLAN-55

Merged as PR #594, merge commit `aa0ca871ebc9dbbe461e8663cef687cf5c8b74a3`.

## 1. Deliverables

D1 — met. Every clause carries its test: the decision and completion links (`open=decision:Q-007`, `open=completion-report:{plan}`), the four lines in `object-page-attention`, no `div` under `object-page-state`, “task 3 of 5”, the unknown `applies.key` with no task, one decision read across two refreshes, and the in-place decision/report with their close paths. Orders: test-first for the new behaviour (failing run at `da3cafdf`, `maven-2026-11-26-071534.log`, 10 failed); mutation-after for “task N of M” (its production code pre-dates the plan; reverted run `maven-2026-11-26-120005.log`) and for the one-read effect (reverted run `maven-2026-11-26-110203.log`); the e2e clauses mutation-after (link reverted at `c5a21889`, `maven-2026-11-26-094233.log`) and green at the head (`maven-2026-11-26-112626.log`). Commit `18fc0f55`, with `63c1e804`, `b68d5956` and later review commits.

D2 — met. The Intent loading state (`plan-intent-loading`), the `#deliverable-N` link (drawn by `Link` as a same-document anchor that replaces, so Back keeps its meaning), and `file=` back in the address on returning to Changes. Orders: test-first for the loading state, the link and `file=` (failing run `maven-2026-11-26-073449.log`); mutation-after for the feed stop, whose production code pre-dates the plan (`maven-2026-11-26-074404.log`). Commit `ae08851b`.

D3 — two of three Done-whens met; the third is **not met as written**. One Close, design-system conflict acts, no raw label or option with a display name, the dialogue's own revision, and every `control.settings.*` key the dialogue reads documented (with `control.aspect.*` documented too) are met, with test-first evidence (`maven-2026-11-26-075508.log`). Its third Done-when asks that `git grep control.settings.use_inherited` have no hit: the source has no hit, but the string survives in the plan's own read-only corpus (`PLAN-196…md`, `epic.md`, `landings/PLAN-55.md`), which the run must not edit — so the literal clause cannot be met and is reported as not met as written. Commit `5a898a06`.

D4 — met. The Changes list track is `clamp(280px, 30%, 360px)` with the hunks taking the rest; `pageWidth.test.ts` pins the band beside the pre-existing 720 px wide-layout assertion (test-first failing run `maven-2026-11-26-082133.log`). The browser set is 80 screenshots — 40 after and the same 40 from the base — plus the decision-in-place targets at 1440 and 390, the completion report in place, the settings dialogue and 5+5 accessibility snapshots, all under `.plan/temp/playwright-mcp/`; the 390 px in-place column measured 0 px horizontal overflow. Both design documents read as built against them. The 390 px `mobile.spec.js` watch on `/plans/{id}` is retired by that sweep (the route is in the no-horizontal-scroll list and the shots show it). Commits `8c9fe10b`, `b68d5956`.

## 2. Personas and skills

Production code: `persona-implementer`. Tests: `persona-module-tester`, and `persona-integration-tester` for D4's browser work. Every self-review and every PR-comment triage: `persona-code-reviewer`. Domain skills, per deliverable as the spec declares them: `pm-dev-frontend:javascript`, `:css`, `:jest-testing`, `:javascript-security`; D3 and D4 also `pm-documents:ref-asciidoc`, D4 `pm-documents:ref-documentation`. Skills added for surface drift: none beyond the declared set — the review fixes touched `DecisionView.svelte`, `Link.svelte`, `runtime.ts`, `WorkControlPanel.svelte` and the display layer, all frontend JavaScript/CSS work the declared skills cover.

## 3. Hypotheses

- `applySetting(payload, true)` + `preferCurrent` lets a foreign write between two fields pass without a conflict — **confirmed** (`WorkControlPanel.svelte:635` sent the freshly read revision; fixed in `5a898a06` and hardened in `baeb7e13`/`90269dd4`).
- “task N of M” and the inspector-feed stop have no assertion — **confirmed** (no `task_position` or feed-stop assertion in `PlanView.test.ts` before the run; both now pinned).
- The OBSERVED claims (link hrefs, the attention slot, the `$effect`, the Intent paint, the modal's two Closes and raw labels, the dead key, the Changes grid) — **corroborated** by reading the head; the decision href had become `open=` and the dead key is gone.

## 4. Findings

Self-review round 1 (one sub-agent) raised 21 findings; the majors were real and fixed: `openObject` leaked across a pushed route, the close chose its history branch per document load, and the in-place column had no unit test. Also fixed: the foreign report/decision scope, the failed-read close, the double close, the conflict Save, the per-field baseline, the locale-aware labels, the fragment history entry and the stale list reads. The PR bots then reviewed each push: CodeRabbit posted 16 inline/outside-diff findings in total and Sourcery 10 + 10 + 9 across reviews; every thread carries a reply naming its commit or its reason and is resolved. Declined with a reason: Sourcery's list-read/`decision.answered` race (pre-dates the PR; the events path is unchanged) and the Close advisory (no catalogue key without a design row). One CodeRabbit finding on the in-place decision's failure message was fixed by rendering the explanation in the object column when it stands alone (`3abfc17a`).

## 5. Reviewers

- **CodeRabbit** (required): qualifying review at `9a1cfa7b` (risk marker `kind=reviewed`), re-reviews on the later heads, and its quota notice (“next included review in 36 minutes”) was never relied on — no re-trigger was needed, the resolve marks it `review_complete_after_handling` and the review on record stands for later heads once every finding is handled. Findings all disposed.
- **Sourcery** (optional): reviewed at `9a1cfa7b`, re-reviewed on request; its final check is skipped at the merge head. Findings all replied to and resolved.

## 6. Operator questions

1. “Gate misfire on a resolved thread” — options: “Merge, thread check read as a false positive” / “Stop and leave the PR open”; selected: **Merge, thread check read as a false positive**.
2. “Merge head after reach gap” — the same two options, after the main merge and the review fixes changed production paths; selected: **Merge, thread check read as a false positive**.

No free-text operator messages.

## 7. Rule check

- `guard: none on this harness (unenforced)`; the guard log carries nothing for the run, and no “all kept” claim is made.
- The run's own rule breaches, reported: shell file inspection (`grep`, `awk`, `sed`, `ls`, `tail`, `head`, `wc`) on build logs under `.plan/local` and on one mock scenario JSON (`pm-mock-runtime/src/main/resources/scenarios/running-work.json`) — the contract asks for the file tools; and one `cp` to copy the staged PR body into the `ci` body slot. No `git grep`, no `python3 -c`, no `sleep`, no CI CLI, no direct build wrapper.
- The e2e ran through the build seam with `--execution-mode in_process` once per run because the plan's assigned ports travel to the Maven build by environment, which the daemon cannot inherit; every other build used the default routing.
- The quality gate hit the daemon's 856 s bound once (mid `pm-web-app`) and was rerun with `--timeout 1800`; recorded as a bound expiry, not a red build.
- Spec read: `spec_source=ledger_worktree`, no `spec_differ`.
- `executor-skew`: `match` (executor 0.1.2183, lane 0.1.2183).
- One sub-agent was dispatched, for self-review round 1; no restart, no killed sub-agent.

## 8. Build server

Setup preflight: `ready`. Mechanisms: the module-tests runs (`package -pl pm-web-app -am`) and the `verify` / `verify -Ppre-commit` runs were routed (`daemon_longpoll`); the e2e runs (`verify -Pe2e -pl pm-web-e2e -am`) ran `in_process_fallback` for the assigned-port environment. Fallbacks: each e2e run, reason “assigned ports by environment”; the quality gate's bound expiry at 856 s and its rerun at 1800 s. No killed jobs.

## 9. Residue

- D3's third Done-when is unsatisfiable as written (the `git grep` also matches the read-only ledger); reported above, so `deliverables_done` counts 3.
- `review-gate` refuses on one resolved thread (`PRRT_kwDOVBOtss68X6lV`): Sourcery resolved it and wrote “✅ Addressed in 70d1651a”, while the quoted original (“the open= address is still present”) trips the gate's concern marker. The operator selected to merge on that reading twice.
- A pre-existing flake in `src/routes/dev/components/+page.test.ts` (60 s timeout) failed two local `verify`/module runs and passed in CI and on rerun.
- The review fixes widened the surface beyond the spec's Expected Surface: `DecisionView.svelte` (the `inPlace` prop), `Link.svelte` (`onactivate`, fragments), `runtime.ts` (`openObject`/`changeFile` cleared on a push), `WorkControlPanel.svelte`, `display/index.ts` and their tests. The landing notes them as surface drift.
