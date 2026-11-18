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

---

# Second landing: deliverables 5 to 7 (#478)

> Written by `analyze` on 2026-11-18 from the inbox message `rb-66-web-onboarding-frame-and-roles-002.md` (a lead, not a fact), git, the provider, the engine's state catalogue in `pm-mcp-core` (which the lane did not read: its PR body says so) and a read-only three-layer leaf. The orchestrator re-read the contract points marked "re-read" in `../pm-mcp-core/doc/specification/state-catalogue/14-machine-onboarding.adoc` and on `origin/main` at `98ca84d0`.

Merged as `98ca84d0` (2026-11-18T04:42Z), 22 files, +1155/-71. The Emit Amendment of 2026-11-17 binds the run ("5 to 7 first, then 8 to 10"). The lane reports `deliverables_done=2` of 10 (counting this pull request's deliverables, D6 and D7; D5 it counts met for the published shape), `self-review: budget-exhausted` (three rounds), 9 CodeRabbit findings handled, Sonar 0. D8 to D10 (`catalogue.select`, `store.choose`, `harness.choose`) remain open and are not defects of this landing.

## Deliverable Fidelity vs Spec (second landing)

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D5 — `harness.none` | partly: delivered only against a mock shape | the screen lists each failed harness with its answer and fix (`HarnessNoneScreen.svelte:34-35,100-140`; test "names each harness that failed its check at harness.none…"); path and check when reported (`:109-121`). The clause's own literal (`no Machine · harness.none text`) cannot fail: `git grep "Machine ·"` at `98ca84d0^1` finds no app code, so the assertion holds by construction (`OnboardingView.scenarios.test.ts:967`; the lane says so); the test earns its red through the per-harness assertions, whose data is hand-written in the test and mirrors the mock. **The facts the screen reads are the mock's, not the engine's (layer 2).** |
| D6 — `auto.result` | shipped | a *Change* per row opening that aspect (`AutoResultScreen.svelte:28,44-45,83-96`; test "offers a Change per row of auto.result that opens that aspect again"); `onboard.link.change` = Change / Ändern with the accessible `Change Language` / `Sprache ändern`; PLAN-64's residue fixed: `auto.run` and the `.probe`/`.working` suffix rules (`screens.ts:123-125,147-159`; `screens.registry.test.ts` "draws a working state the mock does not list") and per-link labels on the working screen (`WorkingScreen.svelte`, `view.linkLabel(id)`; only one link id covered). The first landing's L3-1 is closed: `onboard.link.change` and `onboard.link.fix_credential` exist (re-read: `onboarding.en.properties`) |
| D7 — `language.choose` | behaviour shipped; order `mutation-after`, not `test-first` | after "Use Deutsch for both" the next screen is German (`runtime.ts:1745`, `i18n/index.ts:692`, `OnboardingView.svelte:120`; `onboarding.spec.js:145-163`); the lane records a mutation red (`097e3721`, `onboarding.spec.js:161`) and reports honestly that no base-commit red is recoverable (`executor_in_worktree: absent`); the leaf could not verify the run from the logs |

Browser checks, every deliverable: not shown by reading. Constraints: en/de pairs with the same placeholders (`{aspect}`, `{harnesses}`, `{locations}`); the text guard baseline is not in the diff stat; the design documents were updated, with the stale lines below; **"no new mock-only state facts" is not met.**

## Three-Layer Analysis (second landing)

- **Verdict:** layer 1 minor (the D5 clause literal, one order); layer 2 **contract skew twice over, against the Emit Amendment's own rule**, and three lane claims about the contract that are wrong; layer 3: no blocker, **2 major**, 10 minor, 2 leads.

### Layer 2 — the contract, read in the engine's state catalogue (re-read)

| Lane claim | Verdict | Evidence |
|------------|---------|----------|
| "the per-row `change:<aspect>` id shape is a contract gap" | **contradicted in part** | the catalogue states the link: `auto.result` offers `change:<aspect>` ("opens that step to change it", the leaf's quote of `14-machine-onboarding.adoc:207`; re-read `:208`: `done` → `auto.finished`; `change` → `auto.change`). The one real gap is the spelling after the colon: the catalogue does not say; the settings frame uses `reconfigure:<aspect>` with `harnesses`, `models`, `credentials`, `plan-defaults` while `OnboardingAspect.wireValue` (pm-api) is `harness`, `catalogue`, `store`, `defaults`, and the mock and the screen use the latter |
| "`checks[]`, `path` and `check` are a contract gap, the shape the screen has read since PLAN-64, removed from the mock" | **contradicted** | before this PR `checks[]` existed only in the mock's `harness.probe-failed`, read by `ProbeFailedScreen`; this PR copied it into the mock's `harness.none` (re-read: base `onboarding.json` has `checks` at `:889` before `harness.none` at `:952`; HEAD has it at `:1041` inside `harness.none` at `:1029`). **The catalogue's `harness.none` row names none of it** (re-read `:277-283`: "names how to install a known one (each profile's install hint) and offers to add another. `recheck`, `add`; the onboarding rests here until the operator acts"). The per-harness name, cause and fix are the facts of **`harness.blocked`** (re-read `:325-330`: "Names the harness, the cause, and the fix"). `HarnessScanResult` (pm-api) states `path`, `name`, `harness_id` in the `harness.choose` scan, not at `harness.none` |
| "the seven `*.reconfigure-result` states are the mock's" | borne out, and it matters | the real runtime has no such states: the catalogue says a `change` on `auto.result` calls that aspect as a later run through `onboard.next` and its return leads back to `onboard.auto-result` (re-read `:60-62,70`). The Change does **not** dead-end against the real daemon; the mock's states are a workaround: 7 registry claims in `screens.ts` (re-read: 7 lines), 14 `*.reconfigure-result.title` keys and about 270 lines of mock |
| "`later` on `harness.none` is a gap" | wrong way round | the catalogue offers `recheck` and `add` only: `later` is mock-only; the PR changed the web design to say `later` "sends a step" (`onboarding.adoc:216`), against the terminal design (`06-transcripts-steps-1-3.adoc:96`: "nothing is sent") |
| "the engine's state catalogue is outside this repository (not read)" | true, and the cause | the sibling checkout `../pm-mcp-core` is read-only reachable; the lane's spec told it to name where the contract states each fact |

State facts, and what the screen shows against the real shape:

| Fact | Where the contract states it | Against the real shape |
|------|------------------------------|------------------------|
| `harness.none` `checks[]`, per-harness answer and fix | nowhere (only the mock's `harness.probe-failed` and now `harness.none`); the cause and fix are `harness.blocked`'s | `failed=[]`: the runtime `summary` or "No harness found" plus the buttons: the pre-PR picture |
| `path` | `HarnessScanResult.path` (`harness.choose`) | not shown |
| `check` id | nowhere (`check_id` only in the mock's `check.result` events) | not shown |
| install hint per profile | catalogue `:281`; `harness-profiles/01-profile-schema.adoc:248` (`install_hint`) | **not rendered at all**: the design's install list (`onboarding.adoc` about `:200-205`) has no code |
| `results[]` field name; per-item check line | the catalogue says only "item schema of `auto.result`"; the CLI specification (`01-cli-and-installer.adoc:266`) lists key and "the check line that confirmed it" | reads a top-level `results`; the check line is never shown |
| reasons | the CLI closed list: `system`, `only_supported`, `all_supported`, `all_found`, `default`, `available`, `tool_logged_in`, `asked`; pm-api `AutoResultReason`: `detected`, `all_supported`, `default`, `asked`; the catalogue carries 4 `onboard.auto.reason.*` keys | five unknown reasons are humanised into English on a German screen ("Only supported", `AutoResultScreen.svelte:83`); a missing reason reads "the default", an invented reason |
| "looked for" (`looks_for`, `locations`, `in`, `harnesses`) | no published home | optional by construction: no crash |
| per-row link id | `change:<aspect>`, aspect spelling unspecified | if the runtime sends `change:harnesses`, `:models`, `:credentials` or `:plan-defaults`, `hasChangeLink` finds no row match and the links fall to the footer as "Change {aspect}" buttons through `humanizeKey`: degrades, does not dead-end (`AutoResultScreen.svelte:54-58,102-112`); one bare `change` link renders one "Change" button with an empty body that the daemon refuses, the refusal swallowed (`runtime.ts:1748`): a lead |
| recorded language | `MachineSettingsRecord.languageInterface`, which defaults to `en` (`MachineSettingsRecord.java:53`) | a fresh machine reads as "recorded en" |

### Layer 3 — Correct (second landing)

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `onboarding.json` `harness.none.facts.checks` (re-read), `HarnessNoneScreen.svelte:34-35,38-43` | the real daemon's `harness.none` | the screen is the old title plus buttons; the install hints the catalogue says the state names are not shown, so the operator learns nothing about installing a harness; D5 is delivered for the mock only, against the Emit Amendment's rule "do not build against the mock's own shape" | major (contract) |
| L3-2 | `screens.ts:217-303` (7 claims, re-read), `onboarding.json` (the seven states), 14 title keys | any `change` against the real daemon | dead weight in the registry, the catalogue and the mock for a flow the engine does not have; the e2e and browser evidence show the mock's route, not the engine's `change` → `onboard.next` → aspect states | major (contract) |
| L3-3 | `HarnessNoneScreen.svelte:38-43` | a runtime `summary` (a single ASCII line, `hypermedia-format/01-…:90-93`) | the German heading is English on every real screen: the new `onboard.harness.none.title` and `unusable_title` keys are shadowed; the e2e avoids asserting the card heading; the lane called it refuted ("every screen does this") | minor |
| L3-4 | `runtime.ts:1716,1743-1747` | a stale `loadOnboardingState` started before the step lands first | the revision guard compares apply order, not request order: the step's own newer `next` is dropped and the screen stays on the old state after the click; the test covers the opposite order only | minor |
| L3-5 | `runtime.ts:1309-1310` (lead) | the real daemon's `onboarding.step_completed` (pm-api `OnboardingStepCompletedEvent`; the mock does not emit it) | the SSE reload paints the next screen before the language is adopted: D7's "unbounded wait" does not cover it; the e2e cannot show it | minor (lead) |
| L3-6 | `runtime.ts:1811` (lead) | the automatic path | adopts the recorded language once at its start; the language aspect is recorded later in the run, and `auto.result` and `onboard.completed` trigger no adoption: the result may still be in the browser's language | minor (lead) |
| L3-7 | `i18n/index.ts:692-707`, `client.ts:2927` | a slow runtime | the language step's wait cannot hang forever (15 s per call) but the record button can spin up to about 45 s; pairing keeps a 2 s bound | minor |
| L3-8 | `i18n/index.ts:548,573,692` | a toggle clicked just before "record" | `setLocale` records its explicit choice only after its load and checks `generation`; `adoptRecordedLocaleNow` bumps it: the toggle is dropped silently | minor |
| L3-9 | `adoptRecordedLocaleNow`, the `adoptLocale` option, `explicitChoiceThisVisit`, the generation bump of CodeRabbit inline-5/6, `startOnboardingAuto`'s adoption, `forgetVisitChoice` (`i18n/index.ts:612`, exported for tests only) | — | no unit test: removing `explicitChoiceThisVisit` fails no test; only the e2e and one "late answer" test exist | minor (test) |
| L3-10 | `humanizeKey` (`i18n/index.ts:798`) | an unknown `change:<x>` or `credential:github` link | reads "Change:foo": it does not split on `:`; a fallback | minor |
| L3-11 | `HarnessNoneScreen.svelte:34-43` | a response with only `result: 'pass'` entries | "No harness found" with no explanation | minor |
| L3-12 | `AutoResultScreen.svelte:83` | a missing `reason` | shown as "the default" (an invented reason); the footer Change buttons use `on:click` while the row buttons use `onclick` (works in tests; delegation not confirmed) | minor |
| L3-13 | the seven `onboard.link.change\:<aspect>` keys | — | all "Change", used by no screen (the screen uses `onboard.link.change`): dead keys | minor |

**Layer 3 verdict:** no blocker; two major (L3-1, L3-2), both against the Emit Amendment's rule and the engine's catalogue; 11 minor, 2 of them leads. `deliverables_done=2` is generous: D5 counts partly, D6 and D7 delivered (D7's order is `mutation-after`).

**Stale design lines:** `onboarding.adoc:216` cites `web.onboard.later` (no such key in any catalogue; the screen uses `onboard.later.hint` and `onboard.link.later`) and says *Later* "sends a step for `later`" against the terminal design and the catalogue; `onboarding.adoc` about `:200-206` and "the install pages open in a new tab" describe an install list the screen does not render (the `.install` and `.or_add` keys were dropped from `onboarding-texts.adoc:45` with no replacement); `onboarding-texts.adoc:257,261` still list `web.onboard.later` and `web.onboard.later_action` (nonexistent: the lane said it fixed such keys); `onboarding-steps-4-7.adoc:440` ("this client reads `change:<aspect>`") mischaracterises the catalogue, which states `change:<aspect>` itself (the catalogue's own aspect-spelling mismatch is not mentioned); no document mentions the seven `*.reconfigure-result` states: the design says to run the aspect again through `onboard.next`.

## Metrics and Anomalies (second landing)

- Tokens `unknown`; `plugin_version` `unknown`; executor `0.1.2149`. `landing-check`: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`.
- Self-review: three rounds (13; 3 blocking, 2 major, 8 minor with B3 refuted by a whole-tree gate log; further minor); CodeRabbit: nine review-body findings, all fixed in `63f628c3`, `6baa911a`, `3a097f43`; re-read: `review-gate --pr-number 478` shows `unresolved_review_threads=0`, `review_body_findings_total=9` (its `handled:0` is the gate lacking `--disposition`). Sonar MCP search for pull request 478 returns no issues.
- Builds: 20 daemon-routed, 11 `in_process_fallback` (the e2e runs: the daemon's child environment is a whitelist without `PM_MOCK_WEB_PORT` and `PM_WEB_PORT`, so a routed e2e would use the operator's own ports: the same cause the PLAN-49 and PLAN-51 lanes reported; for the plan-marshall repository); queued waits up to 873 s.
- The lane's rule breaks, by its own count: one heredoc `python3 -`, shell `grep`/`sed` over build logs, a `git show origin/main:<path> | sed`, five in-process e2e runs; `spec_differ: true` at setup (the main checkout's copy differed from the ledger's, which bound); no guard enforced.
- The mailbox read with a digit-leading plan id is refused (`invalid_target_plan`): the sixth lane to report it; the lane notes a delivery to this plan's mailbox would have been invisible.
- No operator question was asked.

## Reconciliation Actions (second landing)

- [x] row: the split recorded as for the first landing (the operator's choice of 2026-11-17): `running` → `staged` → `launched` for deliverables 8 to 10; `pr` restamped `478`; `landing` stays `landings/PLAN-66.md` (both landings are in this file); `plan_marshall_plan_id` unchanged
- [x] epic.md: Open Defect `PLAN-66 (D5-D7) landed`
- [x] the inbox message archived on consume; resume anchor and `queue-view.md`

## Follow-Ups (second landing)

- **The contract decision (operator, item (0) of the decisions) is now larger:** D1 to D4 and D5 added mock-only state facts and states; the engine's catalogue states `harness.none`'s install hints, `auto.result`'s `change:<aspect>` (with an unspecified aspect spelling) and the reasons list differently. Nothing staged.
- **A PLAN-66 residue plan** (named, NOT staged): render `harness.none` as the catalogue states it (install hints; `recheck`, `add`) and drop the mock-only `checks[]` and `later`; drop the seven `*.reconfigure-result` states, 14 keys and the mock lines and run the aspect through `onboard.next`; the reasons and the aspect spelling against the catalogue; the German heading shadowed by `summary`; the revision guard; adoption on the SSE path and the auto path; the missing unit tests; the stale design lines.
- **For the plan-marshall repository:** the aspect spelling of the `change:<aspect>` and `reconfigure:<aspect>` links differs between two states of the same catalogue (`harnesses` against `harness`); the CLI specification's reason list and `AutoResultReason` differ.
- **The emit of D8 to D10** (the operator's choice of 2026-11-17, "Emit D5-10 now", covers the remainder) carries an amendment: read the catalogue's rows for `catalogue.select`, `store.choose`, `harness.choose`; build no mock-only fact or state; the real `harness.choose` scan result (`HarnessScanResult`) is the home of `path`.
