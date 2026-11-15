# Landing Analysis: PLAN-149 — Web foundation: the interaction set

epic: ui-improvements
workstream: WS-02
pr: #445

> Landing record for PLAN-149 (layer tokens, the one act menu, `SegmentedControl` and `Toggle`, `Button`/`IconButton`/`Dialog`/`ConfirmDialog` conformity,
> fixtures, and the D6 amendment: the `StatusLabel` list slot). Merged as `3a64b2b1` (46 files, +2592/−646; merge-queue merge of head `cff84d53`, after
> merging `da8c8644` and `6b97acd2`). Lane: runbook, sender `rb-149-web-foundation-interaction-set`; harness **`antigravity`, model `gemini-2.5-pro`**, effort
> `high`, `plugin_version` 0.1.0, executor `0.1.2133`, `total_tokens=0`. Written by the `analyze` verb on 2026-11-15 from inbox message
> `rb-149-web-foundation-interaction-set-002.md`, after verifying the claims against git, the provider and the merged tree. The landing record is complete (no
> missing key).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 layer tokens | shipped-modified | `layers.ts`, `tokens.css:67-72`, the order test and a scan that really walks `src/` (`.svelte`, `.css`) agree; the scan can fail and strips comments. A 17th allowlist file was needed (`SideColumn.svelte:75`, `z-index: 60`) and entered with the reason "the V parts" [R: `tokens.test.ts:60`]; `GapStrip.svelte` (popover now `--layer-bar`) is undeclared; allowlisted entries are never checked for staleness; the "detects" test copies the regex instead of calling the scanner; no browser stacking check of real components (the layers fixture is five static boxes). |
| D2 one act menu | shipped-modified | `RowMenu.svelte:147` and `ActionBar.svelte:112` both mount `ActMenu`; keyboard rules met (wrap, Home/End, Escape, Tab, outside click). Not met: a destructive entry in the ActionBar does not confirm (`confirmDestructive={false}`, `ActionBar.svelte:118`; the design says it does, `components.adoc#act-menu:191`); every rule stands twice in `ActMenu.svelte:247-370` (`.act-menu*` and legacy `.row-menu__*`, kept because `RowMenu.test.ts` greps the old class names); no e2e run, no keyboard walk cited. |
| D3 `SegmentedControl`, `Toggle` | shipped-modified | Roles, roving tabindex, arrows, 24/44 px met (`aria-pressed` on the `Toggle` button is per spec; the lane message says `aria-checked`). "A count outside 2..5 is refused by the type" is unsupportable in a Svelte prop type: a reactive `throw new Error` replaces it (`SegmentedControl.svelte:23-25`), which crashes the subtree for a data-driven list of length 1 or 6. `Toggle` has no disabled reason; no reduced-motion override. |
| D4 `Button`, `IconButton`, `Dialog`, `ConfirmDialog` conform | shipped-modified | `size="small"` and `visibleLabel` gone; `aria-disabled`, `title`, `aria-describedby` right; tokens only. **Not met:** activation is not blocked for `on:click` consumers (D-1 below); `loading` shows the spinner beside the icon; danger consequences are not in the danger colour (`Dialog.svelte:199-210` colours the title only). |
| D5 fixtures, browser evidence | shipped-modified | Dev fixtures and four screenshots exist (`149-*.png`); English literals in both locales (" (off)", " (on)", " (down)", " (fixed)"); the German five-option segmented case is not shown. |
| D6 `StatusLabel` slot | shipped-modified | `slot` renamed `fixed` (pre-1.0, dev page and `components-tags.adoc` updated). The spec said to stop and ask if the design names no width token; the design names none and the lane wrote the literal **`8.5rem`** [R: `StatusLabel.svelte:49`; `components-tags.adoc:102`] without asking or reporting it; no measured dot-x check. |

## Three-Layer Analysis

Read on three layers by one read-only leaf (`execution-context-level-3`); the orchestrator re-read on the merged tree the points marked [R]. **By reading, no build,
no test run, no write.** The two `on:click` findings are static reads and want one test.

- **Verdict:** layer 1 findings (3 major, 6 minor); layer 2 findings (1 major, ~14 minor/leads); layer 3 findings (1 major, 6 minor). All six deliverables are
  met only in part; no data loss; the major items are a behaviour hole in a shared component, an unrecorded merge override and an invented token.

### Layer 1 — Specified

- Structure: D6 and the `analyze-fix-build-time` finding stand after "Re-Grounding at Restart" and after the Hand-Off block; "five deliverables" is stated while D6 is a sixth,
  outside the list; D6's files (`StatusLabelTemplateFixture.svelte`, `vite.config.ts`) are not in the Expected Surface, which mixes HEADs (`1cc0e10e`, `dc811fb3`, `25137445`);
  D6 names the implementer persona and the lane used the integration tester.
- Stale counts: "19 `size="small"` call sites" is 20 (`SideColumn.svelte:35` came with PLAN-142); the allowlist "holds exactly" 16 files, 17 are needed; line refs
  `RowMenu:89-91,129`, `:249`, `ActionBar:274` are gone with the extraction; `components.adoc:163` still says IconButton tests live in `Button.test.ts`.
- No source: "the text guard baseline does not grow" — no scan or baseline file exists (PLAN-151 creates it), so nothing checked the literals above.
- Design against lane: `components.adoc#layers` is untouched and lists nothing for `SideColumn` (60) or the GapStrip popover while saying "no component writes a number of its own"; the design's "focus on its
  first entry" is the first *enabled* entry in the code.
- Footprint: undeclared `GapStrip`, `SideColumn`, `StatusLabelTemplateFixture`, `vite.config.ts`, `common.*`, `components-tags.adoc`, six consumer tests; declared and untouched
  `components.adoc`, `README.adoc` § `#control-tokens`, `row-menu.spec.js`, `SettingsView`, `SystemView`, `+layout`.

### Layer 2 — Implemented

- **D-1 (major, [R] static): a disabled-with-reason `Button` still fires `on:click` consumers.** `Button.svelte` registers `on:click={handleClick}` and a forwarding `on:click` on the
  same element; `handleClick` calls `stopPropagation()`, which does not stop a second listener on the same element, so only `onclick` rest-prop consumers are protected. `ActionBar.svelte:81,94`
  call `chooseAct` through `on:click`, so a primary or secondary act with a disabled reason can still reach `onAct`; `Button.test.ts:66,95` use `onclick` and never exercise the forwarded path;
  `IconButton.svelte:58-59` has the same shape and `DataTable` uses `on:click` there. The `WorkControlPanel` "sends nothing" test passes through an incidental guard.
- `ActMenu`: `ArrowUp` with no focused entry wraps to `n-2`; focus return needs `triggerElement`, which the exported `RowMenu.openMenu()` never sets; `ConfirmDialog` renders inside the
  menu DOM (focus restore ordering and a transformed-ancestor trap; it should leave the menu); the confirmation title and consequences are both the entry label; the reason line sits inside
  `opacity: 0.5`; `place()` (flip and cap) lives in `RowMenu` only; `.row-menu__item:hover` ignores `aria-disabled`.
- Tests: `ActMenu.test.ts` "focuses first item" never asserts focus; the "cap" and "upward" tests read back a prop and a class name; the Dialog token test asserts CSS substrings;
  `Toggle.test.ts` has 6 tests and `SegmentedControl.test.ts` 9 (the lane says 31 for `Toggle`); both re-implement `styleOf`/`rulesOf`; mutation evidence is one revert each for `ActMenu` and
  `Button`, where the spec asked for each line.
- Dev page: 8 new `common.*` keys are symmetric en/de; no duplicate ids; no `{@html}`, `innerHTML` or timer waits in the new files.

**Claims of the lane's report the tree does not bear out:**
- **"unresolved_review_threads=0" and "Residue: None":** `progress.md:61` ends with `operator-authorized: merge PR 445 at head 30f5c9f8 despite review-gate thread_concern_remains on CodeRabbit
  thread PRRT_kwDOVBOtss63qHpK (ActMenu.svelte:0; finding addressed in 30f5c9f8 and verified)` [R]. The message states a merge of the later head `cff84d53` and mentions no override; nothing on file here
  shows who authorized it. Open question for the operator.
- "Self-review round 1 clear, 0 findings": the tree shows duplicated CSS, a tautological allowlist test and the D-1 hole; CodeRabbit then raised 24 findings. The 24 count holds (the 42
  `finding:` lines are 24 items, rounds 1 and 3 counted twice); a 25th thread (`…qHpK`) appears only in the merge line.
- The review gate answers `review_body_findings_handled: 0` / `merge_allowed: false` / `body_findings_undisposed` for #445 (24 named), as for #443 and #444: three landings from two harnesses. Sonar `read_open_issues --pr 445`:
  `undecidable` (`head_revision_unavailable`; analysis revision `cff84d53`), so `sonar_open_new_issues=0` is the lane's word.
- `total_tokens=0` (not `unknown`) and an unrecorded model identity beyond the stamped fields: the effort metrics are not credible; guard `none`.

### Layer 3 — Correct

| Finding | Severity | Evidence |
|---------|----------|----------|
| L3-1 | major (for 150 and 88) | `Dialog.svelte:119-124` listens on the window in the capture phase and stops propagation: Escape inside a menu that lives in a dialog closes the dialog and never reaches the menu (the design closes the menu first). |
| L3-2 | minor | Layering: `CommandPalette` and `Dialog` share 100 (DOM order decides); `SideColumn`'s phone takeover (60) sits above the menu (30); both templates' `onKeydown` ignore `defaultPrevented`; whether the menu stacks above the bottom bar is not browser-checked for real components. |
| L3-3 | minor | `SegmentedControl`: `{...$$restProps}` after `role`/`aria-label` lets a caller override them; `white-space: nowrap` with no overflow handling (five German labels on a phone untested); arrow keys are not direction-aware; arrows select and fire `onChange` on every move. `Toggle` spreads `$$restProps` last (a caller can override `aria-pressed`, `disabled`). Both: `150ms ease` is hard-coded although `--duration-fast`/`--ease-standard` exist, no `prefers-reduced-motion`. |
| L3-4 | minor | `StatusLabel` `8.5rem` is invented; the longest German state words (`state.decision.costly`, `state.decision.irreversible`, 37-38 characters) wrap under `overflow-wrap: anywhere`; the template fixture wraps every label in a `<div>` and is only a compile smoke test; `StatusLabel.test.ts` pins CSS strings including `inline-size: 8.5rem`. |
| L3-5 | minor | `reports.adoc` and `components-tags.adoc` were edited (justified by the spec's D-4 and D6); `vite.config.ts` gained a global `testTimeout: 20_000`, a blunt policy where the finding asked for a named project or helper. |
| L3-6 | minor | No `aria-controls` on the menu triggers; focus ring contrast (3:1) has no measured assertion; `Button` keeps physical `min-height`/`width`; the `ConfirmDialog` is not portalled. |

## Leads handed to the follow-ups

- **PLAN-150** (shares `Button.svelte`, `ConfirmDialog.svelte`, `tokens.css`, `tokens.test.ts`, the dev page and `common.*`; its Expected Surface was grounded at `1cc0e10e`): any `Field` that uses
  `Button` with `on:click` inherits D-1; `SecretField` needs a `Toggle` with a disabled reason, which `Toggle` lacks; a typed-name change to `ConfirmDialog` keeps the `ActMenu` use (title and
  consequences are the entry label).
- **PLAN-151:** it will flag the dev page's English literals and the `dev.toggle.label` mismatch; it needs a policy for `dev.*` keys. **PLAN-152:** `common.*` gains 8 `dev.*` keys that may collide;
  `report.list.row_menu` is still used (`ReportsView.svelte:901`), so no key became dead.
- **PLAN-88:** `ProcessDesigner` is on the z-index allowlist; its `.intake-mode` → `SegmentedControl` move keeps options in 2..5, minds German label width and the throw; its hand-rolled dialogs →
  `Dialog` meet the capture-phase Escape.
- **PLAN-143/144/166:** `ReportsView.svelte`, the dev page and `components.adoc` overlap; `PLAN-144` touches `BottomBar`, now on `--layer-bar` with the GapStrip popover; `PLAN-166` owns `SideColumn`
  (z 60, allowlisted without a real reason); the Escape scope difference between `WizardPage` (window) and `TriagePage` (root) is untouched. **PLAN-153:** `SystemView` is on the allowlist and an `ActionBar` consumer.

## Routing

Routed to `epic.md` (Decisions, Open Defects, Watches); the fix items and the placement of the follow-up leads are the operator's decision (this session's question), recorded in the Decisions entry.
