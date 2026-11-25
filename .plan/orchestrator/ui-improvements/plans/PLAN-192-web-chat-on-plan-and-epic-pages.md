# PLAN-192: The Chat on the Plan's and the Epic's Page

epic: ui-improvements
workstream: WS-07

> Staged plan spec, PARKED. Split out of PLAN-135 (its deliverable 5) on the operator's word of 2026-11-25 ("split as proposed"): the
> cleanup of 2026-11-25 found that the plan and epic pages carry no side column (PlanView uses no template, EpicView sits on
> `ObjectPage`), so the `Chat` PLAN-135 builds has nothing to dock into there. **Unparked only when a template with a side column
> carries those pages (a design decision against `components.adoc:365-376`, `navigation.adoc:279-285,414-415`); re-ground then.**
> This spec is SELF-SUFFICIENT. **It adds no dependency.**

## Objective

The plan's and the epic's chat open beside the page, in the template's side column, like the decision's and the start's, instead of
the `ChatPanel` inside `WorkControlPanel`.

## Deliverables

1. **The chat in the side column, and the plan's and epic's chat acts**
 - What:
   - The plan's and the epic's `Chat` (PLAN-135) is passed to the side column of the template that carries the plan and epic pages:
     `side` snippet, `sideOpen` from `chat=open`, `onHideSide` from _Hide the chat_ and `Escape`, which replace the address without
     `chat`. The width (`480`, or `400` where `480` would cost a column), the take-the-space rule and the phone (the chat takes the
     screen and the bottom bar) are the template's, measured on its container; this plan asserts them. What stays the chat's own: the
     transcript keeps a third of the height with the on-screen keyboard open, and the pinned proposal folds into the composer's head
     (`chat.adoc#placement`).
   - Hiding keeps the chat running; the act that opened it carries a `CountBadge` of the replies that arrived meanwhile; the scroll
     position and the draft are kept per chat. Reloading, hiding, or opening on another device returns to the same chat.
   - Replace `ChatPanel` in `WorkControlPanel.svelte` (the plan's and the epic's chat acts).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - View tests assert `sideOpen` from `chat=open`, hiding by `onHideSide` with a replace, the `CountBadge`, the kept draft (evidence order: `test-first`).
2. **The chat e2e and the browser check**
 - What: `pm-web-e2e/tests/chat.spec.js` on the mock: open, stream, stop, hide and return, reload, second device through
   `/mock/broadcast` (or PLAN-194's fixtures), in German and English, light and dark, at `1440`, `1024` and `390` px; the rail expanded
   and collapsed.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `chat.spec.js` passes with the plan's ports; it waits on events, never on time (evidence order: `test-first`).
   - Browser check (Playwright MCP) on `running-work` → `plan fix-relay` with the chat open: widths, phone, working line, a streaming
     reply, Stop; baseline and after snapshots kept (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=21920 PM_WEB_PORT=21921 ./mvnw -pl pm-web-e2e verify -Pe2e -am`,
  the full `./mvnw verify` and `./mvnw verify -Ppre-commit` at the head. Ports: mock web `21920`, app `21921` (`AGENTS.md` § Ports).
- **No new dependency or plugin.** Java 25, no generic `Exception`/`RuntimeException` caught or thrown in production code, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`, contract values literal. No `{@html}` with model or engine text, no timer wait in a test. Pre-1.0: no shim.
- ZERO Sonar findings on the new code of the pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`; a dismissal only on the operator's selection.
- The merge rule (`CLAUDE.md`, Git Workflow): the run merges its own pull request once every review thread has a reply and is resolved, every review-body item is answered, some head has had one CodeRabbit review, the build and Sonar are green. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator only if it still refuses.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Never touch, rebase or merge another plan's branch (the parked PLAN-42's included). Other plans run at the same time: never stop or reuse a process this worktree did not start.

## Claim Labels

- OBSERVED: `PlanView.svelte` uses no page template (`:839` plan header, `:963` `WorkControlPanel`); `EpicView.svelte:724` sits on `ObjectPage`, which has no side column (`ObjectPage.svelte:11`) — read at `3dea04b2` (cleanup 2026-11-25)
- OBSERVED: `WorkControlPanel.svelte` renders `ChatPanel` for the plan's and epic's chat (`:1136`, `:1154`) — read at `3dea04b2`
- Verify-first clause: at unpark, re-read which template carries the plan and epic pages and its side API; a page without a side column stops this plan.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.svelte` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-e2e/tests/chat.spec.js` — absent at HEAD 3dea04b2; created by this plan

## Dependencies and Sequencing

- Depends on: PLAN-135 (the `Chat` component), PLAN-55 (the plan page), and a design decision that gives the plan and epic pages a side column.
- Overlaps with: PLAN-137 and PLAN-193 (`WorkControlPanel.svelte`, the ended chat block).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-192-web-chat-on-plan-and-epic-pages.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports
its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
