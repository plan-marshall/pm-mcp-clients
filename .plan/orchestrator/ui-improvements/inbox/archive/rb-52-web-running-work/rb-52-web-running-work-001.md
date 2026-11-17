envelope_version=1
sender_type=plan
sender_id=rb-52-web-running-work
epic=ui-improvements
kind=landing
created=2026-11-17T11:43:09Z

```landing-facts
schema=landing-facts/1
plan_id=rb-52-web-running-work
epic=ui-improvements
pr=#459
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=4
review_body_findings_handled=4
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-52 — Web: Running Work Across Projects

PR #459, merged in `d79cc5f13f0a43b87c819c1f7dd3a25120157a59` by the merge queue (merge-group run
44073849188, success). Branch `feature/52-web-running-work` deleted locally and remotely; worktree
`.plan/local/worktrees/52-web-running-work` removed; `main` pulled to the merge commit.

The spec was read from the ledger worktree (`spec_source: ledger_worktree`); no `spec_differ` marker
was reported, so the two copies agreed.

## Deliverables

### D1 — Rows and groups — not fully met (every Done-when met)

| Done-when | Order | Verdict | Test | Failing run | Commit |
|---|---|---|---|---|---|
| `overview.spec.js`: columns align at 1440 px; row menu is an icon button | `test-first` | met | `Overview View (UC-PLN-4) › aligns plan row columns at 1440 px and renders the row menu as an icon button` | `8f9e8cf4`, `pm-web-e2e/maven-2026-11-16-082226.log:819-833` | `91ddea7b` |
| (D6) rail item opens running work after ended work; reload of the unfilled `/overview` stays unfilled | `test-first` | met | `Overview View (UC-PLN-4) › the rail item opens running work after ended work was open, and a reload of the unfilled /overview stays unfilled` | `8f9e8cf4`, `pm-web-e2e/maven-2026-11-16-082226.log:839-862` | `91ddea7b` |
| (D6) no read or write of `pm_overview_show_ended` / `pm_overview_ended_page` | `test-first` | met | `OverviewView component › never reads or writes pm_overview_show_ended or pm_overview_ended_page in localStorage` | `8f9e8cf4`, `pm-web-app/maven-2026-11-16-081851.log:921-933` | `91ddea7b` |
| Browser check on `fully-onboarded` and `running-work` → `overview.running` | `none` | met | artifacts below | — | `8d26f028` |

Per-head test runs (module-tests, plus test-compile): `8d26f028` (`maven-2026-11-16-122447.log`),
`3b1d73a3` (`maven-2026-11-16-142157.log`), `b1a140cb` (`maven-2026-11-16-150339.log`), `d50c99f4` +
`22205088` (`maven-2026-11-17-063433.log`, `-072953.log`), `48f39137` (`-073141.log`), `c4c72b26` +
`e3bf986f` via the full gate, `5e3c67fb` (`maven-2026-11-17-094307.log`).

### D2 — One list, one count — met

| Done-when | Order | Verdict | Test | Failing run | Commit |
|---|---|---|---|---|---|
| One unit test: both groupings hold the same plans; the epic head count equals its rows | `test-first` | met | `OverviewView component › renders by-plan and by-epic from one plan list and derives the epic head count from its rows` | `91ddea7b`, `pm-web-app/maven-2026-11-16-100821.log:990-1006` | `1c4a1711` |
| Browser check (same targets) | `none` | met | artifacts below | — | `1c4a1711` |

The epic head is `{running} running · {staged} staged`, which `doc/design/shared/plan-overview.adoc:78`
and `:307` prescribe; `running` derives from the rendered rows, `staged` is the epic's own runtime
figure.

### D3 — Ended work and alongside — not fully met

| Done-when | Order | Verdict | Test | Failing run | Commit |
|---|---|---|---|---|---|
| `overview.spec.js`: **aligned ended-work columns** | `test-first` | **not met as written** | `Overview View (UC-PLN-4) › navigates to ended work, verifies outcome badges and pagination` | no failing run: the alignment assertions were added in `8d26f028` and the base run (`8f9e8cf4`, `pm-web-e2e/maven-2026-11-16-082226.log:388`) passed the test before they existed. The behaviour is machine-checked green at the head — `.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-17-084951.log`, 452 passed — but a green run is not `test-first` evidence. | `8d26f028` |
| `overview.spec.js`: the rail item opens running work after ended work was open | `test-first` | met | `Overview View (UC-PLN-4) › the rail item opens running work after ended work was open, and a reload of the unfilled /overview stays unfilled` | `8f9e8cf4`, `pm-web-e2e/maven-2026-11-16-082226.log:839-862` | `8d26f028` |
| Browser check (same targets, ended-work table) | `none` | met | artifacts below | — | `8d26f028` |

`deliverables_done` therefore counts 2 of 3: D3 is not fully met because one of its Done-when
clauses is not met as written.

### Browser-check artifacts

`.plan/temp/playwright-mcp/52-web-running-work-fully-onboarded-{en-light-1440x900,en-light-by-epic-1440x900,en-light-ended-1440x900,en-dark-1440x900,de-dark-1440x900,de-light-1440x900,de-light-390x844,de-dark-390x844,en-dark-390x844,en-light-390x844}-{before,after}.{yml,png}`

`.plan/temp/playwright-mcp/52-web-running-work-running-work-{en-light-1440x900,en-light-by-epic-1440x900,en-light-ended-1440x900,en-dark-1440x900,de-dark-1440x900,de-light-1440x900,de-light-390x844,de-dark-390x844,en-dark-390x844,en-light-390x844}-{before,after}.{yml,png}`

Checked in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844. Alignment read from bounding
boxes: plan rows at x = 553 / 610 / 747 / 883 / 1003 / 1147 / 1243 / 1371; nested epic rows at
566 / 623 / 734 / 870 / 990 / 1134 / 1230 / 1358; ended-table columns at 537 / 609 / 903 / 1095 /
1183 / 1295 on every row. The row menu is a 28 × 28 icon button. The `fully-onboarded` after set was
taken at 12:25 on 2026-11-16, after `8d26f028` (12:23); the `running-work` set was taken in this run
at the same head. No visual fix followed them.

## Personas and skills

| Step | Persona | Domain skills |
|---|---|---|
| D1 production code and its e2e | `plan-marshall:persona-integration-tester` | `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security` |
| D2 | `plan-marshall:persona-implementer` | `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security` |
| D3 | `plan-marshall:persona-integration-tester` | `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security` |
| Every self-review round and the PR triage | `plan-marshall:persona-code-reviewer` (with its adversarial-refute standard) | — |

Skills added for surface drift: none. `pm-web-app/src/lib/navigation/history.ts` and
`pm-web-app/src/lib/stores/runtime.ts` are not in the spec's Expected Surface, but both are the same
module whose skills were already loaded, and both were required by D6's reload/Back clause. The
Expected Surface is a hint, not a fence.

## Hypotheses

- HYPOTHESIS (paging): "the ended-work list can be paged by the runtime through an existing endpoint
  the client already calls" — **confirmed** at the merged head,
  `pm-web-app/src/lib/api/client.ts:3671-3676` (`getEndedWork(projectId, page)` →
  `/api/v1/projects/{id}/ended?page=N`), and the page's reader at `OverviewView.svelte`
  (`loadEndedWork`) — no new endpoint was needed.

## Findings

### Self-review (4 rounds, clear)

- Round 1 (10 items): D3's alignment evidence gap accepted and reported as not met as written; the
  `history.ts` docstring made false by the new write, and the missing unit test for it, fixed in
  `843578ea`/`3b1d73a3`; `history.ts` being outside the Expected Surface accepted; the epic head's
  `staged` figure **refuted** as a defect (`plan-overview.adoc:78`, `:307`); the four pre-existing
  assertions changed by the amendments accepted (two strengthened); no `$derived` refuted as a
  preference; the red e2e head traced to the four pre-existing `data-table` failures.
- Round 2 (4 nits): the vacuous reload test renamed, the replace-not-push intent and the
  `back_forward` arm pinned, `resetOpenedFromOutside` returned to the load's initial state, the
  evidence pointer corrected — all in `3b1d73a3`.
- Round 3 (2 nits, at the limit): a test for the reset's `initialMarked` arm (`b1a140cb`) and the D3
  evidence pointer in the run record corrected. The operator was asked whether to continue past the
  limit and chose one confirmation round.
- Confirmation round: both fixes verified non-vacuous; nothing open. Its out-of-scope note (the
  reset's `null` arm is untested) is recorded as residue below.

### PR comments (CodeRabbit)

| Finding | Severity | Disposition |
|---|---|---|
| `BottomBar.svelte`: a pending Running Work navigation replaces a newer destination | minor | fixed `22205088`, then reworked onto the store's epoch in `48f39137`; answered on the thread |
| `OverviewView.svelte`: a project change consumed before the list is available is lost | major | fixed `22205088` |
| `OverviewView.svelte`: Ended Work not reloaded when the address changes project | major | fixed `22205088` |
| `runtime.ts`: the guard missed an intervening navigation that returns to the same address | minor | fixed `48f39137` (navigation epoch, adopted routes counted) |
| `OverviewView.svelte`: nested plan title opened the epic | major | fixed `48f39137` (row and title open the plan; e2e helpers click the epic head) |
| `OverviewView.svelte`: a title-only plan matched two same-titled epics | minor | fixed `48f39137` (unique-title rule) |
| `OverviewView.svelte`: the grouping address dropped the displayed project | minor | fixed `5e3c67fb` |

Four review-body findings were reported by `review-gate` (`inline-1`…`inline-4`) and all four were
passed as dispositions; the three outside-diff findings carry no thread (a GitHub limitation) and are
recorded here and in the run's progress record. Every thread was answered with the fixing commit and
resolved.

## Reviewers

| Reviewer | Required | Outcome | Head reviewed | History |
|---|---|---|---|---|
| CodeRabbit | yes | reviewed | `b1a140cb`, `d50c99f4`, `48f39137` (marker `kind=reviewed`, `coveredCommitId` naming each head) | refused on quota at 17:19 on 2026-11-16; re-triggered at 90 minutes and escalated once (`@coderabbitai full review`); three reviews and two follow-up review bodies; all findings fixed |
| Sourcery | no | refused on quota | — | refused (2 days 19 hours) and never re-triggered; optional, does not gate |

CodeRabbit is marked `review_complete_after_handling` and holds qualifying reviews, so the currency
demand for later heads did not apply and no re-trigger was posted for them.

## Operator questions

1. "The self-review reached its 3-round limit. Round 3 reported two nits, fixed in b1a140cb,
   unconfirmed by a round. How should I proceed?" — options {One confirmation round (Recommended),
   Proceed to the PR now}; selected **One confirmation round (Recommended)**.
2. "The merge queue dequeued PR #459 on a Maven Central 429 in the merge-group quarkus-alignment job;
   every gate on e3bf986f holds. How should I proceed?" — options {Re-enqueue now (Recommended),
   Wait 15 minutes then re-enqueue, Stop and leave the PR open}; selected **Re-enqueue now
   (Recommended)**.

No free-text operator messages were given outside these two answers.

## Rule check

`guard: none on this harness (unenforced)` — nothing was enforced, so "all kept" is not claimed and
no guard-log line is reported. The run's own actions, counted by the run:

- **Executor only** — one break: an inline `python3 - <<EOF` heredoc used to inspect a scenario JSON
  early in this session; corrected by re-reading the file with the Read tool. Every script, build and
  CI action afterwards went through `.plan/execute-script.py`, the resolved `build` map and `ci`.
- **Shell** — several breaks, all disclosed in the run record: shell `ls` listings for
  `.plan/temp/**` (some with `|`, `;` and `echo` separators), shell `stat`, `wc -l` and `find` for file
  metadata/listing, shell `grep` on a piped `ls`, and two commands that chained two `git` calls with
  `;`. File contents were otherwise read and searched with the file tools.
- **Stay inside the repository** — two breaks: the installed lane text was read from
  `~/.config/opencode/skills/plan-marshall-runbook-lane/SKILL.md` (the skill tool truncated it) and two
  parent directories were listed there; and the daemon job logs under
  `~/.plan-marshall/marshalld/job-logs/` were read where a build result named them as its `log_file`.
  No credential file, config directory or environment dump was read, and no secret was printed.
- **`.plan/` state** — kept: writes went to `.plan/temp/`, to CI body slots the `prepare-*` verbs
  returned, and to the landing file. No file under `.plan/orchestrator/` was edited.
- **Git** — kept: nothing staged wholesale, no `stash`, `restore`, `checkout -- <path>`, `rebase` or
  forced push. The base branch was brought in by merge, twice.
- **Spec read** — `spec_source: ledger_worktree`, no `spec_differ`.
- **Executor skew** — `executor-skew` answered `match` (executor and lane both `0.1.2135`).
- **Restarts and re-dispatches** — one harness restart and several idle gaps/compactions, each
  recorded; four self-review sub-agents (rounds 1–3 and the confirmation round), the first of which
  was re-prompted after a compaction. Every step was resumed from the run record, never redone.

## Build server

- Setup preflight: `ready`.
- Builds, by mechanism: `daemon_longpoll` for every build (quality-gate ×8, module-tests ×2,
  test-compile ×1, pm-web-e2e ×3). `in_process_fallback`: none. `launch-fault-retry`: none.
- One routed `verify -Ppre-commit` at `8d26f028` hit the daemon's supervisory bound
  (`timeout_used_seconds 1098`, `timeout_source explicit`) at 18:14 min while `pm-web-app`'s
  `npm run test:ci` ran; surefire had 5102 passed / 0 failed up to the kill. The split form was run
  with a raised bound and went green.
- Several builds queued behind other plans' builds (the daemon's two machine-global slots were held by
  two other plans at one point; a second e2e run then timed out 23 `waitForApp` checks under that
  load). Re-run on an idle daemon, it reproduced only the four pre-existing failures.
- `killed` jobs: none.

## Residue

- **D3's aligned ended-work columns is not met as written** — the `test-first` evidence was never
  produced. The behaviour is green in the e2e suite at the head.
- The four pre-existing `data-table.spec.js:52` failures, red at the base commit, were fixed by `main`
  by the time the branch merged; the e2e suite was fully green (452 passed) on the merged tree.
- `resetOpenedFromOutside`'s `null` arm (a reset after a fresh, non-reload load) still has no test —
  noted by the confirmation round, out of the two fixes it confirmed, and not required by any
  Done-when.
- The run record's model field carries the landing-time stamp (`deepseek-v4.1-flash/high`); the
  record also documents an earlier session's switch Orchestrator (Argon) → Gemini 4 Argon (Low), whose
  provider ids were not recorded.
