# PLAN-135: Web Chat, the Core: Transcript, Streaming, Markdown, Composer, Side Column (D4 Web Chat, Part 1)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED
> D4 and D5 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The web app has a small `ChatPanel` that posts a message and shows the answer in one piece. The AGREED design replaces it with
one `Chat`: a side column beside the object (the column engine's side column of `PLAN-142`; D6: no side column of the chat's
own), a transcript that streams the reply, the role's text rendered safely as
Markdown through `marked` (approved by the operator), a working line with the `WaitingMark`, a composer with Send and Stop, a
draft kept per chat, and an address `chat=open`. This plan builds that core for the plan's and the epic's chat acts and
retires `ChatPanel` there. Proposals, notices and status cards are PLAN-136; the other places and the removal of
`ChatPanel` everywhere are PLAN-137. Voice is not part of this plan: the composer keeps a snippet slot that draws nothing.

## Deliverables

Specification: `doc/design/web-app/chat.adoc` (§ `#everywhere`, `#placement`, `#transcript`, `#working`, `#markdown`,
`#composer`, `#live`, `#waiting`, `#renderer-decision`, `#texts`), `doc/design/web-app/components-chat.adoc` (§ `#chat`,
`#chat-turn`, `#chat-markdown`), `doc/design/web-app/navigation.adoc` (§ `#side-columns`, `#url`),
`doc/design/web-app/components-brand.adoc` (§ `#waiting-mark`), `doc/design/shared/work-actions.adoc` § `#chat`.

1. **The chat client, store and stream**
 - What:
   - A `pm-web-app/src/lib/chat/` module (not more lines in the 3,973-line `client.ts`): typed model of the B1 contract, the
     requests (open, read a page with `?before=`, turn with `client_turn_id`, stop, end, proposal accept and reject by
     `turn_id`), a per-chat store, and the follow rule: open the subject's stream before reading the chat, apply events after
     the read, read again after a `reset`, so a proposal decided meanwhile never stays open.
   - Only events of the catalogue (`chat.turn.started`, `.delta`, `.progress`, `.completed`, `chat.proposal`, `chat.ended`); the
     draft per chat in `localStorage` until it is sent; the operator's turn appears at once as `sending…` and is confirmed by the
     `202`; a failed send keeps the text and marks the turn Not sent.
   - The address: `chat=open` and `turn={turn_id}` in `routes.ts` and `adoptRouteFromLocation` (`stores/runtime.ts`), opened and
     hidden by replacing the address, never pushing; `turn=` reads pages back until the turn is loaded. Where `PLAN-143` has
     landed, its typed query vocabulary already carries `chat` and `turn` and its one writer does the replace: use them, add no
     second parser or writer. On a task's address `chat=open` names the chat of the plan the address is in
     (`/plans/fix-relay/tasks/TASK-4?chat=open` is the plan's chat beside the task), and a reload restores it so (D6;
     `navigation.adoc#side-columns` :284).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security, pm-dev-frontend:jest-testing
 - Done when:
   - Unit tests with a fake stream assert: the stream opens before the read; deltas append in order; a `reset` re-reads; a
     repeated `client_turn_id` after a lost answer sends once; a failed send keeps the draft; `chat=open` and `turn=` round-trip
     through the address without a history push (evidence order: `test-first`).
2. **`ChatMarkdown` and `marked`**
 - What:
   - Add `marked` to `pm-web-app/package.json` and `package-lock.json` (the operator's approval is recorded at
     `chat.adoc` § `#renderer-decision` and `#decisions`) and use it as the lexer only. A Svelte renderer creates one element per
     token of the closed subset (paragraph, emphasis, strong, inline code, block quote, list, table with sideways scroll, code
     block with its language label and a Copy act and no colours, link, reference) with text always in a text node: no
     `{@html}`, no `innerHTML`, no HTML string.
   - Links only with `http` or `https`, through `ExternalLink` (see O1), with the host shown beside a text that does not name it;
     an image is a link and never loaded; an `html` token and every token outside the subset render as their source text;
     references of the turn's `refs` become reference chips (`Link` in the chip look). A fenced Mermaid block is shown as its
     source in a code block labelled `mermaid` (O6).
   - While streaming it renders the text so far; an unfinished block shows as its text until it completes. It is never given
     the operator's text, an untrusted turn or a notice. The web lint fails on `{@html}` in it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security, pm-dev-frontend:jest-testing
 - Done when:
   - `ChatMarkdown.test.ts` has hostile fixtures that fail a naive renderer: a `javascript:` link, a `data:` link, a raw
     `<script>`, nested HTML, an image, an over-long table, an unfinished fence while streaming; each asserts text nodes only and
     no element created from markup (evidence order: `test-first`).
   - `eslint` fails on a `{@html}` added to the component (evidence order: `mutation-after`).
3. **`ChatTurn`, the working line and the transcript behaviour**
 - What:
   - `ChatTurn`: the operator's turn right-aligned on `--color-bg-subtle` headed You; the role's turn full width with the role's
     glyph, name and model; consecutive turns of a side grouped; an untrusted turn on the role's side with the label
     _From outside · not checked_ and its icon, plain text, never passed to the renderer; a turn from another device headed
     `You · iPad`; day lines; Copy on hover and focus; Try again and Edit the message for a turn not sent.
   - The working line: before the first word the role's line is the `WaitingMark` with `control.chat.working`; the steps the
     role reports are muted lines under it, folded into `3 steps` once the text arrives; the text grows in place with a soft
     caret (still under reduced motion); a turn announced again by `chat.turn.started` starts again from its working line;
     without pieces the working line stands until `chat.turn.completed`.
   - The transcript is `role="log"`; a turn being written is `aria-busy="true"` until it completes, so a screen reader reads the
     finished reply once; stick to the bottom while at the bottom, otherwise the pill _New reply_ (`web.chat.new_reply`); a page
     at a time without moving the turns in view. The `WaitingMark` is the screen's one waiting mark, and is its own status line;
     the chat adds no live region for it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Component tests per state (sending, streaming, complete, stopped, failed, not sent, untrusted, other device, working, steps
     folded, started again, no pieces) fail on a revert; one test asserts the transcript's live regions and `aria-busy`
     (evidence order: `test-first`).
4. **`Chat`: head, composer, empty chat, New chat**
 - What:
   - The head (the subject's mark and title, with the role and its model, the scope, the running state; _Hide the chat_ ghost
     with `Escape`; _New chat_ secondary, ending this chat and opening an empty one, asking first only while a proposal is open,
     which the proposal card of PLAN-136 supplies). `data-testid="chat"`; props `subject`, `scope`, `archive`, `opener`, `onhide`,
     `onnew`.
   - The composer: a text area that grows from one to eight lines, `Enter` sends, `Shift+Enter` adds a line, nothing is sent
     while a composition is open (`isComposing`); Send becomes _Stop the reply_ while the role answers and the field stays
     enabled with the focus; the counter from 7000 characters and Send disabled beyond 8000 with its reason; `#` offers the
     subject's tasks, files and decisions; an empty chat shows `control.chat.empty` and two or three starters as chips that fill
     the field and send nothing; the hint `Dictate with your system` (`web.voice.os_hint`, no code of the speech role); a
     snippet slot for the microphone that draws nothing. The focus moves into the field when the chat opens.
   - PLAN-147 residue (landing `landings/PLAN-147.md`): the composer is built on PLAN-147's `Compose`. Its name union lets
     `label=""` with no `ariaLabel` through and so yields an unnamed text area (`Compose.svelte:64-78`, `:196`); make an empty
     `label` count as absent (the field is then named by `ariaLabel`, which becomes required) and test it. `Compose` calls
     `onsend` with an empty or whitespace value whenever it is not `disabled` (`Compose.svelte:140-145`); state the contract
     (the consumer holds the act back with `disabled` and its reason) in `components.adoc` § `#compose` and pin it in a test.
   - Texts: every `web.chat.*` key of `chat.adoc` § `#texts` (not `web.voice.*`) in en and de.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `Chat.test.ts` covers the send, stop, composition, limit, counter, starter, `#`, hide, new, draft-survives-reload and focus
     rules and fails on a revert (evidence order: `test-first`).
   - `Compose.test.ts` fails for an unnamed field (`label=""`, no `ariaLabel`) and pins the empty-value `onsend` contract
     (evidence order: `test-first`).
   - A fixture of `Chat` and `ChatTurn` on `/dev/components` in both themes and both locales.
5. **The chat in the engine's side column, and the plan's and epic's chat acts**
 - What:
   - No side slot of this plan's own (D6 Q5: no interim slot). The plan's and the epic's `Chat` is passed to the column engine's
     side column of `PLAN-142`: the `TriagePage` that carries the plan and the epic routes (`PLAN-144` deliverable 4) receives the
     `Chat` as its `side` snippet, `sideOpen` from `chat=open`, and `onHideSide` from _Hide the chat_ and `Escape`, which replace the
     address without `chat` (`components.adoc#triage-page` :374). The width (`480`, or `400` where `480` would cost a column), the
     take-the-space rule and the phone (the chat takes the screen and the bottom bar) are the template's, measured on its
     container, never the viewport; this plan asserts them, it does not compute them. What stays the chat's own: the transcript keeps
     a third of the height with the on-screen keyboard open, and the pinned proposal folds into the composer's head
     (`chat.adoc#placement` :70).
   - Hiding keeps the chat running; the act that opened it carries a `CountBadge` of the replies that arrived meanwhile; the scroll
     position and the draft are kept per chat. Reloading, hiding, or opening on another device returns to the same chat.
   - Replace `ChatPanel` in `WorkControlPanel.svelte` (the plan's and the epic's chat acts); the other call sites are PLAN-137.
   - e2e `pm-web-e2e/tests/chat.spec.js` on the mock: open, stream, stop, hide and return, reload, second device through
     `/mock/broadcast`, in German and English, light and dark, at `1440`, `1024` and `390` px; the rail expanded and collapsed.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) shows this deliverable's change as specified on `running-work` → `plan fix-relay` with the
     chat open: the widths and the phone view, the working line, a streaming reply, Stop; baseline and after snapshots plus
     screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).
   - `chat.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; it waits on events, never on time (evidence order: `test-first`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (every rewrite reviewed and
  committed), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api,pm-mock-runtime verify`.
- Exactly one new dependency, `marked`, approved by the operator (`chat.adoc` § `#renderer-decision`); no other dependency or plugin,
  and no sanitizer. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`, `outerHTML`, `insertAdjacentHTML` or
  `{@html}` with model or runtime text (every model text through a text-treating sink), no timer waits in tests, the npm scripts
  `lint`, `format`, `test`, `test:ci-strict`, `quality` stay.
- No `style` attribute (CSP); no `data:` URL; the text guard baseline does not grow; every new text from the catalogue, en and de;
  the chat never borrows a `start.*` key.
- Frontend tests on Vitest with Testing Library; fixtures of hostile input stay in the test for the renderer.
- Ports: mock web port 21350 (`20000 + 10 × 135`), app port 21351 (`AGENTS.md` § Ports); never stop or reuse a process this
  worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has had one
  CodeRabbit review; counts stated from `ci pr comments`; the run merges itself once the gates hold (no further operator word); commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `ChatPanel.svelte` (324 lines) is used at `WorkControlPanel.svelte:1098,1116`, `DecisionView.svelte:1198`, `EpicView.svelte:973` and `StartView.svelte:1116` — read by `git grep -n ChatPanel -- pm-web-app/src`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ChatPanel.svelte is 324 lines; used at WorkControlPanel:1118,1136, DecisionView:1240, EpicView:984, StartView:1214 (lines drifted)
- OBSERVED: `package.json` has no `dependencies` block and no `marked` — read at `pm-web-app/package.json`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: package.json has only devDependencies (:30); no dependencies block and no marked
- OBSERVED: `KindMark`, `Link`, `ExternalLink`, `PrChip`, `Compose` and `FormStatus` do not exist in `pm-web-app/src/lib/components/` — absence read by `git ls-files | grep` for each name at `1cc0e10e`
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: KindMark, Link, ExternalLink, PrChip, Compose, FormStatus all exist in components/ at HEAD (PLAN-147 #420 dea405d8); RowLink too (#427) | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- OBSERVED: `ObjectPage.svelte` has no side slot and `TriagePage.svelte` still has the slots `filters`, `list` and `detail` — read at `pm-web-app/src/lib/templates/ObjectPage.svelte`, `TriagePage.svelte`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ObjectPage has no side slot; TriagePage still has slots filters/list/detail and props selected/hasFilters only
- OBSERVED: D6 fixed the side column on `TriagePage` and `WizardPage` (`side`, `sideOpen`, `onHideSide`, built by `PLAN-142`), none on `ObjectPage`, no side column in the chat plans, `480`/`400` measured on the container, the phone covering the bottom bar, and `open=` forms waiting for `PLAN-143` — read at `doc/design/web-app/components.adoc:361,374,381`, `doc/design/web-app/navigation.adoc:275-284,383-418`, `doc/design/web-app/chat.adoc:40,342` (`4a84f727`, D6 merged `8568b56e`)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: components.adoc:361,374,381 and navigation.adoc:275-284 carry the D6 side-column API on TriagePage/WizardPage, none on ObjectPage; chat.adoc:40
- OBSERVED: `adoptRouteFromLocation` reads no `chat` or `turn` query parameter — read at `pm-web-app/src/lib/stores/runtime.ts:940-966`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: adoptRouteFromLocation now runtime.ts:1058; no chat or turn query parameter in routes.ts or runtime.ts
- OBSERVED: `client.ts` holds the chat types at `:1565` and `:1572`, the entry chat at `:2328-2362`, the ended chat at `:2393-2417`, the plan, epic and decision chat at `:3355-3477`, and one shared `/api/v1/events` connection at `:3671` — read at `pm-web-app/src/lib/api/client.ts`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Facts hold, lines moved: ChatTurn/ChatProposal client.ts:1567/1574, normaliseChat :1656, entry chat :2478-2520, subject chats :3632-3790, shared events :4034
- OBSERVED: the web app listens for events outside the catalogue (`plan.changed`, `decision.created`) — read at `pm-web-app/src/lib/views/PlanView.svelte:124`, `DecisionView.svelte:322`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanView.svelte:126-132 listens plan.changed; DecisionView:330 decision.created; neither is in EventName
- OBSERVED: there is no Mermaid or diagram renderer for model text in `pm-web-app` — `git grep -i mermaid -- pm-web-app/src pm-web-app/package.json` is empty
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: git grep -i mermaid in pm-web-app/src and package.json is empty
- HYPOTHESIS: `client.ts` can open a plan's, an epic's and a workspace's event stream like the entry's (`subscribeStream` at `:3736`) — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `subscribeStream` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: subscribeStream (client.ts:4131) is path-generic, wrapped only for entry (:4099); mock serves plan/epic/entry/workspace /events (MockRuntimeEngine:6943-6958)
- HYPOTHESIS: the plan's and the epic's routes sit on `TriagePage` at the then-current HEAD (`PLAN-144` deliverable 4), so their `Chat` has a `side` to go into — confirm/refute at the route host of `/plans/{id}` and `/epics/{id}` and `pm-web-app/src/lib/templates/TriagePage.svelte` § props (verify-at-outline; refuted → stop and loop back, no interim slot)
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: No plan or epic route sits on TriagePage: only ReportsView uses it (:956); TriagePage has no side/sideOpen/onHideSide; PLAN-142/144 not landed | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- HYPOTHESIS: `marked` adds no transitive package and no peer conflict with Vite 8 and Svelte 5 — confirm/refute at `pm-web-app/package-lock.json` after `npm install marked` in the worktree (verify-at-outline)
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Needs npm install in a worktree; package.json and lock hold no marked, so peer and transitive impact cannot be read
- Verify-first clause: the D1 foundation components (O1) and the column engine's side props (`PLAN-142`) are read at the current HEAD before scoping; if `KindMark`, `Link`, `ExternalLink`, `PrChip`, `Compose` still do not exist, the operator's answer to O1 decides what this plan builds; if `TriagePage` has no `side`, `sideOpen`, `onHideSide`, stop and loop back.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural clause: D1 components now exist, but TriagePage lacks the side props so its loop-back trigger holds until PLAN-142 lands

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/chat/` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Chat.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Compose.svelte` — present at `dea405d8` (PLAN-147)
- OBSERVED: `pm-web-app/src/lib/components/Compose.test.ts` — present at `dea405d8` (PLAN-147)
- OBSERVED: `doc/design/web-app/components.adoc` — present at `dea405d8`
- OBSERVED: `pm-web-app/src/lib/components/ChatTurn.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ChatMarkdown.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` — present at `1cc0e10e`; deleted by PLAN-137
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at `1cc0e10e` (read only: its `side`, `sideOpen`, `onHideSide` are `PLAN-142`'s)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/package.json` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/package-lock.json` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at `1cc0e10e`; holds the `web.chat.*` keys
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/chat.spec.js` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/StartView.scenarios.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-130` (`WaitingMark`), `PLAN-132` and `PLAN-133` (contract and mock); `PLAN-45` landing; `PLAN-142` (the side column's `side`, `sideOpen`, `onHideSide` on `TriagePage`; D6) and `PLAN-144` deliverable 4 (the plan and epic routes on the engine); the operator's answers to O1 (D1 foundation components) and O6 (Mermaid). `PLAN-143` is soft: where it has landed its query vocabulary and writer carry `chat=`/`turn=`.
- Overlaps with:
  - `PLAN-45` (running): `client.ts`, `views/`, `routes/+layout.svelte`, `routes/dev/components/+page.svelte`; this plan follows it.
  - `PLAN-72` (parked): `package.json`, `package-lock.json`, `components/` and `views/`; it follows this plan.
  - `PLAN-52` (parked): `ChatPanel.svelte`, `OverviewView.svelte`; it follows PLAN-137.
- Adjacent to: the voice package (the composer's slot, `VoiceInput`, `web.voice.*`), `PLAN-55` and `PLAN-58` (they place the chat in the plan and epic pages when re-grounded).

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces` against everything that landed since; re-read the D4 and D5 chat documents (still `AGREED`); confirm the operator's answers to O1, O6, O7 (O2 is settled by D6, merged #403 `8568b56e`: the engine's side column of `PLAN-142`).

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- `PLAN-161` (chat bridge) first adapts the existing `client.ts` chat calls and the views to B1 (bodies with `client_turn_id`/`turn_id`,
  `ChatResource` reads through one normaliser). This plan replaces that adaptation and deletes it; the OBSERVED `client.ts` line refs are
  stale since `a139ec62` (ended-work calls already migrated there) — re-ground at outline.

## PLAN-161 Landing Residue (Amendment 2026-11-14)

Operator decision 2026-11-14 ("Fold into PLAN-135/138"). PLAN-161 (#429, `dc811fb3`) bridged the web chat to B1. Add to this plan:

- **Open a chat (new item for D1/D5).** The web only READS a chat (`client.ts:3631` `getChat` is GET). `POST …/chat` opens one
  (`ChatRoutes.chat()`, `web-app/chat.adoc:249`; `work-actions.adoc:384`: the runtime never opens a chat by itself) and is called for the
  entry only (`openEntryChat` `:2483`). Against the mock, every plan, epic, review or ended subject without a seeded chat answers 404 and the
  panel shows "chat closed" (4 running plans of `running-work`). The decomposition read-back (`StartView.svelte:851` `onChat`) sets
  `chatOpen` without opening a chat and sends turns to an unopened chat (`target_not_found`). Open the chat before the first turn on every
  subject; test on an UNSEEDED subject (unit, e2e, browser).
- **StartView's refresh machinery** (`refreshChat`/`chatGeneration`/`chatReadSeq`, `:225`, about 110 lines) has no test
  (`subscribeEntryEvents` is stubbed in `StartView.test.ts:20`, `.scenarios.test.ts:23`). A refresh started during a pending answer or send
  captures the bumped generation, and answered/closed results never bump `chatReadSeq`, so a late snapshot can bring back an answered
  proposal (CodeRabbit's last word on #429 thread `StartView:241`, unanswered). Replace or fix with tests of the event refresh, read order,
  entry change and that race.
- **Try again keeps the turn id:** today `client_turn_id` is reused only for one automatic retry inside one `chatTurn` call
  (`client.ts:3721`); the operator's Try again draws a new id and can double-send.
- This plan replaces the bridge (one normaliser `normaliseChat` `client.ts:1656`; the follow rule `:1710`, `:3763`). The OBSERVED `client.ts`
  line refs are stale. Empty-text `notice` turns would draw an empty line (PLAN-136).

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 2 contradicted: KindMark, Link, ExternalLink, PrChip, Compose, FormStatus all exist in components/ at HEAD (PLAN-147 #420 dea405d8); RowLink too (#427)
- Claim 10 contradicted: No plan or epic route sits on TriagePage: only ReportsView uses it (:956); TriagePage has no side/sideOpen/onHideSide; PLAN-142/144 not landed
- KindMark, Link, ExternalLink, PrChip, Compose, FormStatus and RowLink exist (#420, #427): D1 shrinks to what is missing. No chat place sits on a template yet (PLAN-142 not landed).

## Finding from `analyze-fix-build-time` (#432), 2026-11-14

- **`chat.ended` refreshes the entry (operator: "Refresh the entry").** `StartView` refreshes the entry on the entry stream's events
  only; `chat.turn.*` and `chat.proposal` go to the chat's own read and `chat.ended` refreshes nothing. On `chat.ended` re-read the entry
  (state and offered acts); a test drives the event and sees the re-read.
- `client.test.ts` lost its chat rows in #432's rebase: check that the dedicated chat tests cover the EPIC and the REJECT variants of the
  subject chat routes, and add what is missing.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-135-web-chat-core.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Mermaid (O6):** a fenced Mermaid block is shown as its source in a code block labelled `mermaid`; no renderer is added.
- **Voice (O7):** carved out; the composer keeps the system-dictation hint `web.voice.os_hint` and an empty microphone slot.
- **Side column (O2):** decided by the D6 design session (WS-06, staged 2026-11-10) together with the tab sets; this plan stays parked until those documents are `AGREED` and the D1 foundation part with KindMark, Link, ExternalLink, PrChip, Compose and FormStatus has landed (O1). *Settled 2026-11-11:* D6 merged (#403, `8568b56e`): no interim slot; the chat goes into `PLAN-142`'s `side` on `TriagePage` and `WizardPage`, `ObjectPage` has none; this plan waits for `PLAN-142` (deliverable 5 above is rewritten accordingly; the former "built against today's single object column" slot is removed).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: the D1 foundation part with KindMark, Link, ExternalLink, PrChip, Compose and FormStatus is PLAN-147; the side column is decided by PLAN-141 (D6).

## PLAN-134 Landing Residue (Amendment 2026-11-14)

PLAN-134 landed (#428, `7adb56c2`). The transcript now receives `notice` turns (cumulative lines,
possibly re-completed under the same turn id) and `chat.turn.started.context`. The core renderer must ignore or
render them without breaking, even if this plan lands before PLAN-136.

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-142` and `PLAN-144` D4 (plan and epic routes on the engine). Met: `PLAN-130`, `PLAN-132` to `PLAN-134`, `PLAN-147`, and O1/O2/O6/O7.
- Re-ground at outline: there is no chat place on `TriagePage` yet (claim 10).
- It carries residue from `PLAN-161`, the `#432` finding and `PLAN-134` (see the amendments above).
- The `marked` dependency is operator-approved.
