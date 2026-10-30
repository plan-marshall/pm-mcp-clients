# WS-04: Terminal Client: Safe and Working Acts

epic: ui-improvements

## Charter

Make every dialogue of `pm-operator` that acts safe and working, proven in a real pseudo-terminal: the PTY harness for acting dialogues, the strict choice prompt, confirmed runtime acts, actions by state, the answer dialogue, the event contract, the state dialogue of both onboardings, the cleanup dialogue, and one system model (CLI review: Cross-Cutting S-01, Use Cases S-01 to S-08).

## Scope

- In scope: `pm-operator` dialogues, commands, action menus, system view, console notices; `pm-api` event kinds; the `/mock/acts` endpoint of the mock; the PTY tests of `pm-operator`.
- Out of scope: Texts, display layer, keys, presentation (WS-05); the web app (WS-02, WS-03).

## Plans

| Plan | Status | Lane | Notes |
|------|--------|------|-------|
| PLAN-02-cli-pty-dialogue-harness | staged | runbook | Terminal: PTY Tests for Every Dialogue That Acts |
| PLAN-10-cli-choice-prompt-pause | staged | runbook | Terminal: Strict Choice Prompt and a Safe Pause |
| PLAN-13-cli-event-contract | staged | runbook | Terminal: One Event Contract, Pushed Notices, Badges |
| PLAN-17-cli-runtime-act-confirm | staged | runbook | Terminal: Confirm the Runtime Acts |
| PLAN-20-cli-actions-by-state | staged | runbook | Terminal: Action Menus by State |
| PLAN-21-cli-answer-dialogue | staged | runbook | Terminal: A Working Answer Dialogue |
| PLAN-22-cli-state-dialogue-onboarding | staged | runbook | Terminal: One State Dialogue for Both Onboardings |
| PLAN-23-cli-cleanup-dialogue | staged | runbook | Terminal: The Cleanup Dialogue |
| PLAN-26-cli-system-model | staged | runbook | Terminal: One System Model |
| PLAN-28-verify-cli-safety | staged | plan-marshall | Verify the Terminal Safety Fixes — SUPERSEDED, moved into PLAN-33 (2026-10-30) |
| PLAN-32-cli-config-contract | parked | runbook | Terminal: The Config Contract |
| PLAN-33-verify-cli-acts | staged | plan-marshall | Verify the Terminal Acts |
| PLAN-79-cui-testing-operator | staged | runbook | Java: pm-operator Tests on Generated Input Data; after PLAN-77, when no terminal plan runs; added 2026-10-30 |

## Sequencing and Surface Notes

- PLAN-10 after PLAN-02
- PLAN-13 after PLAN-02
- PLAN-17 after PLAN-10
- PLAN-20 after PLAN-10
- PLAN-21 after PLAN-10
- PLAN-22 after PLAN-10
- PLAN-23 after PLAN-10
- PLAN-26 after PLAN-17, PLAN-11
- PLAN-28 superseded: its checks are in PLAN-33
- PLAN-84 (find) belongs with PLAN-36 (needs) in WS-05 by topic; its row was staged in WS-04 and the queue verb cannot change a row's workstream
- PLAN-32 after PLAN-26
- PLAN-33 after PLAN-21, PLAN-13, PLAN-22, PLAN-23, PLAN-26, PLAN-11
- PLAN-84 after PLAN-82 (one after the other with PLAN-36, 42, 48, 30 on `PmOperator`, `PmConsole`, `01-commands.adoc`)
- PLAN-79 after PLAN-77 (and when none of the terminal plans that edit pm-operator tests is running)
