# Landing Analysis: PLAN-130 — Web: the Brand Mark and the Waiting Mark (D3, First Half)

epic: ui-improvements
workstream: WS-02
pr: #414 (https://github.com/plan-marshall/pm-mcp-clients/pull/414), squash `078b2ec5` (22 files), merge head `73ef0500`

> Written by the `analyze` verb (2026-11-11) from `web-brand-mark-and-waiting-mark-009.md`, verified against git, `ci pr comments`,
> the review threads (GraphQL), `runbook_lane review-gate` and the PR body. `/plan-marshall` lane, claude `claude-sonnet-5-5`/low,
> plugin and executor `0.1.2090`; 14 572 367 tokens, 31 535 s wall.

## Deliverable Fidelity vs Spec

The spec has five deliverables; the landing counts seven (its own task split; "deliverable 7 operator-accepted" is the
Sonar-count criterion).

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Mascot tokens | shipped-modified | `tokens.css:97-101` (`:root` only), value test `tokens.test.ts:448-459`, no-literal scan `:609-630`; the bullet "the kind-tint check reads fur and gold from the tokens" not delivered and not reported — the check does not exist yet (`components-tags.adoc:239`; it is PLAN-147's) |
| D2 BrandMark | shipped-as-specified (one gap) | `BrandMark.svelte:1-111` (no plate, forced colours `:104-110`, aria `:36-41`); callers `NavRail.svelte:91,98`, `PairingView.svelte:91`, `EmptyState.svelte:30` — the only ones in any `.svelte` file. Gap: no brand icon in the onboarding header on a phone (rail hidden below 768 px, `NavRail.svelte:440`; `onboarding.adoc:43`) |
| D3 Tab copies | shipped-as-specified | `static/brand-mark*.svg` equal to `doc/resources/brand/favicon*.svg`; `plan-marshall-mark.svg` deleted, unreferenced; `favicon.test.ts:121-212` (palette set, stroke-inclusive 1..23 bounds) |
| D4 WaitingMark | shipped-as-specified | `WaitingMark.svelte:20` polite `role=status`, `:49-53` colour, `:83-163` 3.6 s timing, `:165-181` reduced motion; dev fixture `+page.svelte:382-386`; `dev.section.brand`/`dev.waiting.label` en/de parity |
| D5 S12 and browser evidence | shipped (not shown by reading) | PR body: rendered at 16 and 24 px, `kind-plan`, `kind-task`, `kind-report` read neither as clipboard nor badge; `kind-plan.svg` unchanged. 40 screenshots + 3 sheets under `pm-web-e2e/test-results/s12-brand/`, not committed |

Added-unplanned: `pm-web-e2e/tests/brand-s12-screenshots.spec.js` (414 lines, permanent, asserts nothing, dev-server only);
`NavRail.test.ts` (+55), `+page.test.ts` (+23); `color: var(--color-brand)` removed from `PairingView`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: 22 files of components, tokens, assets and e2e specs. No
  write; it loaded only the base persona (not the listed frontend skills) and skipped the envelope's logging writes — disclosed.
  Re-read by the orchestrator at `078b2ec5`: `NavRail.svelte:91,98,440`, the `BrandMark` caller grep over `.svelte`,
  `BrandMark.svelte:17-30`, `brand-mark.adoc:172-182`, `onboarding.adoc:43`, the PR body's S12 and Test Plan. Confirmed.
- **Verdict:** layer 1 findings (minor: the Done-when "`git grep brand-mark-plate` is empty" contradicts "a test fails if the plate
  returns"; D2 asks a monochrome-64 `EmptyState` against `brand-mark.adoc:180` "Not used: the empty states"; D1 assumes a kind-tint
  check that does not exist; "refused by the type" needs a per-rendering type); layer 2 findings (minor: the D1 tint bullet and
  HYPOTHESIS 3 unreported; forced colours, reduced motion and timing tested by regex over source only; no type-refusal test); layer 3
  findings (all minor/nit: the size union draws nothing for an undelivered size; the phone onboarding without the icon; the S12 spec
  in the permanent suite, dev-server import `:288`, tab icon rendered at 64 px `:301-317`; stale `favicon.ts:5-6` comment; a possible
  round-cap dot at the waiting mark's start frame; `Math.random` input in `WaitingMark.test.ts:20-26`). No blocker or major.
- The three "weakened" tests (operator-accepted): `EmptyState` and `favicon` replacements are stronger than before; `BrandMark`'s
  replacements guard both renderings, every size, token colour and no plate — the lost guard is 1..23 containment of the inline
  drawing (measured by hand inside).
- Confirmed sound: tokens exact and `:root` only, no colour literal elsewhere under `src/`; colour only via `--color-mascot-*`;
  collapsed-rail naming; static tab SVGs inside 1..23; no `{@html}`, no timer waits; no dangling `plan-marshall-mark` reference.

Routing (operator, 2026-11-11, AskUserQuestion): the size type, the `favicon.ts` comment, the S12 spec and the start-frame dot →
**PLAN-131** deliverable 6 (surface +6); the phone onboarding header → **PLAN-64** (a Done-when added). The monochrome `EmptyState` is
removed by PLAN-131 deliverable 3 (`mascot` replaces `brand`). PLAN-147's S12 clause settled from the PR body (the `kind-plan`
redraw only for being apart from `kind-task`).

## Metrics and Anomalies

- Facts complete for a claude lane (tokens, plugin version present); `unresolved_review_threads`, `review_body_findings_*`
  `unknown` (the lifecycle does not measure them).
- Eight candidate lessons (001-008): four spec-authoring rules adopted by the operator into epic.md; the pm-web-app inventory
  mapping a direct-PR candidate; three plan-marshall-side items to Open Defects. All archived.
- Pre-push gate recorded on whole-tree runs, not per bundle (the lane's own report).

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `73ef0500` (the merge head): 3 inline threads (size union, favicon unsupported-shape reachability,
  phone screenshots without the rail head), all replied and resolved as declined/known limits; the leaf found the first decline only
  partly sound (routed) and the third exposing the onboarding gap (routed). Merge rule met. `ci pr comments` says `unresolved: 6`
  over 15 comments, but GraphQL shows 3 of 3 threads resolved; `review-gate` answered `undecidable_read` (the "Files with no
  reviewable changes" block — the standing Open Defect). A CodeRabbit walkthrough edit re-opened the triage and held the merge
  (lesson 006).
- CI/merge: via the queue (`078b2ec5`, 17:20Z).
- Sonar: `0` confirmed by the lane at `73ef0500` (analysis revision = head); hotspots not counted. Not re-read here (merged PR,
  Sonar MCP down).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 414; `landing` `landings/PLAN-130.md`
- [x] PLAN-131 deliverable 6, PLAN-64 Done-when, PLAN-147 S12 settled (operator)
- [x] epic.md: spec-authoring rules (lessons 002-005), Open Defects (006-008), direct-PR candidate (001)
- [x] messages 001-009 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-147 (head of the D1 foundation chain) and PLAN-131 (mascot) waited on this landing: unblocked.
