# PLAN-148: Web Foundation: Reason Badge, Phase Label, Marker, RowLink and the Tag Rules (D1 Foundation, Part 2)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents. This spec is SELF-SUFFICIENT:
> the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-147 (#420, `dea405d8`) delivered `KindMark`, `Link`, `ExternalLink`, `PrChip`, `Compose` and `FormStatus`. This part completes the tag family of `components-tags.adoc` (`ReasonBadge`, `PhaseLabel`, `Marker`) and builds `RowLink`, the row that opens its object.
It also brings the three tag components that already exist (`StatusLabel`, `CountBadge`, `Chip`) up to the agreed rules (the `!` from the catalogue, the copy act in the accessible name,
`--color-success-subtle` with a value of its own). The parked list, inbox, running-work, plan and epic plans (49, 51, 52, 55, 58, 59, 62) wait for these pieces.

## Deliverables

Read first: `doc/design/web-app/components-tags.adoc` (§ `#tag-set` to `#tag-examples`), `components.adoc` (§ `#row-link`, `#clickable`), `shared/needs-and-find.adoc` (§ `#reasons`, the keys table),
`shared/texts-and-languages.adoc` (§ `#phases`), `workflow-visual-language.adoc` (§ `#mapping`), and the pictures `web-component-{reason-badge,phase-label,marker,row-link,count-badge,chip,status-label}.svg`.

1. **`ReasonBadge`**
 - What:
   - `ReasonBadge.svelte`, props `reason`, `count` (default 1), `tone` (`attention` or `danger`; derived from the reason when absent). A pill (`--radius-full`) whose text is the key `overview.needs.badge`
     (`! {count, plural, =1 {{reason}} other {# · {reason}}}`) with the reason word `overview.needs.<reason>`; the `!` comes from the catalogue.
   - Tone: `--color-attention-subtle` with `--color-attention-text`; `--color-danger-subtle` with `--color-danger` for `landing_failed` and the integrity reason. The wire value of that reason is `integrity_pause`
     (`NeedsOperatorKind`); the design text says `integrity_hold` (operator decision D-2, assumed: the design follows the code and the two documents are corrected in this pull request).
   - Add `overview.needs.completion_review` (en `completion review`, de `Abschlussprüfung`). A reason the catalogue does not know renders the catalogue fallback word in the attention tone.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `ReasonBadge.test.ts` covers every reason of the closed list (13) in `en` and `de`, the count form (`! 2 · decision`), the two tones, an unknown reason, and fails on a revert of the tone map.
     Evidence order: `test-first`.
   - The contrast pairs `attention-text` on `attention-subtle` and `danger` on `danger-subtle` are asserted in `tokens.test.ts` in both themes. Evidence order: `test-first`.
   - `OverviewCatalogueTest` passes with the new key in both locales. Evidence order: `test-first`.
 - Implements: `components-tags.adoc` § `#reason-badge`; `shared/needs-and-find.adoc` § `#reasons`.
2. **`PhaseLabel` and `Marker`**
 - What:
   - `PhaseLabel.svelte`, props `phase`, `role`: the phase icon (the phase map of `workflow-visual-language.adoc` § `#mapping`) and the word `phase.name.<phase>`, no box, `--color-text-secondary`;
     with a role the word `role.name.<role>` follows after ` · ` (`execute · implement`). Add `phase.name.init|refine|outline|plan|execute|finalize` (German equal to English) and `role.name.*` for the roles
     the runtime sends as the asking role. The `phase.<phase>` ids stay icon ids and are never text.
   - `Marker.svelte`, props `kind` (`recommended`, `default`, `this_one`) and `label`: a small neutral icon and its word in `--color-text-secondary`, after the option's label. No fill, no border, no brand colour.
     Add `common.marker.recommended|default|this_one`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `PhaseLabel.test.ts`: six phases with their icon and word in both locales; the role form; "no wire token reaches the DOM" for an unknown phase; the kind of a node is not repeated. `Marker.test.ts`: three kinds, no brand colour.
     Both fail on a revert. Evidence order: `test-first`.
   - The Java catalogue tests pass with the new keys, and `StateWordsCompletenessTest` still passes. Evidence order: `test-first`.
 - Implements: `components-tags.adoc` § `#phase-label`, `#markers`; `shared/texts-and-languages.adoc` § `#phases`.
3. **`RowLink`**
 - What:
   - `RowLink.svelte`, props `href`, `current` and the slots of a row (kind, title, tags, the row's acts). The title is the link, its target stretched over the whole row: the whole row takes hover, pointer and focus ring and a click anywhere opens the object.
     The controls inside (a `RowMenu`, a favourite `Toggle`, a copy `Chip`) lie above the link as separate targets and never open it. The selected row carries `aria-current="true"` and the `--color-brand-subtle` ground.
   - The row is 40 px high, 32 px compact, and 44 px target on a phone. The component stacks its controls above the link without writing a numeric `z-index` (PLAN-149 owns the layer tokens).
   - `href` is guarded by the exported `isAppPath` of `Link.svelte` (PLAN-147): an address that is not an app path renders no anchor. No second guard is written.
   - PLAN-147 residue (landing `landings/PLAN-147.md`): `ExternalLink.test.ts` gains the refused forms `https:\\host`, `https:/<tab>/host`, a leading C0 control character and `https:///host`
     in its table; the dead `WEB_SCHEMES` set of `ExternalLink.svelte:3` is removed (the `try/catch` stays for an invalid host).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `RowLink.test.ts`: exactly one anchor per row, the controls are not inside the anchor, `aria-current` follows `current`, the focus ring and hover are on the whole row, and a click on the title navigates while a click on a nested
     button does not. Fails on a revert. Evidence order: `test-first`.
   - `RowLink.test.ts` reuses the authority table of `Link.test.ts` (`//`, `/\`, tab/LF/CR between the slashes, a foreign origin): no anchor for any of them. Evidence order: `test-first`.
   - `ExternalLink.test.ts` refuses the four added forms; the guard still passes its existing table. Evidence order: `mutation-after`.
   - The dev-page fixture of a row with a `RowMenu`, a `Chip` and a `Toggle`-less favourite is checked in a browser: a click on the menu opens the menu and does not navigate (evidence order: `none`).
 - Implements: `components.adoc` § `#row-link`, `#clickable`, `#interaction-states`.
4. **The agreed rules on `StatusLabel`, `CountBadge`, `Chip` and `--color-success-subtle`**
 - What (these serve existing consumers; their visible fields and edges are enumerated, one criterion each):
   - `CountBadge` (consumers: `Tabs.svelte:131` and the dev page). Attention text is the key `overview.needs.count` (`! {count}`), so the `!` is the catalogue's; a plain count stays a neutral number. Edges:
     count `0`, `1`, `12`, `1000` (the number is shown whole, never capped); attention with and without `label` (the label stays the accessible name, the number `aria-hidden`); plain with a label; never brand-coloured.
   - `Chip` (consumer: the dev page). The accessible name is `common.chip.copy` (_Copy {value}_), the advisory names the act. Edges: an empty `value` renders nothing; a long value stays within `max-inline-size`;
     a refused clipboard shows no _Copied_; a second click inside the window restarts the reset; the copied state is announced politely.
   - `StatusLabel` (consumers: `CompletionReportView.svelte` and the dev page). Props unchanged (`tone`, `label`); add the fixed-width state slot for lists so dots stand one under the other. Edges: each of the five tones;
     a long German word wraps without moving the dot; the word is never the wire value.
   - `--color-success-subtle` gets its own value in both themes instead of the alias of `--color-bg-subtle` (`tokens.css:125` at `dea405d8`), and `tokens.test.ts:149` asserts the new value. The value is not stated in the agreed documents (operator decision D-3, assumed:
     chosen in the pull request within a measured rule, written into the table of `theme-readability.adoc`): `--color-text-primary` and `--color-text-secondary` ≥ 4.5:1 and `--color-success` ≥ 3:1 on it, in both themes.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Each listed field and edge has an assertion in `CountBadge.test.ts`, `Chip.test.ts`, `StatusLabel.test.ts`; each fails on a revert of its line. Evidence order: `mutation-after` (reverted code and failing assertion quoted in the pull request body).
   - `tokens.test.ts` asserts the new `--color-success-subtle` values and the three contrast rules in both themes. Evidence order: `test-first`.
   - The `.svelte` places that read `--color-success-subtle` are looked at in a browser in both themes; none loses contrast (evidence order: `none`).
 - Implements: `components-tags.adoc` § `#count-badge`, `#chip`, `#status-label`, `#tag-colour`.
5. **Fixtures and browser evidence**
 - What: fixtures of `ReasonBadge` (13 reasons, count form, both tones), `PhaseLabel` (six phases, with a role), `Marker` (three), `RowLink` (plain, current, with menu and chip) and the three conformed tags on the dev page in both themes and locales.
   The row of the closed examples in `components-tags.adoc` § `#tag-examples` is drawn once as a fixture.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The same browser check also covers the PLAN-147 fixture groups (Kinds on six grounds, Links, PrChip, Compose, FormStatus) and the PLAN-131 mascot row where it has landed:
     at 390×844 the `Link`/`ExternalLink` targets measure 44 px, at 1440×900 24 px; the console is read from load. PLAN-147's own check predates its last visual fix (`3988e4d1`) and its
     screenshots are lost (evidence order: `none`).
   - Browser check (Playwright MCP) of `/dev/components` at 1440×900 and 390×844, `de` and `en`, both themes; screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), `./mvnw -pl pm-api verify`, and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text, no timer waits in tests, the npm scripts stay; no static `style` attribute (`lib/csp.test.ts`).
- Every component obeys `components.adoc` § `#rules` and § `#new-component`; every new text comes from the catalogue in English and German; the text guard baseline does not grow. Java tests: JUnit 5, input from `Generators`, no Mockito.
- The design documents are updated in the same pull request wherever a deliverable says so (here: `components-tags.adoc` and `shared/needs-and-find.adoc` for the integrity reason, `theme-readability.adoc` for `--color-success-subtle`).
- Ports of this plan: mock web port 21480, app port 21481 (`20000 + 10 × 148`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `ReasonBadge`, `PhaseLabel`, `Marker`, `RowLink` do not exist — absence read in `pm-web-app/src/lib/components/` at `1cc0e10e`
- OBSERVED: `overview.needs.badge` and `overview.needs.count` exist; `overview.needs.completion_review` does not; the integrity key is `overview.needs.integrity_pause` — read at `pm-api/src/main/resources/messages/overview.en.properties:16-39`
- OBSERVED: the contract value is `integrity_pause` (`NeedsOperatorKind.INTEGRITY_PAUSE("integrity_pause")`) while `components-tags.adoc` § `#reason-badge` and `shared/needs-and-find.adoc:42` say `integrity_hold` — read at `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorKind.java:25` and both documents
- OBSERVED: `phase.name.*` and `role.name.*` are absent from the `*.en.properties` packs — read by `git grep` at `1cc0e10e`
- OBSERVED: `CountBadge` draws a bare `{count}` for an attention count; its consumers are `Tabs.svelte:131` and the dev page — read at `CountBadge.svelte:15-25` and by `git grep "<CountBadge"`
- OBSERVED: `Chip` has `data-advisory={$t('web.common.copy.title')}` and no `aria-label`; its only consumer is the dev page — read at `Chip.svelte:80-90`
- OBSERVED: `StatusLabel` is used by `CompletionReportView.svelte` and the dev page — read by `git grep "<StatusLabel"`
- OBSERVED: `--color-success-subtle: var(--color-bg-subtle)` at `tokens.css:113`, asserted as an alias at `tokens.test.ts:133`, and read in 13 places of `.svelte` files — read at both files and by `git grep`
- HYPOTHESIS: the roles the runtime sends as asking role are a closed list that an enum or `OperatorRolesCatalogueTest` already names — confirm/refute at `pm-api/src/test/java/de/planmarshall/api/message/OperatorRolesCatalogueTest.java` (verify-at-outline)
- HYPOTHESIS: the phase icons are drawn by `WorkflowIcon.svelte` from the ids `phase.<phase>` — confirm/refute at `pm-web-app/src/lib/components/WorkflowIcon.svelte` § props (verify-at-outline)
- Verify-first clause: re-read the consumers of `CountBadge`, `Chip` and `StatusLabel` at the current HEAD; if a new consumer appeared (PLAN-45), add its fields and edges to deliverable 4 before scoping.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/ReasonBadge.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ReasonBadge.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PhaseLabel.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PhaseLabel.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Marker.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Marker.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/RowLink.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/RowLink.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/CountBadge.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/CountBadge.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Chip.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Chip.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/StatusLabel.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/StatusLabel.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/WorkflowIcon.svelte` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-web-app/src/lib/components/Link.svelte` — present at HEAD dea405d8 (read; `isAppPath` exported)
- OBSERVED: `pm-web-app/src/lib/components/ExternalLink.svelte` — present at HEAD dea405d8
- OBSERVED: `pm-web-app/src/lib/components/ExternalLink.test.ts` — present at HEAD dea405d8
- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/OverviewCatalogueTest.java` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/StateWordsCompletenessTest.java` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/components-tags.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-147` (the dev page, `tokens.css` and `common.*` it edits; `KindMark` is the first slot of every row); `PLAN-45` landed.
- Overlaps with:
  - `PLAN-45` (running): `common.*`, the dev page. `PLAN-123` (running), `PLAN-126` (staged): `common.*`, `overview.*` properties.
  - `PLAN-49`, `PLAN-52` (parked): `overview.*` properties, the ReasonBadge consumers; they follow this plan.
  - `PLAN-70`, `PLAN-72` (parked): `tokens.css`.
- Adjacent to: `Tabs.svelte` (read, unchanged); `OverviewView.svelte` and the other views (they adopt the tags in their own plans); the terminal console badges (`operator.console.badge.*`, terminal package).
- Unblocks: 49 (ReasonBadge, RowLink), 51 (RowLink, Chip, CountBadge), 52 (RowLink, PhaseLabel, ReasonBadge), 55 and 62 (PhaseLabel, StatusLabel), 58, 59 (Marker, ReasonBadge).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since (PLAN-45, PLAN-147, PLAN-129, PLAN-130). Settle each HYPOTHESIS. Re-check the operator's answers to D-2 and D-3
and that `needs-and-find.adoc` § `#reasons` still lists the closed list of 13.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

Operator decision 2026-11-13 ("Fold into PLAN-148"): PLAN-133 (#424, `a139ec62`) moved the mock onto the B1 chat contract and left the
web chat behind. `pm-web-e2e/tests/showroom.spec.js:76-80` (the entry chat step on `ent-showroom`: `source-chat`, `chat-panel`,
`chat-proposal`, `fsynced`) now fails against the mock, and this plan's head gate runs `-Pe2e`.

- **D6 (new) — the e2e chat window.** Mark ONLY that chat step `test.fixme` (split it into its own `test` if needed, so the
  sources and file-tree checks of the same test keep running), with a comment naming `PLAN-161` (the chat bridge), which removes the mark.
  Do not touch the web client's chat code: `PLAN-161` migrates it. Any other e2e failure at the head that traces to the chat contract gets
  the same treatment, each listed in the pull request body. Done when: `-Pe2e` passes at the head and the pull request lists every
  `fixme` with its owner. Evidence order: `none`.
- Surface: `pm-web-e2e/tests/showroom.spec.js` (present at `a139ec62`).
- Rebase onto `a139ec62`: `running-work.json` and `fully-onboarded.json` changed heavily; the scenario loader now refuses unknown chat keys
  at load. No source of this plan's surface conflicts.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-148-web-foundation-tags-and-row-link.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
