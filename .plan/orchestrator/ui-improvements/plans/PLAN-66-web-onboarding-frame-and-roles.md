# PLAN-66: Web: Onboarding Variations (Frame, Roles, Providers, Harness, Language, Models, Store)

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Scope note (cleanup 2026-11-02): 10 deliverables exceed the split guard (about 6). This is a WEAK MERGE of the structure review of 2026-10-30 (the former PLAN-68 and PLAN-69 folded in); the lane is licensed to split it by deliverable at outline (1 to 4, 5 to 7, the rest) and says so in the progress record. Keep each pull request under 100 files.

## Objective

Add the onboarding variations of UC-P-03 (UC-S-04): the frame until onboarded, `defaults.review`, `roles.review`, `providers.failed` (deliverables 1 to 4), then `harness.none`, `auto.result`, `language.choose` (5 to 7, the former `PLAN-68`) and `catalogue.select`, `store.choose`, `harness.choose` (8 to 10, the former `PLAN-69`). The three plans were one chain cut by count ("part 1, 2, 3") and ran strictly in series on the same registry; the structure review of 2026-10-30 merged them into this one. Each deliverable is a screen with its own test-first test and its own browser check, so the work can still be shipped in steps by the lane.

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/plan.adoc (UC-S-04)`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-03`
- `doc/claude_review/use-cases/findings-onboarding-entry.adoc`

## Deliverables

1. **Frame**
   - What: Until onboarded, the header shows brand, machine, language and theme only; the step bar marks no step on the mode screen.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `onboarding.spec.js`: no navigation before onboarding is done (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **defaults.review**
   - What: Per axis: name, one sentence of meaning, a choice of its variants with plain names, variants that need a missing provider disabled with the reason; posture only on a later run.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `defaults.review`: no raw unit id (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
3. **roles.review**
   - What: Roles grouped by phase; departures marked; an advisory per role; a change per role (harness, model, effort); "Ask the steward" with accept or reject per departure; an unmet role empty and assignable.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `roles.review` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
4. **providers.failed**
   - What: Label "Provider"; acts "Enter the token again" (`CredentialEntry`), "Check again", "Continue without".
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `providers.failed` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
5. **harness.none** (from the former `PLAN-68`, superseded: the three onboarding variation plans were a chain cut by count, not by topic)
   - What: Per harness found: name, path, the check that failed, the answer received, the fix.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `harness.none`: no `Machine · harness.none` text (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `harness.none`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
6. **auto.result** (from the former `PLAN-68`)
   - What: A "Change" link per row that opens the sub-workflow of that aspect.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `auto.result` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `auto.result`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
7. **language.choose** (from the former `PLAN-68`)
   - What: After recording, the app locale follows the interface language the onboarding recorded; the browser's own language choice (offered on the pairing page and in Settings → "This browser", `PLAN-85`) overrides it for this browser and says so in its advisory. (The former text named a header toggle; the control no longer stands in the frame.)
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `onboarding.spec.js`: after "Use Deutsch for both" the next screen is German (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `language.choose`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
8. **catalogue.select** (from the former `PLAN-69`)
   - What: The advice's recommendation preselected and marked "recommended" with its reason; a text field per harness that cannot list its models.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `catalogue.select` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `catalogue.select`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
9. **store.choose** (from the former `PLAN-69`)
   - What: Per option two lines: "Protects a secret against: …", "Does not protect against: …".
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A screen test on `store.choose` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `store.choose`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
10. **harness.choose** (from the former `PLAN-69`)
    - What: Monogram when no icon is fetched; the token line says "usage complete", "usage partial" or "usage not reported".
    - Persona: plan-marshall:persona-implementer
    - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
    - Done when:
      - A screen test on `harness.choose` (evidence order: `test-first`)
      - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `harness.choose`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20660 PM_WEB_PORT=20661 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20660`, app port `20661` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20660"`), then `PM_MOCK_WEB_PORT=20660 ./pm-web-app/start.sh -p 20661` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20660/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/plan.adoc (UC-S-04)`, `doc/claude_review/use-cases/proposals.adoc#UC-P-03` — read at `doc/claude_review/use-cases/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: use-cases/plan.adoc:41 UC-S-04; proposals.adoc:47-48 UC-P-03 present
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): pm-web-app/src/lib/views/onboarding/ still absent (created by this plan); all listed present files still exist, including LanguageChoice.svelte; onboarding properties, ModelsChoice.svelte, OnboardingView.svelte and +layout.svelte changed since the anchor.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: views/onboarding/ still absent; OnboardingView, Aspect/Harness/Language/Models Choice, CredentialEntry, onboarding.spec.js, 3 onboarding adoc, 2 properties all tracked
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first clause; no code fact to settle

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/onboarding/` — present after PLAN-64 (created by PLAN-64); absent at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/LanguageChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ModelsChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/CredentialEntry.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding-steps-4-7.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-64` (`web-onboarding-screen-registry`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 005, 007, 008). **Gate: D1 and
D3 merged** (documents `AGREED`) and the D1 foundation / D3 brand assets packages (restarted with `PLAN-64`). Queue status:
`parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT widened now (a parked
row is not in the disjointness gate population). When restarted:

- Until onboarded the header shows brand, machine, language and theme, so the brand there is the **chosen icon** of D3 (005).
- The token entry is `SecretField`, the per-harness model text field is `TextField`, the choice screens are `RadioGroup` /
  `CheckGroup`; the stray `/>` in `HarnessChoice.svelte:151-152` goes (008); "Abandon" through `ConfirmDialog` (007).
- The role `transcribe` in the roles step belongs to the D4 voice package.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Already done: D10's monogram fallback (cc5794c9, #255) — the usage wording is not. Still due: `HarnessChoice.svelte:151-152` stray duplicate `/>`; `SecretField`/`TextField`/`RadioGroup`/`CheckGroup` do not exist yet (PLAN-150).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-66-web-onboarding-frame-and-roles.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

- OBSERVED: `pm-web-app/src/lib/views/onboarding/` — present after PLAN-64 (created by PLAN-64); absent at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/LanguageChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ModelsChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/CredentialEntry.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding-steps-4-7.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 1cc0e10e

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-64`, in the same wave.
- Re-ground at outline: the monogram fallback is done.
- Still due: the stray `/>` at `HarnessChoice:151-152`, and the choices on the field set.
