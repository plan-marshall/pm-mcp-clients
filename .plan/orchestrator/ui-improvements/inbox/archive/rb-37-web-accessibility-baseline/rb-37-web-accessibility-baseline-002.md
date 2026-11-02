envelope_version=1
sender_type=plan
sender_id=rb-37-web-accessibility-baseline
epic=ui-improvements
kind=landing
created=2026-11-01T23:17:38Z

```landing-facts
schema=landing-facts/1
plan_id=rb-37-web-accessibility-baseline
epic=ui-improvements
pr=#297
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-37 — Web: Accessibility Baseline and axe-core

Merged as `c5bfb1ca79cf891c311db8c7ed29b0b13247fed6`. The worktree, the local branch and the remote ref are gone and the main checkout is on `main` at the merge commit.

## 1. Deliverables

### D1 — Escape and focus (not fully met)

- *e2e keyboard checks in `workspace.spec.js` per panel* — **met for the pause panel and the chat; not met as written for the overflow.** Order `test-first` for the two: written first and seen failing on the base code (`maven-2026-11-01-093545.log:1075-1125`, `control-dialog-pause` and `chat-panel` each still present after Escape). Order `none` for the phone overflow sheet: it is `Dialog.svelte`, whose Escape predates this plan, so that check passed on the base code too (`maven-2026-11-01-093545.log:849`) and is a regression guard.
- *Browser check* — **met**, order `none`. Eight before/after targets `PLAN-37-control-{de,en}-{light,dark}-{1440,390}-{before,after}.{png,yml}` under `.plan/temp/playwright-mcp/`, plus `PLAN-37-pause-open-…`, `PLAN-37-chat-open-…`, `PLAN-37-overflow-open-de-light-390-after.png` and `PLAN-37-control-de-light-1440-disabled.{png,yml}`. Read with `browser_evaluate`: Escape closed the pause dialog (focus back on `action-pause`), the chat (`action-chat`) and the phone sheet (`bottom-more`); the disabled Chat of `relay-metrics` carried `title` and `aria-describedby` → `"Chat ist nicht verfügbar: …"`; `NEBENHER` reads `nebenher`.
- Commits: f2f0498, 187b404, 21640dc.

### D2 — Reasons, labels, sizes (met)

- *Unit tests on `WorkControlPanel` and `ChatPanel` for reason and label* — **met**, order `test-first`. Reason: failing on the base code (`maven-2026-11-01-095506.log:487-557`, `aria-describedby` null). Label: failing against the base code with the label half reverted (`maven-2026-11-01-112108.log:447-485` — the chat input had no accessible name, the icon button no visible label). Green at the head (`maven-2026-11-01-112240.log`, 1022 passed).
- *Browser check* — **met**, as D1.
- Commits: 4aa92cb, f27e1161.

### D3 — axe-core audit (not fully met)

- *`PM_MOCK_WEB_PORT=20370 PM_WEB_PORT=20371 ./mvnw -pl pm-web-e2e verify -Pe2e` passes with the audit on every declared view* — **not met as written**, order `none`. At the merged head the run is `maven-2026-11-01-223530.log`: 326 passed, 7 failed, BUILD FAILURE. All 15 audit tests are green and the audit covers all 22 declared views with zero serious or critical issues. The 7 failures are the base's own — identical ids and identical failure text to `maven-2026-11-01-093545.log`: `epic-view.spec.js:190`, `plan-view.spec.js:38/:154/:225`, `showroom.spec.js:88`, and the text guard on `PlanView` in de and in en. The audit passes; the command does not.
- *Browser check* — **met**, as D1.
- *Open-issue read at the pull request head* — **met**. `read_open_issues --project plan-marshall_pm-mcp-clients --pr 297` at `f5e00490cedec1f5ac43f43aa744957ee1e30ead` (analysis revision equals head revision): `confirmed`, count `0`, no rules left open, no dismissals.
- Commits: a89864e, 447beb5, f27e1161.

## 2. Personas and skills

- D1 and D3: `persona-integration-tester`; D2: `persona-implementer`; every self-review round and the PR triage: `persona-code-reviewer`.
- Per deliverable: `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security`, `pm-dev-frontend:lint-config`, `pm-dev-frontend-cui:cui-javascript-project`; D2 adds `pm-dev-frontend:css` and `pm-dev-frontend:jest-testing`.
- `skills added: none` — the only file outside the spec's Expected Surface besides the lock file is a two-line catalogue entry in each of `pm-api/src/main/resources/messages/start.{en,de}.properties`, a resource file with no Java code.

## 3. Hypotheses

The spec carries no `HYPOTHESIS` labels; its claims are `OBSERVED`. Confirmed against the tree: `WorkControlPanel.svelte`, `ChatPanel.svelte`, `WorkControlPanel.test.ts`, `workspace.spec.js`, `pm-web-e2e/package.json` and `package-lock.json` exist; `tests/helpers/a11y.js` was absent and is created here; `package.json` declared no axe package before this change.

## 4. Findings

Self-review round 1 (13 findings): F2 (record), F3 (names removed from the system view's figures and the plan node), F4 (label half without failing-first evidence), F6 (`aria-modal` on a dialog that traps nothing), F8/F9/F11 (stale docs, an unjustified `svelte-ignore`) — all fixed; F5 (no disabled-control evidence) fixed by the disabled screenshot; F1 (the gate command) reported as not met; F7/F10/F12/F13 recorded as preferences.

Self-review round 2: no new functional defect.

CodeRabbit reviewed f27e1161 with six inline findings and a follow-up, all fixed in 21640dc: Escape now closes only the topmost layer; the control dialog takes the focus when it opens and hands it to the first act that can run when a confirm has disabled the opener; the tablist's arrow walk starts from the selected tab; the decision chat answers against the decision it was opened on; the onboarding step line takes its Tab stop only while it overflows; the audit logs the issues it does not gate on. Two nitpicks were answered rather than changed — the axe gate runs the browser's theme (the contrast matrix already gates both themes at three viewports), and the icon-button label cannot be exercised on a built app because every `IconButton` sits in a bar that overflows only in the showroom, whose route a production build excludes.

CI green on the merged head `f5e00490` (Maven Build 25/26, sonar-build, quarkus-alignment, gate, config, check-changes; Integration Tests test/config/check-changes; Dependency Review; SonarCloud Code Analysis). Sonar's own comment reports 0 new issues and a passed quality gate.

## 5. Reviewers

- `coderabbit` (required, quota-limited): reviewed `f27e1161` at 18:27Z and `21640dc` at 20:53Z; every finding fixed and answered. It never reviewed the heads that followed — `6a0cdeb` (merge of `origin/main`) and the merged `f5e00490` — because it refused on quota at 11:41Z, 13:13Z, 14:56Z, 16:42Z, 19:12Z and 21:32Z, and was re-triggered after each push. **Standing review authorization:** head reviewed `f27e1161`/`21640dc`, current head at merge `f5e00490`; each finding fixed in a pushed commit; four of its six inline threads stay open because the CI wrapper exposes no way to list thread ids and the listing is truncated, so they could not be closed mechanically — the operator accepted that gap.
- `sourcery` (optional, quota-limited): triggered 11:41:53Z, refused (review budget, next in 1 day and 11 hours). Does not gate.

## 6. Operator questions

One question, asked at the merge point with the review surfaces read in the same step:

- Text: "PLAN-37 (PR #297) is complete and green on its current head 6a0cdeb, but the required reviewer will not review that head…" — stating coderabbit's reviewed heads, the sixth quota refusal, CI green on the head, `verify` green with 2553 tests, Sonar 0 confirmed, the e2e audit green on all 22 views with the same 7 base failures, all findings fixed and answered, and the four threads it could not close.
- Options: "Keep waiting for CodeRabbit" (wait out the quota, up to four more 90-minute cycles), "Merge now on the standing authorization" (bends one clause — four threads stay open), "Stop and leave it to you".
- Selected: **"Merge now on the standing authorization"**.

The head at enqueue (`f5e00490`) differs from the one the question named (`6a0cdeb`) only by the merge of `origin/main` that the lane requires immediately before enqueue; no review of the current head arrived after the question.

## 7. Rule check

- `executor-skew`: **match** (executor 0.1.2036, lane 0.1.2036), read before `resolve`.
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true` — the main checkout's copy is older; the ledger copy was read and obeyed.
- Guard: `guard: none on this harness (unenforced)`; `.plan/temp/guard.log` was not produced because no guard is installed.
- Shell file operations: none — every read and search went through the file tools. No file outside the repository was read.
- Builds: all through `plan-marshall:build-maven:maven`. The first `verify` was routed and failed at once — the daemon ran `mvn verify` (not `./mvnw`) in the main checkout and the process could not start (`job-logs/c1b4cbd01f5c4abbbe6e7f17a69ef38a.log`, exit -1). Every build after that ran `--execution-mode in_process`, which the port-bound e2e commands require in any case.
- One deliberate reversion: the D2 source was reverted by hand twice to capture the failing-first runs, then restored; no git restore, stash or rebase was used.
- One rule bent, with the operator's explicit selection: merged while four of coderabbit's inline threads stay open and while its review does not cover the final head. Nothing else was skipped: CI, `verify`, the audit and the Sonar read all hold on the merged head.
- Repository files were edited only inside the worktree. `.plan/` holds only `.plan/temp/`, the `ci` body slots and this message.

## 8. Build server

- Setup preflight: `ready`. The first routed build failed as described; the fallback was `in_process`, recorded per build. No job was `killed`.
- `runtime_stamp_reason` was not returned; `plugin_version` is `unknown`.

## 9. Residue

- The seven e2e failures are the base's own and are untouched; they keep D3's first Done-when from being met as written until another plan fixes them.
- The overflow panel's `test-first` evidence cannot be produced: its Escape predates this plan, so D1 stays not fully met as written.
- The axe audit runs the German interface at 1440×900; an English-only gap in a label would not be caught.
- The icon-only control's visible label is covered by `IconButton.test.ts` and the media query only, for the reason given above.
