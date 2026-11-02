envelope_version=1
sender_type=plan
sender_id=rb-37-web-accessibility-baseline
epic=ui-improvements
kind=landing
created=2026-11-01T15:14:26Z

```landing-facts
schema=landing-facts/1
plan_id=rb-37-web-accessibility-baseline
epic=ui-improvements
pr=#297
merge_state=open
cleanup_owed=true
deliverables_total=3
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:blocked,merge:pending
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-37 — Web: Accessibility Baseline and axe-core

Stopped before the merge: the required reviewer refused on quota three times and no review exists yet. PR #297 is open, CI is green on f27e1161, the Sonar read is a confirmed 0, and the next allowed trigger for `@coderabbitai review` is 2026-11-01T16:26:44Z.

## 1. Deliverables

### D1 — Escape and focus (not fully met)

- *e2e keyboard checks in `workspace.spec.js` per panel* — **met for the pause panel and the chat; not met as written for the overflow.** Order `test-first` for the two: the checks were written first and seen failing on the base code (log `maven-2026-11-01-093545.log:1075-1125` — `control-dialog-pause` and `chat-panel` each still present after Escape). They pass at the head (log `maven-2026-11-01-112458.log`). Order `none` for the phone overflow check: the sheet is `Dialog.svelte`, whose Escape predates this plan, so that check passed on the base code too (`maven-2026-11-01-093545.log:849`) and is a regression guard, never a fail-first test.
- *Browser check* — **met.** Order `none`. Eight before/after targets `PLAN-37-control-{de,en}-{light,dark}-{1440,390}-{before,after}.{png,yml}` under `.plan/temp/playwright-mcp/`, plus `PLAN-37-pause-open-…`, `PLAN-37-chat-open-…` and `PLAN-37-overflow-open-de-light-390-after.png`. Read in the browser with `browser_evaluate`: Escape closed the pause dialog (focus back on `action-pause`), the chat (`action-chat`) and the phone sheet (`bottom-more`); the pause dialog carries `aria-labelledby`.
- Commits: f2f0498, 187b404.

### D2 — Reasons, labels, sizes (met)

- *Unit tests on `WorkControlPanel` and `ChatPanel` for reason and label* — **met.** Order `test-first`. The reason tests failed on the base code (`maven-2026-11-01-095506.log:487-557`, `aria-describedby` null); the label tests failed against the base code with the label half reverted (`maven-2026-11-01-112108.log:447-485` — the chat input had no accessible name and the icon button no visible label). Both halves pass at the head (`maven-2026-11-01-112240.log`, 1022 passed, BUILD SUCCESS).
- *Browser check* — **met.** Same target set. `PLAN-37-control-de-light-1440-disabled.{png,yml}` shows `relay-metrics`' disabled Chat with its reason beside it; the button carries `title="no configuration of replan qualifies yet"` and `aria-describedby="reason-chat-relay-metrics"` → `"Chat ist nicht verfügbar: …"`. `NEBENHER` reads `nebenher` in the after target.
- Commits: 4aa92cb, f27e1161.

### D3 — axe-core audit (not fully met)

- *`PM_MOCK_WEB_PORT=20370 PM_WEB_PORT=20371 ./mvnw -pl pm-web-e2e verify -Pe2e` passes with the audit on every declared view* — **not met as written.** Order `none`. The head run is `maven-2026-11-01-112458.log`: 319 passed, **7 failed**, BUILD FAILURE. All 15 audit tests are green and the audit covers all 22 declared views with zero serious or critical issues. The 7 failures are the base's own, identical in id and in failure text to `maven-2026-11-01-093545.log`: `epic-view.spec.js:190` (tokens `340 k out`), `plan-view.spec.js:38`, `:154`, `:225`, `showroom.spec.js:88`, and the text guard on `PlanView` in de and in en. The audit passes; the command does not.
- *Browser check* — **met.** As D1.
- *Open-issue read at the pull request head* — **met.** `sonar read_open_issues --project plan-marshall_pm-mcp-clients --pr 297` at `f27e1161ccce97e8a929ee00a8c34b9788d3a176` (analysis revision equals head revision): `count_status: confirmed`, `open_issue_count: 0`, no rules left open, no dismissals.
- Commits: a89864e, 447beb5, f27e1161.

## 2. Personas and skills

- D1 and D3: `plan-marshall:persona-integration-tester`; D2: `plan-marshall:persona-implementer`; every self-review round and the PR triage: `plan-marshall:persona-code-reviewer`.
- Domain skills per deliverable: `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security`, `pm-dev-frontend:lint-config`, `pm-dev-frontend-cui:cui-javascript-project` (D1, D3); the same plus `pm-dev-frontend:css` and `pm-dev-frontend:jest-testing` (D2).
- `skills added: none` — the one file outside the spec's Expected Surface besides the lock file is a two-line catalogue entry in each of `pm-api/src/main/resources/messages/start.{en,de}.properties`, a resource file with no Java code.

## 3. Hypotheses

The spec carries no `HYPOTHESIS` labels; its claims are `OBSERVED`. Confirmed against the tree as it stands: `WorkControlPanel.svelte`, `ChatPanel.svelte`, `WorkControlPanel.test.ts`, `workspace.spec.js`, `pm-web-e2e/package.json` and `package-lock.json` all exist, and `pm-web-e2e/tests/helpers/a11y.js` was absent — created here. `pm-web-e2e/package.json` declared no axe package before this change (`package.json:24-32`), and `tests/helpers/a11y.js` is new.

## 4. Findings

Self-review round 1 (13 findings; fixed F2, F3, F4, F6, F8, F9, F11):

- F1 the required gate command does not pass — stands, reported above as not met.
- F2 the progress record cited the wrong log for the passing runs — fixed.
- F3 removing accessible names from the system view's work figures and the plan node's selection control — fixed: the names keep their descriptive sentence with the visible text in front of it, and the plan node carries its select affordance as hidden text of its own.
- F4 the label half of D2 had no failing-first evidence — fixed by a second failing-first run (`maven-2026-11-01-112108.log`).
- F5 no disabled-control evidence in a snapshot — fixed (`PLAN-37-control-de-light-1440-disabled.png`).
- F6 `aria-modal="true"` on an inline dialog that traps nothing — fixed (dropped).
- F8/F9/F11 stale JSDoc and comments, an unjustified `svelte-ignore` — fixed.
- F7 the IconButton label test proves DOM presence, not the media-query visibility; F10 the reason-only `title` asymmetry; F12 the audit runs de at 1440×900 only; F13 Escape discards a half-typed message — recorded as preferences, not fixed.

Self-review round 2: no new functional defect; its record and cosmetic items closed.

CI: green on f27e1161 — `build` (25, 26, quarkus-alignment, sonar-build, gate, config, check-changes), `integration-tests` (test, config, check-changes), `dependency-review`, `SonarCloud Code Analysis`. Sonar's own comment reports 0 new issues and a passed quality gate.

PR comments: three are this run's trigger comments and two are the bots' quota refusals; the Sonar quality-gate comment reports 0 new issues. No review comment exists to triage.

## 5. Reviewers

- `coderabbit` (required, quota-limited): triggered 2026-11-01T11:41:31Z on f27e1161 — **refused**, review limit reached. Re-triggered at the policy's 90-minute mark (13:13:33Z) — refused again. Escalated once with `@coderabbitai full review` (13:23:13Z) — refused. Re-triggered at 14:56:44Z — refused again. No review exists on any head; the standing authorization does not apply, because it requires a qualifying review. Next allowed trigger: 2026-11-01T16:26:44Z.
- `sourcery` (optional, quota-limited): triggered 11:41:53Z — refused (review budget, next in 1 day and 11 hours). Does not gate.

## 6. Operator questions

None asked. No question tool was used in this run.

## 7. Rule check

- `executor-skew`: **match** (executor 0.1.2036, lane 0.1.2036) — read before `resolve`.
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true` — the main checkout's copy is older; the ledger copy was read and obeyed.
- Guard: `guard: none on this harness (unenforced)`; `.plan/temp/guard.log` was not produced because no guard is installed.
- Shell file operations: none — every read and search went through the file tools.
- Builds: all through `plan-marshall:build-maven:maven`, never the wrapper directly. The first `verify` was routed to the daemon and failed at once: the daemon ran `mvn verify` (not `./mvnw`) in the main checkout and the process could not start (`job-logs/c1b4cbd01f5c4abbbe6e7f17a69ef38a.log`, exit -1). Every build after that ran with `--execution-mode in_process`, which the port-bound e2e commands require in any case because `PM_MOCK_WEB_PORT`/`PM_WEB_PORT` are shell-scoped to the invocation.
- One deliberate reversion: the D2 source was reverted by hand twice to capture the failing-first runs, then restored; no git restore, stash or rebase was used. Two builds and the full e2e ran more than once for that reason.
- Repository files were edited only inside the worktree. `.plan/` holds only `.plan/temp/`, the four `ci` body slots and this message.
- No question was asked and no rule was bent.

## 8. Build server

- Setup preflight: `ready`. The first routed build failed as described above; the fallback was `in_process`, recorded per build. No job was `killed`.
- `runtime_stamp_reason` was not returned; `plugin_version` is `unknown`.

## 9. Residue

- The required reviewer's quota is the one open gate. The PR is open and unmerged; the worktree, the branch and PR #297 remain. To finish: re-trigger `@coderabbitai review` after 2026-11-01T16:26:44Z, triage what it reports, then merge through the queue and clean up.
- The seven e2e failures are the base's own and are not touched here; they block D3's first Done-when as written until another plan fixes them.
- The axe audit runs the German interface at 1440×900 only, so an English-only gap in a label would not be caught.
- The icon-only control's visible label cannot be exercised on a built app: every `IconButton` on a shipped view sits in a bar that overflows only in the showroom, whose route a production build excludes. It is covered by `IconButton.test.ts` and by the media query.
