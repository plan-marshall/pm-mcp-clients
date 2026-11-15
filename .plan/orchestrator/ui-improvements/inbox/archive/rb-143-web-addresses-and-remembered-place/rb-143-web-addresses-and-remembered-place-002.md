envelope_version=1
sender_type=plan
sender_id=rb-143-web-addresses-and-remembered-place
epic=ui-improvements
kind=landing
created=2026-11-15T19:19:16Z

# PLAN-143 follow-up — leftovers after the merge attempt

PR #446 (`feature/143-web-addresses-and-remembered-place`) is **open and NOT merged**. The first
landing message (`rb-143-web-addresses-and-remembered-place-001`) was consumed, so this is the
follow-up the runbook lane asks for. Head at writing: `10930a04`.

## Why it did not merge

- **`mergeable: conflicting`** — the branch now conflicts with `main`; auto-merge is armed
  (`auto_merge_armed: true`) but no merge-queue entry exists, so it will never settle until the
  conflict is resolved by merging `origin/main` into the branch.
- **Sonar is stale on the head**: the analysis still names `f65e1341`; `f65e1341` was **0 confirmed**
  new-code issues, and that is the last measured truth.
- **The review gate cannot read CodeRabbit's body**: `undecidable_read` on the counted title
  `Minor comments: 22`. Unresolved threads: **0** (all five inline threads were replied to and
  resolved at the operator's instruction).
- CI on `10930a04`: the checks the client could read are green; CodeRabbit reviewed it (SUCCESS).

## What the run fixed (commits on the branch)

`72805897` D1 · `cd432a3e` D2 · `505f9b2c` D3 · `92c28ec5` D4 · `0eee0444` D5 · `5b60b8e4`,
`5b1d6c28`, `f65e1341`, `10930a04` review rounds.

The last two rounds closed: the Sonar findings of the new code (10 → 0), the start mark read before
the pairing fragment is dropped, the fragment dropped at `/`, the rail's destination mapping for the
new routes, a project link opening the one project page, **the root fill of the remembered place**
(the blocker), **the report detail's own address** (route + report id in the store), **the task
route**, the overview writing its scope, grouping and mode into the address, and `isFillableStart`
rejecting an address that opens an object or the chat.

Local evidence on `10930a04`: `verify -Ppre-commit -pl pm-web-app` green;
`verify -Pe2e -pl pm-web-e2e` green, 414 passed on ports 21430/21431
(`maven-2026-11-15-180217.log`).

## Leftovers for the next session, in order

1. **The conflict**: merge `origin/main` into the branch and resolve it (ReportsView, OverviewView,
   routes.ts and the layout are the likely collision points; another plan landed in the same views).
   Then re-run the gate and the e2e suite.
2. **Sonar re-analysis** of the resolved head, then a fresh `read_open_issues` — the count must be 0
   confirmed, not `undecidable`/stale.
3. **The review gate's `undecidable_read`**: the lane's parser does not know CodeRabbit's
   `Minor comments: 22` title. Either the parser learns that label or the body must be re-read by
   hand; until then the gate can never report `merge_allowed: true` for this PR.
4. `factsOf` opens a remembered plan without its own liveness (the projects summary carries no
   per-plan state) — needs a per-plan read at start.
5. A report address that points to a report outside the first list page is not resolved — needs a
   report read by id.
6. `/decomposition` in a fresh session: `EpicDecompositionView` needs an epic selection; the
   standalone entry belongs to the decomposition plan.
7. `fallBackFromDeletedObject` still has no caller (the deleted-object fallback of D2 does not run).
8. The 22 minor and 2 nitpick findings of CodeRabbit, including the two weak tests
   (`history.test.ts` traversal and the `file=` transition, the latter blocked on the plan's tab
   control which PLAN-55/PLAN-58 own).
9. `/projects/onboard` remains ambiguous with the onboarding (PLAN-59 owns it).

## Done-when verdicts

Unchanged from the first landing: **0 of 5 deliverables met as written**. The address model, the
writer, the remembered place, the views and the e2e spec are implemented; the Done-whens as written
are not all satisfied, and the items above are the gap.
