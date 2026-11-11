# Landing Analysis: PLAN-132 — The Chat Contract in pm-api

epic: ui-improvements
workstream: WS-07
pr: #409 (https://github.com/plan-marshall/pm-mcp-clients/pull/409), squash `572dc739` (79 files, +6890/-179), merge head `5abbd760`

> Written by the `analyze` verb (2026-11-11) from `plan-132-chat-contract-017.md`, verified against git, the review threads
> (GraphQL), the PR reviews and the design table. `/plan-marshall` lane, claude `claude-sonnet-5-5`, plugin and executor
> `0.1.2090`; 44 543 886 tokens, 116 491 s wall. The import-order rewrite was split out first as #410.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Read model | shipped-as-specified | `pm-api/.../api/chat/*` (34 files, `@NullMarked`); eight superseded types name their deleting plans |
| D2 Requests, errors, routes | shipped-as-specified | `ChatTurnRequest.java:24-55`, `ChatTurnAccepted`, `ProposalAnswerRequest`, `ChatRoutes.java`; error codes pre-existed; `ChatRequestsTest.java:251-270` pins titles only |
| D3 Events | shipped-modified | seven names with streams (`EventName.java:135,256-271`), typed payloads, `queued_revision` on `ProcessTree`/`ActivePlanSummary`; `ActivityChangedEvent` without `queued_revision` (the runtime catalogue has none; said only in the PR body) |
| D4 Texts | shipped-as-specified | 106 `control.chat.*` keys en/de = the 106 table rows (PR body says 105); `ChatTextsCatalogueTest` compares verbatim |
| D5 Channel default | shipped-as-specified | no `cli` default; `DecisionRecordsTest` absence tests fail on revert; the six `AnswerCommand` sends listed |

Added-unplanned: `EventStream` enum with a stream set on all ~60 `EventName` members; `DecisionAnsweredEvent.channel` nullable and
`decision_answered_no_device` (CodeRabbit 1b0de6); `work-actions-chat-texts.adoc` edited (operator-accepted); `ChatTurnPointer`,
`ChatOrigin`, `NarrativeLine`, `EndedBy`; mock `answeredPayload`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: 79 files of contract, events, catalogue and consumers.
  No write; listed skills not loaded (CLAUDE.md rules used) — disclosed. Re-read by the orchestrator at `572dc739`:
  `MessageText.java:12-40`, the key literals of `ChatEventsTest.java`, `control.en.properties:227-303`,
  `work-actions-chat-texts.adoc:17`. Confirmed.
- **Verdict:** layer 1 — **MAJOR**: the wire form of the chat's message keys is inconsistent. The design table and `MessageText`'s
  Javadoc say the runtime sends `chat.line.*`/`chat.knows.*`/`chat.applies.*`, rendered through `control.chat.*`; every pm-api golden
  sends `control.chat.*` keys, 11 of which exist in no catalogue (`control.chat.reason.lost` vs `control.chat.lost`, …);
  `MessageText`/`NarrativeLine` accept any dotted key. Minors: a machine-scope decision chat has a route (`ChatRoutes.java:130-132`)
  but no event stream (`06-event-catalogue.adoc#chat-events`); the D3 text contradicts the catalogue. Layer 2 — Done-whens met;
  the "accepted removal" of `ProcessTreeTest::shouldRoundTripProcessTree` is a split that keeps every assertion (mislabel); the
  derived `RESET`/`STREAM_CLOSED` sets are unpinned (`EventNameTest.java:59-66` cannot fail). Layer 3 (minor): `ChatTurn.java:117-140`
  accepts `model`/`steps` on an operator turn that the event refuses; `EventName` accepts an empty stream set; `EventStream`
  duplicates `EventScopeKind` by hand; `DecisionView.svelte:375-381` maps absent channel to "web"; the mock writes an absent device
  as `""` (`MockRuntimeEngine.java:9873-9881`); nits (`ChatResource.java:85` Javadoc, dot-segment guard private to `ChatRoutes`
  while `OperatorUrlUtil.encodePathSegment` has the same weakness, `/api/v1` in three places).
- The six in-run fixes hold, each with a test that fails on revert (open-only `hasProposal`, plain turns refuse markdown,
  dot-segment ids, the design-table comparison, derived stream-control sets, optional channel/device texts).
- Confirmed sound: fail-closed readers (`ChatJson.java`), `jackson-core` only, no generic exceptions, `@NullMarked`, Generators with
  literal goldens, wire shapes and routes against the specs, `pm-operator` main untouched.

Routing (operator, 2026-11-11, AskUserQuestion): the key form → **PLAN-133** deliverable 0 (first, before its mock emits a key:
the wire carries the runtime's `chat.*` keys per the design table; goldens corrected; a mapping test); the pm-api and mock minors
→ **PLAN-133** deliverable 6; `DecisionView`'s absent channel/device → **PLAN-136**; the machine-scope stream gap → Open Defects
(runtime spec).

## Metrics and Anomalies

- `effort` `unknown` (no stamp); `unresolved_review_threads`, `review_body_findings_*` `unknown` (not measured by the lifecycle).
- A Sonar loop-back of 34 findings after the first push (S5778, S1130); the whole-tree import rewrite pushed the PR over the
  bots' size cap and was split out as #410.
- 16 candidate lessons: rule 5 adopted into epic.md; eight plan-marshall-side to Open Defects; five in-run code defects
  (fixed); one inventory item already a direct-PR candidate. All archived.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `9099bca1` (body "4 actionable", 3 inline found — a count mismatch, no unanswered body item) and
  `c5cd818c` (empty bodies); 3 of 3 threads replied and resolved (the machine-stream one rebutted, CodeRabbit withdrew). Final head
  `5abbd760` refused on quota; merged under the operator's `barrier-ask-override` ("one CodeRabbit review of the PR"). Merge rule met.
- CI green at `5abbd760`; merged via the queue.
- Sonar: `0` confirmed by the lane at `5abbd760`. Not re-read here (merged PR, Sonar MCP down).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 409; `landing` `landings/PLAN-132.md`
- [x] PLAN-133 deliverables 0 and 6, PLAN-136 amendment (operator)
- [x] epic.md: spec rule 5, Open Defects (lessons and the machine-scope stream gap)
- [x] messages 001-017 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-133 (running, not yet initialised when amended) starts with deliverable 0. PLAN-134 and PLAN-138 follow PLAN-133.
