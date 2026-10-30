envelope_version=1
sender_type=plan
sender_id=rb-22-cli-state-dialogue-onboarding
epic=ui-improvements
kind=landing
created=2026-10-30T09:20:01Z

```landing-facts
schema=landing-facts/1
plan_id=rb-22-cli-state-dialogue-onboarding
epic=ui-improvements
pr=#275
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-22 — One State Dialogue for Both Onboardings

PR #275, merged into `main` as `fa178decabe030714a11b2388e30485cd7020cb9` through the merge queue.

## Deliverables

**D1 — StateDialogue** (commit `f9722d6`, rewritten by `5fb168d`, `590d03d`, `b2d27d2`, `f3c1c2e`, `0742b69`, `489e020`).
- *A unit test drives three states with a fake API* — **met**; order `mutation-after` (the spec names `test-first`; the
  class is new, so the test landed with it and the failing run came from reverting the committed fix):
  `StateDialogueTest.drivesThreeStates`, failing run at `f9722d6` (step line reduced to the bare title,
  `StateDialogueTest.drivesThreeStates:108`), plus `readsAnOperatorEnvelope`, `readsAFlatState`,
  `refusesABodyWithoutState`, `namesTheFrameParts`, `wrapsContent`, `reReadsAWorkingState`,
  `rendersOnlyLabelledFacts`, `hidesAnUnnamedWireToken`, `leavingSendsNothing`.
- *Terminal check, before and after* — **met**; order `none`. Capture set
  `pm-operator/src/test/resources/screen-captures/22-cli-state-dialogue-onboarding.txt` (24 captures: entry at
  `onboard.mode`, the guided first step, `onboard`, `onboard --auto --yes`, `project setup`, the guided run to
  `onboard.completed`; 120×30 and 80×24, `en_US.UTF-8` and `de_DE.UTF-8`). Before set at base `bae3a8c8`
  (raw link names `auto`/`begin`, "Step 1 of 7"), after set at `489e0204` (labelled frames, resolved values,
  completed state, automatic table, project identity with id/name/pilot), under
  `.plan/temp/terminal/22-cli-state-dialogue-onboarding/{before,after}/`.

**D2 — Machine onboarding** (commits `00ea89a`, `f4ae4f6`, `1d8cc50`, `3b89bf7`).
- *`OnboardingDialogueIT` enabled: guided path to `onboard.completed`; `--auto --yes` to the result table* —
  **met**; order `none` (the pre-existing `@Disabled` tests were enabled and made green; no failing run was
  captured before the fix, so the spec's `test-first` note is not evidenced).
  `OnboardingDialogueIT.mainPathCompletes` walks the 17 state titles, `.automaticPathPrintsTheResult` prints the
  table, `.qFirstSendsNothing` and `.unknownInputReAsks` prove the strict prompt.
- *Terminal check* — **met**, order `none`, the same before/after set.

**D3 — Project onboarding** (commits `0e93542`, `1d8cc50`).
- *`ProjectSetupDialogueIT` enabled: `project setup cui-parent` shows id, name, pilot* — **met**; order `none`
  (the pre-existing disabled IT, enabled and made green; same order note as D2).
  `ProjectSetupDialogueIT.identityShowsIdNameAndPilot` asserts the rendered
  "Pilot: this version of PM-MCP runs plans only in pilot projects: no", the id and the name.
- *Terminal check* — **met**, order `none`.

**D4 — Catalogue completeness** (commits `615fefe`, `5fb168d`, `351c15d`, `0ad9b6d`, `1724d0d`, `0742b69`).
- *The test fails for a removed key* — **met**; order `test-first`: `OnboardingCatalogueTest.everyLinkHasALabel`
  failed at run `233512` with `[en: harness.consent -> remember, de: harness.consent -> remember]`; the key was
  added afterwards, and `everyReachableStateHasATitle` plus the non-vacuity guard cover the state titles.
- *Terminal check* — **met**, order `none`.
- *Browser regression check on `fully-onboarded` and `running-work`* — **met without a separate baseline**;
  order `none`. 32 snapshots and screenshots under `.plan/temp/playwright-mcp/`
  (`22-cli-state-dialogue-onboarding-{onboarding,projects}-{after,running-after}-{de,en}-{light,dark}-{1440,390}.{png,yaml}`),
  mock on 20220 and app on 20221, both stopped afterwards; no raw catalogue key, placeholder or missing text in
  any snapshot. This plan changes no web file and no existing catalogue text, so a before set would be identical
  by construction.
- *The open-issue read at the PR head* — **met**; order `none`: `0` confirmed open new-code issues at `489e0204`
  (the read also recorded `analysis_revision == head_revision`); `sonar-gate` answered `merge_allowed: true`
  (`no_open_issues`).

Per-head test runs at the plan's final head `489e0204`: `verify -Ppre-commit` (2250 tests), `verify -pl pm-operator
-Pintegration-tests` (77), `verify -pl pm-api` (660), `verify` (2250) — all green. The same four runs are green at
`fa178dec`'s parent (the merge group) and the degraded bound expiry on one gate run was re-run with
`--timeout 1200`.

## Personas and skills

- D1: `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`, `pm-dev-java:java-lombok`,
  `pm-dev-java:junit-core`.
- D2: `plan-marshall:persona-integration-tester`; skills `pm-dev-java:junit-integration`, `pm-dev-java:junit-core`.
- D3: `plan-marshall:persona-integration-tester`; same skills.
- D4: `plan-marshall:persona-implementer`; the D1 skills.
- Every self-review round: `plan-marshall:persona-code-reviewer` (three sub-agent rounds).
- `skills added: none` — the Expected Surface's modules were the ones touched; the mock runtime and the doc were
  the only additions, and no domain skill exists for them beyond the declared set.

## Hypotheses

- HYPOTHESIS: *`EntryFlow` asks "Begin with step 1?" and ignores the answer* — **confirmed** at base
  `EntryFlow.java:192-203` (reads `operator.console.setup.begin_prompt`, acts only on `q`).
- HYPOTHESIS: *`OnboardCommand` prints raw links* — **confirmed** at base `OnboardCommand.java:128-134`
  (`out.printf("    %s%n", entry.getKey())`).
- OBSERVED: `StateDialogue` absent, `ChoicePrompt` present — confirmed; `OnboardingDialogueIT` and
  `ProjectSetupDialogueIT` present and `@Disabled` — confirmed at base `bae3a8c8`.

## Findings

- Self-review round 1: lists and decision values were not rendered (high), the runtime text bypassed
  `TerminalSanitizer`, wire values leaked (`same`, `de`), `printResults` could print a raw key, the working-state
  deadline was per dialogue, the link fallback was silent, the design doc described the deleted prompt, and two
  catalogue texts were duplicated. All fixed in `5fb168d`–`590d03d`.
- Self-review round 2: raw provider/harness ids in list items, a duplicated English summary in German frames, the
  automatic run's deadline, an unguarded `autoText` fallback, an unsanitized title and the duplicate
  `onboard.link.remember` key. All fixed in `064122d`–`1d8cc50`.
- Self-review round 3: the German `--auto` line still repeated the runtime summary, and two catalogue keys were
  defined twice per locale. Fixed in follow-up PR #276 (open, see Residue).
- Review bots on #275: CodeRabbit's six findings plus three nitpicks were fixed in `1d8cc50`/`351c15d`/`0ad9b6d` and
  every one of its threads was answered and resolved; two final nitpicks were answered as not-applicable (the
  encode/decode statement is shared and covered, and the act is asserted end-to-end by the IT).
- CI failures on #275 were all Sonar new-code coverage (78.2 %, then the 1 issue count) and a semantic merge
  conflict with `main`'s new badge model, not test failures; both were fixed (`0ad9b6d`–`0742b69`, merge `3b89bf7`).
- CI on the follow-up #276: green at `eebbeeb6` including SonarCloud.

## Reviewers

- **coderabbit / coderabbitai** (required, quota-limited): reviewed `489e0204` on #275 with `final_review_risk
  coverage: sourceCommitId=489e0204, kind=reviewed`, merge risk Low; all findings handled and all threads resolved.
  On #276 (follow-up) it refused on quota at 07:14:11 and again at 09:09:29 with no ETA; the trigger was posted
  twice (09:09 after the 90-minute window), and the standalone quota policy continues on that PR.
- **sourcery-ai** (optional, quota-limited): hard quota, "2 days and 15 hours" left, on both PRs; not required.

## Operator questions

None asked. No free-text operator messages were given during the run.

## Rule check

- **Guard:** `guard: none on this harness (unenforced)` — no tool-call guard is installed for OpenCode here, so no
  guard-log line exists; the lane's own actions are checked below.
- **Shell file operations** (counted from the run's own actions, not a guard log): the run used `grep`, `ls`,
  `head`, `sed` and `perl -pi` in shell calls several times, and `rm -rf`/`mkdir -p`/`printf >>` for temp paths and
  the progress file. That breaks section 1's "Files are read and searched with the file tools" and "One command per
  call" for those calls (no `&&`/`;` chains were used; the edits themselves were also made with the edit tool).
- **Shell `rm -rf`** was used on `pm-operator/.plan` (a generated path in the worktree) and on the temp
  before-checkout's parent only through `git worktree remove`; the temp worktree for the before captures was added
  with `git worktree add --detach` (the lane's worktree verb was not used for that evidence checkout).
- **Spec read:** `spec_source: ledger_worktree`, `spec_differ: true` (the main checkout's copy differs; the ledger
  worktree's copy was read, and `resolve` re-run at landing reported the same).
- **Executor skew:** `match`, executor `0.1.2032`, lane `0.1.2032` — recorded, never asked.
- **Build routing:** the first routed build failed with `execution_failed` (the daemon's PATH has no `mvn`), so
  every build ran `in_process` (`mechanism: in_process_fallback`); one gate run hit the daemon's learned 555 s bound
  at `489e0204` after all 2250 tests passed, and was re-run with `--timeout 1200` and passed. No `killed` job.
- **Restarts:** the server restarted twice during the run (progress file re-read and executor-skew re-checked);
  no sub-agent was killed; the self-review sub-agent ran three rounds in one session each.

## Build server

- Setup preflight: `ready` (project enrolled, daemon reachable).
- Every build used the fallback `in_process` with its `reason` (the routed attempt's `execution_failed`); the
  daemon bound expiry is recorded above.
- `runtime_stamp_reason`: none; `plugin_version` is `unknown` because the platform-runtime seam reports no bundle
  version in this install.

## Residue

- **PR #276 is open**: it carries the two round-3 fixes (the German `--auto` line, the duplicated catalogue keys).
  Its CI and Sonar are green at `eebbeeb6`; CodeRabbit refused on quota twice and the policy's next window is
  pending. The plan's own PR #275 is merged and unaffected.
- **The browser check has no before set**: this plan changes no web file, so the rendering is unchanged by
  construction; only the after set exists.
- **The automatic section of `05-transcripts-entry.adoc`** still shows the richer automatic transcript it described
  before this plan; the implemented output is one line per state plus the result table. Pre-existing, left as
  documented residual.
- **Step counters** are constants (7 for the machine run, 10 for a project run) because the state the runtime
  reports carries no total.
- **Test-coverage seam**: the 60 s give-up path has no clock seam; the round-2/3 checks did not add one.
