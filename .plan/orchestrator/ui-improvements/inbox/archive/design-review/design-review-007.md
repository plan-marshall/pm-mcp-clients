envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T18:15:51Z
revision=5
amended=2026-11-06T20:06:28Z

# Interaction elements: one closed set for buttons, links, menus and clickable objects

Sender: operator design review, 2026-11-06. A full review of every interaction element (buttons, links, menus, toggles, clickable rows and cards) in the web app, in the browser against the mock runtime and in the code, checked against the agreed designs. It builds on `design-review-003.md` (navigation) and `design-review-006.md` (tags).

## Operator feedback
Review the interaction elements (buttons, links, clickable objects) the same way as the tags: are structure, location and content consistent? (First checked: no staged plan owns this. PLAN-47 (shipped) brought `ActionBar` and `byState`. PLAN-52/55/58/59 adopt them for their views. PLAN-85 extracts the theme and language choices.)

## Current state
**The agreed components exist; most views bypass them.**
- `Button` (primary/secondary/ghost/danger, `loading`, `disabledReason`), `IconButton`, `ActionBar`, `RowMenu`, `ConfirmDialog`/`Dialog`, `Breadcrumb`, `Tabs`, `Chip` exist.
- Next to them there are **226 raw `<button>` in 45 files** and about 20 hand-rolled classes: `.btn-link` (twice), `.breadcrumb-link`, `.report-link-btn`, `.back-link`, `.btn-toggle`, `.toggle-btn` (two meanings), `.card-action`, `.review-open`, `.refresh-btn`, `.order-btn`, `.fav-toggle-btn`, `.notice-dismiss`, `.intake-mode`, `.tab-btn`, ….
- The wizard and modal files (StartView, DecompositionReviewView, FindingsReviewModal, GrantModal, HoldResolutionModal, StartWorkModal, …) use bare buttons without any variant.
- `Tabs`, `Chip`, `Card`, `AttentionStrip`, `Toast`, `ObjectPage` and `WizardPage` are used by no view.
- `byState.PROJECT_ACTS` is used nowhere.
- At least 16 dialogs are hand-rolled instead of `Dialog`/`ConfirmDialog`.

**Seen in the browser:**
- **Running work, row "Aktionen":** plain text, no button look, no hover. It sits mid-row in the epic row and at the right in the plan row. A click opens an **act panel at the top of the list** (above the filter, far from the row), pushes the list down, and is closed only by a text "Schließen". Escape does nothing.
- **Running work cards** are `<div on:click>` (`OverviewView.svelte:909,1017,1112`, the a11y warnings switched off). A mouse click on the card opens it, with no pointer and no hover. The keyboard reaches only the title and "Aktionen".
- **Plan page:** two act groups.
  - Top right: "Prozess gestalten", "Spring dorthin, wo er steht", PR chip, underlined "Zeige die Details", ghost "Zurück": five elements in four forms.
  - Below the title: `ActionBar` "Halte den Plan an", "Chat", "⋯ Mehr" (with "Einstellungen" and "Brich den Plan ab", no danger look, no "…").
  - **No primary act.** A second link to the same report ("Abschlussbericht", `PlanView:760`).
- **Projects card:** the card is not clickable. Its name is plain text, and a row of four small text links does everything: "Laufende Arbeit" (opens the overview *without* the project), "Einrichtung", and "Freigeben…" and "Austragen…" in red, at rest (as `README.adoc:351` prescribes today, against `interaction-affordances.adoc:120`). Stars and "↑ ↓" are tiny glyph buttons.
- **Mode switches look like acts:** "Beendete Arbeit" is styled like "Starte das Epic". "nach Plan / nach Epic" is a segmented control with a solid violet fill (`README.adoc:448` says `--color-primary-subtle`).
- **Top-bar breadcrumb:** TopBar passes no `href` (`TopBar.svelte:44-50`), so every segment is plain text. PlanView and EpicView draw a second, clickable breadcrumb of their own.

**From the code:**
- **More than one primary per screen** (`README.adoc:351`, `components.adoc:26`: exactly one):
  - Projects: "+ Projekt hinzufügen" twice, plus "Setze fort" per card.
  - System: "Upgrade…", "Upgrade now", "Try again", "Load again", "Reconnect", "Review cleanup" plus the `ActionBar` primary. The upgrade act exists twice on one screen.
  - Decisions: waive, confirm, own-words and notify are all primary.
- **Danger at rest without a confirm** (`components.adoc:39`: danger only inside a danger dialogue; `interaction-affordances.adoc#acts`: destructive acts in the overflow, through `ConfirmDialog`):
  - Decisions "Ablehnen" is filled danger with no confirm (`DecisionView:862`, no `ConfirmDialog` in the view).
  - Cleanup "Delete N items…" and ProjectOnboarding "Abandon" are filled danger on the page.
  - Projects "Freigeben… / Austragen…" are red links in every card, with hand-rolled modals.
- **One act, several weights:** "Back" is ghost (Plan, Epic, CompletionReport, Designer, Decomposition), secondary (Overview, Settings, MachineSettings, ProjectOnboarding, Cleanup) or a text link (Cleanup "← System"). "Close" is ghost or secondary. "Save" is primary or secondary. "Retry" is secondary or ghost. Dialog footers come in four orders, and the cancel button is ghost or secondary.
- **Navigation drawn as six different things:** `.breadcrumb-link`, `.report-link-btn`, `.btn-link`, `.back-link`, ghost `Button` (Reports), and real `<a>`. PR links fall back to `href="#"` when the URL is missing (`PlanView:691`, `CompletionReportView:145`, `OverviewView:743`).
- **Five segmented-control implementations:**
  - `role="radio"` without a radiogroup (Overview);
  - `.btn-toggle` with no `aria-pressed` (System);
  - `.intake-mode` (Designer);
  - `role="tab"` buttons next to the unused `Tabs` (StartWorkModal, ConfinedFilePicker, GitSourceIntakeModal, Cleanup, the Overview project list);
  - `ViewLayoutToggle`.
- **Labels:**
  - **Verb forms:** imperative with object ("Zeige die Details", "Starte den Plan"), bare ("Chat", "Mehr"), nouns ("Einrichtung", "Aktionen", "Abschlussbericht"), questions ("Warum?") and glyphs ("↑ ↓ ↻ ×", "+ ").
  - **Literals:** "project", "attention", `${n} items` (`CleanupView:496`), "+ ".
  - **One act, many names:**
    - back: "Back" / "Back to the settings" / "Back to the job" / "Back to System" / "Back to the designer" / "Stay";
    - cancel: "Cancel" / "Keep it" / "Not now" / "Leave it" / "Later" / "Close" / "Back";
    - retry: "Retry" / "Try again" / "Load it again" / "Reconnect";
    - open the report: "Show the details" / "Completion report" / "Open the completion report".
  - **Duplicate keys:** `common.back` / `common.actions.back` / `web.common.back`, `common.close` / `web.common.close`, `common.save` / `web.common.save`. "Later" appears with two keys in one view.
  - **The trailing "…"** is set on some dialog openers ("Freigeben…", "Upgrade…") and not on others ("Brich den Plan ab", "Abandon", "Archive", "Decline").
  - **The plan act "Cancel"** (end the plan) clashes with the dialog "Cancel" (dismiss).
  - **One label for two acts:** "Open" for two designer panels, "Instruct again" for two steps.
  - **Two key families for the same decomposition acts:** `decomposition.*` and `start.decomposition.*`, in two view implementations.
- **States:**
  - `disabledReason` and `loading` exist on `Button`, but most call sites pass `disabled={busy}` plus a text swap.
  - `aria-pressed` on some toggles, missing on others (System window/group, Designer intake, the project star).
  - List selection uses `aria-pressed` in one tree and `aria-current` in another (`PlanView:910` vs `:947`).
  - The `ActionBar` menu items have no focus style of their own.
  - Hand-rolled controls fall below the 24 px target (`.btn-link` xs, `.fav-toggle-btn` ≈ 20 px, `.order-btn` 28 px, `.toggle-btn`, `.breadcrumb-link` padding 0, `.notice-dismiss`) and do not reach 44 px on the phone (`components.adoc` rules).
- **Hand-written act lists** instead of `byState`: Projects (`PROJECT_ACTS` unused), Decisions, Cleanup, ProjectOnboarding, the wizards, the Overview create acts, the PlanView head acts, the System upgrade panel. The plan act `decision` is excluded from `WorkControlPanel` on purpose ("`decision` is the plan page's", `:129-130`) and has no defined place in the plan head.
- **Docs vs code:** `components.adoc:48` says `IconButton` has "no visible label", while `IconButton.svelte:120-130` shows the label from 768 px up.

## The design

### 1. A closed set: one component per kind of interaction
| Kind | Component | Element | Used for |
|---|---|---|---|
| **Act** | `Button` (primary / secondary / ghost / danger) | `<button>` | does something: start, hold, save, decide, confirm |
| **Icon act** | `IconButton` (icon only, accessible name, tooltip; ghost) | `<button>` | a compact act in a row or toolbar: row menu `⋯`, copy, clear. A secondary act that is icon-only (the chat's Send) is a `Button` with `icon` and a visually hidden label. |
| **Object acts** | `ActionBar` (primary, ≤ 3 secondary, `⋯ Mehr` overflow) with acts from `byState` | | every object's acts, in its head |
| **Row acts** | `RowMenu` (`IconButton ⋯` + the same menu as the `ActionBar` overflow) with acts from `byState` | | the acts of a row, at the row's right end |
| **Navigation** | `Link` (in-app, every place has a URL per 003) and `ExternalLink` (new tab, external icon) | `<a href>` | goes somewhere: object, report, PR, docs, designer |
| **Whole row / card** | `RowLink`: the row's title is the link, stretched over the whole row | `<a href>` | opens the object the row stands for |
| **Mode / grouping switch** | `SegmentedControl` (`role="radiogroup"` + `aria-checked`, chosen = `--color-primary-subtle`) | | by plan / by epic, running / ended, time window, intake mode |
| **Tabs** | `Tabs` | `role="tablist"` | the tabs of an object (Process, Intent, …) |
| **Toggle** | `Toggle` (`aria-pressed`) | `<button>` | an on/off of one thing: favourite, follow mode |
| **Dialog** | `Dialog`, `ConfirmDialog` | `role="dialog"` + focus trap | every modal, every confirmation |
| **Copy** | `Chip` | `<button>` | ids and branches (006). PR numbers and references are links in the chip look (`PrChip`, reference chips), not copy chips. |
| **Breadcrumb** | `Breadcrumb` with `href` per segment | `<a href>` | the one path in the top bar (003) |

- **Rule of the element: navigation is a link, an act is a button.** No button drawn as a link, no link that acts. Every in-app link has a real `href`, so open-in-new-tab, middle-click and copy-link work. A missing URL renders text, never `href="#"`.
- No raw `<button>`/`<a>` and no ad-hoc button/link class outside `lib/components`. The wizards and modals move to `Button`, `Dialog` and `WizardPage`.
- No clickable `div`/`span`/`li`/`tr`.

### 2. Hierarchy
- **One primary per screen**: the main next act of the object shown, taken from `byState`. **Only the rightmost object column carries it** (003 §2). List column heads show their act as secondary. The lens's create act is the primary only while no object column is open. The chat's Send is a secondary icon `Button` (009). Everything else is secondary (at most three visible), and the rest goes into `⋯ Mehr`.
- **Ghost** only for leaving without acting: dialog/wizard Back, Close, Cancel, dismiss.
- **Danger** only on the confirming button of a `ConfirmDialog`. A destructive act is opened from the overflow menu (menu item in danger text colour, label with "…"), never as a visible danger button or red link at rest.
- **Decisions:** the options of a question are `RadioGroup` cards (008), and the one act is the primary "Entscheide". A waiver keeps its agreed answers and its no-preselection rule (`shared/decisions.adoc:169-174`, `decisions-texts.adoc:265-267`): "Erteile den Waiver" and "Lehne ab" are **both secondary**, neither is preselected, `Enter` answers nothing, and "Später" is ghost. `Enter` decides only a decision with a selected option. Neither is a danger act. A choice that cannot be undone asks through `ConfirmDialog`.
- **Dialog footer:** right-aligned, cancel (ghost) on the left, confirm (primary or danger) on the right, in every dialog.

### 3. Fixed places
- **Object head** (object column, 003): `ActionBar` at the right of the head. Primary first, then the secondaries, then `⋯ Mehr`. Related places are links in the head's id line (006): PR chip, "Abschlussbericht", the process designer. There is one link per destination per page. The plan act `decision` ("Zur Entscheidung") is a head link while the plan waits for a decision; it opens the decision in place beside the plan (003 §1, §5).
- **Row:** `RowLink` over the row. The row's acts are a `RowMenu ⋯` at its right end, as the last element after the tags (006), and the menu opens **at the row**. The act panel above the list and the text "Aktionen" go. **One exception, from the agreed `shared/project-explorer.adoc#project-row`:** a ready project's row keeps the visible act "Füge einen Plan hinzu" before its `RowMenu`; on a project that is not ready it is shown disabled with its reason (`shared/project-explorer.adoc:76`).
- **List column head:** the lens's create act ("Starte einen Plan", "Füge ein Projekt hinzu"; primary only while no object column is open) and the `SegmentedControl` for mode / grouping. The empty state repeats the same act only while the list is empty, and then the head shows none.
- **Back:** there is no in-page Back. Up and the breadcrumb (003) cover it. Back exists only inside wizards and dialogs (ghost, footer left), plus the one in-place return of 003 §4 ("Zurück zum Job", a ghost act in the head of an in-place object column).
- **View-local controls** (jump to now, follow mode, expand all) sit in the head of the column they act on (the phase tree's head), not in the object's act bar.

### 4. Labels
- **Acts:** imperative + object, in DE the du-form already used ("Starte den Plan", "Halte den Plan an", "Prüfe erneut", "Beende den Chat"; without object only where the object is obvious: "Speichere"; `texts-and-languages.adoc:87`), sentence case, short. **Navigation links** name their destination as a noun ("Abschlussbericht", "Einstellungen", "Prozess-Designer"). The plan's settings act is "Plan-Einstellungen", so "Einstellungen" names only the machine settings in the rail (001).
- **One key per act:** back, close, cancel, save, retry, later and the open-report act each have one shared key. The duplicates and the two decomposition key families are merged. "Retry" is "Versuche es erneut" everywhere. Reconnecting is its own act.
- **"…"** if and only if a dialog that asks for more follows (all confirmations, rename, upgrade). Never on a link.
- **No clash between acts:** the plan act "Cancel" becomes "End the plan…" / "Beende den Plan…", with `control.end.title` ("{plan} beenden?") and `control.end.action` renamed with it; it stays distinct from abandon ("Gib den Plan auf"). `control.end.action` is an ICU select over cancel / abandon / integrity; only its cancel branch changes. The epic's "Cancel" (`control.cancel_epic.*`, "{epic} abbrechen?") becomes "Beende das Epic…" in the same way. Both are shared texts: the terminal's designs (`terminal-client/19-actions.adoc`, `22-texts-actions.adoc`) change with them. Dialog "Cancel" / "Abbrechen" then means only dismiss.
- **No glyph labels** (`↑ ↓ ↻ × +`) without an icon and accessible name from the catalogue. No literals.
- **One label = one act:** two acts never share a label, and an act carries the same label in every view.

### 5. States (in the components, so every use gets them)
- **Hover** on every interactive element. **Pointer** only on interactive elements. A `RowLink` row highlights as a whole.
- **Focus-visible** ring (`--focus-ring`) on everything, including menu items, `RowLink` (on the row) and segmented options.
- **Disabled** always with its reason (`disabledReason`), never silently greyed.
- **Busy** through `Button loading`, never a text swap.
- **Pressed / chosen / current:**
  - `aria-pressed` for a `Toggle`;
  - `aria-checked` for a `SegmentedControl` option;
  - `aria-current="true"` for the selected row of a list column;
  - `aria-selected` for `Tabs`.
- **Targets:** at least 24 × 24 px on desktop and 44 × 44 px below 768 px, for every control including icon buttons, stars and the row menu.
- **`IconButton`** is icon-only everywhere (tooltip + accessible name). A labelled button with an icon is a `Button` with `icon`. `components.adoc:48` stands, and `IconButton.svelte:120-130` changes.

### 6. Clickable objects
- **A row or card is clickable if and only if it opens an object**, and then the whole row is a `RowLink`. Hover, pointer and focus are on the whole row. Nested controls (`RowMenu ⋯`, favourite toggle, copy chip) are separate targets above the link and never trigger it.
- **A block that does not open anything** has no hover lift, no pointer and no card shadow that suggests a click.
- **Applied:**
  - Running work rows → `RowLink` to the plan/epic;
  - Projects rows → `RowLink` to the one project page (003);
  - the "Laufende Arbeit" link becomes the project page's link to Running work scoped to the project;
  - Decisions/Reports list rows → `RowLink`;
  - Epic workstream rows → `RowLink` (EpicView's `.plan-card` is already close to this).
- **Lists of the Overview project column, the Start project list and the decision list** are list-column selections (003), i.e. `RowLink`s with `aria-current`, not `role="tab"`.

## Agreed documents that change
- `web-app/components.adoc`: the set of section 1, including `Link`, `ExternalLink`, `RowLink`, `SegmentedControl` and `Toggle`, and the rules of 2–6. Components that stay unused are removed from the doc in the same package.
- `web-app/interaction-affordances.adoc#acts`: row acts in `RowMenu` at the row, object acts in the head's `ActionBar`, both from `byState`, plus the fixed places of 3.
- `web-app/README.adoc`: :351 (one primary; the danger entry is an overflow item, not a red link), :448 (segmented control), and the Back rule (003).
- `shared/texts-and-languages.adoc`: verb/noun rule, one key per act, the "…" rule, "End the plan…".
- `shared/work-actions-texts.adoc`: `control.end.*` and `control.cancel_epic.*` renamed ("End the plan…", "End the epic…").
- `terminal-client/19-actions.adoc`, `terminal-client/22-texts-actions.adoc`: the same renames for the terminal (shared texts, both clients).
- `shared/project-explorer.adoc`: project rows (`RowLink`, `RowMenu`, the visible "Füge einen Plan hinzu"), and the release dialog footer (:122) per the footer and "…" rules.
- `web-app/README.adoc:351`: coordinated with 006 (one edit; see `design-review-010.md`).

## Affected plans
- **Held per 003 (adopt this when restarted):** PLAN-52 (rows as `RowLink`, row menu `IconButton ⋯` at the row, no act panel), PLAN-55 (plan head: one `ActionBar` with a primary; "Prozess gestalten" / "Abschlussbericht" as head links; "Spring dorthin" in the phase-tree head; no "Zurück"), PLAN-58 (epic head the same), PLAN-59 (project rows as `RowLink`, acts from `PROJECT_ACTS` via `RowMenu` instead of its deliverable 4's `ActionBar` on cards, release / unenrol in the overflow through `ConfirmDialog`, "Füge einen Plan hinzu" visible on a ready project's row as agreed).
- **PLAN-49 (What waits):** its rows are `RowLink`s.
- **PLAN-51 (Decisions):** choices per 2, one primary, list rows `RowLink` + `aria-current`, merge the two "Later" keys.
- **PLAN-53 (start wizard):** bare buttons → `Button` on `WizardPage`, one primary per step, the duplicate "Choose"/"Add" labels resolved.
- **PLAN-64 / PLAN-66 (onboarding):** "Abandon" into the overflow through `ConfirmDialog`. Dialogs via `Dialog`.
- **PLAN-62 (plan phase workflows):** tree expander and node selection as one `RowLink` plus a disclosure button, with `aria-current` for the selected node.
- **PLAN-85 (theme/language in browser settings):** the duplicated language toggle in `BottomBar` (`:169`, `:212`) goes with the extraction.
- **PLAN-88 (designer panes):** `.intake-mode` → `SegmentedControl`, hand-rolled dialogs → `Dialog`, the two "Open" labels named apart.
- **PLAN-72 (token cleanup lint):** a lint that fails on raw `<button>`/`<a>` and on the ad-hoc classes outside `lib/components`, and on `href="#"`.
- **PLAN-73 (text guard):** the literals and glyph labels above as expected failures.
- **PLAN-65 (verify web work views), PLAN-70 (light theme):** check hover, focus and the danger dialog in both themes.
- **PLAN-74:** closes the docs.

## Suggested action (the epic decides)
1. **Design** (session D1 of `design-review-010.md`, together with 006 and 008): the set, hierarchy, places, labels, states and clickable-object rules above, then operator review to `AGREED`.
2. **Foundation (web):**
   - `Link`, `ExternalLink`, `RowLink`, `SegmentedControl`, `Toggle`;
   - `RowMenu` on the `ActionBar` menu;
   - the plan act `decision` as a head link in the plan's object head;
   - the label keys merged;
   - the lint guard of PLAN-72.
3. **Apply per view** through the plans above. The views no staged plan owns go into one package: System (one primary, the upgrade act once, dialogs), Cleanup (delete via overflow + `ConfirmDialog`, `Back` → Up), CompletionReport, Reports (ghost-as-link → `Link`), decomposition (one implementation, one key set). Settings / MachineSettings belong to PLAN-60, the breadcrumb to the D2 engine, the bottom bar to PLAN-49 (`design-review-010.md`).

## Process
The design of this message is produced **before any implementation**, in its own Claude Code session: design session **D1 (component vocabulary)** of `design-review-010.md`.
- That session sets the documents it creates or changes to `Status: DRAFT`, reviews them (`ref-documentation` docs review, `ref-asciidoc` validate and verify-links) and opens a pull request.
- The operator sets them to `AGREED` as its last commit, and the pull request is merged on the operator's word.
- Only after that merge do the implementation plans named above start or restart.

Order, document ownership, open points, what each plan waits for, and the merged per-plan instructions are in `design-review-010.md`.
