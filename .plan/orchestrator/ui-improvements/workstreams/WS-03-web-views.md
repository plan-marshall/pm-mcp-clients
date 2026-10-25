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
| PLAN-57-web-decision-feedback | staged | runbook | Web: Correct Feedback for a Decision |
| PLAN-58-web-epic-page | staged | runbook | Web: The Epic Page |
| PLAN-59-web-projects-and-onboarding | staged | runbook | Web: Projects Grid and Project Onboarding |
| PLAN-60-web-settings-one-place | staged | runbook | Web: One Settings Place, One Save per Form |
| PLAN-61-verify-web-shell-and-patterns | staged | plan-marshall | Verify the Web Shell and Patterns |
| PLAN-62-web-plan-phase-workflows | staged | runbook | Web: Plan Phases Show Their Workflows and Intent |
| PLAN-64-web-onboarding-screen-registry | staged | runbook | Web: Every Onboarding State Has a Screen |
| PLAN-65-verify-web-work-views | staged | plan-marshall | Verify the Web Work Views |
| PLAN-66-web-onboarding-frame-and-roles | staged | runbook | Web: Onboarding Variations, Frame to Providers |
| PLAN-67-verify-web-plan-and-decisions | staged | plan-marshall | Verify the Web Plan Page and Decisions |
| PLAN-68-web-onboarding-harness-language | staged | runbook | Web: Onboarding Variations, Harness to Language |
| PLAN-69-web-onboarding-models-store | staged | runbook | Web: Onboarding Variations, Models to Harness Choice |
| PLAN-70-web-light-theme-pass | staged | runbook | Web: Light Theme as a Designed Theme |
| PLAN-71-verify-web-onboarding | staged | plan-marshall | Verify the Web Onboarding |
| PLAN-72-web-token-cleanup-lint | staged | runbook | Web: Remove Old Tokens, Lint Raw Values |
| PLAN-73-web-text-guard-failing | staged | runbook | Web: Text Guard in Failing Mode |
| PLAN-74-web-design-docs-closure | staged | runbook | Web: Design Documents Closed |
| PLAN-75-verify-web-final | staged | plan-marshall | Verify the Web App as a Product |

## Sequencing and Surface Notes

- PLAN-39 after PLAN-31, PLAN-27, PLAN-04
- PLAN-43 after PLAN-39
- PLAN-45 after PLAN-35, PLAN-39
- PLAN-46 after PLAN-39, PLAN-08
- PLAN-47 after PLAN-43
- PLAN-49 after PLAN-43, PLAN-08
- PLAN-51 after PLAN-43
- PLAN-52 after PLAN-43, PLAN-35, PLAN-03
- PLAN-53 after PLAN-43, PLAN-45
- PLAN-54 after PLAN-43
- PLAN-55 after PLAN-43, PLAN-47
- PLAN-57 after PLAN-51
- PLAN-58 after PLAN-43, PLAN-47, PLAN-03
- PLAN-59 after PLAN-43, PLAN-47
- PLAN-60 after PLAN-43, PLAN-47
- PLAN-61 after PLAN-39, PLAN-43, PLAN-45, PLAN-47, PLAN-46
- PLAN-62 after PLAN-55, PLAN-03
- PLAN-64 after PLAN-45, PLAN-60
- PLAN-65 after PLAN-49, PLAN-52, PLAN-53, PLAN-58, PLAN-59, PLAN-54, PLAN-60
- PLAN-66 after PLAN-64
- PLAN-67 after PLAN-55, PLAN-62, PLAN-51, PLAN-57
- PLAN-68 after PLAN-66
- PLAN-69 after PLAN-68
- PLAN-70 after PLAN-52, PLAN-53, PLAN-58, PLAN-59, PLAN-54, PLAN-60, PLAN-69, PLAN-62, PLAN-57, PLAN-49, PLAN-46
- PLAN-71 after PLAN-64, PLAN-66, PLAN-68, PLAN-69
- PLAN-72 after PLAN-70
- PLAN-73 after PLAN-70
- PLAN-74 after PLAN-72, PLAN-73
- PLAN-75 after PLAN-73, PLAN-72, PLAN-74
