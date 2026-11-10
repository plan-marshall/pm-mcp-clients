# WS-07: Chat

epic: ui-improvements

> Charter document for one workstream — a coherent slice of the epic with its own goal
> and surface. Lives at `workstreams/WS-07-chat.md` and is tracked in the
> `workstreams[]` field of the epic header, `status.json`. See
> `persona-plan-orchestrator/standards/orchestration-model.md` for the tier contract.

## Charter

One feature across four layers: the chat with the planning role, as designed by D4 (PLAN-115) and re-scoped by D5
(PLAN-125), both `AGREED`. The workstream holds the client contract, the mock runtime that serves it, the web chat and the
terminal chat, so that the rule "contract and mock first, then each client" and the shared chat texts live in one place.
Staged 2026-11-10 on the operator's decision (stage the D4/D5 client packages) from drafts grounded at `1cc0e10e`.

## Scope

- In scope: the `pm-api` chat model, events and texts; the chat serving of `pm-mock-runtime`; the `pm-web-app` chat
  components and their adoption in the views; the terminal chat dialogues of `pm-operator`; the `doc/design` consistency
  edits found on the way.
- Out of scope: the voice package (transcription, the speech role, `VoiceInput`; a later package); the engine and the
  daemon (`plan-marshall/pm-mcp-core`, `plan-marshall/plan-marshall-mcp`, epic.md Open Defects 7); the D1 foundation
  components and the D2 column engine (not staged; operator decisions O1 and O2 of 2026-11-10).

## Plans

| Plan | Status | Notes |
|------|--------|-------|
| PLAN-132-chat-contract | staged | B1, `pm-api`; first of the chain |
| PLAN-133-chat-mock-runtime | staged | B2, routes, streaming, timing, failures; new `ChatRegistry`; after PLAN-132 and PLAN-45 |
| PLAN-134-chat-mock-context-narrative-queued | staged | B3, context, narrative, queued revision; after PLAN-133 |
| PLAN-135-web-chat-core | parked | C1; waits for the D1 chat building blocks (O1) and the side-column decision (O2) |
| PLAN-136-web-chat-proposals-and-status | parked | C2; after PLAN-135 and PLAN-134 |
| PLAN-137-web-chat-places | parked | C3; after PLAN-136; ChatPanel retirement and adoption owner (O3), chat addresses (O10) |
| PLAN-138-cli-chat-dialogue | staged | D1; after PLAN-132, PLAN-133 and PLAN-123 |
| PLAN-139-cli-chat-knows-and-status | staged | D2; after PLAN-138 and PLAN-134 |
| PLAN-140-cli-chat-places | staged | D3; after PLAN-139; chat addresses (O10) |

The D3 brand packages PLAN-130 (brand icon, tokens, WaitingMark) and PLAN-131 (mascot figure and surfaces) belong to
WS-02 web-foundation, whose charter already names brand; PLAN-135 waits for PLAN-130's WaitingMark.
