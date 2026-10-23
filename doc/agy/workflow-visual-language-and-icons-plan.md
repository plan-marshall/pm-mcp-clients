# Implementation Plan: `workflow-visual-language-and-icons` (A Distinct Icon for Every Workflow and Sub-Workflow)

This implementation plan introduces a small visual language for workflow units. The web UI is text-driven today: a phase, a sub-workflow, a remediation policy, and a recipe are distinguished only by their names. The plan adds a stable `icon` field to the `pmwf` frontmatter, delivers one original icon per known workflow unit, documents how to make and maintain those icons, and uses them in the web app wherever a unit or phase is shown.

The icon is **data, not decoration**: every unit's frontmatter carries a stable icon id, and the client resolves that id to a delivered SVG. There is no unreleased compatibility layer: the field is required and every existing unit fixture is migrated in the same change, but the DSL format stays at **1** — the format version changes only after a release. A future/unknown id still has a readable client fallback, but a missing field is a schema refusal.

The work is deliberately split across the repositories that own each part:

| Part | Repository | Why there |
|---|---|---|
| `icon` field in the `pmwf` schema, reader, printer, and schema docs | `pm-mcp-core` (`pm-workflow`) | The frontmatter is a closed, versioned schema owned by `pm-workflow`; a client must not invent a wire field. |
| Visual specification, icon assets, registry, client model, and web usage | `pm-mcp-clients` | The visual language and the delivered browser assets live with the web app; `pm-api` carries the client-facing frontmatter model. |
| `icon:` lines in the shipped default units and recipes | `plan-marshall-mcp` | The default `.pmwf` resources are model-facing content of the assembly, not of this repository. |

> [!IMPORTANT]
> **Current sibling dependency**: `plan-marshall-mcp`'s `default-units-and-drift-tests` package is **blocked** and the default `pm-mcp-server/src/main/resources/workflows/*.pmwf` resources do not exist on its `main` branch. This plan prepares the schema, visual language, icons, and web usage now. The `icon:` lines in the shipped unit descriptions are a gated final phase: the worker must not invent default units. The plan records that dependency explicitly and cannot claim that phase complete until the sibling package lands.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-3`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-3) (Design a Plan's Process), [`UC-PLN-1`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1) (Follow a Plan Down to Its Parts), [`UC-ONB-3`](../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc#UC-ONB-3) (Languages and Message Catalogue) |
| **Schema** | [`workflow-dsl/01-grammar.adoc#unit-frontmatter`](../../../pm-mcp-core/doc/specification/workflow-dsl/01-grammar.adoc#unit-frontmatter); [`pm-workflow/doc/developer/workflow-dsl.adoc#unit-frontmatter`](../../../pm-mcp-core/pm-workflow/doc/developer/workflow-dsl.adoc#unit-frontmatter); [`workflow-dsl/04-ast-node-model.adoc#process-designer`](../../../pm-mcp-core/doc/specification/workflow-dsl/04-ast-node-model.adoc#process-designer) |
| **Default units** | [`plan-marshall-mcp/doc/plans/default-units-and-drift-tests.adoc`](../../../plan-marshall-mcp/doc/plans/default-units-and-drift-tests.adoc) (**blocked**); [`state-catalogue.adoc`](../../../pm-mcp-core/doc/specification/state-catalogue.adoc) |
| **Local client design** | [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc), [`doc/design/web-app/entry.adoc`](../design/web-app/entry.adoc) |
| **Current model** | [`WorkflowUnit.java`](../../pm-api/src/main/java/de/planmarshall/api/designer/WorkflowUnit.java), [`WorkflowFrontmatter`](../../pm-web-app/src/lib/api/client.ts), [`frontmatter.ts`](../../pm-web-app/src/lib/utils/frontmatter.ts) |
| **Predecessor plans** | [`implemented/web-reports-system-and-designer-plan.md`](implemented/web-reports-system-and-designer-plan.md), [`implemented/web-session-pairing-and-dev-host-plan.md`](implemented/web-session-pairing-and-dev-host-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

### 1.1 Finding Analysis and Decisions

| # | Finding / decision | Consequence | Correction in this plan |
|---|---|---|---|
| **V1** | Workflow surfaces are text-only. `ProcessDesigner` lists unit names and descriptions; phase badges in `PlanView`, `OverviewView`, and `EpicView` are text. | A person must read every label to tell an init phase from a remediation policy. | Introduce one icon per known unit/phase and use it beside the existing text; text remains the accessible source of truth. |
| **V2** | The `pmwf` frontmatter has no carrier for a visual identity. `UnitFrontmatterReader` accepts a closed key set. | Any client-side mapping by unit name would be a second, drifting registry. | Add a **required** `icon` field to the schema, validated as an icon id; keep the DSL format at 1 and migrate every existing unit fixture. The value travels with the unit through the API. |
| **V3** | There is no delivery or maintenance mechanism for icons. | Assets, API names, and CSS would drift. | Add a visual specification, an asset directory, a typed registry, one `WorkflowIcon` component, and drift tests. Adding an icon becomes an SVG + registry + spec row; no view edits. |
| **V4** | The default `.pmwf` resources are not available on `main`. | The `icon:` lines cannot be added to shipped descriptions today. | Prepare the exact unit-to-icon map and a sibling follow-up. The worker must not fabricate default units; the phase stays gated until the default-units package lands. |
| **V5** | Blending third-party icons is a commercial-software licensing risk. | An attractive shortcut could impose attribution, trademark, or redistribution obligations. | Standalone original icons are the default. A blend is allowed only after the provenance gate in the visual specification: commercial-use permission, modification/redistribution permission, attribution satisfiable, no trademark/logo confusion, and written provenance. The Git icon is our own branch/commit metaphor, not the Git logo. |
| **V6** | The web app already has phase names and unit names but no icon field in its frontmatter parser. | A schema-only field would not appear in the designer or views. | Extend `WorkflowUnit.Frontmatter`, `WorkflowFrontmatter`, and the designer's `FrontmatterFields`; preserve the field when editing, show it read-only/preview in the panel, and fall back safely. |

---

## 2. Scope & Objectives

### Part A: Schema Field (`pm-mcp-core`)

1. Add a **required** `icon` key to the unit frontmatter:
   - `icon: phase.init` (the value is an icon id, not a file path).
   - Grammar: `[a-z][a-z0-9._-]{0,63}`; no slash, backslash, whitespace, colon, or `..`.
   - Required in the schema. A unit without the key is refused.
2. Keep the DSL format at **1**:
   - `ProjectUnitLoader.COMPILED_FORMAT_VERSION` stays `1`; no unit header changes.
   - The format version is changed only after a release, not for this pre-release field addition.
3. Extend `UnitFrontmatter`, `UnitFrontmatterReader`, and `WorkflowPrinter`:
   - Read once, at most once; add `icon` to `REQUIRED_KEYS`.
   - Print it after `type` and before the interface lines; always print it for a valid unit.
   - Round-trip tests, missing-icon refusal, and invalid-value refusal.
4. Extend the schema docs and add an ADR:
   - `workflow-dsl/01-grammar.adoc` field table and frontmatter example.
   - `pm-workflow/doc/developer/workflow-dsl.adoc` fields.
   - ADR `workflow-dsl/008-required-icon-field.adoc`: required field within format 1, no backward compatibility, every existing unit migrated before the first release, and no format bump before a release.
5. Migrate the existing fixtures:
   - Audit every `pm-workflow/src/test/resources/**/*.pmwf` and every inline unit text in `pm-workflow` tests; add `icon:` and keep `format 1`.
   - Preserve the intended first refusal in each refused fixture: a fixture that tests a different rule gets an icon; add a dedicated `icon-missing.pmwf` and `icon-invalid.pmwf` for the new rule.
   - Update diagram, fuzz, unit-fixture, and passing fixtures; no test may be waived for the new required field.
6. The parser is the gate; the client model and default units follow the same required contract.

### Part B: Visual Specification (`pm-mcp-clients`)

1. Add `doc/design/web-app/workflow-visual-language.adoc` as the single visual authority. It must cover:
   - **Visual grammar**: 24×24 grid, 2 px monochrome stroke, round caps/joins, no fills except deliberate small accents, one clear silhouette, legible at 16/20/24 px, no text in the icon, light/dark via `currentColor`.
   - **Semantic families**: phases, flow/control, git, remediation, re-planning, adjudication, axes, compiled units, recipes, fallback. Shapes must be distinguishable by silhouette, not color alone.
   - **Technical contract**: SVG 1.1, `viewBox="0 0 24 24"`, `fill="none"`, `stroke="currentColor"`, no `<text>`, `<script>`, external `href`, embedded raster, CSS `style` block, or hard-coded colors; optimize with SVGO; one icon per file; file name is the id with dots replaced by hyphens (`phase.init` → `phase-init.svg`).
   - **Naming and stability**: icon ids are stable; art changes keep the id; a rename/deprecation needs an alias/ migration note; the unit name and the icon id are different concepts.
   - **Licensing and provenance**: standalone original icons by default; the blend gate above; a provenance table for any non-original source (author, source URL, license, commercial-use conclusion, attribution, approval). No brand logos, no copied icon-font glyphs, no remote SVG references.
   - **Accessibility and usage**: decorative icons are `aria-hidden="true"`; the visible label or an accessible name remains; never icon-only for an action without a name; sizes and spacing; no color-only meaning; focus/keyboard unaffected.
   - **Update checklist**: add SVG, add registry entry, add spec row, add `icon:` to the unit frontmatter, run the registry/drift tests.
2. Add the **icon catalogue table** (Part C) to the spec with id, meaning, unit/phase, asset, and status.
3. Keep the document in this repository; it is the visual contract, not a schema.

### Part C: Icons and Registry (`pm-mcp-clients`)

1. Add assets under `pm-web-app/src/lib/icons/workflows/`, one SVG per id.
2. Add `pm-web-app/src/lib/icons/workflowIcons.ts`:
   - Imports each SVG with Vite's `?url`.
   - Exports `WORKFLOW_ICON_IDS`, `workflowIconUrl(id)`, and a typed `WorkflowIconId`.
   - An unknown id resolves to `workflow.default`; no exception, no broken image. An empty value is a schema refusal, not a valid unit.
3. Add `pm-web-app/src/lib/components/WorkflowIcon.svelte`:
   - CSS mask with `currentColor`, `aria-hidden="true"`, sizes 16/20/24, optional `label` for non-decorative use.
   - No `{@html}` and no raw SVG from server content.
4. Add tests:
   - Every registry id has an existing asset and is declared in the visual catalogue.
   - Every known unit/phase mapping in Part C resolves.
   - Duplicate ids and missing assets fail.
   - SVG lint: viewBox, currentColor, no forbidden elements/external references, no text.
   - The fallback renders for an unknown id.
5. Icon set (known units and phases):
   - Phases: `phase.init`, `phase.refine`, `phase.outline`, `phase.plan`, `phase.execute`, `phase.finalize`, `flow.land`.
   - Sub-workflows: `git.commit`, `remediation.ci`, `remediation.review`, `remediation.analysis`, `remediation.build`, `remediation.merge`, `remediation.qgate`, `flow.replan`, `flow.adjudicate`.
   - Axes: `axis.landing.files`, `axis.landing.pr`, `axis.landing.main`, `axis.method.test-first`, `axis.method.tests-after`, `axis.ask.every`, `axis.ask.key`, `axis.ask.questions`.
   - Compiled/family: `unit.plan`, `unit.epic`, `unit.decomposition`, `unit.review`, `unit.decision`, `unit.chat`, `unit.onboarding`, `unit.setup`, `unit.start`, `unit.source`, `unit.task`, `unit.entity`, `unit.landing`.
   - Recipes/fallback: `recipe.default`, `workflow.default`.
   - The catalogue maps the unit ids (`phase-1-init`, `land`, `git-commit-and-push`, `remediation.*`, `replan`, `adjudicate-decisions`, axis variant ids, compiled unit ids) to these ids.

### Part D: Web App Usage (`pm-mcp-clients`)

1. Extend the client model:
   - `pm-api` `WorkflowUnit.Frontmatter` adds a required `icon` and its JSON round-trip.
   - `pm-web-app` `WorkflowFrontmatter` adds required `icon`.
   - `frontmatter.ts` `FrontmatterFields` adds required `icon`, reads/writes it, keeps an unknown id as its value (not as an extra field), and `emptyFrontmatter` uses `workflow.default` so a newly added frontmatter validates.
2. `ProcessDesigner`:
   - Show `WorkflowIcon` in the installed-unit list and the selected unit header.
   - Show a read-only icon preview in the frontmatter panel (and, if cheap, a select of delivered ids; the text remains the source of truth).
   - Use `unit.frontmatter.icon`; an empty draft value shows `workflow.default` as a defensive fallback and the schema validation reports the missing field. Do not keep a name-to-icon compatibility map.
3. `PlanView`:
   - Put the phase icon beside the current-phase badge and, where the process tree shows phase headers, beside those headers.
4. `OverviewView` and `EpicView`:
   - Put the phase icon beside their phase badges. These are the same phase ids, so no new data is needed.
5. Optional, only if the recipe representation already exposes a workflow frontmatter icon:
   - `StartView` recipe cards. If it does not, leave it out and record the follow-up rather than inventing a new API field.
6. Do not iconify task nodes, worker rows, or arbitrary plan titles; the visual language is for workflows/phases, not every entity.

### Part E: Shipped Unit Descriptions (`plan-marshall-mcp`, gated)

1. **Precondition**: the `default-units-and-drift-tests` package produces `pm-mcp-server/src/main/resources/workflows/*.pmwf` (currently blocked; verify before starting this phase).
2. When present, add `icon:` to every default unit's frontmatter using the Part C map; add `icon:` to the shipped recipe workflows with `recipe.default` or a specific recipe icon.
3. Add a sibling drift test/check:
   - Every default `.pmwf` has an `icon`.
   - Every icon id resolves in the clients registry (a small script can take the clients checkout or a checked-in icon-id manifest).
   - No duplicate icon id among units unless intentionally shared by a family.
4. If the precondition is not met, **do not create stub units**. Record the blocked dependency and leave this phase open for the sibling follow-up.

### Part F: Verification

1. Schema: `pm-mcp-core` reader/printer tests and docs; valid/invalid `icon` cases.
2. Client: `npm test`, `npm run check`, `npm run lint`, Prettier on changed files; icon registry/SVG lint; `npm run test:urls` unaffected.
3. Web: at least one rendered test proving the designer and a phase badge use the icon and that an unknown id falls back.
4. Cross-repo: when Part E runs, the sibling drift script; otherwise record the blocked status.
5. Full pre-commit in each changed repository.

### Non-Goals

- Adding a full design system, illustration set, or color palette beyond the icon language.
- Replacing text labels with icons; text stays.
- Icons for plans, tasks, workers, or projects (only workflows/phases and their unit families).
- Changing the DSL format version; the required field stays within format 1 until after a release.
- Copying third-party brand logos or icon-font glyphs.
- Unblocking the `plan-marshall-mcp` default-units package.

---

## 3. Cross-Repository Isolation & Landing Order

> [!IMPORTANT]
> **Pre-flight — pull `main` in every repository before branching**: before creating any worktree or branch in `pm-mcp-core`, `pm-mcp-clients`, or `plan-marshall-mcp`, run `git checkout main && git pull --rebase origin main` in that repository. Repeat it for each repository in the landing order; never create a worktree from a stale `main`.

| Order | Repository | Worktree | Branch | Scope |
|---|---|---|---|---|
| **1** | `pm-mcp-core` | `/Users/oliver/git/worktrees/pm-mcp-core-workflow-icon-field` | `feat/workflow-icon-field` | Frontmatter field, reader, printer, docs, ADR, schema tests. |
| **2** | `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-workflow-visual-language` | `feat/workflow-visual-language` | Visual spec, icons, registry, `pm-api`/web model, designer and phase views, tests. |
| **3** | `plan-marshall-mcp` | `/Users/oliver/git/worktrees/plan-marshall-mcp-workflow-default-icons` | `feat/workflow-default-icons` | `icon:` lines in default units/recipes, sibling drift test. **Blocked** until `default-units-and-drift-tests` is available. |

**Landing rules**:
- The schema workstream lands first; the clients model can be developed in parallel, but the clients PR should not claim runtime delivery until the schema is in. Both use the required field within format 1; there is no optional-path fallback.
- The clients workstream is the primary plan record; its completion report links the schema PR and the default-units follow-up.
- The default-units workstream runs only after the default-unit resources exist; it must not create placeholder `.pmwf` files.
- Each repository uses its own pre-commit and review rules; worker PRs never use `skip-bot-review`.

### Boundary Rules

- **`pm-mcp-core` sole owner**: `UnitFrontmatter`, `UnitFrontmatterReader`, `WorkflowPrinter`, their tests, the workflow-DSL docs and the new ADR.
- **`pm-mcp-clients` sole owner**: `pm-api` `WorkflowUnit`, web `WorkflowFrontmatter`, `frontmatter.ts`, the visual spec, `pm-web-app/src/lib/icons/workflows/**`, `workflowIcons.ts`, `WorkflowIcon.svelte`, `ProcessDesigner`, `PlanView`, `OverviewView`, `EpicView`, related tests.
- **`plan-marshall-mcp` sole owner**: `pm-mcp-server/src/main/resources/workflows/*.pmwf`, default recipe resources, the sibling drift test/script, its README/index as applicable.
- **No touch**: task/worker/project representations, pairing/session code, the Vite dev-host configuration, unrelated catalogues.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Clients worktree created **after pulling `main`**; the schema worktree is allocated at the start of its own workstream and the default-units worktree stays gated. Step 1 committed and pushed in the primary repository.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-java:java-core` for the `pm-workflow` schema? Use the sibling's Java standards and tests.
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the registry, component, parser, and views.
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for registry, SVG, frontmatter, and view tests.
  - `plan-marshall-persona-security-expert` for SVG trust (no external refs, no raw HTML) and licensing/provenance review.
  - `plan-marshall-persona-documenter` for the visual specification and schema docs.
  - `plan-marshall-persona-code-reviewer` for review-bot triage.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Completion report written for the clients plan (`doc/agy/workflow-visual-language-and-icons-plan-report.md`), plan moved to `doc/agy/implemented/`, **all relative links rebased**, and dashboard/index updated. Record the sibling PRs and the default-units gate. (Do NOT move the report to `verified/` — Step 8 is reserved for the orchestrator.)
- [ ] 5. Pre-commit passed in each changed repository: `./mvnw verify -Ppre-commit` for `pm-mcp-core` and `pm-mcp-clients`; the sibling's own gate for `plan-marshall-mcp`.
- [ ] 6. PRs opened in the landing order (no `skip-bot-review`); review windows and 100% findings resolution via `persona-code-reviewer`.
- [ ] 7. Enqueued and landed in the landing order; worktrees and branches cleaned up.
- [ ] 8. Post-landing verification performed; the clients report moved to `doc/agy/verified/`. The default-units phase remains a tracked gate until its sibling PR lands.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`; its links use `../meta`, `../design`, `../../../pm-mcp-core`, `../../../plan-marshall-mcp`, `../../../plan-marshall-documentation`, and `implemented/<name>.md`. When Step 4 moves it to `doc/agy/implemented/`, rebase every link (`../../meta`, `../../design`, `../../../../pm-mcp-core`, `../../../../plan-marshall-mcp`, `../../../../plan-marshall-documentation`, `../runbook.md`, sibling `<name>.md`) and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Schema** | `plan-marshall-persona-implementer` (sibling Java standards) | `pm-dev-java:junit-core` | Required frontmatter field within format 1, reader/printer, docs/ADR, fixture migration. |
| **Phase 2: Visual language** | `plan-marshall-persona-documenter` | `pm-dev-frontend:css`<br>`plan-marshall-persona-security-expert` | Visual grammar, SVG contract, accessibility, license/provenance gate. |
| **Phase 3: Icons & registry** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-module-tester` | SVG set, typed registry, CSS-mask component, drift tests. |
| **Phase 4: Web usage** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-module-tester` | Process designer, phase badges, frontmatter parsing, fallback. |
| **Phase 5: Default units (gated)** | `plan-marshall-persona-implementer` | `plan-marshall-persona-documenter` | Apply `icon:` to shipped `.pmwf` files, drift check, blocked dependency. |
| **Phase 6: Verification & review** | `plan-marshall-persona-code-reviewer` | `plan-marshall-persona-integration-tester` | Cross-repo evidence, review triage, report. |

---

## 6. Phased Technical Recipe

### Phase 1: Add the Schema Field (`pm-mcp-core`)

- [ ] **1.1 Extend the frontmatter model**:
  - In `UnitFrontmatter`, add a required `String icon` after `type`. Update the Javadoc.
  - In `UnitFrontmatterReader`, add `KEY_ICON` to `KEYS`, `SINGLE_KEYS`, and `REQUIRED_KEYS`; read it once; validate the icon-id grammar; put the value into `Complete`.
  - In `WorkflowPrinter`, always print `icon` after `type`; keep the documented field order.
  - Keep `ProjectUnitLoader.COMPILED_FORMAT_VERSION` at `1`; no `.pmwf` header changes.
- [ ] **1.2 Migrate the fixtures and tests**:
  - Audit every `pm-workflow/src/test/resources/**/*.pmwf` and every inline unit text in `pm-workflow` tests; add `icon:` and keep `format 1`.
  - Preserve the intended first refusal in refused fixtures: add an icon where the fixture tests another rule; add dedicated `icon-missing.pmwf` and `icon-invalid.pmwf`; keep format-refusal fixtures at their tested format.
  - A valid unit with `icon: phase.init` round-trips through parser and printer.
  - A missing `icon` is refused; invalid values (`../x`, `Phase.Init`, `a/b`, 65+ chars) are refused; a repeated `icon` is refused.
  - The JSON/API model tests in `pm-api` mirror the required wire key.
- [ ] **1.3 Documentation**:
  - Update the grammar field table, the developer guide's field list, and the frontmatter example.
  - Add the required-field ADR: field required within format 1, no backward compatibility, every existing unit fixture migrated, and the format version changes only after a release.

### Phase 2: Visual Specification (`pm-mcp-clients`)

- [ ] **2.1 Write `doc/design/web-app/workflow-visual-language.adoc`** with the sections in Part B. Include the icon catalogue table (Part C) and the provenance table.
- [ ] **2.2 Define the visual grammar**:
  - 24×24 grid, 2 px stroke, round caps/joins, no fills/text, currentColor, legible at 16 px, distinct silhouettes per family.
  - Phase lifecycle metaphors: init (seed/spark), refine (lens/polish), outline (outline tree), plan (checklist/calendar), execute (play/gear), finalize (flag/check), land (merge arrow).
  - Sub-workflow metaphors: git (branch/commit), remediation (wrench/loop), replan (reroute), adjudication (scales).
  - Axis metaphors: files (document), pull request (PR arrow), main (branch), test-first (test tube first), tests-after (clock/test), ask variants (speech/question/key).
  - Compiled family metaphors: plan/epic/decomposition/review/decision/chat/onboarding/setup/start/source/task/entity/landing.
- [ ] **2.3 Define the technical contract and license gate** exactly as in Part B. State explicitly: **standalone original icons only** for this delivery; blends require a completed provenance row and a commercial-use conclusion. The Git icon is not the Git logo.
- [ ] **2.4 Define accessibility/usage**:
  - Icons are decorative beside text (`aria-hidden`), or carry a title/accessible name when standalone.
  - No icon-only actions; no color-only meaning; 16/20/24 px; currentColor; high contrast.
- [ ] **2.5 Define the update workflow**:
  - Add SVG → add registry entry → add/update the catalogue row → add `icon:` to the unit → run tests. No view code change.

### Phase 3: Icons and Registry (`pm-mcp-clients`)

- [ ] **3.1 Create the SVG set** from the Part C catalogue, using the visual spec. Use the file naming convention (dots → hyphens). Keep every icon monochrome and self-contained.
- [ ] **3.2 Add `workflowIcons.ts`**:
  - Import every SVG with `?url`; export a frozen `WORKFLOW_ICON_IDS` and `workflowIconUrl(id)`.
  - Unknown id → `workflow.default`; an empty value is a schema error.
  - Add a typed `WorkflowIconId`.
- [ ] **3.3 Add `WorkflowIcon.svelte`**:
  - CSS mask with `currentColor`; `aria-hidden` by default; optional label; size prop.
  - No `{@html}`, no remote URLs, no inline server content.
- [ ] **3.4 Add `workflowIcons.test.ts` / SVG lint**:
  - Asset exists for every id; known unit/phase mapping resolves; no duplicates; fallback works.
  - Parse each SVG: `viewBox="0 0 24 24"`, `currentColor`, no `<text>/<script>/<image>/<use href=...>`, no external `href`, no hard-coded color.
  - Every id has a row in the visual catalogue (a small parser over the table or a checked-in JSON companion; choose one source of truth and state it).
- [ ] **3.5 Keep the structure simple**:
  - Adding a new unit icon requires only a new SVG + registry entry + catalogue row + `icon:` line. Do not add per-view conditionals; resolve by id.

### Phase 4: Client Model and Web Usage (`pm-mcp-clients`)

- [ ] **4.1 Extend the client contract**:
  - `pm-api` `WorkflowUnit.Frontmatter`: add required `icon`, `toMap`/`fromMap`, and tests.
  - Web `WorkflowFrontmatter`: add required `icon: string`.
  - `frontmatter.ts`: add `icon` to `FrontmatterFields`, `SCALAR_FIELDS`, `emptyFrontmatter` (`workflow.default`), read/write logic; update the designer panel.
- [ ] **4.2 ProcessDesigner**:
  - Unit list rows: `WorkflowIcon` before the unit name.
  - Selected unit header: icon + name.
  - Frontmatter panel: icon preview and (if cheap) a select of delivered ids; editing writes `icon:`; an unknown id stays visible and falls back visually; an empty draft shows the fallback until validation refuses it.
  - Resolve `unit.frontmatter.icon`; do not keep a name-to-icon compatibility map.
- [ ] **4.3 PlanView**:
  - Phase badge icon via `phase.<current_phase>`; phase headers in the process tree when present.
- [ ] **4.4 OverviewView / EpicView**:
  - Phase badge icons; same phase map.
- [ ] **4.5 Optional recipe surface**:
  - Only if `RecipeRef`/catalogue already exposes the workflow frontmatter icon; otherwise leave it as a follow-up. Do not add a redundant API field just for an icon.
- [ ] **4.6 Tests**:
  - A rendered designer test asserts the icon for a known unit and the fallback for an unknown id; an empty draft is a validation error, not a valid unit.
  - A phase badge test asserts `phase-5-execute` shows `phase.execute`.
  - A frontmatter parser round-trip preserves `icon` and unknown extras.
  - Accessibility: decorative icon is `aria-hidden`; text remains.

### Phase 5: Shipped Unit Descriptions (`plan-marshall-mcp`, gated)

- [ ] **5.1 Verify the precondition**: `plan-marshall-mcp/pm-mcp-server/src/main/resources/workflows/*.pmwf` exists. If not, stop this phase; record the blocked `default-units-and-drift-tests` package in the report; do not create stubs.
- [ ] **5.2 Apply the map**:
  - Add `icon: <id>` to each default unit frontmatter (after `type`).
  - Add `icon: <recipe id>` or `recipe.default` to each shipped recipe workflow.
  - Use the Part C mapping; no duplicate ids within the same family unless the visual spec says the family intentionally shares one.
- [ ] **5.3 Add the sibling drift check**:
  - Every default `.pmwf` has an icon; every icon resolves in the clients registry (script reads the clients checkout or a checked-in icon-id manifest).
  - The check fails on a missing/unknown/duplicate icon.
- [ ] **5.4 If blocked**: the clients/schema workstreams still land; the plan report records Part E as blocked and the exact follow-up branch/PR condition.

### Phase 6: Verification & Report

- [ ] **6.1 Tests**:
  - `pm-mcp-core`: `./mvnw verify -Ppre-commit` (or the module’s gate).
  - `pm-mcp-clients`: `npm test`, `npm run check`, `npm run lint`, Prettier on changed files, the icon registry/SVG tests, and `./mvnw verify -Ppre-commit`.
  - `plan-marshall-mcp`: its own gate plus the default-unit drift check (when unblocked).
- [ ] **6.2 Manual visual check**:
  - Designer list/header and plan/overview/epic phase badges show distinct icons at 16 and 24 px in light/dark; text remains; unknown id shows the fallback.
  - No icon-only action without a name.
- [ ] **6.3 Report**:
  - Write `doc/agy/workflow-visual-language-and-icons-plan-report.md` with the schema PR, clients PR, icon catalogue, license/provenance statement, view evidence, test results, and the default-units gate.
  - Move the plan to `doc/agy/implemented/`, rebase links, update the implemented index and dashboard.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **V1** | Every designable workflow unit and phase has a distinct icon in the catalogue; icons are used in the Process Designer and phase badges. | Registry test + designer/plan/overview/epic tests + manual visual check. |
| **V2** | `icon` is a required frontmatter field with the icon-id grammar within **format 1**; a missing or invalid id is refused; a valid unit round-trips; the format version is unchanged. | `pm-mcp-core` reader/printer tests; schema docs; fixture migration. |
| **V2** | The client `WorkflowUnit.Frontmatter`/web `WorkflowFrontmatter` carries `icon` and round-trips it. | `pm-api` + web parser tests. |
| **V3** | The visual specification covers visual grammar, SVG contract, accessibility, license/provenance, naming, catalogue, and update process. | `doc/design/web-app/workflow-visual-language.adoc`. |
| **V3** | Adding a new icon requires only an SVG, a registry entry, a catalogue row, and the unit `icon:` line; no view edits. | Registry/component design + update checklist. |
| **V4** | The default-unit `icon:` phase either lands with real `.pmwf` resources or is explicitly recorded as blocked; no stub units are invented. | Sibling PR/drift check or blocked evidence in the report. |
| **V5** | The delivered set is original/standalone; no third-party brand logo or remote asset; any blend has a completed provenance row and commercial-use conclusion. | Visual spec provenance table + SVG lint (no external refs). |
| **V6** | Unknown icon ids fall back to `workflow.default`; an empty draft is a schema error; text labels remain; icons are decorative (`aria-hidden`) unless named. | Fallback test + accessibility test. |
| **V7** | Tests and pre-commit pass in every changed repository. | Phase 6 output; completion report. |

---

## 8. Risk & Rollback Notes

- **Sibling blockage**: the default-units package is blocked; do not create placeholder `.pmwf` files or claim the descriptions are updated. The schema/client/visual layer is useful immediately; the sibling phase is a tracked gate.
- **Schema compatibility**: there is no backward compatibility: `icon` is required and every existing `.pmwf` fixture and shipped unit is migrated before the first release. The DSL format stays at **1**; the format version changes only after a release, as the project is still in development. A unit without `icon` is refused.
- **License risk**: standalone icons are the default. A blend is allowed only after the provenance gate; the Git icon is our own branch/commit metaphor. No third-party paths, fonts, or remote references.
- **Visual ambiguity**: icons are decorative beside text; no icon-only action, no color-only meaning, test at 16 px, light/dark.
- **Maintenance**: the registry is the single id source; the SVG lint and catalogue test prevent orphan assets/unknown ids. A new icon is a four-step change, not a view edit.
- **Cross-repo drift**: the schema, client model, and default units are three repositories. The clients report links both sibling PRs; the default-unit drift script is the final gate.
- **Rollback**: revert the relevant PR in each repository and restore the pre-icon units; the format stays 1 throughout. There is no released compatibility obligation; the pre-release migration is a clean cut, and an unknown icon id still falls back to `workflow.default` after a partial client rollback.
