envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T19:15:23Z
revision=5
amended=2026-11-06T20:06:29Z

# Design review 2026-11-06: process, order, document ownership and per-plan instructions

Sender: operator design review, 2026-11-06. The index of `design-review-001.md` … `design-review-009.md`: how their design is produced, in which order, which design session owns which document, what each affected plan waits for, and one merged instruction list per plan, so each plan spec is amended once.

## Process: design first, in separate Claude Code sessions
- **New design is done before the implementation.** Each design session below runs in **its own Claude Code session** (its own plan, started with its own prompt), not inside an implementation plan and not in this review session. (Staged plans that already edit design text in their own scope, PLAN-60 `texts-and-languages.adoc:224` and PLAN-85 `README.adoc`, keep doing so, sequenced after the session that owns that document.)
- **Status and merge.** The documents follow the lifecycle of `doc/design/README.adoc:20` (`DRAFT` → `AGREED` → `FINAL`).
  - A design session sets every document it creates or changes to `Status: DRAFT` in its pull request.
  - The operator reviews it and sets it to `AGREED` as the last commit of that pull request.
  - The pull request is merged only on the operator's word (`project-setup.adoc` § From a Branch to `main`). **A design session's plan runs without auto-merge:** its finalize stops once the pull request is green and the review comments are answered, reports, and waits for the operator's `AGREED` commit and word to merge.
  - **The gate for a waiting plan is the merge of its session's pull request with the documents at `AGREED`**, not the status the documents have today.
  - `FINAL` is set once the implementation is complete (PLAN-74 closes the docs).
- **Review in a design session:** the `doc/design/` changes with `ref-documentation` docs review plus `ref-asciidoc` validate and verify-links, as PLAN-74 does. Changes in `plan-marshall-documentation` and `pm-mcp-core` (D4's roadmap, requirement and specification changes) are reviewed with `doc-review` and `trace.py` in those repositories.
- **Where decisions are recorded:** the operator's decisions of a session are written into its design documents. Points still open are tracked in `plan-marshall-documentation/doc/discussions/open-questions.adoc` (`project-setup.adoc` § Open Questions). The operator's approval of the `marked` dependency (009) is recorded in `chat.adoc`.
- **Running plans:** D1 edits `terminal-client/19-actions.adoc` after the running PLAN-97 has landed; D3 and D4 edit `web-app/onboarding.adoc` after the running PLAN-104 has landed; the 001 and 002 runbooks start after the running PLAN-106 has landed (or merge `main` and rebase before landing).
- **Prototypes:** where a message asks for one (the column window of 003, the chat of 009, the icon review sheet of 005), it is built inside the design session on the mock runtime, throw-away, to settle the design before `AGREED`.
- **Now, without a design session:** 001 (rail label, icon, and the shared text `control.plan.settings` → "Plan-Einstellungen" with `shared/work-actions-texts.adoc:41-43` and the terminal's plan menu in the same pull request), 002 (remove the start-page toggle) and the code part of 004 (remove the layout modes) ship now as runbook packages. The design text of 004 is removed by D2.

## Design sessions, order and ownership
| # | Design session | From messages | Creates / changes (owner) | Needs first |
|---|---|---|---|---|
| D1 | **Component vocabulary** | 006, 007, 008, and KindMark of 003 §6 | `web-app/components.adoc`: tag family (StatusLabel, ReasonBadge, CountBadge, Chip, PrChip, KindMark, PhaseLabel, markers), interaction set (Button, IconButton, ActionBar, RowMenu, Link, ExternalLink, RowLink, SegmentedControl, Toggle, Tabs, Dialog, ConfirmDialog, Breadcrumb), fields and forms (Field … FormStatus, Compose); `web-app/README.adoc` :141, :204, :297, :351 (the one edit for 006 + 007), :370-386, :448, control tokens; `shared/texts-and-languages.adoc`; `web-app/workflow-visual-language.adoc:451-476` (phase word); `shared/plan-overview.adoc:65-69`; `shared/needs-and-find.adoc` (item kinds → reasons); `web-app/interaction-affordances.adoc#acts`; `shared/project-explorer.adoc` (rows, release dialog); `shared/work-actions-texts.adoc` (`control.end.*`, `control.cancel_epic.*`); **the terminal's designs for every shared text it changes** (`terminal-client/19-actions.adoc`, `22-texts-actions.adoc`; `doc/design/README.adoc` "Both clients, one state") | — (D1 writes its rules in terms of "the object shown" and "the selected row"; D2 adds the column clauses and the links to `navigation.adoc`) |
| D2 | **Navigation** | 003, 004 (design text) | **new** `web-app/navigation.adoc` (§1–10, width table); `web-app/components.adoc` (column templates: TriagePage as the column engine, ObjectPage regions in the object column; after D1); `web-app/README.adoc` :19, :138 (rail entries), :144, :182, :211-239, :481ff; `web-app/view-density-and-filtering.adoc` (layout modes removed per 004; the Projects filter, live region and key pattern per 008); `shared/plan-view.adoc:43,182`; `shared/plan-overview.adoc:161-169`; `shared/cleanup.adoc:139`; `shared/completion-report.adoc:104`; `web-app/needs-and-find.adoc` (:29, bottom bar, rows); `shared/project-explorer.adoc` (the one project page; after D1's row edit); `web-app/interaction-affordances.adoc` (keys; after D1's #acts edit); the README routes for `/settings` tabs (PLAN-60) and `/settings/browser` (PLAN-85) in the one address scheme; `doc/plans/web-moving-between-plans.adoc` (PLAN-83: task 1 folded into the engine, anchors updated) and its index row | D1 (KindMark, RowLink, row slots); the column-window prototype |
| D3 | **Brand and mascot** | 005 (001 is code-only) | `web-app/brand-mark.adoc` (rewrite); `web-app/workflow-visual-language.adoc` (colour exception; after D1); `web-app/components.adoc` (EmptyState mascot option, BrandMark, WaitingMark; after D1); `web-app/README.adoc` (rail head and rail foot sentence of :138, after D2); `web-app/entry.adoc`; `web-app/onboarding.adoc` (figures); the pose briefs (artwork supplied by the operator or a named image tool) | D1 (EmptyState, tokens); draft `kind-project` / `kind-plan` SVGs for the review sheet; runs beside D2 |
| D4 | **Chat and voice** | 009 | **new** `web-app/chat.adoc`; `shared/work-actions.adoc:350-432`; `shared/work-actions-chat-texts.adoc`; `shared/decisions.adoc` / `decisions-texts.adoc`; `shared/consents.adoc` (speech); `web-app/onboarding.adoc` and `web-app/machine-settings.adoc` (role `transcribe`; after D3); `web-app/components.adoc` (Chat, ChatTurn, ProposalCard, VoiceInput, markdown renderer; after D1); in `plan-marshall-documentation`, first: the roadmap entry, PM-WF-24 (`requirements/02-workflow.adoc:384`, the speech exception to "sent nowhere else"), the onboarding use cases, `runtime-model/01-processes-and-transport.adoc` (routes), `runtime-model/06-event-catalogue.adoc` (`chat.*`), `runtime-model/05-mock-runtime.adoc`, `cli-and-security/03-web-server.adoc` (routes, CSP, `Permissions-Policy`); the terminal's onboarding roles transcript and consent listing (both clients); the contract and mock design for this repository; requests to `plan-marshall/pm-mcp-core` (streaming `chat.reply`, `transcribe`; its state catalogue changes before `AGREED`, or `chat.adoc` marks those parts pending) and `plan-marshall/plan-marshall-mcp` (routes, events, headers) | **Streaming chat and voice are in the staging window (operator, 2026-11-06)**; D4 records the roadmap entry and changes the requirement and specification first (`project-setup.adoc` § Planning the Work); D1 (Compose, ExternalLink, chip look); D2 (§10 side column); D3 (WaitingMark); the chat prototype on the realistic mock. |

**Order:** D1 → D2 and D3 → D4. D3 runs beside D2 for `brand-mark.adoc`, `entry.adoc`, `workflow-visual-language.adoc` and the review sheet; its `README.adoc` and `components.adoc` edits are made after D2's pull request has merged, and D3's pull request merges after D2's.
- Each session edits only the lines it owns.
- A document owned by two sessions is edited in the order given in the table, never in parallel: `README.adoc`, `components.adoc`, `plan-overview.adoc`, `project-explorer.adoc`, `interaction-affordances.adoc`, `onboarding.adoc`, `workflow-visual-language.adoc`, `view-density-and-filtering.adoc`.
- PLAN-85's own `README.adoc` edit (rail foot, theme, More sheet) and PLAN-60's settings routes run after D2 (and D3 for PLAN-85).

## Operator decisions (2026-11-06) and remaining open points
- **D1, kind tints:** only Project, Epic and Plan are tinted. Side job, Decision, Report and Machine item stay neutral (shape + word). German buttons use the du-imperative ("Speichere", `texts-and-languages.adoc:87`).
- **D2, bottom bar:** What waits · Running work · Decisions · More. Reports moves into More, with its unread count shown on More. *Open:* the width table, from the prototype.
- **D3, brand:**
  - **Icon:** chosen on the review sheet (both candidates drawn). *Open until the sheet exists.*
  - **Provenance:** the mascot is the operator's own work, free to use and modify. brand-mark.adoc records it.
  - **Rail-foot bear:** wanted, as a reversible test with a fixed, contrast-checked opacity.
  - **Poses:** the D3 session writes the pose briefs; the artwork is supplied by the operator or an image tool the operator names, as one set for review. The source image is used until a pose is accepted.
- **D4, staging window:** streaming chat **and** voice (the `transcribe` role and microphone) are both in the window now. The roadmap entry, the requirement and the specification change first.
- **Later:** syntax highlighting (`shiki` `codeToTokens` or `prismjs`), a separate dependency decision.

## Per-plan instructions (merged from 001–009)
**What "waits for" means:** a plan waits for the merge of the named design sessions **and** for the implementation package that builds what it uses:
- D1 → the D1 foundation package;
- D2 → the D2 navigation engine;
- D3 → the D3 brand assets;
- D4 → the D4 web chat (and the voice package for voice).

A plan never builds these components itself. **Chat adoption:** the D4 web chat package builds `Chat` and adopts it on the surfaces no staged plan owns (decomposition, epic review, project-onboarding drawer, start entry); the chat parts of PLAN-51, 52, 55, 58 and 59 are adopted by each plan after the D4 web chat package. Voice parts belong to the D4 voice package. (Running PLAN-106 rewrites `WorkControlPanel` act lists; line references into it in 007 and 009 are re-read when used.)

- **PLAN-29 verify-web-texts (staged).** *Waits with PLAN-85.*
- **PLAN-36 cli-needs-verb (staged).** It builds on `overview.needs.*`. If it has landed, the terminal package's normalisation updates its `NeedsCommand` too.
- **PLAN-41 verify-web-design-system (staged).** *Hold.* It would verify the component set D1 replaces; it is re-grounded after D1–D3.
- **PLAN-45 error and loading pattern (staged).** *Waits for: nothing new.* Field, form and chat errors use the same `displayError` codes (PLAN-09) as its load errors (008, 009).
- **PLAN-49 web-attention-inbox (staged).** *Waits for: D1, D2, D3.*
  - `pm_start_route` is gone, so it does not take the toggle over (002);
  - What waits is a launcher at `/`, which becomes an `AppRoute` (003);
  - rows: KindMark + ReasonBadge on `overview.needs.*`, `decision` items as Decision rows (003, 006), each row a `RowLink` (007);
  - the empty state uses the mascot "nothing waits" pose or the source bust (005);
  - the bottom bar What waits · Running work · Decisions · More (003).
- **PLAN-51 web-decisions-triage (staged).** *Waits for: D1, D2; its decision-chat part waits for D4.*
  - layout: list → one decision, the next decision after deciding is a `replaceState`; `Enter` decides a decision with a selected option (003);
  - it starts from Decisions without layout modes (the 004 runbook ships first);
  - tags: subject as Chip, phase in list and detail, reversibility and "blocks the plan" as detail text, "3 offen" as CountBadge in the group head (006);
  - interaction: options as `RadioGroup` cards and one primary "Entscheide"; the waiver's agreed answers "Erteile den Waiver" and "Lehne ab" both secondary, no preselection, `Enter` answers nothing, "Später" ghost (`decisions.adoc:174`); rows `RowLink` + `aria-current`; one "Later" key (007);
  - fields: "Notiz (optional)", own words as `Compose`, waiver reason `TextArea` with the one required text, "Entscheide" disabled with its reason, its "Entschieden: <Option>" line as the form's `FormStatus` (008);
  - chat: the decision chat in the new `Chat` with the agreed texts ("Antworte mit diesen Worten") (009).
- **PLAN-52 web-running-work (staged).** *Hold. Waits for: D1, D2; its ended-work chat waits for D4.* When restarted it puts Running work on the D2 engine: project list column, running / ended and by plan / by epic as URL modes, follow mode (003). Its filter chip row (deliverable 1) and its ended-work `DataTable` tab (deliverable 3) are dropped. Row slots (006), `RowLink` and `RowMenu` at the row (007), the ended-work chat read-only (009).
- **PLAN-53 web-start-wizard (staged).** *Waits for: D1; its chat part waits for D4.*
  - `Button` on `WizardPage`, one primary per step, the duplicate "Choose" / "Add" labels resolved (007);
  - fields via `Field`, Enter submits, errors under the field, documents mode as the shared `RadioGroup` (008);
  - the start-entry chat in `Chat` (009).
- **PLAN-55 web-plan-page / PLAN-58 web-epic-page (staged).** *Hold. Waits for: D1, D2; the chat column waits for D4.* When restarted:
  - the object column with its head and tabs; PLAN-55 drops the head path of its deliverable 1 (the breadcrumb is the only path) and its rule that a waiting plan's primary act opens the decision (it is a head link, 007 §3) (003);
  - head slots: KindMark · title / Chip · PrChip · head links · PhaseLabel · StatusLabel (006);
  - one `ActionBar` with the primary, "Prozess gestalten" and "Abschlussbericht" as head links, the plan's `decision` act as a head link that opens the decision in place, "Spring dorthin" in the phase-tree head, no "Zurück" (007);
  - the chat as a side column (009).
- **PLAN-59 web-projects-and-onboarding (staged).** *Hold. Waits for: D1, D2; its chat waits for D4.* When restarted:
  - the one project page and project rows as `RowLink` (003);
  - readiness as StatusLabel, "setup needs" as ReasonBadge (006);
  - acts from `PROJECT_ACTS` via `RowMenu` instead of the `ActionBar` on cards of its deliverable 4; "Füge einen Plan hinzu" visible on a ready project's row and disabled with its reason otherwise; release / unenrol through `ConfirmDialog` (007);
  - `FilterInput`, the release reason in `ConfirmDialog`, per-aspect settings by the settings-form rule (008);
  - the project-onboarding chat in `Chat` (009).
- **PLAN-60 web-settings-one-place (staged).** *Waits for: D1, D2* (its tabs follow D2's address scheme and routes table).
  - It owns merging `/settings` and `/settings/machine`, and starts after the 001 runbook (001).
  - Its form rule becomes the general settings-form rule: "Speichere" disabled until a change, changed marks, reset; device rename as an inline edit (008).
  - The `transcribe` setting and the speech-consent withdrawal belong to the D4 voice package.
- **PLAN-62 web-plan-phase-workflows (staged).** *Waits for: D1, D2.*
  - the phase tree as a column of the plan; its tree icons must not clash with the kind marks (003);
  - PhaseLabel and StatusLabel, no "Phase phase" (006);
  - a node row = `RowLink` + a disclosure button with `aria-current`; plain arrows stay with the tree (007).
- **PLAN-64 / PLAN-66 onboarding (staged).** *Waits for: D1, D3.*
  - welcome and done figures, the chosen icon in the header (005);
  - "Abandon" in the overflow through `ConfirmDialog`, dialogs via `Dialog` (007);
  - `SecretField`, `TextField`, `RadioGroup` / `CheckGroup`, the stray `/>` in `HarnessChoice.svelte:151-152` (008).
  - The role `transcribe` in the roles step belongs to the D4 voice package.
- **PLAN-65 verify-web-work-views (staged).** *Waits for: D1, D2, and the held PLAN-52/58/59; its chat check waits for D4.*
  - Its `doc/claude_review` checks (e.g. UX-V-01 bottom bar, UX-V-09 ended table, UX-V-12 settings redirect) are re-grounded against `navigation.adoc` and the D1 `components.adoc` before it runs.
  - It then checks hover, focus and the danger dialog (007) and the read-only chat (009).
- **PLAN-70 light-theme-pass (staged).** *Waits for: D1–D4 and their implementation packages.* It checks every visual of 001–009 in both themes: kind tints, state tones, reason colours, field look, mascot, chat. The route baselines are re-taken after the views move.
- **PLAN-71 verify-onboarding (staged).** *Hold.* It is re-grounded after D1 and D3, and runs after PLAN-64/66.
- **PLAN-72 token-cleanup-lint (staged).** *Waits for: D1, D3, D4 and the per-view packages, including "views no staged plan owns".*
  - Tokens: kind tints, `--color-success-subtle` with a value of its own, the diff tokens (003, 006, 009), the `--color-mascot-*` tokens (005).
  - Lints that fail on: `.badge` and ad-hoc tag classes; raw `<button>` / `<a>` / `<input>` / `<textarea>` / `<select>`; ad-hoc button classes; `href="#"`; `outline: none` outside `lib/components` (006, 007, 008). The D1 foundation adds them as warnings; PLAN-72, after the per-view packages, switches them to errors and keeps its token and stylelint work.
  - No `{@html}` in the markdown renderer (009).
- **PLAN-73 text-guard-failing (staged).** *Waits for: D1, the D4 web chat, the per-view packages (including the views no staged plan owns) and PLAN-70*, as PLAN-72. Its expected first failures are:
  - wire values in tags (006);
  - literals and glyph labels (007);
  - machine ids as labels, raw `e.message`, labels with colons or title case (008);
  - `start.*` keys reused in the chat instead of `control.chat.*` (009).
- **PLAN-74 web-design-docs-closure (staged).**
  - Its deliverable 1 (writing `web-app/README.adoc` structure, principles, visual language) is dropped: D1 and D2 write the README.
  - Its deliverables 3 and 4 (residue fixes in `components.adoc`, `RowMenu` at 390 px, `texts-and-languages.adoc#time`, `project-explorer.adoc`, `work-actions.adoc`) are staged as a new runbook, `web-design-docs-residue`, with no dependency on PLAN-72/73. It runs before D1, and not in parallel with PLAN-107 (both touch `RowMenu.test.ts`).
  - PLAN-74 keeps deliverable 2 (the proposal-status list) and setting `FINAL`, *after D1–D4 and their implementations*.
- **PLAN-75 verify-web-final (staged).** *Hold.* It is re-grounded after D1–D4.
- **PLAN-83 web-moving-between-plans (staged).** *Superseded:* its only deliverable (the work list beside a plan) is folded into the D2 navigation engine. "Add a plan" on a project's row is already PLAN-59 deliverable 4. "One notification for a burst" is already PLAN-51 deliverable 5 and waits with PLAN-51. The row of `doc/plans/web-moving-between-plans.adoc` is set `done` by the last of the D2 engine, PLAN-51 and PLAN-59, and D2 updates its anchors.
- **PLAN-85 web-theme-in-browser-settings (staged).** *Waits for: D1, D2, D3.*
  - It starts after the 001 runbook; the rail icon is not its concern (001).
  - The rail-foot bear moves above the collapse control, and the pairing and revoked layouts are checked together with the figure (005).
  - The duplicated language toggle in `BottomBar` goes, inside D2's bottom bar and More sheet (003, 007).
  - Theme and language become a `RadioGroup`, applied at once (008); `/settings/browser` follows D2's address scheme.
  - The per-browser transcription choice belongs to the D4 voice package.
- **PLAN-88 web-designer-panes (staged).** *Waits for: D1.*
  - `SegmentedControl`, `Dialog`, the two "Open" labels named apart (007);
  - frontmatter via `Field`, the file and paste inputs, the project as `SelectField` (008).
- **PLAN-107 web-reports-fourth-residue (staged).** No design dependency. It starts once the running PLAN-97 has landed, and is sequenced with the 001 and 004 runbooks (same `common.*.properties`) and the `web-design-docs-residue` runbook (`RowMenu.test.ts`). The Reports and CompletionReport redesign comes later, in the package for views no staged plan owns.
- **Terminal client.**
  - **The needs-spelling normalisation is one pull request, the terminal package:** `pm-api`, the catalogue, and the `pm-operator` readers (`PmConsole`, `ConsoleBadge`, `ConsoleNotice`, `OverviewFormatting`, and `NeedsCommand` if PLAN-36 has landed). It starts after the running PLAN-104 has landed, and the D1 web foundation builds on it.
  - The other shared-text changes of D1 (`control.end.*`, `control.cancel_epic.*`) go into the same terminal package. Those of D4 (the `transcribe` role and speech consent in the onboarding and consent listings) go into the D4 voice package's terminal part.

**New packages, staged after their design session:**
- **The terminal package** (first): the needs-spelling normalisation across `pm-api`, catalogue and `pm-operator`, and D1's other shared-text renames.
- **D1 foundation:** components, the web side of the catalogue clean-up, lints as warnings.
- **D2 navigation engine** (including PLAN-83's work list, the breadcrumb with `href` and Up in `TopBar`).
- **D3 brand assets and icon.**
- **D4:**
  - contract + mock;
  - the web chat (built and adopted on the surfaces no staged plan owns);
  - the voice package: the `transcribe` role in onboarding and settings, the per-browser choice, the microphone, contract + mock.
- **Views no staged plan owns:** System, Cleanup, CompletionReport, Reports, decomposition, Pairing, `HoldResolutionModal`, `FindingsReviewModal`, `ConfinedFilePicker`, `PrAdoptPanel`, `RecipeSelector`. (Settings / MachineSettings forms and device rename → PLAN-60; `PlanSettingsModal` → PLAN-55; breadcrumb and `TopBar` → D2 engine; bottom bar → PLAN-49, with PLAN-85 only removing the toggle; `WorkControlPanel` dialogs → PLAN-55 / PLAN-58.)

**Now, without design (runbooks):**
- **001:** the rail label, the cog, `control.plan.settings` → "Plan-Einstellungen" with its shared text and the terminal check.
- **002:** remove the start-page toggle.
- **004:** the code part (the layout modes).
- **The `web-design-docs-residue` runbook** (PLAN-74 deliverables 3 and 4).
- **PLAN-107**, once PLAN-97 has landed.
