# PLAN-140: Terminal Chat in Its Places: Decision, Review, Decomposition, Start (D4/D5 Terminal Chat, Part 3)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D4 and D5
> documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The dialogue of PLAN-138 and PLAN-139 holds for every place a chat opens; the terminal has the plan's, the epic's and ended work's. This plan adds
the places that are missing or wrong: the decision's chat (`h)` / `t)` at its prompt; the key is not advertised today), the review's chat (it opens the
whole epic's chat whatever the item), the decomposition's, and the start's (`ChatDialogue`, which ends on an empty line and uses its own `start.chat.*`
texts), and the `h) chat` of the menus and ended views.

## Deliverables

Specification: `doc/design/terminal-client/14-transcripts-decisions.adoc` § `#t-decision-chat` and the setup note; `doc/design/terminal-client/19-actions.adoc`
§ `#review`, `#chat`; `doc/design/terminal-client/18-starting.adoc` § `#decomposition`, `#chat`; `doc/design/shared/decisions.adoc` § `#chat`, `#setup`;
`doc/design/shared/starting-work.adoc` § `#chat`; `doc/design/shared/work-actions.adoc` § `#chat-places`, `#review`; `doc/design/terminal-client/28-chat.adoc`.

1. **The decision's chat**
 - What:
   - At a decision's prompt `h) chat` (German `t) chat`, `operator.actions.key.chat`) stands with _none fits_ and _later_, for the whole decision; it is no answer and the
     plan waits while it runs. It opens the chat with the decision as its scope; the role follows who asked (`implement` for Q-007, `replan` or `decompose` for one a quorum or a
     server rule raised); a setup question of the workflow's own is refused with `decision.chat.no_role`; a waiver request offers no chat.
   - A proposed answer is offered only where the part takes own words; `Enter` sends it to `interpret` as the operator's words and the read-back follows as in the own-words
     dialogue; `r) reject` drops it and the chat goes on. A revision accepted in the chat leaves the decision open unless the runtime closes it as withdrawn.
   - Leaving (empty first line, `q`, `Ctrl+D`) keeps the chat and prints the decision again, answerable in all its ways; left while the role writes or a proposal is open, the decision's
     answers are held and its prompt offers `h) chat`, `l) later` and `?` only, under `operator.decision.chat_wait`, and is drawn again once the reply has finished or the proposal was
     decided. Answering the decision ends its chat; `– Q-007 was answered in the web app (iPad); the chat has ended.` where it was answered elsewhere.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert the key is offered in `en` and `de`, not for a waiver, refused with the catalogue text for a rule-raised setup question; a proposed answer reaches `interpret`
     unchanged; the held prompt while writing and with a proposal open; answering ends the chat (evidence order: `test-first`).
2. **The review's chat and the decomposition's chat**
 - What:
   - `h) chat` in the review asks its scope, `1) this item ←   2) the whole register`, and opens the chat with that scope; an accepted change becomes the item's disposition or rewrite
     (`ReviewDialogue` opens the whole epic's chat whatever the choice today); the decomposition's `h) chat` opens the chat of the draft; both end with their review or draft.
   - The addresses of these two chats follow the operator's answer to O10; until then this deliverable is blocked.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert the item scope is the one requested, a register scope the other, an accepted change lands on the item, and the chat ends with the review (evidence order: `test-first`).
3. **The start's chat**
 - What:
   - Replace `ChatDialogue` and its `start.chat.*` texts with the shared dialogue: `h) chat` at `start.sources` and `start-epic.sources` offered once a source is stored and each source from outside the machine is
     screened, for a plan and an epic, never for a recipe; the proposals are a rewrite of or an addition to the description, stored through `start.store-description`, never another source; an empty line leaves
     the chat (it no longer ends it).
   - Delete `ChatDialogue.java`, the `start.chat.*` keys it used, and `StartChatResource` and `FollowUpProposalResource` with their codecs and tests where PLAN-138 left them.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `git grep -n "ChatDialogue\|start.chat" -- pm-operator pm-api` is empty (evidence order: `none`).
   - Tests assert the offering rule, a recipe without the key, an untrusted source marked, the stored description, and that an empty line keeps the chat (evidence order: `test-first`).
4. **`h) chat` in the menus and the ended views**
 - What:
   - `h) chat` in the menu below a row and in the menu of `a` on ended work and an ended view opens the same dialogue with its scope (`ActionMenus`, `ActionsMenu`); on ended work the chat is
     read only in the sense that the archive never changes, its composer works, and its one proposal is the follow-up plan (done by PLAN-138 for `plan chat`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when:
   - Tests assert the key in the row menu and the ended menu and that it opens the chat of the right subject (evidence order: `test-first`).
5. **Evidence and documents**
 - What:
   - PTY tests `DecisionChatIT`, and a review and start chat case in the existing ITs; screen captures in `en` and `de` saved as `pm-operator/src/test/resources/screen-captures/PLAN-140.txt`:
     a decision chat with a proposed answer, a held prompt, a review scope choice, the start chat.
   - The text table of `14-transcripts-decisions.adoc` and `22-texts-actions.adoc` is updated with every key the places use.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - The ITs pass with `-Pintegration-tests` and assert the screen (evidence order: `test-first`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify` and `… -Pintegration-tests` pass at the head, and so does `-Ppre-commit` with every rewrite committed.
- No new dependency or plugin. Test input data from `Generators` with `@EnableGeneratorController`; expected catalogue text literal; every new text en and de; never catch or throw generic
  `Exception`/`RuntimeException`; package-private over public; no `System.out`/`System.err` except where a CLI writes its own output; no shims. The answer dialogue's sanitizing writer
  (PLAN-124) stays the only path for decision text.
- Terminal check through the screen capture harness (`27-tui-testing.adoc`), saved as `pm-operator/src/test/resources/screen-captures/PLAN-140.txt`, in `en` and `de`.
- Ports: mock web port 21400 (`20000 + 10 × 140`), app port 21401 (`AGENTS.md` § Ports); never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has had one CodeRabbit review; counts stated from `ci pr comments`; merge on the
  operator's word only; commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `AnswerCommand.java` offers no `h) chat` today (PLAN-124 removed the advertisement until the surface exists); `git grep -n chat -- AnswerCommand.java` finds one unrelated line — read at `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: AnswerCommand has no chat mention at all now (spec said one line); no h) chat is offered
- OBSERVED: `ReviewDialogue` has a `CHAT` decision and the key `chat` at its prompt — read at `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java:106,120,175,201`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ReviewDialogue CHAT :106, enum :120, key chat :175,201; case CHAT opens WorkChatDialogue epic chat at :107
- OBSERVED: `StartDialogue` handles `h` with `openChat()` and runs `ChatDialogue` — read at `pm-operator/src/main/java/de/planmarshall/operator/StartDialogue.java:247,672-682`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: StartDialogue case h -> openChat() :247; openChat :672-684 runs new ChatDialogue(...)
- OBSERVED: `ChatDialogue` ends the chat on an empty line — read at `pm-operator/src/main/java/de/planmarshall/operator/ChatDialogue.java:58-91`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ChatDialogue.run :84-85 an empty answer calls endChat(entryId)
- OBSERVED: the design says the decision chat's key is `operator.actions.key.chat` (`h`, `t`) and that without the surface it is not advertised — read at `doc/design/terminal-client/14-transcripts-decisions.adoc:17,195,253`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: 14-transcripts-decisions.adoc:195 (key operator.actions.key.chat, h/t) and :253 (not advertised without the surface)
- HYPOTHESIS: the review chat's item scope is not sent by the current client (the web one is the same) — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java` § `case CHAT` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ReviewDialogue:107 opens WorkChatDialogue epic chat with epicId only; no item scope is sent (web EpicView:477 the same)
- HYPOTHESIS: `ActionMenus` and `ActionsMenu` already list a chat entry that runs `WorkChatDialogue` for plans and epics only — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionMenus.java` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ActionMenus chat entries :39,128,247,267; chat runs WorkChatDialogue for plan/epic kinds only (ActionSupport:360, OperatorActions:92, WorkActionDialogue:307)
- HYPOTHESIS: `StartApi` carries the entry's chat routes and needs the B1 route builders — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/StartApi.java` § chat (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: StartApi uses ChatRoutes.entry for open :135, turn :151, page :175, accept :199, end :213 (B1 builders)
- Verify-first clause: PLAN-123 (running) and PLAN-124 (landed) edit `AnswerCommand`; re-read the decision prompt at the then-current HEAD before scoping deliverable 1; the operator's answer to O10 is read before scoping deliverable 2.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural clause; PLAN-124 shape confirmed (no chat in AnswerCommand) but PLAN-123 status unreadable

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartDialogue.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ChatDialogue.java` — present at `1cc0e10e`; deleted by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartApi.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DecomposeCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionMenus.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionsMenu.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/StartDialogueTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/StartApiTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswerMultipartTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/AnswerDialogueIT.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/DecisionChatIT.java` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-140.txt` — absent; created by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/start/StartChatResource.java` — present at `1cc0e10e`; deleted by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/start/FollowUpProposalResource.java` — present at `1cc0e10e`; deleted by this plan
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/14-transcripts-decisions.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/22-texts-actions.adoc` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatTurnResource.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatProposalResource.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatScope.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatState.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatRole.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/start/StartStateResource.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/control/ControlCataloguesTest.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/start/StartCodecTest.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/start/StartContractTest.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatRegistry.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatScripts.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatProposalsTest.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatRevisionQueueTest.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatContextNarrativeTest.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatListenerAndResidueTest.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/chat/ChatProposal.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/chat/ChatMessageKeysTest.java` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at `7adb56c2` (D-R134)
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at `7adb56c2` (D-R134)

## Dependencies and Sequencing

- Depends on: `PLAN-138`, `PLAN-139`; `PLAN-123` landing; the operator's answer to O10.
- Overlaps with:
  - `PLAN-123` (running): `AnswerCommand.java`.
  - `PLAN-30` (staged): `AnswerCommand`, `WorkActionDialogue`, `operator.*.properties`, `LiteralTextGuardTest`.
  - `PLAN-44` (staged): `AnswerCommand.java`, `ActionSupport`, `PmConsole`.
  - `PLAN-79` (staged): rewrites every operator test.
- Adjacent to: `EntryFlow` and the project onboarding dialogue (the setup question chat is the decision chat of deliverable 1).

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; list which of PLAN-123, 30, 44 landed.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- `StartApi` already runs the start chat on the B1 model (`a139ec62`); the HYPOTHESIS "StartApi needs the B1 route builders" is settled true.
  This plan still replaces its 60 s polling with stream-following and the `start.chat.*` texts; `StartChatResource` deletion stays here.
- **m5:** the poll loop `StartApi.java:158-160` (`LockSupport.parkNanos`, up to 600 GETs) does not check the interrupt flag. The
  replacement must stop on interrupt; one test.
- `PLAN-161` adapts `WorkActionsClient`/`WorkChatDialogue` to B1 first; this plan and `PLAN-138` replace that adaptation.
- D2 (review/decomposition chat) needs the O10 route builders in `ChatRoutes` (absent at `a139ec62`); see the epic's Open Items.

## PLAN-161 Landing Residue (Amendment 2026-11-14)

- The terminal's ended-work menu and `h) chat` need the same new-entry surface as `PLAN-138` D4, and D1's decision chat address needs the
  decision envelope's subject id: both in the epic's contract Open Item.
- Owner of the orphaned old-shape `pm-api` chat types: after deleting `StartChatResource`/`FollowUpProposalResource`, this plan also deletes
  `control.ChatTurnResource`, `ChatProposalResource`, `ChatScope`, `ChatState`, `ChatRole` and `StartStateResource.chat` with their codecs
  and tests, if nothing else uses them.

## Finding from `analyze-fix-build-time` (#432), 2026-11-14

- **Rebuild "no role qualifies" (operator: "Rebuild on ChatRegistry").** PLAN-133 moved the start chat into `ChatRegistry`;
  the old `StartRegistry` fixture member `chat.available: false` that produced `start.chat.role_unqualified` went with it, and so did
  its tests. `StartStateResource.CHAT_ROLE_UNQUALIFIED` still exists and nothing sets it. Give the mock a way to refuse opening a chat
  because no role qualifies (a scenario flag or state read by `ChatRegistry`, answered with the contract's refusal), a scenario that
  uses it, and the terminal's line for it; keep the constant (or its B1 successor) — do not delete it with the old start-chat types. If
  the B1 contract has no refusal code for it, it belongs to the epic's chat contract Open Item: stop and ask.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-140-cli-chat-places.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Review and decomposition chats (O10):** own addresses, as PLAN-132 records; this plan follows them.

## PLAN-127 Landing Residue (Amendment 2026-11-13)

- `AnswerCommand` line refs are stale since `98cea294` (no `chat` yet: the PLAN-140 premise holds). `EpicCommand` chat now at `:699`,
  `PlanCommand` chat at `:799`, `ActionSupport.chat` at `:350-386`. The EOF contract (`StartPrompt.StartLeftException`) and the
  `readOwnWords`/`clarified` path are shared with the answer verb.

## PLAN-134 Landing Residue (Amendment 2026-11-14)

PLAN-134 (#428, `7adb56c2`) landed without its fixtures and with open review findings. The operator's
routing on 2026-11-14 folds all of PLAN-134's mock residue into this plan. Do it as **D-R134** before the CLI
deliverables, in the same commit stream as the `role_unqualified` work on `ChatRegistry`, which edits the same files.
Line refs are at `7adb56c2`: CR = `pm-mock-runtime/.../ChatRegistry.java`, ME = `.../MockRuntimeEngine.java`.

1. **Scenarios (PLAN-134 D5, not shipped).** Extend `running-work.json` and `fully-onboarded.json` with:
   - a plan with a running task, and narrative events on it;
   - a plan decision chat that carries a revision proposal;
   - an ended plan;
   - knows and narrative for an epic and an entry.

   All of it must pass the load validation. PLAN-136, PLAN-137 and PLAN-139 consume these fixtures. Until they
   exist, their browser and PTY checks drive the events through `/mock/broadcast`.
2. **D-new-1 (m2 from PLAN-133, not shipped).** `applyChatProposal` (ME:918-931) only bumps an epic's rows or
   draft revision. Make it apply `after`, and update `ChatProposalsTest:216-232`.
3. **Race test.** Add a test for the replaced-worker race. The fix shipped untested: the check and replace in
   `playFailure` happen under one lock (CR:688-699).
4. **Review findings left unfixed (CodeRabbit "not marking addressed"), each with a test:**
   - **M1.** `scope.ended` does not close the revision window. `revisionWindowOpen` (ME:994-1014) ignores
     `planActivities`. A revision accepted from that plan's decision chat queues and never settles. It must be
     refused as lapsed. Thread 4573728016.
   - **m1.** `plan.paused`, whose state is `pausing`, is narrated as `paused` (CR:1195-1198). The later real
     `paused` is then deduplicated away (CR:1166). Thread 4573726390.
   - **m2.** `planFor` (ME:1024-1038) binds an epic or entry decision `X/Q-9` to a plan named `X` by id prefix.
     Thread 4569017923.
   - **m4.** An infeasible task outcome is narrated as done (CR:1115-1117).
   - **m5.** A `chat.proposal` ACCEPTED sent through `/mock/broadcast` (CR:1885-1897) skips `revisionRefusal`. It
     also overwrites a queued revision and can throw an NPE that the IOException-only catch at CR:1791 does not catch.
5. **Operator decision 2026-11-14, REFUSED stays rejectable.** A proposal refused for no plan, a passed boundary or
   a failed check must accept `reject`. Today `answer` (CR:1618) needs `isOpen()`, which is true for OPEN only
   (`ChatProposal:103`). Extend `isOpen()` to REFUSED for reject only. Thread 4573384117. The contract text lives in **pm-mcp-core**
   `doc/specification/state-catalogue.adoc` § `chat-proposal-states` (epic Open Item "chat proposal contract"),
   not in this repo.
6. **Operator decision 2026-11-14, the conflict and lapse notice gets a line key.** pm-mcp-core
   `state-catalogue.adoc` § `chat-proposal-states` says conflict and lapse are "told by a notice", but its closed
   `chat.line.*` table (§ `chat-narrative`) has no key for it. The key is named there first (epic Open Item
   "chat proposal contract"); this plan mirrors the named key.
   - Add the key to the catalogue and to `control.en/de.properties` (`ChatMessageKeysTest`).
   - Emit it as a notice turn on a conflict and on a lapse.
7. **Test pins PLAN-134 left out.** For apply, refuse, lapse, chat-end and decision, assert `plan.revision_queued`,
   the overview row, `activity.changed` and their order. Today only accept is pinned (`ChatRevisionQueueTest:100-128`).
   Assert "reject still taken" per refusal, and add a test for "another subject".
8. **Narrative completeness (PLAN-134 D1/D2 partly shipped):**
   - the "pull request merged" line;
   - epic and entry lines (`row_*`, `source_*`, `auto_changed`);
   - knows for epics and entries beyond `description` (ME:952-953): sources, rows, items;
   - pins for native `decision.opened` and for pausing.
9. **Smaller items:**
   - The hard-coded hold `H-1` in later plan replies (`ChatScripts:51,148-152`).
   - No `instance.changed` is emitted on an applied revision (CR:1318-1329).
   - Check against the contract that re-sending `chat.turn.completed` per notice extension is allowed (CR:602-622).

This is about 9 items, which breaks the ~6 scope guard. If the plan grows past it, split D-R134 into its own PR
first. It must land before PLAN-136, PLAN-137 and PLAN-139 need the fixtures.
