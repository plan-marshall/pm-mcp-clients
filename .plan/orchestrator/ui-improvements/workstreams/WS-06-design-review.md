# WS-06: Design Review 2026-11-06

epic: ui-improvements

> Charter document for one workstream — a coherent slice of the epic with its own goal
> and surface. Lives at `workstreams/WS-06-design-review.md` and is tracked in the
> `workstreams[]` field of the epic header, `status.json`. See
> `persona-plan-orchestrator/standards/orchestration-model.md` for the tier contract.

## Charter

The operator's browser design review of 2026-11-06 (inbox messages `design-review-001` … `010`, archived under
`inbox/archive/design-review/`) changes the design of both clients before it changes their code. This workstream owns
the **design sessions** (D1 component vocabulary, D2 navigation, D3 brand and mascot, D4 chat and voice) and the small
design-document residue that has to land before them. Its outcome: every document the review names is `AGREED` and
merged, so that the held and gated web plans of WS-02/WS-03 can be restarted against it. The implementation packages the
design produces (terminal package, D1 foundation, D2 navigation engine, D3 brand assets, D4 contract + mock, D4 web chat,
D4 voice, the views no staged plan owns) are staged **after** their design session, not before.

## Scope

A design session is a docs plan: it creates or changes the documents it owns in `doc/design/` (and, for D4, requirements
and specification in sibling repositories), reviews them, opens one pull request per repository with the documents at
`Status: DRAFT`, and stops. The operator sets `AGREED` as the last commit and the pull request is merged on the
operator's word only.

- In scope: `doc/design/web-app/*`, `doc/design/shared/*`, `doc/design/terminal-client/*` (the shared texts the review
  changes), the design residue of the reports landing (PLAN-108), throw-away prototypes on the mock runtime that settle a
  design before `AGREED`.
- Out of scope: any production code, test or catalogue change (WS-02/WS-03/WS-04 packages), the code-only runbooks of the
  review (PLAN-109, 110, 111 live in WS-03), and `doc/claude_review/`.

## Plans

| Plan | Status | Notes |
|------|--------|-------|
| PLAN-108-web-design-docs-residue | staged | PLAN-74 deliverables 3 and 4 split off; runs before D1 |
| PLAN-112-design-d1-component-vocabulary | staged | tag family, interaction set, fields and forms, KindMark (messages 006, 007, 008) |
| PLAN-113-design-d2-navigation | staged | `navigation.adoc`, column window, URL scheme (messages 003, 004 design text); after D1 |
| PLAN-114-design-d3-brand-and-mascot | staged | `brand-mark.adoc` rewrite, icon review sheet, pose briefs (message 005); beside D2 |
| PLAN-115-design-d4-chat-and-voice | staged | `chat.adoc`, roadmap/requirement first, `transcribe` role (message 009); after D1, D2, D3 |

## Sequencing and Surface Notes

- Order: PLAN-108 → D1 → D2 and D3 → D4 (`design-review-010.md` § Design sessions, order and ownership).
- A document owned by two sessions is edited in the order of the table, never in parallel: `README.adoc`,
  `components.adoc`, `plan-overview.adoc`, `project-explorer.adoc`, `interaction-affordances.adoc`, `onboarding.adoc`,
  `workflow-visual-language.adoc`, `view-density-and-filtering.adoc`.
- No design session runs with auto-merge. Each session's gate for the plans that wait on it is the merge of its pull
  request with the documents at `AGREED`.
- D1 edits `terminal-client/19-actions.adoc` after the running PLAN-97 has landed; D3 and D4 edit `web-app/onboarding.adoc`
  after the running PLAN-104 has landed.
- Hand-off is a direct Claude Code session per plan (operator instruction 2026-11-06), not the runbook lane and not
  `/plan-marshall`.
