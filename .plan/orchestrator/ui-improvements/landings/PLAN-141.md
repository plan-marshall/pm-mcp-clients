# Landing Analysis: PLAN-141 — Design D6: Plan and Epic Tabs, the Intent Design and the Chat's Side Column

epic: ui-improvements
workstream: WS-06
pr: #403 (https://github.com/plan-marshall/pm-mcp-clients/pull/403), merged `8568b56e` through the merge queue on the operator's word (2026-11-10)

> Landing record for a direct design session (no lane, no landing-facts block). Written by the orchestrator from the
> session's report (`inbox/archive/plan-141/plan-141-001.md`), the PR, and the orchestrator's own review of #403.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| 1 Current state and options, seven questions one at a time | shipped-as-specified | the session's findings file in its worktree; seven operator answers of 2026-11-10 (epic.md Watches) |
| 2 The plan documents | shipped-as-specified | `shared/plan-view.adoc#tabs`, `web-app/plan-view-interaction.adoc` rewritten for the column model |
| 3 The epic and intent documents | shipped-modified | `shared/epic-view.adoc#tabs`, `shared/intent-and-outline.adoc`, new `shared/intent-and-outline-texts.adoc` (75 keys after review), verdict states in `web-app/components-tags.adoc`; README at 492 lines |
| 4 The navigation seams | shipped-as-specified | `web-app/navigation.adoc` (#url, #side-columns, #rely), `shared/plan-overview.adoc#persistence`, `web-app/chat.adoc`; Q7 needed no change in `chat.adoc:239`/`machine-settings.adoc:76` |
| 5 Review and pull request | shipped-modified | the operator's AGREED was set by the orchestrator on the operator's word after its review; CodeRabbit reviewed `721afd68` |

Spec hypothesis refuted by the session: the engine's intent record DOES carry ADR relation, evidence, `NG-n`, `role` and the
`not_run` reason; the client contract grows (a contract + mock step before PLAN-145).

## Three-Layer Analysis

- **Dispatch:** one read-only review leaf (`execution-context-level-3`) on the session's head `207b62f0` against the seven
  decisions — reason: 13 design documents, the depth of a reading; no separate leaf on the merge commit, because every
  later commit is the orchestrator's own fix of that review or of CodeRabbit's review of `721afd68`, each re-read at its line.
- **Verdict:** layer 1 clean (the decisions are stated and consistent); layer 2 findings (3 blockers, 6 majors, 11 minors,
  4 nits — fixed in `e4109c2e`); layer 3 findings (CodeRabbit: 5 minor + 1 major-unverified — fixed in `87288908`,
  `cc00f135`). Nothing open at the merge but the minor below.

Fixed on the branch (orchestrator, on the operator's word): README plan-tree and fill rules; Changes text per client; area
`intent` registered (`texts-and-languages.adoc`, AGREED, edited); chart read-only on the project's setup tab
(`project-explorer.adoc`, AGREED, edited) and named by its resource; head counts from the verdict words; German keeps
`Deliverable`, `Outline`, `Role`; What waits addressed as `/?lens=waits` (operator's choice; `needs-and-find.adoc` edited);
a task address's `chat=open` is the plan's chat; selecting a part ends follow mode; the over_size resource needs an absent
proposal (contract step). Not fixed (minor): `navigation.adoc` cites plan ids `PLAN-135..137`, `PLAN-142..144` that exist only
in the ledger.

## Routing and Merge Behavior

- Review: CodeRabbit refused twice on quota, then reviewed `721afd68`: 5 inline threads, all replied and resolved; the review
  body's additional comment answered by PR comment. `review-gate`: 0 unresolved threads. Sourcery out of budget.
- CI/merge: docs only, gate green; merged via the queue as `8568b56e`.
- Sonar: `n/a` (docs only; analysis skipped by change detection).
- Tokens / model: not measured (direct session).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 403; `landing` `landings/PLAN-141.md`
- [x] epic.md Watch retired to this report; the owed amendments stay listed there until done
- [ ] spec amendments owed: PLAN-52, 55, 58, 62, 135..137, 142..146; `doc/plans/plan-changes.adoc` (`?tab=changes&file=`, a
  repository document: a direct change); a contract + mock step before PLAN-145 (stage, or fold into PLAN-145)
- [ ] the D6 worktree `.plan/local/worktrees/141-design-d6-tabs-intent-and-side-column` and its local branch to remove

## Follow-Ups

- The amendments above; then the parked web plans that waited on D6 (PLAN-55, 58, 62, 145, 146; PLAN-142..144 on the D1
  foundation too) can be re-grounded for release.
