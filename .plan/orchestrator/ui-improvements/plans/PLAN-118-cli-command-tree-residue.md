# PLAN-118: CLI: What the Command Tree Plan Left Open

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane (operator rule 2026-11-07).
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.
> Staged 2026-11-07 from the landing analysis of PLAN-97 (`landings/PLAN-97.md`: PR #358 merged with 0 unresolved threads and
> 0 Sonar issues, but `drain` can signal an unrelated process, twelve review body items are unanswered, the `help` verb list
> has no test). One pull request on purpose. Keep it under 40 files.

## Objective

PLAN-97 gave the operator one command tree, the drain signal and accept-edit by project. It left the drain signalling a
process it cannot identify, the machine-derived bound untested, one verb list in three places, the replies owed to the
reviewer, and decisions in the PR body that the design does not hold. This plan closes them so that `drain` never signals a
process it cannot prove is the recorded one, and every item has an answer where the reviewer reads it.

## Deliverables

1. **`drain` signals only a process it can identify**
   - What: `RuntimeRecord.processAlive` (`pm-api/src/main/java/de/planmarshall/api/runtime/RuntimeRecord.java` ~66) returns
     true when the record has no start instant or when the process info has none (`.orElse(true)`); `DrainCommand` then
     `destroy()`s the handle (SIGTERM), so a reused pid is signalled (CodeRabbit Major on `DrainCommand` ~84-85, unhandled).
     Fail closed: a process is signalled only when the record carries a start instant AND the process reports an equal one
     (to the second); otherwise `drain` reports the runtime as not identifiable and signals nothing (a text in the en and de
     packs, an exit code the design names). The PR body claim "signals only a live recorded process whose start time matches"
     becomes true. The drain bound (`boundOfMachine`, `drainTimeout`: `machine-config.json`, default 600 + 5 s + 10 s) is
     tested on the production path: absent config, a non-default `drain_timeout_seconds`, zero and an invalid value.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: a record without a start instant, a process without start info, and a mismatching start
       instant signal nothing; a matching one signals (evidence order: `test-first`)
     - The bound tests fail when `boundOfMachine` ignores the configured value, the +5 s or the +10 s (evidence order:
       `mutation-after`), the reverted code in the pull request body
2. **One source for the verb list, and a test of `help`**
   - What: the verbs of the command tree stand in three hand-copied places: the `help.more` text
     (`operator.{en,de}.properties` ~112), the `PmConsole` completer and `PmOperatorTest`'s `DESIGN_VERBS` (CodeRabbit "derive
     from the design / command model", unhandled). Derive the completer and the help text from the registered command model;
     the test compares the registry with the design's verb list read from `doc/design/terminal-client/01-commands.adoc`, not
     with a second hand copy. `PmConsoleTest` ~144 asserts the verbs of `help` (the spec of PLAN-97 asked for it); the
     `--help` test of `PmOperatorTest` asserts a verb that exists, not `contains("project")`. `error.enrolment_tampered.fix`
     prints `<project_id>` literally: it takes the id as a parameter.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Adding a verb to the registry without the design fails a test; a verb missing from `help` fails a test (evidence
       order: `mutation-after`)
     - `git grep -n "DESIGN_VERBS" -- pm-operator/src/test` names no hand-copied list (evidence order: `none`)
3. **Every item answered where the reviewer reads it**
   - What: (a) the twelve body items of CodeRabbit review `5810938147` of PR 358 (1 Major `DrainCommand`, 3 Minor, 8
     nitpicks), done or declined with a reason, one reply each in a comment of PR 358 naming the commit (or the plan item that
     answers it). (b) The three replies owed by PLAN-97 D4(i) on the PLAN-95 threads (`60EE-P`, `60EE9o`, `60EE-Z` of PR
     327), with the follow-up issues they name (open them with the operator's word, or record the item in the pull request).
     (c) D4(f): `serveNeedsOperatorPage` (`MockRuntimeEngine` ~5946): the code and Javadoc state why it is unchanged, or it
     changes as the PLAN-97 spec asked.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core
   - Done when:
     - The pull request lists every item it answers with the commit or the reason, and the replies exist on GitHub (list
       them through the REST/GraphQL reads for PRs 358 and 327), none of its own unresolved (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
       (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)
4. **Decisions of the verb folding in the design, and the owner questions**
   - What: write into `doc/design/terminal-client/01-commands.adoc` and `17-projects.adoc` what PLAN-97 decided only in its
     pull request: (a) `project add` today is the old `enrol` (a direct enrol request); the onboarding dialogue left the shell
     with `onboard`; the design line says `project add` onboards: state the target and record the gap as an open item with
     its owner, or restore the dialogue (the operator decides: put the question in the pull request, do not guess); (b)
     `project release` and `project unenrol` need `--yes` and have no interactive confirmation or reason prompt: state it
     or add the prompt per the design's confirmation rule; (c) Q1: `doc/specification/…/02-operator-channel.adoc:42` of the
     specification repository still names `enrol` (companion pull request not opened: record it as an open item with its
     owner `plan-marshall/plan-marshall-documentation`); (d) Q3: the sub-verbs `plan findings|watches|resolve|settings`,
     `epic review|instruct`, `runtime … --drain`: a decision per verb; (e) `onboard --auto` and `--step` were removed with no
     design line: add it. The design's `profile`, `decision-log`, `waive`, `grant-access` and `sweep` have no registered
     verb: list them as open items.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The documents pass `pm-documents:ref-documentation` docs review and `pm-documents:ref-asciidoc` `validate` and
       `verify-links`; every open item names its owner (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-api,pm-operator verify -Ppre-commit` and
  `./mvnw -pl pm-api,pm-operator verify -Pintegration-tests` pass AT THE FINAL HEAD (state the head the gate ran at);
  run the gate once on the final head and commit what it rewrites.
- No new dependency or plugin. Code standards (CLAUDE.md): no generic `Exception`/`RuntimeException` catch or throw in
  production code, `@NullMarked`, JUnit 5 only, `Generators` with `@EnableGeneratorController` (`nonBlankStrings`, never
  `nonEmptyStrings`), no arbitrary timer wait.
- Ports of this plan: `21180` and `21181` where a deliverable starts a process (`AGENTS.md` § Ports: `20000 + 10 × 118`);
  never stop a process this worktree did not start.
- Every `test-first` clause cites a FAILING and a PASSING run with the assertion quoted in the pull request body.
- ZERO Sonar findings (the operator, 2026-11-05): `sonar: configured` (project `plan-marshall_pm-mcp-clients`); 0 open
  new-code issues at the merge head, fixed in a commit of this pull request; state `sonar_open_new_issues=N` by that name.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled, the
  findings of EVERY review handled, the review BODIES (nitpicks included) answered; reply IN the thread; state
  `unresolved_review_threads=N` and `review_body_findings_total/handled` counting every actionable item. The pull request is
  merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request
  body carries no generator footer.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.

## Claim Labels

- OBSERVED: `RuntimeRecord.processAlive` returns true when `startInstant == null` or the process has no start info
  (`.orElse(true)`), and `DrainCommand.call()` signals the filtered handle — read by the orchestrator at `origin/main`
  `d8efe35f` on 2026-11-07
- OBSERVED: PR 358 has 24 threads, 0 unresolved, and one review `5810938147` whose body holds items beyond the threads
  (count 12 per a read-only agent: `DrainCommand:84-85`, `DESIGN_VERBS`, the completer, the bound test); the Sonar gate passed
  with 0 new issues — read through GraphQL and REST on 2026-11-07
- HYPOTHESIS: `PmConsoleTest:144` asserts only `"Commands:"`, `DrainCommandTest` never mentions `machine-config`, `project add`
  is only the old `enrol`, the real `pm-mcpd` answers the withdraw `DELETE` with 405 — by a read-only agent's reading of
  `d8efe35f`, not run; confirm/refute at the files named
- Source: `landings/PLAN-97.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every
  HYPOTHESIS; a refuted cause re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/RuntimeRecord.java` — present at HEAD d8efe35f
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/RuntimeRecordTest.java` — present at HEAD d8efe35f
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD d8efe35f
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DrainCommand.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/DrainCommandTest.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleTest.java` — present at HEAD d8efe35f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ProjectCommandTest.java` — present at HEAD d8efe35f
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD d8efe35f
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD d8efe35f
- OBSERVED: `doc/design/terminal-client/17-projects.adoc` — present at HEAD d8efe35f

## Dependencies and Sequencing

- Depends on: `PLAN-97` (shipped)
- Overlaps with: `PLAN-116` (`PmConsole.java`: one shared file does not block; whichever starts second rebases on the
  other's merge), `PLAN-112` (running, D1: `terminal-client/17-projects.adoc`, `19-actions.adoc` text; rebase on its merge
  or, if D1 is not merged, edit only `01-commands.adoc` in deliverable 4(a)-(e) and leave `17-projects.adoc` last)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode) — a small plan (operator rule 2026-11-07).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-118-cli-command-tree-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO
file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other
ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole
sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
