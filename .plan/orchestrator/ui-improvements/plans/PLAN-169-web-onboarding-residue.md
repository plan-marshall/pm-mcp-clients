# PLAN-169: Web: The Onboarding Choice Screens Say What the Engine Published, and Keep What the Operator Typed

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-19 (handle-open question 2 of 5, proposal 1: one web-side residue plan for PLAN-66, the mock cut-back a second plan, staged later) from `landings/PLAN-66.md` (third landing, the three-layer analysis of #485, `321985ac`). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-66 deliverables 8 to 10 (#485) landed with two major defects and a contract that was never read: on Linux the recommended, preselected Secret Service store tells the operator it protects "without macOS asking first" (`AspectChoice.svelte:98-104`), a harness that cannot list its models gets no field to type one into when the state has no `harnesses[]` (`ModelsChoice.svelte:107-127`), an unavailable store is disabled so the catalogue's `os_unavailable` exit can never be taken, and the screens read about twenty facts that no source publishes. This plan makes the three choice screens (`catalogue.select`, `store.choose`, `harness.choose`) say only what the engine's state catalogue publishes, keep the operator's typed input across a refresh, enter a model id correctly, read in en and de without ambiguity, and be tested with the shape the engine publishes, not with a shape the tests invent.

## Deliverables

Read first:
- `landings/PLAN-66.md` of this epic, third landing (layer 1 L1-1 to L1-5, layer 2, layer 3 L3-1 to L3-9). The points marked "re-read" were read at `321985ac` by the orchestrator and again at `ac538733` for this spec (L3-1, L3-2, L3-3, L3-5, L3-7, L3-9, the dead key `onboard.catalogue.select.group`); the others are the analysis leaf's reading and are re-checked at outline.
- The engine's rows, read-only: `../pm-mcp-core/doc/specification/state-catalogue/19-onboarding-aspects-models-providers.adoc` (`store.choose` at `:180-191`, `catalogue.select`), `14-machine-onboarding.adoc` (`harness.choose` at `:305`), `../pm-mcp-core/doc/specification/credentials-and-enrolment/01-provider-credentials.adoc` (§ Credential Store as an Operator Choice, `:127,143`).
- `doc/design/web-app/onboarding.adoc`, `onboarding-texts.adoc`.

1. **The contract of every fact the three screens read, named, and the screens cut back where none exists**
   - What: the screens read facts that no source publishes and that exist only as web-local types: `catalogue.select` `models[].allowed`, `.reason`, `.rationale`, `.harness_name`, `harnesses[].cannot_list`, `.models_listed`, `.display_name`, `no_advice_reason`; `store.choose` `stores[].protects_against`, `.does_not_protect_against`, `.backend`, `.platform`, `.os` (and the state-level `platform`, `os`, `backend` that `StoreScreen.svelte:10` forwards); `harness.choose`/`harness.tokens-*` `token_detail` (L1-3). For each fact the lane reads the engine's row and `pm-api` and decides ONE of: the row names it (the web types it from the row, the row cited in a comment at the type), or no row names it and the screen is cut back to the published shape (the operator's rule of 2026-11-18: the web does not build against a shape that only the mock or the web holds), or the screen cannot work without it and the fact is reported in the landing message as a contract gap, one line per fact, never silently. `TokensScreen` and its `harness.tokens-*` states are mock-only (no row in the catalogue): they are not extended; the two wordings of one fact ("usage complete" on `harness.choose`, "complete: {token_detail}" on `TokensScreen`) are made one wording or the screen is named as a gap (L2). No file of `pm-mock-runtime` and no engine document is edited by this deliverable.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A table in the landing message names, for each fact listed above, the engine row that publishes it or the decision (cut back, or contract gap) and the line that carries the decision (evidence order: `none`).
     - For each fact that is cut back, a test fails at `ac538733` and passes: the screen rendered from a state that carries only the published fields shows no label, group or claim built from the cut fact (evidence order: `test-first`).
2. **The store options claim only what the engine publishes, and the OS-unavailable exit is open**
   - What: (a) `protectionDetail` (`AspectChoice.svelte:92-125`) no longer chooses a platform's copy by guessing: for a store row without the engine's own protection text it prints the generic line the catalogue row names ("what protects a secret in it and what does not") and no claim of a platform, so a Linux Secret Service option never reads "without macOS asking first" and the "other" copy invents no protection (L3-1, L3-4; the credentials specification says the Secret Service lets every process of the user read an unlocked collection, `01-provider-credentials.adoc:127,143`). (b) `StoreScreen.svelte:25-28` records the link of the kind the operator chose (`os` for an OS store, `file` for the file store) and records nothing for a kind it does not know, instead of falling back to `links[0]` (L3-3). (c) An unavailable OS store stays selectable as the way into `store.unavailable`: choosing it records `os_unavailable` (the catalogue row, `19-…:185-191`); `canRecordStore` (`AspectChoice.svelte:83`) and the primary button (`:209`) no longer cut the exit off (L1-4; the spec of PLAN-66 D9 did not ask for the disabling).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `ac538733` and passes: the published store row `{kind:'os', name:'GNOME Keyring', available:true, recommended:true}` rendered with no `platform` prop contains neither "macOS" nor "without macOS asking first" (evidence order: `test-first`).
     - A test fails at `ac538733` and passes: choosing an unavailable OS store records the `os_unavailable` link, and a store of an unknown kind records no link (evidence order: `test-first`).
     - The same two tests are run once with their production change reverted and fail (evidence order: `mutation-after`).
3. **The model field exists for every harness that cannot list, and a model id is entered correctly**
   - What: (a) a harness that cannot list its models and has no rows still gets its group and its text field in the shape the engine publishes (no `harnesses[]`): the groups are built from the harnesses the state names, not only from `harnesses[]`, initial rows and rows (`ModelsChoice.svelte:107-127`, L3-2; which fact names the harness is settled by deliverable 1). (b) A model id is compared case-insensitively where the engine's ids are, a `::` in an id does not collide with the row key (`:139-151`, L3-6), an IME composition is not added half-typed (`isComposing`), and an ignored add (empty, duplicate) says so (L3-6). (c) The checkbox of a model is named by its model and its harness, and the group carries a role that ties the box to its harness when one model id appears under two harnesses (`:233`, L3-7).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `ac538733` and passes: a cannot-list harness with zero rows in a state with no `harnesses[]` shows a text field named for the harness (evidence order: `test-first`).
     - Tests fail at `ac538733` and pass: two ids that differ only in case are one row, an id containing `::` is added once and removed once, an IME `compositionend` adds nothing half-typed, an ignored add shows its reason (evidence order: `test-first`).
     - Two rows with the same model id under two harnesses have distinct accessible names, checked by a role query (evidence order: `test-first`).
4. **A refresh of the state does not wipe what the operator typed or unticked**
   - What: a refreshed state that changes one flag of the proposal wipes the operator's added models, deselections and drafts without notice (`ModelsChoice.svelte:89-98` `prevProposalSignature`, `AspectChoice.svelte:67-81`, `HarnessChoice.svelte:58-70` `selectionSignature`; L3-5). The three components resync only the part of the selection the changed fact belongs to, and keep the operator's own edits (added ids, unticked rows, drafts); a change that makes an edit meaningless (a removed row) drops that edit and says so. The rule is one function or one documented pattern for the three components, decided at outline.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test per component fails at `ac538733` and passes: the operator adds a model id, unticks a row and types a draft, the state is refreshed with one proposal flag changed, and the three edits are still there (evidence order: `test-first`).
     - A test per component: a refresh with no change keeps every edit, and a refresh that removes the edited row drops that edit and prints the notice (evidence order: `test-first`).
5. **The words, the keys and the design lines of PLAN-66 D8 to D10 read right**
   - What: the partial usage line ("usage partial: no cache read, cache write", `reports.en.properties:96` and the German line) is unambiguous and grammatical in en and de, and the two expected values that lock the old wording in are updated (L3-8; `scenarios.test.ts:1828`, `harnessUsage.test.ts:52`). The dead keys are removed from both packs and from the guard's list: `onboard.catalogue.select.group` (read: referenced nowhere in `pm-api`, `pm-web-app` or `doc`), and, if the sweep at outline finds them unreferenced, `onboard.harness.tokens.partial`, `.not_reported` and the duplicate `onboard.link.yes_remember` (L3-9); `OnboardingCatalogueTest.java:53-54` lists two of them and is updated with them. The design lines that PLAN-66 D8 to D10 broke are corrected: `onboarding.adoc:190` and `:458` say what the screens show (the "Why?" rationale lines the lane deleted, and the harness token line), and `onboarding-texts.adoc:57` lists no key that does not exist.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - A test fails at `ac538733` and passes: the partial usage line for `{cache read, cache write}` reads as the new wording in en and de (evidence order: `test-first`).
     - `git grep -n "onboard.catalogue.select.group" -- pm-api pm-web-app doc` names no line (evidence order: `none`).
     - A reader of `onboarding.adoc` and `onboarding-texts.adoc` finds no line the screens contradict and no key that is not in the pack, checked against the packs by the lane (evidence order: `none`).
6. **The tests of D8 to D10 made able to fail, and the browser check of what they could not show**
   - What: every test of the three screens that passes `platform:'linux'` (or any shape the engine does not publish) also runs with the published shape of deliverable 1; `AspectChoice.test.ts:30` asserts both protection lines and not only the marker (L2); `vi.restoreAllMocks()` moves from `beforeEach` to `afterEach` in the scenario describes (`OnboardingView.scenarios.test.ts:58,1402,1627,1739`, the `jest-testing` rule); the claims of the lane's report the tree does not bear out are not repeated in the progress record (no test-first order is claimed that the commits do not show). The browser check of the landing that could not be made (the mock's `catalogue.select` carries no `recommended` or `allowed`, so the marker and the Linux copy cannot appear in a mock browser check) is made with a scenario state that carries the published shape, en and de, light and dark, two sizes; the screenshots are read. Whether the engine accepts the `models` payload of `ModelsScreen.svelte:39` (full UI rows rather than `{harness_id, model_id, source}`; an unmodified screen sends `{}`) is read from the engine's row; if the row does not settle it, the landing message names it as not shown by reading.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-frontend:jest-testing, pm-dev-frontend:javascript, pm-dev-frontend:css
   - Done when:
     - Each changed assertion fails when the production line it covers is reverted (the lane reverts it once and records the failing run; evidence order: `mutation-after`).
     - `git grep -n "restoreAllMocks" -- pm-web-app/src/lib/views/OnboardingView.scenarios.test.ts` names no line inside a `beforeEach` (evidence order: `none`).
     - The screenshots of `catalogue.select`, `store.choose` and `harness.choose` in en and de, light and dark, at two sizes are read and the landing message states what each shows (evidence order: `none`).

Not in this plan, named so they are not lost (each stays in `epic.md` as an Open Defect): the PLAN-66 mock cut-back decided on 2026-11-18 (`harness.none` against the catalogue's install hints, the seven mock-only `*.reconfigure-result` states and their 14 title keys and mock lines): a second residue plan, staged later, held behind PLAN-79 and PLAN-168 because it edits `MockRuntimeEngine.java`; the minors of PLAN-66's first two landings (the German heading shadowed by `summary`, the revision guard's request order, adoption on the SSE and auto paths, the missing unit tests of the language adoption, `deriveStepFromState(undefined)` returning 1); the `change:<aspect>` spelling (an engine decision); the `HarnessChoice` resync and the `harnessNames()` change at `OnboardingView.svelte:202` as non-contract facts only insofar as deliverable 1 does not name them.

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan_marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); a dismissal is made only on the operator's selection.
- Build and test through Maven only (`CLAUDE.md`). Beside the lane's build gate, these pass at the head: `./mvnw verify -pl pm-web-app -am`, `./mvnw verify -Pe2e -pl pm-web-e2e -am` (the Playwright suite runs because `pm-web-app` changes; pass the ports the lane's `AGENTS.md` names), `./mvnw verify -pl pm-api`. Run `-Pcoverage` and `-Pintegration-tests` as separate invocations.
- No new dependency or plugin. No `innerHTML`, `outerHTML`, `insertAdjacentHTML` or `{@html}`; no arbitrary timer wait in a test (`CLAUDE.md` Web Standards). Text from the runtime or from a fact is rendered through a text-treating sink and the display layer.
- Texts come from the catalogue in `en` and `de`; the expected text of a contract stays literal in a test; a view this plan touches passes the text guard (the baseline only shrinks).
- Every view this plan touches uses the design system components; no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- **The engine's catalogue is read before the outline and every fact is named by its row (deliverable 1).** The lane does not build against the mock's own shape, edits no file of `pm-mock-runtime` and nothing in `../pm-mcp-core`.
- Every behaviour change has a failing-first run in the progress record; the order of each Done-when is the order the evidence has: a `none` Done-when is never described as failing, and "fail-first" is used only for `test-first` and `mutation-after` rows.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met.
- Pull request size: if the changed set passes 100 files, stop and ask the operator to split (CodeRabbit refuses above 100 files). Expected: about 25 files.
- Scope: six deliverables, the split guard's limit, on ONE surface (the three choice screens, their `pm-api` texts and their design lines); proceeding unsplit is the recorded rationale (the operator's choice of one web-side plan, 2026-11-19).
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: `protectionDetail` picks the macOS Keychain copy when the store row has no `backend`, `platform` or `os` and the `platform` prop is empty — read at `pm-web-app/src/lib/components/AspectChoice.svelte:92-105` at `ac538733` (`const backend = (store.backend ?? store.platform ?? store.os ?? platform ?? '')`, `backend === ''` selects `os.keychain`)
- OBSERVED: `StoreScreen` forwards `platform` from the STATE's `platform`, `os` or `backend` fact, not from the row — read at `pm-web-app/src/lib/views/onboarding/StoreScreen.svelte:10`; the engine's `store.choose` row names "each offered store with what protects a secret in it and what does not" and no `platform` fact — read at `../pm-mcp-core/doc/specification/state-catalogue/19-onboarding-aspects-models-providers.adoc:184`; the mock's store rows carry `kind`, `name`, `available`, `recommended` only — read at `pm-mock-runtime/src/main/resources/scenarios/onboarding.json:1779-1790`
- OBSERVED: `StoreScreen` records `view.linkBy(String(choice), 'os', 'file') ?? view.links[0]` — read at `StoreScreen.svelte:25-28`
- OBSERVED: the groups of `ModelsChoice` are built from `harnesses[]`, the initial rows and the rows only — read at `ModelsChoice.svelte:107-127` (`buildGroups`)
- OBSERVED: the engine's `store.choose` routes `os_unavailable` to `store.unavailable` — read at `19-onboarding-aspects-models-providers.adoc:185,191`; the web disables an unavailable store — `canRecordStore` at `AspectChoice.svelte:83` and `disabled={kind === 'store' ? !canRecordStore : false}` at `:209`
- OBSERVED: `onboard.catalogue.select.group` is in both `onboarding.*.properties` (`:250`) and referenced nowhere else in `pm-api`, `pm-web-app` or `doc` — read by `git grep -n "onboard.catalogue.select.group" origin/main` at `ac538733`
- OBSERVED: `vi.restoreAllMocks()` is called at `OnboardingView.scenarios.test.ts:58,1402,1627,1739` — read by `git grep -n "restoreAllMocks"`; whether each is inside a `beforeEach` is read at outline
- HYPOTHESIS: the engine's `catalogue.select` row names the harnesses and says which cannot list their models, so deliverable 3(a) can build the groups from the state's own facts — confirm/refute at `19-onboarding-aspects-models-providers.adoc` § `catalogue.select` (verify-at-outline; if no row names them, deliverable 1 decides the fact and deliverable 3 follows it)
- HYPOTHESIS: `onboard.harness.tokens.partial`, `.not_reported` and `onboard.link.yes_remember` are unreferenced — `OnboardingCatalogueTest.java:53-54` lists two of them, which may be a guard that pins their existence — confirm/refute by `git grep` over `pm-api`, `pm-web-app` and `doc` (verify-at-outline)
- HYPOTHESIS: the German partial-usage line is ungrammatical and the English one ambiguous — read at `reports.en.properties:96` and `reports.de.properties:96`; the leaf's wording is not re-read (verify-at-outline)
- HYPOTHESIS: `onboarding.adoc:190` and `:458` describe the screens before PLAN-66 D8 to D10 and `onboarding-texts.adoc:57` lists keys that do not exist — confirm/refute against the packs at HEAD (verify-at-outline)
- HYPOTHESIS: the engine accepts the `models` payload of `ModelsScreen.svelte:39` (full UI rows, or `{}` for an unmodified screen) — confirm/refute at the engine's `catalogue.select` row; may remain "not shown by reading"
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; PLAN-165 has landed and touched none of this plan's files, see Dependencies. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.svelte`
- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/ModelsChoice.svelte`
- OBSERVED: `pm-web-app/src/lib/components/ModelsChoice.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte`
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.test.ts`
- OBSERVED: `pm-web-app/src/lib/display/harnessUsage.ts`
- OBSERVED: `pm-web-app/src/lib/display/harnessUsage.test.ts`
- OBSERVED: `pm-web-app/src/lib/views/onboarding/StoreScreen.svelte`
- OBSERVED: `pm-web-app/src/lib/views/onboarding/ModelsScreen.svelte`
- OBSERVED: `pm-web-app/src/lib/views/onboarding/TokensScreen.svelte`
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.ts`
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte`
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.scenarios.test.ts`
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.test.ts`
- OBSERVED: `pm-web-app/src/styles/choice-screen.css`
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties`
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties`
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties`
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/OnboardingCatalogueTest.java`
- OBSERVED: `doc/design/web-app/onboarding.adoc`
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc`

## Dependencies and Sequencing

- Depends on: `PLAN-66` (shipped: #467 `735318df`, #478 `98ca84d0`, #485 `321985ac`); the engine's state catalogue at `../pm-mcp-core` (read-only).
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-19; shipped, parked and superseded plans are not in the disjointness population):
  - `PLAN-165` (shipped 2026-11-19, #503 `a8ac159d`): the 13 files the corpus read as shared at staging are **not** touched by that pull request (re-read: none of this plan's 23 files changed between `ac538733` and `a8ac159d`), so the overlap is gone and no rebase is owed. Its exported refresh sets (`pm-web-app/src/lib/stores/refreshEvents.ts`) exist now; `OnboardingView` is not one of their consumers.
  - `PLAN-154` (staged): `reports.en.properties` and `reports.de.properties` (deliverable 5's usage line); whichever lands second rebases and keeps both sides.
  - `PLAN-72`, `PLAN-73`, `PLAN-74` (parked): the same web files and `doc/design/web-app/onboarding*.adoc`; re-grounded when one is restarted, not part of the gate.
- Adjacent to: the second PLAN-66 residue plan (the mock cut-back, not staged); PLAN-168 (the terminal's refusal path, a different module).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-169-web-onboarding-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
