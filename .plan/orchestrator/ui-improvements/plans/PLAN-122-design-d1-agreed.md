# PLAN-122: Design D1: Set the D1 Documents to AGREED

epic: ui-improvements
workstream: WS-06

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane (a small, documents-only plan).
> Staged 2026-11-08 on the operator's decision: the operator reviewed the four decisions of D1 and accepted each as
> written. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The D1 design session (PLAN-112, PR #365, `aa3a9be3`) merged its documents at `Status: DRAFT`. The design-review process
of this epic says the operator sets `AGREED` as the last act of a design session. D2 (PLAN-113) and D3 (PLAN-114), and
everything behind them, wait for D1 at `AGREED`.

On 2026-11-08 the operator reviewed and accepted the four decisions D1 put up for review:

1. **Kind tints.** Project olive, Epic magenta, Plan teal, with the measured rule. See `components-tags.adoc`
   § The Three Kind Tints.
2. **`RowMenu` keeps a disabled entry with its `disabledReason`.** See `components.adoc` § RowMenu.
3. **Stacking layers become tokens:** `--layer-sticky` 1, `--layer-bar` 20, `--layer-menu` 30, `--layer-toast` 90,
   `--layer-dialog` 100. See `README.adoc`.
4. **A project's owed credential or setup-only provider check shows the reason `setup`.** `credential` names a
   machine-wide credential. See `project-explorer.adoc:70` and `needs-and-find.adoc:39`.

This plan records that acceptance in the documents. It changes no content.

## Deliverables

1. **The D1 documents read `Status: AGREED`**
 - What:
   - In each document below, change `Status: \`DRAFT\`` to `Status: \`AGREED\``. The line number is the one at
     `ef7d2389`.
   - Before each edit, confirm with `git log -S 'Status: \`DRAFT\`' -- {file}` that the DRAFT line was introduced or
     kept by #365. A document whose DRAFT line comes from another, unmerged design session is reported and left at
     DRAFT.
   - Change nothing else. Each document's status sentence, if it has one, may say "agreed by the operator on
     2026-11-08".

   The documents:
   - `doc/design/shared/needs-and-find.adoc:12`
   - `doc/design/shared/plan-overview.adoc:12`
   - `doc/design/shared/project-explorer.adoc:12`
   - `doc/design/shared/texts-and-languages.adoc:14`
   - `doc/design/shared/work-actions-review-texts.adoc:10`
   - `doc/design/shared/work-actions-texts.adoc:10`
   - `doc/design/shared/work-actions.adoc:12`
   - `doc/design/terminal-client/04-console.adoc:13`
   - `doc/design/terminal-client/17-projects.adoc:13`
   - `doc/design/terminal-client/19-actions.adoc:15`
   - `doc/design/terminal-client/22-texts-actions.adoc:15`
   - `doc/design/web-app/README.adoc:12`
   - `doc/design/web-app/components-fields.adoc:15`
   - `doc/design/web-app/components-tags.adoc:15`
   - `doc/design/web-app/components.adoc:15`
   - `doc/design/web-app/interaction-affordances.adoc:12`
   - `doc/design/web-app/workflow-visual-language.adoc:12`

   `doc/design/shared/intent-and-outline.adoc` and `doc/design/web-app/plan-view-interaction.adoc` are also DRAFT, but
   they are not D1 documents. They stay at DRAFT.
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc
 - Done when:
   - `git grep -n 'Status: \`DRAFT\`' -- doc/design` lists none of the files above, except those reported as not D1.
     Evidence order: `none`.
   - `git diff --stat` against the base shows one changed line per file, plus the status sentence where present.
     Evidence order: `none`.
   - `asciidoc validate` is clean on every changed file. Evidence order: `none`.

## Standing Constraints

- Documents only: no code, no test, no build change. The project's CI runs on the pull request as usual.
- No content change. A finding a reviewer raises about the content is answered with "D1 content was accepted by the
  operator on 2026-11-08; a change needs a new design act". The finding goes into the pull request body as a residue
  item.
- Write-set discipline: the main checkout is never edited. Work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word. Every review body is answered where the reviewer reads it.
  Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator
  footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`). The open-issue count
  at the head is recorded: `0` confirmed, or every remaining issue dismissed with its reason.

## Claim Labels

- OBSERVED: the 17 documents above carry `Status: \`DRAFT\`` at the named lines, and each was changed by #365
  (`git show --stat aa3a9be3`). Read at `ef7d2389` by the orchestrator on 2026-11-08.
- OBSERVED: the operator accepted the four D1 decisions, one by one, on 2026-11-08 (AskUserQuestion; `epic.md`
  Decisions).
- Verify-first clause: re-read each Status line at the current HEAD before editing. Running and staged plans touch some
  of these files, but none edits a Status line.

## Expected Surface

- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/shared/project-explorer.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/shared/work-actions-review-texts.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/shared/work-actions-texts.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/shared/work-actions.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/terminal-client/17-projects.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/terminal-client/22-texts-actions.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/web-app/README.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/web-app/components-fields.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/web-app/components-tags.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/web-app/components.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/web-app/interaction-affordances.adoc` — present at `ef7d2389`
- OBSERVED: `doc/design/web-app/workflow-visual-language.adoc` — present at `ef7d2389`

## Dependencies and Sequencing

- Depends on: `PLAN-112` (shipped, #365)
- Unblocks: `PLAN-113` (D2) and `PLAN-114` (D3). D3's pull request merges after D2's. `PLAN-115` (D4) follows both.
- Overlaps with: a Status line in documents that `PLAN-34`, `PLAN-120`, `PLAN-121` and `PLAN-84` also edit. It is a
  one-line change per file, so whichever starts second rebases trivially.

## Hand-Off Command

Lane: `runbook`, a small documents-only plan.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-122-design-d1-agreed.md
```

## Write-Boundary

The session implementing this spec touches only its own repository's documents. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write.
The session reports its outcome through its PR and its inbox message. The qualifiers of the inbox exception, and the
only sanctioned write mechanism, are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger
Write-Boundary.
