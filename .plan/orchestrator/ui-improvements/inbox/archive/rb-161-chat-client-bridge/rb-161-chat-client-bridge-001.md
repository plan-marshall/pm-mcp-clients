envelope_version=1
sender_type=plan
sender_id=rb-161-chat-client-bridge
epic=ui-improvements
kind=landing
created=2026-11-14T01:15:59Z
revision=2
amended=2026-11-14T01:16:49Z

```landing-facts
schema=landing-facts/1
plan_id=rb-161-chat-client-bridge
epic=ui-improvements
pr=#429
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=0.1.2118
executor_version=0.1.2118
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=9
review_body_findings_handled=9
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-161 landed — The Web and Terminal Chats Talk to the Chat Contract Again

Merged as #429, squash `dc811fb3`. Branch `fix/161-chat-client-bridge` deleted (local and remote), worktree removed, `main` pulled to the merge commit.

## 1. Deliverables

**D1 — The web client calls speak B1. met.**
`client.ts` `normaliseChat` maps `ChatResource` → `ChatReadBack` (the one wire reader for plan, epic, entry and ended work); `normaliseFollowUp` reuses it. Turns post `ChatTurnRequest` with a per-send `client_turn_id` (reused on a retry), answers post `ProposalAnswerRequest` (`turn_id`), ends use the B1 end act, and the reply is followed by turn id and status. Only an **open** proposal is carried.
Test `client.test.ts` "ApiClient chat on the B1 contract" (12 cases) + `client.normalise.test.ts` (4 cases); order **test-first** — failing run `2728abe8…`, `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-13-165341.log` (10 failed / 1796 passed). Commit `9ae531c3`, extended in `3801b716`.

**D2 — The web views open and run their chats against the mock. met.**
`StartView` opens the entry chat on `ChatRoutes.entry(…).chat()`, re-reads it on reopen (`getEntryChat`) and on `chat.turn.started` / `chat.turn.completed` / `chat.proposal`, orders its reads against its own sends/ends/answers, and clears the cached chat when another entry is selected. `WorkControlPanel` sends/answers by turn and reports an accepted follow-up as a status notice. `EpicView` answers the review chat by turn.
Unit tests: five StartView/WorkControlPanel cases plus EpicView and DecisionView; order **mutation-after** (the spec asked test-first; the views' call sites changed with D1's client, so fail-first was not observable) — failing run `b6401851…`, `…/pm-web-app/maven-2026-11-13-174330.log` (the four view cases). Commits `b1671888`, `3801b716`, `1083b953`, `31ee3b23`, `c2e316a9`, `d50c6a2c`.
`showroom.spec.js` "opens the start entry's refinement chat" — order **none**; the PLAN-148 `test.fixme` removed, the suite green at the merged head (`…/pm-web-e2e/maven-2026-11-14-000947.log`: 395 passed, 75 skipped, ports 21610/21611).
Browser check — order **none**: 18 screenshots in `.plan/temp/playwright-mcp/plan161-*.png` (entry chat, plan chat with the streamed reply, epic chat, ended-work follow-up before/after accept, light and dark at 1280×800 and 390×844), and the console read in `plan161-console-errors.txt` / `plan161-console-warnings.txt` (0 errors, 0 warnings on a paired load; the two 401s belong to the pair page before pairing).

**D3 — The terminal's plan, epic and ended-work chat speak B1. met.**
`WorkActionsClient` uses `ChatRoutes`, sends `client_turn_id`, follows the reply by turn id and status (20 s bound, interrupt-aware) and answers by `turn_id`; `WorkChatDialogue` renders `author`/`text`/`untrusted`, the applies point (two new operator texts), asks only while `hasProposal()`, follows the paged transcript by the last **settled** turn and skips the proposal turn; the ended-work chat accepts the follow-up on the runtime and says the start is being prepared. `ChatReadBackResource` deleted.
`ControlDialoguesTest` (49 tests) and `PlanChatIT` — order **mutation-after**: old-shape body `41977b52…`, `…/pm-api,pm-operator/maven-2026-11-13-183545.log` (`invalid_arguments: Unknown field in ChatTurnRequest: message`); reply follow reverted `3733a612…`, `…-183701.log` (`ControlDialoguesTest` 2 errors). Commit `cd083494`, extended through `31ee3b23`.
Terminal capture — order **none**: `pm-operator/target/screens/PLAN-161/plan-chat-running-work-120x30-{en,de}.txt` from `pm-operator/src/test/resources/screen-captures/PLAN-161.txt` (scenario `running-work`, applies line in both locales).

**D4 — Consumers listed, nothing left behind. met.**
The PR body carries the consumer table. Content search over `pm-web-app/src`, `pm-web-e2e/tests`, `pm-operator/src`, `pm-relay/src` at the head: no old-shape call outside the decision-chat row, which the operator left unaddressed and PLAN-137 owns. `pm-relay` had none. Order **none**.

## 2. Personas and skills

- Production code: `persona-implementer` (the spec's own `Persona:` line). Unit/module tests: `persona-module-tester`; the terminal IT: `persona-integration-tester`; every self-review round and every PR comment triage: `persona-code-reviewer`.
- D1 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`; D2 the same plus `pm-dev-frontend:css`; D3 `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`; D4 `pm-dev-frontend:javascript`, `pm-dev-java:java-core`. `skills added: none` — no deliverable left its expected surface.

## 3. Hypotheses

- **The decision resource names its scope — confirmed in part**: `DecisionEnvelope` carries `DecisionScope` (plan, epic, setup, machine) and a `plan`/`project` member, but not the subject id for every scope; for epic, entry and machine decisions the only trace is free text in `subject`. Operator decision: keep the "no chat" notice; PLAN-137 owns the address.
- **The answer to an accepted follow-up, or an event, names the new start entry — refuted**: the accept answers a `ChatResource` with no entry id or link, `StartRegistry.openFollowUp` broadcasts no `start.*` event, the ended plan/epic resource does not carry it, and no project-entries listing exists. Operator decision: the web shows `control.chat.follow_up.opened`, the terminal says the start is being prepared; neither invents a route.

## 4. Findings

- Self-review (3 rounds, `persona-code-reviewer`), then review of the PR. Self-review round 1 fixed an answered proposal that stayed actionable in both clients (the runtime keeps it in the resource) and the terminal's paged-transcript cursor; round 2 the notice tone and four unpinned fixes; round 3 a layout line and two nits, fixed at the round limit and recorded as `budget-exhausted` in the facts above (the operator had chosen to proceed with the fixes as they stand).
- CodeRabbit findings (9 in the review bodies, all handled): ended-work accept on the runtime (`31ee3b23`), streaming-transcript cursor (`31ee3b23`), remaining reply budget and last snapshot (`31ee3b23`), `chat.turn` event name (`31ee3b23`), cached chat across entries (`31ee3b23`), superseded refresh (`c2e316a9`), read ordering among refreshes (`d50c6a2c`), ended chat on reopen (`d50c6a2c`), and the decision-chat gating **declined** with the operator's reason. Each thread has a reply naming a commit (or the reason) and is resolved.
- Sonar: 2 new-code findings on the first head (`java:S1192`, `typescript:S9382`) cleared in `c3d6e308`; a later unused import cleared in `211ffb69`; then 12 unused imports and one unused parameter from the follow-up fix in `04e7e2df`, cleared. The new-code coverage gate (73% → 86.7%) was raised with four tests in `4330ff13`.
- Sourcery: refused on its own quota (recorded); it is optional.

## 5. Reviewers

| Reviewer | Kind | Result | Head reviewed | History |
|---|---|---|---|---|
| coderabbit | required | reviewed | `610d642c` (marker `final_review_risk_coverage` = reviewed at that head) | triggered once at `c1f71754`; incremental reviews at `04e7e2df` and `610d642c`; its per-hour limit hit once (`next included review available in 40 minutes`) and the automatic incremental review covered the later heads; a manual re-trigger was refused as rate limited and the run did not wait for a further one |
| sourcery | optional | refused (own quota: 250,000 diff characters per 7 days) | — | triggered at `c1f71754`; refused; optional, so no gate |

The required reviewer's review is the qualifying one, every finding is handled and every thread is resolved, and `review-gate` reports `merge_allowed: true` with `unresolved_review_threads: 0`, 9/9 body findings handled.

## 6. Operator questions

1. "The lane text is version 0.1.2118 but `.plan/execute-script.py` is version 0.1.2106 … How should I proceed?" — options {Regenerate the executor, then run the lane (Recommended); Stop — report blocked} — selected **Regenerate the executor**, done (`match` afterwards).
2. "D2 ended-work follow-up: no contract surface names the new start entry … How should I proceed?" — options {Keep the notice, no navigation (Recommended); Change the mock so the answer names the entry; Stop the run as blocked} — selected **Keep the notice, no navigation**.
3. "D1 decision chat: the decision resource carries scope but not the subject id … How should I proceed?" — options {Keep the 'no chat' notice, state the gap in the PR (Recommended); Address machine-scope decisions only; Parse the id from the decision's subject text} — selected **Keep the 'no chat' notice**.
4. "The self-review hit its three-round limit … How should I proceed?" — options {Proceed with the fixes as they stand (Recommended); Run a fourth round to confirm} — selected **Proceed with the fixes as they stand**.

No free-text messages were used to bend a rule.

## 7. Rule check

- **Files with shell commands**: the run used the shell for `git` (status/add/commit/push/fetch/merge/worktree), `cp` into the ci body slots, `grep` over build logs and graded screens, `ls`/`wc` over the screenshot directory, `tail`, and one `find` for jacoco reports. The file tools were used for source reads and searches; the shell file operations above are reported here as the rule counts them. The self-review sub-agent reported one `ls` of its own.
- **Builds**: every build went through `plan-marshall:build-maven:maven` with the resolved `build` commands and the default routing; every result was `daemon_longpoll` with no fallback. No CI CLI and no test binary was run directly.
- **`.plan/` writes**: `.plan/temp/**` (progress, PR body draft, landing, screenshots, console logs), the four `ci` body slots the `prepare-*` verbs returned under `.plan/local/plans/NO_PLAN/work/ci-bodies/`, and this landing message. Nothing else.
- **Executor skew**: `skew` at 0.1.2106 → 0.1.2118, regenerated with the operator's word, then `match` (recorded again at landing: 0.1.2118).
- **Spec read**: `spec_source: ledger_worktree`, no `spec_differ` marker.
- **Guard**: `guard: none` on this harness — nothing was enforced, so this report claims no "all kept" and no guard-log line (`guard: none on this harness (unenforced)`).
- **Sub-agent**: one general sub-agent used for the three self-review rounds, all in the same session; no restart, no killed sub-agent, no re-dispatch other than the rounds.

## 8. Build server

- Setup preflight: **ready**. Every build resolved `daemon_longpoll` (no `in_process_fallback`, no launch-fault retry, no `killed` job). 12 routed builds, including the e2e suite and the two `-Pcoverage` and `-Pintegration-tests` runs.
- No `runtime_stamp_reason` was reported; `plugin_version` is `unknown` (the runtime-info seam reports none).

## 9. Residue

- The decision chat stays unaddressed (operator decision; PLAN-137 owns it).
- The ended-work follow-up shows a notice rather than opening the start (operator decision; no B1 surface names the entry).
- `doc/design/web-app/chat.adoc` and `doc/design/shared/work-actions.adoc` still describe the way to the start: that is the target PLAN-137 delivers; the PR body records the divergence.
- `ChatScope`, `ChatState` and `ChatRole` remain because `StartChatResource` and `FollowUpProposalResource` read them; deleting them is not this plan's surface.
- The browser check's ended-work evidence uses `docs-typo` as the ended plan (the seeded ended list does not carry it): the ended view was opened through the app's own `openEndedPlanView`, the same call the Ended Work row makes.
- `inbox landing-check` reports `total_tokens` missing: no token total was measured for this run, and the lane's value for an unread figure is `unknown`, which the check does not accept as a number. `plugin_version` was read as the installed bundle's `0.1.2118` (the executor's own version and the installed skill cache path), because the runtime-info seam `resolve` reads reports none.
