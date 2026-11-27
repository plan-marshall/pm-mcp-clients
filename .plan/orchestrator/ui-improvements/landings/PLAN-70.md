# PLAN-70 landing record

epic: ui-improvements
plan: rb-70-web-light-theme-pass (runbook lane, harness `antigravity`, so not the "opencode" the spec names; model and effort `unknown`; `total_tokens` `unknown`; `plugin_version=0.1.0`, false against the installed 0.1.2183; executor 0.1.2183)
pr: #601, squash `693d0597` (13 files) on `51b979ad`, merged 2026-11-27T22:28:10Z (merge group run 46075233726). Head `74073a86`.
analysed: 2026-11-27, by sub-agent in three layers, by reading only (no build). The copied-baseline claim was re-checked by the orchestrator: the md5 of `70-web-light-theme-pass-overview-de-light-before.png` equals that of `…-overview-before.png`.

Message `rb-70-web-light-theme-pass-001` (2026-11-27T22:29:54Z):
- `merge_state=merged`, `deliverables_done=2` of 2.
- 0 unresolved threads; 4 of 4 review-body findings handled.
- `sonar_open_new_issues=0`, `self-review: clear`.

**Sonar.**
- PR 601: 0 new issues, 0 hotspots, gate OK. Coverage on new code shows 0.0 %, because the change is TS/CSS only.
- Default branch, read 2026-11-27 after the `main` task `AaIfL-XI0fDYkpJpHkzq` (22:46:02Z, after this merge): gate OK, coverage on new code 90.3 %, `count_status: confirmed`, **3 open**. They are the three PLAN-181 keys.

## Layer 1: specified (the Re-Grounding of 2026-11-26 binds)

| Item | Verdict |
|---|---|
| D1(a) real brand pair instead of the literals, with a mutation run | met. `theme.test.ts:227-241` reads `tokens.css`. The mutation log `maven-2026-11-27-115918.log` shows the failure. Only the light brand was mutated, so the dark half has no mutation proof |
| D1(c) `--control-border` measured against 3:1 first, test-first, then retuned in both themes | met. Test `tokens.test.ts:598-620`, red on the old tokens (log `114850`), green after (log `115357`, 3485 tests). Before: light 1.57/1.46/1.37, dark 1.74/1.91/1.56. After (`#888880` light, `#65718B` dark): light 3.57/3.33/3.13, dark 3.53/3.86/3.25/3.16 |
| Token change only on evidence | met. 4 lines in `tokens.css` (`:90,170,211,255`); `COLOUR_TOKENS` updated; no surface or shadow token touched |
| Dark theme | intended change: `--color-border-strong` `#3a4356`→`#65718b`, contrast only rises. Side effect: everything else on that token gets a much heavier border (secondary `Button`, `Link`, the `DataTable` head, `Compose`, `NavRail`, `BottomBar`, `CommandPalette`, `OnboardingView`). Only one dark screenshot was taken (overview) |
| D1(b) light review of every rail route plus `/`, with screenshots | **partly met**. 54 shots, and the baselines are real except one. Faults:<br>• the after-shots predate `e42c360d` (19:57Z, `WorkControlPanel.svelte:1291`);<br>• the "DE" baseline is a byte copy of the EN baseline;<br>• `project-onboarding` has no before-shot;<br>• the de/en × dark/light × phone matrix exists for overview only;<br>• the Design Review visuals of 2026-11-06 (pairing-page mascot, empty states, kind tints) were not reviewed |
| `theme-readability.spec.js` green in light | met by equivalence: 192/192 at `e21bd24c`, which renders the same as the merge head. But the `pm-web-e2e` module build fails at every head on `designer.spec.js:264`, a pre-existing red from PLAN-178 #557 (`e22b302d`). Disclosed by the run |
| D2: the four doc fixes and the tuned values recorded | met (`theme-readability.adoc:20,75,206,213,72`) |

**True count:** 2 of 2 deliverables. D1(b)'s evidence is weaker than the message claims.

## Layer 2: implemented

13 files:
- 4 in the Expected Surface: `tokens.css`, `tokens.test.ts`, `theme.test.ts`, `theme-readability.adoc`.
- 9 outside it, explained by the message: bespoke inputs that bordered with `--color-border` (1.13-1.29:1) now use `--control-border`. The files: `FilterInput.svelte` and its test, `BlockPalette`, `CredentialEntry`, `WorkControlPanel:1291`, `ProcessDesigner`, `ProjectExplorerView:1819,1992`, `ProjectOnboardingView`, `SettingsView`.
- All SHAs exist.

**Stale or false claims:**
1. `plugin_version=0.1.0`: it is 0.1.2183. Every antigravity run reports this placeholder.
2. "18 views across EN/DE": the DE baseline is a copy of the EN one.
3. "3.57:1" is given for both themes; in dark it is 3.53.
4. "`theme-readability.spec.js` passed" is true, but the e2e module build failed.
5. The mutation job id has 31 hex characters (a typo).

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. Clean:
- The WCAG maths (`colourMetrics.ts`: relative luminance; an alpha value fails loudly).
- 3:1 for non-text and 4.5:1 for text.
- No hex outside `tokens.css`, no `package.json` change, no `{@html}`, no timer wait.
- The scanner reads `.svelte` only, never `doc/`, so it is compliant with PLAN-198.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | The Svelte scanner is a regex and char-walk parser. False negatives:<br>• `class={expr}` and a quote inside a class are not parsed;<br>• `data-class=` matches;<br>• `src/routes` and `base.css` are not scanned;<br>• logical border sides are missed;<br>• `:focus`/`:hover` are skipped;<br>• the floor `>= 14` is a magic number | `tokens.test.ts:976-1108` |
| L3-2 | minor | A fourth CSS rule reader, although the Re-Grounding asked to "reuse one reader". It duplicates `templateStyle.ts` | `tokens.test.ts:993-1017` |
| L3-3 | minor | Re-reads `tokens.css` in the per-theme loop, and duplicates the `TEXT_PAIRS` brand pair | `theme.test.ts:227-231` |
| L3-4 | minor | The screenshot evidence of D1(b), see above; the third plan with stale shots after PLAN-55 and PLAN-196 | `.plan/temp/playwright-mcp/70-*` |
| L3-5 | info | `FilterInput.test.ts` asserts source text, redundant with the scanner | `FilterInput.test.ts` |
| L3-6 | info | A stale doc comment ("two tuned brand values") | `tokens.test.ts:61` |
| L3-7 | info | The heavier `--color-border-strong` on secondary buttons, the table head, `Compose`, the rail and the palette needs a deliberate design look, in dark especially | `tokens.css:255` |

## Residue

- **Red on main, not PLAN-70's:** `pm-web-e2e/tests/designer.spec.js:264` (`:272`) expects "Then in force: the compiled default." while `designer.remove.none` (`designer.en.properties:99`) says "No copy is in force afterwards." It came with PLAN-178 #557 (`e22b302d`). CI runs no Playwright, so it stays unseen.
- **Shared code files:**
  - PLAN-199 (launched): `ProjectExplorerView.svelte`, 2 CSS lines, trivial drift.
  - PLAN-135: only indirectly, since `Compose` and the dev page render the heavier border.
  - PLAN-62, PLAN-138, PLAN-198: none.
- The landing-fact hygiene of antigravity runs (`plugin_version=0.1.0`) is a harness or lane defect.
