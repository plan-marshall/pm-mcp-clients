# PLAN-139: Terminal Chat: What the Role Knows, Status Lines, the Subject's Notices (D5 Terminal Chat, Part 2)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D5
> documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

D5 gave the terminal the same awareness as the web chat: a line under the head says what the role knows, an empty chat offers numbered
starters, older turns are read a page at a time, what has acts of its own is shown as a status line with `o) open it` (the chat carries
out no act but its own proposal), an act the role names is printed as its command and never run, the subject's narrative arrives as
`–` lines while the chat stays, and a plan's queued revision is told in the plan view. This plan builds that on the dialogue of
PLAN-138 and the mock of PLAN-134. The decision, review, decomposition and start places are PLAN-140.

## Deliverables

Specification: `doc/design/terminal-client/28-chat.adoc` (the whole part); `doc/design/terminal-client/19-actions.adoc` § `#chat-context`;
`doc/design/shared/work-actions.adoc` § `#chat-knows`, `#chat-acts`, `#chat-narrative`; `doc/design/shared/plan-view.adoc` § `#chat-revision`;
`doc/design/terminal-client/04-console.adoc` § `#line-editor`; `doc/design/terminal-client/22-texts-actions.adoc`.

1. **What the role knows, the starters, older turns, the reserved lines**
 - What:
   - Print `replan knows: the description, 17 tasks, phase execute, version 7 · as of 10:42` (`control.chat.knows`, `control.chat.knows.*`) under the head
     from the chat's `context`; reprint it above a reply when what it names changed, with its new time.
   - An empty chat prints `control.chat.empty` and the scope's starters numbered (`control.chat.starter.<scope>`); a first line that is only the number of a starter
     puts its text into the line editor, where it can be changed, and sends nothing; once the chat has a turn a number is a message.
   - A chat with turns prints its newest page (50 turns); `older` (German `älter`, `operator.actions.chat.older_command`) as the whole first line prints the page before
     above the prompt until `This is the beginning of the chat.` (`operator.actions.chat.no_older`).
   - Reserved first lines, compared as typed: `q`, `new`/`neu`, `older`/`älter`, `changes`/`änderungen`, `o`, `o <id>`; every other line, one that only begins with such a word
     included, is a message; to send a reserved word, type it with a space before it (sent without that space).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert the knows line at open and after a changed turn, the starter prefill, `older` to the beginning, and that `older now` and ` changes` are messages while
     `older` and `changes` are not; each fails on a revert (evidence order: `test-first`).
2. **Status lines and `o`**
 - What:
   - Where a reply or a notice names a decision, a hold, the plan's queued revision or an act the role names, a status line follows it with the target's mark, id and
     state in the words of its view, kept current while the chat is open, never offering the target's acts: `◆ Q-007 · open · blocks the plan   o) open it`.
   - `o` as the whole first line opens the target of the last status line printed (also one printed by `older` or `changes`), `o <id>` another, and says
     `operator.actions.chat.nothing_to_open` where none was printed; it leaves the chat, which stays as after an empty line, and opens the decision's answer dialogue,
     a hold's dialogue, the plan view (queued revision), or the view of the subject that owns the act; `h) chat` there or `plan chat` returns to the chat.
   - An act the role names is printed with its command and never run: `pause` `plan pause <plan_id>`, `start` `plan start <plan_id>`, `cancel`, `abandon`, `settings`
     `config <aspect> --plan <plan_id>` with `<aspect>` as a placeholder, `next_plan` `epic next <epic_id>`, `auto`, `decompose`, `archive`, `cancel_epic` `epic cancel <epic_id>`; the
     command carries no argument the reference does not name.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test per target kind asserts the status line, that no act of the target is offered, that `o` leaves the chat without ending it and opens the right dialogue, `o` with no
     line printed, and that no printed command carries an argument the reference lacks (evidence order: `test-first`).
3. **What happened meanwhile**
 - What:
   - While the chat is open it follows its subject's stream (the plan's, the epic's, the entry's, the scope of a decision, the workspace's for ended work, which has no notices),
     so a turn, a proposal or the end from another device arrives as in the web app.
   - A notice line starts with `–`, muted, with its time (`control.chat.line.*`), printed above the prompt and never inside a reply being printed (lines that arrive while a reply is
     printed wait until it ends); a line that names a decision or a hold is followed by its status line.
   - More than three lines of one notice turn fold into `5 changes to fix-relay · changes shows them` (`operator.actions.chat.lines_folded`); `changes` prints them. Without cursor
     control (`TERM=dumb`, a screen reader) the lines are printed between lines, at the next prompt, as every notice of the console.
   - A chat ended from another device or by the subject prints the notice of its cause (`chat.ended.ended_by`) and returns to the prompt.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-security, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests with the mock in `ordered` timing assert a notice never lands inside a printed reply, the fold at 3 and 4 lines, `changes`, the `TERM=dumb` form, and each ended cause
     (evidence order: `test-first`).
4. **The queued revision in the plan view and the overview**
 - What:
   - The plan view (`PlanViewLevel`, `PlanViewFormatting`) and the overview row say `Revision from the chat queued · applies after TASK-4` (`control.chat.revision_queued`; a paused plan
     `applies when fix-relay starts again`) with `h) chat` on that chat's subject where `chat_open` is true, or `from an earlier chat` / `from the chat of Q-007` (`control.chat.revision_origin`)
     without a way where it is not; the version line says `version 8 · from the chat` (`control.chat.from_chat`) after it is applied; the line goes when it is refused or lapses.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert each case in en and de on the plan view and the overview row, and the removal on each exit (evidence order: `test-first`).
5. **Texts and evidence**
 - What:
   - The keys of `28-chat.adoc` § `#texts` in `operator.*.properties` (en and de), the narrative lines through `control.chat.line.*` of PLAN-132; a PTY test `ChatKnowsIT` (knows line, starter, `older`,
     a status line and `o`, a folded notice, `changes`) and screen captures in `en` and `de` saved as `pm-operator/src/test/resources/screen-captures/PLAN-139.txt`.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ChatKnowsIT` passes with `-Pintegration-tests` and asserts the screen (evidence order: `test-first`).
   - The open-issue read at the pull request head is recorded (open Sonar issue count and rules left open; `unknown` for a failed read, never `0`); a confirmed `0` or every open issue
     dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify` and `… -Pintegration-tests` pass at the head, and so does `-Ppre-commit` with every rewrite committed.
- No new dependency or plugin. Test input data from `Generators` with `@EnableGeneratorController`; expected catalogue text literal; every new text en and de; never catch or throw generic `Exception`/`RuntimeException`;
  package-private over public; no `System.out`/`System.err` except where a CLI writes its own output; no shims. Output safety: control sequences removed from every runtime or model string.
- Terminal check through the screen capture harness (`27-tui-testing.adoc`), saved as `pm-operator/src/test/resources/screen-captures/PLAN-139.txt`, in `en` and `de`.
- Ports: mock web port 21390 (`20000 + 10 × 139`), app port 21391 (`AGENTS.md` § Ports); never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has had one CodeRabbit review; counts stated from `ci pr comments`; merge on the
  operator's word only; commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `PlanViewLevel`, `PlanViewFormatting`, `OverviewFormatting` exist in `planview/` and `overview/` and are edited by PLAN-123 (running) — read at `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewLevel.java`, `.../overview/OverviewFormatting.java`; the PLAN-123 surface in `queue-view.md`
- OBSERVED: the terminal has no chat that follows a stream and no `o`, `older` or `changes` — read at `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` § `execute`
- OBSERVED: events are pushed above the prompt by the console with `LineReader.printAbove` — read at `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java:652,788`
- HYPOTHESIS: a dialogue can print a `–` line above its prompt through the same `LineReader` it asks with, without `PmConsole` — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` § `reader()` (verify-at-outline)
- HYPOTHESIS: the plan view and the overview row get `queued_revision` from the resources PLAN-132 extends, so no new route is read — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` after PLAN-132 (verify-at-outline)
- HYPOTHESIS: pre-filling a line editor buffer for a starter is possible with `LineReader.readLine(prompt, null, buffer)` — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` § `read` (verify-at-outline)
- Verify-first clause: re-read the files above at the then-current HEAD (PLAN-123, 44 and 50 touch `planview/` and `overview/`); PLAN-138 and PLAN-134 must have landed.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewLevel.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewFormatting.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/ProjectWorkLevel.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/planview/PlanViewFormattingTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFormattingTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ChatKnowsIT.java` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-139.txt` — absent; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/22-texts-actions.adoc` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-138`, `PLAN-134`.
- Overlaps with:
  - `PLAN-123` (running): `planview/*`, `overview/*`, `operator.*.properties`.
  - `PLAN-44`, `PLAN-50` (staged): `planview/` and `overview/` directories; this plan follows them.
  - `PLAN-84` (running): `PmConsole.java` (only if the notices need it).
  - `PLAN-79` (staged): rewrites every operator test.
- Adjacent to: `AnswerCommand` and `ReviewDialogue` (PLAN-140), the web's own status cards (PLAN-136).

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; list which of PLAN-123, 44, 50 landed.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-139-cli-chat-knows-and-status.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.
