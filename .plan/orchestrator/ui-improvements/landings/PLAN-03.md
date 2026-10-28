# Landing Analysis: PLAN-03 — Mock scenario states

epic: ui-improvements
workstream: WS-01
pr: #253 (merge commit `4efa0314d6d07c7bdca99d3e79a400d88cdf7ddf`; PR #250 closed unmerged, same commits)

> Landing record for one shipped plan. Source: inbox message `rb-03-mock-scenario-states-001.md`
> (runbook lane, harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2021`), analysed
> 2026-10-28 against the merged PR, the CI checks, the merge diff, the lane's progress file and the
> browser text dumps.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 State facts applied by goto | shipped-as-specified, one exception recorded by the plan | The merge diff changes `MockRuntimeEngine`, `StartRegistry`, `running-work.json`, `reports-and-system.json` and `onboarding.json`; `ScenarioStateContractTest` exists (one test method that walks the states). The 48 before/after text dumps exist under `.plan/temp/playwright-mcp/rb-texts/` (3 scenarios × 8 combinations × before/after); the progress file gives the counts the message states (`projects-and-start` 0–2 → 19 of 26, `reports-and-system` 14 of 18). The counts were read from the progress file, not re-run. Exception: `system.running`, `system.stopped`, `system.restarting` and `system.force-stop` change the API resource but no rendered view, because `pm-web-app` shows the lifecycle only as the draining banner. |
| D2 Fixture consistency | shipped-modified | `FixtureConsistencyTest` has 12 test methods at the merge commit (the message says 12). It added `CleanupFixtures`, edited four `pm-web-e2e` specs and the `pm-operator` transcript fixture, none of which the spec declared (see the surface delta below). The plan refuted its own hypothesis that the e2e specs carry their own fixtures, which is why those edits were needed. The plans-rows pair of CC-F-09 ("12 plans vs 3 rows") was left as the specified summary, with a reason recorded. |
| D3 README and scenario descriptions | shipped-as-specified | `pm-mock-runtime/README.adoc` is in the merge diff. The use-case id check against `plan-marshall-documentation/doc/use-cases/` was not re-run. |

**Surface delta** (`inbox landing-check`, base `origin/main` at `4efa0314`, not stale): declared 9, realized 18,
state `expansion_detected`.

- Added, realized but never declared (11): `CleanupFixtures.java`, `scenarios/onboarding.json`,
  `EpicViewScenarioTest`, `RunningWorkScenarioTest`, `SystemScenarioTest`, `StartRegistryTest`,
  `pm-operator/src/test/resources/transcript/control-epic.transcript`, and the four `pm-web-e2e` specs
  `control`, `epic-view`, `overview` and `plan-view`.
- Missing, declared but untouched (2): `scenarios/projects-and-start.json` and `scenarios/fully-onboarded.json`.
  The facts that make `projects-and-start` states change the view came from the engine, `StartRegistry`
  and `onboarding.json`.
- The landing says `skills added: none — no deliverable touched a module outside the Expected Surface`. The diff
  touches `pm-operator` and `pm-web-e2e`, which the spec did not declare; the statement holds only for skill
  resolution, not for the surface.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero).
- Duration: two days, from the lane session start `2026-10-27T07:14Z` to the landing message `2026-10-28T13:45Z`,
  over at least three sessions.
- Anomalies:
  - PR #250 was closed unmerged and re-opened as #253 on a fresh branch with identical commits, to get a fresh
    SonarCloud analysis. The old analysis kept one finding (`javabugs:S2259`) at the same key and line through
    seven rewrites of the construct, and the dismissal verb answered HTTP 400.
  - One routed build hit its explicit 920 s bound and was re-run at 1800 s.
  - One flake in an untouched module (`pm-relay` `PmMcpTest.worker`), green on re-run.
  - The first routed build failed on a relative `--project-dir`.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed two heads (`33737ba`, `6a001a9`), 10 findings in total, all fixed or answered. A
  third review of the merged head was refused ("Review rate limited"), so the merged head `46673b5e` carries no
  CodeRabbit review. The merge went ahead on the operator's word ("Merge now via the merge queue"). Sourcery
  refused on its own 7-day budget and its check is skipped; it never gates the merge.
- CI/merge: `ci pr view 253` reads `state: merged`, merge commit `4efa0314…`, which is the `origin/main` base the
  completeness check resolved. All 30 checks on #253 are `pass` or `skipping`, including `build / build (25)`,
  `build (26)`, `quarkus-alignment`, `sonar-build`, `integration-tests / test` and `build / conclusion`.
  `ci pr view 250` reads `state: closed`, no merge commit, as the message says. No rebase conflicts.
- Sonar: the landing reports `open_issue_count=0`, `open_issue_rules=none`. Measured at the merged PR:
  `sonar_rest gate-status --pr 253` is `OK` (reliability 1, coverage 86.2 %, duplication 0.2 %), the SonarCloud
  check passes, and the Sonar MCP search for the PR returns no issues. `sonar_rest search --pr 253` returns four
  line-0 issues (the `javabugs:S2259` bug and three `java:S1192` literal smells) with no status field. They match
  the findings the landing says were driven to zero, and read as resolved history; that verb cannot tell resolved
  from open, so the `0` rests on the gate and the MCP read. Corroborated, no anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-03 --status shipped`
- [x] row `pr` stamped `253` — the anchor carried #250, which is closed; #253 is the merged PR
- [x] row `landing` stamped `landings/PLAN-03.md`
- [x] row `plan_marshall_plan_id` stamped `03-mock-scenario-states` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Surface under-declared; gate impact.** The merged footprint overlaps the declared surface of nine other specs.
  PLAN-02 and PLAN-76 are shipped. None of the in-flight plans (10, 15, 19) overlaps. The staged ones are PLAN-11
  (six files), PLAN-47, 52, 55, 58, 62 (an e2e spec each) and PLAN-50 (the operator transcript). Their premises
  were written against the old fixture values, so they are to be re-checked at HEAD before they are emitted.
  Recorded as a Watch and in Queue annotations; no spec was edited, because the plans were not read against the
  new HEAD here.
- **Likely repeat under-declaration in PLAN-11.** PLAN-11 changes mock fixtures and routes but does not declare
  the e2e specs or the transcript that assert fixture values. Recorded in the Watch.
- **Four lifecycle states are invisible in `pm-web-app`.** No staged web plan owns rendering them (PLAN-11, 17 and
  26 mention `system.*` but are mock and CLI plans). Recorded as a Watch.
- **Stale remote branch.** `feature/03-mock-scenario-states` (PR #250's head, `8cf8cfb`) still exists on the
  remote, although the landing reports `cleanup_owed=false`. Its own `-b` branch was pruned. Recorded as a Watch.
  Deleted from the remote on 2026-10-28 on the operator's word (no open PR, content contained in `4efa0314`).
- **Residue outside the boundary:** `doc/design/terminal-client/26-cleanup.adoc` still says 1.8 GB (the fixture
  says 1.9), and `MachineSettingsView.scenarios.test.ts` still uses `36 roles` as an input value. Recorded as a Watch.
- **Process deviations of the run** (shell file operations, one `perl -pi -e` rewrite, a `cd` into the worktree in
  one shell call): recorded here only. The guard was `none` on this harness, so nothing enforced them.
