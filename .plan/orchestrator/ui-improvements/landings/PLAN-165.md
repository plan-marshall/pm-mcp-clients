# Landing Analysis: PLAN-165 — Lists derived from one definition, Playwright green and web coverage in Sonar

epic: ui-improvements
workstream: WS-04
pr: #503 (merged as a8ac159d, 2026-11-19; 20 files, +1072/-81)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-165-derive-lists-from-one-definition-001`, `landing-check` complete: false, `total_tokens` and `plugin_version` `unknown`) after verifying claims against ground truth. The plan derives three hand-kept lists from one definition each and brings the web module's unit coverage to Sonar.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 dialogue keys derived | met | **met** | `CUT_KEYS`, `PLAN_KEYS`, `READ_BACK_KEYS` (`EpicDecompositionDialogue.java:98-113`) and `offeredLine(keys)` (`:223`): the accepted set and the offered line read one list; the branches switch over the act (`CutAct`, `PlanAct`, `ReadBackAct`), so a new act is a compile error; transcripts byte-identical (nothing under `src/test/resources`, `ControlDialoguesTest` and `StartDialogueTest` untouched, the literal lines pinned there since before) |
| D2 `CleanupKind` derived | met | **met** | `KindDisplay` and `KIND_DISPLAYS` built by iterating `CleanupKind.values()` through an exhaustive switch; the hand-written list is gone; order and labels as before; a German durations test is new (not run against the old code by the leaf) |
| D3 refresh events from one exported set | met | **met-modified (operator-approved)** | `stores/refreshEvents.ts` exports three sets read by four consumers (`runtime.ts`, `ProjectExplorerView`, `StartView`, `PlanView`); compared with the literals at `ac538733`: exactly two names differ, `project.released` and `project.enrolled`, dropped on the operator's answer to the lane's question; re-read: `EventName` holds only `project.work_started`, `project.work_ended`, `project.changed` (`:113-119`); the mock broadcasts the two dropped names, each paired with `project.changed` (`MockRuntimeEngine.java:9798-9800`, `:10284-10285`), so no view loses a refresh (a release now causes one explorer read, not two) |
| D4 e2e result recorded | met | **met, not decidable by reading** | nothing under `pm-web-e2e` changed; the build logs show 456 passed twice; the lane's "the showroom/designer hypothesis is refuted" and "no subject" are its account |
| D5 `-Psonar` writes the web LCOV | met | **met by construction, Sonar read not checked** | new `sonar` profile with the `test:coverage` override (`pm-web-app/pom.xml`); no `.github/` file in the diff |
| D6 coverage and Sonar condition | met | **met by logs, Sonar read not checked** | coverage logs: branch 79.96 → 80.14 %, `lineDiff.ts` 66.66 → 95.83, `projectCounts.ts` 68.75 → 100, `devRouteExclusion.ts` → 96.66 (its before-figure 73.33 not read), 176 files / 3046 tests; thresholds of 80 % added in `vite.config.ts`; `sonar.coverage.exclusions` dropped after the profile exists (34763b06 then 431c478d, one PR) |

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3, 22 + 7 + 10 numbered groups); the orchestrator re-read the points marked "re-read" at `a8ac159d`: `EventName`, the mock's broadcast sites, the pom diff, the untouched PLAN-169 surface, the missing annotation on `EpicDecompositionDialogueKeysTest`, the moved `CleanupCommand` lines, `OverviewView`'s own list. By reading and the leaf's own read-only scripts: no build, no test run. **Not read by the leaf:** Sonar results and thread state, the 224 `SF:` entries of `lcov.info` (no file), the before-figure of `devRouteExclusion.ts`, any real emission site of `project.changed` in the engine (none found in `pm-mcp-core` or `plan-marshall-mcp`), the progress record, the bodies of the views beyond the diff hunks.

- **Dispatch:** one read-only leaf, which also re-read the line references of the staged specs PLAN-168, PLAN-169 and PLAN-171 against the merge.
- **Verdict:** no blocker, no major, ten minor or latent. The two dropped event names are the only behaviour change.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | `deliverables_done=6` | holds for D1 to D4 and D6's logs; D5 and D6's Sonar clauses are the lane's account | with the lane | the Sonar search for #503 returns no issues (re-read); `new_coverage` 99.0 % is not re-read |
| L1-2 | D3 "refresh on exactly the events they refreshed on before" | holds except the two operator-approved drops; **evidenced only by a one-time diff** | partly | L3-1 |
| L1-3 | the Expected Surface | **met-modified** | against the lane | `PlanView.svelte`, `refreshEvents.ts` and its test, two new Java tests and three D6 test files outside the declared directories (`dev/`, `display/`, `utils/`) are in the diff; the message names the directories, not `PlanView.svelte` |
| L1-4 | the spec's "no change to the CI files" | holds | with the lane | no `.github/` file in the 20 files; CodeRabbit's inline-4 was declined on that ground |
| L1-5 | what the diff changes besides the spec | **not stated in the message** | against the lane | `sonar.test.inclusions` is extended (`**/setupTests.ts`, `**/src/test/**`, `**/src/lib/test/**`) because the dropped exclusion no longer hides those files (re-read); it appears only in commit texts |
| L1-6 | the 100-file question | does not apply | with the lane | 20 files |

### Layer 2 — Implemented

- **L2-1:** `EpicDecompositionDialogueKeysTest` can fail: literal offered lines, literal act and effect tables written independently of the pairing, and the rendered effects; limit: a key accepted by a hand-written branch outside the list is not detectable (a re-introduced literal). `CleanupDurationsKindsTest` figures are independent; a label swap between kinds is caught only by the literal English and German lines in `CleanupCommandTest`.
- **L2-2:** the four consumer tests iterate `it.each(<the exported set>)`, so **removing a member from a set passes every test**; only `reset`, `source.added` and three negative cases are literal. The membership check against `EventName` is a real failing check.
- **L2-3:** the catalogue parser is copied in three web tests (`refreshEvents.test.ts:28`, `epicEvents.test.ts:20`, `intentEvents.test.ts:28`) with the same regex and path; the copies agree.
- **L2-4 (re-read):** `EpicDecompositionDialogueKeysTest` has neither `@EnableGeneratorController` nor a `Generators.` call (count 0); its typed unknown key `z`, scripted answers and ids are hand-written input. `CleanupDurationsKindsTest` and the new German case in `CleanupCommandTest` draw inside the test.
- **L2-5:** the key and act switches are exhaustive; every accepted key is offered and every offered key is accepted at `a8ac159d`.
- **L2-6:** non-test changes: `CleanupCommand.java` (derived mapping, two constants), `EpicDecompositionDialogue.java` (derived lists; `offered` and `offeredLine` package-private for the tests), `refreshEvents.ts`, `runtime.ts`, three views, `pom.xml`, `vite.config.ts`; behaviour unchanged except D3's two names and the build and Sonar configuration.
- **L2-7:** the three Sonar fixes (9b407643: `answerCut` extracted, `KIND_UNENROLLED`, a method reference) match their commit text and change no behaviour.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | the four consumer tests | a member removed from an exported set | no test fails; the spec's "exactly the events before" is evidenced once, by the diff | minor |
| L3-2 | `pm-web-app/pom.xml`, `vite.config.ts` | a web change adds about 15 uncovered branches (of about 11,123) | the margin is 0.14 pt (80.14 vs 80; a run varied to 80.07), and the Sonar job now runs `test:coverage` with the floor, so the build fails; the floor is not enforced on the Dependabot path (`test:ci`) | minor |
| L3-3 | `pm-web-app/pom.xml` comment | — | Sonar's JavaScript analyzer indexes `.ts` and `.js` only, so the 99.0 % new coverage covers no `.svelte`; most of the app (the views) is not under the new-code condition (the lane states the limit; whether the analyzer skips `.svelte` is not verified) | minor, acknowledged |
| L3-4 | `EpicDecompositionDialogue.java` | an empty key list; duplicate keys (the tests compare through `LinkedHashSet`); a digit-spelled key (shadows a plan number, `accepted()` runs before `planAt`) | an offered line with a leading ", "; not detected; a shadowed plan | minor, latent (none reachable with the current constants) |
| L3-5 | `OverviewView.svelte:331-340` (re-read), `views/epicEvents.ts`, `components/intentEvents.ts` | a name renamed in one place | `OverviewView` keeps a literal seven-name list that nothing tests against `EventName`; "one name has one home" holds only inside `refreshEvents.ts` | minor (named by the lane) |
| L3-6 | `pm-api` `ProjectReleasedEvent.java:36`, `ProjectEnrolledEvent.java:34` | — | payload records for events the closed catalogue lacks, produced only by the mock; the message calls the drift "in a module this plan does not own": the records are in `pm-api`, in this repository | minor |
| L3-7 | commit text of 23505a08 | — | says nothing in this repository streams the dropped names; the mock does (1711d45b corrected the code comment, not the commit text) | minor |
| L3-8 | `.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-19-073919.log` | — | an earlier e2e attempt ended `BUILD FAILURE` (`vite: command not found`, exit 127) three minutes before the green run; an environment failure, not reported | minor |
| L3-9 | `EpicDecompositionDialogueKeysTest` | — | typed literal input in a class that carries no annotation, against the test-data rule; the message does not claim otherwise | minor |
| L3-10 | `CleanupDurationsKindsTest` javadoc | — | says the expected figures are written as a switch over the enum; there is none | minor |

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec Standing Constraints | no new dependency or plugin; no CI file change | conforms | L1-4 |
| CLAUDE.md Code Standards | input from `Generators` with `@EnableGeneratorController` on every class that takes input | partly | L3-9 |
| the lane rules | no shell file operations | **deviates, self-reported** | the lane read logs and listings with `tail`, `grep`, `ls`, `find`, `awk` (message section 7) |
| spec D3 | an unreadable name is reported to the operator | conforms | the lane asked, the operator chose to drop the two names (section 6) |

## Metrics and Anomalies

- Tokens `unknown`, plugin version `unknown` (harness `opencode`, model `deepseek-v4.1-flash/high`, effort high, executor 0.1.2149): unobserved, not zero.
- Anomalies: three self-review rounds; three Sonar findings fixed; one `git commit --amend` of an unpushed commit (a mistyped trailer domain); the unreported e2e environment failure of L3-8; the lane's inbox address `165-derive-lists-from-one-definition` starts with a digit and is refused by `inbox read` (`invalid_target_plan`), the mailbox was read by the queue id.

## Routing and Merge Behavior

- Review gate re-read with `runbook_lane review-gate --pr-number 503`: `unresolved_review_threads=0` (lane 0, **same**), `review_body_findings_total=12` (lane 12, **same**); `handled` reads 0 and `merge_allowed: false, body_findings_undisposed` now, where the lane read `all_handled`: the disposition record went with its worktree (the counting gap, now on #438, #497, #500, #502, #503).
- Sonar: lane `sonar_open_new_issues=0`, quality gate ok; the Sonar search for pull request 503 returns no issues (0, **same**); `new_coverage` 99.0 % not re-read.
- CI/merge: merged through the merge queue as `a8ac159d` (re-read: `MERGED`, 20 files, +1072/-81). Sourcery's request to re-add `project.released` and `project.enrolled` was declined on the operator's explicit selection.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 503; `landing` `landings/PLAN-165.md`; `plan_marshall_plan_id` `165-derive-lists-from-one-definition`
- [x] inbox: `rb-165-derive-lists-from-one-definition-001` archived as consumed
- [x] staged specs re-read against the merge: PLAN-169's overlap with PLAN-165 is refuted (#503 touched none of its 23 files) and removed; PLAN-168's `CleanupCommand` lines moved by +5 (`:568,586` → `:573,591`) and its overlap note is updated; PLAN-171 gains `EpicDecompositionDialogueKeysTest`
- [x] epic.md: an Open Defect entry for this landing; resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: the minors above; L3-1 and L3-4 are the two that make the new definition weaker than it reads (a dropped set member and a duplicate key pass), L3-9 is added to PLAN-171's scope.
- Residue the lane names, not staged: folding `OverviewView.svelte` and `views/epicEvents.ts` into the exported sets (needs more sets), the mock's two extra broadcasts and the two `pm-api` payload records, the Dependabot path not running the coverage profile, the normal dialogue menus hardcoded in the message catalogue (CodeRabbit inline-8).
- For plan-marshall: `inbox` address derivation from a plan id that starts with a digit (`invalid_target_plan`, the mailbox read refuses it; now from three plans); `review-gate` counts a disposition it cannot see as undisposed once the progress file is gone; a lane that reads logs with shell tools.
