# Completion Report: `workflow-visual-language-and-icons`

**Plan**: [`doc/agy/implemented/workflow-visual-language-and-icons-plan.md`](implemented/workflow-visual-language-and-icons-plan.md)
**Branch**: `feat/workflow-visual-language`
**Repositories**: `pm-mcp-clients` (this report), `pm-mcp-core` (schema, landed), `plan-marshall-mcp` (default units, still gated)

---

## 1. Executive Summary

The clients workstream of the workflow visual language is complete. Every designable workflow unit, sub-workflow, phase, plan axis, compiled unit, and recipe now has one original monochrome SVG, a typed registry, a CSS-mask component, a published visual specification, and a catalogue that the tests enforce. The `icon` field travels from the `pmwf` frontmatter through the `pm-api` designer record and the web frontmatter parser into the process designer and the phase badges of the plan, overview, and epic views.

The client never relies on a name-to-icon table for a unit that carries its own frontmatter: it resolves `unit.frontmatter.icon` through the registry. An unknown or not-yet-known id stays visible in the editor and renders `workflow.default`; an empty value is left empty for the schema to refuse. The DSL format stays at **1**, and the default-units phase remains blocked exactly as the plan requires.

## 2. Delivered Artifacts

### 2.1 Visual Specification (`pm-mcp-clients`, `doc/**`)

| File | Change |
|---|---|
| `doc/design/web-app/workflow-visual-language.adoc` | New single visual authority: visual grammar, semantic families, SVG technical contract, naming and stability, licence/provenance gate, accessibility and usage, the 40-row icon catalogue, the unit/phase map, the update checklist, and the sources-of-truth table. |
| `doc/design/web-app/README.adoc` | Indexed the new document in the Web App list and the structure table. |
| `doc/design/web-app/process-designer.adoc` | Added the icon row to the frontmatter sketch and the field's behaviour (read-only preview, unknown fallback, empty value is a schema finding). |
| `doc/design/web-app/process-designer-texts.adoc` | Added `designer.frontmatter.icon` (EN/DE) to the designer text catalogue. |

### 2.2 Icons, Registry, and Component (`pm-mcp-clients`, `pm-web-app`)

| File | Change |
|---|---|
| `pm-web-app/src/lib/icons/workflows/*.svg` | 40 original icons, one per id, named by the id with dots replaced by hyphens. |
| `pm-web-app/src/lib/icons/workflowIcons.ts` | Typed `WORKFLOW_ICON_IDS`, `WorkflowIconId`, frozen URL map, `isWorkflowIconId`, `workflowIconUrl`, `phaseIconId`, and `unitIconId` (including the remediation category rule and the catalogue aliases). |
| `pm-web-app/src/lib/components/WorkflowIcon.svelte` | CSS-mask component with `currentColor`, sizes 16/20/24, `aria-hidden` by default, optional accessible label, no `{@html}` and no remote content. |
| `pm-web-app/src/vite-env.d.ts` | Vite's asset-module types for `?url` imports. |

### 2.3 Client Contract and Web Usage (`pm-mcp-clients`)

| File | Change |
|---|---|
| `pm-api/.../designer/WorkflowUnit.java` | Required `icon` on `Frontmatter`, JSON `icon` in `toMap`/`fromMap`, Javadoc updated. |
| `pm-api/.../messages/designer.{en,de}.properties` | `designer.frontmatter.icon` in both languages. |
| `pm-mock-runtime/.../DesignerRegistry.java` | Reads `icon:` from the parsed frontmatter block and carries it into the API record. |
| `pm-mock-runtime/.../scenarios/{designer,fully-onboarded}.json` | Every fixture unit that has a frontmatter now carries its icon, in both the JSON representation and the unit text. |
| `pm-web-app/src/lib/api/client.ts` | `WorkflowFrontmatter.icon`. |
| `pm-web-app/src/lib/utils/frontmatter.ts` | `icon` in `FrontmatterFields`, `SCALAR_FIELDS`, read/write paths, `emptyFrontmatter()` (`workflow.default`) and the fresh-block writer. |
| `pm-web-app/src/lib/views/ProcessDesigner.svelte` | Icon in the installed-unit rows and the selected header; read-only preview plus editable id in the frontmatter panel; the new-unit template and `setField` write `icon`. |
| `pm-web-app/src/lib/views/PlanView.svelte` | Phase icon on the current-phase badge and on process-tree phase headers. |
| `pm-web-app/src/lib/views/OverviewView.svelte` | Phase icon on every plan and child-plan phase badge. |
| `pm-web-app/src/lib/views/EpicView.svelte` | Phase icon on the running-plan phase badge. |

### 2.4 Tests

| File | Coverage added |
|---|---|
| `pm-web-app/src/lib/icons/workflowIcons.test.ts` | Asset exists per id, no orphan or duplicate, catalogue set equality, every unit/phase mapping resolves, phase-unit and bare-token equivalence, unknown/empty fallback, and the full SVG lint (viewBox, currentColor, no forbidden element/reference/style, no hard-coded colour). |
| `pm-web-app/src/lib/components/WorkflowIcon.test.ts` | Decorative `aria-hidden` render, unknown-id fallback, named `role="img"` render. |
| `pm-web-app/src/lib/utils/frontmatter.test.ts` | Icon round-trip, unknown id stays the icon value rather than an extra, empty value stays empty, missing icon receives the default on edit. |
| `pm-web-app/src/lib/views/ProcessDesigner.test.ts` | Unit-list and header icons, frontmatter edit writes `icon`, unknown id and empty id both render the fallback. |
| `pm-web-app/src/lib/views/PlanView.test.ts` | `phase-5-execute` badge and process-tree header show `phase.execute`. |
| `pm-web-app/src/lib/views/OverviewView.test.ts` | Every rendered phase badge carries a phase icon. |
| `pm-web-app/src/lib/views/EpicView.test.ts` | The running-plan phase badge carries `phase.execute`. |
| `pm-api/.../designer/DesignerRecordsTest.java` | Icon round-trips through the JSON codec. |
| `pm-api/.../message/DesignerCatalogueTest.java` | `designer.frontmatter.icon` is present and non-blank in EN. |
| `pm-mock-runtime/.../DesignerScenarioTest.java` | The served finalize unit carries `phase.finalize`. |

## 3. Test & Verification Results

| Check | Result |
|---|---|
| `npm test` (`pm-web-app`) | **54 test files, 471 tests passed**. |
| `npm run check` (`pm-web-app`) | **0 errors, 0 warnings** (`svelte-check`). |
| `npm run lint` (`pm-web-app`) | **Clean**. |
| Prettier on all changed TS/Svelte files | **Written and clean**. |
| `./mvnw test -pl pm-api -Dtest=DesignerRecordsTest,DesignerCatalogueTest` | **Passed**. |
| `./mvnw test -pl pm-mock-runtime -am -Dtest=DesignerScenarioTest` | **Passed**. |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** — full reactor, OpenRewrite write mode, licence headers, Checkstyle/Enforcer, Javadoc, all unit tests, and the web build; no post-gate reformatting or untracked artifacts. |

## 4. Domain Invariants

| Invariant | Status | Evidence |
|---|---|---|
| The DSL format stays at 1 | **Holds** | The schema change landed in `pm-mcp-core` without a header change; this workstream adds no format token. |
| `icon` is required in the wire model | **Holds** | `WorkflowUnit.Frontmatter.icon`, `WorkflowFrontmatter.icon`, and the parser's `icon` field; `emptyFrontmatter` starts at `workflow.default`. |
| Every known unit and phase has a distinct icon | **Holds** | 40 delivered ids; catalogue/registry set equality; unit/phase map resolves every row; 40 distinct asset URLs. |
| Unknown ids fall back, empty ids are schema errors | **Holds** | Registry fallback tests, component fallback test, designer preview test; the parser keeps an empty value empty so the runtime refuses it. |
| Icons are decorative beside text | **Holds** | `WorkflowIcon` is `aria-hidden` unless labelled; all uses sit beside text; tests assert the attribute. |
| No third-party artwork or remote asset | **Holds** | Standalone original line art; SVG lint forbids `href`, `<image>`, `<use>`, `<style>`, `data:` and foreign elements; provenance table records the original-work approval. |
| The Git icon is not the Git logo | **Holds** | The drawing is a trunk/commit/branch metaphor, recorded in the visual spec and the catalogue. |
| No stub default units | **Holds** | The `plan-marshall-mcp` workflows directory is absent; Phase 5 is recorded as blocked. |
| Bilingual catalogue | **Holds** | `designer.frontmatter.icon` added to EN and DE; the catalogue test compares key sets. |
| Responsive layout | **Holds** | Icons are inline, size-bounded, and add no fixed-width layout; the designer's existing phone behaviour is unchanged. |

## 5. Cross-Repository Status

| Order | Repository | Scope | State |
|---|---|---|---|
| 1 | `pm-mcp-core` | `icon` field in the `pmwf` schema, reader, printer, docs, ADR, fixture migration | **Landed** — [PR #150](https://github.com/plan-marshall/pm-mcp-core/pull/150), merge `ac347032` |
| 2 | `pm-mcp-clients` | Visual specification, 40 icons, registry, component, `pm-api`/web model, designer and phase views, tests | **Implemented in this report's branch** |
| 3 | `plan-marshall-mcp` | `icon:` lines in the shipped default units and recipes, sibling drift check | **Blocked** — `pm-mcp-server/src/main/resources/workflows/*.pmwf` does not exist on `main`; no placeholder units were created |

### 5.1 Follow-Up Gate

The `default-units-and-drift-tests` package in `plan-marshall-mcp` must land first. When it does:

1. Re-open `/Users/oliver/git/worktrees/plan-marshall-mcp-workflow-default-icons` on `feat/workflow-default-icons` from the updated `main`.
2. Add `icon:` to every default `.pmwf` using the unit/phase map in the visual specification, and `recipe.default` (or a specific recipe icon) to every shipped recipe.
3. Add the sibling drift check: every default unit has an icon, every icon resolves in this registry (or in a checked-in icon-id manifest), and no unintended duplicate id exists.
4. Run that repository's own pre-commit and review cycle.

No stub `.pmwf` resource is part of this delivery.

## 6. Licence and Provenance Statement

All 40 icons are standalone original line art drawn for this project. No icon-font glyph, brand logo, stock icon, or remote asset is copied, traced, or referenced. The provenance table in the visual specification records the author, the absence of a source file, the project licence, the commercial-use conclusion, and the approval. A future blend must complete a provenance row before its SVG is committed; the Git icon remains our own branch-and-commit metaphor, not the Git logo.

## 7. Notes and Non-Goals

- The delivered set has **40** ids: the plan's 39 plus `unit.lesson`. `lessons` is a designable compiled unit in the process designer (see `process-designer.adoc`), and the acceptance criterion requires every designable unit to have a distinct icon, so the lesson family was added and catalogued rather than sharing `unit.source`.
- The optional `StartView` recipe-card surface was evaluated and left out: the recipe representation exposes no workflow frontmatter icon, and adding a redundant API field was out of scope. It is recorded here as the follow-up rather than invented.
- Task nodes, worker rows, plan titles, projects, and epics are deliberately not iconified; the visual language covers workflows and phases.
- Icons are data: adding one is an SVG, a registry entry, a catalogue row, and the unit's `icon:` line. No view edit is required.
