# Landing Analysis: PLAN-59 — Web: Projects Grid and Project Onboarding

epic: ui-improvements
workstream: WS-03
pr: #538 (merged as f73bd9dd, `feat(web): rebuild the projects lens and the project onboarding (PLAN-59)`; 28 files, +3023/-1978)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-59-web-projects-and-onboarding-001.md`, runbook lane, harness `opencode`, model `deepseek-v4.1-flash/high`, effort `high`, executor `0.1.2183`; analysed 2026-11-23). `landing-check`: `total_tokens` and `plugin_version` are `unknown`. The message was read as a lead; one read-only leaf read the squash commit and re-read two build logs; the orchestrator re-read the three Layer-3 sites named below at `f73bd9dd` and the Sonar gate.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Projects lens | shipped as the merged design (rows plus one project page), by the operator's recorded choice; vocabulary not at zero: `ProjectExplorerView` keeps a raw `<button class="ended-row">` (:1399), a raw `<input>` (:1525) and two `<select>`; the over-size spread is hand-built text rows (`OverSizeChart.svelte`, PLAN-146, is not mounted) | `ProjectExplorerView.svelte` (leaf) |
| D2 Identity step | **met in part:** the three fields exist but **cannot be recorded for a project being added** (L3-1); the pilot checkbox is always shown (design: only while the lifecycle gate holds); the run has no address of its own | `ProjectOnboardingView.svelte:201,669-681` (re-read) |
| D3 Needs the terminal | shipped; the card shows at steps 1–3 of every being-added run (L3-3) | `ProjectOnboardingView.svelte:112-118` (re-read) |
| D4 Acts of a project, Add a plan | shipped; `byState.ts:433-476` still has `open_running_work` as primary and `add_plan` secondary, the page re-orders in `ProjectExplorerView.svelte:858-885` (two sources of truth), no project act is tested in `byState.test.ts` | leaf |
| D5 Onboarding choice screens (PLAN-169 residue) | D5b and D5c(AspectChoice) met with real mutation runs (leaf re-read logs `185208` and `222109:1200`); **D5a partly:** the scan is kept in-session, a fresh mount still names no harness (the engine gap, declared); **D5c coverage is not "at the head":** 80.24 % branch was run before three commits (+297/-51, 7 files) and the main merge, margin 0.24 pt | `OnboardingView.svelte:199-241`; log `205521:965` |
| added-unplanned | 13 of 28 files are outside the Expected Surface (the landing says "the two touched files"): `common.*`/`onboarding.*` properties, `ProjectsCatalogueTest`, `vocabulary-baseline.json`, `RowMenu.svelte`, `display/index.ts`, `catalogue.test.ts`, `routes.test.ts`, `runtime.ts`, `RolesReview.svelte`, `screens.ts`, `chrome.spec.js`, `showroom.spec.js`; declared but untouched: `byState.test.ts`, `AspectChoice.test.ts`, `ModelsChoice.*`, `AppRouter.svelte`; **no `doc/design/**` file although the spec's Standing Constraint requires the design pages updated** | `git show f73bd9dd --stat` |

**"5 of 5 met" is the lane's count;** D2 is met in part (L3-1, the pilot gate, no address), D5c's coverage clause is not at the head, and the design pages were not updated.

## Three-Layer Analysis

By reading, no build, no test run, no write.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, plan-less), 28 files and +3023 lines.
- **Verdict:** layer 1 findings (8, minor); layer 2 findings (5 minor, 2 info; all five declared residue items check out as true); layer 3 findings (**3 major**, 11 minor, 1 info). No blocker.
- **Re-read by the orchestrator at `f73bd9dd`:** `ProjectOnboardingView.svelte:196-206,112-118,676-684` (L3-1, L3-3); `ProjectExplorerView.svelte:23,960-961,1277-1278` and `ActiveEpicSummary`/`ActivePlanSummary` `needs_operator_count` (L3-4). The remaining points are leaf reads, marked as leads.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity |
|---------|-------|---------|----------|
| L1-1 (minor) | the design pages are not updated; the new `project.needs_terminal.*`, `project.page.*`, `project.identity.*` texts, the Alt+arrow route and the needs-terminal pattern appear in no design table | leaf | refutes |
| L1-2 (minor) | the "after" screenshots (21:43–21:46) predate `6c7e58c8` (21:47), `444834fe`, `eac5ccb8`, `7fcd7fae`; only 7 PNGs | leaf | refutes |
| L1-3 (minor) | D5c coverage not at the head (see above); the same flaw the spec named for PLAN-169 | leaf | refutes |
| L1-4 (minor) | D2: pilot checkbox always shown (`ProjectOnboardingView.svelte:669-675`); no address of its own (`/projects/onboard`, which `epic.md` names PLAN-59 as owner of) | leaf | refutes |
| L1-5 (minor) | 13 of 28 files outside the surface (listed above) | leaf | benign |
| L1-6 (minor) | `byState.ts` primary/secondary vs the re-grounding | leaf | refutes |
| L1-7 (minor) | vocabulary baseline shrank but is not zero | leaf | refutes |
| L1-8 (minor) | D5a only partly done; the spec said to ask the operator if the engine row settled neither, and the lane did not (`progress.md:32`) | leaf | refutes |

### Layer 2 — Implemented

Non-vacuous: the D5a scenario, the D5b exact-body tests (fail under both mutations), Check again re-read, the Add a plan disabled reason, reorder with refusal restore. Gaps: no test for the identity Save disabled with no project id (`ProjectOnboardingView.test.ts:165-204` passes a `projectId`); the shared `applyRoute` change `objectTab: null` (`runtime.ts:1519-1521`) has no store-level test; the e2e "starts a plan from a row" asserts the start view and a URL change only (the unit test carries the real assertion); no tests for stale-generation guards, SSE refresh, ended-work paging, focus after reorder or the unenrol notice; the old `?project=` tests were deleted and the code kept; `chrome.spec.js:71-79` lost its "list text buttons carry no advisory" assertion. **Declared residue is true:** the engine gap; the terminal still formats `project.state.*` (`ProjectSelector.java:344`, `ProjectsExplorerLevel.java:358`) and its assertion in `ProjectsCatalogueTest` is now pinned by key presence only; no later-run-at-aspect code; `focusRequestedProject` unreachable.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ProjectOnboardingView.svelte:201,680-681,372-381` (re-read); engine `15-project-onboarding.adoc:15`, `MockRuntimeEngine.java:4872-4884`, `client.ts:3256-3281` (leaf) | a project being added (`projects.being-added`, the state D2 draws) | `saveIdentity` returns without a `projectId` and Save is disabled with "records its identity in the terminal client until the project is enrolled"; `runOfBeingAdded` drops every link; the engine spec says the choices are operator waits served to both clients and the mock serves `links.set` on the entry, so the fields are decorative until the terminal enrols and the on-screen sentence states an engine limit the spec contradicts (confirmed for spec and mock, suspected for the real engine) | **major** |
| L3-2 | `runtime.ts:2033-2043` (leaf) | a reload or a second tab on `/projects/onboard` | the run state lives only in the store; the page shows the "which checkout" form, not the run; the design says each run is a route of its own; the path collides with a project whose id is `onboard`; the epic's open item owned by PLAN-59, unaddressed (suspected, reload not run) | **major** |
| L3-3 | `ProjectOnboardingView.svelte:112-118` (re-read) | any being-added run, steps 1–3 | `needsTerminal` is true whenever `build_environment.captured_at` is absent and `runOfBeingAdded` never carries one: the card asserts "Still missing: the build environment" without a read, and Check again re-reads the explorer, which cannot carry it, so the card never clears; the design names this case only at step ③ | minor |
| L3-4 | `ProjectExplorerView.svelte:960-961,1277-1278` (re-read); `EnrolledProject.java:31` (leaf) | any project with `needs_operator_count > 0`, ready ones included | `ReasonBadge reason="setup"` labels operator items as a setup need; screenshot `59-projects-and-start-after.png` shows "ready" beside "! 2 · setup"; the declined CodeRabbit item backs the word, not the label on ready projects | **major** |
| L3-5 | `runtime.ts:1519-1521,1633`; `PlanView.svelte:322-334` | return from a task to a plan or epic | `applyRoute` resets `objectTab: null` for every route and `goBack` passes no tab, so the tab may drop (the reset also fixes a real leak) | minor, suspected |
| L3-6 | `ProjectExplorerView.svelte:269` | a `project.changed` event during a read or save | `loadProjectPage` does `++pageGeneration`, so an in-flight `loadSetup`, over_size read, ended-work read or settings save is dropped and `settingsLoading` / `isLoadingOnboarding` stay true | minor |
| L3-7 | `:382-398,1364-1424` | "Older ended work" | replaces the page, no way back (`has_newer` ignored), reopening does not reload | minor |
| L3-8 | `:1338-1343` | an epic row | reads "N plans · 0 epics"; plan rows show no needs marker; ended rows no outcome | minor |
| L3-9 | release/unenrol | unenrol refused `scope_active`; after an unenrol | the release dialog lists "ends" and "stays" only; one sentence instead of the running work plus "Release the project…"; nothing says it was unenrolled (`project.release.unenrolled` unused) | minor |
| L3-10 | being-added rows (`:1134-1141` vs `:831`) | ctrl/cmd-click | visible Continue and Abandon where the design says a menu; the click handler hijacks ctrl/cmd-click | minor |
| L3-11 | keyed-each reorder | reorder | focus may drop in Chrome; nothing announces the new position | minor, suspected |
| L3-12 | the "most used" sort | none | no most-used field or order exists in `pm-api` or the mock, yet the heading prints "(most used)" | minor |
| L3-13 | dead code and keys | none | `focusRequestedProject`, `focusedFilter` and the popstate listener (`:132-133,187-225`), a dead `act.id==='add_plan'` branch (`:807-810`), keys with no user (`project.act.start_plan/start_epic/setup/release/unenrol/open_running_work`, `project.page.load_failed`, `project.explorer.move_up/move_down`), a hard-coded English `'pm-operator project add'` fallback (`ProjectOnboardingView.svelte:707,745`), a concatenated `project.release.refused` + `'. '` + `refused_hint` | minor |
| L3-14 | `OnboardingView.svelte:213-236` | a state with both `harnesses[]` and a scan | `harnessNamesFrom` takes one source, never both | minor, suspected |
| L3-15 | the changed files | none | no `{@html}`, `innerHTML` or timer wait; every new key in en and de; no wire-name mismatch found for `readiness`, `EndedWorkItem`, `ProjectDetailResponse` | info |

**Declined CodeRabbit items** (`progress.md:41,48,50`): "most used is the runtime's order" is design-backed (`project-explorer.adoc:153-154`) but nothing on the wire carries it (L3-12); "reason is `setup`" is design-backed for the word but not for the label on ready projects (L3-4); "page order is the design's" is backed by `#project-page`, while `#actions` lists another order, so the two design paragraphs disagree and the lane did not say so.

**Not shown by reading:** the 20 CodeRabbit dispositions; Sonar `0` (gate OK, below); logs `185009, 221729, 203647, 203425, 221127, 010711, 002751`; real-engine acceptance of a pre-enrolment `set`; a browser reload of `/projects/onboard`; focus behaviour.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Dependencies | no dependency without asking | conforms | no POM or package file in the diff |
| CLAUDE.md § Web Standards | no `{@html}`; no timer wait | conforms | leaf |
| spec Standing Constraint | design pages updated with the change | deviates | no `doc/design/**` file in the diff |
| spec | after-shots re-taken after visual fixes | deviates | L1-2 |
| spec | coverage at the head | deviates | L1-3 |
| CLAUDE.md | zero Sonar findings | conforms | `sonar_rest gate-status --pr 538` OK (new coverage 100.0, duplication 0.0, hotspots reviewed 100); the lane states `0` |
| lane rules | forbidden flags | deviates (disclosed) | one `--force-with-lease` push (a fast-forward, no history rewritten) |

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `unknown`; model `deepseek-v4.1-flash/high`; `guard: none on this harness (unenforced)`.
- Review: `review_body_findings_total=20`, `handled=20`, `unresolved_review_threads=0`; 17 fixed, 3 declined with the design as the reason.
- One routed build hit the daemon's 300 s bound and was re-run in-process; the mailbox read was refused `invalid_target_plan` (the runbook-lane mailbox defect recurs).
- Suite 3254 → 3238 tests (`ProjectExplorerView.test.ts` rewritten, 1355 lines changed; the spec lists no "Tests to remove").

## Routing and Merge Behavior

- Merge `f73bd9dd` (merge queue, merge group `45095013557` green).
- Overlap observed: the footprint touches the declared surfaces of the staged PLAN-85 (`routes.ts`, `routes.test.ts`, `SettingsView.svelte`, `chrome.spec.js`, README), PLAN-135, 136 and 144 (`routes.ts`, `runtime.ts`, `common.*`, and for 144 also `TopBar.svelte`, `SettingsView.svelte`, `chrome.spec.js`), and the parked PLAN-137 and PLAN-55: **their claims must be re-read at `14c5ecc4` before any emit.**

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 538; `landing` `landings/PLAN-59.md`; `plan_marshall_plan_id` `rb-59-web-projects-and-onboarding`
- [x] epic.md: Open Defect for this landing (three layer-3 majors); `PLAN-169 landed` and `PLAN-66 deliverables 8 to 10` entries partly retired (exact `models` payload, the AspectChoice resync revert runs, `yes_remember`, the harness seen in the same run); the fresh-mount case and the coverage at the head stay named; `PLAN-143 landed` stays open; the `/projects/onboard` history entries stay open
- [x] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Layer-3 majors L3-1, L3-2, L3-4 are reported to the operator at once and not fixed inline;** a fix plan is staged only on the operator's word. L3-2 is also the `/projects/onboard` ambiguity the epic already names with PLAN-59 as owner.
- PLAN-85, 135, 136, 144 (staged), PLAN-137 and PLAN-55 (parked): re-read the claims at `14c5ecc4`.
- PLAN-146's `OverSizeChart` on the setup tab is still unmounted.
