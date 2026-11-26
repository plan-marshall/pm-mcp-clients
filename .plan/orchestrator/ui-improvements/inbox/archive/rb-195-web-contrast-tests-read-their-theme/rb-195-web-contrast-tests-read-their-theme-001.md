envelope_version=1
sender_type=plan
sender_id=rb-195-web-contrast-tests-read-their-theme
epic=ui-improvements
kind=landing
created=2026-11-26T01:59:33Z

```landing-facts
schema=landing-facts/1
plan_id=rb-195-web-contrast-tests-read-their-theme
epic=ui-improvements
pr=#588
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=2
review_body_findings_handled=2
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

`guard: none on this harness (unenforced)`

# PLAN-195 — the contrast tests read the theme they name

PR #588 merged as `51e3bcc9`. Worktree, local branch and remote branch removed; `main` pulled to the
merge commit.

## Deliverables

1. **The e2e contrast matrix audits the requested theme** — met.
   - Done-when (order `mutation-after`): met. Test: `assertTheme` in `auditViewContrast`
     (the contrast matrix). Failing run: commit `e16cfe22` mutated, the storage write removed —
     `.plan/local/plans/NO_PLAN/build-results/pm-web-app,pm-web-e2e/maven-2026-11-25-181544.log`,
     `Error: the dark audit would measure the document's light theme` at `contrast.js:23`
     (52 occurrences; 25 of that run's 78 failures are the app server's `ERR_CONNECTION_REFUSED`
     cascade, not theme verdicts). Passing run: `maven-2026-11-25-195203.log`, 471 passed,
     1 failed (the named pre-existing `designer.spec.js:272`), 75 skipped. Commit `e16cfe22`.
2. **The unit tests read the real blocks of both themes** — met.
   - Done-when 1 (order `test-first`): met. Test: `tokens.test.ts` › `keeps the dark palette
     distinct from the light one for every theme-varying colour`. Failing run: dark block set equal
     to the light one — `maven-2026-11-25-175028.log`, `a dark colour token repeats its light value:
     expected [ '--color-bg-app', …(28) ] to deeply equal []`. Passes at `c21872f6` (3480 tests).
   - Done-when 2 (order `mutation-after`): met. Tests: `RowMenu.test.ts` › D2a focus cue,
     `Button.test.ts` › variant contrast. Failing run: commit `269f8074` with dark
     `--color-bg-subtle` `#1e2433 → #8b7ef9` — `maven-2026-11-25-193709.log`,
     `focus cue contrast in the dark palette: expected 1 to be greater than or equal to 3` and
     `disabled contrast in the dark theme: expected 1.0782521728300856 to be greater than or equal
     to 4.5`; restored and green. Commit `c21872f6`, tightened in `269f8074`.
   - Done-when 3 (order `none`): met vacuously — no real-theme pair fails and no token was retuned;
     `tokens.css` is absent from the final diff.
   - Per-head test runs: `verify` green at `e16cfe22` (799 s), `269f8074` (619 s), `9768e391`
     (768 s), `1aec310c` (745 s), `fc84c95e` (535 s); the e2e suite at `269f8074` (471/1/75).
3. **Operator decision + review — the decision-opened line names its blocker** — met.
   - Operator-chosen unblock: `pm-api` catalogues aligned to the design table (`8a69f3ec`); after
     merging `main` (whose `458f9e3c` fixed the producer's argument) the branch keeps main's
     `blocksOf` and adds the rendered-label test (`1aec310c`). Verified by
     `ChatTextsCatalogueTest` and `ChatContextNarrativeTest`; `verify` green at `fc84c95e`.

## Personas and skills

- D1 and D2: persona `plan-marshall:persona-module-tester`; domain skills
  `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing` (declared by the spec).
- Review fixes and the operator-chosen catalogue fix touched `pm-api` and `pm-mock-runtime`. No Java
  domain skill was loaded; reported under Rule check as a surface-drift deviation.

## Hypotheses

- D1: `setTheme` set only the attribute (`contrast.js:14-19`), called before `page.goto`
  (`theme-readability.spec.js:664` before `:667`), no `dataset.theme` assertion — **confirmed**.
- D2: `blockOf` at `Button.test.ts:291`, `tokens.test.ts:321`, `lib/test/stylesheet.ts:23`;
  `parseThemes` at `tokens.test.ts:354-368`; no palettes-differ guard — **confirmed**, and the
  count is **refuted in part**: five copies exist (`WaitingMark.test.ts:38`,
  `BrandMark.test.ts:33` read component `<style>` blocks, not theme blocks; left in place).

## Findings

- Self-review round 1 (persona-code-reviewer): 4 defects fixed — top-level `:root`, `blockOf` doc,
  named `tokenOf`, module-private page audit; 2 accepted as not-defects. Round 2: all confirmed,
  no new defect. Open: 0.
- CodeRabbit `9768e391`: `inline-1` (the producer sent `yes`/`no`, so the label was empty) —
  fixed `1aec310c`, thread replied and resolved; `inline-2` (the media-query exclusion list) —
  fixed by depth-aware `topLevelBlockOf`, thread replied and resolved. Dispositions:
  `coderabbit inline inline-1 — 1aec310c`, `coderabbit inline inline-2 — 1aec310c`.
- Sourcery: no findings; approved at `fc84c95e`.
- SonarCloud: 0 new issues, quality gate passed at `fc84c95e`.

## Reviewers

| Reviewer | Required | Result | Head | History |
|---|---|---|---|---|
| coderabbit | yes | reviewed (COMMENTED, 2 findings) | `9768e391` | triggered 20:51, refused quota; re-triggered 22:36, refused; escalated `@coderabbitai full review` at 00:08 after the 90-minute window, reviewed 00:12:58. Findings handled; `review_complete_after_handling` so no re-review was required for the later heads. |
| sourcery | no | approved | `fc84c95e` | refused on its own 250k-character budget at 20:51 (optional, non-gating); approved on the final head. |

## Operator questions

1. Asked: "PR #588 is complete and locally green, but the merge gate is blocked on two facts: CI is
   red in `pm-api`'s `ChatTextsCatalogueTest` because `origin/main`'s design table and catalogue
   disagree (main's `12281d41`); CodeRabbit refused on quota with no review. What should the run
   do?" Options: stop and report (Recommended); fix the catalogue in this PR; revert the design doc;
   wait out CodeRabbit. Selected: **fix the catalogue in this PR**.

Free-text operator messages: none.

## Rule check

- `guard: none on this harness (unenforced)` — nothing was enforced by a tool-call guard.
- Own deviations, not from a guard log:
  - two shell file operations that the file tools should have served: one `ls` of the epic directory
    and two `wc -l` calls on build logs;
  - the Java modules were edited without loading `pm-dev-java` domain skills (surface drift from the
    operator-chosen and review-driven changes);
  - the `verify -Ppre-commit` profile rewrote two unrelated `pm-mock-runtime` files on every run;
    they were returned to their tracked content each time and never committed (this plan is
    test-only).
- Executor-skew verdict: `match` (executor 0.1.2183, lane 0.1.2183). Spec read: `ledger_worktree`,
  no `spec_differ`.
- Build logs outside the repository were read only as the build result's own `log_file` (§4).
- No session restart, no killed sub-agent, no re-dispatch.

## Build server

- Setup preflight: `ready`.
- Mechanism: every build ran `daemon_longpoll`; `in_process_fallback` 0; no degraded result, no
  launch-fault retry. Several jobs waited in the daemon queue (`queued_seconds`), which is
  admission, not a fallback.
- Two failures were not build verdicts: the first e2e run failed at setup (`vite` absent because
  `pm-web-app` was not in the reactor), and the `8a69f3ec` verify failed on the catalogue mismatch
  before the base merge.
- `runtime_stamp_reason`: none reported; `plugin_version` is `unknown` as the resolve states.

## Residue

- A same-head D1 mutation re-run at `269f8074` collapsed on the app server's
  `ERR_CONNECTION_REFUSED` before the dark block
  (`.plan/local/plans/NO_PLAN/build-results/pm-web-app,pm-web-e2e/maven-2026-11-25-200557.log`,
  43 passed / 429 failed); the cited D1 artifact is the earlier run on byte-identical
  `setTheme`/`assertTheme`.
- The baseline e2e run at HEAD ended in the same app-server collapse after the dark/desktop block
  (`maven-2026-11-25-170535.log`, 353 passed / 119 failed); its only non-infrastructure failure was
  `designer.spec.js:272`, matching the spec's named red.
- `main`'s design table and catalogue disagreed for part of this run (`12281d41` … `458f9e3c`);
  the branch carries the catalogue alignment and main carries the producer fix.
