# Implementation Plan: `interaction-titles-and-brand-mark` (Distinct Advisory Text and a Project-Owned Mark)

Two interaction surfaces currently contradict the app's own intent.

The hover advisory was introduced to explain an action, but its last-resort fallback is the visible label, so most buttons now show a tooltip that repeats the button. Only one control in the whole web app passes an explicit advisory, which is why the duplicate text is systemic rather than accidental. The convention this plan adopts is exact: an element's **label key** identifies it, and its advisory is a **separate key with a `.title` suffix** — `plan.path.jump` explains the visible label, `plan.path.jump.title` explains the action. A control with nothing better to say gets no tooltip at all.

The top-left brand is the `✦` character in two places in the header. It reads as a generic sparkle/Gemini-like glyph, not as this project. The project mascot is a 1.4 MB raster illustration and is far too large to use directly, so this plan draws a new, original, stylized vector mark that takes the mascot as inspiration only and becomes the app's own mark.

Both changes are front-end plus catalogue work in `pm-mcp-clients`. No server, API, or data-model change.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-ENT-1`](../../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1) (Enter and Connect), [`UC-PLN-1`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1) (Follow a Plan Down to Its Parts), [`UC-PLN-4`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (See What Runs), [`UC-PLN-6`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6) (Catch Up on What Happened) |
| **Requirements** | `PM-SEC-7` (both clients as dependable views) |
| **Advisory layer today** | [`advisoryTitle.ts`](../../../pm-web-app/src/lib/utils/advisoryTitle.ts) — `titleFor` falls back to `data-advisory`, `aria-label`, then `textContent` |
| **Shared button** | [`Button.svelte`](../../../pm-web-app/src/lib/components/Button.svelte) |
| **Header today** | [`+layout.svelte`](../../../pm-web-app/src/routes/+layout.svelte) — two `✦` spans (`brand-home`, `brand-inert`) |
| **Existing icon pattern** | [`WorkflowIcon.svelte`](../../../pm-web-app/src/lib/components/WorkflowIcon.svelte), [`workflowIcons.ts`](../../../pm-web-app/src/lib/icons/workflowIcons.ts) — delivered SVGs, CSS mask, `currentColor` |
| **Visual authority** | [`workflow-visual-language.adoc`](../../design/web-app/workflow-visual-language.adoc) (grid, provenance, accessibility rules this mark must also satisfy) |
| **Mascot (inspiration only)** | `/Users/oliver/git/plan-marshall-documentation/doc/resources/planmarshall.png` |
| **Existing tests** | [`layout.test.ts`](../../../pm-web-app/src/routes/layout.test.ts), [`catalogue.test.ts`](../../../pm-web-app/src/lib/i18n/catalogue.test.ts), [`chrome.spec.js`](../../../pm-web-e2e/tests/chrome.spec.js) |
| **Operational SOP** | [`runbook.md`](../runbook.md) |

### 1.1 Finding Analysis

| # | Finding | Root cause | Correction |
|---|---|---|---|
| **F1** | Title attributes on interaction elements show the same text as the button. | `titleFor` falls back to `aria-label`, then to `visibleLabelOf(node)`. Since only one view passes `data-advisory`, nearly every generated title is the label. | Remove the label/aria-label fallback. Only an explicit `data-advisory` (from a `.title` key) or an authored `title` produces a tooltip. No advisory means no `title`. |
| **F2** | The titles may not be separate keys. | They are not: no `.title` convention exists for the web controls. The operator catalogue already uses `.title` for its headings, but the web audit never adopted it. | Add `.title` keys next to their label keys, in English and German, and use `data-advisory={$t('<label-key>.title')}`. |
| **F3** | The top-left icon is a Gemini-like sparkle. | The header renders the literal character `✦` in both the linked and the inert brand. | Replace it with an original stylized vector mark, driven by `currentColor`, in both branches. |
| **F4** | The mascot cannot be used directly. | `planmarshall.png` is 1.4 MB and a full illustration. | Draw a 24 × 24 vector that abstracts the mascot's character; never embed, trace, or resample the PNG. |
| **F5** | A new mark must not become an accessibility or provenance problem. | The brand is decorative beside the visible `plan-marshall` text; the visual-language rules forbid text inside icons and require original artwork. | `aria-hidden="true"` with the visible brand text as the name; provenance recorded; no third-party logo or copied mascot. |

---

## 2. Scope & Objectives

### Part A: Advisory keys and behavior (`pm-web-app`, `pm-api`)

1. Change `advisoryTitle.ts`:
   - `titleFor` considers only an explicit `title` authored by the view and `data-advisory`; it never reads `aria-label` or `textContent`.
   - When neither is present, remove any generated `title`; do not add a new one.
   - Keep the `generatedTitles` WeakMap protection so a view-authored `title` is never overwritten and a changed `data-advisory` updates its tooltip.
   - Keep `installAdvisoryTitles` as the safety net for native buttons.
2. Adopt the naming convention everywhere:
   - visible/accessible label key `X` → advisory key `X.title`;
   - the advisory text must add information (consequence, scope, shortcut, or state), never restate the label;
   - if no useful sentence exists, omit the advisory.
3. Add `.title` keys for the control classes that need one:
   - **Plan view**: jump to current position, open designer, completion report, back, expand/collapse node, select part/task.
   - **Reports**: mark all reports read, report details (enabled and deleted-subject variants), load older, layout mode, filter clear.
   - **Decisions**: allow/deny notifications, dismiss notice, waiver submit, option selection, decision chat.
   - **Overview / epic**: open plan, open epic, ended-work pagination, layout/filter controls.
   - **Header**: brand home (migrate `web.frame.home.hint` → `web.frame.home.title`), settings, connection badge if it becomes interactive.
   - Icon-only controls and destructive/irreversible actions always carry a distinct advisory.
4. Rename the one existing informative `title` (the deleted-report explanation) to a `.title` key so the convention has no exceptions.
5. Add a deterministic check:
   - every `data-advisory={$t('…')}` argument ends in `.title`;
   - every referenced `.title` key exists in both locales;
   - for every `.title` key whose base key exists, the English values differ;
   - no web view passes a label key directly as `data-advisory`.
6. Document the convention in `doc/design/web-app/interaction-affordances.adoc`, with examples and the “no useful sentence → no tooltip” rule.

### Part B: Brand mark (`pm-web-app`, `pm-api` catalogue where needed)

7. Draw `pm-web-app/src/lib/icons/brand/plan-marshall-mark.svg`:
   - `24 × 24` grid, drawing inside `2..22`, one colour (`currentColor`), no `<text>`, no gradients, no embedded raster;
   - original line/plane art that abstracts the mascot: a **keystone of three ascending plan sheets** with a forward **baton/chevron** cut through it and one small **node** marking the runtime; readable at 20–24 px;
   - no third-party logo, no trace of `planmarshall.png`, no Gemini-like four-point sparkle.
8. Add `BrandMark.svelte`:
   - CSS-mask pattern like `WorkflowIcon`, `background-color: currentColor`, sizes `20` and `24`;
   - `aria-hidden="true"` by default; an optional `label` turns it into `role="img"`;
   - `data-testid="brand-mark"`.
9. Replace both `✦` spans in `+layout.svelte` with `<BrandMark size={24} />`; keep the `app-header__logo` box, the brand text, the linked/inert split, and the current click behavior.
10. Record the mark in `doc/design/web-app/brand-mark.adoc`: construction grid, meaning of the parts, provenance statement, light/dark contrast, and usage rules.
11. Optional but preferred: add `pm-web-app/static/brand-mark.svg` and a `<link rel="icon">` in `src/app.html` so the browser tab uses the same mark. If the adapter's static handling makes this risky, record it as a follow-up rather than blocking the header change.

### Non-Goals

- No change to the workflow icon catalogue, the 40 delivered unit icons, or the visual-language registry.
- No use, resize, or embedding of `planmarshall.png`.
- No new tooltip library, popover, or custom hover card: native `title` only.
- No change to button labels, actions, routes, or permissions.
- No redesign of the header layout beyond the mark.

---

## 3. Isolation & Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-titles-and-brand-mark` | `feat/titles-and-brand-mark` | `pm-web-app`, `pm-api` message catalogue, tests, design notes, report |

> **Pre-flight**: `git checkout main && git pull --rebase origin main` before creating the worktree.

**Sole-owner files**: `advisoryTitle.ts`, `Button.svelte` if its action wiring needs adjustment, `BrandMark.svelte`, the brand SVG, `+layout.svelte`, the affected view templates for `data-advisory`, `common/plan_view/reports/decisions/overview/epic_view.{en,de}.properties`, the new tests, `layout.test.ts`, `chrome.spec.js`, the two design notes, and the report.

**No touch**: `pm-api` records, `pm-mock-runtime` scenarios, `pm-mcp-server`, the plan-view selection logic, the reports/decisions layout and filter logic, and the workflow icon set.

> Coordination note: the plan-view and reports/decisions plans also touch their views. This plan changes only the advisory attributes and the header; keep the edits line-local and rebase on `main` before opening the PR if either sibling has landed.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-titles-and-brand-mark` on branch `feat/titles-and-brand-mark` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the advisory behavior and component;
  - `pm-dev-frontend:css` for the mask sizing, header alignment, and theme contrast;
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for the behavior and source-contract tests;
  - `plan-marshall-persona-security-expert` for the tooltip/text trust surface (no HTML injection);
  - `plan-marshall-persona-documenter` for the convention, provenance, and report;
  - `plan-marshall-persona-code-reviewer` for review triage.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Completion report written (`doc/agy/interaction-titles-and-brand-mark-plan-report.md`), plan moved to `doc/agy/implemented/`, links rebased, dashboard updated.
- [x] 5. Pre-commit passed: `./mvnw verify -Ppre-commit`.
- [x] 6. PR opened (never `skip-bot-review` on worker PRs); review windows and all bot/Sonar findings resolved.
- [ ] 7. Enqueued in the merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and the report moved to `doc/agy/verified/interaction-titles-and-brand-mark-plan-report.md` by the orchestrator.

> [!IMPORTANT]
> **Archived plan link rule**: this plan now lives at `doc/agy/implemented/`, and its links are already rebased to the archive depth (`../../design`, `../../../pm-web-app`, `../../../pm-api`, `../../../pm-web-e2e`, `../../../../plan-marshall-documentation`, and sibling `<name>.md`). If it is moved again, rebase every link and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Advisory behavior** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript` | Remove the label fallback, keep view-authored titles, update `Button`/observer behavior. |
| **Phase 2: Key convention & audit** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript` | `.title` keys, informative copy, EN/DE parity, no duplicate text. |
| **Phase 3: Brand mark** | `plan-marshall-persona-implementer` | `pm-dev-frontend:css`, `pm-documents:svg-diagrams` | Original 24 × 24 vector, mask component, header fit, theme contrast. |
| **Phase 4: Tests** | `plan-marshall-persona-module-tester` | `pm-dev-frontend:jest-testing` | Behavior, source-contract, SVG contract, header tests, e2e. |
| **Phase 5: Docs & review** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Convention note, provenance, report, finding triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Advisory Behavior (`pm-web-app`)

- [x] **1.1 Change `titleFor`**
  - Candidates: explicit `title` (already protected), then `data-advisory`. Stop there.
  - Remove `visibleLabelOf` from the candidate path (keep it only if a test needs it, otherwise delete the helper).
  - If neither candidate is non-empty, return `''`; `applyAdvisory` then removes a generated `title` and does not add one.
- [x] **1.2 Keep authored titles**
  - A view-authored `title` (current value ≠ the value in `generatedTitles`) is never overwritten.
  - A changed `data-advisory` updates the generated title, as today.
- [x] **1.3 Update `Button.svelte` only if needed**
  - It already spreads `data-advisory` through `$$restProps` and applies `use:advisory`; confirm the prop is not swallowed.
  - Native `<button>` and `<a>` controls rely on `installAdvisoryTitles`; the observer's attribute filter already includes `data-advisory` and `title`.
- [x] **1.4 Tests for the behavior**
  - a button with only visible text gets **no** `title`;
  - a button with `aria-label` and no advisory gets **no** `title`;
  - a button with `data-advisory` gets exactly that `title`;
  - a view-authored `title` survives a label change;
  - changing `data-advisory` updates the title;
  - removing `data-advisory` removes the generated title.

### Phase 2: Key Convention and Audit (`pm-api`, `pm-web-app`)

- [x] **2.1 Add `.title` keys next to their label keys**
  - Use the label key as the identifier; append `.title`.
  - Example pairs:
    - `plan.path.jump` / `plan.path.jump.title=Jump to the position that runs now and expand its path`;
    - `designer.plan.open` (already has `designer.plan.hint`) → rename the advisory to `designer.plan.open.title` and point the button at it;
    - `report.action.mark_all_read` / `report.action.mark_all_read.title=Mark every loaded report as read; older reports stay unread`;
    - `report.action.details` / `report.action.details.title=Open the report's detail, or the view it leads to`;
    - `report.action.details.deleted.title=The subject was deleted by cleanup on {date}; the report body stays readable`;
    - `decision.notify.allow` / `decision.notify.allow.title=Allow desktop notifications for decisions in this browser`;
    - `decision.notify.deny` / `decision.notify.deny.title=Keep decisions in the app without desktop notifications`;
    - `common.layout.split` / `common.layout.split.title=Show the list and the selected item side by side`;
    - `plan.view.toggle_node` / `plan.view.toggle_node.title=Expand or collapse this part without changing the narrative`.
  - All values in `common`, `plan_view`, `designer`, `reports`, `decisions`, `overview`, and `epic_view` English and German files.
- [x] **2.2 Audit every interaction element**
  - Use a source sweep for `<Button`, `<button`, `data-advisory`, and `title=` in `pm-web-app/src`.
  - For each control, choose one of: (a) no tooltip (label is sufficient), (b) `.title` key with new information.
  - Required categories: icon-only controls, destructive/irreversible actions, actions with a scope beyond the visible item (mark all read, load older), toggles/segmented controls, filter clear, pagination, tree toggle, back/navigation.
  - Remove any `title` whose text is identical to the visible label; do not leave a duplicate behind.
- [x] **2.3 Migrate the existing authored titles**
  - `web.frame.home.hint` → `web.frame.home.title`; apply it as `data-advisory`.
  - `report.detail.deleted_by_cleanup` → `report.action.details.deleted.title` (parameterized).
  - Any other `title=` on an interactive element: rename to `X.title` or delete if duplicate.
- [x] **2.4 Add the deterministic convention check**
  - New `advisoryTitle.convention.test.ts` (or a case in `catalogue.test.ts`):
    - scan `pm-web-app/src` for `data-advisory={$t('…')}`; every key ends in `.title`;
    - every referenced key exists in both catalogues;
    - for every `.title` key with a base key, `en[base] !== en[title]`;
    - fail with the offending file/key.
- [x] **2.5 Catalogue parity**
  - `./mvnw -pl pm-api test` (or the focused catalogue tests) proves both locales carry every key.

### Phase 3: Brand Mark (`pm-web-app`)

- [x] **3.1 Draw the mark**
  - File: `pm-web-app/src/lib/icons/brand/plan-marshall-mark.svg`.
  - Grid `24 × 24`, drawing `2..22`, `fill`/`stroke` = `currentColor` only, no `<text>`, no `<style>`, no metadata, no raster.
  - Construction: three offset plan sheets rising left-to-right (the plan stages), a diagonal baton/chevron crossing them (the marshal's direction), and one small dot/node at the leading edge (the runtime). The silhouette must remain legible at 20 px and must not resemble a four-point sparkle.
  - Original artwork; the mascot informs character (upright, forward-moving, plan-led), not geometry. Record in the provenance table that the PNG was not traced, embedded, or resampled.
- [x] **3.2 Add `BrandMark.svelte`**
  - `import markUrl from '../icons/brand/plan-marshall-mark.svg?url';`
  - CSS mask with `background-color: currentColor`, `inline-size`/`block-size` from a `size` prop (20 or 24), `aria-hidden={label ? undefined : 'true'}`, `role={label ? 'img' : undefined}`, `aria-label={label}`, `data-testid="brand-mark"`.
- [x] **3.3 Replace the header glyph**
  - In `+layout.svelte`, replace both `<span class="app-header__logo" aria-hidden="true">✦</span>` occurrences with `<BrandMark size={24} />`.
  - Keep the `app-header__logo` wrapper class for spacing and `color: var(--color-primary)`; adjust the CSS to `display: inline-flex` if the mask needs a box.
  - Keep `brand-home`'s `aria-label`, its `title` (now `data-advisory` / `.title`), and the inert branch unchanged.
- [x] **3.4 Optional favicon**
  - Add `pm-web-app/static/brand-mark.svg` (copy of the mark) and `<link rel="icon" type="image/svg+xml" href="%sveltekit.assets%/brand-mark.svg" />` in `src/app.html`.
  - Verify the static adapter copies it; if not, record it as a follow-up.
- [x] **3.5 Contrast and sizing**
  - Light and dark theme: `currentColor` follows `--color-primary`; no hard-coded fill.
  - 24 px in the header, aligned with the brand text baseline; no layout shift versus `✦`.

### Phase 4: Tests

- [x] **4.1 `advisoryTitle` tests** (Phase 1 list).
- [x] **4.2 Convention test** (Phase 2.4).
- [x] **4.3 `BrandMark.test.ts`**
  - renders `[data-testid="brand-mark"]` with `aria-hidden="true"` by default;
  - `label` gives `role="img"` and the accessible name;
  - the SVG file parses, has `viewBox="0 0 24 24"`, contains `currentColor`, and contains no `<text>`, `<image>`, `<style>`, or `data:image`;
  - source sweep: no `✦` remains in `pm-web-app/src`.
- [x] **4.4 `layout.test.ts`**
  - `brand-home` and `brand-inert` each contain one brand mark;
  - the header still shows the brand text and the click still calls `openStartPage`;
  - the brand link's advisory is the `.title` value, not the visible label.
- [x] **4.5 Playwright (`chrome.spec.js`, `entry.spec.js`)**
  - the mark is visible in the header at desktop and mobile widths;
  - clicking the brand still navigates home;
  - a representative button (plan jump, report details, or mark-all-read) has a `title` that differs from its visible text;
  - a button with no advisory has no `title`.
- [x] **4.6 Accessibility**
  - the mark is decorative beside the text and adds no focus stop;
  - no tooltip is the only name of a control;
  - the brand's accessible name remains the visible `plan-marshall` text plus the `aria-label`.

### Phase 5: Documentation, Verification, Report

- [x] **5.1 `doc/design/web-app/interaction-affordances.adoc`**
  - the `X` → `X.title` rule, the “add information, never restate” rule, and the “no useful sentence → no tooltip” rule;
  - the audit categories and the convention test as the enforcement.
- [x] **5.2 `doc/design/web-app/brand-mark.adoc`**
  - grid, construction, meaning, provenance (original, mascot not traced), sizing, theme, accessibility, and the favicon decision.
- [x] **5.3 Verification**
  - `cd pm-web-app && npm test -- advisoryTitle BrandMark layout catalogue`;
  - `npm run check`, `npm run lint`;
  - `./mvnw -pl pm-api test` for catalogue parity;
  - the Playwright chrome/entry specs;
  - `./mvnw verify -Ppre-commit`.
- [x] **5.4 Report** `doc/agy/interaction-titles-and-brand-mark-plan-report.md`: the before/after of a duplicate title and a distinct one, the audit table (control → key → decision), the brand mark at 20/24 px in both themes, the provenance statement, and the exact commands/results.
- [x] **5.5 Archive**: move the plan to `doc/agy/implemented/`, rebase links, update the dashboard, keep the report for Step 8.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | No generated `title` is ever the visible label or `aria-label`; a control without an advisory has no `title`. | Behavior tests for label-only, aria-label-only, explicit, and removal cases. |
| **F2** | Every advisory uses a separate `.title` key named after the element's label key; EN and DE both carry it; the text adds information and differs from the label. | Convention test and catalogue parity. |
| **F2** | The audit covers plan, reports, decisions, overview/epic, header, filter/layout, and pagination controls; no duplicate tooltip remains. | Audit table in the report and source sweep. |
| **F3** | The header renders the new original mark in both the linked and inert brand; no `✦` remains. | `BrandMark`/layout tests and source sweep. |
| **F4** | The mark is a 24 × 24 original vector, `currentColor`, no text/raster/third-party logo, and does not trace the mascot. | SVG contract test and provenance note. |
| **F5** | The mark is decorative (`aria-hidden`), adds no focus stop, and the brand keeps its accessible name and click behavior. | Accessibility test and Playwright. |
| **—** | Light and dark themes render with sufficient contrast; the header does not shift. | Screenshots in the report and a layout assertion. |
| **—** | `npm run check`, `npm run lint`, focused tests, `pm-api` catalogue tests, Playwright, and `./mvnw verify -Ppre-commit` pass. | CI / report. |

---

## 8. Risk & Rollback Notes

- **Loss of tooltips**: removing the label fallback means controls without a `.title` lose their hover text. That is intended; the audit adds titles only where they say something new. Review the audit table before merging to catch a control that genuinely needed one.
- **Explicit `title` handling**: the WeakMap logic must keep treating a view-authored title as authoritative. A regression here freezes the first label or overwrites an intentional explanation.
- **Catalogue drift**: every `.title` key needs both locales. The convention test and `pm-api` parity test are the guard.
- **Brand originality**: do not derive the vector by tracing or auto-vectorizing the PNG. The provenance note states the geometry was drawn from the brief; if any doubt remains, replace the contested part rather than documenting a blend.
- **Header regressions**: the mark uses `currentColor`; a hard-coded fill would break dark mode. Check both themes and both brand branches.
- **Parallel work**: the tree and reports/decisions plans edit the same view files for selection and layout. Keep this plan's edits to advisory attributes and the header; rebase on `main` before the PR and resolve only line-local conflicts.
- **Rollback**: restore the `✦` spans and the `titleFor` fallbacks; the catalogue keys can stay unused or be removed with the advisory attributes.
