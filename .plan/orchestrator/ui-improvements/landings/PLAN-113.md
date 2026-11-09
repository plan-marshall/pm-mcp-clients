# Landing Analysis: PLAN-113 — Design D2: Navigation

epic: ui-improvements
workstream: WS-06
pr: #380 (https://github.com/plan-marshall/pm-mcp-clients/pull/380), merged as `95df75bb` (squash, 15 documents; the operator's AGREED commit `467fa02f`, made by the orchestrator on the operator's decisions)

> Landing record for a direct Claude Code design session. Inbox message `plan-113-001.md` and the operator's paste,
> 2026-11-08. Checked against git, the PR (read through the `ci` abstraction) and a read-only leaf.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence |
|-------------|---------|----------|
| `web-app/navigation.adoc` (sections 1–10 of message 003) | shipped | 407 lines: lenses `:19`, levels and column types `:54-78`, window `:81`, width table `:113-150`, gap strip `:159`, Up / breadcrumb / Back `:169`, addresses `:178`, keys `:226`, phone `:256`, side columns `:269`, texts `:302`. The width and chat tables were recomputed by the leaf: consistent |
| `components.adoc` column templates | shipped | `:354-377`: `TriagePage` is the column engine, `ObjectPage` the object column |
| `README.adoc` shell, bars, widths, routes | shipped | `:19-20`, `:139`, `:142`, `:145`, `:186`, `:243-273`, `:511-573` |
| `view-density-and-filtering.adoc` | shipped | layout modes, breakpoint and rollback removed; the Projects filter, live region and one key pattern added |
| web parts of the other documents; PLAN-83 folded | shipped | all 15 files map to a deliverable |
| The operator's decisions (width table, gap strip, `[`/`]`, chat 480/400, breadcrumb = Up; `?node=`, pinned found row, workflow paths, `/` vs `Ctrl+K`) | shipped | `nav:116-150`, `:159`, `:243`, `:273-300`, `:203`, `needs-and-find:58`, `nav:215` |

All 13 changed design documents read `Status: AGREED` at `95df75bb`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`), documents only.
- **Verdict:** layer 1 sound with 3 seams; layer 2 complete; layer 3 has 1 declared stale document and 8 minor seams.
- **Re-read by the orchestrator at `95df75bb`:** `epic-view.adoc:47`; `README.adoc:542,548`; `navigation.adoc:111,200,213`; `plan-view-interaction.adoc` (4 × "right panel"); `README.adoc` (573 lines). All confirmed.

### Seams

| Id | File:line | Seam | Severity |
|----|-----------|------|----------|
| S1 | `navigation.adoc:111` | the window rule has no case for n = 1: at 768 px it yields no column, or one column with a strip, while `:126-131,:165` say one column has no strip | minor |
| S2 | `navigation.adoc:60` vs `:116` | the chain is six items in the diagram and five in the width table | minor |
| S3 | `navigation.adoc:213` vs `web-app/needs-and-find.adoc:21`, `README.adoc:57,526` | the remembered place vs `/` (What waits) is unsettled | minor (needs a decision) |
| S4 | `plan-view-interaction.adoc:15-106` | still the two-panel / right-panel layout, linked from an AGREED row (`README:88`); declared and left for PLAN-55 / PLAN-62 | major (declared) |
| S5 | `shared/epic-view.adoc:47` | the epic head still draws `project › epic` as a second path | minor |
| S6 | `README.adoc:542` vs `:548` | "no workflow lies below /projects/" vs `/projects/{id}/start`; `/epics/{id}/decomposition`, `/entries/{id}` | minor |
| S7 | `navigation.adoc:200`, `completion-report.adoc:104` | `?open=completion-report` lacks the `{kind}:{id}` shape; the Reports breadcrumb leaves the lens | minor |
| S8 | `shared/decisions.adoc:132`, `work-actions.adoc:382` | the chat wording is stale ("panel", the decision stays visible, "End the chat returns") vs `nav:166,274-285` | minor |
| S9 | `plan-overview.adoc:55` vs `nav:78` | a project whose work ends moves the selection to a neighbour vs the pinned row | minor |
| S10 | `README.adoc:493` | the cleanup sketch shows a second path | minor |
| S11 | `README.adoc:124`; `view-density:63` | the Texts row omits `navigation.adoc#texts`; `common.filter.results/.clear/.empty` are in no catalogue table | minor |
| C1 | `web-app/README.adoc` | 573 lines vs the 500-line convention (`doc/design/README.adoc:17`); was 562 | conformance |
| C2 | `doc/plans/README.adoc:369`, `web-moving-between-plans.adoc:11` | the dependency is not named by package id; a "Done when" line sits in the package file (`project-setup.adoc:93,95`) | conformance |

## Routing and Merge Behavior

- **Review:** CodeRabbit, 1 body (6 actionable). Threads: 6, all replied, 5 resolved, 1 answered without a change. Sourcery skipped (budget). The merge rule is met.
- **Decisions:** the operator accepted the four review decisions on 2026-11-08. The AGREED commit `467fa02f` was labelled `skip-bot-review`.
- **CI and merge:** green; merged through the merge queue on the operator's word as `95df75bb`.
- **Cleanup:** the worktree (with the untracked prototype `pm-web-app/src/routes/dev/columns/`) and the branch were removed. The three sibling-repo symlinks in `.plan/local/worktrees/` are kept until D3 finishes.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 380; `landing` `landings/PLAN-113.md`; `plan_marshall_plan_id` `plan-113`
- [x] epic.md Watch; D3 told it can finish #379, with the anchors for its README/components edits
- [x] seams reported to the operator 2026-11-08

## Follow-Ups

- S1–S11, C1 and C2: reported to the operator (see the decision in `epic.md`).
- For D3 (anchors at `95df75bb`): `README.adoc:23,117-118,129-147` (the rail cell `:139` holds head and foot in one line), `:160-187`, `:355`, `:379`, `:423`, `:452`; `components.adoc:13`, `:34-67` (vocabulary), `:432-439` (`EmptyState`, the `brand` prop at `:439`), `:459-466` (supporting), `:468-479` (new-component procedure). `components.adoc` has 479 lines, so `MascotFigure` / `WaitingMark` likely need a part of their own. `README.adoc` is already 573 lines, so D3 should move material out, not add to it.
