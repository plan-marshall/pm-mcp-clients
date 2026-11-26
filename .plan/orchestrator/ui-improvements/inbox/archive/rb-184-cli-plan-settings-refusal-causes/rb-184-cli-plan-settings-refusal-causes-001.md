envelope_version=1
sender_type=plan
sender_id=rb-184-cli-plan-settings-refusal-causes
epic=ui-improvements
kind=landing
created=2026-11-26T05:02:29Z
revision=1
amended=2026-11-26T05:02:57Z

# PLAN-184 landing — the terminal's refusal names the cause the runtime sent

```landing-facts
schema=landing-facts/1
plan_id=rb-184-cli-plan-settings-refusal-causes
epic=ui-improvements
pr=#587
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=0.1.2183
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=2
review_body_findings_handled=2
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

Everything above is taken from `.plan/temp/rb-184-cli-plan-settings-refusal-causes/progress.md`
and the reads named in the sections below. Merge commit `9011e6d1b23ce5be1b2e63d601db71ab22b42837`;
the PR was merged by the merge queue (position 1, merge-group run `45757220035`).

## 1. Deliverables

### D1 — the refusal names the cause the runtime sent, and the invented pair is gone — met

- **Done-when 1** (coded `400`/`409` answers show their cause on stderr with exit 1, English and
  German; a revision `409 conflict` still conflicts; `422` no longer special): **met**,
  `order: test-first`. Tests `PlanCommandTest.InGerman.refusedInvalidArgumentsNamesItsCause`,
  `codedConflictNamesItsCause[team_owned, base_branch_fixed, plan_ended]`,
  `status422IsNoLongerSpecial`, `codedRefusalKeepsTheChoiceReasonFirst`;
  `ControlCommandTest.settingsCodedRefusalNamesTheCause[400 invalid_arguments, 409 team_owned,
  409 base_branch_fixed, 409 plan_ended]`, `settingsStatus422IsNoLongerSpecial`. Failing run:
  commit `7a3c770c`, output `080e5355363a480da1460bbae0427e81`
  (`.../marshalld/job-logs/080e5355363a480da1460bbae0427e81.log`), 12 failures, e.g.
  `PlanCommandTest:1227` "nqbdayzypp = ywge wird abgelehnt; nichts wurde geändert. ==> expected:
  <true> but was: <false>". Commit `2ef0f0ca` (with the review rounds' commits `d13d9d4b`,
  `b128921e`, `f8eafd4c`, `2189ae58`).
- **Done-when 2** (the `ControlCommandTest` replacement and the `--yes` retry refused with a coded
  `409`): **met**, `order: mutation-after`. `ControlCommandTest.settingsRetriedWriteNamesTheCause`
  fails with the retry's `refused(...)` argument changed back to `null`: commit `857dd6e0`, output
  `3208c441d2ba407eb6482f6b75898bd0`, `ControlCommandTest:488` "Multiple Failures (2 failures)";
  restored and green. The same order covers the dialogue's coded `409`
  (`settingsCodedConflictEndsTheDialogue`, `2c83c699ccbd4989a67145d6c3d69808`, "Expected
  OperatorRefusedException to be thrown, but nothing was thrown") and its coded `400`
  (`settingsCodedRefusalShowsTheCatalogueCause`, `ac3a856289eb4d22a7349aefe149e13a`), both mutated
  at `d13d9d4b`.
- **Done-when 3** (`./mvnw -pl pm-api,pm-operator verify` passes, the test number is higher by the
  added tests, catalogue parity): **met**, `order: none`. Whole-tree `verify` at `bf002bc7`: 5684
  tests, `48d43530698a4662a5e9caa8734bc957`; the branch adds 15 executions and removes 3 against its
  base (net +12). `ApiContractTest`, `ErrorTextsTest`, `ControlCatalogueTest`,
  `ChatTextsCatalogueTest` hold the catalogues. The exact `-pl` invocation was not run; the
  resolved whole-tree gate subsumes it.

Per-head runs at `bf002bc7`: `module-tests` 5684 (`03b9b6cbd63b4d389ba3be62e91e9c21`), `verify`
5684 (`48d43530698a4662a5e9caa8734bc957`), `verify -Pcoverage` 5684
(`2cefa0c884384f70a8501d59aa2c1255`), `verify -Ppre-commit` 5684
(`1bf99735d3da4ad0baa5c353423278b2`). Earlier heads carry their own runs (see the progress file).

### D2 — the code the specification leaves open is reported — met

- **Done-when 1** (PR body and landing name the open question with its three places, and state no
  code was chosen): **met**, `order: none`. PR #587 § _The open question_; the same section is
  repeated in § 6 of this landing.
- **Done-when 2** (the changed design lines pass `validate` and `verify-links`, docs review clean):
  **met**, `order: none`. `asciidoc validate` on `doc/design/terminal-client/02-levels.adoc`: 0
  errors, 0 warnings. `docs review`: 0 issues
  (`.plan/temp/rb-184-cli-plan-settings-refusal-causes/docs-review-02-levels-round2.json`).
  `verify-links` at the repository's own layout: 13 links, 0 broken
  (`links-main-02-levels-round2.md`); the worktree run reports 10 broken targets, all pre-existing
  cross-repository links whose sibling checkouts are absent at the worktree depth — the change adds
  no link and the 13-link set is identical in both runs.

No base worktree was created; the failing run was taken at the branch's own base (`7a3c770c`), so
nothing is owed and `cleanup_owed=false`.

## 2. Personas and skills

| Step | Persona | Skills |
|---|---|---|
| D1 implementation | `plan-marshall:persona-implementer` | `pm-dev-java:java-core`, `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing` |
| D2 documentation | `plan-marshall:persona-documenter` | `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation` |
| Both self-review rounds, review triage | `plan-marshall:persona-code-reviewer` | — |

`skills added: pm-dev-java:java-core (…)` no skill was added for surface drift beyond the declared
sets. The `pm-mock-runtime` files the gate rewrote, and `ChatRegistry.java`, were touched without the
`pm-dev-java` skills re-loaded for a deliverable; they are `pm-operator`'s sibling module and the
standards are the same.

## 3. Hypotheses

- **H4** (a coded `409` body parsed by `PlanSettingsUpdateResult.fromJson` ends on the neutral line
  with exit 1 and no cause): **confirmed** by reading and by the failing run —
  `PlanCommand.java:1736-1743` parsed every `200`/`409` as a result, `apply` then reached
  `refused(...)` with no refusal. Refuted after the fix, as the coded-`409` tests show.
- **H5** (`PlanSettingsDialogue:181` and the one-shot can share one text per code): **refuted** —
  they shared no text; sharing needed `WorkActionsClient` to carry the code, which it now does
  (`SettingsAnswer.refusalCode`), and the dialogue folds the code's title into its one line.

## 4. Findings

- **Self-review round 1** (sub-agent, `persona-code-reviewer`): F1 the dialogue printed the runtime's
  English reason on a German screen — fixed; F2 the coded-`409` dialogue change was untested and its
  javadoc false — fixed (two tests, mutation runs); F3 the `409` discriminator was duplicated — fixed
  into `PlanSettingsUpdateResult.resultOrNull`; F5 tests fed the undefined `422 invalid_value` pair as
  a default — fixed; F7 a refusal body carrying `state` could be read as a result — fixed and
  unit-tested; F10 a stale "value code" javadoc — fixed. F4 (English leg in `ControlCommandTest`) and
  F6 (`verify-links` artifact at the canonical layout) were answered by argument, recorded in the PR.
- **Self-review round 2**: N1 a DisplayName still said "value code" — fixed; N2 `resultOrNull`'s
  javadoc over-promised — fixed; N3 a flat `code` beside `state` was still read as a result — fixed
  (result evidence required, both shapes tested); N4 the design sentence claimed the one-shot's rule
  for the dialogue — fixed for both paths; N5 "cause" vs "title" wording — fixed; N6/N7 out of scope,
  residue; N8 the head was not gated — closed by the runs at each head; N9 mutation commit ids — now
  recorded. Round 2 reported nothing blocking.
- **CI**: `build (25)` and `sonar-build` failed at `11654625` on `ChatTextsCatalogueTest[79]` —
  `main`'s `#585` had changed the design table and the mock producer but not the catalogue row. Fixed
  in `bd521fab`; disclosed in the PR as the base branch's regression.
- **SonarCloud**: three findings on the new code at `fbb761aa` (S1192 ×2, S3358) and one at
  `fbb761aa`'s successor (S2637) — all fixed in `fbb761aa` and `f8eafd4c`; final read at `bf002bc7`:
  0 issues, gate OK.
- **CodeRabbit** (at `f8eafd4c`, two actionable findings, both fixed in `2189ae58`): the mock's
  `decision.opened` producer sent `blocks=yes/no` while the catalogue selects by
  `plan`/`epic`/`setup`/`entry`/`task`; the settings dialogue discarded the cause of a retry refused
  after a conflict. Both threads replied and resolved (`PRRT_kwDOVBOtss68MWtf`,
  `PRRT_kwDOVBOtss68MWti`).
- **Sourcery**: approved `f8eafd4c`, no finding.
- **Progress-file disposition lines**: `finding: coderabbit inline 1 — 2189ae58`,
  `finding: coderabbit inline 2 — 2189ae58`.

## 5. Reviewers

| Reviewer | Kind | Result | Head reviewed | History |
|---|---|---|---|---|
| CodeRabbit | required (quota-limited, `review_complete_after_handling`) | reviewed | `f8eafd4c` (marker `kind=reviewed`, `coveredCommitId` `f8eafd4c`) | triggered 20:30Z, refused on quota 20:30Z and 00:50Z (90-minute policy wait observed), re-triggered 02:23Z, reviewed 02:29Z; 2 findings, both handled; threads resolved |
| Sourcery | optional (quota-limited) | reviewed | `f8eafd4c` | refused on its own budget 20:30Z, later APPROVED at 00:24Z; no finding |

Standing review authorization was not needed: CodeRabbit's review exists and every finding of it is
handled, so it is the review at any head of this PR.

## 6. The open question (D2), and the operator

The Runtime Model specification names no code for "a value the runtime refuses itself". The three
places that mention the gap:

1. `doc/design/web-app/machine-settings.adoc:214` — the web app reads `invalid_value` on a `400` as
   its wire code, a code in no closed catalogue.
2. `doc/design/terminal-client/01-commands.adoc:62` — the `plan settings` row, and the rule now in
   `02-levels.adoc:108`: the client shows the code the runtime sent it, and names no code of its own.
3. The Runtime Model error list (`plan-marshall-documentation` / `pm-mcp-core`,
   `runtime-model/01-processes-and-transport.adoc`, the `400` row and the `409` list) — no member
   answers "the runtime refused the submitted value".

No code was chosen. The owners of the catalogue and of the Runtime Model specification decide it.

**Operator questions asked in this run: none.** No free-text operator message was received. When the
SSH session was interrupted by the environment, the run resumed from the progress file rather than
asking.

## 7. Rule check

`guard: none on this harness (unenforced)` — the resolve result reported `guard: none`, so no
guard-log line exists and "all kept" is not claimed.

The run's **own** actions, counted by the model rather than from a guard log:

- **Shell file operations issued** — these breach § 1's "files are read and searched with the file
  tools": `grep` in ~15 shell calls (test and property searches), `sed -n` twice (imports and the
  `docs review` range), `ls -d` and `ls` twice (the `target/` layout), `find` once (locating a
  surefire report), and `cp` once (copying the PR body into the prepared edit slot). No `cat`, `head`
  or shell file write outside `.plan/temp/` and the `ci` body slots.
- **Routed builds**: 40, all `mechanism=daemon_longpoll`, no in-process fallback. One bound expiry:
  the first mutation run of the dialogue test (`995bbc7abf6f46bcb63ae9860b41355b`, 1322 s) hung
  because the mutated dialogue looped on scripted input that never left; the test input was made to
  leave so the mutated run fails fast (`2c83c699…`). No killed job.
- **Operator messages**: none. **Model switches**: none (`deepseek-v4.1-flash/high` throughout).
- **Restarts**: one — an environment interruption; the session re-read the lane text and resumed at
  the first step not recorded as done.
- **Sub-agents**: two self-review rounds, both completed and neither killed.
- **Spec read**: `spec_source: ledger_worktree`, no `spec_differ` marker. **executor-skew**:
  `match` (executor 0.1.2183, lane 0.1.2183).
- **Harness bridge defect**: the resolved `inbox_read_command` (`--plan-id 184-cli-plan-settings-refusal-causes`)
  is refused as `invalid_target_plan` because the plan id starts with a digit; the read was made with
  the sender id instead. This is a runbook-lane / inbox address defect, not a plan defect.

## 8. Build server

Setup preflight: `ready` (registered, daemon up). Every build routed: `mechanism=daemon_longpoll`
for all 40; no `in_process_fallback`, no `degraded`, no launch-fault retry. One `timeout` (the
mutation hang above), no `killed` job. `build_timeout_seconds` was re-resolved as 877 at landing
time; the runs used the 1322 s bound the earlier resolve supplied. `runtime_stamp_reason`: none.
`plugin_version` is read through `executor-skew`'s `lane_version` (0.1.2183, the version of the lane
text this run executed, sourced from the flat install's stamp), because the runtime-info seam itself
answered `unknown`. `total_tokens` stays `unknown`: the run kept no token accounting, and a number
would be a guess — the inbox `landing-check` therefore reports the facts block incomplete on that one
key.

## 9. Residue

- **Inherited, untouched**: on `--yes`, a second conflict (or a too-late `409` result) still prints
  the neutral line with exit 1 instead of the conflict path, and the one-shot still does not say "too
  late" where the dialogue does.
- **The `chore(build)` commit message of `857dd6e0`** says the `Pattern` import form was "already
  that way on `main`"; that is wrong, and the correction lives in the PR body.
- **The inbox**: no mailbox could be read (`mailbox_state: no_mailbox`, `delivery_state:
  never_delivered`); nothing was delivered, and nothing is reported as "no mail".
- **Cross-repository links** in `02-levels.adoc` cannot be resolved from a worktree; the canonical
  layout check is the recorded artifact.
