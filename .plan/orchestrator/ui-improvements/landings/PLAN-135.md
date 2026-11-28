# PLAN-135 landing record

epic: ui-improvements
plan: rb-135-web-chat-core (runbook lane, harness `antigravity`, model and effort `unknown`; `total_tokens` `unknown`; `plugin_version=0.1.0` is the antigravity placeholder, the installed version is 0.1.2183; executor 0.1.2183)
pr: #598, squash `bdaf6f26` (34 files, +7028/−79) on `8358675d`, merged 2026-11-28T11:09:08Z (merge group run 46137467204). Head `c6861895`, the same tree as the squash.
analysed: 2026-11-28, by sub-agent in three layers, by reading only (no build). L3-1 was re-read by the orchestrator (`Chat.svelte:266-281`, `ChatTranscript.svelte:195-197`).

Message `rb-135-web-chat-core-001` (2026-11-28T11:10:51Z):
- `merge_state=merged`, `deliverables_done=4` of 5; D5 moved to PLAN-192 by the Split of 2026-11-25.
- 0 unresolved threads; 70 of 70 review-body findings handled (R1 32 + R2 23 + R3 15, of which 1 was declined and 1 deferred).
- `sonar_open_new_issues=0`, `self-review: clear`.

**Sonar.**
- PR 598: 0 issues, gate OK, coverage on new code 91.9 %.
- Default branch, read 2026-11-28 after the `main` task `AaIh6Om0wnWyEqGvGte4` (11:27:22Z, after this merge): gate OK, coverage on new code 90.3 %, `count_status: confirmed`, **3 open**. They are the three PLAN-181 keys.

**XSS: clean.**
- `markdown.ts:397-418` uses `marked.lexer` only: no `parse`, no renderer, no `marked.use`.
- `ChatMarkdown.svelte` walks the tokens into Svelte elements with `{…}` text sinks.
- Raw HTML tokens and anything outside the subset render as their raw text.
- Links: `parseSafeWebUrl` admits `https?://` only and refuses C0 control characters. `ExternalLink` renders `rel="noopener noreferrer"`. `javascript:`, `data:` and `mailto:` addresses render as text.
- An image becomes a link, never an `<img>`.
- No `{@html}`, `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `DOMParser` or `srcdoc` anywhere in the chat files.

**Dependency.** `marked@^18.3.0` (MIT), the one the operator approved. The lock adds exactly one package, with no transitive packages.

## Layer 1: specified (the Re-Grounding of 2026-11-26 binds)

| Item | Verdict |
|---|---|
| D1 `lib/chat/` beside the bridge; stream before read; deltas in order; a reset re-reads | met (`store.ts:568-617,811-824`; `store.test.ts:54,184`) |
| D1 open before the first turn on an unseeded subject | met (`ensureChatOpen` `:619`, auto-open on 404 `:732-734`; test `:227`) |
| D1 Try again keeps `client_turn_id`; a failed send keeps the draft | met (`retryTurn` `:959-972`; test `:253`) |
| D1 setter in `runtime.ts`, no push | met: `setChatOpen`/`setChatTurn` `runtime.ts:2208-2236` replace the entry (`runtime.test.ts:1939-1968`). Nothing uses them outside tests yet |
| D1 `chat.ended` re-reads the entry | met (`StartView.svelte:352-354`; the pinned "no re-read" row is flipped) |
| D2 lexer-only renderer, closed subset, Mermaid behind one function, hostile fixtures | met (`markdown.ts:276,359-418`; `ChatMarkdown.test.ts:63-92,94,109,221`) |
| D2 `{@html}` guard as a Vitest source scan | met, narrow. `ChatMarkdown.test.ts:10-24` scans `ChatMarkdown.svelte` only, for `{@html` and `innerHTML`, as the spec named. The mutation run cannot be verified: the log went with the removed worktree, and test and code share commit `187b2ee3` |
| D2 Mermaid design sentence | partly. The Re-Grounding asked to *add* the interim sentence; `chat.adoc:136` and `components-chat.adoc:76` *replaced* the design target ("drawn as every diagram") instead (L3-8) |
| D3 per-state tests, `role=log`, `aria-busy`, positioned box | met (`ChatTurn.test.ts:39-308`) |
| D3 "reads the finished reply once" | **not met** (L3-1) |
| D3 "a turn from another device: `You · iPad`" | partly: tested only through a `deviceName` prop. The wire `ChatTurn` has no device (L3-2) |
| D4 head, composer, starters, `#`, hide, new, draft across reload, focus | met (`Chat.test.ts:48-429`) |
| D4 Compose `label=''` without `ariaLabel`; the empty-value `onsend` contract | met. A type guard and `@ts-expect-error`, caught by Maven's `svelte-check`; the sentence is at `components-fields.adoc:140` |
| D4 `web.chat.*` keys in en and de; fixture on `/dev/components` | met. Full en/de parity; `ChatFixture.svelte`. No screenshot was required |
| D5 | moved to PLAN-192; correctly not done |
| Test-first evidence | cannot be verified: every red log named lived in the removed worktree, and each deliverable's test and code share one commit |

**True count:** 4 of 4 in scope. D3 is weaker than "met".

## Layer 2: implemented

34 files.

Outside the Expected Surface:
- `ChatTranscript.svelte` (new; it holds the log and the live region) and `dev/ChatFixture.svelte` (new).
- `vocabulary.test.ts`: `Chat`, `ChatTurn` and `ChatMarkdown` leave `PENDING_LIST`.
- `api/client.ts` changed beyond the chat:
  - `subscribePathEvents` is new;
  - `subscribeEvents` now wraps each callback, which ends its Set de-duplication;
  - `clearTimeout(deadline)` after `fetch` (L3-11).
- `StartView.svelte`: act-path read ordering `:467-478`, two new race tests, and about 40 lines of prettier churn.

In the surface but untouched: `lib/routes.ts`, `StartView.scenarios.test.ts`.

**Stale or false claims:**
1. `plugin_version=0.1.0` (the antigravity placeholder).
2. "70/70 handled" counts one decline (`inline-18`) and one deferral (`inline-25`).
3. "full verify 5741 tests" cannot be checked: the log is gone.
4. The `inline-18` rationale cites `streamState.test.ts:131`, which tests a connect that never answers, not heartbeats. CodeRabbit accepted the decline anyway.
5. "D2 test-first and mutation-after" cannot be verified.

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | **major** | The completed reply's full text, as raw markdown source, goes into a second polite `aria-live` region beside the `role="log"`. A screen reader reads it twice, against D3 and `chat.adoc:191` ("once"). `previousCompletedId` starts empty, so opening a chat also announces the last historic reply; `Chat.test.ts:353` pins that | `Chat.svelte:266-281`, `ChatTranscript.svelte:195-197` |
| L3-2 | minor | "Another device" is not real: the contract has no device per turn, and `localDevice` would label every own turn `You · X` | `ChatTranscript.svelte:233`; `pm-api …/chat/ChatTurn.java:46-59` |
| L3-3 | minor | After a failed send the text goes back to the draft *and* a Not-sent turn stays. Send on the restored draft draws a new `client_turn_id`, a double send after a lost answer; only Try again is idempotent | `store.ts:774-793,925-957` |
| L3-4 | minor | Try again on an ended or replaced chat opens a new chat, and `applySnapshot` drops the Not-sent turn; on failure its text is lost | `store.ts:619-633,971` |
| L3-5 | minor | More than 500 events before the first read settles are dropped silently, with no forced re-read | `store.ts:580-588` |
| L3-6 | minor | `openNew` race: events of the new chat that arrive before `applySnapshot(opened)` fail the `chat_id` filter | `store.ts:383-385,862-869` |
| L3-7 | minor | `Chat` binds its store once (`untrack`) and never follows a `subject` change. Hosts must `{#key}` it | `Chat.svelte:45-46` |
| L3-8 | minor | Design rows overclaim: "render inside the landed side column", "adopt the chat on the decomposition…", while `Chat` sits only on `/dev/components`. The Mermaid target sentence was replaced | `chat.adoc:136,342`, `components-chat.adoc:76`, `navigation.adoc:415` |
| L3-9 | minor | Every delta re-lexes the whole reply: O(n²) on long replies | `ChatMarkdown.svelte:28`, `markdown.ts:397-418` |
| L3-10 | minor | `aria-busy` sits on the whole log; the role glyph is the author's first letter; the English literal `'planner'` is a fallback (text-guard risk) | `ChatTranscript.svelte:216`, `ChatTurn.svelte:143-145`, `Chat.svelte:302,368` |
| L3-11 | info (positive) | `clearTimeout(deadline)` after `fetch` fixes a bug present since #274: every SSE stream, the shared `/api/v1/events` included, was aborted 10 s after connecting. A half-open connection now hangs undetected, because there is no heartbeat or idle watchdog in the contract | `api/client.ts:5233` |
| L3-12 | info | `key in REASON_KEYS`/`FINDING_KEYS` lets prototype keys through; use `Object.hasOwn` | `messages.ts:42-46` |
| L3-13 | info | Auto-open on a 404 at mount is consistent with the design only while `Chat` mounts solely from the act Chat | `store.ts:732-734` |
| L3-14 | info | Every production place still uses the old `ChatPanel`, whose send draws `randomUUID` each time, so a double send stays possible there until PLAN-137/192/193 | `api/client.ts` `sendChatTurn` |

Clean: no unhandled rejection, no stream leak, the Stop snapshot merge, the `chat.ended` filter, en/de parity.

## Residue

**Shared code files:**
- PLAN-62 (running): `runtime.ts` and its test (2).
- PLAN-199 (launched): `common.{en,de}`, `api/client.ts` and its test (4).
- PLAN-198 (launched): `vocabulary.test.ts` (1). `Chat`, `ChatTurn` and `ChatMarkdown` have left `PENDING_LIST`.
- PLAN-138: none.

**PLAN-136:** its gate "after PLAN-135" is formally open. Before an emit it needs a re-grounding:
- Stale claims:
  - `chat.spec.js` "created by PLAN-135" (it is PLAN-192's);
  - "through PLAN-135, PLAN-142 side column" (`Chat` sits in no side column).
- Already done:
  - D3's folds, chips and the knows line (`ChatTurn.svelte:82-138`, `Chat.svelte:144-150,343`);
  - D1's store-side accept and reject by `turn_id`;
  - the New-chat confirmation while a proposal is open (`Chat.svelte:245-258`).
- Conflicts and gaps:
  - D2's "notices never a turn" conflicts with notices rendered as turns;
  - "announced once" must deal with L3-1;
  - its browser check on the plan page has no `Chat` host until PLAN-137 or PLAN-192.

**Other followers:**
- PLAN-137: not open (it depends on PLAN-136). Its `StartView` lines moved (`refreshChat` `:371`, `onChat` `:1170`). `setChatOpen` exists, and hosts must `{#key}` `Chat`.
- PLAN-192: the PLAN-135 dependency is met. It is still parked on the side-column design.
- PLAN-193: follows PLAN-137 and PLAN-192.

**Routes:**
- A device field per turn: route it to the contract owner, or drop the D3 promise.
- A stream heartbeat or idle watchdog belongs in the transport specification.
- The doc overclaims (L3-8) want correcting.
