# WS-01: Shared Foundation: Mock, Display Vocabulary, Use Cases

epic: ui-improvements

## Charter

Everything both clients stand on and that neither client owns: the mock runtime as a test bed in which every scenario state reaches the views it concerns, the shared display vocabulary (state words, the time and duration rule, the id-to-title rule) in the shared catalogue and in `shared/texts-and-languages.adoc`, and the use-case documents the review proposes. The workstream closes when its verification plan has passed.

## Scope

- In scope: `pm-mock-runtime`; the shared catalogue keys of `pm-api/src/main/resources/messages/common.*`; `doc/design/shared/texts-and-languages.adoc`; the use-case documents in `plan-marshall-documentation`.
- Out of scope: Any client code (`pm-web-app`, `pm-operator`): owned by WS-02 to WS-05.

## Plans

| Plan | Status | Lane | Notes |
|------|--------|------|-------|
| PLAN-03-mock-scenario-states | staged | runbook | Mock: Every Scenario State Reaches Its Views |
| PLAN-06-shared-display-vocabulary | staged | runbook | Shared Display Vocabulary for Both Clients |
| PLAN-08-use-case-documents | staged | runbook | Use-Case Document Changes of Both Reviews |
| PLAN-11-mock-cli-fixes | staged | runbook | Mock: Plan Details per Id, Routes, Lifecycle State |
| PLAN-18-verify-shared-foundation | staged | plan-marshall | Verify the Shared Foundation |

## Sequencing and Surface Notes

- PLAN-11 after PLAN-03
- PLAN-18 after PLAN-03, PLAN-06, PLAN-08, PLAN-11
