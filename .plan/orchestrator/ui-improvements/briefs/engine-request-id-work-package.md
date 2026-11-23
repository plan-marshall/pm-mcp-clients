# Brief: `request_id` and the quorum channel on the answer path (an engine work package)

epic: ui-improvements
written: 2026-11-23 (the orchestrator, on the operator's word: decision 5, proposal 1)
status: NOT a plan and NOT queued. A brief for a DIRECT SESSION of the operator in `plan-marshall/pm-mcp-core` (the engine's catalogue and specification are design work, and that repository is not this epic's tree). The orchestrator starts nothing and writes nothing outside this epic's tree.

## Why

PLAN-51 (#469) built the "own answer confirmed" behaviour on a `request_id` that exists only on this side of the contract. The operator decided on 2026-11-18 (handle-open, proposal 3) that the field **grows in the engine** rather than being dropped from the client, together with a channel value for a quorum answer. Until the engine carries it, the client keeps its pending-own-answer fallback.

## What the clients have today (re-read at `3ae8a305`)

- `pm-api`: `request_id` is a field of `DecisionAnswerRequest` (`pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnswerRequest.java:86,130`) and of `DecisionAnsweredEvent` (`DecisionAnsweredEvent.java:65,94`); `DecisionRecordsTest.requestIdRoundTrips` pins the round trip.
- The web client sends it on the answer request and reads it from `decision.answered`; the mock echoes it; the web view shows own, console and elsewhere lines from it (PLAN-51 D4, `landings/PLAN-51.md`).
- The channel on an answer is `web`, `cli` or `other` (the catalogue keys `decision.channel.*`); a quorum answer (several operators, one decision) has no channel value of its own.

## What the engine has (re-read in `../pm-mcp-core`, `cd05e7ab`)

- The answer event is named in the state catalogue: `decision_opened`, `decision_answered`, `decision_closed` "answered, with its channel and device" (`doc/specification/state-catalogue.adoc:154`). It carries **no `request_id`**.
- The only `request_id` in the engine's specification is the waiver-request counter (`doc/specification/identifiers/02-registry-stores-to-web.adoc:25`; `phase-workflows/07-questions-waivers-operator-influence.adoc:68`): a different thing. The client-side name collides with it in vocabulary, so the new field needs a name that cannot be confused with it, or an explicit statement of the difference.
- The decision store and its quorum rule are in `doc/specification/store-schemas/14-plan-instance-waivers-questions.adoc` (a decision with a declared quorum, `none fits` answers); the lane of PLAN-51 did not read the engine.

## The package, in order

1. **`pm-mcp-core` (this brief's repository): the catalogue and the specification.** Decide and write: (a) the answer request carries an opaque client-chosen `request_id` (the name, the bound on its length, the rule when it is absent: the engine accepts a request without it); (b) `decision.answered` echoes the id of the request that produced the answer, and carries none for an answer that came from another client or from the quorum; (c) the channel value of a quorum answer (the clients render "answered elsewhere in {channel}"); (d) what the daemon does with an unknown `request_id` on the answer endpoint (ignore). Update the state catalogue row, the event schema and the traceability rows.
2. **`plan-marshall-mcp` (the daemon): a plan** that implements the contract: accept, store nothing durable (the id lives on the event), echo it, emit the quorum channel. A `plan-marshall-mcp` epic or plan of the operator's own; not staged here.
3. **`pm-mcp-clients` (this epic) after both:** the web view and the terminal client drop the pending-own-answer fallback and rely on the echo; `pm-api`'s records keep the field under the engine's name; the mock follows the catalogue; the design (`doc/design/shared/decisions.adoc#outcomes`, `:179-185`, which names neither `request_id` nor the fallback) says how an answer is confirmed. This part is a plan of this epic and is staged when step 1 is merged, not before.

## What the operator decides (the orchestrator will not)

- The field's name and whether the client's current `request_id` is renamed (a rename is cheap now: pre-1.0, no shim).
- Whether an absent id is allowed or refused (the client always sends one today).
- The quorum channel's name and wording.

## Where it touches this epic

- PLAN-51's entry in `epic.md` (`Open Defects`) stays open for the Enter, busy-marker, German dative and reload-order defects, which this package does not cover; the `request_id` follow-up of that entry and of `PLAN-66 deliverables 1 to 4` points here.
- No staged plan depends on this brief. PLAN-136 (staged, web chat proposals) and PLAN-135 do not read the answer path.
