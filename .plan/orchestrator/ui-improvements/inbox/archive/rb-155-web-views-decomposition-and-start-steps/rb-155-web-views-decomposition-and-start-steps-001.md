envelope_version=1
sender_type=plan
sender_id=rb-155-web-views-decomposition-and-start-steps
epic=ui-improvements
kind=landing
created=2026-11-22T14:14:42Z

# Landing: PLAN-155 (Web views: decomposition and start steps)

## Summary

Successfully completed staged plan `PLAN-155` on the runbook lane, migrating decomposition and start-step views to D1 design system vocabulary, adding per-site error code pins to mock runtime, capturing 32 browser evidence screenshots, and merging PR #531 to `main`.

## Deliverables & Commits

- **D1 (`f44024b9`)**: `EpicDecompositionView.svelte` on D1 vocabulary (button hierarchy with `agree-commit` primary, `Compose` for instructions, `StatusLabel` + icons for check states, `Dialog` replacing hand-drawn modal, leave layer allowlist).
- **D2 (`802ed745`)**: `DecompositionReviewView.svelte` on D1 vocabulary (Keep/Edit/Drop secondary, read-back with Accept primary, `Compose` and `TextArea`, `KeyValueList` for objective/deliverables/surface/dependencies/order, diff signs `−` and `+` without color tokens, 0 vocabulary-lint findings).
- **D3 (`fa2234e4`, `cf9fbfa3`)**: `EpicAdoptView.svelte` (Adopt primary, Start new secondary, ghost cancel/back, failure causes under `ErrorState`) and `RecipeReadyView.svelte` (parameters via `Field`, `TextField`, `SelectField`, primary button naming the act).
- **D4 (`34e40128`)**: Browser evidence: `decomposition.spec.js` (3/3 passed), `theme-readability.spec.js` (192/192 passed), 32 screenshots generated under `.plan/temp/rb-155-web-views-decomposition-and-start-steps/screenshots/` (4 views × 2 viewports [1440x900, 390x844] × 2 locales [en, de] × 2 themes [light, dark]).
- **D5 (`9762fc19`)**: `StartView.svelte` retry button with `btn--secondary`, per-site not-found code pins added and verified across `MockRuntimeEngineTest.java`, `DecompositionScenarioTest.java`, `ControlScenarioTest.java`, and `EnrolScenarioTest.java`, vocabulary baseline updated.
- **Review Polish (`c0a379c2`)**: Addressed all CodeRabbit review feedback: preserved objective change alongside instruction in `DecompositionReviewView`, supported all check failure error rules in `EpicDecompositionView`, guarded against Enter during active IME composition in `RecipeReadyView`, and normalized read-back colors to `var(--color-text-primary)`.

## Test Evidence & Discipline

- **D1 test (`order: test-first`)**: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` (9/9 passed, previously failed with missing instruct-input, expect(agree).toBeDisabled(), plan-token-rotation)
- **D2 test (`order: test-first`)**: `pm-web-app/src/lib/views/DecompositionReviewView.test.ts` (6/6 passed, previously failed with aria-disabled, missing KeyValueList, btn--ghost, role textbox, read-back format)
- **D3 test (`order: test-first`)**: `pm-web-app/src/lib/views/EpicAdoptView.test.ts`, `pm-web-app/src/lib/views/RecipeReadyView.test.ts` (4/4 passed, previously failed with btn--secondary on adoptBtn, missing error-state, parameter-target missing, ready-start-act missing)
- **D5 test (`order: mutation-after`)**: `pm-web-app/src/lib/views/StartView.test.ts` (29/29 passed), `pm-mock-runtime/src/test/java/de/planmarshall/mock/{ControlScenarioTest,DecompositionScenarioTest,EnrolScenarioTest,runtime/MockRuntimeEngineTest}.java` (141/141 passed)
- **D5 mutation**: `StartView.test.ts:282` mutated to `btn--primary` failed (received `btn btn--secondary`); `MockRuntimeEngineTest.java:810` mutated to `scope_not_found` failed (expected: `<scope_not_found>` but was: `<target_not_found>`)
- **D4 browser evidence**: 32 screenshots under `.plan/temp/rb-155-web-views-decomposition-and-start-steps/screenshots/`; `theme-readability.spec.js` (192/192 passed), `decomposition.spec.js` (3/3 passed)
- **Build Gate**: `build-gate: pass sha cf9fbfa3`
- **Self-Review**: `self-review: round 1: clear`
- **Review Gate**: `review-gate: pass pr 531` (all 3 CodeRabbit threads resolved with commit `c0a379c2`)

## Merge

- **PR**: [#531](https://github.com/plan-marshall/pm-mcp-clients/pull/531)
- **Merge Commit**: `9af8642576ddeaac357496fe7e6e48a2eb035fa9`
