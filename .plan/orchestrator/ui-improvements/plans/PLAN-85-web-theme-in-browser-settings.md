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
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: the pairing page (empty profile) and `fully-onboarded`: the pairing page with the control in light and dark, the rail and the More sheet without it; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **"This browser" in Settings**
   - What: `BrowserSettingsView.svelte` at `/settings/browser`, reached from the Settings area (a link beside the existing settings pages; `PLAN-60` makes it the tab "This browser" of the one settings view): the theme choice and the language choice with one sentence ("Kept in this browser. It is not sent to the machine."), applied at once, no Save, no "changed" mark; the view is the one home of every setting that belongs to the browser alone, so a later such setting goes here and not into another plan's view. The design lands in `doc/design/web-app/browser-settings.adoc` (linked from the README) and the README states the new placement of the control; the texts `web.settings.browser.*` are added in English and German.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-documents:ref-asciidoc
   - Done when:
     - `BrowserSettingsView.test.ts`: the three theme choices and the languages, the current ones marked, a choice applies at once and is stored under `pm_theme` and `pm_locale`, no save control (evidence order: `test-first`)
     - An e2e test: from Settings to "This browser", choose light and the other language, reload, both choices stand; the route is reachable from the rail's Settings destination (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` → `/settings/browser` in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
     - The design documents state the new placement and no document still says the control stands in the rail foot or the More sheet (a search for "rail foot" and "More sheet" with "theme" finds none) (evidence order: `none`)

3. **The entry pages show what the wire sends (folded from the PLAN-157 landing, operator 2026-11-23)**
   - What: the layer-3 findings of `landings/PLAN-157.md`, each re-read at `3ae8a305` and still open. The entry pages (`PairingView`, `RevokedView`, `ListenerOffView`) are not in the surface of deliverables 1 and 2 (option A took the controls off them); this deliverable adds them.
     - `RevokedView` resolves its reason sentence from keys the wire never sends. The runtime ends the stream with `device_revoked` (with channel, device and time) or `device_expired`, and `RevokedInfo` (`runtime.ts:55-60`) carries `reason`, `revoked_by`, `revoked_at` and `details`, no channel; `resolveReason` (`RevokedView.svelte:25-75`) matches `device`, `terminal`, `others`, `self` and `idle`, so the table lines of `entry.adoc` § `#revoked` and their tests cover invented inputs and a real revocation never shows the terminal, others, self or idle line. Settle at outline which facts the wire carries (`runtime.ts:745-755`, `client.ts:5200-5208`); carry the channel in `RevokedInfo` if the wire has it, or amend the design table to the lines the wire can drive; derive every line from the real values; drop the `as unknown as Record<string, string>` reads and the unused `days: 30`.
     - The `shellGone` guard is not pinned: `layout.test.ts:105-123` passes with `if (shellGone) return` (`+layout.svelte:257`) removed (the catalogue store is preset in `beforeEach`, and `initRuntime` never calls the probe the test asserts on). Write a test that fails without the guard.
     - Read at outline and fix where small: the held-figure retry is unbounded (`stores/heldFigure.ts` with `+layout.svelte:312-318`, a refetch on every store update while the figure fails); `createReconnectLoop.start()` is not idempotent (`runtime.ts:343`, a second call overwrites `interval`); the revoked-by fallback line changed text in a "pure restyle"; the exact-8 `CodeField` against the old "at least eight" and what `pair` is sent (`client.ts:3128`, raw); the narrowed `data:image/webp` scan and its silent build-dir skip.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - `RevokedView.test.ts` is fed `device_revoked` and `device_expired` as the wire sends them and shows each line; a test fed with the old keys is gone. Evidence order: `test-first`.
     - The `shellGone` test fails with the guard removed. Evidence order: `mutation-after`.

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
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
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: theme.ts:20 key, :57 read, :76 write; app.html:16-36 reads pm_theme before first paint (3dea04b2)
- OBSERVED: the choice stands in the rail foot (`NavRail.svelte`, fieldset `theme-choice`) and, on a phone, in the More sheet (`BottomBar.svelte`); `+layout.svelte` holds `themeChoice` and calls `writeThemeChoice` — read at HEAD 07104678; `doc/design/web-app/README.adoc` § Light and Dark Themes (line 374) and the rail description (line 171) say so
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: NavRail.svelte:184-211; BottomBar.svelte:196-231; +layout.svelte:210,286-290; README.adoc:150,156,426 (3dea04b2)
- OBSERVED: `PairingView.svelte`, `RevokedView.svelte`, `SettingsView.svelte` (devices, `/settings`) and `MachineSettingsView.svelte` (`/settings/machine`) exist; `nav.ts` has a Settings destination — read at HEAD 07104678
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: MachineSettingsView.svelte is gone; /settings/machine replaced by ?tab=setup (routes.ts:761-765); PairingView, RevokedView, SettingsView exist (3dea04b2)
- OBSERVED: the language choice is stored in local storage under `pm_locale` and is offered in the rail foot and the More sheet beside the theme — stays stored as it is; its control moves with the theme control
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: i18n/index.ts:8 pm_locale; NavRail.svelte:184-193; BottomBar.svelte:199-207,242-250 (3dea04b2)
- HYPOTHESIS: `PairingView` and `RevokedView` render without the frame, so the control has to be mounted by the views themselves (`PLAN-39` kept the frame controls on such pages) — confirm/refute at the two views and `AppShell.svelte` (verify-at-outline)
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: +layout.svelte:336-390 renders RevokedView/PairingView inside AppShell; AppShell.svelte:35,45 always mounts NavRail and BottomBar; BottomBar.svelte:234-273 frame fallback carries both controls (3dea04b2)
- HYPOTHESIS: `helpers/contrast.js` `setTheme` sets the theme through the page's control or storage, and other e2e specs click the rail's theme control — search `pm-web-e2e/tests` for the control's test ids and `pm_theme` (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: contrast.js:14-19 setTheme sets only data-theme; chrome.spec.js and workspace.spec.js assert through the rail control (3dea04b2)
- OBSERVED (reported by the run of PLAN-80, not verified here): the contrast matrix of `theme-readability.spec.js` calls `setTheme` (`helpers/contrast.js`) before `page.goto`, and `app.html` re-derives `data-theme` from `localStorage.pm_theme`, which nothing writes — so the "dark" audit may run under the light theme. This plan changes where the choice is made and the helpers that set it: it makes `setTheme` set `pm_theme` in the page's origin before the app loads (or choose through the control on the pairing page) and asserts `document.documentElement.dataset.theme` equals the requested theme in every audited test, so no audit passes under the wrong theme.
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: theme-readability.spec.js:664 setTheme then :667 page.goto; no dataset.theme assertion (3dea04b2)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-60` (the one settings view), `PLAN-70` (light theme pass), `PLAN-16` (locale switch) and `PLAN-37` (accessibility baseline) edit the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural; PLAN-60 landed (#457) (3dea04b2)
- OBSERVED: `RevokedView` resolves its reason from `device`, `terminal`, `others`, `self`, `idle`, `device_revoked` and `device_expired`, and the wire sends only the last two — read at `pm-web-app/src/lib/views/RevokedView.svelte:25-75` (3ae8a305) and `runtime.ts:55-60,745-755`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: RevokedView.svelte:20-76 invented reason keys; runtime.ts:66-71 RevokedInfo drops the channel; 06-event-catalogue.adoc:26 stream.closed carries channel+device+time (3dea04b2)
- OBSERVED: the `shellGone` guard stands at `+layout.svelte:257` and `layout.test.ts:105` is the only test that names it — read by `git grep -n shellGone -- pm-web-app/src`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: +layout.svelte:247,249,257 shellGone; only routes/layout.test.ts names it (3dea04b2)

## Expected Surface



- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD 1cc0e10e (tab host; owned by PLAN-60 / PLAN-45)
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 1cc0e10e (routes row of the tab only)
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.test.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.scenarios.test.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/routes.test.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.test.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.svelte` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.test.ts` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.svelte` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/stores/heldFigure.ts` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `doc/design/web-app/entry.adoc` — present at HEAD 3ae8a305 (folded D3, 2026-11-23)
- OBSERVED: `doc/design/web-app/machine-settings.adoc` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-e2e/tests/brand-s12-screenshots.spec.js` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)

## Dependencies and Sequencing

- Depends on: `PLAN-04` (`web-theme-and-titles`, shipped), `PLAN-27` (`web-brand`, shipped), `PLAN-39` (`web-app-shell`, shipped), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-60` (the one settings view: this plan's view becomes its tab "This browser"; run one after the other, either order), `PLAN-70` (light theme pass: `theme-readability.spec.js`), `PLAN-16` (locale switch in the same frame), `PLAN-37` (accessibility baseline), `PLAN-83` (`AppShell`, shell files), `PLAN-46` (shell)
- Adjacent to: `PLAN-60`

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 001, 003, 005, 007, 008, 009).
**Gate: D1, D2 and D3 merged** (documents `AGREED`) and the D1 foundation / D2 engine / D3 brand assets packages; **it starts
after `PLAN-109`** (the rail icon is not its concern). Queue status: `parked`; the orchestrator re-grounds this spec
(deliverables, Expected Surface) when it is restarted. The Expected Surface above is NOT widened now (a parked row is not in
the disjointness gate population). When restarted:

- The rail-foot bear (D3, a reversible test with a fixed, contrast-checked opacity) moves above the collapse control once
  theme and language leave the rail foot; the pairing and revoked layouts are checked **together with the figure** (005).
- The duplicated language toggle in `BottomBar` (`:169`, `:212`) goes, inside D2's bottom bar and More sheet (What waits ·
  Running work · Decisions · More) (003, 007).
- Theme and language become a `RadioGroup`, applied at once (008); `/settings/browser` follows D2's address scheme and routes
  table (it becomes the tab "This browser" of `PLAN-60`'s one settings view).
- The per-browser transcription choice ("Dieser Browser") belongs to the D4 voice package, not to this plan (009).

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 4 contradicted: +layout.svelte:309-374 renders RevokedView, PairingView, ListenerOffView, OnboardingView inside AppShell; BottomBar.svelte:205-239 fallback frame bar carries both controls
- Claim 4 is refuted: `+layout.svelte:309-374` renders the entry pages inside `AppShell`, and `BottomBar.svelte:205-239`'s fallback bar carries both controls. Option A of 2026-11-10 conflicts with this spec's Write-Boundary list (old surface still listed): settle at outline.

## Re-Grounding 2026-11-22 (cleanup, at `7b60cec0`)

Verdicts persisted per claim by `ui-improvements/cleanup` (`corpus set-verdict`, origin/main `7b60cec0`; sibling repos read at their local refs only). Contradicted claims are stamped `rescoped: yes` with the corrections below; read before the outline:

- Claims 2 and 4 contradicted (`rescoped: yes`): `MachineSettingsView.svelte` no longer exists (PLAN-60 #457; `/settings/machine` redirects to `?tab=setup`, `routes.ts:728`); `+layout.svelte:354-361` renders `RevokedView`, `PairingView`, `ListenerOffView` and `OnboardingView` inside `AppShell` and the `BottomBar` frame fallback `:243` carries both controls: option A dropped this deliverable. The body still carries the pre-option-A Objective and D1/D2 text; the `ThemeChoice.svelte`/`LocaleSwitch.svelte` rows of the Write-Boundary are obsolete.
- Open ambiguity: which `BottomBar` toggle goes (the More-sheet one, which `README.adoc:156` requires, or the frame-bar one, needed on the phone before pairing)? Both are asserted in `mobile.spec.js:159,240` (added to the surface). `README.adoc:485` documents `?tab=browser` and a `/settings/browser` redirect the code does not have (`routes.ts:730`: "browser arrives with PLAN-85"). Re-read `PairingView`, `RevokedView`, `ListenerOffView` at `fe44e6a7` (PLAN-157 #535).

## Folded from the PLAN-157 landing 2026-11-23 (analyze, operator)

The operator chose (decision 2, proposal 1) to fold the layer-3 findings of PLAN-157 (#535) into this plan as deliverable 3. **A correction of the proposal's premise:** the Expected Surface of this plan does NOT hold the entry views (option A of 2026-11-10 took them out; only the Write-Boundary list below still names them), so the fold widens the surface by the nine paths added above; the wire-value work of the entry pages is unrelated to the theme tab and can move to another plan on the operator's word. PLAN-85 was NOT re-grounded at this fold beyond the two new claims: its older claims (surface touched by PLAN-59, 156 and 157: `routes.ts`, `SettingsView.svelte`, `chrome.spec.js`, `runtime.ts`) are re-read before the emit, and the plan emits after PLAN-144 lands.


## Re-Grounding 2026-11-25

Re-grounded by the cleanup of 2026-11-25 (read-only sub-agent, contradictions spot-checked by the orchestrator) at `3dea04b2`, against the code and the design and specification changes since 2026-11-15: #570 (`20f971fe`: `over_size`, `epic.overlap_found`, `stale`→`unreadable`, the phase `sanity-check`), pm-mcp-core #607/#614 and plan-marshall-documentation #201/#202 ("observe facts instead of predicting them", two rounds), clients #578 (protected project configuration waits for the operator's decision) and the inbox messages `doc-review-2026-11-17-001` and `doc-review-2026-11-24-001`. **This section binds over the body where they differ.**

- Claims 2, 4 contradicted: `MachineSettingsView.svelte` is gone (`/settings/machine` → `?tab=setup`); the pairing and revoked views render inside `AppShell`, which always mounts the rail and the bottom bar (`+layout.svelte:336-390`, `AppShell.svelte:35,45`).
- **D1 is deleted** (option A, 2026-11-10: both controls stay in the rail foot and the More sheet, `README.adoc:150,156,426`); `ThemeChoice.svelte`, `LocaleSwitch.svelte` and `BrowserSettingsView.svelte` leave the surface. The `setTheme`/`pm_theme`/`dataset.theme` bullet moves to PLAN-70 D1.
- **D2** becomes the tab `browser` of the one `SettingsView` (`README.adoc:426,487,494`): add it to `SETTINGS_TABS`/`TAB_KEYS`, the panel (with `RadioGroup`), the `replaceLegacyAddress` rule for `/settings/browser`, texts `web.settings.browser.*`, and `machine-settings.adoc:60` (sixth tab). Drop `browser-settings.adoc`. Delete the Done-when "no document still says the control stands in the rail foot or More sheet" (the design requires it); replace by "`README.adoc:426,487` match the tab as built". State who owns this browser's dictation choice (`web.voice.browser_choice`): if the voice package has not landed, the tab ships theme and language only.
- **Operator decision before emit**: which duplicate toggle in `BottomBar` goes. Proposal: keep the More sheet toggle, make the frame-bar controls (`BottomBar.svelte:234-273`) the documented pre-pairing place, and fix `README.adoc:426`'s "no rail" sentence.
- **D3 RevokedView**: the wire carries channel, device and time for `device_revoked`/`device_expired` (`06-event-catalogue.adoc:26`); `RevokedInfo` drops the channel (`runtime.ts:66-71`) and the view resolves invented reason keys. Carry the channel and derive "in the terminal" / "from device X"; amend the rows of `entry.adoc:84-100` the wire cannot tell apart ("all other devices", "idle"); no test fed with invented keys.
- Removed from the Expected Surface (void or replaced, see above): pm-web-app/src/lib/views/BrowserSettingsView.svelte, pm-web-app/src/lib/views/BrowserSettingsView.test.ts.

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

- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/AppShell.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/ThemeChoice.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/LocaleSwitch.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/theme.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD 1cc0e10e (tab host; owned by PLAN-60 / PLAN-45)
- OBSERVED: `pm-web-app/src/lib/views/BrowserSettingsView.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/BrowserSettingsView.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/helpers/contrast.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/entry.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at HEAD 1cc0e10e

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Placement (option A):** the AGREED README stands: theme and language stay in the rail foot (desktop) and the More sheet (phone). This plan is reduced to: (1) the "This browser" tab `?tab=browser` of the one settings view (theme and language as `RadioGroup`, applied at once, stored under `pm_theme`/`pm_locale`, the sentence "Kept in this browser. It is not sent to the machine."), and (2) the duplicated language toggle of `BottomBar.svelte` (`:172`, `:215`) goes. Deliverable 1 of the re-grounding amendment (controls off the frame) is dropped; nothing moves to the pairing or revoked pages; `README.adoc`, `entry.adoc` and `theme-readability.adoc` are not changed for placement. The re-grounding's claim 6 (`setTheme` before `page.goto`) is PLAN-70's deliverable 1.
- Expected Surface reduced to outcome A (see the section above).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: the entry pages are restyled by PLAN-157 (no theme or language control is added there, option A).

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-60` (tab host), `PLAN-144` (`BottomBar`) and `PLAN-150` (`RadioGroup`). `PLAN-109` is met.
- Option A applies: only the tab and the `BottomBar` duplicate toggle.
- Claim 4 is refuted.
- The Write-Boundary still lists the old pairing/revoked surface; trim it at outline.
