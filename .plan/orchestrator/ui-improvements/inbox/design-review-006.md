envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T17:01:26Z
revision=5
amended=2026-11-06T19:36:26Z

# Tags: one closed tag system for state, reason, count, id, phase and kind

Sender: operator design review, 2026-11-06. A full review of every tag-like element (badges, chips, pills, counts, state words) in the web app, in the browser against the mock runtime and in the code, checked against the agreed designs.

## Operator feedback
Review the tags used everywhere. Are their structure, location and content consistent? (First checked: no staged plan owns this. PLAN-104 covers the terminal console badges only.)

## Current state
**The agreed components are there but bypassed.** `components.adoc` defines `StatusLabel` (dot + word, no box, :89), `CountBadge` (rail, tabs, group heads only, :98) and `Chip` (ids, branches, versions, copy on click, :107).
- `StatusLabel` is used once (`CompletionReportView.svelte:141`).
- `CountBadge` appears only inside the unused `Tabs`.
- `Chip` is used by no view (only `routes/dev/components`).
- Every other tag is ad-hoc `<span class="badge …">`. `.badge` is redeclared, scoped, in at least five views with different shapes: `radius-full` in Overview and System, `radius-sm` in ProjectExplorer, Cleanup and Decision, and borders only in ProjectExplorer.
- **There is no global `.badge` rule**, so in PlanView, EpicView, NodeNarrative, PrAdoptPanel and EpicDecompositionView the badge classes have no CSS at all. In the browser the plan head shows "execute läuft 2 h 10 min" as plain text.

**Structure, location, content and colour diverge:**

| Concept | Renderings found | Examples |
|---|---|---|
| Needs-you count `! n` | 3 colours, 3 formats | Running-work column: red fill "! 2" (`OverviewView:617-626`, `badge--attention` = danger). Projects: amber outlined "! 2" (`ProjectExplorerView`, `badge--warning`). Plan card: "! Entscheidung" without count. Rail: bare number in brand violet (`nav-rail__count`). The agreed form is `! 2` warning (`README.adoc:373`). The key `overview.needs.count=! {count}` is never used, and "!" is hard-coded. |
| State | 5 markups | `badge--state` (Overview: running = green text on grey, waiting = amber, failed / held / done / staged = no rule, i.e. grey), `status-tag` (System), `StatusLabel` (completion report, running = blue `--color-running`), unstyled text (Plan / Epic head), a bare wire string (`NodeNarrative:109`). |
| Hold | in two places at once | Side-job row: "! Landen fehlgeschlagen" red at the **left** + "fehlgeschlagen" grey at the **right** + "· fehlgeschlagen" in the sentence: one fact three times. Plan row: hold at the right before phase and state. |
| Kind | 4 forms | Glyph span + grey pill "Plan" / violet pill "Epic" before the title (Overview). The glyph inside the pill text "◇ epic" (ended table). A pill after the title (Epic head). Capitalised state words "Plan / Epic / Logs" (Cleanup). The `kind.*` SVG icons are unused. |
| Phase | 3 forms, English wire tokens | Mono pill with icon "execute" / "finalize" (Overview). Plain text (Plan head). "execute · implement" without icon (Decisions). No `phase.*` catalogue key. `workflow-visual-language.adoc:451-476` allows "the bare runtime token", against `components.adoc:20-25` / `texts-and-languages.adoc` (no wire value on screen). |
| Id | none are Chips | Decision subject "plan fix-relay" violet mono (`badge-subject`), task id plain semibold, workstream id plain mono, unit id `.chip--unit`, epic id breadcrumb text. |
| PR | 4 renderings | Boxed mono chip in the plan's action bar (top right, between buttons), plain mono link (Overview), semibold primary link (completion report), small accent link (Epic). |
| Counts | 8 shapes | `CountBadge`, `nav-rail__count`, `bottom-bar__count`, `unread-pill` "3 offen" (violet pill next to the Decisions title), `unread-count` "5 ungelesene Berichte" (text), `section-count`, `section-badge`, `badge--counts` "3 laufen · 3 vorgesehen". |

**Location.**
- The hold marker is right in plan cards and left in the project column and side jobs.
- Kind is before the title in Overview and after it in the Epic head.
- Phase / state are in a right-hand group in rows and after the title in heads.
- The PR sits in the action bar (plan), in the detail line (Overview) or after the title (completion report).
- "Completion review ready" is at the far right of the epic head, apart from the other status.
- The decision's phase tag is in the detail but not in the list.
- "Aktionen" sits mid-row in the epic row and at the right in the plan row.

**Content.**
- **Raw wire values and literals on screen:**
  - the fallback "! attention" (`OverviewView:945,1051,1137`);
  - the epic card prints the raw `needs_operator_kind`, while the plan card translates it;
  - `{cut.status}` (`EpicDecompositionView:267`);
  - `{task.status}` (`NodeNarrative:109`);
  - `{asking_phase} · {asking_role}` (`DecisionView:887`);
  - "attempt N" (`PlanView:962,1024`), although `plan.node.attempt` exists;
  - task role, harness and model as wire;
  - the technical key in "Einrichtung braucht: credential sonar" (Projects).
- **Duplicate wording in the plan tree:** "Phase phase init" (kind word + a name that repeats the kind).
- **Case:**
  - kind words: "plan" in EN `overview.kind.*`, "Plan" in DE;
  - `state.node_kind.phase=Phase` vs `plan.node.kind.phase=phase`;
  - "Automatic: on" capitalised among lowercase tags;
  - `state.model.ok=OK` vs `system.models.ok=ok`.
- **One state, several words:** DE "held" is "aufgehalten" (`state.plan.held`, `overview.state.held`) and "zurückgehalten" (`state.epic.held`, `overview.needs.held`).
- **Duplicate keys for one meaning:**
  - `overview.needs.waiver` / `waiver_request`;
  - `waits_for_you` / `operator_wait`;
  - `overview.state.*` / `state.plan.*` / `epic.views.row.*` / `overview.epic.status`;
  - `decision.reversibility.*` / `state.decision.*`.

  This breaks `texts-and-languages.adoc:112` ("the shared key is the authority").
- **One fact twice in a row:** the side-job state is in both sentence and tag, and the plan card shows the phase in its tag and again in "in execute 25 min".

**Colour.**
- "Needs you" is red, amber or violet depending on the view.
- "running" is green in badges and blue in `StatusLabel` (`--color-success-subtle` is aliased to `--color-bg-subtle`, `tokens.css:113`, so running = green text on grey).
- "failed" is grey next to a red hold.
- **Brand violet carries about ten meanings:** decision subject, "this one" device, recommended/default, tool-call tag, cleanup banner, open-decisions pill, unread reports, rail counts, worker busy. That is against `README.adoc:351` (the brand marks the one primary action).
- **Amber carries about nine meanings:** off-track, retry, recovering, untrusted, setup needs, costly, waiting, `! n`, waiting_for_you.
- **Red** carries attention, irreversible, cancelled, error, dropped and conflict.

## The design

### 1. A closed set of tag types, one component each
| Type | Component | Shape | Says |
|---|---|---|---|
| **State** | `StatusLabel` | dot + word, **no box** (as agreed, `components.adoc:89`) | the object's lifecycle state: running, waiting for you, held, failed, done, staged, ended, … |
| **Reason** | `ReasonBadge` (new; the reason layer of `design-review-003.md`) | pill `radius-full`, starts with `!`, word | why the object needs the operator: the item kinds of `shared/needs-and-find.adoc:40`, except on a row that is itself the decision (a plan or epic row shows its waiting decision as `! Entscheidung`) (waiver, landing failed, setup needs, credential, cleanup due, stalled, held, integrity hold, …), worded by the agreed `overview.needs.*` keys |
| **Count** | `CountBadge` | pill `radius-full`, number | a count in the rail, a tab or a group head only. An attention count starts with `!` (`components.adoc:25`). |
| **Id** | `Chip` | mono, `radius-sm`, copy on click | plan/epic/task/unit id, branch, version, machine name |
| **PR** | `PrChip`: an `ExternalLink` drawn in the chip look | mono chip, opens the PR; it does not copy (the copy act sits beside it as an `IconButton` where needed) | "PR #214" |
| **Kind** | `KindMark` (`design-review-003.md`) | icon + word, no box | the kind: project, epic, plan, side job, decision, report, machine item |
| **Phase** | `PhaseLabel` (new) | phase icon + word, no box | the phase the object is in |

Everything else that looks like a tag today becomes **text in the detail line**, not a box:
- epic mode "Automatic: on";
- scope "2 of 3 at once";
- counts like "3 running · 3 staged";
- "frees 1.2 MB", worker counts, "3 of 4 votes";
- reversibility ("can be undone") as the decision's detail sentence with a neutral icon.

"Recommended" / "default" / "this one" are **markers**, drawn as a small neutral icon + word, not brand-coloured pills. `.badge*`, `.status-tag`, `.unread-pill`, `.plan__tag`, `.entry-kind-tag`, `.task-role-tag`, `.pr-badge-link`, `.chip--unit` and the other ad-hoc tag classes go.

### 2. One fixed place
- **Row** (the one row shape of `design-review-003.md`): `KindMark · title · Chip(id)` … `ReasonBadge · PhaseLabel · StatusLabel · time · acts` (right group, in this order).
  - **The reason comes first** (`README.adoc:385`).
  - At most one tag per type per row.
  - The row's sentence never repeats a tag's fact.
  - Acts are always the last element on the right.
- **Object head:** `KindMark · title` / `Chip(id) · PrChip · head links · PhaseLabel · StatusLabel · state sentence` (the same phase-then-state order as rows; the path is the top-bar breadcrumb of 003 §4).
  - The PR chip sits in the id line, never in the action bar.
  - "Completion review ready" is a `ReasonBadge` (a new `overview.needs.*` key) in the same line, not a far-right button label.
- **Group head** (a workstream, a list column head, a rail entry, a tab): name + `CountBadge` right-aligned (`components.adoc:98`: never in a row). A row that needs the operator several times carries the count inside its `ReasonBadge` (`! 2 · Entscheidung`, `shared/plan-overview.adoc:69`).
- **Detail and list show the same tags.** The decision's phase is in both or in neither.

### 3. One meaning per colour
- **State tones** (`StatusLabel` dot): running = `--color-running` (blue), waiting for you / held = attention (amber), failed = danger (red), done / merged = success (green), staged / ended / cancelled = neutral.
- **Reason:** amber for every reason, red only for landing failed and integrity hold.
- **Count:** `! n` attention (amber) wherever it appears (rail, tabs, column heads). A plain count (unread reports) is neutral.
- **Brand violet** marks only the primary action, focus and the selected item. It is never an id, count, marker or tag.
- **Kind** colour stays on the kind icon only (`design-review-003.md`).
- `--color-success-subtle` gets its own value instead of the `--color-bg-subtle` alias.
- Danger and attention are not used for reversibility, untrusted sources or retries. Those are detail text with an icon.

### 4. One word per value, never a wire value
- **Every tag word comes from the catalogue:** no "attention", no "attempt N", no `{task.status}`, `{cut.status}`, `{asking_phase}`, role or harness wire, no "credential sonar". The setup-needs reason gets a catalogue word with the provider's display name.
- **The shared key is the authority:**
  - states only through `state.*` via `displayState`;
  - reasons only through the agreed `overview.needs.*` keys (`shared/needs-and-find.adoc:40`); no new key family;
  - the duplicates listed above are removed. Where two keys exist because the contract accepts two wire spellings (`waiver` / `waiver_request`, `waits_for_you` / `operator_wait`; `shared/needs-and-find.adoc:40`), `pm-api` first normalises the spelling, then one key remains, for both clients (the terminal uses them, `PmConsole.java:741`, `OverviewFormatting.java:197`);
  - DE "held" has one word everywhere.
- **Phases are English product terms in both languages:** init, refine, outline, plan, execute, finalize (and the role words shown in tags). They come from `phase.name.*` catalogue keys (not `phase.*`, which are the icon ids) whose DE value equals EN, rendered by `PhaseLabel`, so the catalogue owns them and the text guard sees no wire value. `workflow-visual-language.adoc:451-476` changes from "bare runtime token" to "the phase word of the catalogue".
- **Case:** a tag word follows its language's sentence rules: DE nouns capitalised ("Plan", "Epic", "Nebenarbeit" as a new key; the catalogue has only `overview.group.alongside=nebenher`), EN lowercase ("plan", "epic", "side job"; `shared/plan-overview.adoc` uses the word `plan`). `KindMark` words are nouns.
- **The `! n` format** is `! 2` in a `CountBadge`. In a row's `ReasonBadge` it is `! Entscheidung`, or `! 2 · Entscheidung` when several (`shared/plan-overview.adoc:69`). It uses the key `overview.needs.count`, not a hard-coded `!`.
- No glyphs in tag text (`◇`, `▸`, `←`, `↳`): `KindMark` and icons replace them. The plan-tree node shows "init" with the phase icon, not "Phase phase init".

### 5. Examples (today → this design)
A box means "needs you", a dot means "for information". Calm rows contain no box. The only box in a row is the reason.

```
Running work, plan row
today:   ▸ [Plan] Drop the legacy endpoints   [! Entscheidung] [⚑ finalize] [wartet auf dich]
design:  ▤ Plan  Drop the legacy endpoints    [! Entscheidung]  ⚑ finalize  ● wartet auf dich

Running work, side job (one fact once)
today:   [! Landen fehlgeschlagen]  lande CI-Fehler · fehlgeschlagen          [fehlgeschlagen]
design:  ⇄ Nebenarbeit  lande CI-Fehler                [! Landen fehlgeschlagen]  ● fehlgeschlagen

Running work, calm row
today:   └ Fix Relay Transport                     [▶ execute] [läuft]   Aktionen
design:  └ ▤ Fix Relay Transport                    ▶ execute  ● läuft   ⋯

Projects row
today:   [bereit] [! 2]  3 Pläne · 1 Epic
design:  ● bereit  [! 2]  3 Pläne · 1 Epic
today:   [Einrichtung braucht: credential sonar] [! 1]
design:  ● Einrichtung offen  [! Zugangsdaten]          (the provider is named where the act opens, shared/needs-and-find.adoc:39)

Plan head
today:   Fix Relay Transport  ▶ execute läuft 2 h 10 min        (unstyled)
design:  Fix Relay Transport
         fix-relay  PR #214  ● läuft · ▶ execute · seit 2 h 10 min

Decision
today:   [plan fix-relay] [execute · implement] [blockiert den Plan] [lässt sich rückgängig machen]
design:  fix-relay  ▶ execute · implement
         detail line: blockiert den Plan · ↺ lässt sich rückgängig machen
```
`[…]` = box (ReasonBadge, CountBadge), `●` = StatusLabel dot in its tone, `fix-relay` = Chip, `PR #214` = PrChip, `▤ Plan` / `⇄ Nebenarbeit` = KindMark, `▶ execute` / `⚑ finalize` = PhaseLabel. The state column has a fixed width so the dots line up in a list. A failed or stalled row carries its ReasonBadge as well, where the runtime lists a reason, so it stands out even though its state is only a dot. A visible act in a row (the agreed "Füge einen Plan hinzu" of 007 §3) is the one exception to "only boxes are reasons".

## Agreed documents that change
- `web-app/components.adoc`: the tag family (StatusLabel, ReasonBadge, CountBadge, Chip/PrChip, KindMark, PhaseLabel, markers) with the slot order and colour rules above.
- `web-app/README.adoc`: the state/badge table (:370-386), radius rule (:204), machine pill (:141), brand rule (:351).
- `web-app/workflow-visual-language.adoc:451-476`: phase word instead of the bare token.
- `shared/texts-and-languages.adoc`: the phase vocabulary, the one-key rule applied to states and reasons.
- `shared/plan-overview.adoc:65-69`: kind = KindMark.
- `shared/needs-and-find.adoc`: the item kinds become reasons (`ReasonBadge`), the row's KindMark is the object's kind, `decision` items are Decision rows.
- `web-app/README.adoc:351`: coordinated with 007 (one edit; see `design-review-010.md`).

## Affected plans
- **PLAN-52 web-running-work (staged):** "the project sidebar becomes a filter chip row" conflicts with the column navigation of `design-review-003.md` (the project column is the lens's list column). **Hold** with 55/58/59/83. Its row content (kind icon, title, state, phase, since, …) follows the row slots above.
- **PLAN-55 / PLAN-58 (plan / epic page, held per 003):** the heads take the head slots above (no unstyled badges, PR chip in the id line).
- **PLAN-49 (What waits):** rows = KindMark + ReasonBadge (the reason layer).
- **PLAN-51 (Decisions):** subject as Chip, phase in list and detail, reversibility as detail text, "3 offen" as CountBadge in the group head.
- **PLAN-59 (Projects, held per 003):** readiness = StatusLabel, "setup needs" = ReasonBadge with a catalogue word.
- **PLAN-62 (plan phase workflows):** tree rows "icon, name without the kind, state word" match this design (PhaseLabel, StatusLabel). It removes "Phase phase …".
- **PLAN-72 token-cleanup-lint:** the ad-hoc tag CSS goes, and `--color-success-subtle` gets a value. The lint can forbid `.badge` outside the components.
- **PLAN-73 text-guard-failing:** the wire values and literals above are exactly what a failing text guard must catch. List them as its expected first failures.
- **PLAN-70 light-theme-pass:** the state tones and reason colours are checked in light and dark.
- **PLAN-74:** closes the docs after this.

## Suggested action (the epic decides)
1. **Design** (session D1 of `design-review-010.md`, together with 007 and 008): the tag family in `components.adoc` and the document changes above, then operator review to `AGREED`.
2. **Foundation (web, one package):**
   - `ReasonBadge`, `PhaseLabel`, `PrChip` and markers, with `StatusLabel` / `CountBadge` / `Chip` as they are;
   - the catalogue clean-up (one key per meaning, phase vocabulary, the `overview.needs.*` reasons, no duplicates);
   - a lint / test guard that fails on `class="badge` outside `lib/components` and on a wire value in a tag.
3. **Apply per view** with the plans above. Views no staged plan touches (System, Cleanup, Completion report, Settings, Source list, PR adopt, choice screens, EpicDecomposition, shell rail / bottom bar counts) go into one runbook-sized package.

## Process
The design of this message is produced **before any implementation**, in its own Claude Code session: design session **D1 (component vocabulary)** of `design-review-010.md`.
- That session sets the documents it creates or changes to `Status: DRAFT`, reviews them (`ref-documentation` docs review, `ref-asciidoc` validate and verify-links) and opens a pull request.
- The operator sets them to `AGREED` as its last commit, and the pull request is merged on the operator's word.
- Only after that merge do the implementation plans named above start or restart.

Order, document ownership, open points, what each plan waits for, and the merged per-plan instructions are in `design-review-010.md`.
