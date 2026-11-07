envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T18:45:18Z
revision=9
amended=2026-11-06T20:06:29Z

# Chat: one high-quality conversation surface for every place the operator talks to a role

Sender: operator design review, 2026-11-06. An analysis of every chat surface in the web app (browser against the mock runtime, code, client contract, mock, agreed designs and use cases), with a visual and interaction design for one chat component, the contract and mock work it needs, and voice input for every text field.

## Operator feedback
- At several places the operator chats with the system. The quality bar is high, because people are used to modern chat interfaces.
- Analyse the current design and implementation, and create a high-end visual and interaction design.
- If the mock does not support it yet, that is part of the plan.
- Waiting indicators may carry the brand (the mascot, `design-review-005.md`).
- Side note: is there a way to add voice-to-text input using the operating system, for the chat and for every other text field? As in native chat apps, there are two microphones: the operating system's, and the app's own. The app's own needs a separate transcription role (model), chosen in the onboarding / configuration, including the browser's recognition with its privacy consequences shown.

## Current state

### Where the operator chats today
| Surface | Opens | Talks to | Implementation |
|---|---|---|---|
| Plan "Chat" | inline panel under the plan's act bar, pushing the plan down | `replan` | `ChatPanel` via `WorkControlPanel.svelte:1121-1160` |
| Epic "Chat" | the same | `decompose` | the same; the mock answers GET only |
| Ended plan/epic "Chat" | the same, read-only | `replan` / `decompose` | input and Send visible but **inert** (`onSend={() => {}}`), no transcript (`WorkControlPanel:1123-1139`) |
| Epic review chat | from `FindingsReviewModal` | `decompose` | ignores the item id (the design allows one item or the whole register) and never ends on the server (`EpicView:506`) |
| Decision "Chatte zuerst darüber" | `ChatPanel` inside the decision card, answers disabled while open | the asking task's role | not mocked at all (the e2e test expects "No chat is available") |
| Start entry | `ChatPanel` in a column beside the sources | `refine` / `decompose` | listens to `chat.turn` on the entry stream, an event that is not in the contract |
| Decomposition "Chat" | a button in `DecompositionReviewView:254` | (should be `decompose`) | no panel is rendered in that branch, so the button shows nothing (inferred) |
| Project onboarding | right drawer `ProjectOnboardingView:658-723` | the setup's role | a **second, different chat UI** (single line, no Enter, no Escape, no focus handling, placeholder only) |
| Instruct / own words | text areas | one-shot | not a chat (they stay one-shot) |

### Seen in the browser (plan chat)
- The panel opens inline and pushes the plan down. Its head is "fix-relay / replan" with a text "Schließen".
- Every turn is the same grey box, headed with the **raw role id** ("operator", "replan"). There is no "Du", no side, no avatar, no time.
- The input is a **single-line field without label**. The send button says **"Weiter"** (`start.sources.continue`).
- The pending change proposal (strikethrough old, new, "Verwirf die Änderung" / "Nimm es an" as plain text buttons) **disappeared after the next message**, undecided (the mock clears it on every turn).
- The mock answers **instantly with one fixed sentence**: "I have read the plan; say what should change, or ask on." There is no waiting state, no streaming and no stop.

### `ChatPanel` (shared, 324 lines)
- Plain text only (`pre-wrap`): no markdown, no code blocks, no streaming, no working line.
- `at` is never shown, and the `{#each}` has no key.
- No stick-to-bottom, no "new message" cue, no `role="log"` / `aria-live`. Focus does not move into the input on open, and the input loses focus while busy.
- The draft is cleared **before** the request, so a failed send loses the text. There is no retry, no stop and no edit.
- Enter sends without an IME/dictation composition guard. There is no multiline, no 8000-character limit (`state-catalogue.adoc`, `chat.turn` `text[1..8000]`) and no draft persistence.
- "Close" ends the chat and Escape only hides it, while the design calls the button "End the chat".
- The proposal card hard-codes the title "addition" and omits `summary` and `applies` ("applies after the current task").
- The designed text keys `control.chat.*` (`shared/work-actions-chat-texts.adoc`: "You", "{role} answers", "Your message", "Send", …) **do not exist**, and the panel reuses `start.*` keys.

### Client contract (`pm-api`)
- `ChatTurnResource` = `{role, text, at, untrusted}`: no id, no kind, no status, no format.
- A turn `POST` returns the whole read-back **after** the role has answered: no job id, no deltas, no stop route, no history paging.
- `EventName` has **no chat event**. Another device's turn is never seen live.
- The SSE stream (`client.ts:3663-3780`, reconnect and `Last-Event-ID`) and the per-task `activity/events` stream are a ready precedent for incremental text.

### Mock (`pm-mock-runtime`)
**Supported:**
- the plan chat: GET, turn (fixed reply, clears the proposal), accept / reject;
- the entry chat (a formulaic echo, one seeded proposal);
- the ended chat (one follow-up proposal).

**Missing:**
- latency and streaming;
- `chat.*` events;
- infeasible / lost / error replies;
- stop;
- markdown, code and references in replies;
- untrusted turns;
- multi-turn context;
- the decision chat (no route);
- the epic turn / accept / reject;
- `chat/end` for plan and epic (no route found);
- the project-onboarding chat step (no route found, unverified).

### Agreed rules this design keeps (`shared/work-actions.adoc:350-432`, `decisions.adoc:119-132`, `starting-work.adoc:126-149`, UC-CTL-1, `pm-mcp-core` PM-WF-24)
- **One dialogue wherever it is opened.** A plan talks to `replan`, an epic (incl. decomposition and review) to `decompose`, a start entry to `refine` / `decompose`, a decision to the role of the asking task, ended work read-only to `replan` / `decompose`.
- **Nothing changes without Accept.** A rejected change is gone. The chat never answers a decision: a proposed answer becomes own words and passes interpret + read-back. A running plan goes on while the chat runs, and a revision takes effect at the next task boundary.
- **Untrusted text** is shown as text, never rendered as markdown.
- The transcript stays with the subject, newest at the bottom, sent nowhere else.
- **Web placement:** "a panel beside the view it was opened from, the message field at its bottom; at phone width it takes the screen", (`shared/work-actions.adoc:382`), with its own scroll (`web-app/README.adoc:220`: a column with a live stream keeps its own scroll).
- **No `{@html}`** for model text. A sanitizer or markdown library is a dependency and needs the operator's decision (`CLAUDE.md`).

## The design

### 1. One component, `Chat`, everywhere
- Plan, epic, decomposition, review (item or whole register), decision, start entry, ended work (read-only) and the project-onboarding question all use the same `Chat`. The onboarding drawer and the inert ended-chat input go.
- "Ask the steward" stays a consent + model task, and the instruct / own-words fields stay one-shot (they use `Compose` of 008).

### 2. Placement
- **The chat column.** The chat opens as a column **beside** the object it belongs to, never inline above it:
  - it is a **side column** as defined in `design-review-003.md` §10: not a level, docked right of the object column it belongs to, in the URL as `?chat=open` (opened and hidden by `replaceState`);
  - width 400–480 px;
  - its own scroll.
- **Narrow screens.** When the window cannot show the object column and the chat side by side, the chat takes the space and the object becomes a hidden level in the gap strip (003 §10). On the phone it is full screen.
- **Decisions.** For a decision, the chat sits beside the decision (the decision's answers stay disabled while it is open, as agreed).
- **Head:**
  - `KindMark` + subject title, then "mit replan" (role) and the scope in the agreed words of `control.chat.scope` ("der ganze Plan", …; `shared/work-actions-chat-texts.adoc`);
  - the running state ("replan antwortet…" while working);
  - right: **"Blende den Chat aus"** (Escape: hides, the chat keeps running, and the "Chat" act carries a `CountBadge` of unread replies) and **"Beende den Chat"** (the agreed text; secondary, because ending drops an open proposal, `shared/work-actions.adoc:380`).

### 3. The transcript
- **Two sides.**
  - The operator's turns are right-aligned, on a subtle surface, headed "Du".
  - The role's turns are left-aligned, full-width text without a bubble (reading width), headed by the role's mark (a round role glyph, neutral) + role name ("replan") + its model in small muted text ("claude sonnet").
  - Consecutive turns of one side are grouped.
- **Time.** A day separator ("Heute", "Gestern", a date); the time on hover / focus and for screen readers.
- **The role's text** is rendered as a **safe markdown subset**:
  - paragraphs, **bold**, *italic*, `inline code`, blockquotes;
  - lists and tables (horizontal scroll when wide);
  - code blocks (mono, language label, copy button, horizontal scroll, no syntax colours in the first step);
  - links (only `http(s)`, as `ExternalLink` of 007); images are rendered as links, never loaded;
  - references to tasks, files (`path:line`), decisions and plans become reference chips: links in the chip look (006 `PrChip` style) that open their target.

  The renderer produces Svelte nodes, never HTML strings, so it needs no sanitizer. Raw HTML and anything outside the subset stay literal text. Parsing uses `marked` (lexer only), see "Markdown rendering: library research".
- **Untrusted turns** (from outside the machine) are plain text, never markdown, marked with a neutral "Von außen · ungeprüft" label and icon (not only a border).
- **The operator's text** is plain text.
- **Per turn on hover / focus:** copy; for the operator's last failed turn: retry and edit.
- **Working state.**
  - Before the first word, the role's line shows the **branded waiting mark** (`design-review-005.md`, now a defined surface) + "replan liest den Plan…".
  - **Progress steps** the role reports ("liest TASK-4", "prüft die Tests") appear as compact muted lines under it and fold into "3 Schritte" once the answer arrives.
  - While the answer streams, its text grows with a soft caret.
- **Notices in the transcript** are centred muted lines, not turns:
  - "Die Rolle kann das nicht ändern: <Grund>" (infeasible);
  - "Die Antwort ging verloren: <Grund> · Versuche es erneut" (lost);
  - "Angehalten" (stopped, the partial answer kept);
  - the agreed text of `decisions-texts.adoc:303` ("{id} wurde über {channel} ({device}) beantwortet; der Chat ist beendet.") when a decision was answered elsewhere;
  - the agreed "{subject} geändert" / "abgelehnt: nichts geändert" (`shared/work-actions-chat-texts.adoc`) with the time, after a proposal.

### 4. The change proposal
- **A distinct card in the transcript**, at the turn that proposed it, with:
  - the title from the proposal kind ("Änderung der Beschreibung", "Neuer Task", "Antwortvorschlag", "Folgeplan"; no hard-coded "addition");
  - the `summary`;
  - a **diff** (removed lines with "−" and added lines with "+", on the dedicated diff tokens `--color-diff-removed` / `--color-diff-added`: an explicit, documented exception to 006 §3, valid only inside a proposal diff; word-level highlights; collapsed beyond 12 lines);
  - the `applies` line ("gilt nach dem laufenden Task").
- **Acts:** the agreed texts: primary "Nimm sie an", secondary "Lehne sie ab". For a decision: "Antworte mit diesen Worten" (`decision.chat.use_words`). For ended work: "Starte damit einen Plan".
- **A pending proposal never disappears.** While it is open, a compact copy is **pinned above the composer** ("1 offener Vorschlag · Ansehen"), and the operator can keep talking. A new proposal replaces the open one only with a notice ("Der vorige Vorschlag wurde ersetzt"). After Accept or Reject, the card collapses into its notice line.

### 5. The composer
- An **auto-growing text area** (1–8 lines, then scroll), with the visible label "Deine Nachricht" for screen readers and a placeholder with a scope-specific example.
- **Enter sends, Shift+Enter adds a line.** No send while an IME or dictation composition is active (`isComposing`).
- **Send** is a secondary `Button` with the arrow icon and the agreed label "Sende sie" (`control.chat.send`; visually hidden next to the icon, 007 §1). While the role answers, it becomes **Stop** (square), which stops the reply and keeps the partial text.
- **The draft** is kept per chat (`localStorage`) until it is sent successfully. A failed send keeps it in the field and marks the turn "Nicht gesendet · Versuche es erneut".
- **Optimistic echo:** the operator's turn appears at once ("wird gesendet…") and is confirmed by the server.
- A counter appears from 7000 characters (limit 8000).
- **References:** typing `#` offers the subject's tasks, files and decisions and inserts them as reference chips (sent as plain text ids, rendered as chips).
- **Empty chat:**
  - a short "replan kennt" line (description, 17 tasks, stand: execute);
  - 2–3 **suggested starters** for the scope as chips ("Was ist das Risiko von TASK-4?", "Schlage einen zusätzlichen Test vor"), from the catalogue;
  - one tap fills the composer and does not send.
- **Ended work:** no composer, only the transcript and the follow-up proposal.

### 6. Scroll and live behaviour
- **Scrolling:** stick to the bottom while the operator is at the bottom. If they have scrolled up, a "↓ Neue Antwort" pill appears instead of jumping. The scroll position is kept per chat while hidden.
- **Other devices:** turns, proposals and the end of the chat from another device arrive live (the events of 9) and appear in place.
- **Accessibility:**
  - the transcript is `role="log"`, and the **completed** reply is announced politely (not every streamed word);
  - focus moves into the composer on open and stays there while the role answers (the composer is not disabled, only Send becomes Stop);
  - Escape hides, and focus returns to the "Chat" act;
  - `c` opens the chat where PLAN-51 defines it;
  - proposals are announced ("Vorschlag: Änderung der Beschreibung").

### 7. Waiting and the brand
- **Short waits** (a button, under about 1–2 s) keep the neutral `Spinner`.
- **The chat's working line** uses the **waiting mark** defined in `design-review-005.md` §6 (owned there).
- **The mascot is not the chat partner:** roles keep their neutral role glyphs, so the operator never mistakes the mascot for "the AI".

### 8. Voice input (chat and every text field)
There are two microphones, as in native chat apps.

**1. The operating system's dictation (always there, no app code)**
- It works in every native text field:
  - the microphone on the phone's keyboard (iOS / Android);
  - macOS Dictation (press Fn twice or the microphone key; on-device on Apple silicon);
  - Windows voice typing (Win+H).
- The app cannot start it, and it does not need to. Every field is made dictation-ready:
  - only native `<input>` / `<textarea>` (008);
  - no key handler that swallows composition or `insertText` input;
  - Enter-to-send guarded by `isComposing`;
  - `spellcheck` / `autocapitalize` on for prose (off for secrets, codes and ids).
- A per-platform hint ("Diktieren: Fn Fn · Win+H") in the composer and the help makes it discoverable.

**2. The app's own microphone button (the transcription role)**
- A microphone `IconButton` in the composer, and in prose text areas (reasons, descriptions, own words). It is shown only when a transcription method is configured and available in this browser.
- **Press to talk:**
  - the field shows a level meter, the elapsed time and **Stop / Cancel**;
  - the transcript is inserted at the cursor as normal, editable text and is **never sent automatically**;
  - Escape cancels;
  - a failed transcription keeps the recording until retried or discarded.
- **How it transcribes** is the new role **`transcribe`** (speech-to-text), chosen in the machine onboarding (roles step) and changeable in the machine settings. A project may narrow it, never widen it. The choices, each with its privacy line shown at the choice:

| Method | Where the audio goes | Availability |
|---|---|---|
| **Off** (default) | nowhere; OS dictation only | always |
| **Browser recognition** (Web Speech API) | Chrome / Edge: to the browser vendor's cloud service (Google / Microsoft) by default; Safari: Apple's speech service or on device; Firefox: not supported | per browser; the button appears only where the browser supports it |
| **Local model on the machine** (e.g. a Whisper-class model run by the daemon) | stays on the machine; the browser sends the recording to the daemon over the paired connection | needs the model installed; the onboarding checks it like a harness |
| **Speech provider** (a cloud speech-to-text service with its own credential) | to that provider | needs a provider + token (the provider step of the onboarding) |

- The choice is a **consent with its consequence** ("Die Aufnahme geht an Google, um sie in Text umzuwandeln"), remembered like the other consents (`shared/consents.adoc`) and withdrawable in the machine settings.
- **Which setting wins:** the machine setting decides which methods are **allowed**. Each browser then chooses among the allowed ones ("Dieser Browser", PLAN-85, staged); browser recognition is only offered there if the machine allows it. A project may narrow the machine's allowed methods, never widen them.
- **Both clients:** the role `transcribe` and the speech consent are shared states (`web-app/onboarding.adoc:12`, `shared/consents.adoc`). The terminal's onboarding roles transcript lists the role as "set in the web app" and its consent listing shows and withdraws the speech consent; the terminal records no audio.
- Recordings are not stored after transcription. The transcript is ordinary operator text and is not marked untrusted.
- The microphone permission is asked by the browser on first use. If it is denied, the button explains how to allow it.
- **Not the planning models.** The planning roles' harnesses (Claude Code, OpenCode) and their models are not used for transcription. `transcribe` is a separate role with its own model or provider, so a privacy choice for speech never changes how plans are worked.

## Contract, engine and mock (part of the plan)
**Client contract (`pm-api`, this repository):**
- `ChatTurnResource` gains:
  - `id`;
  - `kind` (`message | proposal | progress | notice`);
  - `status` (`sending | streaming | complete | stopped | failed`);
  - `format` (`plain | markdown`; untrusted is always `plain`);
  - optional `refs[]` (task / file / decision ids) and `model`.
- A turn `POST` returns at once with the operator turn's id (202). The reply comes through events.
- New `EventName`s: `chat.turn.started`, `chat.turn.delta` (`{chat_id, turn_id, text}`), `chat.turn.progress`, `chat.turn.completed`, `chat.proposal`, `chat.ended`.
- `POST …/chat/turns/{id}/stop`. History paging for long transcripts.
- Transcription: `POST /api/v1/transcriptions` (audio from the browser, for the methods *local model* and *speech provider*) → `{text}`, with the error codes of PLAN-45. The machine's configured method and availability are part of the machine settings read model, so the client knows whether to show the button. The role `transcribe` joins the roles of the onboarding.
- The shared text keys `control.chat.*` of `work-actions-chat-texts.adoc` are created (de/en), plus the new ones of this design.

**Engine (`plan-marshall/pm-mcp-core`, outside this repository):** `chat.reply` (PM-WF-24) must emit deltas, progress and stop. The `transcribe` role (its configuration, the local-model check, the provider, the transcription job) is a new engine capability. This is filed there as its own request.

**Daemon (`plan-marshall/plan-marshall-mcp`, outside this repository):** `pm-mcpd` serves the 202 turn POST, the stop route, the `chat.*` events and `POST /api/v1/transcriptions`, and its `Permissions-Policy` must allow the microphone for the app's own origin. This is filed there as its own request, beside the one for `pm-mcp-core`.

**Requirements and specification (`plan-marshall/plan-marshall-documentation`):** streaming chat and voice input are new runtime surface. The operator has placed **both in the staging window** (2026-11-06); **D4 records that first** in the roadmap (`project-setup.adoc` § Planning the Work). Before `chat.adoc` is `AGREED`, these change: the chat requirement PM-WF-24 (`requirements/02-workflow.adoc:384`, "sent nowhere else" gains the speech exception: browser recognition or a speech provider receives the dictated audio, by the operator's consent), the onboarding use cases, the local API (`specification/runtime-model/01-processes-and-transport.adoc`: 202 reply, stop route, `/api/v1/transcriptions`), the event catalogue (`runtime-model/06-event-catalogue.adoc`: `chat.*`), the mock runtime specification (`runtime-model/05-mock-runtime.adoc`), and the daemon (`cli-and-security/03-web-server.adoc`: routes, CSP, `Permissions-Policy` for the microphone). A design never contradicts the specification. The `pm-mcp-core` state catalogue (`state-catalogue.adoc#chat`: deltas, progress, stop) changes before `AGREED`, or `chat.adoc` marks those parts as pending that change; until it lands, the web shows the working line and the whole reply on `chat.turn.completed`.

**Mock (`pm-mock-runtime`):**
- **Every route:** plan, epic, decision, entry, ended, review (item and register), decomposition, project onboarding; turn, accept, reject, end, stop.
- **A realistic mode** for demos: a latency before the first token, streaming deltas over SSE at a reading pace, progress steps, scripted multi-turn conversations per scenario (markdown with lists, code blocks and task / file references, a proposal in a later turn, a decision answer proposal), untrusted turns, and a second-device turn via `/mock/broadcast`.
- **Failure injection** per scenario or mock control: infeasible, lost, HTTP error, slow, stop.
- **Transcription:** the `transcribe` role in onboarding and settings (all four methods, a local model found / not found), and `POST /api/v1/transcriptions` returning a scripted text after a delay, plus a failure.
- **A deterministic mode** for tests: replies come as events in order, without timers, so e2e tests wait on the events (no timer waits, as the web standards require).

## Markdown rendering: library research (npm registry, 2026-11-06)
Requirement: render the role's markdown into Svelte nodes (no `{@html}`, `CLAUDE.md`), raw HTML never becomes markup, links restricted to `http(s)`, a license fit for a proprietary product (MIT / BSD / ISC / Apache-2.0; no copyleft beyond file-level MPL).

| Option | License (incl. transitive) | Size | Fit |
|---|---|---|---|
| **A. `marked` (lexer only) + the app's own Svelte token renderer** | `marked` 18.1.3 MIT, **0 dependencies** | ≈ 0.56 MB unpacked (bundle unmeasured) | Full control, smallest supply chain. The app writes the renderer for the subset (paragraph, emphasis, code, code block, list, link, blockquote, table, `html` token → literal text). GFM is built in. |
| **B. `svelte-exmarkdown`** 5.0.2 (Svelte ^5.1.3, unified / remark / rehype) | MIT; 95 transitive packages: MIT 92, Apache-2.0 2, ISC 1 | ≈ 8.5 MB installed incl. Svelte (bundle unmeasured) | **No `{@html}`/`innerHTML` in the package.** Raw HTML is rendered as literal text (`Renderer.svelte`: `raw` → `{astNode.value}`). It spreads element properties and has no URL filter, so `a` and `img` must be overridden with own components (protocol allowlist; images dropped or shown as links). Component overrides exist. GFM via `svelte-exmarkdown/gfm`. Last published 2025-08. |
| C. `@humanspeak/svelte-markdown` 2.2.0 (marked + `htmlparser2`) | MIT | ≈ 0.74 MB + peers | Renders raw HTML in markdown as real elements, and its highlight / KaTeX / Mermaid extensions use `{@html}`. **Not suitable** without forking. |
| D. `marked` / `markdown-it` (MIT) → HTML + `DOMPurify` 3.4.17 (MPL-2.0 OR Apache-2.0) | permissive (take DOMPurify under Apache-2.0) | small | Needs `{@html}`: **against the repository rule**. Not suitable. |
| (old) `svelte-markdown` 0.4.1 | MIT | — | Svelte 4 only. Not suitable. |

**Syntax highlighting (later step, optional):**
- `shiki` 4.6.0, MIT (43 transitive packages, MIT / ISC). Its `codeToTokens` API returns tokens that the code block renders as `<span>`s, so no `{@html}` is needed. The JavaScript regex engine avoids WASM, and languages load on demand.
- Alternative: `prismjs` 1.30.0, MIT, 0 dependencies, `Prism.tokenize`.
- `highlight.js` (BSD-3-Clause) produces HTML strings, so it is not suitable.

**Chosen: A.** `marked` (MIT, no transitive dependencies) is used as the lexer only. The app's own Svelte renderer turns its tokens into nodes for the subset of section 3:
- an `html` token is rendered as literal text;
- links are restricted to `http(s)` and rendered with `ExternalLink`;
- images are rendered as links;
- task, file and decision references become reference chips: `Link`s in the chip look (like `PrChip`), never copy `Chip`s.

The renderer is tested with its own fixtures, including hostile input (`javascript:` links, raw `<script>`, nested HTML). Untrusted turns are never passed to it. **The operator has approved adding `marked` to `pm-web-app`** (`CLAUDE.md`: a dependency needs the operator's word). Syntax highlighting is a later, separate dependency decision (`shiki` `codeToTokens` or `prismjs`). Bundle sizes were not measured.

## Agreed documents that change
- New `doc/design/web-app/chat.adoc` (sections 1–8), linked from `web-app/README.adoc` and `shared/work-actions.adoc`.
- `shared/work-actions.adoc:350-432` and `shared/work-actions-chat-texts.adoc`: the new states (streaming, stopped, failed), the pinned proposal, the stop act, the starters, the texts.
- `web-app/components.adoc`: `Chat`, `ChatTurn`, `ProposalCard`, `VoiceInput` (the microphone button), the markdown subset renderer. `Chat` uses `Compose` of 008. The waiting mark is owned by 005.
- `web-app/onboarding.adoc` / `machine-settings.adoc` and the use cases of onboarding (`plan-marshall-documentation`): the role `transcribe` with its four methods and consents.
- `shared/decisions.adoc` / `shared/decisions-texts.adoc`: the decision chat in the new component (texts unchanged).
- `shared/consents.adoc`: the speech consent of the `transcribe` methods.
- `web-app/navigation.adoc` §10 (written by D2; D4 reads it): the chat as a side column.
- `pm-mcp-core` specification (PM-WF-24, `state-catalogue.adoc` `chat.*`): deltas, progress, stop (request filed there).

## Affected plans
- **No staged plan owns the chat.** PLAN-51 (decision chat guards, `c`), PLAN-52 (ended-work "Chat about it", held per 003), PLAN-65 (verify the read-only chat) and PLAN-53 (start entry) use it and take the new component when they run.
- **PLAN-55 / PLAN-58 (held per 003):** the chat column instead of the inline panel.
- **PLAN-59 (held per 003) / the project onboarding:** the drawer is replaced by `Chat`.
- **PLAN-45:** chat errors use the shared `displayError` codes (PLAN-09), as PLAN-45 does.
- **PLAN-72 / PLAN-73:** no `{@html}` in the renderer (guard), and `control.chat.*` instead of reused `start.*` keys.
- **PLAN-70:** the chat in both themes.
- **The D4 voice package** (not PLAN-60 / PLAN-64/66 / PLAN-85): the role `transcribe` in the onboarding's roles step and the machine settings, the speech-consent withdrawal, the per-browser choice ("Dieser Browser"), the microphone button, contract + mock.

## Suggested action (the epic decides)
1. **Design:** `chat.adoc` and the document changes above, then operator review to `AGREED`, with a clickable prototype on the realistic mock.
2. **Contract + mock package** (this repository): the `pm-api` additions, the events, the stop route, the text keys, and the mock's routes, realistic and deterministic modes and failure injection. In parallel, the request to `pm-mcp-core` for streaming in `chat.reply`.
3. **Web package:**
   - `Chat` with transcript, markdown subset renderer, proposal card, composer, scroll and live behaviour, waiting mark;
   - adopt it on the surfaces no staged plan owns (decomposition, epic review, start entry, project onboarding); PLAN-51, 52, 55, 58 and 59 adopt it in their own views after this package (`design-review-010.md`);
   - remove the onboarding drawer and the inert ended input.
4. **Voice:** the dictation-ready rules land with the field system of 008. The `transcribe` role and the microphone button form their own package after the chat package: onboarding / settings choice with consents, the per-browser choice, the button in the composer and prose fields, contract + mock. The engine part is filed in `pm-mcp-core`.

## Process
The design of this message is produced **before any implementation**, in its own Claude Code session: design session **D4 (chat and voice)** of `design-review-010.md`.
- That session sets the documents it creates or changes to `Status: DRAFT`, reviews them (`doc/design/`: `ref-documentation` docs review, `ref-asciidoc` validate and verify-links; `plan-marshall-documentation` and `pm-mcp-core`: `doc-review` and `trace.py` in those repositories) and opens a pull request per repository.
- The operator sets them to `AGREED` as its last commit, and the pull request is merged on the operator's word.
- Only after that merge do the implementation plans named above start or restart.

Order, document ownership, open points, what each plan waits for, and the merged per-plan instructions are in `design-review-010.md`.
