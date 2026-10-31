# PLAN-80: Web: Frontend Standards Conformance (Domain Configuration, Scripts, Test Hygiene)

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make the web modules follow the frontend standards of the marketplace: `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`, `arch-gate-js`) and `pm-dev-frontend-cui` (`cui-javascript-project`). The sibling plan `PLAN-77` does the same for the Java modules. An analysis of the repository at HEAD 7e2b4b61 (2026-10-30) found the web modules largely in line (ES modules, flat ESLint config with the security plugin, Prettier, lockfiles tracked, no HTML-parsing sink in production code, the required npm scripts in `pm-web-app`) and these gaps: the `javascript` domain of `.plan/marshal.json` matches `*.js`, `*.mjs`, `*.jsx` and `*.css` only, so the 100 Svelte components and the TypeScript sources get no domain skill by file type, and the `pm-dev-frontend-cui` bundle is not configured at all; `pm-web-e2e` lacks the required scripts `test:ci-strict` and `quality`; six timer waits stand in tests; `any` or a type suppression stands in two files. The plan closes those gaps and writes the rule down. Every other web plan names the missing skills in its deliverables, so new and changed web code follows the standards from now on.

## Deliverables

1. **Domain configuration and the rule**
   - What: through the configuration verbs of `marshall-steward` (never by editing `.plan/marshal.json` by hand), make the `javascript` domain match `**/*.ts` and `**/*.svelte` as well, and configure `pm-dev-frontend-cui` as the additive domain for the web modules. Add a "Web Standards" section of its own to `CLAUDE.md`, directly after § Code Standards and not inside it (so it cannot conflict with the paragraph `PLAN-77` adds to § Code Standards when both run at once): ES modules; flat ESLint config only; no `innerHTML`, `outerHTML`, `insertAdjacentHTML` or `{@html}` with untrusted or model-written text (text-treating sinks only, a sanitizer is a dependency and needs the operator); no arbitrary timer wait in a test; the required npm scripts; the Svelte and SvelteKit layout under `src/` is the project's own structure and stands in for the standard types of `cui-javascript-project`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend-cui:cui-javascript-project
   - Done when:
     - The `marshall-steward` read of the domain configuration lists the `.ts` and `.svelte` globs for `javascript` and the `pm-dev-frontend-cui` domain, and the executor is regenerated afterwards (evidence order: `none`)
     - The paragraph is in `CLAUDE.md` and names the Svelte layout departure (evidence order: `none`)
2. **Scripts and Node version**
   - What: `pm-web-e2e/package.json` gets the scripts `quality` (lint and format check) and `test:ci-strict`; where `test:ci-strict` cannot be a plain test run because the Playwright suite needs a running mock and app, the script runs the guard `test:ports-guard` and the departure is written next to it. Check that `engines.node` of both `package.json` files (`>=22.17`, `>=22`) agrees with the Node version the frontend-maven-plugin installs (property `frontend.node.version`, set outside this repository by the parent POM): report both versions; a mismatch is fixed in `package.json`, never in the parent.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:lint-config, pm-dev-frontend-cui:cui-javascript-project
   - Done when:
     - `npm run quality` and `npm run test:ci-strict` exist and pass in `pm-web-e2e` (evidence order: `none`)
     - The two Node versions are recorded in the progress record (evidence order: `none`)
3. **Test hygiene**
   - What: remove the arbitrary timer waits: 4 `setTimeout` sites in `advisoryTitle.test.ts` and `EpicView.test.ts`, 2 `waitForTimeout` sites in `pm-web-e2e/tests/helpers/textGuard.js` and `contrast.js`; each becomes a wait on a condition (fake timers, an awaited event, a `waitFor`). Resolve the `any` and suppression sites of `Chip.test.ts` and `OverviewView.svelte` by typing them, or write the reason at the site.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-frontend:jest-testing, pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - A search for `setTimeout` in the unit tests and `waitForTimeout` in `pm-web-e2e/tests` finds none, and `npm run test:ci` of `pm-web-app` passes with the same number of tests or more (evidence order: `none`)
     - `npm run lint` of both modules passes with no new warning (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through npm scripts and Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`.
- No new dependency or plugin. In particular NOT added, and left to the operator's decision: `stylelint` (the `css` and `lint-config` skills expect it; the repository has 3 CSS files and component styles), `dependency-cruiser` (the `arch-gate-js` fitness check) and a sanitizer such as DOMPurify (no production code needs one). Recorded as a Watch.
- The test runner is Vitest with Testing Library, not Jest; the `jest-testing` standards (AAA, one behaviour per test, no arbitrary waits, DOM and component testing, mocking) are applied as method and the Jest-specific setup is not.
- Behaviour is unchanged; no rendered output changes. A test that fails after the change exposes a real timing defect; report it, never restore the wait.
- Browser and terminal checks do not apply: no rendered output changes. No ports are assigned.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the `javascript` domain of `.plan/marshal.json` has `file_globs` `**/*.js`, `**/*.mjs`, `**/*.jsx`, `**/*.css` and the bundle `pm-dev-frontend`; no domain names `pm-dev-frontend-cui` — read at `.plan/marshal.json` § `skill_domains`
- OBSERVED: `pm-web-app/src` holds 100 `.svelte`, 33 non-test `.ts`, 83 `*.test.ts`, 0 `.js` and 3 `.css` files; `pm-web-e2e/tests` holds 29 `.js` files — counted at HEAD 7e2b4b61
- OBSERVED: `pm-web-app/package.json` has the scripts `lint`, `format`, `test`, `test:ci-strict`, `quality`; `pm-web-e2e/package.json` has `test`, `test:ports-guard`, `lint`, `format` and no `test:ci-strict` or `quality` — read at the two files
- OBSERVED: both `package.json` files have `"type": "module"` and tracked lockfiles; both `eslint.config.js` are flat configs with `eslint-plugin-security`; `.prettierrc` exists in both — read at HEAD 7e2b4b61
- OBSERVED: no `{@html}`, `innerHTML`, `outerHTML` or `insertAdjacentHTML` in production sources of `pm-web-app` (three hits, all in `*.test.ts`) — searched at HEAD 7e2b4b61
- OBSERVED: `setTimeout` stands in `advisoryTitle.test.ts` and `EpicView.test.ts` (4 sites); `waitForTimeout` in `pm-web-e2e/tests/helpers/textGuard.js` and `contrast.js` (2 sites) — searched at HEAD 7e2b4b61
- HYPOTHESIS: the `any`/suppression search (28 hits in `Chip.test.ts` and `OverviewView.svelte`) counts type words in a test double and a few annotations, not 28 defects — confirm/refute at those two files (verify-at-outline)
- HYPOTHESIS: `frontend.node.version` of the parent POM satisfies `>=22.17` — confirm/refute at the parent `pm-mcp-parent` POM resolved by Maven (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-39` (running) edits `+layout.svelte` and `workspace.spec.js`, and other web plans edit tests, so re-count the sites. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `.plan/marshal.json` — present at HEAD 7e2b4b61
- OBSERVED: `CLAUDE.md` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-e2e/package.json` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-app/package.json` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-app/src/lib/utils/advisoryTitle.test.ts` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-app/src/lib/views/EpicView.test.ts` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-app/src/lib/components/Chip.test.ts` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-e2e/tests/helpers/textGuard.js` — present at HEAD 7e2b4b61
- OBSERVED: `pm-web-e2e/tests/helpers/contrast.js` — present at HEAD 7e2b4b61

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: `PLAN-77` (`CLAUDE.md`: `PLAN-77` adds a paragraph inside § Code Standards, this plan a section of its own after it; the two edits touch different lines, and either order merges cleanly; run one after the other or merge by hand); `PLAN-09`, `PLAN-52` (`OverviewView.svelte`)
- Adjacent to: `PLAN-77` (the Java counterpart)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-80-web-standards-conformance.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
