# PLAN-182: Web: The Machine Onboarding Follows the Engine's Catalogue (the PLAN-66 Residue)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 5, proposal 1), which carries out the PLAN-66 residue plan the operator decided on 2026-11-18 (proposal 3: the
> mock-only facts and states are cut back to the engine's catalogue): the mock cut-back of the second landing (#478), the
> contract finding of the first (#467) and the minors of the three landings (`landings/PLAN-66.md`). The web residue of the third
> landing's two majors was done by PLAN-169 and PLAN-59 deliverable 5; this plan keeps what they left. This spec is
> SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It starts after PLAN-174 (#533),
> PLAN-181 and PLAN-176 have landed** (the onboarding scenario test, the mock, `ProjectOnboardingView` and the catalogue are shared).

## Objective

The machine onboarding screens were built against the mock's own shape. The engine's state catalogue does not have what the mock has: its `harness.none` names install hints and offers `recheck` and `add`, not per-harness `checks[]`, `path`, `check` or `later`; a `change` on `auto.result` is a later run through `onboard.next`, so the seven `*.reconfigure-result` states are the mock's workaround and dead weight in the registry, the catalogue and the mock. This plan cuts the mock, the screens and the design back to the published shape, fixes the minors the three PLAN-66 landings found, and pins each with a test fed with what the engine sends.

## Deliverables

The documents are the specification; read them first: the engine's `state-catalogue/14-machine-onboarding.adoc` (`harness.none` `:277-283`, `harness.blocked` `:325-330`, `auto.result` `:60-62,70,207-208`) in `plan-marshall/pm-mcp-core`, `doc/design/web-app/onboarding.adoc`, `onboarding-texts.adoc` and `onboarding-steps-4-7.adoc`, and `landings/PLAN-66.md` (all three landings) and `PLAN-169.md` for every finding with its file and line.

1. **A change runs through `onboard.next`: the seven reconfigure-result states go**
 - What: the real runtime has no `*.reconfigure-result` states: a `change` on `auto.result` calls that aspect as a later run through `onboard.next` and its return leads back to `onboard.auto-result` (`14-machine-onboarding.adoc:60-62,70,208`). Remove the seven states from the mock (`onboarding.json`, about 270 lines), the seven registry claims (`screens.ts:221-306`), the 14 `*.reconfigure-result.title` keys and the seven dead `onboard.link.change:<aspect>` keys (en and de), the tests that exercise them (`OnboardingView.scenarios.test.ts`, `screens.registry.test.ts`), and make the mock's `change` follow the catalogue (the aspect run through `onboard.next`, back to `auto.result`). The spelling of the aspect after the colon is unspecified in the catalogue (the settings frame uses `reconfigure:<aspect>` with `harnesses`, `models`, `credentials`, `plan-defaults`, while `OnboardingAspect.wireValue` is `harness`, `catalogue`, `store`, `defaults`): report it as a finding for the engine's specification, do not decide it here. A bare `change` link renders one "Change" button with an empty body that the daemon refuses and the refusal is swallowed (`runtime.ts:1748`): show the refusal. An unknown `change:<x>` or `credential:github` link reads "Change:foo" because `humanizeKey` (`i18n/index.ts:798`) does not split on `:`: split it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `OnboardingView.scenarios.test.ts` and `OnboardingScenarioTest.java` (Generators, `@EnableGeneratorController`) run a Change from `auto.result` through the aspect's state and back to `auto.result` with no `*.reconfigure-result` state anywhere; `git grep -n "reconfigure-result" -- pm-web-app/src pm-mock-runtime/src pm-api/src` is empty; a refused `change` shows its refusal; each fails with its production line reverted. Evidence order: `test-first`.
2. **`harness.none` shows what its row names**
 - What: the catalogue's `harness.none` row "names how to install a known one (each profile's install hint) and offers to add another. `recheck`, `add`" (`14-machine-onboarding.adoc:277-283`); the per-harness name, cause and fix are the facts of `harness.blocked` (`:325-330`). The mock copied the `checks[]` of its `harness.probe-failed` into `harness.none` and the screen reads them (`HarnessNoneScreen.svelte:31-43`); `path`, `check` and `later` are mock-only. Render the install hint of each profile (`harness-profiles/01-profile-schema.adoc:248` `install_hint`; the design's install list `onboarding.adoc:200-206` has no code), `recheck` and `add`; remove `checks[]`, `path`, `check` and `later` from the mock's `harness.none` and from the screen, and the `onboard.later.hint` use there. A runtime `summary` is a single ASCII line (`hypermedia-format/01-…:90-93`) that shadows the German heading (`onboard.harness.none.title`, `.unusable_title`): the heading comes from the catalogue, the summary stays a sentence under it. A response with only `result: 'pass'` entries no longer reads "No harness found" with no explanation.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `OnboardingView.scenarios.test.ts` is fed the `harness.none` shape the catalogue names and shows the install hint per profile, `recheck` and `add` in English and German, and none of `checks`, `path`, `later`; each fails with its production line reverted. Evidence order: `test-first`.
3. **The roles review and the onboarding minors**
 - What: each re-read at outline and fixed where it still holds (the leaf's reading of 2026-11-18, not re-run):
   - `RolesReview.svelte:222-225,291,300` prints the raw key `onboard.roles.effort.<value>` for an effort outside low, medium and high (the engine defines the effort as what the harness accepts for the model, `model-work/01-roles-and-configuration.adoc:84`); a role that disappears after a refresh still sends its decision (`:96-104` against `:315-322`); the Advisory button shows for a row without `advisory`, flips `aria-pressed` and opens nothing; "Save 1 roles" has no plural form (`onboard.roles.review.save` en and de); every row's Change and Advisory buttons share their accessible names and Accept/Reject has no pressed state, and the focus does not move into an opened edit or token field.
   - A refused `roles.validate` and back remounts the screen and the hand edits vanish (a lead); a refreshed state that changes one flag of the proposal wipes the operator's added models, deselections and drafts without notice (`ModelsChoice.svelte`, `AspectChoice.svelte`, `HarnessChoice.svelte`); the `ModelsChoice` add accepts case variants, a `::` in an id and an IME confirm with a half-typed id (no `isComposing` check, no feedback for an ignored add); the same model id under two harnesses gives two identical checkbox names; `AspectChoice`'s "other" copy states a protection the store does not publish; `CredentialEntry` stays open after a saved token and nothing triggers "Check again".
   - `runtime.ts:1716,1743-1747`: the revision guard compares apply order, not request order, so a stale `loadOnboardingState` that lands first makes the step's own newer `next` drop; `adoptRecordedLocaleNow`, the `adoptLocale` option and `explicitChoiceThisVisit` (`i18n/index.ts:548,573,612,692-707`) have no unit test (removing `explicitChoiceThisVisit` fails none) and a toggle clicked just before "record" is dropped silently; the record button can spin up to about 45 s against pairing's 2 s bound.
   - `AutoResultScreen.svelte:83` shows a missing `reason` as "the default" and five reasons outside the catalogue's four (`onboard.auto.reason.*`) humanised into English on a German screen: show nothing for a missing reason and the catalogue's words for the rest; `reports.*.properties:96` "usage partial: no cache read, cache write" is ambiguous and the German line is ungrammatical (both locked as expected values in `scenarios.test.ts` and `harnessUsage.test.ts`).
   - The palette-before-onboarding fix (`AppShell.svelte:47-49`) and `TopBar` have no test; the `onboard.link.yes_remember` key has no user outside `ProjectOnboardingView.linkLabel`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Each item that still holds at the head has a test that fails with its production line reverted; an item that no longer holds is named as such in the pull request. Evidence order: `test-first`.
4. **The design lines and browser evidence**
 - What: bring the design to the code and the catalogue: `onboarding.adoc:216` cites `web.onboard.later` (no such key) and says Later "sends a step" against the terminal design (`06-transcripts-steps-1-3.adoc:96`: "nothing is sent"); `onboarding.adoc:200-206` and "the install pages open in a new tab" describe the install list deliverable 2 now builds; `onboarding-texts.adoc:257,261` list nonexistent `web.onboard.later` and `web.onboard.later_action` and omit `onboard.roles.review.advisory_for`, `.change_for`, `.refused` and `onboard.defaults.review.choose_available`; `onboarding-steps-4-7.adoc:26,30,33,62` print the class as raw `read_only` and `zero_tool`, its Check section's sketch order differs from the state's link order, and `:440` ("this client reads `change:<aspect>`") mischaracterises the catalogue; the facts `provider_id`, `unavailable`, `departure` and `departures[]` are stated as state facts no engine specification names (say so, or cut them as the 2026-11-18 decision says). Browser check of `harness.none`, `auto.result` with a Change, the roles review and the defaults review.
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - A search of the three design documents for `web.onboard.later`, `later_action`, `reconfigure-result` and the removed `checks[]` finds none, and every key `onboarding-texts.adoc` lists exists in the catalogue. Evidence order: `none`.
   - `onboarding.spec.js` runs the onboarding with the cut-back mock and passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set (it waits on the address and the DOM); the full `-Pe2e` run at the head is green and cited. Evidence order: `test-first`.
   - Browser check (Playwright MCP) of `harness.none`, `auto.result` with a Change, the roles review and the defaults review in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`, after the last visual commit. Evidence order: `none`.

5. **The project onboarding records its identity and keeps its address on the real wire (folded 2026-11-24: PLAN-176 L3-1, L3-2, L3-3)**
 - What: PLAN-176 (#549) wired the project onboarding against fields no wire record carries (`landings/PLAN-176.md`, read at `adb0d310`). (a) A being-added run cannot save its identity: `canSave` needs `run.links.set` (`ProjectOnboardingView.svelte:125`), `runOfBeingAdded` (`:417-444`) takes its links only from the explorer's `being_added` row, and `BeingAddedProject` (`pm-api`) carries only `checkout_path`, `current_step`, `step_number`, `total_steps`, `continue_link`, `abandon_link`; the mock serves `links.set` (and the entry's facts) on `GET /api/v1/entries/{id}` (`MockRuntimeEngine.java:4941,5002`), which the view never reads. Read the entry for the run's `links` and `build_environment`, record the identity by following the `set` link's `href` (not a path rebuilt in `actEntry(entryId,'set')`, `:223`), and drop the invented client fields `BeingAddedProject.links`, `.build_environment`, `ProjectOnboardingState.lifecycle_gate`, `EnrolledProject.needs_operator_kind` and `PlanCompletionReport.phase` (`client.ts:141,157-158,237,1106`) or replace each with what the wire sends; the pilot checkbox gate on `lifecycle_gate` (`:730`) and the needs-terminal card at step 3 (`:122-124`) read only wire facts. (b) The run's address is lost: `openProjectOnboarding` writes `/projects/onboarding/{id}` (`runtime.ts:2103-2110`), but the layout's `syncPath` replaces the URL with `addressOfState` on every store change (`routes/+layout.svelte:263-277`) and `routeParamsOf` (`runtime.ts:1576-1591`) carries no `onboardingId`, so a reload lands on `/projects/onboarding` and the run is gone. Carry the onboarding id in the route params. (c) The design still lists `/projects/onboard` as an old address and names no `/projects/onboarding/{id}` (`doc/design/web-app/navigation.adoc:222`, `doc/design/web-app/README.adoc:494`): bring both to the code.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `ProjectOnboardingView.test.ts` is fed a `being_added` row exactly as `BeingAddedProject` serialises and the entry exactly as the mock serves it, and records the identity through the entry's `set` `href` with the specified body; no test feeds a field the wire record lacks; each fails with its production line reverted. Evidence order: `test-first`.
   - `runtime.test.ts` mounts the address writer (or calls `addressOfState` after `openProjectOnboarding`) and the address keeps `/projects/onboarding/{id}`; `projects.spec.js` reloads a started run and lands on the same run (waits on the address and the DOM, ports set). Evidence order: `test-first`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime,pm-web-app verify`, `./mvnw -pl pm-api,pm-mock-runtime,pm-web-app verify -Ppre-commit` (review every rewrite), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head, and `-Pcoverage` at the head after the last code commit (minimum 80 % per module). The CLI modules run against the mock too: the full `./mvnw verify` passes.
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in English and German; the text guard baseline does not grow. Java: `CLAUDE.md` Code Standards. Pre-1.0: no shim for a removed state or key.
- The engine's catalogue is the contract: a fact the catalogue does not name is not read by a screen, and a gap in the catalogue is reported to the operator as a finding, never filled from the mock. A test is fed with what the engine sends and the mock must send it too.
- Ports of this plan: mock web port 21820, app port 21821 (`20000 + 10 × 182`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). A dismissal needs the operator's word.
- A `test-first` clause cites a failing run at the plan's start; where the base-sha instrument is unavailable, say so and do not relabel the clause.

## Claim Labels

- OBSERVED: the seven `*.reconfigure-result` states still exist in the mock, the registry and the catalogue — read by `git grep -c reconfigure-result` (mock `onboarding.json` 21, `screens.ts` 7, `OnboardingView.scenarios.test.ts` 2, `onboarding.en.properties` 7, `onboarding.de.properties` 7) and `pm-web-app/src/lib/views/onboarding/screens.ts:221-306`
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: git grep -c reconfigure-result at a1125521: onboarding.json 21, screens.ts 7, OnboardingView.scenarios.test.ts 2, onboarding.en/de.properties 7 each (unchanged)
- OBSERVED: the seven `onboard.link.change:<aspect>` keys still stand in en and de (14 lines) — read by `git grep -n 'onboard.link.change\:' -- pm-api/src/main/resources`
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: onboard.link.change\:<aspect> 7 aspects x en/de = 14 lines at :313-319, plus the plain onboard.link.change at :312 (a1125521)
- OBSERVED: `HarnessNoneScreen` reads `checks` and shows the `later` hint, and the mock's `onboarding.json` carries `"checks"` twice — read at `pm-web-app/src/lib/views/onboarding/HarnessNoneScreen.svelte:9,31-35,140-141` and `git grep -c '"checks"' -- pm-mock-runtime/src/main/resources/scenarios/onboarding.json`
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: onboarding.json carries "checks" 2 times at a1125521; onboarding views unchanged since 3ae8a305 except ProjectOnboardingView
- OBSERVED: `RolesReview` builds the effort label from `onboard.roles.effort.${effort}` for any effort value — read at `pm-web-app/src/lib/views/onboarding/RolesReview.svelte:222-225,291,300`
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: pm-web-app/src/lib/views/onboarding/ unchanged between 3ae8a305 and a1125521 (git diff --stat empty for RolesReview)
- OBSERVED: `StoreScreen` records the link of the chosen kind and `os_unavailable` for an unavailable OS store, with no `links[0]` fallback (the PLAN-66 L3-3 and L1-4 are fixed) — read at `pm-web-app/src/lib/views/onboarding/StoreScreen.svelte:22-26`
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: StoreScreen.svelte unchanged between 3ae8a305 and a1125521
- OBSERVED: the dead keys `onboard.catalogue.select.group`, `onboard.harness.tokens.partial` and `.not_reported` are gone from the catalogue — read by `git grep -n` over `pm-api/src/main/resources` (0 lines each)
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: git grep ^key= for the three dead keys over pm-api/src/main/resources: 0 lines at a1125521
- OBSERVED: a being-added run cannot save its identity on the wire, and `routeParamsOf` carries no `onboardingId` — read at `pm-web-app/src/lib/views/ProjectOnboardingView.svelte:125,417-444`, `pm-api/.../BeingAddedProject.java` (six fields, no `links`), `pm-mock-runtime/.../MockRuntimeEngine.java:4941,5002`, `pm-web-app/src/lib/stores/runtime.ts:1576-1591,2103-2110`, `pm-web-app/src/routes/+layout.svelte:263-277` (orchestrator re-read at `e0e95c05`, 2026-11-24; the address loss is a structural read, confirmed by deliverable 5's e2e)
  - verdict: corroborated | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: routeParamsOf (runtime.ts:1673-1689 at a1125521, moved by #558) still carries no onboardingId; BeingAddedProject.java:31-38 six fields, no links; mock being-added at MockRuntimeEngine.java:4952,4991 (moved by #553)
- HYPOTHESIS: the engine's catalogue names no `checks[]`, `path`, `check` or `later` on `harness.none` — confirm/refute at `pm-mcp-core` `doc/specification/state-catalogue/14-machine-onboarding.adoc:277-283` (verify-at-outline; the PLAN-66 landing read it on 2026-11-18)
  - verdict: unverifiable | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: engine catalogue not re-read; settled at outline
- HYPOTHESIS: the minors of deliverable 3 still hold at the head (the leaf's readings of 2026-11-18, PLAN-169 and PLAN-59 deliverable 5 may have changed some) — confirm/refute each at its file and line (verify-at-outline)
  - verdict: unverifiable | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: the minors are leaf readings; individually settled at outline
- Verify-first clause: before each deliverable re-read the named files and the engine's catalogue at the then-current HEAD; PLAN-174, PLAN-181 and PLAN-176 (the onboarding scenario test, the mock, `ProjectOnboardingView`, the catalogue) have landed first. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: a11255211a50ffc405556f358ae5c626bf1c7b98 | by: ui-improvements/next | rescoped: n/a | evidence: procedural verify-first clause; PLAN-174, 176, 181 and 185 have landed

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/onboarding.json` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.registry.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/onboarding/HarnessNoneScreen.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/onboarding/RolesReview.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/onboarding/AutoResultScreen.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.scenarios.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/ModelsChoice.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/ModelsChoice.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/CredentialEntry.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/shell/AppShell.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/shell/TopBar.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/web-app/onboarding-steps-4-7.adoc` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD adb0d310 (deliverable 5, folded 2026-11-24)
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at HEAD adb0d310 (deliverable 5)
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD adb0d310 (deliverable 5)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD adb0d310 (deliverable 5)
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD adb0d310 (deliverable 5, read; edited only if the address writer must change)
- OBSERVED: `pm-web-e2e/tests/projects.spec.js` — present at HEAD adb0d310 (deliverable 5)
- OBSERVED: `doc/design/web-app/navigation.adoc` — present at HEAD adb0d310 (deliverable 5)
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD adb0d310 (deliverable 5)

## Dependencies and Sequencing

- Depends on: `PLAN-66`, `PLAN-169`, `PLAN-59` (shipped); `PLAN-174` (#533, running: `OnboardingScenarioTest.java`), `PLAN-181` (running: `pm-mock-runtime` Sonar part, `OnboardingScenarioTest.java`) and `PLAN-176` (`ProjectOnboardingView`, the catalogue) landed first — all three have landed (PLAN-176 #549 `ae123acb`, 2026-11-24).
- Overlaps with: `PLAN-185` (running: `runtime.ts`, `runtime.test.ts`; sequenced after it unless the operator decides), `PLAN-85`, `PLAN-135`, `PLAN-136` (staged: `runtime.ts`, `routes.ts`, `common.*`), `PLAN-177` and `PLAN-179` (no shared file beyond `runtime.ts` and `i18n/index.ts`). Sequenced after `PLAN-174`, `PLAN-181` and `PLAN-176`.
- Adjacent to: the engine work package for `request_id` (`briefs/engine-request-id-work-package.md`, not a plan; no shared file), the engine's gaps this plan reports (the aspect spelling after `change:`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-182-web-onboarding-contract-cut-back-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
