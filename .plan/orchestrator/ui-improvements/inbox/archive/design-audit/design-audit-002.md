envelope_version=1
sender_type=orchestrator
sender_id=design-audit
epic=ui-improvements
kind=finding
created=2026-11-02T11:31:34Z

# Design audit: client design and packages changed (branch docs/design-audit, commit fca5c537)

Sender: the design audit session. Follows design-audit-001. The changes sit on branch `docs/design-audit` of pm-mcp-clients and are not merged; they merge only on the operator's word. Nothing in this epic was edited. Re-read your affected plans against the branch once it is merged.

- **Statuses.** Every design file carries its own status, and the design README has no status column any more. The web-app files that said FINAL (README, components, brand-mark, interaction-affordances, theme-readability, view-density) are AGREED. intent-and-outline, plan-view-interaction and workflow-visual-language stay DRAFT. PLAN-74 no longer needs to set statuses and should not write a PLAN-id status list into the README.
- **Text tables.** All 14 tables of terminal-client 09–13 are replaced by links to the `pm-api` message packs, which are the source. Every section and anchor stays. PLAN-30 should drop its table edits; the strings it moves go into the packs only.
- **Command tree (01-commands).** The `abort` row is gone, `runtime stop|restart [--when-done|--now]` and `runtime stop --withdraw` are spelled one way, `--drain` is gone, and withdraw links core runtime-lifecycle/01#withdraw-drain. PLAN-42 (help lists every verb) should wait for `operator-command-tree`.
- **Plans.** The README links the project setup for the process and states the client-work track. Done rows keep no link, and dependencies are named by package id. The 9 package files hold tasks with anchors. The `needs-and-find` design task is recorded as finished. The Done-when lines of PLAN-36, 84, 51, 59 and 83 now mean "row `done`, no link".
- **New packages:** `operator-drain-signal` (D25), `operator-command-tree` (D26: start, onboard, enrol, unenrol and abort folded into the design verbs or removed), `accept-edit-enrolment` (R7-7). PLAN-79 (the pm-operator tests) should be sequenced after them.
- **Open points found:** the `operator.key.*` keys are in no pack. `pm-operator enrol` is in the spec but not in the design. The server answers the withdraw DELETE with 405 (a plan-marshall-mcp item).
