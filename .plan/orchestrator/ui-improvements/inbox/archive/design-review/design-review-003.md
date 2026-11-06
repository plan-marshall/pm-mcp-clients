envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T16:10:29Z
revision=12
amended=2026-11-06T20:06:27Z

# Unified navigation: one list → content pattern for every kind

Sender: operator design review, 2026-11-06, in the browser against the mock runtime (`mock-machine`), checked against the code, the agreed designs and the staged plans, and reviewed adversarially. The operator has settled the design below, except the width table, which comes from the prototype of the design session. This message is the input for a new design document `doc/design/web-app/navigation.adoc`.

## Operator feedback
- The left-to-right navigation of Running work (rail → select an item → its content on the right) becomes the general pattern. You select something, and the right side shows its content or the list of the next elements. The path is the same for every kind.
- Needed for this: a consistent Back navigation, and a clear visual identification of the kinds (project, epic, plan, report, …) through shape and colour.
- The views of *Work* must work together. One unified navigation, the same for all kinds.

## Current state (code and app)
- **Each view has its own layout.** Running work: rail · project column · the project's work. Projects: a card stack where clicking a card does nothing, and four text links per card. Decisions: one long list with every form expanded. Reports: its own list · detail. Plan and Epic: full-width pages.
- **Back is a caller stack.** `goBack()` / `navigationStack` (`pm-web-app/src/lib/stores/runtime.ts:~890-915`) with a `history.back()` fallback, used by Plan, Epic and Settings. Back buttons also sit in CompletionReport (`:101`), Overview (`:670`), ProjectOnboarding, EpicAdopt, EpicDecomposition, MachineSettings ("Zurück zu den Einstellungen") and Cleanup ("← System", "Zurück zum Job"), each in its own place and weight.
- **Two breadcrumbs.** The top bar and the object page each show a path. The epic's path uses its slug.
- **Rail selection** comes from `activeNavRoute` (`shell/nav.ts:136-152`): plan/epic → Running work always.
- **URL.** Every route-param change after the first write is a `pushState` (`routes/+layout.svelte:228-241`). The app also selects on its own: Running work picks the first project (`OverviewView.svelte:267`), and the plan view follows the running task (`PlanView.svelte:286,414`). Addressable today: `/plans/{id}`, `/epics/{id}`, `/decisions?decision=`, `/reports?plan=`. Not addressable: the selected project of Running work, its mode (running/ended, by plan/by epic), the selected task, the ended state of a plan (`runtime.ts:1167`), a project selection (`/projects?project=` is a text filter).
- **Kind identity is ad hoc.** Text glyphs `◇`/`▸` (`OverviewView.svelte:711-712`), a grey plan badge and a violet epic badge. The only kind colour is `--color-epic #7A3FD1`, close to the brand violet `#5B4BDB` (`styles/tokens.css:78-89`). Icons exist but rows do not use them: `kind-decision.svg` (diamond), `kind-epic.svg` (layers), `kind-plan.svg` (looks like `kind-task.svg` at 16 px). There is no `kind-project`.
- **Templates.** `ObjectPage` (head, attention, tabs; 1440 px cap) and `TriagePage` (list · detail; a viewport query at 1099 px that ignores the rail, `TriagePage.svelte:57`) exist. Only Reports and System use a template.

## The design

### 1. Lenses and the rail
- The rail entries of *Work* and *Records* are **lenses**: What waits, Running work, Projects, Decisions, Reports. Each lens has a list column of its own items.
- **The rail shows the lens of the current route. Every kind has exactly one canonical route**, and so one home lens:

| Kind | Home lens |
|---|---|
| Project | Projects (the one project page: work, setup, release) |
| Epic, Plan, Side job | Running work (running or ended mode) |
| Decision | Decisions |
| Report | Reports |
| Machine item | credential → Settings, cleanup due → System |

- **Selecting in a lens's own list scopes the lens.** For example, a project selected in Running work narrows the work list to that project. It is not the project page, and the rail stays.
- **Opening an object from another lens goes to its canonical route**, and the rail moves with it. Every project link (plan breadcrumb, find, report path chip) opens the one project page.
- **In place instead of a jump**, in exactly these cases: a plan opened from the cleanup job (`shared/cleanup.adoc:139`), the decision of a waiting plan, the completion report of an ended plan. The object opens as a content column inside its caller. The caller's state (e.g. the cleanup checks) stays, and the rail does not move.
- What waits (`/`, PLAN-49) is a launcher: each item opens at its canonical route or in place.

### 2. Levels as columns
- A page is a **chain of levels**, always following the structure: lens list → project › epic › plan (a plan without an epic: project › plan) → the plan's phase tree → task. "By plan / by epic" only groups rows inside a column and never changes the chain.
- Each column lists the children of the item selected in the column to its left. Opening a child therefore shows its siblings, with the child selected, in the column to its left.
- **Two column types:**
  - The **object column** is the rightmost object: full head (kind mark, title, id, state sentence, acts; the path is the top-bar breadcrumb of §4), attention strip, tabs, body. It uses ObjectPage's regions, without the 1440 px cap inside a column. **Only the rightmost object column carries the screen's primary act** (007 §2).
  - A **list column** is an object to the left of its opened child. It shows its children list with a compact head (kind mark, title, state word, and its main act as a secondary button). Selecting the object itself closes the child and makes it the object column again, with all its tabs.
- **Selection missing from the list:** if the selected item is not a row of its column (ended, read, resolved, filtered out, on another page), it is shown as a **pinned row** at the top, labelled with the reason. The ended mode of Running work lists every project with ended work. An object deleted while shown falls back to its parent with a notice line, and the URL is replaced, not pushed.

### 3. The column window
- **The screen shows the rightmost columns that fit: 1, 2, 3 or 4.** The window is computed from the **container width** (not the viewport) and fixed column widths, and it changes on resize. The phone is the same rule with one column.
- Widths: list column ≈ 220–320 px, object column ≥ 480 px. A rightmost list with nothing selected takes the object column's width. Each column scrolls on its own. `navigation.adoc` publishes the width table (rail expanded and collapsed, at 360 / 768 / 1024 / 1280 / 1440 / 1920 px). At 1024 px (744 px of content with the rail expanded) two columns fit only with a narrow list column, so the gap strip there shows its marks only and names them in a popover; the width table states the rule per width.
- **Gap strip:** when levels are hidden on the left, a narrow strip at the left edge shows them as stacked kind marks. Their names are visible (not tooltip-only), and the strip is a button group with accessible names. Selecting a mark brings that level back into view. The strip changes no selection, no URL and no history. With one column there is no strip. When a side column (the chat, §10) takes the only column, the object returns through the chat head's "Blende den Chat aus", not through a strip.
- Shifting the window slides briefly, and does not move at all when reduced motion is requested.

### 4. Up, breadcrumb, Back
- **Up** sits at a fixed place in the head of the first visible column. It shows the parent's kind mark and name (the epic's KindMark + "Relay Hardening") and **navigates** to the parent: it pushes the parent's URL, and the parent becomes the deepest selection. It behaves the same at every width.
- **One breadcrumb**, in the top bar: lens › project › epic › plan, each with its kind mark, titles not slugs. Every segment navigates like Up. The object column's head carries no second path.
- **Browser Back/Forward** move through history.
- There is no caller stack: `goBack()`, `navigationStack` and every in-page Back button listed under Current state go. Remaining backs have their own meaning: the wizard Back, dialogue cancels (007), and **closing an in-place object column** (§1: a plan opened from the cleanup job), which is a ghost act "Zurück zum Job" in that column's head and returns to the caller with its state (`shared/cleanup.adoc:139`).

### 5. URL and history
- **Every selection is in the URL**, in one scheme: path segments for objects, query for scope and mode (selected project in Running work, running/ended, by plan/by epic, follow mode). **An in-place object** (§1) is a query on its caller's URL (`?open=plan:fix-relay`, `?open=decision:Q-007`), opened by push and closed by history back ("Zurück zum Job" is a history back). The plan head's link to its waiting decision opens it in place, beside the plan.
- **Push** only for a change of depth (open a child, Up, breadcrumb) or of lens. **Replace** for a sibling change in the same column, for j/k moves, and for every selection the app makes on its own.
- Following the running task is a **follow mode** (a mode in the URL, not a task id) until the operator selects a task.
- **The URL wins over the remembered place** (`pm_overview_*`, `shared/plan-overview.adoc:161-169`). A bare rail entry is filled from the remembered place by `replaceState` only.
- A pairing code in the fragment is redeemed before the first write and never written into history.

### 6. Kinds and KindMark
- **One kind per object, a closed set:** Project, Epic, Plan, Side job (landings and running setup/onboarding runs, "alongside" in `shared/plan-overview.adoc:53`), Decision, Report, Machine item (credential, cleanup due). Phases and tasks belong to the plan and use the icons of PLAN-62. Workstreams group rows inside a column and are not a level.
- **`KindMark`** = icon + word (the word visually hidden where space is short). It is used in every row, list column head, object head, breadcrumb segment, Up, the gap strip, find and What waits.
- **Shape:** Project = new `kind-project`, Epic = layers (`kind-epic.svg`), Plan = redrawn `kind-plan` (distinct from `kind-task` at 16 px), Decision = diamond (`kind-decision.svg`), Report = `kind-report`, Side job and Machine item = their own icons.
- **Colour = tinted icons only**, never on the row, its edge or its background. **Only Project, Epic and Plan are tinted** (operator, 2026-11-06; defined by D1, used here); Side job, Decision, Report and Machine item stay neutral and are told apart by shape and word. The three tints stay clear of the state hues (running, attention, danger, success, neutral), the brand violet and the mascot's orange and gold (005), and reach ≥ 3:1 as non-text graphics in light and dark. "Clear of" is measurable (D1 sets a minimum hue / ΔE distance), and the three tints may also differ by lightness.
- **What waits:** a What-waits row's KindMark is the object's kind. The item kind of `shared/needs-and-find.adoc:40` (stalled, held, landing_failed, setup, credential, cleanup_due, waiver, …) becomes the row's **reason** (`ReasonBadge`, 006), worded by the agreed `overview.needs.*` keys. A `decision` item is a row of kind Decision without a reason badge.
- **One row shape** for every kind in lists: the row slots of the tag design (006 §2).

### 7. Keyboard and focus
- **Ctrl+←/→** move between columns. Plain arrows stay with the widget (the phase tree, `Tabs.svelte:86-89`, text fields). `Enter` keeps its meaning per widget (in Decisions it decides).
- After every window shift, focus moves to the heading of the new column, and the move is announced in the polite live region. Focus never falls to `<body>` when its column hides. Hidden columns are out of the tab order and the accessibility tree.

### 8. Phone
- One column, Up, the breadcrumb shortened in the middle, no gap strip.
- **The bottom bar** (operator, 2026-11-06): What waits · Running work · Decisions · More. Reports moves into More, and its unread count shows on More. This changes `README.adoc:144` and follows `web-app/needs-and-find.adoc:29` (What waits first). "More" gets an active state for the destinations it holds (`BottomBar.svelte:132-138`).

### 9. Scope
- **Not list → content:** start (WizardPage), decomposition, onboarding, settings, designer, system/cleanup (the cleanup job hosts in-place objects, see 1).
- **Web only.** The terminal keeps Esc = return to the opener (`terminal-client/15-overview.adoc:238-244`).
- **No layout modes:** the column window is the only layout (message 004).

### 10. Side columns (chat)
- The chat (009) is a **side column**, not a level. It docks to the right of the object column it belongs to. The object keeps its full head, and the gap strip never shows the chat.
- If the window cannot show the object column and the chat, the chat takes the space and the object becomes a hidden level in the gap strip (the object column's own levels shift left, as usual).
- The open chat is in the URL as a query (`?chat=open`). Opening and hiding it is a `replaceState`, not a push.
- Hiding the chat (Escape) keeps it running. Ending it is the chat's own act.

## Agreed documents that change
- New: `doc/design/web-app/navigation.adoc` (sections 1–10, with the width table).
- `web-app/components.adoc` (D2 writes only the column templates; KindMark and the kind tints are D1's, `design-review-010.md`): the column templates (TriagePage as the column engine; ObjectPage regions inside the object column).
- `web-app/README.adoc`: rail and frame (:138, :144), body scrolling (:182), page widths (:211-239), routes table with one address scheme (:481ff).
- `web-app/view-density-and-filtering.adoc`: the layout-mode sections go (message 004).
- Shared (web part only): `shared/plan-view.adoc:43,182` (head path → breadcrumb), `shared/plan-overview.adoc:161-169` (persistence vs URL), `shared/cleanup.adoc:139` (in place), `shared/completion-report.adoc:104` ("its plan view"), `shared/needs-and-find.adoc` and `web-app/needs-and-find.adoc` (row = KindMark + reason; :29 the bottom bar and the removed table/list switch), `shared/project-explorer.adoc` (the one project page: work, setup, release), `web-app/interaction-affordances.adoc` (keys: Ctrl+←/→, Up).

## Affected plans
- **Hold until `navigation.adoc` is AGREED:** PLAN-55 (plan page → object column), PLAN-58 (epic page → object/list column), PLAN-59 (Projects: list → one project page instead of a card grid), PLAN-83 (moving between plans: replaced by the column chain; fold it into the navigation work), PLAN-52 (web-running-work: its "project sidebar becomes a filter chip row" contradicts the project list column of Running work).
- **Rescope:** PLAN-51 (Decisions: list → one decision, the next decision after deciding is a replace, the layout modes removed per message 004, `Enter` decides). PLAN-49 (What waits as launcher, rows with KindMark + reason layer, `/` becomes an `AppRoute`). PLAN-62 (the phase tree is a column of the plan, its icons must not clash with kind marks, plain arrows stay with the tree).
- **PLAN-70 / PLAN-72:** the kind tints exist in both themes and fall under the token lint. The route baselines are retaken after the views move.
- **PLAN-74:** closes the docs after this design. It does not write it.

## Suggested action (the epic decides)
1. **Design package** (this repository's `doc/design/`): `navigation.adoc` and the document changes above, then operator review to `AGREED`. Before AGREED, a throw-away prototype on the mock runtime checks the width table, focus and the gap strip at 1024 / 1280 / 1440 px and on the phone.
2. **Foundation package (web):**
   - the column engine on `TriagePage` (container-width window, gap strip, focus management);
   - the URL scheme and the history rule in the URL sync;
   - Up and the kind-marked breadcrumb in `TopBar`;
   - `activeNavRoute` = the route's lens;
   - removal of `goBack` / `navigationStack` and the "Zurück" buttons;
3. **Apply per view** through the held and rescoped plans: 52 (Running work onto the engine), 55, 58, 59, 51, 49, Reports onto the engine (PLAN-83 is folded into the engine). Then 70 → 72/73 → 74 as queued.

## Process
The design of this message is produced **before any implementation**, in its own Claude Code session: design session **D2 (navigation)** of `design-review-010.md`.
- That session sets the documents it creates or changes to `Status: DRAFT`, reviews them (`ref-documentation` docs review, `ref-asciidoc` validate and verify-links) and opens a pull request.
- The operator sets them to `AGREED` as its last commit, and the pull request is merged on the operator's word.
- Only after that merge do the implementation plans named above start or restart.

Order, document ownership, open points, what each plan waits for, and the merged per-plan instructions are in `design-review-010.md`.

## Open points for the design session
Only the width table, settled with the operator from the column-window prototype (`design-review-010.md`).
