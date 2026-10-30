# WS-03: Web Information Architecture and Views

epic: ui-improvements

## Charter

Turn the web app from a window per resource into a control room: app shell, attention inbox, command palette, page templates, one primary act per state, one error and loading pattern, then every view by value to the operator, the onboarding variations, and the closing steps (light theme, token cleanup, guard in failing mode, design documents). Web review: UX S-01 to S-13, Use Cases S-02 to S-07 and S-09 to S-10, Visual Design S-06 to S-09, Cross-Cutting S-06 and S-07.

## Scope

- In scope: `pm-web-app/src/lib/views/`, `lib/shell/`, `lib/templates/`, `lib/actions/`, `lib/stores/`, `lib/routes.ts`; the view specs of `pm-web-e2e`; the view catalogue areas; `doc/design/web-app/` and `doc/design/shared/` documents of the views.
- Out of scope: The foundation modules of WS-02 (consumed, not reworked); the mock runtime (WS-01).

## Plans

| Plan | Status | Lane | Notes |
|------|--------|------|-------|
| PLAN-39-web-app-shell | staged | runbook | Web: App Shell With Rail, Top Bar, Bottom Bar, Live Indicator |
| PLAN-43-web-page-templates | staged | runbook | Web: Four Page Templates and Four Reference Views |
| PLAN-45-web-error-loading-pattern | staged | runbook | Web: One Error Pattern, One Loading Pattern |
| PLAN-46-web-command-palette | staged | runbook | Web: Command Palette, Jump by Name |
| PLAN-47-web-actions-by-state | staged | runbook | Web: One Primary Act per State |
| PLAN-49-web-attention-inbox | staged | runbook | Web: Attention Inbox as Home |
| PLAN-51-web-decisions-triage | staged | runbook | Web: Decisions as Triage |
| PLAN-52-web-running-work | staged | runbook | Web: Running Work Across Projects |
| PLAN-53-web-start-wizard | staged | runbook | Web: Start as a Wizard With a Working Gate |
| PLAN-54-web-reports-and-designer | staged | runbook | Web: Reports Triage and Designer Panes |
| PLAN-55-web-plan-page | staged | runbook | Web: The Plan Page on the Object Template |
| PLAN-57-web-decision-feedback | staged | runbook | Web: Correct Feedback for a Decision — SUPERSEDED, moved into PLAN-51 (2026-10-30) |
| PLAN-58-web-epic-page | staged | runbook | Web: The Epic Page |
| PLAN-59-web-projects-and-onboarding | staged | runbook | Web: Projects Grid and Project Onboarding |
| PLAN-60-web-settings-one-place | staged | runbook | Web: One Settings Place, One Save per Form |
| PLAN-61-verify-web-shell-and-patterns | staged | plan-marshall | Verify the Web Shell and Patterns — SUPERSEDED, moved into PLAN-65 (2026-10-30) |
| PLAN-62-web-plan-phase-workflows | staged | runbook | Web: Plan Phases Show Their Workflows and Intent |
| PLAN-64-web-onboarding-screen-registry | staged | runbook | Web: Every Onboarding State Has a Screen |
| PLAN-65-verify-web-work-views | staged | plan-marshall | Verify the Web Work Views |
| PLAN-66-web-onboarding-frame-and-roles | staged | runbook | Web: Onboarding Variations, Frame to Providers |
| PLAN-67-verify-web-plan-and-decisions | staged | plan-marshall | Verify the Web Plan Page and Decisions — SUPERSEDED, moved into PLAN-65 (2026-10-30) |
| PLAN-68-web-onboarding-harness-language | staged | runbook | Web: Onboarding Variations, Harness to Language — SUPERSEDED, moved into PLAN-66 (2026-10-30) |
| PLAN-69-web-onboarding-models-store | staged | runbook | Web: Onboarding Variations, Models to Harness Choice — SUPERSEDED, moved into PLAN-66 (2026-10-30) |
| PLAN-70-web-light-theme-pass | staged | runbook | Web: Light Theme as a Designed Theme |
| PLAN-71-verify-web-onboarding | staged | plan-marshall | Verify the Web Onboarding |
| PLAN-72-web-token-cleanup-lint | staged | runbook | Web: Remove Old Tokens, Lint Raw Values |
| PLAN-73-web-text-guard-failing | staged | runbook | Web: Text Guard in Failing Mode |
| PLAN-74-web-design-docs-closure | staged | runbook | Web: Design Documents Closed |
| PLAN-75-verify-web-final | staged | plan-marshall | Verify the Web App as a Product |
| PLAN-88-web-designer-panes | staged | runbook | Web: Process Designer in Three Panes (the second half of PLAN-54; added 2026-10-30) |
| PLAN-85-web-theme-in-browser-settings | staged | runbook | Web: Theme and Language on the Pairing Page and in "This Browser" (operator request; added 2026-10-30) |
| PLAN-83-web-moving-between-plans | staged | runbook | Web: From Plan to Plan, a Plan From the Row, One Notification for a Burst (package web-moving-between-plans; added 2026-10-30) |

## Sequencing and Surface Notes

- PLAN-39 after PLAN-31, PLAN-27, PLAN-04
- PLAN-43 after PLAN-39
- PLAN-45 after PLAN-35, PLAN-39
- PLAN-46 after PLAN-39, PLAN-08, PLAN-82
- PLAN-85 has no plan dependency left (PLAN-04, 27, 39 shipped); one after the other with PLAN-60 (settings view), PLAN-70 (theme audit), PLAN-16 (locale)
- PLAN-83 after PLAN-81 (and one after the other with PLAN-52, 55, 58, 59, 51, 57 on the views they share)
- PLAN-47 after PLAN-43
- PLAN-49 after PLAN-43, PLAN-08, PLAN-82
- PLAN-51 after PLAN-43
- PLAN-52 after PLAN-43, PLAN-35, PLAN-03
- PLAN-53 after PLAN-43, PLAN-45
- PLAN-54 after PLAN-43
- PLAN-55 after PLAN-43, PLAN-47
- PLAN-57 superseded: its deliverables are PLAN-51 deliverables 3 and 4; PLAN-51 deliverable 5 is the notification rule of the package web-moving-between-plans
- PLAN-58 after PLAN-43, PLAN-47, PLAN-03
- PLAN-59 after PLAN-43, PLAN-47
- PLAN-60 after PLAN-43, PLAN-47
- PLAN-61 superseded: its checks are in PLAN-65
- PLAN-62 after PLAN-55, PLAN-03
- PLAN-64 after PLAN-45, PLAN-60
- PLAN-65 after PLAN-49, PLAN-52, PLAN-53, PLAN-58, PLAN-59, PLAN-54, PLAN-60
- PLAN-66 after PLAN-64
- PLAN-67 superseded: its checks are in PLAN-65
- PLAN-68 and PLAN-69 superseded: their deliverables are PLAN-66 deliverables 5 to 10
- PLAN-70 after PLAN-52, PLAN-53, PLAN-58, PLAN-59, PLAN-54, PLAN-88, PLAN-60, PLAN-66, PLAN-62, PLAN-51, PLAN-49, PLAN-46, PLAN-83, PLAN-85
- PLAN-71 after PLAN-64, PLAN-66
- PLAN-65 also checks PLAN-61's and PLAN-67's stages, PLAN-83 and PLAN-85; PLAN-88 after PLAN-43
- Acts by state: PLAN-47 builds the mechanism and applies it to the system, the devices and the shared control panel; PLAN-55, 58 and 59 apply it to the plan, epic and project views they rebuild
- Page templates: PLAN-43 builds the four and moves the system view; PLAN-51, 53, 55 and 58 move their own views
- Error texts are PLAN-86's, loading texts PLAN-45's, the Change line PLAN-55's and PLAN-54's, "Add a plan" PLAN-59's
- PLAN-72 after PLAN-70
- PLAN-73 after PLAN-70
- PLAN-74 after PLAN-72, PLAN-73
- PLAN-75 after PLAN-73, PLAN-72, PLAN-74
