# Landing Analysis: PLAN-147 — Web Foundation: KindMark, Link, ExternalLink, PrChip, Compose, FormStatus (D1 Foundation, Part 1)

epic: ui-improvements
workstream: WS-01
pr: #420 (https://github.com/plan-marshall/pm-mcp-clients/pull/420), squash `dea405d8` (parent `37f51af7`), merge head `c4e592c1`

> Written by the `analyze` verb (2026-11-12) from `plan-147-web-foundation-kind-links-008.md` and candidate lessons 001-007, verified
> against git, the PR threads and reviews, the design and the SonarCloud API. `/plan-marshall`, claude `sonnet-5-5`; executor `0.1.2106`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Tints, icons, keys | shipped-modified | six `--color-kind-*` in four theme blocks (`tokens.css:89-91,167-169,205-207,242-244`), four rules in `tokens.test.ts` both themes, `colourMetrics.ts`; five icons (`uiIcons.ts:91-99`), `kind-plan` redrawn; only two keys added: the seven `common.kind.*` already existed (spec claim false) |
| D2 `KindMark` | shipped-as-specified | icon-only tint, word in surrounding colour, `sr-only` word (`KindMark.svelte:67,82-96`) |
| D3 `Link`, `ExternalLink`, `PrChip` | shipped-as-specified (+ guard) | `isAppPath` refuses authority forms (`Link.svelte:13,23-33`); `ExternalLink` syntax-first (`:11-21`); 17-row table `Link.test.ts:75-117`; `PrChip` never `href="#"` |
| D4 `Compose`, `FormStatus` | shipped-as-specified | IME (keyCode 229), held Enter, finally-focus, describedby reason; live region always present |
| D5 Fixtures and evidence | shipped with evidence gap | fixtures on `/dev/components`; screenshots taken 09:17Z before `3988e4d1` (24/44 px targets) and lost with the worktree; Chrome not Playwright, other viewports (disclosed) |

Added-unplanned: `colourMetrics.ts` + test, `README.adoc`, `components-fields.adoc`, `theme-readability.adoc`, `components.adoc` § `#link`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`); no write. Orchestrator re-read `Compose.svelte:64-78,196`,
  `Link.svelte` (`isAppPath` exported) and PLAN-148 D5 at `dea405d8`. Confirmed.
- **Verdict:** no blocker. Layer 1 — D5 put post-push facts into a Done-when (ours; epic rule (4) existed but the spec predates it) → five
  operator overrides; "no `common.kind.*`" false; `Link`'s non-app `href` unspecified; `test-first` for tints unattainable by order.
  Layer 2 — 8/8; `mutation-after` hex not quoted in the PR body (decision log only). Layer 3 — **major (evidence):** no browser check of the
  merged state. **Minor:** `Compose` `label=""` without `ariaLabel` → unnamed field; empty-value `onsend` contract untested; four URL
  forms missing from the `ExternalLink` table (guard correct); dead `WEB_SCHEMES`; PR body "14 fixed in 3988e4d1" (13 + 1 in `c4e592c1`).
- Confirmed sound: both URL guards (no bypass found); 14/14 threads replied and resolved, review bodies answered (issue comment
  6831706588); Sonar `0` (API: issues 0, hotspots 0, gate OK at `c4e592c1`); trailer, no footer.

Routing (operator, 2026-11-12, AskUserQuestion):
- Screenshots → **PLAN-148** D5 (re-check PLAN-147/131 fixtures at 390×844, 44 px targets, console from load).
- "Reword all open specs": post-push Done-whens removed from 47 staged specs (Sonar read → Standing Constraint; screenshots not a gate).
- Minors split: `Compose` items → **PLAN-135** D4; `ExternalLink` table, dead `WEB_SCHEMES`, `RowLink` reuses `isAppPath` → **PLAN-148** D3.
- Lessons: all 7 adopted, filed in plan-marshall's store (`2026-11-12-19-001`…`-007`); Open Defects.

## Routing and Merge Behavior

- Review: Sourcery 5 + CodeRabbit 9 threads, all replied and resolved (13 replies stock text); CodeRabbit refused re-review at the final
  heads for quota, its earlier review on record; one barrier loop-back on CodeRabbit's edited summary comment.
- Three loop-back cycles after the first push (14 findings; `https:host/path`; `//app.invalid/settings`).
- Merged by the run via the queue; squash message = PR body incl. CodeRabbit summary (repository default under the merge queue).
- Sonar `0` confirmed.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 420; `landing` `landings/PLAN-147.md`
- [x] PLAN-148 D3/D5 and PLAN-135 D4 amended (operator)
- [x] staged specs reworded (rule (4))
- [x] lessons filed (plan-marshall store); Open Defects; messages 001-008 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-131 (#422) rebases onto `dea405d8`: `+page.svelte`/`+page.test.ts` conflict likely (keep both blocks).
- PLAN-148 is next in the web chain; its re-grounding covers the moved `tokens.css`/`tokens.test.ts` anchors.
