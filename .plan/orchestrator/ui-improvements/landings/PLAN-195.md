# PLAN-195 landing record

epic: ui-improvements
plan: rb-195-web-contrast-tests-read-their-theme (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` and `plugin_version` `unknown`; executor 0.1.2183)
pr: #588, squash `51e3bcc9` (6 files) on `458f9e3c`, merge queue, merged 2026-11-26T01:58:05Z
analysed: 2026-11-26, by sub-agent in three layers, by reading only (no build); the file list and `theme.test.ts:223-239` re-read by the orchestrator at `origin/main` `9011e6d1`

Message `rb-195-web-contrast-tests-read-their-theme-001` (revision 0, 2026-11-26T01:59:33Z): `merge_state=merged`, `deliverables_done=2` of 2, 0 unresolved threads, 2 of 2 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`. One operator question in the run (CI red on `main`'s `ChatTextsCatalogueTest` after #585; selected "fix the catalogue in this PR").

**Merge corroborated:** `gh pr view 588` merged 01:58:05Z, merge commit `51e3bcc91e36016da2b50c48231128469cd15023`, parent `458f9e3c` (#582). Diff: 6 code files, no `doc/` file.

**Sonar.** PR: 0 open new issues at `fc84c95e`, gate OK (per the message). Default branch, read by the orchestrator 2026-11-26 after the `main` task `AaIWTII6yu25SKZIp0Bi` (submitted 05:20:42Z, SUCCESS, after all three merges of this round; the earlier `main` tasks 02:22:11Z and 00:39:53Z follow #588 and #582): gate OK (`new_coverage` 90.2 %), `count_status: confirmed`, **3 open** — the three PLAN-181 keys only (`AaFGfsYupdXNQ-N4ArY-`, `AaGRMDtGxhXuOwwhaS0K`, `AaEcTdY0MJNX7b6L9QAy`), none in a file this PR touched.

**SNAPSHOT deploy of `51e3bcc9`:** `unknown`.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 the e2e matrix audits the theme it names | met: `setTheme` writes `pm_theme` in the app's origin (`contrast.js:36-45`); `assertTheme` (`:21-24`, a hard `expect` on `dataset.theme`) runs before every measurement (`:295`); `auditPageContrast` is module-private (`:277`), so no unguarded path remains. `mutation-after`: the cited log `…/maven-2026-11-25-181544.log` holds 51 lines "the dark audit would measure the document's light theme" (the message says 52); passing run 471/1/75, the 1 being the named `designer.spec.js` red |
| D2 the unit tests read the real theme blocks | met: one reader (`lib/test/stylesheet.ts:45` `topLevelBlockOf`, `:112` `parseThemes`), the copies in `tokens.test.ts` and `Button.test.ts` gone; `RowMenu.test.ts:293-302` measures `--color-brand` on `--color-bg-subtle` (what `ActMenu.svelte:353-356` paints) in light, dark and system-dark; the palettes-differ guard `tokens.test.ts:355-361`. Logs and timestamps fit `test-first` (DW1) and `mutation-after` (DW2); DW3 vacuous (`tokens.css` not in the diff, no pair fails) |

True count by reading: 2 of 2. The extra check: `control.chat.line.decision_opened` (`control.{en,de}.properties:307`), the design row (`work-actions-chat-texts.adoc:336-338`) and `ChatRegistry.blocksOf` (`:1227`) agree on `origin/main`; tested by `ChatTextsCatalogueTest.controlChatKey`, the new `decisionOpenedNamesTheBlocker` and `ChatContextNarrativeTest:142,159-161`.

## Layer 2: implemented

All 6 files trace: `contrast.js` → D1; the four `pm-web-app` files → D2; `ChatTextsCatalogueTest.java` (+16, outside the Expected Surface) → the operator's selection. `theme-readability.spec.js` (in the surface) untouched, with a true reason. **Stale claims:** the PR body and the message (§ Personas, § Operator decision) present the `control.{en,de}.properties` alignment and the `pm-mock-runtime` producer fix as this PR's change; the squash diff holds neither — #582 (`458f9e3c`) landed both, and the branch's `8a69f3ec`/`1aec310c` resolved to main's content in the merges. "52 occurrences" is 51.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. Clean: no timer wait added, no `innerHTML`, no dependency; the Java test uses `Generators` with `@EnableGeneratorController`, literals are contract values.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | a contrast unit test still measures invented RGB literals, not the theme (dark primary `rgb(96,165,250)` on `rgb(19,27,46)`; `tokens.css` brand `#8b7ef9`, surface `#161b22`) — the same bug class as this plan, pre-existing, outside its surface; residue for PLAN-70 | `pm-web-app/src/lib/views/theme.test.ts:223-239` |
| L3-2 | minor | `blocksOf`'s `decision.blocks` branch — the shape `MockRuntimeEngine.addDecisionAndBroadcast` broadcasts — is untested; only `plan` and `no` are rendered by a test (#582's surface; PLAN-197 changes the fallback) | `ChatRegistry.java:1229-1231`, `ChatContextNarrativeTest:142` |
| L3-3 | info | `parseThemes` takes the first occurrence of the light/dark/system-dark blocks and `topLevelBlockOf` matches `:root` by prefix; correct for today's `tokens.css` | `stylesheet.ts:117-120` |
| L3-4 | info | byte-identical `blockOf` copies remain (component styles, disclosed in the PR) | `WaitingMark.test.ts:38`, `BrandMark.test.ts:33` |
| L3-5 | info | `Button`'s variant contrast skips the system-dark block (pinned to dark by `tokens.test.ts:352`) | `Button.test.ts:280-289` |

Dropped by the sub-agent as unconfirmed: whether the `projects` and `theme-readability` reds the spec named turned green.

## Residue for the plans that touch the same surface

Shared code files with this merge: PLAN-70 (parked; its D-items on the dark contrast audit now rest on `assertTheme` and the one stylesheet reader; L3-1 belongs there), PLAN-85 (none in code). The emit gate for PLAN-70 ("after PLAN-195") is open; PLAN-70 is re-read against `51e3bcc9` before its emit.
