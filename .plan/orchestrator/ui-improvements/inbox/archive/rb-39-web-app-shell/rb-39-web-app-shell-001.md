envelope_version=1
sender_type=plan
sender_id=rb-39-web-app-shell
epic=ui-improvements
kind=landing
created=2026-10-30T08:34:26Z

```landing-facts
schema=landing-facts/1
plan_id=rb-39-web-app-shell
epic=ui-improvements
pr=#274
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
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

**D1 — Shell split**
- `chrome.spec.js`: three groups, the counts the header carried, every route inside the shell — met, order `test-first`, tests "draws three rail groups with the counts the header carried", "renders every route of the rail and the top bar inside the shell"; failing run `maven-2026-10-29-190928.log` (job `acbf3e9eb81549bc8e6ec59d6eb9658b`) "✘ 8 … (5.5s)"; commit `4ac14b49`, green at every later head (last e2e run at `8225582e`).
- Browser check (`fully-onboarded`, the ten routes, rail groups/counts, at 1440×900 and 390×844, `de`/`en`, light/dark, mock stop/restart, before+after) — met, order `none`. Artifacts under `.plan/temp/playwright-mcp/`: `39-shell-before.{png,yml}`, `39-shell-after-{de,en}-{light,dark}.{png,yml}`, `39-shell-de-light-<route>.yml` (ten routes), `39-phone-after-*`, `39-live-after-*`.

**D2 — Phone**
- `mobile.spec.js`: four labelled items, no horizontal scroll on any route at 390 px — met, order `test-first`, tests "carries four labelled destinations in the bottom bar" (also asserts the 3/5 counts), "keeps every route without horizontal scroll at 390 px", "keeps the start and decomposition pages without horizontal scroll at 390 px"; failing run `maven-2026-10-29-190928.log` "✘ 84 … (5.4s)", "✘ 85 … (10.5s)"; commit `4ac14b49`, green at `8225582e`.
- Browser check — met, order `none`: `39-phone-after-{de,en}-{light,dark}.{png,yml}`, `39-phone-after-en-light-more.{png,yml}`, `39-phone-after-en-light-pair.{png,yml}`; overflow measured 0 on every route.

**D3 — Live indicator**
- Unit test drives the stream states; e2e stops and restarts the mock — met, order `test-first`, tests `src/lib/api/streamState.test.ts`, `src/lib/shell/LiveIndicator.test.ts`, `chrome.spec.js` "turns amber when the mock stops and live again when it restarts", "reaches Offline after the retries and comes back on Try again"; failing run `maven-2026-10-29-191719.log` (job `d27a380117354043b8788d3ccdd3418e`) "FAIL src/lib/api/streamState.test.ts … 4 tests failed"; commit `4ac14b49`, green at `8225582e`.
- Browser check — met, order `none`: `39-live-after-connected.{png,yml}` (Live), `39-live-after-reconnecting.{png,yml}` (Reconnecting…), `39-live-after-offline.{png,yml}` (Offline + Try again), `39-live-after-retried.{png,yml}` (Live again).
- Open-issue read at the PR head — met, order `none`: `sonar read_open_issues` at `8225582e`, `confirmed`, 0 open new-code issues; `sonar-gate` `merge_allowed: true`.

Per-head test runs (`verify -Ppre-commit -pl pm-web-app`: 946 unit tests; `verify -Pe2e -pl pm-web-e2e`: 305 browser tests; `verify -pl pm-api`: 655): `4ac14b49`, `4dbfe130`, `3f4b274`, `9bfa960`, `6d3eec32`, `92a02cf`, `36f570ab`, `80f00b60`, `c01b358f`, `8225582e`; the full `verify` at `6d3eec32`, `9bfa960`, `36f570ab`, `80f00b60`, `8225582e` (all green). The e2e module was green at `4ac14b49`, `9bfa960`, `6d3eec32`, `92a02cf`, `36f570ab`, `80f00b60`, `c01b358f`, `8225582e`.

## Personas and skills

- D1: `persona-integration-tester`, skills `pm-dev-frontend:javascript`.
- D2: `persona-integration-tester`, skills `pm-dev-frontend:javascript`.
- D3: `persona-implementer`, skills `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- `persona-code-reviewer` for both self-review rounds and every PR comment.
- Skills added for surface drift: none named beyond those; the responsive fixes in shared components and views used the loaded `pm-dev-frontend:javascript`/`css`.

## Hypotheses

The spec carries no `HYPOTHESIS` lines; its Claim Labels record one OBSERVED claim (corroborated at `cc5794c9`) and the verify-first clause. Re-read at the head: `+layout.svelte` held the header and no `lib/shell/` existed (confirmed before D1); `+layout.svelte` wired the favicon to `openDecisionsCount` (kept); the container components existed (used, not rebuilt); the four `system.*` lifecycle states stayed out of the machine pill.

## Findings

Self-review round 1 (sub-agent, diff `origin/main...HEAD`): 8 open items — stream `offline` unreachable on an accepted-then-ended stream, stale "Live" on teardown/401/revoke, amber LAN pill, unguarded bottom bar, plain-text Try again, ⌘K on a disabled trigger, `aria-pressed` with a state-dependent name, mock-restart helper path/timeout/recovery. All fixed in `4dbfe130`, except the items answered below. Round 2: 19 items — the phone frame controls lost on a page with no destination, the revoked-stream state, a dead LAN rule, a stale test name, the descriptor write before the health check, and the recovery that could mask a failure; all fixed in `1c305e6`. Round 3 was not dispatched: every remaining item is either a reporting obligation or a decision answered below.
- Answered, not fixed: hand-rolled navigation rows (they are navigation list rows, not plain text buttons); the brand absent below 768 px (the design's phone frame keeps path and search, the mark lives in the rail head); `isOffline` kept as store state (the pre-existing request-failure flag, no longer painted); the phone "Inbox" item deferred with its route to PLAN-49.
- Sonar at the head: `typescript:S7776` (nav.ts) fixed in `36561cc`; `java:S1192` fixed in `36f570ab`; `java:S3358`/`typescript:S2486` fixed in `8225582e`; re-read at `8225582e` confirmed 0.
- CI: two bound expiries of the daemon's 300 s default (`b68af95e`, then re-run with `--timeout 1800`); one job terminated by the run itself when its source changed mid-run (`99c2c983`, recorded and re-run); one flaky integration test (`ConsoleNoticeIT.decisionOpenedNotice`, pm-operator, merged from main) failed once and passed on `ci checks rerun`; the e2e suite showed two flaky failures in one run (theme-readability mobile EpicView, revoked text guard) and was green on the re-run.

## Reviewers

- `coderabbit` (required, quota-limited): refused on quota at 21:42Z, 23:45Z, 00:42Z, 06:41Z, 08:01Z (all recorded); re-triggered after the 90-minute policy at 23:00Z; reviewed `6d3eec3` at 00:21Z (marker `kind=reviewed`, `coveredCommitId=6d3eec3`) with 6 findings + follow-up + nitpick, and `80f00b60` at 07:20Z with 4 findings + follow-up + nitpick. Every finding was fixed (commits `9bfa960`, `c01b358f`) or answered, every thread resolved. Standing authorization used for the merge at `8225582e`: qualifying review on a PR head, every finding handled, re-trigger refused at 08:01Z, all other gates green on the head.
- `sourcery` (optional): refused on quota ("4 days and 1 hour"); gates nothing.

## Operator questions

1. "The e2e module build (a Standing Constraint of the plan) fails one pre-existing test on a defect from origin/main, not from this branch. How should I proceed?" — options: {Stop and leave the PR open | Merge, record the failure | Fix the mock fallback here}; selected **Fix the mock fallback here** (commit `92a02cf`).
2. "May I merge PR #274 at 8225582e, with the required reviewer's last review on an earlier head (standing authorization)?" — options: {Merge with the standing authorization | Wait for a review of the head | Do not merge}; selected **Merge with the standing authorization**.
No other questions; no free-text operator messages.

## Rule check

- guard: none on this harness (unenforced). The run's own actions: repeated shell reads of repository files and logs (`grep`, `sed`, `ls`, `cat`, `tail`, `head`, `awk`) instead of the file tools — a rule break, recorded here; write redirects into `.plan/temp/` (three comment captures, one earlier patch check); `kill`/`pkill` of the run's own leftover processes (a vite server, a mock, playwright Chrome, and one stale e2e run I terminated); one server restart mid-run with the lane text re-read at 01:20 and recorded; `git merge` of two moving base commits (allowed); no stash, restore, rebase, forced push or `git branch -D`. No guard log exists for this harness.
- `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs).
- `executor-skew`: `match` (executor 0.1.2032, lane 0.1.2032).

## Build server

- Setup preflight: `ready`; every build routed (`mechanism=daemon_longpoll`), no degraded fallback, no `executor_absent_from_worktree`.
- Two bound expiries at the daemon's 300 s default early in the run (recorded); later long builds raised the bound with `--timeout` and completed.
- One killed job: the e2e run I terminated when its source changed mid-run (`99c2c983`), recorded and re-run.
- No `runtime_stamp_reason`; `plugin_version` resolved as `unknown`.

## Residue

- The phone bar ships Running work, Decisions, Reports, More; the attention inbox item and its route arrive with PLAN-49, so no control points at a page that does not exist.
- The search trigger is present and disabled with its reason; PLAN-46 builds the palette and enables it.
- The browser evidence lives under the main checkout's `.plan/temp/playwright-mcp/` (the worktree was removed after the merge).
- One integration test proved flaky in CI (`ConsoleNoticeIT.decisionOpenedNotice`) and passed on rerun; two theme-readability e2e tests were flaky in one run and passed on re-run.
- `isOffline` remains a write-only store field; the suites at head are green with it.
