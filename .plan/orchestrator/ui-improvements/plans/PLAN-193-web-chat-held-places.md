# PLAN-193: The Chat's Held Places: Project Setup, Decomposition and Review, Ended Work, and the End of `ChatPanel`

epic: ui-improvements
workstream: WS-07

> Staged plan spec, PARKED. Split out of PLAN-137 on the operator's word of 2026-11-25 ("split as proposed"): PLAN-137 keeps the start's
> and the decision's place; this plan holds the rest — PLAN-137 deliverable 1's project-setup half and deliverables 2, 4 and 5 — which the
> cleanup of 2026-11-25 found blocked. **Unparked per deliverable when its block is gone; re-ground then.** This spec is
> SELF-SUFFICIENT. **It adds no dependency.**

## Objective

Every chat place of the web app uses `Chat`, and `ChatPanel` with the old chat client goes. Blocks at staging: the project setup step has no
template with a side column (`ProjectOnboardingView` draws its own drawer); the runtime specification names no address for the
decomposition and review chats (runtime-model `01-processes-and-transport.adoc:179,289-300`; `ChatRoutes.java:27-28`; epic Open Item O10);
the ended-work follow-up waits on the epic's chat contract Open Item; `ChatPanel` cannot go while EpicView's review chat uses the old client.

## Deliverables

1. **The project setup's chat** (blocked: no side column on the setup step)
 - What: the project onboarding's drawer (`ProjectOnboardingView.svelte`, chat drawer ~`:946`, sends via `applyStep('chat')`) and the decision
   chat of a question of the project's setup become `Chat` in the side column of the setup step; a question the setup's workflow raised itself
   has no role and says so (`decision.chat.no_role`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when: `ProjectOnboardingView.test.ts` asserts the chat in the side column, `chat=open`, and the no-role line (evidence order: `test-first`).
2. **The decomposition chat and the epic review chat** (blocked: no runtime address)
 - What: the decomposition draft's chat and the review chat of one item or of the whole register as `Chat` with the scope the design names; an
   accepted change becomes the item's disposition or rewrite, or a change of the draft; a draft row or register item changed in the chat is
   marked `control.chat.changed_in_chat` until the decomposition or review is committed, the same on every device; the chat ends on the server
   when the review ends. `DecompositionReviewView`'s `onChat` button (today wired to a local flag that opens nothing) opens it. Route builders
   in `ChatRoutes` and the mock routes follow the runtime's addresses.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when: tests assert the item scope is sent, the mark appears and goes on commit, and the chat ends on the server with the review (evidence order: `test-first`).
3. **Ended work and the follow-up plan** (blocked: the chat contract Open Item)
 - What: the ended plan's and epic's chat (`replan`, `decompose`): the composer works (the archive never changes); the one proposal is the
   follow-up plan; accepting opens the start of a new plan in the same project with the description stored and the chat goes on
   (`control.chat.follow_up.opened`, PLAN-136); the transcript is kept beside the archive entry. The row act "Chat about it" stays PLAN-52's.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when: a test asserts the composer enabled, the archive untouched by a turn, accepting opens the start with the description and does not end the chat (evidence order: `test-first`).
4. **`ChatPanel` and the old chat client are gone** (after 1–3 and PLAN-192)
 - What: delete `ChatPanel.svelte` and `ChatPanel.test.ts`; delete the old chat types and methods of `client.ts` (`ChatTurn`, `ChatProposal`,
   `ChatReadBack`, `getChat`, `chatWithSubject`, `answerSubjectChatProposal`, `endChat`, the decision and entry chat methods, `getEndedChat`
   as they are replaced) and the `start.chat.*` keys nothing uses (the shared design keeps `start.chat.subject/waits/proposed/stored`,
   `shared/starting-work-texts.adoc:197-212`); the e2e specs follow. Browser check of every place in German and English, light and dark, at
   `1440`, `1024` and `390` px.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when: `git grep -n "ChatPanel" -- pm-web-app pm-web-e2e` is empty (evidence order: `none`); the browser check shows each place (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only: the module builds, `PM_MOCK_WEB_PORT=21930 PM_WEB_PORT=21931 ./mvnw -pl pm-web-e2e verify -Pe2e -am`, the
  full `./mvnw verify` and `./mvnw verify -Ppre-commit` at the head. Ports: mock web `21930`, app `21931`.
- **No new dependency or plugin.** Java 25, no generic `Exception`/`RuntimeException` caught or thrown in production code, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`, contract values literal. No `{@html}` with model or engine text, no timer wait in a test. Pre-1.0: no shim.
- ZERO Sonar findings on the new code of the pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`; a dismissal only on the operator's selection.
- The merge rule (`CLAUDE.md`, Git Workflow): the run merges its own pull request once every review thread has a reply and is resolved, every review-body item is answered, some head has had one CodeRabbit review, the build and Sonar are green. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator only if it still refuses.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Never touch, rebase or merge another plan's branch (the parked PLAN-42's included). Other plans run at the same time: never stop or reuse a process this worktree did not start.

## Claim Labels

- OBSERVED: `ProjectOnboardingView` uses no page template and draws its own chat drawer (~`:937-1000`), sending through `applyStep('chat')` (`:578-586`) — read at `3dea04b2` (cleanup 2026-11-25)
- OBSERVED: `ChatRoutes.java:27-28` leaves the review and decomposition chats out "until the runtime specification names their addresses"; runtime-model `01-processes-and-transport.adoc:179,289-300` lists plan, epic, entry, decision and ended routes only — read at `3dea04b2` / documentation `origin/main`
- OBSERVED: `EpicView.svelte:655-658` reads the review chat through `getChat('epic:'+id)` without the item, and `:693-696` ends it locally only — read at `3dea04b2`
- Verify-first clause: before each deliverable, re-read its block; a deliverable whose block still stands is not started and is reported.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/chat/ChatRoutes.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/chat/ChatRoutesTest.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-e2e/tests/start.spec.js` — present at HEAD 3dea04b2

## Dependencies and Sequencing

- Depends on: PLAN-135, PLAN-136, PLAN-137 (the start and decision places), PLAN-194 (fixtures); deliverable 4 also on PLAN-192.
- Blocks named per deliverable above.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-193-web-chat-held-places.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports
its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
