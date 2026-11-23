# Landing Analysis: PLAN-174 — CLI: The Refusal Residue, Second Round

epic: ui-improvements
plan: PLAN-174 (`plan-174-cli-refusal-residue-second`, WS-05, `plan-marshall` lane)
pr: #533, merged as `fcf87728` (merge queue); first parent `3ae8a305`

> Landing record for one shipped plan. Written by `analyze` (inbox messages `plan-174-cli-refusal-residue-second-001..009.md`: eight candidate lessons and the landing, `plan-marshall` lane, harness `claude`, model `claude-opus-5-5`, effort `low`, executor `0.1.2173`; analysed 2026-11-23). Three-layer reading by one read-only leaf from `git show fcf87728:` and `git diff 3ae8a305 fcf87728`; every point marked "re-read" was read at `fcf87728` (by the leaf; the orchestrator re-read L3-1, L2-1 and the Sonar literals itself). Nothing was built or run.

## Deliverable Fidelity vs Spec

| D | Verdict | Evidence |
|---|---------|----------|
| D1 the exit-code table of every verb | met, some clauses unevidenced | verb registry checked against `PmOperator`'s subcommands, a verb with no case fails (`ExitCodesTest.java:241-367,637-648`); refusal rows for web, runtime, drain, credentials, recipe, enrol-harness, accept-edit, needs; `RANDOM_ID` gone; the 130, 3 and 4 tests at `:754,775-835`. Four rows assert a one-line text, not three lines (L1-2). The recorded "mutation-after" runs are not in the landing message (L1-1) |
| D2 the other PLAN-30 tests | met | `MockRuntimeEngineTest` uses the credential decision path; the unready-provider test is non-vacuous (`MockRuntimeEngine.java:8874`); `PicocliLanguageTest` generated verbs with `@EnableGeneratorController` |
| D3 one-shot stderr and the cause | met-modified / partly | no `HTTP {` message left, the dead key `enrolment.harness.failed` removed, every listed site writes to stderr; several print one catalogue line, not the three-line refusal (L1-2); `plan settings` drops the cause (**L3-1**); `epic decompose` renders a different text for the same 404 (L1-3); a stdout-then-fail site remains (L1-4) |
| D4 the refusal parser | met | `OperatorRefusedException.java:256-300,368-380`: the 3-argument constructor is gone, `getMessage()` is `message(null)`, `message(locale)` and `screenText` exist; tests at `OperatorRefusedExceptionTest.java:75-190` are non-vacuous and generated |
| D5 the literal guard and the German texts | met on the named Done-whens, guard blind in places | six blind-spot tests assert exact violation lists (`LiteralTextGuardTest.java:234-320`); the locale-less `MessageCatalogue.get(` and `format(` greps are empty; the singular "At least 1 day." and `plan report` / `reports` in German are pinned. English literals remain in the `plan <id>` tree (L2-1) |
| D6 the design lines and the captures | met-modified | three design lines aligned (`03-dialogues.adoc:269`, `19-actions.adoc:190`, `work-actions.adoc:222`); `ApiErrorCode.java:155` loses "with an audit id"; eight `.after.txt` files and `PLAN-174.txt` committed; nothing compares them (L2-6) |

- **Added unplanned:** about 55 files of 90 are outside the Expected Surface (the plan said about 35): 22 `pm-operator` main files (`AcceptEditCommand`, `ActionSupport`, `ChatDialogue`, `CleanupCommand`, `CredentialsCommand`, `DecomposeCommand`, `EntryFlow`, `FindCommand`, `HoldDialogue`, `LessonSourceCommand`, `OverviewFullScreenApp`, `PmConsole`, `ProjectSelector`, `RecipeCommand`, `RolesCommand`, `RuntimeActDialogue`, `RuntimeCommand`, `StartDialogue`, `StartPrompt`, `StartRenderer`, `WebCommand`, `WorkActionDialogue`; some fixed real swallowed errors), 25 `pm-operator` tests, `control.*` and `projects.*` message packs (en, de), `EnrolCatalogueTest`, `OperatorRolesCatalogueTest`, `MockRuntimeEngineTest`, `OnboardingScenarioTest`, and eight capture files. The landing message does not name the growth (L1-7).
- **Declared but untouched:** `RefusalRenderer.java`, `EpicCommandTest`, `PmOperatorTest`, `ScreenCaptureIT.java`, `screen-captures/PLAN-168.txt`.

## Three-Layer Analysis

Verdict: **no blocker, 1 major, 14 minor, 4 info.** "6 of 6" is generous: D3, D5 and D6 meet their literal Done-whens with real residue. Re-read: L3-1 and L2-1 by the orchestrator.

### Layer 1 — Specified

| id | severity | file:line | finding |
|----|----------|-----------|---------|
| L1-1 | minor | (landing message, PR body) | the "mutation-after" clauses of D1, D2 and D4 have no failing run recorded anywhere readable; the PR body says "each mutation-checked" and that intermediate commits "over-claimed" |
| L1-2 | minor | `PlanCommand.java:1702-1760`, `AnswerCommand.java:167-172`, `ReportsCommand.java:279-285,335-340`, `ProjectCommand.java:115-122,344-386,744`, `ClientCommand.java:123-142`, `ConfigCommand.java:~778` | D3 asks "the three-line refusal" for each site; these print one catalogue line (the `ExitCodesTest` rows encode the deviation); a defensible reading, but not the spec's text |
| L1-3 | minor | `PlanCommand.java:173`, `DecomposeCommand.java:~320,274-279` | one classification of a codeless 404, two renderings (`epic decompose` prints "Epic X not found." on one line); `19-actions.adoc`'s "read that refusal the same way" overstates |
| L1-4 | minor | `ConfigCleanupCommand.java:436-438`, `ConfigCommand.java:735-750`, `AcceptEditCommand.java:330`, `EnrolHarnessCommand.java:122`, `RolesCommand.java:359` | stdout-then-exit sites remain although `03-dialogues.adoc:269` says "every refusal ... stderr"; `config set` and `plan settings` now disagree on the same conflict |
| L1-5 | info | (landing message), `19-actions.adoc:190` | the status of "launchable only when the scope has room" is absent from the landing message (the doc has it as confirmed by derivation from `08-epic-coordination.adoc#server-started-plans`, the client's inference); the 404 rule is cited from `identifiers/01-…` rule 6 (model-supplied identifiers) where the operator API's fact is `runtime-model/01-processes-and-transport.adoc:236` |
| L1-6 | minor | PR body against the diff | the PR says the `plan.activity.kind.egress` key was added in both `plan_view` packs; the squash diff has no `plan_view.*` change (both keys existed at `3ae8a305`) |
| L1-7 | minor | the diff | 90 files, +5800/-1086 against "about 35, stop above 100"; the growth is not named in the landing message |

### Layer 2 — Implemented

| id | severity | file:line | finding |
|----|----------|-----------|---------|
| L2-1 | minor, re-read | `PlanCommand.java:584/587,614,617,620,650,672` | English still printed in the German `plan <id>` tree (`kindPrefix` returns "phase " and "sub-workflow ", "tasks: %d of %d done", "waits: ", "off-track: ", "waits slot", "run N"); the guard's blind spot is `list.add("…")` then `String.join` then `printf`, and a helper's return value; trigger `pm-operator plan <id>` with `LANG=de_DE` on a plan with tasks |
| L2-2 | minor | `LiteralTextGuardTest.java:~100-150` | the exemptions are wide (registered verb names, the last segment of every catalogue key, `ApiErrorCode` wire values, any literal compared via `equals`); a word equal to any key's last segment is invisible; five more edge shapes CodeRabbit named stay uncovered (lane-declared) |
| L2-3 | judgement | `LiteralTextGuardTest` | the two lost assertions (the per-helper "guarded source not found" and the old `isEmpty` assert) are **acceptable**: the guard now scans every non-`package-info` file below `src/main/java/.../operator` recursively, a superset; a renamed helper can no longer drop out silently |
| L2-4 | minor | `ExitCodesTest` | nine verbs (status, web, drain, credentials, project, client, recipe, lesson, roles) have a "success" row with no arguments, which a parent-help run satisfies; all refusal rows share one canned `500 {"error":"internal_fault"}`; the 130 test relies on a 20 s loop |
| L2-5 | minor | tests | no German behaviour test for `ProjectsCommand` ("Alle Projekte", "nach Name", `bereit`), the `plan <id>` tree keys, `operator.plan.running_title`, `operator.plan.tree.pull_request`, `report.tokens.unmeasured`, `report.list.project`; only the static guard covers them |
| L2-6 | minor | `ScreenCaptureIT.java:~103-107` | skipped unless the capture-set property is given; the committed `.after.txt` files are compared by no test; D6's "`ScreenCaptureIT` runs them" is manual only |
| L2-7 | minor | `PlanCommandTest.java:1192,1235-1240`, `ControlCommandTest.java:420-429` | the "does not fit" tests pin `422 invalid_value`, a contract that does not exist (L3-1) |

Clean checks (re-read): no Mockito or Hamcrest added; no `System.out`/`err` added; no generic `catch`/`throw` in production; every changed class using `Generators` carries `@EnableGeneratorController`; no new package, so no `package-info` gap; no new logger or suppression; en/de parity holds for every key added in `control`, `projects` and `enrolment`, and in `operator` apart from the moved `operator.dialogue.left_raise_scope` (now past tense `verlassen`); German texts are catalogue-keyed; key-removal hygiene tests exist (`EnrolCatalogueTest.java:129-130`, `OperatorCatalogueHygieneTest.java:55-58`); no pom or package file.

### Layer 3 — Correct

| id | severity | file:line | trigger and wrong result |
|----|----------|-----------|--------------------------|
| **L3-1** | **major**, re-read | `PlanCommand.java:1698` (`valueRefused = response.status() == 422 && INVALID_VALUE.equals(refusal.rawCode())`), `:1681-1700,1702-1760` | `plan settings <plan> --set aspect=value` against a conforming runtime answers 400 `invalid_arguments` or 409 `team_owned`, `base_branch_fixed` or `plan_ended` (the lane also names `axis_bound`, which the Runtime Model specification does not list): the "does not fit" arm never fires and the coded refusal is dropped; the operator sees only the neutral "X = V is refused; nothing changed." with exit 1 although the runtime named a cause the catalogue could render. No 422 and no `invalid_value` in `runtime-model/01-processes-and-transport.adoc:233-241` or `ApiErrorCode`, and the mock emits neither; `invalid_value` on a 400 exists only in this repository's design (`doc/design/web-app/machine-settings.adoc:214`), so the lane's "the spec puts it on a 400" is imprecise. The `--yes` retry has no test. Orchestrator note: `put` parses EVERY 409 as a `PlanSettingsUpdateResult` (`PlanCommand.java:1693`), so what a coded 409 body (`team_owned`, `base_branch_fixed`) yields there was not run; the claim that it ends on the neutral line is the lane's and the leaf's reading |
| L3-2 | minor | `ReportsCommand.java:~279-295` | when every id is refused, `reports read` prints the success glyph with "0 reports marked read." on stdout, the refusals on stderr, exit 1 |
| L3-3 | minor | `operator.reports.not_found`, `AnswerCommand`, `ProjectCommand` | every 404 reads "not found" (a route miss, `project_not_enrolled`): lane-declared for reports and answer, the same pattern in `project <id>` |
| L3-4 | minor, undeclared behaviour change | `ConfigCommand.java:~565-572`, `CredentialsCommand.java:~125-140,395,442` | Ctrl+D at the `config` "Open:" prompt now exits 1 with `operator.settings.input_ended` (the design names neither key nor code); a failed credential status read (any non-200/404) now aborts `set`/`rotate`/`remove` with exit 1 where `set` continued as if the entry were absent (a hardening, unannounced) |
| L3-5 | minor | `CleanupCommand.java:1229`, `operator.en.properties`, captures | "Delete?" appears twice (the preview appends `delete_confirm`, the prompt bar starts with it; `26-cleanup.adoc:75` shows one); `delete_preview_frees` has leading spaces that `.properties` strips; at 80x24 in German the line wraps mid-word; the mock's English shows in the German captures (lane-declared) |
| L3-6 | minor | `OperatorRefusedException.java:368-380` | the trace check is frame-based; a flattened one-line trace or a Python traceback is no longer caught (the plan chose frames over words deliberately); body arguments are capped, not trace-checked |
| L3-7 | minor | `StartApi.java:~363-410` | `StartActException` prefers the server's `message` over the catalogue text, so a runtime's English shows on a German screen, prefixed by the link id |
| L3-8 | minor | `ApiErrorCode.java:155`, `01-processes-and-transport.adoc:239` | the Javadoc dropped "with an audit id" while the spec still says `internal_fault (with audit_id)` |
| L3-9 | minor | `DecomposeCommand.java:274` | `notFound(PrintWriter out, …)` is called with `err` (`:80,127,210`): a misleading parameter name |
| L3-10 | info | `epic next` | with no launchable and no scope-blocked row it sends `raise_scope:false`, prints `control.epic.next_none` from the pre-act read and exits 0; consistent with `19-actions.adoc:190` |

Engine facts the lane left open (the leaf read the specifications; no daemon was read): (a) the 404 body shape is NOT specified (`runtime-model/01-processes-and-transport.adoc:233` says "a status and one code of the closed list"; `19-actions.adoc:44` labels it the client's reading); (b) `project_id` and `part` are not in the engine's refusal body (it states a scope with its `project_id` in a state, `hypermedia-format/…:278`); (c) an empty-body 404 as a missing route is consistent with, not stated by, the specification; (d) `invalid_value` is a defect (L3-1); (e) `reports read` exiting 1 on a refused read is consistent with `24-reports.adoc:30` and `03-dialogues.adoc:269`, but no design text states the partial-success case; (f) every 404 worded "not found" is confirmed (L3-3).

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Dependencies | no dependency without asking | conforms | no pom or package file in the diff |
| CLAUDE.md § Code Standards | no Mockito/Hamcrest, no `System.out`, no generic `Exception`, `@EnableGeneratorController` | conforms | leaf, clean checks above |
| spec | the recorded mutation runs | not evidenced | L1-1 |
| spec | about 35 files, stop above 100 | deviates (inside the bound) | 90 files; the growth is not named (L1-7) |
| CLAUDE.md | zero Sonar findings | **not decidable** | `gate_status` OK on the PR (new coverage 93.2, duplication 0.2, hotspots reviewed 100) and on `main` (analysis `AaIHi4HebfAANPRQy9_T` at 08:35Z, coverage 90.1); the lane states `sonar_open_new_issues=0`; `sonar_rest search --pr 533` lists **14 open** code smells in 8 touched files (5 CRITICAL `S1192`/`S3776`, 6 MAJOR, 3 MINOR, all at line 0) while four of them name a duplicated literal that occurs once in the merged sources (`"phase"` ×4 at `3ae8a305`, ×1 at `fcf87728`; `"reason"` and `"count"` once each), so the PR list looks like the analysis of an earlier head (the rebase of the finalize run re-fired the Sonar step). Recorded `unknown`, never `0`; the standing pm-operator debt is the `pm-operator` Sonar plan staged after this landing |
| lane rules | forbidden flags | not read | the lane ran on the `plan-marshall` lane with its own guard |

## Metrics and Anomalies

- `total_tokens=87005282`, `total_wall_seconds=222272` (about 62 h), harness `claude`, model `claude-opus-5-5`, effort `low`; `plugin_version` and `executor_version` `0.1.2173`. The landing message carries every key (`landing-check` reports complete).
- Review: `review_body_findings_total=3`, `handled=3`, `unresolved_review_threads=0`; `code-review` blocking 0, advisory 4; `functional-review` 6 of 6 met, 0 residuals declared. The operator granted extra review rounds three times (functional-review twice, code-review twice) and decided one rebase.
- A rebase onto a moved `main` hit a content conflict in `ReportsCommandTest` that the merge-tree probes had not predicted; the pre-merge gate then refused `merge_candidate_not_gated` until `ci-verify`, `sonar-roundtrip` and `automatic-review` were re-fired at the rebased head; a simplify edit was committed during a running whole-tree verify. The eight candidate lessons (001 to 008) say so; their routing is below.
- The CI Java 27 job failed once on a generated "unknown name" input that equalled a served name for about 1 seed in 100 (candidate lesson 002); fixed in the run.
- `archive-plan` was `pending` at the landing (the lane's step list), `cleanup_owed=false`.

## Routing and Merge Behavior

- Merge `fcf87728` by the merge queue. Overlap with the running plans: one file by path, `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java` (PLAN-181 declares it; PLAN-180 and 181 had already noted it as the only #533 overlap). PLAN-144's declared pm-api files (`common`, `settings`, `system` properties) were not touched; no `pm-web-app`, `pm-relay`, `pm-exec` or `pm-web-e2e` file changed.
- **Declared surfaces of other plans this footprint touched** (their claims are re-read at `fcf87728` before any emit; some of the named plans have shipped or are parked and were not checked here): `OnboardingScenarioTest` PLAN-175, PLAN-182 (staged); `MockRuntimeEngineTest` PLAN-158; `ApiErrorCode.java` PLAN-132, 86, 45; `control.*` properties PLAN-106, 109, 132, 136 (staged), 156, 58, 73; `projects.*` properties PLAN-158, 176 (staged), 59, 90; `OperatorRolesCatalogueTest` PLAN-148's hypothesis only. The CLI chain PLAN-42, 44, 48, 50 and 63 shared 7 to 11 files with #533 (known): they are re-read at emit.
- Sonar: see Conformance. Merge-queue run green.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 533; `landing` `landings/PLAN-174.md`; `plan_marshall_plan_id` `plan-174-cli-refusal-residue-second`
- [x] epic.md: Open Defect for this landing (one layer-3 major, L3-1), the residue named
- [x] the eight candidate lessons: 001 to 007 promoted (filed in the owning plan-marshall store as `2026-11-23-10-001` to `-007`), 008 recorded in epic.md as a lane-budget watch for the operator
- [x] inbox messages 001 to 009 archived under `inbox/archive/plan-174-cli-refusal-residue-second/`
- [x] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Layer-3 major L3-1 is reported to the operator at once and not fixed inline;** a fix plan is staged only on the operator's word. L3-4 (two undeclared behaviour changes) is a design question for the same decision.
- The minors L1-2 to L1-4, L2-1, L2-4 to L2-6 and L3-2, L3-3, L3-5, L3-7 are the residue of the CLI refusal work; they are candidates for the next CLI plan on `PlanCommand` and `ConfigCommand` (PLAN-42 and the chain), named as D0 clauses at that plan's emit, never as a plan of their own unless the operator says so.
- Engine facts (a) to (c) are the client's reading: confirm against `pm-mcpd` in `pm-e2e` (another repository); (d) is the catalogue's and the Runtime Model specification's to settle (the code for a value the runtime refuses); both are recorded in `epic.md`.
- The `pm-operator` Sonar part (standing debt: the pm-operator share of the standing smells) is now unblocked.
