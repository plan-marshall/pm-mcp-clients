envelope_version=1
sender_type=plan
sender_id=rb-123-cli-display-layer-residue
epic=ui-improvements
kind=landing
created=2026-11-11T10:13:34Z

```landing-facts
schema=landing-facts/1
plan_id=rb-123-cli-display-layer-residue
epic=ui-improvements
pr=#404
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=5
review_body_findings_handled=5
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-123 — merged

PR #404 landed through the merge queue: merge commit `1bdfede6791f50a1ea33c9d572039095220ad605`, head `595e1ef3`. This landing supersedes the `merge_state=open` message filed before CodeRabbit's review landed.

## 1. Deliverables

- **D1 — the regressions and the unanswered review items: met.**
  - (a) met, `test-first`, `DisplayTest.shortStyleGermanDot`; failing run before `e33e1b7b`, job `ecc9823270a9462d8aab27d794ffc68b` (expected `12. Sep.`, got `12 Sept.`).
  - (b) met, `test-first`, `GermanAgeDisplayTest.agesFollowTheContextClock`, `OverviewCommandTest.runningSinceUsesTheContextClockZone`, `ProjectsFullScreenTest` (age at the fixed clock); failing runs job `d3958e9104fe4cec946179777399d696`, job `9cf088e55df344d6bc474b73f9db7064`.
  - (c) met, `test-first`, `ProjectWorkLevelTest.landingWithoutUnitShowsPlaceholder`, `GermanAgeDisplayTest.unitTitleSitesHandleMissingAndUnknownUnit`; failing run job `4e6b1d5b3c344b98a454e78452251c9d`.
  - (d) met, `test-first`, `GermanAgeDisplayTest.missingTimeIsUnknown`, `WebDevicesCommandTest.formatLastUsedNeverUsed`; failing run job `d3958e9104fe4cec946179777399d696`.
  - (e) met, `test-first`, `EpicViewLevelTest.eventsUseTheClockZone`; failing run job `30e7fb1d203a40f5abb9c4bd20349d1c`.
  - (f) met, `none`, `PlanCompletionLevelTest`, `PlanViewLevelTest.germanChangeLine`, `ProcessTreeTest`.
  - #371: met, `none`, the two threads (`PRRT_kwDOVBOtss61Blxm`, `PRRT_kwDOVBOtss61Blxh`) replied and resolved, the second review body (`IC_kwDOVBOtss8AAAABkzis7A`) answered.
- **D2 — one formatter for durations and times: met.** The amendment's grep over `pm-operator/src/main` returns no line (`none`); `ProjectWorkLevelTest.twoDaysRendersInBothLanguages`, `PlanViewLevelTest.twoDaysRendersInBothLanguages` and `SystemLevelTest.twoDaysUptimeRendersInBothLanguages` pin `2 d` / `2 T` (`test-first`, failing run job `1d04179409784c46b9989a05219ebc29`).
- **D3 — no raw text reaches the operator: met.** `TuiCapabilityIT.screensAndTranscriptsHaveNoRawWireTokensOrPlaceholders` and `TuiCapabilityIT.transcriptsHaveNoRawResidue` (`mutation-after`, mutated `Display.gate`, failing run job `bdf6c55ccf8d40e985bf3bb245052b5a`), plus `git grep -n "Display\.\(word\|title\)(" -- pm-operator/src/main` naming `EpicCommand`, `StatusCommand`, `StateDialogue`, `ProjectWorkLevel` (`none`).
- **D4 — the screen evidence PLAN-34 owed: met.** `pm-operator/src/test/resources/screen-captures/PLAN-123.txt` exists and `ScreenCaptureIT` rendered all 28 screens (`none`, job `9628d177661b43ca9f0f11da7b783e01`, screens under `.plan/temp/terminal/PLAN-123/after/`); the head's open-issue read is a confirmed `0` with 0 hotspots and an `ok` quality gate (83.9 % new-code coverage), recorded on the PR.

Per-head test runs: `module-tests` green at `e33e1b7b`, `0c909c6b`, `bb9ce356`, `f8fdfeb2`, `0c4bea46`, `101b705b`, `ac323f2f`, `595e1ef3`; `verify -Ppre-commit` (full reactor) green at `f8fdfeb2`, `0c4bea46`, `595e1ef3`; `verify -Pintegration-tests` green at `f8fdfeb2`, `101b705b`, `595e1ef3`.

## 2. Personas and skills

- D1, D2, D3: `persona-implementer`; `pm-dev-java:java-core`, `pm-dev-java:java-null-safety` (D1), `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- D4: `persona-integration-tester`; `pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing`.
- Each self-review round: `persona-code-reviewer` as a standard.
- Skills added for surface drift: none.

## 3. Hypotheses

- The `d MMM` German date — **confirmed** (`Display.java:336-338`); `CleanupCommand` follows.
- `Instant.now()` / `Clock.systemUTC()` in `StatusCommand` — **confirmed**; all sites take `context.clock()`.
- The empty unit slot in `ProjectWorkLevel` — **confirmed**; `Display.title` with `unit.unknown`.
- `Display.word/title` without a caller — **confirmed**; 17 call sites.
- The raw waiver gate — **confirmed**; `Display.gate`.
- The duration/placeholder/raw-id site list — **confirmed** at HEAD.
- HYPOTHESIS "the daemon sends ISO `running_since`" — still **unverifiable**; the mock's fixture carries a formatted value, so the status captures use `running-work`'s plain `08:14`.

## 4. Findings

- **Self-review round 1 (12 findings).** Fixed in `22fbe2d0`, `7e93e9db`, `6ff9a5c9`: duplicate plan id in the completion line, the English `describe` in the status tree, the missing transcript scan, the German marker jam, the missing fixed-clock `projects` assertion. Two pre-existing surfaces were declined with reasons and carried as residue: the printed `plan report` is English-only, and `RolesCommand` is an English-only screen wholesale.
- **Self-review round 2.** Closed the round-1 items and the four low findings it raised (half-localized web line, no regression test for the column and duplicate id, an emptied fixture, `recently` for a null paired date).
- **D1/D3 left stale fixtures.** The full `test` run at the head found three transcripts and three assertions pinned to the old lone dash; fixed in `37cb442d`, together with the pre-commit rewrites the earlier commits never landed.
- **Sonar.** 9 new-code issues on `f8fdfeb2`, 3 on `0c4bea46`, `0` from `c8be43d1`; coverage on new code rose 77.2 % → 83.9 % once `Display.eventTime` and `Display.gate` had tests.
- **CodeRabbit's review of `c8be43d1` (5 inline threads + 2 body findings).** `finding: coderabbit inline inline-1 — declined: phase names are identical in both languages by design (texts-and-languages.adoc, phase.name.*)` (withdrawn by the reviewer); `inline-2 — declined: the plan forbids raw plan ids in the printed epic` (withdrawn); `inline-3 — ac323f2f` (plan states); `inline-4 — declined: RolesCommand is an English-only screen wholesale` (residue); `inline-5 — ac323f2f` (blank `running_since`); `outside-diff-1 — ac323f2f` (web `put` locale); `nitpick-1 — ac323f2f` (kind-only waiver heading).
- **CodeRabbit re-review of the merge head `595e1ef3`.** No new findings; no unresolved thread.
- **Sourcery** (optional): refused, diff over 150 k characters.

## 5. Reviewers

| Reviewer | Required | Result | Head | History |
|---|---|---|---|---|
| coderabbit | yes | **reviewed** | `c8be43d1`, then `595e1ef3` | 3 quota refusals (15:15, 16:05, 16:35) and 2 more after pushes (09:21, then the merge-head trigger accepted); reviews at 08:49:48 and 09:48:20; all findings handled; check SUCCESS on `595e1ef3` |
| sourcery | no | refused (diff too large) | none | triggered 15:15; not gating |

Standing review authorization was available (qualifying review on a head, every finding handled, re-trigger refused on quota), and the reviewer then reviewed the current head itself; the operator's word was taken before the enqueue.

## 6. Operator questions

1. Asked at the merge gate: "Merge PR #404 now? Head 595e1ef3 …". Options: **Merge now (Recommended)** / Wait, keep it open / Stop the run. Selected: **Merge now (Recommended)**.

Free-text operator messages: "review now" (2026-11-11), which resumed the run after the quota window.

## 7. Rule check

- `guard: none on this harness (unenforced)` — no guard log for this run; nothing claimed as kept.
- `executor-skew`: **match** (`0.1.2090` / `0.1.2090`), re-checked at the resume.
- `spec_source`: **ledger_worktree**; `spec_differ: true` (the main checkout's copy was the older, pre-amendment one until the `origin/main` merge aligned it).
- File reads used the file tools throughout; shell use was one command per call, no pipes in the run's own operations (the only compound call was the `fetch` + `rev-list` pair, which the lane does not forbid and which needs the fetch first).
- Every build ran as the resolve result's Maven canonical from the main checkout against the worktree, routed to the daemon (`mechanism: daemon_longpoll`). No build fell back or was retried as a launch fault. No `killed` job.
- One break to report: the first capture attempts used `plans -> E -> Enter` and landed on the `needs:0` pseudo-project's ended-work view; the completion captures were re-taken with `plan report fix-relay` after finding that only `reports-and-system` serves the completion endpoint.

## 8. Build server

- Setup preflight `ready`; every build submitted (`daemon_longpoll`, `in_process_fallback`: 0). Queued behind other plans on several runs (up to 649 s), which the mechanism serialises per worktree.
- No `runtime_stamp_reason` was returned.

## 9. Residue

- **The printed `plan report` is hard-coded English** and bypasses the completion catalogue; the German capture of it is English-only. Answering it needs new lowercase keys and a separate change.
- **`RolesCommand` is an English-only screen wholesale**; this plan changed only its placeholders. CodeRabbit asked for the full localization and the request was declined with that reason.
- **The German completion report's deliverable states (`done`)** come from the same printed surface.
- **The mailbox could not be read** on any of the three reads: `orchestrator inbox read` answers `invalid_target_plan` because the plan id `123-cli-display-layer-residue` fails the required `^[a-z][a-z0-9-]*$`. Stated as "could not look", not as no mail.
