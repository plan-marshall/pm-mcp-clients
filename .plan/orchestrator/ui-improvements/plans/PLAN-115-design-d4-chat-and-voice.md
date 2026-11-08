# PLAN-115: Design D4: Chat and Voice (One Chat Component, Streaming Contract, `transcribe` Role)

epic: ui-improvements
workstream: WS-06

> Staged plan spec — one design session across three repositories, handed off as a direct Claude Code session (operator
> instruction 2026-11-06; not the runbook lane, not `/plan-marshall`).
> The DESIGN of this plan is written in the archived operator message named under Sources; this spec fixes the scope, the
> owned documents, the process and the done criteria. Where the two differ in detail, the message governs the design and
> this spec governs the process.

## Objective

Operator design review of 2026-11-06, session **D4 (chat and voice)** of `design-review-010.md`: one high-quality `Chat`
component for every place the operator talks to a role (plan, epic, decomposition, review, decision, start entry, ended
work read-only, project onboarding), with a streaming transcript, a safe markdown subset, the change-proposal card with a
diff, a composer, scroll and live behaviour, the chat as a side column, and voice input (OS dictation readiness plus the
app's own microphone through the new role `transcribe`). **Streaming chat and voice are both in the staging window
(operator, 2026-11-06)**, so D4 first records the roadmap entry and changes the requirement and the specification in
`plan-marshall-documentation` and `pm-mcp-core`, and only then writes `doc/design/web-app/chat.adoc` — a design never
contradicts the specification. The D4 contract + mock package, the web chat package and the voice package are staged after
this session's merge; PLAN-51, 52, 53, 55, 58, 59 and 65 adopt the chat after the web chat package.

## Sources (read in full before scoping)

All under the epic tree `inbox/archive/design-review/` (resolve the tree with `orchestrator resolve-path --slug
ui-improvements`; read, never write):

- `design-review-009.md` (the whole design: §1–§8, the contract / engine / daemon / mock part, the markdown-library
  research with its chosen option, the agreed documents, the suggested action)
- `design-review-003.md` §10 (the side column, written by D2) and `design-review-005.md` §6 (the waiting mark, owned by D3)
- `design-review-007.md` / `design-review-008.md` (the `Compose`, `ExternalLink`, chip look the chat uses; D1)
- `design-review-010.md` § Process, § Design sessions (row D4), § Operator decisions (D4 staging window, `marked`), §
  Per-plan instructions (chat adoption)

## Deliverables

1. **Roadmap, requirement and specification first** (`plan-marshall/plan-marshall-documentation`, checked with `doc-review`
   and `trace.py` there): the roadmap entry that places streaming chat and voice in the staging window (`project-setup.adoc`
   § Planning the Work); PM-WF-24 (`requirements/02-workflow.adoc:384`, "sent nowhere else" gains the speech exception:
   browser recognition or a speech provider receives the dictated audio by the operator's consent); the onboarding use
   cases; `specification/runtime-model/01-processes-and-transport.adoc` (202 reply, stop route, `/api/v1/transcriptions`);
   `runtime-model/06-event-catalogue.adoc` (`chat.*`); `runtime-model/05-mock-runtime.adoc`; `cli-and-security/03-web-server.adoc`
   (routes, CSP, `Permissions-Policy` for the microphone).
2. **`plan-marshall/pm-mcp-core`** — `state-catalogue.adoc#chat` (deltas, progress, stop; PM-WF-24) changes before `AGREED`,
   or `chat.adoc` marks those parts as pending; the engine capability `transcribe` (configuration, local-model check,
   provider, transcription job) is filed there as its own request, and the daemon routes / events / headers as a request to
   `plan-marshall/plan-marshall-mcp`.
3. **`doc/design/web-app/chat.adoc` (new)** — sections 1–8 of message 009, linked from `web-app/README.adoc` and
   `shared/work-actions.adoc`; the contract, engine, daemon and mock parts of the message as the design of the contract +
   mock package; the markdown-rendering decision (`marked` as lexer only, own Svelte token renderer; **the operator's
   approval to add `marked` to `pm-web-app` is recorded in `chat.adoc`**; syntax highlighting is a later, separate dependency
   decision). A clickable prototype on the realistic mock settles the design before `AGREED`.
4. **Other documents of this repository** — `shared/work-actions.adoc:350-432` and `shared/work-actions-chat-texts.adoc`
   (streaming, stopped, failed, the pinned proposal, the stop act, the starters, the texts); `shared/decisions.adoc` /
   `shared/decisions-texts.adoc` (the decision chat in the new component, texts unchanged); `shared/consents.adoc` (the speech
   consent); `web-app/components.adoc` (`Chat`, `ChatTurn`, `ProposalCard`, `VoiceInput`, the markdown renderer; after D1);
   `web-app/onboarding.adoc` and `web-app/machine-settings.adoc` (role `transcribe`; **after D3 and after the running
   PLAN-104**); the terminal's onboarding roles transcript and consent listing (both clients see the shared role and
   consent; the terminal records no audio) — in the terminal documents D4 names, none of which collides with D1's.
5. **Review and pull requests** — one pull request per repository, see Standing Constraints.

## Standing Constraints

- Design only in this repository. The contract + mock package (`pm-api` additions, events, stop route, text keys, the mock's
  realistic / deterministic modes and failure injection), the web chat package and the voice package are staged after this
  session's merge. A prototype is throw-away.
- Order: D4 comes after D1 (Compose, ExternalLink, chip look), D2 (§10 side column) and D3 (WaitingMark, and `onboarding.adoc`
  figures). The roadmap / requirement / specification changes come before `chat.adoc` is `AGREED`.
- Each session edits only the lines it owns.
- Every document is `Status: DRAFT` in the pull request; the operator sets `AGREED` as the last commit; each pull request is
  merged only on the operator's word. **No auto-merge**: the finalize stops once every pull request is green and its review
  comments are answered, reports, and waits.
- Review: `doc/design/` with `pm-documents:ref-documentation` (docs review) plus `pm-documents:ref-asciidoc` (`validate`,
  `verify-links`); `plan-marshall-documentation` and `pm-mcp-core` with `doc-review` and `trace.py` in those repositories.
- Open points go to `plan-marshall-documentation/doc/discussions/open-questions.adoc`. The operator's approval of `marked`
  is recorded in `chat.adoc`; no dependency is added in this session.
- The chat's untrusted turns stay plain text (never passed to the markdown renderer); the renderer produces Svelte nodes and
  never HTML strings (`CLAUDE.md` Web Standards: no `{@html}` / `innerHTML`).
- Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; pull request bodies carry no generator footer.

## Claim Labels

- OBSERVED: the sibling checkouts `../plan-marshall-documentation` and the engine repository are expected next to this one
  (`CLAUDE.md` § Project); `../plan-marshall-documentation` was listed at HEAD 60b4bfa5
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: ../plan-marshall-documentation and ../pm-mcp-core present next to the checkout
- HYPOTHESIS: PM-WF-24 is at `plan-marshall-documentation/doc/requirements/02-workflow.adoc:384` and says chat content is
  "sent nowhere else" — confirm/refute at that file § PM-WF-24 (verify-at-outline)
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: PM-WF-24 at 02-workflow.adoc:377, send-it-nowhere-else bullet at :384
- HYPOTHESIS: `ChatTurnResource` in `pm-api` has no `id`, `kind`, `status`, `format`, `refs[]` or `model` today (an asserted
  absence) — confirm/refute at `pm-api/src/main/java/**/ChatTurnResource.java` § record components (verify-at-outline)
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: ChatTurnResource.java:32 has only role, text, at, untrusted
- HYPOTHESIS: `marked` 18.1.3 is MIT with zero dependencies and `svelte-exmarkdown` renders raw HTML as literal text — taken
  from the review's npm research of 2026-11-06 — confirm/refute at the npm registry entries and, for the second claim,
  `svelte-exmarkdown` `Renderer.svelte` § `raw` (verify-at-outline; bundle sizes were not measured)
  - verdict: unverifiable | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: npm registry and svelte-exmarkdown not readable; pm-web-app/package.json has no marked
- Verify-first clause: re-read each cited range at the current HEAD of each repository before writing the rule it supports.
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: runtime-model 01/05/06 docs, 03-web-server.adoc, pm-mcp-core state-catalogue.adoc and open-questions.adoc exist

## Expected Surface

- OBSERVED: `doc/design/web-app/chat.adoc` — absent at HEAD 60b4bfa5; created by this plan
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 60b4bfa5 (after D1)
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 60b4bfa5 (link only)
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 60b4bfa5 (after D3 and PLAN-104)
- OBSERVED: `doc/design/web-app/machine-settings.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/work-actions.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/work-actions-chat-texts.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/decisions.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/consents.adoc` — present at HEAD 60b4bfa5
- HYPOTHESIS: the sibling-repository files named in deliverables 1 and 2 (`plan-marshall-documentation` requirements, runtime
  model and web-server specification; `pm-mcp-core` state catalogue) are outside this repository's checkout and are not read
  by this spec — confirm each exists at the start of the session (verify-at-outline)
- OBSERVED: `doc/design/shared/starting-work.adoc` — present (has a #chat section) at HEAD aa3a9be3

## Dependencies and Sequencing

- Depends on: `PLAN-112` (D1), `PLAN-113` (D2), `PLAN-114` (D3); `PLAN-104` for `onboarding.adoc`
- Blocks: the D4 contract + mock package, the D4 web chat package, the D4 voice package; the chat parts of PLAN-51, 52, 53,
  55, 58, 59, 65; PLAN-70 (chat in both themes), PLAN-72 / PLAN-73 (renderer guard, `control.chat.*`)
- Overlaps with: `PLAN-114` (D3: `onboarding.adoc`), `PLAN-113` (D2: §10)
- Adjacent to: none

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. `chat.adoc` is absent. Besides the listed files the deliverable also names the terminal onboarding-roles transcript and consent listing under `doc/design/terminal-client/` and `shared/starting-work.adoc`; `pm-web-app/package.json` has no `marked` yet (the lexer is approved by the operator, not yet a dependency; the npm registry could not be read). Files outside this repository are HYPOTHESIS until read.

## Operator Decision 2026-11-08 (the waiting mark)

The `WaitingMark` that D3 (PLAN-114) defines is the clipboard whose TODO lines are ticked one after another, independent of
the brand icon choice (the marshal star badge is the brand mark candidate). The chat's working line uses it as D3 defines
it; this plan neither redraws nor re-specifies it. Expected Surface unchanged.

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository, after PLAN-112, PLAN-113 and PLAN-114 have merged, and give it exactly this prompt (the orchestrator prints
the absolute spec path at emit time):

```text
Implement the staged design plan PLAN-115 (design D4: chat and voice) of the epic ui-improvements. Read the whole spec
first: <absolute path of this file>. Then read every source named under "Sources" in full. Follow the spec's Standing
Constraints exactly: first record the roadmap entry and change the requirement and specification in
plan-marshall-documentation and pm-mcp-core (each with its own review), then write chat.adoc and the other documents of this
repository, all Status: DRAFT, build the clickable prototype on the realistic mock before AGREED, ref-documentation docs
review plus ref-asciidoc validate and verify-links here, one pull request per repository, no auto-merge, stop when every
pull request is green and its review comments are answered, and report. Do not edit anything under .plan/orchestrator/ except
your own inbox message (orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only the design and specification documents this spec names (this repository and
the two sibling repositories). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR
and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
