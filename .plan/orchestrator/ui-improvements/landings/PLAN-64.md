# Landing Analysis: PLAN-64 — Every onboarding state has a screen

epic: ui-improvements
workstream: WS-03
pr: #462 (https://github.com/plan-marshall/pm-mcp-clients/pull/462)

> Landing record, written by `analyze` on 2026-11-17 from the inbox message
> `rb-64-web-onboarding-screen-registry-001.md` (a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `2fc22f3d`.

Merged as `2fc22f3d20d174645513fbdc993065c20408c91d`, 33 files, +1777/-693. Sender `rb-64-web-onboarding-screen-registry`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 — registry, no `{:else if}` chain | shipped; the lane's "not met as written" is its tooling gap, not the spec | `screens.ts` holds the rules; `OnboardingView.svelte` keeps only the `isProjectOnboarding` and `loadFailed` branches; all 91 states of `onboarding.json` resolve to exactly one rule (leaf, hand-checked). At the base `screens.ts` does not exist, so test 1 fails at import: a `test-first` run was meetable, the base worktree executor was `absent` |
| D2 — missing screen at runtime | shipped | `MissingScreen.svelte`, `ScreenHost.svelte`; `OnboardingView.scenarios.test.ts:523` asserts heading, command, Back and the absence of the state id, and fails against the old card that printed `{stateId}`; `:545` covers `entry.lan-open` |
| D3 — entry states in settings | removed by the spec's Operator Decisions 2026-11-10 | confirmed by the operator at the start (lane, question 1) |
| PLAN-130 residue — brand mark below 768 px, bar height reserved | shipped, markup and CSS text only | `OnboardingView.test.ts:330` checks one 28 px mark and the stylesheet text (`display:none`, the `@media (max-width: 767px)` rule); jsdom evaluates no layout; the phone width is covered by screenshots only; pm-web-e2e is untouched |

**Files the diff touches that the Expected Surface does not list:** `ConsentBox.svelte` (the third answer `no_advice`, added after a CodeRabbit finding, with tests at `scenarios.test.ts:556,581`), `OnboardingView.test.ts`, `screens.css`, the design documents `onboarding.adoc` and `onboarding-texts.adoc`, the 22 screen components. All trace to the registry or the review.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; 33 files, 22 new components. The orchestrator re-read the points below on `2fc22f3d` (and the old chain at `2fc22f3d^1`).
- **Verdict:** layer 1 minor findings; layer 2 findings (two tests that cannot fail alone, a deviation from VD-S-06, six round-2 items confirmed plus one new); layer 3: no blocker, **1 major (re-read; conditional on the runtime), 1 major carried over**, 6 minor, 2 info.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | the Objective and title ("move `entry.*` into settings") match the Done-whens | checked | refutes | the Operator Decisions of 2026-11-10 supersede them; the Objective was never edited, the Done-whens are consistent once the clause is applied |
| L1-2 | "D3 is met" in the Unparked section | checked | refutes | "D3" there is the design-review brand package, "former deliverable 3" elsewhere is the entry states; the lane read it correctly |
| L1-3 | D1 and D2 `test-first` unmeetable | checked | refutes | satisfiable at the base (test 1 fails at import; the unknown-state assertion fails against the old card); the lane's gap was its executor in the base tree |
| L1-4 | the residue Done-when asks for a component or e2e test at a phone width | checked | refutes | no test sets a width |
| L1-5 | PLAN-130's premise (no mark on a phone) | checked | confirms | `TopBar.svelte:84` holds no `BrandMark`, `AppShell.svelte:74` hides the rail below 768 px |

**Layer 1 verdict:** findings, minor — a spec whose Objective contradicts its own amendment; sound once the amendment is read.

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| registry test 2 ("never the fallback") and test 4 | cannot fail alone | `screens.registry.test.ts:37,55`: test 2's filter selects exactly the ids test 1 already catches (`screenNameFor` returns `missing` for null, `screens.ts:312`); only tests 1, 5 and 6 can fail on their own |
| registry test covers the 91 scenario ids only | gap | a typo'd rule id or a dead rule goes unnoticed; ids named in the design (`auto.run`, `roles.propose`, `store.move`, `store.unavailable`, `harness.scan`, `store.detect`) are invisible to it |
| `MissingScreen` and the other 21 screens use hand-built `.state-card`, `.completed-row`, `<code>` | deviates from VD-S-06 | `MissingScreen.svelte:15`; `ErrorState` (read-only surface, PLAN-45) is not reused; undeclared in the narrative |
| the sticky-bar reserve | not shown by reading | `OnboardingView.svelte:345` adds `padding-block-end: var(--space-16)`; the bar sits in flow in a column flex frame (`AppShell.svelte:74`), so the reserve cannot be shown necessary; no test, no deliverable names it |
| catalogue | borne out | 8 keys added in en and de, all used; no orphan, no missing key. The texts design omits `onboard.harness.add.name` and `.executable` |
| round-2 items of the lane | all six **confirmed** | summary 3-column grid with two cells (`SummaryScreen.svelte:26`, `screens.css:91` `auto 8rem 1fr`); stale `ModeScreen.svelte:7` comment; `WorkingScreen.svelte:23` OR guard (prints "on  · " with one field; role defaults to the English "steward" under `de`); `TokensScreen.svelte:44` falls back to the harness-choice title; `status.only` absent from the prose (`screens.ts:15-19`); `CONSENT_ID` not frame-scoped (`screens.ts:120`, `wait.consent` and `settings.consent` resolve to `ConsentScreen`) |
| **new, not in the narrative** | minor | `TokensScreen.svelte:49-51` has the same two-cell `.completed-row`, its value sentence wraps in the 8rem column on `harness.tokens-*`; `auto.result` test asserts value and reason only, never the aspect column |
| the lane's `deliverables_done=0` of 3 | understates | D1, D2 and the residue shipped; the lane counts `not met as written` as 0 |

Not shown by reading: the 91 + 65 screenshots and the e2e runs (450 passed, 75 skipped at `4e75d715`), the text guard baseline (file not in the diff), the Playwright claims for the 13 states that show the load-failure card, Sonar beyond the MCP search.

**Layer 2 verdict:** findings — no blocker; the spec's `D1`/`D2` are delivered, the evidence order is the lane's.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `screens.ts:123-141` (re-read: `WORKING_STATES` lists `auto.scan` and the fixed ids, no `auto.run`, no `*.working`, no `*.probe` suffix rule; the old chain at `2fc22f3d^1:OnboardingView.svelte:55-60` took `.probe`, `.working` and `auto.run`) | the runtime reports `auto.run` or any `*.working` state outside the 91 of the mock | `screenFor` returns null, the operator sees "This screen is not available — run pm-operator" during the automatic run instead of the progress screen; the registry test stays green because the scenario lacks the id. Whether the real runtime emits it is not shown (`pm-mcpd` is not in this repository); the design `onboarding-steps-4-7.adoc:346,385` names `auto.run` | **major** (conditional) |
| L3-2 | `WorkingScreen.svelte:41` | any link of a working state, e.g. `harness.verify-profile`'s `edit` | every link is labelled "Continue without the advice" (`common.model.continue_without`) and posts that link; carried over from the old chain | **major** (carried over) |
| L3-3 | `AutoResultScreen.svelte:27` (re-read: the keys are `onboard.step.{language,harnesses,models,roles,credentials,providers,plans,done}`; the scenario's aspects are `language, harness, catalogue, roles, store, providers, defaults`) | `auto.result` with aspect `harness`, `catalogue`, `store`, `defaults` | no key resolves; `translate()` falls back to a humanized English word under `de`, and the words disagree with the step bar; the spec's "no raw enum" clause is not met in the strict sense | minor |
| L3-4 | `ConsentScreen.svelte:22` | a consent state offering neither `no` nor `decline` | "No" takes `links[0]`, which can be the accepting one; the registry now claims every future `*.consent` id (L2) | minor, latent |
| L3-5 | `OnboardingView.svelte:127` | `goBack()` with `window.history.length > 1` from a pairing page or another origin | Back on the fallback can leave the app or bounce; with length 1 it only re-reads the same state; no test of the Back act | minor |
| L3-6 | `StatusScreen.svelte:43` | `sonar.key`, `providers.credentials` | the title asks for a token, the screen offers none; the one link posts with no body (the lane names the selection bodies as follow-ups) | minor, not shown by reading |
| L3-7 | `MissingScreen.svelte:26` | any `settings.*`, `wait.*`, `web.pair` state | the prompt `$ ` is a literal outside the catalogue; the sentence "pm-operator opens the console at this step" is not borne out in this repository | minor, not shown by reading |
| L3-8 | the 22 screens | — | confirmed: no `innerHTML`, `{@html}`; the state id never reaches the DOM from the fallback; `ScreenHost` re-creates the component on identity change, no `$effect`, no leak | info |

**Layer 3 verdict:** findings — no blocker; 2 major (one conditional on the runtime, one carried over from the old chain), 5 minor, 1 info.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer waits; ES modules | conforms | no hit in the diff; `waitFor`, `findBy`, `getBy` only |
| CLAUDE.md Dependencies | none added without asking | conforms | no `pom.xml` or `package.json` in the 33 files |
| spec Standing | the design document is updated in the same PR | conforms, with a gap | `onboarding.adoc`, `onboarding-texts.adoc` edited; two added keys not listed |
| spec Standing | design system v2 components (VD-S-06) | deviates | hand-built markup in every screen |
| spec Standing | server values through the display layer and formatter | not shown by reading | none of the screens imports `../../display`; whether model or runtime sentences are exempt is not stated |
| spec Standing | text guard baseline only shrinks | not shown by reading | the baseline is not in the diff |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | re-read: the 33 files carry no `.plan` path |

## Metrics and Anomalies

- Tokens: `unknown` (unobserved, not zero); `plugin_version` `unknown`; executor `0.1.2135`.
- `landing-check`: `complete: false`, `missing_keys` `total_tokens`, `plugin_version`. Recorded as an Open Defect.
- Lane rule breaks the lane owns: the PR was opened before self-review round 1; the merge came after round 1 and round 2 was dispatched only afterwards, so round 2's open items were not waited for (`self-review: budget-exhausted`); one `sleep 1`; a `cp` of a prepared body; the browser sweep ran through `browser_run_code_unsafe`; a full `verify -Ppre-commit` ran with mutation-after edits in the tree.
- The mailbox could not be read at the address `resolve` emits (`invalid_target_plan`, a plan id may not start with a digit); the third lane to report it, for the plan-marshall repository.
- The merge queue dequeued #462 once (merge-group build hit Maven Central 429); the operator answered "re-enqueue now".

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `46252917` on its own (10 inline findings: two fixed in `4e75d715`, eight answered with a reason, all threads resolved); re-triggers refused on its quota. Re-read: `review-gate --pr-number 462 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=10` (its `handled:0` is the gate lacking `--disposition`). The merge conforms to the standing rule (one CodeRabbit review on record; later heads fine).
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull request 462 returns no issues.
- `.plan`: #462 carries no file under `.plan` (re-read).

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-64 --status shipped`
- [x] row `pr` stamped — `462`
- [x] row `landing` stamped — `landings/PLAN-64.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-64-web-onboarding-screen-registry`
- [x] epic.md: Open Defect `PLAN-64 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-64 residue plan** (named, NOT staged): L3-1 (`auto.run` and the `.working`/`.probe` suffix states, and a registry test that reads rule ids and the design's state ids back), L3-2 (link labels per link), L3-3 (aspect labels), L3-4 and the consent frame scope, the Back act, the summary and tokens grids, the stale comments, `WorkingScreen` guard, `TokensScreen` title, a test at phone width, the `ErrorState` question.
- **PLAN-66** (`Emit after PLAN-64`) loses its gate; its spec claim "views/onboarding/ still absent" is stale (the directory now exists with 22 screens and the registry): re-ground at outline. PLAN-66 adds screens for states that the registry must claim: it builds on the registry's L3-1 and consent-scope defects.
- PLAN-130 residue is delivered here (markup and CSS only).
