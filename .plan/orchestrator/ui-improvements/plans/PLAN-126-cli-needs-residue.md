# PLAN-126: Terminal: "Needs You" Made Correct (PLAN-36 Residue)

epic: ui-improvements
workstream: WS-05

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision
> of 2026-11-10 (residues get fixed) from `landings/PLAN-36.md` and PLAN-33's finding 2b. This spec is SELF-SUFFICIENT:
> the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-36 (#391, `36fb27d9`) gave the terminal the `needs` verb, the overview's "Needs you" group with `!`, and the badge words
for `credential` and `cleanup_due`. No bot reviewed any head of it, and four findings are major:

- **Notices.** `needs_operator.changed` now raises a console notice for any kind, a regression on every console.
- **The command.** `plan hold <id>` is printed and run for `landing_failed`, a verb that does not exist.
- **The selection.** The "Needs you" rows are keyed by index, so an item arriving above the selection moves it.
- **The count test.** The one-count test compares totals only, and the mock was given a second derivation to make them meet.

PLAN-33 also found that a REFUSED projects read renders as "Nothing runs…" (finding 2b). Make the needs surfaces do what
`doc/design/shared/needs-and-find.adoc` and `doc/design/terminal-client/04-console.adoc` say, with tests that can fail.

## Deliverables

1. **A notice only for what a change adds**
 - What:
   - `ConsoleNotice.noticeKindOf` (`ConsoleNotice.java:388-393`) turns every `needs_operator.changed` whose `kind` is not `none`
     into a notice of that kind. That yields `Q-???` question notices, duplicates of `hold.opened`, and a bell on every count
     change including a decrease. `04-console.adoc:34` says a notice is raised for each item a change ADDS, and the table
     `:68-100` gives every kind its notice. `EventKind.java:28-30` says the event is "the badge's source and not a notice".
     Make the two agree (OPEN DECISION 1, operator) and implement the chosen rule. The recommended rule: a notice per ADDED
     item of the kinds no dedicated event announces (`credential`, `cleanup_due`, and `setup` / `findings` where
     verify-at-outline shows no other event), found by comparing the machine summary
     (`NeedsOperatorChangedEvent.summary`) with the previous one; never for a decrease; never a second notice for an item
     `decision.opened`, `hold.opened` or `condition.raised` already announced.
   - The credential notice (`ConsoleNotice.java:193-202`) reads the key from the item, through the sanitizer, with a
     catalogue "unknown" word. Today it is the English literal `"unknown"`, and a generic `key` member is read as scope.
   - `EventKind.java:89,92` invent the wire names `credential.needed` and `cleanup.due`. Remove them, or take the names
     from the event catalogue of the specification (`06-event-catalogue.adoc`).
   - `ConsoleBadge.command()` (`ConsoleBadge.java:98-117`) has no production caller and answers `onboarding` for some kinds.
     Give it its caller (the prompt badge's command) or delete it with its coverage test.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:java-security, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests: a decrease raises no notice and no bell; a plan-level `kind: decision` change after `decision.opened` yields
     exactly one notice; a machine summary that adds a credential yields one notice naming the sanitized key; each fails on a
     revert. Evidence order: `test-first`.
2. **Each item opens what the design says, and the selection follows the item**
 - What:
   - `OverviewFormatting.landingFailedCommand` (`:498-501`) returns `plan hold <planId>`; `PlanCommand` has no `hold`
     subcommand, and `04-console.adoc:76-80` gives `epic <id>` for `held` and `landing_failed`. One derivation
     (`formatNeedsItemCommand`) serves `needs`, the overview rows and `!` (`OverviewFullScreenApp` `openNeedsItemAct`
     path, `runVerb` near `:1347`). Every command it can print must be a registered verb (test over all kinds).
   - Rows are keyed `needs:<index>` (`ProjectsOverviewLevel.java:40,139,189`): key by the item's identity (its link, else
     kind plus scope) so the selection and `!` follow the item when one arrives above it.
   - Titles instead of raw ids as the subject (`OverviewFormatting.java:399-416`); the `blocks` wire token through catalogue
     words (`common.kind.*`; `:425-436`); and the English fallbacks that PLAN-123 did not remove (`machine`, `cleanup`,
     `credential`, `Needs you · `, `! `, the dead `hasKey` fallbacks at `:427-435,454-457`,
     `ProjectsOverviewLevel.java:168-170`). Re-read after PLAN-123 and do only the remainder.
     PLAN-123 (`1bdfede6`) left one: `ProjectsOverviewLevel.java:177` still calls the English-only overload
     `OverviewFormatting.formatNeedsItemSubject(item)` (`OverviewFormatting.java:343-345`), so a German overview shows
     `cleanup`/`credential`/`machine`; delete the overload (no compatibility shim) and pass the locale (folded 2026-11-11,
     operator; `landings/PLAN-123.md`).
   - `NeedsOperatorKind.fromMap` (`NeedsOperatorKind.java:74-82`) throws on a kind it does not know, so one new runtime
     kind fails the whole list. An unknown kind renders with a catalogue word for "another reason" and the list continues.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test over every `NeedsOperatorKind` shows each printed command resolves to a registered verb and that `landing_failed`
     with a plan prints `epic <id>`. Evidence order: `test-first`.
   - A test inserts an item above the selected row and shows `!` still opens the selected item; fails on a revert to the
     index key. Evidence order: `test-first`.
   - Screen capture (set `PLAN-126.txt`): the overview with the "Needs you" group, `en` and `de`, at 120×30 and
     80×24, shows no raw id, no wire token, no English word in `de`. Evidence order: `none`.
3. **The list is complete and a failed read says so**
 - What:
   - `NeedsCommand` prints page 1 only and ignores `has_older`; the design says never a silent cut
     (`needs-and-find.adoc`). Add paging (`Show more`) as the design defines it.
   - `NeedsCommand.fetchDecisionIds` (`:121-127`) joins `/api/v1/decisions` itself (last one wins); the design says no client
     joins a store of its own. Take the decision id from the item's `link`, as the overview already does
     (`OverviewFormatting.java:492-494`).
   - PLAN-33 finding 2b (CLI-UC-F-15): `OverviewFullScreenApp.fetchProjects` (`:486-495`) swallows an `IOException` and ignores a
     non-200, so a refused read renders `overview.projects.empty` ("Nothing runs…", `ProjectsOverviewLevel.rows:86-89`).
     `fetchNeedsOperator` (`:475-484`) keeps a stale group and shows no refusal; `fetchExplorer` (`:497-509`) does the same.
     Give each full-screen level an error state: a failed or refused read shows the refusal text (the renderer of PLAN-30
     if it has landed, else `ErrorTexts`) and never the empty-state text.
   - A plan with a hold and a decision reaches the decision (today the refused hold read hides it).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - On `reports-and-system` with `/api/v1/projects` refused, the overview shows the refusal and not "Nothing runs"; fails on
     a revert. Evidence order: `test-first`.
   - `needs` against a page with `has_older` prints the `Show more` form. Evidence order: `test-first`.
4. **One count, and tests that can fail**
 - What:
   - `NeedsOneCountTest.sameCountsAcrossFourSurfaces` (`:56`) asserts the total 12 only. Compare per kind across `needs`, the
     overview group, `status.needs_operator`, `questions` and the badge.
   - Remove the mock's second derivation (`MockRuntimeEngine.java:5718-5738`, `mergeMachineNeeds`) so counts derive from the
     one list, as its own Javadoc says.
   - Weak tests: `NeedsCommandTest.needsEmpty` (`:165`, ends in `|| !output.isBlank()`); bare lowercase substrings that also
     match the command column; the "title fallback" test that runs through `fr` to `en`; `NeedsIT` starts at a project level,
     not the overview root, and accepts staying on the row; the `openNeedsItemAct` path is untested; the `TuiLayoutTest` OR and
     the loosened `TuiCapabilityIT:242` accept `? more`.
   - `? help` is pushed out of the 80-column footer (`TuiKeyRegistry.java:318-356`): keep it.
   - `@ParameterizedTest` for the 3+ variant tests (`consoleBadgeCommandExhaustive`, `needsItemFormattingExhaustive`, the
     notices). Input data from `Generators`.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - The count test fails when the mock's kind composition changes and the total does not. Evidence order: `mutation-after`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed. Evidence order: `none`.
5. **What `find` left open (from the landing of PLAN-84, folded 2026-11-10 on the operator's decision)**
 - What: read `landings/PLAN-84.md` of the epic; lines are as read at `1b938f37`. First check whether PR #405
   (`fix/84-find-kind-words`: the German kind word, Esc's double line end, the project-open wording, the weak choice test, the
   catalogue test's key set) has merged; do only what it leaves.
   - **The shell path opens the view on the terminal it holds.** `FindCommand.choose()` (`:310-322`) opens a lease from
     `context.openTerminal()` but `open()` (`:371-372`) runs `PmOperator.execute` without `context.useTerminal(lease.terminal())`, so
     `pm-operator find x` then a number gives the nested `plan`/`epic`/`answer` view a second JLine terminal, which
     `OperatorContext.java:236-250` says falls back to a dumb one. Lend the lease for the nested verb, and add a PTY test (`FindIT`) of
     the shell path: `find relay`, then a number, then the view's header.
   - **Ctrl+C in the console's `/` field** is read as char 3 (`FindCommand.java:88,440`) after `terminal.enterRawMode()` (`:385`),
     which keeps ISIG: on a real tty the signal may kill the console instead of leaving the field. Leave the field on Ctrl+C on a real
     tty (a PTY test that types Ctrl+C in the field and finds the prompt again). `SecretReader.java:56-66` has the same pattern:
     name it in the landing, do not change it here.
   - **The match title through `Display`** (`FindCommand.java:196`): a blank or control-only title renders as the Display rule says,
     never as an empty cell or a bare id (as the location titles at `:255-264` already do); a test pins it.
   - **The typed text in the header and in "nothing matches"** (`:180-187`), not the runtime's echo (`FindPage.java:99` makes a
     missing echo `""`).
   - **Backspace removes one code point** (`:456-463`), so a supplementary character leaves no lone surrogate in `q=`.
   - **Exit codes** as `doc/design/terminal-client/01-commands.adoc` § Exit Codes says: leaving the choice with `q` is 3; an empty
     text is a refusal, not 0. Where the design and the code differ beyond these two, report it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - The shell-path PTY test fails when the lease is not lent (evidence order: `mutation-after`).
   - The Ctrl+C PTY test, the title test, the header test and the backspace test fail before the change (evidence order: `test-first`).
   - `FindCommandTest` asserts the exit code of `q` and of an empty text (evidence order: `test-first`).

Not split: five deliverables; deliverable 5 is the find residue the operator folded here, because this plan already waits for
PLAN-84 and touches `PmConsole`/`PmOperator`. Rationale recorded by the orchestrator.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`
  and `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Pintegration-tests` pass at the head, and so does
  `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Ppre-commit`, with every rewrite committed.
- No new dependency or plugin. Input test data comes from `Generators` with `@EnableGeneratorController`; expected catalogue
  text stays literal. Every new text is in en and de.
- Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public; no compatibility shims.
- Terminal check through the screen capture harness (`ScreenCaptureIT`; command line in `doc/design/terminal-client/27-tui-testing.adoc`),
  saved as `pm-operator/src/test/resources/screen-captures/PLAN-126.txt`. Never run `pm-operator` in the agent's own shell.
- Ports of this plan: per `AGENTS.md` § Ports formula (`20000 + 10 × 126`): mock web port 21260, app port 21261. Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule, stated hard because PLAN-36 broke it:**
  - The pull request is merged only after at least one CodeRabbit review of some head with its findings handled. A lane's
    "standing review authorization" does not replace it. If CodeRabbit refuses on quota, stop and ask the operator.
  - The merge question is asked only when every review thread has a reply and is resolved, and every review body is answered.
  - The pull request body states the thread count, the resolved count and the review-body items with their count, read
    through `ci pr comments`, never from memory.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
    The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED, read at `1cc0e10e` on 2026-11-10:
  - `ConsoleNotice.java:388-393` returns `EventKind.fromType(kind)` for any `needs_operator.changed` whose kind is not `none`.
  - `EventKind.java:28-30` calls the event the badge's source and not a notice; `04-console.adoc:34` says a notice per ADDED item.
  - `OverviewFormatting.java:498-501` returns `"plan hold " + planId`; `PlanCommand` registers no `hold` subcommand.
  - `ProjectsOverviewLevel.java:189` keys rows `needs:<index>`; `needsItemOf` (`:134-147`) parses the index.
  - `OverviewFullScreenApp.java:486-495` ignores an `IOException` and a non-200 of `/api/v1/projects`.
  - `NeedsOneCountTest:56` asserts the total 12; `MockRuntimeEngine.java:5722` `mergeMachineNeeds` adds machine items to the kind counts.
  - `NeedsOperatorKind.java:74-82` throws on an unknown kind.
- OBSERVED, from `landings/PLAN-36.md` and not re-read: PR #391 had 0 threads and 0 review bodies; CodeRabbit refused 7 triggers on quota.
- HYPOTHESIS: the machine summary event carries enough to find the ADDED item (the `needs_operator` member with per-kind items).
  Confirm or refute at `pm-api` § `NeedsOperatorChangedEvent` / `NeedsOperatorSummary` and the event catalogue (verify-at-outline).
- HYPOTHESIS: no other event announces `setup` and `findings` items. Confirm or refute at `EventKind` § `events` lists and the
  event catalogue (verify-at-outline). It decides which kinds the notice rule covers.
- HYPOTHESIS: at 80×24 the `!` footer cell pushes `? help` out. Confirm or refute at `TuiKeyRegistry` § `keepCells` /
  `moreCell` and `TuiLayoutTest` (verify-at-outline).
- Verify-first clause: re-read each named line at the current HEAD before editing. PLAN-123 (running when this was staged)
  changes `NeedsCommand`, `OverviewFormatting.formatNeedsItem*`, `ConsoleNotice` and `OverviewFullScreenApp`; do only what it leaves.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNotice.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleBadge.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/NeedsCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/ProjectsOverviewLevel.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiKeyRegistry.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventKind.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorKind.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsCommandTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsOneCountTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConsoleNoticeTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConsoleBadgeTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/ProjectsOverviewLevelTest.java` — HYPOTHESIS (named by the landing); sweep at outline
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/NeedsIT.java` — HYPOTHESIS (named by the landing); sweep at outline
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TuiCapabilityIT.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiLayoutTest.java` — HYPOTHESIS (named by the landing); sweep at outline
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-126.txt` — absent; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/FindCommand.java` — present at `1b938f37` (deliverable 5)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at `1b938f37` (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/FindCommandTest.java` — present at `1b938f37` (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/FindIT.java` — present at `1b938f37` (deliverable 5)
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at `1b938f37` (deliverable 5)

## Dependencies and Sequencing

- Depends on: `PLAN-36` (shipped, #391), `PLAN-123` (running, PR #404 open: `ConsoleNotice`, `NeedsCommand`, `OverviewFormatting`,
  `OverviewFullScreenApp`), `PLAN-84` (shipped, #397; its follow-up PR #405 open: `FindCommand`, `PmConsole`, the catalogue)
- Overlaps with: `PLAN-44` (`ConsoleNotice`, `TuiKeyRegistry`, `OverviewFormatting`: starts after this plan), `PLAN-42` and
  `PLAN-48` (`NeedsCommand`, `OverviewFullScreenApp`: start after), `PLAN-50` (`ProjectsOverviewLevel`, the needs rows' columns: starts
  after, and owns the column grid), `PLAN-30` (the refusal renderer feeds deliverable 3's error state: if PLAN-30 has landed, use it),
  `PLAN-79` (rewrites the test classes named here, last)
- Adjacent to: `PLAN-33` (verifies the screens; records the commit it verified, does not wait)

## Hand-Off Command

Lane: `runbook`.

/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-126-cli-needs-residue.md

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session
reports its outcome through its PR and its inbox message. The qualifiers of the inbox exception, and the sole sanctioned write
mechanism, are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Notice rule (decision 1):** `needs_operator.changed` raises a console notice only for each ADDED item of a kind no other event announces (`credential`, `cleanup_due`; `setup` and `findings` only if no other event announces them), found by diffing the machine summary; nothing on a decrease, never a duplicate of `decision.opened` or `hold.opened`. `04-console.adoc:34` stands; the Javadoc of `EventKind.java:28-30` is corrected to it.
