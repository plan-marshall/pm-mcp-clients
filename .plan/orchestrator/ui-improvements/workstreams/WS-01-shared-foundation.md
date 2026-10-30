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
| PLAN-77-cui-standards-foundation | staged | runbook | Java: CUI Standards Foundation (dependencies, logging, null safety, first test conversions); highest priority, added 2026-10-30 |
| PLAN-78-cui-testing-api | staged | runbook | Java: pm-api Tests on Generated Input Data |
| PLAN-81-overview-order-and-size | staged | runbook | Shared: Running Work in a Stable Order, and a Plan's Size of Change — SUPERSEDED, moved into PLAN-82 and the client plans (2026-10-30) |
| PLAN-82-needs-and-find-contract | staged | runbook | Shared: The Runtime Contract and Mock for Order, Size, What Waits and Finding Work, With the Design of the Two Views (packages overview-order-and-size and needs-and-find, shared half; added 2026-10-30) |
| PLAN-86-shared-error-texts | staged | runbook | Shared: One Text Set per API Error Code (Title, Cause, Fix) for Both Clients (added 2026-10-30) |
| PLAN-87-cui-testing-mock-runtime | staged | runbook | Java: pm-mock-runtime Tests on Generated Input Data (split from PLAN-77; after PLAN-77) |

## Sequencing and Surface Notes

- PLAN-11 after PLAN-03
- PLAN-18 after PLAN-03, PLAN-06, PLAN-08, PLAN-11
- PLAN-78 after PLAN-77
- PLAN-81 superseded: the contract half is PLAN-82 deliverable 1; the terminal half PLAN-34 deliverable 1; the web halves PLAN-52, 55, 54
- PLAN-34, 36, 46, 49, 52, 54, 55, 83 and 84 after PLAN-82 (served order, size, lists, matches)
- PLAN-09, 30 and 45 after PLAN-86 (the error texts)
- PLAN-87 after PLAN-77
