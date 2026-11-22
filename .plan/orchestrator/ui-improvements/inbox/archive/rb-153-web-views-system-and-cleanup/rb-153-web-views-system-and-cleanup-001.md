envelope_version=1
sender_type=plan
sender_id=rb-153-web-views-system-and-cleanup
epic=ui-improvements
kind=landing
created=2026-11-22T16:06:31Z

```landing-facts
schema=landing-facts/1
plan_id=rb-153-web-views-system-and-cleanup
epic=ui-improvements
pr=#532
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=4
review_body_findings_handled=4
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

`guard: none on this harness (unenforced)`.

## Deliverables

| # | Done-when | Verdict | Order | Test | Failing run |
|---|---|---|---|---|---|
| 1 | the same figures and selections through the new components (`SystemView.test.ts`, `system.spec.js`) | met | `test-first` | the view tests, copied onto `9dbaea80` | 18 failures naming the new controls (`maven-2026-11-22-125948.log`, job `fce8c3af414848f5b37a141ed1978d99`) |
| 1 | `lint:vocabulary` 0 for SystemView, the tag-class scan empty | met | `none` | `npm run lint:vocabulary` on the worktree (`npm-2026-11-22-111243.log`, no `SystemView.svelte` line) and the `class="…(badge|status-tag)"` scan (no match) | — |
| 1 | the text guard baseline unchanged | met | `none` | `pm-web-e2e/tests/text-guard-baseline.json` untouched; the e2e guard reports `SystemView (de): 0`, `(en): 0` | — |
| 2 | stop/stop-now/restart/upgrade/withdraw through the new dialogs; `Escape` and focus; the drain preselected; a second confirm ignored | met | `test-first` | the dialog tests, copied onto `9dbaea80` | the same base run fails them (`maven-2026-11-22-125948.log`) |
| 2 | no `z-index`/`role="dialog"`; leaves the layer allowlist | met | `none` | the scan (no match); `tokens.test.ts` without the entry | — |
| 3 | the five CleanupView behaviours, a second confirm ignored, Delete disabled with its reason | met | `test-first` | `CleanupView.test.ts`, copied onto `9dbaea80` | the same base run fails them |
| 3 | `lint:vocabulary` 0, no `z-index`/`role="dialog"`, leaves the allowlist | met | `none` | `npm-2026-11-22-111243.log` (no `CleanupView.svelte` line); the scan; `tokens.test.ts` | — |
| 4 | both views at 1440×900 and 390×844, `de`/`en`, both themes, the dialogs, screenshots kept, the contrast audit green | met | `none` | 4 baseline + 16 after view shots + 4 dialog shots under `.plan/temp/playwright-mcp/pm153-*.png`; `theme-readability.spec.js` "system and cleanup" green (`pm-web-e2e/maven-2026-11-22-140321.log`) | — |
| 5 | a fixture that does not parse fails at `9dbaea80` and passes | met | `test-first` | `vocabulary.test.ts` | `maven-2026-11-22-100233.log` (`expected [Function] to throw`) |
| 5 | each strengthened assertion and each set pin fails on mutation | met | `mutation-after` | mutation A (six lint assertions) and mutation B+C (fallback + three pins) | `maven-2026-11-22-114044.log` (6 failures), `maven-2026-11-22-115644.log` (7 failures); all reverted |
| 5 | `--write-baseline` byte-identical on an unchanged baseline, fails at `9dbaea80` | met | `test-first` | `vocabularyLint.test.ts` | `maven-2026-11-22-100233.log` (`"sele…"` vs `"butt…"`) |
| 5 | the header comment states the printed format | met | `none` | `scripts/vocabulary-lint.mjs:6-24` | — |

Per-head test runs: `package` at `ddb46144`+`2a9a2dfa` (job `aabae008574e4a0fbcc8d4f576e62e94`), at `573f4981` (`maven-2026-11-22-135241.log`), at `6aae0716`, at `799d6722` (after the merge, `maven-2026-11-22-144502.log`, 5424 tests), and `verify` at `f9b14c1d` (`maven-2026-11-22-151419.log`, 0 svelte-check errors). `verify -Pe2e` at `2a9a2dfa`, `6aae0716` and `799d6722` (`pm-web-e2e/maven-2026-11-22-145326.log`, 456 passed).

## Personas and skills

- Production code (D1–D3): `persona-implementer`; tests: `persona-module-tester` (D5) and `persona-integration-tester` (D4); self-review and PR triage: `persona-code-reviewer`.
- D1–D3 named `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`; D4 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`; D5 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:lint-config`. All exist.
- `skills added: pm-dev-frontend:javascript (DataTable's scroll region)` — the a11y fix touched a shared component, reason: the wide tables made the scroll wrapper a region WCAG 2.1.1 covers.

## Hypotheses

- H1 (`DataTable`'s column spec carries the unit and the split columns of the models table): **refuted** — `DataTable.svelte:3-12` carries `id/header/width/numeric` and its cells are strings; the status cells became text, the tokens stay one formatted string.
- H2 (`SystemView` sits on a page template and needs no cap of its own): **confirmed** — it wraps in `DashboardPage` (`SystemView.svelte:544`), which caps at 1440 px.

## Findings

Self-review round 1 (16 findings; all dispositions recorded):
- fixed: the `DataTable` scroll wrapper now takes `tabindex="0"` + the focus ring (axe serious, found by the e2e audit); the workers' column headers re-derive on a locale switch; the cleanup detail line no longer repeats the `KindMark`'s word; the release notes keep their new tab via `ExternalLink`; a second-confirm test for the stop; the workers' head sentence and the exact restart note asserted again; the rule catalogue's `transition: all` count.
- recorded, not changed (the design could not be met without a behaviour or contract change): a worker's state is a text cell in the `DataTable`; a worker's plan cannot be a `Link` (`WorkerMetric` carries no id); a cleanup item is a `CheckField`, not a `CheckGroup` (the per-row _Open_ act has no place in a group); no outcome `StatusLabel` (no such field); the workers'/jobs' head sentences are now accessible names of a `CountBadge`; the model retry/error badges and the recovering notice are detail text.
- pre-existing, named: the delete act's `{what}` was the English literal "N items" (fixed in `85195bc1` by the catalogue phrases); server-provided `detail` strings stay English in the German view.

CodeRabbit (4 findings, all handled in `85195bc1`): the German "items" literal; `modalWhen` read after an `await`; the missing `Finding.message`; `destructive` not forwarded to the act menu. Sourcery refused on quota (optional bot, no gate).

## Reviewers

| Reviewer | State | Head reviewed | History |
|---|---|---|---|
| coderabbit (required) | reviewed | `6aae0716` (`final_review_risk_coverage` names the commit) | triggered at 15:35; reviewed with 4 findings; all fixed and the three threads answered and resolved; `review_complete_after_handling` → no re-trigger on the later heads |
| sourcery (optional) | refused on quota (250k diff characters, next request in 3d22h) | — | triggered once; not a gate |

## Operator questions

1. "Base-sha evidence blocked: `worktree-create --detach` answers `executor_in_worktree: absent`" — options {Regenerate the base tree's executor (Recommended), Report the two clauses not evidenced, Stop}; **selected: "Regenerate the base tree's executor"**, then free text "you regenerate. call the script".
2. "The detached base tree now exists … Please run the project's own steward/generate step … Tell me which option applies when you are done." — options {Done — executor is there now, Cannot — report not evidenced, Stop}; **selected: "you regenerate. call the script"** (free text).
No other question was asked; there were no other operator messages.

## Rule check

- `guard: none on this harness (unenforced)` — nothing was enforced, so nothing is reported as "all kept".
- Rule breaks of the run's own actions: `ls` was used twice at session start (a resume check of `.plan/temp/rb-153-…`); `grep`/`tail`/`wc` were used on build logs and `cp`/`rm`/`mkdir` on `.plan/temp` and the base tree; one backtick pair in a `git commit -m` body was executed by the shell (the message was amended). One hand-run `generate_executor generate` produced the detached base tree's executor, on the operator's explicit selection (question 1/2) after `worktree-create` answered `absent` twice; the executor was generated from that tree's own 168 local scripts. The ad-hoc `lint:vocabulary` and `verify -Pe2e` runs used `--execution-mode in_process` so the assigned ports 21530/21531 reached the mock rather than the operator's default loop. `git worktree remove` was used for the base tree and the plan worktree once each was clean; no branch was deleted with `-D`.
- `executor-skew`: **match** (executor 0.1.2173, lane 0.1.2173) at session start; the executor resolve reports now carries 0.1.2183.
- Spec read: `spec_source=ledger_worktree`, no `spec_differ` marker.
- Restarts, killed sub-agents, re-dispatches: none; one self-review dispatch, no re-dispatch.

## Build server

Setup preflight: `ready`. Every routed build reported `mechanism: daemon_longpoll` (the module-test, the mutation and the merge gates); the ad-hoc e2e and lint runs reported `in_process_fallback` on purpose (ports/env). Fallbacks: one launch-fault-free run; the `pm-api` `EntryRecordsTest` failure on the first `verify` was a randomized-input flake, green on the rerun; one `pm-mock-runtime` timing flake the same way. No `killed` job. `runtime_stamp_reason` was not reported.

## Residue

- The four recorded design deviations above stay for D2/PLAN-141 or a later view plan.
- The `Tabs` adoption and `byState.ts`'s destructive roles are explained in the PR body but sit outside the spec's Expected Surface.
- The mailbox reported `mailbox_state: no_mailbox` (`never_delivered`) at all three reads: this plan has no delivered items, which is not the same as an unread mailbox.
- Sourcery's quota refusal is recorded; CodeRabbit's included review allowance is exhausted for the hour.
