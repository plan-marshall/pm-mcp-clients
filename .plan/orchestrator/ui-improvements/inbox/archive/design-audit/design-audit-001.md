envelope_version=1
sender_type=orchestrator
sender_id=design-audit
epic=ui-improvements
kind=finding
created=2026-11-02T09:19:47Z

# Design audit 2026-11-02: decisions that touch this epic's plans

Sender: the whole-corpus design audit session (plan-marshall-documentation, branch `docs/design-audit`). The operator has decided the points below. The documents are being changed now. Nothing in this epic was edited: this message is the only channel, and the epic decides how to act on it.

## Operator decisions that affect the plans
- **Staging window (D24/D24a).** A package builds only milestones in the staging window. The client work built against the mock runtime is a standing exception (the client-work track): this epic's Milestone 11 work (UC-PLN-7/8 and others) stays allowed. Its integration with the real runtime belongs to the milestone that delivers the runtime. The rule is in project-setup.adoc § Planning the Work.
- **Command tree (D26).** The code follows design `terminal-client/01-commands`. The extra top-level verbs start, onboard, enrol and unenrol are folded in or removed by a clients package. D14 drops `pm-operator abort` (plan cancel|abandon and epic cancel exist).
- **D25 drain (corrected).** `pm-operator drain` signals the pid in `state/runtime.json` and waits the bounded drain time. Withdraw stays, but as `runtime stop --withdraw` per the design (01-commands:84), not `drain --withdraw`. The server's stop honours `when`. The open question "does drain ask for confirmation" (epic.md:423, PLAN-91:61) is already answered: shared/system.adoc:199 says the drain asks nothing.
- **R7-7.** accept-edit follows the spec (`accept-edit enrolment <project_id>`).
- **Renames.** D16: client profile → harness profile, `client_id` → `harness_id`, `pm-operator client` → `pm-operator harness`. D13: `decisions export` → `pm-operator decision-log export`. Each lands as its own package; design 01-commands :28-31, :90 and :95 change with them.
- **Z2.** Package files list tasks only. Plans-index done-rule: the row stays `done`, keeps no link (no `implemented/` link), the file is deleted, and dependencies are named by package id. Implementation status is not written into design documents.
- **R4-14.** The terminal text tables 09–13 are replaced by links to the implemented .properties packs.
- **Design statuses (R7-3/R7-4).** The web-app design FINAL becomes AGREED. The new web-app/components.adoc:15 `FINAL` falls under the same rule.

## Recommended actions per plan (the epic decides)
- **Hold:** PLAN-24 (proof of presence: it turns L-1 into PM-SEC-3 text, a package and an ADR while its milestone is unplanned; or rescope it to an operator decision recorded on L-1). PLAN-42 (help "lists every verb" would fix the extra verbs into tests and docs; hold until the D26 package, or rescope to the design tree). PLAN-79 (rewrites the tests of Drain, AcceptEdit, Start, Onboard and Enrol; sequence it after the D25/D26/R7-7 package).
- **Rescope:** PLAN-30 (drop its table edits in 09/13, R4-14; move the OnboardCommand strings after D26). PLAN-74 (no done/dropped list with PLAN ids in web-app/README; the FINAL→AGREED status can go here). PLAN-36, 84, 51, 59, 83 (Done-when: row `done`, no link). PLAN-36 and 84 (also add the needs/find rows to the spec command tree).
- **Proceed:** PLAN-47 (its withdraw follows the design), 66, 91.
- **The epic already covers part of R7-3:** the start route `/` (PLAN-49), stylelint (PLAN-72), axe-core (#297). The audit will not duplicate them.
- **Ledger.** The copy on main dates from 10-31; the current one is on chore/orchestrator-ledger.
