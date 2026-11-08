# PLAN-114: Design D3: Brand and Mascot (Brand Icon, Illustration, Poses, Waiting Mark)

epic: ui-improvements
workstream: WS-06

> Staged plan spec — one design session, handed off as a direct Claude Code session (operator instruction 2026-11-06;
> not the runbook lane, not `/plan-marshall`).
> The DESIGN of this plan is written in the archived operator message named under Sources; this spec fixes the scope, the
> owned documents, the process and the done criteria. Where the two differ in detail, the message governs the design and
> this spec governs the process.

## Objective

Operator design review of 2026-11-06, session **D3 (brand and mascot)** of `design-review-010.md`: integrate the
plan-marshall mascot (the marshal bear) into the web client's design without spoiling it. D3 rewrites
`doc/design/web-app/brand-mark.adoc` as the brand document for the mascot icon, the illustration surfaces, the asset
rules, the provenance and the closed set of six poses with their rules, defines the waiting mark, and presents the **two
icon candidates** (A bear head, B star clipboard) side by side on a review sheet from which the operator chooses one.
D3 changes documents (and draws draft SVGs and a review sheet) only; the artwork of the poses is supplied by the operator or
an image tool the operator names. PLAN-49 (empty state), PLAN-64/66 (welcome and done figures), PLAN-85 (rail foot) and
the D3 brand assets package wait for the merge of this session's pull request with the documents at `AGREED`.

## Sources (read in full before scoping)

All under the epic tree `inbox/archive/design-review/` (resolve the tree with `orchestrator resolve-path --slug
ui-improvements`; read, never write):

- `design-review-005.md` (the whole design: §1 surfaces, §2 asset rules, §3 icon candidates and the review sheet,
  §4 provenance, §5 poses and restraint, §6 the waiting mark, operator decisions)
- `design-review-001.md` (rail cog icon: code-only, PLAN-109; only the distance of the new icon from the kind marks matters)
- `design-review-003.md` §6 (kind marks and tints: the tints stay clear of the mascot's orange and gold)
- `design-review-010.md` § Process, § Design sessions (row D3), § Operator decisions (D3), § Per-plan instructions

## Deliverables

1. **`web-app/brand-mark.adoc` (rewrite)** — the brand document: the chosen icon (construction, colours, uses, provenance),
   the illustration surfaces table (pairing page, onboarding welcome and done, empty states via `EmptyState` `mascot`,
   terminal states, rail foot), the asset rules (derived WebP files under `pm-web-app/src/lib/assets/mascot/`, CSP
   `img-src 'self' blob:` and `assetsInlineLimit: 0` compatible, ≤ 60 KB each, lazy / async, decorative `alt=""`,
   themes with a soft halo, motion rules), the poses with their rules (six poses, closed set; only calm surfaces; never the
   carrier of the state; one mascot per screen; rail foot and icon never change with state), and the waiting mark.
   **Provenance (operator decision):** the mascot is the operator's own work, free to use in the product and to modify
   (poses, icon); recorded before any asset ships.
2. **The icon review sheet** — rendered from the real SVGs on the mock runtime (screenshots, not a mood board): each
   candidate at 16 (favicon, with and without the attention dot), 24, 28 (rail head) and 64 px (pairing), in colour and
   monochrome, light and dark, in the real rail head (expanded and collapsed) and in a browser tab bar beside other tabs,
   next to draft `kind-project` and `kind-plan` SVGs (D3 draws the drafts; D1 owns the final kind marks) to show the
   distance from `kind-plan`, `kind-report` and `kind-task`. The animated waiting mark of both candidates is shown. The
   operator chooses; the chosen candidate's SVGs are kept, the other's discarded, and `brand-mark.adoc` records the choice.
3. **Pose briefs** — one per pose (what it shows, its surface, its rules). The artwork (raster, style of the source image) is
   supplied by the operator or an image tool the operator names, as one set reviewed on a pose sheet on the real surfaces;
   until a pose is accepted its surface uses the source image (full figure or bust).
4. **Other documents** — `web-app/workflow-visual-language.adoc` (the colour exception for the brand icon; after D1),
   `web-app/components.adoc` (`EmptyState` with the `mascot` option, `BrandMark`, `WaitingMark`; after D1),
   `web-app/README.adoc` (the rail head and the rail foot sentence of :138; after D2), `web-app/entry.adoc` (pairing hero),
   `web-app/onboarding.adoc` (welcome / done figures; **after the running PLAN-104 has landed**).
5. **Review and pull request** — see Standing Constraints.

## Standing Constraints

- Design only: no production code. The derived assets, `BrandMark` on the chosen icon, the favicons, the `EmptyState`
  option, the pairing hero, the terminal states and the rail foot background are the D3 brand assets package, staged after
  this session's merge. The review sheet, the prototypes and the draft SVGs are throw-away unless the operator keeps them.
- Order: D3 runs beside D2 for `brand-mark.adoc`, `entry.adoc`, `workflow-visual-language.adoc` and the review sheet; its
  `README.adoc` and `components.adoc` edits are made after D2's pull request has merged, and **D3's pull request merges
  after D2's**. D3 needs D1 first (EmptyState, tokens).
- Each session edits only the lines it owns. `--color-mascot-*` tokens are specified in the design only; the token file
  is not edited here.
- Every document is `Status: DRAFT` in the pull request; the operator sets `AGREED` as the last commit; the pull request is
  merged only on the operator's word. **No auto-merge**: the finalize stops once the pull request is green and the review
  comments are answered, reports, and waits.
- Review with `pm-documents:ref-documentation` (docs review) plus `pm-documents:ref-asciidoc` (`validate`, `verify-links`)
  and `pm-documents:ref-svg-diagrams` for the SVG drawings, clean.
- The rail-foot bear is wanted as a reversible test with a fixed, contrast-checked opacity; removing it touches no other
  screen. The source image stays in `plan-marshall/doc/resources/planmarshall.png`; only derived files are ever copied.
- No dependency or plugin is added. Work in the session's own worktree and branch; ports are passed explicitly.
- Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer.

## Claim Labels

- OBSERVED: `doc/design/web-app/brand-mark.adoc`, `entry.adoc`, `onboarding.adoc`, `workflow-visual-language.adoc`,
  `components.adoc`, `README.adoc` exist — listed at HEAD 60b4bfa5
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: all six documents named exist on main
- HYPOTHESIS: the brand mark is used in four places (rail head 24 px in a 28 px violet plate, pairing page 64 px, empty
  states muted, favicon `static/brand-mark.svg` and `brand-mark-attention.svg` applied by `src/favicon.ts`) — confirm/refute
  at `pm-web-app/src/lib/shell/NavRail.svelte` § brand block, `pm-web-app/src/lib/views/PairingView.svelte` §
  `plan-marshall-mark` and `pm-web-app/src/lib/favicon.ts` (verify-at-outline)
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: NavRail.svelte:91,96 BrandMark size 24 with plate; BrandMark.svelte:5,64-65 28 px plate; PairingView.svelte:91 and EmptyState.svelte:30 at 64; favicon.ts:12,15 use static/brand-mark*.svg
- HYPOTHESIS: the daemon CSP admits `img-src 'self' blob:` only and `vite.config.ts:122-125` sets `assetsInlineLimit: 0`, so
  every mascot image is a file of the app's own origin — confirm/refute at `pm-web-app/vite.config.ts` §
  `assetsInlineLimit` and the CSP in `plan-marshall/plan-marshall-mcp` `cli-and-security/03-web-server.adoc` (verify-at-outline)
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: vite.config.ts:124 assetsInlineLimit 0; plan-marshall-documentation 03-web-server.adoc:66 img-src 'self' blob:
- HYPOTHESIS: `EmptyState.svelte` is used only by Reports today — confirm/refute at `pm-web-app/src/lib/components/EmptyState.svelte`
  § usages by a repo-wide search (verify-at-outline)
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: EmptyState is used in production only by ReportsView.svelte (besides the dev showroom and its test)
- Verify-first clause: re-read each cited range at the current HEAD before writing the rule it supports.

## Expected Surface

- OBSERVED: `doc/design/web-app/brand-mark.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/workflow-visual-language.adoc` — present at HEAD 60b4bfa5 (after D1)
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 60b4bfa5 (after D1 and D2)
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 60b4bfa5 (after D2)
- OBSERVED: `doc/design/web-app/entry.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 60b4bfa5 (after the running PLAN-104)

## Dependencies and Sequencing

- Depends on: `PLAN-112` (D1: EmptyState, tokens, final kind marks), `PLAN-104` (for `onboarding.adoc` only); its
  `README.adoc` / `components.adoc` edits depend on `PLAN-113` (D2)
- Blocks: PLAN-115 (D4: WaitingMark, `onboarding.adoc` after D3), the D3 brand assets package, PLAN-49, 64, 66, 85, 70
- Overlaps with: `PLAN-113` (D2: `README.adoc`, `components.adoc`; D3's merge is after D2's), `PLAN-115` (D4 edits
  `onboarding.adoc` after D3)
- Adjacent to: none

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. The gate "after the running PLAN-104" is satisfied (#357 landed); D2 (PLAN-113) is still unlanded for `README.adoc` and `components.adoc` (PLAN-112 landed). No mascot or pose assets and no `WaitingMark` exist yet. The claims name no HEAD later than 60b4bfa5; the production users of `EmptyState` are `ReportsView` only.

## Operator Decision 2026-11-08 (the waiting mark)

Amends `design-review-005.md` §3 and §6 and deliverables 1, 2 and 4; the message's other rules stand. The waiting mark no
longer follows the icon choice. Both elements of candidate B are kept, in two roles:

- **The marshal star badge** is the brand mark/badge: candidate B on the review sheet is the star badge alone, without the
  clipboard. The operator still chooses between A (bear head) and B (star badge); `brand-mark.adoc` records the choice.
- **The clipboard is the waiting mark** (`WaitingMark`), whatever icon is chosen: a clipboard with a short TODO list whose
  lines are ticked one after another (✓ by ✓, then the list starts again), drawn in the same line weight and palette as the
  icon set. Static with reduced motion (the list shown fully ticked or unticked, never mid-animation). The star glint is
  dropped as a waiting animation.
- Unchanged from §6: always with a text, one per screen, only where the operator waits on the system (the chat's working
  line, long checks on screens without a figure), never an object's state, short waits (under about 1–2 s) keep the neutral
  `Spinner`, and no waiting mark on a screen that shows a figure.
- The review sheet (deliverable 2) shows the animated clipboard waiting mark once, beside both icon candidates, and in the
  chat's working line and an onboarding check step, light and dark, with and without reduced motion.
- `components.adoc` (deliverable 4) defines `WaitingMark` as the clipboard with its tick animation, and `brand-mark.adoc`
  (deliverable 1) carries its drawing and motion rules. Expected Surface unchanged.

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository, after PLAN-112 has merged (it may run beside PLAN-113), and give it exactly this prompt (the orchestrator
prints the absolute spec path at emit time):

```text
Implement the staged design plan PLAN-114 (design D3: brand and mascot) of the epic ui-improvements. Read the whole spec
first: <absolute path of this file>. Then read every source named under "Sources" in full. Follow the spec's Standing
Constraints exactly: design documents only, Status: DRAFT, draw both icon candidates and present them on one rendered
review sheet for the operator's choice before AGREED, write the pose briefs, ref-documentation docs review plus
ref-asciidoc validate and verify-links, one pull request, no auto-merge, stop when the pull request is green and its review
comments are answered, and report. Do not edit anything under .plan/orchestrator/ except your own inbox message
(orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only this repository's design documents and throw-away review artifacts. It
creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator
owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's
qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
