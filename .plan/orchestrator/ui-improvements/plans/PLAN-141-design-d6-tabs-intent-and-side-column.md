# PLAN-141: Design D6: Plan and Epic Tabs, the Intent Design and the Chat's Side Column Before the Engine

epic: ui-improvements
workstream: WS-06

> Staged plan spec, one short design session handed off as a direct Claude Code session (like D2 and D5; not the runbook lane,
> not `/plan-marshall`). Staged on the operator's decision of 2026-11-10 from the re-grounding of the parked web plans. This
> spec is SELF-SUFFICIENT: the hand-off prompt is a one-line pointer. Where this spec and an AGREED document differ, the AGREED
> document stands until the operator decides otherwise in this session. The queue row's `lane` field (`plan-marshall`) means
> nothing for a design session.

## Objective

D1 to D5 are AGREED and merged. Re-grounding the parked web plans at `1cc0e10e` found what they leave open, and the
parked plans cannot restart until it is decided:

1. The agreed designs know only the generic `tab={id}`. They name no tab set for a plan or an epic (PLAN-55 asks for Process,
   Intent, Activity, Tokens, Settings; PLAN-58 for Workstreams, Intent, Landings, Findings and watches, Tokens, Events).
2. `web-app/plan-view-interaction.adoc` is still `DRAFT`, still draws the two-panel tree and narrative with a path in the
   head, and contradicts `navigation.adoc#levels` (the phase tree is the plan's list column).
3. `shared/intent-and-outline.adoc` is `DRAFT`, has no texts, no verdict tag in the closed tag set, no link from the plan, the
   epic or the README, and no home for the `over_size` chart.
4. The web chat's side column is specified against a column engine that is not built.
5. Two rules need a design line: the rail item against the remembered place (PLAN-52), and what the remembered place holds.
6. Theme and language placement: README option A stays (rail foot and More sheet), and three documents still presuppose a
   "This browser" tab that holds them.

D6 decides these with the operator, one question at a time, writes them into the documents, and brings
`plan-view-interaction.adoc` and `intent-and-outline.adoc` to the state the operator can set `AGREED`. Documents only.

## Sources (read in full before scoping)

Resolve the epic tree with `python3 .plan/execute-script.py plan-marshall:plan-orchestrator:orchestrator resolve-path --slug
ui-improvements`; read, never write.

- `epic.md` § Decisions: the entries 2026-11-06 (5) (the D2 navigation engine, PLAN-83 superseded), 2026-11-08 (the D2 seams
  decision: an explicit click on a lens opens that lens, `/` is What waits, only a bare app start or `/overview` is filled from
  the remembered place), 2026-11-09 (D4 review), 2026-11-10; `landings/PLAN-113.md` (seams S3, S4, S9); `landings/PLAN-125.md`.
- The `## Re-grounding 2026-11-10` amendments of the parked plans PLAN-52, 55, 58, 59, 60, 62, 85 and the staged chat plans
  PLAN-135..137 (their `Depends on` and Verify-first lines).
- This repository: `doc/design/web-app/navigation.adoc` (all), `web-app/README.adoc` (#shell, #themes, #routes),
  `web-app/components.adoc` (#hierarchy, #fixed-places, #templates, #object-page, #new-component), `web-app/components-tags.adoc`
  (#tag-set, #status-label, #chip), `web-app/plan-view-interaction.adoc`, `web-app/chat.adoc` (#placement, #status-cards, #rely),
  `web-app/components-chat.adoc`, `web-app/machine-settings.adoc`, `shared/plan-view.adoc`, `shared/epic-view.adoc`,
  `shared/intent-and-outline.adoc`, `shared/plan-overview.adoc` (#persistence), `terminal-client/16-plan-view.adoc`.
- The code as it is: `pm-web-app/src/lib/templates/*`, `lib/views/PlanView.svelte`, `EpicView.svelte`, `OverviewView.svelte`
  (storage keys `pm_overview_*`), `pm-api/.../api/intent/*`, the mock's intent routes (`MockRuntimeEngine.java:5386`).
- Sibling repositories, read only: `../pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc#intent-record`
  and `store-schemas/06-task-and-references.adoc#outline-record-store` (what the intent and outline records carry).

## Deliverables

1. **Current state and options** (no repository file)
 - What: for each of the seven questions below, the current state with file:line, the gaps and contradictions, two or three
   options with consequences, and a recommendation. Saved as `.plan/temp/d6-findings.md` in the session's worktree and
   **put to the operator one question at a time, waiting for each answer**; every answer is recorded with its date in the
   findings file and in the document it changes.
 - The questions (starting recommendations are the orchestrator's; the session verifies and may change them):
   - **Q1 The plan page in the column model, and its tabs.**
     - Current state: `plan-view.adoc#levels` (:16-47) draws a tree and no tabs. `navigation.adoc#levels` (:64-66) says the
       phase tree is the plan's list of children and `#url` (:203) selects a node by `node={path}`. A task is a level of its own.
       `components.adoc:333` makes the plan's settings a dialogue, _Plan settings…_. `ObjectPage` has a `tabs` slot and no
       production view uses it. `plan-view-interaction.adoc#defaults` selects the running task or node on every load.
     - The gap: with a node always selected, is the plan a list column with the narrative as its child, or an object column with
       tabs? Where do the Intent and the tokens breakdown stand?
     - Options: (A) the plan is the object column with tabs; the Process tab holds the tree with the node narrative beside it
       inside the tab; opening a task makes the plan a list column whose list is the tree. (B) the tree is always the list
       column and the narrative the next column, with Intent and Tokens as head links. (C) tabs Process, Intent, Activity,
       Tokens as PLAN-55 asks (Activity has no design: the worker feed is the task level).
     - Starting recommendation: (A), tabs **Process** (default) and **Intent**; the tokens breakdown stays the one-step
       disclosure of the head line (`plan-view.adoc:169`); no Activity tab and no Settings tab; an ended plan has the same tabs
       read-only.
   - **Q2 The epic page's tabs.**
     - Current state: `epic-view.adoc#layout` (:16-54) lists the workstreams with their plans, sources, landings, findings,
       watches, tokens and events as blocks. A workstream groups rows and is no level (`navigation.adoc:66`).
     - Options: (A) PLAN-58's six tabs with "Workstreams" renamed **Plans**: Plans (default, sources line), Intent, Landings,
       Reviews (findings and watches), Tokens, Events. (B) Plans and Intent tabs only, the rest as lines of the Plans tab.
       (C) Plans, Intent, Activity (landings, reviews, events), Tokens.
     - Starting recommendation: (A). The session also decides where Sources stand, and states that the epic as a list
       column shows the plans grouped by workstream.
   - **Q3 `plan-view-interaction.adoc` in the column model.**
     - Current state: DRAFT (:12). The two-panel rule (:14-32) with a `project › epic › plan` header, "right panel" four times,
       the three acts (:34-67), defaults (:69-80), the narrative (:82-99), the empty states (:101-122), untrusted text (:124-131).
     - Do: rewrite the layout text for Q1's answer. Keep the selection semantics, the defaults, the narrative content, the
       empty states and the untrusted-text rule; drop the header path (the breadcrumb is the only path). Then the document is
       ready for the operator's `AGREED`.
   - **Q4 The intent design.**
     - Current state: `intent-and-outline.adoc` is DRAFT (:12). It has no texts section, though both clients show words such as
       _met_, _you decided_, _the model's call_ and _needs you_. It says "a verdict chip per criterion" (:276) but the closed
       tag set (`components-tags.adoc#tag-set`) has no verdict tag. `plan-view.adoc`, `epic-view.adoc` and `web-app/README.adoc`
       do not mention it. The `over_size` chart is "in the role's settings" (:276), but the route is project-scoped
       (`/api/v1/projects/{id}/metrics/oversize`, resource fields `project_id` and `role`) and `machine-settings.adoc` has no
       role-multiple line.
     - Contract against design: the contract's decider is `operator|quorum|model|fact` against the design's `role`; the
       contract's `non_goals` are plain strings against `NG-n` ids; a decision carries no ADR relation, no evidence and no vote
       support (`PlanIntentResource.Decision`: id, text, decider, options_weighed, question_id); the builder check's `not_run`
       carries no reason field.
     - Decide: (a) the verdict tag: `StatusLabel` states `state.criterion.*` and `state.check.*` (recommended), a neutral
       `Chip`, or a new component through `components.adoc#new-component`. (b) The texts: a new
       `shared/intent-and-outline-texts.adoc` in the house style of `decisions-texts.adoc`, catalogue area `intent`, English and
       German, both clients. (c) The links: the plan's Intent tab (Q1), the epic's tab (Q2), the decomposition review, a
       `web-app/README.adoc` row (the file is at 499 lines: do not grow it, move words out). (d) The home of the chart: the
       project page's setup tab, the machine settings' roles aspect, or both; recommended: the project page setup tab, because
       the route is per project and role. (e) The contract deltas: the web "leaves out a field the record does not carry"
       (`intent-and-outline.adoc:19`), so the recommendation is that the design says so for ADR relation, evidence and vote
       support and the contract grows only when the engine's record carries them (confirm at pm-mcp-core
       `phase-workflows/01-planning-phases-1-2.adoc#intent-record`); the design uses the wire word `model` and numbers
       non-goals `NG-1…` by position.
   - **Q5 The chat's side column before the engine.**
     - Current state: `navigation.adoc#side-columns` (:269-301) and `components.adoc#triage-page` (:363-374) put the side column
       into `TriagePage` and `ObjectPage`; `WizardPage` hosts it (`chat.adoc:40`). In code `TriagePage` has the slots
       `filters`, `list`, `detail` and no `side`; `ObjectPage` and `WizardPage` have no side slot and no production view uses
       either. The staged web chat plan builds the slot as its own deliverable "against today's single object column".
     - Options: (A) the chat plan builds an interim slot on `ObjectPage` and `WizardPage`, and the engine moves it later.
       (B) The slot exists only in the engine: the column engine part 1 (`PLAN-142`) delivers `side`, `sideOpen` and
       `onHideSide` on all three templates, and the chat plans wait for it. (C) as (B), with the template API fixed here so the
       chat plan could build against it.
     - Starting recommendation: (B)/(C). Both packages wait for the D1 foundation anyway; an interim slot is built twice.
     - Also decide: the 480/400 px rule measured on the container; the phone takes the bottom bar; and the `open=` forms of
       status cards (`navigation.adoc:277`, `chat.adoc:164`), which need the address model (`PLAN-143`): until it lands, the
       pushed-address form is the only form.
   - **Q6 The rail item, the remembered place and the lens segment.**
     - Current state: `plan-overview.adoc:164` remembers the project, the view (by plan, by epic) and the selected plan or epic;
       it does not say the mode is remembered. `OverviewView.svelte:191-215` restores the ended section from
       `pm_overview_show_ended`. `navigation.adoc:213` and `plan-overview.adoc:171` fill "a bare start of the app" and a bare
       `/overview` by `replaceState`. PLAN-52 D3 says "the rail item opens running work after ended work was open".
     - Two defects to settle:
       - A bare `/overview` is always filled, so Up and the breadcrumb's lens segment (`navigation.adoc:172-173`: segments
         navigate as Up) can never reach the unselected lens list; Up from a project loops back to the plan.
       - A reload of `/` is indistinguishable from a bare start.
     - Starting recommendation:
       - The remembered place holds project, grouping and the selected plan or epic, never the mode; the rail item therefore
         opens running work, which satisfies PLAN-52's clause.
       - The fill applies only at the entry points: the app opened without a marker in `history.state` (a bare start, also from a
         bookmark) and a click on the rail's lens entry.
       - Up and the breadcrumb's lens segment push the unfilled lens address, marked in `history.state`, which a reload keeps.
       - An ended plan falls back to its project; a project without running work falls back to the project list.
   - **Q7 The theme and language residue.**
     - Current state: README option A stands (`README.adoc:147` rail foot, `:153` and `:431` More sheet). `README.adoc:492`
       (the `browser` tab "This browser", PLAN-85), `machine-settings.adoc:76` ("beside its theme and language") and
       `chat.adoc:239` presuppose a tab that holds them.
     - Do: reword so that the "This browser" tab holds what only a browser chooses (the dictation choice) and says theme and
       language stay in the rail foot and the More sheet. Record that a browser before pairing has no rail, so its theme follows
       the system until paired. The amendments owed to PLAN-85, 66, 60, 29 and 41 are listed in the report.
2. **The plan documents**
 - What: write the answers of Q1 and Q3 into `shared/plan-view.adoc` (the plan's tabs, its head link to the Intent tab, the
   layout in the column model) and rewrite `web-app/plan-view-interaction.adoc` for the column model, Status `DRAFT` in the pull
   request. Keep `navigation.adoc#rely` consistent.
 - Done when:
   - `ref-documentation` review and `ref-asciidoc` `validate` and `verify-links` are clean on the changed documents. Evidence
     order: `none`.
   - No remaining "right panel" or header path in `plan-view-interaction.adoc`; every tab named appears in
     `navigation.adoc#rely` for PLAN-55 and PLAN-62. Evidence order: `none`.
3. **The epic and intent documents**
 - What: write Q2 into `shared/epic-view.adoc`. Write Q4 into `shared/intent-and-outline.adoc` (the links, the verdict tag, the
   home of the chart, the left-out fields, `NG-n` by position), create `shared/intent-and-outline-texts.adoc` (every key and
   word, English and German), add the verdict states to `web-app/components-tags.adoc` if the answer is `StatusLabel`, and add
   the row to the structure table of `web-app/README.adoc` without growing the file past 500 lines.
 - Done when:
   - Every word the intent design uses has a key in the texts document and English and German agree in count (checked against
     `shared/decisions-texts.adoc` for the house shape). Evidence order: `none`.
   - `web-app/README.adoc` stays at or under 500 lines (`wc -l`). Evidence order: `none`.
4. **The navigation seams**
 - What: write Q5, Q6 and Q7 into `web-app/navigation.adoc` (#side-columns, #url, #rely), `shared/plan-overview.adoc#persistence`,
   `web-app/chat.adoc` (#placement, #status-cards, #rely, :239), `web-app/machine-settings.adoc:76` and `web-app/README.adoc:492`
   (reword in place; do not grow the file). Open points go to
   `../plan-marshall-documentation/doc/discussions/open-questions.adoc`.
 - Done when:
   - The same checks as deliverable 2, and `chat.adoc#rely` and the chat plans' dependency lines agree with the Q5 answer.
     Evidence order: `none`.
5. **Review and pull request**
 - What: one pull request in this repository (plus one in `plan-marshall-documentation` only if an open point is filed there).
   Every changed design document is `Status: DRAFT`; the operator sets `AGREED` as the last commit and gives the merge word.
   **No auto-merge.** Stop once the pull request is green and its review comments (threads and bodies) are answered, write one
   inbox message, and report: the seven decisions with dates, the documents changed, the amendments owed to the parked plans,
   and the questions still open.
 - Done when:
   - `ci pr comments` reads no unanswered thread or review body. Evidence order: `none`.

## Standing Constraints

- Design only: no production code, test or catalogue change. The engine (`PLAN-142..144`), the intent web packages
  (`PLAN-145`, `PLAN-146`) and the chat packages are staged by the orchestrator after the merge.
- Each question is put to the operator **one at a time**; do not write a document for a question that is not answered.
- Edit only the lines you own. D6 does not rewrite `navigation.adoc` beyond Q5 and Q6, does not touch the D1 tag, field or
  interaction rules except the verdict states of Q4(a), and does not touch the brand documents.
- The 500-line rule (`doc/design/README.adoc`): `web-app/README.adoc` is at 499 lines.
- Review with `pm-documents:ref-documentation` plus `pm-documents:ref-asciidoc` (`validate`, `verify-links`).
- No dependency or plugin is added. Work only in the session's own worktree and branch; the main checkout is never edited.
- The merge rule: some head has had one CodeRabbit review; every thread replied and resolved; every review body answered. If
  CodeRabbit refuses on quota, stop and ask the operator. Merge only on the operator's word. Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): state `sonar_open_new_issues=N` in the landing message.

## Amendment 2026-11-10 (session 3): PR #400 lands first

The operator decided (2026-11-10) that PLAN-141 starts only after PR #400 (`docs/zed-acp-ingestion`, "a plan's changes in
both clients; a different model in the activity") has merged, and from the `main` that contains it. #400 adds to two of this
plan's documents:

- `shared/plan-view.adoc`: a new section `[#changes]` _The Changes_ (the file list and hunks of the plan's last capture, read
  only, untrusted), opened from the _Change So Far_ size line; the activity entry `model_differs`; the keys `plan.changes.*`
  and `plan.activity.model_differs`.
- `web-app/plan-view-interaction.adoc`: a section `[#changes]` that opens the changes "in the narrative panel" from "the size
  line of the header", with the address `/plans/{plan_id}?changes=open&file=<path>`.
- Also `terminal-client/16-plan-view.adoc` (key `d`, `plan <plan_id> --changes`) and the work package `doc/plans/plan-changes.adoc`.

What this changes for the session:

- **Q1** gains a question: is _Changes_ a tab of the plan (beside Process and Intent), or stays it the one-step disclosure of
  the size line like the tokens? Put it as part of Q1 with options and a recommendation.
- **Q3** rewrites `plan-view-interaction.adoc#changes` for the column model with the rest (no "narrative panel", no header),
  and states how `changes=open&file=` relates to `tab=` and `node=` in `navigation.adoc#url`.
- Re-read both documents at the HEAD the worktree starts from; the line numbers cited above were read at `1cc0e10e` and may
  have moved. The work package `plan-changes` is client work that follows D6's answer; name the amendment it owes in the
  report.

## Claim Labels

- OBSERVED: `plan-view-interaction.adoc` and `intent-and-outline.adoc` are `Status: DRAFT` — read at `doc/design/web-app/plan-view-interaction.adoc:12`, `doc/design/shared/intent-and-outline.adoc:12`
- OBSERVED: `plan-view-interaction.adoc` still draws the two-panel layout with a `project › epic › plan` header — read at `doc/design/web-app/plan-view-interaction.adoc:14-32`
- OBSERVED: `navigation.adoc` makes the phase tree the plan's list of children and selects a node by `node={path}`, a task being a level — read at `doc/design/web-app/navigation.adoc:64-66,203`
- OBSERVED: no tab set of a plan or an epic is designed; `components.adoc` makes the plan's settings a dialogue — read at `doc/design/web-app/components.adoc:333,376-381`
- OBSERVED: `intent-and-outline.adoc` has no texts section and no link to it exists in `plan-view.adoc`, `epic-view.adoc` or `web-app/README.adoc` — read by `git grep -n -i intent -- doc/design/shared/plan-view.adoc doc/design/shared/epic-view.adoc doc/design/web-app/README.adoc` (empty)
- OBSERVED: the closed tag set has no verdict tag — read at `doc/design/web-app/components-tags.adoc#tag-set`
- OBSERVED: the `over_size` route is project-scoped and carries `role` — read at `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java:50-54`, `MockRuntimeEngine.java:5412-5418`
- OBSERVED: the contract's deciders are `operator|quorum|model|fact`, a decision has no ADR relation, evidence or vote support, and `non_goals` are strings — read at `pm-api/src/main/java/de/planmarshall/api/intent/IntentDecider.java:26-35`, `PlanIntentResource.java:60-97`
- OBSERVED: `TriagePage` has no `side` prop; `ObjectPage` and `WizardPage` have no side slot and no production view uses either — read at `pm-web-app/src/lib/templates/*.svelte`; `git grep -n "ObjectPage\|WizardPage" -- pm-web-app/src/lib pm-web-app/src/routes` finds fixtures and tests only
- OBSERVED: `web-app/README.adoc` has 499 lines — read by `wc -l`
- OBSERVED: `OverviewView.svelte` restores the ended section from `pm_overview_show_ended` — read at `pm-web-app/src/lib/views/OverviewView.svelte:191-215`
- OBSERVED: `README.adoc:492`, `machine-settings.adoc:76` and `chat.adoc:239` presuppose a "This browser" tab — read at those lines
- HYPOTHESIS: the engine's intent record carries no ADR relation, evidence or vote support, so "left out" is correct — confirm/refute at `../pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc` § `intent-record` (verify-at-outline)
- HYPOTHESIS: no reload-versus-bare-start distinction exists today; `syncUrl` writes `{ path }` into `history.state` for every entry — confirm/refute at `pm-web-app/src/lib/stores/runtime.ts` § `syncUrl` (verify-at-outline)
- Verify-first clause: re-read each cited range at the current HEAD before writing the rule it supports. Also re-read the amendments of the parked plans: they may have been changed since.

## Expected Surface

- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/plan-view-interaction.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/epic-view.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/intent-and-outline-texts.adoc` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/design/web-app/navigation.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/chat.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/components-tags.adoc` — present at HEAD 1cc0e10e (changed only if Q4(a) adds states)
- OBSERVED: `doc/design/web-app/machine-settings.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 1cc0e10e (499 lines; must not grow)
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at HEAD 1cc0e10e (house shape of a texts document)

## Dependencies and Sequencing

- Depends on: PR #400 merged (operator, 2026-11-10; see the Amendment); D1 to D5 merged and AGREED.
- Blocks: `PLAN-145`, `PLAN-146`; the tab placement of PLAN-55, 58 and 62; PLAN-135..137 (the Q5 ruling); the rules `PLAN-143` and `PLAN-144` implement (Q6); the text amendments of PLAN-85, 66, 60, 29, 41 (Q7).
- Overlaps with: none running; no running plan edits these documents.
- Adjacent to: `PLAN-142..144` (they implement what Q5 and Q6 decide; the specs carry a verify-first clause on the D6 documents).

## Hand-Off Command

Lane: none. A direct Claude Code session in its own worktree and branch of this repository. Run `claude` there, then give it exactly this prompt (the orchestrator prints the absolute spec path at emit time):

```text
Implement the staged design plan PLAN-141 (design D6) of the epic ui-improvements. Read the whole spec first: .plan/orchestrator/ui-improvements/plans/PLAN-141-design-d6-tabs-intent-and-side-column.md (in the shared ledger worktree `.plan/local/worktrees/_orchestrator/`). Then read every source named under "Sources" in full. Follow the spec exactly: documents only, Status: DRAFT, put the seven questions to the operator one at a time and wait for each answer, write only the answered ones, keep web-app/README.adoc at 500 lines or fewer, ref-documentation docs review plus ref-asciidoc validate and verify-links, one pull request, no auto-merge, stop when the pull request is green and its review comments are answered, and report. Do not edit anything under .plan/orchestrator/ except your own inbox message (orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session writes only in its own worktree and its own inbox message (`orchestrator inbox write`); nothing else under `.plan/orchestrator/`. It does not merge. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
