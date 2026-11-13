# PLAN-138: Terminal Chat, the Dialogue: Wire, Leaving, New, the Reply as It Is Written, the Proposal (D4/D5 Terminal Chat, Part 1)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED
> D4 and D5 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The terminal's chat of a plan, an epic and ended work works against the old contract: it posts `{"message": …}` to a route the web and the mock
read as `text`, `plan chat --message` prints raw JSON, an empty line **ends** the chat, no reply streams, and no key stops it. The AGREED design
(`terminal-client/19-actions.adoc` § `#chat`) says the opposite for each: leaving keeps the chat (as Hide does), `new` starts a fresh one, steps
and the reply are printed as they come, `s` stops, and the proposal is read back with the refusals of D5. This plan fixes the dialogue on the
contract of PLAN-132. What the role knows, status lines and notices are PLAN-139; the other places are PLAN-140.

## Deliverables

Specification: `doc/design/terminal-client/19-actions.adoc` § `#chat`, `#chat-ended`; `doc/design/terminal-client/28-chat.adoc` § `#notices` (the
overtaken proposal, the paused plan's chat); `doc/design/terminal-client/04-console.adoc` § `#line-editor`, `#output-safety`;
`doc/design/terminal-client/22-texts-actions.adoc` (`operator.actions.chat.*`); `doc/design/shared/work-actions.adoc` § `#chat`, `#chat-ended`.

1. **The wire**
 - What:
   - Post the turn as `{text, client_turn_id}` to `…/chat/turns` and read the `202`; accept and reject name the proposal's `turn_id`; use the B1 chat
     model and route builders; delete the old chat types from the terminal's use (`ChatReadBackResource`, `ChatTurnResource`, `ChatProposalResource`).
   - `plan chat <plan_id>` and `epic chat <epic_id>` are dialogues only (`19-actions.adoc:27,47`): remove `--message`, which posts to `…/chat`
     and prints raw JSON (O8); `--no-input` refuses with `operator.actions.chat.no_input`.
   - A refusal prints its catalogue text with the state the design names (`act_not_available`, `conflict`, `target_not_found`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-security, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test against the mock asserts the posted body (`text`, `client_turn_id`) and the path, a repeated `client_turn_id` after a lost answer, and
     that `plan chat <id> --message x` is an unknown option (exit `2`); each fails on a revert (evidence order: `test-first`).
2. **Leaving and `new`**
 - What:
   - An empty first line, `q` or `Ctrl+D` (also at `Accept?`) leaves the chat: it stays with its subject, an open proposal included, and prints
     `Left the chat; it stays with fix-relay.` (`operator.actions.chat.left`); `plan chat` (or `h) chat` where a menu offers it) returns to it where it was, an
     open proposal printed again with its `Accept?`. A reply being written goes on in the runtime.
   - `new` (German `neu`, `operator.actions.chat.new_command`) as the first line asks `Start a new chat? y) yes   n) no [Enter]`
     (`control.chat.new_confirm.title`, with `.body` while a proposal is open, which is dropped); `y` ends this chat and opens an empty one, which begins with
     `The earlier chat is kept with fix-relay.` (`control.chat.ended`).
   - Remove the keys and sites of the old behaviour (`operator.actions.chat.kept`, `operator.actions.chat_end`, the start chat's ending on an empty line
     stays PLAN-140).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert that an empty line, `q` and `Ctrl+D` do not call `…/chat/end`, that the return shows the open proposal again, and that `new` with `y`
     calls end and open and with `n` or Enter calls neither (evidence order: `test-first`).
3. **The reply as it is written, and `s`**
 - What:
   - Follow the subject's stream (`SseReader`) from before the chat is read: each step the role reports is a muted line under `▶  replan answers`
     (`control.chat.step.*`), the reply a line at a time as each line completes (never part of a line); while the role writes the prompt is
     `s) stop the reply   q) leave: the reply goes on   ›` (`operator.actions.chat.writing`, `operator.actions.key.stop`, `s` in both languages) with the lines
     printed above it; `s` stops the reply, which keeps its text so far (`Stopped; the reply so far is kept.`, `control.chat.stopped`).
   - A reply announced again for a replaced worker prints `The reply starts again.` (`operator.actions.chat.restarted`) and its lines below; a harness that
     reports no pieces prints the whole reply when it ends; a failed reply prints `control.chat.infeasible`, `control.chat.lost` or
     `control.chat.too_long_reply`. The reply is printed after control characters are removed (`OutputSafety`); an `untrusted` turn is printed between quotes.
   - What `s` and a typed line do while the role writes follows the operator's answer to O9.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-security, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests with the mock in `ordered` timing assert the printed lines in order, that a partial line is never printed, that `s` calls stop and keeps the text, the
     restart line, the no-pieces path and each failure; one asserts that an ESC/CSI/OSC sequence in a delta never reaches the output (evidence order: `test-first`).
4. **The proposal, its refusals and ended work**
 - What:
   - A proposal is a read-back, old against new: `Enter` accepts, `r) reject` drops it; `Start a plan with it? [Enter]   r) reject` in ended work; a check that
     fails prints `not applied: <reason>` and the chat goes on; a running plan prints `✓ accepted: applies after the current task` and later its applied line;
     a paused plan `applies when fix-relay starts again`.
   - Overtaken: `Enter` on a plan's proposal made against an older version prints `control.chat.changed_since` and asks again with `r) reject` only; on a plan's
     revision while another is queued `control.chat.already_queued` (`Accept?` printed again when the queued one is not applied, while an applied one makes it
     a proposal of an older version); a proposal the plan can no longer take prints `control.chat.lapsed` in place of its `Accept?`, which wins where both hold.
   - Before the phase where a revision is proposed the role says so once (`fix-relay proposes no change yet: …`); a paused plan's chat runs and a reply changes nothing.
   - Ended work: the one proposal is the follow-up plan; `Enter` leaves the chat, which goes on and stays with the ended work, and opens `plan add` at its sources
     with the description stored (`control.chat.follow_up.opened`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert each path above, including both exits of already-queued (cleared unapplied, applied) and that ended work's accept does not end the chat
     (evidence order: `test-first`).
5. **Texts and evidence**
 - What:
   - The `operator.actions.chat.*` and `operator.actions.key.{stop,open,chat}` keys of `22-texts-actions.adoc` in en and de; the old chat keys removed;
     `LiteralTextGuardTest` still guards `WorkChatDialogue` and the new classes.
   - A PTY test `ChatDialogueIT` against the mock (plan chat, stream, stop, leave and return, `new`, proposal accept and reject, an overtaken proposal, ended work)
     and screen captures in `en` and `de` saved as `pm-operator/src/test/resources/screen-captures/PLAN-138.txt`: a streamed reply, the `s` prompt, `Left the chat`,
     an overtaken proposal.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ChatDialogueIT` passes with `-Pintegration-tests` and asserts the screen, not only the exit code (evidence order: `test-first`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify` and
  `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Pintegration-tests` pass at the head, and so does `-Ppre-commit` with every rewrite committed.
- No new dependency or plugin. Test input data comes from `Generators` with `@EnableGeneratorController`; expected catalogue text stays literal. Every new text is
  in en and de. Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public; no `System.out`/`System.err` except where
  a CLI writes its own output; no compatibility shims.
- Terminal check through the screen capture harness (`ScreenCaptureIT`; command line in `doc/design/terminal-client/27-tui-testing.adoc`), saved as
  `pm-operator/src/test/resources/screen-captures/PLAN-138.txt`, in `en` and `de`.
- Ports: mock web port 21380 (`20000 + 10 × 138`), app port 21381 (`AGENTS.md` § Ports); other plans run at the same time: never stop or reuse a process this
  worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered; some head has had one
  CodeRabbit review; the pull request body states the thread count, the resolved count and the review-body items from `ci pr comments`, never from memory; merge only
  on the operator's word; commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: the terminal posts `payload.put("message", message)` to `…/chat/turns` and expects status `200` — read at `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java:318-330`
- OBSERVED: `plan chat --message` posts to `…/chat` and prints the raw body — read at `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java:347-361`; `PlanCommand.java:811-831`; `EpicCommand.java:574-594`
- OBSERVED: the design lists `plan chat <plan_id>` and `epic chat <epic_id>` with no `--message` and says chats are dialogues — read at `doc/design/terminal-client/19-actions.adoc:27,39,47`
- OBSERVED: the start chat ends on an empty line (`api.endChat`) — read at `pm-operator/src/main/java/de/planmarshall/operator/ChatDialogue.java:58-91`; the plan and epic chat ends the dialogue on a blank message — read at `WorkChatDialogue.java` § `execute`
- OBSERVED: no stream, no stop key, and no step lines in `WorkChatDialogue` — read at `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` § `execute`
- OBSERVED: `PmConsole` pushes events above the prompt with `reader.printAbove` — read at `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java:652,788`
- OBSERVED: `StartPrompt` wraps a JLine `LineReader` (`askRaw`, `askInline`, `reader()`) — read at `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java:138,155,217`
- HYPOTHESIS: the dialogues in `pm-operator` can read a plan's, an epic's and a workspace's event stream through `pm-api`'s `SseReader` and `UnixSocketHttpClient` without a new dependency — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/http/SseReader.java` and `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` § event stream (verify-at-outline)
- HYPOTHESIS: a typed line `s` or `q` while the reply writes can be read with the JLine `LineReader` and `printAbove` in one thread pair without a raw-key mode — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` § `read` (verify-at-outline)
- HYPOTHESIS: the `operator.actions.chat.*` set of `22-texts-actions.adoc` is 21 keys and none of them but `operator.actions.chat.{you,accept,reject…}` exists — confirm/refute at `doc/design/terminal-client/22-texts-actions.adoc` and `pm-api/src/main/resources/messages/operator.en.properties:341-360` (verify-at-outline)
- Verify-first clause: re-read every file above at the then-current HEAD (PLAN-123, running, edits `PlanCommand` and `AnswerCommand`; PLAN-30, 42, 44, 48 touch `ActionSupport`, `EpicCommand`, `PmConsole`); the operator's answers to O8 and O9 are read before scoping deliverables 1 and 3.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorActions.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlDialoguesTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlFixture.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionSupportTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ChatDialogueIT.java` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-138.txt` — absent; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/22-texts-actions.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-132` and `PLAN-133` (contract and mock); `PLAN-123` landing (it is running and edits `PlanCommand`, `AnswerCommand`); the operator's answers to O8 and O9.
- Overlaps with:
  - `PLAN-123` (running): `PlanCommand.java`, `EpicCommand.java`, `operator.*.properties`.
  - `PLAN-30` (staged): `PlanCommand`, `EpicCommand`, `WorkActionDialogue`, `operator.*.properties`, `LiteralTextGuardTest`.
  - `PLAN-42`, `PLAN-44`, `PLAN-48` (staged): `PlanCommand`, `EpicCommand`, `PmConsole`, `ActionSupport`; this plan follows them.
  - `PLAN-79` (staged): rewrites every `pm-operator` test; it may follow this plan (its sweep then includes `ChatDialogueIT`).
  - `PLAN-84` (running): `PmConsole.java`.
- Adjacent to: `AnswerCommand` (the decision chat is PLAN-140), `ReviewDialogue` and `ChatDialogue` (PLAN-140).

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; list which of PLAN-123, 30, 42, 44, 48, 79 landed and re-place the edits at their new lines.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- `PLAN-161` (chat bridge) adapts `WorkActionsClient.java:305-347`, `WorkChatDialogue` and `ActionSupport.java:380` to the B1 contract
  first. This plan replaces that adaptation; re-ground D1 on what `PLAN-161` landed.
- "the start chat ends on an empty line at `ChatDialogue.java:58-91`" is stale: `ChatDialogue` was rewritten in `a139ec62` (the
  behaviour holds). "Delete the old chat types": `ChatReadBackResource` is used by `ActionSupport` and `WorkActionsClient` unless
  `PLAN-161` removed it.
- Operator-client lesson from PLAN-133: track transcript progress by turn id and status, not by a count; test against streamed timing and a
  transcript longer than one page.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-138-cli-chat-dialogue.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **`plan chat --message` (O8):** removed (the design lists `plan chat <plan_id>` and `epic chat <epic_id>` only; pre-1.0, no shim).
- **Input while the role writes (O9):** line-based: `s`+Enter stops, `q`+Enter leaves, any other line is held and offered as the next first line once the reply has ended.
