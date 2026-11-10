envelope_version=1
sender_type=plan
sender_id=rb-123-cli-display-layer-residue
epic=ui-improvements
kind=landing
created=2026-11-10T17:14:52Z

```landing-facts
schema=landing-facts/1
plan_id=rb-123-cli-display-layer-residue
epic=ui-improvements
pr=#404
merge_state=open
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:pending,merge:pending
unresolved_review_threads=0
review_body_findings_total=0
review_body_findings_handled=0
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-123 — stopped before the merge

The work is complete and every gate but the required reviewer is green at head `c8be43d1`. The run stops here because CodeRabbit, the only required reviewer, is rate-limited and has posted no review of any head, and because the spec's Standing Constraints reserve the merge to the operator's word.

## 1. Deliverables

- **D1 — the regressions and the unanswered review items: met.**
  - (a) met, `test-first`, `DisplayTest.shortStyleGermanDot`; failing run before `e33e1b7b`, job `ecc9823270a9462d8aab27d794ffc68b` (expected `12. Sep.`, got `12 Sept.`).
  - (b) met, `test-first`, `GermanAgeDisplayTest.agesFollowTheContextClock`, `OverviewCommandTest.runningSinceUsesTheContextClockZone`, `ProjectsFullScreenTest` (fixed-clock age); failing runs job `d3958e9104fe4cec946179777399d696` and job `9cf088e55df344d6bc474b73f9db7064`.
  - (c) met, `test-first`, `ProjectWorkLevelTest.landingWithoutUnitShowsPlaceholder`, `GermanAgeDisplayTest.unitTitleSitesHandleMissingAndUnknownUnit`; failing run job `4e6b1d5b3c344b98a454e78452251c9d`.
  - (d) met, `test-first`, `GermanAgeDisplayTest.missingTimeIsUnknown`, `WebDevicesCommandTest.formatLastUsedNeverUsed`; failing run job `d3958e9104fe4cec946179777399d696`.
  - (e) met, `test-first`, `EpicViewLevelTest.eventsUseTheClockZone`; failing run job `30e7fb1d203a40f5abb9c4bd20349d1c`.
  - (f) met, `none`, `PlanCompletionLevelTest`, `PlanViewLevelTest.germanChangeLine`, `ProcessTreeTest`.
  - #371 replies and threads: met, `none`, read back through `ci pr comments --pr-number 371` (`PRRT_kwDOVBOtss61Blxm` and `PRRT_kwDOVBOtss61Blxh` both resolved, review body `IC_kwDOVBOtss8AAAABkzis7A` answered).
- **D2 — one formatter for durations and times: met.** The amendment's grep over `pm-operator/src/main` returns no line (`none`), and `ProjectWorkLevelTest.twoDaysRendersInBothLanguages`, `PlanViewLevelTest.twoDaysRendersInBothLanguages` and `SystemLevelTest.twoDaysUptimeRendersInBothLanguages` pin `2 d` / `2 T` (`test-first`, failing run job `1d04179409784c46b9989a05219ebc29`).
- **D3 — no raw text reaches the operator: met.** `TuiCapabilityIT.screensAndTranscriptsHaveNoRawWireTokensOrPlaceholders` and `TuiCapabilityIT.transcriptsHaveNoRawResidue` (`mutation-after`, mutated `Display.gate`, failing run job `bdf6c55ccf8d40e985bf3bb245052b5a` — `questions contains ci_check:`), and `git grep -n "Display\.\(word\|title\)(" -- pm-operator/src/main` names `EpicCommand`, `StatusCommand`, `StateDialogue` and `ProjectWorkLevel` (`none`). This run additionally moved the plan view's task/job states (`plan.task.*` / `plan.job.*`), the status tree's alongside landings (`overview.landing.*`, `state.landing.*`) and the web state words onto the catalogue.
- **D4 — the screen evidence PLAN-34 owed: met.** `pm-operator/src/test/resources/screen-captures/PLAN-123.txt` exists and `ScreenCaptureIT` rendered all 28 screens green (job `9628d177661b43ca9f0f11da7b783e01`; screens under `.plan/temp/terminal/PLAN-123/after/`). The open-issue read at the head is a confirmed `0` with `0` hotspots and an `ok` quality gate (83.9 % new-code coverage). The German screens were checked against the catalogue's forms and two residues fixed (raw `done · success`, English alongside literals, a jammed epic column).

## 2. Personas and skills

- D1, D2, D3: `persona-implementer`; skills `pm-dev-java:java-core`, `pm-dev-java:java-null-safety` (D1), `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- D4: `persona-integration-tester`; skills `pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing`.
- Every self-review round: `persona-code-reviewer` (as a standard).
- Skills added for surface drift: none — no module outside the spec's Expected Surface was touched.

## 3. Hypotheses

- `Display.java:247` formatted `d MMM` for every language — **confirmed**; now `d. MMM` for German (`Display.java:336-338`), and `CleanupCommand:367-369` too.
- `StatusCommand:461` `Instant.now()` / `:473` `Clock.systemUTC()` — **confirmed**; all sites take `context.clock()`.
- `ProjectWorkLevel:306-312` renders an empty unit slot — **confirmed**; now `Display.title` with `unit.unknown`.
- `Display.word`/`Display.title` had no caller — **confirmed**; 17 call sites now.
- `QuestionsCommand:125` printed the raw gate — **confirmed**; `Display.gate` renders it.
- The duration/placeholder/raw-id site list — **confirmed** at the current HEAD, lines moved.
- HYPOTHESIS "the real daemon sends ISO `running_since`" — still **unverifiable** from the client; the mock's `fully-onboarded` fixture carries the already-formatted `today 08:14`, so the status captures use `running-work`, whose value is a plain `08:14`.

## 4. Findings

- Self-review round 1 (12 findings): fixed the completion line's duplicate plan id, the status `web` line's English overload, the missing transcript scan, the German marker jam and the missing fixed-clock `projects` assertion in commits `22fbe2d0`, `7e93e9db`, `6ff9a5c9`. F1 (printed `plan report` is hard-coded English) and F4 (`RolesCommand` is an English-only screen) are pre-existing surfaces outside the spec's named sites and stay open as residue; the self-review therefore reports `budget-exhausted` rather than `clear`.
- Self-review round 2: closed the round-1 items and the four new low findings it raised.
- The earlier D1/D3 commits left three transcripts stale and three assertions pinned to the old lone dash; the full `test` run at the head exposed them and they are fixed in `37cb442d`. The pre-commit rewrites D1/D3 never committed land there too.
- Sonar (new code): 9 issues on `f8fdfeb2`, 3 on `0c4bea46`, `0` on `c8be43d1`, all fixed (S9358, S3776, S1192, S1172, S2589, S8786). New-code coverage rose 77.2 % → 83.9 % once `Display.eventTime` and `Display.gate` had tests (`c8be43d1`).
- CodeRabbit: no review. Three refusals, all "Review rate limited" (`IC_kwDOVBOtss8AAAABk5cbwQ` at 15:15, `IC_kwDOVBOtss8AAAABk6pONA` at 16:05, `IC_kwDOVBOtss8AAAABk7ULdw` at 16:35). The lane's quota policy sets the next allowed trigger at **18:05:28**; the escalated trigger (`@coderabbitai full review`) follows the first re-trigger, not this stop.
- Sourcery (optional): refused, "pull request is larger than the review limit of 150,000 diff characters". Not gating.

## 5. Reviewers

| Reviewer | Required | Result | Head | History |
|---|---|---|---|---|
| coderabbit | yes | **refused (quota) — no review of any head** | none | triggered 15:15, 16:05, 16:35; three rate-limit refusals; next allowed 18:05:28 |
| sourcery | no | refused (diff too large) | none | triggered 15:15 |

No qualifying review exists, so the standing review authorization does not apply. The merge gate cannot pass without a CodeRabbit review of the head.

## 6. Operator questions

none asked. Free-text operator messages: none.

## 7. Rule check

- `guard: none on this harness (unenforced)` — no tool-call guard is installed for OpenCode here, so no guard log exists for this run; nothing is claimed as kept.
- `executor-skew`: **match** (executor `0.1.2090`, lane `0.1.2090`), recorded.
- `spec_source`: **ledger_worktree**; `spec_differ: true` (the main checkout's copy differs from the ledger copy that was read).
- No shell file operation was used to read or list files; every read went through the file tools.
- Every build ran as the resolve result's Maven canonical, from the main checkout against the worktree, routed to the build daemon (`mechanism=daemon_longpoll`); one job was queued behind other plans (max `queued_seconds: 649`), none fell back.
- One notable break to report: the first capture attempts used `plans -> E -> Enter` for the completion level, which landed on the `needs:0` pseudo-project's ended-work view; the captures were re-taken with `plan report fix-relay` (the design's documented scrollback form) after the mock was found to serve the completion endpoint only in `reports-and-system`.

## 8. Build server

- Setup preflight: `ready`. Every build was submitted and ran in the daemon (`mechanism: daemon_longpoll`, `in_process_fallback`: 0). Queued behind other plans on three runs (649 s, 331 s, 94 s) — the mechanism serialises per worktree, nothing was retried for it.
- Jobs: `292023594a58` (wrong test selector, no green claimed), `d12f224f` (7 stale tests), `b0d3652e`/`6b555edd` (my test bugs), `fde1dfdf`, `42ea34b4`, `867b244a`, `5ea0ce21`, `6eb53ec9`, plus the capture runs `29b94724`, `f0512db7`, `65b84a7a`, `9628d177`. No `killed` job.
- No `runtime_stamp_reason` was returned.

## 9. Residue

- **The printed `plan report` is hard-coded English** and bypasses the completion catalogue; the German capture of it is therefore English-only. The full-screen completion level is localized. Fixing it needs new lowercase catalogue keys and a separate change.
- **`RolesCommand` is an English-only screen wholesale**; this plan changed only its placeholders.
- **The German completion report's deliverable states (`done`)** come from that same English printed surface.
- **Web app**: not run here; the full-reactor `verify -Ppre-commit` at `f8fdfeb2` ran the web module's own checks and was green.
- **The mailbox could not be read**: `orchestrator inbox read` answers `invalid_target_plan` because the plan id `123-cli-display-layer-residue` fails the required `^[a-z][a-z0-9-]*$`. This is stated as "could not look", not as no mail; nothing was delivered to this plan in this session.
- **The merge is owed** once CodeRabbit reviews the head (next allowed trigger 18:05:28, then the escalated trigger if it refuses again) and the operator gives the word.
