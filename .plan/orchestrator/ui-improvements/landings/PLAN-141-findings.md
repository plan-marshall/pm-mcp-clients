# D6 findings (PLAN-141): current state, options, recommendation

Worktree HEAD `d6896a8f` (contains #400). Line numbers read at that HEAD. Answers are recorded per question with their date.

## Verified spec claims and corrections

- OBSERVED, holds: `plan-view-interaction.adoc:12` and `intent-and-outline.adoc:12` are `DRAFT`; the two-panel rule at
  `plan-view-interaction.adoc:14-32`; `navigation.adoc:64-66,203`; `components.adoc:333`; no verdict tag in
  `components-tags.adoc:17-60`; `README.adoc` 499 lines; `OverviewView.svelte:191-215` restores `pm_overview_show_ended`.
- OBSERVED, holds: `TriagePage.svelte` has `filters`, `list`, `detail` and no side; `ObjectPage.svelte` has a `tabs` slot
  (`:43`) and the `path` slot (`:19`), no side; `WizardPage.svelte` has `steps`, body, `back`, `secondary`, `primary`, no
  side. The only production user of a template is `ReportsView.svelte:855` (`TriagePage`).
- Moved line: the tokens disclosure is `plan-view.adoc:192` (spec cited `:169`). #400 added `plan-view.adoc:156-180`
  (`#change-size`, `#changes`) and `plan-view-interaction.adoc:124-127` (`#changes`, address
  `/plans/{plan_id}?changes=open&file=<path>`, "in the narrative panel", "the size line of the header").
- HYPOTHESIS `syncUrl` CONFIRMED: `runtime.ts:906-917` writes `{ path }` into `history.state` for every push and replace;
  nothing reads `history.state` to tell a fresh open (state `null`) from a reload of an entry the app wrote. A marker is
  therefore feasible without a new storage.
- HYPOTHESIS "the engine's intent record carries no ADR relation, evidence or vote support" REFUTED in part:
  `pm-mcp-core/pm-core/.../intent-record-v1.schema.json` `choices[]` carries `adr {adr_id, relation: follows|contradicts}`
  and `evidence` (a path or `pm://` URI for decider `fact`); `non_goals[]` are items `{id: NG-n, text}`; the decider enum is
  `operator|quorum|role|fact`. Vote support is NOT in the record. The builder check's `not_run` is recorded "with its
  reason" (`01-planning-phases-1-2.adoc` § builder-check, routing). So the gaps are in the client contract
  (`PlanIntentResource.Decision`: id, text, decider, options_weighed, question_id; `nonGoals: List<String>`;
  `IntentDecider.MODEL("model")`; `BuilderCheckResource` without a reason), not in the engine.
- New finding (Q7): the operator's decision of 2026-11-10 recorded in PLAN-85 (`## Operator Decisions 2026-11-10`) puts
  theme AND language as `RadioGroup`s into the "This browser" tab, in addition to the rail foot and the More sheet. The
  spec's Q7 starting recommendation (the tab holds only the dictation choice) contradicts that decision.

## Q1 The plan page in the column model, and its tabs

- Current: `plan-view.adoc:16-47` draws a tree under the head, no tabs; `navigation.adoc:66` the phase tree is the plan's
  list of children, a task the next level; `:203` `node={path}` selects a phase or sub-workflow by a replace;
  `plan-view-interaction.adoc:69-80` always selects the running task or node; `components.adoc:333` _Plan settings…_ is a
  dialogue; `plan-view.adoc:192` tokens by phase and role one step from the head line; `plan-view.adoc:159` the size line
  opens the changes; `ObjectPage` has a `tabs` slot no view uses. PLAN-55 asks Process, Intent, Activity, Tokens, Settings.
- Gap: with a node always selected, is the narrative a column (it is no level and no object) or part of the plan's column?
  Where do Intent, tokens and the changes stand?
- Options:
  - (A) The plan is the object column with tabs. _Process_ holds the tree and the selected node's narrative inside the tab.
    Opening a task makes the plan a list column (compact head, its list is the tree) and the task the object column.
  - (B) The tree is always a list column and the narrative the next column; Intent and Tokens are head links. Breaks
    `navigation.adoc#column-types` (a list column is an object to the left of its opened child; a narrative is no object).
  - (C) Tabs Process, Intent, Activity, Tokens (PLAN-55). Activity has no design (the worker feed is the task level);
    tokens is a few lines.
- Changes (amendment): (i) a tab _Changes_ (`tab=changes&file=<path>`), the size line a link to it; (ii) it stays a
  disclosure of the size line, shown in the node narrative's place inside _Process_ (as #400 wrote); (iii) a level of its own.
- Recommendation: (A) with tabs **Process** (default), **Intent**, **Changes** (absent before the first capture, like the
  size line); tokens stays the one-step disclosure of the head line; no Activity, Tokens or Settings tab (_Plan settings…_
  stays a dialogue); an ended plan has the same tabs read-only. Changes as a tab because it has its own selection (a file),
  its own address and its own list, which would otherwise fight the tree's selection for the narrative pane.

## Q2 The epic page's tabs

- Current: `epic-view.adoc:16-54` lists head, acts, workstreams with plans, then lines Sources, Landings, Findings, Watches,
  Tokens, then Events. A workstream is no level (`navigation.adoc:66`).
- Options: (A) PLAN-58's six with "Workstreams" renamed: Plans (default), Intent, Landings, Reviews (findings and
  watches), Tokens, Events. (B) Plans and Intent only, the rest lines of the Plans tab. (C) Plans, Intent, Activity
  (landings, reviews, events), Tokens.
- Recommendation: (A) without the Tokens tab, for the same reason as the plan: **Plans** (default), **Intent**,
  **Landings**, **Reviews** (a `CountBadge` `! n` for new findings), **Events**; tokens is the head line with its one-step
  disclosure per plan (`epic-view.adoc:50`). Sources stand at the top of the Intent tab ("where it ends, and what it was cut
  from"). As a list column the epic shows its plans grouped by workstream (compact head, rows only).

## Q3 `plan-view-interaction.adoc` in the column model

- Current: DRAFT (`:12`); `:14-32` two panels with the `project › epic › plan` header; "right panel" in `:45,49,53,95`;
  three acts `:34-67`; defaults `:69-80`; narrative `:82-99`; empty states `:101-122`; changes `:124-127`; untrusted text
  `:129-136`.
- Do: rewrite the layout for Q1; keep the selection semantics, defaults, narrative, empty states and untrusted-text rule;
  drop the header path. Open detail: how the Process tab lays out tree and narrative inside one object column (at least
  480 px), and how `node=`, `file=` and `tab=` relate.
- Recommendation: tree and narrative side by side inside the tab while the object column is at least 720 px wide (tree
  280-360 px), the narrative below the tree otherwise; `node=` is written only with the Process tab and `file=` only with
  the Changes tab, a tab switch is a replace that drops the other tab's key, and the view keeps the other tab's selection
  in memory while the plan stays open; `changes=open` goes (`tab=changes` replaces it).

## Q4 The intent design

- Current: `intent-and-outline.adoc` DRAFT (`:12`), no texts, "a verdict chip per criterion" (`:276`) though the tag set
  has no verdict tag, not linked from `plan-view.adoc`, `epic-view.adoc` or `web-app/README.adoc`; the chart "in the role's
  settings" (`:276`) but the route is per project and role (`OverSizeDistributionResource` `project_id`, `role`); the
  `over_size` multiple sits in a role artifact's `off_track` (`model-work/01-roles-and-configuration.adoc:74-83`).
- (a) Verdict tag: `StatusLabel` states `state.criterion.{met,not_met,not_verified,not_reviewed}` and
  `state.check.{pending,met,not_met}` (tones: met success, not met danger, not verified neutral — never a warning,
  `intent-and-outline.adoc:99` —, not reviewed / pending neutral); or a neutral `Chip` (wrong: a `Chip` copies an id); or a
  new component. Recommended: `StatusLabel`.
- (b) Texts: `shared/intent-and-outline-texts.adoc`, area `intent`, English and German, both clients. Recommended.
- (c) Links: the plan's Intent tab, the epic's Intent tab, the decomposition review, a README row (move words out to stay
  at 500 lines or fewer).
- (d) Chart home: project page setup tab (route per project and role) / machine settings roles aspect / both.
  Recommended: the project page's setup tab.
- (e) Contract against design, corrected: the engine carries the ADR relation, the fact's evidence, `NG-n` ids and the
  `not_run` reason; only vote support is absent. Options: (1) the design keeps them, drops vote support, uses the engine's
  decider word `role`, and lists the contract delta (`Decision.adr`, `Decision.evidence`, non-goals as `{id, text}`,
  `role` for `model`, `BuilderCheckResource.reason`) for a contract+mock step before PLAN-145; (2) the design says "left
  out" for them until the contract grows, numbers `NG-n` by position and keeps `model`; (3) as (1) but PLAN-145 grows the
  contract itself. Recommended: (1).

## Q5 The chat's side column before the engine

- Current: `navigation.adoc:269-301`, `components.adoc:363-374` put the side column in `TriagePage` (and the spec says
  `ObjectPage`); `chat.adoc:40` and `components.adoc:361` `WizardPage` hosts it; code has none. PLAN-135 D5 builds an
  interim slot "against today's single object column".
- Options: (A) interim slot by the chat plan, moved later; (B) the slot only in the engine (PLAN-142 delivers `side`,
  `sideOpen`, `onHideSide`), the chat plans wait; (C) as (B) with the API fixed here.
- Recommendation: (C), on `TriagePage` and `WizardPage` only (the object column stands inside the engine, so `ObjectPage`
  needs no side of its own); 480/400 measured on the container; on the phone the chat takes the screen and the bottom bar;
  `open=` forms of status cards (`navigation.adoc:277`, `chat.adoc:164`) need PLAN-143, until then the pushed address is
  the only form.

## Q6 The rail item, the remembered place and the lens segment

- Current: `plan-overview.adoc:164` remembers project, view, selected plan or epic (mode not stated);
  `OverviewView.svelte:191-215` also restores the ended section; `navigation.adoc:213`, `plan-overview.adoc:171`,
  `README.adoc:456` fill a bare start and a bare `/overview`; `navigation.adoc:172-173` lens segment navigates as Up.
- Defects: a bare `/overview` is always filled, so Up from a project and the lens segment loop back; a reload of `/` looks
  like a bare start.
- Recommendation: the place holds project, grouping, selected plan or epic, never the mode (the rail opens running work:
  PLAN-52's clause holds); the fill applies only to a load without the app's marker in `history.state` (a fresh open, a
  bookmark) and a click on the rail's lens entry; Up and the lens segment push the unfilled lens address with the marker,
  which a reload keeps; an ended plan falls back to its project, a project without running work to the project list.

## Q7 The theme and language residue

- Current: `README.adoc:147` rail foot, `:153` More sheet, `:431` themes; `:492` `browser` tab "This browser";
  `machine-settings.adoc:76` "beside its theme and language"; `chat.adoc:239` "its own settings (_This browser_)". PLAN-85's
  operator decision of 2026-11-10: the tab holds theme and language as `RadioGroup`s (applied at once, `pm_theme`,
  `pm_locale`, "Kept in this browser. It is not sent to the machine."), rail foot and More sheet stay.
- Options: (A) keep the 2026-11-10 decision: the tab holds theme, language and the dictation choice, rail foot and More
  sheet stay the quick controls; the three lines are right and only `README.adoc:431,492` say so. (B) the spec's
  recommendation: the tab holds only the dictation choice; theme and language only in the rail foot and the More sheet;
  PLAN-85 loses its RadioGroups. Both: a browser before pairing has no rail, so its theme follows the system until paired.
- Recommendation: (A), because it is the operator's recorded decision and the three documents already agree with it.

## Answers

(recorded below as they come, with their date)

- Q1 (operator, 2026-11-10): option A — the plan is the object column with tabs Process (default, tree + node narrative
  inside the tab) and Intent; a task opens as the next level and the plan becomes a list column whose list is the tree;
  tokens stays the head line's one-step disclosure; no Activity, Tokens or Settings tab; an ended plan has the same tabs
  read-only. Changes: a third tab `Changes` (absent before the first capture), the size line links to it, address
  `/plans/{id}?tab=changes&file=<path>` replaces `changes=open`.
- Q2 (operator, 2026-11-10): five tabs Plans (default, grouped by workstream), Intent (Sources at its top), Landings,
  Reviews (findings and watches, `! n` count for new findings), Events; tokens is the head line with its one-step
  disclosure per plan; as a list column the epic shows only its plans grouped by workstream.
- Q3 (operator, 2026-11-10): tree (280–360 px) and narrative side by side while the object column is at least 720 px
  wide, the narrative below the tree otherwise; selection semantics, defaults, narrative, empty states, untrusted text
  kept; header path and "right panel" go. `node=` only with Process, `file=` only with Changes; a tab switch is a replace
  that drops the other tab's key, the view keeps the other selection in memory while the plan stays open.
- Q4 (operator, 2026-11-10): (a) `StatusLabel` states `state.criterion.{met,not_met,not_verified,not_reviewed}` and
  `state.check.{pending,met,not_met}`; (b) new `shared/intent-and-outline-texts.adoc`, area `intent`, EN and DE;
  (c) all four links: the plan's Intent tab, the epic's Intent tab, the decomposition review, a README row (words moved
  out); (d) the chart on the project page's setup tab; (e) the design shows the ADR relation, the evidence, `NG-n` by id
  and the `not_run` reason, uses the engine's `role`, drops vote support; the contract delta (`Decision.adr`,
  `Decision.evidence`, non-goals `{id, text}`, `role` for `model`, `BuilderCheckResource.reason`) is a contract+mock step
  before PLAN-145.
- Q5 (operator, 2026-11-10): no interim slot; PLAN-142 delivers `side`, `sideOpen`, `onHideSide` on `TriagePage` and
  `WizardPage` with the API fixed in `components.adoc` now; `ObjectPage` has no side; the chat plans wait for PLAN-142.
  480/400 measured on the container; on a phone the chat takes the screen and the bottom bar; the `open=` forms of status
  cards and reference chips need PLAN-143, until then the pushed address is the only form.
- Q6 (operator, 2026-11-10): the place holds project, grouping, selected plan or epic, never the mode (the rail opens
  running work); the fill applies only to a load without the app's marker in `history.state` and a click on the rail's
  lens entry; Up and the lens segment push the unfilled lens address with the marker, kept by a reload; an ended plan
  falls back to its project, a project without running work to the project list.
- Q7 (operator, 2026-11-10): keep the PLAN-85 decision: the "This browser" tab holds theme, language and the dictation
  choice; the rail foot and the More sheet stay the quick controls on the same storage; `README.adoc:431,492` reworded;
  a browser before pairing has no rail, so its theme follows the system until it is paired.
