# Landing Analysis: PLAN-114 — Design D3: Brand and Mascot

epic: ui-improvements
workstream: WS-06
pr: #379 (https://github.com/plan-marshall/pm-mcp-clients/pull/379), merged as `64a18ed1` (squash, 24 files under `doc/`, +978/-297; parent `95df75bb`, so D3 landed after D2; the operator's AGREED commit `878615a2`, made by the orchestrator on the operator's decisions)

> Landing record for a direct Claude Code design session. Inbox messages `plan-114-002.md` and `plan-114-003.md` and the
> operator's pastes, 2026-11-08. Checked against git, the PR (read through the `ci` abstraction) and a read-only leaf.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence |
|-------------|---------|----------|
| 1 `brand-mark.adoc` rewrite | shipped-modified | split for the 500-line convention into `brand-mark.adoc` (301: icon `:70-143`, palette `:38-68`, uses `:152-181`, tab `:183-191`, waiting mark `:193-232`, contrast `:234-264`, provenance `:266-274`) and `brand-mark-mascot.adoc` (217: surfaces `:14-56`, assets `:58-71`, rail foot `:73-82`, poses `:84-136`) |
| 2 Icon review sheet | shipped as process | the Choice is recorded (`bm:75`); the sheet and candidate A are deleted, as the spec said, but `bm:75,120` still cite the sheet as evidence (S3) |
| 3 Pose briefs | shipped | six briefs `bmm:143-189`, pose sheet `:191-194`, provenance table `:196-207` (none accepted yet) |
| 4a `workflow-visual-language.adoc` exception | shipped | `WVL:93-101`, `:144` |
| 4b Components | shipped-modified | `BrandMark`, `MascotFigure`, `WaitingMark` in the new part `components-brand.adoc` (58); `EmptyState` `mascot` replaces `brand` (`components.adoc:432-439`) |
| 4c README rail head and foot | shipped | `README:142-146,188,362`; the Reports / System / Cleanup layouts moved verbatim to `view-layouts.adoc` (README 573 → 498: D2's C1 closed) |
| 4d Pairing hero | shipped | `entry.adoc:26-34,49-51,103,121` |
| 4e Onboarding welcome and done | shipped | `onboarding.adoc:43,45-46,57,65,94,450`, `onboarding-steps-4-7.adoc:444` |
| Operator decisions (icon B, `#A0602E`, 28 px without plate, clipboard waiting mark, MascotFigure, provenance, pose art later, both bear tokens, layout move, `entrance`/`src`, WaitingMark's own status line) | shipped | `bm:75,55-57,160,168,198-212,269,41`; `cb:45,54`; `bmm:125-136`; `view-layouts.adoc:12-92` |

All 12 changed design documents read `Status: AGREED` at `64a18ed1`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`), documents only.
- **Verdict:** layer 1 sound (the rail-foot opacity check is handed to PLAN-70, `bmm:78`); layer 2 complete; layer 3 has no blocking error, 13 minor seams and 1 conformance seam. All anchors and `image::` targets resolve; the CSP claim matches `03-web-server.adoc:66`.
- **Re-read by the orchestrator at `64a18ed1`:** line counts (`onboarding.adoc` 504, `WVL` 721, README 498); `README:22,122`; `bmm:43` with the `entry.adoc` anchors (`#after-pairing` at `:124`); `o47:187,374`; `act-favourite-on.svg` (star with a centre dot); "an unknown address" at `bmm:43,116,179` only. All confirmed.

### Seams

| Id | File:line | Seam | Severity |
|----|-----------|------|----------|
| S1 | `bm:218-219`, `brand-waiting-mark.svg` vs `shared/work-actions.adoc:356-373` | the waiting mark is placed in "the chat's working line", which the chat design does not have; the example text is German in an English diagram | minor (D4) |
| S2 | `bm:205`, `cb:58` vs `onboarding.adoc:45,65` | 24 px "at the head of a step" vs onboarding's 20 px at the head of the content | minor |
| S3 | `bm:75,120` | `act.favourite-on` called a plain star, but it has a centre dot; both lines cite the deleted review sheet | minor |
| S4 | `WVL:98` vs `bm:159-172`, `cb:25` | colour sizes 16/24/28/64 vs 16/28/64; `cb` does not say which sizes are colour | minor |
| S5 | `bmm:43,116,179` | "an unknown address" is a terminal state with a pose, but no design defines that page | minor |
| S6 | `bmm:43`; `entry.adoc:58,103` | the not-reachable surface links `#revoked` (it is `#after-pairing`); a revoked device lands on the pairing page (`welcome`) yet is given `access-ended` | minor |
| S7 | `README:22,122` | the Components entries name two parts, not the third (`components-brand.adoc`) | minor |
| S8 | `onboarding.adoc:45` vs `o47:187,374` | "every [S] state shows the waiting mark", but two long runs keep the glyph `⠦` | minor |
| S9 | `o47:444`, `bmm:33-34` vs `shared/project-onboarding.adoc:38` | the project onboarding's done screen "carries the same figure"; the shared design never says so | minor |
| S10 | `bmm:152-157` vs `:33-34` | the `done` surface's ticks vs the source figure's open boxes; declared, temporary | minor |
| S11 | `theme-readability.adoc:23,154-160` vs `bm:41,283` | the token authority has no `--color-mascot-*` rows or contrast pairs | minor |
| S12 | `bm:117-122`, `WVL:101` vs `icons/ui/kind-plan.svg` | distance from the clipboard rests on the clip; D1 owns the final kind marks | minor |
| S13 | `doc/resources/brand/favicon-attention.svg` vs `bm:79` | the attention dot leaves the 1..23 grid | minor (cosmetic) |
| C1 | `onboarding.adoc` (504), `WVL` (721) | over the 500-line convention; `onboarding.adoc` was pushed over by this PR | conformance |

Code still draws the old mark until the D3 brand assets package: `app.html:6`, `favicon.ts`, `NavRail.svelte:91,96`, `BrandMark.svelte`, `EmptyState.svelte` (`brand`), their tests, `pm-web-e2e/tests/chrome.spec.js:119-122`, `pairing.spec.js:35` (expects 64 px). Expected, not a seam.

## Routing and Merge Behavior

- **Review:** CodeRabbit, 1 body (6 actionable, 1 nitpick, 1 additional), on head `9aad1ba`. Threads: 5, all resolved, all replied; the body answered in `a2121ed1`. Not reviewed: README, `components.adoc`, `components-brand.adoc`, `view-layouts.adoc`, `components-tags.adoc` and every commit after `a2121ed1` (rate limit on the third request). The merge rule is met; the gap is recorded. Sourcery skipped (budget).
- **Decisions:** the operator's D3 decisions (2026-11-08) and the review of the README/components text; AGREED commit `878615a2`, label `skip-bot-review`.
- **CI and merge:** green; merged through the merge queue on the operator's word as `64a18ed1`.
- **Cleanup:** the D3 worktree (clean, nothing unpushed) and branch removed; the three sibling-repo symlinks in `.plan/local/worktrees/` removed.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 379; `landing` `landings/PLAN-114.md`; `plan_marshall_plan_id` `plan-114`
- [x] epic.md Watch
- [x] seams reported to the operator 2026-11-08

## Follow-Ups

- S1–S13 and C1: reported to the operator with the D2 seams (see the decision in `epic.md`).
- The brand assets package (code): `BrandMark` redraw without `plate`, `MascotFigure`, `WaitingMark`, `EmptyState` `mascot`, favicons, the e2e expectations.
