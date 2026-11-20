envelope_version=1
sender_type=plan
sender_id=rb-168-cli-refusal-and-exit-residue
epic=ui-improvements
kind=landing
created=2026-11-20T16:09:10Z
revision=2
amended=2026-11-20T16:12:30Z

```landing-facts
schema=landing-facts/1
plan_id=rb-168-cli-refusal-and-exit-residue
epic=ui-improvements
pr=#511
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=50
review_body_findings_handled=50
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.analysed_revision=852dcb186dec7e800354d91d580e74e43610458c
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# Landing — PLAN-168 CLI refusal and exit residue

Merged as **`ad4d59606a5ee1b9cf4417b79ec119f411a47170`** (PR #511, merge queue, merge-group run
`44766105169` success). Last pull-request head: `852dcb186dec7e800354d91d580e74e43610458c` (a merge of
`origin/main` into the branch). Branch `fix/168-cli-refusal-and-exit-residue` and its remote ref are
gone; local `main` is at the merge commit.

`deliverables_done=2`: D1 and D3, whose every Done-when clause is met. D2 and D5 carry the gaps named
below, so neither counts as fully met; D4 is not met; D6 is met for the documents and not for the
captures.

## D1 — One refusal path: local, argumented and bounded — met

- (a) Local refusal renders no "runtime refused" prefix and no unknown-code cause:
  `RefusalRendererTest.localRefusalIsNoUnknownCode`. Order `mutation-after` (the local branch made to
  render the prefix and the unknown cause again), failing output
  `✗ The runtime refused: nuhtq unreachable`, log
  `/Users/oliver/.plan-marshall/marshalld/job-logs/3bd252cf4d994c01945845f47d33a93a.log`.
- (b) `enrolment_tampered` through `render(out, locale, exception)` carries the project id and no `{`:
  `RefusalRendererTest.renderPassesTheArgumentsOfTheBody`, and the argument values are sanitized in
  `bodyArgumentsAreSanitized`.
- (c) A 502 HTML body and a stack-trace body render neither tag nor class name and no more than the
  cap; a code-less 404 on a route the client does not own is `ROUTE_MISSING`, not "This entry does not
  exist": `RefusalRendererTest.bodiesAreNotTheMessage`, `codeLess404IsNoMissingEntry`.
- (d) `internal_fault` in `en` and `de` names no audit id:
  `RefusalRendererTest.internalFaultNamesNoAuditId`. `git grep -n -i "audit id\|audit_id"` over
  `doc/design/terminal-client` and both `error.*.properties` names only
  `03-dialogues.adoc:269`, which now says the code prints none.
- (e) The prefix is `operator.refusal.prefix`; `git grep -n "The runtime refused" -- pm-operator/src/main
  pm-api/src/main/java` names no Java literal. The dead `exitCode() == 4` branch is gone from
  `PmOperator`.

## D2 — Every runtime verb exits through the same refusal handling, and prints it to stderr — met with gaps

Met: `runtime upgrade --check`, `--withdraw` and the release-notes read throw the refusal and exit 1;
the machine-settings read prints the refusal; no `HTTP {` key remains in
`pm-api/src/main/resources/messages/operator.en.properties`; one-shot refusals of `plan`, `epic` and
`config cleanup` go to the error writer while the console and the entry flow keep their one stream.
Evidence: `RefusalStreamTest` (four cases), `PmOperatorTest.upgradeCheckFailedIsARefusal`,
`upgradeWithdrawFailed`, `upgradeNotesNotFound`, order `mutation-after` (the `plan` 404 refusal put back
on stdout), same log as D1; `ConfigCommandTest` for the settings read.

The gaps:

1. The status-without-`pid` test (`PmOperatorTest.startWithoutAPidPrintsALineAndExitsZero`) was
   added only in `00f2d194`, in the review cycle, not with the deliverable; its failing evidence is
   `mutation-after`, log
   `/Users/oliver/.plan-marshall/marshalld/job-logs/edab8c7afa4a4858b7bc55d75e482cf6.log`.
2. The terminal check was made with a PTY capture run
   (`/Users/oliver/.plan-marshall/marshalld/job-logs/3088453f0cbe4d21990051b6e0703628.log`), not with
   `ScreenCaptureIT` as the clause names, and `config cleanup` appears there only as
   `--remind never --yes` — one refusal, not the command-and-console pair the clause asks for.

## D3 — The decision and epic dialogue lines say what is true — met

- `AnswerDirectFormTest.dependencyRefusalNamesThePart` (the body's part, or the unnamed text the spec
  allows) and `dependencyRefusalWithAnUnrepresentablePartFallsBack` (a part beyond the `int` range
  falls back, log `/Users/oliver/.plan-marshall/marshalld/job-logs/0e85acb9408d47828ab1026e7f8875c8.log`);
  `invalidAnswerWithoutAReasonEndsInNoColon`;
  `ConfigCommandTest.cleanupRemindNeverIsRefusedWithItsOwnText`; `EpicCommandTest.noRaiseOnFullScope`;
  `ControlDialoguesTest.workActionNextScopeBlockedLeaveLine`.
- Order `mutation-after`: the mutation (the base minimum key for `--remind never`) failed the remind
  test; log `3bd252cf…`.
- The terminal check of `answer` and `epic next` in `en` and `de` at 80x24 is read (captures below).

## D4 — The tests of PLAN-30 made able to fail — not met

`ExitCodesTest` is unchanged from `2efcaa40`: it still covers `-h`, `--help`, `-V`, `--version` and
`status` on the success side and no per-verb refusal or usage case, `RANDOM_ID` is still built at class
load, and the D9 credential-path act-body assertion and the two mock-runtime pins are not touched. The
per-verb table needs one runtime fixture per verb; this run did not complete it and did not ship a
half-verified table. No mutation evidence exists for it.

## D5 — The display residue closed, and a guard that sees it — met with gaps

Met: every no-locale `MessageCatalogue.get(` on a screen takes the operator's locale; `plan report` in
`de` contains no English word outside the glossary (`PlanCommandTest.planReportInGerman`, order
`mutation-after`, the literal `outcome` label put back, log `3bd252cf…`);
`LiteralTextGuardTest` sees a literal in a ternary and inside `String.format` and guards
`PlanCommand`, `EpicCommand`, `StatusCommand`, `RolesCommand`, `DecomposeCommand`,
`ConfigCleanupCommand` and `DrainCommand`; `OperatorCatalogueHygieneTest.retiredCleanupKeysAreGone`
and `noOperatorKeyNamesAnHttpStatus` hold.

**Correction of the first landing.** The first report claimed that
`git grep -n "MessageCatalogue.get(" -- pm-operator/src/main` names no call without a locale. That
claim was false and is withdrawn. At the merged head the grep names two calls, both in the picocli
depth converter of `PlanCommand.java`: `:669` and `:673`
(`MessageCatalogue.get("operator.plan.depth_invalid")`), which is the `get` overload swap risk the spec
leaves out of scope as L3-17. The clause is met in the letter the plan wrote and not in its intent:
**15 `MessageCatalogue.format(key, …)` calls still pass no locale** — 10 in `PlanCommand.java`
(`:1032`, `:1469`, `:1472`, `:1560`, `:1570`, `:1577`, `:1579`, `:1617`, `:1624`, `:1629`) and 5 in
`ReportsCommand.java` (`:100`, `:103`, `:173`, `:271`, `:321`). `ProjectsCommand` passes the locale
everywhere. The `plan report` path itself is localized; these are the remaining default-language writes
on other screen paths.

The other gaps: the guard is a text guard — a literal in a variable, in another receiver, or a fresh
`new OperatorRefusedException("…")` passes it (residue below); and the catalogue test enumerates the
keys the renderer needs, not every key a screen can print.

## D6 — The design lines match the code, and the captures show what PLAN-30 D8 promised — documents met, captures not met

- Documents — met, order `none`: `03-dialogues.adoc:269` says what `internal_fault` prints, names no
  audit id, and states the one stream rule; `shared/work-actions.adoc:222` and
  `terminal-client/19-actions.adoc:190` now say what `WorkActionDialogue` does (with the running count
  unknown, `epic next` starts a launchable plan without asking and asks to raise the scope only when no
  plan is launchable), `control.epic.scope_unknown` kept for the no-launchable case;
  `26-cleanup.adoc` lists `operator.cleanup.choose.remind` as the pack has it; the four documents were
  read against the code with the `landings/PLAN-30.md` L1-2 table.
- Captures — **not met**: the refusal captures and `epic next` are present and read, but the `cleanup`
  **choice bar** and **delete confirmation** are not in the set. The view closed while the keys were
  typed (`Stream closed`), and the unproduced lines were removed from the set rather than left as a
  broken capture.

## Unrequested visible changes this run made

None of these are asked for by the spec; each follows from a clause and is named so the orchestrator
can weigh it:

1. `config cleanup` with an unrepresentable duration now exits **1** where it exited **3** (it passes
   through the one refusal path).
2. `reports read` now prints the plural form of the count (`✓ 5 reports marked read.`,
   `✓ 1 report marked read.`) where the text was fixed at one wording.
3. `web` device-list failures go to **stderr** when the command is not an interactive terminal (the
   stream rule of D2); the interactive console keeps its one stream.
4. `runtime upgrade --notes` on a 404 is now a generic refusal with exit **1**, where it printed
   "No release notes found." and exited **0**.

## Not confirmed

- `19-actions.adoc`'s rule that the runtime lists a row as launchable only when the scope has room is a
  HYPOTHESIS the run did not verify in `../pm-mcp-core`: D6's correction rests on the client's reading
  of `WorkActionDialogue`, not on the engine's contract. If the engine marks a row launchable without
  room, the corrected line is wrong and must be changed back.
- The body field names the client reads for D1(b) and D3 (`project_id`, `part`) are taken from the mock
  runtime and the error pack; the engine contract in `../pm-mcp-core` was not read line by line.
- The real `pm-mcpd`'s answer for an unknown plan id (code-less 404 vs a coded error) is not confirmed:
  the client now says `ROUTE_MISSING` for a non-JSON 404 and a code-bearing refusal for a coded one, and
  the epic and this plan differ on which a code-less 404 should be. The residue below carries that.
- `--coverage` was not run for this change; the build gate was `verify -Ppre-commit` plus
  `verify -Pintegration-tests -pl pm-operator -am`.

## Surface

The pull request touched 64 files. One of them is outside the spec's Expected Surface:
`pm-web-app/src/lib/views/ProcessDesigner.test.ts`, whose fixture pins the corrected
`error.internal_fault.fix` sentence ("report it with the command you ran" instead of "look the audit id up
in the log") — a consequence of D1(d), not a new web change. Everything else is inside the declared
surface: the four design documents, 18 message packs, 21 operator main classes, the operator tests and
`pm-operator/src/test/resources/screen-captures/PLAN-168.txt`.

## Residue the orchestrator takes

1. **D4** as written — the per-verb success/refusal/usage table of `ExitCodesTest`, `RANDOM_ID` per
   test, the D9 credential act-body route, the two mock-runtime pins, `PicocliLanguageTest`'s generator
   controller.
2. **D6's capture clause** — the `cleanup` choice bar and delete confirmation, both sizes, both
   languages, in the `ScreenCaptureIT` set.
3. D2's `ScreenCaptureIT` terminal check (the run used a PTY capture run instead) and its
   `config cleanup` command-and-console pair.
4. `LiteralTextGuardTest` passes a single-word literal, a literal held in a variable, a literal on
   another receiver, and a new `OperatorRefusedException("…")`.
5. Three `ConfigCommand` paths still print a cause-less refusal to `out`
   (`ConfigCommand.java:147-151`, `:1360-1363`, `:1501-1504`).
6. The "Failed to …" family of texts lost its cause when it went through the one path.
7. `enrolment.*.failed` still prints `HTTP {status}` — no key of `operator.en.properties` does, but this
   pack does.
8. The 15 no-locale `MessageCatalogue.format(key, …)` calls listed under D5, and the `get` overload
   swap risk L3-17 (`PlanCommand.java:669`, `:673`).
9. The code-less-404 question (epic vs plan, above).

## Review record

- CodeRabbit reviewed `bbefb54b` (2026-11-20T06:27:55Z) with **28 actionable items** in its body; each
  got its own PR issue comment (items 1–28), and the later follow-up item 29 did too. The first review
  also carried one inline Major and two outside-diff Minors, fixed in `66081ca7`.
- Reviews of the later heads were handled: `a5cb0919` → `be3f2c93` (a code-less 404 is no longer
  synthesized to `scope_not_found`); `0fb51a3d` → `148a5449` (the three outside-diff findings: the
  renderer's keys derived from `Kind`, `plan <id> --changes --file`'s missing-file refusal on stderr, the
  literal guard's brace exemption narrowed to JSON-shaped literals; plus its two inline threads).
- The last CodeRabbit review is of `0fb51a3d` (2026-11-20T13:22:56Z). Its check runs **SKIPPED** on the
  heads after it (`148a5449`, `b7cd7333`, `68679e38`, the `origin/main` merge `852dcb18`), so no review
  of those heads exists and none was pending. The heads after that review are the two Sonar clean-ups
  and the main merge; they change no behaviour the review had not seen.
- Final gate read at `852dcb18`: `unresolved_review_threads=0`, `review_body_findings_total=50`,
  `review_body_findings_handled=50`, `merge_allowed=true`, `reason=all_handled`. 13 review threads
  replied in-thread and resolved by this run.
- Sourcery (optional) refused structurally on the diff size, as on the first head.

## Gates at the merged head

- `./mvnw verify -Ppre-commit` green (full reactor) — log
  `/Users/oliver/.plan-marshall/marshalld/job-logs/979a23c2b4a245af98d089a1ac6e3139.log` at `852dcb18`.
- `./mvnw verify -Pintegration-tests -pl pm-operator -am` green — log
  `/Users/oliver/.plan-marshall/marshalld/job-logs/60543008a3f742aa9748e22ed7f0bfeb.log`.
- CI on `852dcb18`: build, integration tests, quarkus-alignment and SonarCloud Code Analysis pass. One
  run was red on the flaky `pm-api` test
  `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort`; the run was re-triggered and came
  back green on the same head, with no change to this branch.
- Sonar, read at `852dcb18`: `open_issue_count: 0`, no hotspots, `count_status: confirmed`; the
  round-trip left `none` (`step.sonar-roundtrip.open_issue_rules=none`, gate `ok`).

## Cleanup

- Worktree `.plan/local/worktrees/168-cli-refusal-and-exit-residue` removed without force.
- The worktree's only untracked entry was `pm-operator/.plan/` — the capture run's artifacts. Its 44
  files were copied to `.plan/temp/screens-PLAN-168/` and the directory was then deleted, so the
  worktree was handed back clean.
- Branch and remote ref deleted; local `main` pulled to `ad4d5960`.

## Lane facts

- `total_tokens` and `plugin_version` are `unknown`, and the drain's `landing-check` reports this landing
  incomplete at those two keys. `plugin_version` is unknown because `runbook_lane resolve` itself
  reports it so — a runbook run carries no `metadata.bundle_version` stamp — and `total_tokens` because
  the lane records no metrics for a run. Nothing was invented to fill either; `executor_version` is
  `0.1.2149` and the lane/executor skew read `match`.
- Mailbox read at setup: refused with `invalid_target_plan` (the plan id starts with a digit). Reported
  as a refusal, never read as no mail.
- Builds all ran through the build server (`daemon_longpoll`); the two 300s-floor bound expiries were
  re-run with `--timeout 1800`.
- Rule breaches of this run, named: shell `grep`/`sed`/`find`/`head` reads and pipes in the early
  rounds, and one `cp -R` to preserve the captures; every other file operation went through the file
  tools. No `git checkout`, `restore`, `stash` or `rebase`; two `git -C … add` batches staged by name.
- `executor-skew`: `match` (executor 0.1.2149, lane 0.1.2149); `spec_source=ledger_worktree`.
