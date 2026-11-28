envelope_version=1
sender_type=plan
sender_id=rb-135-web-chat-core
epic=ui-improvements
kind=landing
created=2026-11-28T11:10:51Z

```landing-facts
schema=landing-facts/1
plan_id=rb-135-web-chat-core
epic=ui-improvements
pr=#598
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=4
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=70
review_body_findings_handled=70
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

1. **Deliverables**
   - D1 (`The chat client, store and stream`): met; `order`: `test-first`; failing run `task-251` / `job 0f9fa8f207c348829f478b877764ee71` (`store.test.ts`, `runtime.test.ts`, `StartView.test.ts`); passed at commit `71962fa0` (`npm-2026-11-27-114331.log`, 3 files, 171 tests; check `npm-2026-11-27-114120.log`, lint `npm-2026-11-27-114220.log`).
   - D2 (`ChatMarkdown` and `marked`): met; `order`: `test-first` + `mutation-after`; test-first failed at `npm-2026-11-27-114450.log` (`ChatMarkdown.test.ts`), mutation-after failed (`{@html` source scan) at `npm-2026-11-27-114642.log`; passed at commit `187b2ee3` (`npm-2026-11-27-114656.log`, 1 file, 8 tests; check `npm-2026-11-27-114712.log`, lint `npm-2026-11-27-114757.log`).
   - D3 (`ChatTurn`, the working line and the transcript behaviour): met; `order`: `test-first`; failed at `npm-2026-11-27-114923.log` (`ChatTurn.test.ts`); passed at commit `138d3d03` (`npm-2026-11-27-115411.log`, 1 file, 11 tests; check `npm-2026-11-27-115327.log`, lint `npm-2026-11-27-115427.log`).
   - D4 (`Chat`: head, composer, empty chat, New chat): met; `order`: `test-first`; failed at `npm-2026-11-27-115607.log` (`Chat.test.ts`) and `npm-2026-11-27-115624.log` (`Compose.test.ts`); passed at commit `21e38678` (`npm-2026-11-27-115942.log`, 5 files, 65 tests; check `npm-2026-11-27-120001.log`, lint `npm-2026-11-27-120046.log`).
   - D5 (`The chat in the engine's side column, and the plan's and epic's chat acts`): not met (moved to `PLAN-192` per `Split 2026-11-25` & `Re-Grounding 2026-11-26`).
   - Final head `c6861895b99c65fa091ae8c836801623f33e1268` verification: `npm run check` (`npm-2026-11-28-095149.log`), `npm run lint` (`npm-2026-11-28-095211.log`), `npm run test:ci` (`npm-2026-11-28-095222.log`), full Maven `./mvnw verify` (`job 432016739c504c80b58da2abdc2e9610` / `maven-2026-11-28-103656.log`, `exit_code: 0`, `duration_seconds: 1096`, `tests_run: 5741`); merged to `main` via PR `#598` at `bdaf6f263c89346973cb5522efb9b0b8364975ec` (merge_group run `46137467204`).

2. **Personas and skills**
   - D1–D4 executed under `plan-marshall:persona-implementer`; spec-declared skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security`, `pm-dev-frontend:jest-testing` (D1, D2) and `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing` (D3, D4). D5 (`plan-marshall:persona-integration-tester`) was out of scope per spec split. Self-review ran with `Code Reviewer` (`536060ef-024e-4fa8-9a62-e70a72eafbd3`). No extra domain skills were added.

3. **Hypotheses**
   - `refs/pull/598/merge` CI `PARSE_ERROR` after `26dbee82`: hypothesized duplicate `openPlanView,` import against `origin/main` (PR `#594`); confirmed at `pm-web-app/src/lib/stores/runtime.test.ts` lines 26 & 44 and resolved by merging `origin/main` in `fe54d017`.
   - CodeRabbit R3 `inline-18` (`CONNECT_TIMEOUT_MS` on SSE reads): refuted keeping a stream-read timeout because quiet idle SSE streams do not emit 10s heartbeats (`pm-web-app/src/lib/api/streamState.test.ts:131`).
   - CodeRabbit R3 `inline-25` (TS enum codegen): verified no TS codegen pipeline exists; build-time enum parity against `pm-api` Java enums is enforced in `pm-web-app/src/lib/chat/store.test.ts`.

4. **Findings**
   - Self-review: R1 findings fixed in `4d7a4c09`, vocabulary fix in `dd69c16e`; R2 returned `NO_DEFECTS`.
   - Sonar round 1: resolved in `02bad9d1` and `070e4506`.
   - CodeRabbit R1 (`PRR_kwDOVBOtss8AAAABaz3FnA`): 32 findings fixed in `26dbee82`.
   - Sonar round 2 (`S2245`, `S1874`, `S3776`, `S5906`): 6 issues resolved in `19d0e660` (`0` open, `gate_status: OK`, `new_coverage: 90.0%`).
   - CodeRabbit R2 (`PRR_kwDOVBOtss8AAAABa0OTLw`): 23 findings (`inline-10`–`inline-13`, `outside-diff-1`, `minor-24`–`minor-41`) fixed in `1d7db3a7`; all 4 inline threads replied & resolved.
   - Sonar round 3 (`S1128` unused `Tokens` import at `pm-web-app/src/lib/chat/markdown.ts:1`): fixed in `d372d690` (`0` open, `gate_status: OK`, `new_coverage: 91.0%`).
   - CodeRabbit R3 (`PRR_kwDOVBOtss8AAAABa2jSwg`): 15 inline findings (`inline-14`–`inline-28`) — 13 fixed in `c6861895`, 1 declined by design (`inline-18`), 1 deferred (`inline-25`); all 15 threads replied & resolved.
   - Final gate on `c6861895`: `review-gate` reported `unresolved_review_threads=0`, `review_body_findings_total=70`, `review_body_findings_handled=70`, `merge_allowed=true`; GitHub CI passed (`overall_status: success`); Sonar reported `0` open issues, `0` hotspots, `gate_status: OK`, `new_coverage: 91.9%`.

5. **Reviewers**
   - `coderabbit` (required, `quota_limited=true`, `review_complete_after_handling=true`): reviewed R1 (`PRR_kwDOVBOtss8AAAABaz3FnA`), R2 (`PRR_kwDOVBOtss8AAAABa0OTLw`, head `19d0e660`), R3 (`PRR_kwDOVBOtss8AAAABa2jSwg`, head `d372d690`). Standing review authorization: on merge head `c6861895`, all 70 review body findings were handled (`70/70`) and all inline threads resolved (`0` unresolved); check status `NEUTRAL` and `review_complete_after_handling=true`.
   - `sourcery` (optional, `quota_limited=true`, `review_complete_after_handling=false`): silent.

6. **Operator questions**
   - none asked

7. **Rule check**
   - `guard: none on this harness (unenforced)`
   - `spec_source`: `ledger_worktree` (`/Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-135-web-chat-core.md`), no `spec_differ` marker.
   - `executor_in_worktree`: `present` (`executor_version=0.1.2183`).

8. **Build server**
   - Setup preflight `build_server: ready`.
   - Maven `./mvnw verify` runs executed through `plan-marshall:build-maven:maven` (`job 0f9fa8f207c348829f478b877764ee71`, `86273db3ac55402a83a4a03b74573f85` / `maven-2026-11-27-233848.log`, `05feaf5ba811486e863a1ce03e9d6df8` / `maven-2026-11-28-001712.log`, `aae3f09946aa4d17971d25aa5d5a5852` / `maven-2026-11-28-004456.log`, and final head `432016739c504c80b58da2abdc2e9610` / `maven-2026-11-28-103656.log`). `resolve` returned `model=unknown` and `effort=unknown` directly with no `runtime_stamp_reason`.

9. **Residue**
   - D5 was moved to `PLAN-192` per `Split 2026-11-25` & `Re-Grounding 2026-11-26`. Worktree `.plan/local/worktrees/135-web-chat-core` and local/remote branch `feature/135-web-chat-core` were cleanly removed (`cleanup_owed=false`).
