# PLAN-85: Web: Theme and Language on the Pairing Page and in "This Browser"

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Operator request of 2026-10-30: the light/dark/system control no longer stands on every page, and settings of one type are not spread over several plans (operator, same day). The two preferences of the browser, **theme** and **language**, are therefore handled together here: each control is offered in exactly two places:

- **On the pairing page**, the first screen a browser shows, so both choices are made before anything else is read. A page without a way to Settings (the revoked-device page is the other one) carries the same controls.
- **In Settings, in a new view "This browser"** (`/settings/browser`), beside the other settings of the browser rather than of the machine. A choice applies at once; the view has no Save button and says in one sentence that it is kept in this browser and never sent to the machine.

Both controls leave the rail foot (desktop) and the More sheet (phone), where `PLAN-39` put them. The collapse button of the rail stays on the rail: it is a layout toggle of the rail itself, not a preference to be chosen elsewhere. The storage of both is unchanged: the theme stays in `localStorage` under `pm_theme` (`light`, `dark`, `system`) as `PLAN-04` built it and the language under `pm_locale` (the operator confirmed on 2026-10-30 that local storage is fine), and the inline script of `app.html` keeps setting the theme before the first paint. How the language switch refreshes the texts stays the work of `PLAN-16`; this plan only moves where the person chooses. The design documents change in the same pull request (`doc/design/web-app/README.adoc` §§ shell, rail and themes).

## Deliverables

1. **The controls on the pairing page, off the frame**
   - What: the three-way theme choice and the language choice become components (`ThemeChoice.svelte` and `LocaleSwitch.svelte`, extracted from `NavRail.svelte` and `BottomBar.svelte`; the name `LanguageChoice` is taken by the onboarding's interface-language component in `components/`, a different thing) used by `PairingView.svelte`, `RevokedView.svelte` and the new view; `NavRail.svelte`, `BottomBar.svelte`, `AppShell.svelte` and `routes/+layout.svelte` lose both controls (the props go; the state and the `writeThemeChoice` and `setLocale` calls move to where the components are mounted or stay in the layout as one handler each, whichever leaves less code); the rail keeps its collapse button. Texts stay in the catalogue (`web.frame.theme.label`, `web.frame.language.*` as they exist, and the existing choice labels; no new wording is invented for the pairing page).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Component tests: the pairing page and the revoked page show both controls and apply a choice at once (texts in the other language after choosing it); the rail and the More sheet show neither (evidence order: `test-first`)
     - `pairing.spec.js` and `chrome.spec.js`: choosing dark and German on the pairing page paints and reads them before pairing and both survive pairing and a reload; the rail has no theme and no language control (evidence order: `test-first`)
     - Every e2e test and helper that chose a theme or a language through the rail control (`helpers/contrast.js` `setTheme`, `theme-readability.spec.js`, others found by a search for the control's test ids) uses a place that still exists, and the contrast audit passes in both themes with the same tests or more (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: the pairing page (empty profile) and `fully-onboarded`: the pairing page with the control in light and dark, the rail and the More sheet without it; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **"This browser" in Settings**
   - What: `BrowserSettingsView.svelte` at `/settings/browser`, reached from the Settings area (a link beside the existing settings pages; `PLAN-60` makes it the tab "This browser" of the one settings view): the theme choice and the language choice with one sentence ("Kept in this browser. It is not sent to the machine."), applied at once, no Save, no "changed" mark; the view is the one home of every setting that belongs to the browser alone, so a later such setting goes here and not into another plan's view. The design lands in `doc/design/web-app/browser-settings.adoc` (linked from the README) and the README states the new placement of the control; the texts `web.settings.browser.*` are added in English and German.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-documents:ref-asciidoc
   - Done when:
     - `BrowserSettingsView.test.ts`: the three theme choices and the languages, the current ones marked, a choice applies at once and is stored under `pm_theme` and `pm_locale`, no save control (evidence order: `test-first`)
     - An e2e test: from Settings to "This browser", choose light and the other language, reload, both choices stand; the route is reachable from the rail's Settings destination (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` → `/settings/browser` in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The design documents state the new placement and no document still says the control stands in the rail foot or the More sheet (a search for "rail foot" and "More sheet" with "theme" finds none) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through npm scripts and Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20850`, app port `20851` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- The storage of the choices (`pm_theme` and `pm_locale` in `localStorage`, the inline script of `app.html`, `theme.ts`, the i18n module) is not changed by this plan; a test that finds it changed fails. How the language switch refreshes the texts is `PLAN-16`'s; a defect found in it here is reported to that plan, not fixed twice.
- The contrast audit (`theme-readability.spec.js`) passes in both themes after the change, with the same tests or more.
- Every new text goes through the message catalogue (`web.settings.browser.*` in `settings.{en,de}.properties` of `pm-api`).
- Behaviour is unchanged apart from the placement of the choice; no colour or token changes.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20850"`), then `PM_MOCK_WEB_PORT=20850 ./pm-web-app/start.sh -p 20851` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20850/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the theme is stored in `localStorage` under `pm_theme` with `light`, `dark`, `system`, and `app.html` reads it in an inline script before the first paint — read at `pm-web-app/src/lib/theme.ts` and `pm-web-app/src/app.html` at HEAD 07104678
- OBSERVED: the choice stands in the rail foot (`NavRail.svelte`, fieldset `theme-choice`) and, on a phone, in the More sheet (`BottomBar.svelte`); `+layout.svelte` holds `themeChoice` and calls `writeThemeChoice` — read at HEAD 07104678; `doc/design/web-app/README.adoc` § Light and Dark Themes (line 374) and the rail description (line 171) say so
- OBSERVED: `PairingView.svelte`, `RevokedView.svelte`, `SettingsView.svelte` (devices, `/settings`) and `MachineSettingsView.svelte` (`/settings/machine`) exist; `nav.ts` has a Settings destination — read at HEAD 07104678
- OBSERVED: the language choice is stored in local storage under `pm_locale` and is offered in the rail foot and the More sheet beside the theme — stays stored as it is; its control moves with the theme control
- HYPOTHESIS: `PairingView` and `RevokedView` render without the frame, so the control has to be mounted by the views themselves (`PLAN-39` kept the frame controls on such pages) — confirm/refute at the two views and `AppShell.svelte` (verify-at-outline)
- HYPOTHESIS: `helpers/contrast.js` `setTheme` sets the theme through the page's control or storage, and other e2e specs click the rail's theme control — search `pm-web-e2e/tests` for the control's test ids and `pm_theme` (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-60` (the one settings view), `PLAN-70` (light theme pass), `PLAN-16` (locale switch) and `PLAN-37` (accessibility baseline) edit the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/shell/AppShell.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/shell/ThemeChoice.svelte` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/LocaleSwitch.svelte` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/LanguageChoice.svelte` — present at HEAD 07104678 (the onboarding's interface-language choice; untouched, owned by `PLAN-66`)
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/BrowserSettingsView.svelte` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/BrowserSettingsView.test.ts` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-web-app/src/routes/settings/browser/+page.svelte` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/helpers/contrast.js` — present at HEAD 07104678
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 07104678
- OBSERVED: `doc/design/web-app/browser-settings.adoc` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-04` (`web-theme-and-titles`, shipped), `PLAN-27` (`web-brand`, shipped), `PLAN-39` (`web-app-shell`, shipped), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-60` (the one settings view: this plan's view becomes its tab "This browser"; run one after the other, either order), `PLAN-70` (light theme pass: `theme-readability.spec.js`), `PLAN-16` (locale switch in the same frame), `PLAN-37` (accessibility baseline), `PLAN-83` (`AppShell`, shell files), `PLAN-46` (shell)
- Adjacent to: `PLAN-60`

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-85-web-theme-in-browser-settings.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
