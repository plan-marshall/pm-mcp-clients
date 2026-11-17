# Landing Analysis: PLAN-66 (deliverables 1–4) — Web: Onboarding Frame and Roles

epic: ui-improvements
workstream: WS-03
pr: #467 (https://github.com/plan-marshall/pm-mcp-clients/pull/467)

> Landing record, written by `analyze` on 2026-11-17 from the inbox message
> `rb-66-web-onboarding-frame-and-roles-001.md` (a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `735318df`.
>
> **A split landing.** The spec has ten deliverables and its Scope note licenses splitting by deliverable ("1 to 4, 5 to 7, the rest"). This pull request carries **deliverables 1 to 4**; **5 to 10 are open** (`harness.none`, `auto.result`, `language.choose`, `catalogue.select`, `store.choose`, `harness.choose`) and are not counted as defects of this landing.

Merged as `735318df95fabee678fc8538960b781a84b7568b` (2026-11-17T18:02:30Z), 21 files, +2038/-334. Sender `rb-66-web-onboarding-frame-and-roles`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated). The lane reports `deliverables_done=4` of 10 and `self-review: budget-exhausted` (three rounds; one item left open by the operator's decision).

## Deliverable Fidelity vs Spec

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D1 — the frame until onboarded | shipped | no navigation before onboarding: `TopBar.svelte:60,95-108`, `NavRail.svelte:36-41,113-132` (inert brand), `AppShell.svelte:47-49` (palette not mounted); the mode screen marks no step (`runtime.ts:229`; `runtime.test.ts:1208`); pinned by `onboarding.spec.js:48` (the lane's failing run found no `brand-inert` at base; the destination-testid loop was already true at base) |
| D2 — `defaults.review` | shipped, one clause partly | axes with name, meaning and plain-name variants, no raw unit id (`DefaultsScreen.svelte:117-136,226-239`; `OnboardingView.scenarios.test.ts:356`, non-vacuous); posture on a later run only (`:146-150`); untouched sends nothing, a changed line sends only the changed axes (`:197-207`; `:398,438`). **Partly:** "variants that need a missing provider disabled with the reason" is client logic (`:151,175-194`; tests `:398,467`) with no producer: the mock carries no `unavailable` and no engine spec names it; the lane reported this gap. The `posture: null` client branch (`:204`) has no web test |
| D3 — `roles.review` | shipped | grouped by phase, departures marked with Accept and Reject, advisory, change per role, an open role empty and assignable with the record button waiting (`RolesReview.svelte:133-137,188-198,236-252,349-352,370-404,427-452,518-531`; tests `:510,636,772`). Weak: "Ask the steward" asserts the text, not the call; a stale decision for a role that vanished keeps its decision (L3-4) |
| D4 — `providers.failed` | shipped | Provider, cause, fix; "Enter the token again" opens `CredentialEntry` for the state's `provider_id`; "Check again" and "Continue without" are the state's links (`ProvidersFailedScreen.svelte:29-86`; tests `:714,752`) |

Browser checks (every deliverable): not shown by reading. Constraints: VD-S-06 components only (Button, RadioGroup, StatusLabel, SelectField, TextField; no emoji, no own width); en/de key sets identical with the same placeholders (the leaf diffed them); text guard baseline untouched; AspectChoice's `roles` and `defaults` branches removed with no dangling caller (`StoreScreen`, `ProvidersScreen` keep `store` and `providers`); three design documents updated.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf — reason: the Dispatch Decision Rule; 21 files, 549 lines of scenario tests and a 648-line new component. Build logs and screenshots were not opened. The orchestrator re-read the contract points below on `735318df` and in the sibling repository `pm-mcp-core`.
- **Verdict:** layer 1 minor (one clause partly); layer 2 one contradicted lane claim and **one contract skew**; layer 3: no blocker, **1 major** (the contract skew has two faces), 9 minor, 2 leads.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | variants needing a missing provider are disabled with the reason | checked | partly | no state carries `unavailable`; nothing greys; no rendered evidence (the lane's residue 1, confirmed) |
| L1-2 | `defaults.validate` marks a refused axis with its reason (the Plans section of the design, unchanged) | checked | refutes | no client code for it |
| L1-3 | the token entry is a `SecretField` (the amendment) | checked | refutes | `CredentialEntry.svelte` uses a bare `<input type=password>`; `SecretField` is used only in `SettingsView`; the secret handling itself is sound (a local variable cleared after a save, nothing logged, `encodeURIComponent` on the key) |
| L1-4 | server values through the display layer | checked | partly | `role.role`, model ids and unknown harness ids print raw (`RolesReview.svelte:357,361,217-219`); `cause`, `fix`, `reason` print as plain text |
| L1-5 | the design document is updated in the same PR | checked | partly | three documents changed; stale lines below |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| the lane's residue 3: "the mock's step path does not persist `landing`/`method`/`asking` for `defaults.reconfigure`" | **not borne out** | re-read: `MockRuntimeEngine.recordPlanDefaults` persists `landing`, `method`, `asking` and the posture (`:8404-8420`); the leaf finds it called for `defaults.reconfigure` at `:8379` (not re-read by the orchestrator); the residue is stale or names a different path |
| residues 1, 4, 5 | borne out | no `unavailable` in the mock; `roles.reconfigure` carries one role; `deriveStepFromState(undefined)` returns 1 (`runtime.ts:226-227`) and the store's initial `activeStep` is 1 (`:185`): the mode screen shows step 1 until the first state loads, not for one frame |
| the frame, the inert brand, step 0, the untouched-sends-nothing rule, the posture, the role grouping and origins, the record button, the three acts | borne out | see the table |
| the PR says the mock "carries the new facts (`phase`, `class`, `advisory`, `departure`, `provider_id`)" | **true and the whole problem** | see the contract skew |

**Contract skew (re-read).** The new state facts exist only in the mock scenario (`pm-mock-runtime/.../onboarding.json`), in the web client's own local interfaces (`RoleRow`, `Departure`, `Advisory` in `RolesReview.svelte:22-53`, `Unavailable` in `DefaultsScreen.svelte:38-42`; `OnboardingState` is an index signature, `client.ts:1300`) and in the design this PR wrote (`onboarding-steps-4-7.adoc`). The real contract: `pm-api`'s `RoleAssignment` has `role`, `harnessId`, `modelId`, `effort`, `origin` (`default`, `steward`, `changed`) and nothing else (re-read: `RoleAssignment.java:22-34`); `PlanDefaults` has `landing`, `method`, `asking`, `posture` and no `unavailable`; no `pm-api` type has `provider_id` (re-read: no hit for `provider_id`, `departure`, `advisory` in `pm-api/src/main`); the engine's state catalogue (`pm-mcp-core/doc/specification/state-catalogue/19-onboarding-aspects-models-providers.adoc:92-103,273,326-331`) names none of these fields and no record body `departures[]` or `roles[]`; the mock engine has no handler for those record bodies. What a real runtime would send, and what the screens then do: no `phase` → one "Other roles" group; no `class` → every role "other"; no `advisory` → the Advisory button is a silent no-op (`:454`); no `departure` → no Accept or Reject although `origin: steward` still reads "Departure from the steward"; no `default` → Reject labels the row "Departure rejected" and leaves the steward's values (`:192-197`); no `unavailable` → nothing greys; no `provider_id` → no token entry (a deliberate, tested fallback).

Not shown by reading: the failing-run logs, the browser captures, the CI runs; the daemon's code is not in this checkout, so what it emits beyond the catalogue is not claimed.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ProvidersFailedScreen.svelte:77-86`, `OnboardingView.svelte:82-86`; catalogue `19-…adoc:273` (re-read: `providers.credentials` → `providers.failed (fix_credential)`; the design's terminal side lists `fix_credential` too, `03-dialogues.adoc:118`); re-read: `onboard.link.fix_credential` and `onboard.link.change` exist in neither properties file (only `onboard.link.check_again` of the three) | the real daemon's `providers.failed` with a `fix_credential` link; `defaults.review` and `roles.review` with the catalogue's `change` link | "Enter the token again" plus a lowercase English "fix credential" button, in German too, for the same act; `change` renders as a dead, unlabelled button (`DefaultsScreen.svelte:214-215`, `RolesReview.svelte:310-312` route every non-record link to `otherLinks`) | major (against the real link ids; the daemon was not read) |
| L3-2 | the contract skew above | the real runtime | the departures, advisories, phase groups, greyed variants and the token entry never appear unless the producer side grows; the record bodies `departures[]` and `roles[]` are exercised against nothing | major (a decision, not a code defect) |
| L3-3 | `RolesReview.svelte:71,221-227,289-292,300` | an effort outside low, medium and high (the engine spec defines the effort as what the harness accepts for the model, `pm-mcp-core/.../model-work/01-roles-and-configuration.adoc:84`) | the raw key `onboard.roles.effort.<value>` prints in the row and the proposal line, and the select cannot show it (lead: no harness effort list read) | minor |
| L3-4 | `RolesReview.svelte:96-104` against `:315-322` (re-read: only roles still present are cleared; `record()` sends every key of `decisions`) | a role that disappears after a refresh | its decision still travels as `{role, decision}` | minor |
| L3-5 | `RolesReview.svelte:415-424,454` | a row without `advisory` | the Advisory button is always shown, `aria-pressed` flips, nothing opens | minor |
| L3-6 | `onboard.roles.review.save` (en "Save {count} roles", de "{count} Rollen speichern") | one role (the mock's `roles.reconfigure`) | "Save 1 roles": no plural form | minor |
| L3-7 | `AppShell.svelte:47-49`; no test | removing the `{#if}` that keeps the command palette out of the frame | fails nothing: the palette-before-onboarding fix is untested; `TopBar` has no test file; "Ask the steward"'s call and `posture: null` are unasserted | minor (test gaps) |
| L3-8 | `RolesReview` Change and Advisory buttons, the Accept/Reject choice | screen reader and keyboard | every row's buttons share the names "Change" and "Advisory" (the per-role text is a `title`); no pressed state on Accept/Reject; focus does not move into the opened edit or token field | minor (a11y) |
| L3-9 | `onboard.link.continue_without=Ohne fortfahren`, `onboard.roles.class.other=sonstige` | German | terse, a bare adjective; readable | minor |
| L3-10 | the primary button on `providers.failed` (`OnboardingView.svelte:75-78`) | the mock's link order (check_again, continue_without) | "last link = primary" makes "Continue without" the primary act; inherited, not new | minor |
| L3-11 | `RolesReview` component state (lead) | a refused `roles.validate` and back | the screen remounts and hand edits and decisions vanish unless the daemon echoes them | minor (lead) |
| L3-12 | `CredentialEntry` after a save | a saved token | the entry stays open and nothing triggers "Check again" | minor |

**Layer 3 verdict:** findings — no blocker; one major in two faces (L3-1, L3-2); 9 minor, 1 lead.

**Stale design lines:** `doc/design/web-app/onboarding-steps-4-7.adoc:26,30,33,62` (the class as raw `read_only`, `zero_tool`; the screen prints words), its Check section (the sketch order `[ Continue without ] [ Check again ]` against the state's link order), the Plans section's "a refused line returns to `defaults.review` with the axis marked ✗ <reason>" (no client code), and the new `provider_id`, `unavailable`, `departure`, `departures[]` stated as state facts that no engine spec names; `onboarding-texts.adoc` (about line 60) omits `onboard.roles.review.advisory_for`, `.change_for`, `.refused` and `onboard.defaults.review.choose_available`. `onboarding.adoc:41` is consistent with the code.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer waits; ES modules | conforms | none in the added lines (the leaf) |
| CLAUDE.md Dependencies | none added | conforms | no `pom.xml` or `package.json` in the file list |
| spec Standing | VD-S-06 v2 components | conforms | L1-3's `<input type=password>` is pre-existing in `CredentialEntry` |
| spec Standing | values through the display layer | partly | L1-4 |
| spec Standing | the design document is updated in the same PR | partly | stale lines above |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | the lane states none; #467's file list has no `.plan/orchestrator` path (re-read: `git show --stat 735318df`) |
| the engine's contract (`pm-mcp-core`) | the client follows the published state shape | **deviates** | the skew above |

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `unknown`; executor `0.1.2135`. `landing-check`: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`; `surface_delta` `unmeasured`.
- Self-review: three rounds (11, 9, 9 findings; 8 of the third fixed); the operator chose "Proceed and report the gap" for the open item. Sonar `java:S3776` on `recordAspectAnswer` at `8b7010b4` was fixed in `c6e9a47c`.
- Flakes, each re-run green: `pm-operator OverviewFullScreenTest`, `pm-api EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` (generator seed), the axe `settings and designer` audit, the chrome command-palette test. 39 builds, all `daemon_longpoll`, no fallback.
- The lane's rule breaks: two shell calls chaining commands (corrected); no guard enforced on this harness. The `PLAN-66` lane used the `NO_PLAN` default body slot for its pull-request body: the ledger lands used their own slots meanwhile.
- The mailbox read with a digit-leading plan id is refused: not reported by this lane.

## Routing and Merge Behavior

- Review: CodeRabbit, 7 inline findings, all fixed in `ef6a0bbf`, one refuted with a stated reason; re-read: `review-gate --pr-number 467 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=7` (its `handled:0` is the gate lacking `--disposition`). Its qualifying review is at `44eb9c37`; later heads were not re-triggered (`review_complete_after_handling`). Sourcery refused on quota, optional.
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull request 467 returns no issues.
- Merged through the queue.

## Reconciliation Actions

- [x] the split recorded by the operator's answer (2026-11-17, "Back to staged for D5-10"): row `status` `running` → `staged` → `launched` (the emit of the remainder); `pr` stamped `467`, `landing` stamped `landings/PLAN-66.md` and `plan_marshall_plan_id` stamped `rb-66-web-onboarding-frame-and-roles`, all for deliverables 1 to 4; the next landing restamps them; the spec carries the Emit Amendment 2026-11-17
- [x] epic.md: Open Defect `PLAN-66 (D1-D4) landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-66 residue plan** (named, NOT staged): the real link ids `fix_credential` and `change` (L3-1), the effort set (L3-3), the stale decision (L3-4), the no-op Advisory (L3-5), the plural (L3-6), the palette and `TopBar` tests (L3-7), the accessible names (L3-8), the stale design lines.
- **The contract decision (operator):** the engine's state catalogue and `pm-api` do not carry `phase`, `class`, `advisory`, `departure`, `default`, `unavailable`, `provider_id` or the record bodies `departures[]` and `roles[]`. Either the daemon's specification and `pm-api` grow them (a change in `pm-mcp-core` and here, before the screens are of use against the real runtime), or the screens are cut back to the published shape. Nothing was staged.
- **Deliverables 5 to 10** are open. `auto.result` (D6) lands in the registry whose known residue is PLAN-64's (`WORKING_STATES` lacks `auto.run` and the suffix rules; every working-state link labelled "Continue without the advice"): that residue should come before or with D5 to D10, as the ledger already said.
