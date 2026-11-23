# Brief: the chat contract (O10) and the chat proposal contract (an engine work package)

epic: ui-improvements
written: 2026-11-23 (the orchestrator, on the operator's word: decision 6, proposal 1)
status: NOT a plan and NOT queued. A brief for a DIRECT SESSION of the operator in `plan-marshall/pm-mcp-core` (the state catalogue is design work, and that repository is not this epic's tree), with one question for the daemon specification in `plan-marshall-documentation`. The orchestrator starts nothing and writes nothing outside this epic's tree. The clients plan of step 3 is staged only when step 1 is merged.

## Why

Two open items of this epic's `Open Defects` (2026-11-13 and 2026-11-14) wait on the engine's documents:

- **The chat contract gaps (O10 + PLAN-161).** The operator set the condition: one `pm-api` and mock plan, staged once the runtime documents name the surfaces, before PLAN-137 D2 to D4, PLAN-138 D4 and PLAN-140 D1 and D2.
- **The chat proposal contract (PLAN-134 landing).** A cross-repo edit in `pm-mcp-core` `doc/specification/state-catalogue.adoc`, before PLAN-140 D-R134 items 5 and 6.

The operator decided on 2026-11-23 (handle-open, decision 6, proposal 1) to settle both in the engine first and to stage the clients plan against what the engine then says, not against hypotheses.

## What the engine and the daemon specification say today (re-read: `pm-mcp-core` `cd05e7ab`, `plan-marshall-documentation` `feab6a6`)

- **Where a chat is addressed** (`runtime-model/01-processes-and-transport.adoc:168,280`): `GET` and `POST …/chat`, `…/chat/turns`, `…/chat/turns/{turn_id}/stop`, `…/chat/proposal/{accept|reject}`, `…/chat/end` below a plan, an epic, an entry, a decision of any scope but the machine's, or an ended plan or epic. The machine scope's decision offers no chat (`POST …/chat` there is refused `act_not_available`). **This is more than the 2026-11-13 entry could assume**: the addresses now exist for the subjects.
- **The two chats the clients lack** are named in the state catalogue as called states: `decomposition.chat` (the draft) and `review.chat` (one item or the whole register), "called by a state that offers it, on the call stack, with the caller's subject and scope" (`state-catalogue.adoc:86`; `state-catalogue/17-epic-decomposition-and-review.adoc:15,116`). **Nothing says whether they have an address of their own** or are the epic's `…/chat` with the draft or the review as the subject's scope.
- **The client side:** `ChatRoutes` (`pm-api/src/main/java/de/planmarshall/api/chat/ChatRoutes.java:25-27`) has builders for the chat of a plan, an epic, an entry, an ended plan or epic and a decision of each scope (`ChatRoutes.java:49-130`, `machineDecision` included) and none for the draft or the review; its Javadoc says they are left out "until the runtime specification and the core state catalogue name their addresses".
- **The proposal states** (`state-catalogue.adoc:138,213`): `refused` is "where a check of its path failed, the finding shown"; the text promises `reject` only for `conflict` ("the proposal stays visible and can be rejected"). A refused proposal's `reject` is not promised.
- **The line keys** (`state-catalogue.adoc:146-168`): the closed `chat.line.*` table has `phase_changed`, `task_ended`, `decision_*`, `hold_*`, `waiver_*`, `plan_*`, `version_changed`, `replan_triggered`, `pr_*`, `row_*`, `auto_changed`, `register_added`, `item_disposed`, `source_*`. The text says conflict and lapse are "told by a notice" but **no key names that notice**. No client uses `proposal_conflict` or `proposal_lapsed` yet.

## The package, in order

### Part A: the addresses (O10 items 1 and 3)

1. **The draft's and the review's chats.** Decide and write: do `decomposition.chat` and `review.chat` have an address of their own (name it: below the epic, with the item or the register as the subject's scope?), or are they the epic's `…/chat` whose subject carries the called state? The answer decides whether the clients need two new route builders or none. If none, say so in the daemon specification so the clients' Javadoc can stop waiting.
2. **The decision envelope's subject id for every scope.** Today `plan` and `project` carry an id; `epic`, `entry` and `machine` are free text in the envelope. A client that must address `…/chat` of a decision below its scope needs the subject's id as a field. Decide the field and its rule per scope (the machine scope has none and offers no chat).
3. **The accepted ended-work follow-up.** The accept answers a `ChatResource` with no entry id; `StartRegistry.openFollowUp` (client side, the mock) broadcasts no `start.*` event and no project-entries listing exists. Decide what the accept returns (the new entry's id), the event the entry announces itself with, and the listing a client reads to find it. (The same gap is the old settled.md "F9 … needs a decision".)

### Part B: the proposal contract (the `pm-mcp-core` edit)

4. **REFUSED stays rejectable.** § `chat-proposal-states` and the row at `:213` say `reject` is taken for `conflict`; write that a proposal `refused` by a check of its path (no plan, a passed boundary, a failed check) also takes `reject`, and what `reject` of it does (it drops the proposal, the finding stays in the transcript).
5. **A line key for the conflict and the lapse notices.** Add the keys to the closed `chat.line.*` table (for example `proposal_conflict` and `proposal_lapsed`, with their arguments: the proposal's turn id, the instance version, the reason `a revision is already queued` among them) and say what each is derived from.
6. Update the state catalogue rows, the `chat` section's `since_last_turn` text where it lists the outcomes, and the traceability rows.

### Part C: the clients, after A and B are merged (a plan of this epic, not staged now)

7. **`pm-api` and the mock:** the `ChatRoutes` builders A1 settles, the decision envelope's subject id (A2), the follow-up's entry id and event (A3), the two proposal rules and the two line keys (B4, B5) in the catalogue the mock mirrors.
8. **The web view and the terminal client:** PLAN-140 D-R134 items 5 and 6 and PLAN-137, PLAN-138 and PLAN-140's chat deliverables read the contract; the web view's `control.chat.follow_up.opened` message gets its destination, and the decision chat stops saying "no chat". Those plans are staged or parked behind the CLI chain; they are re-grounded when this part lands.

## What the operator decides (the orchestrator will not)

- Whether the draft's and the review's chats have an address of their own and its shape (A1).
- The decision envelope's subject-id field and its name (A2).
- What the follow-up's accept returns and how its entry is announced (A3).
- Whether a refused proposal's `reject` drops it silently or leaves a notice (B4).
- The two line keys' names and arguments (B5).

## Where it touches this epic

- The `Open Defects` entries "the chat contract gaps (O10 + PLAN-161)" and "the chat proposal contract (PLAN-134 landing)" stay open and carry this brief; they close when step 6 is merged and the Part C plan ships.
- No staged plan is changed by this brief. PLAN-137 (parked), PLAN-138 and PLAN-140 (behind PLAN-42 and PLAN-44) keep their chat deliverables as they are and are re-grounded when Part A and B are merged.
- A clients plan (no number yet) is staged after step 6, not before: staging it now would make its claims about unconfirmed addresses.
