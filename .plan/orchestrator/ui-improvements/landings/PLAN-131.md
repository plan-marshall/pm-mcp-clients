# Landing Analysis: PLAN-131 — Web Mascot Figure, Its Surfaces and the Unreachable Page

epic: ui-improvements
workstream: WS-01
pr: #422 (https://github.com/plan-marshall/pm-mcp-clients/pull/422), squash `6e8c29af` (parent `56e1ad37`, 51 files, +3428/-274), merge head `f3b9a79f`

> Written by the `analyze` verb (2026-11-13) from `plan-131-web-mascot-figure-surfaces-006.md` and candidate lessons 001-005, verified
> against git, GitHub's thread state, the review bodies, the design and the SonarCloud API. `/plan-marshall`, claude `sonnet-5-5`, effort medium; executor `0.1.2106`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Derived images | shipped (test gap) | four WebP files < 60 KB in `lib/assets/mascot/`, `?url`, `assetsInlineLimit: 0` (`vite.config.ts:124`, `csp.test.ts:92`); no WebP-signature or `data:` scan test (`MascotFigure.test.ts:68-73` checks size only) |
| D2 `MascotFigure` | shipped-as-specified | six poses × two crops, `srcset`, `alt=""` + `aria-hidden` (`:88-89`), halo by gradient, reduced motion (`:129-133`); added `mascotFiguresOnScreen` (`ResizeObserver`) |
| D3 `EmptyState` mascot | shipped-as-specified | `MASCOT_HEIGHT=140`, `brand` removed, `ReportsView.svelte:1054` |
| D4 Surfaces | shipped (better than spec) | pairing hero by page-width container query (`PairingView.svelte:401`); revoked/listener-off/unreachable at 180 px; new `UnreachableView` covering the open view (`+layout.svelte:364-371`) |
| D5 Rail-foot bear | shipped-as-specified | `NavRail.svelte:374-391`, opacity 0.18/0.12, hidden collapsed / < 260 px / while a figure shows; browser screenshots not linked |
| D6 PLAN-130 residue | shipped-modified | `BrandMarkSize<R>`; favicon header; `WaitingMark` start frame; S12 opt-in; the runtime size refusal dropped (spec said keep) |
| D7-D9 | shipped | tests, docs (`brand-mark-mascot.adoc` § assets), e2e `pairing.spec.js` (one mascot, 28/64 px marks) |

Weakened tests (operator-accepted, AskUserQuestion): BrandMark size guard → compile-time (`npm-typecheck` in Maven); EmptyState brand →
mascot; two `error-pattern.spec.js` cases (one mildly weaker); `theme-readability` inventory 22 → 23; S12 opt-in. All justified; the PR body
lists 5, the landing 6.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`); no write. Orchestrator re-read GitHub's thread state (8 of 10 unresolved,
  0 replies) and confirmed each fix at the merge head `f3b9a79f`.
- **Verdict:** no blocker, no product major. **Major (process):** the merge rule was NOT met — 8 of 10 CodeRabbit threads had no reply and
  were unresolved at merge (all fixed in code: `3d9b33ee`, `c67ac4c1`, `bd00e8bb`); the PR body states no counts; the landing wrote the three
  review-gate keys as `unknown` and merged. Body items (1 review, 10 actionable + 1 nitpick) answered in an issue comment.
  Minors: reconnect code duplicated and `ListenerOffView` bypasses `retryRuntime` (two inits possible); `FIGURE_HEIGHT` ×3; `NavRail` 258/200
  without guard; `BrandMark.test.ts:227` reads `.webp` as text, `walk` ×3; `shellGone` window untested; held figure never retried after one
  failure; eager hero fetched on phones; dev-page test order-dependent; flaky `NeedsItemCommandsTest` (blank-title draw, visible by reading);
  PR body advisory list stale.
- Confirmed sound: Sonar 0 issues, 0 hotspots, gate OK (2 `S7780` fixed, not dismissed); both dev-page blocks kept beside PLAN-147's;
  `PmConsole.java` not in the squash (already fixed by #425); trailer, no footer.

Routing (operator, 2026-11-13, AskUserQuestion):
- 8 threads: **replied and resolved by the orchestrator** (2026-11-13, one reply per thread citing the fixing commit and the line at `f3b9a79f`); #422 now 0 unresolved.
- All minors → **PLAN-157** deliverable 3 (surface +16); PLAN-157 re-grounded (`ListenerOffView.test.ts` and `UnreachableView` exist).
- Lessons adopted, filed in plan-marshall's store: `2026-11-13-10-004` (review_support crashes on a binary path), `-001` (manage-status
  from a leaf cwd), `-002` (rebase re-gating + functional-review delta, 004+005 merged), `-003` (new: `unknown` review-gate keys fail closed).

## Routing and Merge Behavior

- Review: CodeRabbit one formal review (5879478651, `5b367e78`); later runs rate-limited; Sourcery none. Merged by the run via the queue.
- Cost: a rebase over #425 during finalize re-ran every gate (~1.5 h); functional-review's delta round counted upstream paths.
- Sonar `0` confirmed (issues and hotspots).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 422; `landing` `landings/PLAN-131.md`
- [x] #422 threads replied and resolved (operator)
- [x] PLAN-157 deliverable 3 + re-grounding (operator)
- [x] lessons filed; Open Defects; messages 001-006 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-148 (running) shares the dev page: `+page.test.ts:202-225` takes the first three `empty-state` nodes by order.
- PLAN-70 (parked) builds on the rail-foot bear as built.
