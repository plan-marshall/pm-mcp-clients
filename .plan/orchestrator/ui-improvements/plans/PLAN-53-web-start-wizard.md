# PLAN-53: Web: Start as a Wizard With a Working Gate

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make starting work a wizard (UX-P-07) with one primary act per step, Back on every step, a setup gate with progress and a time limit, the error pattern for an unknown project, and a lazy entry (UX-S-10, start part of UC-S-09; UC-P-07).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-10)`
- `doc/claude_review/ux/proposals.adoc#UX-P-07`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-07`
- `doc/claude_review/use-cases/findings-projects-starting.adoc`
- `doc/claude_review/ux/findings.adoc#UX-F-12`

## Deliverables

1. **Gate**
   - What: The start page sits on the `WizardPage` template of `PLAN-43` (this plan moves it there: `PLAN-43` no longer does). "Checking the project's setup…" with a spinner; after 20 s the cause and "Try again"; a refused gate with its reason and act; a start for an unknown project shows the error pattern with "Choose a project".
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `start.spec.js`: a gate that does not answer shows its cause after 20 s; `/start` without a project (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `projects-and-start` → `start.sources`, `start.refused`; `/start` without a project; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Steps 1 to 3**
   - What: What (two cards), Sources (source cards with state; one "Add a source" menu; the description as a source card with one "Add"), Ready (title, id, sources, what happens next; primary "Start the plan"); footer Back · Discard (overflow) · primary.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `start.spec.js`, `git-sources.spec.js`, `lesson-source.spec.js`: one primary per step; Back works (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `projects-and-start` → `start.sources`, `start.refused`; `/start` without a project; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
3. **Lazy entry**
   - What: The entry is created when the first source is added; leaving without a source leaves no entry.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A unit test of `lib/stores/start.ts`; e2e on `projects-and-start` → `start.sources` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `projects-and-start` → `start.sources`, `start.refused`; `/start` without a project; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20530 PM_WEB_PORT=20531 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20530`, app port `20531` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20530"`), then `PM_MOCK_WEB_PORT=20530 ./pm-web-app/start.sh -p 20531` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20530/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-10)`, `doc/claude_review/ux/proposals.adoc#UX-P-07` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: UX-S-10 at ux/plan.adoc:68, UX-P-07 at proposals.adoc:284, UC-P-07 at use-cases/proposals.adoc:138 present
- HYPOTHESIS: the start view hangs on "checking" after a jump to a `start.*` state (UC-F-36) because the gate has no time limit — confirm/refute at `pm-web-app/src/lib/stores/start.ts` (verify-at-outline)
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: Client bounds every request at 15 s (client.ts:2199,2325; 9fb2e7fa #399) and StartView.svelte:752-766 shows Skeleton then ErrorState with retry; no 20 s gate bound; stores/start.ts is modal state only | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): Line references in the hypothesis moved (StartView.svelte load() at 120-163, client.ts request() at 3036); StartView.svelte grew to 1327 lines and now shows start.state.checking for the documents source (line 343) too; SourceList.svelte, start properties and starting-work-texts.adoc changed since the anchor.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Drift only: StartView.svelte now 1425 lines, load() at 139-185, request() near client.ts:2325, start.state.checking at :1142 and :1183
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural verify-first instruction; no code fact
- HYPOTHESIS: the entry is created by `openEntry` before any source exists and the gate refusal creates none, so a lazy entry needs a split of that call — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `openEntry` and `doc/design/shared/starting-work.adoc#steps` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: StartView.svelte:156 calls openEntry on load before any source; client.ts:2432 POST /projects/{id}/entries; refusal is a resource field with no entry; starting-work.adoc:184 says leaving keeps the entry
- HYPOTHESIS: no `StartView.test.ts` and no `start.test.ts` exist, so the gate and lazy-entry tests are new files — confirm/refute at `pm-web-app/src/lib/views/` and `pm-web-app/src/lib/stores/` (verify-at-outline)
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: StartView.test.ts (68 lines) and StartView.scenarios.test.ts exist at HEAD; only stores/start.test.ts is absent | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/SourceList.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/SourceIntakePanel.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/RecipeSelector.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/GitSourceIntakeModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/LessonSourceModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/start.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/start.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/start.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/git-sources.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/lesson-source.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/starting-work.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/starting-work-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/StartView.scenarios.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/templates/pageWidth.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-45` (`web-error-loading-pattern`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 007, 008, 009). **Gate: D1
merged** (document `AGREED`) and the D1 foundation package; its chat part waits for the D4 web chat package. Queue status:
`parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT widened now (a parked
row is not in the disjointness gate population). When restarted:

- `Button` on `WizardPage`, one primary per step, the duplicate "Choose" / "Add" labels resolved (007).
- Fields via `Field`, Enter submits, errors under the field, documents mode as the shared `RadioGroup` (008).
- The start-entry chat in `Chat` (009), with `control.chat.*` keys rather than reused `start.*` keys.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 1 contradicted: Client bounds every request at 15 s (client.ts:2199,2325; 9fb2e7fa #399) and StartView.svelte:752-766 shows Skeleton then ErrorState with retry; no 20 s gate bound; stores/start.ts is modal state only
- Claim 5 contradicted: StartView.test.ts (68 lines) and StartView.scenarios.test.ts exist at HEAD; only stores/start.test.ts is absent
- Already done in part: the client bounds every request at 15 s and StartView shows Skeleton → ErrorState with retry (#399); the 20 s figure and "Choose a project" are not done. `StartView.test.ts` and `.scenarios.test.ts` exist.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-53-web-start-wizard.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/shared/starting-work.adoc` (#steps, #sources, #description, #documents, #chat, #ready, #leaving), `doc/design/web-app/components.adoc` (#hierarchy, #templates), `doc/design/web-app/components-fields.adoc` (#form-shape, #validation, #choice). HEAD 1cc0e10e.

1. **The gate.** `StartView.svelte` sits on `WizardPage` (720 px). The gate shows "Checking the project's setup…" (`start.state.checking`) with a spinner; after 20 s the cause and _Try again_ (a client bound, with a signal passed to `request()`; the `ErrorState` and loading pattern of `PLAN-45`); a refused gate shows what is missing and its one act; `/start` without a project shows the error pattern with _Choose a project_.
   - Done when: `start.spec.js`: a gate that does not answer shows its cause after 20 s; `/start` without a project (evidence order: `test-first`); the browser check on `projects-and-start` → `start.sources`, `start.refused` (evidence order: `none`).
2. **Sources and Ready.** ① Sources: for a plan first `start.choose` (from sources / a recipe, a recipe disabled with its reason); then the list of sources with the add buttons of the design; the description is an editor with one _Add_; _Continue_ names what it waits for; ② Ready: title and id editable, _Start the plan_ the one primary. Footer: _Back_ ghost left, _Abandon_ in the overflow, the primary right; one primary per step; no two acts with the same label. Fields use `Field`; the documents mode is a `RadioGroup`.
   - Done when: `start.spec.js`, `git-sources.spec.js`, `lesson-source.spec.js`: one primary per step; Back works; Abandon asks first (evidence order: `test-first`); the browser check (evidence order: `none`); the open-issue read at the head (evidence order: `none`).
3. **Lazy entry — conditional.** Created only if the contract allows the gate to be checked without an entry: the entry is created when the first source is added and leaving without a source leaves none. If not, this deliverable is dropped and the plan says so.
   - Done when: a unit test of `lib/stores/start.ts` (a new file `start.test.ts`) and an e2e on `projects-and-start` → `start.sources` (evidence order: `test-first`).
4. **Chat about the request** — only if `PLAN-53` and not D4W adopts it (see the ownership note below). The chat is the `Chat` side column of `WizardPage`, opened at ①, disabled with its reason until a source is stored, with `control.chat.*` keys.

Ownership: the start flow's components (`SourceList`, `SourceIntakePanel`, `RecipeSelector`, the source modals) belong to this plan, not to the views package. The chat of the start entry has two owners in the designs (`chat.adoc#rely`); the orchestrator names one.

Removed or replaced: a numbered "What" step (it is `start.choose` inside ①); "Add a source" as one menu; _Discard_ (it is _Abandon_).

Implements: `doc/design/shared/starting-work.adoc` § Steps, The List of Sources, Leaving and Coming Back; `doc/design/web-app/components.adoc` § Hierarchy.

Dependencies: D1 foundation (Button hierarchy, Field, RadioGroup), `PLAN-45` (merge first), D4 web chat package (deliverable 4 only). D2 navigation engine is soft (the wizard is not a column chain; the routes `/start`, `/projects/{id}/start`, `/entries/{id}` already exist).

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Chat adoption:** `PLAN-137` (web chat places) replaces every `ChatPanel` call site and the project-onboarding chat drawer and deletes `ChatPanel`. This plan drops its own chat deliverable or chat lines; it only places the `Chat` side column in its redesigned layout where the design shows it.

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-150`, and after `PLAN-155` and `PLAN-156`, to avoid collisions on `StartView` and its modals.
- Re-ground at outline: claims 1 and 5 are contradicted. The 15 s bound, `Skeleton`/`ErrorState`/retry (#399) and the `StartView` tests exist. The 20 s figure is not done.
