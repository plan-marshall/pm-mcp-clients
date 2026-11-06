# PLAN-112: Design D1: Component Vocabulary (Tags, Interaction Elements, Fields and Forms)

epic: ui-improvements
workstream: WS-06

> Staged plan spec — one design session, handed off as a direct Claude Code session (operator instruction 2026-11-06;
> not the runbook lane, not `/plan-marshall`).
> The DESIGN of this plan is written in the archived operator messages named under Sources; this spec fixes the
> scope, the owned documents, the process and the done criteria. Where the two differ in detail, the messages govern the
> design and this spec governs the process.

## Objective

Operator design review of 2026-11-06, session **D1 (component vocabulary)** of `design-review-010.md`: write the closed
component vocabulary of the web client as design — the **tag family** (message 006: `StatusLabel`, `ReasonBadge`,
`CountBadge`, `Chip`, `PrChip`, `KindMark`, `PhaseLabel`, markers), the **interaction set** (message 007: `Button`,
`IconButton`, `ActionBar`, `RowMenu`, `Link`, `ExternalLink`, `RowLink`, `SegmentedControl`, `Toggle`, `Tabs`, `Dialog`,
`ConfirmDialog`, `Breadcrumb`) and **fields and forms** (message 008: `Field` … `FormStatus`, `Compose`), plus the
`KindMark` and the three kind tints of message 003 §6. D1 changes documents only. Every other design session and every
web plan that adopts these components waits for the merge of this session's pull request with the documents at `AGREED`.

## Sources (read in full before scoping)

All under the epic tree `inbox/archive/design-review/` (resolve the tree with `orchestrator resolve-path --slug
ui-improvements`; read the files, never write them):

- `design-review-006.md` (tags), `design-review-007.md` (interaction elements), `design-review-008.md` (fields and forms)
- `design-review-003.md` §6 (kinds, `KindMark`, the tints) and §1 (lenses, only as context)
- `design-review-010.md` § Process, § Design sessions (row D1), § Operator decisions, § Per-plan instructions
- `design-review-005.md` §3, §6 and the "Affected plans" line on KindMark colours (the tints stay clear of the mascot's
  orange and gold)

## Deliverables

1. **`web-app/components.adoc`: the vocabulary** — the tag family (006), the interaction set (007), "Fields and forms"
   (008) and `KindMark` with the kind tints (003 §6, D1 owns them). D1 writes its rules in terms of "the object shown" and
   "the selected row"; D2 adds the column clauses. Operator decisions already taken: only Project, Epic and Plan are
   tinted, Side job, Decision, Report and Machine item stay neutral (shape + word); the tints stay clear of the state
   hues, the brand violet and the mascot's orange and gold and reach ≥ 3:1 as non-text graphics in light and dark (a
   measurable minimum hue / ΔE distance is set here); German buttons use the du-imperative ("Speichere",
   `texts-and-languages.adoc:87`).
2. **Web-app documents** — `web-app/README.adoc` :141, :204, :297, :351 (the one edit for 006 + 007), :370-386, :448 and the
   control tokens; `web-app/workflow-visual-language.adoc:451-476` (the phase word of the catalogue instead of the bare
   token); `web-app/interaction-affordances.adoc#acts`.
3. **Shared documents** — `shared/texts-and-languages.adoc` (phase vocabulary, label/hint/placeholder/error rules, one key
   per concept); `shared/plan-overview.adoc:65-69` (kind = `KindMark`); `shared/needs-and-find.adoc` (item kinds become
   reasons, the row's `KindMark` is the object's kind, `decision` items are Decision rows); `shared/project-explorer.adoc`
   (rows, release dialog); `shared/work-actions-texts.adoc` (`control.end.*`, `control.cancel_epic.*`; and the agreed
   `control.plan.settings` → "Plan settings" / "Plan-Einstellungen" if PLAN-109 has not already landed it).
4. **The terminal's designs for every shared text D1 changes** — `terminal-client/19-actions.adoc`, `22-texts-actions.adoc`
   and `doc/design/README.adoc` ("Both clients, one state"). ⛔ `19-actions.adoc` is edited **after the running PLAN-97 has
   landed** (it owns that file); if PLAN-97 has not landed when the rest is done, do this part last and rebase, never
   edit around its pull request.
5. **Review and pull request** — see Standing Constraints (DRAFT, review, no auto-merge, operator sets `AGREED`).

## Standing Constraints

- Design only: no production code, no test, no catalogue (`*.properties`) change in this session. The catalogue
  clean-up and the lints (as warnings) belong to the D1 foundation package, staged after this session.
- Each session edits only the lines it owns (`design-review-010.md` § Design sessions). A document owned by two sessions is
  edited in the order of that table: D1 first. Do not pre-empt D2's column clauses, D3's brand edits or D4's chat.
- Every document created or changed is set to `Status: DRAFT` in the pull request (lifecycle `DRAFT` → `AGREED` → `FINAL`,
  `doc/design/README.adoc:20`). The operator sets `AGREED` as the last commit of the pull request; **the pull request is
  merged only on the operator's word** (`project-setup.adoc` § From a Branch to `main`). The session's finalize stops once
  the pull request is green and the review comments are answered, reports, and waits. No auto-merge.
- Review the `doc/design/` changes with `pm-documents:ref-documentation` (docs review) plus `pm-documents:ref-asciidoc`
  (`validate` and `verify-links`), clean, as PLAN-74 does. The runbook lane's `trace.py` does not run in a direct session.
- The operator's decisions of the session are written into the design documents; points still open go into
  `plan-marshall-documentation/doc/discussions/open-questions.adoc` (`project-setup.adoc` § Open Questions), not into
  this repository's ledger.
- Where this repository's `CLAUDE.md` says "never add a dependency without asking": none is added here. D1 adds no
  prototype dependency; a throw-away prototype (KindMark tints on a review sheet) may use the mock runtime only.
- Work in the session's own worktree and branch; the main checkout is never edited. Ports for any mock or app start are
  passed explicitly (`AGENTS.md` § Ports); never stop a process this worktree did not start.
- Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer.

## Claim Labels

- OBSERVED: `doc/design/web-app/components.adoc`, `README.adoc`, `workflow-visual-language.adoc`,
  `interaction-affordances.adoc`, `doc/design/shared/{texts-and-languages,plan-overview,needs-and-find,project-explorer,work-actions-texts}.adoc`
  and `doc/design/terminal-client/{19-actions,22-texts-actions}.adoc` exist — listed at HEAD 60b4bfa5
- HYPOTHESIS: the line numbers named in the messages (`README.adoc` :141, :204, :297, :351, :370-386, :448;
  `workflow-visual-language.adoc:451-476`; `plan-overview.adoc:65-69`) still point at the text the messages quote — the
  review was written against an earlier HEAD and PLAN-102/105/106 have landed or are running since — confirm/refute at
  each named file's cited range (verify-at-outline)
- HYPOTHESIS: `--color-epic #7A3FD1` is the only kind colour today and is close to the brand violet `#5B4BDB` — confirm/refute
  at `pm-web-app/src/styles/tokens.css` § `--color-epic` and § `--color-brand` (verify-at-outline)
- HYPOTHESIS: the terminal reads the needs spellings (`waiver` / `waiver_request`, `waits_for_you` / `operator_wait`)
  through `PmConsole.java:741` and `OverviewFormatting.java:197` — confirm/refute at those two symbols (verify-at-outline)
- Verify-first clause: before writing a rule, re-read the cited range at the current HEAD; a refuted citation re-scopes
  that rule and is reported in the pull request, never silently worked around.

## Expected Surface

- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/workflow-visual-language.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/interaction-affordances.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/project-explorer.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/work-actions-texts.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at HEAD 60b4bfa5 (owned by the running PLAN-97 until it lands)
- OBSERVED: `doc/design/terminal-client/22-texts-actions.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/README.adoc` — present at HEAD 60b4bfa5

## Dependencies and Sequencing

- Depends on: `PLAN-108` (`web-design-docs-residue`, runs before D1 so the two do not edit `components.adoc` in parallel);
  `PLAN-97` for `terminal-client/19-actions.adoc` only
- Blocks: PLAN-113 (D2), PLAN-114 (D3), PLAN-115 (D4) and, through them, every gated web plan (`design-review-010.md`
  § Per-plan instructions); the D1 foundation package and the terminal package are staged after this session's merge
- Overlaps with: `PLAN-106` (running; `WorkControlPanel` act lists — line references in 007 and 009 into it are re-read when
  used), `PLAN-109` (`control.plan.settings` text)
- Adjacent to: none

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository and give it exactly this prompt (the orchestrator prints the absolute spec path at emit time):

```text
Implement the staged design plan PLAN-112 (design D1: component vocabulary) of the epic ui-improvements. Read the whole
spec first: <absolute path of this file>. Then read every source named under "Sources" in full. Follow the spec's
Standing Constraints exactly: design documents only, Status: DRAFT, ref-documentation docs review plus ref-asciidoc
validate and verify-links, one pull request, no auto-merge, stop when the pull request is green and its review comments
are answered, and report. Do not edit anything under .plan/orchestrator/ except your own inbox message
(orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only this repository's design documents (and the sibling repository files this
spec names, none for D1). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR
and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
