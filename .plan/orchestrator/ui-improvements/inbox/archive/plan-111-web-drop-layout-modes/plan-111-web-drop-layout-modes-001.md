envelope_version=1
sender_type=plan
sender_id=plan-111-web-drop-layout-modes
epic=ui-improvements
kind=landing
created=2026-11-07T09:32:23Z
revision=1
amended=2026-11-07T14:04:39Z

PLAN-111 (drop the layout modes): PR #363 merged on the operator's word as bb30d71e (2026-11-07T14:03:54Z). Every Done-when clause is met except the browser check, which stayed blocked: the session's browser tools never saw a browser, even after the operator ran /chrome twice. The operator chose to merge without it.

```landing-facts
schema=landing-facts/1
plan_id=plan-111-web-drop-layout-modes
epic=ui-improvements
pr=#363
merge_state=merged
cleanup_owed=n/a
deliverables_total=1
deliverables_done=0
total_tokens=unknown
steps=tests-first:done,implement:done,npm-quality:done,e2e-decisions-spec:done,maven-verify:done,pr:done,review-comments:done,sonar:done,browser-check:blocked
harness=claude
model=claude-opus-5-5
effort=unknown
plugin_version=0.1.2036
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

## What shipped (PR #363, head 69d4e91c, merged as bb30d71e)

- Deleted `ViewLayoutToggle.svelte`, `stores/viewLayout.ts` and `viewLayout.test.ts`.
- `DecisionView.svelte` is one column: no toggle or header row wrapper, no split or compact branch, no split placeholder, and no `.decisions-split*` or `.layout-compact` CSS.
- **Palette deep-link reworked:**
  - A match clears a filter that hides the decision, scrolls to its card and marks it (`aria-current="true"` and a primary left border).
  - A filter that hides the marked decision drops the mark.
  - The request stays pending while a reload is in flight. This came from a CodeRabbit finding, fixed test-first in `793356e1`.
- Removed `common.layout.*` (seven keys) and `decisions.split.select` from both `de` and `en`, so catalogue parity is kept.
- `DecisionView.test.ts`: the four layout tests are removed. The palette tests are rewritten for the single column, plus a test for the one-column header.
  - The test-first clause test is `scrolls to and marks the decision in the single column`. It runs with no storage set and asserts `localStorage` is empty before and after. It was red before the change.
- `pm-web-e2e/tests/decisions.spec.js`: the compact step is replaced by a check that the header has no buttons and the card is in the single list.

## Done-when

- The grep finds nothing in `pm-web-app`, `pm-web-e2e` or `pm-api`: met.
- Component test for the deep-link with no layout storage, written test-first: met.
- `npm run quality` passes (1322 tests), and the decisions e2e spec passes 10/10 on ports 21100/21101: met.
- `./mvnw -pl pm-web-app verify`, `-pl pm-web-e2e verify` and `-pl pm-api verify` all pass. CI on head `69d4e91c` is green.
- Sonar at the head: 0 open issues, quality gate OK. The two S5906 smells in the new test were fixed in `69d4e91c`, not dismissed.
- Browser check: **NOT met (blocked).** Claude in Chrome was not connected for the whole session (`list_connected_browsers` returned an empty list each time), so no baseline or after screenshots exist. Per the spec this was not replaced by tests.

## Residue

- **Port block mismatch.** The spec names mock port 21100 and app port 21101. AGENTS.md § Ports computes `20000 + 10 × 111` = 21110/21111 for PLAN-111; 21100/21101 is PLAN-110's block by that formula. I used the spec's ports, which were free.
- **REST search counts fixed issues.** After the fix, `sonar_rest search --pr 363` still listed both fixed S5906 issues with line `0`. The SonarCloud check run and the Sonar MCP search both report 0 open issues, so `search` appears not to filter by issue status.
- **No independent review.** Sourcery skipped the PR (weekly review budget used up). CodeRabbit reviewed both heads: one finding, fixed, and the thread is resolved.
- **Pre-existing formatting drift** (not touched): prettier drift in `DecisionView.svelte`/`.test.ts` and in `pm-web-e2e` `port-guard.spec.js` and `theme-readability.spec.js`.
