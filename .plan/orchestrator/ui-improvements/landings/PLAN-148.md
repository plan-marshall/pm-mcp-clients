# Landing Analysis: PLAN-148 — Web Foundation: Tags, RowLink and the Tag Rules

epic: ui-improvements
workstream: WS-08
pr: #427 (https://github.com/plan-marshall/pm-mcp-clients/pull/427), squash `25137445` (34 files, +1602/-79), head `8f926e04`

> Written by the `analyze` verb on 2026-11-14 from `rb-148-web-foundation-tags-and-row-link-001.md`. Verified by reading against git,
> GitHub GraphQL and the SonarCloud public API. Runbook lane: opencode `deepseek-v4.1-flash/high`, executor `0.1.2106`→`0.1.2118`.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence (at `25137445`) |
|---|---|---|
| D1 ReasonBadge | shipped | 13 reasons (`ReasonBadge.svelte:7-37`), a tone map (`:57-59`), a fallback (`:99-105`), keys in en/de. The `test-first` contrast pair already existed in `TEXT_PAIRS` (`tokens.test.ts:200,202`), so its red was vacuous. |
| D2 PhaseLabel, Marker | shipped | `PhaseLabel.svelte:7-45`, `Marker.svelte:7-29`. Keys: `phase.name.*` ×6, `role.name.*` ×6, `common.marker.*` ×3. An unknown role is dropped silently (`:90`). |
| D3 RowLink (+ PLAN-147 residue) | shipped | One anchor; controls outside it; `isAppPath` reused; `AUTHORITY_FORM` shared; the `ExternalLink` forms and C0 cases (`ExternalLink.test.ts:95-117`); `WEB_SCHEMES` removed. Minors m1–m3 (a11y and stacking). |
| D4 Rules | shipped; one evidence-order gap; slot defect | `CountBadge`, `Chip`, and `--color-success-subtle` (`#E9F6EE`/`#15271D`). Contrast recomputed: success on the block is 4.78 light / 5.95 dark. **M1:** the `StatusLabel` prop is named `slot` (reserved in Svelte). **M2:** the list slot has no width. The token Done-when was `mutation-after`, not `test-first` (accepted by the operator). |
| D5 Fixtures, browser | shipped | Dev page `+page.svelte:515-578`; 8 + 8 screenshots (not viewed by the orchestrator). |
| D6 e2e fixme (amendment) | shipped, then superseded | The fixme naming PLAN-161 was removed by #429; no fixme is left. |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`). Verdict: no blocker.
- **Layer 1:** D-2 was carried as "assumed: the design follows the code" (`integrity_pause`) and was never asked. It reversed an open work package (M3).
- **Layer 3, majors:**
  - M1: `StatusLabel.svelte:13` `slot` gives `slot_attribute_invalid` in the snippet form the adopters will write.
  - M2: `:47-57`: the inline-grid shrink-wraps, so the dots do not align. The tests pin CSS strings only.
  - M3: the D-2 direction.
- **Minors:**
  - m1: `aria-current` sits on the wrapper div.
  - m2: `:focus-within` rings the row on a mouse click and the title is ringed twice.
  - m3: the `kind` slot and other controls in `tags` are covered by the stretched link.
  - m4: the RowLink tests are mostly CSS-text pins.
  - m5: the D1 red was vacuous.
  - m6: the enum-guard regex skips digit constants.
  - m7: only 5 of 13 success-subtle places were seen in a browser.
  - m8: the role set is open.
  - m9: the advisory convention differs between Marker and Chip.
  - m10: the theme document says ≥3:1 while the test enforces 4.5:1.
  - m11: an `ExternalLink` comment.
- **Process:** the main checkout was edited once in error and restored (self-reported; guard `none`).

## Routing and Merge Behavior

- **Review:** CodeRabbit reviewed `0204d229` and its 8 threads are replied to and resolved: 4 fixed in `ee17e45e`, 1 partly declined with a guard, 3 declined because the expected value of a contract stays literal. The declines are right. One reply wrongly says no test parses `.adoc`; `SpecificationTables` came with #426.
- **Review body:** no body-only items. "8/8" counts the inline threads.
- **Sourcery:** refused on quota (optional).
- **Sonar:** 0 open issues and 0 hotspots; quality gate OK. 3 × `typescript:S7780` were fixed.
- **Merge:** merged through the queue after about 1.5 h of non-admission (auto-merge armed; the queue only took the PR after a new head).

## Reconciliation Actions (operator, 2026-11-14)

- [x] M1 and M2 → **PLAN-149 D6** (rename the prop, give the slot a real width, add a template fixture and a measured browser check). Both must land before PLAN-49.
- [x] M3 → **Decision:** `integrity_hold` (the work package stands). The package must also flip `needs-and-find.adoc` and `components-tags.adoc` back and update `ReasonBadge` and its test. That is a repository doc edit outside the ledger.
- [x] RowLink a11y and stacking (m1–m3), plus the second detail line → PLAN-49 (first adopter). The disclosure button → PLAN-62. The role set (m8) → PLAN-51/52.
- [x] m7 → the browser checks of PLAN-153/154/155. The Marker/Chip convention → PLAN-150. Chip truncation → PLAN-157. The catalogue inventory → PLAN-152. Tokens (success-subtle delivered; m10) → PLAN-72.
- [x] Stale "PLAN-148 marks the fixme" premise in PLAN-149–157 rewritten.
- [x] Lessons `2026-11-14-10-004` (reserved prop names), `-005` (layout by measurement), `-006` (assumed decisions); evidence added to `2026-11-13-14-010`.
- [x] Row `shipped`, pr 427, landing; message archived.
